import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.loading.specs.BaseWeaponSpec;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * External harness for the installed Nexerelin mining-cache picker. Run from the
 * Chief Navigator directory with -Xverify:none for the obfuscated engine parser.
 * No replacement picker or production runtime classes are used by this test.
 */
public final class MiningWeaponIsolationRegression {
    private static final String COLOSSAL = "chief_navigator_drifting_wall_colossal_inert";
    private static final String VANILLA = "lightac";
    private static final List<String> STATION_IDS = Arrays.asList(
            "chief_navigator_naval_gun", "chief_navigator_damocles", "chief_navigator_metalstorm",
            "chief_navigator_destroyed_naval_gun", "chief_navigator_destroyed_damocles",
            "chief_navigator_destroyed_metalstorm", COLOSSAL,
            "chief_navigator_drifting_wall_annihilator_launcher",
            "chief_navigator_drifting_wall_colossal_destroyed",
            "chief_navigator_drifting_wall_annihilator_destroyed",
            "chief_navigator_ithaca_atropos_battery", "chief_navigator_ithaca_locust_pd_array");
    private static List<String> weaponIds = Collections.emptyList();
    private static final Map<String, BaseWeaponSpec> specs = new LinkedHashMap<>();
    private static int nativeCalls;

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static String read(Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String withoutHashComments(String source) {
        StringBuilder result = new StringBuilder();
        boolean quoted = false;
        boolean escaped = false;
        boolean comment = false;
        for (char character : source.toCharArray()) {
            if (comment) {
                if (character != '\n' && character != '\r') continue;
                comment = false;
            }
            if (!quoted && character == '#') {
                comment = true;
                continue;
            }
            result.append(character);
            if (quoted && character == '\\' && !escaped) {
                escaped = true;
                continue;
            }
            if (character == '"' && !escaped) quoted = !quoted;
            escaped = false;
        }
        return result.toString();
    }

    private static Map<String, JSONObject> nativeRows(Path csv) throws Exception {
        // Reuse the installed game's CSV parser, including its quoting rules.
        JSONArray rows = (JSONArray) Class.forName("com.fs.starfarer.loading.G")
                .getMethod("o00000", String.class).invoke(null, read(csv));
        Map<String, JSONObject> result = new LinkedHashMap<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            String id = row.optString("id");
            if (!id.isEmpty()) check(result.put(id, row) == null, "Duplicate weapon ID: " + id);
        }
        return result;
    }

    private static BaseWeaponSpec nativeSpec(Path definitions, String id, JSONObject row) throws Exception {
        check(row != null, "Missing CSV row: " + id);
        JSONObject definition = new JSONObject(withoutHashComments(read(definitions.resolve(id + ".wpn"))));
        BaseWeaponSpec spec = new BaseWeaponSpec(id);
        spec.setSize(WeaponSize.valueOf(definition.getString("size")));
        spec.setType(WeaponType.valueOf(definition.getString("type")));
        spec.setMountType(WeaponType.valueOf(definition.optString("mountTypeOverride",
                definition.getString("type"))));
        spec.setBaseValue((float) row.optDouble("base value", 0));
        spec.setWeaponName(row.getString("name"));
        for (String value : row.optString("hints").split(",")) {
            if (!value.trim().isEmpty()) spec.getAIHints().add(AIHints.valueOf(value.trim()));
        }
        for (String value : row.optString("tags").split(",")) {
            if (!value.trim().isEmpty()) spec.addTag(value.trim());
        }
        return spec;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> api, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[] {api}, handler);
    }

    private static Object objectMethod(Object proxy, Method method, Object[] arguments) {
        switch (method.getName()) {
            case "toString": return "MiningWeaponIsolationRegression API fixture";
            case "hashCode": return System.identityHashCode(proxy);
            case "equals": return proxy == arguments[0];
            default: throw new AssertionError("Unexpected Object method: " + method);
        }
    }

    private static SettingsAPI settings() {
        return proxy(SettingsAPI.class, (proxy, method, arguments) -> {
            if (method.getDeclaringClass() == Object.class) return objectMethod(proxy, method, arguments);
            switch (method.getName()) {
                case "getWeaponSpec":
                    BaseWeaponSpec spec = specs.get((String) arguments[0]);
                    check(spec != null, "Unknown weapon requested by native picker: " + arguments[0]);
                    return spec;
                case "loadJSON": return new JSONObject();
                case "getMergedSpreadsheetDataForMod": return new JSONArray();
                case "getMergedJSONForMod": return new JSONObject();
                case "getString": return "fixture";
                case "getBoolean": case "isDevMode": return false;
                case "getFloat": return 0f;
                default: throw new AssertionError("Unexpected SettingsAPI call: " + method);
            }
        });
    }

    private static SectorAPI sector() {
        return proxy(SectorAPI.class, (proxy, method, arguments) -> {
            if (method.getDeclaringClass() == Object.class) return objectMethod(proxy, method, arguments);
            if (method.getName().equals("getAllWeaponIds")) return new ArrayList<>(weaponIds);
            throw new AssertionError("Unexpected SectorAPI call: " + method);
        });
    }

    private static WeaponSpecAPI pick(Method nativePicker, String... ids) throws Exception {
        weaponIds = Arrays.asList(ids);
        nativeCalls++;
        return (WeaponSpecAPI) nativePicker.invoke(null);
    }

    private static void verifyPicker(Method nativePicker, List<String> customIds) throws Exception {
        int protectedCount = 0;
        for (String id : customIds) {
            // A singleton proves exclusion directly, without a lucky random sample.
            check(pick(nativePicker, id) == null, "Mining accepts protected custom weapon: " + id);
            check(pick(nativePicker, id, VANILLA).getWeaponId().equals(VANILLA),
                    "Protected weapon masks the ordinary vanilla candidate: " + id);
            protectedCount++;
        }
        check(pick(nativePicker, VANILLA).getWeaponId().equals(VANILLA),
                "Native mining must still produce an ordinary vanilla gun");

        int oldLive = 0;
        int oldDestroyed = 0;
        for (String id : STATION_IDS) {
            BaseWeaponSpec spec = specs.get(id);
            check(spec.getTags().contains("restricted"), "Station row lacks restricted: " + id);
            Set<String> before = new LinkedHashSet<>(spec.getTags());
            try {
                // Reconstruct only the prior tag field: retain hints, base value,
                // size, no_drop and no_drop_salvage exactly as authored.
                spec.getTags().remove("restricted");
                check(spec.hasTag("no_drop") && spec.hasTag("no_drop_salvage"),
                        "Negative control must retain both historical drop exclusions: " + id);
                WeaponSpecAPI originalPick = pick(nativePicker, id);
                if (spec.getAIHints().contains(AIHints.SYSTEM)) {
                    check(originalPick == null, "Original destroyed socket must retain SYSTEM: " + id);
                    oldDestroyed++;
                } else {
                    check(originalPick != null && originalPick.getWeaponId().equals(id),
                            "Negative control fails to reproduce original mining eligibility: " + id);
                    oldLive++;
                }
            } finally {
                spec.getTags().clear();
                spec.getTags().addAll(before);
            }
            check(pick(nativePicker, id) == null, "Restored restriction failed: " + id);
        }
        check(oldLive == 7 && oldDestroyed == 5, "Expected seven live and five destroyed station weapons");
        List<String> combined = new ArrayList<>(customIds);
        combined.add(VANILLA);
        check(pick(nativePicker, combined.toArray(new String[0])).getWeaponId().equals(VANILLA),
                "Combined native mining pool must contain only the vanilla control");
        System.out.println("PASS " + nativePicker.getDeclaringClass().getName() + ".getRandomWeapon: "
                + protectedCount + " custom rows excluded; 7 original live station weapons reproduced "
                + "(including Colossal Naval Turret); 5 destroyed SYSTEM sockets excluded; vanilla gun retained");
    }

    public static void main(String[] arguments) throws Exception {
        Path modRoot = Path.of("").toAbsolutePath().normalize();
        Path gameRoot = modRoot;
        while (gameRoot != null && !Files.isDirectory(gameRoot.resolve("starsector-core"))) {
            gameRoot = gameRoot.getParent();
        }
        check(gameRoot != null, "Cannot locate the installed Starsector core above the checkout");
        Path core = gameRoot.resolve("starsector-core");
        Map<String, JSONObject> rows = nativeRows(modRoot.resolve("data/weapons/weapon_data.csv"));
        check(rows.size() == 27, "Expected all 27 Chief Navigator weapon rows, found " + rows.size());
        for (Map.Entry<String, JSONObject> row : rows.entrySet()) {
            check(row.getKey().startsWith("chief_navigator_"), "Unexpected custom weapon ID: " + row.getKey());
            specs.put(row.getKey(), nativeSpec(modRoot.resolve("data/weapons"), row.getKey(), row.getValue()));
        }
        check(rows.keySet().containsAll(STATION_IDS), "Missing internal station weapon row");
        specs.put(VANILLA, nativeSpec(core.resolve("data/weapons"), VANILLA,
                nativeRows(core.resolve("data/weapons/weapon_data.csv")).get(VANILLA)));

        SettingsAPI oldSettings = Global.getSettings();
        SectorAPI oldSector = Global.getSector();
        Field prismRestrictions = null;
        Object oldRestrictions = null;
        Field magicSettings = null;
        Object oldMagicSettings = null;
        try {
            Global.setSettings(settings());
            Global.setSector(sector());
            magicSettings = Class.forName("org.magiclib.util.MagicSettings").getField("modSettings");
            oldMagicSettings = magicSettings.get(null);
            JSONObject nex = new JSONObject();
            nex.put("mining_ship_strengths", new JSONObject());
            nex.put("mining_weapon_strengths", new JSONObject());
            nex.put("mining_hidden_ships_and_weapons", new JSONArray());
            magicSettings.set(null, new JSONObject().put("nexerelin", nex));

            Class<?> prism = Class.forName("exerelin.campaign.submarkets.PrismMarket");
            prism.getMethod("checkLoadFailure").invoke(null);
            prismRestrictions = prism.getDeclaredField("restrictedWeapons");
            prismRestrictions.setAccessible(true);
            oldRestrictions = prismRestrictions.get(null);
            prismRestrictions.set(null, new LinkedHashSet<String>());
            check(((Set<?>) prism.getMethod("getRestrictedWeapons").invoke(null)).isEmpty(),
                    "Prism blacklist must not conceal a missing native restriction");

            List<String> customIds = new ArrayList<>(rows.keySet());
            verifyPicker(Class.forName("exerelin.campaign.MiningHelperLegacy")
                    .getMethod("getRandomWeapon"), customIds);
            try {
                Class<?> v2 = Class.forName("exerelin.campaign.MiningHelperV2");
                verifyPicker(v2.getMethod("getRandomWeapon"), customIds);
            } catch (ClassNotFoundException absent) {
                System.out.println("Installed Nexerelin has no MiningHelperV2; mining uses MiningHelperLegacy");
            }
            System.out.println("PASS native CSV parser, native weapon specs and " + nativeCalls
                    + " installed Nex picker invocations; Prism blacklist empty");
        } finally {
            if (prismRestrictions != null) prismRestrictions.set(null, oldRestrictions);
            if (magicSettings != null) magicSettings.set(null, oldMagicSettings);
            Global.setSettings(oldSettings);
            Global.setSector(oldSector);
            weaponIds = Collections.emptyList();
            specs.clear();
        }
    }
}
