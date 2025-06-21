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
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.UriPermission
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.model.RequestedPermissions
import com.simplified.wsstatussaver.model.WaDirectory
import com.simplified.wsstatussaver.recordException

const val STORAGE_PERMISSION_REQUEST = 100

val IsScopedStorageRequired = hasR()
val IsSAFRequired = hasR()

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

fun Uri.isTreeUri() = DocumentsContract.isTreeUri(this)

fun Uri.isWhatsAppDirectory() = WaDirectory.entries.any { it.isThis(this) }

fun Uri.toWhatsAppDirectory() = WaDirectory.entries.firstOrNull { it.isThis(this) }

fun Uri.isCustomSaveDirectory(contentResolver: ContentResolver): Boolean {
    if (!isTreeUri() || isWhatsAppDirectory())
        return false

    return contentResolver.allPermissionsGranted(this)
}

fun Context.getReadableDirectories() = contentResolver.persistedUriPermissions.getReadableDirectories()

fun ContentResolver.takePermissions(uri: Uri, flags: Int): Boolean {
    val result = runCatching { takePersistableUriPermission(uri, flags) }
    if (result.isFailure) {
        result.exceptionOrNull()?.let { recordException(it) }
    }
    return result.isSuccess
}

fun ContentResolver.allPermissionsGranted(against: Uri) =
    persistedUriPermissions.any { it.allPermissionsGranted(against) }

fun UriPermission.allPermissionsGranted(against: Uri) = uri == against && isWritePermission && isReadPermission

fun List<UriPermission>.getReadableDirectories() = WaDirectory.entries.filter { it.isReadable(this) }

fun Context.hasStoragePermissions(): Boolean = doIHavePermissions(*getApplicablePermissions())

fun Context.hasSAFPermissions(): Boolean = !IsSAFRequired || getReadableDirectories().isNotEmpty()

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

fun Fragment.getReadableDirectories() = requireContext().getReadableDirectories()

fun Fragment.hasStoragePermissions() = requireContext().hasStoragePermissions()

fun Fragment.hasSAFPermissions() = requireContext().hasSAFPermissions()

fun Fragment.hasPermissions() = requireContext().hasPermissions()

fun Fragment.requestWithoutOnboard() = requireActivity().requestWithoutOnboard()

fun Fragment.requestPermissions() = requireActivity().requestPermissions(true)

fun Fragment.takePermissions(selectedUri: Uri?, isShowToast: Boolean = true): Boolean {
    val context = this.context
    if (selectedUri == null || context == null) return false
    val directory = selectedUri.toWhatsAppDirectory()
    if (directory != null && !directory.isLegacy) {
        if (!directory.isReadable(context)) { // Check if access has not already been given previously
            val mask = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(selectedUri, mask)
            if (isShowToast) showToast(R.string.permissions_granted_successfully)
            syncPermissions()
            return true
        }
    } else {
        if (isShowToast) showToast(R.string.select_the_correct_location, Toast.LENGTH_LONG)
    }
    return false
}

fun Fragment.releasePermissions(): Boolean {
    val context = this.context ?: return false
    return context.getReadableDirectories().all {
        it.releasePermissions(context)
    }
}

fun Fragment.syncPermissions() {
    val directories = getReadableDirectories()
    val legacyDirectories = directories.filter { it.isLegacy }
    if (legacyDirectories.isNotEmpty() && directories.any { it == WaDirectory.Media }) {
        for (dir in legacyDirectories) {
            dir.releasePermissions(requireContext())
        }
    }
}