import chiefnavigator.weapons.OnslaughtChargePulseEffect;
import chiefnavigator.weapons.IthacaResearchUpgrades;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.BeamWeaponSpecAPI;
import java.nio.file.*;
import java.util.*;
import org.json.*;
import org.lwjgl.util.vector.Vector2f;

/** Per-gun stochastic timing, independent native bolt sizes and scoped windup damage. */
public final class OnslaughtChargePulseRegression extends WallResearchRegression {
    static final String HULL = IthacaResearchUpgrades.ITHACA_ONSLAUGHT_HULL_ID;
    static final String LASER = IthacaResearchUpgrades.ITHACA_ONSLAUGHT_LASER_ID;
    static final class Fixture {
        final OnslaughtChargePulseEffect effect = new OnslaughtChargePulseEffect();
        final List<Float> shots = new ArrayList<>();
        final List<Integer> lengths = new ArrayList<>();
        final Vector2f muzzle = new Vector2f(160, -70), velocity = new Vector2f(10, 20);
        final ShipAPI source;
        final WeaponAPI weapon;
        final CombatEngineAPI engine;
        float charge, cooldown, dps = 1000f;
        boolean paused, alive = true, phased, disabled, forced, main;
        Fixture(String hull, String parent, String gun) {
            ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class, (n,a) ->
                    n.equals("getHullId") ? hull : n.equals("getDParentHullId") ? parent : null);
            source = mock(ShipAPI.class, (n,a) -> n.equals("getHullSpec") ? hullSpec
                    : n.equals("isAlive") ? alive : n.equals("isPhased") ? phased
                    : n.equals("getVelocity") ? velocity : null);
            BeamWeaponSpecAPI spec = mock(BeamWeaponSpecAPI.class, (n,a) ->
                    n.equals("getDamagePerSecond") ? dps : null);
            weapon = mock(WeaponAPI.class, (n,a) -> n.equals("getShip") ? source
                    : n.equals("getId") ? gun : n.equals("getSpec") ? spec
                    : n.equals("getChargeLevel") ? charge : n.equals("getCooldownRemaining") ? cooldown
                    : n.equals("getFirePoint") ? muzzle : n.equals("getCurrAngle") ? 37f
                    : n.equals("isDisabled") ? disabled : n.equals("isForceDisabled") ? forced : null);
            engine = mock(CombatEngineAPI.class, (n,a) -> {
                if (n.equals("isPaused")) return paused;
                if (n.equals("spawnProjectile")) {
                    check(a[0] == source && a[1] == weapon, "Native source and weapon identity");
                    check(a.length==6,"Use initialized weapon spec, never a raw projectile override");
                    String id=(String)a[2];String prefix="chief_navigator_onslaught_charge_pulse";
                    check(id.startsWith(prefix),"Immutable native projectile variant");
                    int length=id.equals(prefix)?300:Integer.parseInt(id.substring(prefix.length()+1));
                    check(List.of(150,188,225,263,300,338,375,413,450).contains(length),
                            "Per-bolt length stays within +/-50% of doubled visible length");
                    lengths.add(length);
                    check(a[3].equals(muzzle) && a[3] != muzzle && a[5].equals(velocity)
                            && a[5] != velocity && (Float) a[4] == 37f, "Exact muzzle, aim and velocity copies");
                    shots.add(charge);
                    return mock(DamagingProjectileAPI.class, (pn,pa) -> {
                        if (pn.equals("setDamageAmount")) check(Math.abs((Float)pa[0] - dps * .1f) < .001f,
                                "Exactly 10% of live firing damage");
                        return null;
                    });
                }
                throw new AssertionError("Unexpected engine mutation: " + n);
            });
        }
        Fixture() { this(HULL, null, LASER); }
        void step(float value) { charge = value; effect.advance(1f/120f, engine, weapon, main); }
        List<Float> cycle(int hz) {
            step(0); shots.clear();
            for (int i=1; i<hz*3; i++) step(i/(hz*3f));
            return new ArrayList<>(shots);
        }
    }
    static void data() throws Exception {
        JSONObject projectile = new JSONObject(Files.readString(Path.of(
                "data/weapons/proj/chief_navigator_onslaught_charge_pulse_shot.proj")));
        check(projectile.getInt("length") == 300 && projectile.getInt("width") == 9,
                "Three times width and twice actual visible length at native 60-FPS speed floor");
        for(int length:List.of(150,188,225,263,338,375,413,450)) {
            JSONObject variant=new JSONObject(Files.readString(Path.of(
                    "data/weapons/proj/chief_navigator_onslaught_charge_pulse_shot_"+length+".proj")));
            check(variant.getInt("length")==length && variant.getInt("width")==9,
                    "All immutable variants preserve width and bounded random length");
        }
        check(projectile.getString("spawnType").equals("BALLISTIC_AS_BEAM")
                && projectile.getString("collisionClass").equals("RAY")
                && projectile.getBoolean("passThroughFighters")
                && !projectile.getBoolean("passThroughFightersOnlyWhenDestroyed"),
                "Native ray collision, shield handling and naval fighter pass-through");
        JSONObject weapon = new JSONObject(Files.readString(Path.of(
                "data/weapons/chief_navigator_onslaught_charge_pulse.wpn")));
        check(weapon.getString("projectileSpecId").equals(projectile.getString("id")), "Native spec linkage");
        Class<?> csv;
        try { csv=Class.forName("com.fs.starfarer.loading.G"); }
        catch(ClassNotFoundException rc7) { csv=Class.forName("com.fs.starfarer.loading.void"); }
        JSONArray rows=(JSONArray)csv
                .getMethod("o00000",String.class).invoke(null,Files.readString(Path.of("data/weapons/weapon_data.csv")));
        int found=0;
        for(int i=0;i<rows.length();i++) {
            JSONObject row=rows.getJSONObject(i);
            if(!weapon.getString("id").equals(row.optString("id")))continue;
            check(row.getDouble("damage/shot")==100 && row.getDouble("range")==1200
                    && row.getDouble("proj speed")==6000 && row.getString("type").equals("ENERGY"),
                    "Native CSV accepts the fast 100-damage pulse");found++;
        }
        check(found==1,"Exactly one pulse weapon registration");
    }
    public static void main(String[] args) throws Exception {
        data();
        for(int hz:new int[]{120,60,10}) {
            Fixture left=new Fixture(),right=new Fixture();
            List<Float> first=left.cycle(hz),second=right.cycle(hz);
            check(first.size()==11 && second.size()==11,"All pulses at " + hz + " Hz");
            check(new HashSet<>(left.lengths).size()>1,"Lengths vary between individual pulses");
            if(hz>=60) {
                check(!first.equals(second),"Simultaneous barrels roll independent cadence");
                check(!first.equals(left.cycle(hz)),"Cadence rerolls on every new charge");
            }
            for(int i=1;i<first.size();i++) check(first.get(i)>=first.get(i-1),"No reversed pulse order");
            check(first.get(10)<.99f && first.get(0)>.01f,"Windup-only timing bounds");
            check((first.get(3)-first.get(0))/3f > (first.get(10)-first.get(7))/3f,
                    "Final stutter stays denser than the opening");
        }
        Fixture f=new Fixture();f.step(.4f);int count=f.shots.size();
        f.paused=true;f.step(.9f);check(f.shots.size()==count,"Pause creates no pulses");
        f.paused=false;f.step(.3f);f.step(.8f);check(f.shots.size()==count,"Cancelled charge never replays");
        f.step(0);f.cooldown=20;f.step(.8f);check(f.shots.size()==count,"No cooldown pulses");
        f.cooldown=0;f.main=true;f.step(.8f);check(f.shots.size()==count,"No overlap with main beam");
        for(int state=0;state<4;state++) {
            Fixture blocked=new Fixture();blocked.alive=state!=0;blocked.phased=state==1;
            blocked.disabled=state==2;blocked.forced=state==3;
            blocked.cycle(60);check(blocked.shots.isEmpty(),"Invalid source/weapon suppresses pulses");
        }
        for(String hull:new String[]{"onslaught","chief_navigator_ithaca_intact_base"})
            check(new Fixture(hull,null,LASER).cycle(60).isEmpty(),"No foreign hull pulses");
        check(new Fixture(HULL,null,"tpc").cycle(60).isEmpty(),"No stock-weapon pulses");
        Fixture damaged=new Fixture(HULL+"_d",HULL,LASER);damaged.dps=2000;
        check(damaged.cycle(60).size()==11,"Exact D-parent eligibility and live damage scaling");
        check(new Fixture(IthacaResearchUpgrades.ITHACA_ODYSSEY_HULL_ID,null,
                IthacaResearchUpgrades.ITHACA_ODYSSEY_LASER_ID).cycle(60).size()==11,
                "Odyssey follows the same safe nine-length native spawn path");
        System.out.println("PASS: native pulse specs; width9, visible length300 +/-50%; independent rerolled cadence/length; "
                + "120/60/10-Hz windups; 10% damage; pause/cancellation/cooldown/beam/source gates.");
    }
}
