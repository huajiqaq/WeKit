package moe.ouom.wekit.ui.creator.dialog.item.chat.risk

import android.content.Context
import android.text.InputType
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

        addEditTextPreference(
            key = "red_packet_delay_custom",
            title = "基础延迟",
            summary = "延迟时间",
            defaultValue = "1000",
            hint = "请输入延迟时间（毫秒）",
            inputType = InputType.TYPE_CLASS_NUMBER,
            maxLength = 5,
            summaryFormatter = { value ->
                if (value.isEmpty()) "0 ms" else "$value ms"
            }
        )

        addSwitchPreference(
            key = "red_packet_delay_random",
            title = "随机延时",
            summary = "在基础延迟上增加 ±500ms 随机偏移，防止风控"
        )
    }
}