package chiefnavigator.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.missions.cb.BaseCustomBounty;
import com.fs.starfarer.api.impl.campaign.missions.cb.CustomBountyCreator;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseMissionHub;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMission;
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionHub;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

/** Advances native bounty-family difficulty history for campaign testing. */
public final class AddContactBountyProgress implements BaseCommand {
    private static final int DEFAULT_LEVELS = 5;

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) return CommandResult.WRONG_CONTEXT;
        int levels = DEFAULT_LEVELS;
        String argument = args == null ? "" : args.trim();
        if (!argument.isEmpty()) {
            try {
                levels = Integer.parseInt(argument);
            } catch (NumberFormatException invalid) {
                return CommandResult.BAD_SYNTAX;
            }
        }
        if (levels < 1 || levels > 10) return CommandResult.BAD_SYNTAX;

        InteractionDialogAPI dialog = Global.getSector() == null
                || Global.getSector().getCampaignUI() == null ? null
                : Global.getSector().getCampaignUI().getCurrentInteractionDialog();
        SectorEntityToken target = dialog == null ? null : dialog.getInteractionTarget();
        PersonAPI person = target == null ? null : target.getActivePerson();
        if (person == null || person.getMarket() == null) {
            Console.showMessage("Open a contact's available jobs, then run addcontactbountyprogress [levels].");
            return CommandResult.ERROR;
        }
        MissionHub current = BaseMissionHub.get(person);
        if (!(current instanceof BaseMissionHub) || current.getPerson() != person) {
            Console.showMessage("The current NPC has no supported contact mission hub.");
            return CommandResult.ERROR;
        }
        Map<String, BaseCustomBounty> families = findEligibleOffers((BaseMissionHub) current);
        if (families.isEmpty()) {
            Console.showMessage("This NPC has no unaccepted custom bounty offer. "
                    + "Open their available jobs first.");
            return CommandResult.ERROR;
        }
        for (BaseCustomBounty bounty : families.values()) {
            int before = getProgress(bounty);
            int after = addProgress(bounty, levels);
            Console.showMessage(bounty.getMissionId() + " bounty progress: "
                    + before + " -> " + after + " (maximum "
                    + CustomBountyCreator.MAX_DIFFICULTY + ").");
        }
        Console.showMessage("Progress is shared across contacts using each bounty family. "
                + "Run refreshcontactbounties to generate new offers.");
        return CommandResult.SUCCESS;
    }

    /** Finds each offered family's actual native global key once. */
    public static Map<String, BaseCustomBounty> findEligibleOffers(BaseMissionHub hub) {
        Map<String, BaseCustomBounty> families = new LinkedHashMap<>();
        if (hub == null || hub.getPerson() == null || hub.getOfferedMissions() == null) {
            return families;
        }
        for (HubMission mission : hub.getOfferedMissions()) {
            if (!(mission instanceof BaseCustomBounty)) continue;
            BaseCustomBounty bounty = (BaseCustomBounty) mission;
            if (bounty.getCurrentStage() != null || bounty.isMissionCreationAborted()
                    || bounty.getCreator() == null || bounty.getHub() != hub
                    || bounty.getPerson() != hub.getPerson()) continue;
            families.putIfAbsent(bounty.getAggregateDataKey(), bounty);
        }
        return families;
    }

    /** Matches the native effective base before its low/normal/high adjustments. */
    public static int getProgress(BaseCustomBounty bounty) {
        List<Integer> history = bounty.getAggregateData().completedDifficulty;
        float total = 0f;
        for (Integer difficulty : history) total += difficulty;
        return (int) Math.floor(total / Math.max(history.size(),
                BaseCustomBounty.NUM_TO_TRACK_FOR_DIFFICULTY));
    }

    /** Updates only native difficulty history, without completing a mission. */
    public static int addProgress(BaseCustomBounty bounty, int levels) {
        if (levels < 1 || levels > 10) {
            throw new IllegalArgumentException("Bounty progress levels must be between 1 and 10.");
        }
        int after = Math.min(CustomBountyCreator.MAX_DIFFICULTY,
                getProgress(bounty) + levels);
        List<Integer> history = bounty.getAggregateData().completedDifficulty;
        history.clear();
        for (int index = 0; index < BaseCustomBounty.NUM_TO_TRACK_FOR_DIFFICULTY; index++) {
            history.add(after);
        }
        return after;
    }
}
