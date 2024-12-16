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
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.doOnPreDraw
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialFadeThrough
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.WhatSaveViewModel
import com.simplified.wsstatussaver.adapter.ClientAdapter
import com.simplified.wsstatussaver.databinding.FragmentOnboardBinding
import com.simplified.wsstatussaver.extensions.applyPortraitInsetter
import com.simplified.wsstatussaver.extensions.formattedAsHtml
import com.simplified.wsstatussaver.extensions.getClientSAFIntent
import com.simplified.wsstatussaver.extensions.getOnBackPressedDispatcher
import com.simplified.wsstatussaver.extensions.hasPermissions
import com.simplified.wsstatussaver.extensions.hasQ
import com.simplified.wsstatussaver.extensions.hasStoragePermissions
import com.simplified.wsstatussaver.extensions.isNullOrEmpty
import com.simplified.wsstatussaver.extensions.launchSafe
import com.simplified.wsstatussaver.extensions.openWeb
import com.simplified.wsstatussaver.extensions.requestWithoutOnboard
import com.simplified.wsstatussaver.extensions.showToast
import com.simplified.wsstatussaver.extensions.takePermissions
import com.simplified.wsstatussaver.fragments.AboutFragment
import com.simplified.wsstatussaver.fragments.base.BaseFragment
import com.simplified.wsstatussaver.fragments.binding.OnboardBinding
import com.simplified.wsstatussaver.interfaces.IClientCallback
import com.simplified.wsstatussaver.interfaces.IPermissionChangeListener
import com.simplified.wsstatussaver.model.WaClient
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class OnboardFragment : BaseFragment(R.layout.fragment_onboard), View.OnClickListener, IClientCallback,
    IPermissionChangeListener {

    private val args by navArgs<OnboardFragmentArgs>()
    private var _binding: OnboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WhatSaveViewModel by activityViewModel()

    private lateinit var permissionRequest: ActivityResultLauncher<Intent>
    private var clientAdapter: ClientAdapter? = null
    private var selectedClient: WaClient? = null

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
            if (result != null && selectedClient?.takePermissions(requireContext(), result) == true) {
                viewModel.reloadAll()
                clientAdapter?.notifyDataSetChanged()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = OnboardBinding(FragmentOnboardBinding.bind(view))
        binding.agreementText.applyPortraitInsetter {
            type(navigationBars = true) {
                margin()
            }
        }

        postponeEnterTransition()
        enterTransition = MaterialFadeThrough().addTarget(view)
        reenterTransition = MaterialFadeThrough().addTarget(view)
        view.doOnPreDraw { startPostponedEnterTransition() }

        binding.grantStorageButton.setOnClickListener(this)
        binding.continueButton.setOnClickListener(this)
        binding.privacyPolicyButton.setOnClickListener(this)
        setupViews()
        setupClientPermissions()
        setupGrantButtonIcon()

        viewModel.getInstalledClients().observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                binding.noClientText.isVisible = true
                binding.recyclerView.isVisible = false
            } else {
                binding.noClientText.isVisible = false
                binding.recyclerView.isVisible = true
            }
            clientAdapter?.setClients(it)
        }

        statusesActivity.addPermissionsChangeListener(this)
        getOnBackPressedDispatcher().addCallback(viewLifecycleOwner, onBackPressedCallback)
    }

    private fun setupViews() {
        if (args.isFromSettings) {
            binding.subtitle.isVisible = false
            binding.agreementText.isVisible = false
            binding.storagePermissionView.isGone = hasStoragePermissions()
        }
    }

    private fun setupGrantButtonIcon() {
        val iconRes = if (hasStoragePermissions()) R.drawable.ic_round_check_24dp else R.drawable.ic_storage_24dp
        binding.grantStorageButton.setIconResource(iconRes)
    }

    private fun setupClientPermissions() {
        if (hasQ()) {
            clientAdapter = ClientAdapter(requireContext(), R.layout.item_client_onboard, this)
            binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerView.adapter = clientAdapter
        } else {
            binding.clientPermissionView.isVisible = false
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun clientClick(client: WaClient) {
        if (client.hasPermissions(requireContext())) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.revoke_permissions_title)
                .setMessage(
                    getString(
                        R.string.revoke_permissions_message,
                        client.displayName
                    ).formattedAsHtml()
                )
                .setPositiveButton(R.string.revoke_action) { _: DialogInterface, _: Int ->
                    if (client.releasePermissions(requireContext())) {
                        showToast(R.string.permissions_revoked_successfully)
                        viewModel.reloadAll()
                        clientAdapter?.notifyDataSetChanged()
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        } else if (hasQ()) { // Just to remove the Lint warning
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(getString(R.string.saf_tutorial, client.getSAFDirectoryPath()).formattedAsHtml())
                .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                    this.selectedClient = client
                    permissionRequest.launchSafe(requireContext().getClientSAFIntent(client))
                }
                .show()
        }
    }

    override fun checkModeForClient(client: WaClient): Int {
        if (client.hasPermissions(requireContext())) {
            return IClientCallback.MODE_CHECKED
        }
        return IClientCallback.MODE_UNCHECKED
    }

    override fun permissionsStateChanged(hasPermissions: Boolean) {
        setupGrantButtonIcon()
        if (hasPermissions) {
            viewModel.reloadAll()
        }
    }

    override fun onClick(view: View) {
        when (view) {
            binding.grantStorageButton -> {
                if (!hasStoragePermissions()) {
                    requestWithoutOnboard()
                }
            }

            binding.privacyPolicyButton -> {
                requireContext().openWeb(AboutFragment.PRIVACY_POLICY)
            }

            binding.continueButton -> {
                getOnBackPressedDispatcher().onBackPressed()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.loadClients()
    }

    override fun onDestroyView() {
        statusesActivity.removePermissionsChangeListener(this)
        super.onDestroyView()
        _binding = null
    }

    private fun handleBackPress(): Boolean {
        // WORKAROUND: Sometimes the callback is executed even when the fragment has already
        // been removed, which is why some users got IllegalStateException errors.
        // For now, we only have to manually check the state of the fragment and cancel the
        // callback by returning 'true' when it is not visible.
        if (!isVisible) return true
        if (clientAdapter?.isNullOrEmpty() == true) return false
        if (!hasPermissions()) {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.permissions_denied_message)
                .setPositiveButton(R.string.continue_action) { _: DialogInterface, _: Int ->
                    closeOnboard()
                }
                .setNegativeButton(R.string.grant_permissions, null)
                .show()
            return true
        }
        return false
    }

    private fun closeOnboard() {
        if (isVisible) findNavController().popBackStack()
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (!handleBackPress()) {
                remove()
                getOnBackPressedDispatcher().onBackPressed()
            }
        }
    }
}