package data.campaign.rulecmd;

import chiefnavigator.config.ChiefNavigatorSettings;
import chiefnavigator.quest.SinniBarEvent;
import chiefnavigator.quest.SinniContact;
import chiefnavigator.quest.SinniEventideIntel;
import chiefnavigator.quest.SinniOneMoreHorizonIntel;
import chiefnavigator.quest.TroyDepartureInteraction;
import chiefnavigator.topography.SinniHyperspaceTopographyEventIntel;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.RuleAPI;
import com.fs.starfarer.api.campaign.rules.RulesAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;
import java.awt.Color;
import java.util.List;
import java.util.Map;

/** Side effects for the rules-driven Sinni introduction. */
public final class ChiefNavigatorSinniIntroCMD extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog,
                           List<Token> params, Map<String, MemoryAPI> memoryMap) {
        if (params.isEmpty()) return false;
        String action = params.get(0).getString(memoryMap);
        if ("archiveText".equals(action)) {
            if (params.size() < 3 || dialog == null) return false;
            String style = params.get(1).getString(memoryMap);
            String trigger = params.get(2).getString(memoryMap);
            boolean warning = "warning".equals(style);
            if ((!warning && !"denied".equals(style))
                    || (warning ? params.size() != 3 : params.size() < 4)
                    || trigger == null || trigger.isBlank()) return false;
            TextPanelAPI text = dialog.getTextPanel();
            if (text == null) return false;
            String[] highlights = new String[params.size() - 3];
            for (int index = 3; index < params.size(); index++) {
                String phrase = params.get(index).getStringWithTokenReplacement(
                        ruleId, dialog, memoryMap);
                if (phrase == null || phrase.isBlank()) return false;
                highlights[index - 3] = phrase;
            }
            String prose = resolveTextOnlyRule(ruleId, dialog, memoryMap, trigger);
            if (prose == null) return false;
            try {
                text.setFontSmallInsignia();
                if (warning) text.addPara(prose, Misc.getNegativeHighlightColor());
                else text.addPara(prose, new Color(82, 88, 94),
                        Misc.getNegativeHighlightColor(), highlights);
                return true;
            } finally {
                text.setFontInsignia();
            }
        }
        if ("styledText".equals(action)) {
            if (params.size() != 3 || dialog == null) return false;
            String font = params.get(1).getString(memoryMap);
            String trigger = params.get(2).getString(memoryMap);
            if ((!"orbitron".equals(font) && !"victor".equals(font)
                    && !"victor16".equals(font)
                    && !"small_insignia".equals(font))
                    || trigger == null || trigger.isBlank()) return false;
            TextPanelAPI text = dialog.getTextPanel();
            if (text == null) return false;
            if ("victor16".equals(font)) {
                return showVictor16Text(ruleId, dialog, memoryMap, trigger, text);
            }
            try {
                if ("orbitron".equals(font)) text.setFontOrbitron();
                else if ("victor".equals(font)) text.setFontVictor();
                else text.setFontSmallInsignia();
                return FireBest.fire(ruleId, dialog, memoryMap, trigger);
            } finally {
                text.setFontInsignia();
            }
        }
        if ("routeInfo".equals(action)) {
            MemoryAPI local = memoryMap.get(MemKeys.LOCAL);
            if (local == null) return false;
            local.set("$chiefNavigatorTroyDPLimit", TroyDepartureInteraction.formatDP(
                    ChiefNavigatorSettings.getTroyFleetDPLimit()));
            return true;
        }
        if ("generousRoute".equals(action)) {
            return ChiefNavigatorSettings.getTroyFleetDPLimit() >= 240f;
        }
        if ("accept".equals(action)) {
            Global.getSector().getMemoryWithoutUpdate().set(SinniBarEvent.STARTED, true);
            SinniContact.joinPlayerFleet(dialog == null
                    ? null : dialog.getTextPanel());
            SinniHyperspaceTopographyEventIntel.unlockForSinni(dialog);
            SinniEventideIntel.ensureExists(dialog.getTextPanel());
            SinniOneMoreHorizonIntel.ensureExists(dialog.getTextPanel());
            return true;
        }
        if ("brief".equals(action)) {
            SinniEventideIntel.complete(dialog.getTextPanel());
            return true;
        }
        return false;
    }

    private static boolean showVictor16Text(String ruleId, InteractionDialogAPI dialog,
                                           Map<String, MemoryAPI> memoryMap, String trigger,
                                           TextPanelAPI text) {
        String prose = resolveTextOnlyRule(ruleId, dialog, memoryMap, trigger);
        if (prose == null) return false;
        TooltipMakerAPI tooltip = text.beginTooltip();
        tooltip.setParaFont("graphics/fonts/victor16.fnt");
        tooltip.addPara(prose, 0f);
        text.addTooltip();
        return true;
    }

    private static String resolveTextOnlyRule(String ruleId, InteractionDialogAPI dialog,
                                              Map<String, MemoryAPI> memoryMap, String trigger) {
        if (Global.getSector() == null) return null;
        RulesAPI rules = Global.getSector().getRules();
        if (rules == null) return null;
        RuleAPI selected = rules.getBestMatching(ruleId, trigger, dialog, memoryMap);
        if (selected == null || !selected.getOptions().isEmpty()
                || !selected.getScriptCopy().isEmpty()) return null;
        String prose = selected.pickText();
        if (prose == null || prose.isBlank()) return null;
        prose = rules.performTokenReplacement(selected.getId(), prose,
                dialog.getInteractionTarget(), memoryMap);
        return prose == null || prose.isBlank() ? null : prose;
    }
}
