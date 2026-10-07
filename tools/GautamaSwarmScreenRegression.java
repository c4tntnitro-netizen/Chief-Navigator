import chiefnavigator.hullmods.ScyllaFragileSwarmHullmod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;

/** Verifies that Gautama's replenishing escorts remain easy and quiet to kill. */
public final class GautamaSwarmScreenRegression {
    public static void main(String[] args) {
        StatBonus hull = new StatBonus();
        MutableStat hullDamage = new MutableStat(1f);
        MutableStat armorDamage = new MutableStat(1f);
        MutableShipStatsAPI stats = WallResearchRegression.mock(
                MutableShipStatsAPI.class, (n, a) -> {
                    if (n.equals("getHullBonus")) return hull;
                    if (n.equals("getHullDamageTakenMult")) return hullDamage;
                    if (n.equals("getArmorDamageTakenMult")) return armorDamage;
                    return null;
                });

        ScyllaFragileSwarmHullmod hullmod =
                new ScyllaFragileSwarmHullmod();
        hullmod.applyEffectsBeforeShipCreation(
                ShipAPI.HullSize.FIGHTER, stats, "test");
        WallResearchRegression.check(Math.abs(hull.getMult() - 0.25f) < .0001f,
                "Gautama swarm hull must be quarter strength");
        WallResearchRegression.check(
                Math.abs(hullDamage.getMult() - 1.5f) < .0001f,
                "Gautama swarm must take 1.5x hull damage");
        WallResearchRegression.check(
                Math.abs(armorDamage.getMult() - 1.5f) < .0001f,
                "Gautama swarm must take 1.5x armor damage");

        float[] explosionScale = {1f};
        float[] hulkChance = {1f};
        boolean[] hidden = {false};
        ShipAPI fighter = WallResearchRegression.mock(ShipAPI.class, (n, a) -> {
            if (n.equals("setExplosionScale")) explosionScale[0] = (Float) a[0];
            if (n.equals("setHulkChanceOverride")) hulkChance[0] = (Float) a[0];
            if (n.equals("setDoNotRender")) hidden[0] = (Boolean) a[0];
            return null;
        });
        hullmod.applyEffectsAfterShipCreation(fighter, "test");
        WallResearchRegression.check(explosionScale[0] == 0f,
                "Gautama swarm ship explosion must be suppressed");
        WallResearchRegression.check(hulkChance[0] == 0f,
                "Gautama swarm hulk must be suppressed");
        WallResearchRegression.check(hidden[0],
                "Only the fragment cloud, not the carrier sprite, should render");

        System.out.println("PASS: Gautama escorts are fragile and have quiet "
                + "underlying-ship deaths.");
    }
}
