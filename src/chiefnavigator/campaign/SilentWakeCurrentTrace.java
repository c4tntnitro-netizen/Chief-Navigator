package chiefnavigator.campaign;

import chiefnavigator.quest.OdysseyExpanseSystem;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import org.lwjgl.util.vector.Vector2f;

/** Bounded live diagnostic: observe campaign dispatch and movement without changing either. */
public final class SilentWakeCurrentTrace implements EveryFrameScript {
    private int samples;
    private float elapsed;
    private Vector2f previous;

    @Override public boolean isDone() { return samples >= 30; }
    @Override public boolean runWhilePaused() { return false; }

    @Override
    public void advance(float amount) {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null || Global.getSector().isPaused()) return;
        if (!(fleet.getContainingLocation() instanceof StarSystemAPI)
                || !OdysseyExpanseSystem.SILENT_WAKE_ID.equals(
                        ((StarSystemAPI) fleet.getContainingLocation()).getOptionalUniqueId())) return;
        elapsed += amount;
        if (elapsed < 5f && samples > 0) return;
        float window = elapsed;
        elapsed = 0f;
        String terrainState = "terrain=MISSING";
        int matches = 0;
        for (CampaignTerrainAPI terrain : fleet.getContainingLocation().getTerrainCopy()) {
            if (terrain.getPlugin() instanceof SilentWakeCurrentTerrain) {
                matches++;
                terrainState = ((SilentWakeCurrentTerrain) terrain.getPlugin()).diagnosticState(fleet);
            }
        }
        CampaignUIAPI ui = Global.getSector().getCampaignUI();
        Vector2f position = fleet.getLocation();
        Global.getLogger(SilentWakeCurrentTrace.class).info(
                "SILENT_WAKE_TRACE_V2 sample=" + samples + " window=" + window
                + " position=" + position
                + " moved=" + (previous == null ? "first" : Vector2f.sub(position, previous, null))
                + " velocity=" + fleet.getVelocity()
                + " movementVelocity=" + fleet.getVelocityFromMovementModule()
                + " destination=" + fleet.getMoveDestination()
                + " acceleration=" + fleet.getAcceleration()
                + " ai=" + fleet.isAIMode()
                + " direct=" + ui.isFollowingDirectCommand()
                + " mouse=" + ui.isPlayerFleetFollowingMouse()
                + " transitioning=" + fleet.isInHyperspaceTransition()
                + " orbit=" + (fleet.getOrbit() != null)
                + " fleetListed=" + fleet.getContainingLocation().getFleets().contains(fleet)
                + " terrainCount=" + matches + " " + terrainState);
        previous = new Vector2f(position);
        samples++;
    }
}
