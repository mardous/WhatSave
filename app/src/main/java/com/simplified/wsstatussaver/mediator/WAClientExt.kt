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

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.text.HtmlCompat
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.extensions.packageInfo

internal typealias WAClientFilter = (WAClient) -> Boolean

internal fun WAClient.toCompleteClient(context: Context, packageManager: PackageManager = context.packageManager) {
    try {
        val packageInfo = packageManager.packageInfo(this.packageName!!)
        val applicationInfo = packageInfo.applicationInfo
        if (appIcon == null) {
            appIcon = applicationInfo.loadIcon(packageManager)
        }
        if (appName == null) {
            appName = applicationInfo.loadLabel(packageManager)
        }
        if (appDescription == null) {
            val messageRes = if (isOfficialClient) R.string.client_info else R.string.client_info_not_official
            appDescription = HtmlCompat.fromHtml(
                context.getString(messageRes, packageInfo.versionName), HtmlCompat.FROM_HTML_MODE_COMPACT
            )
        }
    } catch (e: PackageManager.NameNotFoundException) {
        if (appIcon == null) {
            appIcon = AppCompatResources.getDrawable(context, R.drawable.ic_client_default)
        }
        if (appName == null) {
            appName = this.name
        }
        if (appDescription == null) {
            appDescription = context.getString(R.string.client_info_unknown)
        }
    }
}

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