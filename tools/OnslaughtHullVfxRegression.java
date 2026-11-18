import chiefnavigator.weapons.DriftingWallBeamEffect;
import chiefnavigator.weapons.IthacaResearchUpgrades;
import chiefnavigator.weapons.OnslaughtIthDischargeVfx;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.*;
import java.awt.Color;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;

/** Visual callbacks, multi-gun lifecycle, local hull coverage and no damage/vent commands. */
public final class OnslaughtHullVfxRegression extends WallResearchRegression {
    private static final String HULL = IthacaResearchUpgrades.ITHACA_ONSLAUGHT_HULL_ID;
    private static final String LASER = IthacaResearchUpgrades.ITHACA_ONSLAUGHT_LASER_ID;

    private static final class Fixture {
        final Map<String, Object> data = new HashMap<>();
        final Vector2f position = new Vector2f(1250, -780);
        final Vector2f motion = new Vector2f(27, -13);
        final List<EveryFrameCombatPlugin> plugins = new ArrayList<>();
        final List<WeaponAPI> weapons = new ArrayList<>();
        final List<Vector2f> arcPoints = new ArrayList<>();
        final boolean[] firing = {false, false};
        final float[] charge = {0, 0};
        final ShipAPI ship;
        final CombatEngineAPI engine;
        int installations, removals, particleCount;
        boolean alive = true, present = true, paused;
        final float facing;

        Fixture(String hull, String dParent, float facing) {
            this.facing = facing;
            data.put("chief_navigator_ithaca_research_component_mask", 0);
            data.put("chief_navigator_isa_refraction_enabled", false);
            ShipHullSpecAPI spec = mock(ShipHullSpecAPI.class, (n, a) -> n.equals("getHullId") ? hull
                    : n.equals("getDParentHullId") ? dParent : null);
            ship = mock(ShipAPI.class, (n, a) -> {
                if (n.equals("getHullSpec")) return spec;
                if (n.equals("getCustomData")) return data;
                if (n.equals("setCustomData")) data.put((String) a[0], a[1]);
                if (n.equals("getLocation")) return position;
                if (n.equals("getVelocity")) return motion;
                if (n.equals("getFacing")) return this.facing;
                if (n.equals("getId")) return "hull-vfx";
                if (n.equals("getAllWeapons")) return weapons;
                if (n.equals("isAlive")) return alive;
                if (n.equals("giveCommand") || n.equals("setFlux") || n.equals("setHitpoints"))
                    throw new AssertionError("Visual effect must not change gameplay: " + n);
                return null;
            });
            engine = mock(CombatEngineAPI.class, (n, a) -> {
                if (n.equals("isPaused")) return paused;
                if (n.equals("isEntityInPlay")) return present;
                if (n.equals("getShips")) return List.of(ship);
                if (n.equals("addPlugin")) { plugins.add((EveryFrameCombatPlugin) a[0]); installations++; }
                if (n.equals("removePlugin")) { plugins.remove(a[0]); removals++; }
                if (n.equals("spawnEmpArcVisual")) {
                    check(a[1] == ship && a[3] == ship, "Both visual arc ends follow their own hull");
                    arcPoints.add(new Vector2f((Vector2f) a[0])); arcPoints.add(new Vector2f((Vector2f) a[2]));
                    check((Float) a[4] >= 4 && (Float) a[4] <= 8, "Ship-scale native arc thickness");
                }
                if (n.equals("addNebulaSmokeParticle") || n.equals("addNebulaParticle") || n.equals("addSmoothParticle")) {
                    particleCount++;
                    throw new AssertionError("Post-fire steam and heat-purge particles must remain disabled");
                }
                if (n.equals("spawnEmpArc") || n.equals("spawnEmpArcPierceShields") || n.startsWith("applyDamage"))
                    throw new AssertionError("Hull discharge must remain purely visual");
                return null;
            });
            for (int index = 0; index < 2; index++) {
                final int mount = index;
                BeamAPI beam = mock(BeamAPI.class, (n, a) -> n.equals("getBrightness") ? 1f : null);
                weapons.add(mock(WeaponAPI.class, (n, a) -> {
                    if (n.equals("getId")) return LASER;
                    if (n.equals("getShip")) return ship;
                    if (n.equals("getLocation")) return position;
                    if (n.equals("getBeams")) return firing[mount] ? List.of(beam) : List.of();
                    if (n.equals("getChargeLevel")) return charge[mount];
                    return null;
                }));
            }
        }

        void installThroughWeapons() {
            DriftingWallBeamEffect effect = new DriftingWallBeamEffect();
            for (int repeat = 0; repeat < 3; repeat++) for (WeaponAPI weapon : weapons) effect.advance(.01f, engine, weapon);
        }

        void step(float amount) {
            for (EveryFrameCombatPlugin plugin : List.copyOf(plugins)) plugin.advance(amount, List.of());
        }

        Vector2f local(Vector2f world) {
            double angle = Math.toRadians(facing);
            float x = world.x - position.x, y = world.y - position.y;
            return new Vector2f((float) (x * Math.cos(angle) + y * Math.sin(angle)),
                    (float) (-x * Math.sin(angle) + y * Math.cos(angle)));
        }

        void coverage(List<Vector2f> points, String kind) {
            float min = Float.MAX_VALUE, max = -Float.MAX_VALUE, left = 0, right = 0;
            for (Vector2f point : points) {
                Vector2f local = local(point);
                check(local.x > -120 && local.x < 235 && Math.abs(local.y) < 125,
                        kind + " origins stay on the authored bow-to-stern hull network");
                check(Math.abs(local.y) > 40, "The central coaxial lightning path is removed");
                min = Math.min(min, local.x); max = Math.max(max, local.x);
                left = Math.max(left, local.y); right = Math.min(right, local.y);
            }
            check(max - min > 285 && left > 100 && right < -100,
                    kind + " spans bow, stern and both sides rather than the two gun pivots");
        }
    }

    private static void verifyCycle(float facing) {
        Fixture f = new Fixture(HULL, null, facing);
        f.installThroughWeapons();
        check(f.installations == 1 && f.plugins.size() == 1, "Both guns share exactly one hull controller");
        f.step(.3f);
        check(f.arcPoints.isEmpty() && f.particleCount == 0, "Idle hull has no lightning or heat purge");
        f.charge[0] = .7f; f.charge[1] = .7f;
        for (int pulse = 0; pulse < 3; pulse++) f.step(.3f);
        f.coverage(f.arcPoints, "Charge lightning");
        f.charge[0] = 0; f.charge[1] = 0; f.step(.3f);
        check(f.particleCount == 0, "Cancelled charge never produces a post-fire vent");
        f.arcPoints.clear(); f.firing[0] = true; f.firing[1] = true;
        for (int pulse = 0; pulse < 6; pulse++) f.step(.15f);
        f.coverage(f.arcPoints, "Firing lightning");
        f.firing[0] = false; f.step(.15f);
        check(f.particleCount == 0 && !f.arcPoints.isEmpty(), "No vent while the second gun is still firing");
        f.paused = true; f.firing[1] = false;
        int arcs = f.arcPoints.size(); f.step(1f);
        check(f.particleCount == 0 && f.arcPoints.size() == arcs, "Pause freezes visual progression");
        f.paused = false; f.step(.1f);
        check(f.particleCount == 0, "The final gun stopping does not emit the retired steam plume");
        for (int repeat = 0; repeat < 5; repeat++) f.step(.2f);
        check(f.particleCount == 0, "Idle frames keep the retired purge disabled");
        Vector2f displacement = new Vector2f(400, 275);
        f.position.translate(displacement.x, displacement.y);
        f.arcPoints.clear();
        f.firing[1] = true; f.step(.2f); f.step(.2f); f.coverage(f.arcPoints, "Moving side-hull lightning");
        f.firing[1] = false; f.step(.2f);
        check(f.particleCount == 0, "Later discharges cannot re-enable post-fire steam");
        f.present = false; f.step(.1f);
        check(f.plugins.isEmpty() && f.removals == 1
                        && !f.data.containsKey("chief_navigator_onslaught_ith_discharge_vfx"),
                "Removed ship releases its visual plugin and combat-only cache");
    }

    public static void main(String[] args) {
        SettingsAPI oldSettings = Global.getSettings(); SectorAPI oldSector = Global.getSector();
        try {
            Global.setSettings(mock(SettingsAPI.class, (n, a) -> n.equals("getColor") ? Color.WHITE : null));
            Global.setSector(null);
            for (float facing : new float[]{0, 90, 180, 270}) verifyCycle(facing);
            Fixture damaged = new Fixture(HULL + "_d", HULL, 37);
            damaged.installThroughWeapons();
            check(damaged.installations == 1, "The exact damaged ITH parent retains the hull discharge effect");
            Fixture foreign = new Fixture("onslaught", null, 0);
            OnslaughtIthDischargeVfx.install(foreign.engine, foreign.weapons.get(0));
            check(foreign.installations == 0, "Stock Onslaught and foreign hulls never acquire this visual");
            Fixture dead = new Fixture(HULL, null, 0); dead.alive = false;
            OnslaughtIthDischargeVfx.install(dead.engine, dead.weapons.get(0));
            check(dead.installations == 0, "No discharge controller installs on a dead hull");
            System.out.println("PASS: side-hull charge/firing arcs with no central coaxial path, four world orientations, "
                    + "shared dual-gun lifecycle, no steam/purge, cancellation/pause/removal, D-parent scope and visual-only APIs");
        } finally { Global.setSettings(oldSettings); Global.setSector(oldSector); }
    }
}
