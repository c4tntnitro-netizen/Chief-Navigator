package chiefnavigator.quest;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI;
import com.fs.starfarer.api.campaign.ai.TacticalModulePlugin;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Headless checks for the command invasion's exclusive campaign target. */
public final class IthacaEndgameTargetLockRegression {
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
                    return null;
                }));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        List<SectorEntityToken> invasionBlocks =
                new ArrayList<SectorEntityToken>();
        List<SectorEntityToken> reciprocalBlocks =
                new ArrayList<SectorEntityToken>();
        final SectorEntityToken[] tacticalTarget = {null};
        final SectorEntityToken[] priorityTarget = {null};

        TacticalModulePlugin tactical = mock(
                TacticalModulePlugin.class,
                (name, callArgs) -> {
                    if (name.equals("getTarget")) return tacticalTarget[0];
                    if (name.equals("setTarget")) {
                        tacticalTarget[0] = (SectorEntityToken) callArgs[0];
                    }
                    if (name.equals("setPriorityTarget")) {
                        priorityTarget[0] =
                                (SectorEntityToken) callArgs[0];
                    }
                    return null;
                });
        ModularFleetAIAPI invasionAI = mock(
                ModularFleetAIAPI.class,
                (name, callArgs) -> {
                    if (name.equals("getTacticalModule")) return tactical;
                    if (name.equals("doNotAttack")) {
                        invasionBlocks.add((SectorEntityToken) callArgs[0]);
                    }
                    return null;
                });

        CampaignFleetAPI invasion = mock(
                CampaignFleetAPI.class,
                (name, callArgs) -> name.equals("getAI")
                        ? invasionAI : null);
        CampaignFleetAPI target = mock(
                CampaignFleetAPI.class,
                (name, callArgs) -> null);
        ModularFleetAIAPI tfsAI = mock(
                ModularFleetAIAPI.class,
                (name, callArgs) -> {
                    if (name.equals("doNotAttack")) {
                        reciprocalBlocks.add(
                                (SectorEntityToken) callArgs[0]);
                    }
                    return null;
                });
        CampaignFleetAPI tfs = mock(
                CampaignFleetAPI.class,
                (name, callArgs) -> name.equals("getAI") ? tfsAI : null);
        CampaignFleetAPI playerLike = mock(
                CampaignFleetAPI.class,
                (name, callArgs) -> null);
        CampaignFleetAPI otherBastion = mock(
                CampaignFleetAPI.class,
                (name, callArgs) -> name.equals("getAI") ? tfsAI : null);
        StarSystemAPI system = mock(
                StarSystemAPI.class,
                (name, callArgs) -> name.equals("getFleets")
                        ? Arrays.asList(
                                invasion,
                                target,
                                tfs,
                                playerLike,
                                otherBastion)
                        : null);

        int blocked = OdysseyPredatorScript.lockIthacaEndgameTarget(
                invasion, system, target);
        check(blocked == 3, "Every non-target fleet must be excluded");
        check(invasionBlocks.contains(tfs), "TFS must not distract invasion");
        check(invasionBlocks.contains(playerLike),
                "Player must not distract invasion");
        check(invasionBlocks.contains(otherBastion),
                "Other bastions must not distract invasion");
        check(!invasionBlocks.contains(target),
                "Assigned bastion must remain attackable");
        check(reciprocalBlocks.size() == 2
                        && reciprocalBlocks.get(0) == invasion
                        && reciprocalBlocks.get(1) == invasion,
                "NPC fleets must be prevented from intercepting invasion");
        check(priorityTarget[0] == target,
                "Assigned bastion must remain the priority target");
        check(tacticalTarget[0] == target,
                "Assigned bastion must be the active tactical target");

        System.out.println("PASS: invasion ignores player/TFS/other bastions "
                + "and remains hard-locked to its assigned wall module.");
    }
}
