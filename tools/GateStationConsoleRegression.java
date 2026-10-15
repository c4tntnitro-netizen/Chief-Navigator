package chiefnavigator.console;

import chiefnavigator.quest.IthacaSectionEncounter;
import chiefnavigator.quest.OdysseyExpanseSystem;
import chiefnavigator.quest.TaskForceSpartanFaction;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.*;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;

/** Invokes the real console command; only the canonical idle station may vanish. */
public final class GateStationConsoleRegression {
    private interface Call { Object invoke(String method, Object[] args); }
    private static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[]{type}, (proxy, method, args) -> {
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == args[0];
                    if (method.getName().equals("toString")) return type.getSimpleName();
                    Object result = call.invoke(method.getName(), args);
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
    private static MemoryAPI memory(Map<String, Object> flags) {
        return mock(MemoryAPI.class, (name, args) -> {
            if (name.equals("getBoolean")) return Boolean.TRUE.equals(flags.get(args[0]));
            if (name.equals("get")) return flags.get(args[0]);
            if (name.equals("contains")) return flags.containsKey(args[0]);
            if (name.equals("set") || name.equals("unset")) {
                throw new AssertionError("Command mutated campaign memory: " + name);
            }
            return null;
        });
    }
    private static final class World {
        final Map<String, Object> flags = new HashMap<>();
        final Map<String, Object> systemFlags = new HashMap<>();
        final Map<String, SectorEntityToken> entities = new HashMap<>();
        final List<SectorEntityToken> removed = new ArrayList<>();
        CampaignFleetAPI player;
        SectorEntityToken interacting;
        final StarSystemAPI system;
        World() {
            flags.put("$chief_navigator_ithaca_final_center_created_v1", true);
            flags.put("$chief_navigator_final_labor_failed", true);
            flags.put("unrelated-saved-value", 72);
            systemFlags.put("$chief_navigator_odyssey_ashen_verge_abyss_v1", true);
            system = mock(StarSystemAPI.class, (name, args) -> {
                if (name.equals("getOptionalUniqueId")) return OdysseyExpanseSystem.ASHEN_VERGE_ID;
                if (name.equals("getMemoryWithoutUpdate")) return memory(systemFlags);
                if (name.equals("getEntityById")) return entities.get(args[0]);
                if (name.equals("removeEntity")) {
                    SectorEntityToken target = (SectorEntityToken) args[0];
                    removed.add(target);
                    entities.remove(target.getId());
                }
                return null;
            });
            Map<String, Object> centerFlags = Collections.singletonMap(
                    "$chief_navigator_fob_ithaca_owned_v1", true);
            SectorEntityToken center = mock(SectorEntityToken.class, (name, args) -> {
                if (name.equals("getId")) return IthacaSectionEncounter.FOB_ENTITY_ID;
                if (name.equals("getCustomEntityType")) return "chief_navigator_fob_ithaca_campaign";
                if (name.equals("getMemoryWithoutUpdate")) return memory(centerFlags);
                return null;
            });
            entities.put(IthacaSectionEncounter.FOB_ENTITY_ID, center);
            entities.put("unrelated-fleet", mock(SectorEntityToken.class,
                    (name, args) -> name.equals("getId") ? "unrelated-fleet" : null));
            Global.setSector(mock(SectorAPI.class, (name, args) -> {
                if (name.equals("getStarSystems")) return Collections.singletonList(system);
                if (name.equals("getPlayerFleet")) return player;
                if (name.equals("getMemoryWithoutUpdate")) return memory(flags);
                if (name.equals("getCampaignUI")) return mock(CampaignUIAPI.class,
                        (n, a) -> n.equals("getCurrentInteractionDialog") && interacting != null
                                ? mock(InteractionDialogAPI.class,
                                        (d, v) -> d.equals("getInteractionTarget") ? interacting : null)
                                : null);
                return null;
            }));
        }
    }
    private static final class Fleet {
        boolean station = true, player, transitioning, despawning;
        String faction = TaskForceSpartanFaction.ID;
        String variant = OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_VARIANT_ID;
        String hull = OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_HULL_ID;
        BattleAPI battle;
        final List<FleetMemberAPI> members = new ArrayList<>();
        final CampaignFleetAPI api;
        Fleet(World world) {
            FleetMemberAPI member = mock(FleetMemberAPI.class, (name, args) -> {
                if (name.equals("getVariant")) return mock(ShipVariantAPI.class,
                        (n, a) -> n.equals("getHullVariantId") ? variant : null);
                if (name.equals("getHullSpec")) return mock(ShipHullSpecAPI.class,
                        (n, a) -> n.equals("getHullId") ? hull : null);
                return null;
            });
            members.add(member);
            api = mock(CampaignFleetAPI.class, (name, args) -> {
                if (name.equals("getId")) return OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_STATION_ID;
                if (name.equals("getContainingLocation")) return world.system;
                if (name.equals("isStationMode")) return station;
                if (name.equals("isPlayerFleet")) return player;
                if (name.equals("isInHyperspaceTransition")) return transitioning;
                if (name.equals("isDespawning")) return despawning;
                if (name.equals("getBattle")) return battle;
                if (name.equals("getFaction")) return mock(FactionAPI.class,
                        (n, a) -> n.equals("getId") ? faction : null);
                if (name.equals("getFleetData")) return mock(FleetDataAPI.class,
                        (n, a) -> n.equals("getMembersListCopy") ? new ArrayList<>(members) : null);
                if (name.startsWith("set") || name.startsWith("add")
                        || name.startsWith("clear") || name.startsWith("remove")) {
                    throw new AssertionError("Command mutated surviving fleet: " + name);
                }
                return null;
            });
            world.entities.put(OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_STATION_ID, api);
        }
    }
    public static void main(String[] args) throws Exception {
        String savedLogInitialization = System.getProperty("log4j.defaultInitOverride");
        System.setProperty("log4j.defaultInitOverride", "true");
        SectorAPI savedSector = Global.getSector();
        SettingsAPI savedSettings = Global.getSettings();
        try {
            // Native Console.showMessage initializes its output panel with a mod check.
            Global.setSettings(mock(SettingsAPI.class, (name, values) ->
                    name.equals("getModManager") ? mock(ModManagerAPI.class, (n, a) -> null) : null));
            run();
            Path root = Path.of(args.length == 0 ? "." : args[0]);
            String csv = Files.readString(root.resolve("data/console/commands.csv"));
            check(csv.contains("DestroyIthacaGateStation,chiefnavigator.console.DestroyIthacaGateStation,"),
                    "Console command is not registered");
        } finally {
            Global.setSector(savedSector);
            Global.setSettings(savedSettings);
            if (savedLogInitialization == null) System.clearProperty("log4j.defaultInitOverride");
            else System.setProperty("log4j.defaultInitOverride", savedLogInitialization);
        }
        System.out.println("PASS: real gate-station console invocation removes only idle owned station; "
                + "absent/foreign/player/incompatible/busy states and all Labor flags preserved.");
    }
    private static void run() {
        DestroyIthacaGateStation command = new DestroyIthacaGateStation();
        World world = new World();
        Fleet fleet = new Fleet(world);
        Map<String, Object> savedFlags = new HashMap<>(world.flags);
        SectorEntityToken unrelated = world.entities.get("unrelated-fleet");
        check(command.runCommand("", CommandContext.COMBAT_MISSION) == CommandResult.WRONG_CONTEXT,
                "Combat context accepted");
        check(command.runCommand("extra", CommandContext.CAMPAIGN_MAP) == CommandResult.BAD_SYNTAX,
                "Unexpected arguments accepted");
        check(world.removed.isEmpty(), "Context/syntax checks removed station");
        check(command.runCommand("", CommandContext.CAMPAIGN_MAP) == CommandResult.SUCCESS,
                "Valid station not removed");
        check(world.removed.size() == 1 && world.removed.get(0) == fleet.api,
                "Removal did not use exact original station token");
        check(world.entities.size() == 2 && world.entities.get("unrelated-fleet") == unrelated,
                "Unrelated campaign entities changed");
        check(world.flags.equals(savedFlags), "Labor or unrelated flags changed");
        check(command.runCommand("", CommandContext.CAMPAIGN_MAP) == CommandResult.SUCCESS
                && world.removed.size() == 1, "Absent station was regenerated or removed twice");

        for (int scenario = 0; scenario < 10; scenario++) {
            world = new World(); fleet = new Fleet(world);
            switch (scenario) {
                case 0: fleet.faction = "foreign"; break;
                case 1: fleet.player = true; break;
                case 2: world.player = fleet.api; break;
                case 3: fleet.variant = "wrong_variant"; break;
                case 4: fleet.hull = "wrong_hull"; break;
                case 5: fleet.station = false; break;
                case 6: fleet.members.clear(); break;
                case 7: fleet.battle = mock(BattleAPI.class, (n, a) -> null); break;
                case 8: fleet.transitioning = true; break;
                case 9: fleet.despawning = true; break;
            }
            check(command.runCommand("", CommandContext.CAMPAIGN_MAP) == CommandResult.ERROR,
                    "Unsafe scenario accepted: " + scenario);
            check(world.removed.isEmpty()
                    && world.entities.get(OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_STATION_ID) == fleet.api,
                    "Unsafe station changed: " + scenario);
        }
        world = new World(); fleet = new Fleet(world); world.interacting = fleet.api;
        check(command.runCommand("", CommandContext.CAMPAIGN_MAP) == CommandResult.ERROR
                && world.removed.isEmpty(), "Active station interaction removed");
        world = new World();
        world.entities.put(OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_STATION_ID,
                mock(SectorEntityToken.class, (n, a) -> null));
        check(command.runCommand("", CommandContext.CAMPAIGN_MAP) == CommandResult.ERROR
                && world.removed.isEmpty(), "Wrong-type claim removed");
        world = new World(); new Fleet(world);
        world.entities.remove(IthacaSectionEncounter.FOB_ENTITY_ID);
        check(command.runCommand("", CommandContext.CAMPAIGN_MAP) == CommandResult.ERROR
                && world.removed.isEmpty(), "Missing FOB allowed removal");
    }
}
