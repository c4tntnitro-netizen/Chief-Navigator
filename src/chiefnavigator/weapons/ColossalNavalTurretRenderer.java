package chiefnavigator.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import java.awt.Color;
import java.util.IdentityHashMap;
import java.util.Map;

/** Keeps the original naval artwork separate from native damage-flash sizing. */
public final class ColossalNavalTurretRenderer
        extends BaseCombatLayeredRenderingPlugin {
    private static final String WEAPON_ID =
            "chief_navigator_drifting_wall_colossal_inert";
    private static final String ART =
            "graphics/weapons/chief_navigator_drifting_wall_colossal_inert.png";
    private static final String REGISTRY_KEY =
            ColossalNavalTurretRenderer.class.getName();
    private static final float RENDER_RADIUS = 1160f + 32f;

    private final CombatEngineAPI engine;
    private final WeaponAPI weapon;
    private final ShipAPI ship;
    private final SpriteAPI artwork;
    private final SpriteAPI combatUnder;
    private final Color originalUnderColor;
    private final Color hiddenUnderColor;
    private final Registry registry;
    private boolean installed;

    private static final class Registry {
        private final Map<WeaponAPI, ColossalNavalTurretRenderer> weapons =
                new IdentityHashMap<>();
    }

    private ColossalNavalTurretRenderer(
            CombatEngineAPI engine, WeaponAPI weapon, SpriteAPI artwork,
            SpriteAPI combatUnder, Registry registry) {
        super(CombatEngineLayers.PHASED_SHIPS_LAYER);
        this.engine = engine;
        this.weapon = weapon;
        this.ship = weapon.getShip();
        this.artwork = artwork;
        this.combatUnder = combatUnder;
        this.registry = registry;
        this.originalUnderColor = combatUnder.getColor();
        Color original = originalUnderColor == null ? Color.WHITE : originalUnderColor;
        this.hiddenUnderColor = new Color(
                original.getRed(), original.getGreen(), original.getBlue(), 0);
        artwork.setCenter(artwork.getWidth() * 0.5f,
                artwork.getHeight() * (weapon.getSlot().isHardpoint() ? 0.25f : 0.5f));
        artwork.setNormalBlend();
    }

    /** Registers once per combat weapon, retaining native artwork on failure. */
    public static boolean install(CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine == null || weapon == null || !WEAPON_ID.equals(weapon.getId())
                || weapon.getShip() == null || weapon.getSlot() == null) return false;
        SpriteAPI under = weapon.getUnderSpriteAPI();
        if (under == null || weapon.getSprite() == null) return false;
        Map<String, Object> data = engine.getCustomData();
        Object existing = data.get(REGISTRY_KEY);
        if (existing != null && !(existing instanceof Registry)) return false;
        Registry registry = existing == null ? new Registry() : (Registry) existing;
        if (registry.weapons.containsKey(weapon)) return true;

        ColossalNavalTurretRenderer renderer = null;
        try {
            // Settings returns an independent sprite; no shared asset or glow is changed.
            SpriteAPI artwork = Global.getSettings().getSprite(ART);
            if (artwork == null) return false;
            renderer = new ColossalNavalTurretRenderer(
                    engine, weapon, artwork, under, registry);
            CombatEntityAPI visual = engine.addLayeredRenderingPlugin(renderer);
            if (visual == null) return false;
            if (renderer.entity == null) renderer.init(visual);
            renderer.installed = true;
            registry.weapons.put(weapon, renderer);
            data.put(REGISTRY_KEY, registry);
            // Hide only this combat duplicate. The UI loads its own under-sprite.
            // Its RGB is unchanged; only visibility is altered. Native renderUnder
            // rewrites alphaMult, so retain the zero alpha in the color itself.
            under.setColor(renderer.hiddenUnderColor);
            return true;
        } catch (RuntimeException failure) {
            if (renderer != null) renderer.cleanup();
            Global.getLogger(ColossalNavalTurretRenderer.class).warn(
                    "Could not install naval artwork renderer; retaining native artwork", failure);
            return false;
        }
    }

    @Override
    public void init(CombatEntityAPI entity) {
        super.init(entity);
        followWeapon();
    }

    @Override
    public void advance(float amount) {
        if (!installed) return;
        if (!engine.isEntityInPlay(ship)) {
            cleanup();
            return;
        }
        followWeapon();
    }

    private void followWeapon() {
        if (entity != null) entity.getLocation().set(weapon.getLocation());
    }

    @Override
    public float getRenderRadius() {
        return RENDER_RADIUS;
    }

    @Override
    public boolean isExpired() {
        return !installed || !engine.isEntityInPlay(ship);
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (layer != CombatEngineLayers.PHASED_SHIPS_LAYER || viewport == null
                || isExpired() || ship.isDoNotRender() || ship.isDoNotRenderWeapons()) return;
        SpriteAPI nativeSprite = weapon.getSprite();
        if (nativeSprite == null) return;
        float alpha = ship.getCombinedAlphaMult() * viewport.getAlphaMult();
        if (alpha <= 0f) return;
        Color color = nativeSprite.getColor();
        artwork.setColor(color == null ? Color.WHITE : color);
        artwork.setAlphaMult(alpha);
        artwork.setAngle(weapon.getCurrAngle() - 90f);
        // Native renderAtCenter anchors the configured pivot at this position,
        // including the quarter-height pivot of a hardpoint.
        artwork.renderAtCenter(weapon.getLocation().x, weapon.getLocation().y);
    }

    @Override
    public void cleanup() {
        installed = false;
        if (registry.weapons.get(weapon) == this) registry.weapons.remove(weapon);
        // Do not overwrite a later visibility/color change made by another owner.
        if (hiddenUnderColor.equals(combatUnder.getColor())) {
            combatUnder.setColor(originalUnderColor == null ? Color.WHITE : originalUnderColor);
        }
    }
}
