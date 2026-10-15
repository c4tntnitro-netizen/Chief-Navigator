package chiefnavigator.quest;

import chiefnavigator.campaign.OdysseyAbyssHolePlugin;
import chiefnavigator.weapons.IthacaResearchUpgrades;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.NascentGravityWellAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.StarTypes;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.ids.Planets;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin.DerelictShipData;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin.DerelictType;
import com.fs.starfarer.api.impl.campaign.procgen.Constellation;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec.DropData;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.PerShipData;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.ShipCondition;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.CustomConstellationParams;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.BaseTiledTerrain;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.lwjgl.util.vector.Vector2f;

/** Six-system Odyssey constellation in remote Abyssal hyperspace. */
public final class OdysseyExpanseSystem {
    public static final String SYSTEM_ID = "chief_navigator_odyssey_expanse";
    public static final String ENTRY_ID = "chief_navigator_odyssey_ingress";
    public static final String ENTERED = "$chief_navigator_odyssey_expanse_entered";
    static final String HEGEMONY_FOB_ID =
            "chief_navigator_hegemony_odyssey_camp";
    private static final String HEGEMONY_FOB_TYPE = "station_lowtech2";
    private static final String HEGEMONY_FOB_DERELICT_NAME =
            "Domain Derelict Battlestation";
    private static final String HEGEMONY_FOB_RESTORED =
            "$chief_navigator_hegemony_fob_restored";
    private static final String HEGEMONY_FOB_CREATED_V1 =
            "$chief_navigator_hegemony_fob_created_v1";
    private static final String HEGEMONY_FOB_INVALID_V1 =
            "$chief_navigator_hegemony_fob_invalid_v1";
    private static final String HEGEMONY_FOB_NAME_V2 =
            "$chief_navigator_hegemony_fob_name_v2";

    public static final String PALE_REACH_ID =
            "chief_navigator_odyssey_pale_reach";
    public static final String MESSINA_ID =
            "chief_navigator_odyssey_messina";
    public static final String MESSINA_BREACH_ID =
            "chief_navigator_messina_breach";
    private static final String MESSINA_BREACH_HYPER_ID =
            "chief_navigator_messina_breach_hyper";
    private static final String LEGACY_MESSINA_GRAVITY_WELL_HYPER_ID =
            "chief_navigator_messina_gravity_well_hyper";
    private static final String MESSINA_GRAVITY_WELL_HYPER_ID =
            "chief_navigator_messina_gravity_well_hyper_v2";
    public static final String MESSINA_JUMP_HYPER_ID =
            "chief_navigator_messina_jump_hyper_v3";
    private static final String CLUSTER_JUMP_HYPER_PREFIX =
            "chief_navigator_cluster_jump_hyper_";
    private static final String CLUSTER_SECONDARY_LOCAL_PREFIX =
            "chief_navigator_cluster_secondary_local_";
    private static final String CLUSTER_SECONDARY_HYPER_PREFIX =
            "chief_navigator_cluster_secondary_hyper_";
    private static final String CLUSTER_TERTIARY_LOCAL_PREFIX =
            "chief_navigator_cluster_tertiary_local_";
    private static final String CLUSTER_TERTIARY_HYPER_PREFIX =
            "chief_navigator_cluster_tertiary_hyper_";
    private static final String CLUSTER_GRAVITY_WELL_PREFIX =
            "chief_navigator_cluster_gravity_well_";
    private static final String CLUSTER_BODY_WELL_REPAIR_V3 =
            "$chief_navigator_cluster_body_well_repair_v3";
    private static final String VANILLA_HYPERSPACE_ACCESS_V4 =
            "$chief_navigator_vanilla_hyperspace_access_v4";
    private static final String HYPERSPACE_GENERATION_ATTEMPTED_V1 =
            "$chief_navigator_hyperspace_generation_attempted_v1";
    private static final String HYPERSPACE_ENDPOINT_SPACING_V1 =
            "$chief_navigator_hyperspace_endpoint_spacing_v1";
    private static final String EXPANSE_LOCAL_JUMP_ID =
            "chief_navigator_odyssey_expanse_local_jump";
    private static final String LAST_LIGHT_LOCAL_JUMP_ID =
            "chief_navigator_odyssey_last_light_local_jump";
    public static final String SILENT_WAKE_ID =
            "chief_navigator_odyssey_silent_wake";
    public static final String ASHEN_VERGE_ID =
            "chief_navigator_odyssey_ashen_verge";
    public static final String LAST_LIGHT_ID =
            "chief_navigator_odyssey_last_light";
    public static final String LAST_LIGHT_GUARDIAN_WRECK_ID =
            "chief_navigator_last_light_guardian_wreck";
    public static final String DEVOURED_REACH_ID =
            "chief_navigator_odyssey_devoured_reach";
    public static final String DEVOURED_RING_ID =
            "chief_navigator_devoured_ring";
    private static final String DEVOURED_RING_TYPE =
            "chief_navigator_devoured_ring";
    private static final String AUTHORED_SYSTEM_MARKER =
            "$chief_navigator_odyssey_authored_system_v1";
    private static final String[] HOLLOW_WORLD_IDS = {
        "chief_navigator_devoured_mara",
        "chief_navigator_devoured_vey",
        "chief_navigator_devoured_ketu"
    };
    private static final String[] HOLLOW_WORLD_NAMES = {
        "Mara", "Vey", "Ketu"
    };
    private static final String[] HOLLOW_WORLD_TYPES = {
        "chief_navigator_hollow_world_1",
        "chief_navigator_hollow_world_2",
        "chief_navigator_hollow_world_3"
    };
    private static final String HOLLOW_WORLD_RENDER_V2 =
            "$chief_navigator_hollow_world_system_render_v2";
    private static final float[][] HOLLOW_WORLD_LOCATIONS = {
        {-5700f,  3000f},
        { 5600f,  3400f},
        { 1200f, -6100f}
    };

    private static final String GENERATED_NEBULA =
            "$chief_navigator_odyssey_generated_nebula_v6_messina";
    private static final String GENERATED_WHITE_DWARF =
            "$chief_navigator_odyssey_white_dwarf_v4_messina";
    private static final String GENERATED_MESSINA =
            "$chief_navigator_odyssey_messina_black_hole_v1";
    private static final String GENERATED_SILENT_WAKE =
            "$chief_navigator_odyssey_silent_wake_flow_v1";
    private static final String GENERATED_SANZU_WAKE_DERELICTS =
            "$chief_navigator_sanzu_wake_derelicts_v1";
    static final String GENERATED_ASHEN_VERGE =
            "$chief_navigator_odyssey_ashen_verge_abyss_v1";
    private static final String LEGACY_GENERATED_ASHEN_VERGE_BROWN_DWARF =
            "$chief_navigator_odyssey_ashen_verge_brown_dwarf_v1";
    private static final String GENERATED_ASHEN_VERGE_SUN_V2 =
            "$chief_navigator_odyssey_ashen_verge_sun_v2";
    private static final String GENERATED_ASHEN_VERGE_NEBULA_V1 =
            "$chief_navigator_odyssey_ashen_verge_nebula_v1";
    private static final String GENERATED_RESEARCH_STATIONS =
            "$chief_navigator_odyssey_research_stations_v1";
    private static final String GENERATED_RESEARCH_CORE_LOOT =
            "$chief_navigator_odyssey_research_core_loot_v1";
    private static final String LEGACY_ODYSSEY_COMBAT_CORE_ID =
            "chief_navigator_odyssey_combat_core";
    private static final String RESEARCH_COMPONENT_PROVISIONED_PREFIX =
            "$chief_navigator_odyssey_research_component_provisioned_v1_";
    private static final String RESEARCH_STATION_SALVAGED_PREFIX =
            "$chief_navigator_odyssey_research_station_salvaged_v2_";
    private static final String RESEARCH_STATION_MARKER =
            "$chief_navigator_odyssey_research_station";
    private static final String RESEARCH_SALVAGE_TRACKING_MIGRATED =
            "$chief_navigator_odyssey_research_salvage_tracking_v2";
    private static final String RESEARCH_STATION_MAP_IMPORTANT =
            "chief_navigator_research_station_map";
    private static final String GENERATED_SCATTERED_SALVAGE =
            "$chief_navigator_odyssey_scattered_salvage_v1";
    private static final String GENERATED_LAST_LIGHT_GUARDIAN_WRECK =
            "$chief_navigator_last_light_guardian_wreck_v1";
    private static final String LAST_LIGHT_GUARDIAN_VARIANT =
            "chief_navigator_last_light_guardian_Standard";
    private static final String EXPANSE_RESEARCH_PREFIX =
            "chief_navigator_expanse_research_";
    private static final String SILENT_WAKE_RESEARCH_PREFIX =
            "chief_navigator_silent_wake_research_";
    private static final String ASHEN_VERGE_RESEARCH_PREFIX =
            "chief_navigator_ashen_verge_research_";
    private static final String SILENT_WAKE_FLOW_TYPE =
            "chief_navigator_silent_wake_flow";
    private static final String SILENT_WAKE_FLOW_ID =
            "chief_navigator_silent_wake_flow_anchor";
    private static final String SANZU_WAKE_DERELICT_PREFIX =
            "chief_navigator_sanzu_wake_derelict_";
    private static final int SANZU_DERELICTS_PER_PLANET = 2;
    private static final String[] SANZU_DERELICT_FACTIONS = {
        Factions.HEGEMONY,
        Factions.PERSEAN,
        Factions.TRITACHYON,
        Factions.DIKTAT,
        Factions.LUDDIC_CHURCH,
        Factions.LUDDIC_PATH,
        Factions.PIRATES,
        Factions.INDEPENDENT
    };
    // The serialized custom-entity type keeps its original name so older saves
    // can load it before the obsolete Ashen Verge anchor is migrated.
    private static final String MESSINA_BLACK_CLOUD_TYPE =
            "chief_navigator_ashen_verge_clouds";
    private static final String MESSINA_BLACK_CLOUD_ID =
            "chief_navigator_messina_black_cloud_anchor";
    private static final String LEGACY_ASHEN_VERGE_CLOUD_ID =
            "chief_navigator_ashen_verge_cloud_anchor";
    private static final String ASHEN_VERGE_NEBULA_ID =
            "chief_navigator_ashen_verge_nebula";
    private static final String ASHEN_VERGE_NEBULA_MARKER =
            "$chief_navigator_ashen_verge_nebula_layer";
    private static final String ASHEN_VERGE_NEBULA_MASK =
            "graphics/terrain/chief_navigator_ashen_verge_nebula_mask.png";
    private static final String ASHEN_VERGE_BACKGROUND =
            "graphics/backgrounds/chief_navigator_ashen_verge_nebula_v1.jpg";
    private static final String ASHEN_VERGE_NEBULA_TEXTURE =
            "chief_navigator_ashen_verge_nebula_v1";
    private static final String FOB_ITHACA_TYPE =
            "chief_navigator_fob_ithaca_campaign";
    private static final String FOB_ITHACA_ID =
            "chief_navigator_fob_ithaca";
    public static final String FOB_ITHACA_GATE_ID =
            "chief_navigator_fob_ithaca_gate";
    private static final String FOB_ITHACA_GATE_MARKER =
            "$chief_navigator_fob_ithaca_gate_v1";
    public static final String FOB_ITHACA_GATE_DEFENSE_STATION_ID =
            "chief_navigator_fob_ithaca_gate_defense_station";
    static final String FOB_ITHACA_BREACH_DEFENSE_NAME =
            "Breach Defense Station";
    public static final String FOB_ITHACA_GATE_DEFENSE_VARIANT_ID =
            "chief_navigator_spartan_battlestation_Teal";
    public static final String FOB_ITHACA_GATE_DEFENSE_HULL_ID =
            "chief_navigator_spartan_battlestation";
    private static final String FOB_ITHACA_GATE_DEFENSE_CREATED_V1 =
            "$chief_navigator_fob_ithaca_gate_defense_created_v1";
    private static final String FOB_ITHACA_GATE_DEFENSE_INVALID_V1 =
            "$chief_navigator_fob_ithaca_gate_defense_invalid_v1";
    private static final String FOB_ITHACA_GATE_IMPORTANT =
            "chief_navigator_fob_ithaca_gate_visible";
    public static final String FOB_ITHACA_SPARTAN_GUARD_ID =
            "chief_navigator_fob_ithaca_spartan_guard";
    public static final String FOB_ITHACA_SPARTAN_MOUTH_NORTH_ID =
            "chief_navigator_fob_ithaca_spartan_mouth_north";
    public static final String FOB_ITHACA_SPARTAN_MOUTH_SOUTH_ID =
            "chief_navigator_fob_ithaca_spartan_mouth_south";
    private static final String FOB_ITHACA_SPARTAN_MOUTH_NORTH_ANCHOR_ID =
            "chief_navigator_fob_ithaca_spartan_mouth_north_anchor";
    private static final String FOB_ITHACA_SPARTAN_MOUTH_SOUTH_ANCHOR_ID =
            "chief_navigator_fob_ithaca_spartan_mouth_south_anchor";
    private static final String FOB_ITHACA_SPARTAN_GUARD_CREATED_V1 =
            "$chief_navigator_fob_ithaca_spartan_guard_created_v1";
    private static final String FOB_ITHACA_SPARTAN_GUARD_INVALID_V1 =
            "$chief_navigator_fob_ithaca_spartan_guard_invalid_v1";
    private static final String FOB_ITHACA_SPARTAN_FLEET_MARKER =
            "$chief_navigator_fob_ithaca_spartan_guard_fleet_v1";
    private static final String FOB_ITHACA_SPARTAN_MOUTH_CREATED_PREFIX =
            "$chief_navigator_fob_ithaca_spartan_mouth_created_v1_";
    private static final String FOB_ITHACA_SPARTAN_MOUTH_INVALID_PREFIX =
            "$chief_navigator_fob_ithaca_spartan_mouth_invalid_v1_";
    private static final String FOB_ITHACA_SPARTAN_MOUTH_ANCHOR_CREATED_PREFIX =
            "$chief_navigator_fob_ithaca_spartan_mouth_anchor_created_v1_";
    private static final String FOB_ITHACA_SPARTAN_MOUTH_ANCHOR_MARKER =
            "$chief_navigator_fob_ithaca_spartan_mouth_anchor_v1";
    private static final String FOB_ITHACA_SPARTAN_MOUTH_ANCHOR_INVALID_PREFIX =
            "$chief_navigator_fob_ithaca_spartan_mouth_anchor_invalid_v1_";
    private static final String[] FOB_ITHACA_SPARTAN_MOUTH_IDS = {
        FOB_ITHACA_SPARTAN_MOUTH_NORTH_ID,
        FOB_ITHACA_SPARTAN_MOUTH_SOUTH_ID
    };
    private static final String[] FOB_ITHACA_SPARTAN_MOUTH_ANCHOR_IDS = {
        FOB_ITHACA_SPARTAN_MOUTH_NORTH_ANCHOR_ID,
        FOB_ITHACA_SPARTAN_MOUTH_SOUTH_ANCHOR_ID
    };
    private static final String[] FOB_ITHACA_SPARTAN_MOUTH_NAMES = {
        "Task Force Spartan Mouth Guard North",
        "Task Force Spartan Mouth Guard South"
    };
    private static final float[][] FOB_ITHACA_SPARTAN_MOUTH_OFFSETS = {
        {-2150f, 500f},
        {-2150f, -500f}
    };
    private static final float FOB_ITHACA_GATE_DEFENSE_OFFSET_X = -1620f;
    private static final float FOB_ITHACA_GATE_DEFENSE_OFFSET_Y = 0f;
    private static final String LEGACY_SPARTAN_THREAT_WARD_ID =
            "chief_navigator_spartan_threat_ward";
    private static final String FOB_ITHACA_FINAL_LABOR_ATTRITION_LOCK =
            "$chief_navigator_fob_ithaca_final_labor_attrition_lock_v1";
    private static final String[] FOB_ITHACA_SPARTAN_GATE_VARIANTS = {
        "onslaught_xiv_Elite",
        "onslaught_xiv_Elite",
        "onslaught_xiv_Elite",
        "dominator_XIV_Elite",
        "dominator_XIV_Elite",
        "dominator_XIV_Elite",
        "dominator_XIV_Elite",
        "lasher_Assault",
        "lasher_Assault",
        "lasher_Assault",
        "lasher_Assault",
        "lasher_Assault"
    };
    private static final String[] FOB_ITHACA_SPARTAN_GATE_HULLS = {
        "onslaught_xiv",
        "onslaught_xiv",
        "onslaught_xiv",
        "dominator_xiv",
        "dominator_xiv",
        "dominator_xiv",
        "dominator_xiv",
        "lasher",
        "lasher",
        "lasher",
        "lasher",
        "lasher"
    };
    private static final String[][] FOB_ITHACA_SPARTAN_MOUTH_VARIANTS = {
        {
            "onslaught_xiv_Elite",
            "dominator_XIV_Elite",
            "dominator_XIV_Elite",
            "enforcer_XIV_Elite",
            "enforcer_XIV_Elite",
            "lasher_Assault",
            "lasher_Assault",
            "lasher_Assault"
        },
        {
            "legion_xiv_Elite",
            "dominator_XIV_Elite",
            "dominator_XIV_Elite",
            "enforcer_XIV_Elite",
            "enforcer_XIV_Elite",
            "lasher_Assault",
            "lasher_Assault",
            "lasher_Assault"
        }
    };
    private static final String[][] FOB_ITHACA_SPARTAN_MOUTH_HULLS = {
        {
            "onslaught_xiv",
            "dominator_xiv",
            "dominator_xiv",
            "enforcer_xiv",
            "enforcer_xiv",
            "lasher",
            "lasher",
            "lasher"
        },
        {
            "legion_xiv",
            "dominator_xiv",
            "dominator_xiv",
            "enforcer_xiv",
            "enforcer_xiv",
            "lasher",
            "lasher",
            "lasher"
        }
    };
    private static final String FOB_ITHACA_IMPORTANT =
            "chief_navigator_fob_ithaca_visible";
    private static final String FOB_ITHACA_OWNER_MARKER =
            "$chief_navigator_fob_ithaca_owned_v1";
    private static final String FOB_ITHACA_MARKET_ID =
            "chief_navigator_fob_ithaca_storage_market";
    private static final String FOB_ITHACA_MARKET_MARKER =
            "$chief_navigator_fob_ithaca_market_v2";
    private static final float FOB_ITHACA_INTERACTION_RADIUS = 750f;
    private static final String FOB_ITHACA_RING_COLLISION_TYPE =
            "chief_navigator_fob_ithaca_ring_collision";
    private static final String FOB_ITHACA_RING_COLLISION_MARKER =
            "$chief_navigator_fob_ithaca_ring_collision_v1";
    private static final String FOB_ITHACA_RING_COLLISION_PREFIX =
            "chief_navigator_fob_ithaca_ring_collision_";
    private static final int FOB_ITHACA_RING_COLLISION_COUNT = 29;
    private static final float FOB_ITHACA_RING_COLLISION_RADIUS = 185f;
    private static final float FOB_ITHACA_RING_CENTERLINE_RADIUS = 1620f;
    private static final float FOB_ITHACA_RING_ARC_START = -160f;
    private static final float FOB_ITHACA_RING_ARC_END = 160f;
    private static final String SILENT_WAKE_PASSAGE_ID =
            "chief_navigator_silent_wake_passage";
    private static final String ASHEN_VERGE_PASSAGE_ID =
            "chief_navigator_ashen_verge_passage";
    private static final String MESSINA_BREACH_VISUAL_ID =
            "chief_navigator_messina_breach_visual_system";
    private static final String MESSINA_BREACH_HYPER_VISUAL_ID =
            "chief_navigator_messina_breach_visual_hyper";
    private static final String MESSINA_GRAVITY_VISUAL_ID =
            "chief_navigator_messina_gravity_visual_hyper";
    private static final String MESSINA_SHOWCASE_ANCHOR_ID =
            "chief_navigator_messina_showcase_anchor";
    private static final String NORMAL_MESSINA_ACCESS_CONFIGURED =
            "$chief_navigator_odyssey_single_messina_jump_v2";
    private static final String SHOWCASE_ANCHOR_ID =
            "chief_navigator_odyssey_showcase_anchor";
    private static final String ENTRY_IMPORTANT =
            "chief_navigator_odyssey_ingress";
    private static final String FOXTROT_TERMINUS_NAME =
            "Foxtrot Terminus Aperture";
    private static final String CLUSTER_ACCESS_DETECTED =
            "chief_navigator_cluster_access_detected";
    private static final float SYSTEM_ACCESS_VISIBLE_RANGE = 18000f;
    /** The Avici ingress remains a conspicuously vast drain into the Abyss. */
    private static final float AVICI_PRIMARY_HYPER_RADIUS = 350f;
    /** Other hyperspace-facing endpoints vary deterministically in this range. */
    private static final float MIN_HYPERSPACE_JUMP_RADIUS = 55f;
    private static final float HYPERSPACE_JUMP_RADIUS_VARIATION = 41f;

    private static final float ENTRY_OFFSET_X = 2000f;
    private static final float ENTRY_OFFSET_Y = 1000f;
    private static final float ENTRY_X = -14000f;
    private static final float HOLE_RADIUS = 7400f;
    private static final float HOLE_FEATHER_RADIUS = 9600f;
    private static final float LEGACY_HOLE_FEATHER_RADIUS = 20000f;
    private static final long HYPER_ENDPOINT_SCATTER_SEED =
            0x4f72696f6e4b6e6fL;
    private static final float MIN_HYPER_ENDPOINT_OFFSET = 1150f;
    private static final float HYPER_ENDPOINT_OFFSET_VARIATION = 400f;
    /** Vanilla-style radius for body-linked nascent gravity wells. */
    private static final float CLUSTER_BODY_WELL_RADIUS = 470f;
    private static final float REMOTE_HOLE_X_FACTOR = 0.9f;
    private static final float REMOTE_HOLE_Y_FACTOR = -0.9f;

    /** x, y, visual radius; shared with the baked Sanzu flow field. */
    private static final float[][] SILENT_WAKE_OBSTACLES = {
        {0f, 0f, 760f},
        {-3900f, 4100f, 760f},
        {3350f, 950f, 1040f},
        {-1850f, -4300f, 850f}
    };
    private static final OrbitPlacement[] EXPANSE_RESEARCH_STATIONS = {
        orbit(SYSTEM_ID + "_world_ossa", 70f, 1100f, 90f, 25f),
        orbit(SYSTEM_ID + "_world_aulis", 210f, 1000f, 85f, 205f)
    };
    private static final OrbitPlacement[] SILENT_WAKE_RESEARCH_STATIONS = {
        orbit(SILENT_WAKE_ID + "_lower_rock", 310f, 1100f, 88f, 310f),
        orbit(SILENT_WAKE_ID + "_upper_rock", 125f, 1050f, 82f, 125f)
    };
    private static final OrbitPlacement[] ASHEN_VERGE_RESEARCH_STATIONS = {
        orbit(ASHEN_VERGE_ID + "_world_scamander",
                75f, 1000f, 84f, 70f),
        orbit(ASHEN_VERGE_ID + "_world_simois",
                220f, 1000f, 86f, 245f)
    };
    private static final String[] SILENT_WAKE_PLANET_IDS = {
        SILENT_WAKE_ID + "_upper_rock",
        SILENT_WAKE_ID + "_great_rock",
        SILENT_WAKE_ID + "_lower_rock"
    };
    private static final String[] SILENT_WAKE_PLANET_NAMES = {
        "Cairn",
        "Breakwater",
        "Undertow"
    };
    private static final String[] SILENT_WAKE_PLANET_TYPES = {
        Planets.BARREN3,
        Planets.ICE_GIANT,
        Planets.ROCKY_ICE
    };

    /**
     * Ordinary worlds left behind by the sector's dead settlements. Devoured
     * Reach is deliberately absent: only its three eaten planets belong there.
     */
    private static final WorldSpec[] EXPANSE_WORLDS = {
        new WorldSpec("ithome", "Ithome", "barren2",
                24f, 135f, 3600f, 145f, Conditions.RUINS_WIDESPREAD),
        new WorldSpec("ossa", "Ossa", "rocky_metallic",
                137f, 105f, 5200f, 235f, Conditions.RUINS_SCATTERED),
        new WorldSpec("pelion", "Pelion", "frozen",
                248f, 165f, 7000f, 355f, Conditions.RUINS_EXTENSIVE),
        new WorldSpec("aulis", "Aulis", "barren-bombarded",
                326f, 115f, 8600f, 485f, null)
    };
    private static final SalvageSiteSpec[] EXPANSE_SALVAGE = {
        habitat("argosy_habitat", SYSTEM_ID + "_world_ossa",
                210f, 650f, 55f, 18f),
        habitat("pilgrim_habitat", SYSTEM_ID + "_world_ithome",
                45f, 700f, 62f, 211f),
        mining("ithome_mine", SYSTEM_ID + "_world_ithome",
                225f, 900f, 78f, 74f),
        mining("pelion_mine", SYSTEM_ID + "_world_pelion",
                330f, 900f, 85f, 286f)
    };

    private static final WorldSpec[] AVICI_WORLDS = {
        new WorldSpec("raurava", "Raurava", "irradiated",
                105f, 120f, 6100f, 280f, Conditions.RUINS_EXTENSIVE),
        new WorldSpec("kalasutra", "Kalasutra", "barren3",
                214f, 150f, 7800f, 410f, Conditions.RUINS_WIDESPREAD),
        new WorldSpec("sanghata", "Sanghata", "rocky_ice",
                314f, 110f, 9400f, 570f, null)
    };
    private static final SalvageSiteSpec[] AVICI_SALVAGE = {
        habitat("raurava_habitat", MESSINA_ID + "_world_raurava",
                135f, 700f, 60f, 42f),
        habitat("kalasutra_habitat", MESSINA_ID + "_world_kalasutra",
                30f, 750f, 65f, 187f),
        mining("raurava_mine", MESSINA_ID + "_world_raurava",
                310f, 950f, 82f, 106f),
        mining("sanghata_mine", MESSINA_ID + "_world_sanghata",
                160f, 850f, 76f, 301f)
    };

    /** Sanzu's planets stay fixed because they are obstacles in its flow map. */
    private static final String[] SANZU_RUINS = {
        Conditions.RUINS_WIDESPREAD,
        null,
        Conditions.RUINS_EXTENSIVE
    };
    private static final SalvageSiteSpec[] SANZU_SALVAGE = {
        habitat("north_habitat", SILENT_WAKE_ID + "_upper_rock",
                120f, 750f, 66f, 335f),
        habitat("undertow_habitat", SILENT_WAKE_ID + "_lower_rock",
                35f, 700f, 62f, 145f),
        mining("breakwater_mine", SILENT_WAKE_ID + "_great_rock",
                210f, 900f, 80f, 80f),
        mining("south_mine", SILENT_WAKE_ID + "_lower_rock",
                230f, 1000f, 86f, 255f)
    };

    private static final WorldSpec[] ASHEN_VERGE_WORLDS = {
        new WorldSpec("ilion", "Ilion", "barren-bombarded",
                18f, 175f, 2700f, 92f, Conditions.RUINS_VAST),
        new WorldSpec("dardania", "Dardania", "desert",
                76f, 145f, 3900f, 150f, Conditions.RUINS_EXTENSIVE),
        new WorldSpec("scamander", "Scamander", "irradiated",
                136f, 120f, 5100f, 220f, Conditions.RUINS_WIDESPREAD),
        new WorldSpec("ida", "Ida", "frozen",
                188f, 165f, 6400f, 310f, Conditions.RUINS_EXTENSIVE),
        new WorldSpec("rhoeteum", "Rhoeteum", "barren2",
                231f, 100f, 7600f, 390f, Conditions.RUINS_WIDESPREAD),
        new WorldSpec("tenedos", "Tenedos", "rocky_metallic",
                286f, 135f, 8900f, 510f, Conditions.RUINS_VAST),
        new WorldSpec("simois", "Simois", "barren3",
                337f, 105f, 10300f, 680f, Conditions.RUINS_EXTENSIVE)
    };
    private static final SalvageSiteSpec[] ASHEN_VERGE_SALVAGE = {
        habitat("ilion_habitat", ASHEN_VERGE_ID + "_world_ilion",
                22f, 750f, 64f, 22f),
        habitat("dardania_habitat", ASHEN_VERGE_ID + "_world_dardania",
                81f, 700f, 62f, 81f),
        habitat("ida_habitat", ASHEN_VERGE_ID + "_world_ida",
                164f, 800f, 70f, 164f),
        habitat("tenedos_habitat", ASHEN_VERGE_ID + "_world_tenedos",
                247f, 800f, 70f, 247f),
        mining("scamander_mine", ASHEN_VERGE_ID + "_world_scamander",
                51f, 850f, 74f, 51f),
        mining("ida_mine", ASHEN_VERGE_ID + "_world_ida",
                316f, 1050f, 88f, 116f),
        mining("rhoeteum_mine", ASHEN_VERGE_ID + "_world_rhoeteum",
                198f, 800f, 72f, 198f),
        mining("tenedos_mine", ASHEN_VERGE_ID + "_world_tenedos",
                74f, 1100f, 92f, 274f),
        mining("simois_mine", ASHEN_VERGE_ID + "_world_simois",
                329f, 800f, 72f, 329f)
    };

    private static final WorldSpec[] LAST_LIGHT_WORLDS = {
        new WorldSpec("vigil", "Vigil", "barren",
                31f, 130f, 3300f, 135f, Conditions.RUINS_EXTENSIVE),
        new WorldSpec("afterglow", "Afterglow", "irradiated",
                112f, 155f, 4900f, 225f, Conditions.RUINS_WIDESPREAD),
        new WorldSpec("ember", "Ember", "desert",
                191f, 120f, 6500f, 335f, Conditions.RUINS_EXTENSIVE),
        new WorldSpec("cinder", "Cinder", "rocky_metallic",
                267f, 100f, 7900f, 450f, Conditions.RUINS_SCATTERED),
        new WorldSpec("gloam", "Gloam", "frozen",
                329f, 170f, 9500f, 610f, null)
    };
    private static final SalvageSiteSpec[] LAST_LIGHT_SALVAGE = {
        habitat("vigil_habitat", LAST_LIGHT_ID + "_world_vigil",
                57f, 700f, 62f, 57f),
        habitat("gloam_habitat", LAST_LIGHT_ID + "_world_gloam",
                224f, 850f, 74f, 224f),
        mining("afterglow_mine", LAST_LIGHT_ID + "_world_afterglow",
                119f, 900f, 78f, 119f),
        mining("ember_mine", LAST_LIGHT_ID + "_world_ember",
                302f, 850f, 74f, 302f)
    };

    private static final String[] PLACEHOLDER_IDS = {
        MESSINA_ID,
        SILENT_WAKE_ID,
        ASHEN_VERGE_ID,
        LAST_LIGHT_ID,
        DEVOURED_REACH_ID
    };
    private static final String[] PLACEHOLDER_NAMES = {
        "Avici",
        "Sanzu",
        "Ashen Verge",
        "Last Light",
        "Devoured Reach"
    };
    /**
     * Hyperspace offsets from the center of the Odyssey Sector pocket.
     * Alpha Odyssey is at (2000, 1000). Its two southern route legs remain
     * identical: (2250, -3250) to Sanzu and again to Avici. The other three
     * systems sit roughly one route-leg away from their stated neighbor.
     */
    private static final float[][] PLACEHOLDER_OFFSETS = {
        {6500f, -5500f},
        {4250f, -2250f},
        {5900f,  1000f},
        {800f,  -2700f},
        {5250f,  4845f}
    };
    private OdysseyExpanseSystem() { }

    /** Package-visible topology probe used by the headless layout regression. */
    static float[] getAuthoredHyperspaceOffset(String systemId) {
        if (SYSTEM_ID.equals(systemId)) {
            return new float[] {ENTRY_OFFSET_X, ENTRY_OFFSET_Y};
        }
        for (int index = 0; index < PLACEHOLDER_IDS.length; index++) {
            if (PLACEHOLDER_IDS[index].equals(systemId)) {
                return new float[] {
                    PLACEHOLDER_OFFSETS[index][0],
                    PLACEHOLDER_OFFSETS[index][1]
                };
            }
        }
        return null;
    }

    public static StarSystemAPI ensureExists() {
        StarSystemAPI existing = findExisting();
        // Static world topology is created once and is serialized by the
        // campaign.  A normal load, chart open, or interaction must never
        // turn into a migration/rebuild pass: doing so makes incomplete or
        // transient engine bookkeeping capable of deleting healthy objects.
        // Preserve serialized entity identity. The one allowed topology
        // migration below changes only the fixed positions of known linked
        // endpoints so older saves do not retain three overlapping apertures.
        if (existing != null) {
            repairClusterJumpPointSpacing(existing);
            return existing;
        }

        StarSystemAPI mainClaim = findSystemClaim(SYSTEM_ID);
        if (mainClaim != null) {
            Global.getLogger(OdysseyExpanseSystem.class).error(
                    "Odyssey Sector system ID is already owned by "
                            + mainClaim.getName()
                            + "; refusing to create or rewrite it");
            return null;
        }
        for (String id : PLACEHOLDER_IDS) {
            StarSystemAPI claim = findSystemClaim(id);
            if (claim == null) continue;
            Global.getLogger(OdysseyExpanseSystem.class).error(
                    "Odyssey Sector subsystem ID " + id
                            + " is already owned by " + claim.getName()
                            + "; refusing partial generation");
            return null;
        }

        StarSystemAPI expanse = createGeneratedNebula();

        configureExpanse(expanse);
        ensureEntry(expanse);
        ensureHegemonyFobBattlestation(expanse);

        List<StarSystemAPI> placeholders = ensureClusterSystems();
        ensureScatteredSystemContent(expanse, placeholders);
        ensureResearchStationContent(expanse, placeholders);
        configureClusterJumpPoints(expanse, placeholders);
        bindConstellation(expanse, placeholders);
        ensureSystemMusic(expanse, placeholders);
        reserveAbyssalHole();
        ensureShowcaseEncounters(expanse, findSystem(DEVOURED_REACH_ID));
        return expanse;
    }

    /** Applies the position-only endpoint repair during ordinary save loads. */
    public static void repairClusterJumpPointSpacing() {
        StarSystemAPI expanse = findExisting();
        if (expanse != null) repairClusterJumpPointSpacing(expanse);
    }

    /**
     * Uses Starsector's native location-music contract. This survives combat
     * and is also honored by replacement MusicPlayerPlugin implementations.
     */
    private static void ensureSystemMusic(
            StarSystemAPI expanse,
            List<StarSystemAPI> clusterSystems) {
        setSystemMusic(expanse,
                chiefnavigator.campaign.AlphaOdysseyMusicScript.MUSIC_ID);
        for (StarSystemAPI system : clusterSystems) {
            if (system == null) continue;
            String id = system.getOptionalUniqueId();
            if (SILENT_WAKE_ID.equals(id)) {
                setSystemMusic(system,
                        chiefnavigator.campaign.SanzuMusicScript.MUSIC_ID);
            } else if (MESSINA_ID.equals(id)) {
                setSystemMusic(system,
                        chiefnavigator.campaign.AviciMusicScript.MUSIC_ID);
            }
        }
    }

    private static void setSystemMusic(
            StarSystemAPI system, String musicSetId) {
        if (system == null || musicSetId == null) return;
        system.getMemoryWithoutUpdate().set("$musicSetId", musicSetId);
    }

    public static StarSystemAPI findExisting() {
        for (StarSystemAPI candidate : Global.getSector().getStarSystems()) {
            if (!SYSTEM_ID.equals(candidate.getOptionalUniqueId())) continue;
            if (isOwnedSystem(candidate, SYSTEM_ID)) return candidate;
        }
        return null;
    }

    public static boolean isInside(LocationAPI location) {
        if (!(location instanceof StarSystemAPI)) return false;
        StarSystemAPI system = (StarSystemAPI) location;
        String id = system.getOptionalUniqueId();
        if (SYSTEM_ID.equals(id)) return isOwnedSystem(system, id);
        for (String placeholderId : PLACEHOLDER_IDS) {
            if (placeholderId.equals(id)) {
                return isOwnedSystem(system, id);
            }
        }
        return false;
    }

    private static StarSystemAPI createGeneratedNebula() {
        CustomConstellationParams params =
                new CustomConstellationParams(StarAge.OLD);
        params.name = "Odyssey Sector";
        params.numStars = 1;
        params.forceNebula = true;
        params.systemTypes.add(StarSystemType.NEBULA);
        params.location = getExpanseLocation();

        Constellation constellation = new StarSystemGenerator(params).generate();
        if (constellation == null || constellation.getSystems().isEmpty()) {
            throw new IllegalStateException(
                    "Failed to generate the Odyssey Expanse nebula system");
        }
        StarSystemAPI system = constellation.getSystems().get(0);
        system.setOptionalUniqueId(SYSTEM_ID);
        system.getMemoryWithoutUpdate().set(GENERATED_NEBULA, true);
        return system;
    }

    private static void configureExpanse(StarSystemAPI system) {
        system.setName("The Odyssey Expanse");
        system.setOptionalUniqueId(SYSTEM_ID);
        system.getLocation().set(getExpanseLocation());
        system.setMapGridWidthOverride(36000f);
        system.setMapGridHeightOverride(30000f);
        system.setMaxRadiusInHyperspace(1450f);
        system.setDoNotShowIntelFromThisLocationOnMap(false);
        system.setProcgen(false);
        system.getMemoryWithoutUpdate().set(GENERATED_NEBULA, true);
        system.getMemoryWithoutUpdate().set(AUTHORED_SYSTEM_MARKER, true);
        applyClusterTags(system);
    }

    public static SectorEntityToken ensureEntry(StarSystemAPI system) {
        if (system == null) return null;
        SectorEntityToken existing = system.getEntityById(ENTRY_ID);
        if (existing != null && !isEntry(existing)) {
            Global.getLogger(OdysseyExpanseSystem.class).error(
                    "Foxtrot Terminus ID contains incompatible serialized "
                            + "state; refusing replacement");
            return null;
        }
        JumpPointAPI entry;
        if (existing instanceof JumpPointAPI) {
            entry = (JumpPointAPI) existing;
        } else {
            entry = Global.getFactory().createJumpPoint(
                    ENTRY_ID, FOXTROT_TERMINUS_NAME);
            entry.setAutoCreateEntranceFromHyperspace(false);
            system.addEntity(entry);
        }
        entry.setName(FOXTROT_TERMINUS_NAME);
        entry.setLocation(ENTRY_X, 0f);
        entry.setRadius(175f);
        makePermanentMapLandmark(entry);
        entry.getDetectedRangeMod().modifyFlat(ENTRY_IMPORTANT, 12000f);
        entry.setStandardWormholeToStarfieldVisual();
        entry.forceOpen();
        entry.setInteractionImage("illustrations", "jump_point_hyper");
        Misc.makeImportant(entry, ENTRY_IMPORTANT);
        return entry;
    }

    /** Returns the serialized Foxtrot endpoint without creating a replacement. */
    public static SectorEntityToken findEntry(StarSystemAPI system) {
        if (system == null) return null;
        SectorEntityToken entry = system.getEntityById(ENTRY_ID);
        return isEntry(entry) ? entry : null;
    }

    /**
     * Keeps the abandoned low-tech battlestation beside Old Milix. Older
     * saves used this id for an invisible camp token; replacing that token
     * makes the Hegemony expedition's eventual foothold a physical landmark.
     */
    static SectorEntityToken ensureHegemonyFobBattlestation(
            StarSystemAPI system) {
        if (system == null) return null;
        SectorEntityToken station = system.getEntityById(HEGEMONY_FOB_ID);
        boolean created = false;
        if (station != null
                && !HEGEMONY_FOB_TYPE.equals(
                        station.getCustomEntityType())) {
            if (!system.getMemoryWithoutUpdate().getBoolean(
                    HEGEMONY_FOB_INVALID_V1)) {
                Global.getLogger(OdysseyExpanseSystem.class).error(
                        "Hegemony FOB ID contains incompatible serialized "
                                + "state; refusing replacement");
                system.getMemoryWithoutUpdate().set(
                        HEGEMONY_FOB_INVALID_V1, true);
            }
            return null;
        }
        if (station == null) {
            if (system.getMemoryWithoutUpdate().getBoolean(
                    HEGEMONY_FOB_CREATED_V1)) {
                return null;
            }
            station = system.addCustomEntity(
                    HEGEMONY_FOB_ID,
                    HEGEMONY_FOB_DERELICT_NAME,
                    HEGEMONY_FOB_TYPE,
                    Factions.DERELICT);
            system.getMemoryWithoutUpdate().set(
                    HEGEMONY_FOB_CREATED_V1, true);
            created = true;
        } else {
            system.getMemoryWithoutUpdate().set(
                    HEGEMONY_FOB_CREATED_V1, true);
        }

        PlanetAPI anchor = findHegemonyFobWorld(system);
        if (anchor != null && station.getOrbitFocus() != anchor) {
            float orbitRadius = anchor.getRadius() + 620f;
            station.setCircularOrbitPointingDown(
                    anchor,
                    218f,
                    orbitRadius,
                    Math.max(55f, orbitRadius / 11f));
        } else if (anchor == null && station.getOrbitFocus() == null) {
            station.setFixedLocation(-6500f, 4800f);
        }

        migrateHegemonyFobName(system, station);
        station.setInteractionImage("illustrations", "orbital");
        makePermanentMapLandmark(station);
        if (created) configureHegemonyFobBattlestation(station, false);
        return station;
    }

    /** One-time rename of the exact legacy label; preserves the saved token. */
    private static void migrateHegemonyFobName(
            StarSystemAPI system,
            SectorEntityToken station) {
        if (system.getMemoryWithoutUpdate().getBoolean(
                HEGEMONY_FOB_NAME_V2)) return;
        if (!station.getMemoryWithoutUpdate().getBoolean(
                    HEGEMONY_FOB_RESTORED)
                && "Derelict Battlestation".equals(station.getName())) {
            station.setName(HEGEMONY_FOB_DERELICT_NAME);
        }
        system.getMemoryWithoutUpdate().set(HEGEMONY_FOB_NAME_V2, true);
    }

    /** Returns the serialized FOB token only when it is the authored type. */
    static SectorEntityToken findHegemonyFobBattlestation(
            StarSystemAPI system) {
        if (system == null) return null;
        SectorEntityToken station = system.getEntityById(HEGEMONY_FOB_ID);
        return station != null && HEGEMONY_FOB_TYPE.equals(
                station.getCustomEntityType()) ? station : null;
    }

    /** True only while Old Milix's authored battlestation is abandoned. */
    public static boolean isDomainDerelictBattlestation(
            SectorEntityToken target) {
        return target != null
                && HEGEMONY_FOB_ID.equals(target.getId())
                && HEGEMONY_FOB_TYPE.equals(target.getCustomEntityType())
                && !target.getMemoryWithoutUpdate().getBoolean(
                        HEGEMONY_FOB_RESTORED);
    }

    /** Switches the same station between its abandoned and occupied states. */
    static void configureHegemonyFobBattlestation(
            SectorEntityToken station,
            boolean restored) {
        if (station == null || !HEGEMONY_FOB_TYPE.equals(
                station.getCustomEntityType())) return;
        if (restored) {
            station.setName("Hegemony Expeditionary Forward Base");
            station.setFaction(HegemonyExpeditionFaction.ID);
            station.getMemoryWithoutUpdate().unset("$abandonedStation");
            station.getMemoryWithoutUpdate().set(
                    HEGEMONY_FOB_RESTORED, true);
        } else {
            station.setName(HEGEMONY_FOB_DERELICT_NAME);
            station.setFaction(Factions.DERELICT);
            station.getMemoryWithoutUpdate().set("$abandonedStation", true);
            station.getMemoryWithoutUpdate().unset(
                    HEGEMONY_FOB_RESTORED);
        }
    }

    private static PlanetAPI findHegemonyFobWorld(StarSystemAPI system) {
        PlanetAPI largestGasGiant = null;
        PlanetAPI largestWorld = null;
        for (PlanetAPI planet : system.getPlanets()) {
            if (planet == null || planet.isStar()) continue;
            String name = planet.getName();
            if (name != null && "Old Milix".equalsIgnoreCase(name.trim())) {
                return planet;
            }
            if (largestWorld == null
                    || planet.getRadius() > largestWorld.getRadius()) {
                largestWorld = planet;
            }
            if (planet.isGasGiant()
                    && (largestGasGiant == null
                            || planet.getRadius()
                                    > largestGasGiant.getRadius())) {
                largestGasGiant = planet;
            }
        }
        return largestGasGiant != null ? largestGasGiant : largestWorld;
    }

    private static List<StarSystemAPI> ensureClusterSystems() {
        List<StarSystemAPI> systems = new ArrayList<StarSystemAPI>();
        for (int index = 0; index < PLACEHOLDER_IDS.length; index++) {
            String id = PLACEHOLDER_IDS[index];
            String name = PLACEHOLDER_NAMES[index];
            if (MESSINA_ID.equals(id)) {
                systems.add(ensureMessinaSystem(PLACEHOLDER_OFFSETS[index]));
                continue;
            }
            if (SILENT_WAKE_ID.equals(id)) {
                systems.add(ensureSilentWakeSystem(PLACEHOLDER_OFFSETS[index]));
                continue;
            }
            if (ASHEN_VERGE_ID.equals(id)) {
                systems.add(ensureAshenVergeSystem(PLACEHOLDER_OFFSETS[index]));
                continue;
            }
            if (DEVOURED_REACH_ID.equals(id)) {
                systems.add(ensureDevouredReachSystem(
                        PLACEHOLDER_OFFSETS[index]));
                continue;
            }
            StarSystemAPI system = findSystem(id);
            boolean created = system == null;
            if (created) system = Global.getSector().createStarSystem(name);

            configureClusterSystem(system, id, name, PLACEHOLDER_OFFSETS[index]);
            if (created || !system.getMemoryWithoutUpdate().getBoolean(
                    GENERATED_WHITE_DWARF)) {
                if (system.getStar() == null) {
                    PlanetAPI star = system.initStar(
                            id + "_star", StarTypes.WHITE_DWARF, 450f, 300f);
                    star.setName(name);
                }
                system.getMemoryWithoutUpdate().set(GENERATED_WHITE_DWARF, true);
            }
            systems.add(system);
        }
        return systems;
    }

    /** Populates every ordinary destination while leaving Devoured Reach bare. */
    private static void ensureScatteredSystemContent(
            StarSystemAPI expanse,
            List<StarSystemAPI> clusterSystems) {
        ensureAuthoredWorlds(expanse, EXPANSE_WORLDS);
        ensureScatteredSalvage(expanse, EXPANSE_SALVAGE);

        for (StarSystemAPI system : clusterSystems) {
            if (system == null) continue;
            String id = system.getOptionalUniqueId();
            if (MESSINA_ID.equals(id)) {
                ensureAuthoredWorlds(system, AVICI_WORLDS);
                ensureScatteredSalvage(system, AVICI_SALVAGE);
            } else if (SILENT_WAKE_ID.equals(id)) {
                ensureSanzuRuins(system);
                ensureScatteredSalvage(system, SANZU_SALVAGE);
            } else if (ASHEN_VERGE_ID.equals(id)) {
                ensureAuthoredWorlds(system, ASHEN_VERGE_WORLDS);
                ensureScatteredSalvage(system, ASHEN_VERGE_SALVAGE);
            } else if (LAST_LIGHT_ID.equals(id)) {
                ensureAuthoredWorlds(system, LAST_LIGHT_WORLDS);
                ensureScatteredSalvage(system, LAST_LIGHT_SALVAGE);
                ensureLastLightGuardianWreck(system);
            }
        }
    }

    /** Applies research-station orbits only after every anchor world exists. */
    private static void ensureResearchStationContent(
            StarSystemAPI expanse,
            List<StarSystemAPI> clusterSystems) {
        migrateResearchStationSalvageTracking(expanse, clusterSystems);
        ensureResearchStations(
                expanse,
                EXPANSE_RESEARCH_PREFIX,
                EXPANSE_RESEARCH_STATIONS);
        for (StarSystemAPI system : clusterSystems) {
            if (system == null) continue;
            String id = system.getOptionalUniqueId();
            if (SILENT_WAKE_ID.equals(id)) {
                ensureResearchStations(
                        system,
                        SILENT_WAKE_RESEARCH_PREFIX,
                        SILENT_WAKE_RESEARCH_STATIONS);
            } else if (ASHEN_VERGE_ID.equals(id)) {
                ensureResearchStations(
                        system,
                        ASHEN_VERGE_RESEARCH_PREFIX,
                        ASHEN_VERGE_RESEARCH_STATIONS);
            }
        }
    }

    private static void ensureAuthoredWorlds(
            StarSystemAPI system,
            WorldSpec[] specs) {
        if (system == null) return;
        SectorEntityToken focus = system.getStar();
        if (focus == null) focus = system.getCenter();
        if (focus == null) {
            system.initNonStarCenter();
            focus = system.getCenter();
        }

        String systemId = system.getOptionalUniqueId();
        for (WorldSpec spec : specs) {
            String id = systemId + "_world_" + spec.idSuffix;
            SectorEntityToken existing = system.getEntityById(id);
            PlanetAPI planet = existing instanceof PlanetAPI
                    ? (PlanetAPI) existing : null;
            if (planet == null) {
                planet = system.addPlanet(
                        id,
                        focus,
                        spec.name,
                        spec.type,
                        spec.angle,
                        spec.radius,
                        spec.distance,
                        spec.orbitDays);
                double radians = Math.toRadians(spec.angle);
                planet.setFixedLocation(
                        (float) Math.cos(radians) * spec.distance,
                        (float) Math.sin(radians) * spec.distance);
                planet.setSkipForJumpPointAutoGen(false);
            }
            planet.setName(spec.name);
            ensureRuins(planet, spec.ruins);
        }
    }

    private static void ensureSanzuRuins(StarSystemAPI system) {
        for (int index = 0; index < SILENT_WAKE_PLANET_IDS.length; index++) {
            SectorEntityToken entity = system.getEntityById(
                    SILENT_WAKE_PLANET_IDS[index]);
            if (entity instanceof PlanetAPI) {
                ensureRuins((PlanetAPI) entity, SANZU_RUINS[index]);
            }
        }
    }

    /** Adds one configured ruin tier without resetting survey/exploration state. */
    private static void ensureRuins(PlanetAPI planet, String ruins) {
        if (planet == null || ruins == null) return;
        MarketAPI market = planet.getMarket();
        if (market == null) {
            Misc.initConditionMarket(planet);
            market = planet.getMarket();
            if (market != null) {
                market.setSurveyLevel(MarketAPI.SurveyLevel.NONE);
            }
        }
        if (market == null || hasRuins(market)) return;
        market.addCondition(ruins);
    }

    private static boolean hasRuins(MarketAPI market) {
        return market.hasCondition(Conditions.RUINS_SCATTERED)
                || market.hasCondition(Conditions.RUINS_WIDESPREAD)
                || market.hasCondition(Conditions.RUINS_EXTENSIVE)
                || market.hasCondition(Conditions.RUINS_VAST);
    }

    /**
     * Salvage sites are generated once. Their system marker remains after
     * salvage so depleted habitats and mines do not reappear on later loads.
     */
    private static void ensureScatteredSalvage(
            StarSystemAPI system,
            SalvageSiteSpec[] specs) {
        if (system == null) return;

        boolean sitesGenerated = system.getMemoryWithoutUpdate().getBoolean(
                GENERATED_SCATTERED_SALVAGE);
        String systemId = system.getOptionalUniqueId();
        for (SalvageSiteSpec spec : specs) {
            String id = systemId + "_salvage_" + spec.idSuffix;
            SectorEntityToken site = system.getEntityById(id);
            if (site == null && !sitesGenerated) {
                site = BaseThemeGenerator.addSalvageEntity(
                        system, spec.entityType, Factions.NEUTRAL);
                site.setId(id);
                site.setName(spec.name);
                site.setDiscoverable(true);
            }
            if (site == null) continue;
            applyOrbit(system, site, spec.orbit);
        }
        system.getMemoryWithoutUpdate().set(
                GENERATED_SCATTERED_SALVAGE, true);
    }

    /** Adds Last Light's one-time Guardian wreck, unlocked by Ungaikyo's death. */
    private static void ensureLastLightGuardianWreck(StarSystemAPI system) {
        if (system == null || system.getMemoryWithoutUpdate().getBoolean(
                GENERATED_LAST_LIGHT_GUARDIAN_WRECK)) {
            return;
        }

        SectorEntityToken wreck = system.getEntityById(
                LAST_LIGHT_GUARDIAN_WRECK_ID);
        if (wreck == null) {
            PerShipData ship = new PerShipData(
                    LAST_LIGHT_GUARDIAN_VARIANT,
                    ShipCondition.BATTERED,
                    0f);
            ship.shipName = "Guardian";
            ship.nameAlwaysKnown = true;
            DerelictShipData params = new DerelictShipData(ship, false);
            wreck = BaseThemeGenerator.addSalvageEntity(
                    system, Entities.WRECK, Factions.NEUTRAL, params);
            if (wreck == null) return;
            wreck.setId(LAST_LIGHT_GUARDIAN_WRECK_ID);
            wreck.setName("Derelict Guardian Droneship");

            SalvageSpecialAssigner.ShipRecoverySpecialCreator creator =
                    new SalvageSpecialAssigner.ShipRecoverySpecialCreator(
                            new Random(0x4c61737447756172L),
                            0, 0, false, null, null);
            Misc.setSalvageSpecial(
                    wreck, creator.createSpecial(wreck, null));
        }

        wreck.setDiscoverable(true);
        applyOrbit(
                system,
                wreck,
                orbit(
                        LAST_LIGHT_ID + "_world_ember",
                        221f,
                        1050f,
                        96f,
                        221f));
        system.getMemoryWithoutUpdate().set(
                GENERATED_LAST_LIGHT_GUARDIAN_WRECK, true);
    }

    private static SalvageSiteSpec habitat(
            String idSuffix,
            String anchorId,
            float angle,
            float radius,
            float orbitDays,
            float facing) {
        return new SalvageSiteSpec(
                idSuffix,
                "Derelict Orbital Habitat",
                Entities.ORBITAL_HABITAT_REMNANT,
                orbit(anchorId, angle, radius, orbitDays, facing));
    }

    private static SalvageSiteSpec mining(
            String idSuffix,
            String anchorId,
            float angle,
            float radius,
            float orbitDays,
            float facing) {
        return new SalvageSiteSpec(
                idSuffix,
                "Derelict Mining Station",
                Entities.STATION_MINING_REMNANT,
                orbit(anchorId, angle, radius, orbitDays, facing));
    }

    private static OrbitPlacement orbit(
            String anchorId,
            float angle,
            float radius,
            float orbitDays,
            float facing) {
        return new OrbitPlacement(
                anchorId, angle, radius, orbitDays, facing);
    }

    private static void applyOrbit(
            StarSystemAPI system,
            SectorEntityToken entity,
            OrbitPlacement placement) {
        SectorEntityToken anchor = system.getEntityById(placement.anchorId);
        if (anchor == null) {
            Global.getLogger(OdysseyExpanseSystem.class).warn(
                    "[ODYSSEY_ORBIT] Missing anchor " + placement.anchorId
                    + " for " + entity.getId() + " in " + system.getName()
                    + "; falling back to the system primary.");
            anchor = system.getStar();
            if (anchor == null) anchor = system.getCenter();
        }
        if (anchor == null) return;
        entity.setCircularOrbit(
                anchor,
                placement.angle,
                placement.radius,
                placement.orbitDays);
        entity.setFacing(placement.facing);
    }

    private static final class WorldSpec {
        private final String idSuffix;
        private final String name;
        private final String type;
        private final float angle;
        private final float radius;
        private final float distance;
        private final float orbitDays;
        private final String ruins;

        private WorldSpec(
                String idSuffix,
                String name,
                String type,
                float angle,
                float radius,
                float distance,
                float orbitDays,
                String ruins) {
            this.idSuffix = idSuffix;
            this.name = name;
            this.type = type;
            this.angle = angle;
            this.radius = radius;
            this.distance = distance;
            this.orbitDays = orbitDays;
            this.ruins = ruins;
        }
    }

    private static final class SalvageSiteSpec {
        private final String idSuffix;
        private final String name;
        private final String entityType;
        private final OrbitPlacement orbit;

        private SalvageSiteSpec(
                String idSuffix,
                String name,
                String entityType,
                OrbitPlacement orbit) {
            this.idSuffix = idSuffix;
            this.name = name;
            this.entityType = entityType;
            this.orbit = orbit;
        }
    }

    private static final class OrbitPlacement {
        private final String anchorId;
        private final float angle;
        private final float radius;
        private final float orbitDays;
        private final float facing;

        private OrbitPlacement(
                String anchorId,
                float angle,
                float radius,
                float orbitDays,
                float facing) {
            this.anchorId = anchorId;
            this.angle = angle;
            this.radius = radius;
            this.orbitDays = orbitDays;
            this.facing = facing;
        }
    }

    /** The Starving Threat's centuries-old attempt to digest a Domain gate. */
    private static StarSystemAPI ensureDevouredReachSystem(float[] offset) {
        StarSystemAPI system = findSystem(DEVOURED_REACH_ID);
        if (system == null) {
            system = Global.getSector().createStarSystem("Devoured Reach");
        }
        if (system.getCenter() == null) system.initNonStarCenter();

        configureClusterSystem(
                system, DEVOURED_REACH_ID, "Devoured Reach", offset);
        system.setMapGridWidthOverride(24000f);
        system.setMapGridHeightOverride(22000f);
        system.setHasSystemwideNebula(false);
        system.setLightColor(new Color(92, 78, 110));

        SectorEntityToken ring = system.getEntityById(DEVOURED_RING_ID);
        if (ring != null && !isDevouredRing(ring)) {
            Global.getLogger(OdysseyExpanseSystem.class).error(
                    "Devoured Ring ID contains incompatible state; refusing "
                            + "to replace or rewrite it");
            return system;
        }
        if (ring == null) {
            ring = system.addCustomEntity(
                    DEVOURED_RING_ID,
                    "Devoured Ring",
                    DEVOURED_RING_TYPE,
                    Factions.NEUTRAL);
        }
        ring.setName("Devoured Ring");
        ring.setFixedLocation(0f, 0f);
        ring.setSensorProfile(1f);
        ring.setDiscoverable(false);
        ring.addTag(Tags.HAS_INTERACTION_DIALOG);
        ring.getDetectedRangeMod().modifyFlat(
                CLUSTER_ACCESS_DETECTED, SYSTEM_ACCESS_VISIBLE_RANGE);

        for (int index = 0; index < HOLLOW_WORLD_IDS.length; index++) {
            SectorEntityToken world = system.getEntityById(
                    HOLLOW_WORLD_IDS[index]);
            if (world != null && !world.getMemoryWithoutUpdate().getBoolean(
                    HOLLOW_WORLD_RENDER_V2)) {
                system.removeEntity(world);
                world = null;
            }
            if (world == null) {
                world = system.addCustomEntity(
                        HOLLOW_WORLD_IDS[index],
                        HOLLOW_WORLD_NAMES[index],
                        HOLLOW_WORLD_TYPES[index],
                        Factions.NEUTRAL);
            }
            world.setName(HOLLOW_WORLD_NAMES[index]);
            world.setFixedLocation(
                    HOLLOW_WORLD_LOCATIONS[index][0],
                    HOLLOW_WORLD_LOCATIONS[index][1]);
            world.setFacing(40f + index * 113f);
            world.getMemoryWithoutUpdate().set(HOLLOW_WORLD_RENDER_V2, true);
            makePermanentMapLandmark(world);
        }
        return system;
    }

    /**
     * A fixed north-to-south nebula current. The renderer bakes its fluid
     * streamlines around the same four circular obstacles configured here.
     */
    private static StarSystemAPI ensureSilentWakeSystem(float[] offset) {
        StarSystemAPI system = findSystem(SILENT_WAKE_ID);
        if (system == null) {
            system = Global.getSector().createStarSystem("Sanzu");
        }

        configureClusterSystem(
                system, SILENT_WAKE_ID, "Sanzu", offset);
        system.setMapGridWidthOverride(24000f);
        system.setMapGridHeightOverride(21000f);
        system.setHasSystemwideNebula(false);
        system.setLightColor(new Color(148, 179, 220));
        ensureWhiteDwarfStar(system, SILENT_WAKE_ID, "Sanzu");

        for (int index = 0; index < SILENT_WAKE_PLANET_IDS.length; index++) {
            float[] obstacle = SILENT_WAKE_OBSTACLES[index + 1];
            PlanetAPI planet = null;
            SectorEntityToken existing = system.getEntityById(
                    SILENT_WAKE_PLANET_IDS[index]);
            if (existing instanceof PlanetAPI) planet = (PlanetAPI) existing;
            if (planet == null) {
                planet = system.addPlanet(
                        SILENT_WAKE_PLANET_IDS[index],
                        system.getStar(),
                        SILENT_WAKE_PLANET_NAMES[index],
                        SILENT_WAKE_PLANET_TYPES[index],
                        index * 113f,
                        obstacle[2] - 110f,
                        3000f + index * 1000f,
                        120f + index * 40f);
            }
            planet.setName(SILENT_WAKE_PLANET_NAMES[index]);
            planet.setRadius(obstacle[2] - 110f);
            planet.setFixedLocation(obstacle[0], obstacle[1]);
            planet.setSkipForJumpPointAutoGen(false);
        }

        ensureSanzuWakeDerelicts(system);

        ensureEnvironmentAnchor(
                system, SILENT_WAKE_FLOW_ID, SILENT_WAKE_FLOW_TYPE);
        String currentId = chiefnavigator.campaign.SilentWakeCurrentTerrain.ID;
        if (system.getEntityById(currentId) == null) {
            SectorEntityToken current = system.addTerrain(currentId, null);
            current.setId(currentId);
            current.setFixedLocation(0f, 0f);
        }
        system.getMemoryWithoutUpdate().set(GENERATED_SILENT_WAKE, true);
        return system;
    }

    /**
     * Seeds two persistent, recoverable wrecks into the downstream wake of
     * every planet. Missing wrecks do not respawn after the generation marker
     * is set, so salvaging the field remains permanent.
     */
    private static void ensureSanzuWakeDerelicts(StarSystemAPI system) {
        if (system.getMemoryWithoutUpdate().getBoolean(
                GENERATED_SANZU_WAKE_DERELICTS)) {
            return;
        }

        String campaignSeed = Global.getSector().getSeedString();
        long baseSeed = 0x53616e7a75L;
        if (campaignSeed != null) {
            baseSeed = baseSeed * 31L + campaignSeed.hashCode();
        }
        boolean complete = true;

        for (int planetIndex = 0;
                planetIndex < SILENT_WAKE_PLANET_IDS.length;
                planetIndex++) {
            float[] obstacle = SILENT_WAKE_OBSTACLES[planetIndex + 1];
            for (int wreckIndex = 0;
                    wreckIndex < SANZU_DERELICTS_PER_PLANET;
                    wreckIndex++) {
                String id = SANZU_WAKE_DERELICT_PREFIX
                        + planetIndex + "_" + wreckIndex;
                if (system.getEntityById(id) != null) continue;

                Random random = new Random(baseSeed
                        + planetIndex * 104729L
                        + wreckIndex * 15485863L);
                float downstream = obstacle[2]
                        * (1.8f + wreckIndex * 1.05f
                                + random.nextFloat() * 0.35f);
                float lateral = obstacle[2]
                        * (random.nextFloat() - 0.5f) * 1.05f;
                float facing = random.nextFloat() * 360f;

                DerelictShipData data = pickSanzuWakeDerelict(random);
                if (data == null) {
                    complete = false;
                    continue;
                }

                SectorEntityToken wreck = BaseThemeGenerator.addSalvageEntity(
                        system,
                        Entities.WRECK,
                        Factions.NEUTRAL,
                        data);
                wreck.setId(id);
                wreck.setFixedLocation(
                        obstacle[0] + lateral,
                        obstacle[1] - downstream);
                wreck.setFacing(facing);
                wreck.setDiscoverable(true);

                SalvageSpecialAssigner.ShipRecoverySpecialCreator creator =
                        new SalvageSpecialAssigner.ShipRecoverySpecialCreator(
                                random, 0, 0, false, null, null);
                Misc.setSalvageSpecial(
                        wreck, creator.createSpecial(wreck, null));
            }
        }

        if (complete) {
            system.getMemoryWithoutUpdate().set(
                    GENERATED_SANZU_WAKE_DERELICTS, true);
        }
    }

    /** Selects ordinary faction hulls and rejects every capital result. */
    private static DerelictShipData pickSanzuWakeDerelict(Random random) {
        DerelictType[] types = {
            DerelictType.SMALL,
            DerelictType.MEDIUM,
            DerelictType.LARGE
        };
        for (int attempt = 0; attempt < 64; attempt++) {
            String factionId = SANZU_DERELICT_FACTIONS[
                    random.nextInt(SANZU_DERELICT_FACTIONS.length)];
            DerelictType type = types[random.nextInt(types.length)];
            DerelictShipData data = DerelictShipEntityPlugin.createRandom(
                    factionId, type, random, 0f);
            if (data == null || data.ship == null) continue;
            ShipVariantAPI variant = data.ship.getVariant();
            if (variant == null || variant.isFighter() || variant.isStation()) {
                continue;
            }
            HullSize size = variant.getHullSpec().getHullSize();
            if (size != HullSize.FRIGATE
                    && size != HullSize.DESTROYER
                    && size != HullSize.CRUISER) {
                continue;
            }
            data.ship.condition = DerelictShipEntityPlugin
                    .pickBadCondition(random);
            return data;
        }
        return null;
    }

    /** Warm, nebula-backed stellar system containing FOB Ithaca. */
    private static StarSystemAPI ensureAshenVergeSystem(float[] offset) {
        StarSystemAPI system = findSystem(ASHEN_VERGE_ID);
        if (system == null) {
            system = Global.getSector().createStarSystem("Ashen Verge");
        }

        configureClusterSystem(
                system, ASHEN_VERGE_ID, "Ashen Verge", offset);
        system.setMapGridWidthOverride(24000f);
        system.setMapGridHeightOverride(19000f);
        system.setHasSystemwideNebula(true);
        system.setBackgroundTextureFilename(ASHEN_VERGE_BACKGROUND);
        system.setLightColor(new Color(255, 214, 176));
        ensureAshenVergeSun(system, ASHEN_VERGE_ID, "Ashen Verge");
        removeEntityById(system, LEGACY_ASHEN_VERGE_CLOUD_ID);
        ensureAshenVergeNebula(system);
        ensureFobIthaca(system);
        system.getMemoryWithoutUpdate().set(GENERATED_ASHEN_VERGE, true);
        return system;
    }

    /** Reconciles every unsalvaged authored research station independently. */
    private static void ensureResearchStations(
            StarSystemAPI system,
            String idPrefix,
            OrbitPlacement[] placements) {
        if (system == null) return;

        for (int index = 0; index < placements.length; index++) {
            String id = idPrefix + index;
            String componentId = getResearchComponentId(idPrefix, index);
            String provisionedKey = RESEARCH_COMPONENT_PROVISIONED_PREFIX + id;
            if (isResearchStationSalvaged(id)) {
                SectorEntityToken stale = system.getEntityById(id);
                if (isOwnedResearchStation(stale)) {
                    Misc.makeUnimportant(
                            stale, RESEARCH_STATION_MAP_IMPORTANT);
                    system.removeEntity(stale);
                }
                continue;
            }
            boolean componentProvisioned = Global.getSector()
                    .getMemoryWithoutUpdate().getBoolean(provisionedKey);
            SectorEntityToken station = system.getEntityById(id);
            if (station == null) {
                station = BaseThemeGenerator.addSalvageEntity(
                        system,
                        Entities.STATION_RESEARCH_REMNANT,
                        Factions.NEUTRAL);
                station.setId(id);
                station.setName("Derelict Research Station");
                station.getMemoryWithoutUpdate().set(
                        RESEARCH_STATION_MARKER, true);
            }
            if (!isOwnedResearchStation(station)) {
                if (station != null) {
                    Global.getLogger(OdysseyExpanseSystem.class).error(
                            "Research station ID " + id
                                    + " is owned by incompatible serialized "
                                    + "state; refusing mutation");
                }
                continue;
            }
            station.setName("Derelict Research Station");
            applyOrbit(system, station, placements[index]);
            markResearchStationOnSystemMap(station);
            stripResearchStationAICoreLoot(station);

            if (!componentProvisioned) {
                CargoAPI componentLoot = Global.getFactory().createCargo(true);
                SpecialItemData component = new SpecialItemData(
                        componentId, null);
                if (BaseSalvageSpecial.getCombinedExtraSalvage(station)
                        .getQuantity(CargoAPI.CargoItemType.SPECIAL, component)
                        < 1f) {
                    componentLoot.addSpecial(component, 1f);
                    BaseSalvageSpecial.addExtraSalvage(
                            station, componentLoot);
                }
                Global.getSector().getMemoryWithoutUpdate().set(
                        provisionedKey, true);
            }
        }
        system.getMemoryWithoutUpdate().set(
                GENERATED_RESEARCH_STATIONS, true);
        system.getMemoryWithoutUpdate().set(
                GENERATED_RESEARCH_CORE_LOOT, true);
    }

    /** Keeps every live research POI named and visible on the Tab map. */
    private static void markResearchStationOnSystemMap(
            SectorEntityToken station) {
        makePermanentMapLandmark(station);
        station.setSensorProfile(1f);
        station.setDiscoverable(false);
        station.getDetectedRangeMod().modifyFlat(
                RESEARCH_STATION_MAP_IMPORTANT, 100000f);
        // Visibility is permanent, but the exclamation mark belongs only to
        // the active research Labor and is managed by MenelausTrial. Clear
        // this older unconditional reason as an in-place save migration.
        Misc.makeUnimportant(station, RESEARCH_STATION_MAP_IMPORTANT);
    }

    /**
     * Converts the old "missing token means salvaged" convention into an
     * explicit per-station record. A missing legacy station still awards its
     * component, preserving completed work in old saves.
     */
    private static void migrateResearchStationSalvageTracking(
            StarSystemAPI expanse,
            List<StarSystemAPI> clusterSystems) {
        if (Global.getSector().getMemoryWithoutUpdate().getBoolean(
                RESEARCH_SALVAGE_TRACKING_MIGRATED)) {
            return;
        }
        migrateResearchStationSalvageTracking(
                expanse,
                EXPANSE_RESEARCH_PREFIX,
                EXPANSE_RESEARCH_STATIONS.length);
        for (StarSystemAPI system : clusterSystems) {
            if (system == null) continue;
            String systemId = system.getOptionalUniqueId();
            if (SILENT_WAKE_ID.equals(systemId)) {
                migrateResearchStationSalvageTracking(
                        system,
                        SILENT_WAKE_RESEARCH_PREFIX,
                        SILENT_WAKE_RESEARCH_STATIONS.length);
            } else if (ASHEN_VERGE_ID.equals(systemId)) {
                migrateResearchStationSalvageTracking(
                        system,
                        ASHEN_VERGE_RESEARCH_PREFIX,
                        ASHEN_VERGE_RESEARCH_STATIONS.length);
            }
        }
        Global.getSector().getMemoryWithoutUpdate().set(
                RESEARCH_SALVAGE_TRACKING_MIGRATED, true);
    }

    private static void migrateResearchStationSalvageTracking(
            StarSystemAPI system, String idPrefix, int total) {
        if (system == null || !system.getMemoryWithoutUpdate().getBoolean(
                GENERATED_RESEARCH_STATIONS)) {
            return;
        }
        for (int index = 0; index < total; index++) {
            String id = idPrefix + index;
            if (system.getEntityById(id) != null) continue;
            Global.getSector().getMemoryWithoutUpdate().set(
                    RESEARCH_STATION_SALVAGED_PREFIX + id, true);
            String provisionedKey = RESEARCH_COMPONENT_PROVISIONED_PREFIX + id;
            if (!Global.getSector().getMemoryWithoutUpdate().getBoolean(
                    provisionedKey)) {
                grantMigratedResearchComponent(
                        id,
                        getResearchComponentId(idPrefix, index),
                        provisionedKey);
            }
        }
    }

    /** Records vanilla salvage before its one-second fade removes the token. */
    public static void markResearchStationSalvaged(
            SectorEntityToken station) {
        if (station == null || Global.getSector() == null) return;
        String id = station.getId();
        if (!isOwnedResearchStation(station)) return;
        Global.getSector().getMemoryWithoutUpdate().set(
                RESEARCH_STATION_SALVAGED_PREFIX + id, true);
        Misc.makeUnimportant(station, RESEARCH_STATION_MAP_IMPORTANT);
    }

    private static boolean isResearchStationId(String id) {
        return isResearchStationId(
                        id,
                        EXPANSE_RESEARCH_PREFIX,
                        EXPANSE_RESEARCH_STATIONS.length)
                || isResearchStationId(
                        id,
                        SILENT_WAKE_RESEARCH_PREFIX,
                        SILENT_WAKE_RESEARCH_STATIONS.length)
                || isResearchStationId(
                        id,
                        ASHEN_VERGE_RESEARCH_PREFIX,
                        ASHEN_VERGE_RESEARCH_STATIONS.length);
    }

    private static boolean isResearchStationId(
            String id, String prefix, int total) {
        if (id == null) return false;
        for (int index = 0; index < total; index++) {
            if ((prefix + index).equals(id)) return true;
        }
        return false;
    }

    /** True only for one of the six authored Odyssey research stations. */
    public static boolean isOwnedResearchStation(SectorEntityToken station) {
        return station != null && isResearchStationId(station.getId())
                && Entities.STATION_RESEARCH_REMNANT.equals(
                        station.getCustomEntityType());
    }

    private static boolean isResearchStationSalvaged(String id) {
        return Global.getSector() != null
                && Global.getSector().getMemoryWithoutUpdate().getBoolean(
                        RESEARCH_STATION_SALVAGED_PREFIX + id);
    }

    /**
     * Research stations in the Knot replace every AI-core roll with their
     * deterministic Ithaca component, including surviving stations in old
     * saves that still carry the former guaranteed combat core.
     */
    private static void stripResearchStationAICoreLoot(
            SectorEntityToken station) {
        if (station == null) return;
        removeAICoreDropGroups(station.getDropRandom());
        removeAICoreDropGroups(station.getDropValue());
        BaseSalvageSpecial.ExtraSalvage permanent =
                BaseSalvageSpecial.getExtraSalvage(station);
        BaseSalvageSpecial.ExtraSalvage temporary =
                BaseSalvageSpecial.getTempExtraSalvage(station);
        stripAICoreCargo(permanent == null ? null : permanent.cargo);
        stripAICoreCargo(temporary == null ? null : temporary.cargo);
    }

    private static void removeAICoreDropGroups(List<DropData> drops) {
        if (drops == null) return;
        drops.removeIf(drop -> drop != null
                && drop.group != null
                && drop.group.startsWith("ai_cores"));
    }

    private static void stripAICoreCargo(CargoAPI cargo) {
        if (cargo == null) return;
        removeCommodity(cargo, "alpha_core");
        removeCommodity(cargo, "beta_core");
        removeCommodity(cargo, "gamma_core");
        removeCommodity(cargo, LEGACY_ODYSSEY_COMBAT_CORE_ID);
    }

    private static void removeCommodity(CargoAPI cargo, String commodityId) {
        float quantity = cargo.getCommodityQuantity(commodityId);
        if (quantity > 0f) cargo.removeCommodity(commodityId, quantity);
    }

    private static String getResearchComponentId(
            String idPrefix, int index) {
        int offset;
        if (EXPANSE_RESEARCH_PREFIX.equals(idPrefix)) {
            offset = 0;
        } else if (SILENT_WAKE_RESEARCH_PREFIX.equals(idPrefix)) {
            offset = EXPANSE_RESEARCH_STATIONS.length;
        } else if (ASHEN_VERGE_RESEARCH_PREFIX.equals(idPrefix)) {
            offset = EXPANSE_RESEARCH_STATIONS.length
                    + SILENT_WAKE_RESEARCH_STATIONS.length;
        } else {
            throw new IllegalArgumentException(
                    "Unknown research-station prefix: " + idPrefix);
        }
        return IthacaResearchUpgrades.COMPONENT_IDS[offset + index];
    }

    /** Gives pre-component saves the item their already-salvaged POI lacked. */
    private static void grantMigratedResearchComponent(
            String stationId,
            String componentId,
            String provisionedKey) {
        if (Global.getSector().getPlayerFleet() == null) return;
        CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
        SpecialItemData component = new SpecialItemData(componentId, null);
        if (cargo.getQuantity(CargoAPI.CargoItemType.SPECIAL, component) < 1f) {
            cargo.addSpecial(component, 1f);
        }
        Global.getSector().getMemoryWithoutUpdate().set(provisionedKey, true);
        Global.getLogger(OdysseyExpanseSystem.class).info(
                "Granted migrated Ithaca research component " + componentId
                + " for previously salvaged station " + stationId);
    }

    /** Total research-station POIs authored across the Odyssey Sector. */
    public static int getResearchStationTotal() {
        return EXPANSE_RESEARCH_STATIONS.length
                + SILENT_WAKE_RESEARCH_STATIONS.length
                + ASHEN_VERGE_RESEARCH_STATIONS.length;
    }

    /**
     * Counts stations the player has actually researched from durable
     * per-station salvage records, never from entity absence alone.
     */
    public static int getResearchedStationCount() {
        return countResearchedStations(
                        findSystemById(SYSTEM_ID),
                        EXPANSE_RESEARCH_PREFIX,
                        EXPANSE_RESEARCH_STATIONS.length)
                + countResearchedStations(
                        findSystemById(SILENT_WAKE_ID),
                        SILENT_WAKE_RESEARCH_PREFIX,
                        SILENT_WAKE_RESEARCH_STATIONS.length)
                + countResearchedStations(
                        findSystemById(ASHEN_VERGE_ID),
                        ASHEN_VERGE_RESEARCH_PREFIX,
                        ASHEN_VERGE_RESEARCH_STATIONS.length);
    }

    /** Remaining station tokens, permanently marked on their system maps. */
    public static List<SectorEntityToken> getRemainingResearchStations() {
        List<SectorEntityToken> result = new ArrayList<SectorEntityToken>();
        addResearchStations(
                result,
                findSystemById(SYSTEM_ID),
                EXPANSE_RESEARCH_PREFIX,
                EXPANSE_RESEARCH_STATIONS.length);
        addResearchStations(
                result,
                findSystemById(SILENT_WAKE_ID),
                SILENT_WAKE_RESEARCH_PREFIX,
                SILENT_WAKE_RESEARCH_STATIONS.length);
        addResearchStations(
                result,
                findSystemById(ASHEN_VERGE_ID),
                ASHEN_VERGE_RESEARCH_PREFIX,
                ASHEN_VERGE_RESEARCH_STATIONS.length);
        return result;
    }

    private static void addResearchStations(
            List<SectorEntityToken> result,
            StarSystemAPI system,
            String idPrefix,
            int total) {
        if (system == null) return;
        for (int index = 0; index < total; index++) {
            String id = idPrefix + index;
            if (isResearchStationSalvaged(id)) continue;
            SectorEntityToken station = system.getEntityById(id);
            if (isOwnedResearchStation(station)) result.add(station);
        }
    }

    private static int countResearchedStations(
            StarSystemAPI system, String idPrefix, int total) {
        if (system == null) return 0;
        int researched = 0;
        for (int index = 0; index < total; index++) {
            if (isResearchStationSalvaged(idPrefix + index)) researched++;
        }
        return researched;
    }

    private static void ensureFobIthaca(StarSystemAPI system) {
        SectorEntityToken ithaca = system.getEntityById(FOB_ITHACA_ID);
        if (ithaca != null && !isFobIthaca(ithaca)) {
            Global.getLogger(OdysseyExpanseSystem.class).error(
                    "FOB Ithaca ID contains incompatible state; refusing "
                            + "to replace or rewrite it");
            return;
        }
        if (ithaca == null) {
            ithaca = system.addCustomEntity(
                    FOB_ITHACA_ID,
                    "FOB Ithaca",
                    FOB_ITHACA_TYPE,
                    Factions.PLAYER);
            ithaca.getMemoryWithoutUpdate().set(
                    FOB_ITHACA_OWNER_MARKER, true);
        }
        // Ithaca and its three physical bastions are friendly campaign
        // infrastructure. Reassert this for saves created while the FOB was
        // neutral/hostile so nearby battles put it on the player's side.
        ithaca.setFaction(Factions.PLAYER);
        // Close enough that the complete installation enters the default
        // arrival view, without overlapping the Ashen Verge primary.
        ithaca.setFixedLocation(4500f, 0f);
        ithaca.setFacing(0f);
        // The central body and three colocated bastion tokens are interactive.
        // Invisible collision beads follow the surviving ring metal while
        // leaving the broken western opening as the sole route into
        // Menelaus's interior courtyard.
        // Planet-style visibility: planets have no sensor profile, so the
        // campaign engine renders them as known scenery instead of first
        // turning the entire body into a fading sensor contact. Ithaca is a
        // planet-scale fixed landmark and must use the same path.
        ithaca.setSensorProfile(null);
        ithaca.setDiscoverable(null);
        // Custom-entity tags are serialized into saves. Builds from before
        // the bastion encounters marked Ithaca non-clickable, so changing
        // custom_entities.json alone does not repair an existing campaign.
        // Reassert the live interaction tags every time the system is
        // ensured, allowing all three section fights at the visible FOB.
        ithaca.removeTag(Tags.NON_CLICKABLE);
        ithaca.addTag(Tags.STATION);
        ithaca.addTag(Tags.HAS_INTERACTION_DIALOG);
        // Remove the obsolete contact-range overrides from existing saves.
        // They are not merely unnecessary without a sensor profile: leaving
        // them in place risks sending the custom entity back through sensor
        // contact fading in a later engine update.
        ithaca.getDetectedRangeMod().unmodify(CLUSTER_ACCESS_DETECTED);
        ithaca.setExtendedDetectedAtRange(null);
        Misc.makeImportant(ithaca, FOB_ITHACA_IMPORTANT);
        ensureFobIthacaServices(ithaca);
        IthacaSectionEncounter.ensureSectionStations(ithaca);
        ensureFobIthacaRingCollision(system, ithaca);
        ensureFobIthacaGate(system, ithaca);
        ensureFobIthacaGateDefenseStation(system, ithaca);
    }

    /**
     * Approximates the visible 320-degree ring with overlapping circular
     * campaign blockers. Starsector has no concave campaign collision mesh,
     * so the damaged western gap is represented by the deliberately omitted
     * 40-degree section rather than by one impassable center-sized circle.
     */
    private static void ensureFobIthacaRingCollision(
            StarSystemAPI system, SectorEntityToken ithaca) {
        float step = (FOB_ITHACA_RING_ARC_END
                - FOB_ITHACA_RING_ARC_START)
                / (FOB_ITHACA_RING_COLLISION_COUNT - 1f);
        for (int index = 0;
                index < FOB_ITHACA_RING_COLLISION_COUNT;
                index++) {
            String id = FOB_ITHACA_RING_COLLISION_PREFIX + index;
            SectorEntityToken existing = system.getEntityById(id);
            CustomCampaignEntityAPI blocker =
                    existing instanceof CustomCampaignEntityAPI
                    ? (CustomCampaignEntityAPI) existing : null;
            if (existing != null && (blocker == null
                    || !FOB_ITHACA_RING_COLLISION_TYPE.equals(
                            blocker.getCustomEntityType())
                    || !existing.getMemoryWithoutUpdate().getBoolean(
                            FOB_ITHACA_RING_COLLISION_MARKER))) {
                continue;
            }
            if (blocker == null) {
                blocker = system.addCustomEntity(
                        id,
                        "",
                        FOB_ITHACA_RING_COLLISION_TYPE,
                        Factions.PLAYER);
                blocker.getMemoryWithoutUpdate().set(
                        FOB_ITHACA_RING_COLLISION_MARKER, true);
            }

            float angle = FOB_ITHACA_RING_ARC_START + step * index;
            double radians = Math.toRadians(angle);
            blocker.setRadius(FOB_ITHACA_RING_COLLISION_RADIUS);
            blocker.setFixedLocation(
                    ithaca.getLocation().x
                            + (float) Math.cos(radians)
                                    * FOB_ITHACA_RING_CENTERLINE_RADIUS,
                    ithaca.getLocation().y
                            + (float) Math.sin(radians)
                                    * FOB_ITHACA_RING_CENTERLINE_RADIUS);
            suppressFobIthacaRingCollisionContact(blocker);
        }
    }

    /**
     * Removes sensor-contact behavior from every owned ring collision bead.
     * A zero sensor profile is still a close-range contact in Starsector and
     * repeatedly produces detection pings as the player crosses the ring.
     */
    static void suppressFobIthacaRingCollisionContacts(
            StarSystemAPI system) {
        if (system == null) return;
        for (int index = 0;
                index < FOB_ITHACA_RING_COLLISION_COUNT;
                index++) {
            SectorEntityToken entity = system.getEntityById(
                    FOB_ITHACA_RING_COLLISION_PREFIX + index);
            if (!(entity instanceof CustomCampaignEntityAPI)
                    || !FOB_ITHACA_RING_COLLISION_TYPE.equals(
                            ((CustomCampaignEntityAPI) entity)
                                    .getCustomEntityType())
                    || !entity.getMemoryWithoutUpdate().getBoolean(
                            FOB_ITHACA_RING_COLLISION_MARKER)) {
                continue;
            }
            suppressFobIthacaRingCollisionContact(entity);
        }
    }

    private static void suppressFobIthacaRingCollisionContact(
            SectorEntityToken blocker) {
        blocker.setSensorProfile(null);
        blocker.setDiscoverable(null);
        blocker.getDetectedRangeMod().unmodify(CLUSTER_ACCESS_DETECTED);
        blocker.setExtendedDetectedAtRange(null);
        blocker.addTag(Tags.NON_CLICKABLE);
    }

    /**
     * Keeps the forbidden Janus gate and its visible Spartan cordon inside
     * Ithaca's western interior. Exact ids and type repair older saves and
     * ensure the vanilla inactive-gate visual never becomes scannable.
     */
    private static void ensureFobIthacaGate(
            StarSystemAPI system, SectorEntityToken ithaca) {
        SectorEntityToken gate = system.getEntityById(FOB_ITHACA_GATE_ID);
        if (gate != null && !isFobIthacaGate(gate)) {
            Global.getLogger(OdysseyExpanseSystem.class).error(
                    "FOB Ithaca gate ID contains incompatible state; refusing "
                            + "to replace or rewrite it");
            return;
        }
        if (gate == null) {
            gate = system.addCustomEntity(
                    FOB_ITHACA_GATE_ID,
                    "Ithaca Janus Gate",
                    Entities.INACTIVE_GATE,
                    Factions.NEUTRAL);
            gate.getMemoryWithoutUpdate().set(FOB_ITHACA_GATE_MARKER, true);
        }

        gate.setFixedLocation(
                ithaca.getLocation().x - 1050f,
                ithaca.getLocation().y);
        gate.setFacing(0f);
        gate.setSensorProfile(1f);
        gate.setDiscoverable(false);
        gate.removeTag(Tags.NON_CLICKABLE);
        gate.addTag(Tags.HAS_INTERACTION_DIALOG);
        gate.getDetectedRangeMod().modifyFlat(
                CLUSTER_ACCESS_DETECTED,
                SYSTEM_ACCESS_VISIBLE_RANGE);
        Misc.makeImportant(gate, FOB_ITHACA_GATE_IMPORTANT);

        ensureFobIthacaSpartanGuard(system, gate);
        ensureFobIthacaSpartanMouthGuards(system, ithaca);
    }

    /**
     * Maintains the visible, fightable battlestation centered in Ithaca's
     * forty-degree western breach. Unlike the three ring bastions, this fleet
     * is deliberately rendered on the campaign map: it is the final Labor's
     * exact assault target and the rally point for Task Force Spartan.
     */
    static CampaignFleetAPI ensureFobIthacaGateDefenseStation(
            StarSystemAPI system) {
        if (system == null) return null;
        SectorEntityToken ithaca = system.getEntityById(FOB_ITHACA_ID);
        return ensureFobIthacaGateDefenseStation(system, ithaca);
    }

    private static CampaignFleetAPI ensureFobIthacaGateDefenseStation(
            StarSystemAPI system, SectorEntityToken ithaca) {
        if (system == null || !isFobIthaca(ithaca)) return null;

        SectorEntityToken existing = system.getEntityById(
                FOB_ITHACA_GATE_DEFENSE_STATION_ID);
        CampaignFleetAPI station = existing instanceof CampaignFleetAPI
                ? (CampaignFleetAPI) existing : null;
        boolean created = false;
        if (station != null
                && TroyArrivalScript.isFleetBusyForMutation(station)) {
            return null;
        }
        if (existing != null && !isValidFobIthacaGateDefenseStation(station)) {
            // Native defeat can leave the exact owned station token empty
            // before it despawns. Restore its core in place, never replace a
            // foreign/player/non-station object or a live battle participant.
            if (station != null && station.isStationMode()
                    && !isPlayerControlledFleet(station) && station.isEmpty()
                    && !TroyArrivalScript.isFleetBusyForMutation(station)
                    && !MenelausTrial.isFinalLaborFailed()
                    && !OdysseyPredatorScript.isBreachDefenseAssaultCommitted()) {
                addFobIthacaBreachDefenseCore(station);
                created = true;
            } else {
                if (!system.getMemoryWithoutUpdate().getBoolean(
                        FOB_ITHACA_GATE_DEFENSE_INVALID_V1)) {
                    Global.getLogger(OdysseyExpanseSystem.class).error(
                            "FOB Ithaca gate-defense ID contains incompatible "
                                    + "serialized state; refusing replacement");
                    system.getMemoryWithoutUpdate().set(
                            FOB_ITHACA_GATE_DEFENSE_INVALID_V1, true);
                }
                return null;
            }
        }
        if (station == null) {
            // This specific defender must survive ambient attrition. Never
            // rebuild it during the real breach assault or after Ithaca falls.
            if (MenelausTrial.isFinalLaborFailed()
                    || OdysseyPredatorScript
                            .isBreachDefenseAssaultCommitted()) {
                return null;
            }
            station = Global.getFactory().createEmptyFleet(
                    TaskForceSpartanFaction.ID,
                    FOB_ITHACA_BREACH_DEFENSE_NAME,
                    true);
            station.setId(FOB_ITHACA_GATE_DEFENSE_STATION_ID);
            addFobIthacaBreachDefenseCore(station);
            system.addEntity(station);
            system.getMemoryWithoutUpdate().set(
                    FOB_ITHACA_GATE_DEFENSE_CREATED_V1, true);
            created = true;
        } else {
            system.getMemoryWithoutUpdate().set(
                    FOB_ITHACA_GATE_DEFENSE_CREATED_V1, true);
        }

        station.setName(FOB_ITHACA_BREACH_DEFENSE_NAME);
        station.setFaction(TaskForceSpartanFaction.ID, true);
        station.setNoFactionInName(true);
        station.setTransponderOn(true);
        station.setStationMode(true);
        station.setNoAutoDespawn(true);
        station.setAbortDespawn(true);
        station.setAI(null);
        station.setDoNotAdvanceAI(true);
        station.clearAssignments();
        station.setVelocity(0f, 0f);
        station.setFixedLocation(
                ithaca.getLocation().x + FOB_ITHACA_GATE_DEFENSE_OFFSET_X,
                ithaca.getLocation().y + FOB_ITHACA_GATE_DEFENSE_OFFSET_Y);
        station.setFacing(0f);
        station.setDiscoverable(false);
        station.setInteractionTarget(station);
        station.removeTag(Tags.NON_CLICKABLE);
        station.addTag(Tags.STATION);
        station.getMemoryWithoutUpdate().set(MemFlags.STATION_FLEET, true);
        station.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        station.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
        station.getMemoryWithoutUpdate().unset(
                MemFlags.FLEET_IGNORES_OTHER_FLEETS);
        if (!OdysseyPredatorScript.isBreachDefenseAssaultCommitted()) {
            station.getMemoryWithoutUpdate().set(
                    MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS, true);
        }
        if (created) {
            for (FleetMemberAPI member
                    : station.getFleetData().getMembersListCopy()) {
                member.getStatus().repairFully();
                member.getStatus().resetAmmoState();
                member.getRepairTracker().setCR(
                        member.getRepairTracker().getMaxCR());
            }
        }
        station.forceSync();
        return station;
    }

    private static void addFobIthacaBreachDefenseCore(CampaignFleetAPI station) {
        FleetMemberAPI core = Global.getFactory().createFleetMember(
                FleetMemberType.SHIP, FOB_ITHACA_GATE_DEFENSE_VARIANT_ID);
        station.getFleetData().addFleetMember(core);
        station.getFleetData().setFlagship(core);
        station.getFleetData().setSyncNeeded();
        station.getFleetData().syncIfNeeded();
    }

    private static boolean isValidFobIthacaGateDefenseStation(
            CampaignFleetAPI station) {
        if (station == null || isPlayerControlledFleet(station)
                || !station.isStationMode()) return false;
        List<FleetMemberAPI> members = station.getFleetData()
                .getMembersListCopy();
        return members.size() == 1
                && members.get(0).getVariant() != null
                && FOB_ITHACA_GATE_DEFENSE_VARIANT_ID.equals(
                        members.get(0).getVariant().getHullVariantId())
                && members.get(0).getHullSpec() != null
                && FOB_ITHACA_GATE_DEFENSE_HULL_ID.equals(
                        members.get(0).getHullSpec().getHullId());
    }

    /** True only for the authored, non-player Ithaca breach station. */
    static boolean isOwnedFobIthacaGateDefenseStation(
            SectorEntityToken entity) {
        return entity instanceof CampaignFleetAPI
                && FOB_ITHACA_GATE_DEFENSE_STATION_ID.equals(entity.getId())
                && isValidFobIthacaGateDefenseStation(
                        (CampaignFleetAPI) entity);
    }

    private static boolean isPlayerControlledFleet(CampaignFleetAPI fleet) {
        return fleet != null && (fleet.isPlayerFleet()
                || Global.getSector() != null
                        && fleet == Global.getSector().getPlayerFleet());
    }

    /** Re-pins the breach station after collision or hostile contact. */
    static void anchorFobIthacaGateDefenseStation(
            StarSystemAPI system, SectorEntityToken ithaca) {
        if (system == null || !isFobIthaca(ithaca)) return;
        SectorEntityToken entity = system.getEntityById(
                FOB_ITHACA_GATE_DEFENSE_STATION_ID);
        if (!(entity instanceof CampaignFleetAPI)) return;
        CampaignFleetAPI station = (CampaignFleetAPI) entity;
        if (!isValidFobIthacaGateDefenseStation(station)
                || TroyArrivalScript.isFleetBusyForMutation(station)
                || station.isEmpty()) return;
        station.setStationMode(true);
        station.setAI(null);
        station.setDoNotAdvanceAI(true);
        station.setVelocity(0f, 0f);
        station.setLocation(
                ithaca.getLocation().x + FOB_ITHACA_GATE_DEFENSE_OFFSET_X,
                ithaca.getLocation().y + FOB_ITHACA_GATE_DEFENSE_OFFSET_Y);
        station.setFacing(0f);
    }

    /** Maintains all three 240-DP Spartan fleets for Threat targeting. */
    static List<CampaignFleetAPI> ensureFobIthacaSpartanGuards(
            StarSystemAPI system) {
        List<CampaignFleetAPI> result = new ArrayList<CampaignFleetAPI>();
        if (system == null) return result;
        SectorEntityToken gate = system.getEntityById(FOB_ITHACA_GATE_ID);
        SectorEntityToken ithaca = system.getEntityById(FOB_ITHACA_ID);
        if (isFobIthacaGate(gate)) {
            CampaignFleetAPI gateWatch = ensureFobIthacaSpartanGuard(
                    system, gate);
            if (gateWatch != null && !gateWatch.isEmpty()) {
                result.add(gateWatch);
            }
        }
        if (isFobIthaca(ithaca)) {
            result.addAll(ensureFobIthacaSpartanMouthGuards(
                    system, ithaca));
        }
        return result;
    }

    /** Starts Labor V with full rosters, then makes every TFS loss persistent. */
    static void beginFobIthacaFinalLaborAttrition(StarSystemAPI system) {
        if (system == null || isFobIthacaFinalLaborAttritionLocked(system)) {
            return;
        }
        system.getMemoryWithoutUpdate().unset(
                FOB_ITHACA_FINAL_LABOR_ATTRITION_LOCK);
        ensureFobIthacaSpartanGuards(system);
        system.getMemoryWithoutUpdate().set(
                FOB_ITHACA_FINAL_LABOR_ATTRITION_LOCK, true);
    }

    /** Ends the attrition snapshot; normal fleet replenishment may resume. */
    static void endFobIthacaFinalLaborAttrition(
            StarSystemAPI system, boolean restoreNow) {
        if (system == null) return;
        system.getMemoryWithoutUpdate().unset(
                FOB_ITHACA_FINAL_LABOR_ATTRITION_LOCK);
        if (restoreNow) ensureFobIthacaSpartanGuards(system);
    }

    static boolean isFobIthacaFinalLaborAttritionLocked(
            StarSystemAPI system) {
        return system != null && system.getMemoryWithoutUpdate().getBoolean(
                FOB_ITHACA_FINAL_LABOR_ATTRITION_LOCK);
    }

    /** A non-hostile, non-despawning Spartan picket that visibly seals the gate. */
    private static CampaignFleetAPI ensureFobIthacaSpartanGuard(
            StarSystemAPI system, SectorEntityToken gate) {
        SectorEntityToken existing = system.getEntityById(
                FOB_ITHACA_SPARTAN_GUARD_ID);
        CampaignFleetAPI guard = existing instanceof CampaignFleetAPI
                ? (CampaignFleetAPI) existing : null;
        boolean created = false;
        if (existing != null
                && !isOwnedFobIthacaSpartanGuard(guard)) {
            boolean alreadyLogged = system.getMemoryWithoutUpdate() != null
                    && system.getMemoryWithoutUpdate().getBoolean(
                            FOB_ITHACA_SPARTAN_GUARD_INVALID_V1);
            if (!alreadyLogged) {
                Global.getLogger(OdysseyExpanseSystem.class).error(
                        "Spartan gate-watch ID contains incompatible "
                                + "serialized state; refusing replacement");
                if (system.getMemoryWithoutUpdate() != null) {
                    system.getMemoryWithoutUpdate().set(
                            FOB_ITHACA_SPARTAN_GUARD_INVALID_V1, true);
                }
            }
            return null;
        }
        if (guard == null) {
            if (isFobIthacaFinalLaborAttritionLocked(system)) return null;
            boolean previouslyCreated = system.getMemoryWithoutUpdate()
                    .getBoolean(FOB_ITHACA_SPARTAN_GUARD_CREATED_V1);
            if (!previouslyCreated
                    && system.getMemoryWithoutUpdate().getBoolean(
                            GENERATED_ASHEN_VERGE)) {
                // Do not invent the guard solely to repair an older save.
                system.getMemoryWithoutUpdate().set(
                        FOB_ITHACA_SPARTAN_GUARD_CREATED_V1, true);
                return null;
            }
            guard = Global.getFactory().createEmptyFleet(
                    TaskForceSpartanFaction.ID,
                    "Task Force Spartan Gate Watch",
                    true);
            guard.setId(FOB_ITHACA_SPARTAN_GUARD_ID);
            guard.setName("Task Force Spartan Gate Watch");
            guard.setNoFactionInName(true);
            system.addEntity(guard);
            system.getMemoryWithoutUpdate().set(
                    FOB_ITHACA_SPARTAN_GUARD_CREATED_V1, true);
            created = true;
        } else {
            system.getMemoryWithoutUpdate().set(
                    FOB_ITHACA_SPARTAN_GUARD_CREATED_V1, true);
        }

        if (TroyArrivalScript.isFleetBusyForMutation(guard)) return guard;

        maintainFobSpartanRoster(
                system,
                guard,
                FOB_ITHACA_SPARTAN_GATE_VARIANTS,
                FOB_ITHACA_SPARTAN_GATE_HULLS);

        configureFobSpartanCampaignFlags(guard);
        if (guard.getBattle() == null) {
            // Place a new fleet once. Reconciliation runs repeatedly while
            // Starving fleets choose targets, so teleporting an existing
            // guard here makes it move and visibly snap back every cycle.
            if (created) {
                guard.setLocation(
                        gate.getLocation().x,
                        gate.getLocation().y - 450f);
            }
            // Do not overwrite a live intercept/engage course during the
            // recurring population pass. Re-arm the long guard order only
            // when the assignment queue is actually empty.
            if (guard.getCurrentAssignment() == null) {
                guard.addAssignment(
                        FleetAssignment.DEFEND_LOCATION,
                        gate,
                        1000000f,
                        "maintaining the sealed Janus cordon");
            }
        }
        return guard;
    }

    private static List<CampaignFleetAPI> ensureFobIthacaSpartanMouthGuards(
            StarSystemAPI system, SectorEntityToken ithaca) {
        List<CampaignFleetAPI> result = new ArrayList<CampaignFleetAPI>();
        for (int index = 0;
                index < FOB_ITHACA_SPARTAN_MOUTH_IDS.length;
                index++) {
            float x = ithaca.getLocation().x
                    + FOB_ITHACA_SPARTAN_MOUTH_OFFSETS[index][0];
            float y = ithaca.getLocation().y
                    + FOB_ITHACA_SPARTAN_MOUTH_OFFSETS[index][1];
            SectorEntityToken anchor = system.getEntityById(
                    FOB_ITHACA_SPARTAN_MOUTH_ANCHOR_IDS[index]);
            boolean anchorCreated = false;
            if (anchor == null) {
                String anchorCreatedKey =
                        FOB_ITHACA_SPARTAN_MOUTH_ANCHOR_CREATED_PREFIX
                                + index;
                if (system.getMemoryWithoutUpdate().getBoolean(
                            anchorCreatedKey)
                        || system.getMemoryWithoutUpdate().getBoolean(
                                GENERATED_ASHEN_VERGE)) {
                    system.getMemoryWithoutUpdate().set(
                            anchorCreatedKey, true);
                } else {
                    anchor = system.createToken(x, y);
                    anchor.setId(
                            FOB_ITHACA_SPARTAN_MOUTH_ANCHOR_IDS[index]);
                    anchor.getMemoryWithoutUpdate().set(
                            FOB_ITHACA_SPARTAN_MOUTH_ANCHOR_MARKER, true);
                    system.getMemoryWithoutUpdate().set(
                            anchorCreatedKey, true);
                    anchorCreated = true;
                }
            } else {
                MemoryAPI anchorMemory = anchor.getMemoryWithoutUpdate();
                if (anchorMemory == null || !anchorMemory.getBoolean(
                        FOB_ITHACA_SPARTAN_MOUTH_ANCHOR_MARKER)) {
                    String invalidKey =
                            FOB_ITHACA_SPARTAN_MOUTH_ANCHOR_INVALID_PREFIX
                                    + index;
                    if (system.getMemoryWithoutUpdate() == null
                            || !system.getMemoryWithoutUpdate().getBoolean(
                                    invalidKey)) {
                        Global.getLogger(OdysseyExpanseSystem.class).error(
                                "Spartan mouth-anchor ID " + index
                                        + " contains incompatible serialized "
                                        + "state; refusing mutation");
                        if (system.getMemoryWithoutUpdate() != null) {
                            system.getMemoryWithoutUpdate().set(
                                    invalidKey, true);
                        }
                    }
                    anchor = null;
                } else if (system.getMemoryWithoutUpdate() != null) {
                    system.getMemoryWithoutUpdate().set(
                            FOB_ITHACA_SPARTAN_MOUTH_ANCHOR_CREATED_PREFIX
                                    + index,
                            true);
                }
            }
            if (anchorCreated) {
                anchor.setFixedLocation(x, y);
            }

            SectorEntityToken existing = system.getEntityById(
                    FOB_ITHACA_SPARTAN_MOUTH_IDS[index]);
            CampaignFleetAPI guard = existing instanceof CampaignFleetAPI
                    ? (CampaignFleetAPI) existing : null;
            boolean created = false;
            if (existing != null
                    && !isOwnedFobIthacaSpartanGuard(guard)) {
                String invalidKey =
                        FOB_ITHACA_SPARTAN_MOUTH_INVALID_PREFIX + index;
                boolean alreadyLogged = system.getMemoryWithoutUpdate()
                        != null && system.getMemoryWithoutUpdate()
                                .getBoolean(invalidKey);
                if (!alreadyLogged) {
                    Global.getLogger(OdysseyExpanseSystem.class).error(
                            "Spartan mouth-guard ID " + index
                                    + " contains incompatible serialized "
                                    + "state; refusing replacement");
                    if (system.getMemoryWithoutUpdate() != null) {
                        system.getMemoryWithoutUpdate().set(
                                invalidKey, true);
                    }
                }
                continue;
            }
            if (guard == null) {
                if (isFobIthacaFinalLaborAttritionLocked(system)) continue;
                String createdKey =
                        FOB_ITHACA_SPARTAN_MOUTH_CREATED_PREFIX + index;
                boolean previouslyCreated = system.getMemoryWithoutUpdate()
                        .getBoolean(createdKey);
                if (!previouslyCreated
                        && system.getMemoryWithoutUpdate().getBoolean(
                                GENERATED_ASHEN_VERGE)) {
                    // Do not invent the guard solely to repair an older save.
                    system.getMemoryWithoutUpdate().set(createdKey, true);
                    continue;
                }
                guard = Global.getFactory().createEmptyFleet(
                        TaskForceSpartanFaction.ID,
                        FOB_ITHACA_SPARTAN_MOUTH_NAMES[index],
                        true);
                guard.setId(FOB_ITHACA_SPARTAN_MOUTH_IDS[index]);
                guard.setName(FOB_ITHACA_SPARTAN_MOUTH_NAMES[index]);
                guard.setNoFactionInName(true);
                system.addEntity(guard);
                system.getMemoryWithoutUpdate().set(createdKey, true);
                created = true;
            } else {
                system.getMemoryWithoutUpdate().set(
                        FOB_ITHACA_SPARTAN_MOUTH_CREATED_PREFIX + index,
                        true);
            }

            if (TroyArrivalScript.isFleetBusyForMutation(guard)) {
                if (!guard.isEmpty()) result.add(guard);
                continue;
            }
            maintainFobSpartanRoster(
                    system,
                    guard,
                    FOB_ITHACA_SPARTAN_MOUTH_VARIANTS[index],
                    FOB_ITHACA_SPARTAN_MOUTH_HULLS[index]);
            configureFobSpartanCampaignFlags(guard);
            if (guard.getBattle() == null) {
                // Existing guards must be allowed to follow their campaign
                // assignment. Repeated hard placement causes visible jitter.
                if (created) guard.setLocation(x, y);
                if (anchor != null && guard.getCurrentAssignment() == null) {
                    guard.addAssignment(
                            FleetAssignment.DEFEND_LOCATION,
                            anchor,
                            1000000f,
                            "guarding the mouth of FOB Ithaca");
                }
            }
            if (!guard.isEmpty()) result.add(guard);
        }
        return result;
    }

    private static void configureFobSpartanCampaignFlags(
            CampaignFleetAPI guard) {
        guard.getMemoryWithoutUpdate().set(
                FOB_ITHACA_SPARTAN_FLEET_MARKER, true);
        // setFaction migrates serialized Hegemony fleets in existing saves.
        if (guard.getFaction() == null
                || !TaskForceSpartanFaction.ID.equals(
                        guard.getFaction().getId())) {
            guard.setFaction(TaskForceSpartanFaction.ID, true);
        }
        guard.setNoAutoDespawn(true);
        guard.setAbortDespawn(true);
        guard.setTransponderOn(true);
        // Older builds made Spartan globally non-hostile. That override also
        // suppressed faction hostility to Threat and kept the fleets out of
        // nearby battles.
        guard.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE);
        guard.getMemoryWithoutUpdate().unset(
                MemFlags.NON_HOSTILE_OVERRIDES_MAKE_HOSTILE);
        guard.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE);
        guard.getMemoryWithoutUpdate().unset(
                MemFlags.FLEET_IGNORES_OTHER_FLEETS);
        guard.getMemoryWithoutUpdate().unset(
                MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS);
        guard.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        guard.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
    }

    /** True only for one of Ithaca's three authored Spartan cordon fleets. */
    static boolean isOwnedFobIthacaSpartanGuard(CampaignFleetAPI fleet) {
        if (fleet == null || isPlayerControlledFleet(fleet)
                || fleet.getId() == null) return false;
        boolean authoredId = FOB_ITHACA_SPARTAN_GUARD_ID.equals(fleet.getId())
                || FOB_ITHACA_SPARTAN_MOUTH_NORTH_ID.equals(fleet.getId())
                || FOB_ITHACA_SPARTAN_MOUTH_SOUTH_ID.equals(fleet.getId());
        if (!authoredId) return false;
        return fleet.getMemoryWithoutUpdate() != null
                && fleet.getMemoryWithoutUpdate().getBoolean(
                        FOB_ITHACA_SPARTAN_FLEET_MARKER);
    }

    private static void maintainFobSpartanRoster(
            StarSystemAPI system,
            CampaignFleetAPI guard,
            String[] variants,
            String[] hulls) {
        if (guard == null || guard.getBattle() != null) return;
        if (isFobIthacaFinalLaborAttritionLocked(system)) return;
        java.util.List<FleetMemberAPI> members =
                guard.getFleetData().getMembersListCopy();
        boolean[] retained = new boolean[members.size()];
        boolean changed = false;
        for (int desired = 0;
                desired < variants.length;
                desired++) {
            FleetMemberAPI member = null;
            for (int existing = 0; existing < members.size(); existing++) {
                if (!retained[existing]
                        && hulls[desired].equals(
                                members.get(existing).getHullId())) {
                    retained[existing] = true;
                    member = members.get(existing);
                    break;
                }
            }
            if (member == null) {
                member = addFobSpartanMember(
                        guard, variants[desired]);
                changed = true;
            }
            if (member != null && prepareFobSpartanMember(member)) {
                changed = true;
            }
        }

        // Preserve unmatched serialized ships. Reconciliation may restore
        // missing authored hulls, but it must not delete save contents.
        if (changed) {
            guard.getFleetData().setSyncNeeded();
            guard.getFleetData().syncIfNeeded();
        }
    }

    private static FleetMemberAPI addFobSpartanMember(
            CampaignFleetAPI fleet, String variantId) {
        FleetMemberAPI member = Global.getFactory().createFleetMember(
                FleetMemberType.SHIP, variantId);
        fleet.getFleetData().addFleetMember(member);
        return member;
    }

    private static boolean prepareFobSpartanMember(FleetMemberAPI member) {
        boolean changed = false;
        if (member.getVariant().hasHullMod(
                LEGACY_SPARTAN_THREAT_WARD_ID)) {
            member.setVariant(member.getVariant().clone(), false, false);
            member.getVariant().removeMod(LEGACY_SPARTAN_THREAT_WARD_ID);
            member.getVariant().removePermaMod(
                    LEGACY_SPARTAN_THREAT_WARD_ID);
            member.getVariant().getSMods().remove(
                    LEGACY_SPARTAN_THREAT_WARD_ID);
            member.getVariant().removeSuppressedMod(
                    LEGACY_SPARTAN_THREAT_WARD_ID);
            changed = true;
        }
        member.getStatus().repairFully();
        member.getStatus().resetAmmoState();
        member.getRepairTracker().setCR(
                member.getRepairTracker().getMaxCR());
        return changed;
    }

    /**
     * Keeps Ithaca marketless and retires the short-lived native-market
     * implementation without losing anything the player stored there.
     */
    private static void ensureFobIthacaServices(SectorEntityToken station) {
        if (station == null || Global.getSector() == null) return;

        CargoAPI entityCargo = FobIthacaStorageDialog.getStoredCargo(station);
        List<MarketAPI> retired = new ArrayList<MarketAPI>();
        MarketAPI attached = station.getMarket();
        if (isOwnedFobIthacaMarket(attached, station)) {
            retired.add(attached);
        } else if (attached != null) {
            Global.getLogger(OdysseyExpanseSystem.class).warn(
                    "Detaching an unexpected market from authored FOB "
                            + "Ithaca without deleting that market");
        }
        MarketAPI saved = Global.getSector().getEconomy().getMarket(
                FOB_ITHACA_MARKET_ID);
        if (isOwnedFobIthacaMarket(saved, station)
                && !retired.contains(saved)) {
            retired.add(saved);
        }
        for (MarketAPI market : retired) {
            retireFobIthacaMarket(market, station, entityCargo);
        }

        station.setMarket(null);
        // Station-owned cargo is the persistent backing store for the custom cargo,
        // fleet, and refit screens. Free transfer makes it behave like the
        // storage tab without registering a market or population.
        station.setFreeTransfer(true);
        FobIthacaContacts.ensureForStation(station);
    }

    private static boolean isOwnedFobIthacaMarket(
            MarketAPI market, SectorEntityToken station) {
        return market != null && (FOB_ITHACA_MARKET_ID.equals(market.getId())
                || market.getMemoryWithoutUpdate().getBoolean(
                        FOB_ITHACA_MARKET_MARKER)
                || market.getPrimaryEntity() == station);
    }

    private static void retireFobIthacaMarket(
            MarketAPI market,
            SectorEntityToken station,
            CargoAPI entityCargo) {
        if (market == null) return;

        if (market.getSubmarket(Submarkets.SUBMARKET_STORAGE) != null) {
            CargoAPI storageCargo = market.getSubmarket(
                    Submarkets.SUBMARKET_STORAGE).getCargoNullOk();
            if (storageCargo != null && entityCargo != null
                    && storageCargo != entityCargo) {
                entityCargo.addAll(storageCargo);
                storageCargo.clear();
            }
        }

        ImportantPeopleAPI important = Global.getSector().getImportantPeople();
        for (PersonAPI person : new ArrayList<PersonAPI>(
                market.getPeopleCopy())) {
            if (market.getCommDirectory().getEntryForPerson(person) != null) {
                market.getCommDirectory().removePerson(person);
            }
            market.removePerson(person);
            person.setMarket(null);
            if (FobIthacaContacts.isOwnedIthacaContact(person)) {
                ImportantPeopleAPI.PersonDataAPI data = important.getData(
                        person);
                if (data != null && data.getLocation() != null) {
                    data.getLocation().setMarket(null);
                    data.getLocation().setEntity(station);
                }
            } else if (important.containsPerson(person)) {
                // These are the procedural administrators and officers that
                // prompted this rollback. They belong only to the retired
                // market and must not survive it as orphaned contacts.
                important.removePerson(person);
            }
        }
        market.getCommDirectory().clear();
        if (market.getPrimaryEntity() == station) {
            market.setPrimaryEntity(null);
        }
        if (market.isInEconomy()) {
            Global.getSector().getEconomy().removeMarket(market);
        }
    }

    private static void ensureWhiteDwarfStar(
            StarSystemAPI system,
            String id,
            String name) {
        if (system.getStar() == null) {
            PlanetAPI star = system.initStar(
                    id + "_star", StarTypes.WHITE_DWARF, 450f, 300f);
            star.setName(name);
        } else {
            system.getStar().setName(name);
        }
        system.getStar().setSkipForJumpPointAutoGen(false);
        system.getMemoryWithoutUpdate().set(GENERATED_WHITE_DWARF, true);
    }

    /** Creates the bright Ashen Verge primary and migrates its old dwarfs. */
    private static void ensureAshenVergeSun(
            StarSystemAPI system,
            String id,
            String name) {
        PlanetAPI star = system.getStar();
        if (star == null) {
            star = system.initStar(
                    id + "_star", StarTypes.YELLOW, 450f, 350f);
        } else if (!StarTypes.YELLOW.equals(star.getTypeId())) {
            star.changeType(StarTypes.YELLOW, new Random(id.hashCode()));
        }
        star.setName(name);
        star.setRadius(450f);
        star.setSkipForJumpPointAutoGen(false);
        system.getMemoryWithoutUpdate().set(
                GENERATED_ASHEN_VERGE_SUN_V2, true);
    }

    /**
     * Adds one authored blue cloud layer over the warm distant background.
     * Its mask leaves Ithaca and the western final-Labor approach clear.
     */
    private static void ensureAshenVergeNebula(StarSystemAPI system) {
        if (system == null) return;
        system.setHasSystemwideNebula(true);

        SectorEntityToken idMatch = system.getEntityById(
                ASHEN_VERGE_NEBULA_ID);
        if (idMatch != null && !(idMatch instanceof CampaignTerrainAPI)) {
            Global.getLogger(OdysseyExpanseSystem.class).error(
                    "Ashen Verge nebula ID contains incompatible serialized "
                            + "state; refusing replacement");
            system.getMemoryWithoutUpdate().set(
                    GENERATED_ASHEN_VERGE_NEBULA_V1, true);
            return;
        }

        CampaignTerrainAPI nebula = idMatch instanceof CampaignTerrainAPI
                ? (CampaignTerrainAPI) idMatch : null;
        if (nebula == null) {
            for (CampaignTerrainAPI terrain : system.getTerrainCopy()) {
                if (terrain != null
                        && terrain.getMemoryWithoutUpdate().getBoolean(
                                ASHEN_VERGE_NEBULA_MARKER)) {
                    nebula = terrain;
                    break;
                }
            }
        }
        if (nebula != null) {
            nebula.setId(ASHEN_VERGE_NEBULA_ID);
            nebula.getMemoryWithoutUpdate().set(
                    ASHEN_VERGE_NEBULA_MARKER, true);
            applyAshenVergeNebulaPalette(nebula);
            system.getMemoryWithoutUpdate().set(
                    GENERATED_ASHEN_VERGE_NEBULA_V1, true);
            return;
        }

        // A saved system which already recorded this layer but lost its token
        // stays authoritative. Do not recreate serialized terrain on load.
        if (system.getMemoryWithoutUpdate().getBoolean(
                GENERATED_ASHEN_VERGE_NEBULA_V1)) {
            return;
        }

        SectorEntityToken created = Misc.addNebulaFromPNG(
                ASHEN_VERGE_NEBULA_MASK,
                0f,
                0f,
                system,
                "terrain",
                ASHEN_VERGE_NEBULA_TEXTURE,
                4,
                4,
                StarAge.OLD);
        if (!(created instanceof CampaignTerrainAPI)) {
            Global.getLogger(OdysseyExpanseSystem.class).error(
                    "Could not create the authored Ashen Verge nebula");
            return;
        }
        nebula = (CampaignTerrainAPI) created;
        nebula.setId(ASHEN_VERGE_NEBULA_ID);
        nebula.getMemoryWithoutUpdate().set(
                ASHEN_VERGE_NEBULA_MARKER, true);
        applyAshenVergeNebulaPalette(nebula);
        system.getMemoryWithoutUpdate().set(
                GENERATED_ASHEN_VERGE_NEBULA_V1, true);
    }

    /** Recolors an already-serialized layer without replacing its terrain. */
    private static void applyAshenVergeNebulaPalette(
            CampaignTerrainAPI nebula) {
        if (nebula == null
                || !(nebula.getPlugin() instanceof BaseTiledTerrain)) {
            return;
        }
        BaseTiledTerrain plugin = (BaseTiledTerrain) nebula.getPlugin();
        BaseTiledTerrain.TileParams params = plugin.getParams();
        if (params == null
                || ("terrain".equals(params.cat)
                        && ASHEN_VERGE_NEBULA_TEXTURE.equals(params.key))) {
            return;
        }
        params.cat = "terrain";
        params.key = ASHEN_VERGE_NEBULA_TEXTURE;
        plugin.init(nebula.getType(), nebula, params);
    }

    private static void ensureEnvironmentAnchor(
            StarSystemAPI system,
            String id,
            String type) {
        SectorEntityToken anchor = system.getEntityById(id);
        if (anchor == null || !type.equals(anchor.getCustomEntityType())) {
            if (anchor != null) system.removeEntity(anchor);
            anchor = system.addCustomEntity(id, "", type, "neutral");
        }
        anchor.setFixedLocation(0f, 0f);
        anchor.setDiscoverable(false);
        anchor.setSensorProfile(0f);
        if (MESSINA_BLACK_CLOUD_TYPE.equals(type)
                && anchor instanceof CustomCampaignEntityAPI) {
            // Active layers are serialized. Repair older saves in place
            // without deleting or replacing the fog anchor.
            ((CustomCampaignEntityAPI) anchor).setActiveLayers(
                    CampaignEngineLayers.TERRAIN_5);
        }
    }

    /** Defensive copy for the renderer so save-loaded plugins cannot mutate it. */
    public static float[][] getSilentWakeObstacles() {
        float[][] copy = new float[SILENT_WAKE_OBSTACLES.length][3];
        for (int index = 0; index < SILENT_WAKE_OBSTACLES.length; index++) {
            System.arraycopy(SILENT_WAKE_OBSTACLES[index], 0, copy[index], 0, 3);
        }
        return copy;
    }

    private static StarSystemAPI ensureMessinaSystem(float[] offset) {
        StarSystemAPI system = findSystem(MESSINA_ID);
        boolean created = system == null;
        if (created) system = Global.getSector().createStarSystem("Avici");

        system.setName("Avici");
        system.setBaseName("Avici");
        system.setOptionalUniqueId(MESSINA_ID);
        system.getLocation().set(
                getHoleCenterX() + offset[0],
                getHoleCenterY() + offset[1]);
        system.setBackgroundTextureFilename("graphics/backgrounds/background4.jpg");
        system.setLightColor(new Color(92, 72, 118));
        system.setAge(StarAge.OLD);
        system.setType(StarSystemType.SINGLE);
        system.setProcgen(false);
        system.setMapGridWidthOverride(26000f);
        system.setMapGridHeightOverride(22000f);
        system.setMaxRadiusInHyperspace(1200f);
        system.setDoNotShowIntelFromThisLocationOnMap(false);
        system.getMemoryWithoutUpdate().set(AUTHORED_SYSTEM_MARKER, true);
        applyClusterTags(system);

        if (system.getStar() == null) {
            PlanetAPI blackHole = system.initStar(
                    MESSINA_ID + "_black_hole",
                    StarTypes.BLACK_HOLE,
                    500f,
                    900f);
        }
        system.getStar().setName("Avici");
        if (created || !system.getMemoryWithoutUpdate().getBoolean(
                GENERATED_MESSINA)) {
            system.getMemoryWithoutUpdate().set(GENERATED_MESSINA, true);
        }
        ensureEnvironmentAnchor(
                system, MESSINA_BLACK_CLOUD_ID, MESSINA_BLACK_CLOUD_TYPE);
        AviciHabitatEncounter.ensureExists(system);
        ensureMessinaBreach(system);
        return system;
    }

    private static JumpPointAPI ensureMessinaBreach(StarSystemAPI system) {
        JumpPointAPI breach = null;
        SectorEntityToken byId = system.getEntityById(MESSINA_BREACH_ID);
        if (byId instanceof JumpPointAPI) breach = (JumpPointAPI) byId;
        if (breach == null && !system.getJumpPoints().isEmpty()) {
            breach = (JumpPointAPI) system.getJumpPoints().get(0);
        }
        if (breach == null) {
            breach = Global.getFactory().createJumpPoint(
                    MESSINA_BREACH_ID,
                    "Avici Jump-point");
            system.addEntity(breach);
        }

        breach.setId(MESSINA_BREACH_ID);
        breach.setName("Avici Jump-point");
        breach.setFixedLocation(-10800f, -1200f);
        breach.setRadius(200f);
        makePermanentMapLandmark(breach);
        breach.setAutoCreateEntranceFromHyperspace(false);
        breach.setStandardWormholeToHyperspaceVisual();
        breach.setInteractionImage("illustrations", "jump_point_normal");
        return breach;
    }

    private static void configureClusterSystem(
            StarSystemAPI system,
            String id,
            String name,
            float[] offset) {
        system.setName(name);
        system.setBaseName(name);
        system.setOptionalUniqueId(id);
        system.getLocation().set(
                getHoleCenterX() + offset[0],
                getHoleCenterY() + offset[1]);
        system.setBackgroundTextureFilename("graphics/backgrounds/background4.jpg");
        system.setLightColor(new Color(205, 225, 255));
        system.setAge(StarAge.OLD);
        system.setType(StarSystemType.SINGLE);
        system.setProcgen(false);
        system.setMaxRadiusInHyperspace(1100f);
        system.setDoNotShowIntelFromThisLocationOnMap(false);
        system.getMemoryWithoutUpdate().set(AUTHORED_SYSTEM_MARKER, true);
        applyClusterTags(system);
        if (system.getStar() != null) system.getStar().setName(name);
    }

    /**
     * Generates the sixteen ordinary Odyssey passages once, then rediscovers
     * and presents those same serialized objects on subsequent loads. Body
     * gravity wells remain ordinary {@link NascentGravityWellAPI} entities.
     */
    private static void configureClusterJumpPoints(
            StarSystemAPI expanse,
            List<StarSystemAPI> clusterSystems) {
        LocationAPI hyperspace = Global.getSector().getHyperspace();
        StarSystemAPI messina = findSystem(MESSINA_ID);
        if (messina == null || messina.getStar() == null) return;

        List<StarSystemAPI> allSystems = new ArrayList<StarSystemAPI>();
        allSystems.add(expanse);
        allSystems.addAll(clusterSystems);
        List<JumpPointAPI> keepHyper = new ArrayList<JumpPointAPI>();

        for (StarSystemAPI system : allSystems) {
            ensurePermanentHyperspaceAnchor(system);
            boolean mayGenerate = !system.getMemoryWithoutUpdate()
                    .getBoolean(VANILLA_HYPERSPACE_ACCESS_V4)
                    && !system.getMemoryWithoutUpdate().getBoolean(
                            HYPERSPACE_GENERATION_ATTEMPTED_V1);
            List<JumpPointAPI> keepLocal = new ArrayList<JumpPointAPI>();
            JumpPointAPI local = mayGenerate
                    ? ensureClusterLocalEndpoint(system)
                    : findExistingClusterLocalEndpoint(system, 0);
            if (local == null) continue;
            keepLocal.add(local);

            int expectedCount = getAuthoredHyperspaceEndpointCount(
                    system.getOptionalUniqueId());
            boolean singleEntrance = expectedCount == 1;
            if (!singleEntrance) {
                JumpPointAPI secondary = mayGenerate
                        ? ensureClusterSecondaryEndpoint(system)
                        : findExistingClusterLocalEndpoint(system, 1);
                if (secondary != null) {
                    keepLocal.add(secondary);
                }

                JumpPointAPI tertiary = mayGenerate
                        ? ensureClusterTertiaryEndpoint(system)
                        : findExistingClusterLocalEndpoint(system, 2);
                if (tertiary != null) {
                    keepLocal.add(tertiary);
                }
            }

            for (JumpPointAPI authored : keepLocal) {
                if (mayGenerate) {
                    authored.setAutoCreateEntranceFromHyperspace(true);
                    authored.setStandardWormholeToHyperspaceVisual();
                }
                makePermanentMapLandmark(authored);
                authored.forceOpen();
            }
            if (mayGenerate) {
                for (PlanetAPI body : system.getPlanets()) {
                    body.setSkipForJumpPointAutoGen(false);
                }
            }

            if (mayGenerate) {
                system.getMemoryWithoutUpdate().set(
                        HYPERSPACE_GENERATION_ATTEMPTED_V1, true);
                for (JumpPointAPI authored : keepLocal) {
                    authored.clearDestinations();
                }
                // This method is reached for first-time world construction,
                // not routine loading. Never prune existing hyperspace state
                // here: another mod may legitimately own nearby entities.
                system.autogenerateHyperspaceJumpPoints(true, false, false);
            }

            int paired = 0;
            for (int routeIndex = 0;
                    routeIndex < keepLocal.size(); routeIndex++) {
                JumpPointAPI localEndpoint = keepLocal.get(routeIndex);
                JumpPointAPI hyperEndpoint = findExistingHyperspaceEndpoint(
                        hyperspace, localEndpoint);
                if (hyperEndpoint == null) continue;
                placeClusterHyperEndpoint(
                        hyperEndpoint, system, routeIndex);
                makePermanentMapLandmark(hyperEndpoint);
                hyperEndpoint.forceOpen();
                if (!keepHyper.contains(hyperEndpoint)) {
                    keepHyper.add(hyperEndpoint);
                }
                paired++;
            }
            rediscoverClusterGravityWells(hyperspace, system);
            if (mayGenerate && keepLocal.size() == expectedCount
                    && paired == expectedCount) {
                system.getMemoryWithoutUpdate().set(
                        VANILLA_HYPERSPACE_ACCESS_V4, true);
            } else if (mayGenerate) {
                Global.getLogger(OdysseyExpanseSystem.class).error(
                        "Odyssey hyperspace generation remained incomplete "
                                + "for " + system.getName() + ": paired "
                                + paired + " of " + expectedCount);
            }
            system.getMemoryWithoutUpdate().set(
                    NORMAL_MESSINA_ACCESS_CONFIGURED, true);
        }

        if (!hasCompleteAuthoredHyperspaceEndpointSet(
                hyperspace, keepHyper)) {
            Global.getLogger(OdysseyExpanseSystem.class).error(
                    "Odyssey world generation produced fewer than all 16 "
                            + "authored hyperspace endpoints; preserving the "
                            + "campaign as-is instead of rebuilding it");
        }
    }

    /**
     * Moves only the sixteen canonical, already-linked hyperspace endpoints.
     * The jump objects and their destinations remain untouched so existing
     * campaign references survive the spacing repair.
     */
    private static void repairClusterJumpPointSpacing(
            StarSystemAPI expanse) {
        if (expanse == null || expanse.getMemoryWithoutUpdate().getBoolean(
                HYPERSPACE_ENDPOINT_SPACING_V1)) return;
        LocationAPI hyperspace = Global.getSector().getHyperspace();
        if (hyperspace == null) return;

        List<StarSystemAPI> systems = new ArrayList<StarSystemAPI>();
        systems.add(expanse);
        for (String id : PLACEHOLDER_IDS) {
            StarSystemAPI system = findSystem(id);
            if (system != null) systems.add(system);
        }

        int expectedTotal = 0;
        int placedTotal = 0;
        for (StarSystemAPI system : systems) {
            int expected = getAuthoredHyperspaceEndpointCount(
                    system.getOptionalUniqueId());
            expectedTotal += expected;
            for (int routeIndex = 0;
                    routeIndex < expected; routeIndex++) {
                JumpPointAPI local = findExistingClusterLocalEndpoint(
                        system, routeIndex);
                JumpPointAPI endpoint = findExistingHyperspaceEndpoint(
                        hyperspace, local);
                if (endpoint == null) continue;
                placeClusterHyperEndpoint(endpoint, system, routeIndex);
                placedTotal++;
            }
        }
        if (expectedTotal == 16 && placedTotal == expectedTotal) {
            expanse.getMemoryWithoutUpdate().set(
                    HYPERSPACE_ENDPOINT_SPACING_V1, true);
            Global.getLogger(OdysseyExpanseSystem.class).info(
                    "Spread all 16 Odyssey hyperspace jump points away "
                            + "from their previously overlapping positions");
        } else {
            Global.getLogger(OdysseyExpanseSystem.class).warn(
                    "Odyssey jump-point spacing repair found " + placedTotal
                            + " of " + expectedTotal
                            + " linked endpoints; will retry on the next load");
        }
    }

    private static void placeClusterHyperEndpoint(
            JumpPointAPI endpoint,
            StarSystemAPI system,
            int routeIndex) {
        if (endpoint == null || system == null) return;
        float[] offset = getClusterHyperOffset(system, routeIndex);
        endpoint.setFixedLocation(
                system.getLocation().x + offset[0],
                system.getLocation().y + offset[1]);
    }

    private static void removeTrackedHyperspaceAccess(
            StarSystemAPI system, LocationAPI hyperspace) {
        if (system.getAutogeneratedJumpPointsInHyper() != null) {
            for (JumpPointAPI jump : new ArrayList<JumpPointAPI>(
                    system.getAutogeneratedJumpPointsInHyper())) {
                if (jump.getContainingLocation() == hyperspace) {
                    hyperspace.removeEntity(jump);
                }
            }
            system.getAutogeneratedJumpPointsInHyper().clear();
        }
        if (system.getAutogeneratedNascentWellsInHyper() != null) {
            for (NascentGravityWellAPI well :
                    new ArrayList<NascentGravityWellAPI>(
                            system.getAutogeneratedNascentWellsInHyper())) {
                if (well.getContainingLocation() == hyperspace) {
                    hyperspace.removeEntity(well);
                }
            }
            system.getAutogeneratedNascentWellsInHyper().clear();
        }
    }

    private static void removeLegacyManualHyperspaceAccess(
            StarSystemAPI system, LocationAPI hyperspace) {
        String systemId = system.getOptionalUniqueId();
        removeEntityById(hyperspace,
                CLUSTER_JUMP_HYPER_PREFIX + systemId);
        removeEntityById(hyperspace,
                CLUSTER_SECONDARY_HYPER_PREFIX + systemId);
        removeEntityById(hyperspace,
                CLUSTER_TERTIARY_HYPER_PREFIX + systemId);
        if (MESSINA_ID.equals(systemId)) {
            removeEntityById(hyperspace, MESSINA_JUMP_HYPER_ID);
        }
        String wellPrefix = CLUSTER_GRAVITY_WELL_PREFIX + systemId;
        for (SectorEntityToken entity : new ArrayList<SectorEntityToken>(
                hyperspace.getAllEntities())) {
            String id = entity.getId();
            if (id != null && (id.equals(wellPrefix)
                    || id.startsWith(wellPrefix + "_"))) {
                hyperspace.removeEntity(entity);
            }
        }
    }

    private static void removeSurplusLocalJumpPoints(
            StarSystemAPI system, List<JumpPointAPI> keepLocal) {
        for (SectorEntityToken token : new ArrayList<SectorEntityToken>(
                system.getJumpPoints())) {
            if (keepLocal.contains(token) || ENTRY_ID.equals(token.getId())) {
                continue;
            }
            system.removeEntity(token);
        }
    }

    private static JumpPointAPI ensureClusterHyperEndpoint(
            LocationAPI hyperspace,
            StarSystemAPI system,
            JumpPointAPI local,
            String hyperId,
            String name,
            float offsetX,
            float offsetY,
            boolean useAviciSize,
            boolean mayCreateMissing) {
        // Preserve the serialized entity whenever possible.  Older builds
        // left valid jump objects in hyperspace while losing the system's
        // autogenerated bookkeeping references; recreating those objects
        // needlessly invalidates save references.  Destination identity is
        // the strongest signal, followed by the canonical stable ID.
        JumpPointAPI hyperJump = findExistingHyperspaceEndpoint(
                hyperspace, local);
        SectorEntityToken existingJump = hyperspace.getEntityById(hyperId);
        boolean mayClaimStableId = true;
        if (hyperJump == null && existingJump instanceof JumpPointAPI) {
            hyperJump = (JumpPointAPI) existingJump;
        } else if (hyperJump != null && existingJump != null
                && existingJump != hyperJump) {
            // A linked live endpoint wins over an unlinked claimant of its
            // stable ID. Only a deliberate generation/migration pass may
            // remove the duplicate and normalize the linked object's ID.
            if (mayCreateMissing) {
                hyperspace.removeEntity(existingJump);
            } else {
                mayClaimStableId = false;
            }
        } else if (hyperJump == null && existingJump != null) {
            if (mayCreateMissing) {
                hyperspace.removeEntity(existingJump);
            } else {
                return null;
            }
        }
        if (hyperJump == null && mayCreateMissing) {
            hyperJump = Global.getFactory().createJumpPoint(hyperId, name);
            hyperspace.addEntity(hyperJump);
        }
        if (hyperJump == null) return null;

        if (mayClaimStableId) hyperJump.setId(hyperId);
        hyperJump.setName(name);
        hyperJump.setFixedLocation(
                system.getLocation().x + offsetX,
                system.getLocation().y + offsetY);
        makePermanentMapLandmark(hyperJump);
        hyperJump.setAutoCreateEntranceFromHyperspace(false);
        hyperJump.setInteractionImage("illustrations", "jump_point_hyper");
        hyperJump.clearDestinations();
        hyperJump.addDestination(new JumpPointAPI.JumpDestination(
                local,
                system.getName()));
        hyperJump.setStandardWormholeToStarfieldVisual();
        hyperJump.forceOpen();
        // Only Avici's primary ingress remains enormous. Every other endpoint
        // gets a smaller, stable radius derived from its unique ID so the
        // hyperspace view does not read as a field of identical apertures.
        hyperJump.setRadius(getHyperspaceJumpRadius(hyperId, useAviciSize));

        local.setStandardWormholeToHyperspaceVisual();
        // Ordinary Odyssey-sector jump points are paired hyperspace exits,
        // never shortcuts into another realspace system. Clearing first also
        // repairs saves made while the retired Avici-Sanzu-Ashen route was
        // active. Foxtrot Terminus is not passed through this helper.
        local.clearDestinations();
        local.addDestination(new JumpPointAPI.JumpDestination(
                hyperJump,
                "Hyperspace"));
        return hyperJump;
    }

    /** Finds the existing reciprocal endpoint without relying on save lists. */
    private static JumpPointAPI findExistingHyperspaceEndpoint(
            LocationAPI hyperspace, JumpPointAPI local) {
        if (hyperspace == null || local == null) return null;
        for (SectorEntityToken entity : new ArrayList<SectorEntityToken>(
                hyperspace.getAllEntities())) {
            if (!(entity instanceof JumpPointAPI)
                    || entity.getContainingLocation() != hyperspace) {
                continue;
            }
            JumpPointAPI candidate = (JumpPointAPI) entity;
            for (JumpPointAPI.JumpDestination destination :
                    candidate.getDestinations()) {
                if (destination != null
                        && destination.getDestination() == local) {
                    return candidate;
                }
            }
        }
        for (JumpPointAPI.JumpDestination destination :
                local.getDestinations()) {
            SectorEntityToken target = destination == null
                    ? null : destination.getDestination();
            if (target instanceof JumpPointAPI
                    && target.getContainingLocation() == hyperspace) {
                return (JumpPointAPI) target;
            }
        }
        return null;
    }

    /**
     * Restores presentation on the same serialized body-well objects without
     * trusting or rewriting the system's autogenerated bookkeeping list.
     */
    private static void rediscoverClusterGravityWells(
            LocationAPI hyperspace, StarSystemAPI system) {
        if (hyperspace == null || system == null) return;
        String prefix = CLUSTER_GRAVITY_WELL_PREFIX
                + system.getOptionalUniqueId();
        for (SectorEntityToken entity : new ArrayList<SectorEntityToken>(
                hyperspace.getAllEntities())) {
            if (!(entity instanceof NascentGravityWellAPI)
                    || entity.getContainingLocation() != hyperspace) {
                continue;
            }
            SectorEntityToken target =
                    ((NascentGravityWellAPI) entity).getTarget();
            String id = entity.getId();
            if (target != null && target.getContainingLocation() == system
                    || id != null && (id.equals(prefix)
                            || id.startsWith(prefix + "_"))) {
                makePermanentMapLandmark(entity);
            }
        }
    }

    /** Guards the only constellation-wide destructive jump cleanup. */
    private static boolean hasCompleteAuthoredHyperspaceEndpointSet(
            LocationAPI hyperspace, List<JumpPointAPI> keep) {
        if (hyperspace == null || keep == null || keep.size() != 16) {
            return false;
        }
        List<JumpPointAPI> unique = new ArrayList<JumpPointAPI>();
        for (JumpPointAPI endpoint : keep) {
            if (endpoint == null
                    || endpoint.getContainingLocation() != hyperspace
                    || unique.contains(endpoint)) {
                return false;
            }
            unique.add(endpoint);
        }
        return true;
    }

    /** Stable authored ID for one of the sixteen ordinary hyperspace ends. */
    static String getAuthoredHyperspaceEndpointId(
            String systemId, int routeIndex) {
        if (systemId == null || routeIndex < 0
                || routeIndex >= getAuthoredHyperspaceEndpointCount(
                        systemId)) {
            return null;
        }
        if (routeIndex == 0) {
            return MESSINA_ID.equals(systemId)
                    ? MESSINA_JUMP_HYPER_ID
                    : CLUSTER_JUMP_HYPER_PREFIX + systemId;
        }
        if (routeIndex == 1) {
            return CLUSTER_SECONDARY_HYPER_PREFIX + systemId;
        }
        return CLUSTER_TERTIARY_HYPER_PREFIX + systemId;
    }

    /** Number of ordinary authored routes exposed by one Odyssey system. */
    static int getAuthoredHyperspaceEndpointCount(String systemId) {
        if (DEVOURED_REACH_ID.equals(systemId)) return 1;
        if (SYSTEM_ID.equals(systemId)
                || MESSINA_ID.equals(systemId)
                || SILENT_WAKE_ID.equals(systemId)
                || ASHEN_VERGE_ID.equals(systemId)
                || LAST_LIGHT_ID.equals(systemId)) {
            return 3;
        }
        return 0;
    }

    private static String getAuthoredHyperspaceEndpointName(
            StarSystemAPI system,
            JumpPointAPI localEndpoint,
            int routeIndex) {
        if (routeIndex == 1) return getClusterSecondaryName(system);
        if (routeIndex == 2) return getClusterTertiaryName(system);
        String localName = localEndpoint == null
                ? null : localEndpoint.getName();
        return localName == null || localName.trim().isEmpty()
                ? system.getName() + " Jump-point" : localName;
    }

    private static float getHyperspaceJumpRadius(
            String hyperId, boolean useAviciSize) {
        if (useAviciSize) return AVICI_PRIMARY_HYPER_RADIUS;
        String systemId = hyperId == null ? "" : hyperId;
        int routeOffset = 0;
        if (systemId.startsWith(CLUSTER_SECONDARY_HYPER_PREFIX)) {
            systemId = systemId.substring(
                    CLUSTER_SECONDARY_HYPER_PREFIX.length());
            routeOffset = 13;
        } else if (systemId.startsWith(CLUSTER_TERTIARY_HYPER_PREFIX)) {
            systemId = systemId.substring(
                    CLUSTER_TERTIARY_HYPER_PREFIX.length());
            routeOffset = 27;
        } else if (systemId.startsWith(CLUSTER_JUMP_HYPER_PREFIX)) {
            systemId = systemId.substring(CLUSTER_JUMP_HYPER_PREFIX.length());
        }
        int stable = systemId.hashCode() & Integer.MAX_VALUE;
        return MIN_HYPERSPACE_JUMP_RADIUS
                + (stable + routeOffset)
                        % (int) HYPERSPACE_JUMP_RADIUS_VARIATION;
    }

    /**
     * Produces vanilla-like, stochastic-looking endpoint scatter while keeping
     * the result stable across saves. Each system gets its own angular gaps
     * and radial distances instead of a rotated copy of one triangle.
     */
    private static float[] getClusterHyperOffset(
            StarSystemAPI system, int routeIndex) {
        return getStableHyperspaceEndpointOffset(
                system == null ? null : system.getOptionalUniqueId(),
                routeIndex);
    }

    /** Package-visible for the headless topology regression. */
    static float[] getStableHyperspaceEndpointOffset(
            String systemId, int routeIndex) {
        if (systemId == null || routeIndex < 0 || routeIndex > 2) {
            return new float[] {0f, 0f};
        }

        long seed = HYPER_ENDPOINT_SCATTER_SEED
                ^ ((long) systemId.hashCode() * 0x9e3779b97f4a7c15L);
        Random random = new Random(seed);

        float startAngle = random.nextFloat() * 360f;
        float firstGap = 105f + random.nextFloat() * 30f;
        float secondGap = 105f + random.nextFloat() * 30f;
        float[] angles = {
            startAngle,
            startAngle + firstGap,
            startAngle + firstGap + secondGap
        };
        float[] radii = new float[3];
        for (int index = 0; index < radii.length; index++) {
            radii[index] = MIN_HYPER_ENDPOINT_OFFSET
                    + random.nextFloat() * HYPER_ENDPOINT_OFFSET_VARIATION;
        }

        // Avici sits in the pocket's feathered southeast edge. Scatter its
        // apertures through an irregular inward-facing fan so none fall back
        // into the surrounding Abyss.
        if (MESSINA_ID.equals(systemId)) {
            float inward = (float) Math.toDegrees(Math.atan2(5500f, -6500f));
            float[] fan = {-65f, 0f, 65f};
            for (int index = 0; index < fan.length; index++) {
                angles[index] = inward + fan[index]
                        + (random.nextFloat() - 0.5f) * 10f;
            }
        }

        double radians = Math.toRadians(angles[routeIndex]);
        return new float[] {
            (float) Math.cos(radians) * radii[routeIndex],
            (float) Math.sin(radians) * radii[routeIndex]
        };
    }

    /**
     * Restores vanilla-style exposed gravity wells for every authored
     * celestial body. Primaries, planets, and the three hollow worlds each
     * keep an independently targeted well.
     */
    private static List<NascentGravityWellAPI> ensureClusterGravityWells(
            LocationAPI hyperspace,
            StarSystemAPI system) {
        List<NascentGravityWellAPI> result =
                new ArrayList<NascentGravityWellAPI>();
        boolean rebuild = !system.getMemoryWithoutUpdate().getBoolean(
                CLUSTER_BODY_WELL_REPAIR_V3);
        if (rebuild) {
            // Replace the former single-primary repair with one clean well
            // per body, including saves whose old primary token was invisible.
            for (SectorEntityToken token : new ArrayList<SectorEntityToken>(
                    hyperspace.getAllEntities())) {
                if (!(token instanceof NascentGravityWellAPI)) continue;
                SectorEntityToken target =
                        ((NascentGravityWellAPI) token).getTarget();
                if (isClusterGravityWellForSystem(token, target, system)) {
                    hyperspace.removeEntity(token);
                }
            }
        }

        List<SectorEntityToken> bodies =
                new ArrayList<SectorEntityToken>();
        bodies.addAll(system.getPlanets());
        if (DEVOURED_REACH_ID.equals(system.getOptionalUniqueId())) {
            for (String hollowWorldId : HOLLOW_WORLD_IDS) {
                SectorEntityToken hollowWorld = system.getEntityById(
                        hollowWorldId);
                if (hollowWorld != null) bodies.add(hollowWorld);
            }
        }

        for (SectorEntityToken body : bodies) {
            if (body == null || body.getContainingLocation() != system) {
                continue;
            }
            String id = getClusterGravityWellId(system, body);
            SectorEntityToken existing = hyperspace.getEntityById(id);
            NascentGravityWellAPI well = null;
            if (existing instanceof NascentGravityWellAPI
                    && ((NascentGravityWellAPI) existing).getTarget()
                            == body) {
                well = (NascentGravityWellAPI) existing;
            } else if (existing != null) {
                hyperspace.removeEntity(existing);
            }
            if (well == null) {
                well = Global.getSector().createNascentGravityWell(body, 50f);
                hyperspace.addEntity(well);
            }

            well.setId(id);
            well.setName(body.getName() + " Gravity Well");
            // Radius zero stacked every body-linked well on the system anchor,
            // compositing their standard sprites into one magenta block.
            // Keep the vanilla renderer and let it distribute wells using
            // each body's realspace bearing around a conventional ring.
            well.autoUpdateHyperLocationBasedOnInSystemEntityAtRadius(
                    body, CLUSTER_BODY_WELL_RADIUS);
            well.setInteractionImage("illustrations", "jump_point_hyper");
            makePermanentMapLandmark(well);
            well.setSensorProfile(1f);
            well.setDiscoverable(false);
            well.getDetectedRangeMod().modifyFlat(id, 100000f);
            result.add(well);
        }

        // Remove randomized duplicates or stale wells for deleted bodies while
        // leaving wells owned by other systems untouched.
        for (SectorEntityToken token : new ArrayList<SectorEntityToken>(
                hyperspace.getAllEntities())) {
            if (!(token instanceof NascentGravityWellAPI)
                    || result.contains(token)) {
                continue;
            }
            SectorEntityToken target =
                    ((NascentGravityWellAPI) token).getTarget();
            if (isClusterGravityWellForSystem(token, target, system)) {
                hyperspace.removeEntity(token);
            }
        }
        system.getMemoryWithoutUpdate().set(
                CLUSTER_BODY_WELL_REPAIR_V3, true);
        return result;
    }

    private static String getClusterGravityWellId(
            StarSystemAPI system, SectorEntityToken body) {
        String systemId = system.getOptionalUniqueId();
        if (body == system.getStar()) {
            // Preserve the established primary ID so external save references
            // continue resolving after the multi-body repair.
            return CLUSTER_GRAVITY_WELL_PREFIX + systemId;
        }
        return CLUSTER_GRAVITY_WELL_PREFIX + systemId + "_" + body.getId();
    }

    private static boolean isClusterGravityWellForSystem(
            SectorEntityToken well,
            SectorEntityToken target,
            StarSystemAPI system) {
        if (target != null && target.getContainingLocation() == system) {
            return true;
        }
        String id = well == null ? null : well.getId();
        String prefix = CLUSTER_GRAVITY_WELL_PREFIX
                + system.getOptionalUniqueId();
        return id != null && (id.equals(prefix) || id.startsWith(prefix + "_"));
    }

    /**
     * Alpha Odyssey's in-system jump points are exits into hyperspace only.
     * Remove stale direct links into Sanzu or any other realspace system
     * left by earlier topology revisions.
     */
    private static void removeCrossSystemDestinationsFromExpanse(
            StarSystemAPI expanse,
            LocationAPI hyperspace) {
        for (SectorEntityToken token : new ArrayList<SectorEntityToken>(
                expanse.getJumpPoints())) {
            if (!(token instanceof JumpPointAPI)) continue;
            JumpPointAPI jump = (JumpPointAPI) token;
            if (ENTRY_ID.equals(jump.getId())) continue;
            for (JumpPointAPI.JumpDestination destination :
                    new ArrayList<JumpPointAPI.JumpDestination>(
                            jump.getDestinations())) {
                SectorEntityToken target = destination.getDestination();
                if (target == null) continue;
                LocationAPI targetLocation = target.getContainingLocation();
                if (targetLocation != null
                        && targetLocation != expanse
                        && targetLocation != hyperspace) {
                    jump.removeDestination(target);
                }
            }
        }
    }

    private static JumpPointAPI ensureClusterSecondaryEndpoint(
            StarSystemAPI system) {
        String id = system.getOptionalUniqueId();
        if (SYSTEM_ID.equals(id)) {
            return ensureLocalPassage(
                    system,
                    CLUSTER_SECONDARY_LOCAL_PREFIX + id,
                    "Alpha Odyssey Fringe Jump-point",
                    -7250f,
                    -8650f);
        }
        if (SILENT_WAKE_ID.equals(id)) {
            return ensureLocalPassage(
                    system,
                    CLUSTER_SECONDARY_LOCAL_PREFIX + id,
                    "Sanzu Downstream Jump-point",
                    -7800f,
                    -6500f);
        }
        if (ASHEN_VERGE_ID.equals(id)) {
            return ensureLocalPassage(
                    system,
                    CLUSTER_SECONDARY_LOCAL_PREFIX + id,
                    "Ashen Verge Fringe Jump-point",
                    -11200f,
                    -2500f);
        }
        if (MESSINA_ID.equals(id)) {
            return ensureLocalPassage(
                    system,
                    CLUSTER_SECONDARY_LOCAL_PREFIX + id,
                    "Avici Outer Jump-point",
                    9200f,
                    5200f);
        }
        if (DEVOURED_REACH_ID.equals(id)) {
            return ensureLocalPassage(
                    system,
                    CLUSTER_SECONDARY_LOCAL_PREFIX + id,
                    "Devoured Reach Outer Jump-point",
                    -9700f,
                    -2400f);
        }
        if (LAST_LIGHT_ID.equals(id)) {
            return ensureLocalPassage(
                    system,
                    CLUSTER_SECONDARY_LOCAL_PREFIX + id,
                    "Last Light Inner Jump-point",
                    -8200f,
                    -4100f);
        }
        return null;
    }

    private static String getClusterSecondaryName(StarSystemAPI system) {
        String id = system.getOptionalUniqueId();
        if (SYSTEM_ID.equals(id)) return "Alpha Odyssey Fringe Jump-point";
        if (SILENT_WAKE_ID.equals(id)) {
            return "Sanzu Downstream Jump-point";
        }
        if (ASHEN_VERGE_ID.equals(id)) {
            return "Ashen Verge Fringe Jump-point";
        }
        if (MESSINA_ID.equals(id)) return "Avici Outer Jump-point";
        if (DEVOURED_REACH_ID.equals(id)) {
            return "Devoured Reach Outer Jump-point";
        }
        return "Last Light Inner Jump-point";
    }

    private static JumpPointAPI ensureClusterTertiaryEndpoint(
            StarSystemAPI system) {
        String id = system.getOptionalUniqueId();
        if (SYSTEM_ID.equals(id)) {
            return ensureLocalPassage(
                    system,
                    CLUSTER_TERTIARY_LOCAL_PREFIX + id,
                    "Alpha Odyssey Inner Jump-point",
                    -3950f,
                    10350f);
        }
        if (SILENT_WAKE_ID.equals(id)) {
            return ensureLocalPassage(
                    system,
                    CLUSTER_TERTIARY_LOCAL_PREFIX + id,
                    "Sanzu Crosscurrent Jump-point",
                    9300f,
                    -2600f);
        }
        if (ASHEN_VERGE_ID.equals(id)) {
            return ensureLocalPassage(
                    system,
                    CLUSTER_TERTIARY_LOCAL_PREFIX + id,
                    "Ashen Verge Inner Jump-point",
                    7800f,
                    8800f);
        }
        if (MESSINA_ID.equals(id)) {
            return ensureLocalPassage(
                    system,
                    CLUSTER_TERTIARY_LOCAL_PREFIX + id,
                    "Avici Polar Jump-point",
                    1300f,
                    -10300f);
        }
        if (DEVOURED_REACH_ID.equals(id)) {
            return ensureLocalPassage(
                    system,
                    CLUSTER_TERTIARY_LOCAL_PREFIX + id,
                    "Devoured Reach Inner Jump-point",
                    7600f,
                    -7200f);
        }
        if (LAST_LIGHT_ID.equals(id)) {
            return ensureLocalPassage(
                    system,
                    CLUSTER_TERTIARY_LOCAL_PREFIX + id,
                    "Last Light Fringe Jump-point",
                    9100f,
                    1800f);
        }
        return null;
    }

    private static String getClusterTertiaryName(StarSystemAPI system) {
        String id = system.getOptionalUniqueId();
        if (SYSTEM_ID.equals(id)) return "Alpha Odyssey Inner Jump-point";
        if (SILENT_WAKE_ID.equals(id)) {
            return "Sanzu Crosscurrent Jump-point";
        }
        if (ASHEN_VERGE_ID.equals(id)) {
            return "Ashen Verge Inner Jump-point";
        }
        if (MESSINA_ID.equals(id)) return "Avici Polar Jump-point";
        if (DEVOURED_REACH_ID.equals(id)) {
            return "Devoured Reach Inner Jump-point";
        }
        return "Last Light Fringe Jump-point";
    }

    private static JumpPointAPI ensureClusterLocalEndpoint(
            StarSystemAPI system) {
        String id = system.getOptionalUniqueId();
        JumpPointAPI local;
        if (SYSTEM_ID.equals(id)) {
            local = ensureLocalPassage(
                    system,
                    EXPANSE_LOCAL_JUMP_ID,
                    "Odyssey Expanse Jump-point",
                    10450f,
                    1650f);
            local.clearDestinations();
            return local;
        }
        if (MESSINA_ID.equals(id)) {
            return ensureMessinaBreach(system);
        }
        if (SILENT_WAKE_ID.equals(id)) {
            return ensureLocalPassage(
                    system,
                    SILENT_WAKE_PASSAGE_ID,
                    "Sanzu Confluence",
                    1900f,
                    9200f);
        }
        if (ASHEN_VERGE_ID.equals(id)) {
            return ensureLocalPassage(
                    system,
                    ASHEN_VERGE_PASSAGE_ID,
                    "Ashen Verge Jump-point",
                    -2500f,
                    1400f);
        }
        if (DEVOURED_REACH_ID.equals(id)) {
            local = ensureLocalPassage(
                    system,
                    CLUSTER_JUMP_HYPER_PREFIX + id + "_local",
                    "Devoured Reach Threshold",
                    1800f,
                    10200f);
            local.clearDestinations();
            return local;
        }
        if (LAST_LIGHT_ID.equals(id)) {
            local = ensureLocalPassage(
                    system,
                    LAST_LIGHT_LOCAL_JUMP_ID,
                    "Last Light Jump-point",
                    -2300f,
                    8900f);
            local.clearDestinations();
            return local;
        }
        return null;
    }

    /** Looks up an authored realspace endpoint without mutating save state. */
    private static JumpPointAPI findExistingClusterLocalEndpoint(
            StarSystemAPI system, int routeIndex) {
        if (system == null || routeIndex < 0 || routeIndex > 2) return null;
        String systemId = system.getOptionalUniqueId();
        String id;
        if (routeIndex == 1) {
            id = CLUSTER_SECONDARY_LOCAL_PREFIX + systemId;
        } else if (routeIndex == 2) {
            id = CLUSTER_TERTIARY_LOCAL_PREFIX + systemId;
        } else if (SYSTEM_ID.equals(systemId)) {
            id = EXPANSE_LOCAL_JUMP_ID;
        } else if (MESSINA_ID.equals(systemId)) {
            id = MESSINA_BREACH_ID;
        } else if (SILENT_WAKE_ID.equals(systemId)) {
            id = SILENT_WAKE_PASSAGE_ID;
        } else if (ASHEN_VERGE_ID.equals(systemId)) {
            id = ASHEN_VERGE_PASSAGE_ID;
        } else if (DEVOURED_REACH_ID.equals(systemId)) {
            id = CLUSTER_JUMP_HYPER_PREFIX + systemId + "_local";
        } else if (LAST_LIGHT_ID.equals(systemId)) {
            id = LAST_LIGHT_LOCAL_JUMP_ID;
        } else {
            return null;
        }
        SectorEntityToken existing = system.getEntityById(id);
        return existing instanceof JumpPointAPI
                ? (JumpPointAPI) existing : null;
    }

    private static void removeSurplusClusterAccess(
            StarSystemAPI system,
            List<JumpPointAPI> keepLocal,
            List<JumpPointAPI> keepHyper,
            LocationAPI hyperspace,
            List<NascentGravityWellAPI> keepGravityWells) {
        for (SectorEntityToken token : new ArrayList<SectorEntityToken>(
                system.getJumpPoints())) {
            if (keepLocal.contains(token)
                    || ENTRY_ID.equals(token.getId())
                    || SILENT_WAKE_PASSAGE_ID.equals(token.getId())
                    || ASHEN_VERGE_PASSAGE_ID.equals(token.getId())) {
                continue;
            }
            system.removeEntity(token);
        }

        List<JumpPointAPI> generated = system.getAutogeneratedJumpPointsInHyper();
        if (generated != null) {
            for (JumpPointAPI hyperJump :
                    new ArrayList<JumpPointAPI>(generated)) {
                if (!keepHyper.contains(hyperJump)
                        && hyperJump.getContainingLocation() == hyperspace) {
                    hyperspace.removeEntity(hyperJump);
                }
            }
            generated.clear();
        }

        List<NascentGravityWellAPI> wells =
                system.getAutogeneratedNascentWellsInHyper();
        if (wells != null) {
            for (NascentGravityWellAPI well :
                    new ArrayList<NascentGravityWellAPI>(wells)) {
                if (!keepGravityWells.contains(well)
                        && well.getContainingLocation() == hyperspace) {
                    hyperspace.removeEntity(well);
                }
            }
            wells.clear();
            wells.addAll(keepGravityWells);
        }
    }

    private static JumpPointAPI ensureLocalPassage(
            StarSystemAPI system,
            String id,
            String name,
            float x,
            float y) {
        JumpPointAPI passage = null;
        SectorEntityToken existing = system.getEntityById(id);
        if (existing instanceof JumpPointAPI) {
            passage = (JumpPointAPI) existing;
        } else if (existing != null) {
            system.removeEntity(existing);
        }
        if (passage == null) {
            passage = Global.getFactory().createJumpPoint(id, name);
            system.addEntity(passage);
        }
        passage.setId(id);
        passage.setName(name);
        passage.setFixedLocation(x, y);
        passage.setRadius(175f);
        makePermanentMapLandmark(passage);
        passage.setAutoCreateEntranceFromHyperspace(false);
        passage.setStandardWormholeToHyperspaceVisual();
        passage.setInteractionImage("illustrations", "jump_point_normal");
        passage.forceOpen();
        return passage;
    }

    private static void removeEntityById(LocationAPI location, String id) {
        SectorEntityToken entity = location.getEntityById(id);
        if (entity != null) {
            location.removeEntity(entity);
        }
    }

    /** Gives authored landmarks the same persistent visibility as planets. */
    private static void makePermanentMapLandmark(
            SectorEntityToken entity) {
        entity.setSensorProfile(null);
        entity.setDiscoverable(null);
        entity.getDetectedRangeMod().unmodify(CLUSTER_ACCESS_DETECTED);
        entity.setExtendedDetectedAtRange(null);
    }

    /** Creates and repairs the token used to render a system in hyperspace. */
    private static void ensurePermanentHyperspaceAnchor(
            StarSystemAPI system) {
        system.generateAnchorIfNeeded();
        SectorEntityToken anchor = system.getHyperspaceAnchor();
        if (anchor == null) return;
        anchor.setLocation(
                system.getLocation().x,
                system.getLocation().y);
        makePermanentMapLandmark(anchor);
    }

    /**
     * Old saves may retain generated fringe/inner jump points after their
     * bookkeeping lists have been cleared. The Alpha Odyssey cavity is wholly
     * reserved by this mod, so remove every other hyperspace jump inside it.
     */
    private static void removeOtherJumpPointsInOdysseyHole(
            LocationAPI hyperspace,
            List<JumpPointAPI> keep) {
        float radiusSquared = LEGACY_HOLE_FEATHER_RADIUS
                * LEGACY_HOLE_FEATHER_RADIUS;
        for (SectorEntityToken entity : new ArrayList<SectorEntityToken>(
                hyperspace.getAllEntities())) {
            // Some engine implementations may expose a nascent well through
            // additional interfaces. It is never surplus jump access.
            if (entity instanceof NascentGravityWellAPI) continue;
            if (!(entity instanceof JumpPointAPI) || keep.contains(entity)) {
                continue;
            }
            float dx = entity.getLocation().x - getHoleCenterX();
            float dy = entity.getLocation().y - getHoleCenterY();
            if (dx * dx + dy * dy <= radiusSquared) {
                hyperspace.removeEntity(entity);
            }
        }
    }

    private static void applyClusterTags(StarSystemAPI system) {
        system.removeTag(Tags.THEME_HIDDEN);
        system.addTag(Tags.THEME_SPECIAL);
        system.addTag(Tags.THEME_UNSAFE);
        system.removeTag(Tags.SYSTEM_ABYSSAL);
        system.addTag(Tags.NOT_RANDOM_MISSION_TARGET);
        system.addTag(Tags.NO_TOPOGRAPHY_SCANS);
        system.addTag(Tags.DO_NOT_SHOW_STRANDED_DIALOG);
        system.removeTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER);
        system.removeTag(Tags.STAR_HIDDEN_ON_MAP);
    }

    private static void bindConstellation(
            StarSystemAPI expanse,
            List<StarSystemAPI> placeholders) {
        Constellation constellation = expanse.getConstellation();
        if (constellation == null) {
            constellation = new Constellation(
                    Constellation.ConstellationType.NEBULA, StarAge.OLD);
            constellation.getSystems().add(expanse);
            expanse.setConstellation(constellation);
        }
        constellation.setNameOverride("Odyssey Sector");
        for (StarSystemAPI system : placeholders) {
            if (!constellation.getSystems().contains(system)) {
                constellation.getSystems().add(system);
            }
            system.setConstellation(constellation);
        }
    }

    /**
     * Clears one constellation-sized cavity and suppresses Abyssal depth
     * inside it, revealing ordinary hyperspace while preserving the enclosing
     * Abyss beyond a broad feathered boundary.
     */
    private static void reserveAbyssalHole() {
        HyperspaceTerrainPlugin plugin = Misc.getHyperspaceTerrainPlugin();
        if (plugin == null) return;
        OdysseyAbyssHolePlugin.install(
                plugin,
                getHoleCenterX(),
                getHoleCenterY(),
                HOLE_RADIUS,
                HOLE_FEATHER_RADIUS);
        NebulaEditor editor = new NebulaEditor(plugin);
        restoreAbyssalRingFromSurroundings(plugin, editor);
        editor.clearArc(
                getHoleCenterX(), getHoleCenterY(),
                0f, HOLE_RADIUS, 0f, 360f);
        editor.clearArc(
                getHoleCenterX(), getHoleCenterY(),
                HOLE_RADIUS, HOLE_FEATHER_RADIUS, 0f, 360f, 0.25f);

        plugin.forceClearSampleCache();
    }

    /**
     * Older saves already have the former 20,000-unit clearing baked into the
     * hyperspace tile array. Refill its discarded outer annulus from nearby,
     * untouched Abyss tiles before cutting the new smaller pocket.
     */
    private static void restoreAbyssalRingFromSurroundings(
            HyperspaceTerrainPlugin plugin,
            NebulaEditor editor) {
        int[][] tiles = editor.getTiles();
        if (tiles == null || tiles.length == 0 || tiles[0].length == 0) return;

        float tileSize = plugin.getTileSize();
        Vector2f terrainCenter = plugin.getEntity().getLocation();
        float originX = terrainCenter.x - tiles.length * tileSize * 0.5f;
        float originY = terrainCenter.y - tiles[0].length * tileSize * 0.5f;
        int minX = Math.max(0, (int) Math.floor(
                (getHoleCenterX() - LEGACY_HOLE_FEATHER_RADIUS - originX)
                        / tileSize));
        int maxX = Math.min(tiles.length - 1, (int) Math.ceil(
                (getHoleCenterX() + LEGACY_HOLE_FEATHER_RADIUS - originX)
                        / tileSize));
        int minY = Math.max(0, (int) Math.floor(
                (getHoleCenterY() - LEGACY_HOLE_FEATHER_RADIUS - originY)
                        / tileSize));
        int maxY = Math.min(tiles[0].length - 1, (int) Math.ceil(
                (getHoleCenterY() + LEGACY_HOLE_FEATHER_RADIUS - originY)
                        / tileSize));
        float innerSquared = HOLE_FEATHER_RADIUS * HOLE_FEATHER_RADIUS;
        float outerSquared = LEGACY_HOLE_FEATHER_RADIUS
                * LEGACY_HOLE_FEATHER_RADIUS;

        for (int x = minX; x <= maxX; x++) {
            float worldX = originX + (x + 0.5f) * tileSize;
            for (int y = minY; y <= maxY; y++) {
                float worldY = originY + (y + 0.5f) * tileSize;
                float dx = worldX - getHoleCenterX();
                float dy = worldY - getHoleCenterY();
                float distanceSquared = dx * dx + dy * dy;
                if (distanceSquared <= innerSquared
                        || distanceSquared > outerSquared) {
                    continue;
                }

                long hash = ((long) x * 73856093L)
                        ^ ((long) y * 19349663L)
                        ^ 0x5A17C9E3L;
                float unit = (hash & 0xffffL) / 65535f;
                float angle = (float) Math.atan2(dy, dx)
                        + (unit - 0.5f) * 0.42f;
                float sourceRadius = LEGACY_HOLE_FEATHER_RADIUS
                        + tileSize * (1.5f + unit * 4.5f);
                float sourceX = getHoleCenterX()
                        + (float) Math.cos(angle) * sourceRadius;
                float sourceY = getHoleCenterY()
                        + (float) Math.sin(angle) * sourceRadius;
                int sourceTileX = (int) ((sourceX - originX) / tileSize);
                int sourceTileY = (int) ((sourceY - originY) / tileSize);
                if (sourceTileX < 0 || sourceTileX >= tiles.length
                        || sourceTileY < 0
                        || sourceTileY >= tiles[0].length) {
                    continue;
                }
                tiles[x][y] = tiles[sourceTileX][sourceTileY];
            }
        }
        plugin.forceClearSampleCache();
    }

    private static StarSystemAPI findSystem(String id) {
        StarSystemAPI candidate = findSystemClaim(id);
        return isOwnedSystem(candidate, id) ? candidate : null;
    }

    private static StarSystemAPI findSystemClaim(String id) {
        for (StarSystemAPI candidate : Global.getSector().getStarSystems()) {
            if (id.equals(candidate.getOptionalUniqueId())) return candidate;
        }
        return null;
    }

    /** Stable lookup used by campaign population scripts and save repair. */
    public static StarSystemAPI findSystemById(String id) {
        return findSystem(id);
    }

    /** Stable lookup for one of Devoured Reach's three hollow worlds. */
    public static SectorEntityToken getDevouredWorld(int index) {
        if (index < 0 || index >= HOLLOW_WORLD_IDS.length) return null;
        StarSystemAPI reach = findSystem(DEVOURED_REACH_ID);
        if (reach == null) return null;
        SectorEntityToken world = reach.getEntityById(HOLLOW_WORLD_IDS[index]);
        return world != null
                && HOLLOW_WORLD_TYPES[index].equals(world.getCustomEntityType())
                ? world : null;
    }

    /** Stable destination lookup for the post-Troy expedition objective. */
    public static SectorEntityToken getFobIthaca() {
        StarSystemAPI ashenVerge = findSystem(ASHEN_VERGE_ID);
        if (ashenVerge == null) return null;
        SectorEntityToken ithaca = ashenVerge.getEntityById(FOB_ITHACA_ID);
        return isFobIthaca(ithaca) ? ithaca : null;
    }

    /** True only for Chief Navigator's authored FOB Ithaca center. */
    public static boolean isFobIthaca(SectorEntityToken entity) {
        return entity != null && FOB_ITHACA_ID.equals(entity.getId())
                && FOB_ITHACA_TYPE.equals(entity.getCustomEntityType())
                && entity.getMemoryWithoutUpdate() != null
                && entity.getMemoryWithoutUpdate().getBoolean(
                        FOB_ITHACA_OWNER_MARKER);
    }

    /** True only for Chief Navigator's authored Ithaca gate. */
    public static boolean isFobIthacaGate(SectorEntityToken entity) {
        if (entity == null || !FOB_ITHACA_GATE_ID.equals(entity.getId())
                || !Entities.INACTIVE_GATE.equals(entity.getCustomEntityType())
                || entity.getMemoryWithoutUpdate() == null) return false;
        if (entity.getMemoryWithoutUpdate().getBoolean(
                FOB_ITHACA_GATE_MARKER)) return true;
        if (!(entity.getContainingLocation() instanceof StarSystemAPI)) {
            return false;
        }
        StarSystemAPI system = (StarSystemAPI) entity.getContainingLocation();
        return isOwnedSystem(system, ASHEN_VERGE_ID)
                && isFobIthaca(system.getEntityById(FOB_ITHACA_ID));
    }

    /** True only for Chief Navigator's authored Foxtrot Terminus. */
    public static boolean isEntry(SectorEntityToken entity) {
        if (!(entity instanceof JumpPointAPI)
                || !ENTRY_ID.equals(entity.getId())
                || !(entity.getContainingLocation() instanceof StarSystemAPI)) {
            return false;
        }
        return isOwnedSystem(
                (StarSystemAPI) entity.getContainingLocation(), SYSTEM_ID);
    }

    /** True only for Chief Navigator's authored Devoured Ring. */
    public static boolean isDevouredRing(SectorEntityToken entity) {
        return entity != null && DEVOURED_RING_ID.equals(entity.getId())
                && DEVOURED_RING_TYPE.equals(entity.getCustomEntityType());
    }

    /** True only for Last Light's authored recoverable Guardian wreck. */
    public static boolean isLastLightGuardianWreck(
            SectorEntityToken entity) {
        if (entity == null
                || !LAST_LIGHT_GUARDIAN_WRECK_ID.equals(entity.getId())
                || !(entity.getContainingLocation() instanceof StarSystemAPI)) {
            return false;
        }
        return isOwnedSystem(
                (StarSystemAPI) entity.getContainingLocation(),
                LAST_LIGHT_ID);
    }

    private static boolean isOwnedSystem(
            StarSystemAPI system, String expectedId) {
        if (system == null || expectedId == null
                || !expectedId.equals(system.getOptionalUniqueId())
                || system.getMemoryWithoutUpdate() == null) return false;
        if (system.getMemoryWithoutUpdate().getBoolean(
                AUTHORED_SYSTEM_MARKER)) return true;
        if (SYSTEM_ID.equals(expectedId)) {
            return system.getMemoryWithoutUpdate().getBoolean(
                    GENERATED_NEBULA);
        }
        if (MESSINA_ID.equals(expectedId)) {
            return system.getMemoryWithoutUpdate().getBoolean(
                    GENERATED_MESSINA);
        }
        if (SILENT_WAKE_ID.equals(expectedId)) {
            return system.getMemoryWithoutUpdate().getBoolean(
                    GENERATED_SILENT_WAKE);
        }
        if (ASHEN_VERGE_ID.equals(expectedId)) {
            return system.getMemoryWithoutUpdate().getBoolean(
                    GENERATED_ASHEN_VERGE);
        }
        if (PALE_REACH_ID.equals(expectedId)
                || LAST_LIGHT_ID.equals(expectedId)) {
            return system.getMemoryWithoutUpdate().getBoolean(
                    GENERATED_WHITE_DWARF);
        }
        if (DEVOURED_REACH_ID.equals(expectedId)) {
            return isDevouredRing(system.getEntityById(DEVOURED_RING_ID));
        }
        return false;
    }

    private static Vector2f getExpanseLocation() {
        Vector2f center = getInitialHoleCenter();
        return new Vector2f(
                center.x + ENTRY_OFFSET_X,
                center.y + ENTRY_OFFSET_Y);
    }

    private static float getHoleCenterX() {
        return getHoleCenter().x;
    }

    private static float getHoleCenterY() {
        return getHoleCenter().y;
    }

    /** Positions Alpha Odyssey beyond the vanilla map in the remote southeast. */
    private static Vector2f getHoleCenter() {
        // A saved pocket belongs to its saved system coordinates. Map mods may
        // change global dimensions between sessions; do not move the boundary
        // away from the already serialized worlds, terrain, and jump points.
        StarSystemAPI existing = Global.getSector() == null ? null : findExisting();
        if (existing != null) {
            return new Vector2f(existing.getLocation().x - ENTRY_OFFSET_X,
                    existing.getLocation().y - ENTRY_OFFSET_Y);
        }
        return getInitialHoleCenter();
    }

    private static Vector2f getInitialHoleCenter() {
        return new Vector2f(
                Global.getSettings().getFloat("sectorWidth")
                        * REMOTE_HOLE_X_FACTOR,
                Global.getSettings().getFloat("sectorHeight")
                        * REMOTE_HOLE_Y_FACTOR);
    }

    /** Defensive copy of the Alpha Odyssey hyperspace-pocket center. */
    public static Vector2f getOdysseyHoleCenter() {
        return getHoleCenter();
    }

    /** Includes the normal-space cavity and its visible Abyssal transition. */
    public static boolean isInOdysseyHyperspace(Vector2f location) {
        if (location == null) return false;
        float dx = location.x - getHoleCenterX();
        float dy = location.y - getHoleCenterY();
        float huntRadius = HOLE_FEATHER_RADIUS + 2500f;
        return dx * dx + dy * dy <= huntRadius * huntRadius;
    }

    /** Returns the nearest allowed point inside the Odyssey hyperspace pocket. */
    public static Vector2f clampToOdysseyHyperspace(
            Vector2f location, float boundaryInset) {
        Vector2f center = getHoleCenter();
        if (location == null) return center;
        float radius = Math.max(1f,
                HOLE_FEATHER_RADIUS + 2500f - Math.max(0f, boundaryInset));
        float dx = location.x - center.x;
        float dy = location.y - center.y;
        float distanceSquared = dx * dx + dy * dy;
        if (distanceSquared <= radius * radius) {
            return new Vector2f(location);
        }
        float distance = (float) Math.sqrt(distanceSquared);
        if (distance <= 0.0001f) return center;
        float scale = radius / distance;
        return new Vector2f(center.x + dx * scale, center.y + dy * scale);
    }

    static void ensureShowcaseEncounters(
            StarSystemAPI system,
            StarSystemAPI devouredReach) {
        DriftingWallEncounter.ensureExists(system);
        MenelausTrial.ensureWorld(system);
        StarSystemAPI lastLight = findSystem(LAST_LIGHT_ID);
        if (lastLight != null) {
            TroyArrivalScript.retireTimeFreezeFacetSample();
            if (MenelausTrial.isComplete()) {
                SectorEntityToken guardianWreck = lastLight.getEntityById(
                        LAST_LIGHT_GUARDIAN_WRECK_ID);
                TroyArrivalScript.spawnStarvingThreatSample(
                        lastLight,
                        guardianWreck == null
                                ? lastLight.getCenter() : guardianWreck);
            }
        }

        if (devouredReach == null || MenelausTrial.isComplete()) return;
        SectorEntityToken messinaAnchor = devouredReach.getEntityById(
                MESSINA_SHOWCASE_ANCHOR_ID);
        if (messinaAnchor == null) {
            messinaAnchor = devouredReach.createToken(0f, 0f);
            messinaAnchor.setId(MESSINA_SHOWCASE_ANCHOR_ID);
        }
        TroyArrivalScript.spawnStarvingThreatSample(
                devouredReach, messinaAnchor);
    }

    /** Removes the obsolete standalone Spartan test encounter from old saves. */
    static void retireTaskForceSpartanEncounter() {
        retireTaskForceSpartanEncounter(findExisting());
    }

    private static void retireTaskForceSpartanEncounter(
            StarSystemAPI system) {
        boolean retired = TroyArrivalScript.retireTaskForceSpartanSample();
        if (retired && system != null) {
            SectorEntityToken anchor = system.getEntityById(
                    SHOWCASE_ANCHOR_ID);
            if (anchor != null) system.removeEntity(anchor);
        }
    }

    /** Compatibility entry point retained for obsolete saved prototype rooms. */
    static void ensureShowcaseEncounters(StarSystemAPI system) {
        ensureShowcaseEncounters(system, findSystem(DEVOURED_REACH_ID));
    }
}
