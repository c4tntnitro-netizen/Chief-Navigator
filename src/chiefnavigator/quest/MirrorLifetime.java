package chiefnavigator.quest;

import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/** Combat-local ownership of one reflection generation, including detached parts. */
final class MirrorLifetime {
    static final String LINEAGE_KEY = "chief_navigator_ungaikyo_mirror_lifetime";
    static final String PENDING_CLEANUP_KEY =
            "chief_navigator_ungaikyo_mirror_pending_cleanup";

    private enum Role { PRIMARY, COMPONENT }

    /** Copying this exact token preserves provenance; hull/owner/source ids do not. */
    private static final class Lineage {
        final MirrorLifetime lifetime;
        final Role role;
        Lineage(MirrorLifetime lifetime, Role role) {
            this.lifetime = lifetime;
            this.role = role;
        }
    }

    private final Map<ShipAPI, Role> roles = new IdentityHashMap<>();
    private final List<ShipAPI> ships = new ArrayList<>();
    private final Map<FleetMemberAPI, Role> members = new IdentityHashMap<>();
    private final Map<ShipAPI, FleetMemberAPI> shipMembers = new IdentityHashMap<>();
    private final Set<FighterWingAPI> wings = identitySet();
    private final Set<ShipAPI> managerShipsPending = identitySet();
    private final Set<FighterWingAPI> managerWingsPending = identitySet();
    private final Set<ShipAPI> stamped = identitySet();
    private final Lineage primaryLineage = new Lineage(this, Role.PRIMARY);
    private final Lineage componentLineage = new Lineage(this, Role.COMPONENT);
    private final ShipAPI originalRoot;
    private final String sourceMemberId;
    private Throwable lastCaptureFailure;

    private MirrorLifetime(ShipAPI root) {
        originalRoot = root;
        Object source = readUnrelated(() -> root.getCustomData().get(
                GautamaFinalRematchBattleCreationPlugin.MIRROR_SOURCE_MEMBER_KEY));
        sourceMemberId = source instanceof String ? (String) source : null;
        register(root, Role.PRIMARY);
        FleetMemberAPI member = readUnrelated(root::getFleetMember);
        if (member != null) {
            shipMembers.put(root, member);
            members.put(member, Role.PRIMARY);
        }
    }

    static MirrorLifetime forShip(ShipAPI ship) {
        if (ship == null) throw new IllegalArgumentException("A mirror lifetime needs a ship");
        Object token = readUnrelated(() -> ship.getCustomData().get(LINEAGE_KEY));
        if (token instanceof Lineage) return ((Lineage) token).lifetime;
        MirrorLifetime result = new MirrorLifetime(ship);
        // Publish before spawning/advancing components, so a copied custom-data
        // map can identify its originating generation without a global hull rule.
        try {
            ship.setCustomData(LINEAGE_KEY, result.primaryLineage);
            result.stamped.add(ship);
        } catch (Throwable failure) {
            GautamaFinalRematchBattleCreationPlugin.rethrowIfFatal(failure);
            result.lastCaptureFailure = failure;
        }
        return result;
    }

    boolean contains(ShipAPI ship) { return roles.containsKey(ship); }

    List<ShipAPI> getShipsCopy() { return new ArrayList<>(ships); }

    /** Captures explicit public lineage before a parent can lose its attachment links. */
    Throwable capture(CombatEngineAPI engine, CombatFleetManagerAPI manager) {
        Failures failures = new Failures();
        Set<ShipAPI> inspected = identitySet();
        inspectKnown(engine, inspected, failures);

        List<ShipAPI> combatShips = readKnown(
                () -> copy(engine == null ? null : engine.getShips()), failures);
        List<Candidate> candidates = new ArrayList<>();
        Map<ShipAPI, Candidate> snapshots = new IdentityHashMap<>();
        if (combatShips != null) {
            for (ShipAPI candidate : combatShips) {
                if (candidate != null && (!contains(candidate)
                        || (candidate != originalRoot && roles.get(candidate) == Role.PRIMARY))) {
                    Candidate snapshot = new Candidate(candidate);
                    candidates.add(snapshot);
                    snapshots.put(candidate, snapshot);
                }
            }
        }
        Map<ShipAPI, ShipAPI> shardOrigins = new IdentityHashMap<>();
        if (manager != null) {
            Map<DeployedFleetMemberAPI, DeployedFleetMemberAPI> shardMap = readUnrelated(() -> {
                Map<DeployedFleetMemberAPI, DeployedFleetMemberAPI> nativeMap =
                        manager.getShardToOriginalShipMap();
                return nativeMap == null ? Collections.emptyMap() : new IdentityHashMap<>(nativeMap);
            });
            if (shardMap != null) {
                for (Map.Entry<DeployedFleetMemberAPI, DeployedFleetMemberAPI> entry : shardMap.entrySet()) {
                    ShipAPI shard = readUnrelated(() -> entry.getKey().getShip());
                    ShipAPI origin = readUnrelated(() -> entry.getValue().getShip());
                    if (shard != null && origin != null) {
                        shardOrigins.put(shard, origin);
                        if (!contains(shard) && !containsCandidate(candidates, shard)) {
                            Candidate snapshot = new Candidate(shard);
                            candidates.add(snapshot);
                            snapshots.put(shard, snapshot);
                        }
                    }
                }
            }
        }

        // Getter results are cached once per scan. Resolve reversed ordering or
        // multiple explicit lineage hops without repeatedly calling other mods.
        boolean changed;
        do {
            changed = false;
            for (Candidate candidate : candidates) {
                if (contains(candidate.ship)) {
                    if (candidate.ship != originalRoot && roles.get(candidate.ship) == Role.PRIMARY
                            && candidate.hasComponentProof(shardOrigins, snapshots)) {
                        demotePrimary(candidate.ship);
                        inspected.remove(candidate.ship);
                        changed = true;
                    }
                    continue;
                }
                Role role = candidate.relatedRole(shardOrigins);
                if (role != null) {
                    register(candidate.ship, role);
                    changed = true;
                }
            }
            if (changed) inspectKnown(engine, inspected, failures);
        } while (changed);
        lastCaptureFailure = failures.failure;
        return failures.failure;
    }

    private void inspectKnown(CombatEngineAPI engine, Set<ShipAPI> inspected, Failures failures) {
        for (int index = 0; index < ships.size(); index++) {
            ShipAPI ship = ships.get(index);
            if (!inspected.add(ship)) continue;
            Boolean present = engine == null ? null : readKnown(
                    () -> engine.isEntityInPlay(ship), failures);
            // Retain provenance/member identity for late descendants, but an
            // entity already removed cannot invalidate every later cleanup retry.
            if (Boolean.FALSE.equals(present)) continue;
            Role role = roles.get(ship);
            if (!stamped.contains(ship)) {
                readKnown(() -> {
                    ship.setCustomData(LINEAGE_KEY,
                            role == Role.PRIMARY ? primaryLineage : componentLineage);
                    if (sourceMemberId != null) {
                        ship.setCustomData(
                                GautamaFinalRematchBattleCreationPlugin.MIRROR_SOURCE_MEMBER_KEY,
                                sourceMemberId);
                    }
                    stamped.add(ship);
                    return null;
                }, failures);
            }
            FleetMemberAPI member = readKnown(ship::getFleetMember, failures);
            if (member != null) shipMembers.put(ship, member);
            if (member != null && (role == Role.PRIMARY || !members.containsKey(member))) {
                members.put(member, role);
            }
            List<ShipAPI> children = readKnown(() -> copy(ship.getChildModulesCopy()), failures);
            if (children != null) for (ShipAPI child : children) register(child, Role.COMPONENT);
            List<ShipAPI> drones = readKnown(() -> copy(ship.getDeployedDrones()), failures);
            if (drones != null) for (ShipAPI drone : drones) register(drone, Role.COMPONENT);
            FighterWingAPI ownWing = readKnown(ship::getWing, failures);
            registerWing(ownWing, failures);
            List<FighterWingAPI> launched = readKnown(() -> copy(ship.getAllWings()), failures);
            if (launched != null) for (FighterWingAPI wing : launched) registerWing(wing, failures);
        }
    }

    private void registerWing(FighterWingAPI wing, Failures failures) {
        if (wing == null) return;
        if (wings.add(wing)) managerWingsPending.add(wing);
        List<ShipAPI> fighters = readKnown(() -> copy(wing.getWingMembers()), failures);
        if (fighters != null) for (ShipAPI fighter : fighters) register(fighter, Role.COMPONENT);
        List<FighterWingAPI.ReturningFighter> returning = readKnown(
                () -> copy(wing.getReturning()), failures);
        if (returning != null) {
            for (FighterWingAPI.ReturningFighter fighter : returning) {
                if (fighter != null) register(fighter.fighter, Role.COMPONENT);
            }
        }
    }

    private void register(ShipAPI ship, Role role) {
        if (ship == null || roles.containsKey(ship)) return;
        roles.put(ship, role);
        ships.add(ship);
        managerShipsPending.add(ship);
    }

    private void demotePrimary(ShipAPI ship) {
        roles.put(ship, Role.COMPONENT);
        stamped.remove(ship);
        FleetMemberAPI member = shipMembers.get(ship);
        if (member == null) return;
        for (ShipAPI other : ships) {
            if (roles.get(other) == Role.PRIMARY && shipMembers.get(other) == member) return;
        }
        members.put(member, Role.COMPONENT);
    }

    private final class Candidate {
        final ShipAPI ship;
        final Object token;
        final FleetMemberAPI member;
        final ShipAPI parent;
        final ShipAPI droneSource;
        final FighterWingAPI wing;
        final ShipAPI wingSource;
        final boolean primaryCompatible;

        Candidate(ShipAPI ship) {
            this.ship = ship;
            token = readUnrelated(() -> ship.getCustomData().get(LINEAGE_KEY));
            member = readUnrelated(ship::getFleetMember);
            parent = readUnrelated(ship::getParentStation);
            droneSource = readUnrelated(ship::getDroneSource);
            wing = readUnrelated(ship::getWing);
            wingSource = wing == null ? null : readUnrelated(wing::getSourceShip);
            Boolean compatible = readUnrelated(() -> !ship.isStationModule()
                    && !ship.isDrone() && !ship.isFighter());
            primaryCompatible = Boolean.TRUE.equals(compatible);
        }

        Role relatedRole(Map<ShipAPI, ShipAPI> shardOrigins) {
            // Native split/shard lineage and attached/deployed children are
            // cleanup components, not evidence that the original root survived.
            if (contains(parent) || contains(droneSource) || contains(wingSource)
                    || wings.contains(wing) || contains(shardOrigins.get(ship))) {
                return Role.COMPONENT;
            }
            Role inherited = null;
            if (token == primaryLineage) inherited = Role.PRIMARY;
            else if (token == componentLineage) inherited = Role.COMPONENT;
            else if (member != null) inherited = members.get(member);
            if (inherited == Role.PRIMARY && !primaryCompatible) return Role.COMPONENT;
            return inherited;
        }

        boolean hasComponentProof(Map<ShipAPI, ShipAPI> shardOrigins,
                Map<ShipAPI, Candidate> snapshots) {
            for (ShipAPI source : explicitSources(shardOrigins)) {
                if (source != ship && contains(source)
                        && !sourcePathReaches(source, ship, shardOrigins, snapshots, identitySet())) {
                    return true;
                }
            }
            // A previously owned wing still proves fighter lineage when its
            // source has already vanished, but never demote a self-sourced root.
            return wings.contains(wing) && wingSource == null;
        }

        List<ShipAPI> explicitSources(Map<ShipAPI, ShipAPI> shardOrigins) {
            List<ShipAPI> result = new ArrayList<>();
            if (parent != null) result.add(parent);
            if (droneSource != null) result.add(droneSource);
            if (wingSource != null) result.add(wingSource);
            ShipAPI shardOrigin = shardOrigins.get(ship);
            if (shardOrigin != null) result.add(shardOrigin);
            return result;
        }
    }

    private boolean sourcePathReaches(ShipAPI current, ShipAPI target,
            Map<ShipAPI, ShipAPI> shardOrigins, Map<ShipAPI, Candidate> snapshots,
            Set<ShipAPI> visited) {
        if (current == target) return true;
        if (!contains(current) || !visited.add(current)) return false;
        Candidate snapshot = snapshots.get(current);
        if (snapshot == null) {
            snapshot = new Candidate(current);
            snapshots.put(current, snapshot);
        }
        for (ShipAPI source : snapshot.explicitSources(shardOrigins)) {
            if (sourcePathReaches(source, target, shardOrigins, snapshots, visited)) return true;
        }
        return false;
    }

    ShipAPI findAlivePrimary(CombatEngineAPI engine) {
        if (engine == null) return null;
        for (ShipAPI ship : ships) {
            if (roles.get(ship) == Role.PRIMARY && ship.isAlive()
                    && !ship.isHulk() && engine.isEntityInPlay(ship)) return ship;
        }
        return null;
    }

    /** May be retried; references and manager-removal work remain until confirmed. */
    Throwable removeBestEffort(CombatEngineAPI engine, CombatFleetManagerAPI manager) {
        Failures failures = new Failures();
        failures.capture(capture(engine, manager));
        for (ShipAPI ship : getShipsCopy()) neutralize(ship, failures);
        if (manager != null) {
            for (FighterWingAPI wing : new ArrayList<>(managerWingsPending)) {
                readKnown(() -> {
                    // Native true deals lethal damage before removing fighters;
                    // use the registry's direct entity removal instead so cleanup
                    // does not activate custom fighter damage/death mechanics.
                    manager.removeDeployed(wing, false);
                    managerWingsPending.remove(wing);
                    return null;
                }, failures);
            }
            List<ShipAPI> reverse = getShipsCopy();
            Collections.reverse(reverse);
            for (ShipAPI ship : reverse) {
                if (!managerShipsPending.contains(ship)) continue;
                readKnown(() -> {
                    if (manager.getDeployedFleetMemberEvenIfDisabled(ship) != null) {
                        manager.removeDeployed(ship, true);
                    }
                    managerShipsPending.remove(ship);
                    return null;
                }, failures);
            }
        } else {
            managerShipsPending.clear();
            managerWingsPending.clear();
        }
        if (engine != null) {
            List<ShipAPI> reverse = getShipsCopy();
            Collections.reverse(reverse);
            for (ShipAPI ship : reverse) {
                readKnown(() -> {
                    if (engine.isEntityInPlay(ship)) engine.removeEntity(ship);
                    return null;
                }, failures);
            }
        }
        // A removal callback can synchronously detach or create another entity.
        // Capture those explicit descendants before claiming this generation is
        // gone, and suppress any newly found component until the next retry.
        int previouslyCaptured = ships.size();
        failures.capture(capture(engine, manager));
        for (int index = previouslyCaptured; index < ships.size(); index++) {
            neutralize(ships.get(index), failures);
        }
        return failures.failure;
    }

    boolean isRemovalComplete(CombatEngineAPI engine) {
        if (engine == null || lastCaptureFailure != null
                || !managerShipsPending.isEmpty() || !managerWingsPending.isEmpty()) return false;
        for (ShipAPI ship : ships) {
            Boolean present = readUnrelated(() -> engine.isEntityInPlay(ship));
            if (!Boolean.FALSE.equals(present)) return false;
        }
        return true;
    }

    static void queueCleanup(CombatEngineAPI engine, MirrorLifetime lifetime) {
        if (engine == null || lifetime == null) return;
        pending(engine, true).add(lifetime);
    }

    static Throwable retryPending(CombatEngineAPI engine, CombatFleetManagerAPI manager) {
        if (engine == null) return null;
        Failures failures = new Failures();
        Set<MirrorLifetime> pending = readKnown(() -> pending(engine, false), failures);
        if (pending == null) return failures.failure;
        for (MirrorLifetime lifetime : new ArrayList<>(pending)) {
            failures.capture(lifetime.removeBestEffort(engine, manager));
            if (lifetime.isRemovalComplete(engine)) pending.remove(lifetime);
        }
        if (pending.isEmpty()) engine.getCustomData().remove(PENDING_CLEANUP_KEY);
        return failures.failure;
    }

    @SuppressWarnings("unchecked")
    private static Set<MirrorLifetime> pending(CombatEngineAPI engine, boolean create) {
        Object existing = engine.getCustomData().get(PENDING_CLEANUP_KEY);
        if (existing instanceof Set<?>) return (Set<MirrorLifetime>) existing;
        if (!create) return null;
        Set<MirrorLifetime> result = identitySet();
        engine.getCustomData().put(PENDING_CLEANUP_KEY, result);
        return result;
    }

    private static void neutralize(ShipAPI ship, Failures failures) {
        readKnown(() -> { MirrorCompatibility.forgetConstructionAI(ship); return null; }, failures);
        readKnown(() -> { ship.setSpawnDebris(false); return null; }, failures);
        readKnown(() -> { ship.setHulkChanceOverride(0f); return null; }, failures);
        readKnown(() -> { ship.setShipAI(null); return null; }, failures);
        readKnown(() -> { ship.setHoldFire(true); return null; }, failures);
        readKnown(() -> { ship.setControlsLocked(true); return null; }, failures);
        readKnown(() -> { ship.setCollisionClass(CollisionClass.NONE); return null; }, failures);
        readKnown(() -> { ship.setPhased(true); return null; }, failures);
        readKnown(() -> { ship.setAlphaMult(0f); return null; }, failures);
    }

    private static boolean containsCandidate(List<Candidate> candidates, ShipAPI ship) {
        for (Candidate candidate : candidates) if (candidate.ship == ship) return true;
        return false;
    }

    private static <T> Set<T> identitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<T, Boolean>());
    }

    private static <T> List<T> copy(List<T> values) {
        return values == null ? Collections.emptyList() : new ArrayList<>(values);
    }

    private static <T> T readKnown(Supplier<T> read, Failures failures) {
        try { return read.get(); }
        catch (Throwable failure) { failures.capture(failure); return null; }
    }

    private static <T> T readUnrelated(Supplier<T> read) {
        try { return read.get(); }
        catch (Throwable failure) {
            GautamaFinalRematchBattleCreationPlugin.rethrowIfFatal(failure);
            return null;
        }
    }

    private static final class Failures {
        Throwable failure;
        void capture(Throwable next) {
            if (next == null) return;
            GautamaFinalRematchBattleCreationPlugin.rethrowIfFatal(next);
            if (failure == null) failure = next;
            else if (failure != next) failure.addSuppressed(next);
        }
    }
}
