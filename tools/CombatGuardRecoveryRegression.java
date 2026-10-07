package chiefnavigator.topography;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import com.fs.starfarer.api.util.Misc;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/** Exercises the native recovery blocker with the authored Combat Guard skins. */
public final class CombatGuardRecoveryRegression {
    public static void main(String[] args) throws Exception {
        SettingsAPI previousSettings = Global.getSettings();
        SectorAPI previousSector = Global.getSector();
        try {
            Global.setSettings(SinniTopographyLayoutRegression.mock(SettingsAPI.class, (name, values) -> null));
            Map<String, Object> state = new HashMap<>();
            MemoryAPI memory = SinniTopographyLayoutRegression.mock(MemoryAPI.class, (name, values) -> {
                if (name.equals("get")) return state.get(values[0]);
                if (name.equals("set")) state.put((String) values[0], values[1]);
                return null;
            });
            Global.setSector(SinniTopographyLayoutRegression.mock(SectorAPI.class,
                    (name, values) -> name.equals("getMemoryWithoutUpdate") ? memory : null));
            for (String id : List.of("bastillon", "berserker", "defender", "picket", "rampart", "sentry", "warden", "guardian")) {
                JSONObject skin = new JSONObject(Files.readString(Path.of("data/hulls/skins/chief_navigator_combat_guard_" + id + ".skin")));
                Set<ShipTypeHints> hints = EnumSet.of(ShipTypeHints.UNBOARDABLE);
                ShipHullSpecAPI hull = SinniTopographyLayoutRegression.mock(ShipHullSpecAPI.class, (name, values) -> {
                    if (name.equals("getHints")) return hints;
                    if (name.equals("hasTag")) return "derelict".equals(values[0]) || "auto_rec".equals(values[0]);
                    return null;
                });
                if (!Misc.isUnboardable(hull)) throw new AssertionError("Native vanilla drone must initially be blocked without Automated Ships");
                JSONArray remove = skin.optJSONArray("removeHints");
                if (remove == null) throw new AssertionError("Missing recovery hint removal for " + id);
                for (int index = 0; index < remove.length(); index++) hints.remove(ShipTypeHints.valueOf(remove.getString(index)));
                if (Misc.isUnboardable(hull)) throw new AssertionError("Patched Combat Guard remains unboardable: " + id);
            }
            if (!Misc.getAllowedRecoveryTags().isEmpty()) throw new AssertionError("Verification must not grant Automated Ships recovery globally");
            System.out.println("PASS: all eight Combat Guard skins pass the native boarding/recovery blocker without Automated Ships or global recovery-tag changes.");
        } finally {
            Global.setSector(previousSector);
            Global.setSettings(previousSettings);
        }
    }
}
