package im.zego.commontools.videorender;

import android.annotation.TargetApi;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Choreographer;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

//import im.zego.customrender.ui.VideoRenderPublish;
import im.zego.zegoexpress.callback.IZegoCustomVideoRenderHandler;
import im.zego.zegoexpress.constants.ZegoPublishChannel;
import im.zego.zegoexpress.constants.ZegoVideoFlipMode;
import im.zego.zegoexpress.entity.ZegoVideoEncodedFrameParam;
import im.zego.zegoexpress.entity.ZegoVideoFrameParam;

/**
         * VideoRenderHandler
         * Renderer encapsulation layer, the interface is more conducive to the upper layer call
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class VideoRenderHandler extends IZegoCustomVideoRenderHandler implements Choreographer.FrameCallback {
    private static final String TAG = "VideoRenderHandler";

    public static final Object lock = new Object();

    // opengl color configuration
    public static final int[] CONFIG_RGBA = {
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_NONE
    };

    private EGLContext eglContext;
    private EGLConfig eglConfig;
    private EGLDisplay eglDisplay;
    private boolean isRunning = false;


    private int viewWidth;
    private int viewHeight;

    private HandlerThread thread = null;
    private Handler handler = null;

    /** Single frame video data
     * Including the width, height, data and strides of the video screen
     */

    private ConcurrentHashMap<String, MyVideoFrame> frameMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<ZegoPublishChannel, MyVideoFrame> captureFrameMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, MyVideoFrame> getFrameMap() {
        return frameMap;
    }
    private ConcurrentHashMap<ZegoPublishChannel, MyVideoFrame> getCaptureFrameMap() {
        return captureFrameMap;
    }

    // Store the streamID and corresponding render
    private ConcurrentHashMap<String, Renderer> rendererMap = null;
    private ConcurrentHashMap<String, Renderer> rendererSurfaceViewMap = null;
    private ConcurrentHashMap<ZegoPublishChannel, Renderer> captureRendererMap = null;

    // Initialization, including thread startup, video frame callback monitoring, opengl related parameter settings, etc.
    public final int init() {
        thread = new HandlerThread("VideoRenderHandler" + hashCode());
        thread.start();
        handler = new Handler(thread.getLooper());

        final CountDownLatch barrier = new CountDownLatch(1);
        handler.post(new Runnable() {
            @Override
            public void run() {
                eglDisplay = getEglDisplay();
                eglConfig = getEglConfig(eglDisplay, CONFIG_RGBA);
                eglContext = createEglContext(null, eglDisplay, eglConfig);

                Choreographer.getInstance().postFrameCallback(VideoRenderHandler.this);
                isRunning = true;

                barrier.countDown();
            }
        });
        try {
            barrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        rendererMap = new ConcurrentHashMap<>();
        rendererSurfaceViewMap = new ConcurrentHashMap<>();
        captureRendererMap = new ConcurrentHashMap<>();

        return 0;
    }

    private void checkNotNull() {
        synchronized (lock) {
            if (rendererMap == null) {
                rendererMap = new ConcurrentHashMap<>();
            }
            if(captureRendererMap == null){
                captureRendererMap = new ConcurrentHashMap<>();
            }
        }
    }

    public void setSize(int width,int height){
        viewWidth = width;
        viewHeight = height;
    }
    // Add rendering view based on stream name
    public void addView(final String streamID, final TextureView textureView) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                checkNotNull();
                if (rendererMap.get(streamID) == null) {
                    Log.i(TAG, String.format("new Renderer streamId : %s", streamID));
                    // create a renderer
                    Renderer renderer = new Renderer(eglContext, eglDisplay, eglConfig);
                    // set the render view
                    renderer.setRendererView(textureView);
                    renderer.setStreamID(streamID);
                    rendererMap.put(streamID, renderer);
                } else {
                    rendererMap.get(streamID).setRendererView(textureView);
                    Log.i(TAG, String.format("setRendererView Renderer streamId : %s", streamID));
                }
            }
        });
    }
    public void addView(final String streamID, final SurfaceView textureView) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                checkNotNull();
                if (rendererSurfaceViewMap.get(streamID) == null) {
                    Log.i(TAG, String.format("new Renderer streamId : %s", streamID));
                    // create a renderer
                    Renderer renderer = new Renderer(eglContext, eglDisplay, eglConfig);
                    // set the render view
                    renderer.setRendererView(textureView);
                    renderer.setStreamID(streamID);
                    rendererSurfaceViewMap.put(streamID, renderer);
                } else {
                    rendererSurfaceViewMap.get(streamID).setRendererView(textureView);
                    Log.i(TAG, String.format("setRendererView Renderer streamId : %s", streamID));
                }
            }
        });
    }
    // Add rendering view based on stream name
    public void addCaptureView(final ZegoPublishChannel channel, final TextureView textureView) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                checkNotNull();
                if (captureRendererMap.get(channel) == null) {
                    Log.i(TAG, String.format("new cpature renderer channel : %s", channel));
                    // create a renderer
                    Renderer renderer = new Renderer(eglContext, eglDisplay, eglConfig);
                    // set the render view
                    renderer.setRendererView(textureView);
                    renderer.setPublishChannel(channel);
                    captureRendererMap.put(channel, renderer);
                } else {
                    captureRendererMap.get(channel).setRendererView(textureView);
                    Log.i(TAG, String.format("setRendererView Renderer channel : %s", channel));
                }
            }
        });
    }

    // Delete the rendering view bound by the specified stream
    public void removeView(final String streamID, boolean isSurfaceView) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                checkNotNull();
                if(isSurfaceView){

                    if (rendererSurfaceViewMap.get(streamID) != null) {
                        if(rendererSurfaceViewMap.get(streamID).isTextureView() == !isSurfaceView){
                            Log.i(TAG, String.format("removeView Renderer streamId : %s", streamID));
                            // release EGL Surface
                            rendererSurfaceViewMap.get(streamID).uninitEGLSurface();
                            // release Render
                            rendererSurfaceViewMap.get(streamID).uninit();
                            rendererSurfaceViewMap.remove(streamID);
                        }
                    }
                }else{

                    if (rendererMap.get(streamID) != null) {
                        if(!rendererMap.get(streamID).isTextureView() == isSurfaceView){
                            Log.i(TAG, String.format("removeView Renderer streamId : %s", streamID));
                            // release EGL Surface
                            rendererMap.get(streamID).uninitEGLSurface();
                            // release Render
                            rendererMap.get(streamID).uninit();
                            rendererMap.remove(streamID);
                        }
                    }
                }

                if (rendererSurfaceViewMap.get(streamID) == null && rendererMap.get(streamID) == null){
                    if (getFrameMap().get(streamID) != null) {
                        Log.i(TAG, String.format("removeView frameMap streamId : %s", streamID));
                        getFrameMap().remove(streamID);
                    }
                }
            }
        });
    }
    // Delete the rendering view bound by the specified channel
    public void removeCaptureView(final ZegoPublishChannel channel) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                checkNotNull();
                if (captureRendererMap.get(channel) != null) {
                    Log.i(TAG, String.format("removeView Renderer channel : %s", channel));
                    // release EGL Surface
                    captureRendererMap.get(channel).uninitEGLSurface();
                    // release Render
                    captureRendererMap.get(channel).uninit();
                    captureRendererMap.remove(channel);
                }
                if (getCaptureFrameMap().get(channel) != null) {
                    Log.i(TAG, String.format("removeView frameMap channel : %s", channel));
                    getCaptureFrameMap().remove(channel);
                }
            }
        });
    }

    // Delete all rendered views
    public void removeAllView() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                checkNotNull();
                Log.i(TAG, "removeAllView");
                for (Map.Entry<String, Renderer> entry : rendererMap.entrySet()) {
                    Renderer renderer = entry.getValue();
                    // release EGL Surface
                    renderer.uninitEGLSurface();
                    // release Render
                    renderer.uninit();
                    rendererMap.remove(entry.getKey());
                }
                for (Map.Entry<ZegoPublishChannel, Renderer> entry : captureRendererMap.entrySet()) {
                    Renderer renderer = entry.getValue();
                    // release EGL Surface
                    renderer.uninitEGLSurface();
                    // release Render
                    renderer.uninit();
                    captureRendererMap.remove(entry.getKey());
                }
                for (Map.Entry<String, Renderer> entry : rendererSurfaceViewMap.entrySet()) {
                    Renderer renderer = entry.getValue();
                    // release EGL Surface
                    renderer.uninitEGLSurface();
                    // release Render
                    renderer.uninit();
                    rendererSurfaceViewMap.remove(entry.getKey());
                }


                for (Map.Entry<String, MyVideoFrame> entry : getFrameMap().entrySet()) {
                    getFrameMap().remove(entry.getKey());
                }
            }
        });
    }

    // release Render
    private void release() {

        for (Map.Entry<String, Renderer> entry : rendererMap.entrySet()) {
            Renderer renderer = entry.getValue();
            renderer.uninitEGLSurface();
            renderer.uninit();
        }
        rendererMap.clear();
        rendererMap = null;
        for (Map.Entry<ZegoPublishChannel, Renderer> entry : captureRendererMap.entrySet()) {
            Renderer renderer = entry.getValue();
            renderer.uninitEGLSurface();
            renderer.uninit();
        }
        captureRendererMap.clear();
        captureRendererMap = null;
        for (Map.Entry<String, Renderer> entry : rendererSurfaceViewMap.entrySet()) {
            Renderer renderer = entry.getValue();
            renderer.uninitEGLSurface();
            renderer.uninit();
        }
        rendererSurfaceViewMap.clear();
        rendererSurfaceViewMap = null;

        // destroy EGLContext
        EGL14.eglDestroyContext(eglDisplay, eglContext);
        // release the thread
        EGL14.eglReleaseThread();
        // terminate display
        EGL14.eglTerminate(eglDisplay);

        eglContext = EGL14.EGL_NO_CONTEXT;
        eglDisplay = EGL14.EGL_NO_DISPLAY;
        eglConfig = null;
    }

    // Handle release-related operations, thread stop, remove video frame callback monitoring, etc.
    public final int uninit() {
        final CountDownLatch barrier = new CountDownLatch(1);
        handler.post(new Runnable() {
            @Override
            public void run() {
                isRunning = false;
                release();
                barrier.countDown();
            }
        });
        try {
            barrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        handler.removeCallbacksAndMessages(null);
        handler = null;

        if (Build.VERSION.SDK_INT >= 18) {
            thread.quitSafely();
        } else {
            thread.quit();
        }
        thread = null;

//        for (Map.Entry<String, Renderer> entry : rendererMap.entrySet()) {
//            Renderer renderer = entry.getValue();
//            renderer.uninit();
//        }
//
//        rendererMap = null;
        frameMap = null;

        // Remove video frame callback monitoring
        Choreographer.getInstance().removeFrameCallback(VideoRenderHandler.this);

        // release MediaCodec
        if (mAVCDecoder != null) {
            mAVCDecoder.stopAndReleaseDecoder();
            mAVCDecoder = null;
        }

        return 0;
    }

    // get EGLDisplay
    private static EGLDisplay getEglDisplay() {
        EGLDisplay eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException(
                    "Unable to get EGL14 display: 0x" + Integer.toHexString(EGL14.eglGetError()));
        }
        int[] version = new int[2];
        // initialize EGL
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            throw new RuntimeException(
                    "Unable to initialize EGL14: 0x" + Integer.toHexString(EGL14.eglGetError()));
        }
        return eglDisplay;
    }

    // get EGLConfig
    private static EGLConfig getEglConfig(EGLDisplay eglDisplay, int[] configAttributes) {
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        // choose the best Surface configure
        if (!EGL14.eglChooseConfig(
                eglDisplay, configAttributes, 0, configs, 0, configs.length, numConfigs, 0)) {
            throw new RuntimeException(
                    "eglChooseConfig failed: 0x" + Integer.toHexString(EGL14.eglGetError()));
        }
        if (numConfigs[0] <= 0) {
            throw new RuntimeException("Unable to find any matching EGL config");
        }
        final EGLConfig eglConfig = configs[0];
        if (eglConfig == null) {
            throw new RuntimeException("eglChooseConfig returned null");
        }
        return eglConfig;
    }

    // create EGLContext
    private static EGLContext createEglContext(
            EGLContext sharedContext, EGLDisplay eglDisplay, EGLConfig eglConfig) {
        if (sharedContext != null && sharedContext == EGL14.EGL_NO_CONTEXT) {
            throw new RuntimeException("Invalid sharedContext");
        }
        int[] contextAttributes = {EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE};
        EGLContext rootContext =
                sharedContext == null ? EGL14.EGL_NO_CONTEXT : sharedContext;
        final EGLContext eglContext;
        synchronized (VideoRenderHandler.lock) {
            // create EGLContext to store information of OpenGL ES
            eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, rootContext, contextAttributes, 0);
        }
        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            throw new RuntimeException(
                    "Failed to create EGL context: 0x" + Integer.toHexString(EGL14.eglGetError()));
        }
        return eglContext;
    }

    // Video frame callback implementation
    @Override
    public void doFrame(long frameTimeNanos) {
        if (!isRunning) {
            return;
        }
        Choreographer.getInstance().postFrameCallback(this);

        // draw the frame
        draw();
    }

    // Use the rendering type to draw
    private void draw() {
        for (Map.Entry<ZegoPublishChannel, MyVideoFrame> entry : captureFrameMap.entrySet()) {
            // get the data of video frame
            MyVideoFrame frameBuffer = entry.getValue();
            if (frameBuffer != null) {
                ZegoPublishChannel channel = entry.getKey();

                // get the corresponding render based on streamID
                Renderer renderer = captureRendererMap.get(channel);
                PixelBuffer pixelBuffer = new PixelBuffer();
                pixelBuffer.buffer = frameBuffer.byteBuffers;
                pixelBuffer.strides = frameBuffer.strides;
                pixelBuffer.height = frameBuffer.height;
                pixelBuffer.width = frameBuffer.width;
                pixelBuffer.flip = frameBuffer.flip;

                if (renderer != null) {
                    // draw the frame base on frame data
                    renderer.draw(pixelBuffer);
                }
            }
        }
        for (Map.Entry<String, MyVideoFrame> entry : frameMap.entrySet()) {
            // get the data of video frame
            MyVideoFrame frameBuffer = entry.getValue();
            if (frameBuffer != null) {
                String streamID = entry.getKey();

                // get the corresponding render based on streamID
                Renderer renderer = rendererMap.get(streamID);

                if (renderer != null) {
                    PixelBuffer pixelBuffer = new PixelBuffer();
                    pixelBuffer.buffer = frameBuffer.byteBuffers;
                    pixelBuffer.strides = frameBuffer.strides;
                    pixelBuffer.height = frameBuffer.height;
                    pixelBuffer.width = frameBuffer.width;
                    // draw the frame base on frame data
                    renderer.draw(pixelBuffer);
                }

                // get the corresponding render based on streamID
                Renderer rendererSurfaceView = rendererSurfaceViewMap.get(streamID);

                if (rendererSurfaceView != null) {
                    PixelBuffer pixelBuffer = new PixelBuffer();
                    pixelBuffer.buffer = frameBuffer.byteBuffers;
                    pixelBuffer.strides = frameBuffer.strides;
                    pixelBuffer.height = frameBuffer.height;
                    pixelBuffer.width = frameBuffer.width;
                    // draw the frame base on frame data
                    rendererSurfaceView.draw(pixelBuffer);
                }
            }
        }
        frameMap.clear();
    }


//    MyVideoFrame videoPlayFrame = new MyVideoFrame();


    @Override
    public void onCapturedVideoFrameRawData(ByteBuffer[] data, int[] dataLength, ZegoVideoFrameParam param, ZegoVideoFlipMode flipMode, ZegoPublishChannel channel){
        int sourceP = 0;
        int sourceL = 0;
        MyVideoFrame videoPlayFrame = new MyVideoFrame();
        for(int i=0;i<data.length;i++) {
            if(data[i] != null){
                sourceP = data[i].position();
                sourceL = data[i].limit();
                videoPlayFrame.byteBuffers[i] = ByteBuffer.allocateDirect(data[i].remaining());
                videoPlayFrame.byteBuffers[i].put(data[i]);
                videoPlayFrame.byteBuffers[i].flip();
                videoPlayFrame.byteBuffers[i].position(sourceP);
                videoPlayFrame.byteBuffers[i].limit(sourceL);
            }
        }
        videoPlayFrame.height = param.height;
        videoPlayFrame.width = param.width;
        videoPlayFrame.strides = param.strides;
        videoPlayFrame.flip = flipMode.value();
        getCaptureFrameMap().put(channel, videoPlayFrame);

        Log.d(TAG, String.format("onCapturedVideoFrameRawData, channel:%s, w:%d, h:%d, rotation:%d, flip:%s, format:%s",
                channel.toString(), param.width, param.height, param.rotation, flipMode.toString(), param.format.toString()));
    }

    @Override
    public void onRemoteVideoFrameRawData(ByteBuffer[] data, int[] dataLength, ZegoVideoFrameParam param, String streamID){
        int sourceP = 0;
        int sourceL = 0;
        MyVideoFrame videoPlayFrame1 = new MyVideoFrame();
        for(int i=0;i<data.length;i++) {
            if(data[i] != null){
                sourceP = data[i].position();
                sourceL = data[i].limit();
                videoPlayFrame1.byteBuffers[i] = ByteBuffer.allocateDirect(data[i].remaining());
                videoPlayFrame1.byteBuffers[i].put(data[i]);
                videoPlayFrame1.byteBuffers[i].flip();
                videoPlayFrame1.byteBuffers[i].position(sourceP);
                videoPlayFrame1.byteBuffers[i].limit(sourceL);
            }
        }
        videoPlayFrame1.height = param.height;
        videoPlayFrame1.width = param.width;
        videoPlayFrame1.strides = param.strides;
        getFrameMap().put(streamID, videoPlayFrame1);
        Log.d(TAG, String.format("onRemoteVideoFrameRawData, stream:%s, w:%d, h:%d, rotation:%d, format:%s",
                streamID, param.width, param.height, param.rotation, param.format.toString()));
    }

    @Override
    public void onRemoteVideoFrameEncodedData(ByteBuffer data, int dataLength, ZegoVideoEncodedFrameParam param, long referenceTimeMillisecond, String streamID) {
        Log.d(TAG, String.format("onRemoteVideoFrameEncodedData, stream:%s, w:%d, h:%d, rotation:%d, format:%s, timestamp:%d",
                streamID, param.width, param.height, param.rotation, param.format.toString(), referenceTimeMillisecond));

        byte[] tmpData = new byte[data.capacity()];
        data.position(0); // Without this line, the decoded video will stuck
        data.get(tmpData);
        if (mAVCDecoder != null) {
            viewHeight = param.height;
            viewWidth = param.width;
            // Provide video data and time stamp for decoding
            mAVCDecoder.inputFrameToDecoder(tmpData, (long) referenceTimeMillisecond);
        }
    }

    //  AVCANNEXB Decoder
    private AVCDecoder mAVCDecoder = null;

    // add the render view for  AVCANNEXB video
    public void addDecodView(final TextureView textureView){
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (mAVCDecoder == null){
                    // create the decoder
                    mAVCDecoder = new AVCDecoder(new Surface(textureView.getSurfaceTexture()), textureView.getWidth(), textureView.getHeight());
                    // start the decoder
                    mAVCDecoder.startDecoder();
                }
            }
        });
    }
}
