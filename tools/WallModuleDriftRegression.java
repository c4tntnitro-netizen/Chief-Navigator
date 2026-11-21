package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.lwjgl.util.vector.Vector2f;

/**
 * Ithaca wall modules drifting at 20 units/second in the background.
 *
 * Engine behaviour modelled from RC8 bytecode:
 * - BaseLocation.advance moves every entity by its own stored velocity
 *   (location += getVelocity() * amount);
 * - CampaignFleet.setVelocity writes only the movement module's velocity;
 *   the module copies it back to the stored velocity only while the fleet
 *   has a move destination, and parked stations have none;
 * - a location the player is not in is advanced in chunks of about two
 *   seconds (activeThisFrame), so a 20 u/s stored velocity shows up as a
 *   40-unit jump at each re-pin.
 * The north module's stored velocity below is the value in the user's save.
 */
public final class WallModuleDriftRegression {
    private static final String FOB_ID = "chief_navigator_fob_ithaca";
    private static final String FOB_TYPE = "chief_navigator_fob_ithaca_campaign";
    private static final String FOB_OWNER = "$chief_navigator_fob_ithaca_owned_v1";
    private static final float CENTER_X = 4500f;
    private static final float CENTER_Y = 0f;
    private static final float RADIUS = 2150f;

    interface Call { Object invoke(String name, Object[] args); }

    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[] {type}, (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("hashCode")) return System.identityHashCode(proxy);
                    if (name.equals("equals")) return proxy == args[0];
                    Object result = call.invoke(name, args);
                    if (result != null) return result;
                    Class<?> r = method.getReturnType();
                    if (r == boolean.class) return false;
                    if (r == int.class) return 0;
                    if (r == float.class) return 0f;
                    if (r == long.class) return 0L;
                    if (r == double.class) return 0d;
                    return null;
                }));
    }

    private static int failures;

    static void check(boolean ok, String message) {
        if (!ok) failures++;
        System.out.println((ok ? "ok:   " : "FAIL: ") + message);
    }

    static MemoryAPI memory(Map<String, Object> values) {
        return mock(MemoryAPI.class, (n, a) -> {
            if (n.equals("getBoolean")) return Boolean.TRUE.equals(values.get((String) a[0]));
            if (n.equals("getString")) return (String) values.get((String) a[0]);
            if (n.equals("get")) return values.get((String) a[0]);
            if (n.equals("contains")) return values.containsKey((String) a[0]);
            if (n.equals("set")) { values.put((String) a[0], a[1]); return null; }
            return null;
        });
    }

    /** A parked wall-module fleet with separate stored and module velocities. */
    static final class Station {
        final String id;
        final Vector2f pin;
        final Vector2f loc;
        final Vector2f stored = new Vector2f();
        final Vector2f module = new Vector2f();
        final Map<String, Object> mem = new HashMap<>();
        CampaignFleetAPI fleet;

        Station(String section, float x, float y) {
            id = "chief_navigator_ithaca_station_" + section;
            pin = new Vector2f(x, y);
            loc = new Vector2f(x, y);
            mem.put(IthacaSectionEncounter.MARKER, true);
            mem.put(IthacaSectionEncounter.SECTION_KEY, section);
        }

        /** BaseLocation.advance for an entity with no move destination. */
        void engineStep(float seconds) {
            loc.x += stored.x * seconds;
            loc.y += stored.y * seconds;
        }

        float drift() {
            return Vector2f.sub(loc, pin, null).length();
        }
    }

    static final class World {
        final Station[] stations = {
            new Station("north", CENTER_X, CENTER_Y + RADIUS),
            new Station("east", CENTER_X + RADIUS, CENTER_Y),
            new Station("south", CENTER_X, CENTER_Y - RADIUS)
        };
        final Map<String, Object> fobMem = new HashMap<>();
        final SectorEntityToken center;

        World() {
            fobMem.put(FOB_OWNER, true);
            Map<String, SectorEntityToken> byId = new HashMap<>();
            LocationAPI location = mock(LocationAPI.class, (n, a) ->
                    n.equals("getEntityById") ? byId.get((String) a[0]) : null);
            Vector2f centerLoc = new Vector2f(CENTER_X, CENTER_Y);
            MemoryAPI fobMemory = memory(fobMem);
            center = mock(SectorEntityToken.class, (n, a) -> {
                switch (n) {
                    case "getId": return FOB_ID;
                    case "getCustomEntityType": return FOB_TYPE;
                    case "getMemoryWithoutUpdate": return fobMemory;
                    case "getContainingLocation": return location;
                    case "getLocation": return centerLoc;
                    default: return null;
                }
            });
            byId.put(FOB_ID, center);
            for (Station s : stations) {
                MemoryAPI stationMemory = memory(s.mem);
                s.fleet = mock(CampaignFleetAPI.class, (n, a) -> {
                    switch (n) {
                        case "getId": return s.id;
                        case "getMemoryWithoutUpdate": return stationMemory;
                        case "getContainingLocation": return location;
                        case "getLocation": return s.loc;
                        case "setLocation": s.loc.set((Float) a[0], (Float) a[1]); return null;
                        case "getVelocity": return s.stored;
                        case "setVelocity": s.module.set((Float) a[0], (Float) a[1]); return null;
                        default: return null;
                    }
                });
                byId.put(s.id, s.fleet);
            }
            newSector();
        }

        void newSector() {
            CampaignFleetAPI player = mock(CampaignFleetAPI.class, (n, a) -> null);
            Global.setSector(mock(SectorAPI.class, (n, a) ->
                    n.equals("getPlayerFleet") ? player : null));
        }

        /**
         * One background chunk of Ashen Verge, then our re-pin. Returns the
         * largest drift the chunk caused, measured before the re-pin.
         */
        float cycle(float seconds) {
            float max = 0f;
            for (Station s : stations) {
                s.engineStep(seconds);
                max = Math.max(max, s.drift());
            }
            IthacaSectionEncounter.anchorCampaignStations(center);
            return max;
        }
    }

    static final class WarnCounter extends AppenderSkeleton {
        int count;
        @Override protected void append(LoggingEvent event) {
            if (String.valueOf(event.getMessage()).contains("ITHACA_STATION_ANCHOR")) count++;
        }
        @Override public void close() { }
        @Override public boolean requiresLayout() { return false; }
    }

    public static void main(String[] args) {
        SectorAPI prev = Global.getSector();
        WarnCounter warnings = new WarnCounter();
        Logger.getLogger(IthacaSectionEncounter.class).addAppender(warnings);
        try {
            // The engine model reproduces the reported 40-unit jumps when
            // only the module velocity is cleared (the old re-pin).
            Station model = new Station("north", 0f, 0f);
            model.stored.set(15.69312f, -12.398626f);
            model.fleet = null;
            model.module.set(0f, 0f);
            model.engineStep(2f);
            check(Math.abs(model.drift() - 40f) < 0.01f,
                    "engine model: stored velocity 20 u/s over a 2 s chunk drifts 40 units (as logged)");

            World w = new World();
            w.stations[0].stored.set(15.69312f, -12.398626f);
            w.stations[1].stored.set(3.4382434f, 19.702246f);
            w.stations[2].stored.set(-19.071522f, 6.0230417f);
            w.cycle(0f);
            boolean cleared = true;
            for (Station s : w.stations) {
                cleared &= s.stored.length() == 0f && s.module.length() == 0f;
            }
            check(cleared, "re-pin clears both the stored and the module velocity");
            float worst = 0f;
            for (int i = 0; i < 50; i++) {
                worst = Math.max(worst, w.cycle(2f));
            }
            check(worst == 0f, "50 background chunks: no drift at all (worst " + worst + ")");
            check(warnings.count == 0, "no warnings once the drift is gone");

            // A genuine push (a hostile fleet pressing in) is still corrected
            // and logged, once per station per load.
            Station north = w.stations[0];
            north.loc.set(north.pin.x + 100f, north.pin.y);
            IthacaSectionEncounter.anchorCampaignStations(w.center);
            check(north.drift() == 0f && warnings.count == 1,
                    "a real displacement is re-pinned and logged");
            for (int i = 0; i < 5; i++) {
                north.loc.set(north.pin.x + 100f, north.pin.y);
                IthacaSectionEncounter.anchorCampaignStations(w.center);
            }
            check(north.drift() == 0f && warnings.count == 1,
                    "further displacements are re-pinned without more log lines");
            Station east = w.stations[1];
            east.loc.set(east.pin.x, east.pin.y + 100f);
            IthacaSectionEncounter.anchorCampaignStations(w.center);
            check(warnings.count == 2, "each station logs its own first displacement");
            w.newSector();
            north.loc.set(north.pin.x + 100f, north.pin.y);
            IthacaSectionEncounter.anchorCampaignStations(w.center);
            check(warnings.count == 3, "a newly loaded save logs again, once");

            Station gate = new Station("west", 0f, 0f);
            gate.stored.set(10f, 10f);
            gate.module.set(5f, 5f);
            World holder = w;
            CampaignFleetAPI gateFleet = mock(CampaignFleetAPI.class, (n, a) -> {
                if (n.equals("getVelocity")) return gate.stored;
                if (n.equals("setVelocity")) { gate.module.set((Float) a[0], (Float) a[1]); return null; }
                return null;
            });
            IthacaSectionEncounter.stopCampaignDrift(gateFleet);
            check(gate.stored.length() == 0f && gate.module.length() == 0f && holder != null,
                    "stopCampaignDrift (also used for Breach Defense Station) clears both");
        } finally {
            Logger.getLogger(IthacaSectionEncounter.class).removeAppender(warnings);
            Global.setSector(prev);
        }
        System.out.println(failures == 0 ? "ALL PASSED" : failures + " FAILED");
        if (failures != 0) System.exit(1);
    }
}
