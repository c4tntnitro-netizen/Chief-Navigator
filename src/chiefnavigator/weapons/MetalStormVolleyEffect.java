package chiefnavigator.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.GuidedMissileAI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.util.vector.Vector2f;

import java.util.EnumSet;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

/** Fires a fifteen-missile volley from a shuffled twenty-cell physical rack. */
public final class MetalStormVolleyEffect
        implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin {
    private static final String MISSILE_SPEC_ID = "chief_navigator_metalstorm_missile";
    private static final int COLUMNS = 4;
    private static final int ROWS = 5;
    private static final int CELL_COUNT = COLUMNS * ROWS;
    private static final float WEAPON_SPRITE_SIZE = 640f;
    private static final float COLUMN_SPACING = 49f;
    private static final float ROW_SPACING = 42f;
    private static final float GRID_CENTER_OFFSET = 80f;
    private static final Map<WeaponAPI, VlsState> STATES =
            new WeakHashMap<WeaponAPI, VlsState>();

    @Override
    public void onFire(
            DamagingProjectileAPI trigger,
            WeaponAPI weapon,
            CombatEngineAPI engine) {
        ShipAPI source = weapon.getShip();
        float facing = weapon.getCurrAngle();
        Vector2f gridCenter = getGridCenter(weapon, facing);
        Vector2f sourceVelocity = source == null
                ? new Vector2f()
                : new Vector2f(source.getVelocity());

        VlsState state = getState(engine, weapon);
        int launchIndex = Math.min(state.nextLaunchIndex, CELL_COUNT - 1);
        int cell = state.launchOrder[launchIndex];
        state.nextLaunchIndex = Math.min(
                CELL_COUNT, state.nextLaunchIndex + 1);
        state.reloading = true;
        engine.removeEntity(trigger);

        Vector2f location = getCellLocation(gridCenter, facing, cell);
        CombatEntityAPI spawned = engine.spawnProjectile(
                source,
                weapon,
                weapon.getId(),
                MISSILE_SPEC_ID,
                location,
                facing,
                sourceVelocity);
        if (spawned instanceof MissileAPI) {
            MissileAPI missile = (MissileAPI) spawned;
            missile.setMissileAI(new VerticalLaunchMissileAI(
                    engine,
                    missile,
                    source == null ? null : source.getShipTarget()));
        }
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        SpriteAPI weaponSprite = weapon.getSprite();
        if (weaponSprite != null) {
            weaponSprite.setSize(WEAPON_SPRITE_SIZE, WEAPON_SPRITE_SIZE);
        }
        VlsState state = getState(engine, weapon);
        boolean reloading = weapon.getCooldownRemaining() > 0.01f
                || weapon.getBurstFireTimeRemaining() > 0.01f;
        if (state.reloading && !reloading) {
            state.resetForNextVolley();
        }
        state.reloading = reloading;
    }

    private static VlsState getState(CombatEngineAPI engine, WeaponAPI weapon) {
        VlsState state = STATES.get(weapon);
        if (state == null) {
            state = new VlsState(weapon);
            STATES.put(weapon, state);
            engine.addLayeredRenderingPlugin(new LoadedCapRenderer(state));
        }
        return state;
    }

    private static Vector2f getGridCenter(WeaponAPI weapon, float facing) {
        float radians = (float) Math.toRadians(facing);
        Vector2f mount = weapon.getLocation();
        return new Vector2f(
                mount.x + (float) Math.cos(radians) * GRID_CENTER_OFFSET,
                mount.y + (float) Math.sin(radians) * GRID_CENTER_OFFSET);
    }

    private static Vector2f getCellLocation(Vector2f gridCenter, float facing, int cell) {
        int row = cell / COLUMNS;
        int column = cell % COLUMNS;
        float radians = (float) Math.toRadians(facing);
        float forwardX = (float) Math.cos(radians);
        float forwardY = (float) Math.sin(radians);
        float rightX = -forwardY;
        float rightY = forwardX;
        float forward = (row - (ROWS - 1) * 0.5f) * ROW_SPACING;
        float lateral = (column - (COLUMNS - 1) * 0.5f) * COLUMN_SPACING;
        return new Vector2f(
                gridCenter.x + forwardX * forward + rightX * lateral,
                gridCenter.y + forwardY * forward + rightY * lateral);
    }

    private static final class VlsState {
        private final WeaponAPI weapon;
        private final Random random = new Random();
        private final int[] launchOrder = new int[CELL_COUNT];
        private int nextLaunchIndex;
        private boolean reloading;

        private VlsState(WeaponAPI weapon) {
            this.weapon = weapon;
            resetForNextVolley();
        }

        /** Fisher-Yates once per volley: random order, stable while it fires. */
        private void resetForNextVolley() {
            for (int i = 0; i < CELL_COUNT; i++) {
                launchOrder[i] = i;
            }
            for (int i = CELL_COUNT - 1; i > 0; i--) {
                int swap = random.nextInt(i + 1);
                int cell = launchOrder[i];
                launchOrder[i] = launchOrder[swap];
                launchOrder[swap] = cell;
            }
            nextLaunchIndex = 0;
        }
    }

    /** Draws only the loaded caps that have not yet fired in the current sequence. */
    private static final class LoadedCapRenderer extends BaseCombatLayeredRenderingPlugin {
        private final VlsState state;
        private final SpriteAPI cap;

        private LoadedCapRenderer(VlsState state) {
            this.state = state;
            cap = Global.getSettings().getSprite(
                    "graphics/weapons/chief_navigator_metalstorm_cap_single_v4.png");
            cap.setSize(44f, 44f);
        }

        @Override
        public void init(CombatEntityAPI entity) {
            super.init(entity);
            entity.getLocation().set(state.weapon.getLocation());
        }

        @Override
        public void advance(float amount) {
            if (entity != null) {
                entity.getLocation().set(state.weapon.getLocation());
            }
        }

        @Override
        public boolean isExpired() {
            return state.weapon.getShip() == null || !state.weapon.getShip().isAlive();
        }

        @Override
        public float getRenderRadius() {
            return 360f;
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER);
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (layer != CombatEngineLayers.ABOVE_SHIPS_LAYER) return;
            float facing = state.weapon.getCurrAngle();
            Vector2f gridCenter = getGridCenter(state.weapon, facing);
            cap.setAlphaMult(viewport.getAlphaMult());
            for (int i = state.nextLaunchIndex; i < CELL_COUNT; i++) {
                int cell = state.launchOrder[i];
                Vector2f location = getCellLocation(gridCenter, facing, cell);
                cap.renderAtCenter(location.x, location.y);
            }
        }
    }

    /** Slow simulated vertical rise followed by constant-speed weaving guidance. */
    private static final class VerticalLaunchMissileAI
            implements MissileAIPlugin, GuidedMissileAI {
        private static final float LAUNCH_PHASE = 0.85f;
        private static final float TRANSITION_PHASE = 0.55f;
        private static final float LAUNCH_START_SPEED = 18f;
        private static final float LAUNCH_END_SPEED = 70f;
        private static final float CRUISE_SPEED = 650f;
        private static final float TURN_RATE = 70f;
        private static final float MAX_LEAD_TIME = 1.5f;

        private final CombatEngineAPI engine;
        private final MissileAPI missile;
        private final float launchFacing;
        private final float weaveAmplitude;
        private final float weavePeriod;
        private final float weavePhase;
        private CombatEntityAPI target;
        private float elapsed;

        private VerticalLaunchMissileAI(
                CombatEngineAPI engine,
                MissileAPI missile,
                CombatEntityAPI target) {
            this.engine = engine;
            this.missile = missile;
            this.target = target;
            launchFacing = missile.getFacing();
            weaveAmplitude = 4f + (float) Math.random() * 9f;
            weavePeriod = 1.4f + (float) Math.random() * 1.8f;
            weavePhase = (float) Math.random() * (float) Math.PI * 2f;
            setVelocity(launchFacing, LAUNCH_START_SPEED);
        }

        @Override
        public void advance(float amount) {
            if (engine.isPaused() || missile.isFading() || missile.isFizzling()) {
                return;
            }

            elapsed += amount;
            if (elapsed < LAUNCH_PHASE) {
                missile.setFacing(launchFacing);
                missile.setAngularVelocity(0f);
                float progress = elapsed / LAUNCH_PHASE;
                setVelocity(launchFacing, lerp(
                        LAUNCH_START_SPEED, LAUNCH_END_SPEED, progress));
                return;
            }

            if (!isValidTarget(target)) {
                target = findNearestEnemy();
            }

            if (target != null) {
                float dx = target.getLocation().x - missile.getLocation().x;
                float dy = target.getLocation().y - missile.getLocation().y;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                float leadTime = Math.min(MAX_LEAD_TIME, distance / CRUISE_SPEED);
                float aimX = target.getLocation().x + target.getVelocity().x * leadTime;
                float aimY = target.getLocation().y + target.getVelocity().y * leadTime;
                float desiredFacing = (float) Math.toDegrees(Math.atan2(
                        aimY - missile.getLocation().y,
                        aimX - missile.getLocation().x));
                float weaveScale = Math.min(1f, distance / 500f);
                desiredFacing += (float) Math.sin(
                        weavePhase + elapsed * Math.PI * 2f / weavePeriod)
                        * weaveAmplitude * weaveScale;
                float turn = shortestRotation(missile.getFacing(), desiredFacing);
                float maxTurn = TURN_RATE * amount;
                turn = Math.max(-maxTurn, Math.min(maxTurn, turn));
                missile.setFacing(missile.getFacing() + turn);
                missile.setAngularVelocity(0f);
            }

            float transition = Math.min(
                    1f, (elapsed - LAUNCH_PHASE) / TRANSITION_PHASE);
            float smoothTransition = transition * transition * (3f - 2f * transition);
            setVelocity(missile.getFacing(), lerp(
                    LAUNCH_END_SPEED, CRUISE_SPEED, smoothTransition));
        }

        private boolean isValidTarget(CombatEntityAPI candidate) {
            if (!(candidate instanceof ShipAPI)) {
                return false;
            }
            ShipAPI ship = (ShipAPI) candidate;
            return ship.isAlive()
                    && !ship.isHulk()
                    && ship.getOwner() != 100
                    && ship.getOwner() != missile.getOwner()
                    && engine.isEntityInPlay(ship);
        }

        private CombatEntityAPI findNearestEnemy() {
            ShipAPI best = null;
            float bestDistanceSquared = Float.MAX_VALUE;
            for (ShipAPI candidate : engine.getShips()) {
                if (!isValidTarget(candidate)) {
                    continue;
                }
                float dx = candidate.getLocation().x - missile.getLocation().x;
                float dy = candidate.getLocation().y - missile.getLocation().y;
                float distanceSquared = dx * dx + dy * dy;
                if (distanceSquared < bestDistanceSquared) {
                    best = candidate;
                    bestDistanceSquared = distanceSquared;
                }
            }
            return best;
        }

        private void setVelocity(float facing, float speed) {
            float radians = (float) Math.toRadians(facing);
            missile.getVelocity().set(
                    (float) Math.cos(radians) * speed,
                    (float) Math.sin(radians) * speed);
        }

        private static float lerp(float from, float to, float progress) {
            return from + (to - from) * progress;
        }

        private static float shortestRotation(float from, float to) {
            float result = (to - from) % 360f;
            if (result > 180f) result -= 360f;
            if (result < -180f) result += 360f;
            return result;
        }

        @Override
        public CombatEntityAPI getTarget() {
            return target;
        }

        @Override
        public void setTarget(CombatEntityAPI target) {
            this.target = target;
        }
    }
}
