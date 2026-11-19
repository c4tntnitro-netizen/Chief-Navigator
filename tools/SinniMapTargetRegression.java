package chiefnavigator.quest;

import chiefnavigator.weapons.IthacaResearchUpgrades;
import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.GateEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import java.lang.reflect.*;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;

/** Calls the actual Intel getters; counts searches, not estimated FPS. */
public final class SinniMapTargetRegression {
    interface Call { Object get(String name, Object[] args); }
    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p,m,a) -> {
            if (m.getName().equals("hashCode")) return System.identityHashCode(p);
            if (m.getName().equals("equals")) return p == a[0];
            Object value = call.get(m.getName(), a);
            if (value != null) return value;
            Class<?> t = m.getReturnType();
            if (t == boolean.class) return false; if (t == int.class) return 0;
            if (t == long.class) return 0L; if (t == float.class) return 0f;
            if (t == double.class) return 0d; return null;
        }));
    }
    static void check(boolean value, String why) { if (!value) throw new AssertionError(why); }
    static String key(Class<?> type, String field) throws Exception {
        Field f = type.getDeclaredField(field); f.setAccessible(true); return (String) f.get(null);
    }
    static final class Memory {
        final Map<String,Object> values = new HashMap<>();
        final MemoryAPI api = mock(MemoryAPI.class, (n,a) -> {
            Object v = a == null || a.length == 0 ? null : values.get(a[0]);
            switch (n) {
                case "get": return v;
                case "getBoolean": return Boolean.TRUE.equals(v);
                case "getInt": return v instanceof Number ? ((Number)v).intValue() : 0;
                case "getString": return v instanceof String ? v : null;
                case "contains": return values.containsKey(a[0]);
                case "set": values.put((String)a[0],a[1]); break;
                case "unset": values.remove(a[0]); break;
            } return null;
        });
    }
    static final class Fixture {
        final Memory global = new Memory(), owned = new Memory(), gateState = new Memory();
        final Map<String,SectorEntityToken> entities = new HashMap<>();
        final List<StarSystemAPI> systems = new ArrayList<>();
        final Vector2f position = new Vector2f();
        final StarSystemAPI ashen;
        final SectorEntityToken ithaca, gate, office;
        final WaypointTroyIntel story = new WaypointTroyIntel();
        final SinniContactIntel contact;
        LocationAPI location;
        LocationAPI hyperspace;
        int systemReads, entityReads, cargoReads, marketReads;

        Fixture() throws Exception {
            Global.setSettings(mock(SettingsAPI.class, (n,a) -> null));
            owned.values.put("$chief_navigator_odyssey_authored_system_v1",true);
            ashen = mock(StarSystemAPI.class, (n,a) -> {
                if (n.equals("getOptionalUniqueId")) return OdysseyExpanseSystem.ASHEN_VERGE_ID;
                if (n.equals("getMemoryWithoutUpdate")) return owned.api;
                if (n.equals("getEntityById")) { entityReads++; return entities.get(a[0]); }
                if (n.equals("getNameWithNoType")) return "Lacedaemon";
                return null;
            });
            location = ashen;
            for (int i=0;i<500;i++) {
                String id = "ordinary_" + i;
                systems.add(mock(StarSystemAPI.class, (n,a) -> n.equals("getOptionalUniqueId") ? id : null));
            }
            systems.add(ashen);
            Memory stationState = new Memory();
            stationState.values.put(key(OdysseyExpanseSystem.class,"FOB_ITHACA_OWNER_MARKER"),true);
            ithaca = entity(key(OdysseyExpanseSystem.class,"FOB_ITHACA_ID"),
                    key(OdysseyExpanseSystem.class,"FOB_ITHACA_TYPE"), stationState);
            gateState.values.put(key(OdysseyExpanseSystem.class,"FOB_ITHACA_GATE_MARKER"),true);
            gate = entity(OdysseyExpanseSystem.FOB_ITHACA_GATE_ID,Entities.INACTIVE_GATE,gateState);
            office = entity("eventide_office","planet",new Memory());
            global.values.put(OdysseyExpanseSystem.ENTERED,true);
            global.values.put(MenelausTrial.ACCEPTED,true);
            global.values.put(key(MenelausTrial.class,"FINAL_LABOR_FORCED_FOR_TESTING"),true);
            global.values.put(key(MenelausTrial.class,"LABOR_REPORT_MIGRATION"),true);
            global.values.put(MenelausTrial.GAUTAMA_DEFEATED,true);
            global.values.put(MenelausTrial.FINAL_DEBRIEF_COMPLETE,true);
            global.values.put(key(IthacaResearchUpgrades.class,"CONSUMABLE_MIGRATION_KEY"),true);
            global.values.put(key(IthacaResearchUpgrades.class,"UNLOCKED_MASK_KEY"),15);
            CargoAPI cargo = mock(CargoAPI.class, (n,a) -> {
                if (n.equals("getQuantity")) { cargoReads++; return 0f; } return null;
            });
            CampaignFleetAPI player = mock(CampaignFleetAPI.class, (n,a) -> {
                if (n.equals("getCargo")) return cargo;
                if (n.equals("getContainingLocation")) return location;
                if (n.equals("getLocation") || n.equals("getLocationInHyperspace")) return position;
                return null;
            });
            FactionAPI faction = mock(FactionAPI.class, (n,a) -> null);
            MarketAPI market = mock(MarketAPI.class, (n,a) -> {
                if (n.equals("getPrimaryEntity")) return office;
                if (n.equals("getFaction")) return faction;
                if (n.equals("isInEconomy")) return true;
                return null;
            });
            EconomyAPI economy = mock(EconomyAPI.class, (n,a) -> {
                if (n.equals("getMarket")) { marketReads++; return market; } return null;
            });
            Global.setSector(mock(SectorAPI.class, (n,a) -> {
                switch(n) {
                    case "getMemoryWithoutUpdate": return global.api;
                    case "getCurrentLocation": return location;
                    case "getHyperspace": return hyperspace;
                    case "getPlayerFleet": return player;
                    case "getEconomy": return economy;
                    case "getScripts": return Collections.emptyList();
                    case "getStarSystems": systemReads++; return new ArrayList<>(systems);
                } return null;
            }));
            PersonAPI person = mock(PersonAPI.class, (n,a) -> n.equals("getMemoryWithoutUpdate") ? global.api : null);
            contact = new SinniContactIntel(person,market);
        }
        SectorEntityToken entity(String id, String type, Memory mem) {
            SectorEntityToken value = mock(SectorEntityToken.class, (n,a) -> {
                switch(n) {
                    case "getId": case "getName": return id;
                    case "getCustomEntityType": return type;
                    case "getContainingLocation": return ashen;
                    case "getMemoryWithoutUpdate": return mem.api;
                    case "getLocation": case "getLocationInHyperspace": return new Vector2f(500f,0f);
                } return null;
            });
            entities.put(id,value); return value;
        }
        void expire() throws Exception {
            Field f = SinniNavigationGuidance.class.getDeclaredField("mapResolvedAt");
            f.setAccessible(true); f.setLong(null,System.nanoTime()-1_100_000_000L);
        }
        void expect(SectorEntityToken expected, String why) {
            check(story.getMapLocation(null)==expected,why+" (story)");
            if (expected != null) check(contact.getMapLocation(null)==expected,why+" (contact)");
        }
    }
    static void troyMarker() throws Exception {
        Fixture f = new Fixture();
        f.global.values.remove(OdysseyExpanseSystem.ENTERED);
        f.global.values.put(SinniEventideIntel.BRIEFED,true);
        Memory owned = new Memory(), accessState = new Memory(), stationState = new Memory();
        owned.values.put(key(TroyArrivalScript.class,"AUTHORED_SYSTEM_MARKER"),true);
        accessState.values.put(key(TroyArrivalScript.class,"ACCESS_JUMP_MARKER"),true);
        stationState.values.put(key(TroyArrivalScript.class,"DEPARTURE_STATION_MARKER"),true);
        Map<String,SectorEntityToken> entities = new HashMap<>();
        StarSystemAPI troy = mock(StarSystemAPI.class,(n,a) -> {
            if(n.equals("getOptionalUniqueId")) return TroyArrivalScript.SYSTEM_ID;
            if(n.equals("getMemoryWithoutUpdate")) return owned.api;
            if(n.equals("getEntityById")) return entities.get(a[0]);
            return null;
        });
        String stationId = key(TroyArrivalScript.class,"DEPARTURE_STATION_ID");
        SectorEntityToken anchorage = mock(SectorEntityToken.class,(n,a) -> {
            if(n.equals("getId")) return stationId;
            if(n.equals("getCustomEntityType")) return "station_side00";
            if(n.equals("getContainingLocation")) return troy;
            if(n.equals("getMemoryWithoutUpdate")) return stationState.api;
            return null;
        });
        List<JumpPointAPI.JumpDestination> localDest = new ArrayList<>(), hyperDest = new ArrayList<>();
        JumpPointAPI local = mock(JumpPointAPI.class,(n,a) -> n.equals("getDestinations") ? localDest
                : n.equals("getMemoryWithoutUpdate") ? accessState.api : null);
        JumpPointAPI hyper = mock(JumpPointAPI.class,(n,a) -> n.equals("getDestinations") ? hyperDest
                : n.equals("getMemoryWithoutUpdate") ? accessState.api : null);
        localDest.add(new JumpPointAPI.JumpDestination(hyper,null));
        hyperDest.add(new JumpPointAPI.JumpDestination(local,null));
        f.hyperspace = mock(LocationAPI.class,(n,a) -> n.equals("getEntityById") ? hyper : null);
        f.systems.add(troy);
        entities.put(stationId,anchorage);
        entities.put(key(TroyArrivalScript.class,"ACCESS_JUMP_ID"),local);
        f.expect(hyper,"Outside Troy, cached marker uses its owned hyperspace entrance");
        f.location = troy;
        f.expect(anchorage,"Entering Troy immediately changes marker to the anchorage");
    }
    public static void main(String[] args) throws Exception {
        SectorAPI oldSector=Global.getSector(); SettingsAPI oldSettings=Global.getSettings();
        try {
            Fixture f=new Fixture();
            f.expect(f.gate,"Completed Labors point to unscanned gate");
            int searches=f.systemReads, entities=f.entityReads;
            for(int i=0;i<600;i++) f.expect(f.gate,"Stable marker");
            check(f.systemReads==searches && f.entityReads==entities && f.cargoReads==0,
                    "1200 UI marker requests must not repeat system/entity searches or optional cargo/advice work");
            f.gateState.values.put(GateEntityPlugin.GATE_SCANNED,true);
            f.story.notifyPlayerAboutToOpenIntelScreen();
            f.expect(f.ithaca,"Reopening Intel immediately notices scanned gate and unfinished hunts");
            f.global.values.put(OdysseyPredatorScript.BUDAI_DEFEATED,true);
            f.global.values.put(OdysseyPredatorScript.GAUTAMA_FINAL_REMATCH_DEFEATED,true);
            f.expire(); f.expect(null,"Lazy expiry notices completed hunts");
            searches=f.systemReads;
            for(int i=0;i<600;i++) f.expect(null,"No objective is cached too");
            check(f.systemReads==searches,"Missing objectives must not cause repeated searches");
            f.gateState.values.remove(GateEntityPlugin.GATE_SCANNED);
            String east=SinniNavigationGuidance.getCurrentGuidance().getRoute();
            f.expect(f.gate,"Asking Sinni immediately refreshes the marker");
            f.position.x=1000f;
            String west=SinniNavigationGuidance.getCurrentGuidance().getRoute();
            check(east.contains("east") && west.contains("west"),"Directions must use the live position each time");
            check(f.cargoReads>0,"Full briefing still evaluates optional research advice on demand");
            f.global.values.remove(MenelausTrial.ACCEPTED);
            f.contact.notifyPlayerAboutToOpenIntelScreen();
            f.expect(f.ithaca,"Contact-open invalidation notices pre-acceptance objective");
            f.global.values.clear(); f.location=null;
            f.expect(f.office,"Location change immediately refreshes Eventide objective");
            Fixture next=new Fixture();
            next.expect(next.gate,"Loading another save never returns previous campaign objects");
            Global.setSector(null);
            check(SinniNavigationGuidance.getCurrentMapTarget()==null,"No campaign has no marker");
            troyMarker();
            System.out.println("PASS: 1200 marker calls share one resolution, zero repeated world/cargo queries; negative cache, UI/location/save invalidation, bounded quest refresh, and live briefings.");
        } finally {
            Global.setSector(oldSector); Global.setSettings(oldSettings);
            SinniNavigationGuidance.invalidateMapTarget();
        }
    }
}
