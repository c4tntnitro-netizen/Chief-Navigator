import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Focused headless contract for Labor V's repeatable Gautama pity cycle. */
public final class GautamaFinalLaborPityRegression {
    public static void main(String[] args) throws Exception {
        Class<?> type = Class.forName(
                "chiefnavigator.hullmods.GautamaReincarnation");
        Method enable = method(type, "enableFinalLaborMode",
                CombatEngineAPI.class);
        Method suppress = method(type, "suppressForBattle",
                CombatEngineAPI.class);
        Method canBegin = method(type, "canBegin", CombatEngineAPI.class);
        Method death = method(type, "noteGautamaDeath",
                CombatEngineAPI.class, ShipAPI.class);
        Method record = method(type, "recordFinalLaborPlayerKill",
                CombatEngineAPI.class, ShipAPI.class);
        Method kills = method(type, "getFinalLaborPityKills",
                CombatEngineAPI.class);
        Method pity = method(type, "getFinalLaborPityChanceForKills",
                int.class);
        Method chance = method(type, "getFinalLaborReincarnationChance",
                float.class, int.class);
        Method reset = method(type,
                "resetFinalLaborPityAfterSuccessfulConstruction",
                CombatEngineAPI.class);
        Method warning = method(type, "claimSuccessfulConstructionWarning",
                CombatEngineAPI.class, ShipAPI.class);
        Method getState = method(type, "getState", CombatEngineAPI.class);
        Method attempt = method(type, "attemptReincarnation",
                CombatEngineAPI.class, ShipAPI.class, float.class);

        Map<String, Object> customData = new HashMap<>();
        List<ShipAPI> ships = new ArrayList<>();
        CombatEngineAPI engine = WallResearchRegression.mock(
                CombatEngineAPI.class, (name, argv) -> {
                    if (name.equals("getCustomData")) return customData;
                    if (name.equals("getShips")) return ships;
                    return null;
                });
        ShipHullSpecAPI gautamaSpec = WallResearchRegression.mock(
                ShipHullSpecAPI.class, (name, argv) ->
                        name.equals("getHullId")
                                ? "chief_navigator_scylla" : null);
        ShipAPI gautama = WallResearchRegression.mock(
                ShipAPI.class, (name, argv) -> {
                    if (name.equals("getHullSpec")) return gautamaSpec;
                    if (name.equals("isAlive")) return true;
                    return null;
                });
        ShipHullSpecAPI frigateSpec = WallResearchRegression.mock(
                ShipHullSpecAPI.class, (name, argv) ->
                        name.equals("getHullId")
                                ? "chief_navigator_starving_skirmish_unit"
                                : null);
        ShipAPI frigate = WallResearchRegression.mock(
                ShipAPI.class, (name, argv) -> {
                    if (name.equals("getHullSpec")) return frigateSpec;
                    if (name.equals("getHullSize")) {
                        return ShipAPI.HullSize.FRIGATE;
                    }
                    if (name.equals("getOriginalOwner")) return 1;
                    return null;
                });

        enable.invoke(null, engine);
        check((Boolean) canBegin.invoke(null, engine),
                "Labor V should begin with an open reconstruction cycle");
        ships.add(gautama);
        check(!(Boolean) canBegin.invoke(null, engine),
                "A live Gautama must block simultaneous construction");
        death.invoke(null, engine, gautama);
        ships.clear();
        check((Boolean) canBegin.invoke(null, engine),
                "Gautama's Labor V death must open another cycle");

        ShipAPI reincarnated = WallResearchRegression.mock(
                ShipAPI.class, (name, argv) -> {
                    if (name.equals("getHullSpec")) return gautamaSpec;
                    if (name.equals("isAlive")) return true;
                    return null;
                });
        ships.add(reincarnated);
        check(!(Boolean) canBegin.invoke(null, engine),
                "A reincarnated Gautama must close the current cycle");
        ships.clear();
        death.invoke(null, engine, gautama);
        check(!(Boolean) canBegin.invoke(null, engine),
                "Repeated notification from an old hulk must not clear a "
                        + "newly tracked Gautama");
        death.invoke(null, engine, reincarnated);
        check((Boolean) canBegin.invoke(null, engine),
                "The new Gautama's own death must open the next cycle");

        for (int i = 1; i <= 12; i++) {
            check((Boolean) record.invoke(null, engine, frigate),
                    "Eligible failed death should be recorded at step " + i);
        }
        check((Integer) kills.invoke(null, engine) == 10,
                "Pity kill count must saturate at ten");
        Object state = getState.invoke(null, engine);
        Field warningShown = state.getClass().getDeclaredField("finalLaborWarningShown");
        Field tracked = state.getClass().getDeclaredField("gautama");
        warningShown.setAccessible(true);
        tracked.setAccessible(true);
        // Guarantee the roll succeeds, then let the real begin path fail closed
        // because this headless engine has no fleet manager or constructed ship.
        attempt.invoke(null, engine, frigate, 1f);
        check(!warningShown.getBoolean(state)
                        && warning.invoke(null, engine, gautama) == null,
                "Failed rolls/builds cannot consume or emit the first successful-reincarnation warning");
        ShipAPI firstSuccessfulConstruction = WallResearchRegression.mock(
                ShipAPI.class, (name, argv) -> {
                    if (name.equals("getHullSpec")) return gautamaSpec;
                    if (name.equals("isAlive")) return true;
                    return null;
                });
        tracked.set(state, firstSuccessfulConstruction);
        check("WARNING: Multiple Reincarnations Detected".equals(
                        warning.invoke(null, engine, firstSuccessfulConstruction))
                        && warningShown.getBoolean(state)
                        && warning.invoke(null, engine, firstSuccessfulConstruction) == null,
                "The first actual tracked Labor V construction claims the exact battle warning once");
        reset.invoke(null, engine);
        check((Integer) kills.invoke(null, engine) == 0,
                "Successful construction must reset the pity cycle");
        death.invoke(null, engine, firstSuccessfulConstruction);
        check((Boolean) record.invoke(null, engine, frigate)
                        && (Integer) kills.invoke(null, engine) == 1,
                "A post-respawn death must begin a fresh pity cycle");
        ShipAPI laterConstruction = WallResearchRegression.mock(
                ShipAPI.class, (name, argv) -> {
                    if (name.equals("getHullSpec")) return gautamaSpec;
                    if (name.equals("isAlive")) return true;
                    return null;
                });
        tracked.set(state, laterConstruction);
        check(warning.invoke(null, engine, laterConstruction) == null,
                "Resetting pity and reconstructing again must not reset the battle warning");
        death.invoke(null, engine, laterConstruction);

        ShipAPI invalidOwner = WallResearchRegression.mock(
                ShipAPI.class, (name, argv) -> {
                    if (name.equals("getHullSpec")) return frigateSpec;
                    if (name.equals("getHullSize")) {
                        return ShipAPI.HullSize.FRIGATE;
                    }
                    if (name.equals("getOriginalOwner")) return 100;
                    return null;
                });
        check(!(Boolean) record.invoke(null, engine, invalidOwner)
                        && (Integer) kills.invoke(null, engine) == 1,
                "Neutral or invalid-owner hulls must not advance pity");
        check(close((Float) pity.invoke(null, -5), 0f)
                        && close((Float) pity.invoke(null, 0), 0f)
                        && close((Float) pity.invoke(null, 4), 0.4f)
                        && close((Float) pity.invoke(null, 10), 1f)
                        && close((Float) pity.invoke(null, 100), 1f),
                "Pity bonus must be ten points per prior failure in [0, 100%]");
        check(close((Float) chance.invoke(null, 0.1f, 0), 0.1f)
                        && close((Float) chance.invoke(null, 0.1f, 4), 0.5f)
                        && close((Float) chance.invoke(null, 0.5f, 5), 1f),
                "Hull base plus prior-failure pity must clamp at certainty");

        String implementation = Files.readString(Path.of(
                "src/chiefnavigator/hullmods/GautamaReincarnation.java"),
                StandardCharsets.UTF_8);
        check(implementation.contains("if (beginInternal(engine, source))")
                        && implementation.contains(
                                "resetFinalLaborPityAfterSuccessfulConstruction(engine)"),
                "The successful-construction branch must reset pity");

        suppress.invoke(null, engine);
        check(!(Boolean) canBegin.invoke(null, engine)
                        && (Integer) kills.invoke(null, engine) == 0
                        && warning.invoke(null, engine, laterConstruction) == null,
                "Explicit suppression must dominate Labor V mode");

        System.out.println("PASS: Labor V Gautama cycles are repeatable, "
                + "single-instance, +10-point pity-backed, and suppressible; "
                + "only the first actual reincarnation warns.");
    }

    private static Method method(
            Class<?> owner, String name, Class<?>... parameters)
            throws Exception {
        Method result = owner.getDeclaredMethod(name, parameters);
        result.setAccessible(true);
        return result;
    }

    private static boolean close(float actual, float expected) {
        return Math.abs(actual - expected) < 0.0001f;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
