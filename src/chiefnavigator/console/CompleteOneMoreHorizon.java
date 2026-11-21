package chiefnavigator.console;

import chiefnavigator.quest.SinniOneMoreHorizonIntel;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

/** Testing command: completes One More Horizon and grants its reward. */
public final class CompleteOneMoreHorizon implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage("This command can only be used in the campaign.");
            return CommandResult.WRONG_CONTEXT;
        }
        if (!SinniOneMoreHorizonIntel.forceComplete()) {
            Console.showMessage("One More Horizon is already complete.");
            return CommandResult.SUCCESS;
        }
        Console.showMessage("One More Horizon completed. Reward active: "
                + "+1 base Burn level and hyperspace burn up to 30.");
        return CommandResult.SUCCESS;
    }
}
