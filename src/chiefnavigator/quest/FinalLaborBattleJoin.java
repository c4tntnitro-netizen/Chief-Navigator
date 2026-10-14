package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import java.util.ArrayList;
import java.util.List;

/** Uses native IFF permission only while interacting with the final Spartan battle. */
final class FinalLaborBattleJoin {
    private FinalLaborBattleJoin() { }

    static void withPermission(SectorEntityToken target, Runnable nativeInteraction) {
        List<MemoryAPI> granted = grantPermission(target);
        try {
            nativeInteraction.run();
        } finally {
            for (MemoryAPI memory : granted) {
                memory.unset(MemFlags.MEMORY_KEY_ALLOW_PLAYER_BATTLE_JOIN_TOFF);
            }
        }
    }

    private static List<MemoryAPI> grantPermission(SectorEntityToken target) {
        List<MemoryAPI> granted = new ArrayList<>();
        if (!(target instanceof CampaignFleetAPI) || Global.getSector() == null
                || !MenelausTrial.isFinalLaborActive()) return granted;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        CampaignFleetAPI participant = (CampaignFleetAPI) target;
        BattleAPI battle = participant.getBattle();
        if (player == null || player.isTransponderOn() || battle == null
                || battle.isPlayerInvolved()
                || player.getContainingLocation() != participant.getContainingLocation()) {
            return granted;
        }
        boolean finalStrike = false;
        for (CampaignFleetAPI fleet : battle.getBothSides()) {
            if (fleet != null && fleet.getBattle() == battle
                    && OdysseyPredatorScript.isFinalLaborCenterBattle(fleet)) {
                finalStrike = true;
                break;
            }
        }
        if (!finalStrike) return granted;
        // Native selection without the IFF restriction still owns diplomacy
        // and fleet hostility. Grant permission only on the side it selects.
        BattleAPI.BattleSide side = battle.pickSide(player, false);
        if (side == BattleAPI.BattleSide.NO_JOIN) return granted;
        List<CampaignFleetAPI> playerSide = battle.getSide(side);
        for (CampaignFleetAPI fleet : battle.getBothSides()) {
            if (fleet == null || fleet.getBattle() != battle || fleet.getFaction() == null
                    || !playerSide.contains(fleet)
                    || !TaskForceSpartanFaction.ID.equals(fleet.getFaction().getId())
                    || !(OdysseyExpanseSystem.isOwnedFobIthacaSpartanGuard(fleet)
                            || OdysseyExpanseSystem.isOwnedFobIthacaGateDefenseStation(fleet))) {
                continue;
            }
            MemoryAPI memory = fleet.getMemoryWithoutUpdate();
            // Preserve any existing native permission or explicit denial,
            // including its expiry, instead of overwriting another owner's flag.
            if (memory == null
                    || memory.contains(MemFlags.MEMORY_KEY_ALLOW_PLAYER_BATTLE_JOIN_TOFF)) continue;
            memory.set(MemFlags.MEMORY_KEY_ALLOW_PLAYER_BATTLE_JOIN_TOFF, true);
            granted.add(memory);
        }
        return granted;
    }
}
