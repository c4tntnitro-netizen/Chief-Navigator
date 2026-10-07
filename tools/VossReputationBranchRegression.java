package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.util.Misc.Token;
import com.fs.starfarer.api.util.Misc.TokenType;
import data.campaign.rulecmd.ChiefNavigatorMenelausCMD;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Public query checks: exactly one live Hegemony recognition branch, without side effects. */
public final class VossReputationBranchRegression {
    interface Call { Object invoke(String name, Object[] args); }

    private static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> {
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

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static boolean query(String branch, Map<String, MemoryAPI> memory) {
        return new ChiefNavigatorMenelausCMD().execute("voss_reputation_regression", null,
                List.of(new Token("vossReputation", TokenType.LITERAL),
                        new Token(branch, TokenType.LITERAL)), memory);
    }

    public static void main(String[] args) {
        SectorAPI priorSector = Global.getSector();
        float[] relationship = {0f};
        int[] reads = {0};
        Map<String, Object> staleValues = new HashMap<>();
        staleValues.put("$chiefNavigatorVossHegFriendly", true);
        staleValues.put("$chiefNavigatorVossHegHostile", true);
        staleValues.put("$chiefNavigatorVossHegNeutral", true);
        MemoryAPI memory = mock(MemoryAPI.class, (name, values) -> {
            if (name.equals("set") || name.equals("unset") || name.equals("clear")) {
                throw new AssertionError("Reputation condition must not write memory: " + name);
            }
            if (name.equals("getBoolean")) return Boolean.TRUE.equals(staleValues.get(values[0]));
            if (name.equals("get")) return staleValues.get(values[0]);
            if (name.equals("contains")) return staleValues.containsKey(values[0]);
            return null;
        });
        Map<String, MemoryAPI> scope = Map.of(MemKeys.LOCAL, memory, MemKeys.GLOBAL, memory);
        FactionAPI player = mock(FactionAPI.class, (name, values) -> {
            if (name.equals("getRelationship")) {
                check(values.length == 1 && Factions.HEGEMONY.equals(values[0]),
                        "Ithaca recognition must read core Hegemony, not expedition reputation");
                reads[0]++;
                return relationship[0];
            }
            throw new AssertionError("Recognition must not write faction state: " + name);
        });
        SectorAPI sector = mock(SectorAPI.class, (name, values) -> {
            if (name.equals("getPlayerFaction")) return player;
            throw new AssertionError("Pure reputation query must not alter campaign state: " + name);
        });
        try {
            Global.setSector(sector);
            float[] samples = {-1f, -0.51f, -0.5f, 0f, 0.5f, 0.51f, 1f};
            String[] branches = {"friendly", "hostile", "neutral"};
            int queries = 0;
            // Both directions prove that stale local flags cannot latch an earlier branch.
            for (int direction : new int[]{1, -1}) {
                for (int step = 0; step < samples.length; step++) {
                    relationship[0] = samples[direction > 0 ? step : samples.length - 1 - step];
                    int matches = 0;
                    for (String branch : branches) {
                        boolean expected = switch (branch) {
                            case "friendly" -> relationship[0] > 0.5f;
                            case "hostile" -> relationship[0] < -0.5f;
                            default -> relationship[0] >= -0.5f && relationship[0] <= 0.5f;
                        };
                        boolean actual = query(branch, scope);
                        check(actual == expected,
                                "Wrong " + branch + " result at current reputation " + relationship[0]);
                        if (actual) matches++;
                        queries++;
                    }
                    check(matches == 1, "Each valid reputation must select exactly one branch");
                    check(!query("unknown", scope), "Unrecognized branch must fail closed");
                    queries++;
                }
            }
            check(reads[0] == queries, "Every query must use the current relationship");
            check(staleValues.size() == 3 && staleValues.values().stream().allMatch(Boolean.TRUE::equals),
                    "Stale flags must be ignored, not repaired or consumed");
            check(!new ChiefNavigatorMenelausCMD().execute("missing_branch", null,
                            List.of(new Token("vossReputation", TokenType.LITERAL)), scope),
                    "Incomplete reputation condition must fail closed");
            Global.setSector(null);
            for (String branch : branches) {
                check(!query(branch, scope), "No campaign must fail closed");
            }
            Global.setSector(mock(SectorAPI.class, (name, values) -> null));
            for (String branch : branches) {
                check(!query(branch, scope), "No player faction must fail closed");
            }
            System.out.println("Voss live reputation regression passed: 56 queries, 14 exclusive selections.");
        } finally {
            Global.setSector(priorSector);
        }
    }
}
