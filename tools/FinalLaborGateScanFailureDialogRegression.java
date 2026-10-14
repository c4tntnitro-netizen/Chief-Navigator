package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.VisualPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import java.awt.Color;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/** Durable failure-scene scheduling, vanilla art, letterboxing, and continuation. */
public final class FinalLaborGateScanFailureDialogRegression {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    interface Call { Object invoke(String name, Object[] args); }

    private static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("hashCode")) return System.identityHashCode(proxy);
                    if (name.equals("equals")) return proxy == args[0];
                    if (name.equals("toString")) return type.getSimpleName();
                    Object value = call.invoke(name, args);
                    if (value != null) return value;
                    Class<?> result = method.getReturnType();
                    if (result == boolean.class) return false;
                    if (result == int.class) return 0;
                    if (result == float.class) return 0f;
                    return null;
                }));
    }

    static final class Fixture {
        final Map<String, Object> flags;
        final List<String> text = new ArrayList<>();
        final List<Object> options = new ArrayList<>();
        final List<float[]> renders = new ArrayList<>();
        boolean showingDialog, showingMenu, coreTab, currentDialog, playerBattle, transition, noUI;
        boolean allowOpen = true, dismissed;
        int attempts, selectedChoices;
        float spriteWidth = 800f, spriteHeight = 500f, spriteAlpha = 0.65f;
        Color spriteColor = Color.CYAN;
        BaseCustomUIPanelPlugin illustration;
        InteractionDialogPlugin opened;
        final MemoryAPI memory;
        final SpriteAPI sprite;
        final CampaignFleetAPI player;
        final InteractionDialogAPI dialog;

        Fixture(Map<String, Object> flags) {
            this.flags = flags;
            memory = mock(MemoryAPI.class, (name, args) -> {
                if (name.equals("getBoolean")) return Boolean.TRUE.equals(flags.get(args[0]));
                if (name.equals("set")) {
                    check(args.length == 2, "Scene flags must be durable without an expiry interval");
                    flags.put((String) args[0], args[1]);
                }
                if (name.equals("unset")) flags.remove(args[0]);
                return null;
            });
            sprite = mock(SpriteAPI.class, (name, args) -> {
                if (name.equals("getWidth")) return spriteWidth;
                if (name.equals("getHeight")) return spriteHeight;
                if (name.equals("getAlphaMult")) return spriteAlpha;
                if (name.equals("getColor")) return spriteColor;
                if (name.equals("setSize")) { spriteWidth = (Float) args[0]; spriteHeight = (Float) args[1]; }
                if (name.equals("setAlphaMult")) spriteAlpha = (Float) args[0];
                if (name.equals("setColor")) spriteColor = (Color) args[0];
                if (name.equals("renderAtCenter")) renders.add(new float[] {spriteWidth, spriteHeight,
                        (Float) args[0], (Float) args[1]});
                return null;
            });
            player = mock(CampaignFleetAPI.class, (name, args) -> {
                if (name.equals("getBattle") && playerBattle) return mock(BattleAPI.class, (n, a) -> null);
                if (name.equals("isInHyperspaceTransition")) return transition;
                return null;
            });
            TextPanelAPI textPanel = mock(TextPanelAPI.class, (name, args) -> {
                if (name.equals("addParagraph")) text.add((String) args[0]);
                return null;
            });
            OptionPanelAPI optionPanel = mock(OptionPanelAPI.class, (name, args) -> {
                if (name.equals("clearOptions")) options.clear();
                if (name.equals("addOption")) options.add(args[1]);
                return null;
            });
            VisualPanelAPI visual = mock(VisualPanelAPI.class, (name, args) -> {
                if (name.equals("showCustomPanel")) {
                    check((Float) args[0] == 480f && (Float) args[1] == 300f,
                            "Use the established ending illustration panel");
                    illustration = (BaseCustomUIPanelPlugin) args[2];
                }
                return null;
            });
            dialog = mock(InteractionDialogAPI.class, (name, args) -> {
                if (name.equals("getTextPanel")) return textPanel;
                if (name.equals("getOptionPanel")) return optionPanel;
                if (name.equals("getVisualPanel")) return visual;
                if (name.equals("dismiss")) dismissed = true;
                if (name.equals("addOptionSelectedText")) selectedChoices++;
                return null;
            });
            CampaignUIAPI ui = mock(CampaignUIAPI.class, (name, args) -> {
                if (name.equals("isShowingDialog")) return showingDialog;
                if (name.equals("isShowingMenu")) return showingMenu;
                if (name.equals("getCurrentCoreTab") && coreTab) return com.fs.starfarer.api.campaign.CoreUITabId.INTEL;
                if (name.equals("getCurrentInteractionDialog") && currentDialog) return dialog;
                if (name.equals("showInteractionDialog")) {
                    attempts++;
                    if (!allowOpen) return false;
                    check(args[1] == player, "The ending screen must use the existing player fleet");
                    opened = (InteractionDialogPlugin) args[0];
                    opened.init(dialog);
                    return true;
                }
                return null;
            });
            Global.setSector(mock(SectorAPI.class, (name, args) -> {
                if (name.equals("getMemoryWithoutUpdate")) return memory;
                if (name.equals("getPlayerFleet")) return player;
                if (name.equals("getCampaignUI")) return noUI ? null : ui;
                throw new AssertionError("Defeat presentation must not shut down or alter the campaign: " + name);
            }));
            Global.setSettings(mock(SettingsAPI.class, (name, args) -> {
                if (name.equals("getSprite")) {
                    check(args.length == 2 && args[0].equals("illustrations")
                                    && args[1].equals("bombard_saturation_result"),
                            "Request the vanilla saturation-bombardment illustration");
                    return sprite;
                }
                return null;
            }));
        }
    }

    private static void nativeAssetExists() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        while (root != null && !Files.isRegularFile(root.resolve("starsector-core/data/config/settings.json"))) {
            root = root.getParent();
        }
        check(root != null, "Find the installed native game assets");
        Path core = root.resolve("starsector-core");
        Matcher match = Pattern.compile("\"bombard_saturation_result\"\\s*:\\s*\"([^\"]+)\"")
                .matcher(Files.readString(core.resolve("data/config/settings.json")));
        check(match.find() && match.group(1).equals("graphics/illustrations/bombardment_saturation.jpg"),
                "The requested vanilla illustration key must resolve to the actual native JPEG");
        check(ImageIO.read(core.resolve(match.group(1)).toFile()) != null, "Vanilla illustration must decode");
    }

    public static void main(String[] args) throws Exception {
        SectorAPI priorSector = Global.getSector();
        SettingsAPI priorSettings = Global.getSettings();
        try {
            nativeAssetExists();
            Map<String, Object> flags = new HashMap<>();
            Fixture f = new Fixture(flags);
            FinalLaborGateScanFailureDialogPlugin.request();
            check(flags.isEmpty(), "Another failure or a victory must not queue this ending");
            flags.put(MenelausTrial.FINAL_LABOR_GATE_SCAN_FAILED, true);
            FinalLaborGateScanFailureDialogPlugin.request();
            check(Boolean.TRUE.equals(flags.get(FinalLaborGateScanFailureDialogPlugin.PENDING)), "Queue durably");
            for (int busy = 0; busy < 7; busy++) {
                f.showingDialog = busy == 0; f.showingMenu = busy == 1; f.coreTab = busy == 2;
                f.currentDialog = busy == 3; f.playerBattle = busy == 4; f.transition = busy == 5; f.noUI = busy == 6;
                FinalLaborGateScanFailureDialogPlugin.tryShowPending();
                check(f.attempts == 0 && !flags.containsKey(FinalLaborGateScanFailureDialogPlugin.SHOWN),
                        "Defer while any existing UI, battle or transition owns the screen");
            }
            // Reconstruct all presentation objects from the saved flags: no transient latch is required.
            f = new Fixture(new HashMap<>(flags));
            f.allowOpen = false;
            FinalLaborGateScanFailureDialogPlugin.tryShowPending();
            check(f.attempts == 1 && f.flags.containsKey(FinalLaborGateScanFailureDialogPlugin.PENDING),
                    "An engine-refused opening must retain the saved pending scene");
            f.allowOpen = true;
            FinalLaborGateScanFailureDialogPlugin.tryShowPending();
            check(f.opened instanceof FinalLaborGateScanFailureDialogPlugin
                            && !f.flags.containsKey(FinalLaborGateScanFailureDialogPlugin.PENDING)
                            && Boolean.TRUE.equals(f.flags.get(FinalLaborGateScanFailureDialogPlugin.SHOWN)),
                    "Only a successful display consumes the pending scene");
            check(f.text.equals(List.of("GAME OVER", "The defense of ithaca has failed.",
                            "The Heavenly Strike has completed its scan of Ithaca’s Gate. "
                                    + "To the horror of all who witness, the threat swarm disappears into the gateway. "
                                    + "You frantically contact all major factions to see where the Threat or Gautama may "
                                    + "have surfaced in the Core Worlds, but no one has seen anything of the sort.",
                            "But you know. One day, the Sword of Damocles will fall."))
                            && f.options.equals(List.of(FinalLaborGateScanFailureDialogPlugin.CONTINUE)),
                    "Show the gate-scan defeat narrative and a single Continue option");
            f.illustration.positionChanged(mock(PositionAPI.class, (name, values) -> {
                if (name.equals("getWidth")) return 480f;
                if (name.equals("getHeight")) return 200f;
                return 0f;
            }));
            f.illustration.renderBelow(0.7f);
            check(f.renders.size() == 2 && f.renders.get(1)[0] == 320f && f.renders.get(1)[1] == 200f
                            && f.spriteWidth == 800f && f.spriteHeight == 500f && f.spriteAlpha == 0.65f
                            && f.spriteColor.equals(Color.CYAN),
                    "Letterbox the full illustration and restore sprite size, alpha and color");
            f.opened.optionSelected("Unknown", "unknown");
            check(!f.dismissed, "Only Continue closes the scene");
            f.opened.optionSelected("Continue", FinalLaborGateScanFailureDialogPlugin.CONTINUE);
            check(f.dismissed && f.selectedChoices == 2, "Continue closes normally and preserves choice history");
            FinalLaborGateScanFailureDialogPlugin.request();
            FinalLaborGateScanFailureDialogPlugin.tryShowPending();
            check(f.attempts == 2 && !f.flags.containsKey(FinalLaborGateScanFailureDialogPlugin.PENDING),
                    "Repeated upkeep and requests must not replay an acknowledged ending");
            FinalLaborGateScanFailureDialogPlugin.resetForTesting();
            check(!f.flags.containsKey(FinalLaborGateScanFailureDialogPlugin.PENDING)
                            && !f.flags.containsKey(FinalLaborGateScanFailureDialogPlugin.SHOWN)
                            && Boolean.TRUE.equals(f.flags.get(MenelausTrial.FINAL_LABOR_GATE_SCAN_FAILED)),
                    "Explicit presentation reset must not change gameplay failure state");
            f.flags.put(FinalLaborGateScanFailureDialogPlugin.PENDING, true);
            f.flags.put(MenelausTrial.FINAL_LABOR_GATE_SCAN_FAILED, false);
            FinalLaborGateScanFailureDialogPlugin.tryShowPending();
            check(f.attempts == 2, "Stale pending state must not display after failure mode changes");
            Global.setSector(null);
            FinalLaborGateScanFailureDialogPlugin.request();
            FinalLaborGateScanFailureDialogPlugin.tryShowPending();
            FinalLaborGateScanFailureDialogPlugin.resetForTesting();
            System.out.println("PASS: gate-scan defeat uses vanilla art, survives reload/busy UI, displays once, "
                    + "and Continue returns to the existing campaign without world mutations.");
        } finally {
            Global.setSector(priorSector);
            Global.setSettings(priorSettings);
        }
    }
}
