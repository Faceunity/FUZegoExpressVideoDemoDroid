package im.zego.advancedaudioprocessing.rangeaudio.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import im.zego.advancedaudioprocessing.R;
import im.zego.advancedaudioprocessing.rangeaudio.adapter.MainAdapter;
import im.zego.advancedaudioprocessing.rangeaudio.entity.ModuleInfo;
import im.zego.commontools.logtools.AppLogger;
import im.zego.commontools.logtools.LogView;
import im.zego.commontools.logtools.logLinearLayout;
import im.zego.keycenter.KeyCenter;
import im.zego.keycenter.UserIDHelper;
import im.zego.zegoexpress.ZegoAudioEffectPlayer;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.ZegoMediaPlayer;
import im.zego.zegoexpress.ZegoRangeAudio;
import im.zego.zegoexpress.callback.IZegoApiCalledEventHandler;
import im.zego.zegoexpress.callback.IZegoAudioEffectPlayerEventHandler;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.callback.IZegoIMSendBroadcastMessageCallback;
import im.zego.zegoexpress.callback.IZegoMediaPlayerEventHandler;
import im.zego.zegoexpress.callback.IZegoMediaPlayerLoadResourceCallback;
import im.zego.zegoexpress.callback.IZegoRangeAudioEventHandler;
import im.zego.zegoexpress.constants.ZegoAudioEffectPlayState;
import im.zego.zegoexpress.constants.ZegoMediaPlayerState;
import im.zego.zegoexpress.constants.ZegoMultimediaLoadType;
import im.zego.zegoexpress.constants.ZegoRangeAudioMicrophoneState;
import im.zego.zegoexpress.constants.ZegoRangeAudioMode;
import im.zego.zegoexpress.constants.ZegoRangeAudioSpeakMode;
import im.zego.zegoexpress.constants.ZegoRangeAudioListenMode;
import im.zego.zegoexpress.constants.ZegoScenario;
import im.zego.zegoexpress.constants.ZegoUpdateType;
import im.zego.zegoexpress.entity.ZegoAudioEffectPlayConfig;
import im.zego.zegoexpress.entity.ZegoBroadcastMessageInfo;
import im.zego.zegoexpress.entity.ZegoEngineConfig;
import im.zego.zegoexpress.entity.ZegoEngineProfile;
import im.zego.zegoexpress.entity.ZegoMediaPlayerResource;
import im.zego.zegoexpress.entity.ZegoRoomConfig;
import im.zego.zegoexpress.entity.ZegoUser;

public class RangeAudioActivity extends AppCompatActivity {

    EditText editRoomID;
    EditText editUserID;
    EditText muteUserID;
    Button loginButton;
    AppCompatSpinner audioModeSpinner;
    AppCompatSpinner speakModeSpinner;
    AppCompatSpinner listenModeSpinner;
    EditText editTeamID;
    SwitchMaterial microphoneSwitch;
    SwitchMaterial speakerSwitch;
    SwitchMaterial muteUserSwitch;
    EditText editRange;
    SwitchMaterial sound3DSwitch;
    SeekBar frontValueSeekBar;
    TextView frontValueTextView;
    SeekBar rightValueSeekBar;
    TextView rightValueTextView;
    SeekBar upValueSeekBar;
    TextView upValueTextView;
    SeekBar frontRotateSeekBar;
    TextView frontRotateTextView;
    SeekBar rightRotateSeekBar;
    TextView rightRotateTextView;
    SeekBar upRotateSeekBar;
    TextView upRotateTextView;
    RecyclerView userListView;
    EditText editMediaPlayerIndex;
    EditText editMediaResourceIndex;
    Button mediaPlayerLoadButton;
    Button mediaPlayerStartButton;
    Button mediaPlayerStopButton;
    Button mediaPlayerUpdateButton;
    SeekBar mediaPlayerFrontSeekBar;
    TextView mediaPlayerFrontValueTextView;
    SeekBar mediaPlayerRightSeekBar;
    TextView mediaPlayerRightValueTextView;
    SeekBar mediaPlayerUpSeekBar;
    TextView mediaPlayerUpValueTextView;
    EditText editEffectSoundID;
    EditText editAudioResourceIndex;
    Button audioEffectPlayerStartButton;
    Button audioEffectPlayerStopButton;
    Button audioEffectPlayerUpdateButton;
    SeekBar audioEffectPlayerFrontSeekBar;
    TextView audioEffectPlayerFrontValueTextView;
    SeekBar audioEffectPlayerRightSeekBar;
    TextView audioEffectPlayerRightValueTextView;
    SeekBar audioEffectPlayerUpSeekBar;
    TextView audioEffectPlayerUpValueTextView;

    MainAdapter mainAdapter = new MainAdapter();

    private Long appID;
    private String userID;
    private String appSign;

    private ZegoExpressEngine engine;
    private ZegoRangeAudio rangeAudio;
    private ArrayList<ZegoMediaPlayer> mediaPlayers = new ArrayList<>();
    private ZegoAudioEffectPlayer audioEffectPlayer;
    private ZegoRangeAudioSpeakMode speakMode;
    private ZegoRangeAudioListenMode listenMode;


    float [] selfPosition = new float[3];
    float [] rotateAngles = new float[3];
    float [] matrixRotateFront = new float[3];
    float [] matrixRotateRight = new float[3];
    float [] matrixRotateUp = new float[3];
    float [] mediaPlayerPosition = new float[3];
    float [] audioEffectPlayerPosition = new float[3];

    List<ZegoUserPositionInfo> userPositionList = new ArrayList<>();
    private boolean is_login = false;

    ArrayList<String> mediaPlayerResource = new ArrayList<>();
    ArrayList<String> audioEffectPlayerResource = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_range_audio);
        bindView();
        getAppIDAndUserIDAndAppSign();
        setDefaultValue();
        initEngine();
        initRangeAudio();
        initMediaPlayer();
        initAudioEffectPlayer();
        prepareResource();
        setViewEventListener();
        setLogComponent();
        setApiCalledResult();
    }

    public void bindView(){
        editRoomID = findViewById(R.id.editRoomID);
        editUserID = findViewById(R.id.editUserID);
        muteUserID = findViewById(R.id.editMuteUserID);
        loginButton = findViewById(R.id.loginButton);
        audioModeSpinner = findViewById(R.id.audioModeSpinner);
        speakModeSpinner = findViewById(R.id.speakModeSpinner);
        listenModeSpinner = findViewById(R.id.listenModeSpinner);
        editTeamID = findViewById(R.id.editTeamID);
        microphoneSwitch = findViewById(R.id.microphoneSwitch);
        speakerSwitch = findViewById(R.id.speakerSwitch);
        muteUserSwitch = findViewById(R.id.isMuteSwitch);
        editRange = findViewById(R.id.editRange);
        sound3DSwitch = findViewById(R.id.sound3DSwitch);
        frontValueSeekBar = findViewById(R.id.frontSeekBar);
        frontValueTextView = findViewById(R.id.frontValueTextView);
        rightValueSeekBar = findViewById(R.id.rightSeekBar);
        rightValueTextView = findViewById(R.id.rightValueTextView);
        upValueSeekBar = findViewById(R.id.upSeekBar);
        upValueTextView = findViewById(R.id.upValueTextView);
        frontRotateSeekBar = findViewById(R.id.frontRotateSeekBar);
        frontRotateTextView = findViewById(R.id.frontRotateTextView);
        rightRotateSeekBar = findViewById(R.id.rightRotateSeekBar);
        rightRotateTextView = findViewById(R.id.rightRotateTextView);
        upRotateSeekBar = findViewById(R.id.upRotateSeekBar);
        upRotateTextView = findViewById(R.id.upRotateTextView);
        userListView = findViewById(R.id.userList);

        editMediaPlayerIndex = findViewById(R.id.editMediaPlayerIndex);
        editMediaResourceIndex = findViewById(R.id.editMediaResourceIndex);
        mediaPlayerLoadButton = findViewById(R.id.mediaPlayerLoadButton);
        mediaPlayerStartButton = findViewById(R.id.mediaPlayerStartButton);
        mediaPlayerStopButton = findViewById(R.id.mediaPlayerStopButton);
        mediaPlayerUpdateButton = findViewById(R.id.mediaPlayerUpdateButton);
        mediaPlayerFrontSeekBar = findViewById(R.id.mediaPlayerFrontSeekBar);
        mediaPlayerFrontValueTextView = findViewById(R.id.mediaPlayerFrontValueTextView);
        mediaPlayerRightSeekBar = findViewById(R.id.mediaPlayerRightSeekBar);
        mediaPlayerRightValueTextView = findViewById(R.id.mediaPlayerRightValueTextView);
        mediaPlayerUpSeekBar = findViewById(R.id.mediaPlayerUpSeekBar);
        mediaPlayerUpValueTextView = findViewById(R.id.mediaPlayerUpValueTextView);
        editEffectSoundID = findViewById(R.id.editEffectSoundID);
        editAudioResourceIndex = findViewById(R.id.editAudioResourceIndex);
        audioEffectPlayerStartButton = findViewById(R.id.audioEffectPlayerStartButton);
        audioEffectPlayerStopButton = findViewById(R.id.audioEffectPlayerStopButton);
        audioEffectPlayerUpdateButton = findViewById(R.id.audioEffectPlayerUpdateButton);
        audioEffectPlayerFrontSeekBar = findViewById(R.id.audioEffectPlayerFrontSeekBar);
        audioEffectPlayerFrontValueTextView = findViewById(R.id.audioEffectPlayerFrontValueTextView);
        audioEffectPlayerRightSeekBar = findViewById(R.id.audioEffectPlayerRightSeekBar);
        audioEffectPlayerRightValueTextView = findViewById(R.id.audioEffectPlayerRightValueTextView);
        audioEffectPlayerUpSeekBar = findViewById(R.id.audioEffectPlayerUpSeekBar);
        audioEffectPlayerUpValueTextView = findViewById(R.id.audioEffectPlayerUpValueTextView);
    }

    @SuppressLint("NewApi")
    public void setDefaultValue(){
        editUserID.setText(userID);
        editUserID.setEnabled(false);
        // Set matrix default
        eulerAnglesToRotationMatrix(rotateAngles, matrixRotateFront, matrixRotateRight, matrixRotateUp);
    }

    //get appID and userID and appSign
    public void getAppIDAndUserIDAndAppSign(){
        appID = KeyCenter.getInstance().getAppID();
        userID = UserIDHelper.getInstance().getUserID();
        appSign = KeyCenter.getInstance().getAppSign();
    }
    public void initEngine(){

        ZegoEngineConfig engineConfig = new ZegoEngineConfig();
        HashMap<String, String>advanceConfig = new HashMap<>();
        advanceConfig.put("max_channels", "3");
        advanceConfig.put("room_user_update_optimize", "1");
        engineConfig.advancedConfig = advanceConfig;
        ZegoExpressEngine.setEngineConfig(engineConfig);

        // Initialize ZegoExpressEngine
        ZegoEngineProfile profile = new ZegoEngineProfile();
        profile.appID = appID;
        profile.appSign = appSign;

        // Here we use the high quality chatroom scenario as an example,
        // you should choose the appropriate scenario according to your actual situation,
        // for the differences between scenarios and how to choose a suitable scenario,
        // please refer to https://docs.zegocloud.com/article/14940
        profile.scenario = ZegoScenario.HIGH_QUALITY_CHATROOM;

        profile.application = getApplication();
        engine = ZegoExpressEngine.createEngine(profile, new IZegoEventHandler() {
            @SuppressLint("NewApi")
            @Override
            public void onRoomUserUpdate(String roomID, ZegoUpdateType updateType, ArrayList<ZegoUser> userList) {
                super.onRoomUserUpdate(roomID, updateType, userList);
                if (updateType == ZegoUpdateType.ADD) {
                    for (ZegoUser user : userList) {
                        ZegoUserPositionInfo info = new ZegoUserPositionInfo();
                        info.userID = user.userID;
                        info.position = "";
                        userPositionList.add(info);
                    }
                    String message = selfPosition[0] + "," + selfPosition[1] + "," + selfPosition[2];
                    /** 发送房间广播信令
                     * ❗️❗️❗️房间信令属于低频信息，此方法只为演示Demo使用，开发者需自己使用服务器维护位置信息
                     *  Send room broadcast message
                     * ❗️❗️❗️Room message is low-frequency information. This method is only for testing. Developers need to maintain position information by themselves
                     */
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            engine.sendBroadcastMessage(editRoomID.getText().toString(), message, new IZegoIMSendBroadcastMessageCallback() {
                                @Override
                                public void onIMSendBroadcastMessageResult(int i, long l) {
                                    AppLogger.getInstance().callApi("Send broadcast message result errorCode: %d, messageID: %d", i, l);
                                }
                            });
                        }
                    }, 500);

                } else {
                    for (ZegoUser user : userList) {
                        for (ZegoUserPositionInfo info : userPositionList) {
                            if (info.userID.equals(user.userID)) {
                                userPositionList.remove(info);
                                break;
                            }
                        }
                    }
                }
                setItem();
                AppLogger.getInstance().callApi("Room User Update Callback: %s, UsersCount: %d, roomID: %s", updateType, userList.size(), roomID);
            }

            @Override
            public void onIMRecvBroadcastMessage(String roomID, ArrayList<ZegoBroadcastMessageInfo> messageList) {
                super.onIMRecvBroadcastMessage(roomID, messageList);
                for (ZegoBroadcastMessageInfo messageInfo : messageList) {
                    List<String> list = Arrays.asList(messageInfo.message.split(","));
                    float[] position =  new float[]{Float.parseFloat(list.get(0)), Float.parseFloat(list.get(1)), Float.parseFloat(list.get(2))};
                    for (ZegoUserPositionInfo positionInfo : userPositionList) {
                        if (messageInfo.fromUser.userID.equals(positionInfo.userID)) {
                            positionInfo.position = String.format("%.0f,%.0f,%.0f", position[0], position[1], position[2]);
                        }
                    }
                    // Update other audio position
                    rangeAudio.updateAudioSource(messageInfo.fromUser.userID, position);
                }
                setItem();
            }
        });
        AppLogger.getInstance().callApi("Create ZegoExpressEngine");
    }

    public void initRangeAudio() {
        // initialize RangeAudio
        rangeAudio = engine.createRangeAudio();
        // set range audio event handler
        rangeAudio.setEventHandler(new IZegoRangeAudioEventHandler() {
            @Override
            public void onRangeAudioMicrophoneStateUpdate(ZegoRangeAudio rangeAudio, ZegoRangeAudioMicrophoneState state, int errorCode) {
                super.onRangeAudioMicrophoneStateUpdate(rangeAudio, state, errorCode);
                AppLogger.getInstance().callApi("microphone state update. state: %s, errorCode: %d", state, errorCode);
            }
        });
        AppLogger.getInstance().callApi("Create RangeAudio");
        // Set default value
        rangeAudio.setAudioReceiveRange(100);
        speakMode = ZegoRangeAudioSpeakMode.ALL;
        listenMode = ZegoRangeAudioListenMode.ALL;
    }

    public void initMediaPlayer() {
        for (int i = 0; i <4; ++i) {
            ZegoMediaPlayer player = engine.createMediaPlayer();
            if (player != null) {
                player.setEventHandler(new IZegoMediaPlayerEventHandler() {
                    @Override
                    public void onMediaPlayerStateUpdate(ZegoMediaPlayer mediaPlayer, ZegoMediaPlayerState state, int errorCode) {
                        super.onMediaPlayerStateUpdate(mediaPlayer, state, errorCode);
                        AppLogger.getInstance().receiveCallback("onMediaPlayerStateUpdate, index:%d, state:%s, error:%d", mediaPlayer.getIndex(), state.toString(), errorCode);
                    }
                });
            }
            AppLogger.getInstance().callApi("Create MediaPlayer index:%d %s", i, player != null ? "success" : "failed");
            mediaPlayers.add(player);
        }
    }

    public void initAudioEffectPlayer() {
        audioEffectPlayer = engine.createAudioEffectPlayer();
        if (audioEffectPlayer != null) {
            audioEffectPlayer.setEventHandler(new IZegoAudioEffectPlayerEventHandler() {
                @Override
                public void onAudioEffectPlayStateUpdate(ZegoAudioEffectPlayer audioEffectPlayer, int audioEffectID, ZegoAudioEffectPlayState state, int errorCode) {
                    super.onAudioEffectPlayStateUpdate(audioEffectPlayer, audioEffectID, state, errorCode);
                    AppLogger.getInstance().receiveCallback("onAudioEffectPlayStateUpdate, audioEffectID:%d, state:%s, errorCode:%d", audioEffectID, state.toString(), errorCode);
                }
            });
        }
        AppLogger.getInstance().callApi("Create AudioEffectPlayer %s", audioEffectPlayer != null ? "success" : "failed");
    }

    public void prepareResource() {
        mediaPlayerResource.add("https://storage.zego.im/demo/sample_astrix.mp3");
        mediaPlayerResource.add("https://storage.zego.im/demo/201808270915.mp4");
        mediaPlayerResource.add(getExternalFilesDir("").getPath()+"/test.wav");
        mediaPlayerResource.add(getExternalFilesDir("").getPath()+"/sample.mp3");
        mediaPlayerResource.add(getExternalFilesDir("").getPath()+"/ad.mp4");

        ArrayList<String> copyMediaPlayerResource = new ArrayList<>();
        copyMediaPlayerResource.add("test.wav");
        copyMediaPlayerResource.add("sample.mp3");
        copyMediaPlayerResource.add("ad.mp4");
        copyAssetsFiles(copyMediaPlayerResource);

        audioEffectPlayerResource.add(getExternalFilesDir("").getPath()+"/effect_1_stereo.wav");
        audioEffectPlayerResource.add(getExternalFilesDir("").getPath()+"/effect_2_mono.wav");

        ArrayList<String> copyAudioEffectPlayerResource = new ArrayList<>();
        copyAudioEffectPlayerResource.add("effect_1_stereo.wav");
        copyAudioEffectPlayerResource.add("effect_2_mono.wav");
        copyAssetsFiles(copyAudioEffectPlayerResource);
    }

    public void setItem() {
        mainAdapter.clear();
        for (ZegoUserPositionInfo info : userPositionList) {
            mainAdapter.addModuleInfo(new ModuleInfo().titleName(info.userID).contentName(info.position));
        }
        userListView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        userListView.setAdapter(mainAdapter);
        userListView.setItemAnimator(new DefaultItemAnimator());
    }

    public void setViewEventListener() {
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(is_login == false)
                {
                    is_login = true;
                    loginButton.setText("Logout Room");
                    ZegoUser user = new ZegoUser(editUserID.getText().toString());
                    ZegoRoomConfig roomConfig = new ZegoRoomConfig();
                    roomConfig.isUserStatusNotify = true;
                    //login room
                    engine.loginRoom(editRoomID.getText().toString(), user, roomConfig);
                    AppLogger.getInstance().callApi("Login room, roomID: %s, userID: %s", editRoomID.getText().toString(), user.userID);

                    // Update self position
                    rangeAudio.updateSelfPosition(selfPosition, matrixRotateFront, matrixRotateRight, matrixRotateUp);
                }
                else
                {
                    is_login = false;
                    loginButton.setText("Login Room");
                    AppLogger.getInstance().callApi("Logout room");
                    engine.logoutRoom();
                }
            }
        });

        audioModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] modeOptions = getResources().getStringArray(R.array.audioModeSelection);
                switch (modeOptions[position]) {
                    case "WORLD":
                        // set range audio mode
                        rangeAudio.setRangeAudioMode(ZegoRangeAudioMode.WORLD);
                        AppLogger.getInstance().callApi("Set audio mode: ZegoRangeAudioMode.WORLD");
                        break;
                    case "TEAM":
                        // set range audio mode
                        rangeAudio.setRangeAudioMode(ZegoRangeAudioMode.TEAM);
                        AppLogger.getInstance().callApi("Set audio mode: ZegoRangeAudioMode.TEAM");
                        break;
                    case "SECRETE":
                        // set range audio mode
                        rangeAudio.setRangeAudioMode(ZegoRangeAudioMode.SECRET_TEAM);
                        AppLogger.getInstance().callApi("Set audio mode: ZegoRangeAudioMode.SECRET_TEAM");
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        speakModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] modeOptions = getResources().getStringArray(R.array.speakModeSelection);
                switch (modeOptions[position]) {
                    case "ALL":
                        // set range audio custom mode
                        speakMode = ZegoRangeAudioSpeakMode.ALL;
                        AppLogger.getInstance().callApi("Set audio speak mode: ZegoRangeAudioSpeakMode.ALL");
                        break;
                    case "WORLD":
                        // set range audio custom mode
                        speakMode = ZegoRangeAudioSpeakMode.WORLD;
                        AppLogger.getInstance().callApi("Set audio speak mode: ZegoRangeAudioSpeakMode.WORLD");
                        break;
                    case "TEAM":
                        // set range audio custom mode
                        speakMode = ZegoRangeAudioSpeakMode.TEAM;
                        AppLogger.getInstance().callApi("Set audio speak mode: ZegoRangeAudioSpeakMode.TEAM");
                        break;
                }

                rangeAudio.setRangeAudioCustomMode(speakMode, listenMode);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        listenModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] modeOptions = getResources().getStringArray(R.array.listenModeSelection);
                switch (modeOptions[position]) {
                    case "ALL":
                        // set range audio custom mode
                        listenMode = ZegoRangeAudioListenMode.ALL;
                        AppLogger.getInstance().callApi("Set audio listen mode: ZegoRangeAudioListenMode.ALL");
                        break;
                    case "WORLD":
                        // set range audio custom mode
                        listenMode = ZegoRangeAudioListenMode.WORLD;
                        AppLogger.getInstance().callApi("Set audio listen mode: ZegoRangeAudioListenMode.WORLD");
                        break;
                    case "TEAM":
                        // set range audio custom mode
                        listenMode = ZegoRangeAudioListenMode.TEAM;
                        AppLogger.getInstance().callApi("Set audio listen mode: ZegoRangeAudioListenMode.TEAM");
                        break;
                }

                rangeAudio.setRangeAudioCustomMode(speakMode, listenMode);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        editTeamID.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // set team id
                rangeAudio.setTeamID(s.toString());
                AppLogger.getInstance().callApi("Set team id:%s", s.toString());
            }
        });

        microphoneSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // enable microphone
                rangeAudio.enableMicrophone(isChecked);
                AppLogger.getInstance().callApi("Enable Microphone: %s", isChecked ? "On" : "Off");
            }
        });

        speakerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // enable speaker
                rangeAudio.enableSpeaker(isChecked);
                AppLogger.getInstance().callApi("Enable Speaker: %s", isChecked ? "On" : "Off");
            }
        });

        muteUserSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String muteUserId = muteUserID.getText().toString();
                AppLogger.getInstance().callApi("muteUser:%s, mute:%s", muteUserId, isChecked ? "On" : "Off");
                rangeAudio.muteUser(muteUserId, isChecked);
            }
        });

        editRange.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                float range = 0;
                try {
                    range = Float.parseFloat(s.toString());
                } catch (NumberFormatException e) {
                    range = 0;
                }
                // set receive range
                rangeAudio.setAudioReceiveRange(range);
                AppLogger.getInstance().callApi("Set audio receive range:%s", String.valueOf(range));
            }
        });

        sound3DSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // set 3D sound effect
                rangeAudio.enableSpatializer(isChecked);
                AppLogger.getInstance().callApi("Enable Spatializer: %s", isChecked ? "On" : "Off");
            }
        });

        frontValueSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                selfPosition[0] = seekBar.getProgress();
                rangeAudio.updateSelfPosition(selfPosition, matrixRotateFront, matrixRotateRight, matrixRotateUp);

                String message = selfPosition[0] + "," + selfPosition[1] + "," + selfPosition[2];
                /** 发送房间广播信令
                 * ❗️❗️❗️房间信令属于低频信息，此方法只为演示Demo使用，开发者需自己使用服务器维护位置信息
                 *  Send room broadcast message
                 * ❗️❗️❗️Room message is low-frequency information. This method is only for testing. Developers need to maintain position information by themselves
                 */
                engine.sendBroadcastMessage(editRoomID.getText().toString(), message, new IZegoIMSendBroadcastMessageCallback() {
                    @Override
                    public void onIMSendBroadcastMessageResult(int i, long l) {
                        AppLogger.getInstance().callApi("Send BroadcastMessage:%s", i == 0 ? "Success":"Failed");
                    }
                });
                AppLogger.getInstance().callApi("Update self position:%.0f,%.0f,%.0f", selfPosition[0],selfPosition[1],selfPosition[2]);

                frontValueTextView.setText(String.valueOf(seekBar.getProgress()));
            }
        });

        rightValueSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                selfPosition[1] = seekBar.getProgress();
                rangeAudio.updateSelfPosition(selfPosition, matrixRotateFront, matrixRotateRight, matrixRotateUp);

                String message = selfPosition[0] + "," + selfPosition[1] + "," + selfPosition[2];
                /** 发送房间广播信令
                 * ❗️❗️❗️房间信令属于低频信息，此方法只为演示Demo使用，开发者需自己使用服务器维护位置信息
                 *  Send room broadcast message
                 * ❗️❗️❗️Room message is low-frequency information. This method is only for testing. Developers need to maintain position information by themselves
                 */
                engine.sendBroadcastMessage(editRoomID.getText().toString(), message, new IZegoIMSendBroadcastMessageCallback() {
                    @Override
                    public void onIMSendBroadcastMessageResult(int i, long l) {
                        AppLogger.getInstance().callApi("Send BroadcastMessage:%s", i == 0 ? "Success":"Failed");
                    }
                });
                AppLogger.getInstance().callApi("Update self position:%.0f,%.0f,%.0f", selfPosition[0],selfPosition[1],selfPosition[2]);

                rightValueTextView.setText(String.valueOf(seekBar.getProgress()));
            }
        });

        upValueSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                selfPosition[2] = seekBar.getProgress();
                rangeAudio.updateSelfPosition(selfPosition, matrixRotateFront, matrixRotateRight, matrixRotateUp);

                String message = selfPosition[0] + "," + selfPosition[1] + "," + selfPosition[2];
                /** 发送房间广播信令
                 * ❗️❗️❗️房间信令属于低频信息，此方法只为演示Demo使用，开发者需自己使用服务器维护位置信息
                 *  Send room broadcast message
                 * ❗️❗️❗️Room message is low-frequency information. This method is only for testing. Developers need to maintain position information by themselves
                 */
                engine.sendBroadcastMessage(editRoomID.getText().toString(), message, new IZegoIMSendBroadcastMessageCallback() {
                    @Override
                    public void onIMSendBroadcastMessageResult(int i, long l) {
                        AppLogger.getInstance().callApi("Send BroadcastMessage:%s", i == 0 ? "Success":"Failed");
                    }
                });
                AppLogger.getInstance().callApi("Update self position:%.0f,%.0f,%.0f", selfPosition[0],selfPosition[1],selfPosition[2]);

                upValueTextView.setText(String.valueOf(seekBar.getProgress()));
            }
        });

        frontRotateSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                rotateAngles[0] = (float) (seekBar.getProgress() * Math.PI / 180);
                eulerAnglesToRotationMatrix(rotateAngles, matrixRotateFront, matrixRotateRight, matrixRotateUp);
                // Update self position
                rangeAudio.updateSelfPosition(selfPosition, matrixRotateFront, matrixRotateRight, matrixRotateUp);

                AppLogger.getInstance().callApi("Update front rotate:%d", seekBar.getProgress());

                frontRotateTextView.setText(String.valueOf(seekBar.getProgress()));
            }
        });

        rightRotateSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                rotateAngles[1] = (float) (seekBar.getProgress() * Math.PI / 180);
                eulerAnglesToRotationMatrix(rotateAngles, matrixRotateFront, matrixRotateRight, matrixRotateUp);
                // Update self position
                rangeAudio.updateSelfPosition(selfPosition, matrixRotateFront, matrixRotateRight, matrixRotateUp);

                AppLogger.getInstance().callApi("Update right rotate:%d", seekBar.getProgress());

                rightRotateTextView.setText(String.valueOf(seekBar.getProgress()));
            }
        });

        upRotateSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                rotateAngles[2] = (float) (seekBar.getProgress() * Math.PI / 180);
                eulerAnglesToRotationMatrix(rotateAngles, matrixRotateFront, matrixRotateRight, matrixRotateUp);
                // Update self position
                rangeAudio.updateSelfPosition(selfPosition, matrixRotateFront, matrixRotateRight, matrixRotateUp);

                AppLogger.getInstance().callApi("Update up rotate:%d", seekBar.getProgress());

                upRotateTextView.setText(String.valueOf(seekBar.getProgress()));
            }
        });

        mediaPlayerLoadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int playerIndex = Integer.parseInt(editMediaPlayerIndex.getText().toString());
                int resourceIndex = Integer.parseInt(editMediaResourceIndex.getText().toString());
                ZegoMediaPlayer player = mediaPlayers.get(playerIndex);
                String path = mediaPlayerResource.get(resourceIndex);

                ZegoMediaPlayerResource resource = new ZegoMediaPlayerResource();
                resource.loadType = ZegoMultimediaLoadType.FILE_PATH;
                resource.filePath = path;
                player.loadResourceWithConfig(resource, new IZegoMediaPlayerLoadResourceCallback() {
                    @Override
                    public void onLoadResourceCallback(int errorCode) {
                        AppLogger.getInstance().receiveCallback("loadResourceWithConfig, errorCode:%d", errorCode);
                    }
                });
            }
        });

        mediaPlayerStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int playerIndex = Integer.parseInt(editMediaPlayerIndex.getText().toString());
                ZegoMediaPlayer player = mediaPlayers.get(playerIndex);

                player.start();
            }
        });

        mediaPlayerStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int playerIndex = Integer.parseInt(editMediaPlayerIndex.getText().toString());
                ZegoMediaPlayer player = mediaPlayers.get(playerIndex);

                player.stop();
            }
        });

        mediaPlayerUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int playerIndex = Integer.parseInt(editMediaPlayerIndex.getText().toString());
                ZegoMediaPlayer player = mediaPlayers.get(playerIndex);

                player.updatePosition(mediaPlayerPosition);
            }
        });

        mediaPlayerFrontSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int value = seekBar.getProgress();
                mediaPlayerPosition[0] = value;
                mediaPlayerFrontValueTextView.setText(String.valueOf(value));
            }
        });

        mediaPlayerRightSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int value = seekBar.getProgress();
                mediaPlayerPosition[1] = value;
                mediaPlayerRightValueTextView.setText(String.valueOf(value));
            }
        });

        mediaPlayerUpSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int value = seekBar.getProgress();
                mediaPlayerPosition[2] = value;
                mediaPlayerUpValueTextView.setText(String.valueOf(value));
            }
        });

        audioEffectPlayerStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int effectSoundID = Integer.parseInt(editEffectSoundID.getText().toString());
                int resourceIndex = Integer.parseInt(editAudioResourceIndex.getText().toString());
                String path = audioEffectPlayerResource.get(resourceIndex);

                ZegoAudioEffectPlayConfig config = new ZegoAudioEffectPlayConfig();
                config.playCount = 10;
                audioEffectPlayer.start(effectSoundID, path, config);
            }
        });

        audioEffectPlayerStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int effectSoundID = Integer.parseInt(editEffectSoundID.getText().toString());

                audioEffectPlayer.stop(effectSoundID);
            }
        });

        audioEffectPlayerUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int effectSoundID = Integer.parseInt(editEffectSoundID.getText().toString());

                audioEffectPlayer.updatePosition(effectSoundID, audioEffectPlayerPosition);
            }
        });

        audioEffectPlayerFrontSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int value = seekBar.getProgress();
                audioEffectPlayerPosition[0] = value;
                audioEffectPlayerFrontValueTextView.setText(String.valueOf(value));
            }
        });

        audioEffectPlayerRightSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int value = seekBar.getProgress();
                audioEffectPlayerPosition[1] = value;
                audioEffectPlayerRightValueTextView.setText(String.valueOf(value));
            }
        });

        audioEffectPlayerUpSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int value = seekBar.getProgress();
                audioEffectPlayerPosition[2] = value;
                audioEffectPlayerUpValueTextView.setText(String.valueOf(value));
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
        Intent intent = new Intent(activity, RangeAudioActivity.class);
        activity.startActivity(intent);
    }

    // euler angles to rotate matrix
    private void eulerAnglesToRotationMatrix(float [] theta, float [] matrix_front, float [] matrix_right, float [] matrix_up) {
        float [][] matrix_rotate_front = {
                {1, 0, 0},
                {0, (float)Math.cos(theta[0]), -(float)Math.sin(theta[0])},
                {0, (float)Math.sin(theta[0]), (float)Math.cos(theta[0])},
        };
        float [][] matrix_rotate_right = {
                {(float)Math.cos(theta[1]), 0, (float)Math.sin(theta[1])},
                {0, 1, 0},
                {-(float)Math.sin(theta[1]), 0, (float)Math.cos(theta[1])},
        };
        float [][] matrix_rotate_up = {
                {(float)Math.cos(theta[2]), -(float)Math.sin(theta[2]), 0},
                {(float)Math.sin(theta[2]), (float)Math.cos(theta[2]), 0},
                {0, 0, 1},
        };

        float [][] matrix_rotate = new float[3][3];
        float [][] matrix_rotate_temp = new float[3][3];

        matrixMultiply(matrix_rotate_front, matrix_rotate_right, matrix_rotate_temp);
        matrixMultiply(matrix_rotate_temp, matrix_rotate_up, matrix_rotate);

        matrix_front[0] = matrix_rotate[0][0];
        matrix_front[1] = matrix_rotate[1][0];
        matrix_front[2] = matrix_rotate[2][0];

        matrix_right[0] = matrix_rotate[0][1];
        matrix_right[1] = matrix_rotate[1][1];
        matrix_right[2] = matrix_rotate[2][1];

        matrix_up[0] = matrix_rotate[0][2];
        matrix_up[1] = matrix_rotate[1][2];
        matrix_up[2] = matrix_rotate[2][2];
    }

    private void matrixMultiply(float [][] a, float [][] b, float [][] dst) {
        for(int i=0; i<3; i++) {
            for(int j=0;j<3;j++) {
                dst[i][j] = 0;
                for(int k=0;k<3;k++) {
                    dst[i][j] += a[i][k]*b[k][j];
                }
            }
        }
    }

    private void copyAssetsFiles(final List<String> fileNames) {
        new Thread() {
            public void run() {
                for (String fileName : fileNames) {
                    copyAssetsFile(fileName);
                }
            }
        }.start();
    }

    private void copyAssetsFile(String fileName) {
        final File file = new File(getExternalFilesDir(""), fileName);//getFilesDir()方法用于获取/data/data//files目录
        System.out.println("File Path---->" + file.getAbsolutePath());
        if (file.exists()) {
            System.out.println("File exists.");
            return;
        }
        try {
            // get Assets.
            AssetManager assetManager = getAssets();
            InputStream is = assetManager.open(fileName);
            FileOutputStream fos = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int len = 0;
            // Write file.
            while ((len = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        for (ZegoMediaPlayer player : mediaPlayers) {
            player.setEventHandler(null);
            engine.destroyMediaPlayer(player);
        }
        audioEffectPlayer.setEventHandler(null);
        engine.destroyAudioEffectPlayer(audioEffectPlayer);
        rangeAudio.setEventHandler(null);
        engine.destroyRangeAudio(rangeAudio);
        engine.setEventHandler(null);
        ZegoExpressEngine.destroyEngine(null);
        super.onDestroy();
    }
}

class ZegoUserPositionInfo {
    String userID;
    String position;
}
