package com.qxtx.idea.gradle.demo_rescheck

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.qxtx.idea.gradle.demo_rescheck.databinding.ActivityDemoBinding

class DemoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDemoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}