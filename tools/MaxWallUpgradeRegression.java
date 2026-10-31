import chiefnavigator.weapons.IthacaResearchUpgrades;
import chiefnavigator.quest.MenelausTrial;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/** Headless check for the non-destructive maximum wall-upgrade override. */
public final class MaxWallUpgradeRegression {
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

    public static void main(String[] args) {
        Map<String, Object> state = new HashMap<String, Object>();
        MemoryAPI memory = mock(MemoryAPI.class, (name, callArgs) -> {
            if (name.equals("set")) {
                state.put((String) callArgs[0], callArgs[1]);
            }
            if (name.equals("get")) {
                return state.get((String) callArgs[0]);
            }
            if (name.equals("getInt")) {
                Object value = state.get((String) callArgs[0]);
                return value instanceof Number ? ((Number) value).intValue() : 0;
            }
            if (name.equals("getBoolean")) {
                return Boolean.TRUE.equals(state.get((String) callArgs[0]));
            }
            return null;
        });
        Map<String, Float> held = new HashMap<String, Float>();
        int[] removals = {0};
        CargoAPI cargo = mock(CargoAPI.class, (name, callArgs) -> {
            if (name.equals("getQuantity")) {
                return held.getOrDefault(((SpecialItemData) callArgs[1]).getId(), 0f);
            }
            if (name.equals("removeItems")) {
                String id = ((SpecialItemData) callArgs[1]).getId();
                float amount = ((Number) callArgs[2]).floatValue();
                if (held.getOrDefault(id, 0f) < amount) return false;
                held.put(id, held.get(id) - amount);
                removals[0]++;
                return true;
            }
            if (name.equals("addSpecial")) {
                throw new AssertionError("Research verification must not manufacture cargo");
            }
            return null;
        });
        CampaignFleetAPI player = mock(CampaignFleetAPI.class,
                (name, callArgs) -> name.equals("getCargo") ? cargo : null);
        Global.setSector(mock(SectorAPI.class, (name, callArgs) -> {
            if (name.equals("getMemoryWithoutUpdate")) return memory;
            if (name.equals("getPlayerFleet")) return player;
            return null;
        }));

        state.put("$chief_navigator_ithaca_research_consumables_migrated_v2",
                true);
        state.put("$chief_navigator_ithaca_research_component_mask_v1",
                IthacaResearchUpgrades.MACROSERVOS);

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
                        IthacaResearchUpgrades.AUTOLOADER_CORE),
                "override omitted a component endpoint");
        check(IthacaResearchUpgrades.getInstalledCount() == 1,
                "combat override must not change actual installed progress");
        check(IthacaResearchUpgrades.ALL_UPGRADES == 127
                        && IthacaResearchUpgrades.ORIGINAL_UPGRADES == 63
                        && IthacaResearchUpgrades.hasUpgrade(
                                IthacaResearchUpgrades.getCampaignMask(),
                                IthacaResearchUpgrades.ABYSSAL_LENS),
                "The maximum override extends all six old bits with the independent lens bit");
        check(!IthacaResearchUpgrades.hasRecoveredAllOriginalComponents()
                        && held.isEmpty() && removals[0] == 0,
                "The combat override cannot supply cargo or reveal the seventh station");
        String ledger = IthacaResearchUpgrades.getUpgradeLedger();
        check(IthacaResearchUpgrades.COMPONENT_NAMES.length == 7,
                "Menelaus must track seven independent component states");
        for (String component : IthacaResearchUpgrades.COMPONENT_NAMES) {
            check(ledger.contains(component + ": "), "Missing ledger component: " + component);
        }
        check(ledger.contains("Abyssal Focusing Lens: Not recovered")
                        && ledger.contains("Revealed after recovering the six original components")
                        && !ledger.contains("Sanghata orbit, Avici"),
                "Only actual original-component recovery reveals the seventh station lead");

        state.clear();
        state.put("$chief_navigator_ithaca_research_consumables_migrated_v2", true);
        held.put(IthacaResearchUpgrades.COMPONENT_IDS[6], 2f);
        check(IthacaResearchUpgrades.getCampaignMask() == 0
                        && IthacaResearchUpgrades.getAvailableCount() == 1
                        && IthacaResearchUpgrades.getUpgradeLedger().contains(
                                "Abyssal Focusing Lens: Recovered - awaiting installation"),
                "A held lens stays inert and occupies only one uninstalled ledger entry");
        check(IthacaResearchUpgrades.installRecoveredComponents() == 1
                        && IthacaResearchUpgrades.getCampaignMask() == IthacaResearchUpgrades.ABYSSAL_LENS
                        && IthacaResearchUpgrades.getInstalledCount() == 1
                        && held.get(IthacaResearchUpgrades.COMPONENT_IDS[6]) == 1f
                        && removals[0] == 1,
                "Menelaus consumes exactly one physical lens and installs only its own bit");
        check(IthacaResearchUpgrades.installRecoveredComponents() == 0
                        && IthacaResearchUpgrades.getAvailableCount() == 0
                        && removals[0] == 1
                        && IthacaResearchUpgrades.getUpgradeLedger().contains(
                                "Abyssal Focusing Lens: Installed")
                        && !IthacaResearchUpgrades.hasRecoveredAllOriginalComponents(),
                "A duplicate lens is not consumed and its installation cannot invent original recoveries");

        state.clear();
        held.clear();
        state.put("$chief_navigator_ithaca_research_consumables_migrated_v2", true);
        for (int index = 0; index < 4; index++) {
            held.put(IthacaResearchUpgrades.COMPONENT_IDS[index], 1f);
        }
        check(IthacaResearchUpgrades.installRecoveredComponents() == 4
                        && MenelausTrial.getResearchObjectiveTarget() == 4
                        && MenelausTrial.getResearchObjectiveProgress() == 4
                        && MenelausTrial.isResearchLaborComplete()
                        && !IthacaResearchUpgrades.hasRecoveredAllOriginalComponents(),
                "Four original installations still complete Labor II before the seventh site unlocks");

        state.clear();
        held.clear();
        state.put("$chief_navigator_ithaca_research_consumables_migrated_v2", true);
        for (int index = 0; index < 6; index++) {
            held.put(IthacaResearchUpgrades.COMPONENT_IDS[index], 1f);
            check(IthacaResearchUpgrades.hasRecoveredAllOriginalComponents() == (index == 5),
                    "Actual original recoveries accumulate even when held separately: " + index);
            held.clear();
        }
        check(IthacaResearchUpgrades.hasRecoveredAllOriginalComponents()
                        && IthacaResearchUpgrades.getInstalledCount() == 0
                        && IthacaResearchUpgrades.getUpgradeLedger().contains("Sanghata orbit, Avici"),
                "The seventh lead remains revealed after all six original cargo items leave the fleet");
        System.out.println("PASS: maximum Ithaca wall and gate-defense "
                + "upgrades enable all seven combat effects without changing "
                + "actual installed-component progression; seventh-part ledger, "
                + "consumable installation and original-six recovery gate pass.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
