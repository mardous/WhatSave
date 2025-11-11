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
import android.provider.DocumentsContract.getTreeDocumentId
import android.provider.MediaStore.MediaColumns
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.contentValuesOf
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.simplified.wsstatussaver.database.StatusEntity
import com.simplified.wsstatussaver.database.toSavedStatus
import com.simplified.wsstatussaver.extensions.IsScopedStorageRequired
import com.simplified.wsstatussaver.extensions.allPermissionsGranted
import com.simplified.wsstatussaver.extensions.canonicalOrAbsolutePath
import com.simplified.wsstatussaver.extensions.getUri
import com.simplified.wsstatussaver.extensions.isCustomSaveDirectory
import com.simplified.wsstatussaver.extensions.isTreeUri
import com.simplified.wsstatussaver.extensions.isWhatsAppDirectory
import com.simplified.wsstatussaver.extensions.preferences
import com.simplified.wsstatussaver.extensions.saveLocation
import com.simplified.wsstatussaver.extensions.takePermissions
import com.simplified.wsstatussaver.model.SaveLocation
import com.simplified.wsstatussaver.model.SavedStatus
import com.simplified.wsstatussaver.model.StatusType
import com.simplified.wsstatussaver.model.WaDirectoryUri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class WaSavedContentStorage(private val context: Context, private val contentResolver: ContentResolver) {

    private val preferences = context.preferences()
    private val currentSaveLocation: SaveLocation
        get() = preferences.saveLocation

    fun getCustomDirectoryName(type: StatusType): String {
        return getCustomSaveDirectory(type, SaveLocation.Custom)?.let {
            DocumentFile.fromTreeUri(context, it.treeUri)?.name
        } ?: type.saveType.dirName
    }

    fun setCustomDirectory(type: StatusType, selectedUri: Uri): Boolean {
        if (!selectedUri.isTreeUri() || selectedUri.isWhatsAppDirectory()) return false

        val key = type.saveType.customDirectoryId
        val currentValue = preferences.getString(key, null)

        val currentUri = currentValue?.toUri()?.takeIf { it.isTreeUri() }
        if (currentUri != null && selectedUri == currentUri) {
            if (!contentResolver.allPermissionsGranted(selectedUri)) {
                return contentResolver.takePermissions(selectedUri, ACCESS_FLAGS)
            }
            return false
        }

        currentUri?.let {
            contentResolver.releasePersistableUriPermission(it, ACCESS_FLAGS)
        } ?: preferences.edit { remove(key) }

        if (contentResolver.allPermissionsGranted(selectedUri) || contentResolver.takePermissions(selectedUri, ACCESS_FLAGS)) {
            preferences.edit { putString(key, selectedUri.toString()) }
            return true
        }

        return false
    }

    fun getCustomSaveDirectory(type: StatusType, saveLocation: SaveLocation = currentSaveLocation): WaDirectoryUri? {
        if (saveLocation == SaveLocation.Custom) {
            val savedValue = preferences.getString(type.saveType.customDirectoryId, null)
            if (!savedValue.isNullOrBlank()) {
                val treeUri = savedValue.toUri()
                if (treeUri.isCustomSaveDirectory(contentResolver)) {
                    return WaDirectoryUri(null, treeUri, getTreeDocumentId(treeUri))
                } else {
                    preferences.edit { remove(type.saveType.customDirectoryId) }
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

    fun toSavedStatus(status: StatusEntity, inputStream: InputStream, notify: Boolean): SavedStatus? {
        val customSaveDirectory = getCustomSaveDirectory(status.type)
        if (customSaveDirectory != null) {
            return toCustomDirectory(status, inputStream, customSaveDirectory)
        }
        if (IsScopedStorageRequired) {
            return toMediaStore(status, inputStream, notify)
        }
        return toFileLocation(status, inputStream)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun toMediaStore(
        status: StatusEntity,
        inputStream: InputStream,
        notify: Boolean
    ): SavedStatus? {
        val contentUri = status.type.contentUri
        val contentValues = contentValuesOf(
            MediaColumns.DISPLAY_NAME to status.saveName,
            MediaColumns.RELATIVE_PATH to getRelativePath(status.type),
            MediaColumns.MIME_TYPE to status.type.mimeType
        )
        var uri: Uri? = null
        return try {
            with(contentResolver) {
                uri = insert(contentUri, contentValues)
                if (uri != null) {
                    openOutputStream(uri)?.use { outputStream ->
                        inputStream.copyTo(outputStream,
                            SAVE_BUFFER_SIZE
                        )
                    }
                    if (notify) {
                        notifyChange(contentUri, null)
                    }
                }
                uri?.let { status.toSavedStatus(it, null) }
            }
        } catch (e: IOException) {
            Log.e("StatusRepository", "Couldn't write content at $uri", e)
            if (uri != null) {
                contentResolver.delete(uri, null, null)
            }
            null
        }
    }

    private fun toFileLocation(status: StatusEntity, inputStream: InputStream): SavedStatus? {
        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            val destDirectory = getSaveDirectory(status.type)
            if (destDirectory.isDirectory || destDirectory.mkdirs()) {
                val statusSaveFile = File(destDirectory, status.saveName)
                if (!statusSaveFile.exists() && statusSaveFile.createNewFile()) {
                    statusSaveFile.outputStream().use { os ->
                        inputStream.copyTo(os, SAVE_BUFFER_SIZE)
                    }
                    return status.toSavedStatus(
                        uri = statusSaveFile.getUri(),
                        path = statusSaveFile.canonicalOrAbsolutePath()
                    )
                }
            }
        }
        return null
    }

    private fun toCustomDirectory(
        status: StatusEntity,
        inputStream: InputStream,
        directory: WaDirectoryUri
    ): SavedStatus? {
        val directory = DocumentFile.fromTreeUri(context, directory.treeUri) ?: return null
        val newFile = directory.createFile(status.type.mimeType, status.saveName) ?: return null

        return try {
            contentResolver.openFileDescriptor(newFile.uri, "w")?.use { pfd ->
                FileOutputStream(pfd.fileDescriptor).use {
                    inputStream.copyTo(it)
                }
            } ?: throw IOException("The descriptor could not be opened for writing!")

            status.toSavedStatus(newFile.uri, null)
        } catch (e: IOException) {
            if (newFile.exists()) {
                newFile.delete()
            }
            throw e
        }
    }

    companion object {
        const val ACCESS_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        const val SAVE_BUFFER_SIZE = 2048
    }
}