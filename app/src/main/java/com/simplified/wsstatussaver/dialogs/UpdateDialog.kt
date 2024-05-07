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

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.WhatSaveViewModel
import com.simplified.wsstatussaver.databinding.DialogUpdateInfoBinding
import com.simplified.wsstatussaver.extensions.lastUpdateSearch
import com.simplified.wsstatussaver.extensions.preferences
import com.simplified.wsstatussaver.extensions.showToast
import com.simplified.wsstatussaver.logUpdateRequest
import com.simplified.wsstatussaver.update.GitHubRelease
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class UpdateDialog : BottomSheetDialogFragment(), View.OnClickListener {

    private var _binding: DialogUpdateInfoBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModel<WhatSaveViewModel>()

    private lateinit var release: GitHubRelease

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        release = BundleCompat.getParcelable(requireArguments(), EXTRA_RELEASE, GitHubRelease::class.java)!!
        if (release.isNewer(requireContext())) {
            _binding = DialogUpdateInfoBinding.inflate(layoutInflater)
            binding.downloadAction.setOnClickListener(this)
            binding.cancelAction.setOnClickListener(this)
            fillVersionInfo()
            return BottomSheetDialog(requireContext()).also {
                it.setContentView(binding.root)
                it.setOnShowListener {
                    preferences().lastUpdateSearch = System.currentTimeMillis()
                }
            }
        }
        return MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.the_app_is_up_to_date)
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }

    override fun onClick(view: View) {
        when (view) {
            binding.downloadAction -> {
                viewModel.downloadUpdate(requireContext(), release)
                showToast(R.string.downloading_update)
                logUpdateRequest(release.name, true)
            }

            binding.cancelAction -> {
                release.setIgnored(requireContext())
                logUpdateRequest(release.name, false)
            }
        }
        dismiss()
    }

    @SuppressLint("SetTextI18n")
    private fun fillVersionInfo() {
        binding.versionName.text = release.name
        if (release.body.isNotEmpty()) {
            binding.versionInfo.text = release.body
        } else {
            binding.versionInfo.isVisible = false
        }
        val date = release.getFormattedDate()
        val size = release.getDownloadSize()
        if (date != null && size != null) {
            binding.versionDetail.text = "$date • $size"
        } else if (date != null) {
            binding.versionDetail.text = date
        } else if (size != null) {
            binding.versionDetail.text = size
        } else {
            binding.versionDetail.isVisible = false
        }
    }

    companion object {
        private const val EXTRA_RELEASE = "extra_release"

        fun create(release: GitHubRelease) = UpdateDialog().apply {
            arguments = bundleOf(EXTRA_RELEASE to release)
        }
    }
}