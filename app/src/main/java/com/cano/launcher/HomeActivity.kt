package com.cano.launcher

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cano.launcher.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var adapter: AppAdapter
    private val repository by lazy { AppRepository(packageManager) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = AppAdapter { app -> launchApp(app) }
        binding.appsRecyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        binding.appsRecyclerView.adapter = adapter
        binding.appsRecyclerView.setHasFixedSize(true)
        binding.appsRecyclerView.requestFocus()

        binding.appsRecyclerView.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false

            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    adapter.moveSelection(-1)
                    scrollSelectionIntoView()
                    true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    adapter.moveSelection(1)
                    scrollSelectionIntoView()
                    true
                }
                KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                    adapter.getSelectedApp()?.let { launchApp(it) }
                    true
                }
                KeyEvent.KEYCODE_BACK -> {
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadApps()
    }

    override fun onBackPressed() {
        // El launcher no debe cerrar la sesión ni salir del sistema.
    }

    private fun loadApps() {
        val fallbackIcon = packageManager.getApplicationIcon("android")
        val visibleApps = repository.getLaunchableApps(packageName).take(6)
        val apps = visibleApps.ifEmpty {
            listOf(
                AppEntry("android", "android", "Android", fallbackIcon),
                AppEntry("com.android.settings", "com.android.settings.Settings", "Ajustes", fallbackIcon),
                AppEntry("com.android.vending", "com.android.vending.AssetBrowserActivity", "Play Store", fallbackIcon),
                AppEntry("com.google.android.youtube", "com.google.android.youtube.Shell\$HomeActivity", "YouTube", fallbackIcon),
                AppEntry("com.netflix.mediaclient", "com.netflix.mediaclient.ui.launch.UIWebViewActivity", "Netflix", fallbackIcon)
            )
        }
        adapter.submitItems(apps)
        binding.appsRecyclerView.post {
            binding.appsRecyclerView.scrollToPosition(0)
        }
    }

    private fun launchApp(app: AppEntry) {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setClassName(app.packageName, app.activityName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
            startActivity(intent)
        } catch (_: RuntimeException) {
            // Se ignora un error puntual de aplicación externa durante la prueba del launcher.
        }
    }

    private fun scrollSelectionIntoView() {
        val selectedPosition = adapter.getSelectedPosition()
        if (selectedPosition == RecyclerView.NO_POSITION) return
        binding.appsRecyclerView.scrollToPosition(selectedPosition)
    }
}
