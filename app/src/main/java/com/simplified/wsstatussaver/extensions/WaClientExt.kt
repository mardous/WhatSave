/*
 * Copyright (C) 2024 Christians Martínez Alvarado
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
package com.simplified.wsstatussaver.extensions

import android.content.Context
import com.simplified.wsstatussaver.logDefaultClient
import com.simplified.wsstatussaver.model.WaClient

val REGEX_WHATSAPP = """^(?:Android/media/com\.whatsapp/WhatsApp/|WhatsApp/)(?:accounts/\d+/)?Media/\.Statuses$""".toRegex()
val REGEX_BUSINESS = """^(?:Android/media/com\.whatsapp\.w4b/WhatsApp Business/|WhatsApp Business/)(?:accounts/\d+/)?Media/\.Statuses$""".toRegex()

fun Context.getDefaultClient(): WaClient? {
    val clientPackageName = preferences().defaultClientPackageName
    if (!clientPackageName.isNullOrEmpty()) {
        return getClientIfInstalled(clientPackageName)
    }
    return null
}

fun Context.setDefaultClient(client: WaClient?) {
    logDefaultClient(client?.packageName ?: "cleared")
    preferences().defaultClientPackageName = client?.packageName
}

fun Context.getAllInstalledClients() = WaClient.entries.filter { it.isInstalled(this) }

fun Context.getClientIfInstalled(packageName: String?) =
    getAllInstalledClients().firstOrNull { it.packageName == packageName }

fun Context.getPreferredClient() = getDefaultClient() ?: getAllInstalledClients().firstOrNull()

fun List<WaClient>.getPreferred(context: Context): List<WaClient> {
    val preferred = context.getDefaultClient()
    return if (preferred == null) this else filter { it.packageName == preferred.packageName }
}