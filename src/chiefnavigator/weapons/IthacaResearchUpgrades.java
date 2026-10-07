package chiefnavigator.weapons;

import chiefnavigator.quest.IthacaSectionEncounter;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.combat.ShipAPI;

/** Snapshots independently installed Ithaca components per combat deployment. */
public final class IthacaResearchUpgrades {
    private static final String MASK_KEY =
            "chief_navigator_ithaca_research_component_mask";
    private static final String UNLOCKED_MASK_KEY =
            "$chief_navigator_ithaca_research_component_mask_v1";
    private static final String CONSUMABLE_MIGRATION_KEY =
            "$chief_navigator_ithaca_research_consumables_migrated_v2";
    private static final String NAVAL_MOUNT_HULL_ID =
            "chief_navigator_ithaca_naval_mount";
    private static final String SPARTAN_BATTLESTATION_HULL_ID =
            "chief_navigator_spartan_battlestation";
    private static final String MAX_OVERRIDE_KEY =
            "$chief_navigator_ithaca_research_max_override_v1";
    private static final String ISA_REFRACTION_KEY =
            "chief_navigator_isa_refraction_enabled";
    private static final String ISA_REFRACTION_UNLOCKED_KEY =
            "$chief_navigator_isa_refraction_unlocked_v1";
    private static final String HALL_ISA_OFFICER_GRANTED_KEY =
            "$ship_trophy_room_isa_officer_granted";

    public static final int MACROSERVOS = 1;
    public static final int NAVAL_CYCLER = 1 << 1;
    public static final int ARC_PROJECTOR = 1 << 2;
    public static final int INTERCEPT_MATRIX = 1 << 3;
    public static final int MUNITIONS_COMPILER = 1 << 4;
    public static final int AUTOLOADER_CORE = 1 << 5;
    public static final int ALL_UPGRADES = (1 << 6) - 1;
    public static final float NAVAL_TURN_RATE_MULTIPLIER = 4f;
    public static final float NAVAL_CYCLE_MULTIPLIER = 0.4f;
    public static final float MISSILE_CYCLE_MULTIPLIER = 0.4f;

    public static final String[] COMPONENT_IDS = {
        "chief_navigator_ithaca_macroservos",
        "chief_navigator_ithaca_naval_cycler",
        "chief_navigator_ithaca_arc_projector",
        "chief_navigator_ithaca_intercept_matrix",
        "chief_navigator_ithaca_munitions_compiler",
        "chief_navigator_ithaca_autoloader_core"
    };

    public static final String[] COMPONENT_NAMES = {
        "Colossal Traverse Macroservos",
        "Naval Cycling Regulator",
        "Caged-Arc Projector",
        "Interception Command Matrix",
        "Siege Munitions Compiler",
        "Fortress Autoloader Core"
    };

    /** Menelaus's exact recovery lead for each component's fixed station. */
    public static final String[] COMPONENT_STATION_LOCATIONS = {
        "Ossa orbit, Odyssey Expanse",
        "Aulis orbit, Odyssey Expanse",
        "Undertow orbit, Sanzu",
        "Cairn orbit, Sanzu",
        "Scamander orbit, Ashen Verge",
        "Simois orbit, Ashen Verge"
    };

    private IthacaResearchUpgrades() { }

    public static int getMask(ShipAPI ship) {
        if (ship == null || ship.getHullSpec() == null) return 0;
        if (!isEligibleIthacaHull(ship)) return 0;
        Object cached = ship.getCustomData().get(MASK_KEY);
        if (cached instanceof Integer) return (Integer) cached;
        int mask = getCampaignMask();
        ship.setCustomData(MASK_KEY, mask);
        return mask;
    }

    public static boolean hasUpgrade(ShipAPI ship, int upgrade) {
        return hasUpgrade(getMask(ship), upgrade);
    }

    public static boolean hasUpgrade(int mask, int upgrade) {
        return (mask & upgrade) != 0;
    }

    /** Snapshots Isa's permanent naval-fire-control contribution per deployment. */
    public static boolean hasIsaRefraction(ShipAPI ship) {
        if (!isEligibleIthacaHull(ship)) return false;
        Object cached = ship.getCustomData().get(ISA_REFRACTION_KEY);
        if (cached instanceof Boolean) return (Boolean) cached;
        boolean unlocked = captureIsaRefractionUnlock();
        ship.setCustomData(ISA_REFRACTION_KEY, unlocked);
        return unlocked;
    }

    /** Test-only campaign override; research POIs and Labor progress stay intact. */
    public static boolean enableMaximumUpgradeOverride() {
        if (Global.getSector() == null) return false;
        Global.getSector().getMemoryWithoutUpdate().set(
                MAX_OVERRIDE_KEY, true);
        return true;
    }

    public static boolean isMaximumUpgradeOverrideEnabled() {
        return Global.getSector() != null
                && Global.getSector().getMemoryWithoutUpdate().getBoolean(
                        MAX_OVERRIDE_KEY);
    }

    public static int getCampaignMask() {
        if (Global.getSector() == null) return 0;
        if (isMaximumUpgradeOverrideEnabled()) return ALL_UPGRADES;
        return captureRecoveredComponents();
    }

    /**
     * Compatibility entry point retained for serialized callers. Components
     * now remain inert cargo until Menelaus consumes and installs them.
     */
    public static int captureRecoveredComponents() {
        if (Global.getSector() == null) return 0;
        migrateLegacyUnlocks();
        return getStoredMask();
    }

    /** Consumes and permanently installs every recovered, uninstalled part. */
    public static int installRecoveredComponents() {
        if (Global.getSector() == null
                || Global.getSector().getPlayerFleet() == null) {
            return 0;
        }
        migrateLegacyUnlocks();
        int mask = getStoredMask();
        CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
        int installed = 0;
        for (int index = 0; index < COMPONENT_IDS.length; index++) {
            int bit = 1 << index;
            if ((mask & bit) != 0) continue;
            SpecialItemData item = new SpecialItemData(
                    COMPONENT_IDS[index], null);
            if (cargo.getQuantity(CargoAPI.CargoItemType.SPECIAL, item) < 1f) {
                continue;
            }
            if (cargo.removeItems(CargoAPI.CargoItemType.SPECIAL, item, 1f)) {
                mask |= bit;
                installed++;
            }
        }
        setStoredMask(mask);
        return installed;
    }

    public static int getInstalledCount() {
        return Integer.bitCount(getInstalledMask());
    }

    public static int getAvailableCount() {
        if (Global.getSector() == null
                || Global.getSector().getPlayerFleet() == null) {
            return 0;
        }
        int mask = getInstalledMask();
        CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
        int available = 0;
        for (int index = 0; index < COMPONENT_IDS.length; index++) {
            if ((mask & (1 << index)) != 0) continue;
            SpecialItemData item = new SpecialItemData(
                    COMPONENT_IDS[index], null);
            if (cargo.getQuantity(CargoAPI.CargoItemType.SPECIAL, item) >= 1f) {
                available++;
            }
        }
        return available;
    }

    public static boolean hasAvailableComponents() {
        return getAvailableCount() > 0;
    }

    /** Exact Menelaus-facing state for all six independently installed parts. */
    public static String getUpgradeLedger() {
        int mask = getInstalledMask();
        CargoAPI cargo = Global.getSector() == null
                || Global.getSector().getPlayerFleet() == null
                ? null : Global.getSector().getPlayerFleet().getCargo();
        StringBuilder ledger = new StringBuilder();
        for (int index = 0; index < COMPONENT_IDS.length; index++) {
            if (index > 0) ledger.append('\n');
            ledger.append(COMPONENT_NAMES[index]).append(": ");
            if ((mask & (1 << index)) != 0) {
                ledger.append("Installed");
                continue;
            }
            SpecialItemData item = new SpecialItemData(
                    COMPONENT_IDS[index], null);
            if (cargo != null && cargo.getQuantity(
                    CargoAPI.CargoItemType.SPECIAL, item) >= 1f) {
                ledger.append("Recovered - awaiting installation");
            } else {
                ledger.append("Not recovered\n  Station: ")
                        .append(COMPONENT_STATION_LOCATIONS[index]);
            }
        }
        return ledger.toString();
    }

    private static int getStoredMask() {
        Object stored = Global.getSector().getMemoryWithoutUpdate().get(
                UNLOCKED_MASK_KEY);
        return stored instanceof Number
                ? ((Number) stored).intValue() & ALL_UPGRADES : 0;
    }

    /** Actual installed state; unlike the combat mask, ignores test overrides. */
    private static int getInstalledMask() {
        if (Global.getSector() == null) return 0;
        return captureRecoveredComponents();
    }

    private static void setStoredMask(int mask) {
        Global.getSector().getMemoryWithoutUpdate().set(
                UNLOCKED_MASK_KEY, mask & ALL_UPGRADES);
    }

    /**
     * Older builds unlocked a component as soon as it touched player cargo.
     * Preserve those unlocks, but consume the corresponding physical item
     * once so an old save cannot install or sell the already-integrated part.
     */
    private static void migrateLegacyUnlocks() {
        if (Global.getSector().getMemoryWithoutUpdate().getBoolean(
                CONSUMABLE_MIGRATION_KEY)) {
            return;
        }
        int mask = getStoredMask();
        if (Global.getSector().getPlayerFleet() != null) {
            CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
            for (int index = 0; index < COMPONENT_IDS.length; index++) {
                if ((mask & (1 << index)) == 0) continue;
                SpecialItemData item = new SpecialItemData(
                        COMPONENT_IDS[index], null);
                if (cargo.getQuantity(CargoAPI.CargoItemType.SPECIAL, item)
                        >= 1f) {
                    cargo.removeItems(
                            CargoAPI.CargoItemType.SPECIAL, item, 1f);
                }
            }
        }
        setStoredMask(mask);
        Global.getSector().getMemoryWithoutUpdate().set(
                CONSUMABLE_MIGRATION_KEY, true);
    }

    /**
     * Mirrors Hall of Triumph's durable "Isa was granted" flag into this mod's
     * own save state. The local flag stays true even if Isa later leaves the
     * active roster or Hall of Triumph is subsequently disabled.
     */
    public static boolean captureIsaRefractionUnlock() {
        if (Global.getSector() == null) return false;
        if (Global.getSector().getMemoryWithoutUpdate().getBoolean(
                ISA_REFRACTION_UNLOCKED_KEY)) return true;
        if (!Global.getSector().getMemoryWithoutUpdate().getBoolean(
                HALL_ISA_OFFICER_GRANTED_KEY)) return false;
        Global.getSector().getMemoryWithoutUpdate().set(
                ISA_REFRACTION_UNLOCKED_KEY, true);
        return true;
    }

    private static boolean isEligibleIthacaHull(ShipAPI ship) {
        if (ship == null || ship.getHullSpec() == null) return false;
        String hullId = ship.getHullSpec().getHullId();
        return IthacaSectionEncounter.WALL_HULL_ID.equals(hullId)
                || NAVAL_MOUNT_HULL_ID.equals(hullId)
                || SPARTAN_BATTLESTATION_HULL_ID.equals(hullId);
    }

}
