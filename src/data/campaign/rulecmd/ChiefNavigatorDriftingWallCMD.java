package data.campaign.rulecmd;

import chiefnavigator.quest.DriftingWallEncounter;
import chiefnavigator.quest.DriftingWallLaborSupport;
import chiefnavigator.quest.MenelausTrial;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;
import java.util.List;
import java.util.Map;

/** Rules-driven Drifting Wall state and combat side effects. */
public final class ChiefNavigatorDriftingWallCMD extends BaseCommandPlugin {
    @Override
    public boolean execute(
            String ruleId,
            final InteractionDialogAPI dialog,
            List<Token> params,
            final Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || params.isEmpty()) return false;
        String action = params.get(0).getString(memoryMap);
        final SectorEntityToken station = dialog.getInteractionTarget();
        if ("supportAvailable".equals(action)) {
            return DriftingWallLaborSupport.supportAvailable(station);
        }
        if ("support".equals(action)) {
            return DriftingWallLaborSupport.grant(station);
        }
        if ("stage".equals(action) && params.size() > 1) {
            String stage = params.get(1).getString(memoryMap);
            float repair = DriftingWallEncounter.getRepairDaysRemaining(station);
            if ("friendly".equals(stage)) return MenelausTrial.isWallFriendly();
            if ("repairing".equals(stage)) {
                return !MenelausTrial.isWallFriendly() && repair > 0f;
            }
            if ("early".equals(stage)) {
                return repair <= 0f
                        && MenelausTrial.canAssaultWall()
                        && !MenelausTrial.isWallLaborActive();
            }
            if ("ready".equals(stage)) {
                return repair <= 0f
                        && MenelausTrial.canAssaultWall()
                        && MenelausTrial.isWallLaborActive();
            }
            if ("locked".equals(stage)) {
                return false;
            }
            return false;
        }
        if ("init".equals(action)) {
            MemoryAPI local = memoryMap.get(MemKeys.LOCAL);
            if (local != null) {
                int days = Math.max(1, (int) Math.ceil(
                        DriftingWallEncounter.getRepairDaysRemaining(station)));
                local.set("$chiefNavigatorWallRepairDays", days, 0f);
            }
            return true;
        }
        if (!"engage".equals(action)) return false;

        final CampaignFleetAPI proxy =
                DriftingWallEncounter.createCombatProxy(station);
        if (proxy == null) return false;

        FleetInteractionDialogPluginImpl.FIDConfig config =
                createCombatConfig(station, proxy);

        FleetInteractionDialogPluginImpl plugin =
                new FleetInteractionDialogPluginImpl(config);
        dialog.setInteractionTarget(proxy);
        dialog.setPlugin(plugin);
        plugin.init(dialog);
        return true;
    }

    static FleetInteractionDialogPluginImpl.FIDConfig createCombatConfig(
            final SectorEntityToken station,
            final CampaignFleetAPI proxy) {
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
        // Friendly drones are independent system patrols, not
        // an assault force that swarms the hostile Wall.
        config.pullInAllies = false;
        config.pullInEnemies = false;
        config.pullInStations = false;
        config.showPullInText = true;
        config.lootCredits = false;
        // Let the FID render its ordinary side-by-side fleet preview and
        // engagement choices. Setting this to true makes init() immediately
        // select ENGAGE, bypassing that screen entirely.
        config.straightToEngage = false;
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
                        DriftingWallEncounter.finishCombat(station, proxy);
                        dialog.dismiss();
                    }
                };

        return config;
    }
}
