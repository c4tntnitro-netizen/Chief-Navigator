package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import java.util.ArrayList;
import java.util.Set;

/** One Intel entry for one stage of Menelaus's sequential Labor chain. */
public final class MenelausTrialIntel extends BaseIntelPlugin {
    // Serialized by the former all-in-one Intel entry. Keep these fields until
    // old saves have loaded and the stage-0 entry can retire itself.
    @SuppressWarnings("unused") private int lastMotherships = -1;
    @SuppressWarnings("unused") private int lastResearch = -1;
    @SuppressWarnings("unused") private int lastGautama = -1;
    @SuppressWarnings("unused") private int lastWall = -1;
    @SuppressWarnings("unused") private boolean lastFinalRevealed;

    private int laborStage;
    private int lastProgress = -1;
    private boolean progressInitialized;
    private boolean completed;

    /** Required for save deserialization; legacy checklist entries use stage 0. */
    public MenelausTrialIntel() { }

    private MenelausTrialIntel(int laborStage) {
        this.laborStage = laborStage;
        captureProgress();
    }

    public static MenelausTrialIntel ensureExists(TextPanelAPI text) {
        if (Global.getSector() == null || !MenelausTrial.isAccepted()) {
            return null;
        }
        int stage = MenelausTrial.getCurrentLaborStage();
        if (!isQuestStage(stage)) return null;
        if (stage == MenelausTrial.LABOR_GAUTAMA
                && !MenelausTrial.isFinalLaborBriefed()) {
            return null;
        }
        for (IntelInfoPlugin item : Global.getSector().getIntelManager()
                .getIntel(MenelausTrialIntel.class)) {
            if (item instanceof MenelausTrialIntel) {
                MenelausTrialIntel intel = (MenelausTrialIntel) item;
                if (!intel.isEnded() && !intel.isEnding()
                        && intel.laborStage == stage) {
                    return intel;
                }
            }
        }
        MenelausTrialIntel intel = new MenelausTrialIntel(stage);
        Global.getSector().getIntelManager().addIntel(intel, false, text);
        return intel;
    }

    /** Retires a completed quest, creates its successor, and posts progress. */
    public static void syncProgress(boolean announce) {
        if (Global.getSector() == null || !MenelausTrial.isAccepted()) return;
        int stage = MenelausTrial.getCurrentLaborStage();
        MenelausTrialIntel current = null;
        for (IntelInfoPlugin item : new ArrayList<IntelInfoPlugin>(
                Global.getSector().getIntelManager()
                        .getIntel(MenelausTrialIntel.class))) {
            if (!(item instanceof MenelausTrialIntel)) continue;
            MenelausTrialIntel intel = (MenelausTrialIntel) item;
            if (intel.isEnded() || intel.isEnding()) continue;
            if (intel.laborStage <= MenelausTrial.LABOR_NONE) {
                // Replace the old all-in-one checklist without leaving a
                // duplicate active quest in migrated saves.
                intel.endImmediately();
            } else if (intel.laborStage != stage) {
                if (isStageComplete(intel.laborStage)) {
                    intel.completed = true;
                    intel.lastProgress = getTarget(intel.laborStage);
                    intel.progressInitialized = true;
                    if (announce) {
                        intel.sendUpdateIfPlayerHasIntel("completed", false);
                    }
                    intel.endAfterDelay();
                } else {
                    // Retire stale entries when a save migrates to a newly
                    // reordered or inserted Labor without claiming success.
                    intel.endImmediately();
                }
            } else {
                current = intel;
            }
        }

        if (isQuestStage(stage) && current == null) {
            current = ensureExists(null);
        }
        if (current == null) return;

        int progress = getProgress(stage);
        boolean changed = current.progressInitialized
                && progress != current.lastProgress;
        current.lastProgress = progress;
        current.progressInitialized = true;
        if (announce && changed) {
            current.sendUpdateIfPlayerHasIntel("progress", false);
        }
    }

    private void captureProgress() {
        lastProgress = getProgress(laborStage);
        progressInitialized = true;
    }

    private static boolean isQuestStage(int stage) {
        return stage == MenelausTrial.LABOR_WALL
                || stage == MenelausTrial.LABOR_RESEARCH
                || stage == MenelausTrial.LABOR_MOTHERSHIPS
                || stage == MenelausTrial.LABOR_SENSOR
                || stage == MenelausTrial.LABOR_GAUTAMA;
    }

    private static int getProgress(int stage) {
        if (stage == MenelausTrial.LABOR_WALL) {
            return MenelausTrial.getWallObjectiveProgress();
        }
        if (stage == MenelausTrial.LABOR_RESEARCH) {
            return MenelausTrial.getResearchObjectiveProgress();
        }
        if (stage == MenelausTrial.LABOR_MOTHERSHIPS) {
            return MenelausTrial.getMothershipsDefeated();
        }
        if (stage == MenelausTrial.LABOR_SENSOR) {
            return MenelausSensorLabor.isComplete() ? 1 : 0;
        }
        if (stage == MenelausTrial.LABOR_GAUTAMA) {
            return MenelausTrial.getGautamaObjectiveProgress();
        }
        return 0;
    }

    private static int getTarget(int stage) {
        if (stage == MenelausTrial.LABOR_RESEARCH) {
            return MenelausTrial.getResearchObjectiveTarget();
        }
        if (stage == MenelausTrial.LABOR_MOTHERSHIPS) {
            return MenelausTrial.getMothershipObjectiveTotal();
        }
        if (stage == MenelausTrial.LABOR_GAUTAMA) {
            return MenelausTrial.getGautamaObjectiveTarget();
        }
        return 1;
    }

    private static boolean isStageComplete(int stage) {
        return MenelausTrial.isLaborReported(stage);
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        info.addPara(getName(), getTitleColor(mode), 0f);
        addProgressLine(info, 3f);
    }

    @Override
    public void createSmallDescription(
            TooltipMakerAPI info, float width, float height) {
        info.addImage(getIcon(), width, 128f, 10f);
        if (laborStage == MenelausTrial.LABOR_GAUTAMA
                && MenelausTrial.isFinalLaborFailed()) {
            info.addPara("Ithaca's gate defense has fallen. Starving Threat "
                    + "formations are now emerging from gates throughout the "
                    + "Sector, including the Core Worlds. Destroying an "
                    + "incursion only delays its replacement.",
                    10f, Misc.getNegativeHighlightColor(),
                    "Ithaca's gate defense has fallen", "Core Worlds");
        } else if (!completed
                && MenelausTrial.isLaborReportPending(laborStage)) {
            if (laborStage == MenelausTrial.LABOR_RESEARCH) {
                info.addPara("Four required components have been delivered "
                        + "and installed in Ithaca's defense wall. Report to "
                        + "Menelaus to complete the Labor. Any unclaimed "
                        + "research stations and their unique upgrades are "
                        + "optional and remain recoverable.",
                        10f, Misc.getHighlightColor(),
                        "Four required components", "Ithaca's defense wall",
                        "optional and remain recoverable");
            } else {
                info.addPara("The field objective is complete. Return to FOB "
                        + "Ithaca and report to Menelaus before he opens the "
                        + "next Labor. Ithaca is marked on the campaign map.",
                        10f, Misc.getHighlightColor(),
                        "Return to FOB Ithaca", "report to Menelaus");
            }
        } else if (laborStage == MenelausTrial.LABOR_WALL) {
            info.addPara("With the Guard Motherships restored and four "
                    + "defense-wall upgrades installed, Menelaus has opened "
                    + "the third Labor: disable the "
                    + "Drifting Wall's reactor so he can reclaim the station. "
                    + "Its coordinates are marked on the campaign map.", 10f);
        } else if (laborStage == MenelausTrial.LABOR_RESEARCH) {
            info.addPara("With the Guard Motherships reclaimed, Menelaus has "
                    + "opened the second Labor: recover components from any "
                    + "four of the Odyssey Sector's six derelict research "
                    + "stations, then deliver them to Menelaus at FOB Ithaca "
                    + "for installation in the defense wall. Every station "
                    + "contains a distinct upgrade; the other two are "
                    + "optional and remain recoverable.", 10f);
        } else if (laborStage == MenelausTrial.LABOR_MOTHERSHIPS) {
            info.addPara("Menelaus has opened the first Labor and uploaded the "
                    + "coordinates of four sealed Guard Motherships. Break each "
                    + "command partition and recover its guard flotillas. His "
                    + "Domain-Security IFF Transponder also authorizes command "
                    + "of recovered Domain derelict drones without Automated "
                    + "Ships expertise. After reporting the Labor, you can "
                    + "spend one story point to recruit a random drone from "
                    + "a friendly patrol.", 10f);
        } else if (laborStage == MenelausTrial.LABOR_SENSOR) {
            info.addPara("Menelaus has opened the fourth Labor: reach the Mara "
                    + "Observation Array in Devoured Reach without an aware "
                    + "hostile fleet nearby, then run his black-box sensor "
                    + "package. The package is already integrated into your "
                    + "fleet systems and occupies no cargo space.", 10f);
            info.addPara("A separate optional objective marks a recoverable "
                    + "Doom-class phase cruiser in Sanzu. Menelaus recommends "
                    + "it if your fleet lacks a stealth-capable ship.", 10f);
        } else if (laborStage == MenelausTrial.LABOR_GAUTAMA) {
            info.addPara("Menelaus has opened the fifth and final Labor. "
                    + "Repel the three massive Starving Threat invasions at "
                    + "Ithaca's perimeter bastions. Each surviving invasion "
                    + "will destroy its assigned bastion after roughly two "
                    + "months, then turn on Task Force Spartan. Spartan "
                    + "losses during the Labor are permanent.", 10f);
            info.addPara("The Heavenly Strike cannot arrive until all three "
                    + "perimeter fleets are destroyed. When it does, every "
                    + "surviving Spartan formation will jump directly to the "
                    + "center engagement; preserving them makes the final "
                    + "battle substantially easier.", 10f);
            info.addPara("Gautama is absent from the first three fleets and "
                    + "will not reconstruct after committing the Heavenly "
                    + "Strike. Break the center assault once it arrives.", 10f);
        }
        addProgressLine(info, 10f);

        if (laborStage == MenelausTrial.LABOR_RESEARCH) {
            String recovered = MenelausTrial.getResearchStationsResearched()
                    + " / " + MenelausTrial.getResearchStationTotal();
            info.addPara("Research components recovered: %s. The "
                    + "defense-upgrade ledger continues to track installed, "
                    + "recovered, and missing parts after the four required "
                    + "installations are complete.",
                    10f, Misc.getTextColor(), Misc.getHighlightColor(),
                    recovered);
            info.addPara("The six independent components provide: quadruple "
                    + "naval-turret traverse, 40%% naval-beam cooldowns, "
                    + "shield-respecting beam EMP arcs, six fixed 54-round "
                    + "Cloudswarm PD launchers, guided Atropos conversion for the "
                    + "base Hammer batteries, "
                    + "and 40%% missile refire/reload times plus 108-round "
                    + "Cloudswarm bursts. Components remain inert cargo until "
                    + "delivered to Menelaus, who consumes and installs them "
                    + "in FOB Ithaca's defense wall. His contact menu tracks "
                    + "installed, recovered, and missing parts.",
                    10f);
            info.addPara("If Isa Leicester has ever joined the officer "
                    + "roster, her fire-control package permanently gives "
                    + "Ithaca's naval beams +50%% shield damage and a single "
                    + "half-strength refraction to another enemy.", 10f);
        }
        if (completed || isEnding() || isEnded()) {
            info.addPara("Labor complete. Menelaus has opened the next Labor.",
                    10f, Misc.getPositiveHighlightColor(), "Labor complete");
        }
    }

    private void addProgressLine(TooltipMakerAPI info, float pad) {
        int target = getTarget(laborStage);
        int current = completed ? target : Math.min(target, getProgress(laborStage));
        String progress = current + " / " + target;
        String label;
        if (laborStage == MenelausTrial.LABOR_WALL) {
            label = "Drifting Wall reclaimed: %s";
        } else if (laborStage == MenelausTrial.LABOR_RESEARCH) {
            label = "Required Ithaca wall upgrades installed: %s";
        } else if (laborStage == MenelausTrial.LABOR_MOTHERSHIPS) {
            label = "Guard Motherships reclaimed: %s";
        } else if (laborStage == MenelausTrial.LABOR_SENSOR) {
            label = "Sensor package deployed: %s";
        } else {
            label = "Ithaca invasion waves repelled: %s";
        }
        info.addPara(label, pad, Misc.getTextColor(),
                completed ? Misc.getPositiveHighlightColor()
                        : Misc.getHighlightColor(),
                progress);
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        return MenelausTrial.getObjectiveLocationForStage(laborStage);
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(Tags.INTEL_MISSIONS);
        tags.add(Tags.INTEL_ACCEPTED);
        return tags;
    }

    @Override
    protected String getName() {
        String name;
        if (!completed && MenelausTrial.isLaborReportPending(laborStage)) {
            name = "Labor " + getLaborNumber(laborStage)
                    + ": Return to Menelaus";
        } else if (laborStage == MenelausTrial.LABOR_WALL) {
            name = "Labor III: The Drifting Wall";
        } else if (laborStage == MenelausTrial.LABOR_RESEARCH) {
            name = "Labor II: Salvaged Knowledge";
        } else if (laborStage == MenelausTrial.LABOR_MOTHERSHIPS) {
            name = "Labor I: The Guard Motherships";
        } else if (laborStage == MenelausTrial.LABOR_SENSOR) {
            name = "Labor IV: The Listening Post";
        } else if (laborStage == MenelausTrial.LABOR_GAUTAMA) {
            name = MenelausTrial.isFinalLaborFailed()
                    ? "Labor V: Ithaca Breached"
                    : MenelausTrial.isFinalDebriefPending()
                    ? "Labor V: Return to Menelaus"
                    : "Labor V: Heavenly Strike";
        } else {
            name = "The Labors of Menelaus";
        }
        return completed ? name + " - Complete" : name;
    }

    private static String getLaborNumber(int stage) {
        if (stage == MenelausTrial.LABOR_MOTHERSHIPS) return "I";
        if (stage == MenelausTrial.LABOR_RESEARCH) return "II";
        if (stage == MenelausTrial.LABOR_WALL) return "III";
        if (stage == MenelausTrial.LABOR_SENSOR) return "IV";
        if (stage == MenelausTrial.LABOR_GAUTAMA) return "V";
        return "";
    }

    @Override
    public String getIcon() {
        com.fs.starfarer.api.characters.PersonAPI menelaus =
                MenelausTrial.getOrCreateMenelaus();
        return menelaus == null
                ? Global.getSettings().getSpriteName(
                        "characters", "chief_navigator_menelaus")
                : menelaus.getPortraitSprite();
    }
}
