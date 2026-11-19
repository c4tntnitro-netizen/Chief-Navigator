package chiefnavigator.quest;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/** Transient repair stamps; actual roster/variant changes invalidate them. */
final class FleetIdentityMaintenance {
    private static final Map<CampaignFleetAPI, Stamp> repaired = new WeakHashMap<>();
    private static final Map<CampaignFleetAPI, Long> upgraded = new WeakHashMap<>();

    static void reset() { repaired.clear(); upgraded.clear(); }
    static void invalidate(CampaignFleetAPI fleet) { repaired.remove(fleet); upgraded.remove(fleet); }

    private static long rosterKey(CampaignFleetAPI fleet, int tier) {
        return ((long) tier << 32) | (fleet.getFleetData().getNumMembers() & 0xffffffffL);
    }
    static boolean needsUpgrade(CampaignFleetAPI fleet, int tier) {
        Long previous = upgraded.get(fleet);
        return previous == null || previous.longValue() != rosterKey(fleet, tier);
    }
    static void upgraded(CampaignFleetAPI fleet, int tier) {
        upgraded.put(fleet, rosterKey(fleet, tier));
    }

    static boolean needsRepair(CampaignFleetAPI fleet) {
        Stamp stamp = repaired.get(fleet);
        return stamp == null || stamp.members != fleet.getFleetData().getNumMembers()
                || stamp.commander.get() != fleet.getCommander()
                || fleet.getFaction() == null
                || !stamp.faction.equals(fleet.getFaction().getId());
    }

    static void repaired(CampaignFleetAPI fleet) {
        if (fleet.getFaction() != null) repaired.put(fleet, new Stamp(fleet));
    }

    private static final class Stamp {
        final int members;
        final String faction;
        final WeakReference<PersonAPI> commander;
        Stamp(CampaignFleetAPI fleet) {
            members = fleet.getFleetData().getNumMembers();
            faction = fleet.getFaction().getId();
            commander = new WeakReference<>(fleet.getCommander());
        }
    }
}
