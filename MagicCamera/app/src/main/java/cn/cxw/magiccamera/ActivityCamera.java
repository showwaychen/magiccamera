package cn.cxw.magiccamera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import cn.cxw.magiccameralib.glthread.SurfaceViewPreview;
import cn.cxw.magiccameralib.imagefilter.GPUImageBeautifyFilter;
import cn.cxw.openglesutils.glthread.IPreviewView;
import cn.cxw.svideostreamlib.SVideoStream;
import cn.cxw.svideostreamlib.VideoFrameSource;
import cn.cxw.svideostreamlib.VideoStreamConstants;
import cn.cxw.svideostreamlib.VideoStreamProxy;

/**
 * Created by cxw on 2017/12/17.
 */

public class ActivityCamera extends Activity implements SVideoStream.IStreamEventObserver {
   public static void  startActivity(Context context)
   {
       context.startActivity(new Intent(context, ActivityCamera.class));
   }
    FrameLayout mflPreview = null;
    MagicCameraFrameSource mMCFrameSource = null;
    Button mbtnRecord = null;
    Button mbtnAr = null;
    VideoStreamProxy mMagicCameraLive = null;
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        mflPreview = (FrameLayout) findViewById(R.id.fl_preview);
        IPreviewView preview = new SurfaceViewPreview(this);
        mMCFrameSource = new MagicCameraFrameSource();

//        final  MagicCamera lInst = MagicCamera.getInst();
//        lInst.setPreviewView(preview);
//        lInst.setCameraSize(640, 480);
        mMCFrameSource.setCameraSize(640, 480);
        mMCFrameSource.setPreviewView(preview);
        mflPreview.addView(preview.getView());

        findViewById(R.id.button_choose_filter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                lInst.enableBeauty(!MagicCamera.getInst().isBeautyEnable());
                mMCFrameSource.getMagicCamera().setOpenglDrawer(new GPUImageBeautifyFilter());
            }
        });
        findViewById(R.id.img_switch_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMCFrameSource.switchCamera();
            }
        });

        mbtnRecord = (Button) findViewById(R.id.btn_record);
        mbtnRecord.setEnabled(false);
        mbtnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean streaming = mMagicCameraLive.isInStreaming();
                if (streaming)
                {
                    mMagicCameraLive.stopStream();
                }
                else
                {
                    mMagicCameraLive.setRecordPath(Environment.getExternalStorageDirectory() + "/magiccamera_record.mp4");
                    mMagicCameraLive.startStream();
                }
            }
        });
        mMCFrameSource.setObserver(new VideoFrameSource.VideoFrameSourceObserver() {
            @Override
            public void onStarted() {
                mbtnRecord.setEnabled(true);
            }
        });
        mMagicCameraLive = new VideoStreamProxy();
        mMagicCameraLive.setVideoFrameSource(mMCFrameSource);
        mMagicCameraLive.setStreamEventObserver(this);

        //copy data to sdcard
    }
    @Override
    protected void onDestroy() {

        super.onDestroy();
        mMagicCameraLive.stopStream();
        mMagicCameraLive.setStreamEventObserver(null);
        mMagicCameraLive.destroyStream();
//        mGPUVideoSource.setObserver(null);
//        mGPUVideoSource.stopPreview();
//        mGPUVideoSource.setPreviewView(null);
    }
    void refreshUi()
    {
        boolean streaming = mMagicCameraLive.isInStreaming();
        mbtnRecord.setText(streaming?"stop record":"start record");
        mbtnRecord.setEnabled(true);

    }
    @Override
    public void onEvent(final int eventid, final int error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (eventid)
                {
                    case VideoStreamConstants.SE_StreamStarted:
                        refreshUi();
                        Toast.makeText(ActivityCamera.this, "stream started", Toast.LENGTH_LONG).show();
                        break;
                    case VideoStreamConstants.SE_LiveConnected:
                        Toast.makeText(ActivityCamera.this, "live connected ok", Toast.LENGTH_LONG).show();
                        break;
                    case VideoStreamConstants.SE_StreamFailed:
                        refreshUi();
                        Toast.makeText(getApplicationContext(), VideoStreamConstants.getErrorDes(error), Toast.LENGTH_LONG).show();
                        break;
                    case VideoStreamConstants.SE_StreamStopped:
                        refreshUi();
                        Toast.makeText(ActivityCamera.this, "stream stopped", Toast.LENGTH_LONG).show();
                        break;
                }
            }
        });
    }
}
