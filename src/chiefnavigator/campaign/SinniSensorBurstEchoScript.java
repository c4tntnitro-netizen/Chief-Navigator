package chiefnavigator.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.abilities.SensorBurstAbility;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Anchors three complete sensor refires to a temporary warning beacon. */
public final class SinniSensorBurstEchoScript implements EveryFrameScript {
    private static final float[] REFIRE_DAYS = {3f, 6f, 9f};
    private static final float REMOVE_BEACON_AT_DAY = 10f;

    private float elapsedDays;
    private int nextRefire;
    private boolean done;
    private SectorEntityToken beacon;
    private final List<EchoPulse> activePulses = new ArrayList<EchoPulse>();

    /** Retained so an in-progress sequence from an older save can migrate. */
    public SinniSensorBurstEchoScript() {
    }

    public SinniSensorBurstEchoScript(CampaignFleetAPI player) {
        beacon = deployBeacon(player);
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        if (Global.getSector() == null || Global.getSector().isPaused()) return;

        elapsedDays += Global.getSector().getClock().convertToDays(amount);
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (beacon == null && player != null) beacon = deployBeacon(player);

        while (beacon != null && nextRefire < REFIRE_DAYS.length
                && elapsedDays >= REFIRE_DAYS[nextRefire]) {
            EchoPulse pulse = new EchoPulse(
                    "chief_navigator_sensor_echo_" + Global.getSector().genUID());
            pulse.init(Abilities.SENSOR_BURST, beacon);
            pulse.pressButton();
            activePulses.add(pulse);
            attractNearbyEnemies(beacon, player);
            nextRefire++;
        }

        for (Iterator<EchoPulse> iterator = activePulses.iterator();
                iterator.hasNext();) {
            EchoPulse pulse = iterator.next();
            pulse.advance(amount);
            if (!pulse.isInProgress()) {
                pulse.cleanup();
                iterator.remove();
            }
        }

        if (nextRefire >= REFIRE_DAYS.length && activePulses.isEmpty()
                && elapsedDays >= REMOVE_BEACON_AT_DAY) {
            removeBeacon();
            done = true;
        }
    }

    private static SectorEntityToken deployBeacon(CampaignFleetAPI player) {
        if (player == null || player.getContainingLocation() == null) return null;
        LocationAPI location = player.getContainingLocation();
        String id = "chief_navigator_sensor_echo_beacon_"
                + Global.getSector().genUID();
        CustomCampaignEntityAPI result = location.addCustomEntity(
                id,
                "Sensor Warning Beacon",
                SinniSensorBeaconEntityPlugin.ENTITY_TYPE,
                Factions.PLAYER);
        result.setLocation(player.getLocation().x, player.getLocation().y);
        result.setFacing(player.getFacing());
        result.setDiscoverable(false);
        result.getDetectedRangeMod().modifyFlat(id, 12000f);
        return result;
    }

    private void removeBeacon() {
        if (beacon == null) return;
        LocationAPI location = beacon.getContainingLocation();
        if (location != null) location.removeEntity(beacon);
        beacon = null;
    }

    private static void attractNearbyEnemies(
            SectorEntityToken source, CampaignFleetAPI player) {
        if (source == null || player == null
                || source.getContainingLocation() == null) return;
        for (CampaignFleetAPI fleet
                : source.getContainingLocation().getFleets()) {
            if (fleet == null || fleet.isPlayerFleet() || fleet.getAI() == null
                    || fleet.getBattle() != null || !fleet.isHostileTo(player)) {
                continue;
            }
            float noticeRange = fleet.getMaxSensorRangeToDetect(player)
                    + SensorBurstAbility.DETECTABILITY_RANGE_BONUS;
            if (Misc.getDistance(fleet, source) > noticeRange) continue;
            fleet.getAI().addAssignmentAtStart(
                    FleetAssignment.GO_TO_LOCATION,
                    source,
                    3f,
                    "investigating a sensor burst",
                    null);
        }
    }

    /**
     * Keeps the stock burst timing and ping, but treats the beacon as the
     * sensor origin and reveals contacts around it rather than around the ship.
     */
    private static final class EchoPulse extends SensorBurstAbility {
        private final String modifierId;

        private EchoPulse(String modifierId) {
            this.modifierId = modifierId;
        }

        @Override
        protected void activateImpl() {
            super.activateImpl();
            SectorEntityToken source = getEntity();
            if (source != null && source.isInCurrentLocation()) {
                Global.getSoundPlayer().playUISound(
                        "ui_sensor_burst_on", 1f, 1f);
            }
        }

        @Override
        protected void applyEffect(float amount, float level) {
            SectorEntityToken source = getEntity();
            if (source == null || source.getContainingLocation() == null) return;

            if (source.isInCurrentLocation()) {
                Global.getSector().getMemoryWithoutUpdate().set(
                        MemFlags.GLOBAL_SENSOR_BURST_JUST_USED_IN_CURRENT_LOCATION,
                        true,
                        0.1f);
            }
            source.getMemoryWithoutUpdate().set(
                    MemFlags.JUST_DID_SENSOR_BURST, true, 0.1f);

            if (level <= 0f) return;
            CampaignFleetAPI player = Global.getSector().getPlayerFleet();
            for (SectorEntityToken candidate
                    : source.getContainingLocation().getAllEntities()) {
                if (candidate == null || candidate == source
                        || candidate == player) continue;
                float range = SENSOR_RANGE_BONUS * level;
                if (player != null) {
                    range += player.getMaxSensorRangeToDetect(candidate);
                }
                if (Misc.getDistance(source, candidate) > range) continue;
                candidate.forceSensorContactFaderBrightness(level);
                candidate.forceSensorFaderBrightness(level);
            }
        }

        @Override
        public String getModId() {
            return modifierId;
        }
    }
}
