package chiefnavigator.quest;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.StarSystemAPI;

/**
 * Save-compatibility shell for the retired recycled-corridor prototype.
 *
 * Serialized copies can still exist in older campaigns, so the class remains
 * loadable but immediately completes and never modifies player movement.
 */
public final class TreadmillJourneyScript implements EveryFrameScript {
    private static final String ACTIVE = "$chief_navigator_treadmill_active";

    private final StarSystemAPI system;
    private boolean done;

    public TreadmillJourneyScript(StarSystemAPI system) {
        this.system = system;
    }

    @Override
    public void advance(float amount) {
        finish();
    }

    private void finish() {
        if (done) return;
        done = true;
        Global.getSector().getMemoryWithoutUpdate().unset(ACTIVE);
    }

    @Override public boolean isDone() { return done; }
    @Override public boolean runWhilePaused() { return false; }
}
