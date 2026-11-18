package chiefnavigator.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.threat.ThreatShipConstructionScript;
import com.fs.starfarer.api.util.Misc;
import chiefnavigator.systems.ExodusPlagueAI;

/** Navigation for an unarmed assault unit whose sole attack is Exodus Plague. */
public final class ExodusAssaultAI implements ShipAIPlugin {
    private final ShipAPI ship;
    private final CombatEngineAPI engine;
    private final ShipwideAIFlags flags = new ShipwideAIFlags();
    private final ShipAIConfig config = new ShipAIConfig();
    private final ExodusPlagueAI plague = new ExodusPlagueAI();
    private float doNotFire;

    public ExodusAssaultAI(ShipAPI ship, CombatEngineAPI engine) {
        this.ship = ship; this.engine = engine;
        plague.init(ship, ship.getSystem(), flags, engine);
    }

    @Override
    public void advance(float amount) {
        if (amount <= 0f || engine.isPaused() || !ship.isAlive() || ship.isHulk() || ship.isPhased()
                || ship.hasTag(ThreatShipConstructionScript.SHIP_UNDER_CONSTRUCTION)) return;
        doNotFire = Math.max(0f, doNotFire - amount);
        ShipAPI target = null;
        float closest = Float.MAX_VALUE;
        for (ShipAPI other : engine.getShips()) {
            if (other == ship || !other.isAlive() || other.isHulk() || other.isPhased()
                    || other.getOwner() == ship.getOwner() || other.getOwner() < 0 || other.getOwner() > 1
                    || !engine.isEntityInPlay(other) || !engine.isAwareOf(ship.getOwner(), other)) continue;
            float distance = Misc.getDistance(ship.getLocation(), other.getLocation());
            if (distance < closest) { closest = distance; target = other; }
        }
        if (target == null) { ship.giveCommand(ShipCommand.DECELERATE, null, 0); return; }
        ship.setShipTarget(target);
        float desired = Misc.getAngleInDegrees(ship.getLocation(), target.getLocation());
        float delta = ((desired - ship.getFacing()) % 360f + 540f) % 360f - 180f;
        if (Math.abs(delta) > 3f) ship.giveCommand(delta > 0f ? ShipCommand.TURN_LEFT : ShipCommand.TURN_RIGHT, null, 0);
        if (closest > 2500f) ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
        else if (closest < 1500f) ship.giveCommand(ShipCommand.ACCELERATE_BACKWARDS, null, 0);
        else ship.giveCommand(ShipCommand.DECELERATE, null, 0);
        if (ship.getShield() != null && !ship.getShield().isOn()) ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
        if (doNotFire <= 0f) plague.advance(amount, null, null, target);
    }
    @Override public void setDoNotFireDelay(float amount) { doNotFire = Math.max(doNotFire, amount); }
    @Override public void forceCircumstanceEvaluation() { }
    @Override public boolean needsRefit() { return false; }
    @Override public ShipwideAIFlags getAIFlags() { return flags; }
    @Override public void cancelCurrentManeuver() { }
    @Override public ShipAIConfig getConfig() { return config; }
}
