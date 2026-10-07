package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/** Headless contract for victory -> Menelaus -> completion cinematic. */
public final class MenelausCompletionRegression {
    interface Call { Object invoke(String name, Object[] args); }

    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(), new Class<?>[] {type},
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
                    if (result == int.class) return 0;
                    if (result == float.class) return 0f;
                    return null;
                }));
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        Map<String, Object> flags = new HashMap<String, Object>();
        MemoryAPI memory = mock(MemoryAPI.class, (name, values) -> {
            if (name.equals("getBoolean")) {
                return Boolean.TRUE.equals(flags.get(values[0]));
            }
            if (name.equals("set") && values.length >= 2) {
                flags.put((String) values[0], values[1]);
            }
            if (name.equals("unset")) flags.remove(values[0]);
            return null;
        });
        CampaignFleetAPI player = mock(
                CampaignFleetAPI.class,
                (name, values) -> name.equals("getMemoryWithoutUpdate")
                        ? memory : null);
        InteractionDialogPlugin[] opened = {null};
        CampaignUIAPI ui = mock(CampaignUIAPI.class, (name, values) -> {
            if (name.equals("showInteractionDialog")) {
                opened[0] = (InteractionDialogPlugin) values[0];
                return true;
            }
            return null;
        });
        List<IntelInfoPlugin> hunts = new ArrayList<>();
        IntelManagerAPI manager = mock(IntelManagerAPI.class, (name, values) -> {
            if (name.equals("getIntel")) return new ArrayList<>(hunts);
            if (name.equals("addIntel")) hunts.add((IntelInfoPlugin) values[0]);
            if (name.equals("hasIntel")) return hunts.contains(values[0]);
            return null;
        });
        SectorAPI sector = mock(SectorAPI.class, (name, values) -> {
            if (name.equals("getMemoryWithoutUpdate")) return memory;
            if (name.equals("getPlayerFleet")) return player;
            if (name.equals("getCampaignUI")) return ui;
            if (name.equals("getIntelManager")) return manager;
            return null;
        });
        Global.setSector(sector);

        flags.put(MenelausTrial.ACCEPTED, true);
        flags.put("$chief_navigator_menelaus_final_labor_forced_v1", true);
        flags.put(MenelausTrial.FINAL_LABOR_BRIEFED, true);
        check(MenelausTrial.isFinalLaborActive(),
                "Labor V must be active before the Heavenly Strike dies");

        flags.put(MenelausTrial.GAUTAMA_DEFEATED, true);
        check(MenelausTrial.isFinalDebriefPending(),
                "Victory must route the objective back to Menelaus");
        check(!MenelausTrial.isFinalLaborActive(),
                "The invasion controller must stop during the return trip");
        check(!MenelausTrial.isComplete(),
                "Killing the Strike must not skip Menelaus's debrief");

        check(MenelausTrial.completeFinalDebrief(),
                "Finishing the debrief must commit Labor V");
        check(MenelausTrial.isComplete(),
                "The quest must complete after the debrief");
        check(hunts.size() == 2 && hunts.stream().allMatch(item -> item instanceof PostLaborHuntIntel),
                "The real debrief immediately starts the two boss hunts");
        check(Boolean.TRUE.equals(flags.get(
                        MenelausCompletionDialogPlugin.PENDING)),
                "The debrief must queue the standalone cinematic");

        MenelausCompletionDialogPlugin.tryShowPending();
        check(opened[0] instanceof MenelausCompletionDialogPlugin,
                "The queued cinematic must open as a full interaction dialog");
        check(Boolean.TRUE.equals(flags.get(
                        MenelausCompletionDialogPlugin.SHOWN))
                        && !flags.containsKey(
                                MenelausCompletionDialogPlugin.PENDING),
                "A successfully opened cinematic must be one-time and durable");

        System.out.println("PASS: Heavenly Strike victory routes through "
                + "Menelaus, then opens the one-time completion cinematic.");
    }
}
