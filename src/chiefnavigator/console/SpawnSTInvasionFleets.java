package chiefnavigator.console;

import chiefnavigator.quest.MenelausTrial;
import chiefnavigator.quest.OdysseyPredatorScript;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import java.util.List;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

/** Spawns one full Starving Threat endgame invasion per Ithaca bastion. */
public final class SpawnSTInvasionFleets implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage("This command can only be used in the campaign.");
            return CommandResult.WRONG_CONTEXT;
        }
        if (args != null && !args.trim().isEmpty()) {
            Console.showMessage("Use: spawnstinvasionfleets");
            return CommandResult.BAD_SYNTAX;
        }

        List<CampaignFleetAPI> fleets = OdysseyPredatorScript
                .spawnIthacaEndgameInvasionFleets();
        if (fleets.isEmpty()) {
            Console.showMessage("Could not spawn the invasion. Make sure "
                    + "Ashen Verge and FOB Ithaca have been generated.");
            return CommandResult.ERROR;
        }

        if (MenelausTrial.isFinalLaborActive()) {
            Console.showMessage("Spawned " + fleets.size()
                    + " full Third Strike invasion fleets and reset Labor "
                    + "V's 60-day bastion siege clocks.");
        } else {
            Console.showMessage("Spawned " + fleets.size()
                    + " full Third Strike invasion fleets. Each fleet is "
                    + "executing a persistent station assault against its "
                    + "own Ithaca wall module. Combat damage is unmodified.");
        }
        return CommandResult.SUCCESS;
    }
}
