package chiefnavigator.quest;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.json.JSONObject;

/** Static contracts for the supplied League aftermath art and its scene wiring. */
public final class LeagueSurvivorIllustrationRegression {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final String ILLUSTRATION =
            "chief_navigator_persean_survivors_aftermath";
    private static final String SUPPLIED_SHA256 =
            "64806ce8011cb075759107872120c397048eb74ab0ef4d39a947fd98386c821e";

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(ROOT.resolve(relativePath), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
    }

    private static void verifySuppliedAsset() throws Exception {
        JSONObject settings = new JSONObject(read("data/config/settings.json"));
        String assetPath = settings.getJSONObject("graphics")
                .getJSONObject("illustrations").getString(ILLUSTRATION);
        check(assetPath.equals("graphics/illustrations/" + ILLUSTRATION + ".jpg"),
                "The illustration key must resolve to its dedicated runtime JPEG");
        byte[] bytes = Files.readAllBytes(ROOT.resolve(assetPath));
        check(bytes.length > 3 && (bytes[0] & 0xff) == 0xff
                        && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff,
                "The supplied JPEG must retain its real JPEG format");
        String hash = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(bytes));
        check(hash.equals(SUPPLIED_SHA256),
                "The runtime illustration must preserve the supplied art byte for byte");
        BufferedImage image = ImageIO.read(ROOT.resolve(assetPath).toFile());
        check(image != null && image.getWidth() == 480 && image.getHeight() == 300,
                "The complete supplied composition must fit the native 480x300 panel");
    }

    private static void verifyDialogHook() throws Exception {
        String source = read(
                "src/chiefnavigator/quest/LeagueSurvivorRescueDialogPlugin.java");
        check(Pattern.compile("ILLUSTRATION_ID\\s*=\\s*\"" + ILLUSTRATION + "\"")
                        .matcher(source).find(),
                "The rescue shell must refer to the registered aftermath illustration");
        int initStart = source.indexOf("protected void initConversation(");
        int initEnd = source.indexOf("protected void handleOptionSelected(", initStart);
        check(initStart >= 0 && initEnd > initStart,
                "The rescue shell must retain its normal initialization and choice dispatch");
        String init = source.substring(initStart, initEnd);
        Matcher imageCall = Pattern.compile(
                "dialog\\.getVisualPanel\\(\\)\\.showImagePortion\\(\\s*"
                        + "\"illustrations\",\\s*ILLUSTRATION_ID,\\s*"
                        + "480f,\\s*300f,\\s*0f,\\s*0f,\\s*480f,\\s*300f\\s*\\)")
                .matcher(init);
        check(imageCall.find(),
                "Initialization must show the full supplied image at its native dimensions");
        check(init.indexOf("dialog.showVisualPanel()") >= 0
                        && init.indexOf("dialog.showVisualPanel()") < imageCall.start()
                        && imageCall.end() < init.indexOf("fire(\"league_victory\")"),
                "The visual panel and image must be ready before the opening prose fires");
        check(source.indexOf("showImagePortion(")
                        == source.lastIndexOf("showImagePortion("),
                "The scene must initialize its image once rather than replace it each page");
        for (String replacement : List.of("showPersonInfo(", "showFleetInfo(",
                "showImageVisual(", "showDefaultVisual(", "hideVisualPanel(")) {
            check(!source.contains(replacement),
                    "The rescue shell must retain the aftermath image: " + replacement);
        }
    }

    /** Respect quoted multiline fields and doubled quotes in Starsector's CSV. */
    private static List<List<String>> parseCsv(String csv) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < csv.length(); i++) {
            char c = csv.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                    cell.append(c);
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (!quoted && (c == ',' || c == '\n')) {
                row.add(cell.toString());
                cell.setLength(0);
                if (c == '\n') {
                    rows.add(row);
                    row = new ArrayList<>();
                }
            } else {
                cell.append(c);
            }
        }
        check(!quoted, "The rules CSV must have balanced quoted fields");
        if (cell.length() > 0 || !row.isEmpty()) {
            row.add(cell.toString());
            rows.add(row);
        }
        return rows;
    }

    private static void verifySceneKeepsImage() throws Exception {
        int sceneRows = 0;
        boolean rescue = false;
        boolean abandon = false;
        for (List<String> row : parseCsv(read("data/campaign/rules.csv"))) {
            if (row.size() < 6 || !row.get(1).startsWith("ChiefNavigatorLeagueRescue_")) {
                continue;
            }
            sceneRows++;
            String script = row.get(3).trim();
            check(script.isEmpty() || script.equals("DismissDialog"),
                    "The rescue rules must preserve the initialized illustration on "
                            + row.get(0) + ": " + script);
            rescue |= row.get(5).contains("league_rescue:Deploy the shuttle crews.");
            abandon |= row.get(5).contains("league_abandon:Leave the raiders to their fate.");
        }
        check(sceneRows >= 9 && rescue && abandon,
                "The illustrated scene must still include its continuation and both decisions");
    }

    public static void main(String[] args) throws Exception {
        verifySuppliedAsset();
        verifyDialogHook();
        verifySceneKeepsImage();
        System.out.println("League survivor illustration regression checks passed.");
    }
}
