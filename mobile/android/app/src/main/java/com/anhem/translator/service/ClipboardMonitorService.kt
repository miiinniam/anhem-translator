package com.anhem.translator.service

import android.app.*
import android.content.*
import android.content.ClipboardManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.anhem.translator.AnhemApp
import com.anhem.translator.R
import com.anhem.translator.model.*
import com.anhem.translator.engine.TranslationEngine
import com.anhem.translator.platform.CapabilityRouter
import com.anhem.translator.platform.LiveUpdateProvider
import kotlinx.coroutines.*

/**
 * 剪贴板监听前台服务。
 *
 * 后台持续监听剪贴板变化，检测中/越文字后自动翻译，
 * 并通过 [CapabilityRouter] 选择最佳方式推送灵动岛通知。
 *
 * ## 灵动岛推送链
 * ```
 * ClipboardMonitor → CapabilityRouter.resolve()
 *   ├─ Android16LiveUpdate   (API 36+)
 *   ├─ ColorOS15FluidCloud   (OnePlus 13)
 *   └─ StandardLiveUpdate    (通用)
 * ```
 *
 * 同时监听来自 WebView JS Bridge 的 `ACTION_SHOW_CAPSULE` 广播，
 * 将 WebView 内的翻译结果推送到灵动岛。
 */
class ClipboardMonitorService : Service() {

    companion object {
        /** WebView JS Bridge → Fluid Cloud 广播 */
        const val ACTION_SHOW_CAPSULE = "com.anhem.translator.SHOW_CAPSULE"

        /** Intent 额外参数 */
        const val EXTRA_SRC = "src"
        const val EXTRA_RESULT = "result"
        const val EXTRA_DIR = "dir"

        /** 剪贴板去重窗口（毫秒） */
        private const val DEDUP_WINDOW_MS = 3000L
    }

    // ── 核心依赖 ──────────────────────────────────────────────

    /** 灵动岛能力提供者 — 由 [CapabilityRouter] 按设备选择 */
    private lateinit var liveUpdate: LiveUpdateProvider

    private lateinit var clipboardManager: ClipboardManager
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastText = ""
    private var lastTime = 0L

    // ── WebView Bridge 广播接收器 ─────────────────────────────

    /**
     * 接收来自 WebViewActivity 中的 JS Bridge 触发。
     *
     * 当用户在 WebView 内完成翻译并点击"推送到灵动岛"，
     * AnhemBridge 通过 `sendBroadcast` 触发此接收器。
     */
    private val capsuleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: Intent) {
            if (intent.action != ACTION_SHOW_CAPSULE) return
            val src = intent.getStringExtra(EXTRA_SRC) ?: return
            val result = intent.getStringExtra(EXTRA_RESULT) ?: return
            val dir = intent.getStringExtra(EXTRA_DIR) ?: "auto"
            liveUpdate.showUpdate(src, result, dir)
        }
    }

    // ── 生命周期 ──────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        clipboardManager = getSystemService(ClipboardManager::class.java)
        liveUpdate = CapabilityRouter.resolve(this)
        clipboardManager.addPrimaryClipChangedListener(clipListener)

        // 注册 WebView Bridge 广播
        val filter = IntentFilter(ACTION_SHOW_CAPSULE)
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(capsuleReceiver, filter, 4)
        } else {
            registerReceiver(capsuleReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createForegroundNotification()
        return START_STICKY
    }

    // ── 前台通知 ──────────────────────────────────────────────

    private fun createForegroundNotification() {
        val channelId = "clipboard_monitor"
        val channel = NotificationChannel(
            channelId, "翻译监听",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "anhem 剪贴板翻译监听" }

        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        startForeground(1001, NotificationCompat.Builder(this, channelId)
            .setContentTitle("anhem 翻译监听中")
            .setContentText("复制文本即可翻译")
            .setSmallIcon(R.drawable.ic_anhem)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build())
    }

    // ── 剪贴板监听 ────────────────────────────────────────────

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        val clip = clipboardManager.primaryClip ?: return@OnPrimaryClipChangedListener
        if (clip.itemCount == 0) return@OnPrimaryClipChangedListener
        val text = clip.getItemAt(0).text?.toString()?.trim() ?: return@OnPrimaryClipChangedListener
        if (text.length < 2 || text.length > 2000) return@OnPrimaryClipChangedListener

        // 去重
        val now = System.currentTimeMillis()
        if (text == lastText && now - lastTime < DEDUP_WINDOW_MS) return@OnPrimaryClipChangedListener
        lastText = text; lastTime = now

        if (!hasTargetChars(text)) return@OnPrimaryClipChangedListener

        val storage = (application as AnhemApp).storage
        val settings = storage.loadSettings()
        if (!settings.clipboardMonitorEnabled || settings.apiKey.isBlank()) return@OnPrimaryClipChangedListener

        val persona = Persona.ALL.first { it.id == storage.loadActivePersona() }
        val profile = storage.loadProfile()
        val tone = storage.loadTone()
        val dir = detectDirection(text)

        // 显示进度
        liveUpdate.updateProgress("翻译中… $dir")

        scope.launch {
            try {
                val result = TranslationEngine.translate(text, dir, persona, profile, settings, tone)
                liveUpdate.showUpdate(text, result.text, dir)

                // 更新用量统计
                val stats = storage.loadUsageStats()
                storage.saveUsageStats(stats.copy(
                    reqs = stats.reqs + 1,
                    tokens = stats.tokens + (result.costTokens ?: 0),
                    cny = stats.cny + (result.costCny ?: 0.0)
                ))
            } catch (_: Exception) {
                liveUpdate.showUpdate(text, "⚠ 翻译失败", dir)
            }
        }
    }

    // ── 工具 ──────────────────────────────────────────────────

    /** 检测文本是否包含中文/越南语目标字符 */
    private fun hasTargetChars(text: String): Boolean = text.any {
        val c = it.code
        c in 0x4E00..0x9FFF ||       // CJK
        c in 0x3400..0x4DBF ||       // CJK Ext-A
        c in 0x1EA0..0x1EF9 ||       // Vietnamese
        c in 0x0100..0x024F           // Latin Extended
    }

    // ── 销毁 ──────────────────────────────────────────────────

    override fun onDestroy() {
        try { unregisterReceiver(capsuleReceiver) } catch (_: Exception) {}
        clipboardManager.removePrimaryClipChangedListener(clipListener)
        scope.cancel()
        liveUpdate.dismiss()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
