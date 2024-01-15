/*
 * Copyright (C) 2024 Christians Martínez Alvarado
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
import java.io.File

class SavedStatus(
    type: StatusType,
    name: String,
    fileUri: Uri,
    dateModified: Long,
    size: Long,
    private val path: String?
) : Status(type, name, fileUri, dateModified, size, null, true) {

    fun hasFile(): Boolean = !path.isNullOrBlank()

    fun getFile(): File {
        checkNotNull(path)
        return File(path)
    }
}