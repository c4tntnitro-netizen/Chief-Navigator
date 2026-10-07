package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.RiftLanceEffect;
import com.fs.starfarer.api.impl.combat.threat.ThreatShipConstructionScript;
import com.fs.starfarer.api.impl.combat.threat.VoltaicDischargeOnFireEffect;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import org.lwjgl.util.vector.Vector2f;

/** Calls the real vanilla smoke emitter without running its stock ship construction. */
public final class UngaikyoConstructionSmokeRegression {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        CombatEngineAPI previous = Global.getCombatEngine();
        SettingsAPI previousSettings = Global.getSettings();
        Global.setSettings((SettingsAPI) Proxy.newProxyInstance(
                SettingsAPI.class.getClassLoader(), new Class<?>[] {SettingsAPI.class},
                (proxy, method, values) -> {
                    if (method.getReturnType() == float.class) return 0f;
                    if (method.getReturnType() == int.class) return 0;
                    if (method.getReturnType() == boolean.class) return false;
                    return null;
                }));
        Color smokeColor = Misc.setAlpha(RiftLanceEffect.getColorForDarkening(
                VoltaicDischargeOnFireEffect.EMP_FRINGE_COLOR), 50);
        int[] particles = {0};
        HashMap<String, Object> combatData = new HashMap<>();
        CombatEngineAPI engine = (CombatEngineAPI) Proxy.newProxyInstance(
                CombatEngineAPI.class.getClassLoader(), new Class<?>[] {CombatEngineAPI.class},
                (proxy, method, values) -> {
                    if (method.getName().equals("getCustomData")) return combatData;
                    check(method.getName().equals("addNegativeNebulaParticle"),
                            "Visual-only emitter must not spawn ships, change ownership or apply gameplay effects");
                    check(smokeColor.equals(values[values.length - 1]),
                            "Every puff must use the exact vanilla darkened Threat red and alpha");
                    check(Math.abs((float) values[2] - 39.6f) < 0.001f,
                            "Smoke must retain vanilla hull-relative sizing");
                    check((float) values[6] > 0f, "Native smoke particles must have a finite positive lifetime");
                    particles[0]++;
                    return null;
                });
        ShipAPI ship = (ShipAPI) Proxy.newProxyInstance(
                ShipAPI.class.getClassLoader(), new Class<?>[] {ShipAPI.class},
                (proxy, method, values) -> switch (method.getName()) {
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == values[0];
                    case "getLocation", "getVelocity" -> new Vector2f();
                    case "getCollisionRadius" -> 120f;
                    case "getCustomData" -> new HashMap<String, Object>();
                    default -> throw new AssertionError("Smoke must not mutate its ship: " + method.getName());
                });
        try {
            Global.setCombatEngine(engine);
            Class<?> emitter = Class.forName(
                    "chiefnavigator.quest.GautamaFinalRematchBattleCreationPlugin$MirrorConstructionSmoke");
            check(ThreatShipConstructionScript.class.isAssignableFrom(emitter),
                    "Mirror smoke must reuse the real vanilla construction emitter");
            Constructor<?> constructor = emitter.getDeclaredConstructor(ShipAPI.class, ShipAPI.class, float.class);
            Method advance = emitter.getDeclaredMethod("advanceSmoke", float.class);
            constructor.setAccessible(true); advance.setAccessible(true);
            for (float duration : new float[] {2.25f, 4f, 12f}) {
                Object smoke = constructor.newInstance(ship, ship, duration);
                int before = particles[0];
                advance.invoke(smoke, 0f);
                check(particles[0] == before, "Zero-time/paused updates must not emit smoke");
                advance.invoke(smoke, 0.1f);
                check(particles[0] == before + 11,
                        "Opening summons and later reconstructions must emit vanilla's eleven-puff burst");
                for (int frame = 0; frame < 150; frame++) advance.invoke(smoke, 0.1f);
                int after = particles[0];
                advance.invoke(smoke, 1f);
                check(particles[0] == after, "Smoke must stop emitting when construction finishes");
            }
            String controller = Files.readString(Path.of(
                    "src/chiefnavigator/quest/GautamaFinalRematchBattleCreationPlugin.java"));
            check(controller.contains("constructionSmoke.advanceSmoke(amount);")
                            && controller.contains("mirror, swarmCarrier, this.duration"),
                    "Every dynamic mirror build must own and advance the native visual emitter");
            System.out.println("PASS: opening/rebuilt mirrors emit exact native dark-red smoke with bounded timing and no gameplay side effects.");
        } finally {
            Global.setCombatEngine(previous);
            Global.setSettings(previousSettings);
        }
    }
}
