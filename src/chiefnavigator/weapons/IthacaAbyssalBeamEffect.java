package chiefnavigator.weapons;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.impl.combat.dweller.AbyssalGlareEffect;
import java.awt.Color;

/** Each native beam ray owns its own Glare timing and endpoint state. */
public final class IthacaAbyssalBeamEffect extends AbyssalGlareEffect {
    private static final Color CORE = Color.WHITE;
    private static final Color FRINGE = new Color(255, 30, 45, 155);
    private AbyssalBeamGradientRenderer gradient;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (engine == null || beam == null || engine.isPaused()
                || !IthacaResearchUpgrades.hasUpgrade(
                        beam.getSource(), IthacaResearchUpgrades.ABYSSAL_LENS)) return;
        beam.setCoreColor(CORE);
        beam.setFringeColor(FRINGE);
        if (gradient == null || gradient.isExpired()) {
            if (gradient != null) gradient.cleanup();
            gradient = AbyssalBeamGradientRenderer.install(engine, beam);
        }
        if (gradient != null) gradient.advanceTexture(amount);
        super.advance(amount, engine, beam);
    }
}
