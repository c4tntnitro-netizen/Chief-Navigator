package chiefnavigator.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import java.awt.Color;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

/** A hot realspace aperture leaking darkness through a hyperspace corona. */
public final class MessinaBreachEntityPlugin extends BaseCustomEntityPlugin {
    private static final float APERTURE_RADIUS = 245f;
    private static final float RING_RADIUS = 345f;
    private static final int CIRCLE_STEPS = 48;

    private transient SpriteAPI stars;
    private transient SpriteAPI corona;
    private transient SpriteAPI ring;
    private transient SpriteAPI ring2;
    private transient SpriteAPI cloud;
    private transient SpriteAPI cloud2;
    private float phase;

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
        stars = Global.getSettings().getSprite("misc", "wormhole_stars");
        corona = Global.getSettings().getSprite("misc", "wormhole_corona");
        ring = Global.getSettings().getSprite("misc", "wormhole_ring");
        ring2 = Global.getSettings().getSprite("misc", "wormhole_ring2");
        cloud = Global.getSettings().getSprite("misc", "nebula_particles");
        cloud2 = Global.getSettings().getSprite("misc", "fx_particles1");
    }

    @Override
    public void advance(float amount) {
        phase += amount;
    }

    @Override
    public float getRenderRange() {
        return 1800f;
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (layer != CampaignEngineLayers.ABOVE_STATIONS) return;
        if (stars == null || cloud == null) loadSprites();

        float alpha = viewport.getAlphaMult();
        if (alpha <= 0f) return;
        Vector2f location = entity.getLocation();
        float rotation = entity.getFacing() + phase * 3f;
        float pulse = 0.88f + 0.12f * (float) Math.sin(phase * 1.45f);

        renderVapor(location, alpha, rotation);
        drawSprite(
                corona,
                location,
                RING_RADIUS * 3.35f,
                rotation * 0.35f,
                new Color(105, 24, 245),
                alpha * 0.55f * pulse,
                true);

        renderCoreGradient(location, APERTURE_RADIUS, alpha);
        renderTexturedDisk(
                stars,
                location,
                APERTURE_RADIUS * 0.94f,
                rotation * 0.4f,
                new Color(255, 92, 36),
                alpha * 0.78f);
        renderCoreFilaments(location, APERTURE_RADIUS * 0.82f, alpha * pulse);

        drawSprite(
                ring2,
                location,
                RING_RADIUS * 2.16f,
                rotation + phase * 14f,
                new Color(92, 36, 255),
                alpha * 0.94f,
                true);
        drawSprite(
                ring,
                location,
                RING_RADIUS * 2f,
                -rotation - phase * 10f,
                new Color(255, 70, 235),
                alpha * 0.9f,
                true);

        // The dark material crosses in front of the lower-left rim, matching
        // the silhouette of the supplied reference instead of bisecting it.
        renderSpill(location, alpha * 0.34f, 1.2f, true);
        renderSpill(location, alpha, 1f, false);
    }

    private void renderVapor(Vector2f location, float alpha, float rotation) {
        drawSprite(
                cloud,
                offset(location, 105f, -430f),
                560f,
                rotation * 0.55f,
                new Color(116, 34, 215),
                alpha * 0.23f,
                true);
        drawSprite(
                cloud2,
                offset(location, 165f, -690f),
                470f,
                -rotation * 0.35f,
                new Color(91, 25, 160),
                alpha * 0.16f,
                true);
        drawSprite(
                cloud,
                offset(location, -70f, -300f),
                380f,
                rotation * 0.8f + 70f,
                new Color(198, 45, 240),
                alpha * 0.12f,
                true);
    }

    private void renderCoreGradient(
            Vector2f location,
            float radius,
            float alpha) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glColor4f(0.035f, 0.002f, 0.006f, alpha);
        GL11.glVertex2f(location.x, location.y);
        for (int step = 0; step <= CIRCLE_STEPS; step++) {
            float angle = 360f * step / CIRCLE_STEPS;
            float radians = (float) Math.toRadians(angle);
            GL11.glColor4f(0.82f, 0.075f, 0.015f, alpha * 0.92f);
            GL11.glVertex2f(
                    location.x + (float) Math.cos(radians) * radius,
                    location.y + (float) Math.sin(radians) * radius);
        }
        GL11.glEnd();
        GL11.glPopAttrib();
    }

    private void renderTexturedDisk(
            SpriteAPI sprite,
            Vector2f location,
            float radius,
            float angle,
            Color color,
            float alpha) {
        float rotation = (float) Math.toRadians(angle);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        sprite.bindTexture();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(
                color.getRed() / 255f,
                color.getGreen() / 255f,
                color.getBlue() / 255f,
                alpha);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glTexCoord2f(0.5f, 0.5f);
        GL11.glVertex2f(location.x, location.y);
        for (int step = 0; step <= CIRCLE_STEPS; step++) {
            float arc = (float) (Math.PI * 2d * step / CIRCLE_STEPS);
            float worldCos = (float) Math.cos(arc);
            float worldSin = (float) Math.sin(arc);
            float textureCos = (float) Math.cos(arc + rotation);
            float textureSin = (float) Math.sin(arc + rotation);
            GL11.glTexCoord2f(
                    0.5f + textureCos * 0.5f,
                    0.5f + textureSin * 0.5f);
            GL11.glVertex2f(
                    location.x + worldCos * radius,
                    location.y + worldSin * radius);
        }
        GL11.glEnd();
        GL11.glPopAttrib();
    }

    private void renderCoreFilaments(
            Vector2f location,
            float maxRadius,
            float alpha) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glLineWidth(3f);
        for (int arm = 0; arm < 3; arm++) {
            GL11.glColor4f(1f, 0.27f, 0.035f, alpha * (0.72f - arm * 0.12f));
            GL11.glBegin(GL11.GL_LINE_STRIP);
            for (int step = 0; step <= 34; step++) {
                float progress = step / 34f;
                float angle = phase * 7f + arm * 120f + progress * 285f;
                float radians = (float) Math.toRadians(angle);
                float radius = 24f + maxRadius * progress * 0.88f;
                GL11.glVertex2f(
                        location.x + (float) Math.cos(radians) * radius,
                        location.y + (float) Math.sin(radians) * radius * 0.72f);
            }
            GL11.glEnd();
        }
        GL11.glPopAttrib();
    }

    private void renderSpill(
            Vector2f location,
            float alpha,
            float widthScale,
            boolean glow) {
        float direction = (float) Math.toRadians(220f);
        float alongX = (float) Math.cos(direction);
        float alongY = (float) Math.sin(direction);
        float acrossX = -alongY;
        float acrossY = alongX;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(
                GL11.GL_SRC_ALPHA,
                glow ? GL11.GL_ONE : GL11.GL_ONE_MINUS_SRC_ALPHA);
        if (glow) GL11.glColor4f(0.34f, 0.04f, 0.62f, alpha);
        else GL11.glColor4f(0.002f, 0f, 0.008f, alpha);

        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        for (int step = 0; step <= 16; step++) {
            float progress = step / 16f;
            float distance = 55f + progress * 980f;
            float wobble = (float) Math.sin(step * 1.83f + phase * 0.55f)
                    * 26f * progress;
            float width = (58f + progress * 185f
                    + (float) Math.sin(step * 2.41f) * 17f * progress)
                    * widthScale;
            float centerX = location.x + alongX * distance + acrossX * wobble;
            float centerY = location.y + alongY * distance + acrossY * wobble;
            GL11.glVertex2f(centerX + acrossX * width, centerY + acrossY * width);
            GL11.glVertex2f(centerX - acrossX * width, centerY - acrossY * width);
        }
        GL11.glEnd();
        GL11.glPopAttrib();

        Vector2f end = new Vector2f(
                location.x + alongX * 1010f,
                location.y + alongY * 1010f);
        Color color = glow ? new Color(88, 10, 158) : new Color(1, 0, 3);
        renderBlob(
                new Vector2f(end.x + acrossX * 150f, end.y + acrossY * 150f),
                112f * widthScale,
                color,
                alpha,
                glow);
        renderBlob(end, 142f * widthScale, color, alpha, glow);
        renderBlob(
                new Vector2f(end.x - acrossX * 145f, end.y - acrossY * 145f),
                98f * widthScale,
                color,
                alpha,
                glow);
        renderBlob(
                new Vector2f(
                        location.x + alongX * 92f,
                        location.y + alongY * 92f),
                92f * widthScale,
                color,
                alpha,
                glow);
    }

    private void renderBlob(
            Vector2f location,
            float radius,
            Color color,
            float alpha,
            boolean glow) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(
                GL11.GL_SRC_ALPHA,
                glow ? GL11.GL_ONE : GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(
                color.getRed() / 255f,
                color.getGreen() / 255f,
                color.getBlue() / 255f,
                alpha);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(location.x, location.y);
        for (int step = 0; step <= 24; step++) {
            float angle = (float) (Math.PI * 2d * step / 24d);
            GL11.glVertex2f(
                    location.x + (float) Math.cos(angle) * radius,
                    location.y + (float) Math.sin(angle) * radius);
        }
        GL11.glEnd();
        GL11.glPopAttrib();
    }

    private void drawSprite(
            SpriteAPI sprite,
            Vector2f location,
            float size,
            float angle,
            Color color,
            float alpha,
            boolean additive) {
        sprite.setSize(size, size);
        sprite.setAngle(angle);
        sprite.setColor(color);
        sprite.setAlphaMult(alpha);
        if (additive) sprite.setAdditiveBlend();
        else sprite.setNormalBlend();
        sprite.renderAtCenter(location.x, location.y);
    }

    private Vector2f offset(Vector2f origin, float x, float y) {
        return new Vector2f(origin.x + x, origin.y + y);
    }
}
