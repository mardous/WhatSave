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
package com.simplified.wsstatussaver.model

import android.content.Context
import android.content.Intent
import android.content.UriPermission
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.DocumentsContract.buildDocumentUriUsingTree
import android.provider.DocumentsContract.getTreeDocumentId
import androidx.documentfile.provider.DocumentFile
import com.simplified.wsstatussaver.extensions.decodedUrl
import com.simplified.wsstatussaver.storage.Storage

typealias SegmentResolver = (WaClient) -> List<String>

data class WaDirectoryUri(val client: WaClient?, val treeUri: Uri, private val documentId: String) {
    fun getDocumentUri(targetId: String = getTreeDocumentId(treeUri)): Uri =
        buildDocumentUriUsingTree(treeUri, targetId)

    val childDocumentsUri: Uri
        get() = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
}

enum class WaDirectory(
    val path: String,
    val additionalSegments: SegmentResolver = { emptyList() },
    private val supportedClients: Array<WaClient>,
    val isLegacy: Boolean = false
) {
    Media(
        path = "Android/media",
        additionalSegments = { listOf(it.packageName, it.displayName) },
        supportedClients = arrayOf(WaClient.WhatsApp, WaClient.Business)
    ),
    WhatsApp(
        path = "WhatsApp",
        supportedClients = arrayOf(WaClient.WhatsApp)
    ),
    WhatsAppBusiness(
        path = "WhatsApp Business",
        supportedClients = arrayOf(WaClient.Business)
    ),
    Legacy(
        path = "Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
        supportedClients = arrayOf(WaClient.WhatsApp),
        isLegacy = true
    ),
    LegacyBusiness(
        path = "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses",
        supportedClients = arrayOf(WaClient.Business),
        isLegacy = true
    );

    fun createPrettyPath(st: Storage): String {
        val primaryDevice = st.primaryStorageDevice
        if (primaryDevice != null) {
            return "${primaryDevice.name}/$path"
        }
        return "${Build.MODEL}/$path"
    }

    fun supportsClient(client: WaClient) = supportedClients.contains(client)

    fun isReadable(context: Context): Boolean {
        return isReadable(context.contentResolver.persistedUriPermissions)
    }

    fun isReadable(permissions: List<UriPermission>): Boolean {
        return permissions.any { it.isReadPermission && isThis(it.uri) }
    }

    fun isThis(uri: Uri): Boolean {
        val path = uri.encodedPath?.decodedUrl() ?: return false
        if (path.contains(":")) {
            val lastPart = path.split(":")
            if (lastPart.size == 2) {
                return lastPart[1] == this.path
            }
        }
        return false
    }

    fun releasePermissions(context: Context): Boolean {
        val uriPermissions = context.contentResolver.persistedUriPermissions
        for (perm in uriPermissions) {
            if (isThis(perm.uri)) {
                val flags =
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.releasePersistableUriPermission(perm.uri, flags)
                return true
            }
        }
        return false
    }

    fun getStatusesDirectories(
        context: Context,
        installedClients: List<WaClient>,
        treeUri: Uri
    ): List<WaDirectoryUri> {
        val directories = arrayListOf<WaDirectoryUri>()
        if (!isThis(treeUri)) {
            return directories
        }

        val rootDirectory = DocumentFile.fromTreeUri(context, treeUri)
        if (rootDirectory == null || !rootDirectory.isDirectory) {
            return directories
        }

        for (client in installedClients) {
            if (isLegacy) {
                addDirectory(context, treeUri, directories, rootDirectory, emptyList())
            } else {
                val additionalSegments = additionalSegments(client)
                if (additionalSegments.isEmpty() && !supportsClient(client))
                    continue

                val accountsSegments = additionalSegments.toMutableList().also { it.add("accounts") }
                val accountsDirectory = findSubdirectory(rootDirectory, accountsSegments)
                if (accountsDirectory != null) {
                    val accounts = accountsDirectory.listFiles()
                    for (account in accounts) {
                        val accountName = account.name
                        if (!accountName.isNullOrEmpty()) {
                            addDirectory(context, treeUri, directories, account, listOf("Media", ".Statuses"))
                        }
                    }
                }
                val statusesSegments = additionalSegments.toMutableList()
                    .also { it.addAll(listOf("Media", ".Statuses")) }
                addDirectory(context, treeUri, directories, rootDirectory, statusesSegments)
            }
        }

        return directories
    }

    private fun addDirectory(
        context: Context,
        treeUri: Uri,
        directories: MutableList<WaDirectoryUri>,
        rootDirectory: DocumentFile,
        pathSegments: List<String>
    ) {
        val directory = findSubdirectory(rootDirectory, pathSegments)
        if (directory != null) {
            val uri = directory.uri
            if (DocumentFile.isDocumentUri(context, uri)) {
                val documentId = DocumentsContract.getDocumentId(uri)
                val client = documentId.split(":")
                    .takeIf { it.size >= 2 }?.let { parts ->
                        WaClient.entries.firstOrNull {
                            it.pathRegex.matches(parts[1])
                        }
                    }
                directories.add(WaDirectoryUri(client, treeUri, documentId))
            }
        }
    }

    private fun findSubdirectory(
        parent: DocumentFile,
        pathSegments: List<String>,
        index: Int = 0
    ): DocumentFile? {
        if (index >= pathSegments.size) {
            return parent
        }
        val files = parent.listFiles()
        for (file in files) {
            if (file.isDirectory && file.name == pathSegments[index]) {
                return findSubdirectory(file, pathSegments, index + 1)
            }
        }
        return null
    }
}