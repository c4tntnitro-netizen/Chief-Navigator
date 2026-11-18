import chiefnavigator.hullmods.ThreaxusWallSwarm;
import chiefnavigator.weapons.ThreaxusSwarmLauncherEffect;
import com.fs.starfarer.api.*;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.threat.*;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import java.awt.Color;
import java.lang.reflect.*;
import java.util.*;
import org.dark.graphics.plugins.WeaponDamageSmoke;
import org.json.JSONObject;

/** Runs the actual GraphicsLib smoke plugin against actual native launcher gates. */
public final class ThreaxusLauncherSmokeRegression extends ExodusPlagueRegression {
    static class Mount {
        boolean disabled, fire, noFire, damaged;
        final WeaponAPI weapon;
        Mount(ShipAPI ship, SwarmLauncherEffect effect, String id) {
            weapon=mock(WeaponAPI.class,(n,a)->{
                switch(n) {
                    case "getShip":return ship;
                    case "getId":return id;
                    case "getEffectPlugin":return effect;
                    case "getSize":return WeaponAPI.WeaponSize.MEDIUM;
                    case "getLocation":return ship.getLocation();
                    case "usesAmmo":return true;
                    case "getAmmo":case "getMaxAmmo":return 30;
                    case "isDisabled":return disabled||damaged;
                    case "isForceDisabled":return disabled;
                    case "setForceDisabled":disabled=(Boolean)a[0];break;
                    case "setForceFireOneFrame":fire=(Boolean)a[0];break;
                    case "setForceNoFireOneFrame":noFire=(Boolean)a[0];break;
                }
                return null;
            });
        }
        void consumeFrameFlags() {fire=false;noFire=false;}
    }
    public static void main(String[] args) {
        try {run();} catch(Exception error) {throw new RuntimeException(error);}
    }
    static void run()throws Exception {
        Fixture f=new Fixture();
        int[] smoke={0};
        CombatEngineAPI engine=(CombatEngineAPI)Proxy.newProxyInstance(CombatEngineAPI.class.getClassLoader(),
                new Class<?>[]{CombatEngineAPI.class},(p,m,a)->{
                    if(m.getName().equals("addSmokeParticle")){smoke[0]++;return null;}
                    return m.invoke(f.engine,a);
                });
        Global.setCombatEngine(engine);
        JSONObject smokeSettings=new JSONObject().put("enableWeaponSmoke",true).put("drawOffscreenParticles",true);
        Global.setSettings(mock(SettingsAPI.class,(n,a)->{
            if(n.equals("loadJSON"))return smokeSettings;
            if(n.equals("getColor"))return Color.WHITE;
            if(n.equals("getSprite"))return mock(SpriteAPI.class,(x,y)->null);
            return null;
        }));
        var body=new ThreatxusFabricationRegression.Ship(ThreaxusWallSwarm.HULL_ID,ShipAPI.HullSize.FRIGATE,1);
        DynamicStatsAPI dynamic=mock(DynamicStatsAPI.class,(n,a)->n.equals("getValue")?(a.length==2?a[1]:1f):null);
        ShipVariantAPI variant=mock(ShipVariantAPI.class,(n,a)->n.equals("hasHullMod")?true:null);
        body.stats=mock(MutableShipStatsAPI.class,(n,a)->{
            if(n.equals("getDynamic"))return dynamic;
            if(n.equals("getTimeMult"))return new MutableStat(1f);
            if(n.equals("getZeroFluxSpeedBoost"))return new MutableStat(0f);
            return null;
        });
        body.ship=mock(ShipAPI.class,(n,a)->n.equals("getVariant")?variant:body.call(n,a));
        f.entities.add(body.ship);
        RoilingSwarmEffect.RoilingSwarmParams params=new RoilingSwarmEffect.RoilingSwarmParams();
        params.initialMembers=200;params.baseMembersToMaintain=200;
        RoilingSwarmEffect cloud=new RoilingSwarmEffect(body.ship,params);cloud.advance(.1f);
        check(cloud.getNumActiveMembers()==200,"Real native reserve starts full");
        List<ShipAPI> wingMembers=new ArrayList<>(Collections.nCopies(4,body.ship));
        FighterWingAPI wing=mock(FighterWingAPI.class,(n,a)->n.equals("getWingMembers")?wingMembers:null);
        Field currWing=SwarmLauncherEffect.class.getDeclaredField("currWing");currWing.setAccessible(true);

        SwarmLauncherEffect stock=new SwarmLauncherEffect();currWing.set(stock,wing);
        Mount baseline=new Mount(body.ship,stock,"swarm_launcher");body.weapons.add(baseline.weapon);
        stock.advance(1.1f,engine,baseline.weapon);
        check(baseline.disabled&&!baseline.fire,"Stock launcher disables itself at four swarms despite a full reserve");
        WeaponDamageSmoke graphics=new WeaponDamageSmoke();graphics.init(engine);graphics.advance(.11f,List.of());
        check(smoke[0]==2,"Actual GraphicsLib reproduces two false damage-smoke particles");

        ThreaxusSwarmLauncherEffect fixed=new ThreaxusSwarmLauncherEffect();currWing.set(fixed,wing);
        Mount wall=new Mount(body.ship,fixed,ThreaxusSwarmLauncherEffect.WEAPON_ID);
        fixed.init(wall.weapon);
        check(((Map<?,?>)f.data.get("SL_wpnTexOvd")).get(wall.weapon).equals("swarm_launcher"),
                "Wall registration retains GraphicsLib's actual native texture mapping");
        body.weapons.clear();body.weapons.add(wall.weapon);
        fixed.advance(1.1f,engine,wall.weapon);
        check(wall.disabled&&!wall.fire,"Native force-disabled gate remains set DURING firing operations");
        fixed.advanceAfter(.1f,engine,wall.weapon);
        check(!wall.disabled&&wall.noFire&&!wall.fire,"Post-advance converts only scripted waiting into no-fire");
        graphics.advance(.11f,List.of());check(smoke[0]==2,"Actual GraphicsLib emits no healthy-launcher smoke");
        for(int i=0;i<10;i++) {
            wall.consumeFrameFlags();fixed.advance(.1f,engine,wall.weapon);
            check(wall.disabled&&!wall.fire,"Four-swarm cap still blocks every native firing step");
            fixed.advanceAfter(.1f,engine,wall.weapon);graphics.advance(.11f,List.of());
        }
        check(smoke[0]==2,"Repeated wing-cap waiting produces no fog");
        wall.damaged=true;graphics.advance(.11f,List.of());check(smoke[0]==4,"Real damage still emits ordinary GraphicsLib smoke");wall.damaged=false;
        wingMembers.subList(1,wingMembers.size()).clear();wall.consumeFrameFlags();
        fixed.advance(.1f,engine,wall.weapon);fixed.advanceAfter(.1f,engine,wall.weapon);
        check(wall.fire&&!wall.noFire&&!wall.disabled,"Native launcher becomes ready again when the wing is depleted");
        cloud.getMembers().subList(40,cloud.getMembers().size()).clear();wall.consumeFrameFlags();
        fixed.advance(.1f,engine,wall.weapon);check(wall.disabled&&!wall.fire,"Insufficient fragments still block native firing");
        fixed.advanceAfter(.1f,engine,wall.weapon);check(wall.noFire&&!wall.disabled,"Low-fragment waiting also stays smoke-free");
        Mount startup=new Mount(body.ship,new ThreaxusSwarmLauncherEffect(),ThreaxusSwarmLauncherEffect.WEAPON_ID);
        var startupEffect=(ThreaxusSwarmLauncherEffect)startup.weapon.getEffectPlugin();
        startupEffect.advance(.1f,engine,startup.weapon);check(startup.disabled,"Native startup delay is retained");
        startupEffect.advanceAfter(.1f,engine,startup.weapon);check(startup.noFire&&!startup.disabled,"Startup delay does not appear damaged");
        var foreign=new ThreatxusFabricationRegression.Ship("fabricator_unit",ShipAPI.HullSize.CAPITAL_SHIP,1);
        Mount other=new Mount(foreign.ship,fixed,ThreaxusSwarmLauncherEffect.WEAPON_ID);other.disabled=true;
        fixed.advanceAfter(.1f,engine,other.weapon);check(other.disabled,"Foreign hulls are untouched");
        System.out.println("PASS: actual GraphicsLib reproduces healthy native launcher fog; wall-only post-advance fix preserves cap, startup, fragment and real-damage gates.");
    }
}
