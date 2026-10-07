package chiefnavigator.campaign;

import chiefnavigator.hullmods.DomainSecurityIFFHullmod;
import chiefnavigator.quest.MenelausTrial;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.listeners.ShipRecoveryListener;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import java.util.List;

/** Menelaus's persistent command authorization for recovered Domain drones. */
public final class DomainSecurityIFFAuthorization
        implements ShipRecoveryListener {
    /** Installs the recovery hook without rewriting serialized player ships. */
    public static void install() {
        if (Global.getSector() == null) return;
        Global.getSector().getListenerManager().removeListenerOfClass(
                DomainSecurityIFFAuthorization.class);
        Global.getSector().getListenerManager().addListener(
                new DomainSecurityIFFAuthorization(), true);
    }

    /** Grants the IFF as soon as the player accepts Labor I. */
    public static void grant() {
        synchronizeAuthorization();
    }

    /**
     * Repairs older player-fleet variants that predate the permanent built-in
     * IFF hullmod. Recovery eligibility remains unchanged.
     */
    public static void synchronizeAuthorization() {
        // Compatibility facade only. Authorization is attached at the
        // explicit recovery event; never infer ownership by sweeping the
        // player's serialized roster on load or from a timer.
    }

    @Override
    public void reportShipsRecovered(
            List<FleetMemberAPI> ships, InteractionDialogAPI dialog) {
        if (!MenelausTrial.isAccepted() || ships == null) return;
        for (FleetMemberAPI member : ships) {
            addIFF(member);
        }
    }

    /** Explicit friendly-drone transfers receive the same command authority. */
    public static void authorizeTransferredDrone(FleetMemberAPI member) {
        if (MenelausTrial.isAccepted()) addIFF(member);
    }

    private static boolean isAuthorizedDerelict(FleetMemberAPI member) {
        if (member == null || member.getHullId() == null) return false;
        String hullId = member.getHullId();
        boolean chiefNavigatorRecovery = hullId.startsWith(
                "chief_navigator_combat_guard_")
                || "chief_navigator_last_light_guardian".equals(hullId);
        return chiefNavigatorRecovery
                && member.getHullSpec() != null
                && member.getHullSpec().hasTag("derelict")
                && member.getVariant() != null
                && member.getVariant().hasHullMod(HullMods.AUTOMATED);
    }

    private static void addIFF(FleetMemberAPI member) {
        if (!isAuthorizedDerelict(member)) {
            return;
        }
        ShipVariantAPI original = member.getVariant();
        if (original == null) return;
        if (original.hasHullMod(DomainSecurityIFFHullmod.HULLMOD_ID)
                && original.hasTag(Tags.TAG_AUTOMATED_NO_PENALTY)) {
            return;
        }

        // Recovery variants can originate from shared specs. Clone before
        // making the authorization permanent on this individual ship.
        ShipVariantAPI authorized = original.clone();
        authorized.addPermaMod(DomainSecurityIFFHullmod.HULLMOD_ID);
        authorized.addTag(Tags.TAG_AUTOMATED_NO_PENALTY);
        member.setVariant(authorized, false, true);
    }

    private DomainSecurityIFFAuthorization() { }
}
