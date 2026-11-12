import chiefnavigator.console.AddContactBountyProgress;
import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.missions.cb.BaseCustomBounty;
import com.fs.starfarer.api.impl.campaign.missions.cb.CBDerelict;
import com.fs.starfarer.api.impl.campaign.missions.cb.CBPirate;
import com.fs.starfarer.api.impl.campaign.missions.cb.CustomBountyCreator;
import com.fs.starfarer.api.impl.campaign.missions.cb.MilitaryCustomBounty;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseHubMission;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseHubMissionCreator;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseMissionHub;
import java.awt.Color;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Checks the command against native difficulty/picker behavior, without campaign rewards. */
public final class ContactBountyProgressRegression {
    interface Call { Object invoke(String name, Object[] args); }

    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[] {type}, (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("hashCode")) return System.identityHashCode(proxy);
                    if (name.equals("equals")) return proxy == args[0];
                    if (name.equals("toString")) return type.getSimpleName();
                    Object value = call.invoke(name, args);
                    if (value != null) return value;
                    Class<?> returns = method.getReturnType();
                    if (returns == boolean.class) return false;
                    if (returns == int.class) return 0;
                    if (returns == long.class) return 0L;
                    if (returns == float.class) return 0f;
                    if (returns == double.class) return 0d;
                    return null;
                }));
    }

    static final class MemoryFixture {
        final Map<String, Object> values = new HashMap<>();
        int writes;
        final MemoryAPI api = mock(MemoryAPI.class, (name, args) -> {
            switch (name) {
                case "get": return values.get(args[0]);
                case "contains": return values.containsKey(args[0]);
                case "getBoolean": return Boolean.TRUE.equals(values.get(args[0]));
                case "set": writes++; values.put((String) args[0], args[1]); return null;
                default: throw new AssertionError("Unexpected memory mutation/access: " + name);
            }
        });
    }

    static final class PersonFixture {
        final MemoryFixture memory = new MemoryFixture();
        final FactionAPI faction = mock(FactionAPI.class, (name, args) -> {
            if (name.equals("getId")) return "hegemony";
            throw new AssertionError("Unexpected native picker faction call: " + name);
        });
        final PersonAPI api = mock(PersonAPI.class, (name, args) -> {
            if (name.equals("getMemoryWithoutUpdate")) return memory.api;
            if (name.equals("getFaction")) return faction;
            throw new AssertionError("Progress must not touch relationship/importance/rewards: " + name);
        });
    }

    static final class NativeBounty extends MilitaryCustomBounty {
        int nativeDifficulty(DifficultyChoice choice) { return pickDifficulty(choice); }
        CustomBountyCreator nativeCreator(DifficultyChoice choice, int seed) {
            setGenRandom(new Random(seed));
            return pickCreator(pickDifficulty(choice), choice);
        }
        void markAccepted() { currentStage = Stage.BOUNTY; }
    }

    static NativeBounty bounty(BaseMissionHub hub, String family) {
        NativeBounty mission = new NativeBounty();
        mission.setMissionId(family);
        mission.setHub(hub);
        mission.setCreator(new BaseHubMissionCreator(null));
        mission.setQuality(1f);
        return mission;
    }

    static MemoryFixture world() {
        MemoryFixture memory = new MemoryFixture();
        Global.setSector(mock(SectorAPI.class, (name, args) -> {
            if (name.equals("getMemoryWithoutUpdate")) return memory.api;
            throw new AssertionError("Progress must not create rewards or campaign content: " + name);
        }));
        return memory;
    }

    static void nativeTiers(NativeBounty mission, int low, int normal, int high) {
        check(mission.nativeDifficulty(BaseCustomBounty.DifficultyChoice.LOW) == low,
                "Native easier difficulty differs");
        check(mission.nativeDifficulty(BaseCustomBounty.DifficultyChoice.NORMAL) == normal,
                "Native normal difficulty differs");
        check(mission.nativeDifficulty(BaseCustomBounty.DifficultyChoice.HIGH) == high,
                "Native challenging difficulty differs");
    }

    static void checkProgressAgainstNative() {
        world();
        PersonFixture person = new PersonFixture();
        NativeBounty mission = bounty(new BaseMissionHub(person.api), "mcb");
        check(AddContactBountyProgress.getProgress(mission) == 0,
                "Fresh native bounty family starts at zero");
        nativeTiers(mission, 0, 1, 4);
        BaseCustomBounty.AggregateBountyData data = mission.getAggregateData();
        check(AddContactBountyProgress.addProgress(mission, 5) == 5,
                "Default five-level increase must produce native tier five");
        check(mission.getAggregateData() == data
                        && data.completedDifficulty.equals(Arrays.asList(5, 5, 5)),
                "Preserve native aggregate identity and fill its native three-entry window");
        nativeTiers(mission, 2, 6, 8);

        data.completedDifficulty.clear();
        data.completedDifficulty.add(9);
        check(AddContactBountyProgress.getProgress(mission) == 3,
                "One native completion still uses the three-completion divisor");
        nativeTiers(mission, 0, 4, 6);
        check(AddContactBountyProgress.addProgress(mission, 2) == 5,
                "A partial history must increase effective progression by the requested amount");
        nativeTiers(mission, 2, 6, 8);

        data.completedDifficulty.clear();
        data.completedDifficulty.addAll(Arrays.asList(1, 5));
        check(AddContactBountyProgress.getProgress(mission) == 2,
                "Two native completions retain the native divisor");
        check(AddContactBountyProgress.addProgress(mission, 1) == 3,
                "Two-entry history must increase by exactly one effective level");
        nativeTiers(mission, 0, 4, 6);

        data.completedDifficulty.clear();
        data.completedDifficulty.addAll(Arrays.asList(2, 7, 9));
        check(AddContactBountyProgress.getProgress(mission) == 6,
                "Mixed native history must retain the native average");
        check(AddContactBountyProgress.addProgress(mission, 3) == 9,
                "Existing progression must increase without resetting to the argument");
        nativeTiers(mission, 4, 9, 10);
        check(AddContactBountyProgress.addProgress(mission, 5) == 10,
                "Progress must cap at native MAX_DIFFICULTY");
        nativeTiers(mission, 5, 9, 10);
        check(AddContactBountyProgress.addProgress(mission, 1) == 10,
                "Repeated increases at the native cap must be stable");
        check(person.memory.values.isEmpty(),
                "Progress must leave NPC memory and completion counters untouched");
    }

    static void checkGlobalSharingAndFamilyIsolation() {
        MemoryFixture sectorMemory = world();
        PersonFixture firstPerson = new PersonFixture();
        PersonFixture secondPerson = new PersonFixture();
        NativeBounty first = bounty(new BaseMissionHub(firstPerson.api), "mcb");
        NativeBounty second = bounty(new BaseMissionHub(secondPerson.api), "mcb");
        NativeBounty underworld = bounty(new BaseMissionHub(secondPerson.api), "ucb");
        BaseCustomBounty.AggregateBountyData otherFamily = underworld.getAggregateData();
        otherFamily.completedDifficulty.addAll(Arrays.asList(1, 2, 3));
        NativeBounty accepted = bounty((BaseMissionHub) first.getHub(), "mcb");
        accepted.markAccepted();
        List<BaseHubMission.Abortable> acceptedChanges = new ArrayList<>(accepted.getChanges());

        AddContactBountyProgress.addProgress(first, 5);
        check(first.getAggregateData() == second.getAggregateData()
                        && AddContactBountyProgress.getProgress(second) == 5,
                "The same native family must share sector-global progress across NPCs");
        nativeTiers(second, 2, 6, 8);
        check(underworld.getAggregateData() == otherFamily
                        && otherFamily.completedDifficulty.equals(Arrays.asList(1, 2, 3)),
                "Other mission families must retain their own progression");
        check(sectorMemory.values.size() == 2
                        && sectorMemory.values.get("$mcb_aggregateData") == first.getAggregateData()
                        && sectorMemory.values.get("$ucb_aggregateData") == otherFamily,
                "Only the native per-family sector keys may be written");
        check(accepted.getCurrentStage() == BaseCustomBounty.Stage.BOUNTY
                        && accepted.getChanges().equals(acceptedChanges),
                "Accepted mission stage and native cleanup obligations must remain intact");
        check(firstPerson.memory.values.isEmpty() && secondPerson.memory.values.isEmpty(),
                "Global progression must not change either contact's memory");
    }

    static void checkSelectionAndInvalidAmounts() {
        MemoryFixture sectorMemory = world();
        PersonFixture person = new PersonFixture();
        BaseMissionHub hub = new BaseMissionHub(person.api);
        NativeBounty first = bounty(hub, "mcb");
        for (int invalid : new int[] {-1, 0, 11, Integer.MAX_VALUE}) {
            try {
                AddContactBountyProgress.addProgress(first, invalid);
                throw new AssertionError("Invalid progress amount must be rejected: " + invalid);
            } catch (IllegalArgumentException expected) { }
            check(sectorMemory.values.isEmpty() && sectorMemory.writes == 0,
                    "Invalid amounts must be rejected before native aggregate initialization");
        }
        NativeBounty duplicate = bounty(hub, "mcb");
        NativeBounty accepted = bounty(hub, "accepted_family");
        accepted.markAccepted();
        NativeBounty aborted = bounty(hub, "aborted_family");
        aborted.abort();
        NativeBounty wrongPerson = bounty(hub, "wrong_person");
        wrongPerson.setPersonOverride(new PersonFixture().api);
        NativeBounty foreign = bounty(new BaseMissionHub(new PersonFixture().api), "foreign");
        NativeBounty missingCreator = bounty(hub, "no_creator");
        missingCreator.setCreator(null);
        hub.getOfferedMissions().addAll(Arrays.asList(first, duplicate, accepted,
                aborted, wrongPerson, foreign, missingCreator));
        Map<String, BaseCustomBounty> eligible = AddContactBountyProgress.findEligibleOffers(hub);
        check(eligible.size() == 1 && eligible.get("$mcb_aggregateData") == first,
                "Selection must deduplicate families and exclude accepted/aborted/foreign offers");
        check(sectorMemory.values.isEmpty(),
                "Finding eligible offers must not initialize or mutate progression");
        check(hub.getOfferedMissions().size() == 7
                        && accepted.getCurrentStage() == BaseCustomBounty.Stage.BOUNTY,
                "Progress selection must leave all offered/accepted missions untouched");
    }

    static void checkActualNativeDerelictPicks() {
        world();
        NativeBounty mission = bounty(new BaseMissionHub(new PersonFixture().api), "mcb");
        List<CustomBountyCreator> original = MilitaryCustomBounty.CREATORS;
        try {
            MilitaryCustomBounty.CREATORS = new ArrayList<>(Arrays.asList(new CBPirate(), new CBDerelict()));
            int fresh = 0;
            for (int seed = 0; seed < 400; seed++) {
                if (mission.nativeCreator(BaseCustomBounty.DifficultyChoice.HIGH, seed)
                        instanceof CBDerelict) fresh++;
            }
            check(fresh == 0, "Actual native creator picker must exclude Derelicts at fresh difficulty four");
            AddContactBountyProgress.addProgress(mission, 5);
            int normal = 0;
            int high = 0;
            for (int seed = 0; seed < 400; seed++) {
                if (mission.nativeCreator(BaseCustomBounty.DifficultyChoice.NORMAL, seed)
                        instanceof CBDerelict) normal++;
                if (mission.nativeCreator(BaseCustomBounty.DifficultyChoice.HIGH, seed)
                        instanceof CBDerelict) high++;
            }
            check(normal > 0 && high > 0,
                    "Actual native creator picker must offer Derelicts in both boosted tiers");
            System.out.println("Native picker over 400 seeds: fresh challenging Derelict " + fresh
                    + ", boosted normal " + normal + ", boosted challenging " + high + ".");
        } finally {
            MilitaryCustomBounty.CREATORS = original;
        }
    }

    public static void main(String[] args) {
        SettingsAPI priorSettings = Global.getSettings();
        FactoryAPI priorFactory = Global.getFactory();
        SectorAPI priorSector = Global.getSector();
        try {
            Global.setSettings(mock(SettingsAPI.class, (name, values) -> {
                if (name.equals("getFloat")) return 7f;
                if (name.equals("getInt")) return 3;
                if (name.equals("getColor")) return Color.WHITE;
                return null;
            }));
            Global.setFactory(mock(FactoryAPI.class, (name, values) -> {
                if (name.equals("createCargo")) return mock(CargoAPI.class,
                        (cargoCall, cargoValues) -> {
                            throw new AssertionError("Progress must not change cargo/rewards: " + cargoCall);
                        });
                throw new AssertionError("Progress must not create campaign content: " + name);
            }));
            checkProgressAgainstNative();
            checkGlobalSharingAndFamilyIsolation();
            checkSelectionAndInvalidAmounts();
            checkActualNativeDerelictPicks();
            System.out.println("PASS: native empty/partial/current/capped tiers, global family sharing, "
                    + "family isolation, mutation-free invalid amounts and accepted-offer guards, "
                    + "and actual native Derelict creator eligibility.");
        } finally {
            Global.setSettings(priorSettings);
            Global.setFactory(priorFactory);
            Global.setSector(priorSector);
        }
    }
}
