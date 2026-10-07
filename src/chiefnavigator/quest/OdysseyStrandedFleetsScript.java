package chiefnavigator.quest;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.util.IntervalUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;

/**
 * Populates Silent Wake with stranded human fleets and stages the delayed
 * Hegemony expedition in Alpha Odyssey.
 */
public final class OdysseyStrandedFleetsScript implements EveryFrameScript {
    public static final String VOSS_PERSON_ID =
            "chief_navigator_admiral_voss";
    private static final String VOSS_OWNER_MARKER =
            "$chief_navigator_admiral_voss_owned_v1";
    private static final String VOSS_CREATION_LATCH =
            "$chief_navigator_admiral_voss_created_v1";
    public static final String FIRST_ENTRY_TIMESTAMP =
            "$chief_navigator_odyssey_first_entry_timestamp";

    public static final String SILENT_WAKE_LEAGUE_MARKER =
            "$chief_navigator_stranded_league_fleet";
    private static final String SILENT_WAKE_LEAGUE_SPAWNED =
            "$chief_navigator_silent_wake_league_survivors_spawned";
    private static final String SILENT_WAKE_LEAGUE_FLEET_ID =
            "chief_navigator_silent_wake_league_survivors";
    private static final String SILENT_WAKE_LEAGUE_COMPOSITION_V1 =
            "$chief_navigator_silent_wake_league_survivors_v1";
    private static final String SILENT_WAKE_LEAGUE_CONFIRMED =
            "$chief_navigator_silent_wake_league_confirmed";
    static final String SILENT_WAKE_LEAGUE_PICKUP_RECORDED =
            "$chief_navigator_silent_wake_league_rescue_spawned";
    private static final String SILENT_WAKE_LEAGUE_DEFEATED_V1 =
            "$chief_navigator_silent_wake_league_defeated_v1";
    private static final String SILENT_WAKE_LEAGUE_LAST_X =
            "$chief_navigator_silent_wake_league_last_x";
    private static final String SILENT_WAKE_LEAGUE_LAST_Y =
            "$chief_navigator_silent_wake_league_last_y";
    private static final String SILENT_WAKE_LEAGUE_INVALID_V1 =
            "$chief_navigator_silent_wake_league_invalid_v1";
    private static final String SANZU_EXPLORER_MARKER =
            "$chief_navigator_sanzu_persean_explorer";
    private static final String SANZU_EXPLORER_NEXT_SPAWN_DAY =
            "$chief_navigator_sanzu_persean_next_spawn_day";
    private static final String SANZU_EXPLORER_SERIAL =
            "$chief_navigator_sanzu_persean_serial";
    private static final String SANZU_EXPLORER_ID_PREFIX =
            "chief_navigator_sanzu_persean_explorer_";
    public static final String SANZU_EXILE_MARKER =
            "$chief_navigator_sanzu_hegemony_exile";
    private static final String SANZU_EXILE_AGGRESSIVE =
            "$chief_navigator_sanzu_hegemony_exile_aggressive";
    private static final String SANZU_EXILE_NEXT_SPAWN_DAY =
            "$chief_navigator_sanzu_hegemony_exile_next_spawn_day";
    private static final String SANZU_EXILE_SERIAL =
            "$chief_navigator_sanzu_hegemony_exile_serial";
    private static final String SANZU_EXILE_ID_PREFIX =
            "chief_navigator_sanzu_hegemony_exile_";
    private static final String HEGEMONY_ARRIVED =
            "$chief_navigator_hegemony_odyssey_arrived";
    private static final String HEGEMONY_ITHACA_SCENE_SHOWN =
            "$chief_navigator_hegemony_ithaca_command_scene_shown";
    private static final String HEGEMONY_FLEET_ID =
            "chief_navigator_hegemony_odyssey_expedition";
    private static final String HEGEMONY_TROY_PATROL_ID =
            "chief_navigator_hegemony_troy_patrol";
    private static final String[] HEGEMONY_TROY_PATROL_IDS = {
        HEGEMONY_TROY_PATROL_ID,
        HEGEMONY_TROY_PATROL_ID + "_2",
        HEGEMONY_TROY_PATROL_ID + "_3"
    };
    private static final String HEGEMONY_TROY_PATROL_MARKER =
            "$chief_navigator_hegemony_troy_patrol";
    public static final String HEGEMONY_MARKER =
            "$chief_navigator_hegemony_odyssey_expedition";
    private static final String HEGEMONY_COMPOSITION_V3 =
            "$chief_navigator_hegemony_odyssey_battered_v3";
    private static final String HEGEMONY_CANONICAL_ID_BLOCKED_V1 =
            "$chief_navigator_hegemony_odyssey_id_blocked_v1";
    public static final String HEGEMONY_TRIBUTE_GRACE =
            "$chief_navigator_hegemony_odyssey_tribute_grace";

    public static final float HEGEMONY_DEMAND_SUPPLIES = 60f;
    public static final float HEGEMONY_DEMAND_FUEL = 120f;
    public static final float HEGEMONY_TRIBUTE_GRACE_DAYS = 14f;

    private static final int MAX_SANZU_EXPLORERS = 2;
    private static final int MAX_SANZU_EXILES = 2;
    private static final float SANZU_PATROL_RESPAWN_MIN_DAYS = 18f;
    private static final float SANZU_PATROL_RESPAWN_RANDOM_DAYS = 14f;
    private static final float EXILE_POSTURE_MIN_DAYS = 4f;
    private static final float EXILE_POSTURE_RANDOM_DAYS = 5f;

    private static final String[] SANZU_EXPLORER_ESCORTS = {
        "vigilance_Standard",
        "centurion_Assault",
        "wolf_Assault",
        "shrike_Attack"
    };
    private static final String[] SANZU_EXPLORER_SUPPORT = {
        "shepherd_Frontier",
        "wayfarer_Standard",
        "buffalo_Standard"
    };
    private static final String[] SANZU_EXILE_FRIGATES = {
        "lasher_Standard",
        "wolf_hegemony_Assault",
        "centurion_Assault",
        "vigilance_Standard"
    };

    private final IntervalUtil interval = new IntervalUtil(0.8f, 1.2f);
    private final Random random = new Random();

    @Override
    public void advance(float amount) {
        if (Global.getSector() == null) return;
        interval.advance(amount);
        if (!interval.intervalElapsed()) return;

        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        initializeFirstEntry(memory);
        if (!memory.contains(FIRST_ENTRY_TIMESTAMP)) return;

        long firstEntry = memory.getLong(FIRST_ENTRY_TIMESTAMP);
        float elapsedDays = Global.getSector().getClock()
                .getElapsedDaysSince(firstEntry);
        ensureHegemonyExpedition(memory, elapsedDays);
        maintainHegemonyInfrastructure(memory);
        maintainSanzuPatrols(memory, elapsedDays);
        maybeShowIthacaCommandScene(memory);
    }

    /** Called by explicit Odyssey entrances to start the human arrival timeline. */
    public static void markFirstEntry() {
        if (Global.getSector() == null) return;
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        if (!memory.contains(FIRST_ENTRY_TIMESTAMP)) {
            memory.set(
                    FIRST_ENTRY_TIMESTAMP,
                    Global.getSector().getClock().getTimestamp());
        }
    }

    private static void initializeFirstEntry(MemoryAPI memory) {
        if (memory.contains(FIRST_ENTRY_TIMESTAMP)) return;
        if (memory.getBoolean(OdysseyExpanseSystem.ENTERED)
                || isPlayerInsideOdysseySystem()) {
            markFirstEntry();
        }
    }

    private static boolean isPlayerInsideOdysseySystem() {
        if (Global.getSector().getPlayerFleet() == null
                || !(Global.getSector().getPlayerFleet()
                        .getContainingLocation() instanceof StarSystemAPI)) {
            return false;
        }
        StarSystemAPI current = (StarSystemAPI) Global.getSector()
                .getPlayerFleet().getContainingLocation();
        return OdysseyExpanseSystem.isInside(current);
    }

    /**
     * Plays the expedition's attempted seizure of Ithaca once. The scene waits
     * for an uncluttered campaign frame instead of interrupting another dialog.
     */
    private static void maybeShowIthacaCommandScene(MemoryAPI memory) {
        if (!isHegemonyArrivalUnlocked()
                || !memory.getBoolean(HEGEMONY_ARRIVED)
                || memory.getBoolean(HEGEMONY_ITHACA_SCENE_SHOWN)
                || !isPlayerInsideOdysseySystem()
                || Global.getSector().getPlayerFleet() == null
                || Global.getSector().getCampaignUI()
                        .getCurrentInteractionDialog() != null) {
            return;
        }

        boolean opened = Global.getSector().getCampaignUI()
                .showInteractionDialog(
                        new HegemonyIthacaCommandScene(),
                        Global.getSector().getPlayerFleet());
        if (opened) {
            memory.set(HEGEMONY_ITHACA_SCENE_SHOWN, true);
        }
    }

    private void maintainSanzuPatrols(
            MemoryAPI memory,
            float elapsedDays) {
        StarSystemAPI sanzu = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.SILENT_WAKE_ID);
        if (sanzu == null) return;

        ensureSilentWakeLeagueSurvivors(sanzu, memory);

        int explorers = countAndConfigureExplorers(sanzu);
        if (!memory.contains(SANZU_EXPLORER_NEXT_SPAWN_DAY)) {
            memory.set(SANZU_EXPLORER_NEXT_SPAWN_DAY, elapsedDays);
        }
        if (explorers < MAX_SANZU_EXPLORERS
                && elapsedDays >= memory.getFloat(
                        SANZU_EXPLORER_NEXT_SPAWN_DAY)
                && spawnSanzuExplorer(sanzu, memory)) {
            scheduleNextSpawn(
                    memory, SANZU_EXPLORER_NEXT_SPAWN_DAY, elapsedDays);
        }

        if (!memory.getBoolean(HEGEMONY_ARRIVED)) {
            memory.unset(SANZU_EXILE_NEXT_SPAWN_DAY);
            return;
        }

        int exiles = countAndConfigureExiles(sanzu);
        if (!memory.contains(SANZU_EXILE_NEXT_SPAWN_DAY)) {
            memory.set(SANZU_EXILE_NEXT_SPAWN_DAY, elapsedDays);
        }
        if (exiles < MAX_SANZU_EXILES
                && elapsedDays >= memory.getFloat(
                        SANZU_EXILE_NEXT_SPAWN_DAY)
                && spawnSanzuExile(sanzu, memory)) {
            scheduleNextSpawn(
                    memory, SANZU_EXILE_NEXT_SPAWN_DAY, elapsedDays);
        }
    }

    /**
     * Maintains one authored League survivor fleet. The durable spawned flag
     * deliberately prevents an isolated fleet from reappearing after defeat.
     */
    private void ensureSilentWakeLeagueSurvivors(
            StarSystemAPI silentWake,
            MemoryAPI sectorMemory) {
        boolean arrivalUnlocked = isPerseanArrivalUnlocked();
        CampaignFleetAPI existing = null;
        SectorEntityToken byId = silentWake.getEntityById(
                SILENT_WAKE_LEAGUE_FLEET_ID);
        if (byId != null && (!(byId instanceof CampaignFleetAPI)
                || isPlayerControlledFleet((CampaignFleetAPI) byId)
                || !byId.getMemoryWithoutUpdate().getBoolean(
                        SILENT_WAKE_LEAGUE_MARKER))) {
            if (!sectorMemory.getBoolean(SILENT_WAKE_LEAGUE_INVALID_V1)) {
                Global.getLogger(OdysseyStrandedFleetsScript.class).error(
                        "Silent Wake survivor fleet ID contains "
                                + "incompatible serialized state; refusing "
                                + "replacement");
                sectorMemory.set(SILENT_WAKE_LEAGUE_INVALID_V1, true);
            }
            sectorMemory.set(SILENT_WAKE_LEAGUE_SPAWNED, true);
            return;
        }
        if (byId instanceof CampaignFleetAPI) {
            existing = (CampaignFleetAPI) byId;
        }
        if (existing == null) {
            for (CampaignFleetAPI fleet : new ArrayList<CampaignFleetAPI>(
                    silentWake.getFleets())) {
                if (isPlayerControlledFleet(fleet)) continue;
                if (fleet.getMemoryWithoutUpdate().getBoolean(
                        SILENT_WAKE_LEAGUE_MARKER)) {
                    existing = fleet;
                    break;
                }
            }
        }

        if (existing != null && !existing.isEmpty()) {
            sectorMemory.set(SILENT_WAKE_LEAGUE_SPAWNED, true);
            sectorMemory.set(SILENT_WAKE_LEAGUE_CONFIRMED, true);
            sectorMemory.set(SILENT_WAKE_LEAGUE_LAST_X,
                    existing.getLocation().x);
            sectorMemory.set(SILENT_WAKE_LEAGUE_LAST_Y,
                    existing.getLocation().y);
            configureSilentWakeLeagueSurvivors(existing, silentWake);
            return;
        }
        if (existing != null) {
            if (TroyArrivalScript.isFleetBusyForMutation(existing)) return;
            sectorMemory.set(SILENT_WAKE_LEAGUE_DEFEATED_V1, true);
            silentWake.removeEntity(existing);
        }
        // A player victory queues the rules-authored rescue decision. Merely
        // finding an empty or missing fleet must never embark its personnel.
        // Existing canonical fleets remain in old saves, but a new campaign
        // does not receive this formation until Labor I is complete.
        if (!arrivalUnlocked) return;
        if (sectorMemory.getBoolean(SILENT_WAKE_LEAGUE_SPAWNED)) return;

        CampaignFleetAPI fleet = createSilentWakeLeagueSurvivors();
        if (fleet == null || fleet.isEmpty()) return;
        fleet.setId(SILENT_WAKE_LEAGUE_FLEET_ID);
        fleet.setName("Persean League Survivors");
        fleet.setNoFactionInName(true);
        fleet.setTransponderOn(true);
        fleet.getMemoryWithoutUpdate().set(
                SILENT_WAKE_LEAGUE_MARKER, true);
        fleet.getMemoryWithoutUpdate().set(
                SILENT_WAKE_LEAGUE_COMPOSITION_V1, true);
        silentWake.addEntity(fleet);
        placeSanzuPatrol(fleet);
        configureSilentWakeLeagueSurvivors(fleet, silentWake);
        sectorMemory.set(SILENT_WAKE_LEAGUE_SPAWNED, true);
    }

    static boolean isLeagueSurvivorFleet(CampaignFleetAPI fleet) {
        return fleet != null && !isPlayerControlledFleet(fleet)
                && (SILENT_WAKE_LEAGUE_FLEET_ID.equals(fleet.getId())
                    || fleet.getMemoryWithoutUpdate().getBoolean(
                            SILENT_WAKE_LEAGUE_MARKER));
    }

    private CampaignFleetAPI createSilentWakeLeagueSurvivors() {
        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(
                Factions.PERSEAN, "Persean League Survivors", true);

        FleetMemberAPI pegasus = addBatteredLeagueMember(
                fleet, "pegasus_Strike", 1, 0.48f, 0.58f);
        addBatteredLeagueMember(fleet, "champion_Elite", 2, 0.38f, 0.52f);
        addBatteredLeagueMember(fleet, "eagle_Assault", 2, 0.36f, 0.50f);
        addBatteredLeagueMember(fleet, "eagle_Balanced", 2, 0.36f, 0.50f);
        addBatteredLeagueMember(fleet, "gryphon_Standard", 2, 0.35f, 0.49f);
        addBatteredLeagueMember(fleet, "falcon_Attack", 2, 0.34f, 0.48f);
        addBatteredLeagueMember(fleet, "falcon_CS", 2, 0.34f, 0.48f);
        addBatteredLeagueMember(fleet, "eradicator_Assault", 2, 0.34f, 0.48f);
        addBatteredLeagueMember(fleet, "eradicator_Support", 2, 0.34f, 0.48f);
        for (int index = 0; index < 3; index++) {
            addBatteredLeagueMember(
                    fleet, "hammerhead_Balanced", 2, 0.32f, 0.46f);
        }
        for (int index = 0; index < 2; index++) {
            addBatteredLeagueMember(fleet, "sunder_CS", 2, 0.32f, 0.46f);
        }
        for (int index = 0; index < 4; index++) {
            addBatteredLeagueMember(fleet, "wolf_Assault", 1, 0.30f, 0.44f);
        }

        fleet.getFleetData().setFlagship(pegasus);
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
        fleet.updateCounts();
        return fleet;
    }

    private FleetMemberAPI addBatteredLeagueMember(
            CampaignFleetAPI fleet,
            String variantId,
            int dMods,
            float minimumCR,
            float maximumCR) {
        FleetMemberAPI member = Global.getFactory().createFleetMember(
                FleetMemberType.SHIP, variantId);
        fleet.getFleetData().addFleetMember(member);
        DModManager.addDMods(member, true, dMods, random);
        DModManager.setDHull(member.getVariant());
        float damagedCR = minimumCR
                + random.nextFloat() * (maximumCR - minimumCR);
        member.getRepairTracker().setCR(Math.min(
                member.getRepairTracker().getMaxCR(), damagedCR));
        return member;
    }

    private static void configureSilentWakeLeagueSurvivors(
            CampaignFleetAPI fleet,
            StarSystemAPI silentWake) {
        if (TroyArrivalScript.isFleetBusyForMutation(fleet)) return;
        EncounterCombatChatter.configure(fleet, EncounterCombatChatter.LEAGUE);
        fleet.setFaction(Factions.PERSEAN, true);
        fleet.setNoAutoDespawn(true);
        fleet.setAbortDespawn(true);
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        memory.set(SILENT_WAKE_LEAGUE_MARKER, true);
        memory.set(SILENT_WAKE_LEAGUE_COMPOSITION_V1, true);
        memory.set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        memory.set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
        memory.set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        memory.set(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE, true);
        memory.set(MemFlags.MEMORY_KEY_NO_JUMP, true);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE);
        memory.unset(MemFlags.NON_HOSTILE_OVERRIDES_MAKE_HOSTILE);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE);
        memory.unset(MemFlags.MEMORY_KEY_AVOID_PLAYER_SLOWLY);

        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player != null
                && player.getContainingLocation() == silentWake) {
            ensureHegemonyAssignment(
                    fleet,
                    FleetAssignment.INTERCEPT,
                    player,
                    "intercepting intruders in Silent Wake");
        } else {
            ensureHegemonyAssignment(
                    fleet,
                    FleetAssignment.PATROL_SYSTEM,
                    silentWake.getCenter(),
                    "holding damaged formation in Sanzu");
        }
    }

    private int countAndConfigureExplorers(StarSystemAPI sanzu) {
        int count = 0;
        for (CampaignFleetAPI fleet : new ArrayList<CampaignFleetAPI>(
                sanzu.getFleets())) {
            if (isPlayerControlledFleet(fleet)) continue;
            if (!fleet.getMemoryWithoutUpdate().getBoolean(
                    SANZU_EXPLORER_MARKER)) {
                continue;
            }
            if (TroyArrivalScript.isFleetBusyForMutation(fleet)) {
                count++;
                continue;
            }
            if (fleet.isEmpty()) {
                sanzu.removeEntity(fleet);
                continue;
            }
            count++;
            if (count <= MAX_SANZU_EXPLORERS) {
                configureSanzuExplorer(fleet, sanzu);
            }
        }
        return count;
    }

    private int countAndConfigureExiles(StarSystemAPI sanzu) {
        int count = 0;
        for (CampaignFleetAPI fleet : new ArrayList<CampaignFleetAPI>(
                sanzu.getFleets())) {
            if (isPlayerControlledFleet(fleet)) continue;
            if (!fleet.getMemoryWithoutUpdate().getBoolean(
                    SANZU_EXILE_MARKER)) {
                continue;
            }
            if (TroyArrivalScript.isFleetBusyForMutation(fleet)) {
                count++;
                continue;
            }
            if (fleet.isEmpty()) {
                sanzu.removeEntity(fleet);
                continue;
            }
            count++;
            if (count <= MAX_SANZU_EXILES) {
                configureSanzuExile(fleet, sanzu);
            }
        }
        return count;
    }

    private boolean spawnSanzuExplorer(
            StarSystemAPI sanzu,
            MemoryAPI memory) {
        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(
                Factions.PERSEAN, "Persean Exploration Patrol", true);
        FleetMemberAPI flagship = addReadyMember(
                fleet,
                SANZU_EXPLORER_ESCORTS[
                        random.nextInt(SANZU_EXPLORER_ESCORTS.length)]);
        addReadyMember(
                fleet,
                SANZU_EXPLORER_SUPPORT[
                        random.nextInt(SANZU_EXPLORER_SUPPORT.length)]);
        if (random.nextFloat() < 0.45f) {
            addReadyMember(
                    fleet,
                    SANZU_EXPLORER_ESCORTS[
                            random.nextInt(SANZU_EXPLORER_ESCORTS.length)]);
        }
        finishSmallFleet(fleet, flagship);
        fleet.setId(nextFleetId(
                sanzu,
                memory,
                SANZU_EXPLORER_SERIAL,
                SANZU_EXPLORER_ID_PREFIX));
        fleet.setName("Persean Exploration Patrol");
        fleet.setNoFactionInName(true);
        fleet.setTransponderOn(true);
        fleet.getMemoryWithoutUpdate().set(SANZU_EXPLORER_MARKER, true);
        sanzu.addEntity(fleet);
        placeSanzuPatrol(fleet);
        configureSanzuExplorer(fleet, sanzu);
        return true;
    }

    private boolean spawnSanzuExile(
            StarSystemAPI sanzu,
            MemoryAPI memory) {
        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(
                HegemonyExpeditionFaction.ID, "Hegemony Exile Patrol", true);
        FleetMemberAPI flagship = addBatteredExileMember(
                fleet,
                SANZU_EXILE_FRIGATES[
                        random.nextInt(SANZU_EXILE_FRIGATES.length)]);
        addBatteredExileMember(
                fleet,
                SANZU_EXILE_FRIGATES[
                        random.nextInt(SANZU_EXILE_FRIGATES.length)]);
        if (random.nextFloat() < 0.55f) {
            addBatteredExileMember(
                    fleet,
                    SANZU_EXILE_FRIGATES[
                            random.nextInt(SANZU_EXILE_FRIGATES.length)]);
        }
        if (random.nextFloat() < 0.3f) {
            flagship = addBatteredExileMember(fleet, "enforcer_Outdated");
        }
        finishSmallFleet(fleet, flagship);
        fleet.setId(nextFleetId(
                sanzu,
                memory,
                SANZU_EXILE_SERIAL,
                SANZU_EXILE_ID_PREFIX));
        fleet.setName("Hegemony Exile Patrol");
        fleet.setNoFactionInName(true);
        fleet.setTransponderOn(true);
        fleet.getMemoryWithoutUpdate().set(SANZU_EXILE_MARKER, true);
        sanzu.addEntity(fleet);
        placeSanzuPatrol(fleet);
        configureSanzuExile(fleet, sanzu);
        return true;
    }

    private static FleetMemberAPI addReadyMember(
            CampaignFleetAPI fleet,
            String variantId) {
        FleetMemberAPI member = Global.getFactory().createFleetMember(
                FleetMemberType.SHIP, variantId);
        fleet.getFleetData().addFleetMember(member);
        member.getRepairTracker().setCR(
                member.getRepairTracker().getMaxCR());
        return member;
    }

    private FleetMemberAPI addBatteredExileMember(
            CampaignFleetAPI fleet,
            String variantId) {
        FleetMemberAPI member = Global.getFactory().createFleetMember(
                FleetMemberType.SHIP, variantId);
        fleet.getFleetData().addFleetMember(member);
        DModManager.addDMods(member, true, 1 + random.nextInt(2), random);
        DModManager.setDHull(member.getVariant());
        float maxCR = member.getRepairTracker().getMaxCR();
        member.getRepairTracker().setCR(
                Math.min(maxCR, 0.32f + random.nextFloat() * 0.27f));
        return member;
    }

    private static void finishSmallFleet(
            CampaignFleetAPI fleet,
            FleetMemberAPI flagship) {
        if (flagship != null) fleet.getFleetData().setFlagship(flagship);
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
        fleet.updateCounts();
    }

    private void placeSanzuPatrol(CampaignFleetAPI fleet) {
        float angle = random.nextFloat() * 360f;
        float radius = 3800f + random.nextFloat() * 3800f;
        fleet.setLocation(
                (float) Math.cos(Math.toRadians(angle)) * radius,
                (float) Math.sin(Math.toRadians(angle)) * radius);
    }

    private static void configureSanzuExplorer(
            CampaignFleetAPI fleet,
            StarSystemAPI sanzu) {
        if (TroyArrivalScript.isFleetBusyForMutation(fleet)) return;
        fleet.setFaction(Factions.PERSEAN, true);
        fleet.setNoAutoDespawn(true);
        fleet.setAbortDespawn(true);
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        memory.set(SANZU_EXPLORER_MARKER, true);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_HOSTILE);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE);
        memory.unset(MemFlags.MEMORY_KEY_AVOID_PLAYER_SLOWLY);
        memory.set(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE, true);
        memory.set(MemFlags.NON_HOSTILE_OVERRIDES_MAKE_HOSTILE, true);
        memory.set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE, true);
        memory.set(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE, true);
        memory.set(MemFlags.MEMORY_KEY_NO_JUMP, true);
        if (fleet.getCurrentAssignment() == null) {
            fleet.addAssignment(
                    FleetAssignment.PATROL_SYSTEM,
                    sanzu.getCenter(),
                    1000000f,
                    "surveying Sanzu's comparatively safe reaches");
        }
    }

    private void configureSanzuExile(
            CampaignFleetAPI fleet,
            StarSystemAPI sanzu) {
        // Count aided fleets until they leave Sanzu, but never replace their trip.
        if (ExileSupplyAid.hasReceivedAid(fleet)
                || TroyArrivalScript.isFleetBusyForMutation(fleet)) return;
        fleet.setFaction(HegemonyExpeditionFaction.ID, true);
        fleet.setNoAutoDespawn(true);
        fleet.setAbortDespawn(true);
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        memory.set(SANZU_EXILE_MARKER, true);
        boolean changedPosture = false;
        if (!memory.contains(SANZU_EXILE_AGGRESSIVE)) {
            memory.set(
                    SANZU_EXILE_AGGRESSIVE,
                    random.nextFloat() < 0.45f,
                    EXILE_POSTURE_MIN_DAYS
                            + random.nextFloat() * EXILE_POSTURE_RANDOM_DAYS);
            changedPosture = true;
        }
        boolean aggressive = memory.getBoolean(SANZU_EXILE_AGGRESSIVE);
        memory.set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        memory.set(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE, true);
        memory.set(MemFlags.MEMORY_KEY_NO_JUMP, true);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE);
        if (aggressive) {
            memory.unset(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE);
            memory.unset(MemFlags.NON_HOSTILE_OVERRIDES_MAKE_HOSTILE);
            memory.unset(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE);
            memory.unset(MemFlags.MEMORY_KEY_AVOID_PLAYER_SLOWLY);
            memory.set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
            memory.set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        } else {
            memory.unset(MemFlags.MEMORY_KEY_MAKE_HOSTILE);
            memory.unset(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE);
            memory.set(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE, true);
            memory.set(MemFlags.NON_HOSTILE_OVERRIDES_MAKE_HOSTILE, true);
            memory.set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE, true);
            memory.set(MemFlags.MEMORY_KEY_AVOID_PLAYER_SLOWLY, true);
        }
        if (changedPosture || fleet.getCurrentAssignment() == null) {
            fleet.clearAssignments();
            fleet.addAssignment(
                    FleetAssignment.PATROL_SYSTEM,
                    sanzu.getCenter(),
                    1000000f,
                    aggressive
                            ? "seizing stores after their commander stripped theirs"
                            : "hiding after refusing a suicide patrol");
        }
    }

    private static String nextFleetId(
            StarSystemAPI system,
            MemoryAPI memory,
            String serialKey,
            String prefix) {
        int serial = memory.contains(serialKey)
                ? memory.getInt(serialKey) : 0;
        while (system.getEntityById(prefix + serial) != null) serial++;
        memory.set(serialKey, serial + 1);
        return prefix + serial;
    }

    private void scheduleNextSpawn(
            MemoryAPI memory,
            String key,
            float elapsedDays) {
        memory.set(
                key,
                elapsedDays + SANZU_PATROL_RESPAWN_MIN_DAYS
                        + random.nextFloat()
                                * SANZU_PATROL_RESPAWN_RANDOM_DAYS);
    }

    private void ensureHegemonyExpedition(
            MemoryAPI memory,
            float elapsedDays) {
        StarSystemAPI alpha = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.SYSTEM_ID);
        if (alpha == null) return;

        SectorEntityToken existing = alpha.getEntityById(HEGEMONY_FLEET_ID);
        if (existing != null) {
            CampaignFleetAPI fleet = existing instanceof CampaignFleetAPI
                    ? (CampaignFleetAPI) existing : null;
            if (fleet == null
                    || isPlayerControlledFleet(fleet)
                    || !fleet.getMemoryWithoutUpdate().getBoolean(
                            HEGEMONY_MARKER)) {
                if (!memory.getBoolean(HEGEMONY_CANONICAL_ID_BLOCKED_V1)) {
                    Global.getLogger(OdysseyStrandedFleetsScript.class).error(
                            "Hegemony expedition ID is claimed by "
                                    + "incompatible serialized state; "
                                    + "suppressing reconstruction");
                }
                memory.set(HEGEMONY_CANONICAL_ID_BLOCKED_V1, true);
                return;
            }
            memory.set(HEGEMONY_ARRIVED, true);
            SectorEntityToken camp = ensureHegemonyCampAnchor(alpha);
            if (TroyArrivalScript.isFleetBusyForMutation(fleet)) return;
            if (!fleet.isEmpty() && camp != null) {
                configureHegemonyCamp(fleet, camp);
            }
            return;
        }
        if (memory.getBoolean(HEGEMONY_ARRIVED)
                || !isHegemonyArrivalUnlocked()) {
            return;
        }

        SectorEntityToken camp = ensureHegemonyCampAnchor(alpha);
        if (camp == null) return;
        CampaignFleetAPI fleet = createBatteredHegemonyExpedition();
        if (fleet == null || fleet.isEmpty()) return;

        fleet.setId(HEGEMONY_FLEET_ID);
        fleet.setName("Hegemony Odyssey Expedition");
        fleet.setNoFactionInName(true);
        fleet.setTransponderOn(true);
        fleet.getMemoryWithoutUpdate().set(HEGEMONY_MARKER, true);
        fleet.getMemoryWithoutUpdate().set(HEGEMONY_COMPOSITION_V3, true);
        alpha.addEntity(fleet);
        fleet.setLocation(camp.getLocation().x - 900f, camp.getLocation().y);
        configureHegemonyCamp(fleet, camp);
        memory.set(HEGEMONY_ARRIVED, true);
        OdysseyExpanseSystem.configureHegemonyFobBattlestation(camp, true);

        Global.getSector().getCampaignUI().addMessage(
                "A major Hegemony expedition has arrived in Alpha Odyssey.",
                new Color(245, 205, 90));
    }

    static boolean isPerseanArrivalUnlocked() {
        return MenelausTrial.isAccepted()
                && MenelausTrial.isLaborReported(
                        MenelausTrial.LABOR_MOTHERSHIPS);
    }

    static boolean isHegemonyArrivalUnlocked() {
        return MenelausTrial.isAccepted()
                && MenelausTrial.isLaborReported(
                        MenelausTrial.LABOR_WALL);
    }

    /**
     * Authored composition: more logistics hulls than warships, with one
     * surviving Legion XIV at its center and an escort heavy on small hulls.
     */
    private CampaignFleetAPI createBatteredHegemonyExpedition() {
        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(
                HegemonyExpeditionFaction.ID,
                "Hegemony Odyssey Expedition",
                true);

        FleetMemberAPI legion = addBatteredHegemonyMember(
                fleet, "legion_xiv_Elite", 3);
        addLegionSMods(legion);

        addBatteredHegemonyMember(fleet, "dominator_XIV_Elite", 3);
        addBatteredHegemonyMember(fleet, "eagle_xiv_Elite", 3);
        addBatteredHegemonyMember(fleet, "falcon_xiv_Escort", 2);

        for (int i = 0; i < 4; i++) {
            addBatteredHegemonyMember(fleet, "enforcer_XIV_Elite", 3);
        }
        for (int i = 0; i < 3; i++) {
            addBatteredHegemonyMember(fleet, "vanguard_Attack", 2);
        }
        for (int i = 0; i < 2; i++) {
            addBatteredHegemonyMember(fleet, "wolf_hegemony_Assault", 2);
            addBatteredHegemonyMember(fleet, "lasher_Standard", 2);
        }
        addBatteredHegemonyMember(fleet, "centurion_Assault", 2);

        // Seventeen logistics ships versus sixteen combat ships. Most of the
        // column is transport capacity the expedition can no longer supply.
        addBatteredHegemonyMember(fleet, "atlas_Standard", 4);
        addBatteredHegemonyMember(fleet, "prometheus_Super", 4);
        for (int i = 0; i < 3; i++) {
            addBatteredHegemonyMember(fleet, "colossus_Standard", 3);
            addBatteredHegemonyMember(
                    fleet, "buffalo_hegemony_Standard", 3);
        }
        for (int i = 0; i < 4; i++) {
            addBatteredHegemonyMember(fleet, "phaeton_Standard", 3);
        }
        for (int i = 0; i < 2; i++) {
            addBatteredHegemonyMember(fleet, "tarsus_Standard", 3);
            addBatteredHegemonyMember(fleet, "nebula_Standard", 2);
        }
        addBatteredHegemonyMember(fleet, "dram_Light", 2);

        fleet.getFleetData().setFlagship(legion);
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
        fleet.updateCounts();
        return fleet;
    }

    /** Three built-in hullmods is the maximum player-equivalent S-mod set. */
    private static void addLegionSMods(FleetMemberAPI legion) {
        legion.getVariant().addPermaMod("heavyarmor", true);
        legion.getVariant().addPermaMod("armoredweapons", true);
        legion.getVariant().addPermaMod("expanded_deck_crew", true);
    }

    private FleetMemberAPI addBatteredHegemonyMember(
            CampaignFleetAPI fleet,
            String variantId,
            int dMods) {
        FleetMemberAPI member = Global.getFactory().createFleetMember(
                FleetMemberType.SHIP,
                variantId);
        fleet.getFleetData().addFleetMember(member);
        DModManager.addDMods(member, true, dMods, random);
        DModManager.setDHull(member.getVariant());
        float maxCR = member.getRepairTracker().getMaxCR();
        member.getRepairTracker().setCR(
                Math.min(maxCR, 0.38f + random.nextFloat() * 0.24f));
        return member;
    }

    private static SectorEntityToken ensureHegemonyCampAnchor(
            StarSystemAPI system) {
        return OdysseyExpanseSystem.findHegemonyFobBattlestation(system);
    }

    /**
     * Turns Alpha Odyssey's abandoned battlestation into the expedition FOB
     * and maintains its hostile route-security cordon at Foxtrot Terminus.
     */
    private void maintainHegemonyInfrastructure(
            MemoryAPI sectorMemory) {
        if (sectorMemory.getBoolean(
                HEGEMONY_CANONICAL_ID_BLOCKED_V1)) return;
        StarSystemAPI alpha = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.SYSTEM_ID);
        if (alpha == null) return;

        SectorEntityToken camp = ensureHegemonyCampAnchor(alpha);
        if (camp == null) return;
        boolean arrived = sectorMemory.getBoolean(HEGEMONY_ARRIVED);
        if (!arrived) {
            return;
        }

        for (CampaignFleetAPI candidate :
                new ArrayList<CampaignFleetAPI>(alpha.getFleets())) {
            if (isPlayerControlledFleet(candidate)) continue;
            if (!candidate.getMemoryWithoutUpdate().getBoolean(
                    HEGEMONY_TROY_PATROL_MARKER)) {
                continue;
            }
            if (candidate.isEmpty()) {
                if (TroyArrivalScript.isFleetBusyForMutation(candidate)) {
                    continue;
                }
                alpha.removeEntity(candidate);
            }
        }

        SectorEntityToken terminus = OdysseyExpanseSystem.findEntry(alpha);
        if (terminus == null) return;
        for (int i = 0; i < HEGEMONY_TROY_PATROL_IDS.length; i++) {
            String patrolId = HEGEMONY_TROY_PATROL_IDS[i];
            CampaignFleetAPI patrol = findFleetById(alpha, patrolId);
            if (patrol != null) {
                configureHegemonyTroyPatrol(patrol, terminus);
                continue;
            }

            // A wrong-type claim on an authored ID is authoritative serialized
            // state. Do not replace it from a maintenance timer.
            if (alpha.getEntityById(patrolId) != null) continue;

            patrol = createHegemonyTroyPatrol();
            if (patrol == null || patrol.isEmpty()) continue;
            patrol.setId(patrolId);
            patrol.setName("Foxtrot Terminus Patrol");
            patrol.setNoFactionInName(true);
            patrol.setTransponderOn(true);
            patrol.getMemoryWithoutUpdate().set(
                    HEGEMONY_TROY_PATROL_MARKER, true);
            alpha.addEntity(patrol);
            float angle = 120f * i - 35f;
            float radians = (float) Math.toRadians(angle);
            patrol.setLocation(
                    camp.getLocation().x + (float) Math.cos(radians) * 320f,
                    camp.getLocation().y + (float) Math.sin(radians) * 320f);
            configureHegemonyTroyPatrol(patrol, terminus);
        }
    }

    private static CampaignFleetAPI findFleetById(
            StarSystemAPI system,
            String fleetId) {
        for (CampaignFleetAPI fleet : system.getFleets()) {
            if (fleetId.equals(fleet.getId())) return fleet;
        }
        return null;
    }

    private static CampaignFleetAPI createHegemonyTroyPatrol() {
        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(
                HegemonyExpeditionFaction.ID,
                "Foxtrot Terminus Patrol",
                true);
        FleetMemberAPI flagship = addReadyMember(
                fleet, "enforcer_XIV_Elite");
        addReadyMember(fleet, "vanguard_Attack");
        addReadyMember(fleet, "wolf_hegemony_Assault");
        addReadyMember(fleet, "lasher_Standard");
        finishSmallFleet(fleet, flagship);
        return fleet;
    }

    private static void configureHegemonyTroyPatrol(
            CampaignFleetAPI fleet,
            SectorEntityToken terminus) {
        if (TroyArrivalScript.isFleetBusyForMutation(fleet)) return;
        fleet.setFaction(HegemonyExpeditionFaction.ID, true);
        fleet.setNoAutoDespawn(true);
        fleet.setAbortDespawn(true);
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        memory.set(HEGEMONY_TROY_PATROL_MARKER, true);
        memory.set(MemFlags.MEMORY_KEY_PATROL_FLEET, true);
        memory.set(MemFlags.MEMORY_KEY_NO_JUMP, true);
        memory.unset(MemFlags.MEMORY_KEY_NO_REP_IMPACT);
        memory.set(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE, true);
        memory.unset(MemFlags.MEMORY_KEY_AVOID_PLAYER_SLOWLY);
        MemoryAPI sectorMemory = Global.getSector() == null
                ? null : Global.getSector().getMemoryWithoutUpdate();
        boolean grace = !HegemonyExpeditionFaction.isHostile()
                && (memory.getBoolean(HEGEMONY_TRIBUTE_GRACE)
                || (sectorMemory != null && sectorMemory.getBoolean(
                        HEGEMONY_TRIBUTE_GRACE)));
        if (grace) {
            memory.unset(MemFlags.MEMORY_KEY_MAKE_HOSTILE);
            memory.unset(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE);
            memory.unset(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE);
            memory.set(
                    MemFlags.NON_HOSTILE_OVERRIDES_MAKE_HOSTILE, true);
            memory.set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE, true);
        } else {
            memory.unset(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE);
            memory.unset(MemFlags.NON_HOSTILE_OVERRIDES_MAKE_HOSTILE);
            memory.unset(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE);
            memory.set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
            memory.set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        }
        if (HegemonyExpeditionFaction.isHostile()) {
            memory.set(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE, true);
            CampaignFleetAPI player = Global.getSector().getPlayerFleet();
            if (player != null && player.getContainingLocation() == fleet.getContainingLocation()) {
                ensureHegemonyAssignment(fleet, FleetAssignment.INTERCEPT, player,
                        "engaging enemies of the expedition");
                return;
            }
        }
        if (terminus == null) return;
        if (fleet.getCurrentAssignment() != null
                && fleet.getCurrentAssignment().getTarget() == terminus) {
            return;
        }
        fleet.clearAssignments();
        fleet.addAssignment(
                FleetAssignment.GO_TO_LOCATION,
                terminus,
                1000f,
                "taking station at Foxtrot Terminus");
        fleet.addAssignment(
                FleetAssignment.ORBIT_AGGRESSIVE,
                terminus,
                1000000f,
                "guarding the route to Waypoint Troy");
    }

    private static void configureHegemonyCamp(
            CampaignFleetAPI fleet,
            SectorEntityToken camp) {
        if (TroyArrivalScript.isFleetBusyForMutation(fleet)) return;
        EncounterCombatChatter.configure(fleet, EncounterCombatChatter.HEGEMONY);
        fleet.setFaction(HegemonyExpeditionFaction.ID, true);
        fleet.setNoAutoDespawn(true);
        fleet.setAbortDespawn(true);
        fleet.getMemoryWithoutUpdate().set(HEGEMONY_MARKER, true);
        fleet.getMemoryWithoutUpdate().set(HEGEMONY_COMPOSITION_V3, true);
        configureHegemonyAdmiral(fleet);
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_NO_REP_IMPACT);

        boolean grace = !HegemonyExpeditionFaction.isHostile()
                && fleet.getMemoryWithoutUpdate().getBoolean(
                        HEGEMONY_TRIBUTE_GRACE);
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (grace) {
            fleet.getMemoryWithoutUpdate().unset(
                    MemFlags.MEMORY_KEY_MAKE_HOSTILE);
            fleet.getMemoryWithoutUpdate().unset(
                    MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE);
            fleet.getMemoryWithoutUpdate().unset(
                    MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE);
            fleet.getMemoryWithoutUpdate().set(
                    MemFlags.NON_HOSTILE_OVERRIDES_MAKE_HOSTILE, true);
            fleet.getMemoryWithoutUpdate().set(
                    MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE, true);
            ensureHegemonyAssignment(
                    fleet,
                    FleetAssignment.DEFEND_LOCATION,
                    camp,
                    "rationing your contribution at the expedition camp");
        } else {
            fleet.getMemoryWithoutUpdate().unset(
                    MemFlags.NON_HOSTILE_OVERRIDES_MAKE_HOSTILE);
            fleet.getMemoryWithoutUpdate().unset(
                    MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE);
            fleet.getMemoryWithoutUpdate().set(
                    MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
            fleet.getMemoryWithoutUpdate().set(
                    MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
            fleet.getMemoryWithoutUpdate().set(
                    MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE, true);
            if (player != null
                    && player.getContainingLocation()
                            == fleet.getContainingLocation()) {
                ensureHegemonyAssignment(
                        fleet,
                        FleetAssignment.INTERCEPT,
                        player,
                        "requisitioning supplies for the good of humanity");
            } else {
                ensureHegemonyAssignment(
                        fleet,
                        FleetAssignment.DEFEND_LOCATION,
                        camp,
                        "holding the Alpha Odyssey expedition camp");
            }
        }
    }

    private static void configureHegemonyAdmiral(CampaignFleetAPI fleet) {
        PersonAPI voss = getOrCreateVoss();
        if (voss != null) fleet.setCommander(voss);
    }

    private static void ensureHegemonyAssignment(
            CampaignFleetAPI fleet,
            FleetAssignment assignment,
            SectorEntityToken target,
            String actionText) {
        if (TroyArrivalScript.isFleetBusyForMutation(fleet)) return;
        if (target == null) return;
        if (fleet.getCurrentAssignment() != null
                && fleet.getCurrentAssignment().getAssignment() == assignment
                && fleet.getCurrentAssignment().getTarget() == target) {
            return;
        }
        fleet.clearAssignments();
        fleet.addAssignment(
                assignment,
                target,
                1000000f,
                actionText);
    }

    private static boolean isPlayerControlledFleet(CampaignFleetAPI fleet) {
        return fleet != null && (fleet.isPlayerFleet()
                || Global.getSector() != null
                        && fleet == Global.getSector().getPlayerFleet());
    }

    public static boolean isHegemonyExpedition(SectorEntityToken target) {
        return target instanceof CampaignFleetAPI
                && !isPlayerControlledFleet((CampaignFleetAPI) target)
                && (HEGEMONY_FLEET_ID.equals(target.getId())
                        && target.getMemoryWithoutUpdate().getBoolean(HEGEMONY_MARKER)
                    || target.getMemoryWithoutUpdate().getBoolean(HEGEMONY_TROY_PATROL_MARKER));
    }

    public static boolean isRescueEscortCandidate(SectorEntityToken target) {
        return target instanceof CampaignFleetAPI
                && !isPlayerControlledFleet((CampaignFleetAPI) target)
                && target.getMemoryWithoutUpdate().getBoolean(
                        SANZU_EXILE_MARKER);
    }

    /** Persistent identity shared by the Labor III conference and fleet. */
    public static PersonAPI getOrCreateVoss() {
        ImportantPeopleAPI people = Global.getSector().getImportantPeople();
        MemoryAPI sectorMemory = Global.getSector().getMemoryWithoutUpdate();
        PersonAPI voss = people.getPerson(VOSS_PERSON_ID);
        if (voss == null) {
            if (sectorMemory.getBoolean(VOSS_CREATION_LATCH)) return null;
            sectorMemory.set(VOSS_CREATION_LATCH, true);
            voss = Global.getFactory().createPerson();
            voss.setId(VOSS_PERSON_ID);
            voss.setName(new FullName(
                    "Oren", "Voss", FullName.Gender.MALE));
            voss.setGender(FullName.Gender.MALE);
            voss.setFaction(HegemonyExpeditionFaction.ID);
            voss.setRankId(Ranks.SPACE_ADMIRAL);
            voss.setPostId(Ranks.POST_FLEET_COMMANDER);
            voss.setImportance(PersonImportance.HIGH);
            voss.setPersonality(Personalities.AGGRESSIVE);
            voss.setPortraitSprite(Global.getSettings().getSpriteName(
                    "characters", "chief_navigator_voss"));
            voss.getMemoryWithoutUpdate().set(VOSS_OWNER_MARKER, true);
            people.addPerson(voss);
        } else if (!voss.getMemoryWithoutUpdate().getBoolean(
                VOSS_OWNER_MARKER)) {
            sectorMemory.set(VOSS_CREATION_LATCH, true);
            return null;
        } else {
            sectorMemory.set(VOSS_CREATION_LATCH, true);
        }
        voss.setFaction(HegemonyExpeditionFaction.ID);
        return voss;
    }

    /** Prevents the save-migration fallback from replaying the conference. */
    public static void markIthacaCommandSceneShown() {
        if (Global.getSector() != null) {
            Global.getSector().getMemoryWithoutUpdate().set(
                    HEGEMONY_ITHACA_SCENE_SHOWN, true);
        }
    }

    public static void grantHegemonyTributeGrace(CampaignFleetAPI fleet) {
        if (fleet == null) return;
        fleet.getMemoryWithoutUpdate().set(
                HEGEMONY_TRIBUTE_GRACE,
                true,
                HEGEMONY_TRIBUTE_GRACE_DAYS);
        if (Global.getSector() != null) {
            Global.getSector().getMemoryWithoutUpdate().set(
                    HEGEMONY_TRIBUTE_GRACE,
                    true,
                    HEGEMONY_TRIBUTE_GRACE_DAYS);
        }
        StarSystemAPI alpha = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.SYSTEM_ID);
        if (alpha != null) {
            configureHegemonyCamp(
                    fleet,
                    ensureHegemonyCampAnchor(alpha));
            SectorEntityToken terminus =
                    OdysseyExpanseSystem.findEntry(alpha);
            for (CampaignFleetAPI patrol : alpha.getFleets()) {
                if (!patrol.getMemoryWithoutUpdate().getBoolean(
                        HEGEMONY_TROY_PATROL_MARKER)) {
                    continue;
                }
                patrol.getMemoryWithoutUpdate().set(
                        HEGEMONY_TRIBUTE_GRACE,
                        true,
                        HEGEMONY_TRIBUTE_GRACE_DAYS);
                configureHegemonyTroyPatrol(patrol, terminus);
            }
        } else {
            fleet.clearAssignments();
        }
    }

    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return false; }
}
