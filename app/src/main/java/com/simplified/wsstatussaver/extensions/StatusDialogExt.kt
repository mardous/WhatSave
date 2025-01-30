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

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isGone
import coil3.load
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simplified.wsstatussaver.databinding.DialogProgressBinding
import com.simplified.wsstatussaver.databinding.DialogStatusOptionsBinding
import com.simplified.wsstatussaver.model.Status
import com.simplified.wsstatussaver.R

private typealias StatusBinding = DialogStatusOptionsBinding
private typealias ViewCallback = (View) -> Unit
private typealias StatusCallback = (Status) -> Unit

fun Context.createProgressDialog(): Dialog {
    val builder = MaterialAlertDialogBuilder(this)
    val binding = DialogProgressBinding.inflate(LayoutInflater.from(builder.context))
    return builder.setView(binding.root).setCancelable(false).create()
}

fun Context.showStatusOptions(menu: StatusMenu): Dialog {
    val status = menu.selection
    val binding = StatusBinding.inflate(LayoutInflater.from(this))
    val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
    bottomSheetDialog.setContentView(binding.root)
    bottomSheetDialog.setOnShowListener {
        binding.image.load(status.fileUri)
        binding.setupSave(menu.isSaveEnabled) {
            bottomSheetDialog.dismiss()
            menu.onSaveClick?.invoke(status)
        }
        binding.setupDelete(menu.isDeleteEnabled) {
            bottomSheetDialog.dismiss()
            menu.onDeleteClick?.invoke(status)
        }
        binding.setupListeners {
            bottomSheetDialog.dismiss()
            when (it) {
                binding.shareAction -> menu.onShareClick?.invoke(status)
                binding.image -> menu.onPreviewClick?.invoke(status)
            }
        }
    }
    return bottomSheetDialog.also {
        it.show()
    }
}

private fun StatusBinding.setupSave(isSaveEnabled: Boolean, callback: ViewCallback) {
    if (isSaveEnabled)
        saveAction.setOnClickListener(callback)
    else saveAction.isGone = true
}

private fun StatusBinding.setupDelete(isDeleteEnabled: Boolean, callback: ViewCallback) {
    if (isDeleteEnabled)
        deleteAction.setOnClickListener(callback)
    else deleteAction.isGone = true
}

private fun StatusBinding.setupListeners(callback: ViewCallback) {
    shareAction.setOnClickListener(callback)
    image.setOnClickListener(callback)
}

class StatusMenu(
    val statuses: List<Status>,
    val selectedPosition: Int,
    val isSaveEnabled: Boolean,
    val isDeleteEnabled: Boolean
) {
    var onPreviewClick: StatusCallback? = null
    var onSaveClick: StatusCallback? = null
    var onShareClick: StatusCallback? = null
    var onDeleteClick: StatusCallback? = null

    val selection: Status
        get() = statuses[selectedPosition]

    val selectionAsList: List<Status>
        get() = listOf(selection)

    fun createClick(
        onPreviewClick: StatusCallback? = this.onPreviewClick,
        onSaveClick: StatusCallback? = this.onSaveClick,
        onShareClick: StatusCallback? = this.onShareClick,
        onDeleteClick: StatusCallback? = this.onDeleteClick
    ) = apply {
        this.onPreviewClick = onPreviewClick
        this.onSaveClick = onSaveClick
        this.onDeleteClick = onDeleteClick
        this.onShareClick = onShareClick
    }
}