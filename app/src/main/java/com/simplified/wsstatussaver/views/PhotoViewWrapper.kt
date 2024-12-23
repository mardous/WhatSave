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
package com.simplified.wsstatussaver.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

/**
 * https://github.com/Baseflow/PhotoView/issues/708#issuecomment-1116960531
 */
class PhotoViewWrapper @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {

    private var isParentInterceptionDisallowed = false

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        isParentInterceptionDisallowed = disallowIntercept
        if (disallowIntercept) {
            // PhotoView wants to disallow parent interception, let it be.
            parent.requestDisallowInterceptTouchEvent(true) // don't ban wrapper itself
        } else {
            // PhotoView wants to allow parent interception, we need to re-check it.
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // always false when up or cancel event,
        // which will allow parent interception normally.
        val isMultiTouch = ev.pointerCount > 1

        // re-check if it's multi touch
        parent.requestDisallowInterceptTouchEvent(isParentInterceptionDisallowed || isMultiTouch)
        return false
    }
}