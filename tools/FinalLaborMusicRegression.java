import chiefnavigator.quest.FinalLaborMusic;
import chiefnavigator.quest.BudaiMusic;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/** Headless check for transition-safe Labor V music retries. */
public final class FinalLaborMusicRegression {
    public static void main(String[] args) throws Exception {
        int[] requests = {0};
        int[] suspended = {0};
        int[] released = {0};
        int[] paused = {0};
        int[] restarted = {0};
        List<String> cues = new ArrayList<>();
        SoundPlayerAPI player = WallResearchRegression.mock(
                SoundPlayerAPI.class, (name, callArgs) -> {
                    if (name.equals("playCustomMusic")) {
                        requests[0]++;
                        cues.add((String) callArgs[2]);
                        WallResearchRegression.check(Boolean.TRUE.equals(callArgs[3]),
                                "Every combat cue must loop");
                    }
                    if (name.equals("pauseCustomMusic")) paused[0]++;
                    if (name.equals("restartCurrentMusic")) restarted[0]++;
                    if (name.equals("setSuspendDefaultMusicPlayback")) {
                        if (Boolean.TRUE.equals(callArgs[0])) suspended[0]++;
                        else released[0]++;
                    }
                    return null;
                });
        Global.setSoundPlayer(player);
        Global.setSector(null);
        GameState[] state = {GameState.COMBAT};
        Global.setSettings(WallResearchRegression.mock(SettingsAPI.class,
                (name, callArgs) -> name.equals("getCurrentState") ? state[0] : null));
        FinalLaborMusic.resetForGameLoad();
        WallResearchRegression.check(!FinalLaborMusic.isGospelPlaying(),
                "A fresh campaign must not inherit stale music ownership");

        WallResearchRegression.check(FinalLaborMusic.PERIMETER_FIGHT_ID.equals(
                        "chief_navigator_final_labor_perimeter"),
                "Perimeter combat must use its dedicated Abyssal Rhapsody cue");
        FinalLaborMusic.playPerimeterFight();
        WallResearchRegression.check(requests[0] == 1 && suspended[0] == 1
                        && cues.get(0).equals(FinalLaborMusic.PERIMETER_FIGHT_ID)
                        && FinalLaborMusic.isGospelPlaying(),
                "The three Strikes must loop Abyssal and protect it from Gautama");
        makeVerificationDue();
        for (int frame = 0; frame < 100; frame++) {
            FinalLaborMusic.maintainMissionMusic();
        }
        WallResearchRegression.check(requests[0] == 2
                        && cues.get(1).equals(FinalLaborMusic.PERIMETER_FIGHT_ID),
                "The perimeter keeper must retry Abyssal once, not revert to Gospel");
        FinalLaborMusic.releaseMissionMusic();
        FinalLaborMusic.releaseMissionMusic();
        WallResearchRegression.check(paused[0] == 1 && released[0] == 1
                        && restarted[0] == 1 && !FinalLaborMusic.isGospelPlaying(),
                "Perimeter combat must release custom music exactly once");

        requests[0] = 0;
        released[0] = 0;
        restarted[0] = 0;
        cues.clear();
        FinalLaborMusic.playPerimeterFight();
        state[0] = GameState.CAMPAIGN;
        FinalLaborMusic.maintainMissionMusic();
        FinalLaborMusic.maintainMissionMusic();
        WallResearchRegression.check(requests[0] == 1 && released[0] == 1
                        && restarted[0] == 1,
                "Campaign return must hand Abyssal back to native location music once");
        FinalLaborMusic.releaseMissionMusic();
        state[0] = GameState.COMBAT;

        requests[0] = 0;
        suspended[0] = 0;
        released[0] = 0;
        paused[0] = 0;
        restarted[0] = 0;
        cues.clear();
        FinalLaborMusic.playFinalFight();
        WallResearchRegression.check(requests[0] == 1,
                "Final cue must replace the instrumental");
        WallResearchRegression.check(cues.get(0).equals(FinalLaborMusic.FINAL_FIGHT_ID),
                "The center breach must retain vocal Gospel, not Abyssal");
        WallResearchRegression.check(suspended[0] == 1,
                "Final cue must suspend vanilla music playback");

        WallResearchRegression.check(FinalLaborMusic.isGospelPlaying(),
                "Vocal Gospel cue must be recognized as protected");
        makeVerificationDue();
        FinalLaborMusic.maintainFinalFightMusic();
        WallResearchRegression.check(requests[0] == 2,
                "Final cue must receive one delayed verification request");
        FinalLaborMusic.maintainFinalFightMusic();
        WallResearchRegression.check(requests[0] == 2,
                "A verified live final cue must not restart every frame");

        FinalLaborMusic.releaseMissionMusic();
        WallResearchRegression.check(released[0] == 1,
                "Labor V completion must return vanilla music ownership");

        requests[0] = 0;
        suspended[0] = 0;
        released[0] = 0;
        paused[0] = 0;
        restarted[0] = 0;
        cues.clear();
        BudaiMusic.resetForGameLoad();
        BudaiMusic.playBattle();
        WallResearchRegression.check(requests[0] == 1
                        && suspended[0] == 1,
                "Budai must take music ownership at battle start");
        makeBudaiVerificationDue();
        BudaiMusic.maintainBattleMusic();
        BudaiMusic.maintainBattleMusic();
        WallResearchRegression.check(requests[0] == 2,
                "Budai must retry once without restarting in a loop");

        float[] hull = {0.6f};
        boolean[] alive = {true};
        boolean[] hulk = {false};
        String[] hullId = {"shrouded_maw"};
        ShipHullSpecAPI spec = WallResearchRegression.mock(ShipHullSpecAPI.class,
                (name, values) -> name.equals("getHullId") ? hullId[0] : null);
        ShipAPI budai = WallResearchRegression.mock(ShipAPI.class, (name, values) -> {
            if (name.equals("getHullSpec")) return spec;
            if (name.equals("getHullLevel")) return hull[0];
            if (name.equals("isAlive")) return alive[0];
            if (name.equals("isHulk")) return hulk[0];
            return null;
        });
        hull[0] = 0.1f;
        BudaiMusic.maintainBattleMusic(budai, true);
        WallResearchRegression.check(requests[0] == 2,
                "An ordinary damaged Maw must not trigger Savor");
        hullId[0] = "chief_navigator_charybdis";
        hull[0] = 0.6f;
        BudaiMusic.maintainBattleMusic(budai, true);
        hull[0] = 0.5f;
        BudaiMusic.maintainBattleMusic(budai, true);
        WallResearchRegression.check(requests[0] == 2
                        && cues.get(1).equals(BudaiMusic.BATTLE_ID),
                "Exactly 50% hull must retain Heart even while combat advances");
        hull[0] = Math.nextDown(0.5f);
        BudaiMusic.maintainBattleMusic(budai, false);
        alive[0] = false;
        BudaiMusic.maintainBattleMusic(budai, true);
        alive[0] = true;
        hulk[0] = true;
        BudaiMusic.maintainBattleMusic(budai, true);
        WallResearchRegression.check(requests[0] == 2,
                "Paused combat, dead Budai, and a hulk must not trigger Savor below half");
        hulk[0] = false;
        BudaiMusic.maintainBattleMusic(budai, true);
        WallResearchRegression.check(requests[0] == 3
                        && cues.get(2).equals(BudaiMusic.ENRAGED_ID),
                "The real advancing Budai must switch at the first float below 50% hull");
        makeBudaiVerificationDue();
        BudaiMusic.maintainBattleMusic(budai, true);
        hull[0] = 0.9f;
        BudaiMusic.maintainBattleMusic(budai, true);
        alive[0] = false;
        BudaiMusic.maintainBattleMusic(budai, true);
        BudaiMusic.maintainBattleMusic(null, true);
        WallResearchRegression.check(requests[0] == 4
                        && cues.get(3).equals(BudaiMusic.ENRAGED_ID),
                "Savor retries once and stays latched through healing, death, and removal");
        BudaiMusic.releaseBattleMusic();
        BudaiMusic.releaseBattleMusic();
        WallResearchRegression.check(paused[0] == 1
                        && released[0] == 1
                        && restarted[0] == 1,
                "Budai combat must hand music back to the campaign");

        BudaiMusic.playBattle();
        WallResearchRegression.check(cues.get(4).equals(BudaiMusic.BATTLE_ID),
                "A new battle must start with Heart again");
        alive[0] = true;
        hull[0] = 0.5f;
        BudaiMusic.maintainBattleMusic(budai, true);
        WallResearchRegression.check(requests[0] == 5,
                "A fresh battle must not switch at exactly half hull");
        hull[0] = Math.nextDown(0.5f);
        BudaiMusic.maintainBattleMusic(budai, true);
        BudaiMusic.playGautamaReincarnation();
        BudaiMusic.maintainBattleMusic();
        WallResearchRegression.check(requests[0] == 7
                        && cues.get(6).equals(BudaiMusic.BATTLE_ID),
                "Gautama must not inherit Budai's Savor phase");
        BudaiMusic.releaseBattleMusic();

        System.out.println("PASS: Labor V Abyssal perimeter/vocal center cues and retries; "
                + "Budai's strictly-below-half-hull Savor transition, "
                + "one-shot cue retries, battle reset, Gautama isolation, and campaign handoff.");
    }

    private static void makeVerificationDue() throws Exception {
        Field due = FinalLaborMusic.class.getDeclaredField(
                "verificationAtMillis");
        due.setAccessible(true);
        due.setLong(null, 0L);
    }

    private static void makeBudaiVerificationDue() throws Exception {
        Field due = BudaiMusic.class.getDeclaredField(
                "verificationAtMillis");
        due.setAccessible(true);
        due.setLong(null, 0L);
    }
}
