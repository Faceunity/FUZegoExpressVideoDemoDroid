package im.zego.advancedaudioprocessing.audio3a;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONObject;

import java.util.HashMap;

import im.zego.advancedaudioprocessing.R;
import im.zego.advancedaudioprocessing.earreturnandchannelsettings.EarReturnandChannelSettingsActivity;
import im.zego.commontools.logtools.AppLogger;
import im.zego.commontools.logtools.LogView;
import im.zego.commontools.logtools.logLinearLayout;
import im.zego.commontools.uitools.ZegoViewUtil;
import im.zego.keycenter.KeyCenter;
import im.zego.keycenter.UserIDHelper;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoApiCalledEventHandler;
import im.zego.zegoexpress.callback.IZegoDestroyCompletionCallback;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.callback.IZegoMediaPlayerLoadResourceCallback;
import im.zego.zegoexpress.constants.ZegoAECMode;
import im.zego.zegoexpress.constants.ZegoANSMode;
import im.zego.zegoexpress.constants.ZegoPlayerState;
import im.zego.zegoexpress.constants.ZegoPublisherState;
import im.zego.zegoexpress.constants.ZegoRoomState;
import im.zego.zegoexpress.constants.ZegoRoomStateChangedReason;
import im.zego.zegoexpress.constants.ZegoScenario;
import im.zego.zegoexpress.entity.ZegoAudioConfig;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoEngineConfig;
import im.zego.zegoexpress.entity.ZegoEngineProfile;
import im.zego.zegoexpress.entity.ZegoPublishStreamQuality;
import im.zego.zegoexpress.entity.ZegoRoomConfig;
import im.zego.zegoexpress.entity.ZegoUser;

public class Audio3aActivity extends AppCompatActivity {

    TextView userIDText;
    TextView roomState;
    TextureView preview;
    TextureView playView;
    Button startPublishingButton;
    Button startPlayingButton;
    EditText publishStreamIDEdit;
    EditText playStreamIDEdit;
    SwitchMaterial aecSwitch;
    SwitchMaterial headphoneAecSwitch;
    AppCompatSpinner aecModeSpinner;
    SwitchMaterial agcSwitch;
    SwitchMaterial ansSwitch;
    AppCompatSpinner ansSpinner;
    SwitchMaterial musicDetectSwitch;
    SwitchMaterial recordSwitch;

    String appSign;
    Long appID;
    String userID;
    String roomID;
    String playStreamID;
    String publishStreamID;
    ZegoExpressEngine engine;
    ZegoUser user;

    //Store whether the user is publishing the stream
    boolean isPublish = false;
    //Store whether the user is playing the stream
    boolean isPlay = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio3a);
        bindView();
        getAppIDAndUserIDAndAppSign();
        setDefaultValue();
        initEngineAndUser();
        setLogComponent();
        setEventHandler();
        loginRoom();
        setAecSwitch();
        setHeadphoneAecSwitch();
        setAecModeSpinner();
        setAgcSwitch();
        setAnsSpinner();
        setAnsSwitch();
        setStartPlayButtonEvent();
        setStartPublishButtonEvent();
        setApiCalledResult();
    }
    public void bindView(){
        preview = findViewById(R.id.PreviewView);
        playView = findViewById(R.id.PlayView);
        startPlayingButton = findViewById(R.id.startPlayButton);
        startPublishingButton = findViewById(R.id.startPublishButton);
        publishStreamIDEdit = findViewById(R.id.editPublishStreamID);
        playStreamIDEdit = findViewById(R.id.editPlayStreamID);
        agcSwitch = findViewById(R.id.agc_switch);
        aecSwitch = findViewById(R.id.aec_switch);
        headphoneAecSwitch = findViewById(R.id.headphone_aec_switch);
        aecModeSpinner = findViewById(R.id.aec_spinner);
        ansSpinner = findViewById(R.id.ans_mode_spinner);
        ansSwitch = findViewById(R.id.ans_switch);
        userIDText = findViewById(R.id.userIDText);
        roomState = findViewById(R.id.roomState);
        musicDetectSwitch = findViewById(R.id.musicDetectSwitch);
        recordSwitch = findViewById(R.id.record_switch);
    }
    //get appID and userID and appSign
    public void getAppIDAndUserIDAndAppSign(){
        appID = KeyCenter.getInstance().getAppID();
        userID = UserIDHelper.getInstance().getUserID();
        appSign = KeyCenter.getInstance().getAppSign();
    }
    public void setDefaultValue(){
        //set default publish  and play streamID
        playStreamID = "0019";
        publishStreamID = "0019";
        roomID = "0019";

        userIDText.setText(userID);
        setTitle(getString(R.string.AEC_ANS_AGC));

        musicDetectSwitch.setChecked(true);
        enableMusicDetect(true);

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
        //AppLogger.getInstance().callApi("Create ZegoExpressEngine");
        //create the user
        user = new ZegoUser(userID);
    }

    void enableMusicDetect(boolean enable){
        String ss = enable?"true":"false";
        ZegoEngineConfig engineConfig = new ZegoEngineConfig();
        HashMap<String, String> advanceConfig = new HashMap<>();
        advanceConfig.put("enable_music_detection", ss);
        engineConfig.advancedConfig = advanceConfig;
        ZegoExpressEngine.setEngineConfig(engineConfig);
    }

    void enableMusicDetectRecord(boolean enable){
        String ss = enable?"true":"false";
        ZegoEngineConfig engineConfig = new ZegoEngineConfig();
        HashMap<String, String> advanceConfig = new HashMap<>();
        advanceConfig.put("enable_dump_file", ss);
        engineConfig.advancedConfig = advanceConfig;
        ZegoExpressEngine.setEngineConfig(engineConfig);
    }

    public void loginRoom(){
        //login room
        engine.loginRoom(roomID, user);
        //AppLogger.getInstance().callApi("LoginRoom: %s",roomID);
        //enable the camera
        engine.enableCamera(true);
        //enable the microphone
        engine.muteMicrophone(false);
        //enable the speaker
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

        recordSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ZegoViewUtil.UpdateRoomState(roomState, ZegoRoomStateChangedReason.LOGOUT);

                ZegoExpressEngine.destroyEngine(null);
                engine = null;

                //AppLogger.getInstance().callApi("Enable music detect: %b", isChecked);
                enableMusicDetectRecord(isChecked);
                boolean musicDetectEnabled = musicDetectSwitch.isChecked();
                enableMusicDetect(musicDetectEnabled);

                initEngineAndUser();
                setEventHandler();
                loginRoom();

                int ans_mode = ansSpinner.getSelectedItemPosition();
                boolean enable_ans = ansSwitch.isChecked();

                engine.enableANS(enable_ans);
                //AppLogger.getInstance().callApi("enableANS:%b",enable_ans);

                engine.setANSMode(ZegoANSMode.values()[ans_mode]);
                //AppLogger.getInstance().callApi("setANSMode:%s",ZegoANSMode.values()[ans_mode].toString());
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
    public void setAecSwitch(){
        aecSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                engine.enableAEC(isChecked);
                AppLogger.getInstance().callApi("Enable AEC:%b",isChecked);
            }
        });
    }
    public void setHeadphoneAecSwitch(){
        headphoneAecSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isPublish){
                    stopPublish();
                }
                if(isPlay)
                {
                    // Stop playing
                    engine.stopPlayingStream(playStreamID);
                    AppLogger.getInstance().callApi("Stop Playing Stream:%s",playStreamID);
                    isPlay = false;
                    startPlayingButton.setText(getResources().getString(R.string.start_playing));
                }
                engine.enableHeadphoneAEC(isChecked);
                AppLogger.getInstance().callApi("Enable Headphone AEC:%b",isChecked);
            }
        });
    }
    public void setAecModeSpinner(){
        aecModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] options = getResources().getStringArray(R.array.AECMode);
                switch(options[position]){
                    case "SOFT" :
                        engine.setAECMode(ZegoAECMode.SOFT);
                        AppLogger.getInstance().callApi("SET AEC mode: SOFT");
                        break;
                    case "MEDIUM":
                        engine.setAECMode(ZegoAECMode.MEDIUM);
                        AppLogger.getInstance().callApi("SET AEC mode: MEDIUM");
                        break;
                    case "AGGRESSIVE":
                        engine.setAECMode(ZegoAECMode.AGGRESSIVE);
                        AppLogger.getInstance().callApi("SET AEC mode: AGGRESSIVE");
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
    public void setAgcSwitch(){
        agcSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                engine.enableAGC(isChecked);
                AppLogger.getInstance().callApi("Enable AGC:%b",isChecked);
            }
        });
    }
    public void setAnsSwitch(){
        ansSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                engine.enableANS(isChecked);
                AppLogger.getInstance().callApi("Enable ANS:%b",isChecked);
            }
        });

        musicDetectSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.i("configsss", String.format("onCheckedChanged: %b", isChecked));

                ZegoExpressEngine.destroyEngine(null);
                engine = null;

                Log.i("configsss", String.format("Enable music detect: %b", isChecked));
                //AppLogger.getInstance().callApi("Enable music detect: %b", isChecked);
                enableMusicDetect(isChecked);
                boolean recordEnabled = recordSwitch.isChecked();
                enableMusicDetectRecord(recordEnabled);
                initEngineAndUser();
                setEventHandler();
                loginRoom();

                int ans_mode = ansSpinner.getSelectedItemPosition();
                boolean enable_ans = ansSwitch.isChecked();

                engine.enableANS(enable_ans);
                //AppLogger.getInstance().callApi("enableANS:%b",enable_ans);

                engine.setANSMode(ZegoANSMode.values()[ans_mode]);
                //AppLogger.getInstance().callApi("setANSMode:%s",ZegoANSMode.values()[ans_mode].toString());
            }
        });
    }
    public void setAnsSpinner(){
        ansSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] options = getResources().getStringArray(R.array.ANSMode);
                switch (options[position]){
                    case "SOFT":
                        engine.setANSMode(ZegoANSMode.SOFT);
                        AppLogger.getInstance().callApi("SET AEC mode: SOFT");
                        break;
                    case "AGGRESSIVE":
                        engine.setANSMode(ZegoANSMode.AGGRESSIVE);
                        AppLogger.getInstance().callApi("SET AEC mode: AGGRESSIVE");
                        break;
                    case "MEDIUM":
                        engine.setANSMode(ZegoANSMode.MEDIUM);
                        AppLogger.getInstance().callApi("SET AEC mode: MEDIUM");
                        break;
                    case "AI":
                        engine.setANSMode(ZegoANSMode.AI);
                        AppLogger.getInstance().callApi("SET AEC mode: AI");
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, Audio3aActivity.class);
        activity.startActivity(intent);

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

    @Override
    protected void onDestroy() {
        ZegoExpressEngine.destroyEngine(null);
        super.onDestroy();
    }
}