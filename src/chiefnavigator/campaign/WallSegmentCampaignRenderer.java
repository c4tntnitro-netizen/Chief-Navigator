package chiefnavigator.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import java.awt.Color;

/** Draws the filled campaign-scale form of one curved Ithaca wall section. */
final class WallSegmentCampaignRenderer {
    private static final String INTACT_FRAME =
            "graphics/ships/chief_navigator_drifting_wall_frame_v2.png";
    private static final String DAMAGED_FRAME =
            "graphics/ships/chief_navigator_drifting_wall_frame_v3.png";
    private static final String INSERT =
            "graphics/ships/chief_navigator_drifting_wall_insert_v3.png";

    private static final float FRAME_NATIVE_WIDTH = 1800f;
    private static final float FRAME_NATIVE_HEIGHT = 5700f;
    private static final float INSERT_NATIVE_WIDTH = 400f;
    private static final float INSERT_NATIVE_HEIGHT = 650f;
    private static final float FRAME_TEXTURE_CENTER_X = 900f;
    private static final float HULL_CENTER_X = 600f;
    private static final float MODULE_FORWARD_ANGLE = 270f;

    private static final float[] SLOT_FORWARD = {
        -1620f, -1080f, -540f, 0f, 540f, 1080f, 1620f
    };
    private static final float[] SLOT_LATERAL = {
        -200f, -360f, -460f, -500f, -460f, -360f, -200f
    };
    private static final float[] SLOT_ANGLE = {
        261f, 264f, 267f, 270f, 273f, 276f, 279f
    };

    private static final Color CAMPAIGN_TINT = new Color(180, 176, 165);

    private SpriteAPI intactFrame;
    private SpriteAPI damagedFrame;
    private SpriteAPI insert;

    WallSegmentCampaignRenderer() {
        loadSprites();
    }

    void loadSprites() {
        intactFrame = Global.getSettings().getSprite(INTACT_FRAME);
        damagedFrame = Global.getSettings().getSprite(DAMAGED_FRAME);
        insert = Global.getSettings().getSprite(INSERT);
    }

    void renderSegment(
            float centerX,
            float centerY,
            float angle,
            float width,
            float height,
            boolean damagedEnds,
            float alpha) {
        SpriteAPI frame = damagedEnds ? damagedFrame : intactFrame;
        if (frame == null || insert == null) loadSprites();
        frame = damagedEnds ? damagedFrame : intactFrame;

        frame.setSize(width, height);
        frame.setAngle(angle);
        frame.setColor(CAMPAIGN_TINT);
        frame.setAlphaMult(alpha);
        frame.renderAtCenter(centerX, centerY);

        float scaleX = width / FRAME_NATIVE_WIDTH;
        float scaleY = height / FRAME_NATIVE_HEIGHT;
        insert.setSize(
                INSERT_NATIVE_WIDTH * scaleX,
                INSERT_NATIVE_HEIGHT * scaleY);
        insert.setColor(CAMPAIGN_TINT);
        insert.setAlphaMult(alpha);

        double radians = Math.toRadians(angle);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        for (int i = 0; i < SLOT_FORWARD.length; i++) {
            // Convert the station-module coordinates to offsets from the
            // texture center, then rotate them with the parent frame.
            float localX = (HULL_CENTER_X - SLOT_LATERAL[i]
                    - FRAME_TEXTURE_CENTER_X) * scaleX;
            float localY = -SLOT_FORWARD[i] * scaleY;
            float x = centerX + localX * cos - localY * sin;
            float y = centerY + localX * sin + localY * cos;
            insert.setAngle(angle + SLOT_ANGLE[i] - MODULE_FORWARD_ANGLE);
            insert.renderAtCenter(x, y);
        }
    }
}
