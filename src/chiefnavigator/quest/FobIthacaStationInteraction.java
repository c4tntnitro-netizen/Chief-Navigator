package chiefnavigator.quest;

import com.fs.starfarer.api.campaign.CoreInteractionListener;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.RuleBasedDialog;
import com.fs.starfarer.api.campaign.events.CampaignEventPlugin;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.util.Misc;
import java.util.Map;
import org.lwjgl.input.Keyboard;

/** Marketless, colony-like root interaction for FOB Ithaca. */
public final class FobIthacaStationInteraction
        extends ChoicePreservingDialogPlugin implements RuleBasedDialog {
    private static final String TRIGGER =
            "ChiefNavigatorFobIthacaStation";
    private static final String DIRECTORY =
            "chief_navigator_fob_ithaca_directory";
    private static final String FLEET =
            "chief_navigator_fob_ithaca_fleet";
    private static final String STORAGE =
            "chief_navigator_fob_ithaca_storage";
    private static final String REFIT =
            "chief_navigator_fob_ithaca_refit";
    private static final String LEAVE =
            "chief_navigator_fob_ithaca_station_leave";
    private static final String DIRECTORY_BACK =
            "chief_navigator_ithaca_directory_back";
    private static final String MENELAUS_LEAVE =
            "chief_navigator_fob_ithaca_leave";

    private InteractionDialogAPI dialog;
    private Map<String, MemoryAPI> memoryMap;
    private PersonAPI activeContact;
    private final MenelausDialogueReadState readState = new MenelausDialogueReadState();

    @Override
    protected void initConversation(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        this.memoryMap = RuleDialogSupport.createMemoryMap(dialog);
        this.activeContact = null;
        if (dialog.getInteractionTarget() != null) {
            dialog.getInteractionTarget().setActivePerson(null);
        }
        FobIthacaContacts.ensureForStation(dialog.getInteractionTarget());
        showStation();
    }

    void showStation() {
        showStationVisual();
        if (!RuleDialogSupport.fire(dialog, memoryMap, TRIGGER)) {
            dialog.dismiss();
            return;
        }
        dialog.getOptionPanel().setShortcut(
                FLEET, Keyboard.KEY_F, false, false, false, false);
        dialog.getOptionPanel().setShortcut(
                STORAGE, Keyboard.KEY_I, false, false, false, false);
        dialog.getOptionPanel().setShortcut(
                REFIT, Keyboard.KEY_R, false, false, false, false);
        dialog.getOptionPanel().setShortcut(
                LEAVE, Keyboard.KEY_ESCAPE, false, false, false, false);
    }

    private void showStationVisual() {
        if (dialog.getInteractionTarget() != null
                && dialog.getInteractionTarget()
                        .getCustomInteractionDialogImageVisual() != null) {
            dialog.getVisualPanel().showImageVisual(
                    dialog.getInteractionTarget()
                            .getCustomInteractionDialogImageVisual());
        }
    }

    @Override
    protected void handleOptionSelected(String optionText, Object optionData) {
        // Native EndConversation can clear the person before rebuilding their
        // options. A directory exit must remain usable even in that stale hub.
        if (DIRECTORY_BACK.equals(optionData)
                || MENELAUS_LEAVE.equals(optionData)) {
            returnToDirectory(dialog);
        } else if (activeContact != null) {
            handleContactOption(optionData);
        } else if (DIRECTORY.equals(optionData)) {
            openDirectory();
        } else if (FLEET.equals(optionData)) {
            openCore(CoreUITabId.FLEET);
        } else if (STORAGE.equals(optionData)) {
            openCore(CoreUITabId.CARGO);
        } else if (REFIT.equals(optionData)) {
            openCore(CoreUITabId.REFIT);
        } else if (LEAVE.equals(optionData)) {
            dialog.dismiss();
        }
    }

    private void openDirectory() {
        FobIthacaCommDirectoryDialog.show(dialog);
    }

    private void handleContactOption(Object optionData) {
        if (optionData == null) return;
        MemoryAPI local = memoryMap.get(MemKeys.LOCAL);
        if (local == null) {
            returnToDirectory(dialog);
            return;
        }
        local.set("$option", optionData.toString(), 0f);
        if (!RuleDialogSupport.fire(
                dialog, memoryMap, "DialogOptionSelected")) {
            returnToDirectory(dialog);
            return;
        }
        readState.afterPage(dialog, memoryMap.get(MemKeys.GLOBAL), optionData);
    }

    private void openCore(CoreUITabId tab) {
        dialog.getOptionPanel().clearOptions();
        FobIthacaStorageDialog.show(
                dialog, tab,
                new CoreInteractionListener() {
                    @Override
                    public void coreUIDismissed() {
                        updateMemory();
                        showStation();
                    }
                });
        Misc.stopPlayerFleet();
    }

    public static void returnToStation(InteractionDialogAPI dialog) {
        FobIthacaStationInteraction plugin =
                new FobIthacaStationInteraction();
        dialog.setPlugin(plugin);
        plugin.init(dialog);
    }

    public static void returnToDirectory(InteractionDialogAPI dialog) {
        FobIthacaStationInteraction plugin =
                new FobIthacaStationInteraction();
        dialog.setPlugin(plugin);
        plugin.init(dialog);
        plugin.openDirectory();
    }

    /** Ends an owned contact without native EndConversation's stale option map. */
    public static boolean endContact(InteractionDialogAPI dialog) {
        if (dialog == null
                || !OdysseyExpanseSystem.isFobIthaca(dialog.getInteractionTarget())
                || !(dialog.getPlugin() instanceof FobIthacaStationInteraction
                    || dialog.getPlugin() instanceof FobIthacaContactInteraction)) {
            return false;
        }
        returnToDirectory(dialog);
        return true;
    }

    @Override
    public void notifyActivePersonChanged() {
        PersonAPI person = dialog == null
                || dialog.getInteractionTarget() == null
                ? null : dialog.getInteractionTarget().getActivePerson();
        if (!FobIthacaContacts.isOwnedIthacaContact(person)) {
            activeContact = null;
            updateMemory();
            return;
        }
        activeContact = person;
        updateMemory();
        dialog.getVisualPanel().showPersonInfo(person, true);
    }

    @Override
    public void updateMemory() {
        if (dialog == null) return;
        memoryMap = RuleDialogSupport.createMemoryMap(dialog);
        if (activeContact != null) {
            memoryMap.put(
                    MemKeys.LOCAL,
                    activeContact.getMemoryWithoutUpdate());
        }
    }

    @Override
    public void setActiveMission(CampaignEventPlugin mission) { }

    @Override
    public void reinit(boolean withContinue) {
        activeContact = null;
        updateMemory();
        showStation();
    }

    @Override public void optionMousedOver(
            String optionText, Object optionData) { }
    @Override public void advance(float amount) { }
    @Override public void backFromEngagement(
            EngagementResultAPI battleResult) { }
    @Override public Object getContext() { return activeContact; }
    @Override public Map<String, MemoryAPI> getMemoryMap() {
        return memoryMap;
    }
}
