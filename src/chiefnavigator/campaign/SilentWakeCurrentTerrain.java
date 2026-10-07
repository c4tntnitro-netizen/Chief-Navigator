package chiefnavigator.campaign;

import chiefnavigator.quest.OdysseyExpanseSystem;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.terrain.BaseTerrain;
import com.fs.starfarer.api.impl.campaign.terrain.NebulaTerrainPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import java.util.EnumSet;
import org.lwjgl.util.vector.Vector2f;

/** System-wide sensor haze and gentle southward campaign fleet advection. */
public final class SilentWakeCurrentTerrain extends BaseTerrain {
    public static final String ID = "chief_navigator_silent_wake_current";
    private static final String SENSOR_MOD_ID = ID + "_sensors";
    private static final float DRIFT_SPEED = 30f;
    private transient int diagnosticAdvances;
    private transient int diagnosticApplications;
    private transient float diagnosticImpulse;
    private transient boolean diagnosticSteering;
    private transient CampaignFleetAPI trackedFleet;
    private transient Vector2f reachedDestination;
    private transient boolean wasAIMode;

    @Override
    public void advance(float amount) {
        diagnosticAdvances++;
        // Sensor haze covers the whole system, including sheltered wakes and
        // space outside the visible river. Movement retains its own bounds.
        if (amount > 0f && !Global.getSector().isPaused()) {
            LocationAPI location = entity.getContainingLocation();
            if (OdysseyExpanseSystem.SILENT_WAKE_ID.equals(location.getId())) {
                for (CampaignFleetAPI fleet : location.getFleets()) {
                    if (fleet.isInHyperspaceTransition()
                            || fleet.getContainingLocation() != location) continue;
                    // Native temporary modifiers expire after leaving Sanzu;
                    // refreshing the same key cannot compound the penalty.
                    fleet.getStats().addTemporaryModMult(
                            0.1f, SENSOR_MOD_ID, "Sanzu nebula",
                            NebulaTerrainPlugin.VISIBLITY_MULT,
                            fleet.getStats().getSensorRangeMod());
                }
            }
        }
        super.advance(amount);
    }

    public String diagnosticState(CampaignFleetAPI fleet) {
        return "terrainTicks=" + diagnosticAdvances
                + " applications=" + diagnosticApplications
                + " exposed=" + containsPoint(fleet.getLocation(), 0f)
                + " locked=" + isPreventedFromAffecting(fleet)
                + " stacked=" + fleet.getMemoryWithoutUpdate().contains("$terrain_" + ID)
                + " origin=" + entity.getLocation()
                + " lastImpulse=" + diagnosticImpulse
                + " steering=" + diagnosticSteering;
    }

    @Override public String getEffectCategory() { return ID; }
    @Override public String getTerrainName() { return "Sanzu Current"; }
    @Override public boolean canPlayerHoldStationIn() { return false; }

    @Override
    public EnumSet<CampaignEngineLayers> getActiveLayers() {
        // The separate flow entity draws the clouds; this terrain only moves fleets.
        return EnumSet.noneOf(CampaignEngineLayers.class);
    }

    @Override
    public float getRenderRange() {
        // BaseTerrain also uses this radius to decide which fleets receive effects.
        // Cover the complete 23000 x 21000 river, including its corners.
        return 25000f;
    }

    @Override
    public boolean containsPoint(Vector2f point, float radius) {
        return SilentWakeFlowEntityPlugin.isInExposedCurrent(
                point.x - entity.getLocation().x, point.y - entity.getLocation().y,
                OdysseyExpanseSystem.getSilentWakeObstacles());
    }

    @Override
    public void applyEffect(SectorEntityToken target, float days) {
        if (days <= 0f || Global.getSector().isPaused()
                || target != Global.getSector().getPlayerFleet()
                || !(target instanceof CampaignFleetAPI)) return;
        CampaignFleetAPI fleet = (CampaignFleetAPI) target;
        if (fleet.isInHyperspaceTransition()
                || fleet.getContainingLocation() != entity.getContainingLocation()
                || !containsPoint(fleet.getLocation(), 0f)) return;

        float seconds = days * Global.getSector().getClock().getSecondsPerDay();
        CampaignUIAPI ui = Global.getSector().getCampaignUI();
        boolean steering = hasActiveMovementOrder(fleet, ui);
        float drift = DRIFT_SPEED * seconds;
        Vector2f destination = fleet.getMoveDestination();
        Vector2f driftingHoldPoint = !steering && destination != null
                ? new Vector2f(destination.x, destination.y - drift) : null;
        // setLocation synchronizes BOTH the campaign entity and its movement
        // module. Editing getLocation().y alone is discarded by the next update.
        // Apply the same current regardless of steering, without changing thrust.
        if (fleet.getOrbit() != null) fleet.setOrbit(null);
        fleet.setLocation(fleet.getLocation().x, fleet.getLocation().y - drift);
        if (driftingHoldPoint != null) {
            // A reached destination is just an idle hold point. Carry it with
            // the fleet so automatic braking does not command a return upstream.
            // Active player destinations remain fixed in world coordinates.
            fleet.setMoveDestination(driftingHoldPoint.x, driftingHoldPoint.y);
            reachedDestination = driftingHoldPoint;
        }
        diagnosticApplications++;
        diagnosticImpulse = drift;
        diagnosticSteering = steering;
    }

    private boolean hasActiveMovementOrder(CampaignFleetAPI fleet, CampaignUIAPI ui) {
        if (trackedFleet != fleet) {
            trackedFleet = fleet;
            reachedDestination = null;
            wasAIMode = false;
        }
        boolean newAutopilotOrder = fleet.isAIMode() && !wasAIMode;
        wasAIMode = fleet.isAIMode();
        Vector2f destination = fleet.getMoveDestination();
        if ((ui != null && ui.isPlayerFleetFollowingMouse()) || newAutopilotOrder) {
            reachedDestination = null;
            return true;
        }
        if (destination == null) return false;
        // isFollowingDirectCommand remains true after arrival. Remember the
        // reached hold point so drifting away does not turn it back into an
        // active order. A changed destination or fresh mouse input releases it.
        if (reachedDestination != null
                && Vector2f.sub(destination, reachedDestination, null).lengthSquared() < 1f) {
            return false;
        }
        reachedDestination = null;
        if (Vector2f.sub(destination, fleet.getLocation(), null).lengthSquared() <= 25f
                && fleet.getVelocityFromMovementModule().lengthSquared()
                        <= DRIFT_SPEED * DRIFT_SPEED) {
            reachedDestination = new Vector2f(destination);
            return false;
        }
        return true;
    }

    @Override public boolean hasTooltip() { return true; }

    @Override public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        tooltip.addTitle(getTerrainName());
        tooltip.addPara("A gentle current carries your fleet south. The downstream "
                + "lee of the white dwarf and planets provides shelter. "
                + "Nebular haze halves every fleet's sensor range throughout "
                + "Sanzu, including sheltered areas.", 10f);
    }
}
