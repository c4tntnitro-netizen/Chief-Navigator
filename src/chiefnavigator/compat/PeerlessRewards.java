package chiefnavigator.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;

/** Optional story reward; no Peerless types are resolved when its mod is absent. */
public final class PeerlessRewards {
    private PeerlessRewards() { }

    public static boolean awardOnce(String receipt) {
        SettingsAPI settings = Global.getSettings();
        if (settings == null || settings.getModManager() == null
                || !settings.getModManager().isModEnabled("peerless_skills")) return false;
        try {
            return PeerlessRewardsBridge.awardOnce(receipt);
        } catch (RuntimeException | LinkageError unavailable) {
            Global.getLogger(PeerlessRewards.class).warn(
                    "Optional Peerless reward unavailable; story completion is preserved.", unavailable);
            return false;
        }
    }
}
