package chiefnavigator.quest;

import chiefnavigator.ai.GuardDroneAI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import static chiefnavigator.quest.BudaiSalvageRewardRegression.*;

/** Native AI classification and owned custom-AI restoration; reflection is test-only. */
public final class MirrorCompatibilityRegression {
    public static void main(String[] args) throws Exception {
        SettingsAPI previous = Global.getSettings();
        JSONArray exclusions = new JSONArray();
        Global.setSettings(mock(SettingsAPI.class, (name, values) ->
                name.equals("getJSONArray") ? exclusions : null));
        try {
            ShipAIPlugin generic = nativeBasicAI(new ShipAIConfig());
            check(MirrorCompatibility.isGenericDefaultAI(generic),
                    "The real native BasicShipAI is generic, not a custom wrapper");
            checkGenericActivation(generic);
            ShipAIPlugin fearless = nativeBasicAI(GuardDroneAI.createFearlessConfig());
            check(MirrorCompatibility.isGenericDefaultAI(fearless), "Our installed Fearless remains generic");
            ShipAIConfig tailored = new ShipAIConfig();
            tailored.personalityOverride = "steady";
            ShipAIPlugin configuredBasic = nativeBasicAI(tailored);
            check(MirrorCompatibility.isGenericDefaultAI(configuredBasic),
                    "A configured BasicShipAI (stock Threat/Dweller or KOL) remains generic");
            checkGenericActivation(configuredBasic);
            check(configuredBasic.getConfig() == tailored && "reckless".equals(tailored.personalityOverride),
                    "Configure Basic Fearless in place, preserving its class/native modules/config identity");
            ShipAIPlugin delegate = mock(ShipAIPlugin.class, (name, values) ->
                    name.equals("getConfig") ? new ShipAIConfig() : null);
            ShipAIPlugin wrapper = (ShipAIPlugin) Class.forName(
                    "com.fs.starfarer.combat.entities.Ship$ShipAIWrapper")
                    .getConstructor(ShipAIPlugin.class).newInstance(delegate);
            check(!MirrorCompatibility.isGenericDefaultAI(wrapper),
                    "The real native wrapper retains public custom AI even with default-looking config");
            checkCustomRestore(wrapper);
            checkOptOuts(exclusions);
            System.out.println("PASS: real BasicShipAI/custom wrappers, configured Basic object/context preservation, "
                    + "in-place Fearless, custom handoff without resets/personality changes, and explicit skin/module exclusions.");
        } finally {
            Global.setSettings(previous);
        }
    }

    private static void checkGenericActivation(ShipAIPlugin generic) {
        Map<String, Object> data = new HashMap<>();
        ShipAIPlugin[] current = {generic};
        int[] personalities = {0}, resets = {0}, installs = {0};
        FleetMemberAPI member = mock(FleetMemberAPI.class, (name, values) -> {
            if (name.equals("setPersonalityOverride")) personalities[0]++;
            return null;
        });
        PersonAPI captain = mock(PersonAPI.class, (name, values) -> {
            if (name.equals("setPersonality")) personalities[0]++;
            return null;
        });
        ShipAPI ship = mock(ShipAPI.class, (name, values) -> {
            switch (name) {
                case "getShipAI": return current[0];
                case "getCustomData": return data;
                case "getChildModulesCopy": return List.of();
                case "getFleetMember": return member;
                case "getCaptain": return captain;
                case "setDefaultAI": resets[0]++; break;
                case "setShipAI": current[0] = (ShipAIPlugin) values[0]; installs[0]++; break;
                default: break;
            }
            return null;
        });
        MirrorCompatibility.rememberConstructionAI(ship);
        check(!data.isEmpty(), "Save generic native AI too, retaining its per-ship system modules");
        current[0] = null;
        GautamaFinalRematchBattleCreationPlugin.activateCompletedMirror(ship, member, 1);
        ShipAIConfig config = generic.getConfig();
        check(current[0] == generic && data.isEmpty() && resets[0] == 0
                        && installs[0] == 1 && personalities[0] == 2
                        && "reckless".equals(config.personalityOverride)
                        && config.alwaysStrafeOffensively && !config.backingOffWhileNotVentingAllowed
                        && !config.turnToFaceWithUndamagedArmor && config.burnDriveIgnoreEnemies,
                "A real already-initialized generic BasicShipAI receives full Fearless in place");
    }

    private static void checkCustomRestore(ShipAIPlugin wrapper) throws Exception {
        Map<String, Object> data = new HashMap<>();
        ShipAIPlugin[] current = {wrapper};
        int[] mutations = {0}, resets = {0}, installs = {0};
        PersonAPI captain = mock(PersonAPI.class, (name, values) -> {
            if (name.equals("setPersonality")) mutations[0]++;
            return null;
        });
        FleetMemberAPI member = mock(FleetMemberAPI.class, (name, values) -> {
            if (name.equals("setPersonalityOverride")) mutations[0]++;
            return null;
        });
        ShipAPI ship = mock(ShipAPI.class, (name, values) -> {
            switch (name) {
                case "getCustomData": return data;
                case "getCaptain": return captain;
                case "getFleetMember": return member;
                case "getChildModulesCopy": return List.of();
                case "getShipAI": return current[0];
                case "setDefaultAI": resets[0]++; break;
                case "setShipAI": current[0] = (ShipAIPlugin) values[0]; installs[0]++; break;
                default: break;
            }
            return null;
        });
        MirrorCompatibility.rememberConstructionAI(ship);
        MirrorCompatibility.rememberConstructionAI(ship);
        current[0] = null;
        GautamaFinalRematchBattleCreationPlugin.activateCompletedMirror(ship, member, 1);
        check(current[0] == wrapper && resets[0] == 0 && installs[0] == 1 && mutations[0] == 0,
                "Restore the exact custom wrapper without native reset or forcing custom personality");
        check(data.isEmpty() && MirrorCompatibility.takeConstructionAI(ship) == null,
                "The saved AI is consumed once, not retained through mirror lifetime");
        GautamaFinalRematchBattleCreationPlugin.activateCompletedMirror(ship, member, 1);
        check(resets[0] == 0 && installs[0] == 1 && mutations[0] == 0,
                "An already installed custom AI is never unnecessarily recreated");
        MirrorCompatibility.rememberConstructionAI(ship);
        MirrorCompatibility.forgetConstructionAI(ship);
        MirrorCompatibility.forgetConstructionAI(ship);
        check(data.isEmpty(), "Cleanup is idempotent and releases its saved custom context");

        ShipAIPlugin generic = nativeBasicAI(new ShipAIConfig());
        current[0] = generic;
        MirrorCompatibility.rememberConstructionAI(ship);
        current[0] = wrapper;
        MirrorCompatibility.rememberConstructionAI(ship);
        current[0] = null;
        check(MirrorCompatibility.takeConstructionAI(ship) == wrapper && data.isEmpty(),
                "A late creator's specialized AI supersedes the saved generic construction context");

        current[0] = generic;
        MirrorCompatibility.rememberConstructionAI(ship);
        current[0] = wrapper;
        GautamaFinalRematchBattleCreationPlugin.activateCompletedMirror(ship, member, 1);
        check(current[0] == wrapper && resets[0] == 0 && installs[0] == 1 && mutations[0] == 0
                        && data.isEmpty(),
                "A currently installed specialized AI wins over an older saved generic context");

        MirrorCompatibility.rememberConstructionAI(ship);
        current[0] = generic;
        GautamaFinalRematchBattleCreationPlugin.activateCompletedMirror(ship, member, 1);
        check(current[0] == wrapper && resets[0] == 0 && installs[0] == 2 && mutations[0] == 0,
                "A saved required custom AI replaces only a generic construction-time substitute");
    }

    private static void checkOptOuts(JSONArray exclusions) {
        ShipHullSpecAPI base = hull("unsafe_base", "unsafe_base", null, false, null, null);
        ShipHullSpecAPI skin = hull("unsafe_skin", "unsafe_base", null, false, base, null);
        ShipVariantAPI normal = variant(hull("onslaught_mk1", "onslaught_mk1", null,
                false, null, null), false, null);
        check(!MirrorCompatibility.isMirrorOptedOut(normal), "Modular native hulls are allowed by default");
        exclusions.put("unsafe_base");
        check(MirrorCompatibility.isMirrorOptedOut(variant(skin, false, null)),
                "Configured base-hull ID covers skins without renaming them");
        check(MirrorCompatibility.isMirrorOptedOut(variant(hull("unsafe_d", "unrelated",
                "unsafe_base", false, null, base), false, null)), "D-parent identity is recognized");
        check(MirrorCompatibility.isMirrorOptedOut(variant(hull("tag_skin", "tag_base", null,
                false, hull("tag_base", "tag_base", null, true, null, null), null), false, null)),
                "A base hull opt-out tag covers its skin");
        check(MirrorCompatibility.isMirrorOptedOut(variant(null, true, null)), "Variant tag is explicit opt-out");
        ShipVariantAPI unsafeModule = variant(null, true, null);
        check(MirrorCompatibility.isMirrorOptedOut(variant(normal.getHullSpec(), false, unsafeModule)),
                "A tagged module excludes the root before any partial modular mirror is built");
        check(!MirrorCompatibility.isMirrorOptedOut(variant(null, false, null)),
                "Null public parent/module properties in fixtures are harmless");
    }

    private static ShipHullSpecAPI hull(String id, String baseId, String dParentId,
            boolean tagged, ShipHullSpecAPI base, ShipHullSpecAPI dParent) {
        return mock(ShipHullSpecAPI.class, (name, values) -> {
            switch (name) {
                case "getHullId": return id;
                case "getBaseHullId": return baseId;
                case "getDParentHullId": return dParentId;
                case "getBaseHull": return base;
                case "getDParentHull": return dParent;
                case "hasTag": return tagged && MirrorCompatibility.OPT_OUT_TAG.equals(values[0]);
                default: return null;
            }
        });
    }

    private static ShipVariantAPI variant(ShipHullSpecAPI hull, boolean tagged, ShipVariantAPI module) {
        return mock(ShipVariantAPI.class, (name, values) -> {
            switch (name) {
                case "getHullSpec": return hull;
                case "hasTag": return tagged && MirrorCompatibility.OPT_OUT_TAG.equals(values[0]);
                case "getModuleSlots": return module == null ? List.of() : List.of("module");
                case "getModuleVariant": return module;
                default: return null;
            }
        });
    }

    private static ShipAIPlugin nativeBasicAI(ShipAIConfig config) throws Exception {
        // Constructor-free diagnostic instance: classification reads only its
        // exact native class and public config; handoff advances only AI clocks.
        Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
        Field singleton = unsafeType.getDeclaredField("theUnsafe");
        singleton.setAccessible(true);
        Object unsafe = singleton.get(null);
        Class<?> type = Class.forName("com.fs.starfarer.combat.ai.BasicShipAI");
        Object ai = unsafeType.getMethod("allocateInstance", Class.class).invoke(unsafe, type);
        Field field = type.getDeclaredField("config");
        field.setAccessible(true);
        field.set(ai, config);
        // The public forceCircumstanceEvaluation only advances these native
        // evaluation clocks. Supply them so the real generic handoff can run.
        Class<?> trackerType = Class.forName("com.fs.starfarer.util.IntervalTracker");
        Object tracker = trackerType.getConstructor(float.class, float.class).newInstance(0.1f, 0.2f);
        Field collision = type.getDeclaredField("collisionCheckTracker");
        collision.setAccessible(true);
        collision.set(ai, tracker);
        Field threat = type.getDeclaredField("threatEvalAI");
        threat.setAccessible(true);
        Object evaluator = unsafeType.getMethod("allocateInstance", Class.class)
                .invoke(unsafe, threat.getType());
        for (Field clock : threat.getType().getDeclaredFields()) {
            if (clock.getType() != trackerType) continue;
            clock.setAccessible(true);
            clock.set(evaluator, tracker);
        }
        threat.set(ai, evaluator);
        return (ShipAIPlugin) ai;
    }
}
