package chiefnavigator.quest;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import java.util.Map;
import org.lwjgl.input.Keyboard;

/** Starts the wall battle from a fixed station campaign entity. */
public final class DriftingWallStationInteraction
        extends ChoicePreservingDialogPlugin {
    private static final String ENGAGE = "engage";
    private static final String LEAVE = "leave";

    private InteractionDialogAPI dialog;
    private SectorEntityToken station;

    @Override
    protected void initConversation(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        this.station = dialog.getInteractionTarget();
        dialog.getVisualPanel().showImageVisual(
                station.getCustomInteractionDialogImageVisual());
        float repairDays = DriftingWallEncounter.getRepairDaysRemaining(station);
        if (repairDays > 0f) {
            int days = Math.max(1, (int) Math.ceil(repairDays));
            dialog.getTextPanel().addPara(
                    "The wall fragment is silent. Its exposed reactor is dark, "
                            + "but autonomous frames are already rebuilding the "
                            + "containment assembly. The station remains inert for "
                            + "approximately " + days + " more days.");
            dialog.getOptionPanel().addOption("Leave", LEAVE);
            dialog.getOptionPanel().setShortcut(
                    LEAVE, Keyboard.KEY_ESCAPE, false, false, false, false);
            return;
        }
        dialog.getTextPanel().addPara(
                "The wall fragment hangs absolutely still. Its surviving weapon "
                        + "systems answer your approach with a targeting solution. "
                        + "Behind the foundation, an exposed reactor burns with a "
                        + "cold cyan light.");
        dialog.getOptionPanel().addOption("Engage the drifting wall", ENGAGE);
        dialog.getOptionPanel().addOption("Leave", LEAVE);
        dialog.getOptionPanel().setShortcut(
                LEAVE, Keyboard.KEY_ESCAPE, false, false, false, false);
    }

    @Override
    protected void handleOptionSelected(String optionText, Object optionData) {
        if (ENGAGE.equals(optionData)) {
            engage();
        } else if (LEAVE.equals(optionData)) {
            dialog.dismissAsCancel();
        }
    }

    private void engage() {
        final CampaignFleetAPI proxy =
                DriftingWallEncounter.createCombatProxy(station);
        if (proxy == null) {
            dialog.dismissAsCancel();
            return;
        }

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
        config.delegate = new FleetInteractionDialogPluginImpl.BaseFIDDelegate() {
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

        FleetInteractionDialogPluginImpl plugin =
                new FleetInteractionDialogPluginImpl(config);
        dialog.setInteractionTarget(proxy);
        dialog.setPlugin(plugin);
        plugin.init(dialog);
    }

    @Override public void optionMousedOver(String optionText, Object optionData) { }
    @Override public void advance(float amount) { }
    @Override public void backFromEngagement(EngagementResultAPI battleResult) { }
    @Override public Object getContext() { return null; }
    @Override public Map<String, MemoryAPI> getMemoryMap() { return null; }
}
