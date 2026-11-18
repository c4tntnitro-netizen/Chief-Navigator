package chiefnavigator.quest;

import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.*;
import com.fs.starfarer.api.characters.*;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.campaign.rules.Memory;
import data.campaign.rulecmd.ChiefNavigatorIthacaIsaCMD;
import java.util.*;
import static chiefnavigator.quest.MenelausFlowRegression.mock;

/** Current-roster contact identity, both native shells and exact inert placeholder. */
public final class IthacaIsaContactRegression {
    private static final String OPTION = "chief_navigator_ithaca_isa_upgrade";
    private static final String BACK = "chief_navigator_ithaca_directory_back";
    private static final String TODO = "Isa's engineering skills adds an extra upgrade to ithaca's naval lasers. TODO";
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static Map<String, Object> snapshot(MemoryAPI memory) {
        Map<String, Object> result = new HashMap<>();
        for (String key : memory.getKeys()) result.put(key, memory.get(key));
        return result;
    }

    public static void main(String[] args) throws Exception {
        SectorAPI oldSector = Global.getSector(); FactoryAPI oldFactory = Global.getFactory();
        SettingsAPI oldSettings = Global.getSettings();
        try {
            IthacaPrototypeRewardRegression.initializeNativeRules();
            IthacaPrototypeRewardRegression.Fixture f = new IthacaPrototypeRewardRegression.Fixture();
            Memory isaMemory = new Memory();
            PersonAPI isa = mock(PersonAPI.class, (name, values) -> {
                if (name.equals("getId")) return "ship_trophy_isa";
                if (name.equals("getMemoryWithoutUpdate") || name.equals("getMemory")) return isaMemory;
                if (name.equals("getNameString")) return "Isa Leicester";
                if (name.equals("getPortraitSprite")) return "graphics/portraits/ship_trophy_isa.png";
                if (name.startsWith("set")) throw new AssertionError("Ithaca must not rewrite Isa's officer record: " + name);
                return null;
            });
            f.install();
            f.base.global.set("$ship_trophy_room_isa_officer_granted", true);
            check(FobIthacaContacts.getRosterIsa() == null
                            && !FobIthacaContacts.getDirectoryPeople().contains(isa),
                    "Past recruitment alone does not put Isa in the current directory");
            f.officers.add(mock(OfficerDataAPI.class, (n, a) -> n.equals("getPerson") ? f.base.aias : null));
            check(FobIthacaContacts.getRosterIsa() == null, "Other officers cannot stand in for Isa");
            f.officers.add(mock(OfficerDataAPI.class, (n, a) -> n.equals("getPerson") ? isa : null));
            check(FobIthacaContacts.getRosterIsa() == isa
                            && Collections.frequency(FobIthacaContacts.getDirectoryPeople(), isa) == 1,
                    "Directory borrows the exact roster officer once without a replacement person");
            f.open(isa);
            check(f.base.options.keySet().equals(Set.of(OPTION, BACK)), "Exactly one topic plus standard directory navigation");
            Map<String, Object> before = snapshot(f.base.global);
            int paragraphs = f.base.history.size();
            f.choose(OPTION);
            List<String> added = f.base.history.subList(paragraphs, f.base.history.size()).stream()
                    .filter(text -> text != null && !text.isBlank() && !text.equals("Discuss Ithaca's naval lasers.")).toList();
            check(added.equals(List.of(TODO)), "Selecting the topic displays only the exact supplied placeholder");
            check(f.base.options.keySet().equals(Set.of(OPTION, BACK)) && before.equals(snapshot(f.base.global))
                            && f.factories == 0 && f.members.isEmpty(), "Placeholder has no upgrade, reward, or quest effects");
            f.open(isa); f.choose(OPTION);
            check(before.equals(snapshot(f.base.global)), "Reopening and repeating remain inert");

            // Exercise the actual marketless station plugin used by the native comm directory.
            f.install();
            FobIthacaStationInteraction station = new FobIthacaStationInteraction();
            f.base.plugin = station; station.init(f.base.dialog);
            f.base.active = isa; station.notifyActivePersonChanged();
            f.base.options.clear();
            check(station.getContext() == isa && FireBest.fire(null, f.base.dialog, station.getMemoryMap(), "PickGreeting")
                            && f.base.options.keySet().equals(Set.of(OPTION, BACK)),
                    "Native directory selection opens the same isolated Isa menu");
            station.optionSelected(f.base.options.get(OPTION), OPTION);
            check(f.base.history.contains(TODO) && f.base.options.keySet().equals(Set.of(OPTION, BACK)),
                    "Native station contact dispatch reaches the exact placeholder");
            station.optionSelected(f.base.options.get(BACK), BACK);
            check(f.base.directoryOpens == 1, "The standard return option keeps the directory usable");

            f.officers.clear();
            check(FobIthacaContacts.getRosterIsa() == null && !FobIthacaContacts.getDirectoryPeople().contains(isa)
                            && !FobIthacaContacts.isOwnedIthacaContact(isa), "Removing Isa from the roster removes this contact");
            InteractionDialogPlugin stale = mock(InteractionDialogPlugin.class, (n, a) -> n.equals("getContext") ? isa : null);
            InteractionDialogAPI dialog = mock(InteractionDialogAPI.class, (n, a) -> n.equals("getPlugin") ? stale
                    : n.equals("getInteractionTarget") ? f.base.station : null);
            check(!new ChiefNavigatorIthacaIsaCMD().execute("stale", dialog, List.of(),
                            Map.of(MemKeys.LOCAL, isaMemory)), "Stale Isa menus cannot bypass current-roster availability");
            System.out.println("PASS: roster-only exact Isa contact, isolated single topic, exact TODO, no gameplay effects, "
                    + "both native dialog shells, repeat/reopen, usable return and removal/stale-menu gates");
        } finally { Global.setSector(oldSector); Global.setFactory(oldFactory); Global.setSettings(oldSettings); }
    }
}
