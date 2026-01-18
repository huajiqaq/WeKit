package moe.ouom.wekit.config;

import android.app.Activity;
import java.lang.ref.WeakReference;

public class CacheConfig {

    private CacheConfig() {
        throw new AssertionError("No instance for you!");
    }

    private static WeakReference<Activity> launcherUIActivityRef;

    // account info //

    // login_weixin_username: wxid_apfe8lfoeoad13
    // last_login_nick_name: 帽子叔叔
    // login_user_name: 15068586147
    // last_login_uin: 1293948946
    private static String login_weixin_username;
    private static String last_login_nick_name;
    private static String login_user_name;
    private static String last_login_uin;

    // ------- //


    // wechat app info //

    private static String wechatVersionName; // "8.0.65"
    private static long wechatVersionCode;    // 2960

    // ------- //

    public static void setLauncherUIActivity(Activity activity) {
        // 存入时包装成弱引用
        launcherUIActivityRef = new WeakReference<>(activity);
    }

    public static Activity getLauncherUIActivity() {
        if (launcherUIActivityRef == null) {
            return null;
        }
        return launcherUIActivityRef.get();
    }

    public static String getLogin_weixin_username() {
        return login_weixin_username;
    }

    public static void setLogin_weixin_username(String login_weixin_username) {
        CacheConfig.login_weixin_username = login_weixin_username;
    }

    public static String getLast_login_nick_name() {
        return last_login_nick_name;
    }

    public static void setLast_login_nick_name(String last_login_nick_name) {
        CacheConfig.last_login_nick_name = last_login_nick_name;
    }

    public static String getLogin_user_name() {
        return login_user_name;
    }

    public static void setLogin_user_name(String login_user_name) {
        CacheConfig.login_user_name = login_user_name;
    }

    public static String getLast_login_uin() {
        return last_login_uin;
    }

    public static void setLast_login_uin(String last_login_uin) {
        CacheConfig.last_login_uin = last_login_uin;
    }

    public static String getWechatVersionName() {
        return wechatVersionName;
    }

    public static void setWechatVersionName(String wechatVersionName) {
        CacheConfig.wechatVersionName = wechatVersionName;
    }

    public static long getWechatVersionCode() {
        return wechatVersionCode;
    }

    public static void setWechatVersionCode(long wechatVersionCode) {
        CacheConfig.wechatVersionCode = wechatVersionCode;
    }
}