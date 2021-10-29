package com.dev.helper

import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import com.dev.constant.HookConstant
import com.dev.util.safeLeft
import dalvik.system.DexClassLoader
import java.io.File
import java.io.IOException
import java.lang.reflect.InvocationTargetException

class PluginHelper {
    companion object {
        // Host: ClassLoader -> DexPathList -> Element[]
        // plugin: DexPathList -> private static makePathElements(dexPath) -> Element[]
        fun loadPlugin(
            hostClassLoader: ClassLoader,
            dexFiles: List<File>,
            optimizedDirectory: File  // 解压出来的 dex 文件路径 (/data/data/<Package Name>/…)
        ) {
            try {
                //private final DexPathList pathList
                ReflectHelper.getField(hostClassLoader, "pathList")
                    ?.get(hostClassLoader)?.let { hostDexPathList ->

                        // private Element[] dexElements
                        var hostDexElementsField =
                            ReflectHelper.getField(hostDexPathList, "dexElements")
                        var hostDexElements = hostDexElementsField?.apply {
                            isAccessible = true
                        }?.get(hostDexPathList) as? Array<Any>

                        var pluginDexElements = ReflectHelper.getMethod(
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

                        safeLeft(
                            hostDexElements,
                            pluginDexElements
                        ) { hostElements, pluginElements ->
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

        fun getPluginClassLoader(context: Context): DexClassLoader {
            return DexClassLoader(
                context.getFileStreamPath(HookConstant.PLUGIN_APK_NAME).path,
                FileHelper.getOptimizedDirectory(context).absolutePath,
                null,
                context.classLoader // Thread.currentThread().contextClassLoader
            )
        }

        fun getPluginResource(context: Context): Resources? {
            try {
                val AssetManagerClass = AssetManager::class.java
                val assetManager = AssetManagerClass.newInstance()
                val addAssetPathMethod =
                    AssetManagerClass.getDeclaredMethod("addAssetPath", String::class.java).apply {
                        isAccessible = true
                    }
                addAssetPathMethod.invoke(
                    assetManager,
                    context.getFileStreamPath(HookConstant.PLUGIN_APK_NAME).path
                )
                return Resources(
                    assetManager,
                    context.resources.displayMetrics,
                    context.resources.configuration
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
            return null
        }

        fun isPluginExist(context: Context): Boolean {
            return getPluginClassLoader(context) != null && getPluginResource(context) != null
        }
    }
}