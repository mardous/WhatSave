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
import android.view.View.LAYOUT_DIRECTION_RTL
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Button
import android.widget.ImageView
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.marginBottom
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.simplified.wsstatussaver.getApp

@SuppressLint("DiscouragedApi", "InternalInsetResource")
fun WindowInsetsCompat?.getBottomInsets(): Int {
    var bottomInsets = this?.getInsets(Type.systemBars())?.bottom
    if (bottomInsets == null) {
        val resourceId = getApp().resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            bottomInsets = getApp().resources.getDimensionPixelSize(resourceId)
        }
    }
    return bottomInsets ?: 0
}

typealias InsetsConsumer = (View, Insets) -> Unit

private fun View.isMarginRequired() =
    this is FloatingActionButton || this is Button || this is ImageView

fun View.applyHorizontalWindowInsets(
    left: Boolean = true,
    right: Boolean = true,
    ime: Boolean = false,
    padding: Boolean = !isMarginRequired(),
    addedSpace: Space = Space(),
    consumer: InsetsConsumer? = null
) = applyWindowInsets(
    left = left,
    right = right,
    ime = ime,
    padding = padding,
    addedSpace = addedSpace,
    consumer = consumer
)

fun View.applyBottomWindowInsets(
    ime: Boolean = false,
    padding: Boolean = !isMarginRequired(),
    addedSpace: Space = Space(),
    consumer: InsetsConsumer? = null
) = applyWindowInsets(
    bottom = true,
    ime = ime,
    padding = padding,
    addedSpace = addedSpace,
    consumer = consumer
)

fun View.applyWindowInsets(
    top: Boolean = false,
    left: Boolean = false,
    right: Boolean = false,
    bottom: Boolean = false,
    ime: Boolean = false,
    padding: Boolean = !isMarginRequired(),
    addedSpace: Space = Space(),
    consumer: InsetsConsumer? = null
) {
    if (tag == INSETS_TAG)
        return

    ViewCompat.setOnApplyWindowInsetsListener(this) { v: View, insets: WindowInsetsCompat ->
        val mask = Type.systemBars() or Type.displayCutout() or if (ime) Type.ime() else 0
        val i = insets.getInsets(mask)
        val start = if (layoutDirection == LAYOUT_DIRECTION_RTL) i.right else i.left
        val end = if (layoutDirection == LAYOUT_DIRECTION_RTL) i.left else i.right
        if (!padding) {
            if (v.layoutParams is MarginLayoutParams) {
                v.updateLayoutParams<MarginLayoutParams> {
                    marginStart = (if (left) start else marginStart(false)) + addedSpace.left
                    topMargin = (if (top) i.top else marginTop(false)) + addedSpace.top
                    marginEnd = (if (right) end else marginEnd(false)) + addedSpace.right
                    bottomMargin = (if (bottom) i.bottom else marginBottom(false)) + addedSpace.bottom
                }
            }
        } else {
            v.setPaddingRelative(
                (if (left) start else marginStart(true)) + addedSpace.left,
                (if (top) i.top else marginTop(true)) + addedSpace.top,
                (if (right) end else marginEnd(true)) + addedSpace.right,
                (if (bottom) i.bottom else marginBottom(true)) + addedSpace.bottom
            )
        }
        tag = INSETS_TAG
        consumer?.invoke(v, i)
        insets
    }
}

private const val INSETS_TAG = "[view_insets_applied]"

fun View.paddingSpace() = Space(paddingTop, paddingBottom, paddingStart, paddingEnd)

private fun View.marginTop(padding: Boolean) = if (padding) paddingTop else marginTop
private fun View.marginBottom(padding: Boolean) = if (padding) paddingBottom else marginBottom
private fun View.marginStart(padding: Boolean) = if (padding) paddingStart else marginStart
private fun View.marginEnd(padding: Boolean) = if (padding) paddingEnd else marginEnd

class Space internal constructor(
    val top: Int = 0,
    val bottom: Int = 0,
    val left: Int = 0,
    val right: Int = 0
) {
    companion object {
        fun horizontal(margin: Int) = Space(left = margin, right = margin)
        fun vertical(margin: Int) = Space(top = margin, bottom = margin)
        fun top(margin: Int) = Space(top = margin)
        fun left(margin: Int) = Space(left = margin)
        fun right(margin: Int) = Space(right = margin)
        fun bottom(margin: Int) = Space(bottom = margin)
    }
}