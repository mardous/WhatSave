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

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.UriPermission
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.extensions.getDrawableCompat
import com.simplified.wsstatussaver.extensions.isFromClient
import com.simplified.wsstatussaver.extensions.packageInfo

enum class WaClient(
    val displayName: String,
    private val internalName: String,
    val packageName: String,
    private val iconRes: Int
) {
    WhatsApp("WhatsApp", "WhatsApp", "com.whatsapp", R.drawable.icon_wa),
    Business("WhatsApp Business", "WhatsApp Business", "com.whatsapp.w4b", R.drawable.icon_business);

    fun getIcon(context: Context): Drawable? = context.getDrawableCompat(iconRes)

    fun isInstalled(context: Context): Boolean {
        return try {
            context.packageManager.packageInfo(packageName)
            true
        } catch (ignored: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun hasPermissions(context: Context): Boolean {
        return hasPermissions(context.contentResolver.persistedUriPermissions)
    }

    fun hasPermissions(uriPermissions: List<UriPermission>): Boolean {
        return uriPermissions.any { it.isReadPermission && it.uri.isFromClient(this) }
    }

    fun releasePermissions(context: Context): Boolean {
        val uriPermissions = context.contentResolver.persistedUriPermissions
        for (perm in uriPermissions) {
            if (perm.uri.isFromClient(this)) {
                val flags =
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.releasePersistableUriPermission(perm.uri, flags)
                return true
            }
        }
        return false
    }

    fun getLaunchIntent(packageManager: PackageManager): Intent? {
        return packageManager.getLaunchIntentForPackage(packageName)
    }

    fun getDirectoryPath(): Array<String> {
        return arrayOf(
            "$internalName/Media/.Statuses",
            "Android/media/$packageName/$internalName/Media/.Statuses"
        )
    }

    @TargetApi(Build.VERSION_CODES.Q)
    fun getSAFDirectoryPath(): String {
        return "Android/media/$packageName/$internalName/Media/.Statuses"
    }
}