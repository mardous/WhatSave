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
package com.simplified.wsstatussaver.extensions

import android.widget.ImageView
import coil3.load
import coil3.request.Disposable
import coil3.video.VideoFrameDecoder
import com.simplified.wsstatussaver.model.Status
import com.simplified.wsstatussaver.model.StatusType

fun ImageView.loadImage(status: Status): Disposable {
    return if (status.type == StatusType.VIDEO) {
        load(status.fileUri) {
            decoderFactory { result, options, _ -> VideoFrameDecoder(result.source, options) }
        }
    } else {
        load(status.fileUri)
    }
}