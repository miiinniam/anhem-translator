package com.anhem.translator.platform

import android.content.Context
import android.os.Build

/**
 * 灵动岛 / Live Update 能力路由器。
 *
 * 按优先级选择最佳实现：
 * 1. [Android16LiveUpdate] — Android 16+ (API 36) 原生 Live Update
 * 2. [ColorOS15FluidCloud] — ColorOS 15 流体云优化
 * 3. [StandardLiveUpdate] — 通用降级
 *
 * ### 使用方式
 * ```kotlin
 * val provider = CapabilityRouter.resolve(context)
 * provider.showUpdate("Xin chào", "你好", "vi2zh")
 * ```
 */
object CapabilityRouter {

    @Volatile
    private var cachedProvider: LiveUpdateProvider? = null

    /**
     * 解析当前设备的最佳灵动岛实现。
     *
     * 结果会被缓存，确保 NotificationChannel 只创建一次。
     */
    fun resolve(context: Context): LiveUpdateProvider {
        return cachedProvider ?: synchronized(this) {
            cachedProvider ?: create(context).also { cachedProvider = it }
        }
    }

    /** 返回当前设备的能力描述，供 JS Bridge 上报给前端 */
    fun describeCapability(context: Context): String {
        val p = resolve(context)
        return when (p) {
            is Android16LiveUpdate -> "android16_live_update"
            is ColorOS15FluidCloud -> {
                if (p.isOnePlus) "coloros15_fluid_cloud_oneplus"
                else "coloros15_fluid_cloud"
            }
            else -> "standard_notification"
        }
    }

    private fun create(context: Context): LiveUpdateProvider {
        // 优先：Android 16+ 原生 Live Update
        if (Build.VERSION.SDK_INT >= 36) {
            return Android16LiveUpdate(context.applicationContext)
        }
        // 其次：ColorOS 15 流体云
        if (OnePlusDetect.isColorOS15Plus() || OnePlusDetect.isOPPO()) {
            return ColorOS15FluidCloud(context.applicationContext)
        }
        // 降级：标准通知
        return StandardLiveUpdate(context.applicationContext)
    }
}
