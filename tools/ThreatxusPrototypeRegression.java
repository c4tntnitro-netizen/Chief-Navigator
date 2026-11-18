import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import com.fs.starfarer.api.*;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.*;
import com.fs.starfarer.api.mission.*;
import org.json.*;
import org.lwjgl.util.vector.Vector2f;

/** External-only native RC7 loader, gun fitting, orientation and mission checks. */
public final class ThreatxusPrototypeRegression extends WallResearchRegression {
    static final String ID="chief_navigator_threatxus";
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static JSONObject json(String path)throws Exception {
        return (JSONObject)Class.forName("com.fs.starfarer.loading.LoadingUtils")
                .getMethod("Object",String.class).invoke(null,path);
    }
    static JSONArray csv(String path)throws Exception {
        return (JSONArray)Class.forName("com.fs.starfarer.loading.void").getMethod("o00000",String.class)
                .invoke(null,Files.readString(Path.of(path)));
    }
    static Object put(Class<?> type,String id,Object value)throws Exception {
        Class<?> store=Class.forName("com.fs.starfarer.loading.SpecStore");
        Method m=Arrays.stream(store.getMethods()).filter(x->Modifier.isStatic(x.getModifiers())
                &&Arrays.equals(x.getParameterTypes(),new Class<?>[]{Class.class,String.class,Object.class})).findFirst().orElseThrow();
        return m.invoke(null,type,id,value);
    }
    public static void main(String[] args)throws Exception {
        Class<?> resources=Class.forName("com.fs.util.Object");
        for(Method m:resources.getDeclaredMethods())if(Modifier.isStatic(m.getModifiers())
                &&m.getParameterCount()==0&&m.getReturnType()==resources){
            resources.getMethod("class",String.class).invoke(m.invoke(null),Path.of(".").toAbsolutePath().toString());break;
        }
        Global.setSettings(mock(SettingsAPI.class,(n,a)->n.equals("getFloat")?1f:null));
        JSONObject styles=json("../../starsector-core/data/config/hull_styles.json");
        Class<?> style=Class.forName("com.fs.starfarer.loading.specs.new");
        put(style,"HIGH_TECH",style.getConstructor(String.class,JSONObject.class).newInstance("HIGH_TECH",styles.getJSONObject("HIGH_TECH")));
        put(style,"THREAT",style.getConstructor(String.class,JSONObject.class).newInstance("THREAT",styles.getJSONObject("THREAT")));
        Class.forName("com.fs.starfarer.loading.ShipHullSpecLoader").getMethod("o00000",String.class)
                .invoke(null,"data/hulls/"+ID+".ship");
        Class<?> hullClass=Class.forName("com.fs.starfarer.loading.specs.O00o");
        ShipHullSpecAPI hull=(ShipHullSpecAPI)Class.forName("com.fs.starfarer.loading.SpecStore")
                .getMethod("super",Class.class,String.class).invoke(null,hullClass,ID);
        check(hull!=null&&hull.getHullSize()==ShipAPI.HullSize.CAPITAL_SHIP,"Native loader creates a capital hull");
        check(hull.getAllWeaponSlotsCopy().size()==15,"Twelve core gun mounts plus three separate wall mounts");
        check(new HashSet<>(hull.getBuiltInMods()).equals(Set.of("chief_navigator_threatxus_swarm_cloud","chief_navigator_threaxus_awakening")),"Scoped swarm and awakening hullmods are built in");
        check(((List<?>)hullClass.getMethod("getEngineSlots").invoke(hull)).isEmpty(),"No engine slots, flares or contrails");
        Object sprite=hullClass.getMethod("getSpriteSpec").invoke(hull);
        BoundsAPI bounds=null;
        for(Method m:sprite.getClass().getMethods())if(m.getParameterCount()==0&&BoundsAPI.class.isAssignableFrom(m.getReturnType())){
            bounds=(BoundsAPI)m.invoke(sprite);break;
        }
        check(bounds!=null&&bounds.getSegments().size()==146,"Native loader retains complete silhouette collision segments");
        JSONObject variant=json("data/variants/"+ID+"_Standard.variant");
        check(variant.getJSONArray("modules").length()==3,"One breakaway wall per triangular side");
        Set<String> attached=new HashSet<>();
        JSONObject wallAudit=new JSONObject(Files.readString(Path.of("graphics/source/threaxus_wall_shell_20261117/wall_audit.json")));
        Map<String,JSONObject> nativeSlots=new HashMap<>();
        for(int i=0;i<wallAudit.getJSONArray("attachments").length();i++){
            JSONObject slot=wallAudit.getJSONArray("attachments").getJSONObject(i).getJSONObject("slot");nativeSlots.put(slot.getString("id"),slot);
        }
        for(int i=0;i<variant.getJSONArray("modules").length();i++){
            JSONObject assignment=variant.getJSONArray("modules").getJSONObject(i);
            String slot=JSONObject.getNames(assignment)[0];check(attached.add(slot),"Unique module attachment");
            JSONObject child=json("data/variants/"+assignment.getString(slot)+".variant");
            check(child.getString("hullId").equals("chief_navigator_threaxus_wall"),"All three walls use the supplied new hull");
            Class.forName("com.fs.starfarer.loading.ShipHullSpecLoader").getMethod("o00000",String.class)
                    .invoke(null,"data/hulls/"+child.getString("hullId")+".ship");
            ShipHullSpecAPI childHull=(ShipHullSpecAPI)Class.forName("com.fs.starfarer.loading.SpecStore")
                    .getMethod("super",Class.class,String.class).invoke(null,hullClass,child.getString("hullId"));
            check(childHull!=null&&childHull.getHullSize()==ShipAPI.HullSize.FRIGATE&&childHull.getBuiltInMods().contains("always_detaches"),"Native wall hulls are separate frigate-size breakaway modules");
        }
        Map<String,String> fits=new HashMap<>();
        for(int g=0;g<variant.getJSONArray("weaponGroups").length();g++){
            JSONObject w=variant.getJSONArray("weaponGroups").getJSONObject(g).getJSONObject("weapons");
            for(String key:JSONObject.getNames(w))check(fits.put(key,w.getString(key))==null,"Each mount fitted exactly once");
        }
        for(String id:new HashSet<>(fits.values())){
            String path="../../starsector-core/data/weapons/"+id+".wpn";
            JSONObject weapon=json(path);String projectile=weapon.optString("projectileSpecId","");
            if(!projectile.isEmpty())NativeChargePulseLoadRegression.call("",Path.of("../../starsector-core/data/weapons/proj/"+projectile+".proj"));
            NativeChargePulseLoadRegression.call("",Path.of(path));
        }
        Constructor<?> c=Class.forName("com.fs.starfarer.settings.StarfarerSettings$1").getDeclaredConstructor();c.setAccessible(true);
        SettingsAPI settings=(SettingsAPI)c.newInstance();
        JSONObject definition=json("data/hulls/"+ID+".ship");
        JSONObject audit=new JSONObject(Files.readString(Path.of("graphics/source/threatxus_20261116/geometry_audit.json")));
        Map<String,JSONObject> poses=new HashMap<>();
        for(int i=0;i<audit.getJSONArray("weapon_placements").length();i++){
            JSONObject p=audit.getJSONArray("weapon_placements").getJSONObject(i);poses.put(p.getString("slot"),p);
        }
        int large=0,medium=0,small=0;
        for(WeaponSlotAPI slot:hull.getAllWeaponSlotsCopy()){
            if(slot.isStationModule()){
                check(attached.contains(slot.getId()),"Every attached module has its own native slot");
                JSONObject original=nativeSlots.get(slot.getId());JSONArray pos=original.getJSONArray("locations");
                check(Math.abs(slot.getLocation().x-pos.getDouble(0))<.01&&Math.abs(slot.getLocation().y-pos.getDouble(1))<.01
                        &&Math.abs(((slot.getAngle()-original.getDouble("angle")+540)%360)-180)<.01,"Intended triangular-side attachment coordinates and angle");
                continue;
            }
            check(slot.isTurret()&&!slot.isStationModule(),"Native normal turret conversion");
            check(slot.weaponFits(settings.getWeaponSpec(fits.get(slot.getId()))),"Native weapon fit: "+slot.getId());
            switch(slot.getSlotSize()){case LARGE:large++;break;case MEDIUM:medium++;break;case SMALL:small++;break;}
            for(float facing:new float[]{0,90,180,270}){
                ShipAPI ship=mock(ShipAPI.class,(n,a)->n.equals("getFacing")?facing:n.equals("getLocation")?new Vector2f(1000,-700):null);
                Vector2f world=slot.computePosition(ship),local=slot.getLocation();
                double radians=Math.toRadians(facing);
                float x=(float)(1000+Math.cos(radians)*local.x-Math.sin(radians)*local.y);
                float y=(float)(-700+Math.sin(radians)*local.x+Math.cos(radians)*local.y);
                check(Math.abs(world.x-x)<.01&&Math.abs(world.y-y)<.01,"Cardinal native slot transform");
                if(facing==90){JSONArray pixel=poses.get(slot.getId()).getJSONArray("painted_mount_pixel");
                    check(Math.abs(world.x-(1000+pixel.getDouble(0)-277))<.01
                            &&Math.abs(world.y-(-700+276-pixel.getDouble(1)))<.01,"PNG/local origin registration");}
            }
        }
        check(large==4&&medium==3&&small==5,"Four large, three medium, five small mounts");
        JSONObject row=null;JSONArray rows=csv("data/hulls/ship_data.csv");
        for(int i=0;i<rows.length();i++)if(rows.getJSONObject(i).optString("id").equals(ID)){
            check(row==null,"Unique prototype row");row=rows.getJSONObject(i);
        }
        check(row!=null&&row.getDouble("max speed")==10&&row.getDouble("acceleration")==2&&row.getDouble("max turn rate")==6,"Slow forward drift CSV stats");
        check(!row.getString("hints").contains("STATION")&&row.getString("hints").contains("SHIP_WITH_MODULES"),"Mobile parent with native module support");
        check(row.getString("system id").equals("chief_navigator_threatxus_fabrication"),"Factory is the parent system");
        Class.forName("com.fs.starfarer.loading.ShipHullSpecLoader").getMethod("o00000",String.class)
                .invoke(null,"data/hulls/chief_navigator_exodus_assault.ship");
        ShipHullSpecAPI assault=(ShipHullSpecAPI)Class.forName("com.fs.starfarer.loading.SpecStore")
                .getMethod("super",Class.class,String.class).invoke(null,hullClass,"chief_navigator_exodus_assault");
        check(assault.getHullSize()==ShipAPI.HullSize.DESTROYER&&assault.getAllWeaponSlotsCopy().isEmpty(),"Native Assault body has no ordinary weapon mounts");
        JSONObject assaultVariant=json("data/variants/chief_navigator_exodus_assault_Standard.variant");
        check(assaultVariant.getJSONArray("weaponGroups").length()==0,"Assault loadout carries no guns");
        int assaultRows=0;for(int i=0;i<rows.length();i++)if(rows.getJSONObject(i).optString("id").equals("chief_navigator_exodus_assault")){
            check(rows.getJSONObject(i).getString("system id").equals("chief_navigator_exodus_plague"),"Exodus Plague is the assault unit's only attack");assaultRows++;
        }
        check(assaultRows==1,"One special assault row");
        JSONObject fabVariant=json("../../starsector-core/data/variants/threat/fabricator_unit_Type450.variant");
        check(fabVariant.getString("variantId").equals(chiefnavigator.systems.ThreatxusFabricationSystem.FABRICATOR_VARIANT)
                &&fabVariant.getString("hullId").equals("fabricator_unit"),"Build ID resolves to installed stock Type450 Fabricator");
        Class.forName("com.fs.starfarer.loading.ShipHullSpecLoader").getMethod("o00000",String.class)
                .invoke(null,"../../starsector-core/data/hulls/fabricator_unit.ship");
        ShipHullSpecAPI fab=(ShipHullSpecAPI)Class.forName("com.fs.starfarer.loading.SpecStore")
                .getMethod("super",Class.class,String.class).invoke(null,hullClass,"fabricator_unit");
        check(fab.getHullSize()==ShipAPI.HullSize.CAPITAL_SHIP&&fab.getCollisionRadius()==310
                &&fabVariant.getJSONArray("weaponGroups").length()==5,"Native stock Fabricator body and complete ordinary loadout");
        JSONArray nativeRows=csv("../../starsector-core/data/hulls/ship_data.csv");boolean nativeFab=false;
        for(int i=0;i<nativeRows.length();i++)if(nativeRows.getJSONObject(i).optString("id").equals("fabricator_unit")){
            check(nativeRows.getJSONObject(i).getString("system id").equals("construction_swarm"),"Stock Fabricators retain Construction Swarm");nativeFab=true;
        }
        check(nativeFab,"Native Fabricator stats registered");
        JSONObject factory=json("data/shipsystems/chief_navigator_threatxus_fabrication.system");
        ShipSystemSpecAPI factorySpec=(ShipSystemSpecAPI)Class.forName("com.fs.starfarer.loading.specs.M")
                .getConstructor(JSONObject.class).newInstance(factory);
        check(factorySpec.getStatsScriptClassName().equals("chiefnavigator.systems.ThreatxusFabricationSystem"),"Native factory system binding");
        JSONArray systems=csv("data/shipsystems/ship_systems.csv");int factoryRows=0;
        for(int i=0;i<systems.length();i++)if(systems.getJSONObject(i).optString("id").equals("chief_navigator_threatxus_fabrication")){
            JSONObject s=systems.getJSONObject(i);factoryRows++;
            check(s.getDouble("charge up")==2&&s.getDouble("active")==1&&s.getDouble("cooldown")==25,"Factory base cycle is2s windup,1s active,25s recovery");
        }
        check(factoryRows==1,"One factory system row");
        check(factory.getBoolean("runScriptWhileIdle")&&factory.getString("aiType").equals("NONE"),"Periodic script drives both player and NPC factories");
        List<String> player=new ArrayList<>(),enemy=new ArrayList<>();int[] maps={0};boolean[] essential={false};
        MissionDefinitionAPI api=mock(MissionDefinitionAPI.class,(n,a)->{
            if(n.equals("addToFleet")){((FleetSide)a[0]==FleetSide.PLAYER?player:enemy).add((String)a[1]);}
            if(n.equals("initMap")){maps[0]++;check((Float)a[0]==-8000f&&(Float)a[3]==8000f,"Open flight map");}
            if(n.equals("defeatOnShipLoss"))essential[0]=a[0].equals("Threatxus");
            return null;
        });
        // Compile the loose mission with the game's own Janino, not just javac.
        org.codehaus.janino.SimpleCompiler compiler=new org.codehaus.janino.SimpleCompiler();
        compiler.setParentClassLoader(ThreatxusPrototypeRegression.class.getClassLoader());
        compiler.cook(Files.readString(Path.of("data/missions/chief_navigator_threatxus_test/MissionDefinition.java")));
        MissionDefinitionPlugin mission=(MissionDefinitionPlugin)compiler.getClassLoader()
                .loadClass("data.missions.chief_navigator_threatxus_test.MissionDefinition").getDeclaredConstructor().newInstance();
        mission.defineMission(api);
        check(player.equals(List.of("onslaught_Standard","eagle_Assault","hammerhead_Balanced","hammerhead_Balanced"))
                &&enemy.equals(List.of(ID+"_Standard"))&&maps[0]==1&&!essential[0],"Player strike fleet fights one enemy Threatxus without an arbitrary flagship-loss condition");
        check(csv("data/missions/mission_list.csv").getJSONObject(0).getString("mission").equals("chief_navigator_threatxus_test"),"Mission registration");
        check(json("data/missions/chief_navigator_threatxus_test/descriptor.json").getString("title").equals(definition.getString("hullName")+" Test"),"Mission title follows the in-game boss name");
        check(row.getString("name").equals(definition.getString("hullName")),"Ship display name agrees with data and sprite hull");
        System.out.println("PASS: native RC7 core/146 bounds/zero engines,12 gun fits,3 separate breakaway Threat walls/triangular anchors, mobile-parent hint, display names, assault/Fabricator specs and Janino mission callback.");
    }
}
