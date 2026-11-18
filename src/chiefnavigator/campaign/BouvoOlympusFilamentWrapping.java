package chiefnavigator.campaign;

import chiefnavigator.quest.OdysseyExpanseSystem;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.listeners.CampaignUIRenderingListener;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;

/** Painted, alpha-isolated filament layers following Bouvo Olympus's real star. */
public final class BouvoOlympusFilamentWrapping
        implements EveryFrameScript, CampaignUIRenderingListener {
    static final float SHELL_RADIUS_MULT = 1.85f;
    static final String SPRITE_CATEGORY = "chief_navigator_bouvo_wrapping";
    private static final float TAU = (float) (Math.PI * 2);
    private transient SpriteAPI front;
    private transient SpriteAPI rear;
    float rotation;

    public static void install() {
        SectorAPI sector = Global.getSector();
        if (sector == null) return;
        sector.removeTransientScriptsOfClass(BouvoOlympusFilamentWrapping.class);
        sector.getListenerManager().removeListenerOfClass(BouvoOlympusFilamentWrapping.class);
        BouvoOlympusFilamentWrapping wrapping = new BouvoOlympusFilamentWrapping();
        sector.addTransientScript(wrapping);
        sector.getListenerManager().addListener(wrapping, true);
    }

    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return false; }

    @Override public void advance(float amount) {
        SectorAPI sector = Global.getSector();
        if (sector == null || sector.isPaused() || amount <= 0) return;
        StarSystemAPI system = OdysseyExpanseSystem.findSystemById(OdysseyExpanseSystem.LAST_LIGHT_ID);
        if (system != null && sector.getCurrentLocation() == system) {
            rotation = (rotation + amount * 0.006f) % TAU;
        }
    }

    PlanetAPI visibleStar(ViewportAPI viewport) {
        SectorAPI sector = Global.getSector();
        if (sector == null || viewport == null || viewport.getAlphaMult() <= 0) return null;
        StarSystemAPI system = OdysseyExpanseSystem.findSystemById(OdysseyExpanseSystem.LAST_LIGHT_ID);
        if (system == null || sector.getCurrentLocation() != system) return null;
        CampaignUIAPI ui = sector.getCampaignUI();
        if (ui == null || ui.isShowingDialog() || ui.isShowingMenu()
                || ui.getCurrentCoreTab() != null) return null;
        PlanetAPI star = system.getStar();
        if (star == null || star.getContainingLocation() != system
                || !(OdysseyExpanseSystem.LAST_LIGHT_ID + "_star").equals(star.getId())
                || star.getRadius() <= 0 || !viewport.isNearViewport(
                        star.getLocation(), star.getRadius() * SHELL_RADIUS_MULT)) return null;
        return star;
    }

    @Override public void renderInUICoordsBelowUI(ViewportAPI viewport) {
        PlanetAPI star = visibleStar(viewport);
        if (star == null) return;
        if (front == null || rear == null) {
            front = Global.getSettings().getSprite(SPRITE_CATEGORY, "front");
            rear = Global.getSettings().getSprite(SPRITE_CATEGORY, "rear");
        }
        float diameter = star.getRadius() * SHELL_RADIUS_MULT * 2;
        float x = viewport.convertWorldXtoScreenX(star.getLocation().x);
        float y = viewport.convertWorldYtoScreenY(star.getLocation().y);
        float width = viewport.convertWorldWidthToScreenWidth(diameter);
        float height = viewport.convertWorldHeightToScreenHeight(diameter);
        float degrees = (float) Math.toDegrees(rotation);
        // The rear PNG's circular alpha opening is baked at 1.04 star radii.
        // Both layers use their original black pixels, never a runtime tint.
        draw(rear, x, y, width, height, -degrees * 0.35f, viewport.getAlphaMult() * 0.82f);
        draw(front, x, y, width, height, degrees * 0.65f, viewport.getAlphaMult() * 0.96f);
    }

    private static void draw(SpriteAPI sprite, float x, float y, float width, float height,
            float angle, float alpha) {
        sprite.setNormalBlend();
        sprite.setSize(width, height);
        sprite.setAngle(angle);
        sprite.setAlphaMult(alpha);
        sprite.renderAtCenter(x, y);
    }

    @Override public void renderInUICoordsAboveUIBelowTooltips(ViewportAPI viewport) { }
    @Override public void renderInUICoordsAboveUIAndTooltips(ViewportAPI viewport) { }
}
