package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;
import java.util.List;

/** Player-hostile siege machines that concentrate on Task Force Spartan. */
public final class IthacaThreatPolicy {
    public static final String FACTION_ID = "chief_navigator_ithaca_threat";
    private static final String CHECKED = "$chief_navigator_threat_opportunity_checked";
    private static final String HOSTILE = "$chief_navigator_threat_opportunity_hostile";

    private IthacaThreatPolicy() { }

    public static void initializeRelations() {
        FactionAPI faction = Global.getSector().getFaction(FACTION_ID);
        faction.setRelationship(Factions.PLAYER, -1f);
        Global.getSector().getPlayerFaction().setRelationship(FACTION_ID, -1f);
        faction.setRelationship(IthacaSectionEncounter.COMBAT_GUARD_FACTION_ID, -1f);
        Global.getSector().getFaction(IthacaSectionEncounter.COMBAT_GUARD_FACTION_ID)
                .setRelationship(FACTION_ID, -1f);
        faction.setRelationship(Factions.HEGEMONY, -1f);
        Global.getSector().getFaction(Factions.HEGEMONY)
                .setRelationship(FACTION_ID, -1f);
        faction.setRelationship(Factions.THREAT, 1f);
        Global.getSector().getFaction(Factions.THREAT)
                .setRelationship(FACTION_ID, 1f);
        faction.setRelationship(Factions.OMEGA, 1f);
    }

    static boolean opportunityAllowed(float distanceFromIthaca, float distanceToPlayer,
            boolean visible, float threatFP, float playerFP) {
        return distanceFromIthaca > 3500f && distanceToPlayer <= 700f
                && visible && threatFP >= Math.max(1f, playerFP) * 1.3f;
    }

    /** Returns true while Spartan or a rare nearby player opportunity is targeted. */
    public static boolean apply(CampaignFleetAPI fleet, StarSystemAPI system) {
        if (!FACTION_ID.equals(fleet.getFaction().getId())) fleet.setFaction(FACTION_ID, true);
        fleet.setNoAutoDespawn(true);
        fleet.setAbortDespawn(true);
        if (fleet.getBattle() != null) return true;
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        memory.set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        memory.set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
        // Preserve actual faction hostility. A blanket non-hostile override
        // would prevent the player from joining Ithaca's side in an ongoing
        // Starving Threat assault.
        memory.unset(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE);
        memory.unset(MemFlags.NON_HOSTILE_OVERRIDES_MAKE_HOSTILE);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_HOSTILE);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE);
        memory.unset(MemFlags.MEMORY_KEY_PURSUE_PLAYER);
        memory.unset(MemFlags.FLEET_DO_NOT_IGNORE_PLAYER);
        memory.unset(MemFlags.MEMORY_KEY_STICK_WITH_PLAYER_IF_ALREADY_TARGET);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE);
        // The Ashen Verge population is allowed to see fleets because Spartan
        // is now its primary campaign target. Player pursuit remains gated by
        // the close-range opportunity check below.
        memory.unset(MemFlags.FLEET_IGNORES_OTHER_FLEETS);
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        SectorEntityToken center = system.getEntityById(IthacaSectionEncounter.FOB_ENTITY_ID);
        boolean eligible = player != null
                && OdysseyExpanseSystem.isFobIthaca(center)
                && player.getContainingLocation() == system
                && opportunityAllowed(Misc.getDistance(player, center),
                        Misc.getDistance(player, fleet), player.isVisibleToSensorsOf(fleet),
                        fleet.getFleetPoints(), player.getFleetPoints());
        if (!eligible) memory.unset(HOSTILE);
        if (eligible && !memory.contains(CHECKED)) {
            memory.set(CHECKED, true, 3f);
            if (Math.random() < 0.1) memory.set(HOSTILE, true, 0.2f);
        }
        if (memory.getBoolean(HOSTILE)) {
            memory.set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true, 0.01f);
            if (fleet.getCurrentAssignment() == null
                    || fleet.getCurrentAssignment().getTarget() != player) {
                fleet.clearAssignments();
                fleet.addAssignment(FleetAssignment.INTERCEPT, player, 0.2f,
                        "probing an isolated fleet");
            }
            return true;
        }

        List<CampaignFleetAPI> spartans = OdysseyExpanseSystem
                .ensureFobIthacaSpartanGuards(system);
        if (spartans.isEmpty()) return false;
        int hash = fleet.getId() == null ? 0 : fleet.getId().hashCode();
        CampaignFleetAPI spartan = spartans.get(
                (hash & Integer.MAX_VALUE) % spartans.size());
        if (fleet.getCurrentAssignment() == null
                || fleet.getCurrentAssignment().getAssignment()
                        != FleetAssignment.INTERCEPT
                || fleet.getCurrentAssignment().getTarget() != spartan) {
            fleet.clearAssignments();
            fleet.addAssignment(
                    FleetAssignment.INTERCEPT,
                    spartan,
                    1000000f,
                    "converging on Task Force Spartan");
        }
        return true;
    }
}
