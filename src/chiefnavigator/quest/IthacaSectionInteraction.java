package chiefnavigator.quest;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;

/** Retargets a large fixed bastion click token to its backing station fleet. */
final class IthacaSectionInteraction
        extends FleetInteractionDialogPluginImpl {
    private final CampaignFleetAPI station;

    IthacaSectionInteraction(CampaignFleetAPI station) {
        super(createConfig(station));
        this.station = station;
    }

    @Override
    public void init(InteractionDialogAPI dialog) {
        IthacaSectionEncounter.revealStationsForInteraction(station);
        dialog.setInteractionTarget(station);
        super.init(dialog);
    }

    private static FIDConfig createConfig(
            final CampaignFleetAPI station) {
        final LocationAPI stationLocation = station.getContainingLocation();
        FIDConfig config = new FIDConfig();
        config.leaveAlwaysAvailable = true;
        config.showCommLinkOption = false;
        config.showWarningDialogWhenNotHostile = false;
        config.firstTimeEngageOptionText = "Attack the station";
        config.alwaysAttackVsAttack = true;
        config.impactsAllyReputation = false;
        config.impactsEnemyReputation = false;
        config.pullInEnemies = true;
        config.pullInStations = true;
        config.showPullInText = true;
        config.playerAttackingStation = true;
        config.lootCredits = false;
        config.delegate = new BaseFIDDelegate() {
            @Override
            public void notifyLeave(InteractionDialogAPI dialog) {
                IthacaSectionEncounter.restoreCampaignPresentation(
                        stationLocation);
            }
        };
        return config;
    }
}
