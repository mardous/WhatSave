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

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import com.simplified.wsstatussaver.database.*
import com.simplified.wsstatussaver.extensions.*
import com.simplified.wsstatussaver.mediator.WAClient
import com.simplified.wsstatussaver.mediator.WAMediator
import com.simplified.wsstatussaver.model.Status
import com.simplified.wsstatussaver.model.StatusType
import com.simplified.wsstatussaver.recordException
import com.simplified.wsstatussaver.storage.Storage
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream

interface StatusesRepository {
    suspend fun statuses(type: StatusType): List<Status>
    suspend fun savedStatuses(type: StatusType): List<Status>
    suspend fun savedStatusesObservable(type: StatusType): LiveData<List<StatusEntity>>
    suspend fun save(status: Status, saveName: String?): Uri?
    suspend fun save(statuses: List<Status>): Map<Status, Uri>
    suspend fun delete(status: Status): Boolean
    suspend fun delete(statuses: List<Status>): Int
}

class StatusesRepositoryImpl(
    private val context: Context,
    private val statusDao: StatusDao,
    private val storage: Storage,
    private val mediator: WAMediator
) : StatusesRepository {

    private val preferences = context.preferences()
    private val statusesLocationPath: String
        get() {
            val statusesLocation = storage.getStatusesLocation()
            return statusesLocation?.path ?: storage.externalStoragePath
        }

    override suspend fun statuses(type: StatusType): List<Status> {
        val statuses = getStatusesFromClient(
            type,
            mediator.getClientsForLoader(),
            preferences.isExcludeOldStatuses(),
            preferences.isExcludeSavedStatuses()
        )
        val doIHavePermissions = context.doIHavePermissions(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (statuses.isEmpty() && doIHavePermissions) {
            statusDao.removeSaves(type.ordinal)
        }
        return statuses
    }

    override suspend fun savedStatuses(type: StatusType): List<Status> =
        getStatusesFromClient(
            type,
            mediator.getSavedStatusesClients(type),
            isExcludeOld = false,
            isExcludeSaved = false,
            isRelativeLocation = false
        )

    override suspend fun savedStatusesObservable(type: StatusType): LiveData<List<StatusEntity>> =
        statusDao.savedStatuses(type.ordinal)

    override suspend fun save(status: Status, saveName: String?): Uri? {
        if (status.path.isEmpty()) {
            return null
        }
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
        val mediaStoreUri = status.getMediaStoreUri(context)
        return if (mediaStoreUri != null) { // it is a saved status
            context.contentResolver.run {
                delete(mediaStoreUri, null, null)
                notifyChange(status.type.contentUri, null)
            }
            val file = File(status.path)
            if (file.exists()) {
                context.tryDeletePath(file)
            } else {
                true
            }
        } else {
            // cannot delete unsaved statuses
            false
        }
    }

    override suspend fun delete(statuses: List<Status>): Int {
        var deleted = 0

        val notifyUris = hashSetOf<Uri>()
        for (status in statuses) {
            val where = status.getMediaStoreWhere()
            notifyUris.add(status.type.contentUri)

            if (context.contentResolver.delete(status.type.contentUri, where.first, where.second) > 0) {
                val file = File(status.path)
                if (file.exists()) {
                    if (context.tryDeletePath(file)) deleted++
                } else {
                    deleted++
                }
            }
        }
        for (uri in notifyUris) {
            context.contentResolver.notifyChange(uri, null)
        }

        return deleted
    }

    private fun getStatusesFromClient(
        statusType: StatusType,
        clients: List<WAClient>,
        isExcludeOld: Boolean,
        isExcludeSaved: Boolean,
        isRelativeLocation: Boolean = true,
    ): List<Status> {
        if (clients.isNotEmpty()) {
            val statuses = ArrayList<Status>()
            for (client in clients) {
                val directories = client.statusesDirectories
                if (directories.isNullOrEmpty()) continue

                try {
                    for (directory in directories) {
                        val statusesDir = if (isRelativeLocation) {
                            File(statusesLocationPath, directory)
                        } else {
                            File(directory)
                        }
                        if (statusesDir.isDirectory) {
                            val files = statusesDir.listFiles { file ->
                                statusType.acceptFileName(file.name) && (!isExcludeOld || !file.isOlderThan(24))
                            }
                            if (files != null) for (file in files) {
                                val isSaved = statusDao.statusSaved(file.absolutePath, file.name)
                                if (isSaved && isExcludeSaved) continue
                                statuses.add(
                                    Status(
                                        statusType,
                                        file.name,
                                        file.absolutePath,
                                        file.lastModified(),
                                        file.length(),
                                        client.packageName,
                                        isSaved
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Throwable) {
                    recordException(e)
                }
            }
            return statuses.also { list ->
                list.sortByDescending { status -> status.dateModified }
            }
        }
        return emptyList()
    }

    private fun saveAndGetUri(status: StatusEntity): Uri? {
        return status.openInputStream().use { stream ->
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
        }
    }

    @Throws(IOException::class)
    private fun saveStatus(status: StatusEntity, inputStream: FileInputStream): Uri? {
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
    private fun saveQ(status: StatusEntity, inputStream: FileInputStream): Uri? {
        val contentUri = status.type.contentUri

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, status.saveName)
            put(MediaStore.MediaColumns.RELATIVE_PATH, status.type.relativePath)
            put(MediaStore.MediaColumns.MIME_TYPE, status.type.mimeType)
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
            val files = statusType.savesDirectory.list { _, name -> name.endsWith(statusType.format) }
            if (!files.isNullOrEmpty()) {
                MediaScannerConnection.scanFile(context, files, null, null)
            }
        }
    }

    companion object {
        private const val SAVE_BUFFER_SIZE = 2048
    }
}