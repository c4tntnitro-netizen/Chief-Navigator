package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.util.Misc.Token;
import com.fs.starfarer.api.util.Misc.TokenType;
import data.campaign.rulecmd.ChiefNavigatorIthacaResidentsCMD;
import java.awt.Color;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lwjgl.util.vector.Vector2f;

/** Headless contracts for the League's one-time, resident-conversation crew reward. */
public final class LeagueSurvivorDeliveryRegression {
    private static final String PENDING =
            "$chief_navigator_ithaca_rescue_in_transit";
    private static final String RESIDENT =
            "$chief_navigator_ithaca_contact_resident";
    private static final String OWNER =
            "$chief_navigator_ithaca_rescue_contact_owned_v1";

    interface Call { Object invoke(String name, Object[] args); }

    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[] {type}, (proxy, method, args) -> {
                    if (method.getName().equals("hashCode")) {
                        return System.identityHashCode(proxy);
                    }
                    if (method.getName().equals("equals")) return proxy == args[0];
                    Object result = call.invoke(method.getName(), args);
                    if (result != null) return result;
                    Class<?> returns = method.getReturnType();
                    if (returns == boolean.class) return false;
                    if (returns == int.class) return 0;
                    if (returns == float.class) return 0f;
                    if (returns == long.class) return 0L;
                    return null;
                }));
    }

    static MemoryAPI memory(Map<String, Object> values) {
        return mock(MemoryAPI.class, (name, args) -> {
            if (name.equals("getBoolean")) return Boolean.TRUE.equals(values.get(args[0]));
            if (name.equals("get")) return values.get(args[0]);
            if (name.equals("getString")) return values.get(args[0]);
            if (name.equals("contains")) return values.containsKey(args[0]);
            if (name.equals("getInt")) return values.getOrDefault(args[0], 0);
            if (name.equals("set")) values.put((String) args[0], args[1]);
            if (name.equals("unset")) values.remove(args[0]);
            return null;
        });
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Fixture {
        final Map<String, Object> saved = new HashMap<>();
        final Map<String, PersonAPI> peopleById = new HashMap<>();
        final List<ImportantPeopleAPI.PersonDataAPI> entries = new ArrayList<>();
        final List<String> messages = new ArrayList<>();
        final LocationAPI ithacaLocation = mock(LocationAPI.class, (name, args) -> null);
        final LocationAPI otherLocation = mock(LocationAPI.class, (name, args) -> null);
        LocationAPI playerLocation = ithacaLocation;
        final Vector2f playerPosition = new Vector2f();
        final SectorEntityToken station;
        final CargoAPI cargo;
        final CampaignFleetAPI player;
        final Map<String, Object> league = new HashMap<>();
        boolean cargoAvailable = true;
        boolean playerAvailable = true;
        int crew = 100;
        int awardCalls;

        Fixture() {
            // Do not create unrelated static residents in this focused harness.
            saved.put("$chief_navigator_menelaus_created_v1", true);
            saved.put("$chief_navigator_ithaca_spartan_captain_created_v1", true);
            MemoryAPI sectorMemory = memory(saved);
            station = station("chief_navigator_fob_ithaca",
                    "chief_navigator_fob_ithaca_campaign", true);
            cargo = mock(CargoAPI.class, (name, args) -> {
                if (name.equals("addCrew")) {
                    check(((Integer) args[0]) == 254, "Every reward must contain exactly 254 crew");
                    crew += (Integer) args[0];
                    awardCalls++;
                }
                if (name.equals("getCrew") || name.equals("getTotalCrew")) return crew;
                return null;
            });
            player = mock(CampaignFleetAPI.class, (name, args) -> {
                if (name.equals("isPlayerFleet")) return true;
                if (name.equals("getContainingLocation")) return playerLocation;
                if (name.equals("getLocation")) return playerPosition;
                if (name.equals("getCargo")) return cargoAvailable ? cargo : null;
                return null;
            });
            ImportantPeopleAPI people = mock(ImportantPeopleAPI.class, (name, args) -> {
                if (name.equals("getPerson")) return peopleById.get(args[0]);
                if (name.equals("getPeopleCopy")) return new ArrayList<>(entries);
                if (name.equals("containsPerson")) return peopleById.containsValue(args[0]);
                return null;
            });
            CampaignUIAPI ui = mock(CampaignUIAPI.class, (name, args) -> {
                if (name.equals("addMessage")) messages.add((String) args[0]);
                return null;
            });
            Global.setSector(mock(SectorAPI.class, (name, args) -> {
                if (name.equals("getMemoryWithoutUpdate")) return sectorMemory;
                if (name.equals("getPlayerFleet")) return playerAvailable ? player : null;
                if (name.equals("getCampaignUI")) return ui;
                if (name.equals("getImportantPeople")) return people;
                return null;
            }));
            league.put(OWNER, true);
            league.put(FobIthacaContacts.LEAGUE_SURVIVOR_MARKER, true);
            person(FobIthacaContacts.LEAGUE_SURVIVOR_PERSON_ID, league);
        }

        SectorEntityToken station(String id, String type, boolean owned) {
            MemoryAPI stationMemory = memory(new HashMap<>(Map.of(
                    "$chief_navigator_fob_ithaca_owned_v1", owned)));
            return mock(SectorEntityToken.class, (name, args) -> {
                if (name.equals("getId")) return id;
                if (name.equals("getCustomEntityType")) return type;
                if (name.equals("getMemoryWithoutUpdate")) return stationMemory;
                if (name.equals("getContainingLocation")) return ithacaLocation;
                if (name.equals("getLocation")) return new Vector2f();
                return null;
            });
        }

        PersonAPI person(String id, Map<String, Object> state) {
            MemoryAPI personMemory = memory(state);
            PersonAPI person = mock(PersonAPI.class, (name, args) -> {
                if (name.equals("getId") || name.equals("getNameString")) return id;
                if (name.equals("getMemoryWithoutUpdate")) return personMemory;
                return null;
            });
            peopleById.put(id, person);
            entries.add(mock(ImportantPeopleAPI.PersonDataAPI.class,
                    (name, args) -> name.equals("getPerson") ? person : null));
            return person;
        }

        PersonAPI leader() { return peopleById.get(FobIthacaContacts.LEAGUE_SURVIVOR_PERSON_ID); }
        boolean claim() { return FobIthacaContacts.grantLeagueConversationCrew(station, leader()); }
        boolean eligible() { return FobIthacaContacts.canGrantLeagueConversationCrew(station, leader()); }
        void visit() { FobIthacaContacts.ensureForStation(station); }
        void delivered() { league.put(RESIDENT, true); league.remove(PENDING); }
        boolean paid() {
            return Boolean.TRUE.equals(saved.get(FobIthacaContacts.LEAGUE_DELIVERY_CREW_GRANTED));
        }
    }

    static Map<String, MemoryAPI> conversationMemory(Fixture fixture, String option) {
        fixture.league.put("$option", option);
        return new HashMap<>(Map.of(
                MemKeys.LOCAL, fixture.leader().getMemoryWithoutUpdate(),
                MemKeys.GLOBAL, memory(fixture.saved)));
    }

    static InteractionDialogAPI conversation(Fixture fixture, Object context) {
        InteractionDialogPlugin plugin = mock(InteractionDialogPlugin.class,
                (name, args) -> name.equals("getContext") ? context : null);
        return mock(InteractionDialogAPI.class, (name, args) -> {
            if (name.equals("getPlugin")) return plugin;
            if (name.equals("getInteractionTarget")) return fixture.station;
            return null;
        });
    }

    static boolean command(String action, InteractionDialogAPI dialog,
            Map<String, MemoryAPI> memory) {
        return new ChiefNavigatorIthacaResidentsCMD().execute("regression", dialog,
                List.of(new Token(action, TokenType.LITERAL)), memory);
    }

    public static void main(String[] args) {
        Global.setSettings(mock(SettingsAPI.class,
                (name, values) -> name.equals("getColor") ? Color.YELLOW : null));
        check(FobIthacaContacts.LEAGUE_DELIVERY_CREW == 254, "The authored crew reward is exactly 254");

        Fixture rescued = new Fixture();
        check(FobIthacaContacts.embarkLeagueSurvivorsFromBattle() != null,
                "The owned survivor leader can embark");
        check(Boolean.TRUE.equals(rescued.league.get(PENDING)) && !rescued.claim()
                        && rescued.crew == 100 && !rescued.paid(),
                "Rescue and in-transit state must grant no usable crew");
        rescued.playerLocation = rescued.otherLocation;
        rescued.visit();
        check(Boolean.TRUE.equals(rescued.league.get(PENDING)) && !rescued.claim(),
                "Being in another system must neither settle nor reward survivors");
        rescued.playerLocation = rescued.ithacaLocation;
        rescued.playerPosition.set(4201f, 0f);
        rescued.visit();
        check(Boolean.TRUE.equals(rescued.league.get(PENDING)) && !rescued.claim(),
                "An Ithaca-system visit outside the arrival radius must not deliver");
        rescued.playerPosition.set(4200f, 0f);
        rescued.visit();
        check(!rescued.league.containsKey(PENDING)
                        && Boolean.TRUE.equals(rescued.league.get(RESIDENT))
                        && rescued.crew == 100 && rescued.awardCalls == 0 && !rescued.paid(),
                "Actual settlement must create the resident but grant no crew before the conversation");
        rescued.visit();
        check(rescued.eligible() && rescued.eligible() && rescued.crew == 100 && !rescued.paid(),
                "Directory rebuilds and pure eligibility queries must never grant crew");

        InteractionDialogAPI talk = conversation(rescued, rescued.leader());
        Map<String, MemoryAPI> future = conversationMemory(rescued, "chief_navigator_league_future");
        check(command("canRecruitLeagueCrew", talk, future) && rescued.crew == 100,
                "The future topic must become eligible without paying during condition evaluation");
        check(command("recruitLeagueCrew", talk, future)
                        && rescued.crew == 354 && rescued.awardCalls == 1 && rescued.paid(),
                "The first future-topic response must grant exactly 254 crew once");
        check(rescued.messages.stream().anyMatch(text -> text.contains("254") && text.contains("joined your fleet")),
                "The campaign must report the actual crew award only after the conversation grants it");
        check(!command("canRecruitLeagueCrew", talk, future)
                        && !command("recruitLeagueCrew", talk, future),
                "Reevaluating or replaying the same response must never pay twice");
        InteractionDialogAPI reopened = conversation(rescued, rescued.leader());
        check(!command("recruitLeagueCrew", reopened,
                        conversationMemory(rescued, "chief_navigator_league_future"))
                        && !rescued.claim() && rescued.crew == 354 && rescued.awardCalls == 1,
                "Re-entering the contact or directly claiming must preserve the single reward");
        rescued.league.put(PENDING, true);
        rescued.league.remove(RESIDENT);
        rescued.visit();
        check(rescued.crew == 354 && rescued.awardCalls == 1,
                "The sector-wide latch must survive local contact-state resets");

        Fixture opening = new Fixture();
        opening.delivered();
        opening.visit();
        check(opening.crew == 100 && !opening.paid(),
                "Merely revisiting already settled residents must not grant the reward");
        InteractionDialogAPI greeting = conversation(opening, opening.leader());
        for (String unrelated : List.of("chief_navigator_league_greeting",
                "chief_navigator_league_story", "chief_navigator_league_sara", "")) {
            Map<String, MemoryAPI> selection = conversationMemory(opening, unrelated);
            check(!command("canRecruitLeagueCrew", greeting, selection)
                            && !command("recruitLeagueCrew", greeting, selection)
                            && opening.crew == 100 && !opening.paid(),
                    "Opening or selecting another topic must never grant crew: " + unrelated);
        }

        Fixture deferred = new Fixture();
        deferred.league.put(PENDING, true);
        deferred.cargoAvailable = false;
        deferred.visit();
        check(Boolean.TRUE.equals(deferred.league.get(RESIDENT)) && !deferred.paid()
                        && deferred.awardCalls == 0,
                "Missing cargo must not consume the reward even if settlement completes");
        check(!deferred.eligible() && !deferred.claim() && !deferred.paid(),
                "A conversation without player cargo must not consume the claim");
        deferred.cargoAvailable = true;
        deferred.visit();
        check(deferred.crew == 100 && !deferred.paid() && deferred.eligible()
                        && deferred.claim() && deferred.crew == 354
                        && deferred.awardCalls == 1 && deferred.paid(),
                "Cargo recovery must leave payout deferred until the future-topic response");

        Fixture identity = new Fixture();
        identity.league.put(PENDING, true);
        for (SectorEntityToken impostor : List.of(
                identity.station("unrelated_station", "chief_navigator_fob_ithaca_campaign", true),
                identity.station("chief_navigator_fob_ithaca", "unrelated_type", true),
                identity.station("chief_navigator_fob_ithaca", "chief_navigator_fob_ithaca_campaign", false))) {
            FobIthacaContacts.ensureForStation(impostor);
            check(!FobIthacaContacts.grantLeagueConversationCrew(impostor, identity.leader())
                            && Boolean.TRUE.equals(identity.league.get(PENDING)) && !identity.paid(),
                    "Only the owned Ithaca center may settle or reward the rescue");
        }
        identity.delivered();
        identity.playerAvailable = false;
        check(!identity.claim() && !identity.paid(), "An absent player fleet must leave the reward unpaid");

        Fixture otherResidents = new Fixture();
        otherResidents.peopleById.remove(FobIthacaContacts.LEAGUE_SURVIVOR_PERSON_ID);
        otherResidents.entries.clear();
        for (String marker : List.of(FobIthacaContacts.HEGEMONY_EXILE_MARKER,
                FobIthacaContacts.KSHAYA_REFUGEE_MARKER)) {
            Map<String, Object> state = new HashMap<>(Map.of(OWNER, true, marker, true, PENDING, true));
            otherResidents.person("other_rescue_" + otherResidents.entries.size(), state);
        }
        otherResidents.visit();
        check(otherResidents.crew == 100 && !otherResidents.paid(),
                "Exile and Kshaya settlements must never produce the League crew reward");
        Map<String, Object> duplicateState = new HashMap<>(Map.of(
                OWNER, true, FobIthacaContacts.LEAGUE_SURVIVOR_MARKER, true, PENDING, true));
        otherResidents.person("noncanonical_league_contact", duplicateState);
        otherResidents.visit();
        PersonAPI duplicate = otherResidents.peopleById.get("noncanonical_league_contact");
        check(!FobIthacaContacts.grantLeagueConversationCrew(otherResidents.station, duplicate)
                        && otherResidents.crew == 100 && !otherResidents.paid(),
                "A different contact must not substitute for the authored survivor leader");

        Fixture wrongContact = new Fixture();
        wrongContact.delivered();
        Map<String, MemoryAPI> selected = conversationMemory(wrongContact, "chief_navigator_league_future");
        PersonAPI foreign = mock(PersonAPI.class, (name, values) -> {
            if (name.equals("getId")) return FobIthacaContacts.LEAGUE_SURVIVOR_PERSON_ID;
            if (name.equals("getMemoryWithoutUpdate")) return memory(new HashMap<>(wrongContact.league));
            return null;
        });
        check(!FobIthacaContacts.canGrantLeagueConversationCrew(wrongContact.station, foreign)
                        && !FobIthacaContacts.grantLeagueConversationCrew(wrongContact.station, foreign),
                "Matching role and ID do not authorize a different person object");
        check(!command("recruitLeagueCrew", conversation(wrongContact, foreign), selected)
                        && !command("recruitLeagueCrew", conversation(wrongContact, null), selected)
                        && !command("recruitLeagueCrew", null, selected),
                "Wrong, missing, or absent live conversation context must fail closed");
        Map<String, MemoryAPI> foreignMemory = conversationMemory(wrongContact, "chief_navigator_league_future");
        foreignMemory.put(MemKeys.LOCAL, foreign.getMemoryWithoutUpdate());
        check(!command("recruitLeagueCrew", conversation(wrongContact, wrongContact.leader()), foreignMemory),
                "Stale foreign person memory must not authorize a payout");
        Map<String, MemoryAPI> noPerson = conversationMemory(wrongContact, "chief_navigator_league_future");
        noPerson.remove(MemKeys.LOCAL);
        check(!command("recruitLeagueCrew", conversation(wrongContact, wrongContact.leader()), noPerson)
                        && wrongContact.crew == 100 && !wrongContact.paid(),
                "Missing person memory must leave the reward unpaid");

        Fixture alreadyPaid = new Fixture();
        alreadyPaid.delivered();
        alreadyPaid.saved.put(FobIthacaContacts.LEAGUE_DELIVERY_CREW_GRANTED, true);
        alreadyPaid.visit();
        check(!alreadyPaid.eligible() && !alreadyPaid.claim()
                        && alreadyPaid.awardCalls == 0 && alreadyPaid.crew == 100,
                "An existing earned claim must never be repaid at its new conversation trigger");

        Fixture unowned = new Fixture();
        unowned.league.clear();
        unowned.league.put(RESIDENT, true);
        check(!unowned.claim() && !unowned.paid(),
                "A same-id person without the authored League role must not receive the reward");
        unowned.league.put(OWNER, true);
        unowned.league.put(FobIthacaContacts.HEGEMONY_EXILE_MARKER, true);
        check(!unowned.claim() && !unowned.paid(),
                "An owned exile accidentally stored at the canonical id must not count as League survivors");
        Global.setSector(null);
        check(!FobIthacaContacts.grantLeagueConversationCrew(identity.station, identity.leader()),
                "An absent sector must fail closed");
        Global.setSettings(null);
        System.out.println("League survivor conversation-reward regression checks passed.");
    }
}
