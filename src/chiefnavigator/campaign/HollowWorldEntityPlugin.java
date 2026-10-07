package chiefnavigator.campaign;

import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;

/** Keeps Devoured Reach's hollow worlds rendered across the full system. */
public final class HollowWorldEntityPlugin extends BaseCustomEntityPlugin {
    private static final float SYSTEM_WIDE_RENDER_RANGE = 30000f;

    @Override
    public float getRenderRange() {
        return SYSTEM_WIDE_RENDER_RANGE;
    }
}
