package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin.DerelictShipData;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.PerShipData;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.ShipCondition;
import com.fs.starfarer.api.util.Misc;
import java.util.Random;

/** Campaign state and authored objects for Menelaus's covert sensor Labor. */
public final class MenelausSensorLabor {
    public static final String SENSOR_ARRAY_ID =
            "chief_navigator_devoured_sensor_array";
    public static final String DOOM_WRECK_ID =
            "chief_navigator_sanzu_doom_wreck";
    public static final String SENSOR_TARGET =
            "$chief_navigator_sensor_labor_target";
    public static final String DEPLOYED =
            "$chief_navigator_sensor_labor_deployed";
    private static final String DOOM_WRECK_MARKER =
            "$chief_navigator_sensor_labor_doom_wreck";
    private static final String SENSOR_INVALID =
            "$chief_navigator_sensor_labor_target_invalid_v1";
    private static final String DOOM_INVALID =
            "$chief_navigator_sensor_labor_doom_invalid_v1";

    private static final String DOOM_REVEALED =
            "$chief_navigator_sensor_labor_doom_revealed";
    private static final String DOOM_RECOVERED =
            "$chief_navigator_sensor_labor_doom_recovered";
    private static final String SENSOR_IMPORTANT =
            "chief_navigator_sensor_labor_objective";
    private static final String DOOM_IMPORTANT =
            "chief_navigator_sensor_labor_doom";

    private MenelausSensorLabor() { }

    /** Creates the Labor target once when progression unlocks this stage. */
    public static void ensureWorld() {
        if (Global.getSector() == null || !MenelausTrial.isAccepted()) return;

        if (MenelausTrial.getCurrentLaborStage()
                == MenelausTrial.LABOR_SENSOR && !isComplete()) {
            ensureSensorArray();
            revealDoomOption();
        }
        refreshWorldState();
    }

    /** Refreshes markers without recreating a missing serialized objective. */
    public static void refreshWorldState() {
        if (Global.getSector() == null || !MenelausTrial.isAccepted()) return;
        SectorEntityToken array = getSensorArray();
        if (isActive() && !isComplete()) {
            if (array != null) Misc.makeImportant(array, SENSOR_IMPORTANT);
        } else {
            if (array != null) {
                Misc.makeUnimportant(array, SENSOR_IMPORTANT);
            }
        }

        updateDoomRecoveryState();
        MenelausDoomIntel.sync();
    }

    public static boolean isComplete() {
        return Global.getSector() != null
                && memory().getBoolean(DEPLOYED);
    }

    public static boolean isActive() {
        return MenelausTrial.getCurrentLaborStage()
                == MenelausTrial.LABOR_SENSOR;
    }

    public static boolean isDeploymentTarget(SectorEntityToken target) {
        return target != null
                && SENSOR_ARRAY_ID.equals(target.getId())
                && Entities.SENSOR_ARRAY.equals(target.getCustomEntityType())
                && target.getMemoryWithoutUpdate() != null
                && target.getMemoryWithoutUpdate().getBoolean(SENSOR_TARGET);
    }

    public static boolean canDeployAt(SectorEntityToken target) {
        return isActive() && !isComplete() && isDeploymentTarget(target);
    }

    /** Completes the Labor. The package is mission state, never cargo. */
    public static void deploy(TextPanelAPI text) {
        if (!isActive() || isComplete()) return;
        memory().set(DEPLOYED, true);
        SectorEntityToken array = getSensorArray();
        if (array != null) {
            Misc.makeUnimportant(array, SENSOR_IMPORTANT);
        }
        StarSystemAPI expanse = OdysseyExpanseSystem.findExisting();
        if (expanse != null) {
            // Updates Labor IV to its return-and-report state before this
            // interaction closes.
            MenelausTrial.refreshProgress(expanse);
        } else {
            MenelausTrialIntel.syncProgress(true);
        }
        MenelausDoomIntel.sync();
    }

    public static SectorEntityToken getSensorArray() {
        StarSystemAPI reach = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.DEVOURED_REACH_ID);
        if (reach == null) return null;
        SectorEntityToken array = reach.getEntityById(SENSOR_ARRAY_ID);
        return isDeploymentTarget(array) ? array : null;
    }

    public static SectorEntityToken getDoomWreck() {
        StarSystemAPI sanzu = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.SILENT_WAKE_ID);
        if (sanzu == null) return null;
        SectorEntityToken wreck = sanzu.getEntityById(DOOM_WRECK_ID);
        return isOwnedDoomWreck(wreck) ? wreck : null;
    }

    public static boolean isDoomRevealed() {
        return Global.getSector() != null
                && memory().getBoolean(DOOM_REVEALED);
    }

    public static boolean isDoomRecovered() {
        return Global.getSector() != null
                && memory().getBoolean(DOOM_RECOVERED);
    }

    /** Records recovery only from the authored wreck's salvage interaction. */
    public static void markDoomRecovered(SectorEntityToken entity) {
        if (Global.getSector() == null || !isOwnedDoomWreck(entity)) return;
        memory().set(DOOM_RECOVERED, true);
        Misc.makeUnimportant(entity, DOOM_IMPORTANT);
        MenelausDoomIntel.sync();
    }

    private static SectorEntityToken ensureSensorArray() {
        StarSystemAPI reach = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.DEVOURED_REACH_ID);
        if (reach == null) return null;

        SectorEntityToken array = reach.getEntityById(SENSOR_ARRAY_ID);
        if (array != null && !isDeploymentTarget(array)) {
            if (!memory().getBoolean(SENSOR_INVALID)) {
                Global.getLogger(MenelausSensorLabor.class).error(
                        "Sensor Labor ID contains incompatible serialized "
                                + "state; refusing mutation");
                memory().set(SENSOR_INVALID, true);
            }
            return null;
        }
        if (array == null) {
            array = reach.addCustomEntity(
                    SENSOR_ARRAY_ID,
                    "Mara Observation Array",
                    Entities.SENSOR_ARRAY,
                    Factions.NEUTRAL);
        }

        SectorEntityToken mara = OdysseyExpanseSystem.getDevouredWorld(0);
        if (mara != null) {
            array.setFixedLocation(
                    mara.getLocation().x + 900f,
                    mara.getLocation().y - 420f);
        } else {
            array.setFixedLocation(-4800f, 2580f);
        }
        array.setFacing(210f);
        array.setSensorProfile(null);
        array.setDiscoverable(null);
        array.addTag(Tags.HAS_INTERACTION_DIALOG);
        array.getMemoryWithoutUpdate().set(SENSOR_TARGET, true);
        return array;
    }

    private static void revealDoomOption() {
        if (memory().getBoolean(DOOM_REVEALED)) return;
        StarSystemAPI sanzu = OdysseyExpanseSystem.findSystemById(
                OdysseyExpanseSystem.SILENT_WAKE_ID);
        if (sanzu == null) return;

        SectorEntityToken wreck = sanzu.getEntityById(DOOM_WRECK_ID);
        if (wreck == null) {
            PerShipData ship = new PerShipData(
                    "doom_Strike", ShipCondition.AVERAGE, 0f);
            ship.shipName = "TTS Penumbra";
            ship.nameAlwaysKnown = true;
            DerelictShipData params = new DerelictShipData(ship, false);
            wreck = BaseThemeGenerator.addSalvageEntity(
                    sanzu, Entities.WRECK, Factions.NEUTRAL, params);
            wreck.setId(DOOM_WRECK_ID);
            wreck.getMemoryWithoutUpdate().set(DOOM_WRECK_MARKER, true);
            wreck.setName("Wreck of the TTS Penumbra");
            wreck.setFixedLocation(2450f, -5350f);
            wreck.setFacing(137f);

            SalvageSpecialAssigner.ShipRecoverySpecialCreator creator =
                    new SalvageSpecialAssigner.ShipRecoverySpecialCreator(
                            new Random(0x50656e756d627261L),
                            0, 0, false, null, null);
            Misc.setSalvageSpecial(
                    wreck, creator.createSpecial(wreck, null));
        } else if (!isOwnedDoomWreck(wreck)) {
            if (!memory().getBoolean(DOOM_INVALID)) {
                Global.getLogger(MenelausSensorLabor.class).error(
                        "Doom wreck ID contains incompatible serialized "
                                + "state; leaving it untouched");
                memory().set(DOOM_INVALID, true);
            }
            memory().set(DOOM_REVEALED, true);
            return;
        }

        wreck.setSensorProfile(null);
        wreck.setDiscoverable(null);
        Misc.makeImportant(wreck, DOOM_IMPORTANT);
        memory().set(DOOM_REVEALED, true);
        MenelausDoomIntel.ensureExists(null);
    }

    private static void updateDoomRecoveryState() {
        if (!memory().getBoolean(DOOM_REVEALED)
                || memory().getBoolean(DOOM_RECOVERED)) {
            return;
        }
        SectorEntityToken wreck = getDoomWreck();
        if (wreck != null) {
            Misc.makeImportant(wreck, DOOM_IMPORTANT);
        }
    }

    private static boolean isOwnedDoomWreck(SectorEntityToken wreck) {
        return wreck != null && DOOM_WRECK_ID.equals(wreck.getId())
                && Entities.WRECK.equals(wreck.getCustomEntityType())
                && wreck.getMemoryWithoutUpdate() != null
                && wreck.getMemoryWithoutUpdate().getBoolean(
                        DOOM_WRECK_MARKER);
    }

    private static com.fs.starfarer.api.campaign.rules.MemoryAPI memory() {
        return Global.getSector().getMemoryWithoutUpdate();
    }
}
