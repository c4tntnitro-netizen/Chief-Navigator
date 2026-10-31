package chiefnavigator.console;

import chiefnavigator.weapons.IthacaResearchUpgrades;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

/** Enables every authored Ithaca wall and gate-defense research upgrade. */
public final class MaxUpgradeWallModules implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(
                    "This command can only be used in the campaign.");
            return CommandResult.WRONG_CONTEXT;
        }
        if (args != null && !args.trim().isEmpty()) {
            Console.showMessage("Use: maxupgradewallmodules");
            return CommandResult.BAD_SYNTAX;
        }
        if (!IthacaResearchUpgrades.enableMaximumUpgradeOverride()) {
            Console.showMessage(
                    "Could not access campaign state for wall upgrades.");
            return CommandResult.ERROR;
        }

        Console.showMessage("All seven Ithaca research upgrades are enabled "
                + "for combat, including the Abyssal Focusing Lens. Component "
                + "recovery and Labor progress were not changed.");
        return CommandResult.SUCCESS;
    }
}
