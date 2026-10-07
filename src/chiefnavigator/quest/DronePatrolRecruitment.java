package chiefnavigator.quest;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Retired friendly-patrol transfers; disabled hostile drones use battle recovery. */
public final class DronePatrolRecruitment {
    private DronePatrolRecruitment() { }

    public static List<FleetMemberAPI> getEligibleMembers(CampaignFleetAPI patrol) {
        return Collections.emptyList();
    }

    public static boolean isAvailable(CampaignFleetAPI patrol) {
        return false;
    }

    public static FleetMemberAPI chooseRandom(CampaignFleetAPI patrol, Random random) {
        return null;
    }

    public static boolean transfer(CampaignFleetAPI patrol, FleetMemberAPI member) {
        return false;
    }
}