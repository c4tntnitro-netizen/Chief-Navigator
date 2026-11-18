import chiefnavigator.ai.ExodusAssaultAI;
import chiefnavigator.hullmods.ThreatxusSwarmCloud;
import chiefnavigator.systems.*;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.threat.*;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.State;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;

/** Real native cloud/construction callbacks behind the fabrication-only system. */
public final class ThreatxusFabricationRegression extends ExodusPlagueRegression {
    static final class Ship extends Body {
        final String hull;
        final ShipAPI.HullSize size;
        final MutableStat zeroFlux=new MutableStat(50f),damageTaken=new MutableStat(1f);
        final StatBonus armorBonus=new StatBonus();
        final Map<String,MutableStat> phaseStats=new HashMap<>();
        final List<ShipAPI> modules=new ArrayList<>();
        final List<Object> listeners=new ArrayList<>();
        final List<WeaponAPI> weapons=new ArrayList<>();
        boolean holdOneFrame;
        ShipAPI parent;
        final List<ShipCommand> blocked=new ArrayList<>();
        final Set<String> tags=new HashSet<>();
        float alpha=1f;
        CollisionClass collision;
        ShipSystemAPI.SystemState systemState=ShipSystemAPI.SystemState.IDLE;
        Ship(String hull,ShipAPI.HullSize size,int side){
            this.hull=hull;this.size=size;
            ship(side,size==ShipAPI.HullSize.FIGHTER);
            collision=fighter?CollisionClass.FIGHTER:CollisionClass.SHIP;
            ShipHullSpecAPI spec=mock(ShipHullSpecAPI.class,(n,a)->{
                if(n.equals("getHullId")||n.equals("getBaseHullId"))return hull;
                if(n.equals("getHullSize"))return size;
                if(n.equals("getHints"))return EnumSet.noneOf(ShipHullSpecAPI.ShipTypeHints.class);
                if(n.equals("getAllWeaponSlotsCopy"))return Collections.emptyList();
                return null;
            });
            ShipVariantAPI variant=mock(ShipVariantAPI.class,(n,a)->n.equals("getHullSpec")?spec:null);
            stats=mock(MutableShipStatsAPI.class,(n,a)->{
                if(n.equals("getEntity"))return ship;
                if(n.equals("getVariant"))return variant;
                if(n.equals("getZeroFluxSpeedBoost"))return zeroFlux;
                if(n.equals("getHullDamageTakenMult"))return damageTaken;
                if(n.equals("getArmorDamageTakenMult")||n.equals("getShieldDamageTakenMult")||n.equals("getEmpDamageTakenMult"))
                    return phaseStats.computeIfAbsent(n,k->new MutableStat(1f));
                if(n.equals("getMaxSpeed"))return phaseStats.computeIfAbsent(n,k->new MutableStat(10f));
                if(n.equals("getAcceleration")||n.equals("getDeceleration"))return phaseStats.computeIfAbsent(n,k->new MutableStat(2f));
                if(n.equals("getMaxTurnRate"))return phaseStats.computeIfAbsent(n,k->new MutableStat(6f));
                if(n.equals("getTurnAcceleration"))return phaseStats.computeIfAbsent(n,k->new MutableStat(10f));
                if(n.equals("getEffectiveArmorBonus"))return armorBonus;
                if(n.equals("getTimeMult"))return new MutableStat(1f);
                return null;
            });
            data.put("spec",spec);
            if(fighter)wing=mock(FighterWingAPI.class,(n,a)->n.equals("getWingId")?SwarmLauncherEffect.CONSTRUCTION_SWARM_WING:null);
        }
        @Override Object call(String n,Object[] a){
            switch(n){
                case "getHullSpec":return data.get("spec");
                case "getChildModulesCopy":return new ArrayList<>(modules);
                case "getParentStation":return parent;
                case "getAllWeapons":return weapons;
                case "setHoldFireOneFrame":holdOneFrame=(Boolean)a[0];return null;
                case "addListener":listeners.add(a[0]);return null;
                case "hasListenerOfClass":return listeners.stream().anyMatch(x->((Class<?>)a[0]).isInstance(x));
                case "getListeners":return listeners.stream().filter(x->((Class<?>)a[0]).isInstance(x)).toList();
                case "hasTag":return tags.contains(a[0]);
                case "addTag":tags.add((String)a[0]);return null;
                case "removeTag":tags.remove(a[0]);return null;
                case "getHullSize":return size;
                case "getAlphaMult":return alpha;
                case "setAlphaMult":alpha=(Float)a[0];return null;
                case "getCollisionRadius":return hull.equals("fabricator_unit")?310f:size==ShipAPI.HullSize.CAPITAL_SHIP?287f:fighter?20f:130f;
                case "getMaxSpeedWithoutBoost":return size==ShipAPI.HullSize.CAPITAL_SHIP?10f:90f;
                case "getCollisionClass":return collision;
                case "setCollisionClass":collision=(CollisionClass)a[0];return null;
                case "getWeaponGroupsCopy":return Collections.emptyList();
                case "setShipAI":case "setDefaultAI":pilot=(ShipAIPlugin)a[0];return null;
                case "blockCommandForOneFrame":blocked.add((ShipCommand)a[0]);return null;
                case "getEngineController":return mock(ShipEngineControllerAPI.class,(x,y)->null);
                case "getSystem":return mock(ShipSystemAPI.class,(x,y)->{
                    if(x.equals("getState"))return systemState;
                    if(x.equals("canBeActivated"))return systemState==ShipSystemAPI.SystemState.IDLE;
                    return null;
                });
                case "useSystem":systemUses++;systemState=ShipSystemAPI.SystemState.IN;return null;
            }
            return super.call(n,a);
        }
    }
    static final class FactoryFixture extends Fixture {
        final Map<ShipAPI,Ship> bodies=new IdentityHashMap<>();
        final List<Ship> carriers=new ArrayList<>(),units=new ArrayList<>(),fabricators=new ArrayList<>();
        final List<String> order=new ArrayList<>();
        final Ship factory;
        FactoryFixture(){this(0);}
        FactoryFixture(int side){
            factory=add(new Ship(ThreatxusSwarmCloud.HULL_ID,ShipAPI.HullSize.CAPITAL_SHIP,side));
            factory.loc.set(1000,-400);factory.vel.set(10,0);
            manager=mock(CombatFleetManagerAPI.class,(n,a)->{
                if(n.equals("isSuppressDeploymentMessages"))return suppress;
                if(n.equals("setSuppressDeploymentMessages")){suppress=(Boolean)a[0];return null;}
                if(n.equals("spawnShipOrWing")){
                    check(suppress,"Construction deployment stays quiet");
                    boolean carrier=a[0].equals(SwarmLauncherEffect.CONSTRUCTION_SWARM_WING);
                    boolean fab=a[0].equals(ThreatxusFabricationSystem.FABRICATOR_VARIANT);
                    check(carrier||fab||a[0].equals(ThreatxusFabricationSystem.ASSAULT_VARIANT),"Only authored assault/stock Fabricator builds");
                    Ship body=add(new Ship(carrier?"construction_swarm":fab?"fabricator_unit":ThreatxusFabricationSystem.ASSAULT_HULL_ID,
                            carrier?ShipAPI.HullSize.FIGHTER:fab?ShipAPI.HullSize.CAPITAL_SHIP:ShipAPI.HullSize.DESTROYER,side));
                    body.loc.set((Vector2f)a[1]);body.facing=(Float)a[2];
                    if(carrier){body.loc.x+=600;body.loc.y-=350;body.vel.set(100,50);carriers.add(body);}
                    else if(fab){
                        // Stock Fragment Swarm plus Type450's60% coordinator.
                        RoilingSwarmEffect nativeCloud=FragmentSwarmHullmod.createSwarmFor(body.ship);
                        nativeCloud.getParams().baseMembersToMaintain=160;
                        nativeCloud.getParams().initialMembers=160;
                        nativeCloud.addMembers(110); // emulate pre-advanced native fill
                        fabricators.add(body);order.add((String)a[0]);
                    } else {
                        // Native full-size ships also receive ten0.2s warmup ticks.
                        ThreatxusSwarmCloud mod=new ThreatxusSwarmCloud();
                        mod.applyEffectsAfterShipCreation(body.ship,"warmup");
                        for(int frame=0;frame<10;frame++){
                            mod.advanceInCombat(body.ship,.2f);
                            if(body.pilot!=null)body.pilot.advance(.2f);
                        }
                        check(body.systemUses==0&&visual(body).getNumActiveMembers()==0,"No attacks/cloud fill before fabrication begins");
                        units.add(body);order.add((String)a[0]);
                    }
                    return body.ship;
                }
                return null;
            });
        }
        Ship add(Ship body){bodies.put(body.ship,body);entities.add(body.ship);return body;}
    }
    static RoilingSwarmEffect visual(Ship body){return (RoilingSwarmEffect)body.data.get(ThreatxusSwarmCloud.CLOUD_KEY);}

    static void cloudAndDrift(){
        FactoryFixture f=new FactoryFixture();ThreatxusSwarmCloud hullmod=new ThreatxusSwarmCloud();
        hullmod.applyEffectsBeforeShipCreation(ShipAPI.HullSize.CAPITAL_SHIP,f.factory.stats,"test");
        near(f.factory.zeroFlux.getModifiedValue(),0,"No zero-flux speed burst");
        f.paused=true;hullmod.advanceInCombat(f.factory.ship,.1f);check(visual(f.factory)==null,"Pause creates no cloud");f.paused=false;
        Vector2f loc=new Vector2f(f.factory.loc),vel=new Vector2f(f.factory.vel);
        hullmod.advanceInCombat(f.factory.ship,.1f);
        RoilingSwarmEffect cloud=visual(f.factory);
        check(cloud!=null&&cloud.getParams().initialMembers==1500&&cloud.getParams().baseMembersToMaintain==1500,
                "Keep1000 broad fragments and add500 core fragments");
        check(cloud.getParams().memberRespawnRate==75,"Maintenance scales with the added core");
        near(cloud.getParams().maxOffset,287f*3.5f,"Broader parent cloud extent");
        check(!cloud.getParams().generateOffsetAroundAttachedEntityOval&&cloud.getParams().offsetModifier!=null,
                "Parent replaces the native hull band with a diffuse distribution");
        check(cloud.getParams().memberExchangeClass==null&&RoilingSwarmEffect.getSwarmFor(f.factory.ship)==null,
                "Parent visual supplies no ammunition or foreign swarm exchanges");
        com.fs.starfarer.api.util.Misc.random.setSeed(20261116L);
        cloud.advance(.1f);check(cloud.getNumActiveMembers()==1500,"Actual native cloud fills all1500 sprites");
        diffuseProfile(cloud);
        check(f.factory.commands.contains(ShipCommand.ACCELERATE)&&f.factory.blocked.containsAll(List.of(
                ShipCommand.ACCELERATE_BACKWARDS,ShipCommand.DECELERATE,ShipCommand.STRAFE_LEFT,ShipCommand.STRAFE_RIGHT)),
                "Forward-only drift retains native physics and turning");
        check(f.factory.loc.equals(loc)&&f.factory.vel.equals(vel),"Hullmod never teleports or overwrites momentum");
        int layers=f.layers.size();hullmod.advanceInCombat(f.factory.ship,.1f);
        check(visual(f.factory)==cloud&&f.layers.size()==layers,"Repeated callbacks never multiply the cloud");
        Ship other=f.add(new Ship("ordinary_ship",ShipAPI.HullSize.CAPITAL_SHIP,0));
        hullmod.advanceInCombat(other.ship,.1f);check(visual(other)==null&&other.commands.isEmpty(),"Ordinary ships unaffected");
        RoilingSwarmEffect foreign=new RoilingSwarmEffect(f.factory.ship,new RoilingSwarmEffect.RoilingSwarmParams());
        f.factory.alive=false;hullmod.advanceInCombat(f.factory.ship,.1f);check(cloud.isForceDespawn(),"Death disperses private cloud");
        cloud.getMembers().clear();check(cloud.isExpired()&&RoilingSwarmEffect.getSwarmFor(f.factory.ship)==foreign,
                "Private expiry preserves another registered native cloud");
    }
    static void diffuseProfile(RoilingSwarmEffect cloud){
        int[] bins=new int[4];int distant=0,center=0;
        float extent=cloud.getParams().maxOffset;
        for(var member:cloud.getMembers()){
            double x=member.offset.x/extent,y=member.offset.y/(extent*.9f);
            double r=Math.sqrt(x*x+y*y);
            check(Double.isFinite(r)&&r<=1.00001,"All native offsets remain inside the stable spring envelope");
            bins[Math.min(3,(int)(r*4))]++;
            if(r>.6)distant++;
            if(r<.1)center++;
        }
        // Compare counts per annular area, rather than raw counts (outer
        // annuli have more area). Wide tolerances avoid testing random noise.
        for(int i=1;i<4;i++)check(bins[i-1]/(double)(2*i-1)>bins[i]/(double)(2*i+1),
                "Actual fragment density decreases through every outward annulus");
        check(distant>180&&distant<360,"A substantial diffuse skirt extends beyond600 units");
        check(bins[3]>35&&bins[3]<145,"Sparse outskirts replace the sharply populated rim");
        check(center>60&&center<120,"Added core approximately triples central occupancy without thickening outskirts");
        System.out.println("Diffuse native cloud annular counts="+Arrays.toString(bins)+", beyond0.6 extent="+distant+", center="+center);
    }
    static void fabricationAndOnlyAttack(){
        FactoryFixture f=new FactoryFixture();ThreatxusSwarmCloud hullmod=new ThreatxusSwarmCloud();
        ThreatxusFabricationSystem system=new ThreatxusFabricationSystem();
        hullmod.advanceInCombat(f.factory.ship,.1f);
        system.apply(f.factory.stats,"test",State.IDLE,0);
        check(f.factory.systemUses==1,"Ready factory automatically starts its system for periodic production");
        system.apply(f.factory.stats,"test",State.IN,.5f);check(f.units.isEmpty(),"Windup does not create units");
        system.apply(f.factory.stats,"test",State.ACTIVE,1);
        system.apply(f.factory.stats,"test",State.ACTIVE,1);
        check(f.units.size()==1&&f.carriers.size()==1,"Exactly one unit and construction carrier per cycle");
        Ship unit=f.units.get(0),carrier=f.carriers.get(0);
        check(!f.suppress,"Deployment suppression restored");
        check(unit.tags.contains(ThreatShipConstructionScript.SHIP_UNDER_CONSTRUCTION)&&unit.alpha==0,"Native construction starts hidden/tagged");
        check(Vector2f.sub(carrier.loc,f.factory.loc,null).length()>480,"Native prewarm corrected to an external build site");
        check(f.factory.collision==CollisionClass.SHIP&&f.factory.damageTaken.getModifiedValue()==1,
                "Factory is never made invulnerable or noncolliding by native construction");
        check(carrier.collision==CollisionClass.NONE&&carrier.damageTaken.getModifiedValue()==0,"Only disposable construction carrier is protected");
        hullmod.advanceInCombat(unit.ship,.1f);RoilingSwarmEffect swarm=visual(unit);
        check(swarm.getParams().baseMembersToMaintain==400&&!swarm.getParams().withInitialMembers&&!swarm.getParams().withRespawn,
                "Dense native destroyer cloud waits for construction handoff");
        check(swarm.getParams().generateOffsetAroundAttachedEntityOval&&swarm.getParams().maxOffset==65
                &&swarm.getParams().offsetModifier==null,"Assault-unit cloud keeps its existing native spread");
        swarm.advance(.1f);check(swarm.getNumActiveMembers()==0&&unit.pilot==null,"No premature autonomous attack or free fill");
        f.tick(19.9f);check(unit.tags.contains(ThreatShipConstructionScript.SHIP_UNDER_CONSTRUCTION),"Full twenty-second fabrication time");
        f.tick(.2f);check(!unit.tags.contains(ThreatShipConstructionScript.SHIP_UNDER_CONSTRUCTION)&&unit.alpha==1,"Native assembly completes");
        hullmod.advanceInCombat(unit.ship,.1f);swarm.advance(.1f);
        check(swarm.getNumActiveMembers()==400&&unit.pilot instanceof ExodusAssaultAI,"Four hundred native fragments and system-only AI after assembly");
        check(f.factory.damageTaken.getModifiedValue()==1,"Native completion never modifies factory defenses");
        Ship target=f.add(new Ship("enemy",ShipAPI.HullSize.CRUISER,1));target.loc.set(unit.loc.x+3000,unit.loc.y);
        unit.pilot.advance(.5f);check(unit.systemUses==1&&unit.commands.contains(ShipCommand.ACCELERATE),"Unarmed unit approaches and activates Exodus Plague");
        ExodusPlagueSystem plague=new ExodusPlagueSystem();
        for(int frame=0;frame<300;frame++)plague.apply(unit.stats,"plague",State.ACTIVE,1);
        check(f.launched.size()==400,"Fabricated unit's sole attack emits the full tapered Exodus burst");
        Vector2f left=ExodusPlagueSystem.emitterOrigin(unit.ship,0),right=ExodusPlagueSystem.emitterOrigin(unit.ship,1);
        near(Vector2f.sub(left,right,null).length(),42,"Assault emitters use native forward mount spacing");
        for(int i=0;i<3;i++)cycle(f,system);
        check(f.units.size()==1&&f.fabricators.size()==3&&ThreatxusFabricationSystem.activeFabricators(f.factory.ship,f.engine)==3,
                "Fabricators fill after first assault; their cap includes building units");
        Ship fab=f.fabricators.get(0);
        check(fab.tags.contains(ThreatShipConstructionScript.SHIP_UNDER_CONSTRUCTION)&&fab.alpha==0,"Stock Fabricator uses native construction");
        check(RoilingSwarmEffect.getSwarmFor(fab.ship).getNumActiveMembers()==0,"Prewarmed Fabricator cloud is cleared before assembly");
        check(Vector2f.sub(fab.loc,f.factory.loc,null).length()>670,"Fabricator build site clears both native collision radii");
        f.tick(20.1f);
        check(!fab.tags.contains(ThreatShipConstructionScript.SHIP_UNDER_CONSTRUCTION)&&fab.alpha==1
                &&RoilingSwarmEffect.getSwarmFor(fab.ship).getNumActiveMembers()==160,"Stock Fabricator assembly transfers160 fragments");
        hullmod.advanceInCombat(fab.ship,.1f);
        check(visual(fab)==null&&!(fab.pilot instanceof ExodusAssaultAI),"Stock Fabricators keep their native cloud and AI");
        for(int i=0;i<2;i++)cycle(f,system);
        check(f.units.size()==3&&f.fabricators.size()==3&&!system.isUsable(null,f.factory.ship),"Independent three-assault and three-Fabricator caps");
        cycle(f,system);check(f.units.size()==3&&f.fabricators.size()==3,"No fourth Fabricator or assault, queue or cap bypass");
        unit.alive=false;cycle(f,system);
        check(f.units.size()==4&&ThreatxusFabricationSystem.activeUnits(f.factory.ship,f.engine)==3,"Destroyed assault immediately frees one slot");
        fab.alive=false;cycle(f,system);
        check(f.fabricators.size()==4&&ThreatxusFabricationSystem.activeFabricators(f.factory.ship,f.engine)==3,"Destroyed Fabricator frees its own slot");
        int count=f.units.size();f.paused=true;system.apply(f.factory.stats,"test",State.ACTIVE,1);check(f.units.size()==count,"Paused factory creates nothing");
        f.paused=false;f.factory.alive=false;system.apply(f.factory.stats,"test",State.COOLDOWN,0);system.apply(f.factory.stats,"test",State.ACTIVE,1);
        check(f.units.size()==count,"Destroyed factory stops production");
    }
    static void cycle(FactoryFixture f,ThreatxusFabricationSystem system){
        system.apply(f.factory.stats,"test",State.COOLDOWN,0);
        system.apply(f.factory.stats,"test",State.ACTIVE,1);
    }
    static void priorityAndIsolation(){
        FactoryFixture f=new FactoryFixture(1);ThreatxusFabricationSystem system=new ThreatxusFabricationSystem();
        cycle(f,system);check(f.units.size()==1&&f.fabricators.isEmpty(),"Empty fleet prioritizes assault");
        cycle(f,system);check(f.fabricators.size()==1,"An assembling assault already satisfies priority");
        f.units.get(0).alive=false;cycle(f,system);
        check(f.units.size()==2&&f.fabricators.size()==1,"No remaining assault preempts the next Fabricator even below its cap");
        check(f.units.get(1).owner==1&&f.fabricators.get(0).owner==1,"Both build types inherit enemy ownership");
        f.entities.remove(f.fabricators.get(0).ship);cycle(f,system);
        check(ThreatxusFabricationSystem.activeFabricators(f.factory.ship,f.engine)==1,"Removed Fabricator frees its slot");
        Ship second=f.add(new Ship(ThreatxusSwarmCloud.HULL_ID,ShipAPI.HullSize.CAPITAL_SHIP,1));
        check(ThreatxusFabricationSystem.activeUnits(second.ship,f.engine)==0
                &&ThreatxusFabricationSystem.activeFabricators(second.ship,f.engine)==0,"Each Threatxus tracks only its own children");
        Ship foreign=f.add(new Ship("fabricator_unit",ShipAPI.HullSize.CAPITAL_SHIP,1));
        check(ThreatxusFabricationSystem.activeFabricators(f.factory.ship,f.engine)==1,"Unrelated friendly Fabricators do not consume slots");
        f.fabricators.get(1).owner=0;
        check(ThreatxusFabricationSystem.activeFabricators(f.factory.ship,f.engine)==0,"Captured Fabricators no longer consume an owned slot");
    }
    public static void main(String[] args){
        cloudAndDrift();fabricationAndOnlyAttack();priorityAndIsolation();
        FactoryFixture prewarm=new FactoryFixture();
        Ship pending=prewarm.add(new Ship(ThreatxusFabricationSystem.ASSAULT_HULL_ID,ShipAPI.HullSize.DESTROYER,0));
        Ship nearby=prewarm.add(new Ship("nearby_enemy",ShipAPI.HullSize.CRUISER,1));nearby.loc.set(1000,0);
        new ThreatxusSwarmCloud().applyEffectsAfterShipCreation(pending.ship,"spawn");
        new ExodusAssaultAI(pending.ship,prewarm.engine).advance(1f);
        new ExodusPlagueSystem().apply(pending.stats,"spawn",State.ACTIVE,1f);
        check(pending.alpha==0&&pending.systemUses==0&&prewarm.launched.isEmpty(),"Creation gate prevents premature Exodus attacks even with an injected AI");
        FactoryFixture enemy=new FactoryFixture(1);
        new ThreatxusFabricationSystem().apply(enemy.factory.stats,"enemy",State.ACTIVE,1);
        check(enemy.units.size()==1&&enemy.units.get(0).owner==1&&enemy.carriers.get(0).owner==1,
                "Enemy mission factory creates enemy-owned construction and assault units");
        System.out.println("PASS: actual1500/400-sprite native clouds/denser core, private registry, drift, native20s assault/Fabricator construction, parent vulnerability, assault priority, independent three-live caps, ownership/isolation/cap reuse and unchanged Exodus400-packet attack.");
    }
}
