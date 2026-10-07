package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.InteractionDialogImageVisual;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.VisualPanelAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.campaign.rulecmd.ChiefNavigatorPeopleCMD;
import java.awt.Color;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Headless native-slot and four-speaker/illustrated-card layout contracts. */
public final class ConversationPortraitsRegression {
    private static final String CREST = "graphics/factions/spartan_crest.png";

    interface Call { Object invoke(String name, Object[] args); }

    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[] {type}, (proxy, method, args) -> {
                    if (method.getName().equals("hashCode")) {
                        return System.identityHashCode(proxy);
                    }
                    if (method.getName().equals("equals")) return proxy == args[0];
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

    static void equal(float actual, float expected, String message) {
        check(Math.abs(actual - expected) < 0.001f,
                message + ": expected " + expected + ", got " + actual);
    }

    static PersonAPI person(String id, String name, Color color) {
        FactionAPI faction = mock(FactionAPI.class, (method, args) -> {
            if (method.equals("getBaseUIColor")) return color;
            if (method.equals("getCrest")) return CREST;
            return null;
        });
        return mock(PersonAPI.class, (method, args) -> {
            if (method.equals("getId")) return id;
            if (method.equals("getNameString")) return name;
            if (method.equals("getPortraitSprite")) return "portrait/" + name;
            if (method.equals("getFaction")) return faction;
            return null;
        });
    }

    static final class Element {
        float width, height, left, top;
        String image, name;
        Color color;
        Alignment alignment;
        boolean positioned, scrollable;
    }

    static final class Fixture {
        final List<String> events = new ArrayList<>();
        final List<PersonAPI> nativePeople = new ArrayList<>();
        final List<Element> elements = new ArrayList<>();
        final Map<Object, Element> elementByUi = new IdentityHashMap<>();
        boolean customAvailable = true;
        float panelWidth, panelHeight;
        boolean banner;
        final CustomPanelAPI panel = mock(CustomPanelAPI.class, (method, args) -> {
            if (method.equals("createUIElement")) {
                Element element = new Element();
                element.width = (Float) args[0];
                element.height = (Float) args[1];
                element.scrollable = (Boolean) args[2];
                elements.add(element);
                LabelAPI label = mock(LabelAPI.class, (labelMethod, labelArgs) -> {
                    if (labelMethod.equals("setAlignment")) {
                        element.alignment = (Alignment) labelArgs[0];
                    }
                    return null;
                });
                TooltipMakerAPI ui = mock(TooltipMakerAPI.class,
                        (uiMethod, uiArgs) -> {
                    if (uiMethod.equals("addImage")) {
                        element.image = (String) uiArgs[0];
                        equal((Float) uiArgs[1], element.width, "Image width");
                        equal((Float) uiArgs[2], element.height, "Image height");
                    }
                    if (uiMethod.equals("addPara")) {
                        element.name = (String) uiArgs[0];
                        element.color = (Color) uiArgs[1];
                        return label;
                    }
                    return null;
                });
                elementByUi.put(ui, element);
                return ui;
            }
            if (method.equals("addUIElement")) {
                Element element = elementByUi.get(args[0]);
                return mock(PositionAPI.class, (positionMethod, positionArgs) -> {
                    if (positionMethod.equals("inTL")) {
                        element.left = (Float) positionArgs[0];
                        element.top = (Float) positionArgs[1];
                        element.positioned = true;
                    }
                    return null;
                });
            }
            return null;
        });
        final VisualPanelAPI visual = mock(VisualPanelAPI.class, (method, args) -> {
            events.add(method);
            if (method.equals("showPersonInfo")) {
                nativePeople.add((PersonAPI) args[0]);
                banner = args.length == 3
                        && Boolean.FALSE.equals(args[1])
                        && Boolean.FALSE.equals(args[2]);
            }
            if (method.equals("showSecondPerson") || method.equals("showThirdPerson")) {
                nativePeople.add((PersonAPI) args[0]);
            }
            if (method.equals("showCustomPanel")) {
                check(args[2] == null, "Cards need no animation or input callback");
                panelWidth = (Float) args[0];
                panelHeight = (Float) args[1];
                return customAvailable ? panel : null;
            }
            return null;
        });
        final InteractionDialogAPI dialog = mock(InteractionDialogAPI.class,
                (method, args) -> method.equals("getVisualPanel") ? visual : null);

        void assertCleanSlots() {
            check(events.size() >= 2 && events.get(0).equals("hideSecondPerson")
                            && events.get(1).equals("hideThirdPerson"),
                    "A page must remove both stale native companions first");
        }

        void assertCardsFit() {
            for (Element element : elements) {
                check(element.positioned && !element.scrollable,
                        "Every native card element needs a fixed placement");
                check(element.left >= 0f && element.top >= 0f
                                && element.left + element.width <= panelWidth + 0.001f
                                && element.top + element.height <= panelHeight + 0.001f,
                        "A card element extends outside its bounded visual panel");
                if (element.name != null) {
                    check(element.alignment == Alignment.MID,
                            "Portrait names must be centered beneath their image");
                }
            }
        }
    }

    static void nativeSlotsAndIdentity() {
        PersonAPI menelaus = person("menelaus", "Menelaus", Color.CYAN);
        PersonAPI sinni = person("sinni", "Sinni", Color.CYAN);
        PersonAPI aias = person(FobIthacaContacts.SPARTAN_CAPTAIN_PERSON_ID,
                "Aias Kleon", Color.CYAN);
        for (int count = 1; count <= 3; count++) {
            Fixture f = new Fixture();
            PersonAPI[] participants = {menelaus, sinni, aias};
            ConversationPortraits.show(f.dialog,
                    java.util.Arrays.copyOf(participants, count));
            f.assertCleanSlots();
            check(f.nativePeople.size() == count && f.elements.isEmpty(),
                    "One to three speakers must use only native person slots");
            for (int i = 0; i < count; i++) {
                check(f.nativePeople.get(i) == participants[i],
                        "Native speaker order must follow the authored page order");
            }
        }
        Fixture f = new Fixture();
        ConversationPortraits.show(f.dialog, null, menelaus, menelaus,
                person("menelaus", "duplicate identity", Color.RED), sinni, null);
        check(f.nativePeople.equals(List.of(menelaus, sinni)),
                "Nulls, object duplicates, and repeated person IDs consume no slots");
        f = new Fixture();
        ConversationPortraits.show(f.dialog, aias, sinni);
        check(f.banner, "Aias's primary native panel must retain the Spartan banner");
        f = new Fixture();
        ConversationPortraits.show(f.dialog, (PersonAPI[]) null);
        f.assertCleanSlots();
        check(f.events.contains("fadeVisualOut") && f.nativePeople.isEmpty(),
                "An empty page participant set must clear the old speaker visual");
        ConversationPortraits.show(null, sinni);
    }

    static void fourSpeakerGrid() {
        PersonAPI[] people = {
            person("menelaus", "Menelaus", Color.CYAN),
            person("voss", "Voss", Color.ORANGE),
            person(FobIthacaContacts.SPARTAN_CAPTAIN_PERSON_ID,
                    "Aias Kleon", Color.CYAN),
            person("sinni", "Sinni", Color.WHITE)
        };
        Fixture f = new Fixture();
        ConversationPortraits.show(f.dialog, people);
        f.assertCleanSlots();
        f.assertCardsFit();
        equal(f.panelWidth, 416f, "Four-person grid width");
        equal(f.panelHeight, 328f, "Four-person grid height");
        check(f.nativePeople.isEmpty(), "The four-person visual must not drop a speaker");
        List<Element> portraits = f.elements.stream()
                .filter(e -> e.image != null && e.image.startsWith("portrait/")).toList();
        List<Element> crests = f.elements.stream()
                .filter(e -> CREST.equals(e.image)).toList();
        check(portraits.size() == 4 && crests.size() == 2,
                "Show exactly four real portraits plus Aias's two flanking crests");
        for (int i = 0; i < 4; i++) {
            equal(portraits.get(i).width, 128f, "Standard portrait width");
            equal(portraits.get(i).height, 128f, "Standard portrait height");
            equal(portraits.get(i).left, 40f + (i % 2) * 208f, "Grid column");
            equal(portraits.get(i).top, (i / 2) * 164f, "Grid row");
        }
        Element aias = portraits.get(2);
        check(crests.get(0).left + crests.get(0).width < aias.left
                        && crests.get(1).left > aias.left + aias.width,
                "Spartan heraldry must flank, never cover, Aias's portrait");
        Element vossName = f.elements.stream()
                .filter(e -> "Voss".equals(e.name)).findFirst().orElseThrow();
        check(Color.ORANGE.equals(vossName.color), "Keep native faction-colored names");

        // A subsequent page clears stale slots and stops displaying the grid.
        f.events.clear();
        ConversationPortraits.show(f.dialog, people[0], people[3]);
        f.assertCleanSlots();
        check(f.nativePeople.equals(List.of(people[0], people[3])),
                "The next two-speaker page returns to the native pair");
        f = new Fixture();
        f.customAvailable = false;
        ConversationPortraits.show(f.dialog, people);
        check(f.nativePeople.equals(List.of(people[0], people[1], people[2])),
                "Unavailable native custom panels fail safely to the supported trio");
    }

    static void illustratedCardsAndLetterbox() {
        SettingsAPI previousSettings = Global.getSettings();
        SectorAPI previousSector = Global.getSector();
        float[] sourceSize = {600f, 200f};
        SpriteAPI sprite = mock(SpriteAPI.class, (method, args) -> {
            if (method.equals("getWidth")) return sourceSize[0];
            if (method.equals("getHeight")) return sourceSize[1];
            return null;
        });
        Global.setSettings(mock(SettingsAPI.class, (method, args) -> {
            if (method.equals("getSprite")) return sprite;
            if (method.equals("getSpriteName")) return "art/" + args[1];
            return null;
        }));
        try {
            PersonAPI[] people = {
                person("sinni", "Sinni", Color.CYAN),
                person("player", "Captain", Color.WHITE),
                person("officer", "Officer", Color.GREEN)
            };
            for (int count = 0; count <= 3; count++) {
                Fixture f = new Fixture();
                ConversationPortraits.showIllustrated(f.dialog, "art/scene",
                        java.util.Arrays.copyOf(people, count));
                f.assertCleanSlots();
                f.assertCardsFit();
                equal(f.panelWidth, 480f, "Illustrated panel width");
                equal(f.panelHeight, count == 0 ? 300f : 476f, "Illustrated panel height");
                check(f.nativePeople.isEmpty(), "Speaker cards must not replace the art");
                Element art = f.elements.get(0);
                equal(art.width, 480f, "Wide art fitted width");
                equal(art.height, 160f, "Wide art natural aspect ratio");
                equal(art.top, 70f, "Wide art vertical letterbox centering");
                equal(art.left, 0f, "Wide art horizontal centering");
                check(f.elements.stream().filter(e -> e.image != null
                                && e.image.startsWith("portrait/")).count() == count,
                        "Keep every illustrated conversation speaker under the art");
            }
            Global.setSector(mock(SectorAPI.class, (method, args) -> {
                throw new AssertionError("Art-only scenes must not create or mutate people: " + method);
            }));
            for (String asset : List.of("chief_navigator_sinni_eventide_tea",
                    "chief_navigator_alpha_odyssey_arrival", "chief_navigator_sinni_ending")) {
                Fixture artOnly = new Fixture();
                boolean shown = new ChiefNavigatorPeopleCMD().execute("artOnly", artOnly.dialog,
                        Misc.tokenize("illustrated " + asset), Map.of());
                check(shown, "A two-argument illustration command must succeed without a speaker");
                artOnly.assertCleanSlots();
                artOnly.assertCardsFit();
                equal(artOnly.panelHeight, 300f, "Art-only scenes must not reserve a portrait row");
                check(artOnly.nativePeople.isEmpty() && artOnly.elements.size() == 1
                                && artOnly.elements.get(0).image.equals("art/" + asset)
                                && artOnly.elements.get(0).name == null,
                        "Keep only the complete illustration, with no portrait or name: " + asset);
            }
            sourceSize[0] = 400f;
            sourceSize[1] = 600f;
            Fixture f = new Fixture();
            ConversationPortraits.showIllustrated(f.dialog, "art/tall", people[0]);
            Element art = f.elements.get(0);
            equal(art.width, 200f, "Tall art fitted width");
            equal(art.height, 300f, "Tall art fitted height");
            equal(art.left, 140f, "Tall art horizontal letterbox centering");
            equal(art.top, 0f, "Tall art vertical centering");
            f.assertCardsFit();
            f = new Fixture();
            f.customAvailable = false;
            ConversationPortraits.showIllustrated(f.dialog, "art/scene", people);
            check(f.nativePeople.equals(List.of(people)),
                    "Missing custom-panel support safely falls back to native portraits");
            f = new Fixture();
            ConversationPortraits.showIllustrated(f.dialog, null, people[0]);
            check(f.nativePeople.equals(List.of(people[0])),
                    "An absent illustration uses the ordinary person visual");
        } finally {
            Global.setSettings(previousSettings);
            Global.setSector(previousSector);
        }
    }

    static void saraPresentationAndFiveSpeakerGrid() {
        FactoryAPI previousFactory = Global.getFactory();
        SectorAPI previousSector = Global.getSector();
        Map<String, Object> fields = new HashMap<>();
        boolean[] dead = {false};
        int[] created = {0};
        MemoryAPI memory = mock(MemoryAPI.class, (method, args) -> {
            check(method.equals("getBoolean") && AviciHabitatEncounter.SARA_DEAD.equals(args[0]),
                    "Sara presentation must only query her existing death flag");
            return dead[0];
        });
        PersonAPI sara = mock(PersonAPI.class, (method, args) -> {
            if (method.startsWith("set")) fields.put(method.substring(3), args[0]);
            if (method.equals("getNameString")) return ((FullName) fields.get("Name")).getFirst();
            if (method.equals("getFaction")) return null;
            if (method.startsWith("get")) return fields.get(method.substring(3));
            return null;
        });
        Global.setFactory(mock(FactoryAPI.class, (method, args) -> {
            check(method.equals("createPerson"), "Sara must not create an officer or other campaign object");
            created[0]++;
            return sara;
        }));
        Global.setSector(mock(SectorAPI.class, (method, args) -> {
            check(method.equals("getMemoryWithoutUpdate"),
                    "Sara's display identity must never be registered as a contact or officer");
            return memory;
        }));
        try {
            Fixture f = new Fixture();
            check(new ChiefNavigatorPeopleCMD().execute("sara", f.dialog,
                            Misc.tokenize("show sara"), Map.of()), "Sara's authored role executes");
            check(f.nativePeople.equals(List.of(sara)) && created[0] == 1,
                    "Sara uses one native person slot");
            check("Sara".equals(sara.getNameString())
                            && "chief_navigator_sara".equals(sara.getId())
                            && "graphics/portraits/portrait37.png".equals(sara.getPortraitSprite())
                            && fields.get("Gender") == FullName.Gender.FEMALE,
                    "Use exactly the requested vanilla portrait37 and Sara's authored identity");
            f = new Fixture();
            ConversationPortraits.show(f.dialog,
                    person("menelaus", "Menelaus", Color.CYAN),
                    person("voss", "Voss", Color.ORANGE),
                    person(FobIthacaContacts.SPARTAN_CAPTAIN_PERSON_ID, "Aias Kleon", Color.CYAN),
                    person("sinni", "Sinni", Color.WHITE), sara);
            f.assertCleanSlots();
            f.assertCardsFit();
            equal(f.panelHeight, 492f, "The Sara aside retains all five conference participants");
            check(f.elements.stream().filter(e -> e.image != null
                            && !e.image.equals(CREST)).count() == 5,
                    "Five speakers must not silently lose Sara or another portrait");
            dead[0] = true;
            f = new Fixture();
            check(new ChiefNavigatorPeopleCMD().execute("deadSara", f.dialog,
                            Misc.tokenize("show sara"), Map.of()), "A dead Sara role clears safely");
            check(created[0] == 1 && f.nativePeople.isEmpty() && f.events.contains("fadeVisualOut"),
                    "Death or remembrance pages must never recreate a live Sara portrait");
            f = new Fixture();
            check(new ChiefNavigatorPeopleCMD().execute("clear", f.dialog,
                            Misc.tokenize("clear"), Map.of()), "Explicit scene restoration clears portraits");
            f.assertCleanSlots();
            check(f.events.contains("fadeVisualOut"), "Clear must remove the primary portrait too");
            Fixture restore = new Fixture();
            InteractionDialogImageVisual scene = new InteractionDialogImageVisual("art/scene", 480f, 300f);
            SectorEntityToken target = mock(SectorEntityToken.class, (method, values) ->
                    method.equals("getCustomInteractionDialogImageVisual") ? scene : null);
            InteractionDialogAPI withTarget = mock(InteractionDialogAPI.class, (method, values) -> {
                if (method.equals("getVisualPanel")) return restore.visual;
                if (method.equals("getInteractionTarget")) return target;
                return null;
            });
            check(new ChiefNavigatorPeopleCMD().execute("restore", withTarget,
                            Misc.tokenize("clear"), Map.of()), "Restore the scene's native default visual");
            check(restore.events.indexOf("showImageVisual") > restore.events.indexOf("fadeVisualOut"),
                    "Clearing Sara must preserve any authored/native habitat illustration afterward");
        } finally {
            Global.setFactory(previousFactory);
            Global.setSector(previousSector);
        }
    }

    public static void main(String[] args) {
        nativeSlotsAndIdentity();
        fourSpeakerGrid();
        illustratedCardsAndLetterbox();
        saraPresentationAndFiveSpeakerGrid();
        System.out.println("Conversation portrait slot, four-person grid, and illustrated-card checks passed.");
    }
}
