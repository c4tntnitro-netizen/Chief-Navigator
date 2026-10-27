package chiefnavigator.campaign;

import chiefnavigator.quest.MenelausTrial;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.listeners.ShipRecoveryListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import java.util.List;

/** Menelaus's one-time native skill reward; retains the historical class id. */
public final class DomainSecurityIFFAuthorization
        implements ShipRecoveryListener {
    public static final String REWARD_GRANTED =
            "$chief_navigator_menelaus_automated_ships_reward_v1";

    /** Retires the old per-ship recovery authorization hook. */
    public static void install() {
        if (Global.getSector() == null) return;
        Global.getSector().getListenerManager().removeListenerOfClass(
                DomainSecurityIFFAuthorization.class);
    }

    /** Called only by Labor-I acceptance or an explicit Menelaus contact. */
    public static void grant() {
        if (!MenelausTrial.isAccepted()) return;
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        if (memory.getBoolean(REWARD_GRANTED)) return;
        MutableCharacterStatsAPI stats = Global.getSector().getPlayerStats();
        if (stats == null) return;

        if (stats.getSkillLevel(Skills.AUTOMATED_SHIPS) > 0f) {
            stats.addPoints(1);
        } else {
            // Native skill refresh handles recovery access and fleet effects.
            // Setting the level directly does not spend a skill point.
            stats.setSkillLevel(Skills.AUTOMATED_SHIPS, 1f);
        }
        memory.set(REWARD_GRANTED, true);
    }

    /** Inert compatibility entry point: no load-time or periodic rewards. */
    public static void synchronizeAuthorization() { }

    /** Old recovery callbacks must neither modify ships nor grant rewards. */
    @Override
    public void reportShipsRecovered(
            List<FleetMemberAPI> ships, InteractionDialogAPI dialog) { }

    /** Historical transfer hook, retained without per-ship authorization. */
    public static void authorizeTransferredDrone(FleetMemberAPI member) { }

    private DomainSecurityIFFAuthorization() { }
}
