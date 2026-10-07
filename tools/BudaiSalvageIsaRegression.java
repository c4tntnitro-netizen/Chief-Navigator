package chiefnavigator.quest;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.VisualPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.campaign.rules.RuleAPI;
import com.fs.starfarer.api.campaign.rules.RulesAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static chiefnavigator.quest.BudaiSalvageRewardRegression.*;

/** Real scene callbacks retain permanent eligibility independently of roster/IDs. */
public final class BudaiSalvageIsaRegression {
    private static final String UNLOCK = "$chief_navigator_isa_refraction_unlocked_v1";
    private static final String PORTRAIT = "graphics/portraits/ship_trophy_isa.png";
    private static final class Fixture {
        final Map<String, Object> saved = new HashMap<>();
        final List<String> stages = new ArrayList<>();
        final List<PersonAPI> portraits = new ArrayList<>();
        boolean hallEnabled = true;
        boolean failPortrait;
        int wreckDisplays;
        int displayPeople;
        int weapons;
        int rewardCalls;
        String displayId;
        String displayImage;
        FullName displayName;
        final MemoryAPI memory = memory(saved);
        final PersonAPI display = mock(PersonAPI.class, (name, args) -> {
            if (name.equals("getId")) return displayId;
            if (name.equals("getNameString")) return displayName.getFullName();
            if (name.equals("getPortraitSprite")) return displayImage;
            if (name.equals("setId")) displayId = (String) args[0];
            if (name.equals("setName")) displayName = (FullName) args[0];
            if (name.equals("setPortraitSprite")) displayImage = (String) args[0];
            return null;
        });
        final InteractionDialogAPI dialog;

        Fixture() {
            ModManagerAPI mods = mock(ModManagerAPI.class, (name, args) -> {
                if (name.equals("isModEnabled")) {
                    check("ship_trophy_room".equals(args[0]), "Use HoT's installed mod ID for the portrait asset");
                    return hallEnabled;
                }
                return null;
            });
            Global.setSettings(mock(SettingsAPI.class, (name, args) -> {
                if (name.equals("getModManager")) return mods;
                if (name.equals("getSpriteName")) {
                    check(hallEnabled && "characters".equals(args[0]) && "ship_trophy_isa".equals(args[1]), "Use HoT's registered Isa portrait only when enabled");
                    return PORTRAIT;
                }
                return null;
            }));
            CargoAPI cargo = mock(CargoAPI.class, (name, args) -> {
                if (name.equals("addWeapons")) {
                    check(BudaiSalvageDialogPlugin.WEAPON_ID.equals(args[0]), "Correct salvage weapon");
                    weapons += ((Number) args[1]).intValue();
                    rewardCalls++;
                }
                return null;
            });
            CampaignFleetAPI fleet = mock(CampaignFleetAPI.class, (name, args) -> {
                if (name.equals("getFleetData")) throw new AssertionError("Neither eligibility nor portrait rendering may inspect roster IDs/names");
                if (name.equals("getCargo")) return cargo;
                if (name.equals("getMemoryWithoutUpdate")) return memory;
                return null;
            });
            RuleAPI rule = mock(RuleAPI.class, (name, args) ->
                    name.equals("getOptions") || name.equals("getScriptCopy") ? List.of() : null);
            RulesAPI rules = mock(RulesAPI.class, (name, args) -> {
                if (name.equals("getBestMatching")) { stages.add((String) args[1]); return rule; }
                return null;
            });
            Global.setSector(mock(SectorAPI.class, (name, args) -> {
                if (name.equals("getPlayerFleet")) return fleet;
                if (name.equals("getMemoryWithoutUpdate") || name.equals("getPlayerMemoryWithoutUpdate")) return memory;
                if (name.equals("getRules")) return rules;
                if (name.equals("getImportantPeople")) throw new AssertionError("Do not look up another mod's person ID");
                return null;
            }));
            FleetMemberAPI wreck = mock(FleetMemberAPI.class, (name, args) -> null);
            Global.setFactory(mock(FactoryAPI.class, (name, args) -> {
                if (name.equals("createPerson")) {
                    if (failPortrait) throw new IllegalStateException("Simulated presentation failure");
                    displayPeople++;
                    return display;
                }
                check(name.equals("createFleetMember") && "onslaught_mk1_Ancient".equals(args[1]), "Keep the consumed Onslaught visual");
                return wreck;
            }));
            VisualPanelAPI visual = mock(VisualPanelAPI.class, (name, args) -> {
                if (name.equals("showFleetMemberInfo")) wreckDisplays++;
                if (name.equals("showPersonInfo")) portraits.add((PersonAPI) args[0]);
                return null;
            });
            OptionPanelAPI options = mock(OptionPanelAPI.class, (name, args) -> null);
            dialog = mock(InteractionDialogAPI.class, (name, args) -> {
                if (name.equals("getVisualPanel")) return visual;
                if (name.equals("getOptionPanel")) return options;
                if (name.equals("getInteractionTarget")) return fleet;
                if (name.equals("dismiss")) throw new AssertionError("Authored scene must remain open");
                return null;
            });
        }

        void scene(boolean assisted, boolean showPortrait) {
            BudaiSalvageDialogPlugin.request();
            BudaiSalvageDialogPlugin plugin = new BudaiSalvageDialogPlugin();
            plugin.init(dialog);
            String branch = assisted ? "isa" : "salvage_chief";
            check(stages.equals(List.of("ChiefNavigatorBudaiSalvage_opening_" + branch)), "Permanent unlock selects the correct opening");
            check(wreckDisplays == 1 && portraits.isEmpty(), "Opening retains the consumed Onslaught visual");
            plugin.optionSelected("Continue.", "assess_" + branch);
            check(portraits.equals(showPortrait ? List.of(display) : List.of()), "Assessment shows Isa's page-local portrait");
            if (showPortrait) {
                check(displayPeople == 1 && "chief_navigator_budai_salvage_isa".equals(displayId)
                        && "Isa Leicester".equals(displayName.getFullName()) && PORTRAIT.equals(displayImage),
                        "Temporary portrait identity uses Isa's actual HoT image without person-ID collisions");
            }
            plugin.optionSelected("Begin the extraction.", "extract_" + branch);
            plugin.optionSelected("Begin the extraction.", "extract_" + branch);
            check(weapons == (assisted ? 2 : 1) && rewardCalls == 1, "Correct reward is granted exactly once");
            check(portraits.size() == (showPortrait ? 3 : 0), "Extraction keeps the portrait visible");
            check(Boolean.TRUE.equals(saved.get(UNLOCK)) == assisted, "Preserve the existing permanent unlock behavior");
        }
    }

    public static void main(String[] args) {
        SectorAPI previousSector = Global.getSector();
        SettingsAPI previousSettings = Global.getSettings();
        FactoryAPI previousFactory = Global.getFactory();
        try {
            Fixture recruited = new Fixture();
            recruited.saved.put("$ship_trophy_room_isa_officer_granted", true);
            recruited.scene(true, true);
            Fixture dismissed = new Fixture();
            dismissed.saved.put(UNLOCK, true);
            dismissed.scene(true, true);
            Fixture unassisted = new Fixture();
            unassisted.scene(false, false);
            Fixture disabledHall = new Fixture();
            disabledHall.saved.put(UNLOCK, true);
            disabledHall.hallEnabled = false;
            disabledHall.scene(true, false);
            Fixture brokenVisual = new Fixture();
            brokenVisual.saved.put(UNLOCK, true);
            brokenVisual.failPortrait = true;
            brokenVisual.scene(true, false);
        } finally {
            Global.setFactory(previousFactory);
            Global.setSettings(previousSettings);
            Global.setSector(previousSector);
        }
        System.out.println("PASS: permanent Isa unlock still grants two weapons after dismissal; HoT portrait appears in assessment/extraction without roster or person-ID lookup; presentation failures cannot change rewards.");
    }
}
