package moe.ouom.wekit.ui.creator.dialog

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialog
import androidx.appcompat.widget.Toolbar
import androidx.compose.ui.unit.sp
import com.google.android.material.materialswitch.MaterialSwitch
import moe.ouom.wekit.config.ConfigManager
import moe.ouom.wekit.constants.Constants
import moe.ouom.wekit.util.common.ModuleRes
import moe.ouom.wekit.util.log.Logger
import androidx.core.view.isEmpty

/**
 * 模仿 Activity 布局的 Dialog
 * 使用 Rikka 风格动态生成设置项
 */
abstract class BaseRikkaDialog(
    context: Context,
    private val title: String
) : AppCompatDialog(context, getThemeId()) {

    companion object {
        private fun getThemeId(): Int {
            // 必须使用模块定义的 Theme，否则 Material 控件会崩溃
            val themeId = ModuleRes.getId("Theme.WeKit", "style")
            return if (themeId != 0) themeId else android.R.style.Theme_DeviceDefault_Light_NoActionBar
        }
    }

    protected lateinit var contentContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置 Window 属性
        window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        // 加载基础框架布局
        val layoutId = ModuleRes.getId("module_dialog_frame", "layout")
        if (layoutId == 0) {
            Logger.e("BaseRikkaDialog: 找不到 module_dialog_frame 布局")
            return
        }

        // 使用 Dialog 自身的 layoutInflater (绑定了 CommonContextWrapper)
        val rootView = layoutInflater.inflate(layoutId, null)
        setContentView(rootView)

        // 初始化 Toolbar
        val idAppBar = ModuleRes.getId("topAppBar", "id")
        val toolbar = rootView.findViewById<Toolbar>(idAppBar)

        toolbar.title = title
        // 设置返回图标
        toolbar.setNavigationIcon(ModuleRes.getId("ic_outline_arrow_back_ios_new_24", "drawable"))
        toolbar.setNavigationOnClickListener { dismiss() }

        // 初始化内容容器 (ScrollView + LinearLayout)
        val idSettingsFrame = ModuleRes.getId("settings", "id")
        val settingsFrame = rootView.findViewById<FrameLayout>(idSettingsFrame)

        val scrollView = ScrollView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isVerticalScrollBarEnabled = false
        }

        contentContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            // 底部留白，防止被导航栏遮挡
            setPadding(0, 0, 0, 100)
        }

        scrollView.addView(contentContainer)
        settingsFrame.addView(scrollView)

        // 回调子类初始化列表
        initPreferences()
    }

    // 子类必须实现这个方法来添加内容
    abstract fun initPreferences()

    /**
     * 添加一个小标题 (Category)
     */
    protected fun addCategory(title: String) {
        val layoutId = ModuleRes.getId("module_item_entry_category", "layout")
        if (layoutId == 0) return

        val view = layoutInflater.inflate(layoutId, contentContainer, false)

        val idTitle = ModuleRes.getId("title", "id")
        val tvTitle = view.findViewById<TextView>(idTitle)

        // 隐藏不需要的图标
        val idIcon = ModuleRes.getId("icon", "id")
        val idArrow = ModuleRes.getId("arrow", "id")
        view.findViewById<View>(idIcon)?.visibility = View.GONE
        view.findViewById<View>(idArrow)?.visibility = View.GONE

        view.minimumHeight = 0
        view.background = null

        val density = context.resources.displayMetrics.density

        // 左右保持 16dp
        val paddingHorizontal = (16 * density).toInt()
        // 底部保持 8dp
        val paddingBottom = (8 * density).toInt()

        // 如果 count == 0，说明这是第一个添加的控件，不需要大的分隔距离
        val isFirstItem = contentContainer.isEmpty()

        val paddingTop = if (isFirstItem) {
            (10 * density).toInt() // 第一个
        } else {
            (24 * density).toInt() // 后续的
        }

        view.setPadding(paddingHorizontal, paddingTop, paddingHorizontal, paddingBottom)

        // 设置文字
        tvTitle.text = title
        tvTitle.textSize = 14f
        val accentColor = ModuleRes.getColor("colorAccent")
        tvTitle.setTextColor(accentColor)

        contentContainer.addView(view)
    }

    /**
     * 添加一个开关选项 (Switch Preference)
     * @param key 存入 ConfigManager 的 key (不包含前缀，内部会自动处理)
     * @param title 标题
     * @param summary 描述
     */
    protected fun addSwitchPreference(key: String, title: String, summary: String) {
        val layoutId = ModuleRes.getId("module_item_switch", "layout")
        if (layoutId == 0) return

        // 使用 layoutInflater
        val view = layoutInflater.inflate(layoutId, contentContainer, false)

        val tvTitle = view.findViewById<TextView>(ModuleRes.getId("title", "id"))
        val tvSummary = view.findViewById<TextView>(ModuleRes.getId("summary", "id"))
        val switchWidget = view.findViewById<MaterialSwitch>(ModuleRes.getId("widget_switch", "id"))
        val root = view.findViewById<View>(ModuleRes.getId("item_root", "id"))

        tvTitle.text = title
        tvSummary.text = summary

        val fullKey = "${Constants.PrekXXX}$key"

        // 读取配置
        val isChecked = ConfigManager.getDefaultConfig().getBooleanOrFalse(fullKey)
        switchWidget.isChecked = isChecked

        val listener = CompoundButton.OnCheckedChangeListener { _, checked ->
            ConfigManager.getDefaultConfig().edit().putBoolean(fullKey, checked).apply()
            Logger.d("BaseRikkaDialog: Config changed [$fullKey] -> $checked")
        }

        switchWidget.setOnCheckedChangeListener(listener)
        root.setOnClickListener { switchWidget.toggle() }

        contentContainer.addView(view)
    }
}