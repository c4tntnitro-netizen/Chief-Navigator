package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.combat.ShipAPI;

/** Budai's Heart of Corruption opening and irreversible Savor transition. */
public final class BudaiMusic {
    public static final String BATTLE_ID = "chief_navigator_budai_battle";
    public static final String ENRAGED_ID = UngaikyoMusic.ENRAGED_ID;
    private static final long RETRY_DELAY_MILLIS = 2500L;
    private static String lastRequestedId;
    private static String verificationId;
    private static long verificationAtMillis;
    private static boolean enraged;

    private BudaiMusic() { }

    public static void playBattle() {
        resetForGameLoad();
        play(BATTLE_ID, true, "Budai battle start");
    }

    /** Starts the same cue when Gautama reconstructs outside Labor V. */
    public static void playGautamaReincarnation() {
        resetForGameLoad();
        play(BATTLE_ID, true, "Gautama reincarnation");
    }

    public static void maintainBattleMusic() {
        play(enraged ? ENRAGED_ID : BATTLE_ID, false, "battle keeper");
    }

    /** Only the actual Budai may trigger the half-hull transition. */
    public static void maintainBattleMusic(ShipAPI budai, boolean combatAdvancing) {
        if (combatAdvancing && budai != null && budai.isAlive() && !budai.isHulk()
                && budai.getHullSpec() != null
                && "chief_navigator_charybdis".equals(budai.getHullSpec().getHullId())
                && budai.getHullLevel() < 0.5f) {
            enraged = true;
        }
        maintainBattleMusic();
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
            Global.getLogger(BudaiMusic.class).error(
                    "Could not release Budai music ownership",
                    ex);
        } finally {
            resetForGameLoad();
        }
    }

    /** Clears process-static cue state when another campaign is loaded. */
    public static void resetForGameLoad() {
        lastRequestedId = null;
        verificationId = null;
        verificationAtMillis = 0L;
        enraged = false;
    }

    private static void play(String musicId, boolean forceRequest, String cue) {
        SoundPlayerAPI soundPlayer = Global.getSoundPlayer();
        if (soundPlayer == null) return;
        try {
            long now = System.currentTimeMillis();
            if (forceRequest || !musicId.equals(lastRequestedId)) {
                request(soundPlayer, musicId, cue);
                verificationId = musicId;
                verificationAtMillis = now + RETRY_DELAY_MILLIS;
                return;
            }
            if (!musicId.equals(verificationId)
                    || now < verificationAtMillis) return;

            // playCustomMusic does not reliably replace getCurrentMusicId();
            // retry exactly once after combat loading, never every 2.5s.
            verificationId = null;
            verificationAtMillis = 0L;
            request(soundPlayer, musicId, cue + " delayed startup retry");
        } catch (RuntimeException ex) {
            Global.getLogger(BudaiMusic.class).error(
                    "Could not start Budai music cue " + cue, ex);
        }
    }

    private static void request(
            SoundPlayerAPI soundPlayer,
            String musicId,
            String cue) {
        soundPlayer.setSuspendDefaultMusicPlayback(true);
        soundPlayer.playCustomMusic(1, 1, musicId, true);
        lastRequestedId = musicId;
        Global.getLogger(BudaiMusic.class).info(
                "Budai music requested (" + musicId + "): " + cue);
    }
}
