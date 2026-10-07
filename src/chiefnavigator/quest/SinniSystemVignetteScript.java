package chiefnavigator.quest;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI;
import com.fs.starfarer.api.campaign.ai.TacticalModulePlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.StarTypes;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import chiefnavigator.campaign.DevouredRingEntityPlugin;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.lwjgl.util.vector.Vector2f;

/**
 * Opens Sinni's one-time stellar vignettes after entering a safe system.
 * An unseen category remains eligible while a hostile fleet is tracking the
 * player; merely opening the scene never consumes it.
 */
public final class SinniSystemVignetteScript implements EveryFrameScript {
    private static final String COMPLETED_PREFIX =
            "$chief_navigator_sinni_vignette_complete_";
    public static final String ALPHA_ODYSSEY_ARRIVAL_PENDING =
            "$chief_navigator_alpha_odyssey_arrival_pending_v1";
    private static final String GAN_EDEN_SYSTEM_ID =
            "ship_trophy_gan_eden";
    private static final String GAN_EDEN_SYSTEM_NAME = "Gan Eden";
    private static final String GAN_EDEN_TRANSIT_SYSTEM_ID =
            "ship_trophy_gan_eden_power_transit_system";
    private static final String GAN_EDEN_TRANSIT_SYSTEM_NAME =
            "Power Transit Gate - Gan Eden";
    private static final String LAST_REQUEST_ACTIVE =
            "$chief_navigator_sinni_last_request_active_v1";
    private static final String LAST_REQUEST_COMPLETE =
            "$chief_navigator_sinni_one_more_horizon_complete_v1";
    private static final String LAST_REQUEST_CATEGORY =
            "$chief_navigator_sinni_last_request_category_v1";
    private static final String LAST_REQUEST_SYSTEM =
            "$chief_navigator_sinni_last_request_system_v1";
    private static final String LAST_REQUEST_DEFERRED_SYSTEM =
            "$chief_navigator_sinni_last_request_deferred_system_v1";
    private static final String ALL_TYPES_COMPLETION_PENDING =
            "$chief_navigator_sinni_all_types_completion_pending_v1";
    private static final String ALL_TYPES_COMPLETION_QUEUED_AT =
            "$chief_navigator_sinni_all_types_completion_queued_at_v1";
    private static final float ALL_TYPES_COMPLETION_DELAY_DAYS = 1f;
    static final String LAST_REQUEST_IMPORTANCE =
            "chief_navigator_sinni_one_more_horizon";

    enum Category {
        /** Bespoke Troy Terminus arrival; not an exploration category. */
        ALPHA_ODYSSEY("alpha_odyssey"),
        /** Bespoke first arrival in Avici; not an exploration category. */
        AVICI("avici"),
        /** Authored Orion Knot arrivals; excluded from the stellar chart. */
        SANZU("sanzu"),
        /** Retired arrivals; retain identifiers for historical completion flags. */
        ASHEN_VERGE("ashen_verge"),
        DEVOURED_REACH("devoured_reach"),
        NEBULA("nebula"),
        BLACK_HOLE("black_hole"),
        NEUTRON_STAR("neutron_star"),
        /** Retained only so completed old saves keep their fuel-use reward. */
        BROWN_DWARF("brown_dwarf"),
        WHITE_DWARF("white_dwarf"),
        MAIN_SEQUENCE("main_sequence"),
        GIANT_STAR("giant_star"),
        /** Legacy completion flag migrated into GIANT_STAR on load. */
        MASSIVE_STAR("massive_star"),
        TRINARY("trinary"),
        /** Retired one-category-left scene; identifier retained for old saves. */
        LAST_REQUEST("last_request"),
        ALL_TYPES_COMPLETE("all_types_complete");

        final String id;

        Category(String id) {
            this.id = id;
        }
    }

    static final class Candidate {
        final Category category;
        final String subtype;
        final String sceneId;
        final LastRequestTarget lastRequestTarget;

        Candidate(Category category, String subtype) {
            this(category, subtype, category.id, null);
        }

        Candidate(Category category, String subtype, String sceneId) {
            this(category, subtype, sceneId, null);
        }

        Candidate(
                Category category,
                String subtype,
                String sceneId,
                LastRequestTarget lastRequestTarget) {
            this.category = category;
            this.subtype = subtype;
            this.sceneId = sceneId;
            this.lastRequestTarget = lastRequestTarget;
        }
    }

    static final class LastRequestTarget {
        final Category category;
        final StarSystemAPI system;
        final float distanceLy;

        LastRequestTarget(
                Category category,
                StarSystemAPI system,
                float distanceLy) {
            this.category = category;
            this.system = system;
            this.distanceLy = distanceLy;
        }
    }

    private static final Category[] EXPLORATION_CATEGORIES = {
        Category.NEBULA,
        Category.BLACK_HOLE,
        Category.NEUTRON_STAR,
        Category.WHITE_DWARF,
        Category.MAIN_SEQUENCE,
        Category.GIANT_STAR,
        Category.TRINARY
    };

    private final IntervalUtil interval = new IntervalUtil(0.15f, 0.25f);
    private StarSystemAPI observedSystem;
    private int settledIntervals;

    @Override
    public void advance(float amount) {
        if (Global.getSector() == null) return;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player == null) return;

        interval.advance(amount);
        if (!interval.intervalElapsed()) return;
        migrateLegacyCategoryFlags();
        SinniVignetteRewards.advance(player);
        syncOneMoreHorizon();

        StarSystemAPI system = currentSystem(player);
        if (!isSinniInFleet(player) || system == null
                || player.isInHyperspaceTransition()
                || player.getBattle() != null) {
            observedSystem = null;
            settledIntervals = 0;
            return;
        }

        // Let campaign AI acquire its tactical target before evaluating safety.
        if (system != observedSystem) {
            observedSystem = system;
            settledIntervals = 0;
            return;
        }
        if (settledIntervals < 1) {
            settledIntervals++;
            return;
        }

        // The authored Ithaca first-contact beat owns the final approach. In
        // particular, this prevents any generic stellar vignette from winning
        // the same campaign frame after an eastern-system arrival.
        if (FobIthacaApproachScript.shouldTakeDialoguePriority(player)) {
            return;
        }

        if (Global.getSector().getCampaignUI().isShowingDialog()
                || Global.getSector().getCampaignUI().isShowingMenu()
                || isTrackedByHostileFleet(player)) {
            return;
        }

        // Authored regional arrivals take precedence over stellar vignettes
        // and their epilogue. In particular, Avici's first view should not be
        // displaced by One More Horizon becoming ready on the same frame.
        Candidate bespoke = classifyBespokeSystem(system);
        if (bespoke != null && !isComplete(bespoke.category)) {
            showVignette(bespoke, system, player);
            return;
        }

        if (isAllTypesCompletionPending()) {
            if (!isAllTypesCompletionReady()) return;
            showVignette(new Candidate(
                    Category.ALL_TYPES_COMPLETE,
                    "all_types_complete"), system, player);
            return;
        }

        Candidate candidate = classifySystem(system);
        if (candidate == null || isComplete(candidate.category)) return;

        showVignette(candidate, system, player);
    }

    private static void showVignette(
            Candidate candidate,
            StarSystemAPI system,
            CampaignFleetAPI player) {
        Global.getSector().getCampaignUI().showInteractionDialog(
                new SinniSystemVignetteInteraction(
                        candidate,
                        displayName(system)),
                player);
    }

    static boolean isSinniInFleet(CampaignFleetAPI player) {
        if (!Global.getSector().getMemoryWithoutUpdate().getBoolean(
                SinniBarEvent.STARTED)) {
            return false;
        }
        return Global.getSector().getImportantPeople().getPerson(
                SinniContact.PERSON_ID) != null
                && player.getFleetData().getOfficerData(
                        Global.getSector().getImportantPeople().getPerson(
                                SinniContact.PERSON_ID)) != null;
    }

    private static StarSystemAPI currentSystem(CampaignFleetAPI player) {
        return player.getContainingLocation() instanceof StarSystemAPI
                ? (StarSystemAPI) player.getContainingLocation() : null;
    }

    static String displayName(StarSystemAPI system) {
        String name = system.getName();
        if (name == null || name.trim().length() == 0) {
            name = system.getBaseName();
        }
        return name == null || name.trim().length() == 0
                ? "this system" : name;
    }

    static Candidate classifySystem(StarSystemAPI system) {
        if (system == null) return null;
        Candidate bespoke = classifyBespokeSystem(system);
        if (bespoke != null) return bespoke;
        if (!isExplorationVignetteSystem(system)) return null;
        if (isTrinarySystem(system)) {
            return new Candidate(Category.TRINARY, "trinary");
        }
        if (Boolean.TRUE.equals(system.hasSystemwideNebula())
                || system.getType() == StarSystemGenerator.StarSystemType.NEBULA) {
            return new Candidate(Category.NEBULA, "nebula");
        }
        if (system.hasBlackHole()) {
            return new Candidate(Category.BLACK_HOLE, "black_hole");
        }
        if (system.hasPulsar()) {
            return new Candidate(Category.NEUTRON_STAR, "neutron_star");
        }

        PlanetAPI star = system.getStar();
        if (star == null) return null;
        String type = star.getTypeId();
        if (StarTypes.BLACK_HOLE.equals(type)) {
            return new Candidate(Category.BLACK_HOLE, "black_hole");
        }
        if (StarTypes.NEUTRON_STAR.equals(type)) {
            return new Candidate(Category.NEUTRON_STAR, "neutron_star");
        }
        if (StarTypes.BROWN_DWARF.equals(type)) {
            return null;
        }
        if (StarTypes.WHITE_DWARF.equals(type)) {
            return new Candidate(Category.WHITE_DWARF, "white_dwarf");
        }
        if (StarTypes.RED_DWARF.equals(type)) {
            return new Candidate(Category.MAIN_SEQUENCE, "red_dwarf");
        }
        if (StarTypes.ORANGE.equals(type)) {
            return new Candidate(Category.MAIN_SEQUENCE, "orange_star");
        }
        if (StarTypes.YELLOW.equals(type)) {
            return new Candidate(Category.MAIN_SEQUENCE, "yellow_star");
        }
        if (StarTypes.ORANGE_GIANT.equals(type)) {
            return new Candidate(Category.GIANT_STAR, "orange_giant");
        }
        if (StarTypes.RED_GIANT.equals(type)) {
            return new Candidate(Category.GIANT_STAR, "red_giant");
        }
        if (StarTypes.BLUE_GIANT.equals(type)) {
            return new Candidate(
                    Category.GIANT_STAR, "blue_giant", "massive_star");
        }
        if (StarTypes.BLUE_SUPERGIANT.equals(type)) {
            return new Candidate(Category.GIANT_STAR,
                    "blue_supergiant", "massive_star");
        }
        if (StarTypes.RED_SUPERGIANT.equals(type)) {
            return new Candidate(Category.GIANT_STAR,
                    "red_supergiant", "massive_star");
        }
        return null;
    }

    private static Candidate classifyBespokeSystem(StarSystemAPI system) {
        if (system == null) return null;
        // Once the Troy crossing owns this system's first-entry scene, keep
        // returning the special category even after completion. This prevents
        // the generic nebula vignette from opening immediately afterward while
        // leaving that vignette unseen for the next ordinary nebula.
        if (isAlphaOdysseySystem(system)
                && isAlphaOdysseyArrivalOwned()) {
            return new Candidate(Category.ALPHA_ODYSSEY, "alpha_odyssey");
        }
        if (matchesSystem(
                system, OdysseyExpanseSystem.MESSINA_ID, "Avici")) {
            return new Candidate(Category.AVICI, "avici");
        }
        if (matchesSystem(system, OdysseyExpanseSystem.SILENT_WAKE_ID, "Sanzu")) {
            return new Candidate(Category.SANZU, "sanzu");
        }
        return null;
    }

    static boolean isTrinarySystem(StarSystemAPI system) {
        return system != null
                && system.getStar() != null
                && system.getSecondary() != null
                && system.getTertiary() != null;
    }

    /** Generic exploration conversations belong only to remote, ordinary systems. */
    static boolean isExplorationVignetteSystem(StarSystemAPI system) {
        if (system == null || OdysseyExpanseSystem.isInside(system)
                || isGanEdenSystem(system)) {
            return false;
        }
        return !system.hasTag(Tags.THEME_CORE)
                && !system.hasTag(Tags.THEME_CORE_POPULATED)
                && !system.hasTag(Tags.THEME_CORE_UNPOPULATED);
    }

    /** Optional Hall of Triumph integration using identifiers only. */
    private static boolean isGanEdenSystem(StarSystemAPI system) {
        return matchesSystem(system, GAN_EDEN_SYSTEM_ID, GAN_EDEN_SYSTEM_NAME)
                || matchesSystem(system, GAN_EDEN_TRANSIT_SYSTEM_ID,
                        GAN_EDEN_TRANSIT_SYSTEM_NAME);
    }

    private static boolean matchesSystem(
            StarSystemAPI system, String id, String name) {
        if (system == null) return false;
        return equalsIgnoreCase(id, system.getOptionalUniqueId())
                || equalsIgnoreCase(id, system.getId())
                || equalsIgnoreCase(name, system.getName())
                || equalsIgnoreCase(name, system.getBaseName());
    }

    private static boolean equalsIgnoreCase(String expected, String actual) {
        return actual != null && expected.equalsIgnoreCase(actual);
    }

    static boolean isTrackedByHostileFleet(CampaignFleetAPI player) {
        LocationAPI location = player == null ? null : player.getContainingLocation();
        if (location == null) return false;
        for (CampaignFleetAPI fleet : location.getFleets()) {
            if (isActivelyTrackingPlayer(fleet, player)) return true;
        }
        return false;
    }

    static boolean isActivelyTrackingPlayer(
            CampaignFleetAPI fleet,
            CampaignFleetAPI player) {
        if (fleet == null || player == null || fleet == player
                || fleet.isEmpty() || fleet.isDespawning()
                || fleet.isStationMode()
                || fleet.getContainingLocation() != player.getContainingLocation()
                || fleet.getBattle() != null || fleet.getAI() == null
                || fleet.getAI().isFleeing()
                || !fleet.isHostileTo(player)) {
            return false;
        }

        CampaignFleetAIAPI ai = fleet.getAI();
        if (ai instanceof ModularFleetAIAPI) {
            TacticalModulePlugin tactical =
                    ((ModularFleetAIAPI) ai).getTacticalModule();
            if (tactical != null) {
                if (tactical.isFleeing() || tactical.isStandingDown()) {
                    return false;
                }
                if (tactical.getTarget() == player
                        || tactical.getPriorityTarget() == player) {
                    return true;
                }
            }
        }

        FleetAssignmentDataAPI assignment = ai.getCurrentAssignment();
        return assignment != null
                && assignment.getTarget() == player
                && (assignment.getAssignment() == FleetAssignment.INTERCEPT
                        || assignment.getAssignment() == FleetAssignment.FOLLOW);
    }

    static boolean isComplete(Category category) {
        return category != null && Global.getSector() != null
                && Global.getSector().getMemoryWithoutUpdate().getBoolean(
                        COMPLETED_PREFIX + category.id);
    }

    static void markComplete(Category category) {
        if (category == null || Global.getSector() == null) return;
        boolean newlyCompleted = !isComplete(category);
        Global.getSector().getMemoryWithoutUpdate().set(
                COMPLETED_PREFIX + category.id, true);
        SinniVignetteRewards.onVignetteCompleted(category);
        if (!memory().getBoolean(LAST_REQUEST_COMPLETE)
                && areAllAvailableSightseeingCategoriesComplete()) {
            if (newlyCompleted && isExplorationCategory(category)) {
                memory().set(ALL_TYPES_COMPLETION_QUEUED_AT,
                        Global.getSector().getClock().getTimestamp());
            }
            queueAllTypesCompletion();
        }
        if (category == Category.ALPHA_ODYSSEY) {
            Global.getSector().getMemoryWithoutUpdate().set(
                    ALPHA_ODYSSEY_ARRIVAL_PENDING, false);
        }
    }

    private static boolean isExplorationCategory(Category category) {
        for (Category active : EXPLORATION_CATEGORIES) {
            if (active == category) return true;
        }
        return false;
    }

    private static LastRequestTarget getStoredLastRequestTarget(
            StarSystemAPI origin) {
        Category category = categoryFromId(
                memory().getString(LAST_REQUEST_CATEGORY));
        StarSystemAPI target = findSystem(
                memory().getString(LAST_REQUEST_SYSTEM));
        if (!isExplorationCategory(category) || target == null) return null;
        float distance = origin == null ? 0f : Misc.getDistanceLY(
                origin.getLocation(), target.getLocation());
        return new LastRequestTarget(category, target, distance);
    }

    private static void storeLastRequestTarget(LastRequestTarget target) {
        if (target == null || target.system == null) return;
        memory().set(LAST_REQUEST_CATEGORY, target.category.id);
        memory().set(LAST_REQUEST_SYSTEM, systemId(target.system));
    }

    static void acceptLastRequest(
            LastRequestTarget target,
            TextPanelAPI text) {
        if (target == null || target.system == null) return;
        storeLastRequestTarget(target);
        memory().set(LAST_REQUEST_ACTIVE, true);
        memory().unset(LAST_REQUEST_DEFERRED_SYSTEM);
        SectorEntityToken objective = objectiveToken(target.system);
        if (objective != null) {
            Misc.makeImportant(objective, LAST_REQUEST_IMPORTANCE);
        }
        SinniOneMoreHorizonIntel.ensureExists(text);
    }

    static void deferLastRequest(StarSystemAPI currentSystem) {
        memory().set(LAST_REQUEST_DEFERRED_SYSTEM, systemId(currentSystem));
    }

    static void completeOneMoreHorizon() {
        StarSystemAPI target = findSystem(
                memory().getString(LAST_REQUEST_SYSTEM));
        SectorEntityToken objective = objectiveToken(target);
        if (objective != null) {
            Misc.makeUnimportant(objective, LAST_REQUEST_IMPORTANCE);
        }
        memory().set(LAST_REQUEST_COMPLETE, true);
        memory().unset(LAST_REQUEST_ACTIVE);
        memory().unset(ALL_TYPES_COMPLETION_PENDING);
        memory().unset(ALL_TYPES_COMPLETION_QUEUED_AT);
        SinniOneMoreHorizonIntel.complete();
    }

    static boolean isOneMoreHorizonActive() {
        return Global.getSector() != null
                && memory().getBoolean(LAST_REQUEST_ACTIVE)
                && !memory().getBoolean(LAST_REQUEST_COMPLETE);
    }

    static boolean isOneMoreHorizonComplete() {
        return Global.getSector() != null
                && memory().getBoolean(LAST_REQUEST_COMPLETE);
    }

    static LastRequestTarget getActiveLastRequestTarget() {
        if (!isOneMoreHorizonActive()) return null;
        return getStoredLastRequestTarget(null);
    }

    /**
     * Returns one real, nearby destination for each unfinished category.
     * A legacy accepted request keeps its stored target only until the next
     * campaign update retires that old detour state and its marker.
     */
    static List<LastRequestTarget> getRemainingSightseeingDestinations() {
        List<LastRequestTarget> ordered = new ArrayList<>();
        if (Global.getSector() == null
                || Global.getSector().getStarSystems() == null) {
            return ordered;
        }

        Vector2f origin = null;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player != null) origin = player.getLocationInHyperspace();

        Map<Category, LastRequestTarget> nearest =
                new EnumMap<>(Category.class);
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            Candidate candidate = classifySystem(system);
            if (candidate == null
                    || !isExplorationCategory(candidate.category)
                    || isComplete(candidate.category)) {
                continue;
            }
            float distance = origin == null ? 0f : Misc.getDistanceLY(
                    origin, system.getLocation());
            LastRequestTarget current = nearest.get(candidate.category);
            if (current == null || distance < current.distanceLy) {
                nearest.put(candidate.category, new LastRequestTarget(
                        candidate.category, system, distance));
            }
        }

        LastRequestTarget active = getActiveLastRequestTarget();
        if (active != null && !isComplete(active.category)) {
            nearest.put(active.category, active);
        }
        for (Category category : EXPLORATION_CATEGORIES) {
            LastRequestTarget target = nearest.get(category);
            if (target != null) ordered.add(target);
        }
        return ordered;
    }

    static boolean isAllTypesCompletionPending() {
        return Global.getSector() != null
                && memory().getBoolean(ALL_TYPES_COMPLETION_PENDING)
                && !memory().getBoolean(LAST_REQUEST_COMPLETE);
    }

    private static void queueAllTypesCompletion() {
        memory().set(ALL_TYPES_COMPLETION_PENDING, true);
        // Persist the clock once, including for old pending saves without a
        // timestamp. Watcher updates and reloads must not restart the day.
        if (!(memory().get(ALL_TYPES_COMPLETION_QUEUED_AT) instanceof Number)) {
            memory().set(ALL_TYPES_COMPLETION_QUEUED_AT,
                    Global.getSector().getClock().getTimestamp());
        }
    }

    private static boolean isAllTypesCompletionReady() {
        Object queuedAt = memory().get(ALL_TYPES_COMPLETION_QUEUED_AT);
        return isAllTypesCompletionPending() && queuedAt instanceof Number
                && Global.getSector().getClock().getElapsedDaysSince(
                        ((Number) queuedAt).longValue())
                                >= ALL_TYPES_COMPLETION_DELAY_DAYS;
    }

    private static void syncOneMoreHorizon() {
        if (Global.getSector() == null) return;
        retireLastRequest();
        if (!memory().getBoolean(SinniBarEvent.STARTED)
                || memory().getBoolean(LAST_REQUEST_COMPLETE)) {
            return;
        }
        SinniOneMoreHorizonIntel.ensureExists(null);
        if (areAllAvailableSightseeingCategoriesComplete()) {
            queueAllTypesCompletion();
        } else {
            // Older accepted requests could queue the finale before a newly
            // available category was visited. The completed chart is decisive.
            memory().unset(ALL_TYPES_COMPLETION_PENDING);
            memory().unset(ALL_TYPES_COMPLETION_QUEUED_AT);
        }
    }

    /** Retires the old detour without consuming sightseeing progress or rewards. */
    private static void retireLastRequest() {
        String targetId = memory().getString(LAST_REQUEST_SYSTEM);
        if (!memory().getBoolean(LAST_REQUEST_ACTIVE)
                && targetId == null
                && memory().getString(LAST_REQUEST_CATEGORY) == null
                && memory().getString(LAST_REQUEST_DEFERRED_SYSTEM) == null) {
            return;
        }
        SectorEntityToken objective = objectiveToken(findSystem(targetId));
        if (objective != null) {
            Misc.makeUnimportant(objective, LAST_REQUEST_IMPORTANCE);
        }
        memory().unset(LAST_REQUEST_ACTIVE);
        memory().unset(LAST_REQUEST_CATEGORY);
        memory().unset(LAST_REQUEST_SYSTEM);
        memory().unset(LAST_REQUEST_DEFERRED_SYSTEM);
    }

    private static void migrateLegacyCategoryFlags() {
        if (Global.getSector() == null || isComplete(Category.GIANT_STAR)
                || !isComplete(Category.MASSIVE_STAR)) {
            return;
        }
        memory().set(COMPLETED_PREFIX + Category.GIANT_STAR.id, true);
    }

    static String categoryDisplayName(Category category) {
        if (category == Category.NEBULA) return "Nebula";
        if (category == Category.BLACK_HOLE) return "Black-hole system";
        if (category == Category.NEUTRON_STAR) return "Neutron-star system";
        if (category == Category.WHITE_DWARF) return "White-dwarf system";
        if (category == Category.MAIN_SEQUENCE) return "Main-sequence system";
        if (category == Category.GIANT_STAR) {
            return "Giant or massive star system";
        }
        if (category == Category.TRINARY) return "Trinary star system";
        return "Stellar system";
    }

    static int getRequiredSightseeingCount() {
        return getAvailableExplorationCategories().size();
    }

    static int getCompletedSightseeingCount() {
        int completed = 0;
        for (Category category : getAvailableExplorationCategories()) {
            if (isComplete(category)) completed++;
        }
        return completed;
    }

    private static EnumSet<Category> getAvailableExplorationCategories() {
        EnumSet<Category> available = EnumSet.noneOf(Category.class);
        if (Global.getSector() == null
                || Global.getSector().getStarSystems() == null) {
            return available;
        }
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            Candidate candidate = classifySystem(system);
            if (candidate != null && isExplorationCategory(
                    candidate.category)) {
                available.add(candidate.category);
            }
        }
        return available;
    }

    private static boolean areAllAvailableSightseeingCategoriesComplete() {
        EnumSet<Category> available = getAvailableExplorationCategories();
        if (available.isEmpty()) return false;
        for (Category category : available) {
            if (!isComplete(category)) return false;
        }
        return true;
    }

    /** Hidden epilogue response unlocked by the Ring's recovered route data. */
    static boolean isDevouredRingPathKnown() {
        return DevouredRingEntityPlugin.hasPathInformation();
    }

    private static Category categoryFromId(String id) {
        if (id == null) return null;
        for (Category category : Category.values()) {
            if (category.id.equals(id)) return category;
        }
        return null;
    }

    private static String systemId(StarSystemAPI system) {
        if (system == null) return "";
        String id = system.getId();
        if (id == null || id.length() == 0) id = system.getOptionalUniqueId();
        return id == null ? "" : id;
    }

    static StarSystemAPI findSystem(String id) {
        if (Global.getSector() == null || id == null || id.length() == 0) {
            return null;
        }
        StarSystemAPI direct = Global.getSector().getStarSystem(id);
        if (direct != null) return direct;
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (id.equals(system.getId())
                    || id.equals(system.getOptionalUniqueId())) {
                return system;
            }
        }
        return null;
    }

    static SectorEntityToken objectiveToken(StarSystemAPI system) {
        if (system == null) return null;
        if (system.getStar() != null) return system.getStar();
        return system.getCenter();
    }

    private static MemoryAPI memory() {
        return Global.getSector().getMemoryWithoutUpdate();
    }

    /** Arms the cinematic only for a committed story-route transit. */
    public static void armAlphaOdysseyArrival() {
        if (Global.getSector() == null
                || isComplete(Category.ALPHA_ODYSSEY)) {
            return;
        }
        Global.getSector().getMemoryWithoutUpdate().set(
                ALPHA_ODYSSEY_ARRIVAL_PENDING, true);
    }

    static boolean isAlphaOdysseySystem(StarSystemAPI system) {
        return system != null && OdysseyExpanseSystem.SYSTEM_ID.equals(
                system.getOptionalUniqueId());
    }

    private static boolean isAlphaOdysseyArrivalOwned() {
        return Global.getSector() != null
                && (Global.getSector().getMemoryWithoutUpdate().getBoolean(
                        ALPHA_ODYSSEY_ARRIVAL_PENDING)
                    || isComplete(Category.ALPHA_ODYSSEY));
    }

    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return false; }
}
