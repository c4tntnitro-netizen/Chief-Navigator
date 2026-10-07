package chiefnavigator.quest;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.IntervalUtil;

/** Repairs Sinni's native event creator in both new and existing campaigns. */
public final class SinniBarEventAvailabilityScript implements EveryFrameScript {
    private final IntervalUtil interval = new IntervalUtil(0.5f, 1f);

    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return false; }

    @Override
    public void advance(float amount) {
        if (Global.getSector() == null) return;
        interval.advance(Global.getSector().getClock().convertToDays(amount));
        if (interval.intervalElapsed()) {
            SinniBarEvent.ensureCreatorRegistered();
            if (Global.getSector().getMemoryWithoutUpdate().getBoolean(
                    SinniBarEvent.STARTED)) {
                SinniContact.ensureContact(null);
            }
        }
    }
}
