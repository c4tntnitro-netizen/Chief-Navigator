package data.campaign.rulecmd;

import chiefnavigator.quest.MenelausTrial;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;
import java.util.List;
import java.util.Map;

/** A friendly patrol acknowledges the player's IFF without offering ship transfers. */
public final class ChiefNavigatorDronePatrolCMD extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog,
            List<Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || params.isEmpty()
                || !MenelausTrial.isFriendlyDronePatrol(
                        dialog.getInteractionTarget())) return false;
        if (!"init".equals(params.get(0).getString(memoryMap))) return false;
        MemoryAPI local = memoryMap.get(MemKeys.LOCAL);
        if (local == null) return false;
        CampaignFleetAPI patrol = (CampaignFleetAPI) dialog.getInteractionTarget();
        local.set("$chiefNavigatorDronePatrolName", patrol.getName(), 0f);
        dialog.getVisualPanel().showFleetInfo(null, patrol, null, null);
        return true;
    }
}