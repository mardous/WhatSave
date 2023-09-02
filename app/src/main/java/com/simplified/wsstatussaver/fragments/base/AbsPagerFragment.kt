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
package com.simplified.wsstatussaver.fragments.base

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.WhatSaveViewModel
import com.simplified.wsstatussaver.adapter.StatusAdapter
import com.simplified.wsstatussaver.databinding.FragmentStatusesPageBinding
import com.simplified.wsstatussaver.extensions.*
import com.simplified.wsstatussaver.fragments.binding.StatusesPageBinding
import com.simplified.wsstatussaver.interfaces.IMultiStatusCallback
import com.simplified.wsstatussaver.interfaces.IPermissionChangeListener
import com.simplified.wsstatussaver.interfaces.IScrollable
import com.simplified.wsstatussaver.model.Status
import com.simplified.wsstatussaver.model.StatusType
import com.simplified.wsstatussaver.mvvm.DeletionResult
import com.simplified.wsstatussaver.mvvm.SaveResult
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * @author Christians Martínez Alvarado (mardous)
 */
abstract class AbsPagerFragment : BaseFragment(R.layout.fragment_statuses_page),
    OnRefreshListener,
    IScrollable,
    IPermissionChangeListener,
    IMultiStatusCallback {

    private var _binding: StatusesPageBinding? = null
    protected val binding get() = _binding!!

    protected val viewModel by activityViewModel<WhatSaveViewModel>()
    protected lateinit var deletionRequestLauncher: ActivityResultLauncher<IntentSenderRequest>
    protected lateinit var statusType: StatusType
    protected var statusAdapter: StatusAdapter? = null

    private val statusesFragment: AbsStatusesFragment
        get() = parentFragment as AbsStatusesFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val arguments = arguments
        if (arguments != null) {
            statusType = arguments.serializable(EXTRA_TYPE, StatusType::class)!!
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentStatusesPageBinding.bind(view)
        _binding = StatusesPageBinding(binding).apply {
            swipeRefreshLayout.setOnRefreshListener(this@AbsPagerFragment)
            swipeRefreshLayout.setColorSchemeColors(view.context.resolveColorAttr(androidx.appcompat.R.attr.colorPrimary))

            recyclerView.setPadding(resources.displayMetrics.density.toInt() * 4)
            recyclerView.layoutManager =
                GridLayoutManager(requireActivity(), resources.getInteger(R.integer.statuses_grid_span_count))
            recyclerView.adapter = onCreateAdapter().apply {
                registerAdapterDataObserver(adapterDataObserver)
            }.also { newStatusAdapter ->
                statusAdapter = newStatusAdapter
            }

            emptyButton.setOnClickListener {
                onEmptyViewButtonClick()
            }
        }

        deletionRequestLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                Toast.makeText(context, R.string.deletion_success, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, R.string.deletion_failed, Toast.LENGTH_SHORT).show()
            }
        }

        statusesFragment.getViewPager().doOnPageSelected(viewLifecycleOwner) {
            statusAdapter?.finishActionMode()
        }
    }

    protected abstract fun onCreateAdapter(): StatusAdapter

    override fun onScrollToTop() {
        binding.recyclerView.scrollToPosition(0)
    }

    override fun onRefresh() {
        loadStatuses()
    }

    override fun onStart() {
        super.onStart()
        statusesActivity.addPermissionsChangeListener(this)
    }

    override fun onStop() {
        super.onStop()
        statusesActivity.removePermissionsChangeListener(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        statusAdapter?.unregisterAdapterDataObserver(adapterDataObserver)
        statusAdapter = null
    }

    private val adapterDataObserver = object : AdapterDataObserver() {
        override fun onChanged() {
            binding.emptyView.isVisible = statusAdapter.isNullOrEmpty()
        }
    }

    override fun onHasPermissionsChangeListener() {
        loadStatuses()
    }

    override fun onMultiSelectionAction(item: MenuItem, selection: List<Status>) {
        when (item.itemId) {
            R.id.action_share -> {
                requestContext { context ->
                    startActivitySafe(
                        selection.toShareIntent(context).toChooser(getString(R.string.share_with))
                    )
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
                        deletionRequestLauncher.launch(IntentSenderRequest.Builder(it).build())
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

    override fun onPreviewStatusClick(status: Status) {
        requestContext { context ->
            startActivitySafe(status.toPreviewIntent(context)) { _: Throwable, activityNotFound: Boolean ->
                if (activityNotFound) {
                    requestView { view ->
                        Snackbar.make(
                            view, getString(
                                R.string.there_is_not_an_app_available_to_open_this_x, getString(status.type.nameRes)
                            ), Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    override fun onSaveStatusClick(status: Status) {
        requestContext { context ->
            viewModel.isSaved(status).observe(viewLifecycleOwner) { isSaved ->
                if (isSaved) {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.save_again_title)
                        .setMessage(R.string.you_saved_this_status_previously)
                        .setPositiveButton(R.string.save_action) { _, _ ->
                            saveStatus(status)
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                } else {
                    saveStatus(status)
                }
            }
        }
    }

    override fun onShareStatusClick(status: Status) {
        requestContext { context ->
            startActivitySafe(status.toShareIntent(context).toChooser(getString(R.string.share_with)))
        }
    }

    @SuppressLint("CheckResult")
    private fun saveStatus(status: Status) {
        if (preferences().isRequireSaveName()) {
            context?.requestStatusName(getNewSaveName()) { entered ->
                viewModel.saveStatus(status, entered).observe(viewLifecycleOwner) {
                    processSaveResult(it)
                }
            }
        } else {
            viewModel.saveStatus(status).observe(viewLifecycleOwner) {
                processSaveResult(it)
            }
        }
    }

    protected abstract fun onLoadStatuses(type: StatusType)

    protected abstract fun onEmptyViewButtonClick()

    private fun processSaveResult(result: SaveResult) {
        requestView { view ->
            if (result.isSaving) {
                Snackbar.make(view, R.string.saving_status, Snackbar.LENGTH_SHORT).show()
            } else {
                if (result.isSuccess) {
                    val message = getString(R.string.saved_x_statuses, result.saved)
                    val snackBar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                    if (result.saved == 1) {
                        snackBar.setAction(R.string.open_action) {
                            onPreviewStatusClick(result.statuses.single())
                        }
                    } else {
                        snackBar.setAction(R.string.share_action) {
                            startActivitySafe(result.statuses.toShareIntent(requireContext()))
                        }
                    }
                    snackBar.show()
                    loadStatuses()
                } else {
                    Snackbar.make(view, R.string.failed_to_save, Snackbar.LENGTH_SHORT).show()
                }
            }
            statusAdapter?.isSavingContent = result.isSaving
        }
    }

    protected fun processDeletionResult(result: DeletionResult) {
        requestView { view ->
            if (result.isSuccess) {
                Snackbar.make(view, R.string.deletion_success, Snackbar.LENGTH_SHORT).show()
                loadStatuses()
            } else {
                Snackbar.make(view, R.string.deletion_failed, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    protected fun loadStatuses() {
        requestView {
            binding.emptyView.isVisible = false
            binding.swipeRefreshLayout.isRefreshing = true
            onLoadStatuses(statusType)
        }
    }

    companion object {
        const val EXTRA_TYPE = "extra_type"
    }
}