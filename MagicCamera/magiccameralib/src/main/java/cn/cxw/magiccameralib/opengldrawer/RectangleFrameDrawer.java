package cn.cxw.magiccameralib.opengldrawer;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import cn.cxw.magiccameralib.OpenglesDrawer;
import cn.cxw.openglesutils.GlProgram;

/**
 * Created by cxw on 2017/12/16.
 */

public class RectangleFrameDrawer extends OpenglesDrawer {

    static String TAG = "RectangleFrameDrawer";


    //gl_PointSiz?????
    public static final String VERTEX_SHADER = "" +
            "attribute vec4 position;\n" +
            "uniform mat4 vMatrix;"+
            "attribute float  aPointSize;\n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = vMatrix*position;\n" +
            "    gl_PointSize  = 10.0f;\n" +
            "}";
    public static final String FRAGMENT_SHADER = "" +
            "precision mediump float;" +
            "uniform vec4 vColor;" +
            "void main()\n" +
            "{\n" +
            "     gl_FragColor = vColor;\n" +
            "}";
    static final float LINE[] = {
            -1.0f, -1.0f,
            1.0f, 1.0f
    };
    static final float RECT[] = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            1.0f, 1.0f,
            -1.0f, 1.0f
    };
    GlProgram mGlProgram = null;
    GeometricElement.Rect mRect = new GeometricElement.Rect(0,0,0,0);
    private FloatBuffer vertexBuffer;
    private int mPositionHandle;
    private int mColorHandle;
    int mMatrixHandle;

    //设置颜色，依次为红绿蓝和透明通道
    float color[] = { 1.0f, 1.0f, 1.0f, 1.0f };
    float matrix[] = {
            1f, 0f,0f,0f,
            0f,1f,0f,0f,
            0f,0f,1f,0f,
            0f,0f,0f,1f
    };
    @Override
    protected void onInit() {
        mGlProgram = new GlProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        mPositionHandle = mGlProgram.getAttribLocation("position");
        vertexBuffer = ByteBuffer.allocateDirect(RECT.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.put(RECT).position(0);
        //获取片元着色器的vColor成员的句柄
        mColorHandle = mGlProgram.getUniformLocation("vColor");
        mMatrixHandle = mGlProgram.getUniformLocation("vMatrix");
        super.onInit();
    }

    boolean inflateVertexBuffer()
    {
        if (mRect == null)
        {
            return false;
        }
        if (mOutputHeight <= 0 || mOutputWidth <= 0)
        {
            return false;
        }
//        if (mRect.width() <= 0 || mRect.height() <= 0)
//        {
//            return false;
//        }
        GeometricElement.RotationRect rotationRect = mRect.rotationRect(mRotation);
        rotationRect.convert2GlVexter(RECT, 2, mOutputWidth, mOutputHeight);
//        RECT[0] = (float) mRect.left * 2 / mOutputWidth - 1.0f;
//        RECT[1] = (float) mRect.bottom  * 2/ mOutputHeight- 1.0f;
//        RECT[2] = (float) mRect.right  * 2/ mOutputWidth- 1.0f;
//        RECT[3] = (float) mRect.bottom * 2 / mOutputHeight- 1.0f;
//        RECT[4] = (float) mRect.right  * 2/ mOutputWidth- 1.0f;
//        RECT[5] = (float) mRect.top  * 2/ mOutputHeight- 1.0f;
//        RECT[6] = (float) mRect.left * 2 / mOutputWidth- 1.0f;
//        RECT[7] = (float) mRect.top * 2 / mOutputHeight- 1.0f;
//
//        float centerX = (float)(mRect.left + mRect.width()/2) *2 / mOutputWidth -1f;
//        float centerY = (float)(mRect.top + mRect.height()/2) *2 / mOutputHeight -1f;


//        Log.d(TAG, Arrays.toString(RECT));
//        Log.d(TAG, "centerX = "+ centerX + " y = " + centerY);
        float[] varray = new float[16];
        Matrix.setIdentityM(varray, 0);
//        System.arraycopy(matrix, 0, varray, 0, 16);
//        Matrix.translateM(varray, 0, -centerX, -centerY, 0);
//        Matrix.rotateM(varray, 0, 30, 0f, 0f, 1f);
//        Matrix.translateM(varray, 0, centerX, centerY, 0);
//        float[] rotationMatrix = new float[16];
//        Matrix.rotateM(rotationMatrix, 0, 30, 0.0f, 0f, 0.0f);
//        Matrix.multiplyMV(varray, 0, rotationMatrix, 0, varray, 0);



        vertexBuffer.put(RECT).position(0);

        GLES20.glVertexAttribPointer(mPositionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        //指定vMatrix的值
        GLES20.glUniformMatrix4fv(mMatrixHandle,1,false,varray,0);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        return true;
    }


    @Override
    protected void onDraw() {
        mGlProgram.useProgram();
        if (inflateVertexBuffer())
        {

            GLES20.glLineWidth(3);
            //设置绘制三角形的颜色
            GLES20.glUniform4fv(mColorHandle, 1, color, 0);
            GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, 4);
            GLES20.glDisableVertexAttribArray(mPositionHandle);
        }

        mRect = null;
        vertexBuffer.clear();
    }

    @Override
    public void destroy() {
        if (mGlProgram != null)
        {
            mGlProgram.release();
        }
        super.destroy();
    }

    public void setRect(GeometricElement.Rect rect)
    {
        mRect = rect;
    }

    float mRotation = 0.0f;
    public void setRotation(float angle)
    {
        mRotation = angle;
    }

}
