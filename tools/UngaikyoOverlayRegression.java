import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.threat.FragmentSwarmHullmod;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/** Exercises vanilla's actual overlay hook with Ungaikyo's authored skin tags. */
public final class UngaikyoOverlayRegression {
    public static void main(String[] args) throws Exception {
        JSONObject skin = read("data/hulls/skins/chief_navigator_ungaikyo.skin");
        check(skin.getString("baseHullId").equals("overseer_unit")
                        && skin.getString("skinHullId").equals("chief_navigator_ungaikyo")
                        && skin.getString("systemId").equals("chief_navigator_ungaikyo_construction_swarm")
                        && skin.getString("spriteName").equals("graphics/ships/starving_threat/"
                                + "chief_navigator_ungaikyo_overseer_obsidian_v1.png"),
                "The retained Ungaikyo identity must use the obsidian Overseer and unique reflection/extraction system");
        for (String field : new String[]{"bounds", "weaponSlots", "engineSlots", "center",
                "collisionRadius", "width", "height"}) {
            check(!skin.has(field), "Inherit unmodified vanilla Overseer geometry: " + field);
        }
        Set<String> tags = new HashSet<>();
        JSONArray authoredTags = skin.getJSONArray("tags");
        for (int index = 0; index < authoredTags.length(); index++) {
            tags.add(authoredTags.getString(index));
        }
        check(!FragmentSwarmHullmod.SHOW_OVERLAY_ON_THREAT_SHIPS,
                "Vanilla must suppress the retrofit coating on Threat hulls by default");
        Global.setSettings(WallResearchRegression.mock(SettingsAPI.class,
                (name, values) -> name.equals("getSpriteName") ? "fragment_overlay" : null));

        Set<String> untagged = new HashSet<>(tags);
        untagged.remove(Tags.THREAT);
        check(overlayWrites(untagged) == 3,
                "The old untagged skin must reproduce the blotchy retrofit overlay");
        check(tags.contains(Tags.THREAT) && overlayWrites(tags) == 0,
                "Ungaikyo must skip every overlay setter through vanilla Threat classification");

        JSONObject variant = read("data/variants/chief_navigator_ungaikyo_Fabricator.variant");
        JSONObject stock = read("../../starsector-core/data/variants/threat/"
                + "overseer_unit_Type250.variant");
        for (String field : new String[]{"hullMods", "weaponGroups",
                "fluxCapacitors", "fluxVents", "goalVariant", "permaMods", "sMods"}) {
            check(variant.get(field).toString().equals(stock.get(field).toString()),
                    "Ungaikyo must retain the exact stock Overseer Type250 " + field);
        }
        check(variant.getString("hullId").equals("chief_navigator_ungaikyo")
                        && variant.getString("variantId").equals("chief_navigator_ungaikyo_Fabricator"),
                "Keep historical hull and variant IDs for saved encounters");
        for (String field : new String[]{"removeBuiltInMods", "removeBuiltInWeapons",
                "removeWeaponSlots", "removeEngineSlots"}) {
            check(skin.getJSONArray(field).length() == 0, "Preserve inherited Overseer systems: " + field);
        }
        check(skin.getJSONObject("builtInWeapons").length() == 0 && tags.contains("threat_overseer"),
                "Retain native Overseer role classification without extra weapon overrides");
        System.out.println("PASS: untagged overlay reproduced, native Threat overlay suppression, "
                + "native Overseer geometry, stock Type250 weapons/hullmods/flux, and retained encounter IDs/system.");
    }

    private static int overlayWrites(Set<String> tags) {
        int[] writes = {0};
        ShipHullSpecAPI hull = WallResearchRegression.mock(ShipHullSpecAPI.class,
                (name, values) -> name.equals("hasTag") ? tags.contains(values[0]) : null);
        ShipAPI ship = WallResearchRegression.mock(ShipAPI.class, (name, values) -> {
            if (name.equals("getHullSpec")) return hull;
            if (name.startsWith("setExtraOverlay")) writes[0]++;
            return null;
        });
        new FragmentSwarmHullmod().applyEffectsAfterShipCreation(ship, "fragment_swarm");
        return writes[0];
    }

    private static JSONObject read(String path) throws Exception {
        return new JSONObject(Files.readString(Path.of(path)));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
