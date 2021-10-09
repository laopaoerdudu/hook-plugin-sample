package com.dev.util

import android.app.Activity
import android.app.Instrumentation
import com.dev.framework.HookedInstrumentation
import com.dev.manager.PluginManager
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException

object ReflectUtil {
    var mInstrumentation: Instrumentation? = null // ActivityThread 的 Instrumentation 实例
    var sCurrentActivityThread: Any? = null  // ActivityThread 实例
    var activityMInstrumentation: Instrumentation? = null
    private var mInstrumentationField: Field? = null
    private var activityMInstrumentationField: Field? = null

    fun setup() {
        try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")

            // public static ActivityThread currentActivityThread() { return sCurrentActivityThread; }
            val currentActivityThreadMethod =
                activityThreadClass.getDeclaredMethod("currentActivityThread").apply {
                    isAccessible = true
                }
            sCurrentActivityThread = currentActivityThreadMethod.invoke(null)

            // Instrumentation mInstrumentation
            mInstrumentationField =
                activityThreadClass.getDeclaredField("mInstrumentation").apply {
                    isAccessible = true
                }
            mInstrumentation =
                mInstrumentationField?.get(sCurrentActivityThread) as? Instrumentation

            // private Instrumentation mInstrumentation;
            activityMInstrumentationField =
                Activity::class.java.getDeclaredField("mInstrumentation").apply {
                    isAccessible = true
                }
        } catch (ex: ClassNotFoundException) {
            ex.printStackTrace()
        } catch (ex: NoSuchMethodException) {
            ex.printStackTrace()
        } catch (ex: IllegalAccessException) {
            ex.printStackTrace()
        } catch (ex: InvocationTargetException) {
            ex.printStackTrace()
        } catch (ex: NoSuchFieldException) {
            ex.printStackTrace()
        }
    }

    fun setActivityThreadInstrumentation(
        activityThread: Any,
        hookInstrumentation: HookedInstrumentation
    ) {
        try {
            mInstrumentationField?.set(activityThread, hookInstrumentation)
        } catch (ex: IllegalAccessException) {
            ex.printStackTrace()
        }
    }

    fun setActivityInstrumentation(activity: Activity, manager: PluginManager) {
        try {
            activityMInstrumentation =
                activityMInstrumentationField?.get(activity) as? Instrumentation
            activityMInstrumentation?.let { instrumentation ->
                activityMInstrumentationField?.set(
                    activity,
                    HookedInstrumentation(instrumentation, manager)
                )
            }
        } catch (ex: IllegalAccessException) {
            ex.printStackTrace()
        }
    }

    fun setField(classType: Class<*>, instance: Any, fieldName: String, value: Any?) {
        try {
            classType.getDeclaredField(fieldName).apply {
                isAccessible = true
            }.set(instance, value)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}