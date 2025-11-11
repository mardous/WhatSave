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
package com.simplified.wsstatussaver

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import com.simplified.wsstatussaver.extensions.getDefaultDayNightMode
import com.simplified.wsstatussaver.extensions.migratePreferences
import com.simplified.wsstatussaver.extensions.packageInfo
import com.simplified.wsstatussaver.extensions.preferences
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

fun getApp(): App = App.instance

/**
 * @author Christians Martínez Alvarado (mardous)
 */
class App : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        instance = this

        preferences().migratePreferences()

        startKoin {
            androidContext(this@App)
            modules(appModules)
        }

        AppCompatDelegate.setDefaultNightMode(preferences().getDefaultDayNightMode())
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }

    val versionName: String
        get() = packageManager.packageInfo().versionName ?: "0"

    val fullVersionName: String
        get() = versionName.let { if (isFDroidBuild()) "$it (F-Droid)" else it }

    companion object {
        internal lateinit var instance: App
            private set

        fun isFDroidBuild() = BuildConfig.FLAVOR.equals("fdroid", ignoreCase = true)

        fun getFileProviderAuthority(): String = instance.packageName + ".file_provider"
    }
}