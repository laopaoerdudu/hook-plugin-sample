package com.dev

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dev.constant.HookConstant.Companion.PLUGIN_ACTIVITY
import com.dev.constant.HookConstant.Companion.PLUGIN_APK_NAME
import com.dev.constant.HookConstant.Companion.PLUGIN_PACKAGE_NAME
import com.dev.manager.HookActivityManager
import com.dev.helper.PluginHelper
import com.dev.helper.FileHelper
import com.dev.manager.HookBroadCastManager
import com.dev.manager.HookServiceManager
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        FileHelper.copyAssetsFileToSystemDir(this, PLUGIN_APK_NAME)
        PluginHelper.loadPlugin(
            classLoader,
            listOf(File(getFileStreamPath(PLUGIN_APK_NAME.replace(".apk", ".dex")).absolutePath)),
            FileHelper.getOptimizedDirectory(this)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        HookActivityManager.setUp(applicationContext)
        HookActivityManager.hookActivityThreadInstrumentation()
        HookActivityManager.hookActivityInstrumentation(this)
        HookBroadCastManager.loadBroadcast(applicationContext)

        // HookServiceManager.hookIActivityManager(applicationContext)
        // HookServiceManager.preLoadServices(FileHelper.getFile(this))
        findViewById<Button>(R.id.btnStartPlugin).setOnClickListener {
            val classType = HookActivityManager.classLoader?.loadClass("$PLUGIN_PACKAGE_NAME.Util")
            val result = classType?.getDeclaredMethod("getAge")?.apply {
                isAccessible = true
            }?.invoke(classType.newInstance()) as? Int
            Toast.makeText(this, "age = $result", Toast.LENGTH_LONG).show()
        }

        findViewById<Button>(R.id.btnStartPluginActivity).setOnClickListener {
            startActivity(Intent().apply {
                component = ComponentName(PLUGIN_PACKAGE_NAME, PLUGIN_ACTIVITY)
            })
        }

        findViewById<Button>(R.id.btnStartPluginBroadCast).setOnClickListener {
            // TODO: fix `java.lang.ClassCastException: android.content.pm.PackageParser$Activity cannot be cast to android.app.Activity`
            sendBroadcast(Intent().apply {
                action = "com.dev.plugin.receiver.PluginReceiver"
            })
        }

        findViewById<Button>(R.id.btnStartPluginDynamicBroadCast).setOnClickListener {
            val `DynamicBroadcast` =
                HookActivityManager.classLoader?.loadClass("$PLUGIN_PACKAGE_NAME.receiver.DynamicBroadcast")
            `DynamicBroadcast`?.getDeclaredMethod(
                "onReceive",
                Context::class.java,
                Intent::class.java
            )?.invoke(`DynamicBroadcast`.newInstance(), this, Intent().apply {
                action = "com.dev.plugin.receiver.DynamicBroadcast"
            })
        }

        findViewById<Button>(R.id.btnStartPluginService).setOnClickListener {
            startService(Intent().apply {
                component =
                    ComponentName(PLUGIN_PACKAGE_NAME, "com.dev.plugin.service.PluginService")
            })
        }

        findViewById<Button>(R.id.btnStopPluginService).setOnClickListener {
            stopService(Intent().apply {
                component =
                    ComponentName(PLUGIN_PACKAGE_NAME, "com.dev.plugin.service.PluginService")
            })
        }
    }
}