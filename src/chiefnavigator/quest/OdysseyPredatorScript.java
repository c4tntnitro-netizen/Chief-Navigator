package chiefnavigator.quest;

import chiefnavigator.hullmods.IthacaSiegeStalemateHullmod;
import chiefnavigator.weapons.IthacaResearchUpgrades;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI;
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI;
import com.fs.starfarer.api.campaign.ai.NavigationModulePlugin;
import com.fs.starfarer.api.campaign.ai.TacticalModulePlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.loading.WeaponGroupSpec;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import com.fs.starfarer.api.impl.combat.threat.ThreatFIDConfig;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import java.util.ArrayList;
import org.lwjgl.util.vector.Vector2f;

/** Maintains Odyssey Sector predators and renewable hostile populations. */
public final class OdysseyPredatorScript implements EveryFrameScript {
    public static final int FORCE_FINAL_LABOR_MISSING_WORLD = -1;
    public static final int FORCE_FINAL_LABOR_BUSY = -2;
    private static final String STARVING_PREFIX =
            "chief_navigator_messina_starving_patrol_";
    private static final String ASHEN_STARVING_PREFIX =
            "chief_navigator_ashen_starving_patrol_";
    private static final String DEVOURED_STARVING_PREFIX =
            "chief_navigator_devoured_starving_mass_";
    private static final String ALPHA_STARVING_PATROL_PREFIX =
            "chief_navigator_alpha_odyssey_starving_patrol_";
    private static final String SANZU_STARVING_PATROL_PREFIX =
            "chief_navigator_sanzu_starving_patrol_";
    private static final String REGIONAL_SMALL_PATROL_MARKER =
            "$chief_navigator_regional_small_starving_patrol_v1";
    private static final String ROGUE_DERELICT_PREFIX =
            "chief_navigator_odyssey_rogue_derelict_";
    public static final String AUTHORED_MOBILE_DERELICT_MARKER =
            "$chief_navigator_authored_mobile_derelict_v1";
    private static final String ROGUE_DERELICT_MARKER =
            "$chief_navigator_odyssey_rogue_derelict_v1";
    private static final String ROGUE_DERELICT_ANCHOR_PREFIX =
            "chief_navigator_odyssey_rogue_derelict_anchor_";
    private static final String ROGUE_DERELICT_SPAWNED_PREFIX =
            "$chief_navigator_odyssey_rogue_derelict_spawned_v1_";
    private static final String ROGUE_DERELICT_LAYOUT_MIGRATED_PREFIX =
            "$chief_navigator_odyssey_rogue_derelict_layout_v2_";
    private static final String[] AUTHORED_DERELICT_PATROL_PREFIXES = {
        ROGUE_DERELICT_PREFIX,
        "chief_navigator_wall_derelict_roamer_",
        "chief_navigator_ashen_trial_roamer_",
        "chief_navigator_unsealed_guard_"
    };
    private static final String[] DERELICT_PATROL_SYSTEM_IDS = {
        OdysseyExpanseSystem.SYSTEM_ID,
        OdysseyExpanseSystem.ASHEN_VERGE_ID,
        OdysseyExpanseSystem.LAST_LIGHT_ID
    };
    private static final String TROY_TRANSIT_PENDING =
            "$chief_navigator_troy_transit_pending";
    private static final String FINAL_INVASION_PREFIX =
            "chief_navigator_final_labor_invasion_";
    private static final String ITHACA_MONTHLY_STRIKE_ID =
            "chief_navigator_ithaca_monthly_strike";
    private static final String ITHACA_CONSOLE_STRIKE_PREFIX =
            "chief_navigator_ithaca_console_strike_";
    private static final String ITHACA_ENDGAME_CONSOLE_PREFIX =
            "chief_navigator_ithaca_endgame_invasion_";
    private static final String ITHACA_ENDGAME_INVASION_MARKER =
            "$chief_navigator_ithaca_endgame_invasion";
    private static final String ITHACA_ENDGAME_INVASION_ACTIVE =
            "$chief_navigator_ithaca_endgame_invasion_active_v1";
    private static final String ITHACA_ENDGAME_PLAYER_INTERVENED_PREFIX =
            "$chief_navigator_ithaca_endgame_player_intervened_";
    private static final String ITHACA_ENDGAME_TARGET_CORRECTION_LOG =
            "$chief_navigator_ithaca_endgame_target_correction_log_v1";
    public static final String ITHACA_FINAL_CENTER_BATTLE_MARKER =
            "$chief_navigator_ithaca_final_center_battle_v1";
    private static final String ITHACA_FINAL_LABOR_INITIALIZED =
            "$chief_navigator_ithaca_final_labor_initialized_v1";
    private static final String ITHACA_FINAL_LABOR_LANE_REPELLED_PREFIX =
            "$chief_navigator_ithaca_final_labor_lane_repelled_";
    private static final String ITHACA_FINAL_LABOR_SIEGE_STARTED_PREFIX =
            "$chief_navigator_ithaca_final_labor_siege_started_";
    private static final String ITHACA_FINAL_LABOR_BASTION_LOST_PREFIX =
            "$chief_navigator_ithaca_final_labor_bastion_lost_";
    private static final String ITHACA_FINAL_LABOR_PHASE =
            "$chief_navigator_ithaca_final_labor_phase_v1";
    private static final String ITHACA_FINAL_LABOR_TFS_TARGET =
            "$chief_navigator_ithaca_final_labor_tfs_target_v1";
    private static final String ITHACA_FINAL_LABOR_SIEGE_TOKEN_PREFIX =
            "chief_navigator_ithaca_final_labor_siege_";
    private static final String ITHACA_FINAL_CENTER_FLEET_ID =
            "chief_navigator_final_labor_heavenly_strike";
    private static final String ITHACA_FINAL_CENTER_TARGET_ID =
            "chief_navigator_final_labor_center_target";
    private static final String ITHACA_FINAL_CENTER_ASSAULT_ORDERED =
            "$chief_navigator_ithaca_final_center_assault_ordered_v1";
    private static final String ITHACA_FINAL_CENTER_CREATED_V1 =
            "$chief_navigator_ithaca_final_center_created_v1";
    private static final String ITHACA_FINAL_CENTER_AUTORE_SOLVE_LOG =
            "$chief_navigator_ithaca_final_center_autoresolve_log_v1";
    private static final String ITHACA_FINAL_CENTER_TFS_TELEPORTED =
            "$chief_navigator_ithaca_final_center_tfs_teleported_v1";
    private static final String GATE_BREACH_FLEET_PREFIX =
            "chief_navigator_gate_breach_";
    private static final String GATE_BREACH_FLEET_MARKER =
            "$chief_navigator_gate_breach_fleet_v1";
    private static final String GATE_BREACH_SOURCE =
            "$chief_navigator_gate_breach_source_v1";
    private static final String GATE_BREACH_RESPAWN_COOLDOWN =
            "$chief_navigator_gate_breach_respawn_cooldown_v1";
    private static final String ITHACA_MONTHLY_STRIKE_INITIALIZED =
            "$chief_navigator_ithaca_monthly_strike_initialized_v1";
    private static final String ITHACA_MONTHLY_STRIKE_COOLDOWN =
            "$chief_navigator_ithaca_monthly_strike_cooldown_v1";
    private static final String ITHACA_MONTHLY_STRIKE_ACTIVE =
            "$chief_navigator_ithaca_monthly_strike_active_v1";
    private static final String ITHACA_MONTHLY_STRIKE_TIER =
            "$chief_navigator_ithaca_monthly_strike_tier_v1";
    private static final String ITHACA_MONTHLY_STRIKE_TARGET =
            "$chief_navigator_ithaca_monthly_strike_target_v1";
    private static final String ITHACA_STRIKE_ASSAULT_ORDERED =
            "$chief_navigator_ithaca_strike_assault_ordered_v1";
    private static final String ITHACA_STRIKE_TARGET_TOKEN_PREFIX =
            "chief_navigator_ithaca_strike_target_";
    private static final String ITHACA_STRIKE_TRACE_MISSING_SYSTEM =
            "$chief_navigator_ithaca_strike_trace_missing_system_v1";
    private static final String ITHACA_STRIKE_TRACE_MISSING_CENTER =
            "$chief_navigator_ithaca_strike_trace_missing_center_v1";
    private static final String ITHACA_STRIKE_TRACE_PULSE =
            "$chief_navigator_ithaca_strike_trace_pulse_v1";
    private static final String ITHACA_STRIKE_TRACE_BATTLE =
            "$chief_navigator_ithaca_strike_trace_battle_v1";
    private static final String CHARYBDIS_HYPER_ANCHOR_ID =
            "chief_navigator_charybdis_hyper_anchor";
    private static final String OWNED_PROXY_TOKEN_MARKER =
            "$chief_navigator_owned_proxy_token_v1";
    private static final String BLOCKED_PROXY_TOKEN_LOG_PREFIX =
            "$chief_navigator_blocked_proxy_token_v1_";
    private static final String LEGACY_CHARYBDIS_ANCHOR_ID =
            "chief_navigator_charybdis_anchor";
    private static final String DETECTION_MOD =
            "chief_navigator_charybdis_hunter_detection";
    private static final String SENSOR_RANGE_MOD =
            "chief_navigator_charybdis_hunter_sensor_range";
    private static final String SENSOR_STRENGTH_MOD =
            "chief_navigator_charybdis_hunter_sensor_strength";
    public static final String STARVING_THREAT_MARKER =
            "$chief_navigator_starving_threat_fleet";
    /** Durable sector flag: the one-time post-Labor rematch is over. */
    public static final String GAUTAMA_FINAL_REMATCH_DEFEATED =
            "$chief_navigator_gautama_final_rematch_defeated_v1";
    /** Fleet marker used by interaction and battle-plugin routing. */
    public static final String GAUTAMA_FINAL_REMATCH_MARKER =
            "$chief_navigator_gautama_final_rematch_fleet_v1";
    private static final String STARVING_THREAT_HULL_TAG =
            "chief_navigator_starving_threat";
    private static final String STOCK_THREAT_HULLMOD = "threat_hullmod";
    private static final String FRAGMENT_SWARM_HULLMOD = "fragment_swarm";
    private static final String GAUTAMA_HULL_ID = "chief_navigator_scylla";
    private static final String GAUTAMA_FINAL_HULL_ID =
            "chief_navigator_scylla_final";
    private static final String GAUTAMA_REINCARNATING_HULL_ID =
            "chief_navigator_scylla_reincarnating";
    private static final String GAUTAMA_VARIANT_ID =
            "chief_navigator_scylla_Fabricator";
    private static final String GAUTAMA_FINAL_VARIANT_ID =
            "chief_navigator_scylla_Final";
    private static final String GAUTAMA_REINCARNATING_VARIANT_ID =
            "chief_navigator_scylla_reincarnating_Fabricator";
    private static final String UNGAIKYO_HULL_ID =
            "chief_navigator_ungaikyo";
    private static final String UNGAIKYO_VARIANT_ID =
            "chief_navigator_ungaikyo_Fabricator";
    private static final String BUDAI_HUNTER_MARKER =
            "$chief_navigator_budai_hunter";
    private static final String BUDAI_SPAWNED =
            "$chief_navigator_budai_spawned";
    public static final String BUDAI_DEFEATED =
            "$chief_navigator_budai_defeated_v1";
    private static final int FIRST_STRIKE = 0;
    private static final int SECOND_STRIKE = 1;
    private static final int THIRD_STRIKE = 2;

    // Deterministic Starving equivalents of the role-count ranges used by
    // vanilla's First, Second, and Third Strike generation. Columns are:
    // Fabricator, Hive, Overseer, Line, Assault, and Skirmish.
    private static final int[][] POST_LABOR_STRIKE_COUNTS = {
        {0, 1, 3, 1, 2, 4},
        {1, 1, 3, 1, 2, 4},
        {2, 3, 4, 3, 5, 8}
    };
    // Same role order as ordinary Strikes. These replace, not supplement,
    // their Third Strike roster, retaining roughly the same deployment cost.
    private static final int[] SCYLLA_STRIKE_COUNTS = {1, 9, 1, 2, 1, 3};
    private static final int[] SIREN_STRIKE_COUNTS = {1, 1, 2, 8, 2, 10};
    private static final int[] CYCLOPS_STRIKE_COUNTS = {9, 1, 2, 2, 2, 4};
    private static final String[] STARVING_ROLE_HULLS = {
        "chief_navigator_starving_fabricator_unit",
        "chief_navigator_starving_hive_unit",
        "chief_navigator_starving_overseer_unit",
        "chief_navigator_starving_standoff_unit",
        "chief_navigator_starving_assault_unit",
        "chief_navigator_starving_skirmish_unit"
    };
    private static final String[][] STARVING_ROLE_VARIANTS = {
        {"chief_navigator_starving_fabricator_Type450"},
        {"chief_navigator_starving_hive_Type350"},
        {"chief_navigator_starving_overseer_Type250"},
        {
            "chief_navigator_starving_standoff_Type300",
            "chief_navigator_starving_standoff_Type301",
            "chief_navigator_starving_standoff_Type302"
        },
        {
            "chief_navigator_starving_assault_Type200",
            "chief_navigator_starving_assault_Type201"
        },
        {
            "chief_navigator_starving_skirmish_Type100",
            "chief_navigator_starving_skirmish_Type101"
        }
    };

    /**
     * One ordered reinforcement tranche for Labor V's Heavenly Strike.
     * Roles are deliberately interleaved so the campaign fleet's reserve
     * order never degenerates into a block of one hull type. The old finale
     * carried one eleven-unit First Strike escort; three copies of this
     * eleven-slot pattern triple that supporting force while trading one
     * skirmisher in each tranche for a Fabricator.
     */
    private static final int[] ITHACA_FINAL_CENTER_MIXED_WAVE_ROLES = {
        0,  // Fabricator
        4,  // Assault escort
        1,  // Hive
        2,  // Overseer escort
        5,  // Skirmish escort
        3,  // Line/standoff escort
        4,  // Assault escort
        2,  // Overseer escort
        5,  // Skirmish escort
        2,  // Overseer escort
        5   // Skirmish escort
    };
    private static final int ITHACA_FINAL_CENTER_MIXED_WAVE_COUNT = 3;
    private static final String ITHACA_FINAL_CENTER_WAVE_MEMBER_PREFIX =
            "chief_navigator_final_labor_wave_";

    private static final int ASHEN_STARVING_PATROL_COUNT = 10;
    private static final int DEVOURED_STARVING_COUNT = 24;
    private static final int ALPHA_STARVING_PATROL_COUNT = 2;
    private static final int SANZU_STARVING_PATROL_COUNT = 2;
    private static final int ROGUE_DERELICT_COUNT = 8;
    static final float ITHACA_MONTHLY_STRIKE_MIN_DAYS = 25f;
    static final float ITHACA_MONTHLY_STRIKE_MAX_DAYS = 35f;
    private static final float ITHACA_MONTHLY_STRIKE_LIFETIME_DAYS = 20f;
    private static final float ITHACA_MONTHLY_STRIKE_MIN_SPAWN_RANGE = 11000f;
    private static final float ITHACA_MONTHLY_STRIKE_MAX_SPAWN_RANGE = 15000f;
    private static final float ITHACA_STRIKE_TARGET_TOKEN_RADIUS = 100f;
    private static final float ITHACA_STRIKE_TARGET_TOKEN_PERIOD = 60f;
    private static final float ITHACA_STRIKE_PRIORITY_DAYS = 1000f;
    private static final float ITHACA_ENDGAME_INVASION_DAYS = 1000000f;
    private static final float ITHACA_ENDGAME_SPAWN_DISTANCE = 7000f;
    private static final float ITHACA_ENDGAME_IGNORE_DISTRACTION_DAYS = 2f;
    static final float ITHACA_FINAL_LABOR_SIEGE_DAYS = 60f;
    private static final float ITHACA_FINAL_LABOR_SIEGE_STANDOFF = 1200f;
    private static final float ITHACA_FINAL_LABOR_SIEGE_BLOCK_DAYS = 2f;
    private static final float ITHACA_FINAL_CENTER_DEFENSE_DISTANCE = 650f;
    private static final float ITHACA_FINAL_CENTER_SPAWN_DISTANCE = 9000f;
    private static final float CHARYBDIS_STAGING_OFFSET = 900f;
    private static final float BUDAI_INITIAL_SPAWN_DISTANCE = 100_000f;
    private static final float CHARYBDIS_SENSOR_OVERRIDE = 1000000f;
    private static final float TROY_ARRIVAL_SAFE_RADIUS = 4500f;
    private static final float TROY_ARRIVAL_CLEAR_RADIUS = 6500f;
    static final float DERELICT_JUMP_AVOIDANCE_MIN = 4500f;
    static final float DERELICT_JUMP_AVOIDANCE_MAX = 6500f;
    static final float DERELICT_JUMP_ARRIVAL_TRIGGER =
            TROY_ARRIVAL_SAFE_RADIUS;
    static final float DERELICT_JUMP_ARRIVAL_CLEARANCE =
            TROY_ARRIVAL_CLEAR_RADIUS;
    static final float DERELICT_ANCHOR_CLEARANCE = 7500f;
    private static final float ROGUE_DERELICT_PLAYER_SPAWN_CLEARANCE = 7000f;
    private static final float DERELICT_JUMP_AVOIDANCE_DURATION = 1f;

    private static final float[][] ASHEN_STARVING_OFFSETS = {
        {-9500f,  6800f}, {-4800f,  7100f}, { 1200f,  6900f},
        { 9000f,  6200f}, {-9300f,  1200f}, { 9400f,   700f},
        {-8600f, -5600f}, {-2600f, -6900f}, { 3500f, -6700f},
        { 9200f, -5200f}
    };

    private static final float[][] DEVOURED_STARVING_OFFSETS = {
        {    0f,  1450f}, {  560f,  1380f}, { 1080f,  1120f},
        { 1430f,   520f}, { 1500f,     0f}, { 1380f,  -620f},
        { 1030f, -1170f}, {  480f, -1510f}, { -120f, -1580f},
        { -720f, -1430f}, {-1220f, -1030f}, {-1510f,  -390f},
        {-1500f,   300f}, {-1180f,  1010f}, { -650f,  1420f},
        { -220f,  2060f}, {-7600f,  6100f}, {-2600f,  7600f},
        { 3600f,  7200f}, { 7900f,  4900f}, {-8200f, -2500f},
        {-3900f, -7200f}, { 2600f, -7900f}, { 8200f, -3700f}
    };

    // Kept away from the ordinary arrival corridors. PATROL_SYSTEM makes
    // these starting positions seeds rather than permanent guard posts.
    private static final float[][] ALPHA_STARVING_PATROL_OFFSETS = {
        {-8200f,  6100f}, { 8600f, -6300f}
    };
    private static final float[][] SANZU_STARVING_PATROL_OFFSETS = {
        {-7200f,  5400f}, { 7500f, -5700f}
    };

    private static final String[] REGIONAL_SMALL_PATROL_VARIANTS = {
        "chief_navigator_starving_skirmish_Type100",
        "chief_navigator_starving_skirmish_Type101",
        "chief_navigator_starving_assault_Type200",
        "chief_navigator_starving_assault_Type201",
        "chief_navigator_starving_overseer_Type250"
    };
    private static final String[] REGIONAL_LINE_VARIANTS = {
        "chief_navigator_starving_standoff_Type300",
        "chief_navigator_starving_standoff_Type301",
        "chief_navigator_starving_standoff_Type302"
    };
    private static final float REGIONAL_LINE_CHANCE = 0.06f;

    private static final String[][] STARVING_LOADOUTS = {
        {
            "chief_navigator_starving_standoff_Type300",
            "chief_navigator_starving_skirmish_Type100",
            "chief_navigator_starving_skirmish_Type101"
        },
        {
            "chief_navigator_starving_assault_Type200",
            "chief_navigator_starving_overseer_Type250",
            "chief_navigator_starving_skirmish_Type100"
        },
        {
            "chief_navigator_starving_fabricator_Type450",
            "chief_navigator_starving_standoff_Type301",
            "chief_navigator_starving_skirmish_Type101",
            "chief_navigator_starving_skirmish_Type100"
        },
        {
            "chief_navigator_starving_hive_Type350",
            "chief_navigator_starving_assault_Type201",
            "chief_navigator_starving_standoff_Type302",
            "chief_navigator_starving_skirmish_Type100"
        }
    };

    private static final String[] ROGUE_SYSTEM_IDS = {
        OdysseyExpanseSystem.SYSTEM_ID,
        OdysseyExpanseSystem.SYSTEM_ID,
        OdysseyExpanseSystem.SILENT_WAKE_ID,
        OdysseyExpanseSystem.SILENT_WAKE_ID,
        OdysseyExpanseSystem.SYSTEM_ID,
        OdysseyExpanseSystem.ASHEN_VERGE_ID,
        OdysseyExpanseSystem.LAST_LIGHT_ID,
        OdysseyExpanseSystem.LAST_LIGHT_ID
    };

    private static final float[][] ROGUE_DERELICT_OFFSETS = {
        {-13000f,    0f}, {13000f,     0f},
        {-6200f, -4300f}, { 7100f,  3900f},
        {    0f,-13000f}, { 9200f,  8200f},
        {-9800f, -1200f}, { 9800f,  1200f}
    };

    private static final String[][] ROGUE_DERELICT_LOADOUTS = {
        {
            "chief_navigator_combat_guard_rampart_Standard",
            "chief_navigator_combat_guard_defender_PD",
            "chief_navigator_combat_guard_picket_Assault"
        },
        {
            "chief_navigator_combat_guard_bastillon_Standard",
            "chief_navigator_combat_guard_sentry_FS",
            "chief_navigator_combat_guard_warden_Defense"
        },
        {
            "chief_navigator_combat_guard_berserker_Assault",
            "chief_navigator_combat_guard_rampart_Standard",
            "chief_navigator_combat_guard_defender_PD",
            "chief_navigator_combat_guard_picket_Assault"
        }
    };

    private final IntervalUtil interval = new IntervalUtil(0.35f, 0.55f);
    private float bastionRepairSeconds;
    @Override
    public void advance(float amount) {
        if (Global.getSector() == null) return;
        // Combat plugins cannot reliably observe the return to campaign.
        // Release encounter custom streams here before location music resumes.
        UngaikyoMusic.releaseBattleMusic();
        BudaiMusic.releaseBattleMusic();
        BudaiSalvageDialogPlugin.tryShowPending();
        IthacaResearchUpgrades.captureRecoveredComponents();
        IthacaResearchUpgrades.captureIsaRefractionUnlock();
        applyCharybdisTrackingOverride();
        if (MenelausTrial.isFinalLaborActive()) {
            // Keep transition-time cleanup from leaving the perimeter or
            // gate-approach phase silent between population ticks.
            FinalLaborMusic.maintainMissionMusic();
        } else {
            FinalLaborMusic.releaseMissionMusic();
        }
        bastionRepairSeconds += amount;
        interval.advance(amount);
        if (!interval.intervalElapsed()) {
            maintainDerelictJumpArrivalClearance();
            maintainDevouredJumpArrivalClearance();
            return;
        }

        StarSystemAPI repairSystem = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.ASHEN_VERGE_ID);
        if (repairSystem != null) {
            IthacaSectionEncounter.advanceBastionRepairs(
                    repairSystem.getEntityById(IthacaSectionEncounter.FOB_ENTITY_ID),
                    Global.getSector().getClock().convertToDays(bastionRepairSeconds));
        }
        bastionRepairSeconds = 0f;
        ensureStarvingPopulation();
        ensureMonthlyIthacaStrike();
        maintainIthacaEndgameConsoleInvasion();
        ensureRogueDerelictPopulation();
        ensureFinalLaborInvasion();
        ensureGateBreachPopulation();
        TroyArrivalScript.retireTimeFreezeFacetSample();
        ensurePostLaborUngaikyo();
        ensureCharybdisHunter();
        maintainDerelictJumpPointClearance();
        maintainDevouredJumpPointClearance();
    }

    /**
     * Historical compatibility entry point. Global containment passes were
     * retired because a broad scan must never delete or relocate serialized
     * campaign fleets during load or maintenance.
     */
    public static void enforceThreatContainment() {
        // Intentionally inert.
    }

    private static int getRogueDerelictIndex(CampaignFleetAPI fleet) {
        if (fleet == null || fleet.getId() == null
                || !fleet.getId().startsWith(ROGUE_DERELICT_PREFIX)) {
            return -1;
        }
        try {
            int index = Integer.parseInt(
                    fleet.getId().substring(ROGUE_DERELICT_PREFIX.length()));
            return index >= 0 && index < ROGUE_DERELICT_COUNT ? index : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    /** Identifies only Chief Navigator's authored Starving Threat fleets. */
    static boolean isAuthoredStarvingThreatFleet(CampaignFleetAPI fleet) {
        if (fleet == null || isPlayerControlledFleet(fleet)) return false;
        return fleet.getMemoryWithoutUpdate().getBoolean(
                STARVING_THREAT_MARKER);
    }

    private static boolean isPlayerControlledFleet(CampaignFleetAPI fleet) {
        return fleet != null && (fleet.isPlayerFleet()
                || Global.getSector() != null
                        && fleet == Global.getSector().getPlayerFleet());
    }

    /** Re-identifies old Omega-authored officers without replacing save objects. */
    static void configureStarvingThreatIdentity(CampaignFleetAPI fleet) {
        if (fleet == null || isPlayerControlledFleet(fleet)) return;
        if (TroyArrivalScript.isFleetBusyForMutation(fleet)) return;
        String factionId = getStarvingThreatFactionId(fleet);
        if (fleet.getFaction() == null
                || !factionId.equals(fleet.getFaction().getId())) {
            fleet.setFaction(factionId, true);
        }
        fleet.setInflater(null);
        fleet.getMemoryWithoutUpdate().set(STARVING_THREAT_MARKER, true);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_IGNORE_PLAYER_COMMS, true);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN,
                new ThreatFIDConfig());
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_PATROL_FLEET);

        normalizeStarvingThreatPerson(fleet.getCommander(), factionId);
        boolean variantsChanged = false;
        for (FleetMemberAPI member
                : fleet.getFleetData().getMembersListCopy()) {
            variantsChanged |= normalizeStarvingThreatHullmods(member);
            normalizeStarvingThreatPerson(member.getCaptain(), factionId);
            chiefnavigator.hullmods.StarvingThreatHullmod
                    .enforceHivePersonality(member);
        }
        if (variantsChanged) {
            fleet.getFleetData().setSyncNeeded();
            fleet.getFleetData().syncIfNeeded();
        }
    }

    /** Repairs legacy member variants without mutating shared stock variants. */
    static boolean normalizeStarvingThreatHullmods(FleetMemberAPI member) {
        if (member == null || member.getVariant() == null) return false;

        ShipVariantAPI original = member.getVariant();
        if (isGautamaMember(member)) {
            boolean hullmodsNeedRepair = referencesHullmod(
                    original, FRAGMENT_SWARM_HULLMOD)
                    || referencesHullmod(original, STOCK_THREAT_HULLMOD)
                    || referencesHullmod(
                            original,
                            chiefnavigator.hullmods.StarvingThreatHullmod
                            .HULLMOD_ID)
                    || referencesHullmod(original, "fragment_coordinator")
                    || referencesHullmod(original, "secondary_fabricator");
            ShipVariantAPI canonical = canonicalGautamaVariant(member);
            boolean armamentNeedsRepair = canonical != null
                    && !armamentMatches(original, canonical);
            if (!hullmodsNeedRepair && !armamentNeedsRepair) return false;

            ShipVariantAPI normalized = original.clone();
            if (hullmodsNeedRepair) {
                removeHullmodReferences(normalized, FRAGMENT_SWARM_HULLMOD);
                removeHullmodReferences(normalized, STOCK_THREAT_HULLMOD);
                removeHullmodReferences(
                        normalized,
                        chiefnavigator.hullmods.StarvingThreatHullmod
                                .HULLMOD_ID);
                removeHullmodReferences(normalized, "fragment_coordinator");
                removeHullmodReferences(normalized, "secondary_fabricator");
            }
            boolean armamentRepaired = !armamentNeedsRepair;
            if (armamentNeedsRepair) {
                ShipVariantAPI armed = normalized.clone();
                armamentRepaired = copyArmament(armed, canonical);
                if (armamentRepaired) normalized = armed;
            }
            if (!hullmodsNeedRepair && !armamentRepaired) {
                return false;
            }
            member.setVariant(normalized, false, true);
            Global.getLogger(OdysseyPredatorScript.class).info(
                    "Normalized legacy Gautama hullmods/armament on member "
                            + member.getId() + ".");
            return true;
        }

        if (member.getHullSpec() == null
                || !member.getHullSpec().hasTag(STARVING_THREAT_HULL_TAG)) {
            return false;
        }
        String starvingHullmod =
                chiefnavigator.hullmods.StarvingThreatHullmod.HULLMOD_ID;
        boolean stockPresent = referencesHullmod(
                original, STOCK_THREAT_HULLMOD);
        boolean starvingPresent = original.hasHullMod(starvingHullmod)
                && !original.getSuppressedMods().contains(starvingHullmod);
        boolean hullmodsNeedRepair = stockPresent || !starvingPresent;
        ShipVariantAPI canonical = canonicalStarvingVariant(member);
        boolean armamentNeedsRepair = canonical != null
                && !armamentMatches(original, canonical);
        if (!hullmodsNeedRepair && !armamentNeedsRepair) {
            return false;
        }

        ShipVariantAPI normalized = original.clone();
        if (hullmodsNeedRepair) {
            removeHullmodReferences(normalized, STOCK_THREAT_HULLMOD);
            normalized.removeSuppressedMod(starvingHullmod);
            if (!normalized.hasHullMod(starvingHullmod)) {
                normalized.addPermaMod(starvingHullmod);
            }
        }
        boolean armamentRepaired = !armamentNeedsRepair;
        if (armamentNeedsRepair) {
            ShipVariantAPI armed = normalized.clone();
            armamentRepaired = copyArmament(armed, canonical);
            if (armamentRepaired) normalized = armed;
        }
        if (!hullmodsNeedRepair && !armamentRepaired) {
            return false;
        }
        member.setVariant(normalized, false, true);
        Global.getLogger(OdysseyPredatorScript.class).info(
                "Normalized legacy Starving Threat hullmods/armament on "
                        + "member "
                        + member.getId() + " (" + member.getHullId() + ").");
        return true;
    }

    private static boolean referencesHullmod(
            ShipVariantAPI variant, String hullmodId) {
        return variant.hasHullMod(hullmodId)
                || variant.getHullMods().contains(hullmodId)
                || variant.getPermaMods().contains(hullmodId)
                || variant.getSMods().contains(hullmodId)
                || variant.getSuppressedMods().contains(hullmodId);
    }

    private static void removeHullmodReferences(
            ShipVariantAPI variant, String hullmodId) {
        variant.removeMod(hullmodId);
        variant.removePermaMod(hullmodId);
        variant.getSMods().remove(hullmodId);
        variant.removeSuppressedMod(hullmodId);
    }

    /** Resolves the authored Gautama fit appropriate to this serialized skin. */
    private static ShipVariantAPI canonicalGautamaVariant(
            FleetMemberAPI member) {
        if (member == null || Global.getSettings() == null) return null;
        String variantId = GAUTAMA_VARIANT_ID;
        if (GAUTAMA_FINAL_HULL_ID.equals(member.getHullId())) {
            variantId = GAUTAMA_FINAL_VARIANT_ID;
        } else if (GAUTAMA_REINCARNATING_HULL_ID.equals(
                member.getHullId())) {
            variantId = GAUTAMA_REINCARNATING_VARIANT_ID;
        }
        try {
            return Global.getSettings().getVariant(variantId);
        } catch (RuntimeException failure) {
            Global.getLogger(OdysseyPredatorScript.class).error(
                    "Could not resolve canonical Gautama variant "
                            + variantId + "; leaving serialized armament intact",
                    failure);
            return null;
        }
    }

    /** Resolves only Chief Navigator's exact authored Starving variants. */
    private static ShipVariantAPI canonicalStarvingVariant(
            FleetMemberAPI member) {
        if (member == null || member.getVariant() == null
                || Global.getSettings() == null) {
            return null;
        }
        String variantId = member.getVariant().getHullVariantId();
        if (variantId == null
                || !variantId.startsWith("chief_navigator_starving_")) {
            return null;
        }
        try {
            ShipVariantAPI canonical = Global.getSettings().getVariant(variantId);
            if (canonical == null || canonical.getHullSpec() == null
                    || !member.getHullId().equals(
                            canonical.getHullSpec().getHullId())) {
                return null;
            }
            return canonical;
        } catch (RuntimeException failure) {
            Global.getLogger(OdysseyPredatorScript.class).error(
                    "Could not resolve canonical Starving variant "
                            + variantId + "; leaving serialized armament intact",
                    failure);
            return null;
        }
    }

    private static boolean armamentMatches(
            ShipVariantAPI current, ShipVariantAPI canonical) {
        if (current == null || canonical == null
                || current.getFittedWeaponSlots().size()
                        != canonical.getFittedWeaponSlots().size()) {
            return false;
        }
        for (String slotId : canonical.getFittedWeaponSlots()) {
            String expected = canonical.getWeaponId(slotId);
            if (expected == null
                    || !expected.equals(current.getWeaponId(slotId))) {
                return false;
            }
        }
        return true;
    }

    /** Copies weapons/groups only, preserving CR and unrelated saved hullmods. */
    private static boolean copyArmament(
            ShipVariantAPI target, ShipVariantAPI canonical) {
        if (target == null || canonical == null) return false;
        for (String slotId : canonical.getFittedWeaponSlots()) {
            if (target.getSlot(slotId) == null) {
                Global.getLogger(OdysseyPredatorScript.class).error(
                        "Could not copy authored Starving armament: target "
                                + "hull lacks slot " + slotId);
                return false;
            }
        }
        try {
            for (String slotId
                    : new ArrayList<String>(target.getFittedWeaponSlots())) {
                target.clearSlot(slotId);
            }
            for (String slotId : canonical.getFittedWeaponSlots()) {
                target.addWeapon(slotId, canonical.getWeaponId(slotId));
            }
            target.getWeaponGroups().clear();
            for (WeaponGroupSpec group : canonical.getWeaponGroups()) {
                target.addWeaponGroup(group.clone());
            }
            target.setMayAutoAssignWeapons(false);
            return true;
        } catch (RuntimeException failure) {
            Global.getLogger(OdysseyPredatorScript.class).error(
                    "Could not copy authored Starving armament; leaving "
                            + "the serialized variant intact",
                    failure);
            return false;
        }
    }

    private static String getStarvingThreatFactionId(CampaignFleetAPI fleet) {
        String id = fleet.getId();
        LocationAPI location = fleet.getContainingLocation();
        boolean ashen = id != null && id.startsWith(ASHEN_STARVING_PREFIX);
        if (!ashen && location instanceof StarSystemAPI) {
            ashen = location == OdysseyExpanseSystem.findSystemById(
                    OdysseyExpanseSystem.ASHEN_VERGE_ID);
        }
        return ashen ? IthacaThreatPolicy.FACTION_ID : Factions.THREAT;
    }

    private static void normalizeStarvingThreatPerson(
            PersonAPI person, String factionId) {
        if (person == null || person.isDefault()) return;
        person.setFaction(factionId);
        String portrait = person.getPortraitSprite();
        if (portrait == null || portrait.endsWith("/omega.png")) {
            person.setPortraitSprite("graphics/portraits/threat.png");
        }
        if (Commodities.OMEGA_CORE.equals(person.getAICoreId())) {
            person.setAICoreId(null);
        }
    }

    private static void repairStarvingThreatIdentities() {
        java.util.Set<CampaignFleetAPI> seen =
                java.util.Collections.newSetFromMap(
                        new java.util.IdentityHashMap<CampaignFleetAPI,
                                Boolean>());
        for (LocationAPI location : getCampaignLocationsIncludingHyperspace()) {
            for (CampaignFleetAPI fleet : new ArrayList<CampaignFleetAPI>(
                    location.getFleets())) {
                if (seen.add(fleet)
                        && isAuthoredStarvingThreatFleet(fleet)) {
                    if (isGautamaFleet(fleet)
                            && MenelausTrial.isComplete()
                            && TroyArrivalScript
                                    .isFleetBusyForMutation(fleet)) {
                        continue;
                    }
                    configureStarvingThreatIdentity(fleet);
                }
            }
            for (SectorEntityToken entity : new ArrayList<SectorEntityToken>(
                    location.getAllEntities())) {
                if (!(entity instanceof CampaignFleetAPI)) continue;
                CampaignFleetAPI fleet = (CampaignFleetAPI) entity;
                if (seen.add(fleet)
                        && isAuthoredStarvingThreatFleet(fleet)) {
                    if (isGautamaFleet(fleet)
                            && MenelausTrial.isComplete()
                            && TroyArrivalScript
                                    .isFleetBusyForMutation(fleet)) {
                        continue;
                    }
                    configureStarvingThreatIdentity(fleet);
                }
            }
        }
    }

    private static boolean isGautamaFleet(CampaignFleetAPI fleet) {
        if (fleet == null) return false;
        if (TroyArrivalScript.STARVING_THREAT_FLEET_ID.equals(
                fleet.getId())) {
            return true;
        }
        for (FleetMemberAPI member
                : fleet.getFleetData().getMembersListCopy()) {
            if (isGautamaMember(member)) {
                return true;
            }
        }
        return false;
    }

    /** True only while a real Gautama campaign member remains in the fleet. */
    private static boolean hasGautamaMember(CampaignFleetAPI fleet) {
        if (fleet == null) return false;
        for (FleetMemberAPI member
                : fleet.getFleetData().getMembersListCopy()) {
            if (isGautamaMember(member)) return true;
        }
        return false;
    }

    /** True only while the real Ungaikyo campaign member remains. */
    private static boolean hasUngaikyoMember(CampaignFleetAPI fleet) {
        if (fleet == null) return false;
        for (FleetMemberAPI member
                : fleet.getFleetData().getMembersListCopy()) {
            if (member == null) continue;
            if (UNGAIKYO_HULL_ID.equals(member.getHullId())) return true;
            ShipVariantAPI variant = member.getVariant();
            if (variant != null && UNGAIKYO_VARIANT_ID.equals(
                    variant.getHullVariantId())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGautamaMember(FleetMemberAPI member) {
        if (member == null) return false;
        String hullId = member.getHullId();
        if (GAUTAMA_HULL_ID.equals(hullId)
                || GAUTAMA_FINAL_HULL_ID.equals(hullId)
                || GAUTAMA_REINCARNATING_HULL_ID.equals(hullId)) {
            return true;
        }
        ShipVariantAPI variant = member.getVariant();
        return variant != null
                && GAUTAMA_FINAL_VARIANT_ID.equals(
                        variant.getHullVariantId());
    }

    private static void removeEntity(LocationAPI location, String id) {
        if (location == null || id == null) return;
        SectorEntityToken entity = location.getEntityById(id);
        if (entity != null) location.removeEntity(entity);
    }

    private static void markOwnedProxyToken(SectorEntityToken token) {
        if (token != null) {
            token.getMemoryWithoutUpdate().set(
                    OWNED_PROXY_TOKEN_MARKER, true);
        }
    }

    private static boolean isOwnedProxyToken(SectorEntityToken token) {
        return token != null
                && token.getMemoryWithoutUpdate().getBoolean(
                        OWNED_PROXY_TOKEN_MARKER);
    }

    private static void logBlockedProxyTokenOnce(
            String id, SectorEntityToken claimant) {
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        String key = BLOCKED_PROXY_TOKEN_LOG_PREFIX + id;
        if (memory.getBoolean(key)) return;
        memory.set(key, true);
        Global.getLogger(OdysseyPredatorScript.class).error(
                "Temporary campaign token ID " + id
                        + " is occupied by incompatible serialized state ("
                        + claimant.getClass().getName()
                        + "); refusing mutation or replacement");
    }

    /** Keeps sparse, one-time recoverable rogue machines in the Odyssey Sector. */
    private static void ensureRogueDerelictPopulation() {
        MemoryAPI sectorMemory = Global.getSector().getMemoryWithoutUpdate();
        for (int index = 0; index < ROGUE_DERELICT_COUNT; index++) {
            StarSystemAPI system = OdysseyExpanseSystem.findSystemById(
                    ROGUE_SYSTEM_IDS[index]);
            if (system == null) continue;

            String id = ROGUE_DERELICT_PREFIX + index;
            String spawnedKey = ROGUE_DERELICT_SPAWNED_PREFIX + index;
            String migratedKey = ROGUE_DERELICT_LAYOUT_MIGRATED_PREFIX + index;
            if (OdysseyExpanseSystem.SILENT_WAKE_ID.equals(
                    ROGUE_SYSTEM_IDS[index])) {
                // Retired layouts are not pruned from loaded campaigns.
                sectorMemory.set(spawnedKey, true);
                sectorMemory.set(migratedKey, true);
                continue;
            }
            SectorEntityToken existing = system.getEntityById(id);
            if (existing instanceof CampaignFleetAPI) {
                CampaignFleetAPI claimedFleet = (CampaignFleetAPI) existing;
                if (isPlayerControlledFleet(claimedFleet)
                        || !claimedFleet.getMemoryWithoutUpdate().getBoolean(
                                ROGUE_DERELICT_MARKER)) {
                    sectorMemory.set(spawnedKey, true);
                    continue;
                }
            }
            if (existing instanceof CampaignFleetAPI
                    && TroyArrivalScript.isFleetBusyForMutation(
                            (CampaignFleetAPI) existing)) {
                continue;
            }
            if (existing instanceof CampaignFleetAPI
                    && !((CampaignFleetAPI) existing).isEmpty()) {
                CampaignFleetAPI fleet = (CampaignFleetAPI) existing;
                sectorMemory.set(spawnedKey, true);
                configureRogueDerelictFleet(fleet, system);
                continue;
            }
            if (existing instanceof CampaignFleetAPI) {
                // An empty authored fleet is a defeated encounter. Commit the
                // one-shot flag and leave cleanup to Starsector.
                sectorMemory.set(spawnedKey, true);
                continue;
            }
            if (existing != null) {
                // A wrong-type claim on the authored ID is authoritative.
                // Suppress creation rather than deleting serialized state.
                sectorMemory.set(spawnedKey, true);
                continue;
            }

            SectorEntityToken legacyAnchor = system.getEntityById(
                    ROGUE_DERELICT_ANCHOR_PREFIX + index);
            if (legacyAnchor != null) {
                // Older builds created this anchor with the fleet. If only the
                // anchor remains, the fleet was already defeated; do not
                // manufacture another one while migrating the save.
                sectorMemory.set(spawnedKey, true);
                sectorMemory.set(migratedKey, true);
            }
            if (sectorMemory.getBoolean(spawnedKey)) continue;

            Vector2f spawn = getRogueDerelictAnchorLocation(system, index);
            CampaignFleetAPI player = Global.getSector().getPlayerFleet();
            if (player != null && player.getContainingLocation() == system
                    && Misc.getDistance(player.getLocation(), spawn)
                            <= ROGUE_DERELICT_PLAYER_SPAWN_CLEARANCE) {
                // Defer first creation until the player has left the site;
                // never materialize a hostile patrol on top of them.
                continue;
            }
            spawnRogueDerelictFleet(system, index);
            sectorMemory.set(spawnedKey, true);
            sectorMemory.set(migratedKey, true);
        }
    }

    /**
     * Clears stale pursuit assignments before the Troy transition completes.
     * The recurring population update keeps the same spatial protection in
     * place while the player remains close to Foxtrot Terminus.
     */
    public static void prepareTroyArrival(StarSystemAPI system) {
        if (system == null || system != OdysseyExpanseSystem.findExisting()) {
            return;
        }
        SectorEntityToken entry = OdysseyExpanseSystem.findEntry(system);
        if (entry == null) return;
        Global.getSector().getMemoryWithoutUpdate().set(
                TROY_TRANSIT_PENDING, true, 0.25f);

        for (int index = 0; index < ALPHA_STARVING_PATROL_COUNT; index++) {
            SectorEntityToken token = system.getEntityById(
                    ALPHA_STARVING_PATROL_PREFIX + index);
            if (token instanceof CampaignFleetAPI) {
                protectAlphaStarvingArrival((CampaignFleetAPI) token, system);
            }
        }

        for (int index = 0; index < ROGUE_DERELICT_COUNT; index++) {
            if (!OdysseyExpanseSystem.SYSTEM_ID.equals(
                    ROGUE_SYSTEM_IDS[index])) continue;
            SectorEntityToken token = system.getEntityById(
                    ROGUE_DERELICT_PREFIX + index);
            if (!(token instanceof CampaignFleetAPI)) continue;
            CampaignFleetAPI fleet = (CampaignFleetAPI) token;
            if (!fleet.getMemoryWithoutUpdate().getBoolean(
                        ROGUE_DERELICT_MARKER)
                    || TroyArrivalScript.isFleetBusyForMutation(fleet)) {
                continue;
            }
            if (Misc.getDistance(fleet, entry) < TROY_ARRIVAL_CLEAR_RADIUS) {
                Vector2f anchor = getRogueDerelictAnchorLocation(
                        system, index);
                fleet.setLocation(anchor.x, anchor.y);
            }
            fleet.clearAssignments();
            applyTroyArrivalProtection(fleet);
            assignRogueDerelictAnchor(fleet, system, index, true);
        }
    }

    private static void spawnRogueDerelictFleet(
            StarSystemAPI system, int index) {
        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(
                IthacaSectionEncounter.COMBAT_GUARD_FACTION_ID,
                "Rogue Combat Guard Flotilla", true);
        fleet.setId(ROGUE_DERELICT_PREFIX + index);
        fleet.setName("Rogue Combat Guard Flotilla");
        fleet.setNoFactionInName(true);
        fleet.setTransponderOn(false);
        fleet.getMemoryWithoutUpdate().set(ROGUE_DERELICT_MARKER, true);
        fleet.getMemoryWithoutUpdate().set(
                AUTHORED_MOBILE_DERELICT_MARKER, true);

        String[] loadout = ROGUE_DERELICT_LOADOUTS[
                index % ROGUE_DERELICT_LOADOUTS.length];
        for (String variantId : loadout) addMember(fleet, variantId);
        if (!fleet.getFleetData().getMembersListCopy().isEmpty()) {
            fleet.getFleetData().setFlagship(
                    fleet.getFleetData().getMembersListCopy().get(0));
        }
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();

        system.addEntity(fleet);
        Vector2f anchor = getRogueDerelictAnchorLocation(system, index);
        fleet.setLocation(anchor.x, anchor.y);
        configureRogueDerelictFleet(fleet, system);
    }

    private static void configureRogueDerelictFleet(
            CampaignFleetAPI fleet, StarSystemAPI system) {
        if (TroyArrivalScript.isFleetBusyForMutation(fleet)) return;
        fleet.setFaction(IthacaSectionEncounter.COMBAT_GUARD_FACTION_ID, true);
        fleet.setName("Rogue Combat Guard Flotilla");
        MenelausTrial.convertFleetToCombatGuard(fleet);
        int index = getRogueDerelictIndex(fleet);
        boolean arrivalProtected = isTroyArrivalProtected(system);
        if (arrivalProtected) {
            applyTroyArrivalProtection(fleet);
        } else {
            clearTroyArrivalProtection(fleet);
            configureHostileFleet(fleet);
        }
        // This is the deliberate exception to the boss/predator population:
        // disabled rogue hulls must appear in the normal recovery screen.
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_NO_JUMP, true);
        if (index >= 0) {
            assignRogueDerelictAnchor(
                    fleet, system, index, arrivalProtected);
        }
    }

    private static boolean isTroyArrivalProtected(StarSystemAPI system) {
        if (system == null || system != OdysseyExpanseSystem.findExisting()) {
            return false;
        }
        if (Global.getSector().getMemoryWithoutUpdate().getBoolean(
                TROY_TRANSIT_PENDING)) return true;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player == null || player.getContainingLocation() != system) {
            return false;
        }
        SectorEntityToken entry = OdysseyExpanseSystem.findEntry(system);
        return entry != null
                && Misc.getDistance(player, entry) <= TROY_ARRIVAL_SAFE_RADIUS;
    }

    private static void applyTroyArrivalProtection(CampaignFleetAPI fleet) {
        fleet.setNoEngaging(1f);
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_MAKE_HOSTILE);
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE, true);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.NON_HOSTILE_OVERRIDES_MAKE_HOSTILE, true);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE, true);
    }

    private static void clearTroyArrivalProtection(CampaignFleetAPI fleet) {
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE);
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.NON_HOSTILE_OVERRIDES_MAKE_HOSTILE);
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE);
    }

    /** The Foxtrot entrance is not an automatic Starving Threat encounter. */
    static boolean protectAlphaStarvingArrival(
            CampaignFleetAPI fleet, StarSystemAPI system) {
        if (fleet == null || isPlayerControlledFleet(fleet)
                || fleet.getContainingLocation() != system
                || fleet.isEmpty() || fleet.isStationMode()
                || TroyArrivalScript.isFleetBusyForMutation(fleet)
                || !fleet.getMemoryWithoutUpdate().getBoolean(REGIONAL_SMALL_PATROL_MARKER)
                || !isTroyArrivalProtected(system)) {
            return false;
        }
        boolean ownedSlot = false;
        for (int index = 0; index < ALPHA_STARVING_PATROL_COUNT; index++) {
            if ((ALPHA_STARVING_PATROL_PREFIX + index).equals(fleet.getId())) {
                ownedSlot = true;
                break;
            }
        }
        if (!ownedSlot) return false;
        applyTroyArrivalProtection(fleet);
        fleet.getMemoryWithoutUpdate().unset(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE);
        fleet.getMemoryWithoutUpdate().unset(MemFlags.MEMORY_KEY_PURSUE_PLAYER);
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (fleet.getAI() instanceof ModularFleetAIAPI) {
            TacticalModulePlugin tactical =
                    ((ModularFleetAIAPI) fleet.getAI()).getTacticalModule();
            if (tactical != null && player != null) {
                if (tactical.getTarget() == player) tactical.setTarget(null);
                if (tactical.getPriorityTarget() == player) {
                    tactical.setPriorityTarget(null, 0f, false);
                }
            }
        }
        if (fleet.getCurrentAssignment() != null && player != null
                && fleet.getCurrentAssignment().getTarget() == player) {
            fleet.clearAssignments();
        }
        if (fleet.getCurrentAssignment() == null) {
            fleet.addAssignment(FleetAssignment.PATROL_SYSTEM,
                    system.getCenter(), 1000000f, "patrolling the Odyssey Expanse");
        }
        return true;
    }

    private static void assignRogueDerelictAnchor(
            CampaignFleetAPI fleet,
            StarSystemAPI system,
            int index,
            boolean passive) {
        String id = ROGUE_DERELICT_ANCHOR_PREFIX + index;
        SectorEntityToken anchor = system.getEntityById(id);
        if (anchor == null) {
            anchor = system.createToken(0f, 0f);
            anchor.setId(id);
            markOwnedProxyToken(anchor);
        } else if (!isOwnedProxyToken(anchor)) {
            logBlockedProxyTokenOnce(id, anchor);
            return;
        }
        Vector2f safeLocation = getRogueDerelictAnchorLocation(
                system, index);
        anchor.setLocation(safeLocation.x, safeLocation.y);
        FleetAssignment desired = passive
                ? FleetAssignment.ORBIT_PASSIVE
                : FleetAssignment.ORBIT_AGGRESSIVE;
        if (fleet.getCurrentAssignment() == null
                || fleet.getCurrentAssignment().getAssignment() != desired
                || fleet.getCurrentAssignment().getTarget() != anchor) {
            fleet.clearAssignments();
            fleet.addAssignment(
                    desired,
                    anchor,
                    1000000f,
                    passive
                            ? "holding clear of Foxtrot Terminus"
                            : "roaming without a command signal");
        }
    }

    private static Vector2f getRogueDerelictAnchorLocation(
            StarSystemAPI system, int index) {
        return projectOutsideJumpClearance(
                system,
                new Vector2f(
                        ROGUE_DERELICT_OFFSETS[index][0],
                        ROGUE_DERELICT_OFFSETS[index][1]),
                DERELICT_ANCHOR_CLEARANCE);
    }

    /** Finds the nearest deterministic point outside every jump bubble. */
    static Vector2f projectOutsideJumpClearance(
            StarSystemAPI system,
            Vector2f point,
            float clearance) {
        Vector2f result = point == null
                ? new Vector2f() : new Vector2f(point);
        if (system == null || clearance <= 0f
                || system.getJumpPoints() == null
                || !Float.isFinite(result.x)
                || !Float.isFinite(result.y)) {
            return result;
        }

        ArrayList<SectorEntityToken> jumps =
                new ArrayList<SectorEntityToken>();
        for (SectorEntityToken jump : system.getJumpPoints()) {
            if (jump == null || jump.getLocation() == null
                    || !Float.isFinite(jump.getLocation().x)
                    || !Float.isFinite(jump.getLocation().y)) {
                continue;
            }
            jumps.add(jump);
        }
        if (jumps.isEmpty()
                || isOutsideAllJumpClearances(result, jumps, clearance)) {
            return result;
        }

        // The closest point outside a union of equal circles lies either on a
        // smooth radial boundary or at the intersection of two boundaries.
        // Evaluate both finite candidate sets so a live patrol is moved only
        // as far as required, even where several arrival bubbles overlap.
        float safeRadius = clearance + 16f;
        Vector2f best = null;
        for (SectorEntityToken jump : jumps) {
            Vector2f center = jump.getLocation();
            float dx = result.x - center.x;
            float dy = result.y - center.y;
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            if (length < 0.001f) {
                int hash = jump.getId() == null
                        ? 0 : jump.getId().hashCode();
                double angle = (hash & 0xffff)
                        * Math.PI * 2d / 65536d;
                dx = (float) Math.cos(angle);
                dy = (float) Math.sin(angle);
                length = 1f;
            }
            Vector2f candidate = new Vector2f(
                    center.x + dx * safeRadius / length,
                    center.y + dy * safeRadius / length);
            best = chooseCloserSafeJumpCandidate(
                    result, candidate, best, jumps, clearance);
        }

        for (int first = 0; first < jumps.size(); first++) {
            Vector2f a = jumps.get(first).getLocation();
            for (int second = first + 1;
                    second < jumps.size(); second++) {
                Vector2f b = jumps.get(second).getLocation();
                float dx = b.x - a.x;
                float dy = b.y - a.y;
                float distanceSquared = dx * dx + dy * dy;
                if (distanceSquared < 0.0001f) continue;
                float distance = (float) Math.sqrt(distanceSquared);
                if (distance > safeRadius * 2f) continue;

                float midpointX = (a.x + b.x) * 0.5f;
                float midpointY = (a.y + b.y) * 0.5f;
                float heightSquared = safeRadius * safeRadius
                        - distanceSquared * 0.25f;
                if (heightSquared < 0f) heightSquared = 0f;
                float height = (float) Math.sqrt(heightSquared);
                float perpendicularX = -dy / distance;
                float perpendicularY = dx / distance;

                Vector2f candidate = new Vector2f(
                        midpointX + perpendicularX * height,
                        midpointY + perpendicularY * height);
                best = chooseCloserSafeJumpCandidate(
                        result, candidate, best, jumps, clearance);
                candidate = new Vector2f(
                        midpointX - perpendicularX * height,
                        midpointY - perpendicularY * height);
                best = chooseCloserSafeJumpCandidate(
                        result, candidate, best, jumps, clearance);
            }
        }
        if (best != null) return best;

        // Numerical fallback: moving farther than the most distant center plus
        // the clearance guarantees a safe result by reverse triangle inequality.
        float centerX = 0f;
        float centerY = 0f;
        float maxOriginDistance = 0f;
        int directionHash = 1;
        for (SectorEntityToken jump : jumps) {
            Vector2f location = jump.getLocation();
            float originDx = result.x - location.x;
            float originDy = result.y - location.y;
            maxOriginDistance = Math.max(
                    maxOriginDistance,
                    (float) Math.sqrt(
                            originDx * originDx + originDy * originDy));
            centerX += location.x;
            centerY += location.y;
            directionHash = 31 * directionHash
                    + (jump.getId() == null ? 0 : jump.getId().hashCode());
        }
        centerX /= jumps.size();
        centerY /= jumps.size();
        float dx = result.x - centerX;
        float dy = result.y - centerY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.001f) {
            double angle = (directionHash & 0xffff)
                    * Math.PI * 2d / 65536d;
            dx = (float) Math.cos(angle);
            dy = (float) Math.sin(angle);
            length = 1f;
        }
        float travel = maxOriginDistance + safeRadius;
        return new Vector2f(
                result.x + dx * travel / length,
                result.y + dy * travel / length);
    }

    private static Vector2f chooseCloserSafeJumpCandidate(
            Vector2f origin,
            Vector2f candidate,
            Vector2f currentBest,
            ArrayList<SectorEntityToken> jumps,
            float clearance) {
        if (!isOutsideAllJumpClearances(candidate, jumps, clearance)) {
            return currentBest;
        }
        float candidateDx = candidate.x - origin.x;
        float candidateDy = candidate.y - origin.y;
        float candidateDistance = candidateDx * candidateDx
                + candidateDy * candidateDy;
        if (currentBest == null) return candidate;
        float bestDx = currentBest.x - origin.x;
        float bestDy = currentBest.y - origin.y;
        float bestDistance = bestDx * bestDx + bestDy * bestDy;
        return candidateDistance < bestDistance ? candidate : currentBest;
    }

    private static boolean isOutsideAllJumpClearances(
            Vector2f point,
            ArrayList<SectorEntityToken> jumps,
            float clearance) {
        if (point == null || !Float.isFinite(point.x)
                || !Float.isFinite(point.y)) {
            return false;
        }
        float clearanceSquared = clearance * clearance;
        for (SectorEntityToken jump : jumps) {
            Vector2f location = jump.getLocation();
            float dx = point.x - location.x;
            float dy = point.y - location.y;
            if (dx * dx + dy * dy < clearanceSquared) return false;
        }
        return true;
    }

    /** Clears occupied arrival bubbles every frame without replacing fleets. */
    private static void maintainDerelictJumpArrivalClearance() {
        if (Global.getSector() == null) return;
        for (String systemId : DERELICT_PATROL_SYSTEM_IDS) {
            StarSystemAPI system = OdysseyExpanseSystem.findSystemById(
                    systemId);
            if (system == null) continue;
            for (CampaignFleetAPI fleet : system.getFleets()) {
                if (isAuthoredMobileDerelict(fleet)) {
                    clearDerelictJumpArrivalZone(fleet, system);
                }
            }
        }
    }

    /** Keeps Devoured Reach's sole arrival point free of feeding masses. */
    private static void maintainDevouredJumpArrivalClearance() {
        StarSystemAPI system = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.DEVOURED_REACH_ID);
        if (system == null) return;
        for (CampaignFleetAPI fleet : system.getFleets()) {
            if (isDevouredFeedingMass(fleet)) {
                clearJumpArrivalZone(fleet, system);
            }
        }
    }

    /** Refreshes native nav repulsion without touching patrol/pursuit orders. */
    private static void maintainDerelictJumpPointClearance() {
        if (Global.getSector() == null) return;
        for (String systemId : DERELICT_PATROL_SYSTEM_IDS) {
            StarSystemAPI system = OdysseyExpanseSystem.findSystemById(
                    systemId);
            if (system == null) continue;
            for (CampaignFleetAPI fleet : new ArrayList<CampaignFleetAPI>(
                    system.getFleets())) {
                if (isAuthoredMobileDerelict(fleet)) {
                    refreshDerelictJumpAvoidance(fleet, system);
                }
            }
        }
    }

    /** Refreshes native navigation repulsion around the sole threshold. */
    private static void maintainDevouredJumpPointClearance() {
        StarSystemAPI system = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.DEVOURED_REACH_ID);
        if (system == null) return;
        for (CampaignFleetAPI fleet : new ArrayList<CampaignFleetAPI>(
                system.getFleets())) {
            if (isDevouredFeedingMass(fleet)) {
                refreshJumpAvoidance(fleet, system);
            }
        }
    }

    private static boolean isDevouredFeedingMass(CampaignFleetAPI fleet) {
        return fleet != null && !isPlayerControlledFleet(fleet)
                && fleet.getId() != null
                && fleet.getId().startsWith(DEVOURED_STARVING_PREFIX)
                && fleet.getMemoryWithoutUpdate().getBoolean(
                        STARVING_THREAT_MARKER);
    }

    static boolean isAuthoredMobileDerelict(CampaignFleetAPI fleet) {
        if (fleet == null || isPlayerControlledFleet(fleet)
                || fleet.getId() == null
                || !fleet.getMemoryWithoutUpdate().getBoolean(
                        AUTHORED_MOBILE_DERELICT_MARKER)) {
            return false;
        }
        for (String prefix : AUTHORED_DERELICT_PATROL_PREFIXES) {
            if (fleet.getId().startsWith(prefix)) return true;
        }
        return false;
    }

    /**
     * Immediately moves an intact patrol out of an arrival bubble. This is a
     * position correction only: the serialized fleet, roster, assignments,
     * targets, and velocity all remain intact. Once clear, native navigation
     * avoidance keeps the patrol from drifting back toward the jump point.
     */
    static boolean clearDerelictJumpArrivalZone(
            CampaignFleetAPI fleet,
            StarSystemAPI system) {
        if (!isAuthoredMobileDerelict(fleet)) return false;
        return clearJumpArrivalZone(fleet, system);
    }

    private static boolean clearJumpArrivalZone(
            CampaignFleetAPI fleet,
            StarSystemAPI system) {
        if (system == null
                || fleet.getContainingLocation() != system
                || isPlayerControlledFleet(fleet)
                || TroyArrivalScript.isFleetBusyForMutation(fleet)
                || fleet.isEmpty() || fleet.isDespawning()
                || fleet.isStationMode()
                || fleet.isInHyperspaceTransition()
                || fleet.getBattle() != null
                || fleet.getOrbit() != null
                || fleet.getLocation() == null
                || system.getJumpPoints() == null
                || system.getJumpPoints().isEmpty()) {
            return false;
        }

        Vector2f current = fleet.getLocation();
        if (!Float.isFinite(current.x) || !Float.isFinite(current.y)) {
            return false;
        }
        boolean insideDangerZone = false;
        float triggerSquared = DERELICT_JUMP_ARRIVAL_TRIGGER
                * DERELICT_JUMP_ARRIVAL_TRIGGER;
        for (SectorEntityToken jump : system.getJumpPoints()) {
            if (jump == null || jump.getLocation() == null
                    || !Float.isFinite(jump.getLocation().x)
                    || !Float.isFinite(jump.getLocation().y)) {
                continue;
            }
            float dx = current.x - jump.getLocation().x;
            float dy = current.y - jump.getLocation().y;
            if (dx * dx + dy * dy < triggerSquared) {
                insideDangerZone = true;
                break;
            }
        }
        if (!insideDangerZone) return false;

        if (Global.getSector() != null
                && Global.getSector().getCampaignUI() != null
                && Global.getSector().getCampaignUI()
                        .getCurrentInteractionDialog() != null
                && Global.getSector().getCampaignUI()
                        .getCurrentInteractionDialog()
                        .getInteractionTarget() == fleet) {
            return false;
        }

        Vector2f safe = projectOutsideJumpClearance(
                system,
                current,
                DERELICT_JUMP_ARRIVAL_CLEARANCE);
        float dx = safe.x - current.x;
        float dy = safe.y - current.y;
        if (dx * dx + dy * dy < 0.25f) return false;

        fleet.setLocation(safe.x, safe.y);
        return true;
    }

    static void refreshDerelictJumpAvoidance(
            CampaignFleetAPI fleet,
            StarSystemAPI system) {
        if (!isAuthoredMobileDerelict(fleet)) return;
        refreshJumpAvoidance(fleet, system);
    }

    private static void refreshJumpAvoidance(
            CampaignFleetAPI fleet,
            StarSystemAPI system) {
        if (fleet == null || system == null
                || TroyArrivalScript.isFleetBusyForMutation(fleet)
                || fleet.getContainingLocation() != system
                || fleet.isEmpty() || fleet.isDespawning()
                || fleet.isStationMode()
                || fleet.isInHyperspaceTransition()
                || fleet.getBattle() != null
                || !(fleet.getAI() instanceof ModularFleetAIAPI)
                || system.getJumpPoints() == null) {
            return;
        }
        NavigationModulePlugin navigation =
                ((ModularFleetAIAPI) fleet.getAI()).getNavModule();
        if (navigation == null) return;
        for (SectorEntityToken jump : system.getJumpPoints()) {
            if (jump != null) {
                navigation.avoidEntity(
                        jump,
                        DERELICT_JUMP_AVOIDANCE_MIN,
                        DERELICT_JUMP_AVOIDANCE_MAX,
                        DERELICT_JUMP_AVOIDANCE_DURATION);
            }
        }
    }

    private static void ensureStarvingPopulation() {
        ensureRegionalStarvingPatrols(
                OdysseyExpanseSystem.findSystemById(
                        OdysseyExpanseSystem.SYSTEM_ID),
                ALPHA_STARVING_PATROL_PREFIX,
                ALPHA_STARVING_PATROL_COUNT,
                ALPHA_STARVING_PATROL_OFFSETS,
                "hunting through the Odyssey Expanse");
        retireAviciStarvingThreat(
                OdysseyExpanseSystem.findSystemById(
                        OdysseyExpanseSystem.MESSINA_ID));
        ensureRegionalStarvingPatrols(
                OdysseyExpanseSystem.findSystemById(
                        OdysseyExpanseSystem.SILENT_WAKE_ID),
                SANZU_STARVING_PATROL_PREFIX,
                SANZU_STARVING_PATROL_COUNT,
                SANZU_STARVING_PATROL_OFFSETS,
                "hunting through Sanzu");

        StarSystemAPI ashen = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.ASHEN_VERGE_ID);
        ensureStarvingPopulation(
                ashen,
                ASHEN_STARVING_PREFIX,
                ASHEN_STARVING_PATROL_COUNT,
                ASHEN_STARVING_OFFSETS,
                "hunting through Ashen Verge");

        StarSystemAPI devoured = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.DEVOURED_REACH_ID);
        ensureStarvingPopulation(
                devoured,
                DEVOURED_STARVING_PREFIX,
                DEVOURED_STARVING_COUNT,
                DEVOURED_STARVING_OFFSETS,
                "feeding on the Devoured Ring");
    }

    /** Retires Avici's local Starving population without touching story bosses. */
    static void retireAviciStarvingThreat(StarSystemAPI system) {
        if (system == null
                || !OdysseyExpanseSystem.MESSINA_ID.equals(system.getId())) return;
        for (CampaignFleetAPI fleet
                : new ArrayList<CampaignFleetAPI>(system.getFleets())) {
            if (!isAuthoredStarvingThreatFleet(fleet)
                    || fleet.getContainingLocation() != system
                    // The story controller relocates this exact boss fleet
                    // to Devoured Reach/Last Light; never delete its objective.
                    || TroyArrivalScript.STARVING_THREAT_FLEET_ID.equals(fleet.getId())
                    || TroyArrivalScript.CHARYBDIS_FLEET_ID.equals(fleet.getId())
                    || fleet.getMemoryWithoutUpdate().getBoolean(BUDAI_HUNTER_MARKER)
                    || TroyArrivalScript.isFleetBusyForMutation(fleet)) continue;
            system.removeEntity(fleet);
        }
    }

    /**
     * Sends one materialized-from-nowhere First or Second Strike against an
     * Ithaca wall bastion roughly once per campaign month. The fleet exists
     * only for a bounded attack window so a surviving raid cannot block all
     * later deployments.
     */
    private static void ensureMonthlyIthacaStrike() {
        StarSystemAPI ashen = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.ASHEN_VERGE_ID);
        MemoryAPI sectorMemory = Global.getSector().getMemoryWithoutUpdate();
        if (ashen == null) {
            if (!sectorMemory.getBoolean(
                    ITHACA_STRIKE_TRACE_MISSING_SYSTEM)) {
                logIthacaStrikeWarning("Monthly check stopped: Ashen Verge "
                        + "system was not found.");
                sectorMemory.set(ITHACA_STRIKE_TRACE_MISSING_SYSTEM, true);
            }
            return;
        }
        sectorMemory.unset(ITHACA_STRIKE_TRACE_MISSING_SYSTEM);
        SectorEntityToken center = ashen.getEntityById(
                IthacaSectionEncounter.FOB_ENTITY_ID);
        if (!OdysseyExpanseSystem.isFobIthaca(center)) {
            if (!ashen.getMemoryWithoutUpdate().getBoolean(
                    ITHACA_STRIKE_TRACE_MISSING_CENTER)) {
                logIthacaStrikeWarning("Monthly check stopped: FOB Ithaca "
                        + "controller " + IthacaSectionEncounter.FOB_ENTITY_ID
                        + " was not found in Ashen Verge.");
                ashen.getMemoryWithoutUpdate().set(
                        ITHACA_STRIKE_TRACE_MISSING_CENTER, true);
            }
            return;
        }
        ashen.getMemoryWithoutUpdate().unset(
                ITHACA_STRIKE_TRACE_MISSING_CENTER);
        maintainConsoleIthacaStrikes(ashen, center);

        MemoryAPI systemMemory = ashen.getMemoryWithoutUpdate();
        SectorEntityToken existing = ashen.getEntityById(
                ITHACA_MONTHLY_STRIKE_ID);
        CampaignFleetAPI fleet = existing instanceof CampaignFleetAPI
                ? (CampaignFleetAPI) existing : null;
        if (fleet != null && !isAuthoredStarvingThreatFleet(fleet)) {
            logIthacaStrikeWarning("Monthly strike ID is occupied by an "
                    + "unowned fleet; preserving serialized state.");
            return;
        }
        if (fleet != null
                && TroyArrivalScript.isFleetBusyForMutation(fleet)) {
            return;
        }
        if (fleet != null && !fleet.isEmpty() && !fleet.isDespawning()) {
            systemMemory.set(ITHACA_MONTHLY_STRIKE_INITIALIZED, true);
            if (fleet.getBattle() == null && !fleet.getMemoryWithoutUpdate()
                    .contains(ITHACA_MONTHLY_STRIKE_ACTIVE)) {
                logIthacaStrike("Removing expired monthly fleet id="
                        + fleet.getId() + ", members="
                        + fleet.getFleetData().getMembersListCopy().size()
                        + ".");
                ashen.removeEntity(fleet);
                return;
            }
            configureMonthlyIthacaStrike(fleet, ashen, center);
            return;
        }
        if (existing != null) {
            logIthacaStrikeWarning("Invalid monthly strike entity id="
                    + existing.getId() + ", class="
                    + existing.getClass().getName()
                    + "; preserving serialized state.");
            return;
        }

        if (!systemMemory.getBoolean(ITHACA_MONTHLY_STRIKE_INITIALIZED)) {
            systemMemory.set(ITHACA_MONTHLY_STRIKE_INITIALIZED, true);
            logIthacaStrike("Monthly strike system initialized; scheduling "
                    + "the first deployment.");
            scheduleNextMonthlyIthacaStrike(systemMemory, Math.random());
            return;
        }
        if (systemMemory.contains(ITHACA_MONTHLY_STRIKE_COOLDOWN)) return;

        logIthacaStrike("Monthly cooldown elapsed; attempting deployment.");
        fleet = spawnMonthlyIthacaStrike(ashen, center, Math.random());
        if (fleet != null) {
            scheduleNextMonthlyIthacaStrike(systemMemory, Math.random());
        } else {
            logIthacaStrikeWarning("Monthly deployment failed to create a "
                    + "usable fleet; it will retry on the next population "
                    + "update.");
        }
    }

    private static void maintainConsoleIthacaStrikes(
            StarSystemAPI system, SectorEntityToken center) {
        for (CampaignFleetAPI fleet : new ArrayList<CampaignFleetAPI>(
                system.getFleets())) {
            String id = fleet.getId();
            if (id == null || !id.startsWith(
                    ITHACA_CONSOLE_STRIKE_PREFIX)) {
                continue;
            }
            if (!isAuthoredStarvingThreatFleet(fleet)) continue;
            if (TroyArrivalScript.isFleetBusyForMutation(fleet)) continue;
            if (fleet.isEmpty() || fleet.isDespawning()) continue;
            if (fleet.getBattle() == null && !fleet.getMemoryWithoutUpdate()
                    .contains(ITHACA_MONTHLY_STRIKE_ACTIVE)) {
                logIthacaStrike("Removing expired console fleet id=" + id
                        + ".");
                system.removeEntity(fleet);
                continue;
            }
            configureMonthlyIthacaStrike(fleet, system, center);
        }
    }

    private static void scheduleNextMonthlyIthacaStrike(
            MemoryAPI memory, double roll) {
        float delay = getMonthlyIthacaStrikeDelay(roll);
        memory.set(
                ITHACA_MONTHLY_STRIKE_COOLDOWN,
                true,
                delay);
        logIthacaStrike("Next monthly deployment scheduled in " + delay
                + " campaign days.");
    }

    static float getMonthlyIthacaStrikeDelay(double roll) {
        double clamped = Math.max(0d, Math.min(1d, roll));
        return ITHACA_MONTHLY_STRIKE_MIN_DAYS
                + (float) clamped * (ITHACA_MONTHLY_STRIKE_MAX_DAYS
                        - ITHACA_MONTHLY_STRIKE_MIN_DAYS);
    }

    static int getMonthlyIthacaStrikeTier(double roll) {
        return roll < 0.5d ? FIRST_STRIKE : SECOND_STRIKE;
    }

    private static CampaignFleetAPI spawnMonthlyIthacaStrike(
            StarSystemAPI system,
            SectorEntityToken center,
            double tierRoll) {
        int tier = getMonthlyIthacaStrikeTier(tierRoll);
        logIthacaStrike("Creating monthly " + strikeTierName(tier) + ".");
        CampaignFleetAPI fleet = createIthacaStrikeFleet(
                ITHACA_MONTHLY_STRIKE_ID, tier);
        if (fleet == null) return null;

        double angle = Math.random() * Math.PI * 2d;
        float distance = ITHACA_MONTHLY_STRIKE_MIN_SPAWN_RANGE
                + (float) Math.random()
                        * (ITHACA_MONTHLY_STRIKE_MAX_SPAWN_RANGE
                                - ITHACA_MONTHLY_STRIKE_MIN_SPAWN_RANGE);
        system.addEntity(fleet);
        fleet.setLocation(
                center.getLocation().x + (float) Math.cos(angle) * distance,
                center.getLocation().y + (float) Math.sin(angle) * distance);
        logIthacaStrike("Monthly fleet materialized id=" + fleet.getId()
                + ", distanceFromIthaca=" + Misc.getDistance(fleet, center)
                + ", x=" + fleet.getLocation().x
                + ", y=" + fleet.getLocation().y + ".");
        configureMonthlyIthacaStrike(fleet, system, center);
        return fleet;
    }

    /**
     * Console-test hook: materializes a First or Second Strike beside the
     * player and immediately sends it after the nearest surviving bastion.
     * Returns null unless the player and FOB Ithaca are both in Ashen Verge.
     */
    public static CampaignFleetAPI spawnIthacaStrikeNearPlayer(
            boolean secondStrike) {
        logIthacaStrike("Console spawn requested: "
                + (secondStrike ? "Second Strike" : "First Strike") + ".");
        if (Global.getSector() == null
                || Global.getSector().getPlayerFleet() == null) {
            logIthacaStrikeWarning("Console spawn stopped: sector or player "
                    + "fleet was unavailable.");
            return null;
        }
        StarSystemAPI system = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.ASHEN_VERGE_ID);
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (system == null || player.getContainingLocation() != system) {
            logIthacaStrikeWarning("Console spawn stopped: Ashen Verge was "
                    + "unavailable or the player was in "
                    + (player.getContainingLocation() == null
                            ? "no location"
                            : player.getContainingLocation().getName())
                    + ".");
            return null;
        }
        SectorEntityToken center = system.getEntityById(
                IthacaSectionEncounter.FOB_ENTITY_ID);
        if (!OdysseyExpanseSystem.isFobIthaca(center)) {
            logIthacaStrikeWarning("Console spawn stopped: FOB Ithaca "
                    + "controller was missing.");
            return null;
        }

        IthacaThreatPolicy.initializeRelations();
        int tier = secondStrike ? SECOND_STRIKE : FIRST_STRIKE;
        String id = ITHACA_CONSOLE_STRIKE_PREFIX
                + Global.getSector().getClock().getTimestamp()
                + "_" + (int) (Math.random() * 1000000d);
        CampaignFleetAPI fleet = createIthacaStrikeFleet(id, tier);
        if (fleet == null) {
            logIthacaStrikeWarning("Console spawn stopped: fleet roster was "
                    + "empty after creation.");
            return null;
        }

        double angle = Math.random() * Math.PI * 2d;
        float distance = 900f;
        system.addEntity(fleet);
        fleet.setLocation(
                player.getLocation().x + (float) Math.cos(angle) * distance,
                player.getLocation().y + (float) Math.sin(angle) * distance);
        CampaignFleetAPI target = getNearestIthacaStrikeTarget(center, fleet);
        if (target == null) {
            logIthacaStrikeWarning("Console spawn stopped: no surviving FOB "
                    + "Ithaca bastion was found; removing test fleet id="
                    + fleet.getId() + ".");
            system.removeEntity(fleet);
            return null;
        }
        IthacaSectionEncounter.Section section =
                IthacaSectionEncounter.getSectionForStation(target);
        if (section != null) {
            fleet.getMemoryWithoutUpdate().set(
                    ITHACA_MONTHLY_STRIKE_TARGET, section.id);
        }
        configureMonthlyIthacaStrike(fleet, system, center);
        logIthacaStrike("Console fleet materialized id=" + fleet.getId()
                + ", distanceFromPlayer=" + Misc.getDistance(fleet, player)
                + ", target=" + target.getName()
                + ", distanceToTarget=" + Misc.getDistance(fleet, target)
                + ".");
        return fleet;
    }

    /**
     * Console-test hook: creates one mixed themed Strike for each FOB Ithaca
     * bastion and gives every fleet an exact, persistent station assault.
     */
    public static java.util.List<CampaignFleetAPI>
            spawnIthacaEndgameInvasionFleets() {
        return spawnIthacaEndgameInvasionFleets(true);
    }

    private static java.util.List<CampaignFleetAPI>
            spawnIthacaEndgameInvasionFleets(boolean resetProgress) {
        ArrayList<CampaignFleetAPI> result =
                new ArrayList<CampaignFleetAPI>();
        if (Global.getSector() == null) {
            logIthacaStrikeWarning("Endgame invasion spawn stopped: sector "
                    + "was unavailable.");
            return result;
        }

        StarSystemAPI system = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.ASHEN_VERGE_ID);
        if (system == null) {
            logIthacaStrikeWarning("Endgame invasion spawn stopped: Ashen "
                    + "Verge was unavailable.");
            return result;
        }
        SectorEntityToken center = system.getEntityById(
                IthacaSectionEncounter.FOB_ENTITY_ID);
        if (!OdysseyExpanseSystem.isFobIthaca(center)) {
            logIthacaStrikeWarning("Endgame invasion spawn stopped: FOB "
                    + "Ithaca controller was missing or incompatible.");
            return result;
        }

        IthacaThreatPolicy.initializeRelations();
        if (resetProgress) {
            IthacaSectionEncounter.repairAllBastionsForFinalLabor(center);
        } else {
            IthacaSectionEncounter.ensureSectionStations(center);
        }
        if (MenelausTrial.isFinalLaborActive()) {
            for (IthacaSectionEncounter.Section section
                    : IthacaSectionEncounter.Section.values()) {
                if (!IthacaSectionEncounter.isBastionReady(center, section)) {
                    logIthacaStrikeWarning("Labor V opening deferred: "
                            + section.displayName + " is unavailable; no "
                            + "perimeter victories or siege clocks were committed.");
                    return result;
                }
            }
        }
        MemoryAPI sectorMemory = Global.getSector().getMemoryWithoutUpdate();
        sectorMemory.set(ITHACA_ENDGAME_INVASION_ACTIVE, true);
        for (IthacaSectionEncounter.Section section
                : IthacaSectionEncounter.Section.values()) {
            if (resetProgress) {
                sectorMemory.unset(
                        ithacaEndgamePlayerIntervenedKey(section));
                sectorMemory.unset(
                        ithacaFinalLaborLaneRepelledKey(section));
                sectorMemory.unset(
                        ithacaFinalLaborBastionLostKey(section));
                sectorMemory.unset(
                        ithacaFinalLaborSiegeStartedKey(section));
            }
            CampaignFleetAPI target = IthacaSectionEncounter.getStation(
                    center, section);
            if (target == null || target.isEmpty()) {
                logIthacaStrikeWarning("Endgame invasion skipped "
                        + section.displayName + ": station was unavailable.");
                continue;
            }

            String id = ITHACA_ENDGAME_CONSOLE_PREFIX + section.id;
            SectorEntityToken existing = system.getEntityById(id);
            CampaignFleetAPI fleet = existing instanceof CampaignFleetAPI
                    ? (CampaignFleetAPI) existing : null;
            if (fleet != null && !isFinalLaborPerimeterFleet(fleet)) {
                // Never adopt an unrelated fleet merely because it owns the
                // reserved ID. Serialized foreign/player state wins.
                continue;
            }
            if (fleet != null
                    && TroyArrivalScript.isFleetBusyForMutation(fleet)) {
                result.add(fleet);
                continue;
            }
            if (existing != null && fleet == null) continue;
            if (fleet != null) {
                if (!fleet.isEmpty()) {
                    configureIthacaEndgameInvasion(
                            fleet, system, target, section);
                    result.add(fleet);
                }
                continue;
            }

            fleet = createIthacaEndgameInvasionFleet(
                    system, center, target, section, false);
            if (fleet == null) continue;
            result.add(fleet);
        }
        return result;
    }

    /** Three perimeter victories plus the center-breach victory. */
    static int getFinalLaborInvasionProgress() {
        if (Global.getSector() == null) return 0;
        int progress = 0;
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        for (IthacaSectionEncounter.Section section
                : IthacaSectionEncounter.Section.values()) {
            if (memory.getBoolean(
                    ithacaFinalLaborLaneRepelledKey(section))) {
                progress++;
            }
        }
        if (MenelausTrial.isGautamaDefeated()) progress++;
        return progress;
    }

    static int getFinalLaborInvasionTarget() {
        return IthacaSectionEncounter.Section.values().length + 1;
    }

    /** Current map marker for Labor V, preferring a live assault fleet. */
    static SectorEntityToken getFinalLaborObjective() {
        java.util.List<SectorEntityToken> objectives =
                getFinalLaborObjectives();
        return objectives.isEmpty() ? null : objectives.get(0);
    }

    static java.util.List<SectorEntityToken> getFinalLaborObjectives() {
        ArrayList<SectorEntityToken> result =
                new ArrayList<SectorEntityToken>();
        StarSystemAPI system = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.ASHEN_VERGE_ID);
        if (system == null) return result;
        if (getFinalLaborInvasionProgress()
                >= IthacaSectionEncounter.Section.values().length) {
            SectorEntityToken finale = system.getEntityById(
                    ITHACA_FINAL_CENTER_FLEET_ID);
            if (finale instanceof CampaignFleetAPI
                    && isOwnedFinalLaborCenterFleet(
                            (CampaignFleetAPI) finale)) {
                result.add(finale);
            }
            if (!result.isEmpty()) return result;
        }
        for (IthacaSectionEncounter.Section section
                : IthacaSectionEncounter.Section.values()) {
            if (Global.getSector().getMemoryWithoutUpdate().getBoolean(
                    ithacaFinalLaborLaneRepelledKey(section))) {
                continue;
            }
            SectorEntityToken fleet = system.getEntityById(
                    ITHACA_ENDGAME_CONSOLE_PREFIX + section.id);
            if (fleet instanceof CampaignFleetAPI
                    && isFinalLaborPerimeterFleet(
                            (CampaignFleetAPI) fleet)) {
                result.add(fleet);
            }
        }
        if (result.isEmpty()) {
            SectorEntityToken gateDefense = system.getEntityById(
                    OdysseyExpanseSystem
                            .FOB_ITHACA_GATE_DEFENSE_STATION_ID);
            if (OdysseyExpanseSystem
                    .isOwnedFobIthacaGateDefenseStation(gateDefense)) {
                result.add(gateDefense);
            } else {
                SectorEntityToken ithaca = system.getEntityById(
                        IthacaSectionEncounter.FOB_ENTITY_ID);
                if (OdysseyExpanseSystem.isFobIthaca(ithaca)) {
                    result.add(ithaca);
                }
            }
        }
        return result;
    }

    static boolean isFinalLaborCenterBattle(SectorEntityToken opponent) {
        if (!(opponent instanceof CampaignFleetAPI)
                || !opponent.getMemoryWithoutUpdate().getBoolean(
                        ITHACA_FINAL_CENTER_BATTLE_MARKER)) {
            return false;
        }
        CampaignFleetAPI fleet = (CampaignFleetAPI) opponent;
        if (isOwnedFinalLaborCenterFleet(fleet)) return true;
        BattleAPI battle = fleet.getBattle();
        if (battle == null) return false;
        for (CampaignFleetAPI participant : battle.getBothSides()) {
            if (isOwnedFinalLaborCenterFleet(participant)) return true;
        }
        return false;
    }

    /** True for a player battle containing a canonical perimeter invader. */
    static boolean isFinalLaborPerimeterBattle(SectorEntityToken opponent) {
        if (!(opponent instanceof CampaignFleetAPI)) {
            return false;
        }
        CampaignFleetAPI fleet = (CampaignFleetAPI) opponent;
        if (isFinalLaborPerimeterFleet(fleet)) return true;
        BattleAPI battle = fleet.getBattle();
        if (battle == null) return false;
        for (CampaignFleetAPI participant : battle.getBothSides()) {
            if (isFinalLaborPerimeterFleet(participant)) return true;
        }
        return false;
    }

    private static boolean isFinalLaborPerimeterFleet(
            CampaignFleetAPI fleet) {
        if (fleet == null || isPlayerControlledFleet(fleet)
                || fleet.getId() == null) return false;
        boolean exactId = false;
        for (IthacaSectionEncounter.Section section
                : IthacaSectionEncounter.Section.values()) {
            if ((ITHACA_ENDGAME_CONSOLE_PREFIX + section.id).equals(
                    fleet.getId())) {
                exactId = true;
                break;
            }
        }
        return exactId
                && fleet.getMemoryWithoutUpdate().getBoolean(
                        ITHACA_ENDGAME_INVASION_MARKER)
                && !fleet.getMemoryWithoutUpdate().getBoolean(
                        ITHACA_FINAL_CENTER_BATTLE_MARKER);
    }

    private static boolean isOwnedFinalLaborCenterFleet(
            CampaignFleetAPI fleet) {
        return fleet != null && !isPlayerControlledFleet(fleet)
                && ITHACA_FINAL_CENTER_FLEET_ID.equals(fleet.getId())
                && fleet.getMemoryWithoutUpdate().getBoolean(
                        ITHACA_FINAL_CENTER_BATTLE_MARKER);
    }

    private static CampaignFleetAPI createIthacaEndgameInvasionFleet(
            StarSystemAPI system,
            SectorEntityToken center,
            CampaignFleetAPI target,
            IthacaSectionEncounter.Section section,
            boolean replacement) {
        String id = ITHACA_ENDGAME_CONSOLE_PREFIX + section.id;
        CampaignFleetAPI fleet = createIthacaStrikeFleet(
                id, THIRD_STRIKE, section);
        if (fleet == null) return null;
        system.addEntity(fleet);
        placeEndgameInvasionFleet(fleet, center, target, section);
        configureIthacaEndgameInvasion(fleet, system, target, section);
        logIthacaStrike((replacement ? "Re-formed" : "Spawned")
                + " endgame invasion fleet id=" + id + ", members="
                + fleet.getFleetData().getMembersListCopy().size()
                + ", target=" + target.getName() + ".");
        return fleet;
    }

    private static void placeEndgameInvasionFleet(
            CampaignFleetAPI fleet,
            SectorEntityToken center,
            SectorEntityToken target,
            IthacaSectionEncounter.Section section) {
        float dx = target.getLocation().x - center.getLocation().x;
        float dy = target.getLocation().y - center.getLocation().y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1f) {
            float angle = (float) Math.toRadians(
                    section.ordinal() * 120f + 30f);
            dx = (float) Math.cos(angle);
            dy = (float) Math.sin(angle);
            length = 1f;
        }
        fleet.setLocation(
                target.getLocation().x
                        + dx / length * ITHACA_ENDGAME_SPAWN_DISTANCE,
                target.getLocation().y
                        + dy / length * ITHACA_ENDGAME_SPAWN_DISTANCE);
    }

    private static void maintainIthacaEndgameConsoleInvasion() {
        MemoryAPI sectorMemory = Global.getSector().getMemoryWithoutUpdate();
        if (!sectorMemory.getBoolean(
                ITHACA_ENDGAME_INVASION_ACTIVE)) {
            return;
        }
        StarSystemAPI system = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.ASHEN_VERGE_ID);
        if (system == null) return;
        SectorEntityToken center = system.getEntityById(
                IthacaSectionEncounter.FOB_ENTITY_ID);
        if (!OdysseyExpanseSystem.isFobIthaca(center)) return;
        if (MenelausTrial.isFinalLaborActive()) {
            protectFinalLaborSpartanFromAmbientThreats(system);
        }

        for (IthacaSectionEncounter.Section section
                : IthacaSectionEncounter.Section.values()) {
            SectorEntityToken entity = system.getEntityById(
                    ITHACA_ENDGAME_CONSOLE_PREFIX + section.id);
            CampaignFleetAPI fleet = entity instanceof CampaignFleetAPI
                    ? (CampaignFleetAPI) entity : null;
            if (entity != null && fleet == null) {
                // A wrong-type ID claim is serialized truth; fail closed.
                continue;
            }
            if (fleet != null && !isFinalLaborPerimeterFleet(fleet)) {
                // A foreign/player fleet with the canonical ID is likewise
                // authoritative serialized state.
                continue;
            }
            if (sectorMemory.getBoolean(
                    ithacaFinalLaborLaneRepelledKey(section))) {
                if (fleet != null && fleet.isEmpty()
                        && !TroyArrivalScript.isFleetBusyForMutation(fleet)) {
                    system.removeEntity(fleet);
                }
                continue;
            }
            if (fleet == null || fleet.isEmpty()) {
                if (fleet != null
                        && TroyArrivalScript.isFleetBusyForMutation(fleet)) {
                    continue;
                }
                if (MenelausTrial.isFinalLaborActive()) {
                    // A missing lane is not a victory if its fleet never
                    // reached the live siege setup (e.g. startup was blocked).
                    if (!hasFinalLaborLaneDeployed(section)) continue;
                    if (entity != null) system.removeEntity(entity);
                    markFinalLaborLaneRepelled(section);
                    continue;
                }
                if (sectorMemory.getBoolean(
                        ithacaEndgamePlayerIntervenedKey(section))) {
                    continue;
                }
                CampaignFleetAPI target = IthacaSectionEncounter.getStation(
                        center, section);
                if (target == null || target.isEmpty()) continue;
                if (entity != null) system.removeEntity(entity);
                createIthacaEndgameInvasionFleet(
                        system, center, target, section, true);
                continue;
            }

            if (MenelausTrial.isFinalLaborActive()
                    && sectorMemory.getBoolean(
                            ithacaFinalLaborBastionLostKey(section))) {
                configureFinalLaborTfsAssault(
                        fleet, system, center, section);
                continue;
            }

            CampaignFleetAPI target = IthacaSectionEncounter.getStation(
                    center, section);
            if (target == null || target.isEmpty()) {
                if (MenelausTrial.isFinalLaborActive()) {
                    markFinalLaborBastionLost(center, section);
                    configureFinalLaborTfsAssault(
                            fleet, system, center, section);
                }
                continue;
            }
            configureIthacaEndgameInvasion(
                    fleet, system, target, section);
        }
    }

    /** Keeps ordinary Ashen patrols from consuming the Labor V TFS reserve. */
    private static void protectFinalLaborSpartanFromAmbientThreats(
            StarSystemAPI system) {
        for (CampaignFleetAPI spartan
                : OdysseyExpanseSystem.ensureFobIthacaSpartanGuards(system)) {
            CampaignFleetAIAPI spartanAI = spartan.getAI();
            for (CampaignFleetAPI other : system.getFleets()) {
                if (other == null || other == spartan
                        || !other.getMemoryWithoutUpdate().getBoolean(
                                STARVING_THREAT_MARKER)) {
                    continue;
                }
                String phase = other.getMemoryWithoutUpdate().getString(
                        ITHACA_FINAL_LABOR_PHASE);
                if (other.getMemoryWithoutUpdate().getBoolean(
                                ITHACA_FINAL_CENTER_BATTLE_MARKER)
                        || phase != null
                        && phase.startsWith("attacking_tfs_")) {
                    continue;
                }
                if (spartanAI != null) {
                    spartanAI.doNotAttack(
                            other, ITHACA_ENDGAME_IGNORE_DISTRACTION_DAYS);
                }
                if (other.getAI() != null) {
                    other.getAI().doNotAttack(
                            spartan, ITHACA_ENDGAME_IGNORE_DISTRACTION_DAYS);
                }
            }
        }
    }

    private static void configureIthacaEndgameInvasion(
            CampaignFleetAPI fleet,
            StarSystemAPI system,
            CampaignFleetAPI target,
            IthacaSectionEncounter.Section section) {
        if (TroyArrivalScript.isFleetBusyForMutation(fleet)) return;
        removeGautamaFromPerimeterInvasion(fleet);
        configureStarvingThreatIdentity(fleet);
        ensureIthacaSiegeStalemateHullmods(fleet);
        fleet.setName(perimeterStrikeName(section));
        EncounterCombatChatter.configure(fleet, perimeterStrikeName(section));
        fleet.setFaction(IthacaThreatPolicy.FACTION_ID, true);
        fleet.setNoAutoDespawn(true);
        fleet.setAbortDespawn(true);
        fleet.setNoEngaging(0f);

        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        memory.set(ITHACA_ENDGAME_INVASION_MARKER, true);
        memory.set(ITHACA_MONTHLY_STRIKE_TARGET, section.id);
        memory.unset(ITHACA_MONTHLY_STRIKE_ACTIVE);
        memory.set(MemFlags.MEMORY_KEY_NO_JUMP, true);
        memory.set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        memory.set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE);
        memory.unset(MemFlags.NON_HOSTILE_OVERRIDES_MAKE_HOSTILE);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_HOSTILE);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE);
        memory.unset(MemFlags.MEMORY_KEY_PURSUE_PLAYER);
        memory.unset(MemFlags.FLEET_IGNORES_OTHER_FLEETS);
        memory.unset(MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS);
        ensureIthacaEndgameBattleListener(fleet, section);
        if (fleet.getBattle() != null) return;
        if (MenelausTrial.isFinalLaborActive()) {
            maintainFinalLaborBastionSiege(
                    fleet, system, target, section);
            return;
        }

        SectorEntityToken targetToken = ensureIthacaStrikeTargetToken(
                system, target);
        if (!memory.getBoolean(ITHACA_STRIKE_ASSAULT_ORDERED)
                || !isIthacaStrikeAssaultOrder(
                        fleet, targetToken, target)) {
            fleet.clearAssignments();
            String text = "invading " + section.displayName;
            fleet.addAssignment(
                    FleetAssignment.ATTACK_LOCATION,
                    targetToken,
                    ITHACA_ENDGAME_INVASION_DAYS,
                    text);
            fleet.addAssignment(
                    FleetAssignment.GO_TO_LOCATION,
                    targetToken,
                    ITHACA_ENDGAME_INVASION_DAYS,
                    text);
            fleet.addAssignment(
                    FleetAssignment.INTERCEPT,
                    target,
                    ITHACA_ENDGAME_INVASION_DAYS,
                    text);
            memory.set(ITHACA_STRIKE_ASSAULT_ORDERED, true);
            logIthacaStrike("Endgame assault ordered fleet=" + fleet.getId()
                    + ", token=" + targetToken.getId() + ", target="
                    + target.getId() + ".");
        }
        lockIthacaEndgameTarget(fleet, system, target);
    }

    /**
     * Repairs old or externally modified perimeter fleets. Gautama belongs
     * only to the fourth, Breach Defense assault and is never valid here.
     */
    static int removeGautamaFromPerimeterInvasion(CampaignFleetAPI fleet) {
        if (fleet == null || fleet.getBattle() != null) return 0;
        int removed = 0;
        for (FleetMemberAPI member : new ArrayList<FleetMemberAPI>(
                fleet.getFleetData().getMembersListCopy())) {
            if (!isGautamaMember(member)) continue;
            fleet.getFleetData().removeFleetMember(member);
            removed++;
        }
        if (removed <= 0) return 0;
        if (!fleet.getFleetData().getMembersListCopy().isEmpty()) {
            fleet.getFleetData().setFlagship(
                    fleet.getFleetData().getMembersListCopy().get(0));
        }
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
        logIthacaStrikeWarning("Removed " + removed
                + " invalid Gautama member(s) from perimeter invasion id="
                + fleet.getId() + ". Gautama is reserved for the fourth "
                + "Breach Defense assault.");
        return removed;
    }

    /** Holds one Labor V fleet outside its bastion until the 60-day deadline. */
    private static void maintainFinalLaborBastionSiege(
            CampaignFleetAPI fleet,
            StarSystemAPI system,
            CampaignFleetAPI target,
            IthacaSectionEncounter.Section section) {
        if (TroyArrivalScript.isFleetBusyForMutation(fleet)
                || TroyArrivalScript.isFleetBusyForMutation(target)) return;
        MemoryAPI sectorMemory = Global.getSector().getMemoryWithoutUpdate();
        SectorEntityToken center = system.getEntityById(
                IthacaSectionEncounter.FOB_ENTITY_ID);
        if (!OdysseyExpanseSystem.isFobIthaca(center)) return;
        String startedKey = ithacaFinalLaborSiegeStartedKey(section);
        if (!sectorMemory.contains(startedKey)) {
            sectorMemory.set(startedKey,
                    Global.getSector().getClock().getTimestamp());
        }
        if (getFinalLaborSiegeDaysRemaining(section) <= 0f) {
            if (target.getBattle() != null
                    || !markFinalLaborBastionLost(center, section)) {
                return;
            }
            configureFinalLaborTfsAssault(
                    fleet, system, center, section);
            return;
        }

        SectorEntityToken siege = ensureFinalLaborSiegeToken(
                system, center, target, section);
        if (siege == null) return;
        String phase = "siege_" + section.id;
        if (!phase.equals(fleet.getMemoryWithoutUpdate().getString(
                        ITHACA_FINAL_LABOR_PHASE))
                || !isFinalLaborSiegeOrder(fleet, siege)) {
            fleet.clearAssignments();
            fleet.addAssignment(
                    FleetAssignment.GO_TO_LOCATION,
                    siege,
                    ITHACA_ENDGAME_INVASION_DAYS,
                    "closing on " + section.displayName);
            fleet.addAssignment(
                    FleetAssignment.DEFEND_LOCATION,
                    siege,
                    ITHACA_ENDGAME_INVASION_DAYS,
                    "besieging " + section.displayName);
            fleet.getMemoryWithoutUpdate().set(
                    ITHACA_FINAL_LABOR_PHASE, phase);
            fleet.getMemoryWithoutUpdate().unset(
                    ITHACA_STRIKE_ASSAULT_ORDERED);
            fleet.getMemoryWithoutUpdate().unset(
                    ITHACA_FINAL_LABOR_TFS_TARGET);
            logIthacaStrike("Labor V siege position ordered fleet="
                    + fleet.getId() + ", bastion=" + section.id
                    + ", deadlineDays="
                    + ITHACA_FINAL_LABOR_SIEGE_DAYS + ".");
        }
        blockFinalLaborSiegeContacts(fleet, system, target, siege);
    }

    private static SectorEntityToken ensureFinalLaborSiegeToken(
            StarSystemAPI system,
            SectorEntityToken center,
            CampaignFleetAPI target,
            IthacaSectionEncounter.Section section) {
        String id = ITHACA_FINAL_LABOR_SIEGE_TOKEN_PREFIX + section.id;
        SectorEntityToken token = system.getEntityById(id);
        if (token == null) {
            token = system.createToken(
                    target.getLocation().x, target.getLocation().y);
            token.setId(id);
            token.setName(section.displayName + " siege line");
            markOwnedProxyToken(token);
            system.addEntity(token);
        } else if (!isOwnedProxyToken(token)) {
            logBlockedProxyTokenOnce(id, token);
            return null;
        }
        float dx = target.getLocation().x - center.getLocation().x;
        float dy = target.getLocation().y - center.getLocation().y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1f) length = 1f;
        token.setFixedLocation(
                target.getLocation().x
                        + dx / length * ITHACA_FINAL_LABOR_SIEGE_STANDOFF,
                target.getLocation().y
                        + dy / length * ITHACA_FINAL_LABOR_SIEGE_STANDOFF);
        return token;
    }

    private static boolean isFinalLaborSiegeOrder(
            CampaignFleetAPI fleet, SectorEntityToken siege) {
        if (fleet.getCurrentAssignment() == null) return false;
        FleetAssignment assignment = fleet.getCurrentAssignment()
                .getAssignment();
        return fleet.getCurrentAssignment().getTarget() == siege
                && (assignment == FleetAssignment.GO_TO_LOCATION
                        || assignment == FleetAssignment.DEFEND_LOCATION);
    }

    private static void blockFinalLaborSiegeContacts(
            CampaignFleetAPI invasion,
            StarSystemAPI system,
            CampaignFleetAPI bastion,
            SectorEntityToken siege) {
        CampaignFleetAIAPI ai = invasion.getAI();
        if (ai == null) return;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        for (CampaignFleetAPI other : system.getFleets()) {
            if (other == null || other == invasion) continue;
            ai.doNotAttack(other, ITHACA_FINAL_LABOR_SIEGE_BLOCK_DAYS);
            if (other != player && other.getAI() != null) {
                other.getAI().doNotAttack(
                        invasion, ITHACA_FINAL_LABOR_SIEGE_BLOCK_DAYS);
            }
        }
        if (player != null && player != invasion) {
            ai.doNotAttack(player, ITHACA_FINAL_LABOR_SIEGE_BLOCK_DAYS);
        }
        if (ai instanceof ModularFleetAIAPI) {
            TacticalModulePlugin tactical =
                    ((ModularFleetAIAPI) ai).getTacticalModule();
            if (tactical != null) {
                tactical.setPriorityTarget(
                        siege, ITHACA_STRIKE_PRIORITY_DAYS, false);
                tactical.setTarget(siege);
            }
        }
    }

    /** Converts a surviving perimeter invasion into its TFS attack phase. */
    private static void configureFinalLaborTfsAssault(
            CampaignFleetAPI invasion,
            StarSystemAPI system,
            SectorEntityToken center,
            IthacaSectionEncounter.Section section) {
        if (invasion == null || system == null || center == null) return;
        if (TroyArrivalScript.isFleetBusyForMutation(invasion)) return;
        configureStarvingThreatIdentity(invasion);
        ensureIthacaSiegeStalemateHullmods(invasion);
        invasion.setName(perimeterStrikeName(section));
        EncounterCombatChatter.configure(invasion, perimeterStrikeName(section));
        invasion.setFaction(IthacaThreatPolicy.FACTION_ID, true);
        invasion.setNoAutoDespawn(true);
        invasion.setAbortDespawn(true);
        invasion.setNoEngaging(0f);
        MemoryAPI memory = invasion.getMemoryWithoutUpdate();
        memory.set(ITHACA_ENDGAME_INVASION_MARKER, true);
        memory.set(MusicPlayerPluginImpl.COMBAT_MUSIC_SET_MEM_KEY,
                FinalLaborMusic.PERIMETER_FIGHT_ID);
        memory.set(MemFlags.MEMORY_KEY_NO_JUMP, true);
        memory.set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        memory.set(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE, true);
        memory.set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        memory.set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
        ensureIthacaEndgameBattleListener(invasion, section);
        if (invasion.getBattle() != null) return;

        CampaignFleetAPI target = getFinalLaborSpartanTarget(
                invasion, system, section);
        if (target == null) {
            String phase = "waiting_for_tfs_" + section.id;
            if (!phase.equals(memory.getString(
                    ITHACA_FINAL_LABOR_PHASE))) {
                invasion.clearAssignments();
                invasion.addAssignment(
                        FleetAssignment.DEFEND_LOCATION,
                        center,
                        ITHACA_ENDGAME_INVASION_DAYS,
                        "hunting Task Force Spartan");
                memory.set(ITHACA_FINAL_LABOR_PHASE, phase);
                memory.unset(ITHACA_FINAL_LABOR_TFS_TARGET);
            }
            return;
        }

        String phase = "attacking_tfs_" + target.getId();
        if (!phase.equals(memory.getString(ITHACA_FINAL_LABOR_PHASE))
                || invasion.getCurrentAssignment() == null
                || invasion.getCurrentAssignment().getAssignment()
                        != FleetAssignment.INTERCEPT
                || invasion.getCurrentAssignment().getTarget() != target) {
            invasion.clearAssignments();
            invasion.addAssignment(
                    FleetAssignment.INTERCEPT,
                    target,
                    ITHACA_ENDGAME_INVASION_DAYS,
                    "attacking " + target.getName());
            memory.set(ITHACA_FINAL_LABOR_PHASE, phase);
            memory.set(ITHACA_FINAL_LABOR_TFS_TARGET, target.getId());
            logIthacaStrike("Labor V breakthrough fleet="
                    + invasion.getId() + " is attacking TFS target="
                    + target.getId() + ".");
        }
        if (target.getBattle() == null
                && (target.getCurrentAssignment() == null
                        || target.getCurrentAssignment().getAssignment()
                                != FleetAssignment.INTERCEPT
                        || target.getCurrentAssignment().getTarget()
                                != invasion)) {
            target.clearAssignments();
            target.addAssignment(
                    FleetAssignment.INTERCEPT,
                    invasion,
                    ITHACA_ENDGAME_INVASION_DAYS,
                    "engaging a Starving Threat breakthrough");
        }
        lockFinalLaborFleetDuel(invasion, target, system);
    }

    private static CampaignFleetAPI getFinalLaborSpartanTarget(
            CampaignFleetAPI invasion,
            StarSystemAPI system,
            IthacaSectionEncounter.Section section) {
        String preferredId;
        switch (section) {
            case NORTH:
                preferredId = OdysseyExpanseSystem
                        .FOB_ITHACA_SPARTAN_MOUTH_NORTH_ID;
                break;
            case EAST:
                preferredId = OdysseyExpanseSystem
                        .FOB_ITHACA_SPARTAN_GUARD_ID;
                break;
            case SOUTH:
            default:
                preferredId = OdysseyExpanseSystem
                        .FOB_ITHACA_SPARTAN_MOUTH_SOUTH_ID;
                break;
        }
        SectorEntityToken preferred = system.getEntityById(preferredId);
        if (preferred instanceof CampaignFleetAPI
                && OdysseyExpanseSystem.isOwnedFobIthacaSpartanGuard(
                        (CampaignFleetAPI) preferred)
                && !((CampaignFleetAPI) preferred).isEmpty()
                && !TroyArrivalScript.isFleetBusyForMutation(
                        (CampaignFleetAPI) preferred)) {
            return (CampaignFleetAPI) preferred;
        }

        CampaignFleetAPI nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (CampaignFleetAPI candidate
                : OdysseyExpanseSystem.ensureFobIthacaSpartanGuards(system)) {
            if (!OdysseyExpanseSystem.isOwnedFobIthacaSpartanGuard(candidate)
                    || candidate.isEmpty()
                    || TroyArrivalScript.isFleetBusyForMutation(candidate)) {
                continue;
            }
            float distance = Misc.getDistance(invasion, candidate);
            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static void lockFinalLaborFleetDuel(
            CampaignFleetAPI invasion,
            CampaignFleetAPI spartan,
            StarSystemAPI system) {
        CampaignFleetAIAPI invasionAI = invasion.getAI();
        CampaignFleetAIAPI spartanAI = spartan.getAI();
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (invasionAI != null) invasionAI.doNotAttack(spartan, 0f);
        if (spartanAI != null) spartanAI.doNotAttack(invasion, 0f);
        for (CampaignFleetAPI other : system.getFleets()) {
            if (other == null || other == invasion || other == spartan) {
                continue;
            }
            if (invasionAI != null) {
                invasionAI.doNotAttack(
                        other, ITHACA_ENDGAME_IGNORE_DISTRACTION_DAYS);
            }
            if (spartanAI != null) {
                spartanAI.doNotAttack(
                        other, ITHACA_ENDGAME_IGNORE_DISTRACTION_DAYS);
            }
            if (other != player && other.getAI() != null) {
                other.getAI().doNotAttack(
                        invasion, ITHACA_ENDGAME_IGNORE_DISTRACTION_DAYS);
            }
        }
        setTacticalTarget(invasionAI, spartan);
        setTacticalTarget(spartanAI, invasion);
    }

    private static void setTacticalTarget(
            CampaignFleetAIAPI ai, SectorEntityToken target) {
        if (!(ai instanceof ModularFleetAIAPI) || target == null) return;
        TacticalModulePlugin tactical =
                ((ModularFleetAIAPI) ai).getTacticalModule();
        if (tactical == null) return;
        tactical.setPriorityTarget(
                target, ITHACA_STRIKE_PRIORITY_DAYS, false);
        tactical.setTarget(target);
    }

    static float getFinalLaborSiegeDaysRemaining(
            IthacaSectionEncounter.Section section) {
        if (Global.getSector() == null || section == null) return 0f;
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        if (memory.getBoolean(ithacaFinalLaborBastionLostKey(section))) {
            return 0f;
        }
        String key = ithacaFinalLaborSiegeStartedKey(section);
        if (!memory.contains(key)) return ITHACA_FINAL_LABOR_SIEGE_DAYS;
        return Math.max(0f, ITHACA_FINAL_LABOR_SIEGE_DAYS
                - Global.getSector().getClock().getElapsedDaysSince(
                        memory.getLong(key)));
    }

    static boolean hasFinalLaborLaneDeployed(
            IthacaSectionEncounter.Section section) {
        return Global.getSector() != null && section != null
                && Global.getSector().getMemoryWithoutUpdate().contains(
                        ithacaFinalLaborSiegeStartedKey(section));
    }

    private static boolean markFinalLaborBastionLost(
            SectorEntityToken center,
            IthacaSectionEncounter.Section section) {
        if (center == null || section == null) return false;
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        String key = ithacaFinalLaborBastionLostKey(section);
        if (memory.getBoolean(key)) return true;
        if (!IthacaSectionEncounter.disableBastionForFinalLabor(
                center, section)) {
            return false;
        }
        memory.set(key, true);
        SectorEntityToken siege = center.getContainingLocation()
                .getEntityById(
                        ITHACA_FINAL_LABOR_SIEGE_TOKEN_PREFIX + section.id);
        if (isOwnedProxyToken(siege)) {
            center.getContainingLocation().removeEntity(siege);
        } else if (siege != null) {
            logBlockedProxyTokenOnce(
                    ITHACA_FINAL_LABOR_SIEGE_TOKEN_PREFIX + section.id,
                    siege);
        }
        logIthacaStrike("Labor V siege deadline expired; "
                + section.displayName
                + " was destroyed and its invasion is turning on TFS.");
        MenelausTrialIntel.syncProgress(true);
        return true;
    }

    /**
     * Prevents the command siege fleets from opportunistically peeling off
     * toward the player, roaming TFS fleets, or either of the other bastions.
     * Relations stay hostile, so the player can still join the target
     * bastion's battle and fight the invasion normally.
     */
    static int lockIthacaEndgameTarget(
            CampaignFleetAPI fleet,
            StarSystemAPI system,
            CampaignFleetAPI target) {
        if (fleet == null || system == null || target == null
                || fleet.getAI() == null) {
            return 0;
        }

        CampaignFleetAIAPI ai = fleet.getAI();
        int blocked = 0;
        CampaignFleetAPI player = Global.getSector() == null
                ? null : Global.getSector().getPlayerFleet();
        boolean playerBlocked = false;
        for (CampaignFleetAPI other : system.getFleets()) {
            if (other == null || other == fleet || other == target) continue;
            ai.doNotAttack(other, ITHACA_ENDGAME_IGNORE_DISTRACTION_DAYS);
            if (other != player && other.getAI() != null) {
                // Keep roaming TFS/Guard fleets and unrelated hostiles from
                // intercepting the scripted attacker before it reaches its
                // designated bastion. This does not affect direct player
                // interaction or joining the bastion's active battle.
                other.getAI().doNotAttack(
                        fleet, ITHACA_ENDGAME_IGNORE_DISTRACTION_DAYS);
            }
            if (other == player) playerBlocked = true;
            blocked++;
        }
        if (player != null && player != fleet && player != target
                && !playerBlocked) {
            // Usually the player is already in system.getFleets(); this also
            // handles location-transition edge cases where it is not listed.
            ai.doNotAttack(
                    player, ITHACA_ENDGAME_IGNORE_DISTRACTION_DAYS);
            blocked++;
        }

        if (ai instanceof ModularFleetAIAPI) {
            TacticalModulePlugin tactical =
                    ((ModularFleetAIAPI) ai).getTacticalModule();
            if (tactical != null) {
                SectorEntityToken diverted = tactical.getTarget();
                if (diverted != null && diverted != target
                        && !fleet.getMemoryWithoutUpdate().getBoolean(
                                ITHACA_ENDGAME_TARGET_CORRECTION_LOG)) {
                    logIthacaStrike("Correcting endgame invasion distraction "
                            + "fleet=" + fleet.getId() + ", divertedTarget="
                            + diverted.getId() + ", restoredTarget="
                            + target.getId() + ".");
                    fleet.getMemoryWithoutUpdate().set(
                            ITHACA_ENDGAME_TARGET_CORRECTION_LOG,
                            true,
                            0.25f);
                }
                tactical.setPriorityTarget(
                        target, ITHACA_STRIKE_PRIORITY_DAYS, false);
                tactical.setTarget(target);
            }
        }
        return blocked;
    }

    private static String ithacaEndgamePlayerIntervenedKey(
            IthacaSectionEncounter.Section section) {
        return ITHACA_ENDGAME_PLAYER_INTERVENED_PREFIX + section.id;
    }

    private static String ithacaFinalLaborLaneRepelledKey(
            IthacaSectionEncounter.Section section) {
        return ITHACA_FINAL_LABOR_LANE_REPELLED_PREFIX + section.id;
    }

    private static String ithacaFinalLaborSiegeStartedKey(
            IthacaSectionEncounter.Section section) {
        return ITHACA_FINAL_LABOR_SIEGE_STARTED_PREFIX + section.id;
    }

    private static String ithacaFinalLaborBastionLostKey(
            IthacaSectionEncounter.Section section) {
        return ITHACA_FINAL_LABOR_BASTION_LOST_PREFIX + section.id;
    }

    private static void markFinalLaborLaneRepelled(
            IthacaSectionEncounter.Section section) {
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        String key = ithacaFinalLaborLaneRepelledKey(section);
        if (memory.getBoolean(key)) return;
        memory.set(key, true);
        logIthacaStrike("Final Labor invasion repelled at "
                + section.displayName + ".");
        MenelausTrialIntel.syncProgress(true);
    }

    private static void ensureIthacaEndgameBattleListener(
            CampaignFleetAPI fleet,
            IthacaSectionEncounter.Section section) {
        for (FleetEventListener listener : fleet.getEventListeners()) {
            if (listener instanceof IthacaEndgameBattleListener) return;
        }
        fleet.addEventListener(new IthacaEndgameBattleListener(section.id));
    }

    private static final class IthacaEndgameBattleListener
            implements FleetEventListener {
        private final String sectionId;

        private IthacaEndgameBattleListener(String sectionId) {
            this.sectionId = sectionId;
        }

        @Override
        public void reportBattleOccurred(
                CampaignFleetAPI fleet,
                CampaignFleetAPI primaryWinner,
                BattleAPI battle) {
            if (Global.getSector() == null || battle == null) return;
            IthacaSectionEncounter.Section section =
                    IthacaSectionEncounter.Section.fromId(sectionId);
            if (section == null) return;
            boolean defeated = battle.wasFleetDefeated(
                    fleet, primaryWinner) || fleet.isEmpty();
            if (MenelausTrial.isFinalLaborActive() && defeated) {
                markFinalLaborLaneRepelled(section);
            }
            if (MenelausTrial.isFinalLaborActive()) {
                // Combat owns its own music state. Reassert the instrumental
                // as the campaign view returns; only the center plugin may
                // replace it with the vocal cue.
                FinalLaborMusic.playMissionStart();
            }
            if (battle.isPlayerInvolved()) {
                Global.getSector().getMemoryWithoutUpdate().set(
                        ithacaEndgamePlayerIntervenedKey(section), true);
                logIthacaStrike("Player intervened in endgame invasion lane="
                        + sectionId + "; campaign autoresolve replacement is "
                        + "now disabled for that lane.");
            } else if (MenelausTrial.isFinalLaborActive() && defeated) {
                logIthacaStrike("TFS destroyed Labor V perimeter invasion "
                        + "lane=" + sectionId + "; the fleet will not re-form.");
            }
        }

        @Override
        public void reportFleetDespawnedToListener(
                CampaignFleetAPI fleet,
                CampaignEventListener.FleetDespawnReason reason,
                Object param) {
            // The population maintainer handles AI-only battle losses.
        }
    }

    private static CampaignFleetAPI createIthacaStrikeFleet(
            String id, int tier) {
        return createIthacaStrikeFleet(id, tier, null);
    }

    private static CampaignFleetAPI createIthacaStrikeFleet(
            String id, int tier, IthacaSectionEncounter.Section section) {
        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(
                IthacaThreatPolicy.FACTION_ID,
                "Starving Threat Strike",
                true);
        fleet.setId(id);
        fleet.setNoFactionInName(true);
        fleet.setTransponderOn(false);
        fleet.getMemoryWithoutUpdate().set(STARVING_THREAT_MARKER, true);

        if (section == null) {
            addStrikeRoster(fleet, tier);
        } else {
            EncounterCombatChatter.configure(fleet, perimeterStrikeName(section));
            for (String variant : buildPerimeterStrikeRosterVariants(section)) {
                addMember(fleet, variant);
            }
            fleet.getMemoryWithoutUpdate().set(
                    MemFlags.MEMORY_KEY_FLEET_TYPE, FleetTypes.PATROL_LARGE);
        }
        if (fleet.getFleetData().getMembersListCopy().isEmpty()) {
            logIthacaStrikeWarning("Created empty " + strikeTierName(tier)
                    + " roster for id=" + id + ". Check Starving Threat "
                    + "variant IDs and CSV loading.");
            return null;
        }
        fleet.getFleetData().setFlagship(
                fleet.getFleetData().getMembersListCopy().get(0));
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
        fleet.getMemoryWithoutUpdate().set(
                ITHACA_MONTHLY_STRIKE_TIER, tier);
        fleet.getMemoryWithoutUpdate().set(
                ITHACA_MONTHLY_STRIKE_ACTIVE,
                true,
                ITHACA_MONTHLY_STRIKE_LIFETIME_DAYS);
        logIthacaStrike("Fleet built id=" + id + ", tier="
                + strikeTierName(tier) + ", members="
                + fleet.getFleetData().getMembersListCopy().size()
                + ", fleetPoints=" + fleet.getFleetPoints() + ".");
        return fleet;
    }

    static String perimeterStrikeName(IthacaSectionEncounter.Section section) {
        switch (section) {
            case NORTH: return EncounterCombatChatter.SCYLLA;
            case SOUTH: return EncounterCombatChatter.SIREN;
            case EAST: return EncounterCombatChatter.CYCLOPS;
            default: throw new IllegalArgumentException("Unknown bastion");
        }
    }

    /** Mixed, deterministic compositions; maintenance never replenishes them. */
    static java.util.List<String> buildPerimeterStrikeRosterVariants(
            IthacaSectionEncounter.Section section) {
        int[] counts;
        int firstRole;
        switch (section) {
            case NORTH:
                counts = SCYLLA_STRIKE_COUNTS;
                firstRole = 1;
                break;
            case SOUTH:
                counts = SIREN_STRIKE_COUNTS;
                firstRole = 3;
                break;
            case EAST:
                counts = CYCLOPS_STRIKE_COUNTS;
                firstRole = 0;
                break;
            default: throw new IllegalArgumentException("Unknown bastion");
        }
        ArrayList<String> roster = new ArrayList<String>();
        for (int round = 0; ; round++) {
            boolean added = false;
            for (int offset = 0; offset < counts.length; offset++) {
                int role = (firstRole + offset) % counts.length;
                if (round >= counts[role]) continue;
                String[] variants = STARVING_ROLE_VARIANTS[role];
                roster.add(variants[round % variants.length]);
                added = true;
            }
            if (!added) return roster;
        }
    }

    /** Pure latch query: do not run quest reconciliation from station upkeep. */
    static boolean isBreachDefenseAssaultCommitted() {
        if (Global.getSector() == null) return false;
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        return memory.getBoolean(ITHACA_FINAL_CENTER_CREATED_V1)
                && !memory.getBoolean(MenelausTrial.GAUTAMA_DEFEATED);
    }

    /** The one explicitly restorable defender, not a world-topology repair. */
    static void maintainBreachDefenseStation(StarSystemAPI system) {
        if (system == null) return;
        SectorEntityToken center = system.getEntityById(
                IthacaSectionEncounter.FOB_ENTITY_ID);
        if (!OdysseyExpanseSystem.isFobIthaca(center)) return;
        SectorEntityToken entity = system.getEntityById(
                OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_STATION_ID);
        CampaignFleetAPI station;
        if (entity == null) {
            station = OdysseyExpanseSystem
                    .ensureFobIthacaGateDefenseStation(system);
        } else if (entity instanceof CampaignFleetAPI
                && !isPlayerControlledFleet((CampaignFleetAPI) entity)
                && ((CampaignFleetAPI) entity).isStationMode()
                && ((CampaignFleetAPI) entity).isEmpty()) {
            station = OdysseyExpanseSystem
                    .ensureFobIthacaGateDefenseStation(system);
        } else if (OdysseyExpanseSystem
                .isOwnedFobIthacaGateDefenseStation(entity)) {
            station = (CampaignFleetAPI) entity;
        } else {
            return;
        }
        if (station == null
                || TroyArrivalScript.isFleetBusyForMutation(station)) return;
        station.setName(OdysseyExpanseSystem.FOB_ITHACA_BREACH_DEFENSE_NAME);
        if (isBreachDefenseAssaultCommitted()) {
            station.getMemoryWithoutUpdate().unset(
                    MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS);
        } else {
            station.getMemoryWithoutUpdate().set(
                    MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS, true);
        }
        blockAmbientBreachDefenseAttacks(system, station);
    }

    static void blockAmbientBreachDefenseAttacks(
            StarSystemAPI system, CampaignFleetAPI station) {
        for (CampaignFleetAPI other : system.getFleets()) {
            if (other == null || other == station
                    || isPlayerControlledFleet(other)
                    || isOwnedFinalLaborCenterFleet(other)
                    || TroyArrivalScript.isFleetBusyForMutation(other)) {
                continue;
            }
            if (other.getAI() != null) {
                other.getAI().doNotAttack(
                        station, ITHACA_ENDGAME_IGNORE_DISTRACTION_DAYS);
            }
        }
    }

    /**
     * Builds the Labor-V-only Heavenly Strike. Gautama remains unique while
     * each of three ordered reinforcement tranches contains a Fabricator, a
     * Hive, and a complete mixture of escort roles. Ordinary First/Second/
     * Third Strike rosters deliberately remain unchanged.
     */
    private static CampaignFleetAPI createFinalCenterStrikeFleet(String id) {
        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(
                IthacaThreatPolicy.FACTION_ID,
                "Heavenly Strike",
                true);
        fleet.setId(id);
        fleet.setNoFactionInName(true);
        fleet.setTransponderOn(false);
        fleet.getMemoryWithoutUpdate().set(STARVING_THREAT_MARKER, true);

        FleetMemberAPI gautama = addMember(
                fleet, "chief_navigator_scylla_Fabricator");
        java.util.List<String> support = buildFinalCenterRosterVariants();
        for (int index = 0; index < support.size(); index++) {
            int wave = index
                    / ITHACA_FINAL_CENTER_MIXED_WAVE_ROLES.length;
            int slot = index
                    % ITHACA_FINAL_CENTER_MIXED_WAVE_ROLES.length;
            addMember(
                    fleet,
                    support.get(index),
                    finalCenterWaveMemberId(wave, slot));
        }
        if (gautama == null
                || fleet.getFleetData().getMembersListCopy().isEmpty()) {
            logIthacaStrikeWarning("Created empty Heavenly Strike roster for "
                    + "id=" + id + ". Check Starving Threat variant IDs and "
                    + "CSV loading.");
            return null;
        }
        fleet.getFleetData().setFlagship(gautama);
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_FLEET_TYPE,
                FleetTypes.PATROL_LARGE);
        logIthacaStrike("Heavenly Strike built id=" + id
                + ", mixedWaves=" + ITHACA_FINAL_CENTER_MIXED_WAVE_COUNT
                + ", members="
                + fleet.getFleetData().getMembersListCopy().size()
                + ", fleetPoints=" + fleet.getFleetPoints() + ".");
        return fleet;
    }

    /** Pure deterministic roster builder exposed package-locally for QA. */
    static java.util.List<String> buildFinalCenterRosterVariants() {
        ArrayList<String> result = new ArrayList<String>(
                ITHACA_FINAL_CENTER_MIXED_WAVE_COUNT
                        * ITHACA_FINAL_CENTER_MIXED_WAVE_ROLES.length);
        for (int wave = 0;
                wave < ITHACA_FINAL_CENTER_MIXED_WAVE_COUNT;
                wave++) {
            int[] roleOccurrences = new int[STARVING_ROLE_VARIANTS.length];
            for (int role : ITHACA_FINAL_CENTER_MIXED_WAVE_ROLES) {
                String[] variants = STARVING_ROLE_VARIANTS[role];
                int variant = (wave + roleOccurrences[role])
                        % variants.length;
                result.add(variants[variant]);
                roleOccurrences[role]++;
            }
        }
        return result;
    }

    static int getFinalCenterWaveCount() {
        return ITHACA_FINAL_CENTER_MIXED_WAVE_COUNT;
    }

    static int getFinalCenterWaveSize() {
        return ITHACA_FINAL_CENTER_MIXED_WAVE_ROLES.length;
    }

    static String finalCenterWaveMemberId(int wave, int slot) {
        return ITHACA_FINAL_CENTER_WAVE_MEMBER_PREFIX
                + wave + "_" + slot;
    }

    /** Returns -1 unless this is one of the exact authored wave-member IDs. */
    static int getFinalCenterWaveIndex(String memberId) {
        if (memberId == null
                || !memberId.startsWith(
                        ITHACA_FINAL_CENTER_WAVE_MEMBER_PREFIX)) {
            return -1;
        }
        String suffix = memberId.substring(
                ITHACA_FINAL_CENTER_WAVE_MEMBER_PREFIX.length());
        int separator = suffix.indexOf('_');
        if (separator <= 0 || separator != suffix.lastIndexOf('_')) {
            return -1;
        }
        try {
            int wave = Integer.parseInt(suffix.substring(0, separator));
            int slot = Integer.parseInt(suffix.substring(separator + 1));
            if (wave < 0 || wave >= getFinalCenterWaveCount()
                    || slot < 0 || slot >= getFinalCenterWaveSize()) {
                return -1;
            }
            return finalCenterWaveMemberId(wave, slot).equals(memberId)
                    ? wave : -1;
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static void addStrikeRoster(CampaignFleetAPI fleet, int tier) {
        int[] counts = POST_LABOR_STRIKE_COUNTS[tier];
        for (int role = 0; role < counts.length; role++) {
            String[] variants = STARVING_ROLE_VARIANTS[role];
            int offset = (int) (Math.random() * variants.length);
            for (int index = 0; index < counts[role]; index++) {
                addMember(fleet, variants[(offset + index) % variants.length]);
            }
        }
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_FLEET_TYPE,
                tier == THIRD_STRIKE
                        ? FleetTypes.PATROL_LARGE
                        : tier == SECOND_STRIKE
                                ? FleetTypes.PATROL_MEDIUM
                                : FleetTypes.PATROL_SMALL);
    }

    private static void configureMonthlyIthacaStrike(
            CampaignFleetAPI fleet,
            StarSystemAPI system,
            SectorEntityToken center) {
        if (TroyArrivalScript.isFleetBusyForMutation(fleet)) return;
        configureStarvingThreatIdentity(fleet);
        fleet.setFaction(IthacaThreatPolicy.FACTION_ID, true);
        fleet.setNoAutoDespawn(true);
        fleet.setAbortDespawn(true);
        fleet.setNoEngaging(0f);
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        memory.set(MemFlags.MEMORY_KEY_NO_JUMP, true);
        memory.set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        memory.set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE);
        memory.unset(MemFlags.NON_HOSTILE_OVERRIDES_MAKE_HOSTILE);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_HOSTILE);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE);
        memory.unset(MemFlags.MEMORY_KEY_PURSUE_PLAYER);
        memory.unset(MemFlags.FLEET_IGNORES_OTHER_FLEETS);
        memory.unset(MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS);

        int tier = memory.getInt(ITHACA_MONTHLY_STRIKE_TIER);
        fleet.setName(tier == SECOND_STRIKE
                ? "Starving Threat Second Strike"
                : "Starving Threat First Strike");
        if (fleet.getBattle() != null) {
            if (!memory.getBoolean(ITHACA_STRIKE_TRACE_BATTLE)) {
                logIthacaStrike("Fleet id=" + fleet.getId()
                        + " is currently in battle; assignment maintenance "
                        + "is paused.");
                memory.set(ITHACA_STRIKE_TRACE_BATTLE, true);
            }
            return;
        }
        memory.unset(ITHACA_STRIKE_TRACE_BATTLE);

        CampaignFleetAPI target = getMonthlyIthacaStrikeTarget(
                fleet, center);
        if (target == null) {
            if (!memory.contains(ITHACA_STRIKE_TRACE_PULSE)) {
                logIthacaStrikeWarning("Fleet id=" + fleet.getId()
                        + " has no surviving FOB Ithaca bastion target.");
                memory.set(ITHACA_STRIKE_TRACE_PULSE, true, 2f);
            }
            return;
        }

        SectorEntityToken targetToken = ensureIthacaStrikeTargetToken(
                system, target);
        if (targetToken == null) return;
        boolean validAssaultOrder = memory.getBoolean(
                ITHACA_STRIKE_ASSAULT_ORDERED)
                && isIthacaStrikeAssaultOrder(fleet, targetToken, target);
        if (!validAssaultOrder) {
            fleet.clearAssignments();
            // Nexerelin uses this exact sequence for Remnant station attacks.
            // INTERCEPT alone only tracks a fleet the attacker can see; the
            // invisible token supplies an exact destination for our hidden
            // campaign backing fleet, then GO_TO_LOCATION and INTERCEPT act as
            // close-range fallbacks once the target can be acquired.
            fleet.addAssignment(
                    FleetAssignment.ATTACK_LOCATION,
                    targetToken,
                    ITHACA_MONTHLY_STRIKE_LIFETIME_DAYS,
                    "attacking " + target.getName());
            fleet.addAssignment(
                    FleetAssignment.GO_TO_LOCATION,
                    targetToken,
                    ITHACA_MONTHLY_STRIKE_LIFETIME_DAYS,
                    "attacking " + target.getName());
            fleet.addAssignment(
                    FleetAssignment.INTERCEPT,
                    target,
                    ITHACA_MONTHLY_STRIKE_LIFETIME_DAYS,
                    "attacking " + target.getName());
            memory.set(ITHACA_STRIKE_ASSAULT_ORDERED, true);
            logIthacaStrike("Assigned fleet id=" + fleet.getId()
                    + " to Nex station-assault queue ATTACK_LOCATION -> "
                    + "GO_TO_LOCATION -> INTERCEPT for " + target.getName()
                    + ", targetId=" + target.getId()
                    + ", targetTokenId=" + targetToken.getId()
                    + ", targetFaction=" + target.getFaction().getId()
                    + ", relationship=" + fleet.getFaction()
                            .getRelationship(target.getFaction().getId())
                    + ", targetVisibleToFleet="
                    + target.isVisibleToSensorsOf(fleet)
                    + ", targetMakeNonHostile="
                    + target.getMemoryWithoutUpdate().getBoolean(
                            MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE)
                    + ", targetIgnoredByOtherFleets="
                    + target.getMemoryWithoutUpdate().getBoolean(
                            MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS)
                    + ", targetDistanceFromIthaca="
                    + Misc.getDistance(target, center)
                    + ", targetRadius=" + target.getRadius()
                    + ", distance=" + Misc.getDistance(fleet, target) + ".");
        }
        TacticalModulePlugin tactical = setIthacaStrikePriorityTarget(
                fleet, target);
        if (!memory.contains(ITHACA_STRIKE_TRACE_PULSE)) {
            logIthacaStrike("Status fleetId=" + fleet.getId()
                    + ", assignment="
                    + (fleet.getCurrentAssignment() == null
                            ? "none"
                            : fleet.getCurrentAssignment().getAssignment())
                    + ", assignmentTarget="
                    + (fleet.getCurrentAssignment() == null
                            || fleet.getCurrentAssignment().getTarget() == null
                            ? "none"
                            : fleet.getCurrentAssignment().getTarget().getId())
                    + ", intendedTarget=" + target.getId()
                    + ", targetToken=" + targetToken.getId()
                    + ", targetVisibleToFleet="
                    + target.isVisibleToSensorsOf(fleet)
                    + ", tacticalTarget="
                    + entityId(tactical == null
                            ? null : tactical.getTarget())
                    + ", priorityTarget="
                    + entityId(tactical == null
                            ? null : tactical.getPriorityTarget())
                    + ", distance=" + Misc.getDistance(fleet, target)
                    + ", despawning=" + fleet.isDespawning() + ".");
            memory.set(ITHACA_STRIKE_TRACE_PULSE, true, 2f);
        }
    }

    private static SectorEntityToken ensureIthacaStrikeTargetToken(
            StarSystemAPI system, CampaignFleetAPI target) {
        IthacaSectionEncounter.Section section =
                IthacaSectionEncounter.getSectionForStation(target);
        String suffix = section == null ? target.getId() : section.id;
        String tokenId = ITHACA_STRIKE_TARGET_TOKEN_PREFIX + suffix;
        SectorEntityToken token = system.getEntityById(tokenId);
        if (token == null) {
            token = system.createToken(
                    target.getLocation().x, target.getLocation().y);
            token.setId(tokenId);
            token.setName(target.getName() + " assault coordinate");
            markOwnedProxyToken(token);
            system.addEntity(token);
            logIthacaStrike("Created invisible attack-location token id="
                    + tokenId + " for target=" + target.getId() + ".");
        } else if (!isOwnedProxyToken(token)) {
            logBlockedProxyTokenOnce(tokenId, token);
            return null;
        }
        if (token.getOrbitFocus() != target
                || Math.abs(token.getCircularOrbitRadius()
                        - ITHACA_STRIKE_TARGET_TOKEN_RADIUS) > 1f) {
            token.setCircularOrbit(
                    target,
                    0f,
                    ITHACA_STRIKE_TARGET_TOKEN_RADIUS,
                    ITHACA_STRIKE_TARGET_TOKEN_PERIOD);
        }
        return token;
    }

    private static boolean isIthacaStrikeAssaultOrder(
            CampaignFleetAPI fleet,
            SectorEntityToken targetToken,
            CampaignFleetAPI target) {
        if (fleet.getCurrentAssignment() == null) return false;
        FleetAssignment assignment = fleet.getCurrentAssignment()
                .getAssignment();
        SectorEntityToken assignmentTarget = fleet.getCurrentAssignment()
                .getTarget();
        return (assignment == FleetAssignment.ATTACK_LOCATION
                        && assignmentTarget == targetToken)
                || (assignment == FleetAssignment.GO_TO_LOCATION
                        && assignmentTarget == targetToken)
                || (assignment == FleetAssignment.INTERCEPT
                        && assignmentTarget == target);
    }

    private static TacticalModulePlugin setIthacaStrikePriorityTarget(
            CampaignFleetAPI fleet, CampaignFleetAPI target) {
        if (!(fleet.getAI() instanceof ModularFleetAIAPI)) return null;
        TacticalModulePlugin tactical = ((ModularFleetAIAPI) fleet.getAI())
                .getTacticalModule();
        if (tactical != null && tactical.getPriorityTarget() != target) {
            // Nexerelin's VengeanceFleetIntel uses a priority target alongside
            // ATTACK_LOCATION when the real fleet is outside sensor contact.
            tactical.setPriorityTarget(
                    target, ITHACA_STRIKE_PRIORITY_DAYS, false);
        }
        return tactical;
    }

    private static String entityId(SectorEntityToken entity) {
        return entity == null || entity.getId() == null
                ? "none" : entity.getId();
    }

    private static CampaignFleetAPI getMonthlyIthacaStrikeTarget(
            CampaignFleetAPI fleet, SectorEntityToken center) {
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        IthacaSectionEncounter.Section stored = IthacaSectionEncounter.Section
                .fromId(memory.getString(ITHACA_MONTHLY_STRIKE_TARGET));
        CampaignFleetAPI target = stored == null
                ? null : IthacaSectionEncounter.getStation(center, stored);
        if (target != null && !target.isEmpty()
                && !IthacaSectionEncounter.isDisabled(center, stored)) {
            return target;
        }
        if (stored != null) {
            logIthacaStrikeWarning("Stored target " + stored.id
                    + " is missing, empty, or disabled for fleet id="
                    + fleet.getId() + "; selecting another bastion.");
        }

        ArrayList<CampaignFleetAPI> available =
                new ArrayList<CampaignFleetAPI>();
        for (IthacaSectionEncounter.Section section
                : IthacaSectionEncounter.Section.values()) {
            CampaignFleetAPI candidate = IthacaSectionEncounter.getStation(
                    center, section);
            if (candidate != null && !candidate.isEmpty()
                    && !IthacaSectionEncounter.isDisabled(center, section)) {
                available.add(candidate);
            }
        }
        if (available.isEmpty()) {
            logIthacaStrikeWarning("No available bastions for fleet id="
                    + fleet.getId() + ".");
            return null;
        }
        target = available.get((int) (Math.random() * available.size()));
        IthacaSectionEncounter.Section section =
                IthacaSectionEncounter.getSectionForStation(target);
        if (section != null) {
            memory.set(ITHACA_MONTHLY_STRIKE_TARGET, section.id);
        }
        logIthacaStrike("Selected target " + target.getName()
                + " for fleet id=" + fleet.getId() + ".");
        return target;
    }

    private static CampaignFleetAPI getNearestIthacaStrikeTarget(
            SectorEntityToken center, SectorEntityToken origin) {
        CampaignFleetAPI nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (IthacaSectionEncounter.Section section
                : IthacaSectionEncounter.Section.values()) {
            CampaignFleetAPI candidate = IthacaSectionEncounter.getStation(
                    center, section);
            if (candidate == null || candidate.isEmpty()
                    || IthacaSectionEncounter.isDisabled(center, section)) {
                continue;
            }
            float distance = Misc.getDistance(origin, candidate);
            logIthacaStrike("Console target candidate section=" + section.id
                    + ", fleetId=" + candidate.getId()
                    + ", distance=" + distance + ".");
            if (nearest == null || distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }
        if (nearest != null) {
            logIthacaStrike("Console selected nearest target="
                    + nearest.getName() + ", targetId=" + nearest.getId()
                    + ", distance=" + nearestDistance + ".");
        }
        return nearest;
    }

    private static String strikeTierName(int tier) {
        return tier == THIRD_STRIKE
                ? "Third Strike"
                : tier == SECOND_STRIKE
                        ? "Second Strike"
                        : "First Strike";
    }

    private static void logIthacaStrike(String message) {
        Global.getLogger(OdysseyPredatorScript.class).info(
                "[ITHACA_STRIKE] " + message);
    }

    private static void logIthacaStrikeWarning(String message) {
        Global.getLogger(OdysseyPredatorScript.class).warn(
                "[ITHACA_STRIKE] " + message);
    }

    private static void ensureStarvingPopulation(
            StarSystemAPI system,
            String idPrefix,
            int count,
            float[][] offsets,
            String assignmentText) {
        if (system == null) return;

        for (int index = 0; index < count; index++) {
            String id = idPrefix + index;
            String pending = "$" + id + "_replacement_pending";
            String cooldown = "$" + id + "_replacement_cooldown";
            SectorEntityToken existing = system.getEntityById(id);
            if (existing instanceof CampaignFleetAPI
                    && (isPlayerControlledFleet((CampaignFleetAPI) existing)
                        || !((CampaignFleetAPI) existing)
                                .getMemoryWithoutUpdate().getBoolean(
                                        STARVING_THREAT_MARKER))) {
                // A foreign/player claimant is serialized truth. Leave it in
                // place and suppress this population slot.
                continue;
            }
            if (existing instanceof CampaignFleetAPI
                    && TroyArrivalScript.isFleetBusyForMutation(
                            (CampaignFleetAPI) existing)) {
                continue;
            }
            if (existing instanceof CampaignFleetAPI
                    && !((CampaignFleetAPI) existing).isEmpty()) {
                system.getMemoryWithoutUpdate().unset(pending);
                system.getMemoryWithoutUpdate().unset(cooldown);
                system.getMemoryWithoutUpdate().set("$" + id + "_was_present", true);
                configureStarvingFleet(
                        (CampaignFleetAPI) existing,
                        system,
                        assignmentText);
                continue;
            }
            if (existing instanceof CampaignFleetAPI) {
                system.removeEntity(existing);
            } else if (existing != null) {
                // Do not replace a wrong-type serialized object.
                continue;
            }
            // Defeating an Ithaca attacker buys three campaign days instead
            // of another fleet appearing on the next half-second update.
            if (ASHEN_STARVING_PREFIX.equals(idPrefix)
                    && system.getMemoryWithoutUpdate().getBoolean("$" + id + "_was_present")) {
                if (!system.getMemoryWithoutUpdate().getBoolean(pending)) {
                    system.getMemoryWithoutUpdate().set(pending, true);
                    system.getMemoryWithoutUpdate().set(cooldown, true, 3f);
                }
                if (system.getMemoryWithoutUpdate().contains(cooldown)) continue;
            }
            spawnStarvingFleet(
                    system,
                    idPrefix,
                    index,
                    offsets,
                    assignmentText);
            system.getMemoryWithoutUpdate().unset(pending);
            system.getMemoryWithoutUpdate().unset(cooldown);
            system.getMemoryWithoutUpdate().set("$" + id + "_was_present", true);
        }
    }

    /**
     * Maintains sparse local patrols without feeding them through the
     * post-Labor Strike upgrader used by the major Starving populations.
     */
    private static void ensureRegionalStarvingPatrols(
            StarSystemAPI system,
            String idPrefix,
            int count,
            float[][] offsets,
            String assignmentText) {
        if (system == null) return;

        for (int index = 0; index < count; index++) {
            String id = idPrefix + index;
            String wasPresent = "$" + id + "_was_present";
            String replacementPending = "$" + id + "_replacement_pending";
            String cooldown = "$" + id + "_replacement_cooldown";
            SectorEntityToken existing = system.getEntityById(id);
            if (existing instanceof CampaignFleetAPI
                    && (isPlayerControlledFleet((CampaignFleetAPI) existing)
                        || !((CampaignFleetAPI) existing)
                                .getMemoryWithoutUpdate().getBoolean(
                                        REGIONAL_SMALL_PATROL_MARKER))) {
                continue;
            }
            if (existing instanceof CampaignFleetAPI
                    && TroyArrivalScript.isFleetBusyForMutation(
                            (CampaignFleetAPI) existing)) {
                continue;
            }
            if (existing instanceof CampaignFleetAPI
                    && !((CampaignFleetAPI) existing).isEmpty()) {
                system.getMemoryWithoutUpdate().unset(cooldown);
                system.getMemoryWithoutUpdate().unset(replacementPending);
                system.getMemoryWithoutUpdate().set(wasPresent, true);
                configureRegionalStarvingPatrol(
                        (CampaignFleetAPI) existing, system, assignmentText);
                continue;
            }
            if (existing instanceof CampaignFleetAPI) {
                system.removeEntity(existing);
            } else if (existing != null) {
                continue;
            }
            if (system.getMemoryWithoutUpdate().getBoolean(wasPresent)) {
                if (!system.getMemoryWithoutUpdate().getBoolean(
                        replacementPending)) {
                    system.getMemoryWithoutUpdate().set(
                            replacementPending, true);
                    system.getMemoryWithoutUpdate().set(
                            cooldown, true, 6f + (float) Math.random() * 8f);
                    continue;
                }
                if (system.getMemoryWithoutUpdate().contains(cooldown)) {
                    continue;
                }
            }
            spawnRegionalStarvingPatrol(
                    system, id, offsets[index], assignmentText);
            system.getMemoryWithoutUpdate().unset(replacementPending);
            system.getMemoryWithoutUpdate().unset(cooldown);
            system.getMemoryWithoutUpdate().set(wasPresent, true);
        }
    }

    private static void spawnRegionalStarvingPatrol(
            StarSystemAPI system,
            String id,
            float[] offset,
            String assignmentText) {
        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(
                Factions.THREAT, "Starving Threat Patrol", true);
        fleet.setId(id);
        fleet.setName("Starving Threat Patrol");
        fleet.setNoFactionInName(true);
        fleet.setTransponderOn(false);
        fleet.getMemoryWithoutUpdate().set(
                REGIONAL_SMALL_PATROL_MARKER, true);

        int size = 2 + (int) (Math.random() * 3f);
        boolean lineUnit = Math.random() < REGIONAL_LINE_CHANCE;
        for (int slot = 0; slot < size; slot++) {
            String variant;
            if (lineUnit && slot == 0) {
                variant = REGIONAL_LINE_VARIANTS[(int) (Math.random()
                        * REGIONAL_LINE_VARIANTS.length)];
            } else {
                variant = REGIONAL_SMALL_PATROL_VARIANTS[(int) (Math.random()
                        * REGIONAL_SMALL_PATROL_VARIANTS.length)];
            }
            addMember(fleet, variant);
        }
        if (!fleet.getFleetData().getMembersListCopy().isEmpty()) {
            fleet.getFleetData().setFlagship(
                    fleet.getFleetData().getMembersListCopy().get(0));
        }
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
        system.addEntity(fleet);
        fleet.setLocation(offset[0], offset[1]);
        configureRegionalStarvingPatrol(fleet, system, assignmentText);
    }

    private static void configureRegionalStarvingPatrol(
            CampaignFleetAPI fleet,
            StarSystemAPI system,
            String assignmentText) {
        if (TroyArrivalScript.isFleetBusyForMutation(fleet)) return;
        configureStarvingThreatIdentity(fleet);
        fleet.setFaction(Factions.THREAT, true);
        fleet.setName("Starving Threat Patrol");
        configureHostileFleet(fleet);
        clearIthacaProtectionOverride(fleet);
        if (!protectAlphaStarvingArrival(fleet, system)) {
            clearTroyArrivalProtection(fleet);
        }
        fleet.getMemoryWithoutUpdate().set(
                REGIONAL_SMALL_PATROL_MARKER, true);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_NO_JUMP, true);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_FLEET_TYPE, FleetTypes.PATROL_SMALL);
        if (fleet.getCurrentAssignment() == null) {
            fleet.addAssignment(
                    FleetAssignment.PATROL_SYSTEM,
                    system.getCenter(),
                    1000000f,
                    assignmentText);
        }
    }

    private static void spawnStarvingFleet(
            StarSystemAPI system,
            String idPrefix,
            int index,
            float[][] offsets,
            String assignmentText) {
        String factionId = ASHEN_STARVING_PREFIX.equals(idPrefix)
                ? IthacaThreatPolicy.FACTION_ID : Factions.THREAT;
        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(
                factionId, "Starving Threat", true);
        fleet.setId(idPrefix + index);
        fleet.setName("Starving Threat");
        fleet.setNoFactionInName(true);
        fleet.setTransponderOn(false);

        int groups = DEVOURED_STARVING_PREFIX.equals(idPrefix) ? 3 : 1;
        for (int group = 0; group < groups; group++) {
            String[] loadout = STARVING_LOADOUTS[
                    (index + group) % STARVING_LOADOUTS.length];
            for (String variantId : loadout) addMember(fleet, variantId);
        }
        if (!fleet.getFleetData().getMembersListCopy().isEmpty()) {
            fleet.getFleetData().setFlagship(
                    fleet.getFleetData().getMembersListCopy().get(0));
        }
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();

        system.addEntity(fleet);
        fleet.setLocation(
                offsets[index][0],
                offsets[index][1]);
        configureStarvingFleet(fleet, system, assignmentText);
    }

    private static void configureStarvingFleet(
            CampaignFleetAPI fleet,
            StarSystemAPI system,
            String assignmentText) {
        if (TroyArrivalScript.isFleetBusyForMutation(fleet)) return;
        ensurePostLaborStrikeRoster(fleet, getPostLaborStrikeTier(fleet));
        configureStarvingThreatIdentity(fleet);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_NO_JUMP, true);
        if (OdysseyExpanseSystem.DEVOURED_REACH_ID.equals(
                system.getOptionalUniqueId())) {
            configureDevouredStarvingBehavior(fleet, system);
            return;
        }
        if (OdysseyExpanseSystem.ASHEN_VERGE_ID.equals(
                system.getOptionalUniqueId())) {
            if (!IthacaThreatPolicy.apply(fleet, system)) {
                configureAshenStarvingBehavior(fleet, system);
            }
            return;
        }

        fleet.setFaction(Factions.THREAT, true);
        configureHostileFleet(fleet);
        clearIthacaProtectionOverride(fleet);
        if (fleet.getCurrentAssignment() != null
                && IthacaSectionEncounter.isSectionStation(
                        fleet.getCurrentAssignment().getTarget())) {
            fleet.clearAssignments();
        }
        if (fleet.getCurrentAssignment() == null) {
            fleet.addAssignment(
                    FleetAssignment.PATROL_SYSTEM,
                    system.getCenter(),
                    1000000f,
                    assignmentText);
        }
    }

    private static void configureDevouredStarvingBehavior(
            CampaignFleetAPI fleet, StarSystemAPI system) {
        fleet.setName("Starving Threat Feeding Mass");
        fleet.setFaction(Factions.THREAT, true);
        configureHostileFleet(fleet);
        clearIthacaProtectionOverride(fleet);

        int index = getFleetIndex(fleet, DEVOURED_STARVING_PREFIX,
                DEVOURED_STARVING_COUNT);
        SectorEntityToken ring = system.getEntityById(
                OdysseyExpanseSystem.DEVOURED_RING_ID);
        if (!OdysseyExpanseSystem.isDevouredRing(ring)) ring = null;
        boolean ringbound = index < 16 && ring != null;
        SectorEntityToken target = ringbound
                ? ring
                : OdysseyExpanseSystem.getDevouredWorld((index - 16) % 3);
        if (target == null) target = ring != null ? ring : system.getCenter();
        String text = ringbound
                ? "rammed against the Devoured Ring"
                : "feeding on " + target.getName();
        if (fleet.getCurrentAssignment() == null
                || fleet.getCurrentAssignment().getAssignment()
                        != FleetAssignment.ORBIT_AGGRESSIVE
                || fleet.getCurrentAssignment().getTarget() != target) {
            fleet.clearAssignments();
            fleet.addAssignment(
                    FleetAssignment.ORBIT_AGGRESSIVE,
                    target,
                    1000000f,
                    text);
        }
    }

    private static int getFleetIndex(
            CampaignFleetAPI fleet, String prefix, int count) {
        String id = fleet.getId();
        if (id != null && id.startsWith(prefix)) {
            try {
                return Math.abs(Integer.parseInt(
                        id.substring(prefix.length()))) % count;
            } catch (NumberFormatException ignored) {
                // Fall through to the stable ID hash.
            }
        }
        int hash = id == null ? 0 : id.hashCode();
        return (hash & Integer.MAX_VALUE) % count;
    }

    /**
     * Every Ashen patrol is leashed to one surviving Ithaca bastion. The
     * explicit intercept is the only hostile assignment these fleets receive;
     * the caller may temporarily interrupt it for a visible, nearby opportunity.
     */
    private static void configureAshenStarvingBehavior(
            CampaignFleetAPI fleet, StarSystemAPI system) {
        SectorEntityToken center = system.getEntityById(
                IthacaSectionEncounter.FOB_ENTITY_ID);
        if (!OdysseyExpanseSystem.isFobIthaca(center)) return;
        int index = getAshenPatrolIndex(fleet);
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE);
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE);
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_MAKE_HOSTILE);
        fleet.setNoEngaging(0f);

        ArrayList<CampaignFleetAPI> bastions = new ArrayList<CampaignFleetAPI>();
        for (IthacaSectionEncounter.Section section
                : IthacaSectionEncounter.Section.values()) {
            CampaignFleetAPI station = IthacaSectionEncounter.getStation(
                    center, section);
            if (station != null && !station.isEmpty()
                    && !IthacaSectionEncounter.isDisabled(center, section)) {
                bastions.add(station);
            }
        }

        SectorEntityToken target = bastions.isEmpty()
                ? center : bastions.get(index % bastions.size());
        FleetAssignment assignment = bastions.isEmpty()
                ? FleetAssignment.ORBIT_PASSIVE : FleetAssignment.INTERCEPT;
        String assignmentText = bastions.isEmpty()
                ? "holding outside the silent Ithaca ring"
                : "assaulting " + target.getName();

        if (target != null && (fleet.getCurrentAssignment() == null
                || fleet.getCurrentAssignment().getAssignment()
                        != assignment
                || fleet.getCurrentAssignment().getTarget() != target)) {
            fleet.clearAssignments();
            fleet.addAssignment(
                    assignment,
                    target,
                    1000000f,
                    assignmentText);
        }
    }

    private static int getAshenPatrolIndex(CampaignFleetAPI fleet) {
        String id = fleet.getId();
        if (id != null && id.startsWith(ASHEN_STARVING_PREFIX)) {
            try {
                return Math.abs(Integer.parseInt(
                        id.substring(ASHEN_STARVING_PREFIX.length())))
                        % ASHEN_STARVING_PATROL_COUNT;
            } catch (NumberFormatException ignored) {
                // Fall through to the stable ID hash for migrated oddities.
            }
        }
        int hash = id == null ? 0 : id.hashCode();
        return (hash & Integer.MAX_VALUE) % ASHEN_STARVING_PATROL_COUNT;
    }


    private static void clearIthacaProtectionOverride(
            CampaignFleetAPI fleet) {
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE);
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.NON_HOSTILE_OVERRIDES_MAKE_HOSTILE);
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.FLEET_IGNORES_OTHER_FLEETS);
    }

    /** Runs Labor V's three perimeter assaults, then the center breach. */
    private static void ensureFinalLaborInvasion() {
        StarSystemAPI ashen = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.ASHEN_VERGE_ID);
        if (ashen == null) return;
        if (!MenelausTrial.isFinalLaborActive()) {
            clearFinalCenterDefenderMarkers(ashen);
            if (OdysseyExpanseSystem
                    .isFobIthacaFinalLaborAttritionLocked(ashen)) {
                OdysseyExpanseSystem.endFobIthacaFinalLaborAttrition(
                        ashen, true);
            }
            return;
        }

        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        if (!OdysseyExpanseSystem
                .isFobIthacaFinalLaborAttritionLocked(ashen)) {
            OdysseyExpanseSystem.beginFobIthacaFinalLaborAttrition(ashen);
        }
        if (!memory.getBoolean(ITHACA_FINAL_LABOR_INITIALIZED)) {
            if (!TroyArrivalScript.retireGautamaForFinalLabor()) return;
            if (spawnIthacaEndgameInvasionFleets(true).size()
                    != IthacaSectionEncounter.Section.values().length) return;
            memory.set(ITHACA_FINAL_LABOR_INITIALIZED, true);
            FinalLaborMusic.playMissionStart();
            logIthacaStrike("Labor V initialized: three themed Strikes are "
                    + "attacking Ithaca's perimeter bastions.");
        }

        if (getFinalLaborInvasionProgress()
                < IthacaSectionEncounter.Section.values().length) {
            memory.set(ITHACA_ENDGAME_INVASION_ACTIVE, true);
            FinalLaborMusic.maintainMissionMusic();
            return;
        }

        memory.set(ITHACA_ENDGAME_INVASION_ACTIVE, false);
        // Keep the instrumental alive while the final fleet closes on the
        // gate. The battle plugin is the only path that replaces it with the
        // vocal version.
        FinalLaborMusic.maintainMissionMusic();
        ensureFinalCenterBreach(ashen);
    }

    /**
     * Removes an invasion leaked by the former Mara-completion gate. This is
     * deliberately limited to the unreported/unbriefed window and leaves an
     * active battle alone until it has closed.
     */
    private static void clearPrematureFinalLaborInvasion(
            StarSystemAPI ashen) {
        SectorEntityToken center = ashen.getEntityById(
                IthacaSectionEncounter.FOB_ENTITY_ID);
        if (!OdysseyExpanseSystem.isFobIthaca(center)) return;
        ArrayList<SectorEntityToken> remove =
                new ArrayList<SectorEntityToken>();
        SectorEntityToken finale = ashen.getEntityById(
                ITHACA_FINAL_CENTER_FLEET_ID);
        if (finale instanceof CampaignFleetAPI
                && isOwnedFinalLaborCenterFleet((CampaignFleetAPI) finale)
                && TroyArrivalScript.isFleetBusyForMutation(
                        (CampaignFleetAPI) finale)) {
            return;
        }
        if (finale instanceof CampaignFleetAPI
                && isOwnedFinalLaborCenterFleet((CampaignFleetAPI) finale)) {
            remove.add(finale);
        }
        SectorEntityToken centerTarget = ashen.getEntityById(
                ITHACA_FINAL_CENTER_TARGET_ID);
        if (isOwnedProxyToken(centerTarget)) remove.add(centerTarget);
        for (IthacaSectionEncounter.Section section
                : IthacaSectionEncounter.Section.values()) {
            SectorEntityToken fleet = ashen.getEntityById(
                    ITHACA_ENDGAME_CONSOLE_PREFIX + section.id);
            if (fleet instanceof CampaignFleetAPI
                    && isFinalLaborPerimeterFleet((CampaignFleetAPI) fleet)
                    && TroyArrivalScript.isFleetBusyForMutation(
                            (CampaignFleetAPI) fleet)) {
                return;
            }
            if (fleet instanceof CampaignFleetAPI
                    && isFinalLaborPerimeterFleet((CampaignFleetAPI) fleet)) {
                remove.add(fleet);
            }
            SectorEntityToken siege = ashen.getEntityById(
                    ITHACA_FINAL_LABOR_SIEGE_TOKEN_PREFIX + section.id);
            if (isOwnedProxyToken(siege)) remove.add(siege);
        }
        if (remove.isEmpty() && !Global.getSector().getMemoryWithoutUpdate()
                .getBoolean(ITHACA_FINAL_LABOR_INITIALIZED)) {
            return;
        }
        for (SectorEntityToken entity : remove) {
            ashen.removeEntity(entity);
        }

        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        memory.unset(ITHACA_FINAL_LABOR_INITIALIZED);
        memory.unset(ITHACA_ENDGAME_INVASION_ACTIVE);
        memory.unset(ITHACA_FINAL_CENTER_ASSAULT_ORDERED);
        memory.unset(ITHACA_FINAL_CENTER_CREATED_V1);
        memory.unset(ITHACA_FINAL_LABOR_PHASE);
        memory.unset(ITHACA_FINAL_LABOR_TFS_TARGET);
        for (IthacaSectionEncounter.Section section
                : IthacaSectionEncounter.Section.values()) {
            memory.unset(ithacaEndgamePlayerIntervenedKey(section));
            memory.unset(ithacaFinalLaborLaneRepelledKey(section));
            memory.unset(ithacaFinalLaborSiegeStartedKey(section));
            memory.unset(ithacaFinalLaborBastionLostKey(section));
        }
        clearFinalCenterDefenderMarkers(ashen);
        OdysseyExpanseSystem.endFobIthacaFinalLaborAttrition(ashen, true);
        if (center != null) {
            IthacaSectionEncounter.repairAllBastionsForFinalLabor(center);
        }
        StarSystemAPI devoured = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.DEVOURED_REACH_ID);
        SectorEntityToken ring = devoured == null ? null
                : devoured.getEntityById(
                        OdysseyExpanseSystem.DEVOURED_RING_ID);
        if (devoured != null && OdysseyExpanseSystem.isDevouredRing(ring)) {
            TroyArrivalScript.spawnStarvingThreatSample(devoured, ring);
        }
        logIthacaStrike("Removed premature Labor V invasion; awaiting "
                + "the player's report to Menelaus and final briefing.");
    }

    /**
     * Console-test hook: rewinds only Labor V's encounter state and creates a
     * clean opening wave. Earlier Labor objectives and rewards are untouched.
     */
    public static int restartFinalLaborForTesting() {
        if (Global.getSector() == null
                || !MenelausTrial.isFinalLaborActive()) {
            return FORCE_FINAL_LABOR_MISSING_WORLD;
        }
        StarSystemAPI ashen = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.ASHEN_VERGE_ID);
        if (ashen == null) return FORCE_FINAL_LABOR_MISSING_WORLD;
        SectorEntityToken center = ashen.getEntityById(
                IthacaSectionEncounter.FOB_ENTITY_ID);
        if (!OdysseyExpanseSystem.isFobIthaca(center)) {
            return FORCE_FINAL_LABOR_MISSING_WORLD;
        }

        SectorEntityToken finale = ashen.getEntityById(
                ITHACA_FINAL_CENTER_FLEET_ID);
        if (finale instanceof CampaignFleetAPI
                && TroyArrivalScript.isFleetBusyForMutation(
                        (CampaignFleetAPI) finale)) {
            return FORCE_FINAL_LABOR_BUSY;
        }
        for (IthacaSectionEncounter.Section section
                : IthacaSectionEncounter.Section.values()) {
            SectorEntityToken existing = ashen.getEntityById(
                    ITHACA_ENDGAME_CONSOLE_PREFIX + section.id);
            if (existing instanceof CampaignFleetAPI
                    && TroyArrivalScript.isFleetBusyForMutation(
                            (CampaignFleetAPI) existing)) {
                return FORCE_FINAL_LABOR_BUSY;
            }
        }
        if (!TroyArrivalScript.retireGautamaForFinalLabor()) {
            return FORCE_FINAL_LABOR_BUSY;
        }

        clearFinalCenterDefenderMarkers(ashen);
        OdysseyExpanseSystem.endFobIthacaFinalLaborAttrition(
                ashen, true);
        finale = ashen.getEntityById(ITHACA_FINAL_CENTER_FLEET_ID);
        if (finale instanceof CampaignFleetAPI
                && isOwnedFinalLaborCenterFleet(
                        (CampaignFleetAPI) finale)) {
            ashen.removeEntity(finale);
        } else if (finale != null) {
            return FORCE_FINAL_LABOR_BUSY;
        }
        SectorEntityToken centerTarget = ashen.getEntityById(
                ITHACA_FINAL_CENTER_TARGET_ID);
        if (isOwnedProxyToken(centerTarget)) {
            ashen.removeEntity(centerTarget);
        } else if (centerTarget != null) {
            logBlockedProxyTokenOnce(
                    ITHACA_FINAL_CENTER_TARGET_ID, centerTarget);
            return FORCE_FINAL_LABOR_BUSY;
        }

        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        memory.unset(ITHACA_FINAL_LABOR_INITIALIZED);
        memory.unset(ITHACA_ENDGAME_INVASION_ACTIVE);
        memory.unset(ITHACA_FINAL_CENTER_ASSAULT_ORDERED);
        memory.unset(ITHACA_FINAL_CENTER_CREATED_V1);
        for (IthacaSectionEncounter.Section section
                : IthacaSectionEncounter.Section.values()) {
            memory.unset(ithacaEndgamePlayerIntervenedKey(section));
            memory.unset(ithacaFinalLaborLaneRepelledKey(section));
            memory.unset(ithacaFinalLaborSiegeStartedKey(section));
            memory.unset(ithacaFinalLaborBastionLostKey(section));
        }

        ensureFinalLaborInvasion();
        int spawned = 0;
        for (IthacaSectionEncounter.Section section
                : IthacaSectionEncounter.Section.values()) {
            SectorEntityToken entity = ashen.getEntityById(
                    ITHACA_ENDGAME_CONSOLE_PREFIX + section.id);
            if (entity instanceof CampaignFleetAPI
                    && !((CampaignFleetAPI) entity).isEmpty()) {
                spawned++;
            }
        }
        StarSystemAPI expanse = OdysseyExpanseSystem.findExisting();
        if (expanse != null) MenelausTrial.refreshProgress(expanse);
        logIthacaStrike("Console forced Labor V: opening fleets="
                + spawned + ", progress reset, center breach cleared.");
        return spawned;
    }

    private static void ensureFinalCenterBreach(StarSystemAPI system) {
        SectorEntityToken center = system.getEntityById(
                IthacaSectionEncounter.FOB_ENTITY_ID);
        if (!OdysseyExpanseSystem.isFobIthaca(center)) return;
        CampaignFleetAPI gateDefense = OdysseyExpanseSystem
                .ensureFobIthacaGateDefenseStation(system);
        if (gateDefense == null || gateDefense.isEmpty()) return;
        if (TroyArrivalScript.isFleetBusyForMutation(gateDefense)) return;
        suppressPerimeterBastionReinforcements(center, true);
        SectorEntityToken existing = system.getEntityById(
                ITHACA_FINAL_CENTER_FLEET_ID);
        CampaignFleetAPI fleet = existing instanceof CampaignFleetAPI
                ? (CampaignFleetAPI) existing : null;
        if (existing != null && fleet == null) return;
        if (fleet != null) {
            if (!isOwnedFinalLaborCenterFleet(fleet)) return;
            Global.getSector().getMemoryWithoutUpdate().set(
                    ITHACA_FINAL_CENTER_CREATED_V1, true);
            if (fleet.isEmpty()
                    || TroyArrivalScript.isFleetBusyForMutation(fleet)) {
                return;
            }
        } else {
            if (Global.getSector().getMemoryWithoutUpdate().getBoolean(
                    ITHACA_FINAL_CENTER_CREATED_V1)) return;
            Global.getSector().getMemoryWithoutUpdate().set(
                    ITHACA_FINAL_CENTER_CREATED_V1, true);
            fleet = createFinalCenterStrikeFleet(
                    ITHACA_FINAL_CENTER_FLEET_ID);
            if (fleet == null) return;
            system.addEntity(fleet);
            fleet.setLocation(
                    gateDefense.getLocation().x
                            - ITHACA_FINAL_CENTER_SPAWN_DISTANCE,
                    gateDefense.getLocation().y);
            logIthacaStrike("Labor V Breach Defense assault spawned id="
                    + fleet.getId() + ", members="
                    + fleet.getFleetData().getMembersListCopy().size()
                    + ", Gautama=true.");
        }
        configureFinalCenterBreach(fleet, system, gateDefense);
    }

    private static void configureFinalCenterBreach(
            CampaignFleetAPI fleet,
            StarSystemAPI system,
            CampaignFleetAPI gateDefense) {
        if (TroyArrivalScript.isFleetBusyForMutation(fleet)) return;
        configureStarvingThreatIdentity(fleet);
        ensureIthacaSiegeStalemateHullmods(fleet);
        fleet.setName("Heavenly Strike - Gate Assault");
        EncounterCombatChatter.configure(fleet, EncounterCombatChatter.HEAVENLY);
        fleet.setFaction(IthacaThreatPolicy.FACTION_ID, true);
        fleet.setNoAutoDespawn(true);
        fleet.setAbortDespawn(true);
        fleet.setNoEngaging(0f);
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        memory.set(ITHACA_FINAL_CENTER_BATTLE_MARKER, true);
        memory.set(MusicPlayerPluginImpl.COMBAT_MUSIC_SET_MEM_KEY,
                FinalLaborMusic.FINAL_FIGHT_ID);
        memory.unset(ITHACA_MONTHLY_STRIKE_ACTIVE);
        memory.set(MemFlags.MEMORY_KEY_NO_JUMP, true);
        memory.set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        memory.set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE);
        memory.unset(MemFlags.NON_HOSTILE_OVERRIDES_MAKE_HOSTILE);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE);
        memory.unset(MemFlags.MEMORY_KEY_PURSUE_PLAYER);
        ensureFinalCenterBattleListener(fleet);
        gateDefense.getMemoryWithoutUpdate().set(
                ITHACA_FINAL_CENTER_BATTLE_MARKER, true);
        gateDefense.getMemoryWithoutUpdate().unset(
                MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS);
        blockAmbientBreachDefenseAttacks(system, gateDefense);
        assignSpartanCenterDefenders(system, fleet, gateDefense);
        if (fleet.getBattle() != null) return;

        SectorEntityToken targetToken = ensureFinalCenterTargetToken(
                system, gateDefense);
        if (targetToken == null) return;
        if (!memory.getBoolean(ITHACA_FINAL_CENTER_ASSAULT_ORDERED)
                || !isFinalCenterAssaultOrder(
                        fleet, targetToken, gateDefense)) {
            fleet.clearAssignments();
            fleet.addAssignment(
                    FleetAssignment.ATTACK_LOCATION,
                    targetToken,
                    ITHACA_ENDGAME_INVASION_DAYS,
                    "attacking Breach Defense Station");
            fleet.addAssignment(
                    FleetAssignment.GO_TO_LOCATION,
                    targetToken,
                    ITHACA_ENDGAME_INVASION_DAYS,
                    "closing on Breach Defense Station");
            fleet.addAssignment(
                    FleetAssignment.INTERCEPT,
                    gateDefense,
                    ITHACA_ENDGAME_INVASION_DAYS,
                    "attacking Breach Defense Station");
            memory.set(ITHACA_FINAL_CENTER_ASSAULT_ORDERED, true);
            logIthacaStrike("Labor V final assault ordered fleet="
                    + fleet.getId() + ", token=" + targetToken.getId()
                    + ", target=" + gateDefense.getId()
                    + " using ATTACK_LOCATION -> GO_TO_LOCATION -> "
                    + "INTERCEPT.");
        }
        lockFinalCenterBreachTarget(fleet, system, gateDefense);
    }

    private static SectorEntityToken ensureFinalCenterTargetToken(
            StarSystemAPI system, CampaignFleetAPI gateDefense) {
        SectorEntityToken token = system.getEntityById(
                ITHACA_FINAL_CENTER_TARGET_ID);
        if (token == null) {
            token = system.createToken(
                    gateDefense.getLocation().x,
                    gateDefense.getLocation().y);
            token.setId(ITHACA_FINAL_CENTER_TARGET_ID);
            token.setName("Breach Defense Station assault coordinate");
            markOwnedProxyToken(token);
            system.addEntity(token);
        } else if (!isOwnedProxyToken(token)) {
            logBlockedProxyTokenOnce(ITHACA_FINAL_CENTER_TARGET_ID, token);
            return null;
        }
        if (token.getOrbitFocus() != gateDefense
                || Math.abs(token.getCircularOrbitRadius()
                        - ITHACA_STRIKE_TARGET_TOKEN_RADIUS) > 1f) {
            token.setCircularOrbit(
                    gateDefense,
                    180f,
                    ITHACA_STRIKE_TARGET_TOKEN_RADIUS,
                    ITHACA_STRIKE_TARGET_TOKEN_PERIOD);
        }
        return token;
    }

    private static boolean isFinalCenterAssaultOrder(
            CampaignFleetAPI fleet,
            SectorEntityToken targetToken,
            CampaignFleetAPI gateDefense) {
        if (fleet.getCurrentAssignment() == null) return false;
        FleetAssignment assignment = fleet.getCurrentAssignment()
                .getAssignment();
        SectorEntityToken assignmentTarget = fleet.getCurrentAssignment()
                .getTarget();
        return assignment == FleetAssignment.ATTACK_LOCATION
                        && assignmentTarget == targetToken
                || assignment == FleetAssignment.GO_TO_LOCATION
                        && assignmentTarget == targetToken
                || assignment == FleetAssignment.INTERCEPT
                        && assignmentTarget == gateDefense;
    }

    private static void assignSpartanCenterDefenders(
            StarSystemAPI system,
            CampaignFleetAPI invasion,
            CampaignFleetAPI gateDefense) {
        int index = 0;
        for (CampaignFleetAPI spartan
                : OdysseyExpanseSystem.ensureFobIthacaSpartanGuards(system)) {
            if (TroyArrivalScript.isFleetBusyForMutation(spartan)) {
                index++;
                continue;
            }
            spartan.getMemoryWithoutUpdate().set(
                    ITHACA_FINAL_CENTER_BATTLE_MARKER, true);
            if (spartan.getBattle() != null) {
                index++;
                continue;
            }
            if (!spartan.getMemoryWithoutUpdate().getBoolean(
                    ITHACA_FINAL_CENTER_TFS_TELEPORTED)) {
                double angle = Math.toRadians(150d + 30d * index);
                spartan.setLocation(
                        gateDefense.getLocation().x
                                + (float) Math.cos(angle)
                                        * ITHACA_FINAL_CENTER_DEFENSE_DISTANCE,
                        gateDefense.getLocation().y
                                + (float) Math.sin(angle)
                                        * ITHACA_FINAL_CENTER_DEFENSE_DISTANCE);
                spartan.setVelocity(0f, 0f);
                spartan.getMemoryWithoutUpdate().set(
                        ITHACA_FINAL_CENTER_TFS_TELEPORTED, true);
                logIthacaStrike("Teleported surviving TFS fleet="
                        + spartan.getId()
                        + " to defend Breach Defense Station.");
            }
            if (spartan.getCurrentAssignment() == null
                    || spartan.getCurrentAssignment().getAssignment()
                            != FleetAssignment.DEFEND_LOCATION
                    || spartan.getCurrentAssignment().getTarget()
                            != gateDefense) {
                spartan.clearAssignments();
                spartan.addAssignment(
                        FleetAssignment.DEFEND_LOCATION,
                        gateDefense,
                        ITHACA_ENDGAME_INVASION_DAYS,
                        "defending Breach Defense Station");
            }
            if (spartan.getAI() != null) {
                for (CampaignFleetAPI other : system.getFleets()) {
                    if (other != null && other != invasion
                            && other != spartan) {
                        spartan.getAI().doNotAttack(
                                other,
                                ITHACA_ENDGAME_IGNORE_DISTRACTION_DAYS);
                        if (other.getAI() != null) {
                            other.getAI().doNotAttack(
                                    spartan,
                                    ITHACA_ENDGAME_IGNORE_DISTRACTION_DAYS);
                        }
                    }
                }
                if (spartan.getAI() instanceof ModularFleetAIAPI) {
                    TacticalModulePlugin tactical =
                            ((ModularFleetAIAPI) spartan.getAI())
                                    .getTacticalModule();
                    if (tactical != null) {
                        tactical.setPriorityTarget(
                                invasion,
                                ITHACA_STRIKE_PRIORITY_DAYS,
                                false);
                    }
                }
            }
            index++;
        }
    }

    private static void lockFinalCenterBreachTarget(
            CampaignFleetAPI invasion,
            StarSystemAPI system,
            SectorEntityToken target) {
        if (invasion.getAI() == null) return;
        CampaignFleetAIAPI ai = invasion.getAI();
        for (CampaignFleetAPI other : system.getFleets()) {
            if (other == null || other == invasion
                    || other.getMemoryWithoutUpdate().getBoolean(
                            ITHACA_FINAL_CENTER_BATTLE_MARKER)) {
                continue;
            }
            ai.doNotAttack(other, ITHACA_ENDGAME_IGNORE_DISTRACTION_DAYS);
            if (other.getAI() != null) {
                other.getAI().doNotAttack(
                        invasion,
                        ITHACA_ENDGAME_IGNORE_DISTRACTION_DAYS);
            }
        }
        if (ai instanceof ModularFleetAIAPI) {
            TacticalModulePlugin tactical =
                    ((ModularFleetAIAPI) ai).getTacticalModule();
            if (tactical != null) {
                tactical.setPriorityTarget(
                        target, ITHACA_STRIKE_PRIORITY_DAYS, false);
                tactical.setTarget(target);
            }
        }
    }

    private static void ensureFinalCenterBattleListener(
            CampaignFleetAPI fleet) {
        for (FleetEventListener listener : fleet.getEventListeners()) {
            if (listener instanceof FinalCenterBattleListener) return;
        }
        fleet.addEventListener(new FinalCenterBattleListener());
    }

    private static final class FinalCenterBattleListener
            implements FleetEventListener {
        @Override
        public void reportBattleOccurred(
                CampaignFleetAPI fleet,
                CampaignFleetAPI primaryWinner,
                BattleAPI battle) {
            if (Global.getSector() == null || battle == null) return;
            boolean enemyDefeated = battle.wasFleetDefeated(
                    fleet, primaryWinner) || fleet.isEmpty();
            if (battle.isPlayerInvolved()) {
                if (enemyDefeated
                        && !MenelausTrial.isFinalLaborFailed()) {
                    Global.getSector().getMemoryWithoutUpdate().set(
                            MenelausTrial.GAUTAMA_DEFEATED, true);
                    logIthacaStrike("Player repelled the Labor V Breach Defense "
                            + "assault; Gautama and the Heavenly Strike are "
                            + "defeated.");
                    MenelausTrialIntel.syncProgress(true);
                } else {
                    CampaignFleetAPI player = Global.getSector()
                            .getPlayerFleet();
                    boolean playerDefeated = player != null
                            && battle.wasFleetDefeated(
                                    player, primaryWinner);
                    boolean enemyVictorious = battle.wasFleetVictorious(
                            fleet, primaryWinner);
                    if ((playerDefeated || enemyVictorious)
                            && MenelausTrial.failFinalLabor()) {
                        Global.getSector().getCampaignUI().addMessage(
                                "Ithaca has fallen. Starving Threat fleets "
                                        + "are emerging from gates across "
                                        + "the Sector.",
                                Misc.getNegativeHighlightColor());
                        logIthacaStrike("Player lost the Labor V center "
                                + "battle; Sector-wide gate incursions are "
                                + "now active.");
                    }
                }
            } else if (enemyDefeated
                    && !fleet.getMemoryWithoutUpdate().contains(
                    ITHACA_FINAL_CENTER_AUTORE_SOLVE_LOG)) {
                fleet.getMemoryWithoutUpdate().set(
                        ITHACA_FINAL_CENTER_AUTORE_SOLVE_LOG, true);
                // This is an authored gameplay re-formation, explicitly
                // authorized by an observed AI-only loss. Generic absence or
                // malformed save state never clears the creation latch.
                Global.getSector().getMemoryWithoutUpdate().unset(
                        ITHACA_FINAL_CENTER_CREATED_V1);
                logIthacaStrike("TFS autoresolved the Breach Defense assault "
                        + "without "
                        + "the player; the Heavenly Strike will re-form.");
            }
        }

        @Override
        public void reportFleetDespawnedToListener(
                CampaignFleetAPI fleet,
                CampaignEventListener.FleetDespawnReason reason,
                Object param) {
            // Labor V's recurring controller owns replacement and cleanup.
        }
    }

    /** Maintains one renewable Starving Threat incursion at every gate. */
    private static void ensureGateBreachPopulation() {
        if (!MenelausTrial.isFinalLaborFailed()) return;
        for (LocationAPI location : Global.getSector().getAllLocations()) {
            if (location == null
                    || OdysseyExpanseSystem.MESSINA_ID.equals(location.getId())) continue;
            for (SectorEntityToken gate : new ArrayList<SectorEntityToken>(
                    location.getEntitiesWithTag(Tags.GATE))) {
                ensureGateBreachFleet(location, gate);
            }
        }
    }

    private static void ensureGateBreachFleet(
            LocationAPI location, SectorEntityToken gate) {
        if (gate == null || gate.getId() == null) return;
        String source = (location.getId() == null ? "location"
                : location.getId()) + "|" + gate.getId();
        String fleetId = GATE_BREACH_FLEET_PREFIX
                + Integer.toHexString(source.hashCode());
        SectorEntityToken existing = location.getEntityById(fleetId);
        if (existing instanceof CampaignFleetAPI) {
            CampaignFleetAPI fleet = (CampaignFleetAPI) existing;
            if (!isOwnedGateBreachFleet(fleet, source)) return;
            if (TroyArrivalScript.isFleetBusyForMutation(fleet)) return;
            if (!fleet.isEmpty()) {
                configureGateBreachFleet(fleet, gate, source);
                return;
            }
            location.removeEntity(fleet);
            scheduleGateBreachRespawn(gate, source.hashCode());
        } else if (existing != null) {
            // A wrong-type claim on an authored ID is authoritative serialized
            // state; fail closed instead of replacing it in a population tick.
            return;
        }

        if (gate.getMemoryWithoutUpdate().getBoolean(
                GATE_BREACH_RESPAWN_COOLDOWN)) {
            return;
        }

        int hash = source.hashCode() & Integer.MAX_VALUE;
        int tier = hash % 3 == 0 ? SECOND_STRIKE : FIRST_STRIKE;
        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(
                Factions.THREAT, "Starving Threat Gate Incursion", true);
        fleet.setId(fleetId);
        fleet.setNoFactionInName(true);
        fleet.setTransponderOn(false);
        addStrikeRoster(fleet, tier);
        if (fleet.getFleetData().getMembersListCopy().isEmpty()) return;
        fleet.getFleetData().setFlagship(
                fleet.getFleetData().getMembersListCopy().get(0));
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
        location.addEntity(fleet);
        float angle = hash % 360;
        float distance = Math.max(260f, gate.getRadius() + 180f);
        double radians = Math.toRadians(angle);
        fleet.setLocation(
                gate.getLocation().x
                        + (float) Math.cos(radians) * distance,
                gate.getLocation().y
                        + (float) Math.sin(radians) * distance);
        configureGateBreachFleet(fleet, gate, source);
        scheduleGateBreachRespawn(gate, hash);
    }

    private static boolean isOwnedGateBreachFleet(
            CampaignFleetAPI fleet, String source) {
        if (fleet == null || isPlayerControlledFleet(fleet)
                || !fleet.getMemoryWithoutUpdate().getBoolean(
                        GATE_BREACH_FLEET_MARKER)) {
            return false;
        }
        String recordedSource = fleet.getMemoryWithoutUpdate().getString(
                GATE_BREACH_SOURCE);
        return source != null && source.equals(recordedSource);
    }

    private static void configureGateBreachFleet(
            CampaignFleetAPI fleet,
            SectorEntityToken gate,
            String source) {
        if (TroyArrivalScript.isFleetBusyForMutation(fleet)) return;
        configureStarvingThreatIdentity(fleet);
        fleet.setFaction(Factions.THREAT, true);
        fleet.setName("Starving Threat Gate Incursion");
        configureHostileFleet(fleet);
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        memory.set(GATE_BREACH_FLEET_MARKER, true);
        memory.set(GATE_BREACH_SOURCE, source);
        memory.set(MemFlags.MEMORY_KEY_NO_JUMP, true);
        memory.set(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE, true);
        boolean listenerPresent = false;
        for (FleetEventListener listener : fleet.getEventListeners()) {
            if (listener instanceof GateBreachFleetListener) {
                listenerPresent = true;
                break;
            }
        }
        if (!listenerPresent) {
            fleet.addEventListener(new GateBreachFleetListener(gate.getId()));
        }
        if (fleet.getBattle() == null
                && (fleet.getCurrentAssignment() == null
                    || fleet.getCurrentAssignment().getAssignment()
                            != FleetAssignment.PATROL_SYSTEM)) {
            fleet.clearAssignments();
            fleet.addAssignment(
                    FleetAssignment.PATROL_SYSTEM,
                    gate,
                    1000000f,
                    "spreading from " + gate.getName());
        }
    }

    private static void scheduleGateBreachRespawn(
            SectorEntityToken gate, int seed) {
        if (gate == null) return;
        float delay = 18f + ((seed & Integer.MAX_VALUE) % 15);
        gate.getMemoryWithoutUpdate().set(
                GATE_BREACH_RESPAWN_COOLDOWN, true, delay);
    }

    private static final class GateBreachFleetListener
            implements FleetEventListener {
        private final String gateId;

        private GateBreachFleetListener(String gateId) {
            this.gateId = gateId;
        }

        @Override
        public void reportBattleOccurred(
                CampaignFleetAPI fleet,
                CampaignFleetAPI primaryWinner,
                BattleAPI battle) {
            if (battle != null && (fleet.isEmpty()
                    || battle.wasFleetDefeated(fleet, primaryWinner))) {
                schedule(fleet);
            }
        }

        @Override
        public void reportFleetDespawnedToListener(
                CampaignFleetAPI fleet,
                CampaignEventListener.FleetDespawnReason reason,
                Object param) {
            schedule(fleet);
        }

        private void schedule(CampaignFleetAPI fleet) {
            LocationAPI location = fleet == null
                    ? null : fleet.getContainingLocation();
            SectorEntityToken gate = location == null
                    ? null : location.getEntityById(gateId);
            scheduleGateBreachRespawn(gate,
                    (gateId == null ? 0 : gateId.hashCode())
                            + (fleet == null || fleet.getId() == null
                                    ? 0 : fleet.getId().hashCode()));
        }
    }

    private static void clearFinalCenterDefenderMarkers(
            StarSystemAPI system) {
        SectorEntityToken finalFleet = system.getEntityById(
                ITHACA_FINAL_CENTER_FLEET_ID);
        SectorEntityToken gateDefense = system.getEntityById(
                OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_STATION_ID);
        boolean busyFleetRemains = false;
        for (CampaignFleetAPI fleet
                : new ArrayList<CampaignFleetAPI>(system.getFleets())) {
            if (!fleet.getMemoryWithoutUpdate().getBoolean(
                    ITHACA_FINAL_CENTER_BATTLE_MARKER)) {
                continue;
            }
            if (isPlayerControlledFleet(fleet)) {
                busyFleetRemains = true;
                continue;
            }
            if (TroyArrivalScript.isFleetBusyForMutation(fleet)) {
                busyFleetRemains = true;
                continue;
            }
            fleet.getMemoryWithoutUpdate().unset(
                    ITHACA_FINAL_CENTER_BATTLE_MARKER);
            fleet.getMemoryWithoutUpdate().unset(
                    ITHACA_FINAL_CENTER_TFS_TELEPORTED);
            if (fleet.getCurrentAssignment() != null) {
                SectorEntityToken assignmentTarget = fleet
                        .getCurrentAssignment().getTarget();
                if (assignmentTarget == finalFleet
                        || assignmentTarget == gateDefense) {
                    fleet.clearAssignments();
                }
            }
        }
        if (busyFleetRemains) return;
        if (finalFleet instanceof CampaignFleetAPI
                && isOwnedFinalLaborCenterFleet(
                        (CampaignFleetAPI) finalFleet)
                && ((CampaignFleetAPI) finalFleet).isEmpty()
                && !TroyArrivalScript.isFleetBusyForMutation(
                        (CampaignFleetAPI) finalFleet)) {
            system.removeEntity(finalFleet);
        }
        SectorEntityToken target = system.getEntityById(
                ITHACA_FINAL_CENTER_TARGET_ID);
        if (isOwnedProxyToken(target)) {
            system.removeEntity(target);
        } else if (target != null) {
            logBlockedProxyTokenOnce(
                    ITHACA_FINAL_CENTER_TARGET_ID, target);
        }
        SectorEntityToken center = system.getEntityById(
                IthacaSectionEncounter.FOB_ENTITY_ID);
        if (OdysseyExpanseSystem.isFobIthaca(center)) {
            suppressPerimeterBastionReinforcements(center, false);
        }
    }

    private static void suppressPerimeterBastionReinforcements(
            SectorEntityToken center, boolean suppress) {
        if (!OdysseyExpanseSystem.isFobIthaca(center)) return;
        for (IthacaSectionEncounter.Section section
                : IthacaSectionEncounter.Section.values()) {
            CampaignFleetAPI station = IthacaSectionEncounter.getStation(
                    center, section);
            if (station == null) continue;
            if (suppress) {
                station.getMemoryWithoutUpdate().set(
                        MemFlags.FLEET_IGNORES_OTHER_FLEETS, true);
                station.getMemoryWithoutUpdate().set(
                        MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS, true);
            } else {
                station.getMemoryWithoutUpdate().unset(
                        MemFlags.FLEET_IGNORES_OTHER_FLEETS);
                station.getMemoryWithoutUpdate().unset(
                        MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS);
            }
        }
    }

    /** Gives each authored endgame invasion ship its side-discovery marker. */
    static void ensureIthacaSiegeStalemateHullmods(CampaignFleetAPI fleet) {
        if (fleet == null) return;
        boolean changed = false;
        for (FleetMemberAPI member
                : fleet.getFleetData().getMembersListCopy()) {
            ShipVariantAPI original = member.getVariant();
            if (original == null
                    || original.hasHullMod(
                            IthacaSiegeStalemateHullmod.HULLMOD_ID)) {
                continue;
            }
            ShipVariantAPI marked = original.clone();
            marked.addPermaMod(IthacaSiegeStalemateHullmod.HULLMOD_ID);
            member.setVariant(marked, false, true);
            changed = true;
        }
        if (changed) {
            fleet.getFleetData().setSyncNeeded();
            fleet.getFleetData().syncIfNeeded();
        }
    }

    /** Maintains the one-time Ungaikyo encounter after Labor V's debrief. */
    private static void ensurePostLaborUngaikyo() {
        if (!MenelausTrial.isComplete()) return;
        if (isUngaikyoRematchDefeated()) {
            TroyArrivalScript.cleanupGautamaFinalRematch();
            return;
        }
        StarSystemAPI lastLight = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.LAST_LIGHT_ID);
        if (lastLight == null) return;
        SectorEntityToken guardian = lastLight.getEntityById(
                OdysseyExpanseSystem.LAST_LIGHT_GUARDIAN_WRECK_ID);
        TroyArrivalScript.spawnStarvingThreatSample(
                lastLight,
                guardian == null ? lastLight.getCenter() : guardian);
    }

    /** True once the player's real campaign battle ends the rematch. */
    public static boolean isUngaikyoRematchDefeated() {
        return Global.getSector() != null
                && Global.getSector().getMemoryWithoutUpdate().getBoolean(
                        GAUTAMA_FINAL_REMATCH_DEFEATED);
    }

    /** Historical API retained for save and binary compatibility. */
    public static boolean isGautamaFinalRematchDefeated() {
        return isUngaikyoRematchDefeated();
    }

    /** Public route check shared by campaign interaction and battle plugins. */
    public static boolean isUngaikyoRematch(
            SectorEntityToken entity) {
        return entity instanceof CampaignFleetAPI
                && entity.getMemoryWithoutUpdate().getBoolean(
                        GAUTAMA_FINAL_REMATCH_MARKER);
    }

    /** Historical API retained for save and binary compatibility. */
    public static boolean isGautamaFinalRematch(
            SectorEntityToken entity) {
        return isUngaikyoRematch(entity);
    }

    /** Alias spelling the marker's purpose at battle-selection call sites. */
    public static boolean isUngaikyoRematchBattle(
            SectorEntityToken opponent) {
        return isUngaikyoRematch(opponent);
    }

    /** Historical API retained for save and binary compatibility. */
    public static boolean isGautamaFinalRematchBattle(
            SectorEntityToken opponent) {
        return isUngaikyoRematchBattle(opponent);
    }

    /**
     * Commits the additive one-shot flag. Combat plugins may call this only
     * after confirming they are reporting the real campaign fleet.
     */
    public static void markUngaikyoRematchDefeated() {
        if (Global.getSector() == null) return;
        Global.getSector().getMemoryWithoutUpdate().set(
                GAUTAMA_FINAL_REMATCH_DEFEATED, true);
    }

    /** Historical API retained for save and binary compatibility. */
    public static void markGautamaFinalRematchDefeated() {
        markUngaikyoRematchDefeated();
    }

    /** Ensures old saves gain the result listener before their first rematch. */
    static void ensureGautamaFinalRematchListener(
            CampaignFleetAPI fleet) {
        if (fleet == null) return;
        for (FleetEventListener listener : fleet.getEventListeners()) {
            if (listener instanceof GautamaFinalRematchListener) return;
        }
        fleet.addEventListener(new GautamaFinalRematchListener());
    }

    private static final class GautamaFinalRematchListener
            implements FleetEventListener {
        @Override
        public void reportBattleOccurred(
                CampaignFleetAPI fleet,
                CampaignFleetAPI primaryWinner,
                BattleAPI battle) {
            if (battle == null || !battle.isPlayerInvolved()
                    || !isUngaikyoRematch(fleet)) {
                return;
            }
            // Ungaikyo's death is final even when the player disengages while
            // part of the ordinary Third Strike survives. Whole-fleet defeat
            // remains the fallback for externally modified rosters.
            if (hasUngaikyoMember(fleet)
                    && !fleet.isEmpty()
                    && !battle.wasFleetDefeated(fleet, primaryWinner)) {
                return;
            }
            markUngaikyoRematchDefeated();
            Global.getLogger(OdysseyPredatorScript.class).info(
                    "Ungaikyo defeated by the player; "
                            + "the historical Heavenly Strike will not "
                            + "re-form.");
        }

        @Override
        public void reportFleetDespawnedToListener(
                CampaignFleetAPI fleet,
                CampaignEventListener.FleetDespawnReason reason,
                Object param) {
            // Despawn or AI-only loss never consumes the player's one shot.
        }
    }

    /**
     * Grows a Starving fleet to its stable post-Labor Strike tier. Existing
     * survivors are retained and missing roles are added in place.
     */
    static void ensurePostLaborStrikeRoster(
            CampaignFleetAPI fleet, int requestedTier) {
        if (fleet == null || !MenelausTrial.isComplete()) return;
        if (TroyArrivalScript.isFleetBusyForMutation(fleet)) return;
        int tier = Math.max(FIRST_STRIKE,
                Math.min(THIRD_STRIKE, requestedTier));
        int[] counts = POST_LABOR_STRIKE_COUNTS[tier];
        java.util.List<FleetMemberAPI> members =
                fleet.getFleetData().getMembersListCopy();
        boolean changed = false;

        for (int role = 0; role < counts.length; role++) {
            int present = 0;
            for (FleetMemberAPI member : members) {
                if (STARVING_ROLE_HULLS[role].equals(member.getHullId())) {
                    present++;
                }
            }
            while (present < counts[role]) {
                String[] variants = STARVING_ROLE_VARIANTS[role];
                addMember(fleet, variants[present % variants.length]);
                present++;
                changed = true;
            }
        }

        String fleetType = tier == THIRD_STRIKE
                ? FleetTypes.PATROL_LARGE
                : tier == SECOND_STRIKE
                        ? FleetTypes.PATROL_MEDIUM
                        : FleetTypes.PATROL_SMALL;
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_FLEET_TYPE, fleetType);
        if (changed) {
            fleet.getFleetData().setSyncNeeded();
            fleet.getFleetData().syncIfNeeded();
        }
    }

    private static int getPostLaborStrikeTier(CampaignFleetAPI fleet) {
        String id = fleet == null ? null : fleet.getId();
        int hash = id == null ? 0 : id.hashCode();
        return (hash & Integer.MAX_VALUE) % 3;
    }

    private void ensureCharybdisHunter() {
        CampaignFleetAPI charybdis = findBudaiFleet();
        if (isBudaiDefeated()) {
            if (charybdis != null && !TroyArrivalScript.isFleetBusyForMutation(charybdis)) {
                charybdis.clearAssignments();
                charybdis.setAbortDespawn(false);
                charybdis.setNoAutoDespawn(false);
                charybdis.despawn(CampaignEventListener.FleetDespawnReason.OTHER, null);
            }
            return;
        }
        StarSystemAPI avici = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.MESSINA_ID);
        if (avici == null) return;
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        if (charybdis == null) {
            if (memory.getBoolean(BUDAI_SPAWNED)
                    || avici.getEntityById(TroyArrivalScript.CHARYBDIS_FLEET_ID) != null) {
                return;
            }
            charybdis = spawnCharybdis(avici);
            if (charybdis == null) return;
        }
        memory.set(BUDAI_SPAWNED, true);
        if (TroyArrivalScript.isFleetBusyForMutation(charybdis)) return;
        if (!hasBudai(charybdis)) {
            markBudaiDefeated();
            return;
        }
        configureCharybdis(charybdis);
        updateCharybdisAssignment(charybdis, avici, Global.getSector().getPlayerFleet());
    }

    public static boolean isBudaiDefeated() {
        return Global.getSector() != null
                && Global.getSector().getMemoryWithoutUpdate().getBoolean(BUDAI_DEFEATED);
    }

    private static void markBudaiDefeated() {
        if (Global.getSector() != null) {
            Global.getSector().getMemoryWithoutUpdate().set(BUDAI_DEFEATED, true);
        }
    }

    private static CampaignFleetAPI findBudaiFleet() {
        StarSystemAPI avici = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.MESSINA_ID);
        if (avici == null) return null;
        SectorEntityToken entity = avici.getEntityById(TroyArrivalScript.CHARYBDIS_FLEET_ID);
        if (!(entity instanceof CampaignFleetAPI)) return null;
        CampaignFleetAPI fleet = (CampaignFleetAPI) entity;
        return isPlayerControlledFleet(fleet) ? null : fleet;
    }

    /** Returns every campaign repository, including hyperspace explicitly. */
    private static java.util.List<LocationAPI>
            getCampaignLocationsIncludingHyperspace() {
        java.util.List<LocationAPI> result =
                new ArrayList<LocationAPI>();
        java.util.Set<LocationAPI> seen =
                java.util.Collections.newSetFromMap(
                        new java.util.IdentityHashMap<LocationAPI, Boolean>());
        addCampaignLocation(
                result, seen, Global.getSector().getHyperspace());
        for (LocationAPI location : Global.getSector().getAllLocations()) {
            addCampaignLocation(result, seen, location);
        }
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            addCampaignLocation(result, seen, system);
        }
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player != null) {
            addCampaignLocation(
                    result, seen, player.getContainingLocation());
        }
        addCampaignLocation(
                result,
                seen,
                OdysseyExpanseSystem.findSystemById(
                        OdysseyExpanseSystem.MESSINA_ID));
        return result;
    }

    private static void addCampaignLocation(
            java.util.List<LocationAPI> result,
            java.util.Set<LocationAPI> seen,
            LocationAPI location) {
        if (location != null && seen.add(location)) result.add(location);
    }

    /** Compatibility hook for the retired showcase spawner. Never creates a second fleet. */
    static CampaignFleetAPI reconcileBudaiForLegacyCall() {
        return isBudaiDefeated() ? null : findBudaiFleet();
    }

    private static CampaignFleetAPI spawnCharybdis(StarSystemAPI avici) {
        if (isBudaiDefeated()
                || Global.getSector().getMemoryWithoutUpdate().getBoolean(BUDAI_SPAWNED)
                || !SinniSystemVignetteScript.isComplete(
                        SinniSystemVignetteScript.Category.AVICI)) {
            return null;
        }
        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(
                Factions.DWELLER, "Budai", true);
        fleet.setId(TroyArrivalScript.CHARYBDIS_FLEET_ID);
        fleet.setName("Budai");
        fleet.setNoFactionInName(true);
        fleet.setTransponderOn(false);

        addMember(fleet, "chief_navigator_charybdis_Ravenous");
        fleet.getFleetData().setFlagship(
                fleet.getFleetData().getMembersListCopy().get(0));
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();

        avici.addEntity(fleet);
        // First materialization is far outside the system, never beside the
        // player or Kshaya. Normal pursuit/lair movement owns it thereafter.
        Vector2f center = avici.getCenter().getLocation();
        fleet.setLocation(center.x + BUDAI_INITIAL_SPAWN_DISTANCE, center.y);
        configureCharybdis(fleet);
        return fleet;
    }

    private static void configureCharybdis(CampaignFleetAPI fleet) {
        if (isBudaiDefeated()) return;
        if (TroyArrivalScript.isFleetBusyForMutation(fleet)) return;
        fleet.setName("Budai");
        fleet.setNoFactionInName(true);
        ensureBudaiSalvageListener(fleet);
        configureHostileFleet(fleet);
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        memory.set(BUDAI_HUNTER_MARKER, true);
        EncounterCombatChatter.configure(fleet, EncounterCombatChatter.BUDAI);
        configureBudaiInteractionIdentity(fleet);
        memory.set(MusicPlayerPluginImpl.COMBAT_MUSIC_SET_MEM_KEY,
                BudaiMusic.BATTLE_ID);
        boolean wasAvoidingAbyss = memory
                .getBoolean(MemFlags.AVOIDING_ABYSSAL_HYPERSPACE);
        memory.set(
                MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE, true);
        memory.set(
                MemFlags.MEMORY_KEY_PURSUE_PLAYER, true);
        memory.set(
                MemFlags.FLEET_DO_NOT_IGNORE_PLAYER, true);
        memory.set(
                MemFlags.MEMORY_KEY_NEVER_AVOID_PLAYER_SLOWLY, true);
        memory.set(
                MemFlags.MEMORY_KEY_FLEET_DO_NOT_GET_SIDETRACKED, true);
        memory.set(
                MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT, true);
        memory.set(
                MemFlags.MEMORY_KEY_STICK_WITH_PLAYER_IF_ALREADY_TARGET, true);
        // Preserve the vanilla pursuit state regardless of the player's
        // transponder. Exact tracking below does not depend on sensor contact.
        memory.set(
                MemFlags.MEMORY_KEY_SAW_PLAYER_WITH_TRANSPONDER_ON, true);
        memory.set(
                MemFlags.MEMORY_KEY_SAW_PLAYER_WITH_TRANSPONDER_OFF, true);
        memory.set(
                MemFlags.MEMORY_KEY_NO_JUMP, true);
        // Vanilla tactical AI adds a long-lived escape vector as soon as a
        // fleet enters sufficiently deep Abyssal terrain unless this flag is
        // present. That avoidance vector was overpowering the pursuit order.
        memory.set(MemFlags.MAY_GO_INTO_ABYSS, true);
        memory.unset(
                MemFlags.AVOIDING_ABYSSAL_HYPERSPACE);
        if (wasAvoidingAbyss
                && fleet.getAI() instanceof ModularFleetAIAPI) {
            ((ModularFleetAIAPI) fleet.getAI()).getNavModule()
                    .clearAvoidList();
        }
        fleet.getDetectedRangeMod().modifyFlat(DETECTION_MOD, 30000f);
        // Exact movement and real sensor contact are both enforced. The huge
        // flat bonuses remain effective even in the deepest Abyss and ensure
        // the campaign AI can initiate contact when the override reaches the
        // player instead of becoming "blind" at close range.
        fleet.getStats().getSensorStrengthMod().modifyFlat(
                SENSOR_STRENGTH_MOD, CHARYBDIS_SENSOR_OVERRIDE);
        fleet.getStats().getSensorRangeMod().modifyFlat(
                SENSOR_RANGE_MOD, CHARYBDIS_SENSOR_OVERRIDE);
    }

    private static boolean hasBudai(CampaignFleetAPI fleet) {
        if (fleet == null) return false;
        for (FleetMemberAPI member
                : fleet.getFleetData().getMembersListCopy()) {
            if ("chief_navigator_charybdis".equals(member.getHullId())) {
                return true;
            }
        }
        return false;
    }

    private static void ensureBudaiSalvageListener(CampaignFleetAPI fleet) {
        if (fleet == null || isPlayerControlledFleet(fleet)) return;
        fleet.getMemoryWithoutUpdate().set(BUDAI_HUNTER_MARKER, true);
        for (FleetEventListener listener : fleet.getEventListeners()) {
            if (listener instanceof BudaiSalvageListener) return;
        }
        fleet.addEventListener(new BudaiSalvageListener());
    }

    /** Records permanent defeat, preserving the player-only salvage scene. */
    private static final class BudaiSalvageListener
            implements FleetEventListener {
        @Override
        public void reportBattleOccurred(
                CampaignFleetAPI fleet,
                CampaignFleetAPI primaryWinner,
                BattleAPI battle) {
            if (Global.getSector() == null || battle == null || fleet == null
                    || isPlayerControlledFleet(fleet)) {
                return;
            }
            boolean ownedBudaiFleet = TroyArrivalScript.CHARYBDIS_FLEET_ID
                    .equals(fleet.getId())
                    || fleet.getMemoryWithoutUpdate().getBoolean(
                            BUDAI_HUNTER_MARKER);
            if (!ownedBudaiFleet || hasBudai(fleet)) return;
            markBudaiDefeated();
            if (battle.isPlayerInvolved()) BudaiSalvageDialogPlugin.request();
            Global.getLogger(OdysseyPredatorScript.class).info(
                    "Budai permanently defeated; player salvage queued="
                            + battle.isPlayerInvolved());
        }

        @Override
        public void reportFleetDespawnedToListener(
                CampaignFleetAPI fleet,
                CampaignEventListener.FleetDespawnReason reason,
                Object param) {
            if (Global.getSector() == null || fleet == null
                    || isPlayerControlledFleet(fleet)
                    || reason != CampaignEventListener.FleetDespawnReason.DESTROYED_BY_BATTLE) {
                return;
            }
            if (TroyArrivalScript.CHARYBDIS_FLEET_ID.equals(fleet.getId())
                    || fleet.getMemoryWithoutUpdate().getBoolean(BUDAI_HUNTER_MARKER)) {
                markBudaiDefeated();
            }
            // Despawn callbacks alone never grant the player's salvage reward.
        }
    }

    /** Display identity only; never replace officers, skills, or fleet behavior. */
    static void configureBudaiInteractionIdentity(CampaignFleetAPI fleet) {
        if (!isBudaiBattle(fleet)) return;
        fleet.setName("Budai");
        fleet.setNoFactionInName(true);
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        memory.set(BUDAI_HUNTER_MARKER, true);
        memory.set(MemFlags.MEMORY_KEY_IGNORE_PLAYER_COMMS, true);
        memory.unset(MemFlags.MEMORY_KEY_PATROL_FLEET);
        PersonAPI commander = fleet.getCommander();
        if (commander != null) {
            commander.setName(new FullName("Budai", "", FullName.Gender.ANY));
            commander.setPortraitSprite("graphics/portraits/characters/dwelller.png");
        }
    }

    /** True only for a campaign opponent that still contains Budai itself. */
    public static boolean isBudaiBattle(SectorEntityToken opponent) {
        if (isBudaiDefeated()) return false;
        if (!(opponent instanceof CampaignFleetAPI)) return false;
        CampaignFleetAPI fleet = (CampaignFleetAPI) opponent;
        if (isPlayerControlledFleet(fleet)) return false;
        boolean canonical = TroyArrivalScript.CHARYBDIS_FLEET_ID.equals(
                fleet.getId());
        boolean marked = fleet.getMemoryWithoutUpdate().getBoolean(
                BUDAI_HUNTER_MARKER);
        boolean result = (canonical || marked) && hasBudai(fleet);
        if (result) ensureBudaiSalvageListener(fleet);
        return result;
    }

    private static void updateCharybdisAssignment(
            CampaignFleetAPI fleet,
            StarSystemAPI avici,
            CampaignFleetAPI player) {
        if (TroyArrivalScript.isFleetBusyForMutation(fleet)) return;
        LocationAPI location = fleet.getContainingLocation();
        boolean huntPlayer = player != null
                && location == avici
                && player.getContainingLocation() == avici;

        if (huntPlayer) {
            if (fleet.getCurrentAssignment() == null
                    || fleet.getCurrentAssignment().getAssignment()
                            != FleetAssignment.INTERCEPT
                    || fleet.getCurrentAssignment().getTarget() != player) {
                fleet.clearAssignments();
                fleet.addAssignment(
                        FleetAssignment.INTERCEPT,
                        player,
                        1000000f,
                        "hunting through Avici");
            }
            return;
        }

        Vector2f staging = getCharybdisStagingPoint(
                location, avici, player);
        SectorEntityToken anchor = ensureCharybdisAnchor(
                location, staging);
        if (anchor == null) return;
        if (fleet.getCurrentAssignment() == null
                || fleet.getCurrentAssignment().getAssignment()
                        != FleetAssignment.DEFEND_LOCATION
                || fleet.getCurrentAssignment().getTarget() != anchor) {
            fleet.clearAssignments();
            fleet.addAssignment(
                    FleetAssignment.DEFEND_LOCATION,
                    anchor,
                    1000000f,
                    "lurking near Kshaya");
        }
    }

    /**
     * Drives Budai toward the player's exact live coordinates inside Avici.
     * Budai never follows the player through a jump point.
     */
    private static void applyCharybdisTrackingOverride() {
        if (isBudaiDefeated()) return;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        CampaignFleetAPI charybdis = findBudaiFleet();
        if (player == null || charybdis == null) return;
        if (TroyArrivalScript.isFleetBusyForMutation(charybdis)) return;

        StarSystemAPI avici = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.MESSINA_ID);
        LocationAPI location = charybdis.getContainingLocation();
        LocationAPI playerLocation = player.getContainingLocation();
        Vector2f destination = null;

        if (location == avici && playerLocation == avici) {
            destination = new Vector2f(player.getLocation());
        } else if (location == avici) {
            destination = getCharybdisStagingPoint(avici, avici, null);
        }

        if (destination == null) return;
        if (playerLocation == avici
                && charybdis.getAI() instanceof ModularFleetAIAPI) {
            ((ModularFleetAIAPI) charybdis.getAI()).getTacticalModule()
                    .setPriorityTarget(player, 1f, false);
        }
        charybdis.setMoveDestination(destination.x, destination.y);
        charybdis.setMoveDestinationOverride(destination.x, destination.y);
    }

    private static SectorEntityToken ensureCharybdisAnchor(
            LocationAPI location, Vector2f staging) {
        SectorEntityToken anchor = location.getEntityById(
                CHARYBDIS_HYPER_ANCHOR_ID);
        if (anchor == null) {
            anchor = location.createToken(staging.x, staging.y);
            anchor.setId(CHARYBDIS_HYPER_ANCHOR_ID);
            markOwnedProxyToken(anchor);
        } else if (!isOwnedProxyToken(anchor)) {
            logBlockedProxyTokenOnce(CHARYBDIS_HYPER_ANCHOR_ID, anchor);
            return null;
        }
        anchor.setLocation(staging.x, staging.y);
        return anchor;
    }

    private static Vector2f getCharybdisStagingPoint(
            LocationAPI location,
            StarSystemAPI avici,
            CampaignFleetAPI player) {
        Vector2f center;
        if (player != null
                && player.getContainingLocation() == location) {
            center = player.getLocation();
        } else if (location == avici) {
            SectorEntityToken habitat = avici.getEntityById(
                    AviciHabitatEncounter.HABITAT_ID);
            center = habitat == null
                    ? avici.getCenter().getLocation()
                    : habitat.getLocation();
        } else if (player != null
                && player.getContainingLocation() != null
                && player.getContainingLocation() instanceof StarSystemAPI) {
            Vector2f hyperspaceLocation = player.getLocationInHyperspace();
            center = hyperspaceLocation == null
                    ? player.getContainingLocation().getLocation()
                    : hyperspaceLocation;
        } else if (location instanceof StarSystemAPI) {
            center = ((StarSystemAPI) location).getCenter().getLocation();
        } else {
            center = avici.getLocation();
        }
        Vector2f staging = new Vector2f(
                center.x + CHARYBDIS_STAGING_OFFSET,
                center.y - CHARYBDIS_STAGING_OFFSET * 0.55f);
        if (location == Global.getSector().getHyperspace()) {
            staging = OdysseyExpanseSystem.clampToOdysseyHyperspace(
                    staging, 250f);
        }
        return staging;
    }

    private static void configureHostileFleet(CampaignFleetAPI fleet) {
        if (TroyArrivalScript.isFleetBusyForMutation(fleet)) return;
        fleet.setNoAutoDespawn(true);
        fleet.setAbortDespawn(true);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
    }

    private static FleetMemberAPI addMember(
            CampaignFleetAPI fleet, String variantId) {
        return addMember(fleet, variantId, null);
    }

    private static FleetMemberAPI addMember(
            CampaignFleetAPI fleet,
            String variantId,
            String memberId) {
        FleetMemberAPI member = Global.getFactory().createFleetMember(
                FleetMemberType.SHIP, variantId);
        if (memberId != null) member.setId(memberId);
        chiefnavigator.hullmods.StarvingThreatHullmod
                .enforceHivePersonality(member);
        fleet.getFleetData().addFleetMember(member);
        member.getRepairTracker().setCR(
                member.getRepairTracker().getMaxCR());
        return member;
    }

    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return false; }
}
