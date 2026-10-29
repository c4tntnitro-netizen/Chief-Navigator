import chiefnavigator.console.RefreshContactBounties;
import chiefnavigator.quest.OdysseyExpanseSystem;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.cb.CBDerelict;
import com.fs.starfarer.api.impl.campaign.missions.cb.CustomBountyCreator;
import com.fs.starfarer.api.impl.campaign.missions.cb.MilitaryCustomBounty;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Read-only diagnostic checks: registry identity, owned-system counts and no false 0/0 success. */
public final class DerelictFilterStatusRegression {
    interface Call { Object invoke(String name, Object[] args); }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[] {type}, (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("hashCode")) return System.identityHashCode(proxy);
                    if (name.equals("equals")) return proxy == args[0];
                    if (name.equals("toString")) return type.getSimpleName();
                    return call.invoke(name, args);
                }));
    }

    static StarSystemAPI system(String id, boolean owned, String marker, boolean hidden) {
        MemoryAPI memory = mock(MemoryAPI.class, (name, args) -> {
            if (name.equals("getBoolean")) return owned && marker.equals(args[0]);
            throw new AssertionError("Diagnostic must not mutate memory: " + name);
        });
        return mock(StarSystemAPI.class, (name, args) -> {
            if (name.equals("getOptionalUniqueId")) return id;
            if (name.equals("getMemoryWithoutUpdate")) return memory;
            if (name.equals("getEntityById")) return null;
            if (name.equals("hasTag")) return hidden && Tags.THEME_HIDDEN.equals(args[0]);
            throw new AssertionError("Diagnostic must not mutate or reconstruct systems: " + name);
        });
    }

    static void world(List<StarSystemAPI> systems) {
        Global.setSector(mock(SectorAPI.class, (name, args) -> {
            if (name.equals("getStarSystems")) return systems;
            throw new AssertionError("Diagnostic must not mutate sector: " + name);
        }));
    }

    public static final class RegisteredDerelictCreator extends CBDerelict {
        @Override public String getId() { return "CBDerelict"; }

        @Override public boolean systemMatchesRequirement(StarSystemAPI system) {
            throw new AssertionError("Hidden-tag status must not call a creator predicate");
        }
    }

    static List<StarSystemAPI> systems(String marker, boolean allHidden) {
        List<StarSystemAPI> systems = new ArrayList<>();
        String[] ids = {OdysseyExpanseSystem.SYSTEM_ID,
                OdysseyExpanseSystem.MESSINA_ID, OdysseyExpanseSystem.SILENT_WAKE_ID,
                OdysseyExpanseSystem.ASHEN_VERGE_ID, OdysseyExpanseSystem.LAST_LIGHT_ID,
                OdysseyExpanseSystem.DEVOURED_REACH_ID};
        for (int index = 0; index < ids.length; index++) {
            systems.add(system(ids[index], true, marker, allHidden || index != ids.length - 1));
            // A tagged, same-ID foreign system must not inflate either count.
            systems.add(system(ids[index], false, marker, true));
        }
        systems.add(system("another_mod_world", true, marker, true));
        return systems;
    }

    static String statusWithoutMutation() {
        SectorAPI sector = Global.getSector();
        List<CustomBountyCreator> registry = MilitaryCustomBounty.CREATORS;
        List<CustomBountyCreator> entries = new ArrayList<>(registry);
        String status = RefreshContactBounties.getDerelictFilterStatus();
        check(Global.getSector() == sector && MilitaryCustomBounty.CREATORS == registry
                        && registry.equals(entries),
                "Diagnostic must preserve sector and registry identity, order and entries");
        return status;
    }

    public static void main(String[] args) throws Exception {
        SectorAPI priorSector = Global.getSector();
        List<CustomBountyCreator> priorCreators = MilitaryCustomBounty.CREATORS;
        try {
            Field field = OdysseyExpanseSystem.class.getDeclaredField("AUTHORED_SYSTEM_MARKER");
            field.setAccessible(true);
            String marker = (String) field.get(null);
            world(systems(marker, true));
            CBDerelict nativeCreator = new CBDerelict();
            RegisteredDerelictCreator customCreator = new RegisteredDerelictCreator();
            MilitaryCustomBounty.CREATORS = new ArrayList<>(Arrays.asList(nativeCreator));
            String nativeStatus = statusWithoutMutation();
            check(nativeStatus.contains("Derelict generator: CBDerelict;")
                            && nativeStatus.contains("Orion Knot systems tagged THEME_HIDDEN: 6/6 "
                                    + "(native mission-search exclusion)."),
                    "Native CBDerelict must report all six actual owned worlds excluded by hidden tags");

            MilitaryCustomBounty.CREATORS = new ArrayList<>(Arrays.asList(nativeCreator, null, customCreator));
            String customStatus = statusWithoutMutation();
            check(customStatus.contains("Derelict generator: RegisteredDerelictCreator;")
                            && customStatus.contains("THEME_HIDDEN: 6/6"),
                    "Diagnostic must name the actual last-matching creator without calling its predicate");

            MilitaryCustomBounty.CREATORS = new ArrayList<>(Arrays.asList(customCreator, nativeCreator));
            String laterNativeStatus = statusWithoutMutation();
            check(laterNativeStatus.contains("Derelict generator: CBDerelict;")
                            && laterNativeStatus.contains("THEME_HIDDEN: 6/6"),
                    "Native registry precedence must not change the world-tag coverage");

            world(systems(marker, false));
            String partialStatus = statusWithoutMutation();
            check(partialStatus.contains("THEME_HIDDEN: 5/6") && !partialStatus.contains("6/6"),
                    "One untagged owned world must remain visible as incomplete exclusion coverage");

            MilitaryCustomBounty.CREATORS = new ArrayList<>(Arrays.asList(customCreator));
            world(Collections.emptyList());
            String empty = statusWithoutMutation();
            check(empty.contains("RegisteredDerelictCreator")
                            && empty.contains("no owned Orion Knot systems found") && !empty.contains("0/0"),
                    "No owned worlds must be explicit and must not present 0/0 as successful coverage");
            Global.setSector(null);
            check(statusWithoutMutation()
                            .contains("no owned Orion Knot systems found"),
                    "No campaign must not claim successful filtering");
            MilitaryCustomBounty.CREATORS = new ArrayList<>();
            check(statusWithoutMutation().contains("not registered"),
                    "An absent registered creator must be reported explicitly");
            System.out.println("PASS: Derelict status reports actual registry precedence, native 6/6 and partial "
                    + "5/6 hidden-tag coverage, no-owned and missing-generator states without predicate calls or mutation.");
        } finally {
            Global.setSector(priorSector);
            MilitaryCustomBounty.CREATORS = priorCreators;
        }
    }
}
