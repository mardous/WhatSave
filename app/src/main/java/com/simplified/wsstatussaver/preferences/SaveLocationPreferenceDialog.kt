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
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.databinding.DialogSaveLocationBinding
import com.simplified.wsstatussaver.extensions.check
import com.simplified.wsstatussaver.extensions.preferences
import com.simplified.wsstatussaver.extensions.saveLocation
import com.simplified.wsstatussaver.extensions.showToast
import com.simplified.wsstatussaver.model.SaveLocation
import com.simplified.wsstatussaver.model.StatusType
import com.simplified.wsstatussaver.storage.whatsapp.WaSavedContentStorage
import org.koin.android.ext.android.inject

/**
 * @author Christians M. A. (mardous)
 */
class SaveLocationPreferenceDialog : DialogFragment(), View.OnClickListener {

    private val waSavedContentStorage: WaSavedContentStorage by inject()

    private var _binding: DialogSaveLocationBinding? = null
    private val binding get() = _binding!!

    private var selectedLocation: SaveLocation? = null
    private var viewMapping = listOf(
        ViewIdToSaveLocation(R.id.dcim_option, R.id.dcim_radio, SaveLocation.DCIM),
        ViewIdToSaveLocation(R.id.file_type_option, R.id.file_type_radio, SaveLocation.ByFileType),
        ViewIdToSaveLocation(R.id.custom_location_option, R.id.custom_location_radio, SaveLocation.Custom)
    )

    private lateinit var imagesDirectorySelector: ActivityResultLauncher<Uri?>
    private lateinit var videosDirectorySelector: ActivityResultLauncher<Uri?>

    private class ViewIdToSaveLocation(val parentId: Int, val radioId: Int, val location: SaveLocation)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogSaveLocationBinding.inflate(layoutInflater)
        binding.dcimOption.setOnClickListener(this)
        binding.fileTypeOption.setOnClickListener(this)
        binding.customLocationOption.setOnClickListener(this)
        binding.imagesLocation.setOnClickListener(this)
        binding.videosLocation.setOnClickListener(this)
        setSaveLocation(preferences().saveLocation)
        updateCustomDirectoryNames()
        registerForActivityResult()
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
        for (entry in viewMapping) {
            val view = binding.root.findViewById<View>(entry.radioId)
            if (view is CompoundButton) {
                view.check(entry.location == selectedLocation)
            }
        }
    }

    private fun setCustomDirectoryLocation(type: StatusType, uri: Uri?) {
        if (uri == null)
            return

        val isSuccess = waSavedContentStorage.setCustomDirectory(type, uri)
        if (isSuccess) {
            showToast(R.string.custom_save_directory_set)
            updateCustomDirectoryNames()
        } else {
            showToast(R.string.unable_to_set_custom_save_directory)
        }
    }

    override fun onClick(view: View) {
        val location = viewMapping.firstOrNull { it.parentId == view.id }
        if (location != null) {
            setSaveLocation(location.location)
        } else {
            when (view) {
                binding.imagesLocation -> imagesDirectorySelector.launch(null)
                binding.videosLocation -> videosDirectorySelector.launch(null)
            }
        }
    }

    private fun updateCustomDirectoryNames() {
        binding.imagesLocation.text = waSavedContentStorage.getCustomDirectoryName(StatusType.IMAGE)
        binding.videosLocation.text = waSavedContentStorage.getCustomDirectoryName(StatusType.VIDEO)
    }

    private fun registerForActivityResult() {
        imagesDirectorySelector = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            setCustomDirectoryLocation(StatusType.IMAGE, uri)
        }
        videosDirectorySelector = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            setCustomDirectoryLocation(StatusType.VIDEO, uri)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}