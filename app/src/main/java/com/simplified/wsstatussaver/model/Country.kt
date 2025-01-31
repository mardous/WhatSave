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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Country internal constructor(
    @SerialName("country_code")
    val code: Int,
    @SerialName("iso_code")
    val isoCode: String,
    @SerialName("display_name")
    val displayName: String
) {
    fun getId(): String = String.format("%s %s", isoCode, getFormattedCode())

    fun getFormattedCode() = String.format("+%d", code)
}