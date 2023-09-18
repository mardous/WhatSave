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
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.databinding.DialogUpdateInfoBinding
import com.simplified.wsstatussaver.extensions.*
import com.simplified.wsstatussaver.logUpdateRequest
import com.simplified.wsstatussaver.update.GitHubRelease

class UpdateDialog : DialogFragment() {

    private var _binding: DialogUpdateInfoBinding? = null
    private val binding get() = _binding!!

    private lateinit var release: GitHubRelease

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        release = BundleCompat.getParcelable(requireArguments(), EXTRA_RELEASE, GitHubRelease::class.java)!!
        if (release.isNewer(requireContext())) {
            _binding = DialogUpdateInfoBinding.inflate(layoutInflater)
            fillVersionInfo()
            return MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.update_title)
                .setView(binding.root)
                .setCancelable(false)
                .setPositiveButton(R.string.download_action) { _: DialogInterface, _: Int ->
                    context?.openWeb(release.getDownloadUrl())
                    logUpdateRequest(release.name, true)
                }
                .setNegativeButton(android.R.string.cancel) { _: DialogInterface, _: Int ->
                    release.setIgnored(requireContext())
                    logUpdateRequest(release.name, false)
                }
                .setNeutralButton(R.string.more_info_action) { _: DialogInterface, _: Int ->
                    context?.openWeb(release.url)
                }
                .create().also { dialog ->
                    dialog.setOnShowListener {
                        preferences().lastUpdateSearch = System.currentTimeMillis()
                    }
                }
        }
        return MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.the_app_is_up_to_date)
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }

    private fun fillVersionInfo() {
        binding.versionName.text = release.tag
        if (release.body.isNotEmpty()) {
            binding.versionInfo.text = release.body
        } else {
            binding.versionInfo.isVisible = false
        }
        val date = release.getFormattedDate()
        if (date != null) {
            binding.releaseDate.text = getString(R.string.release_date, date)
        } else {
            binding.releaseDate.isVisible = false
        }
        val size = release.getDownloadSize()
        if (size != null) {
            binding.downloadSize.text = getString(R.string.download_size, size)
        } else {
            binding.downloadSize.isVisible = false
        }
    }

    companion object {
        private const val EXTRA_RELEASE = "extra_release"

        fun create(release: GitHubRelease) = UpdateDialog().apply {
            arguments = bundleOf(EXTRA_RELEASE to release)
        }
    }
}