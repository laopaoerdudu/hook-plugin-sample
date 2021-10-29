package com.dev.manager

import android.content.Context
import android.content.res.Resources
import com.dev.helper.PluginHelper
import dalvik.system.DexClassLoader

object PluginManager {
     var classLoader: DexClassLoader? = null
     var resources: Resources? = null

    fun setUp(context: Context?) {
        context?.let {
            classLoader = PluginHelper.getPluginClassLoader(it)
            resources = PluginHelper.getPluginResource(it)
        }
    }
}