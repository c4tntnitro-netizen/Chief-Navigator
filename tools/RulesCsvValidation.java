import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/** External build check against the installed game's actual CSV parser. */
public final class RulesCsvValidation {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected rules.csv path");
        String source = Files.readString(Path.of(args[0]), StandardCharsets.UTF_8);
        JSONArray rows;
        try {
            // This installed loader appears in the startup crash stack. Keep
            // engine internals confined to this external verification tool.
            rows = (JSONArray) Class.forName("com.fs.starfarer.loading.G")
                    .getMethod("o00000", String.class).invoke(null, source);
        } catch (InvocationTargetException ex) {
            throw new IllegalStateException("Starsector rejected " + args[0], ex.getCause());
        }
        if (rows == null || rows.length() == 0) throw new AssertionError("No dialogue rules");
        Set<String> ids = new HashSet<>();
        for (int index = 0; index < rows.length(); index++) {
            JSONObject row = rows.getJSONObject(index);
            String id = row.optString("id").trim();
            if (id.isEmpty() || !ids.add(id)) {
                throw new AssertionError("Missing or duplicate rule id at row " + index + ": " + id);
            }
            if (!row.has("trigger") || !row.has("text") || !row.has("options")) {
                throw new AssertionError("Incomplete native CSV row: " + id);
            }
        }
        System.out.println("PASS: installed Starsector CSV loader accepted " + rows.length()
                + " complete, unique dialogue rules.");
    }
}
