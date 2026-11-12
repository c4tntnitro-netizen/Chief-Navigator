package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.RepGTE;
import com.fs.starfarer.api.impl.campaign.rulecmd.RepIsAtWorst;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.Faction;
import com.fs.starfarer.campaign.FactionManager;
import com.fs.starfarer.campaign.rules.Memory;
import com.fs.starfarer.loading.O0oO;
import com.fs.starfarer.loading.SpecStore;
import com.fs.starfarer.settings.StarfarerSettings;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

/** Native reputation predicates must receive initialized faction facts. */
public final class RuleDialogMemoryRegression {
    private static FactionManager relations;

    interface Call { Object invoke(String name, Object[] args); }

    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[]{type}, (proxy, method, args) -> {
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == args[0];
                    Object result = call.invoke(method.getName(), args);
                    if (result != null) return result;
                    Class<?> returns = method.getReturnType();
                    if (returns == boolean.class) return false;
                    if (returns == int.class) return 0;
                    if (returns == float.class) return 0f;
                    if (returns == long.class) return 0L;
                    if (returns == double.class) return 0d;
                    return null;
                }));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    /**
     * Keep native Faction/Relation reputation behavior without starting a
     * campaign. Native getMemory() normally runs campaign fact plugins;
     * emulate that update's $id fact and count it independently from the
     * native getMemoryWithoutUpdate() accessor, which leaves $id absent.
     */
    private static final class TestFaction extends Faction {
        int factUpdates;

        TestFaction(String id) { super(id); }

        @Override public FactionManager.Relation getRelation(String otherId) {
            return relations.getRelation(getId(), otherId);
        }

        @Override public Memory getMemory() {
            factUpdates++;
            Memory result = (Memory) getMemoryWithoutUpdate();
            result.set("$id", getId());
            return result;
        }
    }

    private static final class Fixture {
        final MemoryAPI global = new Memory();
        final MemoryAPI player = new Memory();
        final MemoryAPI entity = new Memory();
        final MemoryAPI contact = new Memory();
        final MemoryAPI marketMemory = new Memory();
        final TestFaction stationFaction = new TestFaction("regression_station");
        final TestFaction contactFaction = new TestFaction("regression_contact");
        final TestFaction playerFaction = new TestFaction("player");
        FactionAPI currentEntityFaction = stationFaction;
        FactionAPI currentPersonFaction = contactFaction;
        MarketAPI currentMarket;
        SectorEntityToken currentTarget;

        final MarketAPI market = mock(MarketAPI.class, (name, args) -> {
            if (name.equals("getMemory") || name.equals("getMemoryWithoutUpdate")) return marketMemory;
            return null;
        });
        final PersonAPI person = mock(PersonAPI.class, (name, args) -> {
            if (name.equals("getFaction")) return currentPersonFaction;
            if (name.equals("getMemory") || name.equals("getMemoryWithoutUpdate")) return contact;
            return null;
        });
        final SectorEntityToken target = mock(SectorEntityToken.class, (name, args) -> {
            if (name.equals("getFaction")) return currentEntityFaction;
            if (name.equals("getMarket")) return currentMarket;
            if (name.equals("getMemory") || name.equals("getMemoryWithoutUpdate")) return entity;
            return null;
        });
        final InteractionDialogAPI dialog = mock(InteractionDialogAPI.class, (name, args) -> {
            if (name.equals("getInteractionTarget")) return currentTarget;
            return null;
        });

        Fixture() {
            currentTarget = target;
            Global.setSettings(mock(SettingsAPI.class, (name, args) ->
                    name.equals("getColor") ? java.awt.Color.WHITE : null));
            Global.setSector(mock(SectorAPI.class, (name, args) -> {
                if (name.equals("getMemoryWithoutUpdate")) return global;
                if (name.equals("getPlayerMemoryWithoutUpdate")) return player;
                if (name.equals("getFaction") && "player".equals(args[0])) return playerFaction;
                return null;
            }));
        }
    }

    private static void initializeNativeReputation() throws Exception {
        // Minimal test-only native setup; obfuscated classes require -Xverify:none.
        // Faction construction reads a boolean setting before its specs.
        for (Field field : StarfarerSettings.class.getDeclaredFields()) {
            if (field.getType() != JSONObject.class) continue;
            field.setAccessible(true);
            field.set(null, new JSONObject() {
                @Override public boolean getBoolean(String key) { return false; }
            });
        }
        for (String id : new String[]{"neutral", "player", "regression_station", "regression_contact"}) {
            SpecStore.o00000(O0oO.class, id, new O0oO() {
                @Override public String getId() { return id; }
            });
        }
        relations = new FactionManager();
    }

    private static boolean repGte(Map<String, MemoryAPI> memory, String token) {
        return new RepGTE().execute("regression", null,
                Misc.tokenize(token + " SUSPICIOUS"), memory);
    }

    private static void reproduceMissingScope(Fixture f) {
        Map<String, MemoryAPI> broken = new HashMap<>();
        broken.put(MemKeys.LOCAL, f.contact);
        // This matches the formerly incomplete custom-dialog scope. A missing
        // $faction.id falls back to a nonexistent local variable and becomes null.
        try {
            repGte(broken, "$faction.id");
            throw new AssertionError("A missing faction scope must reproduce the native failure");
        } catch (NullPointerException expected) {
            check(expected.getMessage().contains("Faction.getRelation(String)"),
                    "Regression must reproduce the screenshot's native relation failure");
        }
        check(f.playerFaction.getRelationship("unregistered_but_non_null") == 0f,
                "Unknown non-null faction IDs are neutral; null causes this failure");
    }

    private static void stationScope(Fixture f) {
        Map<String, MemoryAPI> scope = RuleDialogSupport.createMemoryMap(f.dialog);
        // Exercise native predicate first so the pre-fix baseline fails with
        // the actual reported Faction.getRelation null exception.
        check(repGte(scope, "$faction.id"), "Station faction reputation predicate must run");
        check(scope.get(MemKeys.GLOBAL) == f.global, "Global scope must remain intact");
        check(scope.get(MemKeys.PLAYER) == f.player, "Player scope must remain intact");
        check(scope.get(MemKeys.ENTITY) == f.entity, "Entity scope must remain intact");
        check(scope.get(MemKeys.LOCAL) == f.entity, "Default local scope must be the entity");
        check(scope.get(MemKeys.FACTION) == f.stationFaction.getMemoryWithoutUpdate(),
                "Faction scope must refer to the entity faction");
        check(f.stationFaction.factUpdates > 0,
                "getMemory() must populate native faction facts before predicates run");
        check(!scope.containsKey(MemKeys.MARKET), "Marketless Ithaca must have no invented market");

        f.currentMarket = f.market;
        scope = RuleDialogSupport.createMemoryMap(f.dialog);
        check(scope.get(MemKeys.MARKET) == f.marketMemory, "Real market memory must be exposed");
        f.currentMarket = null;
    }

    private static void contactScope(Fixture f) {
        Map<String, MemoryAPI> scope = RuleDialogSupport.createMemoryMap(f.dialog);
        scope.put(MemKeys.LOCAL, f.contact);
        RuleDialogSupport.addPersonFactionMemory(scope, f.person);
        check(repGte(scope, "$faction.id"), "Entity faction predicate must survive contact selection");
        check(new RepIsAtWorst().execute("regression", null,
                Misc.tokenize("$personFaction.id SUSPICIOUS"), scope),
                "Contact faction predicate must run with initialized facts");
        check(scope.get(MemKeys.FACTION) != scope.get(MemKeys.PERSON_FACTION),
                "Contact and entity factions must retain their distinct identities");
        check(scope.get(MemKeys.PERSON_FACTION) == f.contactFaction.getMemoryWithoutUpdate(),
                "Person-faction scope must use the selected person's faction");
        check(scope.get(MemKeys.LOCAL) == f.contact, "Adding faction facts must preserve contact local memory");
        check(f.contactFaction.factUpdates > 0, "Person faction facts must be updated");

        f.currentPersonFaction = null;
        RuleDialogSupport.addPersonFactionMemory(scope, f.person);
        check(!scope.containsKey(MemKeys.PERSON_FACTION), "A factionless person must clear stale scope");
        RuleDialogSupport.addPersonFactionMemory(scope, f.person);
        f.currentPersonFaction = f.contactFaction;
        RuleDialogSupport.addPersonFactionMemory(scope, f.person);
        RuleDialogSupport.addPersonFactionMemory(scope, null);
        check(!scope.containsKey(MemKeys.PERSON_FACTION), "Clearing a person must clear stale faction scope");
        check(scope.get(MemKeys.FACTION) == f.stationFaction.getMemoryWithoutUpdate(),
                "Clearing a contact must preserve entity faction memory");
    }

    private static void absentTargetScope(Fixture f) {
        f.currentEntityFaction = null;
        Map<String, MemoryAPI> scope = RuleDialogSupport.createMemoryMap(f.dialog);
        check(!scope.containsKey(MemKeys.FACTION), "Factionless targets must omit faction scope");
        check(scope.get(MemKeys.ENTITY) == f.entity, "Factionless target memory must remain available");
        f.currentTarget = null;
        scope = RuleDialogSupport.createMemoryMap(f.dialog);
        check(scope.get(MemKeys.ENTITY) == f.global && scope.get(MemKeys.LOCAL) == f.global,
                "No-target dialogs must retain the existing global-memory fallback");
        check(!scope.containsKey(MemKeys.FACTION) && !scope.containsKey(MemKeys.MARKET),
                "No-target dialogs must safely omit unavailable scopes");
    }

    public static void main(String[] args) throws Exception {
        SectorAPI previousSector = Global.getSector();
        SettingsAPI previousSettings = Global.getSettings();
        try {
            initializeNativeReputation();
            Fixture fixture = new Fixture();
            reproduceMissingScope(fixture);
            stationScope(fixture);
            contactScope(fixture);
            absentTargetScope(fixture);
            System.out.println("Rule-dialog native faction-memory regression checks passed.");
        } finally {
            Global.setSector(previousSector);
            Global.setSettings(previousSettings);
        }
    }
}
