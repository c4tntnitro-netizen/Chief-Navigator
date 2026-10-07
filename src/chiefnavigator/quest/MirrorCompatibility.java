package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;

/** Explicit mirror exclusions and preservation of ship/system AI ownership. */
final class MirrorCompatibility {
    static final String OPT_OUT_TAG = "chief_navigator_no_mirror";
    static final String EXCLUDED_HULLS_SETTING = "chiefNavigatorMirrorExcludedHullIds";
    private static final String SAVED_AI_KEY =
            "chief_navigator_ungaikyo_construction_ai";
    private static final String BASIC_SHIP_AI_CLASS =
            "com.fs.starfarer.combat.ai.BasicShipAI";

    private MirrorCompatibility() { }

    /** A base-hull exclusion also covers skins/D-hulls; a module opt-out excludes its root. */
    static boolean isMirrorOptedOut(ShipVariantAPI variant) {
        Set<String> excluded = new HashSet<String>();
        if (Global.getSettings() != null) {
            try {
                JSONArray configured = Global.getSettings().getJSONArray(EXCLUDED_HULLS_SETTING);
                if (configured != null) {
                    for (int index = 0; index < configured.length(); index++) {
                        String id = configured.optString(index, "").trim();
                        if (!id.isEmpty()) excluded.add(id);
                    }
                }
            } catch (JSONException failure) {
                throw new IllegalStateException("Invalid mirror hull exclusion configuration", failure);
            }
        }
        return isMirrorOptedOut(variant, excluded,
                Collections.newSetFromMap(new IdentityHashMap<ShipVariantAPI, Boolean>()));
    }

    private static boolean isMirrorOptedOut(ShipVariantAPI variant,
            Set<String> excluded, Set<ShipVariantAPI> seen) {
        if (variant == null || !seen.add(variant)) return false;
        if (variant.hasTag(OPT_OUT_TAG) || isHullOptedOut(variant.getHullSpec(), excluded,
                Collections.newSetFromMap(new IdentityHashMap<ShipHullSpecAPI, Boolean>()))) {
            return true;
        }
        List<String> slots = variant.getModuleSlots();
        if (slots != null) {
            for (String slot : slots) {
                if (isMirrorOptedOut(variant.getModuleVariant(slot), excluded, seen)) return true;
            }
        }
        return false;
    }

    private static boolean isHullOptedOut(ShipHullSpecAPI hull,
            Set<String> excluded, Set<ShipHullSpecAPI> seen) {
        if (hull == null || !seen.add(hull)) return false;
        return hull.hasTag(OPT_OUT_TAG)
                || excluded.contains(hull.getHullId())
                || excluded.contains(hull.getBaseHullId())
                || excluded.contains(hull.getDParentHullId())
                || isHullOptedOut(hull.getBaseHull(), excluded, seen)
                || isHullOptedOut(hull.getDParentHull(), excluded, seen);
    }

    /** Capture before construction shuts down AI, without advancing the delegate. */
    static void rememberConstructionAI(ShipAPI ship) {
        if (ship == null) return;
        Map<String, Object> data = ship.getCustomData();
        if (data == null) return;
        ShipAIPlugin ai = ship.getShipAI();
        if (ai != null && (!data.containsKey(SAVED_AI_KEY) || !isGenericDefaultAI(ai))) {
            // Save native BasicShipAI too: its system/behavior modules already
            // belong to this ship. A late creator may replace it with a custom
            // implementation; preserve that newer specialized context instead.
            data.put(SAVED_AI_KEY, ai);
        }
    }

    /** Consume the saved wrapper by identity; no private delegate access is needed. */
    static ShipAIPlugin takeConstructionAI(ShipAPI ship) {
        if (ship == null || ship.getCustomData() == null) return null;
        Object saved = ship.getCustomData().remove(SAVED_AI_KEY);
        return saved instanceof ShipAIPlugin ? (ShipAIPlugin) saved : null;
    }

    static void forgetConstructionAI(ShipAPI ship) {
        if (ship != null && ship.getCustomData() != null) {
            ship.getCustomData().remove(SAVED_AI_KEY);
        }
    }

    /**
     * Native getShipAI returns BasicShipAI directly, but wraps public mod AIs in
     * Ship$ShipAIWrapper. Never assume that a wrapper's non-null config is vanilla.
     * Native core and mods (e.g. KOL) can configure BasicShipAI differently;
     * it remains the generic implementation and gets the authored Fearless
     * flags in place, without replacing its native modules. Unknown subclasses
     * and wrappers retain their required implementation/configuration unchanged.
     * This checks the public object's exact class name, not engine internals.
     */
    static boolean isGenericDefaultAI(ShipAIPlugin ai) {
        return ai != null && BASIC_SHIP_AI_CLASS.equals(ai.getClass().getName());
    }
}
