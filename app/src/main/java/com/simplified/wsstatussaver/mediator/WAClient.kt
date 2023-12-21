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
import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.extensions.formattedAsHtml
import com.simplified.wsstatussaver.extensions.packageInfo

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
    var isOfficialClient: Boolean = false
) {

    fun getIcon(context: Context): Drawable? {
        return resolvePackageValue(context) {
            it?.applicationInfo?.loadIcon(context.packageManager) ?: getDrawable(context, R.drawable.ic_client_default)
        }
    }

    fun getLabel(context: Context): CharSequence? {
        return resolvePackageValue(context) {
            it?.applicationInfo?.loadLabel(context.packageManager) ?: name
        }
    }

    fun getDescription(context: Context): CharSequence {
        val messageRes = if (isOfficialClient) R.string.client_info else R.string.client_info_not_official
        val versionName = resolvePackageValue(context) {
            it?.versionName ?: context.getString(R.string.client_info_unknown)
        }
        return context.getString(messageRes, versionName).formattedAsHtml()
    }

    private fun <T> resolvePackageValue(context: Context, resolver: (PackageInfo?) -> T): T? {
        if (packageName.isNullOrEmpty()) {
            return null
        }
        val packageInfo = runCatching { context.packageManager.packageInfo(packageName!!) }
        if (packageInfo.isSuccess) {
            return resolver(packageInfo.getOrThrow())
        }
        return resolver(null)
    }
}