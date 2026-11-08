package chiefnavigator.quest;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.fleet.Battle;
import com.fs.starfarer.campaign.rules.Memory;
import java.awt.Color;
import java.lang.reflect.*;
import java.util.*;
import sun.misc.Unsafe;

/** Native Memory, Battle and encounter menu; no join permissions or side overrides. */
public final class IthacaDefenderFriendshipRegression {
    static Unsafe unsafe;
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    interface Call { Object invoke(String name, Object[] args); }
    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("hashCode")) return System.identityHashCode(proxy);
                    if (name.equals("equals")) return proxy == args[0];
                    if (name.equals("toString")) return type.getSimpleName();
                    Object value = call.invoke(name, args);
                    if (value != null) return value;
                    Class<?> result = method.getReturnType();
                    if (result == boolean.class) return false;
                    if (result == int.class) return 0;
                    if (result == float.class) return 0f;
                    if (result == long.class) return 0L;
                    return null;
                }));
    }
    static String key(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return (String) field.get(null);
    }
    static final class Faction {
        final String id;
        final Map<String, Float> relations = new HashMap<>();
        final FactionAPI api;
        Faction(String id) {
            this.id = id;
            api = mock(FactionAPI.class, (name, args) -> {
                if (name.equals("getId")) return id;
                if (name.equals("isPlayerFaction")) return id.equals(Factions.PLAYER);
                if (name.equals("getRelationship")) return relations.getOrDefault((String) args[0], 0f);
                if (name.equals("setRelationship")) relations.put((String) args[0], ((Number) args[1]).floatValue());
                if (name.equals("isHostileTo") || name.equals("isAtWorst")) {
                    String other = args[0] instanceof String ? (String) args[0] : ((FactionAPI) args[0]).getId();
                    float relation = relations.getOrDefault(other, 0f);
                    return name.equals("isHostileTo") ? relation <= -0.5f : relation >= 0.5f;
                }
                return null;
            });
        }
    }
    static final class Fleet {
        final Fixture fixture;
        final Memory memory = new Memory();
        final CampaignFleetAPI api;
        final String id;
        final Faction faction;
        boolean player, station, reportsHostility = true;
        Fleet(Fixture fixture, String id, Faction faction, boolean player, boolean station) {
            this.fixture = fixture;
            this.id = id;
            this.faction = faction;
            this.player = player;
            this.station = station;
            ShipVariantAPI variant = mock(ShipVariantAPI.class, (n, a) -> n.equals("getHullVariantId")
                    ? OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_VARIANT_ID : null);
            ShipHullSpecAPI hull = mock(ShipHullSpecAPI.class, (n, a) -> n.equals("getHullId")
                    ? OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_HULL_ID : null);
            FleetMemberAPI member = mock(FleetMemberAPI.class, (n, a) ->
                    n.equals("getVariant") ? variant : n.equals("getHullSpec") ? hull : null);
            FleetDataAPI data = mock(FleetDataAPI.class, (n, a) ->
                    (n.equals("getMembersListCopy") || n.equals("getCombatReadyMembersListCopy"))
                            ? List.of(member) : null);
            api = mock(CampaignFleetAPI.class, (name, args) -> {
                if (name.equals("getId")) return id;
                if (name.equals("getFaction")) return faction.api;
                if (name.equals("getMemoryWithoutUpdate")) return memory;
                if (name.equals("getContainingLocation")) return fixture.location;
                if (name.equals("getFleetData")) return data;
                if (name.equals("isPlayerFleet")) return this.player;
                if (name.equals("isStationMode")) return this.station;
                if (name.equals("getBattle")) return fixture.battle;
                if (name.equals("isTransponderOn")) return player && fixture.transponder;
                if (name.equals("setTransponderOn")) { fixture.transponder = (Boolean) args[0]; return null; }
                if (name.equals("knowsWhoPlayerIs")) return knowsPlayer();
                if (name.equals("isFriendlyTo")) return knowsPlayer()
                        && faction.api.isAtWorst(fixture.playerFaction.api, RepLevel.FRIENDLY);
                if (name.equals("isHostileTo")) return reportsHostility && knowsPlayer()
                        && faction.api.isHostileTo(fixture.playerFaction.api);
                if (name.startsWith("set") || name.startsWith("add") || name.startsWith("remove")
                        || name.startsWith("clear")) throw new AssertionError(
                        "Friendship reconciliation must not alter roster, orders or ownership: " + name);
                return null;
            });
        }
        boolean knowsPlayer() {
            return fixture.transponder || memory.getBoolean(MemFlags.MEMORY_KEY_SAW_PLAYER_WITH_TRANSPONDER_ON);
        }
    }
    static final class Fixture {
        final Memory campaign = new Memory(), systemMemory = new Memory();
        final Faction playerFaction = new Faction(Factions.PLAYER);
        final Faction spartanFaction = new Faction(TaskForceSpartanFaction.ID);
        final Faction guardFaction = new Faction(IthacaSectionEncounter.COMBAT_GUARD_FACTION_ID);
        final Faction threatFaction = new Faction(IthacaThreatPolicy.FACTION_ID);
        final Map<String, Faction> factions = new HashMap<>();
        final List<CampaignFleetAPI> fleets = new ArrayList<>();
        final StarSystemAPI location;
        final Fleet player, station, strike;
        Battle battle;
        boolean transponder;
        Fixture(boolean reversed) throws Exception {
            for (Faction faction : List.of(playerFaction, spartanFaction, guardFaction, threatFaction,
                    new Faction(Factions.THREAT), new Faction(Factions.OMEGA))) factions.put(faction.id, faction);
            spartanFaction.relations.put(Factions.PLAYER, 1f);
            guardFaction.relations.put(Factions.PLAYER, 1f);
            threatFaction.relations.put(Factions.PLAYER, -1f);
            systemMemory.set(OdysseyExpanseSystem.GENERATED_ASHEN_VERGE, true);
            location = mock(StarSystemAPI.class, (n, a) -> {
                if (n.equals("getOptionalUniqueId")) return OdysseyExpanseSystem.ASHEN_VERGE_ID;
                if (n.equals("getMemoryWithoutUpdate")) return systemMemory;
                if (n.equals("getFleets")) return fleets;
                return null;
            });
            player = new Fleet(this, "player", playerFaction, true, false);
            station = new Fleet(this, OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_STATION_ID,
                    spartanFaction, false, true);
            strike = new Fleet(this, "chief_navigator_final_labor_heavenly_strike", threatFaction, false, false);
            strike.reportsHostility = false;
            station.memory.set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
            strike.memory.set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
            fleets.addAll(List.of(station.api, strike.api));
            battle = (Battle) unsafe.allocateInstance(Battle.class);
            for (String n : List.of("sideOne", "snapshotSideOne")) setSide(n, reversed ? strike.api : station.api);
            for (String n : List.of("sideTwo", "snapshotSideTwo")) setSide(n, reversed ? station.api : strike.api);
            Global.setSector(mock(SectorAPI.class, (n, a) -> {
                if (n.equals("getPlayerFleet")) return player.api;
                if (n.equals("getPlayerFaction")) return playerFaction.api;
                if (n.equals("getFaction")) return factions.get((String) a[0]).api;
                if (n.equals("getStarSystems")) return List.of(location);
                if (n.equals("getMemoryWithoutUpdate")) return campaign;
                throw new AssertionError("Unexpected campaign mutation: " + n);
            }));
        }
        void setSide(String name, CampaignFleetAPI fleet) throws Exception {
            Field f = Battle.class.getDeclaredField(name); f.setAccessible(true);
            f.set(battle, new ArrayList<>(List.of(fleet)));
        }
    }
    static final class Preview extends FleetInteractionDialogPluginImpl {
        final List<Object> choices = new ArrayList<>();
        Preview(Fixture f) {
            super(new FIDConfig());
            playerFleet = f.player.api;
            ongoingBattle = true;
            context.setBattle(f.battle);
            config.impactsEnemyReputation = false;
            options = mock(OptionPanelAPI.class, (n, a) -> {
                if (n.equals("clearOptions")) choices.clear();
                if (n.equals("addOption")) choices.add(a[1]);
                return null;
            });
        }
        @Override protected boolean isFightingOver() { return false; }
        void assertJoin() {
            updateMainState(false);
            check(choices.contains(OptionId.JOIN_ONGOING_BATTLE), "Native menu must offer Join the battle");
        }
    }
    static void assertNativeJoin(Fixture f, boolean reversed) {
        check(f.battle.pickSide(f.player.api) == (reversed ? BattleAPI.BattleSide.TWO : BattleAPI.BattleSide.ONE),
                "Native selection must choose Ithaca without any permission or callback");
        check(f.battle.canJoin(f.player.api), "Native eligibility must permit the living-station battle");
        new Preview(f).assertJoin();
        check(!f.battle.isPlayerInvolved(), "Preview must not join automatically");
        for (CampaignFleetAPI fleet : f.fleets) check(!fleet.getMemoryWithoutUpdate().contains(
                MemFlags.MEMORY_KEY_ALLOW_PLAYER_BATTLE_JOIN_TOFF), "No join override may be installed");
    }
    public static void main(String[] args) throws Exception {
        Field u = Unsafe.class.getDeclaredField("theUnsafe"); u.setAccessible(true); unsafe = (Unsafe) u.get(null);
        SectorAPI oldSector = Global.getSector(); SettingsAPI oldSettings = Global.getSettings();
        FactoryAPI oldFactory = Global.getFactory();
        try {
            Global.setSettings(mock(SettingsAPI.class, (n, a) -> n.equals("getFloat") ? 1f
                    : n.equals("getColor") ? Color.WHITE : null));
            Global.setFactory(mock(FactoryAPI.class, (n, a) -> n.equals("createCargo")
                    ? mock(CargoAPI.class, (cn, ca) -> null) : null));
            for (boolean reversed : new boolean[] {false, true}) {
                for (boolean transponder : new boolean[] {false, true}) {
                    Fixture f = new Fixture(reversed); f.transponder = transponder;
                    f.station.memory.set(MemFlags.MEMORY_KEY_SAW_PLAYER_WITH_TRANSPONDER_ON, true);
                    check(f.battle.pickSide(f.player.api, false) == BattleAPI.BattleSide.NO_JOIN,
                            "Reproduce NO_REP_IMPACT discounting an identified friendly defender");
                    TaskForceSpartanFaction.enforceRelations();
                    check(Misc.isPermaKnowsWhoPlayerIs(f.station.api), "Recognition uses vanilla permanent memory");
                    check(!f.station.memory.contains(MemFlags.MEMORY_KEY_NO_REP_IMPACT), "Friendly side retains native friendship");
                    check(f.strike.memory.getBoolean(MemFlags.MEMORY_KEY_NO_REP_IMPACT)
                            && !Misc.isPermaKnowsWhoPlayerIs(f.strike.api), "Enemy state remains unchanged");
                    assertNativeJoin(f, reversed);
                    TaskForceSpartanFaction.enforceRelations();
                    assertNativeJoin(f, reversed);
                    check(f.transponder == transponder, "Player transponder remains unchanged");
                    check(!f.campaign.getBoolean(MenelausTrial.FINAL_DEBRIEF_COMPLETE), "Test is before Labor V completion");
                }
            }
            Fixture f = new Fixture(false);
            Fleet guard = new Fleet(f, OdysseyExpanseSystem.FOB_ITHACA_SPARTAN_GUARD_ID, f.spartanFaction, false, false);
            guard.memory.set(key(OdysseyExpanseSystem.class, "FOB_ITHACA_SPARTAN_FLEET_MARKER"), true);
            Fleet bastion = new Fleet(f, "chief_navigator_ithaca_station_north", f.guardFaction, false, true);
            bastion.memory.set(IthacaSectionEncounter.MARKER, true);
            bastion.memory.set(IthacaSectionEncounter.SECTION_KEY, "north");
            Fleet foreign = new Fleet(f, "foreign_station", f.spartanFaction, false, true);
            for (Fleet fleet : List.of(guard, bastion, foreign)) {
                fleet.memory.set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true); f.fleets.add(fleet.api);
            }
            TaskForceSpartanFaction.enforceRelations();
            check(Misc.isPermaKnowsWhoPlayerIs(guard.api) && Misc.isPermaKnowsWhoPlayerIs(bastion.api),
                    "Busy saved Spartan guards and bastions receive identity reconciliation");
            check(foreign.memory.getBoolean(MemFlags.MEMORY_KEY_NO_REP_IMPACT)
                            && !Misc.isPermaKnowsWhoPlayerIs(foreign.api), "Foreign claims stay untouched");
            check(f.spartanFaction.api.isHostileTo(f.threatFaction.api)
                            && !f.spartanFaction.api.isHostileTo(f.playerFaction.api), "Native diplomacy preserved");
            System.out.println("PASS: native friendship/recognition, living-station JOIN with transponder on/off, "
                    + "both sides, existing busy saves, ordinary guards/bastions and no join workaround.");
        } finally {
            Global.setSector(oldSector); Global.setSettings(oldSettings); Global.setFactory(oldFactory);
        }
    }
}
