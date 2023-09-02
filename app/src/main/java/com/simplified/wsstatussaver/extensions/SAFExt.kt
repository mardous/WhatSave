/*
 * Copyright (C) 2023 Christians Martínez Alvarado
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by
 * the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package com.simplified.wsstatussaver.extensions

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import java.io.File

private const val TAG = "SAF"
private const val SAF_SDCARD_URI = "saf_sdcard_uri"

var SharedPreferences.safSDCardUri: String?
    get() = getString(SAF_SDCARD_URI, "")
    set(uri) = edit {
        putString(SAF_SDCARD_URI, uri)
    }

val SharedPreferences.isTreeUriSaved: Boolean
    get() = safSDCardUri.isNullOrEmpty()

fun File.isSAFRequiredForPath(): Boolean {
    if (hasR()) {
        return false
    }
    return !canWrite()
}

fun Context.saveTreeUri(data: Uri?) {
    if (data == null) return

    contentResolver.takePersistableUriPermission(
        data, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
    )

    preferences().safSDCardUri = data.toString()
}

fun Context.isSDCardAccessGranted(): Boolean {
    if (!preferences().isTreeUriSaved) return false

    val sdcardUri = preferences().safSDCardUri
    val perms = contentResolver.persistedUriPermissions
    for (perm in perms) {
        if (perm.uri.toString() == sdcardUri && perm.isWritePermission) return true
    }

    return false
}

/**
 * https://github.com/vanilla-music/vanilla-music-tag-editor/commit/e00e87fef289f463b6682674aa54be834179ccf0#diff-d436417358d5dfbb06846746d43c47a5R359
 * Finds needed file through Document API for SAF. It's not optimized yet - you can still gain wrong URI on
 * files such as "/a/b/c.mp3" and "/b/a/c.mp3", but I consider it complete enough to be usable.

 * @return URI for found file. Null if nothing found.
 */
private fun DocumentFile.findDocument(segments: MutableList<String>): Uri? {
    for (file in this.listFiles()) {
        val index = segments.indexOf(file.name)
        if (index == -1) {
            continue
        }
        if (file.isDirectory) {
            segments.remove(file.name)
            return findDocument(segments)
        }
        if (file.isFile && index == segments.size - 1) {
            // got to the last part
            return file.uri
        }
    }
    return null
}

fun Context.tryDeletePath(path: File?): Boolean {
    if (path == null) {
        return false
    } else if (path.isSAFRequiredForPath()) {
        return deleteWithSAF(path.absolutePath)
    } else {
        try {
            return path.delete()
        } catch (e: NullPointerException) {
            Log.e(TAG, "Failed to find file $path")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return false
}

fun Context.deleteWithSAF(path: String): Boolean {
    var uri: Uri? = null
    if (preferences().isTreeUriSaved) {
        val pathSegments = path.split("/").toMutableList()
        val sdcard = preferences().safSDCardUri?.toUri() ?: return false
        val sdcardDirectory = DocumentFile.fromTreeUri(this, sdcard)
        if (sdcardDirectory != null) {
            uri = sdcardDirectory.findDocument(pathSegments)
        }
    }
    if (uri != null) try {
        return DocumentsContract.deleteDocument(contentResolver, uri)
    } catch (e: Exception) {
        Log.e(TAG, "deleteWithSAF: Failed to delete a file descriptor provided by SAF", e)
    }
    return false
}