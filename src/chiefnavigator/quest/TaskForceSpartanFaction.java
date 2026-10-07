package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

/** Durable diplomacy for Menelaus and Task Force Spartan. */
public final class TaskForceSpartanFaction {
    public static final String ID = "chief_navigator_task_force_spartan";

    private TaskForceSpartanFaction() { }

    /**
     * Reasserts the expedition's fixed allegiances. Ithaca siege fleets use
     * their own campaign faction, while the other Starving fleets use the
     * vanilla Threat faction. Legacy Omega relations remain hostile so older
     * save state cannot turn an unmigrated attacker friendly.
     */
    public static void enforceRelations() {
        if (Global.getSector() == null) return;
        FactionAPI spartan = Global.getSector().getFaction(ID);
        if (spartan == null) return;

        setMutualRelationship(spartan, Factions.PLAYER, 1f);
        setMutualRelationship(spartan, IthacaThreatPolicy.FACTION_ID, -1f);
        setMutualRelationship(spartan, Factions.THREAT, -1f);
        setMutualRelationship(spartan, Factions.OMEGA, -1f);
    }

    private static void setMutualRelationship(
            FactionAPI spartan, String otherId, float relationship) {
        FactionAPI other = Global.getSector().getFaction(otherId);
        if (Math.abs(spartan.getRelationship(otherId) - relationship)
                > 0.0001f) {
            spartan.setRelationship(otherId, relationship);
        }
        if (other != null
                && Math.abs(other.getRelationship(ID) - relationship)
                        > 0.0001f) {
            other.setRelationship(ID, relationship);
        }
    }
}
