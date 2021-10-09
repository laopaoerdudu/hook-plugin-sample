package com.dev.manager

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.content.res.Resources
import com.dev.framework.HookedInstrumentation
import com.dev.framework.PluginApp
import com.dev.util.ReflectUtil
import com.dev.util.safeLeft
import dalvik.system.DexClassLoader
import java.io.File
import java.lang.reflect.InvocationTargetException

object PluginManager {
    private var mContext: Context? = null
    private var mPluginApp: PluginApp? = null

    fun setup(context: Context) {
        this.mContext = context
    }

    @SuppressLint("StaticFieldLeak")
    fun hookActivityThreadInstrumentation() {
        try {
            safeLeft(
                ReflectUtil.mInstrumentation,
                ReflectUtil.sCurrentActivityThread
            ) { mInstrumentation, sCurrentActivityThread ->
                val hookInstrumentation = HookedInstrumentation(mInstrumentation, this)
                ReflectUtil.setActivityThreadInstrumentation(
                    sCurrentActivityThread,
                    hookInstrumentation
                )
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun hookActivityInstrumentation(activity: Activity) {
        ReflectUtil.setActivityInstrumentation(activity, this)
    }

    fun setPluginIntent(intent: Intent) {
        val targetPackageName = intent.component?.packageName
        val targetClassName = intent.component?.className // STUB_ACTIVITY
        if (targetPackageName.isNullOrBlank()) return
        if (mContext?.packageName == targetPackageName && isPluginLoaded(targetPackageName)) {
            intent.apply {
                // TODO: STUB_PACKAGE / STUB_ACTIVITY
                setClassName("com.dev", "hello activity")
                putExtra("isPlugin", true)
                putExtra("package", targetPackageName)
                putExtra("activity", targetClassName)
            }
        }
    }

    fun isPluginIntent(intent: Intent?): Boolean {
        if (intent?.getBooleanExtra("isPlugin", false) == true) {
            safeLeft(
                intent.getStringExtra("package"),
                intent.getStringExtra("activity")
            ) { pkg, activity ->
                intent.setClassName(pkg, activity)
            }
            return true
        }
        return false
    }

    fun getPluginApp(): PluginApp? = mPluginApp

    fun isPluginExit(apkPath: String): Boolean {
        if (!File(apkPath).exists()) {
            return false
        }
        setPluginApp(apkPath)?.let {
            return true
        }
    }

    private fun getPluginClassLoader(apkPath: String): DexClassLoader {
        return DexClassLoader(
            apkPath,
            mContext?.getDir("dex", Context.MODE_PRIVATE)?.absolutePath,
            null,
            mContext?.classLoader
        )
    }

    private fun isPluginLoaded(packageName: String): Boolean {
        // TODO: 检查 packageName 是否匹配
        return true
    }

    private fun setPluginApp(apkPath: String) {
        try {
            val assetManager = AssetManager::class.java.newInstance()
            val method =
                AssetManager::class.java.getDeclaredMethod("addAssetPath", String::class.java)
                    .apply {
                        isAccessible = true
                    }
            method.invoke(assetManager, apkPath)
            mPluginApp = PluginApp(
                Resources(
                    assetManager,
                    mContext?.resources?.displayMetrics,
                    mContext?.resources?.configuration
                ),
                getPluginClassLoader(apkPath)
            )
        } catch (ex: IllegalAccessException) {
            ex.printStackTrace()
        } catch (ex: InstantiationException) {
            ex.printStackTrace()
        } catch (ex: NoSuchMethodException) {
            ex.printStackTrace()
        } catch (ex: InvocationTargetException) {
            ex.printStackTrace()
        }
    }
}