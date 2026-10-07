package chiefnavigator.topography;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel.EventStageData;
import com.fs.starfarer.api.impl.campaign.intel.events.EventFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.ht.HyperspaceTopographyEventIntel;
import com.fs.starfarer.api.impl.campaign.intel.events.ht.HyperspaceTopographyEventIntel.Stage;
import chiefnavigator.quest.SinniBarEvent;
import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/** Headless geometry, saved-event continuity, and payout checks for the UI-only fix. */
public final class SinniTopographyLayoutRegression {
    interface Call { Object invoke(String name, Object[] args); }

    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[] {type}, (proxy, method, args) -> {
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == args[0];
                    Object result = call.invoke(method.getName(), args);
                    if (result != null) return result;
                    Class<?> returns = method.getReturnType();
                    if (returns == boolean.class) return false;
                    if (returns == int.class) return 0;
                    if (returns == float.class) return 0f;
                    if (returns == long.class) return 0L;
                    if (returns == double.class) return 0d;
                    return null;
                }));
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static Object hook(BaseEventIntel event, String name, Object stage) throws Exception {
        Method method = BaseEventIntel.class.getDeclaredMethod(name, Object.class);
        method.setAccessible(true);
        return method.invoke(event, stage);
    }

    static float line(BaseEventIntel event, Object stage) throws Exception {
        return ((Number) hook(event, "getStageDownLineLength", stage)).floatValue();
    }

    static float size(BaseEventIntel event, Object stage) throws Exception {
        return ((Number) hook(event, "getStageIconSize", stage)).floatValue();
    }

    static String label(BaseEventIntel event, Object stage) throws Exception {
        return (String) hook(event, "getStageLabel", stage);
    }

    static void rawProgress(BaseEventIntel event, int value) throws Exception {
        Field field = BaseEventIntel.class.getDeclaredField("progress");
        field.setAccessible(true);
        field.setInt(event, value);
    }

    static void notifyStage(SinniHyperspaceTopographyEventIntel event, Object id) throws Exception {
        Method method = SinniHyperspaceTopographyEventIntel.class.getDeclaredMethod(
                "notifyStageReached", EventStageData.class);
        method.setAccessible(true);
        method.invoke(event, event.getDataFor(id));
    }

    private static final class Fixture {
        final Map<String, Object> saved = new HashMap<>();
        int dataItems;

        Fixture() {
            Global.setSettings(mock(SettingsAPI.class, (name, args) ->
                    name.equals("getColor") ? Color.CYAN : null));
            MemoryAPI memory = mock(MemoryAPI.class, (name, args) -> {
                if (name.equals("getBoolean")) return Boolean.TRUE.equals(saved.get(args[0]));
                if (name.equals("get")) return saved.get(args[0]);
                if (name.equals("contains")) return saved.containsKey(args[0]);
                if (name.equals("set")) saved.put((String) args[0], args[1]);
                if (name.equals("unset")) saved.remove(args[0]);
                return null;
            });
            CargoAPI cargo = mock(CargoAPI.class, (name, args) -> {
                if (name.equals("addSpecial")) {
                    check(Items.TOPOGRAPHIC_DATA.equals(((SpecialItemData) args[0]).getId()),
                            "Only vanilla topographic data may be awarded");
                    dataItems += ((Number) args[1]).intValue();
                }
                return null;
            });
            CampaignFleetAPI player = mock(CampaignFleetAPI.class, (name, args) ->
                    name.equals("getCargo") ? cargo : null);
            IntelManagerAPI intel = mock(IntelManagerAPI.class, (name, args) -> null);
            ListenerManagerAPI listeners = mock(ListenerManagerAPI.class, (name, args) -> null);
            Global.setSector(mock(SectorAPI.class, (name, args) -> {
                if (name.equals("getMemoryWithoutUpdate")) return memory;
                if (name.equals("getPlayerFleet")) return player;
                if (name.equals("getIntelManager")) return intel;
                if (name.equals("getListenerManager")) return listeners;
                return null;
            }));
        }
    }

    private static void checkThresholds(SinniHyperspaceTopographyEventIntel event) {
        Object[] custom = {
                SinniHyperspaceTopographyEventIntel.SinniStage.SLIPSURGE_MASTERY,
                SinniHyperspaceTopographyEventIntel.SinniStage.DRIVE_MITES,
                SinniHyperspaceTopographyEventIntel.SinniStage.PURSUIT_BURN,
                SinniHyperspaceTopographyEventIntel.SinniStage.EXTENDED_TOPOGRAPHIC_DATA,
                SinniHyperspaceTopographyEventIntel.SinniStage.ECHO_MAPPING,
                SinniHyperspaceTopographyEventIntel.SinniStage.REPEATABLE_TOPOGRAPHIC_DATA
        };
        int[] thresholds = {1050, 1200, 1350, 1550, 1750, 1950};
        for (int i = 0; i < custom.length; i++) {
            check(event.getRequiredProgress(custom[i]) == thresholds[i],
                    "Presentation must not change the " + custom[i] + " threshold");
        }
        check(event.getMaxProgress() == 1950, "The extended cap must remain 1950");
        if (event.getDataFor(Stage.TOPOGRAPHIC_DATA) != null) {
            check(event.getRequiredProgress(Stage.TOPOGRAPHIC_DATA) == 1000,
                    "The pre-recruitment vanilla payout must still require 1000 points");
        }
    }

    public static void main(String[] args) throws Exception {
        SettingsAPI priorSettings = Global.getSettings();
        SectorAPI priorSector = Global.getSector();
        try {
            Fixture fixture = new Fixture();
            SinniHyperspaceTopographyEventIntel event =
                    new SinniHyperspaceTopographyEventIntel(null, false);
            rawProgress(event, 717);
            event.getDataFor(Stage.GENERATE_SLIPSURGE).wasEverReached = true;
            checkThresholds(event);

            Object first = SinniHyperspaceTopographyEventIntel.SinniStage.SLIPSURGE_MASTERY;
            float vanillaBottom = -line(event, Stage.TOPOGRAPHIC_DATA);
            float firstTop = -line(event, first) - size(event, first);
            check(vanillaBottom <= firstTop - 8f,
                    "The elevated vanilla data icon must leave at least eight pixels of vertical separation");
            check(size(event, Stage.TOPOGRAPHIC_DATA) == 32f && size(event, first) == 48f,
                    "The fix must preserve readable native icon sizes");
            float vanillaLeft = event.getBarWidth() * 1000f / event.getMaxProgress()
                    - size(event, Stage.TOPOGRAPHIC_DATA) / 2f - 1f;
            check(vanillaLeft > 300f,
                    "The elevated icon must remain to the right of the native title in this layout");
            check(label(event, Stage.TOPOGRAPHIC_DATA) == null
                            && "Locked".equals(label(event, first)),
                    "The coincident vanilla threshold label must not overlap the custom Locked label");
            check(line(event, Stage.GENERATE_SLIPSURGE) == 40f && line(event, first) == 40f,
                    "Other large markers must retain their native geometry");

            EventStageData savedPayout = event.getDataFor(Stage.TOPOGRAPHIC_DATA);
            EventFactor savedFactor = mock(EventFactor.class, (name, values) -> null);
            event.getFactors().add(savedFactor);
            SinniHyperspaceTopographyEventIntel same =
                    SinniHyperspaceTopographyEventIntel.ensureInstalled();
            check(same == event && event.getProgress() == 717
                            && event.getDataFor(Stage.TOPOGRAPHIC_DATA) == savedPayout
                            && event.getDataFor(Stage.GENERATE_SLIPSURGE).wasEverReached
                            && event.getFactors().contains(savedFactor),
                    "Loading an already-extended save must not rebuild stages or discard saved progress/factors");
            check(line(event, Stage.TOPOGRAPHIC_DATA) == 96f,
                    "An existing extended save must receive the new layout without a migration");

            rawProgress(event, 1000);
            notifyStage(event, Stage.TOPOGRAPHIC_DATA);
            check(fixture.dataItems == 1 && event.getProgress() >= 700 && event.getProgress() <= 750,
                    "The locked vanilla payout and its reset range must remain unchanged");
            rawProgress(event, 717);
            notifyStage(event, SinniHyperspaceTopographyEventIntel.SinniStage.REPEATABLE_TOPOGRAPHIC_DATA);
            check(fixture.dataItems == 1 && event.getProgress() == 717,
                    "The extension must not award data before Sinni recruitment");

            fixture.saved.put(SinniBarEvent.STARTED, true);
            check(line(event, Stage.TOPOGRAPHIC_DATA) == 48f,
                    "Unlocking Sinni must restore native geometry even before stage reconciliation");
            check("1000".equals(label(event, Stage.TOPOGRAPHIC_DATA)),
                    "The spacing override must not suppress unlocked vanilla labels");
            SinniHyperspaceTopographyEventIntel.ensureInstalled();
            check(event.getDataFor(Stage.TOPOGRAPHIC_DATA) == null,
                    "Recruitment must continue replacing the vanilla payout with the existing custom tree");
            checkThresholds(event);
            rawProgress(event, 1950);
            notifyStage(event, SinniHyperspaceTopographyEventIntel.SinniStage.REPEATABLE_TOPOGRAPHIC_DATA);
            check(fixture.dataItems == 2 && event.getProgress() == 1750,
                    "The unlocked payout must still award one data item and reset to 1750");
        } finally {
            Global.setSector(priorSector);
            Global.setSettings(priorSettings);
        }
        System.out.println("Sinni topography layout regression checks passed.");
    }
}
