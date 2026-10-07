package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.util.Misc;
import java.util.ArrayList;
import java.util.List;

/** Persistent people and rescued crews housed in FOB Ithaca's directory. */
public final class FobIthacaContacts {
    // Historical identity retained only to hide the retired contact in saves.
    public static final String LIAISON_PERSON_ID =
            "chief_navigator_ithaca_hegemony_liaison";
    public static final String SPARTAN_CAPTAIN_PERSON_ID =
            "chief_navigator_ithaca_spartan_captain";
    public static final String LEAGUE_SURVIVOR_PERSON_ID =
            "chief_navigator_ithaca_league_survivor_leader";
    public static final String KSHAYA_REFUGEE_PERSON_ID =
            "chief_navigator_ithaca_kshaya_refugee_mother";

    public static final String LIAISON_MARKER =
            "$chief_navigator_ithaca_liaison";
    public static final String MENELAUS_MARKER =
            "$chief_navigator_ithaca_menelaus_contact";
    private static final String LEGACY_SINNI_MARKER =
            "$chief_navigator_ithaca_sinni_contact";
    public static final String SPARTAN_CAPTAIN_MARKER =
            "$chief_navigator_ithaca_spartan_captain_contact";
    public static final String HEGEMONY_EXILE_MARKER =
            "$chief_navigator_ithaca_hegemony_exile_contact";
    public static final String LEAGUE_SURVIVOR_MARKER =
            "$chief_navigator_ithaca_league_survivor_contact";
    public static final String KSHAYA_REFUGEE_MARKER =
            "$chief_navigator_ithaca_kshaya_refugee_contact";

    private static final String PENDING_RESCUE =
            "$chief_navigator_ithaca_rescue_in_transit";
    private static final String ITHACA_RESIDENT =
            "$chief_navigator_ithaca_contact_resident";
    private static final String RESCUE_CONTACT_OWNER =
            "$chief_navigator_ithaca_rescue_contact_owned_v1";
    private static final String STATIC_CONTACT_OWNER =
            "$chief_navigator_ithaca_static_contact_owned_v1";
    private static final String SPARTAN_CREATION_LATCH =
            "$chief_navigator_ithaca_spartan_captain_created_v1";
    private static final String LEAGUE_CREATION_LATCH =
            "$chief_navigator_ithaca_league_survivor_created_v1";
    private static final String KSHAYA_CREATION_LATCH =
            "$chief_navigator_ithaca_kshaya_refugee_created_v1";
    private static final String RESCUE_CONTACT_SERIAL =
            "$chief_navigator_ithaca_rescue_contact_serial";
    private static final float ARRIVAL_RADIUS = 4200f;
    static final int LEAGUE_DELIVERY_CREW = 254;
    static final String LEAGUE_DELIVERY_CREW_GRANTED =
            "$chief_navigator_ithaca_league_delivery_crew_granted_v1";

    private FobIthacaContacts() { }

    public static void ensureForStation(SectorEntityToken station) {
        if (Global.getSector() == null
                || !OdysseyExpanseSystem.isFobIthaca(station)) {
            return;
        }

        PersonAPI menelaus = MenelausTrial.getOrCreateMenelaus();
        if (MenelausTrial.isOwnedMenelaus(menelaus)) {
            menelaus.getMemoryWithoutUpdate().set(MENELAUS_MARKER, true);
            markResident(menelaus, station);
        }

        // Sinni is the player's navigator and retains her permanent Eventide
        // contact. Older builds mirrored her into Ithaca's market directory;
        // clear that presentation-only marker during save migration.
        if (Global.getSector().getMemoryWithoutUpdate().getBoolean(
                SinniBarEvent.STARTED)) {
            PersonAPI sinni = SinniContact.getOrCreatePerson();
            if (SinniContact.isOwnedPerson(sinni)) {
                sinni.getMemoryWithoutUpdate().unset(LEGACY_SINNI_MARKER);
            }
        }

        PersonAPI captain = getOrCreateSpartanCaptain();
        if (captain != null) {
            captain.getMemoryWithoutUpdate().set(
                    SPARTAN_CAPTAIN_MARKER, true);
            markResident(captain, station);
        }

        if (Global.getSector().getMemoryWithoutUpdate().getBoolean(
                AviciHabitatEncounter.REFUGEES_RESCUED)) {
            PersonAPI refugee = getOrCreateKshayaRefugee();
            if (refugee != null) {
                refugee.getMemoryWithoutUpdate().set(
                        KSHAYA_REFUGEE_MARKER, true);
                markResident(refugee, station);
            }
        }

        if (!isPlayerAtStation(station)) return;
        settleRescuedContacts(station);
    }

    /** Authored residents shown by Ithaca's standalone comm directory. */
    public static List<PersonAPI> getDirectoryPeople() {
        List<PersonAPI> result = new ArrayList<PersonAPI>();
        addIfResident(result, MenelausTrial.getOrCreateMenelaus());
        addIfResident(result, getOrCreateSpartanCaptain());
        if (Global.getSector().getMemoryWithoutUpdate().getBoolean(
                AviciHabitatEncounter.REFUGEES_RESCUED)) {
            addIfResident(result, getOrCreateKshayaRefugee());
        }
        for (ImportantPeopleAPI.PersonDataAPI data : Global.getSector()
                .getImportantPeople().getPeopleCopy()) {
            PersonAPI person = data.getPerson();
            if (person != null && person.getMemoryWithoutUpdate()
                    .getBoolean(ITHACA_RESIDENT)) {
                addIfResident(result, person);
            }
        }
        return result;
    }

    public static PersonAPI getOrCreateSpartanCaptain() {
        ImportantPeopleAPI people = Global.getSector().getImportantPeople();
        MemoryAPI sectorMemory = Global.getSector().getMemoryWithoutUpdate();
        PersonAPI captain = people.getPerson(SPARTAN_CAPTAIN_PERSON_ID);
        if (captain == null) {
            if (sectorMemory.getBoolean(SPARTAN_CREATION_LATCH)) return null;
            sectorMemory.set(SPARTAN_CREATION_LATCH, true);
            captain = Global.getFactory().createPerson();
            captain.setId(SPARTAN_CAPTAIN_PERSON_ID);
            captain.setName(new FullName(
                    "Aias", "Kleon", FullName.Gender.MALE));
            captain.setGender(FullName.Gender.MALE);
            captain.setFaction(TaskForceSpartanFaction.ID);
            captain.setRankId(Ranks.SPACE_CAPTAIN);
            captain.setPostId(Ranks.POST_BASE_COMMANDER);
            captain.setImportance(PersonImportance.HIGH);
            captain.setVoice("soldier");
            captain.setPersonality(Personalities.STEADY);
            captain.setPortraitSprite(Global.getSettings().getSpriteName(
                    "characters", "chief_navigator_spartan_officer"));
            captain.getMemoryWithoutUpdate().set(STATIC_CONTACT_OWNER, true);
            captain.getMemoryWithoutUpdate().set(
                    SPARTAN_CAPTAIN_MARKER, true);
            people.addPerson(captain);
        } else if (!isOwnedStaticContact(
                captain, SPARTAN_CAPTAIN_MARKER)) {
            sectorMemory.set(SPARTAN_CREATION_LATCH, true);
            return null;
        } else {
            sectorMemory.set(SPARTAN_CREATION_LATCH, true);
        }
        // Aias owns Ithaca's administrative command role. Reapply it on
        // every lookup so existing saves move Base Commander off Menelaus.
        captain.setRankId(Ranks.SPACE_CAPTAIN);
        captain.setPostId(Ranks.POST_BASE_COMMANDER);
        return captain;
    }

    public static PersonAPI getOrCreateLeagueSurvivorLeader() {
        ImportantPeopleAPI people = Global.getSector().getImportantPeople();
        MemoryAPI sectorMemory = Global.getSector().getMemoryWithoutUpdate();
        PersonAPI leader = people.getPerson(LEAGUE_SURVIVOR_PERSON_ID);
        if (leader == null) {
            if (sectorMemory.getBoolean(LEAGUE_CREATION_LATCH)) return null;
            sectorMemory.set(LEAGUE_CREATION_LATCH, true);
            leader = Global.getSector().getFaction(Factions.PERSEAN)
                    .createRandomPerson(FullName.Gender.ANY);
            leader.setId(LEAGUE_SURVIVOR_PERSON_ID);
            leader.setRankId(Ranks.SPACE_CAPTAIN);
            leader.setPostId(Ranks.POST_FLEET_COMMANDER);
            leader.setImportance(PersonImportance.MEDIUM);
            leader.setVoice("soldier");
            leader.getMemoryWithoutUpdate().set(STATIC_CONTACT_OWNER, true);
            leader.getMemoryWithoutUpdate().set(
                    LEAGUE_SURVIVOR_MARKER, true);
            people.addPerson(leader);
        } else if (!isOwnedStaticContact(
                leader, LEAGUE_SURVIVOR_MARKER)) {
            sectorMemory.set(LEAGUE_CREATION_LATCH, true);
            return null;
        } else {
            sectorMemory.set(LEAGUE_CREATION_LATCH, true);
        }
        return leader;
    }

    /** The mother and child rescued from Kshaya, now settled at Ithaca. */
    public static PersonAPI getOrCreateKshayaRefugee() {
        if (Global.getSector() == null
                || !Global.getSector().getMemoryWithoutUpdate().getBoolean(
                        AviciHabitatEncounter.REFUGEES_RESCUED)) {
            return null;
        }
        ImportantPeopleAPI people = Global.getSector().getImportantPeople();
        MemoryAPI sectorMemory = Global.getSector().getMemoryWithoutUpdate();
        PersonAPI refugee = people.getPerson(KSHAYA_REFUGEE_PERSON_ID);
        if (refugee == null) {
            if (sectorMemory.getBoolean(KSHAYA_CREATION_LATCH)) return null;
            sectorMemory.set(KSHAYA_CREATION_LATCH, true);
            refugee = Global.getSector().getFaction(Factions.INDEPENDENT)
                    .createRandomPerson(FullName.Gender.FEMALE);
            refugee.setId(KSHAYA_REFUGEE_PERSON_ID);
            refugee.setName(new FullName(
                    "Mira", "Sen", FullName.Gender.FEMALE));
            refugee.setRankId(Ranks.CITIZEN);
            refugee.setPostId(Ranks.POST_CITIZEN);
            refugee.setImportance(PersonImportance.MEDIUM);
            refugee.setVoice("civilian");
            refugee.setPersonality(Personalities.STEADY);
            refugee.getMemoryWithoutUpdate().set(STATIC_CONTACT_OWNER, true);
            refugee.getMemoryWithoutUpdate().set(
                    KSHAYA_REFUGEE_MARKER, true);
            people.addPerson(refugee);
        } else if (!isOwnedStaticContact(
                refugee, KSHAYA_REFUGEE_MARKER)) {
            sectorMemory.set(KSHAYA_CREATION_LATCH, true);
            return null;
        } else {
            sectorMemory.set(KSHAYA_CREATION_LATCH, true);
        }
        return refugee;
    }

    /** Abstracts the damaged group into the player's convoy until Ithaca. */
    public static PersonAPI embarkRescueGroup(
            CampaignFleetAPI fleet,
            boolean hegemonyExiles) {
        if (fleet == null || Global.getSector() == null
                || !OdysseyStrandedFleetsScript
                        .isRescueEscortCandidate(fleet)
                || TroyArrivalScript.isFleetBusyForMutation(fleet)) {
            return null;
        }
        PersonAPI leader = fleet.getCommander();
        ImportantPeopleAPI people = Global.getSector().getImportantPeople();
        if (leader == null
                || people.containsPerson(leader)
                && !isOwnedRescueContact(leader)) {
            leader = fleet.getFaction().createRandomPerson();
        }

        PersonAPI contact = markRescueInTransit(leader, hegemonyExiles);
        if (fleet.getContainingLocation() != null) {
            fleet.getContainingLocation().removeEntity(fleet);
        }
        return contact;
    }

    /** Records rescued League personnel without spawning a survivor fleet. */
    public static PersonAPI embarkLeagueSurvivorsFromBattle() {
        if (Global.getSector() == null) return null;
        return markRescueInTransit(
                getOrCreateLeagueSurvivorLeader(), false);
    }

    private static PersonAPI markRescueInTransit(
            PersonAPI leader,
            boolean hegemonyExiles) {
        if (leader == null) return null;
        ImportantPeopleAPI people = Global.getSector().getImportantPeople();
        if (people.containsPerson(leader)
                && !isOwnedRescueContact(leader)) {
            return null;
        }
        if (leader.getId() == null || leader.getId().trim().isEmpty()
                || people.getPerson(leader.getId()) != leader) {
            MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
            int serial = memory.contains(RESCUE_CONTACT_SERIAL)
                    ? memory.getInt(RESCUE_CONTACT_SERIAL) : 0;
            String id;
            do {
                id = "chief_navigator_ithaca_rescued_contact_" + serial++;
            } while (people.getPerson(id) != null);
            memory.set(RESCUE_CONTACT_SERIAL, serial);
            leader.setId(id);
        }
        if (!people.containsPerson(leader)) people.addPerson(leader);

        leader.getMemoryWithoutUpdate().set(RESCUE_CONTACT_OWNER, true);
        leader.setImportance(PersonImportance.MEDIUM);
        leader.setRankId(Ranks.SPACE_CAPTAIN);
        leader.setPostId(Ranks.POST_FLEET_COMMANDER);
        leader.getMemoryWithoutUpdate().set(PENDING_RESCUE, true);
        leader.getMemoryWithoutUpdate().set(
                hegemonyExiles
                        ? HEGEMONY_EXILE_MARKER
                        : LEAGUE_SURVIVOR_MARKER,
                true);
        return leader;
    }

    private static void settleRescuedContacts(SectorEntityToken station) {
        ImportantPeopleAPI people = Global.getSector().getImportantPeople();
        for (ImportantPeopleAPI.PersonDataAPI data : people.getPeopleCopy()) {
            PersonAPI person = data.getPerson();
            if (person == null || !person.getMemoryWithoutUpdate()
                    .getBoolean(PENDING_RESCUE)) {
                continue;
            }
            person.getMemoryWithoutUpdate().unset(PENDING_RESCUE);
            markResident(person, station);
            Global.getSector().getCampaignUI().addMessage(
                    person.getNameString()
                            + " and the surviving crew have taken quarters at FOB Ithaca.",
                    Misc.getHighlightColor());
        }
    }

    private static void markResident(
            PersonAPI person, SectorEntityToken station) {
        if (person == null || !isOwnedIthacaContact(person)) return;
        person.getMemoryWithoutUpdate().set(ITHACA_RESIDENT, true);
        person.setMarket(null);
        ImportantPeopleAPI.PersonDataAPI data = Global.getSector()
                .getImportantPeople().getData(person);
        if (data != null && data.getLocation() != null) {
            data.getLocation().setMarket(null);
            data.getLocation().setEntity(station);
        }
    }

    /** The first resident conversation about the survivors' future offers crew. */
    public static boolean canGrantLeagueConversationCrew(
            SectorEntityToken station, PersonAPI survivor) {
        if (Global.getSector() == null
                || !OdysseyExpanseSystem.isFobIthaca(station)
                || !isPlayerAtStation(station)) return false;
        MemoryAPI sectorMemory = Global.getSector().getMemoryWithoutUpdate();
        if (sectorMemory.getBoolean(LEAGUE_DELIVERY_CREW_GRANTED)) return false;
        PersonAPI leader = Global.getSector().getImportantPeople()
                .getPerson(LEAGUE_SURVIVOR_PERSON_ID);
        if (survivor == null || leader != survivor
                || !isOwnedIthacaContact(leader)
                || !leader.getMemoryWithoutUpdate().getBoolean(LEAGUE_SURVIVOR_MARKER)
                || !leader.getMemoryWithoutUpdate().getBoolean(ITHACA_RESIDENT)
                || leader.getMemoryWithoutUpdate().getBoolean(PENDING_RESCUE)) return false;
        return Global.getSector().getPlayerFleet().getCargo() != null;
    }

    /** Called only from the League commander's future-topic response. */
    public static boolean grantLeagueConversationCrew(
            SectorEntityToken station, PersonAPI survivor) {
        if (!canGrantLeagueConversationCrew(station, survivor)) return false;
        CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
        cargo.addCrew(LEAGUE_DELIVERY_CREW);
        Global.getSector().getMemoryWithoutUpdate().set(
                LEAGUE_DELIVERY_CREW_GRANTED, true);
        Global.getSector().getCampaignUI().addMessage(
                LEAGUE_DELIVERY_CREW
                        + " rescued League crew have joined your fleet at FOB Ithaca.",
                Misc.getHighlightColor());
        return true;
    }

    static boolean isOwnedIthacaContact(PersonAPI person) {
        if (person == null) return false;
        MemoryAPI memory = person.getMemoryWithoutUpdate();
        return memory.getBoolean(RESCUE_CONTACT_OWNER)
                || memory.getBoolean(STATIC_CONTACT_OWNER)
                || memory.getBoolean(MENELAUS_MARKER)
                || memory.getBoolean(SPARTAN_CAPTAIN_MARKER)
                || memory.getBoolean(LIAISON_MARKER)
                || memory.getBoolean(HEGEMONY_EXILE_MARKER)
                || memory.getBoolean(LEAGUE_SURVIVOR_MARKER)
                || memory.getBoolean(KSHAYA_REFUGEE_MARKER);
    }

    private static boolean isOwnedStaticContact(
            PersonAPI person, String historicalRoleMarker) {
        if (person == null) return false;
        MemoryAPI memory = person.getMemoryWithoutUpdate();
        return memory.getBoolean(STATIC_CONTACT_OWNER)
                || memory.getBoolean(historicalRoleMarker);
    }

    private static boolean isOwnedRescueContact(PersonAPI person) {
        if (person == null) return false;
        MemoryAPI memory = person.getMemoryWithoutUpdate();
        return memory.getBoolean(RESCUE_CONTACT_OWNER)
                || memory.getBoolean(STATIC_CONTACT_OWNER)
                || memory.getBoolean(HEGEMONY_EXILE_MARKER)
                || memory.getBoolean(LEAGUE_SURVIVOR_MARKER);
    }

    private static void addIfResident(
            List<PersonAPI> result, PersonAPI person) {
        // The catch-all resident scan also sees old serialized liaisons.
        // Exclude their exact identity or role without deleting the person.
        if (person != null
                && !LIAISON_PERSON_ID.equals(person.getId())
                && !person.getMemoryWithoutUpdate().getBoolean(LIAISON_MARKER)
                && person.getMemoryWithoutUpdate()
                .getBoolean(ITHACA_RESIDENT)
                && !result.contains(person)) {
            result.add(person);
        }
    }

    private static boolean isPlayerAtStation(SectorEntityToken station) {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        return player != null
                && player.getContainingLocation()
                        == station.getContainingLocation()
                && Misc.getDistance(player, station) <= ARRIVAL_RADIUS;
    }
}
