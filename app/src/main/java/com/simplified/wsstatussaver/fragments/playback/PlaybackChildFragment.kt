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
package com.simplified.wsstatussaver.fragments.playback

import android.os.Bundle
import android.widget.TextView
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.WhatSaveViewModel
import com.simplified.wsstatussaver.extensions.createProgressDialog
import com.simplified.wsstatussaver.extensions.showToast
import com.simplified.wsstatussaver.extensions.startActivitySafe
import com.simplified.wsstatussaver.fragments.playback.PlaybackFragment.Companion.EXTRA_STATUS
import com.simplified.wsstatussaver.model.SavedStatus
import com.simplified.wsstatussaver.model.Status
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import kotlin.properties.Delegates

/**
 * @author Christians M. A. (mardous)
 */
abstract class PlaybackChildFragment(layoutRes: Int) : Fragment(layoutRes) {

    protected val viewModel: WhatSaveViewModel by activityViewModel()

    protected abstract val saveButton: TextView
    protected abstract val shareButton: TextView

    protected var status: Status by Delegates.notNull()
    protected var isSaved: Boolean = false

    private val progressDialog by lazy { requireContext().createProgressDialog() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = BundleCompat.getParcelable(requireArguments(), EXTRA_STATUS, Status::class.java)!!
    }

    override fun onStart() {
        super.onStart()
        viewModel.statusIsSaved(status).observe(viewLifecycleOwner) { isSaved ->
            this.isSaved = (status is SavedStatus) || isSaved
            if (isSaved) {
                saveButton.setText(R.string.saved_label)
                saveButton.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_round_check_24dp, 0, 0, 0
                )
            } else {
                saveButton.setText(R.string.save_action)
                saveButton.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_save_alt_24dp, 0, 0, 0
                )
            }
        }
        saveButton.setOnClickListener {
            if (!isSaved) {
                viewModel.saveStatus(status).observe(viewLifecycleOwner) { result ->
                    if (result.isSuccess) {
                        showToast(R.string.saved_successfully)
                    } else if (!result.isSaving) {
                        showToast(R.string.failed_to_save)
                    }
                }
            }
        }
        shareButton.setOnClickListener {
            viewModel.shareStatus(status).observe(viewLifecycleOwner) { result ->
                if (result.isLoading) {
                    progressDialog.show()
                } else {
                    progressDialog.dismiss()
                    if (result.isSuccess) {
                        startActivitySafe(result.data.createIntent(requireContext()))
                    }
                }
            }
        }
    }
}