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
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.databinding.DialogStatusOptionsBinding
import com.simplified.wsstatussaver.interfaces.IStatusCallback
import com.simplified.wsstatussaver.model.Status

private typealias StatusBinding = DialogStatusOptionsBinding
private typealias ViewCallback = (View) -> Unit

fun Context.showStatusOptions(
    status: Status,
    isSaveEnabled: Boolean,
    isDeleteEnabled: Boolean,
    callback: IStatusCallback
): Dialog {
    val binding = StatusBinding.inflate(LayoutInflater.from(this))
    val bottomSheetDialog = BottomSheetDialog(this)
    bottomSheetDialog.setContentView(binding.root)
    bottomSheetDialog.setOnShowListener {
        binding.setupPreview(status)
        binding.setupSave(isSaveEnabled) {
            bottomSheetDialog.dismiss()
            callback.onSaveStatusClick(status)
        }
        binding.setupDelete(isDeleteEnabled) {
            bottomSheetDialog.dismiss()
            callback.onDeleteStatusClick(status)
        }
        binding.setupListeners {
            bottomSheetDialog.dismiss()
            when (it) {
                binding.shareAction -> callback.onShareStatusClick(status)
                binding.image -> callback.onPreviewStatusClick(status)
            }
        }

        Glide.with(this)
            .asBitmap()
            .load(status.path)
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .into(binding.image)
    }
    return bottomSheetDialog.also {
        it.show()
    }
}

private fun StatusBinding.setupPreview(status: Status) {
    if (status.isVideo) {
        previewAction.setCompoundDrawablesRelativeWithIntrinsicBounds(
            R.drawable.ic_round_play_arrow_24dp, 0, 0, 0
        )
        previewAction.setText(R.string.play_video_action)
    } else {
        previewAction.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_image_24dp, 0, 0, 0)
        previewAction.setText(R.string.view_image_action)
    }
}

private fun StatusBinding.setupSave(isSaveEnabled: Boolean, callback: ViewCallback) {
    if (isSaveEnabled)
        saveAction.setOnClickListener(callback)
    else saveAction.isVisible = false
}

private fun StatusBinding.setupDelete(isDeleteEnabled: Boolean, callback: ViewCallback) {
    if (isDeleteEnabled)
        deleteAction.setOnClickListener(callback)
    else deleteAction.isVisible = false
}

private fun StatusBinding.setupListeners(callback: ViewCallback) {
    shareAction.setOnClickListener(callback)
    image.setOnClickListener(callback)
}