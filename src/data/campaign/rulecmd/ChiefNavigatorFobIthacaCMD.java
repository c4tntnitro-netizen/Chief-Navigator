package data.campaign.rulecmd;

import chiefnavigator.quest.IthacaSectionEncounter;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll;
import com.fs.starfarer.api.util.Misc.Token;
import java.util.List;
import java.util.Map;

/** Rules-driven FOB Ithaca section assault side effects. */
public final class ChiefNavigatorFobIthacaCMD extends BaseCommandPlugin {
    @Override
    public boolean execute(
            String ruleId,
            final InteractionDialogAPI dialog,
            List<Token> params,
            final Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || params.isEmpty()) return false;
        String action = params.get(0).getString(memoryMap);
        if ("leave".equals(action)) {
            dialog.dismissAsCancel();
            return true;
        }
        if (!"engage".equals(action) || params.size() < 2) return false;

        final IthacaSectionEncounter.Section section =
                IthacaSectionEncounter.Section.fromId(
                        params.get(1).getString(memoryMap));
        final SectorEntityToken station = dialog.getInteractionTarget();
        if (section == null || station == null) return false;
        if (IthacaSectionEncounter.isDisabled(station, section)) {
            FireAll.fire(
                    null,
                    dialog,
                    memoryMap,
                    "ChiefNavigatorFobIthacaAlreadyDisabled");
            return true;
        }

        final CampaignFleetAPI sectionStation =
                IthacaSectionEncounter.prepareForCombat(station, section);
        if (sectionStation == null) return false;

        FleetInteractionDialogPluginImpl.FIDConfig config =
                new FleetInteractionDialogPluginImpl.FIDConfig();
        config.leaveAlwaysAvailable = true;
        config.showCommLinkOption = false;
        config.showEngageText = false;
        config.showFleetAttitude = false;
        config.showTransponderStatus = false;
        config.showWarningDialogWhenNotHostile = false;
        config.alwaysAttackVsAttack = true;
        config.impactsAllyReputation = false;
        config.impactsEnemyReputation = false;
        config.pullInAllies = false;
        // Every selectable bastion is a station-mode defender, not a lone
        // ship encounter. Preserve normal station reinforcement behavior so
        // nearby hostile defenders and station forces can join its battle.
        config.pullInEnemies = true;
        config.pullInStations = true;
        config.showPullInText = true;
        config.playerAttackingStation = true;
        config.lootCredits = false;
        config.straightToEngage = true;
        config.dismissOnLeave = false;
        config.printXPToDialog = true;
        config.noSalvageLeaveOptionText = "Continue";
        config.delegate =
                new FleetInteractionDialogPluginImpl.BaseFIDDelegate() {
                    @Override
                    public void battleContextCreated(
                            InteractionDialogAPI dialog,
                            BattleCreationContext context) {
                        context.aiRetreatAllowed = false;
                        context.enemyDeployAll = true;
                        context.fightToTheLast = true;
                        context.objectivesAllowed = false;
                    }

                    @Override
                    public void notifyLeave(InteractionDialogAPI dialog) {
                        IthacaSectionEncounter.finishCombat(
                                station, sectionStation, section);
                        dialog.dismiss();
                    }
                };

        FleetInteractionDialogPluginImpl plugin =
                new FleetInteractionDialogPluginImpl(config);
        dialog.setInteractionTarget(sectionStation);
        dialog.setPlugin(plugin);
        plugin.init(dialog);
        return true;
    }
}
