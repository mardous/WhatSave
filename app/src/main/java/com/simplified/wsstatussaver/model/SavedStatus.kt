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
import android.os.Parcelable
import com.simplified.wsstatussaver.extensions.canonicalOrAbsolutePath
import kotlinx.parcelize.Parcelize
import java.io.File

@Parcelize
class SavedStatus(
    override val type: StatusType,
    override val name: String,
    override val fileUri: Uri,
    override val dateModified: Long,
    override val size: Long,
    private val path: String?
) : Status(type, name, fileUri, dateModified, size, null, true), Parcelable {

    fun hasFile(): Boolean = !path.isNullOrBlank()

    fun getFile(): File {
        checkNotNull(path)
        return File(path)
    }

    fun getFilePath(): String {
        return getFile().canonicalOrAbsolutePath()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SavedStatus) return false
        if (!super.equals(other)) return false

        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (path?.hashCode() ?: 0)
        return result
    }
}