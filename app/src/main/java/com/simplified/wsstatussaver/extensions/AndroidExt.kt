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
package com.simplified.wsstatussaver.extensions

import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.service.notification.NotificationListenerService
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.core.os.BundleCompat
import androidx.core.text.HtmlCompat
import com.simplified.wsstatussaver.getApp
import com.simplified.wsstatussaver.service.MessageCatcherService
import java.io.Serializable
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import kotlin.reflect.KClass

typealias ExceptionConsumer = (Throwable, activityNotFound: Boolean) -> Unit
typealias ContextConsumer = (Context) -> Unit
typealias ViewConsumer = (View) -> Unit

fun Context.getDrawableCompat(resId: Int) = AppCompatResources.getDrawable(this, resId)

fun Context.openWeb(url: String) {
    startActivitySafe(
        Intent(Intent.ACTION_VIEW, url.toUri())
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

fun Context.bindNotificationListener(): Boolean {
    if (isNotificationListener()) {
        return try {
            NotificationListenerService.requestRebind(ComponentName(this, MessageCatcherService::class.java))
            true
        } catch (_: Throwable) {
            false
        }
    }
    return false
}

fun Context.isNotificationListener(): Boolean {
    if (!hasOMR1()) {
        return NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
    }
    val componentName = ComponentName(this, MessageCatcherService::class.java)
    val notificationManager = getSystemService<NotificationManager>()
    return notificationManager?.isNotificationListenerAccessGranted(componentName) == true
}

fun Context.doIHavePermissions(vararg permissions: String): Boolean {
    if (permissions.isEmpty()) return true
    for (permission in permissions) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
    }
    return true
}

fun Context.openSettings(action: String, packageName: String? = this.packageName) {
    val intent = Intent(action)
    if (!packageName.isNullOrEmpty()) {
        intent.data = Uri.fromParts("package", packageName, null)
    }
    startActivitySafe(intent)
}

fun Context.isOnline(requestOnlyWifi: Boolean): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkCapabilities = cm.getNetworkCapabilities(cm.activeNetwork)
    if (networkCapabilities != null) {
        return if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            true
        } else networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) && !requestOnlyWifi
    }
    return false
}

inline fun <reified T : Parcelable> Bundle.parcelableList(key: String, clazz: KClass<T>) =
    BundleCompat.getParcelableArrayList(this, key, clazz.java)

@Suppress("DEPRECATION")
inline fun <reified T : Serializable> Bundle.serializable(key: String, clazz: KClass<T>): T? =
    if (hasT()) {
        getSerializable(key, clazz.java)
    } else {
        val deserialized = getSerializable(key)
        if (clazz.isInstance(deserialized)) (deserialized as T) else null
    }

@Throws(PackageManager.NameNotFoundException::class)
fun PackageManager.packageInfo(packageName: String = getApp().packageName): PackageInfo =
    if (hasT()) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else getPackageInfo(packageName, 0)

fun Context.startActivitySafe(intent: Intent?, onError: ExceptionConsumer? = null) {
    intent.doWithIntent(onError) { startActivity(it) }
}

fun Context.showToast(messageRes: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, messageRes, duration).show()
}

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun String.formattedAsHtml() = HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_COMPACT)

fun String.encodedUrl(charset: String = "UTF-8") = try {
    URLEncoder.encode(this, charset)
} catch (e: UnsupportedEncodingException) {
    null
}

fun String.decodedUrl(charset: String = "UTF-8") = try {
    URLDecoder.decode(this, charset)
} catch (e: UnsupportedEncodingException) {
    null
}

internal fun Intent?.doWithIntent(onError: ExceptionConsumer?, doAction: (Intent) -> Unit) {
    if (this == null) {
        onError?.invoke(NullPointerException("input intent is null"), false)
    } else {
        kotlin.runCatching {
            doAction(this)
        }.onFailure {
            onError?.invoke(it, it is ActivityNotFoundException)
        }
    }
}

fun <T> ActivityResultLauncher<T>.launchSafe(input: T, options: ActivityOptionsCompat? = null) {
    try {
        launch(input, options)
    } catch (e: ActivityNotFoundException) {
        e.printStackTrace()
    }
}

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O_MR1)
fun hasOMR1() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
fun hasP() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
fun hasQ() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
fun hasR() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
fun hasT() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU