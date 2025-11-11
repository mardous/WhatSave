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
package com.simplified.wsstatussaver.mvvm

import android.net.Uri
import com.simplified.wsstatussaver.model.SavedStatus
import com.simplified.wsstatussaver.model.ShareData
import com.simplified.wsstatussaver.model.Status

data class DeletionResult(
    val isDeleting: Boolean = false,
    val statuses: List<Status> = arrayListOf(),
    val deleted: Int = 0
) {
    val isSuccess: Boolean
        get() = statuses.size == deleted

    companion object {
        fun single(status: Status, success: Boolean) =
            DeletionResult(false, listOf(status), if (success) 1 else 0)
    }
}

data class SaveResult(
    val isSaving: Boolean = false,
    val statuses: List<Status> = arrayListOf(),
    val uris: List<Uri> = arrayListOf(),
    val saved: Int = 0
) {
    val isSuccess: Boolean
        get() = statuses.isNotEmpty() && uris.isNotEmpty() && statuses.size == uris.size

    companion object {
        fun single(status: SavedStatus?): SaveResult {
            if (status != null) {
                return SaveResult(
                    isSaving = false,
                    statuses = listOf(status),
                    uris = listOf(status.fileUri),
                    saved = 1
                )
            }
            return SaveResult()
        }
    }
}

data class ShareResult(
    val isLoading: Boolean = false,
    val data: ShareData = ShareData()
) {
    val isSuccess: Boolean
        get() = data.hasData
}