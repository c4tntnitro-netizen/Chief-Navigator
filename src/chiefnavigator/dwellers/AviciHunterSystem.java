package chiefnavigator.dwellers;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Forced Reality keeps its native duration, cooldown and collapse overload. */
public final class AviciHunterSystem extends BaseShipSystemScript {
    public static final float COLLAPSE_OVERLOAD_SECONDS = 6f;
    private final Set<ShipAPI> active = Collections.newSetFromMap(new WeakHashMap<>());

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (stats == null || !(stats.getEntity() instanceof ShipAPI ship)) return;
        if (effectLevel > 0f) active.add(ship);
        AviciHunterDomain.applySystem(ship, effectLevel);
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        if (stats == null || !(stats.getEntity() instanceof ShipAPI ship)) return;
        AviciHunterDomain.applySystem(ship, 0f);
        if (active.remove(ship) && ship.isAlive() && !ship.isHulk()
                && ship.getFluxTracker() != null) {
            ship.getFluxTracker().forceOverload(COLLAPSE_OVERLOAD_SECONDS);
        }
    }
}
