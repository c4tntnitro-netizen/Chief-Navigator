import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import org.histidine.chatter.combat.ChatterCombatPlugin;

/** Contract for Gautama's ordinary-battle reconstruction and authored opt-outs. */
public class GautamaConstructionRegression {
    private static final String ORDINARY_WARNING =
            "WARNING: Reincarnation\n\nRetreat Advised.";
    private static final String FINAL_WARNING =
            "WARNING: Multiple Reincarnations Detected";

    /** Records the real native setup object without initializing a GL drawer. */
    private static final class RecordingChatter extends ChatterCombatPlugin {
        final List<String> names = new ArrayList<>();
        boolean failNext;

        @Override
        public void generateFleetIntro(FleetIntroSetupData warning) {
            WallResearchRegression.check(!warning.hasStatic && !warning.factionFirstTime
                            && warning.fleet == null && warning.image == null && warning.sound == null
                            && "threat".equals(warning.factionId)
                            && warning.flagshipId == null,
                    "Warnings use smooth native setup without any global hull-level static fallback");
            names.add(warning.name);
            if (failNext) {
                failNext = false;
                throw new IllegalStateException("Injected presentation-only Chatter failure");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Class<?> type = Class.forName("chiefnavigator.hullmods.GautamaReincarnation");
        Method canBegin = type.getDeclaredMethod("canBegin", CombatEngineAPI.class);
        Method death = type.getDeclaredMethod("noteGautamaDeath", CombatEngineAPI.class, ShipAPI.class);
        Method enableFinal = type.getDeclaredMethod(
                "enableFinalLaborMode", CombatEngineAPI.class);
        Method suppress = type.getDeclaredMethod(
                "suppressForBattle", CombatEngineAPI.class);
        Method pity = type.getDeclaredMethod(
                "getFinalLaborPityChanceForKills", int.class);
        Method finalChance = type.getDeclaredMethod(
                "getFinalLaborReincarnationChance", float.class, int.class);
        Method pityKills = type.getDeclaredMethod(
                "getFinalLaborPityKills", CombatEngineAPI.class);
        Method recordPity = type.getDeclaredMethod(
                "recordFinalLaborPlayerKill",
                CombatEngineAPI.class, ShipAPI.class);
        Method music = type.getDeclaredMethod(
                "shouldStartReincarnationMusic", boolean.class, boolean.class);
        Method chance = Class.forName(
                "chiefnavigator.hullmods.StarvingThreatHullmod")
                .getDeclaredMethod(
                        "getGautamaReincarnationChance",
                        ShipAPI.HullSize.class,
                        String.class);
        canBegin.setAccessible(true); death.setAccessible(true);
        enableFinal.setAccessible(true); suppress.setAccessible(true);
        pity.setAccessible(true); finalChance.setAccessible(true);
        pityKills.setAccessible(true); recordPity.setAccessible(true);
        music.setAccessible(true);
        chance.setAccessible(true);
        Map<String,Object> data = new HashMap<>();
        List<ShipAPI> ships = new ArrayList<>();
        CombatEngineAPI engine = WallResearchRegression.mock(CombatEngineAPI.class, (n,a) -> {
            if (n.equals("getCustomData")) return data;
            if (n.equals("getShips")) return ships;
            return null;
        });
        ShipHullSpecAPI hull = WallResearchRegression.mock(ShipHullSpecAPI.class,
                (n,a) -> n.equals("getHullId") ? "chief_navigator_scylla" : null);
        ShipAPI gautama = WallResearchRegression.mock(ShipAPI.class, (n,a) -> {
            if (n.equals("getHullSpec")) return hull;
            if (n.equals("isAlive")) return true;
            return null;
        });
        WallResearchRegression.check((Boolean) canBegin.invoke(null, engine),
                "An ordinary battle without Gautama must permit one construction");
        WallResearchRegression.check(Math.abs((Float) chance.invoke(
                        null, ShipAPI.HullSize.FRIGATE,
                        "chief_navigator_starving_skirmish_unit") - 0.10f)
                        < 0.0001f,
                "Starving Threat frigates must use a 10% roll");
        WallResearchRegression.check(Math.abs((Float) chance.invoke(
                        null, ShipAPI.HullSize.DESTROYER,
                        "chief_navigator_starving_assault_unit") - 0.20f)
                        < 0.0001f,
                "Starving Threat destroyers must use a 20% roll");
        WallResearchRegression.check(Math.abs((Float) chance.invoke(
                        null, ShipAPI.HullSize.CRUISER,
                        "chief_navigator_starving_hive_unit") - 0.30f)
                        < 0.0001f,
                "Starving Threat cruisers must use a 30% roll");
        WallResearchRegression.check(Math.abs((Float) chance.invoke(
                        null, ShipAPI.HullSize.CAPITAL_SHIP,
                        "chief_navigator_starving_fabricator_unit") - 0.50f)
                        < 0.0001f,
                "Starving Threat Fabricators must use a 50% roll");
        WallResearchRegression.check(Math.abs((Float) chance.invoke(
                        null, ShipAPI.HullSize.CAPITAL_SHIP,
                        "unrecognized_starving_capital")) < 0.0001f,
                "Unspecified capital hulls must not inherit the Fabricator roll");
        WallResearchRegression.check(Math.abs((Float) chance.invoke(
                        null, ShipAPI.HullSize.FIGHTER,
                        "unrecognized_starving_fighter")) < 0.0001f,
                "Unspecified fighter hulls must not receive a reconstruction roll");
        ships.add(gautama);
        WallResearchRegression.check(!(Boolean) canBegin.invoke(null, engine),
                "A live Gautama must not enable reconstruction");
        death.invoke(null, engine, gautama);
        ships.clear();
        WallResearchRegression.check(!(Boolean) canBegin.invoke(null, engine),
                "A battle that already contained Gautama must not construct a second one");
        Map<String,Object> suppressedData = new HashMap<>();
        CombatEngineAPI suppressedEngine = WallResearchRegression.mock(
                CombatEngineAPI.class, (n,a) -> {
                    if (n.equals("getCustomData")) return suppressedData;
                    if (n.equals("getShips")) return Collections.emptyList();
                    return null;
                });
        suppress.invoke(null, suppressedEngine);
        WallResearchRegression.check(
                !(Boolean) canBegin.invoke(null, suppressedEngine),
                "Explicit suppression must keep reconstruction disabled");
        Map<String,Object> finalData = new HashMap<>();
        List<ShipAPI> finalShips = new ArrayList<>();
        CombatEngineAPI finalEngine = WallResearchRegression.mock(
                CombatEngineAPI.class, (n,a) -> {
                    if (n.equals("getCustomData")) return finalData;
                    if (n.equals("getShips")) return finalShips;
                    return null;
                });
        enableFinal.invoke(null, finalEngine);
        WallResearchRegression.check(
                (Boolean) canBegin.invoke(null, finalEngine),
                "Labor V must enable its repeatable reconstruction cycle");
        finalShips.add(gautama);
        WallResearchRegression.check(
                !(Boolean) canBegin.invoke(null, finalEngine),
                "Labor V must not construct a second live Gautama");
        death.invoke(null, finalEngine, gautama);
        finalShips.clear();
        WallResearchRegression.check(
                (Boolean) canBegin.invoke(null, finalEngine),
                "A Labor V Gautama death must open a fresh cycle");

        ShipHullSpecAPI frigateHull = WallResearchRegression.mock(
                ShipHullSpecAPI.class, (n,a) -> n.equals("getHullId")
                        ? "chief_navigator_starving_skirmish_unit" : null);
        ShipAPI frigate = WallResearchRegression.mock(ShipAPI.class, (n,a) -> {
            if (n.equals("getHullSpec")) return frigateHull;
            if (n.equals("getHullSize")) return ShipAPI.HullSize.FRIGATE;
            return null;
        });
        WallResearchRegression.check(
                (Boolean) recordPity.invoke(null, finalEngine, frigate)
                        && (Integer) pityKills.invoke(null, finalEngine) == 1,
                "A failed eligible Labor V death must add one pity step");
        MemoryAPI permanentMemory = WallResearchRegression.mock(
                MemoryAPI.class, (n,a) -> n.equals("getBoolean")
                        && "$chief_navigator_gautama_final_rematch_defeated_v1"
                                .equals(a[0]));
        SectorAPI permanentSector = WallResearchRegression.mock(
                SectorAPI.class, (n,a) -> n.equals("getMemoryWithoutUpdate")
                        ? permanentMemory : null);
        SectorAPI previousSector = Global.getSector();
        Global.setSector(permanentSector);
        try {
            Map<String,Object> postFinalData = new HashMap<>();
            CombatEngineAPI postFinalEngine = WallResearchRegression.mock(
                    CombatEngineAPI.class, (n,a) -> {
                        if (n.equals("getCustomData")) return postFinalData;
                        if (n.equals("getShips")) return Collections.emptyList();
                        return null;
                    });
            WallResearchRegression.check(
                    !(Boolean) canBegin.invoke(null, postFinalEngine),
                    "Permanent final-rematch defeat must block Gautama in later battles");
        } finally {
            Global.setSector(previousSector);
        }
        WallResearchRegression.check(Math.abs(
                (Float) pity.invoke(null, 0)) < 0.0001f,
                "The first Labor V roll must use only its hull-size base");
        WallResearchRegression.check(Math.abs(
                (Float) pity.invoke(null, 4) - 0.40f) < 0.0001f,
                "Four prior failed rolls must add forty percentage points");
        WallResearchRegression.check(Math.abs(
                (Float) pity.invoke(null, 9) - 0.90f) < 0.0001f,
                "Nine prior failed rolls must add ninety percentage points");
        WallResearchRegression.check(Math.abs(
                (Float) pity.invoke(null, 10) - 1f) < 0.0001f
                        && Math.abs((Float) pity.invoke(null, 99) - 1f)
                        < 0.0001f,
                "Labor V pity must cap at one hundred percent");
        WallResearchRegression.check(Math.abs(
                (Float) finalChance.invoke(null, 0.10f, 4) - 0.50f)
                        < 0.0001f
                        && Math.abs((Float) finalChance.invoke(
                                null, 0.50f, 5) - 1f) < 0.0001f,
                "Labor V must add prior-failure pity to each hull-size base");
        WallResearchRegression.check(
                (Boolean) music.invoke(null, false, false),
                "Ordinary reconstruction must start its authored battle cue");
        WallResearchRegression.check(
                !(Boolean) music.invoke(null, true, false),
                "Final-Labor state must protect Gospel is Gunpowder");
        WallResearchRegression.check(
                !(Boolean) music.invoke(null, false, true),
                "A live Gospel cue must not be replaced");
        verifyWarningSetupAndLifetime(type, gautama);
        List<FleetMemberAPI> roster = new ArrayList<>();
        FleetMemberAPI invalidGautama = WallResearchRegression.mock(
                FleetMemberAPI.class, (n,a) -> n.equals("getHullId")
                        ? "chief_navigator_scylla" : null);
        FleetMemberAPI ordinary = WallResearchRegression.mock(
                FleetMemberAPI.class, (n,a) -> n.equals("getHullId")
                        ? "chief_navigator_starving_hive_unit" : null);
        roster.add(invalidGautama);
        roster.add(ordinary);
        FleetMemberAPI[] flagship = {invalidGautama};
        FleetDataAPI fleetData = WallResearchRegression.mock(
                FleetDataAPI.class, (n,a) -> {
                    if (n.equals("getMembersListCopy")) {
                        return new ArrayList<FleetMemberAPI>(roster);
                    }
                    if (n.equals("removeFleetMember")) roster.remove(a[0]);
                    if (n.equals("setFlagship")) {
                        flagship[0] = (FleetMemberAPI) a[0];
                    }
                    return null;
                });
        CampaignFleetAPI contaminated = WallResearchRegression.mock(
                CampaignFleetAPI.class, (n,a) -> {
                    if (n.equals("getFleetData")) return fleetData;
                    if (n.equals("getId")) return "legacy_perimeter";
                    return null;
                });
        Method scrub = Class.forName(
                "chiefnavigator.quest.OdysseyPredatorScript")
                .getDeclaredMethod(
                        "removeGautamaFromPerimeterInvasion",
                        CampaignFleetAPI.class);
        scrub.setAccessible(true);
        WallResearchRegression.check(
                (Integer) scrub.invoke(null, contaminated) == 1,
                "Contaminated perimeter roster must remove Gautama");
        WallResearchRegression.check(roster.size() == 1
                        && roster.get(0) == ordinary
                        && flagship[0] == ordinary,
                "Perimeter roster must retain and promote an ordinary ship");
        String starving = Files.readString(Paths.get(
                "src/chiefnavigator/hullmods/StarvingThreatHullmod.java"),
                StandardCharsets.UTF_8);
        WallResearchRegression.check(
                starving.contains(
                        "GautamaReincarnation.attemptReincarnation("),
                "Starving Threat death handling must invoke Gautama's scoped roll");
        String finalLabor = Files.readString(Paths.get(
                "src/chiefnavigator/quest/"
                        + "IthacaFinalLaborBattleCreationPlugin.java"),
                StandardCharsets.UTF_8);
        WallResearchRegression.check(
                finalLabor.contains(
                        "GautamaReincarnation.enableFinalLaborMode(engine)")
                        && !finalLabor.contains(
                                "GautamaReincarnation.suppressForBattle(engine)"),
                "Labor V center combat must enable, not suppress, pity cycles");
        String rematch = Files.readString(Paths.get(
                "src/chiefnavigator/quest/"
                        + "GautamaFinalRematchBattleCreationPlugin.java"),
                StandardCharsets.UTF_8);
        WallResearchRegression.check(
                rematch.contains("GautamaReincarnation.suppressForBattle(engine)"),
                "The permanent Last Light rematch must explicitly suppress reincarnation");
        String reincarnation = Files.readString(Paths.get(
                "src/chiefnavigator/hullmods/GautamaReincarnation.java"),
                StandardCharsets.UTF_8);
        WallResearchRegression.check(
                reincarnation.contains(
                        "OdysseyPredatorScript.isGautamaFinalRematchDefeated()")
                        && reincarnation.contains("cleanupFailedConstruction(")
                        && reincarnation.contains("engine.removeEntity(ship)")
                        && reincarnation.contains("state.gautama = null")
                        && reincarnation.contains("state.pending = false"),
                "Permanent defeat and failed partial construction must leave no recreation path or stuck state");
        String modInfo = Files.readString(Paths.get("mod_info.json"),
                StandardCharsets.UTF_8);
        String build = Files.readString(Paths.get("build.ps1"),
                StandardCharsets.UTF_8);
        WallResearchRegression.check(
                modInfo.contains("\"id\": \"chatter\"")
                        && build.contains("CombatChatter.jar")
                        && reincarnation.contains(
                                "FleetIntroSetupData warning")
                        && reincarnation.contains(
                                "WARNING: Reincarnation\\n\\nRetreat Advised.")
                        && reincarnation.contains(
                                "WARNING: Multiple Reincarnations Detected")
                        && reincarnation.indexOf("showReincarnationWarning(engine, constructedGautama)")
                                > reincarnation.indexOf("engine.addPlugin(swarmDeployment)"),
                "Combat Chatter warnings require the exact ordinary/final text and a successfully installed build");
        String reincarnatingSkin = Files.readString(Paths.get(
                "data/hulls/skins/chief_navigator_scylla_reincarnating.skin"),
                StandardCharsets.UTF_8);
        String reincarnatingVariant = Files.readString(Paths.get(
                "data/variants/"
                        + "chief_navigator_scylla_reincarnating_Fabricator.variant"),
                StandardCharsets.UTF_8);
        WallResearchRegression.check(
                reincarnatingSkin.contains("\"fighterBays\": 0")
                        && !reincarnatingSkin.contains("builtInWings")
                        && reincarnatingVariant.contains(
                                "\"hullId\": \"chief_navigator_scylla_reincarnating\"")
                        && reincarnation.contains(
                                "REINCARNATION_SWARM_COUNT = 20")
                        && reincarnation.contains(
                                "REINCARNATION_SWARM_INTERVAL = 1.25f")
                        && reincarnation.contains(
                                "new ReincarnationSwarmDeployment("),
                "Reconstructed Gautama must stage 20 swarms over time instead of owning 20 launch-ready bays");
        System.out.println("PASS: ordinary Gautama construction retains its "
                + "one-incarnation size rolls; Labor V repeats with +10-point "
                + "failed-roll pity and stages its 20-swarm screen, while the "
                + "permanent rematch is suppressed and ordinary patrols flash "
                + "the smooth two-line retreat warning; Labor V warns once "
                + "at its first successful reincarnation.");
    }

    private static void verifyWarningSetupAndLifetime(Class<?> type, ShipAPI gautama)
            throws Exception {
        Method warning = type.getDeclaredMethod(
                "showReincarnationWarning", CombatEngineAPI.class, ShipAPI.class);
        Method getState = type.getDeclaredMethod("getState", CombatEngineAPI.class);
        Method enable = type.getDeclaredMethod("enableFinalLaborMode", CombatEngineAPI.class);
        Method suppress = type.getDeclaredMethod("suppressForBattle", CombatEngineAPI.class);
        warning.setAccessible(true);
        getState.setAccessible(true);
        CombatEngineAPI previous = Global.getCombatEngine();
        try {
            for (boolean finalLabor : new boolean[] {false, true}) {
                Map<String, Object> data = new HashMap<>();
                RecordingChatter chatter = new RecordingChatter();
                data.put(ChatterCombatPlugin.DATA_KEY, chatter);
                CombatEngineAPI engine = WallResearchRegression.mock(CombatEngineAPI.class,
                        (name, args) -> name.equals("getCustomData") ? data
                                : name.equals("getShips") ? Collections.emptyList() : null);
                Global.setCombatEngine(engine);
                if (finalLabor) enable.invoke(null, engine);
                Object state = getState.invoke(null, engine);
                Field tracked = state.getClass().getDeclaredField("gautama");
                Field pending = state.getClass().getDeclaredField("pending");
                tracked.setAccessible(true);
                pending.setAccessible(true);
                warning.invoke(null, engine, gautama);
                WallResearchRegression.check(chatter.names.isEmpty(),
                        "An untracked/failed construction must not display or consume a warning");
                tracked.set(state, gautama);
                pending.setBoolean(state, true);
                warning.invoke(null, engine, gautama);
                WallResearchRegression.check(chatter.names.isEmpty(),
                        "A still-pending construction must not display or consume a warning");
                pending.setBoolean(state, false);
                warning.invoke(null, engine, gautama);
                warning.invoke(null, engine, gautama);
                WallResearchRegression.check(chatter.names.size() == 1
                                && chatter.names.get(0).equals(finalLabor ? FINAL_WARNING : ORDINARY_WARNING),
                        "The first completed setup emits exact native text once in its proper mode");
                if (finalLabor) {
                    enable.invoke(null, engine);
                    tracked.set(state, WallResearchRegression.mock(ShipAPI.class, (name, args) -> null));
                    warning.invoke(null, engine, tracked.get(state));
                    WallResearchRegression.check(chatter.names.size() == 1,
                            "A later Labor V cycle or repeated enable must not re-arm the battle warning");
                }
            }

            Map<String, Object> data = new HashMap<>();
            RecordingChatter chatter = new RecordingChatter();
            data.put(ChatterCombatPlugin.DATA_KEY, chatter);
            CombatEngineAPI engine = WallResearchRegression.mock(CombatEngineAPI.class,
                    (name, args) -> name.equals("getCustomData") ? data
                            : name.equals("getShips") ? Collections.emptyList() : null);
            Global.setCombatEngine(engine);
            enable.invoke(null, engine);
            Object state = getState.invoke(null, engine);
            Field tracked = state.getClass().getDeclaredField("gautama");
            tracked.setAccessible(true);
            tracked.set(state, gautama);
            suppress.invoke(null, engine);
            warning.invoke(null, engine, gautama);
            WallResearchRegression.check(chatter.names.isEmpty(),
                    "Ungaikyo/perimeter suppression dominates either warning mode");
            enable.invoke(null, engine);
            chatter.failNext = true;
            warning.invoke(null, engine, gautama);
            warning.invoke(null, engine, gautama);
            WallResearchRegression.check(chatter.names.size() == 1 && tracked.get(state) == gautama,
                    "A Chatter exception must not unwind the constructed ship or create a repeated alert");
        } finally {
            Global.setCombatEngine(previous);
        }
    }
}
