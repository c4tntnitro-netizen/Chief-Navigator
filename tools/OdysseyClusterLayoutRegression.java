package chiefnavigator.quest;

import java.util.ArrayList;
import java.util.List;

/** Headless checks for the authored Odyssey Sector hyperspace layout. */
public final class OdysseyClusterLayoutRegression {
    private static final float DISTANCE_TOLERANCE = 100f;

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static float distance(float[] first, float[] second) {
        float dx = second[0] - first[0];
        float dy = second[1] - first[1];
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static void checkNear(
            float actual,
            float expected,
            String message) {
        check(Math.abs(actual - expected) <= DISTANCE_TOLERANCE,
                message + ": expected about " + expected + ", got " + actual);
    }

    private static void checkEndpointScatter(String systemId) {
        float[][] endpoints = new float[3][];
        for (int index = 0; index < endpoints.length; index++) {
            endpoints[index] = OdysseyExpanseSystem
                    .getStableHyperspaceEndpointOffset(systemId, index);
        }
        float minimumRadius = 1150f;
        float maximumRadius = 1550f;
        for (int first = 0; first < endpoints.length; first++) {
            float radius = distance(new float[] {0f, 0f}, endpoints[first]);
            check(radius >= minimumRadius && radius <= maximumRadius,
                    systemId + " endpoint must remain inside its authored "
                            + "local scatter range");
            for (int second = first + 1;
                    second < endpoints.length; second++) {
                check(distance(endpoints[first], endpoints[second]) >= 1200f,
                        systemId + " endpoints must remain visibly separated");
            }
        }
    }

    public static void main(String[] args) {
        float[] odyssey = OdysseyExpanseSystem.getAuthoredHyperspaceOffset(
                OdysseyExpanseSystem.SYSTEM_ID);
        float[] sanzu = OdysseyExpanseSystem.getAuthoredHyperspaceOffset(
                OdysseyExpanseSystem.SILENT_WAKE_ID);
        float[] avici = OdysseyExpanseSystem.getAuthoredHyperspaceOffset(
                OdysseyExpanseSystem.MESSINA_ID);
        float[] ashen = OdysseyExpanseSystem.getAuthoredHyperspaceOffset(
                OdysseyExpanseSystem.ASHEN_VERGE_ID);
        float[] lastLight = OdysseyExpanseSystem.getAuthoredHyperspaceOffset(
                OdysseyExpanseSystem.LAST_LIGHT_ID);
        float[] devoured = OdysseyExpanseSystem.getAuthoredHyperspaceOffset(
                OdysseyExpanseSystem.DEVOURED_REACH_ID);

        float routeSpacing = distance(odyssey, sanzu);
        checkNear(distance(sanzu, avici), routeSpacing,
                "Sanzu must remain midway between Alpha Odyssey and Avici");
        check(sanzu[1] < odyssey[1], "Sanzu must remain south of Alpha Odyssey");
        check(avici[1] < sanzu[1], "Avici must remain south of Sanzu");

        check(ashen[0] > odyssey[0], "Ashen Verge must be east of Alpha Odyssey");
        checkNear(ashen[1], odyssey[1],
                "Ashen Verge must sit level with Alpha Odyssey");
        checkNear(distance(odyssey, ashen), routeSpacing,
                "Ashen Verge must be one neighborhood step from Alpha Odyssey");

        check(lastLight[0] < odyssey[0],
                "Last Light must be a little west of Alpha Odyssey");
        check(lastLight[1] < odyssey[1],
                "Last Light must be south of Alpha Odyssey");
        checkNear(distance(odyssey, lastLight), routeSpacing,
                "Last Light must be one neighborhood step from Alpha Odyssey");

        check(devoured[1] > ashen[1],
                "Devoured Reach must be north of Ashen Verge");
        checkNear(distance(ashen, devoured), routeSpacing,
                "Devoured Reach must be one neighborhood step from Ashen Verge");

        check(OdysseyExpanseSystem.getAuthoredHyperspaceOffset(
                        "not_an_odyssey_system") == null,
                "Unknown systems must not acquire an authored cluster offset");

        String[] systemIds = {
            OdysseyExpanseSystem.SYSTEM_ID,
            OdysseyExpanseSystem.SILENT_WAKE_ID,
            OdysseyExpanseSystem.MESSINA_ID,
            OdysseyExpanseSystem.ASHEN_VERGE_ID,
            OdysseyExpanseSystem.LAST_LIGHT_ID,
            OdysseyExpanseSystem.DEVOURED_REACH_ID
        };
        List<float[]> allEndpoints = new ArrayList<float[]>();
        List<String> endpointLabels = new ArrayList<String>();
        for (String systemId : systemIds) {
            checkEndpointScatter(systemId);
            float[] systemOffset = OdysseyExpanseSystem
                    .getAuthoredHyperspaceOffset(systemId);
            int count = OdysseyExpanseSystem
                    .getAuthoredHyperspaceEndpointCount(systemId);
            for (int index = 0; index < count; index++) {
                float[] local = OdysseyExpanseSystem
                        .getStableHyperspaceEndpointOffset(systemId, index);
                allEndpoints.add(new float[] {
                    systemOffset[0] + local[0],
                    systemOffset[1] + local[1]
                });
                endpointLabels.add(systemId + " route " + index);
            }
        }
        for (int first = 0; first < allEndpoints.size(); first++) {
            for (int second = first + 1;
                    second < allEndpoints.size(); second++) {
                check(distance(allEndpoints.get(first), allEndpoints.get(second))
                                >= 500f,
                        endpointLabels.get(first) + " overlaps "
                                + endpointLabels.get(second));
            }
        }
        float formationDifference = 0f;
        for (int index = 0; index < 3; index++) {
            formationDifference += distance(
                    OdysseyExpanseSystem.getStableHyperspaceEndpointOffset(
                            OdysseyExpanseSystem.SYSTEM_ID, index),
                    OdysseyExpanseSystem.getStableHyperspaceEndpointOffset(
                            OdysseyExpanseSystem.ASHEN_VERGE_ID, index));
        }
        check(formationDifference >= 1800f,
                "Alpha Odyssey and Ashen Verge must not share a formation");

        System.out.println("Odyssey cluster layout regression passed.");
    }
}
