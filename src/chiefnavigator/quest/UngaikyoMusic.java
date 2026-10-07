package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.combat.ShipAPI;

/** Owns Ungaikyo's Lord of Ashes opening and irreversible half-hull transition. */
public final class UngaikyoMusic {
    public static final String OPENING_ID = "chief_navigator_ungaikyo_opening";
    public static final String ENRAGED_ID = "chief_navigator_ungaikyo_enraged";
    private static final long RETRY_DELAY_MILLIS = 2500L;
    private static String lastRequestedId;
    private static long verificationAtMillis;
    private static boolean verificationPending;
    private static boolean enraged;

    private UngaikyoMusic() { }

    public static void playBattle() {
        resetForGameLoad();
        play(OPENING_ID);
    }

    /** The encounter controller supplies only the real Ungaikyo Fabricator. */
    public static void maintainBattleMusic(
            ShipAPI ungaikyo, boolean combatAdvancing) {
        if (combatAdvancing && ungaikyo != null && ungaikyo.isAlive()
                && !ungaikyo.isHulk()
                && ungaikyo.getHullLevel() < 0.5f) {
            enraged = true;
        }
        play(enraged ? ENRAGED_ID : OPENING_ID);
    }

    /** Returns music ownership on the first campaign frame after combat. */
    public static void releaseBattleMusic() {
        if (lastRequestedId == null) return;
        SoundPlayerAPI soundPlayer = Global.getSoundPlayer();
        try {
            if (soundPlayer != null) {
                soundPlayer.pauseCustomMusic();
                soundPlayer.setSuspendDefaultMusicPlayback(false);
                soundPlayer.restartCurrentMusic();
            }
        } catch (RuntimeException ex) {
            Global.getLogger(UngaikyoMusic.class).error(
                    "Could not release Ungaikyo music ownership", ex);
        } finally {
            resetForGameLoad();
        }
    }

    public static void resetForGameLoad() {
        lastRequestedId = null;
        verificationAtMillis = 0L;
        verificationPending = false;
        enraged = false;
    }

    private static void play(String musicId) {
        SoundPlayerAPI soundPlayer = Global.getSoundPlayer();
        if (soundPlayer == null) return;
        try {
            long now = System.currentTimeMillis();
            if (!musicId.equals(lastRequestedId)) {
                request(soundPlayer, musicId);
                verificationAtMillis = now + RETRY_DELAY_MILLIS;
                verificationPending = true;
            } else if (verificationPending && now >= verificationAtMillis) {
                // Combat loading can discard an early custom-stream request.
                // Retry once per cue; the reported current id is unreliable.
                verificationPending = false;
                request(soundPlayer, musicId);
            }
        } catch (RuntimeException ex) {
            Global.getLogger(UngaikyoMusic.class).error(
                    "Could not start Ungaikyo music cue " + musicId, ex);
        }
    }

    private static void request(SoundPlayerAPI soundPlayer, String musicId) {
        soundPlayer.setSuspendDefaultMusicPlayback(true);
        soundPlayer.playCustomMusic(1, 1, musicId, true);
        lastRequestedId = musicId;
        Global.getLogger(UngaikyoMusic.class).info(
                "Ungaikyo music cue requested: " + musicId);
    }
}
