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

import com.google.gson.annotations.SerializedName

class Country internal constructor(
    @SerializedName("country_code")
    val code: Int,
    @SerializedName("iso_code")
    val isoCode: String,
    @SerializedName("display_name")
    val displayName: String,
    @SerializedName("format")
    val format: String?
) {
    fun getId(): String = String.format("%s %s", isoCode, getFormattedCode())

    fun getFormattedCode() = String.format("+%d", code)

    fun getFormattedName() = String.format("%s • %s", displayName, isoCode)
}