package chiefnavigator.quest;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import com.fs.starfarer.campaign.rules.Memory;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import org.json.JSONObject;

/** Checks the vanilla location-music route and the Labor V handoff. */
public final class AshenVergeMusicRegression {
    static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    interface Call { Object invoke(String name, Object[] args); }
    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (proxy, method, args) -> {
                    if (method.getName().equals("equals")) return proxy == args[0];
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    Object result = call.invoke(method.getName(), args);
                    if (result != null) return result;
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == float.class) return 0f;
                    if (method.getReturnType() == int.class) return 0;
                    return null;
                }));
    }
    static String key(String name) throws Exception {
        Field f = MenelausTrial.class.getDeclaredField(name); f.setAccessible(true);
        return (String) f.get(null);
    }
    static final class NativeMusic extends MusicPlayerPluginImpl {
        String selected() { return getStarSystemMusicSetId(); }
    }
    public static void main(String[] args) throws Exception {
        SectorAPI previousSector = Global.getSector(); SettingsAPI previousSettings = Global.getSettings();
        SoundPlayerAPI previousSound = Global.getSoundPlayer();
        try {
            Memory campaign = new Memory(), ambient = new Memory(), other = new Memory();
            ambient.set(OdysseyExpanseSystem.GENERATED_ASHEN_VERGE, true);
            StarSystemAPI ashen = mock(StarSystemAPI.class, (n, a) -> {
                if (n.equals("getOptionalUniqueId")) return OdysseyExpanseSystem.ASHEN_VERGE_ID;
                if (n.equals("getMemoryWithoutUpdate")) return ambient;
                return null;
            });
            StarSystemAPI another = mock(StarSystemAPI.class, (n, a) -> {
                if (n.equals("getOptionalUniqueId")) return "other_system";
                if (n.equals("getMemoryWithoutUpdate")) return other;
                return null;
            });
            CampaignFleetAPI player = mock(CampaignFleetAPI.class,
                    (n, a) -> n.equals("getContainingLocation") ? ashen : null);
            Global.setSector(mock(SectorAPI.class, (n, a) -> {
                if (n.equals("getStarSystems")) return List.of(ashen, another);
                if (n.equals("getPlayerFleet")) return player;
                if (n.equals("getMemoryWithoutUpdate")) return campaign;
                return null;
            }));
            Global.setSettings(mock(SettingsAPI.class,
                    (n, a) -> n.equals("getCurrentState") ? GameState.CAMPAIGN : null));
            List<String> audio = new ArrayList<>();
            Global.setSoundPlayer(mock(SoundPlayerAPI.class, (n, a) -> {
                if (n.equals("playCustomMusic")) audio.add((String) a[2]);
                return null;
            }));
            Method setup = OdysseyExpanseSystem.class.getDeclaredMethod(
                    "ensureSystemMusic", StarSystemAPI.class, List.class);
            setup.setAccessible(true);
            setup.invoke(null, null, List.of(ashen, another));
            NativeMusic nativeMusic = new NativeMusic();
            check(FinalLaborMusic.ASHEN_EXPLORATION_ID.equals(nativeMusic.selected()),
                    "Before the story, vanilla music must select only ordinary Lacedaemon exploration");
            check(!other.contains(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY), "Other systems keep their music");
            check(audio.isEmpty(), "Ambient selection must not force a custom stream or suspend music");
            ambient.set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY,
                    FinalLaborMusic.ASHEN_COMPLETED_EXPLORATION_ID);
            FinalLaborMusic.releaseMissionMusic();
            check(FinalLaborMusic.ASHEN_EXPLORATION_ID.equals(nativeMusic.selected()),
                    "An ineligible saved post-story cue is restored without editing quest flags");
            check(campaign.getKeys().isEmpty(), "Ambient eligibility cannot grant any story progress");

            campaign.set(MenelausTrial.ACCEPTED, true);
            campaign.set(MenelausTrial.FINAL_LABOR_BRIEFED, true);
            campaign.set(key("FINAL_LABOR_FORCED_FOR_TESTING"), true);
            campaign.set(key("LABOR_REPORT_MIGRATION"), true);
            check(MenelausTrial.isFinalLaborActive(), "Active Labor V fixture");
            FinalLaborMusic.resetForGameLoad();
            FinalLaborMusic.playMissionStart();
            setup.invoke(null, null, List.of(ashen, another));
            check(FinalLaborMusic.MISSION_START_ID.equals(nativeMusic.selected()),
                    "World reconciliation must preserve Labor V's mission soundtrack");
            campaign.set(MenelausTrial.GAUTAMA_DEFEATED, true);
            FinalLaborMusic.releaseMissionMusic();
            check(FinalLaborMusic.ASHEN_EXPLORATION_ID.equals(nativeMusic.selected()),
                    "Victory without the final debrief cannot unlock the ending recording");
            campaign.set(MenelausCompletionDialogPlugin.SHOWN, true);
            FinalLaborMusic.releaseMissionMusic();
            check(FinalLaborMusic.ASHEN_EXPLORATION_ID.equals(nativeMusic.selected()),
                    "An ending UI flag alone cannot substitute for completing the story");
            campaign.set(MenelausTrial.FINAL_DEBRIEF_COMPLETE, false);
            setup.invoke(null, null, List.of(ashen, another));
            check(FinalLaborMusic.ASHEN_EXPLORATION_ID.equals(nativeMusic.selected()),
                    "An explicit false completion flag also keeps the song locked");
            campaign.set(MenelausTrial.FINAL_DEBRIEF_COMPLETE, true);
            FinalLaborMusic.releaseMissionMusic();
            check(FinalLaborMusic.ASHEN_COMPLETED_EXPLORATION_ID.equals(nativeMusic.selected()),
                    "Only the completed final debrief unlocks the post-story random playlist");
            setup.invoke(null, null, List.of(ashen, another));
            check(FinalLaborMusic.ASHEN_COMPLETED_EXPLORATION_ID.equals(nativeMusic.selected()),
                    "Fresh world setup and ordinary upkeep use the same story gate");
            ambient.set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY_MISSION, "another_mission");
            check("another_mission".equals(nativeMusic.selected()), "Native mission override keeps priority");
            ambient.unset(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY_MISSION);
            ambient.set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, "foreign_override");
            FinalLaborMusic.releaseMissionMusic();
            check("foreign_override".equals(nativeMusic.selected()), "Release preserves unrelated overrides");

            JSONObject music = new JSONObject(Files.readString(Path.of("data/config/sounds.json"))).getJSONObject("music");
            var ordinary = music.getJSONArray(FinalLaborMusic.ASHEN_EXPLORATION_ID);
            check(ordinary.length() == 1, "The existing saved ambient ID contains ordinary exploration only");
            var playlist = music.getJSONArray(FinalLaborMusic.ASHEN_COMPLETED_EXPLORATION_ID);
            check(playlist.length() == 2, "Post-story random selection includes exploration and the ending song");
            var ending = music.getJSONArray(MenelausEndingMusic.MUSIC_ID).getJSONObject(0);
            var ambientEnding = playlist.getJSONObject(1);
            check(ambientEnding.getString("file").equals(ending.getString("file"))
                            && ambientEnding.getString("source").equals(ending.getString("source"))
                            && ambientEnding.getDouble("volume") == ending.getDouble("volume"),
                    "Use the exact ending track, path and volume");
            var stock = playlist.getJSONObject(0);
            check(stock.toString().equals(ordinary.getJSONObject(0).toString()),
                    "Both ambient states retain the same ordinary exploration track");
            check(!ordinary.toString().contains(ending.getString("file")),
                    "The ending recording cannot be randomly selected before story completion");
            try (ZipFile nativeTracks = new ZipFile(Path.of(System.getProperty("chiefNavigator.testCore", "../../starsector-core"), stock.getString("source")).toFile())) {
                check(nativeTracks.getEntry(stock.getString("file")) != null, "Stock exploration track exists in native archive");
            }
            System.out.println("PASS: native Lacedaemon music stays spoiler-free until the final debrief; "
                    + "saved cues, victory-only/false/UI flags, completed playlist, Labor V priority and foreign overrides.");
        } finally {
            FinalLaborMusic.resetForGameLoad();
            Global.setSector(previousSector); Global.setSettings(previousSettings); Global.setSoundPlayer(previousSound);
        }
    }
}
