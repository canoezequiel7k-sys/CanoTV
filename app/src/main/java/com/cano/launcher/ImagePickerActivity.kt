package com.cano.launcher

import android.app.Activity
import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.cano.launcher.databinding.ActivityImagePickerBinding
import java.util.concurrent.Executors

class ImagePickerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImagePickerBinding
    private val worker = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImagePickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.imageList.setOnItemClickListener { _, _, position, _ ->
            val image = images.getOrNull(position) ?: return@setOnItemClickListener
            setResult(
                Activity.RESULT_OK,
                android.content.Intent().setData(image.uri)
            )
            finish()
        }
        binding.cancelButton.setOnClickListener { finish() }
        loadImages()
    }

    private var images: List<DeviceImage> = emptyList()

    private fun loadImages() {
        binding.loading.visibility = View.VISIBLE
        worker.execute {
            val foundImages = queryImages()
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                images = foundImages
                binding.loading.visibility = View.GONE
                binding.emptyMessage.visibility =
                    if (images.isEmpty()) View.VISIBLE else View.GONE
                binding.imageList.visibility =
                    if (images.isEmpty()) View.GONE else View.VISIBLE
                binding.imageList.adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_list_item_1,
                    images.map { it.name }
                )
            }
        }
    }

    private fun queryImages(): List<DeviceImage> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME
        )
        val result = mutableListOf<DeviceImage>()
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            while (cursor.moveToNext() && result.size < MAX_IMAGES) {
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    cursor.getLong(idColumn)
                )
                val name = cursor.getString(nameColumn) ?: getString(R.string.unnamed_image)
                result.add(DeviceImage(uri, name))
            }
        }
        return result
    }

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }

    private data class DeviceImage(val uri: Uri, val name: String)

    companion object {
        private const val MAX_IMAGES = 300
    }
}
