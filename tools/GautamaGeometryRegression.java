import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/** User-authored Gautama geometry, preserved arsenal, and shared skin inheritance. */
public final class GautamaGeometryRegression {
    public static void main(String[] args) throws Exception {
        JSONObject hull = read("data/hulls/chief_navigator_scylla_base.ship");
        JSONObject design = read("graphics/ships/odyssey_bosses/gautama.ship");
        for (String field : new String[]{"bounds", "center",
                "width", "height", "spriteName"}) {
            check(hull.get(field).toString().equals(design.get(field).toString()),
                    "Copy the user's exact geometry/frame: " + field);
        }
        JSONArray engines = hull.getJSONArray("engineSlots");
        JSONArray authoredEngines = design.getJSONArray("engineSlots");
        check(engines.length() == 4 && engines.length() == authoredEngines.length(),
                "Keep all four user-authored engines");
        for (int index = 0; index < engines.length(); index++) {
            JSONObject engine = engines.getJSONObject(index);
            JSONObject authoredEngine = authoredEngines.getJSONObject(index);
            for (String field : new String[]{"location", "length", "width", "angle", "contrailSize"}) {
                check(engine.get(field).toString().equals(authoredEngine.get(field).toString()),
                        "Preserve authored engine geometry: " + index + "/" + field);
            }
            check(engine.getString("style").equals("THREAT"),
                    "Use white vanilla THREAT flares: " + index);
        }
        check(hull.getString("hullId").equals("chief_navigator_scylla_base")
                        && hull.getString("hullSize").equals("CAPITAL_SHIP")
                        && hull.getString("style").equals("THREAT")
                        && hull.getInt("viewOffset") == 300
                        && hull.getInt("shieldRadius") == 375,
                "Do not copy the editor template's new_hull/Frigate/default boss properties");
        JSONArray shieldCenter = hull.getJSONArray("shieldCenter");
        check(shieldCenter.getDouble(0) == 10.5 && shieldCenter.getDouble(1) == 0,
                "Changing the hull pivot must preserve the shield's painted attachment point");
        JSONArray bounds = hull.getJSONArray("bounds");
        double maximumRadius = 0;
        check(bounds.length() == 80, "Keep all forty authored outline vertices");
        for (int index = 0; index < bounds.length(); index += 2) {
            maximumRadius = Math.max(maximumRadius, Math.hypot(
                    bounds.getDouble(index), bounds.getDouble(index + 1)));
        }
        check(hull.getDouble("collisionRadius") == Math.ceil(maximumRadius),
                "Use a valid broad-phase radius around the outline, not the editor's zero radius");
        Map<String, JSONObject> slots = slots(hull);
        Map<String, JSONObject> authoredSlots = slots(design);
        String[][] mapping = {{"WS 001", "WS0001"}, {"WS 032", "WS0002"},
                {"WS 010", "WS0003"}, {"WS 008", "WS0004"},
                {"WS 012", "WS0005"}, {"WS 034", "WS0006"},
                {"WS 020", "WS0007"}, {"WS 033", "WS0008"},
                {"WS 035", "WS0009"}};
        for (String[] pair : mapping) {
            JSONObject live = slots.get(pair[0]);
            JSONObject authored = authoredSlots.get(pair[1]);
            check(live != null && authored != null
                            && live.getJSONArray("locations").toString().equals(
                                    authored.getJSONArray("locations").toString())
                            && live.getDouble("angle") == authored.getDouble("angle")
                            && live.getDouble("arc") > 0,
                    "Authored main gun position/angle and functional arc: " + pair[0]);
        }
        check(slots.get("LB 1").toString().equals(authoredSlots.get("LB 1").toString()),
                "Keep the exact authored launch-bay marker too");
        String[] ids = {"chief_navigator_scylla", "chief_navigator_scylla_final",
                "chief_navigator_scylla_reincarnating"};
        String[] variants = {"chief_navigator_scylla_Fabricator", "chief_navigator_scylla_Final",
                "chief_navigator_scylla_reincarnating_Fabricator"};
        for (int index = 0; index < ids.length; index++) {
            JSONObject skin = read("data/hulls/skins/" + ids[index] + ".skin");
            check(skin.getString("baseHullId").equals(hull.getString("hullId"))
                            && skin.getJSONArray("removeWeaponSlots").length() == 0
                            && skin.getJSONArray("removeEngineSlots").length() == 0,
                    "Every Gautama skin must inherit the shared authored geometry: " + ids[index]);
            for (String field : new String[]{"bounds", "weaponSlots", "engineSlots", "center"}) {
                check(!skin.has(field), "No skin geometry override: " + ids[index] + "/" + field);
            }
            JSONObject variant = read("data/variants/" + variants[index] + ".variant");
            JSONArray groups = variant.getJSONArray("weaponGroups");
            Set<String> fitted = new HashSet<>();
            for (int group = 0; group < groups.length(); group++) {
                JSONObject weapons = groups.getJSONObject(group).getJSONObject("weapons");
                for (Iterator<?> keys = weapons.keys(); keys.hasNext();) {
                    String id = (String) keys.next();
                    JSONObject slot = slots.get(id);
                    String weapon = Files.readString(Path.of("../../starsector-core/data/weapons",
                            weapons.getString(id) + ".wpn"));
                    check(fitted.add(id) && slot != null && slot.getDouble("arc") > 0,
                            "Every retained weapon needs one real, usable slot: " + id);
                    String weaponType = specField(weapon, "type");
                    String slotType = slot.getString("type");
                    check(slotType.equals(weaponType) || slotType.equals("UNIVERSAL")
                                    || slotType.equals("HYBRID")
                                    && (weaponType.equals("BALLISTIC") || weaponType.equals("ENERGY")),
                            "Weapon type must fit its retained mount: " + id);
                    check(size(specField(weapon, "size")) <= size(slot.getString("size")),
                            "Weapon must not be oversized for its mount: " + id);
                }
            }
            check(fitted.size() == 13, "Keep all thirteen guns: " + variants[index]);
        }
        System.out.println("PASS: exact forty-point outline/four engine geometries with white THREAT flares, nine authored gun markers, "
                + "launch bay, correct pivot/radius, and all thirteen compatible guns on all three Gautamas.");
    }

    private static Map<String, JSONObject> slots(JSONObject hull) throws Exception {
        Map<String, JSONObject> slots = new HashMap<>();
        JSONArray data = hull.getJSONArray("weaponSlots");
        for (int index = 0; index < data.length(); index++) {
            JSONObject slot = data.getJSONObject(index);
            check(slots.put(slot.getString("id"), slot) == null, "Duplicate slot ID");
        }
        return slots;
    }

    private static String specField(String source, String field) {
        Matcher match = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([A-Z_]+)\"").matcher(source);
        check(match.find(), "Missing native weapon field " + field);
        return match.group(1);
    }

    private static int size(String size) {
        return switch (size) { case "SMALL" -> 0; case "MEDIUM" -> 1;
            case "LARGE" -> 2; default -> throw new AssertionError(size); };
    }

    private static JSONObject read(String path) throws Exception {
        return new JSONObject(Files.readString(Path.of(path)));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
