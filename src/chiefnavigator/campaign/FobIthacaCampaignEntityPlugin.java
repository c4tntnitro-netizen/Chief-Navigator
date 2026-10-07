package chiefnavigator.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SectorEntityToken.VisibilityLevel;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.io.IOException;

/** Fixed campaign-scale rendering for the complete FOB Ithaca station. */
public final class FobIthacaCampaignEntityPlugin
        extends BaseCustomEntityPlugin {
    // Visual tuning controls; see docs/FOB_ITHACA_PLACEMENT.md.
    private static final float STATION_SIZE = 1500f;
    private static final float STATION_ANGLE = 0f;
    private static final float OFFSET_X = 0f;
    private static final float OFFSET_Y = 0f;
    private static final float ORBITAL_RING_SIZE = 3800f;
    // The ring extends far beyond the central interaction token.  Do not let
    // campaign culling use that small token radius as a proxy for visibility.
    private static final float ALWAYS_VISIBLE_RENDER_RANGE = 100000f;
    private static final float ORBITAL_RING_ANGLE = 0f;
    private static final float ORBITAL_GLOW_ALPHA = 0f;
    private static final float STATION_GLOW_ALPHA = 0f;
    private static final float TWINKLE_SPEED = 0.45f;
    private static final float TWINKLE_DEPTH = 0.07f;
    private static final float STATION_TWINKLE_OFFSET = 1.9f;
    private static final float TOOLTIP_WIDTH = 290f;
    private static final float TOOLTIP_ICON_SIZE = 64f;
    private static final Color STRUCTURE_TINT =
            new Color(115, 110, 115);
    private static final Color ORBITAL_GLOW_TINT =
            new Color(255, 255, 220);
    private static final String SPRITE_CATEGORY = "misc";
    private static final String SPRITE_KEY =
            "chief_navigator_fob_ithaca_station";
    private static final String TEXTURE_PATH =
            "graphics/campaign/fob_ithaca/ithaca_station.png";
    private static final String STATION_GLOW_SPRITE_KEY =
            "chief_navigator_fob_ithaca_station_glow";
    private static final String STATION_GLOW_TEXTURE_PATH =
            "graphics/campaign/fob_ithaca/ithaca_station_glow.png";
    private static final String ORBITAL_RING_SPRITE_KEY =
            "chief_navigator_fob_ithaca_orbital_ring";
    private static final String ORBITAL_RING_TEXTURE_PATH =
            "graphics/campaign/fob_ithaca/ithaca_orbital_ring_v4.png";
    private static final String ORBITAL_GLOW_SPRITE_KEY =
            "chief_navigator_fob_ithaca_orbital_ring_glow";
    private static final String ORBITAL_GLOW_TEXTURE_PATH =
            "graphics/campaign/fob_ithaca/ithaca_orbital_ring_glow_v4.png";

    private static boolean texturesLoaded;
    private transient SpriteAPI stationSprite;
    private transient SpriteAPI stationGlowSprite;
    private transient SpriteAPI orbitalRingSprite;
    private transient SpriteAPI orbitalGlowSprite;
    private transient float twinklePhase;

    @Override
    public void init(SectorEntityToken entity, Object pluginParams) {
        super.init(entity, pluginParams);
        loadSprite();
    }

    private Object readResolve() {
        stationSprite = null;
        stationGlowSprite = null;
        orbitalRingSprite = null;
        orbitalGlowSprite = null;
        twinklePhase = 0f;
        return this;
    }

    private void loadSprite() {
        ensureTexturesLoaded();
        stationSprite = Global.getSettings().getSprite(
                SPRITE_CATEGORY,
                SPRITE_KEY);
        stationGlowSprite = Global.getSettings().getSprite(
                SPRITE_CATEGORY,
                STATION_GLOW_SPRITE_KEY);
        orbitalRingSprite = Global.getSettings().getSprite(
                SPRITE_CATEGORY,
                ORBITAL_RING_SPRITE_KEY);
        orbitalGlowSprite = Global.getSettings().getSprite(
                SPRITE_CATEGORY,
                ORBITAL_GLOW_SPRITE_KEY);
        validateSprite(stationSprite, TEXTURE_PATH);
        validateSprite(stationGlowSprite, STATION_GLOW_TEXTURE_PATH);
        validateSprite(orbitalRingSprite, ORBITAL_RING_TEXTURE_PATH);
        validateSprite(orbitalGlowSprite, ORBITAL_GLOW_TEXTURE_PATH);
    }

    private static void validateSprite(SpriteAPI sprite, String path) {
        if (sprite == null || sprite.getTextureId() == 0) {
            throw new IllegalStateException(
                    "FOB Ithaca texture was not uploaded: " + path);
        }
    }

    private static synchronized void ensureTexturesLoaded() {
        if (texturesLoaded) return;
        loadTexture(TEXTURE_PATH);
        loadTexture(STATION_GLOW_TEXTURE_PATH);
        loadTexture(ORBITAL_RING_TEXTURE_PATH);
        loadTexture(ORBITAL_GLOW_TEXTURE_PATH);
        texturesLoaded = true;
    }

    private static void loadTexture(String path) {
        try {
            Global.getSettings().loadTexture(path);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to load FOB Ithaca texture: " + path,
                    exception);
        }
    }

    @Override
    public void advance(float amount) {
        twinklePhase += amount * TWINKLE_SPEED;
        float fullCycle = (float) (Math.PI * 2.0);
        if (twinklePhase >= fullCycle) {
            twinklePhase %= fullCycle;
        }
    }

    @Override
    public float getRenderRange() {
        return ALWAYS_VISIBLE_RENDER_RANGE;
    }

    @Override
    public boolean hasCustomMapTooltip() {
        return true;
    }

    @Override
    public float getMapTooltipWidth() {
        return TOOLTIP_WIDTH;
    }

    @Override
    public boolean isMapTooltipExpandable() {
        return false;
    }

    @Override
    public void createMapTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        tooltip.addTitle("FOB Ithaca");
        addIdentityBlock(tooltip, 6f);
    }

    @Override
    public void appendToCampaignTooltip(
            TooltipMakerAPI tooltip, VisibilityLevel level) {
        addIdentityBlock(tooltip, 6f);
    }

    private static void addIdentityBlock(
            TooltipMakerAPI tooltip, float pad) {
        TooltipMakerAPI withIcon = tooltip.beginImageWithText(
                TEXTURE_PATH, TOOLTIP_ICON_SIZE);
        withIcon.addPara(
                "Domain Macrocomplex",
                0f,
                Misc.getHighlightColor(),
                "Domain Macrocomplex");
        withIcon.addPara(
                "Ancient Task Force Spartan command installation.", 4f);
        tooltip.addImageWithText(pad);
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (layer != CampaignEngineLayers.STATIONS) return;
        if (stationSprite == null
                || stationGlowSprite == null
                || orbitalRingSprite == null
                || orbitalGlowSprite == null) {
            loadSprite();
        }

        float alpha = viewport.getAlphaMult();
        if (alpha <= 0f) return;

        float centerX = entity.getLocation().x + OFFSET_X;
        float centerY = entity.getLocation().y + OFFSET_Y;
        float ringTwinkle = getTwinkle(0f);
        float stationTwinkle = getTwinkle(STATION_TWINKLE_OFFSET);

        renderNormal(
                orbitalRingSprite,
                ORBITAL_RING_SIZE,
                ORBITAL_RING_ANGLE,
                STRUCTURE_TINT,
                alpha,
                centerX,
                centerY);

        renderAdditive(
                orbitalGlowSprite,
                ORBITAL_RING_SIZE,
                ORBITAL_RING_ANGLE,
                ORBITAL_GLOW_TINT,
                alpha * ORBITAL_GLOW_ALPHA * ringTwinkle,
                centerX,
                centerY);

        renderNormal(
                stationSprite,
                STATION_SIZE,
                STATION_ANGLE,
                STRUCTURE_TINT,
                alpha,
                centerX,
                centerY);

        renderAdditive(
                stationGlowSprite,
                STATION_SIZE,
                STATION_ANGLE,
                ORBITAL_GLOW_TINT,
                alpha * STATION_GLOW_ALPHA * stationTwinkle,
                centerX,
                centerY);
    }

    private float getTwinkle(float offset) {
        float phase = twinklePhase + offset;
        float wave =
                0.55f * (float) Math.sin(phase)
                + 0.30f * (float) Math.sin(phase * 3f + 1.4f)
                + 0.15f * (float) Math.sin(phase * 7f + 0.3f);
        return 1f + TWINKLE_DEPTH * wave;
    }

    private static void renderNormal(
            SpriteAPI sprite,
            float size,
            float angle,
            Color tint,
            float alpha,
            float centerX,
            float centerY) {
        sprite.setSize(size, size);
        sprite.setAngle(angle);
        sprite.setNormalBlend();
        sprite.setColor(tint);
        sprite.setAlphaMult(alpha);
        sprite.renderAtCenter(centerX, centerY);
    }

    private static void renderAdditive(
            SpriteAPI sprite,
            float size,
            float angle,
            Color tint,
            float alpha,
            float centerX,
            float centerY) {
        sprite.setSize(size, size);
        sprite.setAngle(angle);
        sprite.setAdditiveBlend();
        sprite.setColor(tint);
        sprite.setAlphaMult(alpha);
        sprite.renderAtCenter(centerX, centerY);
    }
}
