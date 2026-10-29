import chiefnavigator.console.RefreshContactBounties;
import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.RelationshipAPI;
import com.fs.starfarer.api.impl.campaign.missions.cb.BaseCustomBounty;
import com.fs.starfarer.api.impl.campaign.missions.cb.CBDerelict;
import com.fs.starfarer.api.impl.campaign.missions.cb.CBPirate;
import com.fs.starfarer.api.impl.campaign.missions.cb.CBRemnant;
import com.fs.starfarer.api.impl.campaign.missions.cb.CustomBountyCreator;
import com.fs.starfarer.api.impl.campaign.missions.cb.MilitaryCustomBounty;
import com.fs.starfarer.api.impl.campaign.missions.cb.UnderworldCustomBounty;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseHubMission;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseHubMissionCreator;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseMissionHub;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMission;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent;
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionHub;
import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Native three-choice creation with a registered creator fixture; no campaign/save writes. */
public final class ContactBountyTypeRegression {
    interface Call { Object invoke(String name, Object[] args); }
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("hashCode")) return System.identityHashCode(proxy);
                    if (name.equals("equals")) return proxy == args[0];
                    if (name.equals("toString")) return type.getSimpleName();
                    Object result = call.invoke(name, args);
                    if (result != null) return result;
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
        final MemoryAPI api = mock(MemoryAPI.class, (name, args) -> {
            if (name.equals("get")) return values.get(args[0]);
            if (name.equals("contains")) return values.containsKey(args[0]);
            if (name.equals("getBoolean")) return Boolean.TRUE.equals(values.get(args[0]));
            if (name.equals("set")) { values.put((String) args[0], args[1]); return null; }
            if (name.equals("unset")) { values.remove(args[0]); return null; }
            throw new AssertionError("Unexpected memory access: " + name);
        });
    }
    static final class PersonFixture {
        final MemoryFixture memory = new MemoryFixture();
        final RelationshipAPI relationship = mock(RelationshipAPI.class, (name, args) -> {
            if (name.equals("getRel")) return 0.5f;
            throw new AssertionError("Forced generation must not modify reputation: " + name);
        });
        final FactionAPI faction = mock(FactionAPI.class, (name, args) -> {
            if (name.equals("getId")) return "hegemony";
            throw new AssertionError("Unexpected faction access: " + name);
        });
        final MarketAPI market = mock(MarketAPI.class, (name, args) -> {
            if (name.equals("getFaction")) return faction;
            throw new AssertionError("Unexpected market access: " + name);
        });
        final PersonAPI api = mock(PersonAPI.class, (name, args) -> {
            if (name.equals("getMemoryWithoutUpdate")) return memory.api;
            if (name.equals("getMarket")) return market;
            if (name.equals("getRelToPlayer")) return relationship;
            if (name.equals("getImportance")) return PersonImportance.MEDIUM;
            if (name.equals("getManOrWoman")) return "man";
            if (name.equals("getHeOrShe")) return "he";
            throw new AssertionError("Forced creation must not change contact state: " + name);
        });
    }
    static final class EntityFixture {
        int removals;
        final LocationAPI location = mock(LocationAPI.class, (name, args) -> {
            if (name.equals("removeEntity")) { removals++; return null; }
            throw new AssertionError("Unexpected location mutation: " + name);
        });
        final SectorEntityToken api = mock(SectorEntityToken.class, (name, args) -> {
            if (name.equals("getContainingLocation")) return removals == 0 ? location : null;
            if (name.equals("hasTag")) return false;
            throw new AssertionError("Unexpected entity access: " + name);
        });
    }

    /** Same-ID registered override stands in for any installed Derelict target-picker fix. */
    public static final class RegisteredDerelict extends CBDerelict {
        static final List<Integer> difficulties = new ArrayList<>();
        static final List<EntityFixture> entities = new ArrayList<>();
        static boolean noEligibleTarget;
        public RegisteredDerelict() { }
        @Override public String getId() { return "CBDerelict"; }
        @Override public CustomBountyData createBounty(MarketAPI market,
                HubMissionWithBarEvent mission, int difficulty, Object stage) {
            difficulties.add(difficulty);
            EntityFixture entity = new EntityFixture();
            entities.add(entity);
            mission.getChanges().add(new BaseHubMission.EntityAdded(entity.api));
            if (noEligibleTarget) return null;
            CustomBountyData data = new CustomBountyData();
            data.difficulty = difficulty;
            data.fleet = mock(CampaignFleetAPI.class, (name, args) -> {
                throw new AssertionError("Native generation must not inspect the fixture fleet: " + name);
            });
            return data;
        }
        static void reset() { difficulties.clear(); entities.clear(); noEligibleTarget = false; }
    }

    /** A mod mission subclass must remain outside the exact-native forced-mode boundary. */
    static final class ObservedMilitary extends MilitaryCustomBounty {
    }
    static final class Creator extends BaseHubMissionCreator {
        MilitaryCustomBounty replacement;
        boolean modReplacement;
        Creator() { super(null); setNumCompleted(4); setNumFailed(1); }
        @Override public HubMission createHubMission(MissionHub hub) {
            replacement = modReplacement ? new ObservedMilitary() : new MilitaryCustomBounty();
            replacement.setMissionId("mcb");
            return replacement;
        }
    }
    static final class NonBounty extends BaseHubMission {
        @Override protected boolean create(MarketAPI market, boolean barEvent) { return true; }
    }

    static MilitaryCustomBounty nativeOffer(BaseMissionHub hub, Creator creator,
            EntityFixture oldTarget) {
        MilitaryCustomBounty old = new MilitaryCustomBounty();
        old.setMissionId("mcb"); old.setHub(hub); old.setCreator(creator);
        old.getChanges().add(new BaseHubMission.EntityAdded(oldTarget.api));
        check(old.setPersonMissionRef(hub.getPerson(), "$mcb_ref"), "Fixture native ref must bind");
        return old;
    }

    static Map<String, MemoryAPI> dialogMemory(PersonFixture person) {
        Map<String, MemoryAPI> memory = new HashMap<>();
        memory.put(MemKeys.LOCAL, person.memory.api);
        memory.put(MemKeys.ENTITY, person.memory.api);
        return memory;
    }

    static void verifyNativeChoices(BaseCustomBounty mission, int difficulty) {
        try {
            for (String suffix : new String[] {"Low", "Normal", "High"}) {
                Field creator = BaseCustomBounty.class.getDeclaredField("creator" + suffix);
                creator.setAccessible(true);
                check(creator.get(mission) instanceof RegisteredDerelict,
                        "All native choices must use the actual registered override class");
                Field data = BaseCustomBounty.class.getDeclaredField("data" + suffix);
                data.setAccessible(true);
                check(((CustomBountyCreator.CustomBountyData) data.get(mission)).difficulty == difficulty,
                        "Native test mode must set all three choices to the chosen difficulty");
            }
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot inspect native three-choice generation", failure);
        }
    }

    static void checkResolutionAndInvalidPreflight(RegisteredDerelict registered) {
        PersonFixture person = new PersonFixture();
        BaseMissionHub hub = new BaseMissionHub(person.api);
        Creator creator = new Creator();
        EntityFixture entity = new EntityFixture();
        MilitaryCustomBounty old = nativeOffer(hub, creator, entity);
        hub.getOfferedMissions().add(old);
        check(RefreshContactBounties.resolveBountyType(old, "derelict") == registered,
                "Type resolution must retain the installed creator instance/class");
        check(RefreshContactBounties.resolveDifficulty(registered, null) == 5,
                "Derelict default difficulty must respect native minimum five");
        check(RefreshContactBounties.resolveDifficulty(new CBRemnant(), null) == 7
                        && RefreshContactBounties.resolveDifficulty(new CBPirate(), null) == 5,
                "Default difficulty must rise to a type's native minimum when necessary");
        check(RefreshContactBounties.resolveDifficulty(registered, 5) == 5
                        && RefreshContactBounties.resolveDifficulty(registered, 10) == 10,
                "Explicit valid native difficulties must be accepted");
        for (Integer invalid : new Integer[] {-1, 0, 4, 11, Integer.MAX_VALUE}) {
            try {
                RefreshContactBounties.refreshOffers(hub, null, new HashMap<>(), "derelict", invalid);
                throw new AssertionError("Invalid forced difficulty must be rejected: " + invalid);
            } catch (IllegalArgumentException expected) { }
            check(hub.getOfferedMissions().equals(Arrays.asList(old))
                            && !old.isMissionCreationAborted() && entity.removals == 0
                            && person.memory.values.get("$mcb_ref") == old,
                    "Preflight rejection must leave old offers, targets and native refs intact");
        }
        try {
            RefreshContactBounties.refreshOffers(hub, null, new HashMap<>(), "unknown-type", null);
            throw new AssertionError("Unknown type must be rejected");
        } catch (IllegalArgumentException expected) { }
        check(entity.removals == 0 && creator.replacement == null,
                "Unknown type must fail before cleanup or creation");
    }

    static void checkNativeForcedCreationAndGuards() {
        RegisteredDerelict.reset();
        PersonFixture person = new PersonFixture();
        BaseMissionHub hub = new BaseMissionHub(person.api);
        Creator creator = new Creator();
        EntityFixture oldEntity = new EntityFixture();
        MilitaryCustomBounty old = nativeOffer(hub, creator, oldEntity);
        ObservedMilitary customSubclass = new ObservedMilitary();
        customSubclass.setHub(hub); customSubclass.setCreator(new Creator());
        MilitaryCustomBounty accepted = new MilitaryCustomBounty();
        accepted.setHub(hub); accepted.setCreator(new Creator());
        try {
            Field stage = BaseHubMission.class.getDeclaredField("currentStage");
            stage.setAccessible(true);
            stage.set(accepted, BaseCustomBounty.Stage.BOUNTY);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot prepare native accepted-mission fixture", failure);
        }
        UnderworldCustomBounty underworld = new UnderworldCustomBounty();
        underworld.setHub(hub); underworld.setCreator(new Creator());
        NonBounty other = new NonBounty();
        hub.getOfferedMissions().addAll(Arrays.asList(other, old, customSubclass, accepted, underworld));
        List<HubMission> original = new ArrayList<>(hub.getOfferedMissions());
        int count = RefreshContactBounties.refreshOffers(hub, null, dialogMemory(person), "derelict", null);
        check(count == 1 && oldEntity.removals == 1 && old.isMissionCreationAborted(),
                "Forced refresh must clean and replace only the eligible native offer");
        check(RegisteredDerelict.difficulties.equals(Arrays.asList(5, 5, 5)),
                "Actual native create() must invoke the registered type at native default for all choices");
        verifyNativeChoices(creator.replacement, 5);
        check(person.memory.values.containsKey("$mcb_barEvent")
                        && person.memory.values.get("$mcb_ref") == creator.replacement
                        && creator.replacement.getCreator() == creator
                        && creator.getNumCompleted() == 4 && creator.getNumFailed() == 1,
                "Native creator counters/ref ownership and presentation update must survive forcing");
        for (int index : new int[] {0, 2, 3, 4}) {
            check(hub.getOfferedMissions().get(index) == original.get(index),
                    "Non-bounty, mod subclass, accepted and incompatible-family offers must remain intact");
        }
        check(!customSubclass.isMissionCreationAborted()
                        && accepted.getCurrentStage() == BaseCustomBounty.Stage.BOUNTY
                        && !underworld.isMissionCreationAborted(),
                "Forced type guards must not abort excluded missions");
    }

    static void checkMissingTargetCleanup() {
        RegisteredDerelict.reset();
        RegisteredDerelict.noEligibleTarget = true;
        PersonFixture person = new PersonFixture();
        BaseMissionHub hub = new BaseMissionHub(person.api);
        Creator creator = new Creator();
        EntityFixture entity = new EntityFixture();
        MilitaryCustomBounty old = nativeOffer(hub, creator, entity);
        hub.getOfferedMissions().add(old);
        check(RefreshContactBounties.refreshOffers(hub, null, dialogMemory(person), "derelict", 8) == 0,
                "Native missing-target failure must not claim a new offer");
        check(entity.removals == 1 && RegisteredDerelict.entities.size() == 1
                        && RegisteredDerelict.entities.get(0).removals == 1
                        && creator.replacement.isMissionCreationAborted()
                        && hub.getOfferedMissions().isEmpty()
                        && !person.memory.values.containsKey("$mcb_ref"),
                "Native abort must clean old/partial targets and the person ref when no target exists");
        RegisteredDerelict.reset();
    }

    static void checkExplicitLevelAndModFactoryGuard() {
        RegisteredDerelict.reset();
        PersonFixture person = new PersonFixture();
        BaseMissionHub hub = new BaseMissionHub(person.api);
        Creator creator = new Creator();
        EntityFixture entity = new EntityFixture();
        MilitaryCustomBounty old = nativeOffer(hub, creator, entity);
        hub.getOfferedMissions().add(old);
        creator.modReplacement = true;
        check(RefreshContactBounties.refreshOffers(hub, null, dialogMemory(person), "derelict", 8) == 0,
                "A modded factory replacement must remain outside forced native generation");
        check(hub.getOfferedMissions().equals(Arrays.asList(old)) && entity.removals == 0
                        && !old.isMissionCreationAborted()
                        && person.memory.values.get("$mcb_ref") == old
                        && RegisteredDerelict.difficulties.isEmpty(),
                "Rejecting a mod replacement must preserve the existing offered target and ref");
        creator.modReplacement = false;
        check(RefreshContactBounties.refreshOffers(hub, null, dialogMemory(person), "CB-Derelict", 8) == 1,
                "Native-ID/case/hyphen aliases must resolve an explicit valid difficulty");
        check(RegisteredDerelict.difficulties.equals(Arrays.asList(8, 8, 8)),
                "Explicit difficulty must reach every actual native creator invocation");
        verifyNativeChoices(creator.replacement, 8);
    }

    public static void main(String[] args) {
        SettingsAPI priorSettings = Global.getSettings();
        FactoryAPI priorFactory = Global.getFactory();
        SectorAPI priorSector = Global.getSector();
        List<CustomBountyCreator> priorMilitary = MilitaryCustomBounty.CREATORS;
        List<CustomBountyCreator> priorUnderworld = UnderworldCustomBounty.CREATORS;
        try {
            Global.setSettings(mock(SettingsAPI.class, (name, values) -> {
                if (name.equals("getFloat")) return 7f;
                if (name.equals("getInt")) return 3;
                if (name.equals("getColor")) return Color.WHITE;
                return null;
            }));
            Global.setFactory(mock(FactoryAPI.class, (name, values) -> {
                if (name.equals("createCargo")) return mock(CargoAPI.class, (call, arguments) -> null);
                throw new AssertionError("Unexpected campaign creation: " + name);
            }));
            MemoryFixture memory = new MemoryFixture();
            Global.setSector(mock(SectorAPI.class, (name, values) -> {
                if (name.equals("getMemoryWithoutUpdate")) return memory.api;
                throw new AssertionError("Unexpected sector mutation/access: " + name);
            }));
            RegisteredDerelict registered = new RegisteredDerelict();
            List<CustomBountyCreator> registry = new ArrayList<>(Arrays.asList(new CBPirate(), registered));
            MilitaryCustomBounty.CREATORS = registry;
            List<CustomBountyCreator> contents = new ArrayList<>(registry);
            checkResolutionAndInvalidPreflight(registered);
            checkNativeForcedCreationAndGuards();
            checkMissingTargetCleanup();
            checkExplicitLevelAndModFactoryGuard();
            check(MilitaryCustomBounty.CREATORS == registry && registry.equals(contents)
                            && UnderworldCustomBounty.CREATORS == priorUnderworld,
                    "Forcing must preserve registry identity, entries and other families");
            System.out.println("PASS: installed creator override resolution, mutation-free type/difficulty "
                    + "preflight, actual native all-three forced creation, mission-family/accepted/mod "
                    + "guards, creator counters and native missing-target cleanup; registries unchanged.");
        } finally {
            MilitaryCustomBounty.CREATORS = priorMilitary;
            UnderworldCustomBounty.CREATORS = priorUnderworld;
            Global.setSettings(priorSettings);
            Global.setFactory(priorFactory);
            Global.setSector(priorSector);
        }
    }
}
