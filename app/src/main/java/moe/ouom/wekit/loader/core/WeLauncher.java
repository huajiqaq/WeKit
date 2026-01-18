package moe.ouom.wekit.loader.core;

import static moe.ouom.wekit.constants.Constants.WECHAT_LAUNCHER_UI;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import moe.ouom.wekit.config.CacheConfig;
import moe.ouom.wekit.constants.PackageConstants;
import moe.ouom.wekit.hooks._core.HookItemLoader;
import moe.ouom.wekit.loader.startup.StartupInfo;
import moe.ouom.wekit.util.Initiator;
import moe.ouom.wekit.util.common.ModuleRes;
import moe.ouom.wekit.util.common.SyncUtils;
import moe.ouom.wekit.util.log.Logger;

public class WeLauncher {
    public void init(@NonNull ClassLoader cl, @NonNull ApplicationInfo ai, @NonNull String modulePath, Context context) {
        Initiator.init(context.getClassLoader());

        HookItemLoader hookItemLoader = new HookItemLoader();
        hookItemLoader.loadHookItem(SyncUtils.getProcessType());


        try {
            PackageManager pm = context.getPackageManager();
            // 获取宿主包名的详细信息
            PackageInfo pInfo = pm.getPackageInfo(context.getPackageName(), 0);

            if (pInfo != null) {
                // 存入 CacheConfig
                CacheConfig.setWechatVersionName(pInfo.versionName);
                CacheConfig.setWechatVersionCode(pInfo.getLongVersionCode());

                Logger.i("WeChat Version cached: " + CacheConfig.getWechatVersionName() + " (" + CacheConfig.getWechatVersionCode() + ")");
            }
        } catch (Throwable e) {
            Logger.e("WeLauncher: Failed to load version info: " + e);
        }


        /////  com.tencent.mm.ui.LauncherUI   /////

        // onResume
        try {
            Class<?> launcherUIClass = XposedHelpers.findClass(WECHAT_LAUNCHER_UI, cl);

            XposedHelpers.findAndHookMethod(launcherUIClass, "onResume", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Activity activity = (Activity) param.thisObject;
                    CacheConfig.setLauncherUIActivity(activity);
                    ModuleRes.init(activity, PackageConstants.PACKAGE_NAME_SELF);
                }
            });

        } catch (Throwable e) {
            Logger.e("WeLauncher: Failed to hook LauncherUI: " + e);
        }

        // onCreate
        try {
            XposedHelpers.findAndHookMethod(XposedHelpers.findClass("com.tencent.mm.ui.LauncherUI", cl), "onCreate", Bundle.class, new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    SharedPreferences sharedPreferences = ((Activity) param.thisObject).getSharedPreferences("com.tencent.mm_preferences", 0);
                    String login_weixin_username = sharedPreferences.getString("login_weixin_username", "null");
                    String last_login_nick_name = sharedPreferences.getString("last_login_nick_name", "null");
                    String login_user_name = sharedPreferences.getString("login_user_name", "null");
                    String last_login_uin = sharedPreferences.getString("last_login_uin", "null");

                    // login_weixin_username: wxid_apfe8lfoeoad13
                    // last_login_nick_name: 帽子叔叔
                    // login_user_name: 150665766147
                    // last_login_uin: 1293948946
                    CacheConfig.setLogin_weixin_username(login_weixin_username);
                    CacheConfig.setLast_login_nick_name(last_login_nick_name);
                    CacheConfig.setLogin_user_name(login_user_name);
                    CacheConfig.setLast_login_uin(last_login_uin);
//                    Logger.d("login_weixin_username: " + login_weixin_username + "\nlast_login_nick_name: " + last_login_nick_name + "\nlogin_user_name: " + login_user_name + "\nlast_login_uin: " + last_login_uin);


                }
            });

        } catch (Throwable e) {
            Logger.e("WeLauncher: Failed to hook LauncherUI: " + e);
        }

        /////  -----------------------   /////
    }
}