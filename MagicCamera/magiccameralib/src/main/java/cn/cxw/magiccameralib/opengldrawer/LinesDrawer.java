package cn.cxw.magiccameralib.opengldrawer;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import cn.cxw.magiccameralib.OpenglesDrawer;
import cn.cxw.openglesutils.GlProgram;

/**
 * Created by cxw on 2018/1/1.
 */

public class LinesDrawer extends OpenglesDrawer{
    public static class Point
    {
        public int x = 0;
        public int y = 0;
        public Point(int x, int y)
        {
            this.x = x;
            this.y = y;
        }
    }
    //gl_PointSiz?????
    public static final String VERTEX_SHADER = "" +
            "attribute vec4 position;\n" +
            "attribute float  aPointSize;\n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position;\n" +
            "    gl_PointSize  = 10.0f;\n" +
            "}";
    public static final String FRAGMENT_SHADER = "" +
            "precision mediump float;" +
            "uniform vec4 vColor;" +
            "void main()\n" +
            "{\n" +
            "     gl_FragColor = vColor;\n" +
            "}";

    //同时可以画多个点点组成的线。
    List<Point> mPoints = new ArrayList<>();
    GlProgram mGlProgram = null;
    private FloatBuffer vertexBuffer;
    private int mPositionHandle;
    private int mColorHandle;

    //设置颜色，依次为红绿蓝和透明通道
    float color[] = { 1.0f, 1.0f, 1.0f, 1.0f };

    @Override
    protected void onInit() {
        mGlProgram = new GlProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        mPositionHandle = mGlProgram.getAttribLocation("position");

        //获取片元着色器的vColor成员的句柄
        mColorHandle = mGlProgram.getUniformLocation("vColor");

        super.onInit();
    }
    boolean inflatePoints()
    {
        if (mPoints == null || mPoints.isEmpty())
        {
            return false;
        }
        if (mOutputHeight == 0 || mOutputWidth == 0)
        {
            return false;
        }
        if (mPoints.size() <= 2)
        {
            return false;
        }
        float[] pointarray = new float[mPoints.size() * 4 - 4 ];
        int arrayindex = 0;
        for (Point point:mPoints
                ) {
            pointarray[arrayindex++] = (float)point.x * 2 / mOutputWidth - 1.0f;
            pointarray[arrayindex++] = (float)point.y * 2 / mOutputHeight - 1.0f;
            pointarray[arrayindex++] = (float)point.x * 2 / mOutputWidth - 1.0f;
            pointarray[arrayindex++] = (float)point.y * 2 / mOutputHeight - 1.0f;
        }
        vertexBuffer = ByteBuffer.allocateDirect(pointarray.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.put(pointarray).position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        return true;
    }
    @Override
    protected void onDraw() {
        mGlProgram.useProgram();

        if (inflatePoints())
        {
            GLES20.glLineWidth(3);
            GLES20.glUniform4fv(mColorHandle, 1, color, 0);
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, mPoints.size());
            GLES20.glDisableVertexAttribArray(mPositionHandle);
        }
        mPoints.clear();

    }
    @Override
    public void destroy() {
        if (mGlProgram != null)
        {
            mGlProgram.release();
        }
        super.destroy();
    }
    public void addPoint(Point point)
    {
        mPoints.add(point);
    }
    public void addPoints(List<Point> points)
    {
        mPoints.addAll(points);
    }
}
