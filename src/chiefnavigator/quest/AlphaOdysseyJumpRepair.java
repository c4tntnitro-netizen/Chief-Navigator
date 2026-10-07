package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI.JumpDestination;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.util.Misc;
import java.util.ArrayList;
import java.util.List;

/** Narrow correction for older Alpha saves with duplicate reciprocal exits. */
public final class AlphaOdysseyJumpRepair {
    private static final float MAX_DUPLICATE_DISTANCE = 250f;

    private AlphaOdysseyJumpRepair() { }

    public static void repairDuplicates() {
        if (Global.getSector() == null) return;
        int removed = repairDuplicates(OdysseyExpanseSystem.findExisting(),
                Global.getSector().getHyperspace());
        if (removed > 0) {
            Global.getLogger(AlphaOdysseyJumpRepair.class).info(
                    "Removed " + removed + " redundant Alpha Odyssey "
                            + "hyperspace endpoint(s); retained existing routes");
        }
    }

    static int repairDuplicates(StarSystemAPI expanse, LocationAPI hyperspace) {
        if (Global.getSector() == null || expanse == null || hyperspace == null
                || !hyperspace.isHyperspace()
                || !OdysseyExpanseSystem.SYSTEM_ID.equals(
                        expanse.getOptionalUniqueId())
                || !OdysseyExpanseSystem.isInside(expanse)) return 0;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player != null && (player.isInHyperspaceTransition()
                || player.getBattle() != null)) return 0;

        int removed = 0;
        for (SectorEntityToken token : new ArrayList<SectorEntityToken>(
                expanse.getJumpPoints())) {
            if (!(token instanceof JumpPointAPI)
                    || token.getContainingLocation() != expanse
                    || OdysseyExpanseSystem.ENTRY_ID.equals(token.getId())) {
                continue;
            }
            JumpPointAPI local = (JumpPointAPI) token;
            if (local.isWormhole() || local.getDestinations().size() < 2) continue;
            List<JumpDestination> exits = new ArrayList<JumpDestination>(
                    local.getDestinations());
            List<JumpPointAPI> endpoints = new ArrayList<JumpPointAPI>();
            boolean safe = true;
            for (JumpDestination exit : exits) {
                SectorEntityToken target = exit == null ? null : exit.getDestination();
                if (!(target instanceof JumpPointAPI)
                        || target.getContainingLocation() != hyperspace
                        || !hyperspace.getAllEntities().contains(target)) {
                    safe = false;
                    break;
                }
                JumpPointAPI endpoint = (JumpPointAPI) target;
                if (endpoint.isWormhole() || endpoint.getDestinations().size() != 1
                        || endpoint.getDestinations().get(0) == null
                        || endpoint.getDestinations().get(0).getDestination() != local) {
                    safe = false;
                    break;
                }
                if (!endpoints.contains(endpoint)) endpoints.add(endpoint);
            }
            if (!safe || isOpen(local)) continue;
            JumpPointAPI retained = endpoints.get(0);
            for (JumpPointAPI endpoint : endpoints) {
                if (Misc.getDistance(retained, endpoint) > MAX_DUPLICATE_DISTANCE
                        || isOpen(endpoint)
                        || (endpoint != retained
                            && (isCourseTarget(endpoint)
                                || hasOtherIncomingRoute(endpoint, local, hyperspace)))) {
                    safe = false;
                    break;
                }
            }
            if (!safe) continue;

            // Preserve the first actual destination record, including its
            // arrival-distance settings. Never clear or regenerate topology.
            for (int index = local.getDestinations().size() - 1; index > 0; index--) {
                local.getDestinations().remove(index);
            }
            for (JumpPointAPI endpoint : endpoints) {
                if (endpoint == retained) continue;
                hyperspace.removeEntity(endpoint);
                removed++;
            }
        }
        return removed;
    }

    private static boolean isOpen(SectorEntityToken token) {
        if (Global.getSector().getCampaignUI() == null) return false;
        InteractionDialogAPI dialog = Global.getSector().getCampaignUI()
                .getCurrentInteractionDialog();
        return dialog != null && dialog.getInteractionTarget() == token;
    }

    private static boolean isCourseTarget(SectorEntityToken token) {
        if (Global.getSector().getCampaignUI() != null
                && (Global.getSector().getCampaignUI().getCurrentCourseTarget() == token
                    || Global.getSector().getCampaignUI().getUltimateCourseTarget() == token)) {
            return true;
        }
        return Global.getSector().getUIData() != null
                && Global.getSector().getUIData().getCourseTarget() == token;
    }

    private static boolean hasOtherIncomingRoute(JumpPointAPI endpoint,
            JumpPointAPI local, LocationAPI hyperspace) {
        List<SectorEntityToken> jumps = new ArrayList<SectorEntityToken>(
                hyperspace.getAllEntities());
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            jumps.addAll(system.getJumpPoints());
        }
        for (SectorEntityToken token : jumps) {
            if (!(token instanceof JumpPointAPI) || token == local) continue;
            for (JumpDestination exit : ((JumpPointAPI) token).getDestinations()) {
                if (exit != null && exit.getDestination() == endpoint) return true;
            }
        }
        return false;
    }
}
