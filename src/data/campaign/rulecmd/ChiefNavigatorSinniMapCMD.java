package data.campaign.rulecmd;

import chiefnavigator.quest.OdysseyExpanseSystem;
import chiefnavigator.quest.OrionKnotMapDialog;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;
import java.util.List;
import java.util.Map;

/** Opens Sinni's full-size Orion Knot chart from her Contacts entry. */
public final class ChiefNavigatorSinniMapCMD extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog,
            List<Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || params.isEmpty()
                || !"show".equals(params.get(0).getString(memoryMap))) {
            return false;
        }
        if (Global.getSector() == null
                || !Global.getSector().getMemoryWithoutUpdate().getBoolean(
                        OdysseyExpanseSystem.ENTERED)) {
            dialog.dismissAsCancel();
            return false;
        }
        OrionKnotMapDialog.show(dialog);
        return true;
    }
}
