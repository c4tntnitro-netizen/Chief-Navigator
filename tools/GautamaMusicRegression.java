import chiefnavigator.hullmods.GautamaReincarnation;
import chiefnavigator.hullmods.ScyllaBossHullmod;
import chiefnavigator.quest.BudaiMusic;
import chiefnavigator.quest.FinalLaborMusic;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Drives Gautama's real hull callback, including an already-started defense. */
public final class GautamaMusicRegression {
    public static void main(String[] args) throws Exception {
        Global.setSector(null);
        Global.setSettings(WallResearchRegression.mock(SettingsAPI.class,
                (name, values) -> name.equals("getColor")
                        ? java.awt.Color.WHITE : null));
        checkAlreadyPresentDefense();
        checkConstructionAndPresenceShareOneStart();
        checkOnlyLiveGautamaStartsMusic();
        checkAuthoredLaborMusicRemainsProtected();
        checkCampaignHandoff();
        System.out.println("PASS: already-present Gautama defense music, shared "
                + "construction/presence latch, one delayed retry, identity/death "
                + "guards, Labor V protection, and campaign handoff.");
    }

    private static void checkAlreadyPresentDefense() throws Exception {
        Fixture fixture = new Fixture();
        LiveShip gautama = new LiveShip("chief_navigator_scylla");
        fixture.ships.add(gautama.ship);
        fixture.hullmod.advanceInCombat(gautama.ship, 0f);
        check(fixture.sound.cues.size() == 1
                        && fixture.sound.cues.get(0).equals(BudaiMusic.BATTLE_ID),
                "Joining a defense with Gautama already present must start Heart");
        check(fixture.sound.suspended == 1,
                "Gautama must take music ownership from normal battle music");

        fixture.hullmod.advanceInCombat(gautama.ship, 0.1f);
        check(fixture.sound.cues.size() == 1,
                "The next hull callback must not restart the initial request");
        makeRetryDue();
        fixture.hullmod.advanceInCombat(gautama.ship, 0.1f);
        check(fixture.sound.cues.size() == 2,
                "The live hull must deliver one delayed startup retry");
        for (int frame = 0; frame < 100; frame++) {
            fixture.hullmod.advanceInCombat(gautama.ship, 0.1f);
        }
        LiveShip reincarnated = new LiveShip(
                "chief_navigator_scylla_reincarnating");
        fixture.ships.add(reincarnated.ship);
        fixture.hullmod.advanceInCombat(reincarnated.ship, 0.1f);
        check(fixture.sound.cues.size() == 2,
                "Repeated hull updates and another Gautama cannot restart this battle's cue");

        Fixture nextBattle = new Fixture(fixture.sound, false);
        nextBattle.ships.add(gautama.ship);
        nextBattle.hullmod.advanceInCombat(gautama.ship, 0.1f);
        check(fixture.sound.cues.size() == 3
                        && fixture.sound.cues.get(2).equals(BudaiMusic.BATTLE_ID),
                "A new combat engine must start its own Heart cue");
        makeRetryDue();
        nextBattle.hullmod.advanceInCombat(gautama.ship, 0.1f);
        nextBattle.hullmod.advanceInCombat(gautama.ship, 0.1f);
        check(fixture.sound.cues.size() == 4,
                "The new battle must get its own single retry");
    }

    private static void checkConstructionAndPresenceShareOneStart()
            throws Exception {
        Fixture fixture = new Fixture();
        Method constructionCue = GautamaReincarnation.class.getDeclaredMethod(
                "maintainBattleMusic", CombatEngineAPI.class);
        constructionCue.setAccessible(true);
        constructionCue.invoke(null, fixture.engine);
        check(fixture.sound.cues.size() == 1,
                "Successful construction must start the same Heart cue");
        LiveShip gautama = new LiveShip(
                "chief_navigator_scylla_reincarnating");
        fixture.ships.add(gautama.ship);
        fixture.hullmod.advanceInCombat(gautama.ship, 0.1f);
        check(fixture.sound.cues.size() == 1,
                "A newly visible reconstructed hull must not reset its construction cue");
        makeRetryDue();
        fixture.hullmod.advanceInCombat(gautama.ship, 0.1f);
        constructionCue.invoke(null, fixture.engine);
        fixture.hullmod.advanceInCombat(gautama.ship, 0.1f);
        check(fixture.sound.cues.size() == 2,
                "Construction and presence callbacks must share one bounded retry");
    }

    private static void checkOnlyLiveGautamaStartsMusic() throws Exception {
        Fixture fixture = new Fixture();
        fixture.hullmod.advanceInCombat(null, 0.1f);
        fixture.hullmod.advanceInCombat(
                new LiveShip("fabricator_unit").ship, 0.1f);
        fixture.hullmod.advanceInCombat(
                new LiveShip("chief_navigator_ungaikyo").ship, 0.1f);
        ShipAPI noSpec = WallResearchRegression.mock(ShipAPI.class,
                (name, values) -> name.equals("isAlive") ? true : null);
        fixture.hullmod.advanceInCombat(noSpec, 0.1f);
        LiveShip dead = new LiveShip("chief_navigator_scylla");
        dead.alive = false;
        fixture.hullmod.advanceInCombat(dead.ship, 0.1f);
        LiveShip hulk = new LiveShip("chief_navigator_scylla_reincarnating");
        hulk.hulk = true;
        fixture.hullmod.advanceInCombat(hulk.ship, 0.1f);
        check(fixture.sound.cues.isEmpty(),
                "Missing, unrelated, dead, and hulk ships must not claim Gautama music");

        LiveShip live = new LiveShip("chief_navigator_scylla");
        fixture.hullmod.advanceInCombat(live.ship, 0.1f);
        check(fixture.sound.cues.size() == 1,
                "Rejected ships must not consume the real Gautama's startup latch");
        makeRetryDue();
        live.alive = false;
        fixture.hullmod.advanceInCombat(live.ship, 0.1f);
        check(fixture.sound.cues.size() == 1,
                "A dead Gautama must not dispatch the pending retry");
    }

    private static void checkAuthoredLaborMusicRemainsProtected() {
        Fixture finalLabor = new Fixture();
        GautamaReincarnation.enableFinalLaborMode(finalLabor.engine);
        finalLabor.hullmod.advanceInCombat(
                new LiveShip("chief_navigator_scylla_final").ship, 0.1f);
        check(finalLabor.sound.cues.isEmpty(),
                "Final-Labor mode must exclude ordinary Gautama music");

        Fixture gospel = new Fixture();
        FinalLaborMusic.playFinalFight();
        LiveShip gautama = new LiveShip("chief_navigator_scylla");
        for (int frame = 0; frame < 10; frame++) {
            gospel.hullmod.advanceInCombat(gautama.ship, 0.1f);
        }
        check(gospel.sound.cues.size() == 1
                        && gospel.sound.cues.get(0).equals(FinalLaborMusic.FINAL_FIGHT_ID),
                "An owned Gospel cue must not be overwritten by Gautama's Heart");
        FinalLaborMusic.releaseMissionMusic();
        gospel.hullmod.advanceInCombat(gautama.ship, 0.1f);
        check(gospel.sound.cues.size() == 2
                        && gospel.sound.cues.get(1).equals(BudaiMusic.BATTLE_ID),
                "Protected updates must not consume a later eligible startup latch");

        Fixture perimeter = new Fixture();
        FinalLaborMusic.playPerimeterFight();
        for (int frame = 0; frame < 10; frame++) {
            perimeter.hullmod.advanceInCombat(gautama.ship, 0.1f);
        }
        check(perimeter.sound.cues.size() == 1
                        && perimeter.sound.cues.get(0).equals(
                                FinalLaborMusic.PERIMETER_FIGHT_ID),
                "An owned Abyssal cue must not be overwritten by Gautama's Heart");
        FinalLaborMusic.releaseMissionMusic();
        perimeter.hullmod.advanceInCombat(gautama.ship, 0.1f);
        check(perimeter.sound.cues.size() == 2
                        && perimeter.sound.cues.get(1).equals(BudaiMusic.BATTLE_ID),
                "Abyssal protection must not consume a later eligible startup latch");
    }

    private static void checkCampaignHandoff() {
        Fixture fixture = new Fixture();
        fixture.hullmod.advanceInCombat(
                new LiveShip("chief_navigator_scylla").ship, 0.1f);
        BudaiMusic.releaseBattleMusic();
        BudaiMusic.releaseBattleMusic();
        check(fixture.sound.paused == 1 && fixture.sound.released == 1
                        && fixture.sound.restarted == 1,
                "Campaign return must release Gautama music exactly once");
    }

    private static void makeRetryDue() throws Exception {
        Field due = BudaiMusic.class.getDeclaredField("verificationAtMillis");
        due.setAccessible(true);
        due.setLong(null, 0L);
    }

    private static void check(boolean condition, String message) {
        WallResearchRegression.check(condition, message);
    }

    private static final class Fixture {
        final SoundCapture sound;
        final Map<String, Object> data = new HashMap<>();
        final List<ShipAPI> ships = new ArrayList<>();
        final ScyllaBossHullmod hullmod = new ScyllaBossHullmod();
        final CombatEngineAPI engine = WallResearchRegression.mock(
                CombatEngineAPI.class, (name, values) -> {
                    if (name.equals("getCustomData")) return data;
                    if (name.equals("getShips")) return ships;
                    return null;
                });

        Fixture() { this(new SoundCapture(), true); }

        Fixture(SoundCapture sound, boolean reset) {
            this.sound = sound;
            if (reset) {
                BudaiMusic.resetForGameLoad();
                FinalLaborMusic.resetForGameLoad();
            }
            Global.setSoundPlayer(sound.player);
            Global.setCombatEngine(engine);
        }
    }

    private static final class LiveShip {
        boolean alive = true;
        boolean hulk;
        final ShipAPI ship;

        LiveShip(String hullId) {
            ShipHullSpecAPI spec = WallResearchRegression.mock(
                    ShipHullSpecAPI.class, (name, values) ->
                            name.equals("getHullId") ? hullId : null);
            ship = WallResearchRegression.mock(ShipAPI.class, (name, values) -> {
                if (name.equals("getHullSpec")) return spec;
                if (name.equals("isAlive")) return alive;
                if (name.equals("isHulk")) return hulk;
                return null;
            });
        }
    }

    private static final class SoundCapture {
        final List<String> cues = new ArrayList<>();
        int suspended;
        int released;
        int paused;
        int restarted;
        final SoundPlayerAPI player = WallResearchRegression.mock(
                SoundPlayerAPI.class, (name, values) -> {
                    if (name.equals("playCustomMusic")) {
                        check(Boolean.TRUE.equals(values[3]),
                                "Gautama and protected combat cues must loop");
                        cues.add((String) values[2]);
                    }
                    if (name.equals("setSuspendDefaultMusicPlayback")) {
                        if (Boolean.TRUE.equals(values[0])) suspended++;
                        else released++;
                    }
                    if (name.equals("pauseCustomMusic")) paused++;
                    if (name.equals("restartCurrentMusic")) restarted++;
                    return null;
                });
    }
}
