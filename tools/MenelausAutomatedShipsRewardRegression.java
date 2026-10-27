package chiefnavigator.campaign;

import chiefnavigator.quest.MenelausTrial;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;
import com.fs.starfarer.api.util.Misc.TokenType;
import com.fs.starfarer.campaign.CampaignEngine;
import com.fs.starfarer.campaign.CharacterStats;
import com.fs.starfarer.loading.SkillSpec;
import data.campaign.rulecmd.ChiefNavigatorMenelausCMD;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;
import sun.misc.Unsafe;

/** Exercises the real native skill setter, point accounting and character refresh. */
public final class MenelausAutomatedShipsRewardRegression {
    private static final String REWARD = "$chief_navigator_menelaus_automated_ships_reward_v1";
    private interface Call { Object invoke(String name, Object[] args); }

    private static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
            if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
            if (method.getName().equals("equals")) return proxy == args[0];
            Object value = call.invoke(method.getName(), args);
            if (value != null) return value;
            Class<?> result = method.getReturnType();
            if (result == boolean.class) return false;
            if (result == int.class) return 0;
            if (result == float.class) return 0f;
            if (result == long.class) return 0L;
            if (result == double.class) return 0d;
            return null;
        }));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static Unsafe unsafe() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }

    private static void engine(CampaignEngine value) throws Exception {
        Field field = CampaignEngine.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, value);
    }

    /** Colony administration is unrelated to this headless skill fixture. */
    private static final class NativeStats extends CharacterStats {
        int refreshes;
        @Override public void refreshCharacterStatsEffects() { refreshes++; super.refreshCharacterStatsEffects(); }
        @Override public void refreshAllOutpostsEffectsForPlayerOutposts() { }
    }

    private static final class NativeEngine extends CampaignEngine {
        CharacterStats player;
        @Override public CharacterStats getPlayerStats() { return player; }
    }

    private static final class Fixture {
        final Map<String, Object> saved = new HashMap<>();
        final NativeStats stats;
        final NativeEngine engine;
        final FleetMemberAPI untouchedShip = mock(FleetMemberAPI.class, (name, args) -> {
            throw new AssertionError("Reward must never inspect or mutate ship variants: " + name);
        });
        final InteractionDialogAPI dialog = mock(InteractionDialogAPI.class, (name, args) -> null);
        int removals, rewardWrites, rosterReads;
        boolean statsAvailable = true;

        Fixture() throws Exception {
            saved.put("$chief_navigator_menelaus_created_v1", true);
            saved.put("$chief_navigator_sinni_created_v1", true);
            engine = (NativeEngine) unsafe().allocateInstance(NativeEngine.class);
            engine(engine);
            stats = new NativeStats();
            engine.player = stats;
            stats.setPoints(3);
            MemoryAPI memory = mock(MemoryAPI.class, (name, args) -> {
                if (name.equals("getBoolean")) return Boolean.TRUE.equals(saved.get(args[0]));
                if (name.equals("get")) return saved.get(args[0]);
                if (name.equals("set")) {
                    if (REWARD.equals(args[0])) {
                        check(args.length == 2, "Reward claim must be durable without an expiry");
                        rewardWrites++;
                    }
                    saved.put((String) args[0], args[1]);
                }
                return null;
            });
            FleetDataAPI fleetData = mock(FleetDataAPI.class, (name, args) -> {
                if (name.equals("getOfficersCopy")) return List.of();
                if (name.equals("getMembersListCopy")) { rosterReads++; return List.of(untouchedShip); }
                return null;
            });
            CampaignFleetAPI fleet = mock(CampaignFleetAPI.class, (name, args) -> name.equals("getFleetData") ? fleetData : null);
            PersonAPI player = mock(PersonAPI.class, (name, args) -> name.equals("getStats") ? statsAvailable ? stats : null : null);
            ImportantPeopleAPI people = mock(ImportantPeopleAPI.class, (name, args) -> null);
            ListenerManagerAPI listeners = mock(ListenerManagerAPI.class, (name, args) -> {
                if (name.equals("getListeners")) return List.of();
                if (name.equals("removeListenerOfClass")) {
                    check(args[0] == DomainSecurityIFFAuthorization.class, "Only the historical IFF listener may be removed");
                    removals++;
                }
                if (name.equals("addListener")) throw new AssertionError("The historical recovery listener must stay retired");
                return null;
            });
            Global.setSector(mock(SectorAPI.class, (name, args) -> {
                if (name.equals("getMemoryWithoutUpdate")) return memory;
                if (name.equals("getPlayerStats")) return statsAvailable ? stats : null;
                if (name.equals("getPlayerPerson")) return statsAvailable ? player : null;
                if (name.equals("getPlayerFleet")) return fleet;
                if (name.equals("getImportantPeople")) return people;
                if (name.equals("getListenerManager")) return listeners;
                return null;
            }));
        }

        void accepted() { saved.put(MenelausTrial.ACCEPTED, true); }
        void contact() {
            check(new ChiefNavigatorMenelausCMD().execute("reward_regression", dialog,
                    List.of(new Token("init", TokenType.LITERAL)), Map.of()), "Actual Menelaus contact must remain functional");
        }
        void oldHooks() throws Exception {
            DomainSecurityIFFAuthorization.install();
            DomainSecurityIFFAuthorization.synchronizeAuthorization();
            DomainSecurityIFFAuthorization.authorizeTransferredDrone(untouchedShip);
            Constructor<DomainSecurityIFFAuthorization> constructor = DomainSecurityIFFAuthorization.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance().reportShipsRecovered(List.of(untouchedShip), dialog);
            check(rosterReads == 0, "Load/timer/recovery hooks must not sweep the fleet");
        }
    }

    private static void freeSkillAndNativeEffects() throws Exception {
        Fixture f = new Fixture();
        DomainSecurityIFFAuthorization.grant(); f.contact(); f.oldHooks();
        check(f.stats.getSkillLevel(Skills.AUTOMATED_SHIPS) == 0f && f.stats.getPoints() == 3 && f.rewardWrites == 0,
                "Pre-acceptance calls must grant neither skill nor point");
        check(!Misc.getAllowedRecoveryTags().contains(Tags.AUTOMATED_RECOVERABLE), "Native recovery expertise must initially be absent");
        f.accepted(); f.oldHooks();
        check(f.stats.getSkillLevel(Skills.AUTOMATED_SHIPS) == 0f, "Load/timer/recovery/transfer cannot award a skill");
        int previousRefresh = f.stats.refreshes;
        DomainSecurityIFFAuthorization.grant();
        check(f.stats.getSkillLevel(Skills.AUTOMATED_SHIPS) == 1f && f.stats.getPoints() == 3,
                "Menelaus must grant vanilla Automated Ships without spending a free point");
        check(f.stats.refreshes > previousRefresh && Misc.getAllowedRecoveryTags().contains(Tags.AUTOMATED_RECOVERABLE),
                "The actual native setter must refresh and enable automated recovery");
        check(Boolean.TRUE.equals(f.saved.get(REWARD)) && f.rewardWrites == 1, "Reward must commit one permanent claim");
        f.contact(); DomainSecurityIFFAuthorization.grant(); f.oldHooks();
        check(f.stats.getPoints() == 3 && f.rewardWrites == 1, "Newly granted skill must not convert into repeat free points");
        f.stats.setSkillLevel(Skills.AUTOMATED_SHIPS, 0f); f.contact();
        check(f.stats.getSkillLevel(Skills.AUTOMATED_SHIPS) == 0f, "Respeccing cannot farm another free skill");
        f.stats.setSkillLevel(Skills.AUTOMATED_SHIPS, 1f); f.contact();
        check(f.stats.getPoints() == 3, "Reacquiring the skill cannot farm compensation");
        check(f.rosterReads == 0 && f.removals > 0, "Reward must preserve refits and retire the old recovery hook");
    }

    private static void alreadyOwnedPointOnce() throws Exception {
        for (float initial : new float[] {1f, 2f}) {
            Fixture f = new Fixture(); f.stats.setSkillLevel(Skills.AUTOMATED_SHIPS, initial); f.accepted(); f.contact();
            check(f.stats.getSkillLevel(Skills.AUTOMATED_SHIPS) == initial && f.stats.getPoints() == 4,
                    "Existing owners must retain their skill level and receive exactly one point");
            f.stats.addPoints(-1); DomainSecurityIFFAuthorization.grant(); f.contact(); f.oldHooks();
            check(f.stats.getPoints() == 3 && f.rewardWrites == 1, "Spending compensation cannot enable another reward");
            f.stats.setSkillLevel(Skills.AUTOMATED_SHIPS, 0f); f.contact();
            check(f.stats.getSkillLevel(Skills.AUTOMATED_SHIPS) == 0f, "Compensated owners cannot also farm the free skill");
            Map<String, Object> savedState = new HashMap<>(f.saved);
            Fixture reloaded = new Fixture(); reloaded.saved.putAll(savedState); reloaded.stats.setSkillLevel(Skills.AUTOMATED_SHIPS, initial);
            reloaded.contact();
            check(reloaded.stats.getPoints() == 3 && reloaded.rewardWrites == 0, "Persisted claim must prevent compensation after reload");
        }
    }

    private static void explicitContactAndUnavailableStats() throws Exception {
        Fixture f = new Fixture(); f.accepted();
        check(new ChiefNavigatorMenelausCMD().execute("reward_regression", null,
                List.of(new Token("init", TokenType.LITERAL)), Map.of()), "Null-dialog query remains valid");
        check(f.stats.getSkillLevel(Skills.AUTOMATED_SHIPS) == 0f, "Null-dialog queries cannot claim the reward");
        f.statsAvailable = false; DomainSecurityIFFAuthorization.grant();
        check(f.rewardWrites == 0, "Missing player stats must not consume the reward");
        f.statsAvailable = true; f.contact();
        check(f.stats.getSkillLevel(Skills.AUTOMATED_SHIPS) == 1f && f.stats.getPoints() == 3,
                "Accepted existing saves must receive their skill on explicit contact");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void main(String[] args) throws Exception {
        SettingsAPI oldSettings = Global.getSettings(); SectorAPI oldSector = Global.getSector();
        Field engineSingleton = CampaignEngine.class.getDeclaredField("instance");
        engineSingleton.setAccessible(true);
        CampaignEngine oldEngine = (CampaignEngine) engineSingleton.get(null);
        Map<Field, Object> nativeSettings = new HashMap<>();
        Class<?> store = Class.forName("com.fs.starfarer.loading.SpecStore");
        Map<String, Object> specs = (Map<String, Object>) store.getMethod("new", Class.class).invoke(null, SkillSpec.class);
        Map<String, Object> oldSpecs = new HashMap<>(specs);
        Map<String, Class<?>> scripts = (Map<String, Class<?>>) Class.forName("com.fs.starfarer.loading.scripts.ScriptStore").getField("void").get(null);
        Map<String, Class<?>> oldScripts = new HashMap<>(scripts);
        Map oldCache = new HashMap(CharacterStats.getEffectCache());
        try {
            JSONObject settings = new JSONObject().put("baseMaxAdmins", 2).put("baseMaxOutposts", 4)
                    .put("startingCommandPoints", 5).put("baseNumOfficers", 8);
            for (Field field : Class.forName("com.fs.starfarer.settings.StarfarerSettings").getDeclaredFields()) {
                if (field.getType() == JSONObject.class && Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true); nativeSettings.put(field, field.get(null)); field.set(null, settings);
                }
            }
            Path modRoot = (Files.isDirectory(Path.of("data/hulls")) ? Path.of("") : Path.of("Chief Navigator")).toAbsolutePath().normalize();
            JSONObject definition = new JSONObject(Files.readString(modRoot.getParent().getParent()
                    .resolve("starsector-core/data/characters/skills/automated_ships.skill")));
            JSONArray groups = definition.getJSONArray("effectGroups");
            for (int group = 0; group < groups.length(); group++) {
                JSONArray effects = groups.getJSONObject(group).getJSONArray("effects");
                for (int effect = 0; effect < effects.length(); effect++) {
                    String script = effects.getJSONObject(effect).getString("script"); scripts.put(script, Class.forName(script));
                }
            }
            SkillSpec skill = new SkillSpec(definition);
            specs.clear(); specs.put(Skills.AUTOMATED_SHIPS, skill); CharacterStats.getEffectCache().clear();
            Global.setSettings(mock(SettingsAPI.class, (name, values) -> name.equals("getSkillSpec") ? skill : null));
            freeSkillAndNativeEffects(); alreadyOwnedPointOnce(); explicitContactAndUnavailableStats();
            Global.setSector(null); DomainSecurityIFFAuthorization.grant(); DomainSecurityIFFAuthorization.install();
            System.out.println("PASS: native Automated Ships grants and refreshes for free; prior owners gain one point; claims cannot repeat through contacts, respec, recovery, transfers, load, timers or reload; player refits remain untouched.");
        } finally {
            Global.setSector(oldSector); Global.setSettings(oldSettings); engine(oldEngine);
            for (Map.Entry<Field, Object> entry : nativeSettings.entrySet()) entry.getKey().set(null, entry.getValue());
            specs.clear(); specs.putAll(oldSpecs); scripts.clear(); scripts.putAll(oldScripts);
            CharacterStats.getEffectCache().clear(); ((Map) CharacterStats.getEffectCache()).putAll(oldCache);
        }
    }
}
