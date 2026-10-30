import chiefnavigator.weapons.ColossalNavalTurretRenderer;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.CombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import java.awt.Color;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lwjgl.util.vector.Vector2f;

/** Exercises the production renderer through public API fixtures, without OpenGL. */
public final class ColossalNavalTurretRendererRegression {
    private static final String WEAPON = "chief_navigator_drifting_wall_colossal_inert";
    private static final String ART =
            "graphics/weapons/chief_navigator_drifting_wall_colossal_inert.png";
    private static final List<Sprite> resources = new ArrayList<>();

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void number(float actual, float expected, String message) {
        check(Math.abs(actual - expected) < 0.001f,
                message + ": expected " + expected + ", got " + actual);
    }

    @SuppressWarnings("unchecked")
    private static <T> T mock(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        switch (method.getName()) {
                            case "toString": return type.getSimpleName() + " fixture";
                            case "hashCode": return System.identityHashCode(proxy);
                            case "equals": return proxy == args[0];
                            default: throw new AssertionError(method);
                        }
                    }
                    return handler.invoke(proxy, method, args);
                });
    }

    private static final class Sprite implements InvocationHandler {
        final SpriteAPI api = mock(SpriteAPI.class, this);
        final float width;
        final float height;
        Color color = Color.WHITE;
        float alpha = 1f;
        float angle;
        float centerX;
        float centerY;
        float renderX;
        float renderY;
        int renders;
        int writes;

        Sprite(float width, float height) {
            this.width = width;
            this.height = height;
            centerX = width * 0.5f;
            centerY = height * 0.5f;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            switch (method.getName()) {
                case "getWidth": return width;
                case "getHeight": return height;
                case "getCenterX": return centerX;
                case "getCenterY": return centerY;
                case "getColor": return color;
                case "getAlphaMult": return alpha;
                case "getAngle": return angle;
                case "setCenter":
                    centerX = (Float) args[0]; centerY = (Float) args[1]; writes++; return null;
                case "setColor": color = (Color) args[0]; writes++; return null;
                case "setAlphaMult": alpha = (Float) args[0]; writes++; return null;
                case "setAngle": angle = (Float) args[0]; writes++; return null;
                case "setNormalBlend": writes++; return null;
                case "renderAtCenter":
                    renderX = (Float) args[0]; renderY = (Float) args[1]; renders++; return null;
                default: throw new AssertionError("Unexpected sprite operation: " + method);
            }
        }

        Vector2f worldPixel(float x, float y) {
            // Native renderAtCenter subtracts half the canvas; Sprite.render
            // translates that same half back before rotating and translating
            // -center. T(anchor-half) T(half) R T(-center) anchors this pivot.
            double radians = Math.toRadians(angle);
            float localX = x - centerX;
            float localY = y - centerY;
            return new Vector2f(renderX + (float) (localX * Math.cos(radians) - localY * Math.sin(radians)),
                    renderY + (float) (localX * Math.sin(radians) + localY * Math.cos(radians)));
        }
    }

    private static final class Fixture {
        final Map<String, Object> data = new HashMap<>();
        final List<CombatLayeredRenderingPlugin> plugins = new ArrayList<>();
        final List<Vector2f> anchors = new ArrayList<>();
        final Sprite primary = new Sprite(4f, 4f);
        final Sprite under = new Sprite(420f, 1160f);
        final Sprite glow = new Sprite(420f, 1160f);
        final Color originalUnder = new Color(230, 225, 215, 210);
        final Vector2f location = new Vector2f(100f, 200f);
        String id = WEAPON;
        boolean underPresent = true;
        boolean hardpoint;
        boolean inPlay = true;
        boolean doNotRender;
        boolean doNotRenderWeapons;
        boolean failRegistration;
        boolean hulk;
        float combinedAlpha = 0.4f;
        float angle = 90f;
        int glowQueries;
        final WeaponSlotAPI slot = mock(WeaponSlotAPI.class, (proxy, method, args) -> {
            if (method.getName().equals("isHardpoint")) return hardpoint;
            throw new AssertionError("Unexpected slot operation: " + method);
        });
        final ShipAPI ship = mock(ShipAPI.class, (proxy, method, args) -> {
            switch (method.getName()) {
                case "isDoNotRender": return doNotRender;
                case "isDoNotRenderWeapons": return doNotRenderWeapons;
                case "getCombinedAlphaMult": return combinedAlpha;
                case "isHulk": return hulk;
                case "isAlive": return !hulk;
                default: throw new AssertionError("Unexpected ship/gameplay operation: " + method);
            }
        });
        final WeaponAPI weapon = mock(WeaponAPI.class, (proxy, method, args) -> {
            switch (method.getName()) {
                case "getId": return id;
                case "getShip": return ship;
                case "getSlot": return slot;
                case "getUnderSpriteAPI": return underPresent ? under.api : null;
                case "getSprite": return primary.api;
                case "getGlowSpriteAPI": glowQueries++; return glow.api;
                case "getLocation": return location;
                case "getCurrAngle": return angle;
                default: throw new AssertionError("Unexpected weapon/gameplay operation: " + method);
            }
        });
        final CombatEngineAPI engine = mock(CombatEngineAPI.class, (proxy, method, args) -> {
            switch (method.getName()) {
                case "getCustomData": return data;
                case "isEntityInPlay":
                    check(args[0] == ship, "Only the owning ship determines render lifetime");
                    return inPlay;
                case "addLayeredRenderingPlugin":
                    check(under.color.equals(originalUnder), "Register before hiding the native duplicate");
                    if (failRegistration) throw new IllegalStateException("Expected registration failure");
                    CombatLayeredRenderingPlugin plugin = (CombatLayeredRenderingPlugin) args[0];
                    Vector2f anchor = new Vector2f();
                    CombatEntityAPI entity = mock(CombatEntityAPI.class, (unused, entityMethod, values) -> {
                        if (entityMethod.getName().equals("getLocation")) return anchor;
                        throw new AssertionError("Unexpected visual-entity operation: " + entityMethod);
                    });
                    plugins.add(plugin);
                    anchors.add(anchor);
                    plugin.init(entity);
                    return entity;
                default: throw new AssertionError("Unexpected engine/gameplay operation: " + method);
            }
        });

        Fixture() {
            under.color = originalUnder;
            primary.color = new Color(160, 180, 200, 255);
            primary.alpha = 0.25f;
        }

        CombatLayeredRenderingPlugin install() {
            check(ColossalNavalTurretRenderer.install(engine, weapon), "Renderer must install");
            return plugins.get(plugins.size() - 1);
        }
    }

    private static ViewportAPI viewport(float alpha) {
        return mock(ViewportAPI.class, (proxy, method, args) -> {
            if (method.getName().equals("getAlphaMult")) return alpha;
            throw new AssertionError("Unexpected viewport operation: " + method);
        });
    }

    private static void registrationAndRotation() {
        Fixture fixture = new Fixture();
        int initialResources = resources.size();
        CombatLayeredRenderingPlugin renderer = fixture.install();
        Sprite art = resources.get(initialResources);
        check(renderer.getActiveLayers().equals(EnumSet.of(CombatEngineLayers.PHASED_SHIPS_LAYER)),
                "Opaque art must precede native station weapons and glow");
        check(CombatEngineLayers.PHASED_SHIPS_LAYER.ordinal() + 1
                == CombatEngineLayers.STATION_WEAPONS_LAYER.ordinal(), "Native layer adjacency changed");
        check(renderer.getRenderRadius() >= 1160f, "Culling must include the full original sprite");
        number(art.width, 420f, "Original art width");
        number(art.height, 1160f, "Original art height");
        number(art.centerX, 210f, "Turret X pivot");
        number(art.centerY, 580f, "Turret Y pivot");
        check(fixture.under.color.equals(new Color(230, 225, 215, 0)),
                "Only the combat under-sprite alpha must change");
        check(ColossalNavalTurretRenderer.install(fixture.engine, fixture.weapon)
                && fixture.plugins.size() == 1 && resources.size() == initialResources + 1,
                "Repeated installation must not allocate or register again");
        for (float angle : new float[]{0f, 90f, 180f, 270f, 37f}) {
            fixture.angle = angle;
            renderer.render(CombatEngineLayers.PHASED_SHIPS_LAYER, viewport(0.5f));
            number(art.angle, angle - 90f, "Native orientation correction");
            Vector2f pivot = art.worldPixel(210f, 580f);
            number(pivot.x, fixture.location.x, "Turret pivot world X");
            number(pivot.y, fixture.location.y, "Turret pivot world Y");
        }
        number(art.alpha, 0.2f, "Combined ship/viewport alpha must not multiply primary alpha again");
        check(art.color.equals(fixture.primary.color), "Native ship shading must be retained");
        check(fixture.primary.writes == 0 && fixture.glow.writes == 0 && fixture.glowQueries == 0,
                "Logical primary and full-size native glow must remain untouched");
        fixture.location.set(-600f, 900f);
        renderer.advance(0f);
        number(fixture.anchors.get(0).x, -600f, "Culling entity follows mount X");
        number(fixture.anchors.get(0).y, 900f, "Culling entity follows mount Y");
        renderer.cleanup();
        check(fixture.under.color.equals(fixture.originalUnder), "Cleanup restores original visibility");
        check(renderer.isExpired(), "Cleaned renderer expires");
    }

    private static void hardpointAndVisibility() {
        Fixture fixture = new Fixture();
        fixture.hardpoint = true;
        CombatLayeredRenderingPlugin renderer = fixture.install();
        Sprite art = resources.get(resources.size() - 1);
        number(art.centerY, 290f, "Hardpoint quarter-height pivot");
        fixture.angle = 90f;
        renderer.render(CombatEngineLayers.PHASED_SHIPS_LAYER, viewport(1f));
        Vector2f top = art.worldPixel(210f, 1160f);
        number(top.x, 100f, "Hardpoint barrel X");
        number(top.y, 1070f, "Hardpoint barrel offset preserved");
        fixture.angle = 180f;
        renderer.render(CombatEngineLayers.PHASED_SHIPS_LAYER, viewport(1f));
        top = art.worldPixel(210f, 1160f);
        number(top.x, -770f, "Hardpoint rotated barrel offset X");
        number(top.y, 200f, "Hardpoint rotated barrel offset Y");
        int before = art.renders;
        renderer.render(CombatEngineLayers.STATION_WEAPONS_LAYER, viewport(1f));
        fixture.doNotRender = true;
        renderer.render(CombatEngineLayers.PHASED_SHIPS_LAYER, viewport(1f));
        fixture.doNotRender = false;
        fixture.doNotRenderWeapons = true;
        renderer.render(CombatEngineLayers.PHASED_SHIPS_LAYER, viewport(1f));
        fixture.doNotRenderWeapons = false;
        renderer.render(CombatEngineLayers.PHASED_SHIPS_LAYER, viewport(0f));
        check(art.renders == before, "Suppression flags, wrong layer and zero alpha must hide art");
        fixture.hulk = true;
        renderer.advance(1f);
        renderer.render(CombatEngineLayers.PHASED_SHIPS_LAYER, viewport(1f));
        check(!renderer.isExpired() && art.renders == before + 1,
                "A hulk retains its artwork while the entity remains in play");
        fixture.inPlay = false;
        renderer.advance(1f);
        renderer.render(CombatEngineLayers.PHASED_SHIPS_LAYER, viewport(1f));
        check(renderer.isExpired() && fixture.under.color.equals(fixture.originalUnder),
                "Removal expires the renderer and restores its under-sprite");
    }

    private static void failureAndOwnership() {
        Fixture foreign = new Fixture();
        foreign.id = "lightac";
        int before = resources.size();
        check(!ColossalNavalTurretRenderer.install(foreign.engine, foreign.weapon), "Foreign weapon rejected");
        Fixture noVisual = new Fixture();
        noVisual.underPresent = false;
        check(!ColossalNavalTurretRenderer.install(noVisual.engine, noVisual.weapon),
                "Older definitions and headless fixtures without an under-sprite are skipped");
        check(resources.size() == before, "Skipped weapons allocate no art");
        Fixture failed = new Fixture();
        failed.failRegistration = true;
        check(!ColossalNavalTurretRenderer.install(failed.engine, failed.weapon), "Failure reports no installation");
        check(failed.under.color.equals(failed.originalUnder) && failed.data.isEmpty(),
                "Registration failure must preserve native artwork and leave no registry entry");
        failed.failRegistration = false;
        CombatLayeredRenderingPlugin first = failed.install();
        Sprite firstArt = resources.get(resources.size() - 1);
        Fixture second = new Fixture();
        CombatLayeredRenderingPlugin other = second.install();
        Sprite secondArt = resources.get(resources.size() - 1);
        check(firstArt != secondArt, "Each mount owns an independent artwork sprite");
        Color external = new Color(10, 20, 30, 40);
        failed.under.api.setColor(external);
        first.cleanup();
        check(failed.under.color.equals(external), "Cleanup must preserve a later external visibility change");
        check(second.under.color.getAlpha() == 0 && !other.isExpired(),
                "Cleaning one mount must not alter another mount");
        other.cleanup();
    }

    public static void main(String[] args) {
        SettingsAPI old = Global.getSettings();
        try {
            Global.setSettings(mock(SettingsAPI.class, (proxy, method, values) -> {
                if (method.getName().equals("getSprite")) {
                    check(ART.equals(values[0]), "Use the unchanged original naval artwork");
                    Sprite sprite = new Sprite(420f, 1160f);
                    resources.add(sprite);
                    return sprite.api;
                }
                throw new AssertionError("Unexpected settings operation: " + method);
            }));
            registrationAndRotation();
            hardpointAndVisibility();
            failureAndOwnership();
            System.out.println("PASS production naval renderer: registration, independent art, native pivots/rotation, "
                    + "visibility/alpha, hulk lifetime, failure cleanup and untouched glow/gameplay APIs");
        } finally {
            Global.setSettings(old);
        }
    }
}
