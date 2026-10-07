package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.input.Keyboard;

import java.util.Map;

/** Return-side interaction for the wormhole inside the Odyssey Expanse. */
public final class OdysseyExpanseInteraction extends ChoicePreservingDialogPlugin {
    private static final String RETURN = "return_to_troy";
    private static final String LEAVE = "leave";
    private InteractionDialogAPI dialog;

    @Override
    protected void initConversation(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        dialog.getVisualPanel().showImageVisual(
                dialog.getInteractionTarget().getCustomInteractionDialogImageVisual());
        dialog.getTextPanel().addPara(
                "The wormhole holds Troy behind it like a reflection trapped in black glass. "
                        + "Sinni confirms that the passage remains stable enough to retrace, for now.");
        float fleetDP = TroyDepartureInteraction.getFleetDP(
                Global.getSector().getPlayerFleet());
        dialog.getTextPanel().addPara(
                "Return manifest: %s / %s deployment points. The passage enforces the same limit in both directions.",
                Misc.getTextColor(), Misc.getHighlightColor(),
                TroyDepartureInteraction.formatDP(fleetDP),
                TroyDepartureInteraction.formatDP(
                        TroyDepartureInteraction.MAX_FLEET_DP))
                .setHighlightColors(
                        fleetDP > TroyDepartureInteraction.MAX_FLEET_DP
                                ? Misc.getNegativeHighlightColor()
                                : Misc.getPositiveHighlightColor(),
                        Misc.getHighlightColor());
        dialog.getOptionPanel().addOption("Return through the Troy Terminus", RETURN);
        if (fleetDP > TroyDepartureInteraction.MAX_FLEET_DP) {
            dialog.getOptionPanel().setEnabled(RETURN, false);
            dialog.getOptionPanel().setTooltip(
                    RETURN,
                    "The active fleet must be reduced to 50 DP before it can return to Troy.");
        } else {
            dialog.getOptionPanel().addOptionConfirmation(
                    RETURN,
                    "Leave the Odyssey Expanse and return to Waypoint Troy?",
                    "Return to Troy",
                    "Remain here");
        }
        dialog.getOptionPanel().addOption("Leave", LEAVE);
        dialog.getOptionPanel().setShortcut(
                LEAVE, Keyboard.KEY_ESCAPE, false, false, false, false);
    }

    @Override
    protected void handleOptionSelected(String optionText, Object optionData) {
        if (RETURN.equals(optionData)) {
            returnToTroy();
        } else if (LEAVE.equals(optionData)) {
            leaveFoxtrot();
        }
    }

    /**
     * Mirrors vanilla jump-point exit handling so a plotted course cannot
     * immediately retarget the aperture and reopen this dialog.
     */
    private void leaveFoxtrot() {
        Global.getSector().getCampaignUI().setFollowingDirectCommand(true);
        Global.getSector().setPaused(false);
        dialog.dismiss();
    }

    private void returnToTroy() {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        float fleetDP = TroyDepartureInteraction.getFleetDP(player);
        if (player == null
                || fleetDP > TroyDepartureInteraction.MAX_FLEET_DP) {
            dialog.getTextPanel().addPara(
                    "The return aperture rejects the fleet manifest. The active fleet must remain at or below 50 deployment points.",
                    Misc.getNegativeHighlightColor());
            return;
        }
        StarSystemAPI troy = TroyArrivalScript.findWaypointTroy();
        SectorEntityToken terminus =
                TroyArrivalScript.findOdysseyWormhole(troy);
        if (terminus == null) return;

        SectorEntityToken aperture = dialog.getInteractionTarget();
        dialog.dismiss();
        Global.getSector().doHyperspaceTransition(
                player,
                aperture,
                new JumpPointAPI.JumpDestination(terminus, "Troy Terminus"),
                0.5f);
    }

    @Override public void optionMousedOver(String optionText, Object optionData) { }
    @Override public void advance(float amount) { }
    @Override public void backFromEngagement(EngagementResultAPI battleResult) { }
    @Override public Object getContext() { return null; }
    @Override public Map<String, MemoryAPI> getMemoryMap() { return null; }
}
