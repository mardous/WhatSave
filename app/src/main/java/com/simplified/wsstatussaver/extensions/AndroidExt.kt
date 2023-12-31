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

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import com.simplified.wsstatussaver.getApp
import com.simplified.wsstatussaver.logUrlView
import java.io.Serializable
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import kotlin.reflect.KClass

typealias ExceptionConsumer = (Throwable, activityNotFound: Boolean) -> Unit
typealias ContextConsumer = (Context) -> Unit
typealias ViewConsumer = (View) -> Unit

fun Context.openWeb(url: String) {
    logUrlView(url)
    startActivitySafe(
        Intent(Intent.ACTION_VIEW, url.toUri())
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

fun Context.openGooglePlay(appPackage: String = packageName) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackage"))
    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
        return
    }
    startActivitySafe(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$appPackage".toUri()))
}

fun Context.doIHavePermissions(vararg permissions: String): Boolean {
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> return Environment.isExternalStorageManager()
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
            for (permission in permissions) {
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
        }
    }
    return true
}

@Suppress("DEPRECATION")
fun Context.isOnline(requestOnlyWifi: Boolean): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (hasM()) {
        val networkCapabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        if (networkCapabilities != null) {
            return if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                true
            } else networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) && !requestOnlyWifi
        }
    }
    val networkInfo = cm.activeNetworkInfo
    return networkInfo != null && networkInfo.isConnected && (!requestOnlyWifi || networkInfo.type == ConnectivityManager.TYPE_WIFI)
}

@Suppress("DEPRECATION")
inline fun <reified T : Serializable> Bundle.serializable(key: String, clazz: KClass<T>): T? =
    if (hasT()) {
        getSerializable(key, clazz.java)
    } else {
        val deserialized = getSerializable(key)
        if (clazz.isInstance(deserialized)) (deserialized as T) else null
    }

@Suppress("DEPRECATION")
@Throws(PackageManager.NameNotFoundException::class)
fun PackageManager.packageInfo(packageName: String = getApp().packageName): PackageInfo =
    if (hasT()) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else getPackageInfo(packageName, 0)

fun PackageInfo.versionCode() = PackageInfoCompat.getLongVersionCode(this).toInt()

fun Context.startActivitySafe(intent: Intent?, onError: ExceptionConsumer? = null) {
    intent.doWithIntent(onError) { startActivity(it) }
}

fun Context.startActivityForResultSafe(intent: Intent?, code: Int, onError: ExceptionConsumer? = null) {
    if (this is Activity) {
        intent.doWithIntent(onError) { startActivityForResult(it, code) }
    }
}

fun Context.showToast(messageRes: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, messageRes, duration).show()
}

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Intent?.toChooser(title: CharSequence? = null): Intent? {
    if (this == null) return null
    return Intent.createChooser(this, title)
}

fun String.formatted() = HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_COMPACT)

fun String.encodedUrl(charset: String = "UTF-8") = try {
    URLEncoder.encode(this, charset)
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

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.M)
fun hasM() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N)
fun hasN() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
fun hasQ() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
fun hasR() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
fun hasT() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU