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
import com.simplified.wsstatussaver.extensions.hasN
import com.simplified.wsstatussaver.extensions.preferences
import java.lang.reflect.Array
import java.lang.reflect.InvocationTargetException

/**
 * @author Christians Martínez Alvarado (mardous)
 */
@SuppressLint("DiscouragedPrivateApi")
class Storage(context: Context) {

    private val preferences = context.preferences()
    private val storageManager = context.getSystemService<StorageManager>()!!
    private val storageVolumes = mutableListOf<StorageDevice>()

    val externalStoragePath: String
        get() = Environment.getExternalStorageDirectory().absolutePath

    private fun getStorageVolume(path: String): StorageDevice? {
        return storageVolumes.filterNot { it.path == null }.firstOrNull { it.path == path }
    }

    fun getStorageVolumes(): List<StorageDevice> {
        val storageVolumes = arrayListOf<StorageDevice>()
        try {
            if (hasN()) {
                for (volume in storageManager.storageVolumes) {
                    createStorageDevice(storageManager, volume)?.let { storageVolumes.add(it) }
                }
            } else {
                val array = StorageManager::class.java.getDeclaredMethod("getVolumeList").invoke(storageManager)
                val length = array?.let { Array.getLength(it) }
                if (length != null) for (i in 0 until length) {
                    createStorageDevice(storageManager, Array.get(array, i))?.let { storageVolumes.add(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return storageVolumes
    }

    fun getStatusesLocation(): StorageDevice? {
        return preferences.getString(PREFERENCE_STATUSES_LOCATION, null)?.let { getStorageVolume(it) }
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

    @SuppressLint("SoonBlockedPrivateApi")
    @Throws(NoSuchMethodException::class, InvocationTargetException::class, IllegalAccessException::class)
    private fun createStorageDevice(storageManager: StorageManager, any: Any?): StorageDevice? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (any is StorageVolume) {
                return StorageDevice(
                    any.directory!!.absolutePath, any.uuid, any.isRemovable, any.isPrimary, any.isEmulated, any.state
                )
            }
        } else if (hasN()) {
            if (any is StorageVolume) {
                return StorageDevice(
                    StorageVolume::class.java.getDeclaredMethod("getPath").invoke(any) as String,
                    any.uuid, any.isRemovable, any.isPrimary, any.isEmulated, any.state
                )
            }
        } else if (any != null) {
            val path = any.javaClass.getDeclaredMethod("getPath").invoke(any) as String
            val uuid = any.javaClass.getDeclaredMethod("getUuid").invoke(any) as String
            val isEmulated = any.javaClass.getDeclaredMethod("isEmulated").invoke(any) as Boolean
            val isRemovable = any.javaClass.getDeclaredMethod("isRemovable").invoke(any) as Boolean
            val isPrimary = any.javaClass.getDeclaredMethod("isPrimary").invoke(any) as Boolean
            val state = StorageManager::class.java.getMethod("getVolumeState", String::class.java)
                .invoke(storageManager, path) as String
            return StorageDevice(path, uuid, isRemovable, isPrimary, isEmulated, state)
        }
        return null
    }
}