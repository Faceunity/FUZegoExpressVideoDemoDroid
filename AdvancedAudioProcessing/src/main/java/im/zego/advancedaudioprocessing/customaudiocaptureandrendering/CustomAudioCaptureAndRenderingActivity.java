package im.zego.advancedaudioprocessing.customaudiocaptureandrendering;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import im.zego.advancedaudioprocessing.R;
import im.zego.commontools.logtools.AppLogger;
import im.zego.commontools.logtools.LogView;
import im.zego.commontools.logtools.logLinearLayout;
import im.zego.commontools.uitools.ZegoViewUtil;
import im.zego.keycenter.KeyCenter;
import im.zego.keycenter.UserIDHelper;
import im.zego.zegoexpress.ZegoAudioEffectPlayer;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoApiCalledEventHandler;
import im.zego.zegoexpress.callback.IZegoAudioEffectPlayerLoadResourceCallback;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.constants.ZegoAudioChannel;
import im.zego.zegoexpress.constants.ZegoAudioSampleRate;
import im.zego.zegoexpress.constants.ZegoAudioSourceType;
import im.zego.zegoexpress.constants.ZegoPlayerState;
import im.zego.zegoexpress.constants.ZegoPublishChannel;
import im.zego.zegoexpress.constants.ZegoPublisherState;
import im.zego.zegoexpress.constants.ZegoRoomState;
import im.zego.zegoexpress.constants.ZegoRoomStateChangedReason;
import im.zego.zegoexpress.constants.ZegoScenario;
import im.zego.zegoexpress.entity.ZegoAudioEffectPlayConfig;
import im.zego.zegoexpress.entity.ZegoAudioFrameParam;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoCustomAudioConfig;
import im.zego.zegoexpress.entity.ZegoEngineProfile;
import im.zego.zegoexpress.entity.ZegoRoomConfig;
import im.zego.zegoexpress.entity.ZegoUser;

import static im.zego.zegoexpress.constants.ZegoAudioSampleRate.ZEGO_AUDIO_SAMPLE_RATE_16K;
import static im.zego.zegoexpress.constants.ZegoAudioSampleRate.ZEGO_AUDIO_SAMPLE_RATE_44K;
import static im.zego.zegoexpress.constants.ZegoAudioSampleRate.getZegoAudioSampleRate;

public class CustomAudioCaptureAndRenderingActivity extends AppCompatActivity {

    TextView userIDText;
    TextView roomState;
    TextView roomIDText;
    Button startPublishingButton;
    Button startPlayingButton;
    TextureView preview;
    TextureView playView;


    Long appID;
    String userID;
    String appSign;
    String roomID;
    String publishStreamID;
    String playStreamID;
    ZegoExpressEngine engine;
    ZegoUser user;

    boolean isEnd = false;

    private Integer recordBufferSize;
    private int captureSampleRate = 44100;
    private int captureChannel = AudioFormat.CHANNEL_IN_MONO;
    ByteBuffer PcmBuffer;
    private AudioRecord audioRecord;
    private ZegoAudioFrameParam audioFrameParam = new ZegoAudioFrameParam();
    private ExecutorService singleThreadPool = Executors.newSingleThreadExecutor();
    ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    ZegoAudioEffectPlayer effectPlayer;
    final List<String> fileNames = new ArrayList<>();
    byte[] mediaByte;
    byte[] temp;
    ByteBuffer mediaBuffer;
    int position = 0;
    int audioIndex = 0;

    // Store whether the user is publishing the stream
    boolean isPublish = false;
    // Store whether the user is playing the stream
    boolean isPlay = false;
    // Store whether the microphone is enabled.
    boolean isMicrophone = false;
    // Store whether the custom capture is enabled.
    boolean enableCustomCapture = false;
    // Store whether capture media is enabled.
    boolean isCaptureMedia = false;

    private enum Status {
        STATUS_NO_READY,
        STATUS_READY,
        STATUS_START,
        STATUS_STOP
    }
    private Status status = Status.STATUS_NO_READY;
    float duration = 0.02f; // 20ms
    int sampleRate = 16000;
    int audioChannels = 1;
    int bytesPerSample = 2;

    Thread aacCaptureThread;
    String aacFileName = "BackgroundMusic22SoftPianoMusic.aac";
    byte[] aacDataBuffer;
    byte[] aacTempBuffer;
    int bufferSize;
    boolean sendAsc;
    InputStream aacData;
    byte[] aacConfigBuffer;
    byte[] aacConfigBufferNew;
    int customAudioFrameType = 0;
    boolean exitAacThread;
    int aacSamplerates[] = {96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050, 16000, 12000};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_audio_capture_and_rendering);
        bindView();
        getAppIDAndUserIDAndAppSign();
        setValue();
        initEngineAndUser();
        loginRoom();
        setStartPublishButtonEvent();
        setStartPlayButtonEvent();
        setLogComponent();
        setApiCalledResult();
        setEventHandler();
        setAudioConfig();
        initAudioRecord();
        initAudioTrack();
        initData();
        initAAC();
    }
    public void bindView(){
        userIDText = findViewById(R.id.userIDText);
        roomState = findViewById(R.id.roomState);
        startPlayingButton = findViewById(R.id.startPlayButton);
        startPublishingButton = findViewById(R.id.startPublishButton);
        preview = findViewById(R.id.PreviewView);
        playView = findViewById(R.id.PlayView);
        roomState = findViewById(R.id.roomState);
        roomIDText = findViewById(R.id.roomIDText);
    }
    //get appID and userID and appSign
    public void getAppIDAndUserIDAndAppSign(){
        appID = KeyCenter.getInstance().getAppID();
        userID = UserIDHelper.getInstance().getUserID();
        appSign = KeyCenter.getInstance().getAppSign();
    }

    public void setValue(){
        roomID = getIntent().getStringExtra("roomID");
        publishStreamID = getIntent().getStringExtra("publishStreamID");
        playStreamID = getIntent().getStringExtra("playStreamID");
        isMicrophone = getIntent().getBooleanExtra("isMicrophone",false);
        enableCustomCapture = getIntent().getBooleanExtra("enableCustomCapture",false);
        customAudioFrameType = getIntent().getIntExtra("customAudioFrameType", 0);

        userIDText.setText(userID);
        roomIDText.setText(roomID);
        setTitle(getString(R.string.custom_audio_capture_and_render));

    }
    public void setAudioConfig(){
        if (enableCustomCapture) {
            // Set audio source type is custom.
            ZegoCustomAudioConfig config = new ZegoCustomAudioConfig();
            config.sourceType = ZegoAudioSourceType.CUSTOM;
            engine.enableCustomAudioIO(true, config);
        }
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
    private void initAudioRecord() {
        // get the buffer size of every frame.
        //获取每一帧的字节流大小
        recordBufferSize = AudioRecord.getMinBufferSize(captureSampleRate
                , captureChannel
                , AudioFormat.ENCODING_PCM_16BIT);
        PcmBuffer = ByteBuffer.allocateDirect(recordBufferSize);
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC
                    , captureSampleRate
                    , captureChannel
                    , AudioFormat.ENCODING_PCM_16BIT
                    , recordBufferSize);
        status = Status.STATUS_READY;
    }
    public void startRecord() {
        if (status == Status.STATUS_NO_READY || audioRecord == null) {
            throw new IllegalStateException("AudioRecord is not init");
        }
        if (status == Status.STATUS_START) {
            return;
        }

        if(customAudioFrameType == 0) //PCM frame
        {
            if (isMicrophone) {
                captureMicrophone();
                status = Status.STATUS_START;
            } else {
                isCaptureMedia = true;
                captureMedia();
            }
        }
        else // AAC frame
        {
            startAAC();
        }
    }

    private void startAAC() {
        if(aacCaptureThread != null)
        {
            if(aacCaptureThread.isAlive())
            {
                return;
            }
        }
        exitAacThread = false;

        aacCaptureThread = new Thread(new Runnable() {
            @Override
            public void run() {
                bufferSize = 0;
                sendAsc = false;
                int configLen = 0;
                int samples = 0;
                long timeStamp = 0;
                long lastTimeStamp = 0;
                ByteBuffer dataBuffer = ByteBuffer.allocateDirect(4096);

                // open aac file
                try {
                    aacData = getAssets().open(aacFileName);
                    lastTimeStamp = System.currentTimeMillis();
                    while(exitAacThread == false)
                    {
                        try {
                            AACFixedHeader aacFixedHeader = getAACFrame();
                            if(aacFixedHeader.aac_frame_length == 0)
                            {
                                return;
                            }

                            samples = (aacFixedHeader.number_of_raw_data_blocks_in_frame + 1) * 1024;
                            configLen = 0;

                            // Special config
                            AudioSpecificConfig asc = new AudioSpecificConfig();
                            asc.object_type = aacFixedHeader.profile + 1;
                            asc.sampling_index = aacFixedHeader.sampling_frequency_index;
                            asc.chan_config = aacFixedHeader.channel_configuration;

                            aacConfigBufferNew[0] = (byte)((asc.object_type<<3)|(asc.sampling_index>>1));
                            aacConfigBufferNew[1] = (byte)(((asc.sampling_index&0x1) << 7)|(asc.chan_config<<3));

                            // Set special config
                            if (aacConfigBufferNew[0] != aacConfigBuffer[0] && aacConfigBufferNew[1] != aacConfigBuffer[1])
                            {
                                aacConfigBuffer = aacConfigBufferNew.clone();
                                sendAsc = false;
                            }
                            int bufferLen = (int)aacFixedHeader.aac_frame_length - 7 - (aacFixedHeader.protection_absent==1?0:2);
                            dataBuffer.clear();
                            if (!sendAsc)
                            {
                                dataBuffer.put(aacConfigBuffer, 0, 2);
                                bufferLen+=2;
                                configLen = 2;
                                sendAsc = true;
                            }
                            dataBuffer.put(aacDataBuffer, 7 + (aacFixedHeader.protection_absent==1?0:2), bufferLen);
                            dataBuffer.flip();

                            ZegoAudioFrameParam param = new ZegoAudioFrameParam();
                            param.channel = ZegoAudioChannel.getZegoAudioChannel(aacFixedHeader.channel_configuration);
                            param.sampleRate = ChangeToZegoSamplerate(aacSamplerates[aacFixedHeader.sampling_frequency_index]);

                            // poll audio frame and send to sdk
                            engine.sendCustomAudioCaptureAACData(dataBuffer, bufferLen, configLen, timeStamp, samples, param, ZegoPublishChannel.MAIN);

                            long now = System.currentTimeMillis();
                            long interval = now - lastTimeStamp;
                            if (interval < timeStamp)
                            {
                                Thread.sleep(timeStamp - interval);
                            }

                            timeStamp += (long)((double)samples * 1000/(double)param.sampleRate.value());

                            // Copy remaining data to buffer
                            aacTempBuffer = aacDataBuffer.clone();
                            for(int i=0;i<bufferSize - aacFixedHeader.aac_frame_length;i++)
                            {
                                aacDataBuffer[i] = aacTempBuffer[(int)aacFixedHeader.aac_frame_length + i];
                            }
                            bufferSize -= aacFixedHeader.aac_frame_length;

                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    aacData.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        aacCaptureThread.start();
    }

    private void StopAAC() throws InterruptedException, IOException {
        if(exitAacThread == false)
        {
            exitAacThread = true;
            if(aacCaptureThread != null)
            {
                aacCaptureThread.join();
            }
        }
    }

    private AACFixedHeader getAACFrame() throws IOException {
        AACFixedHeader aacFixedHeader = new AACFixedHeader();
        if (bufferSize < 0x800) {
            int len = aacData.read(aacDataBuffer, bufferSize, 4096-bufferSize);
            if (len != -1) {
                if (len < 4096 - bufferSize)
                    bufferSize = bufferSize+len;
                else
                    bufferSize = 4096;
            }
        }
        if (bufferSize == 0)
            return aacFixedHeader;

        if (!(((aacDataBuffer[0]&0xFF) == 0xFF)&&((aacDataBuffer[1] & 0xF0) == 0xF0)))
            return aacFixedHeader;

        aacFixedHeader.protection_absent = aacDataBuffer[1] & 0x01;
        aacFixedHeader.profile = (aacDataBuffer[2]&0xC0) >> 6;
        aacFixedHeader.sampling_frequency_index = (aacDataBuffer[2]&0x3C) >> 2;
        aacFixedHeader.channel_configuration = ((aacDataBuffer[2]&0x01)<<2)|((aacDataBuffer[3]&0xC0)>>6);
        aacFixedHeader.aac_frame_length = (((aacDataBuffer[3] & 0x3)) << 11)
            | (((aacDataBuffer[4]&0xFF)) << 3) | ((aacDataBuffer[5]&0xFF) >> 5);
        aacFixedHeader.number_of_raw_data_blocks_in_frame = (int)aacDataBuffer[6] & 0x03;

        return aacFixedHeader;
    }

    private void captureMicrophone(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                final byte[] bytes = new byte[recordBufferSize];
                audioRecord.startRecording();
                audioRecord.read(bytes, 0, bytes.length);
                isEnd = false;
                while (true) {
                    if (isEnd) break;

                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    audioRecord.read(bytes, 0, bytes.length);
                    PcmBuffer.clear();
                    PcmBuffer.put(bytes, 0, recordBufferSize);
                    audioFrameParam.sampleRate = ZEGO_AUDIO_SAMPLE_RATE_44K;
                    engine.sendCustomAudioCapturePCMData(PcmBuffer, recordBufferSize, audioFrameParam);
                }
            }
        }).start();
    }
    private void captureMedia(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mediaBuffer != null) {
                        mediaBuffer.clear();
                    }
                    InputStream is = null;
                    is = getAssets().open("test.wav");
                    mediaByte = new byte[is.available()];
                    is.read(mediaByte);
                    is.close();
                    mediaBuffer = ByteBuffer.allocateDirect(mediaByte.length);
                    mediaBuffer.put(mediaByte);
                    mediaBuffer.flip();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
                    @Override
                    public void run() {
                        if (isCaptureMedia) {
                            int dataLength = (int) (duration * sampleRate * audioChannels * bytesPerSample);
                            int length = position + dataLength > mediaByte.length ? mediaByte.length - position : dataLength;
                            temp = new byte[length];
                            mediaBuffer.get(temp, 0, length);
                            ByteBuffer passingBuffer = ByteBuffer.allocateDirect(temp.length);
                            passingBuffer.put(temp);
                            position = position + length;
                            audioFrameParam.sampleRate = ZEGO_AUDIO_SAMPLE_RATE_16K;
                            passingBuffer.flip();
                            engine.sendCustomAudioCapturePCMData(passingBuffer, length, audioFrameParam);
                        }
                    }
                }, 0, (int) (duration * 1000), TimeUnit.MILLISECONDS);
                status = Status.STATUS_START;
            }
        }).start();
    }
    private void stopRecord() {
        if (status == Status.STATUS_STOP) {
            Log.i("[ZEGO]", "The custom audio capture has been disabled, please do not click repeatedly");
            return;
        }
        if (isMicrophone){
            audioRecord.stop();
            isEnd = true;
        } else {
            isCaptureMedia = false;
        }
        engine.stopPreview();
        status = Status.STATUS_STOP;
    }
    public void startPublish(){
        engine.startPreview(new ZegoCanvas(preview));
        engine.startPublishingStream(publishStreamID);

        if (enableCustomCapture){
            if(customAudioFrameType == 0)
            {
                startRecord();
            }
        } else if (!isMicrophone){
            if (effectPlayer == null) {
                effectPlayer = engine.createAudioEffectPlayer();
            }
            String path = "";
            if (customAudioFrameType == 1) {
                path = getExternalFilesDir("").getPath()+ "/" + aacFileName;
            } else
            {
                path = getExternalFilesDir("").getPath()+"/test.wav";
            }
            ZegoAudioEffectPlayConfig playConfig = new ZegoAudioEffectPlayConfig();
            playConfig.isPublishOut = true;
            effectPlayer.start(audioIndex,path,playConfig);
        }
        AppLogger.getInstance().callApi("Start Publishing Stream:%s",publishStreamID);
        startPublishingButton.setText(getString(R.string.stop_publishing));
    }
    public void stopPublish() throws InterruptedException, IOException {
        if (enableCustomCapture){
            stopRecord();
        }else if (!isMicrophone) {
            effectPlayer.stop(audioIndex);
        }
        StopAAC();
        engine.stopPreview();
        engine.stopPublishingStream();
        AppLogger.getInstance().callApi("Stop Publishing Stream:%s",publishStreamID);
        startPublishingButton.setText(getString(R.string.start_publishing));
    }
    public void setStartPlayButtonEvent(){
        startPlayingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if the user is playing the stream, this button is used to stop playing. Otherwise, this button is used to start publishing.
                if (isPlay){
                    if (enableCustomCapture){
                        stopRender();
                    }
                    engine.stopPlayingStream(playStreamID);
                    AppLogger.getInstance().callApi("Stop Playing Stream:%s",playStreamID);
                    startPlayingButton.setText(getString(R.string.start_playing));
                    isPlay = false;
                } else {
                    engine.startPlayingStream(playStreamID, new ZegoCanvas(playView));
                    if (enableCustomCapture){
                        startRender();
                    }
                    startPlayingButton.setText(getString(R.string.stop_playing));
                    AppLogger.getInstance().callApi("Start Playing Stream:%s",playStreamID);
                    isPlay = true;
                }
            }
        });
    }
    public void setStartPublishButtonEvent(){
        startPublishingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if the user is publishing the stream, this button is used to stop publishing. Otherwise, this button is used to start publishing.
                if (isPublish){
                    try {
                        stopPublish();
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace();
                    }
                    isPublish = false;
                } else {
                    startPublish();
                    isPublish = true;
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
                if(state == ZegoPublisherState.PUBLISHING)
                {
                    if(enableCustomCapture)
                    {
                        if(customAudioFrameType == 1)
                        {
                            // start aac audio frame capture after publishing stream
                            startAAC();
                        }
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
    private int mRenderBufferSize;
    private AudioTrack mAudioTrack;
    private int RENDER_SAMPLE_RATE;
    private int CHANNEL = AudioFormat.CHANNEL_OUT_MONO;
    private enum RenderStatus {
        STATUS_NO_READY,
        STATUS_READY,
        STATUS_START,
        STATUS_STOP
    }

    private  static RenderStatus renderStatus;
    private ByteBuffer renderBuffer;

    private void initAudioTrack() {
        if (isMicrophone){
            RENDER_SAMPLE_RATE = 44100;
        } else {
            RENDER_SAMPLE_RATE = 16000;
        }
        mRenderBufferSize = AudioTrack.getMinBufferSize(RENDER_SAMPLE_RATE, CHANNEL, AudioFormat.ENCODING_PCM_16BIT);
        renderBuffer = ByteBuffer.allocateDirect(mRenderBufferSize);
        if (mRenderBufferSize <= 0) {
            throw new IllegalStateException("AudioTrack is not available " + mRenderBufferSize);
        }
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, RENDER_SAMPLE_RATE, CHANNEL, AudioFormat.ENCODING_PCM_16BIT,
                mRenderBufferSize, AudioTrack.MODE_STREAM);
        renderStatus = RenderStatus.STATUS_READY;
    }

    public void startRender() throws IllegalStateException {
        if (renderStatus == RenderStatus.STATUS_NO_READY || mAudioTrack == null) {
            throw new IllegalStateException("AudioTrack is not init");
        }
        if (renderStatus == RenderStatus.STATUS_START) {
            return;
        }
        if (isMicrophone){
            renderFromMicrophone();
            renderStatus = RenderStatus.STATUS_START;
        }else {
            renderFromMedia();
        }
    }
    public void renderFromMicrophone(){
        new Thread() {
            public void run() {
                try {
                    if (mAudioTrack == null) {
                        return;
                    }
                    mAudioTrack.play();
                    byte[] bytes = new byte[mRenderBufferSize];
                    while (renderStatus == RenderStatus.STATUS_START) {
                        renderBuffer.clear();//清除buffer
                        //采集
                        if (isMicrophone){
                            //RENDER_SAMPLE_RATE = 44100;
                            audioFrameParam.sampleRate = ZEGO_AUDIO_SAMPLE_RATE_44K;
                        } else {
                            audioFrameParam.sampleRate = ZEGO_AUDIO_SAMPLE_RATE_16K;
                            //RENDER_SAMPLE_RATE = 16000;
                        }
                        //audioFrameParam.sampleRate = ZEGO_AUDIO_SAMPLE_RATE_16K;
                        engine.fetchCustomAudioRenderPCMData(renderBuffer, mRenderBufferSize, audioFrameParam);
                        if (renderBuffer != null) {
                            renderBuffer.get(bytes);
                        }
                        mAudioTrack.write(bytes, 0, bytes.length);
                    }
                }
                catch (Exception e) {
                    Log.i("[ZEGO]", "playAudioData Exception:" + e.getMessage());
                }
            }
        }.start();
    }
    public void renderFromMedia() {
        new Thread() {
            public void run() {
                try {
                    if (mAudioTrack == null) {
                        return;
                    }
                    int dataLength = (int)(mRenderBufferSize*duration);
                    renderStatus =RenderStatus.STATUS_START;
                    mAudioTrack.play();
                    byte[] bytes = new byte[dataLength];
                    while (renderStatus == RenderStatus.STATUS_START) {
                        renderBuffer.clear();//清除buffer
                        //采集
                        if (isMicrophone){
                            //RENDER_SAMPLE_RATE = 44100;
                            audioFrameParam.sampleRate = ZEGO_AUDIO_SAMPLE_RATE_44K;
                        } else {
                            audioFrameParam.sampleRate = ZEGO_AUDIO_SAMPLE_RATE_16K;
                            //RENDER_SAMPLE_RATE = 16000;
                        }

                        engine.fetchCustomAudioRenderPCMData(renderBuffer, dataLength, audioFrameParam);
                            if (renderBuffer != null) {
                                renderBuffer.get(bytes);
                            }
                            mAudioTrack.write(bytes, 0, bytes.length);
                    }
                } catch (Exception e) {
                    Log.i("[ZEGO]", "playAudioData Exception:" + e.getMessage());
                }
            }
        }.start();
    }
    public void stopRender()  {
        if (renderStatus == RenderStatus.STATUS_NO_READY || renderStatus == RenderStatus.STATUS_READY) {
           return;
        } else {
            mAudioTrack.stop();
            renderStatus = RenderStatus.STATUS_STOP;
        }
    }

    public void releaseRender() {
        if (mAudioTrack != null) {
            mAudioTrack.release();
            mAudioTrack = null;
        }
        renderStatus = RenderStatus.STATUS_NO_READY;
    }
    private void initData() {
        fileNames.add("test.wav");
        fileNames.add(aacFileName);
        copyAssetsFiles(fileNames);
    }

    private void initAAC(){
        aacDataBuffer = new byte[4096];
        aacTempBuffer = new byte[4096];
        bufferSize = 0;
        sendAsc = false;
        aacData = null;
        aacConfigBuffer = new byte[2];
        aacConfigBufferNew = new byte[2];
        exitAacThread = false;
    }

    private ZegoAudioSampleRate ChangeToZegoSamplerate(int sampleRate)
    {
        for(ZegoAudioSampleRate sampleRate1:ZegoAudioSampleRate.values())
        {
            if(sampleRate == sampleRate1.value())
            {
                return sampleRate1;
            }
        }
        return ZegoAudioSampleRate.UNKNOWN;
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
        System.out.println("File path---->" + file.getAbsolutePath());
        if (file.exists()) {
            System.out.println("File exists");
            return;
        }
        try {
            // Get Assets
            AssetManager assetManager = getAssets();
            InputStream is = assetManager.open(fileName);//输入流
            FileOutputStream fos = new FileOutputStream(file);//输出流
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

    @Override
    protected void onStop() {
        stopRecord();
        stopRender();
        engine.destroyAudioEffectPlayer(effectPlayer);
        releaseRender();
        try {
            StopAAC();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        ZegoExpressEngine.destroyEngine(null);
        super.onDestroy();
    }
}