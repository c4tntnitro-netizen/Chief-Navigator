package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI;
import com.fs.starfarer.api.campaign.ai.TacticalModulePlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;

/** Saved, interruptible thirty-day scan after Ithaca's breach defense is lost. */
final class FinalLaborGateAssault {
    static final String DAYS = "$chief_navigator_final_gate_scan_days_v1";
    static final String LAST = "$chief_navigator_final_gate_scan_last_update_v1";
    static final String ACTIVE = "$chief_navigator_final_gate_scan_active_v1";
    static final String STARTED = "$chief_navigator_final_gate_scan_started_v1";
    static final String ROUTE = "$chief_navigator_final_gate_scan_route_v1";
    static final String DEPARTING = "$chief_navigator_final_gate_scan_departing_v1";
    static final float SCAN_DAYS = 30f;
    private static final float ASSIGNMENT_DAYS = 1000000f;

    private FinalLaborGateAssault() { }

    static SectorEntityToken gate(StarSystemAPI system) {
        SectorEntityToken gate = system.getEntityById(OdysseyExpanseSystem.FOB_ITHACA_GATE_ID);
        return OdysseyExpanseSystem.isFobIthacaGate(gate) ? gate : null;
    }

    /** Called even while battle/UI ownership prevents changing fleet orders. */
    static void advanceClock(CampaignFleetAPI fleet, StarSystemAPI system) {
        if (Global.getSector() == null || Global.getSector().getClock() == null) return;
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        long now = Global.getSector().getClock().getTimestamp();
        float elapsed = memory.contains(LAST)
                ? Global.getSector().getClock().getElapsedDaysSince(memory.getLong(LAST)) : 0f;
        memory.set(LAST, now);
        SectorEntityToken gate = gate(system);
        boolean eligible = memory.getBoolean(ROUTE) && gate != null
                && !Global.getSector().isPaused() && !fleet.isEmpty()
                && !TroyArrivalScript.isFleetBusyForMutation(fleet)
                && fleet.getContainingLocation() == system
                && !OdysseyExpanseSystem.isOwnedFobIthacaGateDefenseStation(
                        system.getEntityById(OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_STATION_ID))
                && MenelausTrial.isFinalLaborActive();
        float contact = gate == null ? 0f : fleet.getRadius() + gate.getRadius() + 150f;
        // Native passive orbits can move outside initial contact distance.
        float range = memory.getBoolean(STARTED) ? Math.max(contact, 1200f) : contact;
        eligible &= gate != null && Misc.getDistance(fleet, gate) <= range;
        if (!eligible) {
            memory.set(ACTIVE, false);
            return;
        }
        if (!memory.getBoolean(STARTED)) {
            memory.set(STARTED, true);
            memory.set(DAYS, 0f);
            if (Global.getSector().getCampaignUI() != null) {
                Global.getSector().getCampaignUI().addMessage(
                        "Heavenly Strike is scanning Ithaca Gate. Stop it within 30 days.");
            }
        } else if (memory.getBoolean(ACTIVE) && Float.isFinite(elapsed) && elapsed > 0f) {
            memory.set(DAYS, Math.min(SCAN_DAYS, memory.getFloat(DAYS) + elapsed));
        }
        memory.set(ACTIVE, true);
        if (memory.getFloat(DAYS) >= SCAN_DAYS && MenelausTrial.failFinalLaborGateScan()) {
            memory.set(ACTIVE, false);
            Global.getLogger(FinalLaborGateAssault.class).info(
                    "[ITHACA_STRIKE] Heavenly Strike completed its thirty-day Ithaca Gate scan.");
            FinalLaborGateScanFailureDialogPlugin.request();
            maintainDeparture(fleet, system);
        }
    }

    static void configure(CampaignFleetAPI fleet, StarSystemAPI system,
            SectorEntityToken fallback) {
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        memory.set(ROUTE, true);
        memory.unset(MemFlags.MEMORY_KEY_PURSUE_PLAYER);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE);
        memory.set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
        advanceClock(fleet, system);
        if (MenelausTrial.isFinalLaborGateScanFailed()) return;
        SectorEntityToken gate = gate(system);
        SectorEntityToken target = gate == null ? fallback : gate;
        boolean scanning = gate != null && memory.getBoolean(STARTED)
                && Misc.getDistance(fleet, gate) <= Math.max(1200f,
                        fleet.getRadius() + gate.getRadius() + 150f);
        FleetAssignment order = gate == null ? FleetAssignment.DEFEND_LOCATION
                : scanning ? FleetAssignment.ORBIT_PASSIVE : FleetAssignment.GO_TO_LOCATION;
        if (fleet.getCurrentAssignment() == null
                || fleet.getCurrentAssignment().getAssignment() != order
                || fleet.getCurrentAssignment().getTarget() != target) {
            fleet.clearAssignments();
            fleet.addAssignment(order, target, ASSIGNMENT_DAYS,
                    gate == null ? "holding at Ithaca" : scanning
                            ? "scanning Ithaca Gate" : "approaching Ithaca Gate");
        }
    }

    static void suspend(CampaignFleetAPI fleet) {
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        memory.unset(ROUTE);
        memory.set(ACTIVE, false);
    }

    static void maintainDeparture(CampaignFleetAPI fleet, StarSystemAPI system) {
        if (fleet == null || fleet.isEmpty()
                || TroyArrivalScript.isFleetBusyForMutation(fleet)) return;
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        memory.set(DEPARTING, true);
        memory.set(ACTIVE, false);
        memory.unset(MemFlags.MEMORY_KEY_NO_JUMP);
        memory.unset(MemFlags.MEMORY_KEY_PURSUE_PLAYER);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_HOSTILE);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE);
        memory.set(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE, true);
        memory.set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true);
        memory.set(MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS, true);
        fleet.setNoAutoDespawn(false);
        fleet.setAbortDespawn(false);
        SectorEntityToken exit = gate(system);
        if (fleet.getCurrentAssignment() != null
                && fleet.getCurrentAssignment().getAssignment()
                        == FleetAssignment.GO_TO_LOCATION_AND_DESPAWN
                && (exit == null || fleet.getCurrentAssignment().getTarget() == exit)) {
            exit = fleet.getCurrentAssignment().getTarget();
        } else {
            // The scene's withdrawal ends at the Gate without activating it
            // or creating a corresponding fleet elsewhere in the Sector.
            if (exit == null) {
                exit = system.createToken(fleet.getLocation().x - 20000f, fleet.getLocation().y);
            }
            fleet.clearAssignments();
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, exit,
                    ASSIGNMENT_DAYS, gate(system) == null
                            ? "leaving Ithaca" : "entering Ithaca Gate");
        }
        if (fleet.getAI() instanceof ModularFleetAIAPI) {
            TacticalModulePlugin tactical = ((ModularFleetAIAPI) fleet.getAI()).getTacticalModule();
            if (tactical != null) {
                tactical.setPriorityTarget(exit, 2f, false);
                tactical.setTarget(exit);
            }
        }
    }
}
