package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.intel.contacts.ContactIntel;
import java.util.ArrayList;

/** Persistent Sinni identity and officer-roster migration. */
public final class SinniContact {
    public static final String PERSON_ID = "chief_navigator_sinni";
    private static final String HOME_MARKET_ID = "eventide";
    private static final String PORTRAIT_ID = "chief_navigator_sinni";
    private static final String CHATTER_CHARACTER = "chief_navigator_sinni";
    private static final String OWNER_MARKER =
            "$chief_navigator_sinni_owned_v1";
    private static final String CREATION_LATCH =
            "$chief_navigator_sinni_created_v1";

    private SinniContact() { }

    public static PersonAPI getOrCreatePerson() {
        ImportantPeopleAPI people = Global.getSector().getImportantPeople();
        MemoryAPI sectorMemory = Global.getSector().getMemoryWithoutUpdate();
        PersonAPI person = people.getPerson(PERSON_ID);
        if (person == null) {
            if (sectorMemory.getBoolean(CREATION_LATCH)) return null;
            sectorMemory.set(CREATION_LATCH, true);
            person = Global.getFactory().createPerson();
            person.setId(PERSON_ID);
            person.setName(new FullName("Sinni", "", FullName.Gender.FEMALE));
            person.setGender(FullName.Gender.FEMALE);
            person.setFaction("independent");
            person.setRankId(Ranks.SPACE_CAPTAIN);
            person.setPostId(Ranks.POST_SMUGGLER);
            person.setImportance(PersonImportance.HIGH);
            person.setVoice("spacer");
            person.setPortraitSprite(Global.getSettings().getSpriteName(
                    "characters", PORTRAIT_ID));
            person.getMemoryWithoutUpdate().set(
                    "$chatterChar", CHATTER_CHARACTER);
            person.getMemoryWithoutUpdate().set(OWNER_MARKER, true);
            people.addPerson(person);
        } else if (!isOwnedPerson(person)) {
            sectorMemory.set(CREATION_LATCH, true);
            return null;
        } else {
            sectorMemory.set(CREATION_LATCH, true);
        }
        return person;
    }

    public static boolean isOwnedPerson(PersonAPI person) {
        return person != null && PERSON_ID.equals(person.getId())
                && (person.getMemoryWithoutUpdate().getBoolean(OWNER_MARKER)
                    || CHATTER_CHARACTER.equals(person.getMemoryWithoutUpdate()
                            .getString("$chatterChar")));
    }

    public static String getPortraitSprite() {
        PersonAPI person = getOrCreatePerson();
        return person == null
                ? Global.getSettings().getSpriteName(
                        "characters", PORTRAIT_ID)
                : person.getPortraitSprite();
    }

    /** Removes a premature contact from a campaign where Sinni was not recruited. */
    public static void cleanupLegacyContact() {
        if (Global.getSector() == null) return;
        PersonAPI person = Global.getSector().getImportantPeople().getPerson(PERSON_ID);
        if (!isOwnedPerson(person)) return;

        IntelManagerAPI manager = Global.getSector().getIntelManager();
        for (IntelInfoPlugin item : new ArrayList<IntelInfoPlugin>(
                manager.getIntel())) {
            if (isSinniContact(item)) manager.removeIntel(item);
        }

        MarketAPI market = person.getMarket();
        if (market != null) {
            market.getCommDirectory().removePerson(person);
            market.removePerson(person);
        }
        person.setMarket(null);
    }

    /** Adds Sinni to the officer roster and repairs her permanent contact. */
    public static PersonAPI joinPlayerFleet() {
        return joinPlayerFleet(null);
    }

    /** Adds Sinni and optionally reports the new contact in an interaction. */
    public static PersonAPI joinPlayerFleet(TextPanelAPI text) {
        PersonAPI person = getOrCreatePerson();
        if (person == null) return null;
        person.setPostId(Ranks.POST_OFFICER);

        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet != null && fleet.getFleetData().getOfficerData(person) == null) {
            fleet.getFleetData().addOfficer(person);
        }
        ensureContact(person, text);
        return person;
    }

    /** Keeps exactly one custom, permanent Sinni contact in the Intel manager. */
    public static SinniContactIntel ensureContact(TextPanelAPI text) {
        if (Global.getSector() == null) return null;
        return ensureContact(getOrCreatePerson(), text);
    }

    private static SinniContactIntel ensureContact(
            PersonAPI person, TextPanelAPI text) {
        if (!isOwnedPerson(person)) return null;
        MarketAPI home = Global.getSector().getEconomy().getMarket(HOME_MARKET_ID);
        if (home == null) {
            Global.getLogger(SinniContact.class).warn(
                    "Could not install Sinni contact: Eventide market missing");
            return null;
        }

        IntelManagerAPI manager = Global.getSector().getIntelManager();
        SinniContactIntel keep = null;
        for (IntelInfoPlugin item : new ArrayList<IntelInfoPlugin>(
                manager.getIntel())) {
            if (!isSinniContact(item)) continue;
            if (keep == null && item instanceof SinniContactIntel
                    && !item.isEnded() && !item.isEnding()) {
                keep = (SinniContactIntel) item;
            } else {
                manager.removeIntel(item);
            }
        }

        if (keep == null) {
            keep = new SinniContactIntel(person, home);
            keep.setState(ContactIntel.ContactState.NON_PRIORITY);
            manager.addIntel(keep, false, text);
        }
        keep.ensurePermanentState();
        keep.ensureAtMarket(home);
        return keep;
    }

    private static boolean isSinniContact(IntelInfoPlugin item) {
        // Ownership is established by our Intel implementation, not merely
        // by the person id. Another mod may legitimately expose its own
        // ContactIntel for the same persistent Sinni person.
        return item instanceof SinniContactIntel;
    }
}
