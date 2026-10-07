package chiefnavigator.console;

import chiefnavigator.quest.OdysseyPredatorScript;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

/** Spawns a Starving Threat strike beside the player for campaign testing. */
public final class SpawnIthacaStrike implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage("This command can only be used in the campaign.");
            return CommandResult.WRONG_CONTEXT;
        }

        String value = args == null ? "" : args.trim();
        final boolean secondStrike;
        if (value.isEmpty()) {
            secondStrike = Math.random() >= 0.5d;
        } else if ("first".equalsIgnoreCase(value)) {
            secondStrike = false;
        } else if ("second".equalsIgnoreCase(value)) {
            secondStrike = true;
        } else {
            Console.showMessage("Use: spawnithacastrike [first|second]");
            return CommandResult.BAD_SYNTAX;
        }

        CampaignFleetAPI fleet = OdysseyPredatorScript
                .spawnIthacaStrikeNearPlayer(secondStrike);
        if (fleet == null) {
            Console.showMessage("Could not spawn the strike. Enter Ashen Verge "
                    + "and make sure FOB Ithaca has been generated.");
            return CommandResult.ERROR;
        }

        Console.showMessage("Spawned " + fleet.getName() + " near the player"
                + "; executing the Nex-style station assault queue against "
                + "the nearest surviving FOB Ithaca bastion.");
        return CommandResult.SUCCESS;
    }
}
