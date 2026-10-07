package chiefnavigator.quest;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;

/** Custom conversations retain the selected option before advancing by default. */
abstract class ChoicePreservingDialogPlugin implements InteractionDialogPlugin {
    private InteractionDialogAPI choiceDialog;

    @Override
    public final void init(InteractionDialogAPI dialog) {
        choiceDialog = dialog;
        initConversation(dialog);
    }

    @Override
    public final void optionSelected(String optionText, Object optionData) {
        RuleDialogSupport.preserveChoice(choiceDialog, optionText, optionData);
        handleOptionSelected(optionText, optionData);
    }

    protected abstract void initConversation(InteractionDialogAPI dialog);

    protected abstract void handleOptionSelected(String optionText, Object optionData);
}
