package com.anhem.translator.platform

/**
 * 灵动岛 / 流体云 / Live Update 能力提供者接口。
 *
 * 三种实现：
 * - [Android16LiveUpdate] — Android 16+ 原生 Live Update API
 * - [ColorOS15FluidCloud] — ColorOS 15 流体云（标准 Notification 优化）
 * - [StandardLiveUpdate] — 通用降级方案
 */
interface LiveUpdateProvider {

    /**
     * 显示翻译结果灵动岛通知。
     *
     * @param src        原文
     * @param translated 翻译结果
     * @param dir        翻译方向（"zh2vi"/"vi2zh"/"auto"）
     */
    fun showUpdate(src: String, translated: String, dir: String)

    /**
     * 更新进度预览，用于显示"翻译中…"等中间状态。
     */
    fun updateProgress(preview: String)

    /** 取消灵动岛通知 */
    fun dismiss()
}
