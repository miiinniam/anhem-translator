package com.anhem.translator.ui.capsule

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.anhem.translator.R
import com.anhem.translator.ui.screen.WebViewActivity

/**
 * 灵动岛 / Fluid Cloud 胶囊通知管理器 v3.0。
 *
 * ## 三阶段生命周期
 * ```
 * showProgress("翻译中…")      → 脉冲胶囊（小图标 + 文字）
 *         ↓ 延迟 300ms
 * showResult(src, out, dir)    → 结果胶囊（方向 + 原文预览 + 译文）
 *         ↓ 8 秒后
 * dismiss()                    → 胶囊消失
 * ```
 *
 * ## ColorOS 15 Fluid Cloud 适配要点
 * - CATEGORY_SERVICE + ongoing → 系统识别为灵动岛通知
 * - 短标题 (<20 chars) → 胶囊内文字不截断
 * - BigTextStyle → 展开后显示完整译文
 * - 自定义 SmallIcon → 品牌化胶囊图标
 * - setTimeoutAfter → 自动消失，不留残留
 * - "复制" 快捷操作 → 通知栏按钮直接复制译文
 */
class LiveCapsuleManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "translation_bubble"
        const val NOTIF_ID = 2001
        const val MAX_PREVIEW = 16
        const val AUTO_DISMISS_MS = 8000L
        const val GROUP_KEY = "anhem_translations"
    }

    private val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val handler = Handler(Looper.getMainLooper())
    private var dismissRunnable: Runnable? = null

    init {
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "翻译灵动岛", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "翻译结果显示在状态栏灵动岛胶囊中"
                setShowBadge(false)
                // 低振动，不打搅用户
                vibrationPattern = longArrayOf(0)
                enableVibration(false)
            }
        )
    }

    /** 阶段 1: 显示进度胶囊 */
    fun showProgress(text: String = "翻译中…") {
        cancelDismiss()
        val displayText = if (text.length > MAX_PREVIEW) text.take(MAX_PREVIEW) + "…" else text
        nm.notify(NOTIF_ID, NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_anhem)
            .setContentTitle(displayText)
            .setContentText("anhem 翻译")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setGroup(GROUP_KEY)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setSilent(true)
            .build())
    }

    /** 阶段 2: 显示翻译结果胶囊 */
    fun showResult(src: String, translated: String, dir: String = "auto") {
        cancelDismiss()
        val direction = when (dir) {
            "zh2vi" -> "中→越"
            "vi2zh" -> "越→中"
            else -> "翻译"
        }
        val srcPreview = if (src.length > MAX_PREVIEW) src.take(MAX_PREVIEW) + "…" else src
        val outPreview = if (translated.length > MAX_PREVIEW) translated.take(MAX_PREVIEW) + "…" else translated

        // 点击 → 打开 App
        val clickIntent = Intent(context, WebViewActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("initial_text", src)
            putExtra("direction", dir)
        }
        val clickPi = PendingIntent.getActivity(
            context, 0, clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "复制" 按钮 → 系统剪贴板
        val copyIntent = Intent(context, CopyReceiver::class.java).apply {
            putExtra("text", translated)
        }
        val copyPi = PendingIntent.getBroadcast(
            context, 1, copyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        nm.notify(NOTIF_ID, NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_anhem)
            .setContentTitle("[$direction] $srcPreview")
            .setContentText(outPreview)
            .setStyle(NotificationCompat.BigTextStyle()
                .setBigContentTitle("[$direction] 翻译结果")
                .bigText(translated)
                .setSummaryText("原文: $srcPreview"))
            .setOngoing(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setGroup(GROUP_KEY)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setContentIntent(clickPi)
            .addAction(android.R.drawable.ic_menu_edit, "复制", copyPi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSilent(true)
            .setTimeoutAfter(AUTO_DISMISS_MS)
            .build())

        // 备用: 即使 setTimeoutAfter 不生效，8 秒后手动 dismiss
        scheduleDismiss()
    }

    /** 阶段 3: 立即取消胶囊 */
    fun dismiss() {
        cancelDismiss()
        nm.cancel(NOTIF_ID)
    }

    private fun scheduleDismiss() {
        cancelDismiss()
        dismissRunnable = Runnable { dismiss() }.also {
            handler.postDelayed(it, AUTO_DISMISS_MS)
        }
    }

    private fun cancelDismiss() {
        dismissRunnable?.let { handler.removeCallbacks(it) }
        dismissRunnable = null
    }
}

/**
 * 隐形 BroadcastReceiver — 处理通知栏"复制"按钮点击。
 *
 * 将译文写入系统剪贴板，无需启动 Activity。
 */
class CopyReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val text = intent.getStringExtra("text") ?: return
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("anhem_translation", text))
        // 取消通知
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(LiveCapsuleManager.NOTIF_ID)
    }
}
