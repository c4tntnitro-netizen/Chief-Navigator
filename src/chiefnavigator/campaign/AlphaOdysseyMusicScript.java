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

/** Owns Ryini Beats' "Rising Up" while the player explores Alpha Odyssey. */
public final class AlphaOdysseyMusicScript
        extends BaseCampaignEventListener implements EveryFrameScript {
    public static final String MUSIC_ID =
            "chief_navigator_alpha_odyssey_exploration";

    private boolean inAlphaOdyssey;
    private boolean resumeAfterCombat;

    public AlphaOdysseyMusicScript() {
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
        boolean isInAlphaOdyssey = isAlphaOdyssey(location);
        boolean isInCampaign = Global.getCurrentState() == GameState.CAMPAIGN;

        if (isInAlphaOdyssey && isInCampaign
                && (!inAlphaOdyssey || resumeAfterCombat)) {
            playExplorationMusic();
            resumeAfterCombat = false;
        } else if (!isInAlphaOdyssey && inAlphaOdyssey) {
            restoreDefaultMusic();
            resumeAfterCombat = false;
        }

        inAlphaOdyssey = isInAlphaOdyssey;
    }

    private static boolean isAlphaOdyssey(LocationAPI location) {
        return location instanceof StarSystemAPI
                && OdysseyExpanseSystem.SYSTEM_ID.equals(
                        ((StarSystemAPI) location).getOptionalUniqueId());
    }

    private static void playExplorationMusic() {
        SoundPlayerAPI soundPlayer = Global.getSoundPlayer();
        if (soundPlayer == null) {
            return;
        }
        // Request on every entry and every return from combat. Starsector can
        // leave a stale current-music ID behind after discarding its streamer,
        // so matching the reported ID is not proof that the track is audible.
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
