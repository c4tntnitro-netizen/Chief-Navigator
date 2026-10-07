import chiefnavigator.weapons.IthacaResearchUpgrades;
import chiefnavigator.weapons.IthacaMissileResearchEffect;
import chiefnavigator.weapons.IthacaLocustPDEffect;
import chiefnavigator.weapons.DriftingWallBeamEffect;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.loading.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import org.lwjgl.util.vector.Vector2f;

/** Headless behavior checks: research scope, live ordnance, EMP targeting, Wall encounter. */
public class WallResearchRegression {
    interface Call { Object invoke(String name, Object[] args); }
    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> {
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == args[0];
                    Object value = call.invoke(method.getName(), args);
                    if (value != null) return value;
                    Class<?> result = method.getReturnType();
                    if (result == boolean.class) return false;
                    if (result == float.class) return 0f;
                    if (result == int.class) return 0;
                    return null;
                }));
    }
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    static ShipAPI ship(String hull, int research, int owner, Vector2f position) {
        Map<String, Object> data = new HashMap<>();
        data.put("chief_navigator_ithaca_research_component_mask", research);
        ShipHullSpecAPI spec = mock(ShipHullSpecAPI.class,
                (name, args) -> name.equals("getHullId") ? hull : null);
        return mock(ShipAPI.class, (name, args) -> {
            switch (name) {
                case "getHullSpec": return spec;
                case "getCustomData": return data;
                case "getOwner": return owner;
                case "getLocation": return position;
                case "getVelocity": return new Vector2f();
                case "isAlive": return true;
                case "getCollisionRadius": return hull.contains("base") ? 3100f : 50f;
                default: return null;
            }
        });
    }
    public static void main(String[] args) throws Exception {
        check(IthacaResearchUpgrades.COMPONENT_IDS.length
                        == IthacaResearchUpgrades.COMPONENT_STATION_LOCATIONS.length,
                "Every Ithaca upgrade must name its source station");
        for (String location
                : IthacaResearchUpgrades.COMPONENT_STATION_LOCATIONS) {
            check(location != null && !location.trim().isEmpty(),
                    "Upgrade station location must not be blank");
        }
        com.fs.starfarer.api.Global.setSettings(mock(com.fs.starfarer.api.SettingsAPI.class,
                (n,a) -> n.equals("getColor") ? java.awt.Color.WHITE : null));
        String ithaca = "chief_navigator_ithaca_intact_base";
        int[] missileMasks = {
            0,
            IthacaResearchUpgrades.AUTOLOADER_CORE,
            IthacaResearchUpgrades.MUNITIONS_COMPILER,
            IthacaResearchUpgrades.AUTOLOADER_CORE
                    | IthacaResearchUpgrades.MUNITIONS_COMPILER
        };
        for (int upgrades : missileMasks) {
            ShipAPI source = ship(ithaca, upgrades, 0, new Vector2f());
            check(IthacaResearchUpgrades.getMask(source) == upgrades,
                    "Component-mask snapshot");
            final float[] settings = {20f, 1f, 1500f};
            final float[] nativeRefire = {20f};
            final int[] nativeRefireWrites = {0};
            final boolean[] cloned = {false};
            ProjectileWeaponSpecAPI spec = mock(ProjectileWeaponSpecAPI.class, (n, a) -> {
                if (n.equals("getRefireDelay")) return settings[0];
                if (n.equals("getAmmoPerSecond")) return settings[1];
                if (n.equals("setRefireDelay")) settings[0] = (Float) a[0];
                if (n.equals("setAmmoPerSecond")) settings[1] = (Float) a[0];
                return null;
            });
            AmmoTrackerAPI ammo = mock(AmmoTrackerAPI.class, (n,a) -> null);
            DamageAPI damage = mock(DamageAPI.class, (n,a) -> {
                if (n.equals("setDamage")) settings[2] = (Float) a[0];
                return null;
            });
            WeaponAPI weapon = mock(WeaponAPI.class, (n,a) -> {
                if (n.equals("getShip")) return source;
                if (n.equals("getSpec")) return spec;
                if (n.equals("setRefireDelay")) {
                    nativeRefire[0] = (Float) a[0];
                    nativeRefireWrites[0]++;
                }
                if (n.equals("ensureClonedSpec")) cloned[0] = true;
                if (n.equals("getAmmoTracker")) return ammo;
                if (n.equals("getDamage")) return damage;
                return null;
            });
            int[] replaced = {0, 0};
            CombatEngineAPI engine = mock(CombatEngineAPI.class, (n,a) -> {
                if (n.equals("spawnProjectile")) {
                    check(a[2].equals("chief_navigator_ithaca_atropos_battery"),
                            "Atropos spec");
                    replaced[0]++;
                }
                if (n.equals("removeEntity")) replaced[1]++;
                return null;
            });
            IthacaMissileResearchEffect effect = new IthacaMissileResearchEffect();
            effect.advance(.1f, engine, weapon);
            effect.advance(.1f, engine, weapon);
            float cycle = IthacaResearchUpgrades.hasUpgrade(
                    upgrades, IthacaResearchUpgrades.AUTOLOADER_CORE)
                            ? IthacaResearchUpgrades.MISSILE_CYCLE_MULTIPLIER
                            : 1f;
            check(Math.abs(settings[0] - 20f * cycle) < .001f, "Refire applied once");
            check(Math.abs(nativeRefire[0] - settings[0]) < .001f
                            && nativeRefireWrites[0] == (cycle < 1f ? 1 : 0),
                    "Hammer cycling updates the native refire tracker exactly once");
            check(Math.abs(settings[1] - 1f / cycle) < .001f, "Reload applied once");
            check(cloned[0] == (upgrades != 0), "No shared-spec mutation");
            Map<String, Object> projectileData = new HashMap<>();
            DamagingProjectileAPI projectile = mock(DamagingProjectileAPI.class, (n,a) -> {
                if (n.equals("getLocation")) return new Vector2f();
                if (n.equals("getProjectileSpecId")) return "hammer_torp";
                if (n.equals("getCustomData")) return projectileData;
                if (n.equals("setCustomData")) projectileData.put((String)a[0], a[1]);
                return null;
            });
            effect.onFire(projectile, weapon, engine);
            effect.onFire(projectile, weapon, engine);
            boolean atropos = IthacaResearchUpgrades.hasUpgrade(
                    upgrades, IthacaResearchUpgrades.MUNITIONS_COMPILER);
            check(replaced[0] == (atropos ? 1 : 0)
                    && replaced[1] == replaced[0],
                    "Atropos unlock and Hammer removal");
            check(settings[2] == (atropos ? 1000f : 1500f),
                    "Base Hammer and upgraded Atropos damage");
        }
        check(IthacaResearchUpgrades.getMask(ship(
                "chief_navigator_drifting_wall_base",
                IthacaResearchUpgrades.ALL_UPGRADES, 1, new Vector2f())) == 0,
                "Research must not buff hostile Wall");
        check(IthacaResearchUpgrades.getMask(ship(
                "chief_navigator_ithaca_naval_mount",
                IthacaResearchUpgrades.ALL_UPGRADES, 0,
                new Vector2f())) == IthacaResearchUpgrades.ALL_UPGRADES,
                "Gate-defense naval mounts must inherit Ithaca research");
        check(IthacaResearchUpgrades.getMask(ship(
                "chief_navigator_spartan_battlestation",
                IthacaResearchUpgrades.ALL_UPGRADES, 0,
                new Vector2f())) == IthacaResearchUpgrades.ALL_UPGRADES,
                "Spartan station parent must inherit Ithaca research");

        for (int upgrades : new int[] {
                0,
                IthacaResearchUpgrades.INTERCEPT_MATRIX,
                IthacaResearchUpgrades.INTERCEPT_MATRIX
                        | IthacaResearchUpgrades.AUTOLOADER_CORE}) {
            ShipAPI foundation = ship(ithaca, upgrades, 0, new Vector2f());
            int[] forceDisableCalls = {0};
            boolean[] forcedDisabled = {false};
            int[] pdFlags = {0};
            float[] fixedAngle = {-999f};
            float[] locustSpec = {16f, 1.28125f, 81f, 41f, 41f};
            int[] ammoState = {41, 41};
            float[] trackerState = {1.28125f, 41f};
            boolean[] cloned = {false};
            ProjectileWeaponSpecAPI cloudswarmSpec = mock(
                    ProjectileWeaponSpecAPI.class, (n,a) -> {
                        if (n.equals("getRefireDelay")) return locustSpec[0];
                        if (n.equals("setRefireDelay")) locustSpec[0] = (Float) a[0];
                        if (n.equals("getAmmoPerSecond")) return locustSpec[1];
                        if (n.equals("setAmmoPerSecond")) locustSpec[1] = (Float) a[0];
                        if (n.equals("getBurstSize")) return (int) locustSpec[2];
                        if (n.equals("setBurstSize")) locustSpec[2] = (Integer) a[0];
                        if (n.equals("getMaxAmmo")) return (int) locustSpec[3];
                        if (n.equals("setMaxAmmo")) locustSpec[3] = (Integer) a[0];
                        if (n.equals("getReloadSize")) return locustSpec[4];
                        if (n.equals("setReloadSize")) locustSpec[4] = (Float) a[0];
                        return null;
                    });
            AmmoTrackerAPI cloudswarmAmmo = mock(AmmoTrackerAPI.class, (n,a) -> {
                if (n.equals("getAmmo")) return ammoState[0];
                if (n.equals("setAmmo")) ammoState[0] = (Integer) a[0];
                if (n.equals("getMaxAmmo")) return ammoState[1];
                if (n.equals("setMaxAmmo")) ammoState[1] = (Integer) a[0];
                if (n.equals("getAmmoPerSecond")) return trackerState[0];
                if (n.equals("setAmmoPerSecond")) trackerState[0] = (Float) a[0];
                if (n.equals("getReloadSize")) return trackerState[1];
                if (n.equals("setReloadSize")) trackerState[1] = (Float) a[0];
                return null;
            });
            WeaponAPI locust = mock(WeaponAPI.class, (n,a) -> {
                if (n.equals("getShip")) return foundation;
                if (n.equals("getArcFacing")) return 37f;
                if (n.equals("setCurrAngle")) fixedAngle[0] = (Float) a[0];
                if (n.equals("getSpec")) return cloudswarmSpec;
                if (n.equals("getAmmoTracker")) return cloudswarmAmmo;
                if (n.equals("ensureClonedSpec")) cloned[0] = true;
                if (n.equals("setForceDisabled")) {
                    forceDisableCalls[0]++;
                    forcedDisabled[0] = (Boolean) a[0];
                }
                if (n.equals("setPD") || n.equals("setPDAlso")) pdFlags[0]++;
                return null;
            });
            new IthacaLocustPDEffect().advance(.1f,
                    mock(CombatEngineAPI.class, (n,a) -> null), locust);
            if (upgrades == 0) {
                check(forceDisableCalls[0] == 1 && forcedDisabled[0],
                        "Locust arrays stay locked without the intercept matrix");
            } else {
                check(forceDisableCalls[0] == 0,
                        "Unlocked Locust arrays must preserve external disables");
            }
            check(pdFlags[0] == 2, "Locust arrays keep both PD flags");
            check(Math.abs(fixedAngle[0] - 37f) < .001f,
                    "Cloudswarm launcher remains fixed to its mount facing");
            boolean autoloader = IthacaResearchUpgrades.hasUpgrade(
                    upgrades, IthacaResearchUpgrades.AUTOLOADER_CORE);
            check(cloned[0],
                    "Cloudswarm salvo display uses a per-weapon cloned spec");
            check((int) locustSpec[2] == (autoloader ? 81 : 41)
                            && ammoState[1] == (autoloader ? 81 : 41)
                            && ammoState[0] == (autoloader ? 81 : 41),
                    "Cloudswarm fires 41 missiles, or 81 with autoloader");
            check(Math.abs(trackerState[1] - (autoloader ? 81f : 41f)) < .001f,
                    "Cloudswarm reload size matches its burst");
            check(Math.abs(locustSpec[0] - (autoloader ? 6.4f : 16f)) < .001f
                            && Math.abs(trackerState[0]
                                    - (autoloader ? 6.328125f : 1.28125f)) < .001f,
                    "Autoloader preserves 40% cycling and full-reload time for reduced salvos");
        }

        ShipAPI foundation = ship(ithaca,
                IthacaResearchUpgrades.INTERCEPT_MATRIX, 0, new Vector2f());
        WeaponAPI locustWeapon = mock(WeaponAPI.class, (n,a) -> {
            if (n.equals("getShip")) return foundation;
            if (n.equals("getLocation")) return new Vector2f();
            if (n.equals("getRange")) return 1200f;
            return null;
        });
        MissileAIPlugin[] installedAI = {null};
        float[] sourceFacing = {15f};
        Vector2f sourceVelocity = new Vector2f();
        MissileAPI sourceMissile = mock(MissileAPI.class, (n,a) -> {
            if (n.equals("getOwner")) return 0;
            if (n.equals("getLocation")) return new Vector2f();
            if (n.equals("getVelocity")) return sourceVelocity;
            if (n.equals("getFacing")) return sourceFacing[0];
            if (n.equals("setFacing")) sourceFacing[0] = (Float) a[0];
            if (n.equals("setMissileAI")) installedAI[0] = (MissileAIPlugin) a[0];
            return null;
        });
        MissileAPI incoming = mock(MissileAPI.class, (n,a) -> {
            if (n.equals("getOwner")) return 1;
            if (n.equals("getLocation")) return new Vector2f(500f, 0f);
            if (n.equals("getVelocity")) return new Vector2f();
            return null;
        });
        ShipAPI ordinaryEnemy = ship("enemy_cruiser", 0, 1,
                new Vector2f(100f, 0f));
        CombatEngineAPI pdEngine = mock(CombatEngineAPI.class, (n,a) -> {
            if (n.equals("getMissiles")) {
                return Arrays.asList(sourceMissile, incoming);
            }
            if (n.equals("getShips")) return Collections.singletonList(ordinaryEnemy);
            if (n.equals("isEntityInPlay")) return true;
            return null;
        });
        new IthacaLocustPDEffect().onFire(sourceMissile, locustWeapon, pdEngine);
        check(installedAI[0] instanceof GuidedMissileAI,
                "Unlocked Locust round gets intercept guidance");
        check(((GuidedMissileAI) installedAI[0]).getTarget() == incoming,
                "Locust guidance prioritizes missiles and rejects ordinary ships");
        installedAI[0].advance(.05f);
        check(Math.abs(sourceFacing[0] - 15f) < .001f,
                "Locust round preserves its off-axis tube heading during ejection");
        check(Math.abs(sourceVelocity.length() - 120f) < .01f,
                "Locust round begins at authored launch speed before guidance");

        float[] beamSettings = {10f, 2.5f};
        int[] lightning = {0};
        int[] revertedFireOverMutations = {0};
        ShipAPI upgraded = ship(ithaca,
                IthacaResearchUpgrades.ALL_UPGRADES, 0, new Vector2f());
        BeamWeaponSpecAPI beamSpec = mock(BeamWeaponSpecAPI.class, (n,a) -> {
            if (n.equals("getBurstCooldown")) return beamSettings[0];
            if (n.equals("setBurstCooldown")) beamSettings[0] = (Float)a[0];
            if (n.equals("getTurnRate")) return beamSettings[1];
            if (n.equals("setTurnRate")) beamSettings[1] = (Float)a[0];
            if (n.equals("setCollisionClass")
                    || n.equals("setCollisionClassIfByFighter")) {
                revertedFireOverMutations[0]++;
            }
            return null;
        });
        WeaponAPI charging = mock(WeaponAPI.class, (n,a) -> {
            if (n.equals("getShip")) return upgraded;
            if (n.equals("getSpec")) return beamSpec;
            if (n.equals("getLocation")) return new Vector2f();
            if (n.equals("getChargeLevel")) return 1f;
            if (n.equals("getBeams")) return Collections.emptyList();
            if (n.equals("setSuspendAutomaticTurning")
                    || n.equals("setForceFireOneFrame")
                    || n.equals("setForceNoFireOneFrame")) {
                revertedFireOverMutations[0]++;
            }
            return null;
        });
        CombatEngineAPI chargingEngine = mock(CombatEngineAPI.class, (n,a) -> {
            if (n.equals("spawnEmpArcVisual")) lightning[0]++;
            if (n.equals("applyDamage")) revertedFireOverMutations[0]++;
            return null;
        });
        DriftingWallBeamEffect chargingEffect = new DriftingWallBeamEffect();
        chargingEffect.advance(.25f, chargingEngine, charging);
        chargingEffect.advance(.25f, chargingEngine, charging);
        check(Math.abs(beamSettings[0] - 4f) < .001f,
                "Naval cycling regulator applied only once");
        check(Math.abs(beamSettings[1] - 10f) < .001f,
                "Macroservos quadruple naval traverse only once");
        check(revertedFireOverMutations[0] == 0,
                "Upgraded beam retains ordinary targeting, collision, and damage");
        check(lightning[0] == 2, "Visible charging lightning");

        ShipAPI source = ship(ithaca,
                IthacaResearchUpgrades.ARC_PROJECTOR, 0, new Vector2f());
        List<ShipAPI> ships = new ArrayList<>();
        ships.add(ship("ally", 0, 0, new Vector2f(500f, 100f)));
        ships.add(ship("enemy", 0, 1, new Vector2f(500f, 200f)));
        BeamAPI beam = mock(BeamAPI.class, (n,a) -> {
            if (n.equals("getBrightness")) return 1f;
            if (n.equals("getFrom")) return new Vector2f();
            if (n.equals("getTo")) return new Vector2f(1000f, 0f);
            return null;
        });
        WeaponAPI beamWeapon = mock(WeaponAPI.class, (n,a) -> {
            if (n.equals("getShip")) return source;
            if (n.equals("getBeams")) return Collections.singletonList(beam);
            return null;
        });
        int[] arcs = {0};
        CombatEngineAPI arcEngine = mock(CombatEngineAPI.class, (n,a) -> {
            if (n.equals("getShips")) return ships;
            if (n.equals("spawnEmpArc")) {
                check(a[3] == ships.get(1), "EMP targets enemies only");
                check((Float)a[5] == 0f && (Float)a[6] == 200f, "EMP-only arc");
                arcs[0]++;
            }
            return null;
        });
        Method arc = DriftingWallBeamEffect.class.getDeclaredMethod(
                "arcFromBeam", CombatEngineAPI.class, WeaponAPI.class);
        arc.setAccessible(true);
        arc.invoke(null, arcEngine, beamWeapon);
        check(arcs[0] == 1, "Beam arcs to nearby enemy");
        ships.remove(1);
        arc.invoke(null, arcEngine, beamWeapon);
        check(arcs[0] == 1, "No arc into allied ship");

        ShipAPI isaSource = ship(ithaca, 0, 0, new Vector2f());
        isaSource.getCustomData().put(
                "chief_navigator_isa_refraction_enabled", Boolean.TRUE);
        Object[] isaListener = {null};
        boolean[] listenerInstalled = {false};
        ShieldAPI shield = mock(ShieldAPI.class, (n,a) -> {
            if (n.equals("isOn") || n.equals("isWithinArc")) return true;
            return null;
        });
        ShipHullSpecAPI primarySpec = mock(ShipHullSpecAPI.class,
                (n,a) -> n.equals("getHullId") ? "enemy_primary" : null);
        ShipAPI isaPrimary = mock(ShipAPI.class, (n,a) -> {
            if (n.equals("getHullSpec")) return primarySpec;
            if (n.equals("getOwner")) return 1;
            if (n.equals("getLocation")) return new Vector2f(1000f, 0f);
            if (n.equals("getCollisionRadius")) return 100f;
            if (n.equals("isAlive")) return true;
            if (n.equals("getShield")) return shield;
            if (n.equals("hasListenerOfClass")) return listenerInstalled[0];
            if (n.equals("addListener")) {
                isaListener[0] = a[0];
                listenerInstalled[0] = true;
            }
            return null;
        });
        ShipAPI isaSecondary = ship("enemy_secondary", 0, 1,
                new Vector2f(1800f, 0f));
        WeaponAPI.DerivedWeaponStatsAPI isaDerived = mock(
                WeaponAPI.DerivedWeaponStatsAPI.class,
                (n,a) -> n.equals("getDps") ? 2000f : null);
        final WeaponAPI[] isaWeaponRef = {null};
        BeamAPI isaBeam = mock(BeamAPI.class, (n,a) -> {
            if (n.equals("getBrightness")) return 1f;
            if (n.equals("getFrom")) return new Vector2f();
            if (n.equals("getTo") || n.equals("getRayEndPrevFrame")) {
                return new Vector2f(1000f, 0f);
            }
            if (n.equals("getDamageTarget")) return isaPrimary;
            if (n.equals("getSource")) return isaSource;
            if (n.equals("getWeapon")) return isaWeaponRef[0];
            return null;
        });
        WeaponAPI isaWeapon = mock(WeaponAPI.class, (n,a) -> {
            if (n.equals("getId")) {
                return "chief_navigator_drifting_wall_colossal_inert";
            }
            if (n.equals("getShip")) return isaSource;
            if (n.equals("getBeams")) return Collections.singletonList(isaBeam);
            if (n.equals("getDerivedStats")) return isaDerived;
            if (n.equals("getLocation")) return new Vector2f();
            return null;
        });
        isaWeaponRef[0] = isaWeapon;
        int[] refractions = {0};
        CombatEngineAPI isaEngine = mock(CombatEngineAPI.class, (n,a) -> {
            if (n.equals("getShips")) {
                return Arrays.asList(isaPrimary, isaSecondary);
            }
            if (n.equals("spawnEmpArc")) {
                check(a[2] == isaPrimary && a[3] == isaSecondary,
                        "Isa refraction is a single enemy-to-enemy hop");
                check(Math.abs((Float)a[5] - 250f) < .001f
                                && (Float)a[6] == 0f,
                        "Isa secondary beam is half strength and has no EMP");
                refractions[0]++;
            }
            return null;
        });
        new DriftingWallBeamEffect().advance(
                .25f, isaEngine, isaWeapon);
        check(listenerInstalled[0]
                        && isaListener[0] instanceof DamageTakenModifier,
                "Isa shield listener installed on naval-beam contact");
        check(refractions[0] == 1,
                "Isa shield contact emits one non-propagating refraction");
        MutableStat isaDamageMult = new MutableStat(1f);
        DamageAPI isaDamage = mock(DamageAPI.class,
                (n,a) -> n.equals("getModifier") ? isaDamageMult : null);
        ((DamageTakenModifier) isaListener[0]).modifyDamageTaken(
                isaBeam, isaPrimary, isaDamage,
                new Vector2f(1000f, 0f), true);
        check(Math.abs(isaDamageMult.getModifiedValue() - 1.5f) < .001f,
                "Isa naval beam deals exactly 50% additional shield damage");
        ShipAPI hostileWall = ship("chief_navigator_drifting_wall_base",
                0, 1, new Vector2f());
        hostileWall.getCustomData().put(
                "chief_navigator_isa_refraction_enabled", Boolean.TRUE);
        check(!IthacaResearchUpgrades.hasIsaRefraction(hostileWall),
                "Isa upgrade must not strengthen hostile Wall");

        Method guardVariants = Class.forName("chiefnavigator.quest.DriftingWallEncounter")
                .getDeclaredMethod("getDroneGuardVariants");
        guardVariants.setAccessible(true);
        check(((String[]) guardVariants.invoke(null)).length == 0,
                "The hostile Wall has no authored Guard escort");
        int[] spawned = {0};
        boolean[] paused = {false};
        ShipAPI core = ship("chief_navigator_drifting_wall_base", 0, 1,
                new Vector2f(0f, 3000f));
        List<ShipAPI> wallShips = new ArrayList<>();
        wallShips.add(core);
        com.fs.starfarer.api.Global.setSettings(mock(
                com.fs.starfarer.api.SettingsAPI.class, (n,a) -> {
                    if (n.equals("getColor")) return java.awt.Color.WHITE;
                    return null;
                }));
        CombatFleetManagerAPI manager = mock(CombatFleetManagerAPI.class, (n,a) -> {
            if (n.equals("spawnShipOrWing")) spawned[0]++;
            return null;
        });
        CombatEngineAPI engine = mock(CombatEngineAPI.class, (n,a) -> {
            if (n.equals("getFleetManager")) return manager;
            if (n.equals("getShips")) return wallShips;
            if (n.equals("isPaused")) return paused[0];
            return null;
        });
        Class<?> plugin = Class.forName(
                "chiefnavigator.quest.DriftingWallBattleCreationPlugin$DriftingWallCombatPlugin");
        Constructor<?> constructor = plugin.getDeclaredConstructor(CombatEngineAPI.class);
        constructor.setAccessible(true);
        Object instance = constructor.newInstance(engine);
        Field coreField = plugin.getDeclaredField("core"); coreField.setAccessible(true);
        coreField.set(instance, core);
        Field reactorField = plugin.getDeclaredField("reactor");
        reactorField.setAccessible(true);
        reactorField.set(instance, core);
        Field reactorInitialized = plugin.getDeclaredField(
                "reactorInitialized");
        reactorInitialized.setAccessible(true);
        reactorInitialized.set(instance, true);
        Method advance = plugin.getDeclaredMethod("advance", float.class, List.class);
        advance.setAccessible(true);
        paused[0] = true;
        advance.invoke(instance, 120f, Collections.emptyList());
        check(spawned[0] == 0, "No escort launches while paused");
        paused[0] = false;
        advance.invoke(instance, 1f, Collections.emptyList());
        check(spawned[0] == 0, "No escort deploys during combat");
        // Skip unrelated debug logging; the actual live update still advances
        // far beyond the former sixty-second Gargoyle wave threshold.
        Field debugElapsed = plugin.getDeclaredField("debugElapsed");
        debugElapsed.setAccessible(true);
        debugElapsed.set(instance, -10000f);
        advance.invoke(instance, 180f, Collections.emptyList());
        check(spawned[0] == 0, "No escort or reinforcement waves after three minutes");
        Field disabled = plugin.getDeclaredField("wallDisabled"); disabled.setAccessible(true);
        disabled.set(instance, true);
        advance.invoke(instance, 120f, Collections.emptyList());
        check(spawned[0] == 0, "No launches after reactor destruction");

        Map<String, Object> campaignMemory = new HashMap<>();
        com.fs.starfarer.api.campaign.rules.MemoryAPI memory = mock(
                com.fs.starfarer.api.campaign.rules.MemoryAPI.class, (n,a) -> {
                    if (n.equals("getBoolean")) {
                        return Boolean.TRUE.equals(campaignMemory.get(a[0]));
                    }
                    if (n.equals("set")) campaignMemory.put((String)a[0], a[1]);
                    return null;
                });
        com.fs.starfarer.api.campaign.SectorAPI sector = mock(
                com.fs.starfarer.api.campaign.SectorAPI.class,
                (n,a) -> n.equals("getMemoryWithoutUpdate") ? memory : null);
        com.fs.starfarer.api.Global.setSector(sector);
        check(!IthacaResearchUpgrades.captureIsaRefractionUnlock(),
                "Isa refraction remains locked before recruitment");
        campaignMemory.put("$ship_trophy_room_isa_officer_granted", true);
        check(IthacaResearchUpgrades.captureIsaRefractionUnlock()
                        && Boolean.TRUE.equals(campaignMemory.get(
                                "$chief_navigator_isa_refraction_unlocked_v1")),
                "Hall of Triumph recruitment permanently unlocks refraction");
        campaignMemory.put("$ship_trophy_room_isa_officer_granted", false);
        check(IthacaResearchUpgrades.captureIsaRefractionUnlock(),
                "Isa refraction remains unlocked after roster changes");
        com.fs.starfarer.api.Global.setSector(null);
        System.out.println("PASS: independent component mask, wall and gate-defense scope, "
                + "naval traverse/cycling, missile cycling, Hammer-to-Atropos conversion, "
                + "fixed 41/81-round Cloudswarm PD, off-axis launch and "
                + "missile-only priority, "
                + "EMP targeting, Isa shield refraction, and native naval-beam collision, "
                + "no Guard escort or reinforcement waves, pause and shutdown.");
    }
}
