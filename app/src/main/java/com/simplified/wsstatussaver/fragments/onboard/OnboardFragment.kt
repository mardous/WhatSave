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
package com.simplified.wsstatussaver.fragments.onboard

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.ContentResolver
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.WhatSaveViewModel
import com.simplified.wsstatussaver.adapter.ClientAdapter
import com.simplified.wsstatussaver.databinding.FragmentOnboardBinding
import com.simplified.wsstatussaver.dialogs.PrivacyDialog
import com.simplified.wsstatussaver.extensions.*
import com.simplified.wsstatussaver.fragments.base.BaseFragment
import com.simplified.wsstatussaver.interfaces.IClientCallback
import com.simplified.wsstatussaver.interfaces.IPermissionChangeListener
import com.simplified.wsstatussaver.model.WaClient
import org.koin.androidx.viewmodel.ext.android.activityViewModel

@TargetApi(Build.VERSION_CODES.Q)
class OnboardFragment : BaseFragment(R.layout.fragment_onboard), View.OnClickListener, IClientCallback,
    IPermissionChangeListener {

    private var _binding: OnboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WhatSaveViewModel by activityViewModel()
    private val contentResolver: ContentResolver
        get() = requireContext().contentResolver

    private lateinit var permissionRequest: ActivityResultLauncher<Intent>
    private var clientAdapter: ClientAdapter? = null
    private var selectedClient: WaClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionRequest =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
                if (result != null) takePermissions(result)
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = OnboardBinding(FragmentOnboardBinding.bind(view))

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            binding.grantStorageButton.setOnClickListener(this)
        } else {
            clientAdapter = ClientAdapter(requireContext(), this)
            binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerView.adapter = clientAdapter
            binding.recyclerView.isVisible = true
            binding.storagePermissionView.isVisible = false
        }

        binding.continueButton.setOnClickListener(this)
        binding.privacyPolicyButton.setOnClickListener(this)

        viewModel.getInstalledClients().observe(viewLifecycleOwner) {
            clientAdapter?.setClients(it)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun takePermissions(result: ActivityResult) {
        if (selectedClient != null && result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return
            if (uri.isFromClient(selectedClient!!)) {
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                showToast(R.string.permissions_granted_successfully)
                clientAdapter?.notifyDataSetChanged()
            } else {
                showToast(R.string.select_the_correct_location, Toast.LENGTH_LONG)
            }
        }
    }

    private fun permissionsDenied(): Boolean {
        if (hasQ()) {
            return requireContext().hasNoPermissions()
        }
        return !hasStoragePermission()
    }

    override fun clientClick(client: WaClient) {
        this.selectedClient = client
        permissionRequest.launch(requireContext().getClientSAFIntent(client))
    }

    override fun checkModeForClient(client: WaClient): Int {
        if (client.hasPermissions(requireContext())) {
            return IClientCallback.MODE_CHECKED
        }
        return IClientCallback.MODE_UNCHECKABLE
    }

    override fun onHasPermissionsChangeListener() {
        if (binding.storagePermissionView.isVisible) {
            val iconRes = if (hasStoragePermission()) R.drawable.ic_round_check_24dp else R.drawable.ic_storage_24dp
            binding.grantStorageButton.setIconResource(iconRes)
        }
    }

    override fun onClick(view: View) {
        when (view) {
            binding.grantStorageButton -> {
                if (!hasStoragePermission()) {
                    requestPermission(false)
                }
            }

            binding.privacyPolicyButton -> PrivacyDialog().show(childFragmentManager, "PRIVACY_POLICY")
            binding.continueButton -> {
                if (permissionsDenied()) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setMessage(R.string.has_no_permissions_warning)
                        .setPositiveButton(R.string.continue_action) { _: DialogInterface, _: Int ->
                            findNavController().popBackStack()
                        }
                        .setNegativeButton(R.string.grant_permissions, null)
                        .show()
                    return
                }
                findNavController().popBackStack()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.loadClients()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}