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
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;

/** Plays vanilla Shrouded Dweller encounter music while exploring Avici. */
public final class AviciMusicScript
        extends BaseCampaignEventListener implements EveryFrameScript {
    public static final String MUSIC_ID = "music_dweller_encounter_hostile";

    private boolean inAvici;
    private boolean resumeAfterCombat;

    public AviciMusicScript() {
        super(false);
    }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {
        resumeAfterCombat = true;
    }

    @Override
    public void advance(float amount) {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        StarSystemAPI avici = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.MESSINA_ID);
        LocationAPI location = player == null ? null : player.getContainingLocation();
        boolean isInAvici = avici != null && location == avici;
        boolean isInCampaign = Global.getCurrentState() == GameState.CAMPAIGN;

        if (isInAvici && isInCampaign && (!inAvici || resumeAfterCombat)) {
            playExplorationMusic(avici);
            resumeAfterCombat = false;
        } else if (!isInAvici && inAvici) {
            restoreDefaultMusic();
            resumeAfterCombat = false;
        }

        inAvici = isInAvici;
    }

    private static void playExplorationMusic(StarSystemAPI avici) {
        avici.getMemoryWithoutUpdate().set(
                MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, MUSIC_ID);
        SoundPlayerAPI soundPlayer = Global.getSoundPlayer();
        if (soundPlayer == null) return;
        // Re-request on entry and return from combat even if the reported ID
        // is stale. Native location/encounter music retains normal priority.
        soundPlayer.setSuspendDefaultMusicPlayback(false);
        soundPlayer.playCustomMusic(1, 1, MUSIC_ID, true);
    }

    private static void restoreDefaultMusic() {
        SoundPlayerAPI soundPlayer = Global.getSoundPlayer();
        if (soundPlayer == null) return;
        String current = soundPlayer.getCurrentMusicId();
        if (current != null && (current.contains(MUSIC_ID)
                || current.equals("faction_dweller_encounter.ogg"))) {
            soundPlayer.pauseCustomMusic();
        }
        soundPlayer.setSuspendDefaultMusicPlayback(false);
        soundPlayer.restartCurrentMusic();
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
