package cn.cxw.magiccameralib.imagefilter;

import android.opengl.GLES20;

import java.nio.FloatBuffer;

import cn.cxw.openglesutils.OpenglCommon;

/**
 * Created by user on 2018/1/8.
 */

public class BlendImage extends GPUImageFilter{

    int textureid = OpenglCommon.NO_TEXTURE;
    FloatBuffer cubeBuffer;
    FloatBuffer textureBuffer;
    public  void setParams(int textureid , FloatBuffer cubeBuffer, FloatBuffer textureBuffer)
    {
        this.textureid = textureid;
        this.cubeBuffer = cubeBuffer;
        this.textureBuffer = textureBuffer;
    }

    @Override
    protected void onDraw() {
        if (textureBuffer == null || textureid == OpenglCommon.NO_TEXTURE || textureBuffer == null)
        {
            return ;
        }
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendEquation(GLES20.GL_FUNC_ADD);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        onDraw(textureid, cubeBuffer, textureBuffer);
        GLES20.glDisable(GLES20.GL_BLEND);
    }
}
