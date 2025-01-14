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

import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Environment
import android.os.Parcelable
import androidx.core.content.edit
import androidx.core.net.toUri
import com.simplified.wsstatussaver.BuildConfig
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.extensions.packageInfo
import com.simplified.wsstatussaver.extensions.preferences
import com.simplified.wsstatussaver.extensions.toFileSize
import io.github.g00fy2.versioncompare.Version
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale

@Parcelize
@Serializable
class GitHubRelease(
    val name: String,
    @SerialName("tag_name")
    val tag: String,
    @SerialName("html_url")
    val url: String,
    @SerialName("published_at")
    val date: String,
    val body: String,
    @SerialName("prerelease")
    val isPrerelease: Boolean,
    @SerialName("assets")
    val downloads: List<ReleaseAsset>
) : Parcelable {

    companion object {
        private const val IGNORED_RELEASE = "ignored_release"
    }

    fun isDownloadable(context: Context): Boolean {
        val hasApk = downloads.any { it.isApk }
        if (hasApk) {
            return isNewer(context) && !isIgnored(context)
        }
        return false
    }

    private fun isIgnored(context: Context): Boolean {
        return context.preferences().getString(IGNORED_RELEASE, null) == tag
    }

    fun setIgnored(context: Context) {
        context.preferences().edit {
            putString(IGNORED_RELEASE, tag)
        }
    }

    fun isNewer(context: Context): Boolean {
        try {
            val packageInfo = context.packageManager.packageInfo()
            val installedVersionName = packageInfo.versionName ?: return true
            var updateVersionName = this.tag
            if (updateVersionName.startsWith("v", ignoreCase = true)) {
                updateVersionName = updateVersionName.substring(1)
            }
            return Version(updateVersionName) > Version(installedVersionName)
        } catch (ignored: NameNotFoundException) {
        }
        return true // assume true
    }

    fun getDownloadSize(): String? {
        val assetSize = downloads.firstOrNull { it.isApk }
        return assetSize?.size?.toFileSize()
    }

    fun getDownloadRequest(context: Context): DownloadManager.Request? {
        val apkAsset = downloads.firstOrNull { it.isApk }
        if (apkAsset != null) {
            return DownloadManager.Request(apkAsset.downloadUrl.toUri())
                .setTitle(apkAsset.name)
                .setDescription(context.getString(R.string.downloading_update))
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, apkAsset.name)
                .setMimeType(ReleaseAsset.APK_MIME_TYPE)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        }
        return null
    }

    fun getFormattedDate(): String? {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        val date = format.parse(date)
        if (date != null) {
            return SimpleDateFormat.getDateInstance().format(date)
        }
        return null
    }
}

@Parcelize
@Serializable
class ReleaseAsset(
    val name: String,
    @SerialName("content_type")
    val contentType: String,
    val state: String,
    val size: Long,
    @SerialName("browser_download_url")
    val downloadUrl: String
) : Parcelable {

    val isApk: Boolean
        get() = contentType == APK_MIME_TYPE && name.contains(BuildConfig.FLAVOR)

    companion object {
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}