package chiefnavigator.quest;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.listeners.ExtraSalvageShownListener;

/** Persists completion when vanilla salvage exposes a station's component. */
public final class ResearchStationSalvageListener
        implements ExtraSalvageShownListener {
    @Override
    public void reportExtraSalvageShown(SectorEntityToken entity) {
        OdysseyExpanseSystem.markResearchStationSalvaged(entity);
        MenelausSensorLabor.markDoomRecovered(entity);
    }
}
