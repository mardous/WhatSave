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
package com.simplified.wsstatussaver.database

import com.simplified.wsstatussaver.model.Status
import com.simplified.wsstatussaver.model.StatusType
import java.io.FileInputStream

private fun Status.getSaveName(i: String?, delta: Int = 0): String {
    var saveName = i
    if (saveName.isNullOrBlank()) {
        return type.getDefaultSaveName(delta)
    }
    if (!saveName.endsWith(type.format)) {
        saveName += type.format
    }
    while (saveName!!.startsWith(".")) {
        saveName = saveName.drop(1)
    }
    return saveName
}

fun Int.asStatusType() = StatusType.values().first { it.ordinal == this }

fun Status.toStatusEntity(saveName: String?, delta: Int = 0) = StatusEntity(
    type = type.ordinal,
    name = name,
    origin = path,
    dateModified = dateModified,
    size = size,
    client = clientPackage,
    saveName = getSaveName(saveName, delta)
)

fun StatusEntity.statusType() = type.asStatusType()

fun StatusEntity.openInputStream() = FileInputStream(origin)