package com.cano.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ActivityNotFoundException
import android.graphics.drawable.TransitionDrawable
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.provider.Settings
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.KeyEvent
import android.view.ViewConfiguration
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
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
    private var appliedThemeId: String? = null
    private var appliedLanguage: String? = null
    private var receiverRegistered = false
    private var networkCallbackRegistered = false
    private val refreshApps = Runnable { loadApps() }
    private val connectivityManager by lazy {
        getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = refreshWifiStatus()
        override fun onLost(network: Network) = refreshWifiStatus()
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) =
            refreshWifiStatus()
    }
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            mainHandler.removeCallbacks(refreshApps)
            mainHandler.postDelayed(refreshApps, 350)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleContext.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        appliedLanguage = resources.configuration.locales[0].language

        palette = ThemePalette.forId(settings.themeId, resources.displayMetrics.density)
        adapter = AppAdapter(
            onAppSelected = ::launchApp,
            onFavoriteToggle = ::toggleFavorite,
            onAppFocused = ::showRecommendation,
            onGridKey = ::handleGridKey,
            palette = palette
        )
        binding.appsRecyclerView.layoutManager = GridLayoutManager(this, APP_GRID_COLUMNS)
        binding.appsRecyclerView.adapter = adapter
        binding.appsRecyclerView.itemAnimator = null
        binding.appsRecyclerView.setHasFixedSize(true)
        binding.appsRecyclerView.setOnKeyListener { _, _, event -> handleGridKey(event) }
        binding.appsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateMoreAppsHint()
            }
        })

        binding.navHome.setOnClickListener { showSection(Section.HOME) }
        binding.navFavorites.setOnClickListener { showSection(Section.FAVORITES) }
        binding.navAllApps.setOnClickListener { showSection(Section.ALL) }
        binding.exploreApps.setOnClickListener { showSection(Section.ALL) }
        binding.recommendationCard.setOnClickListener {
            allVisibleApps.getOrNull(adapter.getSelectedPosition())?.let(::launchApp)
        }
        binding.headerSettings.setOnClickListener { openSystemSettings() }
        binding.navStore.setOnClickListener { openAppStore() }
        binding.navSettings.setOnClickListener { openSettings() }
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
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        networkCallbackRegistered = true
        refreshWifiStatus()
    }

    override fun onResume() {
        super.onResume()
        if (appliedLanguage != settings.language) {
            recreate()
            return
        }
        section = if (settings.favoritesFirst) Section.HOME else Section.ALL
        applyPalette()
        refreshWifiStatus()
        loadApps()
    }

    override fun onStop() {
        if (receiverRegistered) {
            unregisterReceiver(packageReceiver)
            receiverRegistered = false
        }
        if (networkCallbackRegistered) {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            networkCallbackRegistered = false
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
        val themeId = settings.themeId
        val themeChanged = appliedThemeId != null && appliedThemeId != themeId
        val previousBackground = binding.homeBackground.drawable
        palette = ThemePalette.forId(themeId, resources.displayMetrics.density)
        val nextBackground = resources.getDrawable(palette.backgroundRes)
        if (themeChanged && previousBackground != null) {
            val transition = TransitionDrawable(arrayOf(previousBackground, nextBackground)).apply {
                isCrossFadeEnabled = true
            }
            binding.homeBackground.setImageDrawable(transition)
            transition.startTransition(220)
            binding.homeBackground.postDelayed({
                if (binding.homeBackground.drawable === transition) {
                    binding.homeBackground.setImageDrawable(nextBackground)
                }
            }, 240)
        } else {
            binding.homeBackground.setImageDrawable(nextBackground)
        }
        appliedThemeId = themeId
        binding.homeBackgroundOverlay.setBackgroundColor(palette.overlay)
        binding.homeRoot.setBackgroundColor(palette.canvas)
        binding.sidebar.setBackgroundColor(palette.sidebar)
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
        styleLogo()
        binding.clock.setTextColor(palette.text)
        binding.wifiStatus.setTextColor(palette.muted)
        binding.wifiIcon.setColorFilter(palette.muted)
        binding.wifiDivider.setBackgroundColor(palette.muted)
        binding.clockDivider.setBackgroundColor(palette.muted)
        binding.headerSettings.setColorFilter(palette.muted)
        binding.welcomeSubtitle.setTextColor(palette.muted)
        binding.exploreApps.background = palette.appCardBackground()
        binding.exploreApps.setTextColor(palette.text)
        binding.appsTitle.setTextColor(palette.text)
        binding.recommendationTitle.setTextColor(palette.text)
        binding.recommendationAppName.setTextColor(palette.text)
        binding.recommendationDescription.setTextColor(palette.muted)
        binding.recommendationCard.background = palette.appCardBackground()
        binding.favoriteHint.setTextColor(palette.muted)
        binding.emptyAppsMessage.setTextColor(palette.muted)
        binding.appsRecyclerView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        binding.moreAppsHint.setColorFilter(palette.text)
        binding.moreAppsHint.background = palette.appCardBackground()
        adapter.setPalette(palette)
    }

    private fun renderApps(resetSelection: Boolean = false) {
        val isHome = section == Section.HOME
        adapter.setHorizontalItems(isHome)
        val currentLayoutManager = binding.appsRecyclerView.layoutManager
        if (isHome) {
            if (currentLayoutManager !is LinearLayoutManager ||
                currentLayoutManager is GridLayoutManager ||
                currentLayoutManager.orientation != RecyclerView.HORIZONTAL
            ) {
                binding.appsRecyclerView.layoutManager = LinearLayoutManager(
                    this,
                    RecyclerView.HORIZONTAL,
                    false
                )
            }
        } else if (currentLayoutManager !is GridLayoutManager) {
            binding.appsRecyclerView.layoutManager = GridLayoutManager(this, APP_GRID_COLUMNS)
        }
        val frameParams = binding.appsFrame.layoutParams as android.widget.LinearLayout.LayoutParams
        frameParams.height = if (isHome) dp(130) else dp(380)
        frameParams.weight = 0f
        binding.appsFrame.layoutParams = frameParams
        binding.appsRecyclerView.layoutParams = binding.appsRecyclerView.layoutParams.apply {
            height = if (isHome) ViewGroup.LayoutParams.MATCH_PARENT else dp(340)
        }
        binding.welcomeTitle.visibility = if (isHome) View.VISIBLE else View.INVISIBLE
        binding.welcomeSubtitle.visibility = if (isHome) View.VISIBLE else View.GONE
        binding.exploreApps.visibility = if (isHome) View.VISIBLE else View.GONE
        binding.recommendationTitle.visibility = if (isHome) View.VISIBLE else View.GONE
        binding.recommendationCard.visibility =
            if (isHome && allApps.isNotEmpty()) View.VISIBLE else View.GONE
        binding.favoriteHint.visibility = if (isHome) View.GONE else View.VISIBLE

        val favoriteKeys = favorites.getFavorites()
        val visibleApps = when (section) {
            Section.HOME -> {
                val homeApps = allApps.filterNot {
                    isDeviceSettingsApp(it) || isUnwantedHomeApp(it)
                }
                if (settings.favoritesFirst) {
                    homeApps.sortedByDescending {
                        "${it.packageName}/${it.activityName}" in favoriteKeys
                    }
                } else {
                    homeApps
                }
            }
            Section.FAVORITES -> allApps.filter {
                "${it.packageName}/${it.activityName}" in favoriteKeys
            }
            Section.ALL -> allApps
        }
        adapter.submitItems(visibleApps, favoriteKeys)
        if (resetSelection && visibleApps.isNotEmpty()) {
            adapter.selectPosition(0)
        }
        allVisibleApps = visibleApps
        adapter.setAppLabelStyle(settings.appLabelSize, settings.appFont)
        updateMoreAppsHint()
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
        binding.appsRecyclerView.post { updateMoreAppsHint() }
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

    private fun styleLogo() {
        binding.logoInitial.setTextColor(palette.text)
        binding.logoMiddle.setTextColor(palette.text)
        binding.logoSuffix.setTextColor(palette.accent)
    }

    private fun isUnwantedHomeApp(app: AppEntry): Boolean {
        val label = app.label.filter { it.isLetterOrDigit() }.lowercase()
        val packageName = app.packageName.filter { it.isLetterOrDigit() }.lowercase()
        return listOf(label, packageName).any {
            it in UNWANTED_HOME_APPS
        }
    }

    private fun isDeviceSettingsApp(app: AppEntry): Boolean =
        app.packageName == SYSTEM_SETTINGS_PACKAGE ||
            app.label.equals("Ajustes", ignoreCase = true) ||
            app.label.equals("Configuración", ignoreCase = true) ||
            app.label.equals("Settings", ignoreCase = true) ||
            app.label.equals("System settings", ignoreCase = true)

    private fun focusFirstApp(
        position: Int = adapter.getSelectedPosition(),
        retriesRemaining: Int = 4
    ): Boolean {
        if (position == RecyclerView.NO_POSITION) return false
        adapter.selectPosition(position)
        binding.appsRecyclerView.scrollToPosition(position)
        binding.appsRecyclerView.post {
            val card = binding.appsRecyclerView.findViewHolderForAdapterPosition(position)?.itemView
            if (card?.requestFocusFromTouch() != true && retriesRemaining > 0) {
                binding.appsRecyclerView.postDelayed(
                    { focusFirstApp(position, retriesRemaining - 1) },
                    50
                )
            }
        }
        return true
    }

    private fun showRecommendation(app: AppEntry) {
        if (section != Section.HOME) return
        binding.recommendationAppIcon.setImageDrawable(app.banner ?: app.icon)
        binding.recommendationAppName.text = app.label
        binding.recommendationDescription.setText(
            if (app.packageName in RECOMMENDATION_APP_PACKAGES) {
                R.string.recommendation_description
            } else {
                R.string.recommendation_generic_description
            }
        )
        binding.recommendationCard.contentDescription =
            getString(
                if (app.packageName in RECOMMENDATION_APP_PACKAGES) {
                    R.string.recommendation_open
                } else {
                    R.string.recommendation_continue
                },
                app.label
            )
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
        overridePendingTransition(R.anim.screen_fade_in, R.anim.screen_fade_out)
    }

    private fun openSystemSettings() {
        try {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        } catch (exception: ActivityNotFoundException) {
            Toast.makeText(this, R.string.system_settings_unavailable, Toast.LENGTH_SHORT).show()
        } catch (exception: SecurityException) {
            Toast.makeText(this, R.string.system_settings_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshWifiStatus() {
        val connected = connectivityManager.activeNetworkInfo?.let {
            it.type == ConnectivityManager.TYPE_WIFI && it.isConnected
        } == true
        runOnUiThread {
            binding.wifiStatus.setText(
                if (connected) R.string.wifi_connected else R.string.wifi_disconnected
            )
            binding.wifiStatus.setTextColor(if (connected) palette.accent else palette.muted)
            binding.wifiIcon.setImageResource(
                if (connected) R.drawable.ic_wifi else R.drawable.ic_wifi_off
            )
            binding.wifiIcon.setColorFilter(if (connected) palette.accent else palette.muted)
            binding.wifiIcon.contentDescription = getString(
                if (connected) R.string.wifi_connected else R.string.wifi_disconnected
            )
        }
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
                if (section == Section.HOME) {
                    if (event.keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                        event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                    ) return false
                    val next = if (event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        current - 1
                    } else {
                        current + 1
                    }
                    if (next !in allVisibleApps.indices) return false
                    focusFirstApp(next)
                    return true
                }
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
        renderApps(resetSelection = true)
    }

    private fun updateMoreAppsHint() {
        val recyclerView = binding.appsRecyclerView
        val atTop = !recyclerView.canScrollVertically(-1)
        binding.moreAppsHint.visibility = if (
            section != Section.HOME &&
            allVisibleApps.size > APP_GRID_COLUMNS * 2 &&
            (atTop || recyclerView.canScrollVertically(1))
        ) View.VISIBLE else View.GONE
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

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
        private const val SYSTEM_SETTINGS_PACKAGE = "com.android.settings"
        private val UNWANTED_HOME_APPS = setOf("dlna", "wifidisplay", "mediacenter")
        private val RECOMMENDATION_APP_PACKAGES = setOf(
            "com.netflix.mediaclient",
            "com.google.android.youtube",
            "com.google.android.youtube.tv",
            "com.disney.disneyplus",
            "com.amazon.avod.thirdpartyclient",
            "com.hbo.hbonow",
            "com.spotify.music"
        )
    }
}
