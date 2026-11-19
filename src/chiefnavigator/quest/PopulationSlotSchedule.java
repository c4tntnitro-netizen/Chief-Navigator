package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/** Transient slot index. Saved quest flags and cooldowns remain authoritative. */
final class PopulationSlotSchedule {
    private static WeakReference<SectorAPI> sector = new WeakReference<>(null);
    private static final Map<String, Slot> slots = new HashMap<>();

    static void reset() {
        sector = new WeakReference<>(Global.getSector());
        slots.clear();
    }

    static void invalidate(String id) {
        for (Slot slot : slots.values()) if (slot.id.equals(id)) slot.resolved = false;
    }

    static Slot get(StarSystemAPI system, String id) {
        if (sector.get() != Global.getSector()) reset();
        String key = system.getId() + "/" + id;
        Slot slot = slots.get(key);
        if (slot == null || slot.system.get() != system) {
            slot = new Slot(system, id);
            slots.put(key, slot);
        }
        return slot;
    }

    static final class Slot {
        final String id;
        final WeakReference<StarSystemAPI> system;
        private WeakReference<SectorEntityToken> claim = new WeakReference<>(null);
        private boolean resolved, occupied;
        private long scheduledAt;
        private float delay = Float.POSITIVE_INFINITY;

        Slot(StarSystemAPI system, String id) {
            this.id = id;
            this.system = new WeakReference<>(system);
        }

        /** Live occupants still receive behavior updates; empty slots sleep until due or invalidated. */
        boolean ready() {
            SectorEntityToken token = claim.get();
            if (occupied && (token == null || token.getContainingLocation() != system.get())) {
                resolved = false;
            }
            if (resolved && occupied) return true;
            if (resolved && (Float.isInfinite(delay) || Global.getSector().getClock() == null
                    || Global.getSector().getClock().getElapsedDaysSince(scheduledAt) < delay)) return false;
            StarSystemAPI location = system.get();
            token = location == null ? null : location.getEntityById(id);
            claim = new WeakReference<>(token);
            occupied = token != null;
            resolved = true;
            delay = Float.POSITIVE_INFINITY;
            return true;
        }

        SectorEntityToken claim() { return claim.get(); }

        void defer(float days) {
            claim.clear();
            occupied = false;
            resolved = true;
            scheduledAt = Global.getSector().getClock().getTimestamp();
            // A bad factory/spec cannot cause a retry storm. Normal saved cooldowns are positive.
            delay = days > 0f && Float.isFinite(days) ? days : 1f;
        }

        void changed() { resolved = false; }
    }
}
