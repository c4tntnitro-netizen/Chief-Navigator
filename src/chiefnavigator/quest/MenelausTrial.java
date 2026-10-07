package chiefnavigator.quest;

import chiefnavigator.campaign.DomainSecurityIFFAuthorization;
import chiefnavigator.weapons.IthacaResearchUpgrades;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;

/** Persistent campaign state for Menelaus's Labors. */
public final class MenelausTrial {
    public static final String PERSON_ID = "chief_navigator_menelaus";
    public static final String STRATEGOS_RANK_ID =
            "chief_navigator_strategos";
    public static final String STRATEGOS_POST_ID =
            "chief_navigator_strategic_intelligence";
    public static final String PERSON_OWNER_MARKER =
            "$chief_navigator_menelaus_owned_v1";
    private static final String PERSON_CREATION_LATCH =
            "$chief_navigator_menelaus_created_v1";
    public static final String ACCEPTED =
            "$chief_navigator_menelaus_trial_accepted";
    public static final String MOTHERSHIPS_CLEARED =
            "$chief_navigator_menelaus_motherships_cleared";
    public static final String GAUTAMA_DEFEATED =
            "$chief_navigator_menelaus_gautama_defeated";
    public static final String FINAL_LABOR_REVEALED =
            "$chief_navigator_menelaus_final_labor_revealed";
    public static final String FINAL_LABOR_BRIEFED =
            "$chief_navigator_menelaus_final_labor_briefed_v2";
    private static final String LEGACY_FINAL_LABOR_BRIEFED =
            "$chief_navigator_menelaus_final_labor_briefed";
    public static final String FINAL_DEBRIEF_COMPLETE =
            "$chief_navigator_menelaus_final_debrief_complete_v1";
    public static final String FINAL_LABOR_FAILED =
            "$chief_navigator_menelaus_final_labor_failed_v1";
    private static final String LABOR_REPORT_MIGRATION =
            "$chief_navigator_menelaus_labor_reports_migrated_v1";
    private static final String LABOR_I_REPORTED =
            "$chief_navigator_menelaus_labor_i_reported_v1";
    private static final String LABOR_II_REPORTED =
            "$chief_navigator_menelaus_labor_ii_reported_v1";
    private static final String LABOR_III_REPORTED =
            "$chief_navigator_menelaus_labor_iii_reported_v1";
    private static final String LABOR_IV_REPORTED =
            "$chief_navigator_menelaus_labor_iv_reported_v1";
    private static final String FINAL_LABOR_FORCED_FOR_TESTING =
            "$chief_navigator_menelaus_final_labor_forced_v1";
    public static final String WALL_FRIENDLY =
            "$chief_navigator_drifting_wall_friendly";
    private static final String WALL_CLEARED_EARLY =
            "$chief_navigator_drifting_wall_cleared_early_v1";
    private static final String WALL_EARLY_REACTION_ACKNOWLEDGED =
            "$chief_navigator_drifting_wall_early_reaction_v1";
    public static final String MOTHERSHIP_MARKER =
            "$chief_navigator_sealed_mothership_poi";
    public static final String ASHEN_ESCALATED =
            "$chief_navigator_menelaus_ashen_escalated";
    private static final String WORLD_INITIALIZED_V1 =
            "$chief_navigator_menelaus_world_initialized_v1";

    public static final int LABOR_NONE = 0;
    public static final int LABOR_WALL = 1;
    public static final int LABOR_RESEARCH = 2;
    public static final int LABOR_MOTHERSHIPS = 3;
    public static final int LABOR_GAUTAMA = 4;
    // Added after the original four-stage save format. Keep Gautama at 4 so
    // serialized Intel entries remain readable; sequence is derived below.
    public static final int LABOR_SENSOR = 5;
    public static final int LABOR_COMPLETE = 6;

    private static final String PORTRAIT_CATEGORY = "characters";
    private static final String PORTRAIT_ID = "chief_navigator_menelaus";
    private static final String MOTHERSHIP_PREFIX =
            "chief_navigator_sealed_mothership_";
    private static final String MOTHERSHIP_SPAWNED_PREFIX =
            "$chief_navigator_sealed_mothership_spawned_";
    private static final String MOTHERSHIP_DEFEATED_PREFIX =
            "$chief_navigator_sealed_mothership_defeated_";
    private static final String MOTHERSHIP_INVALID_PREFIX =
            "$chief_navigator_sealed_mothership_invalid_v1_";
    private static final String MOTHERSHIP_INDEX =
            "$chief_navigator_sealed_mothership_index";
    private static final String MOTHERSHIP_FRIENDLY =
            "$chief_navigator_restored_mothership_friendly_v1";
    private static final String MOTHERSHIP_COMBAT_PROXY_SUFFIX =
            "_combat_proxy";
    private static final String MOTHERSHIP_COMBAT_PROXY_MARKER =
            "$chief_navigator_mothership_combat_proxy_v1";
    private static final String VANILLA_MOTHERSHIP_VARIANT =
            "station_derelict_survey_mothership_Standard";
    private static final String ROAMER_PREFIX =
            "chief_navigator_wall_derelict_roamer_";
    private static final String ROAMER_SPAWNED_PREFIX =
            "$chief_navigator_wall_derelict_roamer_spawned_";
    private static final String FRIENDLY_PREFIX =
            "chief_navigator_unsealed_guard_";
    private static final String FRIENDLY_INVALID_PREFIX =
            "$chief_navigator_unsealed_guard_invalid_v1_";
    private static final String FRIENDLY_SPAWNED_PREFIX =
            "$chief_navigator_unsealed_guard_spawned_v1_";
    public static final String FRIENDLY_DRONE_PATROL_MARKER =
            "$chief_navigator_friendly_drone_patrol_v1";
    private static final String TRIAL_IMPORTANT =
            "chief_navigator_menelaus_first_trial";
    private static final String ASHEN_ROAMER_PREFIX =
            "chief_navigator_ashen_trial_roamer_";
    private static final String ASHEN_ROAMER_SPAWNED_PREFIX =
            "$chief_navigator_ashen_trial_roamer_spawned_";
    private static final String ASHEN_ROAMER_ESCALATED_PREFIX =
            "$chief_navigator_ashen_trial_roamer_escalated_";
    private static final String ASHEN_ROAMER_INVALID_PREFIX =
            "$chief_navigator_ashen_trial_roamer_invalid_v1_";

    private static final String[] MOTHERSHIP_NAMES = {
        "Aegis", "Argos", "Daedalus", "Pylos"
    };
    private static final float[][] MOTHERSHIP_OFFSETS = {
        {5200f, 4700f}, {-7000f, 4000f},
        {6500f, -4800f}, {-7600f, -5000f}
    };
    private static final float[][] ROAMER_OFFSETS = {
        {2600f, 900f}, {1900f, 3100f}, {-1800f, 3300f},
        {-3500f, 1500f}, {-3300f, -1800f}, {-1400f, -3500f},
        {2300f, -3200f}, {3900f, -1200f}, {4400f, 2100f},
        {-4700f, 300f}
    };
    private static final String[][] ROAMER_LOADOUTS = {
        {"chief_navigator_combat_guard_rampart_Standard", "chief_navigator_combat_guard_defender_PD", "chief_navigator_combat_guard_picket_Assault"},
        {"chief_navigator_combat_guard_bastillon_Standard", "chief_navigator_combat_guard_sentry_FS", "chief_navigator_combat_guard_warden_Defense"},
        {"chief_navigator_combat_guard_rampart_Standard", "chief_navigator_combat_guard_defender_PD", "chief_navigator_combat_guard_defender_PD"},
        {"chief_navigator_combat_guard_berserker_Assault", "chief_navigator_combat_guard_picket_Assault", "chief_navigator_combat_guard_sentry_FS"},
        {"chief_navigator_combat_guard_bastillon_Standard", "chief_navigator_combat_guard_warden_Defense", "chief_navigator_combat_guard_warden_Defense"},
        {"chief_navigator_combat_guard_rampart_Standard", "chief_navigator_combat_guard_sentry_FS", "chief_navigator_combat_guard_picket_Assault"},
        {"chief_navigator_combat_guard_bastillon_Standard", "chief_navigator_combat_guard_defender_PD", "chief_navigator_combat_guard_picket_Assault"},
        {"chief_navigator_combat_guard_berserker_Assault", "chief_navigator_combat_guard_sentry_FS", "chief_navigator_combat_guard_sentry_FS"},
        {"chief_navigator_combat_guard_rampart_Standard", "chief_navigator_combat_guard_warden_Defense", "chief_navigator_combat_guard_defender_PD"},
        {"chief_navigator_combat_guard_bastillon_Standard", "chief_navigator_combat_guard_picket_Assault", "chief_navigator_combat_guard_picket_Assault"}
    };
    private static final String[] FRIENDLY_VARIANTS = {
        "chief_navigator_combat_guard_rampart_Standard",
        "chief_navigator_combat_guard_defender_PD",
        "chief_navigator_combat_guard_picket_Assault",
        "chief_navigator_combat_guard_sentry_FS",
        "chief_navigator_combat_guard_warden_Defense"
    };
    private static final float[][] ASHEN_ROAMER_OFFSETS = {
        {8000f, 3800f}, {0f, 6500f}, {-7000f, 4200f},
        {-7500f, -3500f}, {0f, -6500f}, {8500f, -3600f}
    };
    private static final String[] ASHEN_ROAMER_VARIANTS = {
        "chief_navigator_combat_guard_rampart_Standard",
        "chief_navigator_combat_guard_bastillon_Standard",
        "chief_navigator_combat_guard_berserker_Assault",
        "chief_navigator_combat_guard_defender_PD",
        "chief_navigator_combat_guard_picket_Assault",
        "chief_navigator_combat_guard_sentry_FS",
        "chief_navigator_combat_guard_warden_Defense",
        "chief_navigator_combat_guard_defender_PD",
        "chief_navigator_combat_guard_picket_Assault"
    };

    private MenelausTrial() { }

    /** Creates the Labor population once during initial world generation. */
    public static void ensureWorld(StarSystemAPI system) {
        if (system == null || Global.getSector() == null) return;
        if (system.getMemoryWithoutUpdate().getBoolean(
                WORLD_INITIALIZED_V1)) return;
        // Commit before construction. A partial failure is left visible and
        // unsupported instead of being retried as a destructive repair pass.
        system.getMemoryWithoutUpdate().set(WORLD_INITIALIZED_V1, true);
        reconcileLegacyLaborReports();
        getOrCreateMenelaus();
        ensureAshenRoamingPresence();
        SectorEntityToken wall = system.getEntityById(
                DriftingWallEncounter.FLEET_ID);
        if (!DriftingWallEncounter.isDriftingWall(wall)) return;

        for (int i = 0; i < ROAMER_OFFSETS.length; i++) {
            ensureRoamingDerelicts(system, wall, i);
        }
        for (int i = 0; i < MOTHERSHIP_NAMES.length; i++) {
            ensureMothership(system, wall, i);
        }
        refreshProgress(system);
        if (isAccepted()) MenelausSensorLabor.ensureWorld();
    }

    /**
     * Historical compatibility entry point. Cross-location fleet repair was
     * retired; serialized fleet placement is authoritative.
     */
    static boolean relocateEscapedDerelict(CampaignFleetAPI fleet) {
        return false;
    }

    /** Polls persistent objective POIs and materializes unlocked allies. */
    public static void refreshProgress(StarSystemAPI system) {
        if (system == null || Global.getSector() == null) return;
        reconcileLegacyLaborReports();
        int defeated = 0;
        for (int i = 0; i < MOTHERSHIP_NAMES.length; i++) {
            String defeatedKey = MOTHERSHIP_DEFEATED_PREFIX + i;
            if (memory().getBoolean(defeatedKey)) {
                defeated++;
            }
        }

        if (defeated >= MOTHERSHIP_NAMES.length) {
            memory().set(MOTHERSHIPS_CLEARED, true);
        }
        if (isAccepted()) {
            MenelausSensorLabor.refreshWorldState();
            revealFinalLaborIfReady();
            if (isLaborReported(LABOR_MOTHERSHIPS)) {
                ensureFriendlyArmy(system, MOTHERSHIP_NAMES.length * 2);
            }
            MenelausTrialIntel.syncProgress(true);
            if (isComplete()) ensurePostTrialConsequences();
        }
        // Reconcile markers even before the first Labor is accepted. This
        // clears serialized objective reasons from older saves instead of
        // leaving motherships or later-Labor POIs prematurely marked.
        markLiveObjectivesImportant(system);
    }

    public static PersonAPI getOrCreateMenelaus() {
        ImportantPeopleAPI people = Global.getSector().getImportantPeople();
        PersonAPI person = people.getPerson(PERSON_ID);
        if (person == null) {
            if (memory().getBoolean(PERSON_CREATION_LATCH)) return null;
            // Commit before construction so a partial failure cannot turn a
            // later menu/timer lookup into an unbounded recreation loop.
            memory().set(PERSON_CREATION_LATCH, true);
            person = Global.getFactory().createPerson();
            person.setId(PERSON_ID);
            person.setName(new FullName(
                    "Menelaus", "", FullName.Gender.ANY));
            person.setGender(FullName.Gender.ANY);
            person.setFaction(TaskForceSpartanFaction.ID);
            person.setPostId(STRATEGOS_POST_ID);
            person.setImportance(PersonImportance.VERY_HIGH);
            person.setAICoreId(Commodities.ALPHA_CORE);
            person.setPortraitSprite(Global.getSettings().getSpriteName(
                    PORTRAIT_CATEGORY, PORTRAIT_ID));
            person.getMemoryWithoutUpdate().set(PERSON_OWNER_MARKER, true);
            people.addPerson(person);
        } else if (!isOwnedMenelaus(person)) {
            memory().set(PERSON_CREATION_LATCH, true);
            return null;
        } else {
            memory().set(PERSON_CREATION_LATCH, true);
        }
        // Reapply these for serialized people created before Strategos and
        // Strategic Intelligence became Menelaus's canonical identity. In
        // particular, removing the Base Commander post keeps vanilla colony
        // administration options out of his Labors conversation.
        person.setRankId(STRATEGOS_RANK_ID);
        person.setPostId(STRATEGOS_POST_ID);
        return person;
    }

    public static boolean isOwnedMenelaus(PersonAPI person) {
        return person != null && PERSON_ID.equals(person.getId())
                && (person.getMemoryWithoutUpdate().getBoolean(
                            PERSON_OWNER_MARKER)
                    || person.getMemoryWithoutUpdate().getBoolean(
                            FobIthacaContacts.MENELAUS_MARKER));
    }

    public static void accept() {
        accept(null);
    }

    public static void accept(TextPanelAPI text) {
        boolean newlyAccepted = !isAccepted();
        memory().set(ACCEPTED, true);
        if (newlyAccepted) {
            // New campaigns use explicit Menelaus report gates. Older saves
            // are reconciled separately from the stage they had reached.
            memory().set(LABOR_REPORT_MIGRATION, true);
        }
        DomainSecurityIFFAuthorization.grant();
        MenelausTrialIntel.ensureExists(text);
        StarSystemAPI system = OdysseyExpanseSystem.findExisting();
        if (system != null) {
            refreshProgress(system);
            markLiveObjectivesImportant(system);
        }
    }

    public static boolean isAccepted() {
        return Global.getSector() != null
                && memory().getBoolean(ACCEPTED);
    }

    public static boolean areMothershipsCleared() {
        return Global.getSector() != null
                && memory().getBoolean(MOTHERSHIPS_CLEARED);
    }

    public static boolean canAssaultWall() {
        return !isWallFriendly();
    }

    public static boolean isWallLaborActive() {
        return getCurrentLaborStage() == LABOR_WALL;
    }

    public static boolean isResearchLaborActive() {
        return getCurrentLaborStage() == LABOR_RESEARCH;
    }

    public static boolean isMothershipLaborActive() {
        return getCurrentLaborStage() == LABOR_MOTHERSHIPS;
    }

    public static boolean canAssaultMothership(SectorEntityToken entity) {
        return isMothershipLaborActive() && isMothershipPOI(entity);
    }

    public static boolean isGautamaDefeated() {
        return Global.getSector() != null
                && memory().getBoolean(GAUTAMA_DEFEATED);
    }

    public static boolean isWallFriendly() {
        return Global.getSector() != null
                && memory().getBoolean(WALL_FRIENDLY);
    }

    public static boolean shouldAcknowledgeEarlyWall() {
        return isAccepted()
                && memory().getBoolean(WALL_CLEARED_EARLY)
                && !memory().getBoolean(WALL_EARLY_REACTION_ACKNOWLEDGED);
    }

    public static void acknowledgeEarlyWall() {
        if (Global.getSector() != null) {
            memory().set(WALL_EARLY_REACTION_ACKNOWLEDGED, true);
        }
    }

    /** The four preparatory Labors, including their Menelaus reports. */
    public static boolean areInitialLaborsComplete() {
        return isAccepted()
                && (memory().getBoolean(FINAL_LABOR_FORCED_FOR_TESTING)
                    || isWallFriendly()
                    && isResearchLaborComplete()
                    && areMothershipsCleared()
                    && MenelausSensorLabor.isComplete()
                    && isLaborReported(LABOR_MOTHERSHIPS)
                    && isLaborReported(LABOR_RESEARCH)
                    && isLaborReported(LABOR_WALL)
                    && isLaborReported(LABOR_SENSOR));
    }

    public static boolean isFinalLaborRevealed() {
        return areInitialLaborsComplete();
    }

    public static boolean isFinalLaborActive() {
        return getCurrentLaborStage() == LABOR_GAUTAMA
                && isFinalLaborBriefed()
                && !isGautamaDefeated()
                && !isFinalLaborFailed();
    }

    public static boolean isFinalLaborFailed() {
        return Global.getSector() != null
                && memory().getBoolean(FINAL_LABOR_FAILED);
    }

    /** Commits the irreversible gate-breach consequence after a player loss. */
    public static boolean failFinalLabor() {
        if (Global.getSector() == null || isGautamaDefeated()
                || isFinalLaborFailed()) {
            return false;
        }
        memory().set(FINAL_LABOR_FAILED, true);
        memory().unset(FINAL_DEBRIEF_COMPLETE);
        MenelausTrialIntel.syncProgress(true);
        return true;
    }

    /** The Heavenly Strike is dead, but the player has not reported back. */
    public static boolean isFinalDebriefPending() {
        return Global.getSector() != null
                && !isFinalLaborFailed()
                && isGautamaDefeated()
                && !memory().getBoolean(FINAL_DEBRIEF_COMPLETE);
    }

    /** Commits Labor V only when the player finishes Menelaus's debrief. */
    public static boolean completeFinalDebrief() {
        if (!isFinalDebriefPending()) return false;
        memory().set(FINAL_DEBRIEF_COMPLETE, true);
        PostLaborHuntIntel.sync(null);
        MenelausCompletionDialogPlugin.request();
        return true;
    }

    /** Commits Labor V only after the player selects its acceptance option. */
    public static boolean acceptFinalLabor(TextPanelAPI text) {
        if (!isFinalLaborRevealed() || isFinalLaborBriefed()
                || isGautamaDefeated() || isFinalLaborFailed()) {
            return false;
        }
        memory().set(FINAL_LABOR_BRIEFED, true);
        MenelausTrialIntel.ensureExists(text);
        FinalLaborMusic.playMissionStart();
        return true;
    }

    /** Legacy entry point retained for console and old integration callers. */
    public static void acknowledgeFinalLabor() {
        acceptFinalLabor(null);
    }

    public static boolean isFinalLaborBriefed() {
        return Global.getSector() != null
                && memory().getBoolean(FINAL_LABOR_BRIEFED);
    }

    /** All five sequential Labors are complete. */
    public static boolean isComplete() {
        return getCurrentLaborStage() == LABOR_COMPLETE;
    }

    /**
     * Derives the active quest from durable objective and report flags.
     */
    public static int getCurrentLaborStage() {
        if (!isAccepted()) return LABOR_NONE;
        reconcileLegacyLaborReports();
        repairPrematureFinalLaborBriefing();
        if (memory().getBoolean(FINAL_LABOR_FORCED_FOR_TESTING)) {
            return isGautamaDefeated()
                    && memory().getBoolean(FINAL_DEBRIEF_COMPLETE)
                            ? LABOR_COMPLETE : LABOR_GAUTAMA;
        }
        if (!areMothershipsCleared()
                || !isLaborReported(LABOR_MOTHERSHIPS)) {
            return LABOR_MOTHERSHIPS;
        }
        if (!isResearchLaborComplete()
                || !isLaborReported(LABOR_RESEARCH)) {
            return LABOR_RESEARCH;
        }
        if (!isWallFriendly() || !isLaborReported(LABOR_WALL)) {
            return LABOR_WALL;
        }
        if (!MenelausSensorLabor.isComplete()
                || !isLaborReported(LABOR_SENSOR)) {
            return LABOR_SENSOR;
        }
        if (!isGautamaDefeated()) return LABOR_GAUTAMA;
        return memory().getBoolean(FINAL_DEBRIEF_COMPLETE)
                ? LABOR_COMPLETE : LABOR_GAUTAMA;
    }

    /** True while Labor V is revealed by a report but not yet briefed. */
    public static boolean isFinalLaborBriefingPending() {
        if (Global.getSector() == null || !isAccepted()
                || memory().getBoolean(FINAL_LABOR_FORCED_FOR_TESTING)) {
            return false;
        }
        if (MenelausSensorLabor.isComplete()
                && !memory().getBoolean(LABOR_IV_REPORTED)) {
            return true;
        }
        return areInitialLaborsComplete() && !isFinalLaborBriefed();
    }

    /**
     * Console-test hook: skips prerequisite gating without consuming or
     * deleting any of the earlier Labors' authored objectives.
     */
    public static boolean forceFinalLaborAvailableForTesting() {
        if (Global.getSector() == null) return false;
        if (!isAccepted()) accept();
        memory().set(FINAL_LABOR_FORCED_FOR_TESTING, true);
        memory().unset(GAUTAMA_DEFEATED);
        memory().unset(FINAL_LABOR_FAILED);
        memory().unset(FINAL_DEBRIEF_COMPLETE);
        MenelausCompletionDialogPlugin.resetForTesting();
        memory().set(FINAL_LABOR_REVEALED, true);
        memory().set(FINAL_LABOR_BRIEFED, true);

        StarSystemAPI expanse = OdysseyExpanseSystem.findExisting();
        if (expanse != null) refreshProgress(expanse);
        MenelausSensorLabor.ensureWorld();
        MenelausTrialIntel.syncProgress(true);
        return isFinalLaborActive();
    }

    public static int getMothershipObjectiveTotal() {
        return MOTHERSHIP_NAMES.length;
    }

    public static int getWallObjectiveProgress() {
        return isWallFriendly() ? 1 : 0;
    }

    public static int getGautamaObjectiveProgress() {
        return OdysseyPredatorScript.getFinalLaborInvasionProgress();
    }

    public static int getGautamaObjectiveTarget() {
        return OdysseyPredatorScript.getFinalLaborInvasionTarget();
    }

    public static int getResearchStationsResearched() {
        return OdysseyExpanseSystem.getResearchedStationCount();
    }

    public static int getResearchStationTotal() {
        return OdysseyExpanseSystem.getResearchStationTotal();
    }

    /** Labor II needs any four of the six unique components installed. */
    public static int getResearchObjectiveTarget() {
        return 4;
    }

    /** Required Labor progress counts delivery and installation, not salvage. */
    public static int getResearchObjectiveProgress() {
        return IthacaResearchUpgrades.getInstalledCount();
    }

    /** True when the parts in hand can finish Labor II at Menelaus's ledger. */
    public static boolean canCompleteResearchLaborAtIthaca() {
        return getResearchObjectiveProgress()
                + IthacaResearchUpgrades.getAvailableCount()
                >= getResearchObjectiveTarget();
    }

    public static boolean isResearchLaborComplete() {
        if (Global.getSector() == null) return false;
        // A reported Labor is durable completion state. This preserves saves
        // which passed Labor II under the historical five-station rule before
        // components became physical, consumable upgrades.
        return memory().getBoolean(LABOR_II_REPORTED)
                || getResearchObjectiveProgress()
                        >= getResearchObjectiveTarget();
    }

    public static int getMothershipsDefeated() {
        if (Global.getSector() == null) return 0;
        int result = 0;
        for (int i = 0; i < MOTHERSHIP_NAMES.length; i++) {
            if (memory().getBoolean(MOTHERSHIP_DEFEATED_PREFIX + i)) {
                result++;
            }
        }
        return result;
    }

    public static int getMothershipsRemaining() {
        return MOTHERSHIP_NAMES.length - getMothershipsDefeated();
    }

    public static int getFriendlyFleetCount() {
        return isLaborReported(LABOR_MOTHERSHIPS)
                ? MOTHERSHIP_NAMES.length * 2 : 0;
    }

    /** True when a Labor's field objective is finished. */
    public static boolean isLaborObjectiveComplete(int stage) {
        if (stage == LABOR_MOTHERSHIPS) return areMothershipsCleared();
        if (stage == LABOR_RESEARCH) return isResearchLaborComplete();
        if (stage == LABOR_WALL) return isWallFriendly();
        if (stage == LABOR_SENSOR) return MenelausSensorLabor.isComplete();
        if (stage == LABOR_GAUTAMA) return isGautamaDefeated();
        return false;
    }

    /** True after the player has returned to Menelaus for this Labor. */
    public static boolean isLaborReported(int stage) {
        if (Global.getSector() == null) return false;
        reconcileLegacyLaborReports();
        if (stage == LABOR_MOTHERSHIPS) {
            return memory().getBoolean(LABOR_I_REPORTED);
        }
        if (stage == LABOR_RESEARCH) {
            return memory().getBoolean(LABOR_II_REPORTED);
        }
        if (stage == LABOR_WALL) {
            return memory().getBoolean(LABOR_III_REPORTED);
        }
        if (stage == LABOR_SENSOR) {
            return memory().getBoolean(LABOR_IV_REPORTED);
        }
        if (stage == LABOR_GAUTAMA) {
            return memory().getBoolean(FINAL_DEBRIEF_COMPLETE);
        }
        return false;
    }

    /** Field work is complete, but advancement awaits a report at Ithaca. */
    public static boolean isLaborReportPending(int stage) {
        return getCurrentLaborStage() == stage
                && isLaborObjectiveComplete(stage)
                && !isLaborReported(stage);
    }

    /** Commits one preparatory Labor only from its Menelaus report scene. */
    public static boolean reportLabor(int stage) {
        if (Global.getSector() == null
                || getCurrentLaborStage() != stage
                || !isLaborObjectiveComplete(stage)
                || isLaborReported(stage)) {
            return false;
        }
        String flag = getLaborReportFlag(stage);
        if (flag == null) return false;
        memory().set(flag, true);

        StarSystemAPI expanse = OdysseyExpanseSystem.findExisting();
        if (expanse != null) {
            if (stage == LABOR_MOTHERSHIPS) {
                applyMothershipReportOutcome(expanse);
            }
            MenelausSensorLabor.ensureWorld();
            refreshProgress(expanse);
        } else {
            revealFinalLaborIfReady();
            MenelausTrialIntel.syncProgress(true);
        }
        return true;
    }

    /** True only for one of the four fixed Domain mothership POIs. */
    public static boolean isMothershipPOI(SectorEntityToken entity) {
        if (entity == null || entity.getMemoryWithoutUpdate() == null
                || !entity.getMemoryWithoutUpdate().getBoolean(
                        MOTHERSHIP_MARKER)) return false;
        int index = getMothershipIndex(entity);
        return index >= 0 && index < MOTHERSHIP_NAMES.length
                && (MOTHERSHIP_PREFIX + index).equals(entity.getId());
    }

    /** True for a post-report mothership rebuilt under Menelaus's IFF. */
    public static boolean isFriendlyMothership(SectorEntityToken entity) {
        return isMothershipPOI(entity)
                && entity.getMemoryWithoutUpdate().getBoolean(
                        MOTHERSHIP_FRIENDLY);
    }

    public static String getMothershipDisplayName(SectorEntityToken entity) {
        int index = getMothershipIndex(entity);
        if (index < 0 || index >= MOTHERSHIP_NAMES.length) {
            return "Sealed Domain Mothership";
        }
        return (isFriendlyMothership(entity)
                ? "Restored Guard Mothership "
                : "Sealed Domain Mothership ") + MOTHERSHIP_NAMES[index];
    }

    /** Creates a vanilla mothership battle with the authored Guard escorts. */
    public static CampaignFleetAPI createMothershipCombatProxy(
            SectorEntityToken poi) {
        if (!canAssaultMothership(poi)
                || poi.getContainingLocation() == null) {
            return null;
        }
        int index = getMothershipIndex(poi);
        if (index < 0 || index >= MOTHERSHIP_NAMES.length
                || memory().getBoolean(MOTHERSHIP_DEFEATED_PREFIX + index)) {
            return null;
        }

        String proxyId = MOTHERSHIP_PREFIX + index
                + MOTHERSHIP_COMBAT_PROXY_SUFFIX;
        LocationAPI location = poi.getContainingLocation();
        SectorEntityToken stale = location.getEntityById(proxyId);
        if (stale != null) {
            if (!(stale instanceof CampaignFleetAPI)
                    || ((CampaignFleetAPI) stale).isPlayerFleet()
                    || stale == Global.getSector().getPlayerFleet()
                    || !stale.getMemoryWithoutUpdate().getBoolean(
                            MOTHERSHIP_COMBAT_PROXY_MARKER)
                    || TroyArrivalScript.isFleetBusyForMutation(
                            (CampaignFleetAPI) stale)) {
                return null;
            }
            location.removeEntity(stale);
        }

        CampaignFleetAPI fleet = createFleet(
                IthacaSectionEncounter.COMBAT_GUARD_FACTION_ID,
                proxyId,
                getMothershipDisplayName(poi) + " Defense",
                new String[] {
                    VANILLA_MOTHERSHIP_VARIANT,
                    "chief_navigator_combat_guard_warden_Defense",
                    "chief_navigator_combat_guard_warden_Defense",
                    "chief_navigator_combat_guard_defender_PD",
                    "chief_navigator_combat_guard_defender_PD",
                    "chief_navigator_combat_guard_picket_Assault",
                    "chief_navigator_combat_guard_sentry_FS"
                });
        configureHostileFleet(fleet);
        fleet.getMemoryWithoutUpdate().set(
                MOTHERSHIP_COMBAT_PROXY_MARKER, true);
        location.addEntity(fleet);
        fleet.setLocation(poi.getLocation().x, poi.getLocation().y);
        return fleet;
    }

    /** Removes the temporary fleet and commits victory against its POI. */
    public static void finishMothershipCombat(
            SectorEntityToken poi, CampaignFleetAPI proxy) {
        boolean ownedProxy = proxy != null
                && !proxy.isPlayerFleet()
                && proxy != Global.getSector().getPlayerFleet()
                && proxy.getMemoryWithoutUpdate().getBoolean(
                        MOTHERSHIP_COMBAT_PROXY_MARKER);
        boolean defeated = ownedProxy && isMothershipDestroyed(proxy);
        LocationAPI location = poi == null ? null : poi.getContainingLocation();
        if (ownedProxy && proxy.getContainingLocation() != null) {
            proxy.getContainingLocation().removeEntity(proxy);
        }
        if (!defeated || !isMothershipPOI(poi)) return;

        int index = getMothershipIndex(poi);
        if (index < 0 || index >= MOTHERSHIP_NAMES.length) return;
        memory().set(MOTHERSHIP_DEFEATED_PREFIX + index, true);
        if (location != null) {
            Misc.makeUnimportant(poi, TRIAL_IMPORTANT);
            if (location instanceof StarSystemAPI) {
                StarSystemAPI system = (StarSystemAPI) location;
                location.removeEntity(poi);
                refreshProgress(system);
            } else {
                location.removeEntity(poi);
            }
        }
    }

    /** Victory is the loss of the mothership hull, not every disabled escort. */
    private static boolean isMothershipDestroyed(CampaignFleetAPI proxy) {
        if (proxy == null) return false;
        for (FleetMemberAPI member
                : proxy.getFleetData().getMembersListCopy()) {
            if ("station_derelict_survey_mothership".equals(
                    member.getHullId())) {
                return false;
            }
        }
        return true;
    }

    /** Completes the Wall Labor and permanently changes its campaign IFF. */
    public static void reportWallDisabled(SectorEntityToken wall) {
        if (!DriftingWallEncounter.isDriftingWall(wall)
                || !canAssaultWall()) return;
        if (!isWallLaborActive()) {
            memory().set(WALL_CLEARED_EARLY, true);
        }
        memory().set(WALL_FRIENDLY, true);
        wall.setFaction(Factions.PLAYER);
        wall.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE, true);
        wall.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_MAKE_HOSTILE);
        wall.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE);
        Misc.makeUnimportant(wall, TRIAL_IMPORTANT);
        if (wall.getContainingLocation() instanceof StarSystemAPI) {
            markLiveObjectivesImportant(
                    (StarSystemAPI) wall.getContainingLocation());
        }
        revealFinalLaborIfReady();
        MenelausTrialIntel.syncProgress(true);
    }

    /**
     * Returns an uploaded objective location, falling back to FOB Ithaca.
     */
    public static SectorEntityToken getCurrentObjectiveLocation() {
        return getObjectiveLocationForStage(getCurrentLaborStage());
    }

    /** Returns the map target belonging to one separate Labor Intel entry. */
    public static SectorEntityToken getObjectiveLocationForStage(int stage) {
        if (isLaborReportPending(stage)) {
            return getFobIthaca();
        }
        StarSystemAPI expanse = OdysseyExpanseSystem.findExisting();
        if (stage == LABOR_WALL && expanse != null) {
            SectorEntityToken wall = expanse.getEntityById(
                    DriftingWallEncounter.FLEET_ID);
            if (DriftingWallEncounter.isDriftingWall(wall)) return wall;
        }
        if (stage == LABOR_RESEARCH) {
            // Once the player is carrying enough uninstalled components, the
            // actionable objective is delivery at Ithaca rather than another
            // optional station.
            if (canCompleteResearchLaborAtIthaca()) {
                SectorEntityToken ithaca = getFobIthaca();
                if (ithaca != null) return ithaca;
            }
            for (SectorEntityToken station :
                    OdysseyExpanseSystem.getRemainingResearchStations()) {
                if (station != null) return station;
            }
            if (expanse != null) return expanse.getCenter();
        }
        if (stage == LABOR_MOTHERSHIPS && expanse != null) {
            for (int i = 0; i < MOTHERSHIP_NAMES.length; i++) {
                SectorEntityToken poi = expanse.getEntityById(MOTHERSHIP_PREFIX + i);
                if (isMothershipPOI(poi)) return poi;
            }
            return expanse.getCenter();
        }
        if (stage == LABOR_SENSOR) {
            SectorEntityToken array = MenelausSensorLabor.getSensorArray();
            if (array != null) return array;
            StarSystemAPI devoured = OdysseyExpanseSystem.findSystemById(
                    OdysseyExpanseSystem.DEVOURED_REACH_ID);
            if (devoured != null) return devoured.getCenter();
        }
        if (stage == LABOR_GAUTAMA) {
            if (isFinalDebriefPending()) {
                StarSystemAPI ashen = OdysseyExpanseSystem.findSystemById(
                        OdysseyExpanseSystem.ASHEN_VERGE_ID);
                SectorEntityToken ithaca = getFobIthaca();
                if (ithaca != null) return ithaca;
            }
            SectorEntityToken objective = OdysseyPredatorScript
                    .getFinalLaborObjective();
            if (objective != null) return objective;
        }
        if (expanse != null) {
            if (!isWallFriendly()) {
                SectorEntityToken wall = expanse.getEntityById(
                        DriftingWallEncounter.FLEET_ID);
                if (DriftingWallEncounter.isDriftingWall(wall)) return wall;
            }
        }
        return getFobIthaca();
    }

    private static SectorEntityToken getFobIthaca() {
        return OdysseyExpanseSystem.getFobIthaca();
    }

    /** Builds the post-Labors difficulty spike in Ashen Verge once. */
    public static void ensurePostTrialConsequences() {
        if (!isComplete() || Global.getSector() == null) return;
        if (memory().getBoolean(ASHEN_ESCALATED)) return;
        StarSystemAPI ashen = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.ASHEN_VERGE_ID);
        if (ashen == null) return;

        boolean complete = true;
        for (int i = 0; i < ASHEN_ROAMER_OFFSETS.length; i++) {
            complete &= ensureAshenRoamer(ashen, i, true);
        }
        if (complete) memory().set(ASHEN_ESCALATED, true);
    }

    /** Creates the ordinary packs which later grow into full armadas. */
    private static void ensureAshenRoamingPresence() {
        StarSystemAPI ashen = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.ASHEN_VERGE_ID);
        if (ashen == null) return;
        boolean escalated = isComplete();
        for (int i = 0; i < ASHEN_ROAMER_OFFSETS.length; i++) {
            ensureAshenRoamer(ashen, i, escalated);
        }
        if (escalated) memory().set(ASHEN_ESCALATED, true);
    }

    private static void ensureMothership(
            StarSystemAPI system, SectorEntityToken wall, int index) {
        if (memory().getBoolean(MOTHERSHIP_DEFEATED_PREFIX + index)) {
            SectorEntityToken proxy = system.getEntityById(
                    MOTHERSHIP_PREFIX + index
                            + MOTHERSHIP_COMBAT_PROXY_SUFFIX);
            if (proxy instanceof CampaignFleetAPI
                    && !isPlayerControlledFleet((CampaignFleetAPI) proxy)
                    && proxy.getMemoryWithoutUpdate().getBoolean(
                            MOTHERSHIP_COMBAT_PROXY_MARKER)
                    && !TroyArrivalScript.isFleetBusyForMutation(
                            (CampaignFleetAPI) proxy)) {
                system.removeEntity(proxy);
            }
            if (isLaborReported(LABOR_MOTHERSHIPS)) {
                ensureFriendlyMothership(system, wall, index);
            } else {
                SectorEntityToken poi = system.getEntityById(
                        MOTHERSHIP_PREFIX + index);
                if (isMothershipPOI(poi)) system.removeEntity(poi);
            }
            return;
        }
        SectorEntityToken existing = system.getEntityById(
                MOTHERSHIP_PREFIX + index);
        if (isMothershipPOI(existing)) {
            configureMothershipPOI(existing, wall, index);
            memory().set(MOTHERSHIP_SPAWNED_PREFIX + index, true);
            return;
        }
        if (existing != null) {
            logInvalidMothershipOnce(index,
                    "objective ID contains incompatible serialized state");
            memory().set(MOTHERSHIP_SPAWNED_PREFIX + index, true);
            return;
        }

        SectorEntityToken poi = system.addCustomEntity(
                MOTHERSHIP_PREFIX + index,
                "Sealed Domain Mothership " + MOTHERSHIP_NAMES[index],
                Entities.DERELICT_MOTHERSHIP,
                Factions.DERELICT);
        configureMothershipPOI(poi, wall, index);
        memory().set(MOTHERSHIP_SPAWNED_PREFIX + index, true);
    }

    private static void configureMothershipPOI(
            SectorEntityToken poi, SectorEntityToken wall, int index) {
        float x = wall.getLocation().x + MOTHERSHIP_OFFSETS[index][0];
        float y = wall.getLocation().y + MOTHERSHIP_OFFSETS[index][1];
        poi.setFixedLocation(x, y);
        poi.getVelocity().set(0f, 0f);
        poi.setFacing(45f * index);
        poi.setFaction(Factions.DERELICT);
        boolean active = isMothershipLaborActive();
        poi.setDiscoverable(active ? null : Boolean.TRUE);
        poi.setSensorProfile(active ? null : Float.valueOf(600f));
        poi.getMemoryWithoutUpdate().set(MOTHERSHIP_MARKER, true);
        poi.getMemoryWithoutUpdate().set(MOTHERSHIP_INDEX, index);
        poi.getMemoryWithoutUpdate().unset(MOTHERSHIP_FRIENDLY);
        poi.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE);
        poi.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
    }

    /** Restores a defeated mothership at its authored location after report. */
    private static void ensureFriendlyMothership(
            StarSystemAPI system, SectorEntityToken wall, int index) {
        String id = MOTHERSHIP_PREFIX + index;
        SectorEntityToken poi = system.getEntityById(id);
        if (poi != null && !isMothershipPOI(poi)) {
            logInvalidMothershipOnce(index,
                    "friendly objective ID contains incompatible "
                            + "serialized state");
            return;
        }
        if (poi == null) {
            poi = system.addCustomEntity(
                    id,
                    "Restored Guard Mothership " + MOTHERSHIP_NAMES[index],
                    Entities.DERELICT_MOTHERSHIP,
                    IthacaSectionEncounter.COMBAT_GUARD_FACTION_ID);
            poi.getMemoryWithoutUpdate().set(MOTHERSHIP_MARKER, true);
            poi.getMemoryWithoutUpdate().set(MOTHERSHIP_INDEX, index);
        }
        float x = wall.getLocation().x + MOTHERSHIP_OFFSETS[index][0];
        float y = wall.getLocation().y + MOTHERSHIP_OFFSETS[index][1];
        poi.setName("Restored Guard Mothership " + MOTHERSHIP_NAMES[index]);
        poi.setFixedLocation(x, y);
        poi.getVelocity().set(0f, 0f);
        poi.setFacing(45f * index);
        poi.setFaction(IthacaSectionEncounter.COMBAT_GUARD_FACTION_ID);
        poi.setDiscoverable(null);
        poi.setSensorProfile(null);
        poi.getMemoryWithoutUpdate().set(MOTHERSHIP_MARKER, true);
        poi.getMemoryWithoutUpdate().set(MOTHERSHIP_INDEX, index);
        poi.getMemoryWithoutUpdate().set(MOTHERSHIP_FRIENDLY, true);
        poi.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE, true);
        poi.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_MAKE_HOSTILE);
        poi.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE);
        poi.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
    }

    private static void applyMothershipReportOutcome(
            StarSystemAPI system) {
        SectorEntityToken wall = system == null ? null
                : system.getEntityById(DriftingWallEncounter.FLEET_ID);
        if (!DriftingWallEncounter.isDriftingWall(wall)) return;
        for (int index = 0; index < MOTHERSHIP_NAMES.length; index++) {
            if (memory().getBoolean(MOTHERSHIP_DEFEATED_PREFIX + index)) {
                ensureFriendlyMothership(system, wall, index);
            }
        }
    }

    private static void logInvalidMothershipOnce(
            int index, String reason) {
        String key = MOTHERSHIP_INVALID_PREFIX + index;
        if (memory().getBoolean(key)) return;
        memory().set(key, true);
        Global.getLogger(MenelausTrial.class).error(
                "Guard Mothership " + index + ": " + reason
                        + "; refusing replacement");
    }

    private static int getMothershipIndex(SectorEntityToken entity) {
        if (entity == null) return -1;
        if (entity.getMemoryWithoutUpdate().contains(MOTHERSHIP_INDEX)) {
            return entity.getMemoryWithoutUpdate().getInt(MOTHERSHIP_INDEX);
        }
        String id = entity.getId();
        if (id == null || !id.startsWith(MOTHERSHIP_PREFIX)) return -1;
        try {
            return Integer.parseInt(id.substring(MOTHERSHIP_PREFIX.length()));
        } catch (RuntimeException ex) {
            return -1;
        }
    }

    private static void ensureRoamingDerelicts(
            StarSystemAPI system, SectorEntityToken wall, int index) {
        SectorEntityToken existing = system.getEntityById(ROAMER_PREFIX + index);
        if (existing instanceof CampaignFleetAPI) {
            CampaignFleetAPI fleet = (CampaignFleetAPI) existing;
            if (isPlayerControlledFleet(fleet)
                    || !fleet.getMemoryWithoutUpdate().getBoolean(
                            OdysseyPredatorScript
                                    .AUTHORED_MOBILE_DERELICT_MARKER)
                    || TroyArrivalScript.isFleetBusyForMutation(fleet)) {
                return;
            }
            fleet.setName("Unmoored Combat Guard Pack");
            configureHostileFleet(fleet);
            return;
        }
        if (existing != null) return;
        if (memory().getBoolean(ROAMER_SPAWNED_PREFIX + index)) return;

        CampaignFleetAPI fleet = createFleet(
                Factions.DERELICT,
                ROAMER_PREFIX + index,
                "Unmoored Combat Guard Pack",
                ROAMER_LOADOUTS[index]);
        configureHostileFleet(fleet);
        system.addEntity(fleet);
        fleet.setLocation(
                wall.getLocation().x + ROAMER_OFFSETS[index][0],
                wall.getLocation().y + ROAMER_OFFSETS[index][1]);
        fleet.addAssignment(
                com.fs.starfarer.api.campaign.FleetAssignment.PATROL_SYSTEM,
                system.getCenter(), 1000000f,
                "roaming the approaches to the drifting wall");
        memory().set(ROAMER_SPAWNED_PREFIX + index, true);
    }

    private static boolean ensureAshenRoamer(
            StarSystemAPI system, int index, boolean escalated) {
        String id = ASHEN_ROAMER_PREFIX + index;
        SectorEntityToken existing = system.getEntityById(id);
        CampaignFleetAPI fleet = existing instanceof CampaignFleetAPI
                ? (CampaignFleetAPI) existing : null;
        String spawnKey = escalated
                ? ASHEN_ROAMER_ESCALATED_PREFIX + index
                : ASHEN_ROAMER_SPAWNED_PREFIX + index;
        if (existing != null && fleet == null) {
            String invalidKey = ASHEN_ROAMER_INVALID_PREFIX + index;
            if (!memory().getBoolean(invalidKey)) {
                memory().set(invalidKey, true);
                Global.getLogger(MenelausTrial.class).error(
                        "Ashen patrol ID " + index + " contains "
                                + "incompatible serialized state; refusing "
                                + "replacement");
            }
            memory().set(spawnKey, true);
            return true;
        }
        if (fleet != null && (isPlayerControlledFleet(fleet)
                || !fleet.getMemoryWithoutUpdate().getBoolean(
                        OdysseyPredatorScript
                                .AUTHORED_MOBILE_DERELICT_MARKER))) {
            memory().set(spawnKey, true);
            return true;
        }
        if (fleet == null) {
            if (memory().getBoolean(spawnKey)
                    || memory().getBoolean(
                            ASHEN_ROAMER_SPAWNED_PREFIX + index)) {
                // A patrol that once existed and is now absent was defeated;
                // story progression does not reconstruct it.
                memory().set(spawnKey, true);
                return true;
            }
            String[] loadout = escalated
                    ? ASHEN_ROAMER_VARIANTS
                    : ROAMER_LOADOUTS[index % ROAMER_LOADOUTS.length];
            fleet = createFleet(
                    Factions.PLAYER,
                    id,
                    escalated ? "Ascendant Combat Guard Armada"
                            : "Ashen Verge Combat Guard Pack",
                    loadout);
            system.addEntity(fleet);
            fleet.setLocation(
                    ASHEN_ROAMER_OFFSETS[index][0],
                    ASHEN_ROAMER_OFFSETS[index][1]);
            memory().set(ASHEN_ROAMER_SPAWNED_PREFIX + index, true);
            if (escalated) {
                memory().set(ASHEN_ROAMER_ESCALATED_PREFIX + index, true);
            }
        }

        if (isPlayerControlledFleet(fleet)) {
            memory().set(spawnKey, true);
            return true;
        }
        if (fleet.isEmpty()) {
            memory().set(spawnKey, true);
            return true;
        }
        if (TroyArrivalScript.isFleetBusyForMutation(fleet)) return false;

        if (escalated) {
            // Grow the same persistent patrol from three hulls to nine.
            int next = fleet.getFleetData().getMembersListCopy().size();
            while (next < ASHEN_ROAMER_VARIANTS.length) {
                addMember(fleet, ASHEN_ROAMER_VARIANTS[next]);
                next++;
            }
            fleet.setName("Ascendant Combat Guard Armada");
            memory().set(ASHEN_ROAMER_ESCALATED_PREFIX + index, true);
        } else {
            fleet.setName("Ashen Verge Combat Guard Pack");
        }
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
        configureAshenAlliedFleet(fleet);
        if (fleet.getCurrentAssignment() == null) {
            fleet.addAssignment(
                    com.fs.starfarer.api.campaign.FleetAssignment.PATROL_SYSTEM,
                    system.getCenter(), 1000000f,
                    escalated ? "roaming Ashen Verge in force"
                            : "roaming Ashen Verge");
        }
        return true;
    }

    /** Each restored mothership releases two finite, recruitable drone patrols. */
    private static void ensureFriendlyArmy(
            StarSystemAPI system, int desiredCount) {
        SectorEntityToken wall = system.getEntityById(
                DriftingWallEncounter.FLEET_ID);
        if (!DriftingWallEncounter.isDriftingWall(wall)) return;
        int count = Math.min(desiredCount, MOTHERSHIP_NAMES.length * 2);
        for (int i = 0; i < count; i++) {
            SectorEntityToken existing = system.getEntityById(
                    FRIENDLY_PREFIX + i);
            CampaignFleetAPI fleet = existing instanceof CampaignFleetAPI
                    ? (CampaignFleetAPI) existing : null;
            if (existing != null && fleet == null) {
                String invalidKey = FRIENDLY_INVALID_PREFIX + i;
                if (!memory().getBoolean(invalidKey)) {
                    memory().set(invalidKey, true);
                    Global.getLogger(MenelausTrial.class).error(
                            "Guard patrol ID " + i + " contains "
                                    + "incompatible serialized state; "
                                    + "refusing replacement");
                }
                continue;
            }
            if (fleet != null && (isPlayerControlledFleet(fleet)
                    || !fleet.getMemoryWithoutUpdate().getBoolean(
                            OdysseyPredatorScript
                                    .AUTHORED_MOBILE_DERELICT_MARKER)
                    || TroyArrivalScript.isFleetBusyForMutation(fleet))) {
                continue;
            }
            if (fleet != null && !fleet.isEmpty()) {
                memory().set(FRIENDLY_SPAWNED_PREFIX + i, true);
                fleet.setName(MOTHERSHIP_NAMES[i / 2]
                        + " Guard Patrol " + (i % 2 + 1));
                configureFriendlyFleet(fleet, system);
                continue;
            }
            if (existing != null) system.removeEntity(existing);
            // Ships transferred to the player, and patrols lost in combat,
            // stay gone. Population refresh never replenishes their rosters.
            if (memory().getBoolean(FRIENDLY_SPAWNED_PREFIX + i)) continue;
            memory().set(FRIENDLY_SPAWNED_PREFIX + i, true);
            String[] loadout = {
                FRIENDLY_VARIANTS[i % FRIENDLY_VARIANTS.length],
                FRIENDLY_VARIANTS[(i + 1) % FRIENDLY_VARIANTS.length],
                FRIENDLY_VARIANTS[(i + 3) % FRIENDLY_VARIANTS.length]
            };
            fleet = createFleet(
                    IthacaSectionEncounter.COMBAT_GUARD_FACTION_ID,
                    FRIENDLY_PREFIX + i,
                    MOTHERSHIP_NAMES[i / 2] + " Guard Patrol "
                            + (i % 2 + 1),
                    loadout);
            system.addEntity(fleet);
            SectorEntityToken mothership = system.getEntityById(
                    MOTHERSHIP_PREFIX + i / 2);
            SectorEntityToken source = isFriendlyMothership(mothership)
                    ? mothership : wall;
            double releaseAngle = Math.toRadians(45d + 180d * (i % 2));
            fleet.setLocation(
                    source.getLocation().x
                            + (float) Math.cos(releaseAngle) * 700f,
                    source.getLocation().y
                            + (float) Math.sin(releaseAngle) * 700f);
            configureFriendlyFleet(fleet, system);
        }
    }

    /** Only the authored post-Labor-I patrols offer drone reassignment. */
    public static boolean isFriendlyDronePatrol(SectorEntityToken entity) {
        if (!(entity instanceof CampaignFleetAPI)) return false;
        CampaignFleetAPI fleet = (CampaignFleetAPI) entity;
        if (isPlayerControlledFleet(fleet)
                || !fleet.getMemoryWithoutUpdate().getBoolean(
                        FRIENDLY_DRONE_PATROL_MARKER)) return false;
        for (int i = 0; i < MOTHERSHIP_NAMES.length * 2; i++) {
            if ((FRIENDLY_PREFIX + i).equals(fleet.getId())) return true;
        }
        return false;
    }

    private static CampaignFleetAPI createFleet(
            String factionId, String id, String name, String[] variants) {
        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(
                factionId, name, true);
        fleet.setId(id);
        fleet.setName(name);
        fleet.setNoFactionInName(true);
        fleet.setTransponderOn(false);
        if (id != null && (id.startsWith(ROAMER_PREFIX)
                || id.startsWith(FRIENDLY_PREFIX)
                || id.startsWith(ASHEN_ROAMER_PREFIX))) {
            fleet.getMemoryWithoutUpdate().set(
                    OdysseyPredatorScript.AUTHORED_MOBILE_DERELICT_MARKER,
                    true);
        }
        for (String variant : variants) addMember(fleet, variant);
        if (!fleet.getFleetData().getMembersListCopy().isEmpty()) {
            fleet.getFleetData().setFlagship(
                    fleet.getFleetData().getMembersListCopy().get(0));
        }
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
        return fleet;
    }

    private static boolean isPlayerControlledFleet(CampaignFleetAPI fleet) {
        return fleet != null && (fleet.isPlayerFleet()
                || Global.getSector() != null
                        && fleet == Global.getSector().getPlayerFleet());
    }

    private static void addMember(CampaignFleetAPI fleet, String variantId) {
        FleetMemberAPI member = Global.getFactory().createFleetMember(
                FleetMemberType.SHIP, variantId);
        fleet.getFleetData().addFleetMember(member);
        member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
    }

    /** Migrates authored stock Derelict members to their Combat Guard skins. */
    static boolean convertFleetToCombatGuard(CampaignFleetAPI fleet) {
        if (fleet == null) return false;
        boolean changed = false;
        for (FleetMemberAPI member
                : fleet.getFleetData().getMembersListCopy()) {
            String replacementId = getCombatGuardVariantId(member);
            if (replacementId == null) continue;
            ShipVariantAPI replacement = Global.getSettings().getVariant(
                    replacementId);
            if (replacement == null) continue;
            float cr = member.getRepairTracker().getCR();
            member.setVariant(replacement.clone(), false, true);
            member.getRepairTracker().setCR(Math.min(
                    cr, member.getRepairTracker().getMaxCR()));
            changed = true;
        }
        if (changed) {
            fleet.getFleetData().setSyncNeeded();
            fleet.getFleetData().syncIfNeeded();
        }
        return changed;
    }

    private static String getCombatGuardVariantId(FleetMemberAPI member) {
        if (member == null || member.getVariant() == null) return null;
        String variantId = member.getVariant().getHullVariantId();
        if (variantId != null
                && variantId.startsWith("chief_navigator_combat_guard_")) {
            return null;
        }
        String hullId = member.getHullId();
        if ("rampart".equals(hullId)) {
            return "chief_navigator_combat_guard_rampart_Standard";
        }
        if ("bastillon".equals(hullId)) {
            return "chief_navigator_combat_guard_bastillon_Standard";
        }
        if ("berserker".equals(hullId)) {
            return "chief_navigator_combat_guard_berserker_Assault";
        }
        if ("defender".equals(hullId)) {
            return "chief_navigator_combat_guard_defender_PD";
        }
        if ("picket".equals(hullId)) {
            return "chief_navigator_combat_guard_picket_Assault";
        }
        if ("sentry".equals(hullId)) {
            return "chief_navigator_combat_guard_sentry_FS";
        }
        if ("warden".equals(hullId)) {
            return "chief_navigator_combat_guard_warden_Defense";
        }
        return null;
    }

    private static void configureHostileFleet(CampaignFleetAPI fleet) {
        // These packs never accepted Menelaus's restored command partition.
        // Vanilla Derelict and Domain Combat Guard are mutually hostile.
        fleet.setFaction(Factions.DERELICT, true);
        convertFleetToCombatGuard(fleet);
        fleet.setNoAutoDespawn(true);
        fleet.setAbortDespawn(true);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_NO_JUMP, true);
    }

    private static void configureFriendlyFleet(
            CampaignFleetAPI fleet,
            StarSystemAPI system) {
        fleet.setFaction(IthacaSectionEncounter.COMBAT_GUARD_FACTION_ID, true);
        convertFleetToCombatGuard(fleet);
        fleet.setNoAutoDespawn(true);
        fleet.setAbortDespawn(true);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE, true);
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_MAKE_HOSTILE);
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE);
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE, true);
        fleet.getMemoryWithoutUpdate().set(
                FRIENDLY_DRONE_PATROL_MARKER, true);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY);
        fleet.getMemoryWithoutUpdate().set(
            MemFlags.MEMORY_KEY_NO_JUMP, true);

        // The restored drones roam the system; they are not an assault force
        // or a Wall-centered reinforcement ring. Native movement owns them.
        if (fleet.getCurrentAssignment() == null
                || fleet.getCurrentAssignment().getTarget() != system.getCenter()
                || fleet.getCurrentAssignment().getAssignment()
                        != com.fs.starfarer.api.campaign.FleetAssignment
                                .PATROL_SYSTEM) {
            fleet.clearAssignments();
            fleet.addAssignment(
                    com.fs.starfarer.api.campaign.FleetAssignment
                            .PATROL_SYSTEM,
                    system.getCenter(), 1000000f,
                    "roaming the Odyssey Expanse");
        }
    }

    /** Keeps Ashen Verge's autonomous Derelicts on the player's side. */
    private static void configureAshenAlliedFleet(CampaignFleetAPI fleet) {
        fleet.setFaction(IthacaSectionEncounter.COMBAT_GUARD_FACTION_ID, true);
        convertFleetToCombatGuard(fleet);
        fleet.setNoAutoDespawn(true);
        fleet.setAbortDespawn(true);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE, true);
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_MAKE_HOSTILE);
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE);
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_NO_JUMP, true);
    }

    private static void markLiveObjectivesImportant(StarSystemAPI system) {
        int stage = getCurrentLaborStage();
        boolean reportPending = isLaborReportPending(stage);
        for (int i = 0; i < MOTHERSHIP_NAMES.length; i++) {
            SectorEntityToken entity = system.getEntityById(
                    MOTHERSHIP_PREFIX + i);
            if (isMothershipPOI(entity)) {
                if (stage == LABOR_MOTHERSHIPS && !reportPending
                        && !isFriendlyMothership(entity)) {
                    entity.setDiscoverable(null);
                    entity.setSensorProfile(null);
                    entity.setExtendedDetectedAtRange(null);
                    Misc.makeImportant(entity, TRIAL_IMPORTANT);
                } else {
                    Misc.makeUnimportant(entity, TRIAL_IMPORTANT);
                }
            }
        }
        SectorEntityToken wall = system.getEntityById(
                DriftingWallEncounter.FLEET_ID);
        if (DriftingWallEncounter.isDriftingWall(wall)
                && stage == LABOR_WALL && !reportPending) {
            Misc.makeImportant(wall, TRIAL_IMPORTANT);
        } else if (DriftingWallEncounter.isDriftingWall(wall)) {
            Misc.makeUnimportant(wall, TRIAL_IMPORTANT);
        }
        for (SectorEntityToken station :
                OdysseyExpanseSystem.getRemainingResearchStations()) {
            if (stage == LABOR_RESEARCH && !reportPending) {
                Misc.makeImportant(station, TRIAL_IMPORTANT);
            } else {
                Misc.makeUnimportant(station, TRIAL_IMPORTANT);
            }
        }
        StarSystemAPI devoured = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.DEVOURED_REACH_ID);
        SectorEntityToken oldStrike = devoured == null ? null
                : devoured.getEntityById(
                        TroyArrivalScript.STARVING_THREAT_FLEET_ID);
        if (oldStrike instanceof CampaignFleetAPI
                && OdysseyPredatorScript.isAuthoredStarvingThreatFleet(
                        (CampaignFleetAPI) oldStrike)) {
            Misc.makeUnimportant(oldStrike, TRIAL_IMPORTANT);
        }
        for (SectorEntityToken invasion
                : OdysseyPredatorScript.getFinalLaborObjectives()) {
            if (stage == LABOR_GAUTAMA) {
                Misc.makeImportant(invasion, TRIAL_IMPORTANT);
            } else {
                Misc.makeUnimportant(invasion, TRIAL_IMPORTANT);
            }
        }
    }

    private static void revealFinalLaborIfReady() {
        if (areInitialLaborsComplete()) {
            memory().set(FINAL_LABOR_REVEALED, true);
        }
    }

    /** Repairs saves where completing the Mara field work leaked the brief. */
    private static void repairPrematureFinalLaborBriefing() {
        if (memory().getBoolean(FINAL_LABOR_FORCED_FOR_TESTING)) return;
        // The original flag was set as soon as Labor IV's prerequisites were
        // inferred, even if the player never returned to Ithaca. Preserve it
        // only when the player has already made material Labor V progress.
        if (!memory().getBoolean(FINAL_LABOR_BRIEFED)
                && memory().getBoolean(LEGACY_FINAL_LABOR_BRIEFED)
                && memory().getBoolean(LABOR_IV_REPORTED)
                && (OdysseyPredatorScript.getFinalLaborInvasionProgress() > 0
                    || isGautamaDefeated())) {
            memory().set(FINAL_LABOR_BRIEFED, true);
        }
        if (MenelausSensorLabor.isComplete()
                && !memory().getBoolean(LABOR_IV_REPORTED)) {
            memory().unset(FINAL_LABOR_REVEALED);
            memory().unset(FINAL_LABOR_BRIEFED);
        }
    }

    private static String getLaborReportFlag(int stage) {
        if (stage == LABOR_MOTHERSHIPS) return LABOR_I_REPORTED;
        if (stage == LABOR_RESEARCH) return LABOR_II_REPORTED;
        if (stage == LABOR_WALL) return LABOR_III_REPORTED;
        if (stage == LABOR_SENSOR) return LABOR_IV_REPORTED;
        return null;
    }

    /**
     * Preserves the stage reached by saves made before explicit report gates.
     * Only Labors preceding the old active stage are treated as reported; the
     * old active Labor still requires its new debrief once its field work is
     * complete.
     */
    private static void reconcileLegacyLaborReports() {
        if (Global.getSector() == null
                || !memory().getBoolean(ACCEPTED)
                || memory().getBoolean(LABOR_REPORT_MIGRATION)) {
            return;
        }

        int legacyStage;
        if (!areMothershipsCleared()) {
            legacyStage = LABOR_MOTHERSHIPS;
        } else if (!isResearchLaborComplete()
                && !meetsHistoricalResearchSalvageThreshold()) {
            legacyStage = LABOR_RESEARCH;
        } else if (!isWallFriendly()) {
            legacyStage = LABOR_WALL;
        } else if (!MenelausSensorLabor.isComplete()) {
            legacyStage = LABOR_SENSOR;
        } else if (!isGautamaDefeated()
                || !memory().getBoolean(FINAL_DEBRIEF_COMPLETE)) {
            legacyStage = LABOR_GAUTAMA;
        } else {
            legacyStage = LABOR_COMPLETE;
        }

        if (legacyStage != LABOR_MOTHERSHIPS) {
            memory().set(LABOR_I_REPORTED, true);
        }
        if (legacyStage == LABOR_WALL || legacyStage == LABOR_SENSOR
                || legacyStage == LABOR_GAUTAMA
                || legacyStage == LABOR_COMPLETE) {
            memory().set(LABOR_II_REPORTED, true);
        }
        if (legacyStage == LABOR_SENSOR || legacyStage == LABOR_GAUTAMA
                || legacyStage == LABOR_COMPLETE) {
            memory().set(LABOR_III_REPORTED, true);
        }
        if (legacyStage == LABOR_GAUTAMA || legacyStage == LABOR_COMPLETE) {
            memory().set(LABOR_IV_REPORTED, true);
        }
        memory().set(LABOR_REPORT_MIGRATION, true);
    }

    /** Historical pre-component Labor II completion used five of six POIs. */
    private static boolean meetsHistoricalResearchSalvageThreshold() {
        return getResearchStationsResearched()
                >= (int) Math.ceil(getResearchStationTotal() * 0.70d);
    }

    private static com.fs.starfarer.api.campaign.rules.MemoryAPI memory() {
        return Global.getSector().getMemoryWithoutUpdate();
    }
}
