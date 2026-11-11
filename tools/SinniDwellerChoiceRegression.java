import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.*;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.campaign.rules.oOOO;
import com.fs.starfarer.campaign.rules.ooOO;
import java.awt.Color;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import org.json.*;

/** Actual authored CSV predicates and native rule dispatch; campaign/UI are API fixtures. */
public final class SinniDwellerChoiceRegression {
    interface Call { Object invoke(String name, Object[] args); }
    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p, m, a) -> {
            if (m.getName().equals("hashCode")) return System.identityHashCode(p);
            if (m.getName().equals("equals")) return p == a[0];
            Object result = call.invoke(m.getName(), a);
            if (result != null) return result;
            if (m.getReturnType() == boolean.class) return false;
            if (m.getReturnType() == int.class) return 0;
            if (m.getReturnType() == float.class) return 0f;
            if (m.getReturnType() == long.class) return 0L;
            return null;
        }));
    }
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    static MemoryAPI memory(Map<String, Object> values) {
        return mock(MemoryAPI.class, (n, a) -> {
            if (n.equals("getBoolean")) return Boolean.TRUE.equals(values.get(a[0]));
            if (n.equals("get")) return values.get(a[0]);
            if (n.equals("set")) values.put((String) a[0], a[1]);
            return null;
        });
    }
    public static void main(String[] args) throws Exception {
        Global.setSettings(mock(SettingsAPI.class, (n, a) -> n.equals("getColor") ? Color.WHITE : null));
        JSONObject config = new JSONObject().put("ruleCommandPackages", new JSONArray()
                .put("com.fs.starfarer.api.impl.campaign.rulecmd"));
        for (Field field : Class.forName("com.fs.starfarer.settings.StarfarerSettings").getDeclaredFields()) {
            if (field.getType() == JSONObject.class && Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true); field.set(null, config);
            }
        }
        Class<?> store = Class.forName("com.fs.starfarer.loading.scripts.ScriptStore");
        for (Field field : store.getDeclaredFields()) {
            if (field.getType() == org.codehaus.janino.JavaSourceClassLoader.class) {
                field.setAccessible(true); field.set(null, new org.codehaus.janino.JavaSourceClassLoader(
                        SinniDwellerChoiceRegression.class.getClassLoader()));
            }
        }
        @SuppressWarnings("unchecked") Map<String, Class<?>> scripts = (Map<String, Class<?>>) store.getField("void").get(null);
        scripts.put(FireAll.class.getName(), FireAll.class);
        Map<String, Object> local = new HashMap<>(), player = new HashMap<>(), global = new HashMap<>();
        Map<String, MemoryAPI> scope = new HashMap<>(Map.of(MemKeys.LOCAL, memory(local),
                MemKeys.PLAYER, memory(player), MemKeys.GLOBAL, memory(global)));
        Set<String> options = new LinkedHashSet<>();
        OptionPanelAPI panel = mock(OptionPanelAPI.class, (n, a) -> {
            if (n.equals("clearOptions")) options.clear();
            if (n.equals("addOption")) options.add((String) a[1]);
            return null;
        });
        TextPanelAPI text = mock(TextPanelAPI.class, (n, a) -> null);
        InteractionDialogAPI dialog = mock(InteractionDialogAPI.class, (n, a) -> {
            if (n.equals("getOptionPanel")) return panel;
            if (n.equals("getTextPanel")) return text;
            return null;
        });
        JSONArray rows = (JSONArray) Class.forName("com.fs.starfarer.loading.G").getMethod("o00000", String.class)
                .invoke(null, Files.readString(Path.of(args[0])));
        List<ooOO> authored = new ArrayList<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            if (!row.optString("id").startsWith("chiefNavigatorSinniMonsters")) continue;
            ooOO rule = new ooOO(row.getString("id"), row.getString("trigger"), row.optString("conditions"));
            for (String line : row.optString("conditions").split("\\R")) if (!line.isBlank()) rule.getConditions().add(new oOOO(line));
            for (String line : row.optString("script").split("\\R")) if (!line.isBlank()) rule.getScript().add(new oOOO(line));
            rule.getText().add(row.optString("text"));
            for (String line : row.optString("options").split("\\R")) {
                if (line.isBlank()) continue;
                String[] parts = line.split(":", 3);
                Option option = new Option(); option.order = Float.parseFloat(parts[0]); option.id = parts[1]; option.text = parts[2];
                rule.getOptions().add(option);
            }
            authored.add(rule);
        }
        RulesAPI rules = mock(RulesAPI.class, (n, a) -> {
            if (n.equals("performTokenReplacement")) return a[1];
            if (n.equals("getBestMatching") || n.equals("getAllMatching")) {
                List<RuleAPI> matches = new ArrayList<>();
                for (ooOO rule : authored) if (rule.getTrigger().equals(a[1]) && rule.getConditions().stream()
                        .allMatch(e -> e.isTrueFor(rule.getId(), dialog, scope))) matches.add(rule);
                return n.equals("getAllMatching") ? matches : matches.isEmpty() ? null : matches.get(0);
            }
            return null;
        });
        Global.setSector(mock(SectorAPI.class, (n, a) -> n.equals("getRules") ? rules : null));
        for (Boolean seen : new Boolean[]{null, false, true, false, true}) {
            player.clear(); if (seen != null) player.put("$encounteredDweller", seen);
            local.put("$encounteredDweller", true); global.put("$encounteredDweller", true);
            check(FireBest.fire(null, dialog, scope, "ChiefNavigatorSinniIntro_monsters"), "Native root dispatch succeeds");
            Set<String> expected = Boolean.TRUE.equals(seen)
                    ? Set.of("chief_navigator_sinni_dwellers", "chief_navigator_sinni_threat")
                    : Set.of("chief_navigator_sinni_threat");
            check(options.equals(expected), "Actual native options for player encounter flag " + seen + ": " + options);
        }
        System.out.println("PASS: native Sinni intro dispatch hides Dweller answer before encounter, shows it afterward, ignores local/global lookalikes and always preserves the Threat answer.");
    }
}
