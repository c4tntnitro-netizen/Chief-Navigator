import chiefnavigator.quest.*;
import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import java.lang.reflect.*;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;

public class IthacaCampaignRegression {
    public static void main(String[] args) throws Exception {
        Global.setSettings(WallResearchRegression.mock(SettingsAPI.class,
                (n,a) -> n.equals("getColor") ? java.awt.Color.WHITE : null));
        Method gate = IthacaThreatPolicy.class.getDeclaredMethod("opportunityAllowed",
                float.class, float.class, boolean.class, float.class, float.class);
        gate.setAccessible(true);
        WallResearchRegression.check(!(Boolean)gate.invoke(null, 3500f, 100f, true, 200f, 100f), "Ithaca refuge");
        WallResearchRegression.check(!(Boolean)gate.invoke(null, 6000f, 701f, true, 200f, 100f), "No distant pursuit");
        WallResearchRegression.check(!(Boolean)gate.invoke(null, 6000f, 300f, false, 200f, 100f), "No invisible pursuit");
        WallResearchRegression.check(!(Boolean)gate.invoke(null, 6000f, 300f, true, 100f, 100f), "No even-strength opportunism");
        WallResearchRegression.check((Boolean)gate.invoke(null, 6000f, 300f, true, 140f, 100f), "Visible isolated opportunity");

        Map<String,Object> flags = new HashMap<>();
        MemoryAPI memory = WallResearchRegression.mock(MemoryAPI.class, (n,a) -> {
            if (n.equals("set")) flags.put((String)a[0], a[1]);
            if (n.equals("unset")) flags.remove(a[0]);
            if (n.equals("contains")) return flags.containsKey(a[0]);
            if (n.equals("getBoolean")) return Boolean.TRUE.equals(flags.get(a[0]));
            return null;
        });
        List<CampaignFleetAPI> stations = new ArrayList<>();
        MemoryAPI centerMemory = WallResearchRegression.mock(
                MemoryAPI.class, (n,a) -> n.equals("getBoolean") ? true : null);
        SectorEntityToken center = WallResearchRegression.mock(SectorEntityToken.class,
                (n,a) -> {
                    if (n.equals("getId")) {
                        return IthacaSectionEncounter.FOB_ENTITY_ID;
                    }
                    if (n.equals("getCustomEntityType")) {
                        return "chief_navigator_fob_ithaca_campaign";
                    }
                    if (n.equals("getMemoryWithoutUpdate")) {
                        return centerMemory;
                    }
                    if (n.equals("getLocation")) return new Vector2f();
                    return null;
                });
        StarSystemAPI system = WallResearchRegression.mock(StarSystemAPI.class, (n,a) -> {
            if (n.equals("getEntityById")) return center;
            if (n.equals("getFleets")) return stations;
            return null;
        });
        Vector2f playerPosition = new Vector2f(6000f, 0f);
        boolean[] visible = {true};
        CampaignFleetAPI player = WallResearchRegression.mock(CampaignFleetAPI.class, (n,a) -> {
            if (n.equals("getLocation")) return playerPosition;
            if (n.equals("getContainingLocation")) return system;
            if (n.equals("isVisibleToSensorsOf")) return visible[0];
            if (n.equals("getFleetPoints")) return 100;
            return null;
        });
        FactionAPI faction = WallResearchRegression.mock(FactionAPI.class,
                (n,a) -> n.equals("getId") ? IthacaThreatPolicy.FACTION_ID : null);
        CampaignFleetAPI threat = WallResearchRegression.mock(CampaignFleetAPI.class, (n,a) -> {
            if (n.equals("getFaction")) return faction;
            if (n.equals("getMemoryWithoutUpdate")) return memory;
            if (n.equals("getLocation")) return new Vector2f(6200f, 0f);
            if (n.equals("getContainingLocation")) return system;
            if (n.equals("getFleetPoints")) return 140;
            return null;
        });
        Global.setSector(WallResearchRegression.mock(SectorAPI.class,
                (n,a) -> n.equals("getPlayerFleet") ? player : null));
        flags.put("$chief_navigator_threat_opportunity_checked", true);
        flags.put("$chief_navigator_threat_opportunity_hostile", true);
        WallResearchRegression.check(IthacaThreatPolicy.apply(threat, system), "Active short intercept");
        WallResearchRegression.check(flags.containsKey(MemFlags.MEMORY_KEY_MAKE_HOSTILE), "Local hostility");
        visible[0] = false;
        WallResearchRegression.check(!IthacaThreatPolicy.apply(threat, system), "Lose sight: resume siege");
        WallResearchRegression.check(!flags.containsKey(MemFlags.MEMORY_KEY_MAKE_HOSTILE), "Neutral again");
        WallResearchRegression.check(!flags.containsKey(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE), "No aggressive flag");

        MemoryAPI stationMemory = WallResearchRegression.mock(MemoryAPI.class,
                (n,a) -> {
                    if (n.equals("getString")) return "north";
                    if (n.equals("getBoolean")) {
                        return IthacaSectionEncounter.MARKER.equals(a[0]);
                    }
                    return null;
                });
        Vector2f stationPosition = new Vector2f(8000f, 0f);
        CampaignFleetAPI station = WallResearchRegression.mock(CampaignFleetAPI.class, (n,a) -> {
            if (n.equals("getId")) return "chief_navigator_ithaca_station_north";
            if (n.equals("getMemoryWithoutUpdate")) return stationMemory;
            if (n.equals("getLocation")) return stationPosition;
            if (n.equals("isHostileTo")) return a[0] == threat;
            return null;
        });
        stations.add(station);
        Method support = IthacaSupportInteraction.class.getDeclaredMethod(
                "findSupport", CampaignFleetAPI.class, CampaignFleetAPI.class);
        support.setAccessible(true);
        WallResearchRegression.check(support.invoke(null, threat, player) == station, "Support beyond vanilla range");
        stationPosition.x = 9000f;
        WallResearchRegression.check(support.invoke(null, threat, player) == null, "No support beyond 2500");
        System.out.println("PASS: refuge, visibility/distance/strength gates, pursuit cancellation, neutral flags, support range.");
    }
}
