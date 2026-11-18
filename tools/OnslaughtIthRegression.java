import chiefnavigator.weapons.IthacaResearchUpgrades;
import chiefnavigator.weapons.DriftingWallBeamEffect;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.BeamWeaponSpecAPI;
import com.fs.starfarer.loading.specs.BaseWeaponSpec;
import com.fs.starfarer.loading.specs.HullVariantSpec;
import com.fs.starfarer.loading.specs.f;
import com.fs.starfarer.campaign.rules.Memory;
import java.awt.Color;
import java.nio.file.*;
import java.util.*;
import org.json.*;
import org.lwjgl.util.vector.Vector2f;

/** Native fitting, every installed mask, snapshot isolation and naval effects. */
public final class OnslaughtIthRegression extends WallResearchRegression {
    static final String HULL=IthacaResearchUpgrades.ITHACA_ONSLAUGHT_HULL_ID;
    static final String LASER=IthacaResearchUpgrades.ITHACA_ONSLAUGHT_LASER_ID;
    static final String MASK="chief_navigator_ithaca_research_component_mask";
    static JSONObject read(Path path)throws Exception{return new JSONObject(Files.readString(path));}
    static void near(float actual,float expected,String message){check(Math.abs(actual-expected)<.0001f,message+": "+actual+" vs "+expected);}
    static void dataAndFit()throws Exception {
        JSONObject base=read(Path.of("../../starsector-core/data/hulls/onslaught.ship"));
        JSONObject skin=read(Path.of("data/hulls/skins/"+HULL+".skin"));
        JSONObject variant=read(Path.of("data/variants/"+HULL+"_Standard.variant"));
        JSONObject weapon=read(Path.of("data/weapons/"+LASER+".wpn"));
        check(skin.getString("baseHullId").equals("onslaught") && skin.getString("hullName").equals("Onslaught (ITH)"),"Native skin inherits the Onslaught");
        check(skin.getJSONArray("removeWeaponSlots").length()==0 && skin.getJSONArray("removeEngineSlots").length()==0
                && !skin.has("weaponSlotChanges") && !skin.has("hitpoints") && !skin.has("maxSpeed"),"No mount, engine or hull-stat changes");
        check(skin.getJSONObject("builtInWeapons").length()==2,"Exactly two replacement lasers");
        JSONArray slots=base.getJSONArray("weaponSlots"); int tpcs=0;
        for(int i=0;i<slots.length();i++) {
            JSONObject slot=slots.getJSONObject(i);String id=slot.getString("id");
            if(!id.equals("WS 016")&&!id.equals("WS 017"))continue;
            check(base.getJSONObject("builtInWeapons").getString(id).equals("tpc") && skin.getJSONObject("builtInWeapons").getString(id).equals(LASER),"Replace each original TPC slot");
            check(slot.getString("mount").equals("HARDPOINT") && slot.getDouble("angle")==0 && slot.getDouble("arc")==5,"Preserve native fixed TPC arc");
            double lateral=slot.getJSONArray("locations").getDouble(1);
            check(slot.getJSONArray("locations").getDouble(0)==198 && Math.abs(lateral)==70,"Exact native TPC positions");tpcs++;
        }
        check(tpcs==2 && weapon.getJSONArray("hardpointOffsets").getDouble(0)==15,"Native TPC muzzle offsets");
        JSONObject standard=read(Path.of("../../starsector-core/data/variants/onslaught/onslaught_Standard.variant"));
        check(standard.getJSONArray("hullMods").toString().equals(variant.getJSONArray("hullMods").toString())
                && standard.getInt("fluxVents")==variant.getInt("fluxVents")
                && standard.getInt("fluxCapacitors")==variant.getInt("fluxCapacitors"),"Preserve stock standard refit budget");
        int replacements=0;
        for(int i=0;i<variant.getJSONArray("weaponGroups").length();i++) {
            JSONObject fitted=variant.getJSONArray("weaponGroups").getJSONObject(i).getJSONObject("weapons");
            JSONObject original=standard.getJSONArray("weaponGroups").getJSONObject(i).getJSONObject("weapons");
            for(Iterator<?> it=fitted.keys();it.hasNext();) {
                String slot=(String)it.next();String actual=fitted.getString(slot);
                if(slot.equals("WS 016")||slot.equals("WS 017")){check(actual.equals(LASER),"Laser loadout matches built-in");replacements++;}
                else check(actual.equals(original.getString(slot)),"Retain every other stock weapon");
            }
        }
        check(replacements==2,"Both laser mounts are grouped for player control");
        f nativeHull=new f(HULL);nativeHull.setBaseHullId("onslaught");
        HullVariantSpec nativeVariant=new HullVariantSpec(HULL+"_Standard",nativeHull);
        BaseWeaponSpec nativeLaser=new BaseWeaponSpec(LASER);
        nativeLaser.setType(WeaponAPI.WeaponType.BALLISTIC);nativeLaser.setSize(WeaponAPI.WeaponSize.LARGE);
        nativeLaser.getAIHints().add(WeaponAPI.AIHints.SYSTEM);
        for(String slot:List.of("WS 016","WS 017"))nativeVariant.addWeapon(slot,nativeLaser,false);
        check(nativeVariant.getWeaponId("WS 016").equals(LASER) && nativeVariant.getWeaponId("WS 017").equals(LASER),"Actual native variant accepts both built-in beams");
    }
    static ShipAPI freshShip(){
        Map<String,Object> data=new HashMap<>();
        ShipHullSpecAPI spec=mock(ShipHullSpecAPI.class,(n,a)->n.equals("getHullId")?HULL:null);
        return mock(ShipAPI.class,(n,a)->{
            if(n.equals("getHullSpec"))return spec;
            if(n.equals("getCustomData"))return data;
            if(n.equals("setCustomData"))data.put((String)a[0],a[1]);
            return null;
        });
    }
    static void masksAndSnapshots()throws Exception {
        JSONArray rows=(JSONArray)Class.forName("com.fs.starfarer.loading.G")
                .getMethod("o00000",String.class).invoke(null,Files.readString(Path.of("data/weapons/weapon_data.csv")));
        float baseCooldown=Float.NaN;
        for(int i=0;i<rows.length();i++) {
            JSONObject row=rows.getJSONObject(i);
            if(LASER.equals(row.optString("id")))baseCooldown=(float)row.getDouble("burst delay");
        }
        check(Float.isFinite(baseCooldown),"Load the ship laser's actual cooldown through the native CSV parser");
        Global.setSector(null);
        for(String hull:List.of("onslaught","onslaught_xiv","chief_navigator_drifting_wall_base","unrelated_mod_skin"))
            check(IthacaResearchUpgrades.getMask(ship(hull,127,0,new Vector2f()))==0,"Research must not leak to "+hull);
        for(String parent:List.of(HULL,"onslaught")) {
            ShipHullSpecAPI damagedSpec=mock(ShipHullSpecAPI.class,(n,a)->
                    n.equals("getHullId")?parent+"_d":n.equals("getDParentHullId")?parent:null);
            ShipAPI damaged=mock(ShipAPI.class,(n,a)->n.equals("getHullSpec")?damagedSpec:
                    n.equals("getCustomData")?Map.of(MASK,127):null);
            check(IthacaResearchUpgrades.getMask(damaged)==(parent.equals(HULL)?127:0),
                    "D-hull research eligibility follows the exact ITH parent, not the shared Onslaught base");
        }
        for(int mask=0;mask<=127;mask++) {
            ShipAPI source=ship(HULL,mask,0,new Vector2f());
            check(IthacaResearchUpgrades.getMask(source)==mask,"All component bits recognized on ITH");
            DriftingWallBeamEffect effect=new DriftingWallBeamEffect();
            for(int mount=0;mount<2;mount++) {
                float[] stats={2.5f,baseCooldown};int[] clones={0};
                BeamWeaponSpecAPI spec=mock(BeamWeaponSpecAPI.class,(n,a)->{
                    if(n.equals("getTurnRate"))return stats[0];
                    if(n.equals("getBurstCooldown"))return stats[1];
                    if(n.equals("setTurnRate")){check(clones[0]==1,"Clone before changing traverse");stats[0]=(Float)a[0];}
                    if(n.equals("setBurstCooldown")){check(clones[0]==1,"Clone before changing cooldown");stats[1]=(Float)a[0];}
                    return null;
                });
                WeaponAPI w=mock(WeaponAPI.class,(n,a)->{
                    if(n.equals("getId"))return LASER;
                    if(n.equals("getShip"))return source;
                    if(n.equals("getSpec"))return spec;
                    if(n.equals("getBeams"))return List.of();
                    if(n.equals("getLocation"))return new Vector2f();
                    if(n.equals("ensureClonedSpec"))clones[0]++;
                    if(n.equals("setForceFireOneFrame")||n.equals("setForceNoFireOneFrame")||n.equals("setSuspendAutomaticTurning"))throw new AssertionError("Never script the player's aim/fire controls");
                    return null;
                });
                CombatEngineAPI engine=mock(CombatEngineAPI.class,(n,a)->null);
                effect.advance(.01f,engine,w);effect.advance(.01f,engine,w);
                near(stats[0],(mask&1)!=0?10f:2.5f,"One traverse multiplier per mount");
                near(stats[1],baseCooldown*((mask&2)!=0?.4f:1f),"One cycling multiplier per mount");
            }
        }
        Memory memory=new Memory();
        memory.set("$chief_navigator_ithaca_research_consumables_migrated_v2",true);
        memory.set("$chief_navigator_ithaca_research_component_mask_v1",3);
        memory.set("$chief_navigator_ithaca_research_components_recovered_v1",3);
        Global.setSector(mock(SectorAPI.class,(n,a)->n.equals("getMemoryWithoutUpdate")?memory:null));
        ShipAPI before=freshShip();check(IthacaResearchUpgrades.getMask(before)==3,"Read installed campaign components");
        memory.set("$chief_navigator_ithaca_research_component_mask_v1",71);
        check(IthacaResearchUpgrades.getMask(before)==3 && IthacaResearchUpgrades.getMask(freshShip())==71,"Current deployment stays fixed; next deployment receives new research");
        Global.setSector(null);
    }
    static void empAndVisuals(){
        ShipAPI source=ship(HULL,4,0,new Vector2f());ShipAPI enemy=ship("enemy",0,1,new Vector2f(500,200));
        ShipAPI ally=ship("ally",0,0,new Vector2f(500,100));
        boolean[] firing={true};int[] arcs={0},particles={0},plugins={0};
        BeamAPI beam=mock(BeamAPI.class,(n,a)->{
            if(n.equals("getBrightness"))return 1f;
            if(n.equals("getFrom"))return new Vector2f();
            if(n.equals("getTo"))return new Vector2f(1000,0);
            return null;
        });
        WeaponAPI weapon=mock(WeaponAPI.class,(n,a)->{
            if(n.equals("getId"))return LASER;
            if(n.equals("getShip"))return source;
            if(n.equals("getBeams"))return firing[0]?List.of(beam):List.of();
            if(n.equals("getLocation"))return new Vector2f();
            return null;
        });
        CombatEngineAPI engine=mock(CombatEngineAPI.class,(n,a)->{
            if(n.equals("addPlugin"))plugins[0]++;
            if(n.equals("getShips"))return List.of(ally,enemy);
            if(n.equals("spawnEmpArc")){check(a[3]==enemy && (Float)a[5]==0f && (Float)a[6]==200f,"Shield-respecting, EMP-only attack on an enemy");arcs[0]++;}
            if(n.equals("addNebulaSmokeParticle")||n.equals("addNebulaParticle")||n.equals("addSmoothParticle")){
                check((Float)a[2]<=14f,"Ship-scale purge particle sizes");
                check(((Vector2f)a[0]).length()<9f,"Purge remains at the TPC barrel, not the fortress offset");particles[0]++;
            }
            return null;
        });
        DriftingWallBeamEffect effect=new DriftingWallBeamEffect();effect.advance(.25f,engine,weapon);
        check(arcs[0]==1,"Caged-Arc Projector works on the new battery");
        firing[0]=false;effect.advance(.01f,engine,weapon);
        check(plugins[0]==1 && particles[0]==0,"One shared hull effect replaces the barrel-sized purge");
    }
    static void isaScope() throws Exception {
        IsaNavalRefractionRegression.verifyHull(HULL, LASER);
    }
    public static void main(String[] args)throws Exception {
        SettingsAPI settings=Global.getSettings();SectorAPI sector=Global.getSector();
        try {
            Global.setSettings(mock(SettingsAPI.class,(n,a)->n.equals("getColor")?Color.WHITE:null));
            dataAndFit();masksAndSnapshots();empAndVisuals();isaScope();
            System.out.println("PASS: native Onslaught ITH fitting, unchanged TPC geometry/loadout, both lasers across 128 masks, scoped deployment snapshots, EMP and ship-scale effects.");
        } finally {Global.setSettings(settings);Global.setSector(sector);}
    }
}
