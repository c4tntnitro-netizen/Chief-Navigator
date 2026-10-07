package chiefnavigator.quest;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI;
import com.fs.starfarer.api.campaign.ai.TacticalModulePlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.StarTypes;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lwjgl.util.vector.Vector2f;

/** Headless checks for stellar classification and active pursuit gating. */
public final class SinniSystemVignetteRegression {
    interface Call { Object invoke(String name, Object[] args); }

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

    static StarSystemAPI system(
            String starType,
            boolean nebula,
            boolean blackHole,
            boolean pulsar) {
        return system(null, starType, nebula, blackHole, pulsar);
    }

    static StarSystemAPI system(
            String systemId,
            String starType,
            boolean nebula,
            boolean blackHole,
            boolean pulsar) {
        PlanetAPI star = starType == null ? null : mock(
                PlanetAPI.class,
                (name, args) -> name.equals("getTypeId") ? starType : null);
        MemoryAPI systemMemory = mock(MemoryAPI.class, (name, args) ->
                name.equals("getBoolean") ? true : null);
        return mock(StarSystemAPI.class, (name, args) -> {
            if (name.equals("getOptionalUniqueId") || name.equals("getId")) {
                return systemId;
            }
            if (name.equals("getStar")) return star;
            if (name.equals("getMemoryWithoutUpdate")) return systemMemory;
            if (name.equals("hasSystemwideNebula")) return nebula;
            if (name.equals("hasBlackHole")) return blackHole;
            if (name.equals("hasPulsar")) return pulsar;
            if (name.equals("getType")) {
                return nebula ? StarSystemGenerator.StarSystemType.NEBULA
                        : StarSystemGenerator.StarSystemType.SINGLE;
            }
            return null;
        });
    }

    static StarSystemAPI taggedSystem(String tag, String starType) {
        PlanetAPI star = mock(
                PlanetAPI.class,
                (name, args) -> name.equals("getTypeId") ? starType : null);
        return mock(StarSystemAPI.class, (name, args) -> {
            if (name.equals("getStar")) return star;
            if (name.equals("hasTag")) return tag.equals(args[0]);
            if (name.equals("getType")) {
                return StarSystemGenerator.StarSystemType.SINGLE;
            }
            return null;
        });
    }

    static StarSystemAPI trinarySystem(String starType) {
        PlanetAPI primary = mock(
                PlanetAPI.class,
                (name, args) -> name.equals("getTypeId") ? starType : null);
        PlanetAPI secondary = mock(PlanetAPI.class, (name, args) -> null);
        PlanetAPI tertiary = mock(PlanetAPI.class, (name, args) -> null);
        return mock(StarSystemAPI.class, (name, args) -> {
            if (name.equals("getStar")) return primary;
            if (name.equals("getSecondary")) return secondary;
            if (name.equals("getTertiary")) return tertiary;
            if (name.equals("getType")) {
                return StarSystemGenerator.StarSystemType.SINGLE;
            }
            return null;
        });
    }

    static StarSystemAPI destinationSystem(
            String systemId,
            String displayName,
            String starType,
            boolean nebula,
            boolean blackHole,
            boolean pulsar,
            float x,
            float y) {
        PlanetAPI star = starType == null ? null : mock(
                PlanetAPI.class,
                (name, args) -> name.equals("getTypeId") ? starType : null);
        Vector2f location = new Vector2f(x, y);
        return mock(StarSystemAPI.class, (name, args) -> {
            if (name.equals("getOptionalUniqueId") || name.equals("getId")) {
                return systemId;
            }
            if (name.equals("getName") || name.equals("getBaseName")) {
                return displayName;
            }
            if (name.equals("getStar")) return star;
            if (name.equals("hasSystemwideNebula")) return nebula;
            if (name.equals("hasBlackHole")) return blackHole;
            if (name.equals("hasPulsar")) return pulsar;
            if (name.equals("getLocation")) return location;
            if (name.equals("getType")) {
                return nebula ? StarSystemGenerator.StarSystemType.NEBULA
                        : StarSystemGenerator.StarSystemType.SINGLE;
            }
            return null;
        });
    }

    static void installDestinationSector(
            List<StarSystemAPI> systems,
            Vector2f playerLocation,
            Map<String, Object> savedMemory) {
        MemoryAPI memory = mock(MemoryAPI.class, (name, values) -> {
            if (name.equals("getBoolean")) {
                return Boolean.TRUE.equals(savedMemory.get(values[0]));
            }
            if (name.equals("getString")) {
                Object value = savedMemory.get(values[0]);
                return value instanceof String ? value : null;
            }
            if (name.equals("set") && values.length >= 2) {
                savedMemory.put((String) values[0], values[1]);
            }
            if (name.equals("unset")) {
                savedMemory.remove(values[0]);
            }
            return null;
        });
        CampaignFleetAPI player = mock(CampaignFleetAPI.class,
                (name, values) -> name.equals("getLocationInHyperspace")
                                || name.equals("getLocation")
                        ? playerLocation : null);
        SectorAPI sector = mock(SectorAPI.class, (name, values) -> {
            if (name.equals("getMemoryWithoutUpdate")) return memory;
            if (name.equals("getPlayerFleet")) return player;
            if (name.equals("getStarSystems")) return systems;
            if (name.equals("getStarSystem")) {
                String id = (String) values[0];
                for (StarSystemAPI system : systems) {
                    if (id.equals(system.getId())
                            || id.equals(system.getOptionalUniqueId())) {
                        return system;
                    }
                }
            }
            return null;
        });
        com.fs.starfarer.api.Global.setSector(sector);
    }

    static void expectCategory(
            String starType,
            boolean nebula,
            boolean blackHole,
            boolean pulsar,
            SinniSystemVignetteScript.Category category,
            String subtype) {
        SinniSystemVignetteScript.Candidate candidate =
                SinniSystemVignetteScript.classifySystem(
                        system(starType, nebula, blackHole, pulsar));
        check(candidate != null, "Missing category for " + starType);
        check(candidate.category == category,
                "Wrong category for " + starType);
        check(candidate.subtype.equals(subtype),
                "Wrong subtype for " + starType);
    }

    public static void main(String[] args) {
        check(SinniVignetteRewards.NEBULA_SENSOR_PROFILE_MULT == 0.99f,
                "Nebula reward must remain a one-percent profile reduction");
        check(SinniVignetteRewards.BLACK_HOLE_DAMAGE_MULT == 0.90f,
                "Black-hole reward must remain a ten-percent damage reduction");
        check(SinniVignetteRewards.NEUTRON_ACCELERATION_MULT == 1.01f,
                "Neutron-star reward must remain one-percent acceleration");
        check(SinniVignetteRewards.BROWN_DWARF_FUEL_USE_MULT == 0.99f,
                "Legacy brown-dwarf reward must remain save-compatible");
        check(SinniVignetteRewards.WHITE_DWARF_SUPPLY_USE_MULT == 0.99f,
                "White-dwarf reward must remain one-percent upkeep savings");
        check(SinniVignetteRewards.GIANT_STAR_SURVEY_COST_MULT == 0.99f,
                "Giant-star reward must remain one-percent survey savings");
        check(SinniVignetteRewards.MASSIVE_STAR_CREW_LOSS_MULT == 0.99f,
                "Legacy massive-star reward must remain save-compatible");
        check(SinniVignetteRewards.ALL_TYPES_BURN_SPEED_CAP_BONUS == 2f,
                "Completed chart must add two to the Burn speed cap");
        check(SinniVignetteRewards.ALL_TYPES_BASE_BURN_BONUS == 1f,
                "Completed chart must add one base Burn level");

        expectCategory(StarTypes.YELLOW, true, false, false,
                SinniSystemVignetteScript.Category.NEBULA, "nebula");
        expectCategory(StarTypes.BLACK_HOLE, false, true, false,
                SinniSystemVignetteScript.Category.BLACK_HOLE, "black_hole");
        expectCategory(StarTypes.NEUTRON_STAR, false, false, true,
                SinniSystemVignetteScript.Category.NEUTRON_STAR, "neutron_star");
        check(SinniSystemVignetteScript.classifySystem(system(
                        StarTypes.BROWN_DWARF, false, false, false)) == null,
                "Brown dwarfs must no longer consume a vignette category");
        expectCategory(StarTypes.WHITE_DWARF, false, false, false,
                SinniSystemVignetteScript.Category.WHITE_DWARF, "white_dwarf");
        expectCategory(StarTypes.RED_DWARF, false, false, false,
                SinniSystemVignetteScript.Category.MAIN_SEQUENCE, "red_dwarf");
        expectCategory(StarTypes.ORANGE, false, false, false,
                SinniSystemVignetteScript.Category.MAIN_SEQUENCE, "orange_star");
        expectCategory(StarTypes.YELLOW, false, false, false,
                SinniSystemVignetteScript.Category.MAIN_SEQUENCE, "yellow_star");
        expectCategory(StarTypes.ORANGE_GIANT, false, false, false,
                SinniSystemVignetteScript.Category.GIANT_STAR, "orange_giant");
        expectCategory(StarTypes.RED_GIANT, false, false, false,
                SinniSystemVignetteScript.Category.GIANT_STAR, "red_giant");
        expectCategory(StarTypes.BLUE_GIANT, false, false, false,
                SinniSystemVignetteScript.Category.GIANT_STAR, "blue_giant");
        expectCategory(StarTypes.BLUE_SUPERGIANT, false, false, false,
                SinniSystemVignetteScript.Category.GIANT_STAR,
                "blue_supergiant");
        expectCategory(StarTypes.RED_SUPERGIANT, false, false, false,
                SinniSystemVignetteScript.Category.GIANT_STAR,
                "red_supergiant");
        SinniSystemVignetteScript.Candidate massive =
                SinniSystemVignetteScript.classifySystem(system(
                        StarTypes.BLUE_SUPERGIANT,
                        false, false, false));
        check("massive_star".equals(massive.sceneId),
                "Massive stars must retain their authored scene variant");
        SinniSystemVignetteScript.Candidate trinary =
                SinniSystemVignetteScript.classifySystem(
                        trinarySystem(StarTypes.YELLOW));
        check(trinary != null
                        && trinary.category
                                == SinniSystemVignetteScript.Category.TRINARY,
                "Three-star systems must use the trinary category");

        check(SinniSystemVignetteScript.classifySystem(
                system(null, false, false, false)) == null,
                "Starless non-nebula system should not trigger");

        String[] orionKnotIds = {
            OdysseyExpanseSystem.SYSTEM_ID,
            OdysseyExpanseSystem.SILENT_WAKE_ID,
            OdysseyExpanseSystem.ASHEN_VERGE_ID,
            OdysseyExpanseSystem.LAST_LIGHT_ID,
            OdysseyExpanseSystem.DEVOURED_REACH_ID
        };
        for (String id : orionKnotIds) {
            check(!SinniSystemVignetteScript.isExplorationVignetteSystem(
                            system(id, StarTypes.YELLOW,
                                    false, false, false)),
                    "Orion Knot system must suppress generic exploration dialogue: "
                            + id);
        }
        check(SinniSystemVignetteScript.classifySystem(system(
                        "ship_trophy_gan_eden", StarTypes.YELLOW,
                        false, false, false)) == null,
                "Gan Eden must suppress exploration dialogue by system id");
        check(SinniSystemVignetteScript.classifySystem(system(
                        "ship_trophy_gan_eden_power_transit_system",
                        StarTypes.YELLOW, false, false, false)) == null,
                "Gan Eden transit system must suppress exploration dialogue");
        StarSystemAPI ganEdenByName = mock(StarSystemAPI.class,
                (name, values) -> {
                    if (name.equals("getName") || name.equals("getBaseName")) {
                        return "Gan Eden";
                    }
                    if (name.equals("getStar")) {
                        return mock(PlanetAPI.class, (n,a) ->
                                n.equals("getTypeId") ? StarTypes.YELLOW : null);
                    }
                    if (name.equals("getType")) {
                        return StarSystemGenerator.StarSystemType.SINGLE;
                    }
                    return null;
                });
        check(SinniSystemVignetteScript.classifySystem(ganEdenByName) == null,
                "Legacy Gan Eden must suppress exploration dialogue by name");
        String[] coreTags = {
            Tags.THEME_CORE,
            Tags.THEME_CORE_POPULATED,
            Tags.THEME_CORE_UNPOPULATED
        };
        for (String tag : coreTags) {
            check(SinniSystemVignetteScript.classifySystem(
                            taggedSystem(tag, StarTypes.YELLOW)) == null,
                    "Core system tag must suppress exploration dialogue: "
                            + tag);
        }

        LocationAPI location = mock(LocationAPI.class, (name, values) -> null);
        LocationAPI elsewhere = mock(LocationAPI.class, (name, values) -> null);
        CampaignFleetAPI player = mock(CampaignFleetAPI.class,
                (name, values) -> name.equals("getContainingLocation")
                        ? location : null);

        TacticalModulePlugin tracking = mock(TacticalModulePlugin.class,
                (name, values) -> name.equals("getTarget") ? player : null);
        ModularFleetAIAPI trackingAI = mock(ModularFleetAIAPI.class,
                (name, values) -> name.equals("getTacticalModule")
                        ? tracking : null);
        CampaignFleetAPI hostileTracker = mock(CampaignFleetAPI.class,
                (name, values) -> {
                    if (name.equals("getContainingLocation")) return location;
                    if (name.equals("getAI")) return trackingAI;
                    if (name.equals("isHostileTo")) return true;
                    return null;
                });
        check(SinniSystemVignetteScript.isActivelyTrackingPlayer(
                        hostileTracker, player),
                "Hostile tactical target should block the scene");

        TacticalModulePlugin idle = mock(
                TacticalModulePlugin.class, (name, values) -> null);
        ModularFleetAIAPI idleAI = mock(ModularFleetAIAPI.class,
                (name, values) -> name.equals("getTacticalModule")
                        ? idle : null);
        CampaignFleetAPI idleHostile = mock(CampaignFleetAPI.class,
                (name, values) -> {
                    if (name.equals("getContainingLocation")) return location;
                    if (name.equals("getAI")) return idleAI;
                    if (name.equals("isHostileTo")) return true;
                    if (name.equals("getInteractionTarget")) return player;
                    return null;
                });
        check(!SinniSystemVignetteScript.isActivelyTrackingPlayer(
                        idleHostile, player),
                "Idle hostile with a stale interaction target should not block");

        TacticalModulePlugin standingDown = mock(TacticalModulePlugin.class,
                (name, values) -> {
                    if (name.equals("getTarget")) return player;
                    if (name.equals("isStandingDown")) return true;
                    return null;
                });
        ModularFleetAIAPI standingDownAI = mock(ModularFleetAIAPI.class,
                (name, values) -> name.equals("getTacticalModule")
                        ? standingDown : null);
        CampaignFleetAPI standingDownHostile = mock(CampaignFleetAPI.class,
                (name, values) -> {
                    if (name.equals("getContainingLocation")) return location;
                    if (name.equals("getAI")) return standingDownAI;
                    if (name.equals("isHostileTo")) return true;
                    if (name.equals("getInteractionTarget")) return player;
                    return null;
                });
        check(!SinniSystemVignetteScript.isActivelyTrackingPlayer(
                        standingDownHostile, player),
                "Standing-down fleet with a stale target should not block");

        CampaignFleetAPI nonHostileTracker = mock(CampaignFleetAPI.class,
                (name, values) -> {
                    if (name.equals("getContainingLocation")) return location;
                    if (name.equals("getAI")) return trackingAI;
                    if (name.equals("isHostileTo")) return false;
                    return null;
                });
        check(!SinniSystemVignetteScript.isActivelyTrackingPlayer(
                        nonHostileTracker, player),
                "Non-hostile tracker should not block the scene");

        CampaignFleetAPI remoteTracker = mock(CampaignFleetAPI.class,
                (name, values) -> {
                    if (name.equals("getContainingLocation")) return elsewhere;
                    if (name.equals("getAI")) return trackingAI;
                    if (name.equals("isHostileTo")) return true;
                    return null;
                });
        check(!SinniSystemVignetteScript.isActivelyTrackingPlayer(
                        remoteTracker, player),
                "Tracker in another location should not block the scene");

        FleetAssignmentDataAPI intercept = mock(FleetAssignmentDataAPI.class,
                (name, values) -> {
                    if (name.equals("getTarget")) return player;
                    if (name.equals("getAssignment")) {
                        return FleetAssignment.INTERCEPT;
                    }
                    return null;
                });
        CampaignFleetAIAPI fallbackAI = mock(CampaignFleetAIAPI.class,
                (name, values) -> name.equals("getCurrentAssignment")
                        ? intercept : null);
        CampaignFleetAPI fallbackTracker = mock(CampaignFleetAPI.class,
                (name, values) -> {
                    if (name.equals("getContainingLocation")) return location;
                    if (name.equals("getAI")) return fallbackAI;
                    if (name.equals("isHostileTo")) return true;
                    return null;
                });
        check(SinniSystemVignetteScript.isActivelyTrackingPlayer(
                        fallbackTracker, player),
                "Hostile intercept assignment should block the scene");

        ModularFleetAIAPI standingDownInterceptAI = mock(
                ModularFleetAIAPI.class,
                (name, values) -> {
                    if (name.equals("getTacticalModule")) return standingDown;
                    if (name.equals("getCurrentAssignment")) return intercept;
                    return null;
                });
        CampaignFleetAPI standingDownInterceptor = mock(CampaignFleetAPI.class,
                (name, values) -> {
                    if (name.equals("getContainingLocation")) return location;
                    if (name.equals("getAI")) return standingDownInterceptAI;
                    if (name.equals("isHostileTo")) return true;
                    return null;
                });
        check(!SinniSystemVignetteScript.isActivelyTrackingPlayer(
                        standingDownInterceptor, player),
                "Standing-down fleet with stale intercept should not block");

        StarSystemAPI avici = system(
                OdysseyExpanseSystem.MESSINA_ID,
                StarTypes.BLACK_HOLE,
                false,
                true,
                false);
        check(OdysseyExpanseSystem.MESSINA_ID.equals(avici.getId()),
                "Avici regression fixture must expose its historical ID");
        check(SinniSystemVignetteScript.classifySystem(avici).category
                        == SinniSystemVignetteScript.Category.AVICI,
                "Avici arrival must outrank the generic black-hole category");

        Map<String, Object> savedMemory = new HashMap<>();
        MemoryAPI memory = mock(MemoryAPI.class, (name, values) -> {
            if (name.equals("getBoolean")) {
                return Boolean.TRUE.equals(savedMemory.get(values[0]));
            }
            if (name.equals("set") && values.length >= 2) {
                savedMemory.put((String) values[0], values[1]);
            }
            return null;
        });
        SectorAPI sector = mock(SectorAPI.class,
                (name, values) -> name.equals("getMemoryWithoutUpdate")
                        ? memory : null);
        com.fs.starfarer.api.Global.setSector(sector);
        StarSystemAPI alphaOdyssey = system(
                OdysseyExpanseSystem.SYSTEM_ID,
                null,
                true,
                false,
                false);
        check(SinniSystemVignetteScript.classifySystem(alphaOdyssey) == null,
                "Unarmed Alpha Odyssey must suppress generic exploration");
        StarSystemAPI sanzu = system(OdysseyExpanseSystem.SILENT_WAKE_ID,
                StarTypes.WHITE_DWARF, true, false, false);
        StarSystemAPI ashen = system(OdysseyExpanseSystem.ASHEN_VERGE_ID,
                StarTypes.YELLOW, true, false, false);
        StarSystemAPI devoured = system(OdysseyExpanseSystem.DEVOURED_REACH_ID,
                null, false, false, false);
        StarSystemAPI lastLight = system(OdysseyExpanseSystem.LAST_LIGHT_ID,
                StarTypes.WHITE_DWARF, false, false, false);
        check(SinniSystemVignetteScript.classifySystem(sanzu).category
                        == SinniSystemVignetteScript.Category.SANZU,
                "Sanzu must use its own arrival instead of a nebula/white-dwarf scene");
        check(SinniSystemVignetteScript.classifySystem(ashen) == null,
                "Ashen Verge must have neither an arrival nor a generic vignette");
        check(SinniSystemVignetteScript.classifySystem(devoured) == null,
                "Devoured Reach must have neither an arrival nor a generic vignette");
        check(!SinniSystemVignetteScript.isExplorationVignetteSystem(ashen)
                        && !SinniSystemVignetteScript.isExplorationVignetteSystem(devoured),
                "Retired regional arrivals must stay excluded from the stellar chart");
        check(SinniSystemVignetteScript.classifySystem(lastLight) == null,
                "Last Light must have neither an arrival nor a generic vignette");
        for (SinniSystemVignetteScript.Category regional : Arrays.asList(
                SinniSystemVignetteScript.Category.SANZU,
                SinniSystemVignetteScript.Category.ASHEN_VERGE,
                SinniSystemVignetteScript.Category.DEVOURED_REACH)) {
            check(!SinniSystemVignetteScript.isComplete(regional),
                    "Classification must not consume a regional arrival");
        }
        SinniSystemVignetteScript.markComplete(
                SinniSystemVignetteScript.Category.SANZU);
        check(SinniSystemVignetteScript.isComplete(
                        SinniSystemVignetteScript.Category.SANZU)
                        && !SinniSystemVignetteScript.isComplete(
                                SinniSystemVignetteScript.Category.ASHEN_VERGE)
                        && !SinniSystemVignetteScript.isComplete(
                                SinniSystemVignetteScript.Category.DEVOURED_REACH),
                "Sanzu completion must leave retired arrival flags untouched");
        SinniSystemVignetteScript.armAlphaOdysseyArrival();
        check(savedMemory.get(
                        SinniSystemVignetteScript.ALPHA_ODYSSEY_ARRIVAL_PENDING)
                        == Boolean.TRUE,
                "Committed Troy transit should arm the arrival");
        check(SinniSystemVignetteScript.classifySystem(alphaOdyssey).category
                        == SinniSystemVignetteScript.Category.ALPHA_ODYSSEY
                        && SinniSystemVignetteScript.classifySystem(alphaOdyssey).sceneId
                                .equals("alpha_odyssey"),
                "Alpha Odyssey arrival must outrank its nebula category");
        SinniSystemVignetteScript.markComplete(
                SinniSystemVignetteScript.Category.ALPHA_ODYSSEY);
        check(SinniSystemVignetteScript.classifySystem(alphaOdyssey).category
                        == SinniSystemVignetteScript.Category.ALPHA_ODYSSEY,
                "Completed arrival must continue suppressing a second popup");
        check(savedMemory.get(
                        SinniSystemVignetteScript.ALPHA_ODYSSEY_ARRIVAL_PENDING)
                        == Boolean.FALSE,
                "Continue should clear only the new pending flag");
        check(!SinniSystemVignetteScript.isComplete(
                        SinniSystemVignetteScript.Category.NEBULA),
                "Absent old-save key should mean unseen");
        SinniSystemVignetteScript.markComplete(
                SinniSystemVignetteScript.Category.NEBULA);
        check(SinniSystemVignetteScript.isComplete(
                        SinniSystemVignetteScript.Category.NEBULA),
                "Completion should persist in sector memory");
        check(!SinniSystemVignetteScript.isComplete(
                        SinniSystemVignetteScript.Category.BLACK_HOLE),
                "One category must not consume another");

        StarSystemAPI farNebula = destinationSystem(
                "regression_far_nebula", "Far Nebula",
                StarTypes.YELLOW, true, false, false, 9000f, 0f);
        StarSystemAPI blackHole = destinationSystem(
                "regression_black_hole", "Black Reach",
                StarTypes.BLACK_HOLE, false, true, false, 3000f, 0f);
        StarSystemAPI completedWhiteDwarf = destinationSystem(
                "regression_white_dwarf", "Pale Lantern",
                StarTypes.WHITE_DWARF, false, false, false, 2000f, 0f);
        StarSystemAPI nearNebula = destinationSystem(
                "regression_near_nebula", "Near Nebula",
                StarTypes.YELLOW, true, false, false, 1000f, 0f);
        StarSystemAPI excludedCoreMainSequence = mock(
                StarSystemAPI.class, (name, values) -> {
                    if (name.equals("getId")
                            || name.equals("getOptionalUniqueId")) {
                        return "regression_core_system";
                    }
                    if (name.equals("getName")
                            || name.equals("getBaseName")) {
                        return "Core System";
                    }
                    if (name.equals("getStar")) {
                        return mock(PlanetAPI.class, (n, a) ->
                                n.equals("getTypeId")
                                        ? StarTypes.YELLOW : null);
                    }
                    if (name.equals("hasTag")) {
                        return Tags.THEME_CORE.equals(values[0]);
                    }
                    if (name.equals("getLocation")) {
                        return new Vector2f(500f, 0f);
                    }
                    if (name.equals("getType")) {
                        return StarSystemGenerator.StarSystemType.SINGLE;
                    }
                    return null;
                });
        List<StarSystemAPI> destinationSystems = Arrays.asList(
                blackHole,
                nearNebula,
                completedWhiteDwarf,
                excludedCoreMainSequence,
                farNebula);
        Map<String, Object> destinationMemory = new HashMap<>();
        destinationMemory.put(
                "$chief_navigator_sinni_vignette_complete_"
                        + SinniSystemVignetteScript.Category.WHITE_DWARF.id,
                Boolean.TRUE);
        installDestinationSector(
                destinationSystems, null,
                destinationMemory);

        List<SinniSystemVignetteScript.LastRequestTarget> destinations =
                SinniSystemVignetteScript
                        .getRemainingSightseeingDestinations();
        check(destinations.size() == 2,
                "Destination list must omit completed, unavailable, and "
                        + "ineligible categories");
        check(destinations.get(0).category
                        == SinniSystemVignetteScript.Category.NEBULA,
                "Destination list must retain exploration-category order");
        check(destinations.get(0).system == nearNebula,
                "Without a player location, destination selection must keep "
                        + "the first eligible system deterministic");
        check(destinations.get(1).category
                        == SinniSystemVignetteScript.Category.BLACK_HOLE,
                "Black-hole destination must follow the nebula destination");
        check(destinations.get(1).system == blackHole,
                "Destination list must expose an actual system per category");

        destinationMemory.put(
                "$chief_navigator_sinni_last_request_active_v1",
                Boolean.TRUE);
        destinationMemory.put(
                "$chief_navigator_sinni_last_request_category_v1",
                SinniSystemVignetteScript.Category.NEBULA.id);
        destinationMemory.put(
                "$chief_navigator_sinni_last_request_system_v1",
                farNebula.getId());
        destinations = SinniSystemVignetteScript
                .getRemainingSightseeingDestinations();
        check(destinations.size() == 2,
                "An active last request must not hide other remaining rows");
        check(destinations.get(0).system == farNebula,
                "Active final-request target must override nearest-system "
                        + "recalculation");

        checkAviciVisitHistory();
        System.out.println("Sinni system-vignette regression checks passed.");
    }

    static void checkAviciVisitHistory() {
        com.fs.starfarer.api.Global.setSector(null);
        check(!SinniSystemVignetteInteraction.hasSeenAvici(),
                "No campaign must leave Avici unseen");

        Map<String, Object> savedMemory = new HashMap<>();
        boolean[] visited = {false};
        MemoryAPI ownedSystemMemory = mock(MemoryAPI.class,
                (name, values) -> name.equals("getBoolean") ? true : null);
        StarSystemAPI avici = mock(StarSystemAPI.class, (name, values) -> {
            if (name.equals("getOptionalUniqueId") || name.equals("getId")) {
                return OdysseyExpanseSystem.MESSINA_ID;
            }
            if (name.equals("getMemoryWithoutUpdate")) return ownedSystemMemory;
            if (name.equals("isEnteredByPlayer")) return visited[0];
            return null;
        });
        installDestinationSector(Arrays.asList(avici), new Vector2f(), savedMemory);
        check(!SinniSystemVignetteInteraction.hasSeenAvici(),
                "A charted but unvisited Avici must use Sanzu's unseen branch");
        visited[0] = true;
        check(SinniSystemVignetteInteraction.hasSeenAvici()
                        && !SinniSystemVignetteScript.isComplete(
                                SinniSystemVignetteScript.Category.AVICI),
                "A visit must select the seen branch even if Avici's scene was deferred");
        check(savedMemory.isEmpty(),
                "Selecting Sanzu's Avici branch must not consume either arrival");

        visited[0] = false;
        String completedAvici = "$chief_navigator_sinni_vignette_complete_avici";
        savedMemory.put(completedAvici, true);
        check(SinniSystemVignetteInteraction.hasSeenAvici(),
                "An already completed Avici arrival remains known in older saves");
        savedMemory.remove(completedAvici);
        installDestinationSector(Arrays.asList(), new Vector2f(), savedMemory);
        check(!SinniSystemVignetteInteraction.hasSeenAvici(),
                "A missing authored Avici must fail closed without creating it");
    }
}
