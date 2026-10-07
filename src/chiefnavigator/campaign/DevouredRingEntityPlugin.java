package chiefnavigator.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.impl.campaign.GateEntityPlugin;
import com.fs.starfarer.api.util.FaderUtil;

/** A vanilla-compatible Janus gate whose activation exposes the Core. */
public final class DevouredRingEntityPlugin extends GateEntityPlugin {
    public static final String PATH_INFORMATION_GAINED =
            "$chief_navigator_devoured_ring_path_information_v1";
    private static final String LEGACY_CORE_ROUTE_OPEN =
            "$chief_navigator_devoured_ring_core_route_open";
    @Override
    public void init(SectorEntityToken entity, Object pluginParams) {
        super.init(entity, pluginParams);
        restoreTransientGateState();
        preventGateActivation();
    }

    /** Rebuilds vanilla gate sprites which are deliberately not serialized. */
    private Object readResolve() {
        restoreTransientGateState();
        return this;
    }

    @Override
    public void advance(float amount) {
        restoreTransientGateState();
        preventGateActivation();
        super.advance(amount);
        preventGateActivation();
    }

    /** The eaten ring is evidence, not a usable member of the Janus lattice. */
    @Override
    public boolean isActive() {
        return false;
    }

    public static void recordPathInformation() {
        if (Global.getSector() != null) {
            Global.getSector().getMemoryWithoutUpdate().set(
                    PATH_INFORMATION_GAINED, true);
        }
    }

    public static boolean hasPathInformation() {
        return Global.getSector() != null
                && Global.getSector().getMemoryWithoutUpdate().getBoolean(
                        PATH_INFORMATION_GAINED);
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        restoreTransientGateState();
        super.render(layer, viewport);
    }

    @Override
    protected void scaleGlowSprites() {
        restoreTransientGateState();
        if (scaledSprites) return;
        super.scaleGlowSprites();
        float diameter = entity.getCustomEntitySpec().getSpriteWidth();
        scannedGlow.setSize(diameter, diameter);
        activeGlow.setSize(diameter, diameter);
        rays.setSize(diameter * 1.05f, diameter * 1.05f);
        whirl1.setSize(diameter * 0.82f, diameter * 0.82f);
        whirl2.setSize(diameter * 0.82f, diameter * 0.82f);
        starfield.setSize(diameter * 0.76f, diameter * 0.76f);
        concentric.setSize(diameter * 0.92f, diameter * 0.92f);
    }

    private void restoreTransientGateState() {
        if (scannedGlow == null || activeGlow == null || rays == null
                || whirl1 == null || whirl2 == null || starfield == null
                || concentric == null) {
            scannedGlow = Global.getSettings().getSprite(
                    "gates", "glow_scanned");
            activeGlow = Global.getSettings().getSprite(
                    "gates", "glow_ring_active");
            concentric = Global.getSettings().getSprite(
                    "gates", "glow_concentric");
            rays = Global.getSettings().getSprite("gates", "glow_rays");
            whirl1 = Global.getSettings().getSprite("gates", "glow_whirl1");
            whirl2 = Global.getSettings().getSprite("gates", "glow_whirl2");
            starfield = Global.getSettings().getSprite("gates", "starfield");
            // This flag is serialized by the vanilla superclass even though
            // the SpriteAPI fields are transient.
            scaledSprites = false;
        }
        if (beingUsedFader == null) {
            beingUsedFader = new FaderUtil(0f, 1f, 1f, false, true);
        }
        if (glowFader == null) {
            glowFader = new FaderUtil(0f, 1f, 1f, true, true);
            glowFader.fadeIn();
        }
    }

    /** Migrates saves in which the old implementation allowed a gate scan. */
    private void preventGateActivation() {
        if (entity == null) return;
        if (GateEntityPlugin.isScanned(entity)) {
            recordPathInformation();
            entity.getMemoryWithoutUpdate().unset(GATE_SCANNED);
            if (Global.getSector() != null) {
                GateData data = GateEntityPlugin.getGateData();
                boolean removed = data != null && data.scanned != null
                        && data.scanned.remove(entity);
                if (removed) {
                    int count = Math.max(0,
                            Global.getSector().getMemoryWithoutUpdate().getInt(
                                    NUM_GATES_SCANNED) - 1);
                    Global.getSector().getMemoryWithoutUpdate().set(
                            NUM_GATES_SCANNED, count);
                }
            }
        }
        madeActive = false;
        if (Global.getSector() != null) {
            Global.getSector().getMemoryWithoutUpdate().unset(
                    LEGACY_CORE_ROUTE_OPEN);
        }
    }
}
