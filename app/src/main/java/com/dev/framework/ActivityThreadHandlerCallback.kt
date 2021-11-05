package com.dev.framework

import android.os.Handler
import android.os.Message
import com.dev.manager.HookActivityManager

@Deprecated("Temporarily useless")
class ActivityThreadHandlerCallback(private val handler: Handler?) : Handler.Callback {
    override fun handleMessage(msg: Message): Boolean {
        HookActivityManager.replacePlaceHolderIntentWithPluginIntent(msg)
        handler?.handleMessage(msg)
        return true
    }
}