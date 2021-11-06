package com.dev.framework

import android.content.Context
import com.dev.manager.HookServiceManager
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

class IActivityManagerHandler(private val context: Context, private val rawIActivityManager: Any?) :
    InvocationHandler {

    override fun invoke(proxy: Any?, method: Method?, args: Array<Any>?): Any? {
        when (method?.name) {
            "startService" -> {
                HookServiceManager.replacePluginIntentWithProxyIntent(args)
            }

            "stopService" -> {
                HookServiceManager.stopService(context, args)
            }

            else -> {

            }
        }
        return method?.invoke(rawIActivityManager, args)
    }
}