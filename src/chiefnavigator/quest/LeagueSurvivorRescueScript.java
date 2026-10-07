package chiefnavigator.quest;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;

/** Queues the authored rescue decision only after a player-side victory. */
public final class LeagueSurvivorRescueScript
        extends BaseCampaignEventListener implements EveryFrameScript {
    public LeagueSurvivorRescueScript() { super(false); }

    public static void install() {
        if (Global.getSector() == null) return;
        Global.getSector().removeTransientScriptsOfClass(
                LeagueSurvivorRescueScript.class);
        Global.getSector().getListenerManager().removeListenerOfClass(
                LeagueSurvivorRescueScript.class);
        LeagueSurvivorRescueScript watcher = new LeagueSurvivorRescueScript();
        Global.getSector().addTransientScript(watcher);
        Global.getSector().addTransientListener(watcher);
    }

    @Override
    public void reportBattleOccurred(CampaignFleetAPI primaryWinner, BattleAPI battle) {
        if (Global.getSector() == null || battle == null
                || !battle.isPlayerInvolved() || primaryWinner == null
                || !battle.isOnPlayerSide(primaryWinner)) {
            return;
        }
        for (CampaignFleetAPI enemy : battle.getNonPlayerSideSnapshot()) {
            if (OdysseyStrandedFleetsScript.isLeagueSurvivorFleet(enemy)
                    && battle.wasFleetDefeated(enemy, primaryWinner)) {
                LeagueSurvivorRescueDialogPlugin.request();
                return;
            }
        }
    }

    @Override public void advance(float amount) {
        LeagueSurvivorRescueDialogPlugin.tryShowPending();
    }
    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return false; }
}
