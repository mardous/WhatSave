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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialFadeThrough
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.WhatSaveViewModel
import com.simplified.wsstatussaver.databinding.DialogSafTutorialBinding
import com.simplified.wsstatussaver.databinding.FragmentOnboardBinding
import com.simplified.wsstatussaver.extensions.IsSAFRequired
import com.simplified.wsstatussaver.extensions.Space
import com.simplified.wsstatussaver.extensions.applyWindowInsets
import com.simplified.wsstatussaver.extensions.directoryAccessRequestIntent
import com.simplified.wsstatussaver.extensions.formattedAsHtml
import com.simplified.wsstatussaver.extensions.getOnBackPressedDispatcher
import com.simplified.wsstatussaver.extensions.hasPermissions
import com.simplified.wsstatussaver.extensions.hasSAFPermissions
import com.simplified.wsstatussaver.extensions.hasStoragePermissions
import com.simplified.wsstatussaver.extensions.releasePermissions
import com.simplified.wsstatussaver.extensions.requestWithoutOnboard
import com.simplified.wsstatussaver.extensions.showToast
import com.simplified.wsstatussaver.extensions.takePermissions
import com.simplified.wsstatussaver.fragments.base.BaseFragment
import com.simplified.wsstatussaver.fragments.binding.OnboardBinding
import com.simplified.wsstatussaver.interfaces.IPermissionChangeListener
import com.simplified.wsstatussaver.model.WaDirectory
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class OnboardFragment : BaseFragment(R.layout.fragment_onboard), View.OnClickListener,
    IPermissionChangeListener {

    private val args by navArgs<OnboardFragmentArgs>()
    private var _binding: OnboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WhatSaveViewModel by activityViewModel()

    private lateinit var permissionRequest: ActivityResultLauncher<Intent>

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
            if (result != null && takePermissions(result)) {
                viewModel.reloadAll()
                updateButtons()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = OnboardBinding(FragmentOnboardBinding.bind(view))
        binding.nestedScrollView.applyWindowInsets(top = true, bottom = true, left = true, right = true)
        binding.continueButton.applyWindowInsets(bottom = true, right = true, addedSpace = Space.viewMargin())

        postponeEnterTransition()
        enterTransition = MaterialFadeThrough().addTarget(view)
        reenterTransition = MaterialFadeThrough().addTarget(view)
        view.doOnPreDraw { startPostponedEnterTransition() }

        binding.grantStorageButton.setOnClickListener(this)
        binding.continueButton.setOnClickListener(this)
        setupViews()
        setupGrantButton()
        setupDirectoryAccess()

        statusesActivity.addPermissionsChangeListener(this)
        getOnBackPressedDispatcher().addCallback(viewLifecycleOwner, onBackPressedCallback)
    }

    private fun setupViews() {
        if (args.isFromSettings) {
            binding.subtitle.isVisible = false
        }
    }

    private fun setupGrantButton(hasPermissions: Boolean = hasStoragePermissions()) {
        if (hasPermissions) {
            binding.grantStorageButton.setIconResource(R.drawable.ic_round_check_24dp)
            binding.grantStorageButton.setText(R.string.permission_granted)
        } else {
            binding.grantStorageButton.icon = null
            binding.grantStorageButton.setText(R.string.grant_permissions)
        }
        binding.grantStorageButton.isEnabled = !hasPermissions
    }

    private fun setupDirectoryAccess() {
        if (!IsSAFRequired) {
            binding.directoryPermissionView.isGone = true
        } else {
            updateButtons()
            binding.listDirectoriesButton.setOnClickListener {
                viewModel.getReadableDirectoryPaths(requireContext()).observe(viewLifecycleOwner) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.accessible_directories)
                        .setItems(it, null)
                        .setPositiveButton(R.string.close_action, null)
                        .show()
                }
            }
            binding.revokeDirectoryAccessButton.setOnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.revoke_permissions_title)
                    .setMessage(R.string.revoke_directory_access_message)
                    .setPositiveButton(R.string.revoke_action) { _: DialogInterface, _: Int ->
                        if (releasePermissions()) {
                            showToast(R.string.permissions_revoked_successfully)
                            updateButtons()
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
            binding.grantDirectoryAccessButton.setOnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.how_to_title)
                    .setView(
                        DialogSafTutorialBinding.inflate(layoutInflater).also {
                            it.textView.text = getString(R.string.saf_tutorial_1, WaDirectory.Media.path).formattedAsHtml()
                        }.root
                    )
                    .setPositiveButton(R.string.got_it_action) { _: DialogInterface, _: Int ->
                        permissionRequest.launch(directoryAccessRequestIntent())
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }
    }

    private fun updateButtons() {
        val hasSAFPermissions = hasSAFPermissions()
        binding.listDirectoriesButton.isVisible = hasSAFPermissions
        binding.revokeDirectoryAccessButton.isVisible = hasSAFPermissions
    }

    override fun permissionsStateChanged(hasPermissions: Boolean) {
        setupGrantButton(hasPermissions)
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