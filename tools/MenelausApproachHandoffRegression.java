package chiefnavigator.quest;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;
import com.fs.starfarer.campaign.rules.Memory;
import java.awt.Color;
import java.lang.reflect.*;
import java.util.*;

/**
 * Runs the real approach shell, then a separate central-module interaction
 * through the campaign router, native rules dialog, and station shell.
 * Campaign/UI repositories reuse the parsed-rules fixture; no
 * production dispatcher or dialog plugin is substituted. Run beside
 * MenelausFlowRegression in its own JVM with -Xverify:none and
 * -Dlog4j.defaultInitOverride=true. Put a candidate runtime jar/class directory
 * before the canonical release jar to compare scene-separation behavior.
 */
public final class MenelausApproachHandoffRegression {
    private static final Object UNHANDLED = new Object();
    private static final String INTRO = "$chief_navigator_menelaus_intro_shown_v2";
    private static final String CARGO = "$chief_navigator_ithaca_storage_cargo";
    private static int completed;

    interface Intercept { Object invoke(String name, Object[] args) throws Exception; }

    private static <T> T wrap(Class<T> type, T original, Intercept intercept) {
        InvocationHandler delegate = Proxy.getInvocationHandler(original);
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> {
                    Object result = intercept.invoke(method.getName(), args);
                    return result == UNHANDLED ? delegate.invoke(original, method, args) : result;
                }));
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static String constant(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true);
        return (String) field.get(null);
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static final class Conversation {
        final MenelausFlowRegression.Fixture f = new MenelausFlowRegression.Fixture();
        final Memory originMemory = new Memory(), factionMemory = new Memory(), systemMemory = new Memory();
        final FactionAPI faction = MenelausFlowRegression.mock(FactionAPI.class, (name, args) -> {
            if (name.equals("getId")) return TaskForceSpartanFaction.ID;
            if (name.equals("getMemory") || name.equals("getMemoryWithoutUpdate")) return factionMemory;
            return null;
        });
        final CargoAPI stored = MenelausFlowRegression.mock(CargoAPI.class, (name, args) -> {
            if (name.equals("getMothballedShips")) return MenelausFlowRegression.mock(FleetDataAPI.class,
                    (method, values) -> method.equals("getMembersListCopy") ? List.of() : null);
            return null;
        });
        SectorEntityToken target, coreTarget, carrierPrimary;
        CoreInteractionListener storageDismissal;
        StoragePlugin openedStorage;
        boolean canonicalPresent = true;
        int coreOpens, closedDialogs;

        Conversation(String origin) throws Exception {
            f.install();
            factionMemory.set("$id", TaskForceSpartanFaction.ID);
            systemMemory.set(constant(OdysseyExpanseSystem.class, "AUTHORED_SYSTEM_MARKER"), true);
            StarSystemAPI system = MenelausFlowRegression.mock(StarSystemAPI.class, (name, args) -> {
                if (name.equals("getOptionalUniqueId")) return OdysseyExpanseSystem.ASHEN_VERGE_ID;
                if (name.equals("getMemoryWithoutUpdate")) return systemMemory;
                if (name.equals("getEntityById")) return canonicalPresent && args[0].equals(f.station.getId()) ? f.station : null;
                if (name.equals("getAllEntities") || name.equals("getFleets")) return List.of();
                return null;
            });
            set(f, "station", wrap(SectorEntityToken.class, f.station, (name, args) -> {
                if (name.equals("getMemory")) return f.entity;
                if (name.equals("getFaction")) return faction;
                if (name.equals("getContainingLocation") || name.equals("getStarSystem")) return system;
                if (name.equals("getCargo")) return stored;
                return UNHANDLED;
            }));
            set(f, "optionPanel", wrap(OptionPanelAPI.class, f.optionPanel, (name, args) ->
                    name.equals("hasOptions") ? !f.options.isEmpty() : UNHANDLED));
            set(f, "visual", wrap(VisualPanelAPI.class, f.visual, (name, args) -> {
                if (!name.equals("showCore")) return UNHANDLED;
                coreTarget = (SectorEntityToken) args[1];
                storageDismissal = (CoreInteractionListener) args[3];
                check(coreTarget == f.station && carrierPrimary == f.station,
                        "Storage UI and its carrier must retain canonical Ithaca");
                check(openedStorage.getCargo() == stored,
                        "Storage must bind the canonical station's exact cargo");
                coreOpens++;
                return null;
            }));
            set(f, "dialog", wrap(InteractionDialogAPI.class, f.dialog, (name, args) -> {
                if (name.equals("getInteractionTarget")) return target;
                if (name.equals("setInteractionTarget")) { target = (SectorEntityToken) args[0]; return null; }
                return UNHANDLED;
            }));
            SectorAPI sector = Global.getSector();
            CampaignFleetAPI playerFleet = wrap(CampaignFleetAPI.class, sector.getPlayerFleet(), (name, args) -> {
                if (name.equals("getContainingLocation")) return system;
                return UNHANDLED;
            });
            CharacterDataAPI character = MenelausFlowRegression.mock(CharacterDataAPI.class,
                    (name, args) -> name.equals("getMemory") ? f.global : null);
            Global.setSector(wrap(SectorAPI.class, sector, (name, args) -> {
                if (name.equals("getMemory")) return f.global;
                if (name.equals("getCharacterData")) return character;
                if (name.equals("getPlayerFleet")) return playerFleet;
                if (name.equals("getStarSystems") || name.equals("getAllLocations")) return List.of(system);
                return UNHANDLED;
            }));
            FactoryAPI factory = Global.getFactory();
            Global.setFactory(wrap(FactoryAPI.class, factory, (name, args) -> {
                if (!name.equals("createMarket") || !args[0].equals("chief_navigator_fob_ithaca_storage_view"))
                    return UNHANDLED;
                StoragePlugin storage = new StoragePlugin(); openedStorage = storage;
                final MarketAPI[] holder = {null};
                SubmarketAPI submarket = MenelausFlowRegression.mock(SubmarketAPI.class, (method, values) -> {
                    if (method.equals("getMarket")) return holder[0];
                    if (method.equals("getPlugin")) return storage;
                    if (method.equals("getFaction")) return faction;
                    return null;
                });
                MarketAPI market = MenelausFlowRegression.mock(MarketAPI.class, (method, values) -> {
                    if (method.equals("getSubmarket")) return submarket;
                    if (method.equals("getFaction")) return faction;
                    if (method.equals("addSubmarket")) storage.init(submarket);
                    if (method.equals("setPrimaryEntity")) carrierPrimary = (SectorEntityToken) values[0];
                    if (method.equals("getPrimaryEntity")) return carrierPrimary;
                    return null;
                });
                holder[0] = market;
                return market;
            }));
            if (origin.equals("player")) target = playerFleet;
            else if (origin.equals("station")) target = f.station;
            else target = MenelausFlowRegression.mock(CampaignFleetAPI.class, (name, args) -> {
                if (name.equals("getId")) return origin + "_intercepted_fleet";
                if (name.equals("getMemoryWithoutUpdate") || name.equals("getMemory")) return originMemory;
                if (name.equals("getFaction")) return faction;
                return null;
            });
        }

        void select(String id) {
            check(f.options.containsKey(id), "Expected visible choice " + id + ", got " + f.options);
            f.plugin.optionSelected(f.options.get(id), id);
            check(f.dismissals == closedDialogs, "Active scene must not dismiss the conversation after " + id);
            check(!f.options.isEmpty() || coreOpens > 0, "Live handoff must keep usable options after " + id);
            check(!f.options.containsKey(RuleBasedInteractionDialogPluginImpl.FAILSAFE_LEAVE),
                    "Native dialogue must never fall back to its emergency Leave option");
        }

        void approachFinalPage() {
            f.plugin = new FobIthacaApproachInteraction();
            f.plugin.init(f.dialog);
            check(!f.options.isEmpty(), "Actual approach opening must render");
            // Start at the final authored approach page; the preceding prose
            // and portrait chain is covered by FobIthacaApproachRegression.
            f.plugin.optionSelected(null, "ring_breach");
            while (!f.options.containsKey("complete")) select(f.options.keySet().iterator().next());
        }

        void completeApproach() {
            SectorEntityToken previousTarget = target;
            InteractionDialogPlugin previousPlugin = f.plugin;
            f.plugin.optionSelected(f.options.get("complete"), "complete");
            check(f.dismissals == closedDialogs + 1,
                    "Completing the approach must close that scene");
            closedDialogs++;
            check(target == previousTarget && f.plugin == previousPlugin,
                    "Approach completion must neither retarget nor replace its dialog plugin");
            check(f.global.getBoolean(FobIthacaApproachScript.COMPLETE)
                            && !f.global.getBoolean(INTRO) && !MenelausTrial.isAccepted(),
                    "Approach completion must persist only the approach, leaving Menelaus pending");
            check(!f.options.containsKey("chief_navigator_menelaus_first_shuttle_continue"),
                    "Approach completion must not render the first-contact scene");
        }

        void openStationIntroduction() throws Exception {
            check(FobIthacaApproachScript.shouldOpenStationIntroduction(f.station),
                    "A completed approach must leave the central-module introduction pending");
            InteractionDialogAPI previousDialog = f.dialog;
            // Recreate the native UI boundary while keeping the campaign state.
            // A new proxy identity ensures this is a fresh interaction, rather
            // than an in-place continuation of the approach shell.
            set(f, "dialog", wrap(InteractionDialogAPI.class, f.dialog,
                    (name, args) -> UNHANDLED));
            check(f.dialog != previousDialog, "Contacting the module must open a separate dialog");
            target = f.station;
            f.options.clear();
            f.plugin = new TreadmillCampaignPlugin().pickInteractionDialogPlugin(target).plugin;
            check(f.plugin instanceof RuleBasedInteractionDialogPluginImpl,
                    "Contacting the central module must select the native first-contact dialog");
            f.plugin.init(f.dialog);
            check(f.options.containsKey("chief_navigator_menelaus_first_shuttle_continue"),
                    "Central-module contact must open the full first-contact scene");
        }
    }

    private static void verifyOrigin(String origin) throws Exception {
        Conversation c = new Conversation(origin); c.approachFinalPage();
        SectorEntityToken previous = c.target;
        c.completeApproach();
        if (previous != c.f.station) {
            check(!FobIthacaApproachScript.shouldOpenStationIntroduction(previous),
                    "Menelaus must not open on the borrowed approach target");
        }
        c.openStationIntroduction();
        c.select("chief_navigator_menelaus_first_shuttle_continue");
        check(c.target == c.f.station,
                "First contact must keep canonical Ithaca as its target (origin=" + origin + ")");
        int choices = 1;
        while (!(c.f.plugin instanceof FobIthacaStationInteraction)) {
            check(++choices < 100, "First contact must reach its station handoff");
            c.select(c.f.options.keySet().iterator().next());
            check(c.target == c.f.station, "First-contact continuations must preserve canonical Ithaca");
        }
        check(c.f.global.getBoolean(INTRO) && !MenelausTrial.isAccepted(),
                "Station handoff completes introduction without accepting a Labor");
        check(!FobIthacaApproachScript.shouldOpenStationIntroduction(c.f.station)
                        && new TreadmillCampaignPlugin().pickInteractionDialogPlugin(c.f.station).plugin
                                instanceof FobIthacaStationInteraction,
                "Later module contacts must select station services without replaying Menelaus");
        check(c.f.options.containsKey("chief_navigator_fob_ithaca_storage"), "Station must expose storage");
        c.select("chief_navigator_fob_ithaca_storage");
        check(c.coreOpens == 1 && c.coreTarget == c.f.station
                        && c.f.entity.get(CARGO) == c.stored && !c.originMemory.contains(CARGO),
                "Storage following first contact must belong exclusively to Ithaca");
        check(previous == c.f.station || previous.getMarket() == null,
                "Borrowed approach target must not receive a storage carrier");
        c.storageDismissal.coreUIDismissed();
        check(c.f.attached == null && c.target == c.f.station && !c.f.options.isEmpty(),
                "Closing storage must restore the canonical station menu");
        completed++;
        System.out.println("PASS approach close -> module contact -> Menelaus -> station/storage: " + origin + " (" + choices + " choices)");
    }

    private static void verifyMissingCanonicalStation() throws Exception {
        Conversation c = new Conversation("player"); c.approachFinalPage(); c.canonicalPresent = false;
        c.completeApproach();
        c.canonicalPresent = true;
        c.openStationIntroduction();
        check(!c.f.global.getBoolean(INTRO) && !MenelausTrial.isAccepted(),
                "A temporarily unavailable station must leave the later introduction uncommitted");
    }

    private static void verifyInterruptedIntroduction() throws Exception {
        Conversation c = new Conversation("player"); c.approachFinalPage(); c.completeApproach();
        c.openStationIntroduction();
        c.select("chief_navigator_menelaus_first_shuttle_continue");
        check(!c.f.global.getBoolean(INTRO) && !MenelausTrial.isAccepted(),
                "Interrupting first contact before its final choice must not commit Menelaus or a Labor");
        c.f.dialog.dismiss();
        c.closedDialogs++;
        c.openStationIntroduction();
        check(c.f.options.containsKey("chief_navigator_menelaus_first_shuttle_continue"),
                "A later module contact must restart an interrupted introduction from its opening");
    }

    public static void main(String[] args) throws Exception {
        SectorAPI sector = Global.getSector(); FactoryAPI factory = Global.getFactory();
        SettingsAPI settings = Global.getSettings();
        try {
            Global.setSettings(MenelausFlowRegression.mock(SettingsAPI.class, (name, values) ->
                    name.equals("getColor") ? Color.WHITE : null));
            MenelausFlowRegression.initializeExpressions(); MenelausFlowRegression.readRules();
            for (String origin : List.of("player", "spartan", "foreign", "station")) verifyOrigin(origin);
            verifyMissingCanonicalStation();
            verifyInterruptedIntroduction();
            check(completed == 4, "Every approach origin must complete");
            System.out.println("PASS: separate approach/module scenes, canonical first-contact routing, storage ownership, and pending/interrupted introduction.");
        } finally {
            Global.setSector(sector); Global.setFactory(factory); Global.setSettings(settings);
        }
    }
}
