package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StoryPointActionDelegate;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.intel.contacts.ContactIntel;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

/** Permanent, fleet-callable Contacts-menu entry for Sinni. */
public final class SinniContactIntel extends ContactIntel {
    private static final String BUTTON_DIRECTIONS =
            "chief_navigator_sinni_contact_directions";
    private static final String BUTTON_ORION_KNOT_MAP =
            "chief_navigator_sinni_contact_orion_knot_map";
    private static final String REMOTE_CONTACT_TRIGGER =
            "ChiefNavigatorSinniRemoteContact";
    private static final String ORION_KNOT_MAP_TRIGGER =
            "ChiefNavigatorSinniOrionKnotMap";

    public SinniContactIntel(PersonAPI person, MarketAPI market) {
        super(person, market);
    }

    @Override
    public void createSmallDescription(
            TooltipMakerAPI info, float width, float height) {
        PersonAPI sinni = getPerson();
        if (sinni != null) {
            TooltipMakerAPI image = info.beginImageWithText(
                    sinni.getPortraitSprite(), 128f);
            image.addPara(sinni.getNameString(), 0f,
                    Misc.getHighlightColor(), sinni.getNameString());
            image.addPara("Chief navigator aboard your fleet", 3f);
            info.addImageWithText(0f);
            info.addRelationshipBar(sinni, width, 10f);
        }

        info.addPara("Sinni remains available over the fleet's private "
                + "command channel and can translate the expedition's live "
                + "objective into a usable course.", 10f);

        SinniNavigationGuidance.Guidance guidance =
                SinniNavigationGuidance.getCurrentGuidance();
        info.addPara("Next destination: %s", 10f,
                Misc.getTextColor(), Misc.getHighlightColor(),
                guidance.getDestination());
        info.addPara(guidance.getObjective(), 6f);
        info.addPara(guidance.getRoute(), 6f);

        String priority = getState() == ContactState.PRIORITY
                ? "Remove priority status" : "Make priority contact";
        info.addButton(priority, BUTTON_PRIORITY, width, 20f, 10f);
        info.addButton("Ask Sinni where to go next", BUTTON_DIRECTIONS,
                width, 20f, 10f);
        boolean arrivedInOrionKnot = Global.getSector() != null
                && Global.getSector().getMemoryWithoutUpdate().getBoolean(
                        OdysseyExpanseSystem.ENTERED);
        if (arrivedInOrionKnot) {
            info.addButton("Open Sinni's Orion Knot map",
                    BUTTON_ORION_KNOT_MAP, width, 20f, 10f);
        }
    }

    @Override
    public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        if (BUTTON_DELETE.equals(buttonId) || BUTTON_SUSPEND.equals(buttonId)) {
            return;
        }
        if (BUTTON_DIRECTIONS.equals(buttonId)) {
            if (Global.getSector() != null) {
                ui.showDialog(null, REMOTE_CONTACT_TRIGGER);
            }
            return;
        }
        if (BUTTON_ORION_KNOT_MAP.equals(buttonId)) {
            if (Global.getSector() != null
                    && Global.getSector().getMemoryWithoutUpdate().getBoolean(
                            OdysseyExpanseSystem.ENTERED)) {
                if (ui != null) ui.showDialog(null, ORION_KNOT_MAP_TRIGGER);
            }
            return;
        }
        super.buttonPressConfirmed(buttonId, ui);
    }

    @Override
    public void storyActionConfirmed(Object buttonId, IntelUIAPI ui) {
        if (BUTTON_DELETE.equals(buttonId) || BUTTON_SUSPEND.equals(buttonId)) {
            return;
        }
        super.storyActionConfirmed(buttonId, ui);
    }

    @Override
    public boolean doesButtonHaveConfirmDialog(Object buttonId) {
        if (BUTTON_DELETE.equals(buttonId) || BUTTON_SUSPEND.equals(buttonId)) {
            return false;
        }
        return super.doesButtonHaveConfirmDialog(buttonId);
    }

    @Override
    public StoryPointActionDelegate getButtonStoryPointActionDelegate(
            Object buttonId) {
        if (BUTTON_DELETE.equals(buttonId) || BUTTON_SUSPEND.equals(buttonId)) {
            return null;
        }
        return super.getButtonStoryPointActionDelegate(buttonId);
    }

    @Override
    public void setState(ContactState requested) {
        if (requested == null
                || requested == ContactState.SUSPENDED
                || requested == ContactState.LOST_CONTACT
                || requested == ContactState.LOST_CONTACT_DECIV) {
            requested = ContactState.NON_PRIORITY;
        }
        super.setState(requested);
    }

    public void ensurePermanentState() {
        setState(getState());
    }

    public void ensureAtMarket(MarketAPI home) {
        if (home == null) return;
        if (market != home || getPerson() == null
                || getPerson().getMarket() != home) {
            relocateToMarket(home, false);
        }
        ensureIsAddedToMarket();
    }

    @Override
    public void loseContact(InteractionDialogAPI dialog) {
        setState(ContactState.NON_PRIORITY);
        ensureIsAddedToMarket();
    }

    @Override
    public boolean shouldRemoveIntel() {
        return false;
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        SectorEntityToken target = SinniNavigationGuidance
                .getCurrentGuidance().getTarget();
        return target == null ? super.getMapLocation(map) : target;
    }
}
