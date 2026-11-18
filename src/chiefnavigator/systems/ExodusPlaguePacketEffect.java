package chiefnavigator.systems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.combat.threat.FragmentSwarmHullmod;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect.RoilingSwarmParams;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect.SwarmMember;
import com.fs.starfarer.api.impl.combat.threat.SwarmLauncherEffect;
import com.fs.starfarer.api.impl.combat.threat.ThreatSwarmAI;
import com.fs.starfarer.api.util.Misc;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.lwjgl.util.vector.Vector2f;

/** Interceptable kinetic packets which become native, autonomous attack swarms on impact. */
public final class ExodusPlaguePacketEffect implements OnHitEffectPlugin {
    public static final String PACKET_ID = "chief_navigator_exodus_plague_packet";
    public static final float RANGE = 7500f;
    public static final float SPEED = 900f;
    // Including the ten-unit fragment envelope: 220 at the muzzle, 286 at full range.
    public static final float HALF_WIDTH = 100f;
    public static final float FAR_HALF_WIDTH = 133f;
    public static final int FRAGMENTS_PER_PACKET = 8;
    public static final float SWARM_LIFETIME = 20f;
    public static final int CONVERSION_INTERVAL = 3;
    public static final int MAX_SWARMS_PER_LAUNCHER = 10;
    private static final String CONTROLLER_KEY = PACKET_ID + "_converted";
    private static final String CONVERTED_KEY = PACKET_ID + "_hit";
    private static final String LAUNCHERS_KEY = PACKET_ID + "_launchers";
    private static final String LAUNCHER_KEY = PACKET_ID + "_launcher";
    private static final String ELIGIBLE_KEY = PACKET_ID + "_eligible";

    private static final class LauncherState {
        int fired;
        int active;
        FighterWingAPI formation;
    }

    @SuppressWarnings("unchecked")
    private static LauncherState launcherFor(Object emitter, CombatEngineAPI engine) {
        Map<Object, LauncherState> launchers =
                (Map<Object, LauncherState>) engine.getCustomData().get(LAUNCHERS_KEY);
        if (launchers == null) {
            launchers = new IdentityHashMap<>();
            engine.getCustomData().put(LAUNCHERS_KEY, launchers);
        }
        return launchers.computeIfAbsent(emitter, ignored -> new LauncherState());
    }

    public static void launch(ShipAPI source, Object emitter, Vector2f origin, float facing, CombatEngineAPI engine) {
        launch(source, emitter, origin, facing, engine, 1f);
    }

    public static void launch(ShipAPI source, Object emitter, Vector2f origin, float facing, CombatEngineAPI engine,
            float widthScale) {
        CombatEntityAPI projectile = engine.spawnProjectile(source, null, PACKET_ID,
                new Vector2f(origin), facing, new Vector2f(source.getVelocity()));
        if (projectile instanceof DamagingProjectileAPI) {
            engine.applyDamageModifiersToSpawnedProjectileWithNullWeapon(source, WeaponAPI.WeaponType.MISSILE,
                    false, ((DamagingProjectileAPI) projectile).getDamage());
            initialize((DamagingProjectileAPI) projectile, emitter, engine, widthScale);
        }
    }

    public static void initialize(DamagingProjectileAPI projectile, Object emitter, CombatEngineAPI engine) {
        initialize(projectile, emitter, engine, 1f);
    }

    public static void initialize(DamagingProjectileAPI projectile, Object emitter, CombatEngineAPI engine, float widthScale) {
        if (!(projectile instanceof MissileAPI) || engine == null) return;
        widthScale = Math.max(.08f, Math.min(1f, widthScale));
        MissileAPI missile = (MissileAPI) projectile;
        // No fitted weapon supplies these fields when a ship system launches a missile.
        missile.setWeaponSpec(PACKET_ID);
        missile.setMaxRange(RANGE);
        Vector2f forward = Misc.getUnitVectorAtDegreeAngle(missile.getFacing());
        float scatter = ThreadLocalRandom.current().nextFloat() * 24f - 12f;
        missile.getLocation().x += forward.x * scatter;
        missile.getLocation().y += forward.y * scatter;
        if (emitter != null) {
            LauncherState launcher = launcherFor(emitter, engine);
            launcher.fired = (launcher.fired + 1) % CONVERSION_INTERVAL;
            projectile.setCustomData(LAUNCHER_KEY, launcher);
            projectile.setCustomData(ELIGIBLE_KEY, launcher.fired == 0);
        }
        // Concentrate the core and make the sides irregular, rather than uniformly filling a ribbon.
        float lane = (ThreadLocalRandom.current().nextFloat() + ThreadLocalRandom.current().nextFloat() - 1f) * widthScale;
        configureFlight(missile, lane);
        // Unguided, with ordinary missile collision and hitpoints: PD destroys real packets.
        missile.setMissileAI(new SwarmFlight(missile, engine, forward, lane,
                ThreadLocalRandom.current().nextFloat() * 6.2831853f,
                1.8f + ThreadLocalRandom.current().nextFloat() * 2.4f,
                (8f + ThreadLocalRandom.current().nextFloat() * 14f) * widthScale));
        RoilingSwarmParams params = new RoilingSwarmParams();
        params.withInitialMembers = false;
        params.withRespawn = false;
        params.baseMembersToMaintain = 0;
        params.removeMembersAboveMaintainLevel = false;
        params.despawnSound = null;
        params.maxOffset = 10f * widthScale;
        params.maxSpeed = SPEED + 200f;
        params.minDespawnTime = 0.1f;
        params.maxDespawnTime = 0.25f;
        params.minFadeoutTime = 0.1f;
        params.maxFadeoutTime = 0.25f;
        RoilingSwarmEffect cloud = new RoilingSwarmEffect(missile, params);
        cloud.addMembers(FRAGMENTS_PER_PACKET);
        for (SwarmMember member : cloud.getMembers()) {
            member.loc.set(Misc.getPointWithinRadius(missile.getLocation(), params.maxOffset));
            member.vel.set(missile.getVelocity());
            member.fader.setDurationIn(0.05f);
        }
    }

    /** Smooth stochastic paths, without homing or widening the requested stream envelope. */
    public static final class SwarmFlight implements MissileAIPlugin {
        private final MissileAPI missile;
        private final CombatEngineAPI engine;
        private final Vector2f forward, sideways, inherited;
        private final float lane, phase, frequency, amplitude;
        private float elapsed;
        public SwarmFlight(MissileAPI missile, CombatEngineAPI engine, Vector2f forward,
                float lane, float phase, float frequency, float amplitude) {
            this.missile = missile;
            this.engine = engine;
            this.forward = new Vector2f(forward);
            this.sideways = new Vector2f(-forward.y, forward.x);
            this.inherited = missile.getSource() == null ? new Vector2f() : new Vector2f(missile.getSource().getVelocity());
            this.lane = lane;
            this.phase = phase;
            this.frequency = frequency;
            this.amplitude = Math.min(22f, amplitude) * (1f - Math.abs(lane));
        }
        private float lateralOffset(float time) {
            return lane * (FAR_HALF_WIDTH - HALF_WIDTH) * SPEED / RANGE * time
                    + amplitude * ((float) Math.sin(phase + time * frequency) - (float) Math.sin(phase));
        }
        @Override
        public void advance(float amount) {
            if (engine.isPaused() || amount <= 0f || missile.isFizzling() || missile.isFading()) return;
            float previous = lateralOffset(elapsed);
            elapsed += amount;
            float lateralSpeed = (lateralOffset(elapsed) - previous) / amount;
            Vector2f velocity = new Vector2f(forward.x * SPEED + sideways.x * lateralSpeed,
                    forward.y * SPEED + sideways.y * lateralSpeed);
            missile.setFacing(Misc.getAngleInDegrees(velocity));
            Vector2f.add(velocity, inherited, velocity);
            missile.getVelocity().set(velocity);
            missile.giveCommand(ShipCommand.ACCELERATE);
        }
    }

    /** Public to make geometry checks exercise the same code used at launch. */
    public static void configureFlight(MissileAPI missile, float lane) {
        lane = Math.max(-1f, Math.min(1f, lane));
        Vector2f forward = Misc.getUnitVectorAtDegreeAngle(missile.getFacing());
        Vector2f sideways = new Vector2f(-forward.y, forward.x);
        missile.getLocation().x += sideways.x * HALF_WIDTH * lane;
        missile.getLocation().y += sideways.y * HALF_WIDTH * lane;
        missile.setStart(new Vector2f(missile.getLocation()));
        float lateralSpeed = SPEED * (FAR_HALF_WIDTH - HALF_WIDTH) / RANGE * lane;
        Vector2f velocity = new Vector2f(forward.x * SPEED + sideways.x * lateralSpeed,
                forward.y * SPEED + sideways.y * lateralSpeed);
        missile.setFacing(Misc.getAngleInDegrees(velocity));
        if (missile.getSource() != null) {
            Vector2f.add(velocity, missile.getSource().getVelocity(), velocity);
        }
        missile.getVelocity().set(velocity);
    }

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point,
            boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        // Native damage has already applied the kinetic hit, including to shields.
        // Interception, range expiry and asteroid hits never create attack swarms.
        if (!(projectile instanceof MissileAPI) || !(target instanceof ShipAPI)
                || engine == null || target.getOwner() == projectile.getOwner()
                || projectile.getOwner() < 0 || projectile.getOwner() > 1
                || !Boolean.TRUE.equals(projectile.getCustomData().get(ELIGIBLE_KEY))
                || Boolean.TRUE.equals(projectile.getCustomData().get(CONVERTED_KEY))) return;
        projectile.setCustomData(CONVERTED_KEY, true);
        LauncherState launcher = (LauncherState) projectile.getCustomData().get(LAUNCHER_KEY);
        if (launcher == null) return;
        Controller controller = (Controller) engine.getCustomData().get(CONTROLLER_KEY);
        if (controller == null) {
            controller = new Controller(engine);
            engine.getCustomData().put(CONTROLLER_KEY, controller);
            engine.addPlugin(controller);
        }
        controller.convert((MissileAPI) projectile, (ShipAPI) target, point, launcher);
    }

    private static final class ReleasedSwarm {
        final ShipAPI ship;
        final LauncherState launcher;
        final RoilingSwarmEffect cloud;
        float remaining = SWARM_LIFETIME;
        ReleasedSwarm(ShipAPI ship, LauncherState launcher, RoilingSwarmEffect cloud) {
            this.ship = ship;
            this.launcher = launcher;
            this.cloud = cloud;
        }
    }

    /** One combat-local controller; native Threat AI flies and fights every released swarm. */
    private static final class Controller extends BaseEveryFrameCombatPlugin {
        private final CombatEngineAPI engine;
        private final List<ReleasedSwarm> swarms = new ArrayList<>();
        Controller(CombatEngineAPI engine) { this.engine = engine; }

        void convert(MissileAPI packet, ShipAPI target, Vector2f point, LauncherState launcher) {
            retireExpired(0f);
            if (launcher.active >= MAX_SWARMS_PER_LAUNCHER) return;
            RoilingSwarmEffect incoming = RoilingSwarmEffect.getSwarmFor(packet);
            int fragments = Math.max(1, Math.min(FRAGMENTS_PER_PACKET,
                    (int) Math.ceil(FRAGMENTS_PER_PACKET * packet.getHullLevel())));
            int capacity = SwarmLauncherEffect.FRAGMENT_NUM.get(SwarmLauncherEffect.ATTACK_SWARM_WING);
            ReleasedSwarm destination = spawn(packet, target, point, launcher);
            if (destination == null) return;
            int before = destination.cloud.getNumActiveMembers();
            if (incoming != null) incoming.transferMembersTo(destination.cloud, fragments);
            int missing = fragments - (destination.cloud.getNumActiveMembers() - before);
            if (missing > 0) destination.cloud.addMembers(missing);
            destination.ship.setHitpoints(Math.min(destination.ship.getMaxHitpoints(),
                    destination.ship.getHitpoints() + destination.ship.getMaxHitpoints() * fragments / capacity));
            if (target.isAlive() && !target.isPhased()) destination.ship.setShipTarget(target);
            if (destination.ship.getShipAI() instanceof ThreatSwarmAI) {
                ((ThreatSwarmAI) destination.ship.getShipAI()).getShared().target = target;
            }
            if (incoming != null) incoming.setForceDespawn(true);
        }

        private ReleasedSwarm spawn(MissileAPI packet, ShipAPI target, Vector2f point, LauncherState launcher) {
            CombatFleetManagerAPI manager = engine.getFleetManager(packet.getOwner());
            if (manager == null) return null;
            // Outside the struck shield/hull, so converted fighters cannot appear inside it.
            Vector2f location = Misc.getUnitVectorAtDegreeAngle(packet.getFacing());
            location.scale(-45f);
            Vector2f.add(location, point, location);
            boolean suppressed = manager.isSuppressDeploymentMessages();
            ShipAPI leader;
            manager.setSuppressDeploymentMessages(true);
            try {
                leader = manager.spawnShipOrWing(SwarmLauncherEffect.ATTACK_SWARM_WING,
                        location, packet.getFacing(), 0f, null);
            } finally {
                manager.setSuppressDeploymentMessages(suppressed);
            }
            if (leader == null) return null;
            FighterWingAPI wing = leader.getWing();
            if (wing != null) wing.setSourceShip(null);
            List<ShipAPI> members = wing == null ? java.util.Collections.singletonList(leader)
                    : new ArrayList<>(wing.getWingMembers());
            ReleasedSwarm result = null;
            for (ShipAPI member : members) {
                FighterWingAPI formation = launcher.formation;
                int groupSize = SwarmLauncherEffect.WING_SIZE.get(SwarmLauncherEffect.ATTACK_SWARM_WING);
                if (formation != null && formation != wing
                        && !formation.getWingMembers().isEmpty()
                        && formation.getWingMembers().size() < groupSize
                        && formation.getWingMembers().stream().anyMatch(s -> s.isAlive() && engine.isEntityInPlay(s))
                        && Misc.getDistance(formation.getWingMembers().get(0).getLocation(), location) <= 300f) {
                    // Match native SwarmLauncherEffect grouping, so every release is
                    // not a separate leader with the native 500-unit leader repulsion.
                    if (wing != null) {
                        wing.removeMember(member);
                        manager.removeDeployed(wing, false);
                    }
                    member.setWing(formation);
                    formation.addMember(member);
                } else if (wing != null) {
                    launcher.formation = wing;
                }
                member.setOwner(packet.getOwner());
                member.setDoNotRender(true);
                member.setExplosionScale(0f);
                member.setHulkChanceOverride(0f);
                member.setImpactVolumeMult(SwarmLauncherEffect.IMPACT_VOLUME_MULT);
                member.getArmorGrid().clearComponentMap();
                // Native fleet deployment pre-advances fighters for two seconds.
                // Undo that simulation before attaching the live incoming fragments.
                member.getLocation().set(location);
                member.setFacing(packet.getFacing());
                member.setAngularVelocity(0f);
                Vector2f arrival = new Vector2f(packet.getVelocity());
                float speed = arrival.length();
                float carrySpeed = Math.min(speed, member.getMaxSpeedWithoutBoost());
                if (speed > 0f) arrival.scale(carrySpeed / speed);
                member.getVelocity().set(arrival);
                RoilingSwarmEffect cloud = FragmentSwarmHullmod.createSwarmFor(member);
                // Only arriving packets provide fragments. No free initial fill, regrowth,
                // or exchanges with unrelated Threat ships or ordinary attack swarms.
                cloud.getParams().withInitialMembers = false;
                cloud.getParams().withRespawn = false;
                RoilingSwarmEffect.getExchangeMap().remove(cloud.getParams().memberExchangeClass, cloud);
                cloud.getParams().memberExchangeClass = null;
                for (SwarmMember fragment : new ArrayList<>(cloud.getMembers())) cloud.removeMember(fragment);
                member.setHitpoints(0.001f);
                member.resetDefaultAI(); // Discard the pre-advanced retreat/flocking decision.
                ReleasedSwarm released = new ReleasedSwarm(member, launcher, cloud);
                swarms.add(released);
                launcher.active++;
                if (result == null) result = released;
            }
            return result;
        }

        @Override
        public void advance(float amount, List<com.fs.starfarer.api.input.InputEventAPI> events) {
            if (engine.isPaused() || amount <= 0f) return;
            retireExpired(amount);
            if (swarms.isEmpty()) {
                engine.getCustomData().remove(CONTROLLER_KEY, this);
                engine.removePlugin(this);
            }
        }

        private void retireExpired(float amount) {
            for (Iterator<ReleasedSwarm> it = swarms.iterator(); it.hasNext();) {
                ReleasedSwarm swarm = it.next();
                swarm.remaining -= amount;
                if (swarm.remaining <= 0f || !swarm.ship.isAlive() || !engine.isEntityInPlay(swarm.ship)) {
                    swarm.cloud.setForceDespawn(true);
                    if (engine.isEntityInPlay(swarm.ship)) {
                        CombatFleetManagerAPI manager = engine.getFleetManager(swarm.ship.getOwner());
                        FighterWingAPI wing = swarm.ship.getWing();
                        if (manager != null && wing != null && wing.getWingMembers().size() <= 1) {
                            manager.removeDeployed(wing, true);
                        } else {
                            if (wing != null) wing.removeMember(swarm.ship);
                            engine.removeEntity(swarm.ship);
                        }
                    }
                    swarm.launcher.active--;
                    it.remove();
                }
            }
        }
    }
}
