package chiefnavigator.quest;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.CrewCompositionAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.impl.combat.CRPluginImpl;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.BiFunction;

/** Native CR crew modifiers with detached generated members and real completion handoff. */
public final class UngaikyoReadinessRegression {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void close(float actual, float expected, String message) {
        check(Math.abs(actual - expected) < 0.00001f, message + ": " + actual + " != " + expected);
    }
    private static <T> T mock(Class<T> type, BiFunction<String, Object[], Object> callback) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> {
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == args[0];
                    Object result = callback.apply(method.getName(), args);
                    if (result != null) return result;
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == float.class) return 0f;
                    if (method.getReturnType() == int.class) return 0;
                    return null;
                }));
    }

    private static final class Member {
        final float minimum;
        float crew, rawCR = 0.13f;
        int crewWrites, updates;
        final MutableStat maximum = new MutableStat(0f);
        final CrewCompositionAPI staffing = mock(CrewCompositionAPI.class, (name, args) -> {
            if (name.equals("getCrew")) return crew;
            if (name.equals("addCrew")) { crew += (Float) args[0]; crewWrites++; }
            return null;
        });
        final MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class,
                (name, args) -> name.equals("getMaxCombatReadiness") ? maximum : null);
        final RepairTrackerAPI repair = mock(RepairTrackerAPI.class, (name, args) -> {
            // These are the installed native RepairTracker's exact public rules:
            // maxCR clamps the modified stat; effective CR also scales by staffing.
            if (name.equals("getMaxCR")) return Math.max(0f, Math.min(1f, maximum.getModifiedValue()));
            if (name.equals("getCR")) return rawCR * fraction();
            if (name.equals("getBaseCR")) return rawCR;
            if (name.equals("setCR")) rawCR = (Float) args[0];
            return null;
        });
        final PersonAPI captain = mock(PersonAPI.class, (name, args) -> null);
        final FleetMemberAPI api;

        Member(float minimum, float crew, float authoredBonus) {
            this.minimum = minimum;
            this.crew = crew;
            maximum.modifyFlat("authored readiness modifier", authoredBonus);
            api = mock(FleetMemberAPI.class, (name, args) -> {
                switch (name) {
                    case "getStats": return stats;
                    case "getRepairTracker": return repair;
                    case "getCrewComposition": return staffing;
                    case "getMinCrew": return this.minimum;
                    case "getCrewFraction": return fraction();
                    case "getCaptain": return captain;
                    case "updateStats": refresh(); break;
                }
                return null;
            });
            refresh();
        }
        float fraction() { return minimum <= 0f ? 1f : Math.min(1f, crew / minimum); }
        void refresh() {
            updates++;
            // Actual installed game code, not a constant 0.7 getMaxCR stub.
            new CRPluginImpl().applyMaxCRCrewModifiers(api);
        }
    }

    private static final class Ship {
        final Member member;
        final List<ShipAPI> children = new ArrayList<>();
        final Map<String, Object> data = new HashMap<>();
        float current = 0.2f, deployment = 0.2f;
        boolean alive = true;
        int evaluations;
        final ShipAIConfig config = new ShipAIConfig(null);
        final ShipAIPlugin ai = mock(ShipAIPlugin.class, (name, args) -> {
            if (name.equals("getConfig")) return config;
            if (name.equals("forceCircumstanceEvaluation")) evaluations++;
            return null;
        });
        final ShipAPI api;
        Ship(Member member) {
            this.member = member;
            api = mock(ShipAPI.class, (name, args) -> {
                switch (name) {
                    case "getFleetMember": return member == null ? null : member.api;
                    case "getCaptain": return member == null ? null : member.captain;
                    case "getChildModulesCopy": return children;
                    case "getCustomData": return data;
                    case "getShipAI": return ai;
                    case "isAlive": return alive;
                    case "getCurrentCR": return current;
                    case "getCRAtDeployment": return deployment;
                    case "setCurrentCR": current = (Float) args[0]; break;
                    case "setCRAtDeployment": deployment = (Float) args[0]; break;
                }
                return null;
            });
        }
    }

    public static void main(String[] args) throws Exception {
        Method fill = GautamaFinalRematchBattleCreationPlugin.class
                .getDeclaredMethod("setMemberToFullCR", FleetMemberAPI.class);
        fill.setAccessible(true); // External harness only; production uses public game APIs.

        Member source = new Member(200f, 20f, 0.15f);
        float sourceRaw = source.rawCR, sourceCrew = source.crew;
        float sourceEffective = source.repair.getCR(), sourceMax = source.repair.getMaxCR();
        int sourceWrites = source.crewWrites, sourceUpdates = source.updates;

        Member empty = new Member(200f, 0f, 0f);
        close(empty.repair.getMaxCR(), 0.2f, "Native zero-staffing reproduces the 20% cap");
        close(empty.repair.getCR(), 0f, "Native effective CR also scales by empty staffing");
        fill.invoke(null, empty.api);
        close(empty.crew, 200f, "Only the generated member receives its minimum staffing");
        close(empty.repair.getMaxCR(), 0.7f, "Staffing restores the native ordinary cap");
        close(empty.rawCR, 0.7f, "Generated raw CR reaches its cap");
        close(empty.repair.getCR(), 0.7f, "Generated effective CR reaches its cap");

        for (float bonus : new float[]{0.15f, -0.1f, 0.5f}) {
            Member modified = new Member(100f, 25f, bonus);
            fill.invoke(null, modified.api);
            float expected = Math.min(1f, 0.7f + bonus);
            close(modified.repair.getMaxCR(), expected, "Preserve authored positive/negative CR modifiers");
            close(modified.repair.getCR(), expected, "Use the resulting normal maximum, without a hardcoded override");
            close(modified.crew, 100f, "Fill exactly the staffing shortage");
        }
        Member automated = new Member(0f, 0f, 0.1f);
        fill.invoke(null, automated.api);
        check(automated.crewWrites == 0, "Zero-crew automated hulls need no synthetic staffing");
        close(automated.repair.getCR(), 0.8f, "Automated hulls retain their own normal CR cap");
        Member staffed = new Member(100f, 250f, 0f);
        int staffedUpdates = staffed.updates;
        fill.invoke(null, staffed.api);
        check(staffed.crewWrites == 0 && staffed.updates == staffedUpdates,
                "Already staffed clones must not gain extra crew or unnecessary stat refreshes");

        Member rootMember = new Member(200f, 0f, 0.15f);
        Member moduleMember = new Member(40f, 0f, -0.1f);
        Ship root = new Ship(rootMember), module = new Ship(moduleMember);
        Ship memberless = new Ship(null), destroyed = new Ship(new Member(40f, 0f, 0f));
        destroyed.alive = false;
        root.children.addAll(List.of(module.api, memberless.api, destroyed.api));
        GautamaFinalRematchBattleCreationPlugin.activateCompletedMirror(root.api, rootMember.api, 1);
        close(root.current, 0.85f, "Completed root current CR");
        close(root.deployment, 0.85f, "Completed root deployment CR");
        close(module.current, 0.6f, "Completed owned module respects its own modified cap");
        close(module.deployment, 0.6f, "Completed owned module deployment CR");
        close(moduleMember.repair.getCR(), 0.6f, "Completed module member effective CR");
        close(memberless.current, 0.85f, "Memberless module inherits generated root readiness");
        close(memberless.deployment, 0.85f, "Memberless module deployment CR");
        close(destroyed.current, 0.2f, "Never restore a destroyed module");
        check(destroyed.member.crewWrites == 0 && root.evaluations == 1 && module.evaluations == 1,
                "Readiness initialization preserves the native one-time AI handoff");

        close(source.crew, sourceCrew, "Player staffing untouched");
        close(source.rawCR, sourceRaw, "Player raw CR untouched");
        close(source.repair.getCR(), sourceEffective, "Player effective CR untouched");
        close(source.repair.getMaxCR(), sourceMax, "Player readiness modifiers untouched");
        check(source.crewWrites == sourceWrites && source.updates == sourceUpdates,
                "Do not refresh or refill the player's source member");
        System.out.println("PASS: native 20% empty-crew reproduction, generated-only staffing, normal modified raw/effective CR, "
                + "automated hulls, completed modules/memberless fallback, one-time AI handoff and untouched player readiness.");
    }
}
