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
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import androidx.core.content.edit
import androidx.core.content.getSystemService
import com.simplified.wsstatussaver.extensions.PREFERENCE_STATUSES_LOCATION
import com.simplified.wsstatussaver.extensions.hasR
import com.simplified.wsstatussaver.extensions.preferences
import com.simplified.wsstatussaver.getApp
import com.simplified.wsstatussaver.recordException
import java.lang.reflect.InvocationTargetException

/**
 * @author Christians Martínez Alvarado (mardous)
 */
@SuppressLint("DiscouragedPrivateApi")
class Storage(context: Context) {

    private val preferences = context.preferences()
    private val storageManager = context.getSystemService<StorageManager>()!!

    val externalStoragePath: String
        get() = Environment.getExternalStorageDirectory().absolutePath

    val storageVolumes: List<StorageDevice> by lazy {
        arrayListOf<StorageDevice>().also { newList ->
            try {
                for (sv in storageManager.storageVolumes) {
                    newList.add(
                        StorageDevice(
                            sv.getPath(),
                            sv.getDescription(getApp().applicationContext),
                            sv.state
                        )
                    )
                }
            } catch (t: Throwable) {
                recordException(t)
            }
        }
    }

    private fun getStorageVolume(path: String): StorageDevice? {
        return storageVolumes.filterNot { it.path == null }.firstOrNull { it.path == path }
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

    @Throws(
        NoSuchMethodException::class,
        InvocationTargetException::class,
        IllegalAccessException::class
    )
    @SuppressLint("DiscouragedPrivateApi")
    private fun StorageVolume.getPath(): String? {
        return if (hasR()) {
            this.directory?.absolutePath
        } else {
            StorageVolume::class.java.getDeclaredMethod("getPath").invoke(this) as? String
        }
    }
}