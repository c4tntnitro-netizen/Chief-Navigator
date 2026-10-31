package chiefnavigator.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.BeamWeaponSpecAPI;
import java.awt.Color;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

/** Replaces only the lens beam's fringe; native core, hit glow and ray stay intact. */
public final class AbyssalBeamGradientRenderer extends BaseCombatLayeredRenderingPlugin {
    private static final String EMPTY_TEXTURE = "graphics/fx/empty.png";
    private static final String WEAPON_ID = "chief_navigator_drifting_wall_colossal_inert";
    private static final String ROUGH_FRINGE = "graphics/fx/beamfringe.png";
    private static final Color TEAL = new Color(38, 145, 158, 255);
    private static final Color RED = new Color(255, 30, 45, 155);
    private static final int SEGMENTS = 24;

    private final CombatEngineAPI engine;
    private final BeamAPI beam;
    private final SpriteAPI fringe;
    private final String originalTexture;
    private final float scrollSpeed;
    private float phase = 1f;
    private boolean installed;

    private AbyssalBeamGradientRenderer(CombatEngineAPI engine, BeamAPI beam,
            BeamWeaponSpecAPI spec, SpriteAPI fringe, String originalTexture) {
        super(CombatEngineLayers.ABOVE_SHIPS_LAYER);
        this.engine = engine;
        this.beam = beam;
        this.fringe = fringe;
        this.originalTexture = originalTexture;
        scrollSpeed = spec.getTextureScrollSpeed() / 128f * spec.getFringeScrollSpeedMult();
    }

    /** Hide the native fringe only after its visual replacement has registered. */
    public static AbyssalBeamGradientRenderer install(CombatEngineAPI engine, BeamAPI beam) {
        if (engine == null || beam == null || beam.getWeapon() == null
                || !WEAPON_ID.equals(beam.getWeapon().getId())
                || !(beam.getWeapon().getSpec() instanceof BeamWeaponSpecAPI)
                || !IthacaResearchUpgrades.hasUpgrade(
                        beam.getSource(), IthacaResearchUpgrades.ABYSSAL_LENS)) return null;
        BeamWeaponSpecAPI spec = (BeamWeaponSpecAPI) beam.getWeapon().getSpec();
        // Native enum texture styles leave getFringeTex null. This exact owned
        // weapon uses ROUGH, whose native fringe is beamfringe.png.
        String originalTexture = spec.getFringeTex() == null ? ROUGH_FRINGE : spec.getFringeTex();
        AbyssalBeamGradientRenderer renderer = null;
        try {
            SpriteAPI sprite = Global.getSettings().getSprite(originalTexture);
            if (sprite == null) return null;
            renderer = new AbyssalBeamGradientRenderer(engine, beam, spec, sprite, originalTexture);
            CombatEntityAPI visual = engine.addLayeredRenderingPlugin(renderer);
            if (visual == null) return null;
            if (renderer.entity == null) renderer.init(visual);
            beam.setFringeTexture(EMPTY_TEXTURE);
            renderer.installed = true;
            return renderer;
        } catch (RuntimeException failure) {
            if (renderer != null) renderer.cleanup();
            Global.getLogger(AbyssalBeamGradientRenderer.class).warn(
                    "Could not install Abyssal beam gradient; retaining native red fringe", failure);
            return null;
        }
    }

    /** The beam callback already receives native source-time-scaled combat time. */
    public void advanceTexture(float amount) {
        if (engine.isPaused()) return;
        phase -= amount * scrollSpeed;
        phase -= (float) Math.floor(phase);
    }

    @Override
    public void init(CombatEntityAPI entity) {
        super.init(entity);
        followBeam();
    }

    @Override
    public void advance(float amount) {
        followBeam();
    }

    private void followBeam() {
        if (entity != null) entity.getLocation().set(
                (beam.getFrom().x + beam.getTo().x) * 0.5f,
                (beam.getFrom().y + beam.getTo().y) * 0.5f);
    }

    @Override
    public float getRenderRadius() {
        return Vector2f.sub(beam.getTo(), beam.getFrom(), null).length() * 0.5f
                + beam.getWidth();
    }

    @Override
    public boolean isExpired() {
        if (!installed || engine.getBeams() == null) return true;
        for (BeamAPI active : engine.getBeams()) {
            if (active == beam) return false;
        }
        return true;
    }

    @Override
    public void cleanup() {
        if (installed) {
            installed = false;
            beam.setFringeTexture(originalTexture);
        }
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (layer != CombatEngineLayers.ABOVE_SHIPS_LAYER || viewport == null
                || isExpired() || beam.getSource().isPiece()) return;
        Vector2f from = beam.getFrom();
        Vector2f to = beam.getTo();
        float dx = to.x - from.x;
        float dy = to.y - from.y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float brightness = beam.getBrightness();
        float width = beam.getWidth() * brightness;
        if (length <= 0.01f || width <= 0.01f || brightness <= 0f) return;
        dx /= length;
        dy /= length;
        float nx = -dy * width * 0.5f;
        float ny = dx * width * 0.5f;
        float cap = width / 3f;
        float bodyEnd = length - cap * 0.5f;
        float texelLength = 128f * Math.max(0.01f, beam.getPixelsPerTexel());
        float textureEnd = phase + (length - 2f * cap) / texelLength;
        float level = (int) (255f * Math.min(1f, brightness)) / 255f;
        float alpha = (int) (255d * Math.pow(level, 2d)) / 255f;

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT
                | GL11.GL_CURRENT_BIT | GL11.GL_TEXTURE_BIT | GL11.GL_LIGHTING_BIT);
        try {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL11.glShadeModel(GL11.GL_SMOOTH);
            GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
            fringe.bindTexture();

            // The native tapered muzzle uses the same texture profile as its body.
            GL11.glBegin(GL11.GL_TRIANGLES);
            vertex(from, dx, dy, nx, ny, cap, -1f, phase, 0f, cap / length, alpha);
            vertex(from, dx, dy, nx, ny, cap, 0f, phase, 0.5f, cap / length, alpha);
            vertex(from, dx, dy, nx, ny, -cap, 0f, phase - 2f * cap / texelLength, 0f,
                    0f, alpha);
            vertex(from, dx, dy, nx, ny, cap, 1f, phase, 1f, cap / length, alpha);
            vertex(from, dx, dy, nx, ny, cap, 0f, phase, 0.5f, cap / length, alpha);
            vertex(from, dx, dy, nx, ny, -cap, 0f, phase - 2f * cap / texelLength, 1f,
                    0f, alpha);
            GL11.glEnd();

            // Colors use distance along the real, collision-truncated ray.
            GL11.glBegin(GL11.GL_QUAD_STRIP);
            for (int i = 0; i <= SEGMENTS; i++) {
                float t = i / (float) SEGMENTS;
                float along = cap + (bodyEnd - cap) * t;
                float u = phase + (textureEnd - phase) * t;
                vertex(from, dx, dy, nx, ny, along, -1f, u, 0f, along / length, alpha);
                vertex(from, dx, dy, nx, ny, along, 1f, u, 1f, along / length, alpha);
            }
            GL11.glEnd();

            // Retain the native soft overshoot at the endpoint, separate from damage.
            float fadeEnd = bodyEnd + width - cap * 0.5f;
            float fadeTextureEnd = textureEnd + width / texelLength;
            GL11.glBegin(GL11.GL_QUAD_STRIP);
            vertex(from, dx, dy, nx, ny, bodyEnd, -1f, textureEnd, 0f, bodyEnd / length, alpha);
            vertex(from, dx, dy, nx, ny, bodyEnd, 1f, textureEnd, 1f, bodyEnd / length, alpha);
            vertex(from, dx, dy, nx, ny, fadeEnd, -1f, fadeTextureEnd, 0f, 1f, 0f);
            vertex(from, dx, dy, nx, ny, fadeEnd, 1f, fadeTextureEnd, 1f, 1f, 0f);
            GL11.glEnd();
        } finally {
            GL11.glPopAttrib();
        }
    }

    private static void vertex(Vector2f from, float dx, float dy, float nx, float ny,
            float along, float side, float u, float v, float fraction, float alpha) {
        float mix = Math.max(0f, Math.min(1f, (fraction - 0.2f) / 0.3f));
        mix = mix * mix * (3f - 2f * mix);
        float red = TEAL.getRed() + (RED.getRed() - TEAL.getRed()) * mix;
        float green = TEAL.getGreen() + (RED.getGreen() - TEAL.getGreen()) * mix;
        float blue = TEAL.getBlue() + (RED.getBlue() - TEAL.getBlue()) * mix;
        float opacity = TEAL.getAlpha() + (RED.getAlpha() - TEAL.getAlpha()) * mix;
        GL11.glColor4f(red / 255f, green / 255f, blue / 255f,
                (int) (opacity * alpha) / 255f);
        GL11.glTexCoord2f(u, v);
        GL11.glVertex2f(from.x + dx * along + nx * side,
                from.y + dy * along + ny * side);
    }
}
