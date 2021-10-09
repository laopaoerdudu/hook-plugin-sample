package com.dev.framework

import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.content.res.Resources
import android.os.Build
import android.os.Handler
import android.os.Message
import java.lang.reflect.Proxy

class HookHelper {
    companion object {
        // 应用向 AMS 发起请求，启动插件 Activity，替换 Intent，欺骗 AMS
        // 将启动的插件的 Intent 替换成启动宿主 Activity 的 Intent
        fun replacePluginIntentWithHostProxyIntent() {
            try {
                // 获取 Singleton<IActivityManager>
                var singleton: Any?
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val iActivityManagerSingletonField =
                        Class.forName("android.app.ActivityManager")
                            ?.getDeclaredField("IActivityManagerSingleton")?.apply {
                                isAccessible = true
                            }
                    singleton = iActivityManagerSingletonField?.get(null)
                } else {
                    val gDefault = Class.forName("android.app.ActivityManagerNative")
                        ?.getDeclaredField("gDefault")?.apply {
                            isAccessible = true
                        }
                    singleton = gDefault?.get(null)
                }

                // 获取 mInstance 对象
                val mInstanceField =
                    Class.forName("android.util.Singleton")?.getDeclaredField("mInstance")?.apply {
                        isAccessible = true
                    }

                // 真正的 IActivityManager 实例
                val mInstance = mInstanceField?.get(singleton)

                // 创建 IActivityManager 的代理对象
                val proxyInstance = Proxy.newProxyInstance(
                    Thread.currentThread().contextClassLoader,
                    arrayOf(Class.forName("android.app.IActivityManager"))
                ) { _, method, args ->
                    args ?: return@newProxyInstance -1
                    if ("startActivity" == method?.name) {
                        var index = -1
                        for (i in args.indices) {
                            if (args[i] is Intent) {
                                index = i
                                break
                            }
                        }
                        val oldIntent = args[index] as? Intent
                        val proxyIntent = Intent().apply {
                            setClassName(PACKAGE_NAME, ProxyActivity::class.java.name)
                            putExtra(START_PLUGIN_INTENT, oldIntent)
                        }
                        // 替换 Intent
                        args[index] = proxyIntent
                    }
                    method?.invoke(mInstance, args)
                }
                if (-1 != proxyInstance) {
                    // 替换系统的 IActivityManager
                    mInstanceField?.set(singleton, proxyInstance)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        // AMS 校验成功之后，调用此方法
        // 将启动的宿主的 Activity 的 Intent 替换成插件 Activity 的 Intent
        fun replaceProxyIntentWithPluginIntent() {
            try {
                val sCurrentActivityThreadField = Class.forName("android.app.ActivityThread")
                    ?.getDeclaredField("sCurrentActivityThread")?.apply {
                        isAccessible = true
                    }
                // 获取 ActivityThread 对象
                val activityThread = sCurrentActivityThreadField?.get(null)

                // 获取 Handler 对象
                val mHField =
                    Class.forName("android.app.ActivityThread")?.getDeclaredField("mH")?.apply {
                        isAccessible = true
                    }
                val mH = mHField?.get(activityThread)

                // 创建一个 Callback 替换系统的 Callback 对象
                val mCallbackField =
                    Class.forName("android.os.Handler")?.getDeclaredField("mCallback")?.apply {
                        isAccessible = true
                    }
                mCallbackField?.set(mH, object : Handler.Callback {
                    override fun handleMessage(msg: Message): Boolean {
                        when (msg.what) {
                            100 -> {
                                try {
                                    val intentField =
                                        msg.obj.javaClass.getDeclaredField("intent")?.apply {
                                            isAccessible = true
                                        }
                                    val proxyIntent = intentField?.get(msg.obj) as? Intent
                                    (proxyIntent?.getParcelableExtra(START_PLUGIN_INTENT) as? Intent)?.let { pluginIntent ->
                                        intentField.set(msg.obj, pluginIntent)
                                    }
                                } catch (ex: Exception) {
                                    ex.printStackTrace()
                                }
                            }
                            159 -> {
                                // public static final int EXECUTE_TRANSACTION = 159;
                                try {
                                    val mActivityCallbacksField =
                                        Class.forName("android.app.servertransaction.ClientTransaction")
                                            ?.getDeclaredField("mActivityCallbacks")?.apply {
                                                isAccessible = true
                                            }
                                    val activityCallbacks =
                                        mActivityCallbacksField?.get(msg.obj) as? List<*>
                                    activityCallbacks ?: return false
                                    for (i in activityCallbacks.indices) {
                                        if ("android.app.servertransaction.LaunchActivityItem" == activityCallbacks[i]?.javaClass?.name) {
                                            val launchActivityItem = activityCallbacks[i]
                                            val mIntentField =
                                                launchActivityItem?.javaClass?.getDeclaredField("mIntent")
                                                    ?.apply {
                                                        isAccessible = true
                                                    }
                                            val proxyIntent =
                                                mIntentField?.get(launchActivityItem) as? Intent
                                            (proxyIntent?.getParcelableExtra(START_PLUGIN_INTENT) as? Intent)?.let { pluginIntent ->
                                                mIntentField.set(launchActivityItem, pluginIntent)
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
                        return false
                    }
                })
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        fun loadResource(context: Context, apkPath: String): Resources? {
            try {
                val assetManager = AssetManager::class.java.getDeclaredConstructor()?.newInstance()
                val addAssetPath =
                    assetManager?.javaClass?.getDeclaredMethod("addAssetPath", String::class.java)
                        ?.apply {
                            isAccessible = true
                        }
                addAssetPath?.invoke(assetManager, apkPath) // apkPath -> 插件的资源路径
                return Resources(
                    assetManager,
                    context.resources?.displayMetrics,
                    context.resources?.configuration
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            return null
        }


    }
}