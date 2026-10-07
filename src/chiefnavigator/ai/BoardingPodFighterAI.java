package chiefnavigator.ai;

import chiefnavigator.weapons.BoardingPodOnHitEffect;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FighterLaunchBayAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

/**
 * Keeps a boarding craft close to its Phantom until it detects exposed hull,
 * then commits to a physical boarding run.
 */
public final class BoardingPodFighterAI implements ShipAIPlugin {
    private static final int HULL_TRACE_STEPS = 96;
    private static final int HULL_TRACE_REFINEMENTS = 8;
    private static final float DETECTION_RANGE = 2400f;
    private static final float DEFENSE_CLEARANCE = 145f;
    private static final float ORBIT_RATE = 24f;
    private static final float SCAN_INTERVAL = 0.15f;
    private static final float APPROACH_SPEED = 480f;

    private final ShipAPI ship;
    private final CombatEngineAPI engine;
    private final ShipwideAIFlags flags = new ShipwideAIFlags();
    private final ShipAIConfig config = new ShipAIConfig();
    private ShipAPI target;
    private float scanRemaining;
    private float orbitAngle;
    private boolean consumed;

    public BoardingPodFighterAI(ShipAPI ship) {
        this.ship = ship;
        engine = Global.getCombatEngine();
        String id = ship.getId();
        orbitAngle = id == null ? 0f : Math.abs(id.hashCode() % 360);
    }

    @Override
    public void advance(float amount) {
        if (consumed || engine == null || engine.isPaused() || !isUsable(ship)) return;

        ShipAPI carrier = getCarrier();
        if (target != null && !isBoardable(target)) target = null;

        scanRemaining -= amount;
        if (target == null && scanRemaining <= 0f) {
            scanRemaining = SCAN_INTERVAL;
            target = findExposedTarget(carrier);
        }

        if (target != null) {
            makeBoardingRun(amount);
        } else {
            defendCarrier(carrier, amount);
        }
    }

    private void defendCarrier(ShipAPI carrier, float amount) {
        if (carrier == null) {
            ship.getVelocity().scale(Math.max(0f, 1f - amount * 2f));
            return;
        }

        orbitAngle = normalizeAngle(orbitAngle + ORBIT_RATE * amount);
        float radius = carrier.getCollisionRadius() + DEFENSE_CLEARANCE;
        Vector2f offset = Misc.getUnitVectorAtDegreeAngle(carrier.getFacing() + orbitAngle);
        Vector2f destination = new Vector2f(
                carrier.getLocation().x + offset.x * radius,
                carrier.getLocation().y + offset.y * radius);
        moveToward(destination, carrier.getVelocity(), amount, 330f);
    }

    private void makeBoardingRun(float amount) {
        float contactRange = target.getCollisionRadius()
                + ship.getCollisionRadius() + 8f;
        float distance = Misc.getDistance(ship.getLocation(), target.getLocation());
        boolean exposedHere = isHullExposed(target, ship.getLocation());

        if (exposedHere && distance <= contactRange) {
            Vector2f impact = findHullImpact(target);
            BoardingPodOnHitEffect.attachBoardingPod(
                    engine, target, impact, ship.getFacing());
            consumed = true;
            FighterWingAPI wing = ship.getWing();
            if (wing != null) {
                FighterLaunchBayAPI bay = wing.getSource();
                wing.removeMember(ship);
                // Boarding consumes a fighter without going through normal
                // destruction. Register that loss explicitly so the bay's
                // replacement cycle launches a new pod.
                if (bay != null) bay.setNumLost(bay.getNumLost() + 1);
            }
            engine.removeEntity(ship);
            return;
        }

        Vector2f destination;
        if (exposedHere) {
            destination = target.getLocation();
        } else {
            ShieldAPI shield = target.getShield();
            float rear = shield == null
                    ? target.getFacing() + 180f
                    : shield.getFacing() + 180f;
            Vector2f flank = Misc.getUnitVectorAtDegreeAngle(rear);
            float flankRange = target.getCollisionRadius() + 80f;
            destination = new Vector2f(
                    target.getLocation().x + flank.x * flankRange,
                    target.getLocation().y + flank.y * flankRange);
        }
        moveToward(destination, target.getVelocity(), amount, APPROACH_SPEED);
    }

    /**
     * Finds the visible hull edge along the pod's approach vector. A ship's
     * collision radius is only a broad-phase circle; placing an attachment on
     * that circle leaves pods floating far away from long or narrow sprites.
     */
    private Vector2f findHullImpact(ShipAPI boardedShip) {
        Vector2f rayStart = new Vector2f(ship.getLocation());
        Vector2f rayEnd = new Vector2f(boardedShip.getLocation());

        // Trace from the approaching craft toward the target and find the
        // first point that enters the real polygonal hull. This avoids API
        // implementations that answer a ray query using the broad-phase
        // collision circle instead of the visible bounds.
        Vector2f previous = new Vector2f(rayStart);
        for (int step = 1; step <= HULL_TRACE_STEPS; step++) {
            float progress = step / (float) HULL_TRACE_STEPS;
            Vector2f current = lerp(rayStart, rayEnd, progress);
            if (boardedShip.isPointInBounds(current)) {
                Vector2f outside = previous;
                Vector2f inside = current;
                for (int refinement = 0;
                        refinement < HULL_TRACE_REFINEMENTS;
                        refinement++) {
                    Vector2f midpoint = lerp(outside, inside, 0.5f);
                    if (boardedShip.isPointInBounds(midpoint)) {
                        inside = midpoint;
                    } else {
                        outside = midpoint;
                    }
                }
                return inside;
            }
            previous = current;
        }

        Vector2f outward = Vector2f.sub(rayStart, rayEnd, null);
        if (outward.lengthSquared() < 0.01f) {
            outward = Misc.getUnitVectorAtDegreeAngle(boardedShip.getFacing());
        } else {
            outward.normalise();
        }

        // Some decorative/module hulls do not expose usable exact bounds.
        // Keep their fallback attachment just inside the broad-phase circle so
        // the 42px pod sprite still overlaps the target instead of hovering.
        float fallbackRadius = Math.max(
                12f, boardedShip.getCollisionRadius() - ship.getCollisionRadius());
        return new Vector2f(
                rayEnd.x + outward.x * fallbackRadius,
                rayEnd.y + outward.y * fallbackRadius);
    }

    private Vector2f lerp(Vector2f from, Vector2f to, float amount) {
        return new Vector2f(
                from.x + (to.x - from.x) * amount,
                from.y + (to.y - from.y) * amount);
    }

    private ShipAPI findExposedTarget(ShipAPI carrier) {
        Vector2f searchOrigin = carrier == null
                ? ship.getLocation() : carrier.getLocation();
        ShipAPI best = null;
        float bestDistance = Float.MAX_VALUE;
        for (ShipAPI candidate : engine.getShips()) {
            if (!isBoardable(candidate) || candidate.getOwner() == ship.getOwner()) continue;
            if (!isHullExposed(candidate, ship.getLocation())) continue;
            float distance = Misc.getDistance(searchOrigin, candidate.getLocation());
            if (distance > DETECTION_RANGE || distance >= bestDistance) continue;
            best = candidate;
            bestDistance = distance;
        }
        return best;
    }

    private boolean isHullExposed(ShipAPI candidate, Vector2f observer) {
        ShieldAPI shield = candidate.getShield();
        return shield == null || shield.isOff() || !shield.isWithinArc(observer);
    }

    private ShipAPI getCarrier() {
        FighterWingAPI wing = ship.getWing();
        if (wing == null) return null;
        ShipAPI carrier = wing.getSourceShip();
        return isUsable(carrier) ? carrier : null;
    }

    private boolean isBoardable(ShipAPI candidate) {
        return isUsable(candidate)
                && !candidate.isFighter()
                && !candidate.isDrone()
                && !candidate.isPhased()
                && candidate.getOwner() != 100;
    }

    private boolean isUsable(ShipAPI candidate) {
        return candidate != null
                && candidate.isAlive()
                && !candidate.isHulk()
                && engine.isEntityInPlay(candidate);
    }

    private void moveToward(
            Vector2f destination,
            Vector2f inheritedVelocity,
            float amount,
            float speedLimit) {
        Vector2f delta = new Vector2f(
                destination.x - ship.getLocation().x,
                destination.y - ship.getLocation().y);
        float distance = delta.length();
        if (distance > 0.01f) delta.scale(1f / distance);

        float speed = Math.min(speedLimit, Math.max(70f, distance * 2.5f));
        Vector2f desired = new Vector2f(
                inheritedVelocity.x + delta.x * speed,
                inheritedVelocity.y + delta.y * speed);
        float blend = Math.min(1f, amount * 4.5f);
        ship.getVelocity().x += (desired.x - ship.getVelocity().x) * blend;
        ship.getVelocity().y += (desired.y - ship.getVelocity().y) * blend;

        if (ship.getVelocity().lengthSquared() > 1f) {
            float desiredFacing = Misc.getAngleInDegrees(new Vector2f(), ship.getVelocity());
            float turn = shortestRotation(ship.getFacing(), desiredFacing);
            float maxTurn = 240f * amount;
            ship.setFacing(ship.getFacing() + Math.max(-maxTurn, Math.min(maxTurn, turn)));
        }
    }

    private float shortestRotation(float from, float to) {
        float result = normalizeAngle(to - from);
        if (result > 180f) result -= 360f;
        return result;
    }

    private float normalizeAngle(float angle) {
        angle %= 360f;
        return angle < 0f ? angle + 360f : angle;
    }

    @Override
    public void setDoNotFireDelay(float amount) {
    }

    @Override
    public void forceCircumstanceEvaluation() {
        scanRemaining = 0f;
    }

    @Override
    public boolean needsRefit() {
        return false;
    }

    @Override
    public ShipwideAIFlags getAIFlags() {
        return flags;
    }

    @Override
    public void cancelCurrentManeuver() {
        target = null;
    }

    @Override
    public ShipAIConfig getConfig() {
        return config;
    }
}
