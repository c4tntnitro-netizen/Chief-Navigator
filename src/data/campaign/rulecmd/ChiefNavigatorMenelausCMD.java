package data.campaign.rulecmd;

import chiefnavigator.quest.MenelausTrial;
import chiefnavigator.quest.MenelausTrialIntel;
import chiefnavigator.quest.FobIthacaContacts;
import chiefnavigator.quest.FobIthacaStationInteraction;
import chiefnavigator.quest.ConversationPortraits;
import chiefnavigator.quest.MenelausDialogueReadState;
import chiefnavigator.quest.SinniContact;
import chiefnavigator.quest.OdysseyStrandedFleetsScript;
import chiefnavigator.quest.OdysseyExpanseSystem;
import chiefnavigator.weapons.IthacaResearchUpgrades;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll;
import com.fs.starfarer.api.impl.campaign.rulecmd.EndConversation;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collections;

/** Rules-side state queries and portrait plumbing for Menelaus. */
public final class ChiefNavigatorMenelausCMD extends BaseCommandPlugin {
    private static final String INTRO_SHOWN_V1 =
            "$chief_navigator_menelaus_intro_shown_v1";
    private static final String INTRO_SHOWN_V2 =
            "$chief_navigator_menelaus_intro_shown_v2";
    private static final String GAUTAMA_VARIANT_ID =
            "chief_navigator_scylla_Fabricator";

    @Override
    public boolean execute(
            String ruleId,
            final InteractionDialogAPI dialog,
            List<Token> params,
            final Map<String, MemoryAPI> memoryMap) {
        if (params.isEmpty()) return false;
        String action = params.get(0).getString(memoryMap);
        if ("vossReputation".equals(action) && params.size() > 1) {
            if (Global.getSector() == null
                    || Global.getSector().getPlayerFaction() == null) return false;
            float relationship = Global.getSector().getPlayerFaction()
                    .getRelationship(Factions.HEGEMONY);
            String branch = params.get(1).getString(memoryMap);
            if ("friendly".equals(branch)) return relationship > 0.5f;
            if ("hostile".equals(branch)) return relationship < -0.5f;
            if ("neutral".equals(branch)) {
                return relationship >= -0.5f && relationship <= 0.5f;
            }
            return false;
        }
        if ("introduced".equals(action)) {
            // Old saves may have lost a zero-duration introduction marker
            // after accepting a Labor. They have already met Menelaus and
            // must retain access to subsequent briefings and reports.
            return Global.getSector() != null
                    && (Global.getSector().getMemoryWithoutUpdate()
                            .getBoolean(INTRO_SHOWN_V2)
                        || MenelausTrial.isAccepted());
        }
        if ("endContact".equals(action)) {
            if (dialog == null) return false;
            if (FobIthacaStationInteraction.endContact(dialog)) return true;
            return new EndConversation().execute(
                    ruleId, dialog, Collections.<Token>emptyList(), memoryMap);
        }
        if ("finishIntro".equals(action)) {
            if (dialog == null) return false;
            if (!markIntroductionShown()) return false;
            FobIthacaStationInteraction.returnToStation(dialog);
            return true;
        }
        if ("stage".equals(action) && params.size() > 1) {
            String stage = params.get(1).getString(memoryMap);
            if ("briefing".equals(stage)) return !MenelausTrial.isAccepted();
            if ("early_wall".equals(stage)) {
                return MenelausTrial.shouldAcknowledgeEarlyWall();
            }
            if (stage.endsWith("_report")) {
                return MenelausTrial.isLaborReportPending(
                        laborStage(stage.substring(
                                0, stage.length() - "_report".length())));
            }
            if ("wall".equals(stage)) return MenelausTrial.getCurrentLaborStage()
                    == MenelausTrial.LABOR_WALL
                    && !MenelausTrial.isLaborReportPending(
                            MenelausTrial.LABOR_WALL);
            if ("research".equals(stage)) return MenelausTrial.getCurrentLaborStage()
                    == MenelausTrial.LABOR_RESEARCH
                    && !MenelausTrial.isLaborReportPending(
                            MenelausTrial.LABOR_RESEARCH);
            if ("motherships".equals(stage)) return MenelausTrial.getCurrentLaborStage()
                    == MenelausTrial.LABOR_MOTHERSHIPS
                    && !MenelausTrial.isLaborReportPending(
                            MenelausTrial.LABOR_MOTHERSHIPS);
            if ("sensor".equals(stage)) return MenelausTrial.getCurrentLaborStage()
                    == MenelausTrial.LABOR_SENSOR
                    && !MenelausTrial.isLaborReportPending(
                            MenelausTrial.LABOR_SENSOR);
            if ("final_offer".equals(stage)) {
                return MenelausTrial.getCurrentLaborStage()
                        == MenelausTrial.LABOR_GAUTAMA
                        && !MenelausTrial.isFinalLaborBriefed()
                        && !MenelausTrial.isFinalDebriefPending()
                        && !MenelausTrial.isFinalLaborFailed();
            }
            if ("gautama".equals(stage)) return MenelausTrial.getCurrentLaborStage()
                    == MenelausTrial.LABOR_GAUTAMA
                    && MenelausTrial.isFinalLaborBriefed()
                    && !MenelausTrial.isFinalDebriefPending();
            if ("debrief".equals(stage)) {
                return MenelausTrial.isFinalDebriefPending();
            }
            if ("complete".equals(stage)) {
                return MenelausTrial.isComplete();
            }
            return false;
        }
        if ("reveal".equals(action)) {
            MenelausTrial.acknowledgeFinalLabor();
            action = "init";
        }
        if ("initEarlyWall".equals(action)) {
            MenelausTrial.acknowledgeEarlyWall();
            action = "init";
        }
        if ("initUpgrades".equals(action)) {
            action = "init";
        }
        if ("showVoss".equals(action)) {
            if (dialog != null) {
                FobIthacaContacts.ensureForStation(
                        dialog.getInteractionTarget());
                PersonAPI menelaus = MenelausTrial.getOrCreateMenelaus();
                PersonAPI voss = OdysseyStrandedFleetsScript
                        .getOrCreateVoss();
                ConversationPortraits.show(dialog, menelaus, voss,
                        FobIthacaContacts.getOrCreateSpartanCaptain(),
                        SinniContact.getOrCreatePerson());
            }
            OdysseyStrandedFleetsScript.markIthacaCommandSceneShown();
            return true;
        }
        if ("hideVoss".equals(action)) {
            if (dialog != null) {
                ConversationPortraits.show(dialog,
                        MenelausTrial.getOrCreateMenelaus(),
                        SinniContact.getOrCreatePerson());
            }
            return true;
        }
        if ("highlightUnread".equals(action)) {
            MenelausDialogueReadState.highlightOptions(dialog, memoryMap.get(MemKeys.GLOBAL));
            return true;
        }
        if ("highlightUpgrades".equals(action)) {
            MemoryAPI local = memoryMap.get(MemKeys.LOCAL);
            if (dialog == null || local == null) return false;
            String ledger = local.getString("$chiefNavigatorUpgradeLedger");
            if (ledger == null) return false;
            List<String> highlights = new ArrayList<>();
            List<Color> colors = new ArrayList<>();
            // Native highlights match in paragraph order, including repeats.
            // Exclude headings, recovery leads, and the pending-install state.
            for (String line : ledger.split("\\R")) {
                if (line.endsWith(": Installed")) {
                    highlights.add("Installed");
                    colors.add(Misc.getPositiveHighlightColor());
                } else if (line.endsWith(": Not recovered")) {
                    highlights.add("Not recovered");
                    colors.add(Misc.getNegativeHighlightColor());
                }
            }
            if (!highlights.isEmpty()) {
                dialog.getTextPanel().highlightInLastPara(
                        highlights.toArray(new String[0]));
                dialog.getTextPanel().setHighlightColorsInLastPara(
                        colors.toArray(new Color[0]));
            }
            return true;
        }
        if ("showSpartan".equals(action)) {
            PersonAPI captain = FobIthacaContacts.getOrCreateSpartanCaptain();
            if (dialog != null) {
                ConversationPortraits.show(dialog, captain,
                        SinniContact.getOrCreatePerson());
            }
            MemoryAPI local = memoryMap.get(MemKeys.LOCAL);
            if (local != null && Global.getSector() != null
                    && Global.getSector().getPlayerPerson() != null) {
                local.set("$chiefNavigatorLaborFivePlayerTitle",
                        "Captain", 0f);
                local.set("$chiefNavigatorLaborFivePlayerName",
                        Global.getSector().getPlayerPerson().getNameString(),
                        0f);
            }
            return true;
        }
        if ("showGautama".equals(action)) {
            if (dialog != null) {
                dialog.getVisualPanel().hideSecondPerson();
                dialog.getVisualPanel().hideThirdPerson();
                try {
                    FleetMemberAPI gautama = Global.getFactory()
                            .createFleetMember(
                                    FleetMemberType.SHIP,
                                    GAUTAMA_VARIANT_ID);
                    if (gautama != null) {
                        gautama.setShipName("Gautama");
                        dialog.getVisualPanel()
                                .showFleetMemberInfo(gautama, true);
                    }
                } catch (RuntimeException failure) {
                    Global.getLogger(ChiefNavigatorMenelausCMD.class).warn(
                            "Could not display Gautama in Menelaus briefing",
                            failure);
                }
            }
            return true;
        }
        if ("init".equals(action)) {
            PersonAPI menelaus = MenelausTrial.getOrCreateMenelaus();
            if (dialog != null) {
                FobIthacaContacts.ensureForStation(
                        dialog.getInteractionTarget());
                ConversationPortraits.show(dialog, menelaus,
                        SinniContact.getOrCreatePerson());
            }
            MemoryAPI local = memoryMap.get(MemKeys.LOCAL);
            if (local != null) {
                local.set("$chiefNavigatorMothershipsRemaining",
                        MenelausTrial.getMothershipsRemaining(), 0f);
                local.set("$chiefNavigatorMothershipsDefeated",
                        MenelausTrial.getMothershipsDefeated(), 0f);
                local.set("$chiefNavigatorFriendlyGuards",
                        MenelausTrial.getFriendlyFleetCount(), 0f);
                local.set("$chiefNavigatorResearchResearched",
                        MenelausTrial.getResearchStationsResearched(), 0f);
                local.set("$chiefNavigatorResearchStationTotal",
                        MenelausTrial.getResearchStationTotal(), 0f);
                local.set("$chiefNavigatorResearchInstalled",
                        MenelausTrial.getResearchObjectiveProgress(), 0f);
                local.set("$chiefNavigatorResearchTarget",
                        MenelausTrial.getResearchObjectiveTarget(), 0f);
                local.set("$chiefNavigatorWallStatus",
                        MenelausTrial.isWallFriendly()
                                ? "reclaimed" : "still hostile", 0f);
                local.set("$chiefNavigatorGautamaStatus",
                        MenelausTrial.isGautamaDefeated()
                                ? "defeated"
                                : "committed to the final Ithaca invasion", 0f);
                setUpgradeVariables(local);
            }
            return true;
        }
        if ("upgrades".equals(action) && params.size() > 1) {
            String upgradeAction = params.get(1).getString(memoryMap);
            if ("available".equals(upgradeAction)) {
                return IthacaResearchUpgrades.hasAvailableComponents();
            }
            if ("install".equals(upgradeAction)) {
                int installed = IthacaResearchUpgrades
                        .installRecoveredComponents();
                if (installed > 0) {
                    com.fs.starfarer.api.campaign.StarSystemAPI expanse =
                            OdysseyExpanseSystem.findExisting();
                    if (expanse != null) {
                        // Re-evaluate Labor II immediately so its markers,
                        // Intel progress, and report option change on the same
                        // interaction that installs the fourth component.
                        MenelausTrial.refreshProgress(expanse);
                    } else {
                        MenelausTrialIntel.syncProgress(true);
                    }
                }
                MemoryAPI local = memoryMap.get(MemKeys.LOCAL);
                if (local != null) {
                    local.set("$chiefNavigatorResearchResearched",
                            MenelausTrial.getResearchStationsResearched(), 0f);
                    local.set("$chiefNavigatorResearchStationTotal",
                            MenelausTrial.getResearchStationTotal(), 0f);
                    local.set("$chiefNavigatorResearchInstalled",
                            MenelausTrial.getResearchObjectiveProgress(), 0f);
                    local.set("$chiefNavigatorResearchTarget",
                            MenelausTrial.getResearchObjectiveTarget(), 0f);
                    setUpgradeVariables(local);
                }
                return true;
            }
            return false;
        }
        if ("accept".equals(action)) {
            MenelausTrial.accept(dialog == null
                    ? null : dialog.getTextPanel());
            if (MenelausTrial.isAccepted()) markIntroductionShown();
            return true;
        }
        if ("acceptFinalLabor".equals(action)) {
            return MenelausTrial.acceptFinalLabor(dialog == null
                    ? null : dialog.getTextPanel());
        }
        if ("report".equals(action) && params.size() > 1) {
            int stage = laborStage(params.get(1).getString(memoryMap));
            return MenelausTrial.reportLabor(stage);
        }
        if ("finish".equals(action)) {
            boolean completed = MenelausTrial.completeFinalDebrief();
            if (completed) {
                MenelausTrialIntel.syncProgress(true);
                MenelausTrial.ensurePostTrialConsequences();
            }
            if (dialog != null) dialog.dismiss();
            return completed;
        }
        if ("repair".equals(action)) {
            if (Global.getSector() == null
                    || Global.getSector().getPlayerFleet() == null) {
                return false;
            }
            for (FleetMemberAPI member : Global.getSector().getPlayerFleet()
                    .getFleetData().getMembersListCopy()) {
                member.getStatus().repairFully();
                RepairTrackerAPI tracker = member.getRepairTracker();
                tracker.setCR(Math.max(tracker.getCR(), tracker.getMaxCR()));
                member.setStatUpdateNeeded(true);
            }
            return true;
        }
        return false;
    }

    private static int laborStage(String name) {
        if ("motherships".equals(name)) {
            return MenelausTrial.LABOR_MOTHERSHIPS;
        }
        if ("research".equals(name)) {
            return MenelausTrial.LABOR_RESEARCH;
        }
        if ("wall".equals(name)) return MenelausTrial.LABOR_WALL;
        if ("sensor".equals(name)) return MenelausTrial.LABOR_SENSOR;
        return MenelausTrial.LABOR_NONE;
    }

    private static boolean markIntroductionShown() {
        if (Global.getSector() == null) return false;
        MemoryAPI global = Global.getSector().getMemoryWithoutUpdate();
        global.set(INTRO_SHOWN_V1, true);
        global.set(INTRO_SHOWN_V2, true);
        return true;
    }

    private static void setUpgradeVariables(MemoryAPI local) {
        local.set("$chiefNavigatorUpgradesInstalled",
                IthacaResearchUpgrades.getInstalledCount(), 0f);
        local.set("$chiefNavigatorUpgradesTotal",
                IthacaResearchUpgrades.COMPONENT_IDS.length, 0f);
        local.set("$chiefNavigatorUpgradesAvailable",
                IthacaResearchUpgrades.getAvailableCount(), 0f);
        local.set("$chiefNavigatorUpgradeLedger",
                IthacaResearchUpgrades.getUpgradeLedger(), 0f);
    }
}
