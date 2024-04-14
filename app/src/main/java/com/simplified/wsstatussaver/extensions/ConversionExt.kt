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
package com.simplified.wsstatussaver.extensions

import org.ocpsoft.prettytime.PrettyTime
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

fun Long.time(maxPrettyTime: Long = 1, maxPrettyTimeUnit: TimeUnit = TimeUnit.HOURS, useTimeFormat: Boolean = false): String {
    val date = Date(this)
    val minElapsedHours = maxPrettyTimeUnit.toMillis(maxPrettyTime)
    if ((System.currentTimeMillis() - this) >= minElapsedHours) {
        val dateFormat = if (useTimeFormat) {
            SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        } else {
            SimpleDateFormat.getDateInstance(DateFormat.SHORT)
        }
        return dateFormat.format(date)
    }
    return prettyTime()
}

fun Long.prettyTime(): String = PrettyTime().format(Date(this))