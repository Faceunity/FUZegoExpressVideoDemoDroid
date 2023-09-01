package im.zego.expresssample.application;

import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDex;

import com.faceunity.nama.FUConfig;
import com.faceunity.nama.FURenderer;
import com.faceunity.nama.utils.FuDeviceUtils;


/**
 * Created by zego on 2018/10/16.
 */

public class ZegoApplication extends Application {
    private static final String TAG = "ZegoApplication";

    public static ZegoApplication zegoApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        FUConfig.DEVICE_LEVEL = FuDeviceUtils.judgeDeviceLevelGPU();
        zegoApplication = this;

        /**
         * 初始化FaceUnity
         */
        registerFURender();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    /**
     * 初始化相芯SDK
     */
    private void registerFURender() {
        FURenderer.getInstance().setup(getApplicationContext());
    }
}
