package chiefnavigator.quest;

import chiefnavigator.dwellers.AviciDwellerContent;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.util.Misc;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** One-time restoration of the owned Budai encounter, never an escort refill. */
final class BudaiFleetComposition {
    static final String MAW_HULL = "chief_navigator_charybdis";
    static final String MAW_VARIANT = "chief_navigator_charybdis_Ravenous";
    static final String APPLIED = "$chief_navigator_budai_maw_sep_escort_v1";
    private static final String HUNTER = "$chief_navigator_budai_hunter";

    private BudaiFleetComposition() { }

    /** SEP DwellerTidalCMD's normal largeFleet/HIGH composition, omitting Maws.
     * The enemy-only port uses its own Banshee/Brain IDs and vanilla weapon fits.
     */
    static List<String> escortVariants(Random random) {
        List<String> variants = new ArrayList<>();
        add(variants, "shrouded_tendril_Roiling", 8 + random.nextInt(4));
        add(variants, AviciDwellerContent.BANSHEE_VARIANT, 4 + random.nextInt(3));
        int eyes = 2 + random.nextInt(3);
        add(variants, "shrouded_eye_Darkened", eyes);
        add(variants, AviciDwellerContent.BRAIN_VARIANT, 1);
        int min = Math.max(0, 3 - eyes), max = Math.max(0, 5 - eyes);
        add(variants, "shrouded_maelstrom_Menacing", min + random.nextInt(max - min + 1));
        return variants;
    }

    private static void add(List<String> variants, String variant, int count) {
        for (int i = 0; i < count; i++) variants.add(variant);
    }

    static void restore(CampaignFleetAPI fleet) {
        if (Global.getSector() == null || fleet == null || OdysseyPredatorScript.isBudaiDefeated()
                || TroyArrivalScript.isFleetBusyForMutation(fleet)
                || !TroyArrivalScript.CHARYBDIS_FLEET_ID.equals(fleet.getId())
                || fleet.getFaction() == null || !Factions.DWELLER.equals(fleet.getFaction().getId())
                || !fleet.getMemoryWithoutUpdate().getBoolean(HUNTER)
                || fleet.getMemoryWithoutUpdate().getBoolean(APPLIED)) return;
        List<FleetMemberAPI> previous = fleet.getFleetData().getMembersListCopy();
        FleetMemberAPI boss = null;
        for (FleetMemberAPI member : previous) {
            if (MAW_HULL.equals(member.getHullId())) { boss = member; break; }
        }
        if (boss == null) {
            for (FleetMemberAPI member : previous) {
                if (AviciDwellerContent.BUDAI_HULL.equals(member.getHullId())) { boss = member; break; }
            }
        }
        if (boss == null) return; // Never resurrect a dead or missing boss.
        ShipVariantAPI maw = Global.getSettings().getVariant(MAW_VARIANT);
        if (maw == null) return;
        List<String> variants = escortVariants(new Random(Misc.getSalvageSeed(fleet)));
        for (String variant : variants) {
            if (Global.getSettings().getVariant(variant) == null) return;
        }
        // Prepare the complete escort before changing any saved roster or boss member.
        List<FleetMemberAPI> escorts = new ArrayList<>();
        for (String variant : variants) {
            FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant);
            if (member == null) return;
            member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
            escorts.add(member);
        }
        if (!MAW_HULL.equals(boss.getHullId())) {
            // Native setVariant updates the same member; ID, captain, name and damage remain owned by it.
            boss.setVariant(maw.clone(), false, true);
        }
        for (FleetMemberAPI member : previous) {
            if (member != boss) fleet.getFleetData().removeFleetMember(member);
        }
        for (FleetMemberAPI member : escorts) fleet.getFleetData().addFleetMember(member);
        fleet.setInflater(null);
        if (fleet.getFlagship() != boss) fleet.getFleetData().setFlagship(boss);
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
        fleet.getMemoryWithoutUpdate().set(APPLIED, true);
    }
}
