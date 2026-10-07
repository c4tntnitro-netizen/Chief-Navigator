package data.campaign.rulecmd;

import chiefnavigator.quest.MenelausTrial;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;
import java.util.List;
import java.util.Map;

/** Rules-side interaction and vanilla-style battle for a fixed mothership POI. */
public final class ChiefNavigatorMothershipCMD extends BaseCommandPlugin {
    @Override
    public boolean execute(
            String ruleId,
            final InteractionDialogAPI dialog,
            List<Token> params,
            final Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || params.isEmpty()) return false;
        String action = params.get(0).getString(memoryMap);
        final SectorEntityToken poi = dialog.getInteractionTarget();

        if ("stage".equals(action) && params.size() > 1) {
            String stage = params.get(1).getString(memoryMap);
            if ("active".equals(stage)) {
                return MenelausTrial.canAssaultMothership(poi);
            }
            if ("friendly".equals(stage)) {
                return MenelausTrial.isFriendlyMothership(poi);
            }
            if ("locked".equals(stage)) {
                return MenelausTrial.isMothershipPOI(poi)
                        && !MenelausTrial.isFriendlyMothership(poi)
                        && !MenelausTrial.isMothershipLaborActive();
            }
            return false;
        }
        if ("init".equals(action)) {
            return MenelausTrial.isMothershipPOI(poi);
        }
        if (!"engage".equals(action)) return false;

        final CampaignFleetAPI proxy =
                MenelausTrial.createMothershipCombatProxy(poi);
        if (proxy == null) return false;

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
        config.pullInEnemies = false;
        config.pullInStations = false;
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
                    }

                    @Override
                    public void notifyLeave(InteractionDialogAPI dialog) {
                        MenelausTrial.finishMothershipCombat(poi, proxy);
                        dialog.dismiss();
                    }
                };

        FleetInteractionDialogPluginImpl plugin =
                new FleetInteractionDialogPluginImpl(config);
        dialog.setInteractionTarget(proxy);
        dialog.setPlugin(plugin);
        plugin.init(dialog);
        return true;
    }
}
