package chiefnavigator.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.EnumSet;

/** Converts a hull impact into a visible boarding craft that drains combat readiness. */
public final class BoardingPodOnHitEffect implements OnHitEffectPlugin {
    private static final float CR_DRAIN_PER_SECOND = 0.01f;
    private static final float DRAIN_DURATION = 10f;
    private static final float POD_SPRITE_WIDTH = 36f;
    private static final float POD_SPRITE_HEIGHT = 44f;
    private static final float POD_CENTER_X = 18f;
    private static final float POD_CENTER_Y = 18f;

    @Override
    public void onHit(
            DamagingProjectileAPI projectile,
            CombatEntityAPI target,
            Vector2f point,
            boolean shieldHit,
            ApplyDamageResultAPI damageResult,
            CombatEngineAPI engine) {
        if (shieldHit || !(target instanceof ShipAPI)) return;
        ShipAPI ship = (ShipAPI) target;
        if (!ship.isAlive() || ship.isHulk() || ship.isFighter()
                || ship.getOwner() == projectile.getOwner()) return;

        attachBoardingPod(engine, ship, point, projectile.getFacing());
    }

    /** Creates the attached craft and its ten-second CR drain. */
    public static void attachBoardingPod(
            CombatEngineAPI engine,
            ShipAPI ship,
            Vector2f point,
            float podFacing) {
        if (engine == null || ship == null || point == null
                || !ship.isAlive() || ship.isHulk() || ship.isFighter()) return;

        engine.addLayeredRenderingPlugin(new AttachedBoardingPod(
                engine, ship, point, podFacing));
        engine.addFloatingText(
                point,
                "Boarded!",
                22f,
                new Color(255, 150, 70),
                ship,
                0.35f,
                1.1f);
        engine.addHitParticle(
                point,
                new Vector2f(ship.getVelocity()),
                42f,
                1f,
                0.25f,
                new Color(255, 175, 90));
    }

    private static final class AttachedBoardingPod
            extends BaseCombatLayeredRenderingPlugin {
        private final CombatEngineAPI engine;
        private final ShipAPI target;
        private final float localBearing;
        private final float localFacing;
        private final float distanceFromCenter;
        private final SpriteAPI sprite;
        private float elapsed;

        private AttachedBoardingPod(
                CombatEngineAPI engine,
                ShipAPI target,
                Vector2f impact,
                float projectileFacing) {
            this.engine = engine;
            this.target = target;
            localBearing = Misc.getAngleInDegrees(target.getLocation(), impact)
                    - target.getFacing();
            localFacing = projectileFacing - target.getFacing();
            distanceFromCenter = Misc.getDistance(target.getLocation(), impact);
            sprite = Global.getSettings().getSprite("graphics/ships/shuttle.png");
            // Match chief_navigator_boarding_pod_fighter.ship exactly. The
            // source art is 18x22 and uses the vanilla shuttle's 9,9 pivot;
            // making this square or centering it at 18,22 visibly shifts the
            // attached representation relative to the deployed fighter.
            sprite.setSize(POD_SPRITE_WIDTH, POD_SPRITE_HEIGHT);
            sprite.setCenter(POD_CENTER_X, POD_CENTER_Y);
        }

        @Override
        public void init(CombatEntityAPI entity) {
            super.init(entity);
            updateLocation();
        }

        @Override
        public void advance(float amount) {
            if (engine.isPaused() || isExpired()) return;
            float activeAmount = Math.min(amount, DRAIN_DURATION - elapsed);
            elapsed += amount;
            updateLocation();
            target.setCurrentCR(Math.max(
                    0f, target.getCurrentCR() - CR_DRAIN_PER_SECOND * activeAmount));
            if (target == engine.getPlayerShip()) {
                engine.maintainStatusForPlayerShip(
                        "chief_navigator_boarding_pods",
                        "graphics/icons/hullsys/flare_launcher.png",
                        "BOARDING PODS ATTACHED",
                        "Combat readiness draining",
                        true);
            }
        }

        private void updateLocation() {
            if (entity == null || target == null) return;
            Vector2f direction = Misc.getUnitVectorAtDegreeAngle(
                    target.getFacing() + localBearing);
            entity.getLocation().set(
                    target.getLocation().x + direction.x * distanceFromCenter,
                    target.getLocation().y + direction.y * distanceFromCenter);
        }

        @Override
        public boolean isExpired() {
            return elapsed >= DRAIN_DURATION
                    || target == null
                    || !target.isAlive()
                    || target.isHulk()
                    || !engine.isEntityInPlay(target);
        }

        @Override
        public float getRenderRadius() {
            return 80f;
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER);
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (layer != CombatEngineLayers.ABOVE_SHIPS_LAYER || entity == null) return;
            sprite.setNormalBlend();
            sprite.setAlphaMult(viewport.getAlphaMult());
            // Shuttle art points to the top of its PNG; direct SpriteAPI angles
            // need -90 degrees for Starsector's zero-degree eastward facing.
            sprite.setAngle(target.getFacing() + localFacing - 90f);
            sprite.renderAtCenter(entity.getLocation().x, entity.getLocation().y);
        }
    }
}
