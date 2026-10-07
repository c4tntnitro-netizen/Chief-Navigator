import chiefnavigator.hullmods.CharybdisDistortion;
import chiefnavigator.systems.BudaiConvulsiveLungeAI;
import chiefnavigator.systems.BudaiConvulsiveLungeSystem;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.dweller.DwellerShroud;
import com.fs.starfarer.api.impl.combat.dweller.DwellerShroud.DwellerShroudParams;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;

/** Exercises fearless AI decisions, enrage ownership, cooldown and retreat guards. */
public final class BudaiMovementRegression {
    private static final class Fixture {
        final Map<String, Object> data = new HashMap<>();
        final Map<String, Object> engineData = new HashMap<>();
        final List<ShipAPI> ships = new ArrayList<>();
        final ShipwideAIFlags flags = new ShipwideAIFlags();
        final Vector2f position = new Vector2f();
        final Vector2f enemyPosition = new Vector2f(1000, 0);
        final BudaiConvulsiveLungeAI ai = new BudaiConvulsiveLungeAI();
        final ShipAIConfig config = new ShipAIConfig();
        final ShipAIPlugin shipAI;
        final DeployedFleetMemberAPI deployed;
        final CombatTaskManagerAPI tasks;
        final CombatFleetManagerAPI manager;
        final CombatEngineAPI engine;
        CombatAssignmentType assignment = CombatAssignmentType.RETREAT;
        boolean forceEngage;
        boolean preventFullRetreat;
        int huntOrders;
        int evaluations;
        int maneuverCancels;
        float facing;
        float hull = 1f;
        float cooldown;
        boolean paused;
        boolean overloaded;
        boolean usable = true;
        boolean enemyAlive = true;
        boolean enemyFighter;
        boolean retreating;
        int enemyOwner = 0;
        int commands;
        float elapsed;
        ShipSystemAPI.SystemState state = ShipSystemAPI.SystemState.IDLE;
        final ShipAPI ship;

        Fixture() {
            shipAI = WallResearchRegression.mock(ShipAIPlugin.class, (name, args) -> {
                if (name.equals("getConfig")) return config;
                if (name.equals("forceCircumstanceEvaluation")) evaluations++;
                if (name.equals("cancelCurrentManeuver")) maneuverCancels++;
                return null;
            });
            deployed = WallResearchRegression.mock(DeployedFleetMemberAPI.class, (name, args) -> null);
            CombatFleetManagerAPI.AssignmentInfo order = WallResearchRegression.mock(
                    CombatFleetManagerAPI.AssignmentInfo.class,
                    (name, args) -> name.equals("getType") ? assignment : null);
            tasks = WallResearchRegression.mock(CombatTaskManagerAPI.class, (name, args) -> {
                switch (name) {
                    case "getAssignmentFor": return order;
                    case "isPreventFullRetreat": return preventFullRetreat;
                    case "setPreventFullRetreat": preventFullRetreat = (Boolean) args[0]; break;
                    case "orderSearchAndDestroy":
                        check(args.length == 2 && args[0] == deployed && Boolean.FALSE.equals(args[1]),
                                "Replace only Budai's own order, never the fleet's orders");
                        assignment = CombatAssignmentType.SEARCH_AND_DESTROY;
                        huntOrders++;
                        break;
                    case "removeAssignment": case "clearTasks": case "setFullAssault":
                        throw new AssertionError("Do not erase shared orders or force fleet-wide assault");
                    default: break;
                }
                return null;
            });
            manager = WallResearchRegression.mock(CombatFleetManagerAPI.class, (name, args) -> {
                switch (name) {
                    case "getTaskManager": return tasks;
                    case "getDeployedFleetMember": return deployed;
                    case "isCanForceShipsToEngageWhenBattleClearlyLost": return forceEngage;
                    case "setCanForceShipsToEngageWhenBattleClearlyLost": forceEngage = (Boolean) args[0]; break;
                    default: break;
                }
                return null;
            });
            ShipSystemAPI system = WallResearchRegression.mock(ShipSystemAPI.class,
                    (name, args) -> {
                        if (name.equals("getState")) return state;
                        if (name.equals("getCooldownRemaining")) return cooldown;
                        if (name.equals("isActive")) return state == ShipSystemAPI.SystemState.IN
                                || state == ShipSystemAPI.SystemState.ACTIVE
                                || state == ShipSystemAPI.SystemState.OUT;
                        if (name.equals("canBeActivated")) return usable;
                        return null;
                    });
            FluxTrackerAPI flux = WallResearchRegression.mock(FluxTrackerAPI.class,
                    (name, args) -> name.equals("isOverloadedOrVenting") ? overloaded : null);
            ship = WallResearchRegression.mock(ShipAPI.class, (name, args) -> {
                switch (name) {
                    case "getCustomData": return data;
                    case "setCustomData": data.put((String) args[0], args[1]); break;
                    case "removeCustomData": data.remove(args[0]); break;
                    case "getAIFlags": return flags;
                    case "getShipAI": return shipAI;
                    case "setShipAI": case "resetDefaultAI":
                        throw new AssertionError("Preserve the existing native ship/system AI");
                    case "getOwner": return 1;
                    case "getHullSize": return ShipAPI.HullSize.CAPITAL_SHIP;
                    case "isAlive": case "isCapital": return true;
                    case "getHullLevel": return hull;
                    case "getFacing": return facing;
                    case "getLocation": return position;
                    case "getVelocity": return new Vector2f();
                    case "getCollisionRadius": return 100f;
                    case "getSystem": return system;
                    case "getFluxTracker": return flux;
                    case "isRetreating": return retreating;
                    case "setRetreating": retreating = (Boolean) args[0]; break;
                    case "giveCommand":
                        if (args[0] == ShipCommand.USE_SYSTEM) commands++;
                        break;
                    default: break;
                }
                return null;
            });
            ShipAPI enemy = WallResearchRegression.mock(ShipAPI.class, (name, args) -> {
                switch (name) {
                    case "getLocation": return enemyPosition;
                    case "getCollisionRadius": return 100f;
                    case "isAlive": return enemyAlive;
                    case "isFighter": return enemyFighter;
                    case "getOwner": return enemyOwner;
                    case "getHullSize": return ShipAPI.HullSize.CRUISER;
                    default: return null;
                }
            });
            ships.add(ship);
            ships.add(enemy);
            engine = WallResearchRegression.mock(CombatEngineAPI.class,
                    (name, args) -> {
                        switch (name) {
                            case "isPaused": return paused;
                            case "isEntityInPlay": return true;
                            case "getShips": return ships;
                            case "getCustomData": return engineData;
                            case "getFleetManager":
                                check((Integer) args[0] == 1, "Never change the opposing fleet manager");
                                return manager;
                            case "addLayeredRenderingPlugin": return ship;
                            case "getTotalElapsedTime": return elapsed;
                            case "getMapWidth": case "getMapHeight": return 10000f;
                            default: return null;
                        }
                    });
            Global.setCombatEngine(engine);
            ai.init(ship, system, flags, engine);
        }

        void advance(float amount) {
            if (!paused) elapsed += amount;
            ai.advance(amount, null, null, null);
        }

        Vector2f destination() {
            return (Vector2f) data.get(BudaiConvulsiveLungeSystem.TARGET_KEY);
        }
    }

    public static void main(String[] args) throws Exception {
        Global.setSettings(WallResearchRegression.mock(com.fs.starfarer.api.SettingsAPI.class,
                (name, values) -> {
                    if (name.equals("getColor")) return java.awt.Color.WHITE;
                    if (name.equals("getAngleInDegreesFast")) {
                        Vector2f direction = values.length == 1 ? (Vector2f) values[0]
                                : Vector2f.sub((Vector2f) values[1], (Vector2f) values[0], null);
                        return (float) Math.toDegrees(Math.atan2(direction.y, direction.x));
                    }
                    return null;
                }));
        for (float facing : new float[] {0f, 90f, 180f, 270f}) {
            Fixture f = new Fixture();
            f.facing = facing;
            double radians = Math.toRadians(facing);
            f.enemyPosition.set((float) Math.cos(radians) * 1000f,
                    (float) Math.sin(radians) * 1000f);
            f.advance(6.1f);
            check(f.commands == 0 && f.destination() == null,
                    "A close enemy must no longer trigger automatic backward jumps at " + facing);
            f.hull = 0.1f;
            f.advance(1.3f);
            check(f.commands == 0,
                    "The actual native hull-loss pullback is rejected at facing " + facing);
            f.ai.giveCommand(new Vector2f((float) Math.cos(radians) * -1000f,
                    (float) Math.sin(radians) * -1000f));
            check(f.commands == 0, "Explicit native rearward commands are rejected");
            f.ai.giveCommand(new Vector2f((float) Math.cos(radians) * 1000f,
                    (float) Math.sin(radians) * 1000f));
            check(f.commands == 1 && f.destination() == null,
                    "Forward native commands remain available at every hull facing");
        }

        Fixture f = new Fixture();
        f.ai.giveCommand(new Vector2f(1000, 0));
        f.state = ShipSystemAPI.SystemState.ACTIVE;
        f.advance(2f);
        f.ai.giveCommand(new Vector2f(1000, 0));
        check(f.commands == 1, "Never queue another command during a lunge");
        f.state = ShipSystemAPI.SystemState.COOLDOWN;
        f.cooldown = 2f;
        f.advance(2f);
        check(f.destination() == null && f.commands == 1,
                "Respect normal cooldown after a completed forward lunge");
        f.cooldown = 0f;
        f.paused = true;
        f.advance(100f);
        f.ai.giveCommand(new Vector2f(1000, 0));
        check(f.commands == 1, "Pause must not activate a lunge");
        f.paused = false;
        f.ai.giveCommand(new Vector2f(1000, 0));
        check(f.commands == 2, "Forward lunges resume when the system is ready");

        f = new Fixture();
        f.ai.giveCommand(new Vector2f(1000, 0));
        f.state = ShipSystemAPI.SystemState.ACTIVE;
        f.advance(5.5f);
        f.state = ShipSystemAPI.SystemState.COOLDOWN;
        for (int second = 0; second < 12; second++) {
            f.cooldown = 12f - second;
            f.advance(1f);
            f.ai.giveCommand(new Vector2f(1000, 0));
            check(f.commands == 1,
                    "Offensive AI must wait through Budai's twelve-second cooldown");
        }
        f.cooldown = 0f;
        f.state = ShipSystemAPI.SystemState.IDLE;
        f.ai.giveCommand(new Vector2f(1000, 0));
        check(f.commands == 2 && f.destination() == null,
                "Forward movement becomes available again after the full cooldown");

        f = new Fixture();
        Vector2f enrageDestination = new Vector2f(321, 456);
        f.data.put(CharybdisDistortion.ENRAGE_SEQUENCE_KEY, new Object());
        f.data.put(BudaiConvulsiveLungeSystem.TARGET_KEY, enrageDestination);
        f.advance(20f);
        f.ai.giveCommand(new Vector2f(1000, 0));
        check(f.commands == 0 && f.destination() == enrageDestination,
                "The random enrage sequence exclusively owns its destination");
        f.data.remove(CharybdisDistortion.ENRAGE_SEQUENCE_KEY);
        f.advance(0.1f);
        check(f.commands == 0, "Leaving enrage must not produce an automatic backstep");

        for (String condition : new String[] {
                "overloaded", "cooldown", "disabled"}) {
            f = new Fixture();
            switch (condition) {
                case "overloaded": f.overloaded = true; break;
                case "cooldown": f.cooldown = 2f; break;
                case "disabled": f.usable = false; break;
                default: break;
            }
            f.advance(0.1f);
            f.ai.giveCommand(new Vector2f(1000, 0));
            check(f.commands == 0, "No forward command when " + condition);
        }

        f = new Fixture();
        f.data.put(BudaiConvulsiveLungeSystem.TARGET_KEY, new Vector2f(-1000, 0));
        f.ai.giveCommand(new Vector2f(1000, 0));
        check(f.destination() == null, "Stock forward lunges must discard stale rear targets");

        Method preventRetreat = CharybdisDistortion.class.getDeclaredMethod(
                "preventRetreat", CombatEngineAPI.class, ShipAPI.class);
        preventRetreat.setAccessible(true);
        for (float hull : new float[] {1f, 0.2f, 0.1f}) {
            f = new Fixture();
            f.hull = hull;
            f.retreating = true;
            f.flags.setFlag(ShipwideAIFlags.AIFlags.BACK_OFF);
            f.flags.setFlag(ShipwideAIFlags.AIFlags.BACKING_OFF);
            preventRetreat.invoke(null, f.engine, f.ship);
            check(!f.retreating, "Prevent retreat at every hull level");
            check(!f.flags.hasFlag(ShipwideAIFlags.AIFlags.BACK_OFF)
                            && !f.flags.hasFlag(ShipwideAIFlags.AIFlags.BACKING_OFF)
                            && f.flags.hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF)
                            && f.flags.hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF_EVEN_WHILE_VENTING),
                    "Hold ground rather than backing off, including at low hull and while venting");
            check(f.forceEngage && f.preventFullRetreat && f.huntOrders == 1,
                    "Block defeat-driven withdrawal and replace Budai's existing retreat order");
            check("reckless".equals(f.config.personalityOverride)
                            && f.config.alwaysStrafeOffensively && !f.config.backingOffWhileNotVentingAllowed,
                    "Configure Fearless in place, retaining native Maw AI and system modules");
            preventRetreat.invoke(null, f.engine, f.ship);
            check(f.huntOrders == 1 && f.evaluations == 1 && f.maneuverCancels == 1,
                    "Ordinary frames must not keep replacing orders or rebuilding/evaluating AI");
            f.assignment = CombatAssignmentType.RETREAT;
            preventRetreat.invoke(null, f.engine, f.ship);
            check(f.huntOrders == 2, "Cancel a later native retreat order too");
            method("restoreRetreatGuard", ShipAPI.class).invoke(null, f.ship);
            check(!f.forceEngage && !f.preventFullRetreat,
                    "Restore the side's original withdrawal settings when Budai leaves");
        }
        f = new Fixture();
        f.forceEngage = true;
        f.preventFullRetreat = true;
        f.assignment = CombatAssignmentType.DEFEND;
        preventRetreat.invoke(null, f.engine, f.ship);
        method("restoreRetreatGuard", ShipAPI.class).invoke(null, f.ship);
        check(f.forceEngage && f.preventFullRetreat && f.huntOrders == 0,
                "Preserve preexisting fleet settings and non-retreat assignments");
        verifyNativeSmoke();
        verifySmokeTransition();
        verifyBubbleDamage();
        System.out.println("PASS: Budai rejects automatic/native rearward escapes, retains forward "
                + "lunges and cooldown/readiness guards, holds ground while venting, preserves "
                + "enrage priority, no retreat orders, temporary heavy native smoke, five-second "
                + "bubble expansion and 30% bubble damage reduction");
    }

    private static void verifySmokeTransition() throws Exception {
        Fixture f = new Fixture();
        Method smoke = method("updateEnrageSmoke", ShipAPI.class);
        Method grow = method("advanceBubbleExpansion", CombatEngineAPI.class, ShipAPI.class, float.class);
        f.data.put(CharybdisDistortion.ENRAGE_SEQUENCE_KEY, new Object());
        smoke.invoke(null, f.ship); // Native shroud is not available yet.
        check(!f.data.containsKey("chief_navigator_charybdis_enrage_smoke"),
                "Missing native shroud must retry without capturing a fake baseline");
        DwellerShroudParams params = new DwellerShroudParams();
        params.negativeParticleNumBase = 11;
        params.negativeParticleNumOverloaded = 5;
        params.negativeParticleGenRate = 1f;
        params.baseMembersToMaintain = 250;
        params.negativeParticleAreaMult = 0.9f;
        params.negativeParticleDurMult = 1f;
        new DwellerShroud(f.ship, params); // Real native registration/lookup, no rendering advance.
        for (int frame = 0; frame < 100; frame++) smoke.invoke(null, f.ship);
        check(params.negativeParticleNumBase == 44 && params.negativeParticleNumOverloaded == 20
                        && params.negativeParticleGenRate == 2f,
                "Heavy smoke is four times the count at twice the emission rate without compounding");
        check(params.baseMembersToMaintain == 250 && params.negativeParticleAreaMult == 0.9f
                        && params.negativeParticleDurMult == 1f,
                "Preserve native particle pool, texture/spread/lifetime and movement");

        String expansionKey = "chief_navigator_charybdis_bubble_expansion";
        var constructor = Class.forName("chiefnavigator.hullmods.CharybdisDistortion$BubbleExpansion")
                .getDeclaredConstructor(float.class);
        constructor.setAccessible(true);
        f.data.remove(CharybdisDistortion.ENRAGE_SEQUENCE_KEY);
        f.data.put(expansionKey, constructor.newInstance(1000f));
        grow.invoke(null, f.engine, f.ship, 2.5f);
        smoke.invoke(null, f.ship);
        near(((Number) f.data.get("chief_navigator_charybdis_distortion_radius")).floatValue(), 2500f,
                "Bubble reaches the midpoint after 2.5 unpaused seconds");
        check(params.negativeParticleNumBase == 28 && params.negativeParticleNumOverloaded == 13
                        && params.negativeParticleGenRate == 1.5f,
                "Smoke emission eases back as the bubble expands");
        f.paused = true;
        grow.invoke(null, f.engine, f.ship, 20f);
        f.paused = false;
        grow.invoke(null, f.engine, f.ship, 0f);
        grow.invoke(null, f.engine, f.ship, -5f);
        near(((Number) f.data.get("chief_navigator_charybdis_distortion_radius")).floatValue(), 2500f,
                "Pause and non-positive amounts cannot advance the transition");
        grow.invoke(null, f.engine, f.ship, 2.5f);
        smoke.invoke(null, f.ship);
        near(((Number) f.data.get("chief_navigator_charybdis_distortion_radius")).floatValue(), 4000f,
                "Full radius is reached after exactly five unpaused seconds");
        check(!f.data.containsKey(expansionKey) && params.negativeParticleNumBase == 11
                        && params.negativeParticleNumOverloaded == 5 && params.negativeParticleGenRate == 1f,
                "Completed expansion restores baseline smoke even while still below half hull");
        f.data.put(CharybdisDistortion.ENRAGE_SEQUENCE_KEY, new Object());
        smoke.invoke(null, f.ship);
        method("clearEnrage", ShipAPI.class).invoke(null, f.ship);
        check(params.negativeParticleNumBase == 11 && params.negativeParticleGenRate == 1f
                        && !f.data.containsKey(CharybdisDistortion.ENRAGE_SEQUENCE_KEY),
                "Death/removal cleanup restores the native shroud and clears phase ownership");
    }

    private static void verifyNativeSmoke() throws Exception {
        Fixture f = new Fixture();
        Field creatorField = CharybdisDistortion.class.getDeclaredField("CHARYBDIS_CREATOR");
        creatorField.setAccessible(true);
        Object creator = creatorField.get(null);
        Method baseline = creator.getClass().getDeclaredMethod(
                "modifyBaselineShroudParams", ShipAPI.class, DwellerShroudParams.class);
        baseline.setAccessible(true);
        for (boolean enraged : new boolean[] {false, true}) {
            if (enraged) f.data.put("chief_navigator_charybdis_enraged", Boolean.TRUE);
            DwellerShroudParams params = new DwellerShroudParams();
            params.spriteKey = "existing_native_shroud";
            params.color = java.awt.Color.BLUE;
            baseline.invoke(creator, f.ship, params);
            check(params.baseMembersToMaintain == 250
                            && params.negativeParticleNumBase == 11
                            && params.negativeParticleNumOverloaded == 5,
                    "Ordinary and enraged Budai retain native Maw smoke counts");
            check(Math.abs(params.negativeParticleAreaMult - 0.9f) < 0.001f
                            && Math.abs(params.negativeParticleDurMult - 1f) < 0.001f
                            && Math.abs(params.maxOffset - 600f) < 0.001f,
                    "No enrage smoke spread/lifetime boost; retain authored 1.5x body scale");
            check(params.color.equals(java.awt.Color.BLUE)
                            && params.spriteKey.equals("existing_native_shroud"),
                    "Keep the ordinary native smoke texture and Budai's established color");
        }
    }

    private static void verifyBubbleDamage() throws Exception {
        Fixture f = new Fixture();
        Class<?> listenerClass = Class.forName(
                "chiefnavigator.hullmods.CharybdisDistortion$OutsideBubbleDamageModifier");
        var constructor = listenerClass.getDeclaredConstructor(ShipAPI.class);
        constructor.setAccessible(true);
        Object listener = constructor.newInstance(f.ship);
        Method hit = listenerClass.getDeclaredMethod("modifyDamageTaken", Object.class,
                CombatEntityAPI.class, DamageAPI.class, Vector2f.class, boolean.class);
        hit.setAccessible(true);
        for (float radius : new float[] {1000f, 2500f, 4000f}) {
            f.data.put("chief_navigator_charybdis_distortion_radius", radius);
            for (String scenario : new String[] {
                    "projectile", "beam", "inside", "boundary", "heHull", "heShield", "unknown", "foreign"}) {
                MutableStat modifier = new MutableStat(100f);
                DamageAPI damage = WallResearchRegression.mock(DamageAPI.class, (name, args) -> {
                    if (name.equals("getModifier")) return modifier;
                    if (name.equals("getType")) return scenario.startsWith("he")
                            ? DamageType.HIGH_EXPLOSIVE : DamageType.ENERGY;
                    return null;
                });
                Vector2f origin = new Vector2f(scenario.equals("inside") ? radius - 1f
                        : scenario.equals("boundary") ? radius : radius + 1f, 0f);
                Object source = scenario.equals("unknown") ? new Object()
                        : scenario.equals("beam") ? WallResearchRegression.mock(BeamAPI.class,
                                (name, args) -> name.equals("getFrom") ? origin : null)
                        : WallResearchRegression.mock(DamagingProjectileAPI.class,
                                (name, args) -> name.equals("getSpawnLocation") ? origin : null);
                CombatEntityAPI target = scenario.equals("foreign")
                        ? WallResearchRegression.mock(ShipAPI.class, (name, args) -> null) : f.ship;
                hit.invoke(listener, source, target, damage, f.position, scenario.equals("heShield"));
                boolean attenuated = !List.of("inside", "boundary", "heHull", "unknown", "foreign")
                        .contains(scenario);
                check(Math.abs(modifier.getModifiedValue() - (attenuated ? 70f : 100f)) < 0.001f,
                        "30% reduction and existing origin/HE-hull/ownership guards at " + radius + ": " + scenario);
            }
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static Method method(String name, Class<?>... params) throws Exception {
        Method result = CharybdisDistortion.class.getDeclaredMethod(name, params);
        result.setAccessible(true);
        return result;
    }

    private static void near(float actual, float expected, String message) {
        check(Math.abs(actual - expected) < 0.001f, message + "; actual=" + actual);
    }
}
