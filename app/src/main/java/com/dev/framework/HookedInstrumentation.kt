package com.dev.framework

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import com.dev.constant.HookConstant.Companion.HOST_APP_PACKAGE_NAME
import com.dev.constant.HookConstant.Companion.HOST_PLACE_HOLDER_ACTIVITY
import com.dev.constant.HookConstant.Companion.KEY_ACTIVITY
import com.dev.constant.HookConstant.Companion.KEY_IS_PLUGIN
import com.dev.constant.HookConstant.Companion.KEY_PACKAGE
import com.dev.manager.PluginManager
import com.dev.util.safeLeft

class HookedInstrumentation(private val rawInstrumentation: Instrumentation) :
    Instrumentation(),
    Handler.Callback {

    override fun callActivityOnCreate(activity: Activity?, icicle: Bundle?) {
        super.callActivityOnCreate(activity, icicle)
    }

    override fun handleMessage(message: Message): Boolean {
        return false
    }

    override fun newActivity(cl: ClassLoader?, className: String?, intent: Intent?): Activity {
        try {
            if (isPluginIntent(intent)) {
                val activity = rawInstrumentation.newActivity(
                    PluginManager.classLoader,
                    intent?.component?.className,
                    intent
                )
                activity?.intent = intent
                AMSHookManager.hookResource(activity, PluginManager.resources)
                return activity
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return super.newActivity(cl, className, intent)
    }

    fun execStartActivity(
        who: Context,
        contextThread: IBinder,
        token: IBinder,
        target: Activity,
        intent: Intent?,
        requestCode: Int,
        options: Bundle?
    ): ActivityResult? {
        val targetPackageName = intent?.component?.packageName
        val targetClassName = intent?.component?.className
        if (HOST_APP_PACKAGE_NAME != targetPackageName) {
            intent?.apply {
                setClassName(HOST_APP_PACKAGE_NAME, HOST_PLACE_HOLDER_ACTIVITY)
                putExtra(KEY_IS_PLUGIN, true)
                putExtra(KEY_PACKAGE, targetPackageName)
                putExtra(KEY_ACTIVITY, targetClassName)
            }
        }
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
            val execStartActivityMethod = Instrumentation::class.java.getDeclaredMethod(
                "execStartActivity", *parameterTypes
            ).apply {
                isAccessible = true
            }

            return execStartActivityMethod.invoke(
                rawInstrumentation,
                parameterValues
            ) as? ActivityResult
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }

    private fun isPluginIntent(intent: Intent?): Boolean {
        if (intent?.getBooleanExtra(KEY_IS_PLUGIN, false) == true) {
            val _pkg = intent.getStringExtra(KEY_PACKAGE)
            val _activity = intent.getStringExtra(KEY_ACTIVITY)
            safeLeft(_pkg, _activity) { pkg, activity ->
                intent.setClassName(pkg, activity)
            }
            return true
        }
        return false
    }
}