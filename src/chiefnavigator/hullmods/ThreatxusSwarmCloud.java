package chiefnavigator.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.combat.threat.FragmentSwarmHullmod;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect.RoilingSwarmParams;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect.SwarmMember;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect.SwarmMemberOffsetModifier;
import com.fs.starfarer.api.impl.combat.threat.ThreatShipConstructionScript;
import com.fs.starfarer.api.util.Misc;
import chiefnavigator.ai.ExodusAssaultAI;
import chiefnavigator.systems.ThreatxusFabricationSystem;

/** Threatxus's private broad swarm, concentrated core and quiet forward drift. */
public final class ThreatxusSwarmCloud extends BaseHullMod {
    public static final String HULL_ID = "chief_navigator_threatxus";
    public static final String HULLMOD_ID = "chief_navigator_threatxus_swarm_cloud";
    public static final String CLOUD_KEY = HULLMOD_ID + "_visual";
    public static final int DENSITY_MULTIPLIER = 10;
    public static final int EXTRA_CORE_MEMBERS = 500;
    private static final String READY_KEY = HULLMOD_ID + "_ready";

    private static boolean isThreatxus(ShipHullSpecAPI hull) {
        return hull != null && (HULL_ID.equals(hull.getHullId()) || HULL_ID.equals(hull.getBaseHullId())
                || HULL_ID.equals(hull.getDParentHullId()));
    }

    private static boolean isAssault(ShipHullSpecAPI hull) {
        return hull != null && (ThreatxusFabricationSystem.ASSAULT_HULL_ID.equals(hull.getHullId())
                || ThreatxusFabricationSystem.ASSAULT_HULL_ID.equals(hull.getBaseHullId())
                || ThreatxusFabricationSystem.ASSAULT_HULL_ID.equals(hull.getDParentHullId()));
    }

    @Override
    public void applyEffectsBeforeShipCreation(HullSize size, MutableShipStatsAPI stats, String id) {
        if (stats != null && stats.getVariant() != null && isThreatxus(stats.getVariant().getHullSpec())) {
            // Keep the intended ten-unit drift when the ship has no flux.
            stats.getZeroFluxSpeedBoost().modifyMult(HULLMOD_ID, 0f);
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship != null && Global.getCombatEngine() != null && isAssault(ship.getHullSpec())) {
            // FleetManager advances new ships before returning them to the
            // construction script. Gate the system/AI before those warmup ticks.
            ship.addTag(ThreatShipConstructionScript.SHIP_UNDER_CONSTRUCTION);
            ship.setAlphaMult(0f);
        }
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (ship == null || !(isThreatxus(ship.getHullSpec()) || isAssault(ship.getHullSpec())) || engine == null
                || engine.isPaused() || amount <= 0f) return;
        RoilingSwarmEffect cloud = (RoilingSwarmEffect) ship.getCustomData().get(CLOUD_KEY);
        if (!ship.isAlive() || ship.isHulk() || !engine.isEntityInPlay(ship)) {
            if (cloud != null) cloud.setForceDespawn(true);
            return;
        }
        boolean assault = isAssault(ship.getHullSpec());
        boolean building = ship.hasTag(ThreatShipConstructionScript.SHIP_UNDER_CONSTRUCTION);
        if (cloud == null) {
            RoilingSwarmParams p = new RoilingSwarmParams();
            p.minOffset = 0f;
            p.maxOffset = Math.min(100f, ship.getCollisionRadius() * .5f);
            p.generateOffsetAroundAttachedEntityOval = true;
            p.spawnOffsetMult = .33f;
            p.spawnOffsetMultForInitialSpawn = 1f;
            if (!assault) {
                // A broad filled cloud, with density falling smoothly to zero,
                // instead of the native hull-following band and its hard rim.
                p.generateOffsetAroundAttachedEntityOval = false;
                p.maxOffset = ship.getCollisionRadius() * 3.5f;
                p.offsetModifier = new DiffuseOffset(p.maxOffset);
            }
            p.maxSpeed = ship.getMaxSpeedWithoutBoost()
                    + Math.max(ship.getMaxSpeedWithoutBoost() * .25f + 50f, 100f);
            p.initialMembers = FragmentSwarmHullmod.getBaseSwarmSize(ship.getHullSize()) * DENSITY_MULTIPLIER;
            if (!assault) p.initialMembers += EXTRA_CORE_MEMBERS;
            p.baseMembersToMaintain = p.initialMembers;
            p.maxNumMembersToAlwaysRemoveAbove = p.initialMembers * 3 / 2;
            p.removeMembersAboveMaintainLevel = false;
            p.memberRespawnRate = FragmentSwarmHullmod.getBaseSwarmRespawnRateMult(ship.getHullSize()) * DENSITY_MULTIPLIER;
            if (!assault) p.memberRespawnRate *= 1.5f;
            p.despawnSound = null;
            p.flashRateMult = .25f;
            p.flashCoreRadiusMult = 0f;
            p.flashRadius = 120f;
            p.withInitialMembers = !building;
            p.withRespawn = !building;
            // Constructor registration is temporary; this cloud supplies no
            // ammunition and neither exchanges nor flocks with other swarms.
            RoilingSwarmEffect previous = RoilingSwarmEffect.getShipMap().get(ship);
            cloud = new VisualCloud(ship, p);
            if (!assault) {
                RoilingSwarmEffect.getShipMap().remove(ship, cloud);
                if (previous != null) RoilingSwarmEffect.getShipMap().put(ship, previous);
            }
            ship.setCustomData(CLOUD_KEY, cloud);
        }
        cloud.getParams().alphaMult = ship.getAlphaMult();
        if (assault) {
            if (building) {
                cloud.getParams().withInitialMembers = false;
                cloud.getParams().withRespawn = false;
                ship.setCustomData(READY_KEY, false);
            } else {
                if (!Boolean.TRUE.equals(ship.getCustomData().get(READY_KEY))) {
                    cloud.getParams().withInitialMembers = true;
                    cloud.getParams().withRespawn = true;
                    ship.setCustomData(READY_KEY, true);
                }
                if (engine.getPlayerShip() != ship && !(ship.getShipAI() instanceof ExodusAssaultAI)) {
                    ship.setShipAI(new ExodusAssaultAI(ship, engine));
                }
            }
            return;
        }
        // Normal native acceleration/collision physics, without teleporting or
        // overwriting momentum. There are no engine slots to render flames.
        ship.blockCommandForOneFrame(ShipCommand.ACCELERATE_BACKWARDS);
        ship.blockCommandForOneFrame(ShipCommand.DECELERATE);
        ship.blockCommandForOneFrame(ShipCommand.STRAFE_LEFT);
        ship.blockCommandForOneFrame(ShipCommand.STRAFE_RIGHT);
        ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
    }

    /** Preserve the broad cloud and add a half-radius core with twice its peak density. */
    private static final class DiffuseOffset implements SwarmMemberOffsetModifier {
        private final float radius;
        DiffuseOffset(float radius) {
            this.radius = radius;
        }
        @Override
        public void modifyOffset(SwarmMember member) {
            // Inverse radial CDF: 1-(1-r*r)^3. Density and its slope both
            // reach zero at the outskirts; no piled-up or clipped edge.
            double r = Math.sqrt(1.0 - Math.cbrt(1.0 - Misc.random.nextFloat()));
            // 1000 broad members +500 in a quarter-area core: the added
            // component has twice the broad component's central density.
            // Together they triple the center without changing the outskirts.
            if (Misc.random.nextFloat() < 1f / 3f) r *= .5;
            double angle = Misc.random.nextDouble() * Math.PI * 2.0;
            member.offset.set((float) (Math.cos(angle) * r * radius),
                    (float) (Math.sin(angle) * r * radius * .9f));
        }
    }

    /** Native expiry must not unmap a different, registered ammunition cloud. */
    private static final class VisualCloud extends RoilingSwarmEffect {
        private final ShipAPI ship;
        VisualCloud(ShipAPI ship, RoilingSwarmParams params) {
            super(ship, params);
            this.ship = ship;
        }
        @Override
        public boolean isExpired() {
            RoilingSwarmEffect registered = getShipMap().get(ship);
            boolean expired = super.isExpired();
            if (expired && registered != null && registered != this) getShipMap().put(ship, registered);
            return expired;
        }
    }
}
