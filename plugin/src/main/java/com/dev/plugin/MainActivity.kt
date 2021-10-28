package com.dev.plugin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Toast.makeText(this, "插件 Activity 启动成功", Toast.LENGTH_LONG).show()
//        setContentView(TextView(this).apply {
//            text = "插件化"
//            gravity = Gravity.CENTER
//        })

       setContentView(R.layout.activity_main)
    }
}