package moe.ouom.wekit.hooks.base

import android.annotation.SuppressLint
import moe.ouom.wekit.hooks._base.ApiHookItem
import moe.ouom.wekit.hooks._core.annotation.HookItem

@SuppressLint("DiscouragedApi")
@HookItem(path = "设置模块入口")
class WeSettingInjector : ApiHookItem() {
    @Throws(Throwable::class)
    override fun entry(classLoader: ClassLoader) {
        // TODO
    }
}