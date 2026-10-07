package chiefnavigator.quest;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import java.util.function.LongSupplier;

/** A scene-local soundtrack timer, including while the campaign is paused. */
final class MenelausEndingMusic implements EveryFrameScript {
    static final String MUSIC_ID = "chief_navigator_sinni_ending";
    private static final long DELAY_NANOS = 10_000_000_000L;

    private final SectorAPI sector;
    private final InteractionDialogAPI dialog;
    private final LongSupplier clock;
    private final long openedAt;
    private SoundPlayerAPI musicPlayer;
    private boolean requested;
    private boolean done;

    static void start(InteractionDialogAPI dialog) {
        SectorAPI sector = Global.getSector();
        if (sector != null && dialog != null) {
            sector.addTransientScript(new MenelausEndingMusic(
                    sector, dialog, System::nanoTime));
        }
    }

    MenelausEndingMusic(
            SectorAPI sector, InteractionDialogAPI dialog, LongSupplier clock) {
        this.sector = sector;
        this.dialog = dialog;
        this.clock = clock;
        openedAt = clock.getAsLong();
        try {
            musicPlayer = Global.getSoundPlayer();
            if (musicPlayer == null) {
                done = true;
                return;
            }
            // Suspension prevents new campaign tracks; the null cue also cuts
            // the currently playing track immediately, without a fade.
            musicPlayer.setSuspendDefaultMusicPlayback(true);
            musicPlayer.playCustomMusic(0, 0, null);
        } catch (RuntimeException ex) {
            Global.getLogger(MenelausEndingMusic.class).warn(
                    "Could not silence music for the Labor V ending", ex);
            finish();
        }
    }

    @Override
    public void advance(float amount) {
        if (done) return;
        CampaignUIAPI ui = sector.getCampaignUI();
        if (Global.getSector() != sector || Global.getSoundPlayer() != musicPlayer
                || ui == null
                || ui.getCurrentInteractionDialog() != dialog) {
            finish();
            return;
        }
        // Campaign time, frame size and dialogue pagination cannot accelerate
        // or reset this real-time delay. No sleeping on the UI thread.
        if (requested || clock.getAsLong() - openedAt < DELAY_NANOS) return;
        requested = true;
        try {
            musicPlayer.playCustomMusic(1, 1, MUSIC_ID, true);
        } catch (RuntimeException ex) {
            Global.getLogger(MenelausEndingMusic.class).warn(
                    "Could not start the Labor V ending soundtrack", ex);
            finish();
        }
    }

    private void finish() {
        done = true;
        if (musicPlayer == null) return;
        SoundPlayerAPI player = musicPlayer;
        musicPlayer = null;
        // Attempt every release step even if an earlier audio call fails.
        try {
            player.pauseCustomMusic();
        } catch (RuntimeException ex) {
            logReleaseFailure(ex);
        }
        try {
            player.setSuspendDefaultMusicPlayback(false);
        } catch (RuntimeException ex) {
            logReleaseFailure(ex);
        }
        try {
            player.restartCurrentMusic();
        } catch (RuntimeException ex) {
            logReleaseFailure(ex);
        }
    }

    private static void logReleaseFailure(RuntimeException ex) {
        Global.getLogger(MenelausEndingMusic.class).warn(
                "Could not release the Labor V ending soundtrack", ex);
    }

    @Override public boolean isDone() { return done; }
    @Override public boolean runWhilePaused() { return true; }
}
