package chiefnavigator.quest;

import chiefnavigator.abilities.SinniAmbushStanceAbility;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.CampaignPlugin.PickPriority;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.characters.AbilityPlugin;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.FullName.Gender;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import java.awt.Color;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import static chiefnavigator.quest.BudaiSalvageRewardRegression.*;

/** Native encounter routing, nonhuman presentation, and exact Budai ownership scope. */
public final class BudaiInteractionRegression {
    private static final String HUNTER = "$chief_navigator_budai_hunter";
    private static final String DWELLER_PORTRAIT =
            "graphics/portraits/characters/dwelller.png";
    private static final Set<String> PRESENTATION_FIELDS = Set.of(
            "showCommLinkOption", "showEngageText", "showFleetAttitude",
            "showTransponderStatus", "showWarningDialogWhenNotHostile",
            "impactsAllyReputation", "impactsEnemyReputation");

    private static final class Fixture {
        final Map<String, Object> saved = new HashMap<>();
        final Map<String, Object> local = new HashMap<>();
        final List<FleetEventListener> listeners = new ArrayList<>();
        final List<FleetMemberAPI> members = new ArrayList<>();
        FullName commanderName = new FullName("Human", "Commander", Gender.MALE);
        String commanderPortrait = "graphics/portraits/portrait1.png";
        String fleetId = TroyArrivalScript.CHARYBDIS_FLEET_ID;
        String fleetName = "Generated human name";
        boolean noFactionInName;
        boolean playerControlled;
        boolean sectorPlayerIsEnemy;
        boolean inAshen;
        boolean ambush;
        int presentationWrites;
        final MutableCharacterStatsAPI commanderStats = mock(
                MutableCharacterStatsAPI.class, (name, args) -> {
                    if (name.startsWith("set") || name.startsWith("add")
                            || name.startsWith("increase") || name.startsWith("decrease")) {
                        throw new AssertionError("Encounter presentation changed skills: " + name);
                    }
                    return null;
                });
        final PersonAPI commander = mock(PersonAPI.class, (name, args) -> {
            switch (name) {
                case "getName": return commanderName;
                case "getNameString": return commanderName.getFullName();
                case "getPortraitSprite": return commanderPortrait;
                case "getStats":
                case "getFleetCommanderStats": return commanderStats;
                case "setName": commanderName = (FullName) args[0]; presentationWrites++; break;
                case "setPortraitSprite": commanderPortrait = (String) args[0]; presentationWrites++; break;
                case "setPersonality":
                case "setStats":
                case "setFaction":
                    throw new AssertionError("Encounter presentation changed officer gameplay: " + name);
                default: break;
            }
            return null;
        });
        final FleetDataAPI enemyRoster = mock(FleetDataAPI.class, (name, args) -> {
            if (name.equals("getMembersListCopy")) return new ArrayList<>(members);
            if (name.startsWith("add") || name.startsWith("remove")
                    || name.equals("setFlagship")) {
                throw new AssertionError("Encounter routing changed Budai's roster: " + name);
            }
            return null;
        });
        final FactionAPI dweller = mock(FactionAPI.class,
                (name, args) -> name.equals("getId") ? Factions.DWELLER : null);
        final CampaignFleetAPI enemy = mock(CampaignFleetAPI.class, (name, args) -> {
            switch (name) {
                case "getId": return fleetId;
                case "getName": return fleetName;
                case "setName": fleetName = (String) args[0]; break;
                case "setNoFactionInName": noFactionInName = (Boolean) args[0]; break;
                case "getMemoryWithoutUpdate": return memory(local);
                case "getFleetData": return enemyRoster;
                case "getCommander": return commander;
                case "getCommanderStats": return commanderStats;
                case "getFaction": return dweller;
                case "getEventListeners": return listeners;
                case "addEventListener": listeners.add((FleetEventListener) args[0]); break;
                case "getContainingLocation": return inAshen ? this.ashen : this.avici;
                case "isPlayerFleet": return playerControlled;
                case "isHostileTo": return true;
                case "setCommander":
                case "setFaction":
                case "setLocation":
                case "despawn":
                case "clearAssignments":
                case "addAssignment":
                    throw new AssertionError("Encounter presentation changed fleet gameplay: " + name);
                default: break;
            }
            return null;
        });
        final AbilityPlugin ability = mock(AbilityPlugin.class,
                (name, args) -> name.equals("isActive") ? ambush : null);
        final CampaignTerrainPlugin terrainPlugin = mock(CampaignTerrainPlugin.class,
                (name, args) -> name.equals("containsEntity") || name.equals("hasAIFlag")
                        ? Boolean.TRUE : null);
        final CampaignTerrainAPI terrain = mock(CampaignTerrainAPI.class,
                (name, args) -> name.equals("getPlugin") ? terrainPlugin : null);
        final StarSystemAPI avici = system(OdysseyExpanseSystem.MESSINA_ID);
        final StarSystemAPI ashen = system(OdysseyExpanseSystem.ASHEN_VERGE_ID);
        final FleetMemberAPI playerMember = mock(FleetMemberAPI.class,
                (name, args) -> null);
        final FleetDataAPI playerRoster = mock(FleetDataAPI.class,
                (name, args) -> name.equals("getMembersListCopy")
                        ? Collections.singletonList(playerMember) : null);
        final CampaignFleetAPI player = mock(CampaignFleetAPI.class, (name, args) -> {
            if (name.equals("getFleetData")) return playerRoster;
            if (name.equals("getFlagship")) return playerMember;
            if (name.equals("getContainingLocation")) return inAshen ? ashen : avici;
            if (name.equals("getAbility")) return ability;
            if (name.equals("isHostileTo")) return true;
            return null;
        });

        Fixture() {
            saved.put(FobIthacaApproachScript.COMPLETE, true);
            local.put(MemFlags.MEMORY_KEY_PATROL_FLEET, true);
            local.put("unrelated_mod_state", "preserve");
            members.add(mock(FleetMemberAPI.class, (name, args) ->
                    name.equals("getHullId") ? "chief_navigator_charybdis" : null));
            Global.setSector(mock(SectorAPI.class, (name, args) -> {
                if (name.equals("getMemoryWithoutUpdate")) return memory(saved);
                if (name.equals("getPlayerFleet")) return sectorPlayerIsEnemy ? enemy : player;
                if (name.equals("getStarSystems")) return List.of(avici, ashen);
                return null;
            }));
        }

        private StarSystemAPI system(String id) {
            Map<String, Object> systemState = Map.of(
                    "$chief_navigator_odyssey_authored_system_v1", true);
            return mock(StarSystemAPI.class, (name, args) -> {
                if (name.equals("getOptionalUniqueId")) return id;
                if (name.equals("getMemoryWithoutUpdate")) return memory(systemState);
                if (name.equals("getTerrainCopy")) return Collections.singletonList(terrain);
                return null;
            });
        }

        void assertIdentity() {
            check(enemy.getCommander() == commander && commander.getStats() == commanderStats
                            && enemy.getFleetData() == enemyRoster && enemy.getFaction() == dweller,
                    "Keep Budai's existing commander, skills, roster, and Dweller faction objects");
            check(commanderName.getFullName().equals("Budai")
                            && commanderPortrait.equals(DWELLER_PORTRAIT)
                            && fleetName.equals("Budai") && noFactionInName,
                    "Replace generated human display names/portrait with Budai's native Dweller identity");
            check(Boolean.TRUE.equals(local.get(HUNTER))
                            && Boolean.TRUE.equals(local.get(MemFlags.MEMORY_KEY_IGNORE_PLAYER_COMMS))
                            && !local.containsKey(MemFlags.MEMORY_KEY_PATROL_FLEET)
                            && "preserve".equals(local.get("unrelated_mod_state")),
                    "Owned Budai ignores comms and drops patrol identity without deleting unrelated state");
        }
    }

    public static void main(String[] args) throws Exception {
        SettingsAPI previousSettings = Global.getSettings();
        SectorAPI previousSector = Global.getSector();
        FactoryAPI previousFactory = Global.getFactory();
        Global.setSettings(mock(SettingsAPI.class,
                (name, values) -> name.equals("getColor") ? Color.WHITE : null));
        Global.setFactory(mock(FactoryAPI.class, (name, values) ->
                name.equals("createCargo") ? mock(CargoAPI.class, (call, args2) -> null) : null));
        try {
            verifyOwnedRoute(false, false);
            verifyOwnedRoute(true, false);
            verifyOwnedRoute(false, true);
            verifyNegativeScope("foreign");
            verifyNegativeScope("missing boss");
            verifyNegativeScope("player flag");
            verifyNegativeScope("player identity");
            verifyNegativeScope("defeated");
            verifyOpeningRule();
        } finally {
            Global.setSector(previousSector);
            Global.setSettings(previousSettings);
            Global.setFactory(previousFactory);
        }
        System.out.println("PASS: scoped Budai native encounter routing, nonhuman identity/opening, "
                + "retired ambush behavior, and unchanged combat/salvage/retreat defaults.");
    }

    private static void verifyOwnedRoute(boolean marked, boolean ambush) throws Exception {
        Fixture f = new Fixture();
        if (marked) {
            f.fleetId = "chief_navigator_budai_marked_test";
            f.local.put(HUNTER, true);
        }
        f.inAshen = ambush;
        f.ambush = ambush;
        check(!SinniAmbushStanceAbility.isAmbushReady(f.player),
                "The Running Dark replacement must never force pursuit battles");
        Map<String, Object> savedBefore = new HashMap<>(f.saved);
        TreadmillCampaignPlugin router = new TreadmillCampaignPlugin();
        PluginPick<InteractionDialogPlugin> pick = router.pickInteractionDialogPlugin(f.enemy);
        check(pick != null && pick.priority == PickPriority.HIGHEST
                        && pick.plugin instanceof BudaiInteraction,
                "Canonical/explicitly marked living Budai wins over Ashen and generic ambush routes");
        verifyConfig(readConfig(pick.plugin), false);
        f.assertIdentity();
        PluginPick<BattleCreationPlugin> battle = router.pickBattleCreationPlugin(f.enemy);
        check(battle != null && battle.priority == PickPriority.HIGHEST
                        && battle.plugin instanceof BudaiBattleCreationPlugin,
                "Presentation retains Budai's dedicated combat and music plugin");
        int listenerCount = f.listeners.size();
        router.pickInteractionDialogPlugin(f.enemy);
        check(listenerCount == 1 && f.listeners.size() == listenerCount,
                "Repeated routing retains exactly one existing defeat/salvage listener");
        check(f.saved.equals(savedBefore),
                "Opening Budai's encounter cannot consume spawn, defeat, or salvage progression");
    }

    private static void verifyConfig(FIDConfig actual, boolean ambush) throws Exception {
        FIDConfig nativeDefaults = new FIDConfig();
        for (Field field : FIDConfig.class.getFields()) {
            Object expected = field.get(nativeDefaults);
            if (PRESENTATION_FIELDS.contains(field.getName())) expected = Boolean.FALSE;
            if (ambush && field.getName().equals("alwaysPursue")) expected = Boolean.TRUE;
            if (ambush && field.getName().equals("firstTimeEngageOptionText")) {
                expected = "Spring the ambush";
            }
            check(Objects.equals(field.get(actual), expected),
                    "Budai changes only presentation/reputation and the existing ambush controls: "
                            + field.getName());
        }
    }

    private static FIDConfig readConfig(InteractionDialogPlugin plugin) throws Exception {
        Field config = FleetInteractionDialogPluginImpl.class.getDeclaredField("config");
        config.setAccessible(true);
        return (FIDConfig) config.get(plugin);
    }

    private static void verifyNegativeScope(String scope) {
        Fixture f = new Fixture();
        if (scope.equals("foreign")) f.fleetId = "foreign_dweller";
        if (scope.equals("missing boss")) f.members.clear();
        if (scope.equals("player flag")) f.playerControlled = true;
        if (scope.equals("player identity")) f.sectorPlayerIsEnemy = true;
        if (scope.equals("defeated")) f.saved.put(OdysseyPredatorScript.BUDAI_DEFEATED, true);
        Map<String, Object> localBefore = new HashMap<>(f.local);
        Map<String, Object> savedBefore = new HashMap<>(f.saved);
        FullName nameBefore = f.commanderName;
        String portraitBefore = f.commanderPortrait;
        String fleetNameBefore = f.fleetName;
        boolean noFactionBefore = f.noFactionInName;
        OdysseyPredatorScript.configureBudaiInteractionIdentity(f.enemy);
        check(!OdysseyPredatorScript.isBudaiBattle(f.enemy),
                "Reject an out-of-scope Budai identity: " + scope);
        PluginPick<InteractionDialogPlugin> pick =
                new TreadmillCampaignPlugin().pickInteractionDialogPlugin(f.enemy);
        check(pick == null || !(pick.plugin instanceof BudaiInteraction),
                "Never install Budai's dedicated route outside ownership/live-boss scope: " + scope);
        check(f.local.equals(localBefore) && f.saved.equals(savedBefore)
                        && f.commanderName == nameBefore && f.commanderPortrait.equals(portraitBefore)
                        && f.fleetName.equals(fleetNameBefore) && f.noFactionInName == noFactionBefore
                        && f.presentationWrites == 0 && f.listeners.isEmpty(),
                "Foreign, player, or defeated fleets retain their identity and state: " + scope);
    }

    private static void verifyOpeningRule() throws Exception {
        List<List<String>> rows = csvRows(Files.readString(Path.of("data/campaign/rules.csv")));
        List<String> opening = null;
        for (List<String> row : rows) {
            if (!row.isEmpty() && row.get(0).equals("chiefNavigatorBudaiEncounterOpening")) {
                check(opening == null, "Budai's native encounter opening must have one rule");
                opening = row;
            }
        }
        check(opening != null && opening.size() >= 7
                        && opening.get(1).equals("BeginFleetEncounter")
                        && opening.get(2).contains(HUNTER + " score:1000")
                        && opening.get(2).contains("!$global." + OdysseyPredatorScript.BUDAI_DEFEATED.substring(1))
                        && !opening.get(4).isBlank() && !opening.get(3).contains("FleetDesc")
                        && opening.get(5).isBlank(),
                "The scoped high-priority native opening replaces generic human FleetDesc prose");
    }

    private static List<List<String>> csvRows(String csv) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < csv.length(); i++) {
            char c = csv.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                    value.append('"');
                    i++;
                } else quoted = !quoted;
            } else if (c == ',' && !quoted) {
                row.add(value.toString());
                value.setLength(0);
            } else if ((c == '\n' || c == '\r') && !quoted) {
                if (c == '\r' && i + 1 < csv.length() && csv.charAt(i + 1) == '\n') i++;
                row.add(value.toString());
                rows.add(row);
                row = new ArrayList<>();
                value.setLength(0);
            } else value.append(c);
        }
        check(!quoted, "Runtime rules CSV must have balanced quoted fields");
        if (!row.isEmpty() || value.length() > 0) {
            row.add(value.toString());
            rows.add(row);
        }
        return rows;
    }
}
