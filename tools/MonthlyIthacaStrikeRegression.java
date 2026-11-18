package chiefnavigator.quest;

/** Pure timing/tier boundary checks for the recurring Ithaca strike. */
public final class MonthlyIthacaStrikeRegression {
    private MonthlyIthacaStrikeRegression() { }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void checkNear(float actual, float expected, String message) {
        check(Math.abs(actual - expected) < 0.0001f,
                message + ": expected " + expected + ", got " + actual);
    }

    public static void main(String[] args) {
        checkNear(OdysseyPredatorScript.getMonthlyIthacaStrikeDelay(-1d),
                25f, "delay clamps low rolls");
        checkNear(OdysseyPredatorScript.getMonthlyIthacaStrikeDelay(0d),
                25f, "minimum delay");
        checkNear(OdysseyPredatorScript.getMonthlyIthacaStrikeDelay(0.5d),
                30f, "midpoint delay");
        checkNear(OdysseyPredatorScript.getMonthlyIthacaStrikeDelay(1d),
                35f, "maximum delay");
        checkNear(OdysseyPredatorScript.getMonthlyIthacaStrikeDelay(2d),
                35f, "delay clamps high rolls");

        check(OdysseyPredatorScript.getMonthlyIthacaStrikeTier(0d) == 0,
                "low rolls select First Strike");
        check(OdysseyPredatorScript.getMonthlyIthacaStrikeTier(0.94999d) == 0,
                "ordinary rolls select First Strike");
        check(OdysseyPredatorScript.getMonthlyIthacaStrikeTier(0.95d) == 1,
                "the next four percent select Second Strike");
        check(OdysseyPredatorScript.getMonthlyIthacaStrikeTier(0.98999d) == 1,
                "Second Strike ends below the final percent");
        check(OdysseyPredatorScript.getMonthlyIthacaStrikeTier(0.99d) == 2,
                "the final one percent selects Third Strike");
        check(OdysseyPredatorScript.getMonthlyIthacaStrikeTier(1d) == 2,
                "high boundary selects Third Strike");

        System.out.println("Monthly Ithaca strike regression checks passed.");
    }
}
