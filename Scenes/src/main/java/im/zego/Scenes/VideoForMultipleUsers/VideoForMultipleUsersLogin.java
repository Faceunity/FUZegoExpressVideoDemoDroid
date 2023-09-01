package im.zego.Scenes.VideoForMultipleUsers;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONObject;

import im.zego.R;
import im.zego.commontools.logtools.AppLogger;
import im.zego.keycenter.KeyCenter;
import im.zego.keycenter.UserIDHelper;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoRoomLoginCallback;
import im.zego.zegoexpress.constants.ZegoScenario;
import im.zego.zegoexpress.constants.ZegoVideoConfigPreset;
import im.zego.zegoexpress.entity.ZegoEngineProfile;
import im.zego.zegoexpress.entity.ZegoRoomConfig;
import im.zego.zegoexpress.entity.ZegoUser;
import im.zego.zegoexpress.entity.ZegoVideoConfig;

public class VideoForMultipleUsersLogin extends AppCompatActivity {

    EditText editRoomID;
    EditText editUserID;
    EditText editUserName;
    Spinner encodeResolutionSpinner;
    EditText editFps;
    Spinner bitrateSpinner;
    Button loginRoomButton;

    ZegoExpressEngine engine;
    ZegoVideoConfig config;
    long appID;
    String userID;
    String appSign;
    String roomID = "0004";
    String userName;
    ZegoUser user;
    int fps;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_for_multiple_users_login);

        bindView();
        getAppIDAndUserIDAndAppSign();
        setDefaultValue();
        requestPermission();
        setEncodeResolutionSpinnerEvent();
        setBitrateSpinnerEvent();
        setLoginRoomButtonEvent();
    }
    public void bindView(){
        editRoomID = findViewById(R.id.editRoomID);
        editUserID = findViewById(R.id.editUserID);
        editUserName = findViewById(R.id.editUserName);
        encodeResolutionSpinner = findViewById(R.id.encodeResolutionSpinner);
        editFps = findViewById(R.id.editFps);
        bitrateSpinner = findViewById(R.id.bitrateSpinner);
        loginRoomButton = findViewById(R.id.loginRoomButton);
    }
    public void requestPermission() {
        String[] PERMISSIONS_STORAGE = {
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO"};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.CAMERA") != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, "android.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(PERMISSIONS_STORAGE, 101);
            }
        }
    }
    //get appID and userID and appSign
    public void getAppIDAndUserIDAndAppSign(){
        appID = KeyCenter.getInstance().getAppID();
        userID = UserIDHelper.getInstance().getUserID();
        appSign = KeyCenter.getInstance().getAppSign();
    }
    public void setDefaultValue(){
        userName = ("Android_" + Build.MODEL).replaceAll(" ", "_");
        //create the user
        user = new ZegoUser(userID, userName);
        // set default configuration
        config = new ZegoVideoConfig(ZegoVideoConfigPreset.PRESET_180P);
        editUserID.setText(userID);
        editUserID.setEnabled(false);
        editUserName.setText(userName);
        setTitle(getString(R.string.video_for_multiple_users));
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
    }
    public void setEncodeResolutionSpinnerEvent(){
        encodeResolutionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] option = getResources().getStringArray(R.array.resolution);
                switch (option[position]){
                    case "180x320":
                        config.setEncodeResolution(180,320);
                        break;
                    case "270x480":
                        config.setEncodeResolution(270,480);
                        break;
                    case "360x640":
                        config.setEncodeResolution(360,640);
                        break;
                    case "540x960":
                        config.setEncodeResolution(540,960);
                        break;
                    case "720x1280":
                        config.setEncodeResolution(720,1280);
                        break;
                    case "1080x1920":
                        config.setEncodeResolution(1080,1920);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
    public void setBitrateSpinnerEvent(){
        bitrateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] option = getResources().getStringArray(R.array.bitrate);
                switch (option[position]){
                    case "300kbps":
                        config.setVideoBitrate(300);
                        break;
                    case "400kbps":
                        config.setVideoBitrate(400);
                        break;
                    case "600kbps":
                        config.setVideoBitrate(600);
                        break;
                    case "1200kbps":
                        config.setVideoBitrate(1200);
                        break;
                    case "1500kbps":
                        config.setVideoBitrate(1500);
                        break;
                    case "3000kbps":
                        config.setVideoBitrate(3000);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
    public void setLoginRoomButtonEvent(){
        loginRoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userID = editUserID.getText().toString();
                if (userID.isEmpty()) {
                    Toast.makeText(VideoForMultipleUsersLogin.this, "userID cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                roomID = editRoomID.getText().toString();
                if (roomID.isEmpty()) {
                    Toast.makeText(VideoForMultipleUsersLogin.this, "roomID cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                userName = editUserName.getText().toString();
                if (editFps.getText().toString().equals(""))
                {
                    Toast.makeText(VideoForMultipleUsersLogin.this, "FPS cannot be Empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    fps = Integer.parseInt(editFps.getText().toString());
                } catch (NumberFormatException e)
                {
                    Toast.makeText(VideoForMultipleUsersLogin.this, "FPS is too large", Toast.LENGTH_SHORT).show();
                    return;
                }
                initEngineAndUser();

                config.setVideoFPS(fps);
                engine.setVideoConfig(config);
                loginRoom();
            }
        });
    }
    public void loginRoom(){
        //enable the camera
        engine.enableCamera(true);
        //enable the microphone
        engine.muteMicrophone(false);
        //enable the speaker
        engine.muteSpeaker(false);
        AppLogger.getInstance().callApi("Login Room:%s",roomID);
        Intent temp= new Intent(VideoForMultipleUsersLogin.this,VideoForMultipleUsersActivity.class);
        temp.putExtra("userName",userName);
        temp.putExtra("userID",userID);
        temp.putExtra("roomID",roomID);
        startActivity(temp);
    }
    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity,VideoForMultipleUsersLogin.class);
        activity.startActivity(intent);
    }
}