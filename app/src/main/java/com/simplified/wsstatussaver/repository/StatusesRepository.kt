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
import com.simplified.wsstatussaver.extensions.hasStoragePermissions
import com.simplified.wsstatussaver.extensions.isExcludeSavedStatuses
import com.simplified.wsstatussaver.extensions.preferences
import com.simplified.wsstatussaver.model.SavedStatus
import com.simplified.wsstatussaver.model.ShareData
import com.simplified.wsstatussaver.model.Status
import com.simplified.wsstatussaver.model.StatusQueryResult
import com.simplified.wsstatussaver.model.StatusQueryResult.ResultCode
import com.simplified.wsstatussaver.model.StatusType
import com.simplified.wsstatussaver.storage.whatsapp.WaContentStorage
import com.simplified.wsstatussaver.storage.whatsapp.WaSavedContentStorage
import java.util.concurrent.TimeUnit
import kotlin.collections.filter

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
    suspend fun save(status: Status, saveName: String?): SavedStatus?
    suspend fun save(statuses: List<Status>): List<SavedStatus>
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
        return ShareData(status.fileUri, status.type.mimeType)
    }

    override suspend fun share(statuses: List<Status>): ShareData {
        val data = hashMapOf<Uri, String>()
        for (status in statuses) {
            data[status.fileUri] = status.type.mimeType
        }
        return if (data.isEmpty()) {
            ShareData.Empty
        } else {
            ShareData(data.keys, data.values.toSet())
        }
    }

    override suspend fun save(status: Status, saveName: String?): SavedStatus? {
        
        //Timestamp issue fix!!
        val currentTimeMillis = System.currentTimeMillis()
        val savable = status.toStatusEntity(saveName, currentTimeMillis)

        val result = createSavedStatus(savable, true)?.also { status ->
            scanSavedStatus(listOf(status))
        }
        return result
    }

    override suspend fun save(statuses: List<Status>): List<SavedStatus> {
        if (statuses.isEmpty()) {
            return emptyList()
        }
        val savedStatuses = mutableListOf<SavedStatus>()
        val unsavedStatuses = statuses.filterNot { it.isSaved }
        val currentTimeMillis = System.currentTimeMillis()
        for ((i, status) in unsavedStatuses.withIndex()) {
            val savable = status.toStatusEntity(null, currentTimeMillis, i)
            val savedStatus = createSavedStatus(savable, false)
            if (savedStatus != null) {
                savedStatuses.add(savedStatus)
            }
        }
        if (savedStatuses.isNotEmpty()) {
            savedStatuses.distinctBy { it.type.mimeType }
                .forEach {
                    contentResolver.notifyChange(it.type.contentUri, null)
                }
            scanSavedStatus(savedStatuses)
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

    private fun execDeletion(status: SavedStatus, notify: Boolean = true): Boolean {
        if (!context.hasStoragePermissions()) {
            return false
        }
        val success = when {
            IsScopedStorageRequired -> {
                runCatching {contentResolver.delete(status.fileUri, null, null) > 0 }
                    .getOrDefault(false)
            }
            status.hasFile() -> {
                runCatching {
                    val file = status.getFile()
                    if (!file.exists() || file.delete()) {
                        contentResolver.delete(
                            status.type.contentUri,
                            "${MediaColumns.DATA}=?",
                            arrayOf(status.getFilePath())
                        )
                        true
                    } else {
                        false
                    }
                }.getOrDefault(false)
            }
            else -> false
        }
        if (success) {
            if (notify) contentResolver.notifyChange(status.type.contentUri, null)
            statusDao.removeSave(status.name)
        }
        return success
    }

    private fun createSavedStatus(status: StatusEntity, notify: Boolean): SavedStatus? {
        val result = runCatching {
            contentResolver.openInputStream(status.origin)?.use { stream ->
                waSavedContentStorage.toSavedStatus(status, stream, notify).also { savedStatus ->
                    if (savedStatus != null) {
                        statusDao.saveStatus(status)
                    }
                }
            }
        }
        return result.getOrNull()
    }

    private fun scanSavedStatus(statuses: List<SavedStatus>) {
        if (!IsScopedStorageRequired) {
            val files = statuses.filter { status -> status.hasFile() }
                .map { status -> status.getFilePath() }
                .toTypedArray()
            if (files.isNotEmpty()) {
                MediaScannerConnection.scanFile(context, files, null, null)
            }
        }
    }
}