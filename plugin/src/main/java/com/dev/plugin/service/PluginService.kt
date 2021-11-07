package com.dev.plugin.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class PluginService : Service() {
    override fun onCreate() {
        Log.i("WWE", "PluginService #onCreate")
        super.onCreate()
    }

    override fun onStart(intent: Intent?, startId: Int) {
        Log.i("WWE", "PluginService #onStart")
        super.onStart(intent, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.i("WWE", "PluginService #onDestroy")
        super.onDestroy()
    }
}