package com.anhem.translator.platform

import android.content.Context
import android.os.Build
import com.anhem.translator.ui.capsule.LiveCapsuleManager

/**
 * Android 16+ (API 36) 原生 Live Update 实现。
 *
 * 使用 [LiveCapsuleManager] 三阶段胶囊 +
 * `REQUEST_PROMOTED_ONGOING` extra 升级为原生灵动岛。
 */
class Android16LiveUpdate(private val context: Context) : LiveUpdateProvider {

    private val capsule = LiveCapsuleManager(context)

    override fun updateProgress(preview: String) {
        capsule.showProgress(preview)
    }

    override fun showUpdate(src: String, translated: String, dir: String) {
        capsule.showResult(src, translated, dir)
    }

    override fun dismiss() {
        capsule.dismiss()
    }
}
