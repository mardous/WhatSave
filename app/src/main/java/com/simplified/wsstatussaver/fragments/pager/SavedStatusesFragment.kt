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
package com.simplified.wsstatussaver.fragments.pager

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.activity.result.IntentSenderRequest
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.adapter.StatusAdapter
import com.simplified.wsstatussaver.extensions.*
import com.simplified.wsstatussaver.model.Status
import com.simplified.wsstatussaver.model.StatusType
import com.simplified.wsstatussaver.fragments.base.AbsPagerFragment

/**
 * @author Christians Martínez Alvarado (mardous)
 */
class SavedStatusesFragment : AbsPagerFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.emptyTitle.text = getString(R.string.no_saved_statuses_title, getString(statusType.nameRes))
        binding.emptyText.text = getString(R.string.save_some_statuses)
        binding.emptyButton.text = getString(R.string.lets_go)

        viewModel.getSavedStatuses(statusType).observe(viewLifecycleOwner) { statuses ->
            statusAdapter?.statuses = statuses
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    override fun onCreateAdapter(): StatusAdapter =
        StatusAdapter(
            requireActivity(),
            Glide.with(this),
            this,
            isSaveEnabled = false,
            isDeleteEnabled = true,
            isWhatsAppIconEnabled = false
        )

    override fun onStart() {
        super.onStart()
        loadStatuses()
    }

    override fun onSaveStatusClick(status: Status) {
        // do nothing
    }

    override fun onDeleteStatusClick(status: Status) {
        requestContext { context ->
            if (hasR()) {
                viewModel.createDeleteRequest(requireContext(), listOf(status)).observe(viewLifecycleOwner) {
                    deletionRequestLauncher.launch(IntentSenderRequest.Builder(it).build())
                }
            } else {
                if (!preferences().isQuickDeletion()) {
                    MaterialAlertDialogBuilder(context).setTitle(R.string.delete_saved_status_title)
                        .setMessage(R.string.this_saved_status_will_be_permanently_deleted)
                        .setPositiveButton(R.string.delete_action) { _: DialogInterface, _: Int ->
                            viewModel.deleteStatus(status).observe(viewLifecycleOwner) {
                                processDeletionResult(it)
                            }
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                } else {
                    viewModel.deleteStatus(status).observe(viewLifecycleOwner) {
                        processDeletionResult(it)
                    }
                }
            }
        }
    }

    override fun onLoadStatuses(type: StatusType) {
        viewModel.loadSavedStatuses(type)
    }

    override fun onEmptyViewButtonClick() {
        findActivityNavController(R.id.main_container).navigate(R.id.homeFragment)
    }
}