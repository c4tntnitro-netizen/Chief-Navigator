package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.PlanetSpecAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

/**
 * Bounds-independent chart of the Orion Knot. This deliberately does not use
 * createSectorMap(), whose vanilla viewport clamps remote hyperspace regions.
 */
final class OrionKnotChartPanelPlugin extends BaseCustomUIPanelPlugin {
    private static final String ABYSS_LABEL_SPRITE =
            "graphics/ui/chief_navigator_persean_orion_abyss_v1.png";
    private static final String NEBULA_MAP_SPRITE =
            "graphics/terrain/nebula_blue_map.png";
    private static final String STAR_ICON = "graphics/warroom/icon_star.png";
    private static final String STARLESS_ICON =
            "graphics/starscape/nebula_center.png";
    private static final float PLOT_SIDE = 36f;
    private static final float PLOT_BOTTOM = 50f;
    private static final float PLOT_TOP = 70f;
    private static final float GRID_SPACING = 75f;
    private static final String[] SYSTEM_IDS = {
        OdysseyExpanseSystem.SYSTEM_ID,
        OdysseyExpanseSystem.MESSINA_ID,
        OdysseyExpanseSystem.SILENT_WAKE_ID,
        OdysseyExpanseSystem.ASHEN_VERGE_ID,
        OdysseyExpanseSystem.LAST_LIGHT_ID,
        OdysseyExpanseSystem.DEVOURED_REACH_ID
    };
    private static final Color[] SYSTEM_COLORS = {
        new Color(245, 208, 105),
        new Color(178, 64, 86),
        new Color(175, 220, 235),
        new Color(235, 153, 82),
        new Color(190, 205, 235),
        new Color(159, 119, 190)
    };
    private static final String[][] PLOTTED_LEGS = {
        {OdysseyExpanseSystem.SYSTEM_ID,
                OdysseyExpanseSystem.ASHEN_VERGE_ID},
        {OdysseyExpanseSystem.SYSTEM_ID,
                OdysseyExpanseSystem.SILENT_WAKE_ID},
        {OdysseyExpanseSystem.SYSTEM_ID,
                OdysseyExpanseSystem.LAST_LIGHT_ID},
        {OdysseyExpanseSystem.ASHEN_VERGE_ID,
                OdysseyExpanseSystem.DEVOURED_REACH_ID},
        {OdysseyExpanseSystem.SILENT_WAKE_ID,
                OdysseyExpanseSystem.MESSINA_ID}
    };
    private static final int GLYPH_ODYSSEY = 0;
    private static final int GLYPH_BLACK_HOLE = 1;
    private static final int GLYPH_CURRENT = 2;
    private static final int GLYPH_STAR = 3;
    private static final int GLYPH_WHITE_DWARF = 4;
    private static final int GLYPH_STARLESS = 5;
    private static final float SIDE_PAD = 105f;
    private static final float TOP_PAD = 96f;
    private static final float BOTTOM_PAD = 104f;
    private static final float LABEL_WIDTH = 190f;
    private static final float LABEL_HEIGHT = 42f;
    private static final float NODE_RADIUS = 20f;

    private final float width;
    private final float height;
    private final List<Node> nodes = new ArrayList<Node>();
    private final List<CloudTile> clouds = new ArrayList<CloudTile>();
    private final SpriteAPI abyssLabel;
    private final SpriteAPI nebulaMap;
    private PositionAPI position;
    private float worldMinX;
    private float worldMinY;
    private float chartOriginX;
    private float chartOriginY;
    private float worldScale = 1f;
    private Float playerX;
    private Float playerY;

    OrionKnotChartPanelPlugin(float width, float height) {
        this.width = width;
        this.height = height;
        this.abyssLabel = Global.getSettings().getSprite(ABYSS_LABEL_SPRITE);
        this.nebulaMap = Global.getSettings().getSprite(NEBULA_MAP_SPRITE);
    }

    void init(CustomPanelAPI panel) {
        loadNodes();
        loadClouds();
        buildLabels(panel);
    }

    private void loadNodes() {
        nodes.clear();
        for (int index = 0; index < SYSTEM_IDS.length; index++) {
            StarSystemAPI system = OdysseyExpanseSystem.findSystemById(
                    SYSTEM_IDS[index]);
            if (system == null) continue;
            PlanetSpecAPI spec = system.getStar() == null ? null
                    : system.getStar().getSpec();
            String iconPath = spec == null ? STARLESS_ICON
                    : spec.getIconTexture();
            if (iconPath == null || iconPath.isEmpty()) iconPath = STAR_ICON;
            Color iconColor = spec == null || spec.getIconColor() == null
                    ? SYSTEM_COLORS[index] : spec.getIconColor();
            nodes.add(new Node(SYSTEM_IDS[index],
                    cleanSystemName(system.getName()), detailFor(index), index,
                    system.getLocation().x, system.getLocation().y,
                    SYSTEM_COLORS[index], iconColor,
                    Global.getSettings().getSprite(iconPath)));
        }
        layoutNodes();
        locatePlayer();
    }

    private void layoutNodes() {
        if (nodes.isEmpty()) return;
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (Node node : nodes) {
            minX = Math.min(minX, node.worldX);
            maxX = Math.max(maxX, node.worldX);
            minY = Math.min(minY, node.worldY);
            maxY = Math.max(maxY, node.worldY);
        }
        float worldWidth = Math.max(1f, maxX - minX);
        float worldHeight = Math.max(1f, maxY - minY);
        float plotWidth = Math.max(1f, width - SIDE_PAD * 2f);
        float plotHeight = Math.max(1f, height - TOP_PAD - BOTTOM_PAD);
        worldScale = Math.min(plotWidth / worldWidth,
                plotHeight / worldHeight);
        worldMinX = minX;
        worldMinY = minY;
        float usedWidth = (maxX - minX) * worldScale;
        float usedHeight = (maxY - minY) * worldScale;
        chartOriginX = (width - usedWidth) * 0.5f;
        chartOriginY = BOTTOM_PAD + (plotHeight - usedHeight) * 0.5f;
        for (Node node : nodes) {
            node.x = chartX(node.worldX);
            node.y = chartY(node.worldY);
        }
    }

    /** Snapshot only visible map samples; never advance or edit terrain. */
    private void loadClouds() {
        clouds.clear();
        if (nodes.isEmpty() || Global.getSector() == null) return;
        HyperspaceTerrainPlugin terrain = Misc.getHyperspaceTerrainPlugin();
        if (terrain == null || terrain.getEntity() == null) return;
        int[][] tiles = terrain.getTiles();
        if (tiles == null || tiles.length == 0 || tiles[0].length == 0) return;
        int samples = Math.max(1, terrain.getNumMapSamples());
        float tileSize = terrain.getTileSize();
        float renderSize = terrain.getTileRenderSize() * samples;
        float originX = terrain.getEntity().getLocation().x
                - tiles.length * tileSize * 0.5f;
        float originY = terrain.getEntity().getLocation().y
                - tiles[0].length * tileSize * 0.5f;
        // Match vanilla's coarse map sampling, restricted to this panel.
        int minX = Math.max(0, (int) Math.floor((worldX(PLOT_SIDE)
                - renderSize - originX) / tileSize / samples) * samples);
        int maxX = Math.min(tiles.length - 1, (int) Math.ceil((worldX(
                width - PLOT_SIDE) + renderSize - originX) / tileSize));
        int minY = Math.max(0, (int) Math.floor((worldY(PLOT_BOTTOM)
                - renderSize - originY) / tileSize / samples) * samples);
        int maxY = Math.min(tiles[0].length - 1, (int) Math.ceil((worldY(
                height - PLOT_TOP) + renderSize - originY) / tileSize));
        for (int x = minX; x <= maxX; x += samples) {
            for (int y = minY; y <= maxY; y += samples) {
                int textureIndex = -1;
                int occupied = 0;
                for (int dx = 0; dx < samples && x + dx < tiles.length; dx++) {
                    for (int dy = 0; dy < samples
                            && y + dy < tiles[0].length; dy++) {
                        int tile = tiles[x + dx][y + dy];
                        if (tile < 0) continue;
                        if (textureIndex < 0) textureIndex = tile;
                        occupied++;
                    }
                }
                if (textureIndex < 0) continue;
                Random random = new Random((long) (x + y * tiles.length)
                        * 1000000L);
                random.nextFloat(); // Vanilla's angle; atlas crops stay axis-aligned.
                float jitterX = (random.nextFloat() - 0.5f) * renderSize * 0.25f;
                float jitterY = (random.nextFloat() - 0.5f) * renderSize * 0.25f;
                CloudTile cloud = CloudTile.crop(
                        chartX(originX + (x + samples * 0.5f) * tileSize + jitterX),
                        chartY(originY + (y + samples * 0.5f) * tileSize + jitterY),
                        renderSize * worldScale, textureIndex,
                        0.67f * occupied / (samples * samples),
                        PLOT_SIDE, PLOT_BOTTOM, width - PLOT_SIDE,
                        height - PLOT_TOP);
                if (cloud != null) clouds.add(cloud);
            }
        }
    }

    private float chartX(float worldX) {
        return chartOriginX + (worldX - worldMinX) * worldScale;
    }

    private float chartY(float worldY) {
        return chartOriginY + (worldY - worldMinY) * worldScale;
    }

    private float worldX(float chartX) {
        return worldMinX + (chartX - chartOriginX) / worldScale;
    }

    private float worldY(float chartY) {
        return worldMinY + (chartY - chartOriginY) / worldScale;
    }

    private void locatePlayer() {
        playerX = null;
        playerY = null;
        if (nodes.isEmpty() || Global.getSector() == null) return;
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return;

        Vector2f location = null;
        if (fleet.getContainingLocation() instanceof StarSystemAPI) {
            String systemId = ((StarSystemAPI) fleet.getContainingLocation())
                    .getOptionalUniqueId();
            for (Node node : nodes) {
                if (node.id.equals(systemId)) {
                    playerX = node.x;
                    playerY = node.y;
                    return;
                }
            }
        } else if (fleet.getContainingLocation()
                == Global.getSector().getHyperspace()) {
            location = fleet.getLocation();
        }
        if (location == null) return;

        float minWorldX = Float.MAX_VALUE;
        float maxWorldX = -Float.MAX_VALUE;
        float minWorldY = Float.MAX_VALUE;
        float maxWorldY = -Float.MAX_VALUE;
        for (Node node : nodes) {
            minWorldX = Math.min(minWorldX, node.worldX);
            maxWorldX = Math.max(maxWorldX, node.worldX);
            minWorldY = Math.min(minWorldY, node.worldY);
            maxWorldY = Math.max(maxWorldY, node.worldY);
        }
        float marginX = Math.max(1f, (maxWorldX - minWorldX) * 0.15f);
        float marginY = Math.max(1f, (maxWorldY - minWorldY) * 0.15f);
        if (location.x < minWorldX - marginX
                || location.x > maxWorldX + marginX
                || location.y < minWorldY - marginY
                || location.y > maxWorldY + marginY) {
            return;
        }
        playerX = chartX(location.x);
        playerY = chartY(location.y);
    }

    private void buildLabels(CustomPanelAPI panel) {
        TooltipMakerAPI title = panel.createUIElement(
                420f, 72f, false);
        title.setTitleOrbitronVeryLarge();
        title.addTitle("Orion Knot Sector", Misc.getBasePlayerColor());
        title.addPara("Sinni's local navigation chart", 2f,
                Misc.getGrayColor(), "local navigation chart");
        panel.addUIElement(title).inTL(28f, 14f);

        TooltipMakerAPI north = panel.createUIElement(24f, 24f, false);
        LabelAPI northLabel = north.addPara("N", 0f);
        northLabel.setAlignment(Alignment.MID);
        northLabel.setColor(Misc.getHighlightColor());
        panel.addUIElement(north).inTR(49f, 61f);

        for (Node node : nodes) {
            TooltipMakerAPI labelPanel = panel.createUIElement(
                    LABEL_WIDTH, LABEL_HEIGHT, false);
            LabelAPI label = labelPanel.addPara(node.name, 0f);
            label.setAlignment(Alignment.MID);
            label.setColor(node.color);
            LabelAPI detail = labelPanel.addPara(node.detail, 1f);
            detail.setAlignment(Alignment.MID);
            detail.setColor(Misc.getGrayColor());
            boolean placeAbove = node.y < BOTTOM_PAD + LABEL_HEIGHT + 18f;
            float top = placeAbove
                    ? height - node.y - NODE_RADIUS - 6f - LABEL_HEIGHT
                    : height - node.y + NODE_RADIUS + 6f;
            panel.addUIElement(labelPanel).inTL(
                    node.x - LABEL_WIDTH * 0.5f, top);
        }

        TooltipMakerAPI legend = panel.createUIElement(
                width - 56f, 24f, false);
        String legendText = playerX == null
                ? "Relative bearings."
                : "The white diamond marks your fleet.";
        LabelAPI legendLabel = legend.addPara(legendText,
                0f, Misc.getGrayColor(), legendText);
        legendLabel.setAlignment(Alignment.MID);
        panel.addUIElement(legend).inBL(28f, 14f);
    }

    @Override
    public void positionChanged(PositionAPI position) {
        this.position = position;
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (position == null) return;
        float left = position.getX();
        float bottom = position.getY();
        float right = left + position.getWidth();
        float top = bottom + position.getHeight();
        float plotLeft = left + PLOT_SIDE;
        float plotRight = right - PLOT_SIDE;
        float plotBottom = bottom + PLOT_BOTTOM;
        float plotTop = top - PLOT_TOP;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        color(new Color(1, 3, 8), 0.98f * alphaMult);
        quad(left, bottom, right, top);

        // Stock blue cloud atlas, sampling the real hyperspace tile field.
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        nebulaMap.bindTexture();
        GL11.glBegin(GL11.GL_QUADS);
        for (CloudTile cloud : clouds) {
            GL11.glColor4f(1f, 1f, 1f, cloud.alpha * alphaMult);
            GL11.glTexCoord2f(cloud.u0, cloud.v0);
            GL11.glVertex2f(left + cloud.left, bottom + cloud.bottom);
            GL11.glTexCoord2f(cloud.u1, cloud.v0);
            GL11.glVertex2f(left + cloud.right, bottom + cloud.bottom);
            GL11.glTexCoord2f(cloud.u1, cloud.v1);
            GL11.glVertex2f(left + cloud.right, bottom + cloud.top);
            GL11.glTexCoord2f(cloud.u0, cloud.v1);
            GL11.glVertex2f(left + cloud.left, bottom + cloud.top);
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        // Native sector-map grid spacing, without the native map's bounds clamp.
        color(new Color(48, 94, 135), 0.52f * alphaMult);
        GL11.glLineWidth(1f);
        GL11.glBegin(GL11.GL_LINES);
        for (float x = plotLeft; x <= plotRight; x += GRID_SPACING) {
            GL11.glVertex2f(x, plotBottom);
            GL11.glVertex2f(x, plotTop);
        }
        for (float y = plotBottom; y <= plotTop; y += GRID_SPACING) {
            GL11.glVertex2f(plotLeft, y);
            GL11.glVertex2f(plotRight, y);
        }
        GL11.glEnd();

        for (int index = 0; index < 110; index++) {
            float x = plotLeft + hash(index * 17 + 3)
                    * (plotRight - plotLeft);
            float y = plotBottom + hash(index * 31 + 11)
                    * (plotTop - plotBottom);
            float brightness = 0.06f + hash(index * 43 + 7) * 0.12f;
            color(new Color(150, 190, 205), brightness * alphaMult);
            float size = index % 13 == 0 ? 1.5f : 0.75f;
            quad(x - size, y - size, x + size, y + size);
        }

        // These are Sinni's useful traversal legs, not implied jump links.
        color(new Color(111, 172, 191), 0.32f * alphaMult);
        GL11.glLineWidth(1f);
        for (String[] leg : PLOTTED_LEGS) {
            Node from = findNode(leg[0]);
            Node to = findNode(leg[1]);
            if (from != null && to != null) {
                dashedLine(left + from.x, bottom + from.y,
                        left + to.x, bottom + to.y, 9f, 7f);
            }
        }

        color(new Color(62, 134, 159), 0.85f * alphaMult);
        GL11.glLineWidth(1f);
        lineLoop(plotLeft, plotBottom, plotRight, plotTop);

        // Compact astrolabe-style north reference.
        float compassX = plotRight - 31f;
        float compassY = plotTop - 35f;
        color(new Color(101, 169, 181), 0.45f * alphaMult);
        circle(compassX, compassY, 19f, 28, false);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(compassX, compassY - 14f);
        GL11.glVertex2f(compassX, compassY + 17f);
        GL11.glVertex2f(compassX - 14f, compassY);
        GL11.glVertex2f(compassX + 14f, compassY);
        GL11.glEnd();
        color(Misc.getHighlightColor(), 0.9f * alphaMult);
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex2f(compassX, compassY + 24f);
        GL11.glVertex2f(compassX - 4f, compassY + 13f);
        GL11.glVertex2f(compassX + 4f, compassY + 13f);
        GL11.glEnd();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        for (Node node : nodes) {
            drawSystemGlyph(node, left + node.x, bottom + node.y,
                    alphaMult);
        }
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        if (playerX != null && playerY != null) {
            float x = left + playerX;
            float y = bottom + playerY;
            color(Color.WHITE, alphaMult);
            GL11.glLineWidth(2f);
            GL11.glBegin(GL11.GL_LINE_LOOP);
            GL11.glVertex2f(x, y + 14f);
            GL11.glVertex2f(x + 10f, y);
            GL11.glVertex2f(x, y - 14f);
            GL11.glVertex2f(x - 10f, y);
            GL11.glEnd();
        }
        GL11.glPopAttrib();

        // The regional label is a purpose-built sprite so it can follow the
        // slanted cartographic treatment without rotating the rest of the UI.
        abyssLabel.setSize(236f, 30f);
        abyssLabel.setAngle(24f);
        abyssLabel.setAlphaMult(0.72f * alphaMult);
        abyssLabel.renderAtCenter(left + 160f, top - 126f);
    }

    private void drawSystemGlyph(Node node, float x, float y,
            float alphaMult) {
        // These monochrome UI glyphs use the same spec coloring as vanilla's
        // sector map; no campaign artwork is recolored.
        node.icon.setNormalBlend();
        node.icon.setColor(node.iconColor);
        node.icon.setAlphaMult(0.85f * alphaMult);
        node.icon.setAngle(0f);
        float size = node.glyph == GLYPH_STARLESS ? 52f : NODE_RADIUS * 2f;
        node.icon.setSize(size, size);
        node.icon.renderAtCenter(x, y);
    }

    private Node findNode(String id) {
        for (Node node : nodes) {
            if (node.id.equals(id)) return node;
        }
        return null;
    }

    private static String cleanSystemName(String name) {
        if (name == null) return "Unknown system";
        String suffix = " Star System";
        return name.endsWith(suffix)
                ? name.substring(0, name.length() - suffix.length()) : name;
    }

    private static String detailFor(int index) {
        switch (index) {
            case GLYPH_ODYSSEY: return "Troy arrival";
            case GLYPH_BLACK_HOLE: return "Supermassive Black Hole";
            case GLYPH_CURRENT: return "Southbound current";
            case GLYPH_STAR: return "FOB Ithaca";
            case GLYPH_WHITE_DWARF: return "Hidden Guardian";
            default: return "Skeletal System";
        }
    }

    private static void color(Color color, float alpha) {
        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f,
                color.getBlue() / 255f, Math.max(0f, Math.min(1f, alpha)));
    }

    private static void quad(float left, float bottom,
            float right, float top) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(left, bottom);
        GL11.glVertex2f(right, bottom);
        GL11.glVertex2f(right, top);
        GL11.glVertex2f(left, top);
        GL11.glEnd();
    }

    private static void lineLoop(float left, float bottom,
            float right, float top) {
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(left, bottom);
        GL11.glVertex2f(right, bottom);
        GL11.glVertex2f(right, top);
        GL11.glVertex2f(left, top);
        GL11.glEnd();
    }

    private static void dashedLine(float x1, float y1, float x2, float y2,
            float dash, float gap) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= 0.01f) return;
        float ux = dx / length;
        float uy = dy / length;
        GL11.glBegin(GL11.GL_LINES);
        for (float at = 0f; at < length; at += dash + gap) {
            float end = Math.min(length, at + dash);
            GL11.glVertex2f(x1 + ux * at, y1 + uy * at);
            GL11.glVertex2f(x1 + ux * end, y1 + uy * end);
        }
        GL11.glEnd();
    }

    private static float hash(int value) {
        int mixed = value * 0x45d9f3b;
        mixed = (mixed ^ (mixed >>> 16)) * 0x45d9f3b;
        mixed ^= mixed >>> 16;
        return (mixed & 0x7fffffff) / (float) Integer.MAX_VALUE;
    }

    private static void circle(float x, float y, float radius,
            int segments, boolean filled) {
        GL11.glBegin(filled ? GL11.GL_TRIANGLE_FAN : GL11.GL_LINE_LOOP);
        if (filled) GL11.glVertex2f(x, y);
        int limit = filled ? segments : segments - 1;
        for (int index = 0; index <= limit; index++) {
            float angle = (float) (Math.PI * 2d * index / segments);
            GL11.glVertex2f(x + (float) Math.cos(angle) * radius,
                    y + (float) Math.sin(angle) * radius);
        }
        GL11.glEnd();
    }

    private static final class Node {
        private final String id;
        private final String name;
        private final String detail;
        private final int glyph;
        private final float worldX;
        private final float worldY;
        private final Color color;
        private final Color iconColor;
        private final SpriteAPI icon;
        private float x;
        private float y;

        private Node(String id, String name, String detail, int glyph,
                float worldX, float worldY, Color color, Color iconColor,
                SpriteAPI icon) {
            this.id = id;
            this.name = name;
            this.detail = detail;
            this.glyph = glyph;
            this.worldX = worldX;
            this.worldY = worldY;
            this.color = color;
            this.iconColor = iconColor;
            this.icon = icon;
        }
    }

    /** A single atlas cell, cropped in panel space with matching UVs. */
    private static final class CloudTile {
        private final float left, bottom, right, top;
        private final float u0, v0, u1, v1, alpha;

        private CloudTile(float left, float bottom, float right, float top,
                float u0, float v0, float u1, float v1, float alpha) {
            this.left = left;
            this.bottom = bottom;
            this.right = right;
            this.top = top;
            this.u0 = u0;
            this.v0 = v0;
            this.u1 = u1;
            this.v1 = v1;
            this.alpha = alpha;
        }

        private static CloudTile crop(float x, float y, float size, int index,
                float alpha, float clipLeft, float clipBottom,
                float clipRight, float clipTop) {
            float originalLeft = x - size * 0.5f;
            float originalBottom = y - size * 0.5f;
            float left = Math.max(clipLeft, originalLeft);
            float bottom = Math.max(clipBottom, originalBottom);
            float right = Math.min(clipRight, x + size * 0.5f);
            float top = Math.min(clipTop, y + size * 0.5f);
            if (left >= right || bottom >= top) return null;
            int cell = Math.floorMod(index, 16);
            float u = (cell % 4) * 0.25f;
            float v = (cell / 4) * 0.25f;
            return new CloudTile(left, bottom, right, top,
                    u + (left - originalLeft) / size * 0.25f,
                    v + (bottom - originalBottom) / size * 0.25f,
                    u + (right - originalLeft) / size * 0.25f,
                    v + (top - originalBottom) / size * 0.25f, alpha);
        }
    }
}
