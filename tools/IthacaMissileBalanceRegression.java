package chiefnavigator.quest;

import chiefnavigator.weapons.IthacaMissileResearchEffect;
import chiefnavigator.weapons.IthacaResearchUpgrades;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;

/** Shared restored Hammer balance, four Cloudswarm mounts, and live Atropos checks. */
public final class IthacaMissileBalanceRegression {
    private static final String ITHACA_HAMMER = "chief_navigator_drifting_wall_annihilator_launcher";
    private static final String WALL_HAMMER = "chief_navigator_drifting_wall_annihilator_launcher";
    private static final String ATROPOS = "chief_navigator_ithaca_atropos_battery";
    private static final double[] LATERAL = {75, -75, 58, -58, 42, -42, 25, -25, 8, -8};
    private static final double[] FAN = {5, -5, 3.866667, -3.866667, 2.8, -2.8,
            1.666667, -1.666667, 0.533333, -0.533333};

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void close(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 0.0001, message + ": " + actual + " != " + expected);
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }

    private static JSONObject json(String path) throws Exception {
        return new JSONObject(read(path));
    }

    private static List<List<String>> csv(String text) {
        List<List<String>> result = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    cell.append(c);
                    i++;
                } else quoted = !quoted;
            } else if (!quoted && (c == ',' || c == '\r' || c == '\n')) {
                row.add(cell.toString());
                cell.setLength(0);
                if (c != ',') {
                    if (!row.get(0).isBlank()) result.add(List.copyOf(row));
                    row.clear();
                    if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                }
            } else cell.append(c);
        }
        check(!quoted && row.isEmpty(), "Weapon CSV must end with balanced fields");
        return result;
    }

    private static Map<String, Map<String, String>> weaponRows() throws Exception {
        List<List<String>> rows = csv(read("data/weapons/weapon_data.csv"));
        List<String> header = rows.get(0);
        Map<String, Map<String, String>> result = new HashMap<>();
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            check(row.size() == header.size(), "Weapon CSV column count: " + row.get(0));
            Map<String, String> fields = new HashMap<>();
            for (int j = 0; j < header.size(); j++) fields.put(header.get(j), row.get(j));
            check(result.put(fields.get("id"), fields) == null, "Duplicate weapon ID " + fields.get("id"));
        }
        return result;
    }

    private static void number(Map<String, String> row, String field, double expected) {
        check(row != null && row.containsKey(field), "Missing weapon field " + field);
        close(Double.parseDouble(row.get(field)), expected, row.get("id") + " " + field);
    }

    private static void missileRow(Map<String, String> row, int count, double recharge, double refire,
                                   double damage) {
        number(row, "ammo", count);
        number(row, "reload size", count);
        number(row, "burst size", count);
        number(row, "ammo/sec", recharge);
        number(row, "chargedown", refire);
        number(row, "range", 10000);
        number(row, "flight time", 13);
        number(row, "damage/shot", damage);
        number(row, "burst delay", .12);
        number(row, "chargeup", 0);
        check(row.get("type").equals("HIGH_EXPLOSIVE"), "Missile damage type must remain HE");
    }

    private static void launchGeometry(JSONObject spec) throws Exception {
        int count = 10;
        for (String kind : List.of("turret", "hardpoint")) {
            JSONArray origins = spec.getJSONArray(kind + "Offsets");
            JSONArray angles = spec.getJSONArray(kind + "AngleOffsets");
            check(origins.length() == count * 2 && angles.length() == count,
                    "Each firing mode must have one origin and angle per shot: " + spec.getString("id"));
            for (int i = 0; i < count; i++) {
                close(origins.getDouble(2 * i), 84, "Missiles must emerge from the front edge");
                close(origins.getDouble(2 * i + 1), LATERAL[i], "Ten authored front-edge ports");
                close(angles.getDouble(i), FAN[i], "Ten authored spread directions");
            }
        }
        check(spec.getString("barrelMode").equals("ALTERNATING"), "Preserve alternating launch ports");
        check(spec.getBoolean("renderBelowAllWeapons"), "Missile housing must remain below naval guns");
    }

    private static Map<String, String> equipment(JSONObject variant) throws Exception {
        Map<String, String> result = new LinkedHashMap<>();
        JSONArray groups = variant.getJSONArray("weaponGroups");
        for (int i = 0; i < groups.length(); i++) {
            JSONObject weapons = groups.getJSONObject(i).getJSONObject("weapons");
            for (var keys = weapons.keys(); keys.hasNext();) {
                String slot = (String) keys.next();
                check(result.put(slot, weapons.getString(slot)) == null, "Duplicate variant weapon slot " + slot);
            }
        }
        return result;
    }

    private static void authoredData() throws Exception {
        Map<String, Map<String, String>> rows = weaponRows();
        missileRow(rows.get(ITHACA_HAMMER), 10, 1, 20, 1500);
        missileRow(rows.get(ATROPOS), 10, 1, 20, 1000);
        missileRow(rows.get(WALL_HAMMER), 10, 1, 20, 1500);
        JSONObject hammer = json("data/weapons/" + ITHACA_HAMMER + ".wpn");
        JSONObject hostile = json("data/weapons/" + WALL_HAMMER + ".wpn");
        JSONObject atropos = json("data/weapons/" + ATROPOS + ".wpn");
        check(ITHACA_HAMMER.equals(WALL_HAMMER)
                        && hammer.getString("id").equals(ITHACA_HAMMER)
                        && !rows.containsKey("chief_navigator_ithaca_hammer_battery"),
                "Ithaca and Wall must share the same tuned battery, not split balance definitions");
        for (String key : List.of("turretSprite", "hardpointSprite", "everyFrameEffect", "onFireEffect")) {
            check(hammer.getString(key).equals(hostile.getString(key)),
                    "Ithaca battery must reuse the existing housing and research callbacks: " + key);
        }
        check(hammer.getString("projectileSpecId").equals("hammer_torp")
                        && hammer.getString("fireSoundTwo").equals("hammer_fire")
                        && atropos.getString("projectileSpecId").equals("atropos_torp")
                        && !atropos.has("onFireEffect"),
                "Retain stock Hammer/Atropos ordnance and a non-recursive replacement helper");
        launchGeometry(hammer);
        launchGeometry(atropos);
        launchGeometry(hostile);
        close(FAN[0] - FAN[1], 10, "Ithaca salvo must span ten degrees");

        JSONObject skin = json("data/hulls/skins/chief_navigator_ithaca_intact_base.skin");
        JSONObject builtins = skin.getJSONObject("builtInWeapons");
        Map<String, String> equipped = equipment(json("data/variants/chief_navigator_ithaca_intact_base_Standard.variant"));
        Map<String, String> enemy = equipment(json("data/variants/chief_navigator_drifting_wall_base_Standard.variant"));
        JSONArray slots = json("data/hulls/chief_navigator_ithaca_foundation.ship")
                .getJSONArray("weaponSlots");
        Map<String, JSONObject> cloudSlots = new HashMap<>();
        for (int i = 0; i < slots.length(); i++) {
            JSONObject slot = slots.getJSONObject(i);
            if (slot.getString("id").startsWith("LOCUST_PD_")) {
                cloudSlots.put(slot.getString("id"), slot);
            }
        }
        check(cloudSlots.keySet().equals(java.util.Set.of(
                        "LOCUST_PD_01", "LOCUST_PD_03", "LOCUST_PD_04", "LOCUST_PD_06")),
                "Keep the correct left pair and its mirrored right pair; remove slots 02/05");
        int cloudCount = 0;
        for (int i = 1; i <= 6; i++) {
            String slot = String.format("LOCUST_PD_%02d", i);
            if (i == 2 || i == 5) {
                check(!builtins.has(slot) && !equipped.containsKey(slot),
                        "Deleted Cloudswarm must not survive in skin or variant: " + slot);
            } else {
                check(builtins.getString(slot).equals("chief_navigator_ithaca_locust_pd_array")
                                && equipped.get(slot).equals("chief_navigator_ithaca_locust_pd_array")
                                && cloudSlots.get(slot).getString("mount").equals("HARDPOINT"),
                        "Keep each other fixed Cloudswarm equipped: " + slot);
                cloudCount++;
            }
        }
        check(cloudCount == 4, "Every intact module keeps exactly four Cloudswarm arrays");
        for (String[] pair : new String[][]{
                {"LOCUST_PD_01", "LOCUST_PD_06"},
                {"LOCUST_PD_03", "LOCUST_PD_04"}}) {
            JSONArray left = cloudSlots.get(pair[0]).getJSONArray("locations");
            JSONArray right = cloudSlots.get(pair[1]).getJSONArray("locations");
            close(right.getDouble(0), left.getDouble(0), "Mirrored Cloudswarm longitudinal position");
            close(right.getDouble(1), -left.getDouble(1), "Mirrored Cloudswarm lateral position");
        }
        number(rows.get("chief_navigator_ithaca_locust_pd_array"), "ammo", 41);
        number(rows.get("chief_navigator_ithaca_locust_pd_array"), "burst size", 81);
        number(rows.get("chief_navigator_ithaca_locust_pd_array"), "chargedown", 16);
        number(rows.get("chief_navigator_ithaca_locust_pd_array"), "range", 1200);
        number(rows.get("chief_navigator_ithaca_locust_pd_array"), "flight time", 3.63);
        for (int i = 1; i <= 4; i++) {
            String slot = String.format("LARGE_BALLISTIC_%02d", i);
            check(builtins.getString(slot).equals(ITHACA_HAMMER) && equipped.get(slot).equals(ITHACA_HAMMER),
                    "All four intact Ithaca batteries must use the shared Wall balance: " + slot);
            String expected = i == 1 || i == 3 ? "chief_navigator_drifting_wall_annihilator_destroyed" : WALL_HAMMER;
            check(enemy.get(slot).equals(expected), "Hostile Wall launcher/destroyed-fixture layout must stay intact");
        }
        String[] medium = {"pulselaser", "pulselaser", "heavyneedler", "heavymauler", "pulselaser",
                "heavyneedler", "heavyneedler", "pulselaser", "heavymauler", "heavyneedler"};
        for (int i = 0; i < medium.length; i++) {
            String slot = String.format("MEDIUM_UNIVERSAL_%02d", i + 1);
            check(builtins.getString(slot).equals(medium[i]) && equipped.get(slot).equals(medium[i])
                            && enemy.get(slot).equals(medium[i]),
                    "Ordinary medium-gun equipment must not change: " + slot);
        }
        for (String directory : List.of("data/hulls", "data/variants")) {
            try (var files = Files.walk(Path.of(directory))) {
                for (Path file : files.filter(Files::isRegularFile).toList()) {
                    String name = file.getFileName().toString();
                    if (!name.endsWith(".ship") && !name.endsWith(".skin") && !name.endsWith(".variant")) continue;
                    check(!Files.readString(file, StandardCharsets.UTF_8)
                                    .contains("chief_navigator_ithaca_hammer_battery"),
                            "Never split Ithaca and Wall launcher balance: " + file);
                }
            }
        }
        number(rows.get("chief_navigator_metalstorm"), "burst size", 15);
        number(rows.get("chief_navigator_metalstorm"), "chargedown", 10);
        number(rows.get("chief_navigator_metalstorm"), "range", 250);
        number(rows.get("chief_navigator_metalstorm"), "flight time", 5.02);
        String metal = read("src/chiefnavigator/weapons/MetalStormVolleyEffect.java");
        check(metal.contains("private static final int COLUMNS = 4;")
                        && metal.contains("private static final int ROWS = 5;")
                        && metal.contains("private static final int CELL_COUNT = COLUMNS * ROWS;"),
                "Reducing Metal Storm's salvo must retain its twenty physical cap cells");
        number(rows.get("chief_navigator_drifting_wall_colossal_inert"), "range", 7500);
        number(rows.get("chief_navigator_naval_gun"), "range", 675);
        number(rows.get("chief_navigator_drifting_wall_colossal_inert"), "damage/second", 2000);
        number(rows.get("chief_navigator_drifting_wall_colossal_inert"), "chargeup", 3);
        number(rows.get("chief_navigator_damocles"), "damage/shot", 400);
        number(rows.get("chief_navigator_damocles"), "burst size", 8);
        number(rows.get("chief_navigator_damocles"), "chargedown", 8);
        untouched("data/weapons/chief_navigator_damocles.wpn",
                "DDC8A00BDB91999D7CE83D6FEFB75E3E8A84B8079A94266A8446243816DF732B");
        untouched("data/weapons/proj/chief_navigator_damocles_bomb.proj",
                "AC568D49E450A9D4C4113E062681C45E5ADD982585D49D8064825E4D5AAD8663");
        untouched("src/chiefnavigator/weapons/DamoclesBombEffect.java",
                "D70A3995A36B7B11734572BB89AC603E310D8EE3575C58A4D0482B80A10EDDB9");
    }

    private static void untouched(String path, String expected) throws Exception {
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(Path.of(path))));
        check(hash.equalsIgnoreCase(expected), "Unrelated Damocles bombs must remain unchanged: " + path);
    }

    interface Call { Object invoke(String name, Object[] args); }

    private static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> {
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == args[0];
                    Object result = call.invoke(method.getName(), args);
                    if (result != null) return result;
                    Class<?> returns = method.getReturnType();
                    if (returns == boolean.class) return false;
                    if (returns == int.class) return 0;
                    if (returns == float.class) return 0f;
                    if (returns == long.class) return 0L;
                    return null;
                }));
    }

    private static void inheritedProjectileFan() {
        String[] hulls = {"chief_navigator_ithaca_intact_base", "chief_navigator_ithaca_intact_base",
                "chief_navigator_drifting_wall_base", "onslaught"};
        int[] masks = {0, IthacaResearchUpgrades.MUNITIONS_COMPILER,
                IthacaResearchUpgrades.ALL_UPGRADES, IthacaResearchUpgrades.ALL_UPGRADES};
        for (int scenario = 0; scenario < hulls.length; scenario++) {
            String hull = hulls[scenario];
            Map<String, Object> shipData = new HashMap<>();
            shipData.put("chief_navigator_ithaca_research_component_mask", masks[scenario]);
            ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class, (name, args) ->
                    name.equals("getHullId") ? hull : null);
            Vector2f velocity = new Vector2f(14, -9);
            ShipAPI source = mock(ShipAPI.class, (name, args) -> {
                if (name.equals("getHullSpec")) return hullSpec;
                if (name.equals("getCustomData")) return shipData;
                if (name.equals("getVelocity")) return velocity;
                return null;
            });
            WeaponAPI weapon = mock(WeaponAPI.class, (name, args) -> name.equals("getShip") ? source : null);
            int[] spawns = {0}, removals = {0};
            for (int tube = 0; tube < FAN.length; tube++) {
                Vector2f origin = new Vector2f(84, (float) LATERAL[tube]);
                float facing = 124.3f + (float) FAN[tube];
                Map<String, Object> projectileData = new HashMap<>();
                DamagingProjectileAPI projectile = mock(DamagingProjectileAPI.class, (name, args) -> {
                    if (name.equals("getProjectileSpecId")) return "hammer_torp";
                    if (name.equals("getLocation")) return origin;
                    if (name.equals("getFacing")) return facing;
                    if (name.equals("getCustomData")) return projectileData;
                    if (name.equals("setCustomData")) projectileData.put((String) args[0], args[1]);
                    return null;
                });
                CombatEngineAPI engine = mock(CombatEngineAPI.class, (name, args) -> {
                    if (name.equals("spawnProjectile")) {
                        check(args.length == 6 && args[0] == source && args[1] == weapon && ATROPOS.equals(args[2]),
                                "Replacement must retain source and use the stock Atropos helper");
                        check(args[3] == origin && args[5] == velocity, "Replacement must preserve firing origin and velocity");
                        close((Float) args[4], facing, "Replacement must inherit the narrowed Hammer facing without recentering");
                        spawns[0]++;
                    }
                    if (name.equals("removeEntity")) {
                        check(args[0] == projectile, "Only the replaced Hammer may be removed");
                        removals[0]++;
                    }
                    return null;
                });
                IthacaMissileResearchEffect effect = new IthacaMissileResearchEffect();
                effect.onFire(projectile, weapon, engine);
                effect.onFire(projectile, weapon, engine);
            }
            int expected = scenario == 1 ? 10 : 0;
            check(spawns[0] == expected && removals[0] == expected,
                    "Each fan projectile converts once only on upgraded Ithaca, never hostile/ordinary hulls");
        }
    }

    public static void main(String[] args) throws Exception {
        authoredData();
        inheritedProjectileFan();
        System.out.println("Ithaca/Drifting Wall missile balance passed: shared ten-shot/20s/10000-range Hammer, narrowed 10-degree fan, four Cloudswarm mounts, unchanged other nerfs/damage/bombs and live Atropos trajectories.");
    }
}
