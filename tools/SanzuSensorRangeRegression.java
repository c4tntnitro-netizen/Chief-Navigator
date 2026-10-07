import chiefnavigator.campaign.SilentWakeCurrentTerrain;
import chiefnavigator.quest.OdysseyExpanseSystem;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.loading.TerrainSpecAPI;
import com.fs.starfarer.campaign.fleet.MutableFleetStats;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.util.vector.Vector2f;

/** Exercises actual terrain ticks and native temporary fleet-stat expiration. */
public class SanzuSensorRangeRegression extends WallResearchRegression {
    static final class Fleet {
        final MutableFleetStats stats = new MutableFleetStats();
        final Vector2f position;
        final CampaignFleetAPI api;
        LocationAPI location;
        boolean transitioning;
        boolean station;

        Fleet(LocationAPI location, float x, float y) {
            this.location = location;
            position = new Vector2f(x, y);
            MemoryAPI memory = mock(MemoryAPI.class, (n, a) -> null);
            api = mock(CampaignFleetAPI.class, (n, a) -> {
                switch (n) {
                    case "getStats": return stats;
                    case "getLocation": return position;
                    case "getContainingLocation": return this.location;
                    case "getMemoryWithoutUpdate": return memory;
                    case "isInHyperspaceTransition": return transitioning;
                    case "isStationMode": return station;
                    case "setLocation": position.set((Float) a[0], (Float) a[1]); break;
                }
                return null;
            });
        }

        float sensorRange() { return stats.getSensorRangeMod().computeEffective(1000f); }
    }

    public static void main(String[] args) {
        TerrainSpecAPI spec = mock(TerrainSpecAPI.class, (n, a) -> null);
        Global.setSettings(mock(SettingsAPI.class, (n, a) -> {
            if (n.equals("getColor")) return Color.WHITE;
            if (n.equals("getTerrainSpec")) return spec;
            return null;
        }));
        List<CampaignFleetAPI> fleets = new ArrayList<>();
        String[] locationId = {OdysseyExpanseSystem.SILENT_WAKE_ID};
        LocationAPI sanzu = mock(LocationAPI.class, (n, a) -> {
            if (n.equals("getId")) return locationId[0];
            if (n.equals("getFleets")) return fleets;
            return null;
        });
        LocationAPI elsewhere = mock(LocationAPI.class, (n, a) -> null);
        Fleet player = new Fleet(sanzu, 8000f, 8000f);
        Fleet distant = new Fleet(sanzu, 100000f, 100000f);
        Fleet sheltered = new Fleet(sanzu, 0f, -1520f);
        Fleet station = new Fleet(sanzu, 100f, 100f);
        station.station = true;
        Fleet foreign = new Fleet(elsewhere, 8000f, 8000f);
        Fleet transition = new Fleet(sanzu, 8000f, 8000f);
        transition.transitioning = true;
        Fleet[] fixtures = {player, distant, sheltered, station, foreign, transition};
        for (Fleet fleet : fixtures) fleets.add(fleet.api);

        boolean[] paused = {false};
        CampaignClockAPI clock = mock(CampaignClockAPI.class, (n, a) -> {
            if (n.equals("getSecondsPerDay")) return 10f;
            if (n.equals("convertToDays")) return (Float) a[0] / 10f;
            return null;
        });
        Global.setSector(mock(SectorAPI.class, (n, a) -> {
            if (n.equals("getPlayerFleet")) return player.api;
            if (n.equals("getClock")) return clock;
            if (n.equals("isPaused")) return paused[0];
            return null;
        }));
        SilentWakeCurrentTerrain terrain = new SilentWakeCurrentTerrain();
        terrain.init(SilentWakeCurrentTerrain.ID, mock(SectorEntityToken.class, (n, a) -> {
            if (n.equals("getLocation")) return new Vector2f();
            if (n.equals("getContainingLocation")) return sanzu;
            if (n.equals("getId")) return SilentWakeCurrentTerrain.ID;
            return null;
        }), null);

        terrain.advance(0f);
        terrain.advance(-1f);
        check(player.sensorRange() == 1000f, "Load/negative ticks do not apply modifiers");
        paused[0] = true;
        terrain.advance(.1f);
        check(player.sensorRange() == 1000f, "Paused ticks do not apply modifiers");
        paused[0] = false;
        terrain.advance(.1f);
        for (Fleet fleet : new Fleet[]{player, distant, sheltered, station}) {
            check(fleet.sensorRange() == 500f, "Every local fleet has half sensor range");
            check(fleet.stats.getFleetwideMaxBurnMod().computeEffective(10f) == 10f,
                    "Sensor haze does not add a burn penalty");
            check(fleet.stats.getDetectedRangeMod().computeEffective(1000f) == 1000f,
                    "Sensor-range request does not change detectability");
        }
        check(player.position.y == 7997f, "Existing exposed player drift is preserved");
        check(sheltered.position.y == -1520f, "Shelter still blocks movement only");
        check(distant.position.y == 100000f, "Outside-river NPC movement is untouched");
        check(foreign.sensorRange() == 1000f, "Foreign locations remain unaffected");
        check(transition.sensorRange() == 1000f, "Hyperspace transitions remain unaffected");

        for (int i = 0; i < 10; i++) terrain.advance(.1f);
        check(player.sensorRange() == 500f, "Repeated refreshes do not stack");
        player.stats.getSensorRangeMod().modifyMult("other_effect", 1.5f);
        terrain.advance(.1f);
        check(player.sensorRange() == 750f, "Other sensor modifiers compose normally");
        player.location = elsewhere;
        terrain.advance(.1f);
        player.stats.advance(.11f);
        check(player.sensorRange() == 1500f, "Leaving expires only the Sanzu modifier");
        distant.stats.advance(.11f);
        locationId[0] = "other_system";
        terrain.advance(.1f);
        check(distant.sensorRange() == 1000f, "Terrain cannot apply haze in other systems");
        System.out.println("PASS: whole-system sensor range, sheltered/distant/NPC/station fleets, scope, nonstacking, native expiration and preserved movement");
    }
}
