package chiefnavigator.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect;
import com.fs.starfarer.api.impl.combat.threat.FragmentSwarmHullmod;
import com.fs.starfarer.api.impl.combat.threat.SwarmLauncherEffect;
import com.fs.starfarer.api.impl.combat.threat.ThreatHullmod;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

/** Complete Threat Hull replacement for materially depleted, ravenous hulls. */
public final class StarvingThreatHullmod extends ThreatHullmod {
    public static final String HULLMOD_ID = "chief_navigator_starving_threat";
    public static final String HIVE_HULL_ID =
            "chief_navigator_starving_hive_unit";
    public static final String HIVE_PERSONALITY = Personalities.TIMID;

    private static final String ATTACK_SWARMS_RELEASED_KEY =
            "chief_navigator_starving_attack_swarms_released";
    private static final float MANEUVERABILITY_MULT = 5f;
    private static final float HULL_MULT = 0.67f;
    private static final float ARMOR_BONUS_PERCENT = 15f;
    private static final float SENSOR_PROFILE_BONUS_PERCENT = 200f;
    private static final float RELEASE_SPEED = 150f;
    private static final String FABRICATOR_HULL_ID =
            "chief_navigator_starving_fabricator_unit";
    private static final float FRIGATE_REINCARNATION_CHANCE = 0.10f;
    private static final float DESTROYER_REINCARNATION_CHANCE = 0.20f;
    private static final float CRUISER_REINCARNATION_CHANCE = 0.30f;
    private static final float FABRICATOR_REINCARNATION_CHANCE = 0.50f;

    @Override
    public void applyEffectsBeforeShipCreation(
            ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        if (stats == null) {
            return;
        }

        // Retain the stock Threat stat package, then replace its zero-profile
        // stealth rule with the Starving Threat's conspicuous emissions.
        super.applyEffectsBeforeShipCreation(hullSize, stats, id);
        // Stock ThreatHullmod recognizes the Hive by its exact vanilla hull
        // ID, not its role tag. Restore that native fragment economy for our
        // separately authored Hive without changing other Starving roles.
        if (stats.getVariant() != null
                && stats.getVariant().getHullSpec() != null
                && HIVE_HULL_ID.equals(
                        stats.getVariant().getHullSpec().getHullId())) {
            stats.getDynamic().getStat(Stats.FRAGMENT_SWARM_RESPAWN_RATE_MULT)
                    .modifyMult(id, HIVE_UNIT_REGEN_RATE_MULT);
            stats.getDynamic().getMod(Stats.FRAGMENT_SWARM_SIZE_MOD)
                    .modifyMult(id, HIVE_UNIT_SWARM_SIZE_MULT);
        }
        applyRavenousSensorProfile(stats.getSensorProfile(), id);
        stats.getAcceleration().modifyMult(id, MANEUVERABILITY_MULT);
        stats.getDeceleration().modifyMult(id, MANEUVERABILITY_MULT);
        stats.getMaxTurnRate().modifyMult(id, MANEUVERABILITY_MULT);
        stats.getTurnAcceleration().modifyMult(id, MANEUVERABILITY_MULT);
        stats.getHullBonus().modifyMult(id, HULL_MULT);
        stats.getArmorBonus().modifyPercent(id, ARMOR_BONUS_PERCENT);
        stats.getSuppliesToRecover().modifyMult(id, 0f);
    }

    static void applyRavenousSensorProfile(
            com.fs.starfarer.api.combat.MutableStat sensorProfile,
            String id) {
        sensorProfile.unmodify(id);
        sensorProfile.modifyPercent(id, SENSOR_PROFILE_BONUS_PERCENT);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship != null) {
            // The inherited Threat stat package is retained, but this class's
            // death tick replaces stock reclamation with attack swarms.
            ship.addTag(ThreatHullmod.SHIP_BEING_RECLAIMED);
            enforceHivePersonality(ship);
        }
    }

    @Override
    public void applyEffectsAfterShipAddedToCombatEngine(
            ShipAPI ship, String id) {
        // Preserve the stock two-sided Threat strategy plugin installed by
        // ThreatHullmod, then repair a just-fabricated Hive whose member or
        // captain did not exist during the earlier creation callback.
        super.applyEffectsAfterShipAddedToCombatEngine(ship, id);
        if (isStarvingHive(ship)) {
            enforceHivePersonality(ship);
            // The default AI may have been constructed before the hullmod's
            // after-creation callback changed its member override.
            ship.resetDefaultAI();
        }
    }

    /** Applies the authored Hive doctrine to new and serialized fleet members. */
    public static boolean enforceHivePersonality(FleetMemberAPI member) {
        if (member == null || !HIVE_HULL_ID.equals(member.getHullId())) {
            return false;
        }
        boolean changed = !HIVE_PERSONALITY.equals(
                member.getPersonalityOverride());
        member.setPersonalityOverride(HIVE_PERSONALITY);
        PersonAPI captain = member.getCaptain();
        if (!hasHivePersonality(captain)) {
            captain.setPersonality(HIVE_PERSONALITY);
            changed = true;
        }
        return changed;
    }

    /** Covers combat-only Hives produced by Construction Swarm. */
    public static boolean enforceHivePersonality(ShipAPI ship) {
        if (!isStarvingHive(ship)) {
            return false;
        }
        boolean changed = enforceHivePersonality(ship.getFleetMember());
        PersonAPI captain = ship.getCaptain();
        if (!hasHivePersonality(captain)) {
            captain.setPersonality(HIVE_PERSONALITY);
            changed = true;
        }
        return changed;
    }

    private static boolean isStarvingHive(ShipAPI ship) {
        return ship != null && ship.getHullSpec() != null
                && HIVE_HULL_ID.equals(ship.getHullSpec().getHullId());
    }

    private static boolean hasHivePersonality(PersonAPI person) {
        return person == null
                || person.getPersonalityAPI() != null
                && HIVE_PERSONALITY.equals(
                        person.getPersonalityAPI().getId());
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null || amount <= 0f) {
            return;
        }

        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || !engine.isEntityInPlay(ship)) {
            return;
        }

        // Remember the side while one of its real hulls is still alive. The
        // controller survives that hull's death and clears detached Attack
        // Swarms only if they become the side's sole deployed combatants.
        StarvingAttackSwarmCleanup.ensureInstalled(
                engine, ship.getOriginalOwner());

        if (!ship.isHulk()
                || ship.getCustomData().containsKey(ATTACK_SWARMS_RELEASED_KEY)) {
            return;
        }

        ship.setCustomData(ATTACK_SWARMS_RELEASED_KEY, Boolean.TRUE);
        RoilingSwarmEffect fragments = RoilingSwarmEffect.getSwarmFor(ship);
        if (fragments != null) {
            fragments.setForceDespawn(true);
        }
        releaseAttackSwarms(engine, ship, getAttackSwarmCount(ship.getHullSize()));
        float reincarnationChance = getGautamaReincarnationChance(ship);
        GautamaReincarnation.attemptReincarnation(
                engine, ship, reincarnationChance);
    }

    static float getGautamaReincarnationChance(ShipAPI ship) {
        if (ship == null) return 0f;
        String hullId = ship.getHullSpec() == null
                ? null : ship.getHullSpec().getHullId();
        return getGautamaReincarnationChance(ship.getHullSize(), hullId);
    }

    static float getGautamaReincarnationChance(
            ShipAPI.HullSize hullSize, String hullId) {
        // The Fabricator is the sole capital in the authored Starving Threat
        // roster. Keep its explicit chance distinct and leave unrecognized
        // capital/fighter hulls at zero rather than inventing another tier.
        if (FABRICATOR_HULL_ID.equals(hullId)) {
            return FABRICATOR_REINCARNATION_CHANCE;
        }
        if (hullSize == ShipAPI.HullSize.CRUISER) {
            return CRUISER_REINCARNATION_CHANCE;
        }
        if (hullSize == ShipAPI.HullSize.DESTROYER) {
            return DESTROYER_REINCARNATION_CHANCE;
        }
        if (hullSize == ShipAPI.HullSize.FRIGATE) {
            return FRIGATE_REINCARNATION_CHANCE;
        }
        return 0f;
    }

    private static int getAttackSwarmCount(ShipAPI.HullSize hullSize) {
        if (hullSize == ShipAPI.HullSize.CAPITAL_SHIP) {
            return 5;
        }
        if (hullSize == ShipAPI.HullSize.CRUISER) {
            return 3;
        }
        if (hullSize == ShipAPI.HullSize.DESTROYER) {
            return 2;
        }
        return 1;
    }

    private static void releaseAttackSwarms(
            CombatEngineAPI engine, ShipAPI source, int count) {
        // Hulks change owner to 100 (neutral). Death-spawned units must retain
        // the combat side the ship belonged to while it was alive.
        int owner = source.getOriginalOwner();
        if (owner != 0 && owner != 1) return;
        CombatFleetManagerAPI manager = engine.getFleetManager(owner);
        if (manager == null || count <= 0) {
            return;
        }

        boolean suppressed = manager.isSuppressDeploymentMessages();
        manager.setSuppressDeploymentMessages(true);
        try {
            for (int i = 0; i < count; i++) {
                float angle = source.getFacing() + (360f * i / count);
                Vector2f direction = Misc.getUnitVectorAtDegreeAngle(angle);
                Vector2f location = new Vector2f(direction);
                location.scale(Math.max(30f, source.getCollisionRadius() * 0.35f));
                Vector2f.add(location, source.getLocation(), location);

                ShipAPI leader = manager.spawnShipOrWing(
                        SwarmLauncherEffect.ATTACK_SWARM_WING,
                        location,
                        angle,
                        0f,
                        null);
                if (leader == null || leader.getWing() == null) {
                    continue;
                }

                FighterWingAPI wing = leader.getWing();
                // These are independent combat wings, not returning carrier
                // fighters. Do not attach them to the now-neutral wreck or
                // remove their active wing from fleet-manager tracking.
                wing.setSourceShip(null);
                for (ShipAPI member : wing.getWingMembers()) {
                    initializeAttackSwarm(member, owner);
                    FragmentSwarmHullmod.createSwarmFor(member);
                    Vector2f launchVelocity = new Vector2f(direction);
                    launchVelocity.scale(RELEASE_SPEED);
                    Vector2f.add(launchVelocity, source.getVelocity(), launchVelocity);
                    member.getVelocity().set(launchVelocity);
                }
            }
        } finally {
            manager.setSuppressDeploymentMessages(suppressed);
        }

        Global.getSoundPlayer().playSound(
                "threat_swarm_launched",
                1f,
                1f,
                source.getLocation(),
                source.getVelocity());
    }

    static void initializeAttackSwarm(ShipAPI member, int owner) {
        member.setOwner(owner);
        // Match SwarmLauncherEffect: the underlying ship is only a carrier
        // for the rendered fragment cloud, never a visible solid drone.
        member.setDoNotRender(true);
        member.setExplosionScale(0f);
        member.setHulkChanceOverride(0f);
        member.setImpactVolumeMult(SwarmLauncherEffect.IMPACT_VOLUME_MULT);
        member.getArmorGrid().clearComponentMap();
    }
}
