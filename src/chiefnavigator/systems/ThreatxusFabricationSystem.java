package chiefnavigator.systems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.impl.combat.threat.*;
import com.fs.starfarer.api.util.Misc;
import chiefnavigator.hullmods.ThreaxusAwakening;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.lwjgl.util.vector.Vector2f;

/** Fabricates Exodus assault units and stock Fabricators using native construction. */
public final class ThreatxusFabricationSystem extends BaseShipSystemScript {
    public static final String SYSTEM_ID = "chief_navigator_threatxus_fabrication";
    public static final String ASSAULT_HULL_ID = "chief_navigator_exodus_assault";
    public static final String ASSAULT_VARIANT = ASSAULT_HULL_ID + "_Standard";
    public static final String FABRICATOR_VARIANT = "fabricator_unit_Type450";
    public static final int MAX_UNITS = 3;
    public static final int MAX_FABRICATORS = 3;
    public static final float CONSTRUCTION_TIME = 20f;
    private static final String KEY = SYSTEM_ID + "_units";

    private static final class Ledger {
        final List<ShipAPI> units = new ArrayList<ShipAPI>();
        final List<ShipAPI> fabricators = new ArrayList<ShipAPI>();
        boolean fired;
        int launched;
    }

    private static Ledger ledger(ShipAPI ship) {
        Ledger value = (Ledger) ship.getCustomData().get(KEY);
        if (value == null) { value = new Ledger(); ship.setCustomData(KEY, value); }
        return value;
    }

    public static int activeUnits(ShipAPI ship, CombatEngineAPI engine) {
        if (ship == null || engine == null) return 0;
        Ledger value = ledger(ship);
        prune(value.units, ship, engine);
        return value.units.size();
    }

    public static int activeFabricators(ShipAPI ship, CombatEngineAPI engine) {
        if (ship == null || engine == null) return 0;
        Ledger value = ledger(ship);
        prune(value.fabricators, ship, engine);
        return value.fabricators.size();
    }

    private static void prune(List<ShipAPI> units, ShipAPI ship, CombatEngineAPI engine) {
        for (Iterator<ShipAPI> it = units.iterator(); it.hasNext();) {
            ShipAPI unit = it.next();
            if (!unit.isAlive() || unit.isHulk() || !engine.isEntityInPlay(unit)
                    || unit.getOwner() != ship.getOwner()) it.remove();
        }
    }

    public static String nextVariant(ShipAPI ship, CombatEngineAPI engine) {
        if (ship == null || engine == null) return null;
        int assaults = activeUnits(ship, engine);
        int fabricators = activeFabricators(ship, engine);
        if (assaults == 0) return ASSAULT_VARIANT;
        if (fabricators < MAX_FABRICATORS) return FABRICATOR_VARIANT;
        return assaults < MAX_UNITS ? ASSAULT_VARIANT : null;
    }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        CombatEngineAPI engine = Global.getCombatEngine();
        return ship != null && ship.getHullSpec() != null
                && ("chief_navigator_threatxus".equals(ship.getHullSpec().getHullId())
                    || "chief_navigator_threatxus".equals(ship.getHullSpec().getBaseHullId())
                    || "chief_navigator_threatxus".equals(ship.getHullSpec().getDParentHullId()))
                && engine != null && engine.isEntityInPlay(ship) && ExodusPlagueSystem.canOperate(ship)
                && !ThreaxusAwakening.isDormant(ship)
                && nextVariant(ship, engine) != null;
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused() || engine.getElapsedInLastFrame() <= 0f
                || stats == null || !(stats.getEntity() instanceof ShipAPI)) return;
        ShipAPI ship = (ShipAPI) stats.getEntity();
        Ledger value = ledger(ship);
        if (state != State.ACTIVE) {
            value.fired = false;
            // Run-while-idle makes fabrication periodic for player and NPC alike.
            if (state == State.IDLE && isUsable(ship.getSystem(), ship)
                    && ship.getSystem() != null && ship.getSystem().canBeActivated()) ship.useSystem();
            return;
        }
        if (value.fired || !isUsable(ship.getSystem(), ship)) return;
        value.fired = true;
        fabricate(ship, value, engine, nextVariant(ship, engine));
    }

    private void fabricate(ShipAPI parent, Ledger value, CombatEngineAPI engine, String variant) {
        boolean fabricator = FABRICATOR_VARIANT.equals(variant);
        CombatFleetManagerAPI manager = engine.getFleetManager(parent.getOwner());
        boolean suppressed = manager.isSuppressDeploymentMessages();
        ShipAPI carrier = null;
        ShipAPI unit = null;
        try {
            manager.setSuppressDeploymentMessages(true);
            Vector2f loc = Misc.getUnitVectorAtDegreeAngle(parent.getFacing() + (value.launched % 2 == 0 ? 90f : -90f));
            // Native Fabricator radius310; Assault radius130. Leave80 clearance.
            loc.scale(parent.getCollisionRadius() + (fabricator ? 390f : 210f));
            Vector2f.add(parent.getLocation(), loc, loc);
            carrier = manager.spawnShipOrWing(SwarmLauncherEffect.CONSTRUCTION_SWARM_WING, loc, parent.getFacing(), 0f, null);
            // Native fighter deployment pre-advances it. Restore the real build site.
            carrier.getLocation().set(loc);
            carrier.getVelocity().set(parent.getVelocity());
            carrier.setFacing(parent.getFacing());
            carrier.setAngularVelocity(0f);
            carrier.setShipAI(null);
            carrier.setDoNotRender(true);
            carrier.setExplosionScale(0f);
            carrier.setHulkChanceOverride(0f);
            carrier.setImpactVolumeMult(SwarmLauncherEffect.IMPACT_VOLUME_MULT);
            carrier.getArmorGrid().clearComponentMap();
            if (carrier.getWing() != null) carrier.getWing().setSourceShip(null);
            RoilingSwarmEffect cloud = FragmentSwarmHullmod.createSwarmFor(carrier);
            RoilingSwarmEffect.getExchangeMap().remove(cloud.getParams().memberExchangeClass, cloud);
            cloud.getParams().memberExchangeClass = null;
            RoilingSwarmEffect.getFlockingMap().remove(cloud.getParams().flockingClass, cloud);
            cloud.getParams().flockingClass = FragmentSwarmHullmod.CONSTRUCTION_SWARM_FLOCKING_CLASS;
            RoilingSwarmEffect.getFlockingMap().add(cloud.getParams().flockingClass, cloud);
            cloud.getParams().withInitialMembers = false;
            cloud.getParams().withRespawn = false;
            // Stock Type450 maintains160 fragments with Fragment Coordinator.
            cloud.getParams().initialMembers = fabricator ? 160
                    : FragmentSwarmHullmod.getBaseSwarmSize(ShipAPI.HullSize.DESTROYER) * 10;
            cloud.getParams().baseMembersToMaintain = cloud.getParams().initialMembers;
            cloud.getParams().maxOffset *= 4f;
            cloud.getParams().spawnOffsetMult = 1f;
            cloud.getMembers().clear();
            cloud.addMembers(cloud.getParams().initialMembers);
            // Only the disposable construction carrier becomes protected by the
            // native script. The Threatxus parent stays vulnerable and mobile.
            ThreatShipConstructionScript construction = new ThreatShipConstructionScript(variant, carrier, 0f, CONSTRUCTION_TIME);
            unit = construction.getShip();
            unit.getLocation().set(loc);
            unit.getVelocity().set(parent.getVelocity());
            unit.setAlphaMult(0f);
            RoilingSwarmEffect target = RoilingSwarmEffect.getSwarmFor(unit);
            if (target != null) {
                target.getMembers().clear();
                target.getParams().withInitialMembers = false;
                target.getParams().withRespawn = false;
            }
            engine.addPlugin(construction);
            (fabricator ? value.fabricators : value.units).add(unit);
            value.launched++;
        } catch (RuntimeException failure) {
            if (unit != null) engine.removeEntity(unit);
            if (carrier != null) engine.removeEntity(carrier);
            throw failure;
        } finally {
            manager.setSuppressDeploymentMessages(suppressed);
        }
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        return index == 0 && state == State.ACTIVE ? new StatusData("fabricating Threat unit", false) : null;
    }
}
