package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.campaign.rules.Option;
import com.fs.starfarer.api.campaign.rules.RuleAPI;
import com.fs.starfarer.api.campaign.rules.RulesAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEvent;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Sinni's rules-authored, any-market story bar event. */
public final class SinniBarEvent extends BaseBarEvent {
    public static final String EVENT_ID = "chief_navigator_sinni_intro";
    public static final String STARTED = "$chief_navigator_started";
    private static final String LEGACY_DECLINE_TIMEOUT =
            "$chief_navigator_sinni_declined_recently";

    private static final String RULE_PREFIX = "ChiefNavigatorSinniIntro_";
    private static final String PROMPT_TRIGGER = RULE_PREFIX + "prompt";
    private static final String INTRO_TRIGGER = RULE_PREFIX + "start";
    private static final String OPTION_PREFIX = "chief_navigator_sinni_";
    private static final String ACCEPT = OPTION_PREFIX + "accept";
    private static final String LEAVE = OPTION_PREFIX + "leave";
    private static final String PLAYER_FEMALE = "$chief_navigator_player_female";
    private static final String PLAYER_NAME = "$chiefNavigatorPlayerName";
    private static final String PLAYER_TITLE = "$chiefNavigatorPlayerTitle";
    private static final String OPTION_COLOR_SETTING =
            "chiefNavigatorSinniOptionColor";
    private static final Color DEFAULT_OPTION_COLOR =
            new Color(255, 96, 104);

    public static void ensureCreatorRegistered() {
        if (Global.getSector() == null) return;
        BarEventManager manager = BarEventManager.getInstance();
        if (manager == null) return;
        // Remove the retired CSV-created wrapper from saves made by the
        // previous implementation before installing the native creator.
        for (BarEventManager.GenericBarEventCreator creator
                : new ArrayList<BarEventManager.GenericBarEventCreator>(
                        manager.getCreators())) {
            if (EVENT_ID.equals(creator.getBarEventId())
                    && !(creator instanceof SinniBarEventCreator)) {
                manager.getCreators().remove(creator);
            }
        }
        if (!manager.hasEventCreator(SinniBarEventCreator.class)) {
            manager.addEventCreator(new SinniBarEventCreator());
        }
    }

    public static boolean isEligible() {
        return Global.getSector() != null
                && !Global.getSector().getMemoryWithoutUpdate().getBoolean(STARTED)
                && !Global.getSector().getMemoryWithoutUpdate()
                        .getBoolean(LEGACY_DECLINE_TIMEOUT);
    }

    @Override public String getBarEventId() { return EVENT_ID; }
    @Override public boolean isAlwaysShow() { return true; }

    @Override
    public boolean shouldShowAtMarket(MarketAPI market) {
        return market != null && isEligible();
    }

    @Override public boolean shouldRemoveEvent() { return done; }

    @Override
    public void addPromptAndOption(InteractionDialogAPI currentDialog,
                                   Map<String, MemoryAPI> currentMemoryMap) {
        if (!isEligible()) return;
        setDialogueVariables(currentMemoryMap);

        RulesAPI rules = Global.getSector().getRules();
        RuleAPI rule = rules.getBestMatching(
                null, PROMPT_TRIGGER, currentDialog, currentMemoryMap);
        if (rule == null) return;

        String prompt = resolve(rule.getId(), rule.pickText(),
                currentDialog, currentMemoryMap);
        List<Option> promptOptions = rule.getOptions();
        if (prompt == null || promptOptions == null || promptOptions.isEmpty()) return;

        currentDialog.getTextPanel().addPara(prompt);
        String optionText = resolve(rule.getId(), promptOptions.get(0).text,
                currentDialog, currentMemoryMap);
        currentDialog.getOptionPanel().addOption(
                optionText, this, getOptionColor(),
                "Meet the woman who sent the note");
    }

    @Override
    public void init(InteractionDialogAPI currentDialog,
                     Map<String, MemoryAPI> currentMemoryMap) {
        super.init(currentDialog, currentMemoryMap);
        setDialogueVariables(currentMemoryMap);
        showStage(INTRO_TRIGGER);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (optionData == null) return;
        String optionId = optionData.toString();
        if (LEAVE.equals(optionId)) {
            done = true;
            return;
        }
        if (ACCEPT.equals(optionId)) {
            showStage(RULE_PREFIX + "accept");
            return;
        }
        if ((OPTION_PREFIX + "coin_gesture").equals(optionId)
                || (OPTION_PREFIX + "coin_price").equals(optionId)) {
            showStage(RULE_PREFIX + "coin");
            return;
        }
        if ((OPTION_PREFIX + "deal_agree").equals(optionId)
                || (OPTION_PREFIX + "deal_nod").equals(optionId)) {
            showStage(RULE_PREFIX + "deal");
            return;
        }
        if (optionId.startsWith(OPTION_PREFIX)) {
            showStage(RULE_PREFIX + optionId.substring(OPTION_PREFIX.length()));
        }
    }

    private void showStage(String trigger) {
        options.clearOptions();
        dialog.getVisualPanel().hideSecondPerson();
        com.fs.starfarer.api.characters.PersonAPI sinni =
                SinniContact.getOrCreatePerson();
        if (sinni != null) dialog.getVisualPanel().showPersonInfo(sinni);
        if (!FireBest.fire(null, dialog, memoryMap, trigger)) {
            done = true;
        }
    }

    private void setDialogueVariables(Map<String, MemoryAPI> currentMemoryMap) {
        if (currentMemoryMap == null || Global.getSector() == null) return;
        MemoryAPI local = currentMemoryMap.get(MemKeys.LOCAL);
        if (local == null) return;
        local.set(PLAYER_FEMALE,
                Global.getSector().getPlayerPerson().getName().getGender()
                        == FullName.Gender.FEMALE);
        local.set(PLAYER_NAME,
                Global.getSector().getPlayerPerson().getNameString());
        local.set(PLAYER_TITLE, "Captain");
    }

    private String resolve(String ruleId, String value,
                           InteractionDialogAPI currentDialog,
                           Map<String, MemoryAPI> currentMemoryMap) {
        if (value == null || Global.getSector() == null
                || Global.getSector().getRules() == null) return value;
        return Global.getSector().getRules().performTokenReplacement(
                ruleId, value, currentDialog.getInteractionTarget(),
                currentMemoryMap);
    }

    private static Color getOptionColor() {
        try {
            Color configured = Global.getSettings().getColor(
                    OPTION_COLOR_SETTING);
            return configured == null ? DEFAULT_OPTION_COLOR : configured;
        } catch (RuntimeException ex) {
            return DEFAULT_OPTION_COLOR;
        }
    }
}
