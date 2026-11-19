package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.StarTypes;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import java.lang.reflect.Proxy;
import static chiefnavigator.quest.SinniSystemVignetteScript.Category.*;
import static chiefnavigator.quest.SinniVignetteCompletionRegression.check;

/** Actual watcher/census coverage for choosing one unfinished overlapping category. */
public final class SinniVignetteOverlapRegression {
    static StarSystemAPI cloudedGiant() {
        return SinniSystemVignetteRegression.system("overlap_giant",
                StarTypes.BLUE_GIANT, true, false, false,
                StarSystemGenerator.StarSystemType.SINGLE);
    }

    static SinniVignetteCompletionRegression.Fixture enter(StarSystemAPI system) {
        SinniVignetteCompletionRegression.Fixture f =
                new SinniVignetteCompletionRegression.Fixture();
        StarSystemAPI local = (StarSystemAPI) Proxy.newProxyInstance(
                StarSystemAPI.class.getClassLoader(), new Class<?>[] {StarSystemAPI.class},
                (proxy, method, args) -> method.getName().equals("getFleets")
                        ? java.util.Collections.emptyList() : method.invoke(system, args));
        f.systems.add(local);
        f.currentSystem = local;
        SinniSystemVignetteScript.invalidateSightseeingCache();
        return f;
    }

    static void checkOneAtATime() {
        SinniVignetteCompletionRegression.Fixture f = enter(cloudedGiant());
        check(SinniSystemVignetteScript.getRequiredSightseeingCount() == 2,
                "Clouded giant must contribute both Giant and Nebula to the chart");
        f.ticks(3);
        check(f.scenes.size() == 1 && f.scenes.get(0).category == GIANT_STAR
                        && "massive_star".equals(f.scenes.get(0).sceneId),
                "First unfinished match must retain the blue giant's authored scene");
        check(SinniSystemVignetteScript.getCompletedSightseeingCount() == 0,
                "Opening a scene must not consume any matching category");
        f.ticks(10);
        check(f.scenes.size() == 1, "Only one scene may be open at a time");
        SinniSystemVignetteScript.markComplete(f.scenes.get(0).category);
        check(SinniSystemVignetteScript.isComplete(GIANT_STAR)
                        && !SinniSystemVignetteScript.isComplete(NEBULA)
                        && !SinniSystemVignetteScript.isAllTypesCompletionPending(),
                "Closing Giant must leave Nebula unfinished and the finale unqueued");
        // A reload keeps only the committed category and offers the other match.
        f.watcher = new SinniSystemVignetteScript();
        SinniSystemVignetteScript.invalidateSightseeingCache();
        f.dialogShowing = false;
        f.ticks(3);
        check(f.scenes.size() == 2 && f.scenes.get(1).category == NEBULA,
                "A completed Giant must fall through to unfinished Nebula after reload");
        SinniSystemVignetteScript.markComplete(f.scenes.get(1).category);
        check(SinniSystemVignetteScript.getCompletedSightseeingCount() == 2
                        && SinniSystemVignetteScript.getRequiredSightseeingCount() == 2
                        && SinniSystemVignetteScript.isAllTypesCompletionPending(),
                "Completing the second match must finish both stable chart slots");
        f.dialogShowing = false;
        int readsBeforeIdle = f.systemReads;
        f.ticks(1000);
        check(f.scenes.size() == 2 && f.systemReads == readsBeforeIdle,
                "Completed overlaps must not repeat scenes or recensus on idle ticks");
    }

    static void checkOtherMatchAlreadyDone() {
        SinniVignetteCompletionRegression.Fixture f = enter(cloudedGiant());
        f.earned(NEBULA);
        f.ticks(3);
        check(f.scenes.size() == 1 && f.scenes.get(0).category == GIANT_STAR,
                "A completed Nebula must leave the unfinished Giant available");
        check(!SinniSystemVignetteScript.isComplete(GIANT_STAR),
                "An interrupted Giant scene must remain unfinished");
        f.watcher = new SinniSystemVignetteScript();
        f.dialogShowing = false;
        f.ticks(3);
        check(f.scenes.size() == 2 && f.scenes.get(1).category == GIANT_STAR,
                "An interrupted scene must be offered again rather than silently completed");
    }

    static PlanetAPI star(String type) {
        return SinniSystemVignetteRegression.mock(PlanetAPI.class,
                (name, args) -> name.equals("getTypeId") ? type : null);
    }

    static void checkMixedTrinary() {
        PlanetAPI primary = star(StarTypes.BLUE_GIANT);
        PlanetAPI secondary = star(StarTypes.YELLOW);
        PlanetAPI tertiary = star(StarTypes.WHITE_DWARF);
        StarSystemAPI system = SinniSystemVignetteRegression.mock(StarSystemAPI.class,
                (name, args) -> {
                    if (name.equals("getStar")) return primary;
                    if (name.equals("getSecondary")) return secondary;
                    if (name.equals("getTertiary")) return tertiary;
                    if (name.equals("hasSystemwideNebula")) return true;
                    return null;
                });
        SinniVignetteCompletionRegression.Fixture f = enter(system);
        // Avoid reward construction here; it has its own native-API regression below.
        f.saved.put("$chief_navigator_vignette_main_sequence_reward_spawned_v1", true);
        check(SinniSystemVignetteScript.getRequiredSightseeingCount() == 5,
                "Mixed trinary must expose Trinary, Giant, Main Sequence, White Dwarf and Nebula");
        f.ticks(3);
        check(f.scenes.get(0).category == TRINARY,
                "Unseen Trinary retains first choice among matching categories");
        f.earned(TRINARY);
        f.earned(GIANT_STAR);
        f.earned(MAIN_SEQUENCE);
        f.earned(NEBULA);
        f.dialogShowing = false;
        f.ticks(1);
        check(f.scenes.size() == 2 && f.scenes.get(1).category == WHITE_DWARF,
                "Completed higher-priority matches must not hide a remaining companion-star category");
        SinniSystemVignetteScript.markComplete(WHITE_DWARF);
        check(SinniSystemVignetteScript.getRequiredSightseeingCount() == 5
                        && SinniSystemVignetteScript.isAllTypesCompletionPending(),
                "The census must stay independent of completion state");
    }

    static void checkMainSequenceRewardOverlap() throws Exception {
        SinniKiteRewardRegression.Fixture f = new SinniKiteRewardRegression.Fixture();
        StarSystemAPI original = f.home.api;
        f.home.api = (StarSystemAPI) Proxy.newProxyInstance(
                StarSystemAPI.class.getClassLoader(), new Class<?>[] {StarSystemAPI.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getSecondary")
                            || method.getName().equals("getTertiary")) return f.home.star;
                    return method.invoke(original, args);
                });
        f.player.location = f.home.api;
        SinniSystemVignetteScript.invalidateSightseeingCache();
        check(SinniSystemVignetteScript.getRequiredSightseeingCount() == 2,
                "Three main-sequence stars must count Main Sequence once alongside Trinary");
        f.saved.values.remove(f.spawned);
        f.saved.values.put("$chief_navigator_sinni_vignette_complete_main_sequence", true);
        f.tick();
        check(Boolean.TRUE.equals(f.saved.values.get(f.spawned))
                        && Boolean.TRUE.equals(f.saved.values.get(f.invalid))
                        && f.factoryCalls == 0,
                "Main-sequence reward must recognize a trinary match and preserve its existing-claim guard");
    }

    static void checkNebulaCenters() {
        enter(SinniSystemVignetteRegression.system("native_nebula",
                "nebula_center_average", true, false, false,
                StarSystemGenerator.StarSystemType.NEBULA));
        check(SinniSystemVignetteScript.getRequiredSightseeingCount() == 1,
                "A native nebula's hidden center star must not invent another category");
        SinniVignetteCompletionRegression.Fixture f = enter(
                SinniSystemVignetteRegression.system("modded_nebula",
                        StarTypes.BLUE_GIANT, true, false, false,
                        StarSystemGenerator.StarSystemType.NEBULA));
        f.earned(NEBULA);
        f.ticks(3);
        check(SinniSystemVignetteScript.getRequiredSightseeingCount() == 2
                        && f.scenes.size() == 1 && f.scenes.get(0).category == GIANT_STAR,
                "An explicit nebula with a real giant must retain its unfinished stellar match");
    }

    static void checkBespokeAndCoreIsolation() {
        SinniVignetteCompletionRegression.Fixture f = enter(
                SinniSystemVignetteRegression.system(OdysseyExpanseSystem.SILENT_WAKE_ID,
                        StarTypes.WHITE_DWARF, true, false, false));
        f.earned(SANZU);
        f.ticks(20);
        check(f.scenes.isEmpty() && SinniSystemVignetteScript.getRequiredSightseeingCount() == 0,
                "Completed authored Sanzu must not fall through to generic overlapping categories");
        f = enter(SinniSystemVignetteRegression.taggedSystem(
                com.fs.starfarer.api.impl.campaign.ids.Tags.THEME_CORE, StarTypes.BLUE_GIANT));
        f.ticks(20);
        check(f.scenes.isEmpty() && SinniSystemVignetteScript.getRequiredSightseeingCount() == 0,
                "Core exclusions must remain in force for remaining categories");
    }

    public static void main(String[] args) throws Exception {
        SettingsAPI settings = Global.getSettings();
        SectorAPI sector = Global.getSector();
        com.fs.starfarer.api.FactoryAPI factory = Global.getFactory();
        try {
            checkOneAtATime();
            checkOtherMatchAlreadyDone();
            checkMixedTrinary();
            checkNebulaCenters();
            checkBespokeAndCoreIsolation();
            checkMainSequenceRewardOverlap();
            System.out.println("Overlapping category selection, committed progress, census, reload and reward checks passed.");
        } finally {
            Global.setSettings(settings);
            Global.setSector(sector);
            Global.setFactory(factory);
            SinniSystemVignetteScript.invalidateSightseeingCache();
        }
    }
}
