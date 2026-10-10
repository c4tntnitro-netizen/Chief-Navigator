package chiefnavigator.campaign;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

/** One-time world installation, including first installs into existing saves. */
public final class CampaignWorldInitialization {
    public static final String ATTEMPTED =
            "$chief_navigator_world_initialization_attempted_v1";
    private static final String ID_PREFIX = "chief_navigator_";
    private static final String MEMORY_PREFIX = "$chief_navigator_";
    public static final String EXPLICIT_ATTEMPTED =
            "$chief_navigator_explicit_world_installation_attempted_v1";

    private CampaignWorldInitialization() { }

    /**
     * Claims initialization before factory calls, so partial construction is
     * never retried over saved state. Legacy saves are adopted without creating
     * or repairing anything, even if only part of their world remains.
     * Call before other load hooks write Chief Navigator's memory flags.
     */
    public static boolean begin(SectorAPI sector, boolean newGame) {
        if (sector == null) return false;
        MemoryAPI memory = sector.getMemoryWithoutUpdate();
        if (memory == null || memory.contains(ATTEMPTED)) return false;

        boolean existingState = hasWorldState(sector);
        if (!newGame && !existingState) {
            for (String key : memory.getKeys()) {
                if (key != null && key.startsWith(MEMORY_PREFIX)) {
                    existingState = true;
                    break;
                }
            }
        }

        // No expiry: this decision belongs to the campaign, not this session.
        memory.set(ATTEMPTED, true);
        return !existingState;
    }

    /**
     * Console-only recovery for an expedition that never acquired any worlds.
     * This is an explicit installation, never an automatic save-load repair.
     * Any surviving namespace claim or committed departure blocks it.
     */
    public static boolean beginExplicitInstallation(SectorAPI sector) {
        if (sector == null) return false;
        MemoryAPI memory = sector.getMemoryWithoutUpdate();
        if (memory == null || memory.contains(EXPLICIT_ATTEMPTED)
                || hasWorldState(sector)) return false;
        String[] progressed = {
            "$chief_navigator_treadmill_entered",
            "$chief_navigator_troy_arrival_notice_shown",
            "$chief_navigator_troy_wormhole_opened",
            "$chief_navigator_odyssey_expanse_entered",
            "$chief_navigator_menelaus_trial_accepted",
            "$chief_navigator_menelaus_final_debrief_complete_v1"
        };
        for (String key : progressed) {
            if (memory.getBoolean(key)) return false;
        }
        // Claim before calling either factory; a partial failure cannot retry.
        memory.set(EXPLICIT_ATTEMPTED, true);
        memory.set(ATTEMPTED, true);
        return true;
    }

    private static boolean hasWorldState(SectorAPI sector) {
        for (StarSystemAPI system : sector.getStarSystems()) {
            String id = system.getOptionalUniqueId();
            if (id != null && id.startsWith(ID_PREFIX)) return true;
            if (hasWorldEntities(system)) return true;
        }
        return hasWorldEntities(sector.getHyperspace());
    }

    private static boolean hasWorldEntities(LocationAPI location) {
        if (location == null) return false;
        for (SectorEntityToken entity : location.getAllEntities()) {
            String id = entity.getId();
            if (id != null && id.startsWith(ID_PREFIX)) return true;
        }
        return false;
    }
}
