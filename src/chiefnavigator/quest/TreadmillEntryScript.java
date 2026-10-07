package chiefnavigator.quest;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;

/** Announces Troy's storage and departure checkpoint without taking control. */
public final class TreadmillEntryScript implements EveryFrameScript {
    public static final String ARRIVAL_NOTICE_SHOWN =
            "$chief_navigator_troy_arrival_notice_shown";
    /** Historical compatibility entry point; routine loads perform no repair. */
    public static void clearLegacyAutomaticCourse() {
        // Intentionally inert. Serialized navigation state belongs to the
        // campaign and is not rewritten merely because the mod was loaded.
    }

    @Override
    public void advance(float amount) {
        if (!SinniEventideIntel.isBriefed()
                || Global.getSector().getMemoryWithoutUpdate()
                        .getBoolean(TroyArrivalScript.WORMHOLE_OPENED)
                || Global.getSector().getMemoryWithoutUpdate().getBoolean(ARRIVAL_NOTICE_SHOWN)) {
            return;
        }

        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player == null || !(player.getContainingLocation() instanceof StarSystemAPI)) return;
        StarSystemAPI system = (StarSystemAPI) player.getContainingLocation();
        if (system != TroyArrivalScript.findWaypointTroy()) return;

        if (TroyArrivalScript.findDepartureStation(system) == null) {
            return;
        }
        Global.getSector().getMemoryWithoutUpdate().set(ARRIVAL_NOTICE_SHOWN, true);
        Global.getSector().getCampaignUI().addMessage(
                "The Troy Expedition Anchorage is marked on the system map. Approach it when ready.");
    }

    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return false; }
}
