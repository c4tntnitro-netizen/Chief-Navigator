package chiefnavigator.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A visible minnow that Sinni turns from the player's drive wake onto an enemy. */
public final class DriveMiteEntityPlugin extends BaseCustomEntityPlugin {
    // These legacy IDs and class names are retained so existing saves remain compatible.
    public static final String ENTITY_TYPE = "chief_navigator_drive_mite";
    private static final String DEBUFF_ID = "chief_navigator_drive_mites";
    private static final String DEBUFF_MEMORY = "$chief_navigator_drive_mites_active";
    private static final float HOMING_SPEED = 650f;
    private static final float SOURCE_ORBIT_SECONDS = 0.8f;
    private static final float LIFETIME_SECONDS = 12f;
    private static final float DEBUFF_DAYS = 3f;

    public static final class Params {
        public CampaignFleetAPI source;
        public CampaignFleetAPI target;
        public float orbitAngle;

        public Params(CampaignFleetAPI source, CampaignFleetAPI target,
                      float orbitAngle) {
            this.source = source;
            this.target = target;
            this.orbitAngle = orbitAngle;
        }
    }

    private CampaignFleetAPI source;
    private CampaignFleetAPI target;
    private float orbitAngle;
    private float elapsed;
    private boolean feeding;
    private boolean removed;

    @Override
    public void init(SectorEntityToken entity, Object pluginParams) {
        super.init(entity, pluginParams);
        if (pluginParams instanceof Params) {
            Params params = (Params) pluginParams;
            source = params.source;
            target = params.target;
            orbitAngle = params.orbitAngle;
        }
    }

    public static void spawnSwarm(CampaignFleetAPI source, float range, int count) {
        if (source == null || !source.isInHyperspace()
                || source.getContainingLocation() == null) return;
        LocationAPI location = source.getContainingLocation();
        List<CampaignFleetAPI> targets = new ArrayList<CampaignFleetAPI>();
        for (CampaignFleetAPI fleet : location.getFleets()) {
            if (fleet == source || fleet.isDespawning() || fleet.isEmpty()) continue;
            if (!source.isHostileTo(fleet) && !fleet.isHostileTo(source)) continue;
            if (Misc.getDistance(source, fleet) > range) continue;
            targets.add(fleet);
        }
        targets.sort(Comparator.comparingDouble(fleet -> Misc.getDistance(source, fleet)));

        for (int i = 0; i < count; i++) {
            CampaignFleetAPI target = targets.isEmpty()
                    ? null : targets.get(i % targets.size());
            float angle = i * 360f / Math.max(1, count);
            CustomCampaignEntityAPI mite = location.addCustomEntity(
                    Misc.genUID(), null, ENTITY_TYPE, Factions.PLAYER,
                    new Params(source, target, angle));
            Vector2f offset = Misc.getUnitVectorAtDegreeAngle(angle);
            offset.scale(source.getRadius() + 20f);
            mite.setLocation(source.getLocation().x + offset.x,
                    source.getLocation().y + offset.y);
            mite.setFacing(angle);
        }
    }

    @Override
    public void advance(float amount) {
        if (removed) return;
        elapsed += amount;
        if (!isValid(source) || entity.getContainingLocation() != source.getContainingLocation()
                || elapsed >= LIFETIME_SECONDS) {
            remove();
            return;
        }

        if (feeding && isValid(target)
                && target.getContainingLocation() == entity.getContainingLocation()) {
            orbitTarget(amount);
        } else if (elapsed >= SOURCE_ORBIT_SECONDS && isValid(target)
                && target.getContainingLocation() == entity.getContainingLocation()) {
            homeOnTarget(amount);
        } else {
            orbitSource(amount);
        }
    }

    private void homeOnTarget(float amount) {
        Vector2f from = entity.getLocation();
        Vector2f to = target.getLocation();
        Vector2f delta = Vector2f.sub(to, from, new Vector2f());
        float distance = delta.length();
        if (distance <= target.getRadius() + 12f) {
            boolean firstMinnow = !target.getMemoryWithoutUpdate()
                    .getBoolean(DEBUFF_MEMORY);
            applyDebuff(target);
            feeding = true;
            if (firstMinnow) {
                target.addFloatingText("Minnows eating drive!",
                        new Color(100, 210, 255), 1.5f, true);
            }
            return;
        }
        if (distance <= 0f) return;
        delta.normalise();
        float step = Math.min(distance, HOMING_SPEED * amount);
        delta.scale(step);
        entity.setLocation(from.x + delta.x, from.y + delta.y);
        entity.setFacing(Misc.getAngleInDegrees(delta));
    }

    private void orbitTarget(float amount) {
        orbitAngle += 210f * amount;
        float radius = target.getRadius() + 24f
                + 12f * (float) Math.sin(elapsed * 6f + orbitAngle * 0.03f);
        Vector2f offset = Misc.getUnitVectorAtDegreeAngle(orbitAngle);
        offset.scale(radius);
        entity.setLocation(target.getLocation().x + offset.x,
                target.getLocation().y + offset.y);
        entity.setFacing(orbitAngle + 90f);
    }

    private void orbitSource(float amount) {
        orbitAngle += 150f * amount;
        float radius = source.getRadius() + 28f
                + 10f * (float) Math.sin(elapsed * 5f);
        Vector2f offset = Misc.getUnitVectorAtDegreeAngle(orbitAngle);
        offset.scale(radius);
        entity.setLocation(source.getLocation().x + offset.x,
                source.getLocation().y + offset.y);
        entity.setFacing(orbitAngle + 90f);
    }

    private static boolean isValid(CampaignFleetAPI fleet) {
        return fleet != null && !fleet.isDespawning()
                && fleet.getContainingLocation() != null;
    }

    private static void applyDebuff(CampaignFleetAPI target) {
        target.getMemoryWithoutUpdate().set(DEBUFF_MEMORY, true, DEBUFF_DAYS);
        target.getStats().getFleetwideMaxBurnMod().modifyMult(
                DEBUFF_ID, 0.5f, "Minnows");
        target.getStats().getAccelerationMult().modifyMult(
                DEBUFF_ID, 0.5f, "Minnows");
        if (!target.hasScriptOfClass(DriveMiteDebuffScript.class)) {
            target.addScript(new DriveMiteDebuffScript(target));
        }
    }

    private void remove() {
        if (removed) return;
        removed = true;
        LocationAPI location = entity.getContainingLocation();
        if (location != null) location.removeEntity(entity);
    }

    private static final class DriveMiteDebuffScript implements EveryFrameScript {
        private final CampaignFleetAPI target;
        private boolean done;

        private DriveMiteDebuffScript(CampaignFleetAPI target) {
            this.target = target;
        }

        @Override
        public void advance(float amount) {
            if (!isValid(target)
                    || !target.getMemoryWithoutUpdate().getBoolean(DEBUFF_MEMORY)) {
                if (target != null) {
                    target.getStats().getFleetwideMaxBurnMod().unmodify(DEBUFF_ID);
                    target.getStats().getAccelerationMult().unmodify(DEBUFF_ID);
                }
                done = true;
                return;
            }
            target.getStats().getFleetwideMaxBurnMod().modifyMult(
                    DEBUFF_ID, 0.5f, "Minnows");
            target.getStats().getAccelerationMult().modifyMult(
                    DEBUFF_ID, 0.5f, "Minnows");
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public boolean runWhilePaused() {
            return false;
        }
    }
}
