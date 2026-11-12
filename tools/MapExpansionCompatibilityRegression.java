package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import java.awt.Color;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.lwjgl.util.vector.Vector2f;

/** Resized-map placement, saved-pocket continuity, and actual WH marker filtering. */
public final class MapExpansionCompatibilityRegression {
    private static float width = 164000f;
    private static float height = 104000f;
    private static LocationAPI current;

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void near(float expected, float actual, String message) {
        check(Math.abs(expected - actual) < 0.02f,
                message + ": expected " + expected + ", got " + actual);
    }

    private static void checkTroyPlacement() {
        for (float[] scale : new float[][] {{0.5f,0.5f}, {1f,1f},
                {1.5f,1.5f}, {3f,3f}, {2f,0.75f}}) {
            width = 164000f * scale[0];
            height = 104000f * scale[1];
            Vector2f troy = TroyArrivalScript.getInitialHyperspaceLocation();
            near(59000f * scale[0], troy.x, "Troy must follow horizontal map scale");
            near(-45000f * scale[1], troy.y, "Troy must follow vertical map scale");
            check(Math.abs(troy.x) < width / 2f && Math.abs(troy.y) < height / 2f,
                    "Troy must stay reachable inside the ordinary Sector bounds");
        }
    }

    private static void checkSavedPocket() throws Exception {
        OdysseyDuplicateJumpRegression.World alpha =
                new OdysseyDuplicateJumpRegression.World(OdysseyExpanseSystem.SYSTEM_ID);
        // Use the production ownership key rather than assuming its spelling.
        alpha.memory.values.put(OdysseyDuplicateJumpRegression.key(
                "AUTHORED_SYSTEM_MARKER"), true);
        Global.setSector(OdysseyDuplicateJumpRegression.mock(SectorAPI.class, (name, args) -> {
            if (name.equals("getStarSystems")) return Arrays.asList(alpha.system());
            throw new AssertionError("Pocket lookup must not mutate campaign state: " + name);
        }));
        for (float scale : new float[] {0.5f, 1f, 3f}) {
            width = 164000f * scale;
            height = 104000f * scale;
            Vector2f pocket = OdysseyExpanseSystem.getOdysseyHoleCenter();
            Method initialLocation = OdysseyExpanseSystem.class.getDeclaredMethod("getExpanseLocation");
            initialLocation.setAccessible(true);
            Vector2f initial = (Vector2f) initialLocation.invoke(null);
            near(width * 0.9f + 2000f, initial.x,
                    "Initial generation must not adopt a native generator's temporary coordinates");
            near(height * -0.9f + 1000f, initial.y,
                    "Initial placement and saved-pocket lookup must have separate ownership");
            near(147000f, pocket.x, "Saved pocket must remain with its saved Alpha system");
            near(-93000f, pocket.y, "Global map changes must not shift the saved boundary");
            check(OdysseyExpanseSystem.isInOdysseyHyperspace(pocket),
                    "Saved pocket must remain playable after dimensions change");
            Vector2f clamped = OdysseyExpanseSystem.clampToOdysseyHyperspace(
                    new Vector2f(pocket.x + 1000000f, pocket.y), 100f);
            check(OdysseyExpanseSystem.isInOdysseyHyperspace(clamped),
                    "Containment must use the same saved pocket as navigation");
        }
        Vector2f copy = OdysseyExpanseSystem.getOdysseyHoleCenter();
        copy.set(0f, 0f);
        near(149000f, alpha.position.x, "Returned centers must not alias saved positions");
        check(alpha.memory.writes == 0 && alpha.removed.isEmpty(),
                "Pocket lookup must preserve all serialized objects and flags");

        Global.setSector(OdysseyDuplicateJumpRegression.mock(SectorAPI.class, (name, args) ->
                name.equals("getStarSystems") ? Collections.emptyList() : null));
        near(width * 0.9f, OdysseyExpanseSystem.getOdysseyHoleCenter().x,
                "Initial pocket must still use the new campaign's dimensions");
    }

    private static void checkMapTargets(boolean withWideHorizons) throws Exception {
        OdysseyDuplicateJumpRegression.World troy =
                new OdysseyDuplicateJumpRegression.World(TroyArrivalScript.SYSTEM_ID);
        OdysseyDuplicateJumpRegression.World hyper =
                new OdysseyDuplicateJumpRegression.World(null);
        OdysseyDuplicateJumpRegression.World ecw =
                new OdysseyDuplicateJumpRegression.World("achean");
        troy.memory.values.put("$chief_navigator_troy_authored_system_v1", true);
        OdysseyDuplicateJumpRegression.Jump local = new OdysseyDuplicateJumpRegression.Jump(
                troy, TroyArrivalScript.ACCESS_JUMP_ID, "Troy Access", -2000f, 0f);
        OdysseyDuplicateJumpRegression.Jump entrance = new OdysseyDuplicateJumpRegression.Jump(
                hyper, TroyArrivalScript.ACCESS_HYPER_JUMP_ID, "Waypoint Troy", 59000f, -45000f);
        local.memory.values.put("$chief_navigator_troy_access_endpoint", true);
        entrance.memory.values.put("$chief_navigator_troy_access_endpoint", true);
        local.link(entrance);
        entrance.link(local);
        SectorEntityToken station = OdysseyDuplicateJumpRegression.mock(
                SectorEntityToken.class, (name, args) -> {
                    if (name.equals("getContainingLocation")) return troy.api;
                    throw new AssertionError("Map lookup must not mutate its target: " + name);
                });
        SectorEntityToken foreign = OdysseyDuplicateJumpRegression.mock(
                SectorEntityToken.class, (name, args) ->
                        name.equals("getContainingLocation") ? ecw.api : null);
        current = hyper.api;
        Global.setSector(OdysseyDuplicateJumpRegression.mock(SectorAPI.class, (name, args) -> {
            if (name.equals("getStarSystems")) return Arrays.asList(ecw.system(), troy.system());
            if (name.equals("getHyperspace")) return hyper.api;
            if (name.equals("getCurrentLocation")) return current;
            throw new AssertionError("Map lookup must preserve campaign state: " + name);
        }));
        check(TroyArrivalScript.getMapTarget(station) == entrance.api,
                "Outside Troy, Intel must center on the actual two-sided entrance");
        check(TroyArrivalScript.getMapTarget(foreign) == foreign,
                "An ECW or other foreign objective must remain untouched");
        check(TroyArrivalScript.getMapTarget(null) == null, "Missing objectives stay missing");
        current = troy.api;
        check(TroyArrivalScript.getMapTarget(station) == station,
                "Inside Troy, Intel must continue pointing at the anchorage");
        current = hyper.api;
        if (withWideHorizons) {
            Method markers = Class.forName("org.widehorizons.ui.scripts.WHSystemDiscovery")
                    .getDeclaredMethod("mapMarkersFor", StarSystemAPI.class);
            markers.setAccessible(true);
            List<?> whMarkers = (List<?>) markers.invoke(null, troy.system());
            check(!whMarkers.contains(entrance.api),
                    "The real WH discovery pass must leave Troy's custom access outside its marker set");
            check(TroyArrivalScript.getMapTarget(station) == entrance.api,
                    "WH marker filtering must not break Troy's Intel destination");
        }
        hyper.entities.remove(entrance.api);
        check(TroyArrivalScript.getMapTarget(station) == station,
                "A missing entrance must not remove a valid saved objective from Intel");
        hyper.entities.add(entrance.api);
        entrance.memory.values.clear();
        check(TroyArrivalScript.getMapTarget(station) == station,
                "An unowned entrance must preserve the existing objective as a fallback");
        entrance.memory.values.put("$chief_navigator_troy_access_endpoint", true);
        entrance.destinations.clear();
        check(TroyArrivalScript.getMapTarget(station) == station,
                "A broken route must preserve the objective without repairing saved topology");
        check(troy.removed.isEmpty() && hyper.removed.isEmpty()
                        && troy.memory.writes == 0 && entrance.memory.writes == 0,
                "Map lookup must never recreate or rewrite saved topology");
    }

    public static void main(String[] args) throws Exception {
        SettingsAPI priorSettings = Global.getSettings();
        SectorAPI priorSector = Global.getSector();
        try {
            Global.setSettings(OdysseyDuplicateJumpRegression.mock(SettingsAPI.class, (name, values) -> {
                if (name.equals("getFloat")) return "sectorWidth".equals(values[0]) ? width : height;
                if (name.equals("getColor")) return Color.WHITE;
                return null;
            }));
            checkTroyPlacement();
            checkSavedPocket();
            checkMapTargets(args.length > 0 && args[0].equals("with-wide-horizons"));
            System.out.println("Map expansion compatibility regression passed.");
        } finally {
            Global.setSettings(priorSettings);
            Global.setSector(priorSector);
        }
    }
}
