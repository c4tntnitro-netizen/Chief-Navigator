package chiefnavigator.quest;

import chiefnavigator.weapons.IthacaResearchUpgrades;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec.DropData;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial;
import java.awt.Color;
import java.lang.reflect.Proxy;
import java.util.*;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;

/** Native salvage construction and saved-state checks for the gated seventh POI. */
public final class AviciResearchStationRegression {
    private static final String STATION = OdysseyExpanseSystem.AVICI_RESEARCH_STATION_ID;
    private static final String ATTEMPTED = "$chief_navigator_avici_research_reveal_attempted_v1";
    private static final String SALVAGED = "$chief_navigator_odyssey_research_station_salvaged_v2_";
    private static final String INSTALLED = "$chief_navigator_ithaca_research_component_mask_v1";
    private static final String MARKER = "$chief_navigator_odyssey_research_station";
    private interface Call { Object invoke(String method, Object[] args); }

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

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Memory {
        final Map<String, Object> values = new HashMap<>();
        final MemoryAPI api = mock(MemoryAPI.class, (method, args) -> {
            Object value = args == null || args.length == 0 ? null : values.get(args[0]);
            switch (method) {
                case "get": return value;
                case "getBoolean": return Boolean.TRUE.equals(value);
                case "getInt": return value instanceof Number ? ((Number) value).intValue() : 0;
                case "getLong": return value instanceof Number ? ((Number) value).longValue() : 0L;
                case "getFloat": return value instanceof Number ? ((Number) value).floatValue() : 0f;
                case "contains": return values.containsKey(args[0]);
                case "set": values.put((String) args[0], args[1]); break;
                case "unset": values.remove(args[0]); break;
                default: break;
            }
            return null;
        });
    }

    private static final Map<CargoAPI, Cargo> CARGOS = new IdentityHashMap<>();
    private static final class Cargo {
        final Map<String, Float> specials = new HashMap<>();
        final Map<String, Float> commodities = new HashMap<>();
        final CargoAPI api = mock(CargoAPI.class, (method, args) -> {
            switch (method) {
                case "getQuantity": return specials.getOrDefault(((SpecialItemData) args[1]).getId(), 0f);
                case "addSpecial": specials.merge(((SpecialItemData) args[0]).getId(), (Float) args[1], Float::sum); break;
                case "removeItems": {
                    String item = ((SpecialItemData) args[1]).getId();
                    float quantity = specials.getOrDefault(item, 0f);
                    if (quantity < (Float) args[2]) return false;
                    specials.put(item, quantity - (Float) args[2]);
                    return true;
                }
                case "getCommodityQuantity": return commodities.getOrDefault((String) args[0], 0f);
                case "removeCommodity": commodities.merge((String) args[0], -(Float) args[1], Float::sum); break;
                case "addAll": {
                    Cargo source = CARGOS.get(args[0]);
                    source.specials.forEach((id, quantity) -> specials.merge(id, quantity, Float::sum));
                    source.commodities.forEach((id, quantity) -> commodities.merge(id, quantity, Float::sum));
                    break;
                }
                default: break;
            }
            return null;
        });
        Cargo() { CARGOS.put(api, this); }
    }

    private static DropData drop(String group) {
        DropData drop = new DropData();
        drop.group = group;
        return drop;
    }

    private static final class Station {
        final Memory memory = new Memory();
        final List<DropData> random = new ArrayList<>(Arrays.asList(drop("ai_cores3"), drop("supplies")));
        final List<DropData> value = new ArrayList<>(Arrays.asList(drop("ai_cores1"), drop("basic")));
        final StatBonus detected = new StatBonus();
        final CustomCampaignEntityAPI api;
        String id, name, type = Entities.STATION_RESEARCH_REMNANT;
        LocationAPI location;
        SectorEntityToken orbit;
        float angle, radius, period;
        int mutations;
        Station(LocationAPI location) {
            this.location = location;
            api = mock(CustomCampaignEntityAPI.class, (method, args) -> {
                switch (method) {
                    case "getId": return id;
                    case "getName": return name;
                    case "getCustomEntityType": return type;
                    case "getContainingLocation": return this.location;
                    case "getMemoryWithoutUpdate": return memory.api;
                    case "getDetectedRangeMod": return detected;
                    case "getDropRandom": return random;
                    case "getDropValue": return value;
                    case "getOrbitFocus": return orbit;
                    case "getLocation": return new Vector2f();
                    case "setId": id = (String) args[0]; mutations++; break;
                    case "setName": name = (String) args[0]; mutations++; break;
                    case "setCircularOrbit":
                        orbit = (SectorEntityToken) args[0];
                        angle = (Float) args[1]; radius = (Float) args[2]; period = (Float) args[3];
                        mutations++; break;
                    default: if (method.startsWith("set")) mutations++; break;
                }
                return null;
            });
        }
    }

    private static final class Fixture {
        final Memory saved = new Memory(), worldMemory = new Memory();
        final Cargo cargo = new Cargo();
        final List<EveryFrameScript> scripts = new ArrayList<>();
        final List<StarSystemAPI> systems = new ArrayList<>();
        final Map<String, SectorEntityToken> remote = new HashMap<>();
        final Map<String, SectorEntityToken> local = new HashMap<>();
        final List<Station> stations = new ArrayList<>();
        StarSystemAPI avici;
        final StarSystemAPI other;
        final PlanetAPI sanghata;
        boolean absent, paused, failFactory;
        int factoryCalls;
        Fixture() throws Exception {
            saved.values.put("$chief_navigator_ithaca_research_consumables_migrated_v2", true);
            worldMemory.values.put("$chief_navigator_odyssey_authored_system_v1", true);
            avici = mock(StarSystemAPI.class, (method, args) -> {
                switch (method) {
                    case "getOptionalUniqueId": return OdysseyExpanseSystem.MESSINA_ID;
                    case "getName": return "Avici";
                    case "getMemoryWithoutUpdate": return worldMemory.api;
                    case "getEntityById":
                        for (Station station : stations) if (Objects.equals(station.id, args[0])) return station.api;
                        return local.get(args[0]);
                    case "addCustomEntity": {
                        factoryCalls++;
                        check(saved.api.getBoolean(ATTEMPTED), "Creation must be latched before the native salvage factory");
                        if (failFactory) return null;
                        check(Entities.STATION_RESEARCH_REMNANT.equals(args[2]), "Seventh station must retain ordinary native research salvage");
                        Station station = new Station((LocationAPI) null);
                        station.location = this.avici;
                        stations.add(station);
                        return station.api;
                    }
                    default: break;
                }
                return null;
            });
            other = mock(StarSystemAPI.class, (method, args) -> method.equals("getEntityById") ? remote.get(args[0]) : null);
            sanghata = mock(PlanetAPI.class, (method, args) -> {
                if (method.equals("getId")) return OdysseyExpanseSystem.MESSINA_ID + "_world_sanghata";
                if (method.equals("getContainingLocation")) return avici;
                return null;
            });
            local.put(sanghata.getId(), sanghata);
            systems.add(avici);
            systems.add(other);
            for (String systemId : new String[] {OdysseyExpanseSystem.SYSTEM_ID,
                    OdysseyExpanseSystem.SILENT_WAKE_ID, OdysseyExpanseSystem.ASHEN_VERGE_ID}) {
                systems.add(mock(StarSystemAPI.class, (method, args) -> {
                    if (method.equals("getOptionalUniqueId")) return systemId;
                    if (method.equals("getMemoryWithoutUpdate")) return worldMemory.api;
                    return null;
                }));
            }
            CampaignFleetAPI player = mock(CampaignFleetAPI.class, (method, args) -> method.equals("getCargo") ? cargo.api : null);
            Global.setSector(mock(SectorAPI.class, (method, args) -> {
                switch (method) {
                    case "getMemoryWithoutUpdate": return saved.api;
                    case "getPlayerFleet": return player;
                    case "getStarSystems": return absent ? Collections.singletonList(other) : systems;
                    case "getAllLocations": return new ArrayList<LocationAPI>(systems);
                    case "isPaused": return paused;
                    case "addTransientScript": scripts.add((EveryFrameScript) args[0]); break;
                    case "removeTransientScriptsOfClass": scripts.removeIf(script -> args[0] == script.getClass()); break;
                    default: break;
                }
                return null;
            }));
            Global.setSettings(mock(SettingsAPI.class, (method, args) -> method.equals("getColor") ? Color.WHITE : null));
            SalvageEntityGenDataSpec nativeSpec = new SalvageEntityGenDataSpec(new JSONObject()
                    .put("id", Entities.STATION_RESEARCH_REMNANT).put("stationRole", ""));
            Global.setSettings(mock(SettingsAPI.class, (method, args) -> {
                if (method.equals("getColor")) return Color.WHITE;
                if (method.equals("getSpec")) return nativeSpec;
                return null;
            }));
            Global.setFactory(mock(FactoryAPI.class, (method, args) -> method.equals("createCargo") ? new Cargo().api : null));
        }
        void recover(int count) {
            for (int index = 0; index < count; index++) cargo.specials.put(IthacaResearchUpgrades.COMPONENT_IDS[index], 1f);
        }
        void tick() { new AviciResearchStationScript().advance(2f); }
    }

    private static void prerequisitesAndSalvage() throws Exception {
        Fixture f = new Fixture();
        check(OdysseyExpanseSystem.getResearchStationTotal() == 7, "All six original stations plus the new station must remain authored");
        check(MenelausTrial.getResearchObjectiveTarget() == 4, "Labor II must still require four installed components");
        f.recover(5);
        f.tick();
        check(f.factoryCalls == 0 && !f.saved.api.getBoolean(ATTEMPTED), "Five recovered components must not reveal the final station");
        IthacaResearchUpgrades.enableMaximumUpgradeOverride();
        f.tick();
        check(f.factoryCalls == 0, "Combat test override must not unlock the station");
        for (String prefix : new String[] {"chief_navigator_expanse_research_", "chief_navigator_silent_wake_research_", "chief_navigator_ashen_verge_research_"}) {
            f.saved.values.put(SALVAGED + prefix + "0", true);
            f.saved.values.put(SALVAGED + prefix + "1", true);
        }
        f.tick();
        check(f.factoryCalls == 0, "Merely displaying all six salvage windows must not qualify as recovery");
        f.recover(6);
        f.paused = true;
        f.tick();
        check(f.factoryCalls == 0, "Paused campaign must not introduce objects");
        f.paused = false;
        f.tick();
        check(f.factoryCalls == 1 && f.stations.size() == 1, "All six actual parts must reveal exactly one new station");
        Station station = f.stations.get(0);
        check(station.orbit == f.sanghata && station.angle == 45f && station.radius == 1200f && station.period == 95f,
                "Station must orbit the real Sanghata, clear of central Kshaya and jumps");
        check(OdysseyExpanseSystem.isOwnedResearchStation(station.api), "Exact new station must route through ordinary owned research salvage");
        check(station.random.size() == 1 && station.value.size() == 1, "AI-core drop groups must be removed while other native loot survives");
        CargoAPI loot = BaseSalvageSpecial.getCombinedExtraSalvage(station.api);
        check(loot.getQuantity(CargoAPI.CargoItemType.SPECIAL, new SpecialItemData(IthacaResearchUpgrades.COMPONENT_IDS[6], null)) == 1f,
                "New station must carry exactly one Abyssal Focusing Lens");
        check(OdysseyExpanseSystem.getRemainingResearchStations().contains(station.api), "New live station must appear in remaining research hints");
        int mutations = station.mutations;
        f.tick();
        AviciResearchStationScript.install();
        AviciResearchStationScript.install();
        check(f.scripts.size() == 1, "Load hook must install one transient script");
        f.scripts.get(0).advance(2f);
        check(f.factoryCalls == 1 && station.mutations == mutations, "Reload/ticks must preserve station identity, orbit, loot, and name");
        new ResearchStationSalvageListener().reportExtraSalvageShown(station.api);
        check(f.saved.api.getBoolean(SALVAGED + STATION), "Native salvage callback must durably record seventh-station completion");
        check(OdysseyExpanseSystem.getResearchedStationCount() == 7, "Salvage progress must count all seven stations");
        check(!OdysseyExpanseSystem.getRemainingResearchStations().contains(station.api), "Salvaged station must leave remaining hints before native removal");
        f.stations.clear();
        f.tick();
        check(f.factoryCalls == 1, "A salvaged or absent station must never respawn");
    }

    private static void savedClaimsAndFailures() throws Exception {
        Fixture f = new Fixture();
        f.saved.values.put(INSTALLED, 63);
        f.tick();
        check(f.factoryCalls == 1, "Six already installed original parts must qualify without cargo");
        f.stations.clear();
        f.tick();
        check(f.factoryCalls == 1, "An unsalvaged station's unexplained absence must remain authoritative");

        f = new Fixture();
        f.recover(6);
        Station foreign = new Station(f.other);
        foreign.id = STATION;
        foreign.type = "station_lowtech1";
        f.remote.put(STATION, foreign.api);
        f.tick();
        check(f.factoryCalls == 0 && foreign.mutations == 0 && f.saved.api.getBoolean(ATTEMPTED),
                "A remote foreign ID claim must block creation without mutation");
        f.remote.clear();
        f.tick();
        check(f.factoryCalls == 0, "Clearing a collision must not restart the one-time event");
        new ResearchStationSalvageListener().reportExtraSalvageShown(foreign.api);
        check(!f.saved.api.getBoolean(SALVAGED + STATION), "Wrong-type/foreign station must not earn research completion");

        f = new Fixture();
        f.recover(6);
        f.local.clear();
        f.tick();
        f.local.put(f.sanghata.getId(), f.sanghata);
        f.tick();
        check(f.factoryCalls == 0 && f.saved.api.getBoolean(ATTEMPTED), "Missing authored planet must fail closed without later topology repair");

        f = new Fixture();
        f.recover(6);
        f.failFactory = true;
        try { f.tick(); } catch (NullPointerException expected) { /* Native factory cannot configure a null token. */ }
        f.failFactory = false;
        f.tick();
        check(f.factoryCalls == 1 && f.saved.api.getBoolean(ATTEMPTED), "Failed native factory must remain latched across later ticks");
    }

    public static void main(String[] args) throws Exception {
        SectorAPI priorSector = Global.getSector();
        SettingsAPI priorSettings = Global.getSettings();
        FactoryAPI priorFactory = Global.getFactory();
        try {
            prerequisitesAndSalvage();
            savedClaimsAndFailures();
            System.out.println("Avici research station regression passed");
        } finally {
            Global.setSector(priorSector);
            Global.setSettings(priorSettings);
            Global.setFactory(priorFactory);
            CARGOS.clear();
        }
    }
}
