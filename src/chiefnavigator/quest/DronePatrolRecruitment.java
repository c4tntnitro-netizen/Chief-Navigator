package chiefnavigator.quest;

import chiefnavigator.campaign.DomainSecurityIFFAuthorization;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Reassigns existing friendly patrol drones without replenishing their source. */
public final class DronePatrolRecruitment {
    private DronePatrolRecruitment() { }

    public static List<FleetMemberAPI> getEligibleMembers(
            CampaignFleetAPI patrol) {
        List<FleetMemberAPI> result = new ArrayList<FleetMemberAPI>();
        if (!isAvailable(patrol)) return result;
        for (FleetMemberAPI member
                : patrol.getFleetData().getMembersListCopy()) {
            if (member != null && !member.isFighterWing()
                    && !member.isStation() && member.getHullId() != null
                    && member.getHullId().startsWith(
                            "chief_navigator_combat_guard_")
                    && member.getHullSpec() != null
                    && member.getHullSpec().hasTag("derelict")
                    && member.getVariant() != null
                    && member.getVariant().hasHullMod(HullMods.AUTOMATED)) {
                result.add(member);
            }
        }
        return result;
    }

    public static boolean isAvailable(CampaignFleetAPI patrol) {
        if (Global.getSector() == null
                || !MenelausTrial.isFriendlyDronePatrol(patrol)
                || patrol.getBattle() != null || patrol.isDespawning()
                || patrol.isInHyperspaceTransition()
                || patrol.getContainingLocation() == null) return false;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        return player != null && player != patrol
                && patrol.getContainingLocation() == player.getContainingLocation()
                && patrol.getContainingLocation().getEntityById(patrol.getId())
                        == patrol;
    }

    public static FleetMemberAPI chooseRandom(
            CampaignFleetAPI patrol, Random random) {
        List<FleetMemberAPI> candidates = getEligibleMembers(patrol);
        return candidates.isEmpty() ? null
                : candidates.get(random.nextInt(candidates.size()));
    }

    /** Called only by the confirmed story-point action, not a rules option. */
    public static boolean transfer(
            CampaignFleetAPI patrol, FleetMemberAPI member) {
        if (member == null || !getEligibleMembers(patrol).contains(member)) {
            return false;
        }
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player.getFleetData().getMembersListCopy().contains(member)) {
            return false;
        }
        try {
            patrol.getFleetData().removeFleetMember(member);
            player.getFleetData().addFleetMember(member);
            DomainSecurityIFFAuthorization.authorizeTransferredDrone(member);
            patrol.getFleetData().setSyncNeeded();
            patrol.getFleetData().syncIfNeeded();
            player.getFleetData().setSyncNeeded();
            player.getFleetData().syncIfNeeded();
            return true;
        } catch (RuntimeException failure) {
            // Preserve the actual ship if a fleet-data update fails. The
            // confirmed action refunds its point rather than charging for it.
            if (player.getFleetData().getMembersListCopy().contains(member)) {
                player.getFleetData().removeFleetMember(member);
            }
            if (!patrol.getFleetData().getMembersListCopy().contains(member)) {
                patrol.getFleetData().addFleetMember(member);
            }
            throw failure;
        }
    }
}
