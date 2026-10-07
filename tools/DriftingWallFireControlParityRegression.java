import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Source/data contract for damaged-Wall naval fire control.
 *
 * The intact Ithaca foundation is the authority: the damaged Wall may replace
 * individual emplacements with wrecks, but every surviving naval gun must use
 * the same sight package, authored slot arc, autofire configuration, and
 * vanilla station-AI targeting path.
 */
public final class DriftingWallFireControlParityRegression {
    private static final Path ROOT = Path.of("").toAbsolutePath();

    public static void main(String[] args) throws Exception {
        String driftingHull = read(
                "data/hulls/chief_navigator_drifting_wall_base.ship");
        String ithacaHull = read(
                "data/hulls/chief_navigator_ithaca_foundation.ship");
        String regularNavalMount = read(
                "data/hulls/chief_navigator_ithaca_naval_mount.ship");
        String spartanBattlestation = read(
                "data/hulls/chief_navigator_spartan_battlestation.ship");
        String driftingVariant = read(
                "data/variants/chief_navigator_drifting_wall_base_Standard.variant");
        String ithacaVariant = read(
                "data/variants/chief_navigator_ithaca_intact_base_Standard.variant");
        String spartanVariant = read(
                "data/variants/chief_navigator_spartan_battlestation_Teal.variant");
        String legacyNavalVariant = read(
                "data/variants/chief_navigator_spartan_naval_laser_Standard.variant");
        String battlePlugin = read(
                "src/chiefnavigator/quest/DriftingWallBattleCreationPlugin.java");
        String ithacaBattlePlugin = read(
                "src/chiefnavigator/quest/IthacaSectionBattleCreationPlugin.java");
        String fireControl = read(
                "src/chiefnavigator/quest/IthacaFoundationFireControl.java");
        String beamEffect = read(
                "src/chiefnavigator/weapons/DriftingWallBeamEffect.java");
        String legacyHitscanEffect = read(
                "src/chiefnavigator/weapons/DriftingWallHitscanEffect.java");
        String weaponData = read("data/weapons/weapon_data.csv");
        String longRangeHullmod = read(
                "src/chiefnavigator/hullmods/IthacaLongRangeEmplacement.java");
        String researchUpgrades = read(
                "src/chiefnavigator/weapons/IthacaResearchUpgrades.java");

        require(driftingHull.contains(
                        "\"chief_navigator_ithaca_naval_fire_control\""),
                "Damaged Wall must carry Ithaca naval sight control");
        require(ithacaHull.contains(
                        "\"chief_navigator_ithaca_naval_fire_control\""),
                "Intact Ithaca foundation must carry naval sight control");
        require(regularNavalMount.contains(
                        "\"chief_navigator_ithaca_naval_fire_control\""),
                "Regular Ithaca naval module must carry naval sight control");
        require(spartanBattlestation.contains(
                        "\"chief_navigator_ithaca_naval_fire_control\""),
                "Spartan direct naval mount must carry naval sight control");

        assertSlotParity(driftingHull, ithacaHull,
                "COL_NAVAL_01", 35f, 360f);
        assertSlotParity(driftingHull, ithacaHull,
                "COL_NAVAL_02", 329f, 360f);
        assertSlotParity(driftingHull, ithacaHull,
                "COL_NAVAL_03", 108f, 360f);
        assertSlotParity(driftingHull, ithacaHull,
                "COL_NAVAL_04", 251f, 360f);

        assertAutofireGroup(driftingVariant,
                "COL_NAVAL_02", "COL_NAVAL_03", "COL_NAVAL_04");
        assertInertFixtureGroup(driftingVariant, "COL_NAVAL_01",
                "chief_navigator_drifting_wall_colossal_inert");
        assertInertFixtureGroup(driftingVariant, "LARGE_BALLISTIC_01",
                "chief_navigator_drifting_wall_annihilator_launcher");
        assertAutofireGroup(ithacaVariant,
                "COL_NAVAL_01", "COL_NAVAL_02", "COL_NAVAL_03",
                "COL_NAVAL_04");
        assertSharedNavalWeapon(driftingVariant,
                "COL_NAVAL_02", "COL_NAVAL_03", "COL_NAVAL_04");
        assertSharedNavalWeapon(ithacaVariant,
                "COL_NAVAL_01", "COL_NAVAL_02", "COL_NAVAL_03",
                "COL_NAVAL_04");
        assertSharedNavalWeapon(spartanVariant, "CN NAVAL");
        assertSharedNavalWeapon(legacyNavalVariant, "WS 001");

        float baseRange = weaponRange(weaponData,
                "chief_navigator_drifting_wall_colossal_inert");
        require(baseRange == 7500f,
                "Shared foundation naval turret range must be reduced 25% to 7,500");
        require(!driftingHull.contains("\"chief_navigator_ithaca_range\"")
                        && !ithacaHull.contains(
                                "\"chief_navigator_ithaca_range\""),
                "New damaged and intact foundations must share native range");
        require(spartanBattlestation.contains("\"supercomputer\"")
                        && !spartanBattlestation.contains(
                                "\"chief_navigator_ithaca_range\""),
                "Spartan battlestation must use only its authored targeting "
                        + "computer range bonus, not the legacy 10x package");
        require(regularNavalMount.contains(
                        "\"chief_navigator_ithaca_range\""),
                "Legacy modular emplacement must retain its 10x package");
        require(longRangeHullmod.contains(
                        "private static final float RANGE_MULTIPLIER = 10f;"),
                "Legacy modular emplacement multiplier changed unexpectedly");
        require(baseRange * 10f == 75000f,
                "Legacy modular naval maximum must also be reduced 25% to 75,000");
        require(weaponRange(weaponData, "chief_navigator_naval_gun") == 675f,
                "Legacy naval gun range must share the 25% reduction to 675");
        require(legacyHitscanEffect.contains(
                        "private static final float RANGE = 7500f;"),
                "Dormant hitscan compatibility path must share 7,500 range");
        require(!researchUpgrades.contains("WeaponRangeBonus"),
                "Installed research must not create a divergent naval range");
        require(weaponValue(weaponData,
                        "chief_navigator_drifting_wall_colossal_inert",
                        "turn rate") == 2.5f,
                "Full arcs must retain the authored 2.5-degree traverse rate");
        String navalHints = weaponTextValue(weaponData,
                "chief_navigator_drifting_wall_colossal_inert", "hints");
        require(navalHints.contains("DO_NOT_CONSERVE")
                        && !navalHints.contains("STRIKE"),
                "Sustained naval beams must fire freely rather than waiting "
                        + "for a vanilla strike-weapon opportunity");

        require(battlePlugin.contains(
                        "IthacaFoundationFireControl.ensureActive(ship);"),
                "Damaged Wall must activate shared foundation fire control");
        require(countOccurrences(battlePlugin,
                        "IthacaFoundationFireControl.ensureActive(") == 1,
                "Damaged Wall must initialize fire control once instead of "
                        + "resetting vanilla autofire every frame");
        require(ithacaBattlePlugin.contains(
                        "IthacaFoundationFireControl.ensureActive(foundation);"),
                "Intact Ithaca must activate shared foundation fire control");
        require(fireControl.contains("foundation.setControlsLocked(false);")
                        && fireControl.contains("foundation.resetDefaultAI();")
                        && fireControl.contains("group.toggleOn();")
                        && fireControl.contains("group.toggleOff();"),
                "Shared foundation control must unlock, install vanilla AI, "
                        + "enable live autofire, and disable wreck groups");
        require(fireControl.contains("weapon.setForceDisabled(true);")
                        && fireControl.contains("weapon.setForceDisabled(false);"),
                "Shared restoration must distinguish inert fixtures from live mounts");
        require(!fireControl.contains("setShipTarget(")
                        && !fireControl.contains("setShipAI(")
                        && !fireControl.contains("setForceFireOneFrame"),
                "Shared foundation control must not force AI, targets, or fire");
        require(!battlePlugin.contains("core.setShipAI("),
                "Wall foundation must retain vanilla station AI");
        require(!battlePlugin.contains("core.setShipTarget("),
                "Wall foundation must not receive a forced target");
        int discovery = battlePlugin.indexOf("if (core == null)");
        int reactorDiscovery = battlePlugin.indexOf("findReactor();", discovery);
        int pauseGuard = battlePlugin.indexOf(
                "if (engine.isPaused()) return;", discovery);
        int combatWork = battlePlugin.indexOf(
                "deployDroneGuards();", reactorDiscovery);
        require(discovery >= 0 && reactorDiscovery > discovery
                        && pauseGuard > reactorDiscovery
                        && combatWork > pauseGuard,
                "Deployment pause must initialize/restore/unlock fire control "
                        + "before freezing spawns, repair, and combat state");
        require(!beamEffect.contains("setSuspendAutomaticTurning")
                        && !beamEffect.contains("setForceFireOneFrame")
                        && !beamEffect.contains("setForceNoFireOneFrame")
                        && !beamEffect.contains("setCurrAngle"),
                "Naval visual effect must not override vanilla aim or fire logic");

        System.out.println(
                "Drifting Wall fire-control parity regression checks passed.");
    }

    private static void assertSlotParity(
            String driftingHull,
            String ithacaHull,
            String slotId,
            float authoredAngle,
            float authoredArc) {
        String drifting = objectContaining(driftingHull, slotId);
        String ithaca = objectContaining(ithacaHull, slotId);
        float driftingAngle = number(drifting, "angle");
        float driftingArc = number(drifting, "arc");
        require(driftingAngle == number(ithaca, "angle")
                        && driftingArc == number(ithaca, "arc"),
                slotId + " must match intact Ithaca angle and arc");
        require(driftingAngle == authoredAngle && driftingArc == authoredArc,
                slotId + " must preserve its authored angle and arc");
    }

    private static void assertAutofireGroup(
            String variant, String... slotIds) {
        String group = groupContaining(variant, slotIds[0]);
        require(group.contains("\"autofire\": true"),
                "Naval weapon group must start in autofire");
        for (String slotId : slotIds) {
            require(group.contains("\"" + slotId + "\""),
                    "Naval autofire group is missing " + slotId);
        }
    }

    private static void assertInertFixtureGroup(
            String variant, String slotId, String liveWeaponId) {
        String group = groupContaining(variant, slotId);
        require(group.contains("\"autofire\": false"),
                slotId + " wreck group must not use autofire");
        require(!group.contains("\"" + liveWeaponId + "\""),
                slotId + " wreck must not share a group with live weapons");
    }

    private static String groupContaining(String variant, String slotId) {
        int slot = variant.indexOf("\"" + slotId + "\"");
        require(slot >= 0, "Missing weapon slot " + slotId);
        int weaponMapStart = variant.lastIndexOf("\"weapons\"", slot);
        int groupStart = variant.lastIndexOf("{", weaponMapStart - 1);
        int groupEnd = variant.indexOf("}\n    }", slot);
        require(groupStart >= 0 && groupEnd > groupStart,
                "Could not resolve weapon group for " + slotId);
        return variant.substring(groupStart, groupEnd);
    }

    private static void assertSharedNavalWeapon(
            String variant, String... slotIds) {
        for (String slotId : slotIds) {
            require(variant.contains("\"" + slotId + "\": "
                            + "\"chief_navigator_drifting_wall_colossal_inert\""),
                    slotId + " must use the shared colossal naval turret spec");
        }
    }

    private static float weaponRange(String csv, String weaponId) {
        return weaponValue(csv, weaponId, "range");
    }

    private static float weaponValue(
            String csv, String weaponId, String column) {
        return Float.parseFloat(weaponTextValue(csv, weaponId, column));
    }

    private static String weaponTextValue(
            String csv, String weaponId, String column) {
        String[] lines = csv.split("\\R");
        List<String> header = parseCsv(lines[0]);
        int idColumn = header.indexOf("id");
        int valueColumn = header.indexOf(column);
        require(idColumn >= 0 && valueColumn >= 0,
                "weapon_data.csv is missing id/" + column + " columns");
        for (int index = 1; index < lines.length; index++) {
            List<String> row = parseCsv(lines[index]);
            if (row.size() <= Math.max(idColumn, valueColumn)
                    || !weaponId.equals(row.get(idColumn))) continue;
            return row.get(valueColumn);
        }
        throw new AssertionError("Missing weapon row " + weaponId);
    }

    private static int countOccurrences(String source, String needle) {
        int count = 0;
        int from = 0;
        while (true) {
            int found = source.indexOf(needle, from);
            if (found < 0) return count;
            count++;
            from = found + needle.length();
        }
    }

    private static List<String> parseCsv(String line) {
        List<String> result = new ArrayList<String>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char c = line.charAt(index);
            if (c == '"') {
                if (quoted && index + 1 < line.length()
                        && line.charAt(index + 1) == '"') {
                    field.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                result.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        result.add(field.toString());
        return result;
    }

    private static String objectContaining(String json, String value) {
        int valueAt = json.indexOf("\"id\": \"" + value + "\"");
        require(valueAt >= 0, "Missing slot " + value);
        int start = json.lastIndexOf('{', valueAt);
        int end = json.indexOf('}', valueAt);
        require(start >= 0 && end > start, "Malformed slot " + value);
        return json.substring(start, end + 1);
    }

    private static float number(String object, String key) {
        String token = "\"" + key + "\":";
        int at = object.indexOf(token);
        require(at >= 0, "Missing numeric property " + key);
        at += token.length();
        while (at < object.length()
                && Character.isWhitespace(object.charAt(at))) at++;
        int end = at;
        while (end < object.length()) {
            char c = object.charAt(end);
            if (!(Character.isDigit(c) || c == '-' || c == '.' || c == '+')) {
                break;
            }
            end++;
        }
        return Float.parseFloat(object.substring(at, end));
    }

    private static String read(String relative) throws Exception {
        return Files.readString(ROOT.resolve(relative), StandardCharsets.UTF_8);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
