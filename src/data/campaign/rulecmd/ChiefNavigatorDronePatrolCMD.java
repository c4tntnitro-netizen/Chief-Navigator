package data.campaign.rulecmd;

import chiefnavigator.quest.DronePatrolRecruitment;
import chiefnavigator.quest.MenelausTrial;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.SetStoryOption;
import com.fs.starfarer.api.impl.campaign.rulecmd.SetStoryOption.BaseOptionStoryPointActionDelegate;
import com.fs.starfarer.api.impl.campaign.rulecmd.SetStoryOption.StoryOptionParams;
import com.fs.starfarer.api.util.Misc.Token;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Rules own the prose; native story-point confirmation owns payment. */
public final class ChiefNavigatorDronePatrolCMD extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog,
            List<Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || params.isEmpty()
                || !MenelausTrial.isFriendlyDronePatrol(
                        dialog.getInteractionTarget())) return false;
        CampaignFleetAPI patrol = (CampaignFleetAPI) dialog.getInteractionTarget();
        String action = params.get(0).getString(memoryMap);
        if ("hasShips".equals(action)) {
            return !DronePatrolRecruitment.getEligibleMembers(patrol).isEmpty();
        }
        MemoryAPI local = memoryMap.get(MemKeys.LOCAL);
        if (local == null) return false;
        if ("init".equals(action)) {
            local.set("$chiefNavigatorDroneCount",
                    DronePatrolRecruitment.getEligibleMembers(patrol).size(), 0f);
            local.set("$chiefNavigatorDronePatrolName", patrol.getName(), 0f);
            local.set("$chiefNavigatorDroneRecruited", false, 0f);
            dialog.getVisualPanel().showFleetInfo(null, patrol, null, null);
            return true;
        }
        if ("configureStoryOption".equals(action) && params.size() >= 3) {
            String optionId = params.get(1).getString(memoryMap);
            if (!dialog.getOptionPanel().hasOption(optionId)) return false;
            String logText = params.get(2).getStringWithTokenReplacement(
                    params.get(2).getString(memoryMap), dialog, memoryMap);
            // Reassignment grants a permanent ship, not bonus experience.
            StoryOptionParams story = new StoryOptionParams(optionId, 1,
                    "historianBP", "technology", logText);
            return SetStoryOption.set(dialog, story,
                    new RecruitmentAction(dialog, story, patrol, local));
        }
        return false;
    }

    static final class RecruitmentAction
            extends BaseOptionStoryPointActionDelegate {
        private final CampaignFleetAPI patrol;
        private final MemoryAPI local;
        private FleetMemberAPI selected;
        private boolean payable = true;
        private boolean prepared;
        private boolean resolved;

        RecruitmentAction(InteractionDialogAPI dialog, StoryOptionParams story,
                CampaignFleetAPI patrol, MemoryAPI local) {
            super(dialog, story);
            this.patrol = patrol;
            this.local = local;
        }

        @Override
        public void preConfirm() {
            if (resolved) {
                payable = false;
                return;
            }
            selected = null;
            prepared = true;
            payable = Global.getSector() != null
                    && Global.getSector().getPlayerStats().getStoryPoints() >= 1;
            if (payable) {
                selected = DronePatrolRecruitment.chooseRandom(
                        patrol, new Random());
                payable = selected != null;
            }
            local.set("$chiefNavigatorDroneRecruited", false, 0f);
            // Native confirmation calls this once and suppresses the later
            // rule-based callback's echo. Do not add a second selected line.
            super.preConfirm();
        }

        @Override
        public int getRequiredStoryPoints() {
            // Native UI re-reads this after preConfirm and before charging.
            return payable && !resolved ? 1 : 0;
        }

        @Override
        public void confirm() {
            if (!prepared || resolved) return;
            resolved = true;
            if (!payable) return;
            boolean transferred = false;
            try {
                transferred = DronePatrolRecruitment.transfer(patrol, selected);
            } catch (RuntimeException failure) {
                Global.getLogger(ChiefNavigatorDronePatrolCMD.class).error(
                        "Unable to reassign friendly patrol drone", failure);
            }
            if (!transferred) {
                // There is no bonus XP to unwind; refund exactly the one point
                // native confirmation has just spent if the transfer failed.
                Global.getSector().getPlayerStats().addStoryPoints(1);
                return;
            }
            local.set("$chiefNavigatorDroneShipName", selected.getShipName(), 0f);
            local.set("$chiefNavigatorDroneRecruited", true, 0f);
            dialog.getVisualPanel().showFleetMemberInfo(selected, false);
            // The native original-option callback renders the result rule.
        }
    }
}
