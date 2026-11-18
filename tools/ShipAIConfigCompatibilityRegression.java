import chiefnavigator.ai.BoardingPodFighterAI;
import chiefnavigator.ai.GuardDroneAI;
import chiefnavigator.dwellers.ShroudedBrainGuardianAI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.*;
import java.awt.Color;
import java.util.List;

/** Executes the three constructor call sites against the installed native API. */
public final class ShipAIConfigCompatibilityRegression {
    public static void main(String[] args) {
        boolean broken = List.of(args).contains("--expect-broken");
        SettingsAPI settings = Global.getSettings();
        CombatEngineAPI engine = Global.getCombatEngine();
        try {
            Global.setSettings(AviciDwellerCombatRegression.mock(SettingsAPI.class,
                    (n, a) -> n.equals("getColor") ? Color.WHITE : null));
            Global.setCombatEngine(null);
            ShipAPI ship = AviciDwellerCombatRegression.mock(ShipAPI.class,
                    (n, a) -> n.equals("getId") ? "boarding-constructor" : null);
            Runnable[] cases = {
                () -> {
                    ShipAIConfig config = new ShroudedBrainGuardianAI(ship).getConfig();
                    check("timid".equals(config.personalityOverride) && !config.alwaysStrafeOffensively
                            && !config.backingOffWhileNotVentingAllowed, "Brain configuration");
                },
                () -> {
                    ShipAIConfig config = GuardDroneAI.createFearlessConfig();
                    check("reckless".equals(config.personalityOverride) && config.alwaysStrafeOffensively
                            && !config.backingOffWhileNotVentingAllowed && !config.turnToFaceWithUndamagedArmor
                            && config.burnDriveIgnoreEnemies, "Fearless configuration");
                },
                () -> {
                    ShipAIConfig config = new BoardingPodFighterAI(ship).getConfig();
                    check(config != null && !config.alwaysStrafeOffensively
                            && config.backingOffWhileNotVentingAllowed && config.turnToFaceWithUndamagedArmor
                            && !config.burnDriveIgnoreEnemies && config.personalityOverride == null,
                            "Boarding defaults");
                }
            };
            for (Runnable test : cases) {
                boolean failed = false;
                try { test.run(); }
                catch (NoSuchMethodError error) {
                    check(broken && error.getMessage().contains("ShipAIConfig"), "Unexpected linkage failure");
                    failed = true;
                }
                check(failed == broken, "Constructor compatibility outcome");
            }
            System.out.println(broken ? "PASS: reproduced all three old ShipAIConfig linkage failures"
                    : "PASS: all three AI constructors link to the native API and retain their configurations");
        } finally {
            Global.setCombatEngine(engine);
            Global.setSettings(settings);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
