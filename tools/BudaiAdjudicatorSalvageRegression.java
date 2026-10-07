import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Source/data contract for Budai's one-time Heavy Adjudicator salvage. */
public final class BudaiAdjudicatorSalvageRegression {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static Path findRoot() {
        Path candidate = Paths.get("").toAbsolutePath();
        for (int depth = 0; depth < 4 && candidate != null; depth++) {
            if (Files.isRegularFile(candidate.resolve(
                    "src/chiefnavigator/ChiefNavigatorModPlugin.java"))) {
                return candidate;
            }
            candidate = candidate.getParent();
        }
        throw new IllegalStateException("Could not locate Chief Navigator root");
    }

    private static String read(Path root, String relative) throws Exception {
        return new String(Files.readAllBytes(root.resolve(relative)),
                StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<String>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < line.length()
                        && line.charAt(index + 1) == '"') {
                    value.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                values.add(value.toString());
                value.setLength(0);
            } else {
                value.append(current);
            }
        }
        values.add(value.toString());
        return values;
    }

    private static Map<String, String> weaponRow(String csv, String id) {
        String[] lines = csv.split("\\R");
        List<String> headers = parseCsvLine(lines[0]);
        for (int index = 1; index < lines.length; index++) {
            List<String> values = parseCsvLine(lines[index]);
            if (values.size() < 2 || !id.equals(values.get(1))) continue;
            check(values.size() == headers.size(),
                    "Weapon row has " + values.size() + " fields; expected "
                            + headers.size());
            Map<String, String> row = new HashMap<String, String>();
            for (int field = 0; field < headers.size(); field++) {
                row.put(headers.get(field), values.get(field));
            }
            return row;
        }
        throw new AssertionError("Missing weapon row " + id);
    }

    private static void expect(
            Map<String, String> row, String field, String expected) {
        check(expected.equals(row.get(field)),
                field + " mismatch: expected '" + expected + "', got '"
                        + row.get(field) + "'");
    }

    public static void main(String[] args) throws Exception {
        Path root = findRoot();
        String id = "chief_navigator_mountable_heavy_adjudicator";
        Map<String, String> row = weaponRow(
                read(root, "data/weapons/weapon_data.csv"), id);

        expect(row, "name", "Heavy Adjudicator");
        expect(row, "base value", "500000");
        expect(row, "range", "800");
        expect(row, "damage/shot", "800");
        expect(row, "turn rate", "0");
        expect(row, "OPs", "40");
        expect(row, "ammo", "20");
        expect(row, "ammo/sec", "0.5");
        expect(row, "reload size", "5");
        expect(row, "type", "FRAGMENTATION");
        expect(row, "energy/shot", "100");
        expect(row, "chargeup", "0");
        expect(row, "chargedown", "0.333333");
        expect(row, "burst size", "1");
        expect(row, "min spread", "0");
        expect(row, "max spread", "5");
        expect(row, "spread/shot", "3");
        expect(row, "spread decay/sec", "5");
        expect(row, "proj speed", "1200");
        expect(row, "extraArcForAI", "3");
        expect(row, "hints", "PD,PD_ALSO");
        expect(row, "groupTag", "");
        String tags = row.get("tags");
        for (String tag : new String[]{
                "restricted", "no_bp", "no_dealer", "no_drop",
                "no_drop_salvage"}) {
            check(tags.contains(tag), "Missing acquisition tag " + tag);
        }
        check(!tags.contains("no_sell"),
                "The most-expensive status should remain meaningful when sold");
        check(!row.get("hints").contains("SYSTEM"),
                "The reward must not retain the built-in-only SYSTEM hint");

        String weapon = read(root,
                "data/weapons/chief_navigator_mountable_heavy_adjudicator.wpn");
        check(weapon.contains("\"type\":\"BALLISTIC\"")
                        && weapon.contains("\"size\":\"LARGE\""),
                "The clone must fit large ballistic mounts");
        for (String stockAsset : new String[]{
                "heavy_adjudicator_turret_base.png",
                "heavy_adjudicator_turret_recoil.png",
                "heavy_adjudicator_hardpoint_base.png",
                "heavy_adjudicator_hardpoint_recoil.png",
                "\"projectileSpecId\":\"heavy_adjudicator_shot\"",
                "\"fireSoundTwo\":\"heavy_adjudicator_fire\""}) {
            check(weapon.contains(stockAsset),
                    "Mountable clone lost vanilla asset/behavior: " + stockAsset);
        }
        check(!weapon.contains("Effect"),
                "The vanilla behavior clone must not add a custom effect");

        String dialog = read(root,
                "src/chiefnavigator/quest/BudaiSalvageDialogPlugin.java");
        check(dialog.contains("WEAPON_COUNT = 2")
                        && dialog.contains("UNASSISTED_WEAPON_COUNT = 1")
                        && dialog.contains("grantReward(boolean isaAssisted)")
                        && dialog.contains("grantReward(isaAssisted)"),
                "The salvage reward must distinguish Isa's two guns from "
                        + "the unassisted single gun");
        check(dialog.contains("onslaught_mk1_Ancient")
                        && dialog.contains("showFleetMemberInfo(wreck, true)"),
                "The consumed Onslaught Mk.I must remain a display-only member");
        check(dialog.contains("captureIsaRefractionUnlock()")
                        && dialog.contains("opening_salvage_chief")
                        && dialog.contains("private boolean isaAssisted"),
                "The optional Isa branch needs a dependency-free fallback");
        check(!dialog.contains("shiptrophy."),
                "Chief Navigator must not import Hall of Triumph classes");

        String predator = read(root,
                "src/chiefnavigator/quest/OdysseyPredatorScript.java");
        check(predator.contains("class BudaiSalvageListener")
                        && predator.contains("BudaiSalvageDialogPlugin.request()")
                        && predator.contains("BudaiSalvageDialogPlugin.tryShowPending()"),
                "The first player kill must queue and later open the salvage scene");

        String rules = read(root, "data/campaign/rules.csv");
        check(rules.contains("ChiefNavigatorBudaiSalvage_opening_isa")
                        && rules.contains("ChiefNavigatorBudaiSalvage_opening_salvage_chief")
                        && rules.contains("Two modularized Heavy Adjudicators are transferred")
                        && rules.contains("A single modularized Heavy Adjudicator is transferred")
                        && rules.contains("DismissDialog"),
                "Rules-authored Isa and fallback salvage branches are incomplete");

        String all = read(root, "README.md")
                + read(root, "docs/CONCEPT.md")
                + read(root, "AGENTS.md")
                + predator + dialog + rules
                + read(root, "data/weapons/weapon_data.csv");
        check(!all.contains("Fo Dou You Huo")
                        && !all.contains("chief_navigator_fo_dou_you_huo"),
                "The abandoned unique Hungering Rift reward still exists");

        check(!Files.exists(root.resolve(
                        "data/weapons/chief_navigator_fo_dou_you_huo.wpn"))
                        && !Files.exists(root.resolve(
                                "src/chiefnavigator/weapons/FoDouYouHuoEffect.java")),
                "The abandoned unique weapon files must be removed");

        System.out.println("PASS: Budai disgorges a display-only Onslaught Mk.I "
                + "whose one-time salvage yields two mountable, stock-behavior "
                + "Heavy Adjudicators with Isa, or one without her.");
    }
}
