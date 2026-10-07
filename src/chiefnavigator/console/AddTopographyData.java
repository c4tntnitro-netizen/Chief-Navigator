package chiefnavigator.console;

import chiefnavigator.topography.SinniHyperspaceTopographyEventIntel;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

/** Testing command for advancing the Hyperspace Topography event. */
public final class AddTopographyData implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage("This command can only be used in the campaign.");
            return CommandResult.WRONG_CONTEXT;
        }

        String value = args == null ? "" : args.trim();
        final int amount;
        try {
            amount = Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            Console.showMessage("Amount must be a positive whole number.");
            return CommandResult.BAD_SYNTAX;
        }
        if (amount <= 0) {
            Console.showMessage("Amount must be greater than zero.");
            return CommandResult.BAD_SYNTAX;
        }

        SinniHyperspaceTopographyEventIntel event =
                SinniHyperspaceTopographyEventIntel.ensureInstalled();
        if (event == null) {
            Console.showMessage("Another mod owns the Hyperspace Topography "
                    + "event; Chief Navigator left it unchanged.");
            return CommandResult.ERROR;
        }
        int before = event.getProgress();
        int after = Math.min(event.getMaxProgress(), before + amount);
        event.setProgress(after);
        int actual = event.getProgress();
        Console.showMessage("Added " + (after - before)
                + " Hyperspace Topography progress ("
                + before + " -> " + actual + ").");
        if (after == event.getMaxProgress() && after - before < amount) {
            Console.showMessage("Progress is capped at " + event.getMaxProgress() + ".");
        }
        return CommandResult.SUCCESS;
    }
}
