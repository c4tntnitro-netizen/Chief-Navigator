package chiefnavigator.quest;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

/**
 * One More Horizon reward: in hyperspace, burn bonuses keep counting past the
 * engine's limit of 20, up to 30.
 *
 * The engine clamps FleetData.getBurnLevel() to 20 (literal in
 * starfarer_obf.jar). Vanilla storm strikes pass that limit with
 * HyperStormBoost, a short fleet script that adds velocity each frame until
 * the fleet reaches maxStormStrikeBurn (30). This is the same push, held while
 * the fleet is in hyperspace and actually travelling.
 *
 * The fleet's own burn is the engine formula without the clamp:
 * fleetwide max burn mod applied to the slowest ship's burn. Once the fleet is
 * at its normal top speed and heading for a destination, it is carried along
 * its current heading, ramping at its own acceleration, up to that burn
 * (capped at 30). The push is invisible: no glow, no jolt. It never steers,
 * stops for hard turns, stopping and Go Slow, and does nothing for fleets
 * whose burn is 20 or less.
 *
 * Lifecycle: a transient sector script started by the once-per-second vignette
 * rewards pass when the fleet is in hyperspace; it ends itself on leaving
 * hyperspace, so it is never saved and never runs in realspace.
 */
final class SinniBurnCap implements EveryFrameScript {
    static final float MAX_BURN = 30f;
    static final float ENGINE_CAP = 20f;
    static final float MIN_MOVING_SPEED = 10f;
    /** Heading must point within about 45 degrees of the destination. */
    static final float ALIGNMENT_DOT = 0.7f;
    /** Leave this many seconds of travel before a destination unpushed. */
    static final float STOPPING_SECONDS = 2f;
    /** Start only once the fleet has reached its normal top speed. */
    static final float TOP_SPEED_FRACTION = 0.97f;

    private final CampaignFleetAPI fleet;
    private boolean done;
    /** Speed the fleet is being carried at above its normal top speed. */
    private float cruise;

    private SinniBurnCap(CampaignFleetAPI fleet) {
        this.fleet = fleet;
    }

    /** Called once per second by the vignette rewards pass. */
    static void ensureRunning(CampaignFleetAPI fleet) {
        if (fleet == null || Global.getSector() == null) return;
        if (!SinniSystemVignetteScript.isOneMoreHorizonComplete()) return;
        if (!fleet.isInHyperspace()) return;
        if (Global.getSector().hasTransientScript(SinniBurnCap.class)) return;
        Global.getSector().addTransientScript(new SinniBurnCap(fleet));
    }

    /** The engine's burn formula without its clamp to 20. */
    static float uncappedBurn(CampaignFleetAPI fleet) {
        return fleet.getStats().getFleetwideMaxBurnMod()
                .computeEffective(fleet.getFleetData().getMinBurnLevel());
    }

    static float targetBurn(CampaignFleetAPI fleet) {
        return Math.min(MAX_BURN, uncappedBurn(fleet));
    }

    @Override
    public void advance(float amount) {
        if (done) return;
        if (Global.getSector().getPlayerFleet() != fleet
                || !fleet.isInHyperspace()
                || !SinniSystemVignetteScript.isOneMoreHorizonComplete()) {
            done = true;
            return;
        }
        if (amount <= 0f) return;
        if (fleet.isInHyperspaceTransition() || fleet.getBattle() != null
                || fleet.wasSlowMoving()) {
            cruise = 0f;
            return;
        }
        float target = targetBurn(fleet);
        if (target <= ENGINE_CAP || !shouldCarry()) {
            cruise = 0f;
            return;
        }
        Vector2f vel = fleet.getVelocity();
        float speed = vel.length();
        float topSpeed = fleet.getTravelSpeed();
        if (cruise <= 0f) {
            if (speed < topSpeed * TOP_SPEED_FRACTION) return;
            cruise = Math.max(speed, topSpeed);
        }
        // Same proportion between burn and speed as the fleet's normal top
        // speed, so any travel-speed bonus carries over.
        float cappedBurn = Math.max(1f, fleet.getFleetData().getBurnLevel());
        float targetSpeed = topSpeed * Misc.getSpeedForBurnLevel(target)
                / Misc.getSpeedForBurnLevel(cappedBurn);
        if (targetSpeed <= topSpeed) {
            cruise = 0f;
            return;
        }
        // Ramp at the fleet's own acceleration, like any burn increase.
        cruise = Math.min(targetSpeed, cruise + fleet.getAcceleration() * amount);
        // The movement module brakes overspeed by about (acceleration + top
        // speed) per second before it moves the fleet; add that back so the
        // fleet actually travels at the cruise speed, steadily.
        float set = cruise + (fleet.getAcceleration() + topSpeed) * amount;
        float scale = set / Math.max(speed, 1f);
        fleet.setVelocity(vel.x * scale, vel.y * scale);
    }

    /** Travelling at speed toward a destination, not turning hard or stopping. */
    private boolean shouldCarry() {
        Vector2f vel = fleet.getVelocity();
        float speed = vel.length();
        if (speed < MIN_MOVING_SPEED) return false;
        Vector2f dest = fleet.getMoveDestination();
        if (dest == null) return false;
        CampaignUIAPI ui = Global.getSector().getCampaignUI();
        boolean followingMouse = ui != null && ui.isPlayerFleetFollowingMouse();
        float distance = Misc.getDistance(fleet.getLocation(), dest);
        float stopping = Misc.getSpeedForBurnLevel(MAX_BURN) * STOPPING_SECONDS;
        if (!followingMouse && distance < stopping) return false;
        if (distance > 1f) {
            Vector2f heading = new Vector2f(vel.x / speed, vel.y / speed);
            Vector2f toDest = Misc.getUnitVector(fleet.getLocation(), dest);
            if (Vector2f.dot(heading, toDest) < ALIGNMENT_DOT) return false;
        }
        return true;
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
