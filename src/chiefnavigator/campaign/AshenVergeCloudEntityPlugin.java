package chiefnavigator.campaign;

import chiefnavigator.quest.AviciHabitatEncounter;
import chiefnavigator.quest.TroyArrivalScript;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

/**
 * Avici's starfield-covering, pitch-black nebula banks. Rendering and
 * campaign effects sample the same deterministic field, so every visible
 * cloud is physical. The historical class name is retained for save safety.
 */
public final class AshenVergeCloudEntityPlugin
        extends BaseCustomEntityPlugin {
    private static final String MOD_ID = "chief_navigator_messina_black_fog";
    private static final String EFFECT_NAME = "Inside Avici's black fog";
    private static final float LEFT = -12000f;
    private static final float RIGHT = 12000f;
    private static final float BOTTOM = -9500f;
    private static final float TOP = 9500f;
    private static final int GRID_X = 81;
    private static final int GRID_Y = 65;
    private static final float CELL_X = (RIGHT - LEFT) / (GRID_X - 1f);
    private static final float CELL_Y = (TOP - BOTTOM) / (GRID_Y - 1f);
    private static final float HABITAT_CLEAR_RADIUS = 1250f;
    private static final float HABITAT_FEATHER_RADIUS = 2250f;

    /** center x/y and x/y radii for an overlapping organic cloud union. */
    private static final float[][] BLOBS = {
        {-8500f, 6500f, 4200f, 2600f},
        {-4300f, 5650f, 4700f, 3100f},
        {2100f, 7000f, 3700f, 2350f},
        {7600f, 5100f, 3900f, 3350f},
        {9050f, 450f, 3000f, 4300f},
        {4750f, -3100f, 4700f, 3000f},
        {350f, -5900f, 4300f, 2950f},
        {-4750f, -4300f, 3800f, 3900f},
        {-9350f, -6650f, 3500f, 2500f},
        {-7700f, 300f, 2850f, 3500f}
    };

    private transient float[][] densityGrid;

    @Override
    public void init(SectorEntityToken entity, Object pluginParams) {
        super.init(entity, pluginParams);
        bakeDensityGrid();
    }

    private Object readResolve() {
        bakeDensityGrid();
        return this;
    }

    private void bakeDensityGrid() {
        densityGrid = new float[GRID_X][GRID_Y];
        for (int x = 0; x < GRID_X; x++) {
            float localX = LEFT + x * CELL_X;
            for (int y = 0; y < GRID_Y; y++) {
                float localY = BOTTOM + y * CELL_Y;
                densityGrid[x][y] = calculateDensity(localX, localY);
            }
        }
    }

    @Override
    public void advance(float amount) {
        if (entity == null || entity.getContainingLocation() == null) return;
        Vector2f origin = entity.getLocation();
        Vector2f habitat = getHabitatLocation();
        for (CampaignFleetAPI fleet :
                entity.getContainingLocation().getFleets()) {
            float density = applyHabitatClearing(sampleDensity(
                    fleet.getLocation().x - origin.x,
                    fleet.getLocation().y - origin.y),
                    fleet.getLocation().x,
                    fleet.getLocation().y,
                    habitat);
            if (density <= 0.18f) continue;
            applyAbyssalEffect(fleet, smoothstep(
                    Math.min(1f, (density - 0.18f) / 0.42f)));
        }
    }

    /** Matches vanilla full-depth Abyssal burn, visibility, and sensor values. */
    private void applyAbyssalEffect(CampaignFleetAPI fleet, float strength) {
        float burnMult = 1f - (1f - HyperspaceTerrainPlugin.ABYSS_BURN_MULT)
                * strength;
        float visibilityMult = 1f
                - (1f - HyperspaceTerrainPlugin.ABYSS_VISIBLITY_MULT)
                * strength;
        float sensorMult = 1f
                - (1f - HyperspaceTerrainPlugin.ABYSS_SENSOR_RANGE_MULT)
                * strength;
        MutableFleetStatsAPI stats = fleet.getStats();
        if (!TroyArrivalScript.CHARYBDIS_FLEET_ID.equals(fleet.getId())) {
            // Budai keeps open-space campaign speed through Avici's custom
            // Abyssal fog; its visibility and sensor penalties still apply.
            stats.addTemporaryModMult(
                    0.1f,
                    MOD_ID + "_burn",
                    EFFECT_NAME,
                    burnMult,
                    stats.getFleetwideMaxBurnMod());
        }
        stats.addTemporaryModMult(
                0.1f,
                MOD_ID + "_visibility",
                EFFECT_NAME,
                visibilityMult,
                stats.getDetectedRangeMod());
        stats.addTemporaryModMult(
                0.1f,
                MOD_ID + "_sensors",
                EFFECT_NAME,
                sensorMult,
                stats.getSensorRangeMod());
    }

    @Override
    public float getRenderRange() {
        return 28000f;
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        // Jump points render on JUMP_POINTS, between TERRAIN_5 and TERRAIN_6.
        // Keeping this opaque field below them prevents valid, permanently
        // visible Avici exits from being painted over by pure black quads.
        if (layer != CampaignEngineLayers.TERRAIN_5) return;
        if (densityGrid == null) bakeDensityGrid();
        float globalAlpha = viewport.getAlphaMult();
        if (globalAlpha <= 0f) return;

        Vector2f origin = entity.getLocation();
        Vector2f habitat = getHabitatLocation();
        int minX = clampGridX((int) Math.floor(
                (viewport.getLLX() - origin.x - LEFT) / CELL_X) - 1);
        int maxX = clampGridX((int) Math.ceil(
                (viewport.getLLX() + viewport.getVisibleWidth()
                        - origin.x - LEFT) / CELL_X) + 1);
        int minY = clampGridY((int) Math.floor(
                (viewport.getLLY() - origin.y - BOTTOM) / CELL_Y) - 1);
        int maxY = clampGridY((int) Math.ceil(
                (viewport.getLLY() + viewport.getVisibleHeight()
                        - origin.y - BOTTOM) / CELL_Y) + 1);

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glBegin(GL11.GL_QUADS);
        for (int x = minX; x < maxX; x++) {
            float x0 = origin.x + LEFT + x * CELL_X;
            float x1 = x0 + CELL_X;
            for (int y = minY; y < maxY; y++) {
                float y0 = origin.y + BOTTOM + y * CELL_Y;
                float y1 = y0 + CELL_Y;
                float d00 = applyHabitatClearing(
                        densityGrid[x][y], x0, y0, habitat);
                float d10 = applyHabitatClearing(
                        densityGrid[x + 1][y], x1, y0, habitat);
                float d11 = applyHabitatClearing(
                        densityGrid[x + 1][y + 1], x1, y1, habitat);
                float d01 = applyHabitatClearing(
                        densityGrid[x][y + 1], x0, y1, habitat);
                if (Math.max(Math.max(d00, d10), Math.max(d11, d01))
                        < 0.008f) continue;
                blackVertex(x0, y0, d00, globalAlpha);
                blackVertex(x1, y0, d10, globalAlpha);
                blackVertex(x1, y1, d11, globalAlpha);
                blackVertex(x0, y1, d01, globalAlpha);
            }
        }
        GL11.glEnd();
        GL11.glPopAttrib();
    }

    private void blackVertex(
            float x,
            float y,
            float density,
            float globalAlpha) {
        float opacity = density >= 0.72f
                ? 1f
                : smoothstep(density / 0.72f);
        GL11.glColor4f(0f, 0f, 0f, globalAlpha * opacity);
        GL11.glVertex2f(x, y);
    }

    private float sampleDensity(float x, float y) {
        if (x <= LEFT || x >= RIGHT || y <= BOTTOM || y >= TOP) return 0f;
        float exactX = (x - LEFT) / CELL_X;
        float exactY = (y - BOTTOM) / CELL_Y;
        int lowerX = Math.min(GRID_X - 2, Math.max(0, (int) exactX));
        int lowerY = Math.min(GRID_Y - 2, Math.max(0, (int) exactY));
        float fractionX = exactX - lowerX;
        float fractionY = exactY - lowerY;
        float bottomDensity = densityGrid[lowerX][lowerY]
                + (densityGrid[lowerX + 1][lowerY]
                        - densityGrid[lowerX][lowerY]) * fractionX;
        float topDensity = densityGrid[lowerX][lowerY + 1]
                + (densityGrid[lowerX + 1][lowerY + 1]
                        - densityGrid[lowerX][lowerY + 1]) * fractionX;
        return bottomDensity + (topDensity - bottomDensity) * fractionY;
    }

    private float calculateDensity(float x, float y) {
        float warpedX = x
                + (float) Math.sin(y * 0.00105f) * 430f
                + (float) Math.sin((x + y) * 0.00061f) * 210f;
        float warpedY = y
                + (float) Math.sin(x * 0.00083f) * 360f
                + (float) Math.cos((x - y) * 0.00049f) * 170f;
        float union = 0f;
        for (int index = 0; index < BLOBS.length; index++) {
            float[] blob = BLOBS[index];
            float dx = (warpedX - blob[0]) / blob[2];
            float dy = (warpedY - blob[1]) / blob[3];
            float normalized = (float) Math.sqrt(dx * dx + dy * dy);
            float contribution = smoothstep(clamp01((1.12f - normalized)
                    / 0.34f));
            union = 1f - (1f - union) * (1f - contribution);
        }

        // Preserve a recognizable star-lit eye at the heart of the system.
        float centerDistance = (float) Math.sqrt(x * x + y * y);
        float starClearing = smoothstep(clamp01(
                (centerDistance - 1050f) / 950f));
        return clamp01(union * starClearing);
    }

    private Vector2f getHabitatLocation() {
        if (entity == null || entity.getContainingLocation() == null) {
            return null;
        }
        SectorEntityToken habitat = entity.getContainingLocation()
                .getEntityById(AviciHabitatEncounter.HABITAT_ID);
        return habitat == null ? null : habitat.getLocation();
    }

    private float applyHabitatClearing(
            float density,
            float worldX,
            float worldY,
            Vector2f habitat) {
        if (habitat == null || density <= 0f) return density;
        float dx = worldX - habitat.x;
        float dy = worldY - habitat.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        float fraction = (distance - HABITAT_CLEAR_RADIUS)
                / (HABITAT_FEATHER_RADIUS - HABITAT_CLEAR_RADIUS);
        return density * smoothstep(fraction);
    }

    private int clampGridX(int value) {
        return Math.max(0, Math.min(GRID_X - 1, value));
    }

    private int clampGridY(int value) {
        return Math.max(0, Math.min(GRID_Y - 1, value));
    }

    private float smoothstep(float value) {
        float clamped = clamp01(value);
        return clamped * clamped * (3f - 2f * clamped);
    }

    private float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
