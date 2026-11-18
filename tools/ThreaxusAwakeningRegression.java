import chiefnavigator.hullmods.ThreaxusAwakening;
import chiefnavigator.hullmods.ThreatxusSwarmCloud;
import chiefnavigator.systems.ThreatxusFabricationSystem;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.State;
import org.lwjgl.util.vector.Vector2f;

/** Public callbacks with actual MutableStats/native damage modifier semantics. */
public final class ThreaxusAwakeningRegression extends ExodusPlagueRegression {
    public static void main(String[] args){
        var f=new ThreatxusFabricationRegression.FactoryFixture(1);
        com.fs.starfarer.api.Global.setSettings(mock(com.fs.starfarer.api.SettingsAPI.class,(n,a)->{
            if(n.equals("getColor"))return java.awt.Color.WHITE;
            if(n.equals("getSprite"))return mock(com.fs.starfarer.api.graphics.SpriteAPI.class,(x,y)->null);
            if(n.equals("getAngleInDegreesFast")){
                Vector2f to=(Vector2f)a[a.length-1],from=a.length==2?(Vector2f)a[0]:new Vector2f();
                return (float)Math.toDegrees(Math.atan2(to.y-from.y,to.x-from.x));
            }
            return null;
        }));
        var core=f.factory;
        ThreaxusAwakening phase=new ThreaxusAwakening();
        ThreatxusFabricationSystem factory=new ThreatxusFabricationSystem();
        phase.applyEffectsBeforeShipCreation(ShipAPI.HullSize.CAPITAL_SHIP,core.stats,"test");
        phase.applyEffectsAfterShipCreation(core.ship,"test");
        near(core.stats.getMaxSpeed().getModifiedValue(),0,"Dormant core cannot move");
        near(core.damageTaken.getModifiedValue(),1,"Native parent remains hull-damageable for last-module bookkeeping");
        phase.advanceInCombat(core.ship,5f);
        check(ThreaxusAwakening.isDormant(core.ship),"Empty pre-module deployment warmup cannot wake core");
        boolean[] coreGun={false};
        core.weapons.add(mock(WeaponAPI.class,(n,a)->{if(n.equals("setForceNoFireOneFrame"))coreGun[0]=(Boolean)a[0];return null;}));
        for(int i=0;i<3;i++){
            var child=f.add(new ThreatxusFabricationRegression.Ship("chief_navigator_threaxus_wall",ShipAPI.HullSize.FRIGATE,1));
            child.parent=core.ship;core.modules.add(child.ship);
        }
        phase.advanceInCombat(core.ship,.1f);
        check(coreGun[0]&&!core.holdOneFrame,"Only core weapons are suppressed; inherited parent hold-fire never silences module guns");
        check(!factory.isUsable(null,core.ship),"Dormant core cannot fabricate");
        factory.apply(core.stats,"test",State.ACTIVE,1);
        check(f.units.isEmpty()&&f.fabricators.isEmpty(),"Dormant factory emits no child unit");
        var guard=(DamageTakenModifier)core.listeners.get(0);
        MutableStat incoming=new MutableStat(100f);
        DamageAPI hit=mock(DamageAPI.class,(n,a)->n.equals("getModifier")?incoming:null);
        check(guard.modifyDamageTaken(null,core.ship,hit,core.loc,false)!=null,"Dormant body/contact/explosion hits intercepted");
        near(incoming.getModifiedValue(),0,"Dormant core takes no incoming damage");
        MutableStat foreignDamage=new MutableStat(100f);
        DamageAPI otherHit=mock(DamageAPI.class,(n,a)->n.equals("getModifier")?foreignDamage:null);
        check(guard.modifyDamageTaken(null,core.modules.get(0),otherHit,core.loc,false)==null,"Core guard never protects a module or foreign ship");
        near(foreignDamage.getModifiedValue(),100,"Modules take ordinary incoming damage");
        for(int i=0;i<2;i++)f.bodies.get(core.modules.get(i)).alive=false;
        phase.advanceInCombat(core.ship,.1f);
        check(ThreaxusAwakening.isDormant(core.ship),"One remaining module holds first phase even if every other section is gone");
        f.bodies.get(core.modules.get(2)).alive=false;
        f.paused=true;phase.advanceInCombat(core.ship,10f);
        check(ThreaxusAwakening.isDormant(core.ship),"Pause freezes the transition");
        f.paused=false;
        var target=f.add(new ThreatxusFabricationRegression.Ship("player",ShipAPI.HullSize.CRUISER,0));
        target.loc.set(core.loc.x,core.loc.y+2000f);
        Vector2f position=new Vector2f(core.loc),velocity=new Vector2f(core.vel);
        phase.advanceInCombat(core.ship,.1f);
        check(!ThreaxusAwakening.isDormant(core.ship),"Last attached module wakes the core");
        near(core.stats.getMaxSpeed().getModifiedValue(),70,"Awakened chase speed");
        near(core.stats.getAcceleration().getModifiedValue(),20,"Native chase acceleration");
        near(core.stats.getMaxTurnRate().getModifiedValue(),15,"Awakened turn rate");
        near(core.stats.getArmorDamageTakenMult().getModifiedValue(),1,"Awake armor protection is gone");
        near(core.stats.getEmpDamageTakenMult().getModifiedValue(),1,"Awake EMP protection is gone");
        check(core.target==target.ship&&core.commands.contains(ShipCommand.ACCELERATE)
                &&core.commands.contains(ShipCommand.TURN_LEFT),"Awake core turns and pursues a visible hostile ship");
        check(core.loc.equals(position)&&core.vel.equals(velocity),"Transition/chase never teleports or overwrites momentum");
        MutableStat awakeDamage=new MutableStat(100f);
        DamageAPI awakeHit=mock(DamageAPI.class,(n,a)->n.equals("getModifier")?awakeDamage:null);
        check(guard.modifyDamageTaken(null,core.ship,awakeHit,core.loc,false)==null,"Awake core takes ordinary damage");
        near(awakeDamage.getModifiedValue(),100,"Damage listener releases core completely");
        check(factory.isUsable(null,core.ship),"Awake core unlocks fabrication");
        factory.apply(core.stats,"test",State.ACTIVE,1);
        check(f.units.size()==1,"Awakened production still prioritizes Exodus Assault");
        for(int i=0;i<10;i++)phase.advanceInCombat(core.ship,.1f);
        near(core.stats.getMaxSpeed().getModifiedValue(),70,"Awakening bonus is applied exactly once");
        check(core.listeners.size()==1,"Repeated callbacks never stack damage listeners");
        System.out.println("PASS:3wall gate, deployment warmup/pause, protected dormant core, live module guns, native damageability after last-wall death, one-time70speed pursuit, ordinary awake damage and fabrication unlock.");
    }
}
