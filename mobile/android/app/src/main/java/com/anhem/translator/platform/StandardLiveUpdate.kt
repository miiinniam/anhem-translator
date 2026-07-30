package com.anhem.translator.platform

import android.content.Context
import com.anhem.translator.ui.capsule.LiveCapsuleManager

/**
 * 通用标准通知降级方案 — 三阶段胶囊。
 */
class StandardLiveUpdate(context: Context) : LiveUpdateProvider {

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
