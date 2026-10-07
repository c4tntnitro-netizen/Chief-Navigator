package data.campaign.rulecmd;

import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;

/** Guards the Wall encounter's normal pre-battle fleet preview. */
public final class DriftingWallPreviewRegression {
    public static void main(String[] args) {
        FleetInteractionDialogPluginImpl.FIDConfig config =
                ChiefNavigatorDriftingWallCMD.createCombatConfig(null, null);
        check(!config.straightToEngage,
                "Drifting Wall must show the normal fleet preview");
        check(!config.pullInAllies,
                "Roaming recruitment patrols must not reinforce the Wall assault");
        check(!config.pullInEnemies && !config.pullInStations,
                "The Wall assault must remain isolated from campaign support");
        System.out.println(
                "Drifting Wall pre-battle preview regression checks passed.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
