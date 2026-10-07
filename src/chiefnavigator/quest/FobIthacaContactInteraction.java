package chiefnavigator.quest;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import java.util.Map;

/** Rules-backed conversation opened from Ithaca's native directory. */
final class FobIthacaContactInteraction extends ChoicePreservingDialogPlugin {
    private static final String BACK =
            "chief_navigator_ithaca_directory_back";
    private static final String MENELAUS_LEAVE =
            "chief_navigator_fob_ithaca_leave";
    private final PersonAPI person;
    private InteractionDialogAPI dialog;
    private Map<String, MemoryAPI> memoryMap;
    private final MenelausDialogueReadState readState = new MenelausDialogueReadState();

    FobIthacaContactInteraction(PersonAPI person) {
        this.person = person;
    }

    @Override
    protected void initConversation(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        this.memoryMap = RuleDialogSupport.createMemoryMap(dialog);
        // PickGreeting and every subsequent branch must see the selected
        // contact's markers as local memory even though Ithaca remains the
        // interaction target.
        memoryMap.put(MemKeys.LOCAL, person.getMemoryWithoutUpdate());
        dialog.getVisualPanel().showPersonInfo(person, true);
        if (!RuleDialogSupport.fire(dialog, memoryMap, "PickGreeting")) {
            returnToDirectory();
            return;
        }
        readState.afterPage(dialog, memoryMap.get(MemKeys.GLOBAL), null);
    }

    @Override
    protected void handleOptionSelected(String optionText, Object optionData) {
        if (BACK.equals(optionData) || MENELAUS_LEAVE.equals(optionData)) {
            returnToDirectory();
            return;
        }
        if (optionData == null) return;
        memoryMap.get(MemKeys.LOCAL).set(
                "$option", optionData.toString(), 0f);
        if (!RuleDialogSupport.fire(
                dialog, memoryMap, "DialogOptionSelected")) {
            returnToDirectory();
            return;
        }
        readState.afterPage(dialog, memoryMap.get(MemKeys.GLOBAL), optionData);
    }

    private void returnToDirectory() {
        FobIthacaStationInteraction.returnToDirectory(dialog);
    }

    @Override public void optionMousedOver(
            String optionText, Object optionData) { }
    @Override public void advance(float amount) { }
    @Override public void backFromEngagement(
            EngagementResultAPI battleResult) { }
    @Override public Object getContext() { return person; }
    @Override public Map<String, MemoryAPI> getMemoryMap() {
        return memoryMap;
    }
}
