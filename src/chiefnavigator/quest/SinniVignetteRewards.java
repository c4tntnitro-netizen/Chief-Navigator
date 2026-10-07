package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin.DerelictShipData;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.StarTypes;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.PerShipData;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.ShipCondition;
import com.fs.starfarer.api.util.Misc;
import java.util.Random;

/** Tiny active rewards plus save-compatible bonuses from retired categories. */
final class SinniVignetteRewards {
    static final float NEBULA_SENSOR_PROFILE_MULT = 0.99f;
    static final float BLACK_HOLE_DAMAGE_MULT = 0.90f;
    static final float NEUTRON_ACCELERATION_MULT = 1.01f;
    static final float BROWN_DWARF_FUEL_USE_MULT = 0.99f;
    static final float WHITE_DWARF_SUPPLY_USE_MULT = 0.99f;
    static final float GIANT_STAR_SURVEY_COST_MULT = 0.99f;
    static final float MASSIVE_STAR_CREW_LOSS_MULT = 0.99f;
    static final float ALL_TYPES_BURN_SPEED_CAP_BONUS = 2f;
    static final float ALL_TYPES_BASE_BURN_BONUS = 1f;

    private static final String NEBULA_MOD =
            "chief_navigator_vignette_nebula_plotting";
    private static final String BLACK_HOLE_MOD =
            "chief_navigator_sinni_black_hole_mastery";
    private static final String NEUTRON_MOD =
            "chief_navigator_vignette_pulsar_timing";
    private static final String BROWN_DWARF_MOD =
            "chief_navigator_vignette_poor_light_navigation";
    private static final String WHITE_DWARF_MOD =
            "chief_navigator_vignette_remnant_economy";
    private static final String GIANT_STAR_MOD =
            "chief_navigator_vignette_changing_seas";
    private static final String MASSIVE_STAR_MOD =
            "chief_navigator_vignette_stellar_wind_drills";
    private static final String ALL_TYPES_SPEED_CAP_MOD =
            "chief_navigator_vignette_completed_chart_speed_cap";
    private static final String ALL_TYPES_BASE_BURN_MOD =
            "chief_navigator_vignette_completed_chart_base_burn";

    private static final String MAIN_SEQUENCE_FLEET_ID =
            "chief_navigator_vignette_main_sequence_pirate";
    private static final String MAIN_SEQUENCE_FLEET_MARKER =
            "$chief_navigator_vignette_main_sequence_pirate";
    private static final String MAIN_SEQUENCE_WRECK_ID =
            "chief_navigator_vignette_main_sequence_kite_wreck";
    private static final String MAIN_SEQUENCE_WRECK_MARKER =
            "$chief_navigator_vignette_main_sequence_kite_wreck";
    private static final String MAIN_SEQUENCE_REWARD_SPAWNED =
            "$chief_navigator_vignette_main_sequence_reward_spawned_v1";
    private static final String MAIN_SEQUENCE_WRECK_REVEALED =
            "$chief_navigator_vignette_main_sequence_wreck_revealed_v1";
    private static final String MAIN_SEQUENCE_EVENT_DRIVEN =
            "$chief_navigator_vignette_main_sequence_event_driven_v3";
    private static final String MAIN_SEQUENCE_STAGED_WRECK =
            "$chief_navigator_vignette_main_sequence_staged_wreck_v2";
    private static final String MAIN_SEQUENCE_STAGED_SYSTEM =
            "$chief_navigator_vignette_main_sequence_staged_system_v2";
    private static final String MAIN_SEQUENCE_PIRATE_DEFEATED =
            "$chief_navigator_vignette_main_sequence_pirate_defeated_v1";
    private static final String MAIN_SEQUENCE_REWARD_INVALID_V1 =
            "$chief_navigator_vignette_main_sequence_reward_invalid_v1";
    private static final String MAIN_SEQUENCE_WRECK_IMPORTANT =
            "chief_navigator_vignette_main_sequence_kite";
    private static final String KITE_VARIANT = "kite_original_Stock";

    private SinniVignetteRewards() { }

    static void advance(CampaignFleetAPI player) {
        if (Global.getSector() == null || player == null) return;
        applyPassiveBonuses(player);
        ensureMainSequenceReward(player);
        maintainMainSequenceReward();
    }

    static void onVignetteCompleted(
            SinniSystemVignetteScript.Category category) {
        if (category != SinniSystemVignetteScript.Category.MAIN_SEQUENCE
                || Global.getSector() == null) {
            return;
        }
        ensureMainSequenceReward(Global.getSector().getPlayerFleet());
    }

    private static void applyPassiveBonuses(CampaignFleetAPI player) {
        boolean chartComplete =
                SinniSystemVignetteScript.isOneMoreHorizonComplete();
        if (chartComplete) {
            float speedPerBurn = Global.getSettings().getFloat(
                    "speedPerBurnLevel");
            player.getStats().getMovementSpeedMod().modifyFlat(
                    ALL_TYPES_SPEED_CAP_MOD,
                    ALL_TYPES_BURN_SPEED_CAP_BONUS * speedPerBurn,
                    "Sinni: completed stellar chart");
            player.getStats().getFleetwideMaxBurnMod().modifyFlat(
                    ALL_TYPES_BASE_BURN_MOD,
                    ALL_TYPES_BASE_BURN_BONUS,
                    "Sinni: completed stellar chart");
        } else {
            player.getStats().getMovementSpeedMod().unmodifyFlat(
                    ALL_TYPES_SPEED_CAP_MOD);
            player.getStats().getFleetwideMaxBurnMod().unmodifyFlat(
                    ALL_TYPES_BASE_BURN_MOD);
        }

        boolean nebula = complete(SinniSystemVignetteScript.Category.NEBULA);
        if (nebula) {
            player.getStats().getSensorProfileMod().modifyMult(
                    NEBULA_MOD,
                    NEBULA_SENSOR_PROFILE_MULT,
                    "Sinni: nebula plotting");
        } else {
            player.getStats().getSensorProfileMod().unmodify(NEBULA_MOD);
        }

        boolean neutron = complete(
                SinniSystemVignetteScript.Category.NEUTRON_STAR);
        if (neutron) {
            player.getStats().getAccelerationMult().modifyMult(
                    NEUTRON_MOD,
                    NEUTRON_ACCELERATION_MULT,
                    "Sinni: pulsar timing");
        } else {
            player.getStats().getAccelerationMult().unmodify(NEUTRON_MOD);
        }

        boolean brownDwarf = complete(
                SinniSystemVignetteScript.Category.BROWN_DWARF);
        if (brownDwarf) {
            player.getStats().getFuelUseHyperMult().modifyMult(
                    BROWN_DWARF_MOD,
                    BROWN_DWARF_FUEL_USE_MULT,
                    "Sinni: poor-light navigation");
        } else {
            player.getStats().getFuelUseHyperMult().unmodify(
                    BROWN_DWARF_MOD);
        }

        boolean giant = complete(
                SinniSystemVignetteScript.Category.GIANT_STAR);
        if (giant) {
            player.getStats().getDynamic().getStat(Stats.SURVEY_COST_MULT)
                    .modifyMult(
                            GIANT_STAR_MOD,
                            GIANT_STAR_SURVEY_COST_MULT,
                            "Sinni: changing-seas survey models");
        } else {
            player.getStats().getDynamic().getStat(Stats.SURVEY_COST_MULT)
                    .unmodify(GIANT_STAR_MOD);
        }

        boolean massive = complete(
                SinniSystemVignetteScript.Category.MASSIVE_STAR);
        if (massive) {
            player.getStats().getDynamic().getStat(
                    Stats.NON_COMBAT_CREW_LOSS_MULT).modifyMult(
                            MASSIVE_STAR_MOD,
                            MASSIVE_STAR_CREW_LOSS_MULT,
                            "Sinni: stellar-wind drills");
        } else {
            player.getStats().getDynamic().getStat(
                    Stats.NON_COMBAT_CREW_LOSS_MULT).unmodify(
                            MASSIVE_STAR_MOD);
        }

        StarSystemAPI system = player.getContainingLocation()
                instanceof StarSystemAPI
                ? (StarSystemAPI) player.getContainingLocation() : null;
        boolean blackHole = complete(
                SinniSystemVignetteScript.Category.BLACK_HOLE)
                && isBlackHoleSystem(system);
        boolean whiteDwarf = complete(
                SinniSystemVignetteScript.Category.WHITE_DWARF);
        for (FleetMemberAPI member
                : player.getFleetData().getMembersListCopy()) {
            if (blackHole) {
                member.getStats().getDynamic().getStat(
                        Stats.CORONA_EFFECT_MULT).modifyMult(
                                BLACK_HOLE_MOD,
                                BLACK_HOLE_DAMAGE_MULT,
                                "Sinni: black-hole mastery");
            } else {
                member.getStats().getDynamic().getStat(
                        Stats.CORONA_EFFECT_MULT).unmodify(BLACK_HOLE_MOD);
            }

            if (whiteDwarf) {
                member.getStats().getSuppliesPerMonth().modifyMult(
                        WHITE_DWARF_MOD,
                        WHITE_DWARF_SUPPLY_USE_MULT,
                        "Sinni: remnant economy");
            } else {
                member.getStats().getSuppliesPerMonth().unmodify(
                        WHITE_DWARF_MOD);
            }
        }
    }

    private static boolean complete(
            SinniSystemVignetteScript.Category category) {
        return SinniSystemVignetteScript.isComplete(category);
    }

    private static boolean isBlackHoleSystem(StarSystemAPI system) {
        if (system == null) return false;
        if (system.hasBlackHole()) return true;
        PlanetAPI star = system.getStar();
        return star != null && StarTypes.BLACK_HOLE.equals(star.getTypeId());
    }

    private static void ensureMainSequenceReward(CampaignFleetAPI player) {
        if (player == null
                || !complete(SinniSystemVignetteScript.Category.MAIN_SEQUENCE)
                || memory().getBoolean(MAIN_SEQUENCE_REWARD_SPAWNED)
                || !(player.getContainingLocation() instanceof StarSystemAPI)) {
            return;
        }
        StarSystemAPI system = (StarSystemAPI) player.getContainingLocation();
        SinniSystemVignetteScript.Candidate candidate =
                SinniSystemVignetteScript.classifySystem(system);
        if (candidate == null
                || candidate.category
                        != SinniSystemVignetteScript.Category.MAIN_SEQUENCE) {
            return;
        }

        SectorEntityToken fleetClaim = findEntity(MAIN_SEQUENCE_FLEET_ID);
        SectorEntityToken wreckClaim = findEntity(MAIN_SEQUENCE_WRECK_ID);
        if (fleetClaim != null || wreckClaim != null
                || memory().contains(MAIN_SEQUENCE_STAGED_WRECK)) {
            memory().set(MAIN_SEQUENCE_REWARD_SPAWNED, true);
            memory().set(MAIN_SEQUENCE_REWARD_INVALID_V1, true);
            Global.getLogger(SinniVignetteRewards.class).error(
                    "Main-sequence reward IDs contain pre-existing or "
                            + "partial serialized state; suppressing "
                            + "reconstruction");
            return;
        }

        // Commit before touching factory or variant data. A missing or broken
        // third-party dependency may forfeit this tiny reward, but it cannot
        // turn into an every-frame duplicate-spawn/crash loop.
        memory().set(MAIN_SEQUENCE_REWARD_SPAWNED, true);

        float angle = (float) Math.toRadians(player.getFacing() + 150f);
        float x = player.getLocation().x + (float) Math.cos(angle) * 2400f;
        float y = player.getLocation().y + (float) Math.sin(angle) * 2400f;

        try {
        CampaignFleetAPI pirate = Global.getFactory().createEmptyFleet(
                Factions.PIRATES, "Lone Pirate Raider", true);
        pirate.setId(MAIN_SEQUENCE_FLEET_ID);
        pirate.setNoFactionInName(true);
        pirate.setTransponderOn(false);
        pirate.setNoAutoDespawn(true);
        pirate.setAbortDespawn(true);
        pirate.getMemoryWithoutUpdate().set(
                MAIN_SEQUENCE_FLEET_MARKER, true);
        pirate.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
        pirate.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        pirate.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE, true);
        pirate.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        pirate.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
        pirate.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_NO_JUMP, true);

        FleetMemberAPI kite = Global.getFactory().createFleetMember(
                FleetMemberType.SHIP, KITE_VARIANT);
        PersonAPI captain = pirate.getFaction().createRandomPerson();
        pirate.setCommander(captain);
        pirate.getFleetData().addFleetMember(kite);
        pirate.getFleetData().setFlagship(kite);
        kite.setCaptain(captain);
        kite.getStatus().repairFully();
        kite.getRepairTracker().setCR(kite.getRepairTracker().getMaxCR());
        pirate.getFleetData().setSyncNeeded();
        pirate.getFleetData().syncIfNeeded();
        system.addEntity(pirate);
        pirate.setLocation(x, y);
        pirate.addAssignment(
                FleetAssignment.INTERCEPT,
                player,
                1000000f,
                "closing on your fleet");
        ensureKiteDefeatListener(pirate);
        memory().set(MAIN_SEQUENCE_EVENT_DRIVEN, true);
        } catch (RuntimeException ex) {
            memory().set(MAIN_SEQUENCE_REWARD_INVALID_V1, true);
            Global.getLogger(SinniVignetteRewards.class).error(
                    "Main-sequence reward construction failed; leaving "
                            + "partial state untouched and disabling retry",
                    ex);
        }
    }

    private static void maintainMainSequenceReward() {
        if (!memory().getBoolean(MAIN_SEQUENCE_REWARD_SPAWNED)
                || memory().getBoolean(MAIN_SEQUENCE_REWARD_INVALID_V1)
                || memory().getBoolean(MAIN_SEQUENCE_WRECK_REVEALED)
                || memory().getBoolean(MAIN_SEQUENCE_EVENT_DRIVEN)) {
            return;
        }
        CampaignFleetAPI pirate = findPirate();
        Object staged = memory().get(MAIN_SEQUENCE_STAGED_WRECK);
        if (staged != null && !(staged instanceof SectorEntityToken)) {
            invalidateMainSequenceReward(
                    "Main-sequence staged wreck contains incompatible state");
            return;
        }
        SectorEntityToken wreck = staged instanceof SectorEntityToken
                ? (SectorEntityToken) staged : findEntity(MAIN_SEQUENCE_WRECK_ID);
        SectorEntityToken pirateClaim = findEntity(MAIN_SEQUENCE_FLEET_ID);
        if ((wreck != null && !isOwnedMainSequenceWreck(wreck))
                || (pirateClaim != null && pirate == null)) {
            memory().set(MAIN_SEQUENCE_REWARD_INVALID_V1, true);
            Global.getLogger(SinniVignetteRewards.class).error(
                    "Main-sequence reward ID is owned by incompatible "
                            + "serialized state; refusing mutation");
            return;
        }
        // Compatibility only: older versions already prepared a hidden wreck.
        // New encounters create no wreck at all until the pirate is destroyed.
        try {
            if (wreck != null) hideWreck(wreck);
        } catch (RuntimeException failure) {
            memory().set(MAIN_SEQUENCE_REWARD_INVALID_V1, true);
            Global.getLogger(SinniVignetteRewards.class).error(
                    "Main-sequence wreck staging failed; preserving the "
                            + "prepared reward and disabling retry", failure);
            return;
        }
        if (memory().getBoolean(MAIN_SEQUENCE_REWARD_INVALID_V1)) return;
        if (pirate != null) ensureKiteDefeatListener(pirate);
        if (pirate != null && !pirate.isEmpty()) {
            memory().set(MAIN_SEQUENCE_EVENT_DRIVEN, true);
            return;
        }
        if (pirate != null && pirate.isEmpty()
                && !TroyArrivalScript.isFleetBusyForMutation(pirate)) {
            memory().set(MAIN_SEQUENCE_PIRATE_DEFEATED, true);
        }
        if (!memory().getBoolean(MAIN_SEQUENCE_PIRATE_DEFEATED)) return;
        completeMainSequenceReward(pirate);
    }

    private static void completeMainSequenceReward(CampaignFleetAPI pirate) {
        if (!memory().getBoolean(MAIN_SEQUENCE_REWARD_SPAWNED)
                || !memory().getBoolean(MAIN_SEQUENCE_PIRATE_DEFEATED)
                || memory().getBoolean(MAIN_SEQUENCE_REWARD_INVALID_V1)
                || memory().getBoolean(MAIN_SEQUENCE_WRECK_REVEALED)) {
            return;
        }
        Object staged = memory().get(MAIN_SEQUENCE_STAGED_WRECK);
        SectorEntityToken wreck = staged instanceof SectorEntityToken
                ? (SectorEntityToken) staged : findEntity(MAIN_SEQUENCE_WRECK_ID, true);
        if ((staged != null && !(staged instanceof SectorEntityToken))
                || (wreck != null && !isOwnedMainSequenceWreck(wreck))
                || (pirate != null && !isOwnedMainSequencePirate(pirate))) {
            invalidateMainSequenceReward("Main-sequence reward has incompatible ownership");
            return;
        }
        LocationAPI location = pirate == null ? null : pirate.getContainingLocation();
        if (wreck == null && !(location instanceof StarSystemAPI)) {
            invalidateMainSequenceReward("Main-sequence reward has no battle location");
            return;
        }
        // Consume the one shot before factory or recovery code. Repeated battle
        // callbacks, upkeep, and reloads may never create another reward.
        memory().set(MAIN_SEQUENCE_WRECK_REVEALED, true);
        try {
            if (wreck == null) {
                PerShipData ship = new PerShipData(KITE_VARIANT, ShipCondition.PRISTINE, 0f);
                ship.nameAlwaysKnown = true;
                wreck = BaseThemeGenerator.addSalvageEntity(
                        location, Entities.WRECK, Factions.NEUTRAL,
                        new DerelictShipData(ship, false));
                wreck.setId(MAIN_SEQUENCE_WRECK_ID);
                wreck.getMemoryWithoutUpdate().set(MAIN_SEQUENCE_WRECK_MARKER, true);
                wreck.setName("Pristine Kite (S) Wreck");
                SalvageSpecialAssigner.ShipRecoverySpecialCreator creator =
                        new SalvageSpecialAssigner.ShipRecoverySpecialCreator(
                                new Random(0x4b69746553534cL), 0, 0, false, null, null);
                Misc.setSalvageSpecial(wreck, creator.createSpecial(wreck, null));
            }
            if (pirate != null) {
                wreck.setFixedLocation(pirate.getLocation().x, pirate.getLocation().y);
                wreck.setFacing(pirate.getFacing());
            }
            if (!revealWreck(wreck)) return;
            memory().unset(MAIN_SEQUENCE_STAGED_WRECK);
            memory().unset(MAIN_SEQUENCE_STAGED_SYSTEM);
        } catch (RuntimeException failure) {
            memory().set(MAIN_SEQUENCE_REWARD_INVALID_V1, true);
            Global.getLogger(SinniVignetteRewards.class).error(
                    "Main-sequence wreck reveal failed; preserving the "
                            + "prepared reward and disabling retry", failure);
        }
    }

    private static void ensureKiteDefeatListener(CampaignFleetAPI pirate) {
        for (FleetEventListener listener : pirate.getEventListeners()) {
            if (listener instanceof KiteDefeatListener) return;
        }
        pirate.addEventListener(new KiteDefeatListener());
    }

    private static final class KiteDefeatListener implements FleetEventListener {
        @Override
        public void reportBattleOccurred(CampaignFleetAPI fleet,
                CampaignFleetAPI primaryWinner, BattleAPI battle) {
            if (battle == null || !isOwnedMainSequencePirate(fleet) || !fleet.isEmpty()) return;
            memory().set(MAIN_SEQUENCE_PIRATE_DEFEATED, true);
            completeMainSequenceReward(fleet);
        }

        @Override
        public void reportFleetDespawnedToListener(CampaignFleetAPI fleet,
                FleetDespawnReason reason, Object param) {
            if (reason != FleetDespawnReason.DESTROYED_BY_BATTLE
                    || !isOwnedMainSequencePirate(fleet)) return;
            memory().set(MAIN_SEQUENCE_PIRATE_DEFEATED, true);
            completeMainSequenceReward(fleet);
        }
    }

    private static boolean isOwnedMainSequencePirate(CampaignFleetAPI fleet) {
        return fleet != null && !fleet.isPlayerFleet()
                && fleet != Global.getSector().getPlayerFleet()
                && MAIN_SEQUENCE_FLEET_ID.equals(fleet.getId())
                && fleet.getMemoryWithoutUpdate() != null
                && fleet.getMemoryWithoutUpdate().getBoolean(MAIN_SEQUENCE_FLEET_MARKER);
    }

    private static CampaignFleetAPI findPirate() {
        SectorEntityToken entity = findEntity(MAIN_SEQUENCE_FLEET_ID);
        if (entity instanceof CampaignFleetAPI) {
            CampaignFleetAPI fleet = (CampaignFleetAPI) entity;
            return isOwnedMainSequencePirate(fleet) ? fleet : null;
        }
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            for (CampaignFleetAPI fleet : system.getFleets()) {
                if (isOwnedMainSequencePirate(fleet)) return fleet;
            }
        }
        return null;
    }

    private static boolean isOwnedMainSequenceWreck(
            SectorEntityToken wreck) {
        return wreck != null
                && !(wreck instanceof CampaignFleetAPI)
                && MAIN_SEQUENCE_WRECK_ID.equals(wreck.getId())
                && Entities.WRECK.equals(wreck.getCustomEntityType())
                && wreck.getMemoryWithoutUpdate() != null
                && wreck.getMemoryWithoutUpdate().getBoolean(
                        MAIN_SEQUENCE_WRECK_MARKER);
    }

    private static SectorEntityToken findEntity(String id) {
        return findEntity(id, false);
    }

    private static SectorEntityToken findEntity(String id, boolean aliveOnly) {
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            SectorEntityToken entity = system.getEntityById(id);
            if (entity != null && (!aliveOnly || entity.isAlive())) return entity;
        }
        LocationAPI hyperspace = Global.getSector().getHyperspace();
        SectorEntityToken entity = hyperspace == null
                ? null : hyperspace.getEntityById(id);
        return entity != null && (!aliveOnly || entity.isAlive()) ? entity : null;
    }

    private static void hideWreck(SectorEntityToken wreck) {
        if (memory().get(MAIN_SEQUENCE_STAGED_WRECK) == wreck && !wreck.isAlive()) return;
        LocationAPI location = wreck.getContainingLocation();
        if (!(location instanceof StarSystemAPI)) {
            invalidateMainSequenceReward(
                    "Main-sequence wreck has no valid staging system");
            return;
        }
        // Retain the fully prepared, serialized wreck outside the active
        // entity collection. A zero profile is still detectable nearby;
        // forcing its fader out merely creates another acquisition ping.
        memory().set(MAIN_SEQUENCE_STAGED_WRECK, wreck);
        memory().set(MAIN_SEQUENCE_STAGED_SYSTEM, location.getId());
        wreck.addTag(Tags.NON_CLICKABLE);
        wreck.addTag(Tags.NO_ENTITY_TOOLTIP);
        wreck.setDiscoverable(false);
        location.removeEntity(wreck);
    }

    private static boolean revealWreck(SectorEntityToken wreck) {
        String systemId = memory().getString(MAIN_SEQUENCE_STAGED_SYSTEM);
        LocationAPI destination = wreck.getContainingLocation();
        if (systemId != null) {
            destination = null;
            for (StarSystemAPI system : Global.getSector().getStarSystems()) {
                if (system.getId().equals(systemId)) {
                    destination = system;
                    break;
                }
            }
        }
        SectorEntityToken claim = findEntity(MAIN_SEQUENCE_WRECK_ID, true);
        if (destination == null || claim != null && claim != wreck) {
            invalidateMainSequenceReward(
                    "Main-sequence wreck reveal has a missing system or conflicting ID");
            return false;
        }
        if (wreck.isAlive() && wreck.getContainingLocation() != destination) {
            invalidateMainSequenceReward(
                    "Main-sequence wreck is attached to an incompatible system");
            return false;
        }
        // getEntityById's cache may still contain a token removed earlier in
        // this update. isAlive checks actual repository membership instead.
        if (!wreck.isAlive()) destination.addEntity(wreck);
        wreck.removeTag(Tags.NON_CLICKABLE);
        wreck.removeTag(Tags.NO_ENTITY_TOOLTIP);
        wreck.setDiscoverable(null);
        wreck.setSensorProfile(null);
        Misc.makeImportant(wreck, MAIN_SEQUENCE_WRECK_IMPORTANT);
        return true;
    }

    private static void invalidateMainSequenceReward(String message) {
        memory().set(MAIN_SEQUENCE_REWARD_INVALID_V1, true);
        Global.getLogger(SinniVignetteRewards.class).error(message);
    }

    private static com.fs.starfarer.api.campaign.rules.MemoryAPI memory() {
        return Global.getSector().getMemoryWithoutUpdate();
    }
}
