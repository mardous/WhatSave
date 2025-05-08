/*
 * Copyright (C) 2025 Christians Martínez Alvarado
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
/*
 * Copyright (C) 2025 Christians Martínez Alvarado
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
import java.util.concurrent.TimeUnit

class WaFile(
    val id: String?,
    val owner: WaClient?,
    val path: String?,
    val name: String,
    val lastModified: Long,
    val size: Long,
    val uri: Uri
) {
    fun toStatus(type: StatusType, isSaved: Boolean) =
        Status(type, name, uri, lastModified, size, owner?.packageName, isSaved)

    fun toSavedStatus(type: StatusType) =
        SavedStatus(type, name, uri, lastModified, size, path)

    fun isOlderThan(timeUnit: TimeUnit, duration: Long): Boolean {
        return (System.currentTimeMillis() - lastModified) >= timeUnit.toMillis(duration)
    }
}