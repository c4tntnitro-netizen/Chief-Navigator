package chiefnavigator.systems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.dweller.ConvulsiveLungeSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.State;
import org.lwjgl.util.vector.Vector2f;

/** Vanilla Convulsive Lunge with an optional encounter-authored destination. */
public final class BudaiConvulsiveLungeSystem
        extends ConvulsiveLungeSystemScript {
    public static final String TARGET_KEY =
            "chief_navigator_budai_forced_lunge_target";

    @Override
    public void apply(
            MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (state == State.IN && dest == null
                && stats != null && stats.getEntity() instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) stats.getEntity();
            Object target = ship.getCustomData().get(TARGET_KEY);
            if (target instanceof Vector2f) {
                // The stock script normalizes every AI target to its fixed
                // 1,000-unit pull. Supplying its protected destination here
                // retains the vanilla spring, particles, and sounds while
                // allowing Budai's scripted convulsions to vary in distance.
                dest = new Vector2f((Vector2f) target);
            }
        }
        super.apply(stats, id, state, effectLevel);
    }
}
