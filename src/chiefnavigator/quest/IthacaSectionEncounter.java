package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import java.util.List;
import org.lwjgl.util.vector.Vector2f;

/** Three persistent, fixed, independently attackable FOB Ithaca stations. */
public final class IthacaSectionEncounter {
    public static final String COMBAT_GUARD_FACTION_ID =
            "chief_navigator_domain_combat_guard";
    public static final String FOB_ENTITY_ID = "chief_navigator_fob_ithaca";
    public static final String MARKER = "$chief_navigator_ithaca_section_combat";
    public static final String SECTION_KEY = "$chief_navigator_ithaca_section_id";
    public static final String INTERACTION_MARKER =
            "$chief_navigator_ithaca_section_interaction";
    public static final String WALL_HULL_ID =
            "chief_navigator_ithaca_intact_base";
    public static final String REAR_BULKHEAD_HULL_ID =
            "chief_navigator_ithaca_rear_bulkhead";
    public static final String CORE_HULL_ID = "chief_navigator_ithaca_section_core";

    private static final String LEGACY_PROXY_ID =
            "chief_navigator_ithaca_section_combat_proxy";
    private static final String STATION_ID_PREFIX =
            "chief_navigator_ithaca_station_";
    static final String INTERACTION_ID_PREFIX =
            "chief_navigator_ithaca_bastion_interaction_";
    static final String INTERACTION_ENTITY_TYPE =
            "chief_navigator_fob_ithaca_bastion_interaction";
    private static final String MEMBER_ID_PREFIX =
            "chief_navigator_ithaca_member_";
    private static final String ACTIVE_SECTION_KEY =
            "$chief_navigator_ithaca_active_section";
    private static final String WALL_VARIANT =
            "chief_navigator_ithaca_intact_base_Standard";
    private static final String REAR_BULKHEAD_VARIANT =
            "chief_navigator_ithaca_rear_bulkhead_Standard";
    private static final String CORE_VARIANT =
            "chief_navigator_ithaca_section_core_Active";
    private static final String CORE_DISABLED_IN_COMBAT_PREFIX =
            "$chief_navigator_ithaca_section_core_disabled_in_combat_";
    private static final String STATION_CREATED_PREFIX =
            "$chief_navigator_ithaca_section_created_v1_";
    private static final String STATION_INVALID_PREFIX =
            "$chief_navigator_ithaca_section_invalid_v1_";
    private static final String INTERACTION_CREATED_PREFIX =
            "$chief_navigator_ithaca_section_interaction_created_v1_";
    private static final String REBUILD_PROGRESS_PREFIX =
            "$chief_navigator_ithaca_bastion_rebuild_days_";
    static final float BASTION_REPAIR_DAYS = 30f;
    private static final String STATION_DRIFT_TRACE =
            "$chief_navigator_ithaca_station_drift_trace_v1";
    private static final String EMPTY_CAMPAIGN_SPRITE =
            "graphics/fx/empty.png";
    private static final String COMBAT_WALL_SPRITE =
            "graphics/ships/chief_navigator_ithaca_intact_generated_v1.png";
    private static final Vector2f COMBAT_WALL_SIZE =
            new Vector2f(5632f, 2216f);
    private static final int ACTIVE_MEMBER_COUNT = 1;
    // The solid ring resolves approaching fleets to roughly 1,930 units from
    // center. Keep the invisible, fightable station outside that boundary so
    // an attacker can physically overlap it and initiate a campaign battle.
    private static final float COMBAT_STATION_RADIUS = 2150f;
    private static final float STATION_DRIFT_LOG_THRESHOLD = 2f;
    private static final Vector2f HIDDEN_CAMPAIGN_SPRITE_SIZE =
            new Vector2f(1f, 1f);
    static final float CAMPAIGN_INTERACTION_RADIUS = 450f;

    private IthacaSectionEncounter() { }

    public enum Section {
        NORTH("north", "North Bastion", 0f, 1650f,
                0f, COMBAT_STATION_RADIUS,
                "$chief_navigator_ithaca_north_disabled"),
        EAST("east", "East Bastion", 1650f, 0f,
                COMBAT_STATION_RADIUS, 0f,
                "$chief_navigator_ithaca_east_disabled"),
        SOUTH("south", "South Bastion", 0f, -1650f,
                0f, -COMBAT_STATION_RADIUS,
                "$chief_navigator_ithaca_south_disabled");

        public final String id;
        public final String displayName;
        private final float offsetX;
        private final float offsetY;
        private final float combatOffsetX;
        private final float combatOffsetY;
        private final String disabledKey;

        Section(
                String id,
                String displayName,
                float offsetX,
                float offsetY,
                float combatOffsetX,
                float combatOffsetY,
                String disabledKey) {
            this.id = id;
            this.displayName = displayName;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.combatOffsetX = combatOffsetX;
            this.combatOffsetY = combatOffsetY;
            this.disabledKey = disabledKey;
        }

        public static Section fromId(String id) {
            if (id == null) return null;
            for (Section section : values()) {
                if (section.id.equalsIgnoreCase(id)) return section;
            }
            return null;
        }
    }

    public static boolean isDisabled(
            SectorEntityToken station, Section section) {
        return OdysseyExpanseSystem.isFobIthaca(station) && section != null
                && station.getMemoryWithoutUpdate().getBoolean(
                        section.disabledKey);
    }

    /** Preserves completed bastions when an old FOB campaign token migrates. */
    static boolean[] captureDisabledState(SectorEntityToken station) {
        Section[] sections = Section.values();
        boolean[] disabled = new boolean[sections.length];
        if (station == null) return disabled;
        for (int i = 0; i < sections.length; i++) {
            disabled[i] = station.getMemoryWithoutUpdate().getBoolean(
                    sections[i].disabledKey);
        }
        return disabled;
    }

    static void restoreDisabledState(
            SectorEntityToken station, boolean[] disabled) {
        if (station == null || disabled == null) return;
        Section[] sections = Section.values();
        for (int i = 0; i < sections.length && i < disabled.length; i++) {
            if (disabled[i]) {
                station.getMemoryWithoutUpdate().set(
                        sections[i].disabledKey, true);
            }
        }
    }

    /** Creates each campaign station once and otherwise preserves its token. */
    public static void ensureSectionStations(SectorEntityToken center) {
        ensureSectionStations(center, false);
    }

    private static void ensureSectionStations(
            SectorEntityToken center, boolean restoreDestroyed) {
        ensureSectionStations(center, restoreDestroyed, null);
    }

    private static void ensureSectionStations(
            SectorEntityToken center, boolean restoreDestroyed, Section only) {
        if (!OdysseyExpanseSystem.isFobIthaca(center)
                || center.getContainingLocation() == null) return;
        LocationAPI location = center.getContainingLocation();

        for (Section section : Section.values()) {
            if (only != null && section != only) continue;
            if (restoreDestroyed && isBastionBusy(center, section)) continue;
            String id = stationId(section);
            SectorEntityToken existing = location.getEntityById(id);
            if (existing instanceof CampaignFleetAPI
                    && isPlayerControlledFleet(
                            (CampaignFleetAPI) existing)) {
                logInvalidStationOnce(center, section,
                        "station ID is claimed by the player fleet");
                continue;
            }
            if (isDisabled(center, section)) {
                if (existing == null) {
                    removeSectionInteractionTarget(center, section);
                    continue;
                }
                if (!(existing instanceof CampaignFleetAPI)
                        || !isSectionStation(existing)
                        || getSectionForStation(
                                (CampaignFleetAPI) existing) != section) {
                    logInvalidStationOnce(center, section,
                            "disabled station ID is owned by incompatible "
                                    + "serialized state");
                    continue;
                }
                CampaignFleetAPI disabledFleet =
                        (CampaignFleetAPI) existing;
                if (TroyArrivalScript.isFleetBusyForMutation(
                        disabledFleet)) continue;
                location.removeEntity(disabledFleet);
                removeSectionInteractionTarget(center, section);
                continue;
            }
            CampaignFleetAPI fleet = existing instanceof CampaignFleetAPI
                    ? (CampaignFleetAPI) existing : null;
            boolean disabled = false;
            if (existing != null && fleet == null) {
                logInvalidStationOnce(center, section,
                        "station ID is occupied by "
                                + existing.getClass().getName());
                continue;
            }
            if (fleet == null) {
                if (!restoreDestroyed && (center.getMemoryWithoutUpdate().getBoolean(
                            stationCreatedKey(section))
                        || isEstablishedAshenVerge(location))) {
                    center.getMemoryWithoutUpdate().set(
                            stationCreatedKey(section), true);
                    continue;
                }
                fleet = createStation(center, section, disabled);
                center.getMemoryWithoutUpdate().set(
                        stationCreatedKey(section), true);
            } else {
                if (!isSectionStation(fleet)
                        || getSectionForStation(fleet) != section) {
                    logInvalidStationOnce(center, section,
                            "station ID is owned by incompatible serialized "
                                    + "state");
                    continue;
                }
                center.getMemoryWithoutUpdate().set(
                        stationCreatedKey(section), true);
            }
            if (fleet.getBattle() != null) continue;
            if (restoreDestroyed) {
                if (TroyArrivalScript.isFleetBusyForMutation(fleet)) continue;
                if (fleet.getFleetData().getMembersListCopy().isEmpty()) {
                    // Only an explicit reset or completed timed rebuild may
                    // refill the exact owned foundation.
                    FleetMemberAPI wall = addMember(
                            fleet, WALL_VARIANT, section, "wall", 0);
                    fleet.getFleetData().setFlagship(wall);
                    fleet.getFleetData().setSyncNeeded();
                    fleet.getFleetData().syncIfNeeded();
                }
            }
            if (!isValidStation(fleet, section, disabled)) {
                logInvalidStationOnce(center, section,
                        "serialized station roster is incompatible");
                continue;
            }
            configureCampaignStation(fleet, center, section, disabled);
            ensureSectionInteractionTarget(center, section, restoreDestroyed);
        }
    }

    /** Gameplay repairs, not a load-time world reconciliation or combat heal. */
    static void advanceBastionRepairs(SectorEntityToken center, float days) {
        if (Global.getSector() == null || Global.getSector().isPaused()
                || !Float.isFinite(days) || days <= 0f
                || MenelausTrial.isFinalLaborActive()
                || MenelausTrial.isFinalLaborFailed()
                || !OdysseyExpanseSystem.isFobIthaca(center)
                || center.getContainingLocation() == null) return;
        LocationAPI location = center.getContainingLocation();
        for (Section section : Section.values()) {
            SectorEntityToken claim = location.getEntityById(stationId(section));
            CampaignFleetAPI station = getStation(center, section);
            if ((claim != null && station == null)
                    || isBastionBusy(center, section)) continue;
            if (station != null && !station.getFleetData()
                    .getMembersListCopy().isEmpty()
                    && !isValidStation(station, section, false)) continue;

            String progressKey = REBUILD_PROGRESS_PREFIX + section.id;
            if (isDisabled(center, section) || station == null
                    || station.getFleetData().getMembersListCopy().isEmpty()) {
                // A missing ungenerated object is not a casualty to rebuild.
                if (station == null && !isDisabled(center, section)
                        && !center.getMemoryWithoutUpdate().getBoolean(
                                stationCreatedKey(section))
                        && !isEstablishedAshenVerge(location)) continue;
                Object saved = center.getMemoryWithoutUpdate().get(progressKey);
                double progress = saved instanceof Number
                        ? ((Number) saved).doubleValue() : 0d;
                progress = Math.min(BASTION_REPAIR_DAYS, progress + days);
                center.getMemoryWithoutUpdate().set(progressKey, progress);
                if (progress < BASTION_REPAIR_DAYS) continue;
                boolean disabled = isDisabled(center, section);
                center.getMemoryWithoutUpdate().unset(section.disabledKey);
                ensureSectionStations(center, true, section);
                station = getStation(center, section);
                if (station == null || !isValidStation(station, section, false)) {
                    if (disabled) center.getMemoryWithoutUpdate().set(
                            section.disabledKey, true);
                    continue;
                }
                repairStationFully(station);
                center.getMemoryWithoutUpdate().unset(progressKey);
            } else {
                center.getMemoryWithoutUpdate().unset(progressKey);
                for (FleetMemberAPI member
                        : station.getFleetData().getMembersListCopy()) {
                    member.getStatus().repairFraction(days / BASTION_REPAIR_DAYS);
                }
            }
        }
    }

    private static boolean isBastionBusy(SectorEntityToken center, Section section) {
        CampaignFleetAPI station = getStation(center, section);
        if (station != null && TroyArrivalScript.isFleetBusyForMutation(station)) {
            return true;
        }
        SectorEntityToken target = Global.getSector() == null
                || Global.getSector().getCampaignUI() == null
                || Global.getSector().getCampaignUI().getCurrentInteractionDialog() == null
                ? null : Global.getSector().getCampaignUI()
                        .getCurrentInteractionDialog().getInteractionTarget();
        return target != null && (target == center || target == station
                || target == center.getContainingLocation().getEntityById(
                        interactionId(section)));
    }

    static boolean isBastionReady(SectorEntityToken center, Section section) {
        return !isDisabled(center, section)
                && isValidStation(getStation(center, section), section, false)
                && !isBastionBusy(center, section);
    }

    /** Removes one Labor V bastion after its uninterrupted siege expires. */
    static boolean disableBastionForFinalLabor(
            SectorEntityToken center, Section section) {
        if (center == null || section == null
                || !OdysseyExpanseSystem.isFobIthaca(center)
                || center.getContainingLocation() == null) {
            return false;
        }
        CampaignFleetAPI station = getStation(center, section);
        if (station == null
                || TroyArrivalScript.isFleetBusyForMutation(station)) {
            if (center.getContainingLocation().getEntityById(
                        stationId(section)) != null) {
                logInvalidStationOnce(center, section,
                        "refusing to disable an incompatible or busy station "
                                + "claim");
            }
            return false;
        }
        SectorEntityToken interaction = Global.getSector() == null
                || Global.getSector().getCampaignUI() == null
                || Global.getSector().getCampaignUI()
                        .getCurrentInteractionDialog() == null
                ? null
                : Global.getSector().getCampaignUI()
                        .getCurrentInteractionDialog()
                        .getInteractionTarget();
        if (interaction == station
                || getSectionForInteractionTarget(interaction) == section) {
            return false;
        }

        center.getMemoryWithoutUpdate().set(section.disabledKey, true);
        LocationAPI location = center.getContainingLocation();
        location.removeEntity(station);
        removeSectionInteractionTarget(center, section);
        return true;
    }

    /** One-time story/testing reset, never called by ordinary station upkeep. */
    static void repairAllBastionsForFinalLabor(SectorEntityToken center) {
        if (!OdysseyExpanseSystem.isFobIthaca(center)
                || center.getContainingLocation() == null) return;
        for (Section section : Section.values()) {
            SectorEntityToken claim = center.getContainingLocation()
                    .getEntityById(stationId(section));
            CampaignFleetAPI station = getStation(center, section);
            if (claim != null && station == null) continue;
            if (station != null && !station.getFleetData()
                    .getMembersListCopy().isEmpty()
                    && !isValidStation(station, section, false)) continue;
            if (isBastionBusy(center, section)) continue;
            center.getMemoryWithoutUpdate().unset(section.disabledKey);
            // This is an explicit story reset, not a routine reconciliation.
            // A bastion deliberately removed by an earlier siege may be
            // introduced again here exactly once.
            center.getMemoryWithoutUpdate().unset(stationCreatedKey(section));
            center.getMemoryWithoutUpdate().unset(
                    interactionCreatedKey(section));
            center.getMemoryWithoutUpdate().unset(
                    REBUILD_PROGRESS_PREFIX + section.id);
            ensureSectionStations(center, true, section);
            station = getStation(center, section);
            if (isBastionReady(center, section)) repairStationFully(station);
        }
    }

    /** Returns the persistent station chosen at the central FOB controller. */
    public static CampaignFleetAPI prepareForCombat(
            SectorEntityToken center, Section selected) {
        if (!OdysseyExpanseSystem.isFobIthaca(center)
                || selected == null) return null;
        ensureSectionStations(center);
        clearCombatMarkers();
        Global.getSector().getMemoryWithoutUpdate().set(
                ACTIVE_SECTION_KEY, selected.id);

        CampaignFleetAPI result = null;
        for (Section section : Section.values()) {
            CampaignFleetAPI fleet = getStation(center, section);
            if (fleet == null || isDisabled(center, section)) continue;
            revealCombatSprite(fleet);
            if (section == selected) result = fleet;
        }
        return result;
    }

    /** Commits disabled cores and restores surviving fleet tokens in place. */
    public static void finishCombat(
            SectorEntityToken center,
            CampaignFleetAPI selectedFleet,
            Section selected) {
        if (!OdysseyExpanseSystem.isFobIthaca(center)) return;
        for (Section section : Section.values()) {
            if (consumeCoreDisabledMarker(section)) {
                center.getMemoryWithoutUpdate().set(
                        section.disabledKey, true);
            }
        }
        Global.getSector().getMemoryWithoutUpdate().unset(ACTIVE_SECTION_KEY);

        for (Section section : Section.values()) {
            CampaignFleetAPI fleet = getStation(center, section);
            if (isDisabled(center, section)) {
                if (fleet != null && fleet.getBattle() == null) {
                    center.getContainingLocation().removeEntity(fleet);
                }
                removeSectionInteractionTarget(center, section);
                continue;
            }
            if (fleet == null || fleet.getBattle() != null
                    || !isValidStation(fleet, section, false)) {
                continue;
            }
            // Damage now repairs gradually in campaign, not on dialog exit.
            configureCampaignStation(fleet, center, section, false);
            ensureSectionInteractionTarget(center, section);
        }
    }

    /** Called by the combat plugin when any bastion core is destroyed. */
    static void reportCoreDisabled(Section section) {
        if (Global.getSector() == null || section == null) return;
        Global.getSector().getMemoryWithoutUpdate().set(
                coreDisabledKey(section), true);
    }

    static Section getActiveSection() {
        if (Global.getSector() == null) return null;
        return Section.fromId(Global.getSector().getMemoryWithoutUpdate()
                .getString(ACTIVE_SECTION_KEY));
    }

    static Section getSectionForMember(FleetMemberAPI member) {
        if (member == null || member.getId() == null
                || !member.getId().startsWith(MEMBER_ID_PREFIX)) {
            return null;
        }
        String suffix = member.getId().substring(MEMBER_ID_PREFIX.length());
        int separator = suffix.indexOf('_');
        if (separator <= 0) return null;
        return Section.fromId(suffix.substring(0, separator));
    }

    /** Returns the persistent campaign station for one visible ring bastion. */
    public static CampaignFleetAPI getStation(
            SectorEntityToken center, Section section) {
        if (center == null || center.getContainingLocation() == null) {
            return null;
        }
        if (!OdysseyExpanseSystem.isFobIthaca(center)) return null;
        SectorEntityToken entity = center.getContainingLocation()
                .getEntityById(stationId(section));
        if (!(entity instanceof CampaignFleetAPI)) return null;
        CampaignFleetAPI fleet = (CampaignFleetAPI) entity;
        return isPlayerControlledFleet(fleet)
                        || !isSectionStation(fleet)
                        || getSectionForStation(fleet) != section
                ? null : fleet;
    }

    /** Resolves either a legacy direct-fleet click or its large proxy token. */
    public static CampaignFleetAPI resolveStationForInteraction(
            SectorEntityToken target) {
        if (isSectionStation(target)) {
            return (CampaignFleetAPI) target;
        }
        Section section = getSectionForInteractionTarget(target);
        if (section == null || target.getContainingLocation() == null) {
            return null;
        }
        SectorEntityToken center = target.getContainingLocation()
                .getEntityById(FOB_ENTITY_ID);
        if (!OdysseyExpanseSystem.isFobIthaca(center)) return null;
        return getStation(center, section);
    }

    /** True only for one of the invisible, Wall-sized bastion click targets. */
    public static boolean isSectionInteractionTarget(
            SectorEntityToken target) {
        return getSectionForInteractionTarget(target) != null;
    }

    private static Section getSectionForInteractionTarget(
            SectorEntityToken target) {
        if (target == null
                || !INTERACTION_ENTITY_TYPE.equals(
                        target.getCustomEntityType())
                || target.getId() == null
                || !target.getId().startsWith(INTERACTION_ID_PREFIX)
                || !target.getMemoryWithoutUpdate().getBoolean(
                        INTERACTION_MARKER)) {
            return null;
        }
        return Section.fromId(target.getMemoryWithoutUpdate().getString(
                SECTION_KEY));
    }

    private static void ensureSectionInteractionTarget(
            SectorEntityToken center, Section section) {
        ensureSectionInteractionTarget(center, section, false);
    }

    private static void ensureSectionInteractionTarget(
            SectorEntityToken center, Section section, boolean restoreDestroyed) {
        LocationAPI location = center.getContainingLocation();
        String interactionId = interactionId(section);
        SectorEntityToken existing = location.getEntityById(interactionId);
        CustomCampaignEntityAPI target =
                existing instanceof CustomCampaignEntityAPI
                        ? (CustomCampaignEntityAPI) existing : null;
        if (existing != null && (target == null
                || !INTERACTION_ENTITY_TYPE.equals(
                        target.getCustomEntityType())
                || !existing.getMemoryWithoutUpdate().getBoolean(
                        INTERACTION_MARKER))) {
            logInvalidStationOnce(center, section,
                    "interaction ID contains incompatible serialized state");
            return;
        }
        if (target == null) {
            if (!restoreDestroyed && (center.getMemoryWithoutUpdate().getBoolean(
                        interactionCreatedKey(section))
                    || isEstablishedAshenVerge(location))) {
                center.getMemoryWithoutUpdate().set(
                        interactionCreatedKey(section), true);
                return;
            }
            target = location.addCustomEntity(
                    interactionId,
                    "FOB Ithaca - " + section.displayName,
                    INTERACTION_ENTITY_TYPE,
                    COMBAT_GUARD_FACTION_ID);
            center.getMemoryWithoutUpdate().set(
                    interactionCreatedKey(section), true);
        } else {
            center.getMemoryWithoutUpdate().set(
                    interactionCreatedKey(section), true);
        }

        target.setName("FOB Ithaca - " + section.displayName);
        target.setFaction(COMBAT_GUARD_FACTION_ID);
        target.setRadius(CAMPAIGN_INTERACTION_RADIUS);
        target.setFixedLocation(
                center.getLocation().x + section.offsetX,
                center.getLocation().y + section.offsetY);
        target.setFacing(0f);
        target.setSensorProfile(null);
        target.setDiscoverable(null);
        target.setExtendedDetectedAtRange(null);
        target.removeTag(Tags.NON_CLICKABLE);
        target.addTag(Tags.STATION);
        target.addTag(Tags.HAS_INTERACTION_DIALOG);
        target.getMemoryWithoutUpdate().set(SECTION_KEY, section.id);
        target.getMemoryWithoutUpdate().set(INTERACTION_MARKER, true);
    }

    private static void removeSectionInteractionTarget(
            SectorEntityToken center, Section section) {
        if (center == null || section == null
                || center.getContainingLocation() == null) {
            return;
        }
        SectorEntityToken target = center.getContainingLocation()
                .getEntityById(interactionId(section));
        if (isSectionInteractionTarget(target)) {
            center.getContainingLocation().removeEntity(target);
        } else if (target != null) {
            logInvalidStationOnce(center, section,
                    "interaction ID is owned by incompatible serialized "
                            + "state");
        }
    }

    private static CampaignFleetAPI createStation(
            SectorEntityToken center, Section section, boolean disabled) {
        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(
                COMBAT_GUARD_FACTION_ID,
                "FOB Ithaca - " + section.displayName,
                true);
        fleet.setId(stationId(section));
        fleet.setName("FOB Ithaca - " + section.displayName);
        fleet.setFaction(COMBAT_GUARD_FACTION_ID);
        fleet.setNoFactionInName(true);

        FleetMemberAPI wall = addMember(
                fleet, WALL_VARIANT, section, "wall", 0);
        fleet.getFleetData().setFlagship(wall);

        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
        center.getContainingLocation().addEntity(fleet);
        configureCampaignStation(fleet, center, section, disabled);
        return fleet;
    }

    private static void repairStationFully(CampaignFleetAPI fleet) {
        for (FleetMemberAPI member
                : fleet.getFleetData().getMembersListCopy()) {
            member.getStatus().repairFully();
            member.getStatus().resetAmmoState();
            member.getRepairTracker().setCR(
                    member.getRepairTracker().getMaxCR());
        }
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
    }

    private static void logInvalidStationOnce(
            SectorEntityToken center, Section section, String reason) {
        String key = STATION_INVALID_PREFIX + section.id;
        if (center.getMemoryWithoutUpdate().getBoolean(key)) return;
        center.getMemoryWithoutUpdate().set(key, true);
        Global.getLogger(IthacaSectionEncounter.class).error(
                "FOB Ithaca " + section.displayName + ": " + reason
                        + "; refusing delete/recreate repair");
    }

    private static boolean isEstablishedAshenVerge(LocationAPI location) {
        return location instanceof StarSystemAPI
                && ((StarSystemAPI) location).getMemoryWithoutUpdate()
                        .getBoolean(OdysseyExpanseSystem.GENERATED_ASHEN_VERGE);
    }

    private static void configureCampaignStation(
            CampaignFleetAPI fleet,
            SectorEntityToken center,
            Section section,
            boolean disabled) {
        if (fleet == null) return;
        fleet.setFaction(COMBAT_GUARD_FACTION_ID, true);
        fleet.setName("FOB Ithaca - " + section.displayName);
        fleet.setNoFactionInName(true);
        fleet.setTransponderOn(false);
        fleet.setStationMode(true);
        fleet.setNoAutoDespawn(true);
        fleet.setAbortDespawn(true);
        fleet.setAI(null);
        fleet.setDoNotAdvanceAI(true);
        fleet.clearAssignments();
        fleet.setVelocity(0f, 0f);
        fleet.setFixedLocation(
                center.getLocation().x + section.combatOffsetX,
                center.getLocation().y + section.combatOffsetY);
        fleet.setFacing(0f);
        fleet.setSensorProfile(0f);
        fleet.setForceNoSensorProfileUpdate(true);
        fleet.setDiscoverable(false);
        // Each physical module is its own interaction/combat target. The
        // central FOB entity retains its separate rules dialogue.
        fleet.setInteractionTarget(fleet);
        fleet.addTag(Tags.STATION);
        // A fixed custom token owns the Wall-sized click radius. Keep this
        // backing station out of mouse selection while retaining it for
        // combat, reinforcements, and campaign AI targeting.
        fleet.addTag(Tags.NON_CLICKABLE);
        fleet.getMemoryWithoutUpdate().set(SECTION_KEY, section.id);
        fleet.getMemoryWithoutUpdate().set(MemFlags.STATION_FLEET, true);
        fleet.getMemoryWithoutUpdate().set(MARKER, true);
        applyFriendlyCampaignFlags(fleet);
        hideCampaignSprite(fleet);
        fleet.forceSync();
    }

    private static boolean isValidStation(
            CampaignFleetAPI fleet, Section section, boolean disabled) {
        if (fleet == null || !fleet.isStationMode()) return false;
        if (!section.id.equals(fleet.getMemoryWithoutUpdate().getString(
                SECTION_KEY))) return false;

        int walls = 0;
        List<FleetMemberAPI> members = fleet.getFleetData()
                .getMembersListCopy();
        for (FleetMemberAPI member : members) {
            if (WALL_HULL_ID.equals(member.getHullId())) walls++;
        }
        return walls == 1 && members.size() == ACTIVE_MEMBER_COUNT;
    }

    static String[] getDroneGuardVariants(Section section) {
        switch (section) {
            case NORTH:
                return new String[] {
                    "chief_navigator_combat_guard_rampart_Standard",
                    "chief_navigator_combat_guard_defender_PD",
                    "chief_navigator_combat_guard_defender_PD",
                    "chief_navigator_combat_guard_sentry_FS",
                    "chief_navigator_combat_guard_warden_Defense"
                };
            case EAST:
                return new String[] {
                    "chief_navigator_combat_guard_rampart_Standard",
                    "chief_navigator_combat_guard_picket_Assault",
                    "chief_navigator_combat_guard_picket_Assault",
                    "chief_navigator_combat_guard_defender_PD",
                    "chief_navigator_combat_guard_warden_Defense"
                };
            case SOUTH:
                return new String[] {
                    "chief_navigator_combat_guard_rampart_Standard",
                    "chief_navigator_combat_guard_defender_PD",
                    "chief_navigator_combat_guard_sentry_FS",
                    "chief_navigator_combat_guard_sentry_FS",
                    "chief_navigator_combat_guard_warden_Defense"
                };
            default:
                return new String[0];
        }
    }

    private static FleetMemberAPI addMember(
            CampaignFleetAPI fleet,
            String variantId,
            Section section,
            String role,
            int index) {
        FleetMemberAPI member = Global.getFactory().createFleetMember(
                FleetMemberType.SHIP, variantId);
        member.setId(MEMBER_ID_PREFIX + section.id + "_" + role + "_" + index);
        member.setShipName(section.displayName);
        fleet.getFleetData().addFleetMember(member);
        member.getRepairTracker().setCR(
                member.getRepairTracker().getMaxCR());
        return member;
    }

    private static void applyFriendlyCampaignFlags(CampaignFleetAPI fleet) {
        // Do not use MAKE_NON_HOSTILE here. It is a blanket fleet-level
        // override: hostile Starving Threat fleets can receive an INTERCEPT
        // order and reach the station, but the engine will refuse to start a
        // battle. The guard faction's +1 player relationship supplies the
        // intended player IFF without suppressing hostility from Threat.
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE);
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.NON_HOSTILE_OVERRIDES_MAKE_HOSTILE);
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_MAKE_HOSTILE);
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE);
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE);
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE);
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.FLEET_IGNORES_OTHER_FLEETS);
        fleet.getMemoryWithoutUpdate().unset(
                MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
    }

    /** True only for one of the three rebuilt campaign station fleets. */
    public static boolean isSectionStation(SectorEntityToken entity) {
        if (!(entity instanceof CampaignFleetAPI)) return false;
        CampaignFleetAPI fleet = (CampaignFleetAPI) entity;
        if (isPlayerControlledFleet(fleet)
                || !entity.getMemoryWithoutUpdate().getBoolean(MARKER)) {
            return false;
        }
        String sectionId = entity.getMemoryWithoutUpdate().getString(
                SECTION_KEY);
        return entity.getId() != null
                && entity.getId().startsWith(STATION_ID_PREFIX)
                && Section.fromId(sectionId) != null;
    }

    /**
     * Restores the real combat art for every station before vanilla gathers
     * nearby station reinforcements into the battle.
     */
    public static void revealStationsForInteraction(
            CampaignFleetAPI selected) {
        if (selected == null || isPlayerControlledFleet(selected)
                || selected.getContainingLocation() == null) {
            return;
        }
        clearCombatMarkers();
        Section selectedSection = getSectionForStation(selected);
        if (selectedSection != null) {
            Global.getSector().getMemoryWithoutUpdate().set(
                    ACTIVE_SECTION_KEY, selectedSection.id);
        }
        SectorEntityToken center = selected.getContainingLocation()
                .getEntityById(FOB_ENTITY_ID);
        if (!OdysseyExpanseSystem.isFobIthaca(center)) return;
        for (Section section : Section.values()) {
            CampaignFleetAPI station = getStation(center, section);
            if (station != null) revealCombatSprite(station);
        }
    }

    /** Resolves the stable section identity stored on a campaign station. */
    public static Section getSectionForStation(CampaignFleetAPI station) {
        if (station == null || isPlayerControlledFleet(station)) return null;
        return Section.fromId(station.getMemoryWithoutUpdate().getString(
                SECTION_KEY));
    }

    /** Re-hides surviving fleet sprites behind the baked campaign ring. */
    public static void restoreCampaignPresentation(
            LocationAPI location) {
        if (location == null) return;
        SectorEntityToken center = location.getEntityById(FOB_ENTITY_ID);
        if (!OdysseyExpanseSystem.isFobIthaca(center)) return;
        for (Section section : Section.values()) {
            CampaignFleetAPI station = getStation(center, section);
            if (station == null) continue;
            station.setFixedLocation(
                    center.getLocation().x + section.combatOffsetX,
                    center.getLocation().y + section.combatOffsetY);
            station.setVelocity(0f, 0f);
            hideCampaignSprite(station);
            station.forceSync();
        }
    }

    private static boolean isPlayerControlledFleet(CampaignFleetAPI fleet) {
        return fleet != null && (fleet.isPlayerFleet()
                || Global.getSector() != null
                        && fleet == Global.getSector().getPlayerFleet());
    }

    /**
     * Re-pins the three campaign backing fleets every frame. Station mode and
     * disabled campaign AI prevent intentional travel, but campaign fleet
     * collision can still translate a station when a hostile fleet presses
     * into it. The fightable target must remain on its authored ring module.
     */
    static void anchorCampaignStations(SectorEntityToken center) {
        if (!OdysseyExpanseSystem.isFobIthaca(center)
                || center.getContainingLocation() == null) return;
        for (Section section : Section.values()) {
            CampaignFleetAPI station = getStation(center, section);
            if (station == null) continue;
            float x = center.getLocation().x + section.combatOffsetX;
            float y = center.getLocation().y + section.combatOffsetY;
            float dx = station.getLocation().x - x;
            float dy = station.getLocation().y - y;
            float displacement = (float) Math.sqrt(dx * dx + dy * dy);
            if (displacement >= STATION_DRIFT_LOG_THRESHOLD
                    && !station.getMemoryWithoutUpdate().contains(
                            STATION_DRIFT_TRACE)) {
                Global.getLogger(IthacaSectionEncounter.class).warn(
                        "[ITHACA_STATION_ANCHOR] Re-pinning "
                        + station.getId() + " after campaign displacement="
                        + displacement + ", radiusFromIthaca="
                        + distance(station, center) + ", velocity="
                        + station.getVelocity() + ", inBattle="
                        + (station.getBattle() != null) + ".");
                station.getMemoryWithoutUpdate().set(
                        STATION_DRIFT_TRACE, true, 0.25f);
            }
            station.setStationMode(true);
            station.setAI(null);
            station.setDoNotAdvanceAI(true);
            station.setVelocity(0f, 0f);
            station.setLocation(x, y);
            station.setFacing(0f);
        }
    }

    private static float distance(
            SectorEntityToken one, SectorEntityToken two) {
        float dx = one.getLocation().x - two.getLocation().x;
        float dy = one.getLocation().y - two.getLocation().y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static void hideCampaignSprite(CampaignFleetAPI fleet) {
        FleetMemberAPI flagship = fleet == null ? null : fleet.getFlagship();
        if (flagship == null) return;
        flagship.setSpriteOverride(EMPTY_CAMPAIGN_SPRITE);
        // The composite Ithaca entity draws the module and its paired custom
        // token owns interaction, so the backing fleet should render nothing.
        flagship.setOverrideSpriteSize(
                new Vector2f(HIDDEN_CAMPAIGN_SPRITE_SIZE));
        fleet.updateFleetView();
    }

    static void revealCombatSprite(CampaignFleetAPI fleet) {
        FleetMemberAPI flagship = fleet == null ? null : fleet.getFlagship();
        if (flagship == null) return;
        flagship.setSpriteOverride(COMBAT_WALL_SPRITE);
        flagship.setOverrideSpriteSize(new Vector2f(COMBAT_WALL_SIZE));
        fleet.updateFleetView();
        fleet.forceSync();
    }

    private static void clearCombatMarkers() {
        if (Global.getSector() == null) return;
        for (Section section : Section.values()) {
            Global.getSector().getMemoryWithoutUpdate().unset(
                    coreDisabledKey(section));
        }
    }

    private static boolean consumeCoreDisabledMarker(Section section) {
        if (Global.getSector() == null) return false;
        String key = coreDisabledKey(section);
        boolean disabled = Global.getSector().getMemoryWithoutUpdate()
                .getBoolean(key);
        Global.getSector().getMemoryWithoutUpdate().unset(key);
        return disabled;
    }

    private static String stationId(Section section) {
        return STATION_ID_PREFIX + section.id;
    }

    private static String interactionId(Section section) {
        return INTERACTION_ID_PREFIX + section.id;
    }

    private static String stationCreatedKey(Section section) {
        return STATION_CREATED_PREFIX + section.id;
    }

    private static String interactionCreatedKey(Section section) {
        return INTERACTION_CREATED_PREFIX + section.id;
    }

    private static String coreDisabledKey(Section section) {
        return CORE_DISABLED_IN_COMBAT_PREFIX + section.id;
    }
}
