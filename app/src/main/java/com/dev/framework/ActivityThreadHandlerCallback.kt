package com.dev.framework

import android.os.Handler
import android.os.Message

class ActivityThreadHandlerCallback(private val handler: Handler?) : Handler.Callback {
    override fun handleMessage(msg: Message): Boolean {
        AMSHookManager.replacePlaceHolderIntentWithPluginIntent(msg)
        handler?.handleMessage(msg)
        return true
    }
}