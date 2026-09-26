package com.personalai.jarvis.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageHelper {

    /**
     * Reads an image Uri, downsizes it to maxDimension preserving aspect ratio,
     * and returns a base64 encoded JPEG string suitable for OpenAI Vision API.
     */
    fun uriToBase64(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1280,
        quality: Int = 80
    ): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (originalBitmap == null) return null

            val width = originalBitmap.width
            val height = originalBitmap.height

            val scaledBitmap = if (width > maxDimension || height > maxDimension) {
                val ratio = width.toFloat() / height.toFloat()
                val targetW: Int
                val targetH: Int
                if (ratio > 1) {
                    targetW = maxDimension
                    targetH = (maxDimension / ratio).toInt().coerceAtLeast(1)
                } else {
                    targetH = maxDimension
                    targetW = (maxDimension * ratio).toInt().coerceAtLeast(1)
                }
                Bitmap.createScaledBitmap(originalBitmap, targetW, targetH, true)
            } else {
                originalBitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val bytes = outputStream.toByteArray()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Reads a file path, downsizes it, and returns base64.
     */
    fun fileToBase64(
        filePath: String,
        maxDimension: Int = 1280,
        quality: Int = 80
    ): String? {
        return try {
            val file = File(filePath.removePrefix("file://"))
            if (!file.exists()) return null

            val originalBitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
            val width = originalBitmap.width
            val height = originalBitmap.height

            val scaledBitmap = if (width > maxDimension || height > maxDimension) {
                val ratio = width.toFloat() / height.toFloat()
                val targetW: Int
                val targetH: Int
                if (ratio > 1) {
                    targetW = maxDimension
                    targetH = (maxDimension / ratio).toInt().coerceAtLeast(1)
                } else {
                    targetH = maxDimension
                    targetW = (maxDimension * ratio).toInt().coerceAtLeast(1)
                }
                Bitmap.createScaledBitmap(originalBitmap, targetW, targetH, true)
            } else {
                originalBitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val bytes = outputStream.toByteArray()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Creates a temporary file in app cache for camera photo capture.
     */
    fun createTempCameraFile(context: Context): File {
        val cacheDir = context.cacheDir
        val imagesDir = File(cacheDir, "camera_photos").apply { mkdirs() }
        return File(imagesDir, "capture_${System.currentTimeMillis()}.jpg")
    }
}
