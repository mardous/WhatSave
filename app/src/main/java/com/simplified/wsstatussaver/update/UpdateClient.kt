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
package com.simplified.wsstatussaver.update

import android.content.Context
import com.simplified.wsstatussaver.extensions.UpdateSearchMode
import com.simplified.wsstatussaver.extensions.getUpdateSearchMode
import com.simplified.wsstatussaver.extensions.isOnline
import com.simplified.wsstatussaver.extensions.isUpdateOnlyWifi
import com.simplified.wsstatussaver.extensions.lastUpdateSearch
import com.simplified.wsstatussaver.extensions.preferences
import java.util.concurrent.TimeUnit

fun Context.isAbleToUpdate(): Boolean {
    val minElapsedMillis = when (preferences().getUpdateSearchMode()) {
        UpdateSearchMode.EVERY_DAY -> TimeUnit.DAYS.toMillis(1)
        UpdateSearchMode.EVERY_FIFTEEN_DAYS -> TimeUnit.DAYS.toMillis(15)
        UpdateSearchMode.WEEKLY -> TimeUnit.DAYS.toMillis(7)
        UpdateSearchMode.MONTHLY -> TimeUnit.DAYS.toMillis(30)
        else -> -1
    }
    val elapsedMillis = System.currentTimeMillis() - preferences().lastUpdateSearch
    if ((minElapsedMillis > -1) && elapsedMillis >= minElapsedMillis) {
        return isOnline(preferences().isUpdateOnlyWifi())
    }
    return false
}

const val DEFAULT_USER = "mardous"
const val DEFAULT_REPO = "WhatSave"