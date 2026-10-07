package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/** Hall-style full interaction shown after Menelaus closes the final debrief. */
public final class MenelausCompletionDialogPlugin
        extends ChoicePreservingDialogPlugin {
    static final String PENDING =
            "$chief_navigator_menelaus_completion_scene_pending_v1";
    static final String SHOWN =
            "$chief_navigator_menelaus_completion_scene_shown_v1";

    private static final String ILLUSTRATION_CATEGORY = "illustrations";
    private static final String ILLUSTRATION_ID =
            "chief_navigator_sinni_ending";
    private static final String RULE_PREFIX =
            "ChiefNavigatorLabor5Ending_";
    private static final String PLAYER_TITLE =
            "$chiefNavigatorLabor5PlayerTitle";
    private static final float ILLUSTRATION_WIDTH = 480f;
    private static final float ILLUSTRATION_HEIGHT = 300f;

    private final Map<String, MemoryAPI> memoryMap =
            new HashMap<String, MemoryAPI>();
    private InteractionDialogAPI dialog;

    static void request() {
        if (Global.getSector() == null) return;
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        if (!memory.getBoolean(SHOWN)) memory.set(PENDING, true);
    }

    static void resetForTesting() {
        if (Global.getSector() == null) return;
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        memory.unset(PENDING);
        memory.unset(SHOWN);
    }

    /** Waits until the Menelaus interaction has actually closed. */
    static void tryShowPending() {
        SectorAPI sector = Global.getSector();
        if (sector == null || sector.getPlayerFleet() == null) return;
        MemoryAPI memory = sector.getMemoryWithoutUpdate();
        if (!memory.getBoolean(PENDING) || memory.getBoolean(SHOWN)) return;

        CampaignUIAPI ui = sector.getCampaignUI();
        if (ui == null || ui.isShowingDialog() || ui.isShowingMenu()
                || ui.getCurrentCoreTab() != null) {
            return;
        }

        boolean opened = ui.showInteractionDialog(
                new MenelausCompletionDialogPlugin(),
                sector.getPlayerFleet());
        if (opened) {
            memory.unset(PENDING);
            memory.set(SHOWN, true);
        }
    }

    @Override
    protected void initConversation(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        dialog.setPromptText("");
        dialog.setBackgroundDimAmount(1f);
        SpriteAPI illustration = Global.getSettings().getSprite(
                ILLUSTRATION_CATEGORY, ILLUSTRATION_ID);
        dialog.getVisualPanel().showCustomPanel(
                ILLUSTRATION_WIDTH,
                ILLUSTRATION_HEIGHT,
                new LetterboxIllustrationPlugin(illustration));
        MenelausEndingMusic.start(dialog);
        prepareMemoryMap();
        if (!RuleDialogSupport.fire(
                dialog, memoryMap, RULE_PREFIX + "opening")) {
            Global.getLogger(MenelausCompletionDialogPlugin.class).warn(
                    "Missing Labor V ending opening rule");
            dialog.dismiss();
        }
    }

    private void prepareMemoryMap() {
        SectorAPI sector = Global.getSector();
        if (sector == null) return;
        MemoryAPI global = sector.getMemoryWithoutUpdate();
        memoryMap.put(MemKeys.GLOBAL, global);
        MemoryAPI local = Global.getFactory().createMemory();
        local.set(PLAYER_TITLE, "Captain");
        memoryMap.put(MemKeys.LOCAL, local);
        if (sector.getPlayerFleet() != null) {
            memoryMap.put(
                    MemKeys.PLAYER,
                    sector.getPlayerFleet().getMemoryWithoutUpdate());
        }
    }

    @Override
    protected void handleOptionSelected(String optionText, Object optionData) {
        if (optionData == null || dialog == null) return;
        String optionId = optionData.toString();
        if (!RuleDialogSupport.fire(
                dialog, memoryMap, RULE_PREFIX + optionId)) {
            Global.getLogger(MenelausCompletionDialogPlugin.class).warn(
                    "Missing Labor V ending stage " + optionId);
            dialog.dismiss();
        }
    }

    @Override public void optionMousedOver(String text, Object data) { }
    @Override public void advance(float amount) { }
    @Override public void backFromEngagement(EngagementResultAPI result) { }
    @Override public Object getContext() { return null; }
    @Override public Map<String, MemoryAPI> getMemoryMap() { return memoryMap; }

    /** Preserves the full illustration rather than cropping it to the panel. */
    private static final class LetterboxIllustrationPlugin
            extends BaseCustomUIPanelPlugin {
        private final SpriteAPI illustration;
        private final float sourceWidth;
        private final float sourceHeight;
        private PositionAPI position;

        private LetterboxIllustrationPlugin(SpriteAPI illustration) {
            this.illustration = illustration;
            sourceWidth = illustration.getWidth();
            sourceHeight = illustration.getHeight();
        }

        @Override
        public void positionChanged(PositionAPI position) {
            this.position = position;
        }

        @Override
        public void renderBelow(float alphaMult) {
            if (position == null || sourceWidth <= 0f || sourceHeight <= 0f) {
                return;
            }
            float centerX = position.getX() + position.getWidth() * 0.5f;
            float centerY = position.getY() + position.getHeight() * 0.5f;
            float oldWidth = illustration.getWidth();
            float oldHeight = illustration.getHeight();
            float oldAlpha = illustration.getAlphaMult();
            Color oldColor = illustration.getColor();

            illustration.setColor(Color.BLACK);
            illustration.setSize(position.getWidth(), position.getHeight());
            illustration.setAlphaMult(alphaMult);
            illustration.renderAtCenter(centerX, centerY);

            float scale = Math.min(
                    position.getWidth() / sourceWidth,
                    position.getHeight() / sourceHeight);
            illustration.setColor(Color.WHITE);
            illustration.setSize(sourceWidth * scale, sourceHeight * scale);
            illustration.renderAtCenter(centerX, centerY);

            illustration.setSize(oldWidth, oldHeight);
            illustration.setAlphaMult(oldAlpha);
            illustration.setColor(oldColor);
        }
    }
}
