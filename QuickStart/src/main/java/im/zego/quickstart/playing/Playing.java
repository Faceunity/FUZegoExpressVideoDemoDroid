package im.zego.quickstart.playing;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONObject;

import im.zego.commontools.logtools.AppLogger;
import im.zego.commontools.logtools.LogView;
import im.zego.commontools.logtools.logLinearLayout;
import im.zego.commontools.uitools.ZegoViewUtil;
import im.zego.keycenter.KeyCenter;
import im.zego.keycenter.UserIDHelper;
import im.zego.quickstart.R;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoApiCalledEventHandler;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.constants.ZegoPlayerState;
import im.zego.zegoexpress.constants.ZegoRoomStateChangedReason;
import im.zego.zegoexpress.constants.ZegoScenario;
import im.zego.zegoexpress.constants.ZegoStreamResourceMode;
import im.zego.zegoexpress.constants.ZegoViewMode;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoEngineProfile;
import im.zego.zegoexpress.entity.ZegoPlayerConfig;
import im.zego.zegoexpress.entity.ZegoUser;

public class Playing extends AppCompatActivity {

    EditText editRoomID;
    EditText editUserID;
    EditText editStreamID;
    AppCompatSpinner viewModeSpinner;
    AppCompatSpinner resourceModeSpinner;
    SwitchMaterial videoSwitch;
    SwitchMaterial audioSwitch;
    Button startPlayingButton;
    TextureView playView;
    TextView roomState;

    Long appID;
    String roomID;
    String userID;
    String appSign;
    String playStreamID;
    boolean isPlay = false;

    ZegoExpressEngine engine;
    ZegoCanvas playCanvas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playing);
        bindView();
        setLogComponent();
        requestPermission();
        getAppIDAndUserIDAndAppSign();
        initEngineAndUser();
        setDefaultConfig();
        setViewModeEvent();
        setCameraSwitchEvent();
        setMicrophoneSwitchEvent();
        setStartPlayingButtonEvent();
        setEventHandler();
        setApiCalledResult();
    }
    public void bindView(){
        editRoomID = findViewById(R.id.editRoomID);
        editUserID = findViewById(R.id.editUserID);
        editStreamID = findViewById(R.id.editStreamID);
        viewModeSpinner = findViewById(R.id.viewModeSpinner);
        videoSwitch = findViewById(R.id.cameraSwitch);
        audioSwitch = findViewById(R.id.microphoneSwitch);
        startPlayingButton = findViewById(R.id.startButton);
        playView = findViewById(R.id.textureView);
        roomState = findViewById(R.id.roomState);
        resourceModeSpinner = findViewById(R.id.resourceMode);
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
    // Request for permission
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

        // Here we use the broadcast scenario as an example,
        // you should choose the appropriate scenario according to your actual situation,
        // for the differences between scenarios and how to choose a suitable scenario,
        // please refer to https://docs.zegocloud.com/article/14940
        profile.scenario = ZegoScenario.BROADCAST;

        profile.application = getApplication();
        engine = ZegoExpressEngine.createEngine(profile, null);

        AppLogger.getInstance().callApi("Create ZegoExpressEngine");
    }
    public void loginRoom(){
        // Create the user
        ZegoUser user = new ZegoUser(userID);
        // Login room
        engine.loginRoom(roomID, user);
        AppLogger.getInstance().callApi("LoginRoom: %s",roomID);
    }
    public void setDefaultConfig(){
        // Set default play StreamID
        playStreamID = "0003";
        // Set default room ID
        roomID = "0003";
        // Set Zego Canvas
        playCanvas = new ZegoCanvas(playView);

        playCanvas.backgroundColor = Color.WHITE;
        editUserID.setText(userID);
        editUserID.setEnabled(false);
        videoSwitch.setChecked(true);
        audioSwitch.setChecked(true);
        setTitle(getString(R.string.playing));
    }
    // Set view mode
    public void setViewModeEvent(){
        viewModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] options = getResources().getStringArray(R.array.viewMode);

                switch (options[position]) {
                    case "AspectFit":
                        playCanvas.viewMode = ZegoViewMode.ASPECT_FIT;
                        AppLogger.getInstance().callApi("Change View Mode: mode = ZegoViewMode.ASPECT_FIT");
                        break;
                    case "AspectFill":
                        playCanvas.viewMode = ZegoViewMode.ASPECT_FILL;
                        AppLogger.getInstance().callApi("Change View Mode: mode = ZegoViewMode.ASPECT_FILL");
                        break;
                    case "ScaleToFill":
                        playCanvas.viewMode = ZegoViewMode.SCALE_TO_FILL;
                        AppLogger.getInstance().callApi("Change View Mode: mode = ZegoViewMode.SCALE_TO_FILL");
                        break;
                }
                if (isPlay) {
                    // Restart playing.
                    engine.startPlayingStream(playStreamID,playCanvas);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
    // Set camera switch event
    public void setCameraSwitchEvent(){
        videoSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    // Enable playing video
                    engine.mutePlayStreamVideo(playStreamID,false);
                    AppLogger.getInstance().callApi("Video On");
                } else {
                    // Disable playing video
                    engine.mutePlayStreamVideo(playStreamID,true);
                    AppLogger.getInstance().callApi("Video Off");
                }
            }
        });
    }
    public void setMicrophoneSwitchEvent(){
        audioSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    // Enable playing audio
                    engine.mutePlayStreamAudio(playStreamID,false);
                    AppLogger.getInstance().callApi("Audio On");
                } else {
                    // Disable playing audio
                    engine.mutePlayStreamAudio(playStreamID,true);
                    AppLogger.getInstance().callApi("Audio Off");
                }
            }
        });
    }
    public void setStartPlayingButtonEvent(){
        startPlayingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playView.setVisibility(isPlay ? View.GONE:View.VISIBLE);

                if (!isPlay) {
                    // Get roomID, stream ID and user ID
                    roomID = editRoomID.getText().toString();
                    playStreamID = editStreamID.getText().toString();
                    userID = editUserID.getText().toString();
                    // Login the room
                    loginRoom();
                    // Start publishing stream
                    editRoomID.setEnabled(false);
                    editStreamID.setEnabled(false);
                    editUserID.setEnabled(false);
                    ZegoPlayerConfig playerConfig = new ZegoPlayerConfig();

                    if (resourceModeSpinner.getSelectedItem().toString().equals(getString(R.string.resource_mode_CDN))) {
                        playerConfig.resourceMode = ZegoStreamResourceMode.ONLY_CDN;
                    } else if (resourceModeSpinner.getSelectedItem().toString().equals(getString(R.string.resource_mode_rtc))) {
                        playerConfig.resourceMode = ZegoStreamResourceMode.ONLY_RTC;
                    } else if (resourceModeSpinner.getSelectedItem().toString().equals(getString(R.string.resource_mode_L3))) {
                        playerConfig.resourceMode = ZegoStreamResourceMode.ONLY_L3;
                    } else if (resourceModeSpinner.getSelectedItem().toString().equals(getString(R.string.resource_mode_CDN_Plus))) {
                        playerConfig.resourceMode = ZegoStreamResourceMode.CDN_PLUS;
                    } else {
                        playerConfig.resourceMode = ZegoStreamResourceMode.DEFAULT;
                    }

                    engine.startPlayingStream(playStreamID,playCanvas, playerConfig);
                    engine.mutePlayStreamVideo(playStreamID,!videoSwitch.isChecked());
                    engine.mutePlayStreamAudio(playStreamID,!audioSwitch.isChecked());
                    AppLogger.getInstance().callApi("Start Playing Stream:%s",playStreamID);
                    startPlayingButton.setText(getResources().getString(R.string.stop_playing));
                    isPlay = true;
                } else {
                    // Logout room
                    engine.logoutRoom(roomID);
                    AppLogger.getInstance().callApi("Logout Room:%s",roomID);
                    editRoomID.setEnabled(true);
                    editStreamID.setEnabled(true);
                    editUserID.setEnabled(false);
                    engine.stopPlayingStream(playStreamID);
                    isPlay = false;
                    startPlayingButton.setText(getResources().getString(R.string.start_playing));
                    AppLogger.getInstance().callApi("Stop Playing Stream:%s",playStreamID);
                }
            }
        });
    }
    public void setEventHandler(){
        engine.setEventHandler(new IZegoEventHandler() {
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
            // The callback triggered when the room connection state changes.
            @Override
            public void onRoomStateChanged(String roomID, ZegoRoomStateChangedReason reason, int errorCode, JSONObject extendedData) {
                ZegoViewUtil.UpdateRoomState(roomState, reason);
            }
        });
    }
    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity,Playing.class);
        activity.startActivity(intent);
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
        // Destroy the engine
        ZegoExpressEngine.destroyEngine(null);
        super.onDestroy();
    }
}