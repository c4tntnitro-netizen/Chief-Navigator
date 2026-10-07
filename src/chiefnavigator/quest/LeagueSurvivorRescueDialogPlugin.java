package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import java.util.Map;

/** Rules-backed aftermath; rescued personnel embark only at the final choice. */
public final class LeagueSurvivorRescueDialogPlugin extends ChoicePreservingDialogPlugin {
    static final String PENDING =
            "$chief_navigator_silent_wake_league_rescue_pending_v1";
    static final String RESOLVED =
            "$chief_navigator_silent_wake_league_rescue_resolved_v1";
    private static final String RULE_PREFIX = "ChiefNavigatorLeagueRescue_";
    private static final String ILLUSTRATION_ID =
            "chief_navigator_persean_survivors_aftermath";
    private InteractionDialogAPI dialog;
    private Map<String, MemoryAPI> memoryMap;

    static void request() {
        if (Global.getSector() == null) return;
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        if (!isResolved(memory)) memory.set(PENDING, true);
    }

    private static boolean isResolved(MemoryAPI memory) {
        return memory.getBoolean(RESOLVED)
                || memory.getBoolean(
                        OdysseyStrandedFleetsScript.SILENT_WAKE_LEAGUE_PICKUP_RECORDED);
    }

    static void tryShowPending() {
        SectorAPI sector = Global.getSector();
        if (sector == null) return;
        MemoryAPI memory = sector.getMemoryWithoutUpdate();
        if (isResolved(memory)) {
            memory.unset(PENDING);
            return;
        }
        if (!memory.getBoolean(PENDING)) return;
        CampaignFleetAPI player = sector.getPlayerFleet();
        if (player == null || player.getBattle() != null
                || player.isInHyperspaceTransition()) return;
        CampaignUIAPI ui = sector.getCampaignUI();
        if (ui == null || ui.isShowingDialog() || ui.isShowingMenu()
                || ui.getCurrentCoreTab() != null) return;
        ui.showInteractionDialog(new LeagueSurvivorRescueDialogPlugin(), player);
    }

    static boolean completeRescue() {
        if (Global.getSector() == null) return false;
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        if (isResolved(memory)) return true;
        if (!memory.getBoolean(PENDING)
                || FobIthacaContacts.embarkLeagueSurvivorsFromBattle() == null) {
            return false;
        }
        memory.set(OdysseyStrandedFleetsScript.SILENT_WAKE_LEAGUE_PICKUP_RECORDED, true);
        memory.set(RESOLVED, true);
        memory.unset(PENDING);
        return true;
    }

    static void completeAbandonment() {
        if (Global.getSector() == null) return;
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        if (!memory.getBoolean(PENDING) || isResolved(memory)) return;
        memory.set(RESOLVED, true);
        memory.unset(PENDING);
    }

    @Override
    protected void initConversation(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        memoryMap = RuleDialogSupport.createMemoryMap(dialog);
        dialog.setPromptText("");
        dialog.setBackgroundDimAmount(0.5f);
        dialog.showVisualPanel();
        dialog.getVisualPanel().showImagePortion(
                "illustrations", ILLUSTRATION_ID,
                480f, 300f, 0f, 0f, 480f, 300f);
        fire("league_victory");
    }

    @Override
    protected void handleOptionSelected(String optionText, Object optionData) {
        if (optionData == null) return;
        String id = optionData.toString();
        if ("complete_league_rescue".equals(id)) {
            if (!completeRescue()) {
                Global.getLogger(LeagueSurvivorRescueDialogPlugin.class).warn(
                        "Could not embark the authored League survivor contact");
                return;
            }
        } else if ("complete_league_abandon".equals(id)) {
            completeAbandonment();
        }
        fire(id);
    }

    private void fire(String id) {
        if (!RuleDialogSupport.fire(dialog, memoryMap, RULE_PREFIX + id)) {
            Global.getLogger(LeagueSurvivorRescueDialogPlugin.class).warn(
                    "Missing League rescue stage: " + id);
            dialog.dismiss();
        }
    }

    @Override public void optionMousedOver(String text, Object data) { }
    @Override public void advance(float amount) { }
    @Override public void backFromEngagement(EngagementResultAPI result) { }
    @Override public Object getContext() { return null; }
    @Override public Map<String, MemoryAPI> getMemoryMap() { return memoryMap; }
}
