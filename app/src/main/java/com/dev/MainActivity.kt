package com.dev

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.dev.constant.HookConstant.Companion.PLUGIN_ACTIVITY
import com.dev.constant.HookConstant.Companion.PLUGIN_APK_NAME
import com.dev.constant.HookConstant.Companion.PLUGIN_PACKAGE_NAME
import com.dev.helper.FileHelper.Companion.copyAssetsFileToSystemDir
import com.dev.helper.FileHelper.Companion.getOptimizedDirectory
import com.dev.helper.PluginHelper
import com.dev.test.Constants
import com.dev.test.PluginManager
import dalvik.system.DexClassLoader
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var mPluginManager: PluginManager

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        copyAssetsFileToSystemDir(this, PLUGIN_APK_NAME)
        PluginHelper.loadPlugin(
            classLoader,
            listOf(File(getFileStreamPath(PLUGIN_APK_NAME.replace(".apk", ".dex")).absolutePath)),
            getOptimizedDirectory(this)
        )
        // AMSHookHelper.replacePluginIntentWithHostPlaceHolderIntent()
        //        AMSHookHelper.replaceHostPlaceHolderIntentWithPluginIntent()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mPluginManager = PluginManager.getInstance(applicationContext)
        mPluginManager.hookInstrumentation()
        mPluginManager.hookCurrentActivityInstrumentation(this)

//        ReflectHelper.setup()
//        PluginManager.setup(applicationContext)
//        PluginManager.hookActivityThreadInstrumentation()
//        PluginManager.hookActivityInstrumentation(this)

        findViewById<Button>(R.id.btnStartPlugin).setOnClickListener {
            // 加载普通的插件类
            val classLoader = DexClassLoader(
                getFileStreamPath(PLUGIN_APK_NAME).path,
                getOptimizedDirectory(this).absolutePath,
                null,
                classLoader
            )
            val classType = classLoader.loadClass("$PLUGIN_PACKAGE_NAME.Util")
            val result = classType.getDeclaredMethod("getAge").apply {
                isAccessible = true
            }.invoke(classType.newInstance()) as? Int
            Toast.makeText(this, "age = $result", Toast.LENGTH_LONG).show()
        }

        findViewById<Button>(R.id.btnStartPluginActivity).setOnClickListener {
            // 加载插件 Activity
            if (mPluginManager.loadPlugin(Constants.PLUGIN_PATH)) {
                startActivity(Intent().apply {
                    component = ComponentName(PLUGIN_PACKAGE_NAME, PLUGIN_ACTIVITY)
                })
            }
        }
    }
}