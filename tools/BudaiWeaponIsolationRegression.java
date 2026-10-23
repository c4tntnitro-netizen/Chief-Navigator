import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.loading.specs.BaseWeaponSpec;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;

/**
 * Installed native candidate-list, refit-fit and explicit variant contracts.
 * Run with -Xverify:none for the installed engine's obfuscated field names.
 */
public final class BudaiWeaponIsolationRegression {
    private static final String RIFT = "chief_navigator_charybdis_assaying_rift";
    private static final String JET = "chief_navigator_charybdis_pseudoparticle_jet";
    private static final String REWARD = "chief_navigator_mountable_heavy_adjudicator";

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static String read(String relative) throws Exception {
        return Files.readString(Path.of(relative), StandardCharsets.UTF_8);
    }

    private static Map<String, JSONObject> nativeWeaponRows() throws Exception {
        JSONArray rows = (JSONArray) Class.forName("com.fs.starfarer.loading.G")
                .getMethod("o00000", String.class).invoke(null, read("data/weapons/weapon_data.csv"));
        Map<String, JSONObject> result = new LinkedHashMap<>();
        for (int index = 0; index < rows.length(); index++) {
            JSONObject row = rows.getJSONObject(index);
            String id = row.optString("id");
            if (!id.isEmpty()) check(result.put(id, row) == null, "Duplicate native weapon row: " + id);
        }
        return result;
    }

    private static BaseWeaponSpec spec(String id, JSONObject row) throws Exception {
        check(row != null, "Missing native weapon row: " + id);
        JSONObject definition = new JSONObject(read("data/weapons/" + id + ".wpn"));
        BaseWeaponSpec result = new BaseWeaponSpec(id);
        result.setType(WeaponType.valueOf(definition.getString("type")));
        result.setMountType(WeaponType.valueOf(definition.optString("mountTypeOverride",
                definition.getString("type"))));
        result.setSize(WeaponSize.valueOf(definition.getString("size")));
        result.setWeaponName(row.getString("name"));
        for (String hint : row.optString("hints").split(",")) {
            if (!hint.trim().isEmpty()) result.getAIHints().add(AIHints.valueOf(hint.trim()));
        }
        for (String tag : row.optString("tags").split(",")) {
            if (!tag.trim().isEmpty()) result.addTag(tag.trim());
        }
        return result;
    }

    private static Set<String> ids(List<WeaponSpecAPI> specs) {
        Set<String> result = new LinkedHashSet<>();
        for (WeaponSpecAPI spec : specs) result.add(spec.getWeaponId());
        return result;
    }

    private static void metadataAndBehavior(Map<String, JSONObject> rows,
            BaseWeaponSpec rift, BaseWeaponSpec jet, BaseWeaponSpec reward) throws Exception {
        for (BaseWeaponSpec boss : new BaseWeaponSpec[] {rift, jet}) {
            check(boss.getAIHints().contains(AIHints.SYSTEM),
                    "Budai-only weapon must carry the native built-in selection exclusion: " + boss.getWeaponId());
            Set<String> exclusionTags = new HashSet<>(Arrays.asList("restricted", Tags.NO_BP_DROP,
                    "no_drop", "no_drop_salvage", "no_sell", "no_dealer"));
            check(boss.getTags().equals(exclusionTags),
                    "Boss tags must contain only native acquisition exclusions, with no faction, blueprint, "
                            + "private marker or autofit-selection tags: " + boss.getWeaponId());
            check(boss.getAutofitCategory() == null && boss.getAutofitCategoriesInPriorityOrder().isEmpty(),
                    "Boss-only weapon must not advertise any autofit category: " + boss.getWeaponId());
        }
        check(rift.getType() == WeaponType.MISSILE && rift.getMountType() == WeaponType.SYNERGY
                        && jet.getType() == WeaponType.ENERGY && jet.getMountType() == WeaponType.HYBRID
                        && rift.getSize() == WeaponSize.MEDIUM && jet.getSize() == WeaponSize.MEDIUM,
                "Selection metadata must preserve the authored weapon types, mount overrides and sizes");
        JSONObject riftDefinition = new JSONObject(read("data/weapons/" + RIFT + ".wpn"));
        JSONObject jetDefinition = new JSONObject(read("data/weapons/" + JET + ".wpn"));
        check(riftDefinition.getString("everyFrameEffect").equals(
                        "com.fs.starfarer.api.impl.combat.dweller.AssayingRiftEffect")
                        && jetDefinition.getString("everyFrameEffect").equals(
                                "com.fs.starfarer.api.impl.combat.dweller.PseudoparticleJetEffect")
                        && riftDefinition.getString("projectileSpecId").equals(RIFT + "_proj")
                        && jetDefinition.getString("projectileSpecId").equals(JET + "_shot"),
                "The metadata fix must retain native effects and existing custom projectile identities");
        check(rows.get(RIFT).getString("damage/shot").equals("600")
                        && rows.get(RIFT).getString("range").equals("2000")
                        && rows.get(RIFT).getString("OPs").equals("18")
                        && rows.get(RIFT).getString("customAncillary").contains("50%%")
                        && rows.get(JET).getString("damage/shot").equals("62.5")
                        && rows.get(JET).getString("range").equals("600")
                        && rows.get(JET).getString("OPs").equals("22"),
                "Combat statistics and escaped percentage tooltip must survive the metadata-only isolation");
        check(!reward.getAIHints().contains(AIHints.SYSTEM)
                        && reward.getType() == WeaponType.BALLISTIC && reward.getSize() == WeaponSize.LARGE,
                "The Heavy Adjudicator reward remains an ordinary large ballistic weapon");
    }

    @SuppressWarnings("unchecked")
    private static void actualNativeListsAndFit(BaseWeaponSpec rift, BaseWeaponSpec jet,
            BaseWeaponSpec reward) throws Exception {
        Class<?> registry = Class.forName("com.fs.starfarer.loading.Q");
        Field registryField = registry.getDeclaredField("new");
        registryField.setAccessible(true);
        Map<String, BaseWeaponSpec> registryMap = (Map<String, BaseWeaponSpec>) registryField.get(null);
        Map<String, BaseWeaponSpec> previousRegistry = new LinkedHashMap<>(registryMap);
        Class<?> store = Class.forName("com.fs.starfarer.loading.SpecStore");
        Map<String, Object> storeMap = (Map<String, Object>) store.getMethod("new", Class.class)
                .invoke(null, BaseWeaponSpec.class);
        Map<String, Object> previousStore = new LinkedHashMap<>(storeMap);
        try {
            registryMap.clear(); storeMap.clear();
            Method register = registry.getMethod("super", String.class, BaseWeaponSpec.class);
            Method storeSpec = store.getMethod("o00000", Class.class, String.class, Object.class);
            for (BaseWeaponSpec spec : new BaseWeaponSpec[] {rift, jet, reward}) {
                register.invoke(null, spec.getWeaponId(), spec);
                storeSpec.invoke(null, BaseWeaponSpec.class, spec.getWeaponId(), spec);
            }
            Constructor<?> settingsConstructor = Class.forName("com.fs.starfarer.settings.StarfarerSettings$1")
                    .getDeclaredConstructor();
            settingsConstructor.setAccessible(true);
            SettingsAPI settings = (SettingsAPI) settingsConstructor.newInstance();
            check(ids(settings.getAllWeaponSpecs()).equals(Collections.singleton(REWARD)),
                    "Installed native general candidate list must exclude both boss weapons while retaining the reward");
            check(ids(settings.getActuallyAllWeaponSpecs()).equals(new HashSet<>(Arrays.asList(RIFT, JET, REWARD)))
                            && ids(settings.getSystemWeaponSpecs()).equals(new HashSet<>(Arrays.asList(RIFT, JET))),
                    "Boss weapons stay directly resolvable and classified as built-ins in the actual engine registry");
            rift.getAIHints().remove(AIHints.SYSTEM);
            check(ids(settings.getAllWeaponSpecs()).contains(RIFT),
                    "Negative control must reproduce catalog presence despite restricted/blueprint/drop tags");
            rift.getAIHints().add(AIHints.SYSTEM);

            WeaponSlotAPI largeBallistic = nativeSlot(WeaponType.BALLISTIC, WeaponSize.LARGE);
            WeaponSlotAPI mediumUniversal = nativeSlot(WeaponType.UNIVERSAL, WeaponSize.MEDIUM);
            WeaponSlotAPI largeUniversal = nativeSlot(WeaponType.UNIVERSAL, WeaponSize.LARGE);
            check(largeBallistic.weaponFits(reward) && largeUniversal.weaponFits(reward)
                            && !mediumUniversal.weaponFits(reward),
                    "Actual native refit fitting must accept the reward only in adequately sized compatible mounts");
            rift.getAIHints().remove(AIHints.SYSTEM);
            jet.getAIHints().remove(AIHints.SYSTEM);
            check(mediumUniversal.weaponFits(rift) && mediumUniversal.weaponFits(jet),
                    "Without SYSTEM, both weapons remain compatible with native slots; this alone does not prove bounty eligibility");
            rift.getAIHints().add(AIHints.SYSTEM);
            jet.getAIHints().add(AIHints.SYSTEM);
            check(!mediumUniversal.weaponFits(rift) && !mediumUniversal.weaponFits(jet),
                    "The same native refit path must reject SYSTEM-marked boss clones in otherwise compatible medium universal slots");
            authoredFit(rift, jet);
        } finally {
            rift.getAIHints().add(AIHints.SYSTEM);
            jet.getAIHints().add(AIHints.SYSTEM);
            registryMap.clear(); registryMap.putAll(previousRegistry);
            storeMap.clear(); storeMap.putAll(previousStore);
        }
    }

    private static WeaponSlotAPI nativeSlot(WeaponType type, WeaponSize size) throws Exception {
        Class<?> slot = Class.forName("com.fs.starfarer.loading.specs.nullsuper");
        Constructor<?> constructor = slot.getConstructor(String.class, WeaponType.class, WeaponSize.class,
                Class.forName("com.fs.starfarer.loading.specs.nullsuper$o"), Vector2f.class,
                Class.forName("com.fs.starfarer.loading.specs.W"), float.class, float.class);
        return (WeaponSlotAPI) constructor.newInstance("test", type, size, null, new Vector2f(), null, 0f, 360f);
    }

    private static void authoredFit(BaseWeaponSpec rift, BaseWeaponSpec jet) throws Exception {
        JSONObject authored = new JSONObject(read("data/variants/chief_navigator_charybdis_Ravenous.variant"));
        Class<?> hullClass = Class.forName("com.fs.starfarer.loading.specs.g");
        Object hull = hullClass.getConstructor(String.class).newInstance(authored.getString("hullId"));
        ShipVariantAPI variant = (ShipVariantAPI) Class.forName("com.fs.starfarer.loading.specs.HullVariantSpec")
                .getConstructor(String.class, hullClass).newInstance(authored.getString("variantId"), hull);
        Method add = variant.getClass().getMethod("addWeapon", String.class, BaseWeaponSpec.class, boolean.class);
        int rifts = 0, jets = 0;
        JSONArray groups = authored.getJSONArray("weaponGroups");
        for (int group = 0; group < groups.length(); group++) {
            JSONObject weapons = groups.getJSONObject(group).getJSONObject("weapons");
            for (Iterator<?> slots = weapons.keys(); slots.hasNext();) {
                String slot = (String) slots.next(), id = weapons.getString(slot);
                if (!id.equals(RIFT) && !id.equals(JET)) continue;
                add.invoke(variant, slot, id.equals(RIFT) ? rift : jet, false);
                check(id.equals(variant.getWeaponId(slot))
                                && variant.getWeaponSpec(slot).getWeaponId().equals(id),
                        "Native explicit authored fitting must still resolve the exact boss weapon: " + slot);
                if (id.equals(RIFT)) rifts++; else jets++;
            }
        }
        check(rifts == 6 && jets == 2 && variant.getFittedWeaponSlots().size() == 8,
                "The historical Budai variant retains all six Hungering Rifts and two Pseudoparticle Jets");
    }

    public static void main(String[] args) throws Exception {
        Map<String, JSONObject> rows = nativeWeaponRows();
        BaseWeaponSpec rift = spec(RIFT, rows.get(RIFT));
        BaseWeaponSpec jet = spec(JET, rows.get(JET));
        BaseWeaponSpec reward = spec(REWARD, rows.get(REWARD));
        metadataAndBehavior(rows, rift, jet, reward);
        actualNativeListsAndFit(rift, jet, reward);
        System.out.println("PASS: native weapon CSV parsing and candidate/refit exclusion, only native exclusion tags, "
                + "direct authored six-Rift/two-Jet fit, unchanged boss behavior metadata, and mountable Heavy Adjudicator reward.");
    }
}
