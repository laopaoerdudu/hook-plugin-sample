package com.dev

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.dev.manager.HookServiceManager

class ProxyService : Service() {
    override fun onCreate() {
        Log.i("WWE", "ProxyService #onCreate")
        super.onCreate()
    }

    override fun onStart(intent: Intent?, startId: Int) {
        Log.i("WWE", "ProxyService #onStart")
        HookServiceManager.onStart(intent, startId)
        super.onStart(intent, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.i("WWE", "ProxyService #onDestroy")
        super.onDestroy()
    }
}