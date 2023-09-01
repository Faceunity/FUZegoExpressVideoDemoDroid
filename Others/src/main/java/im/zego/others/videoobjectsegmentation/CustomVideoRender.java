package im.zego.others.videoobjectsegmentation;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Xfermode;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.renderscript.RenderScript;
import android.util.Log;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
//import java.util.logging.Handler;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import im.zego.zegoexpress.callback.IZegoCustomVideoRenderHandler;
import im.zego.zegoexpress.constants.ZegoPublishChannel;
import im.zego.zegoexpress.constants.ZegoVideoFlipMode;
import im.zego.zegoexpress.constants.ZegoVideoFrameFormat;
import im.zego.zegoexpress.entity.ZegoVideoFrameParam;

public class CustomVideoRender extends IZegoCustomVideoRenderHandler {

    class RenderConfig{
        ZegoVideoFrameParam frameParam;
        ZegoVideoFlipMode filpMode;
        ByteBuffer buffer;
        RenderConfig(){

        }
    }
    HashMap<String, TextureView> views = new HashMap<>();
    TextureView previewView = null;
    TextureView playView1 = null;
    SurfaceView playView2 = null;
    String playStream1 = "";
    String playStream2 = "";
    Paint paintPreview = new Paint();
    Paint paintPlayView1 = new Paint();
    Paint paintPlayView2 = new Paint();
    boolean isPreview = false;
    Matrix matrix = new Matrix();
    byte r;
    byte g;
    byte b;
    byte a;
    final int WHAT = 102;
    android.os.Handler handler;
    android.os.Handler handlerPlay1;
    android.os.Handler handlerPlay2;
    ConcurrentLinkedQueue dataQueue = new ConcurrentLinkedQueue();
    ConcurrentLinkedQueue dataQueuePlay1 = new ConcurrentLinkedQueue();
    ConcurrentLinkedQueue dataQueuePlay2 = new ConcurrentLinkedQueue();

    CustomVideoRender(){
        matrix.postScale(-1, 1);

        //Preview
        handler = new Handler(Looper.myLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);

                switch (msg.what) {
                    case WHAT:
                        {
                            if(dataQueue.size() == 0){
                                Canvas canvas = previewView.lockCanvas();
                                if(canvas != null){
                                    Paint paintPlayView = new Paint();
                                    paintPlayView.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                                    canvas.drawPaint(paintPlayView);
                                }
                                previewView.unlockCanvasAndPost(canvas);
                                return;
                            }

                            RenderConfig renderFrame = (RenderConfig)dataQueue.poll();
                            int[] array = new int[renderFrame.buffer.capacity()/4];
                            //IntBuffer array = IntBuffer.allocate(renderFrame.buffer.capacity()/4);
                            for(int i=0;i <renderFrame.buffer.capacity()/4;i++){
                                b =  renderFrame.buffer.get(i*4);
                                g =  renderFrame.buffer.get(i*4+1);
                                r =  renderFrame.buffer.get(i*4+2);
                                a =  renderFrame.buffer.get(i*4+3);

                                int dd = (a&0xff)<<24 | (b&0xff)<<16 | (g&0xff)<<8 | (r&0xff);
//                                array.put(i,dd);
                                array[i] = dd;
                            }

                            Bitmap bmp = Bitmap.createBitmap(array, renderFrame.frameParam.width, renderFrame.frameParam.height, Bitmap.Config.ARGB_8888);
                            Bitmap bbmp = Bitmap.createScaledBitmap(bmp, previewView.getWidth(), previewView.getHeight(), false);
                            Bitmap bmpFinal = bbmp;
                            try
                            {
                                //镜像水平翻转
                                if(renderFrame.filpMode == ZegoVideoFlipMode.X){
                                    bmpFinal = Bitmap.createBitmap(bbmp, 0,0,bbmp.getWidth(), bbmp.getHeight(),matrix, false);
                                }
                                // 异常，则关闭硬件加速
//                            previewView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
//                            boolean xx = previewView.isHardwareAccelerated();
                                Canvas canvas = previewView.lockCanvas();
                                if(canvas != null){
                                    // 清屏
                                    paintPreview.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                                    canvas.drawPaint(paintPreview);

                                    //离屏绘制
                                    int layerID = canvas.saveLayer(0,0,canvas.getWidth(),canvas.getHeight(), null,Canvas.ALL_SAVE_FLAG);
                                    paintPreview.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
                                    canvas.drawBitmap(bmpFinal, 0,0, paintPreview);
                                    paintPreview.setXfermode(null);
                                    canvas.restoreToCount(layerID);
                                }
                                previewView.unlockCanvasAndPost(canvas);

                            }
                            catch (Exception x){
                            }
                        }
                        break;
                }
            }
        };

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Message message = new Message();
                message.what = WHAT;
                message.obj = System.currentTimeMillis();
                handler.sendMessage(message);
            }
        };

        Timer timer = new Timer();
        timer.schedule(task, 1000,50);

        // play view1
        handlerPlay1 = new Handler(Looper.myLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);

                switch (msg.what) {
                    case WHAT:
                    {
                        if(dataQueuePlay1.size() == 0){
                            Canvas canvas = playView1.lockCanvas();
                            if(canvas != null){
                                Paint paintPlayView = new Paint();
                                paintPlayView.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                                canvas.drawPaint(paintPlayView);
                            }
                            playView1.unlockCanvasAndPost(canvas);
                            return;
                        }

                        RenderConfig renderFrame = (RenderConfig)dataQueuePlay1.poll();
                        int[] array = new int[renderFrame.buffer.capacity()/4];

                        for(int i=0;i <renderFrame.buffer.capacity()/4;i++){
                            b =  renderFrame.buffer.get(i*4);
                            g =  renderFrame.buffer.get(i*4+1);
                            r =  renderFrame.buffer.get(i*4+2);
                            a =  renderFrame.buffer.get(i*4+3);

                            int dd = (a&0xff)<<24 | (b&0xff)<<16 | (g&0xff)<<8 | (r&0xff);
                            array[i] = (dd);
                        }

                        Bitmap bmp = Bitmap.createBitmap(array, renderFrame.frameParam.width, renderFrame.frameParam.height, Bitmap.Config.ARGB_8888);
                        Bitmap bbmp = Bitmap.createScaledBitmap(bmp, previewView.getWidth(), previewView.getHeight(), false);
                        Bitmap bmpFinal = bbmp;
                        try
                        {
                            // 异常，则关闭硬件加速
//                            previewView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
//                            boolean xx = previewView.isHardwareAccelerated();
                            Canvas canvas = playView1.lockCanvas();
                            if(canvas != null){
                                // 清屏
                                paintPlayView1.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                                canvas.drawPaint(paintPlayView1);

                                //离屏绘制
                                int layerID = canvas.saveLayer(0,0,canvas.getWidth(),canvas.getHeight(), null,Canvas.ALL_SAVE_FLAG);
                                paintPlayView1.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
                                canvas.drawBitmap(bmpFinal, 0,0, paintPlayView1);
                                paintPlayView1.setXfermode(null);
                                canvas.restoreToCount(layerID);
                            }
                            playView1.unlockCanvasAndPost(canvas);

                        }
                        catch (Exception x){
                        }
                    }
                    break;
                }
            }
        };

        TimerTask taskPlay1 = new TimerTask() {
            @Override
            public void run() {
                Message message = new Message();
                message.what = WHAT;
                message.obj = System.currentTimeMillis();
                handlerPlay1.sendMessage(message);
            }
        };

        Timer timerPlay1 = new Timer();
        timerPlay1.schedule(taskPlay1, 1000,50);

        // play view2
        handlerPlay2 = new Handler(Looper.myLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);

                switch (msg.what) {
                    case WHAT:
                    {
                        if(dataQueuePlay2.size() == 0){
                            Canvas canvas = playView2.getHolder().lockCanvas();
                            if(canvas != null){
                                Paint paintPlayView = new Paint();
                                paintPlayView.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                                canvas.drawPaint(paintPlayView);
                            }
                            playView2.getHolder().unlockCanvasAndPost(canvas);
                            return;
                        }

                        RenderConfig renderFrame = (RenderConfig)dataQueuePlay2.poll();
                        int[] array = new int[renderFrame.buffer.capacity()/4];

                        for(int i=0;i <renderFrame.buffer.capacity()/4;i++){
                            b =  renderFrame.buffer.get(i*4);
                            g =  renderFrame.buffer.get(i*4+1);
                            r =  renderFrame.buffer.get(i*4+2);
                            a =  renderFrame.buffer.get(i*4+3);

                            int dd = (a&0xff)<<24 | (b&0xff)<<16 | (g&0xff)<<8 | (r&0xff);
                            array[i] = (dd);
                        }

                        Bitmap bmp = Bitmap.createBitmap(array, renderFrame.frameParam.width, renderFrame.frameParam.height, Bitmap.Config.ARGB_8888);
                        Bitmap bbmp = Bitmap.createScaledBitmap(bmp, previewView.getWidth(), previewView.getHeight(), false);
                        Bitmap bmpFinal = bbmp;
                        try
                        {
                            // 异常，则关闭硬件加速
//                            previewView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
//                            boolean xx = previewView.isHardwareAccelerated();
                            Canvas canvas = playView2.getHolder().lockCanvas();
                            if(canvas != null){
                                // 清屏
                                paintPlayView2.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                                canvas.drawPaint(paintPlayView2);

                                //离屏绘制
                                int layerID = canvas.saveLayer(0,0,canvas.getWidth(),canvas.getHeight(), null,Canvas.ALL_SAVE_FLAG);
                                paintPlayView2.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
                                canvas.drawBitmap(bmpFinal, 0,0, paintPlayView2);
                                paintPlayView2.setXfermode(null);
                                canvas.restoreToCount(layerID);
                            }
                            playView2.getHolder().unlockCanvasAndPost(canvas);

                        }
                        catch (Exception x){
                        }
                    }
                    break;
                }
            }
        };

        TimerTask taskPlay2 = new TimerTask() {
            @Override
            public void run() {
                Message message = new Message();
                message.what = WHAT;
                message.obj = System.currentTimeMillis();
                handlerPlay2.sendMessage(message);
            }
        };

        Timer timerPlay2 = new Timer();
        timerPlay2.schedule(taskPlay2, 1000,50);
    }

    @Override
    public void onCapturedVideoFrameRawData(ByteBuffer[] data, int[] dataLength, ZegoVideoFrameParam param, ZegoVideoFlipMode flipMode, ZegoPublishChannel channel) {
        super.onCapturedVideoFrameRawData(data, dataLength, param, flipMode, channel);

        if(param.format == ZegoVideoFrameFormat.RGBA32)
        {
            RenderConfig renderConfig = new RenderConfig();
            renderConfig.frameParam = param;
            renderConfig.filpMode = flipMode;
            renderConfig.buffer = ByteBuffer.allocateDirect(data[0].remaining());

//            renderConfig.buffer.order(data[0].order());
//            data[0].get(renderConfig.buffer,0,data[0].capacity());
            int sourceP = data[0].position();
            int sourceL = data[0].limit();
            renderConfig.buffer.put(data[0]);
            renderConfig.buffer.flip();
            data[0].position();
            data[0].limit(sourceL);
            data[0].position(sourceP);
            dataQueue.add(renderConfig);
        }
    }

    @Override
    public void onRemoteVideoFrameRawData(ByteBuffer[] data, int[] dataLength, ZegoVideoFrameParam param, String streamID) {
        super.onRemoteVideoFrameRawData(data, dataLength, param, streamID);

        if(param.format == ZegoVideoFrameFormat.RGBA32)
        {
            RenderConfig renderConfig = new RenderConfig();
            renderConfig.frameParam = param;
            int sourceP = data[0].position();
            int sourceL = data[0].limit();
            renderConfig.buffer = ByteBuffer.allocateDirect(data[0].remaining());
            renderConfig.buffer.put(data[0]);
            renderConfig.buffer.flip();

            data[0].limit(sourceL);
            data[0].position(sourceP);
            if(streamID.equals(playStream1)){
                dataQueuePlay1.add(renderConfig);
            }
            if(streamID.equals(playStream2)){
                dataQueuePlay2.add(renderConfig);
            }
        }
    }

    public void setPlayView1(View view){
        this.playView1 = (TextureView)view;
    }

    public void setPlayView2(View view) {
        this.playView2 = (SurfaceView) view;
    }

    public void setPlayStream1(String stream){
        this.playStream1 = stream;
    }

    public void setPlayStream2(String stream) {
        this.playStream2 = stream;
    }

    public void setPreviewView(View view){
        this.previewView =  (TextureView) view;
    }

    public void EnablePreview(boolean enable){
        this.isPreview = enable;
    }
}
