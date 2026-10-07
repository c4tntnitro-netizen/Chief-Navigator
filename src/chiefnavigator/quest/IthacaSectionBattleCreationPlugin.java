package chiefnavigator.quest;

import chiefnavigator.hullmods.IthacaSiegeStalemateHullmod;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.lwjgl.util.vector.Vector2f;

/**
 * Drifting-Wall-style combat for the three intact FOB Ithaca bastions.
 * Every participating campaign station contributes one complete foundation;
 * the selected bastion remains centered and nearby stations reinforce it.
 */
public final class IthacaSectionBattleCreationPlugin
        extends BattleCreationPluginImpl {
    @Override
    public void initBattle(
            BattleCreationContext context, MissionDefinitionAPI api) {
        super.initBattle(context, api);
        context.objectivesAllowed = false;
        context.enemyDeployAll = true;
        context.aiRetreatAllowed = false;
        context.fightToTheLast = true;
    }

    @Override
    public void afterDefinitionLoad(final CombatEngineAPI engine) {
        super.afterDefinitionLoad(engine);
        engine.addPlugin(new IthacaBastionCombatPlugin(engine));
    }

    private static final class IthacaBastionCombatPlugin
            extends BaseEveryFrameCombatPlugin {
        private static final String STAT_ID =
                "chief_navigator_ithaca_bastion_anchor";
        private static final float FOUNDATION_Y = 3000f;
        private static final float FOUNDATION_FACING = 270f;
        private static final float REINFORCEMENT_SPACING = 6000f;
        private static final float REPAIR_DELAY_SECONDS = 6f;
        private static final float REPAIR_FRACTION_PER_SECOND = 0.005f;

        private final CombatEngineAPI engine;
        private final Map<IthacaSectionEncounter.Section, ShipAPI> foundations =
                new EnumMap<IthacaSectionEncounter.Section, ShipAPI>(
                        IthacaSectionEncounter.Section.class);
        private final Map<IthacaSectionEncounter.Section, ShipAPI> reactors =
                new EnumMap<IthacaSectionEncounter.Section, ShipAPI>(
                        IthacaSectionEncounter.Section.class);
        private final Map<IthacaSectionEncounter.Section, Float>
                lastReactorHitpoints =
                new EnumMap<IthacaSectionEncounter.Section, Float>(
                        IthacaSectionEncounter.Section.class);
        private final Map<IthacaSectionEncounter.Section, Float>
                repairDelayRemaining =
                new EnumMap<IthacaSectionEncounter.Section, Float>(
                        IthacaSectionEncounter.Section.class);
        private final Set<ShipAPI> configured =
                java.util.Collections.newSetFromMap(
                        new java.util.IdentityHashMap<ShipAPI, Boolean>());
        private final Set<IthacaSectionEncounter.Section> reactorSeen =
                EnumSet.noneOf(IthacaSectionEncounter.Section.class);
        private final Set<IthacaSectionEncounter.Section> disabled =
                EnumSet.noneOf(IthacaSectionEncounter.Section.class);
        private final Set<IthacaSectionEncounter.Section> guardsDeployed =
                EnumSet.noneOf(IthacaSectionEncounter.Section.class);

        private IthacaSectionEncounter.Section selectedSection;
        private boolean finished;

        private IthacaBastionCombatPlugin(CombatEngineAPI engine) {
            this.engine = engine;
            selectedSection = IthacaSectionEncounter.getActiveSection();
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            discoverFoundations();
            if (foundations.isEmpty()) return;
            if (selectedSection == null
                    || !foundations.containsKey(selectedSection)) {
                selectedSection = foundations.keySet().iterator().next();
            }

            Map<IthacaSectionEncounter.Section, Vector2f> anchors =
                    buildAnchors();
            for (Map.Entry<IthacaSectionEncounter.Section, ShipAPI> entry :
                    foundations.entrySet()) {
                Vector2f anchor = anchors.get(entry.getKey());
                if (anchor != null) {
                    anchorFoundation(entry.getValue(), anchor);
                    deployDroneGuards(entry.getKey(), anchor);
                }
                findReactor(entry.getKey(), entry.getValue());
            }

            for (IthacaSectionEncounter.Section section :
                    IthacaSectionEncounter.Section.values()) {
                ShipAPI reactor = reactors.get(section);
                if (reactor != null && reactor.isAlive() && !reactor.isHulk()) {
                    pulseReactor(reactor);
                    repairReactor(section, reactor, amount);
                } else if (reactorSeen.contains(section)
                        && !disabled.contains(section)) {
                    disableBastion(section);
                }
            }

            ShipAPI objective = reactors.get(selectedSection);
            if (objective != null
                    && objective.isAlive()
                    && !objective.isHulk()) {
                int integrity = Math.round(100f
                        * objective.getHitpoints()
                        / objective.getMaxHitpoints());
                engine.maintainStatusForPlayerShip(
                        this,
                        "graphics/icons/hullsys/fortress_shield.png",
                        "FOB ITHACA - "
                                + selectedSection.displayName.toUpperCase(),
                        "Rear reactor integrity: " + integrity + "%",
                        false);
            }
        }

        private void discoverFoundations() {
            for (ShipAPI ship : engine.getShips()) {
                if (configured.contains(ship) || !ship.isAlive()) continue;
                if (!IthacaSectionEncounter.WALL_HULL_ID.equals(
                        ship.getHullSpec().getHullId())) continue;
                IthacaSectionEncounter.Section section =
                        IthacaSectionEncounter.getSectionForMember(
                                ship.getFleetMember());
                if (section == null) continue;
                configured.add(ship);
                foundations.put(section, ship);
                immobilize(ship);
                restoreStructure(ship);
                restoreLiveWeapons(ship);
            }
        }

        private Map<IthacaSectionEncounter.Section, Vector2f> buildAnchors() {
            List<IthacaSectionEncounter.Section> present =
                    new ArrayList<IthacaSectionEncounter.Section>();
            if (selectedSection != null
                    && foundations.containsKey(selectedSection)) {
                present.add(selectedSection);
            }
            for (IthacaSectionEncounter.Section section :
                    IthacaSectionEncounter.Section.values()) {
                if (section != selectedSection
                        && foundations.containsKey(section)) {
                    present.add(section);
                }
            }

            Map<IthacaSectionEncounter.Section, Vector2f> result =
                    new EnumMap<IthacaSectionEncounter.Section, Vector2f>(
                            IthacaSectionEncounter.Section.class);
            if (present.isEmpty()) return result;
            result.put(present.get(0), new Vector2f(0f, FOUNDATION_Y));
            if (present.size() == 2) {
                result.put(present.get(1),
                        new Vector2f(REINFORCEMENT_SPACING, FOUNDATION_Y));
            } else if (present.size() >= 3) {
                result.put(present.get(1),
                        new Vector2f(-REINFORCEMENT_SPACING, FOUNDATION_Y));
                result.put(present.get(2),
                        new Vector2f(REINFORCEMENT_SPACING, FOUNDATION_Y));
            }
            return result;
        }

        private void anchorFoundation(ShipAPI foundation, Vector2f anchor) {
            if (foundation == null || !foundation.isAlive()) return;
            foundation.getLocation().set(anchor);
            foundation.getVelocity().set(0f, 0f);
            foundation.setFacing(FOUNDATION_FACING);
            foundation.setAngularVelocity(0f);
            // Zero movement statistics and the hard anchor immobilize the
            // Vast Bulk without locking controls, which would also stop AI
            // weapon operation.
            if (!disabled.contains(getSection(foundation))) {
                IthacaFoundationFireControl.ensureActive(foundation);
            }
        }

        private void deployDroneGuards(
                IthacaSectionEncounter.Section section,
                Vector2f foundationAnchor) {
            if (guardsDeployed.contains(section)) return;
            CombatFleetManagerAPI manager = engine.getFleetManager(1);
            String[] variants =
                    IthacaSectionEncounter.getDroneGuardVariants(section);
            float[] offsets = new float[] {
                -1800f, -900f, 0f, 900f, 1800f
            };
            for (int i = 0; i < variants.length; i++) {
                float x = foundationAnchor.x
                        + offsets[i % offsets.length];
                float y = foundationAnchor.y - 2400f
                        - 350f * (i % 2);
                ShipAPI guard = manager.spawnShipOrWing(
                        variants[i],
                        new Vector2f(x, y),
                        FOUNDATION_FACING,
                        0f);
                IthacaSiegeStalemateHullmod.markWallDefender(guard);
            }
            guardsDeployed.add(section);
        }

        private void findReactor(
                IthacaSectionEncounter.Section section,
                ShipAPI foundation) {
            if (reactorSeen.contains(section)) return;
            for (ShipAPI module : foundation.getChildModulesCopy()) {
                if (!DriftingWallEncounter.REACTOR_HULL_ID.equals(
                        module.getHullSpec().getHullId())) continue;
                reactors.put(section, module);
                reactorSeen.add(section);
                lastReactorHitpoints.put(section, module.getHitpoints());
                repairDelayRemaining.put(section, 0f);
                module.setShowModuleJitterUnder(true);
                restoreStructure(module);
                return;
            }
        }

        private void pulseReactor(ShipAPI reactor) {
            float phase = engine.getTotalElapsedTime(false) * 3f;
            float pulse = 0.28f + 0.10f
                    * (0.5f + 0.5f * (float) Math.sin(phase));
            reactor.setJitterUnder(
                    this,
                    new Color(35, 220, 255, 150),
                    pulse,
                    5,
                    0f,
                    18f);
            reactor.setJitter(
                    this,
                    new Color(170, 250, 255, 90),
                    pulse * 0.2f,
                    2,
                    0f,
                    5f);
        }

        private void repairReactor(
                IthacaSectionEncounter.Section section,
                ShipAPI reactor,
                float amount) {
            float hitpoints = reactor.getHitpoints();
            Float lastValue = lastReactorHitpoints.get(section);
            float last = lastValue == null ? hitpoints : lastValue;
            Float delayValue = repairDelayRemaining.get(section);
            float delay = delayValue == null ? 0f : delayValue;
            if (hitpoints < last - 0.5f) {
                delay = REPAIR_DELAY_SECONDS;
            } else if (delay > 0f) {
                delay = Math.max(0f, delay - amount);
            } else if (amount > 0f && hitpoints < reactor.getMaxHitpoints()) {
                float repaired = reactor.getMaxHitpoints()
                        * REPAIR_FRACTION_PER_SECOND * amount;
                reactor.setHitpoints(Math.min(
                        reactor.getMaxHitpoints(), hitpoints + repaired));
            }
            repairDelayRemaining.put(section, delay);
            lastReactorHitpoints.put(section, reactor.getHitpoints());
        }

        private void disableBastion(IthacaSectionEncounter.Section section) {
            disabled.add(section);
            ShipAPI foundation = foundations.get(section);
            if (foundation != null) {
                disableWeapons(foundation);
                foundation.setShipSystemDisabled(true);
                foundation.setDefenseDisabled(true);
                foundation.setControlsLocked(true);
            }
            IthacaSectionEncounter.reportCoreDisabled(section);
            if (section == selectedSection && !finished) {
                finished = true;
                engine.endCombat(1.5f, FleetSide.PLAYER);
            }
        }

        private void disableWeapons(ShipAPI ship) {
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                weapon.stopFiring();
                weapon.setForceDisabled(true);
            }
            for (ShipAPI module : ship.getChildModulesCopy()) {
                for (WeaponAPI weapon : module.getAllWeapons()) {
                    weapon.stopFiring();
                    weapon.setForceDisabled(true);
                }
                module.setShipSystemDisabled(true);
                module.setDefenseDisabled(true);
                module.setControlsLocked(true);
            }
        }

        private void immobilize(ShipAPI ship) {
            ship.getMutableStats().getMaxSpeed().modifyMult(STAT_ID, 0f);
            ship.getMutableStats().getAcceleration().modifyMult(STAT_ID, 0f);
            ship.getMutableStats().getDeceleration().modifyMult(STAT_ID, 0f);
            ship.getMutableStats().getMaxTurnRate().modifyMult(STAT_ID, 0f);
            ship.getMutableStats().getTurnAcceleration().modifyMult(
                    STAT_ID, 0f);
        }

        private void restoreStructure(ShipAPI ship) {
            ship.setHitpoints(ship.getMaxHitpoints());
            ArmorGridAPI armor = ship.getArmorGrid();
            float max = armor.getMaxArmorInCell();
            float[][] grid = armor.getGrid();
            for (int x = 0; x < grid.length; x++) {
                for (int y = 0; y < grid[x].length; y++) {
                    armor.setArmorValue(x, y, max);
                }
            }
            ship.syncWithArmorGridState();
            ship.syncWeaponDecalsWithArmorDamage();
        }

        private void restoreLiveWeapons(ShipAPI ship) {
            IthacaFoundationFireControl.ensureActive(ship);
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                IthacaFoundationFireControl.restoreWeapon(weapon);
            }
            for (ShipAPI module : ship.getChildModulesCopy()) {
                module.setControlsLocked(false);
                module.setShipSystemDisabled(false);
                module.setDefenseDisabled(false);
                for (WeaponAPI weapon : module.getAllWeapons()) {
                    IthacaFoundationFireControl.restoreWeapon(weapon);
                }
            }
        }

        private IthacaSectionEncounter.Section getSection(ShipAPI ship) {
            return ship == null ? null
                    : IthacaSectionEncounter.getSectionForMember(
                            ship.getFleetMember());
        }
    }
}
