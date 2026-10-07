package data.campaign.rulecmd;

import chiefnavigator.quest.OdysseyExpanseSystem;
import chiefnavigator.quest.PostLaborHuntIntel;
import chiefnavigator.quest.PostLaborHuntIntel.Target;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;
import java.util.List;
import java.util.Map;

/** Location questions are confined to Kleon's existing Ithaca conversation. */
public final class ChiefNavigatorPostLaborHuntCMD extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog,
            List<Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || params.isEmpty() || dialog.getPlugin() == null
                || !OdysseyExpanseSystem.isFobIthaca(dialog.getInteractionTarget())) return false;
        Object context = dialog.getPlugin().getContext();
        if (!(context instanceof PersonAPI)) return false;
        PersonAPI person = (PersonAPI) context;
        MemoryAPI local = memoryMap.get(MemKeys.LOCAL);
        if (!PostLaborHuntIntel.isKleon(person)
                || local != person.getMemoryWithoutUpdate()) return false;
        String action = params.get(0).getString(memoryMap);
        if ("budaiAvailable".equals(action)) return PostLaborHuntIntel.isAvailable(Target.BUDAI);
        if ("ungaikyoAvailable".equals(action)) return PostLaborHuntIntel.isAvailable(Target.UNGAIKYO);
        if ("briefBudai".equals(action)
                && "chief_navigator_ithaca_hunt_budai".equals(local.getString("$option"))) {
            return PostLaborHuntIntel.brief(Target.BUDAI, dialog.getTextPanel());
        }
        if ("briefUngaikyo".equals(action)
                && "chief_navigator_ithaca_hunt_ungaikyo".equals(local.getString("$option"))) {
            return PostLaborHuntIntel.brief(Target.UNGAIKYO, dialog.getTextPanel());
        }
        return false;
    }
}
