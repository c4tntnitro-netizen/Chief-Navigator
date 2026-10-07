package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import java.util.Map;

/** Rules-backed shell for the one-time Spartan first-contact scene. */
public final class FobIthacaApproachInteraction
        extends ChoicePreservingDialogPlugin {
    private static final String RULE_PREFIX =
            "ChiefNavigatorFobIthacaApproach_";
    private static final String PLAYER_TITLE =
            "$chiefNavigatorFobIthacaPlayerTitle";
    private static final String COMPLETE_OPTION = "complete";
    private static final String OPEN_CHANNEL_OPTION = "open_channel";

    private InteractionDialogAPI dialog;
    private Map<String, MemoryAPI> memoryMap;
    private PersonAPI officer;

    @Override
    protected void initConversation(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        this.memoryMap = RuleDialogSupport.createMemoryMap(dialog);
        MemoryAPI local = Global.getFactory().createMemory();
        memoryMap.put(MemKeys.LOCAL, local);
        local.set(PLAYER_TITLE, "Captain");

        dialog.setPromptText("");
        dialog.setBackgroundDimAmount(0.45f);
        officer = createSpartanOfficer();
        showOpeningPortraits();
        if (!RuleDialogSupport.fire(
                dialog, memoryMap, RULE_PREFIX + "opening")) {
            Global.getLogger(FobIthacaApproachInteraction.class).warn(
                    "Missing FOB Ithaca approach opening rule");
            dialog.dismiss();
        }
    }

    @Override
    protected void handleOptionSelected(String optionText, Object optionData) {
        if (optionData == null) return;
        String optionId = optionData.toString();
        if (OPEN_CHANNEL_OPTION.equals(optionId) && officer != null) {
            dialog.getVisualPanel().hideSecondPerson();
            dialog.getVisualPanel().hideThirdPerson();
            // The standard person panel includes his faction's banner.
            dialog.getVisualPanel().showPersonInfo(officer, false, false);
            PersonAPI sinni = SinniContact.getOrCreatePerson();
            PersonAPI player = Global.getSector().getPlayerPerson();
            if (sinni != null) dialog.getVisualPanel().showSecondPerson(sinni);
            if (player != null) dialog.getVisualPanel().showThirdPerson(player);
        }
        if (COMPLETE_OPTION.equals(optionId)) {
            FobIthacaApproachScript.markComplete();
        }
        if (!RuleDialogSupport.fire(
                dialog, memoryMap, RULE_PREFIX + optionId)) {
            Global.getLogger(FobIthacaApproachInteraction.class).warn(
                    "Missing FOB Ithaca approach stage " + optionId);
            dialog.dismiss();
        }
    }

    @Override public void optionMousedOver(String text, Object data) { }
    @Override public void advance(float amount) { }
    @Override public void backFromEngagement(EngagementResultAPI result) { }
    @Override public Object getContext() { return null; }
    @Override public Map<String, MemoryAPI> getMemoryMap() { return memoryMap; }

    /** Shows the bridge conversation until the Spartan channel opens. */
    private void showOpeningPortraits() {
        if (Global.getSector() == null) return;
        PersonAPI player = Global.getSector().getPlayerPerson();
        PersonAPI sinni = SinniContact.getOrCreatePerson();
        if (player != null) {
            dialog.getVisualPanel().showPersonInfo(player, true);
            if (sinni != null) {
                dialog.getVisualPanel().showSecondPerson(sinni);
            }
        } else if (sinni != null) {
            dialog.getVisualPanel().showPersonInfo(sinni, true);
        }
    }

    private static PersonAPI createSpartanOfficer() {
        return FobIthacaContacts.getOrCreateSpartanCaptain();
    }
}
