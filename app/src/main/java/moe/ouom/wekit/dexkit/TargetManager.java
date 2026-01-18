package moe.ouom.wekit.dexkit;

import static moe.ouom.wekit.config.ConfigManager.cGetBoolean;
import static moe.ouom.wekit.config.ConfigManager.cGetString;
import static moe.ouom.wekit.config.ConfigManager.cPutBoolean;
import static moe.ouom.wekit.config.ConfigManager.cPutString;
import static moe.ouom.wekit.util.Initiator.loadClass;
import static moe.ouom.wekit.util.common.Utils.findMethodByName;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;

import moe.ouom.wekit.config.ConfigManager;
import moe.ouom.wekit.util.log.Logger;

public class TargetManager {

    /* =========================================================
     *  Config helpers
     * ========================================================= */

    public static boolean isNeedFindTarget() {
        return cGetBoolean("isNeedFindTarget", true);
    }

    public static void setIsNeedFindTarget(boolean b) {
        cPutBoolean("isNeedFindTarget", b);
    }

    public static String getLastWeChatVersion() {
        return cGetString("LastWeChatVersion", "");
    }

    public static void setLastWeChatVersion(String version) {
        cPutString("LastWeChatVersion", version);
    }

    /* =========================================================
     *  Core: batch search in one DexKit execute()
     * ========================================================= */

    public static void runMethodFinder(ApplicationInfo ai, ClassLoader cl, Activity activity, OnTaskCompleteListener cb) {
        new Thread(() -> {
            DexKitExecutor executor = new DexKitExecutor(ai.sourceDir, cl);
            StringBuilder out = new StringBuilder();

            executor.execute((bridge, loader) -> {
//                // Methods
//                findAndCache(bridge, cl, MethodMatcher.create().usingStrings("rootVMBuild"), MethodCacheKey_AIOParam, out);
//                findAndCache(bridge, cl, MethodMatcher.create().usingStrings("findViewById(...)").usingStrings("inputRoot", "getContext(...)", "sendBtn", "binding"), MethodCacheKey_InputRoot, out);
//                findAndCache(bridge, cl, MethodMatcher.create().usingStrings("inputRoot.findViewById(R.id.send_btn)"), MethodCacheKey_InputRoot, out);
//                findAndCache(bridge, cl, MethodMatcher.create().usingStrings("AIOMarkdownContentComponent").usingStrings("bind status=").paramCount(2), MethodCacheKey_MarkdownAIO, out);
//                findAndCache(bridge, cl, MethodMatcher.create().usingStrings("getBuddyName()"), MethodCacheKey_getBuddyName, out);
//                findAndCache(bridge, cl, MethodMatcher.create().usingStrings("getDiscussionMemberShowName uin is null"), MethodCacheKey_getDiscussionMemberShowName, out);
//                findAndCache(bridge, cl, MethodMatcher.create().usingStrings("peerUid").usingStrings("panelCallback"), MethodCacheKey_ChatPanelBtn, out);
//
//                // Class
//                findAndCacheClass(bridge, cl,
//                        ClassMatcher.create().usingStrings("QQCustomMenuItem{title="),
//                        "com.tencent.qqnt.aio.menu.ui",
//                        ClazzCacheKey_AbstractQQCustomMenuItem,
//                        out);
            });

            out.append("\n\n\n... 搜索结果已缓存");
            String result = out.toString();
            activity.runOnUiThread(() -> {
                if (cb != null) cb.onTaskComplete(result);
            });
        }).start();
    }

    private static void findAndCache(DexKitBridge bridge, ClassLoader cl, MethodMatcher matcher, String key, StringBuilder log) {
        try {
            MethodData md = bridge.findMethod(FindMethod.create().matcher(matcher)).single();
            Method m = md.getMethodInstance(cl);
            String sig = m.getDeclaringClass().getName() + "#" + m.getName();
            cPutString(key, sig);
            log.append("\n").append(key).append("-> ").append(sig);
        } catch (Throwable t) {
            Logger.e(key, t);
        }
    }

    private static void findAndCacheClass(DexKitBridge bridge, ClassLoader cl, ClassMatcher matcher, String pkg, String key, StringBuilder log) {
        try {
            FindClass fc = FindClass.create().searchPackages(pkg).matcher(matcher);
            ClassData cd = bridge.findClass(fc).singleOrThrow(() -> new IllegalStateException("Non‑unique class"));
            Class<?> clazz = cd.getInstance(cl);
            cPutString(key, clazz.getName());
            log.append("\n").append(key).append("-> ").append(clazz.getName());
        } catch (Throwable t) {
            Logger.e(key, t);
        }
    }


    public static void removeAllMethodSignature() {
        ConfigManager cfg = ConfigManager.getCache();
        SharedPreferences.Editor e = cfg.edit();
        for (String k : cfg.getAll().keySet()) if (k.startsWith("method_")) e.remove(k);
        e.apply();
    }

    public static Method requireMethod(String key) {
        try {
            String[] p = cGetString(key, null).split("#");
            return findMethodByName(loadClass(p[0]), p[1]);
        } catch (Throwable t) {
            Logger.e(key, t);
            return null;
        }
    }

    public static Class<?> requireClazz(String key) {
        try {
            return loadClass(cGetString(key, null));
        } catch (Throwable t) {
            Logger.e(key, t);
            return null;
        }
    }

    public interface OnTaskCompleteListener {
        void onTaskComplete(String result);
    }
}
