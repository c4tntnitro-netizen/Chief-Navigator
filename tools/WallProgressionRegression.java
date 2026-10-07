import chiefnavigator.ChiefNavigatorModPlugin;
import chiefnavigator.quest.MenelausTrial;
import chiefnavigator.quest.OdysseyExpanseSystem;
import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import java.lang.reflect.*;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;

public class WallProgressionRegression extends WallResearchRegression {
    private static String privateFlag(String fieldName) throws Exception {
        Field field = MenelausTrial.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (String) field.get(null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void main(String[] args) throws Exception {
        Map<String, Boolean> flags = new HashMap<>();
        MemoryAPI memory = mock(MemoryAPI.class,
                (n,a) -> n.equals("getBoolean") ? flags.getOrDefault(a[0], false) : null);
        MemoryAPI generated = mock(MemoryAPI.class,
                (n,a) -> n.equals("getBoolean")
                        && "$chief_navigator_odyssey_research_stations_v1".equals(a[0]));
        List<StarSystemAPI> systems = new ArrayList<>();
        for (String id : new String[] {
                OdysseyExpanseSystem.SYSTEM_ID,
                OdysseyExpanseSystem.SILENT_WAKE_ID,
                OdysseyExpanseSystem.ASHEN_VERGE_ID}) {
            systems.add(mock(StarSystemAPI.class, (n,a) -> {
                if (n.equals("getOptionalUniqueId")) return id;
                if (n.equals("getMemoryWithoutUpdate")) return generated;
                return null;
            }));
        }
        Global.setSector(mock(SectorAPI.class,
                (n,a) -> {
                    if (n.equals("getMemoryWithoutUpdate")) return memory;
                    if (n.equals("getStarSystems")) return systems;
                    return null;
                }));
        Class<?> arrivals = Class.forName(
                "chiefnavigator.quest.OdysseyStrandedFleetsScript");
        Method perseanGate = arrivals
                .getDeclaredMethod("isPerseanArrivalUnlocked");
        perseanGate.setAccessible(true);
        Method hegemonyGate = arrivals
                .getDeclaredMethod("isHegemonyArrivalUnlocked");
        hegemonyGate.setAccessible(true);
        String reportMigration = privateFlag("LABOR_REPORT_MIGRATION");
        String laborIReported = privateFlag("LABOR_I_REPORTED");
        String laborIIIReported = privateFlag("LABOR_III_REPORTED");
        flags.put(reportMigration, true);
        for (int mask = 0; mask < 8; mask++) {
            flags.put(MenelausTrial.ACCEPTED, (mask & 1) != 0);
            flags.put(laborIReported, (mask & 2) != 0);
            flags.put(laborIIIReported, (mask & 4) != 0);
            check((Boolean) perseanGate.invoke(null)
                            == ((mask & 3) == 3),
                    "Persean fleet requires reported Labor I");
            check((Boolean) hegemonyGate.invoke(null)
                            == ((mask & 5) == 5),
                    "Hegemony fleet requires reported Labor III");
        }
        System.out.println("PASS: Labor I and Labor III arrival gates");
    }
}
