import chiefnavigator.weapons.AbyssalBeamGradientRenderer;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.BeamWeaponSpecAPI;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.vector.Vector2f;

/** Offscreen framebuffer check of the actual textured fringe, without a game or visible window. */
public final class AbyssalBeamGradientRenderRegression {
    private static final int WIDTH = 1280, HEIGHT = 300;
    private interface Call { Object invoke(String name, Object[] args); }

    private static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (proxy, method, args) -> {
                    Object value = call.invoke(method.getName(), args);
                    if (value != null) return value;
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == float.class) return 0f;
                    if (method.getReturnType() == int.class) return 0;
                    return null;
                }));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static int texture(Path path) throws Exception {
        BufferedImage image = ImageIO.read(path.toFile());
        ByteBuffer rgba = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                rgba.put((byte) (argb >> 16)).put((byte) (argb >> 8))
                        .put((byte) argb).put((byte) (argb >> 24));
            }
        }
        rgba.flip();
        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, image.getWidth(), image.getHeight(),
                0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, rgba);
        return texture;
    }

    private static BufferedImage capture() {
        ByteBuffer rgba = BufferUtils.createByteBuffer(WIDTH * HEIGHT * 4);
        GL11.glFinish();
        GL11.glReadPixels(0, 0, WIDTH, HEIGHT, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, rgba);
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int offset = (y * WIDTH + x) * 4;
                int rgb = (rgba.get(offset) & 255) << 16 | (rgba.get(offset + 1) & 255) << 8
                        | (rgba.get(offset + 2) & 255);
                image.setRGB(x, HEIGHT - 1 - y, rgb);
            }
        }
        return image;
    }

    private static double[] sample(BufferedImage image, int centerX) {
        double[] channels = new double[3];
        for (int x = centerX - 12; x <= centerX + 12; x++) {
            for (int y = 120; y <= 180; y++) {
                Color color = new Color(image.getRGB(x, y));
                channels[0] += color.getRed();
                channels[1] += color.getGreen();
                channels[2] += color.getBlue();
            }
        }
        return channels;
    }

    public static void main(String[] args) throws Exception {
        Path core = Path.of(args.length > 1 ? args[1] : "../../starsector-core").toAbsolutePath().normalize();
        Path output = Path.of(args[0]).toAbsolutePath();
        Files.createDirectories(output);
        SettingsAPI oldSettings = Global.getSettings();
        Pbuffer buffer = new Pbuffer(WIDTH, HEIGHT, new PixelFormat().withAlphaBits(8), null, null);
        try {
            buffer.makeCurrent();
            GL11.glViewport(0, 0, WIDTH, HEIGHT);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            GL11.glOrtho(0, WIDTH, 0, HEIGHT, -1, 1);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
            int texture = texture(core.resolve("graphics/fx/beamfringe.png"));
            SpriteAPI sprite = mock(SpriteAPI.class, (name, values) -> {
                if (name.equals("bindTexture")) GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
                return null;
            });
            Global.setSettings(mock(SettingsAPI.class,
                    (name, values) -> name.equals("getSprite") ? sprite : null));
            ShipHullSpecAPI hull = mock(ShipHullSpecAPI.class,
                    (name, values) -> name.equals("getHullId") ? "chief_navigator_ithaca_intact_base" : null);
            Map<String, Object> data = new HashMap<>();
            data.put("chief_navigator_ithaca_research_component_mask", 64);
            ShipAPI source = mock(ShipAPI.class, (name, values) -> {
                if (name.equals("getHullSpec")) return hull;
                if (name.equals("getCustomData")) return data;
                return null;
            });
            BeamWeaponSpecAPI spec = mock(BeamWeaponSpecAPI.class, (name, values) -> {
                // The real ROUGH enum style leaves the explicit fringe path null.
                if (name.equals("getTextureScrollSpeed")) return 520f;
                if (name.equals("getFringeScrollSpeedMult")) return 1f;
                return null;
            });
            WeaponAPI weapon = mock(WeaponAPI.class, (name, values) -> {
                if (name.equals("getSpec")) return spec;
                if (name.equals("getId")) return "chief_navigator_drifting_wall_colossal_inert";
                return null;
            });
            Vector2f from = new Vector2f(100f, 150f), to = new Vector2f(1100f, 150f);
            float[] brightness = {1f};
            BeamAPI beam = mock(BeamAPI.class, (name, values) -> {
                if (name.equals("getFrom")) return from;
                if (name.equals("getTo")) return to;
                if (name.equals("getSource")) return source;
                if (name.equals("getWeapon")) return weapon;
                if (name.equals("getBrightness")) return brightness[0];
                if (name.equals("getWidth")) return 90f;
                if (name.equals("getPixelsPerTexel")) return 5f;
                return null;
            });
            CombatEngineAPI engine = mock(CombatEngineAPI.class, (name, values) -> {
                if (name.equals("getBeams")) return List.of(beam);
                if (name.equals("addLayeredRenderingPlugin")) {
                    Vector2f position = new Vector2f();
                    return mock(CombatEntityAPI.class,
                            (call, ignored) -> call.equals("getLocation") ? position : null);
                }
                return null;
            });
            ViewportAPI viewport = mock(ViewportAPI.class, (name, values) -> null);
            AbyssalBeamGradientRenderer renderer = AbyssalBeamGradientRenderer.install(engine, beam);
            check(renderer != null, "The actual renderer must install with the native fringe texture");
            GL11.glClearColor(0f, 0f, 0f, 1f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            renderer.render(CombatEngineLayers.ABOVE_SHIPS_LAYER, viewport);
            BufferedImage full = capture();
            double[] start = sample(full, 200), halfway = sample(full, 650), end = sample(full, 1000);
            check(start[1] > start[0] * 2 && start[2] > start[0] * 2,
                    "Actual framebuffer must show teal at the start");
            check(halfway[0] > halfway[1] * 3 && halfway[0] > halfway[2] * 3
                            && end[0] > end[1] * 3 && end[0] > end[2] * 3,
                    "Actual framebuffer must be red by halfway and remain red toward the impact");
            ImageIO.write(full, "png", output.resolve("abyssal-gradient-fringe.png").toFile());
            brightness[0] = 0.5f;
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            renderer.render(CombatEngineLayers.ABOVE_SHIPS_LAYER, viewport);
            BufferedImage fading = capture();
            double[] dim = sample(fading, 650);
            check(dim[0] > 0 && dim[0] < halfway[0] * 0.3,
                    "The fringe must retain the native combined width/alpha fade");
            ImageIO.write(fading, "png", output.resolve("abyssal-gradient-fade.png").toFile());
            renderer.advanceTexture(0.25f);
            brightness[0] = 1f;
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            renderer.render(CombatEngineLayers.ABOVE_SHIPS_LAYER, viewport);
            BufferedImage scrolling = capture();
            check(scrolling.getRGB(650, 150) != full.getRGB(650, 150)
                            || scrolling.getRGB(200, 150) != full.getRGB(200, 150),
                    "Native fringe texture must scroll while the color transition stays in place");
            check(GL11.glGetError() == GL11.GL_NO_ERROR, "Renderer must leave no OpenGL error");
            GL11.glDeleteTextures(texture);
            System.out.println("PASS: actual offscreen GL fringe is teal at source, red by midpoint, "
                    + "with native texture scrolling and brightness fade; previews in " + output);
        } finally {
            Global.setSettings(oldSettings);
            buffer.destroy();
        }
    }
}
