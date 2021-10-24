package com.dev

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.dev.constant.HookConstant.Companion.PLUGIN_APK_NAME
import com.dev.helper.FileHelper.Companion.copyAssetsFileToSystemDir
import com.dev.helper.FileHelper.Companion.getOptimizedDirectory
import com.dev.helper.PluginHelper
import dalvik.system.DexClassLoader
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        copyAssetsFileToSystemDir(this, PLUGIN_APK_NAME)
        PluginHelper.loadPlugin(
            classLoader,
            listOf(File(getFileStreamPath(PLUGIN_APK_NAME.replace(".apk", ".dex")).absolutePath)),
            getOptimizedDirectory(this)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.btnStartPlugin).setOnClickListener {
            val classLoader = DexClassLoader(
                getFileStreamPath(PLUGIN_APK_NAME).path,
                getOptimizedDirectory(this).absolutePath,
                null,
                classLoader
            )
            val classType = classLoader.loadClass("com.dev.plugin.Util")
            val result = classType.getDeclaredMethod("getAge").apply {
                isAccessible = true
            }.invoke(classType.newInstance()) as? Int
            Toast.makeText(this, "age = $result", Toast.LENGTH_LONG).show()
        }

        findViewById<Button>(R.id.btnStartPluginActivity).setOnClickListener {

        }
    }
}