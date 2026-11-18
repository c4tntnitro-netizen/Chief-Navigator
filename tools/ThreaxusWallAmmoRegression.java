import chiefnavigator.hullmods.ThreaxusWallSwarm;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.combat.threat.*;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import java.util.*;

/** Real native cloud capacity and SwarmLauncherEffect ready/depleted behavior. */
public final class ThreaxusWallAmmoRegression extends ExodusPlagueRegression {
    public static void main(String[] args){
        Fixture f=new Fixture();
        var body=new ThreatxusFabricationRegression.Ship(ThreaxusWallSwarm.HULL_ID,ShipAPI.HullSize.FRIGATE,1);
        Map<String,MutableStat> values=new HashMap<>();Map<String,StatBonus> mods=new HashMap<>();
        DynamicStatsAPI dynamic=mock(DynamicStatsAPI.class,(n,a)->{
            String id=(String)a[0];
            if(n.equals("getMod"))return mods.computeIfAbsent(id,k->new StatBonus());
            if(n.equals("getStat"))return values.computeIfAbsent(id,k->new MutableStat(1f));
            if(n.equals("getValue"))return a.length==2?mods.computeIfAbsent(id,k->new StatBonus()).computeEffective((Float)a[1])
                    :values.computeIfAbsent(id,k->new MutableStat(1f)).getModifiedValue();
            return null;
        });
        ShipVariantAPI variant=mock(ShipVariantAPI.class,(n,a)->n.equals("getHullSpec")?body.data.get("spec"):
                n.equals("hasHullMod")?true:null);
        body.stats=mock(MutableShipStatsAPI.class,(n,a)->{
            if(n.equals("getDynamic"))return dynamic;
            if(n.equals("getVariant"))return variant;
            if(n.equals("getEntity"))return body.ship;
            if(n.equals("getTimeMult"))return new MutableStat(1f);
            if(n.equals("getZeroFluxSpeedBoost"))return new MutableStat(0f);
            return null;
        });
        body.ship=mock(ShipAPI.class,(n,a)->n.equals("getVariant")?variant:body.call(n,a));
        f.entities.add(body.ship);
        ThreaxusWallSwarm reserve=new ThreaxusWallSwarm();
        reserve.applyEffectsBeforeShipCreation(ShipAPI.HullSize.FRIGATE,body.stats,"test");
        reserve.applyEffectsBeforeShipCreation(ShipAPI.HullSize.FRIGATE,body.stats,"test");
        near(dynamic.getValue(Stats.FRAGMENT_SWARM_SIZE_MOD,20f),200,"Wall reserve is200, with no stacking on repeated application");
        new FragmentSwarmHullmod().advanceInCombat(body.ship,.1f);
        RoilingSwarmEffect cloud=RoilingSwarmEffect.getSwarmFor(body.ship);
        check(cloud!=null&&cloud.getParams().baseMembersToMaintain==200,"Native Fragment Swarm sees the actual reserve");
        near(cloud.getParams().memberRespawnRate,10,"Native fragment regrowth is ten per second");
        cloud.advance(.1f);check(cloud.getNumActiveMembers()==200,"Native cloud fills all200 real fragments");
        boolean[] fire={false,false},disabled={false,false};int[] refilled={0,0};
        SwarmLauncherEffect[] effects={new SwarmLauncherEffect(),new SwarmLauncherEffect()};
        for(int i=0;i<2;i++){
            final int side=i;
            body.weapons.add(mock(WeaponAPI.class,(n,a)->{
                if(n.equals("getShip"))return body.ship;
                if(n.equals("getId"))return "swarm_launcher";
                if(n.equals("getEffectPlugin"))return effects[side];
                if(n.equals("usesAmmo"))return true;
                if(n.equals("getAmmo")||n.equals("getMaxAmmo"))return 20;
                if(n.equals("setAmmo")){refilled[side]++;return null;}
                if(n.equals("setForceFireOneFrame")){fire[side]=(Boolean)a[0];return null;}
                if(n.equals("setForceDisabled")){disabled[side]=(Boolean)a[0];return null;}
                return null;
            }));
        }
        for(int i=0;i<2;i++)effects[i].advance(1.1f,f.engine,body.weapons.get(i));
        check(fire[0]&&fire[1]&&!disabled[0]&&!disabled[1],"Both native hardpoint launchers can fire from the local reserve");
        check(refilled[0]>0&&refilled[1]>0,"Native Threat ammo refill is active");
        cloud.getMembers().subList(40,cloud.getMembers().size()).clear();
        Arrays.fill(fire,false);
        for(int i=0;i<2;i++)effects[i].advance(.1f,f.engine,body.weapons.get(i));
        check(disabled[0]&&disabled[1]&&!fire[0]&&!fire[1],"Depletion correctly disables the native launchers; no free firing");
        System.out.println("PASS: native200-fragment wall cloud,10/sec regen, two actual SwarmLauncherEffect ready/refill/depletion gates.");
    }
}
