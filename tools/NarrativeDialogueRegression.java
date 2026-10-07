package chiefnavigator.quest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Static contracts for the post-Labors and Odyssey environmental dialogue. */
public final class NarrativeDialogueRegression {
    private static final Path ROOT = Path.of("").toAbsolutePath();

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void contains(String text, String expected, String message) {
        check(text.contains(expected), message + ": " + expected);
    }

    private static void omits(String text, String forbidden, String message) {
        check(!text.contains(forbidden), message + ": " + forbidden);
    }

    public static void main(String[] args) throws Exception {
        String rules = read("data/campaign/rules.csv");
        String voss = read(
                "src/chiefnavigator/quest/HegemonyExpeditionInteraction.java");
        String contacts = read(
                "src/chiefnavigator/quest/FobIthacaContacts.java");
        String stationInteraction = read(
                "src/chiefnavigator/quest/FobIthacaStationInteraction.java");
        String strandedFleets = read(
                "src/chiefnavigator/quest/OdysseyStrandedFleetsScript.java");
        String system = read(
                "src/chiefnavigator/quest/OdysseyExpanseSystem.java");
        String plugin = read(
                "src/chiefnavigator/quest/TreadmillCampaignPlugin.java");
        String helenInk = read("dialogue/menelaus_helen_final.ink");
        String vossInk = read("dialogue/voss_requisition.ink");
        String residentsInk = read("dialogue/ithaca_residents.ink");
        String battlestationInk = read(
                "dialogue/derelict_battlestation.ink");

        contains(rules, "chiefNavigatorMenelausHelenFinal,",
                "The final Helen conversation must remain rules-authored");
        contains(rules,
                "$global.chief_navigator_menelaus_helen_final_heard_v2",
                "The final Helen conversation must be one-time");
        contains(rules, "AUTHORITY: GRAND NAVARCH KALI MOLINA",
                "Helen's manifest must name Molina's command authority");
        contains(rules, "My first and only rebellion against my nation",
                "Menelaus must admit that he fabricated the authorization");
        contains(rules, "chief_navigator_menelaus_helen_navarch_2",
                "The player must be able to ask about Menelaus's old status");
        contains(rules, "By undergoing 'Kenosis'—self-emptying",
                "Menelaus must explain the model for his self-limitation");
        contains(rules, "I would not sully my experiences with Lieutenant "
                        + "Commander Argyros for certainty",
                "Menelaus must reject simulating Helen for certainty");
        contains(rules, "That AI onboard FOB Ithaca is an annoying motherfucker",
                "Helen's journal must retain her own profane voice");
        contains(rules, "Still annoying as fuck. But I like it.",
                "Helen's journal must end on her admission");
        contains(helenInk, "AUTHORITY: GRAND NAVARCH KALI MOLINA",
                "The Helen Ink source must mirror the revised manifest");
        contains(helenInk, "=== changed_6 ===",
                "The Helen Ink source must retain the Kenosis explanation");
        contains(helenInk, "=== helen_diary ===",
                "The Helen Ink source must retain the journal ending");
        contains(helenInk, "You loved her.",
                "Sinni must surmise Menelaus's motive herself");
        contains(rules,
                "chiefNavigatorMenelausHelenEnd,DialogOptionSelected,"
                        + "$option == chief_navigator_menelaus_helen_end,"
                        + "\"$global.chief_navigator_menelaus_helen_final_heard_v2 = true\n"
                        + "DismissDialog\"",
                "Only the journal exit may complete the revised conversation");
        String finalCommit =
                "$global.chief_navigator_menelaus_helen_final_heard_v2 = true";
        check(rules.indexOf(finalCommit) == rules.lastIndexOf(finalCommit),
                "No intermediate Helen page may consume the conversation");
        omits(rules, "chiefNavigatorFobIthacaCompleteHelenLifeOption",
                "The superseded later-life continuation must be removed");
        omits(rules, "chiefNavigatorMenelausHelenLifeCrossing",
                "The superseded later-life branch must be unreachable");
        omits(rules, "You purchased my life with my trust",
                "The superseded recovered transcript must be removed");
        omits(helenInk, "=== helen_life_1 ===",
                "The Ink mirror must contain only the revised final scene");

        contains(voss, "RuleDialogSupport.fire",
                "Voss's custom interaction must dispatch rules dialogue");
        check(!voss.contains("addPara(") && !voss.contains("addOption("),
                "Voss prose and choices must not be hard-coded in Java");
        contains(voss, "HEGEMONY_DEMAND_SUPPLIES",
                "Voss must use the canonical supply demand");
        contains(voss, "HEGEMONY_DEMAND_FUEL",
                "Voss must use the canonical fuel demand");
        contains(rules, "chiefNavigatorVossPaid,ChiefNavigatorVossRequisitionPaid",
                "Voss must acknowledge successful payment");
        contains(rules, "common defense for fourteen days",
                "Voss must state the exact safe-passage term");
        contains(rules, "Domain Maritime Law 0081ac",
                "Voss must retain the supplied emergency-law demand");
        contains(rules, "chiefNavigatorVossHostileHunt",
                "Negative expedition reputation must expose the supplied hunt scene");
        contains(voss, "config.impactsEnemyReputation = true",
                "Combat must use vanilla enemy reputation loss");
        omits(rules, "chief_navigator_voss_requisition_leave",
                "The requisition cannot offer a no-cost disengagement route");
        omits(voss, "chief_navigator_voss_requisition_leave",
                "Java must not dismiss a removed requisition leave route");
        contains(voss, "leaveAlwaysAvailable = false",
                "The battle handoff must not expose a harmless leave option");
        omits(vossInk, "Attempt to disengage",
                "The Voss Ink source must route inability and refusal to battle");
        contains(strandedFleets,
                "Global.getSector().getMemoryWithoutUpdate().set(\n"
                        + "                    HEGEMONY_TRIBUTE_GRACE",
                "Safe passage must be visible to the whole Hegemony cordon");
        contains(strandedFleets,
                "configureHegemonyTroyPatrol(patrol, terminus)",
                "Payment must immediately reconfigure each Foxtrot patrol");

        contains(contacts, "KSHAYA_REFUGEE_PERSON_ID",
                "The rescued Kshaya family must have a persistent contact");
        contains(contacts, "AviciHabitatEncounter.REFUGEES_RESCUED",
                "The Kshaya contact must require the rescue outcome");
        contains(rules, "chiefNavigatorIthacaKshayaGreeting,",
                "Mira and Toma must have rules-authored dialogue");
        omits(rules, "chiefNavigatorIthacaLiaison",
                "The retired liaison's dialogue must be removed");
        omits(residentsInk, "liaison_",
                "The residents Ink must contain no retired liaison branches");
        omits(contacts, "getOrCreateLiaison",
                "Directory lookups must never create the retired liaison");
        contains(rules, "chiefNavigatorIthacaLeagueAttack,",
                "The League survivors must account for their attack");
        contains(rules, "chiefNavigatorIthacaLeagueReturnGreeting,",
                "The League commander needs a persistent return greeting");
        contains(rules, "chiefNavigatorIthacaKshayaReturnGreeting,",
                "Mira and Toma need a persistent return greeting");
        contains(rules, "chiefNavigatorIthacaDirectoryBackOption,",
                "The comm-directory return choice must remain rules-authored");
        omits(stationInteraction, "Return to the comm directory.",
                "Contact option wording must not be hard-coded in Java");
        contains(rules, "$chiefNavigatorIthacaLeagueMet = true",
                "Contact greeting state must use person-local rule memory");
        contains(rules,
                "$chief_navigator_ithaca_kshaya_refugee_contact\n"
                        + "$global.chief_navigator_sara_dead",
                "Sara's remembrance must read the sector-global death flag");
        for (String flag : new String[] {"LeagueAskedSanzu", "LeagueAskedAttack",
                "LeagueAskedFuture", "KshayaAskedSettling", "KshayaAskedPast",
                "KshayaAskedFuture", "KshayaAskedSara"}) {
            omits(rules, "!$chiefNavigatorIthaca" + flag,
                    "The user's resident topics must remain repeatable");
        }
        omits(rules, "chiefNavigatorIthacaKshayaOptionB,",
                "Mira's menu must not restore the removed Kshaya-history question");
        contains(rules, "chiefNavigatorIthacaLeagueSanzu2,",
                "The League history must retain the Starving Threat follow-up");
        contains(residentsInk, "=== league_victory ===",
                "The Ink must include the user's post-battle rescue decision");
        contains(rules, "chiefNavigatorLeagueRescueVictory,",
                "The post-battle decision must be rules-authored");
        contains(rules, "!$global.chief_navigator_sara_dead",
                "Sara's post-battle aside must require that she is alive");
        omits(strandedFleets, "FobIthacaContacts.embarkLeagueSurvivorsFromBattle()",
                "The population watcher must not auto-rescue the defeated crews");
        contains(rules, "I caught Yvan's sleeve",
                "Mira's account must match the Kshaya rescue scene");
        contains(rules, "Sara pushed Toma behind an access column",
                "Sara's final action must match the Kshaya rescue scene");
        contains(rules, "$PersonRank $personName receives your call",
                "The generated League commander must be identified in prose");
        omits(residentsInk, "catching her sleeve and asking her to go back",
                "The residents Ink must not repeat the old Sara contradiction");
        omits(residentsInk, "I remember her carrying me",
                "Toma must not remember an action absent from the rescue scene");

        contains(system, "isDomainDerelictBattlestation",
                "The authored station must expose an exact predicate");
        contains(plugin, "ChiefNavigatorDerelictBattlestation",
                "The campaign plugin must route the derelict to rules");
        contains(rules, "chiefNavigatorBattlestationConclusion,",
                "The battlestation investigation must have a conclusion");
        for (String revised : new String[] {
                "Upon boarding, your operations team finds residual power",
                "The empty emplacements tell a clear story.",
                "reactor shielding was cut into plates for supply transports",
                "ships assigned to a departure column.",
                "The destination: the Persean Sector.",
                "Estimated time: According to Sinni, far too optimistic.",
                "The next automated request is due in another fifty-eight cycles."}) {
            contains(rules, revised, "The revised battlestation prose must be wired in");
            contains(battlestationInk, revised, "The Ink must mirror the user's revised prose");
        }
        omits(battlestationInk, "CREW RELEASED TO EVACUATION DETAIL",
                "The habitation deck must leave the final ration entry blank");
        omits(rules, "CREW RELEASED TO EVACUATION DETAIL",
                "The removed evacuation label must not remain in runtime dialogue");
        omits(rules, "ChiefNavigatorDerelictBattlestationOptions",
                "The battlestation inspection must have no branching menu");
        omits(battlestationInk, "=== inspection_hub ===",
                "The Ink must follow the single linear inspection");
        for (String area : new String[] {"Hull", "Hab", "Command"}) {
            omits(rules, "$chiefNavigatorBattlestation" + area + "Inspected",
                    "Legacy area flags must not skip or gate linear pages");
        }
        String battlestationCommit = "$chiefNavigatorBattlestationConcluded = true";
        check(rules.indexOf(battlestationCommit) == rules.lastIndexOf(battlestationCommit),
                "The inspection must have only one completion endpoint");
        contains(rules,
                "chiefNavigatorBattlestationLeave,DialogOptionSelected,"
                        + "$option == chief_navigator_battlestation_leave,\""
                        + battlestationCommit + "\nDismissDialog\"",
                "Only leaving the final page may complete the inspection");
        contains(rules, "shielding cut too close to minimum tolerance",
                "The investigation must give a physical reason salvage is unsafe");
        contains(rules, "identification beacon sweeps across your receiver",
                "The battlestation must end on its still-active challenge");
        contains(battlestationInk,
                "What remains of the reactor is locked into the stationkeeping grid",
                "The battlestation Ink source must mirror the no-salvage finding");
        omits(rules, "ledger entry written in steel",
                "The battlestation must not explain its evidence through a ledger metaphor");
        omits(rules, "SetMemory ",
                "Runtime state must use rules assignments, not a nonexistent command");

        for (String ink : new String[] {
                "dialogue/menelaus_helen_final.ink",
                "dialogue/voss_requisition.ink",
                "dialogue/ithaca_residents.ink",
                "dialogue/derelict_battlestation.ink"}) {
            check(Files.isRegularFile(ROOT.resolve(ink)),
                    "Missing synchronized Ink source: " + ink);
        }

        System.out.println("Narrative dialogue regression checks passed.");
    }

    private static String read(String path) throws Exception {
        return Files.readString(ROOT.resolve(path), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
    }
}
