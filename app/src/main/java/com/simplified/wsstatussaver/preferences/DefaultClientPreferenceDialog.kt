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
package com.simplified.wsstatussaver.preferences

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.content.DialogInterface.OnShowListener
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.WhatSaveViewModel
import com.simplified.wsstatussaver.adapter.ClientAdapter
import com.simplified.wsstatussaver.databinding.DialogRecyclerviewBinding
import com.simplified.wsstatussaver.extensions.getDefaultClient
import com.simplified.wsstatussaver.extensions.setDefaultClient
import com.simplified.wsstatussaver.extensions.showToast
import com.simplified.wsstatussaver.interfaces.IClientCallback
import com.simplified.wsstatussaver.model.WaClient
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * @author Christians Martínez Alvarado (mardous)
 */
class DefaultClientPreferenceDialog : DialogFragment(), OnShowListener, IClientCallback {

    private var _binding: DialogRecyclerviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WhatSaveViewModel by activityViewModel()

    private lateinit var clientAdapter: ClientAdapter
    private var defaultClient: WaClient? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogRecyclerviewBinding.inflate(layoutInflater)
        binding.empty.setText(R.string.installed_clients_empty)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = ClientAdapter(binding.root.context, R.layout.item_client, this).apply {
            registerAdapterDataObserver(adapterDataObserver)
        }.also {
            clientAdapter = it
        }

        defaultClient = requireContext().getDefaultClient()
        viewModel.getInstalledClients().observe(this) { installedClients ->
            clientAdapter.setClients(installedClients)
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.default_client_title)
            .setView(binding.root)
            .setNegativeButton(R.string.close_action, null)
            .create().also {
                it.setOnShowListener(this)
            }
    }

    private val adapterDataObserver: AdapterDataObserver = object : AdapterDataObserver() {
        override fun onChanged() {
            super.onChanged()
            binding.empty.isVisible = clientAdapter.itemCount == 0
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun clientClick(client: WaClient) {
        defaultClient = if (client == defaultClient) null else client
        if (defaultClient == null) {
            showToast(R.string.default_client_cleared)
        } else {
            showToast(getString(R.string.x_is_the_default_client_now, client.displayName))
        }
        requireContext().setDefaultClient(defaultClient)
        clientAdapter.notifyDataSetChanged()
    }

    override fun checkModeForClient(client: WaClient): Int {
        return if (client == defaultClient) IClientCallback.MODE_CHECKED else IClientCallback.MODE_UNCHECKED
    }

    override fun onShow(dialogInterface: DialogInterface) {
        viewModel.loadClients()
    }

    override fun onDismiss(dialog: DialogInterface) {
        clientAdapter.unregisterAdapterDataObserver(adapterDataObserver)
        super.onDismiss(dialog)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}