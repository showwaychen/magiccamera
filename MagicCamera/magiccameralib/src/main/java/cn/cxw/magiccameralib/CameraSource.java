package cn.cxw.magiccameralib;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import cn.cxw.magiccameralib.imagefilter.GPUImageFilter;
import cn.cxw.openglesutils.GlTextureFrameBuffer;
import cn.cxw.openglesutils.OpenglCommon;
import cn.cxw.openglesutils.Rotation;
import cn.cxw.openglesutils.TextureRotationUtil;

/**
 * Created by user on 2017/12/14.
 */

public class CameraSource extends GPUImageFilter {

    private static final String TAG = CameraSource.class.getCanonicalName();
    private static final int NO_CAMERA = -1;
    static class ImageSize
    {
        public int w = 0;
        public int h = 0;
        public float ratio = 0.0f;
        public ImageSize(int w, int h)
        {
            this.w = w;
            this.h = h;
            if (h != 0)
            {
                ratio = (float) w / (float) h;
            }
        }
    }
    static ImageSize outPutSizeList[] = {new ImageSize(640, 480),
                                            new ImageSize(640, 360),
                                        new ImageSize(480, 640),
                                        new ImageSize(360, 640)};
    public static final String VERTEX_SHADER = "" +
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            " \n" +
            "varying vec2 textureCoordinate;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position;\n" +
            "    textureCoordinate = inputTextureCoordinate.xy;\n" +
            "}";
    public static final String FRAGMENT_SHADER = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "varying highp vec2 textureCoordinate;\n" +
            " \n" +
            "uniform samplerExternalOES inputImageTexture;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "}";

    ///////////////////
    public  interface CameraEventObserver
    {
        int EVENT_STARTEDPREVIEW = 1;
        int EVENT_STOPPREVIEW = 2;
        int EVENT_FRAMEAVAILABLE = 3;
        void onStartedPreview();
        void onStopPreview();
        void onFrameAvailable();
    }
    WeakReference<CameraEventObserver> mEventObserver;
    public void setObserver(CameraEventObserver observer)
    {
        mEventObserver = new WeakReference<>(observer);
    }
    private void notifyEvent(int eventid)
    {
        CameraEventObserver tmp = mEventObserver.get();
        if (tmp != null)
        {
            switch (eventid)
            {
                case CameraEventObserver.EVENT_STARTEDPREVIEW:
                    tmp.onStartedPreview();
                    break;
                case CameraEventObserver.EVENT_STOPPREVIEW:
                    tmp.onStopPreview();
                    break;
                case CameraEventObserver.EVENT_FRAMEAVAILABLE:
                    tmp.onFrameAvailable();
                    break;
            }
        }
    }
//////////

    ///////////// camera about
    SurfaceTexture mSufaceTexture = null;
    int mCameraId = NO_CAMERA;
    int mCameraFace = -1;
    int mCameraWidth = 640;
    int mCameraHeight = 480;

    Camera mCamera = null;
    int mTextureId = OpenglCommon.NO_TEXTURE;
    FloatBuffer mVertexBuffer;
    float mDefaultVertex[] = {-1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f};
    FloatBuffer mTextureCoorBuffer;
    private Rotation mRotation = Rotation.NORMAL;
    GlTextureFrameBuffer mTextureFrameBuffer = null;
    boolean mIsPreview = false;

    public CameraSource()
    {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        if (hasFrontCamera())
        {
            mCameraFace = Camera.CameraInfo.CAMERA_FACING_FRONT;
        }else if (hasBackCamera())
        {
            mCameraFace = Camera.CameraInfo.CAMERA_FACING_BACK;
        }

        mVertexBuffer = ByteBuffer.allocateDirect(mDefaultVertex.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertexBuffer.put(mDefaultVertex).position(0);
        mTextureCoorBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    }
    public void setCameraRotation(int rotation)
    {
        switch (rotation)
        {
            case 0:
                mRotation = Rotation.NORMAL;
                break;
            case 90:
                mRotation = Rotation.ROTATION_90;
                break;
            case 180:
                mRotation = Rotation.ROTATION_180;
                break;
            case 270:
                mRotation = Rotation.ROTATION_270;
                break;
        }
//        adjustImageScaling();
    }
    //通过 显示的屏幕的宽高比来调整，crop
    float mRatio = 0.0f;
    public void setDisplayRatio(float ratio)
    {
        mRatio = ratio;
    }
    public void adjustImageScaling() {
        if (mIsPreview)
        {
            return ;
        }
        mCameraId = getCameraId(mCameraFace);
        if (mCameraId == NO_CAMERA)
        {
            return ;
        }
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        //摄像头旋转是按逆时针方向旋转的。
        setCameraRotation(info.orientation);

        mOutputWidth = mCameraWidth;
        mOutputHeight = mCameraHeight;
        if (mRotation == Rotation.ROTATION_270 || mRotation == Rotation.ROTATION_90) {
            mOutputWidth = mCameraHeight;
            mOutputHeight = mCameraWidth;
        }
//        float ratio = 0.5625f;
        float ratio = mRatio;

        boolean flipHorizontal = mCameraFace == Camera.CameraInfo.CAMERA_FACING_FRONT;
        float[] textureCords = TextureRotationUtil.getRotation(mRotation, flipHorizontal, false);
        if (ratio != 0.0f)
        {
            int nearestindex = 0;
            float diffratio = 2.f;
            for (int i = 0; i < outPutSizeList.length; i++)
            {
                float curdiff = Math.abs(outPutSizeList[i].ratio - ratio);
                if (curdiff < diffratio)
                {
                    diffratio = curdiff;
                    nearestindex = i;
                }
            }

            float wratio = 1.0f;
            float hratio = (float) outPutSizeList[nearestindex].w / (float) mOutputWidth;
            mOutputWidth = outPutSizeList[nearestindex].w;

            //crop
            //贴图坐标的旋转本质上就是改变顶点坐标与纹理坐标的对应关系。所以对于crop操作就是对纹理坐标系中的四个点的大小操作。
            //  (0, 1)     (1, 1)
            //
            //
            //  (0, 0)     (1, 0)
            float winc = (1.0f - wratio)/ 2.f;
            float hinc = (1.0f - hratio) / 2.f;
            for (int i = 0; i < textureCords.length; i+=2)
            {
                if (textureCords[i] == 0.0f && textureCords[i + 1] == 0.0f)
                {
                    textureCords[i + 1] += hinc;
                }
                if (textureCords[i] == 1.0f && textureCords[i + 1] == 0.0f)
                {
                    textureCords[i + 1] += hinc;
                }
                if (textureCords[i] == 0.0f && textureCords[i + 1] == 1.0f)
                {
                    textureCords[i + 1] -= hinc;
                }
                if (textureCords[i] == 1.0f && textureCords[i + 1] == 1.0f)
                {
                    textureCords[i + 1] -= hinc;
                }
            }
        }

        mTextureCoorBuffer.clear();
        mTextureCoorBuffer.put(textureCords).position(0);
    }
    public  int getTextureId()
    {
        return  mTextureFrameBuffer.getTextureId();
    }
    public  void onDraw()
    {
        if (!isInitialized())
        {
            return ;
        }
        mSufaceTexture.updateTexImage();
        mGlProgram.useProgram();
        if (mTextureFrameBuffer == null)
        {
            mTextureFrameBuffer = new GlTextureFrameBuffer(GLES20.GL_RGBA);
//            mTextureFrameBuffer.setSize(getCameraFrameWidth(), getCameraFrameHeight());
            mTextureFrameBuffer.setSize(getOutputWidth(), getOutputHeight());

        }

        mTextureFrameBuffer.activeFrameBuffer();
        GLES20.glClearColor(0, 0, 0, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        mVertexBuffer.position(0);
        mTextureCoorBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);

        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, mTextureCoorBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);
        if (mTextureId != OpenglCommon.NO_TEXTURE)
        {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId);
            // Set the sampler to texture unit 0
            GLES20.glUniform1i(mGLUniformTexture, 0);
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        //restore status
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }
    public boolean startPreview()
    {
        if (mIsPreview)
        {
            return true;
        }

        if (mSufaceTexture == null)
        {
            Log.d(TAG, "no surfacetexture");
            return false;
        }

//        adjustImageScaling();
        try {
            mCamera = Camera.open(mCameraId);
            Camera.Parameters cparams = mCamera.getParameters();
            if (cparams.getSupportedFocusModes().contains(
                    Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                cparams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }

            cparams.setPreviewSize(mCameraWidth, mCameraHeight);
            mCamera.setParameters(cparams);

            mCamera.setPreviewTexture(mSufaceTexture);
            mCamera.startPreview();
//            adjustImageScaling();
            if (mTextureFrameBuffer != null)
            {
                mTextureFrameBuffer.release();
                mTextureFrameBuffer = null;
            }
//            mTextureFrameBuffer = new GlTextureFrameBuffer(GLES20.GL_RGBA);
//            mTextureFrameBuffer.setSize(getCameraFrameWidth(), getCameraFrameHeight());
            Log.d(TAG, "startPreview success");
            mIsPreview = true;
            notifyEvent(CameraEventObserver.EVENT_STARTEDPREVIEW);
        }catch (Exception e)
        {
            Log.e(TAG, "startPreview failed = " + e.getMessage());
            mCamera = null;
            mCameraId = NO_CAMERA;
            return false;
        }
        return false;
    }
    public void stopPreview()
    {
        if (isPreview())
        {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            mCameraId = NO_CAMERA;
            if (mTextureFrameBuffer != null)
            {
                mTextureFrameBuffer.release();
                mTextureFrameBuffer = null;
            }
            mIsPreview = false;
        }
    }
    public void restartPreview() {
        destroySurfaceTexture();
        stopPreview();
        createSurfaceTexture();
        startPreview();
    }
    public void switchCamera() {
        Log.i(TAG, "rotateCamera");
        mCameraFace =  mCameraFace == Camera.CameraInfo.CAMERA_FACING_FRONT ?
                Camera.CameraInfo.CAMERA_FACING_BACK :
                Camera.CameraInfo.CAMERA_FACING_FRONT;
        restartPreview();
    }
    public boolean isPreview()
    {
        return mIsPreview;
    }
    public int getCameraId(int cameraface)
    {
        int cameraid = NO_CAMERA;
        int numberCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberCameras; i++)
        {
            Camera.CameraInfo  info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == cameraface)
            {
                cameraid = i;
                break;
            }
        }
        return cameraid;
    }
    public  boolean hasCamera(int cameraface)
    {
        return getCameraId(cameraface) != NO_CAMERA;
    }
    public boolean hasFrontCamera()
    {
        return hasCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
    }
    public boolean hasBackCamera()
    {
        return hasCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
    }
    public void setCameraSize(int width , int height)
    {
        if (width <= 0 || height <= 0)
        {
            return ;
        }
        mCameraWidth = width;
        mCameraHeight = height;
    }
    void createSurfaceTexture()
    {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        mSufaceTexture = new SurfaceTexture(textures[0]);
        mSufaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                notifyEvent(CameraEventObserver.EVENT_FRAMEAVAILABLE);
            }
        });
    }

    void destroySurfaceTexture()
    {
        if (mTextureId != OpenglCommon.NO_TEXTURE)
        {
            GLES20.glDeleteTextures(1, new int[]{mTextureId}, 0);
            mTextureId = OpenglCommon.NO_TEXTURE;
        }
    }

    @Override
    public void onInit() {
        super.onInit();
        createSurfaceTexture();
        startPreview();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopPreview();
        destroySurfaceTexture();
    }
}
