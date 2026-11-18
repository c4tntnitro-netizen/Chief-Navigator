import chiefnavigator.hullmods.DomaindessyAirGroup;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.*;
import com.fs.starfarer.loading.specs.HullVariantSpec;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import org.json.*;

/** Parses actual skins/variants/wing CSV and initializes native torpedo/PD damage. */
public final class NativeDomaindessyLoadRegression extends WallResearchRegression {
    static Path ROOT=Path.of(".").toAbsolutePath().normalize();
    static Path CORE=findCore();
    static Path findCore() {
        for(Path parent=ROOT;parent!=null;parent=parent.getParent())
            if(Files.isRegularFile(parent.resolve("starsector-core/starfarer.api.jar")) && Files.isRegularFile(parent.resolve("jre/bin/java.exe")))return parent.resolve("starsector-core");
        throw new IllegalStateException("Cannot locate installed Starsector above checkout");
    }
    static Object resources;
    static Class<?> resourceClass, hullType;
    static SettingsAPI nativeSettings;
    static Method lookup,register;
    static JSONObject read(Path path)throws Exception {
        return new JSONObject(Files.readString(path).replaceAll("#[^\\r\\n]*", ""));
    }
    static void invoke(Method method,Object... args)throws Exception {
        try{method.invoke(null,args);}catch(InvocationTargetException e){throw new RuntimeException(e.getCause());}
    }
    static void csv(String type)throws Exception {
        Method method=Arrays.stream(Class.forName("com.fs.starfarer.loading."+type).getMethods())
                .filter(m->Modifier.isStatic(m.getModifiers())&&m.getReturnType()==void.class&&m.getParameterCount()==0).findFirst().orElseThrow();
        invoke(method);
    }
    static void weapon(String id,Path wpn)throws Exception {
        JSONObject data=read(wpn);
        String projectile=data.optString("projectileSpecId");
        if(!projectile.isEmpty())try(var files=Files.walk(CORE.resolve("data"))) {
            String filename=(projectile.equals("locust")?"locust_srm":projectile)+".proj";
            Path path=files.filter(p->p.getFileName().toString().equals(filename)).findFirst().orElseThrow();
            NativeChargePulseLoadRegression.call("o00000",ROOT.relativize(path.normalize()));
        }
        NativeChargePulseLoadRegression.call("new",ROOT.relativize(wpn.normalize()));
    }
    public static void main(String[] args)throws Exception {
        // Reuses the fixture's isolated native ModManager; it does not touch user settings.
        NativeChargePulseDamageRegression.main(new String[0]);
        Constructor<?> settings=Class.forName("com.fs.starfarer.settings.StarfarerSettings$1").getDeclaredConstructor();
        settings.setAccessible(true);nativeSettings=(SettingsAPI)settings.newInstance();
        Global.setSettings(mock(SettingsAPI.class,(n,a)->n.equals("getFloat")?1f:n.equals("getColor")?java.awt.Color.WHITE:null));
        try{resourceClass=Class.forName("com.fs.util.C");}catch(ClassNotFoundException e){resourceClass=Class.forName("com.fs.util.Object");}
        for(Method method:resourceClass.getDeclaredMethods())if(Modifier.isStatic(method.getModifiers())&&method.getParameterCount()==0&&method.getReturnType()==resourceClass){resources=method.invoke(null);break;}
        for(String id:List.of("atropos","atropos_single","ioncannon_fighter","lightdualac"))
            weapon(id,CORE.resolve("data/weapons/"+id+".wpn"));
        weapon("chief_navigator_lightning_cloudswarm",ROOT.resolve("data/weapons/chief_navigator_lightning_cloudswarm.wpn"));
        Path fixture=ROOT.resolve("out_domaindessy_check/native_fixture");
        resourceClass.getMethod("class",String.class).invoke(resources,fixture.toString());
        csv("WeaponSpreadsheetLoader");
        ProjectileWeaponSpecAPI doubleRack=(ProjectileWeaponSpecAPI)nativeSettings.getWeaponSpec("atropos");
        ProjectileWeaponSpecAPI singleRack=(ProjectileWeaponSpecAPI)nativeSettings.getWeaponSpec("atropos_single");
        check(doubleRack.getMaxAmmo()==2&&singleRack.getMaxAmmo()==1,"Actual native double vs single Atropos ammo");
        ProjectileWeaponSpecAPI pd=(ProjectileWeaponSpecAPI)nativeSettings.getWeaponSpec("chief_navigator_lightning_cloudswarm");
        check(pd.getBurstSize()==8&&pd.getMaxAmmo()==8&&pd.getMaxRange()==800,"Native miniature Cloudswarm magazine/burst/range");
        Object missile=pd.getProjectileSpec();
        check(missile.getClass().getMethod("getDamage").invoke(missile)!=null,"Native PD missile damage initialized");

        hullType=Arrays.stream(HullVariantSpec.class.getConstructors()).filter(c->c.getParameterCount()==2&&c.getParameterTypes()[0]==String.class).findFirst().orElseThrow().getParameterTypes()[1];
        Class<?> store=Class.forName("com.fs.starfarer.loading.SpecStore");
        lookup=Arrays.stream(store.getMethods()).filter(m->Modifier.isStatic(m.getModifiers())&&m.getReturnType()==Object.class
                &&Arrays.equals(m.getParameterTypes(),new Class<?>[]{Class.class,String.class})).findFirst().orElseThrow();
        register=Arrays.stream(store.getMethods()).filter(m->Modifier.isStatic(m.getModifiers())&&m.getReturnType()==void.class
                &&Arrays.equals(m.getParameterTypes(),new Class<?>[]{Class.class,String.class,Object.class})).findFirst().orElseThrow();
        // D-hull detection consults the native built-in hullmod records. These
        // four mods are not D-mods; effects need not run in this loading fixture.
        Class<?> modType=Class.forName("com.fs.starfarer.loading.specs.O");
        for(String id:List.of("hiressensors","eccm","surveying_equipment","chief_navigator_domaindessy_air_group")) {
            Object mod=modType.getConstructor().newInstance();modType.getMethod("setId",String.class).invoke(mod,id);
            register.invoke(null,modType,id,mod);
        }
        Class<?> styleType=Arrays.stream(hullType.getMethods()).filter(m->m.getName().equals("setHullStyle")).findFirst().orElseThrow().getParameterTypes()[0];
        JSONObject styles=read(CORE.resolve("data/config/hull_styles.json"));
        for(String name:List.of("HIGH_TECH","MIDLINE"))register.invoke(null,styleType,name,styleType.getConstructor(String.class,JSONObject.class).newInstance(name,styles.getJSONObject(name)));
        Class<?> loader=Class.forName("com.fs.starfarer.loading.ShipHullSpecLoader");
        Method baseLoader=Arrays.stream(loader.getMethods()).filter(m->Modifier.isStatic(m.getModifiers())&&Arrays.equals(m.getParameterTypes(),new Class<?>[]{String.class})).findFirst().orElseThrow();
        Method skinLoader=Arrays.stream(loader.getDeclaredMethods()).filter(m->Modifier.isStatic(m.getModifiers())&&!Modifier.isPublic(m.getModifiers())&&Arrays.equals(m.getParameterTypes(),new Class<?>[]{String.class})).findFirst().orElseThrow();skinLoader.setAccessible(true);
        for(String id:List.of("odyssey","thunder","trident")) {
            invoke(baseLoader,ROOT.relativize(CORE.resolve("data/hulls/"+id+".ship")).toString().replace('\\','/'));
            Object hull=lookup.invoke(null,hullType,id);
            for(String setter:List.of("setEngineSpec","setReactorSpec","setArmorSpec")) {
                Method m=Arrays.stream(hullType.getMethods()).filter(v->v.getName().equals(setter)).findFirst().orElseThrow();
                m.invoke(hull,m.getParameterTypes()[0].getConstructor().newInstance());
            }
        }
        Set<String> ids=new HashSet<>();for(int mask=0;mask<128;mask++)ids.add(DomaindessyAirGroup.profile(mask));
        ids.add("chief_navigator_lightning");ids.add("chief_navigator_poseidon");
        for(String id:ids)invoke(skinLoader,"data/hulls/skins/"+id+".skin");
        for(String id:ids) {
            ShipHullSpecAPI hull=(ShipHullSpecAPI)lookup.invoke(null,hullType,id);
            check(hull!=null,"Actual native skin registered: "+id);
            if(id.startsWith(DomaindessyAirGroup.ROOT)) {
                check(hull.getFighterBays()==4&&hull.getBuiltInWings().size()==4,"Actual four built-in native bays: "+id+" bays="+hull.getFighterBays()+" wings="+hull.getBuiltInWings());
                check(hull.getWeaponSlot("WS 006")==null&&hull.getBuiltInWeapons().isEmpty(),"No aft missile mount or built-in naval lasers");
                check(hull.getBuiltInMods().containsAll(List.of("hiressensors","eccm","surveying_equipment","chief_navigator_domaindessy_air_group")),"Inherited native hullmods retained");
            }
        }
        for(String id:ids)if(id.startsWith(DomaindessyAirGroup.ROOT)) {
            ShipHullSpecAPI damaged=(ShipHullSpecAPI)lookup.invoke(null,hullType,id+com.fs.starfarer.api.util.Misc.D_HULL_SUFFIX);
            check(damaged!=null&&damaged.getDParentHullId().equals(id)&&damaged.getBuiltInWings().size()==4,
                    "Actual native damaged profile inherits four fixed wings: "+id);
        }
        HullVariantSpec poseidon=new HullVariantSpec(read(ROOT.resolve("data/variants/chief_navigator_poseidon_Bomber.variant")));
        check(poseidon.getWeaponId("WS 004").equals("atropos")&&poseidon.getWeaponId("WS 005").equals("atropos"),"Actual native Poseidon variant parses both double racks");
        HullVariantSpec lightning=new HullVariantSpec(read(ROOT.resolve("data/variants/chief_navigator_lightning_Fighter.variant")));
        check(lightning.getWeaponId("WS 001").equals("ioncannon_fighter")&&lightning.getWeaponId("WS 002").equals("lightdualac")&&lightning.getWeaponId("WS 003").equals("chief_navigator_lightning_cloudswarm"),"Actual native Lightning loadout");
        csv("FighterWingSpreadsheetLoader");
        for(String id:List.of("chief_navigator_lightning_wing","chief_navigator_poseidon_wing"))
            check(nativeSettings.getFighterWingSpec(id).getNumFighters()==2,"Actual native two-craft custom wing: "+id);
        Constructor<?> makeVariant=HullVariantSpec.class.getConstructor(String.class,hullType);
        for(int mask=0;mask<128;mask++)for(boolean damaged:new boolean[]{false,true}) {
            String suffix=damaged?com.fs.starfarer.api.util.Misc.D_HULL_SUFFIX:"";
            ShipHullSpecAPI base=nativeSettings.getHullSpec(DomaindessyAirGroup.ROOT+suffix);
            ShipVariantAPI candidate=(ShipVariantAPI)makeVariant.newInstance("native_air_group",base);
            DomaindessyAirGroup.configure(candidate,mask,nativeSettings);
            check(candidate.getHullSpec().getHullId().equals(DomaindessyAirGroup.profile(mask)+suffix),
                    "Research configuration resolves through actual public settings catalog");
        }
        System.out.println("PASS: actual native eight skins, fighter variants/wing CSV, four fixed bays, preserved hullmods, initialized miniature PD and four Atropos per Poseidon.");
    }
}
