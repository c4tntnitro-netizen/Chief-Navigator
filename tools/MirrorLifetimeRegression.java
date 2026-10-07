package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import java.awt.Color;
import java.util.*;
import static chiefnavigator.quest.BudaiSalvageRewardRegression.*;

/** Populated public-lineage registry and retryable removal, with no live game required. */
public final class MirrorLifetimeRegression {
    private static final class ShipState {
        final Map<String, Object> data = new HashMap<>();
        final List<ShipAPI> children = new ArrayList<>(), drones = new ArrayList<>();
        final List<FighterWingAPI> wings = new ArrayList<>();
        FleetMemberAPI member = mock(FleetMemberAPI.class, (name, args) -> null);
        ShipAPI parent, droneSource;
        FighterWingAPI wing;
        boolean module, drone, fighter, alive = true, unreadableWing;
        boolean holdFire, locked, phased;
        CollisionClass collision = CollisionClass.SHIP;
        float alpha = 1f;
        int mutations;
        final ShipAPI ship = mock(ShipAPI.class, (name, args) -> {
            switch (name) {
                case "getCustomData": return data;
                case "setCustomData": data.put((String) args[0], args[1]); break;
                case "getFleetMember": return member;
                case "getChildModulesCopy": return new ArrayList<>(children);
                case "getDeployedDrones": return new ArrayList<>(drones);
                case "getAllWings": return new ArrayList<>(wings);
                case "getParentStation": return parent;
                case "getDroneSource": return droneSource;
                case "getWing":
                    if (unreadableWing) throw new IllegalStateException("Unrelated bad wing accessor");
                    return wing;
                case "isStationModule": return module;
                case "isDrone": return drone;
                case "isFighter": return fighter;
                case "isAlive": return alive;
                case "getCollisionClass": return collision;
                case "setCollisionClass": collision = (CollisionClass) args[0]; mutations++; break;
                case "setHoldFire": holdFire = (Boolean) args[0]; mutations++; break;
                case "setControlsLocked": locked = (Boolean) args[0]; mutations++; break;
                case "setPhased": phased = (Boolean) args[0]; mutations++; break;
                case "setAlphaMult": alpha = ((Number) args[0]).floatValue(); mutations++; break;
                case "setShipAI":
                case "setSpawnDebris":
                case "setHulkChanceOverride": mutations++; break;
                default: break;
            }
            return null;
        });
    }

    private static final class WingState {
        ShipAPI source;
        final List<ShipAPI> fighters = new ArrayList<>();
        final List<FighterWingAPI.ReturningFighter> returning = new ArrayList<>();
        final FighterWingAPI wing = mock(FighterWingAPI.class, (name, args) -> {
            if (name.equals("getSourceShip")) return source;
            if (name.equals("getWingMembers")) return new ArrayList<>(fighters);
            if (name.equals("getReturning")) return new ArrayList<>(returning);
            return null;
        });
    }

    private static final class Fixture {
        final Set<ShipAPI> present = Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<ShipAPI> managed = Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<ShipAPI, Integer> removeFailures = new IdentityHashMap<>();
        final Map<String, Object> engineData = new HashMap<>();
        final Map<DeployedFleetMemberAPI, DeployedFleetMemberAPI> shards = new IdentityHashMap<>();
        final Set<FighterWingAPI> removedWings = Collections.newSetFromMap(new IdentityHashMap<>());
        int wingFailures;
        boolean shardMapUnreadable;
        ShipAPI removalTrigger;
        ShipState spawnedDuringRemoval;
        final CombatEngineAPI engine = mock(CombatEngineAPI.class, (name, args) -> {
            if (name.equals("getCustomData")) return engineData;
            if (name.equals("getShips")) return new ArrayList<>(present);
            if (name.equals("isEntityInPlay")) return present.contains(args[0]);
            if (name.equals("removeEntity")) {
                ShipAPI ship = (ShipAPI) args[0];
                int remaining = removeFailures.getOrDefault(ship, 0);
                if (remaining > 0) {
                    removeFailures.put(ship, remaining - 1);
                    throw new IllegalStateException("Temporary native entity removal failure");
                }
                present.remove(ship);
                if (ship == removalTrigger && spawnedDuringRemoval != null) {
                    add(spawnedDuringRemoval);
                    spawnedDuringRemoval = null;
                }
            }
            return null;
        });
        final CombatFleetManagerAPI manager = mock(CombatFleetManagerAPI.class, (name, args) -> {
            if (name.equals("getShardToOriginalShipMap")) {
                if (shardMapUnreadable) throw new IllegalStateException("Transient manager shard-map failure");
                return shards;
            }
            if (name.equals("getDeployedFleetMemberEvenIfDisabled")) {
                return managed.contains(args[0]) ? deployed((ShipAPI) args[0]) : null;
            }
            if (name.equals("removeDeployed")) {
                if (args[0] instanceof FighterWingAPI) {
                    check(!(Boolean) args[1],
                            "Wing cleanup must not deliberately trigger native fighter damage/death effects");
                    if (wingFailures > 0) {
                        wingFailures--;
                        throw new IllegalStateException("Temporary wing removal failure");
                    }
                    removedWings.add((FighterWingAPI) args[0]);
                } else managed.remove(args[0]);
            }
            return null;
        });

        void add(ShipState... states) {
            for (ShipState state : states) {
                present.add(state.ship);
                managed.add(state.ship);
            }
        }
    }

    private static DeployedFleetMemberAPI deployed(ShipAPI ship) {
        return mock(DeployedFleetMemberAPI.class, (name, args) ->
                name.equals("getShip") ? ship : name.equals("getMember") ? ship.getFleetMember() : null);
    }

    public static void main(String[] args) {
        SettingsAPI settings = Global.getSettings();
        Global.setSettings(mock(SettingsAPI.class,
                (name, values) -> name.equals("getColor") ? Color.WHITE : null));
        try {
            verifyDetachedCarrierTreeAndRetries();
            verifyTransformationsAndShardRoles();
            verifyIndependentGenerations();
            verifyRemovedBrokenOwnerDoesNotRetainQueue();
            verifyRemovalCallbackDescendantRetainsQueue();
            verifyLateShardProofAndCycleGuards();
        } finally { Global.setSettings(settings); }
        System.out.println("PASS: lifetime-owned vambraces, returning fighters, drones, "
                + "explicit transformations, shard/component roles, source isolation, and cleanup retries.");
    }

    private static void verifyDetachedCarrierTreeAndRetries() {
        Fixture f = new Fixture();
        ShipState root = new ShipState(), vambrace = new ShipState(), nested = new ShipState();
        ShipState fighter = new ShipState(), returning = new ShipState(), drone = new ShipState();
        ShipState player = new ShipState(), ordinary = new ShipState(), unreadable = new ShipState();
        root.data.put(GautamaFinalRematchBattleCreationPlugin.MIRROR_SOURCE_MEMBER_KEY, "original-player-member");
        // The logging/source id is deliberately insufficient to claim another ship.
        ordinary.data.put(GautamaFinalRematchBattleCreationPlugin.MIRROR_SOURCE_MEMBER_KEY, "original-player-member");
        unreadable.unreadableWing = true;
        vambrace.module = true;
        vambrace.member = root.member;
        vambrace.parent = root.ship;
        nested.module = true;
        nested.parent = vambrace.ship;
        root.children.add(vambrace.ship);
        vambrace.children.add(nested.ship);
        drone.drone = true;
        drone.droneSource = root.ship;
        root.drones.add(drone.ship);
        WingState wing = new WingState();
        wing.source = vambrace.ship;
        fighter.fighter = returning.fighter = true;
        fighter.wing = returning.wing = wing.wing;
        wing.fighters.add(fighter.ship);
        wing.returning.add(new FighterWingAPI.ReturningFighter(returning.ship, null));
        vambrace.wings.add(wing.wing);
        f.add(root, vambrace, nested, fighter, returning, drone, player, ordinary, unreadable);
        f.shardMapUnreadable = true;
        MirrorLifetime lifetime = MirrorLifetime.forShip(root.ship);
        check(lifetime.capture(f.engine, f.manager) == null,
                "Unrelated wing accessors or transient manager shard-map failure cannot poison a valid mirror");
        check(lifetime.getShipsCopy().size() == 6 && lifetime.contains(returning.ship)
                        && lifetime.contains(drone.ship) && !lifetime.contains(player.ship)
                        && !lifetime.contains(ordinary.ship) && !lifetime.contains(unreadable.ship),
                "Capture only the exact root/module/wing/drone public lineage");
        check(MirrorLifetime.forShip(vambrace.ship) == lifetime
                        && vambrace.data.get(GautamaFinalRematchBattleCreationPlugin.MIRROR_SOURCE_MEMBER_KEY)
                                .equals("original-player-member"),
                "Components retain generation ownership and source logging identity");
        root.children.clear();
        root.drones.clear();
        vambrace.children.clear();
        vambrace.wings.clear();
        vambrace.parent = nested.parent = drone.droneSource = null;
        wing.source = null;
        wing.fighters.clear();
        wing.returning.clear();
        root.alive = false;
        check(lifetime.capture(f.engine, f.manager) == null
                        && lifetime.findAlivePrimary(f.engine) == null,
                "Detached surviving vambraces/fighters/drones do not make a dead root live");
        f.removeFailures.put(vambrace.ship, 2);
        f.wingFailures = 1;
        MirrorLifetime.queueCleanup(f.engine, lifetime);
        check(lifetime.removeBestEffort(f.engine, f.manager) != null
                        && !lifetime.isRemovalComplete(f.engine),
                "Failed known removal keeps an incomplete lifetime for retry");
        check(vambrace.holdFire && vambrace.locked && vambrace.phased
                        && vambrace.alpha == 0f && vambrace.collision == CollisionClass.NONE,
                "A pending detached component cannot fight, collide, or visibly retreat");
        check(MirrorLifetime.retryPending(f.engine, f.manager) != null
                        && f.engineData.containsKey(MirrorLifetime.PENDING_CLEANUP_KEY),
                "A repeated native removal failure must retain the cleanup queue");
        check(MirrorLifetime.retryPending(f.engine, f.manager) == null
                        && lifetime.isRemovalComplete(f.engine)
                        && !f.engineData.containsKey(MirrorLifetime.PENDING_CLEANUP_KEY)
                        && f.removedWings.contains(wing.wing),
                "Eventually remove every detached entity and its native wing registration");
        check(f.present.size() == 3 && f.present.contains(player.ship)
                        && f.present.contains(ordinary.ship) && f.present.contains(unreadable.ship)
                        && player.mutations == 0 && ordinary.mutations == 0 && unreadable.mutations == 0,
                "Cleanup preserves player originals and every unrelated Third Strike/mod ship");
    }

    private static void verifyTransformationsAndShardRoles() {
        Fixture f = new Fixture();
        ShipState original = new ShipState(), replacement = new ShipState(), copiedReplacement = new ShipState();
        ShipState shard = new ShipState(), module = new ShipState(), chainedDrone = new ShipState();
        original.data.put(GautamaFinalRematchBattleCreationPlugin.MIRROR_SOURCE_MEMBER_KEY, "transform-source");
        module.module = true;
        module.member = original.member;
        module.parent = original.ship;
        chainedDrone.drone = true;
        chainedDrone.droneSource = module.ship;
        f.add(original, shard, module, chainedDrone);
        f.shards.put(deployed(shard.ship), deployed(original.ship));
        MirrorLifetime lifetime = MirrorLifetime.forShip(original.ship);
        check(lifetime.capture(f.engine, f.manager) == null && lifetime.contains(shard.ship)
                        && lifetime.contains(module.ship) && lifetime.contains(chainedDrone.ship),
                "Discover reverse-order explicit parent/drone and native shard lineage");
        original.alive = false;
        check(lifetime.findAlivePrimary(f.engine) == null,
                "A live native shard and same-member module remain components, not a replacement parent");
        replacement.member = original.member;
        f.add(replacement);
        check(lifetime.capture(f.engine, f.manager) == null
                        && lifetime.findAlivePrimary(f.engine) == replacement.ship,
                "A genuinely independent same-generated-member transformation is a primary successor");
        copiedReplacement.data.putAll(replacement.data);
        replacement.alive = false;
        f.add(copiedReplacement);
        check(lifetime.capture(f.engine, f.manager) == null
                        && lifetime.findAlivePrimary(f.engine) == copiedReplacement.ship,
                "A distinct-member transformation must copy the exact private generation/primary token");
        check(lifetime.removeBestEffort(f.engine, f.manager) == null
                        && lifetime.isRemovalComplete(f.engine) && f.present.isEmpty(),
                "All transformed primaries, shards, and detached components share generation cleanup");
    }

    private static void verifyIndependentGenerations() {
        Fixture f = new Fixture();
        for (int generation = 0; generation < 3; generation++) {
            ShipState root = new ShipState(), child = new ShipState();
            root.data.put(GautamaFinalRematchBattleCreationPlugin.MIRROR_SOURCE_MEMBER_KEY, "same-source");
            root.children.add(child.ship);
            child.module = true;
            f.add(root, child);
            MirrorLifetime lifetime = MirrorLifetime.forShip(root.ship);
            check(lifetime.capture(f.engine, f.manager) == null,
                    "Each rebuilt generation captures its own component identities");
            root.children.clear();
            check(lifetime.removeBestEffort(f.engine, f.manager) == null
                            && lifetime.isRemovalComplete(f.engine) && f.present.isEmpty(),
                    "Repeated rebuild cleanup cannot leave earlier-generation detached modules");
        }
    }

    private static void verifyRemovedBrokenOwnerDoesNotRetainQueue() {
        Fixture f = new Fixture();
        ShipState root = new ShipState();
        root.unreadableWing = true;
        f.add(root);
        MirrorLifetime lifetime = MirrorLifetime.forShip(root.ship);
        MirrorLifetime.queueCleanup(f.engine, lifetime);
        check(lifetime.removeBestEffort(f.engine, f.manager) != null && f.present.isEmpty(),
                "A live owned accessor failure remains reportable while removal still proceeds");
        check(MirrorLifetime.retryPending(f.engine, f.manager) == null
                        && lifetime.isRemovalComplete(f.engine)
                        && !f.engineData.containsKey(MirrorLifetime.PENDING_CLEANUP_KEY),
                "Once absence is confirmed, broken removed accessors cannot retain cleanup forever");
    }

    private static void verifyRemovalCallbackDescendantRetainsQueue() {
        Fixture f = new Fixture();
        ShipState root = new ShipState(), lateDrone = new ShipState();
        lateDrone.drone = true;
        lateDrone.droneSource = root.ship;
        f.add(root);
        f.removalTrigger = root.ship;
        f.spawnedDuringRemoval = lateDrone;
        MirrorLifetime lifetime = MirrorLifetime.forShip(root.ship);
        MirrorLifetime.queueCleanup(f.engine, lifetime);
        check(lifetime.removeBestEffort(f.engine, f.manager) == null
                        && lifetime.contains(lateDrone.ship)
                        && !lifetime.isRemovalComplete(f.engine),
                "A synchronously spawned descendant must prevent premature cleanup completion");
        check(lateDrone.holdFire && lateDrone.locked && lateDrone.phased
                        && lateDrone.alpha == 0f && lateDrone.collision == CollisionClass.NONE,
                "A late public-lineage descendant is neutralized before its first retry");
        lateDrone.droneSource = null;
        check(MirrorLifetime.retryPending(f.engine, f.manager) == null
                        && lifetime.isRemovalComplete(f.engine) && f.present.isEmpty()
                        && !f.engineData.containsKey(MirrorLifetime.PENDING_CLEANUP_KEY),
                "Post-removal capture retains late descendants even if they detach before retry");
    }

    private static void verifyLateShardProofAndCycleGuards() {
        Fixture f = new Fixture();
        ShipState root = new ShipState(), shard = new ShipState();
        shard.member = root.member;
        root.parent = root.ship; // Bad self-link must not make the original root a component.
        f.add(root, shard);
        f.shards.put(deployed(shard.ship), deployed(root.ship));
        f.shardMapUnreadable = true;
        MirrorLifetime lifetime = MirrorLifetime.forShip(root.ship);
        check(lifetime.capture(f.engine, f.manager) == null
                        && lifetime.findAlivePrimary(f.engine) == root.ship,
                "Unreadable global shard data is isolated and original self-links cannot demote the root");
        root.alive = false;
        check(lifetime.findAlivePrimary(f.engine) == shard.ship,
                "Before component proof, an independent same-member entity has transformation provenance");
        f.shardMapUnreadable = false;
        check(lifetime.capture(f.engine, f.manager) == null
                        && lifetime.findAlivePrimary(f.engine) == null,
                "Later explicit native shard proof must demote an initially inferred primary");

        ShipState replacement = new ShipState(), child = new ShipState();
        replacement.member = root.member;
        f.add(replacement);
        check(lifetime.capture(f.engine, f.manager) == null
                        && lifetime.findAlivePrimary(f.engine) == replacement.ship,
                "Shard demotion must not erase the original generated member's primary lineage");
        child.module = true;
        child.parent = replacement.ship;
        f.add(child);
        check(lifetime.capture(f.engine, f.manager) == null && lifetime.contains(child.ship),
                "A transformed primary still captures its own true component");
        replacement.parent = child.ship;
        check(lifetime.capture(f.engine, f.manager) == null
                        && lifetime.findAlivePrimary(f.engine) == replacement.ship,
                "A cyclic component back-link cannot falsely demote the current transformed root");
        check(lifetime.removeBestEffort(f.engine, f.manager) == null
                        && lifetime.isRemovalComplete(f.engine) && f.present.isEmpty(),
                "Late-demoted shards and cyclic source links still permit exact identity cleanup");
    }
}
