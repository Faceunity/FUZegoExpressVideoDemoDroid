package im.zego.advancedaudioprocessing.originalaudiodataacquisition;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONObject;

import java.nio.ByteBuffer;

import im.zego.advancedaudioprocessing.R;
import im.zego.advancedaudioprocessing.audio3a.Audio3aActivity;
import im.zego.commontools.logtools.AppLogger;
import im.zego.commontools.logtools.LogView;
import im.zego.commontools.logtools.logLinearLayout;
import im.zego.commontools.uitools.ZegoViewUtil;
import im.zego.keycenter.KeyCenter;
import im.zego.keycenter.UserIDHelper;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoApiCalledEventHandler;
import im.zego.zegoexpress.callback.IZegoAudioDataHandler;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.constants.ZegoAudioChannel;
import im.zego.zegoexpress.constants.ZegoAudioDataCallbackBitMask;
import im.zego.zegoexpress.constants.ZegoAudioSampleRate;
import im.zego.zegoexpress.constants.ZegoPlayerState;
import im.zego.zegoexpress.constants.ZegoPublisherState;
import im.zego.zegoexpress.constants.ZegoRoomState;
import im.zego.zegoexpress.constants.ZegoRoomStateChangedReason;
import im.zego.zegoexpress.constants.ZegoScenario;
import im.zego.zegoexpress.entity.ZegoAudioConfig;
import im.zego.zegoexpress.entity.ZegoAudioFrameParam;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoEngineProfile;
import im.zego.zegoexpress.entity.ZegoRoomConfig;
import im.zego.zegoexpress.entity.ZegoUser;

public class OriginalAudioDataAcquisitionActivity extends AppCompatActivity {

    TextView roomState;
    TextView userIDText;
    TextureView preview;
    TextureView playView;
    Button startPublishingButton;
    Button startPlayingButton;
    EditText publishStreamIDEdit;
    EditText playStreamIDEdit;
    SwitchMaterial enableCallbackSwitch;

    Long appID;
    String userID;
    String appSign;
    String roomID;
    String playStreamID;
    String publishStreamID;
    ZegoExpressEngine engine;
    ZegoUser user;
    ZegoAudioFrameParam param=new ZegoAudioFrameParam();
    int bitmask = 0;

    //Store whether the user is publishing the stream
    boolean isPublish = false;
    //Store whether the user is playing the stream
    boolean isPlay = false;
    boolean isCapturePrintLog = false;
    boolean isMixPrintLog = false;
    boolean isPlaybackPrintLog = false;
    boolean isEnableCallback = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_original_audio_data_acquisition);
        bindView();
        setLogComponent();
        getAppIDAndUserIDAndAppSign();
        setDefaultValue();
        initEngineAndUser();
        loginRoom();
        setEventHandler();
        setStartPublishButtonEvent();
        setStartPlayButtonEvent();
        setApiCalledResult();
        setEnableCallbackSwitch();
        setAudioDataHandler();
    }
    public void bindView(){
        roomState = findViewById(R.id.roomState);
        userIDText = findViewById(R.id.userIDText);
        preview = findViewById(R.id.PreviewView);
        playView = findViewById(R.id.PlayView);
        startPublishingButton = findViewById(R.id.startPublishButton);
        startPlayingButton = findViewById(R.id.startPlayButton);
        publishStreamIDEdit = findViewById(R.id.editPublishStreamID);
        playStreamIDEdit = findViewById(R.id.editPlayStreamID);
        enableCallbackSwitch = findViewById(R.id.callbackSwitch);
    }
    //get appID and userID and appSign
    public void getAppIDAndUserIDAndAppSign(){
        appID = KeyCenter.getInstance().getAppID();
        userID = UserIDHelper.getInstance().getUserID();
        appSign = KeyCenter.getInstance().getAppSign();
    }
    public void setDefaultValue(){
        //set default publish  and play streamID
        playStreamID = "0021";
        publishStreamID = "0021";
        roomID = "0021";

        userIDText.setText(userID);
        setTitle(getString(R.string.original_audio_data_acquisition));

        // Enable obtaining raw audio data
        param.channel = ZegoAudioChannel.STEREO;
        param.sampleRate = ZegoAudioSampleRate.ZEGO_AUDIO_SAMPLE_RATE_8K;
        // Add bitmask and turn on the switch of collecting audio data
        // The bitmask values corresponding to capture, playback and mixing are: CAPTURED=1, PLAYBACK=2, MIXED=4.
        // The final value of bitmask is 7, which means that capture callback,
        // playback callback, and mixing callback will be triggered at the same time.
        // 采集，拉流，混合对应的位掩码值分别是：CAPTURED=1，PLAYBACK=2, MIXED=4，bitmask最终得到的值为7，表示会同时触发采集、拉流、混合的原始数据回调。
        bitmask |= ZegoAudioDataCallbackBitMask.CAPTURED.value();
        bitmask |= ZegoAudioDataCallbackBitMask.MIXED.value();
        bitmask |= ZegoAudioDataCallbackBitMask.PLAYBACK.value();
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
        // Create the user
        user = new ZegoUser(userID);
    }
    public void loginRoom(){
        // Login room
        engine.loginRoom(roomID, user);
        AppLogger.getInstance().callApi("LoginRoom: %s",roomID);
        // Enable the camera
        engine.enableCamera(true);
        // Enable the microphone
        engine.muteMicrophone(false);
        // Enable the speaker
        engine.muteSpeaker(false);
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
    public void setEventHandler(){
        engine.setEventHandler(new IZegoEventHandler() {
            // The callback triggered when the room state changes.
            @Override
            public void onRoomStateChanged(String roomID, ZegoRoomStateChangedReason reason, int errorCode, JSONObject extendedData) {
                ZegoViewUtil.UpdateRoomState(roomState, reason);
            }
            // The callback triggered when the state of stream publishing changes.
            @Override
            public void onPublisherStateUpdate(String streamID, ZegoPublisherState state, int errorCode, JSONObject extendedData) {
                super.onPublisherStateUpdate(streamID, state, errorCode, extendedData);
                // If the state is PUBLISHER_STATE_NO_PUBLISH and the errcode is not 0, it means that stream publishing has failed
                // and no more retry will be attempted by the engine. At this point, the failure of stream publishing can be indicated
                // on the UI of the App.
                if(errorCode != 0 && state.equals(ZegoPublisherState.NO_PUBLISH)) {
                    if (isPublish) {
                        startPublishingButton.setText(ZegoViewUtil.GetEmojiStringByUnicode(ZegoViewUtil.crossEmoji) + getString(R.string.stop_publishing));
                    }
                } else {
                    if (isPublish) {
                        startPublishingButton.setText(ZegoViewUtil.GetEmojiStringByUnicode(ZegoViewUtil.checkEmoji) + getString(R.string.stop_publishing));
                    }
                }
            }
            // The callback triggered when the state of stream playing changes.
            @Override
            public void onPlayerStateUpdate(String streamID, ZegoPlayerState state, int errorCode, JSONObject extendedData) {
                super.onPlayerStateUpdate(streamID, state, errorCode, extendedData);
                // If the state is PLAYER_STATE_NO_PLAY and the errcode is not 0, it means that stream playing has failed and
                // no more retry will be attempted by the engine. At this point, the failure of stream playing can be indicated
                // on the UI of the App.
                if(errorCode != 0 && state.equals(ZegoPlayerState.NO_PLAY)) {
                    if (isPlay) {
                        startPlayingButton.setText(ZegoViewUtil.GetEmojiStringByUnicode(ZegoViewUtil.crossEmoji) + getString(R.string.stop_playing));
                    }
                } else {
                    if (isPlay) {
                        startPlayingButton.setText(ZegoViewUtil.GetEmojiStringByUnicode(ZegoViewUtil.checkEmoji) + getString(R.string.stop_playing));
                    }
                }
            }

        });
    }
    public void setStartPublishButtonEvent(){
        startPublishingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPublish) {
                    engine.startPreview(new ZegoCanvas(preview));
                    publishStreamID = publishStreamIDEdit.getText().toString();
                    // Start publishing stream
                    engine.startPublishingStream(publishStreamID);
                    AppLogger.getInstance().callApi("Start Publishing Stream:%s",publishStreamID);
                    startPublishingButton.setText(getResources().getString(R.string.stop_publishing));
                    isPublish = true;
                } else {
                    stopPublish();
                }
            }
        });
    }
    public void stopPublish(){
        // Stop preview
        engine.stopPreview();
        // Stop publishing
        engine.stopPublishingStream();
        AppLogger.getInstance().callApi("Stop Publishing Stream:%s",publishStreamID);
        isPublish = false;
        startPublishingButton.setText(getResources().getString(R.string.start_publishing));
    }
    public void setStartPlayButtonEvent(){
        startPlayingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPlay){
                    playStreamID = playStreamIDEdit.getText().toString();
                    if (playStreamID!=null){
                        // Start playing
                        engine.startPlayingStream(playStreamID,new ZegoCanvas(playView));
                        AppLogger.getInstance().callApi("Start Playing Stream:%s",playStreamID);
                        isPlay = true;
                        startPlayingButton.setText(getResources().getString(R.string.stop_playing));
                    }
                } else {
                    // Stop playing
                    engine.stopPlayingStream(playStreamID);
                    AppLogger.getInstance().callApi("Stop Playing Stream:%s",playStreamID);
                    isPlay = false;
                    startPlayingButton.setText(getResources().getString(R.string.start_playing));
                }
            }
        });
    }
    public void setEnableCallbackSwitch(){
        enableCallbackSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    engine.startAudioDataObserver(bitmask, param);
                } else {
                    engine.stopAudioDataObserver();
                }
                isEnableCallback = isChecked;
                AppLogger.getInstance().callApi("%s Audio Data Observer", isChecked ? "Start" : "Stop");
                isCapturePrintLog = false;
                isMixPrintLog = false;
                isPlaybackPrintLog = false;
            }
        });
    }
    public void setAudioDataHandler(){
        // Set Raw Audio Data Callback
        // 设置原始音频数据回调
        engine.setAudioDataHandler(new IZegoAudioDataHandler() {
            @Override
            public void onCapturedAudioData(ByteBuffer data, int dataLength, ZegoAudioFrameParam param) {
                // 本地采集音频数据，推流后可收到回调
                // Collect local audio data , you can receive a callback after publishing the stream
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isCapturePrintLog && isEnableCallback) {
                            AppLogger.getInstance().receiveCallback("onCapturedAudioData");
                            isCapturePrintLog = true;
                        }
                    }
                });
            }

            @Override
            public void onPlaybackAudioData (ByteBuffer data, int dataLength, ZegoAudioFrameParam param) {
                // 远端拉流音频数据，开始拉流后可收到回调
                // Play audio data remotely, you can receive a callback after you start streaming
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isPlaybackPrintLog && isEnableCallback) {
                            AppLogger.getInstance().receiveCallback("onPlaybackAudioData");
                            isPlaybackPrintLog = true;
                        }
                    }
                });
            }

            @Override
            public void onMixedAudioData(ByteBuffer data, int dataLength, ZegoAudioFrameParam param) {
                // 本地采集与远端拉流声音混合后的音频数据回调
                // Receive audio data after mixing local collection and remote audio data */
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isMixPrintLog && isEnableCallback) {
                            AppLogger.getInstance().receiveCallback("onMixedAudioData");
                            isMixPrintLog = true;
                        }
                    }
                });
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
    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, OriginalAudioDataAcquisitionActivity.class);
        activity.startActivity(intent);

    }
    @Override
    protected void onDestroy() {
        ZegoExpressEngine.destroyEngine(null);
        super.onDestroy();
    }
}