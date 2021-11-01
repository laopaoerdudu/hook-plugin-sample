package com.dev.framework.manager

import android.content.Context
import android.content.res.Resources
import com.dev.helper.PluginHelper
import dalvik.system.DexClassLoader

object PluginManager {
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
}