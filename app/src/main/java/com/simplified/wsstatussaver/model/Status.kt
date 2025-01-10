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
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @author Christians Martínez Alvarado (mardous)
 */
@Parcelize
open class Status(
    open val type: StatusType,
    open val name: String,
    open val fileUri: Uri,
    open val dateModified: Long,
    open val size: Long,
    open val clientPackage: String?,
    open val isSaved: Boolean
) : Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Status) return false

        if (type != other.type) return false
        if (name != other.name) return false
        if (fileUri != other.fileUri) return false
        if (dateModified != other.dateModified) return false
        if (size != other.size) return false
        if (clientPackage != other.clientPackage) return false
        if (isSaved != other.isSaved) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + fileUri.hashCode()
        result = 31 * result + dateModified.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + (clientPackage?.hashCode() ?: 0)
        result = 31 * result + isSaved.hashCode()
        return result
    }
}