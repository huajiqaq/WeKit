package moe.ouom.wekit.ui.creator.dialog

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import moe.ouom.wekit.util.common.ModuleRes

class MainSettingsDialog(context: Context) : BaseSettingsDialog(context, "WeKit") {

    override fun initList() {
        // 定义大类列表：名称 -> 图标资源名
        val categories = listOf(
            "聊天与消息" to "ic_twotone_message_24",
            "资料卡" to "ic_profile",
            "优化与修复" to "ic_baseline_auto_fix_high_24",
            "开发者选项" to "ic_baseline_developer_mode_24",
            "娱乐功能" to "ic_baseline_free_breakfast_24"
        )

        categories.forEach { (name, iconName) ->
            addCategoryEntry(name, iconName)
        }
    }

    private fun addCategoryEntry(name: String, iconName: String) {
        val view = ModuleRes.inflate("module_item_entry", contentContainer) ?: return
        
        val idIcon = ModuleRes.getId("icon", "id")
        val idTitle = ModuleRes.getId("title", "id")
        val idRoot = ModuleRes.getId("item_root", "id")

        val ivIcon = view.findViewById<ImageView>(idIcon)
        val tvTitle = view.findViewById<TextView>(idTitle)
        val root = view.findViewById<LinearLayout>(idRoot)

        tvTitle.text = name
        
        // 尝试加载图标，如果没有则使用默认
        val iconDrawable = ModuleRes.getDrawable(iconName)
        if (iconDrawable != null) {
            ivIcon.setImageDrawable(iconDrawable)
        } else {
            // 设置一个默认图标防止空白
            ivIcon.setImageResource(android.R.drawable.ic_menu_agenda) 
        }

        root.setOnClickListener {
            // 点击进入二级页面
            val categoryDialog = CategorySettingsDialog(context, name)
            categoryDialog.show()
        }

        contentContainer.addView(view)
    }
}