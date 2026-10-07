package chiefnavigator.hullmods;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;

/** A finite, hull-only reserve; time passes only while Budai can use healing. */
public final class BudaiRegeneration {
    private static final String STATE_KEY = "chief_navigator_budai_regeneration";
    private static final String HULL_ID = "chief_navigator_charybdis";
    private static final double HEALING_SECONDS = 100d;
    private static final double HULL_FRACTION = 0.5d;

    private BudaiRegeneration() { }

    public static boolean isBudai(ShipAPI ship) {
        return ship != null && ship.getHullSpec() != null
                && HULL_ID.equals(ship.getHullSpec().getHullId());
    }

    public static void advance(CombatEngineAPI engine, ShipAPI ship, float amount) {
        if (engine == null || engine.isPaused() || !Float.isFinite(amount)
                || amount <= 0f || !isBudai(ship) || !ship.isAlive()
                || ship.isHulk() || !engine.isEntityInPlay(ship)) {
            return;
        }
        float maxHull = ship.getMaxHitpoints();
        float hull = ship.getHitpoints();
        if (!Float.isFinite(maxHull) || maxHull <= 0f
                || !Float.isFinite(hull) || hull <= 0f) {
            return;
        }

        Object existing = ship.getCustomData().get(STATE_KEY);
        State state;
        if (existing instanceof State && ((State) existing).engine == engine) {
            state = (State) existing;
        } else {
            state = new State(engine, maxHull);
            ship.setCustomData(STATE_KEY, state);
        }

        double missing = Math.min(maxHull, state.maxHull) - (double) hull;
        double remaining = state.reserve - state.healed;
        if (missing <= 0d || remaining <= 0d) {
            return;
        }

        double end = Math.min(HEALING_SECONDS, state.elapsed + amount);
        // Integrating the ramp is frame-size independent. Any fraction too small
        // for float hull is carried forward, rather than lost at high frame rates.
        double available = Math.max(0d, cumulative(state, end) - state.healed);
        double healing = Math.min(Math.min(missing, remaining), available);
        if (healing <= 0d) {
            return;
        }
        double desiredTotal = state.healed + healing;
        double fraction = Math.min(1d, desiredTotal / state.reserve);
        double healingEnd = HEALING_SECONDS * fraction
                / (1d + Math.sqrt(Math.max(0d, 1d - fraction)));
        // Reaching full hull partway through a tick spends only the needed time.
        state.elapsed = Math.max(state.elapsed, Math.min(end, healingEnd));

        float target = (float) Math.min(Math.min(maxHull, state.maxHull), hull + healing);
        if ((double) target - hull > healing) {
            target = Math.nextDown(target);
        }
        if (target > hull) {
            ship.setHitpoints(target);
            state.healed += (double) target - hull;
        }
    }

    private static double cumulative(State state, double elapsed) {
        double fraction = elapsed / HEALING_SECONDS;
        return state.reserve * fraction * (2d - fraction);
    }

    private static final class State {
        final CombatEngineAPI engine;
        final double maxHull;
        final double reserve;
        double elapsed;
        double healed;

        State(CombatEngineAPI engine, float maxHull) {
            this.engine = engine;
            this.maxHull = maxHull;
            this.reserve = maxHull * HULL_FRACTION;
        }
    }
}
