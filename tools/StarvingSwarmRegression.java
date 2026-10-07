import chiefnavigator.hullmods.StarvingThreatHullmod;
import chiefnavigator.hullmods.StarvingAttackSwarmCleanup;
import com.fs.starfarer.api.*;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.threat.ThreatHullmod;
import java.lang.reflect.*;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;

/** Reproduces neutral wreck ownership and checks stock swarm visual setup. */
public class StarvingSwarmRegression {
    public static void main(String[] args) throws Exception {
        WallResearchRegression.check(
                ThreatHullmod.class.isAssignableFrom(
                        StarvingThreatHullmod.class),
                "Starving Threat Hull must replace the stock implementation");
        MutableStat sensorProfile = new MutableStat(100f);
        sensorProfile.modifyMult(StarvingThreatHullmod.HULLMOD_ID, 0f);
        Method profile = StarvingThreatHullmod.class.getDeclaredMethod(
                "applyRavenousSensorProfile", MutableStat.class, String.class);
        profile.setAccessible(true);
        profile.invoke(null, sensorProfile, StarvingThreatHullmod.HULLMOD_ID);
        WallResearchRegression.check(
                Math.abs(sensorProfile.getModifiedValue() - 300f) < 0.0001f,
                "Replacement must remove stock zero profile and add 200%");
        Map<String, Object> creationTags = new HashMap<>();
        ShipAPI created = WallResearchRegression.mock(ShipAPI.class, (n,a) -> {
            if (n.equals("addTag")) creationTags.put((String) a[0], true);
            return null;
        });
        new StarvingThreatHullmod().applyEffectsAfterShipCreation(
                created, StarvingThreatHullmod.HULLMOD_ID);
        WallResearchRegression.check(
                creationTags.containsKey(ThreatHullmod.SHIP_BEING_RECLAIMED),
                "Stock Threat Hull reclamation was not suppressed");

        Global.setSettings(WallResearchRegression.mock(SettingsAPI.class,
                (n,a) -> n.equals("getColor") ? java.awt.Color.WHITE : null));
        Global.setSoundPlayer(WallResearchRegression.mock(SoundPlayerAPI.class, (n,a) -> null));
        Method release = StarvingThreatHullmod.class.getDeclaredMethod(
                "releaseAttackSwarms", CombatEngineAPI.class, ShipAPI.class, int.class);
        release.setAccessible(true);
        for (int side = 0; side <= 1; side++) {
            final int originalSide = side;
            int[] spawns = {0};
            ShipAPI wreck = WallResearchRegression.mock(ShipAPI.class, (n,a) -> {
                if (n.equals("getOwner")) return 100;
                if (n.equals("getOriginalOwner")) return originalSide;
                if (n.equals("getLocation") || n.equals("getVelocity")) return new Vector2f();
                return null;
            });
            FighterWingAPI wing = WallResearchRegression.mock(FighterWingAPI.class, (n,a) -> {
                if (n.equals("setSourceShip")) WallResearchRegression.check(a[0] == null,
                        "Must not attach to neutral wreck");
                if (n.equals("getWingMembers")) return Collections.emptyList();
                return null;
            });
            ShipAPI leader = WallResearchRegression.mock(ShipAPI.class,
                    (n,a) -> n.equals("getWing") ? wing : null);
            CombatFleetManagerAPI manager = WallResearchRegression.mock(
                    CombatFleetManagerAPI.class, (n,a) -> {
                if (n.equals("removeDeployed")) throw new AssertionError("Active wing was untracked");
                if (n.equals("spawnShipOrWing")) {
                    WallResearchRegression.check(a[0].equals("attack_swarm_wing"), "Wrong wing");
                    spawns[0]++;
                    return leader;
                }
                return null;
            });
            CombatEngineAPI engine = WallResearchRegression.mock(CombatEngineAPI.class, (n,a) -> {
                if (n.equals("getFleetManager")) {
                    WallResearchRegression.check((Integer)a[0] == originalSide,
                            "Neutral wreck selected wrong combat side");
                    return manager;
                }
                return null;
            });
            release.invoke(null, engine, wreck, 5);
            WallResearchRegression.check(spawns[0] == 5, "Capital must release five wings");
        }
        Map<String, Object> configured = new HashMap<>();
        ArmorGridAPI armor = WallResearchRegression.mock(ArmorGridAPI.class, (n,a) -> {
            if (n.equals("clearComponentMap")) configured.put("armor", true);
            return null;
        });
        ShipAPI member = WallResearchRegression.mock(ShipAPI.class, (n,a) -> {
            if (n.startsWith("set")) configured.put(n, a[0]);
            if (n.equals("getArmorGrid")) return armor;
            return null;
        });
        Method initialize = StarvingThreatHullmod.class.getDeclaredMethod(
                "initializeAttackSwarm", ShipAPI.class, int.class);
        initialize.setAccessible(true);
        initialize.invoke(null, member, 1);
        WallResearchRegression.check(configured.get("setOwner").equals(1), "Hostile member side");
        WallResearchRegression.check(configured.get("setDoNotRender").equals(true), "Visible carrier sprite");
        WallResearchRegression.check(configured.get("setExplosionScale").equals(0f), "Carrier explosion");
        WallResearchRegression.check(configured.get("setHulkChanceOverride").equals(0f), "Carrier wreck");
        WallResearchRegression.check(configured.get("armor").equals(true), "Component map not cleared");

        Method hasRealShip = StarvingAttackSwarmCleanup.class.getDeclaredMethod(
                "hasLiveNonFighter", CombatEngineAPI.class, int.class);
        Method isAttackSwarm = StarvingAttackSwarmCleanup.class.getDeclaredMethod(
                "isAttackSwarm", ShipAPI.class);
        Method removeLoneSwarms = StarvingAttackSwarmCleanup.class.getDeclaredMethod(
                "removeAttackSwarms", CombatEngineAPI.class, int.class);
        hasRealShip.setAccessible(true);
        isAttackSwarm.setAccessible(true);
        removeLoneSwarms.setAccessible(true);

        ShipHullSpecAPI swarmSpec = WallResearchRegression.mock(
                ShipHullSpecAPI.class, (n,a) -> {
            if (n.equals("getHullId") || n.equals("getBaseHullId")) {
                return "attack_swarm";
            }
            return null;
        });
        Map<String, Object> swarmData = new HashMap<>();
        FighterWingAPI[] detachedWing = new FighterWingAPI[1];
        ShipAPI detachedSwarm = WallResearchRegression.mock(ShipAPI.class, (n,a) -> {
            if (n.equals("getHullSpec")) return swarmSpec;
            if (n.equals("getWing")) return detachedWing[0];
            if (n.equals("getCustomData")) return swarmData;
            if (n.equals("getOwner")) return 1;
            if (n.equals("isFighter") || n.equals("isAlive")) return true;
            return null;
        });
        detachedWing[0] = WallResearchRegression.mock(FighterWingAPI.class, (n,a) -> {
            if (n.equals("getWingId")) return "attack_swarm_wing";
            if (n.equals("getWingMembers")) return Collections.singletonList(detachedSwarm);
            return null;
        });
        ShipHullSpecAPI realSpec = WallResearchRegression.mock(
                ShipHullSpecAPI.class,
                (n,a) -> n.equals("getHullId") ? "chief_navigator_starving_assault_unit" : null);
        ShipAPI realShip = WallResearchRegression.mock(ShipAPI.class, (n,a) -> {
            if (n.equals("getHullSpec")) return realSpec;
            if (n.equals("getOwner")) return 1;
            if (n.equals("isAlive")) return true;
            return null;
        });
        List<ShipAPI> cleanupShips = new ArrayList<>();
        cleanupShips.add(detachedSwarm);
        Map<String, Object> cleanupData = new HashMap<>();
        int[] removedWings = {0};
        CombatFleetManagerAPI cleanupManager = WallResearchRegression.mock(
                CombatFleetManagerAPI.class, (n,a) -> {
            if (n.equals("removeDeployed")) {
                WallResearchRegression.check(a[0] == detachedWing[0]
                                && Boolean.TRUE.equals(a[1]),
                        "Cleanup must remove the tracked detached wing");
                removedWings[0]++;
            }
            return null;
        });
        CombatEngineAPI cleanupEngine = WallResearchRegression.mock(
                CombatEngineAPI.class, (n,a) -> {
            if (n.equals("getShips")) return cleanupShips;
            if (n.equals("getCustomData")) return cleanupData;
            if (n.equals("isEntityInPlay")) return true;
            if (n.equals("getFleetManager")) return cleanupManager;
            return null;
        });
        WallResearchRegression.check((Boolean) isAttackSwarm.invoke(
                        null, detachedSwarm),
                "Stock Attack Swarm hull must be recognized");
        WallResearchRegression.check(!(Boolean) hasRealShip.invoke(
                        null, cleanupEngine, 1),
                "Detached Attack Swarms must not count as deployed real ships");
        cleanupShips.add(realShip);
        WallResearchRegression.check((Boolean) hasRealShip.invoke(
                        null, cleanupEngine, 1),
                "A live deployed Starving hull must keep its swarms in combat");
        cleanupShips.remove(realShip);
        Global.setCombatEngine(cleanupEngine);
        removeLoneSwarms.invoke(null, cleanupEngine, 1);
        WallResearchRegression.check(removedWings[0] == 1,
                "A lone Attack Swarm wing must be removed to unblock reserves");
        System.out.println("PASS: stock Threat Hull is replaced; zero profile becomes +200%; stock reclamation is suppressed; "
                + "neutral wrecks retain original side 0/1; attack wings stay tracked; "
                + "carrier sprites are hidden; lone wings no longer hold up real reinforcements.");
    }
}
