package chiefnavigator.topography;

import chiefnavigator.quest.SinniBarEvent;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel.EventStageData;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel.StageIconSize;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip;
import com.fs.starfarer.api.impl.campaign.intel.events.EventFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.ht.HyperspaceTopographyEventIntel;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Save-compatible extension of the vanilla Hyperspace Topography event.
 *
 * The added milestones are visible but inert until Sinni joins. Before that,
 * the vanilla 1,000-point topographic-data payout and reset remain unchanged.
 * Once unlocked, the vanilla payout stage is replaced by five permanent
 * milestones and a new repeatable payout after the last milestone.
 */
public final class SinniHyperspaceTopographyEventIntel extends HyperspaceTopographyEventIntel {
    public static final String SINNI_REQUIREMENT_MET =
            "$chief_navigator_sinni_requirement_met";
    public static final int SLIPSURGE_MASTERY_PROGRESS = 1050;
    public static final int DRIVE_MITES_PROGRESS = 1200;
    public static final int PURSUIT_BURN_PROGRESS = 1350;
    public static final int STORM_RIDER_PROGRESS = 1550;
    public static final int ECHO_MAPPING_PROGRESS = 1750;
    public static final int EXTENDED_DATA_PROGRESS = 1950;

    /**
     * The filler values keep added-stage ordinals above every vanilla Stage
     * ordinal. BaseEventIntel uses enum ordinals when detecting crossed stages.
     */
    public enum SinniStage {
        VANILLA_0,
        VANILLA_1,
        VANILLA_2,
        VANILLA_3,
        VANILLA_4,
        VANILLA_5,
        VANILLA_6,
        SLIPSURGE_MASTERY,
        DRIVE_MITES,
        PURSUIT_BURN,
        EXTENDED_TOPOGRAPHIC_DATA,
        ECHO_MAPPING,
        REPEATABLE_TOPOGRAPHIC_DATA
    }

    public SinniHyperspaceTopographyEventIntel(TextPanelAPI text, boolean withIntelNotification) {
        super(text, withIntelNotification);
    }

    @Override
    protected void setup() {
        super.setup();
        setMaxProgress(EXTENDED_DATA_PROGRESS);

        addStage(SinniStage.SLIPSURGE_MASTERY, SLIPSURGE_MASTERY_PROGRESS,
                StageIconSize.LARGE);
        addStage(SinniStage.DRIVE_MITES, DRIVE_MITES_PROGRESS,
                StageIconSize.LARGE);
        addStage(SinniStage.PURSUIT_BURN, PURSUIT_BURN_PROGRESS,
                StageIconSize.LARGE);
        addStage(SinniStage.EXTENDED_TOPOGRAPHIC_DATA, STORM_RIDER_PROGRESS,
                StageIconSize.LARGE);
        addStage(SinniStage.ECHO_MAPPING, ECHO_MAPPING_PROGRESS,
                StageIconSize.LARGE);
        addStage(SinniStage.REPEATABLE_TOPOGRAPHIC_DATA, EXTENDED_DATA_PROGRESS,
                true, StageIconSize.SMALL);

        for (SinniStage stage : new SinniStage[] {
                SinniStage.SLIPSURGE_MASTERY,
                SinniStage.DRIVE_MITES,
                SinniStage.PURSUIT_BURN
        }) {
            EventStageData data = getDataFor(stage);
            data.keepIconBrightWhenLaterStageReached = true;
            data.sendIntelUpdateOnReaching = isUnlockedBySinni();
        }
        getDataFor(SinniStage.EXTENDED_TOPOGRAPHIC_DATA)
                .sendIntelUpdateOnReaching = isUnlockedBySinni();
        getDataFor(SinniStage.ECHO_MAPPING)
                .sendIntelUpdateOnReaching = isUnlockedBySinni();
        getDataFor(SinniStage.REPEATABLE_TOPOGRAPHIC_DATA)
                .sendIntelUpdateOnReaching = isUnlockedBySinni();

        configureForUnlockState();
    }

    /** Installs the extended event and preserves an existing vanilla save. */
    public static SinniHyperspaceTopographyEventIntel ensureInstalled() {
        HyperspaceTopographyEventIntel current = HyperspaceTopographyEventIntel.get();
        if (current instanceof SinniHyperspaceTopographyEventIntel) {
            SinniHyperspaceTopographyEventIntel extended =
                    (SinniHyperspaceTopographyEventIntel) current;
            extended.ensureCurrentLayout();
            extended.syncVanillaCompletionMarker();
            return extended;
        }

        if (current == null) {
            return new SinniHyperspaceTopographyEventIntel(null, false);
        }

        // Another mod may own the singleton through its own subclass. Ending
        // and partially copying that object would discard its private state
        // and can start an endless replacement fight on every load. Only the
        // exact vanilla implementation is safe to upgrade in place.
        if (current.getClass() != HyperspaceTopographyEventIntel.class) {
            Global.getLogger(SinniHyperspaceTopographyEventIntel.class).warn(
                    "Another mod owns Hyperspace Topography ("
                            + current.getClass().getName()
                            + "); Chief Navigator's extension is disabled");
            return null;
        }

        int savedProgress = current.getProgress();
        Random savedRandom = current.getRandom();
        List<EventFactor> savedFactors = new ArrayList<EventFactor>(current.getFactors());
        Map<Object, Boolean> reached = new HashMap<Object, Boolean>();
        for (EventStageData data : current.getStages()) {
            reached.put(data.id, data.wasEverReached);
        }

        // Prevent the retiring event from notifying factors that immediately
        // continue to belong to the replacement.
        current.getFactors().clear();
        current.endImmediately();

        SinniHyperspaceTopographyEventIntel replacement =
                new SinniHyperspaceTopographyEventIntel(null, false);
        replacement.progress = Math.min(savedProgress, replacement.getMaxProgress());
        replacement.setRandom(savedRandom);
        replacement.getFactors().addAll(savedFactors);
        for (EventStageData data : replacement.getStages()) {
            Boolean wasReached = reached.get(data.id);
            if (wasReached != null) data.wasEverReached = wasReached.booleanValue();
        }
        replacement.configureForUnlockState();
        replacement.syncVanillaCompletionMarker();
        return replacement;
    }

    public static boolean hasCompletedVanillaTree() {
        if (Global.getSector() != null
                && Global.getSector().getMemoryWithoutUpdate().getBoolean(
                        SINNI_REQUIREMENT_MET)) {
            return true;
        }
        HyperspaceTopographyEventIntel event = HyperspaceTopographyEventIntel.get();
        if (event == null) return false;
        EventStageData data = event.getDataFor(Stage.GENERATE_SLIPSURGE);
        boolean completed = event.isStageActive(Stage.GENERATE_SLIPSURGE)
                || (data != null && data.wasEverReached)
                || event.getProgress() >= PROGRESS_5
                || (Global.getSector() != null
                        && Global.getSector().getPlayerFleet() != null
                        && Global.getSector().getPlayerFleet().getAbility(
                                Abilities.GENERATE_SLIPSURGE) != null);
        if (completed && Global.getSector() != null) {
            Global.getSector().getMemoryWithoutUpdate().set(
                    SINNI_REQUIREMENT_MET, true);
        }
        return completed;
    }

    private void syncVanillaCompletionMarker() {
        hasCompletedVanillaTree();
    }

    public static boolean isTierActive(SinniStage stage) {
        HyperspaceTopographyEventIntel event = HyperspaceTopographyEventIntel.get();
        return event instanceof SinniHyperspaceTopographyEventIntel
                && ((SinniHyperspaceTopographyEventIntel) event).isStageActive(stage);
    }

    /** Called at the moment Sinni joins the expedition. */
    public static void unlockForSinni(InteractionDialogAPI dialog) {
        SinniHyperspaceTopographyEventIntel event = ensureInstalled();
        if (event == null) return;
        event.configureForUnlockState();
        if (dialog != null) {
            dialog.getTextPanel().addParagraph(
                    "Sinni adds a set of proprietary techniques to your Hyperspace "
                    + "Topography records. Five new tiers are now available.",
                    Misc.getPositiveHighlightColor());
        }
    }

    public boolean isUnlockedBySinni() {
        return Global.getSector() != null
                && Global.getSector().getMemoryWithoutUpdate()
                        .getBoolean(SinniBarEvent.STARTED);
    }

    private void ensureCurrentLayout() {
        if (getDataFor(SinniStage.ECHO_MAPPING) == null) {
            int savedProgress = progress;
            Map<Object, Boolean> reached = new HashMap<Object, Boolean>();
            for (EventStageData data : getStages()) {
                reached.put(data.id, data.wasEverReached);
            }
            List<EventFactor> savedFactors = new ArrayList<EventFactor>(factors);
            setup();
            progress = Math.min(savedProgress, getMaxProgress());
            factors.addAll(savedFactors);
            for (EventStageData data : getStages()) {
                Boolean wasReached = reached.get(data.id);
                if (wasReached != null) data.wasEverReached = wasReached.booleanValue();
            }
        }
        configureForUnlockState();
    }

    private void configureForUnlockState() {
        boolean unlocked = isUnlockedBySinni();
        EventStageData vanillaData = getDataFor(Stage.TOPOGRAPHIC_DATA);
        if (unlocked && vanillaData != null) {
            getStages().remove(vanillaData);
        }
        for (SinniStage stage : new SinniStage[] {
                SinniStage.SLIPSURGE_MASTERY,
                SinniStage.DRIVE_MITES,
                SinniStage.PURSUIT_BURN,
                SinniStage.EXTENDED_TOPOGRAPHIC_DATA,
                SinniStage.ECHO_MAPPING,
                SinniStage.REPEATABLE_TOPOGRAPHIC_DATA
        }) {
            EventStageData data = getDataFor(stage);
            if (data != null) data.sendIntelUpdateOnReaching = unlocked;
        }
    }

    @Override
    public boolean isStageActive(Object stageId) {
        if (stageId instanceof SinniStage && !isUnlockedBySinni()) return false;
        return super.isStageActive(stageId);
    }

    @Override
    protected void notifyStageReached(EventStageData stage) {
        if (stage.id == SinniStage.REPEATABLE_TOPOGRAPHIC_DATA) {
            if (!isUnlockedBySinni()) return;
            CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
            cargo.addSpecial(new SpecialItemData(Items.TOPOGRAPHIC_DATA, null), 1);
            setProgress(ECHO_MAPPING_PROGRESS);
            return;
        }
        if (stage.id == SinniStage.DRIVE_MITES) {
            return;
        }
        if (stage.id instanceof SinniStage) return;
        super.notifyStageReached(stage);
        if (stage.id == Stage.GENERATE_SLIPSURGE) {
            Global.getSector().getMemoryWithoutUpdate().set(
                    SINNI_REQUIREMENT_MET, true);
            SinniBarEvent.ensureCreatorRegistered();
        }
    }

    @Override
    protected void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode,
                                   boolean isUpdate, Color tc, float initPad) {
        if (isUpdate && getListInfoParam() instanceof EventStageData) {
            EventStageData data = (EventStageData) getListInfoParam();
            if (data.id == SinniStage.SLIPSURGE_MASTERY) {
                info.addPara("Generate Slipsurge usable at any gravity well", tc, initPad);
                return;
            }
            if (data.id == SinniStage.DRIVE_MITES) {
                info.addPara("Ambush Stance unlocked", tc, initPad);
                return;
            }
            if (data.id == SinniStage.PURSUIT_BURN) {
                info.addPara("Slipsurge Mastery unlocked", tc, initPad);
                return;
            }
            if (data.id == SinniStage.EXTENDED_TOPOGRAPHIC_DATA) {
                info.addPara("Storm Rider unlocked", tc, initPad);
                return;
            }
            if (data.id == SinniStage.ECHO_MAPPING) {
                info.addPara("Echo Mapping unlocked", tc, initPad);
                return;
            }
            if (data.id == SinniStage.REPEATABLE_TOPOGRAPHIC_DATA) {
                info.addPara("Topographic data gained", tc, initPad);
                return;
            }
        }
        super.addBulletPoints(info, mode, isUpdate, tc, initPad);
    }

    @Override
    public void addStageDesc(TooltipMakerAPI info, Object stageId,
                             float initPad, boolean forTooltip) {
        if (!(stageId instanceof SinniStage)) {
            super.addStageDesc(info, stageId, initPad, forTooltip);
            return;
        }

        Color highlight = Misc.getHighlightColor();
        if (!isUnlockedBySinni()) {
            info.addPara("Locked. Recruit %s to unlock this additional "
                            + "Hyperspace Topography tier.", initPad,
                    highlight, "Sinni");
            return;
        }

        if (stageId == SinniStage.SLIPSURGE_MASTERY) {
            info.addPara("Sinni can activate %s from any gravity well, rather "
                            + "than only the most powerful ones.", initPad,
                    highlight, "Generate Slipsurge");
        } else if (stageId == SinniStage.DRIVE_MITES) {
            info.addPara("Increases the detection-range reduction of %s to %s.",
                    initPad, highlight, "Running Dark", "65%");
        } else if (stageId == SinniStage.PURSUIT_BURN) {
            info.addPara("Sinni's %s makes generated slipsurges twice as wide and "
                            + "three times as long.", initPad, highlight,
                    "Slipsurge Mastery");
        } else if (stageId == SinniStage.EXTENDED_TOPOGRAPHIC_DATA) {
            info.addPara("Sinni's %s technique reduces combat-readiness damage "
                            + "from hyperspace storms by %s.", initPad, highlight,
                    "Storm Rider", "50%");
        } else if (stageId == SinniStage.ECHO_MAPPING) {
            info.addPara("Sinni's %s technique causes each manual %s to deploy "
                            + "a temporary warning beacon. The beacon refires the "
                            + "burst three times at %s-day intervals, revealing "
                            + "contacts around its fixed location and drawing "
                            + "nearby hostile fleets toward the signal.",
                    initPad, highlight, "Echo Mapping", "Active Sensor Burst", "3");
        } else if (stageId == SinniStage.REPEATABLE_TOPOGRAPHIC_DATA) {
            info.addPara("A batch of topographic data that can be sold for a "
                    + "considerable number of credits.", initPad);
            info.addPara("Event progress resets to %s points when this outcome "
                            + "is reached.", 10f, highlight,
                            Integer.toString(ECHO_MAPPING_PROGRESS));
        }
    }

    @Override
    public TooltipCreator getStageTooltipImpl(final Object stageId) {
        if (!(stageId instanceof SinniStage)) {
            return super.getStageTooltipImpl(stageId);
        }
        final EventStageData data = getDataFor(stageId);
        return new BaseFactorTooltip() {
            @Override
            public void createTooltip(TooltipMakerAPI tooltip, boolean expanded,
                                      Object tooltipParam) {
                tooltip.addTitle(getSinniStageTitle((SinniStage) stageId));
                addStageDesc(tooltip, stageId, 10f, true);
                if (isUnlockedBySinni()) data.addProgressReq(tooltip, 10f);
            }
        };
    }

    private String getSinniStageTitle(SinniStage stage) {
        if (stage == SinniStage.SLIPSURGE_MASTERY) return "Slipsurge Attunement";
        if (stage == SinniStage.DRIVE_MITES) return "Ambush Stance";
        if (stage == SinniStage.PURSUIT_BURN) return "Slipsurge Mastery";
        if (stage == SinniStage.EXTENDED_TOPOGRAPHIC_DATA) return "Storm Rider";
        if (stage == SinniStage.ECHO_MAPPING) return "Echo Mapping";
        return "Topographic Data";
    }

    @Override
    protected String getStageIconImpl(Object stageId) {
        if (stageId == SinniStage.SLIPSURGE_MASTERY) {
            return Global.getSettings().getAbilitySpec(Abilities.GENERATE_SLIPSURGE)
                    .getIconName();
        }
        if (stageId == SinniStage.DRIVE_MITES) {
            return Global.getSettings().getAbilitySpec(Abilities.GO_DARK)
                    .getIconName();
        }
        if (stageId == SinniStage.PURSUIT_BURN) {
            return Global.getSettings().getAbilitySpec(Abilities.GENERATE_SLIPSURGE)
                    .getIconName();
        }
        if (stageId == SinniStage.EXTENDED_TOPOGRAPHIC_DATA) {
            return Global.getSettings().getAbilitySpec(Abilities.EMERGENCY_BURN)
                    .getIconName();
        }
        if (stageId == SinniStage.ECHO_MAPPING) {
            return Global.getSettings().getAbilitySpec(Abilities.SENSOR_BURST)
                    .getIconName();
        }
        if (stageId == SinniStage.REPEATABLE_TOPOGRAPHIC_DATA) {
            return Global.getSettings().getSpriteName(
                    "events", "hyperspace_topography_TOPOGRAPHIC_DATA");
        }
        return super.getStageIconImpl(stageId);
    }

    @Override
    protected float getStageDownLineLength(Object stageId) {
        if (separateVanillaPayoutMarker(stageId)) {
            // The 1,000-point payout and 1,050-point Sinni tier are only
            // 19 pixels apart on the native bar. Lift the small payout icon
            // above the first Sinni icon without changing either threshold.
            return super.getStageDownLineLength(stageId) + 48f;
        }
        return super.getStageDownLineLength(stageId);
    }

    private boolean separateVanillaPayoutMarker(Object stageId) {
        return stageId == Stage.TOPOGRAPHIC_DATA
                && !isUnlockedBySinni()
                && getDataFor(SinniStage.SLIPSURGE_MASTERY) != null;
    }

    @Override
    protected String getStageLabel(Object stageId) {
        // Native marker labels remain at bar height even when their icons
        // are lifted. Keep the payout's cost in its existing tooltip instead
        // of letting "1000" overlap the adjacent "Locked" label.
        if (separateVanillaPayoutMarker(stageId)) return null;
        if (stageId instanceof SinniStage && !isUnlockedBySinni()) return "Locked";
        return super.getStageLabel(stageId);
    }

    @Override
    protected Color getStageColor(Object stageId) {
        if (stageId instanceof SinniStage && !isUnlockedBySinni()) {
            return new Color(70, 70, 75);
        }
        return super.getStageColor(stageId);
    }

    @Override
    protected Color getStageIconColor(Object stageId) {
        if (stageId instanceof SinniStage && !isUnlockedBySinni()) {
            return new Color(255, 255, 255, 70);
        }
        return super.getStageIconColor(stageId);
    }

    @Override
    protected String getSoundForStageReachedUpdate(Object stageId) {
        if (stageId == SinniStage.SLIPSURGE_MASTERY
                || stageId == SinniStage.DRIVE_MITES
                || stageId == SinniStage.PURSUIT_BURN
                || stageId == SinniStage.EXTENDED_TOPOGRAPHIC_DATA
                || stageId == SinniStage.ECHO_MAPPING) {
            return "ui_learned_ability";
        }
        return super.getSoundForStageReachedUpdate(stageId);
    }
}
