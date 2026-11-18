package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.DelayedActionScript;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

/** Local THREAT or Spartan rescuers converge on a saved distress signal. */
public final class OrionKnotDistressResponse extends DelayedActionScript {
    public enum Outcome { NOTHING, THREAT, SECOND_STRIKE, THIRD_STRIKE, SPARTAN }
    public static final String MARKER = "$chief_navigator_distress_swarm";
    public static final String RESCUE_MARKER = "$chief_navigator_spartan_distress_rescue";
    private static final String[] THREAT_VARIANTS = {
        "chief_navigator_starving_skirmish_Type100",
        "chief_navigator_starving_skirmish_Type101",
        "chief_navigator_starving_assault_Type200",
        "chief_navigator_starving_overseer_Type250"
    };
    private static final String[] SPARTAN_VARIANTS = {
        "enforcer_XIV_Elite", "lasher_Assault", "phaeton_Standard", "buffalo_Standard"
    };
    private final StarSystemAPI system;
    private final Vector2f signal;
    private final Outcome outcome;

    public OrionKnotDistressResponse(float delayDays,
            StarSystemAPI system, Vector2f signal, Outcome outcome) {
        super(delayDays);
        this.system = system;
        this.signal = new Vector2f(signal);
        this.outcome = outcome;
    }

    @Override
    public void doAction() {
        if (outcome == Outcome.NOTHING || !OdysseyExpanseSystem.isInside(system)) return;
        boolean rescue = outcome == Outcome.SPARTAN;
        CampaignFleetAPI swarm = Global.getFactory().createEmptyFleet(
                rescue ? TaskForceSpartanFaction.ID : Factions.THREAT,
                rescue ? "Task Force Spartan Rescue"
                        : outcome == Outcome.SECOND_STRIKE ? "Second Strike"
                        : outcome == Outcome.THIRD_STRIKE ? "Third Strike"
                        : "Starving THREAT Swarm", true);
        if (swarm == null) return;
        swarm.setId((rescue ? "chief_navigator_spartan_rescue_"
                : "chief_navigator_distress_swarm_") + Misc.genUID());
        swarm.setNoFactionInName(true);
        swarm.setTransponderOn(rescue);
        swarm.getMemoryWithoutUpdate().set(rescue ? RESCUE_MARKER : MARKER, true);
        if (outcome == Outcome.SECOND_STRIKE || outcome == Outcome.THIRD_STRIKE) {
            OdysseyPredatorScript.addRareStrikeRoster(swarm,
                    outcome == Outcome.SECOND_STRIKE ? 0.96 : 0.995);
        } else {
            for (String variant : rescue ? SPARTAN_VARIANTS : THREAT_VARIANTS) {
                FleetMemberAPI member = Global.getFactory().createFleetMember(
                        FleetMemberType.SHIP, variant);
                if (member == null) return;
                swarm.getFleetData().addFleetMember(member);
                member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
            }
        }
        swarm.getFleetData().setFlagship(
                swarm.getFleetData().getMembersListCopy().get(0));
        swarm.getFleetData().setSyncNeeded();
        swarm.getFleetData().syncIfNeeded();
        system.addEntity(swarm);
        float angle = (float) (Math.random() * Math.PI * 2);
        swarm.setLocation(signal.x + (float) Math.cos(angle) * 3000f,
                signal.y + (float) Math.sin(angle) * 3000f);
        if (rescue) {
            TaskForceSpartanFaction.configureFriendlyDefender(swarm);
            swarm.getMemoryWithoutUpdate().set("$distressResponse", true, 30f);
            swarm.getCargo().addFuel(500f);
            swarm.getCargo().addSupplies(200f);
        } else {
            OdysseyPredatorScript.configureStarvingThreatIdentity(swarm);
            swarm.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
            swarm.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
            swarm.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
            swarm.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
        }
        // Native distress dialogue returns to this local entity after helping.
        swarm.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_SOURCE_MARKET,
                system.getCenter().getId());
        swarm.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player != null && player.getContainingLocation() == system) {
            swarm.addAssignment(FleetAssignment.INTERCEPT, player, 10f,
                    "converging on a distress signal");
        }
        swarm.addAssignment(FleetAssignment.ORBIT_PASSIVE, system.getCenter(),
                10f, "searching for a distress signal");
        swarm.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,
                system.getCenter(), 1000f);
    }
}
