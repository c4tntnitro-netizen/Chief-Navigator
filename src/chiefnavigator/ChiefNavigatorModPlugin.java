package chiefnavigator;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

/**
 * Loadable entry point for the Chief Navigator mod.
 *
 * Quest and world-generation behavior will be added only after the inherited
 * Ithaca concept has been translated into an explicit technical design.
 */
public final class ChiefNavigatorModPlugin extends BaseModPlugin {
    public static final String HYPERSPACE_ODYSSEY_ABILITY = "chief_navigator_hyperspace_odyssey";

    @Override
    public void onGameLoad(boolean newGame) {
        // Prototype access: Sinni will grant this during the quest once her
        // character and recruitment flow exist.
        if (!Global.getSector().getPlayerStats().getGrantedAbilityIds()
                .contains(HYPERSPACE_ODYSSEY_ABILITY)) {
            Global.getSector().getCharacterData().getMemoryWithoutUpdate()
                    .set("$ability:" + HYPERSPACE_ODYSSEY_ABILITY, true, 0f);
            Global.getSector().getCharacterData().addAbility(HYPERSPACE_ODYSSEY_ABILITY);
        }
        if (Global.getSector().getPlayerFleet().getAbility(HYPERSPACE_ODYSSEY_ABILITY) == null) {
            Global.getSector().getPlayerFleet().addAbility(HYPERSPACE_ODYSSEY_ABILITY);
        }
    }
}
