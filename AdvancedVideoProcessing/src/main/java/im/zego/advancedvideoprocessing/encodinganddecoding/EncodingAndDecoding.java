package im.zego.advancedvideoprocessing.encodinganddecoding;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import im.zego.commontools.logtools.AppLogger;
import im.zego.commontools.logtools.LogView;
import im.zego.commontools.logtools.logLinearLayout;
import im.zego.commontools.uitools.ZegoViewUtil;
import im.zego.advancedvideoprocessing.R;
import im.zego.keycenter.KeyCenter;
import im.zego.keycenter.UserIDHelper;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoApiCalledEventHandler;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.constants.ZegoPlayerState;
import im.zego.zegoexpress.constants.ZegoPublishChannel;
import im.zego.zegoexpress.constants.ZegoPublisherState;
import im.zego.zegoexpress.constants.ZegoRoomStateChangedReason;
import im.zego.zegoexpress.constants.ZegoScenario;
import im.zego.zegoexpress.constants.ZegoTrafficControlFocusOnMode;
import im.zego.zegoexpress.constants.ZegoTrafficControlMinVideoBitrateMode;
import im.zego.zegoexpress.constants.ZegoVideoCodecID;
import im.zego.zegoexpress.constants.ZegoVideoConfigPreset;
import im.zego.zegoexpress.constants.ZegoVideoStreamType;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoEngineProfile;
import im.zego.zegoexpress.entity.ZegoPlayStreamQuality;
import im.zego.zegoexpress.entity.ZegoPublishStreamQuality;
import im.zego.zegoexpress.entity.ZegoUser;
import im.zego.zegoexpress.entity.ZegoVideoConfig;

public class EncodingAndDecoding extends AppCompatActivity {
    TextView userIDText;
    TextureView preview;
    TextureView playView;
    EditText editPublishStreamID;
    EditText editPlayStreamID;
    Button startPublishingButton;
    Button startPlayingButton;

    AppCompatSpinner codecIDSpinner;
    EditText editCaptureWidth;
    EditText editCaptureHeight;
    EditText editEncodeWidth;
    EditText editEncodeHeight;
    EditText editVideoFPS;
    EditText editVideoBitrate;
    Button videoConfigButton;

    SwitchMaterial hardwareEncoderSwitch;
    SwitchMaterial hardwareDecoderSwitch;
    AppCompatSpinner videoLayerSpinner;
    AppCompatSpinner encoderProfileSpinner;

    SwitchMaterial trafficControlSwitch;
    SwitchMaterial trafficControlFocusOnSwitch;
    EditText editTrafficControlMode;
    EditText editTrafficControlWidth;
    EditText editTrafficControlHeight;
    Button trafficControlResolutionButton;
    EditText editTrafficControlFPS;
    EditText editTrafficControlBitrate;
    AppCompatSpinner trafficControlBitrateModeSpinner;

    EditText editSEI;
    Button sendSEIButton;

    TextView roomState;

    TextView publisherResolutionText;
    TextView publisherVideoBitrateText;
    TextView publisherAudioBitrateText;
    TextView publisherFpsText;
    TextView playerResolutionText;
    TextView playerVideoBitrateText;
    TextView playerAudioBitrateText;
    TextView playerFpsText;

    Long appID;
    String roomID = "0012";
    String userID;
    String appSign;
    String publishStreamID;
    String playStreamID;
    boolean isPublish = false;
    boolean isPlay = false;

    ZegoExpressEngine engine;
    ZegoUser user;
    ZegoVideoConfig videoConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encoding_and_decoding);
        bindView();
        requestPermission();
        getAppIDAndUserIDAndAppSign();
        initEngineAndUser();
        loginRoom();
        setDefaultConfig();
        setStartPlayButtonEvent();
        setStartPublishButtonEvent();
        setVideoConfigUpdateEvent();
        setHardwareDecoderSwitchEvent();
        setHardwareEncoderSwitchEvent();
        setCodecIDSpinnerEvent();
        setVideoEncoderProfileSpinner();
        setTrafficControlEvent();
        setSendSEIEvent();
        setEventHandler();
        setLogComponent();
        setVideoLayerSpinner();
        setApiCalledResult();
    }
    public void bindView(){
        userIDText = findViewById(R.id.userID);
        preview = findViewById(R.id.PreviewView);
        playView = findViewById(R.id.PlayView);
        editPublishStreamID = findViewById(R.id.editPublishStreamID);
        editPlayStreamID = findViewById(R.id.editPlayStreamID);
        startPlayingButton = findViewById(R.id.startPlayButton);
        startPublishingButton = findViewById(R.id.startPublishButton);

        roomState = findViewById(R.id.roomState);

        codecIDSpinner = findViewById(R.id.codecIDSpinner);
        editCaptureWidth = findViewById(R.id.captureResolutionWidth);
        editCaptureHeight = findViewById(R.id.captureResolutionHeight);
        editEncodeWidth = findViewById(R.id.encodeResolutionWidth);
        editEncodeHeight = findViewById(R.id.encodeResolutionHeight);
        editVideoFPS = findViewById(R.id.videoFps);
        editVideoBitrate = findViewById(R.id.videoBitrate);
        videoConfigButton = findViewById(R.id.videoConfigUpdate);

        hardwareDecoderSwitch = findViewById(R.id.hardwareDecodeSwitch);
        hardwareEncoderSwitch = findViewById(R.id.hardwareEncodeSwitch);

        videoLayerSpinner = findViewById(R.id.videoLayerSpinner);
        encoderProfileSpinner = findViewById(R.id.encoderProfileSpinner);

        trafficControlSwitch = findViewById(R.id.trafficControlSwitch);
        trafficControlFocusOnSwitch = findViewById(R.id.trafficControlFocusOnSwitch);
        editTrafficControlMode = findViewById(R.id.trafficControlMode);
        editTrafficControlWidth = findViewById(R.id.trafficControlResolutionWidth);
        editTrafficControlHeight = findViewById(R.id.trafficControlResolutionHeight);
        trafficControlResolutionButton = findViewById(R.id.trafficControlResolutionUpdateButton);
        editTrafficControlFPS = findViewById(R.id.trafficControlFPS);
        editTrafficControlBitrate = findViewById(R.id.trafficControlBitrate);
        trafficControlBitrateModeSpinner = findViewById(R.id.trafficControlBitrateModeSpinner);

        editSEI = findViewById(R.id.seiEditText);
        sendSEIButton = findViewById(R.id.sendSEIButton);

        publisherResolutionText = findViewById(R.id.publisherResolution);
        publisherVideoBitrateText = findViewById(R.id.publisherVideoBitrate);
        publisherAudioBitrateText = findViewById(R.id.publisherAudioBitrate);
        publisherFpsText = findViewById(R.id.publisherVdeoFps);
        playerResolutionText = findViewById(R.id.playerResolution);
        playerVideoBitrateText = findViewById(R.id.playerVideoBitrate);
        playerAudioBitrateText = findViewById(R.id.playerAudioBitrate);
        playerFpsText = findViewById(R.id.playerVideoFps);
    }
    // request for permission
    public void requestPermission() {
        String[] PERMISSIONS_STORAGE = {
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO"};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.CAMERA") != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, "android.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(PERMISSIONS_STORAGE, 101);
            }
        }
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
    public void setDefaultConfig(){
        //set the default video configuration
        videoConfig = new ZegoVideoConfig(ZegoVideoConfigPreset.PRESET_360P);
        //set default play streamID
        playStreamID = "0012";
        //set default publish StreamID
        publishStreamID = "0012";
        userIDText.setText(userID);
        setTitle(getString(R.string.encoding_decoding));
    }
    public void setStartPublishButtonEvent(){
        startPublishingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if the user is publishing the stream, this button is used to stop publishing. Otherwise, this button is used to start publishing.
                if (!isPublish) {
                    engine.startPreview(new ZegoCanvas(preview));
                    publishStreamID = editPublishStreamID.getText().toString();
                    // Start publishing stream
                    engine.startPublishingStream(publishStreamID);
                    AppLogger.getInstance().callApi("Start Publishing Stream:%s",publishStreamID);
                    startPublishingButton.setText(getResources().getString(R.string.stop_publishing));
                    isPublish = true;
                } else {
                    stopPublishing();
                }
            }
        });
    }
    public void setStartPlayButtonEvent(){
        startPlayingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if the user is playing the stream, this button is used to stop playing. Otherwise, this button is used to start publishing.
                if (!isPlay){
                    playStreamID = editPlayStreamID.getText().toString();
                    engine.startPlayingStream(playStreamID,new ZegoCanvas(playView));
                    AppLogger.getInstance().callApi("Start Playing Stream:%s",playStreamID);
                    isPlay = true;
                    startPlayingButton.setText(getResources().getString(R.string.stop_playing));
                } else {
                    stopPlaying();
                }
            }
        });
    }
    public void stopPublishing(){
        engine.stopPreview();
        engine.stopPublishingStream();
        AppLogger.getInstance().callApi("Stop Publishing Stream:%s",publishStreamID);
        isPublish = false;
        startPublishingButton.setText(getResources().getString(R.string.start_publishing));
    }
    public void stopPlaying(){
        engine.stopPlayingStream(playStreamID);
        AppLogger.getInstance().callApi("Stop Playing Stream:%s",playStreamID);
        isPlay = false;
        startPlayingButton.setText(getResources().getString(R.string.start_playing));
    }

    public void setVideoConfigUpdateEvent() {
        videoConfigButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                videoConfig.captureWidth = Integer.parseInt(editCaptureWidth.getText().toString());
                videoConfig.captureHeight = Integer.parseInt(editCaptureHeight.getText().toString());
                videoConfig.encodeWidth = Integer.parseInt(editEncodeWidth.getText().toString());
                videoConfig.encodeHeight = Integer.parseInt(editEncodeHeight.getText().toString());
                videoConfig.fps = Integer.parseInt(editVideoFPS.getText().toString());
                videoConfig.bitrate = Integer.parseInt(editVideoBitrate.getText().toString());

                engine.setVideoConfig(videoConfig);
            }
        });
    }

    public void setHardwareEncoderSwitchEvent(){
        hardwareEncoderSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isPublish) {
                    stopPublishing();
                }
                // Enable hardware encoder
                if (isChecked){
                    engine.enableHardwareEncoder(true);
                    AppLogger.getInstance().callApi("Enable Hardware Encoder");
                } else {
                    engine.enableHardwareEncoder(false);
                    AppLogger.getInstance().callApi("Disable Hardware Encoder");
                }
            }
        });
    }
    public void setHardwareDecoderSwitchEvent(){
        hardwareDecoderSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isPlay) {
                    stopPlaying();
                }
                // Enable hardware decoder
                if (isChecked){
                    engine.enableHardwareDecoder(true);
                } else {
                    engine.enableHardwareDecoder(false);
                }
            }
        });
    }
    public void setCodecIDSpinnerEvent(){
        codecIDSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] options = getResources().getStringArray(R.array.advancedVideoProcessingVideoCodecID);
                switch (options[position]){
                    // set CodecID and update the scalable video coding status. If the user set codecID as ZegoVideoCodecID.SVC,
                    // the scalable video coding will be turned on. Otherwise, it will be turned off.
                    case "SVC":
                        videoConfig.setCodecID(ZegoVideoCodecID.SVC);
                        AppLogger.getInstance().callApi("Set CodecID:ZegoVideoCodecID.SVC");
                        break;
                    case "H265":
                        videoConfig.setCodecID(ZegoVideoCodecID.H265);
                        AppLogger.getInstance().callApi("Set CodecID:ZegoVideoCodecID.H265");
                        break;
                    case "DEFAULT":
                        videoConfig.setCodecID(ZegoVideoCodecID.DEFAULT);
                        AppLogger.getInstance().callApi("Set CodecID:ZegoVideoCodecID.DEFAULT");
                        break;
                    case "VP8":
                        videoConfig.setCodecID(ZegoVideoCodecID.VP8);
                        AppLogger.getInstance().callApi("Set CodecID:ZegoVideoCodecID.VP8");
                        break;
                    case "H264DualStream":
                        videoConfig.setCodecID(ZegoVideoCodecID.H264_DUAL_STREAM);
                        AppLogger.getInstance().callApi("Set CodecID:ZegoVideoCodecID.H264_DUAL_STREAM");
                        break;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public void setVideoLayerSpinner(){
        videoLayerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String options[] = getResources().getStringArray(R.array.videoLayer);
                switch (options[position]){
                    case "DEFAULT":
                        engine.setPlayStreamVideoType(playStreamID,ZegoVideoStreamType.DEFAULT);
                        break;
                    case "SMALL":
                        engine.setPlayStreamVideoType(playStreamID,ZegoVideoStreamType.SMALL);
                        break;
                    case "BIG":
                        engine.setPlayStreamVideoType(playStreamID,ZegoVideoStreamType.BIG);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public void setVideoEncoderProfileSpinner() {
        encoderProfileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                String params = "{\"method\": \"express.video.set_video_encoder_profile\",\"params\": {\"profile\": " + position + ",\"channel\": 0 } }";
                engine.callExperimentalAPI(params);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public void setTrafficControlEvent() {
        trafficControlSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String param = editTrafficControlMode.getText().toString();
                if (param.length() > 0) {
                    int property = Integer.parseInt(param);
                    engine.enableTrafficControl(isChecked, property);
                }
            }
        });

        trafficControlFocusOnSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                {
                    engine.setTrafficControlFocusOn(ZegoTrafficControlFocusOnMode.ZEGO_TRAFFIC_CONTROL_FOUNS_ON_REMOTE);
                }
                else
                {
                    engine.setTrafficControlFocusOn(ZegoTrafficControlFocusOnMode.ZEGO_TRAFFIC_CONTROL_FOUNS_ON_LOCAL_ONLY);
                }
            }
        });

        editTrafficControlMode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String param = editTrafficControlMode.getText().toString();
                if (param.length() > 0) {
                    int property = Integer.parseInt(param);
                    engine.enableTrafficControl(trafficControlSwitch.isChecked(), property);
                }
            }
        });

        trafficControlResolutionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int width = Integer.parseInt(editTrafficControlWidth.getText().toString());
                int height = Integer.parseInt(editTrafficControlHeight.getText().toString());
                engine.setMinVideoResolutionForTrafficControl(width, height, ZegoPublishChannel.MAIN);
            }
        });

        editTrafficControlFPS.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                int fps = Integer.parseInt(editTrafficControlFPS.getText().toString());
                engine.setMinVideoFpsForTrafficControl(fps, ZegoPublishChannel.MAIN);
            }
        });

        editTrafficControlBitrate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (editTrafficControlBitrate.getText().toString().length() > 0) {
                    int bitrate = Integer.parseInt(editTrafficControlBitrate.getText().toString());
                    int bitrateMode = trafficControlBitrateModeSpinner.getSelectedItemPosition();
                    engine.setMinVideoBitrateForTrafficControl(bitrate, ZegoTrafficControlMinVideoBitrateMode.getZegoTrafficControlMinVideoBitrateMode(bitrateMode));
                }
            }
        });

        trafficControlBitrateModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int bitrate = Integer.parseInt(editTrafficControlBitrate.getText().toString());
                engine.setMinVideoBitrateForTrafficControl(bitrate, ZegoTrafficControlMinVideoBitrateMode.getZegoTrafficControlMinVideoBitrateMode(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

    public void setSendSEIEvent() {
        sendSEIButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    byte[] param = editSEI.getText().toString().getBytes("UTF-8");
                    engine.sendSEI(param);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, EncodingAndDecoding.class);
        activity.startActivity(intent);
    }

    public void setEventHandler(){
        engine.setEventHandler(new IZegoEventHandler() {
            // The callback triggered when the room connection state changes.
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

            @Override
            public void onPlayerRecvSEI(String streamID, byte[] data) {
                super.onPlayerRecvSEI(streamID, data);

                try {
                    AppLogger.getInstance().receiveCallback("Recv sei data. streamID:" + streamID + " data:" + new String(data, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onPublisherVideoSizeChanged(int width, int height, ZegoPublishChannel channel)
            {
                super.onPublisherVideoSizeChanged(width, height, channel);
                publisherResolutionText.setText(width + "x" + height);
            }

            @Override
            public void onPublisherQualityUpdate(String streamID, ZegoPublishStreamQuality quality) {
                super.onPublisherQualityUpdate(streamID, quality);
                publisherVideoBitrateText.setText(String.format("%.2f", quality.videoKBPS) + "kbps");
                publisherAudioBitrateText.setText(String.format("%.2f", quality.audioKBPS) + "kbps");
                publisherFpsText.setText(String.format("%.2f",quality.videoSendFPS) + "f/s");
            }

            @Override
            public void onPlayerVideoSizeChanged(String streamID, int width, int height)
            {
                super.onPlayerVideoSizeChanged(streamID, width, height);
                playerResolutionText.setText(width + "x" + height);
            }

            @Override
            public void onPlayerQualityUpdate(String streamID, ZegoPlayStreamQuality quality) {
                super.onPlayerQualityUpdate(streamID, quality);
                playerVideoBitrateText.setText(String.format("%.2f", quality.videoKBPS) + "kbps");
                playerAudioBitrateText.setText(String.format("%.2f", quality.audioKBPS) + "kbps");
                playerFpsText.setText(String.format("%.2f",quality.videoRecvFPS) + "f/s");
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
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release the rendering class
        engine.logoutRoom(roomID);
        ZegoExpressEngine.destroyEngine(null);
    }
}