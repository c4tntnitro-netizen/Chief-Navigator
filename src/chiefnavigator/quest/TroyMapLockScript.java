package chiefnavigator.quest;

import com.fs.starfarer.api.EveryFrameScript;
/** Save-compatibility shell for the retired treadmill map lock. */
public final class TroyMapLockScript implements EveryFrameScript {
    @Override
    public void advance(float amount) {
        // Intentionally inert. Older saves may deserialize this class before
        // TroyArrivalScript gets a chance to remove the script.
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }
}
