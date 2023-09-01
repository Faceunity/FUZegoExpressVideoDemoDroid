package im.zego.keycenter;

public final class KeyCenter {

    // Developers can get appID from admin console.
    // https://console.zego.im/dashboard
    // for example: 123456789L;
    private long _appID = ;

    // AppSign only meets simple authentication requirements.
    // If you need to upgrade to a more secure authentication method,
    // please refer to [Guide for upgrading the authentication mode from using the AppSign to Token](https://docs.zegocloud.com/faq/token_upgrade)
    // Developers can get AppSign from admin [console](https://console.zego.im/dashboard)
    // for example: "abcdefghijklmnopqrstuvwxyz0123456789abcdegfhijklmnopqrstuvwxyz01";
    private String _appSign = "";

    private static KeyCenter instance = new KeyCenter();
    private KeyCenter() {}

    public static KeyCenter getInstance() {
        return instance;
    }

    public long getAppID() {
        return _appID;
    }

    public void setAppID(long appID) {
        _appID = appID;
    }

    public String getAppSign() {
        return _appSign;
    }

    public void setAppSign(String appSign) {
        _appSign = appSign;
    }

}
