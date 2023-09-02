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

/**
 * @author Christians Martínez Alvarado (mardous)
 */
open class Status(
    val type: StatusType,
    val name: String,
    val path: String,
    val dateModified: Long,
    val size: Long,
    val clientPackage: String?,
    val isSaved: Boolean
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val status = other as Status
        if (dateModified != status.dateModified) return false
        if (size != status.size) return false
        if (type !== status.type) return false
        if (name != status.name) return false
        if (clientPackage != status.clientPackage) return false
        return path == status.path
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + clientPackage.hashCode()
        result = 31 * result + (dateModified xor (dateModified ushr 32)).toInt()
        result = 31 * result + (size xor (size ushr 32)).toInt()
        return result
    }

    override fun toString(): String {
        return "Status{type=$type, name='$name', path='$path', dateModified=$dateModified, size=$size, clientPackage='$clientPackage'}"
    }
}