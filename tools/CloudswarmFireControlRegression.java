import chiefnavigator.weapons.IthacaLocustPDEffect;
import chiefnavigator.weapons.IthacaResearchUpgrades;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.ProjectileWeaponSpecAPI;
import java.awt.Color;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;

/**
 * Exercises the production Cloudswarm public callbacks, not a duplicate AI.
 * The engine proxy supplies entity identity/lifecycle and records firing API
 * requests. It does not simulate native burst timing, collision, or autofire.
 */
public final class CloudswarmFireControlRegression {
    private interface Call { Object invoke(String name, Object[] args); }

    private static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[] {type}, (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("hashCode")) return System.identityHashCode(proxy);
                    if (name.equals("equals")) return proxy == args[0];
                    if (name.equals("toString")) return type.getSimpleName();
                    Object result = call.invoke(name, args);
                    if (result != null) return result;
                    Class<?> returns = method.getReturnType();
                    if (returns == boolean.class) return false;
                    if (returns == float.class) return 0f;
                    if (returns == double.class) return 0d;
                    if (returns == int.class) return 0;
                    if (returns == long.class) return 0L;
                    return null;
                }));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Body {
        final Vector2f location;
        final Vector2f velocity = new Vector2f();
        int owner = 1;
        boolean inPlay = true;
        boolean alive = true;
        boolean hulk;
        boolean phased;
        boolean fighter;
        boolean drone;
        boolean fading;
        boolean fizzling;
        float facing = 180f;
        int flameOutCalls;
        MissileAIPlugin ai;
        final CombatEntityAPI api;

        Body(boolean missile, float x, float y) {
            location = new Vector2f(x, y);
            if (missile) {
                api = mock(MissileAPI.class, this::call);
            } else {
                api = mock(ShipAPI.class, this::call);
            }
        }

        Object call(String name, Object[] args) {
            switch (name) {
                case "getOwner": return owner;
                case "getLocation": return location;
                case "getVelocity": return velocity;
                case "isAlive": return alive;
                case "isHulk": return hulk;
                case "isPhased": return phased;
                case "isFighter": return fighter;
                case "isDrone": return drone;
                case "isFading": return fading;
                case "isFizzling": return fizzling;
                case "getFacing": return facing;
                case "setFacing": facing = (Float) args[0]; break;
                case "setMissileAI": ai = (MissileAIPlugin) args[0]; break;
                case "getMissileAI": return ai;
                case "flameOut": flameOutCalls++; fizzling = true; break;
                case "getCollisionRadius": return 8f;
            }
            return null;
        }
    }

    private static final class Fixture {
        final List<Body> bodies = new ArrayList<>();
        final Vector2f location = new Vector2f();
        final Vector2f shipLocation = new Vector2f();
        final Map<String, Object> custom = new HashMap<>();
        final IthacaLocustPDEffect effect = new IthacaLocustPDEffect();
        float refire = 16f;
        float nativeRefire = 16f;
        int nativeRefireWrites;
        float ammoRate = 1.28125f;
        float trackerAmmoRate = 1.28125f;
        float reloadSize = 41f;
        float specReloadSize = 41f;
        int maximum = 41;
        int specMaximum = 41;
        int ammo = 41;
        int burst = 81;
        int clones;
        int missileScans;
        int shipScans;
        int stopCalls;
        int cooldownWrites;
        int forceFireCalls;
        int disableClears;
        int removed;
        boolean paused;
        boolean forcedDisabled;
        boolean noFire;
        boolean firing;
        boolean inBurst;
        float cooldownRemaining;
        float currAngle;
        float shipFacing = 90f;
        float arcFacing = 180f;
        float range = 1200f;
        final ShipAPI ship;
        final WeaponAPI weapon;
        final CombatEngineAPI engine;

        Fixture(int mask) {
            custom.put("chief_navigator_ithaca_research_component_mask", mask);
            ShipHullSpecAPI hull = mock(ShipHullSpecAPI.class,
                    (name, args) -> name.equals("getHullId")
                            ? "chief_navigator_ithaca_intact_base" : null);
            ship = mock(ShipAPI.class, (name, args) -> {
                switch (name) {
                    case "getHullSpec": return hull;
                    case "getCustomData": return custom;
                    case "getOwner": return 0;
                    case "getLocation": return shipLocation;
                    case "getVelocity": return new Vector2f();
                    case "getFacing": return shipFacing;
                    case "isAlive": return true;
                    case "getCollisionRadius": return 3100f;
                }
                return null;
            });
            ProjectileWeaponSpecAPI spec = mock(ProjectileWeaponSpecAPI.class,
                    (name, args) -> {
                switch (name) {
                    case "getRefireDelay": return refire;
                    case "setRefireDelay": refire = (Float) args[0]; break;
                    case "getAmmoPerSecond": return ammoRate;
                    case "setAmmoPerSecond": ammoRate = (Float) args[0]; break;
                    case "getBurstSize": return burst;
                    case "setBurstSize": burst = (Integer) args[0]; break;
                    case "getMaxAmmo": return specMaximum;
                    case "setMaxAmmo": specMaximum = (Integer) args[0]; break;
                    case "getReloadSize": return specReloadSize;
                    case "setReloadSize": specReloadSize = (Float) args[0]; break;
                }
                return null;
            });
            AmmoTrackerAPI tracker = mock(AmmoTrackerAPI.class,
                    (name, args) -> {
                switch (name) {
                    case "getAmmo": return ammo;
                    case "setAmmo": ammo = (Integer) args[0]; break;
                    case "getMaxAmmo": return maximum;
                    case "setMaxAmmo": maximum = (Integer) args[0]; break;
                    case "getAmmoPerSecond": return trackerAmmoRate;
                    case "setAmmoPerSecond": trackerAmmoRate = (Float) args[0]; break;
                    case "getReloadSize": return reloadSize;
                    case "setReloadSize": reloadSize = (Float) args[0]; break;
                }
                return null;
            });
            weapon = mock(WeaponAPI.class, (name, args) -> {
                switch (name) {
                    case "getShip": return ship;
                    case "getLocation": return location;
                    case "getRange": return range;
                    case "getArc": return 360f;
                    case "getArcFacing": return arcFacing;
                    case "getCurrAngle": return currAngle;
                    case "setCurrAngle": currAngle = (Float) args[0]; break;
                    case "getSpec": return spec;
                    case "setRefireDelay":
                        nativeRefire = (Float) args[0];
                        nativeRefireWrites++;
                        break;
                    case "getAmmoTracker": return tracker;
                    case "ensureClonedSpec": clones++; break;
                    case "isForceDisabled": return forcedDisabled;
                    case "isDisabled": return forcedDisabled;
                    case "setForceDisabled":
                        forcedDisabled = (Boolean) args[0];
                        if (!forcedDisabled) disableClears++;
                        break;
                    case "setForceNoFireOneFrame": noFire = (Boolean) args[0]; break;
                    case "setForceFireOneFrame": forceFireCalls++; break;
                    case "stopFiring": stopCalls++; firing = false; inBurst = false; break;
                    case "isFiring": return firing;
                    case "isInBurst": return inBurst;
                    case "getCooldownRemaining": return cooldownRemaining;
                    case "setRemainingCooldownTo":
                        cooldownRemaining = (Float) args[0]; cooldownWrites++; break;
                }
                return null;
            });
            engine = mock(CombatEngineAPI.class, (name, args) -> {
                switch (name) {
                    case "isPaused": return paused;
                    case "getMissiles": {
                        missileScans++;
                        List<MissileAPI> found = new ArrayList<>();
                        for (Body body : bodies) {
                            if (body.api instanceof MissileAPI) {
                                found.add((MissileAPI) body.api);
                            }
                        }
                        return found;
                    }
                    case "getShips": {
                        shipScans++;
                        List<ShipAPI> found = new ArrayList<>();
                        for (Body body : bodies) {
                            if (body.api instanceof ShipAPI) found.add((ShipAPI) body.api);
                        }
                        return found;
                    }
                    case "isEntityInPlay":
                        for (Body body : bodies) {
                            if (body.api == args[0]) return body.inPlay;
                        }
                        return args[0] == ship;
                    case "removeEntity": removed++; break;
                }
                return null;
            });
        }

        Body add(boolean missile, float x, float y) {
            Body body = new Body(missile, x, y);
            bodies.add(body);
            return body;
        }

        void advance(float amount) {
            // Native one-frame flags expire between frames; the fixture must
            // not accidentally retain a previous no-fire request forever.
            noFire = false;
            effect.advance(amount, engine, weapon);
        }

        Body fire() {
            Body outgoing = add(true, location.x, location.y);
            outgoing.owner = 0;
            effect.onFire((MissileAPI) outgoing.api, weapon, engine);
            return outgoing;
        }
    }

    private static final int INTERCEPT = IthacaResearchUpgrades.INTERCEPT_MATRIX;

    public static void main(String[] args) throws Exception {
        Global.setSettings(mock(SettingsAPI.class,
                (name, callArgs) -> name.equals("getColor") ? Color.WHITE : null));
        testCadenceAndResearchLock();
        testClosedTargetSet();
        testWeaponLocalRange();
        testAbortAndBoundedAcquisition();
        testTargetlessCooldownDoesNotRestart();
        testSpawnGuardAndGuidance();
        testAirborneAcquisitionRange();
        testAuthoredData();
        System.out.println("Cloudswarm fire-control regression passed: closed PD "
                + "target set, empty-space burst abort, bounded reacquisition, "
                + "41/81-round salvos, 16/6.4s refire, 32/12.8s reload, "
                + "1200-unit launch/1300-unit reacquisition range, "
                + "and cold-ejection steering.");
    }

    private static void testCadenceAndResearchLock() {
        for (int mask : new int[] {INTERCEPT,
                INTERCEPT | IthacaResearchUpgrades.AUTOLOADER_CORE}) {
            Fixture fixture = new Fixture(mask);
            fixture.add(true, 500f, 0f);
            fixture.advance(.1f);
            fixture.advance(.1f);
            boolean autoloader = (mask & IthacaResearchUpgrades.AUTOLOADER_CORE) != 0;
            check(Math.abs(fixture.refire - (autoloader ? 6.4f : 16f)) < .001f,
                    "Cloudswarm refire must retain the longer base cycle");
            check(Math.abs(fixture.nativeRefire - fixture.refire) < .001f
                            && fixture.nativeRefireWrites == (autoloader ? 1 : 0),
                    "Research must update the native refire tracker once, not just the tooltip spec");
            check(Math.abs(fixture.maximum / fixture.trackerAmmoRate
                    - (autoloader ? 12.8f : 32f)) < .001f
                            && Math.abs(fixture.ammoRate - fixture.trackerAmmoRate) < .001f,
                    "Full salvo reload must be 32s base, 12.8s upgraded");
            check(fixture.burst == (autoloader ? 81 : 41)
                            && fixture.reloadSize == fixture.burst
                            && fixture.specReloadSize == fixture.reloadSize
                            && fixture.specMaximum == fixture.maximum
                            && fixture.ammo == fixture.burst,
                    "Magazine/burst/reload upgrade must remain consistent");
            check(fixture.clones == 1,
                    "Actual salvo display uses a per-weapon spec exactly once");
            check(fixture.forceFireCalls == 0 && fixture.disableClears == 0,
                    "PD gate may neither force firing nor clear external disables");
            check(Math.abs(fixture.currAngle - 270f) < .001f,
                    "Fixed launcher must convert hull-relative arc to world facing");
        }
        Fixture locked = new Fixture(0);
        locked.add(true, 500f, 0f);
        locked.advance(.1f);
        check(locked.forcedDisabled, "Research-locked array must remain disabled");
        Body removed = locked.fire();
        check(locked.removed == 1 && removed.ai == null,
                "Locked on-fire callback must remove its projectile");
        Fixture external = new Fixture(INTERCEPT);
        external.forcedDisabled = true;
        external.add(true, 500f, 0f);
        external.advance(.1f);
        check(external.forcedDisabled && external.disableClears == 0,
                "Live reactor/external force-disable must never be cleared");
        Fixture wrapped = new Fixture(INTERCEPT);
        wrapped.shipFacing = -90f;
        wrapped.arcFacing = 10f;
        wrapped.advance(.1f);
        check(Math.abs(wrapped.currAngle - 280f) < .001f,
                "Negative fixed-launcher world facing must normalize correctly");
    }

    private static void testWeaponLocalRange() {
        Fixture edgeLauncher = new Fixture(INTERCEPT);
        edgeLauncher.location.x = 2000f;
        edgeLauncher.add(true, edgeLauncher.location.x + edgeLauncher.range, 0f);
        edgeLauncher.advance(.1f);
        check(!edgeLauncher.noFire,
                "Acquisition must measure from the launcher, not the giant foundation center");
        Fixture farLauncher = new Fixture(INTERCEPT);
        farLauncher.location.x = 5000f;
        farLauncher.add(true, 500f, 0f);
        farLauncher.advance(.1f);
        check(farLauncher.noFire,
                "A center-near target outside this launcher's range must not launch a salvo");
    }

    private static void testClosedTargetSet() {
        for (String kind : new String[] {"empty", "allyMissile", "neutralMissile",
                "fadingMissile", "fizzlingMissile", "absentMissile", "farMissile",
                "ordinaryShip", "allyFighter", "neutralDrone", "deadFighter",
                "hulkDrone", "phaseFighter", "absentFighter", "farDrone"}) {
            Fixture fixture = new Fixture(INTERCEPT);
            if (!kind.equals("empty")) {
                boolean missile = kind.contains("Missile");
                Body body = fixture.add(missile, 500f, 0f);
                body.fighter = kind.contains("Fighter");
                body.drone = kind.contains("Drone");
                if (kind.startsWith("ally")) body.owner = 0;
                if (kind.startsWith("neutral")) body.owner = 100;
                if (kind.startsWith("dead")) body.alive = false;
                if (kind.startsWith("hulk")) body.hulk = true;
                if (kind.startsWith("phase")) body.phased = true;
                if (kind.startsWith("fading")) body.fading = true;
                if (kind.startsWith("fizzling")) body.fizzling = true;
                if (kind.startsWith("absent")) body.inPlay = false;
                if (kind.startsWith("far")) body.location.x = fixture.range + 1f;
            }
            fixture.firing = true;
            fixture.inBurst = true;
            fixture.advance(.1f);
            check(fixture.noFire && fixture.stopCalls > 0 && !fixture.firing,
                    "Invalid target must abort launch/burst: " + kind);
            Body outgoing = fixture.fire();
            check(fixture.removed == 1 && outgoing.ai == null,
                    "Invalid target must fail closed at spawn: " + kind);
        }
        for (String kind : new String[] {"missile", "fighter", "drone"}) {
            Fixture fixture = new Fixture(INTERCEPT);
            Body valid = fixture.add(kind.equals("missile"), fixture.range, 0f);
            valid.fighter = kind.equals("fighter");
            valid.drone = kind.equals("drone");
            fixture.advance(.1f);
            check(!fixture.noFire, "Eligible boundary target must permit native firing: " + kind);
            Body outgoing = fixture.fire();
            check(outgoing.ai instanceof GuidedMissileAI && fixture.removed == 0,
                    "Eligible target must install real guidance: " + kind);
            check(((GuidedMissileAI) outgoing.ai).getTarget() == valid.api,
                    "Guidance must acquire eligible boundary target: " + kind);
        }
    }

    private static void testAbortAndBoundedAcquisition() {
        Fixture fixture = new Fixture(INTERCEPT);
        fixture.advance(.01f);
        for (int i = 0; i < 10; i++) fixture.advance(.01f);
        check(fixture.noFire && fixture.missileScans <= 2 && fixture.shipScans <= 2,
                "Empty-space gate must not scan all combat entities every frame");
        Body incoming = fixture.add(true, 400f, 0f);
        fixture.advance(.25f);
        check(!fixture.noFire, "A new target must unlock native fire after bounded acquisition");
        fixture.firing = true;
        fixture.inBurst = true;
        incoming.inPlay = false;
        fixture.advance(.01f);
        check(fixture.noFire && !fixture.firing,
                "Losing a target must interrupt an already-running salvo");
        incoming.inPlay = true;
        fixture.advance(.25f);
        check(!fixture.noFire && fixture.forceFireCalls == 0,
                "Target return permits, but does not force, another salvo");
    }

    private static void testTargetlessCooldownDoesNotRestart() {
        Fixture cooling = new Fixture(INTERCEPT);
        cooling.firing = true; // Native CHARGING_DOWN still reports firing.
        cooling.inBurst = false;
        cooling.cooldownRemaining = 2.75f;
        for (int i = 0; i < 50; i++) {
            cooling.advance(.01f);
            check(cooling.noFire,
                    "Targetless cooling launcher must still request hold-fire");
            check(cooling.stopCalls == 0 && cooling.cooldownWrites == 0
                            && Math.abs(cooling.cooldownRemaining - 2.75f) < .001f,
                    "Charging-down no-target frames must not restart native cooldown");
        }
        Body stray = cooling.fire();
        check(cooling.removed == 1 && stray.ai == null,
                "A targetless cooling-state spawn must still fail closed");
        check(cooling.stopCalls == 0 && cooling.cooldownWrites == 0
                        && Math.abs(cooling.cooldownRemaining - 2.75f) < .001f,
                "Targetless spawn guard must not restart an existing cooldown");
    }

    private static void testSpawnGuardAndGuidance() {
        Fixture fixture = new Fixture(INTERCEPT);
        Body incoming = fixture.add(true, 900f, 150f);
        Body fighter = fixture.add(false, 400f, 0f);
        fighter.fighter = true;
        fixture.advance(.1f);
        incoming.inPlay = false;
        fighter.inPlay = false;
        Body stale = fixture.fire();
        check(fixture.removed == 1 && stale.ai == null,
                "Fresh spawn guard must reject stale launcher acquisition");

        Fixture guided = new Fixture(INTERCEPT);
        Body priority = guided.add(true, 900f, 150f);
        Body fallback = guided.add(false, 400f, 0f);
        fallback.fighter = true;
        Body outgoing = guided.fire();
        GuidedMissileAI ai = (GuidedMissileAI) outgoing.ai;
        check(ai.getTarget() == priority.api,
                "Hostile missiles take priority over nearer fighters");
        outgoing.ai.advance(.1f);
        check(Math.abs(outgoing.facing - 180f) < .001f
                        && Math.abs(outgoing.velocity.length() - 120f) < .01f,
                "Cold ejection must keep tube angle and authored launch speed");
        for (int i = 0; i < 30; i++) {
            outgoing.ai.advance(.05f);
            outgoing.location.x += outgoing.velocity.x * .05f;
            outgoing.location.y += outgoing.velocity.y * .05f;
        }
        check(outgoing.velocity.x > 0f && Math.abs(outgoing.velocity.length() - 520f) < .01f,
                "Guided missiles must turn toward the right-hand threat, not continue left");
        priority.inPlay = false;
        outgoing.ai.advance(.25f);
        check(ai.getTarget() == fallback.api,
                "Guidance must reacquire an eligible fighter after missile destruction");
        ShipAPI forbidden = (ShipAPI) guided.add(false, 200f, 0f).api;
        ai.setTarget(forbidden);
        check(ai.getTarget() == null, "External targeting cannot open the ordinary-ship target set");
        fallback.inPlay = false;
        int scans = guided.missileScans;
        for (int i = 0; i < 10; i++) outgoing.ai.advance(.01f);
        check(outgoing.flameOutCalls == 1 && ai.getTarget() == null
                        && guided.missileScans - scans <= 1,
                "No eligible fallback must flame out once, not drift or repeatedly rescan");
    }

    private static void testAirborneAcquisitionRange() {
        for (String kind : new String[] {"missile", "fighter", "drone"}) {
            Fixture fixture = new Fixture(INTERCEPT);
            Body initial = fixture.add(true, 500f, 0f);
            Body outgoing = fixture.fire();
            GuidedMissileAI ai = (GuidedMissileAI) outgoing.ai;
            outgoing.location.set(2000f, 150f);
            initial.inPlay = false;
            Body boundary = fixture.add(kind.equals("missile"), 3300f, 150f);
            boundary.fighter = kind.equals("fighter");
            boundary.drone = kind.equals("drone");
            ai.setTarget(boundary.api);
            check(ai.getTarget() == boundary.api,
                    "Airborne target at 1300 units must be valid from the missile: " + kind);
            boundary.location.x = 3301f;
            ai.setTarget(boundary.api);
            check(ai.getTarget() == null,
                    "External targeting must reject airborne targets beyond 1300 units: " + kind);

            boundary.location.x = 3300f;
            outgoing.ai.advance(.2f);
            check(ai.getTarget() == boundary.api && outgoing.flameOutCalls == 0,
                    "Actual guidance must reacquire the closed 1300-unit boundary: " + kind);
            boundary.location.x = 3301f;
            outgoing.ai.advance(.01f);
            check(ai.getTarget() == null && outgoing.flameOutCalls == 1,
                    "Actual guidance must lose a target one unit beyond its halved range: " + kind);
        }
    }

    private static void testAuthoredData() throws Exception {
        JSONObject spec = new JSONObject(Files.readString(Path.of(
                "data/weapons/chief_navigator_ithaca_locust_pd_array.wpn"),
                StandardCharsets.UTF_8));
        check(spec.getBoolean("interruptibleBurst"),
                "Cloudswarm salvos must be authored as interruptible");
        check(spec.getString("projectileSpecId").equals("locust"),
                "Keep stock Locust projectile and the existing ejection fan");
        String[] lines = Files.readString(Path.of("data/weapons/weapon_data.csv"),
                StandardCharsets.UTF_8).split("\\R");
        List<String> header = parseCsv(lines[0]);
        List<String> row = null;
        for (String line : lines) {
            List<String> parsed = parseCsv(line);
            if (parsed.size() > 1
                    && parsed.get(1).equals("chief_navigator_ithaca_locust_pd_array")) {
                row = parsed;
                break;
            }
        }
        check(row != null, "Cloudswarm weapon_data row must exist");
        check(Float.parseFloat(row.get(header.indexOf("range"))) == 1200f,
                "Authored Cloudswarm launcher range must be halved to 1200");
        check(Math.abs(Float.parseFloat(row.get(header.indexOf("flight time"))) - 3.63f) < .001f,
                "Cloudswarm flight time must halve travel while retaining its cold-ejection phases");
        check(Math.abs(Float.parseFloat(row.get(header.indexOf("chargedown"))) - 16f) < .001f,
                "Authored Cloudswarm refire must be 16 seconds");
        check(Math.abs(Float.parseFloat(row.get(header.indexOf("ammo/sec"))) - 1.28125f) < .0001f,
                "Authored Cloudswarm reload must be 41 rounds in 32 seconds");
        for (String column : new String[] {"ammo", "reload size"}) {
            check(Float.parseFloat(row.get(header.indexOf(column))) == 41f,
                    "Base Cloudswarm magazine/reload must use 41: " + column);
        }
        check(Float.parseFloat(row.get(header.indexOf("burst size"))) == 81f,
                "Native burst ceiling must support the upgraded 81-round magazine");
        String tooltip = row.get(header.indexOf("customAncillary"));
        check(tooltip.contains("41") && tooltip.contains("81")
                        && !tooltip.contains("54") && !tooltip.contains("108"),
                "Cloudswarm tooltip must describe current 41/81-round salvos");
    }

    private static List<String> parseCsv(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"'); i++;
                } else quoted = !quoted;
            } else if (c == ',' && !quoted) {
                fields.add(field.toString()); field.setLength(0);
            } else field.append(c);
        }
        fields.add(field.toString());
        return fields;
    }
}
