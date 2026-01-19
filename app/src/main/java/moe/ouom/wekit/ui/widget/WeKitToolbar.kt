package moe.ouom.wekit.ui.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.Toolbar

/**
 * 影子控件
 * 强制系统使用模块 ClassLoader 加载 Toolbar
 */
class WeKitToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : Toolbar(context, attrs, defStyleAttr)