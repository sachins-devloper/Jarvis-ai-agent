package com.personalai.jarvis.tools

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import com.personalai.jarvis.agent.AgentTool
import com.personalai.jarvis.agent.ToolDefinition
import com.personalai.jarvis.agent.ToolParameter
import com.personalai.jarvis.agent.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

data class MediaFileInfo(
    val name: String,
    val path: String,
    val uriString: String,
    val mimeType: String,
    val sizeBytes: Long,
    val dateModifiedMillis: Long,
    val mediaType: String
)

/**
 * FileSearchTool — Searches files (images, audio/music, video, documents, downloads)
 * on the phone using Android MediaStore and provides direct links to open them.
 */
class FileSearchTool(private val context: Context) : AgentTool {

    override val name: String = "search_files"
    override val description: String = "Searches for files on the phone including photos/images, music/songs, videos, and documents by name or file type."

    override val definition: ToolDefinition = ToolDefinition(
        name = name,
        description = description,
        parameters = listOf(
            ToolParameter(
                name = "query",
                type = "string",
                description = "Name keyword to search for (e.g. 'whatsapp', 'screenshot', 'bill', 'song'). Optional.",
                required = false
            ),
            ToolParameter(
                name = "type",
                type = "string",
                description = "Type of files: 'image' (or 'photo'), 'audio' (or 'music', 'song'), 'video', 'document', or 'all'. Defaults to 'all'.",
                required = false
            ),
            ToolParameter(
                name = "limit",
                type = "integer",
                description = "Maximum number of files to return (default 8).",
                required = false
            )
        )
    )

    override suspend fun execute(arguments: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        val query = (arguments["query"] as? String)?.trim() ?: ""
        val typeArg = (arguments["type"] as? String)?.lowercase()?.trim() ?: "all"
        val limit = (arguments["limit"] as? Number)?.toInt() ?: 8

        // Check storage/media permissions
        if (!hasRequiredPermissions(typeArg)) {
            val permName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                when (typeArg) {
                    "image", "photo" -> "Photos & Videos"
                    "audio", "music", "song" -> "Music & Audio"
                    "video" -> "Videos"
                    else -> "Media & Storage"
                }
            } else {
                "Storage"
            }
            return@withContext ToolResult.error("Permission not granted. Please grant $permName permission in Android Settings to search files.")
        }

        try {
            val results = mutableListOf<MediaFileInfo>()

            when (typeArg) {
                "image", "photo", "photos", "images", "img" -> {
                    queryMediaStore(
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        mediaType = "IMAGE",
                        searchQuery = query,
                        limit = limit,
                        outList = results
                    )
                }

                "audio", "music", "song", "songs" -> {
                    queryMediaStore(
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        mediaType = "AUDIO",
                        searchQuery = query,
                        limit = limit,
                        outList = results
                    )
                }

                "video", "videos", "movie", "movies" -> {
                    queryMediaStore(
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        mediaType = "VIDEO",
                        searchQuery = query,
                        limit = limit,
                        outList = results
                    )
                }

                "document", "doc", "pdf", "docs" -> {
                    queryMediaFiles(
                        searchQuery = query,
                        documentOnly = true,
                        limit = limit,
                        outList = results
                    )
                }

                else -> {
                    // Search across images, audio, video, documents
                    val subLimit = (limit / 3).coerceAtLeast(3)
                    queryMediaStore(
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        mediaType = "IMAGE",
                        searchQuery = query,
                        limit = subLimit,
                        outList = results
                    )
                    queryMediaStore(
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        mediaType = "AUDIO",
                        searchQuery = query,
                        limit = subLimit,
                        outList = results
                    )
                    queryMediaStore(
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        mediaType = "VIDEO",
                        searchQuery = query,
                        limit = subLimit,
                        outList = results
                    )
                }
            }

            if (results.isEmpty()) {
                val queryDesc = if (query.isNotBlank()) " matching \"$query\"" else ""
                val typeDesc = if (typeArg != "all") " $typeArg" else ""
                return@withContext ToolResult.success("No$typeDesc files found$queryDesc on your device.")
            }

            val sb = StringBuilder()
            val headerType = if (typeArg != "all") "$typeArg files" else "files"
            val queryText = if (query.isNotBlank()) " matching \"$query\"" else ""
            sb.appendLine("Found ${results.size} $headerType$queryText:")
            sb.appendLine()

            results.forEachIndexed { index, file ->
                val icon = when (file.mediaType) {
                    "IMAGE" -> "🖼️"
                    "AUDIO" -> "🎵"
                    "VIDEO" -> "🎬"
                    else -> "📄"
                }
                val formattedSize = formatFileSize(file.sizeBytes)
                val formattedDate = formatDate(file.dateModifiedMillis)

                sb.appendLine("${index + 1}. $icon [${file.name}](file://${file.path}) ($formattedSize)")
                sb.appendLine("   Path: `${file.path}`")
            }

            sb.appendLine()
            sb.appendLine("💡 *Tap any file path above to open it directly on your phone.*")

            val filesData = results.map {
                mapOf(
                    "name" to it.name,
                    "path" to it.path,
                    "uri" to it.uriString,
                    "mime" to it.mimeType,
                    "size" to formatFileSize(it.sizeBytes),
                    "type" to it.mediaType
                )
            }

            ToolResult(
                success = true,
                output = sb.toString().trim(),
                data = mapOf("files" to filesData)
            )
        } catch (e: Exception) {
            ToolResult.error("File search failed: ${e.localizedMessage ?: e.message}")
        }
    }

    private fun hasRequiredPermissions(type: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return when (type) {
                "image", "photo", "photos", "images", "img" -> {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                }
                "audio", "music", "song", "songs" -> {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
                }
                "video", "videos" -> {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
                }
                else -> {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
                }
            }
        } else {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun queryMediaStore(
        contentUri: Uri,
        mediaType: String,
        searchQuery: String,
        limit: Int,
        outList: MutableList<MediaFileInfo>
    ) {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )

        var selection: String? = null
        var selectionArgs: Array<String>? = null

        if (searchQuery.isNotBlank()) {
            selection = "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
            selectionArgs = arrayOf("%$searchQuery%")
        }

        val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC LIMIT $limit"

        context.contentResolver.query(
            contentUri,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
            val mimeCol = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
            val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val dateCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)

            var count = 0
            while (cursor.moveToNext() && count < limit) {
                val id = if (idCol != -1) cursor.getLong(idCol) else 0L
                val name = if (nameCol != -1) cursor.getString(nameCol) ?: "Unknown" else "Unknown"
                val path = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""
                val mime = if (mimeCol != -1) cursor.getString(mimeCol) ?: "application/octet-stream" else "application/octet-stream"
                val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                val dateSec = if (dateCol != -1) cursor.getLong(dateCol) else 0L

                val itemUri = ContentUris.withAppendedId(contentUri, id)

                outList.add(
                    MediaFileInfo(
                        name = name,
                        path = path,
                        uriString = itemUri.toString(),
                        mimeType = mime,
                        sizeBytes = size,
                        dateModifiedMillis = dateSec * 1000L,
                        mediaType = mediaType
                    )
                )
                count++
            }
        }
    }

    private fun queryMediaFiles(
        searchQuery: String,
        documentOnly: Boolean,
        limit: Int,
        outList: MutableList<MediaFileInfo>
    ) {
        val contentUri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )

        val clauses = mutableListOf<String>()
        val args = mutableListOf<String>()

        if (documentOnly) {
            clauses.add("(${MediaStore.MediaColumns.MIME_TYPE} LIKE ? OR ${MediaStore.MediaColumns.MIME_TYPE} LIKE ? OR ${MediaStore.MediaColumns.MIME_TYPE} LIKE ? OR ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?)")
            args.add("%pdf%")
            args.add("%document%")
            args.add("%text%")
            args.add("%.pdf")
        }

        if (searchQuery.isNotBlank()) {
            clauses.add("${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?")
            args.add("%$searchQuery%")
        }

        val selection = if (clauses.isNotEmpty()) clauses.joinToString(" AND ") else null
        val selectionArgs = if (args.isNotEmpty()) args.toTypedArray() else null
        val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC LIMIT $limit"

        context.contentResolver.query(
            contentUri,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
            val mimeCol = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
            val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val dateCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)

            var count = 0
            while (cursor.moveToNext() && count < limit) {
                val id = if (idCol != -1) cursor.getLong(idCol) else 0L
                val name = if (nameCol != -1) cursor.getString(nameCol) ?: "Unknown" else "Unknown"
                val path = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""
                val mime = if (mimeCol != -1) cursor.getString(mimeCol) ?: "application/octet-stream" else "application/octet-stream"
                val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                val dateSec = if (dateCol != -1) cursor.getLong(dateCol) else 0L

                val itemUri = ContentUris.withAppendedId(contentUri, id)

                outList.add(
                    MediaFileInfo(
                        name = name,
                        path = path,
                        uriString = itemUri.toString(),
                        mimeType = mime,
                        sizeBytes = size,
                        dateModifiedMillis = dateSec * 1000L,
                        mediaType = "DOCUMENT"
                    )
                )
                count++
            }
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val value = bytes / 1024.0.pow(index.toDouble())
        return String.format(Locale.getDefault(), "%.1f %s", value, units[index])
    }

    private fun formatDate(millis: Long): String {
        if (millis <= 0) return ""
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(millis))
    }
}
