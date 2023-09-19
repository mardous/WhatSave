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
package com.simplified.wsstatussaver.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.extensions.preferences
import com.simplified.wsstatussaver.extensions.privacyPolicyAccepted

class PrivacyDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.privacy_and_terms)
            .setMessage(getPrivacyContent())
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                requireContext().preferences().privacyPolicyAccepted = true
            }
            .setNegativeButton(R.string.close_action) { _: DialogInterface, _: Int ->
                requireActivity().finishAffinity()
            }
            .create()
    }

    private fun getPrivacyContent(): CharSequence {
        return requireContext().assets.open("privacy.txt").use {
            it.bufferedReader().readText()
        }
    }
}