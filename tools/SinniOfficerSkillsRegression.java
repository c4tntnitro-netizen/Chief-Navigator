package chiefnavigator.quest;

import chiefnavigator.skills.AdeptOfTheStarsSkill;
import chiefnavigator.skills.AdeptOfTheWavesSkill;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.impl.campaign.OfficerLevelupPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.skills.Helmsmanship;
import com.fs.starfarer.api.impl.campaign.skills.SystemsExpertise;
import java.awt.Color;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/** Headless stat stacking, native skill loading, and recruited-officer continuity.
 * Run in a separate JVM with -Xverify:none for the game's obfuscated classes.
 */
public final class SinniOfficerSkillsRegression {
    interface Call { Object invoke(String name, Object[] args); }

    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[] {type}, (proxy, method, args) -> {
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == args[0];
                    Object result = call.invoke(method.getName(), args);
                    if (result != null) return result;
                    Class<?> returns = method.getReturnType();
                    if (returns == boolean.class) return false;
                    if (returns == int.class) return 0;
                    if (returns == float.class) return 0f;
                    if (returns == long.class) return 0L;
                    if (returns == double.class) return 0d;
                    return null;
                }));
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static void near(float expected, float actual, String message) {
        check(Math.abs(expected - actual) < 0.0001f,
                message + ": expected " + expected + ", got " + actual);
    }

    static final class ShipStats {
        final Map<String, MutableStat> mutable = new HashMap<>();
        final Map<String, StatBonus> bonuses = new HashMap<>();
        final MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, (name, args) -> {
            if (name.startsWith("getSystem")) {
                return bonuses.computeIfAbsent(name, key -> new StatBonus());
            }
            return mutable.computeIfAbsent(name, key -> new MutableStat(
                    name.equals("getAllowZeroFluxAtAnyLevel") ? 0f : 100f));
        });
    }

    static void testWaves() {
        ShipStats fixture = new ShipStats();
        MutableShipStatsAPI stats = fixture.stats;
        String id = AdeptOfTheWavesSkill.ID;
        AdeptOfTheWavesSkill.Level1 base = new AdeptOfTheWavesSkill.Level1();
        AdeptOfTheWavesSkill.Elite elite = new AdeptOfTheWavesSkill.Elite();
        base.apply(stats, HullSize.CRUISER, id, 1f);
        base.apply(stats, HullSize.CRUISER, id, 1f);
        near(125f, stats.getAcceleration().getModifiedValue(), "Waves acceleration/idempotence");
        near(125f, stats.getDeceleration().getModifiedValue(), "Waves deceleration");
        near(150f, stats.getTurnAcceleration().getModifiedValue(), "Native maneuvering convention");
        near(125f, stats.getMaxTurnRate().getModifiedValue(), "Waves turn rate");
        near(110f, stats.getMaxSpeed().getModifiedValue(), "Waves base speed");
        near(0f, stats.getAllowZeroFluxAtAnyLevel().getModifiedValue(), "Non-elite zero-flux gate");
        new Helmsmanship.Level1().apply(stats, HullSize.CRUISER, "helmsmanship", 1f);
        new Helmsmanship.Level2().apply(stats, HullSize.CRUISER, "helmsmanship", 1f);
        near(175f, stats.getAcceleration().getModifiedValue(), "Stacks with Helmsmanship");
        near(250f, stats.getTurnAcceleration().getModifiedValue(), "Stacked turn acceleration");
        near(125f, stats.getMaxSpeed().getModifiedValue(), "Stacked percent speed");
        elite.apply(stats, HullSize.CRUISER, id, 2f);
        elite.apply(stats, HullSize.CRUISER, id, 2f);
        near(140f, stats.getMaxSpeed().getModifiedValue(), "Elite adds a separate flat speed bonus");
        near(1f, stats.getAllowZeroFluxAtAnyLevel().getModifiedValue(), "Native elite zero-flux activation");
        new Helmsmanship.Level3().apply(stats, HullSize.CRUISER, "helmsmanship", 2f);
        new Helmsmanship.Level4().apply(stats, HullSize.CRUISER, "helmsmanship", 2f);
        near(150f, stats.getMaxSpeed().getModifiedValue(), "Both elite speed bonuses stack");
        elite.unapply(stats, HullSize.CRUISER, id);
        near(135f, stats.getMaxSpeed().getModifiedValue(), "Removing Waves elite preserves base/vanilla");
        near(1f, stats.getAllowZeroFluxAtAnyLevel().getModifiedValue(), "Preserve vanilla zero-flux effect");
        base.unapply(stats, HullSize.CRUISER, id);
        near(125f, stats.getMaxSpeed().getModifiedValue(), "Full cleanup preserves vanilla speed");
        near(150f, stats.getAcceleration().getModifiedValue(), "Cleanup preserves vanilla maneuvering");
    }

    static void testStars() {
        ShipStats fixture = new ShipStats();
        MutableShipStatsAPI stats = fixture.stats;
        String id = AdeptOfTheStarsSkill.ID;
        AdeptOfTheStarsSkill.Level1 base = new AdeptOfTheStarsSkill.Level1();
        AdeptOfTheStarsSkill.Elite elite = new AdeptOfTheStarsSkill.Elite();
        base.apply(stats, HullSize.CRUISER, id, 1f);
        base.apply(stats, HullSize.CRUISER, id, 1f);
        near(1f, stats.getSystemUsesBonus().computeEffective(0f), "Stars charge/idempotence");
        near(110f, stats.getSystemRegenBonus().computeEffective(100f), "Stars regen");
        near(110f, stats.getSystemRangeBonus().computeEffective(100f), "Stars range");
        near(85f, stats.getSystemCooldownBonus().computeEffective(100f), "Stars cooldown");
        near(100f, stats.getArmorDamageTakenMult().getModifiedValue(), "Non-elite has no damage reduction");
        new SystemsExpertise.Level1().apply(stats, HullSize.CRUISER, "systems_expertise", 1f);
        new SystemsExpertise.Level2().apply(stats, HullSize.CRUISER, "systems_expertise", 1f);
        new SystemsExpertise.Level3().apply(stats, HullSize.CRUISER, "systems_expertise", 1f);
        new SystemsExpertise.Level4().apply(stats, HullSize.CRUISER, "systems_expertise", 1f);
        near(2f, stats.getSystemUsesBonus().computeEffective(0f), "Charges stack");
        near(160f, stats.getSystemRegenBonus().computeEffective(100f), "Regeneration stacks");
        near(160f, stats.getSystemRangeBonus().computeEffective(100f), "Range stacks");
        near(56.95f, stats.getSystemCooldownBonus().computeEffective(100f), "Cooldown multipliers stack");
        elite.apply(stats, HullSize.CRUISER, id, 2f);
        new SystemsExpertise.Level8().apply(stats, HullSize.CRUISER, "systems_expertise", 2f);
        near(76.5f, stats.getArmorDamageTakenMult().getModifiedValue(), "Elite armor protection stacks");
        near(76.5f, stats.getHullDamageTakenMult().getModifiedValue(), "Elite hull protection stacks");
        near(76.5f, stats.getShieldDamageTakenMult().getModifiedValue(), "Elite shield protection stacks");
        base.unapply(stats, HullSize.CRUISER, id);
        near(1f, stats.getSystemUsesBonus().computeEffective(0f), "Clean up flat charges, preserve vanilla");
        near(150f, stats.getSystemRegenBonus().computeEffective(100f), "Regen cleanup");
        near(150f, stats.getSystemRangeBonus().computeEffective(100f), "Range cleanup");
        near(67f, stats.getSystemCooldownBonus().computeEffective(100f), "Cooldown cleanup");
        elite.unapply(stats, HullSize.CRUISER, id);
        near(90f, stats.getArmorDamageTakenMult().getModifiedValue(), "Elite cleanup preserves vanilla");
        near(90f, stats.getHullDamageTakenMult().getModifiedValue(), "Elite hull cleanup");
        near(90f, stats.getShieldDamageTakenMult().getModifiedValue(), "Elite shield cleanup");
    }

    static MemoryAPI memory(Map<String, Object> values) {
        return mock(MemoryAPI.class, (name, args) -> {
            String key = args == null || args.length == 0 ? "" : (String) args[0];
            switch (name) {
                case "set": values.put(key, args[1]); return null;
                case "contains": return values.containsKey(key);
                case "getBoolean": return Boolean.TRUE.equals(values.get(key));
                case "getInt": return ((Number) values.getOrDefault(key, 0)).intValue();
                case "getString": return values.get(key);
                case "get": return values.get(key);
                default: return null;
            }
        });
    }

    static final class Officer {
        final Map<String, Object> saved = new HashMap<>();
        final Map<String, Float> skills = new HashMap<>();
        final MemoryAPI memory = memory(saved);
        int level = 6;
        long xp = 123456L;
        int points = 3;
        final MutableCharacterStatsAPI stats = mock(MutableCharacterStatsAPI.class, (name, args) -> {
            switch (name) {
                case "getSkillLevel": return skills.getOrDefault(args[0], 0f);
                case "setSkillLevel": skills.put((String) args[0], (Float) args[1]); return null;
                case "getLevel": return level;
                case "getXP": return xp;
                case "getPoints": return points;
                case "setLevel": level = (Integer) args[0]; return null;
                case "setXP": xp = (Long) args[0]; return null;
                case "setPoints": points = (Integer) args[0]; return null;
                default: return null;
            }
        });
        final PersonAPI person = mock(PersonAPI.class, (name, args) -> {
            if (name.equals("getId")) return SinniContact.PERSON_ID;
            if (name.equals("getMemoryWithoutUpdate")) return memory;
            if (name.equals("getStats")) return stats;
            return null;
        });

        Officer() {
            saved.put("$chatterChar", "chief_navigator_sinni");
            saved.put("$some_other_mod_marker", "preserve");
        }
    }

    static void testSavedOfficer() {
        Officer officer = new Officer();
        officer.skills.put("helmsmanship", 2f);
        officer.skills.put("systems_expertise", 2f);
        officer.skills.put("gunnery_implants", 1f);
        Map<String, Float> ordinary = new HashMap<>(officer.skills);
        SinniOfficerTraining.ensureInherentSkills(officer.person);
        SinniOfficerTraining.ensureInherentSkills(officer.person);
        near(1f, officer.skills.get(AdeptOfTheWavesSkill.ID), "Existing officer starts Waves non-elite");
        near(1f, officer.skills.get(AdeptOfTheStarsSkill.ID), "Existing officer starts Stars non-elite");
        for (Map.Entry<String, Float> skill : ordinary.entrySet()) {
            near(skill.getValue(), officer.skills.get(skill.getKey()), "Preserve learned skill " + skill.getKey());
        }
        check(officer.level == 6 && officer.xp == 123456L && officer.points == 3,
                "Preserve saved level, XP, and training points");
        check(officer.skills.size() == ordinary.size() + 2, "No repeated skill grants");
        check(new OfficerLevelupPluginImpl().getMaxLevel(officer.person) == 6,
                "Native officer leveling respects the level-6 cap");
        officer.skills.put(AdeptOfTheWavesSkill.ID, 2f);
        officer.skills.put(AdeptOfTheStarsSkill.ID, 2f);
        SinniOfficerTraining.ensureInherentSkills(officer.person);
        near(2f, officer.skills.get(AdeptOfTheWavesSkill.ID), "Keep Waves elite after reload");
        near(2f, officer.skills.get(AdeptOfTheStarsSkill.ID), "Keep Stars elite after reload");
        check("preserve".equals(officer.saved.get("$some_other_mod_marker")), "Keep other memory");
        Officer foreign = new Officer();
        foreign.saved.remove("$chatterChar");
        SinniOfficerTraining.ensureInherentSkills(foreign.person);
        check(foreign.skills.isEmpty() && !foreign.saved.containsKey(MemFlags.OFFICER_MAX_LEVEL),
                "Never edit a foreign person sharing Sinni's ID");
        SinniOfficerTraining.ensureInherentSkills(null);
        Officer fresh = new Officer();
        fresh.level = 1;
        SinniOfficerTraining.ensureInherentSkills(fresh.person);
        check(fresh.level == 1 && fresh.skills.size() == 2, "Grant inherent skills without free levels");
    }

    static void testRosterIdentity() {
        Officer officer = new Officer();
        Map<String, Object> sectorSaved = new HashMap<>();
        sectorSaved.put("$chief_navigator_sinni_created_v1", true);
        MemoryAPI sectorMemory = memory(sectorSaved);
        Map<String, PersonAPI> people = new HashMap<>();
        ImportantPeopleAPI important = mock(ImportantPeopleAPI.class, (name, args) -> {
            if (name.equals("getPerson")) return people.get(args[0]);
            if (name.equals("addPerson")) {
                PersonAPI person = (PersonAPI) args[0];
                people.put(person.getId(), person);
            }
            return null;
        });
        OfficerDataAPI savedOfficer = mock(OfficerDataAPI.class,
                (name, args) -> name.equals("getPerson") ? officer.person : null);
        List<OfficerDataAPI> roster = new ArrayList<>();
        roster.add(savedOfficer);
        FleetDataAPI fleetData = mock(FleetDataAPI.class, (name, args) -> {
            if (name.equals("getOfficersCopy")) return new ArrayList<>(roster);
            if (name.equals("getOfficerData")) return args[0] == officer.person ? savedOfficer : null;
            if (name.equals("addOfficer")) throw new AssertionError("Must not duplicate saved Sinni");
            return null;
        });
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class,
                (name, args) -> name.equals("getFleetData") ? fleetData : null);
        SectorAPI sector = mock(SectorAPI.class, (name, args) -> {
            if (name.equals("getImportantPeople")) return important;
            if (name.equals("getMemoryWithoutUpdate")) return sectorMemory;
            if (name.equals("getPlayerFleet")) return fleet;
            if (name.equals("getEconomy")) return mock(EconomyAPI.class, (key, values) -> null);
            return null;
        });
        SectorAPI prior = Global.getSector();
        try {
            Global.setSector(sector);
            check(SinniContact.getOrCreatePerson() == officer.person,
                    "Adopt the exact saved officer when ImportantPeople index is missing");
            check(people.get(SinniContact.PERSON_ID) == officer.person, "Restore index by identity");
            check(SinniContact.joinPlayerFleet() == officer.person, "Reuse indexed saved officer");
            check(SinniContact.joinPlayerFleet() == officer.person && roster.size() == 1,
                    "Repeated loading/recruitment cannot duplicate officers");
            check(officer.skills.size() == 2 && officer.level == 6 && officer.xp == 123456L,
                    "Real join path installs missing skills without resetting progression");
        } finally {
            Global.setSector(prior);
        }
    }

    static void testDefinitions(Path root) throws Exception {
        for (String id : new String[] {AdeptOfTheWavesSkill.ID, AdeptOfTheStarsSkill.ID}) {
            JSONObject json = new JSONObject(Files.readString(root.resolve(
                    "data/characters/skills/" + id + ".skill")));
            check(id.equals(json.getString("id")), "Stable skill ID");
            check(json.getBoolean("permanent") && json.getBoolean("elite"),
                    "Inherent skills are permanent and support elite upgrades");
            check("PILOTED_SHIP".equals(json.getString("scope")), "Piloted ship only");
            JSONArray groups = json.getJSONArray("effectGroups");
            check(groups.length() == 2, "Separate base and elite effect groups");
            check(groups.getJSONObject(0).getInt("requiredSkillLevel") == 1
                    && groups.getJSONObject(1).getInt("requiredSkillLevel") == 2,
                    "Elite effects require purchased skill level 2");
            // Supply the native script cache without starting Starsector's campaign loader.
            @SuppressWarnings("unchecked")
            Map<String, Class<?>> scriptCache = (Map<String, Class<?>>) Class.forName(
                    "com.fs.starfarer.loading.scripts.ScriptStore").getField("void").get(null);
            for (int group = 0; group < groups.length(); group++) {
                JSONArray effects = groups.getJSONObject(group).getJSONArray("effects");
                for (int index = 0; index < effects.length(); index++) {
                    String script = effects.getJSONObject(index).getString("script");
                    scriptCache.put(script, Class.forName(script));
                }
            }
            // Native parser and effect casts catch definitions that compile but fail at game load.
            SkillSpecAPI spec = (SkillSpecAPI) Class.forName("com.fs.starfarer.loading.SkillSpec")
                    .getConstructor(JSONObject.class).newInstance(json);
            check(spec.isPermanent() && spec.isElite(), "Native skill flags");
            check(spec.getEffectsAPI().size() == 2, "Native effect loading");
            for (SkillSpecAPI.SkillEffectSpecAPI effect : spec.getEffectsAPI()) {
                check(effect.getAsShipEffect() != null, "Effect implements ShipSkillEffect");
            }
        }
        String csv = Files.readString(root.resolve("data/characters/skills/skill_data.csv"));
        for (String id : new String[] {AdeptOfTheWavesSkill.ID, AdeptOfTheStarsSkill.ID}) {
            long rows = csv.lines().filter(line -> line.startsWith(id + ",")
                    && line.contains("player_only, npc_only, no_ai_core")).count();
            check(rows == 1, "Exactly one Sinni-only registration, no random officer/player learning");
        }
        String plugin = Files.readString(root.resolve("src/chiefnavigator/ChiefNavigatorModPlugin.java"));
        check(plugin.contains("SinniContact.joinPlayerFleet();"), "Existing game-load hook upgrades Sinni");
    }

    public static void main(String[] args) throws Exception {
        SettingsAPI prior = Global.getSettings();
        try {
            Global.setSettings(mock(SettingsAPI.class, (name, values) -> {
                if (name.equals("getColor")) return Color.CYAN;
                if (name.equals("getFloat")) return 1f;
                return null;
            }));
            testWaves();
            testStars();
            testSavedOfficer();
            testRosterIdentity();
            testDefinitions(Path.of(args[0]));
        } finally {
            Global.setSettings(prior);
        }
        System.out.println("PASS: Sinni's inherent skills, native definitions, elite gating, "
                + "vanilla stacking/cleanup, level-6 cap, and saved-officer identity/progression.");
    }
}
