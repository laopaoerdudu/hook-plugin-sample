package com.dev.helper

import com.dev.util.safeLeft
import java.io.File
import java.io.IOException
import java.lang.reflect.InvocationTargetException

class PluginHelper {
    companion object {
        // Host: ClassLoader -> DexPathList -> Element[]
        // plugin: DexPathList -> private static makePathElements(dexPath) -> Element[]
        // List<File> ->  listOf(File(getFileStreamPath(apkName.replace(".apk", ".dex")).absolutePath))
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
    }
}