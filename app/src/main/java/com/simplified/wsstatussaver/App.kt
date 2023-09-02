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
import com.simplified.wsstatussaver.extensions.getDefaultDayNightMode
import com.simplified.wsstatussaver.extensions.packageInfo
import com.simplified.wsstatussaver.extensions.preferences
import com.simplified.wsstatussaver.extensions.versionCode
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

fun getApp(): App = App.instance

/**
 * @author Christians Martínez Alvarado (mardous)
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        startKoin {
            androidContext(this@App)
            modules(appModules)
        }

        AppCompatDelegate.setDefaultNightMode(preferences().getDefaultDayNightMode())
    }

    val versionName: String
        get() = packageManager.packageInfo()?.versionName ?: "0"

    val versionCode: Int
        get() = packageManager.packageInfo()?.versionCode() ?: 0

    companion object {
        internal lateinit var instance: App
            private set

        fun getFileProviderAuthority(): String = instance.packageName + ".file_provider"
    }
}