package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;

/** First room connected to the Odyssey sector-map room. */
public final class VeiledSunRoom {
    public static final String SYSTEM_ID = "chief_navigator_veiled_sun";
    public static final String SYSTEM_JUMP_ID =
            "chief_navigator_veiled_sun_jump";
    public static final String SECTOR_WELL_ID =
            "chief_navigator_veiled_sun_gravity_well";

    private VeiledSunRoom() { }

    /**
     * Retained as a binary-compatibility facade. The room was retired, so an
     * absent serialized room is never regenerated or wired back into a save.
     */
    public static StarSystemAPI ensureExists(StarSystemAPI sectorRoom) {
        return findExisting();
    }

    public static StarSystemAPI findExisting() {
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (SYSTEM_ID.equals(system.getOptionalUniqueId())) return system;
        }
        return null;
    }

    /** Historical entry point; find-only by design. */
    public static JumpPointAPI ensureSystemJump(StarSystemAPI room) {
        return findSystemJump(room);
    }

    public static JumpPointAPI findSystemJump(StarSystemAPI room) {
        if (room == null) return null;
        SectorEntityToken entity = room.getEntityById(SYSTEM_JUMP_ID);
        return entity instanceof JumpPointAPI ? (JumpPointAPI) entity : null;
    }

    /** Historical entry point; find-only by design. */
    public static SectorEntityToken ensureSectorWell(
            StarSystemAPI sectorRoom,
            StarSystemAPI destinationRoom) {
        return findSectorWell(sectorRoom);
    }

    public static SectorEntityToken findSectorWell(StarSystemAPI sectorRoom) {
        return sectorRoom == null ? null
                : sectorRoom.getEntityById(SECTOR_WELL_ID);
    }

}
