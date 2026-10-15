package chiefnavigator.console;

import chiefnavigator.quest.IthacaSectionEncounter;
import chiefnavigator.quest.OdysseyExpanseSystem;
import chiefnavigator.quest.TaskForceSpartanFaction;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import java.util.List;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

/** Removes only the idle, authored Breach Defense Station for campaign testing. */
public final class DestroyIthacaGateStation implements BaseCommand {
    enum RemovalResult { REMOVED, ABSENT, MISSING_WORLD, INCOMPATIBLE, BUSY }

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage("This command can only be used in the campaign.");
            return CommandResult.WRONG_CONTEXT;
        }
        if (args != null && !args.trim().isEmpty()) {
            Console.showMessage("Use: destroyithacagatestation");
            return CommandResult.BAD_SYNTAX;
        }
        switch (removeGateStation()) {
            case REMOVED:
                Console.showMessage("Removed Ithaca's Breach Defense Station. "
                        + "Ordinary pre-assault maintenance may rebuild it. "
                        + "Labor flags and other fleets are unchanged.");
                return CommandResult.SUCCESS;
            case ABSENT:
                Console.showMessage("Ithaca's Breach Defense Station is already absent.");
                return CommandResult.SUCCESS;
            case MISSING_WORLD:
                Console.showMessage("Saved Ashen Verge or FOB Ithaca is unavailable.");
                return CommandResult.ERROR;
            case BUSY:
                Console.showMessage("Leave the station's battle, transition or "
                        + "interaction before removing it.");
                return CommandResult.ERROR;
            default:
                Console.showMessage("The gate-station ID contains a player, foreign "
                        + "or incompatible object; it was left untouched.");
                return CommandResult.ERROR;
        }
    }

    /** The actual guarded removal path, also exercised by the regression tool. */
    static RemovalResult removeGateStation() {
        if (Global.getSector() == null) return RemovalResult.MISSING_WORLD;
        StarSystemAPI system = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.ASHEN_VERGE_ID);
        if (system == null || !OdysseyExpanseSystem.isFobIthaca(
                system.getEntityById(IthacaSectionEncounter.FOB_ENTITY_ID))) {
            return RemovalResult.MISSING_WORLD;
        }
        SectorEntityToken entity = system.getEntityById(
                OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_STATION_ID);
        if (entity == null) return RemovalResult.ABSENT;
        if (!(entity instanceof CampaignFleetAPI)) return RemovalResult.INCOMPATIBLE;
        CampaignFleetAPI station = (CampaignFleetAPI) entity;
        if (!isOwnedStation(station, system)) return RemovalResult.INCOMPATIBLE;
        if (isBusy(station)) return RemovalResult.BUSY;
        system.removeEntity(station);
        return RemovalResult.REMOVED;
    }

    private static boolean isOwnedStation(
            CampaignFleetAPI station, StarSystemAPI system) {
        if (!OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_STATION_ID.equals(
                    station.getId())
                || station.getContainingLocation() != system
                || station.isPlayerFleet()
                || station == Global.getSector().getPlayerFleet()
                || !station.isStationMode()
                || station.getFaction() == null
                || !TaskForceSpartanFaction.ID.equals(station.getFaction().getId())) {
            return false;
        }
        List<FleetMemberAPI> members = station.getFleetData().getMembersListCopy();
        if (members.size() != 1) return false;
        FleetMemberAPI core = members.get(0);
        return core.getVariant() != null && core.getHullSpec() != null
                && OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_VARIANT_ID.equals(
                        core.getVariant().getHullVariantId())
                && OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_HULL_ID.equals(
                        core.getHullSpec().getHullId());
    }

    private static boolean isBusy(CampaignFleetAPI station) {
        if (station.getBattle() != null || station.isInHyperspaceTransition()
                || station.isDespawning()) return true;
        InteractionDialogAPI dialog = Global.getSector().getCampaignUI() == null
                ? null : Global.getSector().getCampaignUI().getCurrentInteractionDialog();
        return dialog != null && dialog.getInteractionTarget() == station;
    }
}
