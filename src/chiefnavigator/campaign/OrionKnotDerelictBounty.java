package chiefnavigator.campaign;

import com.fs.starfarer.api.impl.campaign.missions.cb.CBDerelict;

/** Retained so development saves containing the retired creator still load. */
@Deprecated
public final class OrionKnotDerelictBounty extends CBDerelict {
    @Override
    public String getId() {
        // Native offer dialogue and completion tracking use this identifier.
        return CBDerelict.class.getSimpleName();
    }
}
