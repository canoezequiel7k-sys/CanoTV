package com.cano.launcher

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
        binding.backgroundStatus.text = if (settings.backgroundUri == null) {
            getString(R.string.default_background_active)
        } else {
            getString(R.string.custom_background_active)
        }
        binding.chooseBackgroundButton.setOnClickListener { chooseBackground() }
        binding.resetBackgroundButton.setOnClickListener {
            settings.backgroundUri = null
            binding.backgroundStatus.setText(R.string.default_background_active)
        }
        binding.backButton.setOnClickListener { finish() }
        binding.favoritesFirstSwitch.requestFocus()
    }

    private fun chooseBackground() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        try {
            startActivityForResult(intent, REQUEST_BACKGROUND)
        } catch (_: ActivityNotFoundException) {
            openBuiltInImagePicker()
        }
    }

    @Deprecated("Required for compatibility with the Android 7.1 file picker")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_BACKGROUND || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        if (data.flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                Toast.makeText(this, R.string.image_permission_failed, Toast.LENGTH_LONG).show()
                return
            }
        }
        settings.backgroundUri = uri
        binding.backgroundStatus.setText(R.string.custom_background_active)
    }

    private fun openBuiltInImagePicker() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_STORAGE_PERMISSION
            )
            return
        }
        startActivityForResult(Intent(this, ImagePickerActivity::class.java), REQUEST_BACKGROUND)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_STORAGE_PERMISSION) return
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            openBuiltInImagePicker()
        } else {
            Toast.makeText(this, R.string.image_permission_denied, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val REQUEST_BACKGROUND = 2
        private const val REQUEST_STORAGE_PERMISSION = 3
    }
}
