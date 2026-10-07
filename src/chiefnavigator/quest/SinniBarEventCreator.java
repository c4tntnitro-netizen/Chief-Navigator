package chiefnavigator.quest;

import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarEvent;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventCreator;

public final class SinniBarEventCreator extends BaseBarEventCreator {
    @Override public PortsideBarEvent createBarEvent() { return new SinniBarEvent(); }
    @Override public float getBarEventFrequencyWeight() { return 100f; }
    @Override public boolean isPriority() { return true; }
    @Override public String getBarEventId() { return SinniBarEvent.EVENT_ID; }
    @Override public float getBarEventTimeoutDuration() { return 30f; }
    @Override public float getBarEventAcceptedTimeoutDuration() { return 30f; }
}
