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
import android.content.DialogInterface
import android.text.InputFilter
import android.view.LayoutInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.databinding.DialogStatusNameBinding

typealias InputCallback = (String) -> Unit

fun Context.requestStatusName(generatedName: String, onInput: InputCallback): Dialog {
    val binding: DialogStatusNameBinding
    return MaterialAlertDialogBuilder(this)
        .also {
            binding = DialogStatusNameBinding.inflate(LayoutInflater.from(it.context)).apply {
                editText.filters += InputFilter { source, _, _, _, _, _ ->
                    if (source.isEmpty()) null
                    else if (STATUS_NAME_INVALID_CHARS.indexOf(source.last()) > -1) source.dropLast(1)
                    else null
                }
            }
        }
        .setTitle(R.string.save_action)
        .setView(binding.root)
        .setPositiveButton(android.R.string.ok, null)
        .setNegativeButton(android.R.string.cancel, null)
        .show().also { dialog ->
            dialog.setOnShowListener {
                binding.editText.requestFocus()
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    var entered = binding.editText.text?.toString()
                    if (entered.isNullOrBlank()) {
                        entered = generatedName
                    }
                    onInput(entered)
                    dialog.dismiss()
                }
            }
        }
}