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

import android.content.Context
import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics


private val ANALYTICS_ENABLED = !BuildConfig.DEBUG

fun Context.logToolView(clazz: String, name: String) {
    if (!ANALYTICS_ENABLED) {
        return
    }

    val bundle = bundleOf(
        FirebaseAnalytics.Param.SCREEN_CLASS to clazz,
        FirebaseAnalytics.Param.SCREEN_NAME to name
    )
    FirebaseAnalytics.getInstance(this)
        .logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
}

fun Context.logUpdateRequest(newVersion: String, accepted: Boolean) {
    if (!ANALYTICS_ENABLED) {
        return
    }

    val bundle = bundleOf(
        "new_version" to newVersion,
        "user_accepted" to accepted
    )
    FirebaseAnalytics.getInstance(this)
        .logEvent("request_app_update", bundle)
}

fun Context.logAppUpgrade(oldVersion: Int, newVersion: Int) {
    if (!ANALYTICS_ENABLED) {
        return
    }

    val bundle = bundleOf(
        "old_version_code" to oldVersion,
        "new_version_code" to newVersion
    )
    FirebaseAnalytics.getInstance(this)
        .logEvent("apply_app_update", bundle)
}

fun Context.logDefaultClient(packageName: String) {
    if (!ANALYTICS_ENABLED) {
        return
    }

    FirebaseAnalytics.getInstance(this)
        .logEvent("select_default_client", bundleOf("client_id" to packageName))
}

fun Context.logUrlView(url: String) {
    if (!ANALYTICS_ENABLED) {
        return
    }

    FirebaseAnalytics.getInstance(this)
        .logEvent("open_url", bundleOf("url" to url))
}

fun recordException(throwable: Throwable) {
    if (!ANALYTICS_ENABLED) {
        return
    }
    FirebaseCrashlytics.getInstance().recordException(throwable)
}