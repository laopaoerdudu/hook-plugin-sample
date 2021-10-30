package com.dev.framework

import android.os.Handler
import android.os.Message
import com.dev.framework.manager.HookManager

@Deprecated("Temporarily useless")
class ActivityThreadHandlerCallback(private val handler: Handler?) : Handler.Callback {
    override fun handleMessage(msg: Message): Boolean {
        HookManager.replacePlaceHolderIntentWithPluginIntent(msg)
        handler?.handleMessage(msg)
        return true
    }
}