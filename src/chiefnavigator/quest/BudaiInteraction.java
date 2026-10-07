package chiefnavigator.quest;

import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;

/** Native battle flow without the human-faction presentation for owned Budai. */
public final class BudaiInteraction extends FleetInteractionDialogPluginImpl {
    public BudaiInteraction(boolean ambush) {
        super(encounterConfig(ambush));
    }

    private static FIDConfig encounterConfig(boolean ambush) {
        FIDConfig config = new FIDConfig();
        // Match DwellerCMD's non-human presentation, not its temporary
        // manifestation lifecycle, forced battle rules, or salvage delegate.
        config.showCommLinkOption = false;
        config.showTransponderStatus = false;
        config.showFleetAttitude = false;
        config.showEngageText = false;
        config.showWarningDialogWhenNotHostile = false;
        config.impactsAllyReputation = false;
        config.impactsEnemyReputation = false;
        if (ambush) {
            config.alwaysPursue = true;
            config.firstTimeEngageOptionText = "Spring the ambush";
        }
        return config;
    }

}
