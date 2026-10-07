package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.StarTypes;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lwjgl.util.vector.Vector2f;

/** Actual watcher checks for retiring the one-short offer without losing the finale. */
public final class SinniVignetteCompletionRegression {
    private static final String COMPLETED = "$chief_navigator_sinni_vignette_complete_";
    private static final String ACTIVE = "$chief_navigator_sinni_last_request_active_v1";
    private static final String TARGET_CATEGORY = "$chief_navigator_sinni_last_request_category_v1";
    private static final String TARGET_SYSTEM = "$chief_navigator_sinni_last_request_system_v1";
    private static final String DEFERRED = "$chief_navigator_sinni_last_request_deferred_system_v1";
    private static final String PENDING = "$chief_navigator_sinni_all_types_completion_pending_v1";
    private static final String QUEUED_AT = "$chief_navigator_sinni_all_types_completion_queued_at_v1";
    private static final String COMPLETE = "$chief_navigator_sinni_one_more_horizon_complete_v1";

    interface Call { Object invoke(String name, Object[] args); }

    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[] {type}, (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("hashCode")) return System.identityHashCode(proxy);
                    if (name.equals("equals")) return proxy == args[0];
                    Object result = call.invoke(name, args);
                    if (result != null) return result;
                    Class<?> returns = method.getReturnType();
                    if (returns == boolean.class) return false;
                    if (returns == int.class) return 0;
                    if (returns == float.class) return 0f;
                    if (returns == long.class) return 0L;
                    if (returns == double.class) return 0d;
                    return null;
                }));
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static MemoryAPI memory(Map<String, Object> saved) {
        return mock(MemoryAPI.class, (name, args) -> {
            if (name.equals("getBoolean")) return Boolean.TRUE.equals(saved.get(args[0]));
            if (name.equals("getString")) {
                Object value = saved.get(args[0]);
                return value instanceof String ? value : null;
            }
            if (name.equals("get")) return saved.get(args[0]);
            if (name.equals("contains")) return saved.containsKey(args[0]);
            if (name.equals("set")) saved.put((String) args[0], args[1]);
            if (name.equals("unset")) saved.remove(args[0]);
            if (name.equals("getRequired")) return Collections.emptySet();
            return null;
        });
    }

    static final class SystemFixture {
        final Map<String, Object> saved = new HashMap<>();
        final PlanetAPI star;
        final StarSystemAPI system;

        SystemFixture(String id, String starType, boolean nebula, String tag) {
            MemoryAPI memory = memory(saved);
            Vector2f location = new Vector2f();
            star = mock(PlanetAPI.class, (name, args) -> {
                if (name.equals("getTypeId")) return starType;
                if (name.equals("getMemoryWithoutUpdate")) return memory;
                if (name.equals("getLocation")) return location;
                return null;
            });
            system = mock(StarSystemAPI.class, (name, args) -> {
                if (name.equals("getId") || name.equals("getOptionalUniqueId")) return id;
                if (name.equals("getName") || name.equals("getBaseName")) return id;
                if (name.equals("getStar")) return star;
                if (name.equals("getMemoryWithoutUpdate")) return memory;
                if (name.equals("getLocation")) return location;
                if (name.equals("hasSystemwideNebula")) return nebula;
                if (name.equals("hasBlackHole")) return StarTypes.BLACK_HOLE.equals(starType);
                if (name.equals("hasPulsar")) return StarTypes.NEUTRON_STAR.equals(starType);
                if (name.equals("hasTag")) return tag != null && tag.equals(args[0]);
                if (name.equals("getFleets")) return Collections.emptyList();
                if (name.equals("getType")) return nebula
                        ? StarSystemGenerator.StarSystemType.NEBULA
                        : StarSystemGenerator.StarSystemType.SINGLE;
                return null;
            });
        }
    }

    static final class Fixture {
        final Map<String, Object> saved = new HashMap<>();
        final List<IntelInfoPlugin> intel = new ArrayList<>();
        final List<SinniSystemVignetteScript.Candidate> scenes = new ArrayList<>();
        final List<StarSystemAPI> systems = new ArrayList<>();
        final StatBonus movementSpeed = new StatBonus();
        final StatBonus maxBurn = new StatBonus();
        SinniSystemVignetteScript watcher = new SinniSystemVignetteScript();
        long timestamp = 1_000_000L;
        StarSystemAPI currentSystem;
        boolean dialogShowing;
        boolean transitioning;
        boolean recruited = true;

        Fixture(SystemFixture... fixtures) {
            for (SystemFixture fixture : fixtures) systems.add(fixture.system);
            currentSystem = systems.isEmpty() ? null : systems.get(0);
            saved.put(SinniBarEvent.STARTED, true);
            saved.put(FobIthacaApproachScript.COMPLETE, true);
            Global.setSettings(mock(SettingsAPI.class, (name, args) -> {
                if (name.equals("getColor")) return Color.CYAN;
                if (name.equals("getFloat") && "speedPerBurnLevel".equals(args[0])) return 50f;
                return null;
            }));
            MemoryAPI memory = memory(saved);
            CampaignClockAPI clock = mock(CampaignClockAPI.class, (name, args) -> {
                if (name.equals("getTimestamp")) return timestamp;
                if (name.equals("getElapsedDaysSince")) {
                    return (timestamp - (Long) args[0]) / 1_000_000f;
                }
                return null;
            });
            PersonAPI sinni = mock(PersonAPI.class, (name, args) ->
                    name.equals("getId") ? SinniContact.PERSON_ID : null);
            OfficerDataAPI officer = mock(OfficerDataAPI.class, (name, args) -> null);
            ImportantPeopleAPI people = mock(ImportantPeopleAPI.class, (name, args) ->
                    name.equals("getPerson") && SinniContact.PERSON_ID.equals(args[0])
                            ? sinni : null);
            FleetDataAPI data = mock(FleetDataAPI.class, (name, args) -> {
                if (name.equals("getOfficerData")) return recruited ? officer : null;
                if (name.equals("getMembersListCopy")) return Collections.emptyList();
                return null;
            });
            Map<String, MutableStat> dynamicValues = new HashMap<>();
            DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class, (name, args) -> {
                if (name.equals("getStat")) return dynamicValues.computeIfAbsent(
                        (String) args[0], key -> new MutableStat(1f));
                return null;
            });
            MutableStat acceleration = new MutableStat(1f);
            MutableStat fuelUse = new MutableStat(1f);
            StatBonus sensorProfile = new StatBonus();
            MutableFleetStatsAPI stats = mock(MutableFleetStatsAPI.class, (name, args) -> {
                if (name.equals("getMovementSpeedMod")) return movementSpeed;
                if (name.equals("getFleetwideMaxBurnMod")) return maxBurn;
                if (name.equals("getSensorProfileMod")) return sensorProfile;
                if (name.equals("getAccelerationMult")) return acceleration;
                if (name.equals("getFuelUseHyperMult")) return fuelUse;
                if (name.equals("getDynamic")) return dynamic;
                return null;
            });
            CampaignFleetAPI player = mock(CampaignFleetAPI.class, (name, args) -> {
                if (name.equals("getContainingLocation")) return currentSystem;
                if (name.equals("isInHyperspaceTransition")) return transitioning;
                if (name.equals("getFleetData")) return data;
                if (name.equals("getStats")) return stats;
                if (name.equals("getMemoryWithoutUpdate")) return memory;
                return null;
            });
            IntelManagerAPI manager = mock(IntelManagerAPI.class, (name, args) -> {
                if (name.equals("getIntel")) return intel;
                if (name.equals("addIntel")) intel.add((IntelInfoPlugin) args[0]);
                return null;
            });
            CampaignUIAPI ui = mock(CampaignUIAPI.class, (name, args) -> {
                if (name.equals("isShowingDialog")) return dialogShowing;
                if (name.equals("showInteractionDialog")) {
                    check(args[0] instanceof SinniSystemVignetteInteraction,
                            "Watcher must open the owned vignette shell");
                    try {
                        Field candidate = SinniSystemVignetteInteraction.class
                                .getDeclaredField("candidate");
                        candidate.setAccessible(true);
                        scenes.add((SinniSystemVignetteScript.Candidate) candidate.get(args[0]));
                    } catch (ReflectiveOperationException failure) {
                        throw new AssertionError(failure);
                    }
                    dialogShowing = true;
                    return true;
                }
                return null;
            });
            Global.setSector(mock(SectorAPI.class, (name, args) -> {
                if (name.equals("getMemoryWithoutUpdate")) return memory;
                if (name.equals("getClock")) return clock;
                if (name.equals("getPlayerFleet")) return player;
                if (name.equals("getImportantPeople")) return people;
                if (name.equals("getIntelManager")) return manager;
                if (name.equals("getCampaignUI")) return ui;
                if (name.equals("getStarSystems")) return systems;
                if (name.equals("getStarSystem")) {
                    for (StarSystemAPI system : systems) {
                        if (args[0].equals(system.getId())) return system;
                    }
                }
                return null;
            }));
        }

        void earned(SinniSystemVignetteScript.Category category) {
            saved.put(COMPLETED + category.id, true);
        }

        void ticks(int count) {
            for (int i = 0; i < count; i++) watcher.advance(1f);
        }

        void advanceDays(float days) {
            timestamp += (long) (days * 1_000_000f);
        }
    }

    static SystemFixture nebula() {
        return new SystemFixture("completion_regression_nebula", StarTypes.YELLOW, true, null);
    }

    static SystemFixture blackHole() {
        return new SystemFixture("completion_regression_black_hole", StarTypes.BLACK_HOLE, false, null);
    }

    static void checkOneShortAndCompletion() {
        SystemFixture nebula = nebula();
        SystemFixture blackHole = blackHole();
        Fixture fixture = new Fixture(nebula, blackHole);
        fixture.earned(SinniSystemVignetteScript.Category.NEBULA);
        fixture.ticks(20);
        check(fixture.scenes.isEmpty(), "One remaining category must not open the removed last-request scene");
        check(!SinniSystemVignetteScript.isAllTypesCompletionPending(),
                "One remaining category must not queue the finale");
        check(!fixture.saved.containsKey(QUEUED_AT),
                "A remaining category must not start the completion delay");
        check(fixture.intel.size() == 1, "Sightseeing Intel must remain available without a request");

        SinniSystemVignetteScript.markComplete(SinniSystemVignetteScript.Category.BLACK_HOLE);
        check(SinniSystemVignetteScript.isAllTypesCompletionPending(),
                "The ordinary final-category completion must queue the finale without an accepted request");
        long queuedAt = (Long) fixture.saved.get(QUEUED_AT);
        fixture.ticks(20);
        check(fixture.scenes.isEmpty(),
                "Closing the last category must not immediately open the finale");
        fixture.advanceDays(0.999f);
        fixture.watcher = new SinniSystemVignetteScript();
        fixture.ticks(20);
        check(fixture.scenes.isEmpty() && fixture.saved.get(QUEUED_AT).equals(queuedAt),
                "A reload must preserve the remaining delay and the finale must wait the full day");
        SinniSystemVignetteScript.markComplete(SinniSystemVignetteScript.Category.BLACK_HOLE);
        check(fixture.saved.get(QUEUED_AT).equals(queuedAt),
                "Repeated completion callbacks must not restart the pending day");
        fixture.advanceDays(0.001f);
        fixture.transitioning = true;
        fixture.ticks(4);
        check(fixture.scenes.isEmpty(), "A queued finale must not interrupt hyperspace transit");
        fixture.transitioning = false;
        fixture.dialogShowing = true;
        fixture.ticks(4);
        check(fixture.scenes.isEmpty(), "An elapsed completion delay must still respect an open dialog");
        fixture.dialogShowing = false;
        fixture.ticks(4);
        check(fixture.scenes.size() == 1
                        && fixture.scenes.get(0).category
                                == SinniSystemVignetteScript.Category.ALL_TYPES_COMPLETE,
                "Completing all available categories must open only the completion vignette");
        check(fixture.scenes.get(0).lastRequestTarget == null,
                "The completion vignette must not depend on a one-short destination");
        check(!SinniSystemVignetteScript.isOneMoreHorizonComplete(),
                "Opening the finale must not silently consume its final choice");

        SinniSystemVignetteScript.completeOneMoreHorizon();
        fixture.dialogShowing = false;
        fixture.ticks(30);
        check(fixture.scenes.size() == 1, "The completed finale must never replay");
        check(Boolean.TRUE.equals(fixture.saved.get(COMPLETE))
                        && !fixture.saved.containsKey(PENDING)
                        && !fixture.saved.containsKey(QUEUED_AT),
                "Final choice must persist completion and clear its pending state and delay");
        check(fixture.intel.size() == 1 && fixture.intel.get(0).isEnding(),
                "The existing sightseeing Intel must complete instead of being recreated");
        check(fixture.movementSpeed.getFlatBonus() == 100f
                        && fixture.maxBurn.getFlatBonus() == 1f,
                "The completed chart must keep its existing Burn rewards");
    }

    static void checkLegacyRequestRetirement() {
        SystemFixture nebula = nebula();
        SystemFixture blackHole = blackHole();
        SystemFixture whiteDwarf = new SystemFixture("completion_regression_white_dwarf",
                StarTypes.WHITE_DWARF, false, null);
        Fixture fixture = new Fixture(nebula, blackHole, whiteDwarf);
        fixture.earned(SinniSystemVignetteScript.Category.NEBULA);
        fixture.earned(SinniSystemVignetteScript.Category.BROWN_DWARF);
        fixture.saved.put(ACTIVE, true);
        fixture.saved.put(TARGET_CATEGORY, SinniSystemVignetteScript.Category.BLACK_HOLE.id);
        fixture.saved.put(TARGET_SYSTEM, blackHole.system.getId());
        fixture.saved.put(DEFERRED, nebula.system.getId());
        fixture.saved.put(PENDING, true);
        fixture.saved.put(QUEUED_AT, fixture.timestamp);
        String earnedReward = "$chief_navigator_vignette_main_sequence_reward_spawned_v1";
        fixture.saved.put(earnedReward, true);
        Misc.makeImportant(blackHole.star, SinniSystemVignetteScript.LAST_REQUEST_IMPORTANCE);
        Misc.makeImportant(blackHole.star, "unrelated_regression_mission");

        fixture.ticks(20);
        check(fixture.scenes.isEmpty() && !SinniSystemVignetteScript.isAllTypesCompletionPending(),
                "Two unfinished categories must retire an old premature completion without a popup");
        check(!fixture.saved.containsKey(QUEUED_AT),
                "Retiring a premature finale must also discard its stale delay");
        for (String obsolete : Arrays.asList(ACTIVE, TARGET_CATEGORY, TARGET_SYSTEM, DEFERRED)) {
            check(!fixture.saved.containsKey(obsolete), "Legacy request state must be retired: " + obsolete);
        }
        check(!Misc.isImportantForReason(blackHole.star.getMemoryWithoutUpdate(),
                        SinniSystemVignetteScript.LAST_REQUEST_IMPORTANCE),
                "Retiring the request must remove its exact destination marker");
        check(Misc.isImportantForReason(blackHole.star.getMemoryWithoutUpdate(),
                        "unrelated_regression_mission"),
                "Retiring the request must preserve unrelated mission importance");
        check(Boolean.TRUE.equals(fixture.saved.get(COMPLETED + "brown_dwarf"))
                        && Boolean.TRUE.equals(fixture.saved.get(earnedReward)),
                "Legacy request cleanup must preserve earned completion and reward flags");
        check(fixture.intel.size() == 1 && !fixture.intel.get(0).isEnding(),
                "Request cleanup must preserve the unfinished sightseeing Intel");

        // The formerly accepted target is not necessarily the final unfinished
        // category after a save upgrade or a newly available system. Exercise
        // the callback before the watcher can correct a prematurely queued flag.
        fixture.saved.put(ACTIVE, true);
        fixture.saved.put(TARGET_CATEGORY, SinniSystemVignetteScript.Category.BLACK_HOLE.id);
        fixture.saved.put(TARGET_SYSTEM, blackHole.system.getId());
        SinniSystemVignetteScript.markComplete(SinniSystemVignetteScript.Category.BLACK_HOLE);
        check(!SinniSystemVignetteScript.isAllTypesCompletionPending(),
                "The category callback must not queue a finale just because it completed an old request target");
        fixture.ticks(20);
        check(fixture.scenes.isEmpty() && !SinniSystemVignetteScript.isAllTypesCompletionPending(),
                "One category still remaining after the old target must neither offer nor complete");

        SinniSystemVignetteScript.markComplete(SinniSystemVignetteScript.Category.WHITE_DWARF);
        fixture.ticks(4);
        check(fixture.scenes.isEmpty(), "The real final category must still wait a day after old-request cleanup");
        fixture.advanceDays(1f);
        fixture.ticks(4);
        check(fixture.scenes.size() == 1 && fixture.scenes.get(0).category
                        == SinniSystemVignetteScript.Category.ALL_TYPES_COMPLETE,
                "The real final category must still open the finale after old-request cleanup");
    }

    static void checkAvailableCategoryCensus() {
        SystemFixture nebula = nebula();
        SystemFixture blackHole = blackHole();
        SystemFixture core = new SystemFixture("completion_regression_core",
                StarTypes.WHITE_DWARF, false, Tags.THEME_CORE);
        SystemFixture sanzu = new SystemFixture(OdysseyExpanseSystem.SILENT_WAKE_ID,
                StarTypes.WHITE_DWARF, false, null);
        SystemFixture ashen = new SystemFixture(OdysseyExpanseSystem.ASHEN_VERGE_ID,
                StarTypes.YELLOW, true, null);
        Fixture fixture = new Fixture(nebula, blackHole, core, sanzu, ashen);
        check(SinniSystemVignetteScript.getRequiredSightseeingCount() == 2,
                "Only available ordinary remote categories may count toward chart completion");
        fixture.earned(SinniSystemVignetteScript.Category.NEBULA);
        fixture.earned(SinniSystemVignetteScript.Category.BLACK_HOLE);
        fixture.ticks(4);
        check(fixture.scenes.isEmpty() && SinniSystemVignetteScript.isAllTypesCompletionPending(),
                "A completed legacy chart must queue a delayed finale instead of immediately opening it");
        fixture.advanceDays(1f);
        fixture.ticks(4);
        check(fixture.scenes.size() == 1 && fixture.scenes.get(0).category
                        == SinniSystemVignetteScript.Category.ALL_TYPES_COMPLETE,
                "Absent categories and uncompleted Core/Orion arrivals must not block the finale");

        Fixture empty = new Fixture(core, sanzu, ashen);
        empty.currentSystem = core.system;
        empty.ticks(10);
        check(empty.scenes.isEmpty() && !SinniSystemVignetteScript.isAllTypesCompletionPending(),
                "A Sector with no eligible chart categories must not receive an empty-chart finale");
    }

    static void checkLegacyPendingDelay() {
        Fixture fixture = new Fixture(nebula(), blackHole());
        fixture.earned(SinniSystemVignetteScript.Category.NEBULA);
        fixture.earned(SinniSystemVignetteScript.Category.BLACK_HOLE);
        fixture.saved.put(PENDING, true);
        fixture.ticks(10);
        check(fixture.scenes.isEmpty() && fixture.saved.containsKey(QUEUED_AT),
                "An old pending finale without a timestamp must receive a full-day delay");
        Object queuedAt = fixture.saved.get(QUEUED_AT);
        fixture.advanceDays(0.5f);
        fixture.watcher = new SinniSystemVignetteScript();
        fixture.ticks(10);
        check(fixture.scenes.isEmpty() && queuedAt.equals(fixture.saved.get(QUEUED_AT)),
                "Reloading an old pending finale must not restart its new delay");
        fixture.advanceDays(0.5f);
        fixture.ticks(4);
        check(fixture.scenes.size() == 1 && fixture.scenes.get(0).category
                        == SinniSystemVignetteScript.Category.ALL_TYPES_COMPLETE,
                "An old pending finale must become eligible exactly one campaign day later");
    }

    static void checkStaleDelayOnLastCategory() {
        Fixture fixture = new Fixture(nebula(), blackHole());
        fixture.earned(SinniSystemVignetteScript.Category.NEBULA);
        fixture.saved.put(PENDING, true);
        fixture.saved.put(QUEUED_AT, fixture.timestamp - 3_000_000L);
        SinniSystemVignetteScript.markComplete(SinniSystemVignetteScript.Category.BLACK_HOLE);
        fixture.ticks(10);
        check(fixture.scenes.isEmpty()
                        && fixture.saved.get(QUEUED_AT).equals(fixture.timestamp),
                "The actual last category must start a fresh day even if a legacy pending timer is stale");
        fixture.advanceDays(1f);
        fixture.ticks(4);
        check(fixture.scenes.size() == 1,
                "A corrected stale delay must release the finale after the actual final category's day");
    }

    public static void main(String[] args) {
        SettingsAPI previousSettings = Global.getSettings();
        SectorAPI previousSector = Global.getSector();
        try {
            checkOneShortAndCompletion();
            checkLegacyRequestRetirement();
            checkAvailableCategoryCensus();
            checkLegacyPendingDelay();
            checkStaleDelayOnLastCategory();
            System.out.println("Sinni vignette completion regression checks passed.");
        } finally {
            Global.setSettings(previousSettings);
            Global.setSector(previousSector);
        }
    }
}
