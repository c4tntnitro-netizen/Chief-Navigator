package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreInteractionListener;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import java.util.Map;
import org.lwjgl.input.Keyboard;

/** Free storage and 50-DP checkpoint that opens the Troy Terminus. */
public final class TroyDepartureInteraction extends ChoicePreservingDialogPlugin {
    public static final float MAX_FLEET_DP = 50f;

    private static final String TRIGGER_MAIN =
            "ChiefNavigatorTroyAnchorage_main";
    private static final String TRIGGER_INACTIVE =
            "ChiefNavigatorTroyAnchorage_inactive";
    private static final String TRIGGER_ROUTE_OPEN =
            "ChiefNavigatorTroyAnchorage_route_open";
    private static final String TRIGGER_ADVICE =
            "ChiefNavigatorTroyAnchorage_advice";
    private static final String TRIGGER_INVALID =
            "ChiefNavigatorTroyAnchorage_invalid";
    private static final String TRIGGER_OPENED =
            "ChiefNavigatorTroyAnchorage_opened";
    private static final String STORAGE = "chief_navigator_troy_storage";
    private static final String ADVICE = "chief_navigator_troy_advice";
    private static final String BACK = "chief_navigator_troy_back";
    private static final String DEPART = "chief_navigator_troy_depart";
    private static final String DONE = "chief_navigator_troy_done";
    private static final String LEAVE = "chief_navigator_troy_leave";
    private static final String DP_VALUE = "$chiefNavigatorTroyDP";
    private static final String DP_LIMIT = "$chiefNavigatorTroyDPLimit";
    private InteractionDialogAPI dialog;
    private Map<String, MemoryAPI> memoryMap;

    @Override
    protected void initConversation(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        this.memoryMap = RuleDialogSupport.createMemoryMap(dialog);
        dialog.getVisualPanel().showImageVisual(
                dialog.getInteractionTarget().getCustomInteractionDialogImageVisual());
        showOptions();
    }

    private void showOptions() {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        float fleetDP = getFleetDP(player);

        MemoryAPI local = memoryMap.get(MemKeys.LOCAL);
        local.set(DP_VALUE, formatDP(fleetDP));
        local.set(DP_LIMIT, formatDP(MAX_FLEET_DP));
        boolean missionStarted = SinniEventideIntel.isBriefed();
        boolean routeOpen = Global.getSector().getMemoryWithoutUpdate()
                .getBoolean(TroyArrivalScript.WORMHOLE_OPENED);
        if (!missionStarted) {
            RuleDialogSupport.fire(dialog, memoryMap, TRIGGER_INACTIVE);
        } else if (routeOpen) {
            RuleDialogSupport.fire(dialog, memoryMap, TRIGGER_ROUTE_OPEN);
        } else {
            RuleDialogSupport.fire(dialog, memoryMap, TRIGGER_MAIN);
        }
        if (missionStarted && !routeOpen && fleetDP > MAX_FLEET_DP) {
            dialog.getOptionPanel().setEnabled(DEPART, false);
        }
        dialog.getOptionPanel().setShortcut(
                LEAVE, Keyboard.KEY_ESCAPE, false, false, false, false);
    }

    @Override
    protected void handleOptionSelected(String optionText, Object optionData) {
        if (STORAGE.equals(optionData)) {
            openStorage();
        } else if (ADVICE.equals(optionData)) {
            RuleDialogSupport.fire(dialog, memoryMap, TRIGGER_ADVICE);
            dialog.getOptionPanel().setShortcut(
                    BACK, Keyboard.KEY_ESCAPE, false, false, false, false);
        } else if (BACK.equals(optionData)) {
            showOptions();
        } else if (DEPART.equals(optionData)) {
            beginJourney();
        } else if (DONE.equals(optionData) || LEAVE.equals(optionData)) {
            dialog.dismiss();
        }
    }

    private void openStorage() {
        dialog.getOptionPanel().clearOptions();
        dialog.getVisualPanel().showCore(
                CoreUITabId.CARGO,
                dialog.getInteractionTarget(),
                CampaignUIAPI.CoreUITradeMode.OPEN,
                new CoreInteractionListener() {
                    @Override
                    public void coreUIDismissed() {
                        dialog.getVisualPanel().showImageVisual(
                                dialog.getInteractionTarget()
                                        .getCustomInteractionDialogImageVisual());
                        showOptions();
                    }
                });
        Misc.stopPlayerFleet();
    }

    private void beginJourney() {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        float fleetDP = getFleetDP(player);
        if (!SinniEventideIntel.isBriefed()
                || player == null || fleetDP > MAX_FLEET_DP
                || !(player.getContainingLocation() instanceof StarSystemAPI)) {
            RuleDialogSupport.fire(dialog, memoryMap, TRIGGER_INVALID);
            dialog.getOptionPanel().setShortcut(
                    BACK, Keyboard.KEY_ESCAPE, false, false, false, false);
            return;
        }

        StarSystemAPI system = (StarSystemAPI) player.getContainingLocation();
        if (system != TroyArrivalScript.findWaypointTroy()) {
            return;
        }

        com.fs.starfarer.api.campaign.SectorEntityToken wormhole =
                TroyArrivalScript.ensureOdysseyWormhole(system);
        if (wormhole == null) {
            RuleDialogSupport.fire(dialog, memoryMap, TRIGGER_INVALID);
            dialog.getOptionPanel().setShortcut(
                    BACK, Keyboard.KEY_ESCAPE, false, false, false, false);
            return;
        }
        Global.getSector().getMemoryWithoutUpdate().set(
                TroyArrivalScript.WORMHOLE_OPENED, true);
        Global.getSector().layInCourseFor(wormhole);
        RuleDialogSupport.fire(dialog, memoryMap, TRIGGER_OPENED);
        dialog.getOptionPanel().setShortcut(
                DONE, Keyboard.KEY_ESCAPE, false, false, false, false);
    }

    public static float getFleetDP(CampaignFleetAPI fleet) {
        if (fleet == null || fleet.getFleetData() == null) {
            return 0f;
        }
        float total = 0f;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (!member.isFighterWing()) {
                total += member.getDeploymentPointsCost();
            }
        }
        return total;
    }

    public static String formatDP(float value) {
        if (Math.abs(value - Math.round(value)) < 0.01f) {
            return Integer.toString(Math.round(value));
        }
        return String.format("%.1f", value);
    }

    @Override public void optionMousedOver(String optionText, Object optionData) { }
    @Override public void advance(float amount) { }
    @Override public void backFromEngagement(EngagementResultAPI battleResult) { }
    @Override public Object getContext() { return null; }
    @Override public Map<String, MemoryAPI> getMemoryMap() { return memoryMap; }
}
