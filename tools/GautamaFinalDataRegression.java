import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Static contract for Gautama compatibility and the Ungaikyo encounter. */
public final class GautamaFinalDataRegression {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static String read(String relative) throws Exception {
        return Files.readString(Path.of(relative), StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws Exception {
        String skin = read(
                "data/hulls/skins/chief_navigator_scylla_final.skin");
        check(skin.contains("\"baseHullId\": \"chief_navigator_scylla_base\""),
                "Final Gautama must inherit the historical base hull");
        check(skin.contains("\"skinHullId\": \"chief_navigator_scylla_final\""),
                "Final Gautama skin id must remain stable");
        check(skin.contains("\"fighterBays\": 0"),
                "Final Gautama must have zero fighter bays");
        check(!skin.contains("builtInWings"),
                "Final Gautama must not carry built-in Attack Swarms");
        check(skin.contains("\"chief_navigator_scylla_boss\""),
                "Final Gautama must retain the boss hullmod");
        String shipData = read("data/hulls/ship_data.csv");
        check(shipData.contains(
                        "Gautama Base,chief_navigator_scylla_base,"
                        + "Command Organism,Threat,extraction_protocol,"),
                "Every Gautama skin must inherit the Line Unit's Extraction Protocol");
        check(!shipData.contains(",chief_navigator_ungaikyo,"),
                "Ungaikyo is a skin and must not have a duplicate hull spreadsheet row");

        String variant = read(
                "data/variants/chief_navigator_scylla_Final.variant");
        String historical = read(
                "data/variants/chief_navigator_scylla_Fabricator.variant");
        String reincarnating = read(
                "data/variants/chief_navigator_scylla_reincarnating_Fabricator.variant");
        check(variant.contains("\"hullId\": \"chief_navigator_scylla_final\""),
                "Final variant must select the zero-bay skin");
        check(variant.contains("\"variantId\": \"chief_navigator_scylla_Final\""),
                "Final variant id must remain stable");
        for (String weapon : new String[] {
                "heavy_mass_driver",
                "devouring_swarm",
                "light_mass_driver",
                "unstable_fragment",
                "voltaic_discharge",
                "neoferric_quadcoil"}) {
            check(historical.contains("\"" + weapon + "\"")
                            && reincarnating.contains("\"" + weapon + "\"")
                            && variant.contains("\"" + weapon + "\""),
                    "Every Gautama variant must retain native THREAT weapon: " + weapon);
        }
        check(occurrences(historical,
                        "neoferric_quadcoil") == 2
                        && occurrences(reincarnating,
                        "neoferric_quadcoil") == 2
                        && occurrences(variant,
                        "neoferric_quadcoil") == 2,
                "Every Gautama configuration must mount two Neoferric guns");
        check(occurrences(historical,
                        "heavy_mass_driver") == 4
                        && occurrences(reincarnating,
                        "heavy_mass_driver") == 4
                        && occurrences(variant,
                        "heavy_mass_driver") == 4,
                "Every Gautama configuration must mount four Heavy Mass Drivers");
        check(!historical.contains("chief_navigator_bone_")
                        && !reincarnating.contains("chief_navigator_bone_")
                        && !variant.contains("chief_navigator_bone_"),
                "Gautama must never equip retired custom Starving weapons");
        String gautamaHull = read("data/hulls/chief_navigator_scylla_base.ship");
        for (String slot : new String[] {"WS 032", "WS 033", "WS 034", "WS 035"}) {
            check(gautamaHull.contains("\"id\": \"" + slot + "\""),
                    "Gautama base hull must expose added weapon slot " + slot);
        }

        String weaponData = read("data/weapons/weapon_data.csv");
        int starvingWeapons = 0;
        for (String row : weaponData.split("\\R")) {
            if (!row.contains("chief_navigator_bone_")) continue;
            starvingWeapons++;
            for (String exclusion : new String[] {
                    "restricted", "no_bp", "no_sell", "no_dealer",
                    "no_drop", "no_drop_salvage",
                    "chief_navigator_starving_weapon"}) {
                check(row.contains(exclusion),
                        "Retired custom weapon row must carry exclusion/tag: "
                                + exclusion);
            }
            check(!row.contains("\"threat,"),
                    "Retired custom weapon must not enter the broad THREAT loadout pool");
        }
        check(starvingWeapons == 12,
                "All twelve retired custom weapon definitions must stay restricted");
        for (String row : shipData.split("\\R")) {
            if (!row.contains(",chief_navigator_starving_")) continue;
            check(row.contains("no_autofit"),
                    "Starving hulls must retain their authored loadouts");
        }

        String hullmods = read("data/hullmods/hull_mods.csv");
        check(hullmods.contains(
                        "chief_navigator_gautama_mirror,3,,Threat,"
                        + "\"no_build_in, no_drop, threat\""),
                "Mirror marker must be hidden from ordinary installation/drop");
        check(hullmods.contains(
                        "chiefnavigator.hullmods.GautamaMirrorHullmod"),
                "Mirror marker must bind its combat hullmod script");
        check(hullmods.contains(
                        "Gautama reconstruction: 10% frigate, 20% destroyer, "
                        + "30% cruiser, 50% Fabricator"),
                "Starving Threat description must advertise every scaled roll");

        String rules = read("data/campaign/rules.csv");
        check(rules.contains("Gautama was destroyed with it.")
                        && rules.contains("nanometer-scale black filaments "
                                + "spanning light-years")
                        && rules.contains("designated it 'Ungaikyo.'")
                        && rules.contains("Gautama was a coalescence"),
                "Menelaus's debrief must reveal Ungaikyo as Gautama's true form");
        check(!rules.contains("Gautama survived."),
                "Runtime dialogue must not retain the retired survival claim");

        String ungaikyoSkin = read(
                "data/hulls/skins/chief_navigator_ungaikyo.skin");
        String ungaikyoVariant = read(
                "data/variants/chief_navigator_ungaikyo_Fabricator.variant");
        check(ungaikyoSkin.contains("\"baseHullId\": \"fabricator_unit\"")
                        && ungaikyoSkin.contains(
                                "chief_navigator_ungaikyo_fabricator_v1.png")
                        && ungaikyoSkin.contains(
                                "chief_navigator_ungaikyo_construction_swarm"),
                "Ungaikyo must be a dark asset-backed vanilla Fabricator skin");
        check(ungaikyoSkin.contains("\"threat\"")
                        && ungaikyoVariant.contains("\"fragment_swarm\""),
                "Native Threat tagging must hide the retrofit overlay without removing Fragment Swarm");
        String ungaikyoSystem = read(
                "src/chiefnavigator/systems/UngaikyoConstructionSwarmSystem.java");
        check(ungaikyoSystem.contains("extends ExtractionProtocolSystemScript")
                        && ungaikyoSystem.contains("ACCELERATE_BACKWARDS")
                        && !ungaikyoSystem.contains("ConstructionSwarmSystemScript"),
                "Ungaikyo must extract backward while its controller owns reflection construction");
        check(ungaikyoVariant.contains(
                        "\"hullId\": \"chief_navigator_ungaikyo\"")
                        && ungaikyoVariant.contains("\"heavy_mass_driver\"")
                        && ungaikyoVariant.contains("\"devouring_swarm\"")
                        && ungaikyoVariant.contains("\"secondary_fabricator\""),
                "Ungaikyo must retain the stock Fabricator loadout and hullmods");

        String arrival = read(
                "src/chiefnavigator/quest/TroyArrivalScript.java");
        String rematchRoster = between(arrival,
                "private static final String[] UNGAIKYO_FINAL_REMATCH_VARIANTS",
                "public static final String CHARYBDIS_FLEET_ID");
        check(occurrences(rematchRoster,
                        "chief_navigator_ungaikyo_Fabricator") == 1
                        && occurrences(rematchRoster,
                                "chief_navigator_starving_fabricator_Type450") == 0
                        && occurrences(rematchRoster,
                                "chief_navigator_starving_hive_Type350") == 1
                        && occurrences(rematchRoster,
                                "chief_navigator_starving_overseer_Type250") == 1
                        && occurrences(rematchRoster,
                                "chief_navigator_starving_standoff_Type") == 1
                        && occurrences(rematchRoster,
                                "chief_navigator_starving_assault_Type") == 1
                        && occurrences(rematchRoster,
                                "chief_navigator_starving_skirmish_Type") == 1,
                "New Ungaikyo rosters must have exactly five mixed escorts and no escort Fabricator");
        check(arrival.contains("UNGAIKYO_LAST_LIGHT_ASSIGNMENT_V1")
                        && arrival.contains(
                                "LAST_LIGHT_GUARDIAN_WRECK_ID")
                        && arrival.contains(
                                "coiled around the derelict Guardian"),
                "Ungaikyo must guard the Guardian in Last Light");

        String predator = read(
                "src/chiefnavigator/quest/OdysseyPredatorScript.java");
        check(predator.contains(
                        "OdysseyExpanseSystem.LAST_LIGHT_ID")
                        && predator.contains(
                                "LAST_LIGHT_GUARDIAN_WRECK_ID"),
                "Post-Labor Ungaikyo maintenance must target Last Light");
        String world = read(
                "src/chiefnavigator/quest/OdysseyExpanseSystem.java");
        check(!world.contains("spawnTimeFreezeFacetSample(")
                        && world.contains("retireTimeFreezeFacetSample()"),
                "The Time Freeze prototype must remain shelved");
        String campaign = read(
                "src/chiefnavigator/quest/TreadmillCampaignPlugin.java");
        check(campaign.contains("isLastLightGuardianWreck(target)")
                        && campaign.contains(
                                "!OdysseyPredatorScript"
                                + ".isUngaikyoRematchDefeated()")
                        && campaign.contains(
                                "ChiefNavigatorLastLightGuardianLocked"),
                "Guardian recovery must remain locked until Ungaikyo dies");
        check(rules.contains("ChiefNavigatorLastLightGuardianLocked")
                        && rules.contains(
                                "cannot be approached while Ungaikyo remains coherent"),
                "The locked Guardian needs rules-authored presentation");

        String docs = read("README.md") + read("AGENTS.md")
                + read("docs/CONCEPT.md");
        check(docs.contains("separate 240-DP cap")
                        && docs.contains("below 200 DP")
                        && docs.contains("same-size stock THREAT"),
                "Encounter prose must specify the 240/200-DP selection policy");
        check(docs.contains("never retry or substitute another hull")
                        && docs.contains("only that source"),
                "Third-party reflection failures must quarantine only their source");
        check(!docs.contains("reforms indefinitely at the Devoured Ring")
                        && docs.contains("10% for a frigate")
                        && docs.contains("50% for the Fabricator")
                        && docs.contains("ten percentage points")
                        && docs.contains("successful construction resets")
                        && docs.contains("permanent Ungaikyo encounter"),
                "Project docs must describe Labor V pity while suppressing it for Ungaikyo");
        String rematchController = read(
                "src/chiefnavigator/quest/"
                        + "GautamaFinalRematchBattleCreationPlugin.java");
        check(rematchController.contains(
                        "GautamaReincarnation.suppressForBattle(engine)"),
                "Ungaikyo combat must suppress ordinary Gautama reincarnation");

        System.out.println("PASS: retired custom weapons remain restricted; "
                + "Gautama carries native THREAT weapons, two Neoferric guns "
                + "and four Heavy Mass Drivers "
                + "in every compatibility configuration; Ungaikyo uses a dark "
                + "stock Fabricator, the 240/200-DP reflection policy, and "
                + "suppression without disabling Labor V's pity cycle.");
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        int from = 0;
        while ((from = text.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
    }

    private static String between(
            String text, String startNeedle, String endNeedle) {
        int start = text.indexOf(startNeedle);
        int end = start < 0 ? -1 : text.indexOf(endNeedle, start);
        check(start >= 0 && end > start,
                "Could not locate the authored Ungaikyo rematch roster");
        return text.substring(start, end);
    }
}
