/*
 * Copyright (C) 2025 Christians Martínez Alvarado
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
/*
 * Copyright (C) 2025 Christians Martínez Alvarado
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
package com.simplified.wsstatussaver.storage.whatsapp

import android.content.ContentResolver
import android.content.Context
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import androidx.core.net.toUri
import com.simplified.wsstatussaver.extensions.acceptFileName
import com.simplified.wsstatussaver.extensions.getReadableDirectories
import com.simplified.wsstatussaver.model.StatusType
import com.simplified.wsstatussaver.model.WaClient
import com.simplified.wsstatussaver.model.WaDirectory
import com.simplified.wsstatussaver.model.WaDirectoryUri
import com.simplified.wsstatussaver.model.WaFile
import com.simplified.wsstatussaver.storage.Storage
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class WaContentStorage(
    private val context: Context,
    private val contentResolver: ContentResolver
) : KoinComponent {

    private val storage: Storage by inject()
    private val statusesLocationPath: String
        get() {
            val statusesLocation = storage.getStatusesLocation()
            return statusesLocation?.path ?: storage.externalStoragePath
        }

    fun statusDirectories(clients: List<WaClient>): Set<WaDirectoryUri> {
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

    fun statusDirectoriesAsFiles(client: WaClient): Set<File> {
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

    fun resolveFiles(
        directories: Set<WaDirectoryUri>,
        fileConsumer: (WaFile) -> Unit
    ) {
        val documentSelection = arrayOf(
            Document.COLUMN_DOCUMENT_ID, //0
            Document.COLUMN_DISPLAY_NAME, //1
            Document.COLUMN_LAST_MODIFIED, //2
            Document.COLUMN_SIZE //3
        )
        for (directory in directories) {
            contentResolver.query(directory.getChildrenUri(), documentSelection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) do {
                    val id = cursor.getString(0)
                    val fileName = cursor.getString(1)
                    val lastModified = cursor.getLong(2)
                    val size = cursor.getLong(3)
                    val uri = DocumentsContract.buildChildDocumentsUriUsingTree(
                        directory.getChildrenUri(), id
                    )
                    fileConsumer(WaFile(id, directory.client, null, fileName, lastModified, size, uri))
                } while (cursor.moveToNext())
            }
        }
    }

    fun resolveFiles(directories: Set<File>, type: StatusType, client: WaClient?, fileConsumer: (WaFile) -> Unit) {
        for (directory in directories) {
            if (!directory.isDirectory) continue
            val files = directory.listFiles { _, name -> type.acceptFileName(name) }
            if (!files.isNullOrEmpty()) for (file in files) {
                fileConsumer(WaFile(null, client, file.absolutePath, file.name, file.lastModified(), file.length(), file.toUri()))
            }
        }
    }
}