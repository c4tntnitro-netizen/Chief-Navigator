package chiefnavigator.console;

import chiefnavigator.campaign.CampaignWorldInitialization;
import chiefnavigator.quest.OdysseyExpanseSystem;
import chiefnavigator.quest.TroyArrivalScript;
import com.fs.starfarer.api.Global;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

/** Explicitly installs a missing expedition without rebuilding existing worlds. */
public final class InitializeNavigatorWorlds implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) return CommandResult.WRONG_CONTEXT;
        if (args != null && !args.trim().isEmpty()) return CommandResult.BAD_SYNTAX;
        if (!CampaignWorldInitialization.beginExplicitInstallation(Global.getSector())) {
            Console.showMessage("Installation refused: expedition worlds or departure "
                    + "progress already exist, or this recovery was already attempted.");
            return CommandResult.ERROR;
        }
        if (TroyArrivalScript.ensureWaypointTroyExists() == null
                || OdysseyExpanseSystem.ensureExists() == null) {
            Console.showMessage("World installation did not complete. Check starsector.log; "
                    + "the attempt is latched to prevent repeated partial generation.");
            return CommandResult.ERROR;
        }
        TroyArrivalScript.refreshObjectiveVisibility();
        Console.showMessage("Waypoint Troy and the Orion Knot are installed. "
                + "Existing quest progress was preserved.");
        return CommandResult.SUCCESS;
    }
}
