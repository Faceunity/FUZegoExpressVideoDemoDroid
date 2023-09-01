package im.zego.others.streammixing;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import im.zego.others.R;

import org.json.JSONObject;

import java.util.ArrayList;

import im.zego.commontools.logtools.AppLogger;
import im.zego.commontools.logtools.LogView;
import im.zego.commontools.logtools.logLinearLayout;
import im.zego.commontools.uitools.ZegoViewUtil;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoMixerStartCallback;
import im.zego.zegoexpress.callback.IZegoMixerStopCallback;
import im.zego.zegoexpress.constants.ZegoMixerInputContentType;
import im.zego.zegoexpress.entity.ZegoAudioConfig;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoMixerAudioConfig;
import im.zego.zegoexpress.entity.ZegoMixerInput;
import im.zego.zegoexpress.entity.ZegoMixerOutput;
import im.zego.zegoexpress.entity.ZegoMixerTask;
import im.zego.zegoexpress.entity.ZegoMixerVideoConfig;
import im.zego.zegoexpress.entity.ZegoMixerWhiteboard;
import im.zego.zegoexpress.entity.ZegoWatermark;
import im.zego.zegoexpress.entity.ZegoStream;

public class MixerStartActivity extends AppCompatActivity implements IMixerStreamUpdateHandler {

    private static ArrayList<CheckBox> checkBoxList=new ArrayList<CheckBox>();
    private static LinearLayout ll_checkBoxList;
    private static String mixStreamID = "mix_0025";

    private EditText mixerImageUri1;
    private EditText mixerImageUri2;
    private EditText mixerWhiteboardID;
    private EditText mixerWhiteboardLayoutLeft;
    private EditText mixerWhiteboardLayoutTop;
    private EditText mixerWhiteboardLayoutRight;
    private EditText mixerWhiteboardLayoutBottom;
    private EditText mixerWhiteboardLayoutZOrder;
    private CheckBox mixerIsPPTAnimation;

    private ZegoMixerTask mMixerTask;
    private long mWhiteboardID = 0;

    // Unicode of Emoji
    int checkEmoji = 0x2705;
    int crossEmoji = 0x274c;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mixer_start);

        setLogComponent();
        setUI();

        MixerMainActivity.registerStreamUpdateNotify(this);
    }
    public void setUI(){
        ll_checkBoxList = findViewById(R.id.ll_CheckBoxList);
        TextView tv_room = findViewById(R.id.tv_room_id3);
        mixerImageUri1 = findViewById(R.id.mixerImageUrlEditText1);
        mixerImageUri2 = findViewById(R.id.mixerImageUrlEditText2);
        mixerWhiteboardID = findViewById(R.id.mixerWhiteboardIDEditText);
        mixerWhiteboardLayoutLeft = findViewById(R.id.mixerWhiteboardLeftEditText);
        mixerWhiteboardLayoutTop = findViewById(R.id.mixerWhiteboardTopEditText);
        mixerWhiteboardLayoutRight = findViewById(R.id.mixerWhiteboardRightEditText);
        mixerWhiteboardLayoutBottom = findViewById(R.id.mixerWhiteboardBottomEditText);
        mixerWhiteboardLayoutZOrder = findViewById(R.id.mixerWhiteboardZOrderEditText);
        mixerIsPPTAnimation = findViewById(R.id.mixerWhiteboardPPTAnimation);

        tv_room.setText(MixerMainActivity.roomID);
        ll_checkBoxList.removeAllViews();
        checkBoxList.clear();
        for(ZegoStream stream: MixerMainActivity.streamInfoList){
            CheckBox checkBox=(CheckBox) View.inflate(this, R.layout.checkbox, null);
            checkBox.setText(stream.streamID);
            ll_checkBoxList.addView(checkBox);
            checkBoxList.add(checkBox);
        }
        setTitle(getString(R.string.stream_mixing));
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        MixerMainActivity.registerStreamUpdateNotify(null);
    }

    public void ClickStartMix(View view) {
        int count = 0;
        String streamID_1 = "";
        String streamID_2 = "";
        for (int i = 0; i < checkBoxList.size(); i++) {
            if (checkBoxList.get(i).isChecked()) {
                count++;
                if (streamID_1.equals("")) {
                    streamID_1 = checkBoxList.get(i).getText().toString();
                }
                else if (streamID_2.equals("")){
                    streamID_2 = checkBoxList.get(i).getText().toString();
                }
            }
        }

        mMixerTask = new ZegoMixerTask(String.format("mix_%s", MixerMainActivity.roomID));
        ArrayList<ZegoMixerInput> inputList = new ArrayList<>();
        ZegoMixerInput input_1 = new ZegoMixerInput(streamID_1, ZegoMixerInputContentType.VIDEO, new Rect(0, 0, 360, 320));
        input_1.soundLevelID = 123;
        input_1.label.text = "zego";
        input_1.label.font.border = true;
        input_1.imageInfo.url = mixerImageUri1.getText().toString();
        if (!TextUtils.isEmpty(streamID_1)) {
            inputList.add(input_1);
        }

        ZegoMixerInput input_2 = new ZegoMixerInput(streamID_2, ZegoMixerInputContentType.VIDEO, new Rect(0, 320, 360, 640));
        input_2.soundLevelID = 1235;
        input_2.label.text = "zego";
        input_2.label.font.border = true;
        input_2.label.font.borderColor = 255;
        input_2.imageInfo.url = mixerImageUri2.getText().toString();
        if (!TextUtils.isEmpty(streamID_2)) {
            inputList.add(input_2);
        }
        if (inputList.size() == 0) {
            Toast.makeText(this, "insufficient input list(at least one)", Toast.LENGTH_SHORT).show();

            mMixerTask = null;

            return;
        }

        ArrayList<ZegoMixerOutput> outputList = new ArrayList<>();
        ZegoMixerOutput output = new ZegoMixerOutput(mixStreamID);
        ZegoMixerAudioConfig audioConfig = new ZegoMixerAudioConfig();
        ZegoMixerVideoConfig videoConfig = new ZegoMixerVideoConfig();
        mMixerTask.setVideoConfig(videoConfig);
        mMixerTask.setAudioConfig(audioConfig);
        outputList.add(output);
        mMixerTask.enableSoundLevel(true);
        mMixerTask.setInputList(inputList);
        mMixerTask.setOutputList(outputList);

        ZegoMixerWhiteboard whiteboard = getWhiteboardConfig();
        if (whiteboard != null) {
            mMixerTask.setWhiteboard(whiteboard);
        }

        ZegoWatermark watermark = new ZegoWatermark("preset-id://zegowp.png", new Rect(0,0,300,200));
        mMixerTask.setWatermark(watermark);

        mMixerTask.setBackgroundImageURL("preset-id://zegobg.png");

        MixerMainActivity.engine.startMixerTask(mMixerTask, new IZegoMixerStartCallback() {

            @Override
            public void onMixerStartResult(int errorCode, JSONObject var2) {
                AppLogger.getInstance().receiveCallback("onMixerStartResult: result = " + errorCode);
                if (errorCode != 0) {
                    String msg = getString(R.string.tx_mixer_start_fail) + errorCode;
                    Toast.makeText(MixerStartActivity.this, msg, Toast.LENGTH_SHORT).show();
                    Button step1 = findViewById(R.id.btn_mix_start_mix);
                    step1.setText(ZegoViewUtil.GetEmojiStringByUnicode(ZegoViewUtil.crossEmoji)+getString(R.string.tx_mixer_step1));
                }
                else {
                    AppLogger.getInstance().callApi("mixing task id:%s", mMixerTask.getTaskID());
                    String msg = getString(R.string.tx_mixer_start_ok);
                    Toast.makeText(MixerStartActivity.this, msg, Toast.LENGTH_SHORT).show();
                    Button step1 = findViewById(R.id.btn_mix_start_mix);
                    step1.setText(ZegoViewUtil.GetEmojiStringByUnicode(ZegoViewUtil.checkEmoji)+getString(R.string.tx_mixer_step1));
                }
            }
        });

        ZegoExpressEngine.getEngine().setAudioConfig(new ZegoAudioConfig());
        TextureView tv_play_mix = findViewById(R.id.tv_play_mix);
        ZegoCanvas canvas = new ZegoCanvas(tv_play_mix);
        MixerMainActivity.engine.startPlayingStream(mixStreamID, canvas);
    }

    public void ClickUpdateWhiteboardConfig(View view) {
        // 是否已开始混流
        if (mMixerTask == null) {
            AppLogger.getInstance().callApi("not start mixing");

            return;
        }

        // 仅更新混流配置白板信息
        long whiteboardID = 0;
        ZegoMixerWhiteboard whiteboard = getWhiteboardConfig();
        if (whiteboard != null)
        {
            whiteboardID = whiteboard.whiteboardID;
        }
        AppLogger.getInstance().callApi("update whiteboard config, whiteboard id:%d", whiteboardID);

        mMixerTask.setWhiteboard(whiteboard);
        MixerMainActivity.engine.startMixerTask(mMixerTask, null);
    }

    private ZegoMixerWhiteboard getWhiteboardConfig() {
        long whiteboardID = 0;
        String whiteboardIDText = mixerWhiteboardID.getText().toString();
        if (!TextUtils.isEmpty(whiteboardIDText)) {
            try {
                whiteboardID = Long.parseLong(whiteboardIDText);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        if (whiteboardID == 0) {
            return null;
        }

        int left = 0;
        String leftText = mixerWhiteboardLayoutLeft.getText().toString();
        if (!TextUtils.isEmpty(leftText)) {
            try {
                left = Integer.parseInt(leftText);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        int top = 0;
        String topText = mixerWhiteboardLayoutTop.getText().toString();
        if (!TextUtils.isEmpty(topText)) {
            try {
                top = Integer.parseInt(topText);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        int right = 0;
        String rightText = mixerWhiteboardLayoutRight.getText().toString();
        if (!TextUtils.isEmpty(rightText)) {
            try {
                right = Integer.parseInt(rightText);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        int bottom = 0;
        String bottomText = mixerWhiteboardLayoutBottom.getText().toString();
        if (!TextUtils.isEmpty(bottomText)) {
            try {
                bottom = Integer.parseInt(bottomText);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        int zOrder = 0;
        String zOrderText = mixerWhiteboardLayoutZOrder.getText().toString();
        if (!TextUtils.isEmpty(zOrderText)) {
            try {
                zOrder = Integer.parseInt(zOrderText);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        ZegoMixerWhiteboard whiteboard = new ZegoMixerWhiteboard();
        whiteboard.whiteboardID = whiteboardID;
        whiteboard.layout.left = left;
        whiteboard.layout.top = top;
        whiteboard.layout.right = right;
        whiteboard.layout.bottom = bottom;
        whiteboard.zOrder = zOrder;
        whiteboard.isPPTAnimation = mixerIsPPTAnimation.isChecked();

        return whiteboard;
    }

    public void onRoomStreamUpdate() {
        ll_checkBoxList.removeAllViews();
        checkBoxList.clear();
        for(ZegoStream stream: MixerMainActivity.streamInfoList){
            CheckBox checkBox=(CheckBox) View.inflate(this, R.layout.checkbox, null);
            checkBox.setText(stream.streamID);
            ll_checkBoxList.addView(checkBox);
            checkBoxList.add(checkBox);
        }
    }

    public void onAutoSoundLevelUpdate() {}

    public void ClickStopWatch(View view) {
        MixerMainActivity.engine.stopPlayingStream(mixStreamID);
        AppLogger.getInstance().callApi("Stop Playing Stream:",mixStreamID);
        Button step2 = findViewById(R.id.btn_mix_stopwatchstream);
        step2.setText(ZegoViewUtil.GetEmojiStringByUnicode(ZegoViewUtil.checkEmoji)+getString(R.string.tx_mixer_step2));
    }

    public void ClickStopMix(View view) {
        MixerMainActivity.engine.stopMixerTask(mMixerTask, new IZegoMixerStopCallback() {
            @Override
            public void onMixerStopResult(int i) {
                AppLogger.getInstance().receiveCallback("onMixerStartResult: result = " + i);
                if (i != 0) {
                    String msg = getString(R.string.tx_mixer_stop_fail) + i;
                    Toast.makeText(MixerStartActivity.this, msg, Toast.LENGTH_SHORT).show();
                    Button step3 = findViewById(R.id.btn_mix_stop_mix);
                    step3.setText(ZegoViewUtil.GetEmojiStringByUnicode(ZegoViewUtil.crossEmoji)+getString(R.string.tx_mixer_step3));
                }
                else {
                    String msg = getString(R.string.tx_mixer_stop_ok);
                    Toast.makeText(MixerStartActivity.this, msg, Toast.LENGTH_SHORT).show();
                    Button step1 = findViewById(R.id.btn_mix_stop_mix);
                    step1.setText(ZegoViewUtil.GetEmojiStringByUnicode(ZegoViewUtil.checkEmoji)+getString(R.string.tx_mixer_step3));
                }
            }
        });
        mMixerTask = null;
    }

    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, MixerStartActivity.class);
        activity.startActivity(intent);
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

    // Unicode
    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
