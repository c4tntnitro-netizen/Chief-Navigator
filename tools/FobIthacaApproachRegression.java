package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.VisualPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.campaign.rules.RuleAPI;
import com.fs.starfarer.api.campaign.rules.RulesAPI;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/** Headless checks for first-contact routing and additive completion state. */
public final class FobIthacaApproachRegression {
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
                    if (result == float.class) return 0f;
                    if (result == int.class) return 0;
                    if (result == long.class) return 0L;
                    return null;
                }));
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static String normalizeProse(String text) {
        return text.replace('“', '"').replace('”', '"')
                .replace('’', '\'').replace("**", "")
                .replaceAll("#\\s*font:victor14", "")
                .replace("{player_title}", "$chiefNavigatorFobIthacaPlayerTitle")
                .replaceAll("\\s+", " ").trim();
    }

    static void verifyChoiceContinuations() throws Exception {
        String prefix = "ChiefNavigatorFobIthacaApproach_";
        String inscriptionTrigger = "ChiefNavigatorFobIthacaApproachInscription";
        String channelTailTrigger = "ChiefNavigatorFobIthacaApproachOpenChannelContinue";
        Path root = Path.of("").toAbsolutePath();
        String rules = Files.readString(root.resolve("data/campaign/rules.csv"),
                StandardCharsets.UTF_8);
        // Use the same installed CSV loader as the external build check.
        JSONArray parsed = (JSONArray) Class.forName("com.fs.starfarer.loading.G")
                .getMethod("o00000", String.class).invoke(null, rules);
        Map<String, JSONObject> runtime = new LinkedHashMap<>();
        for (int index = 0; index < parsed.length(); index++) {
            JSONObject row = parsed.getJSONObject(index);
            String trigger = row.optString("trigger");
            if (!trigger.startsWith(prefix) && !trigger.equals(inscriptionTrigger)
                    && !trigger.equals(channelTailTrigger)) continue;
            check(runtime.put(trigger, row) == null,
                    "Approach stages must have one unambiguous rule: " + trigger);
        }
        String ink = Files.readString(root.resolve("dialogue/fob_ithaca_approach.ink"),
                StandardCharsets.UTF_8).replace("\r\n", "\n");
        String[] stages = {
            "opening", "reveal", "battle", "battle_2", "defense_guns",
            "fan_out", "contacts", "warships", "open_channel",
            "confirm_receipt", "acknowledge", "identify", "decline",
            "macrocomplex_awake", "strategos", "fourteenth", "ring_breach"
        };
        Pattern knotPattern = Pattern.compile(
                "(?ms)^=== ([a-z_0-9]+) ===\\s*\\n(.*?)(?=^=== |\\z)");
        Pattern choicePattern = Pattern.compile(
                "(?m)^\\+ \\[(.+)\\] -> ([A-Za-z_0-9]+)\\s*$");
        Matcher knots = knotPattern.matcher(ink);
        Set<String> visited = new HashSet<>();
        int authored = 0;
        for (String stage : stages) {
            check(knots.find() && knots.group(1).equals(stage),
                    "Ink must retain the revised approach sequence at " + stage);
            String block = knots.group(2);
            Matcher choice = choicePattern.matcher(block);
            check(choice.find(), "Each authored approach stage needs its choice: " + stage);
            String label = normalizeProse(choice.group(1));
            String next = choice.group(2);
            String prose = normalizeProse(block.substring(0, choice.start()));
            check(!choice.find(), "Approach stages remain a single choice chain: " + stage);
            check(next.equals(authored + 1 < stages.length ? stages[authored + 1] : "END"),
                    "The revised authored choice must reach the next stage: " + stage);

            StringBuilder runtimeProse = new StringBuilder();
            String trigger = prefix + stage;
            while (true) {
                JSONObject row = runtime.get(trigger);
                check(row != null && visited.add(trigger),
                        "Every approach choice must reach one existing, noncyclic page: " + trigger);
                check(row.optString("conditions").isBlank(),
                        "Approach narration must remain available on its existing route: " + trigger);
                runtimeProse.append(' ').append(row.optString("text"));
                if (trigger.equals(prefix + "open_channel")) {
                    check(row.optString("options").isBlank()
                                    && row.optString("script").replace("\r\n", "\n").equals(
                                            "ChiefNavigatorSinniIntroCMD styledText victor "
                                                    + inscriptionTrigger + "\nFireBest " + channelTailTrigger),
                            "Opening comms must scope Victor to the inscription, then resume ordinary dialogue");
                    JSONObject inscription = runtime.get(inscriptionTrigger);
                    check(inscription != null && visited.add(inscriptionTrigger)
                                    && inscription.optString("conditions").isBlank()
                                    && inscription.optString("script").isBlank()
                                    && inscription.optString("options").isBlank(),
                            "The Victor inscription must remain an automatic text-only segment");
                    runtimeProse.append(' ').append(inscription.optString("text"));
                    trigger = channelTailTrigger;
                    continue;
                }
                check(row.optString("script").isBlank(),
                        "Narrative pages must not commit completion or other state: " + trigger);
                String[] options = row.optString("options").strip().split("\\R");
                check(options.length == 1, "Each runtime approach page needs one option: " + trigger);
                String[] option = options[0].split(":", 3);
                check(option.length == 3 && option[0].matches("[0-9]+"),
                        "Approach choice must retain its native option format: " + trigger);
                if (option[1].startsWith("chief_navigator_auto_page_")) {
                    check(option[2].equals("Continue."),
                            "Runtime pagination must use an ordinary Continue: " + trigger);
                    trigger = prefix + option[1];
                    continue;
                }
                check(option[1].equals(next.equals("END") ? "complete" : next)
                                && normalizeProse(option[2]).equals(label),
                        "Runtime must retain the authored choice and its route: " + stage);
                break;
            }
            check(normalizeProse(runtimeProse.toString()).equals(prose),
                    "Runtime pages must preserve all authored prose in order: " + stage);
            if (label.startsWith("\"")) {
                check(!prose.contains(label),
                        "Spoken choices must not be repeated in the preceding prose: " + stage);
            }
            authored++;
        }
        check(!knots.find(), "The mirror must not retain retired approach stages");
        JSONObject complete = runtime.get(prefix + "complete");
        check(complete != null && complete.optString("conditions").isBlank()
                        && complete.optString("text").isBlank()
                        && complete.optString("options").isBlank()
                        && complete.optString("script").equals("FireAll ChiefNavigatorFobIthaca"),
                "Final completion must retain its existing full Menelaus introduction handoff");
        check(visited.size() + 1 == runtime.size(),
                "No retired or unreachable approach pages may remain");
        check(ink.contains("+ [\"They're losing.\"] -> END"),
                "The final spoken choice replaces the old Continue without changing completion");
        String titleToken = "$chiefNavigatorFobIthacaPlayerTitle";
        MemoryAPI titleMemory = mock(MemoryAPI.class, (name, args) -> {
            if (name.equals("getKeys")) return List.of(titleToken);
            if (name.equals("get") && titleToken.equals(args[0])) return "Captain";
            return null;
        });
        Map<String, MemoryAPI> scope = Map.of(MemKeys.LOCAL, titleMemory);
        SettingsAPI previousSettings = Global.getSettings();
        try {
            Global.setSettings(mock(SettingsAPI.class, (name, args) ->
                    name.equals("getColor") ? Color.WHITE : null));
            String reveal = Misc.replaceTokensFromMemory(
                    runtime.get(prefix + "reveal").optString("text"), scope);
            check(reveal.contains("\"Captain--!\" Sinni turns toward you."),
                    "Native replacement must preserve punctuation beside the reveal's player title");
            for (JSONObject row : runtime.values()) {
                String rendered = Misc.replaceTokensFromMemory(
                        row.optString("text") + row.optString("options"), scope);
                check(!rendered.contains(titleToken),
                        "Native replacement must resolve every approach title in prose and choices");
            }
        } finally {
            Global.setSettings(previousSettings);
        }
    }

    static void verifyPortraitPresentation() {
        SectorAPI previousSector = Global.getSector();
        FactoryAPI previousFactory = Global.getFactory();
        SettingsAPI previousSettings = Global.getSettings();
        try {
            List<String> events = new ArrayList<>();
            Map<Object, String> options = new HashMap<>();
            MemoryAPI memory = mock(MemoryAPI.class, (name, args) -> {
                if (name.equals("getBoolean")) return true;
                if (name.equals("set") && FobIthacaApproachScript.COMPLETE.equals(args[0])) {
                    check(Boolean.TRUE.equals(args[1]), "Completion must remain additive");
                    events.add("complete");
                }
                return null;
            });
            PersonAPI player = mock(PersonAPI.class, (name, args) -> null);
            PersonAPI sinni = mock(PersonAPI.class, (name, args) -> {
                if (name.equals("getId")) return SinniContact.PERSON_ID;
                if (name.equals("getMemoryWithoutUpdate")) return memory;
                return null;
            });
            PersonAPI captain = mock(PersonAPI.class, (name, args) ->
                    name.equals("getMemoryWithoutUpdate") ? memory : null);
            ImportantPeopleAPI people = mock(ImportantPeopleAPI.class,
                    (name, args) -> {
                        if (!name.equals("getPerson")) return null;
                        if (SinniContact.PERSON_ID.equals(args[0])) return sinni;
                        if (FobIthacaContacts.SPARTAN_CAPTAIN_PERSON_ID
                                .equals(args[0])) return captain;
                        return null;
                    });
            VisualPanelAPI visual = mock(VisualPanelAPI.class, (name, args) -> {
                if (name.equals("showPersonInfo")) {
                    if (args[0] == player) {
                        check(args.length == 2 && Boolean.TRUE.equals(args[1]),
                                "Opening player portrait must remain minimal");
                        events.add("player");
                    } else {
                        check(args[0] == captain && args.length == 3
                                        && Boolean.FALSE.equals(args[1])
                                        && Boolean.FALSE.equals(args[2]),
                                "Kleon must use the native faction panel without a relationship bar");
                        events.add("captainBanner");
                    }
                }
                if (name.equals("showSecondPerson")) {
                    check(args[0] == sinni, "Sinni must remain in both bridge and Aias conversations");
                    events.add("sinni");
                }
                if (name.equals("hideSecondPerson")) events.add("hideSinni");
                if (name.equals("hideThirdPerson")) events.add("hideThird");
                if (name.equals("showThirdPerson")) {
                    check(args[0] == player, "Player must be the third participant beside Aias and Sinni");
                    events.add("thirdPlayer");
                }
                return null;
            });
            OptionPanelAPI optionPanel = mock(OptionPanelAPI.class, (name, args) -> {
                if (name.equals("clearOptions")) {
                    events.add("clearOptions");
                    options.clear();
                }
                return null;
            });
            TextPanelAPI text = mock(TextPanelAPI.class, (name, args) -> {
                if (name.equals("clear")) throw new AssertionError("Portrait changes must retain prose history");
                return null;
            });
            InteractionDialogAPI dialog = mock(InteractionDialogAPI.class, (name, args) -> {
                if (name.equals("getVisualPanel")) return visual;
                if (name.equals("getOptionPanel")) return optionPanel;
                if (name.equals("getTextPanel")) return text;
                if (name.equals("dismiss")) throw new AssertionError("The authored page must remain open");
                if (name.equals("addOptionSelectedText")) {
                    check(options.containsKey(args[0]), "Choice must echo before its menu is cleared");
                    events.add("echo:" + args[0]);
                }
                return null;
            });
            RulesAPI rules = mock(RulesAPI.class, (name, args) -> {
                if (!name.equals("getBestMatching")) return null;
                String trigger = (String) args[1];
                return mock(RuleAPI.class, (ruleName, ruleArgs) -> {
                    if (ruleName.equals("getId")) return "test_" + trigger;
                    if (ruleName.equals("getText") || ruleName.equals("getOptions")
                            || ruleName.equals("getScriptCopy")) return List.of();
                    if (ruleName.equals("runScript")) events.add("rule:" + trigger);
                    return null;
                });
            });
            Global.setFactory(mock(FactoryAPI.class, (name, args) ->
                    name.equals("createMemory") ? memory : null));
            Global.setSettings(mock(SettingsAPI.class, (name, args) ->
                    name.equals("getColor") ? Color.WHITE : null));
            Global.setSector(mock(SectorAPI.class, (name, args) -> {
                if (name.equals("getMemoryWithoutUpdate")
                        || name.equals("getPlayerMemoryWithoutUpdate")) return memory;
                if (name.equals("getPlayerPerson")) return player;
                if (name.equals("getImportantPeople")) return people;
                if (name.equals("getRules")) return rules;
                return null;
            }));

            FobIthacaApproachInteraction interaction = new FobIthacaApproachInteraction();
            interaction.init(dialog);
            check(events.indexOf("player") < events.indexOf("sinni")
                            && !events.contains("captainBanner"),
                    "Kleon must not appear before the channel opens");
            options.put("reveal", "Continue.");
            interaction.optionSelected("Continue.", "reveal");
            check(!events.contains("hideSinni") && !events.contains("captainBanner"),
                    "An ordinary continuation must preserve the opening portrait pair");

            options.put("open_channel", "\"Open a channel.\"");
            interaction.optionSelected("\"Open a channel.\"", "open_channel");
            check(Collections.frequency(events, "echo:open_channel") == 1,
                    "Open a channel must echo exactly once");
            check(events.indexOf("echo:open_channel") < events.indexOf("hideSinni")
                            && events.indexOf("hideSinni") < events.indexOf("captainBanner")
                            && events.indexOf("captainBanner") < events.indexOf(
                                    "rule:ChiefNavigatorFobIthacaApproach_open_channel"),
                    "The native Kleon banner must precede his channel page");
            check(Collections.frequency(events, "sinni") == 2
                            && Collections.frequency(events, "thirdPlayer") == 1
                            && events.indexOf("thirdPlayer") < events.indexOf(
                                    "rule:ChiefNavigatorFobIthacaApproach_open_channel"),
                    "Aias's banner must retain both Sinni and player portraits before his page");
            options.put("confirm_receipt", "Continue.");
            interaction.optionSelected("Continue.", "confirm_receipt");
            check(Collections.frequency(events, "captainBanner") == 1
                            && Collections.frequency(events, "hideSinni") == 1,
                    "Following pages must retain Kleon's faction panel without rebuilding it");
            check(!events.contains("complete"),
                    "Ordinary approach choices must not commit first-contact completion");
            options.put("complete", "\"They're losing.\"");
            interaction.optionSelected("\"They're losing.\"", "complete");
            check(Collections.frequency(events, "complete") == 1
                            && Collections.frequency(events, "echo:complete") == 1
                            && events.indexOf("echo:complete") < events.indexOf("complete")
                            && events.indexOf("complete") < events.indexOf(
                                    "rule:ChiefNavigatorFobIthacaApproach_complete"),
                    "The final spoken choice must echo, persist completion, then open Menelaus");
        } finally {
            Global.setSector(previousSector);
            Global.setFactory(previousFactory);
            Global.setSettings(previousSettings);
        }
    }

    static SectorEntityToken target(String id) {
        MemoryAPI ownedMemory = mock(MemoryAPI.class, (name, args) ->
                name.equals("getBoolean") ? true : null);
        if (OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_STATION_ID
                .equals(id)) {
            ShipVariantAPI variant = mock(ShipVariantAPI.class,
                    (name, args) -> name.equals("getHullVariantId")
                            ? OdysseyExpanseSystem
                                    .FOB_ITHACA_GATE_DEFENSE_VARIANT_ID
                            : null);
            ShipHullSpecAPI hull = mock(ShipHullSpecAPI.class,
                    (name, args) -> name.equals("getHullId")
                            ? OdysseyExpanseSystem
                                    .FOB_ITHACA_GATE_DEFENSE_HULL_ID
                            : null);
            FleetMemberAPI member = mock(FleetMemberAPI.class,
                    (name, args) -> {
                        if (name.equals("getVariant")) return variant;
                        if (name.equals("getHullSpec")) return hull;
                        return null;
                    });
            FleetDataAPI data = mock(FleetDataAPI.class,
                    (name, args) -> name.equals("getMembersListCopy")
                            ? Collections.singletonList(member) : null);
            return mock(CampaignFleetAPI.class, (name, args) -> {
                if (name.equals("getId")) return id;
                if (name.equals("isStationMode")) return true;
                if (name.equals("getFleetData")) return data;
                return null;
            });
        }
        if (OdysseyExpanseSystem.FOB_ITHACA_SPARTAN_GUARD_ID.equals(id)
                || OdysseyExpanseSystem.FOB_ITHACA_SPARTAN_MOUTH_NORTH_ID
                        .equals(id)
                || OdysseyExpanseSystem.FOB_ITHACA_SPARTAN_MOUTH_SOUTH_ID
                        .equals(id)) {
            return mock(CampaignFleetAPI.class, (name, args) -> {
                if (name.equals("getId")) return id;
                if (name.equals("getMemoryWithoutUpdate")) {
                    return ownedMemory;
                }
                return null;
            });
        }
        return mock(SectorEntityToken.class, (name, args) -> {
            if (name.equals("getId")) return id;
            if (name.equals("getMemoryWithoutUpdate")) return ownedMemory;
            if (name.equals("getCustomEntityType")) {
                if (IthacaSectionEncounter.FOB_ENTITY_ID.equals(id)) {
                    return "chief_navigator_fob_ithaca_campaign";
                }
                if (OdysseyExpanseSystem.FOB_ITHACA_GATE_ID.equals(id)) {
                    return Entities.INACTIVE_GATE;
                }
            }
            return null;
        });
    }

    static SectorEntityToken bastionTarget(String sectionId) {
        MemoryAPI memory = mock(MemoryAPI.class, (name, args) -> {
            if (name.equals("getBoolean")) {
                return IthacaSectionEncounter.INTERACTION_MARKER.equals(args[0]);
            }
            if (name.equals("getString")
                    && IthacaSectionEncounter.SECTION_KEY.equals(args[0])) {
                return sectionId;
            }
            return null;
        });
        return mock(SectorEntityToken.class, (name, args) -> {
            if (name.equals("getId")) {
                return IthacaSectionEncounter.INTERACTION_ID_PREFIX
                        + sectionId;
            }
            if (name.equals("getCustomEntityType")) {
                return IthacaSectionEncounter.INTERACTION_ENTITY_TYPE;
            }
            if (name.equals("getMemoryWithoutUpdate")) return memory;
            return null;
        });
    }

    public static void main(String[] args) throws Exception {
        verifyChoiceContinuations();
        Map<String, Object> flags = new HashMap<String, Object>();
        MemoryAPI memory = mock(MemoryAPI.class, (name, values) -> {
            if (name.equals("getBoolean")) {
                return Boolean.TRUE.equals(flags.get(values[0]));
            }
            if (name.equals("set") && values.length >= 2) {
                flags.put((String) values[0], values[1]);
            }
            return null;
        });

        PersonAPI sinni = mock(PersonAPI.class, (name, values) -> null);
        OfficerDataAPI officer = mock(
                OfficerDataAPI.class, (name, values) -> null);
        FleetDataAPI fleetData = mock(FleetDataAPI.class, (name, values) ->
                name.equals("getOfficerData") && values[0] == sinni
                        ? officer : null);
        CampaignFleetAPI player = mock(
                CampaignFleetAPI.class,
                (name, values) -> name.equals("getFleetData")
                        ? fleetData : null);
        ImportantPeopleAPI people = mock(
                ImportantPeopleAPI.class,
                (name, values) -> name.equals("getPerson")
                        && SinniContact.PERSON_ID.equals(values[0])
                                ? sinni : null);
        SectorAPI sector = mock(SectorAPI.class, (name, values) -> {
            if (name.equals("getMemoryWithoutUpdate")) return memory;
            if (name.equals("getPlayerFleet")) return player;
            if (name.equals("getImportantPeople")) return people;
            return null;
        });
        Global.setSector(sector);
        flags.put(SinniBarEvent.STARTED, true);

        String[] guardedIds = {
            IthacaSectionEncounter.FOB_ENTITY_ID,
            OdysseyExpanseSystem.FOB_ITHACA_GATE_ID,
            OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_STATION_ID,
            OdysseyExpanseSystem.FOB_ITHACA_SPARTAN_GUARD_ID,
            OdysseyExpanseSystem.FOB_ITHACA_SPARTAN_MOUTH_NORTH_ID,
            OdysseyExpanseSystem.FOB_ITHACA_SPARTAN_MOUTH_SOUTH_ID
        };
        for (String id : guardedIds) {
            check(FobIthacaApproachScript.shouldInterceptInteraction(
                            target(id)),
                    "Pending first contact must intercept " + id);
        }
        check(FobIthacaApproachScript.shouldInterceptInteraction(
                        bastionTarget("north")),
                "Pending first contact must intercept a bastion token");
        check(IthacaSectionEncounter.CAMPAIGN_INTERACTION_RADIUS == 450f,
                "Bastion tokens must match the Drifting Wall radius");
        check(DriftingWallEncounter.CAMPAIGN_INTERACTION_RADIUS == 450f,
                "Drifting Wall interaction radius must be halved");
        check(!FobIthacaApproachScript.shouldInterceptInteraction(
                        target("unrelated")),
                "Unrelated entities must keep their ordinary interaction");

        check(FobIthacaApproachScript.APPROACH_TRIGGER_RADIUS == 3000f,
                "Approach threshold must remain inside the established view");
        check(Math.hypot(4500f, -1800f)
                        > FobIthacaApproachScript.APPROACH_TRIGGER_RADIUS,
                "Primary arrival must not open the scene immediately");

        FobIthacaApproachScript.markComplete();
        check(FobIthacaApproachScript.isComplete(),
                "The final option must persist the additive completion flag");
        check(FobIthacaApproachScript.shouldOpenStationIntroduction(
                        target(IthacaSectionEncounter.FOB_ENTITY_ID)),
                "The first station interaction must open Menelaus's intro");
        check(!FobIthacaApproachScript.shouldInterceptInteraction(
                        target(guardedIds[2])),
                "Completed contact must release ordinary interactions");

        flags.remove(FobIthacaApproachScript.COMPLETE);
        flags.put(FobIthacaApproachScript.MENELAUS_INTRO_SHOWN, true);
        check(!FobIthacaApproachScript.shouldOpenStationIntroduction(
                        target(IthacaSectionEncounter.FOB_ENTITY_ID)),
                "The station introduction must run only once");
        check(!FobIthacaApproachScript.shouldInterceptInteraction(
                        target(guardedIds[2])),
                "A completed Menelaus introduction must suppress a stale "
                        + "approach replay");

        flags.remove(FobIthacaApproachScript.MENELAUS_INTRO_SHOWN);
        flags.put(MenelausTrial.ACCEPTED, true);
        check(!FobIthacaApproachScript.shouldOpenStationIntroduction(
                        target(IthacaSectionEncounter.FOB_ENTITY_ID)),
                "Accepted legacy saves must not replay the station intro");
        check(!FobIthacaApproachScript.shouldInterceptInteraction(
                        target(guardedIds[2])),
                "Accepted legacy saves must be grandfathered past contact");

        String interaction = Files.readString(Path.of("").toAbsolutePath()
                        .resolve("src/chiefnavigator/quest/"
                                + "FobIthacaApproachInteraction.java"),
                StandardCharsets.UTF_8).replace("\r\n", "\n");
        check(interaction.contains(
                        "showPersonInfo(player, true);\n"
                                + "            if (sinni != null) {\n"
                                + "                dialog.getVisualPanel()"
                                + ".showSecondPerson(sinni);"),
                "The opening panel must show the player and Sinni together");
        int openChannel = interaction.indexOf(
                "if (OPEN_CHANNEL_OPTION.equals(optionId)");
        int hideSinni = interaction.indexOf("hideSecondPerson();", openChannel);
        int showOfficer = interaction.indexOf(
                "showPersonInfo(officer, false, false);", openChannel);
        check(openChannel >= 0 && hideSinni > openChannel
                        && showOfficer > hideSinni,
                    "Aias must take the primary banner panel only when comms open");
        check(interaction.indexOf("showSecondPerson(sinni);", showOfficer) > showOfficer
                        && interaction.indexOf("showThirdPerson(player);", showOfficer) > showOfficer,
                "Opening Aias's channel must retain Sinni and player as companions");

        verifyPortraitPresentation();

        System.out.println(
                "FOB Ithaca approach regression checks passed.");
    }
}
