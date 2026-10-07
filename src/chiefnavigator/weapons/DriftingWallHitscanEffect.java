package chiefnavigator.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.EnumSet;

/**
 * Resolves a 7,500-unit naval shot immediately, then telegraphs that solution
 * for one second before the custom impulse slug reaches it and deals damage.
 */
public final class DriftingWallHitscanEffect implements OnFireEffectPlugin {
    private static final float RANGE = 7500f;
    private static final float DAMAGE = 10000f;
    private static final float IMPULSE_DURATION = 1f;
    private static final float PROJECTILE_WIDTH = 360f;
    private static final float PROJECTILE_LENGTH = 540f;
    private static final String PROJECTILE_SPRITE =
            "graphics/weapons/chief_navigator_drifting_wall_impulse_slug_v1.png";
    private static final float SOUND_FULL_VOLUME_DISTANCE = 1200f;
    private static final float SOUND_MAX_DISTANCE = 20000f;
    private static final String LOAD_SOUND_ID = "chief_navigator_naval_gun_load";
    private static final String FIRE_SOUND_ID = "chief_navigator_naval_gun_fire";

    @Override
    public void onFire(
            DamagingProjectileAPI projectile,
            WeaponAPI weapon,
            CombatEngineAPI engine) {
        Vector2f start = new Vector2f(projectile.getLocation());
        Vector2f direction = Misc.getUnitVectorAtDegreeAngle(projectile.getFacing());
        Vector2f maximumEnd = new Vector2f(
                start.x + direction.x * RANGE,
                start.y + direction.y * RANGE);
        ShipAPI source = projectile.getSource();
        HitResult hit = source == null
                ? null : findFirstHit(engine, source, start, direction);

        // The engine projectile is only an AI/firing carrier.  The rendering
        // plugin below owns the visible slug and the delayed impact.
        projectile.setDamageAmount(0f);
        engine.removeEntity(projectile);
        engine.addLayeredRenderingPlugin(new DelayedLinearImpulse(
                engine,
                source,
                start,
                hit == null ? maximumEnd : hit.point,
                hit));
        playDistanceScaledCannonSounds(start);
    }

    private HitResult findFirstHit(
            CombatEngineAPI engine,
            ShipAPI source,
            Vector2f start,
            Vector2f direction) {
        HitResult closest = null;
        for (ShipAPI candidate : engine.getShips()) {
            if (candidate == null || candidate == source
                    || !candidate.isAlive() || candidate.isHulk()
                    || candidate.isPhased()
                    || candidate.getOwner() == source.getOwner()
                    || candidate.getOwner() == 100
                    || candidate.getCollisionClass() == CollisionClass.NONE) {
                continue;
            }

            HitResult shield = findShieldHit(candidate, start, direction);
            RayCircleInterval hullInterval = intersectCircle(
                    start,
                    direction,
                    candidate.getLocation(),
                    candidate.getCollisionRadius());
            HitResult hull = hullInterval == null
                    ? null
                    : findHullHit(candidate, start, direction, hullInterval);
            HitResult candidateHit = nearer(shield, hull);
            if (candidateHit != null
                    && (closest == null || candidateHit.distance < closest.distance)) {
                closest = candidateHit;
            }
        }
        return closest;
    }

    private HitResult findShieldHit(
            ShipAPI target,
            Vector2f start,
            Vector2f direction) {
        ShieldAPI shield = target.getShield();
        if (shield == null || !shield.isOn()) return null;
        RayCircleInterval interval = intersectCircle(
                start, direction, shield.getLocation(), shield.getRadius());
        if (interval == null || interval.entry > RANGE) return null;
        Vector2f point = pointAlong(start, direction, interval.entry);
        if (!shield.isWithinArc(point)) return null;
        return new HitResult(target, point, interval.entry, true);
    }

    private HitResult findHullHit(
            ShipAPI target,
            Vector2f start,
            Vector2f direction,
            RayCircleInterval interval) {
        float from = Math.max(0f, interval.entry - 24f);
        float to = Math.min(RANGE, interval.exit + 24f);
        float step = Math.max(
                8f,
                Math.min(32f, target.getCollisionRadius() / 24f));
        Vector2f previous = pointAlong(start, direction, from);
        boolean previousInside = target.isPointInBounds(previous);
        if (previousInside) return new HitResult(target, previous, from, false);

        for (float distance = from + step; distance <= to + step; distance += step) {
            float clamped = Math.min(distance, to);
            Vector2f point = pointAlong(start, direction, clamped);
            boolean inside = target.isPointInBounds(point);
            if (inside && !previousInside) {
                float low = Math.max(from, clamped - step);
                float high = clamped;
                for (int i = 0; i < 8; i++) {
                    float middle = (low + high) * 0.5f;
                    if (target.isPointInBounds(pointAlong(start, direction, middle))) {
                        high = middle;
                    } else {
                        low = middle;
                    }
                }
                return new HitResult(
                        target, pointAlong(start, direction, high), high, false);
            }
            if (clamped >= to) break;
            previousInside = inside;
        }
        return null;
    }

    private static RayCircleInterval intersectCircle(
            Vector2f start,
            Vector2f direction,
            Vector2f center,
            float radius) {
        float mx = center.x - start.x;
        float my = center.y - start.y;
        float projection = mx * direction.x + my * direction.y;
        float perpendicularSq = mx * mx + my * my - projection * projection;
        float radiusSq = radius * radius;
        if (perpendicularSq > radiusSq) return null;
        float halfChord = (float) Math.sqrt(Math.max(0f, radiusSq - perpendicularSq));
        float entry = projection - halfChord;
        float exit = projection + halfChord;
        if (exit < 0f || entry > RANGE) return null;
        return new RayCircleInterval(Math.max(0f, entry), Math.min(RANGE, exit));
    }

    private static Vector2f pointAlong(
            Vector2f start,
            Vector2f direction,
            float distance) {
        return new Vector2f(
                start.x + direction.x * distance,
                start.y + direction.y * distance);
    }

    private static HitResult nearer(HitResult first, HitResult second) {
        if (first == null) return second;
        if (second == null) return first;
        return first.distance <= second.distance ? first : second;
    }

    private static void playDistanceScaledCannonSounds(Vector2f sourceLocation) {
        Vector2f listener = Global.getSoundPlayer().getListenerPos();
        if (listener == null) return;
        float dx = sourceLocation.x - listener.x;
        float dy = sourceLocation.y - listener.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        if (distance >= SOUND_MAX_DISTANCE) return;
        float progress = Math.max(0f, Math.min(
                1f,
                (distance - SOUND_FULL_VOLUME_DISTANCE)
                        / (SOUND_MAX_DISTANCE - SOUND_FULL_VOLUME_DISTANCE)));
        float smoothProgress = progress * progress * (3f - 2f * progress);
        float volume = 1f - smoothProgress;
        Global.getSoundPlayer().playUISound(LOAD_SOUND_ID, 1f, volume * 0.42f);
        Global.getSoundPlayer().playUISound(FIRE_SOUND_ID, 1f, volume);
    }

    private static final class HitResult {
        private final ShipAPI target;
        private final Vector2f point;
        private final float distance;
        private final boolean shieldHit;

        private HitResult(
                ShipAPI target,
                Vector2f point,
                float distance,
                boolean shieldHit) {
            this.target = target;
            this.point = point;
            this.distance = distance;
            this.shieldHit = shieldHit;
        }
    }

    private static final class RayCircleInterval {
        private final float entry;
        private final float exit;

        private RayCircleInterval(float entry, float exit) {
            this.entry = entry;
            this.exit = exit;
        }
    }

    private static final class DelayedLinearImpulse
            extends BaseCombatLayeredRenderingPlugin {
        private final CombatEngineAPI engine;
        private final ShipAPI source;
        private final Vector2f start;
        private final Vector2f fixedEnd;
        private final ShipAPI target;
        private final boolean shieldHit;
        private final Vector2f localImpactOffset;
        private final float targetFacingAtFire;
        private final SpriteAPI projectileSprite;
        private float elapsed;
        private boolean impactApplied;

        private DelayedLinearImpulse(
                CombatEngineAPI engine,
                ShipAPI source,
                Vector2f start,
                Vector2f end,
                HitResult hit) {
            this.engine = engine;
            this.source = source;
            this.start = new Vector2f(start);
            this.fixedEnd = new Vector2f(end);
            this.target = hit == null ? null : hit.target;
            this.shieldHit = hit != null && hit.shieldHit;
            if (target == null) {
                localImpactOffset = null;
                targetFacingAtFire = 0f;
            } else {
                localImpactOffset = Vector2f.sub(
                        hit.point, target.getLocation(), new Vector2f());
                targetFacingAtFire = target.getFacing();
            }
            projectileSprite = Global.getSettings().getSprite(PROJECTILE_SPRITE);
        }

        @Override
        public void init(CombatEntityAPI entity) {
            super.init(entity);
            if (this.entity != null) {
                this.entity.getLocation().set(
                        (start.x + fixedEnd.x) * 0.5f,
                        (start.y + fixedEnd.y) * 0.5f);
            }
        }

        @Override
        public void advance(float amount) {
            if (engine.isPaused() || impactApplied) return;
            elapsed += amount;
            if (elapsed < IMPULSE_DURATION) return;
            impactApplied = true;
            applyImpact(getCurrentEnd());
        }

        private void applyImpact(Vector2f point) {
            if (target == null
                    || !engine.isEntityInPlay(target)
                    || target.isExpired()
                    || !target.isAlive()) {
                return;
            }
            // A ray that already reached armor cannot be retroactively caught
            // by a shield raised during the visible one-second impulse.
            engine.applyDamage(
                    target,
                    point,
                    DAMAGE,
                    DamageType.KINETIC,
                    0f,
                    false,
                    !shieldHit,
                    source);
            Vector2f velocity = new Vector2f(target.getVelocity());
            engine.addHitParticle(
                    point,
                    velocity,
                    520f,
                    1f,
                    0.48f,
                    new Color(255, 211, 92));
            engine.addSmoothParticle(
                    point,
                    velocity,
                    760f,
                    1f,
                    0.22f,
                    new Color(255, 250, 220));
        }

        private Vector2f getCurrentEnd() {
            if (target == null
                    || !engine.isEntityInPlay(target)
                    || target.isExpired()) {
                return new Vector2f(fixedEnd);
            }
            float angle = (float) Math.toRadians(target.getFacing() - targetFacingAtFire);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            float rotatedX = localImpactOffset.x * cos - localImpactOffset.y * sin;
            float rotatedY = localImpactOffset.x * sin + localImpactOffset.y * cos;
            return new Vector2f(
                    target.getLocation().x + rotatedX,
                    target.getLocation().y + rotatedY);
        }

        @Override
        public boolean isExpired() {
            return impactApplied;
        }

        @Override
        public float getRenderRadius() {
            return Misc.getDistance(start, fixedEnd) * 0.5f + PROJECTILE_LENGTH;
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (impactApplied) return;
            float progress = Math.min(1f, elapsed / IMPULSE_DURATION);
            float appear = Math.min(1f, elapsed / 0.055f);
            float endFade = progress <= 0.92f
                    ? 1f : Math.max(0f, (1f - progress) / 0.08f);
            float pulse = 0.88f + 0.12f
                    * (float) Math.sin(elapsed * Math.PI * 18f);
            float alpha = appear * endFade * pulse;
            Vector2f end = getCurrentEnd();

            renderLine(end, alpha);
            renderProjectile(end, progress, alpha);
        }

        private void renderLine(Vector2f end, float alpha) {
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

            GL11.glColor4f(1f, 0.36f, 0.025f, 0.18f * alpha);
            drawRibbon(end, 150f);
            GL11.glColor4f(1f, 0.73f, 0.18f, 0.74f * alpha);
            drawRibbon(end, 54f);
            GL11.glColor4f(1f, 1f, 0.90f, alpha);
            drawRibbon(end, 12f);
            GL11.glPopAttrib();
        }

        private void renderProjectile(Vector2f end, float progress, float alpha) {
            float x = start.x + (end.x - start.x) * progress;
            float y = start.y + (end.y - start.y) * progress;
            float angle = Misc.getAngleInDegrees(start, end) - 90f;

            projectileSprite.setSize(PROJECTILE_WIDTH, PROJECTILE_LENGTH);
            projectileSprite.setAngle(angle);
            projectileSprite.setColor(Color.WHITE);
            projectileSprite.setAlphaMult(alpha);
            projectileSprite.setNormalBlend();
            projectileSprite.renderAtCenter(x, y);

            projectileSprite.setSize(
                    PROJECTILE_WIDTH * 1.12f,
                    PROJECTILE_LENGTH * 1.12f);
            projectileSprite.setAlphaMult(0.24f * alpha);
            projectileSprite.setAdditiveBlend();
            projectileSprite.renderAtCenter(x, y);
        }

        private void drawRibbon(Vector2f end, float width) {
            float dx = end.x - start.x;
            float dy = end.y - start.y;
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            if (length <= 0.01f) return;
            float nx = -dy / length * width * 0.5f;
            float ny = dx / length * width * 0.5f;
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(start.x + nx, start.y + ny);
            GL11.glVertex2f(start.x - nx, start.y - ny);
            GL11.glVertex2f(end.x - nx, end.y - ny);
            GL11.glVertex2f(end.x + nx, end.y + ny);
            GL11.glEnd();
        }
    }
}
