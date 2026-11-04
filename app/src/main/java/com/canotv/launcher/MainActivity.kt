package com.cano.launcher

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cano.launcher.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.appTitle.text = getString(R.string.app_name)
        binding.appSubtitle.text = getString(R.string.launcher_status)
    }
}
