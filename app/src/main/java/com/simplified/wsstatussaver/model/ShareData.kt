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
package com.simplified.wsstatussaver.model

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.ShareCompat
import com.simplified.wsstatussaver.R

/**
 * @author Christians Martínez Alvarado (mardous)
 */
data class ShareData(val data: Set<Uri> = emptySet(), val mimeTypes: Set<String> = emptySet()) {

    val hasData: Boolean
        get() = data.isNotEmpty() && mimeTypes.isNotEmpty()

    val mimeType: String
        get() = mimeTypes.singleOrNull() ?: "*/*"

    constructor(data: Uri, mimeType: String) : this(setOf(data), setOf(mimeType))

    fun createIntent(context: Context): Intent {
        val builder = ShareCompat.IntentBuilder(context)
            .setType(mimeType)
            .setChooserTitle(context.getString(R.string.share_with))
        if (data.size == 1) {
            builder.setStream(data.single())
        } else if (data.size > 1) for (uri in data) {
            builder.addStream(uri)
        }
        builder.intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return builder.createChooserIntent()
    }

    companion object {
        val Empty = ShareData()
    }
}