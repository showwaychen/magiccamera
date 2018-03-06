package cn.cxw.magiccameralib;

import java.util.LinkedList;

/**
 * Created by cxw on 2017/12/16.
 */

public abstract  class OpenglesDrawer {
    private final LinkedList<Runnable> mRunOnDraw;
    boolean mIsInitialized = false;

    protected int mOutputWidth;
    protected int mOutputHeight;
    public OpenglesDrawer() {
        mRunOnDraw = new LinkedList<Runnable>();
    }
    public final void init() {
        onInit();
        mIsInitialized = true;
        onInitialized();
    }
    protected  void onInit()
    {

    }
    protected void onInitialized() {
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
        mIsInitialized = false;
        onDestroy();
    }
    protected void onDestroy() {
    }

    public boolean isInitialized() {
        return mIsInitialized;
    }
    protected void runPendingOnDrawTasks() {
        while (!mRunOnDraw.isEmpty()) {
            mRunOnDraw.removeFirst().run();
        }
    }
    protected void runOnDraw(final Runnable runnable) {
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
