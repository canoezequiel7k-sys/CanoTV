package com.cano.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ActivityNotFoundException
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnNextLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cano.launcher.databinding.ActivityHomeBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var adapter: AppAdapter
    private val repository by lazy { AppRepository(packageManager) }
    private val worker: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val loadGeneration = AtomicInteger()
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

        adapter = AppAdapter { app -> launchApp(app) }
        binding.appsRecyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        binding.appsRecyclerView.adapter = adapter
        binding.appsRecyclerView.setHasFixedSize(true)
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
                    adapter.submitItems(apps)
                    binding.emptyAppsMessage.visibility =
                        if (apps.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                    val position = adapter.getSelectedPosition()
                    if (position != RecyclerView.NO_POSITION) {
                        binding.appsRecyclerView.scrollToPosition(position)
                        binding.appsRecyclerView.doOnNextLayout {
                            binding.appsRecyclerView.findViewHolderForAdapterPosition(position)
                                ?.itemView?.requestFocus()
                        }
                    }
                }
            }
        }
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
        }
    }

    private fun showLaunchError() {
        Toast.makeText(this, R.string.app_launch_failed, Toast.LENGTH_SHORT).show()
    }
}
