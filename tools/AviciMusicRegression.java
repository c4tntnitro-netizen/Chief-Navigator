import chiefnavigator.campaign.AlphaOdysseyMusicScript;
import chiefnavigator.campaign.AviciMusicScript;
import chiefnavigator.campaign.SanzuMusicScript;
import chiefnavigator.quest.OdysseyExpanseSystem;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Exercises Avici's native cue routing, looping, combat return and exit. */
public final class AviciMusicRegression {
    public static void main(String[] args) throws Exception {
        SettingsAPI oldSettings = Global.getSettings();
        SectorAPI oldSector = Global.getSector();
        SoundPlayerAPI oldSound = Global.getSoundPlayer();
        try {
            Map<String, Object> aviciMemory = new HashMap<>();
            aviciMemory.put("$chief_navigator_odyssey_authored_system_v1", true);
            aviciMemory.put(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, "chief_navigator_avici_loop_b");
            StarSystemAPI avici = system(OdysseyExpanseSystem.MESSINA_ID, aviciMemory);
            Map<String, Object> alphaMemory = new HashMap<>(), sanzuMemory = new HashMap<>();
            StarSystemAPI alpha = system(OdysseyExpanseSystem.SYSTEM_ID, alphaMemory);
            StarSystemAPI sanzu = system(OdysseyExpanseSystem.SILENT_WAKE_ID, sanzuMemory);
            Method setup = OdysseyExpanseSystem.class.getDeclaredMethod(
                    "ensureSystemMusic", StarSystemAPI.class, List.class);
            setup.setAccessible(true);
            setup.invoke(null, alpha, List.of(avici, sanzu));
            check(AviciMusicScript.MUSIC_ID.equals(aviciMemory.get(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY))
                            && AlphaOdysseyMusicScript.MUSIC_ID.equals(alphaMemory.get(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY))
                            && SanzuMusicScript.MUSIC_ID.equals(sanzuMemory.get(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY)),
                    "Native system setup must change only Avici's soundtrack");

            LocationAPI[] location = {avici};
            GameState[] state = {GameState.CAMPAIGN};
            String[] current = {"chief_navigator_avici_loop_b"};
            List<String> cues = new ArrayList<>();
            int[] pauses = {0}, restarts = {0};
            CampaignFleetAPI player = WallResearchRegression.mock(CampaignFleetAPI.class,
                    (name, values) -> name.equals("getContainingLocation") ? location[0] : null);
            Global.setSector(WallResearchRegression.mock(SectorAPI.class, (name, values) -> {
                if (name.equals("getStarSystems")) return List.of(avici);
                if (name.equals("getPlayerFleet")) return player;
                return null;
            }));
            Global.setSettings(WallResearchRegression.mock(SettingsAPI.class,
                    (name, values) -> name.equals("getCurrentState") ? state[0] : null));
            Global.setSoundPlayer(WallResearchRegression.mock(SoundPlayerAPI.class, (name, values) -> {
                if (name.equals("playCustomMusic")) {
                    check(Boolean.TRUE.equals(values[3]), "Avici's new cue must loop");
                    current[0] = (String) values[2];
                    cues.add(current[0]);
                }
                if (name.equals("setSuspendDefaultMusicPlayback")) {
                    check(Boolean.FALSE.equals(values[0]), "Native encounter/combat priority must remain available");
                }
                if (name.equals("getCurrentMusicId")) return current[0];
                if (name.equals("pauseCustomMusic")) pauses[0]++;
                if (name.equals("restartCurrentMusic")) restarts[0]++;
                return null;
            }));

            AviciMusicScript script = new AviciMusicScript();
            script.advance(0f);
            check(cues.equals(List.of("music_dweller_encounter_hostile")), "Entry selects the exact vanilla Dweller cue");
            for (int i = 0; i < 100; i++) script.advance(600f);
            check(cues.size() == 1, "No repeated restarts or timed KARMA playlist while exploring");
            state[0] = GameState.COMBAT;
            current[0] = "chief_navigator_budai_fight";
            script.reportPlayerEngagement(null);
            script.advance(600f);
            check(cues.size() == 1, "Do not replace Budai or any other combat music");
            state[0] = GameState.CAMPAIGN;
            script.advance(0f);
            script.advance(0f);
            check(cues.size() == 2 && cues.get(1).equals(AviciMusicScript.MUSIC_ID),
                    "Return from combat restarts Avici once");
            location[0] = alpha;
            script.advance(0f);
            script.advance(0f);
            check(pauses[0] == 1 && restarts[0] == 1, "Leaving releases only the active Avici stream once");
            location[0] = avici;
            script.advance(0f);
            current[0] = AlphaOdysseyMusicScript.MUSIC_ID;
            location[0] = alpha;
            script.advance(0f);
            check(pauses[0] == 1, "Do not pause another system's already-started track");
            check(script.runWhilePaused() && !script.isDone(), "Keep the existing campaign listener lifecycle");

            String nativeSounds = Files.readString(Path.of("../../starsector-core/data/config/sounds.json"));
            String nativeFaction = Files.readString(Path.of("../../starsector-core/data/world/factions/dweller.faction"));
            String modSounds = Files.readString(Path.of("data/config/sounds.json"));
            check(nativeSounds.contains("\"" + AviciMusicScript.MUSIC_ID + "\"")
                            && nativeSounds.contains("faction_dweller_encounter.ogg")
                            && nativeFaction.contains(AviciMusicScript.MUSIC_ID)
                            && !modSounds.contains("\"" + AviciMusicScript.MUSIC_ID + "\""),
                    "Use the existing vanilla encounter recording without overriding its definition");
            System.out.println("PASS: vanilla Dweller cue, native Avici routing, looping, no restart spam, "
                    + "combat return, exit cleanup, and unchanged Alpha/Sanzu/combat priorities.");
        } finally {
            Global.setSettings(oldSettings);
            Global.setSector(oldSector);
            Global.setSoundPlayer(oldSound);
        }
    }

    private static StarSystemAPI system(String id, Map<String, Object> values) {
        MemoryAPI memory = WallResearchRegression.mock(MemoryAPI.class, (name, args) -> {
            if (name.equals("set")) values.put((String) args[0], args[1]);
            if (name.equals("getBoolean")) return Boolean.TRUE.equals(values.get(args[0]));
            if (name.equals("getString")) return values.get(args[0]);
            return null;
        });
        return WallResearchRegression.mock(StarSystemAPI.class, (name, args) -> {
            if (name.equals("getOptionalUniqueId")) return id;
            if (name.equals("getMemoryWithoutUpdate")) return memory;
            return null;
        });
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
