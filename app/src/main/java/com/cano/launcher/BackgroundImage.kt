package com.cano.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

object BackgroundImage {
    fun decode(context: Context, uri: Uri, targetWidth: Int, targetHeight: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = context.contentResolver.openInputStream(uri) ?: return null
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, targetWidth, targetHeight)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val imageStream = context.contentResolver.openInputStream(uri) ?: return null
        return imageStream.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun sampleSize(width: Int, height: Int, targetWidth: Int, targetHeight: Int): Int {
        var sample = 1
        while (width.toLong() / (sample.toLong() * 2) >= targetWidth ||
            height.toLong() / (sample.toLong() * 2) >= targetHeight
        ) {
            sample *= 2
        }
        return sample
    }
}
