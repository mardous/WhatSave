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
package com.simplified.wsstatussaver.model

import android.annotation.TargetApi
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.annotation.StringRes
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.extensions.acceptFileName
import com.simplified.wsstatussaver.extensions.getNewSaveName
import java.io.File

/**
 * @author Christians Martínez Alvarado (mardous)
 */
enum class StatusType(@StringRes val nameRes: Int, val format: String, private val saveType: StatusSaveType) {
    IMAGE(R.string.type_image, ".jpg", StatusSaveType.IMAGE_SAVE),
    VIDEO(R.string.type_video, ".mp4", StatusSaveType.VIDEO_SAVE);

    fun getDefaultSaveName(timeMillis: Long, delta: Int): String = getNewSaveName(this, timeMillis, delta = delta)

    val contentUri: Uri get() = saveType.contentUri

    val mimeType: String get() = saveType.fileMimeType

    @TargetApi(Build.VERSION_CODES.Q)
    fun getRelativePath(location: SaveLocation): String =
        String.format("%s/%s", saveType.getDirType(location), saveType.dirName)

    fun getSavesDirectory(location: SaveLocation): File =
        File(Environment.getExternalStoragePublicDirectory(saveType.getDirType(location)), saveType.dirName)

    fun getSavedContentFiles(location: SaveLocation): Array<File> {
        val directory = getSavesDirectory(location)
        return directory.listFiles { _, name -> acceptFileName(name) } ?: emptyArray()
    }
}