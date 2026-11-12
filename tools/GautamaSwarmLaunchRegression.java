import chiefnavigator.hullmods.GautamaSwarmLaunch;
import chiefnavigator.hullmods.ScyllaFragileSwarmHullmod;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.threat.FragmentSwarmHullmod;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect.RoilingSwarmParams;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect.SwarmMember;
import com.fs.starfarer.api.impl.combat.threat.ThreatShipConstructionScript;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import java.awt.Color;
import java.lang.reflect.Constructor;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;

/** Real native fragment transfers behind both carrier and staged Gautama screens. */
public final class GautamaSwarmLaunchRegression extends WallResearchRegression {
    private static final String ASSEMBLED = "chief_navigator_gautama_swarm_assembled";
    private static final String WING = "chief_navigator_scylla_attack_swarm_wing";

    private static final class Ship {
        final Map<String, Object> data = new HashMap<>();
        final Set<String> tags = new HashSet<>();
        final Vector2f location = new Vector2f(1000f, -400f);
        final Vector2f velocity = new Vector2f(30f, -20f);
        final ShipAPI api;
        final boolean fighter;
        FighterWingAPI wing;
        ShipAPI source;
        boolean alive = true, hulk, hidden;
        float alpha = 1f;
        int owner = 1, positionWrites;

        Ship(String hull, boolean fighter) {
            this.fighter = fighter;
            Map<String, MutableStat> dynamicStats = new HashMap<>();
            Map<String, StatBonus> dynamicMods = new HashMap<>();
            DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class, (name, args) -> {
                if (name.equals("getValue")) return args.length > 1 ? args[1] : 1f;
                if (name.equals("getStat")) return dynamicStats.computeIfAbsent((String) args[0], key -> new MutableStat(1f));
                if (name.equals("getMod")) return dynamicMods.computeIfAbsent((String) args[0], key -> new StatBonus());
                return null;
            });
            MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, (name, args) -> {
                if (name.equals("getDynamic")) return dynamic;
                if (name.equals("getZeroFluxSpeedBoost")) return new MutableStat(0f);
                return null;
            });
            ShipHullSpecAPI spec = mock(ShipHullSpecAPI.class, (name, args) -> {
                if (name.equals("getHullId") || name.equals("getBaseHullId")) return hull;
                return null;
            });
            ArmorGridAPI armor = mock(ArmorGridAPI.class, (name, args) -> null);
            api = mock(ShipAPI.class, (name, args) -> {
                if (name.equals("getHullSpec")) return spec;
                if (name.equals("getMutableStats")) return stats;
                if (name.equals("getArmorGrid")) return armor;
                if (name.equals("getAllWeapons")) return Collections.emptyList();
                if (name.equals("getCustomData")) return data;
                if (name.equals("setCustomData")) data.put((String) args[0], args[1]);
                if (name.equals("getLocation")) return location;
                if (name.equals("getVelocity")) return velocity;
                if (name.equals("setLocation")) positionWrites++;
                if (name.equals("getOwner") || name.equals("getOriginalOwner")) return owner;
                if (name.equals("setOwner")) owner = (Integer) args[0];
                if (name.equals("getHullSize")) return fighter ? ShipAPI.HullSize.FIGHTER : ShipAPI.HullSize.CAPITAL_SHIP;
                if (name.equals("getCollisionRadius")) return fighter ? 20f : 279f;
                if (name.equals("getMaxSpeedWithoutBoost")) return fighter ? 180f : 60f;
                if (name.equals("getAlphaMult")) return alpha;
                if (name.equals("getWing")) return wing;
                if (name.equals("isFighter")) return fighter;
                if (name.equals("isAlive")) return alive;
                if (name.equals("isHulk")) return hulk;
                if (name.equals("hasTag")) return tags.contains(args[0]);
                if (name.equals("addTag")) tags.add((String) args[0]);
                if (name.equals("removeTag")) tags.remove(args[0]);
                if (name.equals("setDoNotRender")) hidden = (Boolean) args[0];
                return null;
            });
        }

        void attach(ShipAPI parent) {
            source = parent;
            wing = mock(FighterWingAPI.class, (name, args) -> {
                if (name.equals("getWingId")) return WING;
                if (name.equals("getSourceShip")) return source;
                if (name.equals("setSourceShip")) source = (ShipAPI) args[0];
                if (name.equals("getWingMembers")) return Collections.singletonList(api);
                if (name.equals("getLeader")) return api;
                return null;
            });
        }
    }

    private static final class Fixture {
        final Map<String, Object> data = new HashMap<>();
        final List<Ship> launched = new ArrayList<>();
        final List<CombatLayeredRenderingPlugin> rendered = new ArrayList<>();
        final List<BaseEveryFrameCombatPlugin> removed = new ArrayList<>();
        final Ship source = new Ship("chief_navigator_scylla", false);
        final CombatEngineAPI engine;
        boolean paused, suppressMessages;

        Fixture() {
            CombatFleetManagerAPI manager = mock(CombatFleetManagerAPI.class, (name, args) -> {
                if (name.equals("isSuppressDeploymentMessages")) return suppressMessages;
                if (name.equals("setSuppressDeploymentMessages")) suppressMessages = (Boolean) args[0];
                if (name.equals("spawnShipOrWing")) {
                    check(WING.equals(args[0]) && suppressMessages, "Staged launches retain their exact wing and quiet deployment");
                    Ship child = child();
                    child.location.set((Vector2f) args[1]);
                    new ScyllaFragileSwarmHullmod().applyEffectsAfterShipCreation(child.api, "test");
                    launched.add(child);
                    return child.api;
                }
                return null;
            });
            engine = mock(CombatEngineAPI.class, (name, args) -> {
                if (name.equals("getCustomData")) return data;
                if (name.equals("isPaused")) return paused;
                if (name.equals("isEntityInPlay")) return true;
                if (name.equals("getFleetManager")) return manager;
                if (name.equals("removePlugin")) removed.add((BaseEveryFrameCombatPlugin) args[0]);
                if (name.equals("addLayeredRenderingPlugin")) {
                    CombatLayeredRenderingPlugin plugin = (CombatLayeredRenderingPlugin) args[0];
                    rendered.add(plugin);
                    Vector2f location = new Vector2f(), velocity = new Vector2f();
                    CombatEntityAPI entity = mock(CombatEntityAPI.class, (call, ignored) -> {
                        if (call.equals("getLocation")) return location;
                        if (call.equals("getVelocity")) return velocity;
                        return null;
                    });
                    plugin.init(entity);
                    return entity;
                }
                return null;
            });
            Global.setCombatEngine(engine);
        }

        Ship child() {
            Ship child = new Ship("chief_navigator_scylla_attack_swarm", true);
            child.location.set(source.location.x + 45f, source.location.y);
            child.attach(source.api);
            return child;
        }

        RoilingSwarmEffect registeredCloud(int count) {
            RoilingSwarmParams params = new RoilingSwarmParams();
            params.withInitialMembers = false;
            params.withRespawn = false;
            params.minOffset = 350f;
            params.maxOffset = 400f;
            params.spawnOffsetMult = 1f;
            RoilingSwarmEffect cloud = new RoilingSwarmEffect(source.api, params);
            for (int index = 0; index < count; index++) {
                SwarmMember fragment = cloud.addMember();
                double angle = index * Math.PI * 2d / count;
                fragment.loc.set(source.location.x + (float) Math.cos(angle) * 380f,
                        source.location.y + (float) Math.sin(angle) * 380f);
                fragment.vel.set(index + 1f, -index - 2f);
                fragment.fader.forceIn();
            }
            return cloud;
        }
    }

    private static void ordinaryTransfer() {
        Fixture f = new Fixture();
        RoilingSwarmEffect parent = f.registeredCloud(80);
        Ship child = f.child();
        ScyllaFragileSwarmHullmod hullmod = new ScyllaFragileSwarmHullmod();
        hullmod.applyEffectsAfterShipCreation(child.api, "test");
        RoilingSwarmEffect carried = RoilingSwarmEffect.getSwarmFor(child.api);
        check(carried != null && carried.getMembers().isEmpty()
                        && !carried.getParams().withInitialMembers && !carried.getParams().withRespawn,
                "A new bay fighter must not generate a visible fragment cloud at the hull");
        hullmod.applyEffectsAfterShipCreation(child.api, "test");
        check(RoilingSwarmEffect.getSwarmFor(child.api) == carried,
                "Repeated setup retains the same native child cloud");
        Map<SwarmMember, Vector2f> locations = new IdentityHashMap<>(), velocities = new IdentityHashMap<>();
        for (SwarmMember fragment : parent.getMembers()) {
            locations.put(fragment, new Vector2f(fragment.loc));
            velocities.put(fragment, new Vector2f(fragment.vel));
        }
        Vector2f childLocation = new Vector2f(child.location), childVelocity = new Vector2f(child.velocity);
        f.paused = true;
        hullmod.advanceInCombat(child.api, 2f);
        check(carried.getMembers().isEmpty() && !child.data.containsKey(ASSEMBLED),
                "Pause must not consume or permanently mark an ordinary cloud handoff");
        f.paused = false;
        hullmod.advanceInCombat(child.api, 0f);
        check(carried.getMembers().isEmpty(), "Zero combat time must not assemble a new attack swarm");
        hullmod.advanceInCombat(child.api, .1f);
        check(Boolean.TRUE.equals(child.data.get(ASSEMBLED)) && carried.getMembers().size() == 50
                        && parent.getNumActiveMembers() == 80
                        && RoilingSwarmEffect.getSwarmFor(f.source.api) == parent,
                "An ordinary launch moves fifty existing fragments and restores the parent's active reserve");
        for (SwarmMember fragment : carried.getMembers()) {
            check(locations.containsKey(fragment) && fragment.loc.equals(locations.get(fragment))
                            && fragment.vel.equals(velocities.get(fragment)),
                    "Actual native fragment identities, world positions and velocity survive the handoff");
            check(Vector2f.sub(fragment.loc, f.source.location, new Vector2f()).length() > 279f,
                    "The visible launch starts in the surrounding cloud beyond Gautama's hull");
        }
        check(child.location.equals(childLocation) && child.velocity.equals(childVelocity) && child.positionWrites == 0,
                "The hidden fighter's carrier-controlled position and motion must not be teleported");
        List<SwarmMember> members = new ArrayList<>(carried.getMembers());
        hullmod.advanceInCombat(child.api, 100f);
        check(carried.getMembers().equals(members) && parent.getNumActiveMembers() == 80,
                "The same bay fighter receives its visible fragment handoff only once");

        Ship unrelatedCarrier = new Ship("fabricator_unit", false);
        Ship foreignChild = f.child();
        foreignChild.attach(unrelatedCarrier.api);
        hullmod.applyEffectsAfterShipCreation(foreignChild.api, "test");
        hullmod.advanceInCombat(foreignChild.api, .1f);
        check(!foreignChild.data.containsKey(ASSEMBLED)
                        && RoilingSwarmEffect.getSwarmFor(foreignChild.api).getMembers().isEmpty()
                        && parent.getNumActiveMembers() == 80,
                "A custom attack wing on an unrelated carrier must not steal Gautama's cloud or gain its handoff");
    }

    private static void boundedFillAndPrivateCloud() {
        Fixture f = new Fixture();
        RoilingSwarmEffect parent = f.registeredCloud(0);
        Ship child = f.child();
        Vector2f location = new Vector2f(child.location), velocity = new Vector2f(child.velocity);
        check(GautamaSwarmLaunch.assembleFrom(f.source.api, child.api),
                "A newly created empty parent cloud must be filled safely without waiting for native initial-spawn timing");
        check(RoilingSwarmEffect.getSwarmFor(child.api).getMembers().size() == 50
                        && parent.getNumActiveMembers() == 0 && child.location.equals(location)
                        && child.velocity.equals(velocity) && parent.getParams().spawnOffsetMult == 1f,
                "The bounded handoff preserves empty-parent ammo, parameters and existing fighter motion");

        f = new Fixture();
        f.paused = true;
        GautamaSwarmLaunch.maintainCloud(f.source.api, 1f);
        check(f.rendered.isEmpty(), "Pause must not create Gautama's private visual cloud");
        f.paused = false;
        GautamaSwarmLaunch.maintainCloud(f.source.api, .1f);
        check(RoilingSwarmEffect.getSwarmFor(f.source.api) == null,
                "A new Gautama visual cloud must not register a fragment-ammo source for his disabled fragment guns");
        RoilingSwarmEffect privateCloud = null;
        for (CombatLayeredRenderingPlugin plugin : f.rendered) {
            if (plugin instanceof RoilingSwarmEffect
                    && ((RoilingSwarmEffect) plugin).getAttachedTo() == f.source.api) privateCloud = (RoilingSwarmEffect) plugin;
        }
        check(privateCloud != null && privateCloud.getParams().memberExchangeClass == null,
                "Private native fragments remain visible without enabling shared fragment-ammo exchange");
        int clouds = f.rendered.size();
        GautamaSwarmLaunch.maintainCloud(f.source.api, .1f);
        check(f.rendered.size() == clouds, "Repeated maintenance must not duplicate a live visual cloud");
        Ship privateChild = f.child();
        check(GautamaSwarmLaunch.assembleFrom(f.source.api, privateChild.api)
                        && RoilingSwarmEffect.getSwarmFor(privateChild.api).getMembers().size() == 50
                        && RoilingSwarmEffect.getSwarmFor(f.source.api) == null,
                "Ordinary cloud assembly must work without exposing the private source as weapon ammo");
        f.source.alive = false;
        f.source.hulk = true;
        GautamaSwarmLaunch.maintainCloud(f.source.api, .1f);
        check(privateCloud.isForceDespawn(), "Gautama's death must retire only his private visual fragment cloud");
    }

    private static BaseEveryFrameCombatPlugin deployment(Fixture f) throws Exception {
        Class<?> type = Class.forName("chiefnavigator.hullmods.GautamaReincarnation$ReincarnationSwarmDeployment");
        Constructor<?> constructor = type.getDeclaredConstructor(CombatEngineAPI.class, ShipAPI.class, int.class, float.class);
        constructor.setAccessible(true);
        return (BaseEveryFrameCombatPlugin) constructor.newInstance(f.engine, f.source.api, 1, 35f);
    }

    private static void stagedCadenceAndLifetime() throws Exception {
        Fixture f = new Fixture();
        RoilingSwarmEffect parent = f.registeredCloud(80);
        f.source.tags.add(ThreatShipConstructionScript.SHIP_UNDER_CONSTRUCTION);
        BaseEveryFrameCombatPlugin staged = deployment(f);
        staged.advance(10f, Collections.emptyList());
        check(f.launched.isEmpty(), "Reincarnation construction must finish before the staged screen starts");
        f.source.tags.clear();
        staged.advance(.1f, Collections.emptyList());
        staged.advance(1.249f, Collections.emptyList());
        check(f.launched.isEmpty(), "The first staged wing waits its full 1.25 combat seconds");
        f.paused = true;
        staged.advance(100f, Collections.emptyList());
        check(f.launched.isEmpty(), "Pause must not accelerate the staged launch clock");
        f.paused = false;
        staged.advance(.002f, Collections.emptyList());
        check(f.launched.size() == 1, "Only one staged wing launches at the first interval");
        for (int index = 1; index < 20; index++) staged.advance(1.25f, Collections.emptyList());
        check(f.launched.size() == 20 && f.removed.size() == 1 && !f.suppressMessages
                        && parent.getNumActiveMembers() == 80,
                "Reconstruction retains exactly twenty detached launches at 1.25 seconds and restores message state/ammo");
        for (Ship child : f.launched) {
            check(child.source == null && child.owner == 1 && child.hidden
                            && Boolean.TRUE.equals(child.data.get(ASSEMBLED))
                            && RoilingSwarmEffect.getSwarmFor(child.api).getMembers().size() == 50,
                    "Each staged wing keeps its owner, hidden carrier, detached lifetime and fifty native fragments");
            Vector2f relative = Vector2f.sub(child.velocity, f.source.velocity, new Vector2f());
            check(Math.abs(relative.length() - 160f) < .001f,
                    "Cloud-origin appearance preserves staged attack-swarm launch speed");
        }
        staged.advance(100f, Collections.emptyList());
        check(f.launched.size() == 20, "An expired staged deployment cannot launch a twenty-first wing");

        f = new Fixture();
        f.registeredCloud(80);
        staged = deployment(f);
        staged.advance(.1f, Collections.emptyList());
        staged.advance(100f, Collections.emptyList());
        check(f.launched.size() == 1,
                "A long frame must preserve the existing one-wing-per-update staging instead of bursting the whole screen");

        f = new Fixture();
        f.registeredCloud(80);
        staged = deployment(f);
        staged.advance(.1f, Collections.emptyList());
        f.source.alive = false;
        f.source.hulk = true;
        staged.advance(100f, Collections.emptyList());
        check(f.launched.isEmpty() && f.removed.size() == 1,
                "Gautama's destruction cancels remaining staged launches immediately");
    }

    public static void main(String[] args) throws Exception {
        CombatEngineAPI previousEngine = Global.getCombatEngine();
        SettingsAPI previousSettings = Global.getSettings();
        SoundPlayerAPI previousSound = Global.getSoundPlayer();
        try {
            SpriteAPI sprite = mock(SpriteAPI.class, (name, values) -> {
                if (name.equals("getWidth") || name.equals("getHeight")) return 64f;
                return null;
            });
            Global.setSettings(mock(SettingsAPI.class, (name, values) -> {
                if (name.equals("getColor")) return Color.WHITE;
                if (name.equals("getSprite")) return sprite;
                return null;
            }));
            Global.setSoundPlayer(mock(SoundPlayerAPI.class, (name, values) -> null));
            ordinaryTransfer();
            boundedFillAndPrivateCloud();
            stagedCadenceAndLifetime();
            System.out.println("PASS: actual native cloud-fragment handoff, unchanged fragment identity/world motion, "
                    + "ammo-neutral/private source lifecycle, one-shot bay assembly, and paused twenty-wing reincarnation cadence.");
        } finally {
            Global.setCombatEngine(previousEngine);
            Global.setSettings(previousSettings);
            Global.setSoundPlayer(previousSound);
        }
    }
}
