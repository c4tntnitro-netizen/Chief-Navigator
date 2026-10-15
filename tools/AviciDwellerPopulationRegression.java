package chiefnavigator.quest;

import chiefnavigator.dwellers.AviciDwellerContent;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import java.awt.Color;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;

/** Exercises actual campaign creation, persisted clocks, and the scoped population controller. */
public final class AviciDwellerPopulationRegression {
    private static final String SPAWNED = "$chief_navigator_budai_spawned";
    private static final String HUNTER = "$chief_navigator_budai_hunter";
    private static final String COMPLETE = "$chief_navigator_sinni_vignette_complete_avici";
    private interface Call { Object invoke(String name, Object[] args); }

    private static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (proxy, method, args) -> {
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == args[0];
                    Object value = call.invoke(method.getName(), args);
                    if (value != null) return value;
                    Class<?> result = method.getReturnType();
                    if (result == boolean.class) return false;
                    if (result == int.class) return 0;
                    if (result == long.class) return 0L;
                    if (result == float.class) return 0f;
                    if (result == double.class) return 0d;
                    return null;
                }));
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static final class Memory {
        final Map<String, Object> values = new HashMap<>();
        final MemoryAPI api = mock(MemoryAPI.class, (name, args) -> {
            Object value = args == null || args.length == 0 ? null : values.get(args[0]);
            switch (name) {
                case "get": return value;
                case "getBoolean": return Boolean.TRUE.equals(value);
                case "getInt": return value instanceof Number ? ((Number) value).intValue() : 0;
                case "getLong": return value instanceof Number ? ((Number) value).longValue() : 0L;
                case "getFloat": return value instanceof Number ? ((Number) value).floatValue() : 0f;
                case "contains": return values.containsKey(args[0]);
                case "set": values.put((String) args[0], args[1]); break;
                case "unset": values.remove(args[0]); break;
                default: break;
            }
            return null;
        });
    }

    private static final class Member {
        final String variant;
        String shipName;
        final FleetMemberAPI api;
        Member(String variant) {
            this.variant = variant;
            String hull = variant.equals(AviciDwellerContent.BUDAI_VARIANT) ? AviciDwellerContent.BUDAI_HULL
                    : variant.equals(AviciDwellerContent.BANSHEE_VARIANT) ? AviciDwellerContent.BANSHEE_HULL
                    : variant.equals(AviciDwellerContent.BRAIN_VARIANT) ? AviciDwellerContent.BRAIN_HULL
                    : variant.substring(0, variant.lastIndexOf('_'));
            ShipHullSpecAPI spec = mock(ShipHullSpecAPI.class, (name, args) -> {
                if (name.equals("getHullId") || name.equals("getBaseHullId")) return hull;
                return null;
            });
            ShipVariantAPI fit = mock(ShipVariantAPI.class, (name, args) -> {
                if (name.equals("getHullSpec")) return spec;
                if (name.equals("getHullVariantId")) return variant;
                return null;
            });
            RepairTrackerAPI repair = mock(RepairTrackerAPI.class,
                    (name, args) -> name.equals("getMaxCR") ? 0.7f : null);
            api = mock(FleetMemberAPI.class, (name, args) -> {
                if (name.equals("getHullId")) return hull;
                if (name.equals("getHullSpec")) return spec;
                if (name.equals("getVariant")) return fit;
                if (name.equals("getRepairTracker")) return repair;
                if (name.equals("setShipName")) shipName = (String) args[0];
                return null;
            });
        }
    }

    private static final class Fleet {
        final Memory memory = new Memory();
        final List<FleetMemberAPI> members = new ArrayList<>();
        final List<FleetEventListener> listeners = new ArrayList<>();
        final Vector2f position = new Vector2f();
        CampaignFleetAPI api;
        final StatBonus detected = new StatBonus();
        String id;
        String faction = Factions.DWELLER;
        String name;
        LocationAPI location;
        boolean player, transitioning, despawning, inBattle;
        Boolean abortDespawn;
        Runnable removeEntity;
        int mutations;
        FleetAssignmentDataAPI assignment;

        Fleet() {
            FactionAPI identity = mock(FactionAPI.class,
                    (method, args) -> method.equals("getId") ? faction : null);
            MutableFleetStatsAPI stats = mock(MutableFleetStatsAPI.class,
                    (method, args) -> method.equals("getSensorStrengthMod")
                            || method.equals("getSensorRangeMod") ? new StatBonus() : null);
            FleetDataAPI roster = mock(FleetDataAPI.class, (method, args) -> {
                if (method.equals("getMembersListCopy")) return new ArrayList<>(members);
                if (method.equals("addFleetMember")) members.add((FleetMemberAPI) args[0]);
                return null;
            });
            BattleAPI battle = mock(BattleAPI.class, (method, args) -> null);
            api = mock(CampaignFleetAPI.class, (method, args) -> {
                switch (method) {
                    case "getId": return id;
                    case "setId": id = (String) args[0]; break;
                    case "getName": return name;
                    case "setName": name = (String) args[0]; break;
                    case "getFaction": return identity;
                    case "getMemoryWithoutUpdate": return memory.api;
                    case "getFleetData": return roster;
                    case "getStats": return stats;
                    case "getDetectedRangeMod": return detected;
                    case "getEventListeners": return listeners;
                    case "addEventListener": listeners.add((FleetEventListener) args[0]); break;
                    case "isPlayerFleet": return player;
                    case "getContainingLocation": return location;
                    case "isEmpty": return members.isEmpty();
                    case "isInHyperspaceTransition": return transitioning;
                    case "isDespawning": return despawning;
                    case "setAbortDespawn": abortDespawn = (Boolean) args[0]; break;
                    case "getAbortDespawn": return abortDespawn;
                    case "despawn":
                        CampaignEventListener.FleetDespawnReason reason = args == null || args.length == 0
                                ? CampaignEventListener.FleetDespawnReason.OTHER
                                : (CampaignEventListener.FleetDespawnReason) args[0];
                        for (FleetEventListener listener : new ArrayList<>(listeners)) {
                            listener.reportFleetDespawnedToListener(api, reason,
                                    args == null || args.length < 2 ? null : args[1]);
                        }
                        // Native despawn fires listeners before consulting this flag and removing its token.
                        if (!Boolean.TRUE.equals(abortDespawn)) {
                            despawning = true;
                            if (removeEntity != null) removeEntity.run();
                        }
                        break;
                    case "getBattle": return inBattle ? battle : null;
                    case "getLocation": return position;
                    case "setLocation": position.set((Float) args[0], (Float) args[1]); break;
                    case "getCurrentAssignment": return assignment;
                    case "clearAssignments": assignment = null; break;
                    case "addAssignment":
                        FleetAssignment order = (FleetAssignment) args[0];
                        SectorEntityToken target = (SectorEntityToken) args[1];
                        assignment = mock(FleetAssignmentDataAPI.class, (call, ignored) -> {
                            if (call.equals("getAssignment")) return order;
                            if (call.equals("getTarget")) return target;
                            return null;
                        });
                        break;
                    default: break;
                }
                if (method.startsWith("set") || method.startsWith("add") || method.equals("clearAssignments")) {
                    mutations++;
                }
                return null;
            });
        }
    }

    private static final class Fixture {
        final Memory saved = new Memory();
        final Map<CampaignFleetAPI, Fleet> fleetStates = new IdentityHashMap<>();
        final Map<String, SectorEntityToken> local = new HashMap<>();
        final Map<String, SectorEntityToken> remote = new HashMap<>();
        final List<Fleet> created = new ArrayList<>();
        final List<Member> createdMembers = new ArrayList<>();
        final List<EveryFrameScript> installed = new ArrayList<>();
        final Vector2f center = new Vector2f(137f, -22000f);
        final Vector2f habitatPosition = new Vector2f(9000f, -22000f);
        final Vector2f jumpPosition = new Vector2f(-9000f, -18000f);
        final StarSystemAPI avici, other;
        final SectorEntityToken centerToken;
        double days;
        int factoryCalls;
        boolean paused, absent, failFactory, missingVariant;
        SectorEntityToken dialogTarget;

        Fixture() {
            centerToken = token("center", center);
            SectorEntityToken habitat = token(AviciHabitatEncounter.HABITAT_ID, habitatPosition);
            SectorEntityToken jump = token("arrival", jumpPosition);
            local.put(AviciHabitatEncounter.HABITAT_ID, habitat);
            avici = mock(StarSystemAPI.class, (method, args) -> {
                if (method.equals("getId") || method.equals("getOptionalUniqueId")) return OdysseyExpanseSystem.MESSINA_ID;
                if (method.equals("getMemoryWithoutUpdate")) {
                    Memory systemMemory = new Memory();
                    systemMemory.values.put("$chief_navigator_odyssey_authored_system_v1", true);
                    return systemMemory.api;
                }
                if (method.equals("getCenter")) return centerToken;
                if (method.equals("getEntityById")) return local.get(args[0]);
                if (method.equals("getJumpPoints")) return Collections.singletonList(jump);
                if (method.equals("getFleets")) return fleets(local);
                if (method.equals("addEntity")) {
                    CampaignFleetAPI fleet = (CampaignFleetAPI) args[0];
                    local.put(fleet.getId(), fleet);
                    fleetStates.get(fleet).location = (LocationAPI) fleet.getContainingLocation();
                }
                return null;
            });
            other = mock(StarSystemAPI.class, (method, args) -> {
                if (method.equals("getOptionalUniqueId") || method.equals("getId")) return "unrelated";
                if (method.equals("getEntityById")) return remote.get(args[0]);
                if (method.equals("getFleets")) return fleets(remote);
                return null;
            });
            CampaignClockAPI clock = mock(CampaignClockAPI.class, (method, args) -> {
                if (method.equals("getTimestamp")) return Math.round(days * 1000d);
                if (method.equals("getElapsedDaysSince")) return (float) (days - ((Long) args[0]) / 1000d);
                return null;
            });
            InteractionDialogAPI interaction = mock(InteractionDialogAPI.class,
                    (method, args) -> method.equals("getInteractionTarget") ? dialogTarget : null);
            CampaignUIAPI ui = mock(CampaignUIAPI.class,
                    (method, args) -> method.equals("getCurrentInteractionDialog") && dialogTarget != null
                            ? interaction : null);
            Global.setSettings(mock(SettingsAPI.class, (method, args) -> {
                if (method.equals("getColor")) return Color.WHITE;
                if (method.equals("getVariant")) return missingVariant ? null
                        : mock(ShipVariantAPI.class, (call, ignored) -> null);
                if (method.equals("getModManager")) throw new AssertionError("Standalone patrols must not query SEP");
                return null;
            }));
            Global.setSector(mock(SectorAPI.class, (method, args) -> {
                if (method.equals("getMemoryWithoutUpdate")) return saved.api;
                if (method.equals("getClock")) return clock;
                if (method.equals("getCampaignUI")) return ui;
                if (method.equals("isPaused")) return paused;
                if (method.equals("getStarSystems")) return absent ? Collections.singletonList(other) : Arrays.asList(avici, other);
                if (method.equals("getAllLocations")) return Arrays.asList(avici, other);
                if (method.equals("removeTransientScriptsOfClass")) installed.removeIf(script -> args[0] == script.getClass());
                if (method.equals("addTransientScript")) installed.add((EveryFrameScript) args[0]);
                return null;
            }));
            Global.setFactory(mock(FactoryAPI.class, (method, args) -> {
                if (method.equals("createEmptyFleet")) {
                    factoryCalls++;
                    if (failFactory) return null;
                    Fleet fleet = fleet();
                    // The native addEntity operation attaches the object; tests retain that identity.
                    fleet.location = avici;
                    created.add(fleet);
                    return fleet.api;
                }
                if (method.equals("createFleetMember")) {
                    Member member = new Member((String) args[1]);
                    createdMembers.add(member);
                    return member.api;
                }
                throw new AssertionError("Population must not create worlds or other factory objects: " + method);
            }));
        }

        private static List<CampaignFleetAPI> fleets(Map<String, SectorEntityToken> entities) {
            List<CampaignFleetAPI> result = new ArrayList<>();
            for (SectorEntityToken entity : entities.values()) if (entity instanceof CampaignFleetAPI) result.add((CampaignFleetAPI) entity);
            return result;
        }

        Fleet fleet() {
            Fleet fleet = new Fleet();
            fleet.removeEntity = () -> {
                local.remove(fleet.id, fleet.api);
                remote.remove(fleet.id, fleet.api);
                fleet.location = null;
            };
            fleetStates.put(fleet.api, fleet);
            return fleet;
        }

        void unlocked() {
            saved.values.put(SPAWNED, true);
            saved.values.put(OdysseyPredatorScript.BUDAI_SPAWNED_AT, 0L);
            days = 30d;
        }

        void tick() { new AviciDwellerPopulation().advance(2f); }
        int patrols() {
            int total = 0;
            for (SectorEntityToken entity : local.values()) if (AviciDwellerPopulation.isOwnedFleet(entity)) total++;
            return total;
        }
    }

    private static SectorEntityToken token(String id, Vector2f position) {
        return mock(SectorEntityToken.class, (method, args) -> {
            if (method.equals("getId")) return id;
            if (method.equals("getLocation")) return position;
            return null;
        });
    }

    private static Object spawnBudai(Fixture f) throws Exception {
        Method spawn = OdysseyPredatorScript.class.getDeclaredMethod("spawnCharybdis", StarSystemAPI.class);
        spawn.setAccessible(true);
        return spawn.invoke(null, f.avici);
    }

    private static void creationAndClocks() throws Exception {
        Fixture f = new Fixture();
        f.days = 1000d;
        f.tick();
        check(f.factoryCalls == 0 && !f.saved.values.containsKey(OdysseyPredatorScript.BUDAI_LEGACY_GRACE_AT),
                "Never-spawned Budai must not acquire a false campaign-start or legacy clock");
        f.saved.values.put(COMPLETE, true);
        f.failFactory = true;
        check(spawnBudai(f) == null && !f.saved.values.containsKey(OdysseyPredatorScript.BUDAI_SPAWNED_AT)
                        && !f.saved.values.containsKey(SPAWNED),
                "A failed actual fleet creation must not start or consume the spawn timer");

        f = new Fixture();
        f.saved.values.put(COMPLETE, true);
        CampaignFleetAPI boss = (CampaignFleetAPI) spawnBudai(f);
        check(boss != null && f.createdMembers.size() == 1
                        && f.createdMembers.get(0).variant.equals(AviciDwellerContent.BUDAI_VARIANT)
                        && "Budai".equals(f.createdMembers.get(0).shipName),
                "Actual new Budai is one named Hunter with no escorts");
        check(boss.getId().equals(TroyArrivalScript.CHARYBDIS_FLEET_ID)
                        && boss.getLocation().x == f.center.x + 100000f && boss.getLocation().y == f.center.y,
                "The new hull must retain canonical fleet identity and actual100000-unit placement");
        check(Long.valueOf(0L).equals(f.saved.values.get(OdysseyPredatorScript.BUDAI_SPAWNED_AT))
                        && OdysseyPredatorScript.isBudaiBattle(boss),
                "A successful birth at timestamp0 is valid and recognized by existing quest ownership");
        f.days = 29.999d;
        f.saved.values.put(OdysseyPredatorScript.BUDAI_DEFEATED, true);
        f.tick();
        check(f.patrols() == 0, "Budai's death must not bypass the full30-day delay");
        f.days = 30d;
        f.tick();
        check(f.patrols() == 4, "Four patrols unlock at exactly30 calendar days even after Budai dies");
        f.days = 130d;
        f.tick();
        check(f.patrols() == 4 && Long.valueOf(0L).equals(f.saved.values.get(OdysseyPredatorScript.BUDAI_SPAWNED_AT)),
                "New controller instances/reloads never reset birth or exceed the owned cap");
    }

    private static void legacyAndMissingContent() {
        Fixture f = new Fixture();
        f.days = 100d;
        f.saved.values.put(SPAWNED, true);
        check(!OdysseyPredatorScript.hasBudaiSpawnedFor(30f)
                        && Long.valueOf(100000L).equals(f.saved.values.get(OdysseyPredatorScript.BUDAI_LEGACY_GRACE_AT))
                        && !f.saved.values.containsKey(OdysseyPredatorScript.BUDAI_SPAWNED_AT),
                "Proven legacy births get an honest separate grace anchor, not a fabricated actual birth");
        f.days = 129.999d;
        check(!OdysseyPredatorScript.hasBudaiSpawnedFor(30f), "Legacy grace must wait30 complete days");
        f.days = 130d;
        check(OdysseyPredatorScript.hasBudaiSpawnedFor(30f)
                        && Long.valueOf(100000L).equals(f.saved.values.get(OdysseyPredatorScript.BUDAI_LEGACY_GRACE_AT)),
                "Legacy grace persists across queries and reloads");
        f = new Fixture();
        f.days = 500d;
        f.saved.values.put(OdysseyPredatorScript.BUDAI_DEFEATED, true);
        check(!OdysseyPredatorScript.hasBudaiSpawnedFor(30f), "Defeated legacy Budai proves prior spawn without instant unlock");
        f = new Fixture();
        f.unlocked();
        f.absent = true;
        f.tick();
        check(f.factoryCalls == 0, "Missing Avici must not create or repair world topology");
        f.absent = false;
        f.missingVariant = true;
        f.tick();
        check(f.factoryCalls == 0, "Missing required content must fail before fleet creation");
        AviciDwellerPopulation.install();
        AviciDwellerPopulation.install();
        check(f.installed.size() == 1, "Repeated game-load installation retains only one transient controller");
    }

    private static void ownershipAndRosters() {
        Fixture f = new Fixture();
        f.unlocked();
        f.tick();
        check(f.patrols() == 4, "The controller must create only its2Lesser+1Greater+1Transcendent slots");
        long banshees = f.createdMembers.stream().filter(member -> member.variant.equals(AviciDwellerContent.BANSHEE_VARIANT)).count();
        long brains = f.createdMembers.stream().filter(member -> member.variant.equals(AviciDwellerContent.BRAIN_VARIANT)).count();
        check(banshees == 3 && brains == 1 && f.createdMembers.stream().noneMatch(
                        member -> member.variant.equals(AviciDwellerContent.BUDAI_VARIANT)),
                "Ported support stays sparse; ambient rosters must never contain another Hunter");
        for (Fleet fleet : f.created) {
            check(fleet.memory.api.getBoolean(MemFlags.MEMORY_KEY_NO_JUMP)
                            && fleet.assignment.getAssignment() == FleetAssignment.PATROL_SYSTEM
                            && fleet.assignment.getTarget() == f.centerToken,
                    "Every own patrol keeps native local patrol orders and no-jump containment");
            check(Vector2f.sub(fleet.position, f.habitatPosition, new Vector2f()).length() >= 8000f
                            && Vector2f.sub(fleet.position, f.jumpPosition, new Vector2f()).length() >= 6000f,
                    "Initial patrols must not materialize in the habitat shelter or arrival clearance");
        }
        Fleet survivor = f.fleetStates.get(f.local.get(AviciDwellerPopulation.id(2)));
        survivor.members.remove(0);
        List<FleetMemberAPI> preserved = new ArrayList<>(survivor.members);
        f.tick();
        check(f.created.size() == 4 && survivor.members.equals(preserved),
                "Repeated maintenance must preserve actual surviving members instead of replenishing rosters");

        for (String reason : Arrays.asList("battle", "transition", "despawn", "dialog")) {
            f = new Fixture(); f.unlocked(); f.tick();
            Fleet busy = f.created.get(0);
            busy.inBattle = reason.equals("battle");
            busy.transitioning = reason.equals("transition");
            busy.despawning = reason.equals("despawn");
            f.dialogTarget = reason.equals("dialog") ? busy.api : null;
            int mutations = busy.mutations;
            f.tick();
            check(busy.mutations == mutations && f.created.size() == 4,
                    "Busy own fleet must stay untouched: " + reason);
        }

        f = new Fixture(); f.unlocked();
        for (int index = 0; index < 4; index++) {
            String id = AviciDwellerPopulation.id(index);
            if (index == 2) { f.local.put(id, token(id, new Vector2f())); continue; }
            Fleet claim = f.fleet(); claim.id = id; claim.location = f.avici;
            claim.memory.values.put(AviciDwellerPopulation.PATROL_MARKER, true);
            if (index == 0) claim.faction = "foreign";
            if (index == 1) claim.player = true;
            f.local.put(id, claim.api); // slot3 is an owned but retained empty claim.
        }
        f.tick();
        check(f.factoryCalls == 0, "Foreign/player/wrong-type/empty claims must block replacement without mutation");
    }

    private static void verifiedLossAndMissingClaims() {
        Fixture nativeDespawn = new Fixture(); nativeDespawn.unlocked(); nativeDespawn.tick();
        Fleet destroyed = nativeDespawn.created.get(0);
        nativeDespawn.days = 40d;
        destroyed.members.clear();
        destroyed.api.despawn(CampaignEventListener.FleetDespawnReason.DESTROYED_BY_BATTLE, null);
        check(!Boolean.TRUE.equals(destroyed.api.getAbortDespawn())
                        && !nativeDespawn.local.containsKey(destroyed.id) && destroyed.despawning,
                "An actual battle-despawn request must finish native cleanup after the loss listener");
        nativeDespawn.days = 69.999d; nativeDespawn.tick();
        check(nativeDespawn.created.size() == 4,
                "Native-cleaned battle losses still wait the full replacement delay");
        nativeDespawn.days = 70d; nativeDespawn.tick();
        check(nativeDespawn.created.size() == 5 && nativeDespawn.patrols() == 4,
                "Native battle despawn must permit exactly one delayed replacement without manual token deletion");

        Fixture f = new Fixture(); f.unlocked(); f.tick();
        Fleet lost = f.created.get(0);
        FleetEventListener listener = lost.listeners.get(0);
        f.days = 40d;
        lost.members.clear();
        BattleAPI defeat = mock(BattleAPI.class, (method, args) -> method.equals("wasFleetDefeated") ? true : null);
        listener.reportBattleOccurred(lost.api, null, defeat);
        f.days = 50d;
        f.tick();
        check(f.created.size() == 4 && f.local.get(lost.id) == lost.api,
                "Even a verified loss never deletes a retained empty campaign token");
        f.local.remove(lost.id);
        f.days = 69.999d; f.tick();
        check(f.created.size() == 4, "Replacement must wait30 campaign days after verified loss");
        f.days = 70d; f.tick();
        check(f.created.size() == 5 && f.patrols() == 4, "Only the lost slot replaces at its exact deadline");
        listener.reportFleetDespawnedToListener(lost.api, CampaignEventListener.FleetDespawnReason.DESTROYED_BY_BATTLE, null);
        f.local.remove(lost.id); f.days = 200d; f.tick();
        check(f.created.size() == 5, "A delayed old-generation callback must not authorize another replacement");

        f = new Fixture(); f.unlocked(); f.tick();
        lost = f.created.get(0);
        f.local.remove(lost.id);
        lost.listeners.get(0).reportFleetDespawnedToListener(lost.api, CampaignEventListener.FleetDespawnReason.OTHER, null);
        f.days = 1000d; f.tick();
        check(f.created.size() == 4, "Unexplained absence or ordinary despawn is not proof of a lost patrol");
        f.remote.put(lost.id, lost.api); lost.location = f.other;
        int mutations = lost.mutations;
        f.tick();
        check(f.created.size() == 4 && lost.mutations == mutations,
                "A claim in another saved location blocks duplicates and is never teleported or reassigned");
    }

    public static void main(String[] args) throws Exception {
        SectorAPI priorSector = Global.getSector();
        SettingsAPI priorSettings = Global.getSettings();
        FactoryAPI priorFactory = Global.getFactory();
        try {
            creationAndClocks();
            legacyAndMissingContent();
            ownershipAndRosters();
            verifiedLossAndMissingClaims();
            System.out.println("PASS: actual Hunter birth, persisted30-day Avici unlock, sparse standalone rosters, "
                    + "scoped ownership, busy/empty claims, and verified-loss replacement.");
        } finally {
            Global.setSector(priorSector);
            Global.setSettings(priorSettings);
            Global.setFactory(priorFactory);
        }
    }
}
