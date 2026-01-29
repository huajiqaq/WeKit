package moe.ouom.wekit.hooks.sdk.ui

import android.R
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.Menu
import android.view.MenuItem
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import moe.ouom.wekit.constants.Constants
import moe.ouom.wekit.core.dsl.dexMethod
import moe.ouom.wekit.core.model.ApiHookItem
import moe.ouom.wekit.dexkit.DexMethodDescriptor
import moe.ouom.wekit.dexkit.cache.DexCacheManager
import moe.ouom.wekit.dexkit.intf.IDexFind
import moe.ouom.wekit.hooks.core.annotation.HookItem
import moe.ouom.wekit.ui.CommonContextWrapper
import moe.ouom.wekit.ui.creator.dialog.MainSettingsDialog
import moe.ouom.wekit.util.common.SyncUtils
import moe.ouom.wekit.util.log.WeLogger
import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Method
import java.lang.reflect.Modifier

@SuppressLint("DiscouragedApi")
@HookItem(path = "设置模块入口")
class WeSettingInjector : ApiHookItem(), IDexFind {
    private val dexMethodSetKey by dexMethod()
    private val dexMethodSetTitle by dexMethod()
    private val dexMethodGetKey by dexMethod()
    private val dexMethodAddPref by dexMethod()

    companion object {
        private const val KEY_WEKIT_ENTRY = "wekit_settings_entry"
        private const val TITLE_WEKIT_ENTRY = "WeKit 设置"
        private const val MENU_ID_WEKIT = 11451419
        private const val CLS_PREFERENCE = "com.tencent.mm.ui.base.preference.Preference"
    }

    @SuppressLint("NonUniqueDexKitData")
    override fun dexFind(dexKit: DexKitBridge): Map<String, String> {
        val descriptors = mutableMapOf<String, String>()

        // 查找 Preference 类
        val prefClass = dexKit.findClass {
            matcher { className = CLS_PREFERENCE }
        }.singleOrNull() ?: run {
            WeLogger.e("WeSettingInjector: Preference 类未找到")
            return descriptors
        }

        // 查找 setKey 方法
        dexMethodSetKey.find(dexKit, allowMultiple = true, descriptors = descriptors) {
            searchPackages("com.tencent.mm.ui.base.preference")
            matcher {
                declaredClass = CLS_PREFERENCE
                returnType = "void"
                paramTypes("java.lang.String")
                usingStrings("Preference")
            }
        }

        // 查找 setTitle 方法
        val setTitleCandidates = prefClass.findMethod {
            matcher {
                returnType = "void"
                paramTypes("java.lang.CharSequence")
            }
        }
        if (setTitleCandidates.isNotEmpty()) {
            val target = setTitleCandidates.last()
            dexMethodSetTitle.setDescriptor(
                DexMethodDescriptor(
                    target.className,
                    target.methodName,
                    target.methodSign
                )
            )
            dexMethodSetTitle.getDescriptorString()?.let {
                descriptors[dexMethodSetTitle.key] = it
            }
        }

        // 查找 getKey 方法
        WeLogger.d("WeSettingInjector", "Searching for getKey method in ${prefClass.name}")
        val getKeyCandidates = prefClass.findMethod {
            matcher {
                paramCount = 0
                returnType = "java.lang.String"
            }
        }
        WeLogger.d("WeSettingInjector", "Found ${getKeyCandidates.size} String methods with 0 params: ${getKeyCandidates.map { it.name }}")

        val targetGetKey = getKeyCandidates.firstOrNull { it.name != "toString" }
        WeLogger.d("WeSettingInjector", "Selected getKey method: ${targetGetKey?.name}")

        if (targetGetKey != null) {
            dexMethodGetKey.setDescriptor(
                DexMethodDescriptor(
                    targetGetKey.className,
                    targetGetKey.methodName,
                    targetGetKey.methodSign
                )
            )
            dexMethodGetKey.getDescriptorString()?.let {
                descriptors[dexMethodGetKey.key] = it
                WeLogger.d("WeSettingInjector", "Successfully saved getKey descriptor: $it")
            }
        } else {
            WeLogger.e("WeSettingInjector", "Failed to find getKey method!")
        }

        // 查找 Adapter 类和 addPreference 方法
        val adapterClass = dexKit.findClass {
            searchPackages("com.tencent.mm.ui.base.preference")
            matcher {
                superClass = "android.widget.BaseAdapter"
                methods {
                    add {
                        modifiers = Modifier.PUBLIC
                        name = "getView"
                        paramCount = 3
                    }
                    add {
                        name = "<init>"
                        paramCount = 3
                    }
                }
            }
        }.singleOrNull()

        if (adapterClass != null) {
            dexMethodAddPref.find(dexKit, allowMultiple = true, descriptors = descriptors) {
                searchPackages("com.tencent.mm.ui.base.preference")
                matcher {
                    declaredClass = adapterClass.name
                    paramTypes(CLS_PREFERENCE, "int")
                    returnType = "void"
                }
            }
        }

        return descriptors
    }

    override fun entry(classLoader: ClassLoader) {
        // 尝试 Hook 旧版 UI
        tryHookLegacySettings(classLoader)

        // 尝试 Hook 新版 UI (8.0.67+)
        tryHookNewSettings(classLoader)
    }

    /**
     * 适配旧版 SettingsUI (基于 PreferenceScreen)
     */
    private fun tryHookLegacySettings(classLoader: ClassLoader) {
        try {
            // 检查类是否存在
            val clsSettingsUI = try {
                XposedHelpers.findClass(Constants.Companion.CLAZZ_SETTINGS_UI, classLoader)
            } catch (_: Throwable) {
                return // 类不存在，跳过
            }

            // 灰度测试，不再检查微信版本
//            if (requireMinWeChatVersion(MMVersion.MM_8_0_67)) {
//                return // 是新版，跳过
//            }

            val setKeyMethod = dexMethodSetKey.method
            val setTitleMethod = dexMethodSetTitle.method
            val getKeyMethod = dexMethodGetKey.method
            val addPrefMethod = dexMethodAddPref.method

            val mInitView = XposedHelpers.findMethodExact(
                clsSettingsUI,
                "initView",
                *arrayOf<Class<*>>()
            )

            hookAfter(mInitView) { param: XC_MethodHook.MethodHookParam ->
                val activity = param.thisObject as Activity
                WeLogger.i(
                    "WeSettingInjector",
                    "Legacy inject initView in process=${SyncUtils.getProcessName()}"
                )
                injectLegacyEntry(
                    activity,
                    classLoader,
                    setKeyMethod,
                    setTitleMethod,
                    getKeyMethod,
                    addPrefMethod,
                    "initView"
                )
            }

            hookAfter(clsSettingsUI, "onResume") { param ->
                val activity = param.thisObject as Activity
                activity.window?.decorView?.postDelayed({
                    injectLegacyEntry(
                        activity,
                        classLoader,
                        setKeyMethod,
                        setTitleMethod,
                        getKeyMethod,
                        addPrefMethod,
                        "onResume"
                    )
                }, 350L)
            }

            hookBefore(clsSettingsUI, "onPreferenceTreeClick") { param ->
                if (param.args.size < 2) return@hookBefore
                val preference = param.args[1] ?: return@hookBefore

                val key = try {
                    getKeyMethod.invoke(preference) as? String
                } catch (e: Throwable) {
                    WeLogger.e("WeSettingInjector: Failed to read preference key", e)
                    null
                }
                WeLogger.d("WeKit Debug: Click key = $key")

                if (KEY_WEKIT_ENTRY == key) {
                    val activity = param.thisObject as Activity

                    val fixContext = CommonContextWrapper.createAppCompatContext(activity)
                    val dialog = MainSettingsDialog(fixContext)
                    dialog.show()

                    param.result = true
                }
            }

            WeLogger.i("WeSettingInjector: Hooked onPreferenceTreeClick")

        } catch (t: Throwable) {
            WeLogger.e("Legacy Settings: Hook 流程异常", t)
            markCacheOutdated("legacy-hook-setup", t)
        }
    }

    /**
     * 适配新版 MainSettingsUI (基于 Menu 注入)
     * 因为新版 UI 继承结构复杂且混淆严重，通过 onCreateOptionsMenu 注入最稳定
     */
    private fun tryHookNewSettings(classLoader: ClassLoader) {
        try {
            // 检查新版 UI 是否存在，不存在则直接退出，不进行 Hook
            try {
                XposedHelpers.findClass(Constants.Companion.CLAZZ_MAIN_SETTINGS_UI, classLoader)
            } catch (_: Throwable) {
                return
            }

            // 获取基类 MMActivity
            val clsMMActivity = XposedHelpers.findClass(Constants.Companion.CLAZZ_MMActivity, classLoader)

            // ---------------------------------------------------------------------
            // Hook 基类 MMActivity 的 onCreateOptionsMenu
            // ---------------------------------------------------------------------
            val mOnCreateOptionsMenu = XposedHelpers.findMethodExact(
                clsMMActivity,
                "onCreateOptionsMenu",
                Menu::class.java
            )

            hookAfter(mOnCreateOptionsMenu) { param ->
                val activity = param.thisObject
                // 检查当前 Activity 实例的类名是否为 MainSettingsUI
                if (activity.javaClass.name == Constants.Companion.CLAZZ_MAIN_SETTINGS_UI) {
                    val menu = param.args[0] as? Menu ?: return@hookAfter
                    // 防止重复添加
                    if (menu.findItem(MENU_ID_WEKIT) == null) {
                        menu.add(0, MENU_ID_WEKIT, 0, TITLE_WEKIT_ENTRY)
                            .setIcon(R.drawable.ic_menu_preferences)
                        WeLogger.i("New Settings: Injected Menu entry into ${activity.javaClass.simpleName}")
                    }
                }
            }

            // ---------------------------------------------------------------------
            // Hook 基类 MMActivity 的 onOptionsItemSelected 以处理点击事件
            // ---------------------------------------------------------------------
            val mOnOptionsItemSelected = XposedHelpers.findMethodExact(
                clsMMActivity,
                "onOptionsItemSelected",
                MenuItem::class.java
            )

            hookBefore(mOnOptionsItemSelected) { param ->
                val activity = param.thisObject as Activity
                // 检查实例类型
                if (activity.javaClass.name == Constants.Companion.CLAZZ_MAIN_SETTINGS_UI) {
                    val item = param.args[0] as? MenuItem ?: return@hookBefore
                    if (item.itemId == MENU_ID_WEKIT) {
                        openSettingsDialog(activity)
                        param.result = true // 消费事件，阻止传递给微信处理
                    }
                }
            }

            WeLogger.i("New Settings: Hook setup complete (Targeting MMActivity)")

        } catch (t: Throwable) {
            WeLogger.e("New Settings: Hook 流程异常", t)
        }
    }

    private fun openSettingsDialog(activity: Activity) {
        try {
            val fixContext = CommonContextWrapper.createAppCompatContext(activity)
            val dialog = MainSettingsDialog(fixContext)
            dialog.show()
        } catch (e: Throwable) {
            WeLogger.e("Failed to open settings dialog", e)
        }
    }


    override fun unload(classLoader: ClassLoader) {}

    private fun injectLegacyEntry(
        activity: Activity,
        classLoader: ClassLoader,
        setKeyMethod: Method,
        setTitleMethod: Method,
        getKeyMethod: Method,
        addPrefMethod: Method,
        stage: String
    ): Boolean {
        return try {
            val clsIconPref = XposedHelpers.findClass(Constants.Companion.CLAZZ_ICON_PREFERENCE, classLoader)
            val prefInstance = XposedHelpers.newInstance(clsIconPref, activity as Context)

            setKeyMethod.invoke(prefInstance, KEY_WEKIT_ENTRY)
            setTitleMethod.invoke(prefInstance, TITLE_WEKIT_ENTRY)

            val prefScreen = XposedHelpers.callMethod(activity, "getPreferenceScreen")
                ?: throw IllegalStateException("PreferenceScreen is null")

            WeLogger.d(
                "WeSettingInjector",
                "Inject stage=$stage, prefScreen=${prefScreen.javaClass.name}, addPrefDecl=${addPrefMethod.declaringClass.name}"
            )

            if (hasExistingEntry(prefScreen)) {
                WeLogger.d("WeSettingInjector", "Entry already exists, skip inject")
                return true
            }

            val (receiver, method) = resolveAddPrefInvocation(activity, prefScreen, addPrefMethod, prefInstance)
                ?: throw IllegalStateException("Unable to resolve addPreference receiver/method")

            method.isAccessible = true
            method.invoke(receiver, prefInstance, 0)
            notifyDataSetChanged(receiver, prefScreen)

            WeLogger.i("WeSettingInjector", "Created WeKit setting (stage=$stage)")
            true
        } catch (e: Throwable) {
            WeLogger.e("WeSettingInjector: 插入选项失败", e)
            markCacheOutdated("legacy-inject", e)
            false
        }
    }

    private fun hasExistingEntry(prefScreen: Any): Boolean {
        val findPref = prefScreen.javaClass.methods.firstOrNull {
            it.name == "findPreference" &&
                it.parameterTypes.size == 1 &&
                it.parameterTypes[0] == String::class.java
        }
        return try {
            val result = findPref?.invoke(prefScreen, KEY_WEKIT_ENTRY)
            result != null
        } catch (_: Throwable) {
            false
        }
    }

    private fun resolveAddPrefInvocation(
        activity: Activity,
        prefScreen: Any,
        addPrefMethod: Method,
        prefInstance: Any
    ): Pair<Any, Method>? {
        if (addPrefMethod.declaringClass.isInstance(prefScreen)) {
            return prefScreen to addPrefMethod
        }
        val receiver = findReceiverFromActivity(activity, addPrefMethod.declaringClass)
        if (receiver != null) {
            return receiver to addPrefMethod
        }
        val fallbackMethod = findAddPreferenceMethod(prefScreen, prefInstance)
        if (fallbackMethod != null) {
            return prefScreen to fallbackMethod
        }
        return null
    }

    private fun findReceiverFromActivity(activity: Activity, declaringClass: Class<*>): Any? {
        var cls: Class<*>? = activity.javaClass
        while (cls != null) {
            cls.declaredFields.forEach { field ->
                try {
                    field.isAccessible = true
                    val value = field.get(activity) ?: return@forEach
                    if (declaringClass.isInstance(value)) {
                        WeLogger.w(
                            "WeSettingInjector",
                            "Using receiver from activity field ${field.name}: ${value.javaClass.name}"
                        )
                        return value
                    }
                } catch (_: Throwable) {
                    // ignore
                }
            }
            cls = cls.superclass
        }
        return null
    }

    private fun findAddPreferenceMethod(target: Any, prefInstance: Any): Method? {
        val prefClass = prefInstance.javaClass
        val candidates = target.javaClass.methods + target.javaClass.declaredMethods
        return candidates.firstOrNull { method ->
            val params = method.parameterTypes
            if (params.size != 2) return@firstOrNull false
            val firstMatch = params[0].isAssignableFrom(prefClass)
            val secondMatch = params[1] == Int::class.javaPrimitiveType || params[1] == Int::class.java
            method.returnType == Void.TYPE && firstMatch && secondMatch
        }?.also {
            WeLogger.w(
                "WeSettingInjector",
                "Fallback addPref method resolved: ${it.declaringClass.name}#${it.name}"
            )
        }
    }

    private fun notifyDataSetChanged(vararg targets: Any) {
        targets.forEach { target ->
            try {
                val method = target.javaClass.methods.firstOrNull {
                    it.name == "notifyDataSetChanged" && it.parameterTypes.isEmpty()
                } ?: return@forEach
                method.isAccessible = true
                method.invoke(target)
                return
            } catch (_: Throwable) {
                // ignore
            }
        }
    }

    private fun markCacheOutdated(stage: String, throwable: Throwable) {
        WeLogger.w("WeSettingInjector", "Mark cache outdated for $path at $stage", throwable)
        if (path.isNotBlank()) {
            DexCacheManager.deleteCache(path)
        }
    }
}
