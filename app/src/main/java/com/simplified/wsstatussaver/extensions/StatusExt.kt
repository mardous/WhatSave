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
import android.net.Uri
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.model.Status
import com.simplified.wsstatussaver.model.StatusType
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.text.SimpleDateFormat.getDateTimeInstance
import java.util.*

val fileDateFormat: DateFormat by lazy {
    SimpleDateFormat("MMM_d_yyyy_HH.mm.ss", Locale.ENGLISH)
}

fun Status.getFormattedDate(context: Context): String {
    val date = Date(dateModified)
    val resLocale = context.resources.configuration.locales[0]
    return getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT, resLocale).format(date)
}

/**
 * Generates and returns a new save name depending on the
 * given [StatusType] format and the current time.
 */
fun getNewSaveName(type: StatusType? = null, timeMillis: Long, delta: Int): String {
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

val Status.isVideo: Boolean get() = type == StatusType.VIDEO

/**
 * Creates an [Intent] that can be used to open this status
 * in other apps.
 */
fun Status.toPreviewIntent() =
    Intent(Intent.ACTION_VIEW)
        .setDataAndType(fileUri, type.mimeType)
        .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

/**
 * Creates an [Intent] that can be used to share this status.
 */
fun Status.toShareIntent() =
    Intent(Intent.ACTION_SEND)
        .putExtra(Intent.EXTRA_STREAM, fileUri)
        .setType(type.mimeType)
        .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

fun StatusType.acceptFileName(fileName: String): Boolean = !fileName.startsWith(".") && fileName.endsWith(this.format)

fun List<Status>.toShareIntent(): Intent {
    val types = HashSet<String>()
    mapTo(types) { it.type.mimeType }

    val uris = ArrayList<Uri>()
    mapTo(uris) { it.fileUri }

    val mimeType = if (types.size == 1) types.first() else "*/*"
    return Intent(Intent.ACTION_SEND_MULTIPLE)
        .putExtra(Intent.EXTRA_STREAM, uris)
        .setType(mimeType)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}