import chiefnavigator.hullmods.IthacaSiegeStalemateHullmod;
import chiefnavigator.hullmods.SpartanThreatWardHullmod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import java.util.HashMap;
import java.util.Map;

/** Headless checks that legacy siege hullmods are marker-only compatibility. */
public final class IthacaSiegeStalemateRegression {
    private static ShipAPI ship(
            String hullId, boolean siegeInvasion, int[] listenersAdded) {
        Map<String, Object> customData = new HashMap<String, Object>();
        ShipHullSpecAPI hull = WallResearchRegression.mock(
                ShipHullSpecAPI.class,
                (name, args) -> name.equals("getHullId") ? hullId : null);
        ShipVariantAPI variant = WallResearchRegression.mock(
                ShipVariantAPI.class,
                (name, args) -> name.equals("hasHullMod")
                        && siegeInvasion
                        && IthacaSiegeStalemateHullmod.HULLMOD_ID.equals(
                                args[0]));
        return WallResearchRegression.mock(ShipAPI.class, (name, args) -> {
            if (name.equals("getHullSpec")) return hull;
            if (name.equals("getVariant")) return variant;
            if (name.equals("getCustomData")) return customData;
            if (name.equals("setCustomData")) {
                customData.put((String) args[0], args[1]);
            }
            if (name.equals("addListener")) listenersAdded[0]++;
            return null;
        });
    }

    public static void main(String[] args) {
        int[] listenersAdded = {0};
        ShipAPI invasion = ship(
                "chief_navigator_starving_hive", true, listenersAdded);
        ShipAPI reconstructedGautama = ship(
                "chief_navigator_scylla", false, listenersAdded);
        ShipAPI spartan = ship("onslaught_xiv", false, listenersAdded);
        IthacaSiegeStalemateHullmod.markInvasionShip(
                reconstructedGautama);
        WallResearchRegression.check(
                IthacaSiegeStalemateHullmod.isStalemateInvasion(
                        reconstructedGautama),
                "Combat-created Gautama retains its invasion marker");
        WallResearchRegression.check(
                IthacaSiegeStalemateHullmod.isStalemateInvasion(invasion),
                "Serialized invasion hullmod remains a recognized marker");
        new IthacaSiegeStalemateHullmod().applyEffectsAfterShipCreation(
                invasion, IthacaSiegeStalemateHullmod.HULLMOD_ID);
        new SpartanThreatWardHullmod().applyEffectsAfterShipCreation(
                spartan, SpartanThreatWardHullmod.HULLMOD_ID);
        WallResearchRegression.check(
                listenersAdded[0] == 0,
                "Legacy hullmods must install no combat damage listeners");

        System.out.println("PASS: invasion identification remains intact; "
                + "legacy siege and Spartan hullmods alter no combat damage.");
    }
}
