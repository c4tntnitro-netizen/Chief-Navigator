package chiefnavigator.topography;

import chiefnavigator.abilities.SinniAmbushStanceAbility;
import chiefnavigator.quest.SinniBarEvent;
import chiefnavigator.topography.SinniHyperspaceTopographyEventIntel;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.loading.AbilitySpecAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import java.awt.Color;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Runs the actual Running Dark effect and cleanup across the perk threshold. */
public final class SinniRunningDarkRegression {
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static void near(float actual, float expected, String message) {
        check(Math.abs(actual - expected) < 0.0001f,
                message + ": expected " + expected + ", got " + actual);
    }

    public static void main(String[] args) throws Exception {
        SettingsAPI previousSettings = Global.getSettings();
        SectorAPI previousSector = Global.getSector();
        try {
            Map<String, Object> state = new HashMap<>();
            MemoryAPI memory = SinniTopographyLayoutRegression.mock(MemoryAPI.class, (name, values) -> {
                if (name.equals("getBoolean")) return Boolean.TRUE.equals(state.get(values[0]));
                if (name.equals("get")) return state.get(values[0]);
                if (name.equals("set")) state.put((String) values[0], values[1]);
                if (name.equals("contains")) return state.containsKey(values[0]);
                return null;
            });
            AbilitySpecAPI spec = SinniTopographyLayoutRegression.mock(AbilitySpecAPI.class, (name, values) -> {
                if (name.equals("getName")) return "Go Dark";
                if (name.equals("getId")) return Abilities.GO_DARK;
                if (name.equals("getTags")) return Set.of("stealth+", "burn-");
                return null;
            });
            Global.setSettings(SinniTopographyLayoutRegression.mock(SettingsAPI.class, (name, values) -> {
                if (name.equals("getColor")) return Color.WHITE;
                if (name.equals("getAbilitySpec")) return spec;
                return null;
            }));
            StatBonus detection = new StatBonus();
            float[] otherDarkModifier = {1f};
            boolean[] player = {true};
            int[] slowFrames = {0};
            DynamicStatsAPI dynamic = SinniTopographyLayoutRegression.mock(DynamicStatsAPI.class,
                    (name, values) -> name.equals("getValue") ? otherDarkModifier[0] : null);
            MutableFleetStatsAPI stats = SinniTopographyLayoutRegression.mock(MutableFleetStatsAPI.class, (name, values) -> {
                if (name.equals("getDetectedRangeMod")) return detection;
                if (name.equals("getDynamic")) return dynamic;
                return null;
            });
            CampaignFleetAPI fleet = SinniTopographyLayoutRegression.mock(CampaignFleetAPI.class, (name, values) -> {
                if (name.equals("isPlayerFleet")) return player[0];
                if (name.equals("getStats")) return stats;
                if (name.equals("getViews")) return List.of();
                if (name.equals("getAbilities")) return Map.of();
                if (name.equals("goSlowOneFrame")) slowFrames[0]++;
                return null;
            });
            IntelManagerAPI intel = SinniTopographyLayoutRegression.mock(IntelManagerAPI.class, (name, values) -> null);
            ListenerManagerAPI listeners = SinniTopographyLayoutRegression.mock(ListenerManagerAPI.class, (name, values) -> null);
            Global.setSector(SinniTopographyLayoutRegression.mock(SectorAPI.class, (name, values) -> {
                if (name.equals("getMemoryWithoutUpdate")) return memory;
                if (name.equals("getPlayerFleet")) return fleet;
                if (name.equals("getIntelManager")) return intel;
                if (name.equals("getListenerManager")) return listeners;
                return null;
            }));
            SinniHyperspaceTopographyEventIntel event = new SinniHyperspaceTopographyEventIntel(null, false);
            SinniAmbushStanceAbility ability = new SinniAmbushStanceAbility();
            ability.init(Abilities.GO_DARK, fleet);
            Method effect = SinniAmbushStanceAbility.class.getDeclaredMethod("applyEffect", float.class, float.class);
            effect.setAccessible(true);

            state.put(SinniBarEvent.STARTED, true);
            SinniTopographyLayoutRegression.rawProgress(event, 1199);
            effect.invoke(ability, 0.1f, 1f);
            near(detection.computeEffective(1f), 0.5f, "Before 1,200 points, native 50% reduction remains");
            SinniTopographyLayoutRegression.rawProgress(event, 1200);
            effect.invoke(ability, 0.1f, 1f);
            near(detection.computeEffective(1f), 0.35f, "Unlocked Running Dark reduces detection by 65%");
            effect.invoke(ability, 0.1f, 1f);
            near(detection.computeEffective(1f), 0.35f, "Repeated frames do not stack the perk");
            effect.invoke(ability, 0.1f, 0.5f);
            near(detection.computeEffective(1f), 1f, "Native activation gate remains intact");
            effect.invoke(ability, 0.1f, 1f);
            detection.modifyMult("other_mod", 0.9f);
            otherDarkModifier[0] = 0.8f;
            effect.invoke(ability, 0.1f, 1f);
            near(detection.computeEffective(1f), 0.35f * 0.8f * 0.9f, "Other detection modifiers are preserved");
            ability.cleanup();
            near(detection.computeEffective(1f), 0.9f, "Cleanup removes only the ability's modifier");
            detection.unmodify("other_mod");
            otherDarkModifier[0] = 1f;
            player[0] = false;
            effect.invoke(ability, 0.1f, 1f);
            near(detection.computeEffective(1f), 0.5f, "NPC Running Dark remains vanilla");
            player[0] = true;
            state.put(SinniBarEvent.STARTED, false);
            effect.invoke(ability, 0.1f, 1f);
            near(detection.computeEffective(1f), 0.5f, "Sinni recruitment remains required");
            // BaseToggleAbility.cleanup() also applies a zero-level effect.
            check(slowFrames[0] == 9, "Every effect frame, including native cleanup, retains slow movement");
            check(!SinniAmbushStanceAbility.isAmbushReady(fleet), "The retired forced-pursuit effect stays disabled");

            List<String> percentages = new ArrayList<>();
            LabelAPI label = SinniTopographyLayoutRegression.mock(LabelAPI.class, (name, values) -> null);
            TooltipMakerAPI tooltip = SinniTopographyLayoutRegression.mock(TooltipMakerAPI.class, (name, values) -> {
                if (name.equals("addPara")) {
                    for (Object value : values) {
                        if (value instanceof String[]) percentages.addAll(List.of((String[]) value));
                    }
                }
                return name.equals("addTitle") || name.equals("addPara") ? label : null;
            });
            ability.createTooltip(tooltip, false);
            check(percentages.contains("50%"), "Locked ability tooltip retains the native value");
            percentages.clear();
            state.put(SinniBarEvent.STARTED, true);
            ability.createTooltip(tooltip, false);
            check(percentages.contains("65%"), "Unlocked ability tooltip shows the new value before activation");
            System.out.println("PASS: native Running Dark uses 65% reduction at 1,200 points, retains activation/movement/cleanup and other modifiers, and shows correct tooltips.");
        } finally {
            Global.setSector(previousSector);
            Global.setSettings(previousSettings);
        }
    }
}
