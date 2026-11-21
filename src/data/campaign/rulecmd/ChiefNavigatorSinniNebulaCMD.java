package data.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.VisualPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;
import java.util.List;
import java.util.Map;

/** Presentation only; the reveal timing and credit wording belong to rules.csv. */
public final class ChiefNavigatorSinniNebulaCMD extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog,
            List<Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || dialog.getVisualPanel() == null
                || params.size() != 2) return false;

        String path = Global.getSettings().getSpriteName("illustrations",
                "chief_navigator_sinni_nebula_painting");
        dialog.showVisualPanel();
        VisualPanelAPI visual = dialog.getVisualPanel();
        visual.hideSecondPerson();
        visual.hideThirdPerson();
        // The original artwork is square. Fit it whole within the ordinary
        // illustration area, with a separate readable two-line credit below.
        CustomPanelAPI panel = visual.showCustomPanel(480f, 348f, null);
        if (panel == null) return false;
        TooltipMakerAPI painting = panel.createUIElement(300f, 300f, false);
        painting.addImage(path, 300f, 300f, 0f);
        panel.addUIElement(painting).inTL(90f, 0f);

        TooltipMakerAPI credit = panel.createUIElement(480f, 42f, false);
        credit.setParaSmallInsignia();
        credit.addPara(params.get(0).getString(memoryMap),
                Misc.getGrayColor(), 0f);
        credit.addPara(params.get(1).getString(memoryMap),
                Misc.getGrayColor(), 0f);
        panel.addUIElement(credit).inTL(0f, 306f);
        return true;
    }
}
