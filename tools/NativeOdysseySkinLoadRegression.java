import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.loading.specs.f;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import org.json.JSONObject;

/** Actual native skin parsing validates inherited mounts, bays and built-in hullmods. */
public final class NativeOdysseySkinLoadRegression extends WallResearchRegression {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    static void call(Method method, String path) throws Exception {
        try { method.invoke(null, path); }
        catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof Exception) throw (Exception) failure.getCause();
            throw failure;
        }
    }
    public static void main(String[] args) throws Exception {
        Global.setSettings(mock(SettingsAPI.class, (name, values) -> name.equals("getFloat") ? 1f : null));
        NativeChargePulseLoadRegression.main(new String[]{"data/weapons/chief_navigator_odyssey_ith_naval_laser.wpn"});
        Class<?> loader = Class.forName("com.fs.starfarer.loading.ShipHullSpecLoader");
        Class<?> store = Class.forName("com.fs.starfarer.loading.SpecStore");
        Method lookup = store.getMethod("o00000", Class.class, String.class);
        JSONObject styles = null;
        for (Method method : Class.forName("com.fs.starfarer.loading.LoadingUtils").getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers()) && method.getReturnType() == JSONObject.class
                    && Arrays.equals(method.getParameterTypes(), new Class<?>[]{String.class})) {
                method.setAccessible(true);
                styles = (JSONObject) method.invoke(null, "../../starsector-core/data/config/hull_styles.json");
                break;
            }
        }
        check(styles != null, "Native JSON loader available");
        Class<?> styleClass = Class.forName("com.fs.starfarer.loading.specs.B");
        Object style = styleClass.getConstructor(String.class, JSONObject.class).newInstance("HIGH_TECH", styles.getJSONObject("HIGH_TECH"));
        store.getMethod("o00000", Class.class, String.class, Object.class).invoke(null, styleClass, "HIGH_TECH", style);
        call(loader.getMethod("o00000", String.class), "../../starsector-core/data/hulls/odyssey.ship");
        f base = (f) lookup.invoke(null, f.class, "odyssey");
        // Combat statistics and fighter bays live in ship_data.csv, loaded separately.
        base.setEngineSpec(new com.fs.starfarer.loading.specs.Oooo());
        base.setReactorSpec(new com.fs.starfarer.loading.specs.L());
        base.setArmorSpec(new com.fs.starfarer.loading.specs.G());
        base.setFighterBays(2);
        Set<String> before = new HashSet<>();
        for (var slot : base.getAllWeaponSlotsCopy()) before.add(slot.getId());
        Method skinLoader = loader.getDeclaredMethod("new", String.class); skinLoader.setAccessible(true);
        call(skinLoader, "data/hulls/skins/chief_navigator_odyssey_ith.skin");
        ShipHullSpecAPI skin = (ShipHullSpecAPI) lookup.invoke(null, f.class, "chief_navigator_odyssey_ith");
        Set<String> after = new HashSet<>();
        for (var slot : skin.getAllWeaponSlotsCopy()) after.add(slot.getId());
        before.remove("WS 006");
        check(after.equals(before), "Native skin removes only WS006, keeping the launch bay and all other mounts");
        check(base.getWeaponSlotAPI("WS 006") != null && base.getFighterBays() == 2, "Stock Odyssey remains unchanged");
        check(skin.getFighterBays() == 3, "Native skin has exactly three fighter bays");
        check(skin.getBuiltInMods().containsAll(List.of("hiressensors", "eccm", "surveying_equipment")),
                "Native skin adds Surveying Equipment while retaining stock built-ins");
        for (String slot : List.of("WS 002", "WS 003"))
            check("chief_navigator_odyssey_ith_naval_laser".equals(skin.getBuiltInWeapons().get(slot)), "Keep both built-in naval lasers");
        System.out.println("PASS: actual native Odyssey skin loader, aft mount removal, third bay, inherited launch bay/built-ins and stock-hull isolation.");
    }
}
