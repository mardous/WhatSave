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
package com.simplified.wsstatussaver.extensions

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.model.RequestedPermissions

const val STORAGE_PERMISSION_REQUEST = 100

@SuppressLint("InlinedApi")
fun getRequestedPermissions(): Array<RequestedPermissions> {
    return arrayOf(
        RequestedPermissions(1..Build.VERSION_CODES.P, WRITE_EXTERNAL_STORAGE),
        RequestedPermissions(1..Build.VERSION_CODES.S_V2, READ_EXTERNAL_STORAGE),
        RequestedPermissions(Build.VERSION_CODES.TIRAMISU, READ_MEDIA_IMAGES, READ_MEDIA_VIDEO)
    )
}

fun getApplicablePermissions() = getRequestedPermissions()
    .filter { it.isApplicable() }
    .flatMap { it.permissions.asIterable() }
    .toTypedArray()

fun Context.hasStoragePermissions(): Boolean = doIHavePermissions(*getApplicablePermissions())

fun Context.hasSAFPermissions(): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || getAllInstalledClients().any { it.hasPermissions(this) }
}

fun Context.hasPermissions() = hasStoragePermissions() && hasSAFPermissions()

fun FragmentActivity.requestWithOnboard() {
    preferences().isShownOnboard = false
    findNavController(R.id.main_container).navigate(R.id.onboardFragment)
}

fun FragmentActivity.requestWithoutOnboard() {
    requestPermissions(getApplicablePermissions(), STORAGE_PERMISSION_REQUEST)
}

fun FragmentActivity.requestPermissions(isShowOnboard: Boolean = false) {
    if (isShowOnboard) {
        val navController = findNavController(R.id.main_container)
        if (navController.currentDestination?.id == R.id.onboardFragment) {
            requestWithoutOnboard()
        } else {
            requestWithOnboard()
        }
    } else {
        requestWithoutOnboard()
    }
}

fun Fragment.hasStoragePermissions() = requireContext().hasStoragePermissions()

fun Fragment.hasPermissions() = requireContext().hasPermissions()

fun Fragment.requestWithoutOnboard() = requireActivity().requestWithoutOnboard()

fun Fragment.requestPermissions() = requireActivity().requestPermissions(true)