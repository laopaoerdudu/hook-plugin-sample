package com.dev.helper

import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import android.os.Build
import android.os.Handler
import com.dev.framework.ActivityThreadHandlerCallback
import com.dev.framework.IActivityManagerHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy

class AMSHookHelper {
    companion object {
        // 应用向 AMS 发起请求，启动插件 Activity，替换 Intent，欺骗 AMS
        // 将启动的插件的 Intent 替换成占位 Activity 的 Intent
        fun replacePluginIntentWithHostPlaceHolderIntent() {
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
                    val gDefaultField = Class.forName("android.app.ActivityManagerNative")
                        ?.getDeclaredField("gDefault")?.apply {
                            isAccessible = true
                        }
                    singleton = gDefaultField?.get(null)
                }

                // 获取 mInstance 对象
                val mInstanceField =
                    Class.forName("android.util.Singleton")?.getDeclaredField("mInstance")?.apply {
                        isAccessible = true
                    }

                // 真正的 IActivityManager 实例
                val rawIActivityManager = mInstanceField?.get(singleton)

                // 创建 IActivityManager 的代理对象，替换它, 让代理对象帮忙干活
                val proxyIActivityManager = Proxy.newProxyInstance(
                    Thread.currentThread().contextClassLoader,
                    arrayOf(Class.forName("android.app.IActivityManager")),
                    IActivityManagerHandler(rawIActivityManager)
                )

                // 替换系统的 IActivityManager
                mInstanceField?.set(singleton, proxyIActivityManager)
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

        // 我们用替身欺骗了 AMS; 现在我们要换回我们真正需要启动的插件 Activity
        // AMS 校验成功之后，调用此方法
        // 将宿主的占位 Activity 的 Intent 替换成插件 Activity 的 Intent
        fun replaceHostPlaceHolderIntentWithPluginIntent() {
            try {
                val sCurrentActivityThreadField = Class.forName("android.app.ActivityThread")
                    ?.getDeclaredField("sCurrentActivityThread")?.apply {
                        isAccessible = true
                    }

                // 获取 ActivityThread 对象
                val sCurrentActivityThread = sCurrentActivityThreadField?.get(null)

                // 获取 Handler 对象
                val mHField =
                    Class.forName("android.app.ActivityThread")?.getDeclaredField("mH")?.apply {
                        isAccessible = true
                    }
                val mH = mHField?.get(sCurrentActivityThread)

                // 创建一个 Callback 替换系统的 Callback 对象
                val mCallbackField =
                    Class.forName("android.os.Handler")?.getDeclaredField("mCallback")?.apply {
                        isAccessible = true
                    }

                mCallbackField?.set(mH, ActivityThreadHandlerCallback(mH as? Handler))
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