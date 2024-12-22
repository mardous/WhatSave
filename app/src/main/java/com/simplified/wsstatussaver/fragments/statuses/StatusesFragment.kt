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
package com.simplified.wsstatussaver.fragments.statuses

import android.app.Activity
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.WhatSaveViewModel
import com.simplified.wsstatussaver.adapter.StatusAdapter
import com.simplified.wsstatussaver.databinding.FragmentStatusesBinding
import com.simplified.wsstatussaver.extensions.PREFERENCE_DEFAULT_CLIENT
import com.simplified.wsstatussaver.extensions.PREFERENCE_EXCLUDE_SAVED_STATUSES
import com.simplified.wsstatussaver.extensions.PREFERENCE_STATUSES_LOCATION
import com.simplified.wsstatussaver.extensions.PREFERENCE_WHATSAPP_ICON
import com.simplified.wsstatussaver.extensions.createProgressDialog
import com.simplified.wsstatussaver.extensions.dip
import com.simplified.wsstatussaver.extensions.findActivityNavController
import com.simplified.wsstatussaver.extensions.getPreferredClient
import com.simplified.wsstatussaver.extensions.hasR
import com.simplified.wsstatussaver.extensions.isNullOrEmpty
import com.simplified.wsstatussaver.extensions.isQuickDeletion
import com.simplified.wsstatussaver.extensions.isWhatsappIcon
import com.simplified.wsstatussaver.extensions.launchSafe
import com.simplified.wsstatussaver.extensions.preferences
import com.simplified.wsstatussaver.extensions.primaryColor
import com.simplified.wsstatussaver.extensions.requestPermissions
import com.simplified.wsstatussaver.extensions.requestView
import com.simplified.wsstatussaver.extensions.showToast
import com.simplified.wsstatussaver.extensions.startActivitySafe
import com.simplified.wsstatussaver.fragments.base.BaseFragment
import com.simplified.wsstatussaver.fragments.binding.StatusesBinding
import com.simplified.wsstatussaver.fragments.playback.PlaybackFragmentArgs
import com.simplified.wsstatussaver.interfaces.IPermissionChangeListener
import com.simplified.wsstatussaver.interfaces.IScrollable
import com.simplified.wsstatussaver.interfaces.IStatusCallback
import com.simplified.wsstatussaver.model.Status
import com.simplified.wsstatussaver.model.StatusQueryResult
import com.simplified.wsstatussaver.mvvm.DeletionResult
import com.simplified.wsstatussaver.mvvm.SaveResult
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * @author Christians Martínez Alvarado (mardous)
 */
abstract class StatusesFragment : BaseFragment(R.layout.fragment_statuses),
    View.OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener,
    OnRefreshListener, IScrollable, IPermissionChangeListener, IStatusCallback {

    private var _binding: StatusesBinding? = null
    private lateinit var deletionRequestLauncher: ActivityResultLauncher<IntentSenderRequest>
    private val progressDialog by lazy { requireContext().createProgressDialog() }
    private var deletedStatuses = mutableListOf<Status>()

    protected val binding get() = _binding!!
    protected val viewModel by activityViewModel<WhatSaveViewModel>()
    protected var statusAdapter: StatusAdapter? = null

    protected abstract val lastResult: StatusQueryResult?

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
        _binding = StatusesBinding(FragmentStatusesBinding.bind(view)).apply {
            swipeRefreshLayout.setOnRefreshListener(this@StatusesFragment)
            swipeRefreshLayout.setColorSchemeColors(view.context.primaryColor())

            recyclerView.setPadding(dip(R.dimen.status_item_margin))
            recyclerView.layoutManager = GridLayoutManager(requireActivity(), resources.getInteger(R.integer.statuses_grid_span_count))
            recyclerView.adapter = createAdapter().apply {
                registerAdapterDataObserver(adapterDataObserver)
            }.also { newStatusAdapter ->
                statusAdapter = newStatusAdapter
            }

            emptyButton.setOnClickListener(this@StatusesFragment)
        }
        deletionRequestLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                viewModel.removeStatuses(deletedStatuses)
                showToast(R.string.deletion_success)
            }
        }
        preferences().registerOnSharedPreferenceChangeListener(this)
    }

    protected open fun createAdapter(): StatusAdapter {
        return StatusAdapter(
            requireActivity(),
            this,
            isSaveEnabled = true,
            isDeleteEnabled = false,
            isWhatsAppIconEnabled = preferences().isWhatsappIcon()
        )
    }

    override fun scrollToTop() {
        binding.recyclerView.scrollToPosition(0)
    }

    override fun onStart() {
        super.onStart()
        statusesActivity.addPermissionsChangeListener(this)
    }

    override fun onStop() {
        super.onStop()
        statusesActivity.removePermissionsChangeListener(this)
    }

    override fun onClick(view: View) {
        if (view == binding.emptyButton) {
            val resultCode = lastResult?.code
            if (resultCode != StatusQueryResult.ResultCode.Loading) {
                when (resultCode) {
                    StatusQueryResult.ResultCode.PermissionError -> requestPermissions()
                    StatusQueryResult.ResultCode.NotInstalled -> requireActivity().finish()
                    StatusQueryResult.ResultCode.NoStatuses -> requireContext().getPreferredClient()?.let {
                        startActivitySafe(it.getLaunchIntent(requireContext().packageManager))
                    }

                    else -> onRefresh()
                }
            }
        }
    }

    override fun permissionsStateChanged(hasPermissions: Boolean) {
        viewModel.reloadAll()
    }

    override fun multiSelectionItemClick(item: MenuItem, selection: List<Status>) = requestView {
        when (item.itemId) {
            R.id.action_share -> {
                viewModel.shareStatuses(selection).observe(viewLifecycleOwner) {
                    if (it.isLoading) {
                        progressDialog.show()
                    } else {
                        progressDialog.dismiss()
                        if (it.isSuccess) {
                            startActivitySafe(it.data.createIntent(requireContext()))
                        }
                    }
                }
            }

            R.id.action_save -> {
                viewModel.saveStatuses(selection).observe(viewLifecycleOwner) {
                    processSaveResult(it)
                }
            }

            R.id.action_delete -> {
                if (hasR()) {
                    viewModel.createDeleteRequest(requireContext(), selection).observe(viewLifecycleOwner) {
                        deletedStatuses = selection.toMutableList()
                        deletionRequestLauncher.launchSafe(IntentSenderRequest.Builder(it).build())
                    }
                } else {
                    if (!preferences().isQuickDeletion()) {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.delete_saved_statuses_title)
                            .setMessage(
                                getString(R.string.x_saved_statuses_will_be_permanently_deleted, selection.size)
                            )
                            .setPositiveButton(R.string.delete_action) { _: DialogInterface, _: Int ->
                                viewModel.deleteStatuses(selection).observe(viewLifecycleOwner) {
                                    processDeletionResult(it)
                                }
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                    } else {
                        viewModel.deleteStatuses(selection).observe(viewLifecycleOwner) {
                            processDeletionResult(it)
                        }
                    }
                }
            }
        }
    }

    override fun previewStatusesClick(statuses: List<Status>, startPosition: Int) {
        findActivityNavController(R.id.global_container).navigate(
            R.id.playbackFragment,
            PlaybackFragmentArgs.Builder(statuses.toTypedArray(), startPosition).build()
                .toBundle()
        )
    }

    protected fun data(result: StatusQueryResult) {
        statusAdapter?.statuses = result.statuses
        binding.swipeRefreshLayout.isRefreshing = result.isLoading
        if (result.code.titleRes != 0) {
            binding.emptyTitle.text = getString(result.code.titleRes)
            binding.emptyTitle.isVisible = true
        } else {
            binding.emptyTitle.isVisible = false
        }
        if (result.code.descriptionRes != 0) {
            binding.emptyText.text = getString(result.code.descriptionRes)
            binding.emptyText.isVisible = true
        } else {
            binding.emptyText.isVisible = false
        }
        if (result.code.buttonTextRes != 0) {
            binding.emptyButton.text = getString(result.code.buttonTextRes)
            binding.emptyButton.isVisible = true
        } else {
            binding.emptyButton.isVisible = false
        }
    }

    private fun processSaveResult(result: SaveResult) = requestView { view ->
        if (result.isSaving) {
            Snackbar.make(view, R.string.saving_status, Snackbar.LENGTH_SHORT).show()
        } else {
            if (result.isSuccess) {
                if (result.saved == 1) {
                    Snackbar.make(view, R.string.saved_successfully, Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(view, getString(R.string.saved_x_statuses, result.saved), Snackbar.LENGTH_SHORT)
                        .show()
                }
                viewModel.reloadAll()
            } else {
                Snackbar.make(view, R.string.failed_to_save, Snackbar.LENGTH_SHORT).show()
            }
        }
        statusAdapter?.isSavingContent = result.isSaving
    }

    private fun processDeletionResult(result: DeletionResult) = requestView { view ->
        if (result.isDeleting) {
            Snackbar.make(view, R.string.deleting_please_wait, Snackbar.LENGTH_SHORT).show()
        } else if (result.isSuccess) {
            Snackbar.make(view, R.string.deletion_success, Snackbar.LENGTH_SHORT).show()
            viewModel.reloadAll()
        } else {
            Snackbar.make(view, R.string.deletion_failed, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            PREFERENCE_DEFAULT_CLIENT,
            PREFERENCE_STATUSES_LOCATION,
            PREFERENCE_EXCLUDE_SAVED_STATUSES -> onRefresh()

            PREFERENCE_WHATSAPP_ICON -> statusAdapter?.isWhatsAppIconEnabled = sharedPreferences.isWhatsappIcon()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        preferences().unregisterOnSharedPreferenceChangeListener(this)
        deletedStatuses.clear()
        statusAdapter?.unregisterAdapterDataObserver(adapterDataObserver)
        statusAdapter = null
    }

    private val adapterDataObserver = object : AdapterDataObserver() {
        override fun onChanged() {
            binding.emptyView.isVisible = statusAdapter.isNullOrEmpty()
        }
    }
}