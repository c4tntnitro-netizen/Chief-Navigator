package chiefnavigator.quest;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PersistentUIDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lwjgl.util.vector.Vector2f;

/** Headless checks for the user-authorized duplicate Alpha jump repair. */
public final class OdysseyDuplicateJumpRegression {
    interface Call { Object invoke(String name, Object[] arguments); }

    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[] {type}, (proxy, method, args) -> {
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

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static String key(String name) throws Exception {
        Field field = OdysseyExpanseSystem.class.getDeclaredField(name);
        field.setAccessible(true);
        return (String) field.get(null);
    }

    static final class Memory {
        final Map<String, Object> values = new HashMap<>();
        int writes;
        final MemoryAPI api = mock(MemoryAPI.class, (name, args) -> {
            if (name.equals("getBoolean")) return Boolean.TRUE.equals(values.get(args[0]));
            if (name.equals("get") || name.equals("getString")) return values.get(args[0]);
            if (name.equals("contains")) return values.containsKey(args[0]);
            if (name.equals("set")) { values.put((String) args[0], args[1]); writes++; }
            if (name.equals("unset")) { values.remove(args[0]); writes++; }
            return null;
        });
    }

    static final class World {
        final List<SectorEntityToken> entities = new ArrayList<>();
        final List<SectorEntityToken> removed = new ArrayList<>();
        final Memory memory = new Memory();
        final Vector2f position = new Vector2f(149000f, -92000f);
        final String systemId;
        final LocationAPI api;
        boolean hyperspace;

        World(String systemId) {
            this.systemId = systemId;
            hyperspace = systemId == null;
            api = systemId == null ? mock(LocationAPI.class, this::invoke)
                    : mock(StarSystemAPI.class, this::invoke);
        }

        Object invoke(String name, Object[] args) {
            if (name.equals("getOptionalUniqueId") || name.equals("getId")) return systemId;
            if (name.equals("getName")) return systemId == null ? "Hyperspace" : "Odyssey Expanse";
            if (name.equals("getMemoryWithoutUpdate")) return memory.api;
            if (name.equals("getLocation")) return position;
            if (name.equals("isHyperspace")) return hyperspace;
            if (name.equals("getAllEntities")) return new ArrayList<>(entities);
            if (name.equals("getJumpPoints")) {
                List<SectorEntityToken> jumps = new ArrayList<>();
                for (SectorEntityToken entity : entities) {
                    if (entity instanceof JumpPointAPI) jumps.add(entity);
                }
                return jumps;
            }
            if (name.equals("getEntityById")) {
                for (SectorEntityToken entity : entities) {
                    if (args[0].equals(entity.getId())) return entity;
                }
                return null;
            }
            if (name.equals("removeEntity")) {
                SectorEntityToken entity = (SectorEntityToken) args[0];
                check(entities.remove(entity), "Repair attempted to remove an absent token");
                removed.add(entity);
            }
            if (name.startsWith("getAutogenerated")) {
                throw new AssertionError("Repair must not consult transient auto-generation collections");
            }
            if (name.startsWith("set") || name.startsWith("auto")
                    || name.startsWith("create") || name.equals("addEntity")
                    || name.equals("generateAnchorIfNeeded")) {
                throw new AssertionError("Repair must not construct or relocate topology: " + name);
            }
            return null;
        }

        StarSystemAPI system() { return (StarSystemAPI) api; }
    }

    static final class Destinations extends ArrayList<JumpPointAPI.JumpDestination> {
        int writes;
        @Override public boolean add(JumpPointAPI.JumpDestination value) {
            writes++;
            return super.add(value);
        }
        @Override public JumpPointAPI.JumpDestination remove(int index) {
            writes++;
            return super.remove(index);
        }
        @Override public boolean remove(Object value) {
            writes++;
            return super.remove(value);
        }
        @Override public void clear() {
            writes++;
            super.clear();
        }
    }

    static final class Jump {
        final String id;
        final String label;
        final Vector2f position;
        final World world;
        final Destinations destinations = new Destinations();
        final Memory memory = new Memory();
        int destinationWrites;
        boolean wormhole;
        final JumpPointAPI api;

        Jump(World world, String id, String label, float x, float y) {
            this.world = world;
            this.id = id;
            this.label = label;
            position = new Vector2f(x, y);
            api = mock(JumpPointAPI.class, this::invoke);
            world.entities.add(api);
        }

        Object invoke(String name, Object[] args) {
            if (name.equals("getId")) return id;
            if (name.equals("getName")) return label;
            if (name.equals("getLocation")) return position;
            if (name.equals("getContainingLocation")) return world.api;
            if (name.equals("getMemoryWithoutUpdate")) return memory.api;
            if (name.equals("isWormhole")) return wormhole;
            // Native JumpPoint returns its actual destination list.
            if (name.equals("getDestinations")) return destinations;
            if (name.equals("clearDestinations")) { destinations.clear(); destinationWrites++; }
            if (name.equals("addDestination")) {
                destinations.add((JumpPointAPI.JumpDestination) args[0]); destinationWrites++;
            }
            if (name.equals("removeDestination")) {
                destinations.removeIf(destination -> destination.getDestination() == args[0]);
                destinationWrites++;
            }
            if (name.startsWith("set") || name.startsWith("auto")
                    || name.equals("forceOpen") || name.equals("forceClose")) {
                throw new AssertionError("Repair must retain token identity and presentation: " + name);
            }
            return null;
        }

        JumpPointAPI.JumpDestination link(Jump target) {
            JumpPointAPI.JumpDestination destination = new JumpPointAPI.JumpDestination(
                    target.api, target.world.systemId == null ? "Hyperspace" : target.label);
            destination.setMinDistFromToken(37f);
            destination.setMaxDistFromToken(83f);
            destinations.add(destination);
            return destination;
        }
    }

    static final class Fixture {
        final Memory sectorMemory = new Memory();
        final World expanse = new World(OdysseyExpanseSystem.SYSTEM_ID);
        final World other = new World(OdysseyExpanseSystem.ASHEN_VERGE_ID);
        final World foreign = new World("third_party_system");
        final World hyper = new World(null);
        final List<StarSystemAPI> systems = new ArrayList<>();
        final List<Jump> jumps = new ArrayList<>();
        SectorEntityToken interactionTarget;
        SectorEntityToken courseTarget;
        SectorEntityToken ultimateCourseTarget;
        SectorEntityToken persistedCourseTarget;
        boolean playerTransition;
        BattleAPI playerBattle;
        final InteractionDialogAPI dialog = mock(InteractionDialogAPI.class, (name, args) ->
                name.equals("getInteractionTarget") ? interactionTarget : null);
        final CampaignUIAPI ui = mock(CampaignUIAPI.class, (name, args) -> {
            if (name.equals("getCurrentInteractionDialog") && interactionTarget != null) return dialog;
            if (name.equals("getCurrentCourseTarget")) return courseTarget;
            if (name.equals("getUltimateCourseTarget")) return ultimateCourseTarget;
            if (name.equals("clearLaidInCourse") || name.startsWith("layInCourse")) {
                throw new AssertionError("Repair must not rewrite the player's course");
            }
            return null;
        });
        final PersistentUIDataAPI uiData = mock(PersistentUIDataAPI.class, (name, args) ->
                name.equals("getCourseTarget") ? persistedCourseTarget : null);
        final CampaignFleetAPI player = mock(CampaignFleetAPI.class, (name, args) -> {
            if (name.equals("isPlayerFleet")) return true;
            if (name.equals("isInHyperspaceTransition")) return playerTransition;
            if (name.equals("getBattle")) return playerBattle;
            if (name.equals("getContainingLocation")) return hyper.api;
            if (name.equals("setLocation") || name.equals("despawn")) {
                throw new AssertionError("Repair must never mutate the player's fleet");
            }
            return null;
        });
        final SectorAPI sector = mock(SectorAPI.class, (name, args) -> {
            if (name.equals("getHyperspace")) return hyper.api;
            if (name.equals("getStarSystems")) return systems;
            if (name.equals("getAllLocations")) {
                List<LocationAPI> locations = new ArrayList<>(systems);
                locations.add(hyper.api);
                return locations;
            }
            if (name.equals("getMemoryWithoutUpdate")) return sectorMemory.api;
            if (name.equals("getCampaignUI")) return ui;
            if (name.equals("getUIData")) return uiData;
            if (name.equals("getPlayerFleet")) return player;
            return null;
        });

        Fixture() throws Exception {
            systems.add(expanse.system());
            systems.add(other.system());
            systems.add(foreign.system());
            expanse.memory.values.put(key("AUTHORED_SYSTEM_MARKER"), true);
            other.memory.values.put(key("AUTHORED_SYSTEM_MARKER"), true);
            Global.setSector(sector);
            Global.setFactory(mock(FactoryAPI.class, (name, args) -> {
                throw new AssertionError("Duplicate repair must never call factories: " + name);
            }));
        }

        Jump jump(World world, String id, float x, float y) {
            Jump jump = new Jump(world, id, "Fringe Jump-point", x, y);
            jumps.add(jump);
            return jump;
        }

        int writes() {
            int total = expanse.memory.writes + other.memory.writes
                    + foreign.memory.writes + hyper.memory.writes + sectorMemory.writes
                    + expanse.removed.size() + other.removed.size()
                    + foreign.removed.size() + hyper.removed.size();
            for (Jump jump : jumps) total += jump.destinationWrites
                    + jump.destinations.writes + jump.memory.writes;
            return total;
        }
    }

    static final class Pair {
        final Fixture fixture;
        final Jump local;
        final Jump first;
        final Jump redundant;
        final JumpPointAPI.JumpDestination retainedRecord;
        final JumpPointAPI.JumpDestination retainedReciprocal;

        Pair() throws Exception {
            fixture = new Fixture();
            local = fixture.jump(fixture.expanse, "9a91", 5472.772f, -639.3867f);
            first = fixture.jump(fixture.hyper, "9a96", 149789.17f, -92622.01f);
            redundant = fixture.jump(fixture.hyper, "9b3c", 149928.48f, -92638.21f);
            retainedRecord = local.link(first);
            local.link(redundant);
            retainedReciprocal = first.link(local);
            redundant.link(local);
        }

        int repair() {
            return AlphaOdysseyJumpRepair.repairDuplicates(
                    fixture.expanse.system(), fixture.hyper.api);
        }
    }

    static void checkRouteUntouched(Jump jump,
            List<JumpPointAPI.JumpDestination> records, String message) {
        check(jump.destinations.size() == records.size(), message + ": destination count changed");
        for (int index = 0; index < records.size(); index++) {
            check(jump.destinations.get(index) == records.get(index),
                    message + ": existing destination record replaced");
        }
        check(jump.destinationWrites == 0, message + ": API destination writes occurred");
    }

    static void checkNoRepair(Pair pair, String message) {
        int writes = pair.fixture.writes();
        List<JumpPointAPI.JumpDestination> records = new ArrayList<>(pair.local.destinations);
        List<JumpPointAPI.JumpDestination> reciprocal = new ArrayList<>(pair.redundant.destinations);
        check(pair.repair() == 0, message + ": unexpected endpoint deletion");
        checkRouteUntouched(pair.local, records, message);
        checkRouteUntouched(pair.redundant, reciprocal, message);
        check(pair.fixture.writes() == writes, message + ": unrelated mutations occurred");
        check(pair.fixture.hyper.entities.contains(pair.first.api)
                        && pair.fixture.hyper.entities.contains(pair.redundant.api),
                message + ": endpoint disappeared");
    }

    static void savedFailure() throws Exception {
        Pair pair = new Pair();
        Fixture fixture = pair.fixture;
        List<Jump> unrelated = new ArrayList<>();
        String[] localIds = {
            key("EXPANSE_LOCAL_JUMP_ID"),
            key("CLUSTER_SECONDARY_LOCAL_PREFIX") + OdysseyExpanseSystem.SYSTEM_ID,
            key("CLUSTER_TERTIARY_LOCAL_PREFIX") + OdysseyExpanseSystem.SYSTEM_ID
        };
        for (int index = 0; index < localIds.length; index++) {
            Jump local = fixture.jump(fixture.expanse, localIds[index], index * 1500f, 900f);
            Jump hyper = fixture.jump(fixture.hyper,
                    OdysseyExpanseSystem.getAuthoredHyperspaceEndpointId(
                            OdysseyExpanseSystem.SYSTEM_ID, index),
                    147000f + index * 1300f, -90400f);
            local.link(hyper);
            hyper.link(local);
            unrelated.add(local);
            unrelated.add(hyper);
        }
        Jump foxtrot = fixture.jump(fixture.expanse, OdysseyExpanseSystem.ENTRY_ID, 250f, 350f);
        Jump foxtrotFirst = fixture.jump(fixture.hyper, "foxtrot-hyper-one", 120f, 130f);
        Jump foxtrotSecond = fixture.jump(fixture.hyper, "foxtrot-hyper-two", 140f, 150f);
        foxtrot.link(foxtrotFirst);
        foxtrot.link(foxtrotSecond);
        foxtrotFirst.link(foxtrot);
        foxtrotSecond.link(foxtrot);
        unrelated.add(foxtrot);
        unrelated.add(foxtrotFirst);
        unrelated.add(foxtrotSecond);
        Jump foreign = fixture.jump(fixture.foreign, "foreign_jump", -500f, 600f);
        Jump foreignFirst = fixture.jump(fixture.hyper, "foreign_hyper_one", 200f, 400f);
        Jump foreignSecond = fixture.jump(fixture.hyper, "foreign_hyper_two", 200f, 405f);
        foreign.link(foreignFirst);
        foreign.link(foreignSecond);
        foreignFirst.link(foreign);
        foreignSecond.link(foreign);
        unrelated.add(foreign);
        unrelated.add(foreignFirst);
        unrelated.add(foreignSecond);
        Map<Jump, List<JumpPointAPI.JumpDestination>> snapshots = new HashMap<>();
        for (Jump jump : unrelated) snapshots.put(jump, new ArrayList<>(jump.destinations));
        int oldCount = fixture.hyper.entities.size();

        check(pair.repair() == 1, "Saved duplicate Fringe route must remove exactly one extra endpoint");
        check(pair.local.destinations.size() == 1
                        && pair.local.destinations.get(0) == pair.retainedRecord
                        && pair.retainedRecord.getDestination() == pair.first.api,
                "Keep the first existing destination record and endpoint identity");
        check(pair.retainedRecord.getMinDistFromToken() == 37f
                        && pair.retainedRecord.getMaxDistFromToken() == 83f,
                "Retained destination arrival parameters must not change");
        check(pair.first.destinations.size() == 1
                        && pair.first.destinations.get(0) == pair.retainedReciprocal,
                "Retained reciprocal record must remain identical");
        check(fixture.hyper.entities.size() == oldCount - 1
                        && fixture.hyper.entities.contains(pair.first.api)
                        && !fixture.hyper.entities.contains(pair.redundant.api)
                        && fixture.hyper.removed.size() == 1
                        && fixture.hyper.removed.get(0) == pair.redundant.api,
                "Remove only the positively identified redundant hyperspace token");
        check(pair.first.position.x == 149789.17f && pair.first.position.y == -92622.01f
                        && pair.local.position.x == 5472.772f && pair.local.position.y == -639.3867f,
                "Saved endpoint positions must remain unchanged");
        for (Jump jump : unrelated) checkRouteUntouched(jump, snapshots.get(jump),
                "Foxtrot, authored, and foreign routes must remain untouched");
        int writes = fixture.writes();
        for (int repeat = 0; repeat < 1000; repeat++) {
            check(pair.repair() == 0, "Repeated repair must be a no-op");
        }
        check(fixture.writes() == writes && pair.local.destinations.get(0) == pair.retainedRecord,
                "Repeated repair must not rewrite state or destination records");
    }

    static void sameTokenRecords() throws Exception {
        Fixture fixture = new Fixture();
        Jump local = fixture.jump(fixture.expanse, "native_local", 150f, 230f);
        Jump hyper = fixture.jump(fixture.hyper, "native_hyper", 200f, 400f);
        JumpPointAPI.JumpDestination retained = local.link(hyper);
        local.link(hyper);
        hyper.link(local);
        int entities = fixture.hyper.entities.size();
        check(AlphaOdysseyJumpRepair.repairDuplicates(fixture.expanse.system(), fixture.hyper.api) == 0,
                "Duplicate records to the same endpoint must not count as token deletions");
        check(local.destinations.size() == 1 && local.destinations.get(0) == retained,
                "Duplicate records must retain the first exact record");
        check(fixture.hyper.entities.size() == entities && fixture.hyper.removed.isEmpty(),
                "Never delete the retained endpoint when two records target the same token");
    }

    static void conservativeGuards() throws Exception {
        Pair unowned = new Pair();
        unowned.fixture.expanse.memory.values.clear();
        checkNoRepair(unowned, "Unowned Alpha ID claimant");
        Pair far = new Pair();
        far.redundant.position.set(far.first.position.x + 250.25f, far.first.position.y);
        checkNoRepair(far, "Nonadjacent valid routes");
        Pair mixed = new Pair();
        Jump realspace = mixed.fixture.jump(mixed.fixture.other, "other_local", 80f, 90f);
        mixed.local.link(realspace);
        checkNoRepair(mixed, "Mixed hyperspace and realspace destinations");
        Pair reciprocal = new Pair();
        Jump foreignTarget = reciprocal.fixture.jump(reciprocal.fixture.foreign, "foreign_target", 40f, 30f);
        reciprocal.redundant.link(foreignTarget);
        checkNoRepair(reciprocal, "Endpoint with another destination");
        Pair wormhole = new Pair();
        wormhole.local.wormhole = true;
        checkNoRepair(wormhole, "Nonordinary wormhole");
        Pair wrongHyper = new Pair();
        wrongHyper.fixture.hyper.hyperspace = false;
        checkNoRepair(wrongHyper, "Nonhyperspace location");
        Pair shared = new Pair();
        Jump incoming = shared.fixture.jump(shared.fixture.foreign, "shared_incoming", 400f, 500f);
        incoming.link(shared.redundant);
        checkNoRepair(shared, "Redundant endpoint shared with a foreign route");
        Pair hyperShared = new Pair();
        Jump hyperIncoming = hyperShared.fixture.jump(hyperShared.fixture.hyper, "shared_hyper", 900f, 800f);
        hyperIncoming.link(hyperShared.redundant);
        checkNoRepair(hyperShared, "Redundant endpoint shared with another hyperspace route");
        Pair malformed = new Pair();
        malformed.local.destinations.add(null);
        checkNoRepair(malformed, "Malformed destination list");
        Pair notReciprocal = new Pair();
        notReciprocal.redundant.destinations.clear();
        int unpairedWrites = notReciprocal.fixture.writes();
        List<JumpPointAPI.JumpDestination> unpairedRecords = new ArrayList<>(notReciprocal.local.destinations);
        check(notReciprocal.repair() == 0
                        && notReciprocal.fixture.writes() == unpairedWrites
                        && notReciprocal.local.destinations.equals(unpairedRecords),
                "Nonreciprocal extra endpoint must fail closed");
        Pair missing = new Pair();
        missing.fixture.hyper.entities.remove(missing.redundant.api);
        int writes = missing.fixture.writes();
        check(missing.repair() == 0 && missing.local.destinations.size() == 2
                        && missing.fixture.writes() == writes,
                "Missing serialized endpoint must fail closed without reconstruction");
        Pair wrongSystem = new Pair();
        check(AlphaOdysseyJumpRepair.repairDuplicates(wrongSystem.fixture.other.system(),
                        wrongSystem.fixture.hyper.api) == 0
                        && wrongSystem.local.destinations.size() == 2,
                "Other authored systems are outside this repair");
        Pair absent = new Pair();
        check(AlphaOdysseyJumpRepair.repairDuplicates(null, absent.fixture.hyper.api) == 0
                        && AlphaOdysseyJumpRepair.repairDuplicates(absent.fixture.expanse.system(), null) == 0,
                "Null topology must fail closed");
    }

    static void busyGuards() throws Exception {
        Pair transition = new Pair();
        transition.fixture.playerTransition = true;
        checkNoRepair(transition, "Player in hyperspace transition");
        Pair battle = new Pair();
        battle.fixture.playerBattle = mock(BattleAPI.class, (name, args) -> null);
        checkNoRepair(battle, "Player in battle");
        Pair localDialog = new Pair();
        localDialog.fixture.interactionTarget = localDialog.local.api;
        checkNoRepair(localDialog, "Open local jump interaction");
        Pair retainedDialog = new Pair();
        retainedDialog.fixture.interactionTarget = retainedDialog.first.api;
        checkNoRepair(retainedDialog, "Open retained hyperspace interaction");
        Pair extraDialog = new Pair();
        extraDialog.fixture.interactionTarget = extraDialog.redundant.api;
        checkNoRepair(extraDialog, "Open redundant hyperspace interaction");
        Pair course = new Pair();
        course.fixture.courseTarget = course.redundant.api;
        checkNoRepair(course, "Player current autopilot step targets redundant endpoint");
        Pair ultimateCourse = new Pair();
        ultimateCourse.fixture.ultimateCourseTarget = ultimateCourse.redundant.api;
        checkNoRepair(ultimateCourse, "Player ultimate course targets redundant endpoint");
        Pair persistedCourse = new Pair();
        persistedCourse.fixture.persistedCourseTarget = persistedCourse.redundant.api;
        checkNoRepair(persistedCourse, "Player persisted course targets redundant endpoint");
        Pair keptCourse = new Pair();
        keptCourse.fixture.courseTarget = keptCourse.first.api;
        check(keptCourse.repair() == 1 && keptCourse.fixture.courseTarget == keptCourse.first.api,
                "Repair may preserve a course to the retained endpoint without rewriting it");
    }

    static void boundaryAndLegacyOwnership() throws Exception {
        Pair boundary = new Pair();
        boundary.redundant.position.set(boundary.first.position.x + 250f, boundary.first.position.y);
        check(boundary.repair() == 1, "250-unit adjacency boundary is inclusive");
        Pair legacy = new Pair();
        legacy.fixture.expanse.memory.values.clear();
        legacy.fixture.expanse.memory.values.put(key("GENERATED_NEBULA"), true);
        check(legacy.repair() == 1, "Legacy owned Alpha generation marker remains accepted");
    }

    static void publicEntryPoint() throws Exception {
        Pair owned = new Pair();
        AlphaOdysseyJumpRepair.repairDuplicates();
        check(owned.local.destinations.size() == 1
                        && owned.fixture.hyper.removed.size() == 1
                        && owned.local.destinations.get(0) == owned.retainedRecord,
                "Load-time entry point must discover and repair the existing owned Alpha");
        Pair unowned = new Pair();
        unowned.fixture.expanse.memory.values.clear();
        AlphaOdysseyJumpRepair.repairDuplicates();
        check(unowned.local.destinations.size() == 2 && unowned.fixture.hyper.removed.isEmpty(),
                "Load-time entry point must not repair an unowned Alpha ID claimant");
        Global.setSector(null);
        AlphaOdysseyJumpRepair.repairDuplicates();
    }

    public static void main(String[] args) throws Exception {
        Global.setSettings(mock(SettingsAPI.class, (name, arguments) -> {
            if (name.equals("getFloat")) return 1f;
            if (name.equals("getColor")) return Color.WHITE;
            return null;
        }));
        savedFailure();
        sameTokenRecords();
        conservativeGuards();
        busyGuards();
        boundaryAndLegacyOwnership();
        publicEntryPoint();
        System.out.println("Alpha Odyssey duplicate jump-route regression passed.");
    }
}
