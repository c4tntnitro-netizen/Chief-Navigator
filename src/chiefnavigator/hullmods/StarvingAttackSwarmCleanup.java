package chiefnavigator.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect;
import com.fs.starfarer.api.impl.combat.threat.SwarmLauncherEffect;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Prevents detached Attack Swarms from becoming the last combatants on a
 * Starving Threat side. Once no real ship from that side remains on the map,
 * the disposable wings are removed so reserve ships can deploy immediately or
 * the battle can end and campaign replacement logic can run.
 */
public final class StarvingAttackSwarmCleanup
        extends BaseEveryFrameCombatPlugin {
    private static final String PLUGIN_KEY =
            "chief_navigator_starving_attack_swarm_cleanup";

    private final CombatEngineAPI engine;
    private final boolean[] starvingSide = new boolean[2];
    private final IntervalUtil interval = new IntervalUtil(0.15f, 0.25f);

    private StarvingAttackSwarmCleanup(CombatEngineAPI engine) {
        this.engine = engine;
    }

    /** Installs one controller and remembers which combat side owns the hull. */
    public static void ensureInstalled(CombatEngineAPI engine, int owner) {
        if (engine == null || owner < 0 || owner >= 2) return;
        Object existing = engine.getCustomData().get(PLUGIN_KEY);
        StarvingAttackSwarmCleanup plugin;
        if (existing instanceof StarvingAttackSwarmCleanup) {
            plugin = (StarvingAttackSwarmCleanup) existing;
        } else {
            plugin = new StarvingAttackSwarmCleanup(engine);
            engine.getCustomData().put(PLUGIN_KEY, plugin);
            engine.addPlugin(plugin);
        }
        plugin.starvingSide[owner] = true;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null || engine.isPaused() || amount <= 0f) return;
        interval.advance(amount);
        if (!interval.intervalElapsed()) return;

        for (int owner = 0; owner < starvingSide.length; owner++) {
            if (!starvingSide[owner]
                    || hasLiveNonFighter(engine, owner)) continue;
            removeAttackSwarms(engine, owner);
        }
    }

    static boolean hasLiveNonFighter(
            CombatEngineAPI engine, int owner) {
        if (engine == null) return false;
        for (ShipAPI ship : new ArrayList<ShipAPI>(engine.getShips())) {
            if (ship == null || ship.getOwner() != owner
                    || ship.isFighter() || ship.isHulk() || !ship.isAlive()
                    || !engine.isEntityInPlay(ship)) {
                continue;
            }
            return true;
        }
        return false;
    }

    static boolean isAttackSwarm(ShipAPI ship) {
        if (ship == null || ship.getHullSpec() == null) return false;
        if (SwarmLauncherEffect.ATTACK_SWARM_HULL.equals(
                ship.getHullSpec().getHullId())
                || SwarmLauncherEffect.ATTACK_SWARM_HULL.equals(
                        ship.getHullSpec().getBaseHullId())) {
            return true;
        }
        FighterWingAPI wing = ship.getWing();
        if (wing == null) return false;
        String wingId = wing.getWingId();
        return SwarmLauncherEffect.ATTACK_SWARM_WING.equals(wingId)
                || "chief_navigator_scylla_attack_swarm_wing".equals(wingId);
    }

    private static void removeAttackSwarms(
            CombatEngineAPI engine, int owner) {
        CombatFleetManagerAPI manager = engine.getFleetManager(owner);
        Set<FighterWingAPI> wings = Collections.newSetFromMap(
                new IdentityHashMap<FighterWingAPI, Boolean>());
        List<ShipAPI> loose = new ArrayList<ShipAPI>();

        for (ShipAPI ship : new ArrayList<ShipAPI>(engine.getShips())) {
            if (ship == null || ship.getOwner() != owner
                    || !isAttackSwarm(ship)) continue;
            RoilingSwarmEffect swarm = RoilingSwarmEffect.getSwarmFor(ship);
            if (swarm != null) {
                swarm.getParams().despawnSound = null;
                swarm.setForceDespawn(true);
            }
            FighterWingAPI wing = ship.getWing();
            if (wing != null) {
                wings.add(wing);
            } else {
                loose.add(ship);
            }
        }

        for (FighterWingAPI wing : wings) {
            boolean removed = false;
            if (manager != null) {
                try {
                    manager.removeDeployed(wing, true);
                    removed = true;
                } catch (RuntimeException failure) {
                    Global.getLogger(StarvingAttackSwarmCleanup.class).warn(
                            "Could not remove a lone Attack Swarm through its "
                                    + "fleet manager; using entity cleanup.",
                            failure);
                }
            }
            if (!removed) {
                for (ShipAPI member : new ArrayList<ShipAPI>(
                        wing.getWingMembers())) {
                    removeEntity(engine, member);
                }
            }
        }
        for (ShipAPI ship : loose) removeEntity(engine, ship);
    }

    private static void removeEntity(
            CombatEngineAPI engine, ShipAPI ship) {
        if (ship == null) return;
        ship.setSpawnDebris(false);
        ship.setHulkChanceOverride(0f);
        ship.setControlsLocked(true);
        ship.setHoldFire(true);
        ship.setCollisionClass(CollisionClass.NONE);
        ship.setPhased(true);
        ship.setAlphaMult(0f);
        if (engine.isEntityInPlay(ship)) engine.removeEntity(ship);
    }
}
