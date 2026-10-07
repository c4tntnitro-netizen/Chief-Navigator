package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;

/** Supplies buy a chance to reach Ithaca, not an escort or a new contact. */
final class ExileSupplyAid {
    static final String AIDED = "$chief_navigator_sanzu_exiles_aided_v1";
    static final float SUPPLY_COST = 20f;
    private ExileSupplyAid() { }

    static boolean hasReceivedAid(CampaignFleetAPI fleet) {
        return fleet != null && fleet.getMemoryWithoutUpdate().getBoolean(AIDED);
    }

    static boolean canDonate(CampaignFleetAPI fleet, SectorEntityToken station) {
        if (Global.getSector() == null
                || !OdysseyStrandedFleetsScript.isRescueEscortCandidate(fleet)
                || hasReceivedAid(fleet) || fleet.isEmpty()
                || fleet.getBattle() != null || fleet.isInHyperspaceTransition()
                || fleet.isDespawning() || fleet.getContainingLocation() == null
                || !OdysseyExpanseSystem.isFobIthaca(station)) return false;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        return player != null && player.getCargo() != null
                && fleet.getCargo() != null
                && player.getCargo().getSupplies() >= SUPPLY_COST;
    }

    static boolean donateSupplies(
            CampaignFleetAPI fleet, SectorEntityToken station) {
        if (!canDonate(fleet, station)) return false;
        CargoAPI playerCargo = Global.getSector().getPlayerFleet().getCargo();
        playerCargo.removeSupplies(SUPPLY_COST);
        fleet.getCargo().addSupplies(SUPPLY_COST);
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        memory.set(AIDED, true);
        memory.unset(MemFlags.MEMORY_KEY_NO_JUMP);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_HOSTILE);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE);
        memory.unset(MemFlags.MEMORY_KEY_AVOID_PLAYER_SLOWLY);
        memory.set(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE, true);
        memory.set(MemFlags.NON_HOSTILE_OVERRIDES_MAKE_HOSTILE, true);
        memory.set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE, true);
        fleet.setAbortDespawn(false);
        fleet.clearAssignments();
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,
                station, 1000f,
                "seeking refuge at FOB Ithaca");
        return true;
    }
}
