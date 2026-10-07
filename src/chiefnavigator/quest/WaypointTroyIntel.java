package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.Set;

/** Persistent top-level Intel entry for the Orion Knot story. */
public final class WaypointTroyIntel extends BaseIntelPlugin {
    private static final String BUTTON_SINNI_RECOMMENDATION =
            "chief_navigator_adept_sinni_recommendation";
    private static final String BUTTON_ORION_KNOT_MAP =
            "chief_navigator_adept_orion_knot_map";
    private static final String SINNI_CONTACT_TRIGGER =
            "ChiefNavigatorSinniRemoteContact";
    private static final String ORION_KNOT_MAP_TRIGGER =
            "ChiefNavigatorSinniOrionKnotMap";

    public static void ensureExists(TextPanelAPI text) {
        if (Global.getSector() == null
                || !Global.getSector().getMemoryWithoutUpdate().getBoolean(
                        SinniBarEvent.STARTED)) return;
        for (IntelInfoPlugin intel : Global.getSector().getIntelManager()
                .getIntel(WaypointTroyIntel.class)) {
            if (intel instanceof WaypointTroyIntel
                    && !intel.isEnded() && !intel.isEnding()) {
                ((WaypointTroyIntel) intel).setImportant(Boolean.TRUE);
                return;
            }
        }
        WaypointTroyIntel intel = new WaypointTroyIntel();
        intel.setImportant(Boolean.TRUE);
        Global.getSector().getIntelManager().addIntel(intel, false, text);
    }

    /** Posts the Eventide briefing as an update to the persistent story entry. */
    public static void briefingComplete() {
        ensureExists(null);
        for (IntelInfoPlugin intel : Global.getSector().getIntelManager()
                .getIntel(WaypointTroyIntel.class)) {
            if (intel instanceof WaypointTroyIntel && !intel.isEnded()) {
                ((WaypointTroyIntel) intel).sendUpdateIfPlayerHasIntel(
                        "eventide_briefed", false);
            }
        }
    }

    /** Advances the expedition marker from Troy to FOB Ithaca. */
    public static void revealFobIthacaObjective() {
        ensureExists(null);
        SectorEntityToken ithaca = OdysseyExpanseSystem.getFobIthaca();
        if (ithaca != null) {
            Misc.makeImportant(ithaca, "chief_navigator_fob_ithaca_objective");
        }
        for (IntelInfoPlugin intel : Global.getSector().getIntelManager()
                .getIntel(WaypointTroyIntel.class)) {
            if (intel instanceof WaypointTroyIntel && !intel.isEnded()) {
                ((WaypointTroyIntel) intel).sendUpdateIfPlayerHasIntel(
                        "fob_ithaca", false);
            }
        }
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        info.addPara(getName(), getTitleColor(mode), 0f);
        info.addPara(getStatus(), 3f, Misc.getGrayColor(), Misc.getHighlightColor(), getStatus());
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        info.addImage(SinniContact.getPortraitSprite(),
                width, 128f, 10f);
        if (!SinniEventideIntel.isBriefed()) {
            info.addPara("Sinni has joined your fleet and invited you to take "
                    + "tea at her private office on Eventide. She will explain "
                    + "the expedition there.", 10f);
        } else if (Global.getSector().getMemoryWithoutUpdate().getBoolean(
                OdysseyExpanseSystem.ENTERED)
                && !MenelausTrial.isAccepted()) {
            info.addPara("Sinni has guided the expedition through the Troy Terminus and marked FOB Ithaca in Ashen Verge. Reach the installation and establish contact with whoever still commands it.", 10f);
        } else if (!Global.getSector().getMemoryWithoutUpdate().getBoolean(
                OdysseyExpanseSystem.ENTERED)) {
            info.addPara("Sinni has joined your expedition under a cooperative charter. Travel to the expedition anchorage in Waypoint Troy, use its free storage for any ships and cargo you do not intend to bring, and open the route with a fleet worth no more than 30 deployment points.", 10f);
        } else {
            info.addPara("The expedition has reached the Orion Knot. This "
                    + "entry tracks Sinni's journey and the struggle for FOB "
                    + "Ithaca through every stage of the story.", 10f);
        }
        if (Global.getSector().getMemoryWithoutUpdate().getBoolean(
                OdysseyExpanseSystem.ENTERED)) {
            info.addPara("Sinni's Orion Knot map reviews the expedition's "
                    + "current position and the six systems of the Orion "
                    + "Knot.", 10f);
        }
        SinniNavigationGuidance.Guidance guidance =
                SinniNavigationGuidance.getCurrentGuidance();
        info.addPara("Objective: %s", 10f, Misc.getTextColor(),
                Misc.getHighlightColor(), guidance.getObjective());
        info.addButton("Ask Sinni where to go next",
                BUTTON_SINNI_RECOMMENDATION, width, 20f, 10f);
        if (Global.getSector().getMemoryWithoutUpdate().getBoolean(
                OdysseyExpanseSystem.ENTERED)) {
            info.addButton("Open Sinni's Orion Knot map",
                    BUTTON_ORION_KNOT_MAP, width, 20f, 10f);
        }
    }

    @Override
    public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        if (BUTTON_SINNI_RECOMMENDATION.equals(buttonId)) {
            if (ui != null && Global.getSector() != null) {
                ui.showDialog(null, SINNI_CONTACT_TRIGGER);
            }
            return;
        }
        if (BUTTON_ORION_KNOT_MAP.equals(buttonId)) {
            if (ui != null && Global.getSector() != null
                    && Global.getSector().getMemoryWithoutUpdate().getBoolean(
                            OdysseyExpanseSystem.ENTERED)) {
                ui.showDialog(null, ORION_KNOT_MAP_TRIGGER);
            }
            return;
        }
        super.buttonPressConfirmed(buttonId, ui);
    }

    private String getStatus() {
        if (!SinniEventideIntel.isBriefed()) {
            return "Meet Sinni at Eventide";
        }
        if (Global.getSector().getMemoryWithoutUpdate().getBoolean(
                OdysseyExpanseSystem.ENTERED)
                && !MenelausTrial.isAccepted()) {
            return "Reach FOB Ithaca in Ashen Verge";
        }
        if (!Global.getSector().getMemoryWithoutUpdate().getBoolean(
                OdysseyExpanseSystem.ENTERED)
                && Global.getSector().getMemoryWithoutUpdate().getBoolean(
                TroyArrivalScript.WORMHOLE_OPENED)) {
            return "Enter the Troy Terminus";
        }
        if (!Global.getSector().getMemoryWithoutUpdate().getBoolean(
                OdysseyExpanseSystem.ENTERED)
                && Global.getSector().getMemoryWithoutUpdate()
                .getBoolean(TreadmillEntryScript.ARRIVAL_NOTICE_SHOWN)) {
            return "Prepare at the Troy anchorage (30 DP maximum)";
        }
        if (!Global.getSector().getMemoryWithoutUpdate().getBoolean(
                OdysseyExpanseSystem.ENTERED)) {
            return "Travel to Waypoint Troy";
        }

        int stage = MenelausTrial.getCurrentLaborStage();
        if (MenelausTrial.isLaborReportPending(stage)) {
            return "Return to Menelaus at FOB Ithaca";
        }
        if (stage == MenelausTrial.LABOR_MOTHERSHIPS) {
            return "Complete Labor I: Guard Motherships";
        }
        if (stage == MenelausTrial.LABOR_RESEARCH) {
            return "Complete Labor II: Salvaged Knowledge";
        }
        if (stage == MenelausTrial.LABOR_WALL) {
            return "Complete Labor III: The Drifting Wall";
        }
        if (stage == MenelausTrial.LABOR_SENSOR) {
            return "Complete Labor IV: The Listening Post";
        }
        if (stage == MenelausTrial.LABOR_GAUTAMA) {
            return MenelausTrial.isFinalDebriefPending()
                    ? "Return to Menelaus"
                    : "Complete Labor V: Heavenly Strike";
        }
        if (stage == MenelausTrial.LABOR_COMPLETE) {
            return "Scan Ithaca's Janus Gate";
        }
        return "Continue the Orion Knot expedition";
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        return SinniNavigationGuidance.getCurrentGuidance().getTarget();
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(Tags.INTEL_MISSIONS);
        tags.add(Tags.INTEL_ACCEPTED);
        return tags;
    }

    @Override public String getName() {
        return "Adept of the Stars and Waves";
    }

    @Override
    public String getIcon() {
        return SinniContact.getPortraitSprite();
    }
}
