package chiefnavigator.console;

import chiefnavigator.quest.OdysseyExpanseSystem;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.cb.BaseCustomBounty;
import com.fs.starfarer.api.impl.campaign.missions.cb.CustomBountyCreator;
import com.fs.starfarer.api.impl.campaign.missions.cb.MilitaryCustomBounty;
import com.fs.starfarer.api.impl.campaign.missions.cb.UnderworldCustomBounty;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseMissionHub;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMission;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionCreator;
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionHub;
import com.fs.starfarer.api.util.Misc;
import java.util.List;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

/** Rerolls an open contact's unaccepted custom bounties for campaign testing. */
public final class RefreshContactBounties implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) return CommandResult.WRONG_CONTEXT;
        String argument = args == null ? "" : args.trim();
        String type = null;
        Integer difficulty = null;
        if (!argument.isEmpty()) {
            String[] parts = argument.split("\\s+");
            if (parts[0].equalsIgnoreCase("status")) {
                if (parts.length != 1) return CommandResult.BAD_SYNTAX;
                Console.showMessage(getDerelictFilterStatus());
                return CommandResult.SUCCESS;
            }
            if (parts.length > 2) return CommandResult.BAD_SYNTAX;
            type = parts[0];
            if (parts.length == 2) {
                try {
                    difficulty = Integer.valueOf(parts[1]);
                } catch (NumberFormatException invalid) {
                    return CommandResult.BAD_SYNTAX;
                }
            }
        }
        InteractionDialogAPI dialog = Global.getSector() == null
                || Global.getSector().getCampaignUI() == null ? null
                : Global.getSector().getCampaignUI().getCurrentInteractionDialog();
        SectorEntityToken target = dialog == null ? null : dialog.getInteractionTarget();
        PersonAPI person = target == null ? null : target.getActivePerson();
        Map<String, MemoryAPI> memoryMap = dialog == null || dialog.getPlugin() == null
                ? null : dialog.getPlugin().getMemoryMap();
        if (person == null || person.getMarket() == null || memoryMap == null) {
            Console.showMessage("Open a contact's available jobs, then run refreshcontactbounties.");
            return CommandResult.ERROR;
        }
        MissionHub current = BaseMissionHub.get(person);
        if (!(current instanceof BaseMissionHub) || current.getPerson() != person) {
            Console.showMessage("The current NPC has no supported contact mission hub.");
            return CommandResult.ERROR;
        }
        BaseMissionHub hub = (BaseMissionHub) current;
        boolean hasBounty = false;
        if (hub.getOfferedMissions() != null) {
            for (HubMission offered : hub.getOfferedMissions()) {
                if (canRefresh(offered, hub)) {
                    hasBounty = true;
                    break;
                }
            }
        }
        if (!hasBounty) {
            Console.showMessage("This NPC has no unaccepted custom bounty offer to refresh. "
                    + "Open their available jobs first.");
            return CommandResult.ERROR;
        }

        int refreshed;
        try {
            refreshed = type == null ? refreshOffers(hub, dialog, memoryMap)
                    : refreshOffers(hub, dialog, memoryMap, type, difficulty);
        } catch (InvalidSelection invalid) {
            Console.showMessage(invalid.getMessage());
            return CommandResult.BAD_SYNTAX;
        } catch (RuntimeException failure) {
            Console.showException("Contact bounty regeneration failed; check starsector.log.", failure);
            hub.listMissions(dialog, memoryMap, false);
            return CommandResult.ERROR;
        }
        // Native returnToList refreshes choices without generating or clearing other jobs.
        hub.listMissions(dialog, memoryMap, false);
        Console.showMessage("Refreshed " + refreshed + " bounty offer(s) for "
                + person.getNameString() + ". Accepted missions and other jobs are unchanged.");
        if (type != null && refreshed > 0) {
            Console.showMessage("All difficulty choices use the selected bounty type and level.");
            if (normalizeType(type).equals("derelict")) {
                Console.showMessage(getDerelictFilterStatus());
            }
        }
        return refreshed > 0 ? CommandResult.SUCCESS : CommandResult.ERROR;
    }

    private static boolean canRefresh(HubMission mission, BaseMissionHub hub) {
        if (!(mission instanceof BaseCustomBounty)) return false;
        BaseCustomBounty bounty = (BaseCustomBounty) mission;
        return bounty.getCurrentStage() == null && !bounty.isMissionCreationAborted()
                && bounty.getCreator() != null && bounty.getHub() == hub
                && bounty.getPerson() == hub.getPerson();
    }

    /** Uses the same creator, generation and cleanup lifecycle as the native hub. */
    public static int refreshOffers(BaseMissionHub hub, InteractionDialogAPI dialog,
            Map<String, MemoryAPI> memoryMap) {
        return refreshOffersInternal(hub, dialog, memoryMap, null);
    }

    /** Validates every selected native offer before cleaning up any old targets. */
    public static int refreshOffers(BaseMissionHub hub, InteractionDialogAPI dialog,
            Map<String, MemoryAPI> memoryMap, String type, Integer difficulty) {
        Map<HubMission, ForcedOffer> forced = new IdentityHashMap<>();
        Set<String> available = new LinkedHashSet<>();
        if (hub.getOfferedMissions() != null) {
            for (HubMission mission : hub.getOfferedMissions()) {
                if (!canRefresh(mission, hub) || !isNativeBounty(mission)) continue;
                CustomBountyCreator creator = null;
                for (CustomBountyCreator candidate : ((BaseCustomBounty) mission).getCreators()) {
                    if (candidate == null) continue;
                    String id = normalizeType(candidate.getId());
                    available.add(id);
                    if (id.equals(normalizeType(type))) creator = candidate;
                }
                if (creator == null) continue;
                forced.put(mission, new ForcedOffer(creator,
                        resolveDifficulty(creator, difficulty)));
            }
        }
        if (forced.isEmpty()) {
            if (!available.isEmpty()) {
                throw new InvalidSelection("Unknown bounty type '" + type
                        + "'. Available types: " + String.join(", ", available) + ".");
            }
            throw new InvalidSelection("Open an unaccepted native military or underworld bounty offer first.");
        }
        return refreshOffersInternal(hub, dialog, memoryMap, forced);
    }

    public static CustomBountyCreator resolveBountyType(BaseCustomBounty mission, String type) {
        String wanted = normalizeType(type);
        StringJoiner available = new StringJoiner(", ");
        for (CustomBountyCreator creator : mission.getCreators()) {
            if (creator == null) continue;
            String id = normalizeType(creator.getId());
            if (id.equals(wanted)) return creator;
            available.add(id);
        }
        throw new InvalidSelection("Unknown bounty type '" + type
                + "'. Available types: " + available + ".");
    }

    public static int resolveDifficulty(CustomBountyCreator creator, Integer difficulty) {
        int minimum = creator.getMinDifficulty();
        int maximum = creator.getMaxDifficulty();
        int selected = difficulty == null ? Math.max(5, minimum) : difficulty;
        if (selected < minimum || selected > maximum) {
            throw new InvalidSelection(creator.getId() + " difficulty must be "
                    + minimum + "-" + maximum + ".");
        }
        return selected;
    }

    /** Reports the active creator and the native exclusion tag on existing owned systems. */
    public static String getDerelictFilterStatus() {
        CustomBountyCreator selected = null;
        for (CustomBountyCreator creator : MilitaryCustomBounty.CREATORS) {
            if (creator != null && normalizeType(creator.getId()).equals("derelict")) {
                selected = creator;
            }
        }
        if (selected == null) return "Derelict generator: not registered.";
        String prefix = "Derelict generator: " + selected.getClass().getSimpleName() + "; ";
        int owned = 0;
        int hidden = 0;
        if (Global.getSector() != null) {
            for (StarSystemAPI system : Global.getSector().getStarSystems()) {
                if (!OdysseyExpanseSystem.isInside(system)) continue;
                owned++;
                if (system.hasTag(Tags.THEME_HIDDEN)) hidden++;
            }
        }
        if (owned == 0) {
            return prefix + "no owned Orion Knot systems found for this tag check.";
        }
        return prefix + "Orion Knot systems tagged THEME_HIDDEN: " + hidden + "/" + owned
                + " (native mission-search exclusion).";
    }

    private static String normalizeType(String type) {
        String normalized = type == null ? "" : type.toLowerCase(Locale.ROOT).replace("-", "");
        return normalized.startsWith("cb") ? normalized.substring(2) : normalized;
    }

    private static boolean isNativeBounty(HubMission mission) {
        return mission != null && (mission.getClass() == MilitaryCustomBounty.class
                || mission.getClass() == UnderworldCustomBounty.class);
    }

    private static final class ForcedOffer {
        final CustomBountyCreator creator;
        final int difficulty;

        ForcedOffer(CustomBountyCreator creator, int difficulty) {
            this.creator = creator;
            this.difficulty = difficulty;
        }
    }

    private static final class InvalidSelection extends IllegalArgumentException {
        InvalidSelection(String message) {
            super(message);
        }
    }

    private static int refreshOffersInternal(BaseMissionHub hub, InteractionDialogAPI dialog,
            Map<String, MemoryAPI> memoryMap, Map<HubMission, ForcedOffer> forced) {
        List<HubMission> offers = hub.getOfferedMissions();
        if (offers == null) return 0;
        int refreshed = 0;
        for (int index = 0; index < offers.size();) {
            HubMission previous = offers.get(index);
            if (!canRefresh(previous, hub) || (forced != null && !forced.containsKey(previous))) {
                index++;
                continue;
            }
            HubMissionCreator creator = previous.getCreator();
            HubMission replacement = null;
            if (forced != null) {
                // A modded factory may no longer return the native offered mission's class.
                replacement = creator.createHubMission(hub);
                if (!isNativeBounty(replacement)) {
                    index++;
                    continue;
                }
            }
            previous.abort();
            offers.remove(index);
            creator.setSeed(Misc.genRandomSeed());
            creator.updateRandom();
            if (replacement == null) replacement = creator.createHubMission(hub);
            if (replacement == null) continue;
            try {
                replacement.setHub(hub);
                replacement.setCreator(creator);
                replacement.setGenRandom(creator.getGenRandom());
                if (forced != null) {
                    ForcedOffer selection = forced.get(previous);
                    ((BaseCustomBounty) replacement).setTestMode(
                            selection.creator.getClass(), selection.difficulty);
                }
                replacement.createAndAbortIfFailed(hub.getPerson().getMarket(), false);
                if (replacement.isMissionCreationAborted()) continue;
                replacement.updateInteractionData(dialog, memoryMap);
            } catch (RuntimeException failure) {
                try {
                    replacement.abort();
                } catch (RuntimeException cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
                throw failure;
            }
            offers.add(index, replacement);
            refreshed++;
            index++;
        }
        return refreshed;
    }
}
