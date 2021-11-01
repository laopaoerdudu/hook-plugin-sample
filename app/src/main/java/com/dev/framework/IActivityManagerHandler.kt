package com.dev.framework

import android.content.Context
import com.dev.constant.HookConstant.Companion.START_ACTIVITY_METHOD_NAME
import com.dev.manager.HookManager
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

@Deprecated("Temporarily useless")
class IActivityManagerHandler(context: Context?, private val rawIActivityManager: Any?) :
    InvocationHandler {

    override fun invoke(proxy: Any?, method: Method?, args: Array<Any>?): Any? {
        when (method?.name) {
            START_ACTIVITY_METHOD_NAME -> {
                HookManager.replacePluginIntentWithPlaceHolderIntent(args)
            }

            else -> {

            }
        }
        return method?.invoke(rawIActivityManager, args)
    }
}