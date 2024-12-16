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

import android.annotation.SuppressLint
import android.view.View
import androidx.core.view.WindowInsetsCompat
import com.simplified.wsstatussaver.getApp
import dev.chrisbanes.insetter.InsetterDsl
import dev.chrisbanes.insetter.applyInsetter

@SuppressLint("DiscouragedApi", "InternalInsetResource")
fun WindowInsetsCompat?.getBottomInsets(): Int {
    var bottomInsets = this?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom
    if (bottomInsets == null) {
        val resourceId = getApp().resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            bottomInsets = getApp().resources.getDimensionPixelSize(resourceId)
        }
    }
    return bottomInsets ?: 0
}

fun View.applyPortraitInsetter(build: InsetterDsl.() -> Unit) {
    if (!isLandscape()) {
        applyInsetter(build)
    }
}

fun View.applyLandscapeInsetter(build: InsetterDsl.() -> Unit) {
    if (isLandscape()) {
        applyInsetter(build)
    }
}