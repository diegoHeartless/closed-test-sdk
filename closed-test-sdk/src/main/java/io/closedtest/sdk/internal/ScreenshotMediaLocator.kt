package io.closedtest.sdk.internal

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

/** Resolves the newest screenshot in MediaStore taken after [sinceEpochMs]. */
internal object ScreenshotMediaLocator {

    private val SCREENSHOT_HINT_RE = Regex("screenshot", RegexOption.IGNORE_CASE)

    fun findLatestScreenshotUri(context: Context, sinceEpochMs: Long): Uri? {
        val sinceSec = (sinceEpochMs / 1000L).coerceAtLeast(0L)
        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
        val projection =
            arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.RELATIVE_PATH,
            )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        return try {
            context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
                while (cursor.moveToNext()) {
                    val addedSec = cursor.getLong(dateCol)
                    if (addedSec < sinceSec) return@use null
                    val name = cursor.getString(nameCol).orEmpty()
                    val relPath = cursor.getString(pathCol).orEmpty()
                    if (!looksLikeScreenshot(name, relPath)) continue
                    val id = cursor.getLong(idCol)
                    return@use Uri.withAppendedPath(collection, id.toString())
                }
                null
            }
        } catch (_: SecurityException) {
            null
        } catch (_: Throwable) {
            null
        }
    }

    private fun looksLikeScreenshot(displayName: String, relativePath: String): Boolean {
        if (SCREENSHOT_HINT_RE.containsMatchIn(displayName)) return true
        if (SCREENSHOT_HINT_RE.containsMatchIn(relativePath)) return true
        if (relativePath.contains("Pictures/Screenshots", ignoreCase = true)) return true
        return false
    }
}
