package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small rules-engine bridge for custom campaign interaction plugins. */
final class RuleDialogSupport {
    private RuleDialogSupport() { }

    static void preserveChoice(
            InteractionDialogAPI dialog, String optionText, Object optionData) {
        if (dialog == null || optionText == null || optionData == null) return;
        // Match the native rule-based dialog: capture the option while its
        // label still exists, before the next stage clears the option panel.
        dialog.addOptionSelectedText(optionData);
    }

    static Map<String, MemoryAPI> createMemoryMap(
            InteractionDialogAPI dialog) {
        Map<String, MemoryAPI> result =
                new LinkedHashMap<String, MemoryAPI>();
        MemoryAPI global = Global.getSector().getMemoryWithoutUpdate();
        MemoryAPI player = Global.getSector().getPlayerMemoryWithoutUpdate();
        SectorEntityToken target = dialog.getInteractionTarget();
        MemoryAPI entity = target == null
                ? global : target.getMemoryWithoutUpdate();

        result.put(MemKeys.GLOBAL, global);
        result.put(MemKeys.PLAYER, player);
        result.put(MemKeys.ENTITY, entity);
        // These interactions need only short-lived presentation variables.
        // Sharing the target memory avoids depending on an engine-internal
        // Memory implementation while still providing FireBest's local scope.
        result.put(MemKeys.LOCAL, entity);
        if (target != null) {
            FactionAPI faction = target.getFaction();
            // Shared rules resolve reputation operands through $faction.id.
            // The updating getter populates the native transient $id fact.
            if (faction != null) result.put(MemKeys.FACTION, faction.getMemory());
            MarketAPI market = target.getMarket();
            if (market != null) result.put(MemKeys.MARKET, market.getMemory());
        }
        return result;
    }

    static void addPersonFactionMemory(
            Map<String, MemoryAPI> memoryMap, PersonAPI person) {
        memoryMap.remove(MemKeys.PERSON_FACTION);
        if (person != null && person.getFaction() != null) {
            memoryMap.put(MemKeys.PERSON_FACTION, person.getFaction().getMemory());
        }
    }

    static boolean fire(
            InteractionDialogAPI dialog,
            Map<String, MemoryAPI> memoryMap,
            String trigger) {
        dialog.getOptionPanel().clearOptions();
        return FireBest.fire(null, dialog, memoryMap, trigger);
    }
}
