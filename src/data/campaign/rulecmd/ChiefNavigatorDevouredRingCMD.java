package data.campaign.rulecmd;

import chiefnavigator.campaign.DevouredRingEntityPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;
import java.util.List;
import java.util.Map;

/** Records the navigation finding recovered from the unscannable Ring. */
public final class ChiefNavigatorDevouredRingCMD extends BaseCommandPlugin {
    @Override
    public boolean execute(
            String ruleId,
            InteractionDialogAPI dialog,
            List<Token> params,
            Map<String, MemoryAPI> memoryMap) {
        if (params.isEmpty()) return false;
        if ("discover".equals(params.get(0).getString(memoryMap))) {
            DevouredRingEntityPlugin.recordPathInformation();
            return true;
        }
        return false;
    }
}
