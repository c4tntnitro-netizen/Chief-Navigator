package chiefnavigator.quest;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import java.lang.ref.WeakReference;

/** Lightweight persistent objective watcher for Menelaus's Labors. */
public final class MenelausTrialScript implements EveryFrameScript {
    private final IntervalUtil interval = new IntervalUtil(0.4f, 0.7f);
    private boolean initialReconciliation;
    private long observedRevision;
    private static WeakReference<SectorAPI> revisionSector = new WeakReference<>(null);
    private static long revision;

    /** Coalesces progress, cargo, objective and world events into one update. */
    public static void requestReconciliation() {
        currentRevision();
        revision++;
    }

    private static long currentRevision() {
        if (revisionSector.get() != Global.getSector()) {
            revisionSector = new WeakReference<>(Global.getSector());
            revision = 0;
        }
        return revision;
    }

    @Override
    public void advance(float amount) {
        if (Global.getSector() == null) return;
        interval.advance(amount);
        if (!interval.intervalElapsed()) return;
        DriftingWallLaborSupport.advance();
        // Diplomacy is deliberately fixed: other campaign scripts and old
        // saves cannot make Spartan neutral to either Threat faction again.
        TaskForceSpartanFaction.enforceDiplomacy();
        long requested = currentRevision();
        if (!initialReconciliation || observedRevision != requested) {
            observedRevision = requested;
            TaskForceSpartanFaction.enforceRelations();
            StarSystemAPI system = OdysseyExpanseSystem.findExisting();
            initialReconciliation = true;
            if (system != null) {
                // Includes sensor world state and Intel synchronization after
                // acceptance. Before acceptance only load-time cleanup is needed.
                MenelausTrial.refreshProgress(system);
            } else if (MenelausTrial.isAccepted()) {
                MenelausSensorLabor.refreshWorldState();
                MenelausTrialIntel.syncProgress(false);
            }
            if (MenelausTrial.isComplete()) {
                PostLaborHuntIntel.sync(null);
                MenelausTrial.ensurePostTrialConsequences();
            }
        }
        MenelausCompletionDialogPlugin.tryShowPending();
        FinalLaborGateScanFailureDialogPlugin.tryShowPending();
    }

    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return true; }
}
