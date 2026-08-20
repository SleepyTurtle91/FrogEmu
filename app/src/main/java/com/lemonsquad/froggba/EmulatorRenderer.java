package com.lemonsquad.froggba;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Pure renderer — reads the published display buffer and draws it.
 * Does NOT call runFrame or touch the mGBA core in any way.
 */
public class EmulatorRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "FrogEmu_Render";

    public enum ScalingMode {
        FIT_3_2("Aspect Fit (3:2 Aspect Ratio)"),
        INTEGER_MAX("Integer Scaling (6× Pixel Perfect - 1440×960)"),
        INTEGER_5X("Integer Scaling (5× - 1200×800)"),
        INTEGER_4X("Integer Scaling (4× - 960×640)"),
        FULLSCREEN("Stretch (16:9 Full Screen)");

        private final String label;
        ScalingMode(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public enum Upscaler {
        NEAREST("Pixel-Perfect (Crisp Native 1:1 Color)"),
        LCD_GRID("Authentic GBA LCD Sub-Pixel Grid"),
        SCANLINES("Handheld / CRT Scanlines"),
        SCALE2X("Clean EPX / Scale2x Pixel Expansion"),
        BILINEAR("Bilinear (Smooth Edge Interpolation)");

        private final String label;
        Upscaler(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private volatile ByteBuffer mFrameBuffer;

    private int mProgram;
    private int mPositionHandle;
    private int mTexCoordHandle;
    private int mTextureId;

    private final FloatBuffer mVertices;
    private final FloatBuffer mTexCoords;

    private ScalingMode mScalingMode = ScalingMode.FIT_3_2;
    private Upscaler mCurrentUpscaler = Upscaler.NEAREST;
    private volatile boolean mUpscalerChanged = false;
    private volatile boolean mViewportChanged = false;

    private int mSurfaceWidth = 1920;
    private int mSurfaceHeight = 1080;

    private static final float[] QUAD_COORDS = {
        -1.0f,  1.0f,
        -1.0f, -1.0f,
         1.0f,  1.0f,
         1.0f, -1.0f
    };

    private static final float[] TEX_COORDS = {
         0.0f, 0.0f,
         0.0f, 1.0f,
         1.0f, 0.0f,
         1.0f, 1.0f
    };

    // ── Shaders ─────────────────────────────────────────────────────

    private static final String VERTEX_SHADER =
        "attribute vec4 aPosition;\n" +
        "attribute vec2 aTexCoord;\n" +
        "varying vec2 vTexCoord;\n" +
        "void main() {\n" +
        "  gl_Position = aPosition;\n" +
        "  vTexCoord = aTexCoord;\n" +
        "}\n";

    // Strict 1:1 Pixel-Perfect Nearest
    private static final String FRAGMENT_NEAREST =
        "precision mediump float;\n" +
        "uniform sampler2D uTexture;\n" +
        "varying vec2 vTexCoord;\n" +
        "void main() {\n" +
        "  gl_FragColor = texture2D(uTexture, vTexCoord);\n" +
        "}\n";

    // Authentic GBA LCD Sub-Pixel Grid
    private static final String FRAGMENT_LCD_GRID =
        "precision mediump float;\n" +
        "uniform sampler2D uTexture;\n" +
        "varying vec2 vTexCoord;\n" +
        "const vec2 texSize = vec2(240.0, 160.0);\n" +
        "void main() {\n" +
        "  vec4 color = texture2D(uTexture, vTexCoord);\n" +
        "  vec2 grid = fract(vTexCoord * texSize);\n" +
        "  float border = step(0.92, grid.x) + step(0.92, grid.y);\n" +
        "  color.rgb *= (1.0 - 0.20 * min(border, 1.0));\n" +
        "  gl_FragColor = color;\n" +
        "}\n";

    // Scanlines
    private static final String FRAGMENT_SCANLINES =
        "precision mediump float;\n" +
        "uniform sampler2D uTexture;\n" +
        "varying vec2 vTexCoord;\n" +
        "const vec2 texSize = vec2(240.0, 160.0);\n" +
        "void main() {\n" +
        "  vec4 color = texture2D(uTexture, vTexCoord);\n" +
        "  float line = sin(vTexCoord.y * texSize.y * 3.14159265);\n" +
        "  color.rgb *= (0.88 + 0.12 * line * line);\n" +
        "  gl_FragColor = color;\n" +
        "}\n";

    // Clean EPX / Scale2x without fuzzy color-smearing
    private static final String FRAGMENT_SCALE2X =
        "precision mediump float;\n" +
        "uniform sampler2D uTexture;\n" +
        "varying vec2 vTexCoord;\n" +
        "const vec2 texSize = vec2(240.0, 160.0);\n" +
        "void main() {\n" +
        "  vec2 ps = 1.0 / texSize;\n" +
        "  vec2 p = vTexCoord * texSize;\n" +
        "  vec2 p_floor = floor(p);\n" +
        "  vec2 p_fract = fract(p);\n" +
        "  vec2 pc = (p_floor + vec2(0.5, 0.5)) * ps;\n" +
        "  vec4 B = texture2D(uTexture, pc + vec2(0.0, -ps.y));\n" +
        "  vec4 D = texture2D(uTexture, pc + vec2(-ps.x, 0.0));\n" +
        "  vec4 E = texture2D(uTexture, pc);\n" +
        "  vec4 F = texture2D(uTexture, pc + vec2(ps.x, 0.0));\n" +
        "  vec4 H = texture2D(uTexture, pc + vec2(0.0, ps.y));\n" +
        "  vec4 outColor = E;\n" +
        "  if (B != H && D != F) {\n" +
        "      if (p_fract.x < 0.5 && p_fract.y < 0.5) {\n" +
        "          if (D == B) outColor = D;\n" +
        "      } else if (p_fract.x >= 0.5 && p_fract.y < 0.5) {\n" +
        "          if (B == F) outColor = F;\n" +
        "      } else if (p_fract.x < 0.5 && p_fract.y >= 0.5) {\n" +
        "          if (D == H) outColor = D;\n" +
        "      } else {\n" +
        "          if (H == F) outColor = F;\n" +
        "      }\n" +
        "  }\n" +
        "  gl_FragColor = outColor;\n" +
        "}\n";

    // ── Constructor ─────────────────────────────────────────────────

    public EmulatorRenderer() {
        mVertices = ByteBuffer.allocateDirect(QUAD_COORDS.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertices.put(QUAD_COORDS).position(0);

        mTexCoords = ByteBuffer.allocateDirect(TEX_COORDS.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTexCoords.put(TEX_COORDS).position(0);
    }

    // ── Public API ──────────────────────────────────────────────────

    public void setFrameBuffer(ByteBuffer buffer) {
        mFrameBuffer = buffer;
    }

    public void setUpscaler(Upscaler upscaler) {
        if (mCurrentUpscaler != upscaler) {
            mCurrentUpscaler = upscaler;
            mUpscalerChanged = true;
        }
    }

    public Upscaler getCurrentUpscaler() { return mCurrentUpscaler; }

    public void setScalingMode(ScalingMode mode) {
        if (mScalingMode != mode) {
            mScalingMode = mode;
            mViewportChanged = true;
        }
    }

    public ScalingMode getScalingMode() { return mScalingMode; }

    // ── GLSurfaceView.Renderer ──────────────────────────────────────

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        compileCurrentProgram();

        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mTextureId = textures[0];

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
        updateTextureFiltering();

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                240, 160, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        applyViewport();
    }

    private void applyViewport() {
        int width = mSurfaceWidth;
        int height = mSurfaceHeight;
        int vw, vh, vx, vy;

        switch (mScalingMode) {
            case INTEGER_MAX: {
                // Maximum whole integer multiplier that fits screen
                int scale = Math.max(1, Math.min(width / 240, height / 160));
                vw = 240 * scale;
                vh = 160 * scale;
                vx = (width - vw) / 2;
                vy = (height - vh) / 2;
                break;
            }
            case INTEGER_5X: {
                vw = 240 * 5; // 1200
                vh = 160 * 5; // 800
                vx = (width - vw) / 2;
                vy = (height - vh) / 2;
                break;
            }
            case INTEGER_4X: {
                vw = 240 * 4; // 960
                vh = 160 * 4; // 640
                vx = (width - vw) / 2;
                vy = (height - vh) / 2;
                break;
            }
            case FULLSCREEN: {
                vw = width;
                vh = height;
                vx = 0;
                vy = 0;
                break;
            }
            case FIT_3_2:
            default: {
                float targetRatio = 240.0f / 160.0f; // 3:2
                float screenRatio = (float) width / height;
                if (screenRatio > targetRatio) {
                    vh = height;
                    vw = (int) (height * targetRatio);
                    vx = (width - vw) / 2;
                    vy = 0;
                } else {
                    vw = width;
                    vh = (int) (width / targetRatio);
                    vx = 0;
                    vy = (height - vh) / 2;
                }
                break;
            }
        }

        GLES20.glViewport(vx, vy, vw, vh);
        Log.i(TAG, String.format("Viewport applied: mode=%s, rect=[%d, %d, %d, %d]", mScalingMode, vx, vy, vw, vh));
    }

    private void updateTextureFiltering() {
        if (mTextureId == 0) return;
        int filter = (mCurrentUpscaler == Upscaler.BILINEAR) ? GLES20.GL_LINEAR : GLES20.GL_NEAREST;
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, filter);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, filter);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mUpscalerChanged) {
            compileCurrentProgram();
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
            updateTextureFiltering();
            mUpscalerChanged = false;
        }

        if (mViewportChanged) {
            applyViewport();
            mViewportChanged = false;
        }

        ByteBuffer fb = mFrameBuffer;
        if (fb == null) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            return;
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
        fb.position(0);
        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0,
                240, 160, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, fb);

        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, 2,
                GLES20.GL_FLOAT, false, 0, mVertices);

        GLES20.glEnableVertexAttribArray(mTexCoordHandle);
        GLES20.glVertexAttribPointer(mTexCoordHandle, 2,
                GLES20.GL_FLOAT, false, 0, mTexCoords);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTexCoordHandle);
    }

    // ── Shader helpers ──────────────────────────────────────────────

    private void compileCurrentProgram() {
        String frag;
        switch (mCurrentUpscaler) {
            case LCD_GRID:  frag = FRAGMENT_LCD_GRID;  break;
            case SCANLINES: frag = FRAGMENT_SCANLINES; break;
            case SCALE2X:   frag = FRAGMENT_SCALE2X;   break;
            case BILINEAR:  frag = FRAGMENT_NEAREST;   break; // uses GL_LINEAR
            case NEAREST:
            default:        frag = FRAGMENT_NEAREST;   break;
        }

        if (mProgram != 0) GLES20.glDeleteProgram(mProgram);

        mProgram = createProgram(VERTEX_SHADER, frag);
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        mTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "aTexCoord");
    }

    private int loadShader(int type, String code) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);

        int[] status = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            String log = GLES20.glGetShaderInfoLog(shader);
            Log.e(TAG, "Shader compile failed: " + log);
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private int createProgram(String vertSrc, String fragSrc) {
        int vs = loadShader(GLES20.GL_VERTEX_SHADER, vertSrc);
        int fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragSrc);
        if (vs == 0 || fs == 0) {
            Log.e(TAG, "Shader compilation failed — falling back to nearest.");
            if (vs != 0) GLES20.glDeleteShader(vs);
            if (fs != 0) GLES20.glDeleteShader(fs);
            vs = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
            fs = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_NEAREST);
        }
        int prog = GLES20.glCreateProgram();
        GLES20.glAttachShader(prog, vs);
        GLES20.glAttachShader(prog, fs);
        GLES20.glLinkProgram(prog);

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Program link failed: " + GLES20.glGetProgramInfoLog(prog));
            GLES20.glDeleteProgram(prog);
            return 0;
        }
        return prog;
    }
}
