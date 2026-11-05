package com.cano.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ActivityNotFoundException
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.KeyEvent
import android.view.ViewConfiguration
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnNextLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cano.launcher.databinding.ActivityHomeBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class HomeActivity : AppCompatActivity() {
    private enum class Section { HOME, FAVORITES, ALL }

    private lateinit var binding: ActivityHomeBinding
    private lateinit var adapter: AppAdapter
    private lateinit var palette: ThemePalette
    private val repository by lazy { AppRepository(packageManager) }
    private val favorites by lazy { FavoritesStore(this) }
    private val settings by lazy { SettingsStore(this) }
    private val worker: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val loadGeneration = AtomicInteger()
    private var allApps = emptyList<AppEntry>()
    private var allVisibleApps = emptyList<AppEntry>()
    private var gridKeyDownTime = 0L
    private var section = Section.HOME
    private var receiverRegistered = false
    private val refreshApps = Runnable { loadApps() }
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            mainHandler.removeCallbacks(refreshApps)
            mainHandler.postDelayed(refreshApps, 350)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        palette = ThemePalette.forId(settings.themeId, resources.displayMetrics.density)
        adapter = AppAdapter(
            onAppSelected = ::launchApp,
            onFavoriteToggle = ::toggleFavorite,
            onGridKey = ::handleGridKey,
            palette = palette
        )
        binding.appsRecyclerView.layoutManager = GridLayoutManager(this, APP_GRID_COLUMNS)
        binding.appsRecyclerView.adapter = adapter
        binding.appsRecyclerView.setHasFixedSize(true)
        binding.appsRecyclerView.setOnKeyListener { _, _, event -> handleGridKey(event) }

        binding.navHome.setOnClickListener { showSection(Section.HOME) }
        binding.navFavorites.setOnClickListener { showSection(Section.FAVORITES) }
        binding.navAllApps.setOnClickListener { showSection(Section.ALL) }
        binding.navStore.setOnClickListener { openAppStore() }
        binding.navSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.navAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
        listOf(
            binding.navHome,
            binding.navFavorites,
            binding.navAllApps,
            binding.navStore,
            binding.navSettings,
            binding.navAbout
        ).forEach { item ->
            item.setOnKeyListener { _, keyCode, event ->
                if (event.action == android.view.KeyEvent.ACTION_DOWN &&
                    keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT
                ) {
                    focusFirstApp()
                } else {
                    false
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        registerReceiver(packageReceiver, filter)
        receiverRegistered = true
    }

    override fun onResume() {
        super.onResume()
        section = if (settings.favoritesFirst) Section.HOME else Section.ALL
        applyPalette()
        loadApps()
    }

    override fun onStop() {
        if (receiverRegistered) {
            unregisterReceiver(packageReceiver)
            receiverRegistered = false
        }
        mainHandler.removeCallbacks(refreshApps)
        super.onStop()
    }

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }

    override fun onBackPressed() {
        // Home remains available when Back is pressed.
    }

    private fun loadApps() {
        val generation = loadGeneration.incrementAndGet()
        worker.execute {
            val apps = repository.getLaunchableApps(packageName)
            mainHandler.post {
                if (!isFinishing && !isDestroyed && generation == loadGeneration.get()) {
                    allApps = apps
                    favorites.removeMissingApps(apps)
                    renderApps()
                }
            }
        }
    }

    private fun applyPalette() {
        palette = ThemePalette.forId(settings.themeId, resources.displayMetrics.density)
        binding.homeBackground.setImageResource(palette.backgroundRes)
        binding.homeBackgroundOverlay.setBackgroundColor(palette.overlay)
        binding.homeRoot.setBackgroundColor(palette.canvas)
        binding.sidebar.setBackgroundColor(palette.sidebar)
        (binding.sidebar.getChildAt(0) as? android.widget.TextView)?.setTextColor(palette.text)
        listOf(
            binding.navHome,
            binding.navFavorites,
            binding.navAllApps,
            binding.navStore,
            binding.navSettings,
            binding.navAbout
        ).forEach { navigationItem ->
            navigationItem.background = palette.navigationBackground()
        }
        styleNavigation()
        binding.welcomeTitle.setTextColor(palette.text)
        binding.clock.setTextColor(palette.text)
        binding.welcomeSubtitle.setTextColor(palette.muted)
        binding.exploreApps.setBackgroundColor(palette.surface)
        binding.exploreApps.setTextColor(palette.text)
        binding.appsTitle.setTextColor(palette.text)
        binding.favoriteHint.setTextColor(palette.muted)
        binding.emptyAppsMessage.setTextColor(palette.muted)
        binding.appsRecyclerView.setBackgroundColor(palette.gridBackground)
        adapter.setPalette(palette)
    }

    private fun renderApps() {
        val favoriteKeys = favorites.getFavorites()
        val visibleApps = when (section) {
            Section.HOME -> if (settings.favoritesFirst) {
                allApps.sortedByDescending { "${it.packageName}/${it.activityName}" in favoriteKeys }
            } else {
                allApps
            }
            Section.FAVORITES -> allApps.filter {
                "${it.packageName}/${it.activityName}" in favoriteKeys
            }
            Section.ALL -> allApps
        }
        adapter.submitItems(visibleApps, favoriteKeys)
        allVisibleApps = visibleApps
        binding.emptyAppsMessage.visibility =
            if (visibleApps.isEmpty()) View.VISIBLE else View.GONE
        binding.emptyAppsMessage.setText(
            if (section == Section.FAVORITES) R.string.no_favorites else R.string.no_launchable_apps
        )
        binding.appsTitle.setText(
            when (section) {
                Section.FAVORITES -> R.string.favorites_title
                Section.ALL -> R.string.all_apps_title
                Section.HOME -> R.string.apps_title
            }
        )
        binding.navHome.isSelected = section == Section.HOME
        binding.navFavorites.isSelected = section == Section.FAVORITES
        binding.navAllApps.isSelected = section == Section.ALL
        styleNavigation()
        val position = adapter.getSelectedPosition()
        if (position != RecyclerView.NO_POSITION) {
            focusFirstApp(position)
        }
    }

    private fun styleNavigation() {
        listOf(
            binding.navHome,
            binding.navFavorites,
            binding.navAllApps,
            binding.navStore,
            binding.navSettings,
            binding.navAbout
        ).forEach { navigationItem ->
            val selected = navigationItem.isSelected
            (navigationItem.getChildAt(1) as? android.widget.TextView)
                ?.setTextColor(if (selected) palette.text else palette.muted)
            (navigationItem.getChildAt(0) as? android.widget.ImageView)
                ?.setColorFilter(if (selected) palette.text else palette.muted)
        }
    }

    private fun focusFirstApp(
        position: Int = adapter.getSelectedPosition(),
        retriesRemaining: Int = 4
    ): Boolean {
        if (position == RecyclerView.NO_POSITION) return false
        adapter.selectPosition(position)
        binding.appsRecyclerView.scrollToPosition(position)
        binding.appsRecyclerView.requestFocus()
        binding.appsRecyclerView.post {
            val card = binding.appsRecyclerView.findViewHolderForAdapterPosition(position)?.itemView
            if (card != null) {
                card.requestFocus()
            } else if (retriesRemaining > 0) {
                binding.appsRecyclerView.postDelayed(
                    { focusFirstApp(position, retriesRemaining - 1) },
                    50
                )
            } else {
                binding.appsRecyclerView.doOnNextLayout {
                    focusFirstApp(position, retriesRemaining - 1)
                }
            }
        }
        return true
    }

    private fun handleGridKey(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN && event.action != KeyEvent.ACTION_UP) {
            return false
        }
        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (event.action != KeyEvent.ACTION_DOWN) return true
                val current = adapter.getSelectedPosition()
                val column = current % APP_GRID_COLUMNS
                val next = when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> if (column > 0) current - 1 else null
                    KeyEvent.KEYCODE_DPAD_RIGHT ->
                        if (column < APP_GRID_COLUMNS - 1) current + 1 else null
                    KeyEvent.KEYCODE_DPAD_UP ->
                        if (current >= APP_GRID_COLUMNS) current - APP_GRID_COLUMNS else null
                    else -> current + APP_GRID_COLUMNS
                } ?: return false
                if (next !in allVisibleApps.indices) return false
                focusFirstApp(next)
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    if (event.repeatCount == 0) gridKeyDownTime = event.downTime
                    return true
                }
                val app = allVisibleApps.getOrNull(adapter.getSelectedPosition()) ?: return false
                return if (event.eventTime - gridKeyDownTime >=
                    ViewConfiguration.getLongPressTimeout()
                ) {
                    toggleFavorite(app)
                } else {
                    launchApp(app)
                    true
                }
            }
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_STAR -> {
                if (event.action == KeyEvent.ACTION_UP) {
                    allVisibleApps.getOrNull(adapter.getSelectedPosition())?.let(::toggleFavorite)
                }
                return true
            }
            else -> return false
        }
    }

    private fun showSection(newSection: Section) {
        section = newSection
        renderApps()
    }

    private fun toggleFavorite(app: AppEntry): Boolean {
        val added = favorites.toggle(app)
        Toast.makeText(
            this,
            if (added) R.string.favorite_added else R.string.favorite_removed,
            Toast.LENGTH_SHORT
        ).show()
        renderApps()
        return true
    }

    private fun openAppStore() {
        val store = allApps.firstOrNull {
            it.packageName in setOf("com.android.vending", "com.xiaomi.mipicks", "com.xiaomi.market")
        } ?: allApps.firstOrNull {
            it.label.contains("play store", ignoreCase = true) ||
                it.label.contains("app store", ignoreCase = true) ||
                it.label.contains("getapps", ignoreCase = true) ||
                it.label.contains("tienda", ignoreCase = true)
        }
        if (store != null) {
            launchApp(store)
            return
        }

        val fileManager = allApps.firstOrNull {
            it.label.contains("explorador", ignoreCase = true) ||
                it.label.contains("file manager", ignoreCase = true) ||
                it.label.contains("files", ignoreCase = true)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.store_unavailable_title)
            .setMessage(R.string.store_unavailable_message)
            .setNegativeButton(android.R.string.cancel, null)
        if (fileManager != null) {
            dialog.setPositiveButton(R.string.open_file_manager) { _, _ -> launchApp(fileManager) }
        }
        dialog.show()
    }

    private fun launchApp(app: AppEntry) {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(app.launchCategory)
                setClassName(app.packageName, app.activityName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
            startActivity(intent)
        } catch (exception: ActivityNotFoundException) {
            showLaunchError()
            loadApps()
        } catch (exception: SecurityException) {
            showLaunchError()
            loadApps()
        }
    }

    private fun showLaunchError() {
        Toast.makeText(this, R.string.app_launch_failed, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val APP_GRID_COLUMNS = 3
    }
}
