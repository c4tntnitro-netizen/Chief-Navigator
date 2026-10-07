package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.intel.deciv.DecivTracker;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** Persistent Waypoint Troy system generation and goal-station helpers. */
public final class TroyArrivalScript {
    public static final String ENTERED = "$chief_navigator_treadmill_entered";
    public static final String SYSTEM_ID = "chief_navigator_treadmill";
    private static final String AUTHORED_SYSTEM_MARKER =
            "$chief_navigator_troy_authored_system_v1";
    public static final String DEPARTURE_STATION_ID = "chief_navigator_troy_anchorage";
    private static final String DEPARTURE_STATION_MARKER =
            "$chief_navigator_troy_departure_station_v1";
    public static final String ACCESS_JUMP_ID = "chief_navigator_troy_access";
    public static final String ACCESS_HYPER_JUMP_ID =
            "chief_navigator_troy_access_hyper";
    private static final String ACCESS_JUMP_MARKER =
            "$chief_navigator_troy_access_endpoint";
    public static final String WORMHOLE_ID = "chief_navigator_troy_wormhole";
    private static final String WORMHOLE_MARKER =
            "$chief_navigator_troy_wormhole_v1";
    public static final String WORMHOLE_OPENED = "$chief_navigator_troy_wormhole_opened";
    private static final String WORMHOLE_CREATED_V1 =
            "$chief_navigator_troy_wormhole_created_v1";
    private static final String DEPARTURE_MARKET_ID = "chief_navigator_troy_anchorage_market";
    public static final String STATION_ID = "chief_navigator_goal_station";
    public static final String DEFENSE_FLEET_ID = "chief_navigator_ithaca_wall";
    public static final String DEFENSE_MARKER = "$chief_navigator_ithaca_defense";
    public static final String EMPLACEMENT_HULLMOD = "chief_navigator_ithaca_range";
    public static final String WALL_HULL_ID = "chief_navigator_ithaca_wall_segment";
    public static final String APRON_HULL_ID = "chief_navigator_ithaca_mounting_apron";
    public static final String TURRET_HULL_ID = "chief_navigator_ithaca_naval_mount";
    public static final String DAMOCLES_HULL_ID = "chief_navigator_ithaca_damocles_mount";
    public static final String METALSTORM_HULL_ID = "chief_navigator_ithaca_metalstorm_mount";
    public static final String STARVING_THREAT_FLEET_ID = "chief_navigator_starving_threat_sample";
    private static final String STARVING_THREAT_ANCHOR_ID = "chief_navigator_starving_threat_anchor";
    private static final String STARVING_THREAT_ANCHOR_MARKER =
            "$chief_navigator_starving_threat_anchor";
    private static final String STARVING_THREAT_SPAWNED =
            "$chief_navigator_starving_threat_scylla_sample_spawned";
    private static final String GAUTAMA_FINAL_REMATCH_ROSTER_V1 =
            "$chief_navigator_gautama_final_rematch_roster_v1";
    private static final String UNGAIKYO_FINAL_REMATCH_ROSTER_V1 =
            "$chief_navigator_ungaikyo_final_rematch_roster_v1";
    private static final String GAUTAMA_FINAL_REMATCH_CIRCUIT_V1 =
            "$chief_navigator_gautama_final_rematch_circuit_v1";
    private static final String UNGAIKYO_LAST_LIGHT_ASSIGNMENT_V1 =
            "$chief_navigator_ungaikyo_last_light_assignment_v1";
    private static final String GAUTAMA_FINAL_REMATCH_CREATED_V1 =
            "$chief_navigator_gautama_final_rematch_created_v1";
    private static final String GAUTAMA_FINAL_REMATCH_INVALID_V1 =
            "$chief_navigator_gautama_final_rematch_invalid_v1";
    private static final String GAUTAMA_CANONICAL_ID_BLOCKED_V1 =
            "$chief_navigator_gautama_canonical_id_blocked_v1";
    private static final String GAUTAMA_ANCHOR_BLOCKED_V1 =
            "$chief_navigator_gautama_anchor_blocked_v1";
    private static final String GAUTAMA_DEFEATED_IN_AVICI =
            "$chief_navigator_gautama_defeated_in_avici";
    /** Ungaikyo and five non-Fabricator escorts; reflections are combat-only. */
    private static final String[] UNGAIKYO_FINAL_REMATCH_VARIANTS = {
        "chief_navigator_ungaikyo_Fabricator",
        "chief_navigator_starving_hive_Type350",
        "chief_navigator_starving_overseer_Type250",
        "chief_navigator_starving_standoff_Type300",
        "chief_navigator_starving_assault_Type200",
        "chief_navigator_starving_skirmish_Type100"
    };
    public static final String CHARYBDIS_FLEET_ID = "chief_navigator_charybdis_test";
    private static final String CHARYBDIS_ANCHOR_ID = "chief_navigator_charybdis_anchor";
    private static final String CHARYBDIS_SPAWNED = "$chief_navigator_charybdis_test_spawned";
    public static final String TASK_FORCE_SPARTAN_FLEET_ID =
            "chief_navigator_task_force_spartan_test";
    public static final String TASK_FORCE_SPARTAN_MARKER =
            "$chief_navigator_task_force_spartan_test";
    private static final String TASK_FORCE_SPARTAN_ANCHOR_ID =
            "chief_navigator_task_force_spartan_anchor";
    public static final String TIME_FREEZE_FLEET_ID = "chief_navigator_time_freeze_test";
    private static final String TIME_FREEZE_ANCHOR_ID = "chief_navigator_time_freeze_anchor";
    private static final String TIME_FREEZE_SPAWNED = "$chief_navigator_time_freeze_test_spawned";
    private static final String TIME_FREEZE_MARKER =
            "$chief_navigator_time_freeze_facet_guard";
    private static final String TIME_FREEZE_CANONICAL_ID_BLOCKED_V1 =
            "$chief_navigator_time_freeze_id_blocked_v1";
    private static final String TIME_FREEZE_RETIRED_V1 =
            "$chief_navigator_time_freeze_retired_v1";
    private static final String LEGACY_TURRET_HULL_ID = "chief_navigator_ithaca_turret";
    private static final String LEGACY_PROTOTYPE_CLEANED =
            "$chief_navigator_troy_legacy_prototype_cleaned_v4";
    private static final String DEPARTURE_IMPORTANT = "chief_navigator_troy_departure";
    private static final String ACCESS_DETECTED = "chief_navigator_troy_access";
    private static final String CANONICAL_TOPOLOGY =
            "$chief_navigator_troy_canonical_topology_v4";
    private static final String THRESHOLD_IMPORTANT = "chief_navigator_far_threshold";
    private static final float DEPARTURE_X = 0f;
    private static final float DEPARTURE_Y = 0f;
    private static final float ACCESS_X = -2000f;
    private static final float ACCESS_Y = 0f;
    private static final float WORMHOLE_X = 2000f;
    private static final float WORMHOLE_Y = 0f;
    private static StarSystemAPI topologyVerifiedFor;

    private TroyArrivalScript() { }

    public static StarSystemAPI ensureWaypointTroyExists() {
        StarSystemAPI existing = findWaypointTroy();
        // The system and its endpoints are serialized campaign state. Do not
        // canonicalize, replace, or reconstruct them during a normal load.
        if (existing != null) return existing;

        StarSystemAPI claim = findWaypointTroyClaim();
        if (claim != null) {
            Global.getLogger(TroyArrivalScript.class).error(
                    "Waypoint Troy system ID is already owned by "
                            + claim.getName()
                            + "; refusing to create or rewrite it");
            return null;
        }

        StarSystemAPI system = Global.getSector().createStarSystem("Waypoint Troy");
        system.setOptionalUniqueId(SYSTEM_ID);
        system.getMemoryWithoutUpdate().set(AUTHORED_SYSTEM_MARKER, true);
        system.getLocation().set(59000f, -45000f);
        system.setBackgroundTextureFilename("graphics/backgrounds/background4.jpg");
        system.setLightColor(new Color(105, 120, 155));
        system.addTag(Tags.THEME_UNSAFE);
        system.initNonStarCenter();
        makePersistentAndVisible(system);
        ensureDepartureStation(system);
        ensureCanonicalTopology(system);
        return system;
    }

    /** Returns the serialized Troy system without creating or repairing it. */
    public static StarSystemAPI findWaypointTroy() {
        StarSystemAPI candidate = findWaypointTroyClaim();
        return isWaypointTroy(candidate) ? candidate : null;
    }

    private static StarSystemAPI findWaypointTroyClaim() {
        if (Global.getSector() == null) return null;
        for (StarSystemAPI candidate : Global.getSector().getStarSystems()) {
            if (SYSTEM_ID.equals(candidate.getOptionalUniqueId())) {
                return candidate;
            }
        }
        return null;
    }

    /** Re-evaluates quest-marker visibility when the Eventide stage changes. */
    public static void refreshObjectiveVisibility() {
        if (Global.getSector() == null
                || Global.getSector().getHyperspace() == null) return;
        StarSystemAPI system = findWaypointTroy();
        if (system == null) return;
        SectorEntityToken hyperAccess = Global.getSector().getHyperspace()
                .getEntityById(ACCESS_HYPER_JUMP_ID);
        if (!(hyperAccess instanceof JumpPointAPI)
                || !isOwnedTroyAccessPair(
                        system, (JumpPointAPI) hyperAccess)) return;
        if (SinniEventideIntel.isBriefed()) {
            Misc.makeImportant(hyperAccess, ACCESS_DETECTED);
        } else {
            Misc.makeUnimportant(hyperAccess, ACCESS_DETECTED);
        }
    }

    /** Persistent expedition checkpoint with free ship and cargo storage. */
    public static SectorEntityToken ensureDepartureStation(StarSystemAPI system) {
        SectorEntityToken station = system.getEntityById(DEPARTURE_STATION_ID);
        if (station != null && !isDepartureStation(station)) {
            Global.getLogger(TroyArrivalScript.class).error(
                    "Troy anchorage ID contains incompatible state; refusing "
                            + "to replace or rewrite it");
            return null;
        }
        if (station == null) {
            station = system.addCustomEntity(
                    DEPARTURE_STATION_ID,
                    "Troy Expedition Anchorage",
                    "station_side00",
                    Factions.NEUTRAL);
            station.getMemoryWithoutUpdate().set(
                    DEPARTURE_STATION_MARKER, true);
            station.setInteractionImage("illustrations", "orbital");
        }

        // Keep the actual checkpoint at the system center so arriving at Troy
        // can never yield a map full of jump contacts with no visible goal.
        station.setName("Troy Expedition Anchorage");
        station.setLocation(DEPARTURE_X, DEPARTURE_Y);
        station.setFreeTransfer(true);
        station.setSensorProfile(1f);
        station.setDiscoverable(false);
        station.getDetectedRangeMod().modifyFlat("chief_navigator_troy_anchorage", 20000f);
        if (SinniEventideIntel.isBriefed()
                && !Global.getSector().getMemoryWithoutUpdate().getBoolean(WORMHOLE_OPENED)) {
            Misc.makeImportant(station, DEPARTURE_IMPORTANT);
        } else {
            Misc.makeUnimportant(station, DEPARTURE_IMPORTANT);
        }

        MarketAPI market = station.getMarket();
        if (market == null) {
            Misc.setAbandonedStationMarket(DEPARTURE_MARKET_ID, station);
            market = station.getMarket();
        }

        if (market.isInEconomy()) {
            Global.getSector().getEconomy().removeMarket(market);
        }

        market.setName(station.getName());
        market.setSize(0);
        market.setFactionId(Factions.NEUTRAL);
        market.setHidden(true);
        market.setSurveyLevel(MarketAPI.SurveyLevel.FULL);
        market.setPrimaryEntity(station);
        market.setPlanetConditionMarketOnly(false);
        market.setInvalidMissionTarget(true);
        market.setForceNoConvertOnSave(true);
        market.getMemoryWithoutUpdate().set("$noBar", true);
        market.getMemoryWithoutUpdate().set(MemFlags.HIDDEN_BASE_MEM_FLAG, true);
        market.getMemoryWithoutUpdate().set(DecivTracker.NO_DECIV_KEY, true);
        if (market.hasCondition(Conditions.DECIVILIZED)) {
            market.removeCondition(Conditions.DECIVILIZED);
        }
        if (!market.hasCondition(Conditions.ABANDONED_STATION)) {
            market.addCondition(Conditions.ABANDONED_STATION);
        }
        station.getMemoryWithoutUpdate().set("$abandonedStation", true);
        station.setMarket(market);
        if (!market.hasSubmarket(Submarkets.SUBMARKET_STORAGE)) {
            market.addSubmarket(Submarkets.SUBMARKET_STORAGE);
        }

        if (market.getSubmarket(Submarkets.SUBMARKET_STORAGE) != null
                && market.getSubmarket(Submarkets.SUBMARKET_STORAGE).getPlugin()
                instanceof StoragePlugin) {
            ((StoragePlugin) market.getSubmarket(Submarkets.SUBMARKET_STORAGE).getPlugin())
                    .setPlayerPaidToUnlock(true);
        }
        return station;
    }

    /** Returns Troy's serialized anchorage without manufacturing a new one. */
    public static SectorEntityToken findDepartureStation(
            StarSystemAPI system) {
        SectorEntityToken station = system == null ? null
                : system.getEntityById(DEPARTURE_STATION_ID);
        return isDepartureStation(station) ? station : null;
    }

    /** Creates the visible waypoint opened by Sinni after the 50-DP check. */
    public static JumpPointAPI ensureOdysseyWormhole(StarSystemAPI system) {
        if (system == null) return null;
        SectorEntityToken existing = system.getEntityById(WORMHOLE_ID);
        if (existing != null && !isOdysseyWormhole(existing)) {
            Global.getLogger(TroyArrivalScript.class).error(
                    "Troy Terminus ID contains incompatible serialized "
                            + "state; refusing replacement");
            return null;
        }
        JumpPointAPI wormhole;
        if (existing instanceof JumpPointAPI) {
            // The serialized endpoint is authoritative once created. The
            // explicit story action may reveal it, but never relocates or
            // rewrites an existing jump point.
            return (JumpPointAPI) existing;
        } else {
            if (system.getMemoryWithoutUpdate().getBoolean(
                    WORMHOLE_CREATED_V1)) return null;
            wormhole = Global.getFactory().createJumpPoint(WORMHOLE_ID, "Troy Terminus");
            wormhole.getMemoryWithoutUpdate().set(WORMHOLE_MARKER, true);
            wormhole.setStandardWormholeToHyperspaceVisual();
            system.addEntity(wormhole);
        }
        system.getMemoryWithoutUpdate().set(WORMHOLE_CREATED_V1, true);
        wormhole.setAutoCreateEntranceFromHyperspace(false);
        wormhole.setLocation(WORMHOLE_X, WORMHOLE_Y);
        wormhole.setSensorProfile(1f);
        wormhole.setDiscoverable(false);
        wormhole.getDetectedRangeMod().modifyFlat("chief_navigator_troy_wormhole", 12000f);
        wormhole.setInteractionImage("illustrations", "jump_point_normal");
        Misc.makeImportant(wormhole, "chief_navigator_troy_wormhole");
        return wormhole;
    }

    /** Returns the unlocked terminus without recreating a missing endpoint. */
    public static JumpPointAPI findOdysseyWormhole(StarSystemAPI system) {
        if (system == null) return null;
        SectorEntityToken entity = system.getEntityById(WORMHOLE_ID);
        return isOdysseyWormhole(entity) ? (JumpPointAPI) entity : null;
    }

    /** True only for Chief Navigator's authored Troy anchorage. */
    public static boolean isDepartureStation(SectorEntityToken entity) {
        if (entity == null || !DEPARTURE_STATION_ID.equals(entity.getId())
                || !"station_side00".equals(entity.getCustomEntityType())
                || entity.getMemoryWithoutUpdate() == null
                || !entity.getMemoryWithoutUpdate().getBoolean(
                        DEPARTURE_STATION_MARKER)
                || !(entity.getContainingLocation() instanceof StarSystemAPI)) {
            return false;
        }
        return isWaypointTroy((StarSystemAPI) entity.getContainingLocation());
    }

    /** True only for Chief Navigator's authored Troy Terminus. */
    public static boolean isOdysseyWormhole(SectorEntityToken entity) {
        if (!(entity instanceof JumpPointAPI)
                || !WORMHOLE_ID.equals(entity.getId())
                || !(entity.getContainingLocation() instanceof StarSystemAPI)) {
            return false;
        }
        StarSystemAPI system = (StarSystemAPI) entity.getContainingLocation();
        return isWaypointTroy(system)
                && (entity.getMemoryWithoutUpdate().getBoolean(WORMHOLE_MARKER)
                        || system.getMemoryWithoutUpdate().getBoolean(
                                WORMHOLE_CREATED_V1));
    }

    private static boolean isWaypointTroy(StarSystemAPI system) {
        return system != null && SYSTEM_ID.equals(system.getOptionalUniqueId())
                && system.getMemoryWithoutUpdate() != null
                && (system.getMemoryWithoutUpdate().getBoolean(
                        AUTHORED_SYSTEM_MARKER)
                        || system.getMemoryWithoutUpdate().getBoolean(
                                CANONICAL_TOPOLOGY));
    }

    private static void makePersistentAndVisible(StarSystemAPI system) {
        system.getLocation().set(59000f, -45000f);
        system.removeTag(Tags.THEME_HIDDEN);
        system.removeTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER);
        system.setDoNotShowIntelFromThisLocationOnMap(false);
    }

    /** Creates Troy's two-sided access exactly once during system creation. */
    private static void ensureCanonicalTopology(StarSystemAPI system) {
        if (topologyVerifiedFor == system) return;

        SectorEntityToken localClaim = system.getEntityById(ACCESS_JUMP_ID);
        LocationAPI hyperspace = Global.getSector().getHyperspace();
        SectorEntityToken hyperClaim = hyperspace.getEntityById(
                ACCESS_HYPER_JUMP_ID);
        if (localClaim != null && (!(localClaim instanceof JumpPointAPI)
                || localClaim.getMemoryWithoutUpdate() == null
                || !localClaim.getMemoryWithoutUpdate().getBoolean(
                        ACCESS_JUMP_MARKER))) {
            Global.getLogger(TroyArrivalScript.class).error(
                    "Waypoint Troy access ID is already owned by "
                            + localClaim.getClass().getName()
                            + "; refusing to replace it");
            return;
        }
        if (hyperClaim != null && (!(hyperClaim instanceof JumpPointAPI)
                || hyperClaim.getMemoryWithoutUpdate() == null
                || !hyperClaim.getMemoryWithoutUpdate().getBoolean(
                        ACCESS_JUMP_MARKER))) {
            Global.getLogger(TroyArrivalScript.class).error(
                    "Waypoint Troy hyperspace ID is already owned by "
                            + hyperClaim.getClass().getName()
                            + "; refusing to replace it");
            return;
        }
        JumpPointAPI access = localClaim instanceof JumpPointAPI
                ? (JumpPointAPI) localClaim : null;
        if (access == null) {
            access = Global.getFactory().createJumpPoint(
                    ACCESS_JUMP_ID, "Troy Access Point");
            access.setStandardWormholeToHyperspaceVisual();
            access.getMemoryWithoutUpdate().set(ACCESS_JUMP_MARKER, true);
            system.addEntity(access);
        }
        access.setAutoCreateEntranceFromHyperspace(false);
        access.setLocation(ACCESS_X, ACCESS_Y);
        access.setSensorProfile(1f);
        access.setDiscoverable(false);
        access.getDetectedRangeMod().modifyFlat(ACCESS_DETECTED, 12000f);
        access.setInteractionImage("illustrations", "jump_point_normal");

        JumpPointAPI accessInHyperspace = hyperClaim instanceof JumpPointAPI
                ? (JumpPointAPI) hyperClaim : null;
        if (accessInHyperspace == null) {
            accessInHyperspace = Global.getFactory().createJumpPoint(
                    ACCESS_HYPER_JUMP_ID, "Waypoint Troy");
            accessInHyperspace.setStandardWormholeToStarfieldVisual();
            accessInHyperspace.setAutoCreateEntranceFromHyperspace(false);
            accessInHyperspace.getMemoryWithoutUpdate().set(
                    ACCESS_JUMP_MARKER, true);
            hyperspace.addEntity(accessInHyperspace);
        }
        accessInHyperspace.setName("Waypoint Troy");
        accessInHyperspace.setLocation(
                system.getLocation().x, system.getLocation().y);
        accessInHyperspace.setSensorProfile(1f);
        accessInHyperspace.setDiscoverable(false);
        accessInHyperspace.getDetectedRangeMod().modifyFlat(
                ACCESS_DETECTED, 12000f);

        // Wire only the pair we own. Never scan or prune unrelated jump
        // points, and never use transient autogenerated lists as authority.
        if (!hasDestination(access, accessInHyperspace)) {
            access.addDestination(new JumpPointAPI.JumpDestination(
                    accessInHyperspace, "Hyperspace"));
        }
        if (!hasDestination(accessInHyperspace, access)) {
            accessInHyperspace.addDestination(new JumpPointAPI.JumpDestination(
                    access, "the Troy access point"));
        }
        if (SinniEventideIntel.isBriefed()) {
            Misc.makeImportant(accessInHyperspace, ACCESS_DETECTED);
        } else {
            Misc.makeUnimportant(accessInHyperspace, ACCESS_DETECTED);
        }

        system.getMemoryWithoutUpdate().set(CANONICAL_TOPOLOGY, true);
        topologyVerifiedFor = system;
    }

    private static boolean hasDestination(
            JumpPointAPI source, SectorEntityToken target) {
        for (JumpPointAPI.JumpDestination destination :
                source.getDestinations()) {
            if (destination != null
                    && destination.getDestination() == target) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOwnedTroyAccessPair(
            StarSystemAPI system, JumpPointAPI hyperAccess) {
        if (system == null || hyperAccess == null
                || hyperAccess.getMemoryWithoutUpdate() == null
                || !hyperAccess.getMemoryWithoutUpdate().getBoolean(
                        ACCESS_JUMP_MARKER)) {
            return false;
        }
        SectorEntityToken local = system.getEntityById(ACCESS_JUMP_ID);
        if (!(local instanceof JumpPointAPI)
                || local.getMemoryWithoutUpdate() == null
                || !local.getMemoryWithoutUpdate().getBoolean(
                        ACCESS_JUMP_MARKER)) {
            return false;
        }
        return hasDestination((JumpPointAPI) local, hyperAccess)
                && hasDestination(hyperAccess, local);
    }

    static SectorEntityToken spawnGoalStation(StarSystemAPI system, Vector2f location) {
        SectorEntityToken existing = system.getEntityById(STATION_ID);
        if (existing != null) {
            return existing.getMemoryWithoutUpdate().getBoolean(
                    "$chief_navigator_goal_station") ? existing : null;
        }

        SectorEntityToken station = system.addCustomEntity(
                STATION_ID, "The Far Threshold", "station_side00", Factions.NEUTRAL);
        station.getMemoryWithoutUpdate().set(
                "$chief_navigator_goal_station", true);
        station.setLocation(location.x, location.y);
        configureThreshold(station);
        system.getMemoryWithoutUpdate().unset("$chief_navigator_goal_not_spawned");
        return station;
    }

    private static void configureThreshold(SectorEntityToken station) {
        station.setName("The Far Threshold");
        station.setFaction(Factions.NEUTRAL);
        station.setSensorProfile(1f);
        station.setDiscoverable(false);
        station.getDetectedRangeMod().modifyFlat("chief_navigator_goal", 10000f);
        station.setInteractionImage("illustrations", "jump_point_normal");
        station.getMemoryWithoutUpdate().set("$chief_navigator_goal_station", true);
        Misc.makeImportant(station, THRESHOLD_IMPORTANT);
    }

    /** Historical compatibility entry point; load-time cleanup is retired. */
    public static void cleanupLegacyPrototypeContent(StarSystemAPI system) {
        // Intentionally inert. A loaded campaign is not a migration target;
        // static objects are neither deleted nor reconstructed behind it.
    }

    /**
     * Keeps the historical Gautama fleet before Labor V, then migrates that
     * same save-compatible fleet ID into the one-time final rematch.
     */
    static CampaignFleetAPI spawnStarvingThreatSample(StarSystemAPI system, SectorEntityToken station) {
        if (system == null || station == null) return null;
        if (hasBlockingGautamaIdClaimAnywhere()) return null;
        boolean finalRematch = MenelausTrial.isComplete();
        if (finalRematch) {
            // Callers retain their historical Devoured Reach entry points for
            // pre-Labor Gautama. Once the Labors are complete, Last Light is
            // authoritative regardless of which compatibility path called us.
            StarSystemAPI lastLight = OdysseyExpanseSystem.findSystemById(
                    OdysseyExpanseSystem.LAST_LIGHT_ID);
            if (lastLight != null) {
                system = lastLight;
                station = getUngaikyoAnchor(lastLight);
            }
        }
        if (finalRematch
                && OdysseyPredatorScript
                        .isGautamaFinalRematchDefeated()) {
            cleanupGautamaFinalRematch();
            return null;
        }
        if (MenelausTrial.isFinalLaborActive()) {
            retireGautamaForFinalLabor();
            return null;
        }
        CampaignFleetAPI fleet = findGautamaFleetAnywhere();
        if (fleet != null) {
            if (finalRematch) {
                // Never swap variants, move the campaign token, or rewrite
                // orders while Starsector owns the fleet for a battle,
                // transition, or open interaction dialog.
                if (isFleetBusyForMutation(fleet)) return fleet;
                Global.getSector().getMemoryWithoutUpdate().set(
                        GAUTAMA_FINAL_REMATCH_CREATED_V1, true);
                if (fleet.isEmpty()) return fleet;
                ensureUngaikyoFinalRematchRoster(fleet);
                moveGautamaFleetToSystem(fleet, system);
                configureGautamaFinalRematch(system, fleet);
                system.getMemoryWithoutUpdate().set(
                        STARVING_THREAT_SPAWNED, true);
                return fleet;
            }
        }
        if (fleet != null && !finalRematch) {
            // Routine population maintenance must not rewrite a fleet while
            // Starsector owns it for combat, transit, despawn, or a dialog.
            if (isFleetBusyForMutation(fleet)) return fleet;
            LocationAPI oldLocation = fleet.getContainingLocation();
            boolean gautamaPresent = hasGautama(fleet);
            if (oldLocation != system) {
                if (oldLocation != null) oldLocation.removeEntity(fleet);
                system.addEntity(fleet);
            }
            fleet.setName("Heavenly Strike");
            fleet.setNoFactionInName(true);
            ensureStarvingFabricator(fleet);
            if (!gautamaPresent) {
                // Before Labor V is complete, a surviving historical escort
                // fleet without its boss records the objective win. Post-
                // Labor Gautama is never re-added through this legacy path.
                system.getMemoryWithoutUpdate().set(
                        GAUTAMA_DEFEATED_IN_AVICI, true);
            }
            OdysseyPredatorScript.ensurePostLaborStrikeRoster(fleet, 2);
            makeGautamaFlagship(fleet);
            OdysseyPredatorScript.configureStarvingThreatIdentity(fleet);
            configureGautamaAssignment(system, station, fleet, false);
            system.getMemoryWithoutUpdate().set(STARVING_THREAT_SPAWNED, true);
            return fleet;
        }
        if (!MenelausTrial.isComplete() && wasLegacyGautamaDefeated()) {
            system.getMemoryWithoutUpdate().set(STARVING_THREAT_SPAWNED, true);
            system.getMemoryWithoutUpdate().set(
                    GAUTAMA_DEFEATED_IN_AVICI, true);
            return null;
        }
        if (!MenelausTrial.isComplete()
                && system.getMemoryWithoutUpdate().getBoolean(
                        STARVING_THREAT_SPAWNED)) {
            return null;
        }
        if (finalRematch
                && Global.getSector().getMemoryWithoutUpdate().getBoolean(
                        GAUTAMA_FINAL_REMATCH_CREATED_V1)) {
            return null;
        }

        fleet = Global.getFactory().createEmptyFleet(
                Factions.THREAT, "Heavenly Strike", true);
        fleet.setId(STARVING_THREAT_FLEET_ID);
        fleet.setName("Heavenly Strike");
        fleet.setNoFactionInName(true);
        fleet.setTransponderOn(false);

        if (finalRematch) {
            // Commit before building the roster. If a third-party variant
            // throws, the failed attempt is quarantined instead of retried on
            // every population tick.
            Global.getSector().getMemoryWithoutUpdate().set(
                    GAUTAMA_FINAL_REMATCH_CREATED_V1, true);
            ensureUngaikyoFinalRematchRoster(fleet);
        } else {
            addStarvingThreatMember(
                    fleet, "chief_navigator_starving_hive_Type350");
            addStarvingThreatMember(
                    fleet, "chief_navigator_starving_fabricator_Type450");
            addStarvingThreatMember(
                    fleet, "chief_navigator_scylla_Fabricator");
            addStarvingThreatMember(
                    fleet, "chief_navigator_starving_standoff_Type300");
            addStarvingThreatMember(
                    fleet, "chief_navigator_starving_assault_Type200");
            addStarvingThreatMember(
                    fleet, "chief_navigator_starving_overseer_Type250");
            addStarvingThreatMember(
                    fleet, "chief_navigator_starving_skirmish_Type100");
            addStarvingThreatMember(
                    fleet, "chief_navigator_starving_skirmish_Type101");
            addStarvingThreatMember(
                    fleet, "chief_navigator_starving_skirmish_Type100");
            OdysseyPredatorScript.ensurePostLaborStrikeRoster(fleet, 2);
        }
        if (finalRematch) {
            makeUngaikyoFlagship(fleet);
        } else {
            makeGautamaFlagship(fleet);
        }
        OdysseyPredatorScript.configureStarvingThreatIdentity(fleet);
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();

        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);

        system.addEntity(fleet);
        if (finalRematch) {
            placeUngaikyoNearAnchor(fleet, system);
            configureGautamaFinalRematch(system, fleet);
        } else {
            configureGautamaAssignment(system, station, fleet, true);
        }
        system.getMemoryWithoutUpdate().set(STARVING_THREAT_SPAWNED, true);
        return fleet;
    }

    /** Moves Gautama out of the ring so he appears only in Labor V's finale. */
    static boolean retireGautamaForFinalLabor() {
        CampaignFleetAPI fleet = findGautamaFleetAnywhere();
        if (fleet == null) return true;
        if (isFleetBusyForMutation(fleet)) {
            return false;
        }
        LocationAPI location = fleet.getContainingLocation();
        if (location != null) location.removeEntity(fleet);
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            removeOwnedGautamaAnchor(system);
        }
        return true;
    }

    /** Removes the one-shot rematch after its durable completion flag is set. */
    static boolean cleanupGautamaFinalRematch() {
        if (Global.getSector() == null) return false;
        Set<LocationAPI> locations = new HashSet<LocationAPI>();
        locations.addAll(Global.getSector().getStarSystems());
        locations.add(Global.getSector().getHyperspace());
        boolean fleetRemains = false;

        for (LocationAPI location : locations) {
            if (location == null) continue;
            for (CampaignFleetAPI candidate
                    : new ArrayList<CampaignFleetAPI>(
                            location.getFleets())) {
                if (!isOwnedGautamaFleetClaim(candidate)
                        || !candidate.getMemoryWithoutUpdate().getBoolean(
                                OdysseyPredatorScript
                                        .GAUTAMA_FINAL_REMATCH_MARKER)) {
                    continue;
                }
                if (isPlayerControlledFleet(candidate)) {
                    fleetRemains = true;
                    continue;
                }
                if (isFleetBusyForMutation(candidate)) {
                    fleetRemains = true;
                    continue;
                }
                location.removeEntity(candidate);
            }
            SectorEntityToken byId = location.getEntityById(
                    STARVING_THREAT_FLEET_ID);
            if (byId != null) fleetRemains = true;
        }

        if (!fleetRemains) {
            for (StarSystemAPI candidate
                    : Global.getSector().getStarSystems()) {
                removeOwnedGautamaAnchor(candidate);
            }
        }
        return !fleetRemains;
    }

    /** True when moving, rebuilding, or reassigning this fleet is unsafe. */
    static boolean isFleetBusyForMutation(CampaignFleetAPI fleet) {
        if (fleet == null
                || isPlayerControlledFleet(fleet)
                || fleet.getBattle() != null
                || fleet.isInHyperspaceTransition()
                || fleet.isDespawning()) {
            return true;
        }
        return Global.getSector() != null
                && Global.getSector().getCampaignUI() != null
                && Global.getSector().getCampaignUI()
                        .getCurrentInteractionDialog() != null
                && Global.getSector().getCampaignUI()
                        .getCurrentInteractionDialog()
                        .getInteractionTarget() == fleet;
    }

    private static boolean isPlayerControlledFleet(CampaignFleetAPI fleet) {
        return fleet != null && (fleet.isPlayerFleet()
                || Global.getSector() != null
                        && fleet == Global.getSector().getPlayerFleet());
    }

    /** Historical entry point retained as an inert compatibility facade. */
    private static void normalizeGautamaFinalRematchRoster(
            CampaignFleetAPI fleet) {
        // Never rewrite a serialized campaign fleet to make an old save look
        // current. New campaigns construct the canonical roster once.
    }

    private static void moveGautamaFleetToSystem(
            CampaignFleetAPI fleet, StarSystemAPI system) {
        LocationAPI oldLocation = fleet.getContainingLocation();
        if (oldLocation == system) return;
        if (oldLocation != null) oldLocation.removeEntity(fleet);
        system.addEntity(fleet);
        placeUngaikyoNearAnchor(fleet, system);
        for (StarSystemAPI candidate
                : Global.getSector().getStarSystems()) {
            removeOwnedGautamaAnchor(candidate);
        }
    }

    /** Places the local Ungaikyo knot beside its locked Guardian prize. */
    private static void placeUngaikyoNearAnchor(
            CampaignFleetAPI fleet, StarSystemAPI system) {
        SectorEntityToken anchor = getUngaikyoAnchor(system);
        if (fleet == null || anchor == null) return;
        fleet.setLocation(
                anchor.getLocation().x + 1150f,
                anchor.getLocation().y + 425f);
    }

    private static SectorEntityToken getUngaikyoAnchor(
            StarSystemAPI system) {
        if (system == null) return null;
        SectorEntityToken guardian = system.getEntityById(
                OdysseyExpanseSystem.LAST_LIGHT_GUARDIAN_WRECK_ID);
        return guardian == null ? system.getCenter() : guardian;
    }

    /**
     * One-time in-place migration from the retired Gautama rematch roster to
     * Ungaikyo's boss-plus-five escort. Historical fleet ids and markers remain
     * stable so old saves keep the same campaign entity.
     */
    private static void ensureUngaikyoFinalRematchRoster(
            CampaignFleetAPI fleet) {
        if (fleet == null || isPlayerControlledFleet(fleet)
                || fleet.getMemoryWithoutUpdate().getBoolean(
                        UNGAIKYO_FINAL_REMATCH_ROSTER_V1)
                || isFleetBusyForMutation(fleet)) {
            return;
        }

        for (FleetMemberAPI member
                : new ArrayList<FleetMemberAPI>(
                        fleet.getFleetData().getMembersListCopy())) {
            fleet.getFleetData().removeFleetMember(member);
        }
        for (String variantId : UNGAIKYO_FINAL_REMATCH_VARIANTS) {
            FleetMemberAPI member = addStarvingThreatMember(
                    fleet, variantId);
            if (member != null
                    && "chief_navigator_ungaikyo".equals(
                            member.getHullId())) {
                member.setShipName("Ungaikyo");
            }
        }
        makeUngaikyoFlagship(fleet);
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
        fleet.getMemoryWithoutUpdate().set(
                GAUTAMA_FINAL_REMATCH_ROSTER_V1, true);
        fleet.getMemoryWithoutUpdate().set(
                UNGAIKYO_FINAL_REMATCH_ROSTER_V1, true);
        fleet.getMemoryWithoutUpdate().unset(
                GAUTAMA_FINAL_REMATCH_INVALID_V1);
    }

    /** Configures the isolated Last Light patrol without resetting its roster. */
    private static void configureGautamaFinalRematch(
            StarSystemAPI system, CampaignFleetAPI fleet) {
        EncounterCombatChatter.configure(fleet, EncounterCombatChatter.UNGAIKYO);
        fleet.setName("Ungaikyo");
        fleet.setNoFactionInName(true);
        fleet.setTransponderOn(false);
        fleet.setNoAutoDespawn(true);
        fleet.setAbortDespawn(true);
        fleet.setNoEngaging(0f);
        makeUngaikyoFlagship(fleet);
        OdysseyPredatorScript.configureStarvingThreatIdentity(fleet);

        com.fs.starfarer.api.campaign.rules.MemoryAPI memory =
                fleet.getMemoryWithoutUpdate();
        memory.set(OdysseyPredatorScript.GAUTAMA_FINAL_REMATCH_MARKER, true);
        memory.set(MemFlags.MEMORY_KEY_FLEET_TYPE,
                FleetTypes.PATROL_LARGE);
        memory.set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
        memory.set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        memory.set(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE, true);
        memory.set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        memory.set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
        memory.set(MemFlags.MEMORY_KEY_NO_JUMP, true);
        // The mirror-fleet encounter is self-contained. These campaign flags
        // keep the twenty-four feeding masses and any visiting fleet out.
        memory.set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true);
        memory.set(MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS, true);
        OdysseyPredatorScript.ensureGautamaFinalRematchListener(fleet);

        removeOwnedGautamaAnchor(system);

        if (!memory.getBoolean(UNGAIKYO_LAST_LIGHT_ASSIGNMENT_V1)) {
            fleet.clearAssignments();
            addUngaikyoGuardianAssignment(fleet, system);
            // Preserve the retired key so older saves and integrations do not
            // mistake this for an uninitialized historical rematch.
            memory.set(GAUTAMA_FINAL_REMATCH_CIRCUIT_V1, true);
            memory.set(UNGAIKYO_LAST_LIGHT_ASSIGNMENT_V1, true);
        } else if (fleet.getCurrentAssignment() == null) {
            addUngaikyoGuardianAssignment(fleet, system);
        }
    }

    private static void addUngaikyoGuardianAssignment(
            CampaignFleetAPI fleet, StarSystemAPI system) {
        SectorEntityToken anchor = getUngaikyoAnchor(system);
        if (anchor == null) return;
        fleet.addAssignment(
                FleetAssignment.ORBIT_AGGRESSIVE,
                anchor,
                1000000f,
                "coiled around the derelict Guardian");
    }

    private static void configureGautamaAssignment(
            StarSystemAPI system,
            SectorEntityToken ring,
            CampaignFleetAPI fleet,
            boolean newlyCreated) {
        EncounterCombatChatter.configure(fleet, EncounterCombatChatter.HEAVENLY);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_NO_JUMP, true);
        float x = ring.getLocation().x - 2800f;
        float y = ring.getLocation().y + 400f;
        if (newlyCreated) fleet.setLocation(x, y);
        SectorEntityToken anchor = system.getEntityById(
                STARVING_THREAT_ANCHOR_ID);
        if (anchor == null && newlyCreated) {
            anchor = system.createToken(x, y);
            anchor.setId(STARVING_THREAT_ANCHOR_ID);
            anchor.getMemoryWithoutUpdate().set(
                    STARVING_THREAT_ANCHOR_MARKER, true);
        } else if (!isOwnedGautamaAnchor(anchor)) {
            logBlockedGautamaAnchorOnce(anchor);
            anchor = null;
        }
        if (newlyCreated && anchor != null) anchor.setFixedLocation(x, y);
        if (fleet.getCurrentAssignment() == null) {
            fleet.addAssignment(
                    FleetAssignment.ORBIT_AGGRESSIVE,
                    ring,
                    1000000f,
                    "commanding the Heavenly Strike at the Devoured Ring");
        }
    }

    private static CampaignFleetAPI findGautamaFleetAnywhere() {
        CampaignFleetAPI best = null;
        for (StarSystemAPI candidate : Global.getSector().getStarSystems()) {
            SectorEntityToken entity = candidate.getEntityById(
                    STARVING_THREAT_FLEET_ID);
            if (entity instanceof CampaignFleetAPI) {
                CampaignFleetAPI fleet = (CampaignFleetAPI) entity;
                if (isOwnedGautamaFleetClaim(fleet)
                        && (best == null
                            || (!hasEncounterBoss(best)
                                    && hasEncounterBoss(fleet)))) {
                    best = fleet;
                }
            }
        }
        SectorEntityToken hyperspaceEntity = Global.getSector().getHyperspace()
                .getEntityById(STARVING_THREAT_FLEET_ID);
        if (hyperspaceEntity instanceof CampaignFleetAPI) {
            CampaignFleetAPI fleet = (CampaignFleetAPI) hyperspaceEntity;
            if (isOwnedGautamaFleetClaim(fleet)
                    && (best == null
                        || (!hasEncounterBoss(best)
                                && hasEncounterBoss(fleet)))) {
                best = fleet;
            }
        }
        return best;
    }

    /**
     * A canonical ID collision is authoritative serialized state. Suppress
     * the encounter instead of mutating the claimant or creating a duplicate.
     */
    private static boolean hasBlockingGautamaIdClaimAnywhere() {
        for (StarSystemAPI candidate : Global.getSector().getStarSystems()) {
            SectorEntityToken claim = candidate.getEntityById(
                    STARVING_THREAT_FLEET_ID);
            if (claim != null && (!(claim instanceof CampaignFleetAPI)
                    || !isOwnedGautamaFleetClaim(
                            (CampaignFleetAPI) claim))) {
                logBlockingGautamaClaimOnce(claim);
                return true;
            }
        }
        SectorEntityToken claim = Global.getSector().getHyperspace()
                .getEntityById(STARVING_THREAT_FLEET_ID);
        if (claim != null && (!(claim instanceof CampaignFleetAPI)
                || !isOwnedGautamaFleetClaim((CampaignFleetAPI) claim))) {
            logBlockingGautamaClaimOnce(claim);
            return true;
        }
        return false;
    }

    private static boolean isOwnedGautamaFleetClaim(CampaignFleetAPI fleet) {
        return !isPlayerControlledFleet(fleet)
                && (hasEncounterBoss(fleet)
                    || fleet.getMemoryWithoutUpdate().getBoolean(
                            OdysseyPredatorScript.STARVING_THREAT_MARKER)
                    || fleet.getMemoryWithoutUpdate().getBoolean(
                            OdysseyPredatorScript
                                    .GAUTAMA_FINAL_REMATCH_MARKER));
    }

    private static void logBlockingGautamaClaimOnce(
            SectorEntityToken claim) {
        if (Global.getSector().getMemoryWithoutUpdate().getBoolean(
                GAUTAMA_CANONICAL_ID_BLOCKED_V1)) return;
        Global.getSector().getMemoryWithoutUpdate().set(
                GAUTAMA_CANONICAL_ID_BLOCKED_V1, true);
        Global.getLogger(TroyArrivalScript.class).error(
                "Gautama's canonical fleet ID is occupied by incompatible "
                        + "serialized state ("
                        + claim.getClass().getName()
                        + "); suppressing reconstruction");
    }

    private static boolean isOwnedGautamaAnchor(SectorEntityToken anchor) {
        return anchor != null && anchor.getMemoryWithoutUpdate() != null
                && anchor.getMemoryWithoutUpdate().getBoolean(
                        STARVING_THREAT_ANCHOR_MARKER);
    }

    private static void removeOwnedGautamaAnchor(StarSystemAPI system) {
        if (system == null) return;
        SectorEntityToken anchor = system.getEntityById(
                STARVING_THREAT_ANCHOR_ID);
        if (anchor == null) return;
        if (!isOwnedGautamaAnchor(anchor)) {
            logBlockedGautamaAnchorOnce(anchor);
            return;
        }
        system.removeEntity(anchor);
    }

    private static void logBlockedGautamaAnchorOnce(
            SectorEntityToken anchor) {
        if (Global.getSector() == null
                || Global.getSector().getMemoryWithoutUpdate().getBoolean(
                        GAUTAMA_ANCHOR_BLOCKED_V1)) return;
        Global.getSector().getMemoryWithoutUpdate().set(
                GAUTAMA_ANCHOR_BLOCKED_V1, true);
        Global.getLogger(TroyArrivalScript.class).error(
                "Gautama anchor ID is owned by incompatible serialized "
                        + "state (" + anchor.getClass().getName()
                        + "); refusing mutation");
    }

    private static boolean hasGautama(CampaignFleetAPI fleet) {
        for (FleetMemberAPI member
                : fleet.getFleetData().getMembersListCopy()) {
            if (isGautamaMember(member)) return true;
        }
        return false;
    }

    private static boolean hasUngaikyo(CampaignFleetAPI fleet) {
        if (fleet == null) return false;
        for (FleetMemberAPI member
                : fleet.getFleetData().getMembersListCopy()) {
            if (isUngaikyoMember(member)) return true;
        }
        return false;
    }

    private static boolean hasEncounterBoss(CampaignFleetAPI fleet) {
        return hasGautama(fleet) || hasUngaikyo(fleet);
    }

    private static void makeGautamaFlagship(CampaignFleetAPI fleet) {
        for (FleetMemberAPI member
                : fleet.getFleetData().getMembersListCopy()) {
            if (isGautamaMember(member)) {
                fleet.getFleetData().setFlagship(member);
                return;
            }
        }
    }

    private static void makeUngaikyoFlagship(CampaignFleetAPI fleet) {
        if (fleet == null) return;
        for (FleetMemberAPI member
                : fleet.getFleetData().getMembersListCopy()) {
            if (isUngaikyoMember(member)) {
                member.setShipName("Ungaikyo");
                member.setPersonalityOverride(Personalities.CAUTIOUS);
                if (member.getCaptain() != null) {
                    member.getCaptain().setPersonality(Personalities.CAUTIOUS);
                }
                fleet.getFleetData().setFlagship(member);
                return;
            }
        }
    }

    private static boolean isUngaikyoMember(FleetMemberAPI member) {
        if (member == null) return false;
        if ("chief_navigator_ungaikyo".equals(member.getHullId())) {
            return true;
        }
        return member.getVariant() != null
                && "chief_navigator_ungaikyo_Fabricator".equals(
                        member.getVariant().getHullVariantId());
    }

    private static boolean isGautamaMember(FleetMemberAPI member) {
        if (member == null) return false;
        String hullId = member.getHullId();
        if ("chief_navigator_scylla".equals(hullId)
                || "chief_navigator_scylla_final".equals(hullId)) {
            return true;
        }
        return member.getVariant() != null
                && "chief_navigator_scylla_Final".equals(
                        member.getVariant().getHullVariantId());
    }

    private static boolean wasLegacyGautamaDefeated() {
        StarSystemAPI legacy = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.MESSINA_ID);
        return legacy != null
                && (legacy.getMemoryWithoutUpdate().getBoolean(
                        GAUTAMA_DEFEATED_IN_AVICI)
                    || legacy.getMemoryWithoutUpdate().getBoolean(
                            STARVING_THREAT_SPAWNED));
    }

    private static void ensureStarvingFabricator(CampaignFleetAPI fleet) {
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if ("chief_navigator_starving_fabricator_unit".equals(member.getHullId())) {
                return;
            }
        }
        addStarvingThreatMember(fleet, "chief_navigator_starving_fabricator_Type450");
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
    }

    /** Save-compatible objective check for Gautama's historical fleet ID. */
    static boolean isGautamaDefeated(StarSystemAPI system) {
        StarSystemAPI target = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.DEVOURED_REACH_ID);
        if (target == null) target = system;
        if (target == null) return false;
        if (target.getMemoryWithoutUpdate().getBoolean(
                GAUTAMA_DEFEATED_IN_AVICI)) {
            return true;
        }
        CampaignFleetAPI fleet = findGautamaFleetAnywhere();
        if (fleet != null) {
            for (FleetMemberAPI member
                    : fleet.getFleetData().getMembersListCopy()) {
                if (isGautamaMember(member)) {
                    return false;
                }
            }
            target.getMemoryWithoutUpdate().set(
                    GAUTAMA_DEFEATED_IN_AVICI, true);
            return true;
        }
        boolean defeated = target.getMemoryWithoutUpdate().getBoolean(
                STARVING_THREAT_SPAWNED) || wasLegacyGautamaDefeated();
        if (defeated) {
            target.getMemoryWithoutUpdate().set(
                    GAUTAMA_DEFEATED_IN_AVICI, true);
        }
        return defeated;
    }

    private static FleetMemberAPI addStarvingThreatMember(
            CampaignFleetAPI fleet, String variantId) {
        FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variantId);
        chiefnavigator.hullmods.StarvingThreatHullmod
                .enforceHivePersonality(member);
        fleet.getFleetData().addFleetMember(member);
        member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
        return member;
    }

    /** Retired compatibility hook; Budai is owned by OdysseyPredatorScript. */
    static CampaignFleetAPI spawnCharybdisSample(StarSystemAPI system, SectorEntityToken station) {
        return OdysseyPredatorScript.reconcileBudaiForLegacyCall();
    }

    static void ensureCharybdisCampaignVisual(CampaignFleetAPI fleet) {
        // Retained as a binary/save compatibility hook. Budai's separate
        // campaign overlay looked like a second hunter, so no new overlay is
        // created; the real fleet is the sole visible campaign presence.
    }

    /** Replaces the prototype's old stationary assignment, including in old saves. */
    private static boolean ensureCharybdisPursuit(CampaignFleetAPI fleet) {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (fleet == null || player == null) return false;
        if (fleet.getCurrentAssignment() != null
                && fleet.getCurrentAssignment().getAssignment() == FleetAssignment.INTERCEPT
                && fleet.getCurrentAssignment().getTarget() == player) {
            return true;
        }
        fleet.clearAssignments();
        fleet.addAssignment(FleetAssignment.INTERCEPT, player, 1000000f,
                "coming through the bleeding dark");
        return true;
    }

    /**
     * Retires only the historical standalone Spartan test fleet. FOB Ithaca's
     * three story cordon fleets have separate IDs and do not carry this marker.
     * A serialized sample engaged in combat or dialogue is left in place until
     * the recurring population pass can remove it safely.
     *
     * @return true once no matching sample remains in any campaign location
     */
    static boolean retireTaskForceSpartanSample() {
        if (Global.getSector() == null) return false;

        Set<LocationAPI> locations = new HashSet<LocationAPI>();
        locations.addAll(Global.getSector().getStarSystems());
        locations.add(Global.getSector().getHyperspace());
        boolean fullyRetired = true;

        for (LocationAPI location : locations) {
            if (location == null) continue;
            Set<CampaignFleetAPI> samples =
                    new HashSet<CampaignFleetAPI>();
            SectorEntityToken byId = location.getEntityById(
                    TASK_FORCE_SPARTAN_FLEET_ID);
            if (byId instanceof CampaignFleetAPI) {
                samples.add((CampaignFleetAPI) byId);
            }
            for (CampaignFleetAPI candidate
                    : new ArrayList<CampaignFleetAPI>(
                            location.getFleets())) {
                if (candidate.getMemoryWithoutUpdate().getBoolean(
                        TASK_FORCE_SPARTAN_MARKER)) {
                    samples.add(candidate);
                }
            }

            for (CampaignFleetAPI sample : samples) {
                if (!canSafelyRetireTaskForceSpartanSample(sample)) {
                    fullyRetired = false;
                    continue;
                }
                if (sample.getContainingLocation() != null) {
                    sample.getContainingLocation().removeEntity(sample);
                }
            }

            if (containsTaskForceSpartanSample(location)) {
                fullyRetired = false;
            } else {
                SectorEntityToken anchor = location.getEntityById(
                        TASK_FORCE_SPARTAN_ANCHOR_ID);
                if (anchor != null) location.removeEntity(anchor);
            }
        }
        return fullyRetired;
    }

    private static boolean canSafelyRetireTaskForceSpartanSample(
            CampaignFleetAPI fleet) {
        if (fleet == null
                || fleet.getBattle() != null
                || fleet.isInHyperspaceTransition()) {
            return false;
        }
        return Global.getSector().getCampaignUI() == null
                || Global.getSector().getCampaignUI()
                        .getCurrentInteractionDialog() == null
                || Global.getSector().getCampaignUI()
                        .getCurrentInteractionDialog()
                        .getInteractionTarget() != fleet;
    }

    private static boolean containsTaskForceSpartanSample(
            LocationAPI location) {
        SectorEntityToken byId = location.getEntityById(
                TASK_FORCE_SPARTAN_FLEET_ID);
        if (byId instanceof CampaignFleetAPI) return true;
        for (CampaignFleetAPI fleet : location.getFleets()) {
            if (fleet.getMemoryWithoutUpdate().getBoolean(
                    TASK_FORCE_SPARTAN_MARKER)) {
                return true;
            }
        }
        return false;
    }

    /** Dormant constructor retained while the Time Freeze prototype is shelved. */
    static CampaignFleetAPI spawnTimeFreezeFacetSample(
            StarSystemAPI system, SectorEntityToken guardTarget) {
        if (system == null || guardTarget == null) return null;

        CampaignFleetAPI fleet = null;
        boolean spawnedBefore = false;
        boolean blocked = false;
        for (StarSystemAPI candidate : new ArrayList<StarSystemAPI>(
                Global.getSector().getStarSystems())) {
            if (candidate.getMemoryWithoutUpdate().getBoolean(
                    TIME_FREEZE_SPAWNED)) {
                spawnedBefore = true;
            }
            SectorEntityToken entity = candidate.getEntityById(
                    TIME_FREEZE_FLEET_ID);
            if (entity != null) {
                CampaignFleetAPI found = entity instanceof CampaignFleetAPI
                        ? (CampaignFleetAPI) entity : null;
                if (found == null || isPlayerControlledFleet(found)
                        || found.isEmpty()
                        || found.getMemoryWithoutUpdate() == null
                        || !found.getMemoryWithoutUpdate().getBoolean(
                                TIME_FREEZE_MARKER)
                        || fleet != null && fleet != found) {
                    blocked = true;
                } else {
                    fleet = found;
                }
            }
        }

        if (blocked) {
            logBlockedTimeFreezeClaimOnce();
            system.getMemoryWithoutUpdate().set(TIME_FREEZE_SPAWNED, true);
            return null;
        }
        if (fleet != null) {
            system.getMemoryWithoutUpdate().set(TIME_FREEZE_SPAWNED, true);
            // Existing campaign state is authoritative. Never move or
            // reconfigure a serialized Facet during reconciliation.
            return fleet;
        }
        if (!spawnedBefore) {
            // Latch before factory/variant work so a third-party data error
            // cannot turn this into an every-load reconstruction loop.
            system.getMemoryWithoutUpdate().set(TIME_FREEZE_SPAWNED, true);
            try {
                fleet = Global.getFactory().createEmptyFleet(
                        Factions.OMEGA, "Time Freeze Facet", true);
                fleet.setId(TIME_FREEZE_FLEET_ID);
                FleetMemberAPI facet = Global.getFactory().createFleetMember(
                        FleetMemberType.SHIP,
                        "chief_navigator_time_freeze_facet_Attack");
                fleet.getFleetData().addFleetMember(facet);
                facet.getRepairTracker().setCR(
                        facet.getRepairTracker().getMaxCR());
                fleet.getFleetData().setSyncNeeded();
                fleet.getFleetData().syncIfNeeded();
                fleet.getMemoryWithoutUpdate().set(TIME_FREEZE_MARKER, true);
            } catch (RuntimeException ex) {
                Global.getLogger(TroyArrivalScript.class).error(
                        "Unable to create the Time Freeze Facet; leaving the "
                                + "encounter absent", ex);
                return null;
            }
        }
        if (fleet == null) {
            // A prior marker without a fleet means the encounter is over or
            // unavailable. Do not reconstruct it.
            system.getMemoryWithoutUpdate().set(TIME_FREEZE_SPAWNED, true);
            return null;
        }

        system.addEntity(fleet);
        fleet.setName("Time Freeze Facet");
        fleet.setNoFactionInName(true);
        fleet.setFaction(Factions.OMEGA, true);
        fleet.setNoAutoDespawn(true);
        fleet.setAbortDespawn(true);
        fleet.setTransponderOn(false);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE, true);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);

        // Keep the Facet close enough to intercept salvage attempts while still
        // leaving the wreck itself visible and selectable on the campaign map.
        float x = guardTarget.getLocation().x + 850f;
        float y = guardTarget.getLocation().y + 325f;
        fleet.setLocation(x, y);
        fleet.clearAssignments();
        fleet.addAssignment(
                FleetAssignment.DEFEND_LOCATION,
                guardTarget,
                1000000f,
                "guarding the derelict Guardian");
        system.getMemoryWithoutUpdate().set(TIME_FREEZE_SPAWNED, true);
        return fleet;
    }

    /**
     * Retires the shelved Time Freeze encounter without touching unrelated
     * Omega fleets or reconstructing any static Last Light object.
     */
    static boolean retireTimeFreezeFacetSample() {
        if (Global.getSector() == null) return false;
        if (Global.getSector().getMemoryWithoutUpdate().getBoolean(
                TIME_FREEZE_RETIRED_V1)) return true;

        boolean remains = false;
        for (StarSystemAPI system
                : Global.getSector().getStarSystems()) {
            SectorEntityToken entity = system.getEntityById(
                    TIME_FREEZE_FLEET_ID);
            if (!(entity instanceof CampaignFleetAPI)) continue;
            CampaignFleetAPI fleet = (CampaignFleetAPI) entity;
            if (isPlayerControlledFleet(fleet)
                    || fleet.getMemoryWithoutUpdate() == null
                    || !fleet.getMemoryWithoutUpdate().getBoolean(
                            TIME_FREEZE_MARKER)) {
                remains = true;
                continue;
            }
            if (isFleetBusyForMutation(fleet)) {
                remains = true;
                continue;
            }
            system.removeEntity(fleet);
        }
        if (!remains) {
            Global.getSector().getMemoryWithoutUpdate().set(
                    TIME_FREEZE_RETIRED_V1, true);
        }
        return !remains;
    }

    private static void logBlockedTimeFreezeClaimOnce() {
        if (Global.getSector() == null
                || Global.getSector().getMemoryWithoutUpdate().getBoolean(
                        TIME_FREEZE_CANONICAL_ID_BLOCKED_V1)) return;
        Global.getSector().getMemoryWithoutUpdate().set(
                TIME_FREEZE_CANONICAL_ID_BLOCKED_V1, true);
        Global.getLogger(TroyArrivalScript.class).error(
                "Time Freeze Facet ID is owned by incompatible or duplicate "
                        + "serialized state; suppressing reconstruction");
    }

    static CampaignFleetAPI spawnIthacaWall(StarSystemAPI system, SectorEntityToken station) {
        SectorEntityToken existing = system.getEntityById(DEFENSE_FLEET_ID);
        if (existing instanceof CampaignFleetAPI) {
            CampaignFleetAPI wall = (CampaignFleetAPI) existing;
            if (wall.getMemoryWithoutUpdate().getBoolean(DriftingWallEncounter.MARKER)) {
                return wall;
            }
            wall.getMemoryWithoutUpdate().set(DEFENSE_MARKER, true);
            wall.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
            ensureWallStation(wall);
            return wall;
        }

        CampaignFleetAPI wall = Global.getFactory().createEmptyFleet(
                Factions.PIRATES, "FOB Ithaca Defense Wall", true);
        wall.setId(DEFENSE_FLEET_ID);
        wall.setName("FOB Ithaca Defense Wall");
        FleetMemberAPI stationCore = Global.getFactory().createFleetMember(
                FleetMemberType.SHIP, "chief_navigator_ithaca_wall_segment_Standard");
        wall.getFleetData().addFleetMember(stationCore);
        wall.getFleetData().setSyncNeeded();
        wall.getFleetData().syncIfNeeded();
        wall.getMemoryWithoutUpdate().set(DEFENSE_MARKER, true);
        wall.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
        wall.setNoFactionInName(true);
        system.addEntity(wall);
        wall.setLocation(station.getLocation().x - 450f, station.getLocation().y);
        wall.addAssignment(FleetAssignment.DEFEND_LOCATION, station, 1000000f, "holding the wall");
        return wall;
    }

    /** Migrates old independent emplacements to one core with attached station modules. */
    private static void ensureWallStation(CampaignFleetAPI wall) {
        int stationCores = 0;
        for (FleetMemberAPI member : wall.getFleetData().getMembersListCopy()) {
            String hullId = member.getHullId();
            if (WALL_HULL_ID.equals(hullId)) {
                if (stationCores++ > 0) {
                    wall.getFleetData().removeFleetMember(member);
                }
            } else if ("onslaught".equals(hullId)
                    || LEGACY_TURRET_HULL_ID.equals(hullId)
                    || APRON_HULL_ID.equals(hullId)
                    || TURRET_HULL_ID.equals(hullId)
                    || DAMOCLES_HULL_ID.equals(hullId)
                    || METALSTORM_HULL_ID.equals(hullId)) {
                wall.getFleetData().removeFleetMember(member);
            }
        }
        if (stationCores == 0) {
            FleetMemberAPI stationCore = Global.getFactory().createFleetMember(
                    FleetMemberType.SHIP, "chief_navigator_ithaca_wall_segment_Standard");
            wall.getFleetData().addFleetMember(stationCore);
        }
        wall.getFleetData().setSyncNeeded();
        wall.getFleetData().syncIfNeeded();
    }
}
