package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomDialogDelegate;
import com.fs.starfarer.api.campaign.CustomDialogDelegate.CustomDialogCallback;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;

/** Full-size contact overlay modeled on Hall of Triumph's Ship Gallery. */
public final class OrionKnotMapDialog {
    private static final float MAX_WIDTH = 1000f;
    private static final float MAX_HEIGHT = 820f;
    private static final float SCREEN_MARGIN = 180f;

    private OrionKnotMapDialog() { }

    public static void show(InteractionDialogAPI dialog) {
        if (dialog == null) return;
        float width = Math.max(700f, Math.min(MAX_WIDTH,
                Global.getSettings().getScreenWidth() - SCREEN_MARGIN));
        float height = Math.max(500f, Math.min(MAX_HEIGHT,
                Global.getSettings().getScreenHeight() - SCREEN_MARGIN));
        dialog.showCustomDialog(width, height,
                new MapDelegate(dialog, width, height));
    }

    private static final class MapDelegate extends BaseCustomDialogDelegate {
        private final InteractionDialogAPI dialog;
        private final OrionKnotChartPanelPlugin plugin;

        private MapDelegate(InteractionDialogAPI dialog,
                float width, float height) {
            this.dialog = dialog;
            this.plugin = new OrionKnotChartPanelPlugin(width, height);
        }

        @Override
        public void createCustomDialog(CustomPanelAPI panel,
                CustomDialogCallback callback) {
            plugin.init(panel);
        }

        @Override
        public String getConfirmText() {
            return "Close chart.";
        }

        @Override
        public boolean hasCancelButton() {
            return false;
        }

        @Override
        public void customDialogConfirm() {
            dialog.dismiss();
        }

        @Override
        public void customDialogCancel() {
            dialog.dismissAsCancel();
        }

        @Override
        public OrionKnotChartPanelPlugin getCustomPanelPlugin() {
            return plugin;
        }
    }
}
