package cn.cxw.magiccameralib.imagefilter;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import cn.cxw.openglesutils.OpenglCommon;
import cn.cxw.openglesutils.TextureRotationUtil;

/**
 * Created by cxw on 2017/12/17.
 */

public class DisplayFilter extends GPUImageFilter{

    int mTextureid = OpenglCommon.NO_TEXTURE;
    FloatBuffer mCubeBuffer;
    FloatBuffer mDisplayTextureBuffer;
    public DisplayFilter()
    {
        mCubeBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mCubeBuffer.put(CUBE).position(0);
        mDisplayTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.ONSCREEN_TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mDisplayTextureBuffer.put(TextureRotationUtil.ONSCREEN_TEXTURE_NO_ROTATION).position(0);
    }
    public  void setTextureid(int textureid)
    {
        mTextureid = textureid;
    }
    @Override
    protected void onDraw() {
        GLES20.glViewport(0, 0, mOutputWidth, mOutputHeight);
        onDraw(mTextureid, mCubeBuffer, mDisplayTextureBuffer);
    }
}
