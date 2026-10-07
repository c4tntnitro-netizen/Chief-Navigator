package data.campaign.rulecmd;

import chiefnavigator.quest.AviciHabitatEncounter;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;
import java.util.List;
import java.util.Map;

/** Rules command for the persistent Kshaya Habitat story state. */
public final class ChiefNavigatorAviciHabitatCMD
        extends BaseCommandPlugin {
    private static final String HEAVY_WEAPONS =
            "$chief_navigator_avici_habitat_heavy_weapons";
    private static final String DANGER_LEVEL =
            "$chief_navigator_avici_habitat_danger_level";
    private static final String ATE_FOOD =
            "$chief_navigator_avici_habitat_ate_food";
    private static final String RESCUED_MOTHER =
            "$chief_navigator_avici_habitat_rescued_mother";
    private static final String FIGHT_OCCURRED =
            "$chief_navigator_avici_habitat_fight_occurred";
    private static final String PLAYER_TITLE =
            "$chiefNavigatorAviciPlayerTitle";

    @Override
    public boolean execute(
            String ruleId,
            InteractionDialogAPI dialog,
            List<Token> params,
            Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || params.isEmpty()) return false;
        SectorEntityToken habitat = dialog.getInteractionTarget();
        String action = params.get(0).getString(memoryMap);

        if ("stage".equals(action) && params.size() > 1) {
            return AviciHabitatEncounter.isStage(
                    habitat, params.get(1).getString(memoryMap));
        }
        if ("stage_orbital".equals(action)) {
            return AviciHabitatEncounter.isStage(
                            habitat, AviciHabitatEncounter.ORBITAL_DECISION)
                    || AviciHabitatEncounter.isStage(
                            habitat, AviciHabitatEncounter.ESCAPED_AMBUSH);
        }
        if ("heavy".equals(action)) {
            return getBoolean(habitat, HEAVY_WEAPONS);
        }
        if ("not_heavy".equals(action)) {
            return !getBoolean(habitat, HEAVY_WEAPONS);
        }
        if ("danger_positive".equals(action)) {
            return getInt(habitat, DANGER_LEVEL) > 0;
        }
        if ("danger_zero".equals(action)) {
            return getInt(habitat, DANGER_LEVEL) == 0;
        }
        if ("rescue_option".equals(action) && params.size() > 1) {
            String option = params.get(1).getString(memoryMap);
            boolean heavy = getBoolean(habitat, HEAVY_WEAPONS);
            int danger = getInt(habitat, DANGER_LEVEL);
            if ("sneak".equals(option)) return danger == 0;
            if ("heavy".equals(option)) return heavy;
            if ("light".equals(option)) return danger > 0 && !heavy;
            return false;
        }
        if ("ending".equals(action) && params.size() > 1) {
            String ending = params.get(1).getString(memoryMap);
            boolean fight = getBoolean(habitat, FIGHT_OCCURRED);
            boolean rescued = getBoolean(habitat, RESCUED_MOTHER);
            if ("fight".equals(ending)) return fight;
            if ("rescued".equals(ending)) return rescued && !fight;
            if ("left".equals(ending)) return !rescued && !fight;
            return false;
        }
        if ("init".equals(action)) {
            MemoryAPI local = memoryMap.get(MemKeys.LOCAL);
            if (local != null) {
                local.set(PLAYER_TITLE, "Captain");
            }
            MemoryAPI habitatMemory = getHabitatMemory(habitat);
            if (habitatMemory != null
                    && AviciHabitatEncounter.isStage(
                            habitat, AviciHabitatEncounter.MOTHER_WAITING)
                    && !habitatMemory.contains(HEAVY_WEAPONS)) {
                // Historical saves reached this checkpoint with an armed
                // entry team but before the authored loadout choice existed.
                habitatMemory.set(HEAVY_WEAPONS, true);
                habitatMemory.set(DANGER_LEVEL, 1);
                habitatMemory.set(ATE_FOOD, false);
                habitatMemory.set(RESCUED_MOTHER, false);
                habitatMemory.set(FIGHT_OCCURRED, false);
            }
            if (habitatMemory != null
                    && AviciHabitatEncounter.isStage(
                            habitat, AviciHabitatEncounter.ESCAPED_AMBUSH)
                    && !habitatMemory.contains(
                            AviciHabitatEncounter.RESCUE_OUTCOME)) {
                // The retired linear encounter always reached this stage
                // after a violent, successful extraction.
                habitatMemory.set(RESCUED_MOTHER, true);
                habitatMemory.set(FIGHT_OCCURRED, true);
                habitatMemory.set(
                        AviciHabitatEncounter.RESCUE_OUTCOME, "legacy");
            }
            return true;
        }
        if ("boarding".equals(action) && params.size() > 1) {
            MemoryAPI habitatMemory = getHabitatMemory(habitat);
            if (habitatMemory == null) return false;
            boolean heavy = "heavy".equals(
                    params.get(1).getString(memoryMap));
            habitatMemory.set(HEAVY_WEAPONS, heavy);
            habitatMemory.set(DANGER_LEVEL, heavy ? 1 : 0);
            habitatMemory.set(ATE_FOOD, false);
            habitatMemory.set(RESCUED_MOTHER, false);
            habitatMemory.set(FIGHT_OCCURRED, false);
            habitatMemory.unset(AviciHabitatEncounter.RESCUE_OUTCOME);
            return true;
        }
        if ("eat_food".equals(action)) {
            MemoryAPI habitatMemory = getHabitatMemory(habitat);
            if (habitatMemory == null) return false;
            habitatMemory.set(ATE_FOOD, true);
            habitatMemory.set(
                    DANGER_LEVEL, getInt(habitat, DANGER_LEVEL) + 1);
            return true;
        }
        if ("meet_mother".equals(action)) {
            AviciHabitatEncounter.setStage(
                    habitat, AviciHabitatEncounter.MOTHER_WAITING);
            return true;
        }
        if ("rescue".equals(action) && params.size() > 1) {
            MemoryAPI habitatMemory = getHabitatMemory(habitat);
            if (habitatMemory == null) return false;
            String outcome = params.get(1).getString(memoryMap);
            boolean rescued = !"leave".equals(outcome);
            boolean fight = "heavy".equals(outcome)
                    || "light".equals(outcome);
            habitatMemory.set(RESCUED_MOTHER, rescued);
            habitatMemory.set(FIGHT_OCCURRED, fight);
            habitatMemory.set(AviciHabitatEncounter.RESCUE_OUTCOME, outcome);
            AviciHabitatEncounter.setStage(
                    habitat, AviciHabitatEncounter.ORBITAL_DECISION);
            if (rescued) {
                AviciHabitatEncounter.markRefugeesRescued();
            }
            if ("light".equals(outcome)) {
                AviciHabitatEncounter.markSaraDead();
            }
            return true;
        }
        if ("escape".equals(action)) {
            MemoryAPI habitatMemory = getHabitatMemory(habitat);
            if (habitatMemory != null) {
                habitatMemory.set(RESCUED_MOTHER, true);
                habitatMemory.set(FIGHT_OCCURRED, true);
                habitatMemory.set(
                        AviciHabitatEncounter.RESCUE_OUTCOME, "legacy");
            }
            AviciHabitatEncounter.markRefugeesRescued();
            AviciHabitatEncounter.setStage(
                    habitat, AviciHabitatEncounter.ESCAPED_AMBUSH);
            return true;
        }
        if ("bombard".equals(action)) {
            AviciHabitatEncounter.setStage(
                    habitat, AviciHabitatEncounter.BOMBED);
            return true;
        }
        if ("withdraw".equals(action)) {
            AviciHabitatEncounter.setStage(
                    habitat, AviciHabitatEncounter.ABANDONED);
            return true;
        }
        if ("mark_empty".equals(action)
                && AviciHabitatEncounter.isStage(
                        habitat, AviciHabitatEncounter.ABANDONED)) {
            AviciHabitatEncounter.setStage(
                    habitat, AviciHabitatEncounter.EMPTY);
            return true;
        }
        return false;
    }

    private static MemoryAPI getHabitatMemory(
            SectorEntityToken habitat) {
        if (!AviciHabitatEncounter.isHabitat(habitat)) return null;
        return habitat.getMemoryWithoutUpdate();
    }

    private static boolean getBoolean(
            SectorEntityToken habitat, String key) {
        MemoryAPI memory = getHabitatMemory(habitat);
        return memory != null && memory.getBoolean(key);
    }

    private static int getInt(
            SectorEntityToken habitat, String key) {
        MemoryAPI memory = getHabitatMemory(habitat);
        return memory == null ? 0 : memory.getInt(key);
    }
}
