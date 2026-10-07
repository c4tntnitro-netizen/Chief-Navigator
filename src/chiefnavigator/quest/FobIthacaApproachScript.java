package chiefnavigator.quest;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.util.Misc;

/** Opens the first-contact scene on the final approach to FOB Ithaca. */
public final class FobIthacaApproachScript implements EveryFrameScript {
    public static final String COMPLETE =
            "$chief_navigator_fob_ithaca_approach_complete_v1";
    static final String MENELAUS_INTRO_SHOWN =
            "$chief_navigator_menelaus_intro_shown_v2";
    public static final float APPROACH_TRIGGER_RADIUS = 3000f;

    @Override
    public void advance(float amount) {
        if (Global.getSector() == null || isCompleteOrSuperseded()) return;

        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        SectorEntityToken ithaca = OdysseyExpanseSystem.getFobIthaca();
        if (!isEligiblePlayer(player)
                || ithaca == null
                || player.getContainingLocation() != ithaca.getContainingLocation()
                || !(player.getContainingLocation() instanceof StarSystemAPI)
                || player.getContainingLocation()
                        != OdysseyExpanseSystem.findSystemById(
                                OdysseyExpanseSystem.ASHEN_VERGE_ID)
                || player.isInHyperspaceTransition()
                || player.getBattle() != null
                || Global.getSector().getCampaignUI()
                        .getCurrentInteractionDialog() != null
                || Global.getSector().getCampaignUI().isShowingMenu()
                || Misc.getDistance(player, ithaca)
                        > APPROACH_TRIGGER_RADIUS) {
            return;
        }

        Global.getSector().getCampaignUI().showInteractionDialog(
                new FobIthacaApproachInteraction(), player);
    }

    /** Gives this scene priority over other automatic dialogue at Ithaca. */
    static boolean shouldTakeDialoguePriority(CampaignFleetAPI player) {
        if (Global.getSector() == null
                || isCompleteOrSuperseded()
                || !isEligiblePlayer(player)) {
            return false;
        }
        SectorEntityToken ithaca = OdysseyExpanseSystem.getFobIthaca();
        return ithaca != null
                && player.getContainingLocation() == ithaca.getContainingLocation()
                && Misc.getDistance(player, ithaca)
                        <= APPROACH_TRIGGER_RADIUS;
    }

    /** Last-resort router gate if a target is selected on the approach. */
    static boolean shouldInterceptInteraction(SectorEntityToken target) {
        if (target == null
                || isCompleteOrSuperseded()
                || !isEligiblePlayer(Global.getSector() == null
                        ? null : Global.getSector().getPlayerFleet())) {
            return false;
        }
        return OdysseyExpanseSystem.isFobIthaca(target)
                || IthacaSectionEncounter.isSectionInteractionTarget(target)
                || OdysseyExpanseSystem.isFobIthacaGate(target)
                || OdysseyExpanseSystem
                        .isOwnedFobIthacaGateDefenseStation(target)
                || target instanceof CampaignFleetAPI
                        && OdysseyExpanseSystem
                                .isOwnedFobIthacaSpartanGuard(
                                        (CampaignFleetAPI) target);
    }

    /** Runs Menelaus's full introduction once from Ithaca itself. */
    static boolean shouldOpenStationIntroduction(SectorEntityToken target) {
        return Global.getSector() != null
                && target != null
                && OdysseyExpanseSystem.isFobIthaca(target)
                && isComplete()
                && !isMenelausIntroShown()
                && !MenelausTrial.isAccepted();
    }

    static boolean isComplete() {
        return Global.getSector() != null
                && Global.getSector().getMemoryWithoutUpdate().getBoolean(
                        COMPLETE);
    }

    static void markComplete() {
        if (Global.getSector() == null) return;
        Global.getSector().getMemoryWithoutUpdate().set(COMPLETE, true);
    }

    private static boolean isCompleteOrSuperseded() {
        return isComplete() || isMenelausIntroShown()
                || MenelausTrial.isAccepted();
    }

    private static boolean isMenelausIntroShown() {
        return Global.getSector() != null
                && Global.getSector().getMemoryWithoutUpdate().getBoolean(
                        MENELAUS_INTRO_SHOWN);
    }

    private static boolean isEligiblePlayer(CampaignFleetAPI player) {
        if (player == null || Global.getSector() == null) {
            return false;
        }
        return SinniSystemVignetteScript.isSinniInFleet(player);
    }

    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return false; }
}
