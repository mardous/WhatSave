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
package com.simplified.wsstatussaver.storage

import android.os.Environment

/**
 * @author Christians Martínez Alvarado (mardous)
 */
class StorageDevice(val path: String?, val name: String, private val state: String) {

    val isValid: Boolean
        get() = state == Environment.MEDIA_MOUNTED && !path.isNullOrEmpty()

    override fun toString(): String {
        return "StorageDevice{path='$path', name='$name', state=$state}"
    }
}