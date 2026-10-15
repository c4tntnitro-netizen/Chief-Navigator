package chiefnavigator.dwellers;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

/** The port owns its IDs; SEP and the vanilla Dweller registry remain untouched. */
public final class AviciDwellerContent {
    public static final String BANSHEE_HULL = "chief_navigator_avici_banshee";
    public static final String BRAIN_HULL = "chief_navigator_avici_brain";
    public static final String BUDAI_HULL = "chief_navigator_avici_hunter";
    public static final String BANSHEE_VARIANT = BANSHEE_HULL + "_Standard";
    public static final String BRAIN_VARIANT = BRAIN_HULL + "_Standard";
    public static final String BUDAI_VARIANT = BUDAI_HULL + "_Budai";
    public static final String HULLMOD = "chief_navigator_avici_dweller";

    private AviciDwellerContent() { }

    public static boolean isBudaiHull(String hullId) {
        return BUDAI_HULL.equals(hullId);
    }

    public static boolean isPortedHull(String hullId) {
        return BANSHEE_HULL.equals(hullId) || BRAIN_HULL.equals(hullId)
                || isBudaiHull(hullId);
    }

    public static boolean isBrain(ShipAPI ship) {
        return ship != null && ship.getHullSpec() != null
                && BRAIN_HULL.equals(ship.getHullSpec().getHullId());
    }

    /** Preserve the Brain's original allies without granting its aura to Budai. */
    public static boolean isBrainAuraAlly(String hullId) {
        return BANSHEE_HULL.equals(hullId) || BRAIN_HULL.equals(hullId)
                || hullId != null && hullId.startsWith("shrouded_");
    }

    public static boolean isDomainAlly(ShipAPI ship) {
        if (ship == null || ship.getHullSpec() == null) return false;
        String hullId = ship.getHullSpec().getHullId();
        return isPortedHull(hullId) || hullId != null && hullId.startsWith("shrouded_");
    }

    /** Native vector math replaces the port's former LazyLib point helper. */
    static Vector2f pointAt(Vector2f center, float radius, float angle) {
        Vector2f offset = Misc.getUnitVectorAtDegreeAngle(angle);
        offset.scale(radius);
        return Vector2f.add(center, offset, new Vector2f());
    }
}
