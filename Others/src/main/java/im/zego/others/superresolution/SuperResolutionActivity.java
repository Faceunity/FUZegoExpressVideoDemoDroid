package im.zego.others.superresolution;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
import im.zego.zegoexpress.constants.ZegoPlayerState;
import im.zego.zegoexpress.constants.ZegoRoomStateChangedReason;
import im.zego.zegoexpress.constants.ZegoScenario;
import im.zego.zegoexpress.constants.ZegoStreamResourceMode;
import im.zego.zegoexpress.constants.ZegoSuperResolutionState;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoEngineProfile;
import im.zego.zegoexpress.entity.ZegoPlayerConfig;
import im.zego.zegoexpress.entity.ZegoUser;

public class SuperResolutionActivity extends AppCompatActivity {
    String userID;
    String playStreamID;
    String roomID;
    String playStreamID2;
    ZegoExpressEngine engine;
    Long appID;
    String appSign;
    ZegoUser user;
    //Store whether the user is playing the stream
    Boolean isPlay = false;
    Boolean isPlay2 = false;

    TextureView playView;
    TextView roomState;
    Button startPlayingButton;
    EditText playStreamIDEdit;
    EditText superResolutionStreamIDEdit;
    Switch enableSuperResolutionSwitch;
    TextView superResolutionStateText;
    TextView playerVideoSizeText;
    TextView roomInfoText;

    TextureView playView2;
    Button startPlayingButton2;
    EditText playStreamIDEdit2;
    EditText superResolutionStreamIDEdit2;
    Switch enableSuperResolutionSwitch2;
    TextView superResolutionStateText2;
    TextView playerVideoSizeText2;

    Button initSRButton;
    Button uninitButton;

    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, SuperResolutionActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_super_resolution);
//        initData();
        bindView();
        getAppIDAndUserIDAndAppSign();
        setDefaultValue();
        initEngineAndUser();
        loginRoom();
        requestPermission();
        setStartPlayButtonEvent();
        SetSuperResolutionSwitchEvent();
        setApiCalledResult();
        setLogComponent();
        setEventHandler();
    }

    @Override
    protected void onDestroy() {
        //logout and destroy the engine.
        engine.logoutRoom(roomID);
        ZegoExpressEngine.destroyEngine(null);
        super.onDestroy();
    }

    public void bindView(){
        playView = findViewById(R.id.PlayView);
        playView2 = findViewById(R.id.PlayView2);
        startPlayingButton = findViewById(R.id.startPlayButton);
        roomState = findViewById(R.id.roomState);
        playStreamIDEdit = findViewById(R.id.editPlayStreamID);
        superResolutionStreamIDEdit = findViewById(R.id.editSuperResolutionStreamID);
        enableSuperResolutionSwitch = findViewById(R.id.switchEnableSuperResolution);
        superResolutionStateText = findViewById(R.id.textViewSuperResolutionState);
        playerVideoSizeText = findViewById(R.id.textViewVideoSize);

        startPlayingButton2 = findViewById(R.id.startPlayButton2);
        playStreamIDEdit2 = findViewById(R.id.editPlayStreamID2);
        superResolutionStreamIDEdit2 = findViewById(R.id.editSuperResolutionStreamID2);
        enableSuperResolutionSwitch2 = findViewById(R.id.switchEnableSuperResolution2);
        superResolutionStateText2 = findViewById(R.id.textViewSuperResolutionState2);
        playerVideoSizeText2 = findViewById(R.id.textViewVideoSize2);

        roomInfoText = findViewById(R.id.textViewRoomInfo);
        initSRButton = findViewById(R.id.initSRbutton);
        uninitButton = findViewById(R.id.uninitSRbutton);
    }
    public void setDefaultValue(){
        roomID = "0036";
        playStreamID = "0036_1";
        playStreamID2 = "0036_2";
        setTitle(getString(R.string.super_resolution));
        playerVideoSizeText.setText("Player Video Size: ");
        superResolutionStateText.setText("Super Resolution State: ");
        playStreamIDEdit.setText(playStreamID);
        superResolutionStreamIDEdit.setText(playStreamID);
        roomInfoText.setText(String.format("UserID:%s RoomID:%s", userID.toString(), roomID.toString()));
        superResolutionStateText.setText("Super Resolution State: Off errorCode:0");

        playerVideoSizeText2.setText("Player Video Size: ");
        superResolutionStateText2.setText("Super Resolution State: ");
        playStreamIDEdit2.setText(playStreamID2);
        superResolutionStreamIDEdit2.setText(playStreamID2);
        superResolutionStateText2.setText("Super Resolution State: Off errorCode:0");
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

    public void requestPermission() {
        String[] PERMISSIONS_STORAGE = {
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO",
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE"};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.CAMERA") != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, "android.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, "android.permission.READ_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(PERMISSIONS_STORAGE, 101);
            }
        }
    }

    public void setStartPlayButtonEvent(){
        startPlayingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if the user is playing the stream, this button is used to stop playing. Otherwise, this button is used to start publishing.
                if (isPlay){
                    engine.stopPlayingStream(playStreamID);
                    AppLogger.getInstance().callApi("Stop Playing Stream:%s",playStreamID);
                    startPlayingButton.setText(getString(R.string.start_playing));
                    isPlay = false;
//                    firstFrameRenderText.setText("First Frame Render(ms): ");
                    superResolutionStateText.setText("Super Resolution state: ");
                    playerVideoSizeText.setText("Play Video Size: ");
                    playStreamID = "";

                    if(enableSuperResolutionSwitch.isChecked()){
                        enableSuperResolutionSwitch.setOnCheckedChangeListener(null);
                        enableSuperResolutionSwitch.setChecked(false);
                        SetSuperResolutionSwitchEvent();
                    }
                } else {
                    playStreamID = playStreamIDEdit.getText().toString();
                    ZegoPlayerConfig playerConfig = new ZegoPlayerConfig();
                    playerConfig.resourceMode = ZegoStreamResourceMode.ONLY_RTC;
                    engine.startPlayingStream(playStreamID, new ZegoCanvas(playView), playerConfig);
                    startPlayingButton.setText(getString(R.string.stop_playing));
                    AppLogger.getInstance().callApi("Start Playing Stream:%s",playStreamID);
                    isPlay = true;
                }
            }
        });

        startPlayingButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if the user is playing the stream, this button is used to stop playing. Otherwise, this button is used to start publishing.
                if (isPlay2){
                    engine.stopPlayingStream(playStreamID2);
                    AppLogger.getInstance().callApi("Stop Playing Stream:%s",playStreamID2);
                    startPlayingButton2.setText(getString(R.string.start_playing));
                    isPlay2 = false;
//                    firstFrameRenderText.setText("First Frame Render(ms): ");
                    superResolutionStateText2.setText("Super Resolution State: ");
                    playerVideoSizeText2.setText("Play Video Size: ");
                    playStreamID2 = "";
                    if(enableSuperResolutionSwitch2.isChecked()){
                        enableSuperResolutionSwitch2.setOnCheckedChangeListener(null);
                        enableSuperResolutionSwitch2.setChecked(false);
                        SetSuperResolutionSwitchEvent();
                    }
                } else {
                    playStreamID2 = playStreamIDEdit2.getText().toString();
                    ZegoPlayerConfig playerConfig = new ZegoPlayerConfig();
                    playerConfig.resourceMode = ZegoStreamResourceMode.ONLY_RTC;
                    engine.startPlayingStream(playStreamID2, new ZegoCanvas(playView2), playerConfig);
                    startPlayingButton2.setText(getString(R.string.stop_playing));
                    AppLogger.getInstance().callApi("Start Playing Stream:%s",playStreamID2);
                    isPlay2 = true;
                }
            }
        });

        initSRButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppLogger.getInstance().callApi("initVideoSuperResolution");
                engine.initVideoSuperResolution();
            }
        });

        uninitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppLogger.getInstance().callApi("uninitVideoSuperResolution");
                engine.uninitVideoSuperResolution();
                superResolutionStateText.setText("Super Resolution State: Off errorCode:0");
                superResolutionStateText2.setText("Super Resolution State: Off errorCode:0");
                enableSuperResolutionSwitch.setOnCheckedChangeListener(null);
                enableSuperResolutionSwitch2.setOnCheckedChangeListener(null);
                enableSuperResolutionSwitch.setChecked(false);
                enableSuperResolutionSwitch2.setChecked(false);
                SetSuperResolutionSwitchEvent();
            }
        });
    }

    public void SetSuperResolutionSwitchEvent(){
        enableSuperResolutionSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                String streamID = superResolutionStreamIDEdit.getText().toString();
                engine.enableVideoSuperResolution(streamID, b);
                AppLogger.getInstance().callApi("enableVideoSuperResolution, streamID:%s, enable:%b", streamID, b);
            }
        });

        enableSuperResolutionSwitch2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                String streamID = superResolutionStreamIDEdit2.getText().toString();
                engine.enableVideoSuperResolution(streamID, b);
                AppLogger.getInstance().callApi("enableVideoSuperResolution, streamID:%s, enable:%b", streamID, b);
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
            }

            @Override
            public void onPlayerVideoSizeChanged(String streamID, int width, int height) {
                super.onPlayerVideoSizeChanged(streamID, width, height);

                if(streamID.equals(playStreamID)){
                    playerVideoSizeText.setText(String.format("Playe Video Size: %dx%d", width, height));
                }else if(streamID.equals(playStreamID2)){
                    playerVideoSizeText2.setText(String.format("Play Video Size: %dx%d", width, height));
                }
            }

            @Override
            public void onPlayerVideoSuperResolutionUpdate(String streamID, ZegoSuperResolutionState state, int errorCode) {
                super.onPlayerVideoSuperResolutionUpdate(streamID, state, errorCode);

                String superResolutionStateStr = "";
                if(state == ZegoSuperResolutionState.ON)
                {
                    superResolutionStateStr = "Super Resolution State: On";
                }
                else if(state == ZegoSuperResolutionState.OFF)
                {
                    superResolutionStateStr = "Super Resolution State: Off";
                }

                if(streamID.equals(playStreamID)){
                    superResolutionStateText.setText(String.format("%s errorCode:%d", superResolutionStateStr, errorCode));
                }else if(streamID.equals(playStreamID2)){
                    superResolutionStateText2.setText(String.format("%s errorCode:%d", superResolutionStateStr, errorCode));
                }

                AppLogger.getInstance().i(String.format("onPlayerVideoSuperResolutionUpdate, streamID:%s, state:%d, errorCode:%d", streamID, state.value(), errorCode));
            }
        });
    }
}
