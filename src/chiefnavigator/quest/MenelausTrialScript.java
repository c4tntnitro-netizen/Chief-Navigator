package chiefnavigator.quest;

import chiefnavigator.campaign.DomainSecurityIFFAuthorization;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.util.IntervalUtil;

/** Lightweight persistent objective watcher for Menelaus's Labors. */
public final class MenelausTrialScript implements EveryFrameScript {
    private final IntervalUtil interval = new IntervalUtil(0.4f, 0.7f);

    @Override
    public void advance(float amount) {
        if (Global.getSector() == null) return;
        interval.advance(amount);
        if (!interval.intervalElapsed()) return;
        DriftingWallLaborSupport.advance();
        // Diplomacy is deliberately fixed: other campaign scripts and old
        // saves cannot make Spartan neutral to either Threat faction again.
        TaskForceSpartanFaction.enforceRelations();
        StarSystemAPI system = OdysseyExpanseSystem.findExisting();
        if (system != null) MenelausTrial.refreshProgress(system);
        MenelausSensorLabor.refreshWorldState();
        if (MenelausTrial.isAccepted()) {
            DomainSecurityIFFAuthorization.synchronizeAuthorization();
            MenelausTrialIntel.syncProgress(false);
        }
        MenelausCompletionDialogPlugin.tryShowPending();
        if (MenelausTrial.isComplete()) {
            PostLaborHuntIntel.sync(null);
            MenelausTrial.ensurePostTrialConsequences();
        }
    }

    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return true; }
}
