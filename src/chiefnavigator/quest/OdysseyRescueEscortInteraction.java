package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll;
import java.util.Map;

/** Rules-backed supply-or-fight encounter; historical class name retained. */
public final class OdysseyRescueEscortInteraction
        extends ChoicePreservingDialogPlugin {
    private static final String RULE_PREFIX = "ChiefNavigatorExiles_";
    private static final String AID = "exile_aid";
    private static final String ENGAGE = "exile_engage";

    private InteractionDialogAPI dialog;
    private CampaignFleetAPI survivors;
    private Map<String, MemoryAPI> memoryMap;

    @Override
    protected void initConversation(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        survivors = (CampaignFleetAPI) dialog.getInteractionTarget();
        memoryMap = RuleDialogSupport.createMemoryMap(dialog);
        if (survivors.getCommander() != null) {
            dialog.getVisualPanel().showPersonInfo(
                    survivors.getCommander(), true);
        } else {
            dialog.getVisualPanel().showFleetInfo(
                    survivors.getName(), survivors, null, null);
        }

        fire(ExileSupplyAid.hasReceivedAid(survivors)
                ? "underway" : "contact");
    }

    @Override
    protected void handleOptionSelected(String optionText, Object optionData) {
        if (AID.equals(optionData)) {
            fire(ExileSupplyAid.donateSupplies(
                    survivors, OdysseyExpanseSystem.getFobIthaca())
                    ? "aid" : "contact");
        } else if (ENGAGE.equals(optionData)) {
            engage();
        } else if (optionData != null) {
            fire(optionData.toString());
        }
    }

    private void fire(String stage) {
        memoryMap.get(MemKeys.LOCAL).set("$chiefNavigatorExileCanAid",
                ExileSupplyAid.canDonate(
                        survivors, OdysseyExpanseSystem.getFobIthaca()));
        if (!RuleDialogSupport.fire(dialog, memoryMap, RULE_PREFIX + stage)) {
            Global.getLogger(OdysseyRescueEscortInteraction.class).warn(
                    "Missing exile dialogue stage: " + stage);
            dialog.dismiss();
        } else if ("contact".equals(stage)) {
            // Run after the rules have added the options.
            FireAll.fire(null, dialog, memoryMap, "ChiefNavigatorExilesAvailability");
        }
    }

    private void engage() {
        FleetInteractionDialogPluginImpl.FIDConfig config =
                new FleetInteractionDialogPluginImpl.FIDConfig();
        config.leaveAlwaysAvailable = false;
        config.showCommLinkOption = false;
        config.showWarningDialogWhenNotHostile = false;
        config.firstTimeEngageOptionText = "Open fire";
        config.alwaysAttackVsAttack = true;
        config.impactsAllyReputation = false;
        config.impactsEnemyReputation = false;
        config.straightToEngage = true;
        config.delegate = new FleetInteractionDialogPluginImpl.BaseFIDDelegate() {
            @Override
            public void battleContextCreated(
                    InteractionDialogAPI dialog,
                    BattleCreationContext context) {
                context.aiRetreatAllowed = true;
                context.enemyDeployAll = true;
            }
        };
        FleetInteractionDialogPluginImpl plugin =
                new FleetInteractionDialogPluginImpl(config);
        dialog.setPlugin(plugin);
        plugin.init(dialog);
    }

    @Override public void optionMousedOver(String text, Object data) { }
    @Override public void advance(float amount) { }
    @Override public void backFromEngagement(EngagementResultAPI result) { }
    @Override public Object getContext() { return survivors; }
    @Override public Map<String, MemoryAPI> getMemoryMap() { return memoryMap; }
}
