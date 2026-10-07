package chiefnavigator.quest;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipFile;
import org.json.JSONObject;

/** Real timer/ownership implementation against a mocked paused campaign. */
public final class MenelausEndingMusicRegression {
    private static void check(boolean condition, String message) {
        MenelausCompletionRegression.check(condition, message);
    }

    private static final class Fixture {
        final long[] now = {123_000_000L};
        final List<String> audio = new ArrayList<>();
        final List<EveryFrameScript> scripts = new ArrayList<>();
        final InteractionDialogAPI dialog = MenelausCompletionRegression.mock(
                InteractionDialogAPI.class, (name, args) -> {
                    throw new AssertionError("Music must not mutate dialogue: " + name);
                });
        InteractionDialogAPI current = dialog;
        boolean hasUI = true;
        boolean failPlayback;
        boolean failStop;
        boolean failPause;
        final SoundPlayerAPI player = MenelausCompletionRegression.mock(
                SoundPlayerAPI.class, (name, args) -> {
                    if (name.equals("setSuspendDefaultMusicPlayback")) {
                        audio.add("suspend:" + args[0]);
                    } else if (name.equals("playCustomMusic")) {
                        if (args.length == 3 && args[2] == null) {
                            check(Integer.valueOf(0).equals(args[0])
                                            && Integer.valueOf(0).equals(args[1]),
                                    "Opening must stop the current track with no fade");
                            audio.add("stop");
                            if (failStop) throw new IllegalStateException("stop unavailable");
                        } else {
                            check(args.length == 4
                                        && MenelausEndingMusic.MUSIC_ID.equals(args[2])
                                        && Boolean.TRUE.equals(args[3]),
                                "Only the dedicated ending cue may loop");
                            audio.add("play");
                            if (failPlayback) throw new IllegalStateException("audio unavailable");
                        }
                    } else if (name.equals("pauseCustomMusic")) {
                        audio.add("pause");
                        if (failPause) throw new IllegalStateException("pause unavailable");
                    } else if (name.equals("restartCurrentMusic")) {
                        audio.add("restart");
                    }
                    return null;
                });
        final CampaignUIAPI ui = MenelausCompletionRegression.mock(
                CampaignUIAPI.class, (name, args) ->
                        name.equals("getCurrentInteractionDialog") ? current : null);
        final SectorAPI sector = MenelausCompletionRegression.mock(
                SectorAPI.class, (name, args) -> {
                    if (name.equals("getCampaignUI")) return hasUI ? ui : null;
                    if (name.equals("addTransientScript")) {
                        scripts.add((EveryFrameScript) args[0]);
                    }
                    return null;
                });

        Fixture() {
            Global.setSector(sector);
            Global.setSoundPlayer(player);
        }

        MenelausEndingMusic timer() {
            return new MenelausEndingMusic(sector, dialog, () -> now[0]);
        }

        void seconds(double seconds) {
            now[0] = 123_000_000L + (long) (seconds * 1_000_000_000L);
        }
    }

    public static void main(String[] args) throws Exception {
        Fixture f = new Fixture();
        MenelausEndingMusic.start(f.dialog);
        check(f.scripts.size() == 1 && f.scripts.get(0) instanceof MenelausEndingMusic
                        && f.scripts.get(0).runWhilePaused(),
                "Image opening installs one nonpersistent paused-capable timer");
        check(f.audio.equals(List.of("suspend:true", "stop")),
                "Image opening immediately cuts current music and suspends new tracks");
        f.current = null;
        f.scripts.get(0).advance(0f);

        f = new Fixture();
        MenelausEndingMusic timer = f.timer();
        f.seconds(9.999999999);
        timer.advance(100_000f);
        check(f.audio.equals(List.of("suspend:true", "stop")),
                "Campaign acceleration cannot shorten ten seconds of silence");
        f.seconds(10);
        timer.advance(0f);
        check(f.audio.equals(List.of("suspend:true", "stop", "play")),
                "The cue starts at ten monotonic seconds even during a paused frame");
        for (int page = 0; page < 100; page++) {
            f.seconds(10 + page * 0.2);
            timer.advance(0.2f);
        }
        check(f.audio.size() == 3 && !timer.isDone(),
                "Later dialogue pages must not restart or repeatedly request the cue");
        f.current = null;
        timer.advance(0f);
        timer.advance(100f);
        check(timer.isDone() && f.audio.equals(
                        List.of("suspend:true", "stop", "play", "pause", "suspend:false", "restart")),
                "Closure releases custom/default music exactly once");

        f = new Fixture();
        timer = f.timer();
        f.seconds(2);
        f.current = null;
        timer.advance(0f);
        f.seconds(20);
        timer.advance(0f);
        check(timer.isDone() && f.audio.equals(
                        List.of("suspend:true", "stop", "pause", "suspend:false", "restart")),
                "Early closure cancels the piano and releases the silent interval exactly once");

        f = new Fixture();
        timer = f.timer();
        f.seconds(10);
        f.current = MenelausCompletionRegression.mock(
                InteractionDialogAPI.class, (name, values) -> null);
        timer.advance(1f);
        check(timer.isDone() && f.audio.equals(
                        List.of("suspend:true", "stop", "pause", "suspend:false", "restart")),
                "Another open dialogue must never inherit a pending ending cue");

        f = new Fixture();
        timer = f.timer();
        f.seconds(10);
        timer.advance(0f);
        Global.setSector(null);
        timer.advance(0f);
        check(timer.isDone() && f.audio.contains("suspend:false"),
                "The timer cannot claim another campaign");

        f = new Fixture();
        timer = f.timer();
        f.seconds(10);
        f.hasUI = false;
        timer.advance(0f);
        check(timer.isDone() && f.audio.equals(
                        List.of("suspend:true", "stop", "pause", "suspend:false", "restart")),
                "Missing scene UI cancels rather than starting late");

        f = new Fixture();
        Global.setSoundPlayer(null);
        timer = f.timer();
        f.seconds(10);
        timer.advance(0f);
        Global.setSoundPlayer(f.player);
        f.seconds(30);
        timer.advance(1f);
        check(timer.isDone() && f.audio.isEmpty(),
                "Unavailable opening audio must not create a per-frame retry loop");

        f = new Fixture();
        timer = f.timer();
        Global.setSoundPlayer(null);
        f.seconds(10);
        timer.advance(0f);
        check(timer.isDone() && !f.audio.contains("play")
                        && f.audio.contains("suspend:false"),
                "Losing the opening audio player releases ownership without a late piano cue");

        f = new Fixture();
        f.failStop = true;
        f.failPause = true;
        timer = f.timer();
        f.seconds(10);
        timer.advance(0f);
        check(timer.isDone() && f.audio.equals(
                        List.of("suspend:true", "stop", "pause", "suspend:false", "restart")),
                "An opening audio failure must still attempt every ownership release step");

        f = new Fixture();
        timer = f.timer();
        f.failPlayback = true;
        f.failPause = true;
        f.seconds(10);
        timer.advance(0f);
        timer.advance(0f);
        check(timer.isDone() && f.audio.equals(
                        List.of("suspend:true", "stop", "play", "pause", "suspend:false", "restart")),
                "Playback/cleanup failures remain nonfatal and still restore default music");

        verifyAsset();
        Global.setSoundPlayer(null);
        Global.setSector(null);
        System.out.println("PASS: ending opening cuts music immediately, keeps ten real seconds "
                + "of silence, scopes paused playback "
                + "to its exact scene, cancels/releases safely, and uses the unchanged official Ogg.");
    }

    private static void verifyAsset() throws Exception {
        JSONObject sounds = new JSONObject(Files.readString(Path.of("data/config/sounds.json")));
        var cue = sounds.getJSONObject("music").getJSONArray(MenelausEndingMusic.MUSIC_ID);
        check(cue.length() == 1, "Ending cue has exactly one track");
        JSONObject track = cue.getJSONObject(0);
        byte[] audio = Files.readAllBytes(Path.of(track.getString("source"), track.getString("file")));
        try (ZipFile archive = new ZipFile("sounds/music/source/Peritune_Glistening_Ripples.zip")) {
            byte[] original = archive.getInputStream(archive.getEntry(
                    "Peritune_Glistening_Ripples/Peritune_Glistening_Ripples.ogg")).readAllBytes();
            check(Arrays.equals(audio, original), "Runtime recording must be the unmodified official Ogg");
        }
        ByteBuffer bytes = ByteBuffer.wrap(audio).order(ByteOrder.LITTLE_ENDIAN);
        int offset = 0;
        int sampleRate = 0;
        long finalGranule = 0;
        boolean ended = false;
        while (offset < audio.length) {
            check(offset + 27 <= audio.length && audio[offset] == 'O'
                            && audio[offset + 1] == 'g' && audio[offset + 2] == 'g'
                            && audio[offset + 3] == 'S' && audio[offset + 4] == 0,
                    "Every page must have a complete Ogg header");
            int segments = audio[offset + 26] & 0xff;
            int payload = offset + 27 + segments;
            check(payload <= audio.length, "Ogg segment table must be complete");
            int length = 0;
            for (int i = 0; i < segments; i++) length += audio[offset + 27 + i] & 0xff;
            check(payload + length <= audio.length, "Ogg payload must not be truncated");
            if (offset == 0) {
                check(length >= 30 && audio[payload] == 1
                                && new String(audio, payload + 1, 6,
                                        java.nio.charset.StandardCharsets.US_ASCII).equals("vorbis"),
                        "Audio must be actual Ogg Vorbis, not a renamed MP3");
                sampleRate = bytes.getInt(payload + 12);
            }
            ended = (audio[offset + 5] & 4) != 0;
            finalGranule = bytes.getLong(offset + 6);
            offset = payload + length;
        }
        check(ended && sampleRate > 0 && finalGranule > sampleRate * 10L,
                "Official Ogg must contain the full music stream and final end page");
        String source = Files.readString(Path.of(
                "src/chiefnavigator/quest/MenelausCompletionDialogPlugin.java"));
        int opened = source.indexOf("dialog.getVisualPanel().showCustomPanel(");
        int started = source.indexOf("MenelausEndingMusic.start(dialog);");
        check(opened >= 0 && started > opened && started < source.indexOf("prepareMemoryMap();"),
                "Production timer must start immediately after the illustration opens");
        check(started == source.lastIndexOf("MenelausEndingMusic.start(dialog);"),
                "Choices and subsequent scene pages must not install another timer");
        System.out.printf("Official ending Ogg: %d Hz, %.2f seconds, %d bytes.%n",
                sampleRate, (double) finalGranule / sampleRate, audio.length);
    }
}
