import chiefnavigator.weapons.*;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.*;
import com.fs.starfarer.loading.specs.BaseWeaponSpec;
import java.awt.Color;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import org.json.*;
import org.lwjgl.util.vector.Vector2f;

/** Native slot fitting, unchanged Odyssey systems, research, timers and hull visuals. */
public final class OdysseyIthRegression extends WallResearchRegression {
    static final String HULL = IthacaResearchUpgrades.ITHACA_ODYSSEY_HULL_ID;
    static final String LASER = IthacaResearchUpgrades.ITHACA_ODYSSEY_LASER_ID;
    static JSONObject read(String file) throws Exception { return new JSONObject(Files.readString(Path.of(file))); }

    static void fitAndData() throws Exception {
        JSONObject base = read("../../starsector-core/data/hulls/odyssey.ship");
        JSONObject skin = read("data/hulls/skins/" + HULL + ".skin");
        JSONObject variant = read("data/variants/" + HULL + "_Standard.variant");
        JSONObject stock = read("../../starsector-core/data/variants/odyssey_Balanced.variant");
        JSONObject gun = read("data/weapons/" + LASER + ".wpn");
        check(skin.getString("baseHullId").equals("odyssey") && skin.getString("hullName").equals("Odyssey (Ithaca)"), "Exact named Odyssey skin");
        check(skin.getJSONArray("removeWeaponSlots").length() == 1
                && skin.getJSONArray("removeWeaponSlots").getString(0).equals("WS 006")
                && skin.getJSONArray("removeEngineSlots").length() == 0
                && skin.getJSONArray("removeBuiltInMods").length() == 0 && !skin.has("weaponSlotChanges")
                && !skin.has("hitpoints") && !skin.has("maxSpeed") && !skin.has("shieldType"), "Remove only the aft medium missile mount; preserve other geometry, defenses and systems");
        check(skin.getInt("fighterBays") == 3 && skin.getJSONArray("builtInMods").length() == 1
                && skin.getJSONArray("builtInMods").getString(0).equals("surveying_equipment"), "Three fighter bays and built-in Surveying Equipment");
        check(skin.getJSONObject("builtInWeapons").length() == 2 && gun.getString("type").equals("ENERGY")
                && gun.getString("size").equals("LARGE")
                && !gun.getString("turretSprite").equals("graphics/fx/empty.png"), "Two visible large naval turrets for the port broadside");
        BaseWeaponSpec energy = new BaseWeaponSpec(LASER);
        energy.setType(WeaponAPI.WeaponType.ENERGY); energy.setSize(WeaponAPI.WeaponSize.LARGE);
        energy.setMountType(WeaponAPI.WeaponType.ENERGY);
        BaseWeaponSpec ballistic = new BaseWeaponSpec("wrong_type");
        ballistic.setType(WeaponAPI.WeaponType.BALLISTIC); ballistic.setSize(WeaponAPI.WeaponSize.LARGE);
        ballistic.setMountType(WeaponAPI.WeaponType.BALLISTIC);
        Class<?> slotClass = Class.forName("com.fs.starfarer.loading.specs.voidsuper");
        Class<?> mountClass = Class.forName("com.fs.starfarer.loading.specs.voidsuper$o");
        Constructor<?> ctor = slotClass.getConstructors()[0];
        int fitted = 0;
        for (int i = 0; i < base.getJSONArray("weaponSlots").length(); i++) {
            JSONObject s = base.getJSONArray("weaponSlots").getJSONObject(i);
            if (!List.of("WS 002", "WS 003").contains(s.getString("id"))) continue;
            check(s.getString("type").equals("ENERGY") && s.getString("size").equals("LARGE")
                    && s.getString("mount").equals("TURRET") && s.getJSONArray("locations").getDouble(1) > 0
                    && s.getInt("angle") == 90 && s.getInt("arc") == 150,
                    "Use the two existing large energy mounts on the port broadside with stock arcs");
            for (Object mount : mountClass.getEnumConstants()) {
                WeaponSlotAPI slot = (WeaponSlotAPI) ctor.newInstance(s.getString("id"), WeaponAPI.WeaponType.ENERGY,
                        WeaponAPI.WeaponSize.LARGE, mount, new Vector2f((float)s.getJSONArray("locations").getDouble(0),
                        (float)s.getJSONArray("locations").getDouble(1)), null, (float)s.getDouble("angle"), (float)s.getDouble("arc"));
                if (!slot.isTurret()) continue;
                check(slot.weaponFits(energy) && !slot.weaponFits(ballistic),
                        "Native broadside slot rules accept large ENERGY and reject BALLISTIC guns");
                fitted++;
            }
            check(skin.getJSONObject("builtInWeapons").getString(s.getString("id")).equals(LASER), "Laser built into each selected large energy mount");
        }
        check(fitted == 2, "Native fitting verified on both large broadside turrets");
        for (String key : List.of("wings", "hullMods", "fluxVents", "fluxCapacitors"))
            check(stock.get(key).toString().equals(variant.get(key).toString()), "Retain stock refit budget and two wings; third bay is empty: " + key);
        Map<String,String> expected = fittedWeapons(stock), actual = fittedWeapons(variant);
        expected.remove("WS 006"); expected.put("WS 002", LASER); expected.put("WS 003", LASER);
        check(actual.equals(expected), "Only the removed mount and both naval lasers differ from stock weapons");
        for (int i = 0; i < variant.getJSONArray("weaponGroups").length(); i++) {
            check(variant.getJSONArray("weaponGroups").getJSONObject(i).getJSONObject("weapons").length() > 0,
                    "Removed missile mount leaves no empty weapon group");
        }
        JSONArray rows = (JSONArray) Class.forName("com.fs.starfarer.loading.G").getMethod("o00000", String.class)
                .invoke(null, Files.readString(Path.of("data/weapons/weapon_data.csv")));
        JSONObject a = null, b = null;
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            if (LASER.equals(row.optString("id"))) a = row;
            if (IthacaResearchUpgrades.ITHACA_ONSLAUGHT_LASER_ID.equals(row.optString("id"))) b = row;
        }
        check(a != null && b != null, "Native weapon spreadsheet contains both ship lasers");
        for (Iterator<?> keys = b.keys(); keys.hasNext();) {
            String key = (String) keys.next();
            if (!List.of("id", "customAncillary").contains(key)) check(a.optString(key).equals(b.optString(key)), "Same naval balance: " + key);
        }
    }

    static Map<String,String> fittedWeapons(JSONObject variant) throws Exception {
        Map<String,String> fitted = new HashMap<>();
        for (int i = 0; i < variant.getJSONArray("weaponGroups").length(); i++) {
            JSONObject weapons = variant.getJSONArray("weaponGroups").getJSONObject(i).getJSONObject("weapons");
            for (Iterator<?> keys = weapons.keys(); keys.hasNext();) {
                String id = (String) keys.next();
                check(fitted.put(id, weapons.getString(id)) == null, "Each slot fitted once");
            }
        }
        return fitted;
    }

    static void research() {
        Global.setSector(null);
        for (int mask = 0; mask < 128; mask++) {
            ShipAPI source = ship(HULL, mask, 0, new Vector2f());
            check(IthacaResearchUpgrades.getMask(source) == mask, "All seven component flags recognized");
            DriftingWallBeamEffect effect = new DriftingWallBeamEffect();
            for (int mount = 0; mount < 2; mount++) {
                float[] stats = {2.5f, 30f}; int[] clones = {0};
                BeamWeaponSpecAPI spec = mock(BeamWeaponSpecAPI.class, (n,a) -> {
                    if (n.equals("getTurnRate")) return stats[0];
                    if (n.equals("getBurstCooldown")) return stats[1];
                    if (n.equals("getChargedownTime")) return .75f;
                    if (n.equals("setTurnRate")) stats[0] = (float)a[0];
                    if (n.equals("setBurstCooldown")) stats[1] = (float)a[0];
                    return null;
                });
                WeaponAPI w = mock(WeaponAPI.class, (n,a) -> {
                    if (n.equals("getId")) return LASER;
                    if (n.equals("getShip")) return source;
                    if (n.equals("getSpec")) return spec;
                    if (n.equals("getCooldown")) return 30.75f;
                    if (n.equals("getBeams")) return List.of();
                    if (n.equals("getLocation")) return new Vector2f();
                    if (n.equals("ensureClonedSpec")) clones[0]++;
                    return null;
                });
                check(OnslaughtIthDischargeVfx.handles(w), "New gun/hull pair uses ship-scale effects");
                CombatEngineAPI engine = mock(CombatEngineAPI.class, (n,a) -> null);
                effect.advance(.01f, engine, w); effect.advance(.01f, engine, w);
                check(stats[0] == ((mask & 1) != 0 ? 10f : 2.5f) && stats[1] == ((mask & 2) != 0 ? 12f : 30f), "Upgrade each mount exactly once");
                check(clones[0] == ((mask & 3) != 0 ? 1 : 0), "Clone before changing shared weapon data");
            }
        }
        for (String hull : List.of("odyssey", "odyssey_other_skin", "onslaught"))
            check(IthacaResearchUpgrades.getMask(ship(hull, 127, 0, new Vector2f())) == 0, "Stock/foreign hulls remain excluded");
        ShipHullSpecAPI d = mock(ShipHullSpecAPI.class, (n,a) -> n.equals("getHullId") ? HULL+"_d" : n.equals("getDParentHullId") ? HULL : null);
        ShipAPI damaged = mock(ShipAPI.class, (n,a) -> n.equals("getHullSpec") ? d : n.equals("getCustomData") ? Map.of("chief_navigator_ithaca_research_component_mask", 127) : null);
        check(IthacaResearchUpgrades.getMask(damaged) == 127, "Exact D-parent inherits research");
    }

    static void visuals() {
        for (float angle : new float[] {0,90,180,270}) {
            Map<String,Object> data = new HashMap<>(); List<WeaponAPI> weapons = new ArrayList<>();
            List<EveryFrameCombatPlugin> plugins = new ArrayList<>(); List<Vector2f> points = new ArrayList<>();
            ShipHullSpecAPI spec = mock(ShipHullSpecAPI.class, (n,a) -> n.equals("getHullId") ? HULL : null);
            ShipAPI source = mock(ShipAPI.class, (n,a) -> {
                if(n.equals("getHullSpec")) return spec;
                if(n.equals("getCustomData")) return data;
                if(n.equals("setCustomData")) data.put((String)a[0], a[1]);
                if(n.equals("getLocation")) return new Vector2f();
                if(n.equals("getFacing")) return angle;
                if(n.equals("isAlive")) return true;
                if(n.equals("getAllWeapons")) return weapons;
                return null;
            });
            BeamAPI ray = mock(BeamAPI.class, (n,a) -> n.equals("getBrightness") ? 1f : null);
            for(int i=0;i<2;i++) weapons.add(mock(WeaponAPI.class, (n,a) -> n.equals("getId") ? LASER : n.equals("getShip") ? source : n.equals("getBeams") ? List.of(ray) : null));
            CombatEngineAPI engine = mock(CombatEngineAPI.class, (n,a) -> {
                if(n.equals("isEntityInPlay")) return true;
                if(n.equals("addPlugin")) plugins.add((EveryFrameCombatPlugin)a[0]);
                if(n.equals("spawnEmpArcVisual")) { points.add(new Vector2f((Vector2f)a[0])); points.add(new Vector2f((Vector2f)a[2])); }
                if(n.startsWith("addNebula") || n.equals("addSmoothParticle") || n.equals("spawnEmpArc")) throw new AssertionError("No steam or gameplay arcs from cosmetic hull effects");
                return null;
            });
            for(WeaponAPI w:weapons) OnslaughtIthDischargeVfx.install(engine,w);
            check(plugins.size()==1,"Both turrets share one hull effect");
            plugins.get(0).advance(.2f,List.of()); plugins.get(0).advance(.2f,List.of());
            check(!points.isEmpty(),"Odyssey lightning paths rendered");
            for(Vector2f p:points) {
                double r=Math.toRadians(angle); double along=p.x*Math.cos(r)+p.y*Math.sin(r), port=-p.x*Math.sin(r)+p.y*Math.cos(r);
                check(along>=-165 && along<=190 && port>=-60 && port<=51,"Use Odyssey paths rather than Onslaught-wide geometry");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        SettingsAPI oldSettings=Global.getSettings(); SectorAPI oldSector=Global.getSector();
        try {
            Global.setSettings(mock(SettingsAPI.class,(n,a)->n.equals("getColor")?Color.WHITE:null));
            fitAndData(); research(); visuals();
            IsaNavalRefractionRegression.verifyHull(HULL,LASER);
            OnslaughtChargePulseRegression.Fixture pulses=new OnslaughtChargePulseRegression.Fixture(HULL,null,LASER);
            check(pulses.cycle(120).size()==11,"Both broadside guns inherit eleven native charge pulses");
            NativeNavalCyclerRegression.shipCooldown=30; NativeNavalCyclerRegression.shipFlux=900;
            NativeNavalCyclerRegression.Gun gun=new NativeNavalCyclerRegression.Gun(30,2,1f/60f,false,HULL,LASER);
            gun.hideWeaponBeams=true; gun.tailDuration=2; gun.tick(); gun.trigger=false; gun.run(45);
            NativeNavalCyclerRegression.verifyHudAndDuration(gun,12);
            check(gun.cooldownStarts.get(0)>=gun.ends.get(0)+2-gun.step,"Full cooldown waits for Odyssey ray end");
            System.out.println("PASS: Odyssey LARGE/ENERGY broadside fitting, removed aft missile mount, three bays with two default wings, built-in Surveying Equipment, both mounts x128 research masks, native12s cooldown/HUD, Isa, pulses and hull VFX.");
        } finally { Global.setSettings(oldSettings); Global.setSector(oldSector); }
    }
}
