package chiefnavigator.quest;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.IntervalUtil;

/** Reveals the final research station after the six original parts are recovered. */
public final class AviciResearchStationScript implements EveryFrameScript {
    private final IntervalUtil interval = new IntervalUtil(1f, 1.5f);

    public static void install() {
        if (Global.getSector() == null) return;
        Global.getSector().removeTransientScriptsOfClass(
                AviciResearchStationScript.class);
        Global.getSector().addTransientScript(new AviciResearchStationScript());
    }

    @Override
    public void advance(float amount) {
        if (Global.getSector() == null || Global.getSector().isPaused()
                || amount <= 0f) return;
        interval.advance(amount);
        if (interval.intervalElapsed()) {
            OdysseyExpanseSystem.introduceAviciResearchStation();
        }
    }

    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return false; }
}
