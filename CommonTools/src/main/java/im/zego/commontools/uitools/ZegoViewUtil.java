package im.zego.commontools.uitools;
import im.zego.zegoexpress.constants.ZegoRoomStateChangedReason;

import android.widget.TextView;

public class ZegoViewUtil {
    public static final int checkEmoji = 0x2705;               //check mark button
    public static final int crossEmoji = 0x274c;               //cross mark
    public static final int redQuestionEmoji = 0x2753;         //red question mark;
    public static final int blueCircleEmoji = 0x1F535;         //blue circle
    public static final int redCircleEmoji = 0x1F534;          //red circle


    public static final int roomLoginingEmoji = 0x2B55;        //hollow red circle
    public static final int roomLoginedEmoji = 0x1F535;        //blue circle
    public static final int roomLoginFailedEmoji = 0x1F534;    //red circle
    public static final int roomLogoutEmoji = 0x26AA;          //white circle
    public static final int roomLogoutFailedEmoji = 0x26AA;    //red circle
    public static final int roomReconnectingEmoji = 0x2B55;    //hollow red circle
    public static final int roomReconnectedEmoji = 0x1F535;    //blue circle
    public static final int roomReconnectFailedEmoji = 0x1F534;//red circle
    public static final int roomKickoutEmoji = 0x1F534;        //red circle


    public static void UpdateRoomState(TextView view, ZegoRoomStateChangedReason reason)
    {
        switch(reason)
        {
            case LOGINING:
            {
                view.setText(new String(Character.toChars(roomLoginingEmoji)));
            }break;
            case LOGINED:
            {
                view.setText(new String(Character.toChars(roomLoginedEmoji)));
            }break;
            case LOGIN_FAILED:
            {
                view.setText(new String(Character.toChars(roomLoginFailedEmoji)));
            }break;
            case LOGOUT:
            {
                view.setText(new String(Character.toChars(roomLogoutEmoji)));
            }break;
            case LOGOUT_FAILED:
            {
                view.setText(new String(Character.toChars(roomLogoutFailedEmoji)));
            }break;
            case RECONNECTING:
            {
                view.setText(new String(Character.toChars(roomReconnectingEmoji)));
            }break;
            case RECONNECTED:
            {
                view.setText(new String(Character.toChars(roomReconnectedEmoji)));
            }break;
            case RECONNECT_FAILED:
            {
                view.setText(new String(Character.toChars(roomReconnectFailedEmoji)));
            }break;
            case KICK_OUT:
            {
                view.setText(new String(Character.toChars(roomKickoutEmoji)));
            }break;
            default:
            {
                view.setText(new String(Character.toChars(redQuestionEmoji)));
            }break;
        }
    }

    public static String GetEmojiStringByUnicode(int unicode){
        return new String(Character.toChars(unicode));
    }
}
