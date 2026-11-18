package data.campaign.rulecmd;

import chiefnavigator.quest.MenelausTrial;
import chiefnavigator.quest.OdysseyExpanseSystem;
import chiefnavigator.quest.PostLaborHuntIntel;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddShip;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;
import java.util.List;
import java.util.Map;

/** One-time prototype transfer through Kleon's rules-authored Ithaca conversation. */
public final class ChiefNavigatorIthacaPrototypeCMD extends BaseCommandPlugin {
    public static final String CLAIMED = "$chief_navigator_ithaca_onslaught_prototype_claimed_v1";
    public static final String VARIANT = "chief_navigator_onslaught_ith_Standard";
    public static final String ACCEPT = "chief_navigator_ithaca_prototype_accept";

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog,
            List<Token> params, Map<String, MemoryAPI> memoryMap) {
        if (Global.getSector() == null || dialog == null || params == null || params.isEmpty()
                || memoryMap == null || dialog.getPlugin() == null
                || !OdysseyExpanseSystem.isFobIthaca(dialog.getInteractionTarget())) return false;
        Object context = dialog.getPlugin().getContext();
        if (!(context instanceof PersonAPI)) return false;
        PersonAPI person = (PersonAPI) context;
        MemoryAPI local = memoryMap.get(MemKeys.LOCAL);
        if (!PostLaborHuntIntel.isKleon(person) || local != person.getMemoryWithoutUpdate()) return false;
        MemoryAPI global = Global.getSector().getMemoryWithoutUpdate();
        String action = params.get(0).getString(memoryMap);
        if ("claimed".equals(action)) return global.getBoolean(CLAIMED);
        boolean available = global.getBoolean(MenelausTrial.FINAL_DEBRIEF_COMPLETE)
                && !MenelausTrial.isFinalLaborFailed() && !global.getBoolean(CLAIMED);
        if ("available".equals(action)) return available;
        if (!"claim".equals(action) || !available || !ACCEPT.equals(local.getString("$option"))) return false;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player == null || player.getFleetData() == null || Global.getFactory() == null
                || Global.getSettings().getVariant(VARIANT) == null) return false;
        FleetMemberAPI prototype = Global.getFactory().createFleetMember(FleetMemberType.SHIP, VARIANT);
        if (prototype == null) return false;
        prototype.getRepairTracker().setCR(prototype.getRepairTracker().getMaxCR());
        player.getFleetData().addFleetMember(prototype);
        // Commit before presentation: a UI failure must never make this reward repeatable.
        global.set(CLAIMED, true);
        try {
            if (dialog.getVisualPanel() != null) dialog.getVisualPanel().showFleetMemberInfo(prototype, true);
            if (dialog.getTextPanel() != null) AddShip.addShipGainText(prototype, dialog.getTextPanel());
        } catch (RuntimeException error) {
            Global.getLogger(ChiefNavigatorIthacaPrototypeCMD.class).warn(
                    "Prototype transferred; reward presentation could not be completed", error);
        }
        return true;
    }
}
