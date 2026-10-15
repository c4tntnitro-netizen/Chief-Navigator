package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import java.awt.Color;
import java.util.Collections;
import java.util.Map;

/** One-time Labor V defeat scene after Heavenly Strike completes its gate scan. */
public final class FinalLaborGateScanFailureDialogPlugin
        extends ChoicePreservingDialogPlugin {
    static final String PENDING = "$chief_navigator_gate_scan_failure_scene_pending_v1";
    static final String SHOWN = "$chief_navigator_gate_scan_failure_scene_shown_v1";
    static final String ILLUSTRATION_CATEGORY = "illustrations";
    static final String ILLUSTRATION_ID = "bombard_saturation_result";
    static final String CONTINUE = "continue";
    private static final float ILLUSTRATION_WIDTH = 480f;
    private static final float ILLUSTRATION_HEIGHT = 300f;
    private InteractionDialogAPI dialog;

    public static void request() {
        SectorAPI sector = Global.getSector();
        if (sector == null || !MenelausTrial.isFinalLaborGateScanFailed()) return;
        MemoryAPI memory = sector.getMemoryWithoutUpdate();
        if (!memory.getBoolean(SHOWN)) memory.set(PENDING, true);
    }

    /** Retains a queued scene across saves and retries without interrupting another UI. */
    public static void tryShowPending() {
        SectorAPI sector = Global.getSector();
        if (sector == null || !MenelausTrial.isFinalLaborGateScanFailed()) return;
        MemoryAPI memory = sector.getMemoryWithoutUpdate();
        if (!memory.getBoolean(PENDING) || memory.getBoolean(SHOWN)) return;
        CampaignFleetAPI player = sector.getPlayerFleet();
        if (player == null || player.getBattle() != null || player.isInHyperspaceTransition()) return;
        CampaignUIAPI ui = sector.getCampaignUI();
        if (ui == null || ui.isShowingDialog() || ui.isShowingMenu()
                || ui.getCurrentInteractionDialog() != null || ui.getCurrentCoreTab() != null) return;
        if (ui.showInteractionDialog(new FinalLaborGateScanFailureDialogPlugin(), player)) {
            memory.unset(PENDING);
            memory.set(SHOWN, true);
        }
    }

    /** Used only when an explicit testing command restarts Labor V. */
    public static void resetForTesting() {
        if (Global.getSector() == null) return;
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        memory.unset(PENDING);
        memory.unset(SHOWN);
    }

    @Override
    protected void initConversation(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        dialog.setPromptText("");
        dialog.setBackgroundDimAmount(1f);
        SpriteAPI illustration = Global.getSettings().getSprite(
                ILLUSTRATION_CATEGORY, ILLUSTRATION_ID);
        dialog.getVisualPanel().showCustomPanel(ILLUSTRATION_WIDTH, ILLUSTRATION_HEIGHT,
                new LetterboxIllustrationPlugin(illustration));
        dialog.getTextPanel().addParagraph("GAME OVER", new Color(245, 80, 80));
        dialog.getTextPanel().addParagraph("The defense of ithaca has failed.");
        dialog.getTextPanel().addParagraph("The Heavenly Strike has completed its scan of Ithaca’s Gate. "
                + "To the horror of all who witness, the threat swarm disappears into the gateway. "
                + "You frantically contact all major factions to see where the Threat or Gautama may "
                + "have surfaced in the Core Worlds, but no one has seen anything of the sort.");
        dialog.getTextPanel().addParagraph("But you know. One day, the Sword of Damocles will fall.");
        dialog.getOptionPanel().clearOptions();
        dialog.getOptionPanel().addOption("Continue", CONTINUE);
    }

    @Override
    protected void handleOptionSelected(String optionText, Object optionData) {
        if (dialog != null && CONTINUE.equals(optionData)) dialog.dismiss();
    }

    @Override public void optionMousedOver(String text, Object data) { }
    @Override public void advance(float amount) { }
    @Override public void backFromEngagement(EngagementResultAPI result) { }
    @Override public Object getContext() { return null; }
    @Override public Map<String, MemoryAPI> getMemoryMap() {
        return Global.getSector() == null ? Collections.emptyMap()
                : Collections.singletonMap(MemKeys.GLOBAL, Global.getSector().getMemoryWithoutUpdate());
    }

    /** Shows the entire vanilla illustration and restores the borrowed sprite's state. */
    private static final class LetterboxIllustrationPlugin extends BaseCustomUIPanelPlugin {
        private final SpriteAPI illustration;
        private final float sourceWidth;
        private final float sourceHeight;
        private PositionAPI position;

        LetterboxIllustrationPlugin(SpriteAPI illustration) {
            this.illustration = illustration;
            sourceWidth = illustration.getWidth();
            sourceHeight = illustration.getHeight();
        }

        @Override public void positionChanged(PositionAPI position) { this.position = position; }

        @Override public void renderBelow(float alphaMult) {
            if (position == null || sourceWidth <= 0f || sourceHeight <= 0f) return;
            float centerX = position.getX() + position.getWidth() * 0.5f;
            float centerY = position.getY() + position.getHeight() * 0.5f;
            float oldWidth = illustration.getWidth();
            float oldHeight = illustration.getHeight();
            float oldAlpha = illustration.getAlphaMult();
            Color oldColor = illustration.getColor();
            try {
                illustration.setColor(Color.BLACK);
                illustration.setSize(position.getWidth(), position.getHeight());
                illustration.setAlphaMult(alphaMult);
                illustration.renderAtCenter(centerX, centerY);
                float scale = Math.min(position.getWidth() / sourceWidth, position.getHeight() / sourceHeight);
                illustration.setColor(Color.WHITE);
                illustration.setSize(sourceWidth * scale, sourceHeight * scale);
                illustration.renderAtCenter(centerX, centerY);
            } finally {
                illustration.setSize(oldWidth, oldHeight);
                illustration.setAlphaMult(oldAlpha);
                illustration.setColor(oldColor);
            }
        }
    }
}
