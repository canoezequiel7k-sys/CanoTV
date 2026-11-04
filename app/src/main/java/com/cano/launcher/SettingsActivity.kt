package com.cano.launcher

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cano.launcher.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private val settings by lazy { SettingsStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.favoritesFirstSwitch.isChecked = settings.favoritesFirst
        binding.favoritesFirstSwitch.setOnCheckedChangeListener { _, checked ->
            settings.favoritesFirst = checked
        }
        binding.backButton.setOnClickListener { finish() }
        binding.favoritesFirstSwitch.requestFocus()
    }
}
