package data.campaign.rulecmd;

import chiefnavigator.quest.SinniBarEvent;
import chiefnavigator.quest.SinniContact;
import chiefnavigator.quest.SinniEventideIntel;
import chiefnavigator.quest.SinniOneMoreHorizonIntel;
import chiefnavigator.topography.SinniHyperspaceTopographyEventIntel;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;
import java.util.List;
import java.util.Map;

/** Side effects for the rules-driven Sinni introduction. */
public final class ChiefNavigatorSinniIntroCMD extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog,
                           List<Token> params, Map<String, MemoryAPI> memoryMap) {
        if (params.isEmpty()) return false;
        String action = params.get(0).getString(memoryMap);
        if ("accept".equals(action)) {
            Global.getSector().getMemoryWithoutUpdate().set(SinniBarEvent.STARTED, true);
            SinniContact.joinPlayerFleet(dialog == null
                    ? null : dialog.getTextPanel());
            SinniHyperspaceTopographyEventIntel.unlockForSinni(dialog);
            SinniEventideIntel.ensureExists(dialog.getTextPanel());
            SinniOneMoreHorizonIntel.ensureExists(dialog.getTextPanel());
            return true;
        }
        if ("brief".equals(action)) {
            SinniEventideIntel.complete(dialog.getTextPanel());
            return true;
        }
        return false;
    }
}
