package chiefnavigator.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import java.awt.Color;

/** Fixed campaign presentation of the rebuilt Drifting Wall base. */
public final class DriftingWallCampaignEntityPlugin
        extends BaseCustomEntityPlugin {
    private static final String SPRITE_PATH =
            "graphics/ships/chief_navigator_drifting_wall_base_domain_v2.png";
    private static final float WIDTH = 850f;
    private static final float HEIGHT = 334.305f;
    private static final float ANGLE = 0f;
    // This sprite is much wider than the entity's interaction radius.  Keep the
    // plugin active across the whole system so an on-screen edge never pops in.
    private static final float ALWAYS_VISIBLE_RENDER_RANGE = 100000f;
    private static final Color CAMPAIGN_TINT = new Color(180, 176, 165);

    private transient SpriteAPI sprite;

    @Override
    public void init(SectorEntityToken entity, Object pluginParams) {
        super.init(entity, pluginParams);
        loadSprite();
    }

    private Object readResolve() {
        sprite = null;
        return this;
    }

    private void loadSprite() {
        sprite = Global.getSettings().getSprite(SPRITE_PATH);
    }

    @Override
    public void advance(float amount) {
    }

    @Override
    public float getRenderRange() {
        return ALWAYS_VISIBLE_RENDER_RANGE;
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (layer != CampaignEngineLayers.STATIONS) return;
        if (sprite == null) loadSprite();
        float alpha = viewport.getAlphaMult();
        if (alpha <= 0f) return;
        sprite.setSize(WIDTH, HEIGHT);
        sprite.setAngle(ANGLE);
        sprite.setColor(CAMPAIGN_TINT);
        sprite.setAlphaMult(alpha);
        sprite.setNormalBlend();
        sprite.renderAtCenter(entity.getLocation().x, entity.getLocation().y);
    }
}
