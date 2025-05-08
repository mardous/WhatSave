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
package com.simplified.wsstatussaver.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.MediaStore.MediaColumns
import androidx.lifecycle.LiveData
import com.simplified.wsstatussaver.database.StatusDao
import com.simplified.wsstatussaver.database.StatusEntity
import com.simplified.wsstatussaver.database.toStatusEntity
import com.simplified.wsstatussaver.extensions.IsSAFRequired
import com.simplified.wsstatussaver.extensions.IsScopedStorageRequired
import com.simplified.wsstatussaver.extensions.acceptFileName
import com.simplified.wsstatussaver.extensions.getAllInstalledClients
import com.simplified.wsstatussaver.extensions.getPreferred
import com.simplified.wsstatussaver.extensions.getUri
import com.simplified.wsstatussaver.extensions.hasStoragePermissions
import com.simplified.wsstatussaver.extensions.isExcludeSavedStatuses
import com.simplified.wsstatussaver.extensions.preferences
import com.simplified.wsstatussaver.model.SavedStatus
import com.simplified.wsstatussaver.model.ShareData
import com.simplified.wsstatussaver.model.Status
import com.simplified.wsstatussaver.model.StatusQueryResult
import com.simplified.wsstatussaver.model.StatusQueryResult.ResultCode
import com.simplified.wsstatussaver.model.StatusType
import com.simplified.wsstatussaver.storage.whatsapp.WaSavedContentStorage
import com.simplified.wsstatussaver.storage.whatsapp.WaContentStorage
import java.io.File
import java.util.Date
import java.util.concurrent.TimeUnit

interface StatusesRepository {
    fun statusIsSaved(status: Status): LiveData<Boolean>
    suspend fun statuses(type: StatusType): StatusQueryResult
    suspend fun savedStatuses(): StatusQueryResult
    suspend fun savedStatuses(type: StatusType): StatusQueryResult
    suspend fun savedStatusesObservable(type: StatusType): LiveData<List<StatusEntity>>
    suspend fun removeFromDatabase(status: Status)
    suspend fun removeFromDatabase(statuses: List<Status>)
    suspend fun share(status: Status): ShareData
    suspend fun share(statuses: List<Status>): ShareData
    suspend fun save(status: Status, saveName: String?): Uri?
    suspend fun save(statuses: List<Status>): Map<Status, Uri>
    suspend fun delete(status: Status): Boolean
    suspend fun delete(statuses: List<Status>): Int
}

class StatusesRepositoryImpl(
    private val context: Context,
    private val statusDao: StatusDao,
    private val waContentStorage: WaContentStorage,
    private val waSavedContentStorage: WaSavedContentStorage
) : StatusesRepository {

    private val contentResolver: ContentResolver = context.contentResolver
    private val preferences = context.preferences()

    override fun statusIsSaved(status: Status): LiveData<Boolean> =
        statusDao.statusSavedObservable(status.fileUri, status.name)

    override suspend fun statuses(type: StatusType): StatusQueryResult {
        val statusList = arrayListOf<Status>()
        val isExcludeSaved = preferences.isExcludeSavedStatuses()
        val installedClients = context.getAllInstalledClients().getPreferred(context)
        if (installedClients.isEmpty()) {
            return StatusQueryResult(ResultCode.NotInstalled)
        }
        if (IsSAFRequired) {
            val statusesDirectories = waContentStorage.statusDirectories(installedClients)
            if (statusesDirectories.isEmpty()) {
                return StatusQueryResult(ResultCode.PermissionError)
            }
            waContentStorage.resolveFiles(statusesDirectories) { file ->
                if (type.acceptFileName(file.name)) {
                    val isOld = file.isOlderThan(TimeUnit.HOURS, 24)
                    val isSaved = statusDao.statusSaved(file.uri, file.name)
                    if (!isOld && (!isSaved || !isExcludeSaved)) {
                        statusList.add(file.toStatus(type, isSaved))
                    }
                }
            }
        } else {
            if (context.hasStoragePermissions()) {
                for (client in installedClients) {
                    val directories = waContentStorage.statusDirectoriesAsFiles(client)
                    waContentStorage.resolveFiles(directories, type, client) { file ->
                        val isOld = file.isOlderThan(TimeUnit.HOURS, 24)
                        val isSaved = statusDao.statusSaved(file.uri, file.name)
                        if (!isOld && (!isSaved || !isExcludeSaved)) {
                            statusList.add(file.toStatus(type, isSaved))
                        }
                    }
                }
            }
        }
        if (statusList.isNotEmpty()) {
            return StatusQueryResult(ResultCode.Success, statusList.sortedByDescending { it.dateModified })
        }
        return StatusQueryResult(ResultCode.NoStatuses)
    }

    override suspend fun savedStatuses(): StatusQueryResult {
        if (!context.hasStoragePermissions()) {
            return StatusQueryResult(ResultCode.PermissionError)
        }
        val statuses = arrayListOf<SavedStatus>()
        for (type in StatusType.entries) {
            val customSaveDir = waSavedContentStorage.getCustomSaveDirectory(type)
            if (customSaveDir != null) {
                waContentStorage.resolveFiles(setOf(customSaveDir)) {
                    statuses.add(it.toSavedStatus(type))
                }
            } else {
                if (IsScopedStorageRequired) {
                    waSavedContentStorage.getSavedMedia(type).use { cursor ->
                        if (cursor != null && cursor.moveToFirst()) do {
                            statuses.add(cursor.getSavedStatus(type))
                        } while (cursor.moveToNext())
                    }
                } else {
                    val directories = waSavedContentStorage.getSaveDirectories(type)
                    waContentStorage.resolveFiles(directories, type, null) {
                        statuses.add(it.toSavedStatus(type))
                    }
                }
            }
        }
        if (statuses.isEmpty()) {
            return StatusQueryResult(ResultCode.NoSavedStatuses)
        }
        return StatusQueryResult(ResultCode.Success, statuses.sortedByDescending { it.dateModified })
    }

    override suspend fun savedStatuses(type: StatusType): StatusQueryResult {
        if (!context.hasStoragePermissions()) {
            return StatusQueryResult(ResultCode.PermissionError)
        }
        val statuses = arrayListOf<SavedStatus>()
        val customSaveDir = waSavedContentStorage.getCustomSaveDirectory(type)
        if (customSaveDir != null) {
            waContentStorage.resolveFiles(setOf(customSaveDir)) {
                statuses.add(it.toSavedStatus(type))
            }
        } else {
            if (IsScopedStorageRequired) {
                waSavedContentStorage.getSavedMedia(type).use { cursor ->
                    if (cursor != null && cursor.moveToFirst()) do {
                        statuses.add(cursor.getSavedStatus(type))
                    } while (cursor.moveToNext())
                }
            } else {
                waContentStorage.resolveFiles(waSavedContentStorage.getSaveDirectories(type), type, null) {
                    statuses.add(it.toSavedStatus(type))
                }
            }
        }
        if (statuses.isEmpty()) {
            return StatusQueryResult(ResultCode.NoSavedStatuses)
        }
        return StatusQueryResult(ResultCode.Success, statuses.sortedByDescending { it.dateModified })
    }

    override suspend fun savedStatusesObservable(type: StatusType): LiveData<List<StatusEntity>> =
        statusDao.savedStatuses(type.ordinal)

    override suspend fun removeFromDatabase(status: Status) {
        statusDao.removeSave(status.name)
    }

    override suspend fun removeFromDatabase(statuses: List<Status>) {
        statusDao.removeSaves(statuses.map { it.name }.toSet())
    }

    private fun Cursor.getSavedStatus(type: StatusType): SavedStatus {
        val mediaUri = ContentUris.withAppendedId(type.contentUri, getLong(0))
        val name = getString(1)
        val dateModified = getLong(2)
        val size = getLong(3)
        return SavedStatus(type, name, mediaUri, dateModified * 1000, size, null)
    }

    override suspend fun share(status: Status): ShareData {
        if (IsSAFRequired && status !is SavedStatus) {
            val cacheDir = context.externalCacheDir
            if (cacheDir == null || (!cacheDir.exists() && !cacheDir.mkdirs())) {
                return ShareData(status.fileUri, status.type.mimeType)
            }
            val temp = File(cacheDir, status.type.getDefaultSaveName(Date().time, 0))
            if (!temp.exists() || temp.delete()) {
                try {
                    val inputStream = contentResolver.openInputStream(status.fileUri)
                    if (inputStream != null) {
                        inputStream.use {
                            temp.outputStream().use { outputStream ->
                                it.copyTo(outputStream)
                            }
                        }
                        return ShareData(temp.getUri(), status.type.mimeType)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return ShareData.Empty
        }
        return ShareData(status.fileUri, status.type.mimeType)
    }

    override suspend fun share(statuses: List<Status>): ShareData {
        if (IsSAFRequired) {
            val data = hashMapOf<Uri, String>()
            val savedStatuses = statuses.filterIsInstance<SavedStatus>().toSet()
            val unsavedStatuses = statuses.subtract(savedStatuses)
            for (status in savedStatuses) {
                data[status.fileUri] = status.type.mimeType
            }
            if (unsavedStatuses.isEmpty()) {
                return ShareData(data.keys, data.values.toSet())
            }
            val cacheDir = context.externalCacheDir
            if (cacheDir != null && (cacheDir.exists() || cacheDir.mkdirs())) {
                val currentTime = Date().time
                for ((i, status) in unsavedStatuses.withIndex()) {
                    val temp = File(cacheDir, status.type.getDefaultSaveName(currentTime, i + 1))
                    if (!temp.exists() || temp.delete()) {
                        try {
                            val inputStream = contentResolver.openInputStream(status.fileUri)
                            if (inputStream != null) {
                                inputStream.use {
                                    temp.outputStream().use { outputStream ->
                                        it.copyTo(outputStream)
                                    }
                                }
                                data[temp.getUri()] = status.type.mimeType
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                return ShareData(data.keys, data.values.toSet())
            }
            return ShareData.Empty
        } else {
            val data = hashMapOf<Uri, String>()
            for (status in statuses) {
                data[status.fileUri] = status.type.mimeType
            }
            return ShareData(data.keys, data.values.toSet())
        }
    }

    override suspend fun save(status: Status, saveName: String?): Uri? {
        val savable = status.toStatusEntity(saveName)
        val result = saveAndGetUri(savable).apply {
            if (this != null) {
                scanSavedStatuses(status.type)
            }
        }
        return result
    }

    override suspend fun save(statuses: List<Status>): Map<Status, Uri> {
        if (statuses.isEmpty()) {
            return hashMapOf()
        }
        val savedStatuses = HashMap<Status, Uri>()
        val unsavedStatuses = statuses.filterNot { it.isSaved }
        val currentTimeMillis = System.currentTimeMillis()
        for ((i, status) in unsavedStatuses.withIndex()) {
            val savable = status.toStatusEntity(null, currentTimeMillis, i)
            val savedUri = saveAndGetUri(savable)
            if (savedUri != null) {
                savedStatuses[status] = savedUri
            }
        }
        val types = savedStatuses.keys.map { it.type }.toSet()
        for (type in types) {
            scanSavedStatuses(type)
        }
        return savedStatuses
    }

    override suspend fun delete(status: Status): Boolean {
        if (status is SavedStatus) {
            return execDeletion(status)
        }
        return false
    }

    override suspend fun delete(statuses: List<Status>): Int {
        val deletedMessages = statuses.filterIsInstance<SavedStatus>()
            .filter { execDeletion(it, false) }
        val contentUris = deletedMessages.map { it.type.contentUri }.distinct()
        for (uri in contentUris) {
            contentResolver.notifyChange(uri, null)
        }
        return deletedMessages.size
    }

    private fun execDeletion(status: SavedStatus, autoNotify: Boolean = true): Boolean {
        if (!context.hasStoragePermissions()) {
            return false
        }
        val success = when {
            IsScopedStorageRequired -> contentResolver.delete(status.fileUri, null, null) > 0
            status.hasFile() -> {
                val file = status.getFile()
                if (!file.exists() || file.delete()) {
                    contentResolver.delete(status.type.contentUri, "${MediaColumns.DATA}=?", arrayOf(status.getFilePath()))
                    true
                } else false
            }
            else -> false
        }
        if (success) {
            if (autoNotify) contentResolver.notifyChange(status.type.contentUri, null)
            statusDao.removeSave(status.name)
        }
        return success
    }

    private fun saveAndGetUri(status: StatusEntity): Uri? {
        val result = runCatching {
            contentResolver.openInputStream(status.origin)?.use { stream ->
                waSavedContentStorage.toSavedFileUri(status, stream).also { saveUri ->
                    if (saveUri != null) {
                        statusDao.saveStatus(status)
                    }
                }
            }
        }
        return result.getOrNull()
    }

    private fun scanSavedStatuses(statusType: StatusType) {
        if (!IsScopedStorageRequired) {
            waSavedContentStorage.getSaveDirectory(statusType).listFiles { _, name ->
                name.endsWith(statusType.format)
            }?.map { it.absolutePath }?.toTypedArray()?.let {
                if (it.isNotEmpty()) {
                    MediaScannerConnection.scanFile(context, it, arrayOf(statusType.mimeType), null)
                }
            }
        }
    }
}