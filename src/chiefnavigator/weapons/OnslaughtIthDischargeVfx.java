package chiefnavigator.weapons;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.util.List;
import org.lwjgl.util.vector.Vector2f;

/** Shared cosmetic hull lightning for the Ithaca Onslaught and Odyssey. */
public final class OnslaughtIthDischargeVfx extends BaseEveryFrameCombatPlugin {
    private static final String KEY = "chief_navigator_onslaught_ith_discharge_vfx";
    private static final Color FRINGE = new Color(92, 224, 232, 210);
    private static final Color CORE = new Color(229, 255, 250, 235);
    // Onslaught-local forward/port coordinates, following the two painted side hulls.
    private static final float[][][] PATHS = {
        {{198, 70}, {142, 92}, {85, 110}, {18, 114}, {-48, 84}, {-100, 48}},
        {{198, -70}, {142, -92}, {85, -110}, {18, -114}, {-48, -84}, {-100, -48}}
    };
    // Odyssey-local coordinates follow its narrower, asymmetric armor spine.
    private static final float[][][] ODYSSEY_PATHS = {
        {{185,15},{140,34},{92,44},{30,44},{-42,28},{-110,31},{-158,15}},
        {{178,-8},{132,-30},{85,-42},{18,-53},{-50,-47},{-111,-36},{-160,-15}}
    };

    private final CombatEngineAPI engine;
    private final ShipAPI ship;
    private final float[][][] paths;
    private boolean expired;
    private float arcElapsed;
    private int pathIndex;

    private OnslaughtIthDischargeVfx(CombatEngineAPI engine, ShipAPI ship) {
        this.engine = engine;
        this.ship = ship;
        paths = IthacaResearchUpgrades.ITHACA_ODYSSEY_HULL_ID.equals(ship.getHullSpec().getHullId())
                || IthacaResearchUpgrades.ITHACA_ODYSSEY_HULL_ID.equals(ship.getHullSpec().getDParentHullId())
                ? ODYSSEY_PATHS : PATHS;
    }

    public static boolean handles(WeaponAPI weapon) {
        return IthacaResearchUpgrades.isShipNavalLaser(weapon);
    }

    public static void install(CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine == null || !handles(weapon)) return;
        ShipAPI ship = weapon.getShip();
        if (!ship.isAlive() || ship.isHulk()) return;
        Object previous = ship.getCustomData().get(KEY);
        if (previous instanceof OnslaughtIthDischargeVfx) {
            OnslaughtIthDischargeVfx effect = (OnslaughtIthDischargeVfx) previous;
            if (!effect.expired && effect.engine == engine) return;
        }
        OnslaughtIthDischargeVfx effect = new OnslaughtIthDischargeVfx(engine, ship);
        ship.setCustomData(KEY, effect);
        engine.addPlugin(effect);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (expired || engine.isPaused()) return;
        if (!ship.isAlive() || ship.isHulk() || !engine.isEntityInPlay(ship)) {
            expired = true;
            if (ship.getCustomData().get(KEY) == this) ship.getCustomData().remove(KEY);
            engine.removePlugin(this);
            return;
        }
        boolean firing = false;
        float charge = 0f;
        List<WeaponAPI> weapons = ship.getAllWeapons();
        if (weapons != null) for (WeaponAPI weapon : weapons) {
            if (!handles(weapon)) continue;
            boolean beamActive = false;
            for (BeamAPI beam : weapon.getBeams()) {
                if (beam != null && beam.getBrightness() > .05f) { beamActive = true; break; }
            }
            firing |= beamActive;
            if (weapon.getCooldownRemaining() <= 0f) charge = Math.max(charge, weapon.getChargeLevel());
        }
        if (!ship.isPhased() && (firing || charge > .01f)) {
            arcElapsed += amount;
            float interval = firing ? .12f : .22f - .12f * Math.min(1f, charge);
            if (arcElapsed >= interval) {
                arcElapsed = 0f;
                arcAcrossHull(firing ? 1f : charge);
            }
        } else {
            arcElapsed = 0f;
        }
        // TODO: Rework the post-fire steam; the whole-hull plume looked wrong in-game.
        // Keep the heat purge disabled until its visuals are revised.
    }

    private void arcAcrossHull(float charge) {
        float[][] path = paths[pathIndex];
        pathIndex = (pathIndex + 1) % paths.length;
        Vector2f from = hullPoint(path[0][0], path[0][1], 3f);
        for (int i = 1; i < path.length; i++) {
            Vector2f to = hullPoint(path[i][0], path[i][1], 3f);
            engine.spawnEmpArcVisual(from, ship, to, ship, 4f + 4f * charge, FRINGE, CORE);
            from = to;
        }
    }

    private Vector2f hullPoint(float along, float across, float jitter) {
        Vector2f forward = Misc.getUnitVectorAtDegreeAngle(ship.getFacing());
        Vector2f port = new Vector2f(-forward.y, forward.x);
        along += random(-jitter, jitter);
        across += random(-jitter, jitter);
        return new Vector2f(ship.getLocation().x + forward.x * along + port.x * across,
                ship.getLocation().y + forward.y * along + port.y * across);
    }

    private static float random(float low, float high) { return low + (high - low) * (float) Math.random(); }
}
