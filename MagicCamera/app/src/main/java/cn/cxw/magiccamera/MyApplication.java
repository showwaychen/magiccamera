package cn.cxw.magiccamera;

import android.app.Application;

import cn.cxw.magiccameralib.MagicCamera;

/**
 * Created by user on 2017/12/21.
 */

public class MyApplication extends Application{
    @Override
    public void onCreate() {
        super.onCreate();
        MagicCamera.getInst().setContext(this);
    }
}
