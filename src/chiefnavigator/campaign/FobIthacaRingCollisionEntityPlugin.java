package chiefnavigator.campaign;

import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;

/** Invisible campaign collision bead following FOB Ithaca's surviving ring. */
public final class FobIthacaRingCollisionEntityPlugin
        extends BaseCustomEntityPlugin {
    @Override
    public float getRenderRange() {
        return 0f;
    }
}
