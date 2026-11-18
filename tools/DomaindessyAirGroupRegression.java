import chiefnavigator.hullmods.DomaindessyAirGroup;
import chiefnavigator.weapons.IthacaResearchUpgrades;
import chiefnavigator.weapons.LightningCloudswarmEffect;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.loading.specs.HullVariantSpec;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import org.json.*;
import org.lwjgl.util.vector.Vector2f;

/** Real native variant locking/profile switching, research independence and closed PD targeting. */
public final class DomaindessyAirGroupRegression extends WallResearchRegression {
    static final String ROOT=DomaindessyAirGroup.ROOT;
    static final String D=com.fs.starfarer.api.util.Misc.D_HULL_SUFFIX;
    static JSONObject read(String path)throws Exception{return new JSONObject(Files.readString(Path.of(path)));}
    static final Map<String,ShipHullSpecAPI> hulls=new HashMap<>();
    static Constructor<?> hullConstructor, variantConstructor;

    static ShipHullSpecAPI hull(String id,List<String> wings,String parent)throws Exception {
        Object value=hullConstructor.newInstance(id);
        value.getClass().getMethod("setFighterBays",int.class).invoke(value,4);
        if(parent!=null)value.getClass().getMethod("setDParentHullId",String.class).invoke(value,parent);
        ShipHullSpecAPI spec=(ShipHullSpecAPI)value;
        spec.getBuiltInWings().addAll(wings);
        return spec;
    }
    static SettingsAPI settings() {
        return mock(SettingsAPI.class,(name,a)-> {
            if(name.equals("getHullSpec"))return hulls.get((String)a[0]);
            if(name.equals("getAllShipHullSpecs"))return new ArrayList<>(hulls.values());
            if(name.equals("getFighterWingSpec"))return mock(FighterWingSpecAPI.class,(n,v)->null);
            if(name.equals("getColor"))return java.awt.Color.WHITE;
            if(name.equals("getFloat"))return 1f;
            return null;
        });
    }
    static void nativeProfiles()throws Exception {
        variantConstructor=Arrays.stream(HullVariantSpec.class.getConstructors())
                .filter(c->c.getParameterCount()==2&&c.getParameterTypes()[0]==String.class).findFirst().orElseThrow();
        hullConstructor=variantConstructor.getParameterTypes()[1].getConstructor(String.class);
        Set<String> profiles=new HashSet<>();
        for(int mask=0;mask<128;mask++)profiles.add(DomaindessyAirGroup.profile(mask));
        check(profiles.size()==6,"Six unique profiles; Autoloader requires Siege");
        Map<String,List<String>> original=new HashMap<>();
        for(String id:profiles) {
            JSONObject skin=read("data/hulls/skins/"+id+".skin");
            List<String> wings=new ArrayList<>();
            for(int i=0;i<4;i++)wings.add(skin.getJSONArray("builtInWings").getString(i));
            check(skin.getInt("fighterBays")==4&&skin.getJSONObject("builtInWeapons").length()==0,"Four fixed bays and no naval lasers");
            check(skin.getJSONArray("removeWeaponSlots").length()==1
                    &&skin.getJSONArray("removeWeaponSlots").getString(0).equals("WS 006"),"Only retired missile mount removed");
            hulls.put(id,hull(id,wings,null));original.put(id,List.copyOf(wings));
            ShipHullSpecAPI damaged=hull(id+D,wings,id);
            check(damaged.isDHull(),"Native D-parent fixture");
            hulls.put(id+D,damaged);original.put(id+D,List.copyOf(wings));
        }
        for(int mask=0;mask<128;mask++)for(boolean damaged:new boolean[]{false,true}) {
            ShipVariantAPI variant=(ShipVariantAPI)variantConstructor.newInstance("test",hulls.get(ROOT+(damaged?D:"")));
            variant.addPermaMod("comp_armor");
            DomaindessyAirGroup.configure(variant,mask,settings());
            String wanted=DomaindessyAirGroup.profile(mask)+(damaged?D:"");
            check(variant.getHullSpec().getHullId().equals(wanted),"Correct native normal/D profile for mask "+mask);
            boolean interception=(mask&IthacaResearchUpgrades.INTERCEPT_MATRIX)!=0;
            boolean siege=(mask&IthacaResearchUpgrades.MUNITIONS_COMPILER)!=0;
            boolean autoloader=(mask&IthacaResearchUpgrades.AUTOLOADER_CORE)!=0;
            String fighter=interception?DomaindessyAirGroup.LIGHTNING:"broadsword_wing";
            String bomber=!siege?"hoplon_wing":autoloader?DomaindessyAirGroup.POSEIDON:"trident_wing";
            for(int i=0;i<4;i++)check(variant.getWingId(i).equals(i<2?fighter:bomber),"Exact wing composition");
            variant.setWingId(0,"talon_wing");
            check(variant.getWingId(0).equals(fighter),"Native built-in lock remains enforced");
            check(variant.getPermaMods().contains("comp_armor"),"Preserve D-mod/permanent variant state");
            for(var entry:hulls.entrySet())check(entry.getValue().getBuiltInWings().equals(original.get(entry.getKey())),"Shared hull specs never change");
        }
        ShipHullSpecAPI foreign=hull("odyssey",List.of(),null);
        ShipVariantAPI variant=(ShipVariantAPI)variantConstructor.newInstance("stock",foreign);
        DomaindessyAirGroup.configure(variant,127,settings());
        check(variant.getHullSpec()==foreign&&!DomaindessyAirGroup.owns(foreign),"Stock hull with null D-parent stays untouched");
        check(!DomaindessyAirGroup.owns(null),"Null scope rejected safely");

        Map<String,String> weapons=new HashMap<>();
        weapons.put("WS 002",IthacaResearchUpgrades.ITHACA_ODYSSEY_LASER_ID);weapons.put("WS 003","plasma");
        ShipHullSpecAPI[] current={hulls.get(ROOT)};
        ShipVariantAPI legacy=mock(ShipVariantAPI.class,(name,a)-> {
            if(name.equals("getHullSpec"))return current[0];
            if(name.equals("setHullSpecAPI"))current[0]=(ShipHullSpecAPI)a[0];
            if(name.equals("getWeaponId"))return weapons.get(a[0]);
            if(name.equals("clearSlot"))weapons.remove(a[0]);
            return null;
        });
        DomaindessyAirGroup.configure(legacy,127,settings());
        check(!weapons.containsKey("WS 002")&&weapons.get("WS 003").equals("plasma"),"Remove only retired lasers; keep conventional fitted weapons");
        com.fs.starfarer.campaign.rules.Memory memory=new com.fs.starfarer.campaign.rules.Memory();
        memory.set("$chief_navigator_ithaca_research_consumables_migrated_v2",true);
        memory.set("$chief_navigator_ithaca_research_component_mask_v1",127);
        Global.setSector(mock(com.fs.starfarer.api.campaign.SectorAPI.class,(n,a)->n.equals("getMemoryWithoutUpdate")?memory:null));
        ShipVariantAPI callback=(ShipVariantAPI)variantConstructor.newInstance("callback",hulls.get(ROOT));
        MutableShipStatsAPI stats=mock(MutableShipStatsAPI.class,(n,a)->n.equals("getVariant")?callback:null);
        DomaindessyAirGroup effect=new DomaindessyAirGroup();
        Global.setSettings(settings());effect.applyEffectsBeforeShipCreation(ShipAPI.HullSize.CAPITAL_SHIP,stats,"test");
        check(callback.getHullSpec().getHullId().equals(ROOT+"_interception_poseidon"),"Real pre-creation callback selects installed air group");
        int[] lookups={0};
        Global.setSettings(mock(SettingsAPI.class,(n,a)->{if(n.equals("getHullSpec")){lookups[0]++;throw new NoSuchMethodError("injected compatibility failure");}return null;}));
        effect.applyEffectsBeforeShipCreation(ShipAPI.HullSize.CAPITAL_SHIP,stats,"test");
        effect.applyEffectsBeforeShipCreation(ShipAPI.HullSize.CAPITAL_SHIP,stats,"test");
        check(lookups[0]==1,"Failed configuration is disabled rather than retried every callback");
        Global.setSettings(settings());Global.setSector(null);
    }

    static void data()throws Exception {
        JSONObject variant=read("data/variants/"+ROOT+"_Standard.variant");
        check(variant.getJSONArray("wings").length()==4,"Default four-wing configuration");
        Map<String,String> guns=new HashMap<>();
        for(int i=0;i<variant.getJSONArray("weaponGroups").length();i++) {
            JSONObject group=variant.getJSONArray("weaponGroups").getJSONObject(i).getJSONObject("weapons");
            for(Iterator<?> keys=group.keys();keys.hasNext();) {String slot=(String)keys.next();guns.put(slot,group.getString(slot));}
        }
        check(guns.get("WS 002").equals("autopulse")&&guns.get("WS 003").equals("hil")&&!guns.containsKey("WS 006"),"Conventional broadside loadout and removed missile mount");
        JSONObject poseidon=read("data/variants/chief_navigator_poseidon_Bomber.variant");
        JSONObject racks=poseidon.getJSONArray("weaponGroups").getJSONObject(0).getJSONObject("weapons");
        check(racks.length()==2&&racks.getString("WS 004").equals("atropos")&&racks.getString("WS 005").equals("atropos"),"Two stock double Atropos racks per Poseidon");
        JSONObject lightning=read("data/variants/chief_navigator_lightning_Fighter.variant");
        Set<String> fitted=new HashSet<>();
        for(int i=0;i<lightning.getJSONArray("weaponGroups").length();i++) {
            JSONObject w=lightning.getJSONArray("weaponGroups").getJSONObject(i).getJSONObject("weapons");
            for(Iterator<?> keys=w.keys();keys.hasNext();)fitted.add(w.getString((String)keys.next()));
        }
        check(fitted.equals(Set.of("lightdualac","ioncannon_fighter","chief_navigator_lightning_cloudswarm")),"Exact Lightning loadout");
    }

    static final class PD {
        final List<MissileAPI> missiles=new ArrayList<>(); final List<ShipAPI> ships=new ArrayList<>();
        final Vector2f origin=new Vector2f(),velocity=new Vector2f();
        MissileAIPlugin ai; int removed,flamed; float facing;
        ShipAPI source=mock(ShipAPI.class,(n,a)->n.equals("isAlive")?true:n.equals("getOwner")?0:null);
        WeaponAPI weapon=mock(WeaponAPI.class,(n,a)->n.equals("getShip")?source:n.equals("getLocation")?origin:n.equals("getRange")?800f:null);
        CombatEngineAPI engine=mock(CombatEngineAPI.class,(n,a)-> {
            if(n.equals("getMissiles"))return missiles;if(n.equals("getShips"))return ships;
            if(n.equals("isEntityInPlay"))return missiles.contains(a[0])||ships.contains(a[0]);
            if(n.equals("removeEntity"))removed++;
            return null;
        });
        MissileAPI projectile=mock(MissileAPI.class,(n,a)-> {
            if(n.equals("getLocation"))return origin;if(n.equals("getVelocity"))return velocity;
            if(n.equals("getOwner"))return 0;if(n.equals("getFacing"))return facing;
            if(n.equals("setFacing"))facing=(float)a[0];if(n.equals("setMissileAI"))ai=(MissileAIPlugin)a[0];
            if(n.equals("flameOut"))flamed++;
            return null;
        });
        MissileAPI missile(int owner,float x) {
            return mock(MissileAPI.class,(n,a)->n.equals("getOwner")?owner:n.equals("getLocation")?new Vector2f(x,0):n.equals("getVelocity")?new Vector2f():null);
        }
        ShipAPI ship(int owner,boolean fighter,boolean phased) {
            return mock(ShipAPI.class,(n,a)->n.equals("getOwner")?owner:n.equals("getLocation")?new Vector2f(50,0)
                    :n.equals("getVelocity")?new Vector2f():n.equals("isAlive")?Boolean.TRUE
                    :n.equals("isFighter")?Boolean.valueOf(fighter):n.equals("isPhased")?Boolean.valueOf(phased):null);
        }
    }
    static void pd() {
        PD test=new PD(); MissileAPI hostile=test.missile(1,500); test.missiles.add(hostile);
        test.ships.add(test.ship(1,true,false));
        new LightningCloudswarmEffect().onFire(test.projectile,test.weapon,test.engine);
        check(test.ai!=null&&((GuidedMissileAI)test.ai).getTarget()==hostile,"Hostile missiles have priority over fighters");
        test.ai.advance(.1f);check(Math.abs(test.velocity.length()-120)<.01,"Native cold ejection preserved");
        test.ai.advance(1f);check(Math.abs(test.velocity.length()-520)<.01,"Native cruise speed reached");
        test.missiles.clear();test.ships.clear();test.ai.advance(.1f);check(test.flamed==1,"Targetless missile retires without attacking ships");
        PD shipsOnly=new PD(); shipsOnly.ships.add(shipsOnly.ship(1,false,false));
        shipsOnly.missiles.add(shipsOnly.missile(0,50)); shipsOnly.missiles.add(shipsOnly.missile(100,50));
        shipsOnly.ships.add(shipsOnly.ship(1,true,true));
        new LightningCloudswarmEffect().onFire(shipsOnly.projectile,shipsOnly.weapon,shipsOnly.engine);
        check(shipsOnly.removed==1&&shipsOnly.ai==null,"Reject warships, friendly/neutral missiles and phased fighters");
        PD craft=new PD();ShipAPI enemy=craft.ship(1,true,false);craft.ships.add(enemy);
        new LightningCloudswarmEffect().onFire(craft.projectile,craft.weapon,craft.engine);
        check(((GuidedMissileAI)craft.ai).getTarget()==enemy,"Hostile fighter acquisition works");
    }
    public static void main(String[] args)throws Exception {
        Global.setSettings(settings());
        data();nativeProfiles();pd();
        System.out.println("PASS: all128 research masks x native normal/D variants, locked built-ins, stock/shared-spec isolation, conventional mounts, Poseidon racks and closed miniature PD guidance.");
    }
}
