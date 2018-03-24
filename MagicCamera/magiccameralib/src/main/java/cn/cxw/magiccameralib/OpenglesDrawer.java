package cn.cxw.magiccameralib;

import java.util.LinkedList;

/**
 * Created by cxw on 2017/12/16.
 */

public abstract  class OpenglesDrawer {
    public static enum DrawerState
    {
        NONE, INITIALING, INITIALIZED, DESTROYING, DESTROYED
    }
    private final LinkedList<Runnable> mRunOnDraw;
    protected boolean mIsInitialized = false;
    protected boolean mInitialing = false;
    protected  DrawerState mState = DrawerState.NONE;

    protected int mOutputWidth;
    protected int mOutputHeight;
    public OpenglesDrawer() {
        mRunOnDraw = new LinkedList<Runnable>();
    }
    public final void init() {
        synchronized (this) {
            if (mState == DrawerState.INITIALIZED || mState == DrawerState.DESTROYING) {
                return;
            }
            mState = DrawerState.INITIALING;
        }
        mInitialing = true;
        onInit();
        onInitialized();
    }
    protected  void onInit()
    {

    }
    protected void onInitialized() {
        synchronized (this) {
            mState = DrawerState.INITIALIZED;
        }
        mIsInitialized = true;
    }
    protected void preOnDraw()
    {

    }
    public final void draw()
    {
        if (!isInitialized())
        {
            return;
        }
        preOnDraw();
        runPendingOnDrawTasks();
        onDraw();
    }
    abstract protected void onDraw();
    public void destroy()
    {
        synchronized (this) {
            if (mState == DrawerState.DESTROYED || mState == DrawerState.DESTROYING) {
                return;
            }
            mState = DrawerState.DESTROYING;
        }
        mIsInitialized = false;
        onDestroy();
    }
    protected void onDestroy() {
        synchronized (this) {
            mState = DrawerState.DESTROYED;
        }
    }

    public boolean isInitialized() {
//        return mIsInitialized;
        synchronized (this) {
            return mState == DrawerState.INITIALIZED;
        }
    }
    protected void runPendingOnDrawTasks() {
        while (!mRunOnDraw.isEmpty()) {
            mRunOnDraw.removeFirst().run();
        }
    }
    protected void runOnDraw(final Runnable runnable) {
        synchronized (this)
        {
            if (mState == DrawerState.DESTROYED || mState == DrawerState.DESTROYING)
            {
                return ;
            }
        }
        synchronized (mRunOnDraw) {
            mRunOnDraw.addLast(runnable);
        }
    }

    public void onOutputSizeChanged(final int width, final int height) {
        mOutputWidth = width;
        mOutputHeight = height;

    }
    public int getOutputWidth() {
        return mOutputWidth;
    }

    public int getOutputHeight() {
        return mOutputHeight;
    }
}
