package com.anhem.translator.platform

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.anhem.translator.ui.capsule.LiveCapsuleManager

/**
 * ColorOS 15 / OnePlus 13 流体云灵动岛实现。
 *
 * ## 三阶段胶囊生命周期
 * 1. 翻译中 → 进度胶囊（脉冲）
 * 2. 翻译完成 → 结果胶囊（方向+原文+译文+复制按钮）
 * 3. 8 秒后 → 自动消失
 *
 * ## Fluid Cloud 触发条件
 * - `CATEGORY_SERVICE` + 非 ongoing 通知
 * - 短标题 (< 20 chars) 适配挖孔胶囊宽度
 * - BigTextStyle 展开显示完整译文
 * - 自定义 SmallIcon 显示品牌图标
 */
class ColorOS15FluidCloud(private val context: Context) : LiveUpdateProvider {

    val isOnePlus: Boolean by lazy { OnePlusDetect.isOnePlus() }
    val isColorOS15Plus: Boolean by lazy { OnePlusDetect.isColorOS15Plus() }

    private val capsule = LiveCapsuleManager(context)
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun updateProgress(preview: String) {
        capsule.showProgress(preview)
    }

    override fun showUpdate(src: String, translated: String, dir: String) {
        // 阶段 2 → 显示结果胶囊，自动 8 秒后消失
        capsule.showResult(src, translated, dir)
    }

    override fun dismiss() {
        capsule.dismiss()
    }
}

/**
 * OnePlus / ColorOS 设备检测工具。
 */
internal object OnePlusDetect {

    fun isColorOS15Plus(): Boolean {
        return try {
            val clz = Class.forName("android.os.SystemProperties")
            val method = clz.getMethod("get", String::class.java, String::class.java)
            val version = method.invoke(null, "ro.build.version.opporom", "") as? String ?: ""
            version.startsWith("V15") || version.startsWith("15")
        } catch (_: Exception) {
            false
        }
    }

    fun isOnePlus(): Boolean {
        return try {
            val clz = Class.forName("android.os.SystemProperties")
            val method = clz.getMethod("get", String::class.java, String::class.java)
            val brand = method.invoke(null, "ro.product.brand", "") as? String ?: ""
            val manufacturer = method.invoke(null, "ro.product.manufacturer", "") as? String ?: ""
            brand.equals("OnePlus", ignoreCase = true) ||
                manufacturer.equals("OnePlus", ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    fun isOPPO(): Boolean {
        return try {
            val clz = Class.forName("android.os.SystemProperties")
            val method = clz.getMethod("get", String::class.java, String::class.java)
            val brand = method.invoke(null, "ro.product.brand", "") as? String ?: ""
            brand.equals("OPPO", ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }
}
