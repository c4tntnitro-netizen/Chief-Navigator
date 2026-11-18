import chiefnavigator.systems.ExodusPlagueSystem;
import chiefnavigator.systems.ExodusPlaguePacketEffect;
import com.fs.starfarer.api.*;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import org.json.*;

/** Native RC7 system configuration, hidden missile helper and CSV damage initialization. */
public final class ExodusPlagueNativeLoadRegression extends WallResearchRegression {
    static final String ID=ExodusPlagueSystem.SYSTEM_ID,PACKET=ExodusPlaguePacketEffect.PACKET_ID;
    static JSONObject json(String path)throws Exception{return new JSONObject(Files.readString(Path.of(path)));}
    static JSONArray csv(String path)throws Exception {
        Class<?> parser;try{parser=Class.forName("com.fs.starfarer.loading.G");}
        catch(ClassNotFoundException rc7){parser=Class.forName("com.fs.starfarer.loading.void");}
        return (JSONArray)parser.getMethod("o00000",String.class).invoke(null,Files.readString(Path.of(path)));
    }
    static JSONObject row(String path,String id)throws Exception {
        JSONArray rows=csv(path);JSONObject found=null;
        for(int i=0;i<rows.length();i++)if(rows.getJSONObject(i).optString("id").equals(id)) {
            check(found==null,"Duplicate ID "+id);found=rows.getJSONObject(i);
        }
        return found;
    }
    public static void main(String[] args)throws Exception {
        Class<?> resources;try{resources=Class.forName("com.fs.util.C");}
        catch(ClassNotFoundException rc7){resources=Class.forName("com.fs.util.Object");}
        for(Method method:resources.getDeclaredMethods())if(Modifier.isStatic(method.getModifiers())
                &&method.getParameterCount()==0&&method.getReturnType()==resources) {
            resources.getMethod("class",String.class).invoke(method.invoke(null),Path.of(".").toAbsolutePath().toString());break;
        }
        NativeChargePulseLoadRegression.call("o00000",Path.of("data/weapons/proj/"+PACKET+"_shot.proj"));
        NativeChargePulseLoadRegression.call("new",Path.of("data/weapons/"+PACKET+".wpn"));
        Global.setSettings(mock(SettingsAPI.class,(n,a)->n.equals("getFloat")?1f:null));
        // Empty standalone mod registry: no plugins, user mod-list edits or campaign initialization.
        Class<?> unsafeClass=Class.forName("sun.misc.Unsafe");Field field=unsafeClass.getDeclaredField("theUnsafe");field.setAccessible(true);
        Object allocator=field.get(null);Class<?> mods=Class.forName("com.fs.starfarer.launcher.ModManager");
        Object empty=unsafeClass.getMethod("allocateInstance",Class.class).invoke(allocator,mods);
        for(Field f:mods.getDeclaredFields())if(f.getType()==List.class&&!Modifier.isStatic(f.getModifiers())){f.setAccessible(true);f.set(empty,new ArrayList<>());}
        Field manager=mods.getDeclaredField("instance");manager.setAccessible(true);manager.set(null,empty);
        Class<?> loader=Class.forName("com.fs.starfarer.loading.WeaponSpreadsheetLoader");
        Arrays.stream(loader.getMethods()).filter(m->Modifier.isStatic(m.getModifiers())&&m.getReturnType()==void.class
                &&m.getParameterCount()==0).findFirst().orElseThrow().invoke(null);
        Constructor<?> constructor=Class.forName("com.fs.starfarer.settings.StarfarerSettings$1").getDeclaredConstructor();constructor.setAccessible(true);
        SettingsAPI settings=(SettingsAPI)constructor.newInstance();
        ProjectileWeaponSpecAPI helper=(ProjectileWeaponSpecAPI)settings.getWeaponSpec(PACKET);
        MissileSpecAPI missile=(MissileSpecAPI)helper.getProjectileSpec();
        check(missile.getDamage()!=null&&missile.getDamage().getDamage()==50&&missile.getDamage().getType()==DamageType.KINETIC,
                "Native CSV initializes kinetic packet damage before null-weapon system spawning");
        check(missile.getDamage().getClass().getMethod("clone").invoke(missile.getDamage())!=null,"Native damage clone succeeds");
        check(missile.getLaunchSpeed()==900&&missile.getMaxFlightTime()==8.4f&&missile.isFizzleOnReachingWeaponRange(),"Range/speed/lifetime retained");
        check(!missile.isUseProjectileRangeCalculation(),"System helper never dereferences a missing fitted weapon during construction");
        check(helper.getAIHints().contains(WeaponAPI.AIHints.SYSTEM),"Packet helper stays hidden as a system-only definition");
        check(settings.getAllWeaponSpecs().stream().noneMatch(s->s.getWeaponId().equals(PACKET)),"Helper is absent from refit/console catalog");
        JSONObject systemDefinition=json("data/shipsystems/"+ID+".system");
        ShipSystemSpecAPI spec=(ShipSystemSpecAPI)Class.forName("com.fs.starfarer.loading.specs.M")
                .getConstructor(JSONObject.class).newInstance(systemDefinition);
        check(spec.getId().equals(ID)&&spec.getStatsScriptClassName().equals(ExodusPlagueSystem.class.getName())
                &&spec.getAIScriptClassName().equals("chiefnavigator.systems.ExodusPlagueAI"),"Native system JSON binds scripts");
        JSONObject system=row("data/shipsystems/ship_systems.csv",ID);
        check(system!=null&&system.getString("name").equals("Exodus Plague")&&system.getDouble("charge up")==1
                &&system.getDouble("active")==5&&system.getDouble("down")==0&&system.getDouble("cooldown")==20,"Native system CSV timings/name");
        JSONArray ships=csv("data/hulls/ship_data.csv");int found=0;
        for(int i=0;i<ships.length();i++){JSONObject s=ships.getJSONObject(i);
            if(s.optString("id").equals("chief_navigator_scylla_base")){check(s.getString("system id").equals("extraction_protocol"),"Gautama restores native Extraction Protocol");found++;}}
        check(found==1,"One Gautama base row");
        check(row("data/shipsystems/ship_systems.csv","extraction_protocol")==null,"Native Extraction Protocol is not overridden");
        ShipSystemSpecAPI extraction=(ShipSystemSpecAPI)Class.forName("com.fs.starfarer.loading.specs.M")
                .getConstructor(JSONObject.class).newInstance(json("../../starsector-core/data/shipsystems/extraction_protocol.system"));
        check(extraction.getId().equals("extraction_protocol")&&extraction.getStatsScriptClassName().equals(
                "com.fs.starfarer.api.impl.combat.threat.ExtractionProtocolSystemScript"),"Restored system uses the installed native implementation");
        check(row("data/weapons/weapon_data.csv","chief_navigator_threat_swarm_stream")==null,"Old selectable weapon row removed");
        check(!Files.exists(Path.of("data/weapons/chief_navigator_threat_swarm_stream.wpn"))
                &&!Files.exists(Path.of("data/weapons/proj/chief_navigator_threat_swarm_stream_shot.proj")),"Old weapon definitions removed");
        JSONObject hull=json("data/hulls/chief_navigator_scylla_base.ship");
        for(int i=0;i<hull.getJSONArray("weaponSlots").length();i++)check(!Set.of("WS 036","WS 037").contains(
                hull.getJSONArray("weaponSlots").getJSONObject(i).getString("id")),"Launcher mounts removed");
        for(String skin:List.of("chief_navigator_scylla","chief_navigator_scylla_final","chief_navigator_scylla_reincarnating")) {
            JSONObject s=json("data/hulls/skins/"+skin+".skin");check(!s.has("systemId")&&s.getString("baseHullId").equals(
                    "chief_navigator_scylla_base"),"All Gautama skins inherit restored Extraction Protocol");
        }
        JSONObject description=row("data/strings/descriptions.csv",ID);check(description!=null,"System tooltip registered");
        System.out.println("PASS: Gautama restores native Extraction Protocol across all three skins; Exodus Plague scripts/timing/hidden helper damage and tooltip remain available for the future boss; removed weapon/mounts stay removed.");
    }
}
