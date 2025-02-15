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

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.extensions.REGEX_BUSINESS
import com.simplified.wsstatussaver.extensions.REGEX_WHATSAPP
import com.simplified.wsstatussaver.extensions.getDrawableCompat
import com.simplified.wsstatussaver.extensions.packageInfo

enum class WaClient(
    val displayName: String,
    val packageName: String,
    private val iconRes: Int,
    val pathRegex: Regex
) {
    WhatsApp(
        "WhatsApp",
        "com.whatsapp",
        R.drawable.icon_wa,
        REGEX_WHATSAPP
    ),
    Business(
        "WhatsApp Business",
        "com.whatsapp.w4b",
        R.drawable.icon_business,
        REGEX_BUSINESS
    );

    fun getIcon(context: Context): Drawable? = context.getDrawableCompat(iconRes)

    fun isInstalled(context: Context): Boolean {
        return try {
            context.packageManager.packageInfo(packageName)
            true
        } catch (ignored: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getLaunchIntent(packageManager: PackageManager): Intent? {
        return packageManager.getLaunchIntentForPackage(packageName)
    }
}