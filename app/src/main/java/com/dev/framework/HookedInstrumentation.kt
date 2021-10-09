package com.dev.framework

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.util.Log
import android.view.ContextThemeWrapper
import com.dev.manager.PluginManager
import com.dev.helper.ReflectHelper

class HookedInstrumentation(
    private val instrumentation: Instrumentation,
    private val pluginManager: PluginManager
) : Instrumentation(), Handler.Callback {
    fun execStartActivity(
        who: Context,
        contextThread: IBinder,
        token: IBinder,
        target: Activity,
        intent: Intent,
        requestCode: Int,
        options: Bundle
    ): ActivityResult? {
        pluginManager.setPluginIntent(intent)
        Log.d("WWE", "Hook Instrumentation #execStartActivity is ok, who -> $who")
        val parameterTypes = arrayOf(
            Context::class.java,
            IBinder::class.java,
            IBinder::class.java,
            Activity::class.java,
            Intent::class.java,
            Int::class.java,
            Bundle::class.java
        )
        val parameterValues = arrayOf(
            who,
            contextThread,
            token,
            target,
            intent,
            requestCode,
            options
        )
        try {
            val method =
                Instrumentation::class.java.getDeclaredMethod("execStartActivity", *parameterTypes)
                    .apply {
                        isAccessible = true
                    }
            return method.invoke(instrumentation, parameterValues) as? ActivityResult
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }

    override fun newActivity(
        classLoader: ClassLoader?,
        className: String?,
        intent: Intent?
    ): Activity {
        try {
            if (pluginManager.isPluginIntent(intent)) {
                val activity = instrumentation.newActivity(
                    pluginManager.getPluginApp()?.mClassLoader,
                    intent?.component?.className,
                    intent
                )
                activity.intent = intent
                ReflectHelper.setField(
                    ContextThemeWrapper::class.java,
                    activity,
                    "mResources",
                    pluginManager.getPluginApp()?.mResources
                )
                return activity
            }
        } catch (ex: ClassNotFoundException) {
            ex.printStackTrace()
        } catch (ex: IllegalAccessException) {
            ex.printStackTrace()
        } catch (ex: InstantiationException) {
            ex.printStackTrace()
        }
        return super.newActivity(classLoader, className, intent)
    }

    override fun callActivityOnCreate(activity: Activity?, icicle: Bundle?) {
        Log.i("WWE", "HookedInstrumentation #callActivityOnCreate >>>")
        super.callActivityOnCreate(activity, icicle)
    }

    override fun handleMessage(message: Message): Boolean {
        Log.i("WWE", "HookedInstrumentation #handleMessage >>>")
        return false
    }
}