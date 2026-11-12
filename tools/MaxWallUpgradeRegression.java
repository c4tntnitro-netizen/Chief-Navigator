import chiefnavigator.weapons.IthacaResearchUpgrades;
import chiefnavigator.quest.MenelausTrial;
import chiefnavigator.quest.OdysseyExpanseSystem;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/** Actual component recovery, seventh-part installation, and non-destructive test override. */
public final class MaxWallUpgradeRegression {
    private static final String INSTALLED = "$chief_navigator_ithaca_research_component_mask_v1";
    private static final String RECOVERED = "$chief_navigator_ithaca_research_components_recovered_v1";
    private interface Call {
        Object invoke(String name, Object[] args);
    }

    private static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[] {type},
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
                    if (result == float.class) return 0f;
                    if (result == int.class) return 0;
                    if (result == long.class) return 0L;
                    return null;
                }));
    }

    private static final class Fixture {
        final Map<String, Object> state = new HashMap<>();
        final Map<String, Float> held = new HashMap<>();
        int removals, additions;

        Fixture() {
            state.put("$chief_navigator_ithaca_research_consumables_migrated_v2", true);
            MemoryAPI memory = mock(MemoryAPI.class, (name, args) -> {
                Object value = args == null || args.length == 0 ? null : state.get(args[0]);
                if (name.equals("set")) state.put((String) args[0], args[1]);
                if (name.equals("unset")) state.remove(args[0]);
                if (name.equals("get")) return value;
                if (name.equals("getBoolean")) return Boolean.TRUE.equals(value);
                if (name.equals("getInt")) return value instanceof Number ? ((Number) value).intValue() : 0;
                return null;
            });
            CargoAPI cargo = mock(CargoAPI.class, (name, args) -> {
                if (name.equals("getQuantity")) {
                    return held.getOrDefault(((SpecialItemData) args[1]).getId(), 0f);
                }
                if (name.equals("removeItems")) {
                    String id = ((SpecialItemData) args[1]).getId();
                    float amount = ((Number) args[2]).floatValue();
                    if (held.getOrDefault(id, 0f) < amount) return false;
                    held.put(id, held.get(id) - amount);
                    removals++;
                    return true;
                }
                if (name.equals("addSpecial")) {
                    String id = ((SpecialItemData) args[0]).getId();
                    held.put(id, held.getOrDefault(id, 0f) + ((Number) args[1]).floatValue());
                    additions++;
                }
                return null;
            });
            CampaignFleetAPI player = mock(CampaignFleetAPI.class,
                    (name, args) -> name.equals("getCargo") ? cargo : null);
            Global.setSector(mock(SectorAPI.class, (name, args) -> {
                if (name.equals("getMemoryWithoutUpdate")) return memory;
                if (name.equals("getPlayerFleet")) return player;
                return null;
            }));
        }

        void hold(int index, float quantity) {
            held.put(IthacaResearchUpgrades.COMPONENT_IDS[index], quantity);
        }

        int recoveredMask() {
            Object value = state.get(RECOVERED);
            return value instanceof Number ? ((Number) value).intValue() : 0;
        }
    }

    private static void maximumOverride() {
        Fixture f = new Fixture();
        f.state.put(INSTALLED, IthacaResearchUpgrades.MACROSERVOS);

        check(IthacaResearchUpgrades.enableMaximumUpgradeOverride(),
                "override was not enabled");
        check(IthacaResearchUpgrades.isMaximumUpgradeOverrideEnabled(),
                "override flag was not persisted");
        check(IthacaResearchUpgrades.getCampaignMask()
                        == IthacaResearchUpgrades.ALL_UPGRADES,
                "override did not supply all seven components");
        check(IthacaResearchUpgrades.hasUpgrade(
                        IthacaResearchUpgrades.getCampaignMask(),
                        IthacaResearchUpgrades.MACROSERVOS)
                && IthacaResearchUpgrades.hasUpgrade(
                        IthacaResearchUpgrades.getCampaignMask(),
                        IthacaResearchUpgrades.ABYSSAL_LENS),
                "override omitted a component endpoint");
        check(IthacaResearchUpgrades.getInstalledCount() == 1,
                "combat override must not change actual installed progress");
        check(!IthacaResearchUpgrades.hasRecoveredAllOriginalComponents()
                        && f.recoveredMask() == IthacaResearchUpgrades.MACROSERVOS
                        && f.removals == 0 && f.additions == 0 && f.held.isEmpty(),
                "The combat override must not fabricate gathered components or unlock the seventh station");
        String ledger = IthacaResearchUpgrades.getUpgradeLedger();
        check(ledger.contains("Abyssal Focusing Lens: Not recovered")
                        && ledger.contains("Revealed after recovering the six original components")
                        && !ledger.contains("Sanghata orbit, Avici"),
                "The lens recovery lead must remain hidden while only the override grants its combat effect");
    }

    private static void actualRecoveryGate() throws Exception {
        Fixture f = new Fixture();
        check(!IthacaResearchUpgrades.hasRecoveredAllOriginalComponents(),
                "An empty recovery ledger cannot reveal the final station");
        for (int index = 0; index < 6; index++) f.hold(index, 1f);
        Map<String, Float> originalCargo = new HashMap<>(f.held);
        check(IthacaResearchUpgrades.hasRecoveredAllOriginalComponents()
                        && f.recoveredMask() == IthacaResearchUpgrades.ORIGINAL_UPGRADES
                        && IthacaResearchUpgrades.getInstalledCount() == 0
                        && f.held.equals(originalCargo) && f.removals == 0,
                "Holding all six originals reveals Avici without consuming parts or requiring the seventh");
        check(IthacaResearchUpgrades.getUpgradeLedger().contains("Sanghata orbit, Avici"),
                "The newly revealed seventh station must have its exact Avici recovery lead");
        f.held.clear();
        check(IthacaResearchUpgrades.hasRecoveredAllOriginalComponents(),
                "Selling, dropping or storing previously gathered components must preserve the durable reveal");

        f = new Fixture();
        for (int index = 0; index < 6; index++) {
            f.hold(index, 1f);
            check(IthacaResearchUpgrades.hasRecoveredAllOriginalComponents() == (index == 5),
                    "Separate recoveries must accumulate without requiring all parts simultaneously: " + index);
            f.held.clear();
        }
        check(f.recoveredMask() == IthacaResearchUpgrades.ORIGINAL_UPGRADES,
                "Seen recovery history must survive after the last cargo item leaves the fleet");

        f = new Fixture();
        f.state.put(INSTALLED, IthacaResearchUpgrades.ORIGINAL_UPGRADES);
        check(IthacaResearchUpgrades.hasRecoveredAllOriginalComponents() && f.held.isEmpty(),
                "All six actually installed original parts satisfy recovery in existing saves");

        f = new Fixture();
        f.state.put(INSTALLED, IthacaResearchUpgrades.MACROSERVOS | IthacaResearchUpgrades.NAVAL_CYCLER);
        for (int index = 2; index < 6; index++) f.hold(index, 1f);
        check(IthacaResearchUpgrades.hasRecoveredAllOriginalComponents(),
                "Installed parts and recovered cargo jointly satisfy the original-six gate");

        f = new Fixture();
        for (int index = 0; index < 5; index++) f.hold(index, 1f);
        f.hold(6, 1f);
        String salvagePrefix = researchField("RESEARCH_STATION_SALVAGED_PREFIX");
        for (String field : new String[] {"EXPANSE_RESEARCH_PREFIX", "SILENT_WAKE_RESEARCH_PREFIX",
                "ASHEN_VERGE_RESEARCH_PREFIX"}) {
            for (int index = 0; index < 2; index++) f.state.put(salvagePrefix + researchField(field) + index, true);
        }
        check(!IthacaResearchUpgrades.hasRecoveredAllOriginalComponents()
                        && (f.recoveredMask() & IthacaResearchUpgrades.AUTOLOADER_CORE) == 0,
                "A never-held sixth component stays missing despite opening every salvage window and holding the lens");
        f.held.clear();
        check(!IthacaResearchUpgrades.hasRecoveredAllOriginalComponents(),
                "Dropping five known components must not fabricate a recovery of the absent sixth");
        f.hold(5, 1f);
        check(IthacaResearchUpgrades.hasRecoveredAllOriginalComponents(),
                "Only actual recovery of the missing original component finishes the gate");
    }

    private static void seventhInstallationAndLaborProgress() {
        Fixture f = new Fixture();
        f.hold(6, 2f);
        check(IthacaResearchUpgrades.getCampaignMask() == 0
                        && IthacaResearchUpgrades.getAvailableCount() == 1,
                "The recovered lens is inert cargo before Menelaus installs it");
        check(IthacaResearchUpgrades.getUpgradeLedger().contains(
                        "Abyssal Focusing Lens: Recovered - awaiting installation"),
                "Menelaus's ledger must distinguish a held lens from an installed upgrade");
        check(IthacaResearchUpgrades.installRecoveredComponents() == 1
                        && IthacaResearchUpgrades.getCampaignMask() == IthacaResearchUpgrades.ABYSSAL_LENS
                        && IthacaResearchUpgrades.getInstalledCount() == 1
                        && f.held.get(IthacaResearchUpgrades.COMPONENT_IDS[6]) == 1f && f.removals == 1,
                "Normal installation consumes one lens and independently persists its seventh bit");
        check(IthacaResearchUpgrades.installRecoveredComponents() == 0 && f.removals == 1
                        && IthacaResearchUpgrades.getAvailableCount() == 0,
                "A duplicate lens cannot be consumed or counted as another available upgrade");
        check(IthacaResearchUpgrades.getUpgradeLedger().contains("Abyssal Focusing Lens: Installed")
                        && !IthacaResearchUpgrades.hasRecoveredAllOriginalComponents(),
                "Installing the lens must not manufacture the six original recoveries");

        f = new Fixture();
        for (int index = 0; index < 4; index++) f.hold(index, 1f);
        check(IthacaResearchUpgrades.installRecoveredComponents() == 4
                        && MenelausTrial.getResearchObjectiveTarget() == 4
                        && MenelausTrial.getResearchObjectiveProgress() == 4
                        && MenelausTrial.isResearchLaborComplete()
                        && !IthacaResearchUpgrades.hasRecoveredAllOriginalComponents(),
                "Labor II still completes with four original installed components, before the Avici station unlocks");
    }

    private static String researchField(String name) throws Exception {
        Field field = OdysseyExpanseSystem.class.getDeclaredField(name);
        field.setAccessible(true);
        return (String) field.get(null);
    }

    public static void main(String[] args) throws Exception {
        SectorAPI previous = Global.getSector();
        try {
            maximumOverride();
            actualRecoveryGate();
            seventhInstallationAndLaborProgress();
            System.out.println("PASS: all seven combat upgrades, original-six actual/installed/history recovery gate, "
                    + "no salvage-only or override unlock, consumable lens installation, and unchanged four-part Labor II.");
        } finally {
            Global.setSector(previous);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
