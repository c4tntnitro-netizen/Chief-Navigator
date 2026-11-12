package chiefnavigator.quest;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.listeners.ExtraSalvageShownListener;

/** Persists station salvage; exposing loot alone does not record component recovery. */
public final class ResearchStationSalvageListener
        implements ExtraSalvageShownListener {
    @Override
    public void reportExtraSalvageShown(SectorEntityToken entity) {
        OdysseyExpanseSystem.markResearchStationSalvaged(entity);
        MenelausSensorLabor.markDoomRecovered(entity);
    }
}
