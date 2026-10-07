package data.campaign.rulecmd;

import chiefnavigator.quest.MenelausSensorLabor;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;
import java.util.List;
import java.util.Map;

/** Rules-side completion command for the Devoured Reach sensor Labor. */
public final class ChiefNavigatorSensorLaborCMD extends BaseCommandPlugin {
    @Override
    public boolean execute(
            String ruleId,
            InteractionDialogAPI dialog,
            List<Token> params,
            Map<String, MemoryAPI> memoryMap) {
        if (params.isEmpty()) return false;
        String action = params.get(0).getString(memoryMap);
        if ("deploy".equals(action)) {
            MenelausSensorLabor.deploy(
                    dialog == null ? null : dialog.getTextPanel());
            return true;
        }
        return false;
    }
}
