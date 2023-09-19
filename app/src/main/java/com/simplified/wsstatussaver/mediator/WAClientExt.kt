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

import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

internal typealias WAClientFilter = (WAClient) -> Boolean

fun WAClient.getLaunchIntent(packageManager: PackageManager): Intent? {
    if (packageName.isNullOrEmpty()) {
        return null
    }
    val intent = packageManager.getLaunchIntentForPackage(packageName!!)
    if (intent == null) {
        Log.w("WAMediator", "Couldn't find a launch intent for $packageName")
    }
    return intent
}