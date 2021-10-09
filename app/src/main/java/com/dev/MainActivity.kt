package com.dev

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.dev.manager.PluginManager
import com.dev.util.ReflectUtil

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ReflectUtil.setup()
        PluginManager.setup(applicationContext)
        PluginManager.hookActivityThreadInstrumentation()
        PluginManager.hookActivityInstrumentation(this)
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