package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.VisualPanelAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** Page-local speaker visuals, with native slots for ordinary one-to-three-person pages. */
public final class ConversationPortraits {
    private static final float PORTRAIT_SIZE = 128f;
    private static final float CARD_HEIGHT = 164f;
    private static final float GRID_CARD_WIDTH = 208f;
    private static final float ILLUSTRATION_WIDTH = 480f;
    private static final float ILLUSTRATION_HEIGHT = 300f;
    private static final float ILLUSTRATED_PORTRAIT_TOP = 312f;

    private ConversationPortraits() { }

    /** The argument order is visual order; absent or repeated people occupy no slot. */
    public static void show(InteractionDialogAPI dialog, PersonAPI... participants) {
        if (dialog == null || dialog.getVisualPanel() == null) return;
        List<PersonAPI> people = uniqueParticipants(participants);
        VisualPanelAPI visual = prepare(dialog);
        if (people.isEmpty()) {
            visual.fadeVisualOut();
        } else if (people.size() <= 3) {
            showNative(visual, people);
        } else {
            // Starsector has three person slots, not a fourth. Native UI cards
            // retain all four conference participants without cycling portraits.
            int rows = (people.size() + 1) / 2;
            CustomPanelAPI panel = visual.showCustomPanel(
                    2f * GRID_CARD_WIDTH, rows * CARD_HEIGHT, null);
            if (panel == null) {
                showNative(visual, people);
                return;
            }
            for (int i = 0; i < people.size(); i++) {
                addPersonCard(panel, people.get(i), GRID_CARD_WIDTH,
                        (i % 2) * GRID_CARD_WIDTH, (i / 2) * CARD_HEIGHT);
            }
        }
    }

    /** Retain a complete, aspect-correct illustration above the page's speaker portraits. */
    public static void showIllustrated(InteractionDialogAPI dialog,
            String illustrationPath, PersonAPI... participants) {
        if (dialog == null || dialog.getVisualPanel() == null) return;
        if (illustrationPath == null || illustrationPath.isEmpty()) {
            show(dialog, participants);
            return;
        }
        List<PersonAPI> people = uniqueParticipants(participants);
        VisualPanelAPI visual = prepare(dialog);
        int columns = people.size() <= 3 ? Math.max(1, people.size()) : 2;
        int rows = (people.size() + columns - 1) / columns;
        float panelHeight = people.isEmpty() ? ILLUSTRATION_HEIGHT
                : ILLUSTRATED_PORTRAIT_TOP + rows * CARD_HEIGHT;
        CustomPanelAPI panel = visual.showCustomPanel(
                ILLUSTRATION_WIDTH, panelHeight, null);
        if (panel == null) {
            if (!people.isEmpty()) showNative(visual, people);
            return;
        }

        SpriteAPI image = Global.getSettings().getSprite(illustrationPath);
        float imageWidth = image.getWidth();
        float imageHeight = image.getHeight();
        float scale = Math.min(ILLUSTRATION_WIDTH / imageWidth,
                ILLUSTRATION_HEIGHT / imageHeight);
        float width = imageWidth * scale;
        float height = imageHeight * scale;
        TooltipMakerAPI illustration = panel.createUIElement(width, height, false);
        illustration.addImage(illustrationPath, width, height, 0f);
        panel.addUIElement(illustration).inTL(
                (ILLUSTRATION_WIDTH - width) / 2f,
                (ILLUSTRATION_HEIGHT - height) / 2f);

        float cardWidth = ILLUSTRATION_WIDTH / columns;
        for (int i = 0; i < people.size(); i++) {
            addPersonCard(panel, people.get(i), cardWidth,
                    (i % columns) * cardWidth,
                    ILLUSTRATED_PORTRAIT_TOP + (i / columns) * CARD_HEIGHT);
        }
    }

    private static VisualPanelAPI prepare(InteractionDialogAPI dialog) {
        dialog.showVisualPanel();
        VisualPanelAPI visual = dialog.getVisualPanel();
        // Native showPersonInfo can reuse its panel when the primary person is
        // unchanged; explicitly remove old companions before selecting a page.
        visual.hideSecondPerson();
        visual.hideThirdPerson();
        return visual;
    }

    private static void showNative(VisualPanelAPI visual, List<PersonAPI> people) {
        PersonAPI primary = people.get(0);
        if (isSpartanCaptain(primary)) {
            visual.showPersonInfo(primary, false, false);
        } else {
            visual.showPersonInfo(primary, true);
        }
        if (people.size() > 1) visual.showSecondPerson(people.get(1));
        if (people.size() > 2) visual.showThirdPerson(people.get(2));
    }

    private static void addPersonCard(CustomPanelAPI panel, PersonAPI person,
            float cardWidth, float left, float top) {
        float portraitLeft = left + (cardWidth - PORTRAIT_SIZE) / 2f;
        TooltipMakerAPI portrait = panel.createUIElement(
                PORTRAIT_SIZE, PORTRAIT_SIZE, false);
        portrait.addImage(person.getPortraitSprite(),
                PORTRAIT_SIZE, PORTRAIT_SIZE, 0f);
        panel.addUIElement(portrait).inTL(portraitLeft, top);

        FactionAPI faction = person.getFaction();
        Color nameColor = faction == null ? Color.WHITE : faction.getBaseUIColor();
        if (nameColor == null) nameColor = Color.WHITE;
        TooltipMakerAPI name = panel.createUIElement(cardWidth, 28f, false);
        name.setParaSmallOrbitron();
        LabelAPI label = name.addPara(person.getNameString(), nameColor, 0f);
        if (label != null) label.setAlignment(Alignment.MID);
        panel.addUIElement(name).inTL(left, top + PORTRAIT_SIZE + 6f);

        if (isSpartanCaptain(person) && faction != null
                && faction.getCrest() != null && !faction.getCrest().isEmpty()) {
            // The square Spartan crest is legible beside a portrait without
            // squeezing the faction's wide banner into a narrow card margin.
            float crestSize = Math.min(24f,
                    (cardWidth - PORTRAIT_SIZE) / 2f - 4f);
            if (crestSize > 0f) {
                addCrest(panel, faction.getCrest(), crestSize,
                        portraitLeft - crestSize - 4f,
                        top + (PORTRAIT_SIZE - crestSize) / 2f);
                addCrest(panel, faction.getCrest(), crestSize,
                        portraitLeft + PORTRAIT_SIZE + 4f,
                        top + (PORTRAIT_SIZE - crestSize) / 2f);
            }
        }
    }

    private static void addCrest(CustomPanelAPI panel, String path, float size,
            float left, float top) {
        TooltipMakerAPI crest = panel.createUIElement(size, size, false);
        crest.addImage(path, size, size, 0f);
        panel.addUIElement(crest).inTL(left, top);
    }

    private static boolean isSpartanCaptain(PersonAPI person) {
        return FobIthacaContacts.SPARTAN_CAPTAIN_PERSON_ID.equals(person.getId());
    }

    private static List<PersonAPI> uniqueParticipants(PersonAPI[] participants) {
        List<PersonAPI> people = new ArrayList<>();
        if (participants == null) return people;
        Set<PersonAPI> identities = Collections.newSetFromMap(
                new IdentityHashMap<PersonAPI, Boolean>());
        Set<String> ids = new HashSet<>();
        for (PersonAPI person : participants) {
            if (person == null || !identities.add(person)) continue;
            String id = person.getId();
            if (id != null && !id.isEmpty() && !ids.add(id)) continue;
            people.add(person);
        }
        return people;
    }
}
