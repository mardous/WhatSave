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
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.MediaStore.MediaColumns
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import com.simplified.wsstatussaver.database.StatusDao
import com.simplified.wsstatussaver.database.StatusEntity
import com.simplified.wsstatussaver.database.toStatusEntity
import com.simplified.wsstatussaver.extensions.IsSAFRequired
import com.simplified.wsstatussaver.extensions.IsScopedStorageRequired
import com.simplified.wsstatussaver.extensions.acceptFileName
import com.simplified.wsstatussaver.extensions.getAllInstalledClients
import com.simplified.wsstatussaver.extensions.getPreferred
import com.simplified.wsstatussaver.extensions.getReadableDirectories
import com.simplified.wsstatussaver.extensions.getStatusType
import com.simplified.wsstatussaver.extensions.getUri
import com.simplified.wsstatussaver.extensions.hasElapsedTwentyFourHours
import com.simplified.wsstatussaver.extensions.hasStoragePermissions
import com.simplified.wsstatussaver.extensions.isExcludeSavedStatuses
import com.simplified.wsstatussaver.extensions.isOldFile
import com.simplified.wsstatussaver.extensions.preferences
import com.simplified.wsstatussaver.extensions.saveLocation
import com.simplified.wsstatussaver.model.SaveLocation
import com.simplified.wsstatussaver.model.SavedStatus
import com.simplified.wsstatussaver.model.ShareData
import com.simplified.wsstatussaver.model.Status
import com.simplified.wsstatussaver.model.StatusQueryResult
import com.simplified.wsstatussaver.model.StatusQueryResult.ResultCode
import com.simplified.wsstatussaver.model.StatusType
import com.simplified.wsstatussaver.model.WaClient
import com.simplified.wsstatussaver.model.WaDirectory
import com.simplified.wsstatussaver.model.WaDirectoryUri
import com.simplified.wsstatussaver.storage.Storage
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

interface StatusesRepository {
    suspend fun statusDirectories(clients: List<WaClient>): Set<WaDirectoryUri>
    suspend fun statusDirectoriesAsFiles(client: WaClient): Set<File>
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
    private val storage: Storage
) : StatusesRepository {

    private val contentResolver: ContentResolver = context.contentResolver
    private val preferences = context.preferences()
    private val statusSaveLocation: SaveLocation
        get() = preferences.saveLocation
    private val statusesLocationPath: String
        get() {
            val statusesLocation = storage.getStatusesLocation()
            return statusesLocation?.path ?: storage.externalStoragePath
        }

    override suspend fun statusDirectories(clients: List<WaClient>): Set<WaDirectoryUri> {
        val directories = mutableSetOf<WaDirectoryUri>()
        val persistedPermissions = contentResolver.persistedUriPermissions
        val readableDirectories = persistedPermissions.getReadableDirectories()
        if (readableDirectories.isEmpty()) {
            return directories
        }
        for (perm in persistedPermissions) {
            if (!DocumentsContract.isTreeUri(perm.uri)) continue
            val matchingDir = readableDirectories.firstOrNull { it.isThis(perm.uri) }
            if (matchingDir != null) {
                directories.addAll(matchingDir.getStatusesDirectories(context, clients, perm.uri))
            }
        }
        return directories
    }

    override suspend fun statusDirectoriesAsFiles(client: WaClient): Set<File> {
        val directories = mutableSetOf<File>()
        val paths = WaDirectory.entries.filterNot { it.isLegacy }.mapNotNull { dir ->
            if (dir.supportsClient(client)) {
                val additionalSegments = dir.additionalSegments(client)
                if (additionalSegments.isNotEmpty())
                    "${dir.path}/${additionalSegments.joinToString("/")}"
                else dir.path
            } else {
                null
            }
        }
        for (path in paths) {
            File(statusesLocationPath, "${path}/accounts")
                .takeIf { it.isDirectory }?.let { baseDirectory ->
                    baseDirectory.list { file, _ -> file.isDirectory }?.forEach { accountName ->
                        directories.add(File(baseDirectory, "$accountName/Media/.Statuses"))
                    }
                }

            directories.add(File(statusesLocationPath, "${path}/Media/.Statuses"))
        }
        return directories
    }

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
            val statusesDirectories = statusDirectories(installedClients)
            if (statusesDirectories.isEmpty()) {
                return StatusQueryResult(ResultCode.PermissionError)
            }
            val documentSelection = arrayOf(
                Document.COLUMN_DOCUMENT_ID, //0
                Document.COLUMN_DISPLAY_NAME, //1
                Document.COLUMN_LAST_MODIFIED, //2
                Document.COLUMN_SIZE //3
            )
            for (directory in statusesDirectories) {
                contentResolver.query(directory.childDocumentsUri, documentSelection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) do {
                        val id = cursor.getString(0)
                        val fileName = cursor.getString(1)
                        val lastModified = cursor.getLong(2)
                        val size = cursor.getLong(3)
                        val uri = DocumentsContract.buildDocumentUriUsingTree(directory.treeUri, id)
                        if (type.acceptFileName(fileName)) {
                            val isOld = lastModified.hasElapsedTwentyFourHours()
                            val isSaved = statusDao.statusSaved(uri, fileName)
                            if (isOld || (isSaved && isExcludeSaved))
                                continue

                            statusList.add(Status(type, fileName, uri, lastModified, size, directory.client?.packageName, isSaved))
                        }
                    } while (cursor.moveToNext())
                }
            }
        } else {
            if (context.hasStoragePermissions()) {
                for (client in installedClients) {
                    for (directory in statusDirectoriesAsFiles(client)) {
                        if (!directory.isDirectory) continue
                        val statuses = directory.listFiles { _, name -> type.acceptFileName(name) }
                        if (!statuses.isNullOrEmpty()) for (file in statuses) {
                            val fileUri = file.getUri()
                            val fileName = file.name
                            val isSaved = statusDao.statusSaved(fileUri, file.name)
                            if (fileName.isNullOrEmpty() || file.isOldFile() || (isSaved && isExcludeSaved))
                                continue

                            statusList.add(Status(type, fileName, fileUri, file.lastModified(), file.length(), client.packageName, isSaved))
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
        if (IsScopedStorageRequired) {
            for (type in StatusType.entries) {
                type.getSavedMedia(contentResolver).use { cursor ->
                    if (cursor != null && cursor.moveToFirst()) do {
                        statuses.add(cursor.getSavedStatus(type))
                    } while (cursor.moveToNext())
                }
            }
        } else {
            val files = StatusType.entries.flatMap { type ->
                SaveLocation.entries.flatMap { location ->
                    type.getSavedContentFiles(location).toList()
                }
            }
            if (files.isNotEmpty()) for (file in files) {
                val type = file.getStatusType() ?: continue
                statuses.add(
                    SavedStatus(type, file.name, file.getUri(), file.lastModified(), file.length(), file.absolutePath)
                )
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
        if (IsScopedStorageRequired) {
            type.getSavedMedia(contentResolver).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) do {
                    statuses.add(cursor.getSavedStatus(type))
                } while (cursor.moveToNext())
            }
        } else {
            val files = SaveLocation.entries.flatMap {
                type.getSavedContentFiles(it).toList()
            }
            if (files.isNotEmpty()) for (file in files) {
                statuses.add(SavedStatus(type, file.name, file.getUri(), file.lastModified(), file.length(), file.absolutePath))
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
            IsScopedStorageRequired -> {
                try {
                    contentResolver.delete(status.fileUri, null, null) > 0
                } catch (e: SecurityException) {
                    false
                }
            }
            status.hasFile() -> {
                try {
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
                } catch (e: SecurityException) {
                    false
                }
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
                saveStatus(status, stream).also { saveUri ->
                    if (saveUri != null) {
                        statusDao.saveStatus(status)
                    }
                }
            }
        }
        return result.getOrNull()
    }

    @Throws(IOException::class)
    private fun saveStatus(status: StatusEntity, inputStream: InputStream): Uri? {
        if (IsScopedStorageRequired) {
            return saveQ(status, inputStream)
        }
        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            val destDirectory = status.type.getSavesDirectory(statusSaveLocation)
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
            put(MediaColumns.RELATIVE_PATH, status.type.getRelativePath(statusSaveLocation))
            put(MediaColumns.MIME_TYPE, status.type.mimeType)
        }

        var uri: Uri? = null
        var stream: OutputStream? = null
        val resolver = contentResolver
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
        if (!IsScopedStorageRequired) {
            val files = statusType.getSavesDirectory(statusSaveLocation).listFiles { _, name -> name.endsWith(statusType.format) }
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