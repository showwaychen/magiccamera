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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
        public void onPreviewFrame(ByteBuffer directFrameBuffer, int stride, int width, int height, long ptsMS) {
            if (mFrameObserver != null)
            {
                mFrameObserver.OnProcessingFrame(directFrameBuffer, stride, width, height);
            }
        }
    }
    int mSharedTextId = OpenglCommon.NO_TEXTURE;
    Object mSharedToken = new Object();
    Lock lock = new ReentrantLock();
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
            if (mbUseImageReaderThread)
            {
                startImageReaderGLThread();
            }
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
            GLES20.glClearColor(0, 0, 0, 1);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
//            Log.d(TAG, "onGlDrawFrame prelock");
            lock.lock();
//            Log.d(TAG, "onGlDrawFrame locked");
            previewDisplay(mSharedTextId);
            GLES20.glFinish();
//            Log.d(TAG, "onGlDrawFrame preunlock");
            lock.unlock();
//            Log.d(TAG, "onGlDrawFrame unlock");

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
//        if (mbUseImageReaderThread) {
            mFrameWidth = mCameraSource.getOutputWidth();
            mFrameHeight = mCameraSource.getOutputHeight();
//        }
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
        init();
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
    protected void onInit() {
        if (!initFilter())
        {
            Log.e(TAG, "filter init failed");
            return ;
        }
        super.onInit();
    }

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
//                mOpenglCapture.initCapture(mCameraSource.getOutputWidth(), mCameraSource.getOutputHeight());
                mOpenglCapture.initCapture(getFrameWidth(), getFrameHeight());

            }
        }

        int displaytextureid = OpenglCommon.NO_TEXTURE;
//        Log.d(TAG, "ondraw prelock");
//        Log.d(TAG, "ondraw lock");
        mCameraSource.draw();
        lock.lock();
        mFrameBuffer.activeFrameBuffer();
        GLES20.glViewport(0, 0, mFrameBuffer.getWidth(), mFrameBuffer.getHeight());
        GLES20.glClearColor(0, 0, 0, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        if(mOpenglDrawer != null)
        {
//            synchronized (this)
//            {
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

        }
        else
        {
            mFilter.onDraw(mCameraSource.getTextureId());
        }

        mFrameBuffer.disactiveFrameBuffer();
        lock.unlock();
        displaytextureid = mFrameBuffer.getTextureId();
//        }
        //用于画在ImageReader的surface中。
        if(mbUseImageReaderThread  && mGlRenderThread != null)
        {
            //此地方要做下线程同步，因为在不同的线程会使用同一个mDisplayFilter
            lock.lock();
            mDisplayFilter.onOutputSizeChanged(mOutputWidth, mOutputHeight);
            mDisplayFilter.setTextureid(displaytextureid);
            GLES20.glClearColor(0, 0, 0, 1);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            mDisplayFilter.draw();
            lock.unlock();
            if (mOpenglDrawer != null && mOpenglDrawer instanceof OpenglesDrawerEx)
            {
                ((OpenglesDrawerEx)mOpenglDrawer).onScreenOnDraw(mOpenglDrawer.getOutputWidth(), mOpenglDrawer.getOutputHeight());
            }
            mSharedTextId = displaytextureid;
            mGlRenderThread.requestRender();
        }
        else
        {
            previewDisplay(displaytextureid);
            mFrameBuffer.activeFrameBuffer();
            GLES20.glViewport(0, 0, mFrameBuffer.getWidth(), mFrameBuffer.getHeight());
            GLES20.glClearColor(0, 0, 0, 1);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            if (mOpenglDrawer != null && mOpenglDrawer instanceof OpenglesDrawerEx)
            {
                ((OpenglesDrawerEx)mOpenglDrawer).offScreenOnDraw();
            }
            //在屏渲染时，用glReadPixels读出的图像是倒的。
            //此函数原型glReadPixels(
//            int x,
//            int y,
//            int width,
//            int height,
//            int format,
//            int type,
//            java.nio.Buffer pixels
//           );
            //glReadPixels 读的值是从左下角开始的，在opengl es中 坐标系的原点在左下角。但一般的图像是以左上角为原点。
            //这就导致能过这个函数获取的图像是倒的。
            //但为什么显示的是正的呢？个人猜想，可能系统显示的时候，是从左上角取的数据。
            //那为什么用此函数读离屏渲染使用的 frametexture时，是正的呢。
            //个人猜想：
            //图像坐标一般是以左上角为原点。这样主要是为了在内存存储的方便。
            //         (0, 0)      (1, 0)
            //
            //         (0, 1)      (1, 1)
            //但在opengles中使用的时候是以左下角为原点的。所以在使用贴图的时候在opengles世界中，此纹理是倒的。
            //虽然是倒的，但glReadPixels是从左上角读的，所以读的图像就是正的。
            if (!mbUseImageReaderThread && mFrameObserver != null) {
                mOpenglCapture.setTextureId(displaytextureid);
                mOpenglCapture.onCapture();
            }
            mFrameBuffer.disactiveFrameBuffer();
        }

    }
    //@Override
    void previewDisplay(int textureid)
    {
        if (mDisplayFilter == null)
        {
            return ;
        }
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
        mFrameWidth = preViewWidth;
        mFrameHeight = preViewHeight;
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
        if (mGlRenderThread == null)
        {
            mGlRenderThread = new GlThreadPreview(mPreviewView);
        }
        if (mbUseImageReaderThread)
        {
            mGlRenderThread.setRender(new PreviewRender());
            mFrameWidth = mCameraSource.getOutputWidth();
            mFrameHeight = mCameraSource.getOutputHeight();
        }
        else
        {
            mGlRenderThread.setRender(this);
        }
        mGlRenderThread.start();
    }
    void startImageReaderGLThread()
    {
        if (mbUseImageReaderThread) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mOffScreenRender = new GlThreadImageReader(mCameraSource.getOutputWidth(), mCameraSource.getOutputHeight(), this);
                mOffScreenRender.setSharedContext(mGlRenderThread.getEglContext());
                mOffScreenRender.setImageAvailableListener(new ImageAvailable());
                mOffScreenRender.start();
            }
            return ;
        }
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
    int mFrameWidth = 0;
    int mFrameHeight = 0;

    /**
     * @return 回调出来的图像的宽。
     */
    public int getFrameWidth()
    {
        return mFrameWidth;
    }

    /**
     * @return 回调的图像的高。
     */
    public int getFrameHeight()
    {
        return mFrameHeight;
    }
}
