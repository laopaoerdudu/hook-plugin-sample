package com.dev.manager

import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import android.os.Build
import java.lang.ref.WeakReference
import java.lang.reflect.Method

object ResourceHookManager {
    var multipleResources: Resources? = null

    fun setup(context: Context, apkPath: String) {
        // multipleResources?.getQuantityString()
        preloadResource(context, apkPath)
    }

    fun getDrawableId(resName: String, packageName: String): Int? {
        var imgId = multipleResources?.getIdentifier(resName, "mipmap", packageName)
        if (imgId == 0) {
            imgId = multipleResources?.getIdentifier(resName, "drawable", packageName)
        }
        return imgId
    }

    fun getMultipleResource(): Resources? = multipleResources

    // 将插件的资源合并到宿主中,创建合并后的 Resource 替换掉宿主原有的 Resource
    @Synchronized
    private fun preloadResource(context: Context, apkPath: String) {
        try {

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        val assetManager = AssetManager::class.java.newInstance()
        val addAssetPathMethod: Method =
            AssetManager::class.java.getDeclaredMethod("addAssetPath", String::class.java).apply {
                isAccessible = true
            }
        with(addAssetPathMethod) {
            invoke(assetManager, context.packageResourcePath)
            invoke(assetManager, apkPath)
        }

        // 创建合并资源后的 Resource
        val resources = Resources(
            assetManager,
            context.resources.displayMetrics,
            context.resources.configuration
        )

        // 1，替换 ContextImpl 中的 Resource 对象
        val mResourcesFieldForContextImpl =
            context::class.java.getDeclaredField("mResources").apply {
                isAccessible = true
            }
        mResourcesFieldForContextImpl.set(context, resources)

        // 获取 LoadApk 对象
        val mPackageInfoField = context::class.java.getDeclaredField("mPackageInfo").apply {
            isAccessible = true
        }
        val loadApk = mPackageInfoField.get(context)

        // 2，替换掉 LoadApk 中的 Resource 对象
        val mResourcesFieldForLoadApk = loadApk::class.java.getDeclaredField("mResources").apply {
            isAccessible = true
        }
        mResourcesFieldForLoadApk.set(loadApk, resources)

        // 获取 ActivityThread
        val sCurrentActivityThreadField =
            Class.forName("android.app.ActivityThread").getDeclaredField("sCurrentActivityThread")
                .apply {
                    isAccessible = true
                }
        val sCurrentActivityThread = sCurrentActivityThreadField.get(null)

        // 获取 ResourceManager
        val mResourcesManagerField =
            Class.forName("android.app.ActivityThread").getDeclaredField("mResourcesManager")
                .apply {
                    isAccessible = true
                }
        val mResourcesManager = mResourcesManagerField.get(sCurrentActivityThread)

        // 3， 替换 ResourceManager 中 resource
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            val mActiveResourcesField =
                mResourcesManager::class.java.getDeclaredField("mActiveResources").apply {
                    isAccessible = true
                }
            val map =
                (mActiveResourcesField.get(mResourcesManager) as? MutableMap<Any, WeakReference<Resources>>)
            map?.keys?.forEach { key ->
                map[key] = WeakReference(resources)
            }
        } else {
            val mResourceImplsFied =
                mResourcesManager::class.java.getDeclaredField("mResourceImpls").apply {
                    isAccessible = true
                }
            val map =
                mResourceImplsFied.get(mResourcesManager) as? MutableMap<Any, WeakReference<Resources>>

            val mResourcesImplField =
                Resources::class.java.getDeclaredField("mResourcesImpl").apply {
                    isAccessible = true
                }
            val resourcesImpl = mResourcesImplField.get(resources) as? Resources
            map?.keys?.forEach { key ->
                map[key] = WeakReference(resourcesImpl)
            }
        }
        multipleResources = resources
    }

    // How to use?
    // private void usePluginResource() {
    //        ImageView imageView = findViewById(R.id.main_show_plugin_img_iv);
    //        imageView.setImageDrawable(ResourceHookManager.getDrawable("plugin_img", PluginConfig.package_name));
    //    }
}