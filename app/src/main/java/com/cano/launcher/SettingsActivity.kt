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
                ThemePalette.DARK_GRAY -> binding.themeDarkGray.id
                else -> binding.themePurple.id
            }
        )
        applyPalette()
        binding.themeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val themeId = when (checkedId) {
                binding.themeSage.id -> ThemePalette.SAGE
                binding.themeBlueGold.id -> ThemePalette.BLUE_GOLD
                binding.themeRedGray.id -> ThemePalette.RED_GRAY
                binding.themeDarkGray.id -> ThemePalette.DARK_GRAY
                else -> ThemePalette.PURPLE
            }
            settings.themeId = themeId
            applyPalette()
        }
        binding.backButton.setOnClickListener { finishWithTransition() }
        binding.favoritesFirstSwitch.post { binding.favoritesFirstSwitch.requestFocusFromTouch() }
    }

    override fun onBackPressed() {
        finishWithTransition()
    }

    private fun finishWithTransition() {
        finish()
        overridePendingTransition(R.anim.screen_fade_in, R.anim.screen_fade_out)
    }

    private fun applyPalette() {
        palette = ThemePalette.forId(settings.themeId, resources.displayMetrics.density)
        binding.settingsRoot.setBackgroundColor(palette.canvas)
        binding.settingsTitle.setTextColor(palette.text)
        binding.favoritesFirstSwitch.setTextColor(palette.text)
        binding.favoritesFirstSummary.setTextColor(palette.muted)
        binding.themeTitle.setTextColor(palette.text)
        binding.favoritesFirstSwitch.background = palette.settingsOptionBackground()
        listOf(
            binding.themePurple,
            binding.themeSage,
            binding.themeBlueGold,
            binding.themeRedGray,
            binding.themeDarkGray
        ).forEach {
            it.setTextColor(palette.text)
            it.buttonTintList = ColorStateList.valueOf(palette.accent)
            it.background = palette.settingsOptionBackground(showChecked = true)
        }
        binding.favoritesFirstSwitch.thumbTintList = ColorStateList.valueOf(palette.accent)
        binding.backButton.background = palette.settingsOptionBackground()
        binding.backButton.setTextColor(palette.text)
    }
}
