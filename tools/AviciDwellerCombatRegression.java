import chiefnavigator.dwellers.AviciDwellerContent;
import chiefnavigator.dwellers.AviciDwellerHullmod;
import chiefnavigator.dwellers.AviciHunterDomain;
import chiefnavigator.dwellers.AviciHunterSystem;
import chiefnavigator.dwellers.ShroudedBrainAuraStats;
import chiefnavigator.dwellers.ShroudBansheeStats;
import chiefnavigator.dwellers.ShroudBansheeSystemAI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.dweller.DwellerHullmod;
import com.fs.starfarer.api.impl.combat.dweller.DwellerShipCreator;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.State;
import java.awt.Color;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;

/** Actual ported systems against native MutableStat and public combat API fixtures.
 * Verifies combat ownership/range/cleanup, not live GL rendering or encounter balance.
 */
public final class AviciDwellerCombatRegression {
    interface Call { Object invoke(String name, Object[] args); }
    static final Set<String> BONUS_GETTERS = new HashSet<>();
    static {
        for (java.lang.reflect.Method method : MutableShipStatsAPI.class.getMethods()) {
            if (method.getReturnType() == StatBonus.class) BONUS_GETTERS.add(method.getName());
        }
    }

    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> {
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == args[0];
                    if (method.getName().equals("toString")) return type.getSimpleName() + "Fixture";
                    Object value = call.invoke(method.getName(), args);
                    if (value != null) return value;
                    Class<?> result = method.getReturnType();
                    if (result == boolean.class) return false;
                    if (result == float.class) return 0f;
                    if (result == int.class) return 0;
                    if (result == long.class) return 0L;
                    if (result == double.class) return 0d;
                    return null;
                }));
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static void near(float expected, float actual, String message) {
        check(Math.abs(expected - actual) < .001f,
                message + ": expected " + expected + ", got " + actual);
    }

    static final class Vessel {
        final Map<String, Object> data = new HashMap<>();
        final Map<String, MutableStat> values = new HashMap<>();
        final Map<String, StatBonus> bonuses = new HashMap<>();
        final Vector2f location = new Vector2f();
        final Vector2f velocity = new Vector2f();
        final List<Object> listeners = new ArrayList<>();
        final ShipwideAIFlags flags = new ShipwideAIFlags();
        final MutableShipStatsAPI stats;
        ShipAPI ship;
        final String id;
        String hull;
        int owner;
        float facing;
        float flux;
        float overloadDuration;
        int overloads;
        boolean alive = true;
        boolean fighter;
        ShipAPI target;

        Vessel(String id, String hull, int owner, float x) {
            this.id = id;
            this.hull = hull;
            this.owner = owner;
            location.set(x, 0);
            ShipHullSpecAPI spec = mock(ShipHullSpecAPI.class, (n, a) -> switch (n) {
                case "getHullId", "getBaseHullId" -> this.hull;
                case "getHullSize" -> ShipAPI.HullSize.CRUISER;
                default -> null;
            });
            stats = mock(MutableShipStatsAPI.class, (n, a) -> {
                if (n.equals("getEntity")) return this.ship;
                if (BONUS_GETTERS.contains(n)) return bonuses.computeIfAbsent(n, key -> new StatBonus());
                return values.computeIfAbsent(n, key -> new MutableStat(100f));
            });
            FluxTrackerAPI tracker = mock(FluxTrackerAPI.class, (n, a) -> {
                if (n.equals("getFluxLevel")) return flux;
                if (n.equals("beginOverloadWithTotalBaseDuration") || n.equals("forceOverload")) {
                    overloadDuration = (Float) a[0];
                    overloads++;
                }
                return null;
            });
            ship = mock(ShipAPI.class, (n, a) -> {
                switch (n) {
                    case "getId": return this.id;
                    case "getHullSpec": return spec;
                    case "getHullSize": return ShipAPI.HullSize.CRUISER;
                    case "getOwner", "getOriginalOwner": return this.owner;
                    case "getMutableStats": return stats;
                    case "getLocation": return location;
                    case "getVelocity": return velocity;
                    case "getFacing": return facing;
                    case "setFacing": facing = (Float) a[0]; break;
                    case "getCollisionRadius": return 50f;
                    case "isAlive": return alive;
                    case "isHulk": return !alive;
                    case "isFighter": return fighter;
                    case "getHitpoints": case "getMaxHitpoints": return 10000f;
                    case "getHullLevel": return alive ? 1f : 0f;
                    case "getFluxTracker": return tracker;
                    case "getFluxLevel": return flux;
                    case "getCustomData": return data;
                    case "setCustomData": data.put((String) a[0], a[1]); break;
                    case "removeCustomData": data.remove(a[0]); break;
                    case "getShipTarget": return target;
                    case "setShipTarget": target = (ShipAPI) a[0]; break;
                    case "getAIFlags": return flags;
                    case "getAllWeapons": case "getUsableWeapons": return Collections.emptyList();
                    case "addListener": listeners.add(a[0]); break;
                    case "removeListener": listeners.remove(a[0]); break;
                    case "hasListenerOfClass": return listeners.stream().anyMatch(((Class<?>) a[0])::isInstance);
                    default: break;
                }
                return null;
            });
        }

        float value(String getter) {
            if (bonuses.containsKey(getter)) return bonuses.get(getter).computeEffective(100f);
            MutableStat stat = values.get(getter);
            return stat == null ? 100f : stat.getModifiedValue();
        }
    }

    static final class Arena {
        final List<ShipAPI> ships = new ArrayList<>();
        final Map<String, Object> data = new HashMap<>();
        final List<ShipAPI> struck = new ArrayList<>();
        final CombatEngineAPI engine;
        float amount = .25f;
        float elapsed;
        boolean paused;
        boolean visible = true;
        ShipAPI player;

        Arena(Vessel... vessels) {
            for (Vessel vessel : vessels) ships.add(vessel.ship);
            engine = mock(CombatEngineAPI.class, (n, a) -> {
                switch (n) {
                    case "getShips": return ships;
                    case "getCustomData": return data;
                    case "getElapsedInLastFrame": return amount;
                    case "getTotalElapsedTime": return elapsed;
                    case "isPaused": return paused;
                    case "isEntityInPlay": return ships.contains(a[0]);
                    case "getPlayerShip": return player;
                    case "getMapWidth": case "getMapHeight": return 20000f;
                    case "isAwareOf": return visible;
                    case "spawnEmpArc": case "spawnEmpArcPierceShields":
                        if (a[3] instanceof ShipAPI target) struck.add(target);
                        break;
                    default: break;
                }
                return null;
            });
            Global.setCombatEngine(engine);
        }
    }

    static void testBrain() {
        Vessel brain = new Vessel("brain-a", AviciDwellerContent.BRAIN_HULL, 1, 0);
        Vessel second = new Vessel("brain-b", AviciDwellerContent.BRAIN_HULL, 1, 100);
        Vessel ally = new Vessel("banshee", AviciDwellerContent.BANSHEE_HULL, 1, 500);
        Vessel vanilla = new Vessel("maw", "shrouded_maw", 1, 600);
        Vessel player = new Vessel("player", AviciDwellerContent.BANSHEE_HULL, 0, 500);
        Vessel ordinary = new Vessel("ordinary", "onslaught", 1, 500);
        Vessel distant = new Vessel("distant", AviciDwellerContent.BANSHEE_HULL, 1, 6001);
        Arena arena = new Arena(brain, second, ally, vanilla, player, ordinary, distant);
        ShroudedBrainAuraStats first = new ShroudedBrainAuraStats();
        ShroudedBrainAuraStats other = new ShroudedBrainAuraStats();
        first.apply(brain.stats, "neural", State.ACTIVE, 1f);
        near(140f, ally.value("getMaxSpeed"), "Neural Web speed");
        near(130f, ally.value("getFluxDissipation"), "Neural Web dissipation");
        near(130f, vanilla.value("getEnergyWeaponDamageMult"), "Vanilla Dweller compatibility");
        near(100f, player.value("getMaxSpeed"), "Never buff the opposing side");
        near(100f, ordinary.value("getMaxSpeed"), "Only eligible Dwellers receive Neural Web");
        near(100f, distant.value("getMaxSpeed"), "Neural Web radius is bounded");
        first.apply(brain.stats, "neural", State.ACTIVE, 1f);
        near(140f, ally.value("getMaxSpeed"), "Neural Web cannot compound per frame");
        other.apply(second.stats, "neural", State.ACTIVE, 1f);
        near(180f, ally.value("getMaxSpeed"), "Distinct Brains own independent modifiers");
        first.unapply(brain.stats, "neural");
        near(140f, ally.value("getMaxSpeed"), "Stopping one Brain preserves the other Brain");
        ally.location.x = 7000;
        other.apply(second.stats, "neural", State.ACTIVE, 1f);
        near(100f, ally.value("getMaxSpeed"), "Leaving the aura removes its modifiers");
        vanilla.owner = 0;
        vanilla.alive = false;
        other.unapply(second.stats, "neural");
        near(100f, vanilla.value("getEnergyWeaponDamageMult"), "Dead or changed-side targets retain no stale buff");
        ally.location.x = 500;
        arena.paused = true;
        new ShroudedBrainAuraStats().apply(brain.stats, "paused", State.ACTIVE, 1f);
        near(100f, ally.value("getMaxSpeed"), "Paused combat does not advance Neural Web");
        arena.paused = false;
        first.apply(brain.stats, "death", State.ACTIVE, 1f);
        near(140f, ally.value("getMaxSpeed"), "Live source owns its active aura");
        brain.alive = false;
        new AviciDwellerHullmod().advanceInCombat(brain.ship, .1f);
        near(100f, ally.value("getMaxSpeed"), "Actual hullmod death callback clears source's allied buffs");
        brain.alive = true;
        first.apply(brain.stats, "removed", State.ACTIVE, 1f);
        near(140f, ally.value("getMaxSpeed"), "Reused source can own a fresh aura");
        arena.ships.remove(brain.ship);
        new AviciDwellerHullmod().advanceInCombat(brain.ship, .1f);
        near(100f, ally.value("getMaxSpeed"), "Actual hullmod removal callback clears source's allied buffs");
    }

    static void testBanshee() {
        Vessel banshee = new Vessel("banshee", AviciDwellerContent.BANSHEE_HULL, 1, 0);
        Arena arena = new Arena(banshee);
        ShroudBansheeStats system = new ShroudBansheeStats();
        system.apply(banshee.stats, "jump", State.ACTIVE, 1f);
        system.apply(banshee.stats, "jump", State.ACTIVE, 1f);
        near(150f, banshee.value("getEnergyWeaponDamageMult"), "Banshee damage buff is idempotent");
        near(120f, banshee.value("getFluxDissipation"), "Banshee dissipation buff");
        near(140f, banshee.value("getEnergyWeaponRangeBonus"), "Banshee range buff");
        near(0f, banshee.location.x, "No target means no teleport");
        system.unapply(banshee.stats, "jump");
        near(100f, banshee.value("getEnergyWeaponDamageMult"), "Banshee damage cleanup");
        near(100f, banshee.value("getFluxDissipation"), "Banshee dissipation cleanup");
        near(100f, banshee.value("getEnergyWeaponRangeBonus"), "Banshee range cleanup");
        Vessel enemy = new Vessel("enemy", "onslaught", 0, 900);
        arena.ships.add(enemy.ship);
        ShroudBansheeSystemAI.TeleportSolution solution =
                ShroudBansheeSystemAI.pickTeleportSolution(banshee.ship, arena.engine, true);
        check(solution != null && solution.target() == enemy.ship,
                "Banshee chooses an actual opposing target");
        check(ShroudBansheeSystemAI.isRearPoint(enemy.ship, solution.destination()),
                "Banshee destination is behind its target");
        check(ShroudBansheeSystemAI.isPointInsideMap(solution.destination(), arena.engine),
                "Banshee destination remains inside combat bounds");
        arena.visible = false;
        check(ShroudBansheeSystemAI.pickTeleportSolution(banshee.ship, arena.engine, true) == null,
                "Banshee cannot choose hidden enemies");
        arena.visible = true;
        enemy.owner = 1;
        check(ShroudBansheeSystemAI.pickTeleportSolution(banshee.ship, arena.engine, true) == null,
                "Banshee cannot teleport onto an ally");

        Vessel jumping = new Vessel("returning-banshee", AviciDwellerContent.BANSHEE_HULL, 1, 0);
        Vessel brain = new Vessel("return-brain", AviciDwellerContent.BRAIN_HULL, 1, -2000);
        Vessel prey = new Vessel("prey", "onslaught", 0, 900);
        Arena sequence = new Arena(jumping, brain, prey);
        ShroudBansheeStats returning = new ShroudBansheeStats();
        // With runScriptWhileIdle, the native system calls apply(effect=0)
        // through cooldown, rather than terminating with unapply at ten seconds.
        for (int frame = 0; frame < 40; frame++) {
            returning.apply(jumping.stats, "return", State.ACTIVE, 1f);
        }
        check(jumping.location.x > 0 && jumping.location.x < prey.location.x,
                "Actual Banshee system teleports behind its prey");
        float strikePosition = jumping.location.x;
        sequence.paused = true;
        for (int frame = 0; frame < 80; frame++) {
            returning.apply(jumping.stats, "return", State.COOLDOWN, 0f);
        }
        near(strikePosition, jumping.location.x, "Paused cooldown cannot advance the return clock");
        sequence.paused = false;
        for (int frame = 0; frame < 19; frame++) {
            returning.apply(jumping.stats, "return", State.COOLDOWN, 0f);
        }
        near(strikePosition, jumping.location.x, "Banshee remains behind prey until its return deadline");
        near(100f, jumping.value("getEnergyWeaponDamageMult"), "Cooldown drops the active attack buff");
        returning.apply(jumping.stats, "return", State.COOLDOWN, 0f);
        check(Vector2f.sub(jumping.location, brain.location, new Vector2f()).length() < 400f,
                "Banshee's fifteen-second return survives the shorter active system phase");
        float returnPosition = jumping.location.x;
        prey.location.set(returnPosition + 900f, jumping.location.y);
        returning.apply(jumping.stats, "return", State.ACTIVE, 1f);
        check(jumping.location.x > returnPosition + 100f,
                "A second activation starts a new teleport sequence");
        returning.unapply(jumping.stats, "return");
        near(100f, jumping.value("getEnergyWeaponDamageMult"), "Explicit Banshee cleanup removes attack modifiers");
    }

    static void testHunter() {
        Vessel hunter = new Vessel("budai", AviciDwellerContent.BUDAI_HULL, 1, 0);
        Vessel nearAlly = new Vessel("banshee", AviciDwellerContent.BANSHEE_HULL, 1, 500);
        Vessel farAlly = new Vessel("maw", "shrouded_maw", 1, 1500);
        Vessel ordinary = new Vessel("ordinary", "onslaught", 1, 500);
        Vessel player = new Vessel("player", "onslaught", 0, 500);
        Vessel friendly = new Vessel("friendly", "enforcer", 0, 1000);
        Vessel distantPlayer = new Vessel("outside", "onslaught", 0, 4001);
        Vessel neutral = new Vessel("neutral", "onslaught", 100, 500);
        Arena arena = new Arena(hunter, nearAlly, farAlly, ordinary, player,
                friendly, distantPlayer, neutral);
        arena.player = player.ship;
        AviciHunterSystem system = new AviciHunterSystem();
        near(1000f, AviciHunterDomain.getCurrentRadius(hunter.ship), "Resting domain radius");
        AviciHunterDomain.advance(hunter.ship, 1.2f);
        check(arena.struck.contains(player.ship) && arena.struck.contains(friendly.ship),
                "Resting bubble strikes the player's ships and friendlies, including its boundary");
        check(!arena.struck.contains(nearAlly.ship) && !arena.struck.contains(hunter.ship),
                "Lightning excludes Budai and his allies");
        check(!arena.struck.contains(distantPlayer.ship) && !arena.struck.contains(neutral.ship),
                "Lightning excludes ships outside the boundary and neutral owners");
        near(100f, nearAlly.value("getEnergyWeaponDamageMult"), "Inactive domain grants no system damage buff");
        system.apply(hunter.stats, "domain", State.ACTIVE, 1f);
        AviciHunterDomain.advance(hunter.ship, .8f);
        near(1480f, AviciHunterDomain.getCurrentRadius(hunter.ship), "Boundary expands continuously");
        near(170f, hunter.value("getEnergyWeaponDamageMult"), "Budai receives his active domain buff");
        near(170f, nearAlly.value("getEnergyWeaponDamageMult"), "Nearby Dweller receives active domain buff");
        near(100f, farAlly.value("getEnergyWeaponDamageMult"), "Buff never reaches beyond current boundary");
        near(100f, ordinary.value("getEnergyWeaponDamageMult"), "Ordinary non-Dwellers do not receive domain buff");
        near(100f, player.value("getEnergyWeaponDamageMult"), "Enemy ships never receive domain buff");
        near(125f, nearAlly.value("getFluxCapacity"), "Active domain flux capacity");
        near(125f, nearAlly.value("getFluxDissipation"), "Active domain flux dissipation");
        near(85f, nearAlly.value("getEnergyWeaponFluxCostMod"), "Active domain weapon flux costs");
        near(115f, nearAlly.value("getEnergyRoFMult"), "Active domain rate of fire");
        AviciHunterDomain.advance(hunter.ship, .2f);
        near(1600f, AviciHunterDomain.getCurrentRadius(hunter.ship), "One second of expansion");
        near(170f, farAlly.value("getEnergyWeaponDamageMult"), "Expanding edge admits newly enclosed allies");
        AviciHunterDomain.advance(hunter.ship, 4f);
        near(4000f, AviciHunterDomain.getCurrentRadius(hunter.ship), "Five unpaused seconds reaches maximum radius");
        AviciHunterDomain.advance(hunter.ship, 20f);
        near(4000f, AviciHunterDomain.getCurrentRadius(hunter.ship), "Radius cannot exceed authored maximum");
        near(170f, nearAlly.value("getEnergyWeaponDamageMult"), "Domain modifiers cannot compound per frame");
        arena.paused = true;
        system.unapply(hunter.stats, "domain");
        AviciHunterDomain.advance(hunter.ship, 10f);
        near(4000f, AviciHunterDomain.getCurrentRadius(hunter.ship), "Paused combat freezes contraction");
        near(100f, nearAlly.value("getEnergyWeaponDamageMult"), "System collapse immediately removes damage buff");
        near(100f, nearAlly.value("getEnergyWeaponFluxCostMod"), "System collapse removes flux modifier");
        check(hunter.overloads == 1, "Collapse overload fires once for one activation");
        near(6f, hunter.overloadDuration, "Native six-second collapse overload retained");
        system.unapply(hunter.stats, "domain");
        check(hunter.overloads == 1, "Repeated idle unapply cannot retrigger overload");
        arena.paused = false;
        AviciHunterDomain.advance(hunter.ship, 2.5f);
        near(2500f, AviciHunterDomain.getCurrentRadius(hunter.ship), "Boundary contracts continuously");
        AviciHunterDomain.advance(hunter.ship, 2.5f);
        near(1000f, AviciHunterDomain.getCurrentRadius(hunter.ship), "Boundary returns to small resting radius");
        system.apply(hunter.stats, "domain", State.ACTIVE, 1f);
        AviciHunterDomain.advance(hunter.ship, 1f);
        nearAlly.owner = 0;
        farAlly.alive = false;
        AviciHunterDomain.advance(hunter.ship, .1f);
        near(100f, nearAlly.value("getEnergyWeaponDamageMult"), "Changed-side target loses its domain buff");
        near(100f, farAlly.value("getEnergyWeaponDamageMult"), "Destroyed target loses its domain buff");
        near(100f, hunter.value("getHullDamageTakenMult"), "Domain adds no hull damage attenuation");
        near(100f, hunter.value("getArmorDamageTakenMult"), "Domain adds no armor damage attenuation");
        check(hunter.listeners.stream().noneMatch(DamageTakenModifier.class::isInstance),
                "Domain installs no outside-shot damage modifier");
        hunter.alive = false;
        AviciHunterDomain.advance(hunter.ship, .1f);
        check(!hunter.data.containsKey(AviciHunterDomain.STATE_KEY), "Dead Budai releases domain state");
        near(100f, hunter.value("getEnergyWeaponDamageMult"), "Dead Budai releases his system buffs");
        near(100f, farAlly.value("getFluxCapacity"), "Cleanup preserves no stale ally modifiers");
        hunter.alive = true;
        nearAlly.owner = 1;
        AviciHunterDomain.applySystem(hunter.ship, 1f);
        AviciHunterDomain.advance(hunter.ship, .1f);
        near(170f, nearAlly.value("getEnergyWeaponDamageMult"), "Fresh domain buffs a nearby ally");
        arena.ships.remove(hunter.ship);
        new AviciDwellerHullmod().advanceInCombat(hunter.ship, .1f);
        near(100f, nearAlly.value("getEnergyWeaponDamageMult"), "Actual hullmod removal clears Hunter buffs");
        check(!hunter.data.containsKey(AviciHunterDomain.STATE_KEY), "Removal releases the Hunter's visual/state ownership");
        Vessel sepHunter = new Vessel("sep", "insatiable_hunger", 1, 0);
        AviciHunterDomain.applySystem(sepHunter.ship, 1f);
        check(!sepHunter.data.containsKey(AviciHunterDomain.STATE_KEY),
                "Our domain never adopts or mutates SEP's independent hull");
    }

    static void testCreatorIsolation(boolean sepPresent) throws Exception {
        Map<String, DwellerShipCreator> vanillaRegistry = DwellerHullmod.SHIP_CREATORS;
        Map<String, DwellerShipCreator> vanillaBefore = new HashMap<>(vanillaRegistry);
        Map<?, ?> sepRegistry = null;
        Map<?, ?> sepBefore = null;
        if (sepPresent) {
            Class<?> sepHullmod = Class.forName("data.hullmods.DwellerHullmodTidal");
            sepRegistry = (Map<?, ?>) sepHullmod.getField("SHIP_CREATORS").get(null);
            sepBefore = new HashMap<>(sepRegistry);
            check(sepRegistry.containsKey("shrouded_banshee") && sepRegistry.containsKey("insatiable_hunger"),
                    "The coexistence fixture must load the actual installed SEP registry");
        }
        AviciDwellerHullmod own = new AviciDwellerHullmod();
        java.lang.reflect.Method creator = AviciDwellerHullmod.class.getDeclaredMethod("getShipCreator", String.class);
        creator.setAccessible(true);
        for (String hull : List.of(AviciDwellerContent.BANSHEE_HULL,
                AviciDwellerContent.BRAIN_HULL, AviciDwellerContent.BUDAI_HULL)) {
            Object selected = creator.invoke(own, hull);
            check(selected != null && selected.getClass().getPackageName().equals("chiefnavigator.dwellers"),
                    "Our hullmod selects its own creator for " + hull);
        }
        for (String foreign : List.of("shrouded_maw", "shrouded_brain", "shrouded_banshee", "insatiable_hunger")) {
            check(creator.invoke(own, foreign) == null, "Our hullmod never adopts foreign hull " + foreign);
        }
        check(DwellerHullmod.SHIP_CREATORS == vanillaRegistry && vanillaRegistry.equals(vanillaBefore),
                "Our content never mutates the global vanilla creator registry");
        if (sepPresent) {
            check(sepRegistry.equals(sepBefore), "Our content never mutates SEP's independent creator registry");
            System.out.println("PASS: actual installed SEP registry coexists without replacement or mutation");
        }
    }

    static void testNativeRegistries() throws Exception {
        // Keep native loader internals in this external test, never production.
        java.lang.reflect.Method parser = Class.forName("com.fs.starfarer.loading.G")
                .getMethod("o00000", String.class);
        Map<String, Set<String>> expected = new LinkedHashMap<>();
        expected.put("data/hulls/ship_data.csv", Set.of(AviciDwellerContent.BANSHEE_HULL,
                AviciDwellerContent.BRAIN_HULL, AviciDwellerContent.BUDAI_HULL));
        expected.put("data/hullmods/hull_mods.csv", Set.of(AviciDwellerContent.HULLMOD));
        expected.put("data/shipsystems/ship_systems.csv", Set.of("chief_navigator_avici_banshee_jump",
                "chief_navigator_avici_brain_aura", "chief_navigator_avici_hunter_domain"));
        expected.put("data/strings/descriptions.csv", Set.of(AviciDwellerContent.BANSHEE_HULL,
                AviciDwellerContent.BRAIN_HULL, AviciDwellerContent.BUDAI_HULL));
        for (Map.Entry<String, Set<String>> registration : expected.entrySet()) {
            String source = Files.readString(Path.of(registration.getKey()), StandardCharsets.UTF_8);
            org.json.JSONArray rows = (org.json.JSONArray) parser.invoke(null, source);
            Set<String> found = new HashSet<>();
            for (int index = 0; index < rows.length(); index++) {
                org.json.JSONObject row = rows.getJSONObject(index);
                String id = row.optString("id").trim();
                if (registration.getValue().contains(id)) {
                    check(found.add(id), "Native CSV contains duplicate ported registration: " + id);
                }
            }
            check(found.equals(registration.getValue()),
                    "Native loader lost an enemy registration in " + registration.getKey());
        }
    }

    public static void main(String[] args) throws Exception {
        SpriteAPI sprite = mock(SpriteAPI.class, (n, a) ->
                n.equals("getWidth") || n.equals("getHeight") ? 64f : null);
        org.json.JSONObject shaderSettings = new org.json.JSONObject();
        shaderSettings.put("useLargeRipple", false);
        Global.setSettings(mock(SettingsAPI.class, (n, a) -> switch (n) {
            case "getColor" -> Color.WHITE;
            case "getSprite" -> sprite;
            case "loadJSON" -> shaderSettings;
            default -> null;
        }));
        testNativeRegistries();
        testCreatorIsolation(Arrays.asList(args).contains("--sep-present"));
        testBrain();
        testBanshee();
        testHunter();
        System.out.println("PASS: ported Neural Web, Banshee targeting/buffs, and localized Hunter domain ownership/cleanup");
    }
}
