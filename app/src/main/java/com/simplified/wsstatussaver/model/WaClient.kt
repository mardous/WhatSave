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
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.extensions.formattedAsHtml
import com.simplified.wsstatussaver.extensions.getDrawableCompat
import com.simplified.wsstatussaver.extensions.isFromClient
import com.simplified.wsstatussaver.extensions.packageInfo

enum class WaClient(val displayName: String, val packageName: String) {
    WhatsApp("WhatsApp", "com.whatsapp"),
    Business("WhatsApp Business", "com.whatsapp.w4b"),
    OGWhatsApp("OGWhatsApp", "com.gbwhatsapp3");

    fun getIcon(context: Context): Drawable? {
        return resolvePackageValue(context) {
            it?.applicationInfo?.loadIcon(context.packageManager)
                ?: context.getDrawableCompat(R.drawable.ic_client_default)
        }
    }

    fun getLabel(context: Context): CharSequence? {
        return resolvePackageValue(context) { it?.applicationInfo?.loadLabel(context.packageManager) ?: name }
    }

    fun getDescription(context: Context): CharSequence {
        val versionName = resolvePackageValue(context) { it?.versionName }
        if (versionName == null) {
            return context.getString(R.string.client_info_unknown)
        }
        return context.getString(R.string.client_info, versionName).formattedAsHtml()
    }

    fun isInstalled(context: Context): Boolean {
        return try {
            context.packageManager.packageInfo(packageName)
            true
        } catch (ignored: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun hasPermissions(context: Context): Boolean {
        val uriPermissions = context.contentResolver.persistedUriPermissions
        return uriPermissions.any { it.isReadPermission && it.uri.isFromClient(this) }
    }

    fun getLaunchIntent(packageManager: PackageManager): Intent? {
        return packageManager.getLaunchIntentForPackage(packageName)
    }

    fun getDirectoryPath(): String {
        return "$displayName/Media/.Statuses"
    }

    @TargetApi(Build.VERSION_CODES.Q)
    fun getSAFDirectoryPath(): String {
        return "Android/media/$packageName/$displayName/Media"
    }

    private fun <T> resolvePackageValue(context: Context, resolver: (PackageInfo?) -> T): T? {
        if (packageName.isEmpty()) {
            return null
        }
        val packageInfo = runCatching { context.packageManager.packageInfo(packageName) }
        if (packageInfo.isSuccess) {
            return resolver(packageInfo.getOrThrow())
        }
        return resolver(null)
    }
}