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

import android.net.Uri
import androidx.core.content.FileProvider
import com.simplified.wsstatussaver.App
import com.simplified.wsstatussaver.getApp
import java.io.File
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import kotlin.math.log10
import kotlin.math.pow

fun File.getUri(): Uri = FileProvider.getUriForFile(getApp().applicationContext, App.getFileProviderAuthority(), this)

fun File.isOlderThan(maxHours: Int): Boolean {
    return (System.currentTimeMillis() - lastModified()) >= TimeUnit.HOURS.toMillis(maxHours.toLong())
}

fun Long.toFileSize(): String {
    if (this <= 0) {
        return "0 bytes"
    }
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(this.toDouble()) / log10(1024.0)).toInt()
    return String.format(
        "%s %s",
        DecimalFormat("#,##0.#").format(this / 1024.0.pow(digitGroups.toDouble())),
        units[digitGroups]
    )
}