package chiefnavigator.campaign;

import chiefnavigator.quest.OdysseyExpanseSystem;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;

/** Owns zippy's "Watcher of the Cycle -Scene02-" while in Sanzu. */
public final class SanzuMusicScript
        extends BaseCampaignEventListener implements EveryFrameScript {
    public static final String MUSIC_ID =
            "chief_navigator_sanzu_exploration";

    private boolean inSanzu;
    private boolean resumeAfterCombat;

    public SanzuMusicScript() {
        super(false);
    }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {
        resumeAfterCombat = true;
    }

    @Override
    public void advance(float amount) {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        LocationAPI location = player == null
                ? null : player.getContainingLocation();
        boolean isInSanzu = isSanzu(location);
        boolean isInCampaign = Global.getCurrentState() == GameState.CAMPAIGN;

        if (isInSanzu && isInCampaign
                && (!inSanzu || resumeAfterCombat)) {
            playExplorationMusic();
            resumeAfterCombat = false;
        } else if (!isInSanzu && inSanzu) {
            restoreDefaultMusic();
            resumeAfterCombat = false;
        }

        inSanzu = isInSanzu;
    }

    private static boolean isSanzu(LocationAPI location) {
        return location instanceof StarSystemAPI
                && OdysseyExpanseSystem.SILENT_WAKE_ID.equals(
                        ((StarSystemAPI) location).getOptionalUniqueId());
    }

    private static void playExplorationMusic() {
        SoundPlayerAPI soundPlayer = Global.getSoundPlayer();
        if (soundPlayer == null) {
            return;
        }
        // Combat replaces the campaign stream. Re-request the cue whenever
        // campaign control returns instead of trusting the stale music ID.
        soundPlayer.setSuspendDefaultMusicPlayback(true);
        soundPlayer.playCustomMusic(1, 1, MUSIC_ID, true);
    }

    private static void restoreDefaultMusic() {
        SoundPlayerAPI soundPlayer = Global.getSoundPlayer();
        if (soundPlayer == null) {
            return;
        }
        if (matches(soundPlayer.getCurrentMusicId())) {
            soundPlayer.pauseCustomMusic();
        }
        soundPlayer.setSuspendDefaultMusicPlayback(false);
        soundPlayer.restartCurrentMusic();
    }

    private static boolean matches(String current) {
        return current != null && (current.equals(MUSIC_ID)
                || current.equals(MUSIC_ID + ".ogg")
                || current.contains(MUSIC_ID));
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }
}
