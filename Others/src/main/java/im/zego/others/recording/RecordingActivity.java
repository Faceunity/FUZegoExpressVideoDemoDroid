package im.zego.others.recording;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import im.zego.others.R;

import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import im.zego.commontools.logtools.AppLogger;
import im.zego.commontools.logtools.LogView;
import im.zego.commontools.logtools.logLinearLayout;
import im.zego.commontools.uitools.ZegoViewUtil;
import im.zego.keycenter.KeyCenter;
import im.zego.keycenter.UserIDHelper;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.ZegoMediaPlayer;
import im.zego.zegoexpress.callback.IZegoApiCalledEventHandler;
import im.zego.zegoexpress.callback.IZegoDataRecordEventHandler;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.callback.IZegoMediaPlayerLoadResourceCallback;
import im.zego.zegoexpress.constants.ZegoDataRecordState;
import im.zego.zegoexpress.constants.ZegoDataRecordType;
import im.zego.zegoexpress.constants.ZegoPublishChannel;
import im.zego.zegoexpress.constants.ZegoPublisherState;
import im.zego.zegoexpress.constants.ZegoRoomStateChangedReason;
import im.zego.zegoexpress.constants.ZegoScenario;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoDataRecordConfig;
import im.zego.zegoexpress.entity.ZegoEngineProfile;
import im.zego.zegoexpress.entity.ZegoUser;

public class RecordingActivity extends AppCompatActivity {

    TextureView preview;
    TextureView localPlayer;
    Button startPublishingButton;
    Button startRecordingButton;
    Button stopRecordingButton;
    Button playLocalMediaButton;
    TextView userIDText;
    TextView roomState;

    String userID;
    String streamID;
    String roomID;
    ZegoExpressEngine engine;
    Long appID;
    String appSign;
    ZegoUser user;
    ZegoDataRecordConfig recordConfig;
    ZegoMediaPlayer mediaPlayer;

    String filePath;
    String wholeFilePath;

    // Store whether the user is publishing the stream
    Boolean isPublish = false;
    //Store whether the user is recording
    Boolean isRecord = false;
    //Store whether the user is playing local media
    Boolean isPlay = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);
        bindView();
        getAppIDAndUserIDAndAppSign();
        setDefaultValue();
        initEngineAndUser();
        loginRoom();
        setStartPublishingButtonEvent();
        setStartRecordingButtonEvent();
        setStopRecordingButtonEvent();
        setPlayLocalMediaButtonEvent();
        setEventHandler();
        setLogComponent();
        setApiCalledResult();
        setDataRecordHandler();
    }
    public void bindView(){
        preview = findViewById(R.id.preview);
        localPlayer = findViewById(R.id.localPlayer);
        startPublishingButton = findViewById(R.id.startPublishingButton);
        startRecordingButton = findViewById(R.id.startRecordingButton);
        stopRecordingButton = findViewById(R.id.stopRecordingButton);
        playLocalMediaButton = findViewById(R.id.startPlayingButton);
        userIDText = findViewById(R.id.userIDEditText);
        roomState = findViewById(R.id.roomState);
    }
    public void setDefaultValue(){
        roomID = "0026";
        streamID = "0026";
        setTitle(getString(R.string.recording));
        userIDText.setText(userID);

        // Set the file path to save video file
        filePath = getApplicationContext().getExternalFilesDir(null) + "/video/";
        fileIsExist(filePath);
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
        //create the user
        user = new ZegoUser(userID);
    }
    // Initialize media player
    public void initMediaPlayer(){
        mediaPlayer = engine.createMediaPlayer();
        mediaPlayer.setPlayerCanvas(new ZegoCanvas(localPlayer));
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
    public void setStartPublishingButtonEvent(){
        startPublishingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPublish){
                    engine.stopPreview();
                    AppLogger.getInstance().callApi("Stop Publishing Stream:",streamID);
                    startPublishingButton.setText(getString(R.string.start_publishing));
                    engine.stopPublishingStream();
                    isPublish = false;
                } else {
                    engine.startPreview(new ZegoCanvas(preview));
                    engine.startPublishingStream(streamID);
                    startPublishingButton.setText(getString(R.string.stop_publishing));
                    AppLogger.getInstance().callApi("Start Publishing Stream:",streamID);
                    isPublish = true;
                }
            }
        });
    }
    public void setStartRecordingButtonEvent(){
        startRecordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordConfig = new ZegoDataRecordConfig();
                wholeFilePath = filePath+getFileName();
                recordConfig.filePath = wholeFilePath;
                recordConfig.recordType = ZegoDataRecordType.DEFAULT;
                engine.startRecordingCapturedData(recordConfig,ZegoPublishChannel.MAIN);
                AppLogger.getInstance().callApi("Start Recording：filePath = %s,recordType = ZegoDataRecordType.DEFAULT",wholeFilePath);
                isRecord = true;
            }
        });
    }
    public String getFileName(){
        // Use time as filename
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return sdf.format(date) + ".mp4";
    }
    public void setStopRecordingButtonEvent(){
        stopRecordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecord) {
                    engine.stopRecordingCapturedData(ZegoPublishChannel.MAIN);
                    AppLogger.getInstance().callApi("Stop Recording");
                    isRecord = false;
                } else {
                    Toast.makeText(RecordingActivity.this, "Please start recording firstly!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    public void setPlayLocalMediaButtonEvent(){
        playLocalMediaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (wholeFilePath == null){
                    Toast.makeText(RecordingActivity.this, "You haven't recorded!", Toast.LENGTH_SHORT).show();
                    return;
                }
                AppLogger.getInstance().i(wholeFilePath);
                if (!isPlay) {
                    if (mediaPlayer != null) {
                        return;
                    }

                    initMediaPlayer();
                    // Load media resource
                    mediaPlayer.loadResource(wholeFilePath, new IZegoMediaPlayerLoadResourceCallback() {
                        // Callback for media player loads resources.
                        @Override
                        public void onLoadResourceCallback(int i) {
                            if (i == 0){
                                playLocalMediaButton.setText(ZegoViewUtil.GetEmojiStringByUnicode(ZegoViewUtil.checkEmoji)+getString(R.string.stop_local_media));
                                isPlay = true;
                                mediaPlayer.start();
                                AppLogger.getInstance().callApi("Start playing local media");

                                return;
                            }

                            if (i == 1008006){
                                Toast.makeText(RecordingActivity.this, "文件不存在，请检查", Toast.LENGTH_SHORT).show();
                            } else {
                                playLocalMediaButton.setText(ZegoViewUtil.GetEmojiStringByUnicode(ZegoViewUtil.crossEmoji)+getString(R.string.play_local_media));
                                AppLogger.getInstance().fail("[%d]Faile to start playing",i);
                            }

                            engine.destroyMediaPlayer(mediaPlayer);
                            mediaPlayer = null;
                        }
                    });
                } else {
                    mediaPlayer.stop();
                    AppLogger.getInstance().callApi("Stop playing local media");
                    // Destroy Media player after stop it.
                    engine.destroyMediaPlayer(mediaPlayer);
                    mediaPlayer = null;
                    playLocalMediaButton.setText(getString(R.string.play_local_media));
                    isPlay = false;
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


        });
    }
    public void setDataRecordHandler(){
        engine.setDataRecordEventHandler(new IZegoDataRecordEventHandler() {
            // The callback triggered when the state of data recording (to a file) changes.
            @Override
            public void onCapturedDataRecordStateUpdate(ZegoDataRecordState state, int errorCode, ZegoDataRecordConfig config, ZegoPublishChannel channel) {
                super.onCapturedDataRecordStateUpdate(state, errorCode, config, channel);
                if (errorCode ==0) {
                    if (state.equals(ZegoDataRecordState.RECORDING)) {
                        startRecordingButton.setText(ZegoViewUtil.GetEmojiStringByUnicode(ZegoViewUtil.checkEmoji)+getString(R.string.start_recording));
                    } else if (state.equals(ZegoDataRecordState.SUCCESS)){
                        stopRecordingButton.setText(ZegoViewUtil.GetEmojiStringByUnicode(ZegoViewUtil.checkEmoji)+getString(R.string.stop_recording));
                    }
                } else {
                    if (state.equals(ZegoDataRecordState.RECORDING)) {
                        startRecordingButton.setText(ZegoViewUtil.GetEmojiStringByUnicode(ZegoViewUtil.crossEmoji)+getString(R.string.start_recording));
                    } else if (state.equals(ZegoDataRecordState.SUCCESS)){
                        stopRecordingButton.setText(ZegoViewUtil.GetEmojiStringByUnicode(ZegoViewUtil.crossEmoji)+getString(R.string.stop_recording));
                    }
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
        Intent intent = new Intent(activity, RecordingActivity.class);
        activity.startActivity(intent);
    }
    static boolean fileIsExist(String fileName)
    {
        // Check whether the path exist.
        File file=new File(fileName);
        if (file.exists())
            return true;
        else{
            return file.mkdirs();
        }
    }

    @Override
    protected void onDestroy() {
        engine.logoutRoom(roomID);
        ZegoExpressEngine.destroyEngine(null);
        super.onDestroy();
    }
}