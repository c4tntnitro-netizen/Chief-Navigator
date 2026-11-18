import chiefnavigator.weapons.DriftingWallBeamEffect;
import chiefnavigator.weapons.IthacaResearchUpgrades;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.BeamWeaponSpecAPI;
import java.awt.Color;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import org.json.*;
import org.lwjgl.util.vector.Vector2f;
import sun.misc.Unsafe;

/** Real native beam wrapper and burst tracker; internal setup is test-only. */
public final class NativeNavalCyclerRegression extends WallResearchRegression {
    static final String HULL = IthacaResearchUpgrades.ITHACA_ONSLAUGHT_HULL_ID;
    static final String LASER = IthacaResearchUpgrades.ITHACA_ONSLAUGHT_LASER_ID;
    static float shipCooldown, shipFlux;
    static final Unsafe UNSAFE;
    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (Exception failure) { throw new ExceptionInInitializerError(failure); }
    }

    static final class Gun {
        final List<Float> starts = new ArrayList<>(), ends = new ArrayList<>();
        final List<Float> chargePulses = new ArrayList<>();
        final Object tracker;
        final Method advance;
        final WeaponAPI nativeWeapon, weapon;
        final DriftingWallBeamEffect effect = new DriftingWallBeamEffect();
        final CombatEngineAPI engine;
        final float step;
        float time;
        float nativeCooldownRate = 1f;
        float tailDuration, visibleUntil = Float.NEGATIVE_INFINITY;
        boolean hideWeaponBeams, foreignBeamVisible;
        final List<Float> cooldownStarts = new ArrayList<>(), cooldownValues = new ArrayList<>();
        final List<Float> cooldownBarStarts = new ArrayList<>(), cooldownReady = new ArrayList<>();
        final List<Float> cooldownMaxBarDrop = new ArrayList<>();
        boolean measuringCooldown;
        float lastBar;
        boolean beam, paused, trigger = true;
        float firstTimerWrite = Float.NaN, firstCooldown = Float.NaN;
        int writes;

        Gun(float cooldown, int mask, float step, boolean useCampaignOverride) throws Exception {
            this(cooldown, mask, step, useCampaignOverride, HULL, LASER);
        }

        Gun(float cooldown, int mask, float step, boolean useCampaignOverride, String hullId, String weaponId) throws Exception {
            this.step = step;
            Class<?> trackerClass = Class.forName("com.fs.starfarer.combat.entities.ship.trackers.String");
            Class<?> callbacks = Class.forName("com.fs.starfarer.combat.entities.ship.trackers.String$o");
            Object callback = Proxy.newProxyInstance(callbacks.getClassLoader(), new Class<?>[] { callbacks },
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "createBeam": if (!beam) starts.add(time); beam = true; return null;
                            case "destroyBeam":
                                if (beam) { ends.add(time); visibleUntil = time + tailDuration; }
                                beam = false; return null;
                            case "beamExists": return beam;
                            case "isBeamFireOnlyOnFullCharge":
                            case "deductEnergy":
                            case "deductOneAmmo": return true;
                        }
                        if (method.getReturnType() == boolean.class) return false;
                        if (method.getReturnType() == int.class) return 0;
                        if (method.getReturnType() == float.class) return 0f;
                        return null;
                    });
            tracker = trackerClass.getConstructor(float.class, float.class, float.class,
                    float.class, float.class, callbacks).newInstance(3f, .75f, 5f, cooldown,
                            HULL.equals(hullId) ? shipFlux : 0f, callback);
            advance = trackerClass.getMethod("super", boolean.class, float.class);
            com.fs.starfarer.loading.specs.Object spec = new com.fs.starfarer.loading.specs.Object(weaponId);
            spec.setBurstBeam(true);
            spec.setChargeupTime(3f);
            spec.setChargedownTime(.75f);
            spec.setBurstDuration(5f);
            spec.setBurstCooldown(cooldown);
            spec.setTurnRate(2.5f);
            spec.setDamagePerSecond(1000f);
            spec.setBeamFireOnlyOnFullCharge(true);
            Class<?> nativeClass = Class.forName("com.fs.starfarer.combat.entities.ship.A.G");
            nativeWeapon = (WeaponAPI) UNSAFE.allocateInstance(nativeClass);
            for (Field field : nativeClass.getDeclaredFields()) {
                if (field.getType() == spec.getClass() || field.getType() == trackerClass) {
                    field.setAccessible(true);
                    field.set(nativeWeapon, field.getType() == trackerClass ? tracker : spec);
                }
            }
            Map<String, Object> data = new HashMap<>();
            if (!useCampaignOverride) data.put("chief_navigator_ithaca_research_component_mask", mask);
            ShipAPI source = ship(hullId, mask, 0, new Vector2f());
            ShipHullSpecAPI hull = source.getHullSpec();
            ShipAPI ship = mock(ShipAPI.class, (n, a) -> {
                if (n.equals("getHullSpec")) return hull;
                if (n.equals("getCustomData")) return data;
                if (n.equals("setCustomData")) data.put((String) a[0], a[1]);
                try { return ShipAPI.class.getMethod(n, parameterTypes(n, a)).invoke(source, a); }
                catch (Exception ignored) { return null; }
            });
            WeaponAPI[] owner = { null };
            BeamAPI visibleRay = mock(BeamAPI.class, (n, a) -> {
                if (n.equals("getBrightness")) return 1f;
                if (n.equals("getWeapon")) return owner[0];
                if (n.equals("getFrom")) return new Vector2f();
                if (n.equals("getTo")) return new Vector2f(1000f, 0f);
                return null;
            });
            weapon = mock(WeaponAPI.class, (n, a) -> {
                switch (n) {
                    case "getId": return weaponId;
                    case "getShip": return ship;
                    case "getSpec": return nativeWeapon.getSpec();
                    case "ensureClonedSpec": nativeWeapon.ensureClonedSpec(); return null;
                    case "getCooldown": return nativeWeapon.getCooldown();
                    case "getCooldownRemaining": return nativeWeapon.getCooldownRemaining();
                    case "setRemainingCooldownTo":
                        if (writes == 0) firstTimerWrite = time;
                        writes++; nativeWeapon.setRemainingCooldownTo((float) a[0]); return null;
                    case "getChargeLevel": return nativeWeapon.getChargeLevel();
                    case "isBurstBeam": return true;
                    case "isFiring": return nativeWeapon.isFiring();
                    case "getLocation": return new Vector2f();
                    case "getFirePoint": return new Vector2f(15f, 0f);
                    case "getBeams": return !hideWeaponBeams && beam ? List.of(visibleRay) : List.of();
                }
                return null;
            });
            owner[0] = weapon;
            WeaponAPI foreignWeapon = mock(WeaponAPI.class, (n, a) -> null);
            BeamAPI foreignRay = mock(BeamAPI.class, (n, a) -> n.equals("getWeapon") ? foreignWeapon
                    : n.equals("getBrightness") ? 1f : null);
            engine = mock(CombatEngineAPI.class, (n, a) -> {
                if (n.equals("spawnProjectile")) {
                    check(!paused && !beam && time >= visibleUntil
                                    && nativeWeapon.getCooldownRemaining() <= 0f
                                    && nativeWeapon.getChargeLevel() < 1f,
                            "Charge pulses only during the actual native windup");
                    chargePulses.add(time);
                    return mock(DamagingProjectileAPI.class, (pn, pa) -> {
                        if (pn.equals("setDamageAmount")) check(Math.abs((Float) pa[0] - 100f) < .001f,
                                "Each pulse uses 10% of firing damage, independent of cycling");
                        return null;
                    });
                }
                return n.equals("isPaused") ? paused
                    : n.equals("getBeams") ? (beam || time < visibleUntil
                            ? (foreignBeamVisible ? List.of(visibleRay, foreignRay) : List.of(visibleRay))
                            : (foreignBeamVisible ? List.of(foreignRay) : List.of()))
                    : n.equals("getShips") ? List.of() : null;
            });
            check(IthacaResearchUpgrades.getMask(ship) == mask, "Expected exact campaign/deployment upgrade mask");
        }

        void tick() throws Exception {
            if (Float.isNaN(firstCooldown) && nativeWeapon.getCooldownRemaining() > 0f) firstCooldown = time;
            effect.advance(step, engine, weapon); // Native beam invokes the effect before its timer.
            if (!beam && time >= visibleUntil && nativeWeapon.getCooldownRemaining() > 0f
                    && (cooldownStarts.isEmpty() || cooldownStarts.size() < ends.size())) {
                cooldownStarts.add(time);
                cooldownValues.add(nativeWeapon.getCooldownRemaining());
                cooldownBarStarts.add(nativeWeapon.getChargeLevel());
                cooldownMaxBarDrop.add(0f);
                lastBar = nativeWeapon.getChargeLevel();
                measuringCooldown = true;
            } else if (measuringCooldown) {
                int i = cooldownMaxBarDrop.size() - 1;
                float bar = nativeWeapon.getChargeLevel();
                cooldownMaxBarDrop.set(i, Math.max(cooldownMaxBarDrop.get(i), lastBar - bar));
                check(bar <= lastBar + .0001f, "Native HUD cooldown progress must not jump backward");
                lastBar = bar;
                if (nativeWeapon.getCooldownRemaining() <= 0f) {
                    cooldownReady.add(time);
                    measuringCooldown = false;
                }
            }
            if (!paused) {
                float nativeStep = step;
                if (!beam && time >= visibleUntil && nativeWeapon.getCooldownRemaining() > 0f)
                    nativeStep *= nativeCooldownRate;
                time += step;
                advance.invoke(tracker, trigger, nativeStep);
            }
        }

        void run(float seconds) throws Exception {
            while (time < seconds) tick();
        }
    }

    // Resolve ordinary fixture ship accessors without production engine internals.
    static Class<?>[] parameterTypes(String name, Object[] arguments) throws NoSuchMethodException {
        int count = arguments == null ? 0 : arguments.length;
        for (Method method : ShipAPI.class.getMethods())
            if (method.getName().equals(name) && method.getParameterCount() == count) return method.getParameterTypes();
        throw new NoSuchMethodException(name);
    }

    static void compareCycles(float step, boolean override) throws Exception {
        Global.setSector(null);
        if (override) {
            Map<String, Object> flags = new HashMap<>();
            MemoryAPI memory = mock(MemoryAPI.class, (n, a) -> {
                if (n.equals("set")) flags.put((String) a[0], a[1]);
                if (n.equals("getBoolean")) return Boolean.TRUE.equals(flags.get(a[0]));
                return null;
            });
            Global.setSector(mock(SectorAPI.class, (n, a) -> n.equals("getMemoryWithoutUpdate") ? memory : null));
            check(IthacaResearchUpgrades.enableMaximumUpgradeOverride(), "Console command's campaign override must succeed");
            check(IthacaResearchUpgrades.getCampaignMask() == 127, "Console override includes Lens and Cycling Regulator");
        }
        float upgradedCooldown = shipCooldown * IthacaResearchUpgrades.NAVAL_CYCLE_MULTIPLIER;
        Gun patched = new Gun(shipCooldown, override ? 127 : IthacaResearchUpgrades.NAVAL_CYCLER, step, override);
        Gun reference = new Gun(upgradedCooldown, 0, step, false);
        float testDuration = 3f * (3f + 5f + .75f + upgradedCooldown) + 2f;
        patched.run(testDuration); reference.run(testDuration);
        check(patched.starts.size() >= 3 && patched.starts.size() == reference.starts.size(),
                "Regulator must produce the native upgraded number of full-power bursts: " + patched.starts + " vs " + reference.starts);
        for (int i = 0; i < patched.starts.size(); i++)
            check(Math.abs(patched.starts.get(i) - reference.starts.get(i)) <= step * 3.1f,
                    "Actual native refire time must match a controller created with the upgraded cooldown: " + patched.starts + " vs " + reference.starts);
        for (int i = 0; i < Math.min(patched.ends.size(), reference.ends.size()); i++)
            check(Math.abs(patched.ends.get(i) - reference.ends.get(i)) <= step * 3.1f,
                    "Native beam shutdown timing must be preserved");
        verifyHudAndDuration(patched, upgradedCooldown);
        check(Math.abs(((BeamWeaponSpecAPI) patched.weapon.getSpec()).getBurstCooldown() - upgradedCooldown) < .0001f,
                "Keep upgraded weapon metadata consistent");
        float remaining = patched.weapon.getCooldownRemaining();
        patched.paused = true; int writes = patched.writes; patched.tick();
        check(patched.weapon.getCooldownRemaining() == remaining && patched.writes == writes,
                "Pause must not alter native cooldown state");
        System.out.println("PASS: native repeated bursts match " + upgradedCooldown + "-second controller; step=" + step
                + ", consoleOverride=" + override + ", starts=" + patched.starts + ", writes=" + patched.writes);
    }

    static void checkOtherMasksAndFortress() throws Exception {
        Global.setSector(null);
        for (int mask : new int[] {0, IthacaResearchUpgrades.MACROSERVOS, IthacaResearchUpgrades.ABYSSAL_LENS}) {
            Gun gun = new Gun(shipCooldown, mask, 1f / 60f, false);
            Gun control = new Gun(shipCooldown, 0, 1f / 60f, false);
            gun.run(35f); control.run(35f);
            check(gun.starts.equals(control.starts),
                    "Non-Regulator masks retain their full post-beam cooldown: mask=" + mask);
        }
        String fortressHull = "chief_navigator_ithaca_intact_base";
        String fortressWeapon = "chief_navigator_drifting_wall_colossal_inert";
        Gun fortress = new Gun(10f, IthacaResearchUpgrades.NAVAL_CYCLER, 1f / 60f, false, fortressHull, fortressWeapon);
        Gun control = new Gun(4f, 0, 1f / 60f, false, fortressHull, fortressWeapon);
        fortress.run(45f); control.run(45f);
        check(fortress.starts.equals(control.starts) && fortress.writes > 0,
                "The shared fortress effect must match its native4-second upgraded controller");
        System.out.println("PASS: absent regulator, Macroservos-only and Lens-only preserve native timing; fortress regulator matches4-second controller.");
    }

    static void checkSinglePress() throws Exception {
        Global.setSector(null);
        for (int mask : new int[] {0, IthacaResearchUpgrades.NAVAL_CYCLER}) {
            Gun gun = new Gun(shipCooldown, mask, 1f / 60f, false);
            gun.tick(); // A single press commits the native burst.
            gun.trigger = false;
            gun.run(25f);
            check(gun.starts.size() == 1 && gun.ends.size() == 1,
                    "A single press must commit exactly one complete burst");
            check(gun.chargePulses.size() == 11, "A committed native windup emits all eleven pulses");
            check(gun.firstCooldown >= gun.starts.get(0) + 5f - gun.step * 2f,
                    "No native cooldown while charging or during the full-power beam");
            if (mask != 0) check(gun.firstTimerWrite >= gun.starts.get(0) + 5f - gun.step * 2f,
                    "The Cycling Regulator must not touch the timer before the beam finishes firing");
            System.out.println("SINGLE PRESS: mask=" + mask + ", beamStart=" + gun.starts
                    + ", beamEnd=" + gun.ends + ", nativeCooldownStart=" + gun.firstCooldown
                    + ", regulatorWrite=" + gun.firstTimerWrite);
        }
    }

    static void checkVisibleBeamTail() throws Exception {
        Global.setSector(null);
        for (int mask : new int[] {IthacaResearchUpgrades.NAVAL_CYCLER, 0}) {
            Gun gun = new Gun(shipCooldown, mask, 1f / 60f, false);
            gun.hideWeaponBeams = true; // Only the engine still exposes this ray.
            gun.foreignBeamVisible = true; // The other mount must not hold this gun's cooldown.
            gun.tailDuration = 2f;
            gun.tick(); gun.trigger = false; gun.run(8.3f);
            gun.paused = true;
            float held = gun.weapon.getCooldownRemaining(); int writes = gun.writes;
            gun.tick();
            check(gun.weapon.getCooldownRemaining() == held && gun.writes == writes,
                    "Pausing during a visible beam must not advance or rewrite cooldown");
            gun.paused = false; gun.run(12f + shipCooldown);
            float expected = shipCooldown * (mask == 0 ? 1f : IthacaResearchUpgrades.NAVAL_CYCLE_MULTIPLIER);
            check(gun.starts.size() == 1 && gun.cooldownStarts.size() == 1,
                    "One press commits one shot and one post-beam cooldown");
            check(gun.cooldownStarts.get(0) >= gun.ends.get(0) + gun.tailDuration - gun.step,
                    "Cooldown must wait for the engine-only visible ray to disappear");
            verifyHudAndDuration(gun, expected);
            System.out.println("PASS: single-press engine-only ray ends at " + (gun.ends.get(0) + gun.tailDuration)
                    + "; post-beam duration=" + (gun.cooldownReady.get(0) - gun.cooldownStarts.get(0))
                    + ", HUD starts=" + gun.cooldownBarStarts + ", mask=" + mask);
        }
    }

    static void verifyHudAndDuration(Gun gun, float expected) {
        for (float bar : gun.cooldownBarStarts)
            check(Math.abs(bar - 1f) < .0001f,
                    "Native HUD bar must begin full without jumping to a shorter cached-timer fraction: " + gun.cooldownBarStarts);
        for (int i = 0; i < gun.cooldownReady.size(); i++) {
            float elapsed = gun.cooldownReady.get(i) - gun.cooldownStarts.get(i);
            check(Math.abs(elapsed - expected) <= gun.step * 2.1f,
                    "Full post-beam cooldown must retain its actual duration: " + elapsed + " vs " + expected);
            check(gun.cooldownMaxBarDrop.get(i) <= gun.step / expected * 1.1f + .001f,
                    "The native HUD bar must drain continuously: " + gun.cooldownMaxBarDrop);
        }
    }

    static void checkNativeCooldownClock() throws Exception {
        Global.setSector(null);
        float target = shipCooldown * IthacaResearchUpgrades.NAVAL_CYCLE_MULTIPLIER;
        Gun faster = new Gun(shipCooldown, IthacaResearchUpgrades.NAVAL_CYCLER, 1f / 60f, false);
        faster.nativeCooldownRate = 2f;
        faster.tick(); faster.trigger = false; faster.run(12f + shipCooldown);
        verifyHudAndDuration(faster, target / 2f);
        Gun stalled = new Gun(shipCooldown, IthacaResearchUpgrades.NAVAL_CYCLER, 1f / 60f, false);
        stalled.tick(); stalled.trigger = false; stalled.run(10f);
        stalled.nativeCooldownRate = 0f;
        // Consume one transition frame, then verify that an engine-side stall
        // freezes progress even though the effect itself continues advancing.
        stalled.tick();
        float before = stalled.weapon.getChargeLevel();
        stalled.run(10.5f);
        check(Math.abs(stalled.weapon.getChargeLevel() - before) < .0001f,
                "Native cooldown stalls must freeze the HUD without script-clock progress");
        stalled.nativeCooldownRate = 1f;
        stalled.run(12f + shipCooldown);
        float duration = stalled.cooldownReady.get(0) - stalled.cooldownStarts.get(0);
        check(Math.abs(duration - (target + .5f)) < stalled.step * 4f,
                "Native stall time must be retained: " + duration);
        System.out.println("PASS: native double-rate cooldown=" + (target / 2f)
                + "s; engine-side stall freezes HUD and delays refire.");
    }

    public static void main(String[] args) throws Exception {
        SettingsAPI settings = Global.getSettings(); SectorAPI sector = Global.getSector();
        try {
            Global.setSettings(mock(SettingsAPI.class, (n, a) -> n.equals("getColor") ? Color.WHITE : null));
            JSONArray rows = (JSONArray) Class.forName("com.fs.starfarer.loading.G")
                    .getMethod("o00000", String.class).invoke(null, Files.readString(Path.of("data/weapons/weapon_data.csv")));
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                if (LASER.equals(row.optString("id"))) {
                    shipCooldown = (float) row.getDouble("burst delay");
                    shipFlux = (float) row.getDouble("energy/second");
                }
            }
            check(shipCooldown > 0f && shipFlux >= 0f, "Read current ship-laser data using the native parser");
            for (float step : new float[] {1f / 120f, 1f / 60f, .1f}) compareCycles(step, false);
            compareCycles(1f / 60f, true);
            checkOtherMasksAndFortress();
            checkSinglePress();
            checkVisibleBeamTail();
            checkNativeCooldownClock();
        } finally { Global.setSettings(settings); Global.setSector(sector); }
    }
}
