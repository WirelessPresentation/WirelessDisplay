package com.bjnet.airplaydemo.base;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.SurfaceHolder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Administrator on 2017/9/5.
 */

public class JetGlSurfaceView extends GLSurfaceView implements
        GLSurfaceView.Renderer {
    private static final String TAG = "GLSurface";
    int mBufferWidthY, mBufferHeightY, mBufferWidthUV, mBufferHeightUV;

    ByteBuffer mBuffer;

    int mBufferPositionY, mBufferPositionU, mBufferPositionV;

    private static final int FLOAT_SIZE_BYTES = 4;

    private static final int SHORT_SIZE_BYTES = 2;

    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;

    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;

    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;

    private static final float[] TRIANFLE_VERTICES_DATA = {1, -1, 0, 1, 1,
            1, 1, 0, 1, 0, -1, 1, 0, 0, 0, -1, -1, 0, 0, 1};

    private static final short[] INDICES_DATA = {0, 1, 2, 2, 3, 0};

    private FloatBuffer mTriangleVertices;

    private ShortBuffer mIndices;

    private static final String VERTEX_SHADER_SOURCE = "attribute vec4 aPosition;\n"
            + "attribute vec2 aTextureCoord;\n"
            + "varying vec2 vTextureCoord;\n"
            + "void main() {\n"
            + "  gl_Position = aPosition;\n"
            + "  vTextureCoord = aTextureCoord;\n" + "}\n";

    private static final String FRAGMENT_SHADER_SOURCE = "precision mediump float;"
            + "varying vec2 vTextureCoord;"
            + ""
            + "uniform sampler2D SamplerY; "
            + "uniform sampler2D SamplerU;"
            + "uniform sampler2D SamplerV;"
            + ""
            + "const mat3 yuv2rgb = mat3(1, 0, 1.2802,1, -0.214821, -0.380589,1, 2.127982, 0);"
            + ""
            + "void main() {    "
            + "    vec3 yuv = vec3(1.1643 * (texture2D(SamplerY, vTextureCoord).r - 0.0625),"
            + "                    texture2D(SamplerU, vTextureCoord).r - 0.5,"
            + "                    texture2D(SamplerV, vTextureCoord).r - 0.5);"
            + "    vec3 rgb = yuv * yuv2rgb;    "
            + "    gl_FragColor = vec4(rgb, 1.0);" + "} ";

    private int mProgram;

    private int maPositionHandle;

    private int maTextureHandle;

    private int muSamplerYHandle;

    private int muSamplerUHandle;

    private int muSamplerVHandle;

    private int[] mTextureY = new int[1];

    private int[] mTextureU = new int[1];

    private int[] mTextureV = new int[1];

    private boolean mSurfaceCreated = false;

    private boolean mSurfaceDestroyed = false;

    @SuppressWarnings("unused")
    private Context mContext;

    private int mViewWidth, mViewHeight, mViewX, mViewY;
    private boolean mFullScreenRequired;
    private boolean isFirstFrame;

    public JetGlSurfaceView(Context context,
                            boolean fullScreenRequired) {
        super(context);
        isFirstFrame = true;
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        setRenderer(this);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_GPU);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        //setBuffer(buffer, bufferWidth, bufferHeight);
        mContext = context;

        mTriangleVertices = ByteBuffer.allocateDirect(TRIANFLE_VERTICES_DATA.length
                * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mTriangleVertices.put(TRIANFLE_VERTICES_DATA).position(0);

        mIndices = ByteBuffer.allocateDirect(INDICES_DATA.length
                * SHORT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        mIndices.put(INDICES_DATA).position(0);

        mFullScreenRequired = false;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_DITHER);
        GLES20.glDisable(GLES20.GL_STENCIL_TEST);
        GLES20.glDisable(GL10.GL_DITHER);

        String extensions = GLES20.glGetString(GL10.GL_EXTENSIONS);
        Log.d(TAG, "OpenGL extensions=" + extensions);
        Log.d(TAG, "--> onSurfaceCreated");
        // Ignore the passed-in GL10 interface, and use the GLES20
        // class's static methods instead.
        mProgram = createProgram(VERTEX_SHADER_SOURCE,
                FRAGMENT_SHADER_SOURCE);
        if (mProgram == 0) {
            return;
        }
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        checkGlError("glGetAttribLocation aPosition");
        if (maPositionHandle == -1) {
            throw new RuntimeException(
                    "Could not get attrib location for aPosition");
        }
        maTextureHandle = GLES20.glGetAttribLocation(mProgram,
                "aTextureCoord");
        checkGlError("glGetAttribLocation aTextureCoord");
        if (maTextureHandle == -1) {
            throw new RuntimeException(
                    "Could not get attrib location for aTextureCoord");
        }

        muSamplerYHandle = GLES20.glGetUniformLocation(mProgram, "SamplerY");
        if (muSamplerYHandle == -1) {
            throw new RuntimeException(
                    "Could not get uniform location for SamplerY");
        }
        muSamplerUHandle = GLES20.glGetUniformLocation(mProgram, "SamplerU");
        if (muSamplerUHandle == -1) {
            throw new RuntimeException(
                    "Could not get uniform location for SamplerU");
        }
        muSamplerVHandle = GLES20.glGetUniformLocation(mProgram, "SamplerV");
        if (muSamplerVHandle == -1) {
            throw new RuntimeException(
                    "Could not get uniform location for SamplerV");
        }

        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(maPositionHandle,
                3,
                GLES20.GL_FLOAT,
                false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
                mTriangleVertices);
        checkGlError("glVertexAttribPointer maPosition");

        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        checkGlError("glEnableVertexAttribArray maPositionHandle");
        GLES20.glVertexAttribPointer(maTextureHandle,
                2,
                GLES20.GL_FLOAT,
                false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
                mTriangleVertices);
        checkGlError("glVertexAttribPointer maTextureHandle");
        GLES20.glEnableVertexAttribArray(maTextureHandle);
        checkGlError("glEnableVertexAttribArray maTextureHandle");

        GLES20.glGenTextures(1, mTextureY, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureY[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glGenTextures(1, mTextureU, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureU[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glGenTextures(1, mTextureV, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureV[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);

        mSurfaceCreated = true;
        setViewport(getWidth(), getHeight());
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int w, int h) {
        GLES20.glViewport(0, 0, w, h);
        setViewport(w, h);

        Log.i(TAG, "onSurfaceChanged: w:" + w + " h:" + h);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        if (isFirstFrame){
            isFirstFrame = false;
            Log.i(TAG, "onDrawFrame: mViewWidth:"+mViewWidth+" mViewHeight:"+mViewHeight+" mViewX:"+mViewX+" mViewY:"+mViewY);
        }

        GLES20.glViewport(mViewX, mViewY, mViewWidth, mViewHeight);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT
                | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);
        checkGlError("glUseProgram");

        if (mBuffer != null) {
            synchronized (this) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureY[0]);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,
                        0,
                        GLES20.GL_LUMINANCE,
                        mBufferWidthY,
                        mBufferHeightY,
                        0,
                        GLES20.GL_LUMINANCE,
                        GLES20.GL_UNSIGNED_BYTE,
                        mBuffer.position(mBufferPositionY));
                GLES20.glUniform1i(muSamplerYHandle, 0);

                GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureU[0]);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,
                        0,
                        GLES20.GL_LUMINANCE,
                        mBufferWidthUV,
                        mBufferHeightUV,
                        0,
                        GLES20.GL_LUMINANCE,
                        GLES20.GL_UNSIGNED_BYTE,
                        mBuffer.position(mBufferPositionU));
                GLES20.glUniform1i(muSamplerUHandle, 1);

                GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureV[0]);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,
                        0,
                        GLES20.GL_LUMINANCE,
                        mBufferWidthUV,
                        mBufferHeightUV,
                        0,
                        GLES20.GL_LUMINANCE,
                        GLES20.GL_UNSIGNED_BYTE,
                        mBuffer.position(mBufferPositionV));
                GLES20.glUniform1i(muSamplerVHandle, 2);
            }
        }

        GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                INDICES_DATA.length,
                GLES20.GL_UNSIGNED_SHORT,
                mIndices);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "surfaceDestroyed: ");
            mSurfaceCreated = false;
            mSurfaceDestroyed = true;
            super.surfaceDestroyed(holder);
    }

    public boolean isReady() {
        return (mSurfaceCreated && !mSurfaceDestroyed);
    }

    public boolean isDestroyed() {
        return mSurfaceDestroyed;
    }

    public void setBuffer(ByteBuffer buffer, int bufferWidth,
                          int bufferHeight) {
        mBuffer = buffer;
        mBufferWidthY = bufferWidth;
        mBufferHeightY = bufferHeight;

        mBufferWidthUV = (mBufferWidthY >> 1);
        mBufferHeightUV = (mBufferHeightY >> 1);

        mBufferPositionY = 0;
        mBufferPositionU = (mBufferWidthY * mBufferHeightY);
        mBufferPositionV = (mBufferPositionU + (mBufferWidthUV * mBufferHeightUV));

        if (!mFullScreenRequired){
            setViewport(getWidth(), getHeight());
        }
    }

    private int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader,
                    GLES20.GL_COMPILE_STATUS,
                    compiled,
                    0);
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader " + shaderType + ":");
                Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }

        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER,
                fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader);
            checkGlError("glAttachShader");
            GLES20.glAttachShader(program, pixelShader);
            checkGlError("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program,
                    GLES20.GL_LINK_STATUS,
                    linkStatus,
                    0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program: ");
                Log.e(TAG, GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    private void setViewport(int width, int height) {
        if (mFullScreenRequired) {
            mViewWidth = width;
            mViewHeight = height;
            mViewX = mViewY = 0;
        } else {
            float fRatio = ((float) mBufferWidthY / (float) mBufferHeightY);
            mViewWidth = (int) ((float) width / fRatio) > height ? (int) ((float) height * fRatio)
                    : width;
            mViewHeight = (int) (mViewWidth / fRatio) > height ? height
                    : (int) (mViewWidth / fRatio);
            mViewX = ((width - mViewWidth) >> 1);
            mViewY = ((height - mViewHeight) >> 1);
        }
    }

    private void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }
}
