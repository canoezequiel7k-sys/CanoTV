package com.cano.launcher

import android.content.res.ColorStateList
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cano.launcher.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private val settings by lazy { SettingsStore(this) }
    private lateinit var palette: ThemePalette

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        palette = ThemePalette.forId(settings.themeId, resources.displayMetrics.density)

        binding.favoritesFirstSwitch.isChecked = settings.favoritesFirst
        binding.favoritesFirstSwitch.setOnCheckedChangeListener { _, checked ->
            settings.favoritesFirst = checked
        }
        binding.themeRadioGroup.check(
            when (settings.themeId) {
                ThemePalette.SAGE -> binding.themeSage.id
                ThemePalette.BLUE_GOLD -> binding.themeBlueGold.id
                ThemePalette.RED_GRAY -> binding.themeRedGray.id
                else -> binding.themePurple.id
            }
        )
        applyPalette()
        binding.themeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val themeId = when (checkedId) {
                binding.themeSage.id -> ThemePalette.SAGE
                binding.themeBlueGold.id -> ThemePalette.BLUE_GOLD
                binding.themeRedGray.id -> ThemePalette.RED_GRAY
                else -> ThemePalette.PURPLE
            }
            settings.themeId = themeId
            applyPalette()
        }
        binding.backButton.setOnClickListener { finish() }
        binding.favoritesFirstSwitch.requestFocus()
    }

    private fun applyPalette() {
        palette = ThemePalette.forId(settings.themeId, resources.displayMetrics.density)
        binding.settingsRoot.setBackgroundColor(palette.canvas)
        binding.settingsTitle.setTextColor(palette.text)
        binding.favoritesFirstSwitch.setTextColor(palette.text)
        binding.favoritesFirstSummary.setTextColor(palette.muted)
        binding.themeTitle.setTextColor(palette.text)
        listOf(binding.themePurple, binding.themeSage, binding.themeBlueGold, binding.themeRedGray).forEach {
            it.setTextColor(palette.text)
            it.buttonTintList = ColorStateList.valueOf(palette.accent)
        }
        binding.favoritesFirstSwitch.thumbTintList = ColorStateList.valueOf(palette.accent)
        binding.backButton.backgroundTintList = ColorStateList.valueOf(palette.surfaceSelected)
        binding.backButton.setTextColor(palette.text)
    }
}
