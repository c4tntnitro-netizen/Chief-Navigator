package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;

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
        reconcileDefenderRecognition();
    }

    /** Native friendship must remain visible to Battle.pickSide. */
    static void configureFriendlyDefender(CampaignFleetAPI fleet) {
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        // NO_REP_IMPACT also removes a side's normal friendship eligibility.
        // Encounter configs govern dialog reputation consequences separately.
        memory.unset(MemFlags.MEMORY_KEY_NO_REP_IMPACT);
        // Vanilla uses an unexpired identity memory for permanent recognition
        // (Misc.isPermaKnowsWhoPlayerIs), including with the transponder off.
        memory.set(MemFlags.MEMORY_KEY_SAW_PLAYER_WITH_TRANSPONDER_ON, true);
    }

    private static void reconcileDefenderRecognition() {
        StarSystemAPI system = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.ASHEN_VERGE_ID);
        if (system == null) return;
        for (CampaignFleetAPI fleet : system.getFleets()) {
            if (fleet == null || fleet.getFaction() == null) continue;
            String faction = fleet.getFaction().getId();
            boolean spartan = ID.equals(faction)
                    && (OdysseyExpanseSystem.isOwnedFobIthacaSpartanGuard(fleet)
                            || OdysseyExpanseSystem.isOwnedFobIthacaGateDefenseStation(fleet));
            boolean bastion = IthacaSectionEncounter.COMBAT_GUARD_FACTION_ID.equals(faction)
                    && IthacaSectionEncounter.isSectionStation(fleet);
            // Identity-only reconciliation includes already-busy saved fleets.
            // Their roster, assignment, location and battle stay untouched.
            if (spartan || bastion) configureFriendlyDefender(fleet);
        }
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
