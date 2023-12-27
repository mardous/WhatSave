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

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.model.Status
import com.simplified.wsstatussaver.model.StatusType
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.text.SimpleDateFormat.getDateTimeInstance
import java.util.*

val fileDateFormat: DateFormat by lazy {
    SimpleDateFormat("MMM_d_yyyy_HH.mm.ss", Locale.ENGLISH)
}

@Suppress("DEPRECATION")
fun Status.getFormattedDate(context: Context): String {
    val date = Date(dateModified)
    val resLocale = when {
        hasN() -> context.resources.configuration.locales[0]
        else -> context.resources.configuration.locale
    }
    return getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT, resLocale).format(date)
}

/**
 * Generates and returns a new save name depending on the
 * given [StatusType] format and the current time.
 */
fun getNewSaveName(type: StatusType? = null, timeMillis: Long = System.currentTimeMillis(), delta: Int = 0): String {
    var saveName = String.format("Status_%s", fileDateFormat.format(Date(timeMillis)))
    if (delta > 0) {
        saveName += "-$delta"
    }
    if (type != null) {
        saveName += type.format
    }
    return saveName
}

fun Status.getState(context: Context): String =
    if (isSaved) context.getString(R.string.status_saved) else context.getString(R.string.status_unsaved)

/**
 * Returns a SQLite *WHERE* clause that can be used
 * to identify this status within MediaStore.
 *
 * This is intended to be used ONLY with saved statuses.
 */
fun Status.getMediaStoreWhere(): Pair<String, Array<String>> {
    return if (hasQ()) {
        "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} = ?" to arrayOf(
            type.relativePath, name
        )
    } else {
        "${MediaStore.MediaColumns.DATA} = ?" to arrayOf(path)
    }
}

/**
 * Returns the MediaStore (*"content://"*) URI corresponding to this status only
 * if it's saved, otherwise, this method return null.
 */
fun Status.getMediaStoreUri(context: Context): Uri? {
    val where = getMediaStoreWhere()
    val projection = arrayOf(MediaStore.MediaColumns._ID)
    return context.contentResolver.query(type.contentUri, projection, where.first, where.second, null).use { cursor ->
        if (cursor != null && cursor.moveToFirst()) {
            ContentUris.withAppendedId(
                type.contentUri, cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
            )
        } else null
    }
}

fun List<Status>.getMediaStoreUris(context: Context) = mapNotNull { it.getMediaStoreUri(context) }

/**
 * Returns the MediaStore (*"content://"*) URI corresponding to this status
 * if it's saved, otherwise, returned URI will point to the respective file
 * in the WhatsApp's directory.
 */
fun Status.getActualUri(context: Context): Uri {
    val mediaStoreUri = getMediaStoreUri(context)
    if (mediaStoreUri != null) {
        return mediaStoreUri
    }
    return File(path).getUri()
}

val Status.isVideo: Boolean get() = type == StatusType.VIDEO

/**
 * Creates an [Intent] that can be used to open this status
 * in other apps.
 */
fun Status.toPreviewIntent(context: Context): Intent = getActualUri(context).let { uri ->
    Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, this@toPreviewIntent.type.mimeType)
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
}

/**
 * Creates an [Intent] that can be used to share this status.
 */
fun Status.toShareIntent(context: Context): Intent = getActualUri(context).let { uri ->
    Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_STREAM, uri)
        type = this@toShareIntent.type.mimeType
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
}

fun StatusType.acceptFileName(fileName: String): Boolean = !fileName.startsWith(".") && fileName.endsWith(this.format)

fun List<Status>.toShareIntent(context: Context): Intent {
    val types = HashSet<String>()

    val uris = ArrayList<Uri>()
    for (status in this) {
        types.add(status.type.mimeType)
        uris.add(status.getActualUri(context))
    }

    val mimeType = if (types.size == 1) types.first() else "*/*"
    return Intent(Intent.ACTION_SEND_MULTIPLE)
        .putExtra(Intent.EXTRA_STREAM, uris)
        .setType(mimeType)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}