package chiefnavigator.quest;

/** Pure direction-boundary checks for Sinni's live navigation briefing. */
public final class SinniNavigationRegression {
    private static void check(String expected, float x, float y) {
        String actual = SinniNavigationGuidance.compassDirection(x, y);
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " for "
                    + x + "," + y + " but got " + actual);
        }
    }

    private static void checkEquals(
            String expected, String actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected <" + expected
                    + "> but got <" + actual + ">");
        }
    }

    private static void checkContains(
            String actual, String expected, String message) {
        if (!actual.contains(expected)) {
            throw new AssertionError(message + ": <" + actual
                    + "> did not contain <" + expected + ">");
        }
    }

    public static void main(String[] args) {
        check("east", 1f, 0f);
        check("north", 0f, 1f);
        check("west", -1f, 0f);
        check("south", 0f, -1f);
        check("northeast", 1f, 1f);
        check("northwest", -1f, 1f);
        check("southwest", -1f, -1f);
        check("southeast", 1f, -1f);
        check("at our present position", 0f, 0f);

        checkEquals("", SinniNavigationGuidance.optionalWallAdvice(
                3, 4, 6, 4, 1, "Sanzu"),
                "Optional upgrades must stay secondary before four installs");
        String twoStations = SinniNavigationGuidance.optionalWallAdvice(
                4, 4, 6, 4, 0, "Sanzu");
        checkContains(twoStations,
                "remaining 2 Ithaca wall upgrades are optional",
                "All post-gate upgrades must be described as optional");
        checkContains(twoStations, "2 research stations remain to hunt down",
                "Every unrecovered optional component must remain visible");
        checkContains(twoStations, "nearest is in Sanzu",
                "Optional guidance must name the next actual system");

        String mixed = SinniNavigationGuidance.optionalWallAdvice(
                4, 4, 6, 5, 1, "Odyssey Expanse");
        checkContains(mixed, "1 recovered component remains uninstalled",
                "Recovered optional components must retain install guidance");
        checkContains(mixed, "Of those, 1 is aboard for delivery to Menelaus",
                "Carried optional components must use their live cargo state");
        checkContains(mixed, "1 research station remains to hunt down",
                "The last unsalvaged optional component must remain visible");
        checkEquals("", SinniNavigationGuidance.optionalWallAdvice(
                6, 4, 6, 6, 0, null),
                "Fully installed research must not leave stale advice");

        checkEquals("", SinniNavigationGuidance.phaseShipAdvice(false, false),
                "Penumbra guidance must wait until its lead is revealed");
        checkContains(SinniNavigationGuidance.phaseShipAdvice(true, false),
                "optional recoverable Doom-class phase cruiser in Sanzu",
                "Unrecovered Penumbra must remain an optional hunt");
        String recovered = SinniNavigationGuidance.phaseShipAdvice(true, true);
        checkContains(recovered, "Bring the recovered TTS Penumbra",
                "Recovered Penumbra guidance must change immediately");
        checkContains(recovered, "survey-communications deployment",
                "Recovered Penumbra must be recommended for Labor IV");
        String primary = "Reach the sensor array in Mara, then deploy Menelaus' sensor package.";
        String penumbra = SinniNavigationGuidance.phaseShipAdvice(true, false);
        String paragraphs = SinniNavigationGuidance.appendAdvice(
                SinniNavigationGuidance.appendAdvice(primary, penumbra), twoStations);
        checkEquals(primary + "\n\n" + penumbra + "\n\n" + twoStations, paragraphs,
                "Sensor objective, Penumbra and optional wall upgrades need separate paragraphs");
        checkEquals(primary, SinniNavigationGuidance.appendAdvice(primary, ""),
                "No blank paragraph when optional advice is absent");
        System.out.println("PASS: Sinni resolves stable sixteen-point "
                + "compass bearings and live optional-objective guidance.");
    }
}
