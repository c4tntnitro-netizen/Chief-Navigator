import chiefnavigator.systems.ExodusPlaguePacketEffect;
import chiefnavigator.systems.ExodusPlagueSystem;
import chiefnavigator.systems.ExodusPlagueAI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.State;
import com.fs.starfarer.api.*;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.threat.*;
import java.awt.Color;
import java.lang.reflect.*;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;

/** Exercises real native fragment clouds and production launch/contact/lifetime callbacks. */
public class ExodusPlagueRegression {
    interface Call { Object invoke(String name, Object[] args); }
    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p,m,a)->{
            if (m.getName().equals("hashCode")) return System.identityHashCode(p);
            if (m.getName().equals("equals")) return p == a[0];
            Object value=call.invoke(m.getName(),a);
            if(value!=null)return value;
            if(m.getReturnType()==boolean.class)return false;
            if(m.getReturnType()==float.class)return 0f;
            if(m.getReturnType()==int.class)return 0;
            if(m.getReturnType()==double.class)return 0d;
            if(m.getReturnType()==long.class)return 0L;
            return null;
        }));
    }
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static void near(float actual,float expected,String message){check(Math.abs(actual-expected)<.01f,message+": "+actual+" != "+expected);}

    static class Body {
        Vector2f loc=new Vector2f(),vel=new Vector2f();
        Map<String,Object> data=new HashMap<>();
        float facing,hp=60,maxHp=60;
        float maxRange;
        int owner;
        boolean alive=true,fighter,expired,fizzling,phased,constructing,overloaded;
        int systemUses;
        Vector2f mouse;
        ShipAPI source,target,ship;
        MissileAPI missile;
        FighterWingAPI wing;
        ShipAIPlugin pilot;
        int aiResets;
        float angularVelocity;
        MissileAIPlugin ai;
        Vector2f start;
        List<ShipCommand> commands=new ArrayList<>();
        MutableShipStatsAPI stats=mock(MutableShipStatsAPI.class,(n,a)->n.equals("getTimeMult")?new MutableStat(1f):n.equals("getEntity")?ship:null);
        Object call(String n,Object[] a) {
            switch(n) {
                case "getLocation":return loc;
                case "getVelocity":return vel;
                case "getCustomData":return data;
                case "setCustomData":data.put((String)a[0],a[1]);return null;
                case "removeCustomData":data.remove(a[0]);return null;
                case "getOwner":case "getOriginalOwner":return owner;
                case "setOwner":owner=(Integer)a[0];return null;
                case "getFacing":return facing;
                case "setFacing":facing=(Float)a[0];return null;
                case "setStart":start=(Vector2f)a[0];return null;
                case "getSource":return source;
                case "getHitpoints":return hp;
                case "getMaxHitpoints":return maxHp;
                case "getDamage":return mock(DamageAPI.class,(x,y)->x.equals("getDamage")?50f:x.equals("getType")?DamageType.KINETIC:null);
                case "setHitpoints":hp=(Float)a[0];return null;
                case "getHullLevel":return hp/maxHp;
                case "setMaxRange":maxRange=(Float)a[0];return null;
                case "getMaxRange":return maxRange;
                case "isAlive":return alive && hp>0;
                case "isHulk":return !alive;
                case "isExpired":return expired;
                case "isFizzling":return fizzling;
                case "isPhased":return phased;
                case "hasTag":return constructing;
                case "getFluxTracker":return mock(FluxTrackerAPI.class,(x,y)->x.equals("isOverloadedOrVenting")?overloaded:null);
                case "getShipTarget":return target;
                case "getMouseTarget":return mouse;
                case "useSystem":systemUses++;return null;
                case "isFighter":return fighter;
                case "getWing":return wing;
                case "setWing":wing=(FighterWingAPI)a[0];return null;
                case "getShipAI":return pilot;
                case "resetDefaultAI":aiResets++;pilot=new ThreatSwarmAI(ship);return null;
                case "getVariant":return mock(ShipVariantAPI.class,(x,y)->x.equals("getHullVariantId")?SwarmLauncherEffect.ATTACK_SWARM_VARIANT:null);
                case "getWeaponGroupsCopy":return Collections.emptyList();
                case "isWingLeader":return wing!=null&&!wing.getWingMembers().isEmpty()&&wing.getWingMembers().get(0)==ship;
                case "setAngularVelocity":angularVelocity=(Float)a[0];return null;
                case "getAngularVelocity":return angularVelocity;
                case "setShipTarget":target=(ShipAPI)a[0];return null;
                case "getMutableStats":return stats;
                case "getAllWeapons":return Collections.emptyList();
                case "getMaxSpeedWithoutBoost":return 200f;
                case "getCollisionRadius":return 8f;
                case "setMissileAI":ai=(MissileAIPlugin)a[0];return null;
                case "giveCommand":commands.add((ShipCommand)a[0]);return null;
                case "getArmorGrid":return mock(ArmorGridAPI.class,(x,y)->null);
            }
            return null;
        }
        Body ship(int side,boolean isFighter){owner=side;fighter=isFighter;ship=mock(ShipAPI.class,this::call);return this;}
        Body missile(int side,ShipAPI shooter){owner=side;source=shooter;missile=mock(MissileAPI.class,this::call);return this;}
    }

    static class Fixture {
        Map<String,Object> data=new HashMap<>();
        Set<CombatEntityAPI> entities=Collections.newSetFromMap(new IdentityHashMap<>());
        List<EveryFrameCombatPlugin> plugins=new ArrayList<>();
        List<CombatLayeredRenderingPlugin> layers=new ArrayList<>();
        List<Body> spawned=new ArrayList<>();
        List<Body> launched=new ArrayList<>();
        Map<ShipAPI,WeaponAPI> launchers=new IdentityHashMap<>();
        boolean paused,suppress,failSpawn;
        boolean aware=true;
        float frameTime=1/60f;
        ShipAPI player;
        int spawns,removals;
        int untrackedWings;
        int damageModifiers;
        CombatEngineAPI engine;
        CombatFleetManagerAPI manager;
        Fixture() {
            manager=mock(CombatFleetManagerAPI.class,(n,a)->{
                switch(n) {
                    case "isSuppressDeploymentMessages":return suppress;
                    case "setSuppressDeploymentMessages":suppress=(Boolean)a[0];break;
                    case "spawnShipOrWing":
                        spawns++;
                        check(a[0].equals(SwarmLauncherEffect.ATTACK_SWARM_WING),"Native attack wing required");
                        if(failSpawn)throw new IllegalStateException("fixture spawn failure");
                        Body b=new Body().ship(0,true);b.hp=b.maxHp=1000;
                        b.loc.set((Vector2f)a[1]);
                        List<ShipAPI> members=new ArrayList<>();members.add(b.ship);
                        Map<String,Object> wingData=new HashMap<>();
                        b.wing=mock(FighterWingAPI.class,(w,v)->{
                            if(w.equals("getWingId"))return SwarmLauncherEffect.ATTACK_SWARM_WING;
                            if(w.equals("getWingMembers"))return members;
                            if(w.equals("getCustomData"))return wingData;
                            if(w.equals("removeMember")){members.remove(v[0]);return null;}
                            if(w.equals("addMember")){members.add((ShipAPI)v[0]);return null;}
                            if(w.equals("setSourceShip"))check(v[0]==null,"Converted wings must be independent");
                            return null;
                        });
                        spawned.add(b);entities.add(b.ship);
                        // Reproduce native FleetManager's ten0.2s pre-deployment
                        // fighter advances, which our previous fixture omitted.
                        b.loc.x+=650;b.loc.y-=420;b.vel.set(-240,130);b.facing=(Float)a[2]+150;b.angularVelocity=40;
                        FragmentSwarmHullmod.createSwarmFor(b.ship).addMembers(50);
                        return b.ship;
                    case "removeDeployed":
                        if(!(Boolean)a[1]){untrackedWings++;return null;}
                        FighterWingAPI wing=(FighterWingAPI)a[0];
                        for(ShipAPI member:wing.getWingMembers())if(entities.remove(member))removals++;
                        break;
                }
                return null;
            });
            engine=mock(CombatEngineAPI.class,(n,a)->{
                switch(n) {
                    case "getCustomData":return data;
                    case "applyDamageModifiersToSpawnedProjectileWithNullWeapon":
                        check(a[1]==WeaponAPI.WeaponType.MISSILE&&a[3]!=null,"Native missile damage modifiers applied to helper packet");damageModifiers++;break;
                    case "getElapsedInLastFrame":return frameTime;
                    case "getPlayerShip":return player;
                    case "isAwareOf":return aware;
                    case "getShips":return entities.stream().filter(e->e instanceof ShipAPI).map(e->(ShipAPI)e).toList();
                    case "spawnProjectile":
                        check(a[1]==null && a[2].equals(ExodusPlaguePacketEffect.PACKET_ID),"System uses the six-argument hidden weapon helper, without a fitted gun");
                        ShipAPI source=(ShipAPI)a[0];Body packet=new Body().missile(source.getOwner(),source);
                        packet.loc.set((Vector2f)a[3]);packet.facing=(Float)a[4];
                        entities.add(packet.missile);launched.add(packet);return packet.missile;
                    case "isPaused":return paused;
                    case "isShipAlive":return entities.contains(a[0])&&((ShipAPI)a[0]).isAlive();
                    case "isEntityInPlay":case "isMissileAlive":return entities.contains(a[0]);
                    case "applyDamage":if(a[0] instanceof ShipAPI&&((ShipAPI)a[0]).getHitpoints()<=0)entities.remove(a[0]);break;
                    case "getFleetManager":return manager;
                    case "addPlugin":plugins.add((EveryFrameCombatPlugin)a[0]);break;
                    case "removePlugin":plugins.remove(a[0]);break;
                    case "removeEntity":if(entities.remove(a[0])&&a[0] instanceof ShipAPI)removals++;break;
                    case "addLayeredRenderingPlugin":
                        CombatLayeredRenderingPlugin layer=(CombatLayeredRenderingPlugin)a[0];
                        Vector2f location=new Vector2f();
                        CombatEntityAPI entity=mock(CombatEntityAPI.class,(x,y)->x.equals("getLocation")?location:null);
                        layer.init(entity);layers.add(layer);return entity;
                }
                return null;
            });
            Global.setCombatEngine(engine);
            Global.setSettings(mock(SettingsAPI.class,(n,a)->{
                if(n.equals("getColor"))return Color.WHITE;
                if(n.equals("getSprite"))return mock(SpriteAPI.class,(x,y)->null);
                return null;
            }));
            Global.setSoundPlayer(mock(SoundPlayerAPI.class,(n,a)->null));
        }
        Body packet(int side,ShipAPI shooter) {
            return packet(side,shooter,launchers.computeIfAbsent(shooter, s -> launcher(s)));
        }
        WeaponAPI launcher(ShipAPI shooter) {
            return mock(WeaponAPI.class,(n,a)->n.equals("getShip")?shooter:null);
        }
        Body packet(int side,ShipAPI shooter,WeaponAPI launcher) {
            Body b=new Body().missile(side,shooter);entities.add(b.missile);
            new ExodusPlaguePacketEffect().initialize(b.missile,launcher,engine);return b;
        }
        Body thirdPacket(int side,ShipAPI shooter) {packet(side,shooter);packet(side,shooter);return packet(side,shooter);}
        void tick(float time){for(EveryFrameCombatPlugin plugin:new ArrayList<>(plugins))plugin.advance(time,Collections.emptyList());}
    }

    static void geometry() {
        for(int angle:new int[]{0,90,180,270})for(float lane:new float[]{-1,0,1}) {
            Body source=new Body().ship(0,false);source.vel.set(80,-35);
            Body packet=new Body().missile(0,source.ship);packet.facing=angle;packet.loc.set(1200,-700);
            Vector2f origin=new Vector2f(packet.loc);
            ExodusPlaguePacketEffect.configureFlight(packet.missile,lane);
            Vector2f forward=com.fs.starfarer.api.util.Misc.getUnitVectorAtDegreeAngle(angle);
            Vector2f side=new Vector2f(-forward.y,forward.x);
            Vector2f offset=Vector2f.sub(packet.loc,origin,null);
            near(Vector2f.dot(offset,side),100*lane,"220-unit starting envelope at every facing");
            near(Vector2f.dot(offset,forward),0,"No forward jump");
            Vector2f relative=Vector2f.sub(packet.vel,source.vel,null);
            near(Vector2f.dot(relative,forward),900,"Halved forward speed and inherited ship velocity");
            near(Vector2f.dot(offset,side)+Vector2f.dot(relative,side)*7500/900,133*lane,"286-unit end envelope");
            near(Vector2f.sub(packet.start,packet.loc,null).length(),0,"Native range begins at actual launch point");
        }
    }

    static void conversion(int side,boolean shield) {
        Fixture f=new Fixture();Body shooter=new Body().ship(side,false),target=new Body().ship(1-side,false);
        f.entities.add(shooter.ship);f.entities.add(target.ship);
        ExodusPlaguePacketEffect effect=new ExodusPlaguePacketEffect();
        Body first=f.thirdPacket(side,shooter.ship);
        RoilingSwarmEffect cloud=RoilingSwarmEffect.getSwarmFor(first.missile);
        check(cloud.getNumActiveMembers()==8,"Native flight cloud has exactly packet fragments");
        near(first.maxRange,7500,"System-spawned missile receives an explicit native range");
        first.ai.advance(.1f);check(first.commands.size()==1,"Unguided flight accelerates");
        f.paused=true;first.ai.advance(.1f);check(first.commands.size()==1,"Paused missile AI is inert");f.paused=false;
        check(f.spawns==0,"No attack wings exist before contact");
        Map<RoilingSwarmEffect.SwarmMember,Vector2f> positions=new IdentityHashMap<>(),velocities=new IdentityHashMap<>();
        for(var fragment:cloud.getMembers()){positions.put(fragment,new Vector2f(fragment.loc));velocities.put(fragment,new Vector2f(fragment.vel));}
        effect.onHit(first.missile,target.ship,new Vector2f(),shield,null,f.engine);
        check(f.spawns==1 && f.plugins.size()==1,"Contact produces one tracked native wing");
        Body swarm=f.spawned.get(0);
        Vector2f arrival=com.fs.starfarer.api.util.Misc.getUnitVectorAtDegreeAngle(first.facing);arrival.scale(-45f);
        near(Vector2f.sub(swarm.loc,arrival,null).length(),0,"Native two-second pre-advance cannot move a converted swarm off its impact origin");
        near(swarm.facing,first.facing,"Converted swarm faces the incoming packet rather than its pre-advanced escape direction");
        near(swarm.angularVelocity,0,"Pre-advanced rotation is discarded");
        check(swarm.aiResets==1,"Native startup AI is reset after correcting the impact position");
        check(Vector2f.dot(swarm.vel,first.vel)>0&&swarm.vel.length()<=200.01f,"Handoff carries bounded incoming motion");
        check(swarm.owner==side && swarm.target==target.ship,"Correct side and initial target, including shield hits");
        RoilingSwarmEffect converted=RoilingSwarmEffect.getSwarmFor(swarm.ship);
        check(converted.getNumActiveMembers()==8 && cloud.getNumActiveMembers()==0,"Actual native fragments transfer at contact");
        for(var fragment:converted.getMembers()) {
            check(positions.containsKey(fragment),"Converted fragment identity must come from the live incoming cloud");
            near(Vector2f.sub(fragment.loc,positions.get(fragment),null).length(),0,"Handoff never teleports incoming fragments");
            near(Vector2f.sub(fragment.vel,velocities.get(fragment),null).length(),0,"Handoff preserves incoming fragment world motion");
        }
        near(swarm.hp,160.001f,"HP proportional to arriving fragment count");
        check(!converted.getParams().withRespawn && !converted.getParams().withInitialMembers
                && converted.getParams().memberExchangeClass==null,"No free fill/regrowth/foreign exchanges");
        effect.onHit(first.missile,target.ship,new Vector2f(),shield,null,f.engine);
        check(converted.getNumActiveMembers()==8,"Repeated callback cannot duplicate conversion");
        for(int i=0;i<40;i++)effect.onHit(f.packet(side,shooter.ship).missile,target.ship,new Vector2f(),shield,null,f.engine);
        check(f.spawns==10,"Exactly ten autonomous swarms per launcher; extra eligible contacts cannot exceed cap");
        Set<FighterWingAPI> formations=Collections.newSetFromMap(new IdentityHashMap<>());
        for(Body release:f.spawned)formations.add(release.wing);
        check(formations.size()==3&&f.untrackedWings==7,"Ten nearby releases use native4/4/2 formations instead of ten mutually repelling leaders");
        check(((ThreatSwarmAI)swarm.pilot).getShared().target==target.ship,"Native formation immediately knows the struck target");
        check(converted.getNumActiveMembers()==8,"Separate releases never reinforce existing swarms");
        WeaponAPI otherLauncher=f.launcher(shooter.ship);
        for(int i=0;i<30;i++)effect.onHit(f.packet(side,shooter.ship,otherLauncher).missile,target.ship,new Vector2f(),shield,null,f.engine);
        check(f.spawns==20,"Second launcher on the same ship has its own ten-swarm limit and shot sequence");
        Set<FighterWingAPI> allFormations=Collections.newSetFromMap(new IdentityHashMap<>());
        for(Body release:f.spawned)allFormations.add(release.wing);
        check(allFormations.size()==6,"The two emitters retain separate native formations");
        f.paused=true;f.tick(100);check(f.removals==0,"Pause freezes lifetime");f.paused=false;
        f.tick(19);check(f.removals==0,"No early timeout");
        swarm.alive=false;
        for(int i=0;i<3;i++)effect.onHit(f.packet(side,shooter.ship).missile,target.ship,new Vector2f(),shield,null,f.engine);
        check(f.spawns==21,"A destroyed swarm frees a slot immediately at contact");
        f.tick(2);check(f.removals==20,"Original wings expire through fleet manager");
        f.tick(19);check(f.removals==21 && f.plugins.isEmpty(),"All temporary wings and controller retire");
        for(int i=0;i<3;i++)effect.onHit(f.packet(side,shooter.ship).missile,target.ship,new Vector2f(),shield,null,f.engine);
        check(f.spawns==22,"Fresh controller preserves per-launcher shot sequence and released cap slots");
    }

    static void interceptionAndGuards() {
        Fixture f=new Fixture();Body source=new Body().ship(0,false),target=new Body().ship(1,false);
        ExodusPlaguePacketEffect effect=new ExodusPlaguePacketEffect();
        Body destroyed=f.thirdPacket(0,source.ship);f.entities.remove(destroyed.missile);
        RoilingSwarmEffect cloud=RoilingSwarmEffect.getSwarmFor(destroyed.missile);
        check(cloud.shouldDespawnAll(),"Intercepted packets disperse their native flight cloud");
        for(int i=0;i<50;i++)cloud.advance(.1f);
        check(cloud.isExpired() && f.spawns==0,"PD destruction never generates a Defabricate");
        Body packet=f.thirdPacket(0,source.ship);
        effect.onHit(packet.missile,source.ship,new Vector2f(),false,null,f.engine);
        effect.onHit(packet.missile,destroyed.missile,new Vector2f(),false,null,f.engine);
        effect.onHit(packet.missile,mock(CombatEntityAPI.class,(n,a)->null),new Vector2f(),false,null,f.engine);
        check(f.spawns==0,"Friendly, PD and asteroid contacts cannot spawn wings");
        Body partial=f.thirdPacket(0,source.ship);partial.hp=30;
        effect.onHit(partial.missile,target.ship,new Vector2f(),false,null,f.engine);
        check(RoilingSwarmEffect.getSwarmFor(f.spawned.get(0).ship).getNumActiveMembers()==4,
                "Partial PD damage reduces converted fragments");
        f.tick(21);
        Fixture failure=new Fixture();failure.failSpawn=true;failure.suppress=true;
        try {effect.onHit(failure.thirdPacket(0,source.ship).missile,target.ship,new Vector2f(),true,null,failure.engine);
            throw new AssertionError("Expected fixture failure");}
        catch(IllegalStateException expected){check(failure.suppress,"Deployment suppression restored after failure");}
        failure.tick(.1f);check(failure.plugins.isEmpty(),"Failed conversion leaves no controller");
    }

    static void everyThirdFiredPacket() {
        Fixture f=new Fixture();Body source=new Body().ship(0,false),target=new Body().ship(1,false);
        ExodusPlaguePacketEffect effect=new ExodusPlaguePacketEffect();
        for(int i=1;i<=9;i++) {
            Body packet=f.packet(0,source.ship);
            if(i==3) {f.entities.remove(packet.missile);continue;} // PD destroys a conversion-eligible packet.
            effect.onHit(packet.missile,target.ship,new Vector2f(),true,null,f.engine);
            check(f.spawns==(i<6?0:i<9?1:2),"Only every third FIRED packet converts; interception never advances another packet");
        }
    }

    static void stochasticFlight() {
        for(int facing:new int[]{0,90,180,270})for(float lane:new float[]{-1,-.5f,0,.5f,1}) {
            Fixture f=new Fixture();Body source=new Body().ship(0,false);
            source.vel.set(70,-30);
            Body packet=new Body().missile(0,source.ship);packet.facing=facing;
            Vector2f forward=com.fs.starfarer.api.util.Misc.getUnitVectorAtDegreeAngle(facing),side=new Vector2f(-forward.y,forward.x);
            ExodusPlaguePacketEffect.configureFlight(packet.missile,lane);
            ExodusPlaguePacketEffect.SwarmFlight ai=new ExodusPlaguePacketEffect.SwarmFlight(packet.missile,f.engine,
                    forward,lane,.75f,3.1f,22f);
            float min=Float.MAX_VALUE,max=-Float.MAX_VALUE;
            for(int i=0;i<500;i++) {
                ai.advance(1/60f);
                Vector2f relative=Vector2f.sub(packet.vel,source.vel,null);
                near(Vector2f.dot(relative,forward),900,"Weaving retains requested forward speed");
                packet.loc.x+=relative.x/60f;packet.loc.y+=relative.y/60f;
                float across=Vector2f.dot(packet.loc,side),along=Vector2f.dot(packet.loc,forward);
                float bound=100+33*along/7500;
                check(Math.abs(across)<=bound+.05f,"Stochastic flight stays inside the widening envelope");
                float lateral=Vector2f.dot(relative,side);min=Math.min(min,lateral);max=Math.max(max,lateral);
            }
            if(Math.abs(lane)<1)check(max-min>25,"Interior packets weave instead of following straight conveyor lanes");
            Vector2f velocity=new Vector2f(packet.vel);f.paused=true;ai.advance(10);
            near(Vector2f.sub(packet.vel,velocity,null).length(),0,"Pause freezes stochastic motion");
        }
    }

    static void taperedEmission() {
        near(ExodusPlagueSystem.edgeEnvelope(0),0,"Leading tip has zero density envelope");
        near(ExodusPlagueSystem.edgeEnvelope(.5f),.5f,"Leading edge builds smoothly");
        near(ExodusPlagueSystem.edgeEnvelope(1),1,"Core reaches full width");
        near(ExodusPlagueSystem.edgeEnvelope(4.5f),.5f,"Trailing edge fades smoothly");
        near(ExodusPlagueSystem.edgeEnvelope(5),0,"Trailing tip has zero density envelope");
        for(int seed=0;seed<8;seed++) {
            float[] times=ExodusPlagueSystem.emissionTimes(new Random(seed));
            float[] other=ExodusPlagueSystem.emissionTimes(new Random(seed+100));
            check(times.length==200,"Taper preserves the full per-emitter damage budget");
            int[] bins=new int[5];int firstHalf=0,lastHalf=0,different=0;
            float previous=0;
            for(int i=0;i<times.length;i++) {
                float time=times[i];
                check(time>previous&&time<5,"Jittered emissions stay ordered and inside the active period");
                previous=time;bins[(int)time]++;
                if(time<.5f)firstHalf++;
                if(time>4.5f)lastHalf++;
                if(Math.abs(time-other[i])>.0001f)different++;
            }
            check(Arrays.equals(bins,new int[]{25,50,50,50,25}),"Sparse edges and a dense middle replace abrupt constant-rate edges");
            check(firstHalf>=4&&firstHalf<=5&&lastHalf>=4&&lastHalf<=5,"Both tips are substantially sparser than the core");
            check(different>180,"Emitters use independent stochastic timing rather than synchronized rows");
        }
        for(int facing:new int[]{0,90,180,270})for(float scale:new float[]{.1f,.5f,1f}) {
            Fixture f=new Fixture();Body source=new Body().ship(0,false);
            source.vel.set(70,-30);
            Vector2f forward=com.fs.starfarer.api.util.Misc.getUnitVectorAtDegreeAngle(facing);
            Vector2f side=new Vector2f(-forward.y,forward.x);
            for(int sample=0;sample<8;sample++) {
                Body packet=new Body().missile(0,source.ship);packet.facing=facing;f.entities.add(packet.missile);
                ExodusPlaguePacketEffect.initialize(packet.missile,new Object(),f.engine,scale);
                RoilingSwarmEffect cloud=RoilingSwarmEffect.getSwarmFor(packet.missile);
                near(cloud.getParams().maxOffset,10*scale,"Native fragment clouds narrow at the tips");
                check(cloud.getNumActiveMembers()==8,"Taper preserves packet fragments and damage");
                check(Math.abs(Vector2f.dot(packet.loc,side))<=100*scale+.01f,"Actual launch lanes narrow with the envelope");
                for(var member:cloud.getMembers())check(Vector2f.sub(member.loc,packet.loc,null).length()<=10*scale+.01f,
                        "Initial native fragments fit the tapered cloud");
                for(int frame=0;frame<500;frame++) {
                    packet.ai.advance(1/60f);
                    Vector2f relative=Vector2f.sub(packet.vel,source.vel,null);
                    near(Vector2f.dot(relative,forward),900,"Taper does not change forward speed");
                    packet.loc.x+=relative.x/60f;packet.loc.y+=relative.y/60f;
                    float bound=(100+33*(frame+1)/60f*900/7500+44)*scale;
                    check(Math.abs(Vector2f.dot(packet.loc,side))<=bound+.01f,"Real weaving paths retain narrowed leading/trailing sections in flight");
                }
            }
        }
    }

    static void shipSystem() {
        for(int hz:new int[]{10,60,120}) {
            Fixture f=new Fixture();f.frameTime=1f/hz;
            Body source=new Body().ship(0,false),target=new Body().ship(1,false);
            source.target=target.ship;target.loc.set(3000,0);f.entities.add(source.ship);f.entities.add(target.ship);
            ExodusPlagueSystem system=new ExodusPlagueSystem();
            for(int i=0;i<hz;i++)system.apply(source.stats,ExodusPlagueSystem.SYSTEM_ID,State.IN,1f);
            check(f.launched.isEmpty(),"Windup does not emit packets");
            int previousCount=0;
            for(int i=0;i<5*hz;i++) {
                system.apply(source.stats,ExodusPlagueSystem.SYSTEM_ID,State.ACTIVE,1f);
                if((i+1)%hz==0) {
                    int second=(i+1)/hz;
                    check(f.launched.size()-previousCount==(second==1||second==5?50:100),
                            "Production emitter ramps density at both edges at "+hz+"Hz, second "+second);
                    previousCount=f.launched.size();
                }
            }
            check(f.launched.size()==400,"Five-second system emits two200-packet streams at "+hz+"Hz: "+f.launched.size());
            check(f.damageModifiers==400,"System preserves source missile damage modifiers on every packet");
            system.apply(source.stats,ExodusPlagueSystem.SYSTEM_ID,State.ACTIVE,1f);
            check(f.launched.size()==400,"Repeated active callbacks cannot exceed one burst budget");
            ExodusPlaguePacketEffect contact=new ExodusPlaguePacketEffect();
            for(Body packet:f.launched)contact.onHit(packet.missile,target.ship,new Vector2f(),true,null,f.engine);
            check(f.spawns==20,"System's virtual emitters have independent ten-Defabricate caps");
            system.unapply(source.stats,ExodusPlagueSystem.SYSTEM_ID);
            system.apply(source.stats,ExodusPlagueSystem.SYSTEM_ID,State.COOLDOWN,0f);
            check(f.launched.size()==400,"Cooldown emits no packets");
            f.paused=true;system.apply(source.stats,ExodusPlagueSystem.SYSTEM_ID,State.ACTIVE,1f);
            check(f.launched.size()==400,"Paused system is inert");f.paused=false;
            source.phased=true;system.apply(source.stats,ExodusPlagueSystem.SYSTEM_ID,State.ACTIVE,1f);
            check(f.launched.size()==400,"Phase blocks system emission");source.phased=false;
            source.constructing=true;check(!system.isUsable(null,source.ship),"Construction cannot activate the plague");source.constructing=false;
            source.overloaded=true;check(!system.isUsable(null,source.ship),"Overload/vent blocks the plague");source.overloaded=false;
            int start=f.launched.size();for(int i=0;i<hz;i++)system.apply(source.stats,ExodusPlagueSystem.SYSTEM_ID,State.ACTIVE,1f);
            for(Body packet:f.launched.subList(start,f.launched.size()))contact.onHit(packet.missile,target.ship,new Vector2f(),true,null,f.engine);
            check(f.spawns==20,"Unapply/new activation must not reset live emitter caps");
            f.tick(21);check(f.removals==20,"All system-created swarms disperse and free the caps");
            source.alive=false;check(!system.isUsable(null,source.ship),"Dead Gautama cannot activate");
        }
        Fixture f=new Fixture();Body source=new Body().ship(0,false),target=new Body().ship(1,false);
        target.loc.set(2500,0);f.entities.add(source.ship);f.entities.add(target.ship);
        ShipSystemAPI.SystemState[] state={ShipSystemAPI.SystemState.IDLE};
        ShipSystemAPI system=mock(ShipSystemAPI.class,(n,a)->n.equals("getState")?state[0]:n.equals("canBeActivated")?true:null);
        ExodusPlagueAI ai=new ExodusPlagueAI();ai.init(source.ship,system,new ShipwideAIFlags(),f.engine);
        ai.advance(1f,null,null,target.ship);check(source.systemUses==1,"NPC activates plague against an enemy without Energy Lash");
        state[0]=ShipSystemAPI.SystemState.ACTIVE;ai.advance(1f,null,null,target.ship);
        check(source.systemUses==1,"NPC never repeatedly toggles an active system");
        state[0]=ShipSystemAPI.SystemState.IDLE;f.aware=false;ai.advance(1f,null,null,target.ship);
        check(source.systemUses==1,"NPC cannot activate against unseen enemies");
        f.aware=true;target.loc.set(9000,0);ai.advance(1f,null,null,target.ship);
        check(source.systemUses==1,"NPC respects system range");
        for(int facing:new int[]{0,90,180,270}) {
            source.facing=facing;Vector2f a=ExodusPlagueSystem.emitterOrigin(source.ship,0),b=ExodusPlagueSystem.emitterOrigin(source.ship,1);
            near(Vector2f.sub(a,b,null).length(),170,"Virtual emitters retain paired hull spacing at all facings");
        }
    }

    static void continuousImpactHandoff() {
        Fixture f=new Fixture();Body source=new Body().ship(0,false),target=new Body().ship(1,false);
        target.loc.set(1000,0);source.target=target.ship;f.entities.add(source.ship);f.entities.add(target.ship);
        ExodusPlagueSystem system=new ExodusPlagueSystem();ExodusPlaguePacketEffect effect=new ExodusPlaguePacketEffect();
        float first=-1,last=-1;int previous=0;
        for(int frame=0;frame<300;frame++) {
            float time=(frame+1)/60f;
            system.apply(source.stats,ExodusPlagueSystem.SYSTEM_ID,State.ACTIVE,1f);
            for(Body packet:new ArrayList<>(f.launched)) {
                if(!f.entities.contains(packet.missile))continue;
                packet.ai.advance(1/60f);packet.loc.x+=packet.vel.x/60f;packet.loc.y+=packet.vel.y/60f;
                if(packet.loc.x>=1000) {
                    effect.onHit(packet.missile,target.ship,new Vector2f(1000,packet.loc.y),true,null,f.engine);
                    f.entities.remove(packet.missile);
                    if(f.spawns>previous){if(first<0)first=time;last=time;previous=f.spawns;}
                }
            }
            for(var layer:new ArrayList<>(f.layers))if(!layer.isExpired())layer.advance(1/60f);
            f.tick(1/60f);
        }
        check(first>1.3f&&first<1.8f&&last<2.5f&&f.spawns==20,
                "Defabricates emerge on successive eligible impacts during the beam, not in an end-of-attack queue: "+first+" to "+last);
        Body firstSwarm=f.spawned.get(0);firstSwarm.loc.set(1200,160);f.tick(.1f);
        near(firstSwarm.loc.x,1200,"Birth correction never pins the swarm after emergence");
    }

    public static void main(String[] args) {
        new Fixture();
        geometry();
        for(int side=0;side<=1;side++)for(boolean shield:new boolean[]{false,true})conversion(side,shield);
        interceptionAndGuards();
        everyThirdFiredPacket();
        stochasticFlight();
        taperedEmission();
        shipSystem();
        continuousImpactHandoff();
        System.out.println("PASS: smooth density/width taper, independent emission jitter, full400-packet budget, native impact handoff/formations, preserved fragment motion, independent caps/cleanup, stochastic streams, system/AI timing and PD.");
    }
}
