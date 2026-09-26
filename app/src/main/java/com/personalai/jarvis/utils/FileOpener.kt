package com.personalai.jarvis.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object FileOpener {

    /**
     * Opens the file at the specified absolute path or URI using standard Android ACTION_VIEW.
     * Launches the user's preferred viewer (Gallery, Music Player, Video Player, Files, etc.).
     */
    fun openFile(context: Context, rawPath: String) {
        try {
            val cleanPath = rawPath.removePrefix("file://").trim()

            // If it's already a content:// URI
            if (cleanPath.startsWith("content://")) {
                val uri = Uri.parse(cleanPath)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(intent, "Open with").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                return
            }

            val file = File(cleanPath)
            if (!file.exists()) {
                Toast.makeText(context, "File does not exist: ${file.name}", Toast.LENGTH_SHORT).show()
                return
            }

            val mimeType = getMimeType(file)
            val contentUri: Uri = try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (e: Exception) {
                Uri.fromFile(file)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "Open ${file.name}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open file: ${e.localizedMessage ?: e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens the directory / folder containing the file in Android's file manager.
     */
    fun openFolder(context: Context, rawPath: String) {
        try {
            val cleanPath = rawPath.removePrefix("file://").trim()
            val file = File(cleanPath)
            val folder = if (file.isDirectory) file else file.parentFile ?: file

            val folderUri = try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    folder
                )
            } catch (e: Exception) {
                Uri.fromFile(folder)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(folderUri, "resource/folder")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "Open Folder").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            // If folder opening fails, fall back to opening the file itself
            openFile(context, rawPath)
        }
    }

    private fun getMimeType(file: File): String {
        val extension = file.extension.lowercase()
        val fromMap = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        if (!fromMap.isNullOrBlank()) return fromMap

        return when (extension) {
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif" -> "image/*"
            "mp3", "wav", "m4a", "aac", "flac", "ogg", "opus" -> "audio/*"
            "mp4", "mkv", "avi", "mov", "webm", "3gp", "flv" -> "video/*"
            "pdf" -> "application/pdf"
            "txt", "log", "json", "xml", "csv" -> "text/plain"
            "doc", "docx" -> "application/msword"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            "ppt", "pptx" -> "application/vnd.ms-powerpoint"
            "zip", "rar", "7z", "tar", "gz" -> "application/zip"
            "apk" -> "application/vnd.android.package-archive"
            else -> "*/*"
        }
    }
}
