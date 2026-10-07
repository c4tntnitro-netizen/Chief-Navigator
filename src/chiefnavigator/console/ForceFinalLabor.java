package chiefnavigator.console;

import chiefnavigator.quest.MenelausTrial;
import chiefnavigator.quest.OdysseyExpanseSystem;
import chiefnavigator.quest.OdysseyPredatorScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

/** Test-only shortcut which opens Labor V without consuming earlier content. */
public final class ForceFinalLabor implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(
                    "This command can only be used in the campaign.");
            return CommandResult.WRONG_CONTEXT;
        }
        if (args != null && !args.trim().isEmpty()) {
            Console.showMessage("Use: forcefinallabor");
            return CommandResult.BAD_SYNTAX;
        }

        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player != null && player.getBattle() != null) {
            Console.showMessage("Leave the current campaign battle before "
                    + "forcing Labor V.");
            return CommandResult.ERROR;
        }

        if (OdysseyExpanseSystem.findExisting() == null) {
            Console.showMessage("Could not start Labor V because the saved "
                    + "Odyssey Sector is missing. This command will not "
                    + "reconstruct campaign topology.");
            return CommandResult.ERROR;
        }
        if (!MenelausTrial.forceFinalLaborAvailableForTesting()) {
            Console.showMessage("Could not unlock Labor V because campaign "
                    + "state was unavailable.");
            return CommandResult.ERROR;
        }

        int spawned = OdysseyPredatorScript
                .restartFinalLaborForTesting();
        if (spawned == OdysseyPredatorScript.FORCE_FINAL_LABOR_BUSY) {
            Console.showMessage("Labor V is unlocked, but its opening wave "
                    + "could not be rebuilt because Gautama or an existing "
                    + "invasion fleet is currently in battle. Leave that "
                    + "battle and run forcefinallabor again.");
            return CommandResult.SUCCESS;
        }
        if (spawned == OdysseyPredatorScript.FORCE_FINAL_LABOR_MISSING_WORLD) {
            Console.showMessage("Could not start Labor V. Make sure Ashen "
                    + "Verge and FOB Ithaca were generated successfully.");
            return CommandResult.ERROR;
        }
        if (spawned <= 0) {
            Console.showMessage("Labor V is unlocked, but no opening invasion "
                    + "fleets could be created. Check starsector.log for the "
                    + "Ithaca strike diagnostics.");
            return CommandResult.ERROR;
        }

        Console.showMessage("Labor V: Heavenly Strike is now active. Spawned "
                + spawned + " themed perimeter Strike fleets, refreshed the "
                + "three bastions and TFS rosters, started their 60-day "
                + "siege clocks, refreshed the final-Labor Intel, and "
                + "started the mission music cue.");
        return CommandResult.SUCCESS;
    }
}
