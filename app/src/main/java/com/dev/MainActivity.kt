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
import com.dev.manager.HookManager
import com.dev.helper.PluginHelper
import com.dev.helper.FileHelper
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
        HookManager.setUp(applicationContext)
        HookManager.hookActivityThreadInstrumentation()
        HookManager.hookActivityInstrumentation(this)
        findViewById<Button>(R.id.btnStartPlugin).setOnClickListener {
            val classType = HookManager.classLoader?.loadClass("$PLUGIN_PACKAGE_NAME.Util")
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
    }
}