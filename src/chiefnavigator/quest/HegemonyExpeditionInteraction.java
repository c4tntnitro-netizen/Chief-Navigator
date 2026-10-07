package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import java.util.Map;

/** Rules-authored coercive resupply demand from the stranded expedition. */
public final class HegemonyExpeditionInteraction
        extends ChoicePreservingDialogPlugin {
    private static final String OPENING_TRIGGER =
            "ChiefNavigatorVossRequisition";
    private static final String GRACE_TRIGGER =
            "ChiefNavigatorVossRequisitionGrace";
    private static final String PAID_TRIGGER =
            "ChiefNavigatorVossRequisitionPaid";
    private static final String PAY =
            "chief_navigator_voss_requisition_pay";
    private static final String CANNOT_PAY =
            "chief_navigator_voss_requisition_cannot_pay";
    private static final String REFUSE =
            "chief_navigator_voss_requisition_refuse";
    private static final String ENGAGE =
            "chief_navigator_voss_requisition_engage";
    private static final String CONTINUE =
            "chief_navigator_voss_requisition_continue";
    private static final String CAN_PAY =
            "$chiefNavigatorVossCanPay";

    private InteractionDialogAPI dialog;
    private CampaignFleetAPI expedition;
    private Map<String, MemoryAPI> memoryMap;

    @Override
    protected void initConversation(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        this.expedition = (CampaignFleetAPI) dialog.getInteractionTarget();
        this.memoryMap = RuleDialogSupport.createMemoryMap(dialog);
        MemoryAPI local = memoryMap.get(MemKeys.LOCAL);
        local.set("$chiefNavigatorVossHostile", HegemonyExpeditionFaction.isHostile());
        local.set("$chiefNavigatorVossPlayerTitle", "Captain");
        local.set("$chiefNavigatorVossPlayerName",
                Global.getSector().getPlayerPerson().getNameString());
        local.set("$chiefNavigatorVossOutmatched",
                expedition.getFleetPoints() > Global.getSector().getPlayerFleet().getFleetPoints() * 1.5f);

        PersonAPI voss = OdysseyStrandedFleetsScript.getOrCreateVoss();
        if (voss != null) {
            dialog.getVisualPanel().showPersonInfo(voss, true);
        } else {
            dialog.getVisualPanel().showFleetInfo(
                    "Hegemony expedition", expedition, null, null);
        }

        local.set("$chiefNavigatorVossGrace", expedition.getMemoryWithoutUpdate().getBoolean(
                OdysseyStrandedFleetsScript.HEGEMONY_TRIBUTE_GRACE));
        updateCanPay();
        RuleDialogSupport.fire(dialog, memoryMap, "ChiefNavigatorVossEntry");
    }

    @Override
    protected void handleOptionSelected(String optionText, Object optionData) {
        if (PAY.equals(optionData)) {
            payTribute();
        } else if (ENGAGE.equals(optionData)) {
            engage();
        } else if (CONTINUE.equals(optionData)) {
            dialog.dismiss();
        } else if (optionData != null) {
            MemoryAPI local = memoryMap.get(MemKeys.LOCAL);
            if (local == null) return;
            local.set("$option", optionData.toString(), 0f);
            RuleDialogSupport.fire(
                    dialog, memoryMap, "DialogOptionSelected");
        }
    }

    private void updateCanPay() {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        CargoAPI cargo = player == null ? null : player.getCargo();
        boolean canPay = cargo != null
                && cargo.getSupplies()
                        >= OdysseyStrandedFleetsScript.HEGEMONY_DEMAND_SUPPLIES
                && cargo.getFuel()
                        >= OdysseyStrandedFleetsScript.HEGEMONY_DEMAND_FUEL;
        MemoryAPI local = memoryMap.get(MemKeys.LOCAL);
        if (local != null) local.set(CAN_PAY, canPay, 0f);
    }

    private void payTribute() {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        CargoAPI cargo = player.getCargo();
        if (cargo.getSupplies()
                        < OdysseyStrandedFleetsScript.HEGEMONY_DEMAND_SUPPLIES
                || cargo.getFuel()
                        < OdysseyStrandedFleetsScript.HEGEMONY_DEMAND_FUEL) {
            MemoryAPI local = memoryMap.get(MemKeys.LOCAL);
            if (local != null) {
                local.set("$option", CANNOT_PAY, 0f);
                RuleDialogSupport.fire(
                        dialog, memoryMap, "DialogOptionSelected");
            }
            return;
        }

        cargo.removeSupplies(
                OdysseyStrandedFleetsScript.HEGEMONY_DEMAND_SUPPLIES);
        cargo.removeFuel(OdysseyStrandedFleetsScript.HEGEMONY_DEMAND_FUEL);
        expedition.getCargo().addSupplies(
                OdysseyStrandedFleetsScript.HEGEMONY_DEMAND_SUPPLIES);
        expedition.getCargo().addFuel(
                OdysseyStrandedFleetsScript.HEGEMONY_DEMAND_FUEL);
        OdysseyStrandedFleetsScript.grantHegemonyTributeGrace(expedition);
        RuleDialogSupport.fire(dialog, memoryMap, PAID_TRIGGER);
    }

    private void engage() {
        FleetInteractionDialogPluginImpl.FIDConfig config =
                new FleetInteractionDialogPluginImpl.FIDConfig();
        config.leaveAlwaysAvailable = false;
        config.showCommLinkOption = false;
        config.showWarningDialogWhenNotHostile = false;
        config.firstTimeEngageOptionText = "Defy the requisition";
        config.alwaysAttackVsAttack = true;
        config.impactsAllyReputation = false;
        config.impactsEnemyReputation = true;
        config.pullInAllies = true;
        config.pullInEnemies = true;
        config.pullInStations = true;
        config.showPullInText = true;
        config.lootCredits = false;
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

    @Override public void optionMousedOver(String optionText, Object optionData) { }
    @Override public void advance(float amount) { }
    @Override public void backFromEngagement(EngagementResultAPI result) { }
    @Override public Object getContext() { return expedition; }
    @Override public Map<String, MemoryAPI> getMemoryMap() { return memoryMap; }
}
