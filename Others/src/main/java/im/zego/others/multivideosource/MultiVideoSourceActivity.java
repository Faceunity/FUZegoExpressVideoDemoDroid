package im.zego.others.multivideosource;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import im.zego.others.R;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import im.zego.advancedvideoprocessing.CustomerVideoCapture.VideoCaptureFromCamera;
import im.zego.advancedvideoprocessing.CustomerVideoCapture.VideoCaptureFromCamera2;
import im.zego.advancedvideoprocessing.CustomerVideoCapture.VideoCaptureFromCamera3;
import im.zego.advancedvideoprocessing.CustomerVideoCapture.VideoCaptureFromImage;
import im.zego.advancedvideoprocessing.CustomerVideoCapture.VideoCaptureFromImage2;
import im.zego.advancedvideoprocessing.CustomerVideoCapture.VideoCaptureFromImage3;
import im.zego.advancedvideoprocessing.CustomerVideoCapture.ZegoVideoCaptureCallback;
import im.zego.commontools.logtools.AppLogger;
import im.zego.commontools.logtools.LogView;
import im.zego.commontools.logtools.logLinearLayout;
import im.zego.commontools.uitools.ZegoViewUtil;
import im.zego.keycenter.KeyCenter;
import im.zego.keycenter.UserIDHelper;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.ZegoMediaPlayer;
import im.zego.zegoexpress.callback.IZegoApiCalledEventHandler;
import im.zego.zegoexpress.callback.IZegoDestroyCompletionCallback;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.callback.IZegoMediaPlayerLoadResourceCallback;
import im.zego.zegoexpress.constants.ZegoAudioSourceType;
import im.zego.zegoexpress.constants.ZegoMediaPlayerState;
import im.zego.zegoexpress.constants.ZegoPlayerState;
import im.zego.zegoexpress.constants.ZegoPublishChannel;
import im.zego.zegoexpress.constants.ZegoPublisherState;
import im.zego.zegoexpress.constants.ZegoRoomStateChangedReason;
import im.zego.zegoexpress.constants.ZegoScenario;
import im.zego.zegoexpress.constants.ZegoVideoBufferType;
import im.zego.zegoexpress.constants.ZegoVideoConfigPreset;
import im.zego.zegoexpress.constants.ZegoVideoSourceType;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoCustomVideoCaptureConfig;
import im.zego.zegoexpress.entity.ZegoEngineProfile;
import im.zego.zegoexpress.entity.ZegoScreenCaptureConfig;
import im.zego.zegoexpress.entity.ZegoUser;
import im.zego.zegoexpress.entity.ZegoVideoConfig;

public class MultiVideoSourceActivity extends AppCompatActivity {

    TextView engineState;
    Button createEngineButton;
    EditText roomIDEdit;
    TextView roomState;
    TextView userIDText;
    Button loginRoomButton;
    EditText mainPublishStreamIDEdit;
    EditText auxPublishStreamIDEdit;
    EditText mainPlayStreamIDEdit;
    EditText auxPlayStreamIDEdit;
    Button mainPublishButton;
    Button auxPublishButton;
    Button mainPlayButton;
    Button auxPlayButton;
    TextureView mainPreviewView;
    TextureView auxPreviewView;
    TextureView mainPlayView;
    TextureView auxPlayView;
    LinearLayout mainPreviewViewLayout;
    LinearLayout auxPreviewViewLayout;

    String roomID;
    String userID;
    String mainPublishStreamID;
    String auxPublishStreamID;
    String mainPlayStreamID;
    String auxPlayStreamID;

    Spinner mainVideoSourceSpinner;
    Spinner auxVideoSourceSpinner;
    Spinner mainAudioSourceSpinner;
    Spinner auxAudioSourceSpinner;

    ZegoExpressEngine engine;
    ZegoMediaPlayer mediaPlayer;
    Long appID;
    String appSign;
    ZegoUser user;
    boolean isCreateEngine = false;
    boolean isLoginRoom = false;

    ZegoScreenCaptureConfig mainScreenCaptureConfig = new ZegoScreenCaptureConfig();
    ZegoScreenCaptureConfig auxScreenCaptureConfig = new ZegoScreenCaptureConfig();

    ZegoVideoSourceType mainCurrentVideoSourceType = ZegoVideoSourceType.NONE;
    ZegoAudioSourceType mainCurrentAudioSourceType = ZegoAudioSourceType.NONE;
    ZegoVideoSourceType auxCurrentVideoSourceType = ZegoVideoSourceType.NONE;
    ZegoAudioSourceType auxCurrentAudioSourceType = ZegoAudioSourceType.NONE;

    int mainVideoPreviousPosition = 0;
    int auxVideoPreviousPosition = 0;
    int auxAudioPreviousPosition = 0;
    ZegoCanvas mainPreviewCanvas;
    ZegoCanvas auxPreviewCanvas;
    ZegoVideoCaptureCallback mainVideoCustomCapturer = null;
    ZegoVideoCaptureCallback auxVideoCustomCapturer = null;
    AudioCustomCapturer mainAudioCustomCapturer = null;
    AudioCustomCapturer auxAudioCustomCapturer = null;

    boolean isMainPublish = false;
    boolean isAuxPublish = false;
    boolean isMainPlay = false;
    boolean isAuxPlay = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_video_source);
        bindView();
        initData();
        requestPermission();
        setLogComponent();
        getAppIDAndUserIDAndAppSign();
        setDefaultValue();
        setCreateEngineEvent();
        setLoginRoomEvent();
        setPublishButtonEvent();
        setPlayButtonEvent();
        setSpinner();
        setApiCalledResult();
    }

    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, MultiVideoSourceActivity.class);
        activity.startActivity(intent);
    }

    public void bindView() {
        engineState = findViewById(R.id.engineState);
        createEngineButton = findViewById(R.id.createEngineButton);
        roomIDEdit = findViewById(R.id.roomID);
        userIDText = findViewById(R.id.userID);
        loginRoomButton = findViewById(R.id.loginRoomButton);
        mainPublishStreamIDEdit = findViewById(R.id.mainPublishStreamID);
        auxPublishStreamIDEdit = findViewById(R.id.auxPublishStreamID);
        mainPlayStreamIDEdit = findViewById(R.id.mainPlayStreamID);
        auxPlayStreamIDEdit = findViewById(R.id.auxPlayStreamID);
        mainPublishButton = findViewById(R.id.mainPublishButton);
        auxPublishButton = findViewById(R.id.auxPublishButton);
        mainPlayButton = findViewById(R.id.mainPlayButton);
        auxPlayButton = findViewById(R.id.auxPlayButton);
        mainPreviewView = findViewById(R.id.mainPreviewView);
        auxPreviewView = findViewById(R.id.auxPreviewView);
        mainPlayView = findViewById(R.id.mainPlayView);
        auxPlayView = findViewById(R.id.auxPlayView);
        roomState = findViewById(R.id.roomState);
        mainVideoSourceSpinner = findViewById(R.id.mainVideoSources);
        auxVideoSourceSpinner = findViewById(R.id.auxVideoSources);
        mainAudioSourceSpinner = findViewById(R.id.mainAudioSources);
        auxAudioSourceSpinner = findViewById(R.id.auxAudioSources);
        mainPreviewViewLayout = findViewById(R.id.mainPreviewViewLayout);
        auxPreviewViewLayout = findViewById(R.id.auxPreviewViewLayout);
    }

    private void initData() {
        copyAssetsFiles();
    }

    private void copyAssetsFiles() {
        new Thread() {
            public void run() {
                copyAssetsFile("ad.mp4");
            }
        }.start();
    }

    private void copyAssetsFile(String fileName) {
        final File file = new File(getExternalFilesDir(""), fileName);
        System.out.println("File Path---->" + file.getAbsolutePath());
        if (file.exists()) {
            System.out.println("File exists");
            return;
        }
        try {
            // Get Assets.
            AssetManager assetManager = getAssets();
            InputStream is = assetManager.open(fileName);
            FileOutputStream fos = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setDefaultValue() {
        roomID = "0035";
        mainPublishStreamID = "0035";
        auxPublishStreamID = "0036";
        mainPlayStreamID = "0035";
        auxPlayStreamID = "0036";
        userIDText.setText(userID);
        setTitle(getString(R.string.multi_video_source));

        //create the user
        user = new ZegoUser(userID);

        mainPreviewCanvas = new ZegoCanvas(mainPreviewView);
        auxPreviewCanvas = new ZegoCanvas(auxPreviewView);
    }

    //get appID and userID and appSign
    public void getAppIDAndUserIDAndAppSign() {
        appID = KeyCenter.getInstance().getAppID();
        userID = UserIDHelper.getInstance().getUserID();
        appSign = KeyCenter.getInstance().getAppSign();
    }

    public void setCreateEngineEvent() {
        createEngineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isCreateEngine) {
                    AppLogger.getInstance().callApi("Create ZegoExpressEngine");

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

                    ZegoVideoConfig videoConfig = new ZegoVideoConfig(ZegoVideoConfigPreset.PRESET_1080P);
                    engine.setVideoConfig(videoConfig);
                    engine.setVideoConfig(videoConfig, ZegoPublishChannel.AUX);

                    engine.enableCamera(true);

                    // main channel
                    engine.setVideoSource(mainCurrentVideoSourceType);
                    engine.setAudioSource(mainCurrentAudioSourceType);

                    // aux channel
                    if (auxCurrentVideoSourceType == ZegoVideoSourceType.PLAYER) {
                        if (mediaPlayer == null) {
                            initMediaPlayer();
                        }

                        mediaPlayer.setPlayerCanvas(auxPreviewCanvas);
                        engine.setVideoSource(auxCurrentVideoSourceType, mediaPlayer.getIndex(), ZegoPublishChannel.AUX);
                    } else {
                        engine.setVideoSource(auxCurrentVideoSourceType, ZegoPublishChannel.AUX);
                    }
                    engine.setAudioSource(auxCurrentAudioSourceType, ZegoPublishChannel.AUX);
                    if (auxCurrentAudioSourceType == ZegoAudioSourceType.MEDIA_PLAYER) {
                        if (mediaPlayer == null) {
                            initMediaPlayer();
                        }
                    }

                    setEventHandler();

                    isCreateEngine = true;
                    createEngineButton.setText(getString(R.string.destroy_engine));
                    engineState.setText(new String(Character.toChars(ZegoViewUtil.roomLoginedEmoji)));
                } else {
                    if (isLoginRoom) {
                        logoutRoom();
                    }

                    if (mediaPlayer != null) {
                        engine.destroyMediaPlayer(mediaPlayer);
                        mediaPlayer = null;
                    }

                    ZegoExpressEngine.destroyEngine(new IZegoDestroyCompletionCallback() {
                        @Override
                        public void onDestroyCompletion() {
                            isCreateEngine = false;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    engineState.setText(new String(Character.toChars(ZegoViewUtil.roomLogoutEmoji)));
                                }
                            });
                        }
                    });

                    isCreateEngine = false;
                    createEngineButton.setText(getString(R.string.create_engine));
                }
            }
        });
    }

    public void setLoginRoomEvent() {
        loginRoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isCreateEngine) {
                    AppLogger.getInstance().callApi("create engine first.");
                    Toast.makeText(MultiVideoSourceActivity.this, "create engine first.", Toast.LENGTH_SHORT).show();

                    return;
                }

                if (!isLoginRoom) {
                    roomID = roomIDEdit.getText().toString();

                    // login room
                    AppLogger.getInstance().callApi("LoginRoom: %s", roomID);
                    engine.loginRoom(roomID, user);

                    isLoginRoom = true;
                    loginRoomButton.setText(getString(R.string.logout_room));
                } else {
                    logoutRoom();
                }
            }
        });
    }

    public void setPublishButtonEvent() {
        mainPublishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isLoginRoom) {
                    AppLogger.getInstance().callApi("login room first.");
                    Toast.makeText(MultiVideoSourceActivity.this, "login room first.", Toast.LENGTH_SHORT).show();

                    return;
                }

                if (isMainPublish){
                    stopMainPublish();
                } else {
                    if (isMainUseCamera() && isAuxPublish && isAuxUseCamera()) {
                        AppLogger.getInstance().callApi("Start Publishing main channel failed, video source type camera already used in aux channel!");
                        Toast.makeText(MultiVideoSourceActivity.this, "Start Publishing main channel failed, video source type camera already used in aux channel!", Toast.LENGTH_LONG).show();

                        return;
                    }

                    if (isAuxPublish && (mainCurrentVideoSourceType == ZegoVideoSourceType.SCREEN_CAPTURE &&
                            auxCurrentVideoSourceType == ZegoVideoSourceType.SCREEN_CAPTURE) || (mainCurrentAudioSourceType == ZegoAudioSourceType.SCREEN_CAPTURE &&
                            auxCurrentAudioSourceType == ZegoAudioSourceType.SCREEN_CAPTURE)) {
                        AppLogger.getInstance().callApi("Start Publishing main channel failed, screen capture source already used in aux channel!");
                        Toast.makeText(MultiVideoSourceActivity.this, "Start Publishing main channel failed, screen capture source already used in aux channel!", Toast.LENGTH_LONG).show();


                        return;
                    }

                    mainPublishStreamID = mainPublishStreamIDEdit.getText().toString();
                    if (TextUtils.isEmpty(mainPublishStreamID)) {
                        AppLogger.getInstance().callApi("empty stream id");

                        Toast.makeText(getApplicationContext(), "empty stream id", Toast.LENGTH_LONG).show();

                        return;
                    }

                    if (isAuxPublish && TextUtils.equals(mainPublishStreamID, auxPublishStreamID)) {
                        AppLogger.getInstance().callApi("already published stream id");

                        Toast.makeText(getApplicationContext(), "already published stream id", Toast.LENGTH_LONG).show();

                        return;
                    }

                    engine.callExperimentalAPI("{\"method\":\"liveroom.video.set_video_fill_mode\",\"params\":{\"mode\":0,\"channel\":0}}");

                    engine.startPreview(mainPreviewCanvas);

                    AppLogger.getInstance().callApi("Start Publishing main channel: %s", mainPublishStreamID);
                    engine.startPublishingStream(mainPublishStreamID);

                    if (mainCurrentVideoSourceType == ZegoVideoSourceType.SCREEN_CAPTURE) {
                        engine.startScreenCapture(mainScreenCaptureConfig);
                    } else if (mainCurrentVideoSourceType == ZegoVideoSourceType.CUSTOM) {
                        boolean isCamera = true;
                        String itemText = (String) mainVideoSourceSpinner.getSelectedItem();
                        ZegoVideoBufferType bufferType = ZegoVideoBufferType.RAW_DATA;
                        if (TextUtils.equals(itemText, getString(R.string.video_source_custom_capture_gl_texture_2d))) {
                            bufferType = ZegoVideoBufferType.GL_TEXTURE_2D;
                        } else if (TextUtils.equals(itemText, getString(R.string.video_source_custom_capture_encoded_data))) {
                            bufferType = ZegoVideoBufferType.ENCODED_DATA;
                        } else if (TextUtils.equals(itemText, getString(R.string.video_source_custom_capture_image_raw_data))) {
                            isCamera = false;
                        } else if (TextUtils.equals(itemText, getString(R.string.video_source_custom_capture_image_gl_texture_2d))) {
                            isCamera = false;
                            bufferType = ZegoVideoBufferType.GL_TEXTURE_2D;
                        } else if (TextUtils.equals(itemText, getString(R.string.video_source_custom_capture_image_encoded_data))) {
                            isCamera = false;
                            bufferType = ZegoVideoBufferType.ENCODED_DATA;
                        }
                        mainVideoCustomCapturer = initCustomVideoCapturer(isCamera, mainPreviewView, bufferType, ZegoPublishChannel.MAIN);
                    }

                    if (mainCurrentAudioSourceType == ZegoAudioSourceType.CUSTOM) {
                        boolean useDevice = true;
                        int audioFrameType = 0;
                        String itemText = (String) mainAudioSourceSpinner.getSelectedItem();
                        if (TextUtils.equals(getString(R.string.audio_source_custom_capture_file_pcm), itemText)) {
                            useDevice = false;
                        } else if (TextUtils.equals(getString(R.string.audio_source_custom_capture_file_aac), itemText)) {
                            useDevice = false;
                            audioFrameType = 1;
                        }
                        mainAudioCustomCapturer = initAudioCustomCapturer(ZegoPublishChannel.MAIN, useDevice, audioFrameType);
                    }

                    mainPublishButton.setText(getString(R.string.stop_publishing));

                    isMainPublish = true;
                }
            }
        });

        auxPublishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isLoginRoom) {
                    AppLogger.getInstance().callApi("login room first.");
                    Toast.makeText(MultiVideoSourceActivity.this, "login room first.", Toast.LENGTH_SHORT).show();

                    return;
                }

                if (isAuxPublish){
                    stopAuxPublish();
                } else {
                    if (isAuxUseCamera() && isMainPublish && isMainUseCamera()) {
                        AppLogger.getInstance().callApi("Start Publishing aux channel failed, camera source already used in main channel!");
                        Toast.makeText(MultiVideoSourceActivity.this, "Start Publishing aux channel failed, camera source already used in main channel!", Toast.LENGTH_LONG).show();


                        return;
                    }

                    if (isMainPublish && (mainCurrentVideoSourceType == ZegoVideoSourceType.SCREEN_CAPTURE &&
                            auxCurrentVideoSourceType == ZegoVideoSourceType.SCREEN_CAPTURE) || (mainCurrentAudioSourceType == ZegoAudioSourceType.SCREEN_CAPTURE &&
                            auxCurrentAudioSourceType == ZegoAudioSourceType.SCREEN_CAPTURE)) {
                        AppLogger.getInstance().callApi("Start Publishing aux channel failed, screen capture source already used in main channel!");
                        Toast.makeText(MultiVideoSourceActivity.this, "Start Publishing aux channel failed, screen capture source already used in main channel!", Toast.LENGTH_LONG).show();


                        return;
                    }

                    if (auxCurrentVideoSourceType == ZegoVideoSourceType.MAIN_PUBLISH_CHANNEL ||
                            auxCurrentAudioSourceType == ZegoAudioSourceType.MAIN_PUBLISH_CHANNEL) {
                        if (!isMainPublish) {
                            AppLogger.getInstance().callApi("Start Publishing aux channel failed, main publish channel must started when aux channel use main publish channel");

                            Toast.makeText(getApplicationContext(), "Start Publishing aux channel failed, main publish channel must started when aux channel use main publish channel", Toast.LENGTH_LONG).show();

                            return;
                        }
                    }

                    if (auxCurrentVideoSourceType == ZegoVideoSourceType.MAIN_PUBLISH_CHANNEL) {
                        if (isMainPublish && mainCurrentVideoSourceType != ZegoVideoSourceType.CAMERA) {
                            AppLogger.getInstance().callApi("Start Publishing aux channel failed, main channel must use camera when aux channel use main publish channel");

                            Toast.makeText(getApplicationContext(), "Start Publishing aux channel failed, main channel must use camera when aux channel use main publish channel", Toast.LENGTH_LONG).show();

                            return;
                        }
                    }

                    if (auxCurrentAudioSourceType == ZegoAudioSourceType.MAIN_PUBLISH_CHANNEL) {
                        if (isMainPublish && mainCurrentAudioSourceType != ZegoAudioSourceType.MICROPHONE) {
                            AppLogger.getInstance().callApi("Start Publishing aux channel failed, main channel must use microphone when aux channel use main publish channel");

                            Toast.makeText(getApplicationContext(), "Start Publishing aux channel failed, main channel must use microphone when aux channel use main publish channel", Toast.LENGTH_LONG).show();

                            return;
                        }
                    }

                    auxPublishStreamID = auxPublishStreamIDEdit.getText().toString();
                    if (TextUtils.isEmpty(auxPublishStreamID)) {
                        AppLogger.getInstance().callApi("empty stream id");

                        Toast.makeText(getApplicationContext(), "empty stream id", Toast.LENGTH_LONG).show();

                        return;
                    }

                    if (isMainPublish && TextUtils.equals(mainPublishStreamID, auxPublishStreamID)) {
                        AppLogger.getInstance().callApi("already published stream id");

                        Toast.makeText(getApplicationContext(), "already published stream id", Toast.LENGTH_LONG).show();

                        return;
                    }

                    engine.callExperimentalAPI("{\"method\":\"liveroom.video.set_video_fill_mode\",\"params\":{\"mode\":0,\"channel\":1}}");

                    engine.startPreview(auxPreviewCanvas, ZegoPublishChannel.AUX);

                    AppLogger.getInstance().callApi("Start Publishing aux channel: %s", auxPublishStreamID);
                    engine.startPublishingStream(auxPublishStreamID, ZegoPublishChannel.AUX);

                    if (auxCurrentVideoSourceType == ZegoVideoSourceType.SCREEN_CAPTURE) {
                        engine.startScreenCapture(auxScreenCaptureConfig);
                    } else if (auxCurrentVideoSourceType == ZegoVideoSourceType.CUSTOM) {
                        boolean isCamera = true;
                        String itemText = (String) auxVideoSourceSpinner.getItemAtPosition(auxVideoPreviousPosition);
                        ZegoVideoBufferType bufferType = ZegoVideoBufferType.RAW_DATA;
                        if (TextUtils.equals(itemText, getString(R.string.video_source_custom_capture_gl_texture_2d))) {
                            bufferType = ZegoVideoBufferType.GL_TEXTURE_2D;
                        } else if (TextUtils.equals(itemText, getString(R.string.video_source_custom_capture_encoded_data))) {
                            bufferType = ZegoVideoBufferType.ENCODED_DATA;
                        } else if (TextUtils.equals(itemText, getString(R.string.video_source_custom_capture_image_raw_data))) {
                            isCamera = false;
                        } else if (TextUtils.equals(itemText, getString(R.string.video_source_custom_capture_image_gl_texture_2d))) {
                            isCamera = false;
                            bufferType = ZegoVideoBufferType.GL_TEXTURE_2D;
                        } else if (TextUtils.equals(itemText, getString(R.string.video_source_custom_capture_image_encoded_data))) {
                            isCamera = false;
                            bufferType = ZegoVideoBufferType.ENCODED_DATA;
                        }
                        auxVideoCustomCapturer = initCustomVideoCapturer(isCamera, auxPreviewView, bufferType, ZegoPublishChannel.AUX);
                    }

                    if (auxCurrentAudioSourceType == ZegoAudioSourceType.CUSTOM) {
                        boolean useDevice = true;
                        int audioFrameType = 0;
                        String itemText = (String) auxAudioSourceSpinner.getSelectedItem();
                        if (TextUtils.equals(getString(R.string.audio_source_custom_capture_file_pcm), itemText)) {
                            useDevice = false;
                        } else if (TextUtils.equals(getString(R.string.audio_source_custom_capture_file_aac), itemText)) {
                            useDevice = false;
                            audioFrameType = 1;
                        }
                        auxAudioCustomCapturer = initAudioCustomCapturer(ZegoPublishChannel.AUX, useDevice, audioFrameType);
                    }

                    if (auxCurrentVideoSourceType == ZegoVideoSourceType.PLAYER || auxCurrentAudioSourceType == ZegoAudioSourceType.MEDIA_PLAYER) {
                        loadMediaPlayerResource();
                    }

                    auxPublishButton.setText(getString(R.string.stop_publishing));

                    isAuxPublish = true;
                }
            }
        });
    }

    public void setPlayButtonEvent() {
        mainPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isLoginRoom) {
                    AppLogger.getInstance().callApi("login room first.");
                    Toast.makeText(MultiVideoSourceActivity.this, "login room first.", Toast.LENGTH_SHORT).show();

                    return;
                }

                //if the user is playing the stream, this button is used to stop playing. Otherwise, this button is used to start publishing.
                if (isMainPlay){
                    stopMainPlay();
                } else {
                    mainPlayStreamID = mainPlayStreamIDEdit.getText().toString();
                    if (TextUtils.isEmpty(mainPlayStreamID)) {
                        AppLogger.getInstance().callApi("empty stream id");

                        Toast.makeText(getApplicationContext(), "empty stream id", Toast.LENGTH_LONG).show();

                        return;
                    }

                    if (isAuxPlay && TextUtils.equals(mainPlayStreamID, auxPlayStreamID)) {
                        AppLogger.getInstance().callApi("already played stream id");

                        Toast.makeText(getApplicationContext(), "already played stream id", Toast.LENGTH_LONG).show();

                        return;
                    }

                    AppLogger.getInstance().callApi("Start Playing Stream:%s", mainPlayStreamID);
                    engine.startPlayingStream(mainPlayStreamID, new ZegoCanvas(mainPlayView));

                    mainPlayButton.setText(getString(R.string.stop_playing));

                    isMainPlay = true;
                }
            }
        });

        auxPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isLoginRoom) {
                    AppLogger.getInstance().callApi("login room first.");
                    Toast.makeText(MultiVideoSourceActivity.this, "login room first.", Toast.LENGTH_SHORT).show();

                    return;
                }

                //if the user is playing the stream, this button is used to stop playing. Otherwise, this button is used to start publishing.
                if (isAuxPlay){
                    stopAuxPlay();
                } else {
                    auxPlayStreamID = auxPlayStreamIDEdit.getText().toString();
                    if (TextUtils.isEmpty(auxPlayStreamID)) {
                        AppLogger.getInstance().callApi("empty stream id");

                        Toast.makeText(getApplicationContext(), "empty stream id", Toast.LENGTH_LONG).show();

                        return;
                    }

                    if (isMainPlay && TextUtils.equals(mainPlayStreamID, auxPlayStreamID)) {
                        AppLogger.getInstance().callApi("already played stream id");

                        Toast.makeText(getApplicationContext(), "already played stream id", Toast.LENGTH_LONG).show();

                        return;
                    }

                    AppLogger.getInstance().callApi("Start Playing Stream:%s", auxPlayStreamID);
                    engine.startPlayingStream(auxPlayStreamID, new ZegoCanvas(auxPlayView));

                    auxPlayButton.setText(getString(R.string.stop_playing));

                    isAuxPlay = true;
                }
            }
        });
    }

    public void setSpinner() {
        mainVideoSourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String itemText = (String) parent.getItemAtPosition(position);
                boolean isMainUseCamera = true;
                do {
                    if (TextUtils.equals(getString(R.string.video_source_camera), itemText)) {
                        break;
                    }

                    if (TextUtils.equals(getString(R.string.video_source_custom_capture_raw_data), itemText)) {
                        break;
                    }

                    if (TextUtils.equals(getString(R.string.video_source_custom_capture_gl_texture_2d), itemText)) {
                        break;
                    }

                    if (TextUtils.equals(getString(R.string.video_source_custom_capture_encoded_data), itemText)) {
                        break;
                    }

                    isMainUseCamera = false;
                } while (false);
                if (isMainPublish && isAuxPublish && isAuxUseCamera() && isMainUseCamera) {
                    AppLogger.getInstance().callApi("Switch main channel video source type to camera failed, the source already used in aux channel!");
                    Toast.makeText(MultiVideoSourceActivity.this, "Switch main channel video source type to camera failed, the source already used in aux channel!", Toast.LENGTH_LONG).show();

                    mainVideoSourceSpinner.setSelection(mainVideoPreviousPosition);

                    return;
                }

                if (isMainPublish && isAuxPublish && TextUtils.equals(getString(R.string.screen_sharing), itemText) && TextUtils.equals(getString(R.string.screen_sharing), auxVideoSourceSpinner.getSelectedItem().toString())) {
                    AppLogger.getInstance().callApi("Switch main channel to screen capture source failed, screen capture source already used in aux channel!");
                    Toast.makeText(MultiVideoSourceActivity.this, "Switch main channel to screen capture source failed, screen capture source already used in aux channel!", Toast.LENGTH_LONG).show();

                    auxVideoSourceSpinner.setSelection(auxVideoPreviousPosition);

                    return;
                }

                ZegoVideoSourceType videoSourceType;
                if (TextUtils.equals(getString(R.string.video_source_none), itemText)) { // none
                    videoSourceType = ZegoVideoSourceType.NONE;
                } else if (TextUtils.equals(getString(R.string.video_source_camera), itemText)) {
                    videoSourceType = ZegoVideoSourceType.CAMERA;
                } else if (TextUtils.equals(getString(R.string.video_source_screen_sharing), itemText)) {
                    videoSourceType = ZegoVideoSourceType.SCREEN_CAPTURE;
                } else { // custom capture
                    videoSourceType = ZegoVideoSourceType.CUSTOM;
                }
                mainVideoPreviousPosition = position;

                if (!isMainPublish) {
                    mainCurrentVideoSourceType = videoSourceType;
                    if (engine != null) {
                        engine.setVideoSource(mainCurrentVideoSourceType);
                    }
                } else if (videoSourceType != mainCurrentVideoSourceType) {
                    mainSwitchVideoSource(videoSourceType);
                } else if (videoSourceType == ZegoVideoSourceType.CUSTOM) {
                    if (mainVideoCustomCapturer != null) {
                        mainVideoCustomCapturer.onStop(ZegoPublishChannel.MAIN);
                    }
                    mainVideoCustomCapturer = null;

                    engine.stopPreview();

                    mainPreviewViewLayout.removeView(mainPreviewView);
                    mainPreviewViewLayout.addView(mainPreviewView);

                    boolean isCamera = true;
                    ZegoVideoBufferType bufferType = ZegoVideoBufferType.RAW_DATA;
                    if (TextUtils.equals(itemText, getString(R.string.video_source_custom_capture_gl_texture_2d))) {
                        bufferType = ZegoVideoBufferType.GL_TEXTURE_2D;
                    } else if (TextUtils.equals(itemText, getString(R.string.video_source_custom_capture_encoded_data))) {
                        bufferType = ZegoVideoBufferType.ENCODED_DATA;
                    } else if (TextUtils.equals(itemText, getString(R.string.video_source_custom_capture_image_raw_data))) {
                        isCamera = false;
                    } else if (TextUtils.equals(itemText, getString(R.string.video_source_custom_capture_image_gl_texture_2d))) {
                        isCamera = false;
                        bufferType = ZegoVideoBufferType.GL_TEXTURE_2D;
                    } else if (TextUtils.equals(itemText, getString(R.string.video_source_custom_capture_image_encoded_data))) {
                        isCamera = false;
                        bufferType = ZegoVideoBufferType.ENCODED_DATA;
                    }
                    mainVideoCustomCapturer = initCustomVideoCapturer(isCamera, mainPreviewView, bufferType, ZegoPublishChannel.MAIN);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        auxVideoSourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String itemText = (String) parent.getItemAtPosition(position);
                boolean isAuxUseCamera = true;
                do {
                    if (TextUtils.equals(getString(R.string.video_source_camera), itemText)) {
                        break;
                    }

                    if (TextUtils.equals(getString(R.string.video_source_custom_capture_raw_data), itemText)) {
                        break;
                    }

                    if (TextUtils.equals(getString(R.string.video_source_custom_capture_gl_texture_2d), itemText)) {
                        break;
                    }

                    if (TextUtils.equals(getString(R.string.video_source_custom_capture_encoded_data), itemText)) {
                        break;
                    }

                    isAuxUseCamera = false;
                } while (false);
                if (isMainPublish && isAuxPublish && isMainUseCamera() && isAuxUseCamera) {
                    AppLogger.getInstance().callApi("Switch aux channel to camera source failed, camera source already used in main channel!");
                    Toast.makeText(MultiVideoSourceActivity.this, "Switch aux channel to camera source failed, camera source already used in main channel!", Toast.LENGTH_LONG).show();

                    auxVideoSourceSpinner.setSelection(auxVideoPreviousPosition);

                    return;
                }
                if (isMainPublish && isAuxPublish && TextUtils.equals(getString(R.string.screen_sharing), itemText) && TextUtils.equals(getString(R.string.screen_sharing), mainVideoSourceSpinner.getSelectedItem().toString())) {
                    AppLogger.getInstance().callApi("Switch aux channel to screen capture source failed, screen capture source already used in main channel!");
                    Toast.makeText(MultiVideoSourceActivity.this, "Switch aux channel to screen capture source failed, screen capture source already used in main channel!", Toast.LENGTH_LONG).show();

                    auxVideoSourceSpinner.setSelection(auxVideoPreviousPosition);

                    return;
                }

                // 辅路使用复制主路视频输入源时，主路必须使用物理设备
                if (isMainPublish && isAuxPublish &&
                        TextUtils.equals(getString(R.string.video_source_main_publish_channel), itemText) &&
                        mainCurrentVideoSourceType != ZegoVideoSourceType.CAMERA) {
                    AppLogger.getInstance().callApi("main channel must use camera when switch aux channel to main publish channel!");
                    Toast.makeText(MultiVideoSourceActivity.this, "main channel must use camera when switch aux channel to main publish channel!", Toast.LENGTH_LONG).show();

                    auxVideoSourceSpinner.setSelection(auxVideoPreviousPosition);

                    return;
                }

                ZegoVideoSourceType videoSourceType = ZegoVideoSourceType.NONE;
                if (TextUtils.equals(getString(R.string.video_source_none), itemText)) {
                    videoSourceType = ZegoVideoSourceType.NONE;
                } else if (TextUtils.equals(getString(R.string.video_source_camera), itemText)) {
                    videoSourceType = ZegoVideoSourceType.CAMERA;
                } else if (TextUtils.equals(getString(R.string.video_source_media_player), itemText)
                        || TextUtils.equals(getString(R.string.video_source_media_player_using_network_resource), itemText)) {
                    videoSourceType = ZegoVideoSourceType.PLAYER;
                } else if (TextUtils.equals(getString(R.string.video_source_screen_sharing), itemText)) {
                    videoSourceType = ZegoVideoSourceType.SCREEN_CAPTURE;
                } else if (TextUtils.equals(getString(R.string.video_source_main_publish_channel), itemText)) {
                    videoSourceType = ZegoVideoSourceType.MAIN_PUBLISH_CHANNEL;
                } else {
                    videoSourceType = ZegoVideoSourceType.CUSTOM;
                }
                auxVideoPreviousPosition = position;

                if (!isAuxPublish) {
                    auxCurrentVideoSourceType = videoSourceType;
                    if (engine != null) {
                        if (auxCurrentVideoSourceType == ZegoVideoSourceType.PLAYER) {
                            if (mediaPlayer == null) {
                                initMediaPlayer();
                            }

                            mediaPlayer.setPlayerCanvas(auxPreviewCanvas);
                            engine.setVideoSource(auxCurrentVideoSourceType, mediaPlayer.getIndex(), ZegoPublishChannel.AUX);
                        } else {
                            engine.setVideoSource(auxCurrentVideoSourceType, ZegoPublishChannel.AUX);
                        }
                    }
                } else if (videoSourceType != auxCurrentVideoSourceType) {
                    auxSwitchVideoSource(videoSourceType);
                } else if (videoSourceType == ZegoVideoSourceType.PLAYER) {
                    loadMediaPlayerResource();
                } else if (videoSourceType == ZegoVideoSourceType.CUSTOM) {
                    if (auxVideoCustomCapturer != null) {
                        auxVideoCustomCapturer.onStop(ZegoPublishChannel.AUX);
                    }
                    auxVideoCustomCapturer = null;

                    engine.stopPreview(ZegoPublishChannel.AUX);

                    auxPreviewViewLayout.removeView(auxPreviewView);
                    auxPreviewViewLayout.addView(auxPreviewView);

                    boolean isCamera = true;
                    ZegoVideoBufferType bufferType = ZegoVideoBufferType.RAW_DATA;
                    if (TextUtils.equals(itemText, getString(R.string.video_source_custom_capture_gl_texture_2d))) {
                        bufferType = ZegoVideoBufferType.GL_TEXTURE_2D;
                    } else if (TextUtils.equals(itemText, getString(R.string.video_source_custom_capture_encoded_data))) {
                        bufferType = ZegoVideoBufferType.ENCODED_DATA;
                    } else if (TextUtils.equals(itemText, getString(R.string.video_source_custom_capture_image_raw_data))) {
                        isCamera = false;
                    } else if (TextUtils.equals(itemText, getString(R.string.video_source_custom_capture_image_gl_texture_2d))) {
                        isCamera = false;
                        bufferType = ZegoVideoBufferType.GL_TEXTURE_2D;
                    } else if (TextUtils.equals(itemText, getString(R.string.video_source_custom_capture_image_encoded_data))) {
                        isCamera = false;
                        bufferType = ZegoVideoBufferType.ENCODED_DATA;
                    }
                    auxVideoCustomCapturer = initCustomVideoCapturer(isCamera, auxPreviewView, bufferType, ZegoPublishChannel.AUX);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mainAudioSourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                String itemText = (String) adapterView.getItemAtPosition(position);

                if (isMainPublish && isAuxPublish &&
                        TextUtils.equals(getString(R.string.audio_source_screen_capture), itemText) &&
                        auxCurrentAudioSourceType == ZegoAudioSourceType.SCREEN_CAPTURE) {
                    AppLogger.getInstance().callApi("Switch main channel to screen capture source failed, screen capture source already used in aux channel!");
                    Toast.makeText(MultiVideoSourceActivity.this, "Switch main channel to screen capture source failed, screen capture source already used in aux channel!", Toast.LENGTH_LONG).show();

                    auxAudioSourceSpinner.setSelection(auxAudioPreviousPosition);

                    return;
                }

                ZegoAudioSourceType audioSourceType;
                if (TextUtils.equals(getString(R.string.audio_source_none), itemText)) {
                    audioSourceType = ZegoAudioSourceType.NONE;
                } else if (TextUtils.equals(getString(R.string.audio_source_microphone), itemText)) {
                    audioSourceType = ZegoAudioSourceType.MICROPHONE;
                } else if (TextUtils.equals(getString(R.string.audio_source_screen_capture), itemText)) {
                    audioSourceType = ZegoAudioSourceType.SCREEN_CAPTURE;
                } else {
                    audioSourceType = ZegoAudioSourceType.CUSTOM;
                }

                if (!isMainPublish) {
                    mainCurrentAudioSourceType = audioSourceType;
                    if (engine != null) {
                        engine.setAudioSource(mainCurrentAudioSourceType);
                    }
                } else if (audioSourceType != mainCurrentAudioSourceType) {
                    mainSwitchAudioSource(audioSourceType);
                } else if (audioSourceType == ZegoAudioSourceType.CUSTOM) {
                    if (mainAudioCustomCapturer != null) {
                        mainAudioCustomCapturer.stop();
                    }
                    mainAudioCustomCapturer = null;

                    boolean useDevice = true;
                    int audioFrameType = 0;
                    if (TextUtils.equals(getString(R.string.audio_source_custom_capture_file_pcm), itemText)) {
                        useDevice = false;
                    } else if (TextUtils.equals(getString(R.string.audio_source_custom_capture_file_aac), itemText)) {
                        useDevice = false;
                        audioFrameType = 1;
                    }
                    mainAudioCustomCapturer = initAudioCustomCapturer(ZegoPublishChannel.MAIN, useDevice, audioFrameType);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        auxAudioSourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                String itemText = (String) adapterView.getItemAtPosition(position);

                if (isMainPublish && isAuxPublish &&
                        TextUtils.equals(getString(R.string.audio_source_main_publish_channel), itemText) &&
                        mainCurrentAudioSourceType != ZegoAudioSourceType.MICROPHONE) {
                    AppLogger.getInstance().callApi("main channel must use microphone when switch aux channel to main publish channel!");
                    Toast.makeText(MultiVideoSourceActivity.this, "main channel must use microphone when switch aux channel to main publish channel!", Toast.LENGTH_LONG).show();

                    auxAudioSourceSpinner.setSelection(auxAudioPreviousPosition);

                    return;
                }

                if (isMainPublish && isAuxPublish &&
                        TextUtils.equals(getString(R.string.audio_source_screen_capture), itemText) &&
                        mainCurrentAudioSourceType == ZegoAudioSourceType.SCREEN_CAPTURE) {
                    AppLogger.getInstance().callApi("Switch aux channel to screen capture source failed, screen capture source already used in main channel!");
                    Toast.makeText(MultiVideoSourceActivity.this, "Switch aux channel to screen capture source failed, screen capture source already used in main channel!", Toast.LENGTH_LONG).show();

                    auxAudioSourceSpinner.setSelection(auxAudioPreviousPosition);

                    return;
                }

                auxAudioPreviousPosition = position;

                ZegoAudioSourceType audioSourceType = ZegoAudioSourceType.CUSTOM;
                if (TextUtils.equals(getString(R.string.audio_source_none), itemText)) {
                    audioSourceType = ZegoAudioSourceType.NONE;
                } else if (TextUtils.equals(getString(R.string.audio_source_media_player), itemText)) {
                    audioSourceType = ZegoAudioSourceType.MEDIA_PLAYER;
                } else if (TextUtils.equals(getString(R.string.audio_source_main_publish_channel), itemText)) {
                    audioSourceType = ZegoAudioSourceType.MAIN_PUBLISH_CHANNEL;
                } else if (TextUtils.equals(getString(R.string.audio_source_screen_capture), itemText)) {
                    audioSourceType = ZegoAudioSourceType.SCREEN_CAPTURE;
                }

                if (!isAuxPublish) {
                    auxCurrentAudioSourceType = audioSourceType;
                    if (engine != null) {
                        if (auxCurrentAudioSourceType == ZegoAudioSourceType.MEDIA_PLAYER) {
                            if (mediaPlayer == null) {
                                initMediaPlayer();
                            }
                        }

                        engine.setAudioSource(auxCurrentAudioSourceType, ZegoPublishChannel.AUX);
                    }
                } else if (audioSourceType != auxCurrentAudioSourceType) {
                    auxSwitchAudioSource(audioSourceType);
                } else if (audioSourceType == ZegoAudioSourceType.CUSTOM) {
                    if (auxAudioCustomCapturer != null) {
                        auxAudioCustomCapturer.stop();
                    }
                    auxAudioCustomCapturer = null;

                    boolean useDevice = true;
                    int audioFrameType = 0;
                    if (TextUtils.equals(getString(R.string.audio_source_custom_capture_file_pcm), itemText)) {
                        useDevice = false;
                    } else if (TextUtils.equals(getString(R.string.audio_source_custom_capture_file_aac), itemText)) {
                        useDevice = false;
                        audioFrameType = 1;
                    }
                    auxAudioCustomCapturer = initAudioCustomCapturer(ZegoPublishChannel.AUX, useDevice, audioFrameType);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    public void setEventHandler() {
        engine.setEventHandler(new IZegoEventHandler() {
            // The callback triggered when the room connection state changes.
            @Override
            public void onRoomStateChanged(String roomID, ZegoRoomStateChangedReason reason, int errorCode, JSONObject extendedData) {
                ZegoViewUtil.UpdateRoomState(roomState, reason);
            }

            @Override
            public void onPublisherStateUpdate(String s, ZegoPublisherState state, int errorCode, JSONObject jsonObject) {
                super.onPublisherStateUpdate(s, state, errorCode, jsonObject);

                if (errorCode == 0 && state.equals(ZegoPublisherState.NO_PUBLISH)) {
                    if (isMainPublish && TextUtils.equals(s, mainPublishStreamID)) {
                        mainPublishButton.setText(ZegoViewUtil.GetEmojiStringByUnicode(ZegoViewUtil.crossEmoji) + getString(R.string.stop_publishing));
                    }

                    if (isAuxPublish && TextUtils.equals(s, auxPublishStreamID)) {
                        auxPublishButton.setText(ZegoViewUtil.GetEmojiStringByUnicode(ZegoViewUtil.crossEmoji) + getString(R.string.stop_publishing));
                    }
                } else {
                    if (isMainPublish && TextUtils.equals(s, mainPublishStreamID)) {
                        mainPublishButton.setText(ZegoViewUtil.GetEmojiStringByUnicode(ZegoViewUtil.checkEmoji) + getString(R.string.stop_publishing));
                    }

                    if (isAuxPublish && TextUtils.equals(s, auxPublishStreamID)) {
                        auxPublishButton.setText(ZegoViewUtil.GetEmojiStringByUnicode(ZegoViewUtil.checkEmoji) + getString(R.string.stop_publishing));
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
                    if (isMainPlay && streamID.contentEquals(mainPlayStreamID)) {
                        mainPlayButton.setText(ZegoViewUtil.GetEmojiStringByUnicode(ZegoViewUtil.crossEmoji) + getString(R.string.stop_playing));
                    }

                    if (isAuxPlay && streamID.contentEquals(auxPlayStreamID)) {
                        auxPlayButton.setText(ZegoViewUtil.GetEmojiStringByUnicode(ZegoViewUtil.crossEmoji) + getString(R.string.stop_playing));
                    }
                } else {
                    if (isMainPlay && streamID.contentEquals(mainPlayStreamID)) {
                        mainPlayButton.setText(ZegoViewUtil.GetEmojiStringByUnicode(ZegoViewUtil.checkEmoji) + getString(R.string.stop_playing));
                    }

                    if (isAuxPlay && streamID.contentEquals(auxPlayStreamID)) {
                        auxPlayButton.setText(ZegoViewUtil.GetEmojiStringByUnicode(ZegoViewUtil.checkEmoji) + getString(R.string.stop_playing));
                    }
                }
            }

        });
    }

    public void setApiCalledResult() {
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
    public void setLogComponent() {
        logLinearLayout logHiddenView = findViewById(R.id.logView);
        logHiddenView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogView logview = new LogView(getApplicationContext());
                logview.show(getSupportFragmentManager(),null);
            }
        });
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
        if (isLoginRoom) {
            ZegoExpressEngine.getEngine().logoutRoom();
        }
        ZegoExpressEngine.destroyEngine(null);
        super.onDestroy();
    }

    void mainSwitchVideoSource(ZegoVideoSourceType videoSourceType) {
        switch (mainCurrentVideoSourceType) {
            case SCREEN_CAPTURE:
                engine.stopScreenCapture();

                break;

            case CUSTOM:
                ZegoCustomVideoCaptureConfig captureConfig = new ZegoCustomVideoCaptureConfig();
                engine.enableCustomVideoCapture(false, captureConfig, ZegoPublishChannel.MAIN);
                if (mainVideoCustomCapturer != null) {
                    mainVideoCustomCapturer.onStop(ZegoPublishChannel.MAIN);
                }
                mainVideoCustomCapturer = null;

                mainPreviewViewLayout.removeView(mainPreviewView);
                mainPreviewViewLayout.addView(mainPreviewView);

                break;
        }

        if (videoSourceType != ZegoVideoSourceType.CUSTOM) {
            engine.startPreview(mainPreviewCanvas);
        }
        mainCurrentVideoSourceType = videoSourceType;
        engine.setVideoSource(mainCurrentVideoSourceType);

        switch (mainCurrentVideoSourceType) {
            case SCREEN_CAPTURE:
                engine.startScreenCapture(mainScreenCaptureConfig);

                break;

            case CUSTOM:
                engine.stopPreview();

                mainPreviewViewLayout.removeView(mainPreviewView);
                mainPreviewViewLayout.addView(mainPreviewView);

                boolean isCamera = true;
                String itemText = (String) mainVideoSourceSpinner.getSelectedItem();
                ZegoVideoBufferType bufferType = ZegoVideoBufferType.RAW_DATA;
                if (TextUtils.equals(itemText, getString(R.string.video_source_custom_capture_gl_texture_2d))) {
                    bufferType = ZegoVideoBufferType.GL_TEXTURE_2D;
                } else if (TextUtils.equals(itemText, getString(R.string.video_source_custom_capture_encoded_data))) {
                    bufferType = ZegoVideoBufferType.ENCODED_DATA;
                } else if (TextUtils.equals(itemText, getString(R.string.video_source_custom_capture_image_raw_data))) {
                    isCamera = false;
                } else if (TextUtils.equals(itemText, getString(R.string.video_source_custom_capture_image_gl_texture_2d))) {
                    isCamera = false;
                    bufferType = ZegoVideoBufferType.GL_TEXTURE_2D;
                } else if (TextUtils.equals(itemText, getString(R.string.video_source_custom_capture_image_encoded_data))) {
                    isCamera = false;
                    bufferType = ZegoVideoBufferType.ENCODED_DATA;
                }
                mainVideoCustomCapturer = initCustomVideoCapturer(isCamera, mainPreviewView, bufferType, ZegoPublishChannel.MAIN);

                break;
        }
    }

    void mainSwitchAudioSource(ZegoAudioSourceType audioSourceType) {
        switch (mainCurrentAudioSourceType) {
            case CUSTOM:
                if (mainAudioCustomCapturer != null) {
                    mainAudioCustomCapturer.stop();
                }
                mainAudioCustomCapturer = null;

                break;
            case SCREEN_CAPTURE:

                break;
        }

        mainCurrentAudioSourceType = audioSourceType;
        engine.setAudioSource(mainCurrentAudioSourceType);

        String itemText = (String) mainAudioSourceSpinner.getSelectedItem();
        switch (mainCurrentAudioSourceType) {
            case CUSTOM:
                boolean useDevice = true;
                int audioFrameType = 0;
                if (TextUtils.equals(getString(R.string.audio_source_custom_capture_file_pcm), itemText)) {
                    useDevice = false;
                } else if (TextUtils.equals(getString(R.string.audio_source_custom_capture_file_aac), itemText)) {
                    useDevice = false;
                    audioFrameType = 1;
                }
                mainAudioCustomCapturer = initAudioCustomCapturer(ZegoPublishChannel.MAIN, useDevice, audioFrameType);

                break;
            case SCREEN_CAPTURE:

                break;
        }
    }

    void auxSwitchVideoSource(ZegoVideoSourceType videoSourceType) {
        switch (auxCurrentVideoSourceType) {
            case SCREEN_CAPTURE:
                engine.stopScreenCapture();

                break;

            case PLAYER:
                if (mediaPlayer != null) {
                    if (auxCurrentAudioSourceType != ZegoAudioSourceType.MEDIA_PLAYER) {
                        mediaPlayer.pause();
                    }

                    mediaPlayer.setPlayerCanvas(null);
                }

                break;

            case CUSTOM:
                ZegoCustomVideoCaptureConfig captureConfig = new ZegoCustomVideoCaptureConfig();
                engine.enableCustomVideoCapture(false, captureConfig, ZegoPublishChannel.AUX);
                if (auxVideoCustomCapturer != null) {
                    auxVideoCustomCapturer.onStop(ZegoPublishChannel.AUX);
                }
                auxVideoCustomCapturer = null;

                auxPreviewViewLayout.removeView(auxPreviewView);
                auxPreviewViewLayout.addView(auxPreviewView);

                break;
        }

        if (videoSourceType != ZegoVideoSourceType.CUSTOM) {
            engine.startPreview(auxPreviewCanvas, ZegoPublishChannel.AUX);
        }

        auxCurrentVideoSourceType = videoSourceType;
        if (auxCurrentVideoSourceType == ZegoVideoSourceType.PLAYER) {
            if (mediaPlayer == null) {
                initMediaPlayer();
            }
            engine.setVideoSource(auxCurrentVideoSourceType, mediaPlayer.getIndex(), ZegoPublishChannel.AUX);
        } else {
            engine.setVideoSource(auxCurrentVideoSourceType, ZegoPublishChannel.AUX);
        }

        switch (auxCurrentVideoSourceType) {
            case SCREEN_CAPTURE:
                engine.startScreenCapture(auxScreenCaptureConfig);

                break;

            case PLAYER:
                loadMediaPlayerResource();

                mediaPlayer.setPlayerCanvas(auxPreviewCanvas);

                break;

            case CUSTOM:
                engine.stopPreview(ZegoPublishChannel.AUX);

                auxPreviewViewLayout.removeView(auxPreviewView);
                auxPreviewViewLayout.addView(auxPreviewView);

                boolean isCamera = true;
                String itemText = (String) auxVideoSourceSpinner.getSelectedItem();
                ZegoVideoBufferType bufferType = ZegoVideoBufferType.RAW_DATA;
                if (TextUtils.equals(itemText, getString(R.string.video_source_custom_capture_gl_texture_2d))) {
                    bufferType = ZegoVideoBufferType.GL_TEXTURE_2D;
                } else if (TextUtils.equals(itemText, getString(R.string.video_source_custom_capture_encoded_data))) {
                    bufferType = ZegoVideoBufferType.ENCODED_DATA;
                } else if (TextUtils.equals(itemText, getString(R.string.video_source_custom_capture_image_raw_data))) {
                    isCamera = false;
                } else if (TextUtils.equals(itemText, getString(R.string.video_source_custom_capture_image_gl_texture_2d))) {
                    isCamera = false;
                    bufferType = ZegoVideoBufferType.GL_TEXTURE_2D;
                } else if (TextUtils.equals(itemText, getString(R.string.video_source_custom_capture_image_encoded_data))) {
                    isCamera = false;
                    bufferType = ZegoVideoBufferType.ENCODED_DATA;
                }
                auxVideoCustomCapturer = initCustomVideoCapturer(isCamera, auxPreviewView, bufferType, ZegoPublishChannel.AUX);

                break;
        }
    }

    void auxSwitchAudioSource(ZegoAudioSourceType audioSourceType) {
        switch (auxCurrentAudioSourceType) {
            case MEDIA_PLAYER:
                if (mediaPlayer != null && auxCurrentVideoSourceType != ZegoVideoSourceType.PLAYER) {
                    mediaPlayer.pause();
                }

                break;

            case CUSTOM:
                if (auxAudioCustomCapturer != null) {
                    auxAudioCustomCapturer.stop();
                }
                auxAudioCustomCapturer = null;

                break;
            case SCREEN_CAPTURE:

                break;
        }

        auxCurrentAudioSourceType = audioSourceType;
        engine.setAudioSource(auxCurrentAudioSourceType, ZegoPublishChannel.AUX);

        switch (auxCurrentAudioSourceType) {
            case MEDIA_PLAYER:
                if (mediaPlayer == null) {
                    initMediaPlayer();
                }

                loadMediaPlayerResource();

                break;

            case CUSTOM:
                String itemText = (String) auxAudioSourceSpinner.getSelectedItem();
                boolean useDevice = true;
                int audioFrameType = 0;
                if (TextUtils.equals(getString(R.string.audio_source_custom_capture_file_pcm), itemText)) {
                    useDevice = false;
                } else if (TextUtils.equals(getString(R.string.audio_source_custom_capture_file_aac), itemText)) {
                    useDevice = false;
                    audioFrameType = 1;
                }
                auxAudioCustomCapturer = initAudioCustomCapturer(ZegoPublishChannel.AUX, useDevice, audioFrameType);

                break;

            case SCREEN_CAPTURE:

                break;
        }
    }

    void initMediaPlayer() {
        mediaPlayer = engine.createMediaPlayer();
        mediaPlayer.enableRepeat(true);
    }

    void loadMediaPlayerResource() {
        mediaPlayer.stop();

        // 更新媒体播放器资源
        String resPath = getExternalFilesDir("").getPath()+"/ad.mp4";
        if (TextUtils.equals((String) auxVideoSourceSpinner.getItemAtPosition(auxVideoPreviousPosition), getString(R.string.video_source_media_player_using_network_resource))) {
            resPath = "https://storage.zego.im/demo/201808270915.mp4";
        }
        mediaPlayer.loadResource(resPath, new IZegoMediaPlayerLoadResourceCallback() {
            @Override
            public void onLoadResourceCallback(int errorCode) {
                if (errorCode == 0){
                    AppLogger.getInstance().receiveCallback("Load resource successfully!");
                } else {
                    AppLogger.getInstance().fail("[%d] Fail to load resource...",errorCode);
                }
            }
        });

        if (mediaPlayer.getCurrentState() == ZegoMediaPlayerState.PAUSING) {
            mediaPlayer.resume();
        } else if (mediaPlayer.getCurrentState() != ZegoMediaPlayerState.PLAYING) {
            mediaPlayer.start();
        }
    }

    ZegoVideoCaptureCallback initCustomVideoCapturer(boolean isCamera, View previewView, ZegoVideoBufferType bufferType, ZegoPublishChannel channel) {
        ZegoVideoCaptureCallback customCapturer = null;

        ZegoCustomVideoCaptureConfig captureConfig = new ZegoCustomVideoCaptureConfig();
        captureConfig.bufferType = bufferType;
        engine.enableCustomVideoCapture(true, captureConfig, channel);
        if (isCamera) {
            if (bufferType == ZegoVideoBufferType.RAW_DATA) {
                customCapturer = new VideoCaptureFromCamera();
            } else if (bufferType == ZegoVideoBufferType.GL_TEXTURE_2D) {
                customCapturer = new VideoCaptureFromCamera2();
            } else if (bufferType == ZegoVideoBufferType.ENCODED_DATA) {
                customCapturer = new VideoCaptureFromCamera3(MultiVideoSourceActivity.this);
            }
        } else {
            if (bufferType == ZegoVideoBufferType.RAW_DATA) {
                customCapturer = new VideoCaptureFromImage(getApplicationContext(), engine);
            } else if (bufferType == ZegoVideoBufferType.GL_TEXTURE_2D) {
                customCapturer = new VideoCaptureFromImage2(getApplicationContext(), engine);
            } else if (bufferType == ZegoVideoBufferType.ENCODED_DATA) {
                customCapturer = new VideoCaptureFromImage3(getApplicationContext(), engine);
            }
        }

        customCapturer.setView(previewView);
        customCapturer.onStart(channel);

        return customCapturer;
    }

    AudioCustomCapturer initAudioCustomCapturer(ZegoPublishChannel channel, boolean useDevice, int audioFrameType) {
        AudioCustomCapturer customCapturer = new AudioCustomCapturer(getApplicationContext(), engine, channel, useDevice, audioFrameType);
        customCapturer.start();

        return customCapturer;
    }

    boolean isMainUseCamera() {
        boolean ret = true;

        do {
            if (mainCurrentVideoSourceType == ZegoVideoSourceType.CAMERA) {
                break;
            }

            if (mainCurrentVideoSourceType == ZegoVideoSourceType.CUSTOM) {
                String itemText = (String) mainVideoSourceSpinner.getSelectedItem();

                if (TextUtils.equals(getString(R.string.video_source_custom_capture_raw_data), itemText)) {
                    break;
                }

                if (TextUtils.equals(getString(R.string.video_source_custom_capture_gl_texture_2d), itemText)) {
                    break;
                }

                if (TextUtils.equals(getString(R.string.video_source_custom_capture_encoded_data), itemText)) {
                    break;
                }
            }

            ret = false;
        } while (false);

        return ret;
    }

    boolean isAuxUseCamera() {
        boolean ret = true;

        do {
            if (auxCurrentVideoSourceType == ZegoVideoSourceType.CAMERA) {
                break;
            }

            if (auxCurrentVideoSourceType == ZegoVideoSourceType.CUSTOM) {
                String itemText = (String) auxVideoSourceSpinner.getSelectedItem();

                if (TextUtils.equals(getString(R.string.video_source_custom_capture_raw_data), itemText)) {
                    break;
                }

                if (TextUtils.equals(getString(R.string.video_source_custom_capture_gl_texture_2d), itemText)) {
                    break;
                }

                if (TextUtils.equals(getString(R.string.video_source_custom_capture_encoded_data), itemText)) {
                    break;
                }
            }

            ret = false;
        } while (false);

        return ret;
    }

    void logoutRoom() {
        if (isMainPlay) {
            stopMainPlay();
        }

        if (isMainPublish) {
            stopMainPublish();
        }

        if (isAuxPlay) {
            stopAuxPlay();
        }

        if (isAuxPublish) {
            stopAuxPublish();
        }

        // logout room
        AppLogger.getInstance().callApi("LogoutRoom: %s", roomID);
        engine.logoutRoom(roomID);

        isLoginRoom = false;
        loginRoomButton.setText(getString(R.string.login_room));
        ZegoViewUtil.UpdateRoomState(roomState, ZegoRoomStateChangedReason.LOGOUT);
    }

    void stopMainPlay() {
        AppLogger.getInstance().callApi("Stop Playing Stream:%s", mainPlayStreamID);

        engine.stopPlayingStream(mainPlayStreamID);

        mainPlayButton.setText(getString(R.string.start_playing));

        isMainPlay = false;
    }

    void stopMainPublish() {
        AppLogger.getInstance().callApi("Stop Publishing main channel: %s", mainPublishStreamID);

        if (mainVideoCustomCapturer != null) {
            mainVideoCustomCapturer.onStop(ZegoPublishChannel.MAIN);
            mainVideoCustomCapturer = null;
        }
        mainPreviewViewLayout.removeView(mainPreviewView);
        mainPreviewViewLayout.addView(mainPreviewView);

        if (mainAudioCustomCapturer != null) {
            mainAudioCustomCapturer.stop();
            mainAudioCustomCapturer = null;
        }

        if (mainCurrentVideoSourceType == ZegoVideoSourceType.SCREEN_CAPTURE) {
            engine.stopScreenCapture();
        }

        engine.stopPreview();
        engine.stopPublishingStream();

        mainPublishButton.setText(getString(R.string.start_publishing));

        isMainPublish = false;
    }

    void stopAuxPlay() {
        AppLogger.getInstance().callApi("Stop Playing Stream:%s", auxPlayStreamID);

        engine.stopPlayingStream(auxPlayStreamID);

        auxPlayButton.setText(getString(R.string.start_playing));

        isAuxPlay = false;
    }

    void stopAuxPublish() {
        AppLogger.getInstance().callApi("Stop Publishing aux channel: %s", auxPublishStreamID);

        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }

        if (auxVideoCustomCapturer != null) {
            auxVideoCustomCapturer.onStop(ZegoPublishChannel.AUX);
            auxVideoCustomCapturer = null;
        }
        auxPreviewViewLayout.removeView(auxPreviewView);
        auxPreviewViewLayout.addView(auxPreviewView);

        if (auxAudioCustomCapturer != null) {
            auxAudioCustomCapturer.stop();
            auxAudioCustomCapturer = null;
        }

        if (auxCurrentVideoSourceType == ZegoVideoSourceType.SCREEN_CAPTURE) {
            engine.stopScreenCapture();
        }

        engine.stopPreview(ZegoPublishChannel.AUX);
        engine.stopPublishingStream(ZegoPublishChannel.AUX);

        auxPublishButton.setText(getString(R.string.start_publishing));

        isAuxPublish = false;
    }
}