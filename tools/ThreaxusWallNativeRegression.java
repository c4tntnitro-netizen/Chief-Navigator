import com.fs.starfarer.api.*;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.*;
import org.json.*;
import org.lwjgl.util.vector.Vector2f;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

/** Actual installed RC7 hull/weapon parser and world transforms for all three walls. */
public final class ThreaxusWallNativeRegression extends WallResearchRegression {
    static JSONObject json(String p)throws Exception{return ThreatxusPrototypeRegression.json(p);}
    static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    public static void main(String[] args)throws Exception{
        Class<?> resources=Class.forName("com.fs.util.Object");
        for(Method m:resources.getDeclaredMethods())if(Modifier.isStatic(m.getModifiers())&&m.getParameterCount()==0&&m.getReturnType()==resources){
            resources.getMethod("class",String.class).invoke(m.invoke(null),Path.of(".").toAbsolutePath().toString());break;
        }
        Global.setSettings(mock(SettingsAPI.class,(n,a)->n.equals("getFloat")?1f:null));
        JSONObject styles=json("../../starsector-core/data/config/hull_styles.json");Class<?> style=Class.forName("com.fs.starfarer.loading.specs.new");
        for(String s:List.of("HIGH_TECH","THREAT"))ThreatxusPrototypeRegression.put(style,s,style.getConstructor(String.class,JSONObject.class).newInstance(s,styles.getJSONObject(s)));
        Class<?> hullClass=Class.forName("com.fs.starfarer.loading.specs.O00o");
        Class<?> loader=Class.forName("com.fs.starfarer.loading.ShipHullSpecLoader");
        for(String id:List.of("chief_navigator_threatxus","chief_navigator_threaxus_wall"))loader.getMethod("o00000",String.class).invoke(null,"data/hulls/"+id+".ship");
        var getter=Class.forName("com.fs.starfarer.loading.SpecStore").getMethod("super",Class.class,String.class);
        ShipHullSpecAPI parent=(ShipHullSpecAPI)getter.invoke(null,hullClass,"chief_navigator_threatxus");
        ShipHullSpecAPI wall=(ShipHullSpecAPI)getter.invoke(null,hullClass,"chief_navigator_threaxus_wall");
        JSONObject variant=json("data/variants/chief_navigator_threaxus_wall_Standard.variant");
        JSONObject definition=json("data/hulls/chief_navigator_threaxus_wall.ship");
        check(definition.getInt("width")==513&&definition.getInt("height")==225,"Exact supplied art dimensions");
        check(wall.getHullSize()==ShipAPI.HullSize.FRIGATE&&wall.getBuiltInMods().containsAll(List.of("fragment_swarm","threat_hullmod","always_detaches")),"Separate frigate module with native ammunition and breakaway hullmods");
        Map<String,String> fits=new HashMap<>();
        for(int i=0;i<variant.getJSONArray("weaponGroups").length();i++){
            JSONObject g=variant.getJSONArray("weaponGroups").getJSONObject(i);check(g.getBoolean("autofire"),"Every wall group is autonomous");
            JSONObject w=g.getJSONObject("weapons");for(String key:JSONObject.getNames(w))check(fits.put(key,w.getString(key))==null,"No duplicate fitted slot");
        }
        for(String id:new HashSet<>(fits.values())){
            String path=(id.startsWith("chief_navigator_")?"data/weapons/":"../../starsector-core/data/weapons/")+id+".wpn";JSONObject w=json(path);
            if(w.has("projectileSpecId"))NativeChargePulseLoadRegression.call("",Path.of("../../starsector-core/data/weapons/proj/"+w.getString("projectileSpecId")+".proj"));
            NativeChargePulseLoadRegression.call("",Path.of(path));
        }
        Constructor<?> ctor=Class.forName("com.fs.starfarer.settings.StarfarerSettings$1").getDeclaredConstructor();ctor.setAccessible(true);
        SettingsAPI settings=(SettingsAPI)ctor.newInstance();
        int l=0,m=0,s=0,h=0;
        for(WeaponSlotAPI slot:wall.getAllWeaponSlotsCopy()){
            check(slot.weaponFits(settings.getWeaponSpec(fits.get(slot.getId()))),"Actual native fit for "+slot.getId());
            switch(slot.getSlotSize()){case LARGE:l++;break;case MEDIUM:m++;break;case SMALL:s++;break;}
            if(slot.isHardpoint()){h++;check(fits.get(slot.getId()).equals("chief_navigator_threaxus_swarm_launcher"),"Every hardpoint carries the wall's native-stat Swarm Launcher");}
        }
        check(l==2&&m==4&&s==8&&h==2&&fits.size()==14,"Two large/four medium/eight small including two launcher hardpoints");
        JSONObject audit=new JSONObject(Files.readString(Path.of("graphics/source/threaxus_wall_shell_20261117/wall_audit.json")));
        Map<String,JSONObject> plans=new HashMap<>();for(int i=0;i<3;i++){JSONObject a=audit.getJSONArray("attachments").getJSONObject(i);plans.put(a.getJSONObject("slot").getString("id"),a);}
        for(WeaponSlotAPI attachment:parent.getAllWeaponSlotsCopy())if(attachment.isStationModule()){
            JSONObject plan=plans.get(attachment.getId());check(plan!=null,"Three expected side attachments");
            JSONArray normal=plan.getJSONArray("outward_pixel_normal");
            for(float facing:new float[]{0,90,180,270}){
                ShipAPI core=mock(ShipAPI.class,(n,a)->n.equals("getFacing")?facing:n.equals("getLocation")?new Vector2f(1200,-800):null);
                Vector2f pivot=attachment.computePosition(core);float childFacing=facing+attachment.getAngle();
                ShipAPI child=mock(ShipAPI.class,(n,a)->n.equals("getFacing")?childFacing:n.equals("getLocation")?pivot:null);
                double f=Math.toRadians(facing),px=-normal.getDouble(1),py=-normal.getDouble(0);
                double expectedX=Math.cos(f)*px-Math.sin(f)*py,expectedY=Math.sin(f)*px+Math.cos(f)*py;
                for(WeaponSlotAPI slot:wall.getAllWeaponSlotsCopy()){
                    double bearing=Math.toRadians(childFacing+slot.getAngle());
                    check(Math.cos(bearing)*expectedX+Math.sin(bearing)*expectedY>.9999,"Every mount faces outward at each cardinal parent facing");
                    Vector2f position=slot.computePosition(child),local=slot.getLocation();double angle=Math.toRadians(childFacing);
                    check(Math.abs(position.x-(pivot.x+Math.cos(angle)*local.x-Math.sin(angle)*local.y))<.01
                            &&Math.abs(position.y-(pivot.y+Math.sin(angle)*local.x+Math.cos(angle)*local.y))<.01,"Actual module weapon transform preserves mounting pixels");
                }
            }
        }
        System.out.println("PASS: native513x225 wall,14 actual Threat weapon fits,2SwarmLauncher hardpoints,3side poses/all4cardinal outward bearings and source-pixel mount transforms.");
    }
}
