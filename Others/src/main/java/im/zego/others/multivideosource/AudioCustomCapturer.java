package im.zego.others.multivideosource;

import static im.zego.zegoexpress.constants.ZegoAudioSampleRate.ZEGO_AUDIO_SAMPLE_RATE_16K;
import static im.zego.zegoexpress.constants.ZegoAudioSampleRate.ZEGO_AUDIO_SAMPLE_RATE_44K;

import android.content.Context;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;

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

import im.zego.zegoexpress.ZegoAudioEffectPlayer;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.constants.ZegoAudioChannel;
import im.zego.zegoexpress.constants.ZegoAudioSampleRate;
import im.zego.zegoexpress.constants.ZegoPublishChannel;
import im.zego.zegoexpress.entity.ZegoAudioFrameParam;

public class AudioCustomCapturer {
    Context mContext;
    ZegoExpressEngine mEngine;
    private ZegoPublishChannel mChannel = ZegoPublishChannel.MAIN;
    private boolean mUseDevice = false;
    private int mAudioFrameType = 0; // 0, PCM; 1, AAC

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

    public AudioCustomCapturer(Context ctx, ZegoExpressEngine engine, ZegoPublishChannel channel, boolean useDevice, int audioFrameType) {
        mContext = ctx;
        mEngine = engine;
        mChannel = channel;
        mUseDevice = useDevice;
        mAudioFrameType = audioFrameType;
    }

    public void start() {
        status = Status.STATUS_READY;
        if (mUseDevice) {
            initAudioRecord();
        } else {
            if (mAudioFrameType == 0) {
                initData();
            } else if (mAudioFrameType == 1) {
                initAAC();
            }
        }

        startRecord();
    }

    public void stop() {
        stopRecord();
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
    }

    private void startRecord() {
        if (status == Status.STATUS_NO_READY || (mUseDevice && audioRecord == null)) {
            throw new IllegalStateException("AudioRecord is not init");
        }
        if (status == Status.STATUS_START) {
            return;
        }

        if(mAudioFrameType == 0) //PCM frame
        {
            if (mUseDevice) {
                captureMicrophone();
                status = Status.STATUS_START;
            } else {
                captureMedia();
            }
        }
        else // AAC frame
        {
            final Handler handler = new Handler(mContext.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startAAC();
                }
            }, 100);
        }
    }

    private void stopRecord() {
        if (status == Status.STATUS_STOP) {
            Log.i("[ZEGO]", "The custom audio capture has been disabled, please do not click repeatedly");
            return;
        }

        isEnd = true;
        if (mUseDevice){
            try {
                audioRecord.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (mAudioFrameType == 1) {
            try {
                stopAAC();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        status = Status.STATUS_STOP;
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
                    if (mediaBuffer != null) {
                        mediaBuffer.clear();
                    }
                    InputStream is = null;
                    is = mContext.getAssets().open(aacFileName);
                    mediaByte = new byte[is.available()];
                    is.read(mediaByte);
                    is.close();
                    mediaBuffer = ByteBuffer.allocateDirect(mediaByte.length);
                    mediaBuffer.put(mediaByte);
                    mediaBuffer.flip();
                } catch (IOException e) {
                    e.printStackTrace();
                }

//                try {
//                    aacData = mContext.getAssets().open(aacFileName);
                    lastTimeStamp = System.currentTimeMillis();
                    while(true)
                    {
                        if (exitAacThread) {
                            break;
                        }

                        try {
                            AACFixedHeader aacFixedHeader = getAACFrame();
                            if(aacFixedHeader.aac_frame_length == 0)
                            {
                                mediaBuffer.rewind();
                                bufferSize = 0;

                                continue;
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
                            mEngine.sendCustomAudioCaptureAACData(dataBuffer, bufferLen, configLen, timeStamp, samples, param, mChannel);

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
//                    aacData.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
            }
        });
        aacCaptureThread.setName("custom-file-aac");
        aacCaptureThread.start();
    }

    private void stopAAC() throws InterruptedException, IOException {
        if(!exitAacThread)
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
//            int len = aacData.read(aacDataBuffer, bufferSize, 4096-bufferSize);
            int length = 4096 - bufferSize;
            if (mediaBuffer.remaining() < length) {
                length = mediaBuffer.remaining();
            }
            mediaBuffer.get(aacDataBuffer, bufferSize, length);
            int len = aacDataBuffer.length;
            if (len > 0) {
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
        isEnd = false;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                final byte[] bytes = new byte[recordBufferSize];
                audioRecord.startRecording();
                audioRecord.read(bytes, 0, bytes.length);
                while (true) {
                    if (isEnd || !mUseDevice) break;

                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    audioRecord.read(bytes, 0, bytes.length);
                    PcmBuffer.clear();
                    PcmBuffer.put(bytes, 0, recordBufferSize);
                    audioFrameParam.sampleRate = ZEGO_AUDIO_SAMPLE_RATE_44K;
                    mEngine.sendCustomAudioCapturePCMData(PcmBuffer, recordBufferSize, audioFrameParam, mChannel);
                }
            }
        });
        t.setName("custom-mic-pcm");
        t.start();
    }

    private void captureMedia(){
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mediaBuffer != null) {
                        mediaBuffer.clear();
                    }
                    InputStream is = null;
                    is = mContext.getAssets().open("test.wav");
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
                        if (isEnd || mUseDevice) {
                            return;
                        }

                        int dataLength = (int) (duration * sampleRate * audioChannels * bytesPerSample);
                        int length = position + dataLength > mediaByte.length ? mediaByte.length - position : dataLength;
                        if (length == 0) {
                            mediaBuffer.rewind();
                            length = dataLength;
                            position = 0;
                        }
                        temp = new byte[length];
                        mediaBuffer.get(temp, 0, length);
                        ByteBuffer passingBuffer = ByteBuffer.allocateDirect(temp.length);
                        passingBuffer.put(temp);
                        position = position + length;
                        audioFrameParam.sampleRate = ZEGO_AUDIO_SAMPLE_RATE_16K;
                        passingBuffer.flip();
                        mEngine.sendCustomAudioCapturePCMData(passingBuffer, length, audioFrameParam, mChannel);
                    }
                }, 0, (int) (duration * 1000), TimeUnit.MILLISECONDS);
                status = Status.STATUS_START;

                while (true) {
                    if (isEnd) {
                        break;
                    }

                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        t.setName("custom-file-pcm");
        t.start();
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
        final File file = new File(mContext.getExternalFilesDir(""), fileName);//getFilesDir()方法用于获取/data/data//files目录
        System.out.println("File path---->" + file.getAbsolutePath());
        if (file.exists()) {
            System.out.println("File exists");
            return;
        }
        try {
            // Get Assets
            AssetManager assetManager = mContext.getAssets();
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
}
