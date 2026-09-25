package com.freellmapi.android

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val tv = findViewById<TextView>(R.id.tvStatus)
        tv.text = "FreeLLMAPI v1.0.0"
    }
}