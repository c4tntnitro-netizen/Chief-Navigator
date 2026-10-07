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
        check(OdysseyPredatorScript.getMonthlyIthacaStrikeTier(0.49999d) == 0,
                "rolls below one half select First Strike");
        check(OdysseyPredatorScript.getMonthlyIthacaStrikeTier(0.5d) == 1,
                "rolls at one half select Second Strike");
        check(OdysseyPredatorScript.getMonthlyIthacaStrikeTier(1d) == 1,
                "high rolls select Second Strike");

        System.out.println("Monthly Ithaca strike regression checks passed.");
    }
}
