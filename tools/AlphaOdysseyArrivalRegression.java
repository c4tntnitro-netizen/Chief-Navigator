package chiefnavigator.quest;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI;
import com.fs.starfarer.api.campaign.ai.TacticalModulePlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lwjgl.util.vector.Vector2f;

/** Entrance protection must suppress pursuit without modifying patrol populations. */
public final class AlphaOdysseyArrivalRegression {
    interface Call { Object invoke(String name, Object[] args); }

    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[] {type}, (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("hashCode")) return System.identityHashCode(proxy);
                    if (name.equals("equals")) return proxy == args[0];
                    Object value = call.invoke(name, args);
                    if (value != null) return value;
                    Class<?> returns = method.getReturnType();
                    if (returns == boolean.class) return false;
                    if (returns == int.class) return 0;
                    if (returns == float.class) return 0f;
                    if (returns == long.class) return 0L;
                    if (returns == double.class) return 0d;
                    return null;
                }));
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static String key(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return (String) field.get(null);
    }

    static final class Memory {
        final Map<String, Object> values = new HashMap<>();
        int mutations;
        final MemoryAPI api = mock(MemoryAPI.class, (name, args) -> {
            if (name.equals("getBoolean")) return Boolean.TRUE.equals(values.get(args[0]));
            if (name.equals("get") || name.equals("getString")) return values.get(args[0]);
            if (name.equals("contains")) return values.containsKey(args[0]);
            if (name.equals("set")) { values.put((String) args[0], args[1]); mutations++; }
            if (name.equals("unset")) { values.remove(args[0]); mutations++; }
            return null;
        });
        boolean flag(String key) { return Boolean.TRUE.equals(values.get(key)); }
    }

    static final class Fleet {
        final Memory memory = new Memory();
        final Vector2f location = new Vector2f(10000f, 10000f);
        final List<FleetMemberAPI> members = new ArrayList<>();
        final FleetDataAPI data = mock(FleetDataAPI.class, (name, args) -> {
            if (name.equals("getMembersListCopy")) return new ArrayList<>(members);
            if (name.equals("addFleetMember") || name.equals("removeFleetMember")) {
                throw new AssertionError("Entrance protection must not change the roster");
            }
            return null;
        });
        final FactionAPI faction = mock(FactionAPI.class, (name, args) ->
                name.equals("getId") ? Factions.THREAT : null);
        String id;
        StarSystemAPI system;
        SectorEntityToken target;
        SectorEntityToken priority;
        FleetAssignmentDataAPI assignment;
        boolean playerFlag;
        boolean empty;
        boolean station;
        boolean transitioning;
        boolean despawning;
        BattleAPI battle;
        int clearAssignments;
        int addAssignments;
        int targetWrites;
        int priorityWrites;
        int noEngagingWrites;
        int otherFleetWrites;
        float noEngaging;
        final TacticalModulePlugin tactical = mock(TacticalModulePlugin.class, (name, args) -> {
            if (name.equals("getTarget")) return target;
            if (name.equals("getPriorityTarget")) return priority;
            if (name.equals("setTarget")) { target = (SectorEntityToken) args[0]; targetWrites++; }
            if (name.equals("setPriorityTarget")) {
                priority = (SectorEntityToken) args[0]; priorityWrites++;
            }
            return null;
        });
        final ModularFleetAIAPI ai = mock(ModularFleetAIAPI.class, (name, args) -> {
            if (name.equals("getTacticalModule")) return tactical;
            if (name.equals("doNotAttack")) {
                throw new AssertionError("Protection must not leave timed AI exclusions behind");
            }
            return null;
        });
        final CampaignFleetAPI api = mock(CampaignFleetAPI.class, (name, args) -> {
            if (name.equals("getId")) return id;
            if (name.equals("getLocation")) return location;
            if (name.equals("getContainingLocation")) return system;
            if (name.equals("getMemoryWithoutUpdate")) return memory.api;
            if (name.equals("getAI")) return ai;
            if (name.equals("getFleetData")) return data;
            if (name.equals("getFaction")) return faction;
            if (name.equals("getCurrentAssignment")) return assignment;
            if (name.equals("isPlayerFleet")) return playerFlag;
            if (name.equals("isEmpty")) return empty;
            if (name.equals("isStationMode")) return station;
            if (name.equals("isInHyperspaceTransition")) return transitioning;
            if (name.equals("isDespawning")) return despawning;
            if (name.equals("getBattle")) return battle;
            if (name.equals("setNoEngaging")) {
                noEngaging = (Float) args[0]; noEngagingWrites++;
            }
            if (name.equals("clearAssignments")) { assignment = null; clearAssignments++; }
            if (name.equals("addAssignment")) {
                assignment = assignment((FleetAssignment) args[0], (SectorEntityToken) args[1]);
                addAssignments++;
            }
            if (name.equals("setLocation") || name.equals("setVelocity")
                    || name.equals("setId") || name.equals("despawn")) {
                throw new AssertionError("Entrance protection must not move or replace fleets");
            }
            if (name.startsWith("set") && !name.equals("setNoEngaging")) otherFleetWrites++;
            return null;
        });

        int mutations() {
            return memory.mutations + clearAssignments + addAssignments + targetWrites
                    + priorityWrites + noEngagingWrites + otherFleetWrites;
        }
    }

    static FleetAssignmentDataAPI assignment(FleetAssignment kind, SectorEntityToken target) {
        return mock(FleetAssignmentDataAPI.class, (name, args) -> {
            if (name.equals("getAssignment")) return kind;
            if (name.equals("getTarget")) return target;
            return null;
        });
    }

    static final class Fixture {
        final Memory memory = new Memory();
        final Memory systemMemory = new Memory();
        final Map<String, SectorEntityToken> entities = new HashMap<>();
        final List<StarSystemAPI> systems = new ArrayList<>();
        final Vector2f entryLocation = new Vector2f(1200f, 0f);
        final SectorEntityToken center = mock(SectorEntityToken.class, (name, args) -> null);
        final StarSystemAPI system = mock(StarSystemAPI.class, (name, args) -> {
            if (name.equals("getOptionalUniqueId")) return OdysseyExpanseSystem.SYSTEM_ID;
            if (name.equals("getMemoryWithoutUpdate")) return systemMemory.api;
            if (name.equals("getEntityById")) return entities.get(args[0]);
            if (name.equals("getCenter")) return center;
            if (name.equals("removeEntity") || name.equals("addEntity") || name.equals("createToken")) {
                throw new AssertionError("Entrance protection must not alter system entities");
            }
            return null;
        });
        final JumpPointAPI entry = mock(JumpPointAPI.class, (name, args) -> {
            if (name.equals("getId")) return OdysseyExpanseSystem.ENTRY_ID;
            if (name.equals("getContainingLocation")) return system;
            if (name.equals("getLocation")) return entryLocation;
            return null;
        });
        final Fleet player = new Fleet();
        final Fleet[] patrols = {new Fleet(), new Fleet()};
        SectorEntityToken interaction;
        final InteractionDialogAPI dialog = mock(InteractionDialogAPI.class, (name, args) ->
                name.equals("getInteractionTarget") ? interaction : null);
        final CampaignUIAPI ui = mock(CampaignUIAPI.class, (name, args) ->
                name.equals("getCurrentInteractionDialog") && interaction != null ? dialog : null);
        final SectorAPI sector = mock(SectorAPI.class, (name, args) -> {
            if (name.equals("getStarSystems")) return systems;
            if (name.equals("getPlayerFleet")) return player.api;
            if (name.equals("getMemoryWithoutUpdate")) return memory.api;
            if (name.equals("getCampaignUI")) return ui;
            return null;
        });

        Fixture() throws Exception {
            systems.add(system);
            systemMemory.values.put(key(OdysseyExpanseSystem.class, "AUTHORED_SYSTEM_MARKER"), true);
            entities.put(OdysseyExpanseSystem.ENTRY_ID, entry);
            player.id = "player";
            player.system = system;
            player.location.set(entryLocation);
            String prefix = key(OdysseyPredatorScript.class, "ALPHA_STARVING_PATROL_PREFIX");
            for (int index = 0; index < patrols.length; index++) {
                Fleet patrol = patrols[index];
                patrol.id = prefix + index;
                patrol.system = system;
                patrol.memory.values.put(key(OdysseyPredatorScript.class, "REGIONAL_SMALL_PATROL_MARKER"), true);
                patrol.memory.values.put(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
                patrol.memory.values.put(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
                patrol.memory.values.put(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE, true);
                patrol.memory.values.put(MemFlags.MEMORY_KEY_PURSUE_PLAYER, true);
                patrol.target = player.api;
                patrol.priority = player.api;
                patrol.assignment = assignment(FleetAssignment.INTERCEPT, player.api);
                for (int memberIndex = 0; memberIndex < 3; memberIndex++) {
                    patrol.members.add(mock(FleetMemberAPI.class, (name, args) -> null));
                }
                entities.put(patrol.id, patrol.api);
            }
            Global.setSector(sector);
        }
    }

    static void configure(Fleet fleet, StarSystemAPI system) throws Exception {
        Method method = OdysseyPredatorScript.class.getDeclaredMethod("configureRegionalStarvingPatrol",
                CampaignFleetAPI.class, StarSystemAPI.class, String.class);
        method.setAccessible(true);
        try { method.invoke(null, fleet.api, system, "ordinary regional patrol"); }
        catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof Error) throw (Error) failure.getCause();
            if (failure.getCause() instanceof Exception) throw (Exception) failure.getCause();
            throw failure;
        }
    }

    static void checkProtected(Fleet fleet, Fixture fixture) {
        check(!fleet.memory.flag(MemFlags.MEMORY_KEY_MAKE_HOSTILE), "Hostility must be disabled at the entrance");
        check(!fleet.memory.flag(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE), "Aggression must be disabled at the entrance");
        check(fleet.memory.flag(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE), "Native non-hostile override is required");
        check(fleet.memory.flag(MemFlags.NON_HOSTILE_OVERRIDES_MAKE_HOSTILE), "Override must win over faction hostility");
        check(fleet.memory.flag(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE), "Native non-aggressive override is required");
        check(!fleet.memory.flag(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE)
                        && !fleet.memory.flag(MemFlags.MEMORY_KEY_PURSUE_PLAYER), "Legacy pursuit flags must be cleared");
        check(fleet.target == null && fleet.priority == null, "Stale player tactical targets must be cleared");
        check(fleet.noEngaging == 1f, "Native engagement inhibition must be refreshed");
        check(fleet.assignment.getAssignment() == FleetAssignment.PATROL_SYSTEM
                        && fleet.assignment.getTarget() == fixture.center, "Player interception must become ordinary patrol");
    }

    static void pendingAndSpatialProtection() throws Exception {
        Fixture f = new Fixture();
        f.player.system = null; // Transit has not placed the player in the destination yet.
        OdysseyPredatorScript.prepareTroyArrival(f.system);
        check(f.memory.flag(key(OdysseyPredatorScript.class, "TROY_TRANSIT_PENDING")), "Prepare must arm pending transit protection");
        for (Fleet patrol : f.patrols) checkProtected(patrol, f);
        f.memory.values.clear();
        f.player.system = f.system;
        f.player.location.set(f.entryLocation.x + 4500f, f.entryLocation.y);
        for (Fleet patrol : f.patrols) {
            check(OdysseyPredatorScript.protectAlphaStarvingArrival(patrol.api, f.system), "4500-unit boundary must be protected");
            int assignments = patrol.addAssignments;
            int clears = patrol.clearAssignments;
            int targets = patrol.targetWrites + patrol.priorityWrites;
            for (int tick = 0; tick < 1000; tick++) {
                check(OdysseyPredatorScript.protectAlphaStarvingArrival(patrol.api, f.system), "Entrance protection must persist while nearby");
            }
            check(patrol.addAssignments == assignments && patrol.clearAssignments == clears,
                    "Stable patrol assignments must not be reset each update");
            check(patrol.targetWrites + patrol.priorityWrites == targets,
                    "Already-cleared tactical targets must not be reset each update");
            check(patrol.location.equals(new Vector2f(10000f, 10000f)), "Patrol position must be preserved");
        }
        f.player.location.x += 1f;
        for (Fleet patrol : f.patrols) {
            configure(patrol, f.system);
            check(patrol.memory.flag(MemFlags.MEMORY_KEY_MAKE_HOSTILE)
                            && patrol.memory.flag(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE), "Normal regional hostility must return outside entrance protection");
            check(!patrol.memory.flag(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE)
                            && !patrol.memory.flag(MemFlags.NON_HOSTILE_OVERRIDES_MAKE_HOSTILE)
                            && !patrol.memory.flag(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE), "Temporary protection overrides must not leak");
            check(patrol.members.size() == 3, "The regional patrol's original roster must remain intact");
        }
        check(f.entities.size() == 3, "Exactly the original entrance and two patrols must remain");
    }

    static void preserveOtherTargets() throws Exception {
        Fixture f = new Fixture();
        Fleet patrol = f.patrols[0];
        SectorEntityToken other = mock(SectorEntityToken.class, (name, args) -> null);
        patrol.target = other;
        patrol.priority = other;
        FleetAssignmentDataAPI original = assignment(FleetAssignment.PATROL_SYSTEM, f.center);
        patrol.assignment = original;
        check(OdysseyPredatorScript.protectAlphaStarvingArrival(patrol.api, f.system), "Owned entrance patrol must be protected");
        check(patrol.target == other && patrol.priority == other, "Unrelated tactical targets must remain unchanged");
        check(patrol.assignment == original && patrol.clearAssignments == 0 && patrol.addAssignments == 0,
                "Non-player assignment must remain unchanged");
    }

    static void exclusions() throws Exception {
        for (String reason : Arrays.asList("playerFlag", "playerIdentity", "wrongSlot", "paddedSlot", "wrongPrefix", "missingMarker",
                "otherLocation", "otherSystem", "empty", "station", "battle", "transition", "despawn", "dialog")) {
            Fixture f = new Fixture();
            Fleet patrol = f.patrols[0];
            StarSystemAPI requestedSystem = f.system;
            if (reason.equals("playerFlag")) patrol.playerFlag = true;
            if (reason.equals("playerIdentity")) {
                patrol = f.player;
                patrol.id = f.patrols[0].id;
                patrol.memory.values.put(key(OdysseyPredatorScript.class, "REGIONAL_SMALL_PATROL_MARKER"), true);
            }
            if (reason.equals("wrongSlot")) patrol.id = key(OdysseyPredatorScript.class, "ALPHA_STARVING_PATROL_PREFIX") + "2";
            if (reason.equals("paddedSlot")) patrol.id = key(OdysseyPredatorScript.class, "ALPHA_STARVING_PATROL_PREFIX") + "00";
            if (reason.equals("wrongPrefix")) patrol.id = key(OdysseyPredatorScript.class, "SANZU_STARVING_PATROL_PREFIX") + "0";
            if (reason.equals("missingMarker")) patrol.memory.values.clear();
            if (reason.equals("otherLocation")) patrol.system = null;
            if (reason.equals("otherSystem")) {
                requestedSystem = mock(StarSystemAPI.class, (name, args) -> null);
                patrol.system = requestedSystem;
            }
            if (reason.equals("empty")) patrol.empty = true;
            if (reason.equals("station")) patrol.station = true;
            if (reason.equals("battle")) patrol.battle = mock(BattleAPI.class, (name, args) -> null);
            if (reason.equals("transition")) patrol.transitioning = true;
            if (reason.equals("despawn")) patrol.despawning = true;
            if (reason.equals("dialog")) f.interaction = patrol.api;
            int before = patrol.mutations();
            check(!OdysseyPredatorScript.protectAlphaStarvingArrival(patrol.api, requestedSystem), "Must exclude " + reason);
            check(patrol.mutations() == before, "Excluded fleet must remain unchanged: " + reason);
        }
    }

    static void otherRegionalPatrolsRemainHostile() throws Exception {
        Fixture f = new Fixture();
        Memory regionalMemory = new Memory();
        SectorEntityToken regionalCenter = mock(SectorEntityToken.class, (name, args) -> null);
        regionalMemory.values.put(key(OdysseyExpanseSystem.class, "AUTHORED_SYSTEM_MARKER"), true);
        StarSystemAPI region = mock(StarSystemAPI.class, (name, args) -> {
            if (name.equals("getOptionalUniqueId")) return OdysseyExpanseSystem.SILENT_WAKE_ID;
            if (name.equals("getMemoryWithoutUpdate")) return regionalMemory.api;
            if (name.equals("getCenter")) return regionalCenter;
            return null;
        });
        f.systems.add(region);
        Fleet patrol = f.patrols[0];
        patrol.id = key(OdysseyPredatorScript.class, "SANZU_STARVING_PATROL_PREFIX") + "0";
        patrol.system = region;
        FleetAssignmentDataAPI original = patrol.assignment;
        configure(patrol, region);
        check(patrol.memory.flag(MemFlags.MEMORY_KEY_MAKE_HOSTILE)
                        && patrol.memory.flag(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE),
                "Sanzu patrols must remain ordinarily hostile");
        check(patrol.assignment == original && patrol.target == f.player.api
                        && patrol.priority == f.player.api,
                "Foxtrot protection must not clear Sanzu pursuit");
        check(patrol.noEngagingWrites == 0 && patrol.members.size() == 3,
                "Sanzu engagement and rosters must remain intact");
    }

    static void sourceContracts() throws Exception {
        String rules = Files.readString(Paths.get("data/campaign/rules.csv"),
                StandardCharsets.UTF_8);
        String guidanceNotice = "You can ask Sinni for guidance or open her custom "
                + "starscape map of the Orion Knot through Contacts. Her map is also available in the "
                + "Adept of the Stars and Waves Intel entry.";
        int noticeStart = rules.indexOf("chiefNavigatorSinniAlphaOdysseyMapNotice,");
        int noticeEnd = rules.indexOf("chiefNavigatorSinniVignetteNebula,", noticeStart);
        check(noticeStart >= 0 && noticeEnd > noticeStart,
                "The existing first-entry map notice must remain in the arrival flow");
        String notice = rules.substring(noticeStart, noticeEnd);
        check(notice.contains("SetTextHighlightColors hColor")
                        && notice.contains("SetTextHighlights \"\"" + guidanceNotice + "\"\"")
                        && notice.contains("100:complete_alpha_odyssey:Continue."),
                "First-entry guidance/map notice must be highlighted and preserve completion");
        for (String file : Arrays.asList("TroyWormholeInteraction.java", "GoalStationInteraction.java")) {
            String source = Files.readString(Paths.get("src/chiefnavigator/quest", file), StandardCharsets.UTF_8);
            int prepare = source.indexOf("OdysseyPredatorScript.prepareTroyArrival(expanse)");
            int transition = source.indexOf("doHyperspaceTransition(");
            check(prepare >= 0 && prepare < transition, "Both entrances must protect before transit: " + file);
            check(source.contains("SinniSystemVignetteScript.armAlphaOdysseyArrival()"), "Authored arrival must remain armed: " + file);
        }
        Field count = OdysseyPredatorScript.class.getDeclaredField("ALPHA_STARVING_PATROL_COUNT");
        count.setAccessible(true);
        check(count.getInt(null) == 2, "The ordinary regional patrol count must remain two");
        Field sanzuCount = OdysseyPredatorScript.class.getDeclaredField("SANZU_STARVING_PATROL_COUNT");
        sanzuCount.setAccessible(true);
        check(sanzuCount.getInt(null) == 2, "Sanzu must retain its two ordinary regional patrols");
        String source = Files.readString(Paths.get("src/chiefnavigator/quest/OdysseyPredatorScript.java"), StandardCharsets.UTF_8);
        check(!source.contains("AVICI_STARVING_PATROL_"),
                "Avici must no longer define or spawn renewable Starving Threat patrols");
        int populationStart = source.indexOf("private static void ensureStarvingPopulation()");
        int populationEnd = source.indexOf("private static void ensureMonthlyIthacaStrike()", populationStart);
        check(populationStart >= 0 && populationEnd > populationStart
                        && source.substring(populationStart, populationEnd).contains("retireAviciStarvingThreat(")
                        && !source.substring(populationStart, populationEnd).contains("hunting through Avici"),
                "Ordinary population maintenance must retire Avici Starving Threat instead of spawning them");
        int start = source.indexOf("static boolean protectAlphaStarvingArrival(");
        int end = source.indexOf("private static void assignRogueDerelictAnchor(", start);
        String helper = source.substring(start, end);
        for (String forbidden : Arrays.asList("removeEntity(", "addEntity(", "setLocation(", "createEmptyFleet(", "addMember(", "doNotAttack(")) {
            check(!helper.contains(forbidden), "Entrance helper must remain non-destructive: " + forbidden);
        }
    }

    public static void main(String[] args) throws Exception {
        SectorAPI previousSector = Global.getSector();
        SettingsAPI previousSettings = Global.getSettings();
        FactoryAPI previousFactory = Global.getFactory();
        try {
            Global.setSettings(mock(SettingsAPI.class, (name, values) ->
                    name.equals("getColor") ? Color.WHITE : null));
            Global.setFactory(mock(FactoryAPI.class, (name, values) -> {
                throw new AssertionError("Entrance protection must not invoke factories: " + name);
            }));
            pendingAndSpatialProtection();
            preserveOtherTargets();
            exclusions();
            otherRegionalPatrolsRemainHostile();
            sourceContracts();
            System.out.println("Alpha Odyssey entrance protection regression: PASS");
        } finally {
            Global.setSector(previousSector);
            Global.setSettings(previousSettings);
            Global.setFactory(previousFactory);
        }
    }
}
