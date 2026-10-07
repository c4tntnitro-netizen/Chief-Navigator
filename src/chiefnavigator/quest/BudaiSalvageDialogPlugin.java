package chiefnavigator.quest;

import chiefnavigator.weapons.IthacaResearchUpgrades;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import java.util.Map;

/** One-time, rules-authored salvage scene after the player's first Budai kill. */
public final class BudaiSalvageDialogPlugin
        extends ChoicePreservingDialogPlugin {
    public static final String WEAPON_ID =
            "chief_navigator_mountable_heavy_adjudicator";
    public static final int WEAPON_COUNT = 2;
    public static final int UNASSISTED_WEAPON_COUNT = 1;

    static final String PENDING =
            "$chief_navigator_budai_adjudicator_salvage_pending_v1";
    static final String GRANTED =
            "$chief_navigator_budai_heavy_adjudicators_granted_v1";

    private static final String RULE_PREFIX =
            "ChiefNavigatorBudaiSalvage_";
    private static final String WRECK_VARIANT_ID =
            "onslaught_mk1_Ancient";

    private InteractionDialogAPI dialog;
    private Map<String, MemoryAPI> memoryMap;
    private PersonAPI isa;
    private boolean isaAssisted;

    /** Presentation only: Isa need not remain in the active officer roster. */
    static PersonAPI createIsaPortrait() {
        if (Global.getSector() == null || Global.getSettings() == null
                || Global.getSettings().getModManager() == null
                || !Global.getSettings().getModManager().isModEnabled(
                        "ship_trophy_room")) return null;
        // Use HoT's registered image without resolving another mod's person ID.
        // This page-local identity is never added to officers or contacts.
        PersonAPI person = Global.getFactory().createPerson();
        if (person == null) return null;
        person.setId("chief_navigator_budai_salvage_isa");
        person.setName(new FullName("Isa", "Leicester", FullName.Gender.FEMALE));
        person.setGender(FullName.Gender.FEMALE);
        person.setFaction(Factions.PLAYER);
        person.setPortraitSprite(Global.getSettings().getSpriteName(
                "characters", "ship_trophy_isa"));
        return person;
    }

    static void request() {
        SectorAPI sector = Global.getSector();
        if (sector == null) return;
        MemoryAPI memory = sector.getMemoryWithoutUpdate();
        if (!memory.getBoolean(GRANTED)) memory.set(PENDING, true);
    }

    /** Opens only after the post-battle and salvage interfaces have closed. */
    static void tryShowPending() {
        SectorAPI sector = Global.getSector();
        if (sector == null || sector.getPlayerFleet() == null) return;
        MemoryAPI memory = sector.getMemoryWithoutUpdate();
        if (memory.getBoolean(GRANTED)) {
            memory.unset(PENDING);
            return;
        }
        if (!memory.getBoolean(PENDING)) return;

        CampaignUIAPI ui = sector.getCampaignUI();
        if (ui == null || ui.isShowingDialog() || ui.isShowingMenu()
                || ui.getCurrentCoreTab() != null) {
            return;
        }
        ui.showInteractionDialog(
                new BudaiSalvageDialogPlugin(), sector.getPlayerFleet());
    }

    /** The selected version determines the once-only extraction reward. */
    static boolean grantReward(boolean isaAssisted) {
        SectorAPI sector = Global.getSector();
        if (sector == null) return false;
        MemoryAPI memory = sector.getMemoryWithoutUpdate();
        if (memory.getBoolean(GRANTED)) {
            memory.unset(PENDING);
            return true;
        }
        if (!memory.getBoolean(PENDING) || sector.getPlayerFleet() == null) return false;
        CargoAPI cargo = sector.getPlayerFleet().getCargo();
        if (cargo == null) return false;
        // Claim first so even a reentrant extraction callback cannot pay twice.
        memory.set(GRANTED, true);
        memory.unset(PENDING);
        try {
            cargo.addWeapons(WEAPON_ID,
                    isaAssisted ? WEAPON_COUNT : UNASSISTED_WEAPON_COUNT);
        } catch (RuntimeException failure) {
            memory.unset(GRANTED);
            memory.set(PENDING, true);
            return false;
        }
        return true;
    }

    @Override
    protected void initConversation(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        dialog.setPromptText("");
        dialog.setBackgroundDimAmount(0.85f);
        showConsumedOnslaught();
        memoryMap = RuleDialogSupport.createMemoryMap(dialog);
        memoryMap.get(MemKeys.LOCAL).set("$chiefNavigatorBudaiPlayerTitle", "Captain");
        isaAssisted = IthacaResearchUpgrades.captureIsaRefractionUnlock();
        if (isaAssisted) {
            try {
                isa = createIsaPortrait();
            } catch (RuntimeException failure) {
                Global.getLogger(BudaiSalvageDialogPlugin.class).warn(
                        "Could not prepare Isa's salvage portrait", failure);
            }
        }
        String opening = isaAssisted ? "opening_isa" : "opening_salvage_chief";
        if (!RuleDialogSupport.fire(
                dialog, memoryMap, RULE_PREFIX + opening)) {
            Global.getLogger(BudaiSalvageDialogPlugin.class).warn(
                    "Missing Budai salvage opening rule: " + opening);
            dialog.dismiss();
        }
    }

    private void showConsumedOnslaught() {
        try {
            FleetMemberAPI wreck = Global.getFactory().createFleetMember(
                    FleetMemberType.SHIP, WRECK_VARIANT_ID);
            if (wreck == null) return;
            wreck.setShipName("Consumed Onslaught Mk.I");
            dialog.getVisualPanel().showFleetMemberInfo(wreck, true);
        } catch (RuntimeException failure) {
            Global.getLogger(BudaiSalvageDialogPlugin.class).warn(
                    "Could not display the consumed Onslaught Mk.I",
                    failure);
        }
    }

    @Override
    protected void handleOptionSelected(String optionText, Object optionData) {
        if (optionData == null || dialog == null) return;
        String optionId = optionData.toString();
        if ("extract_isa".equals(optionId) || "extract_salvage_chief".equals(optionId)) {
            String expected = isaAssisted ? "extract_isa" : "extract_salvage_chief";
            if (!expected.equals(optionId)) return;
            if (!grantReward(isaAssisted)) {
                Global.getLogger(BudaiSalvageDialogPlugin.class).warn(
                        "Could not grant Budai's Heavy Adjudicators");
                dialog.dismiss();
                return;
            }
        }
        if (isa != null && ("assess_isa".equals(optionId)
                || "extract_isa".equals(optionId))) {
            ConversationPortraits.show(dialog, isa);
        }
        if (!RuleDialogSupport.fire(
                dialog, memoryMap, RULE_PREFIX + optionId)) {
            Global.getLogger(BudaiSalvageDialogPlugin.class).warn(
                    "Missing Budai salvage stage: " + optionId);
            dialog.dismiss();
        }
    }

    @Override public void optionMousedOver(String text, Object data) { }
    @Override public void advance(float amount) { }
    @Override public void backFromEngagement(EngagementResultAPI result) { }
    @Override public Object getContext() { return null; }
    @Override public Map<String, MemoryAPI> getMemoryMap() { return memoryMap; }
}
