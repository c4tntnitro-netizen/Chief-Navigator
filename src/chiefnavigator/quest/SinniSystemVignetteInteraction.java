package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import java.util.Locale;
import java.util.Map;

/** Rules-backed interaction shell for Sinni's stellar-system vignettes. */
public final class SinniSystemVignetteInteraction
        extends ChoicePreservingDialogPlugin {
    private static final String RULE_PREFIX = "ChiefNavigatorSinniVignette_";
    private static final String SYSTEM_NAME = "$chiefNavigatorSinniSystemName";
    private static final String SYSTEM_SUBTYPE =
            "$chiefNavigatorSinniSystemSubtype";
    private static final String PLAYER_GENDER =
            "$chiefNavigatorSinniPlayerGender";
    private static final String PLAYER_TITLE =
            "$chiefNavigatorSinniPlayerTitle";
    private static final String SAW_AVICI =
            "$chiefNavigatorSinniSawAvici";
    private static final String MISSING_TYPE =
            "$chiefNavigatorSinniMissingType";
    private static final String MISSING_SYSTEM =
            "$chiefNavigatorSinniMissingSystem";
    private static final String MISSING_DISTANCE =
            "$chiefNavigatorSinniMissingDistance";
    private static final String DEVOURED_RING_PATH_KNOWN =
            "$chiefNavigatorSinniDevouredRingPathKnown";

    private final SinniSystemVignetteScript.Candidate candidate;
    private final String systemName;
    private InteractionDialogAPI dialog;
    private Map<String, MemoryAPI> memoryMap;

    public SinniSystemVignetteInteraction(
            SinniSystemVignetteScript.Candidate candidate,
            String systemName) {
        this.candidate = candidate;
        this.systemName = systemName;
    }

    @Override
    protected void initConversation(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        this.memoryMap = RuleDialogSupport.createMemoryMap(dialog);
        MemoryAPI local = Global.getFactory().createMemory();
        memoryMap.put(MemKeys.LOCAL, local);
        local.set(SYSTEM_NAME, systemName);
        local.set(SYSTEM_SUBTYPE, candidate.subtype);
        local.set(PLAYER_GENDER,
                Global.getSector().getPlayerPerson().getName().getGender()
                        == FullName.Gender.FEMALE ? "female" : "male");
        local.set(PLAYER_TITLE, "Captain");
        local.set(SAW_AVICI, hasSeenAvici());
        local.set(DEVOURED_RING_PATH_KNOWN,
                SinniSystemVignetteScript.isDevouredRingPathKnown());
        if (candidate.lastRequestTarget != null) {
            local.set(MISSING_TYPE,
                    SinniSystemVignetteScript.categoryDisplayName(
                            candidate.lastRequestTarget.category));
            local.set(MISSING_SYSTEM,
                    candidate.lastRequestTarget.system.getName());
            local.set(MISSING_DISTANCE, String.format(
                    Locale.US,
                    "%.1f",
                    candidate.lastRequestTarget.distanceLy));
        }

        if (candidate.category
                == SinniSystemVignetteScript.Category.ALPHA_ODYSSEY) {
            dialog.showVisualPanel();
            dialog.setPromptText("");
            dialog.setBackgroundDimAmount(1f);
        } else {
            dialog.setBackgroundDimAmount(0.45f);
            com.fs.starfarer.api.characters.PersonAPI sinni =
                    SinniContact.getOrCreatePerson();
            if (sinni != null) {
                dialog.getVisualPanel().showPersonInfo(sinni, true);
            }
            if (candidate.category
                    == SinniSystemVignetteScript.Category.TRINARY) {
                dialog.getVisualPanel().showSecondPerson(createEnya());
            }
        }
        if (!RuleDialogSupport.fire(
                dialog,
                memoryMap,
                RULE_PREFIX + "sinni_" + candidate.sceneId)) {
            Global.getLogger(SinniSystemVignetteInteraction.class).warn(
                    "Missing Sinni vignette root for " + candidate.category.id);
            dialog.dismiss();
        }
    }

    @Override
    protected void handleOptionSelected(String optionText, Object optionData) {
        if (optionData == null) return;
        String optionId = optionData.toString();
        if (("complete_" + candidate.category.id).equals(optionId)) {
            SinniSystemVignetteScript.markComplete(candidate.category);
        } else if ("accept_last_request".equals(optionId)) {
            SinniSystemVignetteScript.acceptLastRequest(
                    candidate.lastRequestTarget,
                    dialog.getTextPanel());
        } else if ("defer_last_request".equals(optionId)) {
            SinniSystemVignetteScript.deferLastRequest(
                    currentSystem());
        } else if ("complete_one_more_horizon".equals(optionId)) {
            SinniSystemVignetteScript.completeOneMoreHorizon();
        }
        if (!RuleDialogSupport.fire(
                dialog, memoryMap, RULE_PREFIX + optionId)) {
            Global.getLogger(SinniSystemVignetteInteraction.class).warn(
                    "Missing Sinni vignette stage " + optionId);
            dialog.dismiss();
        }
    }

    @Override public void optionMousedOver(String text, Object data) { }
    @Override public void advance(float amount) { }
    @Override public void backFromEngagement(EngagementResultAPI result) { }
    @Override public Object getContext() { return null; }
    @Override public Map<String, MemoryAPI> getMemoryMap() { return memoryMap; }

    /** Visiting Avici is enough, even if pursuit deferred its arrival scene. */
    static boolean hasSeenAvici() {
        if (Global.getSector() == null) return false;
        if (SinniSystemVignetteScript.isComplete(
                SinniSystemVignetteScript.Category.AVICI)) return true;
        StarSystemAPI avici = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.MESSINA_ID);
        return avici != null && avici.isEnteredByPlayer();
    }

    private static PersonAPI createEnya() {
        PersonAPI enya = Global.getFactory().createPerson();
        enya.setId("chief_navigator_enya");
        enya.setName(new FullName(
                "Enya", "", FullName.Gender.FEMALE));
        enya.setGender(FullName.Gender.FEMALE);
        enya.setFaction(Factions.PLAYER);
        enya.setRankId(Ranks.SPACE_LIEUTENANT);
        enya.setPostId(Ranks.POST_OFFICER);
        enya.setImportance(PersonImportance.MEDIUM);
        enya.setVoice("soldier");
        enya.setPortraitSprite("graphics/portraits/portrait32.png");
        return enya;
    }

    private static StarSystemAPI currentSystem() {
        if (Global.getSector() == null
                || Global.getSector().getPlayerFleet() == null
                || !(Global.getSector().getPlayerFleet()
                        .getContainingLocation()
                        instanceof StarSystemAPI)) {
            return null;
        }
        return (StarSystemAPI) Global.getSector().getPlayerFleet()
                .getContainingLocation();
    }
}
