package chiefnavigator.quest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Static contract checks for FOB Ithaca's marketless station UI. */
public final class FobIthacaStationRegression {
    private static final Path ROOT = Path.of("").toAbsolutePath();

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void contains(String text, String expected, String message) {
        check(text.contains(expected), message + ": " + expected);
    }

    public static void main(String[] args) throws Exception {
        String system = read(
                "src/chiefnavigator/quest/OdysseyExpanseSystem.java");
        String contacts = read(
                "src/chiefnavigator/quest/FobIthacaContacts.java");
        String plugin = read(
                "src/chiefnavigator/quest/TreadmillCampaignPlugin.java");
        String station = read(
                "src/chiefnavigator/quest/FobIthacaStationInteraction.java");
        String directory = read(
                "src/chiefnavigator/quest/FobIthacaCommDirectoryDialog.java");
        String contact = read(
                "src/chiefnavigator/quest/FobIthacaContactInteraction.java");
        String rules = read("data/campaign/rules.csv");

        contains(system, "chief_navigator_fob_ithaca_storage_market",
                "The retired market id must remain available for migration");
        contains(system, "entityCargo.addAll(storageCargo)",
                "Legacy market storage must migrate back to entity cargo");
        contains(system, "FobIthacaStorageDialog.getStoredCargo(station)",
                "Service setup must share the persistent cargo even when native entity cargo is null");
        contains(system, "getEconomy().removeMarket(market)",
                "The legacy market must be unregistered from the economy");
        contains(system, "station.setMarket(null)",
                "The station must end every repair pass without a market");
        contains(system, "station.setFreeTransfer(true)",
                "Entity cargo must provide free persistent storage");
        check(!system.contains("createMarket(\n"
                        + "                        FOB_ITHACA_MARKET_ID"),
                "Ithaca must never recreate its retired market");

        contains(plugin, "new FobIthacaStationInteraction()",
                "The campaign plugin must own Ithaca's later interactions");
        contains(station, "CoreUITabId.FLEET",
                "The custom station must expose fleet management");
        contains(station, "CoreUITabId.CARGO",
                "The custom station must expose cargo storage");
        contains(station, "CoreUITabId.REFIT",
                "The custom station must expose refitting");
        contains(station, "FobIthacaStorageDialog.show(",
                "Every station core tab must open with real vanilla storage");
        check(!station.contains(".showCore("),
                "Station access must not bypass the shared storage carrier");
        contains(directory, "Global.getFactory().createMarket(",
                "The native directory needs a transient carrier market");
        contains(directory, "dialog.showCommDirectoryDialog(directory)",
                "The station must open Starsector's native comm directory");
        check(!directory.contains("dialog.showCustomDialog("),
                "The retired custom directory overlay must stay removed");
        check(!directory.contains("setPrimaryEntity")
                        && !directory.contains("addMarket("),
                "The carrier must not become a primary entity or economy market");
        contains(directory, "station.setMarket(carrier)",
                "Vanilla's directory constructor must see the carrier market");
        contains(directory, "station.setMarket(previous)",
                "The carrier market must be detached immediately afterward");
        check(!directory.contains("carrier.addPerson(person)"),
                "The transient carrier must not rewrite resident markets");

        check(!contacts.contains("addUnique(result, SinniContact")
                        && !rules.contains("chiefNavigatorIthacaSinniGreeting"),
                "Sinni must not appear in Ithaca's contact directory");
        contains(contacts, "MenelausTrial.getOrCreateMenelaus()",
                "Menelaus must remain in the authored directory roster");
        contains(contacts, "getOrCreateSpartanCaptain()",
                "Aias Kleon must remain in the authored directory roster");
        contains(contacts, "captain.setPostId(Ranks.POST_BASE_COMMANDER)",
                "Aias Kleon must own Ithaca's base-command post");
        contains(contact, "memoryMap.put(MemKeys.LOCAL",
                "Contact rules must receive the selected person's memory");
        contains(contact, "FobIthacaStationInteraction.returnToDirectory(dialog)",
                "Leaving a contact must restore the directory");
        contains(station, "extends ChoicePreservingDialogPlugin implements RuleBasedDialog",
                "The native directory only activates people for rule dialogs");
        contains(station, "public void notifyActivePersonChanged()",
                "Native person selections must update authored contact memory");
        contains(station, "activeContact.getMemoryWithoutUpdate()",
                "Contact rules must receive the native active person's memory");

        contains(rules,
                "chiefNavigatorFobIthacaStation,ChiefNavigatorFobIthacaStation",
                "The station root must remain rules-authored");
        contains(rules,
                "chief_navigator_ithaca_menelaus_labors:Discuss the Labors.",
                "Menelaus must expose the quest from his contact entry");
        contains(rules,
                "chiefNavigatorFobIthacaApproachComplete,"
                        + "ChiefNavigatorFobIthacaApproach_complete,,"
                        + "FireAll ChiefNavigatorFobIthaca",
                "The first approach must still flow into Menelaus's intro");

        System.out.println(
                "FOB Ithaca marketless-station regression checks passed.");
    }

    private static String read(String path) throws Exception {
        return Files.readString(ROOT.resolve(path), StandardCharsets.UTF_8);
    }
}
