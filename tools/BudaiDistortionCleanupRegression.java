import chiefnavigator.hullmods.CharybdisDistortion;
import chiefnavigator.systems.BudaiConvulsiveLungeSystem;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiFunction;
import org.dark.shaders.distortion.DistortionAPI;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.dark.shaders.util.ShaderLib;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;

/** Uses GraphicsLib's real registry, with no hullmod callbacks after death/removal. */
public final class BudaiDistortionCleanupRegression {
    private static final String RIPPLE = "chief_navigator_charybdis_persistent_distortion";
    private static final String RADIUS = "chief_navigator_charybdis_distortion_radius";
    private static final String EXPANSION = "chief_navigator_charybdis_bubble_expansion";

    private static <T> T mock(Class<T> type, BiFunction<String, Object[], Object> call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> {
                    if (method.getName().equals("equals")) return proxy == args[0];
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("toString")) return type.getSimpleName() + "Fixture";
                    Object value = call.apply(method.getName(), args);
                    if (value != null) return value;
                    return switch (method.getReturnType().getName()) {
                        case "boolean" -> false;
                        case "float" -> 0f;
                        case "int" -> 0;
                        case "long" -> 0L;
                        case "double" -> 0d;
                        default -> null;
                    };
                }));
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field result = type.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static final class Vessel {
        final Map<String, Object> data = new HashMap<>();
        final Map<String, MutableStat> movement = new HashMap<>();
        final Vector2f location = new Vector2f();
        final ShipAPI ship;
        boolean alive = true;
        boolean hulk;

        Vessel(String id) {
            MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class,
                    (name, args) -> movement.computeIfAbsent(name, key -> new MutableStat(100f)));
            ship = mock(ShipAPI.class, (name, args) -> switch (name) {
                case "getId" -> id;
                case "isAlive", "hasTag" -> name.equals("hasTag") || alive;
                case "isHulk" -> hulk;
                case "getHullLevel" -> 1f;
                case "getShipAI", "setShipAI", "resetDefaultAI", "setRetreating" ->
                        throw new AssertionError("Budai's field must leave native piloting and retreat decisions alone");
                case "getLocation" -> location;
                case "getCustomData" -> data;
                case "getMutableStats" -> stats;
                case "setCustomData" -> data.put((String) args[0], args[1]);
                case "removeCustomData" -> data.remove(args[0]);
                default -> null;
            });
        }

        void speed(float expected) {
            for (String getter : List.of("getMaxSpeed", "getAcceleration", "getDeceleration",
                    "getMaxTurnRate", "getTurnAcceleration")) {
                check(Math.abs(movement.get(getter).getModifiedValue() - expected) < .001f,
                        getter + " should be " + expected);
            }
        }
    }

    private static final class Arena {
        final Map<String, Object> data = new HashMap<>();
        final List<ShipAPI> ships = new ArrayList<>();
        final List<EveryFrameCombatPlugin> plugins = new ArrayList<>();
        final Set<ShipAPI> removed = Collections.newSetFromMap(new IdentityHashMap<>());
        final List<DistortionAPI> distortions;
        final CombatEngineAPI engine;
        boolean paused;

        @SuppressWarnings("unchecked")
        Arena() throws Exception {
            Class<?> localType = Class.forName("org.dark.shaders.distortion.DistortionShader$LocalData");
            Constructor<?> constructor = localType.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object local = constructor.newInstance();
            distortions = (List<DistortionAPI>) field(localType, "distortions").get(local);
            data.put("shaderlib_DistortionShader", local);
            engine = mock(CombatEngineAPI.class, (name, args) -> switch (name) {
                case "getCustomData" -> data;
                case "getShips" -> ships;
                case "isPaused" -> paused;
                case "isEntityInPlay" -> !removed.contains(args[0]);
                case "getFleetManager" ->
                        throw new AssertionError("Budai's field must not override fleet retreat policy or orders");
                case "addPlugin" -> plugins.add((EveryFrameCombatPlugin) args[0]);
                case "removePlugin" -> plugins.remove(args[0]);
                default -> null;
            });
            Global.setCombatEngine(engine);
        }

        RippleDistortion activate(Vessel source) {
            if (!ships.contains(source.ship)) ships.add(source.ship);
            // Zero elapsed skips unrelated enrage/AI/lightning, preserving the real field path.
            new CharybdisDistortion().advanceInCombat(source.ship, 0f);
            RippleDistortion ripple = (RippleDistortion) source.data.get(RIPPLE);
            check(distortions.contains(ripple), "The field must enter GraphicsLib's real registry");
            return ripple;
        }

        void tick(float amount) {
            for (EveryFrameCombatPlugin plugin : List.copyOf(plugins)) {
                plugin.advance(amount, Collections.emptyList());
            }
        }
    }

    private static void cleanupCase(int cause, boolean paused) throws Exception {
        Arena arena = new Arena();
        Vessel budai = new Vessel("budai");
        Vessel target = new Vessel("target");
        arena.ships.add(target.ship);
        target.movement.put("getMaxSpeed", new MutableStat(100f));
        target.movement.get("getMaxSpeed").modifyMult("foreign_effect", .8f);
        RippleDistortion foreign = new RippleDistortion();
        DistortionShader.addDistortion(foreign);
        RippleDistortion ripple = arena.activate(budai);
        new CharybdisDistortion().advanceInCombat(budai.ship, .01f);
        check(target.movement.get("getMaxSpeed").getModifiedValue() == 40f,
                "The live bubble must stack with an unrelated speed modifier");
        budai.data.put(CharybdisDistortion.ENRAGE_SEQUENCE_KEY, new Object());
        budai.data.put(EXPANSION, new Object());
        budai.data.put(BudaiConvulsiveLungeSystem.TARGET_KEY, new Vector2f());
        if (cause == 0) budai.alive = false;
        if (cause == 1) budai.hulk = true;
        if (cause == 2) {
            arena.removed.add(budai.ship);
            arena.ships.remove(budai.ship);
        }
        arena.paused = paused;
        // Deliberately never advance the hullmod after the ship dies or disappears.
        arena.tick(paused ? 0f : 1f / 60f);
        check(!arena.distortions.contains(ripple), "Dead/removed Budai must release his impulse shader");
        check(arena.distortions.equals(List.of(foreign)), "Unrelated distortions must survive");
        check(!budai.data.containsKey(RIPPLE)
                        && !budai.data.containsKey(EXPANSION)
                        && !budai.data.containsKey(CharybdisDistortion.ENRAGE_SEQUENCE_KEY)
                        && !budai.data.containsKey(BudaiConvulsiveLungeSystem.TARGET_KEY),
                "Cleanup must release the field and active lunge/expansion state");
        check(target.movement.get("getMaxSpeed").getModifiedValue() == 80f,
                "Cleanup must preserve unrelated movement modifiers");
        budai.speed(100f);
        check(arena.plugins.isEmpty(), "The cleanup plugin must unregister itself");
        arena.tick(1f);
        new CharybdisDistortion().advanceInCombat(budai.ship, 0f);
        check(arena.distortions.equals(List.of(foreign)), "A late death callback must be idempotent");
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Global.setSettings(mock(SettingsAPI.class,
                (name, call) -> name.equals("loadJSON") ? new JSONObject() : null));
        // Headless harness only: enable the real registry without constructing a GL context.
        field(ShaderLib.class, "shadersAllowed").setBoolean(null, false);
        DistortionShader shader = new DistortionShader();
        field(DistortionShader.class, "enabled").setBoolean(shader, true);
        ((List<Object>) field(ShaderLib.class, "shaders").get(null)).add(shader);
        field(RippleDistortion.class, "pathsSet").setBoolean(null, true);

        for (int cause = 0; cause < 3; cause++) {
            cleanupCase(cause, false);
            cleanupCase(cause, true);
        }
        Arena arena = new Arena();
        Vessel first = new Vessel("first");
        Vessel second = new Vessel("second");
        RippleDistortion ripple = arena.activate(first);
        check(!ripple.advance(120f), "The maintained live field should remain held");
        for (int i = 0; i < 10; i++) arena.activate(first);
        check(arena.plugins.size() == 1 && arena.distortions.size() == 1,
                "Repeated hullmod callbacks must reuse one ripple and one cleanup plugin");
        first.location.set(123f, -456f);
        first.data.put(RADIUS, 4000f);
        arena.activate(first);
        check(ripple.getLocation().equals(first.location) && ripple.getSize() == 4000f
                        && ripple.getFrameRate() == 0f && ripple.getCurrentFrame() == 30f,
                "Living Budai must retain the moving, expanded, fixed-frame field");
        RippleDistortion other = arena.activate(second);
        first.alive = false;
        arena.tick(0f);
        check(arena.distortions.equals(List.of(other)) && arena.plugins.size() == 1,
                "One source's death must preserve another source's field and cleanup");
        second.speed(50f);
        second.alive = false;
        // Exercise hullmod-first cleanup as well as the independent callback path.
        new CharybdisDistortion().advanceInCombat(second.ship, 0f);
        arena.tick(0f);
        check(arena.distortions.isEmpty() && arena.plugins.isEmpty(), "Both cleanup orders must finish");
        Arena replacement = new Arena();
        Vessel source = new Vessel("replacement");
        RippleDistortion stale = replacement.activate(source);
        source.data.remove(RIPPLE);
        RippleDistortion current = replacement.activate(source);
        replacement.tick(0f);
        check(replacement.distortions.equals(List.of(current)) && !replacement.distortions.contains(stale)
                        && source.data.get(RIPPLE) == current && replacement.plugins.size() == 1,
                "A stale callback must remove only its captured ripple and preserve the replacement");
        System.out.println("PASS: native piloting/retreat ownership; Budai distortion cleanup without post-death hullmod callbacks; death, hulk, removal, pause, field ownership, movement restoration and live field behavior");
    }
}
