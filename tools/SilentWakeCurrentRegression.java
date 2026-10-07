import chiefnavigator.campaign.SilentWakeCurrentTerrain;
import chiefnavigator.quest.OdysseyExpanseSystem;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.loading.TerrainSpecAPI;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.util.vector.Vector2f;

public class SilentWakeCurrentRegression extends WallResearchRegression {
    public static void main(String[] args) {
        List<CampaignFleetAPI> fleets = new ArrayList<>();
        LocationAPI location = mock(LocationAPI.class,
                (n,a) -> n.equals("getFleets") ? fleets : null);
        MemoryAPI memory = mock(MemoryAPI.class, (n,a) -> null);
        TerrainSpecAPI spec = mock(TerrainSpecAPI.class, (n,a) -> null);
        Global.setSettings(mock(SettingsAPI.class, (n,a) -> {
            if (n.equals("getColor")) return Color.WHITE;
            if (n.equals("getTerrainSpec")) return spec;
            return null;
        }));
        Vector2f position = new Vector2f(8000f, 8000f);
        Vector2f destination = new Vector2f(18000f,18000f);
        Vector2f velocity = new Vector2f();
        boolean[] paused = {false}, transition = {false};
        CampaignFleetAPI player = mock(CampaignFleetAPI.class, (n,a) -> {
            switch (n) {
                case "getLocation": return position;
                case "getMoveDestination": return destination;
                case "getVelocity": return velocity;
                case "getVelocityFromMovementModule": return velocity;
                case "getContainingLocation": return location;
                case "getAcceleration": return 100f;
                case "getMemoryWithoutUpdate": return memory;
                case "isInHyperspaceTransition": return transition[0];
                case "setVelocity": velocity.set((Float)a[0], (Float)a[1]); break;
                case "setLocation": position.set((Float)a[0], (Float)a[1]); break;
                case "setMoveDestination": destination.set((Float)a[0], (Float)a[1]); break;
            }
            return null;
        });
        fleets.add(player);
        CampaignUIAPI ui = mock(CampaignUIAPI.class,
                (n,a) -> n.equals("isFollowingDirectCommand") ? true : null);
        CampaignClockAPI clock = mock(CampaignClockAPI.class, (n,a) -> {
            if (n.equals("getSecondsPerDay")) return 10f;
            if (n.equals("convertToDays")) return (Float)a[0] / 10f;
            return null;
        });
        Global.setSector(mock(SectorAPI.class, (n,a) -> {
            if (n.equals("getPlayerFleet")) return player;
            if (n.equals("getClock")) return clock;
            if (n.equals("getCampaignUI")) return ui;
            if (n.equals("isPaused")) return paused[0];
            return null;
        }));
        SilentWakeCurrentTerrain current = new SilentWakeCurrentTerrain();
        SectorEntityToken terrain = mock(SectorEntityToken.class, (n,a) -> {
            if (n.equals("getLocation")) return new Vector2f();
            if (n.equals("getContainingLocation")) return location;
            if (n.equals("isInCurrentLocation")) return true;
            if (n.equals("getId")) return SilentWakeCurrentTerrain.ID;
            return null;
        });
        current.init(SilentWakeCurrentTerrain.ID, terrain, null);
        // Campaign terrain registration queries these before the first tick.
        check(current.getActiveLayers() != null, "Terrain registration supplies layers");
        check(current.getRenderRange() > Math.hypot(11500f, 10500f),
                "Terrain effects reach river corners");
        current.advance(0f);
        current.advance(.1f);
        check(Math.abs(position.y - 7997f) < .001f, "BaseTerrain dispatch moves fleet");
        position.set(11400f,10400f);
        velocity.set(0f,0f);
        current.advance(.1f);
        check(position.y < 10400f, "BaseTerrain range admits river corners");
        position.set(8000f,8000f);
        velocity.set(0f,0f);
        current.applyEffect(player, .01f);
        check(position.equals(new Vector2f(8000f,7997f)), "Continuous drift with day conversion");
        check(destination.equals(new Vector2f(18000f,18000f)), "Active destination unchanged");
        current.applyEffect(player, 1f);
        check(position.y == 7697f, "Current is thirty units per second");
        velocity.set(40f, -200f);
        current.applyEffect(player, 1f);
        check(velocity.equals(new Vector2f(40f,-200f)), "Downstream thrust preserved");
        velocity.set(40f,200f);
        current.applyEffect(player,.01f);
        check(velocity.equals(new Vector2f(40f,200f)), "Upstream thrust preserved");
        for (float[] body : OdysseyExpanseSystem.getSilentWakeObstacles()) {
            position.set(body[0],body[1]-body[2]*2f);
            float shelteredY = position.y;
            velocity.set(0f,0f);
            current.applyEffect(player,1f);
            check(position.y == shelteredY, "Every obstacle shelters its lee");
        }
        position.set(12000f,0f);
        current.applyEffect(player,1f);
        check(position.y == 0f, "Outside river unaffected");
        position.set(8000f,8000f);
        paused[0]=true;
        current.applyEffect(player,1f);
        check(position.y == 8000f, "Paused fleet unaffected");
        paused[0]=false; transition[0]=true;
        current.applyEffect(player,1f);
        check(position.y == 8000f, "Transition unaffected");
        check(!current.canPlayerHoldStationIn(), "Engine station holding disabled in current");
        System.out.println("PASS: terrain registration, BaseTerrain dispatch, drift, timing, thrust, shelter, bounds, pause and transition");
    }
}
