import chiefnavigator.weapons.IthacaAbyssalBeamEffect;
import chiefnavigator.weapons.IthacaResearchUpgrades;
import chiefnavigator.weapons.AbyssalBeamGradientRenderer;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.dweller.AbyssalGlareEffect;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.loading.BeamWeaponSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import java.awt.Color;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;

/** Runs the actual vanilla Glare callback behind Ithaca's hull/upgrade/paused gate. */
public final class AbyssalLensRegression extends WallResearchRegression {
    private static final String SNAPSHOT = "chief_navigator_ithaca_research_component_mask";
    private static final String INSTALLED = "$chief_navigator_ithaca_research_component_mask_v1";
    private static final String ITHACA = "chief_navigator_ithaca_intact_base";
    private static final String ORIGINAL_FRINGE = "regression/fringe-from-beam-spec.png";
    private static final String ORIGINAL_CORE = "regression/core-from-beam-spec.png";
    private static final String DEFAULT_ROUGH_FRINGE = "graphics/fx/beamfringe.png";
    private static final String NAVAL_WEAPON = "chief_navigator_drifting_wall_colossal_inert";
    private static final String EMPTY = "graphics/fx/empty.png";
    private static boolean spritesAvailable = true;

    private static final class Ray {
        final Map<String, Object> data = new HashMap<>();
        final Vector2f from = new Vector2f();
        final Vector2f to = new Vector2f(1000f, 0f);
        final ShipAPI source;
        final BeamAPI beam;
        final String nativeFringe;
        Color core, fringe;
        String fringeTexture, coreTexture = ORIGINAL_CORE;
        int colorWrites, unrelatedMutations, fringeTextureWrites, coreTextureWrites;
        float brightness = 1f;
        boolean hitting = true;

        Ray(String hull, Integer cachedMask) {
            this(hull, cachedMask, ORIGINAL_FRINGE, NAVAL_WEAPON);
        }

        Ray(String hull, Integer cachedMask, String specifiedFringe, String weaponId) {
            nativeFringe = specifiedFringe == null ? DEFAULT_ROUGH_FRINGE : specifiedFringe;
            fringeTexture = nativeFringe;
            if (cachedMask != null) data.put(SNAPSHOT, cachedMask);
            MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class,
                    (name, args) -> name.equals("getTimeMult") ? new MutableStat(1f) : null);
            ShipHullSpecAPI spec = mock(ShipHullSpecAPI.class,
                    (name, args) -> name.equals("getHullId") ? hull : null);
            source = mock(ShipAPI.class, (name, args) -> {
                if (name.equals("getHullSpec")) return spec;
                if (name.equals("getCustomData")) return data;
                if (name.equals("setCustomData")) data.put((String) args[0], args[1]);
                if (name.equals("getMutableStats")) return stats;
                if (name.equals("getLocation") || name.equals("getVelocity")) return new Vector2f();
                if (name.equals("isAlive")) return true;
                if (name.startsWith("set") && !name.equals("setCustomData")) unrelatedMutations++;
                return null;
            });
            BeamWeaponSpecAPI beamSpec = mock(BeamWeaponSpecAPI.class, (name, args) -> {
                if (name.equals("getFringeTex")) return specifiedFringe;
                if (name.equals("getCoreTex")) return ORIGINAL_CORE;
                if (name.equals("getWidth")) return 90f;
                if (name.equals("getPixelsPerTexel")) return 5f;
                if (name.equals("getTextureScrollSpeed")) return 520f;
                if (name.equals("getFringeScrollSpeedMult")) return 1f;
                if (name.startsWith("set")) unrelatedMutations++;
                return null;
            });
            WeaponAPI weapon = mock(WeaponAPI.class, (name, args) -> {
                if (name.equals("getShip")) return source;
                if (name.equals("getSpec")) return beamSpec;
                if (name.equals("getId")) return weaponId;
                if (name.equals("getRange")) return 1500f;
                if (name.startsWith("set") || name.equals("ensureClonedSpec")) unrelatedMutations++;
                return null;
            });
            CombatEntityAPI target = mock(CombatEntityAPI.class, (name, args) -> null);
            beam = mock(BeamAPI.class, (name, args) -> {
                if (name.equals("getSource")) return source;
                if (name.equals("getWeapon")) return weapon;
                if (name.equals("getFrom")) return from;
                if (name.equals("getTo") || name.equals("getRayEndPrevFrame")) return to;
                if (name.equals("getDamageTarget")) return hitting ? target : null;
                if (name.equals("getBrightness")) return brightness;
                if (name.equals("getWidth")) return 90f;
                if (name.equals("getPixelsPerTexel")) return 5f;
                if (name.equals("getLength") || name.equals("getLengthPrevFrame"))
                    return Vector2f.sub(to, from, null).length();
                if (name.equals("getCoreColor")) return core;
                if (name.equals("getFringeColor")) return fringe;
                if (name.equals("setCoreColor")) { core = (Color) args[0]; colorWrites++; }
                if (name.equals("setFringeColor")) { fringe = (Color) args[0]; colorWrites++; }
                if (name.equals("setFringeTexture")) { fringeTexture = (String) args[0]; fringeTextureWrites++; }
                if (name.equals("setCoreTexture")) { coreTexture = (String) args[0]; coreTextureWrites++; }
                return null;
            });
        }
    }

    private static final class Fixture {
        final List<DamagingExplosionSpec> explosions = new ArrayList<>();
        final List<ShipAPI> sources = new ArrayList<>();
        final List<Vector2f> impactLocations = new ArrayList<>();
        final List<BeamAPI> liveBeams = new ArrayList<>();
        final List<AbyssalBeamGradientRenderer> gradients = new ArrayList<>();
        final Map<String, Object> saved = new HashMap<>();
        final CombatEngineAPI engine;
        int visuals, directDamage;
        boolean paused;
        boolean failGradientRegistration;
        Ray advancing;

        Fixture() {
            saved.put("$chief_navigator_ithaca_research_consumables_migrated_v2", true);
            MemoryAPI memory = mock(MemoryAPI.class, (name, args) -> {
                Object value = args == null || args.length == 0 ? null : saved.get(args[0]);
                if (name.equals("get")) return value;
                if (name.equals("getInt")) return value instanceof Number ? ((Number) value).intValue() : 0;
                if (name.equals("getBoolean")) return Boolean.TRUE.equals(value);
                if (name.equals("set")) saved.put((String) args[0], args[1]);
                return null;
            });
            Global.setSector(mock(SectorAPI.class,
                    (name, args) -> name.equals("getMemoryWithoutUpdate") ? memory : null));
            EmpArcEntityAPI visualArc = mock(EmpArcEntityAPI.class, (name, args) -> null);
            engine = mock(CombatEngineAPI.class, (name, args) -> {
                if (name.equals("isPaused")) return paused;
                if (name.equals("getTimeMult")) return new MutableStat(1f);
                if (name.equals("getShips")) return Collections.emptyList();
                if (name.equals("getBeams")) return liveBeams;
                if (name.equals("getCustomData")) return saved;
                if (name.equals("spawnDamagingExplosion")) {
                    DamagingExplosionSpec spec = (DamagingExplosionSpec) args[0];
                    ShipAPI source = (ShipAPI) args[1];
                    Vector2f location = new Vector2f((Vector2f) args[2]);
                    explosions.add(spec); sources.add(source); impactLocations.add(location);
                    return mock(DamagingProjectileAPI.class, (call, ignored) -> {
                        if (call.equals("getSource")) return source;
                        if (call.equals("getLocation")) return location;
                        return null;
                    });
                }
                if (name.equals("addLayeredRenderingPlugin")) {
                    CombatLayeredRenderingPlugin plugin = (CombatLayeredRenderingPlugin) args[0];
                    if (plugin instanceof AbyssalBeamGradientRenderer) {
                        check(advancing != null && advancing.nativeFringe.equals(advancing.fringeTexture),
                                "Register the gradient before hiding the native fringe");
                        if (failGradientRegistration) throw new IllegalStateException("Expected gradient registration failure");
                        gradients.add((AbyssalBeamGradientRenderer) plugin);
                    } else visuals++;
                    Vector2f location = new Vector2f(), velocity = new Vector2f();
                    CombatEntityAPI entity = mock(CombatEntityAPI.class, (call, ignored) -> {
                        if (call.equals("getLocation")) return location;
                        if (call.equals("getVelocity")) return velocity;
                        return null;
                    });
                    if (plugin instanceof AbyssalBeamGradientRenderer) plugin.init(entity);
                    return entity;
                }
                if (name.equals("spawnEmpArcVisual")) return visualArc;
                if (name.equals("applyDamage")) directDamage++;
                return null;
            });
            Global.setCombatEngine(engine);
        }

        void advance(IthacaAbyssalBeamEffect effect, Ray ray, float amount) {
            if (!liveBeams.contains(ray.beam)) liveBeams.add(ray.beam);
            advancing = ray;
            try { effect.advance(amount, engine, ray.beam); }
            finally { advancing = null; }
        }
    }

    private static void scopeAndPause() {
        Fixture f = new Fixture();
        for (String hull : new String[] {ITHACA, "chief_navigator_ithaca_naval_mount",
                "chief_navigator_spartan_battlestation"}) {
            Ray ray = new Ray(hull, IthacaResearchUpgrades.ABYSSAL_LENS);
            int before = f.explosions.size();
            int gradients = f.gradients.size();
            f.advance(new IthacaAbyssalBeamEffect(), ray, .01f);
            check(f.explosions.size() == before + 1 && Color.WHITE.equals(ray.core)
                            && new Color(255, 30, 45, 155).equals(ray.fringe),
                    "An installed lens produces red native Glare on eligible hull: " + hull);
            check(f.gradients.size() == gradients + 1 && EMPTY.equals(ray.fringeTexture)
                            && ORIGINAL_CORE.equals(ray.coreTexture) && ray.coreTextureWrites == 0,
                    "Eligible lens rays replace only their native fringe with the spatial gradient: " + hull);
            check(ray.unrelatedMutations == 0,
                    "Lens must preserve ship stats, targeting, weapon spec and collision: " + hull);
        }
        for (String hull : new String[] {ITHACA, "chief_navigator_drifting_wall_base", "unrelated_hull"}) {
            Ray ray = new Ray(hull, hull.equals(ITHACA) ? 0 : IthacaResearchUpgrades.ALL_UPGRADES);
            int before = f.explosions.size();
            int gradients = f.gradients.size();
            f.advance(new IthacaAbyssalBeamEffect(), ray, 10f);
            check(f.explosions.size() == before && ray.colorWrites == 0 && ray.unrelatedMutations == 0,
                    "The shared naval weapon stays unchanged without an eligible lens source: " + hull);
            check(f.gradients.size() == gradients && ray.fringeTextureWrites == 0 && ray.coreTextureWrites == 0,
                    "Unupgraded, hostile Wall and foreign rays must retain native rendering: " + hull);
        }
        Ray paused = new Ray(ITHACA, IthacaResearchUpgrades.ABYSSAL_LENS);
        IthacaAbyssalBeamEffect effect = new IthacaAbyssalBeamEffect();
        f.paused = true;
        int before = f.explosions.size();
        int gradients = f.gradients.size();
        f.advance(effect, paused, 100f);
        check(f.explosions.size() == before && paused.colorWrites == 0
                        && f.gradients.size() == gradients && paused.fringeTextureWrites == 0,
                "Paused combat must not advance native Glare or mutate beam colors");
        f.paused = false;
        f.advance(effect, paused, .01f);
        check(f.explosions.size() == before + 1,
                "The first unpaused target acquisition must retain native immediate-impact behavior");
        check(f.directDamage == 0, "Native damaging explosions must own damage without a parallel scripted hit");
    }

    private static void nativeImpactAndRayIsolation() {
        Fixture f = new Fixture();
        Ray first = new Ray(ITHACA, IthacaResearchUpgrades.ABYSSAL_LENS);
        Ray second = new Ray(ITHACA, IthacaResearchUpgrades.ABYSSAL_LENS);
        second.to.set(700f, 300f);
        IthacaAbyssalBeamEffect firstEffect = new IthacaAbyssalBeamEffect();
        IthacaAbyssalBeamEffect secondEffect = new IthacaAbyssalBeamEffect();
        f.advance(firstEffect, first, .01f);
        f.advance(secondEffect, second, .01f);
        check(f.explosions.size() == 2 && f.sources.get(0) == first.source && f.sources.get(1) == second.source
                        && f.impactLocations.get(0).equals(first.to) && f.impactLocations.get(1).equals(second.to),
                "Each native ray owns independent acquisition state and places its explosion at its own impact");
        DamagingExplosionSpec nativeSpec = new AbyssalGlareEffect().createExplosionSpec(
                AbyssalGlareEffect.DAMAGE_MULT_NORMAL_WEAPON);
        for (DamagingExplosionSpec actual : f.explosions) {
            check(actual.getMaxDamage() == nativeSpec.getMaxDamage()
                            && actual.getMinDamage() == nativeSpec.getMinDamage()
                            && actual.getRadius() == nativeSpec.getRadius()
                            && actual.getCoreRadius() == nativeSpec.getCoreRadius()
                            && actual.getDuration() == nativeSpec.getDuration()
                            && actual.getDamageType() == nativeSpec.getDamageType()
                            && actual.getCollisionClass() == nativeSpec.getCollisionClass()
                            && actual.getCollisionClassIfByFighter() == nativeSpec.getCollisionClassIfByFighter()
                            && actual.getSoundSetId().equals(nativeSpec.getSoundSetId())
                            && actual.getSoundVolume() == nativeSpec.getSoundVolume(),
                    "The installed lens must use the actual native normal-weapon explosion mechanics");
        }
        check(f.visuals > 0, "Actual native Glare must retain its rift visuals as well as its damaging explosion");
    }

    private static void deploymentSnapshot() {
        Fixture f = new Fixture();
        Ray beforeInstall = new Ray(ITHACA, null);
        IthacaAbyssalBeamEffect oldDeployment = new IthacaAbyssalBeamEffect();
        f.advance(oldDeployment, beforeInstall, .01f);
        check(Integer.valueOf(0).equals(beforeInstall.data.get(SNAPSHOT)),
                "The first deployed ray snapshots the actual installed research mask");
        f.saved.put(INSTALLED, IthacaResearchUpgrades.ABYSSAL_LENS);
        f.advance(oldDeployment, beforeInstall, 1f);
        check(f.explosions.isEmpty() && beforeInstall.colorWrites == 0,
                "A campaign-mask change must not grant the lens to an already deployed source");
        Ray afterInstall = new Ray(ITHACA, null);
        IthacaAbyssalBeamEffect newDeployment = new IthacaAbyssalBeamEffect();
        f.advance(newDeployment, afterInstall, .01f);
        check(f.explosions.size() == 1
                        && Integer.valueOf(IthacaResearchUpgrades.ABYSSAL_LENS).equals(afterInstall.data.get(SNAPSHOT)),
                "A subsequent deployment snapshots and uses the installed seventh component");
        f.saved.put(INSTALLED, 0);
        f.advance(newDeployment, afterInstall, .6f);
        check(f.explosions.size() == 2,
                "A live lens snapshot retains native repeat-impact timing after campaign state changes");
    }

    private static void rendererLifecycle() {
        Fixture f = new Fixture();
        Ray first = new Ray(ITHACA, IthacaResearchUpgrades.ABYSSAL_LENS);
        Ray second = new Ray(ITHACA, IthacaResearchUpgrades.ABYSSAL_LENS);
        IthacaAbyssalBeamEffect firstEffect = new IthacaAbyssalBeamEffect();
        f.advance(firstEffect, first, .01f);
        AbyssalBeamGradientRenderer renderer = f.gradients.get(0);
        check(renderer.getActiveLayers().equals(EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER)),
                "The fringe must render on the native beam layer above ships");
        check(!renderer.isExpired(), "A live ray must retain its renderer");
        float radius = renderer.getRenderRadius();
        first.to.set(3000f, 4000f);
        renderer.advance(.01f);
        check(renderer.getRenderRadius() > radius + 1000f,
                "Gradient culling must follow the current endpoints as the beam length changes");
        f.advance(firstEffect, first, .01f);
        check(f.gradients.size() == 1 && first.fringeTextureWrites == 1,
                "Repeated callbacks must keep one renderer and one native-fringe replacement per ray");
        f.advance(new IthacaAbyssalBeamEffect(), second, .01f);
        check(f.gradients.size() == 2, "Two beam rays must own distinct gradient renderers");
        f.liveBeams.remove(first.beam);
        check(renderer.isExpired() && !f.gradients.get(1).isExpired(),
                "Only the renderer whose exact ray left the native beam list must expire");
        renderer.cleanup();
        check(ORIGINAL_FRINGE.equals(first.fringeTexture) && first.fringeTextureWrites == 2
                        && EMPTY.equals(second.fringeTexture) && first.coreTextureWrites == 0
                        && first.unrelatedMutations == 0,
                "Cleanup restores the original fringe without touching the core, another ray or weapon spec");
    }

    private static void renderingFallback() {
        Fixture f = new Fixture();
        Ray failed = new Ray(ITHACA, IthacaResearchUpgrades.ABYSSAL_LENS);
        f.failGradientRegistration = true;
        f.advance(new IthacaAbyssalBeamEffect(), failed, .01f);
        check(f.explosions.size() == 1 && f.gradients.isEmpty()
                        && failed.fringeTextureWrites == 0 && ORIGINAL_FRINGE.equals(failed.fringeTexture)
                        && Color.WHITE.equals(failed.core) && new Color(255, 30, 45, 155).equals(failed.fringe),
                "Registration failure must preserve native Glare damage, colors, core and visible fringe");
        spritesAvailable = false;
        try {
            Ray missing = new Ray(ITHACA, IthacaResearchUpgrades.ABYSSAL_LENS);
            f.failGradientRegistration = false;
            f.advance(new IthacaAbyssalBeamEffect(), missing, .01f);
            check(f.explosions.size() == 2 && f.gradients.isEmpty() && missing.fringeTextureWrites == 0,
                    "An unavailable fringe resource must leave native rendering and mechanics intact");
        } finally { spritesAvailable = true; }
    }

    private static void nativeRoughDefaultAndWeaponScope() {
        Fixture f = new Fixture();
        Ray rough = new Ray(ITHACA, IthacaResearchUpgrades.ABYSSAL_LENS, null, NAVAL_WEAPON);
        f.advance(new IthacaAbyssalBeamEffect(), rough, .01f);
        check(f.gradients.size() == 1 && EMPTY.equals(rough.fringeTexture)
                        && f.explosions.size() == 1 && rough.coreTextureWrites == 0,
                "Native enum-style ROUGH specs have no explicit fringe path and must still install the gradient");
        f.gradients.get(0).cleanup();
        check(DEFAULT_ROUGH_FRINGE.equals(rough.fringeTexture) && rough.fringeTextureWrites == 2
                        && rough.unrelatedMutations == 0,
                "Cleanup must restore the resolved stock ROUGH fringe without changing the weapon spec");

        Ray failed = new Ray(ITHACA, IthacaResearchUpgrades.ABYSSAL_LENS, null, NAVAL_WEAPON);
        f.failGradientRegistration = true;
        f.advance(new IthacaAbyssalBeamEffect(), failed, .01f);
        check(f.gradients.size() == 1 && DEFAULT_ROUGH_FRINGE.equals(failed.fringeTexture)
                        && failed.fringeTextureWrites == 0 && f.explosions.size() == 2,
                "Failed registration must retain the default native fringe and ordinary Glare behavior");

        Ray foreign = new Ray(ITHACA, IthacaResearchUpgrades.ABYSSAL_LENS, null, "foreign_beam");
        f.failGradientRegistration = false;
        check(AbyssalBeamGradientRenderer.install(f.engine, foreign.beam) == null
                        && f.gradients.size() == 1 && foreign.fringeTextureWrites == 0
                        && foreign.coreTextureWrites == 0 && foreign.unrelatedMutations == 0,
                "The stock-fringe fallback must apply only to the exact owned naval weapon");
    }

    public static void main(String[] args) {
        SettingsAPI settings = Global.getSettings();
        SoundPlayerAPI sound = Global.getSoundPlayer();
        CombatEngineAPI engine = Global.getCombatEngine();
        SectorAPI sector = Global.getSector();
        try {
            Global.setSettings(mock(SettingsAPI.class, (name, values) -> {
                if (name.equals("getColor")) return Color.WHITE;
                if (!name.equals("getSprite") || !spritesAvailable) return null;
                check(ORIGINAL_FRINGE.equals(values[values.length - 1])
                                || DEFAULT_ROUGH_FRINGE.equals(values[values.length - 1]),
                        "Load this beam's explicit fringe or its exact native ROUGH default");
                return mock(SpriteAPI.class, (method, args1) -> {
                    if (method.equals("getTextureId")) return 1;
                    if (method.equals("getWidth") || method.equals("getHeight")
                            || method.equals("getTextureWidth") || method.equals("getTextureHeight")) return 128f;
                    return null;
                });
            }));
            Global.setSoundPlayer(mock(SoundPlayerAPI.class, (name, values) -> null));
            scopeAndPause();
            nativeImpactAndRayIsolation();
            deploymentSnapshot();
            rendererLifecycle();
            renderingFallback();
            nativeRoughDefaultAndWeaponScope();
            System.out.println("PASS: scoped lens fringe gradients and lifecycle, unchanged native white core/red Glare "
                    + "explosions and rifts, per-ray acquisition, pause, deployment snapshots and safe rendering fallback.");
        } finally {
            Global.setSettings(settings);
            Global.setSoundPlayer(sound);
            Global.setCombatEngine(engine);
            Global.setSector(sector);
        }
    }
}
