package com.dev.plugin.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class PluginReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            Toast.makeText(it.applicationContext, "插件的广播启动成功 >>>", Toast.LENGTH_SHORT).show()
        }
    }
}