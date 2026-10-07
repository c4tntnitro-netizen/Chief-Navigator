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
import java.util.Set;

/** Optional Doom recovery objective offered alongside the sensor Labor. */
public final class MenelausDoomIntel extends BaseIntelPlugin {
    private boolean recovered;

    public static MenelausDoomIntel ensureExists(TextPanelAPI text) {
        if (Global.getSector() == null
                || !MenelausSensorLabor.isDoomRevealed()
                || MenelausSensorLabor.isDoomRecovered()) {
            return null;
        }
        for (IntelInfoPlugin item : Global.getSector().getIntelManager()
                .getIntel(MenelausDoomIntel.class)) {
            if (item instanceof MenelausDoomIntel
                    && !item.isEnded() && !item.isEnding()) {
                return (MenelausDoomIntel) item;
            }
        }
        MenelausDoomIntel intel = new MenelausDoomIntel();
        Global.getSector().getIntelManager().addIntel(intel, false, text);
        return intel;
    }

    public static void sync() {
        if (Global.getSector() == null) return;
        if (MenelausSensorLabor.isDoomRevealed()
                && !MenelausSensorLabor.isDoomRecovered()) {
            ensureExists(null);
            return;
        }
        if (!MenelausSensorLabor.isDoomRecovered()) return;
        for (IntelInfoPlugin item : Global.getSector().getIntelManager()
                .getIntel(MenelausDoomIntel.class)) {
            if (!(item instanceof MenelausDoomIntel)
                    || item.isEnded() || item.isEnding()) continue;
            MenelausDoomIntel intel = (MenelausDoomIntel) item;
            intel.recovered = true;
            intel.sendUpdateIfPlayerHasIntel("recovered", false);
            intel.endAfterDelay();
        }
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        info.addPara(getName(), getTitleColor(mode), 0f);
        info.addPara(recovered ? "Doom recovered" : "Doom recovery coordinates",
                3f,
                recovered ? Misc.getPositiveHighlightColor()
                        : Misc.getHighlightColor(),
                recovered ? "Doom recovered" : "Doom");
    }

    @Override
    public void createSmallDescription(
            TooltipMakerAPI info, float width, float height) {
        info.addImage(getIcon(), width, 128f, 10f);
        info.addPara("Menelaus has supplied the coordinates of the TTS "
                + "Penumbra, a salvageable Doom-class phase cruiser adrift "
                + "in Sanzu. Recovering it is optional, but its phase cloak "
                + "may make the Devoured Reach sensor insertion easier.", 10f);
        if (recovered || isEnding() || isEnded()) {
            info.addPara("The TTS Penumbra has been recovered.", 10f,
                    Misc.getPositiveHighlightColor(), "recovered");
        }
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        return MenelausSensorLabor.getDoomWreck();
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
        return recovered
                ? "Optional: A Shadow in Sanzu - Complete"
                : "Optional: A Shadow in Sanzu";
    }

    @Override
    public String getIcon() {
        return Global.getSector().getFaction(Factions.TRITACHYON).getCrest();
    }
}
