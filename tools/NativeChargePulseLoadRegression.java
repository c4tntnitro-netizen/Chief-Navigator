import java.lang.reflect.*;
import java.nio.file.*;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;

/** Exercises the actual .proj/.wpn loader, not just JSON syntax or CSV parsing. */
public final class NativeChargePulseLoadRegression {
    static void call(String name, Path path) throws Exception {
        try {
            Class<?> loader=Class.forName("com.fs.starfarer.loading.WeaponSpecLoader");
            boolean rc7=java.util.Arrays.stream(loader.getMethods()).anyMatch(m->m.getName().equals("super")
                    &&java.util.Arrays.equals(m.getParameterTypes(),new Class<?>[]{String.class}));
            String projectileMethod=rc7?"super":"o00000";
            Method target=path.toString().endsWith(".proj")?loader.getMethod(projectileMethod,String.class):
                    java.util.Arrays.stream(loader.getMethods()).filter(m->Modifier.isStatic(m.getModifiers())
                            &&m.getReturnType()==void.class&&java.util.Arrays.equals(m.getParameterTypes(),new Class<?>[]{String.class})
                            &&!m.getName().equals(projectileMethod)).findFirst().orElseThrow();
            target.invoke(null, path.toString().replace('\\', '/'));
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof Exception) throw (Exception) cause;
            throw failure;
        }
    }
    public static void main(String[] args) throws Exception {
        Class<?> resources;
        try { resources=Class.forName("com.fs.util.C"); }
        catch(ClassNotFoundException rc7) { resources=Class.forName("com.fs.util.Object"); }
        for (Method method : resources.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 0
                    && method.getReturnType() == resources) {
                Object manager = method.invoke(null);
                resources.getMethod("class", String.class).invoke(manager, Path.of(".").toAbsolutePath().toString());
                break;
            }
        }
        call("o00000", Path.of("data/weapons/proj/chief_navigator_onslaught_charge_pulse_shot.proj"));
        for(int length:new int[]{150,188,225,263,338,375,413,450}) {
            String id="chief_navigator_onslaught_charge_pulse_shot_"+length;
            call("o00000",Path.of("data/weapons/proj/"+id+".proj"));
            call("new",Path.of("data/weapons/chief_navigator_onslaught_charge_pulse_"+length+".wpn"));
        }
        call("new", Path.of(args.length == 0 ? "data/weapons/chief_navigator_onslaught_charge_pulse.wpn" : args[0]));
        System.out.println("PASS: native projectile and weapon-spec loading completed.");
    }
}
