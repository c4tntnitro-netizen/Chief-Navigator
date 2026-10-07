package chiefnavigator.quest;

import chiefnavigator.abilities.SinniAmbushStanceAbility;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.BattleCreationPlugin;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.combat.threat.ThreatFIDConfig;

/** Routes the Troy checkpoint, wormholes, and custom showcase battles. */
public final class TreadmillCampaignPlugin extends BaseCampaignPlugin {
    public static final String ID = "chief_navigator_treadmill_campaign_plugin";

    @Override public String getId() { return ID; }
    @Override public boolean isTransient() { return true; }

    @Override
    public PluginPick<InteractionDialogPlugin> pickInteractionDialogPlugin(SectorEntityToken target) {
        // Retry a narrowly scoped duplicate-exit correction if a transition
        // or open jump dialog deferred it during load. Vanilla owns the UI.
        if (target instanceof com.fs.starfarer.api.campaign.JumpPointAPI
                && target.getContainingLocation()
                        instanceof StarSystemAPI
                && OdysseyExpanseSystem.SYSTEM_ID.equals(
                        ((StarSystemAPI) target.getContainingLocation())
                                .getOptionalUniqueId())) {
            AlphaOdysseyJumpRepair.repairDuplicates();
        }
        // This must precede the blanket Ashen Verge fleet route below. It is
        // the race-proof fallback for a click on Ithaca or its Spartan cordon
        // before the spatial first-contact trigger gets a free campaign frame.
        if (FobIthacaApproachScript.shouldInterceptInteraction(target)) {
            return new PluginPick<InteractionDialogPlugin>(
                    new FobIthacaApproachInteraction(),
                    PickPriority.HIGHEST);
        }
        if (FobIthacaApproachScript.shouldOpenStationIntroduction(target)) {
            return new PluginPick<InteractionDialogPlugin>(
                    new RuleBasedInteractionDialogPluginImpl(
                            "ChiefNavigatorFobIthaca"),
                    PickPriority.HIGHEST);
        }
        if (OdysseyExpanseSystem.isFobIthaca(target)) {
            return new PluginPick<InteractionDialogPlugin>(
                    new FobIthacaStationInteraction(),
                    PickPriority.HIGHEST);
        }
        CampaignFleetAPI ithacaSection =
                IthacaSectionEncounter.resolveStationForInteraction(target);
        if (ithacaSection != null) {
            return new PluginPick<InteractionDialogPlugin>(
                    new IthacaSectionInteraction(ithacaSection),
                    PickPriority.HIGHEST);
        }
        if (target instanceof CampaignFleetAPI) {
            final CampaignFleetAPI enemy = (CampaignFleetAPI) target;
            if (MenelausTrial.isFriendlyDronePatrol(target)) {
                return new PluginPick<InteractionDialogPlugin>(
                        new RuleBasedInteractionDialogPluginImpl(
                                "ChiefNavigatorDronePatrol"),
                        PickPriority.HIGHEST);
            }
            if (OdysseyStrandedFleetsScript.isHegemonyExpedition(enemy)) {
                return new PluginPick<InteractionDialogPlugin>(
                        new HegemonyExpeditionInteraction(),
                        PickPriority.HIGHEST);
            }
            if (OdysseyStrandedFleetsScript.isRescueEscortCandidate(enemy)) {
                return new PluginPick<InteractionDialogPlugin>(
                        new OdysseyRescueEscortInteraction(),
                        PickPriority.HIGHEST);
            }
            if (OdysseyPredatorScript.isBudaiBattle(enemy)) {
                OdysseyPredatorScript.configureBudaiInteractionIdentity(enemy);
                CampaignFleetAPI player = com.fs.starfarer.api.Global.getSector()
                        .getPlayerFleet();
                boolean ambush = player != null
                        && (player.isHostileTo(enemy) || enemy.isHostileTo(player))
                        && SinniAmbushStanceAbility.isAmbushReady(player);
                return new PluginPick<InteractionDialogPlugin>(
                        new BudaiInteraction(ambush), PickPriority.HIGHEST);
            }
            if (OdysseyPredatorScript.isUngaikyoRematch(enemy)) {
                FleetInteractionDialogPluginImpl.FIDConfig config =
                        new ThreatFIDConfig().createConfig();
                config.showCommLinkOption = false;
                config.showTransponderStatus = false;
                config.showFleetAttitude = false;
                config.showEngageText = false;
                config.showWarningDialogWhenNotHostile = false;
                config.impactsAllyReputation = false;
                config.impactsEnemyReputation = false;
                // The battle plugin supplies Ungaikyo's reflections;
                // campaign fleets and stations must not join either side.
                config.pullInAllies = false;
                config.pullInEnemies = false;
                config.pullInStations = false;
                config.showPullInText = false;
                return new PluginPick<InteractionDialogPlugin>(
                        new FleetInteractionDialogPluginImpl(config),
                        PickPriority.HIGHEST);
            }
            if (OdysseyPredatorScript.isAuthoredStarvingThreatFleet(enemy)) {
                OdysseyPredatorScript.configureStarvingThreatIdentity(enemy);
                CampaignFleetAPI player = com.fs.starfarer.api.Global.getSector()
                        .getPlayerFleet();
                if (enemy.getContainingLocation() instanceof StarSystemAPI
                        && enemy.getContainingLocation()
                                == OdysseyExpanseSystem.findSystemById(
                                        OdysseyExpanseSystem.ASHEN_VERGE_ID)) {
                    boolean ambush = player != null
                            && (player.isHostileTo(enemy)
                                || enemy.isHostileTo(player))
                            && SinniAmbushStanceAbility.isAmbushReady(player);
                    return new PluginPick<InteractionDialogPlugin>(
                            new IthacaSupportInteraction(ambush, true),
                            PickPriority.HIGHEST);
                }
                FleetInteractionDialogPluginImpl.FIDConfig config =
                        new ThreatFIDConfig().createConfig();
                config.showCommLinkOption = false;
                config.showTransponderStatus = false;
                config.showFleetAttitude = false;
                config.showEngageText = false;
                config.showWarningDialogWhenNotHostile = false;
                config.impactsAllyReputation = false;
                config.impactsEnemyReputation = false;
                return new PluginPick<InteractionDialogPlugin>(
                        new FleetInteractionDialogPluginImpl(config),
                        PickPriority.HIGHEST);
            }
            CampaignFleetAPI player = com.fs.starfarer.api.Global.getSector()
                    .getPlayerFleet();
            if (enemy.getContainingLocation() instanceof StarSystemAPI
                    && enemy.getContainingLocation()
                            == OdysseyExpanseSystem.findSystemById(
                                    OdysseyExpanseSystem.ASHEN_VERGE_ID)) {
                return new PluginPick<InteractionDialogPlugin>(
                        new IthacaSupportInteraction(player != null
                                && (player.isHostileTo(enemy) || enemy.isHostileTo(player))
                                && SinniAmbushStanceAbility.isAmbushReady(player)), PickPriority.HIGHEST);
            }
            if (player != null && (player.isHostileTo(enemy) || enemy.isHostileTo(player))
                    && SinniAmbushStanceAbility.isAmbushReady(player)) {
                FleetInteractionDialogPluginImpl.FIDConfig config =
                        new FleetInteractionDialogPluginImpl.FIDConfig();
                config.alwaysPursue = true;
                config.firstTimeEngageOptionText = "Spring the ambush";
                return new PluginPick<InteractionDialogPlugin>(
                        new FleetInteractionDialogPluginImpl(config),
                        PickPriority.HIGHEST);
            }
        }
        if (TroyArrivalScript.isDepartureStation(target)) {
            return new PluginPick<InteractionDialogPlugin>(
                    new TroyDepartureInteraction(), PickPriority.HIGHEST);
        }
        if (TroyArrivalScript.isOdysseyWormhole(target)) {
            return new PluginPick<InteractionDialogPlugin>(
                    new TroyWormholeInteraction(), PickPriority.HIGHEST);
        }
        if (OdysseyExpanseSystem.isEntry(target)) {
            return new PluginPick<InteractionDialogPlugin>(
                    new OdysseyExpanseInteraction(), PickPriority.HIGHEST);
        }
        if (OdysseyExpanseSystem.isLastLightGuardianWreck(target)
                && !OdysseyPredatorScript.isUngaikyoRematchDefeated()) {
            return new PluginPick<InteractionDialogPlugin>(
                    new RuleBasedInteractionDialogPluginImpl(
                            "ChiefNavigatorLastLightGuardianLocked"),
                    PickPriority.HIGHEST);
        }
        if (OdysseyExpanseSystem.isDevouredRing(target)) {
            return new PluginPick<InteractionDialogPlugin>(
                    new RuleBasedInteractionDialogPluginImpl(
                            "ChiefNavigatorDevouredRing"),
                    PickPriority.HIGHEST);
        }
        if (OdysseyExpanseSystem.isDomainDerelictBattlestation(target)) {
            return new PluginPick<InteractionDialogPlugin>(
                    new RuleBasedInteractionDialogPluginImpl(
                            "ChiefNavigatorDerelictBattlestation"),
                    PickPriority.HIGHEST);
        }
        if (AviciHabitatEncounter.isHabitat(target)) {
            return new PluginPick<InteractionDialogPlugin>(
                    new RuleBasedInteractionDialogPluginImpl(
                            "ChiefNavigatorAviciHabitat"),
                    PickPriority.HIGHEST);
        }
        if (MenelausTrial.isMothershipPOI(target)) {
            return new PluginPick<InteractionDialogPlugin>(
                    new RuleBasedInteractionDialogPluginImpl(
                            "ChiefNavigatorSealedMothership"),
                    PickPriority.HIGHEST);
        }
        if (MenelausSensorLabor.canDeployAt(target)) {
            return new PluginPick<InteractionDialogPlugin>(
                    new RuleBasedInteractionDialogPluginImpl(
                            "ChiefNavigatorSensorArray"),
                    PickPriority.HIGHEST);
        }
        if (OdysseyExpanseSystem.isFobIthacaGate(target)
                || (!MenelausTrial.isComplete()
                        && target instanceof CampaignFleetAPI
                        && OdysseyExpanseSystem
                                .isOwnedFobIthacaSpartanGuard(
                                        (CampaignFleetAPI) target))) {
            return new PluginPick<InteractionDialogPlugin>(
                    new RuleBasedInteractionDialogPluginImpl(
                            "ChiefNavigatorIthacaGate"),
                    PickPriority.HIGHEST);
        }
        if (DriftingWallEncounter.isDriftingWall(target)) {
            return new PluginPick<InteractionDialogPlugin>(
                    new RuleBasedInteractionDialogPluginImpl(
                            "ChiefNavigatorDriftingWall"),
                    PickPriority.HIGHEST);
        }
        return null;
    }

    @Override
    public PluginPick<BattleCreationPlugin> pickBattleCreationPlugin(SectorEntityToken opponent) {
        if (OdysseyPredatorScript.isUngaikyoRematchBattle(opponent)) {
            return new PluginPick<BattleCreationPlugin>(
                    new GautamaFinalRematchBattleCreationPlugin(),
                    PickPriority.HIGHEST);
        }
        if (OdysseyPredatorScript.isFinalLaborCenterBattle(opponent)) {
            return new PluginPick<BattleCreationPlugin>(
                    new IthacaFinalLaborBattleCreationPlugin(),
                    PickPriority.HIGHEST);
        }
        if (OdysseyPredatorScript.isFinalLaborPerimeterBattle(opponent)) {
            return new PluginPick<BattleCreationPlugin>(
                    new FinalLaborPerimeterBattleCreationPlugin(),
                    PickPriority.HIGHEST);
        }
        if (OdysseyPredatorScript.isBudaiBattle(opponent)) {
            return new PluginPick<BattleCreationPlugin>(
                    new BudaiBattleCreationPlugin(), PickPriority.HIGHEST);
        }
        if (isSanzuBattle(opponent)) {
            com.fs.starfarer.api.Global.getLogger(
                    TreadmillCampaignPlugin.class).info(
                            "[SANZU_NEBULA] Routing battle through the "
                                    + "Sanzu nebula battle plugin.");
            return new PluginPick<BattleCreationPlugin>(
                    new SanzuNebulaBattleCreationPlugin(),
                    PickPriority.HIGHEST);
        }
        if (opponent != null && opponent.getMemoryWithoutUpdate().getBoolean(
                TroyArrivalScript.TASK_FORCE_SPARTAN_MARKER)) {
            return new PluginPick<BattleCreationPlugin>(
                    new TaskForceSpartanBattleCreationPlugin(), PickPriority.HIGHEST);
        }
        if (opponent != null && opponent.getMemoryWithoutUpdate().getBoolean(DriftingWallEncounter.MARKER)) {
            return new PluginPick<BattleCreationPlugin>(
                    new DriftingWallBattleCreationPlugin(), PickPriority.HIGHEST);
        }
        if (opponent != null && opponent.getMemoryWithoutUpdate().getBoolean(
                IthacaSectionEncounter.MARKER)) {
            return new PluginPick<BattleCreationPlugin>(
                    new IthacaSectionBattleCreationPlugin(),
                    PickPriority.HIGHEST);
        }
        if (opponent != null && opponent.getMemoryWithoutUpdate().getBoolean(TroyArrivalScript.DEFENSE_MARKER)) {
            return new PluginPick<BattleCreationPlugin>(new IthacaBattleCreationPlugin(), PickPriority.HIGHEST);
        }
        return null;
    }

    private boolean isSanzuBattle(SectorEntityToken opponent) {
        if (opponent != null
                && isSanzuLocation(opponent.getContainingLocation())) {
            return true;
        }
        CampaignFleetAPI player = com.fs.starfarer.api.Global.getSector()
                .getPlayerFleet();
        return player != null
                && isSanzuLocation(player.getContainingLocation());
    }

    private boolean isSanzuLocation(LocationAPI location) {
        return location instanceof StarSystemAPI
                && location == OdysseyExpanseSystem.findSystemById(
                        OdysseyExpanseSystem.SILENT_WAKE_ID);
    }
}
