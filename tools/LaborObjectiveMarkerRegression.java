package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.util.Misc;
import chiefnavigator.weapons.IthacaResearchUpgrades;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Headless lifecycle checks for Labor objective exclamation markers. */
public final class LaborObjectiveMarkerRegression {
    interface Call { Object invoke(String name, Object[] args); }

    private static final String IMPORTANT_PREFIX = "$missionImportant_";

    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(), new Class<?>[] {type},
                (proxy, method, args) -> {
                    if (method.getName().equals("hashCode")) {
                        return System.identityHashCode(proxy);
                    }
                    if (method.getName().equals("equals")) {
                        return proxy == args[0];
                    }
                    Object value = call.invoke(method.getName(), args);
                    if (value != null) return value;
                    Class<?> result = method.getReturnType();
                    if (result == boolean.class) return false;
                    if (result == int.class) return 0;
                    if (result == float.class) return 0f;
                    return null;
                }));
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class MemoryState {
        final Map<String, Object> values = new HashMap<String, Object>();
        final Set<String> importantReasons = new HashSet<String>();
        final MemoryAPI api = mock(MemoryAPI.class, (name, args) -> {
            if (name.equals("getBoolean")) {
                return Boolean.TRUE.equals(values.get((String) args[0]));
            }
            if (name.equals("get")) {
                return values.get((String) args[0]);
            }
            if (name.equals("contains")) {
                String key = (String) args[0];
                if ("$missionImportant".equals(key)) {
                    return !importantReasons.isEmpty();
                }
                return values.containsKey(key);
            }
            if (name.equals("set")) {
                String key = (String) args[0];
                Object value = args[1];
                values.put(key, value);
                if (key.startsWith(IMPORTANT_PREFIX)
                        && Boolean.TRUE.equals(value)) {
                    importantReasons.add(key);
                }
                return null;
            }
            if (name.equals("unset")) {
                String key = (String) args[0];
                values.remove(key);
                importantReasons.remove(key);
                return null;
            }
            return null;
        });

        boolean isImportant() {
            return !importantReasons.isEmpty();
        }
    }

    private static SectorEntityToken entity(
            String id, MemoryState memory) {
        StatBonus detectedRange = new StatBonus();
        return mock(SectorEntityToken.class, (name, args) -> {
            if (name.equals("getId")) return id;
            if (name.equals("getCustomEntityType")) {
                return Entities.STATION_RESEARCH_REMNANT;
            }
            if (name.equals("getMemoryWithoutUpdate")) return memory.api;
            if (name.equals("getDetectedRangeMod")) return detectedRange;
            return null;
        });
    }

    private static StarSystemAPI system(
            String id, Map<String, SectorEntityToken> entities,
            MemoryState memory) {
        return mock(StarSystemAPI.class, (name, args) -> {
            if (name.equals("getOptionalUniqueId")) return id;
            if (name.equals("getEntityById")) {
                return entities.get((String) args[0]);
            }
            if (name.equals("getMemoryWithoutUpdate")) return memory.api;
            return null;
        });
    }

    private static String staticString(Class<?> type, String name)
            throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return (String) field.get(null);
    }

    private static void invoke(Method method, Object... args)
            throws Exception {
        method.invoke(null, args);
    }

    public static void main(String[] args) throws Exception {
        Global.setSettings(mock(SettingsAPI.class, (name, values) -> null));
        String mothershipPrefix = staticString(
                MenelausTrial.class, "MOTHERSHIP_PREFIX");
        String trialReason = staticString(
                MenelausTrial.class, "TRIAL_IMPORTANT");
        String reportMigration = staticString(
                MenelausTrial.class, "LABOR_REPORT_MIGRATION");
        String laborOneReported = staticString(
                MenelausTrial.class, "LABOR_I_REPORTED");
        String laborTwoReported = staticString(
                MenelausTrial.class, "LABOR_II_REPORTED");
        String installedMask = staticString(
                IthacaResearchUpgrades.class, "UNLOCKED_MASK_KEY");
        String consumableMigration = staticString(
                IthacaResearchUpgrades.class,
                "CONSUMABLE_MIGRATION_KEY");
        String researchPrefix = staticString(
                OdysseyExpanseSystem.class, "EXPANSE_RESEARCH_PREFIX");
        String silentResearchPrefix = staticString(
                OdysseyExpanseSystem.class, "SILENT_WAKE_RESEARCH_PREFIX");
        String ashenResearchPrefix = staticString(
                OdysseyExpanseSystem.class, "ASHEN_VERGE_RESEARCH_PREFIX");
        String salvagedPrefix = staticString(
                OdysseyExpanseSystem.class,
                "RESEARCH_STATION_SALVAGED_PREFIX");
        String legacyResearchReason = staticString(
                OdysseyExpanseSystem.class,
                "RESEARCH_STATION_MAP_IMPORTANT");

        MemoryState campaign = new MemoryState();
        MemoryState systemMemory = new MemoryState();
        systemMemory.values.put(
                "$chief_navigator_odyssey_authored_system_v1", true);
        MemoryState mothershipMemory = new MemoryState();
        MemoryState stationMemory = new MemoryState();
        SectorEntityToken mothership = entity(
                mothershipPrefix + "0", mothershipMemory);
        SectorEntityToken researchStation = entity(
                researchPrefix + "0", stationMemory);
        mothershipMemory.values.put(MenelausTrial.MOTHERSHIP_MARKER, true);

        Map<String, SectorEntityToken> expanseEntities =
                new HashMap<String, SectorEntityToken>();
        expanseEntities.put(mothership.getId(), mothership);
        expanseEntities.put(researchStation.getId(), researchStation);
        StarSystemAPI expanse = system(
                OdysseyExpanseSystem.SYSTEM_ID,
                expanseEntities, systemMemory);
        StarSystemAPI silentWake = system(
                OdysseyExpanseSystem.SILENT_WAKE_ID,
                new HashMap<String, SectorEntityToken>(), systemMemory);
        StarSystemAPI ashenVerge = system(
                OdysseyExpanseSystem.ASHEN_VERGE_ID,
                new HashMap<String, SectorEntityToken>(), systemMemory);
        List<StarSystemAPI> systems = new ArrayList<StarSystemAPI>();
        systems.add(expanse);
        systems.add(silentWake);
        systems.add(ashenVerge);
        SectorAPI sector = mock(SectorAPI.class, (name, values) -> {
            if (name.equals("getMemoryWithoutUpdate")) return campaign.api;
            if (name.equals("getStarSystems")) return systems;
            return null;
        });
        Global.setSector(sector);

        Method markResearch = OdysseyExpanseSystem.class.getDeclaredMethod(
                "markResearchStationOnSystemMap", SectorEntityToken.class);
        markResearch.setAccessible(true);
        Method markObjectives = MenelausTrial.class.getDeclaredMethod(
                "markLiveObjectivesImportant", StarSystemAPI.class);
        markObjectives.setAccessible(true);

        // Simulate the two reasons which older saves could carry before the
        // player ever accepted Menelaus's first Labor.
        Misc.makeImportant(mothership, trialReason);
        Misc.makeImportant(researchStation, trialReason);
        Misc.makeImportant(researchStation, legacyResearchReason);
        invoke(markResearch, researchStation);
        MenelausTrial.refreshProgress(expanse);
        check(!mothershipMemory.isImportant(),
                "Mothership must not be marked before Labor I starts");
        check(!stationMemory.isImportant(),
                "Research station must not be marked before Labor II starts");

        // Labor I: all hostile motherships, and only those objectives, are
        // marked until the field objective is complete and report-pending.
        campaign.values.put(reportMigration, true);
        campaign.values.put(MenelausTrial.ACCEPTED, true);
        invoke(markObjectives, expanse);
        check(mothershipMemory.isImportant(),
                "Mothership must be marked while Labor I is active");
        check(!stationMemory.isImportant(),
                "Research station must remain unmarked during Labor I");
        campaign.values.put(MenelausTrial.MOTHERSHIPS_CLEARED, true);
        invoke(markObjectives, expanse);
        check(!mothershipMemory.isImportant(),
                "Mothership marker must clear as soon as Labor I completes");

        // Labor II begins only after Labor I is reported. Salvaging five of
        // six stations must not satisfy it: four components must be delivered
        // and installed in Ithaca's wall through Menelaus's ledger.
        campaign.values.put(laborOneReported, true);
        campaign.values.put(consumableMigration, true);
        invoke(markObjectives, expanse);
        check(stationMemory.isImportant(),
                "Research station must be marked while Labor II is active");
        campaign.values.put(salvagedPrefix + researchPrefix + "1", true);
        campaign.values.put(salvagedPrefix + silentResearchPrefix + "0", true);
        campaign.values.put(salvagedPrefix + silentResearchPrefix + "1", true);
        campaign.values.put(salvagedPrefix + ashenResearchPrefix + "0", true);
        campaign.values.put(salvagedPrefix + ashenResearchPrefix + "1", true);
        invoke(markObjectives, expanse);
        check(stationMemory.isImportant(),
                "Five salvaged stations must not replace component delivery");
        check(MenelausTrial.getResearchStationsResearched() == 5,
                "All six station salvage records remain independently tracked");
        check(!MenelausTrial.isResearchLaborComplete(),
                "Labor II must remain active before four installations");

        campaign.values.put(installedMask, 0b1111);
        check(MenelausTrial.getResearchObjectiveTarget() == 4,
                "Labor II requires exactly four installed upgrades");
        check(MenelausTrial.getResearchObjectiveProgress() == 4,
                "Labor II progress counts installed components");
        check(MenelausTrial.isResearchLaborComplete(),
                "Four installed upgrades must complete the field objective");
        invoke(markObjectives, expanse);
        check(!stationMemory.isImportant(),
                "Research marker must clear as soon as Labor II completes");

        // Saves which already reported Labor II under the historical
        // five-station rule must never be rolled back by the new delivery
        // requirement even if their old component mask is empty.
        campaign.values.put(installedMask, 0);
        campaign.values.put(laborTwoReported, true);
        check(MenelausTrial.isResearchLaborComplete(),
                "A previously reported Labor II remains complete");

        // A still-older save which predates explicit report flags used five
        // salvaged stations as durable completion. Its one-time stage
        // migration must preserve that already-earned advancement.
        campaign.values.remove(laborTwoReported);
        campaign.values.remove(reportMigration);
        check(MenelausTrial.getCurrentLaborStage()
                        == MenelausTrial.LABOR_WALL,
                "Historical five-station saves migrate forward to Labor III");
        check(Boolean.TRUE.equals(campaign.values.get(laborTwoReported)),
                "Historical Labor II completion becomes a durable report");

        System.out.println("PASS: mothership and research-station markers "
                + "appear only during their active Labor; Labor II requires "
                + "four installed components while tracking all six POIs.");
    }
}
