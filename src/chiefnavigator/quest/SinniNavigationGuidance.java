package chiefnavigator.quest;

import chiefnavigator.weapons.IthacaResearchUpgrades;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import java.util.List;
import org.lwjgl.util.vector.Vector2f;

/** Resolves the current story objective into navigator-readable directions. */
public final class SinniNavigationGuidance {
    private static final String[] COMPASS = {
        "east", "east-northeast", "northeast", "north-northeast",
        "north", "north-northwest", "northwest", "west-northwest",
        "west", "west-southwest", "southwest", "south-southwest",
        "south", "south-southeast", "southeast", "east-southeast"
    };

    private SinniNavigationGuidance() { }

    public static final class Guidance {
        private final String destination;
        private final String objective;
        private final String route;
        private final SectorEntityToken target;

        Guidance(String destination, String objective, String route,
                 SectorEntityToken target) {
            this.destination = destination;
            this.objective = objective;
            this.route = route;
            this.target = target;
        }

        public String getDestination() { return destination; }
        public String getObjective() { return objective; }
        public String getRoute() { return route; }
        public SectorEntityToken getTarget() { return target; }
    }

    public static Guidance getCurrentGuidance() {
        if (Global.getSector() == null) {
            return new Guidance("No course available",
                    "The navigation network is offline.",
                    "No live fleet position is available.", null);
        }

        SectorEntityToken target;
        String destination;
        String objective;

        if (!SinniEventideIntel.isBriefed()) {
            MarketAPI eventide = Global.getSector().getEconomy()
                    .getMarket("eventide");
            target = eventide == null ? null : eventide.getPrimaryEntity();
            destination = withSystem("Sinni's office on Eventide", target);
            objective = "Visit Sinni's office for the private expedition "
                    + "briefing before setting out.";
            return finish(destination, objective, target);
        }

        if (!Global.getSector().getMemoryWithoutUpdate().getBoolean(
                OdysseyExpanseSystem.ENTERED)) {
            StarSystemAPI troy = TroyArrivalScript.findWaypointTroy();
            boolean opened = Global.getSector().getMemoryWithoutUpdate()
                    .getBoolean(TroyArrivalScript.WORMHOLE_OPENED);
            if (opened) {
                target = troy == null ? null
                        : TroyArrivalScript.findOdysseyWormhole(troy);
                destination = withSystem("Troy Terminus", target);
                objective = "Enter the open Terminus. The active fleet must "
                        + "still be worth no more than 50 deployment points.";
            } else {
                target = troy == null ? null
                        : TroyArrivalScript.findDepartureStation(troy);
                destination = withSystem("Troy Expedition Anchorage", target);
                objective = "Dock at the anchorage, store anything above the "
                        + "50-DP expedition limit, and authorize the route.";
            }
            return finish(destination, objective, target);
        }

        if (!MenelausTrial.isAccepted()) {
            target = OdysseyExpanseSystem.getFobIthaca();
            destination = withSystem("FOB Ithaca", target);
            objective = "Reach the macrobase in Ashen Verge and answer its "
                    + "command channel.";
            return finish(destination, objective, target);
        }

        int stage = MenelausTrial.getCurrentLaborStage();
        target = MenelausTrial.getCurrentObjectiveLocation();
        if (MenelausTrial.isLaborReportPending(stage)) {
            destination = withSystem("FOB Ithaca", target);
            if (stage == MenelausTrial.LABOR_RESEARCH) {
                objective = "Return to Menelaus at FOB Ithaca to report "
                        + "Labor II. All four required upgrades are already "
                        + "installed in Ithaca's defense wall.";
            } else {
                objective = "The field objective is complete. Return to "
                        + "Menelaus and report before the next Labor can "
                        + "begin.";
            }
        } else if (stage == MenelausTrial.LABOR_MOTHERSHIPS) {
            destination = withSystem(
                    MenelausTrial.getMothershipDisplayName(target), target);
            objective = "Break this sealed Mothership's command partition. "
                    + MenelausTrial.getMothershipsRemaining()
                    + " Mothership objective(s) remain.";
        } else if (stage == MenelausTrial.LABOR_RESEARCH) {
            int remaining = Math.max(0,
                    MenelausTrial.getResearchObjectiveTarget()
                    - MenelausTrial.getResearchObjectiveProgress());
            if (MenelausTrial.canCompleteResearchLaborAtIthaca()) {
                destination = withSystem("FOB Ithaca", target);
                objective = "Return to Menelaus with the recovered "
                        + "components aboard and use his defense-upgrade "
                        + "ledger to install the remaining " + remaining
                        + " required wall " + plural(remaining,
                                "upgrade", "upgrades") + ".";
            } else {
                destination = withSystem(nameOr(target,
                        "Derelict research station"), target);
                objective = "Salvage this station's unique defense "
                        + "component, then deliver it to Menelaus at FOB "
                        + "Ithaca for installation in the defense wall. "
                        + remaining + " required wall " + plural(remaining,
                                "upgrade", "upgrades")
                        + " still need installation.";
            }
        } else if (stage == MenelausTrial.LABOR_WALL) {
            destination = withSystem("Drifting Wall Fragment", target);
            objective = "Disable the Wall's rear reactor while preserving the "
                    + "station for Menelaus.";
        } else if (stage == MenelausTrial.LABOR_SENSOR) {
            destination = withSystem("Mara Observation Array", target);
            objective = "Reach the sensor array in Mara, then deploy "
                    + "Menelaus' sensor package.";
            objective = appendAdvice(objective, phaseShipAdvice(
                    MenelausSensorLabor.isDoomRevealed(),
                    MenelausSensorLabor.isDoomRecovered()));
        } else if (stage == MenelausTrial.LABOR_GAUTAMA) {
            if (!MenelausTrial.isFinalLaborBriefed()) {
                destination = withSystem("FOB Ithaca", target);
                objective = "Menelaus is waiting to offer the fifth and final "
                        + "Labor. Return to Ithaca and decide whether to accept.";
            } else if (MenelausTrial.isFinalDebriefPending()) {
                destination = withSystem("FOB Ithaca", target);
                objective = "The Heavenly Strike is dead. Return to Menelaus "
                        + "and report the fifth Labor complete.";
            } else {
                destination = withSystem(nameOr(target,
                        "Ithaca invasion objective"), target);
                int progress = OdysseyPredatorScript
                        .getFinalLaborInvasionProgress();
                objective = progress < 3
                        ? "Destroy the remaining perimeter Strike before its "
                                + "bastion deadline expires."
                        : "Join Task Force Spartan at the Breach Defense Station "
                                + "and destroy Gautama's Heavenly Strike.";
            }
        } else if (stage == MenelausTrial.LABOR_COMPLETE) {
            StarSystemAPI ashen = OdysseyExpanseSystem.findSystemById(
                    OdysseyExpanseSystem.ASHEN_VERGE_ID);
            target = ashen == null ? target : ashen.getEntityById(
                    OdysseyExpanseSystem.FOB_ITHACA_GATE_ID);
            if (!OdysseyExpanseSystem.isFobIthacaGate(target)) target = null;
            destination = withSystem("Ithaca Janus Gate", target);
            objective = "The gate cordon is open. Scan Ithaca's Janus Gate "
                    + "when you are ready to continue.";
        } else {
            destination = withSystem("FOB Ithaca", target);
            objective = "Return to Menelaus for the next Labor.";
        }
        objective = appendAdvice(objective, currentOptionalWallAdvice());
        return finish(destination, objective, target);
    }

    private static String currentOptionalWallAdvice() {
        int installed = MenelausTrial.getResearchObjectiveProgress();
        int required = MenelausTrial.getResearchObjectiveTarget();
        int total = MenelausTrial.getResearchStationTotal();
        if (installed < required || installed >= total) return "";

        List<SectorEntityToken> stations =
                OdysseyExpanseSystem.getRemainingResearchStations();
        return optionalWallAdvice(
                installed,
                required,
                total,
                MenelausTrial.getResearchStationsResearched(),
                IthacaResearchUpgrades.getAvailableCount(),
                nearestSystemName(stations));
    }

    /** Pure formatter kept package-visible for the focused regression check. */
    static String optionalWallAdvice(
            int installed,
            int required,
            int total,
            int researched,
            int available,
            String nearestSystem) {
        if (installed < required || installed >= total) return "";

        installed = Math.max(0, Math.min(installed, total));
        researched = Math.max(installed, Math.min(researched, total));
        int optional = total - installed;
        int recovered = researched - installed;
        int aboard = Math.max(0, Math.min(available, recovered));
        int unrecovered = total - researched;

        StringBuilder result = new StringBuilder();
        result.append("The remaining ").append(optional)
                .append(" Ithaca wall ")
                .append(plural(optional, "upgrade is", "upgrades are"))
                .append(" optional.");
        if (recovered > 0) {
            result.append(' ').append(recovered).append(" recovered ")
                    .append(plural(recovered, "component remains",
                            "components remain"))
                    .append(" uninstalled.");
            if (aboard > 0) {
                result.append(" Of those, ").append(aboard).append(' ')
                        .append(plural(aboard, "is", "are"))
                        .append(" aboard for delivery to Menelaus.");
            }
        }
        if (unrecovered > 0) {
            result.append(' ').append(unrecovered).append(" research ")
                    .append(plural(unrecovered, "station remains",
                            "stations remain"))
                    .append(" to hunt down");
            if (nearestSystem != null && !nearestSystem.trim().isEmpty()) {
                result.append("; the nearest is in ")
                        .append(nearestSystem.trim());
            }
            result.append('.');
        }
        return result.toString();
    }

    /** Pure formatter kept package-visible for the focused regression check. */
    static String phaseShipAdvice(boolean revealed, boolean recovered) {
        if (recovered) {
            return "Bring the recovered TTS Penumbra for the Mara "
                    + "survey-communications deployment. Sinni recommends "
                    + "its phase cloak for slipping through Devoured Reach "
                    + "before deploying the sensor package.";
        }
        if (revealed) {
            return "The TTS Penumbra, an optional recoverable Doom-class "
                    + "phase cruiser in Sanzu, is worth hunting down; its "
                    + "phase cloak would suit this survey-communications "
                    + "deployment.";
        }
        return "";
    }

    private static String nearestSystemName(
            List<SectorEntityToken> stations) {
        if (stations == null || stations.isEmpty()) return null;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        Vector2f origin = player == null
                ? null : player.getLocationInHyperspace();
        SectorEntityToken nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (SectorEntityToken station : stations) {
            if (station == null) continue;
            if (nearest == null) nearest = station;
            Vector2f position = station.getLocationInHyperspace();
            if (origin == null || position == null) continue;
            float candidate = distance(origin, position);
            if (candidate < nearestDistance) {
                nearest = station;
                nearestDistance = candidate;
            }
        }
        return nearest == null ? null
                : locationName(nearest.getContainingLocation());
    }

    /** Keep optional objectives on their own paragraphs in every guidance view. */
    static String appendAdvice(String objective, String advice) {
        return advice == null || advice.isEmpty()
                ? objective : objective + "\n\n" + advice;
    }

    private static String plural(int count, String singular, String plural) {
        return count == 1 ? singular : plural;
    }

    private static Guidance finish(
            String destination, String objective, SectorEntityToken target) {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        return new Guidance(destination, objective,
                buildRoute(player, target, destination), target);
    }

    static String buildRoute(SectorEntityToken player,
                             SectorEntityToken target,
                             String destination) {
        if (player == null || target == null) {
            return "Course data is incomplete; use the destination name on "
                    + "the navigation display.";
        }
        LocationAPI fromLocation = player.getContainingLocation();
        LocationAPI targetLocation = target.getContainingLocation();
        if (fromLocation != null && fromLocation == targetLocation) {
            Vector2f from = player.getLocation();
            Vector2f to = target.getLocation();
            float distance = distance(from, to);
            if (distance < 50f) {
                return "We are already at " + nameOr(target, destination)
                        + ".";
            }
            return "Within " + locationName(targetLocation) + ", steer "
                    + compassDirection(to.x - from.x, to.y - from.y)
                    + ". " + nameOr(target, destination) + " is about "
                    + roundedDistance(distance) + " campaign units away.";
        }

        Vector2f from = player.getLocationInHyperspace();
        Vector2f to = target.getLocationInHyperspace();
        if (from == null || to == null) {
            return "Leave through a jump point and enter "
                    + locationName(targetLocation) + "; then close on "
                    + nameOr(target, destination) + ".";
        }
        String bearing = compassDirection(to.x - from.x, to.y - from.y);
        if (fromLocation != null && fromLocation.isHyperspace()) {
            return locationName(targetLocation) + " lies " + bearing
                    + " of our current hyperspace position. Enter the system "
                    + "and close on " + nameOr(target, destination) + ".";
        }
        return "Leave " + locationName(fromLocation)
                + " through a jump point. In hyperspace, "
                + locationName(targetLocation) + " lies " + bearing
                + "; enter it and close on " + nameOr(target, destination)
                + ".";
    }

    public static String compassDirection(float dx, float dy) {
        if (Math.abs(dx) < 0.001f && Math.abs(dy) < 0.001f) {
            return "at our present position";
        }
        double degrees = Math.toDegrees(Math.atan2(dy, dx));
        if (degrees < 0d) degrees += 360d;
        int index = ((int) Math.floor((degrees + 11.25d) / 22.5d)) % 16;
        return COMPASS[index];
    }

    private static float distance(Vector2f a, Vector2f b) {
        if (a == null || b == null) return 0f;
        float dx = b.x - a.x;
        float dy = b.y - a.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static String roundedDistance(float distance) {
        if (distance < 250f) return "less than 250";
        int rounded = Math.max(250, Math.round(distance / 250f) * 250);
        return Integer.toString(rounded);
    }

    private static String withSystem(
            String destination, SectorEntityToken target) {
        if (target == null) return destination;
        return destination + " — " + locationName(target.getContainingLocation());
    }

    private static String nameOr(SectorEntityToken target, String fallback) {
        return target == null || target.getName() == null
                || target.getName().trim().isEmpty()
                        ? fallback : target.getName();
    }

    private static String locationName(LocationAPI location) {
        if (location == null) return "the target system";
        String name = location.getNameWithNoType();
        return name == null || name.trim().isEmpty()
                ? "the target system" : name;
    }

}
