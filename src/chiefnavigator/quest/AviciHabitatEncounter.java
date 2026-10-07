package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Planets;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

/** Persistent world state for the last inhabited station in Avici. */
public final class AviciHabitatEncounter {
    public static final String WORLD_ID = "chief_navigator_avici_naraka";
    public static final String HABITAT_ID =
            "chief_navigator_avici_naraka_habitat";
    public static final String REFUGEES_RESCUED =
            "$chief_navigator_avici_naraka_refugees_rescued";
    public static final String SARA_DEAD =
            "$chief_navigator_sara_dead";
    public static final String RESCUE_OUTCOME =
            "$chief_navigator_avici_habitat_rescue_outcome";

    public static final String INHABITED = "inhabited";
    public static final String MOTHER_WAITING = "mother_waiting";
    public static final String ESCAPED_AMBUSH = "escaped_ambush";
    public static final String ORBITAL_DECISION = "orbital_decision";
    public static final String ABANDONED = "abandoned";
    public static final String EMPTY = "empty";
    public static final String BOMBED = "bombed";

    private static final String HABITAT_TYPE =
            "chief_navigator_avici_cult_habitat";
    private static final String STATE =
            "$chief_navigator_avici_naraka_habitat_state";
    private static final String IMPORTANT =
            "chief_navigator_avici_naraka_habitat_visible";

    private AviciHabitatEncounter() { }

    /** Adds the doomed world and habitat to new and existing Avici saves. */
    public static void ensureExists(StarSystemAPI system) {
        if (system == null || system.getStar() == null) return;

        PlanetAPI world = null;
        SectorEntityToken existingWorld = system.getEntityById(WORLD_ID);
        if (existingWorld != null && !(existingWorld instanceof PlanetAPI)) {
            Global.getLogger(AviciHabitatEncounter.class).error(
                    "Avici world ID contains incompatible serialized state; "
                            + "leaving it untouched");
            return;
        }
        if (existingWorld instanceof PlanetAPI) {
            world = (PlanetAPI) existingWorld;
        }
        boolean createdWorld = world == null;
        if (createdWorld) {
            world = system.addPlanet(
                    WORLD_ID,
                    system.getStar(),
                    "Kshaya",
                    Planets.BARREN_BOMBARDED,
                    32f,
                    430f,
                    4300f,
                    210f);
        }
        world.setName("Kshaya");
        world.setRadius(430f);
        world.setCustomDescriptionId(
                "chief_navigator_avici_doomed_world");
        world.setSkipForJumpPointAutoGen(true);
        world.setDiscoverable(false);
        world.setSensorProfile(1f);
        world.addTag(Tags.NOT_RANDOM_MISSION_TARGET);
        if (createdWorld || world.getOrbitFocus() != system.getStar()) {
            world.setCircularOrbit(system.getStar(), 32f, 4300f, 210f);
        }

        SectorEntityToken habitat = system.getEntityById(HABITAT_ID);
        if (habitat != null
                && !HABITAT_TYPE.equals(habitat.getCustomEntityType())) {
            Global.getLogger(AviciHabitatEncounter.class).error(
                    "Avici habitat ID contains incompatible serialized "
                            + "state; leaving it untouched");
            return;
        }
        boolean createdHabitat = habitat == null;
        if (createdHabitat) {
            habitat = system.addCustomEntity(
                    HABITAT_ID,
                    "Kshaya Habitat",
                    HABITAT_TYPE,
                    Factions.NEUTRAL);
        }
        habitat.setDiscoverable(false);
        habitat.setSensorProfile(1f);
        habitat.setInteractionImage("illustrations", "vacuum_colony");
        habitat.addTag(Tags.NOT_RANDOM_MISSION_TARGET);
        habitat.getDetectedRangeMod().modifyFlat(IMPORTANT, 8000f);
        if (createdHabitat || habitat.getOrbitFocus() != world) {
            habitat.setCircularOrbitPointingDown(world, 115f, 690f, 31f);
        }
        syncPresentation(habitat);
    }

    public static boolean isHabitat(SectorEntityToken target) {
        return target != null && HABITAT_ID.equals(target.getId())
                && HABITAT_TYPE.equals(target.getCustomEntityType());
    }

    public static boolean isStage(SectorEntityToken habitat, String stage) {
        return isHabitat(habitat) && stage != null
                && stage.equals(getStage(habitat));
    }

    public static void setStage(SectorEntityToken habitat, String stage) {
        if (!isHabitat(habitat) || !isValidStage(stage)) return;
        habitat.getMemoryWithoutUpdate().set(STATE, stage);
        syncPresentation(habitat);
    }

    public static void markRefugeesRescued() {
        if (Global.getSector() != null) {
            Global.getSector().getMemoryWithoutUpdate().set(
                    REFUGEES_RESCUED, true);
        }
    }

    /** Records Sara's death in the light-weapons rescue branch. */
    public static void markSaraDead() {
        if (Global.getSector() != null) {
            Global.getSector().getMemoryWithoutUpdate().set(
                    SARA_DEAD, true);
        }
    }

    /**
     * Migrates saves that completed Sara's fatal rescue before the global
     * continuity flag existed. Serialized habitat state remains authoritative;
     * this method only derives the additive character flag from it.
     */
    public static void reconcileSaraDeathFlag() {
        if (Global.getSector() == null
                || Global.getSector().getMemoryWithoutUpdate()
                        .getBoolean(SARA_DEAD)) {
            return;
        }
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            SectorEntityToken habitat = system.getEntityById(HABITAT_ID);
            if (!isHabitat(habitat)) continue;
            if ("light".equals(habitat.getMemoryWithoutUpdate()
                    .getString(RESCUE_OUTCOME))) {
                markSaraDead();
            }
            return;
        }
    }

    public static void syncPresentation(SectorEntityToken habitat) {
        if (!isHabitat(habitat)) return;
        String stage = getStage(habitat);
        if (BOMBED.equals(stage)) {
            habitat.setName("Ruined Kshaya Habitat");
            habitat.setFaction(Factions.DERELICT);
            habitat.setCustomDescriptionId(
                    "chief_navigator_avici_habitat_bombed");
        } else if (EMPTY.equals(stage)) {
            habitat.setName("Silent Kshaya Habitat");
            habitat.setFaction(Factions.NEUTRAL);
            habitat.setCustomDescriptionId(
                    "chief_navigator_avici_habitat_empty");
        } else {
            habitat.setName("Kshaya Habitat");
            habitat.setFaction(Factions.NEUTRAL);
            habitat.setCustomDescriptionId(
                    "chief_navigator_avici_habitat_inhabited");
        }
    }

    private static String getStage(SectorEntityToken habitat) {
        if (!isHabitat(habitat)) return INHABITED;
        MemoryAPI memory = habitat.getMemoryWithoutUpdate();
        String stage = memory.getString(STATE);
        return isValidStage(stage) ? stage : INHABITED;
    }

    private static boolean isValidStage(String stage) {
        return INHABITED.equals(stage)
                || MOTHER_WAITING.equals(stage)
                || ESCAPED_AMBUSH.equals(stage)
                || ORBITAL_DECISION.equals(stage)
                || ABANDONED.equals(stage)
                || EMPTY.equals(stage)
                || BOMBED.equals(stage);
    }
}
