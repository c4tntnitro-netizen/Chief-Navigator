package chiefnavigator.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

/** Rift-torpedo-style campaign presence drawn over Budai's pursuing fleet. */
public final class CharybdisCampaignVisualPlugin extends BaseCustomEntityPlugin {
    public static final String ENTITY_TYPE = "chief_navigator_charybdis_campaign_visual";
    public static final String ENTITY_ID = "chief_navigator_charybdis_campaign_visual";

    private static final String CORE_SPRITE = "graphics/missiles/rift_torpedo.png";
    private static final String GLOW_SPRITE = "graphics/fx/engineglow32b.png";
    private static final Color RIFT_RED = new Color(255, 60, 95);
    private static final Color RIFT_PINK = new Color(255, 125, 155);

    public static final class Params {
        public CampaignFleetAPI fleet;

        public Params(CampaignFleetAPI fleet) {
            this.fleet = fleet;
        }
    }

    private CampaignFleetAPI fleet;
    private float elapsed;
    private transient SpriteAPI core;
    private transient SpriteAPI glow;

    @Override
    public void init(SectorEntityToken entity, Object pluginParams) {
        super.init(entity, pluginParams);
        if (pluginParams instanceof Params) fleet = ((Params) pluginParams).fleet;
        loadSprites();
    }

    private Object readResolve() {
        loadSprites();
        return this;
    }

    private void loadSprites() {
        core = Global.getSettings().getSprite(CORE_SPRITE);
        glow = Global.getSettings().getSprite(GLOW_SPRITE);
        core.setAdditiveBlend();
        glow.setAdditiveBlend();
    }

    @Override
    public void advance(float amount) {
        elapsed += amount;
        if (fleet == null || fleet.isDespawning() || fleet.getContainingLocation() == null
                || entity.getContainingLocation() != fleet.getContainingLocation()) {
            // Older saves may contain this retired overlay token. Leave the
            // serialized entity untouched and inert rather than deleting it.
            return;
        }
        entity.setLocation(fleet.getLocation().x, fleet.getLocation().y);
        entity.setFacing(fleet.getFacing());
    }

    @Override
    public float getRenderRange() {
        return 0f;
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        // Retired non-destructively. The fleet sprite alone represents Budai.
    }

    private void drawGlow(float x, float y, float size, Color color,
                          float alpha, float angle) {
        glow.setSize(size, size);
        glow.setColor(color);
        glow.setAlphaMult(alpha);
        glow.setAngle(angle);
        glow.renderAtCenter(x, y);
    }

    private static Vector2f unit(float angle) {
        double radians = Math.toRadians(angle);
        return new Vector2f((float) Math.cos(radians), (float) Math.sin(radians));
    }
}
