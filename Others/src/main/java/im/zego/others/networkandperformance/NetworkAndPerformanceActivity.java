package im.zego.others.networkandperformance;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import im.zego.others.R;

import org.json.JSONObject;

import im.zego.commontools.logtools.AppLogger;
import im.zego.commontools.logtools.LogView;
import im.zego.commontools.logtools.logLinearLayout;
import im.zego.commontools.uitools.ZegoViewUtil;
import im.zego.keycenter.KeyCenter;
import im.zego.keycenter.UserIDHelper;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoApiCalledEventHandler;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.constants.ZegoNetworkSpeedTestType;
import im.zego.zegoexpress.constants.ZegoRoomStateChangedReason;
import im.zego.zegoexpress.constants.ZegoScenario;
import im.zego.zegoexpress.entity.ZegoEngineProfile;
import im.zego.zegoexpress.entity.ZegoNetworkSpeedTestConfig;
import im.zego.zegoexpress.entity.ZegoNetworkSpeedTestQuality;
import im.zego.zegoexpress.entity.ZegoUser;

public class NetworkAndPerformanceActivity extends AppCompatActivity {

    TextView userIDText;
    TextView downConnectCostText;
    TextView downRttText;
    TextView downPacketLostRateText;
    TextView upConnectCostText;
    TextView upRttText;
    TextView upPacketLostRateText;
    EditText expectedDownlinkBitrateEdit;
    EditText expectedUplinkBitrateEdit;
    Button networkSeedTestButton;
    TextView appCpuText;
    TextView appMemoryText;
    TextView appMemoryPercentageText;
    TextView systemMemoryPercentageText;
    TextView roomState;

    String userID;
    String roomID;
    ZegoExpressEngine engine;
    Long appID;
    String appSign;
    ZegoUser user;
    ZegoNetworkSpeedTestConfig config;

    //Store whether the user is testing the network speed
    Boolean isTest = false;

    Handler handler;
    Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_and_performance);
        bindView();
        getAppIDAndUserIDAndAppSign();
        setDefaultValue();
        setLogComponent();
        initEngineAndUser();
        setEventHandler();
        loginRoom();
        setApiCalledResult();
        setNetworkSeedTestButton();
        updatePerformance();
    }
    public void bindView(){
        userIDText = findViewById(R.id.userIDText);
        downConnectCostText = findViewById(R.id.downConnectCost);
        downRttText = findViewById(R.id.downRtt);
        downPacketLostRateText = findViewById(R.id.downLostRate);
        upConnectCostText = findViewById(R.id.upConnectCost);
        upRttText = findViewById(R.id.upRtt);
        upPacketLostRateText = findViewById(R.id.upLostRate);
        expectedDownlinkBitrateEdit = findViewById(R.id.expectedDownlink);
        expectedUplinkBitrateEdit = findViewById(R.id.expectedUplink);
        networkSeedTestButton = findViewById(R.id.networkSpeedTestButton);
        appCpuText = findViewById(R.id.appCpu);
        appMemoryPercentageText = findViewById(R.id.appMemoryPercentage);
        appMemoryText = findViewById(R.id.appMemory);
        systemMemoryPercentageText = findViewById(R.id.systemMemoryPercentage);
        roomState = findViewById(R.id.roomState);
    }
    public void setDefaultValue(){
        roomID = "0031";
        userIDText.setText(userID);
        setTitle(getString(R.string.network_and_performance));

        config =new ZegoNetworkSpeedTestConfig();
        config.testUplink = true;
        config.testDownlink = true;
    }
    //get appID and userID and appSign
    public void getAppIDAndUserIDAndAppSign(){
        appID = KeyCenter.getInstance().getAppID();
        userID = UserIDHelper.getInstance().getUserID();
        appSign = KeyCenter.getInstance().getAppSign();
    }
    public void initEngineAndUser(){
        // Initialize ZegoExpressEngine
        ZegoEngineProfile profile = new ZegoEngineProfile();
        profile.appID = appID;
        profile.appSign = appSign;

        // Here we use the high quality video call scenario as an example,
        // you should choose the appropriate scenario according to your actual situation,
        // for the differences between scenarios and how to choose a suitable scenario,
        // please refer to https://docs.zegocloud.com/article/14940
        profile.scenario = ZegoScenario.HIGH_QUALITY_VIDEO_CALL;

        profile.application = getApplication();
        engine = ZegoExpressEngine.createEngine(profile, null);

        AppLogger.getInstance().callApi("Create ZegoExpressEngine");
        //create the user
        user = new ZegoUser(userID);
    }
    public void loginRoom(){
        //login room
        engine.loginRoom(roomID, user);
        AppLogger.getInstance().callApi("LoginRoom: %s",roomID);
        //enable the camera
        engine.enableCamera(true);
        //enable the microphone
        engine.muteMicrophone(false);
        //enable the speaker
        engine.muteSpeaker(false);
    }
    public void setEventHandler(){
        engine.setEventHandler(new IZegoEventHandler() {
            // The callback triggered when the room connection state changes.
            @Override
            public void onRoomStateChanged(String roomID, ZegoRoomStateChangedReason reason, int errorCode, JSONObject extendedData) {
                ZegoViewUtil.UpdateRoomState(roomState, reason);
            }

            @Override
            public void onNetworkSpeedTestError(int errorCode, ZegoNetworkSpeedTestType type) {
                super.onNetworkSpeedTestError(errorCode, type);
                String error = "";
                if (type.equals(ZegoNetworkSpeedTestType.DOWNLINK)){
                    error = "Download Test Error";
                } else if (type.equals(ZegoNetworkSpeedTestType.UPLINK)){
                    error = "Upload Test Error";
                }
                AppLogger.getInstance().fail("[%d]%s",errorCode,error);
            }

            @Override
            public void onNetworkSpeedTestQualityUpdate(ZegoNetworkSpeedTestQuality quality, ZegoNetworkSpeedTestType type) {
                super.onNetworkSpeedTestQualityUpdate(quality, type);
                if (type.equals(ZegoNetworkSpeedTestType.DOWNLINK)){
                    downConnectCostText.setText(quality.connectCost + "ms");
                    downPacketLostRateText.setText(String.format("%.2f", quality.packetLostRate));
                    downRttText.setText(quality.rtt + "ms");
                } else if (type.equals(ZegoNetworkSpeedTestType.UPLINK)) {
                    upConnectCostText.setText(quality.connectCost + "ms");
                    upPacketLostRateText.setText(String.format("%.2f", quality.packetLostRate));
                    upRttText.setText(quality.rtt + "ms");
                }
            }
        });
    }
    public void setNetworkSeedTestButton(){
        networkSeedTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isTest) {
                    if (expectedDownlinkBitrateEdit.getText().toString().equals("")) {
                        Toast.makeText(NetworkAndPerformanceActivity.this, "Expected Downlink Bitrate cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (expectedUplinkBitrateEdit.getText().toString().equals("")) {
                        Toast.makeText(NetworkAndPerformanceActivity.this, "Expected Uplink Bitrate cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    config.expectedDownlinkBitrate = Integer.valueOf(expectedDownlinkBitrateEdit.getText().toString());
                    config.expectedUplinkBitrate = Integer.valueOf(expectedUplinkBitrateEdit.getText().toString());
                    engine.startNetworkSpeedTest(config);
                    AppLogger.getInstance().callApi("Start network speed test");
                    networkSeedTestButton.setText(getString(R.string.stop_network_speed_test));
                    isTest = true;
                } else {
                    engine.stopNetworkSpeedTest();
                    networkSeedTestButton.setText(getString(R.string.start_network_speed_test));
                    AppLogger.getInstance().callApi("Stop network speed test");
                    isTest = false;
                }
            }
        });
    }
    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, NetworkAndPerformanceActivity.class);
        activity.startActivity(intent);
    }

    // Set log component. It includes a pop-up dialog.
    public void setLogComponent(){
        logLinearLayout logHiddenView = findViewById(R.id.logView);
        logHiddenView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogView logview = new LogView(getApplicationContext());
                logview.show(getSupportFragmentManager(),null);
            }
        });
    }
    public void setApiCalledResult(){
        // Update log with api called results
        ZegoExpressEngine.setApiCalledCallback(new IZegoApiCalledEventHandler() {
            @Override
            public void onApiCalledResult(int errorCode, String funcName, String info) {
                super.onApiCalledResult(errorCode, funcName, info);
                if (errorCode == 0){
                    AppLogger.getInstance().success("[%s]:%s", funcName, info);
                } else {
                    AppLogger.getInstance().fail("[%d]%s:%s", errorCode, funcName, info);
                }
            }
        });
    }
    public void updatePerformance(){
        handler = new Handler(Looper.getMainLooper());
        runnable = new Runnable() {
            @Override
            public void run() {
                appCpuText.setText(DeviceInfoManager.getCurProcessCpuRate()+ "%");
                appMemoryText.setText(DeviceInfoManager.getUsedValue(getApplicationContext()) + "kb");
                appMemoryPercentageText.setText(DeviceInfoManager.getUsedPercentValue(getApplicationContext()));
                systemMemoryPercentageText.setText(DeviceInfoManager.getSystemMemoryRate(getApplicationContext()));
                handler.postDelayed(this, 2000);
            }
        };
        runnable.run();
    }
    @Override
    protected void onDestroy() {
        ZegoExpressEngine.destroyEngine(null);
        handler.removeCallbacks(runnable);
        super.onDestroy();
    }
}