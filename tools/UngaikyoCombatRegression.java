import chiefnavigator.quest.UngaikyoMusic;
import chiefnavigator.systems.UngaikyoConstructionSwarmAI;
import chiefnavigator.systems.UngaikyoConstructionSwarmSystem;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.State;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;

/** Drives the actual encounter controller, music handoff, and extraction script. */
public final class UngaikyoCombatRegression {
    public static void main(String[] args) throws Exception {
        List<String> cues = new ArrayList<>();
        int[] released = {0};
        SoundPlayerAPI sound = WallResearchRegression.mock(
                SoundPlayerAPI.class, (name, a) -> {
                    if (name.equals("playCustomMusic")) {
                        check(Boolean.TRUE.equals(a[3]), "Both cues must loop");
                        cues.add((String) a[2]);
                    }
                    if (name.equals("restartCurrentMusic")) released[0]++;
                    return null;
                });
        Global.setSoundPlayer(sound);
        Global.setSettings(WallResearchRegression.mock(SettingsAPI.class,
                (name, a) -> name.equals("getColor") ? java.awt.Color.WHITE : null));
        Global.setSector(null);
        boolean[] paused = {false};
        List<ShipAPI> ships = new ArrayList<>();
        Map<String, Object> combatData = new HashMap<>();
        CombatEngineAPI engine = WallResearchRegression.mock(
                CombatEngineAPI.class, (name, a) -> {
                    if (name.equals("isPaused")) return paused[0];
                    if (name.equals("getShips")) return ships;
                    if (name.equals("getCustomData")) return combatData;
                    return null;
                });
        Global.setCombatEngine(engine);
        float[] hull = {1f};
        ShipHullSpecAPI bossHull = WallResearchRegression.mock(
                ShipHullSpecAPI.class, (name, a) ->
                        name.equals("getHullId") ? "chief_navigator_ungaikyo" : null);
        ShipAPI boss = WallResearchRegression.mock(ShipAPI.class, (name, a) -> {
            if (name.equals("getHullSpec")) return bossHull;
            if (name.equals("isAlive")) return hull[0] > 0f;
            if (name.equals("getHullLevel")) return hull[0];
            return null;
        });
        ShipHullSpecAPI ordinaryHull = WallResearchRegression.mock(
                ShipHullSpecAPI.class, (name, a) ->
                        name.equals("getHullId") ? "fabricator_unit" : null);
        ships.add(WallResearchRegression.mock(ShipAPI.class, (name, a) -> {
            if (name.equals("getHullSpec")) return ordinaryHull;
            if (name.equals("isAlive")) return true;
            if (name.equals("getHullLevel")) return 0.1f;
            return null;
        }));
        Class<?> type = Class.forName("chiefnavigator.quest."
                + "GautamaFinalRematchBattleCreationPlugin$MirrorFleetController");
        Constructor<?> ctor = type.getDeclaredConstructor(CombatEngineAPI.class);
        ctor.setAccessible(true);
        BaseEveryFrameCombatPlugin controller =
                (BaseEveryFrameCombatPlugin) ctor.newInstance(engine);

        UngaikyoMusic.playBattle();
        check(UngaikyoMusic.OPENING_ID.equals("chief_navigator_ungaikyo_opening"),
                "Ungaikyo must open with its dedicated Lord of Ashes cue");
        controller.advance(0.1f, Collections.emptyList());
        check(cues.size() == 1 && cues.get(0).equals(UngaikyoMusic.OPENING_ID),
                "An ordinary damaged Fabricator must not switch the opening cue");
        makeRetryDue();
        controller.advance(0.1f, Collections.emptyList());
        controller.advance(0.1f, Collections.emptyList());
        check(cues.size() == 2, "The opening cue must retry exactly once");
        ships.add(boss);
        hull[0] = 0.5f;
        controller.advance(0.1f, Collections.emptyList());
        check(cues.size() == 2,
                "Exactly 50% hull and an empty mirror pool must retain Lord of Ashes");
        hull[0] = 0.49f;
        paused[0] = true;
        controller.advance(0.1f, Collections.emptyList());
        paused[0] = false;
        controller.advance(0f, Collections.emptyList());
        check(cues.size() == 2, "Paused deployment must not trigger the transition");
        controller.advance(0.1f, Collections.emptyList());
        check(cues.size() == 3 && cues.get(2).equals(UngaikyoMusic.ENRAGED_ID),
                "The real Fabricator below 50% must switch to Savor");
        makeRetryDue();
        controller.advance(0.1f, Collections.emptyList());
        hull[0] = 0.9f;
        controller.advance(0.1f, Collections.emptyList());
        hull[0] = 0f;
        controller.advance(0.1f, Collections.emptyList());
        check(cues.size() == 4 && cues.get(3).equals(UngaikyoMusic.ENRAGED_ID),
                "The second cue retries once and stays latched after healing or death");
        UngaikyoMusic.releaseBattleMusic();
        UngaikyoMusic.releaseBattleMusic();
        check(released[0] == 1, "Campaign music must be restored exactly once");
        UngaikyoMusic.playBattle();
        check(cues.get(4).equals(UngaikyoMusic.OPENING_ID),
                "A new battle must reset the half-hull transition");
        UngaikyoMusic.releaseBattleMusic();

        verifyExtraction(engine, paused);
        System.out.println("PASS: Ungaikyo's strict half-hull music transition, "
                + "one-shot retries, campaign handoff, autonomous extraction, "
                + "backward thrust, vent/reload, and stat cleanup.");
    }

    private static void verifyExtraction(
            CombatEngineAPI engine, boolean[] paused) {
        List<ShipCommand> commands = new ArrayList<>();
        List<ShipCommand> blocked = new ArrayList<>();
        float[] flux = {0.1f};
        boolean[] ready = {true};
        int[] chargeChanges = {0};
        int[] ammo = {2};
        int[] reloads = {0};
        ShipSystemAPI system = WallResearchRegression.mock(
                ShipSystemAPI.class, (name, a) -> {
                    if (name.equals("canBeActivated")) return ready[0];
                    if (name.equals("setAmmo")) chargeChanges[0]++;
                    return null;
                });
        WeaponSlotAPI slot = WallResearchRegression.mock(WeaponSlotAPI.class,
                (name, a) -> null);
        WeaponAPI weapon = WallResearchRegression.mock(WeaponAPI.class, (name, a) -> {
            if (name.equals("usesAmmo")) return true;
            if (name.equals("getAmmo")) return ammo[0];
            if (name.equals("getMaxAmmo")) return 10;
            if (name.equals("getSlot")) return slot;
            if (name.equals("setAmmo")) { ammo[0] = (Integer) a[0]; reloads[0]++; }
            return null;
        });
        ShipEngineControllerAPI engines = WallResearchRegression.mock(
                ShipEngineControllerAPI.class, (name, a) -> null);
        ShipwideAIFlags flags = new ShipwideAIFlags();
        ShipAPI ship = WallResearchRegression.mock(ShipAPI.class, (name, a) -> {
            if (name.equals("isAlive")) return true;
            if (name.equals("getSystem")) return system;
            if (name.equals("getFluxLevel")) return flux[0];
            if (name.equals("getAIFlags")) return flags;
            if (name.equals("getEngineController")) return engines;
            if (name.equals("getAllWeapons")) return Collections.singletonList(weapon);
            if (name.equals("getWeaponGroupsCopy")) return Collections.emptyList();
            if (name.equals("getLocation")) return new Vector2f();
            if (name.equals("giveCommand")) commands.add((ShipCommand) a[0]);
            if (name.equals("blockCommandForOneFrame")) blocked.add((ShipCommand) a[0]);
            return null;
        });
        UngaikyoConstructionSwarmAI ai = new UngaikyoConstructionSwarmAI();
        ai.init(ship, system, flags, engine);
        ai.advance(0.1f, null, null, null);
        check(commands.isEmpty(), "An unpressured Fabricator must not waste extraction");
        flux[0] = 0.7f;
        ai.advance(0.1f, null, null, null);
        check(commands.size() == 1 && commands.get(0) == ShipCommand.USE_SYSTEM,
                "High flux must activate extraction without an Overseer");
        ready[0] = false;
        ai.advance(0.1f, new Vector2f(1, 0), null, null);
        check(commands.size() == 1, "Cooldown must prevent activation");
        ready[0] = true;
        paused[0] = true;
        ai.advance(0.1f, new Vector2f(1, 0), null, null);
        check(commands.size() == 1, "Paused combat must prevent activation");
        paused[0] = false;
        flux[0] = 0.1f;
        ai.advance(0.1f, new Vector2f(1, 0), null, null);
        check(commands.size() == 2, "Missile danger must trigger the backward dodge");

        MutableStat speed = new MutableStat(50f);
        MutableStat acceleration = new MutableStat(20f);
        MutableStat deceleration = new MutableStat(20f);
        MutableShipStatsAPI stats = WallResearchRegression.mock(
                MutableShipStatsAPI.class, (name, a) -> {
                    if (name.equals("getEntity")) return ship;
                    if (name.equals("getMaxSpeed")) return speed;
                    if (name.equals("getAcceleration")) return acceleration;
                    if (name.equals("getDeceleration")) return deceleration;
                    return null;
                });
        UngaikyoConstructionSwarmSystem script = new UngaikyoConstructionSwarmSystem();
        script.apply(stats, "extraction", State.ACTIVE, 1f);
        check(chargeChanges[0] == 0, "Extraction must not zero an Overseer-only charge");
        check(speed.getModifiedValue() == 150f
                        && acceleration.getModifiedValue() == 220f,
                "Native Extraction Protocol maneuver bonuses must remain intact");
        check(commands.contains(ShipCommand.ACCELERATE_BACKWARDS)
                        && blocked.contains(ShipCommand.ACCELERATE),
                "Active extraction must force backward thrust and block forward thrust");
        script.apply(stats, "extraction", State.OUT, 0.5f);
        script.apply(stats, "extraction", State.OUT, 0.4f);
        check(Collections.frequency(commands, ShipCommand.VENT_FLUX) == 1
                        && ammo[0] == 10 && reloads[0] == 1,
                "Extraction must vent and replenish ammo once on chargedown");
        script.unapply(stats, "extraction");
        check(speed.getModifiedValue() == 50f
                        && acceleration.getModifiedValue() == 20f
                        && deceleration.getModifiedValue() == 20f,
                "Extraction must leave no speed/acceleration bonuses afterward");
    }

    private static void makeRetryDue() throws Exception {
        Field due = UngaikyoMusic.class.getDeclaredField("verificationAtMillis");
        due.setAccessible(true);
        due.setLong(null, 0L);
    }

    private static void check(boolean condition, String message) {
        WallResearchRegression.check(condition, message);
    }
}
