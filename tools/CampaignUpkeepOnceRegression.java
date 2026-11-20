package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Two November 19, 2026 performance fixes:
 * 1. The post-defeat Ungaikyo cleanup stops sweeping the sector after one clean
 *    pass (per loaded sector), but keeps retrying while something blocks it.
 * 2. The Ithaca Spartan guard upkeep runs once per campaign tick, not once per
 *    Lacedaemon patrol.
 */
public final class CampaignUpkeepOnceRegression {
    private interface Call { Object invoke(String name, Object[] args); }

    private static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (proxy, method, args) -> {
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == args[0];
                    Object value = call.invoke(method.getName(), args);
                    if (value != null) return value;
                    Class<?> result = method.getReturnType();
                    if (result == boolean.class) return false;
                    if (result == int.class) return 0;
                    if (result == long.class) return 0L;
                    if (result == float.class) return 0f;
                    if (result == double.class) return 0d;
                    return null;
                }));
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        System.out.println("PASS " + message);
    }

    public static void main(String[] args) {
        cleanupStopsAfterCleanPass();
        guardUpkeepOncePerTick();
        System.out.println("CampaignUpkeepOnceRegression: all checks passed");
    }

    private static void cleanupStopsAfterCleanPass() {
        int[] sweeps = {0};
        SectorEntityToken[] blocker = {null};
        List<StarSystemAPI> systems = new ArrayList<StarSystemAPI>();
        for (int i = 0; i < 3; i++) {
            systems.add(mock(StarSystemAPI.class, (n, a) ->
                    n.equals("getFleets") ? Collections.emptyList() : null));
        }
        LocationAPI hyperspace = mock(LocationAPI.class, (n, a) -> {
            if (n.equals("getFleets")) return Collections.emptyList();
            if (n.equals("getEntityById")) return blocker[0];
            return null;
        });
        Call sectorCalls = (n, a) -> {
            if (n.equals("getStarSystems")) { sweeps[0]++; return new ArrayList<StarSystemAPI>(systems); }
            if (n.equals("getHyperspace")) return hyperspace;
            return null;
        };

        Global.setSector(mock(SectorAPI.class, sectorCalls));
        blocker[0] = mock(SectorEntityToken.class, (n, a) -> null);
        OdysseyPredatorScript.runUngaikyoCleanupUntilClean();
        int blocked = sweeps[0];
        OdysseyPredatorScript.runUngaikyoCleanupUntilClean();
        check(blocked > 0 && sweeps[0] == 2 * blocked,
                "A blocked cleanup keeps retrying");

        blocker[0] = null;
        sweeps[0] = 0;
        OdysseyPredatorScript.runUngaikyoCleanupUntilClean();
        check(sweeps[0] > 0, "A clean pass still sweeps once");
        int clean = sweeps[0];
        for (int i = 0; i < 50; i++) OdysseyPredatorScript.runUngaikyoCleanupUntilClean();
        check(sweeps[0] == clean, "No further sweeps after a clean pass");

        Global.setSector(mock(SectorAPI.class, sectorCalls));
        OdysseyPredatorScript.runUngaikyoCleanupUntilClean();
        check(sweeps[0] > clean, "A newly loaded sector is checked again");
    }

    private static void guardUpkeepOncePerTick() {
        long[] now = {1000L};
        CampaignClockAPI clock = mock(CampaignClockAPI.class,
                (n, a) -> n.equals("getTimestamp") ? now[0] : null);
        Global.setSector(mock(SectorAPI.class,
                (n, a) -> n.equals("getClock") ? clock : null));
        int[] lookups = {0};
        StarSystemAPI lacedaemon = mock(StarSystemAPI.class, (n, a) -> {
            if (n.equals("getEntityById")) lookups[0]++;
            return null;
        });

        for (int patrol = 0; patrol < 10; patrol++) {
            List<CampaignFleetAPI> guards = IthacaThreatPolicy.spartanGuardsThisTick(lacedaemon);
            check(guards != null, "Patrol " + patrol + " gets a guard list");
        }
        int firstTick = lookups[0];
        check(firstTick > 0, "Guard upkeep runs on the first patrol");
        check(firstTick == 2, "Ten patrols in one tick share one upkeep pass");

        now[0] = 2000L;
        IthacaThreatPolicy.spartanGuardsThisTick(lacedaemon);
        check(lookups[0] == 2 * firstTick, "The next tick runs the upkeep again");

        StarSystemAPI other = mock(StarSystemAPI.class, (n, a) -> {
            if (n.equals("getEntityById")) lookups[0]++;
            return null;
        });
        IthacaThreatPolicy.spartanGuardsThisTick(other);
        check(lookups[0] == 3 * firstTick, "A different system is never served from the cache");

        Global.setSector(mock(SectorAPI.class, (n, a) -> null));
        int before = lookups[0];
        IthacaThreatPolicy.spartanGuardsThisTick(lacedaemon);
        check(lookups[0] > before, "Without a clock the upkeep runs uncached");
    }
}
