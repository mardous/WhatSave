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

import android.net.Uri
import android.provider.MediaStore

/**
 * Internal enum that stores information about save types.
 *
 * @author Christians Martínez Alvarado (mardous)
 */
enum class StatusSaveType(
    val dirName: String,
    val fileMimeType: String,
    val contentUri: Uri,
    val dirTypeProvider: (SaveLocation) -> String
) {
    IMAGE_SAVE(
        dirName = "Saved Image Statuses",
        fileMimeType = "image/jpeg",
        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        dirTypeProvider = { it.imageDir }
    ),
    VIDEO_SAVE(
        dirName = "Saved Video Statuses",
        fileMimeType = "video/mp4",
        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        dirTypeProvider = { it.videoDir }
    );

    val customDirectoryId: String
        get() = "custom.${name.lowercase()}"
}