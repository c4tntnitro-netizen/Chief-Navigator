import com.fs.graphics.Sprite;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.combat.entities.Ship;
import java.awt.Color;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.lwjgl.util.vector.Vector2f;
import org.json.JSONObject;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.combat.entities.ship.o0OO;
import sun.misc.Unsafe;

/**
 * External, non-rendering probe of installed native animation behavior.
 * Reflection/Unsafe are confined to the harness. This creates the real native
 * disable-animation object, advances it, and captures native sprite draw calls.
 * It does not emulate a beam constructor or a campaign battle.
 */
public final class NativeGunFlashBurstProbe {
    private static final Class<?> TRACKER = type("com.fs.starfarer.combat.entities.ship.A.String");
    private static final Class<?> BURST = type("com.fs.starfarer.combat.entities.ship.A.String$Oo");
    private static final Unsafe UNSAFE = unsafe();
    private static o0OO healthStats;
    private static com.fs.starfarer.loading.specs.OoOO hullStyle;

    private static Class<?> type(String name) {
        try { return Class.forName(name); }
        catch (Exception failure) { throw new RuntimeException(failure); }
    }

    private static Unsafe unsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (Exception failure) { throw new RuntimeException(failure); }
    }

    private static Field field(Class<?> owner, String name) throws Exception {
        Field result = owner.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }

    private static Method method(Class<?> owner, String name, Class<?>... arguments) throws Exception {
        Method result = owner.getDeclaredMethod(name, arguments);
        result.setAccessible(true);
        return result;
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static final class ProbeShip extends Ship {
        // Never invoked; only the unneeded engine initialization is bypassed.
        private ProbeShip() { super(null, null, false, null, null, null); }
        @Override public float getFacing() { return 33f; }
        @Override public Vector2f getLocation() { return new Vector2f(10f, 20f); }
        @Override public o0OO getStats() { return healthStats; }
        @Override public com.fs.starfarer.loading.specs.OoOO getHullStyle() { return hullStyle; }
    }

    public static final class CaptureSprite extends Sprite {
        int renderCalls;
        float maximumWidth;
        float maximumHeight;
        float lastAngle;
        @Override public void renderAtCenter(float x, float y) {
            renderCalls++;
            maximumWidth = Math.max(maximumWidth, getWidth());
            maximumHeight = Math.max(maximumHeight, getHeight());
            lastAngle = getAngle();
        }
    }

    private static final class Result {
        final float primaryHeight, burstParameter, duration, maximumGlow;
        final int flashes, drawCalls;
        Result(float primaryHeight, float parameter, float duration, float maximumGlow,
                int flashes, int drawCalls) {
            this.primaryHeight = primaryHeight;
            this.burstParameter = parameter;
            this.duration = duration;
            this.maximumGlow = maximumGlow;
            this.flashes = flashes;
            this.drawCalls = drawCalls;
        }
        @Override public String toString() {
            return "primaryHeight=" + primaryHeight + ", nativeBurstParameter=" + burstParameter
                    + ", actualNativeFlashes=" + flashes + ", completedSeconds=" + duration
                    + ", maximumCapturedGlow=" + maximumGlow + ", capturedDrawCalls=" + drawCalls;
        }
    }

    private static Result nativeBurst(float primaryHeight) throws Exception {
        // Actual beam A.oOoO constructor bytecode 757-776 passes this primary
        // sprite height * 1.25. Constructor wiring is audited separately.
        float parameter = primaryHeight * 1.25f;
        Object tracker = UNSAFE.allocateInstance(TRACKER);
        field(TRACKER, "ô00000").setFloat(tracker, parameter);
        field(TRACKER, "super").set(tracker, UNSAFE.allocateInstance(ProbeShip.class));
        CaptureSprite capture = new CaptureSprite();
        for (Field member : TRACKER.getDeclaredFields()) {
            if (member.getType() == Sprite.class) {
                member.setAccessible(true);
                member.set(tracker, capture);
            } else if (member.getType() == Color.class
                    && !java.lang.reflect.Modifier.isStatic(member.getModifiers())) {
                member.setAccessible(true);
                member.set(tracker, Color.WHITE);
            }
        }
        Constructor<?> constructor = BURST.getDeclaredConstructor(TRACKER);
        constructor.setAccessible(true);
        Object burst = constructor.newInstance(tracker);
        Method advance = method(BURST, "o00000", float.class);
        Method render = method(BURST, "o00000", Vector2f.class, float.class);
        Method finished = method(BURST, "o00000");
        Field liveFlashes = field(BURST, "Object");
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        int steps = 0;
        for (; steps < 2400 && !(Boolean) finished.invoke(burst); steps++) {
            advance.invoke(burst, 0.05f);
            seen.addAll((List<?>) liveFlashes.get(burst));
            render.invoke(burst, new Vector2f(), 1f);
        }
        check((Boolean) finished.invoke(burst), "Native burst did not finish within 120 simulated seconds");
        check(seen.size() >= 5 && seen.size() <= Math.ceil(5f + parameter / 3f),
                "Unexpected native flash count: " + seen.size());
        check(Math.abs(capture.maximumWidth - parameter * 4f) < 0.01f,
                "Native outer-glow size differs from constructor/renderer contract: " + capture.maximumWidth);
        check(capture.renderCalls > 0, "The real native renderer must reach capture calls");
        return new Result(primaryHeight, parameter, steps * 0.05f, capture.maximumWidth,
                seen.size(), capture.renderCalls);
    }

    private static Object nativeSlot() throws Exception {
        Class<?> slotClass = type("com.fs.starfarer.loading.specs.nullsuper");
        Constructor<?> slotConstructor = slotClass.getConstructor(String.class, WeaponType.class,
                WeaponSize.class, type("com.fs.starfarer.loading.specs.nullsuper$o"),
                Vector2f.class, type("com.fs.starfarer.loading.specs.W"), float.class, float.class);
        Object node = type("com.fs.starfarer.loading.specs.W")
                .getConstructor(String.class, Vector2f.class).newInstance("probe", new Vector2f());
        return slotConstructor.newInstance("probe", WeaponType.BALLISTIC, WeaponSize.LARGE,
                null, new Vector2f(), node, 20f, 360f);
    }

    private static void underSpriteRotation() throws Exception {
        Object slot = nativeSlot();
        Class<?> slotClass = slot.getClass();
        ProbeShip ship = (ProbeShip) UNSAFE.allocateInstance(ProbeShip.class);
        CaptureSprite art = new CaptureSprite();
        art.setSize(420f, 1160f);
        Method renderUnder = type("com.fs.starfarer.combat.entities.ship.A.OoOO")
                .getMethod("o00000", Sprite.class, slotClass, Ship.class, float.class);
        renderUnder.invoke(null, art, slot, ship, 1f);
        float nativeSlotMidpoint = (Float) slotClass
                .getMethod("computeMidArcAngle", com.fs.starfarer.api.combat.ShipAPI.class).invoke(slot, ship);
        check(Math.abs(art.lastAngle - (nativeSlotMidpoint - 90f)) < 0.01f,
                "Native under-art should use the slot midpoint");
        check(art.getWidth() == 420f && art.getHeight() == 1160f,
                "Native under-art must retain its full authored dimensions");
        float desiredTurnedTurretAngle = 160f - 90f;
        check(Math.abs(art.lastAngle - desiredTurnedTurretAngle) > 1f,
                "Fixture must distinguish native fixed under-art from a turned turret");
        System.out.println("PASS native under-sprite render: full art remains420x1160, angle="
                + art.lastAngle + " from slot/hull; cannot follow a turret turned to160degrees "
                + "(desired draw angle70). Plain under-sprite replacement rejected for moving guns.");
    }

    private static Map<String, Float> nativeHealth(float primaryHeight) throws Exception {
        String soundId = "probe_damage_sound_" + primaryHeight;
        healthStats = (o0OO) UNSAFE.allocateInstance(o0OO.class);
        for (Field member : o0OO.class.getDeclaredFields()) {
            if (member.getType() == StatBonus.class) {
                member.setAccessible(true);
                StatBonus bonus = new StatBonus();
                bonus.modifyMult("probe_foundation", 20f);
                member.set(healthStats, bonus);
            }
        }
        hullStyle = (com.fs.starfarer.loading.specs.OoOO) UNSAFE.allocateInstance(
                com.fs.starfarer.loading.specs.OoOO.class);
        for (Field member : hullStyle.getClass().getDeclaredFields()) {
            if (member.getType() == String.class) {
                member.setAccessible(true);
                member.set(hullStyle, soundId);
            }
        }
        Class<?> store = type("com.fs.starfarer.loading.SpecStore");
        Class<?> soundSpec = type("com.fs.starfarer.loading.specs.K");
        store.getMethod("o00000", Class.class, String.class, Object.class).invoke(null,
                soundSpec, soundId, soundSpec.getConstructor().newInstance());
        Class<?> nativeWeaponApi = type("com.fs.starfarer.combat.systems.o00O");
        Object slot = nativeSlot();
        int[] spriteReads = {0};
        Object weapon = Proxy.newProxyInstance(nativeWeaponApi.getClassLoader(),
                new Class<?>[] {nativeWeaponApi}, (proxy, method, arguments) -> {
                    if (method.getName().equals("getSize")) return WeaponSize.LARGE;
                    if (method.getName().equals("getSlot")) return slot;
                    if (method.getName().equals("getSprite")) {
                        spriteReads[0]++;
                        CaptureSprite sprite = new CaptureSprite();
                        sprite.setSize(primaryHeight, primaryHeight);
                        return sprite;
                    }
                    throw new AssertionError("Unexpected native health dependency: " + method);
                });
        Class<?> healthType = type("com.fs.starfarer.combat.entities.ship.super");
        Object health = healthType.getConstructor(nativeWeaponApi, Ship.class).newInstance(weapon,
                UNSAFE.allocateInstance(ProbeShip.class));
        Map<String, Float> state = new LinkedHashMap<>();
        for (Field member : healthType.getDeclaredFields()) {
            if (member.getType() == float.class
                    && !java.lang.reflect.Modifier.isStatic(member.getModifiers())) {
                member.setAccessible(true);
                state.put(member.getName(), member.getFloat(health));
            }
        }
        check(spriteReads[0] == 0, "Native weapon health unexpectedly reads sprite dimensions");
        check(state.containsValue(16000f), "Native LARGE + foundation20x must retain16000weapon health");
        return state;
    }

    public static void main(String[] arguments) throws Exception {
        JSONObject fixtureSettings = new JSONObject().put("fluxPerCapacitor", 200)
                .put("dissipationPerVent", 10);
        for (Field member : type("com.fs.starfarer.settings.StarfarerSettings").getDeclaredFields()) {
            if (member.getType() == JSONObject.class
                    && java.lang.reflect.Modifier.isStatic(member.getModifiers())) {
                member.setAccessible(true);
                member.set(null, fixtureSettings);
            }
        }
        Result original = nativeBurst(1160f);
        Result smallPrimary = nativeBurst(4f);
        check(original.maximumGlow == 5800f && smallPrimary.maximumGlow == 20f,
                "Small primary must bound actual native glow sizing independently of full artwork");
        check(smallPrimary.flashes <= 7, "Four-pixel native animation has too many flashes");
        check(nativeHealth(1160f).equals(nativeHealth(4f)),
                "Changing primary image dimensions must leave native weapon health/repair state unchanged");
        System.out.println("PASS original: " + original);
        System.out.println("PASS small transparent-primary sizing candidate: " + smallPrimary);
        System.out.println("Native random flash counts/timing vary; glow bounds are deterministic. "
                + "No animation pixels, GL window, game session, weapon stats or combat outcome modified.");
        underSpriteRotation();
        JSONObject report = new JSONObject();
        report.put("method", "Actual installed String$Oo constructor/advance/render; capture-only Sprite subclass; noGL");
        report.put("actual_beam_constructor", "A.oOoO bytecode757-776: primary spritefield294 height *1.25; under spritefield289 separate");
        report.put("persistent_visual_size", "Actual A.String constructor187-238 separately caches max(10,max(primaryWidth,primaryHeight)/7); original165.714,4pxcontrol10");
        report.put("original_primary_height", 1160);
        report.put("original_disable_parameter", 1450);
        report.put("original_maximum_outer_diameter", 5800);
        report.put("control_primary_height", 4);
        report.put("control_disable_parameter", 5);
        report.put("control_maximum_outer_diameter", 20);
        report.put("control_deterministic_flash_count_upper_bound", 7);
        report.put("control_count_explanation", "Native target=5+random*(5/3), integer counter loops while below float target; ceil yieldsatmost7,not6");
        report.put("native_health", "Actual native health constructor invoked twice: LARGE +20x health gives16000; entirefloathealth/repair state equal and zero sprite reads for bothdimensions");
        report.put("original_sample", original.toString());
        report.put("four_pixel_sample", smallPrimary.toString());
        report.put("plain_under_sprite_fix", "Rejected by real native render helper: fixed slot/hullangle -37 instead of turned turretangle70");
        report.put("limits", "Separate audit proves beam-constructor formula; this probe executes native burst and health behavior, not a full combatreproduction or reportertrigger");
        Files.writeString(Path.of(arguments.length > 0 ? arguments[0] : "native_gun_flash_probe_report.json"), report.toString(2));
        System.out.println("PASS nativehealth stays16000 and entirehealth/repair stateunchanged; zero primarysprite reads. Deterministic4pxbounds: <=7flashes, <=20outerdiameter.");
    }
}
