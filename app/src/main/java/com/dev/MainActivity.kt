package com.dev

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.dev.manager.PluginManager
import com.dev.helper.ReflectHelper

class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        ReflectHelper.setup()
        PluginManager.setup(applicationContext)
        PluginManager.hookActivityThreadInstrumentation()
        PluginManager.hookActivityInstrumentation(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.btnStartPlugin).setOnClickListener {
            // Environment.getExternalStorageDirectory() + "/plugin/plugin.apk";Environment.getExternalStorageDirectory() + "/plugin/plugin.apk";

            if(PluginManager.isPluginExit("")) {
                startActivity(Intent().apply {
                    setClassName("plugin-pkg", "plugin-activity")
                })
            }
        }
    }
}