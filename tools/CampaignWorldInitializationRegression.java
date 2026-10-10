import chiefnavigator.campaign.CampaignWorldInitialization;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Behavioral checks for first installation and serialized-state adoption. */
public final class CampaignWorldInitializationRegression {
    interface Call { Object invoke(String name, Object[] arguments); }

    private static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[] {type}, (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("hashCode")) return System.identityHashCode(proxy);
                    if (name.equals("equals")) return proxy == args[0];
                    if (name.equals("toString")) return type.getSimpleName();
                    return call.invoke(name, args);
                }));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static SectorEntityToken entity(String id) {
        return mock(SectorEntityToken.class, (name, args) -> {
            if (name.equals("getId")) return id;
            throw new AssertionError("Unexpected entity access: " + name);
        });
    }

    private static StarSystemAPI system(
            String id, SectorEntityToken... entities) {
        return mock(StarSystemAPI.class, (name, args) -> {
            if (name.equals("getOptionalUniqueId")) return id;
            if (name.equals("getAllEntities")) return Arrays.asList(entities);
            throw new AssertionError("Unexpected system mutation/access: " + name);
        });
    }

    private static final class Save {
        final Map<String, Object> flags = new HashMap<>();
        final List<StarSystemAPI> systems = new ArrayList<>();
        final List<SectorEntityToken> hyperEntities = new ArrayList<>();
        int writes;
        final MemoryAPI memory = mock(MemoryAPI.class, (name, args) -> {
            if (name.equals("contains")) return flags.containsKey(args[0]);
            if (name.equals("getBoolean")) return Boolean.TRUE.equals(flags.get(args[0]));
            if (name.equals("getKeys")) return new ArrayList<>(flags.keySet());
            if (name.equals("set")) {
                check(args.length == 2, "Initialization marker must never expire");
                flags.put((String) args[0], args[1]);
                writes++;
                return null;
            }
            throw new AssertionError("Unexpected memory mutation/access: " + name);
        });
        final LocationAPI hyperspace = mock(LocationAPI.class, (name, args) -> {
            if (name.equals("getAllEntities")) return hyperEntities;
            throw new AssertionError("Unexpected hyperspace mutation/access: " + name);
        });
        final SectorAPI api = mock(SectorAPI.class, (name, args) -> {
            if (name.equals("getMemoryWithoutUpdate")) return memory;
            if (name.equals("getStarSystems")) return systems;
            if (name.equals("getHyperspace")) return hyperspace;
            throw new AssertionError("Unexpected sector mutation/access: " + name);
        });

        boolean begin(boolean newGame) {
            return CampaignWorldInitialization.begin(api, newGame);
        }
    }

    private static void checkFreshInstallAndReload() {
        Save save = new Save();
        StarSystemAPI vanilla = system("corvus", entity("jangala"));
        save.systems.add(vanilla);
        save.hyperEntities.add(entity("corvus_jump"));
        save.flags.put("$another_mod_started", true);
        check(save.begin(false), "Clean existing campaign must allow installation");
        check(Boolean.TRUE.equals(save.flags.get(
                        CampaignWorldInitialization.ATTEMPTED)),
                "Installation must latch before any generation code runs");
        check(save.systems.size() == 1 && save.systems.get(0) == vanilla,
                "Deciding to install must preserve vanilla system identity");
        check(Boolean.TRUE.equals(save.flags.get("$another_mod_started")),
                "Other mods' progress must remain intact");

        // Simulate serialization into a new session, even if generated worlds
        // are subsequently missing: the saved decision must remain authoritative.
        Save reloaded = new Save();
        reloaded.flags.putAll(save.flags);
        check(!reloaded.begin(false), "Reload must never repeat installation");
        check(reloaded.writes == 0, "Reload must not rewrite its permanent latch");
        check(!reloaded.begin(true), "A saved latch must also defeat new-game callbacks");
    }

    private static void checkLegacyAndPartialWorlds() {
        String[] ownedIds = {
            "chief_navigator_treadmill", "chief_navigator_odyssey_expanse",
            "chief_navigator_odyssey_messina", "chief_navigator_odyssey_silent_wake",
            "chief_navigator_odyssey_ashen_verge", "chief_navigator_odyssey_pale_reach",
            "chief_navigator_odyssey_devoured_reach"
        };
        for (String id : ownedIds) {
            Save partial = new Save();
            StarSystemAPI survivor = system(id);
            partial.systems.add(survivor);
            check(!partial.begin(false), "A lone legacy system must block generation: " + id);
            check(partial.systems.size() == 1 && partial.systems.get(0) == survivor,
                    "Legacy system identity must survive adoption");
            check(partial.writes == 1, "Legacy adoption must permanently record its decision");
            partial.systems.clear();
            check(!partial.begin(false), "Missing legacy worlds must never be recreated");
        }

        Save claimed = new Save();
        claimed.systems.add(system("chief_navigator_foreign_claim"));
        check(!claimed.begin(true), "Namespace claims must be preserved even in a new campaign");

        Save localRemnant = new Save();
        localRemnant.systems.add(system("other_system", entity("chief_navigator_fob_ithaca")));
        check(!localRemnant.begin(false), "Static local remnants must block reconstruction");

        Save hyperRemnant = new Save();
        hyperRemnant.hyperEntities.add(entity("chief_navigator_troy_access_hyper"));
        check(!hyperRemnant.begin(false), "Orphaned hyperspace endpoints must block reconstruction");
    }

    private static void checkLegacyProgressAndFailure() {
        for (Object value : new Object[] {true, false, "completed"}) {
            Save legacy = new Save();
            legacy.flags.put("$chief_navigator_started", value);
            check(!legacy.begin(false), "Existing mod memory must block fresh generation");
            check(value.equals(legacy.flags.get("$chief_navigator_started")),
                    "Adoption must preserve saved quest values");
        }

        Save fresh = new Save();
        fresh.flags.put("$chief_navigatorish_other_mod", true);
        check(fresh.begin(false), "A similar unrelated namespace must not block installation");

        Save newCampaign = new Save();
        newCampaign.flags.put("$chief_navigator_pre_generation_hook", true);
        check(newCampaign.begin(true), "New campaigns must retain initial generation");

        Save interrupted = new Save();
        check(interrupted.begin(false), "First construction attempt must be allowed");
        // A factory can fail before even the first system exists. The next load
        // must not infer a fresh installation from that absence and try again.
        check(!interrupted.begin(false), "Interrupted construction must remain quarantined");
        check(interrupted.writes == 1, "Failed initialization must retain the original latch");

        Save invalidMarker = new Save();
        invalidMarker.flags.put(CampaignWorldInitialization.ATTEMPTED, false);
        check(!invalidMarker.begin(false), "An existing invalid latch must fail closed");
        check(!CampaignWorldInitialization.begin(null, false), "No sector means no installation");
    }

    public static void main(String[] args) {
        checkFreshInstallAndReload();
        checkLegacyAndPartialWorlds();
        checkLegacyProgressAndFailure();
        checkExplicitInstallation();
        System.out.println("Campaign world initialization regression passed.");
    }

    private static void checkExplicitInstallation() {
        Save stranded = new Save();
        stranded.flags.put("$chief_navigator_started", true);
        stranded.flags.put("$chief_navigator_eventide_briefed", true);
        check(!stranded.begin(false), "Normal load must still adopt a legacy quest");
        check(CampaignWorldInitialization.beginExplicitInstallation(stranded.api),
                "Explicit command must recover a completely absent pre-departure world");
        check(Boolean.TRUE.equals(stranded.flags.get("$chief_navigator_started"))
                        && Boolean.TRUE.equals(stranded.flags.get("$chief_navigator_eventide_briefed")),
                "Explicit installation must preserve the briefing and officer progress");
        check(!CampaignWorldInitialization.beginExplicitInstallation(stranded.api),
                "Even a factory failure must never repeat the explicit attempt");

        for (boolean hyper : new boolean[] {false, true}) {
            Save partial = new Save();
            if (hyper) partial.hyperEntities.add(entity("chief_navigator_troy_access_hyper"));
            else partial.systems.add(system("other", entity("chief_navigator_orphan")));
            check(!CampaignWorldInitialization.beginExplicitInstallation(partial.api),
                    "Any surviving owned/claimed token must block explicit construction");
            check(partial.writes == 0, "Refusal must not mutate a partial save");
        }
        Save claimant = new Save();
        claimant.systems.add(system("chief_navigator_foreign_claim"));
        check(!CampaignWorldInitialization.beginExplicitInstallation(claimant.api),
                "Foreign namespace claims must remain authoritative");
        for (String key : new String[] {"$chief_navigator_treadmill_entered",
                "$chief_navigator_troy_arrival_notice_shown",
                "$chief_navigator_troy_wormhole_opened",
                "$chief_navigator_odyssey_expanse_entered",
                "$chief_navigator_menelaus_trial_accepted",
                "$chief_navigator_menelaus_final_debrief_complete_v1"}) {
            Save progressed = new Save();
            progressed.flags.put(key, true);
            check(!CampaignWorldInitialization.beginExplicitInstallation(progressed.api),
                    "Committed expedition progress must block recovery: " + key);
            check(progressed.writes == 0, "Refusal must preserve committed progress");
        }
        check(!CampaignWorldInitialization.beginExplicitInstallation(null),
                "Explicit command requires a live sector");
    }
}
