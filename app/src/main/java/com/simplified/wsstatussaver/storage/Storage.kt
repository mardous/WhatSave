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
package com.simplified.wsstatussaver.storage

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import androidx.core.content.edit
import androidx.core.content.getSystemService
import com.simplified.wsstatussaver.extensions.PREFERENCE_STATUSES_LOCATION
import com.simplified.wsstatussaver.extensions.preferences

/**
 * @author Christians Martínez Alvarado (mardous)
 */
@SuppressLint("DiscouragedPrivateApi")
class Storage(context: Context) {

    private val preferences = context.preferences()
    private val storageManager = context.getSystemService<StorageManager>()!!
    private val storageVolumes = mutableListOf<StorageDevice>()
    private var storageVolumesLoaded = false

    val externalStoragePath: String
        get() = Environment.getExternalStorageDirectory().absolutePath

    private fun getStorageVolume(path: String): StorageDevice? {
        return getStorageVolumes().filterNot { it.path == null }.firstOrNull { it.path == path }
    }

    fun getStorageVolumes(): List<StorageDevice> {
        if (storageVolumes.isEmpty() && !storageVolumesLoaded) {
            storageManager.storageVolumes.forEach { volume ->
                createStorageDevice(volume)?.let {
                    storageVolumes.add(it)
                }
            }
            storageVolumesLoaded = true
        }
        return storageVolumes
    }

    fun getStatusesLocation(): StorageDevice? {
        return preferences.getString(PREFERENCE_STATUSES_LOCATION, null)
            ?.let { getStorageVolume(it) }
    }

    fun setStatusesLocation(storageVolume: StorageDevice) {
        val devicePath = storageVolume.path
        preferences.edit {
            if (devicePath.isNullOrEmpty())
                remove(PREFERENCE_STATUSES_LOCATION)
            else putString(PREFERENCE_STATUSES_LOCATION, devicePath)
        }
    }

    fun isStatusesLocation(storageVolume: StorageDevice): Boolean {
        return getStatusesLocation().let {
            if (it == null || !it.isValid)
                externalStoragePath == storageVolume.path
            else it == storageVolume
        }
    }

    fun isDefaultStatusesLocation(storageDevice: StorageDevice): Boolean {
        val devicePath = storageDevice.path
        return if (devicePath.isNullOrEmpty()) false else externalStoragePath == devicePath
    }

    private fun createStorageDevice(any: StorageVolume): StorageDevice? {
        val result = runCatching {
            val path = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> any.directory?.absolutePath
                else -> StorageVolume::class.java.getDeclaredMethod("getPath").invoke(any) as? String
            }
            StorageDevice(path, any.uuid, any.isRemovable, any.isPrimary, any.isEmulated, any.state)
        }
        return result.getOrNull()
    }
}