package chiefnavigator.dwellers;

// Adapted from TIDAL's Shroud Expansion Pack, revision e2bafc5, with permission.

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.dweller.BaseDwellerShipCreator;
import com.fs.starfarer.api.impl.combat.dweller.DwellerCombatPlugin;
import com.fs.starfarer.api.impl.combat.dweller.DwellerShipPart;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;


public class ShroudedBansheeShipCreator extends BaseDwellerShipCreator {
    public static float DAMAGE_MULT = 1.40F;
    public static float FLUX_COST_MULT = 0.15F;
    public static final String KEY = "chief_navigator_avici_banshee_live";

    public void initBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        super.initBeforeShipCreation(hullSize, stats, id);
        stats.getEnergyWeaponFluxCostMod().modifyPercent(id, FLUX_COST_MULT);
        stats.getEnergyWeaponDamageMult().modifyPercent(id, DAMAGE_MULT);
    }


    @Override
    protected DwellerCombatPlugin createPlugin(ShipAPI ship) {
        DwellerCombatPlugin plugin = super.createPlugin(ship);
        List<DwellerShipPart> parts = plugin.getParts();


        // Core body
        DwellerCombatPlugin.WobblyPart core = new DwellerCombatPlugin.WobblyPart(
                AviciDwellerContent.BANSHEE_HULL, 0.85f, 1.2f, new Vector2f(0f, 0f), 0f);
        parts.add(core);

        Color glow = DwellerCombatPlugin.STANDARD_PART_GLOW_COLOR;

        // Twin lateral eyes for a sleeker profile
        DwellerCombatPlugin.WobblyPart eyeLeft = new DwellerCombatPlugin.WobblyPart(
                "shrouded_eye_cluster1", 1.1f, 2, 2, 1.7f, new Vector2f(-55f, 18f), 0f);
        eyeLeft.color = glow;
        eyeLeft.additiveBlend = true;
        parts.add(eyeLeft);

        DwellerCombatPlugin.WobblyPart eyeRight = new DwellerCombatPlugin.WobblyPart(
                "shrouded_eye_cluster2", 0.8f, 2, 2, 1.7f, new Vector2f(55f, -18f), 0f);
        eyeRight.color = glow;
        eyeRight.additiveBlend = true;
        parts.add(eyeRight);

        // Small trailing glow to emphasize motion
        DwellerCombatPlugin.WobblyPart tail = new DwellerCombatPlugin.WobblyPart(
                "shrouded_eye_cluster3", 0.55f, 1, 2, 1.4f, new Vector2f(-90f, 0f), 0f);
        tail.color = glow;
        tail.additiveBlend = true;
        parts.add(tail);

        return plugin;
            }
        }