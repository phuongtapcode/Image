package com.example.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.example.R

data class ResolvedLocalMedia(
    val uriString: String,
    val mediaType: MediaType,
    val displayName: String,
    val persistedPermission: Boolean
)

class MediaUriHelper(private val context: Context) {

    private val resolver: ContentResolver = context.contentResolver

    fun resolveAndPersist(uri: Uri): ResolvedLocalMedia {
        val persisted = try {
            resolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            true
        } catch (_: SecurityException) {
            false
        } catch (_: UnsupportedOperationException) {
            false
        }

        val displayName = queryDisplayName(uri) ?: context.getString(R.string.slot_default_name)
        val mimeType = resolver.getType(uri)?.lowercase().orEmpty()
        val mediaType = detectMediaType(mimeType, displayName)

        return ResolvedLocalMedia(
            uriString = uri.toString(),
            mediaType = mediaType,
            displayName = displayName,
            persistedPermission = persisted
        )
    }

    fun releasePermissionIfHeld(uriString: String) {
        try {
            val uri = Uri.parse(uriString)
            val hasGrant = resolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
            if (hasGrant) {
                resolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        } catch (_: SecurityException) {
            // Ignore if already revoked by the system
        } catch (_: IllegalArgumentException) {
            // Ignore malformed URI
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex >= 0) cursor.getString(columnIndex) else null
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment?.substringAfterLast('/')
    }

    private fun detectMediaType(mimeType: String, displayName: String): MediaType {
        val lowerName = displayName.lowercase()
        return when {
            mimeType == "image/gif" || lowerName.endsWith(".gif") -> MediaType.GIF
            mimeType.startsWith("video/") ||
                lowerName.endsWith(".mp4") ||
                lowerName.endsWith(".mkv") ||
                lowerName.endsWith(".webm") ||
                lowerName.endsWith(".mov") ||
                lowerName.endsWith(".3gp") ||
                lowerName.endsWith(".m4v") -> MediaType.VIDEO
            else -> MediaType.IMAGE
        }
    }
}
