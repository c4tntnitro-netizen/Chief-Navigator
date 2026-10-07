import chiefnavigator.hullmods.CharybdisDistortion;
import chiefnavigator.systems.BudaiConvulsiveLungeAI;
import chiefnavigator.systems.BudaiConvulsiveLungeSystem;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
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
            CombatEngineAPI engine = WallResearchRegression.mock(CombatEngineAPI.class,
                    (name, args) -> {
                        switch (name) {
                            case "isPaused": return paused;
                            case "isEntityInPlay": return true;
                            case "getShips": return ships;
                            case "getCustomData": return engineData;
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
                "preventRetreat", ShipAPI.class);
        preventRetreat.setAccessible(true);
        for (float hull : new float[] {1f, 0.2f, 0.1f}) {
            f = new Fixture();
            f.hull = hull;
            f.retreating = true;
            f.flags.setFlag(ShipwideAIFlags.AIFlags.BACKING_OFF);
            preventRetreat.invoke(null, f.ship);
            check(!f.retreating, "Prevent retreat at every hull level");
            check(!f.flags.hasFlag(ShipwideAIFlags.AIFlags.BACKING_OFF)
                            && f.flags.hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF)
                            && f.flags.hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF_EVEN_WHILE_VENTING),
                    "Hold ground rather than backing off, including at low hull and while venting");
        }
        verifyNativeSmoke();
        verifyBubbleDamage();
        System.out.println("PASS: Budai rejects automatic/native rearward escapes, retains forward "
                + "lunges and cooldown/readiness guards, holds ground while venting, preserves "
                + "enrage priority, no retreat, normal smoke and 30% bubble damage reduction");
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
        for (float radius : new float[] {1000f, 4000f}) {
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
}
