package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import org.lwjgl.input.Keyboard;

import java.util.Map;

/** The Far Threshold at the end of the recycled Troy corridor. */
public final class GoalStationInteraction extends ChoicePreservingDialogPlugin {
    public static final String COMPLETE = "$chief_navigator_treadmill_complete";
    private static final String ENTER = "enter_odyssey_expanse";
    private static final String LEAVE = "leave";
    private InteractionDialogAPI dialog;

    @Override
    protected void initConversation(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        Global.getSector().getMemoryWithoutUpdate().set(COMPLETE, true);
        dialog.getVisualPanel().showImageVisual(
                dialog.getInteractionTarget().getCustomInteractionDialogImageVisual());
        dialog.getTextPanel().addPara(
                "The eastbound road terminates in a plane of depthless blue-black light. "
                        + "Nothing beyond it appears on the Sector map. Sinni studies the moving "
                        + "clouds for several silent seconds, then calls the passage stable.");
        dialog.getTextPanel().addPara(
                "\"This is where our charts stop,\" she says. \"The Odyssey begins on the other side.\"");
        dialog.getOptionPanel().addOption("Cross the Far Threshold", ENTER);
        dialog.getOptionPanel().addOptionConfirmation(
                ENTER,
                "Cross into the uncharted Odyssey Expanse?",
                "Begin the Odyssey",
                "Not yet");
        dialog.getOptionPanel().addOption("Leave", LEAVE);
        dialog.getOptionPanel().setShortcut(LEAVE, Keyboard.KEY_ESCAPE, false, false, false, false);
    }

    @Override protected void handleOptionSelected(String optionText, Object optionData) {
        if (ENTER.equals(optionData)) {
            enterExpanse();
        } else if (LEAVE.equals(optionData)) {
            dialog.dismiss();
        }
    }

    private void enterExpanse() {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        StarSystemAPI expanse = OdysseyExpanseSystem.findExisting();
        SectorEntityToken entry = OdysseyExpanseSystem.findEntry(expanse);
        if (player == null || entry == null) return;

        SinniSystemVignetteScript.armAlphaOdysseyArrival();
        Global.getSector().getMemoryWithoutUpdate().set(OdysseyExpanseSystem.ENTERED, true);
        OdysseyStrandedFleetsScript.markFirstEntry();
        OdysseyPredatorScript.prepareTroyArrival(expanse);
        SectorEntityToken threshold = dialog.getInteractionTarget();
        dialog.dismiss();
        Global.getSector().doHyperspaceTransition(
                player,
                threshold,
                new JumpPointAPI.JumpDestination(entry, "The Odyssey Expanse"),
                0.5f);
        Global.getSector().getCampaignUI().addMessage(
                "The Sector map falls away. The Odyssey Expanse opens ahead.");
    }
    @Override public void optionMousedOver(String optionText, Object optionData) { }
    @Override public void advance(float amount) { }
    @Override public void backFromEngagement(EngagementResultAPI battleResult) { }
    @Override public Object getContext() { return null; }
    @Override public Map<String, MemoryAPI> getMemoryMap() { return null; }
}
