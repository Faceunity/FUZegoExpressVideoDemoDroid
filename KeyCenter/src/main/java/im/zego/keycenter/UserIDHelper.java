package im.zego.keycenter;

import android.os.Build;

import java.util.Random;

public final class UserIDHelper {

    private static UserIDHelper instance = new UserIDHelper();
    private UserIDHelper() {}

    public static UserIDHelper getInstance() {
        return instance;
    }

    private String _userID = "";

    public String getUserID() {
        if (_userID.isEmpty()) {
            Random random = new Random();
            _userID = ("Android_" + Build.MODEL).replaceAll(" ", "_") + "_" + random.nextInt(1000);
        }
        return _userID;
    }

    public void setUserID(String userID) {
        _userID = userID;
    }
}
