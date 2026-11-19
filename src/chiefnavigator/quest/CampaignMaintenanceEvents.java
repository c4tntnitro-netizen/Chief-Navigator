package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.listeners.CurrentLocationChangedListener;
import com.fs.starfarer.api.campaign.listeners.FleetInflationListener;
import java.util.ArrayList;
import java.util.List;

/** Transient event bridge. Native campaign events and typed listeners use separate registries. */
public final class CampaignMaintenanceEvents extends BaseCampaignEventListener
        implements CurrentLocationChangedListener, FleetInflationListener {
    private static boolean interactionPending;
    private CampaignMaintenanceEvents() { super(false); }

    public static void install() {
        SectorAPI sector = Global.getSector();
        if (sector == null) return;
        List<CampaignEventListener> old = sector.getAllListeners();
        if (old != null) for (CampaignEventListener listener : new ArrayList<>(old)) {
            if (listener instanceof CampaignMaintenanceEvents) sector.removeListener(listener);
        }
        sector.getListenerManager().removeListenerOfClass(CampaignMaintenanceEvents.class);
        CampaignMaintenanceEvents events = new CampaignMaintenanceEvents();
        interactionPending = false;
        sector.addTransientListener(events);
        sector.getListenerManager().addListener(events, true);
        FleetIdentityMaintenance.reset();
        PopulationSlotSchedule.reset();
        OdysseyPredatorScript.requestPopulationReconciliation();
        OdysseyExpanseSystem.invalidateSystemLookupCache();
        MenelausTrialScript.requestReconciliation();
    }

    private static boolean relevant(CampaignFleetAPI fleet) {
        return fleet != null && (fleet.getId() != null
                && fleet.getId().startsWith("chief_navigator_")
                || fleet.getMemoryWithoutUpdate() != null
                    && OdysseyPredatorScript.isAuthoredStarvingThreatFleet(fleet));
    }

    private static void fleetChanged(CampaignFleetAPI fleet) {
        if (!relevant(fleet)) return;
        FleetIdentityMaintenance.invalidate(fleet);
        PopulationSlotSchedule.invalidate(fleet.getId());
        OdysseyPredatorScript.requestPopulationReconciliation();
        MenelausTrialScript.requestReconciliation();
        if (AviciDwellerPopulation.isOwnedFleet(fleet)) AviciDwellerPopulation.install();
    }

    @Override public void reportFleetSpawned(CampaignFleetAPI fleet) { fleetChanged(fleet); }
    @Override public void reportFleetJumped(CampaignFleetAPI fleet, SectorEntityToken from,
            JumpPointAPI.JumpDestination to) { fleetChanged(fleet); }
    @Override public void reportFleetInflated(CampaignFleetAPI fleet, FleetInflater inflater) {
        fleetChanged(fleet);
    }
    @Override public void reportFleetDespawned(CampaignFleetAPI fleet,
            CampaignEventListener.FleetDespawnReason reason, Object param) { fleetChanged(fleet); }
    @Override public void reportBattleFinished(CampaignFleetAPI winner, BattleAPI battle) {
        if (battle == null) return;
        List<CampaignFleetAPI> fleets = battle.getSnapshotBothSides();
        if (fleets == null) fleets = battle.getBothSides();
        if (fleets != null) for (CampaignFleetAPI fleet : fleets) fleetChanged(fleet);
    }
    @Override public void reportShownInteractionDialog(InteractionDialogAPI dialog) {
        SectorEntityToken target = dialog == null ? null : dialog.getInteractionTarget();
        if (target != null && (target.getId() != null && target.getId().startsWith("chief_navigator_")
                || target instanceof CampaignFleetAPI && relevant((CampaignFleetAPI) target))) {
            interactionPending = true;
            MenelausTrialScript.requestReconciliation();
        }
    }

    /** The population script pauses during dialogs, so the native open event arms this handoff. */
    static boolean consumeClosedInteraction() {
        if (!interactionPending || Global.getSector() == null) return false;
        CampaignUIAPI ui = Global.getSector().getCampaignUI();
        if (ui != null && ui.isShowingDialog()) return false;
        interactionPending = false;
        return true;
    }
    @Override public void reportCurrentLocationChanged(LocationAPI prev, LocationAPI curr) {
        OdysseyExpanseSystem.invalidateSystemLookupCache();
        PopulationSlotSchedule.reset();
        OdysseyPredatorScript.requestPopulationReconciliation();
        SinniNavigationGuidance.invalidateMapTarget();
        MenelausTrialScript.requestReconciliation();
        AviciDwellerPopulation.install();
    }
}
