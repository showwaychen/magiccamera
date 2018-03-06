package cn.cxw.magiccameralib;

import android.content.Context;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.GLES20;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.nio.ByteBuffer;

import cn.cxw.magiccameralib.imagefilter.DisplayFilter;
import cn.cxw.magiccameralib.imagefilter.GPUImageFilter;
import cn.cxw.openglesutils.GlTextureFrameBuffer;
import cn.cxw.openglesutils.OpenglCommon;
import cn.cxw.openglesutils.glthread.GlRenderThread;
import cn.cxw.openglesutils.glthread.GlThreadImageReader;
import cn.cxw.openglesutils.glthread.GlThreadPreview;
import cn.cxw.openglesutils.glthread.IPreviewView;
import cn.cxw.openglesutils.openglcapture.OpenglCapture;
import cn.cxw.openglesutils.openglcapture.ReadPixelCapture;

/**
 * Created by cxw on 2017/12/17.
 */

public class MagicCamera extends OpenglesDrawer implements CameraSource.CameraEventObserver, GlRenderThread.GLRenderer, IPreviewView.IPreviewCallback{
    static String TAG = MagicCamera.class.getCanonicalName();
    public interface MagicCameraFrameObserver
    {
        void OnProcessingFrame(ByteBuffer framedata, int stride, int width, int height);
    }
    MagicCameraFrameObserver mFrameObserver  = null;
    public void setFrameObserver(MagicCameraFrameObserver observer)
    {
        mFrameObserver = observer;
    }
    OpenglesDrawer mOpenglDrawer = null;
    public void setOpenglDrawer(final OpenglesDrawer drawer)
    {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                if (mOpenglDrawer != null) {
                    mOpenglDrawer.destroy();
                }
                mOpenglDrawer = drawer;
                if (mOpenglDrawer != null) {
                    mOpenglDrawer.init();
                    mOpenglDrawer.onOutputSizeChanged(mCameraSource.getOutputWidth(), mCameraSource.getOutputHeight());
                }
            }
        });
    }
    public OpenglesDrawer getOpenglDrawer()
    {
        return mOpenglDrawer;
    }
    CameraSource mCameraSource;
    //@Override  CameraEventObserver
    @Override
    public void onStartedPreview() {
        mOutputHeight = mCameraSource.getOutputHeight();
        mOutputWidth = mCameraSource.getOutputWidth();
    }

    @Override
    public void onStopPreview() {

    }

    @Override
    public void onFrameAvailable() {
        if (mbUseImageReaderThread && mOffScreenRender != null)
        {
            mOffScreenRender.requestRender();
        }
        else
        {
            if (mGlRenderThread != null)
            {
                mGlRenderThread.requestRender();
            }
        }
    }
    //@Override
    //gl thread
    GlRenderThread mGlRenderThread = null;
    OpenglCapture mOpenglCapture = null;
    GlThreadImageReader mOffScreenRender = null;
    boolean mbUseImageReaderThread = false;
    IPreviewView mPreviewView = null;
    GlTextureFrameBuffer mFrameBuffer = null;

    DisplayFilter mDisplayFilter;
     GPUImageFilter mFilter = new GPUImageFilter();

    Context mContext = null;
    class FrameOnCapture implements OpenglCapture.IFrameCaptured
    {
        @Override
        public void onPreviewFrame(ByteBuffer byteBuffer, int i, int i1, int i2, long l) {

        }
    }
    int mSharedTextId = OpenglCommon.NO_TEXTURE;
    class PreviewRender implements GlRenderThread.GLRenderer
    {
//        GPUImageFilter mPreviewFilter = new GPUImageFilter();
        int mWidth = 0;
        int mHeight = 0;
        @Override
        public void onGlInit(int i, int i1) {
//            mPreviewFilter.init();
            mWidth = i;
            mHeight = i1;
        }

        @Override
        public void onGlResize(int width, int height) {
//            mPreviewFilter.onOutputSizeChanged(width, height);
            mWidth = width;
            mHeight = height;
        }

        @Override
        public void onGlDrawFrame() {
            GLES20.glViewport(0, 0, mWidth, mHeight);
            GLES20.glClearColor(255, 255, 255, 1);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
//            mPreviewFilter.onDraw(mSharedTextId, mCubeBuffer, mDisplayTextureBuffer);
            previewDisplay(mSharedTextId);
        }

        @Override
        public void onGlDeinit() {
//            mPreviewFilter.destroy();
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    class ImageAvailable implements ImageReader.OnImageAvailableListener
    {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            image = reader.acquireNextImage();
            if (image != null) {
                try {
                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    int mSrcStride = 0;
                    mSrcStride = planes[0].getRowStride();
                    if (mFrameObserver != null)
                    {
                        mFrameObserver.OnProcessingFrame(buffer, mSrcStride, image.getWidth(), image.getHeight());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    if (image!=null) {
                        image.close();
                    }
                }
            }
        }
    }
    public void setContext(Context context)
    {
        mContext = context;
    }
    static MagicCamera sMagicCamera = new MagicCamera();
    public static MagicCamera getInst()
    {
        return sMagicCamera;
    }
    protected MagicCamera()
    {
        mCameraSource = new CameraSource();
        mCameraSource.setObserver(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mbUseImageReaderThread = true;

        }
        if (!mbUseImageReaderThread)
        {
            mOpenglCapture = new ReadPixelCapture();
            mOpenglCapture.setOnCapture(new FrameOnCapture());

        }
    }
    public void setPreviewView(IPreviewView pview)
    {
        mPreviewView = pview;
        if (pview != null)
        {
            mGlRenderThread = new GlThreadPreview(pview);
            pview.addRenderCallback(this);
        }
    }



    boolean initFilter()
    {
        if (mContext == null)
        {
            Log.e(TAG, "context is null");
            return false;
        }

        if (mDisplayFilter == null)
        {
            mDisplayFilter = new DisplayFilter();
        }
        mCameraSource.init();
        mDisplayFilter.init();
        mFilter.init();
        if (mOpenglDrawer != null)
        {
            mOpenglDrawer.init();
        }
        return true;
    }
    void destroyFilter()
    {
        mCameraSource.destroy();
        if (mFrameBuffer != null)
        {
            mFrameBuffer.release();
            mFrameBuffer = null;
        }
        mDisplayFilter.destroy();
            mFilter.destroy();
        if (mOpenglDrawer != null)
        {
            mOpenglDrawer.destroy();
        }
    }
    // @Override GlRenderThread.GLRenderer
    @Override
    public void onGlInit(int width, int height) {
        if (!initFilter())
        {
            Log.e(TAG, "filter init failed");
            return ;
        }
        init();
        if (mbUseImageReaderThread)
        {
            startPreviewGlThread();
        }
    }
    @Override
    public void onGlResize(int width, int height) {

    }

    @Override
    public void onGlDrawFrame() {
        draw();
    }

    @Override
    public void onGlDeinit() {
        destroy();
        destroyFilter();
    }
    //@Override

    // @Override  OpenglesDrawer
    @Override
    protected void onDraw() {
        if (mFrameBuffer == null)
        {
            mFrameBuffer = new GlTextureFrameBuffer(GLES20.GL_RGBA);
            mFrameBuffer.setSize(mCameraSource.getOutputWidth(), mCameraSource.getOutputHeight());

//            mCubeTest.onOutputSizeChanged(mCameraSource.getCameraFrameWidth(), mCameraSource.getCameraFrameHeight());
            if(mOpenglDrawer != null)
            {
                mOpenglDrawer.onOutputSizeChanged(mCameraSource.getOutputWidth(), mCameraSource.getOutputHeight());
            }
            if (!mbUseImageReaderThread)
            {
                mOpenglCapture.initCapture(mCameraSource.getOutputWidth(), mCameraSource.getOutputHeight());
            }
        }
        GLES20.glViewport(0, 0, mFrameBuffer.getWidth(), mFrameBuffer.getHeight());
        int displaytextureid = OpenglCommon.NO_TEXTURE;
        mCameraSource.draw();
        displaytextureid = mCameraSource.getTextureId();
        mFrameBuffer.activeFrameBuffer();
        if(mOpenglDrawer == null)
        {
//            mFilter.onDraw(mCameraSource.getTextureId());
            displaytextureid = mCameraSource.getTextureId();
        }
        else
        {
            if (mOpenglDrawer instanceof GPUImageFilter)
            {
                ((GPUImageFilter)mOpenglDrawer).onDraw(mCameraSource.getTextureId());
            }
            else if (mOpenglDrawer instanceof OpenglesDrawerEx)
            {
                ((OpenglesDrawerEx)mOpenglDrawer).setTexture(mCameraSource.getTextureId());
                mOpenglDrawer.draw();
            }
            else
            {
                mFilter.onDraw(mCameraSource.getTextureId());
                mOpenglDrawer.draw();
            }
            displaytextureid = mFrameBuffer.getTextureId();
        }
        if (!mbUseImageReaderThread && mFrameObserver != null) {
            mOpenglCapture.setTextureId(displaytextureid);
            mOpenglCapture.onCapture();
        }
        mFrameBuffer.disactiveFrameBuffer();
        //用于画在ImageReader的surface中。

        if(mbUseImageReaderThread  && mGlRenderThread != null)
        {
            mSharedTextId = displaytextureid;
            mGlRenderThread.requestRender();

            mDisplayFilter.onOutputSizeChanged(mOutputWidth, mOutputHeight);
            mDisplayFilter.setTextureid(displaytextureid);
            mDisplayFilter.draw();
            if (mOpenglDrawer != null && mOpenglDrawer instanceof OpenglesDrawerEx)
            {
                ((OpenglesDrawerEx)mOpenglDrawer).offScreenOnDraw();
            }
        }
        else
        {
            previewDisplay(displaytextureid);
        }
    }
    //@Override
    void previewDisplay(int textureid)
    {
        mDisplayFilter.onOutputSizeChanged(preViewWidth, preViewHeight);
        mDisplayFilter.setTextureid(textureid);
        mDisplayFilter.draw();
        if(mOpenglDrawer instanceof OpenglesDrawerEx)
        {
            ((OpenglesDrawerEx)mOpenglDrawer).onScreenOnDraw(mDisplayFilter.getOutputWidth(), mDisplayFilter.getOutputHeight());
        }
    }
    //@Override  preview callback to startpreview or stoppreview
    int preViewWidth = 0;
    int preViewHeight = 0;
    @Override
    public void onSurfaceCreated(@NonNull IPreviewView.ISurfaceHolder holder, int width, int height) {
    }

    @Override
    public void onSurfaceChanged(@NonNull IPreviewView.ISurfaceHolder holder, int width, int height) {
        preViewWidth = width;
        preViewHeight = height;
        startPreView();
    }
    @Override
    public void onSurfaceDestroyed(@NonNull IPreviewView.ISurfaceHolder holder) {
        stopPreview();
        preViewWidth = 0;
        preViewHeight = 0;
    }
   //@Override
    public void startPreView()
    {
        if (isPreview())
        {
            Log.w(TAG, "has started preview");
            return ;
        }
        mCameraSource.setDisplayRatio((float) preViewWidth / (float)preViewHeight);
        mCameraSource.adjustImageScaling();
        if (mbUseImageReaderThread) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mOffScreenRender = new GlThreadImageReader(mCameraSource.getOutputWidth(), mCameraSource.getOutputHeight(), this);
                mOffScreenRender.setImageAvailableListener(new ImageAvailable());
                mOffScreenRender.start();
            }
            return ;
        }
        startPreviewGlThread();
    }
    void startPreviewGlThread()
    {
        if (mGlRenderThread == null)
        {
            mGlRenderThread = new GlThreadPreview(mPreviewView);
        }
        if (mbUseImageReaderThread && mOffScreenRender != null)
        {
            mGlRenderThread.setSharedContext(mOffScreenRender.getEglContext());
            mGlRenderThread.setRender(new PreviewRender());

        }
        else
        {
            mGlRenderThread.setRender(this);
        }
        mGlRenderThread.start();
    }
    public void stopPreview()
    {
        if(mbUseImageReaderThread && mOffScreenRender != null)
        {
            mOffScreenRender.stopRender();
            mOffScreenRender.release();
            mOffScreenRender = null;
        }
        mGlRenderThread.stopRender();
        mGlRenderThread = null;
    }


    public void switchCamera()
    {
        boolean hasdoublecamera = mCameraSource.hasFrontCamera() && mCameraSource.hasBackCamera();
        if (!hasdoublecamera)
        {
            Log.w(TAG, "only have one camera ,don't need to switch");
            return ;
        }
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                mCameraSource.switchCamera();
            }
        });
    }

    public void setCameraSize(int width , int height)
    {
        mCameraSource.setCameraSize(width, height);
    }
    public boolean isPreview()
    {
        return mCameraSource.isPreview();
    }
}
