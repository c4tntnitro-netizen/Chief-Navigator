package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import java.util.List;
import java.util.Set;

/** Marked sightseeing quest for Sinni's personal stellar chart. */
public final class SinniOneMoreHorizonIntel extends BaseIntelPlugin {
    private boolean complete;

    public static SinniOneMoreHorizonIntel ensureExists(TextPanelAPI text) {
        if (Global.getSector() == null
                || !Global.getSector().getMemoryWithoutUpdate().getBoolean(
                        SinniBarEvent.STARTED)
                || SinniSystemVignetteScript.isOneMoreHorizonComplete()) {
            return null;
        }
        for (IntelInfoPlugin item : Global.getSector().getIntelManager()
                .getIntel(SinniOneMoreHorizonIntel.class)) {
            if (item instanceof SinniOneMoreHorizonIntel
                    && !item.isEnded() && !item.isEnding()) {
                return (SinniOneMoreHorizonIntel) item;
            }
        }
        SinniOneMoreHorizonIntel intel = new SinniOneMoreHorizonIntel();
        intel.setImportant(Boolean.TRUE);
        Global.getSector().getIntelManager().addIntel(intel, false, text);
        return intel;
    }

    /** Repairs the marked quest for recruited saves from earlier builds. */
    public static void reconcile() {
        if (Global.getSector() == null) return;
        if (SinniSystemVignetteScript.isOneMoreHorizonComplete()) {
            complete();
        } else {
            ensureExists(null);
        }
    }

    static void complete() {
        if (Global.getSector() == null) return;
        for (IntelInfoPlugin item : Global.getSector().getIntelManager()
                .getIntel(SinniOneMoreHorizonIntel.class)) {
            if (!(item instanceof SinniOneMoreHorizonIntel)
                    || item.isEnded() || item.isEnding()) continue;
            SinniOneMoreHorizonIntel intel =
                    (SinniOneMoreHorizonIntel) item;
            intel.complete = true;
            intel.sendUpdateIfPlayerHasIntel("complete", false);
            intel.endAfterDelay();
        }
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        info.addPara(getName(), getTitleColor(mode), 0f);
        info.addPara(status(), 3f,
                complete ? Misc.getPositiveHighlightColor()
                        : Misc.getHighlightColor(),
                status());
    }

    @Override
    public void createSmallDescription(
            TooltipMakerAPI info, float width, float height) {
        info.addImage(
                SinniContact.getPortraitSprite(),
                width, 128f, 10f);
        SinniSystemVignetteScript.LastRequestTarget target =
                SinniSystemVignetteScript.getActiveLastRequestTarget();
        if (complete
                || SinniSystemVignetteScript.isOneMoreHorizonComplete()) {
            info.addPara("Sinni has completed her chart of every stellar "
                    + "system category present in this Sector.", 10f);
            return;
        }
        int completed = SinniSystemVignetteScript
                .getCompletedSightseeingCount();
        int required = SinniSystemVignetteScript
                .getRequiredSightseeingCount();
        info.addPara("Help Sinni complete her personal chart by visiting "
                        + "each stellar-system category available in this "
                        + "Sector. Progress: %s.",
                10f,
                Misc.getTextColor(),
                Misc.getHighlightColor(),
                completed + " / " + required);
        info.addPara("Reward: %s and %s.",
                10f,
                Misc.getTextColor(),
                Misc.getHighlightColor(),
                "+2 Burn speed cap",
                "+1 base Burn level");
        List<SinniSystemVignetteScript.LastRequestTarget> destinations =
                SinniSystemVignetteScript
                        .getRemainingSightseeingDestinations();
        if (!destinations.isEmpty()) {
            info.addPara("Remaining systems:", 10f);
            for (SinniSystemVignetteScript.LastRequestTarget destination
                    : destinations) {
                String type = SinniSystemVignetteScript.categoryDisplayName(
                        destination.category);
                String system = SinniSystemVignetteScript.displayName(
                        destination.system);
                info.addPara(type + ": %s",
                        3f,
                        Misc.getTextColor(),
                        Misc.getHighlightColor(),
                        system);
            }
        }
        if (target != null
                && !SinniSystemVignetteScript.isComplete(target.category)) {
            String type = SinniSystemVignetteScript.categoryDisplayName(
                    target.category);
            info.addPara("Sinni has asked to visit a %s in %s: the final "
                            + "unseen category on her personal chart.",
                    10f,
                    Misc.getTextColor(),
                    Misc.getHighlightColor(),
                    type,
                    SinniSystemVignetteScript.displayName(target.system));
        }
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        SinniSystemVignetteScript.LastRequestTarget target =
                SinniSystemVignetteScript.getActiveLastRequestTarget();
        return target == null ? null
                : SinniSystemVignetteScript.objectiveToken(target.system);
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(Tags.INTEL_MISSIONS);
        tags.add(Tags.INTEL_ACCEPTED);
        return tags;
    }

    @Override
    public String getName() {
        return complete ? "One More Horizon - Complete" : "One More Horizon";
    }

    @Override
    public String getIcon() {
        return Global.getSector().getFaction(Factions.INDEPENDENT).getCrest();
    }

    private String status() {
        if (complete || SinniSystemVignetteScript.isOneMoreHorizonComplete()) {
            return "Chart completed";
        }
        SinniSystemVignetteScript.LastRequestTarget target =
                SinniSystemVignetteScript.getActiveLastRequestTarget();
        if (target != null) return "Visit " + target.system.getName();
        if (SinniSystemVignetteScript.isAllTypesCompletionPending()) {
            return "Review the completed chart with Sinni";
        }
        return "Sightseeing: "
                + SinniSystemVignetteScript.getCompletedSightseeingCount()
                + " / "
                + SinniSystemVignetteScript.getRequiredSightseeingCount();
    }
}
