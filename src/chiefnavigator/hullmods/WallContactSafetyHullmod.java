package chiefnavigator.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.lwjgl.util.vector.Vector2f;

/** Solid station geometry without lethal body impacts from its hard anchor. */
public final class WallContactSafetyHullmod extends BaseHullMod {
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (!ship.hasListenerOfClass(BodyImpactFilter.class)) {
            ship.addListener(new BodyImpactFilter());
        }
    }

    public static final class BodyImpactFilter implements DamageDealtModifier {
        private static final String ID = "chief_navigator_wall_body_impact";

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target,
                DamageAPI damage, Vector2f point, boolean shieldHit) {
            // The engine's ship collision handler passes the colliding ship
            // as damage source with no projectile/beam/arc context (param is
            // null). Filter this source's body damage only. Weapon hits carry
            // their own context and must continue to work, even at point blank.
            // This also makes uncontextualized structural blast damage harmless.
            if (param != null || !(target instanceof ShipAPI)) return null;
            damage.getModifier().modifyMult(ID, 0f);
            return ID;
        }
    }
}
