import chiefnavigator.console.RefreshContactBounties;
import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.missions.cb.BaseCustomBounty;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseHubMission;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseHubMissionCreator;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseMissionHub;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMission;
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionHub;
import java.awt.Color;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Real native abort/ref/RNG lifecycle, with campaign entity fixtures and no save writes. */
public final class RefreshContactBountiesRegression {
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

    static final class PersonFixture {
        final Map<String, Object> values = new HashMap<>();
        final MarketAPI market = mock(MarketAPI.class, (name, args) -> null);
        final MemoryAPI memory = mock(MemoryAPI.class, (name, args) -> {
            switch (name) {
                case "contains": return values.containsKey(args[0]);
                case "get": return values.get(args[0]);
                case "set": values.put((String) args[0], args[1]); return null;
                case "unset": values.remove(args[0]); return null;
                default: throw new AssertionError("Unexpected memory call: " + name);
            }
        });
        final PersonAPI api = mock(PersonAPI.class, (name, args) -> {
            if (name.equals("getMemoryWithoutUpdate")) return memory;
            if (name.equals("getMarket")) return market;
            throw new AssertionError("Unexpected person call: " + name);
        });
    }

    static final class EntityFixture {
        int removals;
        final LocationAPI location = mock(LocationAPI.class, (name, args) -> {
            if (name.equals("removeEntity")) { removals++; return null; }
            throw new AssertionError("Unexpected location call: " + name);
        });
        final SectorEntityToken api = mock(SectorEntityToken.class, (name, args) -> {
            if (name.equals("getContainingLocation")) return removals == 0 ? location : null;
            if (name.equals("hasTag")) return false;
            throw new AssertionError("Unexpected entity call: " + name);
        });
    }

    static final class Bounty extends BaseCustomBounty {
        final EntityFixture target = new EntityFixture();
        boolean succeed = true;
        boolean throwDuringCreate;
        boolean created;
        boolean updated;
        boolean rewardGenerated;
        MarketAPI createdAt;

        @Override protected boolean create(MarketAPI market, boolean barEvent) {
            check(!barEvent, "Reroll must use the contact lifecycle");
            check(getHub() != null && getCreator() != null && getGenRandom() != null,
                    "Native hub/creator/RNG must be bound before generation");
            check(setPersonMissionRef(getPerson(), "$" + getMissionId() + "_ref"),
                    "Original abort must release the native per-person mission ref");
            getChanges().add(new EntityAdded(target.api));
            created = true;
            createdAt = market;
            if (throwDuringCreate) throw new IllegalStateException("fixture generation failure");
            return succeed;
        }

        @Override public void genMissionRewardMultAndQuality() { rewardGenerated = true; }

        @Override public void updateInteractionData(InteractionDialogAPI dialog,
                Map<String, MemoryAPI> memoryMap) {
            check(created && rewardGenerated && !isMissionCreationAborted(),
                    "Only successfully generated missions may update offer tokens");
            updated = true;
        }

        void markAccepted() { currentStage = Stage.BOUNTY; }
    }

    static final class Creator extends BaseHubMissionCreator {
        final List<Bounty> generated = new ArrayList<>();
        final String missionId;
        int reseeds;
        boolean succeed = true;
        boolean throwDuringCreate;

        Creator(String missionId) {
            super(null);
            this.missionId = missionId;
            setNumCompleted(7);
            setNumFailed(2);
        }

        @Override public void setSeed(long value) { super.setSeed(value); reseeds++; }

        @Override public HubMission createHubMission(MissionHub hub) {
            Bounty mission = new Bounty();
            mission.setMissionId(missionId);
            mission.succeed = succeed;
            mission.throwDuringCreate = throwDuringCreate;
            generated.add(mission);
            return mission;
        }
    }

    static Bounty offer(BaseMissionHub hub, Creator creator) {
        creator.setSeed(123L);
        creator.updateRandom();
        Bounty mission = (Bounty) creator.createHubMission(hub);
        mission.setHub(hub);
        mission.setCreator(creator);
        mission.setGenRandom(creator.getGenRandom());
        mission.createAndAbortIfFailed(hub.getPerson().getMarket(), false);
        return mission;
    }

    static final class NonBounty extends BaseHubMission {
        int aborts;
        @Override protected boolean create(MarketAPI market, boolean barEvent) { return true; }
        @Override public void abort() { aborts++; super.abort(); }
    }

    static void checkRefreshAndGuards() {
        PersonFixture person = new PersonFixture();
        BaseMissionHub hub = new BaseMissionHub(person.api);
        Creator creator = new Creator("mcb");
        Bounty old = offer(hub, creator);
        NonBounty ordinary = new NonBounty();
        Creator acceptedCreator = new Creator("accepted_mcb");
        Bounty accepted = offer(hub, acceptedCreator);
        accepted.markAccepted();
        Creator abortedCreator = new Creator("aborted_mcb");
        Bounty aborted = offer(hub, abortedCreator);
        aborted.abort();
        BaseMissionHub foreignHub = new BaseMissionHub(new PersonFixture().api);
        Bounty foreign = offer(foreignHub, new Creator("foreign_mcb"));
        Bounty noCreator = new Bounty();
        noCreator.setHub(hub);
        Bounty wrongPerson = new Bounty();
        wrongPerson.setHub(hub);
        wrongPerson.setCreator(new Creator("wrong_person"));
        wrongPerson.setPersonOverride(new PersonFixture().api);
        List<HubMission> offered = hub.getOfferedMissions();
        offered.addAll(Arrays.asList(ordinary, old, accepted, aborted, foreign,
                noCreator, wrongPerson));
        List<HubMission> original = new ArrayList<>(offered);
        int beforeSeeds = creator.reseeds;
        Map<String, MemoryAPI> memory = new HashMap<>();

        int refreshed = RefreshContactBounties.refreshOffers(hub, null, memory);
        check(refreshed == 1 && offered.size() == original.size(),
                "Exactly one safe existing bounty must be replaced");
        Bounty replacement = (Bounty) offered.get(1);
        check(replacement != old && replacement.getCreator() == creator,
                "Keep the original native creator and its completion tracking");
        check(replacement.createdAt == person.market && replacement.updated,
                "Generate at the original contact market and update its offer tokens");
        check(creator.getNumCompleted() == 7 && creator.getNumFailed() == 2,
                "Reroll must not reset or increment native completion/failure counters");
        check(creator.reseeds == beforeSeeds + 1,
                "Reroll must seed fresh randomness before native generation");
        check(old.isMissionCreationAborted() && old.target.removals == 1
                        && old.getChanges().isEmpty(),
                "Native abort must remove the old target and clear tracked changes");
        check(person.values.get("$mcb_ref") == replacement,
                "The new bounty must exclusively own the native mission ref");
        for (int index : new int[] {0, 2, 3, 4, 5, 6}) {
            check(offered.get(index) == original.get(index),
                    "Other offers must retain identity and list order");
        }
        check(ordinary.aborts == 0 && accepted.target.removals == 0
                        && accepted.getCurrentStage() == BaseCustomBounty.Stage.BOUNTY,
                "Non-bounty and accepted missions must remain untouched");
        check(person.values.get("$accepted_mcb_ref") == accepted
                        && foreign.target.removals == 0,
                "Accepted/foreign missions must retain native refs and targets");

        check(RefreshContactBounties.refreshOffers(hub, null, memory) == 1,
                "Repeated refresh must remain usable without a cooldown wait");
        check(replacement.target.removals == 1
                        && person.values.get("$mcb_ref") == offered.get(1),
                "Repeated refresh must clean its prior target without leaking refs");
    }

    static void checkFailedGenerationCleanup() {
        PersonFixture person = new PersonFixture();
        BaseMissionHub hub = new BaseMissionHub(person.api);
        Creator creator = new Creator("mcb");
        Bounty old = offer(hub, creator);
        NonBounty other = new NonBounty();
        hub.getOfferedMissions().addAll(Arrays.asList(other, old));
        creator.succeed = false;
        check(RefreshContactBounties.refreshOffers(hub, null, new HashMap<>()) == 0,
                "Failed native generation must not claim a refreshed offer");
        Bounty failed = creator.generated.get(creator.generated.size() - 1);
        check(old.target.removals == 1 && failed.target.removals == 1
                        && failed.isMissionCreationAborted() && !failed.updated,
                "Native failure cleanup must remove old and partial new targets");
        check(!person.values.containsKey("$mcb_ref")
                        && hub.getOfferedMissions().equals(Arrays.asList(other)),
                "Failed rerolls must leave no stale mission ref or invalid offered mission");
        check(other.aborts == 0 && creator.getNumCompleted() == 7
                        && creator.getNumFailed() == 2,
                "Generation failure must preserve other work and accepted-mission counters");
    }

    static void checkThrownGenerationCleanup() {
        PersonFixture person = new PersonFixture();
        BaseMissionHub hub = new BaseMissionHub(person.api);
        Creator creator = new Creator("mcb");
        Bounty old = offer(hub, creator);
        NonBounty other = new NonBounty();
        hub.getOfferedMissions().addAll(Arrays.asList(old, other));
        creator.throwDuringCreate = true;
        try {
            RefreshContactBounties.refreshOffers(hub, null, new HashMap<>());
            throw new AssertionError("Generation exception must reach the command error handler");
        } catch (IllegalStateException expected) {
            check("fixture generation failure".equals(expected.getMessage()),
                    "Unexpected generation exception");
        }
        Bounty partial = creator.generated.get(creator.generated.size() - 1);
        check(old.target.removals == 1 && partial.target.removals == 1
                        && partial.isMissionCreationAborted() && !partial.updated,
                "Thrown generation must clean both old and partially generated targets");
        check(!person.values.containsKey("$mcb_ref")
                        && hub.getOfferedMissions().equals(Arrays.asList(other))
                        && other.aborts == 0,
                "Generation exceptions must leave no mission ref or partial offer");
    }

    public static void main(String[] args) {
        SettingsAPI priorSettings = Global.getSettings();
        FactoryAPI priorFactory = Global.getFactory();
        try {
            Global.setSettings(mock(SettingsAPI.class, (name, values) -> {
                if (name.equals("getFloat")) return 7f;
                if (name.equals("getInt")) return 3;
                if (name.equals("getColor")) return Color.WHITE;
                return null;
            }));
            Global.setFactory(mock(FactoryAPI.class, (name, values) -> {
                if (name.equals("createCargo")) return mock(CargoAPI.class,
                        (cargoCall, cargoValues) -> null);
                throw new AssertionError("Unexpected factory call: " + name);
            }));
            checkRefreshAndGuards();
            checkFailedGenerationCleanup();
            checkThrownGenerationCleanup();
            System.out.println("PASS: native bounty abort/ref cleanup, fresh creator RNG, "
                    + "counter preservation, offer ordering, accepted/non-bounty/foreign guards, "
                    + "repeated refresh and failed/thrown generation cleanup.");
        } finally {
            Global.setSettings(priorSettings);
            Global.setFactory(priorFactory);
        }
    }
}
