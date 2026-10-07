package chiefnavigator.campaign;

import chiefnavigator.quest.OdysseyExpanseSystem;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import java.awt.Color;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

/** Enlarged nascent-well visual with a view into Avici at its center. */
public final class MessinaGravityWellEntityPlugin
        extends BaseCustomEntityPlugin {
    /** Four times the radius passed to the original vanilla well renderer. */
    private static final float MODEL_RADIUS = 200f;
    private static final float APERTURE_RADIUS = 340f;
    private static final float OUTER_RADIUS = 630f;
    private static final float RING_FADE_START = OUTER_RADIUS * 0.75f;
    private static final int PARTICLE_GRID = 12;
    private static final int CIRCLE_STEPS = 64;
    private static final Color WELL_COLOR = new Color(175, 50, 255);

    private transient SpriteAPI particle;
    private transient SpriteAPI interior;
    private float elapsed;

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
        particle = Global.getSettings().getSprite(
                "graphics/fx/particlealpha32sq.png");
        interior = Global.getSettings().getSprite(
                "misc",
                "chief_navigator_messina_gravity_inside");
    }

    @Override
    public void advance(float amount) {
        elapsed += amount;
    }

    @Override
    public float getRenderRange() {
        return 1500f;
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (layer != CampaignEngineLayers.ASTEROIDS) return;
        if (particle == null || interior == null) loadSprites();

        float alpha = viewport.getAlphaMult();
        if (alpha <= 0f) return;
        Vector2f center = entity.getLocation();

        renderLayeredRing(center, alpha);
        renderInterior(center, alpha);
        renderApertureRim(center, alpha);
    }

    /**
     * Reproduces the six overlaid vanilla gravity-well particles at fourfold
     * scale, then feathers their combined alpha over the outermost 25%.
     */
    private void renderLayeredRing(Vector2f center, float alpha) {
        for (int index = 0; index < 6; index++) {
            float angle = index * 60f;
            float animated = elapsed * 0.25f
                    + (float) Math.toRadians(angle * 2.3f);
            float width = MODEL_RADIUS * 4f
                    * (Math.abs((float) Math.cos(animated)) * 0.75f + 0.35f);
            float height = MODEL_RADIUS * 4f
                    * (Math.abs((float) Math.sin(animated)) * 0.75f + 0.35f);
            float radians = (float) Math.toRadians(angle);
            float particleX = center.x
                    + (float) Math.cos(radians) * MODEL_RADIUS * 0.95f;
            float particleY = center.y
                    + (float) Math.sin(radians) * MODEL_RADIUS * 0.95f;
            renderParticleMesh(
                    center,
                    particleX,
                    particleY,
                    width,
                    height,
                    radians,
                    alpha * 0.25f);
        }
    }

    private void renderParticleMesh(
            Vector2f wellCenter,
            float particleX,
            float particleY,
            float width,
            float height,
            float rotation,
            float alpha) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        particle.bindTexture();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        float texX = particle.getTexX();
        float texY = particle.getTexY();
        float texWidth = particle.getTexWidth();
        float texHeight = particle.getTexHeight();
        float cos = (float) Math.cos(rotation);
        float sin = (float) Math.sin(rotation);

        GL11.glBegin(GL11.GL_QUADS);
        for (int y = 0; y < PARTICLE_GRID; y++) {
            float v0 = y / (float) PARTICLE_GRID;
            float v1 = (y + 1f) / PARTICLE_GRID;
            for (int x = 0; x < PARTICLE_GRID; x++) {
                float u0 = x / (float) PARTICLE_GRID;
                float u1 = (x + 1f) / PARTICLE_GRID;
                renderParticleVertex(
                        wellCenter, particleX, particleY,
                        width, height, cos, sin,
                        u0, v0, texX, texY, texWidth, texHeight, alpha);
                renderParticleVertex(
                        wellCenter, particleX, particleY,
                        width, height, cos, sin,
                        u1, v0, texX, texY, texWidth, texHeight, alpha);
                renderParticleVertex(
                        wellCenter, particleX, particleY,
                        width, height, cos, sin,
                        u1, v1, texX, texY, texWidth, texHeight, alpha);
                renderParticleVertex(
                        wellCenter, particleX, particleY,
                        width, height, cos, sin,
                        u0, v1, texX, texY, texWidth, texHeight, alpha);
            }
        }
        GL11.glEnd();
        GL11.glPopAttrib();
    }

    private void renderParticleVertex(
            Vector2f wellCenter,
            float particleX,
            float particleY,
            float width,
            float height,
            float cos,
            float sin,
            float u,
            float v,
            float texX,
            float texY,
            float texWidth,
            float texHeight,
            float alpha) {
        float localX = (u - 0.5f) * width;
        float localY = (v - 0.5f) * height;
        float worldX = particleX + localX * cos - localY * sin;
        float worldY = particleY + localX * sin + localY * cos;
        float dx = worldX - wellCenter.x;
        float dy = worldY - wellCenter.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        float radialAlpha = 1f;
        if (distance > RING_FADE_START) {
            radialAlpha = (OUTER_RADIUS - distance)
                    / (OUTER_RADIUS - RING_FADE_START);
            radialAlpha = Math.max(0f, Math.min(1f, radialAlpha));
        }

        GL11.glColor4f(
                WELL_COLOR.getRed() / 255f,
                WELL_COLOR.getGreen() / 255f,
                WELL_COLOR.getBlue() / 255f,
                alpha * radialAlpha);
        GL11.glTexCoord2f(texX + u * texWidth, texY + v * texHeight);
        GL11.glVertex2f(worldX, worldY);
    }

    /** Draws the cleaned Avici screenshot as a circular, softly edged view. */
    private void renderInterior(Vector2f center, float alpha) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        interior.bindTexture();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        float solidFraction = 0.9f;
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        interiorVertex(center.x, center.y, 0.5f, 0.5f, alpha);
        for (int step = 0; step <= CIRCLE_STEPS; step++) {
            float angle = (float) (Math.PI * 2d * step / CIRCLE_STEPS);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            interiorVertex(
                    center.x + cos * APERTURE_RADIUS * solidFraction,
                    center.y + sin * APERTURE_RADIUS * solidFraction,
                    0.5f + cos * 0.5f * solidFraction,
                    0.5f + sin * 0.5f * solidFraction,
                    alpha);
        }
        GL11.glEnd();

        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        for (int step = 0; step <= CIRCLE_STEPS; step++) {
            float angle = (float) (Math.PI * 2d * step / CIRCLE_STEPS);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            interiorVertex(
                    center.x + cos * APERTURE_RADIUS * solidFraction,
                    center.y + sin * APERTURE_RADIUS * solidFraction,
                    0.5f + cos * 0.5f * solidFraction,
                    0.5f + sin * 0.5f * solidFraction,
                    alpha);
            interiorVertex(
                    center.x + cos * APERTURE_RADIUS,
                    center.y + sin * APERTURE_RADIUS,
                    0.5f + cos * 0.5f,
                    0.5f + sin * 0.5f,
                    0f);
        }
        GL11.glEnd();
        GL11.glPopAttrib();
    }

    private void interiorVertex(
            float x,
            float y,
            float u,
            float v,
            float alpha) {
        GL11.glColor4f(1f, 1f, 1f, alpha);
        GL11.glTexCoord2f(
                interior.getTexX() + u * interior.getTexWidth(),
                interior.getTexY() + v * interior.getTexHeight());
        GL11.glVertex2f(x, y);
    }

    private void renderApertureRim(Vector2f center, float alpha) {
        float innerRadius = APERTURE_RADIUS * 0.96f;
        float outerRadius = APERTURE_RADIUS * 1.08f;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        for (int step = 0; step <= CIRCLE_STEPS; step++) {
            float angle = (float) (Math.PI * 2d * step / CIRCLE_STEPS);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            GL11.glColor4f(0.69f, 0.2f, 1f, alpha * 0.55f);
            GL11.glVertex2f(
                    center.x + cos * innerRadius,
                    center.y + sin * innerRadius);
            GL11.glColor4f(0.69f, 0.2f, 1f, 0f);
            GL11.glVertex2f(
                    center.x + cos * outerRadius,
                    center.y + sin * outerRadius);
        }
        GL11.glEnd();
        GL11.glPopAttrib();
    }
}
