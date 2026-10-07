package chiefnavigator.hullmods;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.PersonalityAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.combat.threat.ConstructionSwarmSystemScript;
import com.fs.starfarer.api.impl.combat.threat.ThreatHullmod;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/** Native Hive fit, geometry, fragment economy and campaign/fabricated doctrine. */
public final class StarvingHiveParityRegression {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(), new Class<?>[] {type}, handler);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        if (type == char.class) return '\0';
        return null;
    }

    private static PersonAPI person(final String[] personality) {
        return proxy(PersonAPI.class, new InvocationHandler() {
            @Override
            public Object invoke(Object target, Method method, Object[] args) {
                if ("setPersonality".equals(method.getName())) {
                    personality[0] = (String) args[0];
                    return null;
                }
                if ("getPersonalityAPI".equals(method.getName())) {
                    return proxy(PersonalityAPI.class,
                            new InvocationHandler() {
                                @Override
                                public Object invoke(
                                        Object ignored,
                                        Method personalityMethod,
                                        Object[] personalityArgs) {
                                    if ("getId".equals(
                                            personalityMethod.getName())) {
                                        return personality[0];
                                    }
                                    return defaultValue(
                                            personalityMethod.getReturnType());
                                }
                            });
                }
                return defaultValue(method.getReturnType());
            }
        });
    }

    private static FleetMemberAPI member(
            final String hullId,
            final PersonAPI captain,
            final String[] personalityOverride) {
        return proxy(FleetMemberAPI.class, new InvocationHandler() {
            @Override
            public Object invoke(Object target, Method method, Object[] args) {
                String name = method.getName();
                if ("getHullId".equals(name)) return hullId;
                if ("getCaptain".equals(name)) return captain;
                if ("getPersonalityOverride".equals(name)) {
                    return personalityOverride[0];
                }
                if ("setPersonalityOverride".equals(name)) {
                    personalityOverride[0] = (String) args[0];
                    return null;
                }
                return defaultValue(method.getReturnType());
            }
        });
    }

    private static ShipAPI ship(
            final String hullId,
            final FleetMemberAPI member,
            final PersonAPI captain) {
        final ShipHullSpecAPI spec = proxy(
                ShipHullSpecAPI.class,
                (target, method, args) -> "getHullId".equals(
                        method.getName()) ? hullId
                                : defaultValue(method.getReturnType()));
        return proxy(ShipAPI.class, (target, method, args) -> {
            if ("getHullSpec".equals(method.getName())) return spec;
            if ("getFleetMember".equals(method.getName())) return member;
            if ("getCaptain".equals(method.getName())) return captain;
            return defaultValue(method.getReturnType());
        });
    }

    private static void sameJson(Object actual, Object expected, String path) throws Exception {
        if (expected instanceof JSONObject) {
            check(actual instanceof JSONObject, path + " must be an object");
            JSONObject left = (JSONObject) actual;
            JSONObject right = (JSONObject) expected;
            check(left.length() == right.length(), path + " keys differ");
            for (Iterator<?> keys = right.keys(); keys.hasNext();) {
                String key = (String) keys.next();
                check(left.has(key), path + " missing " + key);
                sameJson(left.get(key), right.get(key), path + "/" + key);
            }
        } else if (expected instanceof JSONArray) {
            check(actual instanceof JSONArray, path + " must be an array");
            JSONArray left = (JSONArray) actual;
            JSONArray right = (JSONArray) expected;
            check(left.length() == right.length(), path + " count differs");
            for (int index = 0; index < right.length(); index++) {
                sameJson(left.get(index), right.get(index), path + "/" + index);
            }
        } else if (expected instanceof Number) {
            check(actual instanceof Number && Math.abs(((Number) actual).doubleValue()
                    - ((Number) expected).doubleValue()) < 0.00001, path + " value differs");
        } else {
            check(expected.equals(actual), path + " differs: " + actual + " != " + expected);
        }
    }

    private static JSONObject json(String path) throws Exception {
        return new JSONObject(Files.readString(Paths.get(path), StandardCharsets.UTF_8));
    }

    private static void nativeDataParity() throws Exception {
        JSONObject nativeHull = json("../../starsector-core/data/hulls/hive_unit.ship");
        nativeHull.put("hullId", StarvingThreatHullmod.HIVE_HULL_ID);
        nativeHull.put("hullName", "Starving Hive Unit");
        nativeHull.put("spriteName", "graphics/ships/starving_threat/starving_threat_hive.png");
        JSONArray mods = nativeHull.getJSONArray("builtInMods");
        for (int index = 0; index < mods.length(); index++) {
            if (mods.getString(index).equals("threat_hullmod")) {
                mods.put(index, StarvingThreatHullmod.HULLMOD_ID);
            }
        }
        sameJson(json("data/hulls/chief_navigator_starving_hive_unit.ship"), nativeHull,
                "Hive geometry and built-ins");
        JSONObject nativeFit = json("../../starsector-core/data/variants/threat/hive_unit_Type350.variant");
        nativeFit.put("hullId", StarvingThreatHullmod.HIVE_HULL_ID);
        nativeFit.put("variantId", "chief_navigator_starving_hive_Type350");
        sameJson(json("data/variants/chief_navigator_starving_hive_Type350.variant"), nativeFit,
                "Hive full loadout, hullmods, vents, capacitors and controls");
        String nativeData = Files.readString(Paths.get("../../starsector-core/data/hulls/ship_data.csv"));
        String customData = Files.readString(Paths.get("data/hulls/ship_data.csv"));
        String nativeRow = nativeData.lines().filter(line -> line.startsWith(",hive_unit,"))
                .findFirst().orElseThrow();
        String customRow = customData.lines().filter(line -> line.startsWith("Starving Hive Unit,"))
                .findFirst().orElseThrow();
        String[] fields = nativeData.lines().findFirst().orElseThrow().split(",", -1);
        String csvSplit = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";
        String[] nativeValues = nativeRow.split(csvSplit, -1);
        String[] customValues = customRow.split(csvSplit, -1);
        check(nativeValues.length == fields.length && customValues.length == fields.length,
                "Hive ship data columns must remain aligned");
        for (int column = 2; column < fields.length; column++) {
            if (fields[column].equals("hints") || fields[column].equals("tags")) continue;
            check(nativeValues[column].equals(customValues[column]),
                    "Hive native ship data changed: " + fields[column]);
        }
        String generator = Files.readString(Paths.get("tools/generate_starving_threat_hulls.ps1"));
        check(generator.contains("if ($entry.Base -eq \"hive_unit\")")
                        && generator.contains("$entry.Source -ne \"hive_unit_Type350\""),
                "Generator must not prune native Hive slots or fragment weapons");
    }

    // Before-creation stat tests need no generated world/variant database.
    private static final class ConstructionReady extends ConstructionSwarmSystemScript {
        static void ready() { inited = true; }
    }

    private static final class ShipStats {
        final Map<String, MutableStat> stats = new HashMap<>();
        final Map<String, StatBonus> bonuses = new HashMap<>();
        final Map<String, MutableStat> dynamicStats = new HashMap<>();
        final Map<String, StatBonus> dynamicMods = new HashMap<>();
        final MutableShipStatsAPI api;

        ShipStats(String hullId) {
            ShipHullSpecAPI spec = proxy(ShipHullSpecAPI.class, (target, method, args) ->
                    method.getName().equals("getHullId") ? hullId : defaultValue(method.getReturnType()));
            ShipVariantAPI variant = proxy(ShipVariantAPI.class, (target, method, args) ->
                    method.getName().equals("getHullSpec") ? spec : defaultValue(method.getReturnType()));
            DynamicStatsAPI dynamic = proxy(DynamicStatsAPI.class, (target, method, args) -> {
                if (method.getName().equals("getStat")) {
                    return dynamicStats.computeIfAbsent((String) args[0], key -> new MutableStat(1f));
                }
                if (method.getName().equals("getMod")) {
                    return dynamicMods.computeIfAbsent((String) args[0], key -> new StatBonus());
                }
                return defaultValue(method.getReturnType());
            });
            api = proxy(MutableShipStatsAPI.class, (target, method, args) -> {
                if (method.getName().equals("getVariant")) return variant;
                if (method.getName().equals("getDynamic")) return dynamic;
                if (method.getReturnType() == MutableStat.class) {
                    return stats.computeIfAbsent(method.getName(), key -> new MutableStat(100f));
                }
                if (method.getReturnType() == StatBonus.class) {
                    return bonuses.computeIfAbsent(method.getName(), key -> new StatBonus());
                }
                return defaultValue(method.getReturnType());
            });
        }
    }

    private static void close(float actual, float expected, String message) {
        check(Math.abs(actual - expected) < 0.001f, message + ": " + actual + " != " + expected);
    }

    private static void nativeSwarmParity() {
        ConstructionReady.ready();
        ShipStats stock = new ShipStats(ThreatHullmod.HIVE_UNIT);
        ShipStats starving = new ShipStats(StarvingThreatHullmod.HIVE_HULL_ID);
        ThreatHullmod nativeMod = new ThreatHullmod();
        StarvingThreatHullmod customMod = new StarvingThreatHullmod();
        nativeMod.applyEffectsBeforeShipCreation(ShipAPI.HullSize.CRUISER, stock.api, "native");
        customMod.applyEffectsBeforeShipCreation(ShipAPI.HullSize.CRUISER, starving.api,
                StarvingThreatHullmod.HULLMOD_ID);
        customMod.applyEffectsBeforeShipCreation(ShipAPI.HullSize.CRUISER, starving.api,
                StarvingThreatHullmod.HULLMOD_ID);
        close(starving.api.getDynamic().getMod(Stats.FRAGMENT_SWARM_SIZE_MOD).computeEffective(100f),
                stock.api.getDynamic().getMod(Stats.FRAGMENT_SWARM_SIZE_MOD).computeEffective(100f),
                "Native Hive 4x fragment reserve is restored without stacking");
        close(starving.api.getDynamic().getStat(Stats.FRAGMENT_SWARM_RESPAWN_RATE_MULT).getModifiedValue(),
                stock.api.getDynamic().getStat(Stats.FRAGMENT_SWARM_RESPAWN_RATE_MULT).getModifiedValue(),
                "Native Hive 2x fragment regeneration is restored without stacking");
        close(starving.api.getHullBonus().computeEffective(100f), 67f, "Shared Starving hull");
        close(starving.api.getArmorBonus().computeEffective(100f), 115f, "Shared Starving armor");
        close(starving.api.getAcceleration().getModifiedValue(), 500f, "Shared Starving acceleration");
        close(starving.api.getMaxSpeed().getModifiedValue(), 100f, "Unchanged top speed");
        close(starving.api.getSensorProfile().getModifiedValue(), 300f, "Shared Starving emissions");
        close(starving.api.getSuppliesToRecover().getModifiedValue(), 0f, "Shared Starving recovery");
        for (String hullId : new String[] {"chief_navigator_starving_skirmish_unit",
                "chief_navigator_starving_fabricator_unit", "chief_navigator_scylla"}) {
            ShipStats other = new ShipStats(hullId);
            customMod.applyEffectsBeforeShipCreation(ShipAPI.HullSize.CRUISER, other.api,
                    StarvingThreatHullmod.HULLMOD_ID);
            close(other.api.getDynamic().getMod(Stats.FRAGMENT_SWARM_SIZE_MOD).computeEffective(100f),
                    100f, "Hive-only reserve must not leak to " + hullId);
            close(other.api.getDynamic().getStat(Stats.FRAGMENT_SWARM_RESPAWN_RATE_MULT).getModifiedValue(),
                    1f, "Hive-only regeneration must not leak to " + hullId);
        }
    }

    public static void main(String[] args) throws Exception {
        nativeDataParity();
        nativeSwarmParity();
        check(Personalities.TIMID.equals(
                        StarvingThreatHullmod.HIVE_PERSONALITY),
                "Hive doctrine must match native Timid support AI");

        String[] captainPersonality = {Personalities.AGGRESSIVE};
        String[] memberOverride = {Personalities.AGGRESSIVE};
        PersonAPI captain = person(captainPersonality);
        FleetMemberAPI hive = member(
                StarvingThreatHullmod.HIVE_HULL_ID,
                captain,
                memberOverride);
        check(StarvingThreatHullmod.enforceHivePersonality(hive),
                "A formerly aggressive Hive must use native doctrine");
        check(Personalities.TIMID.equals(memberOverride[0])
                        && Personalities.TIMID.equals(
                                captainPersonality[0]),
                "Hive member and captain must both become Timid");
        check(!StarvingThreatHullmod.enforceHivePersonality(hive),
                "Hive repair must be idempotent");

        String[] fabricatedCaptain = {Personalities.CAUTIOUS};
        String[] fabricatedOverride = {null};
        PersonAPI fabricatedPerson = person(fabricatedCaptain);
        FleetMemberAPI fabricatedMember = member(
                StarvingThreatHullmod.HIVE_HULL_ID,
                fabricatedPerson,
                fabricatedOverride);
        check(StarvingThreatHullmod.enforceHivePersonality(ship(
                        StarvingThreatHullmod.HIVE_HULL_ID,
                        fabricatedMember,
                        fabricatedPerson)),
                "A combat-fabricated Hive must be repaired");
        check(Personalities.TIMID.equals(fabricatedOverride[0])
                        && Personalities.TIMID.equals(
                                fabricatedCaptain[0]),
                "Fabricated Hive must enter combat with native doctrine");

        String[] lineOverride = {Personalities.STEADY};
        FleetMemberAPI line = member(
                "chief_navigator_starving_standoff_unit",
                person(new String[] {Personalities.STEADY}),
                lineOverride);
        check(!StarvingThreatHullmod.enforceHivePersonality(line)
                        && Personalities.STEADY.equals(lineOverride[0]),
                "Non-Hive Starving roles must keep their doctrine");

        String shipData = Files.readString(
                Paths.get("data", "hulls", "ship_data.csv"),
                StandardCharsets.UTF_8);
        String hiveRow = null;
        for (String row : shipData.split("\\R")) {
            if (row.startsWith("Starving Hive Unit,")) {
                hiveRow = row;
                break;
            }
        }
        check(hiveRow != null
                        && hiveRow.contains("threat_timid")
                        && !hiveRow.contains("threat_aggressive")
                        && hiveRow.contains(",Threat,fragment_volley,"),
                "Hive hull data must retain native Timid doctrine and Fragment Volley");

        System.out.println("PASS: exact native Hive geometry, twelve weapons, loadout hullmods, "
                + "fragment reserve/regeneration and Timid campaign/fabricated doctrine; "
                + "shared Starving traits retained, no other-role changes.");
    }
}
