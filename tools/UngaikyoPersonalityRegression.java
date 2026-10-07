package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;

/** Native AI/member handoff, Fearless hunt orders, and separate Cautious boss policy. */
public final class UngaikyoPersonalityRegression {
    public static void main(String[] args) throws Exception {
        SettingsAPI previous = Global.getSettings();
        Map<ShipAPI, ShipState> ships = new IdentityHashMap<>();
        Global.setSettings(mock(SettingsAPI.class, (name, args2) -> {
            if (!name.equals("createDefaultShipAI")) return null;
            ShipState state = ships.get(args2[0]);
            check(state != null && state.defaultActivations == 1
                            && !state.locked && !state.holdFire && !state.phased,
                    "A mirror must be natively activated and released before its custom AI is installed");
            state.config = (ShipAIConfig) args2[1];
            return state.ai;
        }));
        try {
            ShipState originalPlayer = new ShipState("aurora");
            originalPlayer.owner = 0;
            for (String hull : new String[]{"ziggurat", "onslaught_xiv", "aurora"}) {
                ShipState root = new ShipState(hull);
                ShipState module = new ShipState(hull + "_module");
                root.children.add(module.ship);
                ships.put(root.ship, root);
                ships.put(module.ship, module);
                GautamaFinalRematchBattleCreationPlugin.activateCompletedMirror(root.ship, root.member, 1);
                for (ShipState state : List.of(root, module)) {
                    check(state.owner == 1 && state.originalOwner == 1
                                    && state.personality.equals(Personalities.RECKLESS)
                                    && state.memberPersonality.equals(Personalities.RECKLESS)
                                    && state.boundMember == state.member && state.evaluations == 1,
                            "Root and module must have enemy ownership, Reckless personality and their own member/AI");
                    ShipAIConfig config = state.config;
                    check(config.personalityOverride.equals(Personalities.RECKLESS)
                                    && config.alwaysStrafeOffensively && !config.backingOffWhileNotVentingAllowed
                                    && !config.turnToFaceWithUndamagedArmor && config.burnDriveIgnoreEnemies,
                            "Every completed mirror uses the full Fearless config: " + state.hull);
                }
                DeployedFleetMemberAPI deployed = mock(DeployedFleetMemberAPI.class,
                        (name, values) -> name.equals("getShip") ? root.ship : null);
                int[] huntOrders = {0};
                CombatTaskManagerAPI tasks = mock(CombatTaskManagerAPI.class, (name, values) -> {
                    check(name.equals("orderSearchAndDestroy") && values[0] == deployed
                                    && Boolean.FALSE.equals(values[1]),
                            "Only this mirror gets a native hunt order, without command-point cost or Full Assault");
                    huntOrders[0]++;
                    return null;
                });
                CombatFleetManagerAPI manager = mock(CombatFleetManagerAPI.class, (name, values) -> {
                    if (name.equals("getDeployedFleetMember")) {
                        check(values[0] == root.ship, "Hunt only the completed mirror root");
                        return deployed;
                    }
                    if (name.equals("getTaskManager")) {
                        check(Boolean.FALSE.equals(values[0]), "Use the enemy's native task manager");
                        return tasks;
                    }
                    throw new AssertionError("Unexpected fleet mutation: " + name);
                });
                GautamaFinalRematchBattleCreationPlugin.orderCompletedMirrorToHunt(root.ship, manager);
                check(huntOrders[0] == 1, "Issue exactly one hunt order per completion: " + hull);
            }
            check(originalPlayer.owner == 0 && originalPlayer.personality.equals(Personalities.STEADY)
                            && originalPlayer.memberPersonality.equals(Personalities.STEADY),
                    "Do not change the player's original hulls/captains");

            ShipState boss = new ShipState("chief_navigator_ungaikyo");
            GautamaFinalRematchBattleCreationPlugin.configureUngaikyoAI(boss.ship);
            check(boss.personality.equals(Personalities.CAUTIOUS)
                            && boss.memberPersonality.equals(Personalities.CAUTIOUS)
                            && boss.config.personalityOverride.equals(Personalities.CAUTIOUS)
                            && boss.nativeResets == 1 && boss.evaluations == 1,
                    "Ungaikyo must separately use a refreshed native Cautious AI, not Fearless");

            ShipState ordinary = new ShipState("chief_navigator_starving_hive_unit");
            List<FleetMemberAPI> flagships = new ArrayList<>();
            FleetDataAPI data = mock(FleetDataAPI.class, (name, values) -> {
                if (name.equals("getMembersListCopy")) return List.of(ordinary.member, boss.member);
                if (name.equals("setFlagship")) flagships.add((FleetMemberAPI) values[0]);
                return null;
            });
            CampaignFleetAPI fleet = mock(CampaignFleetAPI.class,
                    (name, values) -> name.equals("getFleetData") ? data : null);
            Method flagship = TroyArrivalScript.class.getDeclaredMethod("makeUngaikyoFlagship", CampaignFleetAPI.class);
            flagship.setAccessible(true);
            flagship.invoke(null, fleet);
            check(flagships.equals(List.of(boss.member)) && ordinary.personality.equals(Personalities.STEADY),
                    "Campaign setup keeps the boss Cautious without changing other Threat captains");
            String source = Files.readString(Path.of("src/chiefnavigator/quest/GautamaFinalRematchBattleCreationPlugin.java"));
            check(source.contains("orderCompletedMirrorToHunt(mirror, manager);")
                            && source.contains("live != null && live != ungaikyo"),
                    "Every opening/rebuild completion hunts; boss native AI refresh is deployment-scoped");
            System.out.println("PASS: Ziggurat/Onslaught/Aurora mirror handoff and native hunting, "
                    + "all live modules Fearless, Ungaikyo separately Cautious, and player/ordinary Threat isolation.");
        } finally {
            Global.setSettings(previous);
        }
    }

    private static final class ShipState {
        private final String hull;
        private String personality = Personalities.STEADY, memberPersonality = Personalities.STEADY;
        private int owner = 1, originalOwner, defaultActivations, nativeResets, evaluations;
        private boolean locked = true, holdFire = true, phased = true;
        private FleetMemberAPI boundMember;
        private ShipAIConfig config = new ShipAIConfig();
        private final List<ShipAPI> children = new ArrayList<>();
        private final PersonAPI captain;
        private final FleetMemberAPI member;
        private final ShipAIPlugin ai;
        private ShipAIPlugin activeAI;
        private final ShipAPI ship;

        private ShipState(String hull) {
            this.hull = hull;
            captain = mock(PersonAPI.class, (name, values) -> {
                if (name.equals("setPersonality")) personality = (String) values[0];
                return null;
            });
            member = mock(FleetMemberAPI.class, (name, values) -> {
                if (name.equals("getHullId")) return hull;
                if (name.equals("getCaptain")) return captain;
                if (name.equals("setPersonalityOverride")) memberPersonality = (String) values[0];
                if (name.equals("setOwner")) owner = (Integer) values[0];
                return null;
            });
            ai = mock(ShipAIPlugin.class, (name, values) -> {
                if (name.equals("getConfig")) return config;
                if (name.equals("forceCircumstanceEvaluation")) evaluations++;
                return null;
            });
            ship = mock(ShipAPI.class, (name, values) -> {
                switch (name) {
                    case "getCaptain": return captain;
                    case "getFleetMember": return member;
                    case "getChildModulesCopy": return children;
                    case "getShipAI": return activeAI;
                    case "isAlive": return true;
                    case "setOwner": owner = (Integer) values[0]; break;
                    case "setOriginalOwner": originalOwner = (Integer) values[0]; break;
                    case "setControlsLocked": locked = (Boolean) values[0]; break;
                    case "setHoldFire": holdFire = (Boolean) values[0]; break;
                    case "setPhased": phased = (Boolean) values[0]; break;
                    case "setDefaultAI": boundMember = (FleetMemberAPI) values[0]; defaultActivations++; break;
                    case "setShipAI": activeAI = (ShipAIPlugin) values[0]; break;
                    case "resetDefaultAI": activeAI = ai; nativeResets++; break;
                }
                return null;
            });
        }
    }

    private static <T> T mock(Class<T> type, BiFunction<String, Object[], Object> handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> {
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == args[0];
                    Object result = handler.apply(method.getName(), args);
                    if (result != null) return result;
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    if (method.getReturnType() == float.class) return 0f;
                    return null;
                }));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
