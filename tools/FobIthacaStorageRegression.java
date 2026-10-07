package chiefnavigator.quest;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CoreInteractionListener;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SubmarketPlugin;
import com.fs.starfarer.api.campaign.VisualPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Exercises vanilla storage against Ithaca's exact persistent station-owned cargo. */
public final class FobIthacaStorageRegression {
    private static final String CARGO_KEY = "$chief_navigator_ithaca_storage_cargo";
    interface Call { Object invoke(String name, Object[] args); }

    private static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[] { type }, (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("hashCode")) return System.identityHashCode(proxy);
                    if (name.equals("equals")) return proxy == args[0];
                    Object result = call.invoke(name, args);
                    if (result != null) return result;
                    Class<?> returns = method.getReturnType();
                    if (returns == boolean.class) return false;
                    if (returns == int.class) return 0;
                    if (returns == float.class) return 0f;
                    if (returns == long.class) return 0L;
                    if (returns == double.class) return 0d;
                    if (returns == short.class) return (short) 0;
                    if (returns == byte.class) return (byte) 0;
                    if (returns == char.class) return (char) 0;
                    return null;
                }));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Fixture {
        final Map<String, Float> commodities = new HashMap<>();
        final Map<String, Integer> weapons = new HashMap<>();
        final Map<String, Float> specialItems = new HashMap<>();
        final List<FleetMemberAPI> ships = new ArrayList<>();
        final Map<String, Object> stationFlags = new HashMap<>();
        final List<MarketAPI> createdCarriers = new ArrayList<>();
        final FleetMemberAPI storedShip = mock(FleetMemberAPI.class, (name, args) -> {
            if (name.equals("getId")) return "stored_guardian";
            if (name.equals("getHullId")) return "guardian";
            return null;
        });
        final FactionAPI faction = mock(FactionAPI.class,
                (name, args) -> name.equals("getId") ? TaskForceSpartanFaction.ID : null);
        final MemoryAPI stationMemory = mock(MemoryAPI.class, (name, args) -> {
            if (name.equals("getBoolean"))
                return "$chief_navigator_fob_ithaca_owned_v1".equals(args[0]);
            if (name.equals("get")) return stationFlags.get(args[0]);
            if (name.equals("set")) {
                check(args.length == 2 && CARGO_KEY.equals(args[0]) && args[1] == this.cargo,
                        "The station must retain the exact cargo durably, never an expiring cache or copy");
                check(!stationFlags.containsKey(CARGO_KEY), "Stored cargo must not be replaced on reopening");
                stationFlags.put((String) args[0], args[1]);
            }
            return null;
        });
        final FleetDataAPI shipStorage = mock(FleetDataAPI.class, (name, args) -> {
            if (name.equals("getMembersListCopy")) return List.copyOf(ships);
            if (name.equals("addFleetMember")) ships.add((FleetMemberAPI) args[0]);
            if (name.equals("removeFleetMember")) ships.remove(args[0]);
            return null;
        });
        FleetDataAPI mothballedShips;
        MarketAPI attachedMarket;
        CoreInteractionListener dismissalListener;
        CoreUITabId openedTab;
        StoragePlugin openedStorage;
        int mothballInitializations;
        int coreOpens;
        int returnCalls;
        int cargoCreations;
        boolean nativeCargoAvailable = true;
        boolean cargoFreeTransfer;
        boolean stationFreeTransfer;
        boolean failCore;
        boolean failFactory;

        final CargoAPI cargo = mock(CargoAPI.class, (name, args) -> {
            if (name.equals("getMothballedShips")) return mothballedShips;
            if (name.equals("initMothballedShips")) {
                check(mothballedShips == null, "Opening storage must not reset stored ships");
                check(Factions.PLAYER.equals(args[0]),
                        "New ship storage must belong to the player");
                mothballInitializations++;
                mothballedShips = shipStorage;
            }
            if (name.equals("setFreeTransfer")) cargoFreeTransfer = (boolean) args[0];
            if (name.equals("isFreeTransfer")) return cargoFreeTransfer;
            if (name.equals("getCommodityQuantity")) return commodities.getOrDefault(args[0], 0f);
            if (name.equals("addCommodity"))
                commodities.merge((String) args[0], (Float) args[1], Float::sum);
            if (name.equals("removeCommodity"))
                commodities.merge((String) args[0], -(Float) args[1], Float::sum);
            if (name.equals("getNumWeapons")) return weapons.getOrDefault(args[0], 0);
            if (name.equals("addWeapons"))
                weapons.merge((String) args[0], (Integer) args[1], Integer::sum);
            if (name.equals("removeWeapons"))
                weapons.merge((String) args[0], -(Integer) args[1], Integer::sum);
            if (name.equals("addSpecial"))
                specialItems.merge(((SpecialItemData) args[0]).getId(), (Float) args[1], Float::sum);
            if (name.equals("clear") || name.equals("addAll")
                    || name.equals("removeAll") || name.equals("createCopy"))
                throw new AssertionError("Storage must bind cargo, never copy, clear, or migrate it");
            if (name.equals("getCredits"))
                throw new AssertionError("Opening or closing Ithaca storage must never charge credits");
            return null;
        });
        final SectorEntityToken station = mock(SectorEntityToken.class, (name, args) -> {
            if (name.equals("getId")) return "chief_navigator_fob_ithaca";
            if (name.equals("getName")) return "FOB Ithaca";
            if (name.equals("getCustomEntityType")) return "chief_navigator_fob_ithaca_campaign";
            if (name.equals("getMemoryWithoutUpdate")) return stationMemory;
            if (name.equals("getFaction")) return faction;
            if (name.equals("getCargo")) return nativeCargoAvailable ? cargo : null;
            if (name.equals("getMarket")) return attachedMarket;
            if (name.equals("setMarket")) attachedMarket = (MarketAPI) args[0];
            if (name.equals("setFreeTransfer")) stationFreeTransfer = (boolean) args[0];
            if (name.equals("isFreeTransfer")) return stationFreeTransfer;
            return null;
        });
        final VisualPanelAPI visual = mock(VisualPanelAPI.class, (name, args) -> {
            if (!name.equals("showCore")) return null;
            check(args[1] == station, "Native core must still target the authored station");
            check(args[2] == CampaignUIAPI.CoreUITradeMode.OPEN,
                    "Storage must use the colony-standard open trade mode");
            check(attachedMarket != null && createdCarriers.contains(attachedMarket),
                    "Native core constructor must see the temporary storage carrier");
            check(attachedMarket.isHidden() && attachedMarket.isPlayerOwned(),
                    "Temporary storage carrier must be hidden and player-owned");
            check(attachedMarket.getPrimaryEntity() == station,
                    "Native core must retain the station as its storage carrier's target");
            check(attachedMarket.getSubmarketsCopy().size() == 1,
                    "Ithaca's carrier must contain storage only");
            openedStorage = (StoragePlugin) attachedMarket
                    .getSubmarket(Submarkets.SUBMARKET_STORAGE).getPlugin();
            check(openedStorage.getCargo() == cargo,
                    "Native Storage must expose the exact persistent entity cargo");
            check(cargo.isFreeTransfer(), "The persistent cargo itself must allow free transfer");
            check(openedStorage.getCargo().getMothballedShips() == shipStorage,
                    "Native fleet storage must expose the persistent mothballed ships");
            check(openedStorage.getOnClickAction(null) == SubmarketPlugin.OnClickAction.OPEN_SUBMARKET,
                    "Storage must open immediately without an unlock payment");
            check(openedStorage.isFreeTransfer() && openedStorage.getTariff() == 0f
                            && !openedStorage.isParticipatesInEconomy(),
                    "Storage must be free and outside the economy");
            check(!openedStorage.isIllegalOnSubmarket("heavy_armaments",
                            SubmarketPlugin.TransferAction.PLAYER_SELL)
                            && !openedStorage.isIllegalOnSubmarket(storedShip,
                            SubmarketPlugin.TransferAction.PLAYER_BUY),
                    "Stored items and ships must have unrestricted player-owned transfers");
            openedTab = (CoreUITabId) args[0];
            dismissalListener = (CoreInteractionListener) args[3];
            coreOpens++;
            if (failCore) throw new IllegalStateException("Simulated native core failure");
            return null;
        });
        final InteractionDialogAPI dialog = mock(InteractionDialogAPI.class, (name, args) -> {
            if (name.equals("getInteractionTarget")) return station;
            if (name.equals("getVisualPanel")) return visual;
            return null;
        });

        MarketAPI createCarrier() {
            final boolean[] hidden = { false };
            final boolean[] playerOwned = { false };
            final SectorEntityToken[] primary = { null };
            final StoragePlugin storage = new StoragePlugin();
            final List<SubmarketAPI> submarkets = new ArrayList<>();
            final MarketAPI[] holder = { null };
            final SubmarketAPI submarket = mock(SubmarketAPI.class, (name, args) -> {
                if (name.equals("getMarket")) return holder[0];
                if (name.equals("getPlugin")) return storage;
                if (name.equals("getCargo") || name.equals("getCargoNullOk")) return storage.getCargo();
                if (name.equals("getFaction")) return faction;
                if (name.equals("getSpecId")) return Submarkets.SUBMARKET_STORAGE;
                return null;
            });
            MarketAPI carrier = mock(MarketAPI.class, (name, args) -> {
                if (name.equals("setHidden")) hidden[0] = (boolean) args[0];
                if (name.equals("isHidden")) return hidden[0];
                if (name.equals("setPlayerOwned")) playerOwned[0] = (boolean) args[0];
                if (name.equals("isPlayerOwned")) return playerOwned[0];
                if (name.equals("getFaction")) return faction;
                if (name.equals("getPrimaryEntity")) return primary[0];
                if (name.equals("setPrimaryEntity")) {
                    check(args[0] == null || args[0] == station,
                            "Carrier may bind only the actual station during native core access");
                    primary[0] = (SectorEntityToken) args[0];
                }
                if (name.equals("getSubmarket")) {
                    check(Submarkets.SUBMARKET_STORAGE.equals(args[0]),
                            "Storage carrier must never request a trade submarket");
                    return submarkets.isEmpty() ? null : submarket;
                }
                if (name.equals("hasSubmarket")) return !submarkets.isEmpty();
                if (name.equals("getSubmarketsCopy")) return List.copyOf(submarkets);
                if (name.equals("addSubmarket")) {
                    check(Submarkets.SUBMARKET_STORAGE.equals(args[0]),
                            "Ithaca must add vanilla Storage only");
                    check(submarkets.isEmpty(), "Storage must not be added twice to the carrier");
                    submarkets.add(submarket);
                    storage.init(submarket);
                }
                if (name.equals("addPerson") || name.equals("addIndustry") || name.equals("addCondition"))
                    throw new AssertionError("Storage carrier must not mutate campaign topology or people");
                return null;
            });
            holder[0] = carrier;
            createdCarriers.add(carrier);
            return carrier;
        }

        void install() {
            Global.setFactory(mock(FactoryAPI.class, (name, args) -> {
                if (name.equals("createMarket")) {
                    check("chief_navigator_fob_ithaca_storage_view".equals(args[0])
                                    && "FOB Ithaca".equals(args[1]) && (int) args[2] == 0,
                            "The carrier must remain a size-zero transient storage view");
                    if (failFactory) throw new IllegalStateException("Simulated carrier creation failure");
                    return createCarrier();
                }
                if (name.equals("createCargo")) {
                    check(!nativeCargoAvailable && !stationFlags.containsKey(CARGO_KEY)
                                    && cargoCreations == 0 && Boolean.TRUE.equals(args[0]),
                            "Create unlimited storage only once when both native and retained cargo are absent");
                    cargoCreations++;
                    return cargo;
                }
                return null;
            }));
            Global.setSector(mock(SectorAPI.class, (name, args) -> {
                if (name.equals("getEconomy") || name.equals("getImportantPeople"))
                    throw new AssertionError("Opening storage must not access economy or resident registries");
                return null;
            }));
        }

        void open(CoreUITabId tab) {
            MarketAPI previous = attachedMarket;
            FobIthacaStorageDialog.show(dialog, tab, () -> {
                check(attachedMarket == previous,
                        "The carrier must be detached before the caller restores its station menu");
                assertCarrierReleased();
                returnCalls++;
            });
        }

        void close() {
            check(dismissalListener != null, "Native core must retain a dismissal callback");
            dismissalListener.coreUIDismissed();
            check(attachedMarket == null, "Closing native storage must detach its carrier");
            assertCarrierReleased();
        }

        void assertCarrierReleased() {
            for (MarketAPI carrier : createdCarriers)
                check(carrier.getPrimaryEntity() == null,
                        "A closed or failed carrier must release its station reference");
        }
    }

    private static void verifyPersistentItemsAndShips() {
        Fixture f = new Fixture();
        f.install();
        f.cargo.addCommodity("supplies", 13f);
        f.cargo.addWeapons("heavymauler", 2);
        f.cargo.addSpecial(new SpecialItemData("pristine_nanoforge", null), 1f);
        f.open(CoreUITabId.CARGO);
        f.openedStorage.getCargo().addCommodity("supplies", 7f);
        f.openedStorage.getCargo().addWeapons("heavymauler", 1);
        f.openedStorage.getCargo().getMothballedShips().addFleetMember(f.storedShip);
        f.close();
        f.open(CoreUITabId.FLEET);
        check(f.openedTab == CoreUITabId.FLEET, "Fleet entry must open the native Fleet tab");
        check(f.openedStorage.getCargo().getCommodityQuantity("supplies") == 20f
                        && f.openedStorage.getCargo().getNumWeapons("heavymauler") == 3
                        && f.specialItems.get("pristine_nanoforge") == 1f,
                "Stored cargo must survive closing and reopening through another tab");
        check(f.openedStorage.getCargo().getMothballedShips().getMembersListCopy()
                        .equals(List.of(f.storedShip)),
                "Stored ships must survive closing and reopening");
        f.openedStorage.getCargo().removeCommodity("supplies", 4f);
        f.openedStorage.getCargo().getMothballedShips().removeFleetMember(f.storedShip);
        f.close();
        f.open(CoreUITabId.REFIT);
        check(f.openedTab == CoreUITabId.REFIT, "Refit entry must open the native Refit tab");
        check(f.cargo.getCommodityQuantity("supplies") == 16f && f.ships.isEmpty(),
                "Withdrawals must immediately affect the station's persistent items and ships");
        check(f.mothballInitializations == 1,
                "Cargo's ship storage must initialize only when absent");
        f.close();
        check(f.returnCalls == 3, "Every core dismissal must restore the station exactly once");
    }

    private static void verifyNullableEntityCargo() {
        // Native CustomCampaignEntity.getCargo() can return null after an empty
        // cargo is pruned by writeReplace. Refit must work in that actual state,
        // not just with the permanently non-null cargo used by earlier mocks.
        for (CoreUITabId first : List.of(CoreUITabId.REFIT, CoreUITabId.CARGO, CoreUITabId.FLEET)) {
            Fixture f = new Fixture(); f.nativeCargoAvailable = false; f.install();
            f.open(first);
            check(f.cargoCreations == 1 && f.stationFlags.get(CARGO_KEY) == f.cargo,
                    "Any first entry must create and durably retain missing station storage");
            f.close();
            for (CoreUITabId tab : List.of(CoreUITabId.CARGO, CoreUITabId.FLEET, CoreUITabId.REFIT)) {
                f.open(tab); f.close();
            }
            check(f.cargoCreations == 1 && f.mothballInitializations == 1,
                    "Closing empty storage and switching entry tabs must not recreate cargo or ship storage");
            f.open(CoreUITabId.FLEET);
            f.openedStorage.getCargo().addCommodity("supplies", 11f);
            f.openedStorage.getCargo().getMothballedShips().addFleetMember(f.storedShip);
            f.close(); f.open(CoreUITabId.REFIT);
            check(f.cargo.getCommodityQuantity("supplies") == 11f
                            && f.ships.equals(List.of(f.storedShip)) && f.cargoCreations == 1,
                    "Refit must preserve the exact stored Guardian and items when native entity cargo is null");
            f.close();
        }
    }

    private static void verifyPrunedNativeCargo() {
        Fixture f = new Fixture(); f.install();
        f.open(CoreUITabId.CARGO); f.close();
        check(f.stationFlags.get(CARGO_KEY) == f.cargo && f.cargoCreations == 0,
                "A native cargo must be retained by identity, not replaced");
        f.nativeCargoAvailable = false;
        f.open(CoreUITabId.REFIT);
        check(f.openedStorage.getCargo() == f.cargo && f.cargoCreations == 0,
                "Native pruning of an empty entity cargo must not replace the station-owned backing store");
        f.close();
    }

    private static void verifyExistingShipStorage() {
        Fixture f = new Fixture();
        f.install();
        f.mothballedShips = f.shipStorage;
        f.ships.add(f.storedShip);
        f.open(CoreUITabId.FLEET);
        check(f.mothballInitializations == 0 && f.ships.equals(List.of(f.storedShip)),
                "Opening Fleet must preserve an existing mothballed ship container");
        f.close();
    }

    private static void verifyPreviousMarketAndDuplicateDismissal() {
        Fixture f = new Fixture();
        f.install();
        MarketAPI previous = mock(MarketAPI.class, (name, args) -> null);
        f.attachedMarket = previous;
        f.open(CoreUITabId.CARGO);
        check(f.attachedMarket != previous,
                "Core tabs need their temporary carrier for the entire open session");
        f.dismissalListener.coreUIDismissed();
        check(f.attachedMarket == previous,
                "Closing storage must restore the station's exact previous market");
        f.assertCarrierReleased();
        f.dismissalListener.coreUIDismissed();
        check(f.returnCalls == 1 && f.attachedMarket == previous,
                "Duplicate native dismissals must not duplicate callbacks or change restored state");
    }

    private static void verifyFailureCleanup() {
        Fixture f = new Fixture();
        f.install();
        MarketAPI previous = mock(MarketAPI.class, (name, args) -> null);
        f.attachedMarket = previous;
        f.failCore = true;
        try {
            f.open(CoreUITabId.CARGO);
            throw new AssertionError("Native core construction failure must propagate");
        } catch (IllegalStateException expected) {
            check(expected.getMessage().equals("Simulated native core failure"),
                    "The original native core failure must be preserved");
        }
        check(f.attachedMarket == previous && f.returnCalls == 0,
                "Native core failure must restore the previous market without dismissing the caller");
        f.assertCarrierReleased();

        Fixture creation = new Fixture();
        creation.install();
        creation.attachedMarket = previous;
        creation.failFactory = true;
        try {
            creation.open(CoreUITabId.FLEET);
            throw new AssertionError("Carrier creation failure must propagate");
        } catch (IllegalStateException expected) {
            check(expected.getMessage().equals("Simulated carrier creation failure"),
                    "The original carrier creation failure must be preserved");
        }
        check(creation.attachedMarket == previous && creation.coreOpens == 0
                        && creation.returnCalls == 0,
                "A failed carrier factory must leave the station untouched");
    }

    private static void verifyUnrelatedMarketReplacement() {
        Fixture f = new Fixture();
        f.install();
        final int[] returns = { 0 };
        FobIthacaStorageDialog.show(f.dialog, CoreUITabId.REFIT, () -> returns[0]++);
        MarketAPI replacement = mock(MarketAPI.class, (name, args) -> null);
        f.attachedMarket = replacement;
        f.dismissalListener.coreUIDismissed();
        check(f.attachedMarket == replacement && returns[0] == 1,
                "Closing storage must not overwrite an independently replaced market");
        f.assertCarrierReleased();
    }

    public static void main(String[] args) {
        SectorAPI previousSector = Global.getSector();
        FactoryAPI previousFactory = Global.getFactory();
        try {
            verifyPersistentItemsAndShips();
            verifyNullableEntityCargo();
            verifyPrunedNativeCargo();
            verifyExistingShipStorage();
            verifyPreviousMarketAndDuplicateDismissal();
            verifyFailureCleanup();
            verifyUnrelatedMarketReplacement();
            System.out.println("FOB Ithaca native storage regression passed");
        } finally {
            Global.setSector(previousSector);
            Global.setFactory(previousFactory);
        }
    }
}
