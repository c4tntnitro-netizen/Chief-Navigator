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

/** Foxtrot-style room transfer disguised per side of the connection. */
public final class VeiledSunTransitionInteraction
        extends ChoicePreservingDialogPlugin {
    private static final String TRANSIT = "chief_navigator_transit_veiled_sun";
    private static final String LEAVE = "leave";
    private InteractionDialogAPI dialog;
    private boolean entering;

    @Override
    protected void initConversation(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        SectorEntityToken target = dialog.getInteractionTarget();
        entering = VeiledSunRoom.SECTOR_WELL_ID.equals(target.getId());
        dialog.getVisualPanel().showImageVisual(
                target.getCustomInteractionDialogImageVisual());
        if (entering) {
            dialog.getTextPanel().addPara(
                    "A muted stellar gravity signature resolves inside the dark. "
                            + "The route descends toward a sun almost entirely veiled by cloud.");
            dialog.getOptionPanel().addOption(
                    "Descend into the Veiled Sun", TRANSIT);
        } else {
            dialog.getTextPanel().addPara(
                    "The jump-point opens onto the lightless geometry of the Odyssey Expanse.");
            dialog.getOptionPanel().addOption(
                    "Return to the Odyssey Expanse", TRANSIT);
        }
        dialog.getOptionPanel().addOption("Leave", LEAVE);
        dialog.getOptionPanel().setShortcut(
                LEAVE, Keyboard.KEY_ESCAPE, false, false, false, false);
    }

    @Override
    protected void handleOptionSelected(String optionText, Object optionData) {
        if (TRANSIT.equals(optionData)) {
            transit();
        } else if (LEAVE.equals(optionData)) {
            dialog.dismiss();
        }
    }

    private void transit() {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player == null) return;
        StarSystemAPI sectorRoom = OdysseyExpanseSystem.findExisting();
        StarSystemAPI sunRoom = VeiledSunRoom.findExisting();
        if (sectorRoom == null || sunRoom == null) return;
        SectorEntityToken destination = entering
                ? VeiledSunRoom.findSystemJump(sunRoom)
                : VeiledSunRoom.findSectorWell(sectorRoom);
        if (destination == null) return;
        SectorEntityToken source = dialog.getInteractionTarget();
        String label = entering ? "The Veiled Sun" : "The Odyssey Expanse";
        dialog.dismiss();
        Global.getSector().doHyperspaceTransition(
                player,
                source,
                new JumpPointAPI.JumpDestination(destination, label),
                0.5f);
    }

    @Override public void optionMousedOver(String optionText, Object optionData) { }
    @Override public void advance(float amount) { }
    @Override public void backFromEngagement(EngagementResultAPI battleResult) { }
    @Override public Object getContext() { return null; }
    @Override public Map<String, MemoryAPI> getMemoryMap() { return null; }
}
