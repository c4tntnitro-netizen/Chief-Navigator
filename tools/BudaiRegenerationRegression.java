import chiefnavigator.hullmods.CharybdisDistortion;
import chiefnavigator.hullmods.BudaiRegeneration;
import chiefnavigator.quest.BudaiMusic;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import java.awt.Color;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/** Actual finite-pool helper and music latch against mutable headless ship proxies; no GL. */
public final class BudaiRegenerationRegression {

    private static final class Fixture {
        final Map<String, Object> data = new HashMap<>();
        float max = 10000f;
        float hp = 100f;
        boolean alive = true;
        boolean hulk;
        boolean inPlay = true;
        boolean paused;
        String hullId = "chief_navigator_charybdis";
        int hullWrites;
        final ShipAPI ship;
        final CombatEngineAPI engine;

        Fixture() {
            ShipHullSpecAPI hull = mock(ShipHullSpecAPI.class,
                    (name, args) -> name.equals("getHullId") ? hullId : null);
            ship = mock(ShipAPI.class, (name, args) -> {
                switch (name) {
                    case "getHullSpec": return hull;
                    case "getCustomData": return data;
                    case "setCustomData": data.put((String) args[0], args[1]); return null;
                    case "removeCustomData": data.remove(args[0]); return null;
                    case "getMaxHitpoints": return max;
                    case "getHitpoints": return hp;
                    case "getHullLevel": return hp / max;
                    case "isAlive": return alive;
                    case "isHulk": return hulk;
                    case "setHitpoints":
                        float replacement = (Float) args[0];
                        check(replacement >= hp && replacement <= max,
                                "Regeneration may only heal existing hull within its maximum");
                        hp = replacement;
                        hullWrites++;
                        return null;
                    case "getArmorGrid": case "getFluxTracker": case "getMutableStats":
                    case "getSystem":
                        throw new AssertionError("Hull-only regeneration must not access " + name);
                    default:
                        if (name.startsWith("set") || name.startsWith("reset")
                                || name.startsWith("repair")) {
                            throw new AssertionError("Unexpected non-hull mutation: " + name);
                        }
                        return null;
                }
            });
            engine = mock(CombatEngineAPI.class, (name, args) -> {
                if (name.equals("isPaused")) return paused;
                if (name.equals("isEntityInPlay")) return inPlay && args[0] == ship;
                return null;
            });
        }

        void step(float amount) throws Exception {
            invoke(engine, ship, amount);
        }
    }

    public static void main(String[] args) throws Exception {
        SettingsAPI savedSettings = Global.getSettings();
        SoundPlayerAPI savedSound = Global.getSoundPlayer();
        Global.setSettings(mock(SettingsAPI.class,
                (name, values) -> name.equals("getColor") ? Color.WHITE : null));
        try {
            taperAndFinitePool();
            frameIndependentIntegration();
            fullHullAndOverhealPreserveReserve();
            skippedFramesAndNoResurrection();
            snapshotAndIndependentShips();
            musicAndEnragePrecedeHealing();
            System.out.println("PASS: Budai hull-only finite 50% regeneration pool, linear taper, "
                    + "frame-size independence, paused/full-hull reserve, death/unrelated guards, "
                    + "snapshot persistence and pre-heal music/enrage ordering (mocked lifecycle; no GL).");
        } finally {
            BudaiMusic.resetForGameLoad();
            Global.setSettings(savedSettings);
            Global.setSoundPlayer(savedSound);
        }
    }

    private static void taperAndFinitePool() throws Exception {
        Fixture f = new Fixture();
        f.step(10f);
        near(f.hp, 1050f, "First ten seconds integrate the initial 1%-per-second declining rate");
        f.step(10f);
        near(f.hp, 1900f, "The next ten seconds heal less as the rate tapers linearly");
        new CharybdisDistortion(); // Re-instantiating the hullmod must not replace ship-local state.
        f.step(30f);
        near(f.hp, 3850f, "At fifty actual healing seconds, the consumed integral is 37.5% max hull");
        f.step(50f);
        near(f.hp, 5100f, "The complete taper heals exactly 50% of the snapshotted maximum");
        f.hp -= 1000f;
        int writes = f.hullWrites;
        f.step(10000f);
        near(f.hp, 4100f, "Damage after exhaustion cannot replenish the finite reserve");
        check(f.hullWrites == writes, "An exhausted pool performs no further hull writes");
    }

    private static void frameIndependentIntegration() throws Exception {
        Fixture whole = new Fixture();
        Fixture coarse = new Fixture();
        Fixture fine = new Fixture();
        whole.step(100f);
        for (int i = 0; i < 10; i++) coarse.step(10f);
        for (int i = 0; i < 1000; i++) fine.step(0.1f);
        near(whole.hp, 5100f, "One large frame consumes only the authored half-hull reserve");
        near(coarse.hp, whole.hp, "Coarse chunks integrate the same taper");
        near(fine.hp, whole.hp, "Fine chunks integrate the same taper without frame-size drift");
        Fixture oversized = new Fixture();
        oversized.step(10000f);
        near(oversized.hp, whole.hp, "A frame beyond the taper duration cannot over-heal its pool");
        Fixture tiny = new Fixture();
        tiny.hp = 5000f;
        for (int i = 0; i < 10000; i++) tiny.step(0.000001f);
        near(tiny.hp, 5001f, "Sub-float repair increments accumulate instead of disappearing per frame");
    }

    private static void fullHullAndOverhealPreserveReserve() throws Exception {
        Fixture idle = new Fixture();
        idle.hp = idle.max;
        for (int i = 0; i < 10; i++) idle.step(100f);
        check(idle.hullWrites == 0, "Full-hull time performs no healing");
        idle.hp = 100f;
        idle.step(10f);
        near(idle.hp, 1050f, "Full-hull time cannot advance the healing-only clock");

        Fixture clamped = new Fixture();
        clamped.hp = 9999f;
        clamped.step(100f);
        near(clamped.hp, 10000f, "Heal clamps to the missing hull inside a large frame");
        clamped.step(1000f);
        clamped.hp = 100f;
        clamped.step(10000f);
        near(clamped.hp, 5099f, "Overheal and subsequent full-hull idle time do not waste reserve");
        clamped.hp = 100f;
        clamped.step(1000f);
        near(clamped.hp, 100f, "Only actual applied hull repair consumes the finite 50% reserve");

        Fixture chunks = new Fixture();
        chunks.hp = 9999f;
        for (int i = 0; i < 1000; i++) chunks.step(0.1f);
        chunks.hp = 100f;
        chunks.step(10000f);
        near(chunks.hp, 5099f, "Reaching full hull mid-step remains chunk-independent");
    }

    private static void skippedFramesAndNoResurrection() throws Exception {
        for (String blocker : new String[] {"paused", "dead", "zeroHull", "hulk", "absent", "wrongHull",
                "zero", "negative", "notFinite"}) {
            Fixture f = new Fixture();
            float amount = 50f;
            switch (blocker) {
                case "paused": f.paused = true; break;
                case "dead": f.alive = false; f.hp = 0f; break;
                case "zeroHull": f.hp = 0f; break;
                case "hulk": f.hulk = true; break;
                case "absent": f.inPlay = false; break;
                case "wrongHull": f.hullId = "shrouded_maw"; break;
                case "zero": amount = 0f; break;
                case "negative": amount = -50f; break;
                case "notFinite": amount = Float.NaN; break;
                default: break;
            }
            float before = f.hp;
            f.step(amount);
            check(f.hullWrites == 0 && f.data.isEmpty(),
                    "Ineligible frames may neither heal nor initialize/spend state: " + blocker);
            near(f.hp, before, "Do not resurrect or heal while " + blocker);
            f.paused = false; f.alive = true; f.hp = 100f; f.hulk = false; f.inPlay = true;
            f.hullId = "chief_navigator_charybdis";
            f.step(10f);
            near(f.hp, 1050f, "Skipped frames cannot advance or exhaust the clock: " + blocker);
        }
        Fixture started = new Fixture();
        started.step(10f);
        started.paused = true;
        started.step(1000f);
        started.paused = false;
        started.step(10f);
        near(started.hp, 1900f, "Pause also preserves an already initialized taper");
        invoke(started.engine, null, 10f);
        invoke(null, started.ship, 10f);
        near(started.hp, 1900f, "Null ship/engine calls are inert");
    }

    private static void snapshotAndIndependentShips() throws Exception {
        Fixture f = new Fixture();
        f.step(10f);
        f.max = 20000f;
        f.step(90f);
        near(f.hp, 5100f, "A later maximum-hull change cannot enlarge or replenish the snapshot pool");
        Fixture fresh = new Fixture();
        fresh.max = 20000f;
        fresh.step(100f);
        near(fresh.hp, 10100f, "A separate ship receives its independent actual-maximum half-hull pool");
        f.max = 10000f;
        f.hp = 100f;
        CombatEngineAPI anotherBattle = mock(CombatEngineAPI.class,
                (name, args) -> name.equals("isEntityInPlay") ? args[0] == f.ship : null);
        invoke(anotherBattle, f.ship, 100f);
        near(f.hp, 5100f, "An actual new combat engine owns a new battle-local pool");
    }

    private static void musicAndEnragePrecedeHealing() throws Exception {
        List<String> cues = new ArrayList<>();
        Global.setSoundPlayer(mock(SoundPlayerAPI.class, (name, args) -> {
            if (name.equals("playCustomMusic")) cues.add((String) args[2]);
            return null;
        }));
        BudaiMusic.playBattle();
        Fixture f = new Fixture();
        f.hp = 4999f;
        BudaiMusic.maintainBattleMusic(f.ship, true);
        f.step(1f);
        check(f.hp > 5000f, "This frame actually heals Budai back above the half-hull threshold");
        BudaiMusic.maintainBattleMusic(f.ship, true);
        check(cues.size() == 2 && cues.get(0).equals(BudaiMusic.BATTLE_ID)
                        && cues.get(1).equals(BudaiMusic.ENRAGED_ID),
                "The actual pre-heal music check latches Savor irreversibly across healing");
        String source = Files.readString(Path.of("src/chiefnavigator/hullmods/CharybdisDistortion.java"));
        int start = source.indexOf("public void advanceInCombat(");
        int stop = source.indexOf("private static void preventRetreat(", start);
        check(start >= 0 && stop > start, "Find the real owned combat integration");
        String body = source.substring(start, stop);
        int music = body.indexOf("BudaiMusic.maintainBattleMusic(");
        int enrage = body.indexOf("beginEnrage(ship)");
        int regen = body.indexOf("BudaiRegeneration.advance(");
        check(music >= 0 && enrage >= 0 && regen > music && regen > enrage,
                "Music and the existing half-hull enrage check must execute before regeneration");
        check(body.indexOf("!engine.isPaused()") < regen && body.indexOf("amount > 0f") < regen,
                "Integration remains inside the unpaused positive-amount guard");
    }

    private static void invoke(CombatEngineAPI engine, ShipAPI ship, float amount) throws Exception {
        BudaiRegeneration.advance(engine, ship, amount);
    }

    private static <T> T mock(Class<T> type, BiFunction<String, Object[], Object> call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("equals")) return proxy == args[0];
                    if (name.equals("hashCode")) return System.identityHashCode(proxy);
                    if (name.equals("toString")) return "mock " + type.getSimpleName();
                    Object result = call.apply(name, args == null ? new Object[0] : args);
                    if (result != null) return result;
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == float.class) return 0f;
                    if (method.getReturnType() == int.class) return 0;
                    return null;
                }));
    }

    private static void near(float actual, float expected, String message) {
        check(Math.abs(actual - expected) <= 0.2f,
                message + "; expected=" + expected + ", actual=" + actual);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
