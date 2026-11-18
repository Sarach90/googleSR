package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

private const val TAG = "ProductImageStorage"
private const val DIR_NAME = "product_images"
private const val MAX_DIMENSION_PX = 1024

object ProductImageStorage {

    suspend fun saveFromUri(
        context: Context,
        projectId: String,
        uri: Uri
    ): String? = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val tempFile = File(appContext.cacheDir, "import_tmp_${UUID.randomUUID()}")
        try {
            val copied = copyUriToTempFile(appContext, uri, tempFile)
            if (!copied || !tempFile.exists() || tempFile.length() <= 0L) {
                Log.e(TAG, "Unable to read bytes from URI: $uri")
                return@withContext null
            }

            val bitmap = decodeScaledBitmapFromFile(tempFile) ?: return@withContext null
            try {
                val dir = File(appContext.filesDir, DIR_NAME).apply {
                    if (!exists()) mkdirs()
                }
                val outFile = File(dir, "${projectId}_${UUID.randomUUID()}.jpg")
                FileOutputStream(outFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                }
                outFile.absolutePath
            } finally {
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import product image from URI: $uri", e)
            null
        } finally {
            runCatching { if (tempFile.exists()) tempFile.delete() }
        }
    }

    private fun copyUriToTempFile(context: Context, uri: Uri, destFile: File): Boolean {
        val resolver = context.contentResolver
        val stream = runCatching { resolver.openInputStream(uri) }.getOrNull()
            ?: runCatching { resolver.openAssetFileDescriptor(uri, "r")?.createInputStream() }.getOrNull()
            ?: runCatching { resolver.openTypedAssetFileDescriptor(uri, "image/*", null)?.createInputStream() }.getOrNull()
            ?: runCatching {
                if (uri.scheme == "file" && uri.path != null) File(uri.path!!).inputStream() else null
            }.getOrNull()
            ?: return false

        return stream.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
            true
        }
    }

    private fun decodeScaledBitmapFromFile(file: File): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val source = ImageDecoder.createSource(file)
                return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val origW = info.size.width
                    val origH = info.size.height
                    if (origW > MAX_DIMENSION_PX || origH > MAX_DIMENSION_PX) {
                        val ratio = minOf(
                            MAX_DIMENSION_PX.toFloat() / origW,
                            MAX_DIMENSION_PX.toFloat() / origH
                        )
                        val targetW = (origW * ratio).toInt().coerceAtLeast(1)
                        val targetH = (origH * ratio).toInt().coerceAtLeast(1)
                        decoder.setTargetSize(targetW, targetH)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "ImageDecoder failed on temp file, falling back to BitmapFactory", e)
            }
        }

        return try {
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
            val origW = boundsOptions.outWidth
            val origH = boundsOptions.outHeight

            val decodeOptions = BitmapFactory.Options()
            if (origW > 0 && origH > 0) {
                var sampleSize = 1
                val maxSide = maxOf(origW, origH)
                while (maxSide / sampleSize > MAX_DIMENSION_PX * 2) {
                    sampleSize *= 2
                }
                decodeOptions.inSampleSize = sampleSize
            }

            val decoded = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null
            val scaled = scaleDownIfNeeded(decoded, MAX_DIMENSION_PX)
            if (scaled !== decoded) {
                decoded.recycle()
            }
            scaled
        } catch (e: Exception) {
            Log.e(TAG, "BitmapFactory failed on temp file", e)
            null
        }
    }

    fun loadBitmap(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        return try {
            val file = File(path)
            if (!file.exists()) return null
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode product bitmap at $path", e)
            null
        }
    }

    fun deleteFile(path: String?) {
        if (path.isNullOrBlank()) return
        try {
            val file = File(path)
            if (file.exists() && file.parentFile?.name == DIR_NAME) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not delete image file $path", e)
        }
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val ratio = minOf(maxDim.toFloat() / w, maxDim.toFloat() / h)
        val targetW = (w * ratio).toInt().coerceAtLeast(1)
        val targetH = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }
}
