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
import android.os.Environment
import android.provider.MediaStore

/**
 * Internal enum that stores information about save types.
 *
 * @author Christians Martínez Alvarado (mardous)
 */
internal enum class StatusSaveType(internal val dirType: String, internal val dirName: String,
                          internal val fileMimeType: String, internal val contentUri: Uri) {
    IMAGE_SAVE(Environment.DIRECTORY_DCIM, "Saved Image Statuses", "image/jpeg",
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
    VIDEO_SAVE(Environment.DIRECTORY_DCIM, "Saved Video Statuses", "video/mp4",
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
}