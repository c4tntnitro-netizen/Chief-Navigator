package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CoreInteractionListener;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;

/** Native cargo/ship storage over Ithaca's persistent station-owned cargo. */
final class FobIthacaStorageDialog {
    private static final String CARRIER_MARKET_ID =
            "chief_navigator_fob_ithaca_storage_view";
    private static final String CARGO_KEY = "$chief_navigator_ithaca_storage_cargo";

    private FobIthacaStorageDialog() { }

    static CargoAPI getStoredCargo(SectorEntityToken station) {
        MemoryAPI memory = station.getMemoryWithoutUpdate();
        CargoAPI stored = (CargoAPI) memory.get(CARGO_KEY);
        if (stored == null) {
            // CustomCampaignEntity prunes empty native cargo on save, and its
            // getCargo() is not guaranteed to recreate it. Retain one durable
            // station-owned store independently of that nullable native field.
            stored = station.getCargo();
            if (stored == null) stored = Global.getFactory().createCargo(true);
            memory.set(CARGO_KEY, stored);
        }
        stored.setFreeTransfer(true);
        if (stored.getMothballedShips() == null) {
            stored.initMothballedShips(Factions.PLAYER);
        }
        return stored;
    }

    static void show(InteractionDialogAPI dialog, CoreUITabId tab,
            final CoreInteractionListener listener) {
        final SectorEntityToken station = dialog.getInteractionTarget();
        final MarketAPI previous = station.getMarket();
        CargoAPI stored = getStoredCargo(station);

        final MarketAPI carrier = Global.getFactory().createMarket(
                CARRIER_MARKET_ID, "FOB Ithaca", 0);
        carrier.setFactionId(TaskForceSpartanFaction.ID);
        carrier.setHidden(true);
        carrier.setInvalidMissionTarget(true);
        carrier.setForceNoConvertOnSave(true);
        // Storage permissions only: the station is not made a player colony.
        carrier.setPlayerOwned(true);
        carrier.addSubmarket(Submarkets.SUBMARKET_STORAGE);
        StoragePlugin storage = (StoragePlugin) carrier.getSubmarket(
                Submarkets.SUBMARKET_STORAGE).getPlugin();
        storage.setPlayerPaidToUnlock(true);
        // Alias the persistent store, including its mothballed ships. Never
        // copy cargo into a temporary market that will disappear on closing.
        storage.setCargo(stored);
        carrier.setPrimaryEntity(station);
        station.setMarket(carrier);

        try {
            dialog.getVisualPanel().showCore(tab, station,
                    CampaignUIAPI.CoreUITradeMode.OPEN,
                    new CoreInteractionListener() {
                        private boolean closed;

                        @Override
                        public void coreUIDismissed() {
                            if (closed) return;
                            closed = true;
                            release(station, carrier, previous);
                            listener.coreUIDismissed();
                        }
                    });
        } catch (RuntimeException | Error failure) {
            release(station, carrier, previous);
            throw failure;
        }
        // Keep the carrier attached until dismissal: native cargo/fleet tab
        // switches resolve the interaction target's market again. It is never
        // registered with the economy or retained outside this UI session.
    }

    private static void release(SectorEntityToken station, MarketAPI carrier,
            MarketAPI previous) {
        if (station.getMarket() == carrier) station.setMarket(previous);
        carrier.setPrimaryEntity(null);
    }
}
