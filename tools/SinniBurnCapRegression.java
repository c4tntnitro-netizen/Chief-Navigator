package chiefnavigator.quest;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lwjgl.util.vector.Vector2f;

/**
 * One More Horizon burn cap: hyperspace burn counts past 20 up to 30.
 * Simulates the movement module's overspeed braking between frames, the way
 * the real SmoothMovementModule removes about (acceleration + top speed) per
 * second while a fleet is above its top speed.
 */
public final class SinniBurnCapRegression {
    private static final String COMPLETE = "$chief_navigator_sinni_one_more_horizon_complete_v1";
    private static final float BASE = 20f;
    private static final float PER_BURN = 20f;
    private static final float DT = 1f / 60f;

    interface Call { Object invoke(String name, Object[] args); }

    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[] {type}, (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("hashCode")) return System.identityHashCode(proxy);
                    if (name.equals("equals")) return proxy == args[0];
                    Object result = call.invoke(name, args);
                    if (result != null) return result;
                    Class<?> r = method.getReturnType();
                    if (r == boolean.class) return false;
                    if (r == int.class) return 0;
                    if (r == float.class) return 0f;
                    if (r == long.class) return 0L;
                    if (r == double.class) return 0d;
                    return null;
                }));
    }

    private static int failures;

    static void check(boolean ok, String message) {
        if (!ok) failures++;
        System.out.println((ok ? "ok:   " : "FAIL: ") + message);
    }

    static float speedFor(float burn) {
        return BASE + burn * PER_BURN;
    }

    static final class World {
        final Map<String, Object> memory = new HashMap<>();
        final List<EveryFrameScript> scripts = new ArrayList<>();
        final StatBonus burnMod = new StatBonus();
        final Vector2f vel = new Vector2f();
        final Vector2f loc = new Vector2f();
        Vector2f dest = new Vector2f(0f, 100000f);
        float minBurn = 9f;
        float accel = 50f;
        boolean hyper = true;
        boolean slow = false;
        boolean mouse = false;
        final CampaignFleetAPI fleet;
        final SectorAPI sector;

        World() {
            memory.put(COMPLETE, true);
            MemoryAPI mem = mock(MemoryAPI.class, (n, a) -> {
                if (n.equals("getBoolean")) return Boolean.TRUE.equals(memory.get((String) a[0]));
                if (n.equals("get")) return memory.get((String) a[0]);
                return null;
            });
            MutableFleetStatsAPI stats = mock(MutableFleetStatsAPI.class, (n, a) ->
                    n.equals("getFleetwideMaxBurnMod") ? burnMod : null);
            FleetDataAPI data = mock(FleetDataAPI.class, (n, a) -> {
                if (n.equals("getMinBurnLevel")) return minBurn;
                if (n.equals("getBurnLevel")) return cappedBurn();
                return null;
            });
            fleet = mock(CampaignFleetAPI.class, (n, a) -> {
                switch (n) {
                    case "isInHyperspace": return hyper;
                    case "wasSlowMoving": return slow;
                    case "getVelocity": return vel;
                    case "setVelocity": vel.set((Float) a[0], (Float) a[1]); return null;
                    case "getLocation": return loc;
                    case "getMoveDestination":
                        return mouse ? new Vector2f(loc.x, loc.y + 500f) : dest;
                    case "getTravelSpeed": return speedFor(cappedBurn());
                    case "getAcceleration": return accel;
                    case "getStats": return stats;
                    case "getFleetData": return data;
                    default: return null;
                }
            });
            CampaignUIAPI ui = mock(CampaignUIAPI.class, (n, a) ->
                    n.equals("isPlayerFleetFollowingMouse") ? mouse : null);
            sector = mock(SectorAPI.class, (n, a) -> {
                switch (n) {
                    case "getMemoryWithoutUpdate": return mem;
                    case "getPlayerFleet": return fleet;
                    case "getCampaignUI": return ui;
                    case "addTransientScript": scripts.add((EveryFrameScript) a[0]); return null;
                    case "hasTransientScript":
                        for (EveryFrameScript s : scripts) {
                            if (((Class<?>) a[0]).isInstance(s) && !s.isDone()) return true;
                        }
                        return false;
                    default: return null;
                }
            });
            Global.setSector(sector);
        }

        float uncapped() {
            return burnMod.computeEffective(minBurn);
        }

        float cappedBurn() {
            return Math.min(20f, Math.round(uncapped()));
        }

        SinniBurnCap start() {
            SinniBurnCap.ensureRunning(fleet);
            return (SinniBurnCap) scripts.get(scripts.size() - 1);
        }

        /** One frame: our script, then the movement module's overspeed braking. */
        void frame(SinniBurnCap script) {
            script.advance(DT);
            float top = speedFor(cappedBurn());
            float speed = vel.length();
            if (speed > top) {
                float brake = Math.max(50f, accel + top) * DT;
                float next = speed - Math.min(speed - top, brake);
                vel.scale(next / speed);
            }
            loc.x += vel.x * DT;
            loc.y += vel.y * DT;
        }

        float run(SinniBurnCap script, float seconds) {
            int frames = Math.round(seconds / DT);
            for (int i = 0; i < frames; i++) frame(script);
            return vel.length();
        }
    }

    static void checkFormula() {
        World w = new World();
        w.burnMod.modifyFlat("nav", 1f);
        w.burnMod.modifyFlat("sinni", 1f);
        w.burnMod.modifyFlat("sb", 1f);
        w.burnMod.modifyPercent("sb", 100f);
        check(Math.abs(SinniBurnCap.uncappedBurn(w.fleet) - 21f) < 0.01f,
                "uncapped burn = slowest 9 + 100% of 9 + 3 flat = 21 (StatBonus: flats are added after the percent)");
        w.minBurn = 15f;
        check(Math.abs(SinniBurnCap.targetBurn(w.fleet) - 30f) < 0.01f,
                "target burn stops at 30 (uncapped " + SinniBurnCap.uncappedBurn(w.fleet) + ")");
    }

    static World cruising(float uncappedTarget) {
        World w = new World();
        w.minBurn = 9f;
        w.burnMod.modifyPercent("x", 100f);
        w.burnMod.modifyFlat("x", uncappedTarget - 18f);
        w.vel.set(0f, speedFor(20f));
        return w;
    }

    static void checkCarry() {
        World w = cruising(24f);
        SinniBurnCap s = w.start();
        float early = w.run(s, 0.5f);
        float target = speedFor(24f);
        check(early > speedFor(20f) + 5f && early < target - 20f,
                "speed ramps gradually above 20 (0.5 s: burn " + burnOf(early) + ")");
        float settled = w.run(s, 10f);
        check(Math.abs(settled - target) < target * 0.02f,
                "settles at burn 24 (" + burnOf(settled) + ")");
        float later = w.run(s, 5f);
        check(Math.abs(later - settled) < 2f, "holds steady once settled");
        check(Math.abs(w.vel.x) < 0.01f, "push stays along the heading");

        World fast = cruising(34f);
        SinniBurnCap f = fast.start();
        float top = fast.run(f, 20f);
        check(Math.abs(top - speedFor(30f)) < speedFor(30f) * 0.02f,
                "a fleet with burn 34 tops out at 30 (" + burnOf(top) + ")");
    }

    static void checkNoEffect() {
        World w = cruising(18f);
        SinniBurnCap s = w.start();
        w.vel.set(0f, speedFor(18f));
        check(Math.abs(w.run(s, 3f) - speedFor(18f)) < 0.01f, "burn 18 fleet is unchanged");

        World acc = cruising(24f);
        SinniBurnCap a = acc.start();
        acc.vel.set(0f, speedFor(20f) * 0.5f);
        a.advance(DT);
        check(Math.abs(acc.vel.length() - speedFor(20f) * 0.5f) < 0.01f,
                "no push while still accelerating to normal top speed");

        World turn = cruising(24f);
        SinniBurnCap t = turn.start();
        turn.run(t, 10f);
        turn.dest = new Vector2f(100000f, 0f);
        float after = turn.run(t, 2f);
        check(Math.abs(after - speedFor(20f)) < 1f,
                "hard turn: push stops and speed returns to burn 20 (" + burnOf(after) + ")");

        World stop = cruising(24f);
        SinniBurnCap st = stop.start();
        stop.dest = new Vector2f(0f, 500f);
        stop.run(st, 1f);
        check(stop.vel.length() <= speedFor(20f) + 0.01f, "near a destination: no push");
        stop.mouse = true;
        stop.loc.set(0f, 0f);
        stop.run(st, 3f);
        check(stop.vel.length() > speedFor(21f), "holding the mouse near the ship still pushes");

        World slow = cruising(24f);
        SinniBurnCap sl = slow.start();
        slow.slow = true;
        check(slow.run(sl, 2f) <= speedFor(20f) + 0.01f, "Go Slow: no push");
    }

    static void checkLifecycle() {
        World w = cruising(24f);
        w.memory.remove(COMPLETE);
        SinniBurnCap.ensureRunning(w.fleet);
        check(w.scripts.isEmpty(), "nothing before One More Horizon is complete");
        w.memory.put(COMPLETE, true);
        w.hyper = false;
        SinniBurnCap.ensureRunning(w.fleet);
        check(w.scripts.isEmpty(), "nothing in realspace");
        w.hyper = true;
        SinniBurnCap.ensureRunning(w.fleet);
        SinniBurnCap.ensureRunning(w.fleet);
        check(w.scripts.size() == 1, "one script in hyperspace");
        w.hyper = false;
        w.scripts.get(0).advance(DT);
        check(w.scripts.get(0).isDone(), "ends on leaving hyperspace");
        w.hyper = true;
        SinniBurnCap.ensureRunning(w.fleet);
        check(w.scripts.size() == 2, "starts again on the next hyperspace visit");
    }

    static String burnOf(float speed) {
        return String.format(java.util.Locale.US, "%.1f", (speed - BASE) / PER_BURN);
    }

    public static void main(String[] args) {
        SectorAPI prevSector = Global.getSector();
        SettingsAPI prevSettings = Global.getSettings();
        Global.setSettings(mock(SettingsAPI.class, (n, a) -> {
            if (n.equals("getColor")) return java.awt.Color.CYAN;
            if (n.equals("getBaseTravelSpeed")) return BASE;
            if (n.equals("getSpeedPerBurnLevel")) return PER_BURN;
            if (n.equals("getAngleInDegreesFast") && a != null && a.length == 2) {
                Vector2f f = (Vector2f) a[0];
                Vector2f t = (Vector2f) a[1];
                return (float) Math.toDegrees(Math.atan2(t.y - f.y, t.x - f.x));
            }
            return null;
        }));
        try {
            checkFormula();
            checkCarry();
            checkNoEffect();
            checkLifecycle();
        } finally {
            Global.setSector(prevSector);
            Global.setSettings(prevSettings);
        }
        System.out.println(failures == 0 ? "ALL PASSED" : failures + " FAILED");
        if (failures != 0) System.exit(1);
    }
}
