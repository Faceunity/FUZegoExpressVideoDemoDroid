package im.zego.others.mediaplayer.UI;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import im.zego.others.R;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;

import im.zego.commontools.logtools.AppLogger;
import im.zego.commontools.logtools.LogView;
import im.zego.commontools.logtools.logLinearLayout;
import im.zego.commontools.uitools.ZegoViewUtil;
import im.zego.keycenter.KeyCenter;
import im.zego.keycenter.UserIDHelper;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.ZegoMediaPlayer;
import im.zego.zegoexpress.callback.IZegoApiCalledEventHandler;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.callback.IZegoMediaPlayerEventHandler;
import im.zego.zegoexpress.callback.IZegoMediaPlayerLoadResourceCallback;
import im.zego.zegoexpress.callback.IZegoMediaPlayerSeekToCallback;
import im.zego.zegoexpress.constants.ZegoAlphaLayoutType;
import im.zego.zegoexpress.constants.ZegoMediaPlayerAudioChannel;
import im.zego.zegoexpress.constants.ZegoMediaPlayerFirstFrameEvent;
import im.zego.zegoexpress.constants.ZegoPlayerState;
import im.zego.zegoexpress.constants.ZegoPublisherState;
import im.zego.zegoexpress.constants.ZegoRoomStateChangedReason;
import im.zego.zegoexpress.constants.ZegoScenario;
import im.zego.zegoexpress.constants.ZegoVideoConfigPreset;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoEngineConfig;
import im.zego.zegoexpress.entity.ZegoEngineProfile;
import im.zego.zegoexpress.entity.ZegoMediaPlayerResource;
import im.zego.zegoexpress.entity.ZegoUser;
import im.zego.zegoexpress.entity.ZegoVideoConfig;
import im.zego.zegoexpress.entity.ZegoVoiceChangerParam;
import im.zego.zegoexpress.constants.ZegoMultimediaLoadType;
import im.zego.others.beautyandwatermarkandsnapshot.ImageFilePath;

public class MediaPlayerActivity extends AppCompatActivity {

    Button startPublishingButton;
    Button startPlayingButton;
    TextureView preview;
    TextureView playView;
    TextView userIDText;
    TextView mediaTypeText;
    Button playButton;
    Button pauseButton;
    Button resumeButton;
    Button stopButton;
    SeekBar volumeSeekBar;
    SwitchMaterial repeatSwitch;
    SwitchMaterial auxSwitch;
    SwitchMaterial muteLocal;
    RadioGroup audioTrackIndex;
    TextView pitchText;
    SeekBar pitchSeekBar;
    TextView speedText;
    SeekBar speedSeekBar;
    TextureView mediaPlayerView;
    SeekBar progressBar;
    TextView roomState;

    EditText encodeResolutionWidth;
    EditText encodeResolutionHeight;
    EditText captureResolutionWidth;
    EditText captureResolutionHeight;
    TextView encodeResolutionTitle;
    TextView captureResolutionTitle;
    Button setBackgroundButton;
    LinearLayout mediaPlayerParentView;

    Long appID;
    String appSign;
    String userID;
    String roomID;
    String streamID;
    ZegoExpressEngine engine;
    ZegoMediaPlayer mediaPlayer;
    String path;

    //Store whether the user is playing the stream
    Boolean isPlay = false;
    //Store whether the user is publishing the stream
    Boolean isPublish = false;
    ZegoVoiceChangerParam voiceChangerParam = new ZegoVoiceChangerParam();
    Boolean alphaBlend;
    ZegoAlphaLayoutType alphaLayoutType;
    public static final int PICK_IMAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player);
        bindView();
        setDefaultValue();
        getAppIDAndUserIDAndAppSign();
        initEngineAndUser();
        loginRoom();
        setLogComponent();
        setEventHandler();
        setApiCalledResult();
        setStartPublishButtonEvent();
        setStartPlayButtonEvent();
        setMediaPlayer();
        setMediaPlayerEventHandler();
        setProgressBarEvent();
        setPlayButtonEvent();
        setPauseButtonEvent();
        setResumeButtonEvent();
        setStopButtonEvent();
        setVolumeSeekBarEvent();
        setRepeatSwitchEvent();
        setEnableAuxSwitchEvent();
        setMuteLocalSwitchEvent();
        setAudioTrackIndexEvent();
        setPitchSeekBarEvent();
        setSpeedSeekBarEvent();
        addQuestionToast();
        setBackgroundEventHandler();
    }
    public void bindView(){
        userIDText = findViewById(R.id.userIDText);
        mediaTypeText = findViewById(R.id.mediaType);
        preview = findViewById(R.id.PreviewView);
        playView = findViewById(R.id.PlayView);
        startPublishingButton = findViewById(R.id.startPublishButton);
        startPlayingButton = findViewById(R.id.startPlayButton);
        mediaTypeText = findViewById(R.id.mediaType);
        progressBar = findViewById(R.id.mediaPlayerSeekBar);
        playButton = findViewById(R.id.playButton);
        pauseButton = findViewById(R.id.pauseButton);
        resumeButton = findViewById(R.id.resumeButton);
        stopButton = findViewById(R.id.stopButton);
        volumeSeekBar = findViewById(R.id.volumeSeekBar);
        repeatSwitch = findViewById(R.id.repeatSwitch);
        auxSwitch = findViewById(R.id.enableAuxSwitch);
        muteLocal = findViewById(R.id.muteLocalSwitch);
        audioTrackIndex = findViewById(R.id.audioTrack);
        pitchText = findViewById(R.id.pitch);
        speedText = findViewById(R.id.speed);
        pitchSeekBar = findViewById(R.id.pitchSeekBar);
        speedSeekBar = findViewById(R.id.speedSeekBar);
        mediaPlayerView = findViewById(R.id.mediaPlayerView);
        roomState = findViewById(R.id.roomState);

        encodeResolutionWidth = findViewById(R.id.encodeResolutionWidthMP);
        encodeResolutionHeight = findViewById(R.id.encodeResolutionHeightMP);
        captureResolutionWidth = findViewById(R.id.captureResolutionWidthMP);
        captureResolutionHeight = findViewById(R.id.captureResolutionHeightMP);
        encodeResolutionTitle = findViewById(R.id.encodeResolutionTitleMP);
        captureResolutionTitle = findViewById(R.id.captureResolutionTitleMP);
        setBackgroundButton = findViewById(R.id.buttonSetBackground);

        mediaPlayerParentView = findViewById(R.id.mediaPlayerParentView);
    }

    //get appID and userID and appSign
    public void getAppIDAndUserIDAndAppSign(){
        appID = KeyCenter.getInstance().getAppID();
        userID = UserIDHelper.getInstance().getUserID();
        appSign = KeyCenter.getInstance().getAppSign();
    }

    public void initEngineAndUser(){
        if(getIntent().getBooleanExtra("mediaHardwareDecode", true)){
            HashMap<String, String> advanceConfig = new HashMap<>();
            advanceConfig.put("mediaplayer_hardware_decode", "true");

            ZegoEngineConfig config = new ZegoEngineConfig();
            config.advancedConfig = advanceConfig;
            ZegoExpressEngine.setEngineConfig(config);
            AppLogger.getInstance().callApi("enableHardwareDecoder: 1");
        }else{
            HashMap<String, String> advanceConfig = new HashMap<>();
            advanceConfig.put("mediaplayer_hardware_decode", "false");
            ZegoEngineConfig config = new ZegoEngineConfig();
            config.advancedConfig = advanceConfig;
            ZegoExpressEngine.setEngineConfig(config);
            AppLogger.getInstance().callApi("enableHardwareDecoder: 0");
        }

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
    }
    public void loginRoom(){
        //login room
        engine.loginRoom(roomID, new ZegoUser(userID));
        AppLogger.getInstance().callApi("LoginRoom: %s",roomID);
        //enable the camera
        engine.enableCamera(true);
        //enable the microphone
        engine.muteMicrophone(false);
        //enable the speaker
        engine.muteSpeaker(false);
    }

    public void setDefaultValue(){
        userID = UserIDHelper.getInstance().getUserID();
        roomID = "0027";
        streamID = "0027";
        // Get data from intent
        path = getIntent().getStringExtra("path");
        boolean isVideo = getIntent().getBooleanExtra("type",true);
        if (isVideo){
            mediaTypeText.setText("Video");
        } else {
            mediaTypeText.setText("Audio");
        }
        setTitle(getString(R.string.media_player));
        ZegoViewUtil.UpdateRoomState(roomState, ZegoRoomStateChangedReason.LOGINED);
        userIDText.setText(userID);

        alphaBlend = getIntent().getBooleanExtra("alphaBlend", false);
        alphaLayoutType = ZegoAlphaLayoutType.values()[getIntent().getIntExtra("alphaLayout", 0)];

        if(alphaBlend){
            mediaPlayerView.setOpaque(false);
        }
    }
    public void setStartPublishButtonEvent(){
        startPublishingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if the user is publishing the stream, this button is used to stop publishing. Otherwise, this button is used to start publishing.
                if (isPublish){
                    engine.stopPreview();
                    engine.stopPublishingStream();
                    AppLogger.getInstance().callApi("Stop Publishing Stream:%s",streamID);
                    startPublishingButton.setText(getString(R.string.start_publishing));
                    isPublish = false;
                    encodeResolutionHeight.setEnabled(true);
                    encodeResolutionWidth.setEnabled(true);
                    captureResolutionHeight.setEnabled(true);
                    captureResolutionWidth.setEnabled(true);
                } else {
                    ZegoVideoConfig videoConfig = new ZegoVideoConfig(ZegoVideoConfigPreset.PRESET_360P);
                    if (encodeResolutionWidth.getText().toString().equals(""))
                    {
                        Toast.makeText(getApplicationContext(), "Encode Width cannot be Empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        videoConfig.encodeWidth = Integer.parseInt(encodeResolutionWidth.getText().toString());
                        encodeResolutionWidth.setEnabled(false);
                    } catch (NumberFormatException e)
                    {
                        Toast.makeText(getApplicationContext(), "Encode Width is too large", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (encodeResolutionHeight.getText().toString().equals(""))
                    {
                        Toast.makeText(getApplicationContext(), "Encode Height cannot be Empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        videoConfig.encodeHeight = Integer.parseInt(encodeResolutionHeight.getText().toString());
                        encodeResolutionHeight.setEnabled(false);
                    } catch (NumberFormatException e)
                    {
                        Toast.makeText(getApplicationContext(), "Encode Height is too large", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (captureResolutionWidth.getText().toString().equals(""))
                    {
                        Toast.makeText(getApplicationContext(), "Capture Width cannot be Empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        videoConfig.captureWidth = Integer.parseInt(captureResolutionWidth.getText().toString());
                        captureResolutionWidth.setEnabled(false);
                    } catch (NumberFormatException e)
                    {
                        Toast.makeText(getApplicationContext(), "Capture Width is too large", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (captureResolutionHeight.getText().toString().equals(""))
                    {
                        Toast.makeText(getApplicationContext(), "Capture Height cannot be Empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        videoConfig.captureHeight = Integer.parseInt(captureResolutionHeight.getText().toString());
                        captureResolutionHeight.setEnabled(false);
                    } catch (NumberFormatException e)
                    {
                        Toast.makeText(getApplicationContext(), "Capture Height is too large", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    engine.setVideoConfig(videoConfig);
                    engine.startPreview(new ZegoCanvas(preview));
                    engine.startPublishingStream(streamID);
                    AppLogger.getInstance().callApi("Start Publishing Stream:%s",streamID);
                    startPublishingButton.setText(getString(R.string.stop_publishing));
                    isPublish = true;
                }
            }
        });
    }
    public void setStartPlayButtonEvent(){
        startPlayingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if the user is playing the stream, this button is used to stop playing. Otherwise, this button is used to start publishing.
                if (isPlay){
                    engine.stopPlayingStream(streamID);
                    AppLogger.getInstance().callApi("Stop Playing Stream:%s",streamID);
                    startPlayingButton.setText(getString(R.string.start_playing));
                    isPlay = false;
                } else {
                    engine.startPlayingStream(streamID, new ZegoCanvas(playView));
                    startPlayingButton.setText(getString(R.string.stop_playing));
                    AppLogger.getInstance().callApi("Start Playing Stream:%s",streamID);
                    isPlay = true;
                }
            }
        });
    }
    public void setMediaPlayer(){
        mediaPlayer = engine.createMediaPlayer();
        AppLogger.getInstance().callApi("create Media Player!");
        ZegoCanvas canvas = new ZegoCanvas(mediaPlayerView);
        canvas.alphaBlend = alphaBlend;
        mediaPlayer.setPlayerCanvas(canvas);
        ZegoMediaPlayerResource resource = new ZegoMediaPlayerResource();
        resource.loadType = ZegoMultimediaLoadType.FILE_PATH;
        resource.filePath = path;
        resource.alphaLayout = alphaLayoutType;
        mediaPlayer.loadResourceWithConfig(resource, new IZegoMediaPlayerLoadResourceCallback() {
            @Override
            public void onLoadResourceCallback(int errorCode) {
                if (errorCode == 0){
                    AppLogger.getInstance().receiveCallback("Load resource successfully!");
                    progressBar.setMax((int) (mediaPlayer.getTotalDuration()/1000));
                } else {
                    AppLogger.getInstance().fail("[%d] Fail to load resource...",errorCode);
                }
            }
        });
//        mediaPlayer.loadResource(path, new IZegoMediaPlayerLoadResourceCallback() {
//            @Override
//            public void onLoadResourceCallback(int i) {
//                if (i == 0){
//                    AppLogger.getInstance().receiveCallback("Load resource successfully!");
//                    progressBar.setMax((int) (mediaPlayer.getTotalDuration()/1000));
//                } else {
//                    AppLogger.getInstance().fail("[%d] Fail to load resource...",i);
//                }
//            }
//        });
    }
    public void setMediaPlayerEventHandler(){
        mediaPlayer.setEventHandler(new IZegoMediaPlayerEventHandler() {
            @Override
            public void onMediaPlayerPlayingProgress(ZegoMediaPlayer mediaPlayer, long millisecond) {
                super.onMediaPlayerPlayingProgress(mediaPlayer, millisecond);
                int second = (int)(millisecond/1000);
                progressBar.setProgress(second);
            }

            @Override
            public void onMediaPlayerFirstFrameEvent(ZegoMediaPlayer mediaPlayer, ZegoMediaPlayerFirstFrameEvent event) {
                super.onMediaPlayerFirstFrameEvent(mediaPlayer, event);
                AppLogger.getInstance().receiveCallback("Media player first frame event: %s", event);
            }
        });
    }

    public void setPlayButtonEvent(){
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.start();
                AppLogger.getInstance().callApi("Start Playing");
            }
        });
    }
    public void setPauseButtonEvent(){
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.pause();
                AppLogger.getInstance().callApi("Pause Playing");
            }
        });
    }
    public void setResumeButtonEvent(){
        resumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.resume();
                AppLogger.getInstance().callApi("Resume Playing");
            }
        });
    }
    public void setStopButtonEvent(){
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.stop();
                AppLogger.getInstance().callApi("Stop Playing");
            }
        });
    }
    public void setProgressBarEvent(){
        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = progressBar.getProgress();
                mediaPlayer.seekTo((long) (progress * 1000), new IZegoMediaPlayerSeekToCallback() {
                    @Override
                    public void onSeekToTimeCallback(int i) {
                        if (i == 0){
                            AppLogger.getInstance().receiveCallback("Seek to a given position successfully!");
                        } else {
                            AppLogger.getInstance().fail("[%d] Failed to seek to a given position",i);
                        }
                    }
                });
            }
        });
    }
    public void setVolumeSeekBarEvent(){
        volumeSeekBar.setMax(200);
        volumeSeekBar.setProgress(60);
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mediaPlayer.setVolume(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }
    public void setRepeatSwitchEvent(){
        repeatSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mediaPlayer.enableRepeat(isChecked);
            }
        });
    }
    public void setEnableAuxSwitchEvent(){
        auxSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mediaPlayer.enableAux(isChecked);
            }
        });
    }
    public void setMuteLocalSwitchEvent(){
        muteLocal.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mediaPlayer.muteLocal(isChecked);
            }
        });
    }
    public void setAudioTrackIndexEvent(){
        audioTrackIndex.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.audioTrack1){
                    mediaPlayer.setAudioTrackIndex(0);
                    AppLogger.getInstance().callApi("Set Audio Track: 0");
                } else {
                    mediaPlayer.setAudioTrackIndex(1);
                    AppLogger.getInstance().callApi("Set Audio Track: 1");
                }
            }
        });
    }
    public void setPitchSeekBarEvent(){
        pitchText.setText("0.00");
        pitchSeekBar.setMax(1600);
        pitchSeekBar.setProgress(800);
        pitchSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                voiceChangerParam.pitch = (float)(progress-800)/100;
                mediaPlayer.setVoiceChangerParam(ZegoMediaPlayerAudioChannel.ALL,voiceChangerParam);
                pitchText.setText(String.valueOf(voiceChangerParam.pitch));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }
    public void setSpeedSeekBarEvent(){
        speedText.setText("1.00");
        speedSeekBar.setMax(250);
        speedSeekBar.setProgress(50);
        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float curSpeed = (float)(progress+50)/100;
                mediaPlayer.setPlaySpeed(curSpeed);
                speedText.setText(String.valueOf(curSpeed));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public void setBackgroundEventHandler(){
        setBackgroundButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //choose the watermark from the file.
                Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
                getIntent.setType("image/*");

                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickIntent.setType("image/*");

                Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickIntent});

                startActivityForResult(chooserIntent, PICK_IMAGE);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && data!=null) {
            // Get chosen file path
            String realPath = ImageFilePath.getPath(getApplicationContext(), data.getData());

            setBackgroundPicture(realPath);
        }
    }

    public void setBackgroundPicture(String path){
        File file = new File(path);
        if(file.exists())
        {
            try {
                int width = mediaPlayerParentView.getWidth();
                int height = mediaPlayerParentView.getHeight();
                Bitmap bmp1 = BitmapFactory.decodeFile(path);

                if(bmp1 != null){
                    Bitmap bmp = Bitmap.createScaledBitmap(bmp1, width, height, true);
                    BitmapDrawable drawable = new BitmapDrawable(mediaPlayerParentView.getResources(), bmp);
                    mediaPlayerParentView.setBackground(drawable);
                }
                else
                {
                    AppLogger.getInstance().fail(String.format("BitmapFactory.decodeFile fail!"));
                }
            }catch(Exception e){
                AppLogger.getInstance().fail(String.format("Set background exception:%s", e.toString()));
            }
        }
    }

    public void setEventHandler(){
        engine.setEventHandler(new IZegoEventHandler() {
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
    public void addQuestionToast() {
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view.getId() == R.id.encodeResolutionTitleMP) {
                    Toast.makeText(getApplicationContext(), R.string.encodeResolutionTitle, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (view.getId() == R.id.captureResolutionTitleMP) {
                    Toast.makeText(getApplicationContext(), R.string.captureResolutionTitle, Toast.LENGTH_SHORT).show();
                    return;
                }
            }

        };

        //add Toast in order to show info
        encodeResolutionTitle.setOnClickListener(listener);
        captureResolutionTitle.setOnClickListener(listener);
    }
    @Override
    protected void onDestroy() {
        engine.destroyMediaPlayer(mediaPlayer);
        engine.setEventHandler(null);
        ZegoExpressEngine.destroyEngine(null);
        super.onDestroy();
    }
}