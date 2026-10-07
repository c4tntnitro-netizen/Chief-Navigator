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
                + "fabricator_unit_Type450.variant");
        for (String field : new String[]{"hullMods", "weaponGroups",
                "fluxCapacitors", "fluxVents"}) {
            check(variant.get(field).toString().equals(stock.get(field).toString()),
                    "The visual fix must retain the exact stock Fabricator " + field);
        }
        check(skin.getJSONArray("removeBuiltInMods").length() == 0,
                "Do not remove inherited hullmods to hide the overlay");
        System.out.println("PASS: untagged overlay reproduced, native Threat overlay suppression, "
                + "and unchanged stock Fabricator weapons, hullmods, and flux allocation.");
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
