package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.comm.*;
import com.fs.starfarer.api.campaign.rules.*;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc.Token;
import com.fs.starfarer.api.util.Misc.TokenType;
import data.campaign.rulecmd.ChiefNavigatorPostLaborHuntCMD;
import java.awt.Color;
import java.nio.file.*;
import java.util.*;
import static chiefnavigator.quest.BudaiSalvageRewardRegression.*;

/** Actual hunt lifecycle and Kleon command against mocked campaign services. */
public final class PostLaborHuntRegression {
    private static final class Fixture {
        final Map<String, Object> flags = new HashMap<>(), personFlags = new HashMap<>();
        final List<IntelInfoPlugin> entries = new ArrayList<>();
        final List<StarSystemAPI> systems = new ArrayList<>();
        final MemoryAPI global = mem(flags), local = mem(personFlags);
        final PersonAPI person;
        final SectorEntityToken station;
        Object context;

        Fixture() {
            flags.put("$chief_navigator_menelaus_labor_reports_migrated_v1", true);
            flags.put("$chief_navigator_menelaus_final_labor_forced_v1", true);
            flags.put(MenelausTrial.ACCEPTED, true);
            personFlags.put(FobIthacaContacts.SPARTAN_CAPTAIN_MARKER, true);
            person = mock(PersonAPI.class, (n, a) -> n.equals("getId")
                    ? FobIthacaContacts.SPARTAN_CAPTAIN_PERSON_ID
                    : n.equals("getMemoryWithoutUpdate") ? local : null);
            context = person;
            Map<String, Object> stationFlags = new HashMap<>();
            stationFlags.put("$chief_navigator_fob_ithaca_owned_v1", true);
            MemoryAPI stationMemory = mem(stationFlags);
            station = mock(SectorEntityToken.class, (n, a) -> n.equals("getId")
                    ? "chief_navigator_fob_ithaca" : n.equals("getCustomEntityType")
                    ? "chief_navigator_fob_ithaca_campaign"
                    : n.equals("getMemoryWithoutUpdate") ? stationMemory : null);
            IntelManagerAPI manager = mock(IntelManagerAPI.class, (n, a) -> {
                if (n.equals("getIntel")) return new ArrayList<>(entries);
                if (n.equals("hasIntel")) return entries.contains(a[0]);
                if (n.equals("addIntel")) entries.add((IntelInfoPlugin) a[0]);
                return null;
            });
            Global.setSector(mock(SectorAPI.class, (n, a) -> {
                if (n.equals("getMemoryWithoutUpdate")) return global;
                if (n.equals("getIntelManager")) return manager;
                if (n.equals("getStarSystems")) return systems;
                if (n.equals("getCampaignUI")) return mock(CampaignUIAPI.class, (x, y) -> null);
                if (n.startsWith("create")) throw new AssertionError("Hunts must not create topology");
                return null;
            }));
        }

        void completeLabor() {
            flags.put(MenelausTrial.GAUTAMA_DEFEATED, true);
            flags.put(MenelausTrial.FINAL_DEBRIEF_COMPLETE, true);
        }

        PostLaborHuntIntel hunt(String target) {
            for (IntelInfoPlugin item : entries) {
                PostLaborHuntIntel intel = (PostLaborHuntIntel) item;
                if (intel.getName().startsWith("Hunt " + target)) return intel;
            }
            throw new AssertionError("Missing hunt: " + target);
        }

        boolean command(String action, MemoryAPI selectedMemory) {
            InteractionDialogPlugin plugin = mock(InteractionDialogPlugin.class,
                    (n, a) -> n.equals("getContext") ? context : null);
            InteractionDialogAPI dialog = mock(InteractionDialogAPI.class,
                    (n, a) -> n.equals("getPlugin") ? plugin
                    : n.equals("getInteractionTarget") ? station : null);
            Map<String, MemoryAPI> memories = new HashMap<>();
            memories.put(MemKeys.LOCAL, selectedMemory);
            memories.put(MemKeys.GLOBAL, global);
            return new ChiefNavigatorPostLaborHuntCMD().execute("test", dialog,
                    Collections.singletonList(new Token(action, TokenType.LITERAL)), memories);
        }

        StarSystemAPI system(String id, SectorEntityToken center, SectorEntityToken objective) {
            Map<String, Object> systemFlags = new HashMap<>();
            systemFlags.put("$chief_navigator_odyssey_authored_system_v1", true);
            MemoryAPI systemMemory = mem(systemFlags);
            StarSystemAPI system = mock(StarSystemAPI.class, (n, a) -> {
                if (n.equals("getOptionalUniqueId")) return id;
                if (n.equals("getMemoryWithoutUpdate")) return systemMemory;
                if (n.equals("getCenter")) return center;
                if (n.equals("getEntityById")) return objective;
                return null;
            });
            systems.add(system);
            return system;
        }
    }

    public static void main(String[] args) throws Exception {
        SectorAPI savedSector = Global.getSector();
        SettingsAPI savedSettings = Global.getSettings();
        Global.setSettings(mock(SettingsAPI.class, (n, a) -> n.equals("getColor") ? Color.WHITE : null));
        try {
            Fixture f = new Fixture();
            PostLaborHuntIntel.sync(null);
            check(f.entries.isEmpty(), "Active Labor cannot start hunts");
            f.flags.put(MenelausTrial.GAUTAMA_DEFEATED, true);
            PostLaborHuntIntel.sync(null);
            check(f.entries.isEmpty(), "Victory alone cannot start hunts before debrief");
            check(!f.command("budaiAvailable", f.local), "No pre-completion Kleon topic");
            f.completeLabor();
            for (int i = 0; i < 10; i++) PostLaborHuntIntel.sync(null);
            check(f.entries.size() == 2, "Two independent quests, not duplicated by upkeep");
            check(description(f.hunt("Budai")).contains("Captain Aias Kleon"), "Budai starts at Kleon");
            check(description(f.hunt("Ungaikyo")).contains("Captain Aias Kleon"), "Ungaikyo starts at Kleon");
            check(!description(f.hunt("Budai")).contains("Avici"), "No location before briefing");
            f.system(OdysseyExpanseSystem.ASHEN_VERGE_ID, null, f.station);
            check(f.hunt("Budai").getMapLocation(null) == f.station
                    && f.hunt("Ungaikyo").getMapLocation(null) == f.station,
                    "Both unbriefed hunts mark Ithaca rather than reveal the target systems");
            check(f.command("budaiAvailable", f.local), "Owned Kleon topic available after debrief");
            check(!f.command("briefBudai", f.local), "Wrong selected option cannot change state");
            f.local.set("$option", "chief_navigator_ithaca_hunt_budai");
            check(f.command("briefBudai", f.local), "Budai briefing advances its quest");
            check(f.command("briefBudai", f.local) && f.entries.size() == 2, "Reasking is safe");
            check(description(f.hunt("Budai")).contains("Avici"), "Budai location revealed");
            check(!description(f.hunt("Ungaikyo")).contains("Last Light"), "Other quest stays unbriefed");
            check(!f.command("briefUngaikyo", f.local), "Wrong option cannot brief other target");
            check(!f.command("budaiAvailable", f.global), "Foreign local memory rejected");
            f.personFlags.clear();
            check(!f.command("budaiAvailable", f.local), "Unmarked person rejected");
            f.personFlags.put(FobIthacaContacts.SPARTAN_CAPTAIN_MARKER, true);
            f.context = mock(PersonAPI.class, (n, a) -> n.equals("getId") ? "foreign_person"
                    : n.equals("getMemoryWithoutUpdate") ? f.local : null);
            check(!f.command("budaiAvailable", f.local), "Marked foreign identity rejected");
            f.context = null;
            check(!f.command("budaiAvailable", f.local), "Missing person context rejected");
            f.context = f.person;
            f.local.set("$option", "chief_navigator_ithaca_hunt_ungaikyo");
            check(f.command("briefUngaikyo", f.local), "Owned Ungaikyo question advances its hunt");
            check(description(f.hunt("Ungaikyo")).contains("Last Light"), "Last Light revealed");
            check(f.hunt("Budai").getMapLocation(null) == null, "Missing system left missing");
            SectorEntityToken aviciCenter = mock(SectorEntityToken.class, (n, a) -> null);
            SectorEntityToken lastLightCenter = mock(SectorEntityToken.class, (n, a) -> null);
            SectorEntityToken guardian = mock(SectorEntityToken.class, (n, a) -> null);
            f.system(OdysseyExpanseSystem.MESSINA_ID, aviciCenter, null);
            f.system(OdysseyExpanseSystem.LAST_LIGHT_ID, lastLightCenter, guardian);
            check(f.hunt("Budai").getMapLocation(null) == aviciCenter,
                    "Budai briefing marks Avici, not a live fleet tracking coordinate");
            check(f.hunt("Ungaikyo").getMapLocation(null) == guardian,
                    "Ungaikyo briefing marks the existing Guardian in Last Light");
            f.flags.put(OdysseyPredatorScript.BUDAI_DEFEATED, true);
            PostLaborHuntIntel.sync(null);
            check(f.hunt("Budai").isEnding() && !f.hunt("Ungaikyo").isEnding(), "Independent kill progress");
            check(!f.command("budaiAvailable", f.local), "Dead target question hidden");
            f.flags.put(OdysseyPredatorScript.GAUTAMA_FINAL_REMATCH_DEFEATED, true);
            PostLaborHuntIntel.sync(null);
            check(f.hunt("Ungaikyo").isEnding(), "Permanent Ungaikyo defeat completes quest");
            f.entries.clear();
            f.flags.remove(OdysseyPredatorScript.BUDAI_DEFEATED);
            PostLaborHuntIntel.sync(null);
            check(f.entries.isEmpty(), "Retired entries are not recreated");
            Fixture early = new Fixture();
            early.completeLabor();
            early.flags.put(OdysseyPredatorScript.BUDAI_DEFEATED, true);
            PostLaborHuntIntel.sync(null);
            check(early.entries.size() == 1, "Pre-killed target does not create impossible hunt");
            early.flags.put(OdysseyPredatorScript.GAUTAMA_FINAL_REMATCH_DEFEATED, true);
            PostLaborHuntIntel.sync(null);
            check(early.hunt("Ungaikyo").isEnding(), "Kill without briefing still completes hunt");
            String csv = Files.readString(Path.of("data/campaign/rules.csv"));
            String ink = Files.readString(Path.of("dialogue/ithaca_residents.ink"));
            for (String topic : new String[] {
                    "Where can I find Budai?", "What happens now with the Starving Threat?"}) {
                check(csv.contains(topic) && ink.contains(topic), "Runtime/Ink topic mirror");
            }
            System.out.println("PASS: post-debrief hunt gating, independent Kleon briefing/kill progress, "
                    + "pre-killed targets, one-time creation and scoped commands (mocked campaign; no live UI).");
        } finally {
            Global.setSector(savedSector);
            Global.setSettings(savedSettings);
        }
    }

    private static String description(PostLaborHuntIntel intel) {
        StringBuilder text = new StringBuilder();
        intel.createSmallDescription(mock(TooltipMakerAPI.class, (n, a) -> {
            if (n.equals("addPara")) text.append(Arrays.deepToString(a));
            return null;
        }), 400f, 300f);
        return text.toString();
    }

    private static MemoryAPI mem(Map<String, Object> values) {
        return mock(MemoryAPI.class, (n, a) -> {
            if (n.equals("getString")) return values.get(a[0]);
            if (n.equals("getBoolean")) return Boolean.TRUE.equals(values.get(a[0]));
            if (n.equals("set")) {
                check(a.length == 2, "Hunt flags must not expire");
                values.put((String) a[0], a[1]);
            }
            return null;
        });
    }
}
