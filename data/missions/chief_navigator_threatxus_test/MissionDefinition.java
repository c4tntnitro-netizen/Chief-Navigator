package data.missions.chief_navigator_threatxus_test;

import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

/** Isolated single-hull flight, gun-layout and collision test. */
public final class MissionDefinition implements MissionDefinitionPlugin {
    private static final String DISPLAY_NAME = "Tetsu Kannon";
    @Override
    public void defineMission(MissionDefinitionAPI api) {
        api.initFleet(FleetSide.PLAYER, "HSS", FleetGoal.ATTACK, false, 5);
        api.initFleet(FleetSide.ENEMY, "TX", FleetGoal.ATTACK, true, 5);
        api.setFleetTagline(FleetSide.PLAYER, "Capital strike group");
        api.setFleetTagline(FleetSide.ENEMY, DISPLAY_NAME + ": the sleeping hive");
        api.addBriefingItem("Destroy all three attached Threat walls. Their guns and fixed Swarm Launchers defend the inert, protected core.");
        api.addBriefingItem("The exposed core then awakens and hunts you, activating its weapons and fabrication. Kill " + DISPLAY_NAME + " and its surviving units.");
        api.addBriefingItem("Awakened fabrication prioritizes an Assault Unit if none exist; otherwise it builds up to three Fabricators, then fills Assault Unit slots (three maximum).");
        api.addBriefingItem("The assault units attack only with Exodus Plague; use point defense to intercept their projectiles.");
        api.addToFleet(FleetSide.PLAYER, "onslaught_Standard", FleetMemberType.SHIP, "HSS Resolute", true);
        api.addToFleet(FleetSide.PLAYER, "eagle_Assault", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "hammerhead_Balanced", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "hammerhead_Balanced", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "chief_navigator_threatxus_Standard", FleetMemberType.SHIP, DISPLAY_NAME, true);
        api.initMap(-8000f, 8000f, -8000f, 8000f);
    }
}
