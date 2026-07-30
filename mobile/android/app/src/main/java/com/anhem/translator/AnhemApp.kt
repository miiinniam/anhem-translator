package com.anhem.translator

import android.app.Application
import com.anhem.translator.model.AppStorage

class AnhemApp : Application() {
    lateinit var storage: AppStorage
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        storage = AppStorage(this)
    }

    companion object {
        lateinit var instance: AnhemApp
            private set
    }
}
