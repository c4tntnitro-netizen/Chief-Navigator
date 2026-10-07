import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import data.hullmods.AxialRotation;
import java.awt.Color;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;

/** Actual center-fight deployment/anchor callbacks with the native rotation hullmod. */
public final class FinalLaborStationRegression extends WallResearchRegression {
    private static final String VARIANT = "chief_navigator_spartan_battlestation_Teal";

    private static final class Station {
        final Vector2f position = new Vector2f(), velocity = new Vector2f();
        final List<ShipAPI> modules = new ArrayList<>();
        final Map<String, Object> data = new HashMap<>();
        final MutableStat speed = new MutableStat(20f), acceleration = new MutableStat(10f);
        final MutableStat deceleration = new MutableStat(10f), turn = new MutableStat(2f);
        final MutableStat turnAcceleration = new MutableStat(2f);
        final MutableStat hullDamage = new MutableStat(1f), armorDamage = new MutableStat(1f);
        final ShipAPI ship;
        int owner, rightCommands;
        float facing, angularVelocity;
        boolean noDamageFlashes, controlsLocked = true;

        Station(String variantId, int owner) {
            this.owner = owner;
            turn.modifyMult("external_turn_modifier", 1.25f);
            turnAcceleration.modifyMult("external_turn_modifier", .75f);
            ShipVariantAPI variant = mock(ShipVariantAPI.class,
                    (n, a) -> n.equals("getHullVariantId") ? variantId : null);
            MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, (n, a) -> {
                switch (n) {
                    case "getMaxSpeed": return speed;
                    case "getAcceleration": return acceleration;
                    case "getDeceleration": return deceleration;
                    case "getMaxTurnRate": return turn;
                    case "getTurnAcceleration": return turnAcceleration;
                    case "getHullDamageTakenMult": return hullDamage;
                    case "getArmorDamageTakenMult": return armorDamage;
                }
                return null;
            });
            ship = mock(ShipAPI.class, (n, a) -> {
                switch (n) {
                    case "getVariant": return variant;
                    case "getMutableStats": return stats;
                    case "getChildModulesCopy": return new ArrayList<>(modules);
                    case "getCustomData": return data;
                    case "setCustomData": data.put((String) a[0], a[1]); break;
                    case "getLocation": return position;
                    case "getVelocity": return velocity;
                    case "getOwner": return this.owner;
                    case "setOwner": this.owner = (Integer) a[0]; break;
                    case "isAlive": return true;
                    case "getFacing": return facing;
                    case "setFacing": facing = (Float) a[0]; break;
                    case "getAngularVelocity": return angularVelocity;
                    case "setAngularVelocity": angularVelocity = (Float) a[0]; break;
                    case "setControlsLocked": controlsLocked = (Boolean) a[0]; break;
                    case "setNoDamagedExplosions": noDamageFlashes = (Boolean) a[0]; break;
                    case "giveCommand":
                        if (a[0] == ShipCommand.TURN_RIGHT) rightCommands++;
                        break;
                    case "setHitpoints": case "setHulk": case "setExplosionScale":
                        throw new AssertionError("Visual/anchor fix cannot change damage or death: " + n);
                }
                return null;
            });
        }
    }

    public static void main(String[] args) throws Exception {
        Global.setSettings(mock(SettingsAPI.class,
                (n, a) -> n.equals("getColor") ? Color.WHITE : null));
        Station persistent = new Station(VARIANT, 0);
        Station foreign = new Station("station2_Standard", 0);
        Station enemy = new Station(VARIANT, 1);
        Station[] reserves = {new Station(VARIANT, 0), new Station(VARIANT, 0)};
        Station[] modules = {new Station("module", 0), new Station("module", 0), new Station("module", 0)};
        persistent.modules.add(modules[0].ship);
        reserves[0].modules.add(modules[1].ship);
        reserves[1].modules.add(modules[2].ship);
        List<ShipAPI> ships = new ArrayList<>(List.of(persistent.ship, foreign.ship, enemy.ship));
        int[] spawns = {0};
        boolean[] suppressed = {false};
        CombatFleetManagerAPI manager = mock(CombatFleetManagerAPI.class, (n, a) -> {
            if (n.equals("isSuppressDeploymentMessages")) return suppressed[0];
            if (n.equals("setSuppressDeploymentMessages")) suppressed[0] = (Boolean) a[0];
            if (n.equals("spawnShipOrWing")) {
                check(VARIANT.equals(a[0]), "Reserve station variant stays authored");
                Station reserve = reserves[spawns[0]++];
                reserve.position.set((Vector2f) a[1]);
                reserve.facing = (Float) a[2];
                ships.add(reserve.ship);
                return reserve.ship;
            }
            return null;
        });
        CombatEngineAPI engine = mock(CombatEngineAPI.class, (n, a) -> {
            if (n.equals("getShips")) return ships;
            if (n.equals("getFleetManager")) return manager;
            return null;
        });
        Class<?> type = Class.forName(
                "chiefnavigator.quest.IthacaFinalLaborBattleCreationPlugin$CenterBreachCombatPlugin");
        Constructor<?> constructor = type.getDeclaredConstructor(
                CombatEngineAPI.class, List.class, FleetMemberAPI.class, boolean.class);
        constructor.setAccessible(true);
        Object controller = constructor.newInstance(engine, Collections.emptyList(), null, false);
        Method deploy = method(type, "deployBattlestations", int.class);
        Method configure = method(type, "configureBattleShips", int.class);
        Method anchor = method(type, "anchorBattlestations");
        deploy.invoke(controller, 0);
        check(spawns[0] == 2 && !suppressed[0], "Exactly two reserves; deployment messages restored");
        check(reserves[0].position.equals(new Vector2f(-2200f, -1200f))
                        && reserves[1].position.equals(new Vector2f(2200f, -1200f)),
                "Both reserve positions stay authored");
        check(reserves[0].facing == 90f && reserves[1].facing == 90f, "Initial spawn facing is retained");
        configure.invoke(controller, 0);
        for (Station station : new Station[]{persistent, reserves[0], reserves[1], modules[0], modules[1], modules[2]}) {
            check(station.noDamageFlashes, "All three station parents/modules suppress damage flashes");
            check(station.hullDamage.getModifiedValue() == 1f && station.armorDamage.getModifiedValue() == 1f,
                    "Visual suppression preserves normal hull/armor damage");
        }
        check(!foreign.noDamageFlashes && !enemy.noDamageFlashes, "Other ships/sides are unaffected");
        Station late = new Station("late_module", 0);
        persistent.modules.add(late.ship);
        configure.invoke(controller, 0);
        check(late.noDamageFlashes, "Late station modules are configured too");

        AxialRotation nativeRotation = new AxialRotation();
        for (Station reserve : reserves) {
            Vector2f origin = new Vector2f(reserve.position);
            check(reserve.speed.getModifiedValue() == 0f
                            && reserve.acceleration.getModifiedValue() == 0f
                            && reserve.deceleration.getModifiedValue() == 0f,
                    "Reserves retain zero translation");
            check(reserve.turn.getModifiedValue() == 2.5f && reserve.turnAcceleration.getModifiedValue() == 1.5f,
                    "Native turn stats and existing modifiers remain effective");
            for (int frame = 0; frame < 30; frame++) {
                nativeRotation.advanceInCombat(reserve.ship, .1f);
                // Emulate an independently updated engine orientation before the anchor callback.
                reserve.angularVelocity = -2.5f;
                reserve.facing += reserve.angularVelocity * .1f;
                float engineFacing = reserve.facing;
                reserve.position.translate(5f, 7f);
                reserve.velocity.set(50f, 70f);
                anchor.invoke(controller);
                check(reserve.position.equals(origin) && reserve.velocity.lengthSquared() == 0f,
                        "Anchor still pins position and cancels translation");
                check(reserve.facing == engineFacing && reserve.angularVelocity == -2.5f,
                        "Anchor cannot erase native rotation between frames");
                check(!reserve.controlsLocked, "Station controls remain available to native rotation/weapons");
            }
            check(reserve.rightCommands == 30 && reserve.facing < 90f,
                    "Native Axial Rotation continues commanding rotation over repeated anchor ticks");
        }
        System.out.println("PASS: exact reserve deployment, native rotation with positional anchors, station/module flash suppression, side isolation and unchanged damage");
    }

    private static Method method(Class<?> type, String name, Class<?>... arguments) throws Exception {
        Method result = type.getDeclaredMethod(name, arguments);
        result.setAccessible(true);
        return result;
    }
}
