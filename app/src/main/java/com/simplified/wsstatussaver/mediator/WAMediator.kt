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
package com.simplified.wsstatussaver.mediator

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.simplified.wsstatussaver.extensions.defaultClientPackageName
import com.simplified.wsstatussaver.extensions.packageInfo
import com.simplified.wsstatussaver.extensions.preferences
import com.simplified.wsstatussaver.model.StatusType
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

/**
 * @author Christians Martínez Alvarado (mardous)
 */
class WAMediator internal constructor(private val context: Context) {

    private val gson = GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .create()

    private val preferences = context.preferences()
    private val packageManager: PackageManager = context.packageManager
    private val supportedClients: List<WAClient> by lazy {
        try {
            context.assets.open("clients.json").use { stream: InputStream? ->
                if (stream != null) {
                    gson.fromJson<List<WAClient>>(
                        BufferedReader(InputStreamReader(stream, "UTF-8")),
                        object : TypeToken<List<WAClient?>?>() {}.type
                    ).onEach { it.toCompleteClient(context, packageManager) }
                } else {
                    emptyList()
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to load default clients: $e")
            emptyList()
        }
    }

    fun isAnyWhatsappInstalled(): Boolean = getAnyInstalledClient() != null

    private fun getClients(filter: WAClientFilter? = null): List<WAClient> {
        if (supportedClients.isEmpty()) {
            return ArrayList()
        }
        return supportedClients.filter { client -> filter == null || filter(client) }
    }

    private fun getAnyInstalledClient(filter: WAClientFilter? = null): WAClient? {
        return getInstalledClients(filter).firstOrNull()
    }

    fun getDefaultClient(): WAClient? {
        val clientPackageName = preferences.defaultClientPackageName
        if (!clientPackageName.isNullOrEmpty()) {
            return getAnyInstalledClient { client -> client.packageName == clientPackageName }
        }
        return null
    }

    fun getDefaultClientOrAny(): WAClient? {
        return getDefaultClient() ?: getAnyInstalledClient()
    }

    fun setDefaultClient(client: WAClient?) {
        preferences.defaultClientPackageName = client?.packageName
    }

    fun getInstalledClients(filter: WAClientFilter? = null): List<WAClient> {
        return getClients { client: WAClient -> isInstalled(client) && (filter == null || filter(client)) }
    }

    fun getClientForPackage(packageName: String?): WAClient? {
        return getAnyInstalledClient { client -> client.packageName == packageName }
    }

    fun getClientsForLoader(): List<WAClient> {
        val defaultClient = getDefaultClient()
        if (defaultClient != null) {
            return listOf(defaultClient)
        }
        return getInstalledClients()
    }

    fun getSavedStatusesClients(statusType: StatusType): List<WAClient> {
        val savedClient = WAClient(statusesDirectories = listOf(statusType.savesDirectory.absolutePath))
        return listOf(savedClient)
    }

    fun getWhatsAppLaunchIntent(): Intent? {
        var installedClient = getDefaultClient()
        if (installedClient == null) {
            installedClient = getAnyInstalledClient()
        }
        return installedClient?.getLaunchIntent(packageManager)
    }

    private fun isInstalled(client: WAClient): Boolean {
        if (client.packageName.isNullOrEmpty())
            return false

        try {
            return packageManager.packageInfo(client.packageName!!) != null
        } catch (ignored: PackageManager.NameNotFoundException) {
            Log.i(TAG, "Package ${client.packageName} is not installed")
        }
        return false
    }

    companion object {
        private val TAG = WAMediator::class.java.simpleName
    }
}