import com.fs.graphics.util.Fader;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.campaign.CampaignEngine;
import com.fs.starfarer.campaign.CampaignState;
import com.fs.starfarer.campaign.CampaignUIPersistentData;
import com.fs.starfarer.ui.n;
import com.fs.starfarer.ui.newui.L;
import com.fs.starfarer.ui.newui.o0Oo;
import com.fs.starfarer.ui.newui.thissuper;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import sun.misc.Unsafe;

/**
 * External-only check against the installed native core dismissal/tab methods.
 * Native UI constructors require a renderer. Allocate only their minimal state
 * here; no game, save, rendering, or production reflection is involved.
 * Run with -Xverify:none for the installed engine's obfuscated member names.
 */
public final class FobIthacaStorageDismissalTimingRegression {
    private static final Unsafe UNSAFE;
    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (Exception failure) { throw new ExceptionInInitializerError(failure); }
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        return type.cast(UNSAFE.allocateInstance(type));
    }

    private static void field(Object object, String name, Object value) throws Exception {
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(object, value);
                return;
            } catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }

    private static void fieldsOfType(Object object, Class<?> fieldType, Object value)
            throws Exception {
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getType() == fieldType) {
                    field.setAccessible(true);
                    field.set(object, value);
                }
            }
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        Field instance = CampaignEngine.class.getDeclaredField("instance");
        instance.setAccessible(true);
        Object previousEngine = instance.get(null);
        try {
            CampaignEngine engine = allocate(CampaignEngine.class);
            field(engine, "uiData", allocate(CampaignUIPersistentData.class));
            instance.set(null, engine);

            n cargoButton = allocate(n.class);
            Fader highlight = new Fader(1f, 0.25f, 0.25f);
            highlight.forceIn();
            fieldsOfType(cargoButton, Fader.class, highlight);
            Map<Object, n> buttons = new LinkedHashMap<>();
            buttons.put(CoreUITabId.CARGO, cargoButton);
            thissuper strip = allocate(thissuper.class);
            fieldsOfType(strip, Map.class, buttons);

            L core = allocate(L.class);
            fieldsOfType(core, thissuper.class, strip);
            Fader fade = new Fader(1f, 0.25f, 0.25f);
            fade.forceIn();
            fieldsOfType(core, Fader.class, fade);
            // Values established by the native showCore/modal constructors.
            field(core, "String.return$String", true);
            field(core, "super.while", true);
            field(core, "do.while", new LinkedHashSet<Integer>());
            core.setDelegate(core);

            o0Oo dialog = allocate(o0Oo.class);
            fieldsOfType(dialog, L.class, core);
            CampaignState state = allocate(CampaignState.class);
            field(state, "encounterDialog", dialog);
            check(state.getCurrentCoreTab() == CoreUITabId.CARGO,
                    "Real native tab resolution must initially select CARGO");
            final int[] callbacks = { 0 };
            core.setListener(() -> {
                callbacks[0]++;
                check(fade.isFadingOut(), "Native dismissal must start fading before callback");
                check(state.getCurrentCoreTab() == CoreUITabId.CARGO,
                        "CARGO remains current inside the native dismissal callback");
            });

            core.dismiss(0);
            check(callbacks[0] == 1, "Actual native dismiss must deliver one callback");
            check(fade.isFadingOut() && state.getCurrentCoreTab() == CoreUITabId.CARGO,
                    "CARGO remains current after native dismiss returns during fade-out");
            fade.advance(0.1f);
            check(!fade.isFadedOut() && state.getCurrentCoreTab() == CoreUITabId.CARGO,
                    "CARGO remains current while a paused script can observe the fading UI");
            fade.advance(1f);
            check(fade.isFadedOut() && state.getCurrentCoreTab() == CoreUITabId.CARGO,
                    "Native tab getter does not inspect the retained core's faded state");
            System.out.println("PASS: installed native L.dismiss and CampaignState.getCurrentCoreTab"
                    + " retain CARGO inside the dismissal callback and throughout fade-out.");
        } finally {
            instance.set(null, previousEngine);
        }
    }
}
