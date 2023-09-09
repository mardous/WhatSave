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
import android.database.Cursor
import android.net.Uri
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.google.gson.GsonBuilder
import com.simplified.wsstatussaver.BuildConfig
import com.simplified.wsstatussaver.extensions.*
import com.simplified.wsstatussaver.getApp
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

private fun logInterceptor(): Interceptor {
    val loggingInterceptor = HttpLoggingInterceptor()
    if (BuildConfig.DEBUG) {
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
    } else {
        // disable retrofit log on release
        loggingInterceptor.level = HttpLoggingInterceptor.Level.NONE
    }
    return loggingInterceptor
}

private fun headerInterceptor(context: Context): Interceptor {
    return Interceptor {
        val original = it.request()
        val request = original.newBuilder()
            .header("User-Agent", context.packageName)
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .method(original.method, original.body)
            .build()
        it.proceed(request)
    }
}

fun provideDefaultCache(): Cache? {
    val cacheDir = File(getApp().cacheDir.absolutePath, "/okhttp-caches/")
    if (cacheDir.mkdirs() || cacheDir.isDirectory) {
        return Cache(cacheDir, 1024 * 1024 * 10)
    }
    return null
}

fun provideOkHttp(context: Context, cache: Cache): OkHttpClient {
    return OkHttpClient.Builder()
        .addNetworkInterceptor(logInterceptor())
        .addInterceptor(headerInterceptor(context))
        .connectTimeout(1, TimeUnit.SECONDS)
        .readTimeout(1, TimeUnit.SECONDS)
        .cache(cache)
        .build()
}

fun provideUpdateService(client: OkHttpClient): UpdateService {
    val gson = GsonBuilder()
        .setLenient()
        .create()

    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/repos/")
        .addConverterFactory(GsonConverterFactory.create(gson))
        .callFactory { request -> client.newCall(request) }
        .build()
    return retrofit.create(UpdateService::class.java)
}

private fun Context.getDownloadQuery(): Cursor? {
    val lastUpdateId = preferences().lastUpdateId
    if (lastUpdateId != -1L) {
        return getSystemService<DownloadManager>()?.query(
            DownloadManager.Query()
                .setFilterById(lastUpdateId)
        )
    }
    return null
}

private fun Cursor.isDownloading(): Boolean {
    val index = getColumnIndex(DownloadManager.COLUMN_STATUS)
    if (index == -1) return false
    val status = getInt(index)
    return status == DownloadManager.STATUS_PENDING || status == DownloadManager.STATUS_RUNNING
}

private fun Cursor.getLocalUri(): Uri? {
    val index = getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
    if (index == -1) return null
    val localUri = getString(index)
    if (!localUri.isNullOrEmpty()) {
        return localUri.toUri()
    }
    return null
}

fun Context.isDownloadingAnUpdate(): Boolean {
    return getDownloadQuery().use { downloadQuery ->
        if (downloadQuery != null && downloadQuery.moveToFirst()) {
            downloadQuery.isDownloading()
        } else false
    }
}

fun Context.getInstallableUpdateUri(): Uri? {
    return getDownloadQuery().use { downloadQuery ->
        if (downloadQuery != null && downloadQuery.moveToFirst()) {
            downloadQuery.getLocalUri()
        } else null
    }
}

fun Context.isAbleToUpdate(): Boolean {
    val minElapsedMillis = when (preferences().getUpdateSearchMode()) {
        UpdateSearchMode.EVERY_DAY -> TimeUnit.DAYS.toMillis(1)
        UpdateSearchMode.EVERY_FIFTEEN_DAYS -> TimeUnit.DAYS.toMillis(15)
        UpdateSearchMode.WEEKLY -> TimeUnit.DAYS.toMillis(7)
        UpdateSearchMode.MONTHLY -> TimeUnit.DAYS.toMillis(30)
        else -> -1
    }
    val elapsedMillis = System.currentTimeMillis() - preferences().lastUpdateSearch
    if ((minElapsedMillis >= -1) && elapsedMillis >= minElapsedMillis) {
        return isOnline(preferences().isUpdateOnlyWifi())
    }
    return false
}

fun Context.appUpgraded() {
    preferences().lastVersionCode = getApp().versionCode
    removeUpdate()
}

fun Context.removeUpdate() {
    val lastUpdateId = preferences().lastUpdateId
    if (lastUpdateId != -1L) {
        getSystemService<DownloadManager>()?.remove(lastUpdateId)
    }
    preferences().lastUpdateId = -1L
}

const val DEFAULT_USER = "mardous"
const val DEFAULT_REPO = "WhatSave"