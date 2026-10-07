package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatNebulaAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.util.Misc;
import java.util.EnumSet;
import java.util.Random;
import org.lwjgl.util.vector.Vector2f;

/** Gives every tactical map in Sanzu denser, larger natural nebula banks. */
public final class SanzuNebulaBattleCreationPlugin
        extends BattleCreationPluginImpl {
    private static final String SANZU_NEBULA_TEXTURE =
            "graphics/terrain/chief_navigator_sanzu_combat_nebula.png";
    private static final String SANZU_NEBULA_MAP_TEXTURE =
            "graphics/terrain/chief_navigator_sanzu_combat_nebula_map.png";
    private static final int VANILLA_IN_NEBULA_CLOUD_COUNT = 100;
    private static final int VANILLA_BACKGROUND_CLOUD_COUNT = 15;
    private static final int SANZU_CLOUD_COUNT =
            VANILLA_IN_NEBULA_CLOUD_COUNT + 20;
    private static final int ADDITIONAL_SANZU_CLOUDS =
            SANZU_CLOUD_COUNT - VANILLA_BACKGROUND_CLOUD_COUNT;
    private static final float TERRAIN_CLOUD_BRIGHTNESS = 0.20f;
    private static final float TERRAIN_CLOUD_THICKNESS = 0.72f;
    private static final int LARGE_CLOUD_BANK_COUNT = 36;

    @Override
    public void initBattle(
            BattleCreationContext context, MissionDefinitionAPI api) {
        super.initBattle(context, api);
        // This is an authored, pre-colored atlas. Do not runtime-tint it.
        api.setNebulaTex(SANZU_NEBULA_TEXTURE);
        api.setNebulaMapTex(SANZU_NEBULA_MAP_TEXTURE);
        addSanzuClouds(api);
    }

    @Override
    public void afterDefinitionLoad(CombatEngineAPI engine) {
        super.afterDefinitionLoad(engine);
        CombatNebulaAPI nebula = engine.getNebula();
        softenTerrainClouds(nebula);
        int occupiedTiles = countOccupiedTiles(nebula);
        engine.addLayeredRenderingPlugin(
                new LargeCloudBankRenderer(engine));
        Global.getLogger(SanzuNebulaBattleCreationPlugin.class).info(
                "[SANZU_NEBULA] Generated " + SANZU_CLOUD_COUNT
                        + " natural terrain patches plus "
                        + LARGE_CLOUD_BANK_COUNT
                        + " large visual banks; occupied grid tiles: "
                        + (nebula == null
                                ? "unavailable"
                                : occupiedTiles + "/"
                                        + nebula.getTilesWide() *
                                                nebula.getTilesHigh()));
    }

    private void addSanzuClouds(MissionDefinitionAPI api) {
        Random random = Misc.getRandom(Misc.genRandomSeed(), 5);
        for (int index = 0; index < ADDITIONAL_SANZU_CLOUDS; index++) {
            float x = random.nextFloat() * width - width / 2f;
            float y = random.nextFloat() * height - height / 2f;
            // Match vanilla's radius distribution for a battle occurring
            // inside a nebula. The 2x atlas supplies the larger sprite scale.
            float radius = 200f
                    + random.nextFloat() * 400f
                    + random.nextFloat() * 500f;
            api.addNebula(x, y, radius);
        }
    }

    private void softenTerrainClouds(CombatNebulaAPI nebula) {
        if (nebula == null) return;
        for (int column = 0; column < nebula.getTilesWide(); column++) {
            for (int row = 0; row < nebula.getTilesHigh(); row++) {
                if (!nebula.tileHasNebula(column, row)) continue;
                nebula.setHasNebula(
                        column, row, TERRAIN_CLOUD_BRIGHTNESS);
                CombatNebulaAPI.CloudAPI cloud =
                        nebula.getCloud(column, row);
                if (cloud != null) {
                    cloud.setThickness(TERRAIN_CLOUD_THICKNESS);
                }
            }
        }
    }

    private int countOccupiedTiles(CombatNebulaAPI nebula) {
        if (nebula == null) return 0;
        int occupied = 0;
        for (int column = 0; column < nebula.getTilesWide(); column++) {
            for (int row = 0; row < nebula.getTilesHigh(); row++) {
                if (nebula.tileHasNebula(column, row)) occupied++;
            }
        }
        return occupied;
    }

    /**
     * Broad, rotated banks break up the combat-nebula tile lattice. These are
     * visual only; the ordinary nebula grid continues to provide gameplay.
     */
    private static final class LargeCloudBankRenderer
            extends BaseCombatLayeredRenderingPlugin {
        private static final int ATLAS_COLUMNS = 4;
        private static final int ATLAS_ROWS = 4;
        private static final float MIN_LONG_AXIS = 1600f;
        private static final float MAX_LONG_AXIS = 4000f;
        private static final float EDGE_PADDING = 1800f;

        private final CloudBank[] banks =
                new CloudBank[LARGE_CLOUD_BANK_COUNT];
        private final float renderRadius;

        private LargeCloudBankRenderer(CombatEngineAPI engine) {
            Random random = Misc.getRandom(Misc.genRandomSeed(), 7);
            float mapWidth = engine.getMapWidth();
            float mapHeight = engine.getMapHeight();
            float halfWidth = mapWidth / 2f;
            float halfHeight = mapHeight / 2f;
            renderRadius = (float) Math.sqrt(
                    halfWidth * halfWidth + halfHeight * halfHeight)
                    + EDGE_PADDING + MAX_LONG_AXIS;

            for (int index = 0; index < banks.length; index++) {
                SpriteAPI sprite = Global.getSettings().getSprite(
                        SANZU_NEBULA_TEXTURE);
                selectRandomAtlasCell(sprite, random);

                float longAxis = MIN_LONG_AXIS
                        + random.nextFloat()
                                * (MAX_LONG_AXIS - MIN_LONG_AXIS);
                float shortAxis = longAxis
                        * (0.48f + random.nextFloat() * 0.34f);
                if (random.nextBoolean()) {
                    sprite.setSize(longAxis, shortAxis);
                } else {
                    sprite.setSize(shortAxis, longAxis);
                }
                sprite.setAngle(random.nextFloat() * 360f);
                sprite.setNormalBlend();

                float x = -halfWidth - EDGE_PADDING
                        + random.nextFloat()
                                * (mapWidth + EDGE_PADDING * 2f);
                float y = -halfHeight - EDGE_PADDING
                        + random.nextFloat()
                                * (mapHeight + EDGE_PADDING * 2f);
                float alpha = 0.065f + random.nextFloat() * 0.075f;
                banks[index] = new CloudBank(
                        sprite,
                        new Vector2f(x, y),
                        alpha,
                        longAxis);
            }
        }

        private static void selectRandomAtlasCell(
                SpriteAPI sprite, Random random) {
            float cellWidth = sprite.getTexWidth() / ATLAS_COLUMNS;
            float cellHeight = sprite.getTexHeight() / ATLAS_ROWS;
            int cell = random.nextInt(ATLAS_COLUMNS * ATLAS_ROWS);
            int column = cell % ATLAS_COLUMNS;
            int row = cell / ATLAS_COLUMNS;
            sprite.setTexX(sprite.getTexX() + column * cellWidth);
            sprite.setTexY(sprite.getTexY() + row * cellHeight);
            sprite.setTexWidth(cellWidth);
            sprite.setTexHeight(cellHeight);
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.CLOUD_LAYER);
        }

        @Override
        public float getRenderRadius() {
            return renderRadius;
        }

        @Override
        public boolean isExpired() {
            return false;
        }

        @Override
        public void render(
                CombatEngineLayers layer, ViewportAPI viewport) {
            if (layer != CombatEngineLayers.CLOUD_LAYER) return;
            for (CloudBank bank : banks) {
                if (!viewport.isNearViewport(
                        bank.location, bank.cullingRadius)) continue;
                bank.sprite.setAlphaMult(
                        bank.alpha * viewport.getAlphaMult());
                bank.sprite.renderAtCenter(
                        bank.location.x, bank.location.y);
            }
        }
    }

    private static final class CloudBank {
        private final SpriteAPI sprite;
        private final Vector2f location;
        private final float alpha;
        private final float cullingRadius;

        private CloudBank(
                SpriteAPI sprite,
                Vector2f location,
                float alpha,
                float cullingRadius) {
            this.sprite = sprite;
            this.location = location;
            this.alpha = alpha;
            this.cullingRadius = cullingRadius;
        }
    }
}
