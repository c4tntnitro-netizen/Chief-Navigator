package chiefnavigator.quest;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;

/** Save-migration cleanup for the retired isolated-room travel lock. */
public final class OdysseyExpanseControlScript implements EveryFrameScript {
    private static final String LOCK_ACTIVE = "$chief_navigator_odyssey_jump_lock";
    private static final String HAD_TRANSVERSE_JUMP =
            "$chief_navigator_odyssey_had_transverse_jump";

    @Override
    public void advance(float amount) {
        restoreTransverseJump();
    }

    /** Retained for binary/save compatibility; the Abyss cluster is not locked. */
    public static void lockTransverseJump() {
        restoreTransverseJump();
    }

    public static void restoreTransverseJump() {
        if (Global.getSector() == null) return;
        // Retire only this mod's legacy bookkeeping. Reconstructing a player
        // ability from stale save flags can overwrite another mod's intended
        // ability state, so serialized player state remains authoritative.
        Global.getSector().getMemoryWithoutUpdate().unset(HAD_TRANSVERSE_JUMP);
        Global.getSector().getMemoryWithoutUpdate().unset(LOCK_ACTIVE);
    }

    @Override public boolean isDone() { return true; }
    @Override public boolean runWhilePaused() { return true; }
}
