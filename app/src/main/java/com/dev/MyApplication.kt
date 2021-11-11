package com.dev

import android.app.Application
import android.content.Context
import com.dev.constant.HookConstant.Companion.PLUGIN_APK_NAME
import com.dev.helper.FileHelper

class MyApplication : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        loadPluginDex(base)
    }

    override fun onCreate() {
        super.onCreate()
    }

    private fun loadPluginDex(context: Context?) {
        FileHelper.copyAssetsFileToSystemDir(this, PLUGIN_APK_NAME)
    }
}