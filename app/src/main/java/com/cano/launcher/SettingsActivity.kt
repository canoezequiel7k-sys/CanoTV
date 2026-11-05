package com.cano.launcher

import android.content.res.ColorStateList
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.cano.launcher.databinding.ActivitySettingsBinding

/** Pantalla de preferencias de CanoTV y selector de temas. */
class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private val settings by lazy { SettingsStore(this) }
    private lateinit var palette: ThemePalette

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleContext.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        palette = ThemePalette.forId(settings.themeId, resources.displayMetrics.density)

        binding.favoritesFirstSwitch.isChecked = settings.favoritesFirst
        binding.favoritesFirstSwitch.setOnCheckedChangeListener { _, checked ->
            settings.favoritesFirst = checked
        }
        binding.languageOption.setOnClickListener { showLanguageDialog() }
        binding.appFontOption.setOnClickListener { showFontDialog() }
        binding.appSizeOption.setOnClickListener { showSizeDialog() }
        updatePreferenceLabels()
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
        listOf(binding.languageOption, binding.appFontOption, binding.appSizeOption).forEach {
            it.setTextColor(palette.text)
            it.background = palette.settingsOptionBackground()
        }
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

    private fun updatePreferenceLabels() {
        binding.languageOption.text = getString(
            R.string.language_setting,
            getString(
                if (settings.language == SettingsStore.LANGUAGE_ENGLISH) {
                    R.string.language_english
                } else {
                    R.string.language_spanish
                }
            )
        )
        binding.appFontOption.text = getString(
            R.string.app_font_setting,
            getString(
                when (settings.appFont) {
                    SettingsStore.FONT_ROUNDED -> R.string.font_rounded
                    SettingsStore.FONT_SERIF -> R.string.font_serif
                    else -> R.string.font_sans
                }
            )
        )
        binding.appSizeOption.text = getString(
            R.string.app_size_setting,
            settings.appLabelSize
        )
    }

    private fun showLanguageDialog() {
        val selected = if (settings.language == SettingsStore.LANGUAGE_ENGLISH) 1 else 0
        showChoiceDialog(
            R.string.language_setting_title,
            R.array.language_choices,
            selected
        ) { index ->
            settings.language = if (index == 1) {
                SettingsStore.LANGUAGE_ENGLISH
            } else {
                SettingsStore.LANGUAGE_SPANISH
            }
            recreate()
        }
    }

    private fun showFontDialog() {
        val selected = when (settings.appFont) {
            SettingsStore.FONT_ROUNDED -> 1
            SettingsStore.FONT_SERIF -> 2
            else -> 0
        }
        showChoiceDialog(R.string.app_font_setting_title, R.array.app_font_choices, selected) {
            settings.appFont = when (it) {
                1 -> SettingsStore.FONT_ROUNDED
                2 -> SettingsStore.FONT_SERIF
                else -> SettingsStore.FONT_SANS
            }
            updatePreferenceLabels()
        }
    }

    private fun showSizeDialog() {
        val options = intArrayOf(16, 18, 20)
        val selected = options.indexOf(settings.appLabelSize).coerceAtLeast(0)
        showChoiceDialog(R.string.app_size_setting_title, R.array.app_size_choices, selected) {
            settings.appLabelSize = options[it]
            updatePreferenceLabels()
        }
    }

    private fun showChoiceDialog(
        title: Int,
        choices: Int,
        selected: Int,
        onSelected: (Int) -> Unit
    ) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setSingleChoiceItems(choices, selected) { dialog, which ->
                dialog.dismiss()
                onSelected(which)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
