import chiefnavigator.hullmods.CharybdisDistortion;
import chiefnavigator.systems.BudaiConvulsiveLungeSystem;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.WeaponAPI;
import java.awt.Color;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiFunction;
import org.lwjgl.util.vector.Vector2f;

/** Real enrage controller against mocked native phase/cooldown progression; no GL rendering. */
public final class BudaiLeapCooldownRegression {
    private static final String SYSTEM_ID = "chief_navigator_budai_convulsive_lunge";
    private static final String RADIUS_KEY = "chief_navigator_charybdis_distortion_radius";
    private static final long RANDOM_SEED = 73019L;
    private static Method advance;
    private static Method expand;
    private static Constructor<?> sequenceConstructor;
    private static float budaiLeapCooldown;
    private static float regularMawChargeUp;
    private static float regularMawActive;
    private static float regularMawDown;

    private static final class Flags extends ShipwideAIFlags {
        int writes;
        @Override public void setFlag(AIFlags flag, float duration, Object custom) {
            writes++;
            super.setFlag(flag, duration, custom);
        }
    }

    private static final class Rift {
        float refire;
        float cooldown;
        int refireWrites;
        int cooldownWrites;
        final WeaponAPI api;
        Rift(String id, float delay, float remaining) {
            refire = delay;
            cooldown = remaining;
            api = mock(WeaponAPI.class, (name, args) -> {
                switch (name) {
                    case "getId": return id;
                    case "getRefireDelay": return refire;
                    case "getCooldownRemaining": return cooldown;
                    case "setRefireDelay": refire = (Float) args[0]; refireWrites++; break;
                    case "setRemainingCooldownTo": cooldown = (Float) args[0]; cooldownWrites++; break;
                    default: break;
                }
                return null;
            });
        }
    }

    private static final class Fixture {
        final Map<String, Object> data = new HashMap<>();
        final Flags flags = new Flags();
        final Vector2f position = new Vector2f(321f, -456f);
        final List<Vector2f> destinations = new ArrayList<>();
        final List<Vector2f> origins = new ArrayList<>();
        final List<Float> commandTimes = new ArrayList<>();
        final List<Float> completionTimes = new ArrayList<>();
        final Rift customRift = new Rift("chief_navigator_charybdis_assaying_rift", 6f, 5f);
        final Rift stockRift = new Rift("assaying_rift", 8f, 1f);
        final Rift ordinary = new Rift("ordinary_weapon", 9f, 12f);
        final ShipAPI ship;
        final CombatEngineAPI engine;
        final Object sequence;
        ShipSystemAPI.SystemState state = ShipSystemAPI.SystemState.IDLE;
        float cooldown;
        float phaseRemaining;
        float time;
        float finalCooldownOverride = -1f;
        float perLeapCooldown = -1f;
        boolean ready = true;
        boolean acceptCommands = true;
        int commands;
        int targetWrites;
        int cooldownWrites;
        int tintPlugins;

        Fixture() throws Exception {
            ShipSystemAPI system = mock(ShipSystemAPI.class, (name, args) -> {
                switch (name) {
                    case "getId": return SYSTEM_ID;
                    case "getState": return state;
                    case "getCooldownRemaining": return cooldown;
                    // Deliberately independent of remaining cooldown: both guards must work.
                    case "canBeActivated": return ready;
                    case "setCooldownRemaining":
                        cooldown = (Float) args[0]; cooldownWrites++; break;
                    default: break;
                }
                return null;
            });
            ship = mock(ShipAPI.class, (name, args) -> {
                switch (name) {
                    case "getId": return "budai_cooldown_fixture";
                    case "getSystem": return system;
                    case "getCustomData": return data;
                    case "getLocation": return position;
                    case "getVelocity": return new Vector2f();
                    case "getAIFlags": return flags;
                    case "getAllWeapons":
                        return Arrays.asList(customRift.api, null, stockRift.api, ordinary.api);
                    case "setCustomData":
                        data.put((String) args[0], args[1]);
                        if (BudaiConvulsiveLungeSystem.TARGET_KEY.equals(args[0])) targetWrites++;
                        break;
                    case "removeCustomData": data.remove(args[0]); break;
                    case "getMutableStats":
                        throw new AssertionError("Enrage may not add a system-cooldown stat modifier");
                    case "giveCommand":
                        check(args[0] == ShipCommand.USE_SYSTEM, "Only the existing leap is commanded");
                        check(cooldown <= 0f && ready && !lunging(),
                                "No command may bypass native cooldown/readiness or an active phase");
                        commands++;
                        commandTimes.add(time);
                        destinations.add(new Vector2f(destination()));
                        origins.add(new Vector2f(position));
                        if (acceptCommands) {
                            state = ShipSystemAPI.SystemState.IN;
                            phaseRemaining = regularMawChargeUp;
                        }
                        break;
                    default: break;
                }
                return null;
            });
            engine = mock(CombatEngineAPI.class, (name, args) -> {
                if (name.equals("addLayeredRenderingPlugin")) tintPlugins++;
                return null;
            });
            sequence = sequenceConstructor.newInstance(ship);
            ((Random) field(sequence, "random")).setSeed(RANDOM_SEED);
            data.put(CharybdisDistortion.ENRAGE_SEQUENCE_KEY, sequence);
        }

        boolean lunging() {
            return state == ShipSystemAPI.SystemState.IN
                    || state == ShipSystemAPI.SystemState.ACTIVE
                    || state == ShipSystemAPI.SystemState.OUT;
        }

        Vector2f destination() {
            return (Vector2f) data.get(BudaiConvulsiveLungeSystem.TARGET_KEY);
        }

        void advanceController(float amount) throws Exception {
            expand.invoke(null, engine, ship, amount);
            advance.invoke(null, engine, ship, amount);
            check(cooldownWrites == 0, "The enrage sequence must never write system cooldown");
        }

        // This is a test-only model of the native system clock, not a live-engine simulation.
        void nativeStep(float amount) throws Exception {
            time += amount;
            float left = amount;
            while (left > 0f) {
                if (lunging()) {
                    float consumed = Math.min(left, phaseRemaining);
                    phaseRemaining -= consumed;
                    left -= consumed;
                    if (phaseRemaining > 0f) break;
                    if (state == ShipSystemAPI.SystemState.IN) {
                        state = ShipSystemAPI.SystemState.ACTIVE;
                        phaseRemaining = regularMawActive;
                    } else if (state == ShipSystemAPI.SystemState.ACTIVE) {
                        state = ShipSystemAPI.SystemState.OUT;
                        phaseRemaining = regularMawDown;
                    } else {
                        state = ShipSystemAPI.SystemState.COOLDOWN;
                        cooldown = completionTimes.size() == 3 && finalCooldownOverride >= 0f
                                ? finalCooldownOverride : perLeapCooldown >= 0f
                                        ? perLeapCooldown : budaiLeapCooldown;
                        completionTimes.add(time - left);
                        if (destination() != null) position.set(destination());
                    }
                } else if (cooldown > 0f) {
                    float consumed = Math.min(left, cooldown);
                    cooldown -= consumed;
                    left -= consumed;
                    if (cooldown > 0f) break;
                    state = ShipSystemAPI.SystemState.IDLE;
                } else {
                    break;
                }
            }
            advanceController(amount);
        }

        void noBuffsYet(String stage) {
            check(!data.containsKey(RADIUS_KEY) && tintPlugins == 0
                            && customRift.refireWrites == 0 && stockRift.refireWrites == 0,
                    "Radius, tint and Rift refire must wait for four completed leaps: " + stage);
        }
    }

    public static void main(String[] args) throws Exception {
        SettingsAPI savedSettings = Global.getSettings();
        Global.setSettings(mock(SettingsAPI.class,
                (name, values) -> name.equals("getColor") ? Color.WHITE : null));
        try {
            Class<?> sequenceType = Class.forName(
                    "chiefnavigator.hullmods.CharybdisDistortion$EnrageSequence");
            sequenceConstructor = sequenceType.getDeclaredConstructor(ShipAPI.class);
            sequenceConstructor.setAccessible(true);
            advance = CharybdisDistortion.class.getDeclaredMethod(
                    "advanceEnrageSequence", CombatEngineAPI.class, ShipAPI.class, float.class);
            advance.setAccessible(true);
            expand = CharybdisDistortion.class.getDeclaredMethod(
                    "advanceBubbleExpansion", CombatEngineAPI.class, ShipAPI.class, float.class);
            expand.setAccessible(true);
            verifyDataAndSourceContracts();
            initialCooldownAndReadiness();
            fourNormallyCooledLeaps(false);
            fourNormallyCooledLeaps(true);
            fullCyclesAndExternalCooldownFitSequence();
            activeLungeAndRestAreNotSkipped();
            rejectedCommandRetryRespectsReadiness();
            System.out.println("PASS: Budai retains the full native 5.5s activation cycle plus a 12s cooldown, preserving "
                    + "positive cooldown/readiness guards, four full-phase leaps, rest/retry gates "
                    + "and one-shot final Rift buffs/five-second radius growth without a red tint "
                    + "against a mocked native system clock (no live-game or GL claim).");
        } finally {
            Global.setSettings(savedSettings);
        }
    }

    private static void initialCooldownAndReadiness() throws Exception {
        Fixture f = new Fixture();
        f.state = ShipSystemAPI.SystemState.COOLDOWN;
        f.cooldown = 1.5f;
        for (int i = 0; i < 5; i++) f.advanceController(0.1f);
        check(f.commands == 0 && f.targetWrites == 0 && f.flags.writes == 0,
                "An existing initial cooldown must block commands and both target channels");
        near(f.cooldown, 1.5f, "Advancing the controller cannot shorten initial cooldown");
        f.cooldown = 0f;
        f.ready = false;
        for (int i = 0; i < 5; i++) f.advanceController(0.1f);
        check(f.commands == 0 && f.targetWrites == 0 && f.flags.writes == 0,
                "Zero cooldown is insufficient when native canBeActivated is false");
        f.noBuffsYet("initial blockers");
        f.ready = true;
        f.advanceController(0.01f);
        check(f.commands == 1 && f.targetWrites == 1 && f.flags.writes == 1,
                "The first random leap is issued only once both readiness guards pass");
    }

    private static void fourNormallyCooledLeaps(boolean modifiedFinalCooldown) throws Exception {
        Fixture f = new Fixture();
        if (modifiedFinalCooldown) f.finalCooldownOverride = 2.75f;
        f.state = ShipSystemAPI.SystemState.COOLDOWN;
        // Respect a pre-existing remaining cooldown rather than replacing it with the new base.
        f.cooldown = 1.25f;
        for (int frame = 0; frame < 4000
                && f.data.containsKey(CharybdisDistortion.ENRAGE_SEQUENCE_KEY); frame++) {
            f.nativeStep(0.025f);
            int completed = (Integer) field(f.sequence, "completedLunges");
            if (completed < 4) f.noBuffsYet("completed=" + completed);
            check(f.commands <= 4, "No extra leap during repeated IN/ACTIVE/OUT frames");
        }
        check(f.commands == 4 && (Integer) field(f.sequence, "completedLunges") == 4
                        && f.completionTimes.size() == 4,
                "All four random leaps must actually enter and finish native phases");
        check(!f.data.containsKey(CharybdisDistortion.ENRAGE_SEQUENCE_KEY)
                        && f.destination() == null,
                "The fourth completion releases enrage ownership and removes its final target");
        for (int i = 0; i < 4; i++) {
            float distance = Vector2f.sub(f.destinations.get(i), f.origins.get(i), null).length();
            check(distance >= 699.99f && distance <= 2400.01f,
                    "Preserve the existing 700-2400-unit random destination range");
            check(f.completionTimes.get(i) - f.commandTimes.get(i)
                            >= regularMawChargeUp + regularMawActive + regularMawDown - 0.001f,
                    "Every leap must finish all native Maw charge, active and out phases");
            if (i > 0) check(f.commandTimes.get(i) - f.commandTimes.get(i - 1)
                            >= 5.5f + budaiLeapCooldown - 0.005f,
                    "Enrage leaps must include the full activation cycle and twelve-second cooldown");
            if (i > 0) check(f.commandTimes.get(i) - f.completionTimes.get(i - 1)
                            >= budaiLeapCooldown - 0.005f,
                    "Subsequent threshold leaps retain Budai's full twelve-second cooldown");
        }
        check(f.commandTimes.get(0) >= 1.249f,
                "The first command must wait out an injected positive initial cooldown");
        Random expected = new Random(RANDOM_SEED);
        for (int i = 0; i < 4; i++) {
            float angle = expected.nextFloat() * 360f;
            float distance = 700f + expected.nextFloat() * 1700f;
            Vector2f displacement = Vector2f.sub(f.destinations.get(i), f.origins.get(i), null);
            near(displacement.x, (float) Math.cos(Math.toRadians(angle)) * distance,
                    "Blocked cooldowns must not consume extra random destinations");
            near(displacement.y, (float) Math.sin(Math.toRadians(angle)) * distance,
                    "Retain independently randomized leap angle and distance");
        }
        if (modifiedFinalCooldown) {
            check(f.cooldown > 2.7f,
                    "Fourth completion must preserve a positive modifier-supplied cooldown");
        } else {
            near(f.cooldown, budaiLeapCooldown,
                    "Fourth completion retains Budai's twelve-second base cooldown");
        }
        float finalCooldown = f.cooldown;
        near(((Number) f.data.get(RADIUS_KEY)).floatValue(), 1000f,
                "The fourth completion starts expansion at the original radius, without a jump");
        near(f.customRift.refire, 3f, "Custom Rift still receives its separate half-refire buff");
        near(f.customRift.cooldown, 3f, "Separate Rift weapon cooldown still clamps to its new refire");
        near(f.stockRift.refire, 4f, "Stock Rift keeps its existing half-refire behavior");
        near(f.stockRift.cooldown, 1f, "A shorter Rift weapon cooldown is not increased");
        check(f.customRift.cooldownWrites == 1 && f.stockRift.cooldownWrites == 0
                        && f.tintPlugins == 0 && f.ordinary.refireWrites == 0
                        && f.ordinary.cooldownWrites == 0,
                "Only authored Rift effects are applied; no red tint renderer is installed");
        f.advanceController(2.5f);
        near(((Number) f.data.get(RADIUS_KEY)).floatValue(), 2500f,
                "Bubble grows to its midpoint after 2.5 seconds");
        f.advanceController(2.49f);
        near(((Number) f.data.get(RADIUS_KEY)).floatValue(), 3994f,
                "Expansion must not reach full radius before five seconds");
        f.advanceController(0.01f);
        near(((Number) f.data.get(RADIUS_KEY)).floatValue(), 4000f,
                "Full radius is reached exactly five seconds after the fourth completion");
        for (int i = 0; i < 30; i++) f.advanceController(0.1f);
        near(f.cooldown, finalCooldown, "Post-finish calls never clear the fourth leap cooldown");
        check(f.commands == 4 && f.customRift.refireWrites == 1
                        && f.stockRift.refireWrites == 1 && f.tintPlugins == 0,
                "Finished sequence cannot issue a fifth leap or compound final buffs");
    }

    private static void activeLungeAndRestAreNotSkipped() throws Exception {
        Fixture f = new Fixture();
        f.state = ShipSystemAPI.SystemState.ACTIVE;
        for (int i = 0; i < 10; i++) f.advanceController(0.02f);
        check(f.commands == 0 && f.targetWrites == 0
                        && (Integer) field(f.sequence, "completedLunges") == 0,
                "An already active AI lunge is not interrupted or counted as a threshold leap");
        f.state = ShipSystemAPI.SystemState.IDLE;
        f.advanceController(0.01f);
        f.advanceController(0.01f); // Observe the command's native IN phase.
        f.state = ShipSystemAPI.SystemState.COOLDOWN;
        f.cooldown = 3.25f; // An arbitrary modifier-supplied value, not the custom base.
        f.advanceController(0.01f);
        check(f.commands == 1 && f.destination() == null,
                "One phase exit records exactly one completion and clears its target");
        near(f.cooldown, 3.25f, "Phase exit never resets a modifier-supplied cooldown");
        // Isolate the existing rest gate by supplying an otherwise-ready system in this fixture.
        f.cooldown = 0f;
        f.advanceController(0.06f);
        f.advanceController(0.061f);
        check(f.commands == 1, "The existing rest interval must not be shortcut on a ready system");
        f.advanceController(0.001f);
        check(f.commands == 2, "Normal activation resumes after the existing rest interval");
        f.noBuffsYet("one completion");
    }

    private static void fullCyclesAndExternalCooldownFitSequence() throws Exception {
        Fixture f = new Fixture();
        f.state = ShipSystemAPI.SystemState.COOLDOWN;
        f.cooldown = 6f;
        f.perLeapCooldown = 18f;
        for (int frame = 0; frame < 4000
                && f.data.containsKey(CharybdisDistortion.ENRAGE_SEQUENCE_KEY); frame++) {
            f.nativeStep(0.025f);
            int completed = (Integer) field(f.sequence, "completedLunges");
            if (completed < 4) f.noBuffsYet("full-cycle external cooldown completed=" + completed);
        }
        check(f.commands == 4 && f.completionTimes.size() == 4
                        && (Integer) field(f.sequence, "completedLunges") == 4,
                "Full Maw cycles plus existing/modified cooldowns must finish all four leaps before timeout");
        float minimumDuration = 6f + 4f * (regularMawChargeUp + regularMawActive + regularMawDown)
                + 3f * f.perLeapCooldown;
        check(f.time >= minimumDuration - 0.005f
                        && f.time < staticFloat("ENRAGE_SEQUENCE_TIMEOUT"),
                "Expanded safety timeout covers the actual slower sequence: " + f.time);
        for (int index = 1; index < f.commandTimes.size(); index++) {
            check(f.commandTimes.get(index) - f.completionTimes.get(index - 1) >= 17.995f,
                    "Every slow leap respects the positive external cooldown");
        }
        near(f.cooldown, 18f, "Fourth full leap leaves its modified cooldown intact");
        check(f.tintPlugins == 0 && f.customRift.refireWrites == 1,
                "Slower completion retains one-time threshold buffs");
    }

    private static void rejectedCommandRetryRespectsReadiness() throws Exception {
        Fixture f = new Fixture();
        f.acceptCommands = false; // Native system has not entered IN after its first command.
        f.advanceController(0.01f);
        Vector2f original = f.destination();
        f.advanceController(0.34f);
        check(f.commands == 1 && f.destination() == original,
                "Do not retry a pending command before the existing 0.35-second retry window");
        f.ready = false;
        f.advanceController(0.02f);
        f.cooldown = 2.6f; // A positive external cooldown must still block overdue retries.
        f.ready = true;
        f.advanceController(0.5f);
        check(f.commands == 1 && f.targetWrites == 1 && f.flags.writes == 1
                        && f.destination() == original,
                "An overdue retry cannot change target or command while readiness/cooldown blocks it");
        near(f.cooldown, 2.6f, "An overdue retry cannot clear a newly imposed native cooldown");
        f.cooldown = 0f;
        f.ready = false;
        f.advanceController(0.1f);
        check(f.commands == 1, "Retry still honors canBeActivated when cooldown reaches zero");
        f.ready = true;
        f.advanceController(0.001f);
        check(f.commands == 2 && f.targetWrites == 2 && f.flags.writes == 2
                        && (Integer) field(f.sequence, "completedLunges") == 0,
                "Only a ready retry can issue another command; rejected commands are never completions");
        f.noBuffsYet("pending retry");
    }

    private static void verifyDataAndSourceContracts() throws Exception {
        List<String> csv = Files.readAllLines(Path.of("data/shipsystems/ship_systems.csv"));
        List<String> header = Arrays.asList(csv.get(0).split(",", -1));
        String[] row = csv.stream().filter(line -> line.split(",", -1)[1].equals(SYSTEM_ID))
                .findFirst().orElseThrow(() -> new AssertionError("Missing Budai system CSV row"))
                .split(",", -1);
        String budaiCooldown = row[header.indexOf("cooldown")];
        Path core = Path.of("../starsector-core");
        if (!Files.isDirectory(core)) core = Path.of("../../starsector-core");
        List<String> vanilla = Files.readAllLines(
                core.resolve("data/shipsystems/ship_systems.csv"));
        List<String> vanillaHeader = Arrays.asList(vanilla.get(0).split(",", -1));
        String[] maw = vanilla.stream()
                .map(line -> line.split(",", -1))
                .filter(values -> values.length > 1 && values[1].equals("convulsive_lunge"))
                .findFirst().orElseThrow(() -> new AssertionError("Missing installed regular Maw lunge"));
        String mawCooldown = maw[vanillaHeader.indexOf("cooldown")];
        for (String column : new String[] {"charge up", "active", "down"}) {
            near(Float.parseFloat(row[header.indexOf(column)]),
                    Float.parseFloat(maw[vanillaHeader.indexOf(column)]),
                    "Budai must use the entire native Maw " + column + " phase");
        }
        regularMawChargeUp = Float.parseFloat(maw[vanillaHeader.indexOf("charge up")]);
        regularMawActive = Float.parseFloat(maw[vanillaHeader.indexOf("active")]);
        regularMawDown = Float.parseFloat(maw[vanillaHeader.indexOf("down")]);
        near(regularMawChargeUp + regularMawActive + regularMawDown, 5.5f,
                "Regular Maw activation cycle is 5.5 seconds, not the former 2.1 seconds");
        budaiLeapCooldown = Float.parseFloat(budaiCooldown);
        near(budaiLeapCooldown, 12f,
                "Budai needs twelve seconds of cooldown after each full leap");
        check(budaiLeapCooldown > (mawCooldown.isBlank() ? 0f : Float.parseFloat(mawCooldown)),
                "Budai's slower cooldown must be scoped to his custom system");
        for (String file : new String[] {
                "src/chiefnavigator/hullmods/CharybdisDistortion.java",
                "src/chiefnavigator/systems/BudaiConvulsiveLungeAI.java",
                "src/chiefnavigator/systems/BudaiConvulsiveLungeSystem.java"}) {
            String source = Files.readString(Path.of(file));
            check(!source.contains("setCooldownRemaining("), "No leap cooldown reset in " + file);
            check(!source.contains("getSystemCooldownBonus(")
                            && !source.contains("getSystemRegenBonus("),
                    "No substitute cooldown reduction stat in " + file);
        }
        near(staticFloat("ENRAGE_LUNGE_REST"), 0.12f, "Keep existing rest timing");
        near(staticFloat("ENRAGE_LUNGE_RETRY"), 0.35f, "Keep existing retry timing");
        near(staticFloat("ENRAGE_SEQUENCE_TIMEOUT"), 90f,
                "Bounded timeout must accommodate full native phase durations and prior use/cooldowns");
    }

    private static float staticFloat(String name) throws Exception {
        Field f = CharybdisDistortion.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.getFloat(null);
    }

    private static Object field(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private static <T> T mock(Class<T> type, BiFunction<String, Object[], Object> call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("equals")) return proxy == args[0];
                    if (name.equals("hashCode")) return System.identityHashCode(proxy);
                    if (name.equals("toString")) return "mock " + type.getSimpleName();
                    Object result = call.apply(name, args == null ? new Object[0] : args);
                    if (result != null) return result;
                    Class<?> returns = method.getReturnType();
                    if (returns == boolean.class) return false;
                    if (returns == float.class) return 0f;
                    if (returns == int.class) return 0;
                    return null;
                }));
    }

    private static void near(float actual, float expected, String message) {
        check(Math.abs(actual - expected) < 0.002f,
                message + "; expected=" + expected + ", actual=" + actual);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
