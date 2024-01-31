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
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore.MediaColumns
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import com.simplified.wsstatussaver.database.*
import com.simplified.wsstatussaver.extensions.*
import com.simplified.wsstatussaver.model.*
import com.simplified.wsstatussaver.model.StatusQueryResult.ResultCode
import com.simplified.wsstatussaver.recordException
import com.simplified.wsstatussaver.storage.Storage
import java.io.*

interface StatusesRepository {
    suspend fun statuses(type: StatusType): StatusQueryResult
    suspend fun savedStatuses(type: StatusType): StatusQueryResult
    suspend fun savedStatusesObservable(type: StatusType): LiveData<List<StatusEntity>>
    suspend fun save(status: Status, saveName: String?): Uri?
    suspend fun save(statuses: List<Status>): Map<Status, Uri>
    suspend fun delete(status: Status): Boolean
    suspend fun delete(statuses: List<Status>): Int
}

class StatusesRepositoryImpl(
    private val context: Context,
    private val statusDao: StatusDao,
    private val storage: Storage
) : StatusesRepository {

    private val contentResolver: ContentResolver = context.contentResolver
    private val preferences = context.preferences()
    private val statusesLocationPath: String
        get() {
            val statusesLocation = storage.getStatusesLocation()
            return statusesLocation?.path ?: storage.externalStoragePath
        }

    override suspend fun statuses(type: StatusType): StatusQueryResult {
        val statusList = arrayListOf<Status>()
        val isExcludeOld = preferences.isExcludeOldStatuses()
        val isExcludeSaved = preferences.isExcludeSavedStatuses()
        val installedClients = context.getAllInstalledClients()
        if (installedClients.isEmpty()) {
            return StatusQueryResult(ResultCode.NotInstalled)
        }
        if (hasQ()) {
            val persistedPermissions = contentResolver.persistedUriPermissions
            if (persistedPermissions.isEmpty()) {
                return StatusQueryResult(code = ResultCode.PermissionError)
            }
            for (permission in persistedPermissions) {
                var statusesDir = DocumentFile.fromTreeUri(context, permission.uri)
                if (statusesDir?.name != ".Statuses") {
                    statusesDir = statusesDir?.findFile(".Statuses")
                }
                if (statusesDir == null || !statusesDir.isDirectory) {
                    return StatusQueryResult(code = ResultCode.NoStatuses)
                }
                val statusFiles = statusesDir.listFiles()
                if (statusFiles.isEmpty()) {
                    return StatusQueryResult(code = ResultCode.NoStatuses)
                }
                for (file in statusFiles) {
                    val fileName = file.name ?: continue
                    val client = WaClient.entries.firstOrNull {
                        statusesDir.uri.path?.contains(it.getSAFDirectoryPath()) == true
                    }
                    if (type.acceptFileName(fileName)) {
                        val isSaved = statusDao.statusSaved(file.uri, fileName)
                        if ((file.isOldFile() && isExcludeOld) || (isSaved && isExcludeSaved))
                            continue

                        statusList.add(Status(type, file.name, file.uri, file.lastModified(), file.length(), client?.packageName, isSaved))
                    }
                }
            }
        } else {
            if (context.hasStoragePermissions()) {
                for (client in installedClients.getPreferred(context)) {
                    val directory = File(statusesLocationPath, client.getDirectoryPath())
                    val statuses = directory.listFiles { _, name -> type.acceptFileName(name) }
                    if (!statuses.isNullOrEmpty()) for (file in statuses) {
                        val fileUri = file.toUri()
                        val isSaved = statusDao.statusSaved(fileUri, file.name)
                        if ((file.isOldFile() && isExcludeOld) || (isSaved && isExcludeSaved))
                            continue

                        statusList.add(Status(type, file.name, fileUri, file.lastModified(), file.length(), client.packageName, isSaved))
                    }
                }
            }
        }
        if (statusList.isNotEmpty()) {
            return StatusQueryResult(ResultCode.Success, statusList.sortedByDescending { it.dateModified })
        }
        return StatusQueryResult(ResultCode.NoStatuses)
    }

    override suspend fun savedStatuses(type: StatusType): StatusQueryResult {
        if (!context.hasStoragePermissions()) {
            return StatusQueryResult(ResultCode.PermissionError)
        }
        val statuses = arrayListOf<SavedStatus>()
        if (hasQ()) {
            val projection = arrayOf(
                MediaColumns._ID,
                MediaColumns.DISPLAY_NAME,
                MediaColumns.DATE_MODIFIED,
                MediaColumns.SIZE,
                MediaColumns.RELATIVE_PATH
            )
            val selection = "${MediaColumns.RELATIVE_PATH} LIKE ?"
            val arguments = arrayOf("%${type.relativePath}%")
            contentResolver.query(type.contentUri, projection, selection, arguments, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) do {
                    val mediaUri = ContentUris.withAppendedId(type.contentUri, cursor.getLong(0))
                    val name = cursor.getString(1)
                    val dateModified = cursor.getLong(2)
                    val size = cursor.getLong(3)
                    statuses.add(SavedStatus(type, name, mediaUri, dateModified, size, null))
                } while (cursor.moveToNext())
            }
        } else {
            val files = type.savesDirectory.listFiles { _, name -> type.acceptFileName(name) }
            if (files != null) for (file in files) {
                statuses.add(SavedStatus(type, file.name, file.toUri(), file.lastModified(), file.length(), file.absolutePath))
            }
        }
        if (statuses.isEmpty()) {
            return StatusQueryResult(ResultCode.NoStatuses)
        }
        return StatusQueryResult(ResultCode.Success, statuses.sortedByDescending { it.dateModified })
    }

    override suspend fun savedStatusesObservable(type: StatusType): LiveData<List<StatusEntity>> =
        statusDao.savedStatuses(type.ordinal)

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
        for ((i, status) in unsavedStatuses.withIndex()) {
            val savable = status.toStatusEntity(null, i)
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
            context.contentResolver.notifyChange(uri, null)
        }
        return deletedMessages.size
    }

    private fun execDeletion(status: SavedStatus, autoNotify: Boolean = true): Boolean {
        if (!context.hasStoragePermissions()) {
            return false
        }
        val success = when {
            hasQ() -> contentResolver.delete(status.fileUri, null, null) > 0
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
            if (!status.name.isNullOrEmpty()) statusDao.removeSave(status.name)
        }
        return success
    }

    private fun saveAndGetUri(status: StatusEntity): Uri? {
        return contentResolver.openInputStream(status.origin).use { stream ->
            if (stream != null) {
                val result = kotlin.runCatching {
                    saveStatus(status, stream).also { saveUri ->
                        if (saveUri != null) {
                            statusDao.saveStatus(status)
                        }
                    }
                }

                if (result.isSuccess) {
                    result.getOrThrow()
                } else {
                    result.exceptionOrNull()?.let { recordException(it) }
                    null
                }
            } else null
        }
    }

    @Throws(IOException::class)
    private fun saveStatus(status: StatusEntity, inputStream: InputStream): Uri? {
        if (hasQ()) {
            return saveQ(status, inputStream)
        }
        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            val destDirectory = status.type.savesDirectory
            if (destDirectory.isDirectory || destDirectory.mkdirs()) {
                val statusSaveFile = File(destDirectory, status.saveName)
                if (!statusSaveFile.exists() && statusSaveFile.createNewFile()) {
                    statusSaveFile.outputStream().use { os ->
                        inputStream.copyTo(os, SAVE_BUFFER_SIZE)
                    }
                    return statusSaveFile.getUri()
                }
            }
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveQ(status: StatusEntity, inputStream: InputStream): Uri? {
        val contentUri = status.type.contentUri

        val values = ContentValues().apply {
            put(MediaColumns.DISPLAY_NAME, status.saveName)
            put(MediaColumns.RELATIVE_PATH, status.type.relativePath)
            put(MediaColumns.MIME_TYPE, status.type.mimeType)
        }

        var uri: Uri? = null
        var stream: OutputStream? = null
        val resolver = context.contentResolver
        try {
            uri = resolver.insert(contentUri, values)
            if (uri != null) {
                stream = resolver.openOutputStream(uri)
                if (stream != null) {
                    inputStream.copyTo(stream, SAVE_BUFFER_SIZE)
                }
                resolver.notifyChange(contentUri, null)
            }
        } catch (e: IOException) {
            if (uri != null) {
                resolver.delete(uri, null, null)
            }
            throw e
        } finally {
            stream?.close()
        }
        return uri
    }

    private fun scanSavedStatuses(statusType: StatusType) {
        if (!hasQ()) {
            val files = statusType.savesDirectory.listFiles { _, name -> name.endsWith(statusType.format) }
                ?.map { it.absolutePath }
                ?.toTypedArray()
            if (!files.isNullOrEmpty()) {
                MediaScannerConnection.scanFile(context, files, arrayOf(statusType.mimeType), null)
            }
        }
    }

    companion object {
        private const val SAVE_BUFFER_SIZE = 2048
    }
}