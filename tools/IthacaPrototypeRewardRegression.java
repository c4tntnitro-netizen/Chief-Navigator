package chiefnavigator.quest;

import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.*;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.*;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.rules.A;
import data.campaign.rulecmd.ChiefNavigatorIthacaPrototypeCMD;
import java.awt.Color;
import java.lang.reflect.Proxy;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.*;
import java.util.*;
import static chiefnavigator.quest.MenelausFlowRegression.mock;

/** Actual contact shell, native rule expressions/dispatch and saved reward receipt. */
public final class IthacaPrototypeRewardRegression {
    private static final String TOPIC = "chief_navigator_ithaca_prototype";
    private static final String ACCEPT = ChiefNavigatorIthacaPrototypeCMD.ACCEPT;
    private static final String LATER = "chief_navigator_ithaca_prototype_later";
    private static Map<String, List<String>> rows;
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    static final class Fixture {
        final MenelausFlowRegression.Fixture base;
        final List<FleetMemberAPI> members = new ArrayList<>();
        final List<OfficerDataAPI> officers = new ArrayList<>();
        int factories;
        float cr;
        boolean missingVariant, failFactory;
        FobIthacaContactInteraction plugin;

        Fixture() throws Exception { base = new MenelausFlowRegression.Fixture(); }

        void complete() throws Exception { base.stage(MenelausTrial.LABOR_COMPLETE, true, true); }

        void install() {
            base.install();
            FactoryAPI originalFactory = Global.getFactory();
            SectorAPI originalSector = Global.getSector();
            RulesAPI rules = mock(RulesAPI.class, (name, args) -> {
                if (name.equals("performTokenReplacement")) {
                    @SuppressWarnings("unchecked") Map<String, MemoryAPI> memory = (Map<String, MemoryAPI>) args[3];
                    return Misc.replaceTokensFromMemory((String) args[1], memory);
                }
                if (!name.equals("getAllMatching") && !name.equals("getBestMatching")) return null;
                String trigger = (String) args[1];
                if (trigger.equals("FireAllIntercept")) return null;
                @SuppressWarnings("unchecked") Map<String, MemoryAPI> memory = (Map<String, MemoryAPI>) args[3];
                List<RuleAPI> found = new ArrayList<>();
                for (List<String> row : rows.values()) {
                    if (!row.get(1).equals(trigger)) continue;
                    boolean match = true;
                    for (String condition : row.get(2).split("\\R")) {
                        if (!condition.isBlank() && !new A(condition).isTrueFor(row.get(0), base.dialog, memory)) {
                            match = false; break;
                        }
                    }
                    if (match) found.add(nativeRule(row));
                }
                if (name.equals("getAllMatching")) return found;
                check(found.size() <= 1, "Ambiguous best matching rule for " + trigger);
                return found.isEmpty() ? null : found.get(0);
            });
            FleetDataAPI roster = mock(FleetDataAPI.class, (name, args) -> {
                if (name.equals("getMembersListCopy")) return new ArrayList<>(members);
                if (name.equals("getOfficersCopy")) return new ArrayList<>(officers);
                if (name.equals("addFleetMember")) members.add((FleetMemberAPI) args[0]);
                return null;
            });
            CampaignFleetAPI originalFleet = originalSector.getPlayerFleet();
            CampaignFleetAPI fleet = (CampaignFleetAPI) Proxy.newProxyInstance(CampaignFleetAPI.class.getClassLoader(),
                    new Class<?>[]{CampaignFleetAPI.class}, (proxy, method, args) ->
                            method.getName().equals("getFleetData") ? roster : method.invoke(originalFleet, args));
            Global.setSector((SectorAPI) Proxy.newProxyInstance(SectorAPI.class.getClassLoader(),
                    new Class<?>[]{SectorAPI.class}, (proxy, method, args) -> {
                        if (method.getName().equals("getRules")) return rules;
                        if (method.getName().equals("getPlayerFleet")) return fleet;
                        return method.invoke(originalSector, args);
                    }));
            Global.setFactory((FactoryAPI) Proxy.newProxyInstance(FactoryAPI.class.getClassLoader(),
                    new Class<?>[]{FactoryAPI.class}, (proxy, method, args) -> {
                        if (!method.getName().equals("createFleetMember")) return method.invoke(originalFactory, args);
                        check(args[0] == FleetMemberType.SHIP && ChiefNavigatorIthacaPrototypeCMD.VARIANT.equals(args[1]),
                                "Only the fitted ITH prototype may be granted");
                        factories++;
                        if (failFactory) return null;
                        RepairTrackerAPI repair = mock(RepairTrackerAPI.class, (name, values) -> {
                            if (name.equals("getMaxCR")) return .7f;
                            if (name.equals("setCR")) cr = (Float) values[0];
                            return null;
                        });
                        ShipVariantAPI fit = mock(ShipVariantAPI.class, (name, values) -> {
                            if (name.equals("getHullVariantId")) return ChiefNavigatorIthacaPrototypeCMD.VARIANT;
                            if (name.equals("getFullDesignationWithHullNameForShip")) return "Onslaught (ITH)";
                            return null;
                        });
                        return mock(FleetMemberAPI.class, (name, values) -> name.equals("getRepairTracker") ? repair
                                : name.equals("getVariant") ? fit : null);
                    }));
            Global.setSettings(mock(SettingsAPI.class, (name, args) -> {
                if (name.equals("getColor")) return Color.WHITE;
                if (name.equals("getVariant")) return missingVariant ? null : mock(ShipVariantAPI.class, (n, a) -> null);
                return null;
            }));
        }

        RuleAPI nativeRule(List<String> row) {
            List<Option> options = new ArrayList<>();
            for (String line : row.get(5).split("\\R")) {
                if (line.isBlank()) continue;
                String[] parts = line.split(":", 3);
                Option option = new Option(); option.order = Float.parseFloat(parts[0]);
                option.id = parts[1]; option.text = parts[2]; options.add(option);
            }
            return mock(RuleAPI.class, (name, args) -> {
                if (name.equals("getId")) return row.get(0);
                if (name.equals("getTrigger")) return row.get(1);
                if (name.equals("getOptions")) return options;
                if (name.equals("pickText")) return row.get(4);
                if (name.equals("getText")) return List.of(row.get(4));
                if (name.equals("getScriptCopy")) return List.of();
                if (name.equals("runScript")) {
                    @SuppressWarnings("unchecked") Map<String, MemoryAPI> memory = (Map<String, MemoryAPI>) args[1];
                    for (String line : row.get(3).split("\\R")) {
                        if (!line.isBlank()) new A(line).execute(row.get(0), (InteractionDialogAPI) args[0], memory);
                    }
                }
                return null;
            });
        }

        void open() {
            open(base.aias);
        }

        void open(PersonAPI person) {
            install(); plugin = new FobIthacaContactInteraction(person);
            base.plugin = plugin; plugin.init(base.dialog);
        }

        void choose(String id) {
            check(base.options.containsKey(id), "Missing actual contact option " + id + ": " + base.options);
            plugin.optionSelected(base.options.get(id), id);
        }

        boolean claimed() { return base.global.getBoolean(ChiefNavigatorIthacaPrototypeCMD.CLAIMED); }
    }

    public static void main(String[] args) throws Exception {
        SectorAPI oldSector = Global.getSector(); FactoryAPI oldFactory = Global.getFactory();
        SettingsAPI oldSettings = Global.getSettings();
        try {
            initializeNativeRules();
            Fixture f = new Fixture(); f.open();
            check(!f.base.options.containsKey(TOPIC) && f.factories == 0, "No pre-Labor V prototype offer");
            f.base.global.set(MenelausTrial.GAUTAMA_DEFEATED, true); f.open();
            check(!f.base.options.containsKey(TOPIC), "Victory before Menelaus's debrief does not unlock the gift");
            f.complete(); f.open();
            check(f.base.options.containsKey(TOPIC) && f.factories == 0, "Completed saves gain the dialogue offer only");
            f.choose(TOPIC);
            check(f.base.options.containsKey(ACCEPT) && f.base.options.containsKey(LATER)
                            && f.members.isEmpty() && !f.claimed(), "Opening the offer never grants or consumes it");
            f.choose(LATER); f.open();
            check(f.base.options.containsKey(TOPIC) && !f.claimed(), "Deferring and reopening keep the offer available");
            f.choose(TOPIC); f.choose(ACCEPT);
            check(f.members.size() == 1 && f.factories == 1 && Math.abs(f.cr - .7f) < .0001f && f.claimed(),
                    "Acceptance grants one fully ready prototype and a durable receipt");
            check(!f.base.options.containsKey(TOPIC), "The option disappears immediately after transfer");
            f.open(); check(!f.base.options.containsKey(TOPIC) && f.members.size() == 1, "Reopening cannot duplicate the gift");
            f.members.clear(); f.open();
            check(!f.base.options.containsKey(TOPIC) && f.factories == 1, "Selling, storing or losing the prototype never reissues it");
            for (String failure : List.of("variant", "factory")) {
                f = new Fixture(); f.complete(); f.open(); f.choose(TOPIC);
                f.missingVariant = failure.equals("variant"); f.failFactory = failure.equals("factory");
                f.choose(ACCEPT);
                check(f.members.isEmpty() && !f.claimed() && f.base.options.containsKey(TOPIC),
                        "A failed transfer preserves the offer and does not print success");
                check(f.base.history.stream().noneMatch(text -> text != null && text.contains("She's yours")), "No false reward prose");
                f.missingVariant = false; f.failFactory = false;
                f.choose(TOPIC); f.choose(ACCEPT);
                check(f.members.size() == 1 && f.claimed(), "Deferred transfer retries exactly once");
            }
            System.out.println("PASS: actual Kleon contact shell, native expressions/dispatch, post-debrief gating, "
                    + "defer/reopen, full-CR award, durable no-reissue and missing-content retry paths");
        } finally {
            Global.setSector(oldSector); Global.setFactory(oldFactory); Global.setSettings(oldSettings);
        }
    }

    static void initializeNativeRules() throws Exception {
        Global.setSettings(mock(SettingsAPI.class, (n, a) -> n.equals("getColor") ? Color.WHITE : null));
        MenelausFlowRegression.initializeExpressions();
        // Native command resolution needs the game scripts' parent classpath.
        for (Field field : Class.forName("com.fs.starfarer.loading.scripts.ScriptStore").getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())
                    && field.getType() == org.codehaus.janino.JavaSourceClassLoader.class) {
                field.setAccessible(true);
                field.set(null, new org.codehaus.janino.JavaSourceClassLoader(
                        IthacaPrototypeRewardRegression.class.getClassLoader()));
            }
        }
        rows = DialoguePortraitCoverageRegression.readRules(Files.readString(Path.of("data/campaign/rules.csv")));
    }
}
