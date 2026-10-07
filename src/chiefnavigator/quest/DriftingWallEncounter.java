package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;

/** The isolated wall fragment encountered in the first leg of the Odyssey. */
public final class DriftingWallEncounter {
    public static final String FLEET_ID = "chief_navigator_drifting_wall";
    public static final String MARKER = "$chief_navigator_drifting_wall";
    public static final String CORE_HULL_ID = "chief_navigator_drifting_wall_core";
    public static final String BERTH_HULL_ID = "chief_navigator_drifting_wall_berth";
    public static final String BASE_HULL_ID = "chief_navigator_drifting_wall_base";
    public static final String REACTOR_HULL_ID =
            "chief_navigator_drifting_wall_reactor";

    private static final String ENTITY_TYPE =
            "chief_navigator_drifting_wall_station";
    private static final String COMBAT_PROXY_ID =
            "chief_navigator_drifting_wall_combat_proxy";
    /**
     * Keep the temporary hostile Wall representation distinct from the
     * liberated Domain Combat Guard faction. The Wall assault itself does
     * not pull in campaign patrols; its authored hostile escorts remain.
     */
    public static final String COMBAT_PROXY_FACTION_ID = Factions.DERELICT;
    private static final String BASE_VARIANT_ID =
            "chief_navigator_drifting_wall_base_Standard";
    private static final String[] DRONE_GUARD_VARIANTS = new String[] {
        "chief_navigator_combat_guard_rampart_Standard",
        "chief_navigator_combat_guard_rampart_Standard",
        "chief_navigator_combat_guard_defender_PD",
        "chief_navigator_combat_guard_defender_PD",
        "chief_navigator_combat_guard_picket_Assault",
        "chief_navigator_combat_guard_sentry_FS",
        "chief_navigator_combat_guard_warden_Defense",
        "chief_navigator_combat_guard_warden_Defense"
    };
    private static final String SPAWNED = "$chief_navigator_drifting_wall_spawned";
    private static final String COMBAT_DISABLED_MARKER =
            "$chief_navigator_drifting_wall_reactor_disabled_in_combat";
    private static final String DISABLED_AT =
            "$chief_navigator_drifting_wall_disabled_at";
    private static final String ANCHOR_ID = "chief_navigator_drifting_wall_anchor";
    private static final float ENCOUNTER_X = -500f;
    private static final float CAMPAIGN_REPAIR_DAYS = 30f;
    static final float CAMPAIGN_INTERACTION_RADIUS = 450f;

    private DriftingWallEncounter() { }

    /** True only for Chief Navigator's authored campaign Wall token. */
    public static boolean isDriftingWall(SectorEntityToken entity) {
        return entity != null && FLEET_ID.equals(entity.getId())
                && ENTITY_TYPE.equals(entity.getCustomEntityType())
                && entity.getMemoryWithoutUpdate() != null
                && entity.getMemoryWithoutUpdate().getBoolean(MARKER);
    }

    /** Removes the journey encounter once the fleet has reached the threshold. */
    public static void removeFrom(StarSystemAPI system) {
        if (system == null) return;
        SectorEntityToken wall = system.getEntityById(FLEET_ID);
        if (isDriftingWall(wall)) system.removeEntity(wall);
        removeStaleCombatProxy(system);

        SectorEntityToken legacy = system.getEntityById(TroyArrivalScript.DEFENSE_FLEET_ID);
        if (legacy != null
                && legacy.getMemoryWithoutUpdate().getBoolean(MARKER)) {
            system.removeEntity(legacy);
        }
    }

    /**
     * Ensures the visible encounter is a fixed custom station, never a
     * campaign fleet. A temporary fleet exists only while resolving combat.
     */
    public static SectorEntityToken ensureExists(StarSystemAPI system) {
        if (system == null) return null;
        SectorEntityToken existing = system.getEntityById(FLEET_ID);
        if (existing != null) {
            return isDriftingWall(existing) ? existing : null;
        }

        SectorEntityToken legacy = system.getEntityById(TroyArrivalScript.DEFENSE_FLEET_ID);
        if (legacy != null) return null;
        if (system.getMemoryWithoutUpdate().getBoolean(SPAWNED)) return null;

        // Commit before construction so a partial failure is never retried by
        // a later caller as a destructive repair pass.
        system.getMemoryWithoutUpdate().set(SPAWNED, true);
        SectorEntityToken wall = system.addCustomEntity(
                FLEET_ID,
                "Drifting Wall Fragment",
                ENTITY_TYPE,
                Factions.NEUTRAL);
        wall.setFixedLocation(ENCOUNTER_X, 0f);
        configureStation(wall);
        return wall;
    }

    /** Creates the invisible-behind-the-dialog combat representation. */
    public static CampaignFleetAPI createCombatProxy(SectorEntityToken station) {
        if (!isDriftingWall(station)
                || station.getContainingLocation() == null) return null;
        if (getRepairDaysRemaining(station) > 0f) return null;
        if (!MenelausTrial.canAssaultWall()) return null;
        Global.getSector().getMemoryWithoutUpdate().unset(COMBAT_DISABLED_MARKER);
        LocationAPI location = station.getContainingLocation();
        SectorEntityToken stale = location.getEntityById(COMBAT_PROXY_ID);
        if (stale != null) {
            if (!(stale instanceof CampaignFleetAPI)
                    || isPlayerControlledFleet((CampaignFleetAPI) stale)
                    || !stale.getMemoryWithoutUpdate().getBoolean(MARKER)
                    || TroyArrivalScript.isFleetBusyForMutation(
                            (CampaignFleetAPI) stale)) {
                return null;
            }
            location.removeEntity(stale);
        }

        CampaignFleetAPI wall = Global.getFactory().createEmptyFleet(
                COMBAT_PROXY_FACTION_ID,
                "Drifting Wall Fragment",
                true);
        wall.setId(COMBAT_PROXY_ID);
        wall.setName("Drifting Wall Fragment");
        wall.setNoFactionInName(true);
        wall.setTransponderOn(false);
        wall.setStationMode(true);

        FleetMemberAPI core = Global.getFactory().createFleetMember(
                FleetMemberType.SHIP,
                BASE_VARIANT_ID);
        wall.getFleetData().addFleetMember(core);
        wall.getFleetData().setFlagship(core);
        core.getRepairTracker().setCR(core.getRepairTracker().getMaxCR());
        wall.getFleetData().setSyncNeeded();
        wall.getFleetData().syncIfNeeded();
        applyCombatFlags(wall);

        location.addEntity(wall);
        wall.setLocation(station.getLocation().x, station.getLocation().y);
        return wall;
    }

    public static void finishCombat(
            SectorEntityToken station,
            CampaignFleetAPI proxy) {
        boolean ownedProxy = proxy != null
                && !isPlayerControlledFleet(proxy)
                && proxy.getMemoryWithoutUpdate().getBoolean(MARKER);
        boolean disabled = consumeReactorDisabledMarker()
                || (ownedProxy && proxy.isEmpty());
        if (ownedProxy
                && proxy.getContainingLocation() != null) {
            proxy.getContainingLocation().removeEntity(proxy);
        }
        if (disabled && isDriftingWall(station)) {
            station.getMemoryWithoutUpdate().set(
                    DISABLED_AT,
                    Global.getSector().getClock().getTimestamp());
            MenelausTrial.reportWallDisabled(station);
        }
    }

    /** Called from combat when the exposed module is destroyed. */
    static void reportReactorDisabled() {
        if (Global.getSector() == null) return;
        Global.getSector().getMemoryWithoutUpdate().set(
                COMBAT_DISABLED_MARKER, true);
    }

    static String[] getDroneGuardVariants() {
        return DRONE_GUARD_VARIANTS.clone();
    }

    /** Returns zero once the Wall has rebuilt itself and can be fought again. */
    public static float getRepairDaysRemaining(SectorEntityToken station) {
        if (!isDriftingWall(station)
                || Global.getSector() == null) return 0f;
        if (!station.getMemoryWithoutUpdate().contains(DISABLED_AT)) return 0f;
        long disabledAt = station.getMemoryWithoutUpdate().getLong(DISABLED_AT);
        float remaining = CAMPAIGN_REPAIR_DAYS
                - Global.getSector().getClock().getElapsedDaysSince(disabledAt);
        if (remaining <= 0f) {
            station.getMemoryWithoutUpdate().unset(DISABLED_AT);
            return 0f;
        }
        return remaining;
    }

    private static boolean consumeReactorDisabledMarker() {
        if (Global.getSector() == null) return false;
        boolean disabled = Global.getSector().getMemoryWithoutUpdate()
                .getBoolean(COMBAT_DISABLED_MARKER);
        Global.getSector().getMemoryWithoutUpdate().unset(COMBAT_DISABLED_MARKER);
        return disabled;
    }

    private static void configureStation(SectorEntityToken wall) {
        wall.setFixedLocation(ENCOUNTER_X, 0f);
        if (wall instanceof CustomCampaignEntityAPI) {
            ((CustomCampaignEntityAPI) wall).setRadius(
                    CAMPAIGN_INTERACTION_RADIUS);
        }
        wall.setFacing(0f);
        wall.setSensorProfile(null);
        wall.setDiscoverable(null);
        wall.setExtendedDetectedAtRange(null);
        wall.getMemoryWithoutUpdate().set(MARKER, true);
        wall.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        if (MenelausTrial.isWallFriendly()) {
            wall.setFaction(Factions.PLAYER);
            wall.getMemoryWithoutUpdate().set(
                    MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE, true);
            wall.getMemoryWithoutUpdate().unset(
                    MemFlags.MEMORY_KEY_MAKE_HOSTILE);
            wall.getMemoryWithoutUpdate().unset(
                    MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE);
        } else {
            wall.setFaction(Factions.NEUTRAL);
            wall.getMemoryWithoutUpdate().unset(
                    MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE);
        }
    }

    private static void applyCombatFlags(CampaignFleetAPI fleet) {
        fleet.getMemoryWithoutUpdate().set(MARKER, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        fleet.getMemoryWithoutUpdate().unset(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY);
    }

    private static void removeStaleCombatProxy(StarSystemAPI system) {
        SectorEntityToken entity = system.getEntityById(COMBAT_PROXY_ID);
        if (!(entity instanceof CampaignFleetAPI)) return;
        CampaignFleetAPI fleet = (CampaignFleetAPI) entity;
        if (isPlayerControlledFleet(fleet)
                || !fleet.getMemoryWithoutUpdate().getBoolean(MARKER)
                || TroyArrivalScript.isFleetBusyForMutation(fleet)) {
            return;
        }
        system.removeEntity(fleet);
    }

    private static boolean isPlayerControlledFleet(CampaignFleetAPI fleet) {
        return fleet != null && (fleet.isPlayerFleet()
                || Global.getSector() != null
                        && fleet == Global.getSector().getPlayerFleet());
    }

}
