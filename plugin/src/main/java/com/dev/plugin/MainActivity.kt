package com.dev.plugin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Toast.makeText(this, "插件 Activity 启动成功", Toast.LENGTH_LONG).show()
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        Log.i("WWE", "Plugin onResume >>>")
    }

    override fun onPause() {
        super.onPause()
        Log.i("WWE", "Plugin onPause >>>")
    }

    override fun onStop() {
        super.onStop()
        Log.i("WWE", "Plugin onStop >>>")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("WWE", "Plugin onDestroy >>>")
    }
}