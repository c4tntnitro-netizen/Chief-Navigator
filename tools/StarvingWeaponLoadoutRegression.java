import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.json.JSONArray;
import org.json.JSONObject;

/** Authored Starving/Gautama fits must resolve directly to native THREAT weapons. */
public final class StarvingWeaponLoadoutRegression {
    private static final String RETIRED_PREFIX = "chief_navigator_bone_";
    private static final Set<String> EXCLUDED_FRAGMENTS = new HashSet<>(Arrays.asList(
            "swarm_launcher", "seeker_fragment", "kinetic_fragments",
            "unstable_fragment", "devouring_swarm", "voltaic_discharge"));

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static String read(Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String group(boolean autofire, String mode, String... fit) {
        Map<String, String> weapons = new TreeMap<>();
        for (int index = 0; index < fit.length; index += 2) {
            check(weapons.put(fit[index], fit[index + 1]) == null,
                    "Repeated expected weapon slot " + fit[index]);
        }
        return autofire + "/" + mode + "/" + weapons;
    }

    private static Map<String, List<String>> expectedFits() {
        Map<String, List<String>> fits = new LinkedHashMap<>();
        fits.put("assault_Type200", Arrays.asList(
                group(false, "LINKED", "WS 001", "voidblaster", "WS 002", "voidblaster"),
                group(true, "LINKED", "WS 008", "light_mass_driver", "WS 009", "light_mass_driver")));
        fits.put("assault_Type201", Arrays.asList(
                group(false, "LINKED", "WS 001", "voidblaster", "WS 002", "voidblaster"),
                group(true, "LINKED", "WS 005", "light_mass_driver", "WS 006", "light_mass_driver",
                        "WS 008", "light_mass_driver", "WS 009", "light_mass_driver")));
        fits.put("fabricator_Type450", Arrays.asList(
                group(true, "LINKED", "WS 001", "heavy_mass_driver", "WS 010", "heavy_mass_driver"),
                group(true, "LINKED", "WS 008", "light_mass_driver", "WS 012", "light_mass_driver")));
        fits.put("hive_Type350", Arrays.asList(
                group(false, "LINKED", "WS 002", "seeker_fragment", "WS 003", "seeker_fragment",
                        "WS 004", "seeker_fragment", "WS 007", "seeker_fragment"),
                group(false, "LINKED", "WS 008", "kinetic_fragments", "WS 009", "kinetic_fragments",
                        "WS 010", "kinetic_fragments", "WS 011", "kinetic_fragments"),
                group(true, "LINKED", "WS 005", "unstable_fragment", "WS 006", "unstable_fragment"),
                group(false, "LINKED", "WS 000", "swarm_launcher", "WS 001", "swarm_launcher")));
        fits.put("overseer_Type250", Arrays.asList(
                group(true, "LINKED", "WS 006", "heavy_mass_driver"),
                group(false, "LINKED", "WS 008", "voltaic_cannon", "WS 009", "voltaic_cannon")));
        fits.put("skirmish_Type100", Arrays.asList(
                group(true, "LINKED", "WS 003", "heavy_mass_driver")));
        fits.put("skirmish_Type101", Arrays.asList(
                group(true, "LINKED", "WS 003", "heavy_mass_driver"),
                group(false, "LINKED", "WS 000", "neutron_torpedo"),
                group(false, "ALTERNATING", "WS 001", "voltaic_cannon", "WS 002", "voltaic_cannon")));
        List<String> standardLine = Arrays.asList(
                group(true, "LINKED", "WS 012", "neoferric_quadcoil", "WS 013", "neoferric_quadcoil"),
                group(true, "LINKED", "WS 003", "light_mass_driver"),
                group(true, "LINKED", "WS 010", "heavy_mass_driver", "WS 011", "heavy_mass_driver"));
        fits.put("standoff_Type300", standardLine);
        fits.put("standoff_Type301", Arrays.asList(
                group(true, "LINKED", "WS 012", "neoferric_quadcoil", "WS 013", "neoferric_quadcoil"),
                group(false, "LINKED", "WS 015", "neutron_torpedo", "WS 016", "neutron_torpedo",
                        "WS 017", "neutron_torpedo"),
                group(false, "LINKED", "WS 003", "voltaic_cannon"),
                group(true, "LINKED", "WS 010", "heavy_mass_driver", "WS 011", "heavy_mass_driver")));
        fits.put("standoff_Type302", standardLine);
        return fits;
    }

    private static List<String> expectedGautamaFit() {
        return Arrays.asList(
                group(true, "LINKED", "WS 001", "heavy_mass_driver", "WS 010", "heavy_mass_driver",
                        "WS 034", "heavy_mass_driver", "WS 035", "heavy_mass_driver"),
                group(false, "ALTERNATING", "WS 023", "devouring_swarm", "WS 024", "devouring_swarm"),
                group(true, "LINKED", "WS 008", "light_mass_driver", "WS 012", "light_mass_driver"),
                group(true, "LINKED", "WS 014", "unstable_fragment", "WS 015", "unstable_fragment"),
                group(true, "ALTERNATING", "WS 020", "voltaic_discharge"),
                group(true, "LINKED", "WS 032", "neoferric_quadcoil", "WS 033", "neoferric_quadcoil"));
    }

    private static void nativeWeapon(String weapon, Path core, String context) throws Exception {
        check(!weapon.startsWith(RETIRED_PREFIX), context + " equips retired custom weapon " + weapon);
        Path nativeSpec = core.resolve("data/weapons/" + weapon + ".wpn");
        check(Files.isRegularFile(nativeSpec), context + " has no stock weapon spec: " + weapon);
        check(read(nativeSpec).matches("(?s).*\"id\"\\s*:\\s*\"" + weapon + "\".*"),
                "Stock weapon spec must declare exact native ID " + weapon);
        check(!Files.exists(Path.of("data/weapons", weapon + ".wpn")),
                "Chief Navigator must not override stock weapon spec " + weapon);
    }

    private static List<String> inspectVariant(String id, boolean ordinary, Path core) throws Exception {
        Path path = Path.of("data/variants", id + ".variant");
        String text = read(path);
        check(!text.contains(RETIRED_PREFIX), id + " must only equip native weapon IDs");
        JSONObject variant = new JSONObject(text);
        check(id.equals(variant.getString("variantId")), "Authored variant ID changed: " + id);
        JSONArray groups = variant.getJSONArray("weaponGroups");
        List<String> fit = new ArrayList<>();
        Set<String> fittedSlots = new HashSet<>();
        for (int index = 0; index < groups.length(); index++) {
            JSONObject actual = groups.getJSONObject(index);
            JSONObject weapons = actual.getJSONObject("weapons");
            Map<String, String> bySlot = new TreeMap<>();
            for (Iterator<?> slots = weapons.keys(); slots.hasNext();) {
                String slot = (String) slots.next();
                String weapon = weapons.getString(slot);
                check(fittedSlots.add(slot), id + " repeats slot " + slot);
                nativeWeapon(weapon, core, id);
                check(!ordinary || !EXCLUDED_FRAGMENTS.contains(weapon),
                        id + " restores excluded fragment weapon " + weapon);
                bySlot.put(slot, weapon);
            }
            fit.add(actual.getBoolean("autofire") + "/" + actual.getString("mode") + "/" + bySlot);
        }
        return fit;
    }

    private static void inspectHull(Path hull, Path core) throws Exception {
        String text = read(hull);
        check(!text.contains(RETIRED_PREFIX), hull + " must not build in retired custom weapons");
        JSONObject spec = new JSONObject(text);
        JSONObject weapons = spec.optJSONObject("builtInWeapons");
        if (weapons == null) return;
        for (Iterator<?> slots = weapons.keys(); slots.hasNext();) {
            String slot = (String) slots.next();
            nativeWeapon(weapons.getString(slot), core, hull.toString());
        }
    }

    private static String secondCsvField(String row) {
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        int column = 0;
        for (int index = 0; index < row.length(); index++) {
            char character = row.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < row.length() && row.charAt(index + 1) == '"') {
                    if (column == 1) field.append('"');
                    index++;
                } else quoted = !quoted;
            } else if (character == ',' && !quoted) {
                if (column++ == 1) return field.toString();
            } else if (column == 1) field.append(character);
        }
        return field.toString();
    }

    public static void main(String[] args) throws Exception {
        Path core = args.length > 0 ? Path.of(args[0]) : Path.of("../../starsector-core");
        check(Files.isDirectory(core), "Run from the mod root or pass the starsector-core directory");
        Map<String, List<String>> fits = expectedFits();
        Set<String> ordinaryIds = new HashSet<>();
        for (Map.Entry<String, List<String>> fit : fits.entrySet()) {
            String id = "chief_navigator_starving_" + fit.getKey();
            ordinaryIds.add(id + ".variant");
            List<String> actual = inspectVariant(id, !fit.getKey().equals("hive_Type350"), core);
            check(actual.equals(fit.getValue()), id + " changed authored mounts, counts, groups or controls: " + actual);
        }
        try (Stream<Path> paths = Files.list(Path.of("data/variants"))) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                String name = path.getFileName().toString();
                if (name.startsWith("chief_navigator_starving_") && name.endsWith(".variant")) {
                    check(ordinaryIds.contains(name), "New Starving variant needs native-fit coverage: " + name);
                }
            }
        }
        for (String id : Arrays.asList("chief_navigator_scylla_Fabricator", "chief_navigator_scylla_Final",
                "chief_navigator_scylla_reincarnating_Fabricator")) {
            check(inspectVariant(id, false, core).equals(expectedGautamaFit()),
                    id + " must preserve Gautama's complete authored fit with native IDs");
        }
        for (String role : Arrays.asList("assault", "fabricator", "hive", "overseer", "skirmish", "standoff")) {
            inspectHull(Path.of("data/hulls/chief_navigator_starving_" + role + "_unit.ship"), core);
        }
        inspectHull(Path.of("data/hulls/chief_navigator_scylla_base.ship"), core);
        for (String skin : Arrays.asList("chief_navigator_scylla", "chief_navigator_scylla_final",
                "chief_navigator_scylla_reincarnating")) {
            inspectHull(Path.of("data/hulls/skins/" + skin + ".skin"), core);
        }
        Set<String> stockIds = new HashSet<>();
        for (String row : read(core.resolve("data/weapons/weapon_data.csv")).split("\\R")) {
            stockIds.add(secondCsvField(row));
        }
        for (String row : read(Path.of("data/weapons/weapon_data.csv")).split("\\R")) {
            String id = secondCsvField(row);
            check(id.equals("id") || id.isEmpty() || !stockIds.contains(id),
                    "Chief Navigator must not override native weapon statistics: " + id);
        }
        String generator = read(Path.of("tools/generate_starving_threat_hulls.ps1"));
        check(!generator.contains(RETIRED_PREFIX) && !generator.contains("$starvingWeapons"),
                "Hull generator must retain stock weapon IDs without custom remapping");
        for (String fragment : EXCLUDED_FRAGMENTS) {
            check(generator.contains("\"" + fragment + "\""),
                    "Hull generator must keep fragment exclusion " + fragment);
        }
        try (Stream<Path> paths = Files.walk(Path.of("src"))) {
            for (Path path : (Iterable<Path>) paths.filter(path -> path.toString().endsWith(".java"))::iterator) {
                check(!read(path).contains(RETIRED_PREFIX),
                        "Runtime creation/reconstruction must not remap stock weapons in " + path);
            }
        }
        System.out.println("PASS: ten Starving and three Gautama variants retain native THREAT IDs, "
                + "authored weapon mounts/counts/groups and fragment exclusions; built-ins, generator "
                + "and runtime source cannot restore custom weapons or override native weapon data.");
    }
}
