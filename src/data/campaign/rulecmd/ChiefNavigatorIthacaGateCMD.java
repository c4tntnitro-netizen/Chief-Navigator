package data.campaign.rulecmd;

import chiefnavigator.quest.MenelausTrial;
import chiefnavigator.quest.OdysseyExpanseSystem;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI.JumpDestination;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerUtil;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.GateEntityPlugin;
import com.fs.starfarer.api.impl.campaign.GateEntityPlugin.GateData;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.missions.GateCMD;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;
import java.util.List;
import java.util.Map;

/** Direct access to the scanned Ithaca gate without navigating the remote map. */
public final class ChiefNavigatorIthacaGateCMD extends BaseCommandPlugin {
    public static final String OPTION_ID = "chief_navigator_travel_ithaca_gate";
    private static final String TRANSIT_PENDING = "$chief_navigator_ithaca_gate_transit_pending";

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog,
            List<Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || params == null || params.isEmpty()) return false;
        String action = params.get(0).getString(memoryMap);
        SectorEntityToken destination = destinationFor(dialog);
        if ("canTravel".equals(action)) return destination != null;
        if ("highlight".equals(action)) {
            if (destination == null) {
                dialog.getOptionPanel().setEnabled(OPTION_ID, false);
                return false;
            }
            int required = GateCMD.computeFuelCost(destination);
            float available = Global.getSector().getPlayerFleet().getCargo().getFuel();
            boolean enough = available >= required;
            dialog.getOptionPanel().setEnabled(OPTION_ID, enough);
            dialog.getOptionPanel().setTooltip(OPTION_ID,
                    "Travel to Ithaca Janus Gate in Ashen Verge.\nFuel required: "
                            + Misc.getWithDGS(required) + "."
                            + (enough ? "" : "\nThe fleet does not have enough fuel."));
            return true;
        }
        if (!"traverse".equals(action) || destination == null) return false;

        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        CargoAPI cargo = player.getCargo();
        int required = GateCMD.computeFuelCost(destination);
        if (cargo.getFuel() < required) {
            dialog.getOptionPanel().setEnabled(OPTION_ID, false);
            dialog.getTextPanel().addPara("The fleet does not have enough fuel to reach FOB Ithaca.",
                    Misc.getNegativeHighlightColor());
            return false;
        }
        SectorEntityToken source = dialog.getInteractionTarget();
        // A zero-duration native memory flag prevents reentrant selection until time advances.
        source.getMemoryWithoutUpdate().set(TRANSIT_PENDING, true, 0f);
        cargo.removeFuel(required);
        dialog.dismiss();
        Global.getSector().setPaused(false);
        Global.getSector().doHyperspaceTransition(player, source,
                new JumpDestination(destination, null), 2f);
        float distance = Misc.getDistanceLY(source, destination);
        ((GateEntityPlugin) destination.getCustomPlugin()).showBeingUsed(distance);
        ((GateEntityPlugin) source.getCustomPlugin()).showBeingUsed(distance);
        ListenerUtil.reportFleetTransitingGate(player, source, destination);
        return true;
    }

    /** Resolves existing objects only; never scans, registers or reconstructs a gate. */
    private static SectorEntityToken destinationFor(InteractionDialogAPI dialog) {
        if (Global.getSector() == null || !MenelausTrial.isComplete()
                || MenelausTrial.isFinalLaborFailed()) return null;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        SectorEntityToken source = dialog.getInteractionTarget();
        if (player == null || player.getCargo() == null || player.getBattle() != null
                || player.isInHyperspaceTransition() || source == null
                || source.getContainingLocation() == null
                || source.getContainingLocation() != player.getContainingLocation()
                || !(source.getCustomPlugin() instanceof GateEntityPlugin)
                || !source.hasTag(Tags.GATE)
                || OdysseyExpanseSystem.isDevouredRing(source)
                || !GateEntityPlugin.isScanned(source)
                || source.getMemoryWithoutUpdate().getBoolean(TRANSIT_PENDING)
                || !GateEntityPlugin.areGatesActive() || !GateEntityPlugin.canUseGates()) return null;
        StarSystemAPI ashen = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.ASHEN_VERGE_ID);
        if (ashen == null) return null;
        SectorEntityToken destination = ashen.getEntityById(OdysseyExpanseSystem.FOB_ITHACA_GATE_ID);
        if (destination == source || !OdysseyExpanseSystem.isFobIthacaGate(destination)
                || destination.getContainingLocation() != ashen
                || !(destination.getCustomPlugin() instanceof GateEntityPlugin)
                || !destination.hasTag(Tags.GATE)
                || !GateEntityPlugin.isScanned(destination)) return null;
        Object saved = Global.getSector().getPersistentData().get(GateEntityPlugin.GATE_DATA);
        if (!(saved instanceof GateData data) || data.scanned == null
                || !data.scanned.contains(source) || !data.scanned.contains(destination)) return null;
        return destination;
    }
}
