package com.dev.plugin.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class DynamicBroadcast : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            if("com.dev.plugin.receiver.DynamicBroadcast" == it.action) {
                Toast.makeText(context, "动态广播启动成功", Toast.LENGTH_LONG).show()
            }
        }
    }
}