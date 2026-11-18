import chiefnavigator.weapons.DriftingWallBeamEffect;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.BeamWeaponSpecAPI;
import java.awt.Color;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;

/** Exercise actual weapon callbacks: quarter-strength single hop and no shield modifier. */
public final class IsaNavalRefractionRegression extends WallResearchRegression {
    static void verifyHull(String hull, String weaponId) {
        checkHop(hull, weaponId, 1, true, true, false, 2000f, true);
        checkHop(hull, weaponId, 2, true, true, false, 2000f, true);
        checkHop(hull, weaponId, 1, false, true, false, 2000f, false);
        checkHop(hull, weaponId, 1, true, false, false, 2000f, false);
        checkHop(hull, weaponId, 1, true, true, true, 2000f, false);
        checkHop(hull, weaponId, 1, true, true, false, 4000f, true);
    }

    static void checkHop(String hull, String weaponId, int mounts, boolean unlocked,
            boolean shieldOn, boolean paused, float dps, boolean expected) {
        ShipAPI source = ship(hull, 0, 0, new Vector2f());
        source.getCustomData().put("chief_navigator_isa_refraction_enabled", unlocked);
        ShieldAPI shield = mock(ShieldAPI.class, (n, a) -> n.equals("isOn") || n.equals("isWithinArc") ? shieldOn : null);
        ShipAPI primary = mock(ShipAPI.class, (n, a) -> {
            if (n.equals("getShield")) return shield;
            if (n.equals("isAlive")) return true;
            if (n.equals("getOwner")) return 1;
            if (n.equals("getLocation")) return new Vector2f(1000, 0);
            if (n.equals("getCollisionRadius")) return 100f;
            if (n.equals("addListener") || n.equals("addListenerIfNeeded"))
                throw new AssertionError("Isa must never attach a primary shield-damage modifier");
            return null;
        });
        ShipAPI secondary = ship("enemy_secondary", 0, 1, new Vector2f(1800, 0));
        ShipAPI farther = ship("enemy_farther", 0, 1, new Vector2f(2200, 0));
        ShipAPI ally = ship("ally", 0, 0, new Vector2f(1100, 0));
        int[] hops = {0}; float[] damage = {0};
        CombatEngineAPI engine = mock(CombatEngineAPI.class, (n, a) -> {
            if (n.equals("isPaused")) return paused;
            if (n.equals("getShips")) return List.of(primary, secondary, farther, ally);
            if (n.equals("spawnEmpArc")) {
                check(a[2] == primary && a[3] == secondary, "One hop goes only to the nearest eligible secondary enemy");
                check(a[4] == DamageType.ENERGY && (Float) a[6] == 0f, "Secondary hop deals energy damage without EMP");
                check(Math.abs((Float) a[5] - dps * .25f * .25f) < .001f, "Each hop tick is exactly quarter strength");
                hops[0]++; damage[0] += (Float) a[5];
            }
            return null;
        });
        List<WeaponAPI> weapons = new ArrayList<>();
        for (int i = 0; i < mounts; i++) {
            WeaponAPI.DerivedWeaponStatsAPI stats = mock(WeaponAPI.DerivedWeaponStatsAPI.class,
                    (n, a) -> n.equals("getDps") ? dps * .1f : null);
            BeamWeaponSpecAPI spec = mock(BeamWeaponSpecAPI.class,
                    (n, a) -> n.equals("getDamagePerSecond") ? dps : null);
            WeaponAPI[] ref = {null};
            BeamAPI beam = mock(BeamAPI.class, (n, a) -> {
                if (n.equals("getBrightness")) return 1f;
                if (n.equals("getDamageTarget")) return primary;
                if (n.equals("getFrom")) return new Vector2f();
                if (n.equals("getTo") || n.equals("getRayEndPrevFrame")) return new Vector2f(1000, 0);
                if (n.equals("getSource")) return source;
                if (n.equals("getWeapon")) return ref[0];
                return null;
            });
            ref[0] = mock(WeaponAPI.class, (n, a) -> {
                if (n.equals("getId")) return weaponId;
                if (n.equals("getShip")) return source;
                if (n.equals("getLocation")) return new Vector2f();
                if (n.equals("getBeams")) return List.of(beam);
                if (n.equals("getDerivedStats")) return stats;
                if (n.equals("getSpec")) return spec;
                return null;
            });
            weapons.add(ref[0]);
        }
        DriftingWallBeamEffect effect = new DriftingWallBeamEffect();
        for (int tick = 0; tick < 20; tick++) for (WeaponAPI weapon : weapons) effect.advance(.25f, engine, weapon);
        check(hops[0] == (expected ? mounts * 20 : 0), "No recursive or ineligible refraction ticks");
        check(Math.abs(damage[0] - (expected ? mounts * dps * 5 * .25f : 0f)) < .001f,
                "Five-second secondary damage stays at one quarter of the primary rate");
    }

    public static void main(String[] args) {
        SettingsAPI oldSettings = Global.getSettings(); SectorAPI oldSector = Global.getSector();
        try {
            Global.setSettings(mock(SettingsAPI.class, (n, a) -> n.equals("getColor") ? Color.WHITE : null));
            Global.setSector(null);
            verifyHull("chief_navigator_onslaught_ith", "chief_navigator_onslaught_ith_naval_laser");
            verifyHull("chief_navigator_ithaca_intact_base", "chief_navigator_drifting_wall_colossal_inert");
            checkHop("onslaught", "chief_navigator_onslaught_ith_naval_laser", 1, true, true, false, 2000, false);
            checkHop("chief_navigator_drifting_wall_base", "chief_navigator_drifting_wall_colossal_inert", 1, true, true, false, 2000, false);
            System.out.println("PASS: quarter-strength one-hop refraction, both ITH mounts and fortress, no shield modifier, "
                    + "no recursion, recruitment/pause/shield gates and ordinary/hostile hull isolation");
        } finally { Global.setSettings(oldSettings); Global.setSector(oldSector); }
    }
}
