package chiefnavigator.quest;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;

/** One-shot reveal reconciliation after load or an actual cargo event. */
public final class AviciResearchStationScript implements EveryFrameScript {
    private boolean done;

    public static void install() {
        if (Global.getSector() == null) return;
        Global.getSector().removeTransientScriptsOfClass(
                AviciResearchStationScript.class);
        Global.getSector().addTransientScript(new AviciResearchStationScript());
    }

    @Override
    public void advance(float amount) {
        if (done || Global.getSector() == null || Global.getSector().isPaused()
                || amount <= 0f) return;
        OdysseyExpanseSystem.introduceAviciResearchStation();
        done = true;
    }

    @Override public boolean isDone() { return done; }
    @Override public boolean runWhilePaused() { return false; }
}
