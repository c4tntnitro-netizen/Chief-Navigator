package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Headless contracts for victory gating, deferred presentation and rescue state. */
public final class LeagueSurvivorRescueRegression {
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

    static BattleAPI battle(CampaignFleetAPI player, CampaignFleetAPI enemy,
            boolean involved, boolean won, boolean defeated) {
        return mock(BattleAPI.class, (name, args) -> {
            if (name.equals("isPlayerInvolved")) return involved;
            if (name.equals("isOnPlayerSide")) return won && args[0] == player;
            if (name.equals("getNonPlayerSideSnapshot")) return List.of(enemy);
            if (name.equals("wasFleetDefeated")) return defeated && args[0] == enemy;
            return null;
        });
    }

    public static void main(String[] args) {
        Map<String, Object> saved = new HashMap<>();
        Map<String, Object> contactState = new HashMap<>();
        contactState.put("$chief_navigator_ithaca_static_contact_owned_v1", true);
        contactState.put(FobIthacaContacts.LEAGUE_SURVIVOR_MARKER, true);
        MemoryAPI sectorMemory = memory(saved);
        MemoryAPI contactMemory = memory(contactState);
        PersonAPI leader = mock(PersonAPI.class, (name, values) -> {
            if (name.equals("getId")) return FobIthacaContacts.LEAGUE_SURVIVOR_PERSON_ID;
            if (name.equals("getMemoryWithoutUpdate")) return contactMemory;
            return null;
        });
        int[] contactLookups = {0};
        ImportantPeopleAPI people = mock(ImportantPeopleAPI.class, (name, values) -> {
            if (name.equals("getPerson")) { contactLookups[0]++; return leader; }
            if (name.equals("containsPerson")) return values[0] == leader;
            return null;
        });
        boolean[] busy = {false, false, false, false};
        CoreUITabId[] tab = {null};
        int[] openings = {0};
        CampaignUIAPI ui = mock(CampaignUIAPI.class, (name, values) -> {
            if (name.equals("isShowingDialog")) return busy[0];
            if (name.equals("isShowingMenu")) return busy[1];
            if (name.equals("getCurrentCoreTab")) return tab[0];
            if (name.equals("showInteractionDialog")) { openings[0]++; return true; }
            return null;
        });
        BattleAPI activeBattle = mock(BattleAPI.class, (name, values) -> null);
        CampaignFleetAPI player = mock(CampaignFleetAPI.class, (name, values) -> {
            if (name.equals("isPlayerFleet")) return true;
            if (name.equals("getBattle")) return busy[2] ? activeBattle : null;
            if (name.equals("isInHyperspaceTransition")) return busy[3];
            return null;
        });
        Global.setSector(mock(SectorAPI.class, (name, values) -> {
            if (name.equals("getMemoryWithoutUpdate")) return sectorMemory;
            if (name.equals("getPlayerFleet")) return player;
            if (name.equals("getCampaignUI")) return ui;
            if (name.equals("getImportantPeople")) return people;
            return null;
        }));
        MemoryAPI enemyMemory = memory(new HashMap<>(Map.of(
                OdysseyStrandedFleetsScript.SILENT_WAKE_LEAGUE_MARKER, true)));
        CampaignFleetAPI enemy = mock(CampaignFleetAPI.class, (name, values) -> {
            if (name.equals("getMemoryWithoutUpdate")) return enemyMemory;
            return null;
        });
        LeagueSurvivorRescueScript watcher = new LeagueSurvivorRescueScript();
        watcher.reportBattleOccurred(player, battle(player, enemy, false, true, true));
        watcher.reportBattleOccurred(player, battle(player, enemy, true, false, true));
        watcher.reportBattleOccurred(player, battle(player, enemy, true, true, false));
        check(saved.isEmpty(), "AI-only battles, player defeats and enemy escapes must not queue rescue");
        CampaignFleetAPI unrelated = mock(CampaignFleetAPI.class,
                (name, values) -> name.equals("getMemoryWithoutUpdate")
                        ? memory(new HashMap<>()) : null);
        watcher.reportBattleOccurred(player, battle(player, unrelated, true, true, true));
        check(saved.isEmpty(), "An ordinary League fleet must not trigger the authored rescue");
        check(!OdysseyStrandedFleetsScript.isLeagueSurvivorFleet(player),
                "Fleet ownership checks must always exclude the player");
        watcher.reportBattleOccurred(player, battle(player, enemy, true, true, true));
        check(Boolean.TRUE.equals(saved.get(LeagueSurvivorRescueDialogPlugin.PENDING)),
                "A player-side victory over the authored formation must queue the choice");
        for (int index = 0; index < busy.length; index++) {
            busy[index] = true;
            watcher.advance(1f);
            busy[index] = false;
        }
        tab[0] = CoreUITabId.CARGO;
        watcher.advance(1f);
        tab[0] = null;
        check(openings[0] == 0 && contactLookups[0] == 0,
                "Battle, transitions and every UI obstruction must defer without embarking anyone");
        watcher.advance(1f);
        check(openings[0] == 1 && contactLookups[0] == 0
                        && Boolean.TRUE.equals(saved.get(LeagueSurvivorRescueDialogPlugin.PENDING)),
                "Opening the scene must leave the decision pending and the crews unrescued");
        LeagueSurvivorRescueDialogPlugin.completeAbandonment();
        check(Boolean.TRUE.equals(saved.get(LeagueSurvivorRescueDialogPlugin.RESOLVED))
                        && !saved.containsKey(LeagueSurvivorRescueDialogPlugin.PENDING)
                        && contactLookups[0] == 0,
                "Abandonment must consume the choice without creating a contact");
        LeagueSurvivorRescueDialogPlugin.request();
        watcher.advance(1f);
        check(openings[0] == 1, "Abandonment must remain final across later callbacks");

        saved.clear();
        LeagueSurvivorRescueDialogPlugin.request();
        check(LeagueSurvivorRescueDialogPlugin.completeRescue(),
                "The rescue's final choice must embark the owned survivor contact");
        check(Boolean.TRUE.equals(contactState.get("$chief_navigator_ithaca_rescue_in_transit"))
                        && Boolean.TRUE.equals(saved.get(
                                OdysseyStrandedFleetsScript.SILENT_WAKE_LEAGUE_PICKUP_RECORDED))
                        && Boolean.TRUE.equals(saved.get(LeagueSurvivorRescueDialogPlugin.RESOLVED)),
                "Rescued personnel must enter the existing Ithaca delivery lifecycle");
        int committedLookups = contactLookups[0];
        check(LeagueSurvivorRescueDialogPlugin.completeRescue()
                        && contactLookups[0] == committedLookups,
                "Repeated completion callbacks must not duplicate rescued contacts");
        saved.remove(LeagueSurvivorRescueDialogPlugin.RESOLVED);
        LeagueSurvivorRescueDialogPlugin.request();
        check(!saved.containsKey(LeagueSurvivorRescueDialogPlugin.PENDING),
                "Old saves with the legacy pickup flag must never replay the choice");
        Global.setSector(null);
        System.out.println("League survivor rescue regression checks passed.");
    }
}
