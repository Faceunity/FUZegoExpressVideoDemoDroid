package im.zego.videocapture.ui;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.faceunity.core.entity.FUCameraConfig;
import com.faceunity.core.entity.FURenderFrameData;
import com.faceunity.core.entity.FURenderInputData;
import com.faceunity.core.entity.FURenderOutputData;
import com.faceunity.core.faceunity.FURenderKit;
import com.faceunity.core.listener.OnGlRendererListener;
import com.faceunity.core.renderer.CameraRenderer;
import com.faceunity.nama.FURenderer;
import com.faceunity.nama.data.FaceUnityDataFactory;
import com.faceunity.nama.ui.FaceUnityView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.File;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import im.zego.common.profile.CSVUtils;
import im.zego.common.profile.Constant;
import im.zego.common.util.AppLogger;
import im.zego.common.util.DeviceInfoManager;
import im.zego.common.util.SettingDataUtil;
import im.zego.videocapture.R;
import im.zego.videocapture.enums.CaptureOrigin;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.constants.ZegoPlayerMediaEvent;
import im.zego.zegoexpress.constants.ZegoPublishChannel;
import im.zego.zegoexpress.constants.ZegoPublisherState;
import im.zego.zegoexpress.constants.ZegoRoomState;
import im.zego.zegoexpress.constants.ZegoVideoBufferType;
import im.zego.zegoexpress.constants.ZegoVideoFrameFormat;
import im.zego.zegoexpress.constants.ZegoVideoMirrorMode;
import im.zego.zegoexpress.constants.ZegoViewMode;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoCustomVideoCaptureConfig;
import im.zego.zegoexpress.entity.ZegoRoomConfig;
import im.zego.zegoexpress.entity.ZegoUser;
import im.zego.zegoexpress.entity.ZegoVideoConfig;
import im.zego.zegoexpress.entity.ZegoVideoFrameParam;

/**
 * FaceUnity 接入 activity,采用自定义本地采集和渲染
 */
public class FuCaptureRenderActivity extends AppCompatActivity implements OnGlRendererListener {
    private static final String TAG = "FuCaptureRenderActivity";

    private GLSurfaceView mPreView;
    private TextureView mPlayView;
    private TextView mErrorTxt;
    private Button mDealBtn;
    private Button mDealPlayBtn;
    private String mRoomID = "zgvc_";
    private String mPlayStreamID = "";
    private ZegoExpressEngine mSDKEngine;
    private String userID;
    private String userName;
    private CameraRenderer mCameraRenderer;
    private FaceUnityDataFactory mFaceUnityDataFactory;
    private FUCameraConfig fuCameraConfig;
    private ByteBuffer byteBuffer;
    // Whether the collection source is screen recording
    private int captureOrigin = 0;
    private static final int DEFAULT_VIDEO_WIDTH = 360;
    private static final int DEFAULT_VIDEO_HEIGHT = 640;
    private ZegoVideoBufferType videoBufferType;
    private ZegoCustomVideoCaptureConfig videoCaptureConfig;
    private FaceUnityView faceUnityView;
    private CSVUtils mCSVUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_fu_capture_render);

        mPreView = findViewById(R.id.pre_view);
        mPlayView = findViewById(R.id.play_view);
        mErrorTxt = findViewById(R.id.error_txt);
        mDealBtn = findViewById(R.id.publish_btn);
        mDealPlayBtn = findViewById(R.id.play_btn);
        mRoomID += String.valueOf((int) (Math.random() * 1000));
        // 获取设备唯一ID
        String deviceID = DeviceInfoManager.generateDeviceId(this);
        mRoomID += deviceID;
        mPlayStreamID = mRoomID;

        videoBufferType = ZegoVideoBufferType.getZegoVideoBufferType(getIntent().getIntExtra("ZegoVideoBufferType", 0));
        // 采集源是否是录屏
        // Whether the collection source is screen recording
        captureOrigin = getIntent().getIntExtra("captureOrigin", 0);

        initCsvUtil(this);
        // Initialize SDK login room
        initSDK();
        // start publish stream
        doPublish();
    }

    private void initSDK() {
        // 创建sdk
        AppLogger.getInstance().i(getString(R.string.create_zego_engine));
        mSDKEngine = ZegoExpressEngine.createEngine(SettingDataUtil.getAppId(), SettingDataUtil.getAppKey(), SettingDataUtil.getEnv(), SettingDataUtil.getScenario(), this.getApplication(), null);
        mSDKEngine.setEventHandler(zegoEventHandler);
        videoCaptureConfig = new ZegoCustomVideoCaptureConfig();
        videoCaptureConfig.bufferType = videoBufferType;
        mSDKEngine.enableCustomVideoCapture(true, videoCaptureConfig, ZegoPublishChannel.MAIN);
        ZegoRoomConfig config = new ZegoRoomConfig();
        /* 使能用户登录/登出房间通知 */
        /* Enable notification when user login or logout */
        config.isUserStatusNotify = true;
        String randomSuffix = String.valueOf(new Date().getTime() % (new Date().getTime() / 1000));
        userID = "user" + randomSuffix;
        userName = "userName" + randomSuffix;
        mSDKEngine.loginRoom(mRoomID, new ZegoUser(userID, userName), config);

        fuCameraConfig = new FUCameraConfig();
        mCameraRenderer = new CameraRenderer(mPreView, fuCameraConfig, this);
        faceUnityView = findViewById(R.id.faceUnityView);
        mFaceUnityDataFactory = new FaceUnityDataFactory(0);
        faceUnityView.bindDataFactory(mFaceUnityDataFactory);
    }

    IZegoEventHandler zegoEventHandler = new IZegoEventHandler() {
        @Override
        public void onRoomStateUpdate(String roomID, ZegoRoomState state, int errorCode, JSONObject extendedData) {
            /** 房间状态回调，在登录房间后，当房间状态发生变化（例如房间断开，认证失败等），SDK会通过该接口通知 */
            /** Room status update callback: after logging into the room, when the room connection status changes
             * (such as room disconnection, login authentication failure, etc.), the SDK will notify through the callback
             */
            AppLogger.getInstance().i("onRoomStateUpdate: roomID = " + roomID + ", state = " + state + ", errorCode = " + errorCode);
            if (state == ZegoRoomState.CONNECTED) {
                mErrorTxt.setText("");
            } else if (state == ZegoRoomState.DISCONNECTED) {
                mErrorTxt.setText("login room fail, err:" + errorCode);
            }
        }

        @Override
        public void onPublisherStateUpdate(String streamID, ZegoPublisherState state, int errorCode, JSONObject extendedData) {
            AppLogger.getInstance().i("onPublisherStateUpdate: streamID = " + streamID + ", state = " + state + ", errCode = " + errorCode);
            if (state == ZegoPublisherState.PUBLISH_REQUESTING) {
                mDealBtn.setText("StopPublish");
            }
        }

        @Override
        public void onPlayerMediaEvent(String streamID, ZegoPlayerMediaEvent event) {
            if (event == ZegoPlayerMediaEvent.VIDEO_BREAK_OCCUR) {
                runOnUiThread(() -> {
                    mErrorTxt.setText("play stream fail，err：" + event.value());
                });
            } else if (event == ZegoPlayerMediaEvent.VIDEO_BREAK_RESUME) {
                runOnUiThread(() -> {
                    mErrorTxt.setText("");
                    mDealPlayBtn.setText("StopPlay");
                });
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        mCameraRenderer.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraRenderer.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 登出房间并释放ZEGO SDK
        // Log out of the room and release the ZEGO SDK
        logoutLiveRoom();
        mCameraRenderer.onDestroy();
    }

    // 推流
    public void doPublish() {
        // 设置编码以及采集分辨率
        // Set encoding and acquisition resolution
        ZegoVideoConfig zegoVideoConfig = new ZegoVideoConfig();
        if (captureOrigin == CaptureOrigin.CaptureOrigin_MediaPlayer.getCode()) {//媒体播放文件分辨率为1920*1080，这里设置采集编码分辨率，避免被裁剪
            zegoVideoConfig.captureWidth = 1920;
            zegoVideoConfig.captureHeight = 1080;
            zegoVideoConfig.encodeWidth = 1920;
            zegoVideoConfig.encodeHeight = 1080;
        } else {
            zegoVideoConfig.setCaptureResolution(DEFAULT_VIDEO_WIDTH, DEFAULT_VIDEO_HEIGHT);
            zegoVideoConfig.setEncodeResolution(DEFAULT_VIDEO_WIDTH, DEFAULT_VIDEO_HEIGHT);
        }
        mSDKEngine.setVideoConfig(zegoVideoConfig);

//        ZegoCanvas zegoCanvas = new ZegoCanvas(null);
//        zegoCanvas.viewMode = ZegoViewMode.SCALE_TO_FILL;
        // 设置预览视图及视图展示模式
        // Set preview view and view display mode
//        mSDKEngine.startPreview(zegoCanvas);
        mSDKEngine.setVideoMirrorMode(ZegoVideoMirrorMode.ONLY_PUBLISH_MIRROR);
        mSDKEngine.startPublishingStream(mRoomID);
    }

    // 登出房间，去除推拉流回调监听并释放ZEGO SDK
    // Log out of the room, remove the push-pull flow callback monitoring and release the ZEGO SDK
    public void logoutLiveRoom() {
        mSDKEngine.logoutRoom(mRoomID);
        mSDKEngine.enableCustomVideoCapture(false, videoCaptureConfig, ZegoPublishChannel.MAIN);
        mSDKEngine.setEventHandler(null);
    }

    // 处理推流操作
    // Handling push operations
    public void DealPublishing(View view) {
        // 界面button==停止推流
        if (mDealBtn.getText().toString().equals("StopPublish")) {
            // 停止预览和推流
            mCameraRenderer.closeCamera();
            mSDKEngine.stopPreview();
            mSDKEngine.stopPublishingStream();
            mDealBtn.setText("StartPublish");
        } else {
            mDealBtn.setText("StopPublish");
            //camerarender fucamera -> 直接操作原生camera
            mCameraRenderer.closeCamera();
            mCameraRenderer.reopenCamera();
            // 界面button==开始推流
            doPublish();
        }
    }

    // 处理拉流操作
    public void DealPlay(View view) {
        // 界面button==开始拉流
        if (mDealPlayBtn.getText().toString().equals("StartPlay") && !mPlayStreamID.equals("")) {
            ZegoCanvas zegoCanvas = new ZegoCanvas(mPlayView);
            if (captureOrigin == CaptureOrigin.CaptureOrigin_MediaPlayer.getCode()) {
                zegoCanvas.viewMode = ZegoViewMode.ASPECT_FIT;
            } else {
                zegoCanvas.viewMode = ZegoViewMode.SCALE_TO_FILL;
            }
            // 开始拉流
            mSDKEngine.startPlayingStream(mPlayStreamID, zegoCanvas);
            mDealPlayBtn.setText("StopPlay");
            mErrorTxt.setText("");

        } else {
            // 界面button==停止拉流
            if (!mPlayStreamID.equals("")) {
                //停止拉流
                mSDKEngine.stopPlayingStream(mPlayStreamID);

                mDealPlayBtn.setText("StartPlay");

            }
        }
    }


    @Override
    public void onRenderAfter(@NotNull FURenderOutputData fuRenderOutputData, @NotNull FURenderFrameData fuRenderFrameData) {
        //这里进数据流推送
        if (mCSVUtils != null) {
            long renderTime = System.nanoTime() - start;
            mCSVUtils.writeCsv(null, renderTime);
        }

        if (fuRenderOutputData.getImage() != null && fuRenderOutputData.getImage().getBuffer() != null) {
            // 使用采集视频帧信息构造VideoCaptureFormat
            // Constructing VideoCaptureFormat using captured video frame information
            ZegoVideoFrameParam param = new ZegoVideoFrameParam();
            param.width = fuRenderOutputData.getImage().getWidth();
            param.height = fuRenderOutputData.getImage().getHeight();
            param.strides[0] = fuRenderOutputData.getImage().getWidth();
            param.strides[1] = fuRenderOutputData.getImage().getWidth();
            param.format = ZegoVideoFrameFormat.NV21;
            param.rotation = 180;

            long now; //部分机型存在 surfaceTexture 时间戳不准确的问题
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                now = SystemClock.elapsedRealtime();
            } else {
                now = TimeUnit.MILLISECONDS.toMillis(SystemClock.elapsedRealtime());
            }
            // 将采集的数据传给ZEGO SDKc
            // Pass the collected data to ZEGO SDK
            if (byteBuffer == null) {
                byteBuffer = ByteBuffer.allocateDirect(fuRenderOutputData.getImage().getBuffer().length);
            }
            byteBuffer.put(fuRenderOutputData.getImage().getBuffer());
            byteBuffer.flip();
            mSDKEngine.sendCustomVideoCaptureRawData(byteBuffer, byteBuffer.limit(), param, now);
        }
    }

    long start = 0;

    @Override
    public void onRenderBefore(@Nullable FURenderInputData fuRenderInputData) {
        if (mCSVUtils != null) {
            start = System.nanoTime();
        }
        if (fuRenderInputData != null && fuRenderInputData.getRenderConfig() != null)
            fuRenderInputData.getRenderConfig().setNeedBufferReturn(true);
    }

    @Override
    public void onDrawFrameAfter() {
    }

    @Override
    public void onSurfaceChanged(int i, int i1) {

    }

    @Override
    public void onSurfaceCreated() {
        mFaceUnityDataFactory.bindCurrentRenderer();
    }

    @Override
    public void onSurfaceDestroy() {
        FURenderKit.getInstance().release();
    }

    private static final int ENCODE_FRAME_WIDTH = 960;
    private static final int ENCODE_FRAME_HEIGHT = 540;
    private static final int ENCODE_FRAME_BITRATE = 1000;
    private static final int ENCODE_FRAME_FPS = 30;
    private static final int CAPTURE_WIDTH = 1280;
    private static final int CAPTURE_HEIGHT = 720;
    private static final int CAPTURE_FRAME_RATE = 30;

    //性能测试部分
    private void initCsvUtil(Context context) {
        mCSVUtils = new CSVUtils(context);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
        String dateStrDir = format.format(new Date(System.currentTimeMillis()));
        dateStrDir = dateStrDir.replaceAll("-", "").replaceAll("_", "");
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault());
        String dateStrFile = df.format(new Date());
        String filePath = Constant.filePath + dateStrDir + File.separator + "excel-" + dateStrFile + ".csv";
        Log.d(TAG, "initLog: CSV file path:" + filePath);
        StringBuilder headerInfo = new StringBuilder();
        headerInfo.append("version：").append(FURenderer.getInstance().getVersion()).append(CSVUtils.COMMA)
                .append("机型：").append(android.os.Build.MANUFACTURER).append(android.os.Build.MODEL).append(CSVUtils.COMMA)
                .append("处理方式：双输入纹理输出").append(CSVUtils.COMMA)
                .append("编码方式：硬件编码").append(CSVUtils.COMMA);
//                .append("编码分辨率：").append(ENCODE_FRAME_WIDTH).append("x").append(ENCODE_FRAME_HEIGHT).append(CSVUtils.COMMA)
//                .append("编码帧率：").append(ENCODE_FRAME_FPS).append(CSVUtils.COMMA)
//                .append("编码码率：").append(ENCODE_FRAME_BITRATE).append(CSVUtils.COMMA)
//                .append("预览分辨率：").append(CAPTURE_WIDTH).append("x").append(CAPTURE_HEIGHT).append(CSVUtils.COMMA)
//                .append("预览帧率：").append(CAPTURE_FRAME_RATE).append(CSVUtils.COMMA);
        mCSVUtils.initHeader(filePath, headerInfo);
    }
}
