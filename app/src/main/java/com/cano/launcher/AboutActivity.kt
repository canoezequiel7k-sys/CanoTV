package com.cano.launcher

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.StatFs
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cano.launcher.databinding.ActivityAboutBinding
import java.util.Locale

class AboutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAboutBinding
    private lateinit var palette: ThemePalette

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleContext.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        palette = ThemePalette.forId(
            SettingsStore(this).themeId,
            resources.displayMetrics.density
        )
        binding.aboutRoot.setBackgroundColor(palette.canvas)
        binding.aboutTitle.setTextColor(palette.text)
        binding.aboutMessage.setTextColor(palette.muted)
        binding.backButton.backgroundTintList = ColorStateList.valueOf(palette.surfaceSelected)
        binding.backButton.setTextColor(palette.text)

        val deviceName = listOf(Build.MANUFACTURER, Build.MODEL)
            .filter(String::isNotBlank)
            .distinct()
            .joinToString(" ")
            .ifBlank { getString(R.string.unknown_value) }
        addInfoRow(R.string.device_model, deviceName)
        addInfoRow(
            R.string.android_version,
            getString(R.string.android_version_format, Build.VERSION.RELEASE, Build.VERSION.SDK_INT)
        )
        addInfoRow(R.string.build_device, Build.DEVICE)
        addInfoRow(
            R.string.architecture,
            Build.SUPPORTED_ABIS.firstOrNull() ?: getString(R.string.unknown_value)
        )
        addInfoRow(R.string.display, getDisplayDescription())
        addInfoRow(R.string.memory, getMemoryDescription())
        addInfoRow(R.string.storage, getStorageDescription())
        addInfoRow(R.string.graphics, getGraphicsDescription())
        addInfoRow(
            R.string.app_version,
            packageManager.getPackageInfo(packageName, 0).versionName ?: getString(R.string.unknown_value)
        )
        binding.backButton.setOnClickListener { finish() }
        binding.backButton.requestFocus()
    }

    private fun addInfoRow(labelRes: Int, value: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 5, 16, 5)
            background = palette.infoCardBackground()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = resources.getDimensionPixelSize(R.dimen.device_info_spacing)
            }
        }
        row.addView(TextView(this).apply {
            text = getString(labelRes)
            setTextColor(palette.muted)
            textSize = 12f
        })
        row.addView(TextView(this).apply {
            text = value.ifBlank { getString(R.string.unknown_value) }
            setTextColor(palette.text)
            textSize = 16f
        })
        binding.deviceInfoList.addView(row)
    }

    private fun getDisplayDescription(): String {
        val display = windowManager.defaultDisplay
        val size = android.graphics.Point()
        display.getRealSize(size)
        val density = resources.displayMetrics.densityDpi
        return getString(R.string.display_format, size.x, size.y, density)
    }

    private fun getMemoryDescription(): String {
        val memoryInfo = ActivityManager.MemoryInfo()
        (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memoryInfo)
        return getString(
            R.string.memory_format,
            formatBytes(memoryInfo.totalMem),
            formatBytes(memoryInfo.availMem)
        )
    }

    private fun getStorageDescription(): String {
        val stats = StatFs(dataDir.absolutePath)
        return getString(
            R.string.storage_format,
            formatBytes(stats.totalBytes),
            formatBytes(stats.availableBytes)
        )
    }

    private fun getGraphicsDescription(): String {
        val glVersion = (getSystemService(ACTIVITY_SERVICE) as ActivityManager)
            .deviceConfigurationInfo.reqGlEsVersion
        val major = glVersion shr 16
        val minor = glVersion and 0xffff
        return if (glVersion == 0) {
            getString(R.string.unknown_value)
        } else {
            getString(R.string.opengl_format, major, minor)
        }
    }

    private fun formatBytes(bytes: Long): String {
        val gibibytes = bytes / (1024.0 * 1024 * 1024)
        return getString(R.string.gib_format, String.format(Locale.getDefault(), "%.1f", gibibytes))
    }
}
