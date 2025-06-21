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
package com.simplified.wsstatussaver

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase

private val firebaseCrashlytics: FirebaseCrashlytics by lazy { Firebase.crashlytics }

fun setAnalyticsEnabled(enabled: Boolean) {
    firebaseCrashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG && enabled)
}

fun recordException(throwable: Throwable) {
    firebaseCrashlytics.recordException(throwable)
}