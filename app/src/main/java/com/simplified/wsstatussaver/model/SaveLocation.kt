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

import android.os.Environment

enum class SaveLocation(internal val videoDir: String, internal val imageDir: String) {
    DCIM(Environment.DIRECTORY_DCIM, Environment.DIRECTORY_DCIM),
    ByFileType(Environment.DIRECTORY_MOVIES, Environment.DIRECTORY_PICTURES),
    Custom(Environment.DIRECTORY_DCIM, Environment.DIRECTORY_DCIM);

    companion object {
        fun getWithoutCustom() = SaveLocation.entries.filterNot { it == Custom }
    }
}