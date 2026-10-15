package chiefnavigator.quest;

import chiefnavigator.dwellers.AviciDwellerContent;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.lwjgl.util.vector.Vector2f;

/** Four independently owned, finite-roster Avici patrols; never manages another mod's fleets. */
public final class AviciDwellerPopulation implements EveryFrameScript {
    public static final String PATROL_MARKER = "$chief_navigator_avici_manifestation_v1";
    public static final float UNLOCK_DELAY_DAYS = 30f;
    public static final float REPLACEMENT_DELAY_DAYS = 30f;
    private static final String PREFIX = "chief_navigator_avici_manifestation_";
    private static final String GENERATION = "$chief_navigator_avici_manifestation_generation_v1";
    private static final String TENDRIL = "shrouded_tendril_Roiling";
    private static final String EYE = "shrouded_eye_Darkened";
    private static final String MAELSTROM = "shrouded_maelstrom_Menacing";
    private static final String MAW = "shrouded_maw_Ravenous";
    private static final String[] SUFFIXES = {"lesser_0", "lesser_1", "greater_0", "transcendent_0"};
    private static final String[] NAMES = {"Lesser Manifestation", "Lesser Manifestation",
            "Greater Manifestation", "Transcendent Manifestation"};
    private final IntervalUtil interval = new IntervalUtil(1f, 1.5f);

    public static void install() {
        if (Global.getSector() == null) return;
        Global.getSector().removeTransientScriptsOfClass(AviciDwellerPopulation.class);
        Global.getSector().addTransientScript(new AviciDwellerPopulation());
    }

    @Override public void advance(float amount) {
        if (Global.getSector() == null || Global.getSector().isPaused() || amount <= 0f) return;
        interval.advance(amount);
        if (interval.intervalElapsed()) maintainPopulation();
    }

    static void maintainPopulation() {
        if (Global.getSector() == null || Global.getSector().getClock() == null) return;
        StarSystemAPI avici = OdysseyExpanseSystem.findSystemById(OdysseyExpanseSystem.MESSINA_ID);
        if (avici == null || !OdysseyPredatorScript.hasBudaiSpawnedFor(UNLOCK_DELAY_DAYS)) return;
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        for (int slot = 0; slot < SUFFIXES.length; slot++) {
            String id = id(slot);
            SectorEntityToken claim = findClaim(avici, id);
            if (claim != null) {
                if (isOwnedFleet(claim)) {
                    CampaignFleetAPI fleet = (CampaignFleetAPI) claim;
                    // Retained empty tokens, transitions and ongoing interactions remain authoritative.
                    if (fleet.getContainingLocation() == avici && !fleet.isEmpty()
                            && !TroyArrivalScript.isFleetBusyForMutation(fleet)) {
                        ensureLossListener(fleet);
                        configurePatrol(fleet, avici);
                    }
                }
                continue;
            }
            Object loss = memory.get(lostAtKey(id));
            if (memory.getBoolean(spawnedKey(id))) {
                if (!(loss instanceof Number)) continue;
                float elapsed = Global.getSector().getClock().getElapsedDaysSince(((Number) loss).longValue());
                if (!Float.isFinite(elapsed) || elapsed < REPLACEMENT_DELAY_DAYS) continue;
            }
            createPatrol(avici, slot);
        }
    }

    /** Exact IDs and our own marker are both required; a Dweller faction alone grants no ownership. */
    public static boolean isOwnedFleet(SectorEntityToken entity) {
        if (!(entity instanceof CampaignFleetAPI)) return false;
        CampaignFleetAPI fleet = (CampaignFleetAPI) entity;
        return slot(fleet.getId()) >= 0 && !isPlayer(fleet)
                && fleet.getFaction() != null && Factions.DWELLER.equals(fleet.getFaction().getId())
                && fleet.getMemoryWithoutUpdate().getBoolean(PATROL_MARKER);
    }

    private static boolean isPlayer(CampaignFleetAPI fleet) {
        return fleet.isPlayerFleet() || Global.getSector() != null
                && fleet == Global.getSector().getPlayerFleet();
    }

    static String id(int slot) { return PREFIX + SUFFIXES[slot]; }
    private static int slot(String id) {
        for (int slot = 0; slot < SUFFIXES.length; slot++) if (id(slot).equals(id)) return slot;
        return -1;
    }
    private static String spawnedKey(String id) { return "$" + id + "_spawned_v1"; }
    private static String lostAtKey(String id) { return "$" + id + "_lost_at_v1"; }
    private static String generationKey(String id) { return "$" + id + "_generation_v1"; }

    private static SectorEntityToken findClaim(StarSystemAPI avici, String id) {
        SectorEntityToken local = avici.getEntityById(id);
        if (local != null) return local;
        Set<LocationAPI> seen = Collections.newSetFromMap(new IdentityHashMap<LocationAPI, Boolean>());
        List<LocationAPI> locations = new ArrayList<LocationAPI>();
        locations.add(avici);
        if (Global.getSector().getHyperspace() != null) locations.add(Global.getSector().getHyperspace());
        if (Global.getSector().getAllLocations() != null) locations.addAll(Global.getSector().getAllLocations());
        if (Global.getSector().getStarSystems() != null) locations.addAll(Global.getSector().getStarSystems());
        for (LocationAPI location : locations) {
            if (location == null || !seen.add(location)) continue;
            SectorEntityToken entity = location.getEntityById(id);
            if (entity != null) return entity;
            if (location.getFleets() == null) continue;
            for (CampaignFleetAPI fleet : location.getFleets()) {
                if (id.equals(fleet.getId())) return fleet;
            }
        }
        return null;
    }

    private static void createPatrol(StarSystemAPI avici, int slot) {
        String id = id(slot);
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        int generation = memory.getInt(generationKey(id)) + 1;
        List<String> variants = roster(slot, new Random(31L * id.hashCode() + generation));
        // All imported and vanilla definitions must exist before constructing any campaign object.
        for (String variant : variants) {
            if (Global.getSettings().getVariant(variant) == null) return;
        }
        Vector2f location = spawnLocation(avici, slot, generation);
        if (location == null) return;
        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(Factions.DWELLER, NAMES[slot], true);
        if (fleet == null) return;
        for (String variant : variants) {
            FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant);
            if (member == null) return; // The incomplete, unattached fleet is never published.
            fleet.getFleetData().addFleetMember(member);
            member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
        }
        fleet.setId(id);
        fleet.setName(NAMES[slot]);
        fleet.setNoFactionInName(true);
        fleet.setTransponderOn(false);
        fleet.setInflater(null);
        fleet.getMemoryWithoutUpdate().set(PATROL_MARKER, true);
        fleet.getMemoryWithoutUpdate().set(GENERATION, generation);
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
        fleet.getFleetData().sort();
        ensureLossListener(fleet);
        avici.addEntity(fleet);
        fleet.setLocation(location.x, location.y);
        configurePatrol(fleet, avici);
        memory.set(generationKey(id), generation);
        memory.set(spawnedKey(id), true);
        memory.unset(lostAtKey(id));
    }

    private static List<String> roster(int slot, Random random) {
        List<String> variants = new ArrayList<String>();
        int tier = Math.max(0, slot - 1);
        add(variants, TENDRIL, tier == 0 ? 6 + random.nextInt(3)
                : tier == 1 ? 9 + random.nextInt(4) : 11 + random.nextInt(4));
        add(variants, EYE, tier == 2 ? 2 : 1);
        add(variants, MAELSTROM, 1 + random.nextInt(tier == 2 ? 3 : 2));
        if (tier > 0) add(variants, MAW, 1);
        add(variants, AviciDwellerContent.BANSHEE_VARIANT, tier);
        if (tier == 2) add(variants, AviciDwellerContent.BRAIN_VARIANT, 1);
        return variants;
    }

    private static void add(List<String> variants, String variant, int count) {
        for (int i = 0; i < count; i++) variants.add(variant);
    }

    private static Vector2f spawnLocation(StarSystemAPI avici, int slot, int generation) {
        if (avici.getCenter() == null) return null;
        Vector2f center = avici.getCenter().getLocation();
        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = Math.toRadians(slot * 91f + generation * 17f + attempt * 137.5f);
            float radius = 18000f + 3000f * (attempt % 4);
            Vector2f point = new Vector2f(center.x + (float) Math.cos(angle) * radius,
                    center.y + (float) Math.sin(angle) * radius);
            SectorEntityToken habitat = avici.getEntityById(AviciHabitatEncounter.HABITAT_ID);
            if (habitat != null && Misc.getDistance(point, habitat.getLocation()) < 8000f) continue;
            boolean clear = true;
            for (SectorEntityToken jump : avici.getJumpPoints()) {
                if (jump != null && Misc.getDistance(point, jump.getLocation()) < 6000f) clear = false;
            }
            if (clear) return point;
        }
        return null;
    }

    private static void configurePatrol(CampaignFleetAPI fleet, StarSystemAPI avici) {
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        fleet.setNoAutoDespawn(true);
        memory.set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
        memory.set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        memory.set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
        memory.set(MemFlags.MEMORY_KEY_NO_JUMP, true);
        memory.set(MemFlags.MAY_GO_INTO_ABYSS, true);
        if (fleet.getCurrentAssignment() == null
                || fleet.getCurrentAssignment().getAssignment() != FleetAssignment.PATROL_SYSTEM
                || fleet.getCurrentAssignment().getTarget() != avici.getCenter()) {
            fleet.clearAssignments();
            fleet.addAssignment(FleetAssignment.PATROL_SYSTEM, avici.getCenter(), 1000000f,
                    "manifesting within Avici");
        }
        if (fleet.getAI() instanceof ModularFleetAIAPI) {
            ModularFleetAIAPI ai = (ModularFleetAIAPI) fleet.getAI();
            if (ai.getNavModule() != null) {
                for (SectorEntityToken jump : avici.getJumpPoints()) {
                    if (jump != null) ai.getNavModule().avoidEntity(jump, 3000f, 6000f, 3f);
                }
                SectorEntityToken habitat = avici.getEntityById(AviciHabitatEncounter.HABITAT_ID);
                if (habitat != null) ai.getNavModule().avoidEntity(habitat, 4000f, 8000f, 3f);
            }
        }
    }

    private static void ensureLossListener(CampaignFleetAPI fleet) {
        for (FleetEventListener listener : fleet.getEventListeners()) {
            if (listener instanceof LossListener) return;
        }
        fleet.addEventListener(new LossListener());
    }

    private static void recordLoss(CampaignFleetAPI fleet) {
        if (Global.getSector() == null || Global.getSector().getClock() == null || !isOwnedFleet(fleet)) return;
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        String id = fleet.getId();
        if (memory.getInt(generationKey(id)) != fleet.getMemoryWithoutUpdate().getInt(GENERATION)) return;
        StarSystemAPI avici = OdysseyExpanseSystem.findSystemById(OdysseyExpanseSystem.MESSINA_ID);
        if (avici == null) return;
        SectorEntityToken claim = findClaim(avici, id);
        if (claim != null && claim != fleet) return;
        if (!(memory.get(lostAtKey(id)) instanceof Number)) {
            memory.set(lostAtKey(id), Global.getSector().getClock().getTimestamp());
        }
    }

    private static final class LossListener implements FleetEventListener {
        @Override public void reportBattleOccurred(CampaignFleetAPI fleet,
                CampaignFleetAPI primaryWinner, BattleAPI battle) {
            if (fleet != null && fleet.isEmpty() && battle != null
                    && battle.wasFleetDefeated(fleet, primaryWinner)) recordLoss(fleet);
        }

        @Override public void reportFleetDespawnedToListener(CampaignFleetAPI fleet,
                FleetDespawnReason reason, Object param) {
            if (reason == FleetDespawnReason.DESTROYED_BY_BATTLE) recordLoss(fleet);
        }
    }

    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return false; }
}
