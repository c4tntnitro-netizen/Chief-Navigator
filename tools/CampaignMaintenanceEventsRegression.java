package chiefnavigator.quest;

import chiefnavigator.weapons.IthacaResearchUpgrades;
import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import java.lang.reflect.*;
import java.util.*;

/** Count actual maintenance entry points and exercise native-event invalidation. */
public final class CampaignMaintenanceEventsRegression {
    interface Call { Object call(String name,Object[] args); }
    static <T> T mock(Class<T> type,Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->{
            if(m.getName().equals("hashCode"))return System.identityHashCode(p);
            if(m.getName().equals("equals"))return p==a[0];
            Object v=call.call(m.getName(),a);if(v!=null)return v;
            Class<?> r=m.getReturnType();if(r==boolean.class)return false;if(r==int.class)return 0;
            if(r==long.class)return 0L;if(r==float.class)return 0f;if(r==double.class)return 0d;return null;
        }));
    }
    static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);}
    static class Fixture {
        double days;
        int queries,worldLists,cargoReads,members=2;
        boolean dialogOpen;
        final Map<String,Object> flags=new HashMap<>();
        final Map<String,Double> expires=new HashMap<>();
        final Map<String,SectorEntityToken> entities=new HashMap<>();
        final List<CampaignEventListener> base=new ArrayList<>();
        final List<Object> typed=new ArrayList<>();
        final List<EveryFrameScript> scripts=new ArrayList<>();
        final List<StarSystemAPI> systems=new ArrayList<>();
        PersonAPI commander=mock(PersonAPI.class,(n,a)->null);
        LocationAPI fleetLocation;
        CampaignFleetAPI player;
        MemoryAPI memory=mock(MemoryAPI.class,(n,a)->{
            String key=a!=null&&a.length>0&&a[0] instanceof String?(String)a[0]:null;
            if(key!=null&&expires.containsKey(key)&&days>=expires.get(key)){flags.remove(key);expires.remove(key);}
            Object v=flags.get(key);
            switch(n){
                case "get":return v;case "getBoolean":return Boolean.TRUE.equals(v);
                case "getInt":return v instanceof Number?((Number)v).intValue():0;
                case "getLong":return v instanceof Number?((Number)v).longValue():0L;
                case "contains":return flags.containsKey(key);
                case "getExpire":return expires.containsKey(key)?(float)(expires.get(key)-days):0f;
                case "set":flags.put(key,a[1]);if(a.length>2)expires.put(key,days+((Number)a[2]).doubleValue());break;
                case "unset":flags.remove(key);expires.remove(key);break;
            }return null;
        });
        StarSystemAPI system=mock(StarSystemAPI.class,(n,a)->{
            switch(n){
                case "getId":case "getOptionalUniqueId":return OdysseyExpanseSystem.ASHEN_VERGE_ID;
                case "getMemoryWithoutUpdate":return memory;
                case "getEntityById":queries++;return entities.get(a[0]);
                case "getFleets":return Collections.emptyList();
            }return null;
        });
        CampaignFleetAPI fleet=mock(CampaignFleetAPI.class,(n,a)->{
            switch(n){
                case "getId":return "chief_navigator_test_0";
                case "getContainingLocation":return fleetLocation;
                case "getMemoryWithoutUpdate":return memory;
                case "getCommander":return commander;
                case "getFleetData":return mock(FleetDataAPI.class,(name,args)->{
                    if(name.equals("getNumMembers"))return members;
                    if(name.equals("getMembersListCopy"))throw new AssertionError("A stamp must not scan the roster");
                    return null;
                });
                case "getFaction":return mock(FactionAPI.class,(name,args)->name.equals("getId")?"test":null);
            }return null;
        });
        SectorAPI sector;
        Fixture(){
            systems.add(system);fleetLocation=system;
            CampaignClockAPI clock=mock(CampaignClockAPI.class,(n,a)->{
                if(n.equals("getTimestamp"))return (long)(days*1000);
                if(n.equals("getElapsedDaysSince"))return (float)(days-((Number)a[0]).longValue()/1000d);
                return null;
            });
            ListenerManagerAPI lm=mock(ListenerManagerAPI.class,(n,a)->{
                if(n.equals("addListener"))typed.add(a[0]);
                if(n.equals("removeListenerOfClass"))typed.removeIf(o->o.getClass()==a[0]);
                if(n.equals("getListeners")){List<Object> out=new ArrayList<>();for(Object o:typed)if(((Class<?>)a[0]).isInstance(o))out.add(o);return out;}
                return null;
            });
            sector=mock(SectorAPI.class,(n,a)->{
                switch(n){
                    case "getMemoryWithoutUpdate":return memory;case "getClock":return clock;
                    case "getPlayerFleet":return player;
                    case "getCampaignUI":return mock(CampaignUIAPI.class,(name,args)->name.equals("isShowingDialog")?dialogOpen:null);
                    case "getStarSystems":worldLists++;return systems;
                    case "getAllListeners":return base;
                    case "getListenerManager":return lm;
                    case "addTransientListener":base.add((CampaignEventListener)a[0]);break;
                    case "removeListener":base.remove(a[0]);break;
                    case "addTransientScript":scripts.add((EveryFrameScript)a[0]);break;
                    case "removeTransientScriptsOfClass":scripts.removeIf(o->o.getClass()==a[0]);break;
                }return null;
            });
            Global.setSector(sector);
        }
        CampaignMaintenanceEvents events(){return (CampaignMaintenanceEvents)base.get(0);}
    }
    static void slotsAndEvents() throws Exception {
        Fixture f=new Fixture();CampaignMaintenanceEvents.install();CampaignMaintenanceEvents.install();
        check(f.base.size()==1&&f.typed.size()==1&&f.base.get(0)==f.typed.get(0),"One instance in both native registries after reload");
        f.dialogOpen=true;
        f.events().reportShownInteractionDialog(mock(InteractionDialogAPI.class,(n,a)->n.equals("getInteractionTarget")?f.fleet:null));
        check(!CampaignMaintenanceEvents.consumeClosedInteraction(),"Busy dialog defers population mutation");
        f.dialogOpen=false;
        check(CampaignMaintenanceEvents.consumeClosedInteraction()&&!CampaignMaintenanceEvents.consumeClosedInteraction(),"Dialog-close reconciliation runs once even when population script was paused during the entire dialog");
        String id=f.fleet.getId();
        f.flags.put("$"+id+"_was_present",true);
        f.flags.put("$"+id+"_replacement_pending",true);
        f.memory.set("$"+id+"_replacement_cooldown",true,3f);
        Method regional=OdysseyPredatorScript.class.getDeclaredMethod("ensureRegionalStarvingPatrols",StarSystemAPI.class,String.class,int.class,float[][].class,String.class);
        regional.setAccessible(true);
        regional.invoke(null,f.system,"chief_navigator_test_",1,new float[][]{{0,0}},"test");
        check(f.queries==1,"Initial saved cooldown reconciliation queries its slot once");
        for(int i=0;i<1200;i++)regional.invoke(null,f.system,"chief_navigator_test_",1,new float[][]{{0,0}},"test");
        check(f.queries==1,"1200 maintenance ticks must not search a waiting slot");
        f.entities.put(id,f.fleet); // Foreign claimant appears without our event; deadline must revalidate.
        f.days=3.1;regional.invoke(null,f.system,"chief_navigator_test_",1,new float[][]{{0,0}},"test");
        check(f.queries==2&&f.entities.get(id)==f.fleet,"Deadline revalidates and preserves a foreign claimant");
        for(int i=0;i<600;i++)regional.invoke(null,f.system,"chief_navigator_test_",1,new float[][]{{0,0}},"test");
        check(f.queries==2,"Live claimant is retained without repeated slot searches");
        f.events().reportFleetDespawned(f.fleet,CampaignEventListener.FleetDespawnReason.OTHER,null);
        f.entities.remove(id);f.fleetLocation=null;
        PopulationSlotSchedule.Slot slot=PopulationSlotSchedule.get(f.system,id);
        check(slot.ready()&&slot.claim()==null&&f.queries==3,"Despawn invalidates positive cache");
        check(!slot.ready(),"Unscheduled empty slot stays asleep");
        f.entities.put(id,f.fleet);f.fleetLocation=f.system;
        f.events().reportFleetSpawned(f.fleet);
        check(slot.ready()&&slot.claim()==f.fleet,"Spawn invalidates negative cache");
        Fixture next=new Fixture();check(PopulationSlotSchedule.get(next.system,id).ready(),"Second save initializes its own index");
        check(next.queries==1,"Previous save references never satisfy new save slots");
        System.out.println("PASS: native event registration, 1800 idle slot calls, saved deadline, foreign claim, spawn/despawn and second save");
    }
    static void identityAndQuest() {
        Fixture f=new Fixture();CampaignMaintenanceEvents.install();
        check(FleetIdentityMaintenance.needsRepair(f.fleet),"Initial repair");
        FleetIdentityMaintenance.repaired(f.fleet);FleetIdentityMaintenance.upgraded(f.fleet,1);
        for(int i=0;i<1200;i++)check(!FleetIdentityMaintenance.needsRepair(f.fleet)&&!FleetIdentityMaintenance.needsUpgrade(f.fleet,1),"Unchanged fleet skips deep repair");
        f.members++;check(FleetIdentityMaintenance.needsRepair(f.fleet)&&FleetIdentityMaintenance.needsUpgrade(f.fleet,1),"Roster change invalidates cheap stamp");
        FleetIdentityMaintenance.repaired(f.fleet);FleetIdentityMaintenance.upgraded(f.fleet,1);
        f.events().reportFleetInflated(f.fleet,null);
        check(FleetIdentityMaintenance.needsRepair(f.fleet)&&FleetIdentityMaintenance.needsUpgrade(f.fleet,1),"Same-count variant inflation invalidates repair");
        MenelausTrialScript watcher=new MenelausTrialScript();
        watcher.advance(1f);int scans=f.worldLists;
        for(int i=0;i<600;i++)watcher.advance(1f);
        check(f.worldLists==scans,"Idle objective watcher makes no repeated world searches");
        for(int i=0;i<30;i++)MenelausTrialScript.requestReconciliation();
        watcher.advance(1f);
        check(f.worldLists==scans+1,"Thirty queued objective changes coalesce into one reconciliation");
        for(int i=0;i<600;i++)watcher.advance(1f);
        check(f.worldLists==scans+1,"Reconciled objective watcher stays idle");
        System.out.println("PASS: 1200 cheap identity checks, roster/inflation invalidation and coalesced objective events");
    }
    static void researchTransfers() {
        Fixture f=new Fixture();CampaignMaintenanceEvents.install();
        f.flags.put("$chief_navigator_ithaca_research_consumables_migrated_v2",true);
        f.flags.put("$chief_navigator_ithaca_research_component_mask_v1",0);
        f.flags.put("$chief_navigator_ithaca_research_components_recovered_v1",0);
        boolean[] acquired={false};
        CargoAPI cargo=mock(CargoAPI.class,(n,a)->{
            if(n.equals("getQuantity")){f.cargoReads++;return acquired[0]&&((SpecialItemData)a[1]).getId().equals(IthacaResearchUpgrades.COMPONENT_IDS[0])?1f:0f;}return null;
        });
        f.player=mock(CampaignFleetAPI.class,(n,a)->n.equals("getCargo")?cargo:null);
        for(int i=0;i<1200;i++)IthacaResearchUpgrades.getCampaignMask();
        check(f.cargoReads==0,"Stat reads do not scan cargo");
        acquired[0]=true;
        new ResearchStationSalvageListener().reportSpecialCargoGainedFromRecoveredDerelict(cargo,null);
        check(((Number)f.flags.get("$chief_navigator_ithaca_research_components_recovered_v1")).intValue()==1,"Committed cargo event records recovered component");
        check(IthacaResearchUpgrades.getCampaignMask()==0,"Recovery does not auto-install components");
        System.out.println("PASS: cargo event records acquisition while 1200 stat reads remain read-only");
    }
    public static void main(String[] args)throws Exception{
        slotsAndEvents();identityAndQuest();researchTransfers();
    }
}
