package chiefnavigator.quest;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.listeners.ExtraSalvageShownListener;
import com.fs.starfarer.api.campaign.listeners.CargoScreenListener;
import com.fs.starfarer.api.campaign.listeners.CargoGainedListener;
import com.fs.starfarer.api.campaign.listeners.ColonyInteractionListener;
import com.fs.starfarer.api.campaign.listeners.CurrentLocationChangedListener;
import chiefnavigator.weapons.IthacaResearchUpgrades;

/** Persists station salvage; exposing loot alone does not record component recovery. */
public final class ResearchStationSalvageListener
        implements ExtraSalvageShownListener, CargoScreenListener, CargoGainedListener,
                   ColonyInteractionListener, CurrentLocationChangedListener {
    @Override
    public void reportExtraSalvageShown(SectorEntityToken entity) {
        OdysseyExpanseSystem.markResearchStationSalvaged(entity);
        MenelausSensorLabor.markDoomRecovered(entity);
        MenelausTrialScript.requestReconciliation();
    }

    private static void cargoChanged() {
        // Transaction notifications are sent after native cargo confirmation.
        // Never infer recovery merely from generating or displaying loot.
        IthacaResearchUpgrades.captureRecoveredComponents();
        // Defer world mutation until the campaign resumes, then retire the job.
        AviciResearchStationScript.install();
        MenelausTrialScript.requestReconciliation();
    }

    @Override public void reportPlayerNonMarketTransaction(
            PlayerMarketTransaction transaction, InteractionDialogAPI dialog) {
        cargoChanged();
    }
    @Override public void reportSpecialCargoGainedFromRecoveredDerelict(
            CargoAPI loot, InteractionDialogAPI dialog) { cargoChanged(); }
    @Override public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
        cargoChanged();
    }
    @Override public void reportCargoScreenOpened() { cargoChanged(); }
    @Override public void reportPlayerClosedMarket(MarketAPI market) { cargoChanged(); }
    @Override public void reportCurrentLocationChanged(LocationAPI prev, LocationAPI curr) {
        cargoChanged();
    }
    @Override public void reportPlayerLeftCargoPods(SectorEntityToken entity) { }
    @Override public void reportSubmarketOpened(SubmarketAPI submarket) { }
    @Override public void reportPlayerOpenedMarket(MarketAPI market) { }
    @Override public void reportPlayerOpenedMarketAndCargoUpdated(MarketAPI market) { }
}
