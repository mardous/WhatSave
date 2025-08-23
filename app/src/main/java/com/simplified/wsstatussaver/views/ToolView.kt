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
package com.simplified.wsstatussaver.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import com.simplified.wsstatussaver.R
import androidx.core.content.withStyledAttributes

class ToolView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.materialCardViewStyle
) : MaterialCardView(context, attrs, defStyleAttr) {

    private var titleView: TextView? = null
    private var descView: TextView? = null
    private var iconView: ImageView? = null

    init {
        addView(inflate(context, R.layout.item_view_tool, null))
        titleView = findViewById(R.id.title)
        descView = findViewById(R.id.description)
        iconView = findViewById(R.id.icon)

        context.withStyledAttributes(attrs, R.styleable.ToolView, defStyleAttr, 0) {
            setTitle(getString(R.styleable.ToolView_toolName))
            setDescription(getString(R.styleable.ToolView_toolDescription))
            setIcon(getDrawable(R.styleable.ToolView_toolIcon))
        }
    }

    fun setIcon(icon: Drawable?) {
        iconView?.setImageDrawable(icon)
    }

    fun setTitle(title: CharSequence?) {
        titleView?.text = title
    }

    fun setDescription(description: CharSequence?) {
        descView?.text = description
    }
}