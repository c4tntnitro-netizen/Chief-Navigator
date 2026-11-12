package chiefnavigator.quest;

import chiefnavigator.dwellers.AviciDwellerContent;
import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import java.awt.Color;
import java.lang.reflect.Method;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;
import static chiefnavigator.quest.BudaiSalvageRewardRegression.*;

/** Restored Maw, SEP large-manifestation escorts, one-time conversion and loss preservation. */
public final class BudaiRosterRegression {
    private static final String SPAWNED = "$chief_navigator_budai_spawned";
    private static final String HUNTER = "$chief_navigator_budai_hunter";
    private static final String RETIRED_ESCORT_MARKER =
            "$chief_navigator_budai_post_labor_escort_added";
    private static final String AVICI_COMPLETE =
            "$chief_navigator_sinni_vignette_complete_avici";

    private static final class Fixture {
        final Map<String, Object> saved = new HashMap<>(), local = new HashMap<>();
        final List<FleetMemberAPI> members = new ArrayList<>();
        final List<FleetEventListener> listeners = new ArrayList<>();
        final Vector2f center = new Vector2f(137f, -22000f);
        final Vector2f location = new Vector2f();
        boolean existingMember;
        String hull = BudaiFleetComposition.MAW_HULL;
        int variantWrites;
        boolean player;
        String faction = "dweller";
        final List<String> requestedVariants = new ArrayList<>();
        String shipName;
        final ShipVariantAPI budaiVariant = mock(ShipVariantAPI.class, (name, args) -> null);
        final RepairTrackerAPI repair = mock(RepairTrackerAPI.class,
                (name, args) -> name.equals("getMaxCR") ? 0.7f : null);
        final FleetMemberAPI budai = mock(FleetMemberAPI.class, (name, args) -> {
            if (name.equals("getHullId")) return hull;
            if (name.equals("getVariant")) return budaiVariant;
            if (name.equals("getRepairTracker")) return repair;
            if (name.equals("setVariant")) { hull = BudaiFleetComposition.MAW_HULL; variantWrites++; return null; }
            if (name.equals("setShipName") && !existingMember) {
                shipName = (String) args[0];
                return null;
            }
            if (name.startsWith("set")) {
                throw new AssertionError("Budai's exact member must not be rewritten: " + name);
            }
            return null;
        });
        FleetMemberAPI flagship = budai;
        SectorEntityToken entity;
        boolean busy, transitioning, despawning;
        int added, fleetFactories, memberFactories, flagshipWrites, positionWrites;
        final FleetDataAPI roster = mock(FleetDataAPI.class, (name, args) -> {
            switch (name) {
                case "getMembersListCopy": return new ArrayList<>(members);
                case "addFleetMember": members.add((FleetMemberAPI) args[0]); added++; break;
                case "setFlagship": flagship = (FleetMemberAPI) args[0]; flagshipWrites++; break;
                case "removeFleetMember": members.remove(args[0]); break;
                default: break;
            }
            return null;
        });
        final StatBonus sensorStrength = new StatBonus(), sensorRange = new StatBonus();
        final StatBonus detectedRange = new StatBonus();
        final MutableFleetStatsAPI stats = mock(MutableFleetStatsAPI.class, (name, args) -> {
            if (name.equals("getSensorStrengthMod")) return sensorStrength;
            if (name.equals("getSensorRangeMod")) return sensorRange;
            return null;
        });
        final BattleAPI battle = mock(BattleAPI.class, (name, args) -> null);
        final CampaignFleetAPI fleet = mock(CampaignFleetAPI.class, (name, args) -> {
            switch (name) {
                case "getId": return TroyArrivalScript.CHARYBDIS_FLEET_ID;
                case "isPlayerFleet": return player;
                case "getFaction": return mock(FactionAPI.class,
                        (n, a) -> n.equals("getId") ? faction : null);
                case "getMemoryWithoutUpdate": return memory(local);
                case "getFleetData": return roster;
                case "getFlagship": return flagship;
                case "getEventListeners": return listeners;
                case "addEventListener": listeners.add((FleetEventListener) args[0]); break;
                case "getBattle": return busy ? battle : null;
                case "isInHyperspaceTransition": return transitioning;
                case "isDespawning": return despawning;
                case "getDetectedRangeMod": return detectedRange;
                case "getStats": return stats;
                case "getLocation": return location;
                case "setLocation":
                    location.set((Float) args[0], (Float) args[1]); positionWrites++; break;
                case "despawn":
                    throw new AssertionError("Roster upkeep must not retire an undefeated Budai");
                default: break;
            }
            return null;
        });
        final SectorEntityToken centerToken = mock(SectorEntityToken.class,
                (name, args) -> name.equals("getLocation") ? center : null);
        final StarSystemAPI avici = mock(StarSystemAPI.class, (name, args) -> {
            if (name.equals("getCenter")) return centerToken;
            if (name.equals("getEntityById")) return entity;
            if (name.equals("addEntity")) entity = (SectorEntityToken) args[0];
            return null;
        });

        Fixture(boolean existing) {
            existingMember = existing;
            if (existing) {
                members.add(budai);
                entity = fleet;
                local.put(HUNTER, true);
            }
            Global.setSettings(mock(SettingsAPI.class, (name, args) -> {
                if (name.equals("getColor")) return Color.WHITE;
                if (name.equals("getVariant")) return variant((String) args[0]);
                return null;
            }));
            Global.setSector(mock(SectorAPI.class, (name, args) -> {
                if (name.equals("getMemoryWithoutUpdate")) return memory(saved);
                return null;
            }));
            Global.setFactory(mock(FactoryAPI.class, (name, args) -> {
                if (name.equals("createEmptyFleet")) {
                    fleetFactories++;
                    return fleet;
                }
                if (name.equals("createFleetMember")) {
                    memberFactories++;
                    String id = (String) args[1];
                    requestedVariants.add(id);
                    if (BudaiFleetComposition.MAW_VARIANT.equals(id)) return budai;
                    ShipVariantAPI variant = variant(id);
                    return mock(FleetMemberAPI.class, (n, a) -> {
                        if (n.equals("getVariant")) return variant;
                        if (n.equals("getHullId")) return id.substring(0, id.lastIndexOf('_'));
                        if (n.equals("getRepairTracker")) return repair;
                        return null;
                    });
                }
                return null;
            }));
        }

        ShipVariantAPI variant(String id) {
            final ShipVariantAPI[] value = new ShipVariantAPI[1];
            value[0] = mock(ShipVariantAPI.class, (n, a) -> {
                if (n.equals("getHullVariantId")) return id;
                if (n.equals("clone")) return value[0];
                return null;
            });
            return value[0];
        }

        void completeLabors() {
            saved.put(MenelausTrial.ACCEPTED, true);
            saved.put("$chief_navigator_menelaus_labor_reports_migrated_v1", true);
            saved.put("$chief_navigator_menelaus_final_labor_forced_v1", true);
            saved.put(MenelausTrial.GAUTAMA_DEFEATED, true);
            saved.put(MenelausTrial.FINAL_DEBRIEF_COMPLETE, true);
            check(MenelausTrial.isComplete(), "Fixture must reach actual completed-Labors state");
        }

        void configure() throws Exception {
            Method method = OdysseyPredatorScript.class.getDeclaredMethod(
                    "configureCharybdis", CampaignFleetAPI.class);
            method.setAccessible(true);
            method.invoke(null, fleet);
        }

        CampaignFleetAPI spawn() throws Exception {
            Method method = OdysseyPredatorScript.class.getDeclaredMethod(
                    "spawnCharybdis", StarSystemAPI.class);
            method.setAccessible(true);
            return (CampaignFleetAPI) method.invoke(null, avici);
        }
    }

    public static void main(String[] args) throws Exception {
        SettingsAPI previousSettings = Global.getSettings();
        SectorAPI previousSector = Global.getSector();
        FactoryAPI previousFactory = Global.getFactory();
        Global.setSettings(mock(SettingsAPI.class,
                (name, values) -> name.equals("getColor") ? Color.WHITE : null));
        try {
            verifyEscortedCreation(false);
            verifyEscortedCreation(true);
            verifyNoPostLaborRosterGrowth();
            verifyOneTimeHunterRestoration();
            verifySepComposition();
            verifyBusyAndDefeatedConfigurationGuards();
        } finally {
            Global.setSector(previousSector);
            Global.setFactory(previousFactory);
            Global.setSettings(previousSettings);
        }
        System.out.println("PASS: oversized-Maw variant, SEP large-manifestation escort without Maws, "
                + "one-time saved-Hunter restoration, no escort replenishment, busy/player/foreign guards and no respawn.");
    }

    private static void verifyEscortedCreation(boolean completedLabors) throws Exception {
        Fixture f = new Fixture(false);
        f.saved.put(AVICI_COMPLETE, true);
        if (completedLabors) f.completeLabors();
        check(f.spawn() == f.fleet && f.entity == f.fleet
                        && f.fleetFactories == 1 && f.memberFactories == f.members.size()
                        && f.added == f.members.size() && f.members.size() >= 17 && f.members.size() <= 24
                        && f.members.get(0) == f.budai
                        && f.flagship == f.budai && f.budai.getVariant() == f.budaiVariant
                        && "Budai".equals(f.shipName),
                "Fresh Budai is the named Maw with one SEP large-manifestation escort at either quest stage");
        check(f.requestedVariants.get(0).equals(BudaiFleetComposition.MAW_VARIANT)
                        && f.requestedVariants.stream().filter(id -> id.contains("maw") || id.contains("charybdis")).count() == 1,
                "Budai is the only Maw; special boss weapons remain confined to his variant");
        check(Math.abs(Vector2f.sub(f.location, f.center, null).length() - 100000f) < 0.01f
                        && f.positionWrites == 1,
                "Solo roster change must preserve the 100,000-unit initial position");
        f.saved.put(SPAWNED, true);
        int factories = f.memberFactories;
        check(f.spawn() == null && f.fleetFactories == 1 && f.memberFactories == factories,
                "The one-time spawned latch must still forbid recreation");
        f.saved.remove(SPAWNED);
        f.saved.put(OdysseyPredatorScript.BUDAI_DEFEATED, true);
        check(f.spawn() == null && f.fleetFactories == 1 && f.memberFactories == factories,
                "A permanently defeated Budai never respawns, including after the Labors");
    }

    private static void verifyNoPostLaborRosterGrowth() throws Exception {
        Fixture f = new Fixture(true);
        f.local.put(BudaiFleetComposition.APPLIED, true);
        check(!MenelausTrial.isComplete(), "Opening fixture is before Labors completion");
        for (int pass = 0; pass < 5; pass++) f.configure();
        f.completeLabors();
        for (int pass = 0; pass < 10; pass++) f.configure();
        check(f.members.size() == 1 && f.members.get(0) == f.budai && f.flagship == f.budai
                        && f.added == 0 && f.flagshipWrites == 0 && f.fleetFactories == 0
                        && f.memberFactories == 0 && f.positionWrites == 0
                        && !f.local.containsKey(RETIRED_ESCORT_MARKER),
                "Repeated normal upkeep must not add escorts, replace Budai, or revive the old escort latch");
        check(Boolean.TRUE.equals(f.local.get(HUNTER))
                        && Boolean.TRUE.equals(f.local.get(MemFlags.MEMORY_KEY_NO_JUMP))
                        && Boolean.TRUE.equals(f.local.get(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE))
                        && BudaiMusic.BATTLE_ID.equals(f.local.get(
                                MusicPlayerPluginImpl.COMBAT_MUSIC_SET_MEM_KEY))
                        && f.listeners.size() == 1,
                "Identity, native pursuit, combat music, and the one salvage listener stay active");
    }

    private static void verifyOneTimeHunterRestoration() throws Exception {
        Fixture f = new Fixture(true);
        f.hull = AviciDwellerContent.BUDAI_HULL;
        FleetMemberAPI existingEscort = mock(FleetMemberAPI.class,
                (name, args) -> name.equals("getHullId") ? "shrouded_maw" : null);
        f.members.add(existingEscort);
        f.flagship = existingEscort;
        f.local.put(RETIRED_ESCORT_MARKER, true);
        f.completeLabors();
        f.configure();
        check(f.members.get(0) == f.budai && f.flagship == f.budai && !f.members.contains(existingEscort)
                        && f.hull.equals(BudaiFleetComposition.MAW_HULL) && f.variantWrites == 1
                        && f.memberFactories == f.members.size() - 1
                        && Boolean.TRUE.equals(f.local.get(BudaiFleetComposition.APPLIED))
                        && Boolean.TRUE.equals(f.local.get(RETIRED_ESCORT_MARKER)),
                "Explicit restoration retains the boss member and replaces the old Maw escort once");
        f.members.remove(f.members.size() - 1);
        int size = f.members.size(), factories = f.memberFactories;
        for (int pass = 0; pass < 5; pass++) f.configure();
        check(f.members.size() == size && f.memberFactories == factories && f.variantWrites == 1,
                "Repeated upkeep and escort casualties never refill or repeat the conversion");
        f.members.remove(f.budai);
        check(!OdysseyPredatorScript.isBudaiBattle(f.fleet), "Surviving escorts are not a surviving Budai");
    }

    private static long count(List<String> variants, String id) {
        return variants.stream().filter(id::equals).count();
    }

    private static void verifySepComposition() {
        for (int seed = 0; seed < 100; seed++) {
            List<String> roster = BudaiFleetComposition.escortVariants(new Random(seed));
            long tendrils = count(roster, "shrouded_tendril_Roiling");
            long banshees = count(roster, AviciDwellerContent.BANSHEE_VARIANT);
            long eyes = count(roster, "shrouded_eye_Darkened");
            long maelstroms = count(roster, "shrouded_maelstrom_Menacing");
            check(tendrils >= 8 && tendrils <= 11 && banshees >= 4 && banshees <= 6
                            && eyes >= 2 && eyes <= 4 && count(roster, AviciDwellerContent.BRAIN_VARIANT) == 1
                            && maelstroms >= Math.max(0, 3 - eyes) && maelstroms <= Math.max(0, 5 - eyes)
                            && roster.stream().noneMatch(id -> id.contains("maw") || id.contains("charybdis"))
                            && roster.size() == tendrils + banshees + eyes + maelstroms + 1,
                    "Escort matches SEP's normal largeFleet/HIGH ranges with its Maw entries removed");
        }
    }

    private static void verifyBusyAndDefeatedConfigurationGuards() throws Exception {
        for (String guard : new String[] {"battle", "transition", "despawning", "defeated", "player", "foreign"}) {
            Fixture f = new Fixture(true);
            f.completeLabors();
            if (guard.equals("battle")) f.busy = true;
            if (guard.equals("transition")) f.transitioning = true;
            if (guard.equals("despawning")) f.despawning = true;
            if (guard.equals("defeated")) f.saved.put(OdysseyPredatorScript.BUDAI_DEFEATED, true);
            if (guard.equals("player")) f.player = true;
            if (guard.equals("foreign")) f.faction = "foreign";
            if (guard.equals("foreign")) BudaiFleetComposition.restore(f.fleet); else f.configure();
            check(f.members.size() == 1 && f.members.get(0) == f.budai
                            && f.listeners.isEmpty() && !f.local.containsKey(BudaiFleetComposition.APPLIED)
                            && f.fleetFactories == 0 && f.memberFactories == 0,
                    "Configuration remains deferred/non-spawning under " + guard);
        }
    }
}
