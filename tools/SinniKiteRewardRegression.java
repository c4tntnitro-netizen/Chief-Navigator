package chiefnavigator.quest;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin.DerelictShipData;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.StarTypes;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.ShipCondition;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.ShipRecoverySpecialData;
import java.lang.reflect.Field;
import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.lwjgl.util.vector.Vector2f;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONObject;

/** Post-defeat Kite construction and compatibility with legacy hidden wrecks.
 * Native save-boundary checks also need starfarer_obf.jar and fs.common_obf.jar.
 */
public final class SinniKiteRewardRegression {
    interface Call { Object invoke(String name, Object[] args); }

    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[] {type}, (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("hashCode")) return System.identityHashCode(proxy);
                    if (name.equals("equals")) return proxy == args[0];
                    Object result = call.invoke(name, args);
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

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static String key(String fieldName) throws Exception {
        Field field = SinniVignetteRewards.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (String) field.get(null);
    }

    static final class SavedMemory {
        final Map<String, Object> values = new HashMap<>();
        final Map<String, Integer> writes = new HashMap<>();
        final MemoryAPI api = mock(MemoryAPI.class, (name, args) -> {
            if (name.equals("getBoolean")) return Boolean.TRUE.equals(values.get(args[0]));
            if (name.equals("get") || name.equals("getString")) return values.get(args[0]);
            if (name.equals("contains")) return values.containsKey(args[0]);
            if (name.equals("getKeys")) return values.keySet();
            if (name.equals("set")) {
                values.put((String) args[0], args[1]);
                writes.merge((String) args[0], 1, Integer::sum);
            }
            if (name.equals("unset")) {
                values.remove(args[0]);
                writes.merge((String) args[0], 1, Integer::sum);
            }
            return null;
        });
    }

    static final class TokenState {
        final SavedMemory memory = new SavedMemory();
        final Vector2f position = new Vector2f(100, 200);
        final Set<String> tags = new HashSet<>();
        final Map<String, Integer> calls = new HashMap<>();
        final List<FleetEventListener> listeners = new ArrayList<>();
        final SectorEntityToken token;
        final StatBonus detectedRange = new StatBonus();
        DerelictShipEntityPlugin plugin;
        String id;
        String type;
        LocationAPI location;
        boolean alive;
        boolean player;
        boolean empty;
        boolean transition;
        boolean despawning;
        float facing = 73;
        Boolean discoverable;
        Float sensorProfile;

        TokenState(String id, String type, boolean fleet) {
            this.id = id;
            this.type = type;
            token = fleet ? mock(CampaignFleetAPI.class, this::invoke)
                    : mock(CustomCampaignEntityAPI.class, this::invoke);
        }

        Object invoke(String name, Object[] args) {
            if (name.equals("getId")) return id;
            if (name.equals("getCustomEntityType")) return type;
            if (name.equals("getCustomPlugin")) return plugin;
            if (name.equals("getDetectedRangeMod")) return detectedRange;
            if (name.equals("getMemoryWithoutUpdate")) return memory.api;
            if (name.equals("getEventListeners")) return listeners;
            if (name.equals("addEventListener")) listeners.add((FleetEventListener) args[0]);
            if (name.equals("getLocation")) return position;
            if (name.equals("getFacing")) return facing;
            if (name.equals("getContainingLocation")) return location;
            if (name.equals("isAlive")) return alive;
            if (name.equals("isPlayerFleet")) return player;
            if (name.equals("isEmpty")) return empty;
            if (name.equals("isInHyperspaceTransition")) return transition;
            if (name.equals("isDespawning")) return despawning;
            if (name.equals("hasTag")) return tags.contains(args[0]);
            if (name.equals("getTags")) return tags;
            if (name.equals("addTag")) tags.add((String) args[0]);
            if (name.equals("removeTag")) tags.remove(args[0]);
            if (name.equals("setFixedLocation") || name.equals("setLocation")) {
                position.set(((Number) args[0]).floatValue(),
                        ((Number) args[1]).floatValue());
            }
            if (name.equals("setFacing")) facing = ((Number) args[0]).floatValue();
            if (name.equals("setId")) id = (String) args[0];
            if (name.equals("setDiscoverable")) discoverable = (Boolean) args[0];
            if (name.equals("setSensorProfile")) sensorProfile = (Float) args[0];
            if (name.startsWith("set") || name.startsWith("force")
                    || name.equals("addTag") || name.equals("removeTag")) {
                calls.merge(name, 1, Integer::sum);
            }
            return null;
        }

        int count(String name) { return calls.getOrDefault(name, 0); }

        int allMutations() {
            int result = 0;
            for (int count : calls.values()) result += count;
            return result;
        }
    }

    static final class SystemState {
        final String id;
        final Map<String, SectorEntityToken> active = new LinkedHashMap<>();
        // Native removeEntity retains containingLocation, and its ID cache can
        // still return the removed token until the location's next advance.
        final Map<String, SectorEntityToken> idCache = new HashMap<>();
        final Map<SectorEntityToken, TokenState> known = new HashMap<>();
        StarSystemAPI api;
        int adds;
        int removes;
        int addAttempts;
        int removeAttempts;
        boolean failAdd;
        boolean failRemove;
        int customCreates;
        int identityLookups;
        int fleetScans;
        Call customCreation;
        final PlanetAPI star = mock(PlanetAPI.class,
                (name, args) -> name.equals("getTypeId") ? StarTypes.YELLOW : null);

        SystemState(String id) {
            this.id = id;
            api = mock(StarSystemAPI.class, (name, args) -> {
                if (name.equals("getId") || name.equals("getOptionalUniqueId")) return id;
                if (name.equals("getStar")) return star;
                if (name.equals("getName") || name.equals("getBaseName")) return id;
                if (name.equals("addCustomEntity")) {
                    customCreates++;
                    check(customCreation != null, "Only a destruction callback may create a new wreck");
                    return customCreation.invoke(name, args);
                }
                if (name.equals("getEntityById")) {
                    identityLookups++;
                    return idCache.get(args[0]);
                }
                if (name.equals("getAllEntities")) return new ArrayList<>(active.values());
                if (name.equals("getFleets")) {
                    fleetScans++;
                    List<CampaignFleetAPI> result = new ArrayList<>();
                    for (SectorEntityToken entity : active.values()) {
                        if (entity instanceof CampaignFleetAPI) result.add((CampaignFleetAPI) entity);
                    }
                    return result;
                }
                if (name.equals("removeEntity")) {
                    removeAttempts++;
                    if (failRemove) throw new IllegalStateException("Simulated staging failure");
                    SectorEntityToken token = (SectorEntityToken) args[0];
                    check(active.remove(token.getId()) == token,
                            "Only a currently attached token may be staged");
                    known.get(token).alive = false;
                    removes++;
                }
                if (name.equals("addEntity")) {
                    addAttempts++;
                    if (failAdd) throw new IllegalStateException("Simulated reveal failure");
                    SectorEntityToken token = (SectorEntityToken) args[0];
                    check(!active.containsKey(token.getId()),
                            "The prepared reward must never be inserted twice");
                    active.put(token.getId(), token);
                    idCache.put(token.getId(), token);
                    TokenState state = known.get(token);
                    check(state != null, "Reveal must restore the same prepared token");
                    state.location = api;
                    state.alive = true;
                    adds++;
                }
                return null;
            });
        }

        void seed(TokenState state) {
            known.put(state.token, state);
            active.put(state.id, state.token);
            idCache.put(state.id, state.token);
            state.location = api;
            state.alive = true;
        }

        void advanceCache() {
            idCache.clear();
            idCache.putAll(active);
        }
    }

    static final class Fixture {
        final SavedMemory saved = new SavedMemory();
        final FactionAPI independent = mock(FactionAPI.class, (name, args) ->
                name.equals("pickRandomShipName") ? "Pristine Test Kite" : null);
        final SystemState home = new SystemState("kite_reward_home");
        final SystemState other = new SystemState("other_system");
        final TokenState player = new TokenState("player", null, true);
        final TokenState pirate;
        final TokenState wreck;
        final String spawned;
        final String revealed;
        final String defeated;
        final String invalid;
        final String stagedToken;
        final String stagedSystem;
        final Method maintain;
        final Method ensure;
        SectorEntityToken playerClaim;
        int factoryCalls;

        Fixture() throws Exception {
            spawned = key("MAIN_SEQUENCE_REWARD_SPAWNED");
            revealed = key("MAIN_SEQUENCE_WRECK_REVEALED");
            defeated = key("MAIN_SEQUENCE_PIRATE_DEFEATED");
            invalid = key("MAIN_SEQUENCE_REWARD_INVALID_V1");
            stagedToken = key("MAIN_SEQUENCE_STAGED_WRECK");
            stagedSystem = key("MAIN_SEQUENCE_STAGED_SYSTEM");
            pirate = new TokenState(key("MAIN_SEQUENCE_FLEET_ID"), null, true);
            pirate.memory.values.put(key("MAIN_SEQUENCE_FLEET_MARKER"), true);
            wreck = new TokenState(key("MAIN_SEQUENCE_WRECK_ID"), Entities.WRECK, false);
            wreck.memory.values.put(key("MAIN_SEQUENCE_WRECK_MARKER"), true);
            player.player = true;
            playerClaim = player.token;
            home.seed(player);
            home.seed(pirate);
            home.seed(wreck);
            saved.values.put(spawned, true);
            Field completed = SinniSystemVignetteScript.class.getDeclaredField("COMPLETED_PREFIX");
            completed.setAccessible(true);
            saved.values.put((String) completed.get(null) + "main_sequence", true);
            maintain = SinniVignetteRewards.class.getDeclaredMethod("maintainMainSequenceReward");
            maintain.setAccessible(true);
            ensure = SinniVignetteRewards.class.getDeclaredMethod("ensureMainSequenceReward", CampaignFleetAPI.class);
            ensure.setAccessible(true);
            Global.setSector(mock(SectorAPI.class, (name, args) -> {
                if (name.equals("getMemoryWithoutUpdate")) return saved.api;
                if (name.equals("getFaction")) return independent;
                if (name.equals("getPlayerFleet")) return playerClaim;
                if (name.equals("getStarSystems")) return List.of(home.api, other.api);
                return null;
            }));
            Global.setFactory(mock(FactoryAPI.class, (name, args) -> {
                factoryCalls++;
                throw new AssertionError("A spawned reward must never access the factory: " + name);
            }));
        }

        void tick() throws Exception {
            invoke(ensure, (CampaignFleetAPI) player.token);
            invoke(maintain);
        }

        void ticks(int count) throws Exception {
            for (int i = 0; i < count; i++) tick();
        }

        void stageAsNewSave() {
            home.active.remove(wreck.id);
            home.idCache.remove(wreck.id);
            wreck.alive = false;
            wreck.discoverable = false;
            wreck.tags.add(Tags.NON_CLICKABLE);
            wreck.tags.add(Tags.NO_ENTITY_TOOLTIP);
            saved.values.put(stagedToken, wreck.token);
            saved.values.put(stagedSystem, home.id);
        }

        void noPreparedWreck() {
            home.active.remove(wreck.id);
            home.idCache.remove(wreck.id);
            wreck.alive = false;
            saved.values.remove(stagedToken);
            saved.values.remove(stagedSystem);
        }

        void enablePostFightCreation() throws Exception {
            noPreparedWreck();
            SalvageEntityGenDataSpec spec = new SalvageEntityGenDataSpec(
                    new JSONObject().put("id", Entities.WRECK).put("stationRole", ""));
            Global.setSettings(mock(SettingsAPI.class, (name, args) -> {
                if (name.equals("getSpec") && args[0] == SalvageEntityGenDataSpec.class
                        && Entities.WRECK.equals(args[1])) return spec;
                if (name.equals("getColor")) return Color.WHITE;
                return null;
            }));
            home.customCreation = (name, args) -> {
                check(Boolean.TRUE.equals(saved.values.get(revealed)),
                        "The claim must commit before post-defeat salvage construction");
                check(Entities.WRECK.equals(args[2]) && args[4] instanceof DerelictShipData,
                        "The destruction callback must use a normal vanilla wreck entity");
                DerelictShipData data = (DerelictShipData) args[4];
                wreck.plugin = new DerelictShipEntityPlugin() {
                    @Override public DerelictShipData getData() { return data; }
                };
                home.seed(wreck);
                return wreck.token;
            };
        }

        void complete() throws Exception {
            Method method = SinniVignetteRewards.class.getDeclaredMethod(
                    "completeMainSequenceReward", CampaignFleetAPI.class);
            method.setAccessible(true);
            invoke(method, (CampaignFleetAPI) pirate.token);
        }

        void assertHidden() {
            check(!wreck.alive && !home.active.containsKey(wreck.id)
                            && saved.values.get(stagedToken) == wreck.token
                            && home.id.equals(saved.values.get(stagedSystem)),
                    "The prepared token must be retained outside the active contact repository");
            check(Boolean.FALSE.equals(wreck.discoverable)
                            && wreck.tags.contains(Tags.NON_CLICKABLE)
                            && wreck.tags.contains(Tags.NO_ENTITY_TOOLTIP),
                    "A staged wreck must remain non-discoverable and non-interactable");
            check(!Boolean.TRUE.equals(saved.values.get(revealed))
                            && !Boolean.TRUE.equals(saved.values.get(defeated)),
                    "A live or busy pirate must not release the reward");
            check(wreck.count("forceSensorFaderOut") == 0
                            && wreck.count("forceSensorFaderBrightness") == 0
                            && wreck.count("forceSensorContactFaderBrightness") == 0
                            && factoryCalls == 0,
                    "Hidden upkeep must not reset discovery faders or invoke a spawn factory");
        }

        void assertRevealedOnce(int expectedRemoves) {
            check(home.adds == 1 && home.removes == expectedRemoves
                            && home.active.get(wreck.id) == wreck.token
                            && wreck.alive && wreck.location == home.api,
                    "Defeat must restore exactly the same prepared wreck once in its original system");
            check(Boolean.TRUE.equals(saved.values.get(revealed))
                            && Boolean.TRUE.equals(saved.values.get(defeated))
                            && !saved.values.containsKey(stagedToken)
                            && !saved.values.containsKey(stagedSystem),
                    "Reveal must commit and release staging references");
            check(wreck.discoverable == null
                            && !wreck.tags.contains(Tags.NON_CLICKABLE)
                            && !wreck.tags.contains(Tags.NO_ENTITY_TOOLTIP)
                            && wreck.count("setSensorProfile") == 1
                            && wreck.count("forceSensorFaderBrightness") == 0
                            && wreck.count("forceSensorContactFaderBrightness") == 0
                            && wreck.count("forceSensorFaderOut") == 0,
                    "Legacy reveal restores native discoverability without forced faders");
            check(factoryCalls == 0 && other.adds == 0 && other.removes == 0,
                    "Reveal may not create a replacement or mutate another system");
        }
    }

    static void invoke(Method method, Object... args) throws Exception {
        try {
            method.invoke(null, args);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof Error) throw (Error) cause;
            if (cause instanceof Exception) throw (Exception) cause;
            throw failure;
        }
    }

    static void livePirateThenDefeat(boolean legacy) throws Exception {
        Fixture fixture = new Fixture();
        if (!legacy) fixture.stageAsNewSave();
        fixture.tick();
        fixture.assertHidden();
        check(fixture.home.removes == (legacy ? 1 : 0),
                "Legacy hidden wrecks are staged once; already staged saves are untouched");
        int discoverableCalls = fixture.wreck.count("setDiscoverable");
        fixture.home.advanceCache();
        fixture.pirate.position.set(460, -85);
        fixture.pirate.facing = 130;
        int identityLookups = fixture.home.identityLookups + fixture.other.identityLookups;
        int fleetScans = fixture.home.fleetScans + fixture.other.fleetScans;
        fixture.ticks(1000);
        fixture.assertHidden();
        check(fixture.wreck.count("setDiscoverable") == discoverableCalls
                        && fixture.wreck.count("setSensorProfile") == 0
                        && fixture.home.removes == (legacy ? 1 : 0)
                        && fixture.wreck.count("setFixedLocation") == 0
                        && fixture.wreck.count("setFacing") == 0
                        && fixture.home.identityLookups + fixture.other.identityLookups == identityLookups
                        && fixture.home.fleetScans + fixture.other.fleetScans == fleetScans,
                "A thousand live-pirate updates must not track a hidden wreck or rearm discovery");

        fixture.pirate.empty = true;
        fixture.ticks(1000);
        fixture.assertHidden();
        check(fixture.pirate.listeners.size() == 1,
                "Legacy migration must retain one destruction listener");
        fixture.pirate.listeners.get(0).reportBattleOccurred(
                (CampaignFleetAPI) fixture.pirate.token,
                (CampaignFleetAPI) fixture.player.token,
                mock(BattleAPI.class, (name, args) -> null));
        fixture.assertRevealedOnce(legacy ? 1 : 0);
        int writes = fixture.wreck.allMutations();
        fixture.ticks(1000);
        fixture.assertRevealedOnce(legacy ? 1 : 0);
        check(fixture.wreck.allMutations() == writes,
                "A thousand post-reveal updates must not reset any token property");

        fixture.home.active.remove(fixture.wreck.id);
        fixture.home.advanceCache();
        fixture.wreck.alive = false; // The native recovery interaction consumed it.
        fixture.ticks(1000);
        check(fixture.home.adds == 1 && fixture.factoryCalls == 0
                        && fixture.wreck.allMutations() == writes,
                "Recovering the pristine Kite must permanently prevent another wreck or pirate");
    }

    static void staleCacheSameTickDefeat() throws Exception {
        Fixture fixture = new Fixture();
        fixture.pirate.empty = true;
        fixture.tick();
        fixture.assertRevealedOnce(1);
        check(fixture.home.idCache.get(fixture.wreck.id) == fixture.wreck.token,
                "This fixture must retain native-style stale identity lookup throughout staging");
        fixture.ticks(1000);
        fixture.assertRevealedOnce(1);
    }

    static void freshDestructionCallback(boolean battleCallback) throws Exception {
        Fixture fixture = new Fixture();
        fixture.enablePostFightCreation();
        fixture.complete();
        fixture.ticks(1000);
        check(fixture.pirate.listeners.size() == 1
                        && fixture.home.customCreates == 0
                        && !fixture.home.active.containsKey(fixture.wreck.id)
                        && fixture.wreck.allMutations() == 0
                        && !fixture.saved.values.containsKey(fixture.stagedToken)
                        && !fixture.saved.values.containsKey(fixture.stagedSystem)
                        && fixture.factoryCalls == 0,
                "A thousand pre-fight ticks attach one listener, but create no hidden wreck or fader work");
        FleetEventListener listener = fixture.pirate.listeners.get(0);
        BattleAPI battle = mock(BattleAPI.class, (name, args) -> null);
        listener.reportBattleOccurred((CampaignFleetAPI) fixture.pirate.token,
                (CampaignFleetAPI) fixture.player.token, battle);
        listener.reportBattleOccurred((CampaignFleetAPI) fixture.player.token,
                (CampaignFleetAPI) fixture.player.token, battle);
        for (FleetDespawnReason reason : FleetDespawnReason.values()) {
            if (reason != FleetDespawnReason.DESTROYED_BY_BATTLE) {
                listener.reportFleetDespawnedToListener(
                        (CampaignFleetAPI) fixture.pirate.token, reason, null);
            }
        }
        check(fixture.home.customCreates == 0
                        && !Boolean.TRUE.equals(fixture.saved.values.get(fixture.revealed)),
                "Surviving/retreated ships and non-battle despawn reasons may not release the reward");
        fixture.pirate.position.set(-351, 821);
        fixture.pirate.facing = 142;
        if (battleCallback) {
            fixture.pirate.empty = true;
            listener.reportBattleOccurred((CampaignFleetAPI) fixture.pirate.token,
                    (CampaignFleetAPI) fixture.player.token, null);
            check(fixture.home.customCreates == 0,
                    "An empty fleet with no real battle notification cannot claim recovery");
            listener.reportBattleOccurred((CampaignFleetAPI) fixture.pirate.token,
                    (CampaignFleetAPI) fixture.player.token, battle);
        } else {
            listener.reportFleetDespawnedToListener((CampaignFleetAPI) fixture.pirate.token,
                    FleetDespawnReason.DESTROYED_BY_BATTLE, null);
        }
        check(fixture.home.customCreates == 1 && fixture.home.adds == 0
                        && fixture.home.removes == 0
                        && fixture.home.active.get(fixture.wreck.id) == fixture.wreck.token
                        && fixture.wreck.alive
                        && fixture.wreck.position.equals(fixture.pirate.position)
                        && fixture.wreck.facing == fixture.pirate.facing
                        && fixture.wreck.discoverable == null
                        && fixture.wreck.sensorProfile == null
                        && !fixture.wreck.tags.contains(Tags.NON_CLICKABLE)
                        && !fixture.wreck.tags.contains(Tags.NO_ENTITY_TOOLTIP)
                        && fixture.wreck.count("forceSensorFaderOut") == 0
                        && fixture.wreck.count("forceSensorFaderBrightness") == 0
                        && fixture.wreck.count("forceSensorContactFaderBrightness") == 0,
                "Actual destruction must create one normal clickable wreck at the battle location: "
                        + "creates=" + fixture.home.customCreates + ", adds=" + fixture.home.adds
                        + ", removes=" + fixture.home.removes + ", calls=" + fixture.wreck.calls
                        + ", saved=" + fixture.saved.values);
        Object special = fixture.wreck.memory.values.get("$salvageSpecialData");
        check(special instanceof ShipRecoverySpecialData,
                "The reward must carry vanilla ship-recovery data");
        ShipRecoverySpecialData recovery = (ShipRecoverySpecialData) special;
        check(recovery.ships.size() == 1
                        && recovery.ships.get(0).condition == ShipCondition.PRISTINE
                        && "kite_original_Stock".equals(recovery.ships.get(0).variantId),
                "The single recovery result must be the promised pristine Kite (S)");
        check(Boolean.TRUE.equals(fixture.saved.values.get(fixture.revealed))
                        && Boolean.TRUE.equals(fixture.saved.values.get(fixture.defeated))
                        && !fixture.saved.values.containsKey(fixture.stagedToken)
                        && !fixture.saved.values.containsKey(fixture.stagedSystem),
                "Completion is one durable claim, without hidden-token metadata for new encounters");
        int mutations = fixture.wreck.allMutations();
        for (int repeat = 0; repeat < 1000; repeat++) {
            listener.reportBattleOccurred((CampaignFleetAPI) fixture.pirate.token,
                    (CampaignFleetAPI) fixture.player.token, battle);
            listener.reportFleetDespawnedToListener((CampaignFleetAPI) fixture.pirate.token,
                    FleetDespawnReason.DESTROYED_BY_BATTLE, null);
            fixture.complete();
            fixture.tick();
        }
        check(fixture.home.customCreates == 1 && fixture.wreck.allMutations() == mutations,
                "Duplicate callbacks and upkeep must not create or reconfigure another reward");
        fixture.home.active.remove(fixture.wreck.id);
        fixture.home.advanceCache();
        fixture.wreck.alive = false;
        fixture.ticks(1000);
        fixture.complete();
        check(fixture.home.customCreates == 1 && fixture.wreck.allMutations() == mutations,
                "A recovered pristine Kite is never replaced after another callback or reload-style upkeep");
    }

    static void failClosedCases() throws Exception {
        for (int scenario = 0; scenario < 8; scenario++) {
            Fixture fixture = new Fixture();
            if (scenario == 0) fixture.wreck.memory.values.clear();
            if (scenario == 1) fixture.wreck.type = "unrelated_entity";
            if (scenario == 2) {
                fixture.stageAsNewSave();
                fixture.wreck.id = "unrelated_wreck";
            }
            if (scenario == 3) fixture.pirate.memory.values.clear();
            if (scenario == 4) fixture.pirate.player = true;
            if (scenario == 5) {
                fixture.stageAsNewSave();
                fixture.saved.values.put(fixture.stagedToken, fixture.player.token);
            }
            if (scenario == 6) fixture.saved.values.put(fixture.stagedToken, "not a token");
            if (scenario == 7) fixture.playerClaim = fixture.pirate.token;
            fixture.ticks(1000);
            check(Boolean.TRUE.equals(fixture.saved.values.get(fixture.invalid))
                            && fixture.wreck.allMutations() == 0
                            && fixture.player.allMutations() == 0
                            && fixture.pirate.allMutations() == 0
                            && fixture.home.adds == 0 && fixture.home.removes == 0
                            && fixture.other.adds == 0 && fixture.other.removes == 0,
                    "Incompatible ownership/type/player state must fail closed (case " + scenario + ")");
        }
    }

    static void completedAndMissingCases() throws Exception {
        Fixture completed = new Fixture();
        completed.saved.values.put(completed.revealed, true);
        completed.ticks(1000);
        check(completed.wreck.allMutations() == 0 && completed.home.adds == 0
                        && completed.home.removes == 0 && completed.factoryCalls == 0,
                "An already revealed legacy reward must not be hidden or relabeled");

        Fixture absent = new Fixture();
        absent.home.active.remove(absent.wreck.id);
        absent.home.advanceCache();
        absent.wreck.alive = false;
        absent.ticks(1000);
        check(absent.home.adds == 0 && absent.home.removes == 0
                        && absent.factoryCalls == 0 && absent.wreck.allMutations() == 0,
                "Missing prepared reward state must not create replacement campaign objects");

        Fixture despawning = new Fixture();
        despawning.stageAsNewSave();
        despawning.pirate.empty = true;
        despawning.pirate.despawning = true;
        despawning.ticks(1000);
        despawning.assertHidden();

        Fixture busyEmpty = new Fixture();
        busyEmpty.pirate.empty = true;
        busyEmpty.pirate.transition = true;
        busyEmpty.ticks(1000);
        busyEmpty.assertHidden();
        busyEmpty.pirate.transition = false;
        busyEmpty.tick();
        busyEmpty.assertRevealedOnce(1);
    }

    static void stagedReferenceCases() throws Exception {
        Fixture attachedPointer = new Fixture();
        attachedPointer.saved.values.put(attachedPointer.stagedToken, attachedPointer.wreck.token);
        attachedPointer.saved.values.put(attachedPointer.stagedSystem, attachedPointer.home.id);
        attachedPointer.ticks(1000);
        attachedPointer.assertHidden();
        check(attachedPointer.home.removes == 1
                        && attachedPointer.wreck.count("setDiscoverable") == 1,
                "A staged pointer that still owns an active token must detach it once");

        for (boolean aliveConflict : new boolean[] {false, true}) {
            Fixture fixture = new Fixture();
            fixture.stageAsNewSave();
            fixture.pirate.empty = true;
            TokenState foreign = new TokenState(fixture.wreck.id, Entities.WRECK, false);
            foreign.location = fixture.home.api;
            foreign.alive = aliveConflict;
            fixture.home.known.put(foreign.token, foreign);
            fixture.home.idCache.put(foreign.id, foreign.token);
            if (aliveConflict) fixture.home.active.put(foreign.id, foreign.token);
            fixture.ticks(1000);
            check(foreign.allMutations() == 0,
                    "A conflicting foreign token must never be modified");
            if (aliveConflict) {
                check(Boolean.TRUE.equals(fixture.saved.values.get(fixture.invalid))
                                && fixture.home.active.get(foreign.id) == foreign.token
                                && !fixture.wreck.alive && fixture.home.adds == 0,
                        "An alive conflicting identity must quarantine the prepared reward");
            } else {
                fixture.assertRevealedOnce(0);
                check(!Boolean.TRUE.equals(fixture.saved.values.get(fixture.invalid)),
                        "A stale cache reference to a detached foreign token cannot block reveal");
            }
        }

        Fixture missingHome = new Fixture();
        missingHome.stageAsNewSave();
        missingHome.saved.values.put(missingHome.stagedSystem, "missing_home");
        missingHome.pirate.empty = true;
        missingHome.ticks(1000);
        check(Boolean.TRUE.equals(missingHome.saved.values.get(missingHome.invalid))
                        && missingHome.home.adds == 0 && missingHome.other.adds == 0
                        && !missingHome.wreck.alive,
                "A missing serialized staging destination may not redirect the reward");
    }

    static void nativeMemoryBoundary() throws Exception {
        Class<?> implementation = Class.forName("com.fs.starfarer.campaign.rules.Memory");
        Object memory = implementation.getConstructor().newInstance();
        Method save = implementation.getMethod("replaceEntitiesWithIds", LinkedHashMap.class);
        Method restore = implementation.getMethod("replaceIdsWithEntities", LinkedHashMap.class);
        TokenState prepared = new TokenState("detached_pristine_kite", Entities.WRECK, false);
        prepared.alive = false;
        LinkedHashMap<String, Object> detached = new LinkedHashMap<>();
        detached.put("$prepared", prepared.token);
        save.invoke(memory, detached);
        check(detached.get("$prepared") == prepared.token,
                "Native Memory serialization must retain the full detached reward object");
        restore.invoke(memory, detached);
        check(detached.get("$prepared") == prepared.token,
                "Native Memory restoration must preserve a staged token's identity");

        prepared.alive = true;
        LinkedHashMap<String, Object> attached = new LinkedHashMap<>();
        attached.put("$prepared", prepared.token);
        save.invoke(memory, attached);
        check(attached.get("$prepared") instanceof String
                        && attached.get("$prepared").toString().endsWith(prepared.id),
                "The native comparison must serialize an attached entity as an ID reference");
    }

    static void failedTransitionDoesNotLoop() throws Exception {
        Fixture failedStage = new Fixture();
        failedStage.home.failRemove = true;
        failedStage.tick();
        int mutations = failedStage.wreck.allMutations();
        failedStage.ticks(1000);
        check(Boolean.TRUE.equals(failedStage.saved.values.get(failedStage.invalid))
                        && failedStage.home.removeAttempts == 1
                        && failedStage.home.removes == 0
                        && failedStage.wreck.allMutations() == mutations,
                "A failed detach may log once but must never retry or reset faders every tick");

        Fixture failedReveal = new Fixture();
        failedReveal.stageAsNewSave();
        failedReveal.pirate.empty = true;
        failedReveal.home.failAdd = true;
        failedReveal.ticks(1000);
        check(Boolean.TRUE.equals(failedReveal.saved.values.get(failedReveal.invalid))
                        && failedReveal.home.addAttempts == 1 && failedReveal.home.adds == 0
                        && failedReveal.saved.values.get(failedReveal.stagedToken)
                                == failedReveal.wreck.token,
                "A failed reveal must preserve its prepared token without an every-tick retry loop");

        Fixture failedCreation = new Fixture();
        failedCreation.enablePostFightCreation();
        failedCreation.home.customCreation = (name, values) -> {
            check(Boolean.TRUE.equals(failedCreation.saved.values.get(failedCreation.revealed)),
                    "A failing native factory still observes the precommitted one-shot claim");
            throw new IllegalStateException("Simulated post-defeat salvage creation failure");
        };
        failedCreation.tick();
        FleetEventListener listener = failedCreation.pirate.listeners.get(0);
        listener.reportFleetDespawnedToListener((CampaignFleetAPI) failedCreation.pirate.token,
                FleetDespawnReason.DESTROYED_BY_BATTLE, null);
        for (int repeat = 0; repeat < 1000; repeat++) {
            failedCreation.complete();
            failedCreation.tick();
            listener.reportFleetDespawnedToListener((CampaignFleetAPI) failedCreation.pirate.token,
                    FleetDespawnReason.DESTROYED_BY_BATTLE, null);
        }
        check(Boolean.TRUE.equals(failedCreation.saved.values.get(failedCreation.invalid))
                        && Boolean.TRUE.equals(failedCreation.saved.values.get(failedCreation.revealed))
                        && failedCreation.home.customCreates == 1
                        && !failedCreation.wreck.alive,
                "A failed post-defeat factory is consumed once, never an endless retry/crash loop");
    }

    public static void main(String[] args) throws Exception {
        String previousLogConfiguration = System.getProperty("log4j.defaultInitOverride");
        System.setProperty("log4j.defaultInitOverride", "true");
        Logger logger = Logger.getRootLogger();
        Level previousLogLevel = logger.getLevel();
        logger.setLevel(Level.OFF); // Invalid/failure cases intentionally exercise one-shot error paths.
        SectorAPI previousSector = Global.getSector();
        FactoryAPI previousFactory = Global.getFactory();
        SettingsAPI previousSettings = Global.getSettings();
        try {
            Global.setSettings(mock(SettingsAPI.class, (name, values) ->
                    name.equals("getColor") ? Color.WHITE : null));
            livePirateThenDefeat(false);
            livePirateThenDefeat(true);
            staleCacheSameTickDefeat();
            freshDestructionCallback(true);
            freshDestructionCallback(false);
            failClosedCases();
            completedAndMissingCases();
            stagedReferenceCases();
            failedTransitionDoesNotLoop();
            nativeMemoryBoundary();
            System.out.println("Sinni post-defeat pristine Kite reward regression: PASS");
        } finally {
            Global.setSector(previousSector);
            Global.setFactory(previousFactory);
            Global.setSettings(previousSettings);
            logger.setLevel(previousLogLevel);
            if (previousLogConfiguration == null) System.clearProperty("log4j.defaultInitOverride");
            else System.setProperty("log4j.defaultInitOverride", previousLogConfiguration);
        }
    }
}
