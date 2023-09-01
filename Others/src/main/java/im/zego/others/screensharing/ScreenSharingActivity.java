package im.zego.others.screensharing;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

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
import im.zego.zegoexpress.constants.ZegoAudioChannel;
import im.zego.zegoexpress.constants.ZegoAudioSampleRate;
import im.zego.zegoexpress.constants.ZegoAudioSourceType;
import im.zego.zegoexpress.constants.ZegoPlayerState;
import im.zego.zegoexpress.constants.ZegoPublishChannel;
import im.zego.zegoexpress.constants.ZegoRoomStateChangedReason;
import im.zego.zegoexpress.constants.ZegoScenario;
import im.zego.zegoexpress.constants.ZegoScreenCaptureExceptionType;
import im.zego.zegoexpress.constants.ZegoVideoSourceType;
import im.zego.zegoexpress.entity.ZegoAudioFrameParam;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoEngineProfile;
import im.zego.zegoexpress.entity.ZegoScreenCaptureConfig;
import im.zego.zegoexpress.entity.ZegoUser;
import im.zego.zegoexpress.entity.ZegoVideoConfig;

public class ScreenSharingActivity extends AppCompatActivity {

    TextView userIDText;
    EditText roomIDEdit;
    EditText publishStreamIDEdit;
    Button startScreenCaptureButton;
    EditText playStreamIDEdit;
    Button playButton;
    TextureView playView;
    TextView roomState;
    EditText encodeResolutionWidth;
    EditText encodeResolutionHeight;
    EditText frameRateEdit;
    EditText bitrateEdit;
    Switch captureAudioSwitch;
    Switch captureVideoSwitch;

    Spinner sampleRateSpinner;
    Spinner audioChannelSpinner;

    String userID;
    String publishStreamID;
    String playStreamID;
    String roomID;
    ZegoExpressEngine engine;
    Long appID;
    String appSign;
    ZegoUser user;

    ZegoScreenCaptureConfig screenCaptureConfig = new ZegoScreenCaptureConfig();

    //Store whether the user is playing the stream
    Boolean isPlay = false;
    //Store whether the user is publishing the stream
    Boolean isPublish = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_sharing);
        bindView();
        requestPermission();
        setLogComponent();
        getAppIDAndUserIDAndAppSign();
        setDefaultValue();
        initEngineAndUser();
        prepareScreenCapture();
        setPlayButtonEvent();
        setEventHandler();
        setStartScreenCaptureButtonEvent();
        setApiCalledResult();
        setCaptureParamEvent();
    }
    public void bindView(){
        userIDText = findViewById(R.id.userID);
        roomIDEdit = findViewById(R.id.roomIDEdit);
        publishStreamIDEdit = findViewById(R.id.publishIDEdit);
        startScreenCaptureButton = findViewById(R.id.screenCaptureButton);
        playStreamIDEdit = findViewById(R.id.editPlayStreamID);
        playButton = findViewById(R.id.playButton);
        playView = findViewById(R.id.playView);
        roomState = findViewById(R.id.roomState);
        encodeResolutionWidth = findViewById(R.id.encodeResolutionWidth);
        encodeResolutionHeight = findViewById(R.id.encodeResolutionHeight);
        frameRateEdit = findViewById(R.id.frameRateEdit);
        bitrateEdit = findViewById(R.id.bitrateEdit);
        captureAudioSwitch = findViewById(R.id.captureAudio);
        captureVideoSwitch = findViewById(R.id.captureVideo);
        sampleRateSpinner = findViewById(R.id.sampleRateSpinner);
        audioChannelSpinner = findViewById(R.id.audioChannelSpinner);
    }
    public void setDefaultValue(){
        roomID = "0033";
        publishStreamID = "0033";
        playStreamID = "0033";
        userIDText.setText(userID);
        captureVideoSwitch.setChecked(true);
        captureAudioSwitch.setChecked(true);
        sampleRateSpinner.setSelection(2);
        audioChannelSpinner.setSelection(ZegoAudioChannel.STEREO.value());
        setTitle(getString(R.string.screen_sharing));
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
        profile.scenario = ZegoScenario.BROADCAST;

        profile.application = getApplication();
        engine = ZegoExpressEngine.createEngine(profile, null);

        AppLogger.getInstance().callApi("Create ZegoExpressEngine");
        //create the user
        user = new ZegoUser(userID);
    }

    public void setVideoConfig() {
        ZegoVideoConfig videoConfig = engine.getVideoConfig(ZegoPublishChannel.AUX);
        videoConfig.encodeHeight = Integer.parseInt(encodeResolutionHeight.getText().toString());
        videoConfig.encodeWidth = Integer.parseInt(encodeResolutionWidth.getText().toString());;
        videoConfig.fps = Integer.parseInt(frameRateEdit.getText().toString());
        videoConfig.bitrate = Integer.parseInt(bitrateEdit.getText().toString());
        engine.setVideoConfig(videoConfig, ZegoPublishChannel.AUX);
    }

    public void  prepareScreenCapture(){
        engine.enableHardwareEncoder(true);
        engine.setVideoSource(ZegoVideoSourceType.SCREEN_CAPTURE, ZegoPublishChannel.AUX);
        engine.setAudioSource(ZegoAudioSourceType.SCREEN_CAPTURE, ZegoPublishChannel.AUX);
    }

    public void setStartScreenCaptureButtonEvent(){
        startScreenCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPublish){
                    AppLogger.getInstance().callApi("Stop Publishing Stream: %s",publishStreamID);
                    startScreenCaptureButton.setText(getString(R.string.start_screen_capture));
                    engine.stopScreenCapture();
                    engine.stopPublishingStream(ZegoPublishChannel.AUX);
                    engine.logoutRoom(roomID);
                    isPublish = false;
                } else {
                    setVideoConfig();
                    engine.startScreenCapture(screenCaptureConfig);
                    loginRoom();
                    publishStreamID = publishStreamIDEdit.getText().toString();
                    engine.startPublishingStream(publishStreamID, ZegoPublishChannel.AUX);
                    startScreenCaptureButton.setText(getString(R.string.stop_screen_capture));
                    AppLogger.getInstance().callApi("Start Publishing Stream: %s",publishStreamID);
                    isPublish = true;
                }
            }
        });
    }
    public void loginRoom(){
        roomID = roomIDEdit.getText().toString();
        //login room
        engine.loginRoom(roomID, user);
        AppLogger.getInstance().callApi("LoginRoom: %s",roomID);
    }

    public void setCaptureParamEvent() {
        captureVideoSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                screenCaptureConfig.captureVideo = isChecked;
                engine.updateScreenCaptureConfig(screenCaptureConfig);
            }
        });

        captureAudioSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                screenCaptureConfig.captureAudio = isChecked;
                engine.updateScreenCaptureConfig(screenCaptureConfig);
            }
        });

        sampleRateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ZegoAudioFrameParam param = screenCaptureConfig.audioParam;

                ZegoAudioSampleRate rate = ZegoAudioSampleRate.UNKNOWN;
                switch (position) {
                    case 0:
                        rate = ZegoAudioSampleRate.UNKNOWN;
                        break;
                    case 1:
                        rate = ZegoAudioSampleRate.ZEGO_AUDIO_SAMPLE_RATE_8K;
                        break;
                    case 2:
                        rate = ZegoAudioSampleRate.ZEGO_AUDIO_SAMPLE_RATE_16K;
                        break;
                    case 3:
                        rate = ZegoAudioSampleRate.ZEGO_AUDIO_SAMPLE_RATE_22K;
                        break;
                    case 4:
                        rate = ZegoAudioSampleRate.ZEGO_AUDIO_SAMPLE_RATE_24K;
                        break;
                    case 5:
                        rate = ZegoAudioSampleRate.ZEGO_AUDIO_SAMPLE_RATE_32K;
                        break;
                    case 6:
                        rate = ZegoAudioSampleRate.ZEGO_AUDIO_SAMPLE_RATE_44K;
                        break;
                    case 7:
                        rate = ZegoAudioSampleRate.ZEGO_AUDIO_SAMPLE_RATE_48K;
                        break;
                }
                param.sampleRate = rate;
                engine.updateScreenCaptureConfig(screenCaptureConfig);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        audioChannelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ZegoAudioFrameParam param = screenCaptureConfig.audioParam;
                param.channel = ZegoAudioChannel.getZegoAudioChannel(position);
                engine.updateScreenCaptureConfig(screenCaptureConfig);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public void setPlayButtonEvent(){
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if the user is playing the stream, this button is used to stop playing. Otherwise, this button is used to start publishing.
                if (isPlay){
                    engine.stopPlayingStream(playStreamID);
                    AppLogger.getInstance().callApi("Stop Playing Stream:%s",playStreamID);
                    playButton.setText(getString(R.string.start_playing));
                    isPlay = false;
                } else {
                    playStreamID = playStreamIDEdit.getText().toString();
                    engine.startPlayingStream(playStreamID, new ZegoCanvas(playView));
                    playButton.setText(getString(R.string.stop_playing));
                    AppLogger.getInstance().callApi("Start Playing Stream:%s",playStreamID);
                    isPlay = true;
                }
            }
        });
    }
    public void setEventHandler(){
        engine.setEventHandler(new IZegoEventHandler() {
            // The callback triggered when the room connection state changes.
            @Override
            public void onRoomStateChanged(String roomID, ZegoRoomStateChangedReason reason, int errorCode, JSONObject extendedData) {
                ZegoViewUtil.UpdateRoomState(roomState, reason);
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
                        playButton.setText(ZegoViewUtil.GetEmojiStringByUnicode(ZegoViewUtil.crossEmoji) + getString(R.string.stop_playing));
                    }
                } else {
                    if (isPlay) {
                        playButton.setText(ZegoViewUtil.GetEmojiStringByUnicode(ZegoViewUtil.checkEmoji) + getString(R.string.stop_playing));
                    }
                }
            }

            @Override
            public void onScreenCaptureExceptionOccurred(ZegoScreenCaptureExceptionType exceptionType) {
                super.onScreenCaptureExceptionOccurred(exceptionType);
                AppLogger.getInstance().receiveCallback("screen capture exception occurred: %s", exceptionType);
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
    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, ScreenSharingActivity.class);
        activity.startActivity(intent);
    }
    public void requestPermission() {
        String[] PERMISSIONS_STORAGE = {
                "android.permission.RECORD_AUDIO",
                "android.permission.FOREGROUND_SERVICE"};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.FOREGROUND_SERVICE") != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, "android.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(PERMISSIONS_STORAGE, 101);
            }
        }
    }
    @Override
    protected void onDestroy() {
        ZegoExpressEngine.destroyEngine(null);
        super.onDestroy();
    }
}