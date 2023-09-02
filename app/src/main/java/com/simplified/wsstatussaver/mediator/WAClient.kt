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
package com.simplified.wsstatussaver.mediator

import android.graphics.drawable.Drawable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * @author Christians Martínez Alvarado (mardous)
 */
data class WAClient(
    @Expose
    @SerializedName("name")
    var name: String? = null,
    @Expose
    @SerializedName("package")
    var packageName: String? = null,
    @Expose
    @SerializedName("directories")
    var statusesDirectories: List<String>? = null,
    @Expose
    @SerializedName("official")
    var isOfficialClient: Boolean = false) {

    var appIcon: Drawable? = null
    var appName: CharSequence? = null
    var appDescription: CharSequence? = null
}