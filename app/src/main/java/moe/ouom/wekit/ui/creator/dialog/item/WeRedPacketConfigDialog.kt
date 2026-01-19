package moe.ouom.wekit.ui.creator.dialog.item

import android.content.Context
import moe.ouom.wekit.ui.creator.dialog.BaseRikkaDialog

class WeRedPacketConfigDialog(context: Context) : BaseRikkaDialog(context, "自动抢红包") {

    override fun initPreferences() {
        addCategory("通用设置")

        addSwitchPreference(
            key = "red_packet_notification",
            title = "抢到后通知（没写）",
            summary = "在通知栏显示抢到的金额"
        )

        addCategory("高级选项")

        addSwitchPreference(
            key = "red_packet_self",
            title = "抢自己的红包",
            summary = "默认情况下不抢自己发出的"
        )
        
        addSwitchPreference(
            key = "red_packet_delay_random",
            title = "随机延时",
            summary = "模拟人工操作，防止风控"
        )
    }
}