package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import java.util.Set;

/** Eventide briefing state and migration for the persistent story Intel. */
public final class SinniEventideIntel extends BaseIntelPlugin {
    public static final String BRIEFED =
            "$chief_navigator_eventide_briefed";
    private static final String EVENTIDE_MARKER =
            "chief_navigator_sinni_eventide_meeting";
    private static final String EVENTIDE_MARKET_ID = "eventide";

    private boolean completed;

    public static boolean isBriefed() {
        if (Global.getSector() == null) return false;
        boolean savedProgress = Global.getSector().getMemoryWithoutUpdate()
                .getBoolean(BRIEFED)
                || Global.getSector().getMemoryWithoutUpdate().getBoolean(
                        TroyArrivalScript.WORMHOLE_OPENED)
                || Global.getSector().getMemoryWithoutUpdate().getBoolean(
                        TreadmillEntryScript.ARRIVAL_NOTICE_SHOWN)
                || Global.getSector().getMemoryWithoutUpdate().getBoolean(
                        OdysseyExpanseSystem.ENTERED);
        if (savedProgress) return true;
        if (Global.getSector().getPlayerFleet() == null
                || !(Global.getSector().getPlayerFleet()
                        .getContainingLocation() instanceof StarSystemAPI)) {
            return false;
        }
        StarSystemAPI current = (StarSystemAPI) Global.getSector()
                .getPlayerFleet().getContainingLocation();
        return current == TroyArrivalScript.findWaypointTroy();
    }

    /** Adds the persistent story Intel at its initial Eventide objective. */
    public static SinniEventideIntel ensureExists(TextPanelAPI text) {
        if (Global.getSector() == null
                || !Global.getSector().getMemoryWithoutUpdate().getBoolean(
                        SinniBarEvent.STARTED)
                || isBriefed()) {
            return null;
        }
        retireActive(false);
        WaypointTroyIntel.ensureExists(text);
        markEventide(true);
        return null;
    }

    /** Migrates the former Tea-at-Eventide Intel into one persistent story. */
    public static void reconcile() {
        if (Global.getSector() == null
                || !Global.getSector().getMemoryWithoutUpdate().getBoolean(
                        SinniBarEvent.STARTED)) {
            return;
        }
        if (isBriefed()) {
            Global.getSector().getMemoryWithoutUpdate().set(BRIEFED, true);
            markEventide(false);
        } else {
            markEventide(true);
        }
        retireActive(false);
        WaypointTroyIntel.ensureExists(null);
        TroyArrivalScript.refreshObjectiveVisibility();
    }

    /** Completes the tea meeting and reveals the expedition destination. */
    public static void complete(TextPanelAPI text) {
        if (Global.getSector() == null) return;
        Global.getSector().getMemoryWithoutUpdate().set(BRIEFED, true);
        retireActive(false);
        markEventide(false);
        WaypointTroyIntel.ensureExists(text);
        WaypointTroyIntel.briefingComplete();
        TroyArrivalScript.refreshObjectiveVisibility();
    }

    private static void retireActive(boolean asCompleted) {
        for (IntelInfoPlugin item : Global.getSector().getIntelManager()
                .getIntel(SinniEventideIntel.class)) {
            if (!(item instanceof SinniEventideIntel)
                    || item.isEnded() || item.isEnding()) continue;
            SinniEventideIntel intel = (SinniEventideIntel) item;
            intel.completed = asCompleted;
            intel.endImmediately();
        }
    }

    private static SectorEntityToken getEventide() {
        MarketAPI market = Global.getSector().getEconomy()
                .getMarket(EVENTIDE_MARKET_ID);
        return market == null ? null : market.getPrimaryEntity();
    }

    private static void markEventide(boolean important) {
        SectorEntityToken eventide = getEventide();
        if (eventide == null) return;
        if (important) {
            Misc.makeImportant(eventide, EVENTIDE_MARKER);
        } else {
            Misc.makeUnimportant(eventide, EVENTIDE_MARKER);
        }
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        info.addPara(getName(), getTitleColor(mode), 0f);
        String status = completed
                ? "Expedition briefing received"
                : "Meet Sinni at Eventide";
        info.addPara(status, 3f,
                completed ? Misc.getPositiveHighlightColor()
                        : Misc.getHighlightColor(),
                status);
    }

    @Override
    public void createSmallDescription(
            TooltipMakerAPI info, float width, float height) {
        info.addImage(getIcon(), width, 128f, 10f);
        if (completed) {
            info.addPara("Sinni has explained the Hegemony expedition and "
                    + "placed Waypoint Troy on the navigation plot.", 10f);
        } else {
            info.addPara("Sinni has invited you to take tea at her office on "
                    + "Eventide, where she can discuss the proposed "
                    + "expedition in private.", 10f);
            info.addPara("Objective: Visit Sinni's office on Eventide", 10f,
                    Misc.getTextColor(), Misc.getHighlightColor(),
                    "Visit Sinni's office on Eventide");
        }
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        return getEventide();
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
        return Global.getSector().getFaction(Factions.INDEPENDENT).getCrest();
    }
}
