package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.combat.threat.ThreatFIDConfig;
import com.fs.starfarer.api.util.Misc;

/** Extends only friendly Ithaca station support, not general fleet join range. */
public final class IthacaSupportInteraction extends FleetInteractionDialogPluginImpl {
    public static final float SUPPORT_RANGE = 2500f;

    public IthacaSupportInteraction(boolean ambush) {
        this(ambush, false);
    }

    public IthacaSupportInteraction(boolean ambush, boolean starvingThreat) {
        super(supportConfig(ambush, starvingThreat));
    }

    private static FIDConfig supportConfig(
            boolean ambush, boolean starvingThreat) {
        FIDConfig config = starvingThreat
                ? new ThreatFIDConfig().createConfig()
                : new FIDConfig();
        final FIDDelegate inheritedDelegate = config.delegate;
        if (starvingThreat) {
            // Keep the Threat battle/salvage delegate without inheriting its
            // assumption that every encounter starts mutually hostile.
            config.alwaysAttackVsAttack = false;
            config.alwaysHarry = false;
            config.leaveAlwaysAvailable = true;
            config.showCommLinkOption = false;
            config.showTransponderStatus = false;
            config.showFleetAttitude = false;
            config.showEngageText = false;
            config.showWarningDialogWhenNotHostile = false;
            config.impactsAllyReputation = false;
            config.impactsEnemyReputation = false;
            // Keep a deliberate engage/join choice even though Starving
            // Threat is hostile to the player again.
            config.straightToEngage = false;
        }
        if (ambush) {
            config.alwaysPursue = true;
            config.firstTimeEngageOptionText = "Spring the ambush";
        }
        config.delegate = new BaseFIDDelegate() {
            @Override
            public void battleContextCreated(
                    com.fs.starfarer.api.campaign.InteractionDialogAPI dialog,
                    com.fs.starfarer.api.combat.BattleCreationContext context) {
                if (inheritedDelegate != null) {
                    inheritedDelegate.battleContextCreated(dialog, context);
                }
            }

            @Override
            public void postPlayerSalvageGeneration(
                    com.fs.starfarer.api.campaign.InteractionDialogAPI dialog,
                    com.fs.starfarer.api.impl.campaign.FleetEncounterContext context,
                    com.fs.starfarer.api.campaign.CargoAPI salvage) {
                if (inheritedDelegate != null) {
                    inheritedDelegate.postPlayerSalvageGeneration(
                            dialog, context, salvage);
                }
            }

            @Override
            public void notifyLeave(com.fs.starfarer.api.campaign.InteractionDialogAPI dialog) {
                if (inheritedDelegate != null) {
                    inheritedDelegate.notifyLeave(dialog);
                }
                SectorEntityToken target = dialog.getInteractionTarget();
                if (target != null) IthacaSectionEncounter.restoreCampaignPresentation(
                        target.getContainingLocation());
            }
        };
        return config;
    }

    @Override
    protected void pullInNearbyFleets() {
        super.pullInNearbyFleets();
        BattleAPI battle = context.getBattle();
        if (!ongoingBattle) pullInWallDetachment();
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (!(dialog.getInteractionTarget() instanceof CampaignFleetAPI)) return;
        CampaignFleetAPI enemy = (CampaignFleetAPI) dialog.getInteractionTarget();
        if (ongoingBattle || battle.isStationInvolvedOnPlayerSide()) return;
        CampaignFleetAPI support = findSupport(enemy, player);
        if (support == null || support.getBattle() != null) return;
        IthacaSectionEncounter.revealCombatSprite(support);
        if (!battle.join(support, battle.pickSide(player))) return;
        pulledIn.add(support);
        support.inflateIfNeeded();
        textPanel.addParagraph(support.getName() + ": supporting your forces.");
        battle.genCombined();
        refreshSupportedFleets(battle);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        super.optionSelected(optionText, optionData);
        // Opening an ongoing battle's preview is not consent to fight. Let
        // native JOIN admit the player before sending the Wall detachment.
        if (optionData == OptionId.JOIN_ONGOING_BATTLE && joinedBattle) {
            pullInWallDetachment();
        }
    }

    private void pullInWallDetachment() {
        if (!(dialog.getInteractionTarget() instanceof CampaignFleetAPI)) return;
        BattleAPI battle = context.getBattle();
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        CampaignFleetAPI enemy = (CampaignFleetAPI) dialog.getInteractionTarget();
        CampaignFleetAPI detachment =
                DriftingWallLaborSupport.findSupport(enemy, player);
        if (detachment != null
                && battle.join(detachment, battle.pickSide(player))) {
            pulledIn.add(detachment);
            detachment.inflateIfNeeded();
            battle.genCombined();
            refreshSupportedFleets(battle);
        }
    }

    private void refreshSupportedFleets(BattleAPI battle) {
        battle.takeSnapshots();
        playerFleet = battle.getPlayerCombined();
        otherFleet = battle.getNonPlayerCombined();
        if (!config.straightToEngage) showFleetInfo();
    }

    static CampaignFleetAPI findSupport(CampaignFleetAPI enemy, CampaignFleetAPI player) {
        if (enemy == null || player == null || enemy.getContainingLocation() == null
                || enemy.getContainingLocation() != player.getContainingLocation()) return null;
        CampaignFleetAPI nearest = null;
        float closest = SUPPORT_RANGE;
        for (CampaignFleetAPI fleet : enemy.getContainingLocation().getFleets()) {
            if (!IthacaSectionEncounter.isSectionStation(fleet) || fleet.isEmpty()
                    || fleet.getBattle() != null || fleet.isHostileTo(player)
                    || !fleet.isHostileTo(enemy)) continue;
            float distance = Misc.getDistance(fleet, enemy) - fleet.getRadius() - enemy.getRadius();
            if (distance <= closest) {
                nearest = fleet;
                closest = distance;
            }
        }
        return nearest;
    }
}
