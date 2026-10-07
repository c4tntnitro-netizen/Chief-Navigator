package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CommDirectoryAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;

/** Opens Starsector's native comm directory without making Ithaca a market. */
final class FobIthacaCommDirectoryDialog {
    private static final String CARRIER_MARKET_ID =
            "chief_navigator_fob_ithaca_comm_directory_view";

    private FobIthacaCommDirectoryDialog() { }

    /**
     * Creates a short-lived directory model for the native UI. The carrier is
     * never remains attached to the station and is never registered with the
     * economy, so it cannot generate population or become serialized
     * campaign topology.
     */
    static void show(InteractionDialogAPI dialog) {
        if (dialog == null || dialog.getInteractionTarget() == null) {
            return;
        }

        SectorEntityToken station = dialog.getInteractionTarget();
        FobIthacaContacts.ensureForStation(station);
        MarketAPI carrier = Global.getFactory().createMarket(
                CARRIER_MARKET_ID, "FOB Ithaca", 3);
        carrier.setFactionId(TaskForceSpartanFaction.ID);
        carrier.setHidden(true);
        carrier.setInvalidMissionTarget(true);
        carrier.setForceNoConvertOnSave(true);

        CommDirectoryAPI directory = carrier.getCommDirectory();
        for (PersonAPI person : FobIthacaContacts.getDirectoryPeople()) {
            // MarketAPI.addPerson() would persistently rewrite the person's
            // market field. The directory entry alone is sufficient for the
            // native UI and leaves Ithaca's entity-anchored residents intact.
            directory.addPerson(person);
        }

        // Vanilla's directory constructor reads the interaction target's
        // market for its title and colors even though the public method takes
        // a CommDirectoryAPI. Supply the carrier only for that constructor;
        // the open UI retains its own market reference afterward.
        MarketAPI previous = station.getMarket();
        station.setActivePerson(null);
        station.setMarket(carrier);
        try {
            dialog.showCommDirectoryDialog(directory);
        } finally {
            station.setMarket(previous);
        }
    }
}
