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
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.UriPermission
import android.net.Uri
import android.os.Build
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.core.content.IntentCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.model.RequestedPermissions
import com.simplified.wsstatussaver.model.WaDirectory

const val STORAGE_PERMISSION_REQUEST = 100

val IsScopedStorageRequired = hasQ()
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

fun Uri.toWhatsAppDirectory() = WaDirectory.entries.firstOrNull { it.isThis(this) }

fun Context.getReadableDirectories() = contentResolver.persistedUriPermissions.getReadableDirectories()

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

fun Fragment.takePermissions(result: ActivityResult, isShowToast: Boolean = true): Boolean {
    val context = this.context
    if (result.resultCode == Activity.RESULT_OK && context != null) {
        val uri = result.data?.data ?: return false
        val directory = uri.toWhatsAppDirectory()
        if (directory != null && !directory.isLegacy) {
            if (!directory.isReadable(context)) { // Check if access has not already been given previously
                val mask = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, mask)
                if (isShowToast) showToast(R.string.permissions_granted_successfully)
                syncPermissions()
                return true
            }
        } else {
            if (isShowToast) showToast(R.string.select_the_correct_location, Toast.LENGTH_LONG)
        }
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

fun Fragment.directoryAccessRequestIntent(directory: WaDirectory = WaDirectory.Media): Intent {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
    if (hasQ()) {
        val storageManager = requireContext().getSystemService<StorageManager>()!!
        val treeIntent = storageManager.primaryStorageVolume.createOpenDocumentTreeIntent()
        val uri = IntentCompat.getParcelableExtra(treeIntent, DocumentsContract.EXTRA_INITIAL_URI, Uri::class.java)
        val scheme = uri.toString().replace("/root/", "/document/") + directory.path.encodedUrl()
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, scheme.toUri())
    }
    intent.setFlags(
        Intent.FLAG_GRANT_READ_URI_PERMISSION or
        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
    )
    return intent
}