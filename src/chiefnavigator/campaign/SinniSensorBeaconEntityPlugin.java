package chiefnavigator.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;

import java.awt.Color;

/** Renders a Remnant-style warning beacon without its automatic warning ping. */
public final class SinniSensorBeaconEntityPlugin extends BaseCustomEntityPlugin {
    public static final String ENTITY_TYPE = "chief_navigator_sensor_echo_beacon";

    private static final Color GLOW_COLOR = new Color(255, 100, 0);
    private float phase;
    private transient SpriteAPI sprite;
    private transient SpriteAPI glow;

    @Override
    public void init(SectorEntityToken entity, Object pluginParams) {
        super.init(entity, pluginParams);
        loadSprites();
    }

    private Object readResolve() {
        loadSprites();
        return this;
    }

    private void loadSprites() {
        sprite = Global.getSettings().getSprite(
                "campaignEntities", "warning_beacon");
        glow = Global.getSettings().getSprite(
                "campaignEntities", "warning_beacon_glow");
    }

    @Override
    public void advance(float amount) {
        phase = (phase + amount * 0.6f) % 1f;
    }

    @Override
    public float getRenderRange() {
        return 180f;
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (layer != CampaignEngineLayers.STATIONS) return;
        if (sprite == null || glow == null) loadSprites();

        float alpha = viewport.getAlphaMult()
                * entity.getSensorFaderBrightness()
                * entity.getSensorContactFaderBrightness();
        if (alpha <= 0f) return;

        float width = entity.getCustomEntitySpec().getSpriteWidth();
        float height = entity.getCustomEntitySpec().getSpriteHeight();
        float angle = entity.getFacing() - 90f;
        float x = entity.getLocation().x;
        float y = entity.getLocation().y;

        sprite.setNormalBlend();
        sprite.setColor(Color.WHITE);
        sprite.setAlphaMult(alpha);
        sprite.setAngle(angle);
        sprite.setSize(width, height);
        sprite.renderAtCenter(x, y);

        float pulse = 0.5f + 0.5f
                * (float) Math.sin(phase * Math.PI * 2.0);
        glow.setAdditiveBlend();
        glow.setColor(GLOW_COLOR);
        glow.setAlphaMult(alpha * (0.35f + pulse * 0.65f));
        glow.setAngle(angle);
        glow.setSize(width * (1.05f + pulse * 0.15f),
                height * (1.05f + pulse * 0.15f));
        glow.renderAtCenter(x, y);
    }
}
