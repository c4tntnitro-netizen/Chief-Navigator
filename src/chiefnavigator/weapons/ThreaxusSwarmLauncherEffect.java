package chiefnavigator.weapons;

import chiefnavigator.hullmods.ThreaxusWallSwarm;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPluginWithAdvanceAfter;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponEffectPluginWithInit;
import com.fs.starfarer.api.impl.combat.threat.SwarmLauncherEffect;
import org.dark.shaders.util.ShaderLib;

/** Preserve native swarm gates without advertising healthy waiting as damage. */
public final class ThreaxusSwarmLauncherEffect extends SwarmLauncherEffect
        implements EveryFrameWeaponEffectPluginWithAdvanceAfter, WeaponEffectPluginWithInit {
    public static final String WEAPON_ID = "chief_navigator_threaxus_swarm_launcher";

    @Override
    public void init(WeaponAPI weapon) {
        // Reuse the stock launcher's material, normal and surface maps too.
        ShaderLib.overrideWeaponTexture(weapon, "swarm_launcher");
    }

    @Override
    public void advanceAfter(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (weapon.getShip() == null
                || !ThreaxusWallSwarm.HULL_ID.equals(weapon.getShip().getHullSpec().getHullId())
                || !weapon.isForceDisabled()) return;
        // Native advance has already blocked this frame's firing and cancelled
        // charging/bursts. Keep next frame blocked until native advance decides
        // it is ready again. Clearing only the scripted flag leaves real weapon
        // damage disabled, while GraphicsLib stops treating wing-cap waiting
        // as a damaged mount and emitting smoke from a fully healthy launcher.
        weapon.setForceNoFireOneFrame(true);
        weapon.setForceFireOneFrame(false);
        weapon.setForceDisabled(false);
    }
}
