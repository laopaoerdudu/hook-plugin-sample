package com.dev.manager

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.content.res.Resources
import com.dev.framework.HookedInstrumentation
import com.dev.framework.PluginApp
import com.dev.helper.ReflectHelper
import com.dev.helper.ReflectHelper.getField
import com.dev.helper.ReflectHelper.getMethod
import com.dev.util.safeLeft
import dalvik.system.DexClassLoader
import java.io.File
import java.io.IOException
import java.lang.reflect.InvocationTargetException

object PluginManager {
    private var mContext: Context? = null
    private var mPluginApp: PluginApp? = null

    fun setup(context: Context) {
        this.mContext = context
    }

    fun getPluginApp(): PluginApp? = mPluginApp

    @SuppressLint("StaticFieldLeak")
    fun hookActivityThreadInstrumentation() {
        try {
            safeLeft(
                ReflectHelper.mInstrumentation,
                ReflectHelper.sCurrentActivityThread
            ) { mInstrumentation, sCurrentActivityThread ->
                val hookInstrumentation = HookedInstrumentation(mInstrumentation, this)
                ReflectHelper.setActivityThreadInstrumentation(
                    sCurrentActivityThread,
                    hookInstrumentation
                )
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun hookActivityInstrumentation(activity: Activity) {
        ReflectHelper.setActivityInstrumentation(activity, this)
    }

    fun isPluginExit(apkPath: String): Boolean {
        if (!File(apkPath).exists()) {
            return false
        }
        setPluginApp(apkPath)?.let {
            return true
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

    // Host: ClassLoader -> DexPathList -> Element[]
    // plugin: DexPathList -> private static makePathElements(dexPath) -> Element[]
    // List<File> ->  listOf(File(getFileStreamPath(apkName.replace(".apk", ".dex")).absolutePath))
    fun loadPlugin(
        hostClassLoader: ClassLoader,
        dexFiles: List<File>,
        optimizedDirectory: File
    ) {
        try {
            //private final DexPathList pathList
            getField(hostClassLoader, "pathList")?.get(hostClassLoader)?.let { hostDexPathList ->
                // private Element[] dexElements
                var hostDexElementsField = getField(hostDexPathList, "dexElements")
                var hostDexElements = hostDexElementsField?.apply {
                    isAccessible = true
                }?.get(hostDexPathList) as? Array<Any>

                var pluginDexElements = getMethod(
                    hostDexPathList,
                    "makePathElements",
                    List::class.java,
                    File::class.java,
                    List::class.java
                )?.invoke(
                    null,
                    dexFiles,
                    optimizedDirectory,
                    ArrayList<IOException>()
                ) as? Array<Any>

                safeLeft(hostDexElements, pluginDexElements) { hostElements, pluginElements ->
                    val newElements = java.lang.reflect.Array.newInstance(
                        hostElements::class.java.componentType, // Class<?> componentType
                        hostElements.size + pluginElements.size
                    ) as? Array<Any>

                    // insert the plugin data to new array head
                    // copy data from pluginElements to newElements
                    System.arraycopy(
                        pluginElements,
                        0,
                        newElements,
                        0,
                        pluginElements.size
                    )

                    // copy data from appElements to newElements
                    System.arraycopy(
                        hostElements,
                        0,
                        newElements,
                        pluginElements.size,
                        hostElements.size
                    )

                    hostDexElementsField?.apply {
                        isAccessible = true
                    }?.set(hostDexPathList, newElements)
                }
            }
        } catch (ex: IllegalAccessException) {
            ex.printStackTrace()
        } catch (ex: InvocationTargetException) {
            ex.printStackTrace()
        }
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

    private fun getPluginClassLoader(apkPath: String): DexClassLoader {
        return DexClassLoader(
            mContext?.getFileStreamPath(apkPath)?.path, // dexPath
            mContext?.getDir("dex", Context.MODE_PRIVATE)?.absolutePath, // optimizedDirectory
            null, // librarySearchPath
            mContext?.classLoader // parent
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