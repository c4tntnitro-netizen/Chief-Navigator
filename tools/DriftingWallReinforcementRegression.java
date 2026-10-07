import chiefnavigator.quest.DriftingWallEncounter;
import chiefnavigator.quest.IthacaSectionEncounter;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import java.nio.file.Files;
import java.nio.file.Path;

/** Guards the faction split between hostile Wall and liberated drones. */
public final class DriftingWallReinforcementRegression {
    public static void main(String[] args) throws Exception {
        String battle = Files.readString(Path.of(
                "src/chiefnavigator/quest/DriftingWallBattleCreationPlugin.java"));
        check(!battle.contains("deployDroneGuards") && !battle.contains("spawnShipOrWing"),
                "The Drifting Wall must not spawn a Guard escort");
        check(Factions.DERELICT.equals(
                        DriftingWallEncounter.COMBAT_PROXY_FACTION_ID),
                "The hostile Wall proxy must use the Derelict side");
        check(!IthacaSectionEncounter.COMBAT_GUARD_FACTION_ID.equals(
                        DriftingWallEncounter.COMBAT_PROXY_FACTION_ID),
                "The Wall proxy and liberated Guard patrols must not share a faction");
        System.out.println(
                "Drifting Wall reinforcement faction regression checks passed.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
