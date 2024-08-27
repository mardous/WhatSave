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
package com.simplified.wsstatussaver.dialogs

import android.app.Dialog
import android.content.res.AssetManager
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.databinding.DialogMarkdownBinding
import com.simplified.wsstatussaver.extensions.setMarkdownText

/**
 * @author Christians Martínez Alvarado (mardous)
 */
class LicensesDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val licenses = getLicenses(requireContext().assets)
        if (licenses.isNullOrEmpty()) {
            return Dialog(requireContext())
        }
        val binding = DialogMarkdownBinding.inflate(layoutInflater)
        binding.message.setMarkdownText(licenses)
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.legal_notices)
            .setView(binding.root)
            .setPositiveButton(R.string.close_action, null)
            .create()
    }

    private fun getLicenses(assets: AssetManager): String? {
        try {
            return assets.open("licenses.md")
                .bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}