package chiefnavigator.quest;

import chiefnavigator.topography.SinniHyperspaceTopographyEventIntel;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.characters.AbilityPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel.EventStageData;
import com.fs.starfarer.api.impl.campaign.intel.events.ht.HyperspaceTopographyEventIntel;
import com.fs.starfarer.api.impl.campaign.intel.events.ht.HyperspaceTopographyEventIntel.Stage;
import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** Headless bar eligibility checks against real native topography stages and payout. */
public final class SinniBarEligibilityRegression {
    private static final String DECLINED = "$chief_navigator_sinni_declined_recently";

    interface Call { Object invoke(String name, Object[] args); }

    private static <T> T mock(Class<T> type, Call call) {
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
                    if (returns == byte.class) return (byte) 0;
                    if (returns == short.class) return (short) 0;
                    if (returns == char.class) return (char) 0;
                    return null;
                }));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    /** Restore native serialized state without awarding abilities or opening UI. */
    private static void savedProgress(BaseEventIntel event, int value) throws Exception {
        Field field = BaseEventIntel.class.getDeclaredField("progress");
        field.setAccessible(true);
        field.setInt(event, value);
    }

    private static final class Fixture {
        final Map<String, Object> saved = new HashMap<String, Object>();
        final AbilityPlugin ability = mock(AbilityPlugin.class, (name, args) -> null);
        boolean preGrantedSlipsurge;
        int dataItems;
        int intelAdded;
        int intelRemoved;

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
                            "The native payout must award only topographic data");
                    dataItems += ((Number) args[1]).intValue();
                }
                return null;
            });
            CampaignFleetAPI player = mock(CampaignFleetAPI.class, (name, args) -> {
                if (name.equals("getCargo")) return cargo;
                if (name.equals("getAbility")) {
                    return preGrantedSlipsurge && Abilities.GENERATE_SLIPSURGE.equals(args[0])
                            ? ability : null;
                }
                if (name.equals("hasAbility")) {
                    return preGrantedSlipsurge && Abilities.GENERATE_SLIPSURGE.equals(args[0]);
                }
                return null;
            });
            IntelManagerAPI intel = mock(IntelManagerAPI.class, (name, args) -> {
                if (name.equals("addIntel")) intelAdded++;
                if (name.equals("removeIntel")) intelRemoved++;
                return null;
            });
            ListenerManagerAPI listeners = mock(ListenerManagerAPI.class, (name, args) -> null);
            Global.setSector(mock(SectorAPI.class, (name, args) -> {
                if (name.equals("getMemoryWithoutUpdate")) return memory;
                if (name.equals("getPlayerFleet")) return player;
                if (name.equals("getIntelManager")) return intel;
                if (name.equals("getListenerManager")) return listeners;
                return null;
            }));
        }

        HyperspaceTopographyEventIntel vanilla(int progress) throws Exception {
            HyperspaceTopographyEventIntel event =
                    new HyperspaceTopographyEventIntel(null, false);
            savedProgress(event, progress);
            return event;
        }

        void completed() {
            saved.put(SinniHyperspaceTopographyEventIntel.SINNI_REQUIREMENT_MET, true);
        }
    }

    /** Represents another mod retaining ownership of the native singleton. */
    private static final class ForeignTopography extends HyperspaceTopographyEventIntel {
        final Object privateState = new Object();

        ForeignTopography() {
            super(null, false);
            setMaxProgress(2600);
        }
    }

    public static void main(String[] args) throws Exception {
        SettingsAPI priorSettings = Global.getSettings();
        SectorAPI priorSector = Global.getSector();
        try {
            Global.setSector(null);
            check(!SinniBarEvent.isEligible(), "No sector must suppress the bar event");

            Fixture fixture = new Fixture();
            check(!SinniBarEvent.isEligible(), "No topography event must suppress Sinni");
            check(HyperspaceTopographyEventIntel.get() == null,
                    "Eligibility must not create a topography event");

            fixture = new Fixture();
            fixture.vanilla(0);
            check(!SinniBarEvent.isEligible(), "A fresh zero-progress campaign must suppress Sinni");

            fixture = new Fixture();
            fixture.vanilla(HyperspaceTopographyEventIntel.PROGRESS_5 - 1);
            check(!SinniBarEvent.isEligible(), "The point before the last vanilla ability must fail");

            fixture = new Fixture();
            HyperspaceTopographyEventIntel event =
                    fixture.vanilla(HyperspaceTopographyEventIntel.PROGRESS_5);
            check(event.isStageActive(Stage.GENERATE_SLIPSURGE),
                    "700 points must activate the real final permanent native stage");
            check(SinniBarEvent.isEligible(), "The final vanilla ability must permit Sinni");
            check(Boolean.TRUE.equals(fixture.saved.get(
                            SinniHyperspaceTopographyEventIntel.SINNI_REQUIREMENT_MET)),
                    "Earned completion must be preserved for future loads");

            fixture = new Fixture();
            SinniHyperspaceTopographyEventIntel extended =
                    new SinniHyperspaceTopographyEventIntel(null, false);
            check(extended.getMaxProgress() == 1950,
                    "The real extension must keep its post-recruitment cap");
            check(!SinniBarEvent.isEligible(), "Installing the extension at zero must not unlock Sinni");
            savedProgress(extended, HyperspaceTopographyEventIntel.PROGRESS_5);
            check(SinniBarEvent.isEligible(),
                    "The extension must qualify at the vanilla threshold without reaching 1950");

            fixture = new Fixture();
            event = fixture.vanilla(HyperspaceTopographyEventIntel.PROGRESS_5);
            event.getDataFor(Stage.GENERATE_SLIPSURGE).wasEverReached = true;
            for (EventStageData stage : event.getStages()) {
                stage.sendIntelUpdateOnReaching = false;
            }
            event.setRandom(new Random(73019L));
            event.setProgress(HyperspaceTopographyEventIntel.PROGRESS_MAX);
            check(fixture.dataItems == 1 && event.getProgress() >= 700
                            && event.getProgress() <= 750,
                    "Real native 1000-point payout must award data and reset to 700-750");
            check(event.getDataFor(Stage.TOPOGRAPHIC_DATA).wasEverReached,
                    "The actual native payout must record its reached history");
            check(SinniBarEvent.isEligible(), "Native payout/reset must retain Sinni eligibility");

            fixture = new Fixture();
            fixture.completed();
            check(SinniBarEvent.isEligible(),
                    "A durable saved completion flag must survive absent event intel");

            fixture = new Fixture();
            event = fixture.vanilla(0);
            event.getDataFor(Stage.GENERATE_SLIPSURGE).wasEverReached = true;
            check(SinniBarEvent.isEligible(),
                    "Native saved stage history must survive restored low progress");

            fixture = new Fixture();
            fixture.completed();
            fixture.saved.put(SinniBarEvent.STARTED, true);
            check(!SinniBarEvent.isEligible(), "Already recruited Sinni must not appear again");

            fixture = new Fixture();
            fixture.completed();
            fixture.saved.put(DECLINED, true);
            check(!SinniBarEvent.isEligible(), "A temporary decline must suppress an earned encounter");
            fixture.saved.remove(DECLINED);
            check(SinniBarEvent.isEligible(), "An expired decline must restore earned eligibility");

            fixture = new Fixture();
            fixture.vanilla(0);
            fixture.preGrantedSlipsurge = true;
            check(Global.getSector().getPlayerFleet().getAbility(
                            Abilities.GENERATE_SLIPSURGE) != null,
                    "The zero-progress fixture must really own the pre-granted ability");
            check(!SinniBarEvent.isEligible(),
                    "An ability granted without topography completion must not bypass the gate");
            check(!Boolean.TRUE.equals(fixture.saved.get(
                            SinniHyperspaceTopographyEventIntel.SINNI_REQUIREMENT_MET)),
                    "A pre-granted ability must not become durable completion");

            fixture = new Fixture();
            ForeignTopography foreign = new ForeignTopography();
            Object foreignState = foreign.privateState;
            int added = fixture.intelAdded;
            check(!SinniBarEvent.isEligible(), "A foreign event at zero must obey the same gate");
            savedProgress(foreign, HyperspaceTopographyEventIntel.PROGRESS_5);
            check(SinniBarEvent.isEligible(), "A foreign event may prove native completion");
            check(HyperspaceTopographyEventIntel.get() == foreign
                            && foreign.privateState == foreignState
                            && foreign.getMaxProgress() == 2600
                            && fixture.intelAdded == added && fixture.intelRemoved == 0,
                    "Eligibility must preserve another mod's event instance and state");
        } finally {
            Global.setSector(priorSector);
            Global.setSettings(priorSettings);
        }
        System.out.println("PASS: Sinni waits for earned vanilla Hyperspace Topography completion; "
                + "native payout, saved history, decline, and foreign-event continuity remain intact.");
    }
}