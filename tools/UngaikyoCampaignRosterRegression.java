package chiefnavigator.quest;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import static chiefnavigator.quest.BudaiSalvageRewardRegression.*;

/** Initial five-escort roster, identity-preserving upkeep and mutation guards. */
public final class UngaikyoCampaignRosterRegression {
    private static final String ROSTER_MARKER = "$chief_navigator_ungaikyo_final_rematch_roster_v1";
    private static final String BOSS = "chief_navigator_ungaikyo_Fabricator";
    private static final List<String> EXPECTED = List.of(BOSS,
            "chief_navigator_starving_hive_Type350",
            "chief_navigator_starving_overseer_Type250",
            "chief_navigator_starving_standoff_Type300",
            "chief_navigator_starving_assault_Type200",
            "chief_navigator_starving_skirmish_Type100");

    private static final class Member {
        final String variantId;
        final Map<String, Object> captainState = new HashMap<>();
        final PersonAPI captain = mock(PersonAPI.class, (name, values) -> {
            if (name.equals("getPersonalityAPI")) return null;
            if (name.equals("setPersonality")) captainState.put("personality", values[0]);
            return null;
        });
        final ShipVariantAPI variant;
        final FleetMemberAPI member;
        String name;
        String personality;
        float cr;

        Member(String id) throws Exception {
            variantId = id;
            JSONObject source = new JSONObject(Files.readString(Path.of("data/variants", id + ".variant")));
            String hullId = source.getString("hullId");
            variant = mock(ShipVariantAPI.class, (method, values) ->
                    method.equals("getHullVariantId") ? variantId : null);
            RepairTrackerAPI repair = mock(RepairTrackerAPI.class, (method, values) -> {
                if (method.equals("getMaxCR")) return 0.7f;
                if (method.equals("getCR")) return cr;
                if (method.equals("setCR")) cr = (Float) values[0];
                return null;
            });
            member = mock(FleetMemberAPI.class, (method, values) -> {
                switch (method) {
                    case "getHullId": return hullId;
                    case "getVariant": return variant;
                    case "getCaptain": return captain;
                    case "getRepairTracker": return repair;
                    case "getShipName": return name;
                    case "setShipName": name = (String) values[0]; break;
                    case "getPersonalityOverride": return personality;
                    case "setPersonalityOverride": personality = (String) values[0]; break;
                    default: break;
                }
                return null;
            });
        }
    }

    private static final class Fixture {
        final Map<String, Object> flags = new HashMap<>();
        final List<FleetMemberAPI> members = new ArrayList<>();
        final Map<FleetMemberAPI, Member> states = new HashMap<>();
        final MemoryAPI memory = memory(flags);
        final FleetDataAPI data;
        final CampaignFleetAPI fleet;
        FleetMemberAPI flagship;
        CampaignFleetAPI playerFleet;
        boolean player, battle, transition, despawning, dialog;
        int created, added, removed, synced;

        Fixture() {
            data = mock(FleetDataAPI.class, (method, values) -> {
                switch (method) {
                    case "getMembersListCopy": return new ArrayList<>(members);
                    case "getNumMembers": return members.size();
                    case "addFleetMember": members.add((FleetMemberAPI) values[0]); added++; break;
                    case "removeFleetMember": members.remove(values[0]); removed++; break;
                    case "setFlagship": flagship = (FleetMemberAPI) values[0]; break;
                    case "syncIfNeeded": synced++; break;
                    default: break;
                }
                return null;
            });
            BattleAPI ongoing = mock(BattleAPI.class, (method, values) -> null);
            fleet = mock(CampaignFleetAPI.class, (method, values) -> {
                switch (method) {
                    case "getFleetData": return data;
                    case "getMemoryWithoutUpdate": return memory;
                    case "getFlagship": return flagship;
                    case "isPlayerFleet": return player;
                    case "getBattle": return battle ? ongoing : null;
                    case "isInHyperspaceTransition": return transition;
                    case "isDespawning": return despawning;
                    default: return null;
                }
            });
            InteractionDialogAPI interaction = mock(InteractionDialogAPI.class, (method, values) ->
                    method.equals("getInteractionTarget") ? fleet : null);
            CampaignUIAPI ui = mock(CampaignUIAPI.class, (method, values) ->
                    method.equals("getCurrentInteractionDialog") && dialog ? interaction : null);
            Global.setSector(mock(SectorAPI.class, (method, values) -> {
                if (method.equals("getPlayerFleet")) return playerFleet;
                if (method.equals("getCampaignUI")) return ui;
                return null;
            }));
            Global.setFactory(mock(FactoryAPI.class, (method, values) -> {
                if (!method.equals("createFleetMember")) return null;
                try {
                    Member state = new Member((String) values[1]);
                    states.put(state.member, state);
                    created++;
                    return state.member;
                } catch (Exception failure) {
                    throw new IllegalStateException(failure);
                }
            }));
        }

        void ensure(Method upkeep) throws Exception { upkeep.invoke(null, fleet); }

        List<String> variants() {
            List<String> result = new ArrayList<>();
            for (FleetMemberAPI member : members) result.add(states.get(member).variantId);
            return result;
        }
    }

    private static void checkBusy(Method upkeep, String state) throws Exception {
        Fixture fixture = new Fixture();
        switch (state) {
            case "player-flag": fixture.player = true; break;
            case "player-identity": fixture.playerFleet = fixture.fleet; break;
            case "battle": fixture.battle = true; break;
            case "transition": fixture.transition = true; break;
            case "despawning": fixture.despawning = true; break;
            case "dialog": fixture.dialog = true; break;
            default: throw new AssertionError(state);
        }
        fixture.ensure(upkeep);
        check(fixture.created == 0 && fixture.added == 0 && fixture.removed == 0
                        && fixture.synced == 0 && fixture.flags.isEmpty(),
                "Roster construction must not mutate a " + state + " fleet");
    }

    public static void main(String[] args) throws Exception {
        FactoryAPI previousFactory = Global.getFactory();
        SectorAPI previousSector = Global.getSector();
        Method upkeep = TroyArrivalScript.class.getDeclaredMethod(
                "ensureUngaikyoFinalRematchRoster", CampaignFleetAPI.class);
        upkeep.setAccessible(true); // Private access is confined to this regression harness.
        try {
            Fixture first = new Fixture();
            first.ensure(upkeep);
            check(first.variants().equals(EXPECTED) && first.created == 6 && first.added == 6,
                    "First creation has Ungaikyo plus one each Hive/Overseer/Line/Assault/Skirmish");
            check(first.members.size() - 1 == 5
                            && first.variants().stream().skip(1).noneMatch(id -> id.contains("fabricator")),
                    "No new encounter exceeds five initial escorts or includes an escort Fabricator");
            FleetMemberAPI boss = first.members.get(0);
            Member bossState = first.states.get(boss);
            ShipVariantAPI variant = boss.getVariant();
            check(first.flagship == boss && "Ungaikyo".equals(bossState.name)
                            && Boolean.TRUE.equals(first.flags.get(ROSTER_MARKER)),
                    "The exact unique boss is named, flagged and installed as flagship");
            List<FleetMemberAPI> initialMembers = new ArrayList<>(first.members);
            for (int count = 0; count < 5; count++) first.ensure(upkeep);
            check(first.created == 6 && first.added == 6 && first.removed == 0
                            && first.members.equals(initialMembers) && first.flagship == boss
                            && boss.getVariant() == variant,
                    "Repeated upkeep retains every member, boss and variant by identity");

            first.data.removeFleetMember(first.members.get(1));
            first.data.removeFleetMember(first.members.get(2));
            List<FleetMemberAPI> survivors = new ArrayList<>(first.members);
            int removedAfterLosses = first.removed;
            for (int count = 0; count < 5; count++) first.ensure(upkeep);
            check(first.members.equals(survivors) && first.created == 6 && first.added == 6
                            && first.removed == removedAfterLosses && first.flagship == boss,
                    "Lost escorts stay lost; maintenance never refills or replaces survivors");
            first.data.removeFleetMember(boss);
            first.ensure(upkeep);
            check(first.created == 6 && !first.members.contains(boss),
                    "An initialized roster never recreates a lost campaign boss");

            Fixture serialized = new Fixture();
            serialized.ensure(upkeep);
            Member ordinaryFabricator = new Member("chief_navigator_starving_fabricator_Type450");
            serialized.states.put(ordinaryFabricator.member, ordinaryFabricator);
            serialized.members.add(ordinaryFabricator.member);
            List<FleetMemberAPI> savedRoster = new ArrayList<>(serialized.members);
            serialized.ensure(upkeep);
            check(serialized.members.equals(savedRoster) && serialized.created == 6 && serialized.removed == 0,
                    "Already generated serialized rosters are not trimmed or retroactively rebuilt");

            for (String state : List.of("player-flag", "player-identity", "battle",
                    "transition", "despawning", "dialog")) checkBusy(upkeep, state);
        } finally {
            Global.setFactory(previousFactory);
            Global.setSector(previousSector);
        }
        System.out.println("PASS: six-member initial Ungaikyo roster, five non-Fabricator escorts, "
                + "identity-preserving one-time upkeep, permanent losses, serialized roster preservation and busy/player guards.");
    }
}
