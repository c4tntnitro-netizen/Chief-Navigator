package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import java.lang.reflect.Proxy;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/** Headless checks for assisted/unassisted, pending and one-time salvage claims. */
public final class BudaiSalvageRewardRegression {
    interface Call { Object invoke(String name, Object[] args); }

    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[] {type}, (proxy, method, args) -> {
                    if (method.getName().equals("hashCode")) {
                        return System.identityHashCode(proxy);
                    }
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

    static MemoryAPI memory(Map<String, Object> values) {
        return mock(MemoryAPI.class, (name, args) -> {
            if (name.equals("getBoolean")) return Boolean.TRUE.equals(values.get(args[0]));
            if (name.equals("get")) return values.get(args[0]);
            if (name.equals("contains")) return values.containsKey(args[0]);
            if (name.equals("set")) values.put((String) args[0], args[1]);
            if (name.equals("unset")) values.remove(args[0]);
            return null;
        });
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Fixture {
        final Map<String, Object> saved = new HashMap<>();
        final Map<String, Integer> weapons = new HashMap<>();
        boolean playerAvailable = true;
        boolean cargoAvailable = true;
        boolean reenterOnAdd;
        boolean failNextAdd;
        int addCalls;

        Fixture() {
            MemoryAPI sectorMemory = memory(saved);
            CargoAPI cargo = mock(CargoAPI.class, (name, args) -> {
                if (name.equals("addWeapons")) {
                    check(Boolean.TRUE.equals(saved.get(BudaiSalvageDialogPlugin.GRANTED))
                                    && !saved.containsKey(BudaiSalvageDialogPlugin.PENDING),
                            "The durable claim must commit before cargo addition");
                    if (failNextAdd) {
                        failNextAdd = false;
                        throw new IllegalStateException("Simulated cargo insertion failure");
                    }
                    addCalls++;
                    String weapon = (String) args[0];
                    weapons.merge(weapon, ((Number) args[1]).intValue(), Integer::sum);
                    if (reenterOnAdd) {
                        reenterOnAdd = false;
                        check(BudaiSalvageDialogPlugin.grantReward(false),
                                "A reentrant duplicate callback must observe the committed claim");
                    }
                }
                return null;
            });
            CampaignFleetAPI player = mock(CampaignFleetAPI.class, (name, args) ->
                    name.equals("getCargo") && cargoAvailable ? cargo : null);
            Global.setSector(mock(SectorAPI.class, (name, args) -> {
                if (name.equals("getMemoryWithoutUpdate")) return sectorMemory;
                if (name.equals("getPlayerFleet")) return playerAvailable ? player : null;
                return null;
            }));
        }

        void pending() {
            check(Boolean.TRUE.equals(saved.get(BudaiSalvageDialogPlugin.PENDING))
                            && !saved.containsKey(BudaiSalvageDialogPlugin.GRANTED)
                            && addCalls == 0,
                    "An interrupted claim must stay pending without rewarding or consuming it");
        }

        void rewarded(int count) {
            check(weapons.size() == 1
                            && Integer.valueOf(count).equals(weapons.get(
                                    BudaiSalvageDialogPlugin.WEAPON_ID))
                            && addCalls == 1
                            && Boolean.TRUE.equals(saved.get(BudaiSalvageDialogPlugin.GRANTED))
                            && !saved.containsKey(BudaiSalvageDialogPlugin.PENDING),
                    "The reward must add exactly " + count + " correct weapons, once");
        }
    }

    private static void verifyVariant(boolean assisted, int count) {
        Fixture fixture = new Fixture();
        check(!BudaiSalvageDialogPlugin.grantReward(assisted)
                        && fixture.saved.isEmpty() && fixture.addCalls == 0,
                "An unsolicited extraction callback must not create a reward");
        BudaiSalvageDialogPlugin.request();
        fixture.pending();
        fixture.playerAvailable = false;
        check(!BudaiSalvageDialogPlugin.grantReward(assisted),
                "A missing player fleet must permit a later retry");
        fixture.pending();
        fixture.playerAvailable = true;
        fixture.cargoAvailable = false;
        check(!BudaiSalvageDialogPlugin.grantReward(assisted),
                "Missing cargo must permit a later retry");
        fixture.pending();
        fixture.cargoAvailable = true;
        check(BudaiSalvageDialogPlugin.grantReward(assisted),
                "A pending extraction with cargo must complete");
        fixture.rewarded(count);
        check(BudaiSalvageDialogPlugin.grantReward(assisted)
                        && BudaiSalvageDialogPlugin.grantReward(!assisted),
                "Repeated callbacks from either branch must be idempotent");
        BudaiSalvageDialogPlugin.request();
        fixture.rewarded(count);
    }

    private static void verifyMismatchedBranch(boolean assisted) throws Exception {
        Fixture fixture = new Fixture();
        BudaiSalvageDialogPlugin.request();
        BudaiSalvageDialogPlugin plugin = new BudaiSalvageDialogPlugin();
        Field dialogField = BudaiSalvageDialogPlugin.class.getDeclaredField("dialog");
        dialogField.setAccessible(true);
        dialogField.set(plugin, mock(InteractionDialogAPI.class, (name, values) -> {
            throw new AssertionError("A mismatched extraction must not touch the dialog: " + name);
        }));
        Field assistedField = BudaiSalvageDialogPlugin.class.getDeclaredField("isaAssisted");
        assistedField.setAccessible(true);
        assistedField.setBoolean(plugin, assisted);
        plugin.optionSelected(null, assisted ? "extract_salvage_chief" : "extract_isa");
        fixture.pending();
        check(fixture.weapons.isEmpty(),
                "An opposite-branch callback must not replace the scene's captured reward variant");
    }

    public static void main(String[] args) throws Exception {
        try {
            Global.setSector(null);
            check(!BudaiSalvageDialogPlugin.grantReward(true)
                            && !BudaiSalvageDialogPlugin.grantReward(false),
                    "Missing sector must fail safely");
            BudaiSalvageDialogPlugin.request();
            verifyVariant(true, 2);
            verifyVariant(false, 1);
            verifyMismatchedBranch(true);
            verifyMismatchedBranch(false);

            Fixture legacy = new Fixture();
            legacy.saved.put(BudaiSalvageDialogPlugin.GRANTED, true);
            legacy.saved.put(BudaiSalvageDialogPlugin.PENDING, true);
            legacy.playerAvailable = false;
            check(BudaiSalvageDialogPlugin.grantReward(false)
                            && !legacy.saved.containsKey(BudaiSalvageDialogPlugin.PENDING)
                            && legacy.addCalls == 0,
                    "Legacy completed saves must clear stale pending state without needing cargo");
            legacy.playerAvailable = true;
            check(BudaiSalvageDialogPlugin.grantReward(true)
                            && BudaiSalvageDialogPlugin.grantReward(false),
                    "Legacy claim flags must remain authoritative for both variants");
            BudaiSalvageDialogPlugin.request();
            check(legacy.addCalls == 0 && legacy.weapons.isEmpty()
                            && !legacy.saved.containsKey(BudaiSalvageDialogPlugin.PENDING),
                    "Old two-weapon claims must never receive a new reward or replay the scene");

            Fixture reentrant = new Fixture();
            reentrant.reenterOnAdd = true;
            BudaiSalvageDialogPlugin.request();
            check(BudaiSalvageDialogPlugin.grantReward(true),
                    "Assisted extraction must survive a duplicate nested callback");
            reentrant.rewarded(2);

            Fixture failedAddition = new Fixture();
            failedAddition.failNextAdd = true;
            BudaiSalvageDialogPlugin.request();
            check(!BudaiSalvageDialogPlugin.grantReward(false),
                    "A failed cargo insertion must not report a completed extraction");
            failedAddition.pending();
            check(failedAddition.weapons.isEmpty(),
                    "A failed cargo insertion must not leave a phantom weapon");
            check(BudaiSalvageDialogPlugin.grantReward(false),
                    "A cargo insertion failure must allow the pending extraction to retry");
            failedAddition.rewarded(1);
        } finally {
            Global.setSector(null);
        }
        System.out.println("Budai salvage reward regression checks passed.");
    }
}
