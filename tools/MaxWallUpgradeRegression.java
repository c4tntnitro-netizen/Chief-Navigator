import chiefnavigator.weapons.IthacaResearchUpgrades;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
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
            if (name.equals("getBoolean")) {
                return Boolean.TRUE.equals(state.get((String) callArgs[0]));
            }
            return null;
        });
        Global.setSector(mock(SectorAPI.class, (name, callArgs) ->
                name.equals("getMemoryWithoutUpdate") ? memory : null));

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
                "override did not supply all six components");
        check(IthacaResearchUpgrades.hasUpgrade(
                        IthacaResearchUpgrades.getCampaignMask(),
                        IthacaResearchUpgrades.MACROSERVOS)
                && IthacaResearchUpgrades.hasUpgrade(
                        IthacaResearchUpgrades.getCampaignMask(),
                        IthacaResearchUpgrades.AUTOLOADER_CORE),
                "override omitted a component endpoint");
        check(IthacaResearchUpgrades.getInstalledCount() == 1,
                "combat override must not change actual installed progress");
        System.out.println("PASS: maximum Ithaca wall and gate-defense "
                + "upgrades enable all six combat effects without changing "
                + "actual installed-component progression.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
