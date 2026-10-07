package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

/** Diplomacy belongs to the isolated expedition, not the Core Hegemony. */
public final class HegemonyExpeditionFaction extends BaseCampaignEventListener {
    public static final String ID = "chief_navigator_hegemony_expedition";
    private static final String INITIALIZED =
            "$chief_navigator_expedition_diplomacy_initialized_v1";

    public HegemonyExpeditionFaction() { super(false); }

    public static void install() {
        if (Global.getSector() == null) return;
        FactionAPI faction = Global.getSector().getFaction(ID);
        if (faction == null) return;
        if (!Global.getSector().getMemoryWithoutUpdate().getBoolean(INITIALIZED)) {
            for (FactionAPI other : Global.getSector().getAllFactions()) {
                if (ID.equals(other.getId())) continue;
                faction.setRelationship(other.getId(), 0f);
                other.setRelationship(ID, 0f);
            }
            Global.getSector().getMemoryWithoutUpdate().set(INITIALIZED, true);
        }
        Global.getSector().getListenerManager().removeListenerOfClass(
                HegemonyExpeditionFaction.class);
        Global.getSector().addTransientListener(new HegemonyExpeditionFaction());
    }

    public static boolean isHostile() {
        return Global.getSector().getFaction(ID).getRelationship(Factions.PLAYER) < 0f;
    }

    @Override
    public void reportBattleOccurred(CampaignFleetAPI primaryWinner, BattleAPI battle) {
        if (battle == null || !battle.isPlayerInvolved()) return;
        for (CampaignFleetAPI enemy : battle.getNonPlayerSideSnapshot()) {
            if (!ID.equals(enemy.getFaction().getId())) continue;
            // Vanilla fleet interaction owns reputation loss. Combat only
            // invalidates the expedition's previously issued certificate here.
            Global.getSector().getMemoryWithoutUpdate().unset(
                    OdysseyStrandedFleetsScript.HEGEMONY_TRIBUTE_GRACE);
            for (com.fs.starfarer.api.campaign.LocationAPI location
                    : Global.getSector().getAllLocations()) {
                for (CampaignFleetAPI fleet : location.getFleets()) {
                    if (fleet.isPlayerFleet()
                            || fleet == Global.getSector().getPlayerFleet()) continue;
                    if (ID.equals(fleet.getFaction().getId())) {
                        fleet.getMemoryWithoutUpdate().unset(
                                OdysseyStrandedFleetsScript.HEGEMONY_TRIBUTE_GRACE);
                    }
                }
            }
            return;
        }
    }
}
