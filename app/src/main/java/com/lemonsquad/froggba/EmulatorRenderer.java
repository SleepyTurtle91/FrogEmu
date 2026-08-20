package com.lemonsquad.froggba;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class EmulatorRenderer implements GLSurfaceView.Renderer {

    public enum Upscaler {
        NEAREST,
        SCALE2X,
        HQ2X,
        XBRZ
    }

    private final MainActivity mActivity;
    private ByteBuffer mFrameBuffer;
    
    private int mProgram;
    private int mPositionHandle;
    private int mTexCoordHandle;
    private int mTextureId;
    
    private final FloatBuffer mVertices;
    private final FloatBuffer mTexCoords;
    
    private Upscaler mCurrentUpscaler = Upscaler.NEAREST;
    private boolean mUpscalerChanged = false;
    
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
    
    private final String VERTEX_SHADER =
        "attribute vec4 aPosition;\n" +
        "attribute vec2 aTexCoord;\n" +
        "varying vec2 vTexCoord;\n" +
        "void main() {\n" +
        "  gl_Position = aPosition;\n" +
        "  vTexCoord = aTexCoord;\n" +
        "}\n";
        
    private final String FRAGMENT_SHADER_NEAREST =
        "precision mediump float;\n" +
        "uniform sampler2D uTexture;\n" +
        "varying vec2 vTexCoord;\n" +
        "void main() {\n" +
        "  gl_FragColor = texture2D(uTexture, vTexCoord);\n" +
        "}\n";

    // Placeholder for Scale2x (currently acts as basic bilinear for demonstration of shader swap)
    private final String FRAGMENT_SHADER_SCALE2X =
        "precision mediump float;\n" +
        "uniform sampler2D uTexture;\n" +
        "varying vec2 vTexCoord;\n" +
        "void main() {\n" +
        "  // TODO: Implement actual Scale2x logic\n" +
        "  gl_FragColor = texture2D(uTexture, vTexCoord);\n" +
        "}\n";

    private final String FRAGMENT_SHADER_HQ2X =
        "precision mediump float;\n" +
        "uniform sampler2D uTexture;\n" +
        "varying vec2 vTexCoord;\n" +
        "void main() {\n" +
        "  // TODO: Implement actual HQ2x logic\n" +
        "  gl_FragColor = texture2D(uTexture, vTexCoord);\n" +
        "}\n";

    private final String FRAGMENT_SHADER_XBRZ =
        "precision mediump float;\n" +
        "uniform sampler2D uTexture;\n" +
        "varying vec2 vTexCoord;\n" +
        "void main() {\n" +
        "  // TODO: Implement actual xBRZ logic\n" +
        "  gl_FragColor = texture2D(uTexture, vTexCoord);\n" +
        "}\n";

    public EmulatorRenderer(MainActivity activity) {
        mActivity = activity;
        mVertices = ByteBuffer.allocateDirect(QUAD_COORDS.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertices.put(QUAD_COORDS).position(0);
        mTexCoords = ByteBuffer.allocateDirect(TEX_COORDS.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTexCoords.put(TEX_COORDS).position(0);
    }
    
    public void setFrameBuffer(ByteBuffer buffer) {
        mFrameBuffer = buffer;
    }

    public void setUpscaler(Upscaler upscaler) {
        if (mCurrentUpscaler != upscaler) {
            mCurrentUpscaler = upscaler;
            mUpscalerChanged = true;
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        
        compileCurrentProgram();
        
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mTextureId = textures[0];
        
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 240, 160, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
    }

    private void compileCurrentProgram() {
        String fragmentShader = FRAGMENT_SHADER_NEAREST;
        switch (mCurrentUpscaler) {
            case SCALE2X: fragmentShader = FRAGMENT_SHADER_SCALE2X; break;
            case HQ2X:    fragmentShader = FRAGMENT_SHADER_HQ2X; break;
            case XBRZ:    fragmentShader = FRAGMENT_SHADER_XBRZ; break;
            default:      fragmentShader = FRAGMENT_SHADER_NEAREST; break;
        }

        if (mProgram != 0) {
            GLES20.glDeleteProgram(mProgram);
        }

        mProgram = createProgram(VERTEX_SHADER, fragmentShader);
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        mTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "aTexCoord");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        float targetRatio = 240.0f / 160.0f;
        float screenRatio = (float) width / height;

        int viewWidth, viewHeight;
        int viewX = 0, viewY = 0;

        if (screenRatio > targetRatio) {
            viewHeight = height;
            viewWidth = (int) (height * targetRatio);
            viewX = (width - viewWidth) / 2;
        } else {
            viewWidth = width;
            viewHeight = (int) (width / targetRatio);
            viewY = (height - viewHeight) / 2;
        }
        GLES20.glViewport(viewX, viewY, viewWidth, viewHeight);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mUpscalerChanged) {
            compileCurrentProgram();
            mUpscalerChanged = false;
        }

        if (mFrameBuffer == null) return;
        
        mActivity.runFrameJNI();
        
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        
        GLES20.glUseProgram(mProgram);
        
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
        mFrameBuffer.position(0);
        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, 240, 160, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mFrameBuffer);
        
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, 2, GLES20.GL_FLOAT, false, 0, mVertices);
        
        GLES20.glEnableVertexAttribArray(mTexCoordHandle);
        GLES20.glVertexAttribPointer(mTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mTexCoords);
        
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTexCoordHandle);
    }
    
    private int loadShader(int type, String shaderCode){
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
    
    private int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        return program;
    }
}
