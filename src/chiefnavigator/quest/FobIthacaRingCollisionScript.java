package chiefnavigator.quest;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import java.util.ArrayList;
import org.lwjgl.util.vector.Vector2f;

/**
 * Makes FOB Ithaca's visible surviving ring physically solid in campaign.
 *
 * Custom campaign entities carry radii for selection and proximity, but the
 * engine does not treat those radii as hard collision.  This script therefore
 * resolves mobile fleets against the same 320-degree annulus represented by
 * the invisible collision beads, while preserving the forty-degree western
 * breach as the only entrance to the courtyard.
 */
public final class FobIthacaRingCollisionScript implements EveryFrameScript {
    private static final float CENTERLINE_RADIUS = 1620f;
    private static final float STRUCTURE_HALF_WIDTH = 185f;
    private static final float SOLID_ARC_START = -160f;
    private static final float SOLID_ARC_END = 160f;
    private static final float MAX_FLEET_COLLISION_RADIUS = 120f;
    private static final float SEPARATION_EPSILON = 3f;
    private boolean collisionContactsSuppressed;
    private final IntervalUtil breachDefenseInterval =
            new IntervalUtil(0.5f, 1f);
    private boolean breachDefenseChecked;

    @Override
    public void advance(float amount) {
        if (Global.getSector() == null) return;
        StarSystemAPI system = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.ASHEN_VERGE_ID);
        if (system == null) return;
        SectorEntityToken center = system.getEntityById(
                IthacaSectionEncounter.FOB_ENTITY_ID);
        if (!OdysseyExpanseSystem.isFobIthaca(center)) return;

        breachDefenseInterval.advance(amount);
        if (!breachDefenseChecked || breachDefenseInterval.intervalElapsed()) {
            OdysseyPredatorScript.maintainBreachDefenseStation(system);
            breachDefenseChecked = true;
        }

        // Migrate serialized collision beads created by builds that gave
        // them a zero-valued sensor profile. Null is required to prevent
        // close-range contact acquisition and repeated detection pings.
        if (!collisionContactsSuppressed) {
            OdysseyExpanseSystem.suppressFobIthacaRingCollisionContacts(
                    system);
            collisionContactsSuppressed = true;
        }

        // Station mode and disabled AI do not stop campaign collision from
        // translating a fleet. Keep every fightable Ithaca station on its
        // authored structure, including Breach Defense Station in the center
        // of the broken western breach.
        IthacaSectionEncounter.anchorCampaignStations(center);
        OdysseyExpanseSystem.anchorFobIthacaGateDefenseStation(
                system, center);

        for (CampaignFleetAPI fleet :
                new ArrayList<CampaignFleetAPI>(system.getFleets())) {
            if (fleet == null || fleet.isEmpty() || fleet.isDespawning()
                    || fleet.isStationMode() || isSpartanCordonFleet(fleet)) {
                continue;
            }
            resolveFleet(center, fleet);
        }
    }

    /** The authored cordon deliberately straddles the ring and controls itself. */
    private static boolean isSpartanCordonFleet(CampaignFleetAPI fleet) {
        return OdysseyExpanseSystem.isOwnedFobIthacaSpartanGuard(fleet);
    }

    private static void resolveFleet(
            SectorEntityToken center, CampaignFleetAPI fleet) {
        float dx = fleet.getLocation().x - center.getLocation().x;
        float dy = fleet.getLocation().y - center.getLocation().y;
        float distanceSquared = dx * dx + dy * dy;
        if (distanceSquared <= 0.0001f) return;

        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
        if (angle < SOLID_ARC_START || angle > SOLID_ARC_END) {
            return;
        }

        float fleetRadius = Math.max(20f, Math.min(
                MAX_FLEET_COLLISION_RADIUS, fleet.getRadius()));
        float halfWidth = STRUCTURE_HALF_WIDTH + fleetRadius;
        float innerRadius = CENTERLINE_RADIUS - halfWidth;
        float outerRadius = CENTERLINE_RADIUS + halfWidth;
        float distance = (float) Math.sqrt(distanceSquared);
        if (distance <= innerRadius || distance >= outerRadius) return;

        float nx = dx / distance;
        float ny = dy / distance;
        Vector2f velocity = fleet.getVelocity();
        float radialVelocity = velocity.x * nx + velocity.y * ny;

        // Velocity identifies the side the fleet arrived from, preventing a
        // fast frame from being projected through the wall to the other side.
        boolean arrivedFromInside;
        if (radialVelocity > 0.01f) {
            arrivedFromInside = true;
        } else if (radialVelocity < -0.01f) {
            arrivedFromInside = false;
        } else {
            arrivedFromInside = distance < CENTERLINE_RADIUS;
        }

        float resolvedRadius = arrivedFromInside
                ? innerRadius - SEPARATION_EPSILON
                : outerRadius + SEPARATION_EPSILON;
        fleet.setLocation(
                center.getLocation().x + nx * resolvedRadius,
                center.getLocation().y + ny * resolvedRadius);

        boolean movingIntoWall = arrivedFromInside
                ? radialVelocity > 0f : radialVelocity < 0f;
        if (movingIntoWall) {
            fleet.setVelocity(
                    velocity.x - radialVelocity * nx,
                    velocity.y - radialVelocity * ny);
        }
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }
}
