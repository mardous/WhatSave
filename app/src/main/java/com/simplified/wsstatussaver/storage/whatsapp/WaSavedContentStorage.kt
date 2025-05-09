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
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.DocumentsContract.createDocument
import android.provider.DocumentsContract.getTreeDocumentId
import android.provider.MediaStore.MediaColumns
import androidx.annotation.RequiresApi
import androidx.core.content.contentValuesOf
import androidx.core.content.edit
import androidx.core.net.toUri
import com.simplified.wsstatussaver.database.StatusEntity
import com.simplified.wsstatussaver.extensions.IsScopedStorageRequired
import com.simplified.wsstatussaver.extensions.getUri
import com.simplified.wsstatussaver.extensions.hasFullAccess
import com.simplified.wsstatussaver.extensions.preferences
import com.simplified.wsstatussaver.extensions.saveLocation
import com.simplified.wsstatussaver.model.SaveLocation
import com.simplified.wsstatussaver.model.StatusType
import com.simplified.wsstatussaver.model.WaDirectory
import com.simplified.wsstatussaver.model.WaDirectoryUri
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

class WaSavedContentStorage(context: Context, private val contentResolver: ContentResolver) {

    private val preferences = context.preferences()
    private val currentSaveLocation: SaveLocation
        get() = preferences.saveLocation

    fun getCustomDirectoryName(type: StatusType): String {
        val saveDirectory = getCustomSaveDirectory(type, SaveLocation.Custom)
            ?: return type.saveType.dirName

        val nameSelection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        return contentResolver.query(saveDirectory.documentUri, nameSelection, null, null, null).use { cursor ->
            cursor?.takeIf { it.moveToFirst() }
                ?.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME))
                ?: type.saveType.dirName
        }
    }

    fun setCustomDirectory(type: StatusType, selectedUri: Uri?): Boolean {
        if (selectedUri == null) {
            preferences.edit { remove(type.saveType.customDirectoryId) }
        } else {
            if (DocumentsContract.isTreeUri(selectedUri)) {
                if (WaDirectory.entries.any { it.isThis(selectedUri) }) {
                    return false
                }
                contentResolver.takePersistableUriPermission(
                    selectedUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                preferences.edit {
                    putString(type.saveType.customDirectoryId, selectedUri.toString())
                }
                return true
            }
        }
        return false
    }

    fun getCustomSaveDirectory(type: StatusType, saveLocation: SaveLocation = currentSaveLocation): WaDirectoryUri? {
        if (saveLocation == SaveLocation.Custom) {
            val savedValue = preferences.getString(type.saveType.customDirectoryId, null)
            if (!savedValue.isNullOrBlank()) {
                val treeUri = savedValue.toUri()
                val hasPermission = contentResolver.persistedUriPermissions.any {
                    it.hasFullAccess(treeUri)
                }
                if (DocumentsContract.isTreeUri(treeUri) && hasPermission) {
                    return WaDirectoryUri(null, treeUri, getTreeDocumentId(treeUri))
                } else {
                    setCustomDirectory(type, null)
                }
            }
        }
        return null
    }

    fun getSaveDirectory(type: StatusType, location: SaveLocation = currentSaveLocation): File {
        val publicPath = Environment.getExternalStoragePublicDirectory(
            type.saveType.dirTypeProvider(location)
        )
        return File(publicPath, type.saveType.dirName)
    }

    fun getSaveDirectories(type: StatusType): Set<File> {
        val directories = SaveLocation.getWithoutCustom()
            .mapTo(mutableSetOf()) { getSaveDirectory(type, it) }
        return directories
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getRelativePath(type: StatusType, location: SaveLocation = currentSaveLocation): String =
        String.format("%s/%s", type.saveType.dirTypeProvider(location), type.saveType.dirName)

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getSavedMedia(statusType: StatusType): Cursor? {
        val projection = arrayOf(
            MediaColumns._ID,
            MediaColumns.DISPLAY_NAME,
            MediaColumns.DATE_MODIFIED,
            MediaColumns.SIZE,
            MediaColumns.RELATIVE_PATH
        )
        val entries = SaveLocation.getWithoutCustom()
        val selection = entries.joinToString(" OR ") { "${MediaColumns.RELATIVE_PATH} LIKE ?" }
        val arguments = entries.map { "%${getRelativePath(statusType, it)}%" }.toTypedArray()
        return contentResolver.query(statusType.contentUri, projection, selection, arguments, null)
    }

    fun toSavedFileUri(status: StatusEntity, inputStream: InputStream): Uri? {
        val customSaveDirectory = getCustomSaveDirectory(status.type)
        if (customSaveDirectory != null) {
            return toCustomDirectory(status, inputStream, customSaveDirectory)
        }
        if (IsScopedStorageRequired) {
            return toMediaStore(status, inputStream)
        }
        return toFileLocation(status, inputStream)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun toMediaStore(
        status: StatusEntity,
        inputStream: InputStream
    ): Uri? {
        val contentUri = status.type.contentUri
        val values = contentValuesOf(
            MediaColumns.DISPLAY_NAME to status.saveName,
            MediaColumns.RELATIVE_PATH to getRelativePath(status.type),
            MediaColumns.MIME_TYPE to status.type.mimeType
        )
        var uri: Uri? = null
        try {
            uri = contentResolver.insert(contentUri, values)
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    inputStream.copyTo(outputStream, SAVE_BUFFER_SIZE)
                }
                contentResolver.notifyChange(contentUri, null)
            }
        } catch (e: IOException) {
            if (uri != null) {
                contentResolver.delete(uri, null, null)
            }
            throw e
        }
        return uri
    }

    private fun toFileLocation(status: StatusEntity, inputStream: InputStream): Uri? {
        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            val destDirectory = getSaveDirectory(status.type)
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

    private fun toCustomDirectory(
        status: StatusEntity,
        inputStream: InputStream,
        directory: WaDirectoryUri
    ): Uri? {
        val documentUri = try {
            createDocument(contentResolver, directory.documentUri, status.type.mimeType, status.saveName)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            null
        }
        if (documentUri != null) {
            try {
                contentResolver.openOutputStream(documentUri)?.use { outputStream ->
                    inputStream.copyTo(outputStream, SAVE_BUFFER_SIZE)
                }
            } catch (e: IOException) {
                DocumentsContract.deleteDocument(contentResolver, documentUri)
                throw e
            }
        }
        return documentUri
    }

    companion object {
        const val SAVE_BUFFER_SIZE = 2048
    }
}