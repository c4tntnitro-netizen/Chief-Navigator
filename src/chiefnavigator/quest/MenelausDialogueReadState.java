package chiefnavigator.quest;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import java.util.LinkedHashSet;
import java.util.Set;

/** Presentation-only, durable read history for the explicitly authored Menelaus choices. */
public final class MenelausDialogueReadState {
    private static final String OPTION_PREFIX = "chief_navigator_";
    private static final String READ_PREFIX = "$chief_navigator_menelaus_read_";
    // Exact authored topic/response IDs, not runtime text matching. Navigation,
    // repairs, installation, and final-Labor acceptance keep their existing colors.
    static final Set<String> TOPICS = Set.of(
            "auto_page_chief_navigator_menelaus_labor_four_sinni_why_2",
            "fob_ithaca_upgrades",
            "ithaca_menelaus_labors",
            "menelaus_debrief_gautama",
            "menelaus_first_abomination_core",
            "menelaus_first_abomination_unexpected",
            "menelaus_first_business_core",
            "menelaus_first_business_direct",
            "menelaus_first_business_then",
            "menelaus_first_core_ai",
            "menelaus_first_core_alpha",
            "menelaus_first_helen_owe",
            "menelaus_first_helen_pat",
            "menelaus_first_helen_sinni",
            "menelaus_first_hmm_apologies",
            "menelaus_first_hmm_silent",
            "menelaus_first_hmm_understood",
            "menelaus_first_losing",
            "menelaus_first_losing_2",
            "menelaus_first_need",
            "menelaus_first_need_accept_direct",
            "menelaus_first_need_authority",
            "menelaus_first_need_mercenary",
            "menelaus_first_need_refuse",
            "menelaus_first_onslaught_2",
            "menelaus_first_onslaught_3",
            "menelaus_first_onslaught_look",
            "menelaus_first_onslaught_what",
            "menelaus_first_quiet_2",
            "menelaus_first_quiet_3",
            "menelaus_first_quiet_power",
            "menelaus_first_quiet_watch",
            "menelaus_first_quiet_what",
            "menelaus_first_sinni_assume",
            "menelaus_first_sinni_insists_go",
            "menelaus_first_sinni_insists_go_yvan",
            "menelaus_first_sinni_ready",
            "menelaus_first_sinni_request",
            "menelaus_first_sinni_request_go",
            "menelaus_first_sinni_request_go_yvan",
            "menelaus_first_sinni_stay",
            "menelaus_first_sinni_stay_insubordination",
            "menelaus_first_sinni_stay_no",
            "menelaus_first_unexpected",
            "menelaus_helen_changed",
            "menelaus_helen_changed_2",
            "menelaus_helen_changed_3",
            "menelaus_helen_changed_5",
            "menelaus_helen_changed_6",
            "menelaus_helen_diary",
            "menelaus_helen_final",
            "menelaus_helen_kenosis_home",
            "menelaus_helen_kenosis_love",
            "menelaus_helen_kenosis_man",
            "menelaus_helen_navarch",
            "menelaus_helen_navarch_2",
            "menelaus_helen_sinni_explanation",
            "menelaus_helen_sinni_explanation_2",
            "menelaus_helen_sinni_punch",
            "menelaus_labor_5_realization_2",
            "menelaus_labor_5_realization_3",
            "menelaus_labor_five_breach",
            "menelaus_labor_five_depart",
            "menelaus_labor_five_hostswarms",
            "menelaus_labor_five_line",
            "menelaus_labor_four_active",
            "menelaus_labor_four_detected",
            "menelaus_labor_four_helen",
            "menelaus_labor_four_helen_press",
            "menelaus_labor_four_helen_sinni",
            "menelaus_labor_four_middle",
            "menelaus_labor_four_package",
            "menelaus_labor_four_reach",
            "menelaus_labor_four_sinni_choice",
            "menelaus_labor_four_sinni_refuse",
            "menelaus_labor_four_sinni_why",
            "menelaus_labor_four_starving",
            "menelaus_labor_four_threat",
            "menelaus_labor_four_war",
            "menelaus_labor_one",
            "menelaus_labor_one_collapse",
            "menelaus_labor_one_drone",
            "menelaus_labor_one_helen",
            "menelaus_labor_one_helen_collapse",
            "menelaus_labor_one_name",
            "menelaus_labor_one_name_funny",
            "menelaus_labor_one_name_look",
            "menelaus_labor_three_active",
            "menelaus_labor_three_capture",
            "menelaus_labor_three_disable",
            "menelaus_labor_three_helen",
            "menelaus_labor_three_helen_close",
            "menelaus_labor_three_helen_sinni",
            "menelaus_labor_three_history",
            "menelaus_labor_two_active",
            "menelaus_labor_two_all",
            "menelaus_labor_two_collapse",
            "menelaus_labor_two_cryo",
            "menelaus_labor_two_gautama",
            "menelaus_labor_two_gautama_destroy",
            "menelaus_labor_two_gautama_many",
            "menelaus_labor_two_gautama_transfer",
            "menelaus_labor_two_helen",
            "menelaus_labor_two_helen_sinni",
            "menelaus_labor_two_helen_why",
            "menelaus_labor_two_what");
    static final Set<String> CONTINUATIONS = Set.of(
            "auto_page_chief_navigator_fob_ithaca_debrief_2",
            "auto_page_chief_navigator_fob_ithaca_debrief_3",
            "auto_page_chief_navigator_fob_ithaca_debrief_4",
            "auto_page_chief_navigator_fob_ithaca_final_offer_3",
            "auto_page_chief_navigator_fob_ithaca_research_briefing_2",
            "auto_page_chief_navigator_fob_ithaca_wall_briefing_2",
            "auto_page_chief_navigator_menelaus_first_holodeck_2",
            "auto_page_chief_navigator_menelaus_first_holodeck_3",
            "auto_page_chief_navigator_menelaus_first_holodeck_4",
            "auto_page_chief_navigator_menelaus_first_holodeck_5",
            "auto_page_chief_navigator_menelaus_first_monorail_2",
            "auto_page_chief_navigator_menelaus_first_monorail_3",
            "auto_page_chief_navigator_menelaus_first_need_2",
            "auto_page_chief_navigator_menelaus_first_need_3",
            "auto_page_chief_navigator_menelaus_first_need_4",
            "auto_page_chief_navigator_menelaus_first_sinni_go_2",
            "auto_page_chief_navigator_menelaus_first_sinni_go_3",
            "auto_page_chief_navigator_menelaus_first_sinni_go_4",
            "auto_page_chief_navigator_menelaus_first_sinni_helen_2",
            "auto_page_chief_navigator_menelaus_first_sinni_helen_3",
            "auto_page_chief_navigator_menelaus_helen_diary_2",
            "auto_page_chief_navigator_menelaus_helen_diary_3",
            "auto_page_chief_navigator_menelaus_helen_diary_4",
            "auto_page_chief_navigator_menelaus_helen_diary_5",
            "auto_page_chief_navigator_menelaus_helen_final_2",
            "auto_page_chief_navigator_menelaus_helen_final_3",
            "auto_page_chief_navigator_menelaus_labor_five_depart_2",
            "auto_page_chief_navigator_menelaus_labor_five_field_briefing_2",
            "auto_page_chief_navigator_menelaus_labor_five_hostswarms_2",
            "auto_page_chief_navigator_menelaus_labor_five_hostswarms_3",
            "auto_page_chief_navigator_menelaus_labor_four_helen_2",
            "auto_page_chief_navigator_menelaus_labor_four_helen_3",
            "auto_page_chief_navigator_menelaus_labor_four_reach_2",
            "auto_page_chief_navigator_menelaus_labor_four_reach_3",
            "auto_page_chief_navigator_menelaus_labor_four_reach_4",
            "auto_page_chief_navigator_menelaus_labor_four_sinni_choice_2",
            "auto_page_chief_navigator_menelaus_labor_four_sinni_choice_3",
            "auto_page_chief_navigator_menelaus_labor_four_sinni_why_3",
            "auto_page_chief_navigator_menelaus_labor_four_understood_2",
            "auto_page_chief_navigator_menelaus_labor_four_understood_3",
            "auto_page_chief_navigator_menelaus_labor_one_briefing_2",
            "auto_page_chief_navigator_menelaus_labor_one_briefing_3",
            "auto_page_chief_navigator_menelaus_labor_one_collapse_2",
            "auto_page_chief_navigator_menelaus_labor_one_collapse_3",
            "auto_page_chief_navigator_menelaus_labor_one_helen_2",
            "auto_page_chief_navigator_menelaus_labor_one_helen_3",
            "auto_page_chief_navigator_menelaus_labor_three_helen_2",
            "auto_page_chief_navigator_menelaus_labor_three_helen_3",
            "auto_page_chief_navigator_menelaus_labor_three_history_2",
            "auto_page_chief_navigator_menelaus_labor_three_history_3",
            "auto_page_chief_navigator_menelaus_labor_two_collapse_2",
            "auto_page_chief_navigator_menelaus_labor_two_collapse_3",
            "auto_page_chief_navigator_menelaus_labor_two_cryo_gautama_2",
            "auto_page_chief_navigator_menelaus_labor_two_helen_2",
            "auto_page_chief_navigator_menelaus_labor_two_helen_3",
            "menelaus_early_wall_continue",
            "menelaus_first_finish",
            "menelaus_first_losing_return",
            "menelaus_first_monorail",
            "menelaus_first_need_return",
            "menelaus_first_shuttle_continue",
            "menelaus_first_strategos_approach",
            "menelaus_helen_changed_4",
            "menelaus_helen_kenosis_continue",
            "menelaus_helen_questions",
            "menelaus_report_motherships",
            "menelaus_report_research",
            "menelaus_report_sensor");

    private final Set<String> pending = new LinkedHashSet<String>();

    /** Call only after a successful rules dispatch, once its actual options exist. */
    void afterPage(InteractionDialogAPI dialog, MemoryAPI global, Object selected) {
        if (dialog == null || global == null || dialog.getOptionPanel() == null) return;
        String suffix = topicSuffix(selected);
        if (suffix != null) pending.add(readKey(suffix, global));

        // A multi-page answer is not read just because its opening was selected.
        // Keep it pending until the linear Continue sequence reaches a choice/menu.
        boolean continuing = false;
        for (String continuation : CONTINUATIONS) {
            if (dialog.getOptionPanel().hasOption(OPTION_PREFIX + continuation)) {
                continuing = true;
                break;
            }
        }
        if (!continuing) {
            for (String key : pending) global.set(key, true);
            pending.clear();
        }
        highlightOptions(dialog, global);
    }

    public static void highlightOptions(InteractionDialogAPI dialog, MemoryAPI global) {
        if (dialog == null || global == null || dialog.getOptionPanel() == null) return;
        for (String topic : TOPICS) {
            String id = OPTION_PREFIX + topic;
            if (dialog.getOptionPanel().hasOption(id)) {
                dialog.setOptionColor(id, global.getBoolean(readKey(topic, global))
                        ? Misc.getButtonTextColor() : Misc.getHighlightColor());
            }
        }
    }

    private static String topicSuffix(Object selected) {
        if (!(selected instanceof String)) return null;
        String id = (String) selected;
        if (!id.startsWith(OPTION_PREFIX)) return null;
        String suffix = id.substring(OPTION_PREFIX.length());
        return TOPICS.contains(suffix) ? suffix : null;
    }

    static String readKey(String suffix, MemoryAPI global) {
        // Discuss the Labors opens new material at each Labor, rather than
        // staying permanently read after the first introductory visit.
        if ("ithaca_menelaus_labors".equals(suffix)) {
            suffix += global.getBoolean(MenelausTrial.ACCEPTED) ? "_accepted" : "_intro";
            for (String labor : new String[] {"i", "ii", "iii", "iv"}) {
                suffix += global.getBoolean("$chief_navigator_menelaus_labor_"
                        + labor + "_reported_v1") ? "_1" : "_0";
            }
            suffix += global.getBoolean(MenelausTrial.FINAL_DEBRIEF_COMPLETE) ? "_complete" : "";
        }
        return READ_PREFIX + suffix;
    }
}

