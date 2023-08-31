package im.zego.others.multiplerooms;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import im.zego.others.R;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import im.zego.commontools.logtools.AppLogger;
import im.zego.commontools.logtools.LogView;
import im.zego.commontools.logtools.logLinearLayout;
import im.zego.commontools.uitools.ZegoViewUtil;
import im.zego.keycenter.KeyCenter;
import im.zego.keycenter.UserIDHelper;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoApiCalledEventHandler;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.constants.ZegoPlayerState;
import im.zego.zegoexpress.constants.ZegoPublishChannel;
import im.zego.zegoexpress.constants.ZegoPublisherState;
import im.zego.zegoexpress.constants.ZegoRoomMode;
import im.zego.zegoexpress.constants.ZegoRoomState;
import im.zego.zegoexpress.constants.ZegoRoomStateChangedReason;
import im.zego.zegoexpress.constants.ZegoScenario;
import im.zego.zegoexpress.constants.ZegoUpdateType;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoEngineProfile;
import im.zego.zegoexpress.entity.ZegoPlayerConfig;
import im.zego.zegoexpress.entity.ZegoPublisherConfig;
import im.zego.zegoexpress.entity.ZegoRoomConfig;
import im.zego.zegoexpress.entity.ZegoStream;
import im.zego.zegoexpress.entity.ZegoUser;

public class MultipleRoomsActivity extends AppCompatActivity {

    TextView userIDText;
    EditText roomID1Edit;
    EditText roomID2Edit;
    Button loginRoom1Button;
    Button loginRoom2Button;
    EditText publishRoomIDEdit;
    EditText publishStreamIDEdit;
    EditText playRoomIDEdit;
    EditText playStreamIDEdit;
    Button startPublishingButton;
    Button stopPublishingButton;
    Button startPlayingButton;
    Button stopPlayingButton;

    Button room1UserListButton;
    Button room2UserListButton;
    Button room1StreamListButton;
    Button room2StreamListButton;
    HashMap<String ,ZegoStream> room1Streams = new HashMap<>();
    HashMap<String ,ZegoStream> room2Streams = new HashMap<>();
    HashMap<String ,ZegoUser> room1Users = new HashMap<>();
    HashMap<String ,ZegoUser> room2Users = new HashMap<>();

    TextView roomState1;
    TextView roomState2;
    TextureView preview;
    TextureView playView;
    ZegoRoomConfig roomConfig = new ZegoRoomConfig();

    String userID;
    String publishStreamID;
    String playStreamID;
    String roomID1;
    String roomID2;
    ZegoExpressEngine engine;
    Long appID;
    String appSign;
    ZegoUser user;

    // Store whether the user is publishing the stream
    Boolean isPublish = false;
    //Store whether the user is playing local media
    Boolean isPlay = false;

    //Store whether the room is login
    Boolean isLoginRoomID1 = false;
    //Store whether the room is login
    Boolean isLoginRoomID2 = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiple_rooms);
        bindView();
        getAppIDAndUserIDAndAppSign();
        setDefaultValue();
        initEngineAndUser();
        setLogComponent();
        setEventHandler();
        setApiCalledResult();
        setLoginRoom1ButtonEvent();
        setLoginRoom2ButtonEvent();
        setStartPlayButtonEvent();
        setStartPublishButtonEvent();
        setUserListButtonEvent();
        setStreamListButtonEvent();
    }
    public void bindView(){
        userIDText = findViewById(R.id.userIDText);
        roomID1Edit = findViewById(R.id.room1IDEdit);
        roomID2Edit = findViewById(R.id.room2IDEdit);
        publishRoomIDEdit = findViewById(R.id.publishRoomIDEdit);
        publishStreamIDEdit = findViewById(R.id.publishStreamIDEdit);
        loginRoom1Button = findViewById(R.id.loginRoom1Button);
        loginRoom2Button = findViewById(R.id.loginRoom2Button);
        playRoomIDEdit = findViewById(R.id.playRoomIDEdit);
        playStreamIDEdit = findViewById(R.id.playStreamIDEdit);
        startPublishingButton = findViewById(R.id.startPublishButton);
        stopPublishingButton = findViewById(R.id.stopPublishButton);
        startPlayingButton = findViewById(R.id.startPlayButton);
        stopPlayingButton = findViewById(R.id.stopPlayButton);

        room1UserListButton = findViewById(R.id.room1UserListButton);
        room2UserListButton = findViewById(R.id.room2UserListButton);
        room1StreamListButton = findViewById(R.id.room1StreamListButton);
        room2StreamListButton = findViewById(R.id.room2StreamListButton);

        roomState1 = findViewById(R.id.roomState1);
        roomState2 = findViewById(R.id.roomState2);
        preview = findViewById(R.id.PreviewView);
        playView = findViewById(R.id.PlayView);
    }
    public void setDefaultValue(){
        roomID1 = "00291";
        roomID2 = "00292";
        publishStreamID = "0029";
        playStreamID = "0029";
        // Whether to enable the user in and out of the room callback notification [onRoomUserUpdate],the default is off.
        // If developers need to use ZEGO Room user notifications, make sure that each user who login sets this flag to true
        roomConfig.isUserStatusNotify = true;

        setTitle(getString(R.string.multiple_rooms));
        userIDText.setText(userID);
        roomID1Edit.setText(roomID1);
        roomID2Edit.setText(roomID2);
        publishRoomIDEdit.setText(roomID1);
        ZegoViewUtil.UpdateRoomState(roomState1, ZegoRoomStateChangedReason.LOGOUT);
        ZegoViewUtil.UpdateRoomState(roomState2, ZegoRoomStateChangedReason.LOGOUT);
    }
    //get appID and userID and appSign
    public void getAppIDAndUserIDAndAppSign(){
        appID = KeyCenter.getInstance().getAppID();
        userID = UserIDHelper.getInstance().getUserID();
        appSign = KeyCenter.getInstance().getAppSign();
    }
    public void initEngineAndUser(){
        // If use multi room, must invoke "setRoomMode" before create engine
        ZegoExpressEngine.setRoomMode(ZegoRoomMode.MULTI_ROOM);
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
    public void setLoginRoom1ButtonEvent(){
        loginRoom1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isLoginRoomID1) {
                    roomID1 = roomID1Edit.getText().toString();
                    engine.logoutRoom(roomID1);
                    AppLogger.getInstance().callApi("logout Room1:%s",roomID1);
                    loginRoom1Button.setText("Login room 1");
                    isLoginRoomID1 = false;
                } else {
                    roomID1 = roomID1Edit.getText().toString();
                    engine.loginRoom(roomID1,user,roomConfig);
                    AppLogger.getInstance().callApi("login Room1:%s",roomID1);
                    loginRoom1Button.setText("Logout room 1");
                    isLoginRoomID1 = true;
                }
            }
        });
    }
    public void setLoginRoom2ButtonEvent(){
        loginRoom2Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isLoginRoomID2) {
                    roomID2 = roomID2Edit.getText().toString();
                    engine.logoutRoom(roomID2);
                    AppLogger.getInstance().callApi("logout Room2:%s",roomID2);
                    loginRoom2Button.setText("Login room 2");
                    isLoginRoomID2 = false;
                } else {
                    roomID2 = roomID2Edit.getText().toString();
                    engine.loginRoom(roomID2,user,roomConfig);
                    AppLogger.getInstance().callApi("login Room2:%s",roomID2);
                    loginRoom2Button.setText("Logout room 2");
                    isLoginRoomID2 = true;
                }
            }
        });
    }
    public void setStartPublishButtonEvent(){
        startPublishingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                publishStreamID = publishStreamIDEdit.getText().toString();
                engine.startPreview(new ZegoCanvas(preview));
                ZegoPublisherConfig config = new ZegoPublisherConfig();
                config.roomID = publishRoomIDEdit.getText().toString();
                engine.startPublishingStream(publishStreamID, config, ZegoPublishChannel.MAIN);
                AppLogger.getInstance().callApi("Start Publishing Stream:%s, Room:%s", publishStreamID, config.roomID);
                isPublish = true;
            }
        });
        stopPublishingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                publishStreamID = publishStreamIDEdit.getText().toString();
                engine.stopPreview();
                engine.stopPublishingStream();
                AppLogger.getInstance().callApi("Stop Publishing Stream:%s",publishStreamID);
                isPublish = false;
            }
        });
    }
    public void setStartPlayButtonEvent(){
        startPlayingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playStreamID = playStreamIDEdit.getText().toString();
                ZegoPlayerConfig config = new ZegoPlayerConfig();
                config.roomID = playRoomIDEdit.getText().toString();
                engine.startPlayingStream(playStreamID, new ZegoCanvas(playView), config);
                AppLogger.getInstance().callApi("Start Playing Stream:%s, Room:%s", playStreamID, config.roomID);
                isPlay = true;
            }
        });
        stopPlayingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playStreamID = playStreamIDEdit.getText().toString();
                engine.stopPlayingStream(playStreamID);
                AppLogger.getInstance().callApi("Stop Playing Stream:%s",playStreamID);
                isPlay = false;
            }
        });
    }

    public void setUserListButtonEvent(){
        room1UserListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ListDialog.Builder Builder = new ListDialog.Builder(MultipleRoomsActivity.this);
                Builder.setTitle("Room1 UserList");
                Builder.setUserListString(room1Users);
                ListDialog dialog = Builder.create();
                Builder.refresh.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //update the stream list and publish status
                        Builder.setUserListString(room1Users);
                        //update the view
                        Builder.refresh();
                    }
                });
                dialog.show();
            }
        });

        room2UserListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ListDialog.Builder Builder = new ListDialog.Builder(MultipleRoomsActivity.this);
                Builder.setTitle("Room2 UserList");
                Builder.setUserListString(room2Users);
                ListDialog dialog = Builder.create();
                Builder.refresh.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //update the stream list and publish status
                        Builder.setUserListString(room2Users);
                        //update the view
                        Builder.refresh();
                    }
                });
                dialog.show();
            }
        });
    }
    public void setStreamListButtonEvent(){
        room1StreamListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ListDialog.Builder Builder = new ListDialog.Builder(MultipleRoomsActivity.this);
                Builder.setTitle("Room1 StreamList");
                Builder.setStreamListString(room1Streams);
                ListDialog dialog = Builder.create();
                Builder.refresh.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //update the stream list and publish status
                        Builder.setStreamListString(room1Streams);
                        //update the view
                        Builder.refresh();
                    }
                });
                dialog.show();
            }
        });

        room2StreamListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ListDialog.Builder Builder = new ListDialog.Builder(MultipleRoomsActivity.this);
                Builder.setTitle("Room2 StreamList");
                Builder.setStreamListString(room2Streams);
                ListDialog dialog = Builder.create();
                Builder.refresh.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //update the stream list and publish status
                        Builder.setStreamListString(room2Streams);
                        //update the view
                        Builder.refresh();
                    }
                });
                dialog.show();
            }
        });
    }

    public void setEventHandler(){
        engine.setEventHandler(new IZegoEventHandler() {
            // The callback triggered when the room connection state changes.
            @Override
            public void onRoomStateChanged(String roomID, ZegoRoomStateChangedReason reason, int errorCode, JSONObject extendedData) {
                if(roomID.equals(roomID1))
                {
                    ZegoViewUtil.UpdateRoomState(roomState1, reason);
                }
                else if(roomID.equals(roomID2))
                {
                    ZegoViewUtil.UpdateRoomState(roomState2, reason);
                }
                AppLogger.getInstance().receiveCallback("onRoomStateChanged: roomID = %s, reason = %s",roomID, reason.toString());
            }

            @Override
            public void onRoomStateUpdate(String roomID, ZegoRoomState state, int errorCode, JSONObject extendedData) {
                if (roomID.equals(roomID1) && state != ZegoRoomState.DISCONNECTED) {
                    roomID1Edit.setEnabled(false);
                    if (state == ZegoRoomState.CONNECTED) {
                        room1Users.put(user.userID, user);
                    }
                }
                if (roomID.equals(roomID2) && state != ZegoRoomState.DISCONNECTED) {
                    roomID2Edit.setEnabled(false);
                    if (state == ZegoRoomState.CONNECTED) {
                        room2Users.put(user.userID, user);
                    }
                }

                if (state == ZegoRoomState.DISCONNECTED) {
                    if (roomID.equals(roomID1)) {
                        roomID1Edit.setEnabled(true);
                        room1Users.remove(user.userID);
                    }
                    if (roomID.equals(roomID2)) {
                        roomID2Edit.setEnabled(true);
                        room2Users.remove(user.userID);
                    }
                }
                updateRoom1UserButtonText();
                updateRoom2UserButtonText();
            }

            // The callback triggered when the number of other users in the room increases or decreases.
            @Override
            public void onRoomUserUpdate(String roomID, ZegoUpdateType updateType, ArrayList<ZegoUser> userList) {
                super.onRoomUserUpdate(roomID, updateType, userList);
                if (roomID1Edit.getText().toString().equals(roomID)) {
                    if (updateType.equals(ZegoUpdateType.ADD)) {
                        for (ZegoUser user : userList) {
                            AppLogger.getInstance().receiveCallback("[onRoomUserUpdate] [roomID = %s] Add user [userID = %s]",roomID, user.userID);
                            room1Users.put(user.userID, user);
                        }
                    } else {
                        for (ZegoUser user : userList) {
                            AppLogger.getInstance().receiveCallback("[onRoomUserUpdate] [roomID = %s] Delete user [userID = %s]",roomID, user.userID);
                            room1Users.remove(user.userID);
                        }
                    }
                    updateRoom1UserButtonText();
                } else if (roomID2Edit.getText().toString().equals(roomID)) {
                    if (updateType.equals(ZegoUpdateType.ADD)) {
                        for (ZegoUser user : userList) {
                            AppLogger.getInstance().receiveCallback("[onRoomUserUpdate] [roomID = %s] Add user [userID = %s]",roomID, user.userID);
                            room2Users.put(user.userID, user);
                        }
                    } else {
                        for (ZegoUser user : userList) {
                            AppLogger.getInstance().receiveCallback("[onRoomUserUpdate] [roomID = %s] Delete user [userID = %s]",roomID, user.userID);
                            room2Users.remove(user.userID);
                        }
                    }
                    updateRoom2UserButtonText();
                }
            }

            // The callback triggered when the number of streams published by the other users in the same room increases or decreases.
            @Override
            public void onRoomStreamUpdate(String roomID, ZegoUpdateType updateType, ArrayList<ZegoStream> streamList, JSONObject extendedData) {
                super.onRoomStreamUpdate(roomID, updateType, streamList, extendedData);
                if (roomID1Edit.getText().toString().equals(roomID)) {
                    if (updateType.equals(ZegoUpdateType.ADD)){
                        for (ZegoStream stream:streamList){
                            AppLogger.getInstance().receiveCallback("[onRoomStreamUpdate] [roomID = %s] Add stream [streamID = %s]",roomID,stream.streamID);
                            room1Streams.put(stream.streamID, stream);
                        }
                    } else {
                        for (ZegoStream stream:streamList){
                            AppLogger.getInstance().receiveCallback("[onRoomStreamUpdate] [roomID = %s] Delete stream [streamID = %s]",roomID,stream.streamID);
                            room1Streams.remove(stream.streamID);
                        }
                    }
                    updateRoom1StreamButtonText();
                } else if (roomID2Edit.getText().toString().equals(roomID)) {
                    if (updateType.equals(ZegoUpdateType.ADD)){
                        for (ZegoStream stream:streamList){
                            AppLogger.getInstance().receiveCallback("[onRoomStreamUpdate] [roomID = %s] Add stream [streamID = %s]",roomID, stream.streamID);
                            room2Streams.put(stream.streamID, stream);
                        }
                    } else {
                        for (ZegoStream stream:streamList){
                            AppLogger.getInstance().receiveCallback("[onRoomStreamUpdate] [roomID = %s] Delete stream [streamID = %s]",roomID,stream.streamID);
                            room2Streams.remove(stream.streamID);
                        }
                    }
                    updateRoom2StreamButtonText();
                }
            }

            // The callback triggered when the state of stream publishing changes.
            @Override
            public void onPublisherStateUpdate(String streamID, ZegoPublisherState state, int errorCode, JSONObject extendedData) {
                super.onPublisherStateUpdate(streamID, state, errorCode, extendedData);
                // If the state is PUBLISHER_STATE_NO_PUBLISH and the errcode is not 0, it means that stream publishing has failed
                // and no more retry will be attempted by the engine. At this point, the failure of stream publishing can be indicated
                // on the UI of the App.
                if(state.equals(ZegoPublisherState.NO_PUBLISH)) {
                    room1Streams.remove(streamID);
                    room2Streams.remove(streamID);
//                    if (isPublish) {
//                        startPublishingButton.setText(GetEmojiStringByUnicode(crossEmoji) + getString(R.string.stop_publishing));
//                    }
                } else if (errorCode == 0 && state.equals(ZegoPublisherState.PUBLISHING)) {
                    String tmp = publishRoomIDEdit.getText().toString();
                    if (publishRoomIDEdit.getText().toString().equals(roomID1)) {
                        ZegoStream stream = new ZegoStream();
                        stream.streamID = streamID;
                        stream.user = user;
                        room1Streams.put(streamID, stream);
                    } else if (publishRoomIDEdit.getText().toString().equals(roomID2)) {
                        ZegoStream stream = new ZegoStream();
                        stream.streamID = streamID;
                        stream.user = user;
                        room2Streams.put(streamID, stream);
                    }
                }
                updateRoom1StreamButtonText();
                updateRoom2StreamButtonText();
//                    if (isPublish) {
//                        startPublishingButton.setText(GetEmojiStringByUnicode(checkEmoji) + getString(R.string.stop_publishing));
//                    }

            }
            // The callback triggered when the state of stream playing changes.
            @Override
            public void onPlayerStateUpdate(String streamID, ZegoPlayerState state, int errorCode, JSONObject extendedData) {
                super.onPlayerStateUpdate(streamID, state, errorCode, extendedData);
                // If the state is PLAYER_STATE_NO_PLAY and the errcode is not 0, it means that stream playing has failed and
                // no more retry will be attempted by the engine. At this point, the failure of stream playing can be indicated
                // on the UI of the App.
                if(errorCode != 0 && state.equals(ZegoPlayerState.NO_PLAY)) {
//                    if (isPlay) {
//                        startPlayingButton.setText(GetEmojiStringByUnicode(crossEmoji) + getString(R.string.stop_playing));
//                    }
                } else {
//                    if (isPlay) {
//                        startPlayingButton.setText(GetEmojiStringByUnicode(checkEmoji) + getString(R.string.stop_playing));
//                    }
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
    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, MultipleRoomsActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        ZegoExpressEngine.destroyEngine(null);
        // Restore room mode to single room
        ZegoExpressEngine.setRoomMode(ZegoRoomMode.SINGLE_ROOM);
        super.onDestroy();
    }

    private void updateRoom1UserButtonText() {
        room1UserListButton.setText(String.format("Room1 Users(%d)",room1Users.size()));
    }

    private void updateRoom2UserButtonText() {
        room2UserListButton.setText(String.format("Room2 Users(%d)",room2Users.size()));
    }

    private void updateRoom1StreamButtonText() {
        room1StreamListButton.setText(String.format("Room1 Streams(%d)",room1Streams.size()));
    }

    private void updateRoom2StreamButtonText() {
        room2StreamListButton.setText(String.format("Room2 Streams(%d)",room2Streams.size()));
    }
}