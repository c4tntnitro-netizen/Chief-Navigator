package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
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
        return result;
    }

    static boolean fire(
            InteractionDialogAPI dialog,
            Map<String, MemoryAPI> memoryMap,
            String trigger) {
        dialog.getOptionPanel().clearOptions();
        return FireBest.fire(null, dialog, memoryMap, trigger);
    }
}
