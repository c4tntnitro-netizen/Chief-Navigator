package chiefnavigator.quest;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI;
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI;
import com.fs.starfarer.api.campaign.ai.NavigationModulePlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.lwjgl.util.vector.Vector2f;

/** Headless checks for save-safe Derelict jump-point avoidance. */
public final class DerelictJumpAvoidanceRegression {
    interface Call { Object invoke(String name, Object[] args); }

    static final class NavigationCalls {
        int avoidCount;
        int clearCount;
        int assignmentMutationCount;
        int movementMutationCount;
        int velocityMutationCount;
        final List<Object> avoidedTargets = new ArrayList<Object>();
        float minimum;
        float maximum;
        float duration;
    }

    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(), new Class<?>[] {type},
                (proxy, method, args) -> {
                    if (method.getName().equals("hashCode")) {
                        return System.identityHashCode(proxy);
                    }
                    if (method.getName().equals("equals")) {
                        return proxy == args[0];
                    }
                    Object value = call.invoke(method.getName(), args);
                    if (value != null) return value;
                    Class<?> result = method.getReturnType();
                    if (result == boolean.class) return false;
                    if (result == float.class) return 0f;
                    if (result == int.class) return 0;
                    if (result == long.class) return 0L;
                    return null;
                }));
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static void checkNear(float actual, float expected, String message) {
        check(Math.abs(actual - expected) < 0.001f,
                message + ": expected " + expected + ", got " + actual);
    }

    static JumpPointAPI jump(String id, float x, float y) {
        Vector2f location = new Vector2f(x, y);
        return mock(JumpPointAPI.class, (name, args) -> {
            if (name.equals("getId")) return id;
            if (name.equals("getLocation")) return location;
            return null;
        });
    }

    static StarSystemAPI system(List<JumpPointAPI> jumps) {
        return mock(StarSystemAPI.class, (name, args) ->
                name.equals("getJumpPoints") ? jumps : null);
    }

    static CampaignFleetAPI fleet(
            String id,
            StarSystemAPI system,
            CampaignFleetAIAPI ai,
            boolean station,
            NavigationCalls calls) {
        MemoryAPI memory = mock(MemoryAPI.class, (name, args) ->
                name.equals("getBoolean")
                        && OdysseyPredatorScript
                                .AUTHORED_MOBILE_DERELICT_MARKER
                                .equals(args[0]) ? true : null);
        return mock(CampaignFleetAPI.class, (name, args) -> {
            if (name.equals("getId")) return id;
            if (name.equals("getMemoryWithoutUpdate")) return memory;
            if (name.equals("getContainingLocation")) return system;
            if (name.equals("getAI")) return ai;
            if (name.equals("isStationMode")) return station;
            if (name.equals("clearAssignments")
                    || name.equals("addAssignment")) {
                calls.assignmentMutationCount++;
            }
            if (name.equals("setLocation")
                    || name.equals("setMoveDestination")
                    || name.equals("setMoveDestinationOverride")) {
                calls.movementMutationCount++;
            }
            return null;
        });
    }

    static CampaignFleetAPI movableFleet(
            String id,
            StarSystemAPI system,
            Vector2f location,
            boolean station,
            NavigationCalls calls) {
        MemoryAPI memory = mock(MemoryAPI.class, (name, args) ->
                name.equals("getBoolean")
                        && OdysseyPredatorScript
                                .AUTHORED_MOBILE_DERELICT_MARKER
                                .equals(args[0]) ? true : null);
        return mock(CampaignFleetAPI.class, (name, args) -> {
            if (name.equals("getId")) return id;
            if (name.equals("getMemoryWithoutUpdate")) return memory;
            if (name.equals("getContainingLocation")) return system;
            if (name.equals("getLocation")) return location;
            if (name.equals("isStationMode")) return station;
            if (name.equals("setLocation")) {
                calls.movementMutationCount++;
                location.set((Float) args[0], (Float) args[1]);
            }
            if (name.equals("setVelocity")) {
                calls.velocityMutationCount++;
            }
            if (name.equals("clearAssignments")
                    || name.equals("addAssignment")) {
                calls.assignmentMutationCount++;
            }
            return null;
        });
    }

    static float distance(Vector2f point, SectorEntityToken jump) {
        float dx = point.x - jump.getLocation().x;
        float dy = point.y - jump.getLocation().y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public static void main(String[] args) {
        JumpPointAPI origin = jump("origin", 0f, 0f);
        StarSystemAPI singleJump = system(Arrays.asList(origin));

        Vector2f source = new Vector2f(9000f, 3000f);
        Vector2f far = OdysseyPredatorScript.projectOutsideJumpClearance(
                singleJump, source, 5500f);
        checkNear(far.x, source.x, "Far anchor x changed");
        checkNear(far.y, source.y, "Far anchor y changed");
        check(far != source, "Projection should not mutate or alias its input");

        Vector2f near = OdysseyPredatorScript.projectOutsideJumpClearance(
                singleJump, new Vector2f(100f, 0f), 5500f);
        check(distance(near, origin) >= 5500f,
                "Near anchor remained inside the jump exclusion zone");

        Vector2f centered = OdysseyPredatorScript.projectOutsideJumpClearance(
                singleJump, new Vector2f(0f, 0f), 5500f);
        check(Float.isFinite(centered.x) && Float.isFinite(centered.y),
                "Exact-center projection produced a non-finite coordinate");
        check(distance(centered, origin) >= 5500f,
                "Exact-center anchor remained inside the exclusion zone");

        JumpPointAPI overlapLeft = jump("overlap_left", 0f, 0f);
        JumpPointAPI overlapRight = jump("overlap_right", 8000f, 0f);
        List<JumpPointAPI> overlappingJumps = Arrays.asList(
                overlapLeft, overlapRight);
        Vector2f overlap = OdysseyPredatorScript.projectOutsideJumpClearance(
                system(overlappingJumps), new Vector2f(4000f, 0f), 5500f);
        for (JumpPointAPI jump : overlappingJumps) {
            check(distance(overlap, jump) >= 5500f,
                    "Overlapping jump zones defeated anchor projection");
        }
        check(Float.isFinite(overlap.x) && Float.isFinite(overlap.y),
                "Overlapping-zone fallback produced non-finite coordinates");
        float overlapDx = overlap.x - 4000f;
        float overlapDy = overlap.y;
        check(overlapDx * overlapDx + overlapDy * overlapDy < 6000f * 6000f,
                "Overlapping-zone projection moved a live point unnecessarily far");

        JumpPointAPI lastLightMain = jump(
                "chief_navigator_odyssey_last_light_local_jump", 0f, 1800f);
        JumpPointAPI lastLightInner = jump(
                "chief_navigator_last_light_inner", -7000f, -3500f);
        JumpPointAPI lastLightFringe = jump(
                "chief_navigator_last_light_fringe", 7000f, -3500f);
        List<JumpPointAPI> lastLightJumps = Arrays.asList(
                lastLightMain, lastLightInner, lastLightFringe);
        Vector2f legacyLastLightAnchor =
                new Vector2f(6500f, -4600f);
        Vector2f repairedLastLightAnchor =
                OdysseyPredatorScript.projectOutsideJumpClearance(
                        system(lastLightJumps),
                        legacyLastLightAnchor,
                        OdysseyPredatorScript.DERELICT_ANCHOR_CLEARANCE);
        for (JumpPointAPI jump : lastLightJumps) {
            check(distance(repairedLastLightAnchor, jump)
                            >= OdysseyPredatorScript.DERELICT_ANCHOR_CLEARANCE,
                    "Legacy Last Light patrol anchor was not cleared");
        }
        checkNear(legacyLastLightAnchor.x, 6500f,
                "Projection mutated the legacy Last Light anchor x");
        checkNear(legacyLastLightAnchor.y, -4600f,
                "Projection mutated the legacy Last Light anchor y");
        Vector2f repairedAgain =
                OdysseyPredatorScript.projectOutsideJumpClearance(
                        system(lastLightJumps),
                        repairedLastLightAnchor,
                        OdysseyPredatorScript.DERELICT_ANCHOR_CLEARANCE);
        check(distance(repairedAgain, jump("repaired", repairedLastLightAnchor.x,
                        repairedLastLightAnchor.y)) < 0.01f,
                "Jump clearance projection was not idempotent");

        check(OdysseyPredatorScript.isAuthoredMobileDerelict(fleet(
                        "chief_navigator_odyssey_rogue_derelict_0",
                        singleJump, null, false, new NavigationCalls())),
                "Rogue flotilla was not recognized");
        check(OdysseyPredatorScript.isAuthoredMobileDerelict(fleet(
                        "chief_navigator_wall_derelict_roamer_0",
                        singleJump, null, false, new NavigationCalls())),
                "Wall roamer was not recognized");
        check(OdysseyPredatorScript.isAuthoredMobileDerelict(fleet(
                        "chief_navigator_ashen_trial_roamer_0",
                        singleJump, null, false, new NavigationCalls())),
                "Ashen roamer was not recognized");
        check(OdysseyPredatorScript.isAuthoredMobileDerelict(fleet(
                        "chief_navigator_unsealed_guard_0",
                        singleJump, null, false, new NavigationCalls())),
                "Unsealed guard was not recognized");
        check(!OdysseyPredatorScript.isAuthoredMobileDerelict(fleet(
                        "chief_navigator_odyssey_research_station_0",
                        singleJump, null, false, new NavigationCalls())),
                "Fixed Derelict station was incorrectly treated as a patrol");

        JumpPointAPI second = jump("second", 12000f, 0f);
        StarSystemAPI twoJumps = system(Arrays.asList(origin, second));
        NavigationCalls calls = new NavigationCalls();
        NavigationModulePlugin navigation = mock(
                NavigationModulePlugin.class, (name, values) -> {
                    if (name.equals("avoidEntity")) {
                        calls.avoidCount++;
                        calls.avoidedTargets.add(values[0]);
                        calls.minimum = (Float) values[1];
                        calls.maximum = (Float) values[2];
                        calls.duration = (Float) values[3];
                    } else if (name.equals("clearAvoidList")) {
                        calls.clearCount++;
                    }
                    return null;
                });
        ModularFleetAIAPI modularAI = mock(
                ModularFleetAIAPI.class, (name, values) ->
                        name.equals("getNavModule") ? navigation : null);
        CampaignFleetAPI mobile = fleet(
                "chief_navigator_odyssey_rogue_derelict_1",
                twoJumps, modularAI, false, calls);
        OdysseyPredatorScript.refreshDerelictJumpAvoidance(
                mobile, twoJumps);
        check(calls.avoidCount == 2,
                "Navigation avoidance was not applied to every jump point");
        check(calls.avoidedTargets.get(0) == origin
                        && calls.avoidedTargets.get(1) == second,
                "Navigation avoidance did not preserve jump-point identity");
        checkNear(calls.minimum,
                OdysseyPredatorScript.DERELICT_JUMP_AVOIDANCE_MIN,
                "Wrong full-strength avoidance radius");
        checkNear(calls.maximum,
                OdysseyPredatorScript.DERELICT_JUMP_AVOIDANCE_MAX,
                "Wrong outer avoidance radius");
        checkNear(calls.duration, 1f, "Wrong avoidance refresh duration");
        check(calls.clearCount == 0,
                "Refresh cleared vanilla or third-party avoidance state");
        check(calls.assignmentMutationCount == 0,
                "Refresh mutated the patrol's existing assignments");
        check(calls.movementMutationCount == 0,
                "Refresh teleported or overrode the patrol's movement");

        CampaignFleetAPI station = fleet(
                "chief_navigator_odyssey_rogue_derelict_2",
                twoJumps, modularAI, true, calls);
        OdysseyPredatorScript.refreshDerelictJumpAvoidance(
                station, twoJumps);
        check(calls.avoidCount == 2,
                "Station-mode fleet received mobile patrol avoidance");

        CampaignFleetAIAPI nonModularAI = mock(
                CampaignFleetAIAPI.class, (name, values) -> null);
        CampaignFleetAPI nonModular = fleet(
                "chief_navigator_odyssey_rogue_derelict_3",
                twoJumps, nonModularAI, false, calls);
        OdysseyPredatorScript.refreshDerelictJumpAvoidance(
                nonModular, twoJumps);
        check(calls.avoidCount == 2,
                "Non-modular fleet received unsupported navigation calls");

        StarSystemAPI foreignSystem = system(Arrays.asList(origin));
        OdysseyPredatorScript.refreshDerelictJumpAvoidance(
                mobile, foreignSystem);
        check(calls.avoidCount == 2,
                "Fleet received avoidance for a foreign containing location");

        NavigationCalls clearCalls = new NavigationCalls();
        Vector2f campingLocation = new Vector2f(100f, 0f);
        CampaignFleetAPI camping = movableFleet(
                "chief_navigator_odyssey_rogue_derelict_4",
                twoJumps, campingLocation, false, clearCalls);
        check(OdysseyPredatorScript.clearDerelictJumpArrivalZone(
                        camping, twoJumps),
                "Patrol camping a jump point was not relocated");
        for (SectorEntityToken jump : twoJumps.getJumpPoints()) {
            check(distance(campingLocation, jump)
                            >= OdysseyPredatorScript
                                    .DERELICT_JUMP_ARRIVAL_CLEARANCE,
                    "Relocated patrol remained inside an arrival bubble");
        }
        check(clearCalls.movementMutationCount == 1,
                "Arrival clearance did not perform exactly one relocation");
        check(clearCalls.assignmentMutationCount == 0,
                "Arrival clearance mutated the patrol's assignments");
        check(clearCalls.velocityMutationCount == 0,
                "Arrival clearance overwrote the patrol's velocity");
        check(!OdysseyPredatorScript.clearDerelictJumpArrivalZone(
                        camping, twoJumps),
                "Already-clear patrol was relocated a second time");
        check(clearCalls.movementMutationCount == 1,
                "Arrival clearance produced repeated position snapping");

        NavigationCalls hysteresisCalls = new NavigationCalls();
        CampaignFleetAPI outsideDanger = movableFleet(
                "chief_navigator_odyssey_rogue_derelict_5",
                twoJumps, new Vector2f(5000f, 0f), false, hysteresisCalls);
        check(!OdysseyPredatorScript.clearDerelictJumpArrivalZone(
                        outsideDanger, twoJumps),
                "Patrol outside the danger radius was needlessly relocated");
        check(hysteresisCalls.movementMutationCount == 0,
                "Arrival clearance ignored its relocation hysteresis");

        NavigationCalls stationClearCalls = new NavigationCalls();
        CampaignFleetAPI campingStation = movableFleet(
                "chief_navigator_odyssey_rogue_derelict_6",
                twoJumps, new Vector2f(100f, 0f), true, stationClearCalls);
        check(!OdysseyPredatorScript.clearDerelictJumpArrivalZone(
                        campingStation, twoJumps),
                "Station-mode Derelict was relocated");
        check(stationClearCalls.movementMutationCount == 0,
                "Station-mode guard failed to preserve its position");

        System.out.println("DERELICT JUMP AVOIDANCE REGRESSION OK");
    }
}
