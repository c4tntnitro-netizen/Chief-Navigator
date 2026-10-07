package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.histidine.chatter.ChatterDataManager;
import org.histidine.chatter.ChatterDataManager.FactionFirstEncounterSplashDef;
import org.histidine.chatter.combat.ChatterCombatDrawer;
import org.histidine.chatter.combat.ChatterCombatPlugin;
import org.magiclib.util.MagicSettings;
import org.json.JSONArray;
import org.json.JSONObject;

/** Executes the installed Chatter intro reader/drawer setup without rendering GL. */
public final class EncounterCombatChatterRegression {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == float.class) return 0f;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0d;
        return null;
    }

    private static <T> T mock(Class<T> type, BiFunction<String, Object[], Object> body) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> {
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == args[0];
                    if (method.getName().equals("toString")) return type.getSimpleName();
                    Object result = body.apply(method.getName(), args);
                    return result == null ? defaultValue(method.getReturnType()) : result;
                }));
    }

    private static final class Memory {
        final Map<String, Object> data = new HashMap<>();
        int writes;
        final MemoryAPI api = mock(MemoryAPI.class, (name, args) -> {
            switch (name) {
                case "get", "getString": return data.get(args[0]);
                case "contains": return data.containsKey(args[0]);
                case "getBoolean": return Boolean.TRUE.equals(data.get(args[0]));
                case "getFloat": return data.containsKey(args[0])
                        ? ((Number) data.get(args[0])).floatValue() : 0f;
                case "set":
                    check(args.length == 2, "Encounter intros must use durable native memory keys");
                    data.put((String) args[0], args[1]); writes++; break;
                case "unset": data.remove(args[0]); writes++; break;
            }
            return null;
        });
    }

    private static final class Fleet {
        final Memory memory = new Memory();
        final CampaignFleetAPI api;
        Fleet(String id, String hull, FactionAPI faction, float strength, boolean player) {
            FleetMemberAPI flagship = hull == null ? null : mock(FleetMemberAPI.class,
                    (name, args) -> name.equals("getHullId") ? hull : null);
            api = mock(CampaignFleetAPI.class, (name, args) -> switch (name) {
                case "getId" -> id;
                case "getName" -> "Generic campaign name";
                case "isPlayerFleet" -> player;
                case "getEffectiveStrength" -> strength;
                case "getFlagship" -> flagship;
                case "getFaction" -> faction;
                case "getMemoryWithoutUpdate" -> memory.api;
                default -> null;
            });
        }
    }

    private static final class NativeReader extends ChatterCombatPlugin {
        NativeReader() { drawer = new ChatterCombatDrawer(this); }
        FleetIntroSetupData read(CampaignFleetAPI... enemies) {
            BattleAPI battle = mock(BattleAPI.class, (name, args) ->
                    name.equals("getNonPlayerSide") ? List.of(enemies) : null);
            return getFleetIntroDataFromBattle(battle);
        }
        ChatterCombatDrawer.FleetIntro makeNamedIntro(
                CampaignFleetAPI foreign, CampaignFleetAPI named, String label) {
            FleetIntroSetupData data = read(foreign, named);
            check(data != null && data.fleet == named && label.equals(data.name)
                            && !data.hasStatic && !data.factionFirstTime,
                    "The real reader must prefer " + label + " over a stronger generic/faction fleet");
            generateFleetIntro(data);
            return drawer.intro;
        }
    }

    private static String methodBody(String source, String declaration) {
        int declarationStart = source.indexOf(declaration);
        check(declarationStart >= 0, "Missing encounter wiring method " + declaration);
        int start = source.indexOf('{', declarationStart);
        int depth = 1;
        for (int end = start + 1; end < source.length(); end++) {
            char character = source.charAt(end);
            if (character == '{') depth++;
            if (character == '}' && --depth == 0) {
                return source.substring(start + 1, end).replaceAll("\\s+", " ");
            }
        }
        throw new AssertionError("Unclosed encounter wiring method " + declaration);
    }

    private static void wiring(String source, String method, String call) {
        check(methodBody(source, method).contains(call), "Missing scoped alert: " + method);
    }

    private static void sourceWiring() throws Exception {
        Path quest = Path.of("src/chiefnavigator/quest");
        String predator = Files.readString(quest.resolve("OdysseyPredatorScript.java"));
        String troy = Files.readString(quest.resolve("TroyArrivalScript.java"));
        String stranded = Files.readString(quest.resolve("OdysseyStrandedFleetsScript.java"));
        wiring(predator, "private static void configureIthacaEndgameInvasion(",
                "EncounterCombatChatter.configure(fleet, perimeterStrikeName(section));");
        wiring(predator, "private static void configureFinalLaborTfsAssault(",
                "EncounterCombatChatter.configure(invasion, perimeterStrikeName(section));");
        String strikeCreation = methodBody(predator,
                "String id, int tier, IthacaSectionEncounter.Section section)");
        check(strikeCreation.contains("if (section == null) { addStrikeRoster(fleet, tier); } else { "
                        + "EncounterCombatChatter.configure(fleet, perimeterStrikeName(section));"),
                "Ordinary strikes must remain unnamed while section-owned perimeter fleets receive alerts");
        wiring(predator, "private static void configureCharybdis(",
                "EncounterCombatChatter.configure(fleet, EncounterCombatChatter.BUDAI);");
        wiring(predator, "private static void configureFinalCenterBreach(",
                "EncounterCombatChatter.configure(fleet, EncounterCombatChatter.HEAVENLY);");
        wiring(troy, "private static void configureGautamaFinalRematch(",
                "EncounterCombatChatter.configure(fleet, EncounterCombatChatter.UNGAIKYO);");
        wiring(troy, "private static void configureGautamaAssignment(",
                "EncounterCombatChatter.configure(fleet, EncounterCombatChatter.HEAVENLY);");
        wiring(stranded, "private static void configureHegemonyCamp(",
                "EncounterCombatChatter.configure(fleet, EncounterCombatChatter.HEGEMONY);");
        wiring(stranded, "private static void configureSilentWakeLeagueSurvivors(",
                "EncounterCombatChatter.configure(fleet, EncounterCombatChatter.LEAGUE);");
        check(!methodBody(predator, "static void configureStarvingThreatIdentity(")
                        .contains("EncounterCombatChatter.configure"),
                "Ordinary/foreign Threat identity configuration must not grant a named encounter alert");
        int calls = 0;
        try (var files = Files.walk(Path.of("src"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                Matcher matcher = Pattern.compile("EncounterCombatChatter\\.configure\\(")
                        .matcher(Files.readString(file));
                while (matcher.find()) {
                    calls++;
                    check(Set.of("OdysseyPredatorScript.java", "TroyArrivalScript.java",
                                    "OdysseyStrandedFleetsScript.java")
                                    .contains(file.getFileName().toString()),
                            "Named encounter alerts escaped their owned population paths: " + file);
                }
            }
        }
        check(calls == 9, "Expected exactly nine scoped writers for eight distinct encounter labels");
    }

    public static void main(String[] args) throws Exception {
        SettingsAPI previousSettings = Global.getSettings();
        SectorAPI previousSector = Global.getSector();
        JSONObject previousMagicSettings = MagicSettings.modSettings;
        Map<String, FactionFirstEncounterSplashDef> previousFactionIntros = null;
        try {
            Memory factionMemory = new Memory();
            FactionAPI faction = mock(FactionAPI.class, (name, values) -> switch (name) {
                case "getId" -> "regression_foreign_faction";
                case "getCrest" -> "graphics/factions/test.png";
                case "getMemoryWithoutUpdate" -> factionMemory.api;
                default -> null;
            });
            Fleet player = new Fleet("player", null, faction, 100f, false);
            Global.setSector(mock(SectorAPI.class, (name, values) -> switch (name) {
                case "getPlayerFleet" -> player.api;
                case "getFaction" -> faction;
                case "getAllFactions" -> List.of(faction);
                default -> null;
            }));
            Global.setSettings(mock(SettingsAPI.class, (name, values) ->
                    name.startsWith("getMergedSpreadsheetData") ? new JSONArray() : null));

            // Use the installed static-hull list rather than silently testing an
            // empty list: native generateFleetIntro can still override false.
            String installedSettings = Files.readString(Path.of("../Combat Chatter/data/config/modSettings.json"));
            Matcher staticList = Pattern.compile("\"flagshipsWithStatic\"\\s*:\\s*(\\[[^\\]]*\\])")
                    .matcher(installedSettings);
            check(staticList.find(), "Installed Chatter static-hull list must be inspected");
            JSONObject nameFallbacks = new JSONObject();
            JSONObject chatter = new JSONObject()
                    .put("flagshipsWithStatic", new JSONArray(staticList.group(1)))
                    .put("flagshipToNameMap", nameFallbacks)
                    .put("flagshipToLogoMap", new JSONObject())
                    .put("flagshipToSoundMap", new JSONObject())
                    .put("factionRoundels", new JSONObject())
                    .put("factionSounds", new JSONObject());
            MagicSettings.modSettings = new JSONObject().put("chatter", chatter);
            previousFactionIntros = new HashMap<>(ChatterDataManager.FACTION_FIRST_ENCOUNTER_SPLASHES);
            ChatterDataManager.FACTION_FIRST_ENCOUNTER_SPLASHES.put(faction.getId(),
                    new FactionFirstEncounterSplashDef(faction.getId(), "Generic faction intro",
                            null, null, true));

            List<String> labels = List.of(EncounterCombatChatter.SCYLLA, EncounterCombatChatter.SIREN,
                    EncounterCombatChatter.CYCLOPS, EncounterCombatChatter.HEAVENLY,
                    EncounterCombatChatter.BUDAI, EncounterCombatChatter.UNGAIKYO,
                    EncounterCombatChatter.HEGEMONY, EncounterCombatChatter.LEAGUE);
            check(new HashSet<>(labels).equals(Set.of("Scylla Strike", "Siren Strike", "Cyclops Strike",
                            "Heavenly Strike", "Budai", "Ungaikyo", "Hegemony Expeditionary Fleet",
                            "Persean League Expeditionary Fleet")) && labels.size() == 8,
                    "All eight encounter labels must be exact and unique");
            check(EncounterCombatChatter.NAME_KEY.equals("$chatter_introSplash_name")
                            && EncounterCombatChatter.STATIC_KEY.equals("$chatter_introSplash_static"),
                    "Alert keys must exactly match the installed native Chatter reader");
            NativeReader reader = new NativeReader();
            for (String label : labels) {
                String hull = label.equals("Budai") ? "chief_navigator_charybdis"
                        : label.equals("Ungaikyo") ? "chief_navigator_ungaikyo"
                        : label.equals("Heavenly Strike") ? "chief_navigator_scylla"
                        : label.endsWith(" Strike") ? "chief_navigator_starving_fabricator"
                        : label.startsWith("Hegemony ") ? "legion_xiv" : "pegasus";
                nameFallbacks.put(hull, "Generic flagship intro");
                Fleet named = new Fleet("owned-" + label, hull, faction, 100f, false);
                Fleet foreign = new Fleet("unrelated", null, faction, 500f, false);
                factionMemory.data.clear();
                EncounterCombatChatter.configure(named.api, label);
                check(named.memory.writes == 2 && named.memory.data.size() == 2
                                && Boolean.FALSE.equals(named.memory.data.get(EncounterCombatChatter.STATIC_KEY)),
                        "Named alerts must write only their two smooth native settings");
                ChatterCombatDrawer.FleetIntro intro = reader.makeNamedIntro(foreign.api, named.api, label);
                check(label.toUpperCase(Locale.ROOT).equals(intro.name) && !intro.hasStatic,
                        "The actual installed drawer setup must retain the named smooth alert");
                check(foreign.memory.writes == 0 && foreign.memory.data.isEmpty()
                                && player.memory.writes == 0,
                        "Reading named encounters must not write foreign or player fleet memory");
            }
            Fleet flaggedPlayer = new Fleet("other-player-token", null, faction, 100f, true);
            Fleet nullName = new Fleet("null-name", null, faction, 100f, false);
            EncounterCombatChatter.configure(null, EncounterCombatChatter.BUDAI);
            EncounterCombatChatter.configure(player.api, EncounterCombatChatter.BUDAI);
            EncounterCombatChatter.configure(flaggedPlayer.api, EncounterCombatChatter.BUDAI);
            EncounterCombatChatter.configure(nullName.api, null);
            check(player.memory.writes == 0 && flaggedPlayer.memory.writes == 0
                            && nullName.memory.writes == 0,
                    "Null names and both player-identity guards must reject alert writes");
            sourceWiring();
            System.out.println("PASS: eight exact encounter labels, installed native Chatter priority/"
                    + "smooth drawer setup, player/foreign fleet safety and scoped population wiring.");
        } finally {
            if (previousFactionIntros != null) {
                ChatterDataManager.FACTION_FIRST_ENCOUNTER_SPLASHES.clear();
                ChatterDataManager.FACTION_FIRST_ENCOUNTER_SPLASHES.putAll(previousFactionIntros);
            }
            MagicSettings.modSettings = previousMagicSettings;
            Global.setSettings(previousSettings);
            Global.setSector(previousSector);
        }
    }
}
