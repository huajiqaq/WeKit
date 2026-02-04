package moe.ouom.wekit.util

import android.annotation.SuppressLint
import android.content.SharedPreferences
import de.robv.android.xposed.XposedHelpers
import moe.ouom.wekit.config.RuntimeConfig
import moe.ouom.wekit.host.HostInfo
import moe.ouom.wekit.util.Initiator.loadClass
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date

/**
 * 微信核心算法复原工具
 * 基于源码: xi.k, xv0.y1, xv0.fa, xv0.z1
 */
object WeChatAlgo {

    /**
     * 获取当前登录的微信ID
     */
    fun getSelfWxId(): String {
        val sharedPreferences: SharedPreferences =
            HostInfo.getApplication().getSharedPreferences("com.tencent.mm_preferences", 0)

        RuntimeConfig.setLogin_weixin_username(sharedPreferences.getString("login_weixin_username", ""))
        RuntimeConfig.setLast_login_nick_name(sharedPreferences.getString("last_login_nick_name", ""))
        RuntimeConfig.setLogin_user_name(sharedPreferences.getString("login_user_name", ""))
        RuntimeConfig.setLast_login_uin(sharedPreferences.getString("last_login_uin", "0"))

        return RuntimeConfig.getLogin_weixin_username()
    }

    /**
     * 生成 ClientMsgId
     */
    fun generateClientMsgId(wxId: String, timeMs: Long): Int {
        val rawString = generateClientMsgIdString(wxId, timeMs)
        return rawString.hashCode()
    }

    @SuppressLint("SimpleDateFormat")
    private fun generateClientMsgIdString(str: String?, j16: Long): String {
        val str2: String
        val str3 = SimpleDateFormat("ssHHmmMMddyy").format(Date(j16))

        if (str == null || str.length <= 1) {
            str2 = str3 + "fffffff"
        } else {
            val md5Hex = md5V2(str.toByteArray())
            str2 = str3 + md5Hex.substring(0, 7)
        }

        val suffixHex = String.format("%04x", j16 % 65535)

        val suffixNum = (j16 % 7) + 100

        return str2 + suffixHex + suffixNum
    }

    /**
     * MD5 实现
     */
    private fun md5V2(bytes: ByteArray): String {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            digest.update(bytes)
            val bArrDigest = digest.digest()
            val sb = StringBuilder()
            for (b in bArrDigest) {
                val i = b.toInt() and 0xFF
                var hex = Integer.toHexString(i)
                if (hex.length < 2) {
                    hex = "0$hex"
                }
                sb.append(hex)
            }
            sb.toString()
        } catch (e: Exception) {
            ""
        }
    }
}