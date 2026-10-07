package chiefnavigator.campaign;

import chiefnavigator.skills.OdysseyCombatCoreOfficerPlugin;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;

/** Routes the Odyssean core through Starsector's normal AI-officer UI. */
public final class OdysseyAICoreCampaignPlugin extends BaseCampaignPlugin {
    public static final String ID = "chief_navigator_odyssey_ai_core_plugin";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public PluginPick<AICoreOfficerPlugin> pickAICoreOfficerPlugin(
            String commodityId) {
        if (!OdysseyCombatCoreOfficerPlugin.CORE_ID.equals(commodityId)) {
            return null;
        }
        return new PluginPick<AICoreOfficerPlugin>(
                new OdysseyCombatCoreOfficerPlugin(),
                PickPriority.MOD_SPECIFIC);
    }
}
