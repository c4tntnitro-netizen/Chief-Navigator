package chiefnavigator.campaign;

import chiefnavigator.quest.OdysseyExpanseSystem;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Vector2f;

/**
 * A deterministic north-to-south nebula river. Its potential-flow field is
 * integrated into fixed streamlines once; campaign frames only animate cloud
 * sprites along that immutable result.
 */
public final class SilentWakeFlowEntityPlugin
        extends BaseCustomEntityPlugin {
    private static final float TOP = 10500f;
    private static final float BOTTOM = -10500f;
    private static final float LEFT = -11500f;
    private static final float RIGHT = 11500f;
    private static final int PATH_COUNT = 57;
    private static final int PATH_SAMPLES = 230;
    private static final float STEP = 120f;
    private static final int VORTICES_PER_WAKE = 6;
    private static final int PARTICLES_PER_PATH = 40;
    private static final int BACKGROUND_PARTICLES_PER_PATH = 20;
    private static final float CLOUD_PLANET_PASSTHROUGH_CHANCE = 0.30f;
    private static final float LEE_LENGTH_RADII = 8f;
    private static final String FOG_VERTEX_SHADER =
            "#version 120\n"
            + "varying vec2 v_local;\n"
            + "void main() {\n"
            + "    v_local = gl_MultiTexCoord0.xy;\n"
            + "    gl_Position = ftransform();\n"
            + "}\n";
    private static final String FOG_FRAGMENT_SHADER =
            "#version 120\n"
            + "uniform float u_time;\n"
            + "uniform float u_alpha;\n"
            + "uniform float u_seed;\n"
            + "varying vec2 v_local;\n"
            + "float hash12(vec2 point) {\n"
            + "    return fract(sin(dot(point, vec2(127.1, 311.7)))"
            + " * 43758.5453123);\n"
            + "}\n"
            + "float valueNoise(vec2 point) {\n"
            + "    vec2 cell = floor(point);\n"
            + "    vec2 local = fract(point);\n"
            + "    local = local * local * (3.0 - 2.0 * local);\n"
            + "    float a = hash12(cell);\n"
            + "    float b = hash12(cell + vec2(1.0, 0.0));\n"
            + "    float c = hash12(cell + vec2(0.0, 1.0));\n"
            + "    float d = hash12(cell + vec2(1.0, 1.0));\n"
            + "    return mix(mix(a, b, local.x), mix(c, d, local.x),"
            + " local.y);\n"
            + "}\n"
            + "float fbm(vec2 point) {\n"
            + "    float value = 0.0;\n"
            + "    float amplitude = 0.5;\n"
            + "    for (int octave = 0; octave < 4; octave++) {\n"
            + "        value += amplitude * valueNoise(point);\n"
            + "        point = mat2(0.80, -0.60, 0.60, 0.80)"
            + " * point * 2.03 + 17.17;\n"
            + "        amplitude *= 0.5;\n"
            + "    }\n"
            + "    return value;\n"
            + "}\n"
            + "void main() {\n"
            + "    vec2 point = v_local;\n"
            + "    float radius = length(point);\n"
            + "    float slowA = fbm(point * 0.72"
            + " + vec2(u_seed * 1.91, u_time * 0.035));\n"
            + "    float slowB = fbm(point * 0.91"
            + " + vec2(-u_time * 0.028, u_seed * 2.71));\n"
            + "    vec2 warp = (vec2(slowA, slowB) - 0.5) * 0.62;\n"
            + "    float downstream = clamp((0.55 - point.y) / 2.5,"
            + " 0.0, 1.0);\n"
            + "    vec2 fogUV = point + warp * (0.55 + 0.65 * downstream);\n"
            + "    fogUV.y += u_time * 0.42;\n"
            + "    fogUV.x += (slowA - slowB) * 0.28 * downstream;\n"
            + "    float broad = fbm(fogUV * 1.18"
            + " + vec2(u_seed * 3.13, 0.0));\n"
            + "    float rolling = fbm(fogUV * 2.05"
            + " + vec2(-u_time * 0.055, u_time * 0.14 + u_seed));\n"
            + "    float detail = fbm(fogUV * 4.25"
            + " + vec2(u_time * 0.025, -u_time * 0.19));\n"
            + "    float mass = smoothstep(0.39, 0.72,"
            + " broad * 0.68 + rolling * 0.37);\n"
            + "    float cells = smoothstep(0.50, 0.76,"
            + " rolling * 0.72 + detail * 0.32);\n"
            + "    float density = max(mass * 0.88, cells * 0.72);\n"
            + "    float filament = smoothstep(0.70, 0.91,"
            + " detail * 0.72 + rolling * 0.38);\n"
            + "    float face = 1.0 - smoothstep(0.94, 1.015, radius);\n"
            + "    float faceFlow = face"
            + " * (0.72 + 0.28 * smoothstep(-0.72, 0.82, point.y));\n"
            + "    float rim = (1.0 - smoothstep(0.08, 0.25,"
            + " abs(radius - 0.91)))"
            + " * (1.0 - smoothstep(0.78, 1.08, abs(point.y)));\n"
            + "    float spillDepth = clamp((-point.y - 0.50) / 4.55,"
            + " 0.0, 1.0);\n"
            + "    float spillWave = (slowA - slowB)"
            + " * (0.16 + spillDepth * 0.38)"
            + " + sin(point.y * 4.7 + u_time * 0.18 + u_seed) * 0.055;\n"
            + "    float branchWidth = mix(0.48, 0.13, spillDepth);\n"
            + "    float leftCenter = mix(-0.42, -0.14, spillDepth)"
            + " + spillWave;\n"
            + "    float rightCenter = mix(0.42, 0.14, spillDepth)"
            + " - spillWave * 0.76;\n"
            + "    float middleWidth = mix(0.34, 0.075, spillDepth);\n"
            + "    float leftBranch = 1.0 - smoothstep(branchWidth * 0.52,"
            + " branchWidth, abs(point.x - leftCenter));\n"
            + "    float rightBranch = 1.0 - smoothstep(branchWidth * 0.52,"
            + " branchWidth, abs(point.x - rightCenter));\n"
            + "    float middleBranch = 1.0 - smoothstep(middleWidth * 0.45,"
            + " middleWidth, abs(point.x - spillWave * 0.42));\n"
            + "    float spillSides = max(max(leftBranch, rightBranch)"
            + " * 0.92, middleBranch * 0.62);\n"
            + "    float spillStart = smoothstep(0.48, 0.74, -point.y);\n"
            + "    float tailNoise = broad * 0.58 + rolling * 0.42;\n"
            + "    float tailDistance = -point.y"
            + " + (0.5 - tailNoise) * 0.62;\n"
            + "    float spillEnd = 1.0"
            + " - smoothstep(2.72, 5.02, tailDistance);\n"
            + "    spillEnd *= spillEnd;\n"
            + "    float spillBreakup = smoothstep(0.28, 0.66,"
            + " broad * 0.58 + rolling * 0.58);\n"
            + "    float spill = spillStart * spillEnd * spillSides"
            + " * (0.48 + spillBreakup * 0.52);\n"
            + "    vec2 capPoint = vec2(point.x / 1.16,"
            + " (point.y - 0.72) / 0.46);\n"
            + "    float capEdge = length(capPoint)"
            + " + (rolling - 0.5) * 0.20;\n"
            + "    float cap = 1.0 - smoothstep(0.68, 1.0, capEdge);\n"
            + "    float collar = (1.0 - smoothstep(1.01, 1.50, radius))"
            + " * (1.0 - face);\n"
            + "    float allowed = max(faceFlow,"
            + " max(spill, max(cap, max(rim * 0.68, collar * 0.24))));\n"
            + "    float fog = density * allowed;\n"
            + "    float alpha = u_alpha * clamp(fog * 0.49"
            + " + filament * allowed * 0.17"
            + " + rim * density * 0.12, 0.0, 0.60);\n"
            + "    vec3 baseColor = vec3(0.28, 0.47, 0.71);\n"
            + "    vec3 denseColor = vec3(0.50, 0.69, 0.89);\n"
            + "    vec3 color = mix(baseColor, denseColor,"
            + " clamp(density * 0.76 + filament * 0.18, 0.0, 1.0));\n"
            + "    if (alpha < 0.006) discard;\n"
            + "    gl_FragColor = vec4(color, alpha);\n"
            + "}\n";

    private transient float[][] pathX;
    private transient float[][] pathY;
    private transient int[] pathLengths;
    private transient float[][] planetPassthroughPathX;
    private transient float[][] planetPassthroughPathY;
    private transient int[] planetPassthroughPathLengths;
    private transient float[][] flowObstacles;
    private transient SpriteAPI flowBase;
    private transient SpriteAPI flowDetail;
    private transient int fogShaderProgram;
    private transient int fogTimeUniform;
    private transient int fogAlphaUniform;
    private transient int fogSeedUniform;
    private transient boolean fogShaderAttempted;
    private float phase;
    private float systemCloudPhase;
    private float backgroundCloudPhase;
    private float fogTime;

    @Override
    public void init(SectorEntityToken entity, Object pluginParams) {
        super.init(entity, pluginParams);
        rebuildTransientState();
    }

    private Object readResolve() {
        rebuildTransientState();
        return this;
    }

    private void rebuildTransientState() {
        flowBase = Global.getSettings().getSprite(
                "graphics/fx/slipstream_layer2.png");
        flowDetail = Global.getSettings().getSprite(
                "graphics/fx/slipstream_layer1.png");
        flowObstacles = OdysseyExpanseSystem.getSilentWakeObstacles();
        bakeStreamlines();
    }

    /**
     * Integrates uniform potential flow around several circular bodies, then
     * adds a fixed von Karman street of alternating softened vortices behind
     * each one. The result is deterministic and baked rather than simulated
     * frame by frame.
     */
    private void bakeStreamlines() {
        pathX = new float[PATH_COUNT][PATH_SAMPLES];
        pathY = new float[PATH_COUNT][PATH_SAMPLES];
        pathLengths = new int[PATH_COUNT];
        integrateStreamlines(flowObstacles, pathX, pathY, pathLengths);

        // A minority of smoke clumps are allowed to wash over the planets.
        // Their alternate baked routes still respect obstacle zero, the white
        // dwarf, so the central star never loses the readable divided flow.
        planetPassthroughPathX = new float[PATH_COUNT][PATH_SAMPLES];
        planetPassthroughPathY = new float[PATH_COUNT][PATH_SAMPLES];
        planetPassthroughPathLengths = new int[PATH_COUNT];
        float[][] starOnly = flowObstacles == null
                || flowObstacles.length == 0
                ? new float[0][]
                : new float[][] {flowObstacles[0]};
        integrateStreamlines(
                starOnly,
                planetPassthroughPathX,
                planetPassthroughPathY,
                planetPassthroughPathLengths);
    }

    private void integrateStreamlines(
            float[][] obstacles,
            float[][] outputX,
            float[][] outputY,
            int[] outputLengths) {

        for (int path = 0; path < PATH_COUNT; path++) {
            float x = LEFT + (RIGHT - LEFT) * path / (PATH_COUNT - 1f);
            float y = TOP;
            for (int sample = 0; sample < PATH_SAMPLES; sample++) {
                outputX[path][sample] = x;
                outputY[path][sample] = y;
                if (y <= BOTTOM || sample == PATH_SAMPLES - 1) {
                    outputLengths[path] = sample + 1;
                    break;
                }

                float vx = 0f;
                float vy = -1f;
                for (int index = 0; index < obstacles.length; index++) {
                    float[] body = obstacles[index];
                    float dx = x - body[0];
                    float dy = y - body[1];
                    float radius = body[2];
                    float distanceSquared = Math.max(
                            radius * radius * 1.025f,
                            dx * dx + dy * dy);
                    float inverseDistanceSquared = 1f / distanceSquared;
                    float radiusTerm = radius * radius
                            * inverseDistanceSquared;

                    // Uniform downward potential flow plus a cylinder dipole.
                    vx += 2f * radiusTerm * dx * dy
                            * inverseDistanceSquared;
                    vy += radiusTerm * (-1f + 2f * dy * dy
                            * inverseDistanceSquared);

                    float distance = (float) Math.sqrt(distanceSquared);
                    float influence = 2.15f * radius;
                    if (distance < influence) {
                        float side = dx < -8f ? -1f : dx > 8f ? 1f
                                : ((path + index) & 1) == 0 ? -1f : 1f;
                        float nearness = (influence - distance)
                                / (influence - radius);
                        vx += side * Math.max(0f, nearness) * 0.42f;
                    }

                    // Alternating counter-rotating eddies produce a widening
                    // turbulent street below each obstacle. Because the
                    // current travels north-to-south, negative Y is downstream.
                    float downstream = body[1] - y;
                    if (downstream > radius * 0.48f
                            && downstream < radius * 10.4f) {
                        for (int vortex = 0;
                                vortex < VORTICES_PER_WAKE;
                                vortex++) {
                            float vortexSide = ((vortex + index) & 1) == 0
                                    ? -1f : 1f;
                            float spread = radius * (0.70f + vortex * 0.055f);
                            float vortexX = body[0] + vortexSide * spread;
                            float vortexY = body[1]
                                    - radius * (1.38f + vortex * 1.42f);
                            float vortexDx = x - vortexX;
                            float vortexDy = y - vortexY;
                            float softening = radius * 0.34f;
                            float vortexDistanceSquared = vortexDx * vortexDx
                                    + vortexDy * vortexDy
                                    + softening * softening;
                            float vortexDistance = (float) Math.sqrt(
                                    vortexDx * vortexDx + vortexDy * vortexDy);
                            float cutoffStart = radius * 0.38f;
                            float cutoffEnd = radius * 2.35f;
                            float cutoff = 1f - smoothstep(
                                    (vortexDistance - cutoffStart)
                                            / (cutoffEnd - cutoffStart));
                            float circulation = -vortexSide * radius
                                    * (0.72f - vortex * 0.055f);
                            vx += -circulation * vortexDy
                                    / vortexDistanceSquared * cutoff;
                            vy += circulation * vortexDx
                                    / vortexDistanceSquared * cutoff;
                        }

                        // A mild alternating cross-current joins the softened
                        // cores into one recognisable shedding wake instead of
                        // six isolated circular disturbances.
                        float wakeProgress = downstream / radius;
                        float wakeHalfWidth = radius
                                * (0.72f + wakeProgress * 0.085f);
                        float acrossWake = Math.abs(dx) / wakeHalfWidth;
                        if (acrossWake < 1f) {
                            float envelope = smoothstep(1f - acrossWake)
                                    * smoothstep(Math.min(1f,
                                            (wakeProgress - 0.48f) / 0.9f));
                            vx += (float) Math.sin(
                                    wakeProgress * 2.21f + index * 1.37f)
                                    * 0.24f * envelope;
                            vy *= 1f - 0.13f * envelope;
                        }
                    }
                }

                float speed = (float) Math.sqrt(vx * vx + vy * vy);
                if (speed < 0.08f) {
                    vx = (path & 1) == 0 ? -0.18f : 0.18f;
                    vy = -0.35f;
                    speed = (float) Math.sqrt(vx * vx + vy * vy);
                }
                x += vx / speed * STEP;
                y += vy / speed * STEP;
                x = Math.max(LEFT - 700f, Math.min(RIGHT + 700f, x));
                float[] corrected = outsidePoint(obstacles, path, sample, x, y);
                x = corrected[0];
                y = corrected[1];
            }
        }
    }

    private float[] outsidePoint(
            float[][] obstacles,
            int path,
            int sample,
            float x,
            float y) {
        for (int index = 0; index < obstacles.length; index++) {
            float[] body = obstacles[index];
            float dx = x - body[0];
            float dy = y - body[1];
            float safeRadius = body[2] * 1.035f;
            float distanceSquared = dx * dx + dy * dy;
            if (distanceSquared >= safeRadius * safeRadius) continue;
            float distance = (float) Math.sqrt(Math.max(1f, distanceSquared));
            if (Math.abs(dx) < 1f) {
                dx = ((path + sample + index) & 1) == 0 ? -1f : 1f;
            }
            x = body[0] + dx / distance * safeRadius;
            y = body[1] + dy / distance * safeRadius;
        }
        return new float[] {x, y};
    }

    @Override
    public void advance(float amount) {
        phase = (phase + amount * 0.012f) % 1f;
        systemCloudPhase = (systemCloudPhase + amount * 0.006f) % 1f;
        backgroundCloudPhase = (
                backgroundCloudPhase + amount * 0.00348f) % 1f;
        fogTime += amount * 0.055f;
        if (fogTime > 4096f) fogTime -= 4096f;

    }

    /** Shared current footprint and downstream shelter geometry. */
    public static boolean isInExposedCurrent(float x, float y, float[][] obstacles) {
        if (x < LEFT || x > RIGHT || y < BOTTOM || y > TOP) return false;
        return !isInObstacleLee(x, y, obstacles);
    }

    private static boolean isInObstacleLee(float x, float y, float[][] flowObstacles) {
        if (flowObstacles == null) return false;
        for (float[] body : flowObstacles) {
            float radius = body[2];
            float downstream = body[1] - y;
            if (downstream < radius * 0.72f
                    || downstream > radius * LEE_LENGTH_RADII) {
                continue;
            }
            float wakeProgress = downstream / radius;
            float halfWidth = radius * (0.88f + wakeProgress * 0.055f);
            if (Math.abs(x - body[0]) <= halfWidth) return true;
        }
        return false;
    }

    @Override
    public float getRenderRange() {
        return 25000f;
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (pathX == null || flowBase == null) rebuildTransientState();
        float alpha = viewport.getAlphaMult();
        if (alpha <= 0f) return;
        if (layer == CampaignEngineLayers.TERRAIN_1) {
            renderBackgroundClouds(viewport, alpha);
        } else if (layer == CampaignEngineLayers.TERRAIN_2) {
            renderFlowSheet(alpha);
            renderClouds(viewport, alpha);
        } else if (layer == CampaignEngineLayers.ABOVE_STATIONS) {
            renderPlanetOverflow(viewport, alpha);
        }
    }

    /**
     * A masked FBM sheet makes the current appear to cling to each planet and
     * spill past its lower limb instead of vanishing behind the planet sprite.
     * The ellipse renderer remains as a safe fallback for unsupported GPUs.
     */
    private void renderPlanetOverflow(ViewportAPI viewport, float alpha) {
        if (renderPlanetOverflowShader(viewport, alpha)) return;
        renderPlanetOverflowFallback(viewport, alpha);
    }

    private boolean renderPlanetOverflowShader(
            ViewportAPI viewport,
            float alpha) {
        if (flowObstacles == null || !ensureFogShader()) return false;
        Vector2f origin = entity.getLocation();
        boolean attributesPushed = false;
        try {
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            attributesPushed = true;
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(
                    GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL20.glUseProgram(fogShaderProgram);
            GL20.glUniform1f(fogTimeUniform, fogTime);
            GL20.glUniform1f(fogAlphaUniform, alpha);

            // Obstacle zero is the star; render only the three planets.
            for (int bodyIndex = 1;
                    bodyIndex < flowObstacles.length;
                    bodyIndex++) {
                float[] body = flowObstacles[bodyIndex];
                float centerX = origin.x + body[0];
                float centerY = origin.y + body[1];
                float radius = body[2] - 110f;
                Vector2f center = new Vector2f(centerX, centerY);
                if (!viewport.isNearViewport(center, radius * 5.8f)) continue;
                GL20.glUniform1f(fogSeedUniform, bodyIndex * 4.731f);

                // The shader's organic masks reach zero well inside these
                // padded bounds. A single surface has no internal mesh seams.
                GL11.glBegin(GL11.GL_QUADS);
                emitFogVertex(centerX, centerY, radius, -2.10f, -5.85f);
                emitFogVertex(centerX, centerY, radius, 2.10f, -5.85f);
                emitFogVertex(centerX, centerY, radius, 2.10f, 1.85f);
                emitFogVertex(centerX, centerY, radius, -2.10f, 1.85f);
                GL11.glEnd();
            }
            return true;
        } catch (Throwable exception) {
            Global.getLogger(SilentWakeFlowEntityPlugin.class).warn(
                    "Disabling Sanzu planet-fog shader; using fallback.",
                    exception);
            if (fogShaderProgram != 0) {
                try {
                    GL20.glDeleteProgram(fogShaderProgram);
                } catch (Throwable ignored) {
                    // The OpenGL context itself may be the failure source.
                }
            }
            fogShaderProgram = 0;
            return false;
        } finally {
            try {
                GL20.glUseProgram(0);
            } catch (Throwable ignored) {
                // Preserve the non-shader fallback on unsupported hardware.
            }
            if (attributesPushed) GL11.glPopAttrib();
        }
    }

    private void emitFogVertex(
            float centerX,
            float centerY,
            float radius,
            float localX,
            float localY) {
        GL11.glTexCoord2f(localX, localY);
        GL11.glVertex2f(
                centerX + localX * radius,
                centerY + localY * radius);
    }

    private boolean ensureFogShader() {
        if (fogShaderAttempted) return fogShaderProgram != 0;
        fogShaderAttempted = true;
        int vertexShader = 0;
        int fragmentShader = 0;
        try {
            vertexShader = compileFogShader(
                    GL20.GL_VERTEX_SHADER, FOG_VERTEX_SHADER);
            fragmentShader = compileFogShader(
                    GL20.GL_FRAGMENT_SHADER, FOG_FRAGMENT_SHADER);
            fogShaderProgram = GL20.glCreateProgram();
            GL20.glAttachShader(fogShaderProgram, vertexShader);
            GL20.glAttachShader(fogShaderProgram, fragmentShader);
            GL20.glLinkProgram(fogShaderProgram);
            if (GL20.glGetProgrami(
                    fogShaderProgram, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                throw new IllegalStateException(
                        "Planet-fog shader link failed: "
                                + GL20.glGetProgramInfoLog(
                                        fogShaderProgram, 4096));
            }
            fogTimeUniform = GL20.glGetUniformLocation(
                    fogShaderProgram, "u_time");
            fogAlphaUniform = GL20.glGetUniformLocation(
                    fogShaderProgram, "u_alpha");
            fogSeedUniform = GL20.glGetUniformLocation(
                    fogShaderProgram, "u_seed");
            if (fogTimeUniform < 0
                    || fogAlphaUniform < 0
                    || fogSeedUniform < 0) {
                throw new IllegalStateException(
                        "Planet-fog shader uniforms were optimized out.");
            }
            return true;
        } catch (Throwable exception) {
            Global.getLogger(SilentWakeFlowEntityPlugin.class).warn(
                    "Unable to initialize Sanzu planet-fog shader; "
                            + "using procedural ellipse fallback.",
                    exception);
            if (fogShaderProgram != 0) {
                try {
                    GL20.glDeleteProgram(fogShaderProgram);
                } catch (Throwable ignored) {
                    // Ignore cleanup failure on unsupported hardware.
                }
            }
            fogShaderProgram = 0;
            return false;
        } finally {
            try {
                if (vertexShader != 0) GL20.glDeleteShader(vertexShader);
                if (fragmentShader != 0) GL20.glDeleteShader(fragmentShader);
            } catch (Throwable ignored) {
                // Preserve the fallback if shader-object cleanup is unsupported.
            }
        }
    }

    private int compileFogShader(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS)
                == GL11.GL_FALSE) {
            String stage = type == GL20.GL_VERTEX_SHADER
                    ? "vertex" : "fragment";
            String log = GL20.glGetShaderInfoLog(shader, 4096);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException(
                    "Planet-fog " + stage + " shader failed: " + log);
        }
        return shader;
    }

    private void renderPlanetOverflowFallback(
            ViewportAPI viewport,
            float alpha) {
        if (flowObstacles == null) return;
        Vector2f origin = entity.getLocation();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        // Obstacle zero is the star; the remaining three are fixed planets.
        for (int bodyIndex = 1;
                bodyIndex < flowObstacles.length;
                bodyIndex++) {
            float[] body = flowObstacles[bodyIndex];
            float centerX = origin.x + body[0];
            float centerY = origin.y + body[1];
            float radius = body[2] - 110f;
            Vector2f center = new Vector2f(centerX, centerY);
            if (!viewport.isNearViewport(center, radius * 2.7f)) continue;

            // Dense upstream lip and a faint veil across the visible face.
            renderMistEllipse(
                    centerX,
                    centerY + radius * 0.47f,
                    radius * 2.08f,
                    radius * 0.72f,
                    0f,
                    alpha * 0.16f,
                    0.78f,
                    0.88f,
                    1f);
            renderMistEllipse(
                    centerX,
                    centerY + radius * 0.08f,
                    radius * 1.62f,
                    radius * 1.44f,
                    0f,
                    alpha * 0.055f,
                    0.70f,
                    0.83f,
                    0.98f);

            // Low vapor sheets drift directly over the face.
            for (int wisp = 0; wisp < 9; wisp++) {
                float progress = fractional(
                        phase * 0.72f
                                + wisp / 9f
                                + bodyIndex * 0.173f);
                float fade = (float) Math.sin(Math.PI * progress);
                float curl = (float) Math.sin(
                        progress * Math.PI * 2d
                                + wisp * 1.91f
                                + bodyIndex * 0.77f);
                float widthNoise = hash01(bodyIndex, wisp, 101);
                float heightNoise = hash01(bodyIndex, wisp, 102);
                renderMistEllipse(
                        centerX + curl * radius * (0.08f + progress * 0.16f),
                        centerY + radius * (0.74f - progress * 1.48f),
                        radius * (0.48f + widthNoise * 0.46f),
                        radius * (0.20f + heightNoise * 0.20f),
                        curl * 13f,
                        alpha * fade * (0.065f + widthNoise * 0.045f),
                        0.74f,
                        0.86f,
                        1f);
            }

            // The densest fog spills around the shoulders and curls inward
            // again in the lee, like dry ice pouring over a rounded vessel.
            for (int sideIndex = 0; sideIndex < 2; sideIndex++) {
                float side = sideIndex == 0 ? -1f : 1f;
                renderMistEllipse(
                        centerX + side * radius * 0.68f,
                        centerY + radius * 0.34f,
                        radius * 0.82f,
                        radius * 0.34f,
                        side * 18f,
                        alpha * 0.14f,
                        0.80f,
                        0.90f,
                        1f);
                for (int wisp = 0; wisp < 7; wisp++) {
                    float progress = fractional(
                            phase * 0.86f
                                    + wisp / 7f
                                    + bodyIndex * 0.119f
                                    + sideIndex * 0.31f);
                    float fade = (float) Math.sin(Math.PI * progress);
                    float curl = (float) Math.sin(
                            progress * Math.PI * 2d
                                    + wisp * 2.37f
                                    + bodyIndex);
                    float spread = (float) Math.sin(Math.PI * progress);
                    float sizeNoise = hash01(
                            bodyIndex * 2 + sideIndex, wisp, 111);
                    renderMistEllipse(
                            centerX + side * radius
                                    * (0.66f + spread * 0.24f)
                                    + curl * radius * 0.07f,
                            centerY + radius * (0.52f - progress * 2.05f),
                            radius * (0.46f + sizeNoise * 0.38f),
                            radius * (0.20f + sizeNoise * 0.20f),
                            side * (18f + progress * 24f),
                            alpha * fade * (0.07f + sizeNoise * 0.055f),
                            0.71f,
                            0.84f,
                            1f);
                }
            }
        }
        GL11.glPopAttrib();
    }

    /**
     * The cloud layers now provide the continuous body of the current. Keep
     * only sparse directional glints here; repeated slipstream UVs produced
     * distracting horizontal stripes across the entire system.
     */
    private void renderFlowSheet(float alpha) {
        renderFlowHighlights(alpha);
    }

    private void renderFlowLayer(
            SpriteAPI texture,
            float alpha,
            float longitudinalScale,
            float scrollRepeats,
            float red,
            float green,
            float blue,
            float layerOpacity) {
        if (texture == null) return;
        Vector2f origin = entity.getLocation();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        texture.bindTexture();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glTexParameteri(
                GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(
                GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        float textureX = texture.getTexX();
        float textureY = texture.getTexY();
        float textureWidth = texture.getTexWidth();
        float textureHeight = texture.getTexHeight();
        float scroll = phase * scrollRepeats;
        for (int path = 0; path < PATH_COUNT - 1; path++) {
            int lengthForStrip = Math.min(
                    pathLengths[path], pathLengths[path + 1]);
            GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
            for (int sample = 0; sample < lengthForStrip; sample++) {
                float firstU = flowPotential(
                        pathX[path][sample],
                        pathY[path][sample]) / longitudinalScale - scroll;
                float secondU = flowPotential(
                        pathX[path + 1][sample],
                        pathY[path + 1][sample]) / longitudinalScale - scroll;
                renderFlowVertex(
                        origin,
                        path,
                        sample,
                        firstU,
                        path / (PATH_COUNT - 1f),
                        textureX,
                        textureY,
                        textureWidth,
                        textureHeight,
                        red,
                        green,
                        blue,
                        flowVertexAlpha(path, sample, lengthForStrip,
                                alpha * layerOpacity));
                renderFlowVertex(
                        origin,
                        path + 1,
                        sample,
                        secondU,
                        (path + 1f) / (PATH_COUNT - 1f),
                        textureX,
                        textureY,
                        textureWidth,
                        textureHeight,
                        red,
                        green,
                        blue,
                        flowVertexAlpha(path + 1, sample, lengthForStrip,
                                alpha * layerOpacity));
            }
            GL11.glEnd();
        }
        GL11.glPopAttrib();
    }

    /**
     * Velocity potential for uniform downward flow past the configured
     * circular bodies. Using it as the longitudinal texture coordinate makes
     * texture phase fronts wrap around planets instead of remaining horizontal.
     */
    private float flowPotential(float x, float y) {
        float potential = -y;
        if (flowObstacles == null) return potential;
        for (int index = 0; index < flowObstacles.length; index++) {
            float[] body = flowObstacles[index];
            float dx = x - body[0];
            float dy = y - body[1];
            float radiusSquared = body[2] * body[2];
            float distanceSquared = Math.max(
                    radiusSquared * 1.025f,
                    dx * dx + dy * dy);
            potential -= dy * radiusSquared / distanceSquared;
        }
        return potential;
    }

    private void renderFlowVertex(
            Vector2f origin,
            int path,
            int sample,
            float u,
            float v,
            float textureX,
            float textureY,
            float textureWidth,
            float textureHeight,
            float red,
            float green,
            float blue,
            float alpha) {
        GL11.glColor4f(red, green, blue, alpha);
        GL11.glTexCoord2f(
                textureX + u * textureWidth,
                textureY + v * textureHeight);
        GL11.glVertex2f(
                origin.x + pathX[path][sample],
                origin.y + pathY[path][sample]);
    }

    private float flowVertexAlpha(
            int path,
            int sample,
            int lengthForStrip,
            float alpha) {
        float across = path / (PATH_COUNT - 1f);
        float lateralFade = smoothstep(Math.min(across, 1f - across) * 10f);
        float along = sample / Math.max(1f, lengthForStrip - 1f);
        float longitudinalFade = smoothstep(
                Math.min(along, 1f - along) * 12f);
        float variation = 0.82f + 0.18f * (float) Math.sin(
                path * 0.63f + sample * 0.071f);
        return alpha * lateralFade * longitudinalFade * variation;
    }

    /** Sparse moving glints provide direction without recreating solid bands. */
    private void renderFlowHighlights(float alpha) {
        Vector2f origin = entity.getLocation();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glLineWidth(1.5f);
        for (int path = 4; path < PATH_COUNT; path += 8) {
            for (int segment = 0; segment < 3; segment++) {
                float start = fractional(
                        phase + segment / 3f + path * 0.071f);
                GL11.glBegin(GL11.GL_LINE_STRIP);
                for (int step = 0; step <= 16; step++) {
                    float progress = start + step * 0.0065f;
                    if (progress >= 1f) break;
                    float fade = (float) Math.sin(Math.PI * step / 16f);
                    Vector2f point = samplePath(path, progress);
                    GL11.glColor4f(
                            0.42f,
                            0.72f,
                            1f,
                            alpha * 0.28f * fade);
                    GL11.glVertex2f(
                            origin.x + point.x,
                            origin.y + point.y);
                }
                GL11.glEnd();
            }
        }
        GL11.glPopAttrib();
    }

    /**
     * A slower, darker bank beneath the stream sheet supplies atmospheric
     * depth without flattening the brighter foreground smoke into one layer.
     */
    private void renderBackgroundClouds(ViewportAPI viewport, float alpha) {
        Vector2f origin = entity.getLocation();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        for (int path = 0; path < PATH_COUNT; path++) {
            for (int particleIndex = 0;
                    particleIndex < BACKGROUND_PARTICLES_PER_PATH;
                    particleIndex++) {
                float progress = fractional(
                        backgroundCloudPhase
                                + hash01(path, particleIndex, 101));
                boolean passesThroughPlanets = hash01(
                        path, particleIndex, 200)
                        < CLOUD_PLANET_PASSTHROUGH_CHANCE;
                Vector2f point = samplePath(
                        path, progress, passesThroughPlanets);
                Vector2f before = samplePath(
                        path,
                        Math.max(0f, progress - 0.011f),
                        passesThroughPlanets);
                Vector2f after = samplePath(
                        path,
                        Math.min(0.999f, progress + 0.011f),
                        passesThroughPlanets);
                float tangentX = after.x - before.x;
                float tangentY = after.y - before.y;
                float tangentLength = (float) Math.sqrt(
                        tangentX * tangentX + tangentY * tangentY);
                if (tangentLength < 1f) tangentLength = 1f;
                float lateralOffset = (hash01(path, particleIndex, 102) - 0.5f)
                        * (740f + hash01(path, particleIndex, 103) * 980f);
                point.x += -tangentY / tangentLength * lateralOffset;
                point.y += tangentX / tangentLength * lateralOffset;
                point.x += origin.x;
                point.y += origin.y;

                float sizeRoll = hash01(path, particleIndex, 104);
                float size = 520f + sizeRoll * sizeRoll * 980f;
                float width = size
                        * (1.00f + hash01(path, particleIndex, 105) * 1.16f);
                float height = size
                        * (0.80f + hash01(path, particleIndex, 106) * 0.62f);
                if (!viewport.isNearViewport(point, Math.max(width, height))) {
                    continue;
                }

                float angle = hash01(path, particleIndex, 107) * 360f
                        + backgroundCloudPhase * 31f;
                float opacity = 0.58f
                        + hash01(path, particleIndex, 108) * 0.72f;
                renderBackgroundMistPuff(
                        point,
                        width,
                        height,
                        angle,
                        alpha * opacity,
                        path,
                        particleIndex);
            }
        }
        GL11.glPopAttrib();
    }

    private void renderClouds(ViewportAPI viewport, float alpha) {
        Vector2f origin = entity.getLocation();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        for (int path = 0; path < PATH_COUNT; path++) {
            for (int particleIndex = 0;
                    particleIndex < PARTICLES_PER_PATH;
                    particleIndex++) {
                // Hash the longitudinal position directly instead of placing
                // one puff in every equal-size slot. This creates stable,
                // naturally uneven banks and gaps without a dot lattice.
                float progress = fractional(
                        systemCloudPhase + hash01(path, particleIndex, 1));
                boolean passesThroughPlanets = hash01(
                        path, particleIndex, 201)
                        < CLOUD_PLANET_PASSTHROUGH_CHANCE;
                Vector2f point = samplePath(
                        path, progress, passesThroughPlanets);
                Vector2f before = samplePath(
                        path,
                        Math.max(0f, progress - 0.008f),
                        passesThroughPlanets);
                Vector2f after = samplePath(
                        path,
                        Math.min(0.999f, progress + 0.008f),
                        passesThroughPlanets);
                float tangentX = after.x - before.x;
                float tangentY = after.y - before.y;
                float tangentLength = (float) Math.sqrt(
                        tangentX * tangentX + tangentY * tangentY);
                if (tangentLength < 1f) tangentLength = 1f;
                float lateralOffset = (hash01(path, particleIndex, 2) - 0.5f)
                        * (420f + hash01(path, particleIndex, 8) * 720f);
                point.x += -tangentY / tangentLength * lateralOffset;
                point.y += tangentX / tangentLength * lateralOffset;
                point.x += origin.x;
                point.y += origin.y;
                float sizeRoll = hash01(path, particleIndex, 3);
                float size = 360f + sizeRoll * sizeRoll * 640f;
                float width = size
                        * (1.00f + hash01(path, particleIndex, 4) * 1.08f);
                float height = size
                        * (0.78f + hash01(path, particleIndex, 5) * 0.62f);
                if (!viewport.isNearViewport(point, Math.max(width, height))) {
                    continue;
                }

                float angle = hash01(path, particleIndex, 6) * 360f
                        + systemCloudPhase * 42f;
                float opacityRoll = hash01(path, particleIndex, 7);
                float opacityScale = 0.34f + opacityRoll * opacityRoll * 1.08f;
                renderMistPuff(
                        point,
                        width,
                        height,
                        angle,
                        alpha * opacityScale,
                        path,
                        particleIndex);
            }
        }
        GL11.glPopAttrib();
    }

    /**
     * Builds each cloud from overlapping radial gradients. This intentionally
     * uses no cloud sprite or atlas, eliminating cracked source-tile patterns.
     */
    private void renderMistPuff(
            Vector2f center,
            float width,
            float height,
            float angle,
            float alpha,
            int path,
            int particleIndex) {
        // A large faint skirt bridges neighboring clumps into continuous banks.
        renderMistEllipse(
                center.x,
                center.y,
                width * 1.52f,
                height * 1.46f,
                angle,
                0.045f * alpha,
                0.29f,
                0.48f,
                0.72f);
        renderMistEllipse(
                center.x,
                center.y,
                width,
                height,
                angle,
                0.12f * alpha,
                0.38f,
                0.59f,
                0.83f);

        // Uneven satellite lobes make an irregular smoke mass rather than a
        // repeated circular spot. Their layout is deterministic across saves.
        int lobeCount = 3 + (int) (hash01(path, particleIndex, 9) * 5f);
        for (int lobe = 0; lobe < lobeCount; lobe++) {
            float direction = hash01(path, particleIndex, 10 + lobe * 4)
                    * (float) (Math.PI * 2d);
            float distance = 0.08f
                    + hash01(path, particleIndex, 11 + lobe * 4) * 0.24f;
            float offsetX = (float) Math.cos(direction) * width * distance;
            float offsetY = (float) Math.sin(direction) * height * distance;
            float scale = 0.28f
                    + hash01(path, particleIndex, 12 + lobe * 4) * 0.52f;
            float lobeAlpha = 0.045f
                    + hash01(path, particleIndex, 13 + lobe * 4) * 0.075f;
            renderMistEllipse(
                    center.x + offsetX,
                    center.y + offsetY,
                    width * scale,
                    height * scale
                            * (0.72f + hash01(path, particleIndex, 30 + lobe)
                                    * 0.56f),
                    angle + hash01(path, particleIndex, 40 + lobe) * 180f,
                    lobeAlpha * alpha,
                    0.31f + lobe * 0.025f,
                    0.51f + lobe * 0.02f,
                    0.78f + lobe * 0.025f);
        }
    }

    /** Dark, broad clumps rendered before the textured current and main mist. */
    private void renderBackgroundMistPuff(
            Vector2f center,
            float width,
            float height,
            float angle,
            float alpha,
            int path,
            int particleIndex) {
        renderMistEllipse(
                center.x,
                center.y,
                width * 1.68f,
                height * 1.58f,
                angle,
                0.075f * alpha,
                0.065f,
                0.16f,
                0.29f);
        renderMistEllipse(
                center.x,
                center.y,
                width,
                height,
                angle,
                0.16f * alpha,
                0.09f,
                0.23f,
                0.40f);

        int lobeCount = 3 + (int) (hash01(path, particleIndex, 109) * 4f);
        for (int lobe = 0; lobe < lobeCount; lobe++) {
            float direction = hash01(path, particleIndex, 110 + lobe * 4)
                    * (float) (Math.PI * 2d);
            float distance = 0.06f
                    + hash01(path, particleIndex, 111 + lobe * 4) * 0.28f;
            float scale = 0.34f
                    + hash01(path, particleIndex, 112 + lobe * 4) * 0.58f;
            float offsetX = (float) Math.cos(direction) * width * distance;
            float offsetY = (float) Math.sin(direction) * height * distance;
            renderMistEllipse(
                    center.x + offsetX,
                    center.y + offsetY,
                    width * scale,
                    height * scale
                            * (0.72f + hash01(
                                    path, particleIndex, 150 + lobe) * 0.62f),
                    angle + hash01(path, particleIndex, 160 + lobe) * 180f,
                    alpha * (0.055f + hash01(
                            path, particleIndex, 113 + lobe * 4) * 0.075f),
                    0.075f + lobe * 0.008f,
                    0.18f + lobe * 0.012f,
                    0.32f + lobe * 0.014f);
        }
    }

    private void renderMistEllipse(
            float centerX,
            float centerY,
            float width,
            float height,
            float angle,
            float alpha,
            float red,
            float green,
            float blue) {
        float radians = (float) Math.toRadians(angle);
        float cosRotation = (float) Math.cos(radians);
        float sinRotation = (float) Math.sin(radians);
        float radiusX = width * 0.5f;
        float radiusY = height * 0.5f;

        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glColor4f(red, green, blue, alpha);
        GL11.glVertex2f(centerX, centerY);
        for (int step = 0; step <= 24; step++) {
            float arc = (float) (Math.PI * 2d * step / 24d);
            float localX = (float) Math.cos(arc) * radiusX;
            float localY = (float) Math.sin(arc) * radiusY;
            GL11.glColor4f(red, green, blue, 0f);
            GL11.glVertex2f(
                    centerX + localX * cosRotation
                            - localY * sinRotation,
                    centerY + localX * sinRotation
                            + localY * cosRotation);
        }
        GL11.glEnd();
    }

    private Vector2f samplePath(int path, float progress) {
        return samplePath(path, progress, false);
    }

    private Vector2f samplePath(
            int path,
            float progress,
            boolean passesThroughPlanets) {
        float[][] sourceX = passesThroughPlanets
                ? planetPassthroughPathX : pathX;
        float[][] sourceY = passesThroughPlanets
                ? planetPassthroughPathY : pathY;
        int[] sourceLengths = passesThroughPlanets
                ? planetPassthroughPathLengths : pathLengths;
        int lengthForPath = Math.max(2, sourceLengths[path]);
        float exact = progress * (lengthForPath - 1f);
        int lower = Math.min(lengthForPath - 2, (int) exact);
        float fraction = exact - lower;
        return new Vector2f(
                sourceX[path][lower]
                        + (sourceX[path][lower + 1] - sourceX[path][lower])
                        * fraction,
                sourceY[path][lower]
                        + (sourceY[path][lower + 1] - sourceY[path][lower])
                        * fraction);
    }

    private float fractional(float value) {
        return value - (float) Math.floor(value);
    }

    private float smoothstep(float value) {
        float clamped = Math.max(0f, Math.min(1f, value));
        return clamped * clamped * (3f - 2f * clamped);
    }

    /** Stable integer hash for stochastic-looking but save-safe mist layout. */
    private float hash01(int path, int particleIndex, int channel) {
        int value = path * 0x1f1f1f1f
                ^ particleIndex * 0x6d2b79f5
                ^ channel * 0x9e3779b9;
        value ^= value >>> 16;
        value *= 0x7feb352d;
        value ^= value >>> 15;
        value *= 0x846ca68b;
        value ^= value >>> 16;
        return (value & 0x7fffffff) / 2147483647f;
    }
}
