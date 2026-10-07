import chiefnavigator.hullmods.WallContactSafetyHullmod;
import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

public class WallContactRegression {
    public static void main(String[] args) {
        ShipAPI ship = WallResearchRegression.mock(ShipAPI.class, (n,a) -> null);
        WallContactSafetyHullmod.BodyImpactFilter filter =
                new WallContactSafetyHullmod.BodyImpactFilter();
        for (boolean shields : new boolean[]{false, true}) {
            MutableStat modifier = new MutableStat(1f);
            DamageAPI damage = WallResearchRegression.mock(DamageAPI.class,
                    (n,a) -> n.equals("getModifier") ? modifier : null);
            filter.modifyDamageDealt(null, ship, damage, new Vector2f(), shields);
            WallResearchRegression.check(modifier.getModifiedValue() == 0f,
                    "Body impact must be harmless on hull and shield");
        }
        Object[] weaponContexts = {
            WallResearchRegression.mock(BeamAPI.class, (n,a) -> null),
            WallResearchRegression.mock(DamagingProjectileAPI.class, (n,a) -> null),
            WallResearchRegression.mock(EmpArcEntityAPI.class, (n,a) -> null)
        };
        for (Object context : weaponContexts) {
            MutableStat modifier = new MutableStat(1f);
            DamageAPI damage = WallResearchRegression.mock(DamageAPI.class,
                    (n,a) -> n.equals("getModifier") ? modifier : null);
            WallResearchRegression.check(filter.modifyDamageDealt(context, ship,
                    damage, new Vector2f(), false) == null, "Weapon context untouched");
            WallResearchRegression.check(modifier.getModifiedValue() == 1f,
                    "Beam, projectile and EMP damage preserved");
        }
        System.out.println("PASS: hull/shield body damage zero; beams, projectiles and EMP untouched.");
    }
}
