package chiefnavigator;

import chiefnavigator.ai.GuardDroneAI;
import chiefnavigator.abilities.SinniGenerateSlipsurgeAbility;
import chiefnavigator.abilities.SinniAmbushStanceAbility;
import chiefnavigator.abilities.SinniEmergencyBurnAbility;
import chiefnavigator.abilities.SinniInterdictionPulseAbility;
import chiefnavigator.campaign.SinniStormRiderScript;
import chiefnavigator.campaign.SinniSensorBurstListener;
import chiefnavigator.campaign.AviciMusicScript;
import chiefnavigator.campaign.AlphaOdysseyMusicScript;
import chiefnavigator.campaign.SanzuMusicScript;
import chiefnavigator.campaign.OdysseyAICoreCampaignPlugin;
import chiefnavigator.campaign.DomainSecurityIFFAuthorization;
import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignPlugin.PickPriority;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.characters.AbilityPlugin;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.abilities.GenerateSlipsurgeAbility;
import com.fs.starfarer.api.impl.campaign.abilities.GoDarkAbility;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import chiefnavigator.quest.SinniBarEvent;
import chiefnavigator.quest.SinniBarEventAvailabilityScript;
import chiefnavigator.quest.AviciHabitatEncounter;
import chiefnavigator.quest.SinniContact;
import chiefnavigator.quest.SinniEventideIntel;
import chiefnavigator.quest.SinniOneMoreHorizonIntel;
import chiefnavigator.quest.SinniSystemVignetteScript;
import chiefnavigator.quest.TreadmillCampaignPlugin;
import chiefnavigator.quest.TreadmillEntryScript;
import chiefnavigator.quest.TroyArrivalScript;
import chiefnavigator.quest.OdysseyExpanseControlScript;
import chiefnavigator.quest.OdysseyExpanseSystem;
import chiefnavigator.quest.ResearchStationSalvageListener;
import chiefnavigator.quest.IthacaSectionEncounter;
import chiefnavigator.quest.FobIthacaRingCollisionScript;
import chiefnavigator.quest.FobIthacaApproachScript;
import chiefnavigator.quest.BudaiMusic;
import chiefnavigator.quest.FinalLaborMusic;
import chiefnavigator.quest.UngaikyoMusic;
import chiefnavigator.quest.MenelausTrialScript;
import chiefnavigator.quest.OdysseyPredatorScript;
import chiefnavigator.quest.OdysseyStrandedFleetsScript;
import chiefnavigator.quest.LeagueSurvivorRescueScript;
import chiefnavigator.topography.SinniHyperspaceTopographyEventIntel;

/**
 * Loadable entry point for the Chief Navigator mod.
 *
 * Quest and world-generation behavior will be added only after the inherited
 * Ithaca concept has been translated into an explicit technical design.
 */
public final class ChiefNavigatorModPlugin extends BaseModPlugin {
    private static final String REMOVED_ODYSSEY_ABILITY = "chief_navigator_hyperspace_odyssey";
    private static final String COMBAT_GUARD_RELATIONS_INITIALIZED =
            "$chief_navigator_combat_guard_relations_initialized";

    @Override
    public PluginPick<ShipAIPlugin> pickShipAI(
            FleetMemberAPI member, ShipAPI ship) {
        if (!GuardDroneAI.isGuardDrone(ship)) return null;
        return new PluginPick<ShipAIPlugin>(
                Global.getSettings().createDefaultShipAI(
                        ship, GuardDroneAI.createFearlessConfig()),
                PickPriority.MOD_SPECIFIC);
    }

    @Override
    public void onGameLoad(boolean newGame) {
        BudaiMusic.resetForGameLoad();
        FinalLaborMusic.resetForGameLoad();
        UngaikyoMusic.resetForGameLoad();
        Global.getSector().unregisterPlugin(OdysseyAICoreCampaignPlugin.ID);
        initializeDomainCombatGuardRelations();
        chiefnavigator.quest.IthacaThreatPolicy.initializeRelations();
        chiefnavigator.quest.TaskForceSpartanFaction.enforceRelations();
        chiefnavigator.quest.HegemonyExpeditionFaction.install();
        DomainSecurityIFFAuthorization.install();
        SinniHyperspaceTopographyEventIntel.ensureInstalled();
        if (Global.getSector().getMemoryWithoutUpdate().getBoolean(SinniBarEvent.STARTED)) {
            SinniContact.joinPlayerFleet();
        } else {
            SinniContact.cleanupLegacyContact();
        }
        refreshOverriddenAbilityPlugins();
        restoreRetiredAbilityOverrides();
        removeRetiredAmbushStanceAbility();
        Global.getSector().removeTransientScriptsOfClass(SinniStormRiderScript.class);
        Global.getSector().addTransientScript(new SinniStormRiderScript());
        Global.getSector().removeTransientScriptsOfClass(
                SinniSystemVignetteScript.class);
        Global.getSector().addTransientScript(
                new SinniSystemVignetteScript());
        Global.getSector().getListenerManager().removeListenerOfClass(
                SinniSensorBurstListener.class);
        Global.getSector().addTransientListener(
                new SinniSensorBurstListener());
        Global.getSector().getListenerManager().removeListenerOfClass(
                ResearchStationSalvageListener.class);
        Global.getSector().getListenerManager().addListener(
                new ResearchStationSalvageListener(), true);
        Global.getSector().removeTransientScriptsOfClass(AviciMusicScript.class);
        Global.getSector().getListenerManager().removeListenerOfClass(
                AviciMusicScript.class);
        AviciMusicScript aviciMusic = new AviciMusicScript();
        Global.getSector().addTransientScript(aviciMusic);
        Global.getSector().addTransientListener(aviciMusic);
        Global.getSector().removeTransientScriptsOfClass(
                AlphaOdysseyMusicScript.class);
        Global.getSector().getListenerManager().removeListenerOfClass(
                AlphaOdysseyMusicScript.class);
        Global.getSector().removeTransientScriptsOfClass(
                SanzuMusicScript.class);
        Global.getSector().getListenerManager().removeListenerOfClass(
                SanzuMusicScript.class);

        // Older builds could serialize a save reload while their imperative
        // music controller still had default playback suspended. Native
        // location music owns the stream now, so explicitly hand it back.
        if (Global.getSoundPlayer() != null) {
            Global.getSoundPlayer().setSuspendDefaultMusicPlayback(false);
            Global.getSoundPlayer().restartCurrentMusic();
        }

        // Static campaign objects are generated for a new campaign only.
        // Their serialized instances are authoritative afterward: a missing
        // system in an existing save is left missing instead of being
        // reconstructed during load.
        if (newGame) {
            TroyArrivalScript.ensureWaypointTroyExists();
            OdysseyExpanseSystem.ensureExists();
        }
        AviciHabitatEncounter.reconcileSaraDeathFlag();
        OdysseyExpanseSystem.repairClusterJumpPointSpacing();
        chiefnavigator.quest.AlphaOdysseyJumpRepair.repairDuplicates();
        Global.getSector().removeTransientScriptsOfClass(
                FobIthacaApproachScript.class);
        Global.getSector().addTransientScript(
                new FobIthacaApproachScript());
        Global.getSector().removeTransientScriptsOfClass(
                chiefnavigator.campaign.SilentWakeCurrentTrace.class);
        Global.getSector().addTransientScript(
                new chiefnavigator.campaign.SilentWakeCurrentTrace());
        Global.getLogger(ChiefNavigatorModPlugin.class).info("SILENT_WAKE_TRACE_V2 armed");
        Global.getSector().removeTransientScriptsOfClass(
                MenelausTrialScript.class);
        Global.getSector().addTransientScript(new MenelausTrialScript());
        Global.getSector().removeTransientScriptsOfClass(
                OdysseyPredatorScript.class);
        Global.getSector().addTransientScript(new OdysseyPredatorScript());
        Global.getSector().removeTransientScriptsOfClass(
                OdysseyStrandedFleetsScript.class);
        Global.getSector().addTransientScript(
                new OdysseyStrandedFleetsScript());
        LeagueSurvivorRescueScript.install();
        Global.getSector().removeTransientScriptsOfClass(
                FobIthacaRingCollisionScript.class);
        Global.getSector().addTransientScript(
                new FobIthacaRingCollisionScript());

        SinniEventideIntel.reconcile();
        SinniOneMoreHorizonIntel.reconcile();

        ensureSinniBarEvent();

        Global.getSector().unregisterPlugin(TreadmillCampaignPlugin.ID);
        Global.getSector().registerPlugin(new TreadmillCampaignPlugin());

        Global.getSector().getCharacterData().removeAbility(REMOVED_ODYSSEY_ABILITY);
        Global.getSector().getCharacterData().getMemoryWithoutUpdate()
                .unset("$ability:" + REMOVED_ODYSSEY_ABILITY);
        if (Global.getSector().getPlayerFleet() != null) {
            Global.getSector().getPlayerFleet().removeAbility(REMOVED_ODYSSEY_ABILITY);
        }

        if (!Global.getSector().hasScript(TreadmillEntryScript.class)) {
            Global.getSector().addScript(new TreadmillEntryScript());
        }

        Global.getSector().removeTransientScriptsOfClass(
                OdysseyExpanseControlScript.class);
        OdysseyExpanseControlScript.restoreTransverseJump();
    }

    @Override
    public void onNewGameAfterEconomyLoad() {
        ensureSinniBarEvent();
    }

    /** Mirrors Hall of Triumph's working creator-registration lifecycle. */
    private void ensureSinniBarEvent() {
        if (Global.getSector() == null) return;
        SinniBarEvent.ensureCreatorRegistered();
        Global.getSector().removeScriptsOfClass(
                SinniBarEventAvailabilityScript.class);
        Global.getSector().addScript(new SinniBarEventAvailabilityScript());
    }

    private void initializeDomainCombatGuardRelations() {
        FactionAPI guard = Global.getSector().getFaction(
                IthacaSectionEncounter.COMBAT_GUARD_FACTION_ID);
        FactionAPI player = Global.getSector().getFaction(Factions.PLAYER);
        FactionAPI derelict = Global.getSector().getFaction(Factions.DERELICT);
        FactionAPI threat = Global.getSector().getFaction(Factions.THREAT);
        FactionAPI omega = Global.getSector().getFaction(Factions.OMEGA);
        if (guard == null) return;
        // The surviving Ithaca bastions recognize the player's IFF. They
        // must not side with unrelated Derelicts when a nearby fleet battle
        // gathers station reinforcements.
        guard.setRelationship(Factions.PLAYER, 1f);
        guard.setRelationship(Factions.DERELICT, -1f);
        guard.setRelationship(Factions.THREAT, -1f);
        guard.setRelationship(Factions.OMEGA, -1f);
        if (player != null) {
            player.setRelationship(
                    IthacaSectionEncounter.COMBAT_GUARD_FACTION_ID, 1f);
        }
        if (derelict != null) {
            derelict.setRelationship(
                    IthacaSectionEncounter.COMBAT_GUARD_FACTION_ID, -1f);
        }
        if (threat != null) {
            threat.setRelationship(
                    IthacaSectionEncounter.COMBAT_GUARD_FACTION_ID, -1f);
        }
        if (omega != null) {
            omega.setRelationship(
                    IthacaSectionEncounter.COMBAT_GUARD_FACTION_ID, -1f);
        }
        Global.getSector().getMemoryWithoutUpdate().set(
                COMBAT_GUARD_RELATIONS_INITIALIZED, true);
    }

    private void refreshOverriddenAbilityPlugins() {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return;
        refreshAbility(fleet, Abilities.GENERATE_SLIPSURGE,
                SinniGenerateSlipsurgeAbility.class,
                GenerateSlipsurgeAbility.class);
        refreshAbility(fleet, Abilities.GO_DARK,
                SinniAmbushStanceAbility.class, GoDarkAbility.class);
    }

    private void removeRetiredAmbushStanceAbility() {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return;
        AbilityPlugin retired = fleet.getAbility(SinniAmbushStanceAbility.ID);
        if (retired != null) {
            retired.deactivate();
            fleet.removeAbility(SinniAmbushStanceAbility.ID);
        }
        Global.getSector().getCharacterData().removeAbility(
                SinniAmbushStanceAbility.ID);
    }

    private void restoreRetiredAbilityOverrides() {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return;
        AbilityPlugin emergency = fleet.getAbility(Abilities.EMERGENCY_BURN);
        if (emergency instanceof SinniEmergencyBurnAbility) {
            emergency.deactivate();
            fleet.removeAbility(Abilities.EMERGENCY_BURN);
            fleet.addAbility(Abilities.EMERGENCY_BURN);
        }
        AbilityPlugin interdiction = fleet.getAbility(Abilities.INTERDICTION_PULSE);
        if (interdiction instanceof SinniInterdictionPulseAbility) {
            interdiction.deactivate();
            fleet.removeAbility(Abilities.INTERDICTION_PULSE);
            fleet.addAbility(Abilities.INTERDICTION_PULSE);
        }
    }

    private void refreshAbility(CampaignFleetAPI fleet, String abilityId,
                                Class<?> expectedPluginClass,
                                Class<?> replaceableVanillaClass) {
        AbilityPlugin current = fleet.getAbility(abilityId);
        if (current == null) {
            fleet.addAbility(abilityId);
            return;
        }
        if (current.getClass() == expectedPluginClass) return;
        // Do not deactivate or replace an unknown implementation. In
        // particular, preserve third-party subclasses and their active state.
        if (current.getClass() != replaceableVanillaClass) return;
        current.deactivate();
        fleet.removeAbility(abilityId);
        fleet.addAbility(abilityId);
    }
}
