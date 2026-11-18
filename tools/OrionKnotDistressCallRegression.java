import chiefnavigator.ChiefNavigatorModPlugin;
import chiefnavigator.abilities.OrionKnotDistressCallAbility;
import chiefnavigator.quest.OdysseyPredatorScript;
import chiefnavigator.quest.OrionKnotDistressResponse;
import chiefnavigator.quest.OrionKnotDistressResponse.Outcome;
import chiefnavigator.quest.TaskForceSpartanFaction;
import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.AbilityPlugin;
import com.fs.starfarer.api.fleet.*;
import com.fs.starfarer.api.impl.campaign.abilities.DistressCallAbility;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.DistressResponse;
import com.fs.starfarer.api.loading.AbilitySpecAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.util.DelayedActionScript;
import java.awt.Color;
import java.lang.reflect.*;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;

/** Native activation, delayed fleet creation, Spartan aid and rare Strike rosters. */
public final class OrionKnotDistressCallRegression {
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    static <T> T mock(Class<T> type, OrionKnotHiddenMissionRegression.Call call) {
        return OrionKnotHiddenMissionRegression.mock(type, call);
    }
    static Field field(Class<?> type, String name) throws Exception {
        Field f = type.getDeclaredField(name); f.setAccessible(true); return f;
    }
    static Method method(Class<?> type, String name, Class<?>... params) throws Exception {
        Method m = type.getDeclaredMethod(name, params); m.setAccessible(true); return m;
    }
    static final class ControlledCall extends OrionKnotDistressCallAbility {
        double roll;
        @Override protected double getResponseRoll() { return roll; }
    }
    static final class ForeignCall extends DistressCallAbility { }

    static final class Fixture {
        StarSystemAPI system;
        LocationAPI playerLocation;
        final Vector2f playerPosition = new Vector2f(137f, -290f), responsePosition = new Vector2f();
        final List<EveryFrameScript> scripts = new ArrayList<>();
        final List<CampaignFleetAPI> spawned = new ArrayList<>();
        final List<FleetMemberAPI> members = new ArrayList<>();
        final List<String> variants = new ArrayList<>();
        final List<Float> readiness = new ArrayList<>();
        final List<FleetAssignment> assignments = new ArrayList<>();
        final Map<String, Object> flags = new HashMap<>();
        final float[] responderCargo = new float[2], playerCargoAmounts = new float[2];
        AbilityPlugin ability;
        String faction = Factions.THREAT, fleetName;
        int factories, replacements;
        final SectorEntityToken center = mock(SectorEntityToken.class, (n, a) -> {
            if (n.equals("getLocation")) return new Vector2f();
            if (n.equals("getId")) return "fixture_center";
            return null;
        });
        final JumpPointAPI outer = mock(JumpPointAPI.class, (n, a) -> null);
        final JumpPointAPI inner = mock(JumpPointAPI.class, (n, a) -> {
            if (n.equals("getLocation")) return new Vector2f(1000f, 0f);
            if (n.equals("getDestinations")) return List.of(new JumpPointAPI.JumpDestination(outer, "Hyperspace"));
            return null;
        });
        final AbilitySpecAPI spec = mock(AbilitySpecAPI.class, (n, a) -> {
            if (n.equals("getName")) return "Distress Call";
            if (n.equals("getTags")) return Collections.emptySet();
            if (n.equals("getDeactivationCooldown")) return 1f;
            return null;
        });
        final SettingsAPI settings = mock(SettingsAPI.class, (n, a) -> {
            if (n.equals("getAbilitySpec")) return spec;
            if (n.equals("getColor")) return Color.WHITE;
            if (n.equals("getUnitsPerLightYear")) return 2000f;
            if (n.equals("getFloat") && a[0].equals("unitsPerLightYear")) return 2000f;
            if (n.equals("getCommoditySpec")) return mock(CommoditySpecAPI.class,
                    (x, y) -> x.equals("getName") ? (String) a[0] : null);
            return null;
        });
        final CampaignClockAPI clock = mock(CampaignClockAPI.class, (n, a) -> {
            if (n.equals("getTimestamp")) return 1000L;
            if (n.equals("convertToDays")) return (float) a[0];
            return null;
        });
        final AbilityPlugin other = mock(AbilityPlugin.class, (n, a) -> null);
        final CampaignFleetAPI player = mock(CampaignFleetAPI.class, (n, a) -> {
            switch (n) {
                case "getContainingLocation": return playerLocation;
                case "getLocation": return playerPosition;
                case "getLocationInHyperspace": return new Vector2f(150000f, -94000f);
                case "getAbility": return Abilities.DISTRESS_CALL.equals(a[0]) ? ability : other;
                case "removeAbility": ability = null; return null;
                case "addAbility": replaceAbility(); return null;
                case "getAbilities": return Collections.emptyMap();
                case "isPlayerFleet": case "isInCurrentLocation": return true;
                case "getVisibilityLevelToPlayerFleet": return SectorEntityToken.VisibilityLevel.NONE;
                default: return null;
            }
        });
        void replaceAbility() {
            ability = new OrionKnotDistressCallAbility();
            ability.init(Abilities.DISTRESS_CALL, player);
            replacements++;
        }
        final MemoryAPI memory = mock(MemoryAPI.class, (n, a) -> {
            switch (n) {
                case "set": flags.put((String) a[0], a[1]); return null;
                case "unset": flags.remove(a[0]); return null;
                case "contains": return flags.containsKey(a[0]);
                case "getBoolean": return Boolean.TRUE.equals(flags.get(a[0]));
                case "getInt": return flags.get(a[0]) instanceof Number ? ((Number) flags.get(a[0])).intValue() : 0;
                case "get": case "getString": return flags.get(a[0]);
                default: return null;
            }
        });
        final FleetDataAPI roster = mock(FleetDataAPI.class, (n, a) -> {
            if (n.equals("addFleetMember")) members.add((FleetMemberAPI) a[0]);
            if (n.equals("getMembersListCopy")) return new ArrayList<>(members);
            return null;
        });
        final CargoAPI cargo = cargo(responderCargo), playerCargo = cargo(playerCargoAmounts);
        static CargoAPI cargo(float[] amounts) {
            return mock(CargoAPI.class, (n, a) -> {
                if (n.equals("addFuel")) amounts[0] += ((Number) a[0]).floatValue();
                if (n.equals("addSupplies")) amounts[1] += ((Number) a[0]).floatValue();
                if (n.equals("getMaxFuel")) return 2000f;
                if (n.equals("getMaxCapacity")) return 1000f;
                return null;
            });
        }
        final CampaignFleetAPI responseFleet = mock(CampaignFleetAPI.class, (n, a) -> {
            switch (n) {
                case "getId": return "chief_navigator_distress_fixture";
                case "getContainingLocation": return system;
                case "getFaction": return mock(FactionAPI.class, (x, y) -> x.equals("getId") ? faction : null);
                case "setFaction": faction = (String) a[0]; return null;
                case "setName": fleetName = (String) a[0]; return null;
                case "getFleetData": return roster;
                case "getCargo": return cargo;
                case "getMemoryWithoutUpdate": return memory;
                case "setLocation": responsePosition.set((float) a[0], (float) a[1]); return null;
                case "addAssignment": assignments.add((FleetAssignment) a[0]); return null;
                default: return null;
            }
        });
        final SectorAPI sector = mock(SectorAPI.class, (n, a) -> {
            switch (n) {
                case "getPlayerFleet": return player;
                case "getClock": return clock;
                case "getStarSystems": return List.of(system);
                case "addScript": scripts.add((EveryFrameScript) a[0]); return null;
                case "reportPlayerActivatedAbility": case "reportPlayerDeactivatedAbility":
                case "getCampaignUI": case "getHyperspace": return null;
                default: throw new AssertionError("Unexpected campaign operation: " + n);
            }
        });
        final FactoryAPI factory = mock(FactoryAPI.class, (n, a) -> {
            if (n.equals("createEmptyFleet")) {
                faction = (String) a[0]; fleetName = (String) a[1]; factories++;
                check(faction.equals(Factions.THREAT) || faction.equals(TaskForceSpartanFaction.ID), "Only local THREAT or Spartan fleets");
                return responseFleet;
            }
            if (n.equals("createFleetMember")) {
                String variant = (String) a[1]; variants.add(variant);
                RepairTrackerAPI repair = mock(RepairTrackerAPI.class, (x, y) -> {
                    if (x.equals("getMaxCR")) return .7f;
                    if (x.equals("setCR")) readiness.add((float) y[0]);
                    return null;
                });
                return mock(FleetMemberAPI.class, (x, y) -> x.equals("getRepairTracker") ? repair : null);
            }
            throw new AssertionError("Unexpected fleet factory operation: " + n);
        });
        Fixture(OrionKnotHiddenMissionRegression.SystemFixture source, double roll) {
            system = (StarSystemAPI) Proxy.newProxyInstance(StarSystemAPI.class.getClassLoader(),
                    new Class<?>[] {StarSystemAPI.class}, (proxy, m, a) -> {
                        switch (m.getName()) {
                            case "getCenter": return center;
                            case "getJumpPoints": return List.of(inner);
                            case "addEntity": spawned.add((CampaignFleetAPI) a[0]); return null;
                            case "equals": return proxy == a[0];
                            case "hashCode": return System.identityHashCode(proxy);
                            default: try { return m.invoke(source.api, a); }
                                catch (InvocationTargetException e) { throw e.getCause(); }
                        }
                    });
            playerLocation = system;
            Global.setSettings(settings); Global.setSector(sector); Global.setFactory(factory);
            ControlledCall call = new ControlledCall(); call.roll = roll;
            call.init(Abilities.DISTRESS_CALL, player); ability = call;
        }
        void call() { check(ability.isUsable(), "Distress Call must remain usable"); ability.pressButton(); }
    }

    static final class NativeAid extends DistressResponse {
        boolean completed;
        @Override protected int getNeededFuel() { return 100; }
        @Override protected int getNeededSupplies() { return 20; }
        @Override protected void unrespond() { completed = true; }
        void transfer(Fixture f) {
            fleet = f.responseFleet; playerCargo = f.playerCargo;
            text = mock(TextPanelAPI.class, (n, a) -> n.equals("addPara") ? mock(LabelAPI.class, (x, y) -> null) : null);
            acceptAid();
        }
    }

    public static void main(String[] args) throws Exception {
        SettingsAPI oldSettings = Global.getSettings(); SectorAPI oldSector = Global.getSector(); FactoryAPI oldFactory = Global.getFactory();
        try {
            if (args.length > 0) {
                String source = java.nio.file.Files.readString(java.nio.file.Path.of(args[0]));
                org.json.JSONArray rows = (org.json.JSONArray) Class.forName("com.fs.starfarer.loading.G")
                        .getMethod("o00000", String.class).invoke(null, source);
                boolean found = false;
                for (int i = 0; i < rows.length(); i++) {
                    org.json.JSONObject row = rows.getJSONObject(i);
                    if (!row.getString("id").equals(Abilities.DISTRESS_CALL)) continue;
                    check(!found && row.getString("plugin").equals(OrionKnotDistressCallAbility.class.getName())
                                    && row.getDouble("deactivationCooldown") == 1,
                            "Native ability CSV loader accepts the exact override and ordinary cooldown");
                    found = true;
                }
                check(found, "The native loader must find Distress Call");
            }
            int[] probabilities = new int[5];
            for (int i = 0; i < 10000; i++) probabilities[OrionKnotDistressCallAbility.pickOutcome((i + .5) / 10000).ordinal()]++;
            check(Arrays.equals(probabilities, new int[] {6000, 2500, 400, 100, 1000}), "Exact 60/25/4/1/10 outcome intervals");
            Field count = field(DistressCallAbility.class, "numTimesUsed"), delay = field(DelayedActionScript.class, "daysLeft");
            double[] rolls = {.1, .7, .87, .895, .95};
            for (boolean legacy : new boolean[] {false, true}) for (var source : OrionKnotHiddenMissionRegression.ownedSystems(legacy)) {
                for (int outcome = 0; outcome < rolls.length; outcome++) {
                    Fixture f = new Fixture(source, rolls[outcome]); count.setInt(f.ability, 20);
                    f.call();
                    check(count.getInt(f.ability) == 21 && ((DistressCallAbility) f.ability).getNumUsesInLastPeriod() == 1, "Account for every eligible signal, including silence");
                    check(f.scripts.size() == (outcome == 0 ? 0 : 1), "Silence spawns nothing; all other outcomes schedule one response");
                    f.ability.setCooldownLeft(0f); f.call();
                    check(count.getInt(f.ability) == 21, "Native nearby-repeat limit prevents a reroll");
                    if (outcome == 0) continue;
                    EveryFrameScript response = f.scripts.get(0); float days = delay.getFloat(response);
                    check(days >= 10f && days <= 20f, "Retain ordinary later-call delay");
                    response.advance(days - .01f); check(f.factories == 0, "Wait before materializing response");
                    f.playerPosition.set(50000f, 50000f); response.advance(.02f);
                    int size = outcome == 2 ? 12 : outcome == 3 ? 25 : 4;
                    check(response.isDone() && f.spawned.size() == 1 && f.members.size() == size, "Correct local fleet and full Strike roster size");
                    check(f.readiness.size() == size && f.readiness.stream().allMatch(cr -> cr == .7f), "All ships are combat-ready");
                    check(Math.abs(Vector2f.sub(f.responsePosition, new Vector2f(137f, -290f), null).length() - 3000f) < 1f, "Converge on the original saved signal");
                    check(f.assignments.get(0) == FleetAssignment.INTERCEPT && f.assignments.get(f.assignments.size()-1) == FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, "Local approach and retirement assignments");
                    check(Boolean.TRUE.equals(f.flags.get(MemFlags.MEMORY_KEY_NO_JUMP)), "Keep responders in the calling system");
                    if (outcome == 4) {
                        check(f.faction.equals(TaskForceSpartanFaction.ID) && Boolean.TRUE.equals(f.flags.get("$distressResponse")), "Friendly Spartan responders use native distress dialogue");
                        check(f.responderCargo[0] == 500f && f.responderCargo[1] == 200f, "Rescue tanker and freighter carry fuel/supplies");
                        check(!Boolean.TRUE.equals(f.flags.get(MemFlags.MEMORY_KEY_MAKE_HOSTILE)), "Spartans are not hostile");
                        NativeAid aid = new NativeAid(); aid.transfer(f);
                        check(aid.completed && f.playerCargoAmounts[0] == 100f && f.playerCargoAmounts[1] == 20f, "Native acceptAid actually grants fuel and supplies");
                    } else check(Boolean.TRUE.equals(f.flags.get(MemFlags.MEMORY_KEY_MAKE_HOSTILE)) && Boolean.TRUE.equals(f.flags.get(MemFlags.MEMORY_KEY_IGNORE_PLAYER_COMMS)), "THREAT is hostile and uses nonhuman interaction");
                }
            }
            Fixture f = new Fixture(OrionKnotHiddenMissionRegression.ownedSystems(false).get(0), .7);
            f.call(); check(delay.getFloat(f.scripts.get(0)) >= 1f && delay.getFloat(f.scripts.get(0)) <= 3f, "Retain first-use delay");
            f.playerLocation = null; f.scripts.get(0).advance(4f);
            check(f.spawned.size() == 1 && !f.assignments.contains(FleetAssignment.INTERCEPT), "Departed caller leaves the response at its original system");
            for (String id : new String[] {"ordinary_system", OrionKnotHiddenMissionRegression.IDS[0]}) {
                f = new Fixture(new OrionKnotHiddenMissionRegression.SystemFixture(id), .7); f.call();
                check(f.scripts.size() == 1 && !(f.scripts.get(0) instanceof OrionKnotDistressResponse), "Vanilla help outside owned Orion systems, including foreign ID claims");
            }
            Method rare = method(OdysseyPredatorScript.class, "getRareStrikeTier", double.class);
            int normal=0, second=0, third=0;
            for(int i=0;i<10000;i++) { int tier=(int) rare.invoke(null,(i+.5)/10000); if(tier<0) normal++; else if(tier==1) second++; else if(tier==2) third++; }
            check(normal==9500 && second==400 && third==100, "Exact ambient 95/4/1 probabilities");
            Method addRare = method(OdysseyPredatorScript.class, "addRareStrikeRoster", CampaignFleetAPI.class, double.class);
            for(double roll:new double[]{.2,.96,.995}) {
                f = new Fixture(OrionKnotHiddenMissionRegression.ownedSystems(false).get(0), .1);
                boolean added=(boolean)addRare.invoke(null,f.responseFleet,roll);
                check(added==(roll>=.95) && f.members.size()==(roll<.95?0:roll<.99?12:25), "Ambient rare tier replaces rather than appends the ordinary roster");
            }
            Method refresh = method(ChiefNavigatorModPlugin.class, "refreshOverriddenAbilityPlugins");
            f = new Fixture(OrionKnotHiddenMissionRegression.ownedSystems(false).get(0), .7);
            f.ability = new DistressCallAbility(); f.ability.init(Abilities.DISTRESS_CALL,f.player);
            refresh.invoke(new ChiefNavigatorModPlugin());
            check(f.ability.getClass()==OrionKnotDistressCallAbility.class && f.replacements==1,"Saved vanilla ability receives override");
            refresh.invoke(new ChiefNavigatorModPlugin()); check(f.replacements==1,"No replacement on later loads");
            f.ability=new ForeignCall(); refresh.invoke(new ChiefNavigatorModPlugin()); check(f.ability instanceof ForeignCall,"Preserve another mod's ability implementation");
            System.out.println("PASS: all six current/legacy systems, usable native activation, 60/25/4/1/10 outcomes, silence/use accounting, delays/repeat limit, actual four/12/25-ship responses, native Spartan aid transfer, hostility/locality, vanilla help outside, ambient 95/4/1 rosters and load wiring.");
        } finally { Global.setSettings(oldSettings); Global.setSector(oldSector); Global.setFactory(oldFactory); }
    }
}
