import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.CustomCampaignEntityPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.vector.Vector2f;

/**
 * Sanzu cloud renderer: the optimised SilentWakeFlowEntityPlugin must draw
 * exactly the same pixels as the previous build, and the LunaLib "Sanzu
 * clouds" setting must remove both cloud banks while keeping the glints and
 * planet vapor.
 *
 * Both versions render into a real offscreen OpenGL framebuffer (LWJGL
 * Pbuffer). The old class is loaded from the previous jar by a child-first
 * loader; the new class comes from the classpath. lunalib.lunaSettings.
 * LunaSettings is the test stub in tools/stubs.
 *
 * args[0] = previous jar (old SilentWakeFlowEntityPlugin).
 */
public final class SanzuCloudRenderRegression {
    static final String PLUGIN = "chiefnavigator.campaign.SilentWakeFlowEntityPlugin";
    static final int W = 512;
    static final int H = 512;
    static final float ORIGIN_X = 3000f;
    static final float ORIGIN_Y = -2000f;
    static boolean lunaEnabled = false;
    static int failures;

    static void check(boolean ok, String message) {
        if (!ok) failures++;
        System.out.println((ok ? "ok:   " : "FAIL: ") + message);
    }

    interface Call { Object invoke(String name, Object[] args); }

    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[] {type}, (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("hashCode")) return System.identityHashCode(proxy);
                    if (name.equals("equals")) return proxy == args[0];
                    if (name.equals("toString")) return type.getSimpleName() + "Mock";
                    Object result = call.invoke(name, args);
                    if (result != null) return result;
                    Class<?> r = method.getReturnType();
                    if (r == boolean.class) return false;
                    if (r == int.class) return 0;
                    if (r == float.class) return 0f;
                    if (r == long.class) return 0L;
                    if (r == double.class) return 0d;
                    return null;
                }));
    }

    /** Loads only the old plugin class from the previous jar; the rest is shared. */
    static final class ChildFirst extends ClassLoader {
        private final byte[] bytes;

        ChildFirst(ClassLoader parent, byte[] bytes) {
            super(parent);
            this.bytes = bytes;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException {
            if (!PLUGIN.equals(name)) return super.loadClass(name, resolve);
            synchronized (getClassLoadingLock(name)) {
                Class<?> c = findLoadedClass(name);
                if (c == null) c = defineClass(name, bytes, 0, bytes.length);
                if (resolve) resolveClass(c);
                return c;
            }
        }
    }

    /** Box-shaped viewport, like the campaign's. */
    static final class View implements ViewportAPI {
        final float llx;
        final float lly;
        final float w;
        final float h;
        final String name;

        View(String name, float centerX, float centerY, float halfW, float halfH) {
            this.name = name;
            this.llx = ORIGIN_X + centerX - halfW;
            this.lly = ORIGIN_Y + centerY - halfH;
            this.w = halfW * 2f;
            this.h = halfH * 2f;
        }

        public boolean isNearViewport(Vector2f p, float d) {
            return p.x > llx - d && p.x < llx + w + d
                    && p.y > lly - d && p.y < lly + h + d;
        }
        public Vector2f getCenter() { return new Vector2f(llx + w / 2f, lly + h / 2f); }
        public float getLLX() { return llx; }
        public float getLLY() { return lly; }
        public float getVisibleWidth() { return w; }
        public float getVisibleHeight() { return h; }
        public float getWorldXtoScreenX() { return W / w; }
        public float getWorldYtoScreenY() { return H / h; }
        public float getViewMult() { return w / W; }
        public float getAlphaMult() { return 1f; }
        public float convertScreenXToWorldX(float x) { return llx + x * w / W; }
        public float convertScreenYToWorldY(float y) { return lly + y * h / H; }
        public float convertWorldXtoScreenX(float x) { return (x - llx) * W / w; }
        public float convertWorldYtoScreenY(float y) { return (y - lly) * H / h; }
        public float convertWorldWidthToScreenWidth(float x) { return x * W / w; }
        public float convertWorldHeightToScreenHeight(float y) { return y * H / h; }
        public float convertScreenWidthToWorldWidth(float x) { return x * w / W; }
        public float convertScreenHeightToWorldHeight(float y) { return y * h / H; }
        public void set(float a, float b, float c, float d) { }
        public void setViewMult(float m) { }
        public boolean isExternalControl() { return false; }
        public void setExternalControl(boolean b) { }
        public void setCenter(Vector2f c) { }
        public void setAlphaMult(float a) { }
        public boolean isEverythingNearViewport() { return false; }
        public void setEverythingNearViewport(boolean b) { }
    }

    static CustomCampaignEntityPlugin create(Class<?> cls, SectorEntityToken entity)
            throws Exception {
        CustomCampaignEntityPlugin plugin = (CustomCampaignEntityPlugin)
                cls.getDeclaredConstructor().newInstance();
        plugin.init(entity, null);
        return plugin;
    }

    static byte[] draw(CustomCampaignEntityPlugin plugin, View view,
            CampaignEngineLayers layer) {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(view.llx, view.llx + view.w, view.lly, view.lly + view.h, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glClearColor(0f, 0f, 0f, 0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        plugin.render(layer, view);
        GL11.glFinish();
        ByteBuffer buffer = BufferUtils.createByteBuffer(W * H * 4);
        GL11.glReadPixels(0, 0, W, H, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        byte[] out = new byte[W * H * 4];
        buffer.get(out);
        return out;
    }

    static int differing(byte[] a, byte[] b) {
        int count = 0;
        for (int i = 0; i < a.length; i += 4) {
            if (a[i] != b[i] || a[i + 1] != b[i + 1]
                    || a[i + 2] != b[i + 2] || a[i + 3] != b[i + 3]) {
                count++;
            }
        }
        return count;
    }

    static int lit(byte[] a) {
        int count = 0;
        for (int i = 0; i < a.length; i += 4) {
            if (a[i] != 0 || a[i + 1] != 0 || a[i + 2] != 0 || a[i + 3] != 0) count++;
        }
        return count;
    }

    static long allocatedPerFrame(CustomCampaignEntityPlugin plugin, View view) {
        com.sun.management.ThreadMXBean bean =
                (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long id = Thread.currentThread().getId();
        for (int i = 0; i < 40; i++) frame(plugin, view);
        long before = bean.getThreadAllocatedBytes(id);
        int frames = 60;
        for (int i = 0; i < frames; i++) frame(plugin, view);
        return (bean.getThreadAllocatedBytes(id) - before) / frames;
    }

    static double millisPerFrame(CustomCampaignEntityPlugin plugin, View view) {
        for (int i = 0; i < 40; i++) frame(plugin, view);
        GL11.glFinish();
        int frames = 120;
        long start = System.nanoTime();
        for (int i = 0; i < frames; i++) frame(plugin, view);
        GL11.glFinish();
        return (System.nanoTime() - start) / 1e6 / frames;
    }

    static void frame(CustomCampaignEntityPlugin plugin, View view) {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(view.llx, view.llx + view.w, view.lly, view.lly + view.h, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        plugin.render(CampaignEngineLayers.TERRAIN_1, view);
        plugin.render(CampaignEngineLayers.TERRAIN_2, view);
    }

    public static void main(String[] args) throws Exception {
        byte[] oldBytes;
        try (ZipFile jar = new ZipFile(args[0])) {
            ZipEntry entry = jar.getEntry(PLUGIN.replace('.', '/') + ".class");
            try (InputStream in = jar.getInputStream(entry)) {
                oldBytes = in.readAllBytes();
            }
        }
        Class<?> newClass = Class.forName(PLUGIN);
        Class<?> oldClass = new ChildFirst(
                SanzuCloudRenderRegression.class.getClassLoader(), oldBytes)
                .loadClass(PLUGIN);
        check(newClass != oldClass && newClass.getClassLoader() != oldClass.getClassLoader(),
                "old and new renderer classes loaded side by side");

        SpriteAPI sprite = mock(SpriteAPI.class, (n, a) -> null);
        ModManagerAPI mods = mock(ModManagerAPI.class, (n, a) ->
                n.equals("isModEnabled") ? (lunaEnabled && "lunalib".equals(a[0])) : null);
        Global.setSettings(mock(SettingsAPI.class, (n, a) -> {
            if (n.equals("getSprite")) return sprite;
            if (n.equals("getModManager")) return mods;
            return null;
        }));
        Vector2f origin = new Vector2f(ORIGIN_X, ORIGIN_Y);
        SectorEntityToken entity = mock(SectorEntityToken.class, (n, a) ->
                n.equals("getLocation") ? origin : null);

        float[][] bodies = chiefnavigator.quest.OdysseyExpanseSystem.getSilentWakeObstacles();
        List<View> views = new ArrayList<>();
        views.add(new View("whole river", 0f, 0f, 12500f, 11500f));
        views.add(new View("mid zoom", 0f, 0f, 4000f, 4000f));
        views.add(new View("close", -2000f, 3000f, 1200f, 1200f));
        views.add(new View("planet 1", bodies[1][0], bodies[1][1], 1600f, 1600f));
        views.add(new View("planet 3", bodies[3][0], bodies[3][1], 2400f, 2400f));
        views.add(new View("east edge", 11000f, -6000f, 2500f, 2500f));
        views.add(new View("outside", 30000f, 0f, 2000f, 2000f));

        Pbuffer buffer = new Pbuffer(W, H, new PixelFormat().withAlphaBits(8), null, null);
        try {
            buffer.makeCurrent();
            GL11.glViewport(0, 0, W, H);

            CustomCampaignEntityPlugin oldPlugin = create(oldClass, entity);
            CustomCampaignEntityPlugin newPlugin = create(newClass, entity);
            CampaignEngineLayers[] layers = {
                CampaignEngineLayers.TERRAIN_1,
                CampaignEngineLayers.TERRAIN_2,
                CampaignEngineLayers.ABOVE_STATIONS
            };
            float[] advances = {0f, 37.3f, 91.7f, 413.9f};
            int compared = 0;
            int mismatched = 0;
            int litPixels = 0;
            for (float advance : advances) {
                oldPlugin.advance(advance);
                newPlugin.advance(advance);
                for (View view : views) {
                    for (CampaignEngineLayers layer : layers) {
                        byte[] oldPixels = draw(oldPlugin, view, layer);
                        byte[] newPixels = draw(newPlugin, view, layer);
                        int diff = differing(oldPixels, newPixels);
                        compared++;
                        litPixels += lit(newPixels);
                        if (diff != 0) {
                            mismatched++;
                            System.out.println("      differs: " + view.name + " " + layer
                                    + " after advance " + advance + ": " + diff + " pixels");
                        }
                    }
                }
            }
            check(mismatched == 0 && litPixels > 0, "pixel-identical to the previous build in all "
                    + compared + " renders (7 views x 3 layers x 4 times; "
                    + litPixels + " lit pixels compared)");

            View whole = views.get(0);
            View mid = views.get(1);
            long oldAllocWhole = allocatedPerFrame(oldPlugin, whole);
            long newAllocWhole = allocatedPerFrame(newPlugin, whole);
            long oldAllocMid = allocatedPerFrame(oldPlugin, mid);
            long newAllocMid = allocatedPerFrame(newPlugin, mid);
            System.out.printf("      allocation per frame: whole river %d -> %d bytes, mid zoom %d -> %d bytes%n",
                    oldAllocWhole, newAllocWhole, oldAllocMid, newAllocMid);
            check(newAllocWhole * 10 < oldAllocWhole && newAllocMid * 10 < oldAllocMid,
                    "garbage per frame cut by more than 90%");
            double oldMsWhole = millisPerFrame(oldPlugin, whole);
            double newMsWhole = millisPerFrame(newPlugin, whole);
            double oldMsMid = millisPerFrame(oldPlugin, mid);
            double newMsMid = millisPerFrame(newPlugin, mid);
            System.out.printf("      time per frame (both cloud layers, incl. GPU): whole river %.2f -> %.2f ms, mid zoom %.2f -> %.2f ms%n",
                    oldMsWhole, newMsWhole, oldMsMid, newMsMid);
            check(newMsWhole < oldMsWhole && newMsMid < oldMsMid, "faster in both views");

            // Setting checks use fresh renderers, so compare with a fresh old
            // one (same animation time). LunaLib absent -> on (covered above).
            CustomCampaignEntityPlugin reference = create(oldClass, entity);
            lunaEnabled = true;
            lunalib.lunaSettings.LunaSettings.booleanValue = null;
            CustomCampaignEntityPlugin unset = create(newClass, entity);
            check(differing(draw(unset, mid, CampaignEngineLayers.TERRAIN_1),
                            draw(reference, mid, CampaignEngineLayers.TERRAIN_1)) == 0,
                    "setting unset in LunaLib: clouds drawn");
            lunalib.lunaSettings.LunaSettings.throwOnRead = true;
            CustomCampaignEntityPlugin broken = create(newClass, entity);
            check(differing(draw(broken, mid, CampaignEngineLayers.TERRAIN_1),
                            draw(reference, mid, CampaignEngineLayers.TERRAIN_1)) == 0,
                    "LunaLib read fails: clouds drawn");
            lunalib.lunaSettings.LunaSettings.throwOnRead = false;

            lunalib.lunaSettings.LunaSettings.booleanValue = Boolean.FALSE;
            CustomCampaignEntityPlugin off = create(newClass, entity);
            int offBackground = 0;
            boolean fewer = true;
            boolean vaporKept = true;
            int glintViews = 0;
            for (View view : views) {
                offBackground += lit(draw(off, view, CampaignEngineLayers.TERRAIN_1));
                byte[] offMain = draw(off, view, CampaignEngineLayers.TERRAIN_2);
                byte[] onMain = draw(reference, view, CampaignEngineLayers.TERRAIN_2);
                if (lit(onMain) > 0 && lit(offMain) >= lit(onMain)) fewer = false;
                if (lit(offMain) > 0) glintViews++;
                if (differing(draw(off, view, CampaignEngineLayers.ABOVE_STATIONS),
                        draw(reference, view, CampaignEngineLayers.ABOVE_STATIONS)) != 0) {
                    vaporKept = false;
                }
            }
            check(offBackground == 0, "clouds off: background cloud layer draws nothing");
            check(fewer && glintViews > 0, "clouds off: main layer keeps only the glints ("
                    + glintViews + " views with glints)");
            check(vaporKept, "clouds off: planet vapor identical to before");

            Thread.sleep(1100);
            draw(off, mid, CampaignEngineLayers.TERRAIN_1); // re-read now: still off
            lunalib.lunaSettings.LunaSettings.booleanValue = Boolean.TRUE;
            check(lit(draw(off, mid, CampaignEngineLayers.TERRAIN_1)) == 0,
                    "turned back on: unchanged until the once-a-second re-read");
            Thread.sleep(1100);
            check(differing(draw(off, mid, CampaignEngineLayers.TERRAIN_1),
                            draw(reference, mid, CampaignEngineLayers.TERRAIN_1)) == 0,
                    "turned back on: clouds return within a second, identical");
        } finally {
            buffer.destroy();
        }
        System.out.println(failures == 0 ? "ALL PASSED" : failures + " FAILED");
        if (failures != 0) System.exit(1);
    }
}
