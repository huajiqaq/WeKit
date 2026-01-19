package moe.ouom.wekit.ui.creator.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.widget.Toolbar // 这次可以放心引用 AndroidX 了
import moe.ouom.wekit.util.common.ModuleRes
import androidx.core.graphics.drawable.toDrawable

abstract class BaseSettingsDialog(
    context: Context,
    private val title: String
) : Dialog(context, getThemeId()) {

    companion object {
        private fun getThemeId(): Int {
            return ModuleRes.getId("Theme.WeKit", "style")
        }
    }

    protected lateinit var contentContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Window 设置
        window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        }

        // 使用 Dialog 自身的 layoutInflater，它绑定了 CommonContextWrapper
        val layoutId = ModuleRes.getId("module_dialog_frame", "layout")
        if (layoutId == 0) return // 异常处理

        // 使用 context 的 inflater，这会触发 CommonContextWrapper.getSystemService
        // 进而触发 ModuleFactory，强制使用模块 ClassLoader 加载 Toolbar
        val rootView = layoutInflater.inflate(layoutId, null)

        setContentView(rootView)

        // 初始化 Toolbar
        val idAppBar = ModuleRes.getId("topAppBar", "id")
        val toolbar = findViewById<Toolbar>(idAppBar)

        // 如果一切正常，这里的 toolbar 就是模块 ClassLoader 加载的，不会报错
        toolbar.title = title
        toolbar.setNavigationIcon(ModuleRes.getId("ic_outline_arrow_back_ios_new_24", "drawable"))
        toolbar.setNavigationOnClickListener { dismiss() }

        // 初始化容器
        val idSettingsFrame = ModuleRes.getId("settings", "id")
        val settingsFrame = findViewById<FrameLayout>(idSettingsFrame)

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
            setPadding(0, 0, 0, 100)
        }

        scrollView.addView(contentContainer)
        settingsFrame.addView(scrollView)

        initList()
    }

    abstract fun initList()

    // 给子类用的辅助方法：动态添加条目时，也必须用 context 的 inflater
    protected fun inflateItem(layoutName: String, parent: ViewGroup): android.view.View? {
        val id = ModuleRes.getId(layoutName, "layout")
        if (id == 0) return null
        return layoutInflater.inflate(id, parent, false)
    }
}