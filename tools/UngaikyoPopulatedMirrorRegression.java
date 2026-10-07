package chiefnavigator.quest;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.combat.threat.SwarmLauncherEffect;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import java.awt.Color;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.BiFunction;
import org.lwjgl.util.vector.Vector2f;

/**
 * Runs the real populated reflection controller and native construction/swarm
 * code against a mutable mocked combat lifecycle, not a live game simulation.
 * No controller state is manufactured: actual campaign snapshot, factory,
 * spawning, construction, destruction and resurrection paths are exercised.
 */
public final class UngaikyoPopulatedMirrorRegression {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static Object value(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == float.class) return 0f;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0d;
        return null;
    }

    private static <T> T mock(Class<T> type,
            BiFunction<String, Object[], Object> callback) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[]{type}, (proxy, method, args) -> {
                    if (method.getName().equals("hashCode")) {
                        return System.identityHashCode(proxy);
                    }
                    if (method.getName().equals("equals")) return proxy == args[0];
                    if (method.getName().equals("toString")) return type.getSimpleName()
                            + "@" + System.identityHashCode(proxy);
                    Object result = callback.apply(method.getName(), args);
                    return result == null ? value(method.getReturnType()) : result;
                }));
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static final class Variant {
        final String hull;
        final HullSize size;
        final boolean civilian;
        final Set<String> tags = new HashSet<>();
        final Set<String> permaMods = new HashSet<>();
        final Map<String, String> weapons = new LinkedHashMap<>();
        final Map<String, Variant> modules = new LinkedHashMap<>();
        final List<String> wings = new ArrayList<>();
        final ShipHullSpecAPI hullSpec;
        final ShipVariantAPI api;

        Variant(Fixture fixture, String hull, HullSize size, boolean civilian) {
            this.hull = hull;
            this.size = size;
            this.civilian = civilian;
            weapons.put("MAIN", "authored_" + hull + "_gun");
            if (hull.equals("legion") || hull.equals("heron")) wings.add("broadsword_wing");
            if (hull.equals("onslaught")) {
                modules.put("PORT", new Variant(fixture, "vambrace_port", HullSize.FRIGATE, false));
                modules.put("STARBOARD", new Variant(fixture, "vambrace_starboard", HullSize.FRIGATE, false));
            }
            hullSpec = mock(ShipHullSpecAPI.class, (name, args) -> switch (name) {
                case "getHullId", "getBaseHullId" -> hull;
                case "getHullSize" -> size;
                case "getCollisionRadius" -> size == HullSize.CAPITAL_SHIP ? 160f : 70f;
                case "getHints" -> hints();
                default -> null;
            });
            api = mock(ShipVariantAPI.class, (name, args) -> {
                switch (name) {
                    case "clone": {
                        Variant copy = new Variant(fixture, hull, size, civilian);
                        copy.tags.addAll(tags);
                        copy.permaMods.addAll(permaMods);
                        copy.weapons.clear(); copy.weapons.putAll(weapons);
                        copy.wings.clear(); copy.wings.addAll(wings);
                        copy.modules.clear();
                        for (Map.Entry<String, Variant> entry : modules.entrySet()) {
                            ShipVariantAPI cloned = entry.getValue().api.clone();
                            copy.modules.put(entry.getKey(), fixture.variants.get(cloned));
                        }
                        return copy.api;
                    }
                    case "getHullSpec": return hullSpec;
                    case "getHullSize": return size;
                    case "getHullVariantId": return hull + "_authored";
                    case "getHints": return hints();
                    case "getTags": return tags;
                    case "hasTag": return tags.contains(args[0]);
                    case "getPermaMods": return permaMods;
                    case "getFittedWings": return wings;
                    case "getModuleSlots": return new ArrayList<>(modules.keySet());
                    case "getModuleVariant": {
                        Variant module = modules.get(args[0]);
                        return module == null ? null : module.api;
                    }
                    case "getWeaponId": return weapons.get(args[0]);
                    case "addTag": tags.add((String) args[0]); break;
                    case "addPermaMod": permaMods.add((String) args[0]); break;
                }
                return null;
            });
            fixture.variants.put(api, this);
        }

        Set<ShipTypeHints> hints() {
            return civilian ? EnumSet.of(ShipTypeHints.CIVILIAN)
                    : EnumSet.noneOf(ShipTypeHints.class);
        }
    }

    private static final class Stats {
        final Map<String, MutableStat> stats = new HashMap<>();
        final Map<String, StatBonus> mods = new HashMap<>();
        final DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class, (name, args) -> {
            if (name.equals("getMod")) return mods.computeIfAbsent(
                    (String) args[0], ignored -> new StatBonus());
            if (name.equals("getValue")) return args.length > 1 ? args[1] : 1f;
            return null;
        });
        ShipAPI entity;
        final MutableShipStatsAPI api = mock(MutableShipStatsAPI.class, (name, args) -> {
            if (name.equals("getDynamic")) return dynamic;
            if (name.equals("getEntity")) return entity;
            return stats.computeIfAbsent(name,
                    ignored -> new MutableStat(name.equals("getZeroFluxSpeedBoost") ? 0f : 1f));
        });
    }

    private static final class Member {
        final String id;
        final Variant variant;
        final float dp;
        final Stats stats = new Stats();
        String name;
        String personality = Personalities.STEADY;
        String captainPersonality = Personalities.STEADY;
        int owner;
        float cr = 0.3f;
        final PersonAPI captain = mock(PersonAPI.class, (method, args) -> {
            if (method.equals("getPersonality")) return captainPersonality;
            if (method.equals("setPersonality")) captainPersonality = (String) args[0];
            return null;
        });
        final RepairTrackerAPI repair = mock(RepairTrackerAPI.class, (method, args) -> {
            if (method.equals("getMaxCR")) return 0.7f;
            if (method.equals("getCR")) return cr;
            if (method.equals("setCR")) cr = (Float) args[0];
            return null;
        });
        final FleetMemberAPI api;

        Member(Fixture fixture, String id, Variant variant, float dp) {
            this.id = id;
            this.variant = variant;
            this.dp = dp;
            this.name = id;
            api = mock(FleetMemberAPI.class, (method, args) -> {
                switch (method) {
                    case "getId": return id;
                    case "getVariant": return variant.api;
                    case "getHullSpec": return variant.hullSpec;
                    case "getHullId": return variant.hull;
                    case "getShipName": return name;
                    case "getDeploymentPointsCost": return dp;
                    case "getStats": return stats.api;
                    case "getRepairTracker": return repair;
                    case "getCaptain": return captain;
                    case "getOwner": return owner;
                    case "setOwner": owner = (Integer) args[0]; break;
                    case "setShipName": name = (String) args[0]; break;
                    case "setPersonalityOverride": personality = (String) args[0]; break;
                }
                return null;
            });
            fixture.members.put(api, this);
        }
    }

    private static final class Wing {
        final List<ShipAPI> fighters = new ArrayList<>();
        ShipAPI source;
        final FighterWingAPI api = mock(FighterWingAPI.class, (name, args) -> {
            if (name.equals("getSourceShip")) return source;
            if (name.equals("setSourceShip")) source = (ShipAPI) args[0];
            if (name.equals("getWingMembers")) return new ArrayList<>(fighters);
            if (name.equals("getWingId")) return "broadsword_wing";
            return null;
        });
    }

    private static final class Ship {
        final Member member;
        final Stats stats = new Stats();
        final Map<String, Object> custom = new HashMap<>();
        final Set<String> tags = new HashSet<>();
        final List<ShipAPI> children = new ArrayList<>();
        final List<ShipAPI> drones = new ArrayList<>();
        final List<FighterWingAPI> wings = new ArrayList<>();
        final Vector2f location = new Vector2f();
        final Vector2f velocity = new Vector2f();
        int owner, originalOwner;
        boolean alive = true, expired, fighter, module, drone, locked, holdFire, phased;
        boolean unreadableWing, unreadableHull;
        float alpha = 1f;
        CollisionClass collision = CollisionClass.SHIP;
        ShipAPI parent, droneSource;
        Wing wing;
        ShipAIConfig aiConfig = new ShipAIConfig();
        int defaultAiBindings, circumstanceEvaluations, huntOrders, mutations;
        ShipAIPlugin currentAi;
        final ShipAIPlugin ai = mock(ShipAIPlugin.class, (method, args) -> {
            if (method.equals("getConfig")) return aiConfig;
            if (method.equals("forceCircumstanceEvaluation")) circumstanceEvaluations++;
            return null;
        });
        final ShipAPI api;

        Ship(Fixture fixture, Member member, int owner) {
            this.member = member;
            this.owner = owner;
            this.originalOwner = owner;
            currentAi = ai;
            ArmorGridAPI armor = mock(ArmorGridAPI.class, (method, args) -> null);
            api = mock(ShipAPI.class, (method, args) -> {
                if (method.startsWith("set") || method.equals("resetDefaultAI")) mutations++;
                switch (method) {
                    case "getHullSpec":
                        if (unreadableHull) throw new RuntimeException("unrelated hull accessor");
                        return member.variant.hullSpec;
                    case "getVariant": return member.variant.api;
                    case "getHullSize": return member.variant.size;
                    case "getFleetMember": return member.api;
                    case "getFleetMemberId": return member.id;
                    case "getCaptain": return member.captain;
                    case "getOwner": return this.owner;
                    case "getOriginalOwner": return originalOwner;
                    case "setOwner": this.owner = (Integer) args[0]; break;
                    case "setOriginalOwner": originalOwner = (Integer) args[0]; break;
                    case "getCustomData": return custom;
                    case "setCustomData": custom.put((String) args[0], args[1]); break;
                    case "removeCustomData": custom.remove(args[0]); break;
                    case "isAlive": return alive && !expired;
                    case "isHulk": return !alive;
                    case "isExpired": return expired;
                    case "getHullLevel": return alive ? 1f : 0f;
                    case "isFighter": return fighter;
                    case "isDrone": return drone;
                    case "isStationModule": return module;
                    case "isShipWithModules": return !children.isEmpty();
                    case "getParentStation": return parent;
                    case "getChildModulesCopy": return new ArrayList<>(children);
                    case "getDroneSource": return droneSource;
                    case "getDeployedDrones": return new ArrayList<>(drones);
                    case "getAllWings": return new ArrayList<>(wings);
                    case "getWing":
                        if (unreadableWing) throw new RuntimeException("unrelated wing accessor");
                        return wing == null ? null : wing.api;
                    case "getLocation": return location;
                    case "getVelocity": return velocity;
                    case "getCollisionRadius": return fighter ? 12f : member.variant.hullSpec.getCollisionRadius();
                    case "getMaxSpeedWithoutBoost": return 100f;
                    case "getFacing": return 90f;
                    case "getMutableStats": return stats.api;
                    case "getArmorGrid": return armor;
                    case "getAllWeapons", "getWeaponGroupsCopy": return Collections.emptyList();
                    case "getShipAI": return currentAi;
                    case "setShipAI": currentAi = (ShipAIPlugin) args[0]; break;
                    case "resetDefaultAI": currentAi = ai; break;
                    case "setDefaultAI":
                        check(args[0] == member.api,
                                "Mirror AI must bind its actual spawned member");
                        defaultAiBindings++; currentAi = ai; break;
                    case "getCollisionClass": return collision;
                    case "setCollisionClass": collision = (CollisionClass) args[0]; break;
                    case "setControlsLocked": locked = (Boolean) args[0]; break;
                    case "setHoldFire": holdFire = (Boolean) args[0]; break;
                    case "setPhased": phased = (Boolean) args[0]; break;
                    case "setAlphaMult": alpha = (Float) args[0]; break;
                    case "addTag": tags.add((String) args[0]); break;
                    case "removeTag": tags.remove(args[0]); break;
                    case "hasTag": return tags.contains(args[0]);
                    case "getTags": return tags;
                }
                return null;
            });
            stats.entity = api;
            fixture.ships.put(api, this);
        }
    }

    private static final class Fixture {
        final Map<ShipVariantAPI, Variant> variants = new IdentityHashMap<>();
        final Map<FleetMemberAPI, Member> members = new IdentityHashMap<>();
        final Map<ShipAPI, Ship> ships = new IdentityHashMap<>();
        final List<FleetMemberAPI> roster = new ArrayList<>();
        final Set<ShipAPI> inPlay = Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<String, Object> data = new HashMap<>();
        final Map<FleetMemberAPI, ShipAPI> managedShips = new IdentityHashMap<>();
        final Map<ShipAPI, DeployedFleetMemberAPI> deployed = new IdentityHashMap<>();
        final Map<DeployedFleetMemberAPI, DeployedFleetMemberAPI> shards = new IdentityHashMap<>();
        final Map<ShipAPI, Integer> removalFailures = new IdentityHashMap<>();
        final Map<ShipAPI, Integer> removalAttempts = new IdentityHashMap<>();
        final List<Ship> generated = new ArrayList<>();
        boolean suppressed, paused;
        int memberSequence, constructionWings, smokeParticles;
        BaseEveryFrameCombatPlugin controller;
        Ship boss;
        final CombatTaskManagerAPI tasks = mock(CombatTaskManagerAPI.class, (method, args) -> {
            if (method.equals("orderSearchAndDestroy")) {
                ShipAPI ship = ((DeployedFleetMemberAPI) args[0]).getShip();
                ships.get(ship).huntOrders++;
            }
            return null;
        });
        final CombatFleetManagerAPI manager = mock(CombatFleetManagerAPI.class, (method, args) -> {
            switch (method) {
                case "getOwner": return 1;
                case "isSuppressDeploymentMessages": return suppressed;
                case "setSuppressDeploymentMessages": suppressed = (Boolean) args[0]; break;
                case "spawnFleetMember": {
                    Member member = members.get(args[0]);
                    check(member != null && !roster.contains(member.api),
                            "Factory must produce a combat-only member, never a player original");
                    Ship ship = create(member, 1, true);
                    // Most native/mod creators install an AI immediately. The
                    // Champion exercises the separate null-AI native handoff.
                    if (member.variant.hull.equals("champion")) ship.currentAi = null;
                    ship.location.set((Vector2f) args[1]);
                    managedShips.put(member.api, ship.api);
                    for (Variant module : member.variant.modules.values()) attachModule(ship, module);
                    if (!member.variant.wings.isEmpty()) launchFighters(ship, 2);
                    return ship.api;
                }
                case "spawnShipOrWing": {
                    check(SwarmLauncherEffect.CONSTRUCTION_SWARM_WING.equals(args[0]),
                            "Actual controller may spawn only its native construction wing here");
                    Variant variant = new Variant(this, "construction_swarm", HullSize.FIGHTER, false);
                    Ship carrier = create(new Member(this, "swarm-" + (++memberSequence), variant, 0f), 1, true);
                    carrier.fighter = true;
                    carrier.wing = new Wing();
                    carrier.wing.fighters.add(carrier.api);
                    carrier.location.set((Vector2f) args[1]);
                    constructionWings++;
                    return carrier.api;
                }
                case "getShipFor": return managedShips.get(args[0]);
                case "getDeployedFleetMember", "getDeployedFleetMemberEvenIfDisabled",
                        "getDeployedFleetMemberFromAllEverDeployed": return deployed.get(args[0]);
                case "getAllEverDeployedCopy", "getDeployedCopyDFM": return new ArrayList<>(deployed.values());
                case "getDeployedCopy": return new ArrayList<>(managedShips.keySet());
                case "getShardToOriginalShipMap": return shards;
                case "getTaskManager": return tasks;
                case "removeDeployed":
                    // Intentionally do not cascade entity removal: the real
                    // controller must explicitly finish descendant cleanup.
                    if (args[0] instanceof ShipAPI ship) {
                        Ship state = ships.get(ship);
                        if (state != null && managedShips.get(state.member.api) == ship) {
                            managedShips.remove(state.member.api);
                        }
                    }
                    break;
            }
            return null;
        });
        final CombatEngineAPI engine = mock(CombatEngineAPI.class, (method, args) -> {
            switch (method) {
                case "getFleetManager": return manager;
                case "getShips": return new ArrayList<>(inPlay);
                case "getCustomData": return data;
                case "isPaused": return paused;
                case "getMapWidth", "getMapHeight": return 24000f;
                case "isEntityInPlay": return inPlay.contains(args[0]);
                case "isShipAlive": {
                    Ship ship = ships.get(args[0]);
                    return ship != null && ship.alive && !ship.expired;
                }
                case "removeEntity": {
                    ShipAPI ship = (ShipAPI) args[0];
                    removalAttempts.merge(ship, 1, Integer::sum);
                    int remaining = removalFailures.getOrDefault(ship, 0);
                    if (remaining > 0) {
                        removalFailures.put(ship, remaining - 1);
                        throw new RuntimeException("transient engine removal failure");
                    }
                    inPlay.remove(ship);
                    ships.get(ship).expired = true;
                    break;
                }
                case "addNegativeNebulaParticle": smokeParticles++; break;
                case "addLayeredRenderingPlugin": {
                    Vector2f location = new Vector2f();
                    return mock(CombatEntityAPI.class, (name, values) ->
                            name.equals("getLocation") ? location : null);
                }
            }
            return null;
        });

        Member addSource(String id, String hull, float dp, boolean civilian) {
            Variant variant = new Variant(this, hull,
                    dp >= 40f ? HullSize.CAPITAL_SHIP : HullSize.CRUISER, civilian);
            Member member = new Member(this, id, variant, dp);
            roster.add(member.api);
            return member;
        }

        Ship create(Member member, int owner, boolean generatedShip) {
            Ship ship = new Ship(this, member, owner);
            inPlay.add(ship.api);
            if (generatedShip) generated.add(ship);
            DeployedFleetMemberAPI dfm = mock(DeployedFleetMemberAPI.class, (name, args) -> {
                if (name.equals("getShip")) return ship.api;
                if (name.equals("getMember")) return member.api;
                return null;
            });
            deployed.put(ship.api, dfm);
            return ship;
        }

        Ship attachModule(Ship root, Variant variant) {
            Ship module = create(new Member(this, root.member.id + "-" + variant.hull, variant, 0f), 1, true);
            module.module = true;
            module.parent = root.api;
            root.children.add(module.api);
            return module;
        }

        Wing launchFighters(Ship source, int count) {
            Wing wing = new Wing();
            wing.source = source.api;
            source.wings.add(wing.api);
            for (int i = 0; i < count; i++) {
                Variant variant = new Variant(this, "broadsword", HullSize.FIGHTER, false);
                Ship fighter = create(new Member(this, "fighter-" + (++memberSequence), variant, 0f),
                        source.owner, generated.contains(source));
                fighter.fighter = true;
                fighter.wing = wing;
                wing.fighters.add(fighter.api);
            }
            return wing;
        }

        Ship attachDrone(Ship source) {
            Variant variant = new Variant(this, "deployed_drone", HullSize.FRIGATE, false);
            Ship drone = create(new Member(this, "drone-" + (++memberSequence), variant, 0f), 1, true);
            drone.drone = true;
            drone.droneSource = source.api;
            source.drones.add(drone.api);
            return drone;
        }

        void install() throws Exception {
            Global.setCombatEngine(engine);
            SpriteAPI sprite = mock(SpriteAPI.class, (name, args) -> {
                if (name.equals("getWidth") || name.equals("getHeight")) return 64f;
                return null;
            });
            Global.setSettings(mock(SettingsAPI.class, (name, args) -> {
                if (name.equals("getColor")) return Color.WHITE;
                if (name.equals("getSprite")) return sprite;
                if (name.equals("createDefaultShipAI")) {
                    Ship ship = ships.get(args[0]);
                    ship.aiConfig = (ShipAIConfig) args[1];
                    return ship.ai;
                }
                if (name.equals("getVariant")) {
                    String id = (String) args[0];
                    HullSize size = id.startsWith("fabricator") ? HullSize.CAPITAL_SHIP
                            : id.startsWith("standoff") ? HullSize.CRUISER
                            : id.startsWith("assault") ? HullSize.DESTROYER : HullSize.FRIGATE;
                    return new Variant(this, id.replaceFirst("_.*", ""), size, false).api;
                }
                return null;
            }));
            Global.setFactory(mock(FactoryAPI.class, (name, args) -> {
                if (name.equals("createFleetMember")) {
                    Variant variant = variants.get(args[1]);
                    check(variant != null, "Factory must receive the actual cloned refit");
                    return new Member(this, "mirror-" + (++memberSequence), variant, 0f).api;
                }
                return null;
            }));
            Global.setSoundPlayer(mock(SoundPlayerAPI.class, (name, args) -> null));
            FleetDataAPI fleetData = mock(FleetDataAPI.class, (name, args) ->
                    name.equals("getMembersListCopy") ? new ArrayList<>(roster) : null);
            CampaignFleetAPI player = mock(CampaignFleetAPI.class, (name, args) ->
                    name.equals("getFleetData") ? fleetData : null);
            Global.setSector(mock(SectorAPI.class, (name, args) ->
                    name.equals("getPlayerFleet") ? player : null));
            Variant variant = new Variant(this,
                    GautamaFinalRematchBattleCreationPlugin.UNGAIKYO_HULL_ID,
                    HullSize.CAPITAL_SHIP, false);
            boss = create(new Member(this, "real-ungaikyo", variant, 60f), 1, false);
            Constructor<?> constructor = Class.forName(
                    "chiefnavigator.quest.GautamaFinalRematchBattleCreationPlugin$MirrorFleetController")
                    .getDeclaredConstructor(CombatEngineAPI.class);
            constructor.setAccessible(true);
            controller = (BaseEveryFrameCombatPlugin) constructor.newInstance(engine);
        }

        void advance(float seconds) {
            for (float remaining = seconds; remaining > 0.0001f;) {
                float step = Math.min(0.25f, remaining);
                controller.advance(step, Collections.emptyList());
                remaining -= step;
            }
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> slots() throws Exception {
            return (Map<String, Object>) field(controller, "slots");
        }

        Ship active(String sourceId) throws Exception {
            return ships.get(field(slots().get(sourceId), "active"));
        }

        void assertHealthy(String sourceId) throws Exception {
            Object slot = slots().get(sourceId);
            check(slot != null && !Boolean.TRUE.equals(field(slot, "disabled")),
                    "An unrelated malformed ship or recoverable cleanup failure quarantined " + sourceId);
        }

        void assertRemoved(Collection<Ship> group, String message) {
            for (Ship ship : group) check(!inPlay.contains(ship.api),
                    message + ": " + ship.member.id);
        }
    }

    private static void populatedRepeatedLifecycle() throws Exception {
        Fixture f = new Fixture();
        Member onslaught = f.addSource("player-onslaught", "onslaught", 45f, false);
        Member legion = f.addSource("player-legion", "legion", 40f, false);
        Member aurora = f.addSource("player-aurora", "aurora", 30f, false);
        f.addSource("player-ziggurat", "ziggurat", 35f, false);
        f.addSource("player-champion", "champion", 50f, false);
        f.addSource("player-heron", "heron", 40f, false);
        f.addSource("over-budget-wolf", "wolf", 5f, false);
        f.install();

        Ship playerOriginal = f.create(onslaught, 0, false);
        Ship playerCarrier = f.create(legion, 0, false);
        Wing playerWing = f.launchFighters(playerCarrier, 1);
        Member nativeMember = new Member(f, "ordinary-enemy",
                new Variant(f, "assault_unit", HullSize.DESTROYER, false), 10f);
        Ship nativeEnemy = f.create(nativeMember, 1, false);
        Ship unreadableWing = f.create(new Member(f, "unreadable-wing",
                new Variant(f, "foreign_mod_ship", HullSize.CRUISER, false), 15f), 1, false);
        unreadableWing.unreadableWing = true;
        Ship unreadableHull = f.create(new Member(f, "unreadable-hull",
                new Variant(f, "foreign_bad_hull", HullSize.CRUISER, false), 15f), 1, false);
        unreadableHull.unreadableHull = true;
        List<Ship> untouched = List.of(playerOriginal, playerCarrier,
                f.ships.get(playerWing.fighters.get(0)), nativeEnemy, unreadableWing, unreadableHull);
        check(f.slots().size() == 6 && !f.slots().containsKey("over-budget-wolf"),
                "Populate exactly the actual military 240-DP roster, excluding the excess ship");

        f.advance(0.25f);
        check(f.constructionWings == 6,
                "The real populated opening must spawn one native swarm per selected source");
        for (Object slot : f.slots().values()) {
            check(field(slot, "construction") != null && field(slot, "active") == null,
                    "Opening mirrors must be building, never silently absent");
        }
        Ship constructingOnslaught = f.ships.get(field(
                field(f.slots().get(onslaught.id), "construction"), "mirror"));
        Ship openingDetachedVambrace = f.ships.get(constructingOnslaught.children.get(0));
        constructingOnslaught.children.remove(openingDetachedVambrace.api);
        openingDetachedVambrace.parent = null;
        Ship transientSwarm = f.generated.stream()
                .filter(ship -> ship.member.variant.hull.equals("construction_swarm"))
                .findFirst().orElseThrow();
        f.removalFailures.put(transientSwarm.api, 2);
        f.advance(2.5f);
        check(f.smokeParticles > 0, "Opening must advance the real vanilla smoke emitter");
        check(!f.inPlay.contains(transientSwarm.api)
                        && f.removalAttempts.getOrDefault(transientSwarm.api, 0) >= 3,
                "A failed construction-swarm removal must retry without quarantining its healthy mirror");
        check(openingDetachedVambrace.currentAi == openingDetachedVambrace.ai
                        && openingDetachedVambrace.circumstanceEvaluations == 1
                        && openingDetachedVambrace.defaultAiBindings == 0
                        && !openingDetachedVambrace.locked && !openingDetachedVambrace.holdFire
                        && !openingDetachedVambrace.phased,
                "A live module detached during construction must retain its custom AI and receive one handoff");
        for (String id : f.slots().keySet()) {
            f.assertHealthy(id);
            Ship root = f.active(id);
            check(root != null && root.owner == 1 && !root.locked && !root.holdFire
                            && !root.phased && root.currentAi == root.ai
                            && root.defaultAiBindings == (id.equals("player-champion") ? 1 : 0)
                            && root.circumstanceEvaluations > 0 && root.huntOrders == 1,
                    "Each populated mirror must complete with a live enemy AI and hunt order: " + id);
            Member source = f.members.get(f.roster.stream()
                    .filter(member -> member.getId().equals(id)).findFirst().orElseThrow());
            check(root.member.variant != source.variant
                            && root.member.variant.weapons.equals(source.variant.weapons)
                            && root.member.variant.wings.equals(source.variant.wings),
                    "Military mirrors must clone the actual authored weapons/wings without aliasing");
            for (String berth : source.variant.modules.keySet()) {
                Variant sourceModule = source.variant.modules.get(berth);
                Variant clonedModule = root.member.variant.modules.get(berth);
                check(clonedModule != null && clonedModule != sourceModule
                                && clonedModule.weapons.equals(sourceModule.weapons),
                        "Modular military mirrors must retain independently cloned module refits");
            }
        }

        // Capture a live vambrace and mirror-drone lineage, then make native
        // attachment lists lose those references before the parent dies.
        for (int cycle = 0; cycle < 2; cycle++) {
            Ship root = f.active(onslaught.id);
            List<Ship> retired = new ArrayList<>();
            retired.add(root);
            for (ShipAPI child : root.children) retired.add(f.ships.get(child));
            if (cycle == 0) retired.add(openingDetachedVambrace);
            Ship drone = f.attachDrone(root);
            retired.add(drone);
            // A native shard can share the generated member while having no
            // module flag. Its shard map still makes it a cleanup component,
            // never a transformed primary keeping the dead root's slot alive.
            Ship shard = f.create(root.member, 1, true);
            f.shards.put(f.deployed.get(shard.api), f.deployed.get(root.api));
            retired.add(shard);
            f.advance(0.5f);
            if (cycle == 0) check(openingDetachedVambrace.circumstanceEvaluations == 1,
                    "Detached module AI handoff must not repeat on later controller frames");
            f.shards.remove(f.deployed.get(shard.api));
            Ship detachedVambrace = retired.get(1);
            root.children.remove(detachedVambrace.api);
            detachedVambrace.parent = null;
            root.drones.clear();
            drone.droneSource = null;
            int buildsAtDeath = f.constructionWings;
            if (cycle == 0) f.removalFailures.put(detachedVambrace.api, 10000);
            root.alive = false;
            f.advance(0.5f);
            f.assertHealthy(onslaught.id);
            check(f.active(onslaught.id) == null,
                    "A detached living vambrace must not count as a surviving primary root");
            if (cycle == 0) {
                f.advance(14f);
                check(f.inPlay.contains(detachedVambrace.api)
                                && f.constructionWings == buildsAtDeath
                                && field(f.slots().get(onslaught.id), "construction") == null,
                        "A source must not rebuild past cooldown while its old descendant remains");
                f.assertHealthy(onslaught.id);
                f.removalFailures.remove(detachedVambrace.api);
                f.advance(0.5f);
            }
            f.assertRemoved(retired, "Destroyed parent left a detached vambrace/drone in play");
            f.advance(26f);
            Ship rebuilt = f.active(onslaught.id);
            check(rebuilt != null && rebuilt != root && rebuilt.huntOrders == 1
                            && rebuilt.member.variant.weapons.equals(onslaught.variant.weapons),
                    "The same selected source must rebuild through real timing at least twice");
        }
        check(f.constructionWings == 8,
                "Six opening sources plus two repeated rebuilds must use eight actual constructions");

        // A native/mod transformation swaps the root object but retains the
        // generated member. No test-only ownership marker is copied to it.
        Ship oldAurora = f.active(aurora.id);
        Ship replacement = f.create(oldAurora.member, 1, true);
        oldAurora.alive = false;
        f.inPlay.remove(oldAurora.api);
        oldAurora.expired = true;
        f.managedShips.put(replacement.member.api, replacement.api);
        f.advance(0.5f);
        check(f.active(aurora.id) == replacement,
                "A same-generated-member transformation must become the existing slot's primary");
        int buildsBefore = f.constructionWings;
        f.advance(14f);
        check(f.constructionWings == buildsBefore,
                "A live transformed primary must not provoke a duplicate replacement build");

        // Capture the carrier wing, detach its source reference, and simulate
        // an engine removal failure that outlives the boss's death frame.
        Ship carrier = f.active(legion.id);
        List<Ship> carrierFighters = new ArrayList<>();
        for (FighterWingAPI wing : carrier.wings) {
            for (ShipAPI fighter : wing.getWingMembers()) carrierFighters.add(f.ships.get(fighter));
        }
        f.advance(0.5f);
        carrier.wings.clear();
        for (Ship fighter : carrierFighters) {
            fighter.wing.source = null;
            fighter.wing.fighters.clear();
            fighter.wing = null;
        }
        Ship retryVambrace = f.ships.get(f.active(onslaught.id).children.get(0));
        f.removalFailures.put(retryVambrace.api, 2);
        f.boss.alive = false;
        f.advance(0.25f);
        check(f.inPlay.contains(retryVambrace.api),
                "Fixture must actually retain the descendant after a transient removal failure");
        f.advance(3f);
        f.assertRemoved(f.generated,
                "Ungaikyo death must eventually remove every generation, detached part and fighter");
        check(f.removalAttempts.getOrDefault(retryVambrace.api, 0) >= 3,
                "Failed removal must be retried until the descendant is confirmed out of play");
        check(f.constructionWings == buildsBefore,
                "Boss collapse must never reconstruct another reflection");
        for (Ship ship : untouched) {
            check(f.inPlay.contains(ship.api) && ship.mutations == 0,
                    "Cleanup or AI policy touched a player/native/unreadable unrelated ship");
        }
        for (FleetMemberAPI member : f.roster) {
            Member source = f.members.get(member);
            check(source.variant.tags.isEmpty() && source.variant.permaMods.isEmpty()
                            && source.owner == 0 && Personalities.STEADY.equals(source.personality)
                            && Personalities.STEADY.equals(source.captainPersonality),
                    "Snapshot/construction mutated original campaign member " + source.id);
            for (Variant module : source.variant.modules.values()) {
                check(module.tags.isEmpty() && module.permaMods.isEmpty(),
                        "Mirror modular cloning mutated an original module refit");
            }
        }
    }

    private static void populatedCivilianFallback() throws Exception {
        Fixture f = new Fixture();
        f.addSource("military", "onslaught", 90f, false);
        Member first = f.addSource("civilian-one", "tanker", 60f, true);
        Member second = f.addSource("civilian-two", "freighter", 50f, true);
        f.addSource("unneeded-civilian", "transport", 40f, true);
        f.install();
        f.advance(3f);
        check(f.slots().size() == 3 && !f.slots().containsKey("unneeded-civilian"),
                "Civilian fallback must stop after the real selected pool reaches 200 DP");
        for (Member civilian : List.of(first, second)) {
            Ship mirror = f.active(civilian.id);
            check(mirror != null && mirror.member.variant.hull.equals("fabricator")
                            && mirror.member.variant.size == HullSize.CAPITAL_SHIP,
                    "Selected civilian sources must build their stock same-size Threat replacements");
            check(civilian.variant.hull.equals(civilian == first ? "tanker" : "freighter")
                            && civilian.variant.tags.isEmpty() && civilian.variant.permaMods.isEmpty(),
                    "Civilian substitution must never rewrite a player original");
        }
        f.boss.alive = false;
        f.advance(1f);
        f.assertRemoved(f.generated, "Civilian mirror pool must also fully collapse");
    }

    private static void populatedSnapshotExclusions() throws Exception {
        Fixture f = new Fixture();
        Member optedOut = f.addSource("opted-out-root", "excluded_capital", 60f, false);
        optedOut.variant.tags.add(MirrorCompatibility.OPT_OUT_TAG);
        Member nestedOptOut = f.addSource("opted-out-nested-module", "onslaught", 45f, false);
        Variant nestedModule = new Variant(f, "excluded_nested_vambrace", HullSize.FRIGATE, false);
        nestedModule.tags.add(MirrorCompatibility.OPT_OUT_TAG);
        nestedOptOut.variant.modules.get("PORT").modules.put("INNER", nestedModule);
        f.addSource("eligible-onslaught", "onslaught", 45f, false);
        f.addSource("eligible-legion", "legion", 40f, false);
        f.addSource("eligible-aurora", "aurora", 30f, false);
        f.addSource("eligible-ziggurat", "ziggurat", 35f, false);
        f.addSource("eligible-champion", "champion", 50f, false);
        f.addSource("eligible-heron", "heron", 40f, false);
        f.addSource("over-budget-exclusion-tail", "wolf", 5f, false);
        f.install();

        Set<String> expected = Set.of("eligible-onslaught", "eligible-legion",
                "eligible-aurora", "eligible-ziggurat", "eligible-champion", "eligible-heron");
        check(f.slots().keySet().equals(expected),
                "Root/nested-module opt-outs must consume no DP before the eligible 240-DP pool");
        float selectedDP = 0f;
        for (Object slot : f.slots().values()) selectedDP += (Float) field(slot, "deploymentPoints");
        check(selectedDP == 240f,
                "Eligible ships must still fill the full military budget after preceding exclusions");
        f.advance(3f);
        check(f.constructionWings == expected.size(),
                "Excluded sources must spawn no mirror or construction swarm");
        for (String sourceId : expected) {
            f.assertHealthy(sourceId);
            check(f.active(sourceId) != null,
                    "Every eligible source must construct normally beside excluded campaign members");
        }
        check(optedOut.variant.tags.equals(Set.of(MirrorCompatibility.OPT_OUT_TAG))
                        && nestedModule.tags.equals(Set.of(MirrorCompatibility.OPT_OUT_TAG))
                        && optedOut.variant.permaMods.isEmpty()
                        && nestedOptOut.variant.permaMods.isEmpty()
                        && nestedModule.permaMods.isEmpty(),
                "Exclusion queries must not rewrite opted-out campaign refits or their nested modules");
        f.boss.alive = false;
        f.advance(1f);
        f.assertRemoved(f.generated, "Eligible mirrors in the excluded-source fixture must collapse");
    }

    public static void main(String[] args) throws Exception {
        SettingsAPI settings = Global.getSettings();
        FactoryAPI factory = Global.getFactory();
        SectorAPI sector = Global.getSector();
        CombatEngineAPI engine = Global.getCombatEngine();
        SoundPlayerAPI sound = Global.getSoundPlayer();
        try {
            populatedSnapshotExclusions();
            populatedRepeatedLifecycle();
            populatedCivilianFallback();
            System.out.println("PASS: real populated mirror controller on a mocked combat lifecycle; "
                    + "military/civilian selection, root/nested-module exclusions, native construction smoke/swarms, carriers/modules, "
                    + "two rebuilds, detached vambraces/drones/shards, transformed root, retrying collapse "
                    + "and untouched player/native enemies.");
        } finally {
            Global.setSettings(settings);
            Global.setFactory(factory);
            Global.setSector(sector);
            Global.setCombatEngine(engine);
            Global.setSoundPlayer(sound);
        }
    }
}
