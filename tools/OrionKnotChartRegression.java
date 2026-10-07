package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.ui.*;
import java.awt.Color;
import java.lang.reflect.*;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;

/** Headless checks of live chart geometry, stock icons, and bounded atlas crops. */
public final class OrionKnotChartRegression {
    interface Call { Object invoke(String name, Object[] args); }

    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[]{type}, (proxy, method, args) -> {
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == args[0];
                    Object result = call.invoke(method.getName(), args);
                    if (result != null) return result;
                    Class<?> returns = method.getReturnType();
                    if (returns == boolean.class) return false;
                    if (returns == int.class) return 0;
                    if (returns == long.class) return 0L;
                    if (returns == float.class) return 0f;
                    if (returns == double.class) return 0d;
                    return null;
                }));
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static void near(float actual, float expected, String message) {
        check(Math.abs(actual - expected) < .02f,
                message + ": " + actual + " != " + expected);
    }

    static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    static float number(Object object, String name) throws Exception {
        return ((Number) field(object, name)).floatValue();
    }

    static Object call(Object object, String name, Class<?>[] types, Object... args)
            throws Exception {
        Method method = object.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(object, args);
    }

    @SuppressWarnings("unchecked")
    static List<Object> list(Object object, String name) throws Exception {
        return (List<Object>) field(object, name);
    }

    static void readOnly(String name) {
        check(!name.matches("^(set|add|remove|create|clear|advance|update|generate).*"),
                "Chart attempted a campaign mutation: " + name);
    }

    static final class SpriteRecord {
        final String path;
        Color color;
        float width, height;
        int rendered;
        final SpriteAPI sprite;

        SpriteRecord(String path) {
            this.path = path;
            sprite = mock(SpriteAPI.class, (name, args) -> {
                if (name.equals("setColor")) color = (Color) args[0];
                if (name.equals("setSize")) {
                    width = (Float) args[0]; height = (Float) args[1];
                }
                if (name.equals("renderAtCenter")) rendered++;
                if (name.equals("getTexWidth") || name.equals("getTexHeight")) return 1f;
                return null;
            });
        }
    }

    static final class TestTerrain extends HyperspaceTerrainPlugin {
        int[][] grid = new int[256][256];
        final SectorEntityToken token;
        int samplesRead;

        TestTerrain() {
            token = mock(SectorEntityToken.class, (name, args) -> {
                readOnly(name);
                return name.equals("getLocation") ? new Vector2f(150000f, -94000f) : null;
            });
            fill(3);
        }

        void fill(int value) { for (int[] column : grid) Arrays.fill(column, value); }
        @Override public SectorEntityToken getEntity() { return token; }
        @Override public int[][] getTiles() { return grid; }
        @Override public float getTileSize() { return 400f; }
        @Override public float getTileRenderSize() { return 1000f; }
        @Override public int getNumMapSamples() { samplesRead++; return 5; }
        @Override public void advance(float amount) {
            throw new AssertionError("Chart must not advance terrain");
        }
    }

    static final class Fixture {
        final List<StarSystemAPI> systems = new ArrayList<>();
        final List<Vector2f> locations = new ArrayList<>();
        final Map<SpriteAPI, SpriteRecord> sprites = new IdentityHashMap<>();
        final TestTerrain terrain = new TestTerrain();
        final List<CampaignTerrainAPI> terrainList = new ArrayList<>();
        final LocationAPI hyperspace;
        LocationAPI playerLocation;
        final Vector2f playerPosition = new Vector2f(154250f, -96250f);
        CampaignFleetAPI player;
        int dismissed, canceled;
        Object[] shown;

        Fixture() {
            terrainList.add(mock(CampaignTerrainAPI.class, (name, args) -> {
                readOnly(name);
                return name.equals("getPlugin") ? terrain : null;
            }));
            hyperspace = mock(LocationAPI.class, (name, args) -> {
                readOnly(name);
                return name.equals("getTerrainCopy") ? terrainList : null;
            });
            playerLocation = hyperspace;
            player = mock(CampaignFleetAPI.class, (name, args) -> {
                readOnly(name);
                if (name.equals("getContainingLocation")) return playerLocation;
                if (name.equals("getLocation")) return playerPosition;
                return null;
            });
            Global.setSettings(mock(SettingsAPI.class, (name, args) -> {
                if (name.equals("getColor")) return Color.CYAN;
                if (name.equals("getScreenWidth")) return 1600f;
                if (name.equals("getScreenHeight")) return 1080f;
                if (name.equals("getSprite")) {
                    String path = (String) args[args.length - 1];
                    SpriteRecord sprite = new SpriteRecord(path);
                    sprites.put(sprite.sprite, sprite);
                    return sprite.sprite;
                }
                return null;
            }));
            Global.setSector(mock(SectorAPI.class, (name, args) -> {
                readOnly(name);
                if (name.equals("getPlayerFaction")) return mock(FactionAPI.class,
                        (called, values) -> called.equals("getBaseUIColor") ? Color.CYAN : null);
                if (name.equals("getStarSystems")) return systems;
                if (name.equals("getPlayerFleet")) return player;
                if (name.equals("getHyperspace")) return hyperspace;
                return null;
            }));
            String[] ids = {OdysseyExpanseSystem.SYSTEM_ID, OdysseyExpanseSystem.MESSINA_ID,
                    OdysseyExpanseSystem.SILENT_WAKE_ID, OdysseyExpanseSystem.ASHEN_VERGE_ID,
                    OdysseyExpanseSystem.LAST_LIGHT_ID, OdysseyExpanseSystem.DEVOURED_REACH_ID};
            float[][] positions = {{2000,1000},{6500,-5500},{4250,-2250},
                    {5900,1000},{800,-2700},{5250,4845}};
            String[] icons = {"graphics/icons/nebula_map_icon.png", "graphics/icons/blackhole.png",
                    "graphics/warroom/icon_star.png", "graphics/warroom/icon_star.png",
                    "graphics/warroom/icon_star.png", null};
            for (int i = 0; i < ids.length; i++) {
                locations.add(new Vector2f(150000f + positions[i][0], -94000f + positions[i][1]));
                systems.add(system(ids[i], "System " + i + " Star System", locations.get(i),
                        icons[i], i == 5 ? null : new Color(40 + i * 30, 120, 180)));
            }
        }

        StarSystemAPI system(String id, String title, Vector2f point, String icon, Color color) {
            MemoryAPI memory = mock(MemoryAPI.class, (name, args) -> {
                readOnly(name); return name.equals("getBoolean") ? true : null;
            });
            PlanetSpecAPI spec = "null-spec".equals(icon) ? null : mock(PlanetSpecAPI.class, (name, args) -> {
                readOnly(name);
                if (name.equals("getIconTexture")) return icon;
                if (name.equals("getIconColor")) return color;
                return null;
            });
            PlanetAPI star = icon == null ? null : mock(PlanetAPI.class, (name, args) -> {
                readOnly(name); return name.equals("getSpec") ? spec : null;
            });
            return mock(StarSystemAPI.class, (name, args) -> {
                readOnly(name);
                if (name.equals("getOptionalUniqueId")) return id;
                if (name.equals("getName")) return title;
                if (name.equals("getLocation")) return point;
                if (name.equals("getStar")) return star;
                if (name.equals("getMemoryWithoutUpdate")) return memory;
                return null;
            });
        }

        CustomPanelAPI panel() {
            LabelAPI label = mock(LabelAPI.class, (name, args) -> null);
            TooltipMakerAPI tooltip = mock(TooltipMakerAPI.class,
                    (name, args) -> name.startsWith("add") ? label : null);
            final PositionAPI[] position = new PositionAPI[1];
            position[0] = mock(PositionAPI.class, (name, args) -> position[0]);
            return mock(CustomPanelAPI.class, (name, args) -> {
                if (name.equals("createUIElement")) return tooltip;
                if (name.equals("addUIElement")) return position[0];
                return null;
            });
        }

        OrionKnotChartPanelPlugin chart() {
            OrionKnotChartPanelPlugin chart = new OrionKnotChartPanelPlugin(1000f, 820f);
            chart.init(panel());
            return chart;
        }
    }

    static void checkGeometry(Fixture fixture) throws Exception {
        OrionKnotChartPanelPlugin chart = fixture.chart();
        List<Object> nodes = list(chart, "nodes");
        check(nodes.size() == 6, "The chart must display all six owned real systems");
        float scale = number(chart, "worldScale");
        for (int i = 0; i < nodes.size(); i++) {
            Object node = nodes.get(i);
            near(number(node, "worldX"), fixture.locations.get(i).x, "Uses serialized system x");
            near(number(node, "worldY"), fixture.locations.get(i).y, "Uses serialized system y");
            check(("System " + i).equals(field(node, "name")), "Clean only the native system suffix");
            check(number(node, "x") >= 105f && number(node, "x") <= 895f,
                    "System icon has horizontal panel clearance");
            check(number(node, "y") >= 104f && number(node, "y") <= 724f,
                    "System icon has vertical panel clearance");
            if (i > 0) {
                near(number(node, "x") - number(nodes.get(0), "x"),
                        (fixture.locations.get(i).x - fixture.locations.get(0).x) * scale,
                        "Preserve horizontal bearings under a uniform scale");
                near(number(node, "y") - number(nodes.get(0), "y"),
                        (fixture.locations.get(i).y - fixture.locations.get(0).y) * scale,
                        "Preserve vertical bearings under a uniform scale");
            }
        }
        Object sanzu = nodes.get(2);
        near(number(chart, "playerX"), number(sanzu, "x"), "Hyperspace player x");
        near(number(chart, "playerY"), number(sanzu, "y"), "Hyperspace player y");
        fixture.playerLocation = fixture.systems.get(3);
        chart = fixture.chart();
        near(number(chart, "playerX"), number(list(chart, "nodes").get(3), "x"),
                "A realspace fleet is marked at its containing system");
        fixture.playerLocation = fixture.hyperspace;
        fixture.playerPosition.set(0f, 0f);
        check(field(fixture.chart(), "playerX") == null, "Hide fleets outside the Knot");
        fixture.playerLocation = fixture.system("foreign", "Foreign", new Vector2f(), null, null);
        check(field(fixture.chart(), "playerX") == null, "Hide fleets in unrelated systems");
        fixture.playerLocation = fixture.hyperspace;
        fixture.playerPosition.set(154250f, -96250f);
        fixture.locations.get(3).x += 325f;
        near(number(list(fixture.chart(), "nodes").get(3), "worldX"), 156225f,
                "Reopening reads current serialized coordinates rather than constants");
    }

    static void checkIcons(Fixture fixture) throws Exception {
        OrionKnotChartPanelPlugin chart = fixture.chart();
        List<Object> nodes = list(chart, "nodes");
        String[] expected = {"graphics/icons/nebula_map_icon.png", "graphics/icons/blackhole.png",
                "graphics/warroom/icon_star.png", "graphics/warroom/icon_star.png",
                "graphics/warroom/icon_star.png", "graphics/starscape/nebula_center.png"};
        for (int i = 0; i < nodes.size(); i++) {
            Object node = nodes.get(i);
            SpriteRecord sprite = fixture.sprites.get(field(node, "icon"));
            check(expected[i].equals(sprite.path), "Use the primary's stock map icon");
            if (i < 5) check(field(node, "iconColor").equals(new Color(40 + i * 30, 120, 180)),
                    "Icon coloring follows the live planet spec");
            call(chart, "drawSystemGlyph", new Class<?>[]{node.getClass(), float.class,
                    float.class, float.class}, node, 500f, 400f, 1f);
            check(sprite.color.equals(field(node, "iconColor")) && sprite.rendered == 1,
                    "The stock map glyph renders with its selected icon color");
            near(sprite.width, i == 5 ? 52f : 40f, "Legible map glyph size");
        }
        fixture.systems.set(0, fixture.system(OdysseyExpanseSystem.SYSTEM_ID, "Fallback",
                fixture.locations.get(0), "", null));
        Object fallback = list(fixture.chart(), "nodes").get(0);
        check(fixture.sprites.get(field(fallback, "icon")).path.equals("graphics/warroom/icon_star.png"),
                "Missing native texture falls back to the stock sun");
        check(field(fallback, "iconColor").equals(field(fallback, "color")),
                "Missing native icon color uses the chart's regional color");
        fixture.systems.set(0, fixture.system(OdysseyExpanseSystem.SYSTEM_ID, "No spec",
                fixture.locations.get(0), "null-spec", null));
        fallback = list(fixture.chart(), "nodes").get(0);
        check(fixture.sprites.get(field(fallback, "icon")).path.equals("graphics/starscape/nebula_center.png")
                        && field(fallback, "iconColor").equals(field(fallback, "color")),
                "A primary with no spec safely uses the regional fallback icon and color");
    }

    static void checkClouds(Fixture fixture) throws Exception {
        OrionKnotChartPanelPlugin chart = fixture.chart();
        List<Object> clouds = list(chart, "clouds");
        check(!clouds.isEmpty() && clouds.size() < 300,
                "Cache visible terrain only, not the 65,536-cell sector");
        float[] first = new float[9];
        String[] fields = {"left","bottom","right","top","u0","v0","u1","v1","alpha"};
        for (int i = 0; i < fields.length; i++) first[i] = number(clouds.get(0), fields[i]);
        for (Object cloud : clouds) {
            check(number(cloud,"left") >= 36f && number(cloud,"right") <= 964f
                            && number(cloud,"bottom") >= 50f && number(cloud,"top") <= 750f,
                    "Cloud geometry cannot escape the inset map border");
            check(number(cloud,"u0") >= .75f - .00001f && number(cloud,"u1") <= 1f + .00001f
                            && number(cloud,"v0") >= -.00001f && number(cloud,"v1") <= .25f + .00001f,
                    "Each cloud samples only its selected atlas cell");
            near(number(cloud,"alpha"), .67f, "Full sample opacity matches native map density");
        }
        chart = fixture.chart();
        for (int i = 0; i < fields.length; i++) near(number(list(chart,"clouds").get(0), fields[i]),
                first[i], "Cloud snapshots are deterministic across chart opens");
        for (int[] column : fixture.terrain.grid) for (int tile : column)
            check(tile == 3, "Opening a chart must not edit terrain tiles");
        fixture.terrain.fill(-1);
        check(list(fixture.chart(),"clouds").isEmpty(), "Empty terrain remains clear");
        fixture.terrain.grid[128][128] = 10;
        clouds = list(fixture.chart(),"clouds");
        check(clouds.size() == 1, "One occupied native tile produces one coarse cloud sample");
        near(number(clouds.get(0),"alpha"), .67f / 25f, "Sparse sample preserves true occupancy");
        fixture.terrainList.clear();
        check(list(fixture.chart(),"clouds").isEmpty(), "Missing hyperspace terrain fails closed");
    }

    static void checkAtlasCrop() throws Exception {
        Class<?> tile = Class.forName("chiefnavigator.quest.OrionKnotChartPanelPlugin$CloudTile");
        Method crop = tile.getDeclaredMethod("crop", float.class,float.class,float.class,int.class,
                float.class,float.class,float.class,float.class,float.class);
        crop.setAccessible(true);
        Object clipped = crop.invoke(null, 0f, 0f, 100f, 6, .67f, 0f, 0f, 25f, 25f);
        near(number(clipped,"left"), 0f, "Clip left");
        near(number(clipped,"right"), 25f, "Clip right");
        near(number(clipped,"u0"), .625f, "Left crop advances U within cell 6");
        near(number(clipped,"u1"), .6875f, "Right crop truncates U within cell 6");
        near(number(clipped,"v0"), .375f, "Bottom crop advances V within cell 6");
        near(number(clipped,"v1"), .4375f, "Top crop truncates V within cell 6");
        check(crop.invoke(null, -100f,-100f,10f,6,1f,0f,0f,25f,25f) == null,
                "Completely hidden cells do not enter the render cache");
    }

    static void checkMissing(Fixture fixture) throws Exception {
        fixture.systems.remove(1);
        check(list(fixture.chart(),"nodes").size() == 5, "Missing systems are omitted without generation");
        while (fixture.systems.size() > 1) fixture.systems.remove(fixture.systems.size() - 1);
        Object node = list(fixture.chart(),"nodes").get(0);
        near(number(node,"x"), 500f, "A lone system is centered horizontally");
        near(number(node,"y"), 414f, "A lone system is centered in the vertical plot");
        fixture.systems.clear();
        OrionKnotChartPanelPlugin chart = fixture.chart();
        check(list(chart,"nodes").isEmpty() && list(chart,"clouds").isEmpty()
                        && field(chart,"playerX") == null,
                "No owned systems produces a safe empty chart");
    }

    static void checkDialog(Fixture fixture) {
        InteractionDialogAPI dialog = mock(InteractionDialogAPI.class, (name, args) -> {
            if (name.equals("showCustomDialog")) fixture.shown = args;
            if (name.equals("dismiss")) fixture.dismissed++;
            if (name.equals("dismissAsCancel")) fixture.canceled++;
            return null;
        });
        OrionKnotMapDialog.show(dialog);
        near(((Number) fixture.shown[0]).floatValue(),1000f,"Full-size chart width contract");
        near(((Number) fixture.shown[1]).floatValue(),820f,"Full-size chart height contract");
        CustomDialogDelegate delegate = (CustomDialogDelegate) fixture.shown[2];
        check("Close chart.".equals(delegate.getConfirmText()) && !delegate.hasCancelButton(),
                "Retain the existing single Close chart action");
        check(delegate.getCustomPanelPlugin() instanceof OrionKnotChartPanelPlugin,
                "Dialog retains the bounds-independent panel");
        delegate.createCustomDialog(fixture.panel(), null);
        delegate.customDialogConfirm();
        delegate.customDialogCancel();
        check(fixture.dismissed == 1 && fixture.canceled == 1,
                "Existing confirm and cancel dismissal behavior remains unchanged");
    }

    public static void main(String[] args) throws Exception {
        SettingsAPI previousSettings = Global.getSettings();
        SectorAPI previousSector = Global.getSector();
        try {
            Fixture fixture = new Fixture();
            checkGeometry(fixture);
            checkIcons(fixture);
            checkClouds(fixture);
            checkAtlasCrop();
            checkDialog(fixture);
            checkMissing(fixture);
        } finally {
            Global.setSettings(previousSettings);
            Global.setSector(previousSector);
        }
        System.out.println("Orion Knot chart regression passed: live six-system geometry, fleet marker, "
                + "native icons/colors, bounded read-only terrain cache, atlas clipping, missing systems, "
                + "and existing dialog contract.");
    }
}
