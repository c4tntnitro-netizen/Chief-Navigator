import chiefnavigator.campaign.SilentWakeCurrentTerrain;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.campaign.fleet.SmoothMovementModule;
import org.lwjgl.util.vector.Vector2f;

/** Runs the real engine controller between terrain impulses, without reflection. */
public class SilentWakeMovementRegression extends WallResearchRegression {
    static float travel(float step, float acceleration, int controlMode, boolean sheltered) {
        return travel(step, acceleration, controlMode, sheltered, true);
    }

    static float travel(float step, float acceleration, int controlMode, boolean sheltered, boolean currentOn) {
        SmoothMovementModule movement = new SmoothMovementModule(acceleration, 500f);
        movement.getLocation().set(sheltered ? 0f : 8000f, sheltered ? -1520f : 2000f);
        float startY = movement.getLocation().y;
        Vector2f reported = new Vector2f();
        Vector2f destination = new Vector2f(movement.getLocation());
        if (controlMode > 0 && controlMode != 4) destination.y += 10000f;
        LocationAPI location = mock(LocationAPI.class, (n,a) -> null);
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class, (n,a) -> {
            switch(n) {
                case "getLocation": return movement.getLocation();
                case "getVelocity": return reported;
                case "getVelocityFromMovementModule": return movement.getVelocity();
                case "getMoveDestination": return destination;
                case "setVelocity": movement.getVelocity().set((Float)a[0], (Float)a[1]); break;
                case "setLocation": movement.getLocation().set((Float)a[0], (Float)a[1]); break;
                case "setMoveDestination": destination.set((Float)a[0], (Float)a[1]); break;
                case "getAcceleration": return movement.getAcceleration();
                case "getContainingLocation": return location;
                case "isAIMode": return controlMode == 1;
            }
            return null;
        });
        CampaignClockAPI clock = mock(CampaignClockAPI.class,
                (n,a) -> n.equals("getSecondsPerDay") ? 10f : null);
        CampaignUIAPI ui = mock(CampaignUIAPI.class, (n,a) -> {
            // The live engine leaves this flag true after arriving and stopping.
            if(n.equals("isFollowingDirectCommand")) return true;
            if(n.equals("isPlayerFleetFollowingMouse")) return controlMode == 3;
            return null;
        });
        Global.setSector(mock(SectorAPI.class, (n,a) -> {
            if(n.equals("getPlayerFleet")) return fleet;
            if(n.equals("getClock")) return clock;
            if(n.equals("getCampaignUI")) return ui;
            return null;
        }));
        SilentWakeCurrentTerrain terrain = new SilentWakeCurrentTerrain();
        terrain.init(SilentWakeCurrentTerrain.ID,
                mock(SectorEntityToken.class, (n,a) -> {
                    if(n.equals("getLocation")) return new Vector2f();
                    if(n.equals("getContainingLocation")) return location;
                    return null;
                }), null);
        for(int i=0;i<Math.round(10f/step);i++) {
            if (controlMode == 4 && i == Math.round(3f/step)) destination.y += 10000f;
            if (currentOn) terrain.applyEffect(fleet, step/10f);
            movement.advance(null, destination, new Vector2f(), step);
            reported.set(movement.getVelocity());
        }
        return movement.getLocation().y - startY;
    }

    public static void main(String[] args) {
        Global.setSettings(mock(SettingsAPI.class,
                (n,a) -> n.equals("getColor") ? java.awt.Color.WHITE : null));
        for(float step : new float[]{1f/120f, 1f/60f, 1f/15f, .1f, .25f}) {
            for(float acceleration : new float[]{50f, 100f, 500f, 2000f}) {
                float delta = travel(step, acceleration, 0, false);
                check(Math.abs(delta + 300f) < 3f,
                        "Idle drift survives controller: step=" + step
                                + ", acceleration=" + acceleration + ", travel=" + delta);
            }
        }
        for(int control=1;control<=3;control++) {
            float withCurrent = travel(1f/60f, 100f, control, false);
            float withoutCurrent = travel(1f/60f, 100f, control, false, false);
            check(withCurrent > 1000f,
                    "Upstream steering remains possible: mode=" + control);
            check(Math.abs((withoutCurrent - withCurrent) - 300f) < 3f,
                    "Same current while steering: mode=" + control + ", difference="
                            + (withoutCurrent - withCurrent));
        }
        check(travel(1f/60f, 100f, 4, false) > 500f,
                "New destination releases idle drift even with direct flag unchanged");
        check(Math.abs(travel(1f/60f, 100f, 0, true)) < .01f,
                "Shelter prevents idle drift");
        System.out.println("PASS: real engine movement, 20 idle timing/acceleration cases, "
                + "stale direct flag, changed destination, upstream steering and shelter. "
                + "Idle southward travel in 10s: " + -travel(1f/60f, 100f, 0, false));
    }
}
