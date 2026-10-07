package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;

/** Owns Labor V's campaign, perimeter-combat, and center-breach cues. */
public final class FinalLaborMusic {
    public static final String MISSION_START_ID =
            "chief_navigator_final_labor_mission";
    public static final String PERIMETER_FIGHT_ID =
            "chief_navigator_final_labor_perimeter";
    public static final String FINAL_FIGHT_ID =
            "chief_navigator_final_labor_fight";
    private static final long RETRY_DELAY_MILLIS = 2500L;
    private static String lastRequestedId;
    private static String verificationId;
    private static long verificationAtMillis;
    private static boolean ownsMusic;
    private static boolean combatOverrideActive;

    private FinalLaborMusic() { }

    /** Starts the looping instrumental between Labor V battles. */
    public static void playMissionStart() {
        ensureMissionLocationMusic();
        SoundPlayerAPI soundPlayer = Global.getSoundPlayer();
        if (soundPlayer != null) {
            soundPlayer.setSuspendDefaultMusicPlayback(false);
            soundPlayer.restartCurrentMusic();
        }
        ownsMusic = true;
        lastRequestedId = MISSION_START_ID;
        verificationId = null;
        verificationAtMillis = 0L;
        combatOverrideActive = false;
    }

    /** Starts Abyssal Rhapsody for Scylla, Siren, and Cyclops combat. */
    public static void playPerimeterFight() {
        play(PERIMETER_FIGHT_ID, true, "perimeter fight", true);
    }

    /** Replaces battle music with the vocal version for the center breach. */
    public static void playFinalFight() {
        play(FINAL_FIGHT_ID, true, "center fight", true);
    }

    /** Repairs a campaign-transition request that the audio thread discarded. */
    public static void maintainMissionMusic() {
        ensureMissionLocationMusic();
        if (Global.getCurrentState() == GameState.CAMPAIGN) {
            if (combatOverrideActive) {
                SoundPlayerAPI soundPlayer = Global.getSoundPlayer();
                if (soundPlayer != null) {
                    soundPlayer.setSuspendDefaultMusicPlayback(false);
                    soundPlayer.restartCurrentMusic();
                }
                combatOverrideActive = false;
            }
            ownsMusic = true;
            lastRequestedId = MISSION_START_ID;
            verificationId = null;
            verificationAtMillis = 0L;
            return;
        }
        play(PERIMETER_FIGHT_ID, true, "perimeter keeper", false);
    }

    /** Repairs an early combat-loading request without restarting a live track. */
    public static void maintainFinalFightMusic() {
        play(FINAL_FIGHT_ID, true, "center-fight keeper", false);
    }

    /** Returns campaign music ownership when Labor V is no longer active. */
    public static void releaseMissionMusic() {
        SoundPlayerAPI soundPlayer = Global.getSoundPlayer();
        clearMissionLocationMusic();
        if (soundPlayer == null || !ownsMusic) return;
        try {
            if (combatOverrideActive) soundPlayer.pauseCustomMusic();
            soundPlayer.setSuspendDefaultMusicPlayback(false);
            soundPlayer.restartCurrentMusic();
        } catch (RuntimeException ex) {
            Global.getLogger(FinalLaborMusic.class).error(
                    "Could not release Labor V music ownership", ex);
        } finally {
            ownsMusic = false;
            lastRequestedId = null;
            verificationId = null;
            verificationAtMillis = 0L;
            combatOverrideActive = false;
        }
    }

    /** Historical name: protects every authored Labor V cue, including Abyssal. */
    public static boolean isGospelPlaying() {
        return ownsMusic && (MISSION_START_ID.equals(lastRequestedId)
                || PERIMETER_FIGHT_ID.equals(lastRequestedId)
                || FINAL_FIGHT_ID.equals(lastRequestedId));
    }

    /** Clears process-static cue state when another campaign is loaded. */
    public static void resetForGameLoad() {
        ownsMusic = false;
        combatOverrideActive = false;
        lastRequestedId = null;
        verificationId = null;
        verificationAtMillis = 0L;
    }

    private static void play(
            String musicId, boolean loop, String cue, boolean forceRequest) {
        SoundPlayerAPI soundPlayer = Global.getSoundPlayer();
        if (soundPlayer == null) return;
        try {
            long now = System.currentTimeMillis();
            if (forceRequest || !musicId.equals(lastRequestedId)) {
                request(soundPlayer, musicId, loop, cue);
                verificationId = musicId;
                verificationAtMillis = now + RETRY_DELAY_MILLIS;
                return;
            }
            if (!musicId.equals(verificationId)
                    || now < verificationAtMillis) return;

            // getCurrentMusicId() reports the underlying vanilla state for
            // custom streams. Retry once after combat loading, not forever.
            verificationId = null;
            verificationAtMillis = 0L;
            request(soundPlayer, musicId, loop,
                    cue + " delayed startup retry");
        } catch (RuntimeException ex) {
            Global.getLogger(FinalLaborMusic.class).error(
                    "Could not start Labor V music cue " + cue
                            + " (" + musicId + ")", ex);
        }
    }

    private static void request(
            SoundPlayerAPI soundPlayer,
            String musicId,
            boolean loop,
            String cue) {
        // Vanilla campaign music otherwise remains eligible to immediately
        // replace a custom stream, especially during dialog transitions.
        soundPlayer.setSuspendDefaultMusicPlayback(true);
        soundPlayer.playCustomMusic(1, 1, musicId, loop);
        ownsMusic = true;
        combatOverrideActive = true;
        lastRequestedId = musicId;
        Global.getLogger(FinalLaborMusic.class).info(
                "Labor V music cue requested: " + cue
                        + ", id=" + musicId + ", loop=" + loop);
    }

    private static void ensureMissionLocationMusic() {
        if (Global.getSector() == null) return;
        StarSystemAPI ashen = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.ASHEN_VERGE_ID);
        if (ashen != null) {
            ashen.getMemoryWithoutUpdate().set(
                    MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY,
                    MISSION_START_ID);
        }
    }

    private static void clearMissionLocationMusic() {
        if (Global.getSector() == null) return;
        StarSystemAPI ashen = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.ASHEN_VERGE_ID);
        if (ashen == null) return;
        String current = ashen.getMemoryWithoutUpdate().getString(
                MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY);
        if (MISSION_START_ID.equals(current)
                || FINAL_FIGHT_ID.equals(current)) {
            ashen.getMemoryWithoutUpdate().unset(
                    MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY);
        }
    }
}
