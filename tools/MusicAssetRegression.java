import chiefnavigator.quest.FinalLaborMusic;
import chiefnavigator.quest.UngaikyoMusic;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONObject;

/** Dedicated encounter cues must resolve to distinct, genuine Vorbis assets. */
public final class MusicAssetRegression {
    public static void main(String[] args) throws Exception {
        JSONObject music = new JSONObject(Files.readString(
                Path.of("data/config/sounds.json"))).getJSONObject("music");
        String[] ids = {FinalLaborMusic.MISSION_START_ID,
                FinalLaborMusic.PERIMETER_FIGHT_ID, FinalLaborMusic.FINAL_FIGHT_ID,
                UngaikyoMusic.OPENING_ID, UngaikyoMusic.ENRAGED_ID};
        Set<Path> paths = new HashSet<>();
        for (String id : ids) {
            JSONObject entry = music.getJSONArray(id).getJSONObject(0);
            Path path = Path.of(entry.getString("source"), entry.getString("file"));
            check(paths.add(path), "Encounter cues must not share/repoint an asset: " + id);
            byte[] bytes = Files.readAllBytes(path);
            check(bytes.length > 1_000_000, "Expected a full recording: " + path);
            String header = new String(bytes, 0, Math.min(bytes.length, 96),
                    StandardCharsets.ISO_8859_1);
            check(header.startsWith("OggS") && header.contains("\u0001vorbis"),
                    "Runtime audio must be genuine Ogg Vorbis: " + path);
        }
        String predator = Files.readString(Path.of(
                "src/chiefnavigator/quest/OdysseyPredatorScript.java"));
        check(predator.matches("(?s).*memory\\.set\\(MusicPlayerPluginImpl\\."
                        + "COMBAT_MUSIC_SET_MEM_KEY,\\s*FinalLaborMusic\\."
                        + "PERIMETER_FIGHT_ID\\);.*"),
                "Every themed Strike must also advertise Abyssal to native combat music");
        String credits = Files.readString(Path.of("sounds/chief_navigator/AUDIO_SOURCES.md"));
        check(credits.contains("Lord of Ashes") && credits.contains("4nrI88fmc-I")
                        && !credits.contains("uox67Jtowhk")
                        && credits.contains("Abyssal Rhapsody") && credits.contains("gW2_9Vz_aFI"),
                "Both new recordings must retain Lappy credit and primary source URLs");
        byte[] opening = Files.readAllBytes(Path.of("sounds/music/chief_navigator_ungaikyo_opening.ogg"));
        check(new String(opening, StandardCharsets.ISO_8859_1).contains("4nrI88fmc-I"),
                "The opening audio itself must identify the exact user-selected Lord of Ashes recording");
        System.out.println("PASS: five distinct full Vorbis cues, native Strike music routing, "
                + "and Lord of Ashes/Abyssal source credits.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
