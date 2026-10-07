import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.threat.ThreatShipConstructionScript;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;

/** Runs Gautama's real native-derived construction, including armor and completion. */
public final class GautamaDurabilityRegression {
    private static final String ARMOR_KEY = "ThreatShipConstructionScript";

    public static void main(String[] args) throws Exception {
        checkSharedHull();
        CombatEngineAPI previousEngine = Global.getCombatEngine();
        SettingsAPI previousSettings = Global.getSettings();
        SoundPlayerAPI previousSound = Global.getSoundPlayer();
        try {
            Global.setSettings(WallResearchRegression.mock(SettingsAPI.class, (name, values) -> null));
            Global.setSoundPlayer(WallResearchRegression.mock(SoundPlayerAPI.class, (name, values) -> null));

            Fixture linear = new Fixture();
            ThreatShipConstructionScript construction = linear.create(true, 0f);
            check(linear.aiDisabled == 1 && linear.groupsDisabled == 1,
                    "Keep native construction AI and weapon shutdown");
            check(linear.tags.contains(ThreatShipConstructionScript.SHIP_UNDER_CONSTRUCTION),
                    "Keep native construction state");
            construction.advance(0f, Collections.emptyList());
            near(linear.armor.getMult(), 0f, "Armor starts at zero");
            for (int quarter = 1; quarter <= 4; quarter++) {
                construction.advance(8.75f, Collections.emptyList());
                near(linear.armor.getMult(), 1.2f * quarter / 4f,
                        "Linear armor at construction quarter " + quarter);
                if (quarter == 2) {
                    linear.paused = true;
                    construction.advance(100f, Collections.emptyList());
                    near(linear.armor.getMult(), 0.6f, "Pause must not advance or rewrite construction");
                    linear.paused = false;
                }
            }
            check(linear.removed == 0 && linear.tags.contains(
                            ThreatShipConstructionScript.SHIP_UNDER_CONSTRUCTION),
                    "Native construction retains its exact 35-second completion boundary");
            construction.advance(0.1f, Collections.emptyList());
            check(linear.armor.getMultBonus(ARMOR_KEY) == null,
                    "Native completion must remove the linear multiplier, not leave or reapply it");
            near(linear.armor.getMult(), 1.2f, "Preserve unrelated armor modifiers on completion");
            check(linear.removed == 1 && linear.aiRestored == 1
                            && !linear.tags.contains(ThreatShipConstructionScript.SHIP_UNDER_CONSTRUCTION),
                    "Preserve native plugin retirement, AI handoff and tag cleanup");
            near(linear.hullDamage.getMult(), 1f, "Do not add Gautama invulnerability");
            check(linear.hullWrites == 0, "Construction must not heal or rewrite Gautama's hull");

            Fixture stock = new Fixture();
            stock.create(false, 0f).advance(17.5f, Collections.emptyList());
            near(stock.armor.getMult(), 1.2f * 0.25f,
                    "Ordinary native Threat construction remains quadratic");

            Fixture delayed = new Fixture();
            ThreatShipConstructionScript delayedConstruction = delayed.create(true, 2f);
            delayedConstruction.advance(1f, Collections.emptyList());
            check(delayed.armor.getMultBonus(ARMOR_KEY) == null,
                    "Do not apply an armor modifier before the native delay expires");
            delayedConstruction.advance(9.75f, Collections.emptyList());
            near(delayed.armor.getMult(), 1.2f * 0.25f,
                    "Linear progress excludes the native delay");

            Fixture destroyed = new Fixture();
            ThreatShipConstructionScript destroyedConstruction = destroyed.create(true, 0f);
            destroyed.hulk = true;
            destroyedConstruction.advance(0.875f, Collections.emptyList());
            near(destroyed.armor.getMult(), 1.2f * 0.25f,
                    "Use native accelerated destroyed-hull fade progress, not a second clock");
            System.out.println("PASS: all Gautamas inherit 40,000 hull; reincarnation armor is linear "
                    + "with pause/delay/death timing, 35-second native completion and modifier cleanup intact; "
                    + "ordinary Threat construction stays quadratic and no immunity/healing is added.");
        } finally {
            Global.setCombatEngine(previousEngine);
            Global.setSettings(previousSettings);
            Global.setSoundPlayer(previousSound);
        }
    }

    private static void checkSharedHull() throws Exception {
        String row = Files.readAllLines(Path.of("data/hulls/ship_data.csv")).stream()
                .filter(line -> line.startsWith("Gautama Base,chief_navigator_scylla_base,"))
                .findFirst().orElseThrow();
        check(row.split(",", -1)[6].equals("40000"), "Gautama base hull must have 40,000 hitpoints");
        for (String id : new String[]{"chief_navigator_scylla", "chief_navigator_scylla_final",
                "chief_navigator_scylla_reincarnating"}) {
            JSONObject skin = new JSONObject(Files.readString(Path.of("data/hulls/skins/" + id + ".skin")));
            check(skin.getString("baseHullId").equals("chief_navigator_scylla_base")
                            && !skin.has("hitpoints") && !skin.has("hitpointsMult"),
                    "Every Gautama skin must inherit the shared 40,000 hull: " + id);
        }
        String source = Files.readString(Path.of("src/chiefnavigator/hullmods/GautamaReincarnation.java"));
        check(source.contains("construction = new GautamaConstruction("),
                "The real reincarnation path must instantiate the scoped linear-armor script");
    }

    private static final class Fixture {
        private final StatBonus armor = new StatBonus();
        private final MutableStat hullDamage = new MutableStat(1f);
        private final Set<String> tags = new HashSet<>();
        private final Map<String, Object> data = new HashMap<>();
        private boolean paused, hulk;
        private int aiDisabled, aiRestored, groupsDisabled, removed, hullWrites;
        private final ShipAPI ship, source;
        private final CombatEngineAPI engine;

        private Fixture() {
            armor.modifyMult("unrelated_armor_bonus", 1.2f);
            MutableShipStatsAPI stats = WallResearchRegression.mock(MutableShipStatsAPI.class,
                    (name, values) -> switch (name) {
                        case "getEffectiveArmorBonus" -> armor;
                        case "getHullDamageTakenMult" -> hullDamage;
                        default -> null;
                    });
            ShipEngineControllerAPI engines = WallResearchRegression.mock(ShipEngineControllerAPI.class,
                    (name, values) -> null);
            WeaponGroupAPI group = WallResearchRegression.mock(WeaponGroupAPI.class, (name, values) -> {
                if (name.equals("toggleOff")) groupsDisabled++;
                return null;
            });
            ship = WallResearchRegression.mock(ShipAPI.class, (name, values) -> {
                switch (name) {
                    case "getMutableStats": return stats;
                    case "getEngineController": return engines;
                    case "getLocation", "getVelocity": return new Vector2f();
                    case "getCollisionClass": return CollisionClass.SHIP;
                    case "getCollisionRadius": return 279f;
                    case "getWeaponGroupsCopy": return List.of(group);
                    case "isHulk": return hulk;
                    case "addTag": tags.add((String) values[0]); break;
                    case "removeTag": tags.remove((String) values[0]); break;
                    case "setShipAI": aiDisabled++; break;
                    case "setDefaultAI": aiRestored++; break;
                    case "setHitpoints": hullWrites++; break;
                }
                return null;
            });
            MutableStat sourceDamage = new MutableStat(1f);
            MutableShipStatsAPI sourceStats = WallResearchRegression.mock(MutableShipStatsAPI.class,
                    (name, values) -> name.equals("getHullDamageTakenMult") ? sourceDamage : null);
            source = WallResearchRegression.mock(ShipAPI.class, (name, values) -> switch (name) {
                case "getMutableStats" -> sourceStats;
                case "getOriginalOwner" -> 1;
                case "getLocation", "getVelocity" -> new Vector2f();
                default -> null;
            });
            CombatFleetManagerAPI manager = WallResearchRegression.mock(CombatFleetManagerAPI.class,
                    (name, values) -> name.equals("spawnShipOrWing") ? ship : null);
            engine = WallResearchRegression.mock(CombatEngineAPI.class, (name, values) -> {
                switch (name) {
                    case "isPaused": return paused;
                    case "getCustomData": return data;
                    case "getFleetManager": return manager;
                    case "getShips": return List.of(ship);
                    case "removePlugin": removed++; break;
                }
                return null;
            });
        }

        private ThreatShipConstructionScript create(boolean linear, float delay) throws Exception {
            Global.setCombatEngine(engine);
            if (!linear) return new ThreatShipConstructionScript("test", source, delay, 35f);
            Class<?> type = Class.forName("chiefnavigator.hullmods.GautamaReincarnation$GautamaConstruction");
            Constructor<?> constructor = type.getDeclaredConstructor(
                    String.class, ShipAPI.class, float.class, float.class);
            constructor.setAccessible(true);
            return (ThreatShipConstructionScript) constructor.newInstance("test", source, delay, 35f);
        }
    }

    private static void near(float actual, float expected, String message) {
        check(Math.abs(actual - expected) < 0.0001f, message + ": " + actual + " != " + expected);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
