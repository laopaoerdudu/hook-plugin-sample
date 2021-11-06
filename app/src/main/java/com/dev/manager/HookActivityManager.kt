package com.dev.manager

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.Handler
import android.os.Message
import androidx.appcompat.view.ContextThemeWrapper
import com.dev.constant.HookConstant.Companion.EXECUTE_TRANSACTION
import com.dev.constant.HookConstant.Companion.HOST_APP_PACKAGE_NAME
import com.dev.constant.HookConstant.Companion.HOST_PLACE_HOLDER_ACTIVITY
import com.dev.constant.HookConstant.Companion.KEY_ACTIVITY
import com.dev.constant.HookConstant.Companion.KEY_IS_PLUGIN
import com.dev.constant.HookConstant.Companion.KEY_PACKAGE
import com.dev.constant.HookConstant.Companion.KEY_RAW_INTENT
import com.dev.constant.HookConstant.Companion.LAUNCH_ACTIVITY
import com.dev.framework.ActivityThreadHandlerCallback
import com.dev.framework.HookedInstrumentation
import com.dev.framework.IActivityManagerHandler
import com.dev.helper.PluginHelper
import com.dev.util.safeLeft
import dalvik.system.DexClassLoader
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy

object HookActivityManager {
    var classLoader: DexClassLoader? = null
    var resources: Resources? = null
    var mContext: Context? = null

    fun setUp(context: Context?) {
        context?.let {
            mContext = it
            classLoader = PluginHelper.getPluginClassLoader(it)
            resources = PluginHelper.getPluginResource(it)
        }
    }

    fun hookActivityThreadInstrumentation() {
        try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val currentActivityThreadMethod =
                activityThreadClass.getDeclaredMethod("currentActivityThread").apply {
                    isAccessible = true
                }
            val currentActivityThread = currentActivityThreadMethod.invoke(null)

            // 获取 Instrumentation
            val mInstrumentationField =
                activityThreadClass.getDeclaredField("mInstrumentation").apply {
                    isAccessible = true
                }
            val mInstrumentation =
                mInstrumentationField.get(currentActivityThread) as? Instrumentation
            val hookedInstrumentation = HookedInstrumentation(mInstrumentation)
            mInstrumentationField.set(currentActivityThread, hookedInstrumentation)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun hookActivityInstrumentation(activity: Activity) {
        try {
            val mInstrumentationField =
                Activity::class.java.getDeclaredField("mInstrumentation").apply {
                    isAccessible = true
                }
            val mInstrumentation = mInstrumentationField.get(activity) as? Instrumentation
            val hookedInstrumentation = HookedInstrumentation(mInstrumentation)
            mInstrumentationField.set(activity, hookedInstrumentation)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun hookResource(activity: Activity?, value: Resources?) {
        try {
            val mResourcesField =
                ContextThemeWrapper::class.java.getDeclaredField("mResources").apply {
                    isAccessible = true
                }
            mResourcesField.set(activity, value)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun setPlaceHolderIntent(intent: Intent) {
        val targetPackageName = intent.component?.packageName
        val targetClassName = intent.component?.className
        if (mContext?.packageName != targetPackageName) {
            intent.apply {
                setClassName(HOST_APP_PACKAGE_NAME, HOST_PLACE_HOLDER_ACTIVITY)
                putExtra(KEY_IS_PLUGIN, true)
                putExtra(KEY_PACKAGE, targetPackageName)
                putExtra(KEY_ACTIVITY, targetClassName)
            }
        }
    }

    fun isPluginIntentSetup(intent: Intent): Boolean {
        if (intent.getBooleanExtra(KEY_IS_PLUGIN, false)) {
            safeLeft(
                intent.getStringExtra(KEY_PACKAGE),
                intent.getStringExtra(KEY_ACTIVITY)
            ) { pkg, activity ->
                intent.setClassName(pkg, activity)
            }
            return true
        }
        return false
    }

    @SuppressLint("StaticFieldLeak")
    fun isPluginIntent(intent: Intent): Boolean {
        return intent.getBooleanExtra(KEY_IS_PLUGIN, false)
    }

    fun getComponent(intent: Intent): ComponentName? {
        if(isPluginIntent(intent)) {
            safeLeft(
                intent.getStringExtra(KEY_PACKAGE),
                intent.getStringExtra(KEY_ACTIVITY)
            ) { pkg, activity ->
                return@safeLeft ComponentName(pkg, activity)
            }
        }
        return intent.component
    }

    @Deprecated("Temporarily useless")
    fun replacePlaceHolderIntentWithPluginIntent(msg: Message) {
        when (msg.what) {
            LAUNCH_ACTIVITY -> {
                try {
                    val activityClientRecordClass = msg.obj.javaClass
                    val intentField =
                        activityClientRecordClass.getDeclaredField("intent").apply {
                            isAccessible = true
                        }
                    val placeholderIntent = intentField.get(msg.obj) as? Intent

                    // 把正式启动的 intent 设置进去
                    (placeholderIntent?.getParcelableExtra(KEY_RAW_INTENT) as? Intent)?.let { pluginIntent ->
                        intentField.set(msg.obj, pluginIntent)
                        //placeholderIntent.setComponent(pluginIntent.component)
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
            EXECUTE_TRANSACTION -> {
                try {
                    val mActivityCallbacksField =
                        Class.forName("android.app.servertransaction.ClientTransaction")
                            ?.getDeclaredField("mActivityCallbacks")?.apply {
                                isAccessible = true
                            }
                    val mActivityCallbacks =
                        mActivityCallbacksField?.get(msg.obj) as? List<*>
                    mActivityCallbacks ?: return
                    for (i in mActivityCallbacks.indices) {
                        if ("android.app.servertransaction.LaunchActivityItem" == mActivityCallbacks[i]?.javaClass?.name) {
                            val launchActivityItem = mActivityCallbacks[i]
                            val mIntentField =
                                launchActivityItem?.javaClass?.getDeclaredField("mIntent")
                                    ?.apply {
                                        isAccessible = true
                                    }
                            val placeholderIntent =
                                mIntentField?.get(launchActivityItem) as? Intent
                            (placeholderIntent?.getParcelableExtra(KEY_RAW_INTENT) as? Intent)?.let { pluginIntent ->
                                mIntentField.set(launchActivityItem, pluginIntent)
                                //placeholderIntent.setComponent(pluginIntent.component)
                            }
                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
            else -> {
            }
        }
    }

    @Deprecated("Temporarily useless")
    fun hookIActivityManager(context: Context?) {
        try {
            var IActivityManagerField: Field?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                IActivityManagerField = Class.forName("android.app.ActivityManager")
                    .getDeclaredField("IActivityManagerSingleton").apply {
                        isAccessible = true
                    }
            } else {
                IActivityManagerField =
                    Class.forName("android.app.ActivityManagerNative")
                        .getDeclaredField("gDefault")
                        .apply {
                            isAccessible = true
                        }
            }

            // 获取 Singleton<IActivityManager>
            val singleton = IActivityManagerField?.get(null)

            // 取出单例里面的 IActivityManager
            val mInstanceField =
                Class.forName("android.util.Singleton").getDeclaredField("mInstance").apply {
                    isAccessible = true
                }
            val rawIActivityManager = mInstanceField.get(singleton)

            // 创建代理对象，让代理对象帮忙干活
            val proxyIActivityManager = Proxy.newProxyInstance(
                Thread.currentThread().contextClassLoader,
                arrayOf(Class.forName("android.app.IActivityManager")),
                IActivityManagerHandler(context, rawIActivityManager)
            )

            mInstanceField.set(singleton, proxyIActivityManager)
        } catch (ex: ClassNotFoundException) {
            ex.printStackTrace()
        } catch (ex: NoSuchMethodException) {
            ex.printStackTrace()
        } catch (ex: InvocationTargetException) {
            ex.printStackTrace()
        } catch (ex: IllegalAccessException) {
            ex.printStackTrace()
        } catch (ex: NoSuchFieldException) {
            ex.printStackTrace()
        }
    }

    @Deprecated("Temporarily useless")
    fun hookActivityThreadHandler() {
        // 获取 ActivityThread
        try {
            val ActivityThreadClass = Class.forName("android.app.ActivityThread")
            val sCurrentActivityThreadField =
                ActivityThreadClass.getDeclaredField("sCurrentActivityThread").apply {
                    isAccessible = true
                }
            val sCurrentActivityThread = sCurrentActivityThreadField.get(null)

            // 获取 ActivityThread 中的 handler
            val mH = ActivityThreadClass.getDeclaredField("mH").apply {
                isAccessible = true
            }.get(sCurrentActivityThread) as? Handler

            // 给 handler 添加 callback 监听器，拦截
            val mCallBackField = Handler::class.java.getDeclaredField("mCallback").apply {
                isAccessible = true
            }
            mCallBackField.set(mH, ActivityThreadHandlerCallback(mH))
        } catch (ex: ClassNotFoundException) {
            ex.printStackTrace()
        } catch (ex: NoSuchMethodException) {
            ex.printStackTrace()
        } catch (ex: InvocationTargetException) {
            ex.printStackTrace()
        } catch (ex: IllegalAccessException) {
            ex.printStackTrace()
        } catch (ex: NoSuchFieldException) {
            ex.printStackTrace()
        }
    }
}