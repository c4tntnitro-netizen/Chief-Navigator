package chiefnavigator.abilities;

import chiefnavigator.ChiefNavigatorModPlugin;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.impl.campaign.abilities.BaseDurationAbility;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

/**
 * Playable proof of concept for Sinni's unique navigation ability.
 *
 * The fleet spends three campaign days stationary while the route is plotted,
 * then travels in a straight line at extreme speed until it intersects a
 * hyperspace storm, a slipstream, another fleet, or the prototype range cap.
 */
public final class HyperspaceOdysseyAbility extends BaseDurationAbility {
    private static final String MOD_ID = "chief_navigator_odyssey_charge";
    private static final float LAUNCH_SPEED = 1800f;
    private static final float MAX_RANGE_LY = 30f;
    private static final float FLEET_COLLISION_PADDING = 250f;

    private boolean launchPending;

    @Override
    protected void activateImpl() {
        CampaignFleetAPI fleet = getFleet();
        if (fleet == null || !isUsable()) {
            deactivate();
            return;
        }
        launchPending = true;
    }

    @Override
    protected void applyEffect(float amount, float level) {
        CampaignFleetAPI fleet = getFleet();
        if (fleet == null) return;

        if (level > 0f && level < 1f) {
            fleet.setVelocity(0f, 0f);
            fleet.getStats().getFleetwideMaxBurnMod().modifyMult(MOD_ID, 0f, "Sinni plotting an Odyssey");
        }

        if (level >= 1f && launchPending) {
            launchPending = false;
            fleet.getStats().getFleetwideMaxBurnMod().unmodify(MOD_ID);
            Global.getSector().addScript(new OdysseyLaunchScript(fleet, fleet.getFacing()));
        }
    }

    @Override
    protected void deactivateImpl() {
        cleanupImpl();
    }

    @Override
    protected void cleanupImpl() {
        CampaignFleetAPI fleet = getFleet();
        if (fleet != null) fleet.getStats().getFleetwideMaxBurnMod().unmodify(MOD_ID);
    }

    @Override
    public boolean isUsable() {
        if (!super.isUsable()) return false;
        CampaignFleetAPI fleet = getFleet();
        return fleet != null
                && fleet.isInHyperspace()
                && !fleet.isInHyperspaceTransition()
                && !Global.getSector().getMemoryWithoutUpdate()
                        .getBoolean(OdysseyLaunchScript.ACTIVE_KEY);
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        CampaignFleetAPI fleet = getFleet();
        Color bad = Misc.getNegativeHighlightColor();
        Color highlight = Misc.getHighlightColor();
        float pad = 10f;

        tooltip.addTitle("Hyperspace Odyssey (Prototype)");
        tooltip.addPara("Sinni spends three days plotting a route, locking the fleet in place. " +
                "The fleet is then slingshot forward along its current facing until it strikes " +
                "a hyperspace storm, slipstream, fleet, or the prototype range limit.", pad);
        tooltip.addPara("Maximum test range: %s light-years.", pad, highlight, "30");
        tooltip.addPara("Prototype behavior: no fuel, CR, or collision damage is applied yet.", pad);

        if (fleet != null && !fleet.isInHyperspace()) {
            tooltip.addPara("Must be used in hyperspace.", bad, pad);
        }
        if (Global.getSector().getMemoryWithoutUpdate()
                .getBoolean(OdysseyLaunchScript.ACTIVE_KEY)) {
            tooltip.addPara("An Odyssey is already in progress.", bad, pad);
        }
    }

    private static final class OdysseyLaunchScript implements EveryFrameScript {
        private static final String ACTIVE_KEY = "$chief_navigator_odyssey_active";

        private final CampaignFleetAPI fleet;
        private final Vector2f direction;
        private Vector2f lastLocation;
        private float distanceTravelled;
        private boolean done;

        private OdysseyLaunchScript(CampaignFleetAPI fleet, float facing) {
            this.fleet = fleet;
            this.direction = Misc.getUnitVectorAtDegreeAngle(facing);
            this.lastLocation = new Vector2f(fleet.getLocation());
            Global.getSector().getMemoryWithoutUpdate().set(ACTIVE_KEY, true);
        }

        @Override
        public void advance(float amount) {
            if (done || fleet == null || !fleet.isInHyperspace()
                    || fleet.isInHyperspaceTransition()) {
                finish();
                return;
            }

            distanceTravelled += Misc.getDistance(lastLocation, fleet.getLocation());
            lastLocation.set(fleet.getLocation());

            if (distanceTravelled >= MAX_RANGE_LY * Misc.getUnitsPerLightYear()
                    || hitFleet()
                    || hitSlipstream()
                    || hitStorm()) {
                finish();
                return;
            }

            fleet.setFacing(Misc.getAngleInDegrees(direction));
            fleet.setVelocity(direction.x * LAUNCH_SPEED, direction.y * LAUNCH_SPEED);
        }

        private boolean hitFleet() {
            for (CampaignFleetAPI other : fleet.getContainingLocation().getFleets()) {
                if (other == fleet || other.isDespawning()) continue;
                float collisionRange = fleet.getRadius() + other.getRadius() + FLEET_COLLISION_PADDING;
                if (Misc.getDistance(fleet, other) <= collisionRange) return true;
            }
            return false;
        }

        private boolean hitSlipstream() {
            for (CampaignTerrainAPI terrain : fleet.getContainingLocation().getTerrainCopy()) {
                String className = terrain.getPlugin().getClass().getSimpleName().toLowerCase();
                if (className.contains("slipstream") && terrain.getPlugin().containsEntity(fleet)) {
                    return true;
                }
            }
            return false;
        }

        private boolean hitStorm() {
            if (!(Misc.getHyperspaceTerrain().getPlugin() instanceof HyperspaceTerrainPlugin)) {
                return false;
            }
            HyperspaceTerrainPlugin terrain =
                    (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
            return terrain.getStateAt(fleet, 0f) == HyperspaceTerrainPlugin.LocationState.DEEP_STORM;
        }

        private void finish() {
            if (done) return;
            done = true;
            if (fleet != null) fleet.setVelocity(0f, 0f);
            Global.getSector().getMemoryWithoutUpdate().unset(ACTIVE_KEY);
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public boolean runWhilePaused() {
            return false;
        }
    }
}
