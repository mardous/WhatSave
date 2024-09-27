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
package com.simplified.wsstatussaver.preferences

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.databinding.DialogSaveLocationBinding
import com.simplified.wsstatussaver.extensions.check
import com.simplified.wsstatussaver.extensions.preferences
import com.simplified.wsstatussaver.extensions.saveLocation
import com.simplified.wsstatussaver.model.SaveLocation

/**
 * @author Christians M. A. (mardous)
 */
class SaveLocationPreferenceDialog : DialogFragment(), View.OnClickListener {

    private var _binding: DialogSaveLocationBinding? = null
    private val binding get() = _binding!!

    private var selectedLocation: SaveLocation? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogSaveLocationBinding.inflate(layoutInflater)
        binding.dcimOption.setOnClickListener(this)
        binding.fileTypeOption.setOnClickListener(this)
        setSaveLocation(preferences().saveLocation)
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.save_location_title)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                selectedLocation?.let {
                    preferences().saveLocation = it
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    private fun setSaveLocation(location: SaveLocation) {
        selectedLocation = location
        when (location) {
            SaveLocation.DCIM -> {
                binding.dcimRadio.check(true)
                binding.fileTypeRadio.check(false)
            }

            SaveLocation.ByFileType -> {
                binding.dcimRadio.check(false)
                binding.fileTypeRadio.check(true)
            }
        }
    }

    override fun onClick(view: View) {
        when (view) {
            binding.dcimOption -> setSaveLocation(SaveLocation.DCIM)
            binding.fileTypeOption -> setSaveLocation(SaveLocation.ByFileType)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}