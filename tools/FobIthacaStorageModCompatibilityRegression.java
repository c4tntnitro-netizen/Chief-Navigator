package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.events.CampaignEventManagerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.campaign.CampaignEngine;
import com.fs.starfarer.campaign.ModAndPluginData;
import com.fs.starfarer.campaign.econ.Market;
import com.fs.starfarer.campaign.rules.Memory;
import com.fs.starfarer.campaign.ui.marketinfo.CommodityPanel;
import exerelin.plugins.ExerelinCampaignPlugin;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.BiFunction;
import kaysaar.bmo.buildingmenu.upgradequeue.UpdateQueueMainManager;
import org.lwjgl.util.vector.Vector2f;
import org.json.JSONArray;
import org.json.JSONObject;
import org.magiclib.util.MagicSettings;
import sun.misc.Unsafe;

/**
 * Optional native/mod integration regression; requires the local BMO and Nexerelin jars.
 * Uses their compiled callbacks, native Market.getMemory, and native CampaignEventTarget.
 * The UI tree is a fixture; the separate native-dismiss probe verifies the retained CARGO tab.
 * Does not start the renderer or read/write a save.
 */
public final class FobIthacaStorageModCompatibilityRegression {
    private static final Unsafe UNSAFE;
    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (Exception failure) { throw new ExceptionInInitializerError(failure); }
    }

    /** Reflection-visible methods traversed by the unmodified BMO UI callback. */
    public interface UiTree {
        List<UIComponentAPI> getChildrenCopy();
        UIPanelAPI getCurrentTab();
        CoreUIAPI getCoreUI();
        Object getMarketInfo();
        void showOverview();
    }

    private static <T> T allocate(Class<T> type) throws InstantiationException {
        return type.cast(UNSAFE.allocateInstance(type));
    }

    private static void field(Object target, String name, Object value) throws Exception {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }

    private static Object mock(Class<?>[] types, BiFunction<String, Object[], Object> call) {
        return Proxy.newProxyInstance(FobIthacaStorageModCompatibilityRegression.class.getClassLoader(),
                types, (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("hashCode")) return System.identityHashCode(proxy);
                    if (name.equals("equals")) return proxy == args[0];
                    Object result = call.apply(name, args);
                    if (result != null) return result;
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    if (method.getReturnType() == float.class) return 0f;
                    return null;
                });
    }

    private static UIPanelAPI panel(List<UIComponentAPI> children) {
        return (UIPanelAPI) mock(new Class<?>[] { UIPanelAPI.class, UiTree.class },
                (name, args) -> name.equals("getChildrenCopy") ? children : null);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        CampaignEngine engine = allocate(CampaignEngine.class);
        Field instance = CampaignEngine.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, engine);
        ModAndPluginData plugins = allocate(ModAndPluginData.class);
        field(engine, "modAndPluginData", plugins);
        field(plugins, "plugins", List.of(new ExerelinCampaignPlugin()));

        MarketAPI[] attached = { null };
        SectorEntityToken station = (SectorEntityToken) mock(
                new Class<?>[] { SectorEntityToken.class }, (name, values) -> {
                    if (name.equals("getLocationInHyperspace")) return new Vector2f();
                    if (name.equals("getMarket")) return attached[0];
                    if (name.equals("setMarket")) attached[0] = (MarketAPI) values[0];
                    return null;
                });
        Market carrier = allocate(Market.class);
        field(carrier, "connectedEntities", new LinkedHashSet<SectorEntityToken>());
        field(carrier, "location", new Vector2f());
        carrier.setId("chief_navigator_fob_ithaca_storage_view");
        carrier.setMemory(new Memory());
        field(carrier, "playerOwned", true);
        carrier.setPrimaryEntity(station);
        attached[0] = carrier;

        CommodityPanel commodities = allocate(CommodityPanel.class);
        field(commodities, "market", carrier);
        UIPanelAPI overview = panel(List.of(commodities));
        UIPanelAPI overviewContainer = panel(List.of(overview));
        UIPanelAPI marketContainer = panel(List.of(overviewContainer));
        UIPanelAPI tab = panel(List.of(marketContainer));
        CoreUIAPI core = (CoreUIAPI) mock(
                new Class<?>[] { CoreUIAPI.class, UIPanelAPI.class, UiTree.class },
                (name, values) -> name.equals("getCurrentTab") ? tab : null);
        InteractionDialogAPI dialog = (InteractionDialogAPI) mock(
                new Class<?>[] { InteractionDialogAPI.class, UiTree.class },
                (name, values) -> name.equals("getCoreUI") ? core : null);
        CampaignUIAPI ui = (CampaignUIAPI) mock(new Class<?>[] { CampaignUIAPI.class },
                (name, values) -> name.equals("getCurrentCoreTab") ? CoreUITabId.CARGO
                        : name.equals("getCurrentInteractionDialog") ? dialog : null);
        CampaignEventManagerAPI events = (CampaignEventManagerAPI) mock(
                new Class<?>[] { CampaignEventManagerAPI.class }, (name, values) -> null);
        SectorAPI sector = (SectorAPI) mock(new Class<?>[] { SectorAPI.class },
                (name, values) -> name.equals("getCampaignUI") ? ui
                        : name.equals("getEventManager") ? events
                        : name.equals("getAllFactions") ? List.of() : null);
        Global.setSector(sector);
        // Empty unrelated covert-action configuration is sufficient to initialize Nex's class.
        JSONObject covertConfig = new JSONObject("{actions:{},industrySuccessMods:{},industryDetectionMods:{}}");
        Global.setSettings((SettingsAPI) mock(new Class<?>[] { SettingsAPI.class },
                (name, values) -> name.equals("getMergedJSONForMod") ? covertConfig
                        : name.equals("getMergedSpreadsheetDataForMod") ? new JSONArray() : null));
        Field magicSettings = MagicSettings.class.getDeclaredField("modSettings");
        magicSettings.setAccessible(true);
        magicSettings.set(null, new JSONObject("{nexerelin:{agent_steal_ship_mults:{}}}"));

        UpdateQueueMainManager bmo = new UpdateQueueMainManager();
        require(bmo.runWhilePaused(), "BMO must run during the paused cargo UI fade");
        require(UpdateQueueMainManager.getOverview() == overview,
                "Actual BMO reflection must locate the retained native CommodityPanel");
        bmo.advance(0.016f); // Native market and actual Nex callback pass while storage is open.

        station.setMarket(null);
        carrier.setPrimaryEntity(null); // Original release cleanup, after the close callback.
        try {
            bmo.advance(0.016f);
            throw new AssertionError("Original null-primary carrier must reproduce the close crash");
        } catch (NullPointerException expected) {
            String stack = java.util.Arrays.toString(expected.getStackTrace());
            require(stack.contains("CampaignEventTarget.<init>")
                            && stack.contains("CovertOpsManager.getAlertLevel")
                            && stack.contains("ExerelinCampaignPlugin.updateMarketFacts")
                            && stack.contains("Market.getMemory")
                            && stack.contains("UpdateQueueMainManager.advance"),
                    "Expected the actual BMO -> native market -> Nex -> native target chain: " + stack);
            System.out.println("PASS: original cleanup reproduces the compiled BMO/Nex cargo-close NPE.");
            expected.printStackTrace(System.out);
        }

        carrier.setPrimaryEntity(station);
        carrier.getConnectedEntities().remove(null);
        attached[0] = carrier;
        Method release = FobIthacaStorageDialog.class.getDeclaredMethod("release",
                SectorEntityToken.class, MarketAPI.class, MarketAPI.class);
        release.setAccessible(true);
        release.invoke(null, station, carrier, null);
        require(station.getMarket() == null, "Fixed release must restore marketless Ithaca");
        require(carrier.getPrimaryEntity() == station,
                "Fixed release must preserve the market still retained by the closing cargo panel");
        bmo.advance(0.016f);
        bmo.advance(0.016f);
        require(carrier.getMemoryWithoutUpdate().contains(UpdateQueueMainManager.memKey),
                "Actual BMO callback must complete, including reading its empty upgrade queue");
        System.out.println("PASS: actual Chief Navigator release preserves the carrier for compiled BMO/Nex callbacks.");
        System.out.println("LIMIT: no reporter log or complete game reproduction; versions are the locally installed jars.");
    }
}
