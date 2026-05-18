package com.example.livewallpaper.media

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.util.Log

object VideoUriPermissionHelper {
    private const val TAG = "VideoUriPermission"

    fun takePersistableUriPermission(contentResolver: ContentResolver, uri: Uri, flags: Int) {
        val modeFlags = flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        try {
            contentResolver.takePersistableUriPermission(uri, modeFlags)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to persist uri permission: $uri", t)
        }
    }
}
