package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Two independent post-Labor hunts; the existing encounters own their battles. */
public final class PostLaborHuntIntel extends BaseIntelPlugin {
    public enum Target {
        BUDAI("Budai", "Avici"), UNGAIKYO("Ungaikyo", "Last Light");
        final String name;
        final String systemName;

        Target(String name, String systemName) {
            this.name = name;
            this.systemName = systemName;
        }

        String startedKey() {
            return "$chief_navigator_post_labor_hunt_" + name.toLowerCase(java.util.Locale.ROOT)
                    + "_started";
        }

        boolean defeated() {
            return this == BUDAI ? OdysseyPredatorScript.isBudaiDefeated()
                    : OdysseyPredatorScript.isUngaikyoRematchDefeated();
        }
    }

    private Target target;
    private boolean briefed;
    private boolean completed;

    public PostLaborHuntIntel() { }

    private PostLaborHuntIntel(Target target) {
        this.target = target;
    }

    /** Called after the committed debrief and by the existing objective watcher. */
    public static void sync(TextPanelAPI text) {
        if (Global.getSector() == null || !MenelausTrial.isComplete()) return;
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        for (Target target : Target.values()) {
            PostLaborHuntIntel active = findActive(target);
            if (target.defeated()) {
                if (active != null && !active.completed) {
                    active.completed = true;
                    active.sendUpdateIfPlayerHasIntel("completed", false);
                    active.endAfterDelay();
                }
                continue;
            }
            if (active != null || memory.getBoolean(target.startedKey())) continue;
            PostLaborHuntIntel intel = new PostLaborHuntIntel(target);
            intel.setImportant(Boolean.TRUE);
            Global.getSector().getIntelManager().addIntel(intel, false, text);
            memory.set(target.startedKey(), true);
        }
    }

    public static boolean isAvailable(Target target) {
        return Global.getSector() != null && target != null
                && MenelausTrial.isComplete() && !target.defeated();
    }

    public static boolean brief(Target target, TextPanelAPI text) {
        if (!isAvailable(target)) return false;
        sync(text);
        PostLaborHuntIntel intel = findActive(target);
        if (intel == null) return false;
        if (!intel.briefed) {
            intel.briefed = true;
            intel.sendUpdateIfPlayerHasIntel("location", text);
        }
        return true;
    }

    public static boolean isKleon(PersonAPI person) {
        return person != null
                && FobIthacaContacts.SPARTAN_CAPTAIN_PERSON_ID.equals(person.getId())
                && person.getMemoryWithoutUpdate().getBoolean(
                        FobIthacaContacts.SPARTAN_CAPTAIN_MARKER);
    }

    private static PostLaborHuntIntel findActive(Target target) {
        for (IntelInfoPlugin item : new ArrayList<IntelInfoPlugin>(
                Global.getSector().getIntelManager().getIntel(PostLaborHuntIntel.class))) {
            if (item instanceof PostLaborHuntIntel && !item.isEnded() && !item.isEnding()) {
                PostLaborHuntIntel intel = (PostLaborHuntIntel) item;
                if (intel.target == target) return intel;
            }
        }
        return null;
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        info.addPara(getName(), getTitleColor(mode), 0f);
        info.addPara(status(), 3f, completed ? Misc.getPositiveHighlightColor()
                : Misc.getHighlightColor(), status());
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        if (completed || target.defeated()) {
            info.addPara(target.name + " has been destroyed. Hunt complete.", 10f);
        } else if (!briefed) {
            info.addPara("Speak to %s in %s's comm directory to learn where to find %s, "
                    + "then destroy the target.", 10f, Misc.getTextColor(),
                    Misc.getHighlightColor(), "Captain Aias Kleon", "FOB Ithaca", target.name);
        } else {
            info.addPara("Destroy %s in %s.", 10f, Misc.getTextColor(),
                    Misc.getHighlightColor(), target.name, target.systemName);
            info.addPara(target == Target.BUDAI
                    ? "Budai hunts alone in Avici, returning toward Kshaya. "
                        + "His distortion will pursue your fleet while you remain in the system."
                    : "Ungaikyo's local Fabricator has knotted the filament web around "
                        + "the derelict Guardian in Last Light. Destroy it to free the wreck.", 10f);
        }
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        if (completed || target.defeated()) return null;
        if (!briefed) return OdysseyExpanseSystem.getFobIthaca();
        StarSystemAPI system = OdysseyExpanseSystem.findSystemById(
                target == Target.BUDAI ? OdysseyExpanseSystem.MESSINA_ID
                        : OdysseyExpanseSystem.LAST_LIGHT_ID);
        if (system == null) return null;
        SectorEntityToken wreck = target == Target.UNGAIKYO
                ? system.getEntityById(OdysseyExpanseSystem.LAST_LIGHT_GUARDIAN_WRECK_ID) : null;
        return wreck == null ? system.getCenter() : wreck;
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
        return "Hunt " + target.name + (completed ? " - Complete" : "");
    }

    @Override
    public String getIcon() {
        return Global.getSettings().getSpriteName("characters", "chief_navigator_spartan_officer");
    }

    private String status() {
        if (completed || target.defeated()) return "Target destroyed";
        return briefed ? "Destroy " + target.name + " in " + target.systemName
                : "Speak to Captain Aias Kleon at FOB Ithaca";
    }
}
