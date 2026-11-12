package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.campaign.BaseLocation;
import com.fs.starfarer.campaign.CampaignClock;
import com.fs.starfarer.campaign.CampaignEngine;
import com.fs.starfarer.campaign.Faction;
import com.fs.starfarer.campaign.Hyperspace;
import com.fs.starfarer.campaign.JumpPoint;
import com.fs.starfarer.campaign.StarSystem;
import com.fs.starfarer.campaign.fleet.CampaignFleet;
import com.fs.starfarer.coreui.A.H;
import com.fs.starfarer.coreui.A.O0Oo;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lwjgl.util.vector.Vector2f;
import org.json.JSONObject;
import sun.misc.Unsafe;

/** Exercises the real native anchor generator and course-line target selection. */
public final class TroyNavigationRegression {
    private static final String AUTHORED = "$chief_navigator_troy_authored_system_v1";
    private static final String LEGACY = "$chief_navigator_troy_canonical_topology_v4";
    private static Unsafe unsafe;

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    interface Call { Object invoke(String name, Object[] arguments); }

    private static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("hashCode")) return System.identityHashCode(proxy);
                    if (name.equals("equals")) return proxy == args[0];
                    if (name.equals("toString")) return type.getSimpleName();
                    Object result = call.invoke(name, args);
                    if (result != null) return result;
                    Class<?> returns = method.getReturnType();
                    if (returns == boolean.class) return false;
                    if (returns == int.class) return 0;
                    if (returns == long.class) return 0L;
                    if (returns == float.class) return 0f;
                    if (returns == double.class) return 0d;
                    return null;
                }));
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        return type.cast(unsafe.allocateInstance(type));
    }

    // Constructors are bypassed only in this external harness to avoid textures,
    // economy initialization, and OpenGL. The tested generator/render code is native.
    static final class NativeSystem extends StarSystem {
        String id;
        Vector2f position;
        Map<String, Object> flags;
        MemoryAPI memory;
        int generationCalls;
        NativeSystem() { super("unused"); }
        @Override public String getOptionalUniqueId() { return id; }
        @Override public Vector2f getLocation() { return position; }
        @Override public MemoryAPI getMemoryWithoutUpdate() { return memory; }
        @Override public void generateAnchorIfNeeded() {
            generationCalls++;
            super.generateAnchorIfNeeded();
        }
    }

    static final class NativeHyperspace extends Hyperspace {
        List<SectorEntityToken> entities;
        @Override public boolean isHyperspace() { return true; }
        @Override public boolean isHyperspaceMode() { return true; }
        @Override public void addEntity(SectorEntityToken entity) { entities.add(entity); }
        @Override public List<SectorEntityToken> getAllEntities() { return entities; }
    }

    static final class NativeJump extends JumpPoint {
        BaseLocation where;
        Vector2f position;
        List<JumpPointAPI.JumpDestination> destinations;
        NativeJump() { super("unused", "unused"); }
        @Override public boolean isInHyperspace() { return where.isHyperspace(); }
        @Override public BaseLocation getContainingLocation() { return where; }
        @Override public Vector2f getLocation() { return position; }
        @Override public List<JumpPointAPI.JumpDestination> getDestinations() { return destinations; }
    }

    static final class NativePlayer extends CampaignFleet {
        Vector2f position;
        @Override public Vector2f getLocation() { return position; }
    }

    static final class NativeCourse extends O0Oo {
        SectorEntityToken target;
        @Override public SectorEntityToken getCurrentTarget() { return target; }
    }

    static final class NativeMap extends H {
        BaseLocation where;
        NativeMap() { super(false); }
        @Override public boolean isRadarMode() { return false; }
        @Override public BaseLocation getLocation() { return where; }
    }

    static final class NativeEngine extends CampaignEngine {
        NativeHyperspace hyper;
        NativePlayer player;
        CampaignEngine.o ui;
        CampaignClock testClock;
        @Override public Hyperspace getHyperspace() { return hyper; }
        @Override public CampaignFleet getPlayerFleet() { return player; }
        @Override public CampaignEngine.o getCampaignUI() { return ui; }
        @Override public Faction getFaction(String id) { return null; }
        @Override public CampaignClock getClock() { return testClock; }
    }

    private static NativeSystem system(String id, String marker) throws Exception {
        NativeSystem system = allocate(NativeSystem.class);
        system.id = id;
        system.position = new Vector2f(61321f, -47234f);
        system.flags = new HashMap<>();
        if (marker != null) system.flags.put(marker, true);
        system.flags.put("saved_quest_progress", "unchanged");
        system.memory = mock(MemoryAPI.class, (name, args) -> {
            if (name.equals("getBoolean")) return Boolean.TRUE.equals(system.flags.get(args[0]));
            throw new AssertionError("Anchor repair must not alter system memory: " + name);
        });
        return system;
    }

    private static void setSector(List<StarSystemAPI> systems, NativeHyperspace hyper) {
        Global.setSector(mock(SectorAPI.class, (name, args) -> {
            if (name.equals("getStarSystems")) return systems;
            if (name.equals("getHyperspace")) return hyper;
            throw new AssertionError("Anchor repair must not regenerate the campaign: " + name);
        }));
    }

    /** Finds the actual obfuscated render method by its calls, not its obfuscated name. */
    private static Method courseRenderer() throws Exception {
        try (DataInputStream in = new DataInputStream(H.class.getResourceAsStream("H.class"))) {
            check(in.readInt() == 0xcafebabe, "Native renderer must be a Java class");
            in.readUnsignedShort(); in.readUnsignedShort();
            Object[] pool = new Object[in.readUnsignedShort()];
            for (int i = 1; i < pool.length; i++) {
                int tag = in.readUnsignedByte();
                if (tag == 1) pool[i] = in.readUTF();
                else if (tag == 7 || tag == 8 || tag == 16 || tag == 19 || tag == 20) in.readUnsignedShort();
                else if (tag == 9 || tag == 10 || tag == 11 || tag == 12) {
                    pool[i] = new int[] {tag, in.readUnsignedShort(), in.readUnsignedShort()};
                } else if (tag == 3 || tag == 4 || tag == 17 || tag == 18) in.readInt();
                else if (tag == 5 || tag == 6) { in.readLong(); i++; }
                else if (tag == 15) { in.readUnsignedByte(); in.readUnsignedShort(); }
                else throw new AssertionError("Unknown native constant tag " + tag);
            }
            in.readUnsignedShort(); in.readUnsignedShort(); in.readUnsignedShort();
            for (int count = in.readUnsignedShort(); count > 0; count--) in.readUnsignedShort();
            for (int count = in.readUnsignedShort(); count > 0; count--) skipMember(in);
            Method found = null;
            for (int count = in.readUnsignedShort(); count > 0; count--) {
                in.readUnsignedShort();
                String name = (String) pool[in.readUnsignedShort()];
                String descriptor = (String) pool[in.readUnsignedShort()];
                for (int attrs = in.readUnsignedShort(); attrs > 0; attrs--) {
                    String attribute = (String) pool[in.readUnsignedShort()];
                    byte[] bytes = in.readNBytes(in.readInt());
                    if (!attribute.equals("Code") || !descriptor.equals("(F)V")) continue;
                    try (DataInputStream codeIn = new DataInputStream(new ByteArrayInputStream(bytes))) {
                        codeIn.readUnsignedShort(); codeIn.readUnsignedShort();
                        byte[] code = codeIn.readNBytes(codeIn.readInt());
                        if (calls(code, pool, "getCurrentTarget") && calls(code, pool, "getHyperspaceAnchor")) {
                            check(found == null, "Native course render method must be unique");
                            found = H.class.getDeclaredMethod(name, float.class);
                        }
                    }
                }
            }
            check(found != null, "Native course render branch must exist");
            found.setAccessible(true);
            return found;
        }
    }

    private static void skipMember(DataInputStream in) throws Exception {
        in.readUnsignedShort(); in.readUnsignedShort(); in.readUnsignedShort();
        for (int attrs = in.readUnsignedShort(); attrs > 0; attrs--) {
            in.readUnsignedShort(); in.skipNBytes(in.readInt());
        }
    }

    private static boolean calls(byte[] code, Object[] pool, String name) {
        for (int i = 0; i + 2 < code.length; i++) {
            int opcode = code[i] & 255;
            if (opcode != 0xb6 && opcode != 0xb9) continue;
            int index = (code[i + 1] & 255) << 8 | (code[i + 2] & 255);
            if (index == 0 || index >= pool.length || !(pool[index] instanceof int[])) continue;
            int[] reference = (int[]) pool[index];
            if (reference[0] != 10 && reference[0] != 11) continue;
            int[] nameAndType = (int[]) pool[reference[2]];
            if (name.equals(pool[nameAndType[1]])) return true;
        }
        return false;
    }

    private static void checkNativeCrashAndRepair(NativeEngine engine) throws Exception {
        NativeSystem troy = system(TroyArrivalScript.SYSTEM_ID, AUTHORED);
        NativeSystem foreign = system("another_mod_system", AUTHORED);
        Map<String, Object> flags = new HashMap<>(troy.flags);
        NativeJump entrance = allocate(NativeJump.class);
        entrance.where = engine.hyper;
        entrance.position = new Vector2f(61321f, -47234f);
        entrance.destinations = new ArrayList<>();
        SectorEntityToken local = mock(SectorEntityToken.class, (name, args) -> {
            if (name.equals("getStarSystem") || name.equals("getContainingLocation")) return troy;
            throw new AssertionError("Course selection must preserve the local endpoint: " + name);
        });
        JumpPointAPI.JumpDestination destination = new JumpPointAPI.JumpDestination(local, "Troy");
        entrance.destinations.add(destination);
        engine.hyper.entities.add(entrance);
        NativeCourse course = allocate(NativeCourse.class);
        course.target = entrance;
        engine.ui = mock(CampaignEngine.o.class, (name, args) ->
                name.equals("getCourseWidget") ? course : null);
        NativeMap map = allocate(NativeMap.class);
        map.where = engine.hyper;
        engine.player.position = new Vector2f(troy.position);
        setSector(List.of(troy, foreign), engine.hyper);
        Method renderer = courseRenderer();
        try {
            renderer.invoke(map, 1f);
            throw new AssertionError("Native map rendering must reproduce the missing-anchor crash");
        } catch (InvocationTargetException failure) {
            check(failure.getCause() instanceof NullPointerException,
                    "Before repair, the actual native renderer must fail at its null map target");
            check(failure.getCause().getStackTrace()[0].getClassName().equals(H.class.getName())
                            && failure.getCause().getMessage().contains("SectorEntityToken.getLocation"),
                    "Reproduce the reported native map-target dereference, not an unrelated initialization error");
        }

        TroyArrivalScript.repairWaypointTroyMapAnchor();
        SectorEntityToken anchor = troy.getHyperspaceAnchor();
        check(anchor != null && anchor.getContainingLocation() == engine.hyper,
                "Repair must create a real native hyperspace anchor");
        check(anchor.getLocation().equals(troy.position) && anchor.hasTag("system_anchor"),
                "Native anchor must use saved Troy coordinates and native anchor tag");
        check(anchor.getMemoryWithoutUpdate().get("$anchor_starSystem") == troy,
                "Native anchor must retain its system link");
        // The real renderer now resolves and dereferences the repaired anchor.
        // Player and anchor coincide, so its normal distance guard returns before GL.
        renderer.invoke(map, 1f);
        TroyArrivalScript.repairWaypointTroyMapAnchor();
        check(troy.getHyperspaceAnchor() == anchor && troy.generationCalls == 1,
                "Repeated load repair must preserve the existing anchor identity");
        check(engine.hyper.entities.equals(List.of(entrance, anchor)),
                "Repair must add only the map anchor, without rebuilding endpoints");
        check(entrance.destinations.equals(List.of(destination)) && destination.getDestination() == local
                        && entrance.position.equals(new Vector2f(61321f, -47234f))
                        && troy.position.equals(new Vector2f(61321f, -47234f))
                        && troy.flags.equals(flags) && foreign.generationCalls == 0,
                "Repair must preserve destinations, coordinates, flags, and foreign systems");
    }

    private static void checkScope(NativeEngine engine) throws Exception {
        NativeSystem present = system(TroyArrivalScript.SYSTEM_ID, AUTHORED);
        SectorEntityToken savedAnchor = mock(SectorEntityToken.class, (name, args) -> {
            throw new AssertionError("Existing anchors must not be inspected or modified: " + name);
        });
        present.setHyperspaceAnchor(savedAnchor);
        setSector(List.of(present), engine.hyper);
        TroyArrivalScript.repairWaypointTroyMapAnchor();
        check(present.generationCalls == 0 && present.getHyperspaceAnchor() == savedAnchor,
                "A saved anchor must be retained even when its position is unusual");
        NativeSystem unowned = system(TroyArrivalScript.SYSTEM_ID, null);
        setSector(List.of(unowned), engine.hyper);
        TroyArrivalScript.repairWaypointTroyMapAnchor();
        check(unowned.getHyperspaceAnchor() == null && unowned.generationCalls == 0,
                "An unowned system ID must never authorize anchor generation");
        setSector(List.of(), engine.hyper);
        TroyArrivalScript.repairWaypointTroyMapAnchor();
        setSector(List.of(unowned), null);
        TroyArrivalScript.repairWaypointTroyMapAnchor();
        Global.setSector(null);
        TroyArrivalScript.repairWaypointTroyMapAnchor();
        NativeSystem legacy = system(TroyArrivalScript.SYSTEM_ID, LEGACY);
        setSector(List.of(legacy), engine.hyper);
        TroyArrivalScript.repairWaypointTroyMapAnchor();
        check(legacy.getHyperspaceAnchor() != null && legacy.generationCalls == 1,
                "The established legacy ownership marker must also allow narrow anchor repair");
    }

    public static void main(String[] args) throws Exception {
        Field singleton = Unsafe.class.getDeclaredField("theUnsafe");
        singleton.setAccessible(true);
        unsafe = (Unsafe) singleton.get(null);
        SectorAPI priorSector = Global.getSector();
        SettingsAPI priorSettings = Global.getSettings();
        Field engineSingleton = CampaignEngine.class.getDeclaredField("instance");
        engineSingleton.setAccessible(true);
        CampaignEngine priorEngine = (CampaignEngine) engineSingleton.get(null);
        Map<Field, Object> priorNativeSettings = new HashMap<>();
        try {
            Global.setSettings(mock(SettingsAPI.class, (name, values) -> {
                if (name.equals("getFloat")) return 1f;
                if (name.equals("getColor")) return Color.WHITE;
                return null;
            }));
            Class<?> settings = Class.forName("com.fs.starfarer.settings.StarfarerSettings");
            for (Field field : settings.getDeclaredFields()) {
                if (field.getType() == JSONObject.class && Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    priorNativeSettings.put(field, field.get(null));
                    field.set(null, new JSONObject().put("campaignRadarRadius", 3000f));
                }
            }
            NativeEngine engine = allocate(NativeEngine.class);
            engine.hyper = allocate(NativeHyperspace.class);
            engine.hyper.entities = new ArrayList<>();
            engine.player = allocate(NativePlayer.class);
            engine.testClock = new CampaignClock();
            engineSingleton.set(null, engine);
            checkNativeCrashAndRepair(engine);
            checkScope(engine);
            System.out.println("PASS: native Troy course-renderer crash reproduced and repaired; "
                    + "native anchor generation preserves saved routes, coordinates, flags and existing anchors.");
        } finally {
            Global.setSector(priorSector);
            Global.setSettings(priorSettings);
            engineSingleton.set(null, priorEngine);
            for (Map.Entry<Field, Object> saved : priorNativeSettings.entrySet()) {
                saved.getKey().set(null, saved.getValue());
            }
        }
    }
}
