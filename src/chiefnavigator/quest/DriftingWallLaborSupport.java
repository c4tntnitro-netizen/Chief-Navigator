package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;

/** A single, attrition-persistent Wall detachment for the accepted fifth Labor. */
public final class DriftingWallLaborSupport {
    public static final String FLEET_ID =
            "chief_navigator_drifting_wall_labor_five_support";
    public static final String MARKER =
            "$chief_navigator_drifting_wall_labor_five_support";
    public static final String GRANTED =
            "$chief_navigator_drifting_wall_labor_five_support_granted";
    static final String FLEET_REFERENCE =
            "$chief_navigator_drifting_wall_labor_five_support_fleet";
    static final String BASTION_VARIANT =
            "chief_navigator_combat_guard_bastillon_Standard";
    static final int BASTION_COUNT = 2;

    private DriftingWallLaborSupport() { }

    public static boolean supportAvailable(SectorEntityToken wall) {
        return Global.getSector() != null
                && DriftingWallEncounter.isDriftingWall(wall)
                && wall.getContainingLocation() != null
                && MenelausTrial.isWallFriendly()
                && MenelausTrial.isFinalLaborActive()
                && !memory().getBoolean(GRANTED)
                && Global.getSector().getPlayerFleet() != null
                && Global.getSector().getPlayerFleet().getContainingLocation()
                        == wall.getContainingLocation();
    }

    /** Called automatically by the friendly Wall's accepted-Labor-V root. */
    public static boolean grant(SectorEntityToken wall) {
        if (!supportAvailable(wall)) return false;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player.getContainingLocation() != wall.getContainingLocation()) {
            return false;
        }
        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(
                IthacaSectionEncounter.COMBAT_GUARD_FACTION_ID,
                "Wall Guard Detachment", true);
        fleet.setId(FLEET_ID);
        fleet.setNoFactionInName(true);
        fleet.setTransponderOn(false);
        fleet.setNoAutoDespawn(true);
        fleet.setAbortDespawn(true);
        fleet.setInflater(null);
        MemoryAPI fleetMemory = fleet.getMemoryWithoutUpdate();
        fleetMemory.set(MARKER, true);
        fleetMemory.set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true);
        fleetMemory.set(MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS, true);
        fleetMemory.set(MemFlags.MEMORY_KEY_IGNORE_PLAYER_COMMS, true);
        fleetMemory.set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        fleetMemory.set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
        fleetMemory.set(
                MemFlags.MEMORY_KEY_NO_SHIP_DERELICTS_IN_POST_BATTLE_DEBRIS,
                true);
        // Unlike a confined patrol, this detachment must jump to Ithaca.
        for (int i = 0; i < BASTION_COUNT; i++) {
            FleetMemberAPI member = Global.getFactory().createFleetMember(
                    FleetMemberType.SHIP, BASTION_VARIANT);
            fleet.getFleetData().addFleetMember(member);
            member.getRepairTracker().setCR(
                    member.getRepairTracker().getMaxCR());
        }
        fleet.getFleetData().setFlagship(
                fleet.getFleetData().getMembersListCopy().get(0));
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
        followPlayer(fleet, player);
        memory().set(GRANTED, true);
        memory().set(FLEET_REFERENCE, fleet);
        wall.getContainingLocation().addEntity(fleet);
        fleet.setLocation(player.getLocation().x + 300f,
                player.getLocation().y + 300f);
        return true;
    }

    /** The existing Labor watcher maintains one reference, never a sector scan. */
    public static void advance() {
        if (Global.getSector() == null) return;
        CampaignFleetAPI fleet = getDetachment();
        if (fleet == null) return;
        LocationAPI location = fleet.getContainingLocation();
        if (location == null) {
            memory().unset(FLEET_REFERENCE);
            return;
        }
        if (TroyArrivalScript.isFleetBusyForMutation(fleet)) return;
        if (!MenelausTrial.isFinalLaborActive() || fleet.isEmpty()) {
            fleet.clearAssignments();
            location.removeEntity(fleet);
            memory().unset(FLEET_REFERENCE);
            return;
        }
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player != null && (fleet.getCurrentAssignment() == null
                || fleet.getCurrentAssignment().getAssignment()
                        != FleetAssignment.FOLLOW
                || fleet.getCurrentAssignment().getTarget() != player)) {
            followPlayer(fleet, player);
        }
    }

    /** Only the authored fifth-Labor fights may draw this detachment in. */
    static CampaignFleetAPI findSupport(
            CampaignFleetAPI enemy, CampaignFleetAPI player) {
        if (Global.getSector() == null || enemy == null || player == null
                || !MenelausTrial.isFinalLaborActive()
                || !(OdysseyPredatorScript.isFinalLaborPerimeterBattle(enemy)
                    || OdysseyPredatorScript.isFinalLaborCenterBattle(enemy))) {
            return null;
        }
        CampaignFleetAPI fleet = getDetachment();
        if (fleet == null || fleet.isEmpty()
                || TroyArrivalScript.isFleetBusyForMutation(fleet)
                || fleet.getContainingLocation() == null
                || fleet.getContainingLocation() != player.getContainingLocation()
                || fleet.getContainingLocation() != enemy.getContainingLocation()
                || fleet.isHostileTo(player)
                || !fleet.isHostileTo(enemy)) return null;
        float distance = Misc.getDistance(fleet, player)
                - fleet.getRadius() - player.getRadius();
        return distance <= IthacaSupportInteraction.SUPPORT_RANGE
                ? fleet : null;
    }

    private static CampaignFleetAPI getDetachment() {
        Object value = memory().get(FLEET_REFERENCE);
        if (!(value instanceof CampaignFleetAPI)) return null;
        CampaignFleetAPI fleet = (CampaignFleetAPI) value;
        return fleet.isPlayerFleet()
                || fleet == Global.getSector().getPlayerFleet()
                || !FLEET_ID.equals(fleet.getId())
                || !fleet.getMemoryWithoutUpdate().getBoolean(MARKER)
                        ? null : fleet;
    }

    private static void followPlayer(
            CampaignFleetAPI fleet, CampaignFleetAPI player) {
        fleet.clearAssignments();
        fleet.addAssignment(FleetAssignment.FOLLOW, player, 1000000f,
                "escorting your fleet for the fifth Labor");
    }

    private static MemoryAPI memory() {
        return Global.getSector().getMemoryWithoutUpdate();
    }
}
