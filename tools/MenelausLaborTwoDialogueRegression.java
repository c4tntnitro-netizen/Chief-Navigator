package chiefnavigator.quest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Authored Labor-II prose plus its actual native conversation and services. */
public final class MenelausLaborTwoDialogueRegression {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    public static void main(String[] args) throws Exception {
        String rules = read("data/campaign/rules.csv");
        String ink = read("dialogue/menelaus_contact_labor_2.ink");
        for (String passage : List.of(
                "At least four. As many as possible.",
                "Many of those packages should still exist. Recover them.",
                "Because they are my charge. An irreplaceable treasure of the Domain.",
                "The first quiet districts.",
                "We held our ground and waited.",
                "Whatever returns remembers being destroyed.",
                "It buys us time.",
                "Was that the only reason?",
                "I will be here when you return.")) {
            check(rules.contains(passage), "Authored runtime passage is missing: " + passage);
            check(ink.contains(passage), "Authored Ink passage is missing: " + passage);
        }
        check(ink.contains("Lacedaemon") && !ink.contains("Ashen Verge"),
                "The supplied Labor-II scene consistently names Lacedaemon");
        for (String knot : List.of("menelaus_labor_2_briefing_menu",
                "menelaus_contact_labor_2_menu", "menelaus_spartan_cryo_2",
                "menelaus_gautama_many", "menelaus_gautama_transfer",
                "menelaus_gautama_destroy", "menelaus_helen_2_why",
                "menelaus_helen_2_sinni", "menelaus_contact_leave")) {
            check(ink.contains("=== " + knot + " ==="),
                    "The authored branch must remain represented in Ink: " + knot);
        }
        check(!ink.contains("Every recoverable package.")
                        && !ink.contains("That has been ruled out.")
                        && !ink.contains("Because human beings are hard to replace."),
                "The prior scope and character answers must not displace the supplied revision");
        MenelausFlowRegression.main(new String[] {"--labor-two"});
        System.out.println("Menelaus Labor-II authored dialogue regression checks passed.");
    }
}
