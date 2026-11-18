import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import com.fs.starfarer.api.loading.ProjectileWeaponSpecAPI;
import java.lang.reflect.*;
import java.util.*;

/** Reproduces the RC7 raw-spec null and exercises actual native CSV damage initialization. */
public final class NativeChargePulseDamageRegression extends WallResearchRegression {
    public static void main(String[] args) throws Exception {
        NativeChargePulseLoadRegression.main(new String[0]);
        Constructor<?> constructor=Class.forName("com.fs.starfarer.settings.StarfarerSettings$1").getDeclaredConstructor();
        constructor.setAccessible(true);
        SettingsAPI settings=(SettingsAPI)constructor.newInstance();
        Global.setSettings(mock(SettingsAPI.class,(name,a)->name.equals("getFloat")?1f:null));
        // Standalone fixture: native source-mod bookkeeping without reading/writing
        // the user's enabled-mod list or starting other mods' plugins.
        Class<?> unsafeClass=Class.forName("sun.misc.Unsafe");
        Field singleton=unsafeClass.getDeclaredField("theUnsafe"); singleton.setAccessible(true);
        Object allocator=singleton.get(null);
        Class<?> mods=Class.forName("com.fs.starfarer.launcher.ModManager");
        Object emptyMods=unsafeClass.getMethod("allocateInstance",Class.class).invoke(allocator,mods);
        for(Field field:mods.getDeclaredFields()) if(field.getType()==List.class&&!Modifier.isStatic(field.getModifiers())) {
            field.setAccessible(true); field.set(emptyMods,new ArrayList<>());
        }
        Field manager=mods.getDeclaredField("instance"); manager.setAccessible(true); manager.set(null,emptyMods);
        Class<?> csv=Class.forName("com.fs.starfarer.loading.WeaponSpreadsheetLoader");
        Method init=Arrays.stream(csv.getMethods()).filter(m->Modifier.isStatic(m.getModifiers())
                &&m.getReturnType()==void.class&&m.getParameterCount()==0).findFirst().orElseThrow();
        init.invoke(null);
        for(int length:new int[]{150,188,225,263,300,338,375,413,450}) {
            String id="chief_navigator_onslaught_charge_pulse"+(length==300?"":"_"+length);
            ProjectileWeaponSpecAPI weapon=(ProjectileWeaponSpecAPI)settings.getWeaponSpec(id);
            ProjectileSpecAPI projectile=(ProjectileSpecAPI)weapon.getProjectileSpec();
            check(projectile.getLength()==length&&projectile.getWidth()==9,"Independent native geometry: "+id);
            check(projectile.getDamage()!=null&&projectile.getDamage().getDamage()==100
                    &&projectile.getDamage().getType()==DamageType.ENERGY,"CSV initialized native damage: "+id);
            Object clone=projectile.getDamage().getClass().getMethod("clone").invoke(projectile.getDamage());
            check(clone!=null&&clone!=projectile.getDamage(),"MovingRay's native damage clone succeeds");
            ProjectileSpecAPI raw=settings.getProjectileSpec("chief_navigator_onslaught_charge_pulse_shot"+(length==300?"":"_"+length));
            check(raw.getDamage()==null,"Negative control: raw override reproduces null damage");
        }
        System.out.println("PASS: actual RC7 weapon CSV initializes/clones damage for all nine pulse lengths; old raw overrides remain null.");
    }
}
