import chiefnavigator.quest.*;
import chiefnavigator.weapons.*;
import chiefnavigator.hullmods.DomaindessyAirGroup;
import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.*;
import java.lang.reflect.*;
import java.util.*;
import org.apache.log4j.Level;
import org.lwjgl.util.vector.Vector2f;

/** Real mod entry points with counted API fixtures, not an FPS benchmark. */
public final class CampaignPerformanceRegression {
    interface Call { Object run(String name, Object[] args); }
    static final Map<String, Long> counts = new TreeMap<>();
    static void count(String key) { counts.merge(key, 1L, Long::sum); }
    static void check(boolean b, String message) { if (!b) throw new AssertionError(message + " " + counts); }
    static void expect(String key, long n) { check(counts.getOrDefault(key, 0L) == n, key + " expected " + n); }
    static void report(String label) { System.out.println(label + ": " + counts); counts.clear(); }
    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p,m,a)-> {
            if (m.getName().equals("hashCode")) return System.identityHashCode(p);
            if (m.getName().equals("equals")) return p == a[0];
            Object value = call.run(m.getName(),a);
            if (value != null) return value;
            Class<?> t=m.getReturnType();
            if(t==boolean.class)return false;if(t==int.class)return 0;if(t==long.class)return 0L;
            if(t==float.class)return 0f;if(t==double.class)return 0d;return null;
        }));
    }
    static MemoryAPI memory(Map<String,Object> flags) {
        return mock(MemoryAPI.class,(n,a)-> {
            Object v=a==null||a.length==0?null:flags.get(a[0]);
            switch(n) {
                case "get": return v; case "getBoolean": return Boolean.TRUE.equals(v);
                case "getInt": return v instanceof Number?((Number)v).intValue():0;
                case "getLong": return v instanceof Number?((Number)v).longValue():0L;
                case "getFloat": return v instanceof Number?((Number)v).floatValue():0f;
                case "getString": return v instanceof String?v:null;
                case "contains": return flags.containsKey(a[0]);
                case "set": flags.put((String)a[0],a[1]);break;
                case "unset": flags.remove(a[0]);break;
            }return null;
        });
    }
    static final Map<String,Object> flags=new HashMap<>();
    static final MemoryAPI memory=memory(flags);
    static final Map<String,Float> cargoItems=new HashMap<>();
    static final List<StarSystemAPI> systems=new ArrayList<>();
    static LocationAPI location;
    static CampaignFleetAPI player;
    static StarSystemAPI system(String id, boolean[] owned, boolean[] active) {
        MemoryAPI ownMemory=mock(MemoryAPI.class,(n,a)->n.equals("getBoolean")?owned[0]:null);
        List<CampaignFleetAPI> fleets=new ArrayList<>();
        for(int i=0;i<20;i++) {
            String fleetId=id+"_unrelated_"+i;
            fleets.add(mock(CampaignFleetAPI.class,(n,a)-> {
                if(n.equals("getId")){count("fleet_ids");return fleetId;}
                if(n.equals("getMemoryWithoutUpdate"))return memory;return null;
            }));
        }
        return mock(StarSystemAPI.class,(n,a)-> {
            switch(n) {
                case "getOptionalUniqueId":count("system_ids");return id;
                case "getMemoryWithoutUpdate":return ownMemory;
                case "getFleets":count("fleet_lists");return fleets;
                case "getEntityById":count("entity_lookups");return null;
                case "isEnteredByPlayer":return true;
                case "activeThisFrame":return active[0];
                case "getPlanets":case "getJumpPoints":return Collections.emptyList();
                case "getTags":return Collections.emptySet();
            }return null;
        });
    }
    static SectorAPI sector() {
        CampaignClockAPI clock=mock(CampaignClockAPI.class,(n,a)->n.equals("getElapsedDaysSince")?1f:null);
        return mock(SectorAPI.class,(n,a)-> {
            switch(n) {
                case "getMemoryWithoutUpdate":return memory;case "getPlayerFleet":return player;
                case "getClock":return clock;
                case "getStarSystems":count("system_lists");return new ArrayList<>(systems);
                case "getAllLocations":count("all_locations");return new ArrayList<LocationAPI>(systems);
            }return null;
        });
    }
    static Object invoke(Class<?> type,String name)throws Exception {
        Method method=type.getDeclaredMethod(name);method.setAccessible(true);return method.invoke(null);
    }
    static void campaign()throws Exception {
        for(int i=0;i<500;i++)systems.add(system("ordinary_"+i,new boolean[]{false},new boolean[]{false}));
        boolean[] owned={true},active={false};
        for(String id:new String[]{OdysseyExpanseSystem.SYSTEM_ID,OdysseyExpanseSystem.MESSINA_ID,
                OdysseyExpanseSystem.SILENT_WAKE_ID,OdysseyExpanseSystem.ASHEN_VERGE_ID,
                OdysseyExpanseSystem.LAST_LIGHT_ID,OdysseyExpanseSystem.DEVOURED_REACH_ID})
            systems.add(system(id,owned,active));
        location=systems.get(0);
        CargoAPI cargo=mock(CargoAPI.class,(n,a)-> {
            if(n.equals("getQuantity")){count("cargo_queries");return cargoItems.getOrDefault(((SpecialItemData)a[1]).getId(),0f);}return null;
        });
        player=mock(CampaignFleetAPI.class,(n,a)-> {
            if(n.equals("getCargo"))return cargo;if(n.equals("getContainingLocation"))return location;
            if(n.equals("isPlayerFleet"))return true;return null;
        });
        Global.setSector(sector());
        String recovered="$chief_navigator_ithaca_research_components_recovered_v1";
        flags.put("$chief_navigator_ithaca_research_consumables_migrated_v2",true);
        flags.put("$chief_navigator_ithaca_research_component_mask_v1",0);
        flags.put(recovered,0);
        for(int i=0;i<600;i++) {
            check(IthacaResearchUpgrades.getCampaignMask()==0,"Read-only stat mask");
            check(IthacaResearchUpgrades.getInstalledCount()==0,"Read-only installed count");
        }
        expect("cargo_queries",0);report("1200 stat/progress reads with no components recovered");
        flags.put("$chief_navigator_ithaca_research_component_mask_v1",127);flags.put(recovered,127);
        for(int i=0;i<600;i++)IthacaResearchUpgrades.captureRecoveredComponents();
        expect("cargo_queries",0);report("600 complete research checks");
        flags.put("$chief_navigator_ithaca_research_component_mask_v1",0);flags.put(recovered,63);
        IthacaResearchUpgrades.captureRecoveredComponents();expect("cargo_queries",1);
        cargoItems.put(IthacaResearchUpgrades.COMPONENT_IDS[6],1f);
        IthacaResearchUpgrades.captureRecoveredComponents();expect("cargo_queries",2);
        check(((Number)flags.get(recovered)).intValue()==127,"Recovered history updates immediately");
        cargoItems.clear();IthacaResearchUpgrades.captureRecoveredComponents();expect("cargo_queries",2);
        check(IthacaResearchUpgrades.getCampaignMask()==0,"Recovered cargo does not install itself");
        report("Only missing component queried; acquisition/loss history retained");
        OdysseyExpanseSystem.invalidateSystemLookupCache();
        OdysseyPredatorScript script=new OdysseyPredatorScript();
        for(int i=0;i<600;i++)script.advance(0f);
        expect("system_lists",1);expect("cargo_queries",0);expect("fleet_lists",0);
        report("600 non-maintenance frames outside the region");
        for(String slot:new String[]{"lesser_0","lesser_1","lesser_2","greater_0","greater_1","transcendent_0"}) {
            flags.put("$chief_navigator_avici_manifestation_"+slot+"_spawned_v1",true);
            flags.put("$chief_navigator_avici_manifestation_"+slot+"_lost_at_v1",1L);
        }
        invoke(AviciDwellerPopulation.class,"maintainPopulation");
        expect("all_locations",0);expect("fleet_ids",0);expect("entity_lookups",6);
        report("Six absent Avici patrols within loss cooldown");
        Method physical=OdysseyPredatorScript.class.getDeclaredMethod("advancesPhysicalState",StarSystemAPI.class);physical.setAccessible(true);
        StarSystemAPI ashen=systems.get(503);
        check(!(Boolean)physical.invoke(null,ashen),"Inactive remote physical state skipped");
        active[0]=true;check((Boolean)physical.invoke(null,ashen),"Native remote advancement preserved");
        active[0]=false;location=ashen;check((Boolean)physical.invoke(null,ashen),"Player location always corrected");location=systems.get(0);
        check(OdysseyExpanseSystem.findSystemById(OdysseyExpanseSystem.ASHEN_VERGE_ID)==ashen,"Cached identity");
        owned[0]=false;check(OdysseyExpanseSystem.findSystemById(OdysseyExpanseSystem.ASHEN_VERGE_ID)==null,"Ownership revalidated immediately");owned[0]=true;
        StarSystemAPI replacement=system(OdysseyExpanseSystem.ASHEN_VERGE_ID,new boolean[]{true},new boolean[]{false});
        systems.set(503,replacement);Global.setSector(sector());
        check(OdysseyExpanseSystem.findSystemById(OdysseyExpanseSystem.ASHEN_VERGE_ID)==replacement,"New save never reuses old objects");
        String newId=OdysseyExpanseSystem.PALE_REACH_ID;
        check(OdysseyExpanseSystem.findSystemById(newId)==null,"Absent system is negative cached");
        StarSystemAPI added=system(newId,new boolean[]{true},new boolean[]{false});systems.add(added);
        systems.remove(replacement);
        for (int i=0;i<1200;i++) check(OdysseyExpanseSystem.findSystemById(newId)==null,"No timed world scan");
        OdysseyExpanseSystem.invalidateSystemLookupCache();
        check(OdysseyExpanseSystem.findSystemById(newId)==added,"World-change invalidation discovers added systems");
        check(OdysseyExpanseSystem.findSystemById(OdysseyExpanseSystem.ASHEN_VERGE_ID)==null,"World-change invalidation clears removed systems");
        systems.add(replacement);OdysseyExpanseSystem.invalidateSystemLookupCache();
        check(OdysseyExpanseSystem.findSystemById(OdysseyExpanseSystem.ASHEN_VERGE_ID)==replacement,"Load/generation invalidation is immediate");
        counts.clear();invoke(SinniSystemVignetteScript.class,"getAvailableExplorationCategories");expect("system_lists",1);
        report("One sightseeing scan uses one system-list snapshot");
    }
    static void beam()throws Exception {
        Global.getLogger(DriftingWallBeamEffect.class).setLevel(Level.INFO);
        ShipHullSpecAPI hull=mock(ShipHullSpecAPI.class,(n,a)->n.equals("getHullId")?IthacaResearchUpgrades.ITHACA_ONSLAUGHT_HULL_ID:null);
        ShipAPI ship=mock(ShipAPI.class,(n,a)->n.equals("getHullSpec")?hull:null);
        BeamWeaponSpecAPI spec=mock(BeamWeaponSpecAPI.class,(n,a)->n.equals("getBurstCooldown")?30f:null);
        float[] remaining={0f};
        WeaponAPI weapon=mock(WeaponAPI.class,(n,a)-> {
            switch(n) {
                case "getId":return IthacaResearchUpgrades.ITHACA_ONSLAUGHT_LASER_ID;
                case "getShip":return ship;case "getSpec":return spec;
                case "getBeams":return Collections.emptyList();case "getCooldown":return 40f;
                case "getCooldownRemaining":return remaining[0];
                case "setRemainingCooldownTo":remaining[0]=(Float)a[0];break;
                case "getLocation":return new Vector2f();
            }return null;
        });
        List<BeamAPI> beams=new ArrayList<>();
        BeamAPI own=mock(BeamAPI.class,(n,a)->n.equals("getWeapon")?weapon:n.equals("getBrightness")?1f:null);
        CombatEngineAPI engine=mock(CombatEngineAPI.class,(n,a)-> {
            if(n.equals("getBeams")){count("beam_list_reads");return beams;}
            if(n.equals("getShips")){count("diagnostic_ship_lists");return Collections.emptyList();}return null;
        });
        DriftingWallBeamEffect effect=new DriftingWallBeamEffect();
        Class<?> stateType=Class.forName("chiefnavigator.weapons.DriftingWallBeamEffect$State");
        Constructor<?> ctor=stateType.getDeclaredConstructor();ctor.setAccessible(true);Object state=ctor.newInstance();
        Field after=stateType.getDeclaredField("cooldownAfterBeam");after.setAccessible(true);after.setBoolean(state,true);
        Field states=DriftingWallBeamEffect.class.getDeclaredField("states");states.setAccessible(true);
        ((Map)states.get(effect)).put(weapon,state);
        counts.clear();for(int i=0;i<600;i++)effect.advance(1f/60f,engine,weapon);
        expect("beam_list_reads",600);expect("diagnostic_ship_lists",0);report("600 idle Domainslaught gun frames");
        beams.add(own);remaining[0]=20f;effect.advance(.016f,engine,weapon);
        check(remaining[0]==40f,"Lingering ray retains native cooldown hold");
        beams.clear();effect.advance(.016f,engine,weapon);
        check(remaining[0]==40f,"Tail end starts full cooldown bar");
        remaining[0]=39f;effect.advance(.016f,engine,weapon);
        check(Math.abs(remaining[0]-(40f-40f/30f))<.001f,"Cooldown progression preserved");
    }
    public static void main(String[] args)throws Exception {
        campaign();
        systems.clear(); flags.clear(); counts.clear();
        Global.setSector(sector());
        MenelausTrialScript idleLabors = new MenelausTrialScript();
        for (int i=0;i<1000;i++) idleLabors.advance(1f);
        expect("system_lists",1);
        report("1000 pre-acceptance Labor checks with no authored world: one load reconciliation");
        if (args.length == 0 || !args[0].equals("--campaign-only")) beam();
        Global.setSector(null);
        System.out.println("PASS: measured call reductions, save/topology/ownership invalidation, research history and remote physics. No FPS claim.");
    }
}
