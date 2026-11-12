package chiefnavigator.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.threat.FragmentSwarmHullmod;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect;
import com.fs.starfarer.api.impl.combat.threat.SwarmLauncherEffect;
import com.fs.starfarer.api.impl.combat.threat.ThreatShipConstructionScript;
import org.lwjgl.util.vector.Vector2f;

/** Assembles Gautama's escorts from existing fragments, using native swarm transfers. */
public final class GautamaSwarmLaunch {
    private static final String PRIVATE_CLOUD = "chief_navigator_gautama_launch_cloud";
    private static final String PREPARED = "chief_navigator_gautama_swarm_prepared";
    private static final String ASSEMBLED = "chief_navigator_gautama_swarm_assembled";
    private static final String RESPAWN = "chief_navigator_gautama_swarm_native_respawn";

    private GautamaSwarmLaunch() { }

    /** Runs during fighter creation, before its cloud can fill at the launch bay. */
    public static void prepare(ShipAPI fighter) {
        if (fighter == null || Global.getCombatEngine() == null
                || fighter.getCustomData() == null
                || fighter.getCustomData().containsKey(PREPARED)) return;
        RoilingSwarmEffect cloud = FragmentSwarmHullmod.createSwarmFor(fighter);
        fighter.setCustomData(RESPAWN, cloud.getParams().withRespawn);
        cloud.getParams().withInitialMembers = false;
        cloud.getParams().withRespawn = false;
        fighter.setCustomData(PREPARED, true);
    }

    /** Normal carrier replacements keep their native wing, launch point and velocity. */
    public static void advance(ShipAPI fighter, float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused() || amount <= 0f
                || fighter == null || !fighter.isAlive() || fighter.isHulk()
                || Boolean.TRUE.equals(fighter.getCustomData().get(ASSEMBLED))) return;
        FighterWingAPI wing = fighter.getWing();
        ShipAPI source = wing == null ? null : wing.getSourceShip();
        if (source == null) return; // Detached reconstruction escorts bind explicitly.
        if (isGautama(source)) {
            assembleFrom(source, fighter);
        } else {
            // The custom wing can still be used by an ordinary carrier in a test/refit.
            RoilingSwarmEffect cloud = RoilingSwarmEffect.getSwarmFor(fighter);
            if (cloud != null) {
                cloud.getParams().withInitialMembers = true;
                restoreRespawn(fighter, cloud);
            }
        }
    }

    public static void maintainCloud(ShipAPI gautama, float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused() || amount <= 0f || !isGautama(gautama)) return;
        if (!gautama.isAlive() || gautama.isHulk()) {
            Object cloud = gautama.getCustomData().get(PRIVATE_CLOUD);
            if (cloud instanceof RoilingSwarmEffect) {
                ((RoilingSwarmEffect) cloud).setForceDespawn(true);
            }
        } else if (!gautama.hasTag(ThreatShipConstructionScript.SHIP_UNDER_CONSTRUCTION)) {
            sourceCloud(gautama);
        }
    }

    /** Shared by native carrier launches and the detached post-construction screen. */
    public static boolean assembleFrom(ShipAPI source, ShipAPI fighter) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused() || !isGautama(source)
                || fighter == null || !source.isAlive() || source.isHulk()
                || !fighter.isAlive() || fighter.isHulk()
                || fighter.getOwner() != source.getOwner()
                || source.hasTag(ThreatShipConstructionScript.SHIP_UNDER_CONSTRUCTION)) return false;
        if (Boolean.TRUE.equals(fighter.getCustomData().get(ASSEMBLED))) return true;
        prepare(fighter);
        RoilingSwarmEffect parent = sourceCloud(source);
        RoilingSwarmEffect child = RoilingSwarmEffect.getSwarmFor(fighter);
        if (parent == null || child == null || parent.isForceDespawn() || parent.isDespawning()) return false;

        int size = SwarmLauncherEffect.FRAGMENT_NUM.get(SwarmLauncherEffect.ATTACK_SWARM_WING);
        int needed = Math.max(0, size - child.getNumActiveMembers());
        int reserve = parent.getNumActiveMembers();
        addAtCloud(parent, Math.max(0, needed - reserve));
        Vector2f preferred = new Vector2f(fighter.getVelocity());
        preferred.scale(0.5f);
        Vector2f.add(preferred, fighter.getLocation(), preferred);
        try {
            parent.transferMembersTo(child, needed, preferred, 100f);
        } finally {
            // The carrier screen previously had no fragment-ammunition cost.
            addAtCloud(parent, Math.max(0, reserve - parent.getNumActiveMembers()));
        }
        if (child.getNumActiveMembers() < size) return false;
        restoreRespawn(fighter, child);
        fighter.setCustomData(ASSEMBLED, true);
        return true;
    }

    private static void restoreRespawn(ShipAPI fighter, RoilingSwarmEffect cloud) {
        cloud.getParams().withRespawn = Boolean.TRUE.equals(fighter.getCustomData().get(RESPAWN));
    }

    private static RoilingSwarmEffect sourceCloud(ShipAPI source) {
        Object saved = source.getCustomData().get(PRIVATE_CLOUD);
        if (saved instanceof RoilingSwarmEffect) return (RoilingSwarmEffect) saved;
        RoilingSwarmEffect cloud = RoilingSwarmEffect.getSwarmFor(source);
        if (cloud != null) return cloud;

        cloud = FragmentSwarmHullmod.createSwarmFor(source);
        // This is an assembly visual, not a new ammunition supply for Gautama's guns.
        // Never replace a registered native/construction cloud with this private one.
        if (RoilingSwarmEffect.getShipMap().get(source) == cloud) {
            RoilingSwarmEffect.getShipMap().remove(source);
        }
        RoilingSwarmEffect.getExchangeMap().remove(cloud.getParams().memberExchangeClass, cloud);
        cloud.getParams().memberExchangeClass = null;
        source.setCustomData(PRIVATE_CLOUD, cloud);
        addAtCloud(cloud, Math.max(0, cloud.getParams().initialMembers - cloud.getNumActiveMembers()));
        return cloud;
    }

    /** Native hull-outline placement, with no fresh fragments born at the body center. */
    private static void addAtCloud(RoilingSwarmEffect cloud, int count) {
        if (count <= 0) return;
        float previous = cloud.getParams().spawnOffsetMult;
        try {
            cloud.getParams().spawnOffsetMult = 1f;
            cloud.addMembers(count);
        } finally {
            cloud.getParams().spawnOffsetMult = previous;
        }
    }

    private static boolean isGautama(ShipAPI ship) {
        if (ship == null || ship.getHullSpec() == null) return false;
        String id = ship.getHullSpec().getHullId();
        return "chief_navigator_scylla".equals(id)
                || "chief_navigator_scylla_final".equals(id)
                || "chief_navigator_scylla_reincarnating".equals(id);
    }
}
