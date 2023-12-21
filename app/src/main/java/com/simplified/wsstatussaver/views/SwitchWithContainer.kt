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
import android.util.AttributeSet
import android.widget.CompoundButton
import android.widget.FrameLayout
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.simplified.wsstatussaver.R

class SwitchWithContainer @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr), CompoundButton.OnCheckedChangeListener {

    private var onCheckedChangedListener: CompoundButton.OnCheckedChangeListener? = null
    private var cardView: MaterialCardView? = null
    var switch: MaterialSwitch? = null

    var isChecked: Boolean
        get() = switch?.isChecked ?: false
        set(value) {
            switch?.isChecked = value
        }
    var text: CharSequence?
        get() = switch?.text
        set(value) {
            switch?.text = value
        }

    init {
        addView(inflate(context, R.layout.switch_with_container, null))
        cardView = findViewById(R.id.cardView)
        cardView?.checkedIcon = null
        switch = findViewById(R.id.switchWidget)
        switch?.setOnCheckedChangeListener(this)

        val a = context.obtainStyledAttributes(attrs, R.styleable.SwitchWithContainer, defStyleAttr, 0)
        text = a.getString(R.styleable.SwitchWithContainer_android_text)
        isChecked = a.getBoolean(R.styleable.SwitchWithContainer_android_checked, false)
        a.recycle()
    }

    fun setOnCheckedChangeListener(listener: CompoundButton.OnCheckedChangeListener?) {
        this.onCheckedChangedListener = listener
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        cardView?.isChecked = isChecked
        onCheckedChangedListener?.onCheckedChanged(buttonView, isChecked)
    }
}