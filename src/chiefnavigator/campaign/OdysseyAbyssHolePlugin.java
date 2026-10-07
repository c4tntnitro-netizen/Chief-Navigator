package chiefnavigator.campaign;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceAbyssPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.util.vector.Vector2f;

/**
 * Cuts a feathered region of ordinary hyperspace out of the global Abyss.
 * The vanilla Abyss implementation remains authoritative everywhere else.
 */
public final class OdysseyAbyssHolePlugin
        implements HyperspaceAbyssPlugin, Serializable {
    private static final long serialVersionUID = 1L;

    private final HyperspaceAbyssPlugin delegate;
    private float centerX;
    private float centerY;
    private float innerRadius;
    private float outerRadius;

    private OdysseyAbyssHolePlugin(
            HyperspaceAbyssPlugin delegate,
            float centerX,
            float centerY,
            float innerRadius,
            float outerRadius) {
        this.delegate = delegate;
        configure(centerX, centerY, innerRadius, outerRadius);
    }

    public static void install(
            HyperspaceTerrainPlugin terrain,
            float centerX,
            float centerY,
            float innerRadius,
            float outerRadius) {
        if (terrain == null || terrain.getAbyssPlugin() == null) return;
        HyperspaceAbyssPlugin current = terrain.getAbyssPlugin();
        if (current instanceof OdysseyAbyssHolePlugin) {
            ((OdysseyAbyssHolePlugin) current).configure(
                    centerX, centerY, innerRadius, outerRadius);
            return;
        }
        terrain.setAbyssPlugin(new OdysseyAbyssHolePlugin(
                current, centerX, centerY, innerRadius, outerRadius));
    }

    private void configure(
            float centerX,
            float centerY,
            float innerRadius,
            float outerRadius) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.innerRadius = Math.max(0f, innerRadius);
        this.outerRadius = Math.max(this.innerRadius + 1f, outerRadius);
    }

    private float abyssFraction(Vector2f point) {
        if (point == null) return 1f;
        float dx = point.x - centerX;
        float dy = point.y - centerY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        float value = (distance - innerRadius) / (outerRadius - innerRadius);
        value = Math.max(0f, Math.min(1f, value));
        return value * value * (3f - 2f * value);
    }

    private float abyssFraction(SectorEntityToken token) {
        return token == null ? 1f : abyssFraction(token.getLocation());
    }

    @Override
    public boolean isInAbyss(Vector2f point) {
        return abyssFraction(point) >= 0.999f && delegate.isInAbyss(point);
    }

    @Override
    public boolean isInAbyss(SectorEntityToken token) {
        return abyssFraction(token) >= 0.999f && delegate.isInAbyss(token);
    }

    @Override
    public float getAbyssalDepth(Vector2f point) {
        return delegate.getAbyssalDepth(point) * abyssFraction(point);
    }

    @Override
    public float getAbyssalDepth(SectorEntityToken token) {
        return delegate.getAbyssalDepth(token) * abyssFraction(token);
    }

    @Override
    public float getAbyssalDepth(Vector2f point, boolean withNoise) {
        return delegate.getAbyssalDepth(point, withNoise) * abyssFraction(point);
    }

    @Override
    public float getAbyssalDepth(
            SectorEntityToken token,
            boolean withNoise) {
        return delegate.getAbyssalDepth(token, withNoise) * abyssFraction(token);
    }

    @Override
    public void advance(float amount) {
        delegate.advance(amount);
    }

    @Override
    public List<StarSystemAPI> getAbyssalSystems() {
        List<StarSystemAPI> result = new ArrayList<StarSystemAPI>();
        List<StarSystemAPI> systems = delegate.getAbyssalSystems();
        if (systems == null) return result;
        for (StarSystemAPI system : systems) {
            if (abyssFraction(system.getLocation()) >= 0.999f) {
                result.add(system);
            }
        }
        return result;
    }
}
