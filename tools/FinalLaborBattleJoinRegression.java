package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.campaign.fleet.Battle;
import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import sun.misc.Unsafe;

/** Exercises the native Battle.canJoin/pickSide IFF gate with the scoped exception. */
public final class FinalLaborBattleJoinRegression {
    private static final String JOIN = MemFlags.MEMORY_KEY_ALLOW_PLAYER_BATTLE_JOIN_TOFF;
    private static Unsafe unsafe;

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    interface Call { Object invoke(String name, Object[] args); }

    private static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("hashCode")) return System.identityHashCode(proxy);
                    if (name.equals("equals")) return proxy == args[0];
                    if (name.equals("toString")) return type.getSimpleName();
                    Object result = call.invoke(name, args);
                    if (result != null) return result;
                    Class<?> returns = method.getReturnType();
                    if (returns == boolean.class) return false;
                    if (returns == int.class) return 0;
                    if (returns == float.class) return 0f;
                    if (returns == long.class) return 0L;
                    return null;
                }));
    }

    private static String key(Class<?> type, String field) throws Exception {
        Field member = type.getDeclaredField(field);
        member.setAccessible(true);
        return (String) member.get(null);
    }

    static final class Memory {
        final Map<String, Object> values = new HashMap<>();
        final MemoryAPI api = mock(MemoryAPI.class, (name, args) -> {
            if (name.equals("getBoolean") || name.equals("is")) {
                return Boolean.TRUE.equals(values.get(args[0]));
            }
            if (name.equals("contains")) return values.containsKey(args[0]);
            if (name.equals("get") || name.equals("getString")) return values.get(args[0]);
            if (name.equals("set")) values.put((String) args[0], args[1]);
            if (name.equals("unset")) values.remove(args[0]);
            return null;
        });
    }

    static final class Fleet {
        final Fixture fixture;
        final Memory memory = new Memory();
        final FactionAPI faction;
        final CampaignFleetAPI api;
        String id;
        boolean player;
        boolean hostileToPlayer;
        boolean transponder;

        Fleet(Fixture fixture, String id, String factionId, boolean player) {
            this.fixture = fixture;
            this.id = id;
            this.player = player;
            faction = mock(FactionAPI.class, (name, args) -> {
                if (name.equals("getId")) return factionId;
                if (name.equals("isPlayerFaction")) return player;
                return null;
            });
            api = mock(CampaignFleetAPI.class, (name, args) -> {
                if (name.equals("getId")) return this.id;
                if (name.equals("isPlayerFleet")) return this.player;
                if (name.equals("getFaction")) return faction;
                if (name.equals("getMemoryWithoutUpdate")) return memory.api;
                if (name.equals("getBattle")) return fixture.battle;
                if (name.equals("getContainingLocation")) return fixture.location;
                if (name.equals("isTransponderOn")) return transponder;
                if (name.equals("setTransponderOn")) { transponder = (Boolean) args[0]; return null; }
                if (name.equals("knowsWhoPlayerIs")) return fixture.player.transponder;
                if (name.equals("isHostileTo")) return hostileToPlayer && fixture.player.transponder;
                if (name.equals("isFriendlyTo")) return !this.player && !hostileToPlayer
                        && fixture.player.transponder;
                return null;
            });
        }
    }

    static final class Fixture {
        final Memory memory = new Memory();
        final LocationAPI location = mock(LocationAPI.class, (name, args) -> null);
        final Fleet player = new Fleet(this, "player", "player", true);
        final Fleet spartan = new Fleet(this, OdysseyExpanseSystem.FOB_ITHACA_SPARTAN_GUARD_ID,
                TaskForceSpartanFaction.ID, false);
        final Fleet strike;
        final Battle battle;

        Fixture() throws Exception {
            strike = new Fleet(this, key(OdysseyPredatorScript.class, "ITHACA_FINAL_CENTER_FLEET_ID"),
                    IthacaThreatPolicy.FACTION_ID, false);
            strike.hostileToPlayer = true;
            spartan.memory.values.put(key(OdysseyExpanseSystem.class, "FOB_ITHACA_SPARTAN_FLEET_MARKER"), true);
            strike.memory.values.put(OdysseyPredatorScript.ITHACA_FINAL_CENTER_BATTLE_MARKER, true);
            spartan.memory.values.put(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
            memory.values.put(MenelausTrial.ACCEPTED, true);
            memory.values.put(MenelausTrial.FINAL_LABOR_BRIEFED, true);
            memory.values.put(key(MenelausTrial.class, "FINAL_LABOR_FORCED_FOR_TESTING"), true);
            memory.values.put(key(MenelausTrial.class, "LABOR_REPORT_MIGRATION"), true);
            battle = (Battle) unsafe.allocateInstance(Battle.class);
            for (String name : List.of("sideOne", "snapshotSideOne")) setBattleField(name,
                    new ArrayList<>(List.of(spartan.api)));
            for (String name : List.of("sideTwo", "snapshotSideTwo")) setBattleField(name,
                    new ArrayList<>(List.of(strike.api)));
            Global.setSector(mock(SectorAPI.class, (name, args) -> {
                if (name.equals("getPlayerFleet")) return player.api;
                if (name.equals("getMemoryWithoutUpdate")) return memory.api;
                throw new AssertionError("Join preview must not mutate the campaign: " + name);
            }));
        }

        void setBattleField(String name, Object value) throws Exception {
            Field field = Battle.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(battle, value);
        }

        void assertNoJoin() {
            check(!battle.canJoin(player.api) && battle.pickSide(player.api) == BattleAPI.BattleSide.NO_JOIN,
                    "Native IFF must reject this encounter with the transponder off");
        }
    }

    private static void positiveAndCleanup() throws Exception {
        Fixture f = new Fixture();
        f.assertNoJoin();
        for (CampaignFleetAPI target : List.of(f.strike.api, f.spartan.api)) {
            FinalLaborBattleJoin.withPermission(target, () -> {
                check(f.battle.canJoin(f.player.api)
                                && f.battle.pickSide(f.player.api) == BattleAPI.BattleSide.ONE,
                        "The actual native gate must admit the player on Spartan's side");
                check(f.battle.getSideOne().equals(List.of(f.spartan.api))
                                && f.battle.getSideTwo().equals(List.of(f.strike.api)),
                        "Opening the dialog must not join the battle or change either side");
                check(!f.strike.memory.values.containsKey(JOIN), "Enemy side must never receive permission");
            });
            check(!f.spartan.memory.values.containsKey(JOIN) && !f.player.transponder,
                    "Dialog calls must restore temporary permission and transponder state");
            f.assertNoJoin();
        }
        try {
            FinalLaborBattleJoin.withPermission(f.strike.api, () -> { throw new IllegalStateException("test"); });
            throw new AssertionError("Native dialog failure must propagate");
        } catch (IllegalStateException expected) {
            check(!f.spartan.memory.values.containsKey(JOIN), "Failure must still revoke temporary permission");
        }
        f.spartan.memory.values.put(JOIN, true);
        FinalLaborBattleJoin.withPermission(f.strike.api, () -> check(f.battle.canJoin(f.player.api),
                "Existing native permission must remain usable"));
        check(Boolean.TRUE.equals(f.spartan.memory.values.get(JOIN)), "Existing permission must survive");
    }

    private static void negativeScope() throws Exception {
        for (int scenario = 0; scenario < 5; scenario++) {
            Fixture f = new Fixture();
            if (scenario == 0) f.strike.id = "unrelated_hostile_fleet";
            if (scenario == 1) f.spartan.id = "unowned_spartan_fleet";
            if (scenario == 2) f.spartan.hostileToPlayer = true;
            if (scenario == 3) f.memory.values.put(MenelausTrial.ACCEPTED, false);
            if (scenario == 4) f.spartan.memory.values.put(JOIN, false);
            Map<String, Object> original = new HashMap<>(f.spartan.memory.values);
            FinalLaborBattleJoin.withPermission(f.strike.api, f::assertNoJoin);
            check(f.spartan.memory.values.equals(original) && !f.player.transponder,
                    "Ordinary/foreign/hostile/inactive/explicit-denial encounters must remain unchanged");
        }
        Fixture f = new Fixture();
        f.player.transponder = true;
        FinalLaborBattleJoin.withPermission(f.strike.api, () -> check(f.battle.canJoin(f.player.api),
                "Ordinary transponder-on eligibility must remain native"));
        check(f.player.transponder && !f.spartan.memory.values.containsKey(JOIN),
                "Transponder-on encounters need no permission override");
    }

    public static void main(String[] args) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        unsafe = (Unsafe) field.get(null);
        SectorAPI priorSector = Global.getSector();
        SettingsAPI priorSettings = Global.getSettings();
        try {
            Global.setSettings(mock(SettingsAPI.class, (name, values) -> {
                if (name.equals("getFloat")) return 1f;
                if (name.equals("getColor")) return Color.WHITE;
                return null;
            }));
            positiveAndCleanup();
            negativeScope();
            System.out.println("PASS: native final-battle IFF rejection reproduced; scoped Spartan permission "
                    + "admits the correct side without joining automatically or changing ordinary encounters.");
        } finally {
            Global.setSector(priorSector);
            Global.setSettings(priorSettings);
        }
    }
}
