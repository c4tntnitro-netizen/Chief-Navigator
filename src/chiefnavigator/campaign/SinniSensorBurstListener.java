package chiefnavigator.campaign;

import chiefnavigator.topography.SinniHyperspaceTopographyEventIntel;
import chiefnavigator.topography.SinniHyperspaceTopographyEventIntel.SinniStage;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.AbilityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;

/** Starts Sinni's delayed sequence after the toolbar sensor burst activates. */
public final class SinniSensorBurstListener extends BaseCampaignEventListener {
    public SinniSensorBurstListener() {
        super(false);
    }

    @Override
    public void reportPlayerActivatedAbility(AbilityPlugin ability, Object param) {
        if (Global.getSector() == null || ability == null) return;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player == null || ability != player.getAbility(Abilities.SENSOR_BURST)) {
            return;
        }
        if (SinniHyperspaceTopographyEventIntel.isTierActive(
                SinniStage.ECHO_MAPPING)) {
            Global.getSector().addScript(new SinniSensorBurstEchoScript(player));
        }
    }
}
