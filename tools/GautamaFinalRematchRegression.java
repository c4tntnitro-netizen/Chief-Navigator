package chiefnavigator.quest;

import chiefnavigator.ai.GuardDroneAI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.combat.threat.ConstructionSwarmSystemScript;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EnumSet;

/** Pure contract checks for Ungaikyo reflection filtering and pacing. */
public final class GautamaFinalRematchRegression {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(
            Class<T> type, InvocationHandler handler) {
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

    private static ShipVariantAPI variant(
            final boolean fighter,
            final boolean module,
            final boolean civilian) {
        return proxy(ShipVariantAPI.class, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("isFighter".equals(method.getName())) return fighter;
                if ("getHints".equals(method.getName())) {
                    EnumSet<ShipTypeHints> hints =
                            EnumSet.noneOf(ShipTypeHints.class);
                    if (module) hints.add(ShipTypeHints.MODULE);
                    if (civilian) hints.add(ShipTypeHints.CIVILIAN);
                    return hints;
                }
                return defaultValue(method.getReturnType());
            }
        });
    }

    private static FleetMemberAPI member(
            final String id,
            final ShipVariantAPI variant,
            final boolean wing,
            final boolean mothballed,
            final boolean deployable) {
        return proxy(FleetMemberAPI.class, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String name = method.getName();
                if ("getId".equals(name)) return id;
                if ("getVariant".equals(name)) return variant;
                if ("isFighterWing".equals(name)) return wing;
                if ("isMothballed".equals(name)) return mothballed;
                if ("canBeDeployedForCombat".equals(name)) {
                    return deployable;
                }
                return defaultValue(method.getReturnType());
            }
        });
    }

    public static void main(String[] args) throws Exception {
        check(Personalities.RECKLESS.equals(
                        GautamaFinalRematchBattleCreationPlugin
                                .MIRROR_PERSONALITY),
                "Every mirror member must use the Reckless/Fearless personality");
        ShipAIConfig fearless = GuardDroneAI.createFearlessConfig();
        check(Personalities.RECKLESS.equals(fearless.personalityOverride)
                        && fearless.alwaysStrafeOffensively
                        && !fearless.backingOffWhileNotVentingAllowed
                        && !fearless.turnToFaceWithUndamagedArmor
                        && fearless.burnDriveIgnoreEnemies,
                "Mirror AI must retain the complete vanilla-style Fearless configuration");
        check(GautamaFinalRematchBattleCreationPlugin
                        .isNonfatalMirrorFailure(
                                new RuntimeException("mod ship"))
                        && GautamaFinalRematchBattleCreationPlugin
                                .isNonfatalMirrorFailure(
                                        new LinkageError("optional mod"))
                        && !GautamaFinalRematchBattleCreationPlugin
                                .isNonfatalMirrorFailure(
                                        new OutOfMemoryError("fatal"))
                        && !GautamaFinalRematchBattleCreationPlugin
                                .isNonfatalMirrorFailure(new ThreadDeath())
                        && !GautamaFinalRematchBattleCreationPlugin
                                .isNonfatalMirrorFailure(null),
                "Only nonfatal third-party failures may quarantine one mirror slot");

        ShipVariantAPI ordinary = variant(false, false, false);
        check(GautamaFinalRematchBattleCreationPlugin
                        .isEligiblePlayerMember(member(
                                "deployed", ordinary,
                                false, false, true)),
                "An ordinary deployable campaign ship must be eligible");
        check(!GautamaFinalRematchBattleCreationPlugin
                        .isEligiblePlayerMember(member(
                                null, ordinary,
                                false, false, true)),
                "A ship without a stable campaign-member id must be rejected");
        check(!GautamaFinalRematchBattleCreationPlugin
                        .isEligiblePlayerMember(member(
                                "wing", ordinary,
                                true, false, true)),
                "Fighter-wing members must not create mirror slots");
        check(GautamaFinalRematchBattleCreationPlugin
                        .isEligiblePlayerMember(member(
                                "mothballed", ordinary,
                                false, true, true)),
                "Mothballed campaign ships remain part of the summon pool");
        check(GautamaFinalRematchBattleCreationPlugin
                        .isEligiblePlayerMember(member(
                                "undeployable", ordinary,
                                false, false, false)),
                "Campaign ships are selected from the roster, not deployment state");
        check(!GautamaFinalRematchBattleCreationPlugin
                        .isEligiblePlayerMember(member(
                                "module", variant(false, true, false),
                                false, false, true)),
                "Station modules must not create independent mirror slots");
        check(!GautamaFinalRematchBattleCreationPlugin
                        .isEligiblePlayerMember(member(
                                "fighter", variant(true, false, false),
                                false, false, true)),
                "Fighter variants must not create mirror slots");
        FleetMemberAPI civilian = member(
                "civilian", variant(false, false, true),
                false, false, true);
        check(GautamaFinalRematchBattleCreationPlugin
                        .isCivilianMember(civilian),
                "The CIVILIAN hull hint must drive substitution");
        check("skirmish_unit_Type100".equals(
                        GautamaFinalRematchBattleCreationPlugin
                                .getCivilianReplacementVariantId(
                                        ShipAPI.HullSize.FRIGATE))
                        && "assault_unit_Type200".equals(
                        GautamaFinalRematchBattleCreationPlugin
                                .getCivilianReplacementVariantId(
                                        ShipAPI.HullSize.DESTROYER))
                        && "standoff_unit_Type300".equals(
                        GautamaFinalRematchBattleCreationPlugin
                                .getCivilianReplacementVariantId(
                                        ShipAPI.HullSize.CRUISER))
                        && "fabricator_unit_Type450".equals(
                        GautamaFinalRematchBattleCreationPlugin
                                .getCivilianReplacementVariantId(
                                        ShipAPI.HullSize.CAPITAL_SHIP)),
                "Civilian substitutions must preserve stock THREAT size tiers");
        check(GautamaFinalRematchBattleCreationPlugin.SUMMON_DP_BUDGET
                        == 240f
                        && GautamaFinalRematchBattleCreationPlugin
                                .MILITARY_DP_FLOOR == 200f
                        && GautamaFinalRematchBattleCreationPlugin
                                .fitsSummonBudget(200f, 40f)
                        && !GautamaFinalRematchBattleCreationPlugin
                                .fitsSummonBudget(220f, 25f),
                "The opening pool must use a 240-DP cap and 200-DP civilian fallback floor");

        float frigateTime = GautamaFinalRematchBattleCreationPlugin
                .getConstructionTime(5f);
        float destroyerTime = GautamaFinalRematchBattleCreationPlugin
                .getConstructionTime(20f);
        float capitalTime = GautamaFinalRematchBattleCreationPlugin
                .getConstructionTime(40f);
        check(frigateTime == 4f
                        && Math.abs(destroyerTime - 7f) < 0.0001f
                        && capitalTime == 12f
                        && GautamaFinalRematchBattleCreationPlugin
                                .getConstructionTime(100f) == 12f,
                "Mirror construction must preserve vanilla DP ordering inside "
                        + "the encounter's bounded 4-12 second window");
        String controller = Files.readString(Paths.get(
                "src/chiefnavigator/quest/"
                        + "GautamaFinalRematchBattleCreationPlugin.java"),
                StandardCharsets.UTF_8);
        check(controller.contains("mirror.setDefaultAI(member);")
                        && controller.contains("mirror.setControlsLocked(false);")
                        && controller.contains("mirror.setHoldFire(false);")
                        && controller.contains("mirror.setOriginalOwner(enemyOwner);")
                        && controller.contains("ai.forceCircumstanceEvaluation();"),
                "A finished mirror must be returned to enemy ownership, "
                        + "unlocked, and handed back to active combat AI");
        check(controller.contains("snapshotPass(members, false, 0f)")
                        && controller.contains("spent < MILITARY_DP_FLOOR")
                        && controller.contains("snapshotPass(members, true, spent)")
                        && controller.contains("INITIAL_SUMMON_DURATION")
                        && controller.contains("MAX_CONCURRENT_RESURRECTIONS = 1"),
                "Military reflections must be selected first, civilians only "
                        + "below 200 DP, with one-at-a-time resurrection");
        check(GautamaFinalRematchBattleCreationPlugin.cloudScale(
                        ShipAPI.HullSize.FRIGATE) == 1f
                        && GautamaFinalRematchBattleCreationPlugin.cloudScale(
                                ShipAPI.HullSize.DESTROYER) == 1.6f
                        && GautamaFinalRematchBattleCreationPlugin.cloudScale(
                                ShipAPI.HullSize.CRUISER) == 2.5f
                        && GautamaFinalRematchBattleCreationPlugin.cloudScale(
                                ShipAPI.HullSize.CAPITAL_SHIP) == 4f,
                "Construction clouds must scale predictably by hull size");
        check(GautamaFinalRematchBattleCreationPlugin.smoothstep(-1f) == 0f
                        && GautamaFinalRematchBattleCreationPlugin
                                .smoothstep(0.5f) == 0.5f
                        && GautamaFinalRematchBattleCreationPlugin
                                .smoothstep(2f) == 1f,
                "Construction silhouette fade must remain clamped and smooth");

        System.out.println("PASS: Ungaikyo's 240/200-DP selection, civilian "
                + "THREAT substitutions, opening summon, one-at-a-time "
                + "resurrection, Fearless AI hand-off, failure isolation, "
                + "and THREAT construction visuals remain intact.");
    }
}
