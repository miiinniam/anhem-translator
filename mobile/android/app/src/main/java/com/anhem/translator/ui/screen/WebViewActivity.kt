package com.anhem.translator.ui.screen

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebViewAssetLoader
import com.anhem.translator.AnhemApp
import com.anhem.translator.platform.CapabilityRouter

/**
 * WebView 主界面 — 承载 Material You 风格的翻译 HTML 应用。
 *
 * ## 架构
 * ```
 * ┌─ WebView ─────────────────────────────────┐
 * │  index.html (Material You 翻译器)          │
 * │    ├─ 聊天模式: Persona / 称呼 / 语气     │
 * │    ├─ 阅读模式: 分栏 / 一键粘贴           │
 * │    └─ 设置: API Key / 资料 / 方言        │
 * │                                            │
 * │  AndroidBridge (JS Interface)               │
 * │    ├─ copyToClipboard()                    │
 * │    ├─ showLiveUpdate()     → Broadcast     │
 * │    ├─ getDeviceInfo()      → JSON          │
 * │    └─ syncPref()           → SharedPrefs   │
 * └────────────────────────────────────────────┘
 * ```
 */
class WebViewActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private val bridge = AnhemBridge(this)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        // 沉浸式全屏
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        webView = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                @Suppress("DEPRECATION")
                databaseEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                setSupportMultipleWindows(false)
            }
            addJavascriptInterface(bridge, "AndroidBridge")
        }

        // WebViewAssetLoader: 从 assets/ 安全加载
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                injectDeviceInfo(view)
                injectLocalStorageSync(view)
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (url.startsWith("https://appassets.androidplatform.net/")) return false
                try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) {}
                return true
            }

            override fun shouldInterceptRequest(
                view: WebView,
                request: android.webkit.WebResourceRequest
            ): android.webkit.WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }
        }

        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")
        setContentView(webView)
    }

    // ── JS 注入 ──────────────────────────────────────────────

    /** 注入设备信息到 window.__ANHEM_DEVICE__ */
    private fun injectDeviceInfo(view: WebView) {
        try {
            val capability = CapabilityRouter.describeCapability(this)
            val isOp = capability.contains("oneplus")
            val info = """
            window.__ANHEM_DEVICE__ = {
                platform: 'android',
                osVersion: ${Build.VERSION.SDK_INT},
                device: '${Build.MODEL.replace("'", "\\'")}',
                manufacturer: '${Build.MANUFACTURER.replace("'", "\\'")}',
                capability: '$capability',
                isOnePlus: $isOp,
                isAndroid16Plus: ${Build.VERSION.SDK_INT >= 36}
            };
            """.trimIndent()
            view.evaluateJavascript(info, null)
        } catch (e: Exception) {
            android.util.Log.e("AnhemBridge", "Failed to inject device info", e)
        }
    }

    /** 同步 localStorage 到原生 SharedPreferences */
    private fun injectLocalStorageSync(view: WebView) {
        val js = """
        (function() {
            try {
                var keys = JSON.parse(localStorage.getItem('_syncKeys') || '[]');
                if (keys.length === 0) {
                    keys = ['activePersona', 'tone', 'profile', 'settings', 'clipboardMonitorEnabled'];
                }
                keys.forEach(function(key) {
                    var val = localStorage.getItem(key);
                    if (val !== null) AndroidBridge.syncPref(key, val);
                });
            } catch(e) {}
        })();
        """.trimIndent()
        view.evaluateJavascript(js, null)
    }

    // ── 生命周期 ──────────────────────────────────────────────

    /**
     * 处理 singleTask 模式下从通知/灵动岛点击回的 Intent。
     * 当 Activity 已在栈顶时，系统调用 onNewIntent 而非 onCreate。
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /**
     * 解析 Intent 中的 initial_text 和 direction 参数，
     * 注入 JS 让 WebView 自动填入输入框。
     */
    private fun handleIntent(intent: Intent) {
        val text = intent.getStringExtra("initial_text")
        val dir = intent.getStringExtra("direction")
        if (text != null && ::webView.isInitialized) {
            val escaped = text.replace("\\", "\\\\").replace("'", "\\'")
            webView.evaluateJavascript(
                "(function(){ var inp=document.getElementById('input');if(inp){inp.value='$escaped'; inp.dispatchEvent(new Event('input'));}})();",
                null
            )
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        webView.removeJavascriptInterface("AndroidBridge")
        webView.destroy()
        super.onDestroy()
    }
}

// ═══════════════════════════════════════════════════════════════
// JavaScript Bridge
// ═══════════════════════════════════════════════════════════════

/**
 * 暴露给 HTML 的 Android 桥接接口，挂载为 `window.AndroidBridge`。
 *
 * ### 可用方法
 * | 方法              | 用途                        |
 * |-------------------|----------------------------|
 * | copyToClipboard() | 复制文本到系统剪贴板        |
 * | showLiveUpdate()  | 推送翻译结果到灵动岛        |
 * | getDeviceInfo()   | 获取设备信息（JSON）        |
 * | syncPref()        | 同步设置到原生 SharedPrefs  |
 */
class AnhemBridge(private val activity: WebViewActivity) {

    @JavascriptInterface
    fun copyToClipboard(text: String) {
        val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("anhem_translation", text))
    }

    /**
     * 推送翻译结果到灵动岛 / 流体云。
     *
     * JS 调用: `AndroidBridge.showLiveUpdate(src, result, dir)`
     *
     * 通过广播交由 [ClipboardMonitorService] 统一处理，
     * 其内部使用 [CapabilityRouter] 选择最佳灵动岛实现。
     */
    @JavascriptInterface
    fun showLiveUpdate(src: String, result: String, dir: String) {
        val intent = Intent("com.anhem.translator.SHOW_CAPSULE").apply {
            setPackage(activity.packageName)
            putExtra("src", src)
            putExtra("result", result)
            putExtra("dir", dir)
        }
        activity.sendBroadcast(intent)
    }

    /**
     * 获取设备能力信息 — 前端据此调整 UI 行为。
     *
     * 返回 JSON:
     * ```json
     * {
     *   "platform": "android",
     *   "osVersion": 35,
     *   "device": "OnePlus 13",
     *   "capability": "coloros15_fluid_cloud_oneplus",
     *   "isOnePlus": true
     * }
     * ```
     */
    @JavascriptInterface
    fun getDeviceInfo(): String {
        return """
        {
            "platform": "android",
            "osVersion": ${Build.VERSION.SDK_INT},
            "device": "${Build.MODEL.replace("\"", "\\\"")}",
            "manufacturer": "${Build.MANUFACTURER.replace("\"", "\\\"")}",
            "capability": "${CapabilityRouter.describeCapability(activity)}",
            "isOnePlus": ${CapabilityRouter.describeCapability(activity).contains("oneplus")},
            "isAndroid16Plus": ${Build.VERSION.SDK_INT >= 36}
        }
        """.trimIndent()
    }

    /**
     * 同步 key-value 从 WebView localStorage 到原生 SharedPreferences。
     */
    @JavascriptInterface
    fun syncPref(key: String, value: String) {
        val storage = (activity.application as AnhemApp).storage
        when (key) {
            "activePersona" -> {
                if (value.isNotBlank() && value != "null") storage.saveActivePersona(value)
            }
            "tone" -> storage.saveTone(value)
            else -> {
                activity.getSharedPreferences("anhem_web_sync", Context.MODE_PRIVATE)
                    .edit().putString(key, value).apply()
            }
        }
    }
}
