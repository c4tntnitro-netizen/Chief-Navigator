package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import org.lwjgl.input.Keyboard;

import java.util.Map;

/** The Troy-side wormhole opened after the expedition passes its DP check. */
public final class TroyWormholeInteraction extends ChoicePreservingDialogPlugin {
    private static final String TRIGGER_MAIN =
            "ChiefNavigatorTroyTerminus_main";
    private static final String TRIGGER_INVALID =
            "ChiefNavigatorTroyTerminus_invalid";
    private static final String TRIGGER_TRANSIT =
            "ChiefNavigatorTroyTerminus_transit";
    private static final String ENTER = "chief_navigator_troy_enter";
    private static final String TRANSIT = "chief_navigator_troy_transit";
    private static final String BACK = "chief_navigator_troy_terminus_back";
    private static final String LEAVE = "chief_navigator_troy_terminus_leave";
    private static final String DP_VALUE = "$chiefNavigatorTroyDP";
    private static final String DP_LIMIT = "$chiefNavigatorTroyDPLimit";
    private InteractionDialogAPI dialog;
    private Map<String, MemoryAPI> memoryMap;
    private boolean transitCommitted;

    @Override
    protected void initConversation(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        this.memoryMap = RuleDialogSupport.createMemoryMap(dialog);
        dialog.showVisualPanel();
        dialog.setBackgroundDimAmount(0.4f);
        dialog.getVisualPanel().showImageVisual(
                dialog.getInteractionTarget().getCustomInteractionDialogImageVisual());
        float fleetDP = TroyDepartureInteraction.getFleetDP(
                Global.getSector().getPlayerFleet());
        MemoryAPI local = memoryMap.get(
                com.fs.starfarer.api.campaign.rules.MemKeys.LOCAL);
        local.set(DP_VALUE, TroyDepartureInteraction.formatDP(fleetDP));
        local.set(DP_LIMIT, TroyDepartureInteraction.formatDP(
                TroyDepartureInteraction.MAX_FLEET_DP));
        RuleDialogSupport.fire(dialog, memoryMap, TRIGGER_MAIN);
        if (fleetDP > TroyDepartureInteraction.MAX_FLEET_DP) {
            dialog.getOptionPanel().setEnabled(ENTER, false);
        }
        dialog.getOptionPanel().setShortcut(
                LEAVE, Keyboard.KEY_ESCAPE, false, false, false, false);
    }

    @Override
    protected void handleOptionSelected(String optionText, Object optionData) {
        if (ENTER.equals(optionData)) {
            beginTransit();
        } else if (TRANSIT.equals(optionData)) {
            enterNebula();
        } else if (BACK.equals(optionData)) {
            init(dialog);
        } else if (LEAVE.equals(optionData)) {
            leaveTerminus();
        }
    }

    /** Mirrors vanilla jump-point exit handling for an active plotted course. */
    private void leaveTerminus() {
        Global.getSector().getCampaignUI().setFollowingDirectCommand(true);
        Global.getSector().setPaused(false);
        dialog.dismiss();
    }

    private void beginTransit() {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player == null
                || TroyDepartureInteraction.getFleetDP(player)
                        > TroyDepartureInteraction.MAX_FLEET_DP) {
            RuleDialogSupport.fire(dialog, memoryMap, TRIGGER_INVALID);
            dialog.getOptionPanel().setShortcut(
                    BACK, Keyboard.KEY_ESCAPE, false, false, false, false);
            return;
        }

        dialog.getVisualPanel().fadeVisualOut();
        dialog.hideVisualPanel();
        dialog.setPromptText("");
        dialog.setBackgroundDimAmount(1f);
        RuleDialogSupport.fire(dialog, memoryMap, TRIGGER_TRANSIT);
    }

    private void enterNebula() {
        if (transitCommitted) return;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player == null
                || TroyDepartureInteraction.getFleetDP(player)
                        > TroyDepartureInteraction.MAX_FLEET_DP) {
            RuleDialogSupport.fire(dialog, memoryMap, TRIGGER_INVALID);
            return;
        }

        StarSystemAPI expanse = OdysseyExpanseSystem.findExisting();
        SectorEntityToken entry = OdysseyExpanseSystem.findEntry(expanse);
        if (entry == null) return;
        transitCommitted = true;
        CargoAPI cargo = player.getCargo();
        cargo.removeSupplies(cargo.getSupplies() * 0.5f);
        cargo.removeFuel(cargo.getFuel() * 0.5f);
        SectorEntityToken terminus = dialog.getInteractionTarget();
        SinniSystemVignetteScript.armAlphaOdysseyArrival();
        Global.getSector().getMemoryWithoutUpdate().set(OdysseyExpanseSystem.ENTERED, true);
        OdysseyStrandedFleetsScript.markFirstEntry();
        OdysseyPredatorScript.prepareTroyArrival(expanse);
        WaypointTroyIntel.revealFobIthacaObjective();
        dialog.dismiss();
        Global.getSector().doHyperspaceTransition(
                player,
                terminus,
                new JumpPointAPI.JumpDestination(entry, "The Odyssey Expanse"),
                0.5f);
    }

    @Override public void optionMousedOver(String optionText, Object optionData) { }
    @Override public void advance(float amount) { }
    @Override public void backFromEngagement(EngagementResultAPI battleResult) { }
    @Override public Object getContext() { return null; }
    @Override public Map<String, MemoryAPI> getMemoryMap() { return memoryMap; }
}
