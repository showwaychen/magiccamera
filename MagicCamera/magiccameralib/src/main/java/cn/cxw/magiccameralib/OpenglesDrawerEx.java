package cn.cxw.magiccameralib;

import cn.cxw.openglesutils.OpenglCommon;

/**
 * Created by cxw on 2018/3/3.
 */
//                   draw()
//                     |
//                  <- | ->
//  offScreenOnDraw       onScreenOnDraw
public abstract class OpenglesDrawerEx extends OpenglesDrawer {
    protected int mTextureID = OpenglCommon.NO_TEXTURE;
    public void setTexture(int texture)
    {
        mTextureID = texture;
    }
    public abstract void onScreenOnDraw(int width, int height);
    public abstract void offScreenOnDraw();
}
