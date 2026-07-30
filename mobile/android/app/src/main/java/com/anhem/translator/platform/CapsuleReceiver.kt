package com.anhem.translator.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anhem.translator.AnhemApp

/**
 * 接收来自 WebView JS Bridge 的 Fluid Cloud / Live Update 触发请求。
 *
 * Action: `com.anhem.translator.SHOW_CAPSULE`
 * Extras: `src` (String), `result` (String), `dir` (String)
 *
 * 通过 [CapabilityRouter] 自动选择最佳灵动岛实现：
 * - Android 16+: Live Update Notification
 * - ColorOS 15: 流体云优化
 * - 通用: 标准通知
 */
class CapsuleReceiver : BroadcastReceiver() {

    override fun onReceive(context: android.content.Context, intent: Intent) {
        if (intent.action != "com.anhem.translator.SHOW_CAPSULE") return

        val src = intent.getStringExtra("src") ?: return
        val result = intent.getStringExtra("result") ?: return
        val dir = intent.getStringExtra("dir") ?: "auto"

        val app = context.applicationContext as AnhemApp
        CapabilityRouter.resolve(app).showUpdate(src, result, dir)
    }
}
