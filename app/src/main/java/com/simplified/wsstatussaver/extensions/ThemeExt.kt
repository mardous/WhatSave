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
package com.simplified.wsstatussaver.extensions

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.simplified.wsstatussaver.R

val Context.isNightModeEnabled: Boolean
    get() = resources.configuration.run {
        this.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

fun Context.getGeneralThemeRes(): Int =
    if (isNightModeEnabled && preferences().isJustBlack()) R.style.Theme_WhatSave_Black else R.style.Theme_WhatSave

fun Context.resolveColorAttr(@AttrRes colorAttr: Int, @ColorInt fallback: Int = Color.TRANSPARENT): Int {
    val resolvedAttr = resolveThemeAttr(colorAttr)
    // resourceId is used if it's a ColorStateList, and data if it's a color reference or a hex color
    val colorRes =
        if (resolvedAttr.resourceId != 0) {
            resolvedAttr.resourceId
        } else {
            resolvedAttr.data
        }
    try {
        return ContextCompat.getColor(this, colorRes)
    } catch (_: Resources.NotFoundException) {
    }
    return fallback
}

private fun Context.resolveThemeAttr(@AttrRes attrRes: Int) =
    TypedValue().apply { theme.resolveAttribute(attrRes, this, true) }

fun Context.primaryColor() = resolveColorAttr(com.google.android.material.R.attr.colorPrimary)
fun Context.surfaceColor(fallback: Int = Color.TRANSPARENT) =
    resolveColorAttr(com.google.android.material.R.attr.colorSurface, fallback)