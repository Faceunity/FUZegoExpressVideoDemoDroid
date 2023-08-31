package im.zego.advancedvideoprocessing.CustomerVideoCapture.faceunity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.faceunity.nama.FURenderer;
import com.faceunity.nama.data.FaceUnityDataFactory;
import com.faceunity.nama.ui.FaceUnityView;

import org.json.JSONObject;

import im.zego.advancedvideoprocessing.R;
import im.zego.commontools.logtools.AppLogger;
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
import im.zego.zegoexpress.constants.ZegoVideoBufferType;
import im.zego.zegoexpress.constants.ZegoVideoFrameFormatSeries;
import im.zego.zegoexpress.constants.ZegoVideoMirrorMode;
import im.zego.zegoexpress.constants.ZegoViewMode;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoCustomVideoRenderConfig;
import im.zego.zegoexpress.entity.ZegoEngineProfile;
import im.zego.zegoexpress.entity.ZegoUser;

/**
 * Faceunity自定义渲染
 */
public class FUVideoRenderPublish extends AppCompatActivity implements SensorEventListener {

    TextureView customPreview;
    TextureView customPlayView;
    Button startPublish;
    Button startPlay;
    ZegoExpressEngine engine;
    long appID;
    String userID;
    String appSign;
    String roomID;
    String playStreamID;
    String publishStreamID;

    boolean isPublish = true;
    boolean isPlay;

    private FURenderer mFURenderer;
    private SensorManager mSensorManager;
    private FaceUnityView faceUnityView;
    private FaceUnityDataFactory mFaceUnityDataFactory;
    FUVideoRenderHandler videoRenderer;
    ZegoCustomVideoRenderConfig renderConfig = new ZegoCustomVideoRenderConfig();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fu_video_render_publish);
        getAppIDAndUserIDAndAppSign();
        setRenderConfig();
        setUserConfig();
        setUI();
        initFu();
        setRender();
        initEngineAndLoginRoom();
        setEventHandler();
        setStartPublishButton();
        setStartPlayButton();
        startPublish();
        setApiCalledResult();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }

    //set render configuration
    public void setRenderConfig() {
//        if (!getIntent().getBooleanExtra("isRGB", false)) {
//            renderConfig.frameFormatSeries = ZegoVideoFrameFormatSeries.YUV;
//        } else {
//            renderConfig.frameFormatSeries = ZegoVideoFrameFormatSeries.RGB;
//        }
        renderConfig.frameFormatSeries = ZegoVideoFrameFormatSeries.RGB;
//        if (!getIntent().getBooleanExtra("isRawData", false)) {
//            renderConfig.bufferType = ZegoVideoBufferType.ENCODED_DATA;
//        } else {
//            renderConfig.bufferType = ZegoVideoBufferType.RAW_DATA;
//        }
        renderConfig.bufferType = ZegoVideoBufferType.RAW_DATA;
        renderConfig.enableEngineRender = true;
    }

    private void initFu() {
        faceUnityView = findViewById(R.id.faceUnityView);
        mFURenderer = FURenderer.getInstance();
        mFURenderer.prepareRenderer(null);
        mFaceUnityDataFactory = new FaceUnityDataFactory(-1);
        faceUnityView.bindDataFactory(mFaceUnityDataFactory);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFaceUnityDataFactory.bindCurrentRenderer();
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    //get appID and userID and appSign
    public void getAppIDAndUserIDAndAppSign() {
        appID = KeyCenter.getInstance().getAppID();
        userID = UserIDHelper.getInstance().getUserID();
        appSign = KeyCenter.getInstance().getAppSign();
    }

    //set user configuration
    public void setUserConfig() {
        playStreamID = getIntent().getStringExtra("playStreamID");
        publishStreamID = getIntent().getStringExtra("publishStreamID");
        roomID = getIntent().getStringExtra("roomID");
    }

    //set UI
    public void setUI() {
        customPreview = findViewById(R.id.pre_view);
        customPlayView = findViewById(R.id.play_view);
        startPublish = findViewById(R.id.publishButton);
        startPlay = findViewById(R.id.playButton);
        setTitle(getString(R.string.custom_video_rendering));
    }

    public void initEngineAndLoginRoom() {
        // Initialize the engine
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
        ZegoUser user = new ZegoUser(userID);

        // Set the engine
        engine.enableCustomVideoRender(true, renderConfig);
        engine.setCustomVideoRenderHandler(videoRenderer);
        engine.setVideoMirrorMode(ZegoVideoMirrorMode.BOTH_MIRROR);
        // Log in the room
        engine.loginRoom(roomID, user);
        AppLogger.getInstance().callApi("LoginRoom: %s", roomID);
        engine.enableCamera(true);// Enable the camera
        engine.muteMicrophone(false);// Enable the microphone
        engine.muteSpeaker(false);// Enable the speaker
    }

    public void setRender() {
        //Initialize the renderer
        videoRenderer = new FUVideoRenderHandler();
        videoRenderer.setRender(mFURenderer);
        videoRenderer.init();

        // get the view size and pass to renderer
        customPreview.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        videoRenderer.setSize(customPreview.getMeasuredWidth(), customPreview.getMeasuredHeight());

    }

    public void setStartPublishButton() {
        startPublish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPublish) {
                    startPublish();
                    AppLogger.getInstance().callApi("Start Publishing Stream:%s", publishStreamID);
                    startPublish.setText(getString(R.string.stop_publishing));
                    isPublish = true;
                } else {
                    stopPublish();
                    AppLogger.getInstance().callApi("Stop Publishing Stream:%s", publishStreamID);
                    startPublish.setText(getString(R.string.start_publishing));
                    isPublish = false;
                }
            }
        });
    }

    public void setStartPlayButton() {
        startPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPlay) {
                    startPlay();
                    AppLogger.getInstance().callApi("Start Playing Stream:%s", playStreamID);
                    startPlay.setText(getString(R.string.stop_playing));
                    isPlay = true;
                } else {
                    stopPlay();
                    AppLogger.getInstance().callApi("Stop Playing Stream:%s", playStreamID);
                    startPlay.setText(getString(R.string.start_playing));
                    isPlay = false;
                }
            }
        });
    }

    public void startPublish() {
        ZegoCanvas canvas = new ZegoCanvas(customPreview);
        canvas.viewMode = ZegoViewMode.SCALE_TO_FILL;
        if (renderConfig.bufferType == ZegoVideoBufferType.RAW_DATA) {
            engine.startPreview(null);
            videoRenderer.addCaptureView(ZegoPublishChannel.MAIN, customPreview);
        } else {
            engine.startPreview(canvas);
        }
        engine.startPublishingStream(publishStreamID);
    }

    public void stopPublish() {
        engine.stopPreview();
        engine.stopPublishingStream();
        if (renderConfig.bufferType == ZegoVideoBufferType.RAW_DATA) {
            videoRenderer.removeCaptureView(ZegoPublishChannel.MAIN);
        }
    }

    public void startPlay() {
        engine.startPlayingStream(playStreamID, new ZegoCanvas(null));
        if (renderConfig.bufferType == ZegoVideoBufferType.RAW_DATA) {
            videoRenderer.addView(playStreamID, customPlayView);
        } else {
            videoRenderer.addDecodView(customPlayView);
        }
    }

    public void stopPlay() {
        engine.stopPlayingStream(playStreamID);
        videoRenderer.removeView(playStreamID, false);
    }

    public void setEventHandler() {
        engine.setEventHandler(new IZegoEventHandler() {
            // The callback triggered when the room connection state changes.
            @Override
            public void onRoomStateChanged(String roomID, ZegoRoomStateChangedReason reason, int errorCode, JSONObject extendedData) {
                //ZegoViewUtil.UpdateRoomState(roomState, reason);
                super.onRoomStateChanged(roomID, reason, errorCode, extendedData);
            }

            // The callback triggered when the state of stream publishing changes.
            @Override
            public void onPublisherStateUpdate(String streamID, ZegoPublisherState state, int errorCode, JSONObject extendedData) {
                super.onPublisherStateUpdate(streamID, state, errorCode, extendedData);
                // If the state is PUBLISHER_STATE_NO_PUBLISH and the errcode is not 0, it means that stream publishing has failed
                // and no more retry will be attempted by the engine. At this point, the failure of stream publishing can be indicated
                // on the UI of the App.
            }

            // The callback triggered when the state of stream playing changes.
            @Override
            public void onPlayerStateUpdate(String streamID, ZegoPlayerState state, int errorCode, JSONObject extendedData) {
                super.onPlayerStateUpdate(streamID, state, errorCode, extendedData);
                // If the state is PLAYER_STATE_NO_PLAY and the errcode is not 0, it means that stream playing has failed and
                // no more retry will be attempted by the engine. At this point, the failure of stream playing can be indicated
                // on the UI of the App
            }

        });
    }

    public void setApiCalledResult() {
        // Update log with api called results
        ZegoExpressEngine.setApiCalledCallback(new IZegoApiCalledEventHandler() {
            @Override
            public void onApiCalledResult(int errorCode, String funcName, String info) {
                super.onApiCalledResult(errorCode, funcName, info);
                if (errorCode == 0) {
                    AppLogger.getInstance().success("[%s]:%s", funcName, info);
                } else {
                    AppLogger.getInstance().fail("[%d]%s:%s", errorCode, funcName, info);
                }
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release the rendering class
        engine.logoutRoom(roomID);
        ZegoExpressEngine.destroyEngine(null);
        videoRenderer.uninit();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && null != mFURenderer) {
            float x = event.values[0];
            float y = event.values[1];
            if (Math.abs(x) > 3 || Math.abs(y) > 3) {
                if (Math.abs(x) > Math.abs(y)) {
                    mFURenderer.setDeviceOrientation(x > 0 ? 0 : 180);
                } else {
                    mFURenderer.setDeviceOrientation(y > 0 ? 90 : 270);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}