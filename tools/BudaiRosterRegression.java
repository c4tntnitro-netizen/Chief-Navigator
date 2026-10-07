package chiefnavigator.quest;

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

/** Solo Budai creation and non-mutating roster upkeep before/after the Labors. */
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
        final ShipVariantAPI budaiVariant = mock(ShipVariantAPI.class, (name, args) -> null);
        final RepairTrackerAPI repair = mock(RepairTrackerAPI.class,
                (name, args) -> name.equals("getMaxCR") ? 0.7f : null);
        final FleetMemberAPI budai = mock(FleetMemberAPI.class, (name, args) -> {
            if (name.equals("getHullId")) return "chief_navigator_charybdis";
            if (name.equals("getVariant")) return budaiVariant;
            if (name.equals("getRepairTracker")) return repair;
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
                case "removeFleetMember":
                    throw new AssertionError("Normal upkeep must not trim serialized fleet members");
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
            if (existing) {
                members.add(budai);
                entity = fleet;
            }
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
                    check("chief_navigator_charybdis_Ravenous".equals(args[1]),
                            "Budai creation must request its one authored variant, never an escort");
                    return budai;
                }
                return null;
            }));
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
            verifySoloCreation(false);
            verifySoloCreation(true);
            verifyNoPostLaborRosterGrowth();
            verifySerializedRosterIsNotMigrated();
            verifyBusyAndDefeatedConfigurationGuards();
        } finally {
            Global.setSector(previousSector);
            Global.setFactory(previousFactory);
            Global.setSettings(previousSettings);
        }
        System.out.println("PASS: solo Budai creation, no post-Labor escort growth or replacement, "
                + "serialized roster preservation, busy guards, and no respawn.");
    }

    private static void verifySoloCreation(boolean completedLabors) throws Exception {
        Fixture f = new Fixture(false);
        f.saved.put(AVICI_COMPLETE, true);
        if (completedLabors) f.completeLabors();
        check(f.spawn() == f.fleet && f.entity == f.fleet
                        && f.fleetFactories == 1 && f.memberFactories == 1 && f.added == 1
                        && f.members.size() == 1 && f.members.get(0) == f.budai
                        && f.flagship == f.budai && f.budai.getVariant() == f.budaiVariant,
                "Fresh Budai must be exactly its original singleton both before and after the Labors");
        check(Math.abs(Vector2f.sub(f.location, f.center, null).length() - 100000f) < 0.01f
                        && f.positionWrites == 1,
                "Solo roster change must preserve the 100,000-unit initial position");
        f.saved.put(SPAWNED, true);
        check(f.spawn() == null && f.fleetFactories == 1 && f.memberFactories == 1,
                "The one-time spawned latch must still forbid recreation");
        f.saved.remove(SPAWNED);
        f.saved.put(OdysseyPredatorScript.BUDAI_DEFEATED, true);
        check(f.spawn() == null && f.fleetFactories == 1 && f.memberFactories == 1,
                "A permanently defeated Budai never respawns, including after the Labors");
    }

    private static void verifyNoPostLaborRosterGrowth() throws Exception {
        Fixture f = new Fixture(true);
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

    private static void verifySerializedRosterIsNotMigrated() throws Exception {
        Fixture f = new Fixture(true);
        FleetMemberAPI existingEscort = mock(FleetMemberAPI.class,
                (name, args) -> name.equals("getHullId") ? "shrouded_maelstrom" : null);
        f.members.add(existingEscort);
        f.flagship = existingEscort;
        f.local.put(RETIRED_ESCORT_MARKER, true);
        f.completeLabors();
        for (int pass = 0; pass < 5; pass++) f.configure();
        check(f.members.size() == 2 && f.members.get(0) == f.budai
                        && f.members.get(1) == existingEscort && f.flagship == existingEscort
                        && f.flagshipWrites == 0 && f.added == 0 && f.memberFactories == 0
                        && Boolean.TRUE.equals(f.local.get(RETIRED_ESCORT_MARKER)),
                "Retired escort state stays inert; no retroactive trimming, flagship repair, or replenishment");
    }

    private static void verifyBusyAndDefeatedConfigurationGuards() throws Exception {
        for (String guard : new String[] {"battle", "transition", "despawning", "defeated"}) {
            Fixture f = new Fixture(true);
            f.completeLabors();
            if (guard.equals("battle")) f.busy = true;
            if (guard.equals("transition")) f.transitioning = true;
            if (guard.equals("despawning")) f.despawning = true;
            if (guard.equals("defeated")) f.saved.put(OdysseyPredatorScript.BUDAI_DEFEATED, true);
            f.configure();
            check(f.members.size() == 1 && f.members.get(0) == f.budai
                            && f.listeners.isEmpty() && f.local.isEmpty()
                            && f.fleetFactories == 0 && f.memberFactories == 0,
                    "Configuration remains deferred/non-spawning under " + guard);
        }
    }
}
