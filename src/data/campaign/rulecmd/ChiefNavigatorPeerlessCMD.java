package data.campaign.rulecmd;

import chiefnavigator.quest.MenelausTrial;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import peerless.api.PeerlessSkills;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;
import java.util.List;
import java.util.Map;

/** State and gameplay only; all conversation text lives in rules.csv. */
public final class ChiefNavigatorPeerlessCMD extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog,
            List<Token> params, Map<String, MemoryAPI> memoryMap) {
        if (params.isEmpty()) return false;
        String action = params.get(0).getString(memoryMap);
        if ("canReceive".equals(action)) return MenelausTrial.canReceiveDomainEpic();
        if ("hasTitle".equals(action)) return Global.getSector() != null
                && Global.getSector().getMemoryWithoutUpdate().getBoolean(MenelausTrial.DOMAIN_EPIC);
        if ("hasPoint".equals(action)) return PeerlessSkills.getPoints() > 0;
        if ("canSelect".equals(action)) return PeerlessSkills.canTrain(Skills.HELMSMANSHIP);
        if ("selected".equals(action)) return PeerlessSkills.isTrained(Skills.HELMSMANSHIP);
        if ("receive".equals(action)) return dialog != null && MenelausTrial.receiveDomainEpic();
        if ("select".equals(action)) return dialog != null && PeerlessSkills.train(Skills.HELMSMANSHIP);
        return false;
    }
}
